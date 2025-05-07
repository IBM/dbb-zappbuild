
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

// prepare properties file
writePropsFile()

// create merge build command
def mergeBuildCommand = []
mergeBuildCommand << "${dbbHome}/bin/groovyz"
mergeBuildCommand << "${props.zAppBuildDir}/build.groovy"
mergeBuildCommand << "--workspace ${props.workspace}"
mergeBuildCommand << "--application ${props.app}"
mergeBuildCommand << (props.outDir ? "--outDir ${props.outDir}" : "--outDir ${props.zAppBuildDir}/out")
mergeBuildCommand << "--hlq ${props.hlq}"
mergeBuildCommand << "--logEncoding UTF-8"
mergeBuildCommand << (props.url ? "--url ${props.url}" : "")
mergeBuildCommand << (props.id ? "--id ${props.id}" : "")
mergeBuildCommand << (props.pw ? "--pw ${props.pw}" : "") 
mergeBuildCommand << (props.pwFile ? "--pwFile ${props.pwFile}" : "")
mergeBuildCommand << (props.verbose ? "--verbose" : "")
mergeBuildCommand << (props.propFiles ? "--propFiles ${props.zAppBuildDir}/test/applications/${props.app}/${props.mergeBuild_buildPropSetting},${props.propFiles}" : "")
mergeBuildCommand << "--mergeBuild"

// iterate through change files to test merge build
@Field def assertionList = []
PropertyMappings filesBuiltMappings = new PropertyMappings('mergeBuild_expectedFilesBuilt')
def changedFiles = props.mergeBuild_changedFiles.split(',')
try {
	
	// Create full build command to set baseline
	testUtils.runBaselineBuild()
	
	// test setup
	println("** Processing changed files from mergeBuild_changedFiles property : ${props.mergeBuild_changedFiles}")
		
	changedFiles.each { changedFile ->
		println "\n** Running merge build test for changed file $changedFile"
		
		// update changed file in Git repo test branch
		testUtils.updateFileAndCommit(props.appLocation, changedFile)
		
		// run merge build
		println "** Executing ${mergeBuildCommand.join(" ")}"
		def outputStream = new StringBuffer()
		def process = ['bash', '-c', mergeBuildCommand.join(" ")].execute()
		process.waitForProcessOutput(outputStream, System.err)
		
		// validate build results
		validateMergeBuild(changedFile, filesBuiltMappings, outputStream)
	}
}
finally {
	// report failures
	if (assertionList.size()>0) {
        println "\n***"
	println "**START OF FAILED MERGED BUILD TEST RESULTS**\n"
	println "*FAILED MERGED BUILD TEST RESULTS*\n" + assertionList
	println "\n**END OF FAILED MERGED BUILD TEST RESULTS**"
	println "***"
  }
  // cleanup datasets
  testUtils.cleanUpDatasets(props.mergeBuild_datasetsToCleanUp)
  
}
// script end  

//*************************************************************
// Method Definitions
//*************************************************************

def writePropsFile() {
	println "** Writing propFile ${props.mergeBuild_buildPropSetting} for overwriting the mainBuildBranch"
	def commands = """
    echo "# Overwriting the mainBuildBranch for the mergeBuild scenario \nmainBuildBranch=${props.branch}" > ${props.zAppBuildDir}/test/applications/${props.app}/${props.mergeBuild_buildPropSetting}
    cat ${props.zAppBuildDir}/test/applications/${props.app}/${props.mergeBuild_buildPropSetting}
"""
		def task = ['bash', '-c', commands].execute()
		def outputStream = new StringBuffer();
		task.waitForProcessOutput(outputStream, System.err)
	
}

def validateMergeBuild(String changedFile, PropertyMappings filesBuiltMappings, StringBuffer outputStream) {

	println "** Validating merge build results"
	def expectedFilesBuiltList = filesBuiltMappings.getValue(changedFile).split(',')
	
    try{
	// Validate clean build
	assert outputStream.contains("Build State : CLEAN") : "*! MERGED BUILD FAILED FOR $changedFile\nOUTPUT STREAM:\n$outputStream\n"

	// Validate expected number of files built
	def numMergeFiles = expectedFilesBuiltList.size()
	assert outputStream.contains("Total files processed : ${numMergeFiles}") : "*! MERGED BUILD FOR $changedFile TOTAL FILES PROCESSED ARE NOT EQUAL TO ${numMergeFiles}\nOUTPUT STREAM:\n$outputStream\n"

	// Validate expected built files in output stream
	assert expectedFilesBuiltList.count{ i-> outputStream.contains(i) } == expectedFilesBuiltList.size() : "*! MERGED BUILD FOR $changedFile DOES NOT CONTAIN THE LIST OF BUILT FILES EXPECTED ${expectedFilesBuiltList}\nOUTPUT STREAM:\n$outputStream\n"
	
	println "**"
	println "** MERGED BUILD TEST : PASSED FOR $changedFile **"
	println "**"
    }
    catch(AssertionError e) {
        def result = e.getMessage()
        assertionList << result;
		props.testsSucceeded = 'false'
 }
}

