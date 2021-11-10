
@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import groovy.transform.*
import com.ibm.dbb.*
import com.ibm.dbb.build.*
import com.ibm.jzos.ZFile

@Field BuildProperties props = BuildProperties.getInstance()
println "\n### Executing test script impactBuild_properties.groovy"

// Get the DBB_HOME location
def dbbHome = EnvVars.getHome()
if (props.verbose) println "** DBB_HOME = ${dbbHome}"

// Create full build command to initilize property dependencies
def fullBuildCommand = []
fullBuildCommand << "${dbbHome}/bin/groovyz"
fullBuildCommand << "${props.zAppBuildDir}/build.groovy"
fullBuildCommand << "--workspace ${props.workspace}"
fullBuildCommand << "--application ${props.app}"
fullBuildCommand << (props.outDir ? "--outDir ${props.outDir}" : "--outDir ${props.zAppBuildDir}/out")
fullBuildCommand << "--hlq ${props.hlq}"
fullBuildCommand << "--logEncoding UTF-8"
fullBuildCommand << "--url ${props.url}"
fullBuildCommand << "--id ${props.id}"
fullBuildCommand << (props.pw ? "--pw ${props.pw}" : "--pwFile ${props.pwFile}")
fullBuildCommand << (props.verbose ? "--verbose" : "")
fullBuildCommand << (props.propFiles ? "--propFiles ${props.zAppBuildDir}/test/applications/${props.app}/${props.impactBuild_properties_buildPropSetting},${props.propFiles}" : "")
fullBuildCommand << "--fullBuild"

// create impact build command
def impactBuildCommand = []
impactBuildCommand << "${dbbHome}/bin/groovyz"
impactBuildCommand << "${props.zAppBuildDir}/build.groovy"
impactBuildCommand << "--workspace ${props.workspace}"
impactBuildCommand << "--application ${props.app}"
impactBuildCommand << (props.outDir ? "--outDir ${props.outDir}" : "--outDir ${props.zAppBuildDir}/out")
impactBuildCommand << "--hlq ${props.hlq}"
impactBuildCommand << "--logEncoding UTF-8"
impactBuildCommand << "--url ${props.url}"
impactBuildCommand << "--id ${props.id}"
impactBuildCommand << (props.pw ? "--pw ${props.pw}" : "--pwFile ${props.pwFile}")
impactBuildCommand << (props.verbose ? "--verbose" : "")
impactBuildCommand << (props.propFiles ? "--propFiles ${props.zAppBuildDir}/test/applications/${props.app}/${props.impactBuild_properties_buildPropSetting},${props.propFiles}" : "")
impactBuildCommand << "--impactBuild"

// iterate through change files to test impact build
@Field def assertionList = []
PropertyMappings filesBuiltMappings = new PropertyMappings('impactBuild_properties_expectedFilesBuilt')
def changedPropFile = props.impactBuild_properties_changedFile
println("** Processing changed files from impactBuild_properties_changedFiles property : ${changedPropFile}")
try {
		
		println "\n** Running build to set baseline"
				
		// run impact build
		println "** Executing ${fullBuildCommand.join(" ")}"
		def outputStream = new StringBuffer()
		def process = ['bash', '-c', fullBuildCommand.join(" ")].execute()
		process.waitForProcessOutput(outputStream, System.err)
		
		
		println "\n** Running impact build test for changed file $changedPropFile"
		
		// update changed file in Git repo test branch
		copyAndCommit(changedPropFile)
		
		// run impact build
		println "** Executing ${impactBuildCommand.join(" ")}"
		outputStream = new StringBuffer()
		process = ['bash', '-c', impactBuildCommand.join(" ")].execute()
		process.waitForProcessOutput(outputStream, System.err)
		
		// validate build results
		validateImpactBuild(changedPropFile, filesBuiltMappings, outputStream)
}
finally {
	cleanUpDatasets()
	if (assertionList.size()>0) {
		println "\n***"
	println "**START OF FAILED IMPACT BUILD ON PROPERTY CHANGE TEST RESULTS**\n"
	println "*FAILED IMPACT BUILD ON PROPERT CHANGE TEST  RESULTS*\n" + assertionList
	println "\n**END OF FAILED IMPACT BUILD ON PROPERTY CHANGE TEST RESULTS**"
	println "***"
  }
}
// script end

//*************************************************************
// Method Definitions
//*************************************************************

def copyAndCommit(String changedFile) {
	println "** Copying and committing ${props.zAppBuildDir}/test/applications/${props.app}/${changedFile} to ${props.appLocation}/${changedFile}"
	def commands = """
	cp ${props.zAppBuildDir}/test/applications/${props.app}/${changedFile} ${props.appLocation}/${changedFile}
	cd ${props.appLocation}/
	git add .
	git commit . -m "edited program file"
"""
	def task = ['bash', '-c', commands].execute()
	def outputStream = new StringBuffer();
	task.waitForProcessOutput(outputStream, System.err)
}

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
	assert expectedFilesBuiltList.count{ i-> outputStream.contains(i) } == expectedFilesBuiltList.size() : "*! IMPACT BUILD PROPERTY CHANGE $changedFile DOES NOT CONTAIN THE LIST OF BUILT FILES EXPECTED ${expectedFilesBuiltList}\nOUTPUT STREAM:\n$outputStream\n"
	
	println "**"
	println "** IMPACT BUILD ON PROPERTY CHANGE : PASSED FOR $changedFile **"
	println "**"
	}
	catch(AssertionError e) {
		def result = e.getMessage()
		assertionList << result;
 }
}
def cleanUpDatasets() {
	def segments = props.impactBuild_properties_datasetsToCleanUp.split(',')
	
	println "Deleting impact build PDSEs ${segments}"
	segments.each { segment ->
		def pds = "'${props.hlq}.${segment}'"
		if (ZFile.dsExists(pds)) {
		   if (props.verbose) println "** Deleting ${pds}"
		   ZFile.remove("//$pds")
		}
	}
}
