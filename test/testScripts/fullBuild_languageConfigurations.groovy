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

// Create full build command
def userBuildCommand = []
userBuildCommand << "${dbbHome}/bin/groovyz"
userBuildCommand << "${props.zAppBuildDir}/build.groovy"
userBuildCommand << "--workspace ${props.workspace}"
userBuildCommand << "--application ${props.app}"
userBuildCommand << (props.outDir ? "--outDir ${props.outDir}" : "--outDir ${props.zAppBuildDir}/out")
userBuildCommand << "--hlq ${props.hlq}"
userBuildCommand << "--logEncoding UTF-8"
userBuildCommand << (props.url ? "--url ${props.url}" : "")
userBuildCommand << (props.id ? "--id ${props.id}" : "")
userBuildCommand << (props.pw ? "--pw ${props.pw}" : "") 
userBuildCommand << (props.pwFile ? "--pwFile ${props.pwFile}" : "")
userBuildCommand << (props.verbose ? "--verbose" : "")
userBuildCommand << (props.propFiles ? "--propFiles ${props.propFiles}" : "")
userBuildCommand << "--userBuild ${props.userBuild_languageConfigurations_buildFile}"

try {
	

	/** Test FullBuild **/

	// update language definitions files in workspace
	copyLanguageConfigurations(props.fullBuild_languageConfigurations_updatedLanguageConfigs_fullBuild)
		
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
	def expectedFilesBuiltList = props.fullBuild_languageConfigurations_expectedFilesBuilt_fullBuild.split(',')

	@Field def assertionList = []

	// Validate clean build
	assert outputStream.contains("Build State : CLEAN") : "*! FULL BUILD FAILED\nOUTPUT STREAM:\n$outputStream\n"

	// Validate expected number of files built
	def numFullFiles = expectedFilesBuiltList.size()
	assert outputStream.contains("Total files processed : ${numFullFiles}") : "*! TOTAL FILES PROCESSED ARE NOT EQUAL TO ${numFullFiles}\nOUTPUT STREAM:\n$outputStream\n"

	// Obtain property mapping of prints in the console for compile compileParms
	PropertyMappings compileParmsofFiles = new PropertyMappings("fullBuild_languageConfigurations_compileParms_fullBuild")
	// Iterate over build list
	expectedFilesBuiltList.each { file ->
		def expectedCompilerParms = compileParmsofFiles.getValue(file)
		if (expectedCompilerParms) assert outputStream.contains("${expectedCompilerParms}") : "*! Expected Compiler Parms (${expectedCompilerParms}) do not match for $file\nOUTPUT STREAM:\n$outputStream\n"
	}
		
	// Validate expected built files in output stream
	assert expectedFilesBuiltList.count{ i-> outputStream.contains(i) } == expectedFilesBuiltList.size() : "*! FILES PROCESSED IN THE FULL BUILD DOES NOT CONTAIN THE LIST OF FILES PASSED ${expectedFilesBuiltList}\nOUTPUT STREAM:\n$outputStream\n"
	
	// reset language configuration changes in workspace to cleanup
	resetLanguageConfigurationChanges()
	
	println "**"
	println "** FULL BUILD TEST Language Configurations: PASSED **"
	println "**"
	
	/** Test UserBuild overwride existing build property **/
	
	// update language definitions files in workspace
	copyLanguageConfigurations(props.fullBuild_languageConfigurations_updatedLanguageConfigs_fullBuild)
	
	// update file.propertes in workspace
	copyFileProperties(props.userBuild_languageConfigurations_fileProperties_TC1)
	
	// Run user build
	println "** Executing ${userBuildCommand.join(" ")}"
	process = [
		'bash',
		'-c',
		userBuildCommand.join(" ")
	].execute()
	outputStream = new StringBuffer();
	process.waitForProcessOutput(outputStream, System.err)
	
	assert outputStream.contains(props.userBuild_languageConfigurations_expected_message01_TC1) : "*! Message (${props.userBuild_languageConfigurations_expected_message01_TC1}) could not be found\nOUTPUT STREAM:\n$outputStream\n"
	assert outputStream.contains(props.userBuild_languageConfigurations_expected_message02_TC1) : "*! Message (${props.userBuild_languageConfigurations_expected_message02_TC1}) could not be found\nOUTPUT STREAM:\n$outputStream\n"
	
	// reset language configuration changes in workspace to cleanup
	resetLanguageConfigurationChanges()
	
	println "**"
	println "** USER BUILD TEST Language Configurations TEST 1 OVERRIDE FILE PROPERTY: PASSED **"
	println "**"
	
	/** Test UserBuild unable to override  existing build property **/ 
	
	// update language definitions files in workspace
	copyLanguageConfigurations(props.fullBuild_languageConfigurations_updatedLanguageConfigs_fullBuild)
	
	// update file.propertes in workspace
	copyFileProperties(props.userBuild_languageConfigurations_fileProperties_TC2)
	
	// Run user build
	println "** Executing ${userBuildCommand.join(" ")}"
	process = [
		'bash',
		'-c',
		userBuildCommand.join(" ")
	].execute()
	outputStream = new StringBuffer();
	process.waitForProcessOutput(outputStream, System.err)
	
	assert outputStream.contains(props.userBuild_languageConfigurations_expected_message01_TC2) : "*! Message (${props.userBuild_languageConfigurations_expected_message01_TC2}) could not be found\nOUTPUT STREAM:\n$outputStream\n"
	assert outputStream.contains(props.userBuild_languageConfigurations_expected_message02_TC2) : "*! Message (${props.userBuild_languageConfigurations_expected_message02_TC2}) could not be found\nOUTPUT STREAM:\n$outputStream\n"
	
	// reset language configuration changes in workspace to cleanup
	resetLanguageConfigurationChanges()
	
	println "**"
	println "** USER BUILD TEST Language Configurations TEST 2 with FAILING OVERRIDE FILE PROPERTY: PASSED **"
	println "**"
	
	
	
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
	println "**START OF FAILED TEST CASE for Language Configuration Overrides TEST RESULTS**\n"
	println "*FAILED TEST CASE for Language Configurations RESULTS*\n" + assertionList
	println "\n**END OF FAILED TEST CASE for Language Configurations **"
	println "***"
  }
  
  testUtils.cleanUpDatasets(props.fullBuild_languageConfigurations_datasetsToCleanUp)
  // reset language configuration changes
  resetLanguageConfigurationChanges()
	
}

// script end

//*************************************************************
// Method Definitions
//*************************************************************

def copyLanguageConfigurations(String languageConfigs) {
	def langDefs = languageConfigs.split(',')
	langDefs.each{ langDef ->
		copyBuildConfiguration(langDef.trim())
	}
}

def copyBuildConfiguration(String configFile) {
	println "** Copying ${props.zAppBuildDir}/test/applications/${props.app}/$configFile to ${props.zAppBuildDir}/"
	def commands = """
	cp ${props.zAppBuildDir}/test/applications/${props.app}/$configFile ${props.zAppBuildDir}/$configFile
"""
	def task = ['bash', '-c', commands].execute()
	def outputStream = new StringBuffer();
	task.waitForProcessOutput(outputStream, System.err)
}

def copyFileProperties(String configFile) {
	println "** Copying ${props.zAppBuildDir}/test/applications/${props.app}/$configFile to ${props.zAppBuildDir}/"
	def commands = """
	cp ${props.zAppBuildDir}/test/applications/${props.app}/$configFile ${props.appLocation}/application-conf/file.properties
"""
	def task = ['bash', '-c', commands].execute()
	def outputStream = new StringBuffer();
	task.waitForProcessOutput(outputStream, System.err)
}

def resetLanguageConfigurationChanges() {
	println "** Resetting language configuration changes" 
	def commands = """
	git -C ${props.appLocation} reset --hard
"""
	def task = ['bash', '-c', commands].execute()
	def outputStream = new StringBuffer();
	task.waitForProcessOutput(outputStream, System.err)
}

