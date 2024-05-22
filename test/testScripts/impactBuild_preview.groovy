
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

// create impact build command for preview
def impactBuildPreviewCommand = []
impactBuildPreviewCommand << "${dbbHome}/bin/groovyz"
impactBuildPreviewCommand << "${props.zAppBuildDir}/build.groovy"
impactBuildPreviewCommand << "--workspace ${props.workspace}"
impactBuildPreviewCommand << "--application ${props.app}"
impactBuildPreviewCommand << (props.outDir ? "--outDir ${props.outDir}" : "--outDir ${props.zAppBuildDir}/out")
impactBuildPreviewCommand << "--hlq ${props.hlq}"
impactBuildPreviewCommand << "--logEncoding UTF-8"
impactBuildPreviewCommand << (props.url ? "--url ${props.url}" : "")
impactBuildPreviewCommand << (props.id ? "--id ${props.id}" : "")
impactBuildPreviewCommand << (props.pw ? "--pw ${props.pw}" : "") 
impactBuildPreviewCommand << (props.pwFile ? "--pwFile ${props.pwFile}" : "")
impactBuildPreviewCommand << (props.verbose ? "--verbose" : "")
impactBuildPreviewCommand << (props.propFiles ? "--propFiles ${props.propFiles}" : "")
impactBuildPreviewCommand << "--impactBuild --preview" // this will run zAppBuild only in preview mode.

// create impact build command
def impactBuildCommand = []
impactBuildCommand << "${dbbHome}/bin/groovyz"
impactBuildCommand << "${props.zAppBuildDir}/build.groovy"
impactBuildCommand << "--workspace ${props.workspace}"
impactBuildCommand << "--application ${props.app}"
impactBuildCommand << (props.outDir ? "--outDir ${props.outDir}" : "--outDir ${props.zAppBuildDir}/out")
impactBuildCommand << "--hlq ${props.hlq}"
impactBuildCommand << "--logEncoding UTF-8"
impactBuildCommand << (props.url ? "--url ${props.url}" : "")
impactBuildCommand << (props.id ? "--id ${props.id}" : "")
impactBuildCommand << (props.pw ? "--pw ${props.pw}" : "") 
impactBuildCommand << (props.pwFile ? "--pwFile ${props.pwFile}" : "")
impactBuildCommand << (props.verbose ? "--verbose" : "")
impactBuildCommand << (props.propFiles ? "--propFiles ${props.propFiles}" : "")
impactBuildCommand << "--impactBuild"

// iterate through change files to test impact build
@Field def assertionList = []
PropertyMappings filesBuiltMappings = new PropertyMappings('impactBuild_preview_expectedFilesBuilt')
def changedFiles = props.impactBuild_preview_changedFiles.split(',')
println("** Processing changed files from impactBuild_preview_changedFiles property : ${props.impactBuild_preview_changedFiles}")
try {
	
	// Create full build command to set baseline
	testUtils.runBaselineBuild()
	
	changedFiles.each { changedFile ->
		println "\n** Running IMPACT BUILD WITH PREVIEW TEST for changed file $changedFile"
		
		// update changed file in Git repo test branch
		testUtils.updateFileAndCommit(props.appLocation, changedFile)
		
		// run impact build with preview
		println "** Executing ${impactBuildPreviewCommand.join(" ")}"
		outputStream = new StringBuffer()
		process = ['bash', '-c', impactBuildPreviewCommand.join(" ")].execute()
		process.waitForProcessOutput(outputStream, System.err)
		
		validateImpactBuild(changedFile, filesBuiltMappings, outputStream)
		
		// run impact build
		println "** Executing ${impactBuildCommand.join(" ")}"
		outputStream = new StringBuffer()
		process = ['bash', '-c', impactBuildCommand.join(" ")].execute()
		process.waitForProcessOutput(outputStream, System.err)
		
		// validate build results
		// still expecting the same files being impacted
		validateImpactBuild(changedFile, filesBuiltMappings, outputStream)
	}
}
finally {
	// report failures
	if (assertionList.size()>0) {
        println "\n***"
	println "**START OF FAILED IMPACT BUILD WITH PREVIEW TEST RESULTS**\n"
	println "*FAILED IMPACT BUILD WITH PREVIEW TEST RESULTS*\n" + assertionList
	println "\n**END OF FAILED IMPACT BUILD WITH PREVIEW TEST RESULTS**"
	println "***"
  }
  
  // cleanup datasets
  testUtils.cleanUpDatasets(props.impactBuild_preview_datasetsToCleanUp)
  
}
// script end  

//*************************************************************
// Method Definitions
//*************************************************************

def validateImpactBuild(String changedFile, PropertyMappings filesBuiltMappings, StringBuffer outputStream) {

	println "** Validating impact build results"
	def expectedFilesBuiltList = filesBuiltMappings.getValue(changedFile).split(',')
	
    try{
	// Validate clean build
	assert outputStream.contains("Build State : CLEAN") : "*! IMPACT BUILD FAILED FOR $changedFile\nOUTPUT STREAM:\n$outputStream\n"

	// Validate expected number of files built
	def numImpactFiles = expectedFilesBuiltList.size()
	assert outputStream.contains("Total files processed : ${numImpactFiles}") : "*! IMPACT BUILD FOR $changedFile TOTAL FILES PROCESSED ARE NOT EQUAL TO ${numImpactFiles}\nOUTPUT STREAM:\n$outputStream\n"

	// Validate expected built files in output stream
	assert expectedFilesBuiltList.count{ i-> outputStream.contains(i) } == expectedFilesBuiltList.size() : "*! IMPACT BUILD FOR $changedFile DOES NOT CONTAIN THE LIST OF BUILT FILES EXPECTED ${expectedFilesBuiltList}\nOUTPUT STREAM:\n$outputStream\n"
	
	println "**"
	println "** IMPACT BUILD WITH PREVIEW TEST : PASSED FOR $changedFile **"
	println "**"
    }
    catch(AssertionError e) {
        def result = e.getMessage()
        assertionList << result;
		props.testsSucceeded = 'false'
 }
}
