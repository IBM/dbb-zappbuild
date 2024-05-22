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

// verify required build properties
buildUtils.assertBuildProperties(props.zcee2_requiredBuildProperties)

// create updated build map, removing duplicates in case of PROJECT input Type
HashMap<String, String> updatedBuildList = new HashMap<String, String>()

println("** Streamlining the build list to remove duplicates")
argMap.buildList.each { buildFile ->
    PropertyMappings inputTypeMappings = new PropertyMappings("zcee2_inputType")
    inputType = inputTypeMappings.getValue(buildFile)

    if (inputType) {
        if (inputType == "PROJECT") {
            File changedBuildFile = new File(buildFile);
            File projectDir = changedBuildFile.getParentFile()
            boolean projectDirFound = false
            while (projectDir != null && !projectDirFound) {
                File projectFile = new File(projectDir.getPath() + '/.project')
                if (projectFile.exists()) {
                    projectDirFound = true
                } else {
                    projectDir = projectDir.getParentFile()
                }
            }
            if (projectDirFound) {
                updatedBuildList.putIfAbsent(projectDir.getPath(), "PROJECT")
            } else {
                if (props.verbose) println("!* No project directory found for file '${buildFile}'. Skipping...")
            }
        } else {
            updatedBuildList.put(buildFile, inputType);
        }
    } else {
        println("!* No Input Type mapping for file ${buildFile}, skipping it...")
    }
}

println("** Building ${updatedBuildList.size()} API ${updatedBuildList.size() == 1 ? 'definition' : 'definitions'} mapped to ${this.class.getName()}.groovy script")
// sort the build list based on build file rank if provided
HashMap<String, String> sortedList = buildUtils.sortBuildListAsMap(updatedBuildList, 'zcee2_fileBuildRank')

int currentBuildFileNumber = 1

// iterate through build list
sortedList.each { buildFile, inputType ->
    println "*** (${currentBuildFileNumber++}/${sortedList.size()}) Building ${inputType == "PROJECT" ? 'project' : 'properties file'} $buildFile"

    String parameters = ""
    String outputDir = ""
    String outputFile = ""
    if (inputType == "PROJECT") {
        outputDir = "${props.buildOutDir}/zCEE2/$buildFile"
        parameters = "-od $outputDir -pd $buildFile"
    } else {
        File changedBuildFile = new File(buildFile);
        String outputFileName = changedBuildFile.getName().split("\\.")[0] + "." + inputType.toLowerCase()
        File projectDir = changedBuildFile.getParentFile()
        outputFile = "${props.buildOutDir}/zCEE2/${projectDir.getPath()}/${outputFileName}"
        outputDir = "${props.buildOutDir}/zCEE2/${projectDir.getPath()}"
        parameters = "-f $outputFile -p $buildFile"
    }
    File outputDirectory = new File(outputDir)
    outputDirectory.mkdirs()


    Properties ARAproperties = new Properties()
    File dataStructuresLocation
    File apiInfoFileLocation
    File logFileDirectory
    if (inputType == "ARA") {
        File ARApropertiesFile = new File(buildFile)
        ARApropertiesFile.withInputStream {
            ARAproperties.load(it)
        }
        println("*** dataStructuresLocation: ${ARAproperties.dataStructuresLocation}")
        println("*** apiInfoFileLocation: ${ARAproperties.apiInfoFileLocation}")
        println("*** logFileDirectory: ${ARAproperties.logFileDirectory}")
        dataStructuresLocation = new File(ARAproperties.dataStructuresLocation)
        dataStructuresLocation.mkdirs()
        apiInfoFileLocation = new File(ARAproperties.apiInfoFileLocation)
        apiInfoFileLocation.mkdirs()
        logFileDirectory = new File(ARAproperties.logFileDirectory)
        logFileDirectory.mkdirs()            
    }


    // log file - Changing slashes with dots to avoid conflicts
    String logFilePath = buildFile.replace("/", ".")
    File logFile = new File("${props.buildOutDir}/${logFilePath}.zCEE2.log")
    if (logFile.exists())
        logFile.delete()
    
    String zconbtPath = props.getFileProperty('zcee2_zconbtPath', buildFile)

    File zconbt = new File(zconbtPath)
    if (!zconbt.exists()) {
        def errorMsg = "*! zconbt wasn't find at location '$zconbtPath'" 
        println(errorMsg)
        props.error = "true"
        buildUtils.updateBuildResult(errorMsg:errorMsg)
    } else {
        String[] command;

        command = [zconbtPath, parameters]
        String commandString = command.join(" ")
        if (props.verbose)
            println("** Executing command '${commandString}'...")

        StringBuffer shellOutput = new StringBuffer()
        StringBuffer shellError = new StringBuffer()

        String javaHome = props.getFileProperty('zcee2_JAVA_HOME', buildFile)
        if (!javaHome) {
            javaHome = System.getenv("JAVA_HOME")
        }

        ProcessBuilder cmd = new ProcessBuilder(zconbtPath, parameters);
        Map<String, String> env = cmd.environment();
        env.put("JAVA_HOME", javaHome);
        env.put("PATH", javaHome + "/bin" + ":" + env.get("PATH"))
        Process process = cmd.start()
        process.consumeProcessOutput(shellOutput, shellError)
        process.waitFor()
        if (props.verbose)
            println("** Exit value for the zconbt process: ${process.exitValue()}");
            
        // write outputs to log file
        String enc = props.logEncoding ?: 'IBM-1047'
        logFile.withWriter(enc) { writer ->
            writer.append(shellOutput)
            writer.append(shellError)
        }
        
        if (process.exitValue() != 0) {
            def errorMsg = "*! Error during the zconbt process" 
            println(errorMsg)
            if (props.verbose)
                println("*! zconbt error message:\n${shellError}")
            props.error = "true"
            buildUtils.updateBuildResult(errorMsg:errorMsg)
        } else {
            if (props.verbose)
                println("** zconbt output:\n${shellOutput}")

            ArrayList<String> outputProperty = []
            Path outputDirectoryPath = Paths.get(props.buildOutDir)
            if (inputType == "PROJECT") {
                String[] outputFiles = outputDirectory.list()
                for (int i=0; i<outputFiles.length; i++) {
                    Path outputFilePath = Paths.get(outputDir + "/" + outputFiles[i])
                    Path relativeOutputFilePath = outputDirectoryPath.relativize(outputFilePath)
                    outputProperty.add("[${props.buildOutDir}, ${relativeOutputFilePath.toString()}, zCEE2]")
                }
            } else {
                Path outputFilePath = Paths.get(outputFile)
                Path relativeOutputFilePath = outputDirectoryPath.relativize(outputFilePath)
                outputProperty.add("[${props.buildOutDir}, ${relativeOutputFilePath.toString()}, zCEE2]")

                if (inputType == "ARA") {
                    def ARA_PackageArtifacts = props.getFileProperty('zcee2_ARA_PackageArtifacts', buildFile)
                    if (ARA_PackageArtifacts && ARA_PackageArtifacts.toBoolean()) {

                        String[] outputFiles
                        Path finalOutputFilePath

                        outputFiles = dataStructuresLocation.list()
                        File dataStructuresLocationDir = new File("${props.buildOutDir}/zCEE2/dataStructures")
                        dataStructuresLocationDir.mkdirs()
                        for (int i=0; i<outputFiles.length; i++) {
                            outputFilePath = Paths.get(dataStructuresLocation.getPath() + "/" + outputFiles[i])
                            finalOutputFilePath = Paths.get(dataStructuresLocationDir.getPath() + "/" + outputFiles[i])
                            Files.copy(outputFilePath, finalOutputFilePath, StandardCopyOption.COPY_ATTRIBUTES);
                            relativeOutputFilePath = outputDirectoryPath.relativize(finalOutputFilePath)
                            outputProperty.add("[${props.buildOutDir}, ${relativeOutputFilePath.toString()}, zCEE2-Copy]")
                        }
                        outputFiles = apiInfoFileLocation.list()
                        File apiInfoFileLocationDir = new File("${props.buildOutDir}/zCEE2/apiInfoFiles")
                        apiInfoFileLocationDir.mkdirs()
                        for (int i=0; i<outputFiles.length; i++) {
                            outputFilePath = Paths.get(apiInfoFileLocation.getPath() + "/" + outputFiles[i])
                            finalOutputFilePath = Paths.get(apiInfoFileLocationDir.getPath() + "/" + outputFiles[i])
                            Files.copy(outputFilePath, finalOutputFilePath, StandardCopyOption.COPY_ATTRIBUTES);
                            relativeOutputFilePath = outputDirectoryPath.relativize(finalOutputFilePath)
                            outputProperty.add("[${props.buildOutDir}, ${relativeOutputFilePath.toString()}, zCEE2-Info]")
                        }
                        outputFiles = logFileDirectory.list()
                        File logFileDirectoryDir = new File("${props.buildOutDir}/zCEE2/logs")
                        logFileDirectoryDir.mkdirs()
                        for (int i=0; i<outputFiles.length; i++) {
                            if (outputFiles[i].endsWith(".log")) {
                                outputFilePath = Paths.get(logFileDirectory.getPath() + "/" + outputFiles[i])
                                finalOutputFilePath = Paths.get(logFileDirectoryDir.getPath() + "/" + outputFiles[i])
                                Files.copy(outputFilePath, finalOutputFilePath, StandardCopyOption.COPY_ATTRIBUTES);
                                relativeOutputFilePath = outputDirectoryPath.relativize(finalOutputFilePath)
                                outputProperty.add("[${props.buildOutDir}, ${relativeOutputFilePath.toString()}, zCEE2-Log]")
                            }
                        }
                    }
                }
            }
            AnyTypeRecord zCEEWARRecord = new AnyTypeRecord("USS_RECORD")
            zCEEWARRecord.setAttribute("file", buildFile)
            zCEEWARRecord.setAttribute("label", "z/OS Connect EE OpenAPI 2 definition")
            zCEEWARRecord.setAttribute("outputs", outputProperty.join(";"))
            zCEEWARRecord.setAttribute("command", commandString);
            BuildReportFactory.getBuildReport().addRecord(zCEEWARRecord)
        }
    }
}