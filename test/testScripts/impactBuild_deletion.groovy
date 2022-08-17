
@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import groovy.transform.*
import com.ibm.dbb.*
import com.ibm.dbb.build.*
import com.ibm.jzos.ZFile

@Field BuildProperties props = BuildProperties.getInstance()
println "\n** Executing test script impactBuild_deletion.groovy"

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
impactBuildCommand << "--url ${props.url}"
impactBuildCommand << "--id ${props.id}"
impactBuildCommand << (props.pw ? "--pw ${props.pw}" : "--pwFile ${props.pwFile}")
impactBuildCommand << "--verbose"
impactBuildCommand << (props.propFiles ? "--propFiles ${props.propFiles},${props.zAppBuildDir}/test/applications/${props.app}/${props.impactBuild_deletion_buildPropSetting}" : "")
impactBuildCommand << "--impactBuild"

// iterate through change files to test impact build
@Field def assertionList = []

PropertyMappings outputsDeletedMappings = new PropertyMappings('impactBuild_deletion_deletedOutputs')


def deleteFiles = props.impactBuild_deletion_deleteFiles.split(',')
try {
	deleteFiles.each{ deleteFile ->
		
		// delete file in Git repo test branch
		deleteAndCommit(deleteFile)

		println "\n** Running impact after deleting file $deleteFile"
				
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
		validateImpactBuild(deleteFile, outputsDeletedMappings, outputStream)
	}
}
finally {
	cleanUpDatasets()
	if (assertionList.size()>0) {
		println "\n***"
		println "**START OF FAILED IMPACT BUILD TEST RESULTS**\n"
		println "*FAILED IMPACT BUILD TEST RESULTS*\n" + assertionList
		println "\n**END OF FAILED IMPACT BUILD TEST RESULTS**"
		println "***"
	}
}
// script end

//*************************************************************
// Method Definitions
//*************************************************************

def deleteAndCommit(String deleteFile) {
	println "** Delete $deleteFile"
	def commands = """
	rm ${props.appLocation}/${deleteFile}
	git -C ${props.appLocation} add .
	git -C ${props.appLocation} commit . -m "delete file"
"""
	def task = ['bash', '-c', commands].execute()
	def outputStream = new StringBuffer();
	task.waitForProcessOutput(outputStream, System.err)
}

def validateImpactBuild(String deleteFile, PropertyMappings outputsDeletedMappings, StringBuffer outputStream) {

	println "** Validating impact build results"
	def expectedDeletedFilesList = filesBuiltMappings.getValue(deleteFile).split(',')
	
	try{
		def memberName = CopyToPDS.createMemberName(deleteFile)
		
		
		// Validate clean build
		assert outputStream.contains("Build State : CLEAN") : "*! IMPACT BUILD FAILED FOR $deleteFile\nOUTPUT STREAM:\n$outputStream\n"

		// Validate message that deleted file was deleted from collections
		assert outputStream.contains("*** Deleting renamed logical file for ${props.app}/${deleteFile}") : "*! IMPACT BUILD FOR $deleteFile DO NOT FIND DELETION OF LOGICAL FILE\nOUTPUT STREAM:\n$outputStream\n"
		
		// Validate creation of the Delete Record 
		assert outputStream.contains("** Create deletion record for file") : "*! IMPACT BUILD FOR $deleteFile DO NOT FIND CREATION OF DELETE RECORD\nOUTPUT STREAM:\n$outputStream\n"
		
		expectedDeletedFilesList.each { deletedOutput ->

			assert outputStream.contains("** Document deletion ${props.hlq}.${deletedOutput} for file") : "*! IMPACT BUILD FOR $deleteFile DO NOT FIND CREATION OF DELETE RECORD\nOUTPUT STREAM:\n$outputStream\n"

			// Validate deletion of output
			assert outputStream.contains("** Deleting ${props.hlq}.${deletedOutput}" : "*! IMPACT BUILD FOR $deleteFile DO NOT FIND DELETION OF LOAD MODULE\nOUTPUT STREAM:\n$outputStream\n"

		}
		println "**"
		println "** IMPACT BUILD TEST - FILE DELETE : PASSED FOR DELETING $deleteFile **"
		println "**"
	}
	catch(AssertionError e) {
		def result = e.getMessage()
		assertionList << result;
	}
}
def cleanUpDatasets() {
	def segments = props.impactBuild_deletion_datasetsToCleanUp.split(',')

	println "Deleting impact build PDSEs ${segments}"
	segments.each { segment ->
		def pds = "'${props.hlq}.${segment}'"
		if (ZFile.dsExists(pds)) {
			if (props.verbose) println "** Deleting ${pds}"
			ZFile.remove("//$pds")
		}
	}
}
