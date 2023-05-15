@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import java.io.File
import com.ibm.dbb.build.*
import java.util.regex.Pattern
import java.util.regex.Matcher
import com.ibm.dbb.build.report.BuildReport
import com.ibm.dbb.build.report.records.*

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