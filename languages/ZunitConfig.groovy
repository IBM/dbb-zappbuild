@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.repository.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*


// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def buildUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BuildUtilities.groovy"))
@Field def impactUtils= loadScript(new File("${props.zAppBuildDir}/utilities/ImpactUtilities.groovy"))
@Field def bindUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BindUtilities.groovy"))
@Field RepositoryClient repositoryClient

println("** Building files mapped to ${this.class.getName()}.groovy script")

// verify required build properties
buildUtils.assertBuildProperties(props.cobol_requiredBuildProperties)
buildUtils.assertBuildProperties(props.zunit_requiredBuildProperties)

def langQualifier = "zunit"
buildUtils.createLanguageDatasets(langQualifier)


// iterate through build list
(argMap.buildList).each { buildFile ->
	println "*** Building file $buildFile"

	String member = CopyToPDS.createMemberName(buildFile)

	File logFile = new File("${props.buildOutDir}/${member}.zunit.jcl.log")
	File reportLogFile = new File("${props.buildOutDir}/${member}.zunit.report.log")

	// copy build file and dependency files to data sets
	String rules = props.getFileProperty('zunit_resolutionRules', buildFile)

	DependencyResolver dependencyResolver = buildUtils.createDependencyResolver(buildFile, rules)

	// Parse the playback and the IO files from the bzucfg file
	String playback = getPlaybackFile(buildFile)
	def filesIO = getFilesIO(buildFile)
	
	// Upload BZUCFG file to a BZUCFG Dataset
	buildUtils.copySourceFiles(buildUtils.getAbsolutePath(buildFile), props.zunit_bzucfgPDS, props.zunit_bzuplayPDS, dependencyResolver)

	// Create JCLExec String
	String jobcard = props.jobCard.replace("\\n", "\n")
	String jcl = jobcard
	jcl += """\
\n//*
//BADRC   EXEC PGM=IEFBR14
//DD        DD DSN=&SYSUID..BADRC,DISP=(MOD,CATLG,DELETE),
//             DCB=(RECFM=FB,LRECL=80),UNIT=SYSALLDA,
//             SPACE=(TRK,(1,1),RLSE)
//*
"""
	if(filesIO) {
		// Clean up existing test files ( only if "isDispNew" )
		jcl += """\
//DELETE   EXEC PGM=IDCAMS
//SYSPRINT DD SYSOUT=*
//SYSIN    DD *
"""
		filesIO.each { file ->
			if(file['isDispNew'].toBoolean())
				jcl += "  DELETE ${file['DSN']}\n"
	 		}

		// Create the new test files ( only if "isDispNew" )
		jcl += """\
//*
//PREBZU   EXEC PGM=IEFBR14
"""
		filesIO.each { file ->
			if(file['isDispNew'].toBoolean()) {
				jcl += "//${file['DDName']} DD DISP=(NEW,CATLG,DELETE),\n"
				jcl += "//  SPACE=(TRK,(100,100),RLSE),\n"
				jcl += "//  DCB=(DSORG=PS,BLKSIZE=0,RECFM=${file['format']}"
				jcl += ",LRECL=${file['LRECL']},EROPT=ACC),\n"
				jcl += "//  DSN=${file['DSN']}\n"
			}
		}
	}
	
	// Run Test Case
	jcl += """\
\n//* Action: Run Test Case...
//*
//RUNNER EXEC PROC=BZUPPLAY,
// BZUCFG=${props.zunit_bzucfgPDS}(${member}),
// BZUCBK=${props.cobol_testcase_loadPDS},
// BZULOD=${props.cobol_loadPDS},
//  PARM=('STOP=E,REPORT=XML')
//REPLAY.BZUPLAY DD DISP=SHR,
// DSN=${props.zunit_bzuplayPDS}(${playback})
//REPLAY.BZURPT DD DISP=SHR,
// DSN=${props.zunit_bzureportPDS}(${member})
"""
	if(filesIO) {

		// Specify HLQ of test files
		jcl += """\
//AZUHLQ DD *
${props.hlq}.IO
"""
		// Pass IO files to the test runner
		filesIO.each { file ->
			jcl += "//${file['DDName']} DD DISP=SHR,DSN=${file['DSN']}\n"
		}

	}
	

	if (props.codeZunitCoverage && props.codeZunitCoverage.toBoolean()) {
	   jcl +=
	   "//CEEOPTS DD *                        \n"   +
	   ( ( props.codeCoverageHeadlessHost != null && props.codeCoverageHeadlessPort != null ) ?
	       "TEST(,,,TCPIP&${props.codeCoverageHeadlessHost}%${props.codeCoverageHeadlessPort}:*)  \n" :
	       "TEST(,,,DBMDT:*)  \n" ) +
	   "ENVAR(                                \n" +
	   '"'+ "EQA_STARTUP_KEY=CC,${member},testid=${member},moduleinclude=${member}" + '")' + "\n" +
	   "/* \n"
	}

	jcl += """\
//*
//IFGOOD IF RC<=4 THEN
//GOODRC  EXEC PGM=IEFBR14
//DD        DD DSN=&SYSUID..BADRC,DISP=(MOD,DELETE,DELETE),
//             DCB=(RECFM=FB,LRECL=80),UNIT=SYSALLDA,
//             SPACE=(TRK,(1,1),RLSE)
//       ENDIF
"""
	if (props.verbose) println(jcl)
		
	def dbbConf = System.getenv("DBB_CONF")

	// Create jclExec
	def zUnitRunJCL = new JCLExec().text(jcl)
	zUnitRunJCL.confDir(dbbConf)

	// Execute jclExec
	zUnitRunJCL.execute()

	/**
	 * Store results
	 */

	// Save Job Spool to logFile
	zUnitRunJCL.saveOutput(logFile, props.logEncoding)

	//	// Extract Job BZURPT as XML
	//	def logEncoding = "UTF-8"
	//	zUnitRunJCL.getAllDDNames().each({ ddName ->
	//		if (ddName == 'XML') {
	//			def file = new File("${workDir}/zUnitRunJCLiew${ddName}.xml")
	//			zUnitRunJCL.saveOutput(ddName, file, logEncoding)
	//		}
	//		if (ddName == 'JUNIT') {
	//			def file = new File("${workDir}/zUnitRunJCLiew${ddName}.xml")
	//			zUnitRunJCL.saveOutput(ddName, file, logEncoding)
	//		}
	//		if (ddName == 'CSV') {
	//			def file = new File("${workDir}/zUnitRunJCLiew${ddName}.csv")
	//			zUnitRunJCL.saveOutput(ddName, file, logEncoding)
	//		}
	//	})


	/**
	 * Error Handling
	 * RC 0
	 * RC 4 will keep the build clean, but indicate a warning
	 * RC >=8 will make the build fail
	 *
	 */

	// Splitting the String into a StringArray using CC as the seperator
	def jobRcStringArray = zUnitRunJCL.maxRC.split("CC")

	// This evals the number of items in the ARRAY! Dont get confused with the returnCode itself
	if ( jobRcStringArray.length > 1 ){
		// Ok, the string can be splitted because it contains the keyword CC : Splitting by CC the second record contains the actual RC
		rc = zUnitRunJCL.maxRC.split("CC")[1].toInteger()

		// manage processing the RC, up to your logic. You might want to flag the build as failed.
		if (rc <= props.zunit_maxPassRC.toInteger()){
			println   "***  zUnit Test Job ${zUnitRunJCL.submittedJobId} completed with $rc "
			// Store Report in Workspace
			new CopyToHFS().dataset(props.zunit_bzureportPDS).member(member).file(reportLogFile).hfsEncoding(props.logEncoding).append(false).copy()
			// printReport 
			printReport(reportLogFile)
		} else if (rc <= props.zunit_maxWarnRC.toInteger()){
			String warningMsg = "*! The zunit test returned a warning ($rc) for $buildFile"
			// Store Report in Workspace
			new CopyToHFS().dataset(props.zunit_bzureportPDS).member(member).file(reportLogFile).hfsEncoding(props.logEncoding).append(false).copy()
			// print warning and report
			println warningMsg
			printReport(reportLogFile)
			buildUtils.updateBuildResult(warningMsg:warningMsg,logs:["${member}_zunit.log":logFile],client:getRepositoryClient())
		} else { // rc > props.zunit_maxWarnRC.toInteger()
			props.error = "true"
			String errorMsg = "*! The zunit test failed with RC=($rc) for $buildFile "
			println(errorMsg)
			buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}_zunit.log":logFile],client:getRepositoryClient())
		}
	}
	else {
		// We don't see the CC, assume an exception
		props.error = "true"
		String errorMsg = "*!  zUnit Test Job ${zUnitRunJCL.submittedJobId} failed with ${zUnitRunJCL.maxRC}"
		println(errorMsg)
		buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}_zunit.log":logFile],client:getRepositoryClient())
	}

}

/**
 * Methods
 */

def getRepositoryClient() {
	if (!repositoryClient && props."dbb.RepositoryClient.url")
		repositoryClient = new RepositoryClient().forceSSLTrusted(true)

	return repositoryClient
}

def getPlaybackFile(String xmlFile) {
	String xml = new File(buildUtils.getAbsolutePath(xmlFile)).getText("IBM-1047")
	def parser = new XmlParser().parseText(xml)
	return("${parser.'runner:playback'.@moduleName[0]}")
}

def getFilesIO(String xmlFile) {
	
	def filesIO = []
	String xml = new File(buildUtils.getAbsolutePath(xmlFile)).getText("IBM-1047")	
	def parser = new XmlParser().parseText(xml)

/**
 *  Parse the config file and return a dictionary containing:
 *		- DDName
 *		- DSN
 *		- LRECL
 *		- format -> Dataset format, PDS and VSAM files not yet supported 
 *		- isDispNew -> Indicates if the file has to be created before running the test or is already available on the system
 */

	parser.'runner:fileAttributes'.fileAttributes.each { file -> 
		file.ddInfo.each { dd ->
			filesIO.add([
				"DDName":"${dd.@ddName}",
				"DSN":"${dd.@targetDsn.replace("<HLQ>", "${props.hlq}")}",
				"isDispNew":"${dd.@isDispNew}",
				"format":"${file.@format}",
				"LRECL":getLRECL(file.@format,file.@maxRecordLength,file.@hasCarriageControlCharacter.toBoolean())
				])
		}
	}
	
	return filesIO	
}

/**
 *  Calculate LRECL for Fixed and Variable block datasets.
 *  If hasCarriageControlCharacter the lenght is increased by 1. 
 */

def getLRECL(String format, String maxLRECL, boolean hasCCC) {
	if(format=="FB")
		if(hasCCC)
			return (maxLRECL.toInteger()+1).toString()
		else
			return maxLRECL
	else if(format=="VB")
		return (maxLRECL.toInteger()+4).toString()
	else
		return maxLRECL
}

/**
 *  Parsing the result file and prints summary of the result
 */
def printReport(File resultFile) {

	String reportString
	if (props.logEncoding != null) //if set
		reportString = new FileInputStream(resultFile).getText(props.logEncoding)
	else // Default ibm-1047
		reportString = new FileInputStream(resultFile).getText("IBM-1047")

	try {

		def runnerResult = new XmlParser().parseText(reportString)
		def testCase = runnerResult.testCase
		println "****************** Module ${testCase.@moduleName} ******************"
		println "Name:       ${testCase.@name[0]}"
		println "Status:     ${testCase.@result[0]}"
		println "Test cases: ${testCase.@tests[0]} (${testCase.@passed[0]} passed, ${testCase.@warn[0]} failed, ${testCase.@errors[0]} errors)"
		println "Details: "
		testCase.test.each { test ->
			println "      ${test.@name}   ${test.@result}"
		}
		println "****************** Module ${testCase.@moduleName} ****************** \n"
	} catch (Exception e) {
		print "! Reading zUnit result failed."
	}

}



