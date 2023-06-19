@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import groovy.transform.*
import java.io.File
import com.ibm.dbb.*
import com.ibm.dbb.build.*
import java.util.regex.Pattern
import java.util.regex.Matcher
import com.ibm.dbb.build.report.BuildReport
import com.ibm.jzos.ZFile
import com.ibm.dbb.build.report.records.*

// properties instance
@Field BuildProperties props = BuildProperties.getInstance()

/*
 * testUtilities
 * 
 * - getBuildReportFromStream allows passing the outputstream and retrieve the location of the buildReport
 * - parseBuildReport parses the build report from file and returns the buildReport object
 * - buildReportIncludesOutput validating if a provided build report contains a provided output file
 * 
 */

/**
 * 
 * Extracts the path to the build report from the console log
 * 
 * @param outputStream
 * @return buildReportLocation
 *         <null> if not found
 * 
 */
def getBuildReportFromStream(StringBuffer outputStream) {

	def pattern = Pattern.compile("Writing build report data to (\\/.*.json)")
	// build matcher and find
	Matcher matcher = pattern.matcher(outputStream)

	// get build report path
	if(matcher.find()) {
		buildReportLocation = matcher.group(1)
		return buildReportLocation
	}
	println("!* The location of the Build report could not be found.")
	return null
}

/**
 * 
 * parses the build report from a provided String
 * 
 * @param buildReport
 * @return
 */

def parseBuildReport(String buildReport) {
	def buildReportFile = new File(buildReport)
	if (buildReportFile.exists()) {
		def parsedBuildReport = BuildReport.parse(new FileInputStream(buildReportFile))
		return parsedBuildReport
	}
	return null
}

/**
 * buildReportIncludesOutput validates if the provided member with deployType exist on the buildReport
 * 
 * @param buildReport
 * @param member
 * @param deployType
 * @return boolean - if member/deployType was found in build report
 * 
 */
boolean buildReportIncludesOutput(BuildReport buildReport, String member, String deployType) {

	def buildRecords = buildReport.getRecords().findAll{
		it.getType()==DefaultRecordFactory.TYPE_EXECUTE
	}

	def matchingRecord = buildRecords.find{ report ->
		report.getOutputs().find{ o ->
			o.deployType == deployType &&
					o.dataset.contains(member)
		}
	}

	if (matchingRecord != null) {
		return true
	} else {
		return false
	}
}


/**
 * updateFileAndCommit 
 *   is inserting a blank line at the end of the provided file
 *   and commits the file back to the repository
 * 
 * used in impact build test scenarios
 * 
 */

def updateFileAndCommit(String path, String changedFile) {
	println "** Updating and committing ${path}/${changedFile}"
	
	def commentStmt = ' '

	if (changedFile.endsWith("bms")) { // treat BMS files
		commentStmt = '* Updated file \n        END'
	}
	
	def commands = """
    echo -en \'${commentStmt}\' >> ${path}/${changedFile}
    cd ${path}/
    git add .
    git commit . -m "edited program file $changedFile"
"""
	def task = ['bash', '-c', commands].execute()
	def outputStream = new StringBuffer();
	task.waitForProcessOutput(outputStream, System.err)
}

/**
 * copyAndCommit 
 *  copies a reference file and commits it to the test branch
 * 
 * used in impact build test scenarios
 * 
 */
def copyAndCommit(String changedFile) {
	println "** Copying and committing ${props.zAppBuildDir}/test/applications/${props.app}/${changedFile} to ${props.appLocation}/${changedFile}"
	def commands = """
	cp ${props.zAppBuildDir}/test/applications/${props.app}/${changedFile} ${props.appLocation}/${changedFile}
	cd ${props.appLocation}/
	git add .
	git commit . -m "edited program file $changedFile"
"""
	def task = ['bash', '-c', commands].execute()
	def outputStream = new StringBuffer();
	task.waitForProcessOutput(outputStream, System.err)
}

/**
 * cleanUpDatasets
 *  deletes datasets 
 * 
 * accepts a comma separated list of last level qualifiers (llq)
 * iterates over the lists to delete them
 * 
 */
def cleanUpDatasets(String datasets) {
	def segments = datasets.split(',')
	
	println "\n** Deleting build PDSEs ${segments}"
	segments.each { segment ->
		def pds = "'${props.hlq}.${segment}'"
		if (ZFile.dsExists(pds)) {
		   if (props.verbose) println "** Deleting ${pds}"
		   ZFile.remove("//$pds")
		}
	}
}

/*
 * Create and checkout a local test branch for testing
 */
def createTestBranch() {
	println "** Creating and checking out branch ${props.testBranch}"
	def createTestBranch = """
    cd ${props.zAppBuildDir}
    git checkout ${props.branch}
    git checkout -b ${props.testBranch} ${props.branch}
    git status
"""
	def job = ['bash', '-c', createTestBranch].execute()
	job.waitFor()
	def createBranch = job.in.text
	println "$createBranch"
}

/*
 * Deletes test branch
 */
def deleteTestBranch() {
	println "\n** Deleting test branch ${props.testBranch}"
	def deleteTestBranch = """
    cd ${props.zAppBuildDir}
    rm -r out
    git reset --hard ${props.testBranch}
    git checkout ${props.branch}
    git branch -D ${props.testBranch}
    git status
"""
	def job = ['bash', '-c', deleteTestBranch].execute()
	job.waitFor()
	def deleteBranch = job.in.text
	println "$deleteBranch"
}

/*
 * Reset test branch 
 * to be used for any disruptive test scripts 
 */
def resetTestBranch() {
	println "\n** Resetting test branch ${props.testBranch}"
	deleteTestBranch()
	createTestBranch()
}


/**
 *  runBaselineBuild to initialize collections and build result history
 *
 *    to be used by any test script which uses impact analysis
 *
 *    input parms:
 *
 *    testScriptPropFiles are used to configure
 *    the test via additional properties files
 *    that are loaded via the --propFiles cli argument
 *
 */

/*
 * without arguments, it will not pass any test scripts specific configuration
 *  
 */

def runBaselineBuild() {
	runBaselineBuild(null)
}

/*
 * 
 */

def runBaselineBuild(String testScriptPropFiles) {

	println "\n** Running full build to set baseline"

	def dbbHome = EnvVars.getHome()
				
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
	if (props.propFiles && testScriptPropFiles) {
		fullBuildCommand << "--propFiles ${props.propFiles},${testScriptPropFiles}"
	} else if (props.propFiles) {
		fullBuildCommand << "--propFiles ${props.propFiles}"
	}
	fullBuildCommand << "--fullBuild"

	// run impact build
	println "** Executing ${fullBuildCommand.join(" ")}"
	def outputStream = new StringBuffer()
	def process = [
		'bash',
		'-c',
		fullBuildCommand.join(" ")
	].execute()
	process.waitForProcessOutput(outputStream, System.err)

}