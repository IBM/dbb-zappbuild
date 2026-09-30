
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
impactBuildCommand << "--verbose"
impactBuildCommand << (props.propFiles ? "--propFiles ${props.propFiles},${props.zAppBuildDir}/test/applications/${props.app}/${props.impactBuild_renameWithChange_buildPropSetting}" : "--propFiles ${props.zAppBuildDir}/test/applications/${props.app}/${props.impactBuild_renameWithChange_buildPropSetting}")
impactBuildCommand << "--impactBuild"

@Field def assertionList = []

def renameFile  = props.impactBuild_renameWithChange_renameFile
def newFilename = props.impactBuild_renameWithChange_newFilename

try {

	// Create full build to set baseline
	testUtils.runBaselineBuild()

	// TC2: rename + content change
	// Step 1 - rename the file (git mv)
	renameAndCommit(renameFile, newFilename)

	// Step 2 - modify the new file with a blank line so git sees content change
	//           (lowers similarity score below 100, producing R<score> instead of R100)
	testUtils.updateFileAndCommit(props.appLocation, newFilename)

	println "\n** Running impact build after renaming $renameFile to $newFilename with content change"

	// run impact build
	println "** Executing ${impactBuildCommand.join(" ")}"
	def outputStream = new StringBuffer()
	def process = [
		'bash',
		'-c',
		impactBuildCommand.join(" ")
	].execute()
	process.waitForProcessOutput(outputStream, System.err)

	// validate build results
	validateImpactBuild(renameFile, newFilename, outputStream)
}
finally {
	// report failures
	if (assertionList.size() > 0) {
		println "\n***"
		println "**START OF FAILED IMPACT BUILD TEST RESULTS**\n"
		println "*FAILED IMPACT BUILD TEST RESULTS*\n" + assertionList
		println "\n**END OF FAILED IMPACT BUILD TEST RESULTS**"
		println "***"
	}

	// reset test branch
	testUtils.resetTestBranch()

	// cleanup datasets
	testUtils.cleanUpDatasets(props.impactBuild_renameWithChange_datasetsToCleanUp)
}
// script end

//*************************************************************
// Method Definitions
//*************************************************************

def renameAndCommit(String renameFile, String newFilename) {
	println "** Rename $renameFile to $newFilename"
	def commands = """
	mv ${props.appLocation}/${renameFile} ${props.appLocation}/${newFilename}
	git -C ${props.appLocation} add .
	git -C ${props.appLocation} commit . -m "renamed program file"
"""
	def task = ['bash', '-c', commands].execute()
	def outputStream = new StringBuffer()
	task.waitForProcessOutput(outputStream, System.err)
}

def validateImpactBuild(String renameFile, String newFilename, StringBuffer outputStream) {

	def expectedFilesBuilt = props.impactBuild_renameWithChange_expectedFilesBuilt.split(',')

	try {
		println "** Validating impact build results"

		// Validate clean build
		assert outputStream.contains("Build State : CLEAN") : "*! IMPACT BUILD FAILED FOR rename-with-change of $renameFile\nOUTPUT STREAM:\n$outputStream\n"

		// Validate expected number of files built (the renamed+changed file itself)
		assert outputStream.contains("Total files processed : ${expectedFilesBuilt.size()}") : "*! IMPACT BUILD FOR rename-with-change of $renameFile TOTAL FILES PROCESSED ARE NOT EQUAL TO ${expectedFilesBuilt.size()}\nOUTPUT STREAM:\n$outputStream\n"

		// Validate all expected files appear in the output
		assert expectedFilesBuilt.count { f -> outputStream.contains(f) } == expectedFilesBuilt.size() : "*! IMPACT BUILD FOR rename-with-change of $renameFile DOES NOT CONTAIN EXPECTED BUILT FILES ${expectedFilesBuilt}\nOUTPUT STREAM:\n$outputStream\n"

		// Validate the new file was scanned (similarity < 100 → full rescan of changed content)
		assert outputStream.contains("*** Scanning file ${props.app}/${newFilename}") : "*! IMPACT BUILD FOR rename-with-change of $renameFile DID NOT SCAN NEW FILE ${newFilename}\nOUTPUT STREAM:\n$outputStream\n"

		// Validate the old logical file entry was removed via the rename map (not a plain delete)
		assert outputStream.contains("*** Updating logical file for renamed file ${props.app}/${renameFile} -> ${props.app}/${newFilename}") : "*! IMPACT BUILD FOR rename-with-change of $renameFile DID NOT UPDATE LOGICAL FILE IN COLLECTION\nOUTPUT STREAM:\n$outputStream\n"

		println "**"
		println "** IMPACT BUILD TEST - FILE RENAME WITH CHANGE : PASSED FOR RENAMING $renameFile TO $newFilename **"
		println "**"
	}
	catch (AssertionError e) {
		assertionList << e.getMessage()
		props.testsSucceeded = 'false'
	}
}
