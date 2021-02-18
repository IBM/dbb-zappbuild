@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.repository.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*


// define script properties
@Field BuildProperties zApp = BuildProperties.getInstance()
@Field def buildUtils= loadScript(new File("${zApp.zAppBuildDir}/utilities/BuildUtilities.groovy"))
@Field def impactUtils= loadScript(new File("${zApp.zAppBuildDir}/utilities/ImpactUtilities.groovy"))
@Field def props = buildUtils 
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

	// Parse the playback from the bzucfg file
	String xml = new File(buildUtils.getAbsolutePath(buildFile)).getText("IBM-1047")

	String playback;
	for (line in xml.split('\n')) {
		if (line.contains("runner:playback moduleName")) {
			playback = line.split("=")[1].split("\"")[1]
		}
	}
	
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
//* Action: Run Test Case...
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



