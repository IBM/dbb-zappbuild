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
    
    // log file - Changing slashes with dots to avoid conflicts
    String member = buildFile.replace("/", ".")
    File logFile = new File("${props.buildOutDir}/${member}.zCEE3.log")
    if (logFile.exists())
        logFile.delete()
    
    String gradlePath = props.zcee3_gradlePath

    if (fileExists(gradlePath)) {
        String shellEnvironment = props.zcee3_shellEnvironment
        String encoding = props.logEncoding ?: 'IBM-1047'
        ArrayList<String> optionsList = new ArrayList<String>()
        optionsList.add(gradlePath)
        optionsList.add(gradleBuildLocation)
        if (props.zcee3_gradle_debug && props.zcee3_gradle_debug.toBoolean())
            optionsList.add("--debug")

        if (props.verbose)
            println("*** Executing command '${shellEnvironment}' with options '${optionsList}'")

        UnixExec zCEE3Execution = new UnixExec().command(shellEnvironment)
        zCEE3Execution.setOptions(optionsList)
        zCEE3Execution.output(logFile.getAbsolutePath()).mergeErrors(true);
        zCEE3Execution.setWorkingDirectory(workingDir)
        zCEE3Execution.setFile(buildFile)
        zCEE3Execution.setOutputEncoding(encoding)
        zCEE3Execution.addOutput(props.buildOutDir, "zCEE3/$WarLocation", "zCEE3")
        int returnCode = zCEE3Execution.execute()

        if (returnCode != 0) {
            String errorMsg = "*! Error during the gradle process. Please check the gradle log file at '${logFile.getAbsolutePath()}'."
            println(errorMsg)
            props.error = "true"
            buildUtils.updateBuildResult(errorMsg:errorMsg)
        } else {
            if (WarFile.exists()) {
                // Copy api.war to the buildOutDir directory
                File WarFileTarget = new File(props.buildOutDir + '/zCEE3/' + WarLocation);
                File WarTargetDir = WarFileTarget.getParentFile();
                WarTargetDir.mkdirs();
                Files.copy(WarFile.toPath(), WarFileTarget.toPath(), StandardCopyOption.COPY_ATTRIBUTES);            
            } else {
                String errorMsg = "*! Error when searching for the 'api.war' file at location '${WarLocation}'"
                println(errorMsg)
                props.error = "true"
                buildUtils.updateBuildResult(errorMsg:errorMsg)
            }   
        }
    }
}

def fileExists(String fileLoc){
    File file = new File(fileLoc)
    if (!file.exists()) {
        String errorMsg = "*! z/OS Connect EE OpenAPI 3 process - $fileLoc not found."
        println(errorMsg)
        props.error = "true"
        buildUtils.updateBuildResult(errorMsg:errorMsg)
        return false
    } else {
        return true
    }
}
