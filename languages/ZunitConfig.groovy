@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.metadata.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*
import groovy.xml.*


// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def buildUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BuildUtilities.groovy"))
@Field def impactUtils= loadScript(new File("${props.zAppBuildDir}/utilities/ImpactUtilities.groovy"))

println("** Building ${argMap.buildList.size()} ${argMap.buildList.size() == 1 ? 'file' : 'files'} mapped to ${this.class.getName()}.groovy script")

// verify required build properties
buildUtils.assertBuildProperties(props.cobol_requiredBuildProperties)
buildUtils.assertBuildProperties(props.zunit_requiredBuildProperties)

def langQualifier = "zunit"
buildUtils.createLanguageDatasets(langQualifier)
int currentBuildFileNumber = 1

// iterate through build list
(argMap.buildList.sort()).each { buildFile ->
	println "*** (${currentBuildFileNumber++}/${argMap.buildList.size()}) Building file $buildFile"

	String member = CopyToPDS.createMemberName(buildFile)

	File logFile = new File("${props.buildOutDir}/${member}.zunit.jcl.log")
	File reportLogFile = new File("${props.buildOutDir}/${member}.zunit.report.log")

	
	String dependencySearch = props.getFileProperty('zunit_dependencySearch', buildFile)
	SearchPathDependencyResolver dependencyResolver = new SearchPathDependencyResolver(dependencySearch)
	
	// copy build file and dependency files to data sets
	buildUtils.copySourceFiles(buildFile, props.zunit_bzucfgPDS, 'zunit_dependenciesDatasetMapping', null, dependencyResolver)

	// get logical file
	LogicalFile logicalFile = buildUtils.createLogicalFile(dependencyResolver, buildFile)
		
	// get playback dependency for bzucfg file from logicalFile
 	LogicalDependency playbackFile = getPlaybackFile(logicalFile);
	
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
"""
// Add parms for bzupplay proc / zUnit Runner	
zunitParms = props.getFileProperty('zunit_bzuplayParms', buildFile)
jcl += """\
//  PARM=('$zunitParms')
"""
	if (playbackFile != null) { // bzucfg contains reference to a playback file
		jcl +=
		"//REPLAY.BZUPLAY DD DISP=SHR, \n" +
		"// DSN=${props.zunit_bzuplayPDS}(${playbackFile.getLname()}) \n"
	} else { // no playbackfile referenced
		jcl +=
		"//REPLAY.BZUPLAY DD DUMMY   \n"
	}

	jcl += """\
//REPLAY.BZURPT DD DISP=SHR,
// DSN=${props.zunit_bzureportPDS}(${member})
"""

// Add parms for bzupplay proc / zUnit Runner
zunitDebugParm = props.getFileProperty('zunit_userDebugSessionTestParm', buildFile)

// if code coverage collection is activated
	if (props.codeZunitCoverage && props.codeZunitCoverage.toBoolean()) {
	        // codeCoverageHost
		if (props.codeCoverageHeadlessHost != null)
			codeCoverageHost = props.codeCoverageHeadlessHost
               else
			codeCoverageHost = props.getFileProperty('zunit_CodeCoverageHost', buildFile)
	        // codeCoveragePort
		if (props.codeCoverageHeadlessPort != null)
			codeCoveragePort = props.codeCoverageHeadlessPort
               else
			codeCoveragePort = props.getFileProperty('zunit_CodeCoveragePort', buildFile)
		// codeCoverageOptions
		if (props.codeCoverageOptions != null)
			codeCoverageOptions = props.codeCoverageOptions
               else
			codeCoverageOptions = props.getFileProperty('zunit_CodeCoverageOptions', buildFile)
	
		jcl +=
		"//CEEOPTS DD *                        \n"   +
		( ( codeCoverageHost != null && codeCoveragePort != null && !props.userBuild ) ? "TEST(,,,TCPIP&${codeCoverageHost}%${codeCoveragePort}:*)  \n" : "${zunitDebugParm}  \n" ) +
		"ENVAR(\n"
		if (codeCoverageOptions != null) {
			optionsParms = splitCCParms('"' + "EQA_STARTUP_KEY=CC,${member},t=${member},i=${member}," + codeCoverageOptions + '")');
			optionsParms.each { optionParm ->
				jcl += optionParm + "\n";
			}
		} else {
			jcl += '"' + "EQA_STARTUP_KEY=CC,${member},t=${member},i=${member}" +'")' + "\n"
		}
   		jcl += "/* \n"
	} else if (props.debugzUnitTestcase && props.userBuild) {
		// initiate debug session of test case 
		jcl +=
		"//CEEOPTS DD *                        \n"   +
		  "${zunitDebugParm}  \n"
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

	//  // Extract Job BZURPT as XML
	//  def logEncoding = "UTF-8"
	//  zUnitRunJCL.getAllDDNames().each({ ddName ->
	//    if (ddName == 'XML') {
	//      def file = new File("${workDir}/zUnitRunJCLiew${ddName}.xml")
	//      zUnitRunJCL.saveOutput(ddName, file, logEncoding)
	//    }
	//    if (ddName == 'JUNIT') {
	//      def file = new File("${workDir}/zUnitRunJCLiew${ddName}.xml")
	//      zUnitRunJCL.saveOutput(ddName, file, logEncoding)
	//    }
	//    if (ddName == 'CSV') {
	//      def file = new File("${workDir}/zUnitRunJCLiew${ddName}.csv")
	//      zUnitRunJCL.saveOutput(ddName, file, logEncoding)
	//    }
	//  })


	/**
	 * Error Handling
	 * RC 0
	 * RC 4 will keep the build clean, but indicate a warning
	 * RC >=8 will make the build fail
	 *
	 */

	// Evaluate if running in preview build mode
	if (!props.preview) {

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
				buildUtils.updateBuildResult(warningMsg:warningMsg,logs:["${member}_zunit.log":logFile])
			} else { // rc > props.zunit_maxWarnRC.toInteger()
				props.error = "true"
				String errorMsg = "*! The zunit test failed with RC=($rc) for $buildFile "
				println(errorMsg)
				buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}_zunit.log":logFile])
			}
		}
		else {
			// We don't see the CC, assume an exception
			props.error = "true"
			String errorMsg = "*!  zUnit Test Job ${zUnitRunJCL.submittedJobId} failed with ${zUnitRunJCL.maxRC}"
			println(errorMsg)
			buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}_zunit.log":logFile])
		}
	} else { // skip evaluating Unit tests result
		if (props.verbose) println "*** Evaluation of zUnit test result skipped, because running in preview mode."
	}

}

/**
 * Methods
 */

/*
 * returns the LogicalDependency of the playbackfile
 */
def getPlaybackFile(LogicalFile logicalFile) {
 	// find playback file dependency
 	LogicalDependency playbackDependency = logicalFile.getLogicalDependencies().find {
 		it.getLibrary() == "SYSPLAY"
 	}
	return playbackDependency
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

def splitCCParms(String parms) {
	def outParms = []
	for (int chunk = 0; chunk <= (parms.length().intdiv(72)); chunk++) {
		maxLength = (parms.length() - (chunk * 72))
		if (maxLength > 72)
			maxLength = 72
		outParms.add(parms.substring((chunk * 72), (chunk * 72) + maxLength));
	}
	return outParms
}
