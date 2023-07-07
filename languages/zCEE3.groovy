@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*
import com.ibm.dbb.build.report.*
import com.ibm.dbb.build.report.records.*
import java.nio.file.*;


// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def buildUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BuildUtilities.groovy"))
	
println("** Building ${argMap.buildList.size()} ${argMap.buildList.size() == 1 ? 'file' : 'files'} mapped to ${this.class.getName()}.groovy script")

// verify required build properties
buildUtils.assertBuildProperties(props.zCEE_requiredBuildProperties)

// sort the build list based on build file rank if provided
List<String> sortedList = buildUtils.sortBuildList(argMap.buildList.sort(), 'zCEE_fileBuildRank')
int currentBuildFileNumber = 1

// iterate through build list
sortedList.each { buildFile ->
	println "*** (${currentBuildFileNumber++}/${sortedList.size()}) Building file $buildFile"
	
	boolean alwaysRebuildWAR = props.getFileProperty('zCEE_alwaysRebuildWAR', buildFile).toBoolean()
	
	String workingDir = buildFile.replace("/src/main/api/openapi.yaml", "")
	String gradleBuildLocation = "build"
	if (props.verbose) 
		println("** Path to build.gradle: ${workingDir}/${gradleBuildLocation}")
	String WarLocation = "${workingDir}/build/libs/api.war"
	if (props.verbose) 
		println("** Expected path to api.war: ${WarLocation}")
	File WarFile = new File(WarLocation)
	String[] command;
	String commandString;
	
	if (alwaysRebuildWAR || !WarFile.exists()) {
		println("** File api.war doesn't exist for this z/OS Connect project. Building it...")
		String JAVA_OPTS = props.getFileProperty('zCEE_gradle_JAVA_OPTS', buildFile)
		String gradlePath = props.getFileProperty('zCEE_gradlePath', buildFile)
		String shellEnvironment = props.getFileProperty('zCEE_shellEnvironment', buildFile)
	
		command = [shellEnvironment, gradlePath, gradleBuildLocation]
		commandString = command.join(" ") 
		if (props.verbose)
			println("** Command to execute: ${commandString} - in working directory: ${workingDir}")
		StringBuffer shellOutput = new StringBuffer()
		StringBuffer shellError = new StringBuffer()
	
		ProcessBuilder cmd = new ProcessBuilder(shellEnvironment, gradlePath, gradleBuildLocation);
		Map<String, String> env = cmd.environment();
		env.put("JAVA_OPTS", JAVA_OPTS);
		cmd.directory(new File(workingDir));
		Process process = cmd.start()
		process.consumeProcessOutput(shellOutput, shellError)
		process.waitFor()
		if (props.verbose)
			println("** Exit value for the gradle build: ${process.exitValue()}");
		
		if (process.exitValue() != 0) {
			def errorMsg = "Error during the gradle process" 
			println("*! ${errorMsg}")
			if (props.verbose)
				println("*! gradle error message:\n${shellError}")
			props.error = "true"
			buildUtils.updateBuildResult(errorMsg:errorMsg)
			System.exit(process.exitValue())
		} else {
			if (props.verbose)
				println("** gradle output:\n${shellOutput}")
		}
	}
	if (WarFile.exists()) {
		// Copy api.war to the outDir directory
		File WarFileTarget = new File(props.outDir + '/' + WarLocation);
		File WarTargetDir = WarFileTarget.getParentFile();
		WarTargetDir.mkdirs();
		Files.copy(WarFile.toPath(), WarFileTarget.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
	
		AnyTypeRecord zCEEWARRecord = new AnyTypeRecord("USS_RECORD")
		zCEEWARRecord.setAttribute("file", buildFile)
		zCEEWARRecord.setAttribute("label", "z/OS Connect EE OpenAPI 3 YAML definition")
		zCEEWARRecord.setAttribute("outputs", "[${props.outDir}/${WarLocation}, zCEE3]")
		zCEEWARRecord.setAttribute("command", commandString);
		BuildReportFactory.getBuildReport().addRecord(zCEEWARRecord)
	} else {
		def errorMsg = "Error when locating the api.war file" 
		println("*! ${errorMsg}")
		if (props.verbose)
			println("*! zCEE OpenAPI 3 error:\n${shellError}")
		props.error = "true"
		buildUtils.updateBuildResult(errorMsg:errorMsg)
		System.exit(process.exitValue())
	}	
}