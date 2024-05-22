
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
impactBuildCommand << (props.propFiles ? "--propFiles ${props.propFiles},${props.zAppBuildDir}/test/applications/${props.app}/${props.impactBuild_properties_buildPropSetting}" : "--propFiles ${props.zAppBuildDir}/test/applications/${props.app}/${props.impactBuild_properties_buildPropSetting}")
impactBuildCommand << "--impactBuild"

// iterate through change files to test impact build
@Field def assertionList = []
PropertyMappings filesBuiltMappings = new PropertyMappings('impactBuild_properties_expectedFilesBuilt')
def changedPropFile = props.impactBuild_properties_changedFile
try {
		
		// Test process
	
		// Create full build command to set baseline
		testUtils.runBaselineBuild("${props.zAppBuildDir}/test/applications/${props.app}/${props.impactBuild_properties_buildPropSetting}")
	
		// update changed file in Git repo test branch
		println("\n** Injecting property update impactBuild_properties_changedFiles property : ${changedPropFile}")
		testUtils.copyAndCommit(changedPropFile)
		
		// run impact build
		println "\n** Running impact build test for changed file $changedPropFile"
		println "** Executing ${impactBuildCommand.join(" ")}"
		outputStream = new StringBuffer()
		process = ['bash', '-c', impactBuildCommand.join(" ")].execute()
		process.waitForProcessOutput(outputStream, System.err)
		
		// validate build results
		validateImpactBuild(changedPropFile, filesBuiltMappings, outputStream)
}
finally {
  // report failures
  if (assertionList.size()>0) {
		println "\n***"
	println "**START OF FAILED IMPACT BUILD ON PROPERTY CHANGE TEST RESULTS**\n"
	println "*FAILED IMPACT BUILD ON PROPERT CHANGE TEST  RESULTS*\n" + assertionList
	println "\n**END OF FAILED IMPACT BUILD ON PROPERTY CHANGE TEST RESULTS**"
	println "***"
  }
  
  // reset test branch
  testUtils.resetTestBranch()
  
  // cleanup datasets
  testUtils.cleanUpDatasets(props.impactBuild_properties_datasetsToCleanUp)
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
	assert outputStream.contains("Build State : CLEAN") : "*! IMPACT BUILD FAILED FOR CHANGED PROPERTY $changedFile\nOUTPUT STREAM:\n$outputStream\n"

	// Validate expected number of files built
	def numImpactFiles = expectedFilesBuiltList.size()
	assert outputStream.contains("Total files processed : ${numImpactFiles}") : "*! IMPACT BUILD FAILED FOR CHANGED PROPERTY  $changedFile TOTAL FILES PROCESSED ARE NOT EQUAL TO ${numImpactFiles}\nOUTPUT STREAM:\n$outputStream\n"

	// Validate expected built files in output stream
	assert expectedFilesBuiltList.count{ i-> outputStream.contains(i) } == expectedFilesBuiltList.size() : "*! IMPACT BUILD FAILED FOR CHANGED PROPERTY $changedFile DOES NOT CONTAIN THE LIST OF BUILT FILES EXPECTED ${expectedFilesBuiltList}\nOUTPUT STREAM:\n$outputStream\n"
	
	println "**"
	println "** IMPACT BUILD ON PROPERTY CHANGE : PASSED FOR $changedFile **"
	println "**"
	}
	catch(AssertionError e) {
		def result = e.getMessage()
		assertionList << result;
		props.testsSucceeded = 'false'
 }
}
