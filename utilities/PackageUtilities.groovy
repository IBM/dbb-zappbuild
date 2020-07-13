@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import java.io.File
import com.ibm.dbb.build.*
import com.ibm.dbb.build.report.*
import groovy.transform.*
import com.ibm.dbb.build.DBBConstants.CopyMode
import groovy.json.JsonSlurper

/****************************************************************
 * This script package the application in tar with its app.yaml *
 *                                                              *
 ***************************************************************/

//define script props
@Field BuildProperties props = BuildProperties.getInstance()
@Field def gitUtils= loadScript(new File("GitUtilities.groovy"))
@Field def buildUtils= loadScript(new File("BuildUtilities.groovy"))

def createApplicationPackage () {
	
	def workDir = props.buildOutDir
	def memberCount = 0
	def tarFileName = "$workDir/${props.appName}-${props.appVersion}.tar"
	
	println "** Package the application to $tarFileName"

	def buildFolder = buildUtils.getAbsolutePath(props.zAppBuildDir)
	def sourceFolder = buildUtils.getAbsolutePath(props.application)
	
	def gitBuildBranch = gitUtils.isGitDetachedHEAD(buildFolder) ? 
			gitUtils.getCurrentGitDetachedBranch(buildFolder) : gitUtils.getCurrentGitBranch(buildFolder)
	def gitSourceBranch = gitUtils.isGitDetachedHEAD(sourceFolder) ?
			gitUtils.getCurrentGitDetachedBranch(sourceFolder) : gitUtils.getCurrentGitBranch(sourceFolder)
	
	def gitHash = gitUtils.getCurrentGitHash(sourceFolder)
	
	def gitBuildUrl = gitUtils.getCurrentGitUrl(buildFolder)
	def gitSourceUrl = gitUtils.getCurrentGitUrl(sourceFolder)
	

	//Retrieve the build report and parse the outputs from the build report
	def buildReportFile = new File("$workDir/BuildReport.json")
	assert buildReportFile.exists(), "$buildReportFile does not exist"

	def jsonSlurper = new JsonSlurper()
	def parsedReport = jsonSlurper.parseText(buildReportFile.getText("UTF-8"))
	def outputUnitFragments = [:]
	def outputIntermediateFragments = [:]

	// For each load module, use CopyToHFS with respective CopyMode option to maintain SSI
	def copy = new CopyToHFS()
	def copyModeMap = ["COPYBOOK": CopyMode.TEXT, "DBRM": CopyMode.BINARY, "LOAD": CopyMode.LOAD, "CICS_LOAD": CopyMode.LOAD, "IMS_LOAD": CopyMode.LOAD]


	//Create a temporary directory on zFS to copy the load modules from data sets to
	def tempLoadDir = new File("$workDir/tempLoadDir")
	!tempLoadDir.exists() ?: tempLoadDir.deleteDir()
	tempLoadDir.mkdirs()

	parsedReport.records.each { record ->	
		if (record.outputs != null) {
			record.outputs.each { output ->
				if (output.dataset != null && record.file != null) {
					def (dataset, member) = output.dataset.split("\\(|\\)")
					if (output.deployType != null  ) {
						currentCopyMode = copyModeMap[dataset.replaceAll(/.*\.([^.]*)/, "\$1")]
						def key = "${dataset}#${output.deployType}";
						if ( outputUnitFragments[key] == null )
							outputUnitFragments[key] = "";
						outputUnitFragments[key] +=
								"      - name:           $member\n" +
								"        # NOTES - This can be the unique id of a load module or hash of a text\n" +
								"        hash:      "+Integer.toString(member.hashCode()).replace("-", "")+"\n"+
								"        sourceLocation:\n" +
								"          <<:           *gitHubGenAppSource\n" +
								"          path:         ${record.file}\n" +
								"          commitID:     ${gitHash}\n" +
								"        buildScriptLocation:\n" +
								"          <<:           *gitHubGenAppBuild\n\n"
					} else {
						if ( ! dataset.startsWith (props.hlq) ) {
							return;
						}
						def suffix = dataset.substring(props.hlq.length()+1)
						if ( suffix.contains("COPY"))
							currentCopyMode = CopyMode.TEXT
						else
							currentCopyMode = CopyMode.BINARY
						def key = "${dataset}#${suffix}";
						if ( outputIntermediateFragments[key] == null )
							outputIntermediateFragments[key] = "";
						outputIntermediateFragments[key] +=
								"      - name:           $member\n\n"
					}
					
					// Copy the member
					datasetDir = new File("$tempLoadDir/$dataset")
					datasetDir.mkdirs()
					
					copy.setCopyMode(currentCopyMode)
					copy.setDataset(dataset)
					
					copy.member(member).file(new File("$datasetDir/$member")).copy()
					memberCount++
					
				}
			}
		}
	}

	if ( memberCount == 0 ) {
		if (props.verbose)
			println "** There are no load module to publish"
		return
	}
	
	//Create the application definition file.
	def appYamlWriter = new File("$tempLoadDir/app.yaml")

	//Set up the artifactory information to publish the tar file
	def versionLabel = "${props.startTime}"  as String

	def tarFile = new File(tarFileName)
	def remotePath = "${props.appVersion}/${gitSourceBranch}/${props.buildNumber}/${tarFile.name}"

	if (props.verbose)
		println "Binary repository URL: ${props.repoUrl}/$remotePath"

	appYamlWriter.withWriter("UTF-8") { writer ->
		writer.writeLine("name: ${props.appName}")
		writer.writeLine("version: ${props.appVersion}")
		writer.writeLine("creationTimestamp: \"${versionLabel}\"")

		writer.writeLine("package: ${props.repoUrl}/$remotePath")

		writer.writeLine("packageType: partial")

		writer.writeLine("sources:")
		writer.writeLine("  - id:                 &gitHubGenAppSource")
		writer.writeLine("      type:               git")
		writer.writeLine("      branch:             ${gitSourceBranch}")
		writer.writeLine("      uri:                ${gitSourceUrl}")

		writer.writeLine("  - id:                 &gitHubGenAppBuild")
		writer.writeLine("      type:               git")
		writer.writeLine("      branch:             ${gitBuildBranch}")
		writer.writeLine("      uri:                ${gitBuildUrl}")

		writer.writeLine("\nintermediateUnits:")
		outputIntermediateFragments.each { key , fragment ->
			def (dataset, objectType) = key.split("#")
			writer.writeLine (
					" - originPDS:          $dataset\n" +
					"   type:               PDSE\n" +
					"   objectType:         $objectType\n" +
					"   folder:             $dataset\n"+
					"   resources:")
			writer.writeLine(fragment)
		}
		
		writer.writeLine("deploymentUnits:")
		outputUnitFragments.each { key , fragment ->
			def (dataset, deployType) = key.split("#")
			writer.writeLine (
					" - originPDS:          $dataset\n" +
					"   type:               PDSE\n" +
					"   deployType:         $deployType\n" +
					"   folder:             $dataset\n"+
					"   resources:")
			writer.writeLine(fragment)
		}
	}

	println "** Number of load modules to publish: $memberCount"

	//Package the load files just copied into a tar file using the build
	//label as the name for the tar file.
	StringBuffer out = new StringBuffer()
	StringBuffer err = new StringBuffer()
	
	// def process = "tar -cvf $tarFile *".execute(null, tempLoadDir)
	Process process = ["sh", "-c", "tar cf $tarFile *"].execute([], tempLoadDir)
	process.consumeProcessOutput(out, err)

	def rc = process.waitFor()

	if (rc != 0 || err) {
		println("** Error archiving the application $tarFile.")
		println "ERR: $err"
		System.exit(1)
	}
}