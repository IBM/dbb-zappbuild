@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import groovy.transform.*
import com.ibm.dbb.*
import com.ibm.dbb.build.*

@Field BuildProperties props = BuildProperties.getInstance()
@Field def testUtils = loadScript(new File("../utils/testUtilities.groovy"))

println "\n**************************************************************"
println "** Executing test script ${this.class.getName()}.groovy"
println "**************************************************************"

// Get the DBB_HOME location
def dbbHome = EnvVars.getHome()
if (props.verbose) println "** DBB_HOME = ${dbbHome}"

// Create full build command
def fullBuildCommand = [] 
fullBuildCommand << "${dbbHome}/bin/groovyz"
fullBuildCommand << "${props.zAppBuildDir}/build.groovy"
fullBuildCommand << "--workspace ${props.workspace}"
fullBuildCommand << "--application ${props.app}"
fullBuildCommand << (props.outDir ? "--outDir ${props.outDir}" : "--outDir ${props.zAppBuildDir}/out")
fullBuildCommand << "--hlq ${props.hlq}"
fullBuildCommand << "--logEncoding UTF-8"
fullBuildCommand << (props.url ? "--url ${props.url}" : "")
fullBuildCommand << (props.id ? "--id ${props.id}" : "")
fullBuildCommand << (props.pw ? "--pw ${props.pw}" : "") 
fullBuildCommand << (props.pwFile ? "--pwFile ${props.pwFile}" : "")
fullBuildCommand << (props.verbose ? "--verbose" : "")
fullBuildCommand << (props.propFiles ? "--propFiles ${props.propFiles}" : "")
fullBuildCommand << "--fullBuild"

@Field def assertionList = []

def changedFiles = props.fullBuild_truncation_changedFiles.split(',')
PropertyMappings fullBuild_truncation_errorMsgs = new PropertyMappings('fullBuild_truncation_errorMsg')

try {

	// Create full build command to set baseline
	testUtils.runBaselineBuild()
		
	// test setup
	changedFiles.each { changedFile ->

		println "\n** Running build test for changed file $changedFile"
		
		// update changed file in Git repo test branch
		testUtils.copyAndCommit(changedFile)
		
		// run build
		println "** Executing ${fullBuildCommand.join(" ")}"
		outputStream = new StringBuffer()
		process = ['bash', '-c', fullBuildCommand.join(" ")].execute()
		process.waitForProcessOutput(outputStream, System.err)
		
		// validate build results
		validateBuild(changedFile, fullBuild_truncation_errorMsgs, outputStream)
	}
}
catch(AssertionError e) {
	def result = e.getMessage()
	assertionList << result;
	props.testsSucceeded = 'false'
}
finally {
	// report failures
	if (assertionList.size()>0) {
        println "\n***"
	println "**START OF FULL BUILD TRUNCATION ERROR TEST RESULTS**\n"
	println "*FAILED FULL BUILD TRUNCATION ERROR TEST RESULTS*\n" + assertionList
	println "\n**END OF FULL BUILD TRUNCATION ERROR TEST RESULTS**"
	println "***"
  }
  
  // reset test branch
  testUtils.resetTestBranch()
  
  // cleanup datasets
  testUtils.cleanUpDatasets(props.fullBuild_datasetsToCleanUp)
}


//*************************************************************
// Method Definitions
//*************************************************************

def validateBuild(String changedFile, PropertyMappings fullBuild_truncation_errorMsgs, StringBuffer outputStream) {

	println "** Validating full build truncation results"
	def expectedlogMsg = fullBuild_truncation_errorMsgs.getValue(changedFile)
	
    try{
	// Validate that error state build is written to log
	assert outputStream.contains("Build State : ERROR") : "*! FULL BUILD TRUNCATION ERROR FOR $changedFile\nOUTPUT STREAM:\n$outputStream\n"

	// Validate that truncation message is written to log
	assert outputStream.contains(expectedlogMsg) : "*! FULL BUILD TRUNCATION ERROR for $changedFile\nOUTPUT STREAM:\n$outputStream\n"
	
	// Validate that Finalization process is executed
	assert outputStream.contains("********* Finalization of the build process *****") : "*! FULL BUILD TRUNCATION ERROR FOR $changedFile\nOUTPUT STREAM:\n$outputStream\n"

	println "**"
	println "** FULL BUILD TRUNCATION ERROR TEST : PASSED FOR $changedFile **"
	println "**"
    }
    catch(AssertionError e) {
        def result = e.getMessage()
        assertionList << result;
		props.testsSucceeded = 'false'
 }
}
