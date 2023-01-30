@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import groovy.transform.*
import com.ibm.dbb.*
import com.ibm.dbb.build.*
import com.ibm.jzos.ZFile

@Field BuildProperties props = BuildProperties.getInstance()
println "\n** Executing test script fullBuild_languageConfigurations.groovy"

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
fullBuildCommand << "--url ${props.url}"
fullBuildCommand << "--id ${props.id}"
fullBuildCommand << (props.pw ? "--pw ${props.pw}" : "--pwFile ${props.pwFile}")
fullBuildCommand << (props.verbose ? "--verbose" : "")
fullBuildCommand << (props.propFiles ? "--propFiles ${props.propFiles}" : "")
fullBuildCommand << "--fullBuild"

try {

	// update language definitions files in Git repo
	def langDefs = props.fullBuild_languageConfigurations_updatedLanguageConfigs.split(',')
	langDefs.each{ langDef ->
		copyAndCommitBuildConfig(langDef)
	}
	
	// Run full build
	println "** Executing ${fullBuildCommand.join(" ")}"
	def process = [
		'bash',
		'-c',
		fullBuildCommand.join(" ")
	].execute()
	def outputStream = new StringBuffer();
	process.waitForProcessOutput(outputStream, System.err)

	//Validate build results
	println "** Validating full build with language configuration results"
	def expectedFilesBuiltList = props.fullBuild_languageConfigurations_expectedFilesBuilt.split(',')

	@Field def assertionList = []


	// Validate clean build
	assert outputStream.contains("Build State : CLEAN") : "*! FULL BUILD FAILED\nOUTPUT STREAM:\n$outputStream\n"

	// Validate expected number of files built
	def numFullFiles = expectedFilesBuiltList.size()
	assert outputStream.contains("Total files processed : ${numFullFiles}") : "*! TOTAL FILES PROCESSED ARE NOT EQUAL TO ${numFullFiles}\nOUTPUT STREAM:\n$outputStream\n"

	// Obtain property mapping of prints in the console for compile compileParms
	PropertyMappings compileParmsofFiles = new PropertyMappings("fullBuild_languageConfigurations_compileParms")
	// Iterate over build list
	expectedFilesBuiltList.each { file ->
		expectedCompilerParms = compileParmsofFiles.getValue(file)
		if (expectedCompilerParms) assert outputStream.contains("expectedCompilerParms") : "*! Expected Compiler Parms do not match for $file\nOUTPUT STREAM:\n$outputStream\n"
	}
		
	// Validate expected built files in output stream
	assert expectedFilesBuiltList.count{ i-> outputStream.contains(i) } == expectedFilesBuiltList.size() : "*! FILES PROCESSED IN THE FULL BUILD DOES NOT CONTAIN THE LIST OF FILES PASSED ${expectedFilesBuiltList}\nOUTPUT STREAM:\n$outputStream\n"
	
	println "**"
	println "** FULL BUILD TEST Language Configurations: PASSED **"
	println "**"
}
catch(AssertionError e) {
	def result = e.getMessage()
	assertionList << result;
	props.testsSucceeded = 'false'
}
finally {
	cleanUpDatasets()
	if (assertionList.size()>0) {
		println "\n***"
	println "**START OF FAILED FULL BUILD TEST Language Configurations TEST RESULTS**\n"
	println "*FAILED FULL BUILD TEST Language Configurations RESULTS*\n" + assertionList
	println "\n**END OF FAILED FULL BUILD TEST Language Configurations **"
	println "***"
  }
	
}

// script end

//*************************************************************
// Method Definitions
//*************************************************************

def copyAndCommitBuildConfig() {
	println "** Copying and committing ${props.zAppBuildDir}/test/applications/${props.app}/$configFile to ${props.zAppBuildDir}/build-conf/"
	def commands = """
	cp ${props.zAppBuildDir}/test/applications/${props.app}/$configFile ${props.zAppBuildDir}/$configFile
	cd ${props.appLocation}/
	git add .
	git commit . -m "updated language definition files"
"""
	def task = ['bash', '-c', commands].execute()
	def outputStream = new StringBuffer();
	task.waitForProcessOutput(outputStream, System.err)
}

def cleanUpDatasets() {
	def segments = props.fullBuild_languageConfigurations_datasetsToCleanUp.split(',')
	
	println "Deleting full build PDSEs ${segments}"
	segments.each { segment ->
	    def pds = "'${props.hlq}.${segment}'"
	    if (ZFile.dsExists(pds)) {
	       if (props.verbose) println "** Deleting ${pds}"
	       ZFile.remove("//$pds")
	    }
	}
}
