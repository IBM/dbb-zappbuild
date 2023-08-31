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
buildUtils.assertBuildProperties(props.zcee3_requiredBuildProperties)

// sort the build list based on build file rank if provided
List<String> sortedList = buildUtils.sortBuildList(argMap.buildList.sort(), 'zcee3_fileBuildRank')
int currentBuildFileNumber = 1

// iterate through build list
sortedList.each { buildFile ->
    println "*** (${currentBuildFileNumber++}/${sortedList.size()}) Building file $buildFile"
    
    String workingDir = buildFile.replace("/src/main/api/openapi.yaml", "")
    String gradleBuildLocation = "build"
    if (props.verbose) 
        println("** Path to build.gradle: ${workingDir}/${gradleBuildLocation}")
    String WarLocation = "${workingDir}/build/libs/api.war"
    if (props.verbose) 
        println("** Expected location of the 'api.war' file: ${WarLocation}")
    File WarFile = new File(WarLocation)
    String[] command;
    String commandString;
    
    // log file - Changing slashes with dots to avoid conflicts
    String member = buildFile.replace("/", ".")
    File logFile = new File("${props.buildOutDir}/${member}.zCEE3.log")
    if (logFile.exists())
        logFile.delete()
    
    String JAVA_OPTS = props.getFileProperty('zcee3_gradle_JAVA_OPTS', buildFile)
    String gradlePath = props.getFileProperty('zcee3_gradlePath', buildFile)

    File gradleExecutable = new File(gradlePath)
    if (!gradleExecutable.exists()) {
        def errorMsg = "*! gradle wasn't find at location '$gradlePath'" 
        println(errorMsg)
        props.error = "true"
        buildUtils.updateBuildResult(errorMsg:errorMsg)
    } else {
        String shellEnvironment = props.getFileProperty('zcee3_shellEnvironment', buildFile)

        command = [shellEnvironment, gradlePath, gradleBuildLocation]
        commandString = command.join(" ") 
        if (props.verbose)
            println("** Executing command '${commandString}' in working directory '${workingDir}'...")
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
            
        // write outputs to log file
        String enc = props.logEncoding ?: 'IBM-1047'
        logFile.withWriter(enc) { writer ->
            writer.append(shellOutput)
            writer.append(shellError)
        }
        
        if (process.exitValue() != 0) {
            def errorMsg = "*! Error during the gradle process" 
            println(errorMsg)
            if (props.verbose)
                println("*! gradle error message:\n${shellError}")
            props.error = "true"
            buildUtils.updateBuildResult(errorMsg:errorMsg)
        } else {
            if (props.verbose)
                println("** gradle output:\n${shellOutput}")
            if (WarFile.exists()) {
                // Copy api.war to the buildOutDir directory
                File WarFileTarget = new File(props.buildOutDir + '/zCEE3/' + WarLocation);
                File WarTargetDir = WarFileTarget.getParentFile();
                WarTargetDir.mkdirs();
                Files.copy(WarFile.toPath(), WarFileTarget.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            
                AnyTypeRecord zCEEWARRecord = new AnyTypeRecord("USS_RECORD")
                zCEEWARRecord.setAttribute("file", buildFile)
                zCEEWARRecord.setAttribute("label", "z/OS Connect EE OpenAPI 3 YAML definition")
                zCEEWARRecord.setAttribute("outputs", "[${props.buildOutDir}, zCEE3/$WarLocation, zCEE3]")
                zCEEWARRecord.setAttribute("command", commandString);
                BuildReportFactory.getBuildReport().addRecord(zCEEWARRecord)
            } else {
                def errorMsg = "*! Error when searching for the 'api.war' file at location '${WarLocation}'"
                println(errorMsg)
                props.error = "true"
                buildUtils.updateBuildResult(errorMsg:errorMsg)
            }   
        }
    }
}
