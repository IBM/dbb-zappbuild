@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*
import com.ibm.dbb.build.report.*
import com.ibm.dbb.build.report.records.*
import java.nio.file.*
import groovy.io.FileType

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
    String inputType = inputTypeMappings.getValue(buildFile)
    if (!inputType || inputType.isEmpty()) {
        inputType = props.zcee2_inputType
        println("*! [WARNING] No Input Type mapping for file ${buildFile}, using default type '${inputType}'")
    }
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
}

println("** Building ${updatedBuildList.size()} API ${updatedBuildList.size() == 1 ? 'definition' : 'definitions'} mapped to ${this.class.getName()}.groovy script")
// sort the build list based on build file rank if provided
HashMap<String, String> sortedList = buildUtils.sortBuildListAsMap(updatedBuildList, 'zcee2_fileBuildRank')

int currentBuildFileNumber = 1

// iterate through build list
sortedList.each { buildFile, inputType ->
    println "*** (${currentBuildFileNumber++}/${sortedList.size()}) Building ${inputType == "PROJECT" ? 'project' : 'properties file'} $buildFile"

    String parameters = ""
    String outputFile = ""
    String deployType
    if (inputType == "PROJECT") {
        String outputFileName = Paths.get("zCEE2/$buildFile").getFileName()
        // test if package.xml exsist to determine if it's an API project or a SAR project
        File packageXMLFile = new File("${props.workspace}/$buildFile/package.xml")
        if (packageXMLFile.exists()) {
            // It's an API project (AAR)
            outputFile = "zCEE2/$buildFile/${outputFileName}.aar"
            deployType = "zCEE2-AAR"
        } else {
            // It's a Service project (SAR)
            outputFile = "zCEE2/$buildFile/${outputFileName}.sar"
            deployType = "zCEE2-SAR"
        }
        parameters = "-f ${props.buildOutDir}/$outputFile -pd $buildFile"
    } else {
        File changedBuildFile = new File(buildFile);
        String outputFileName = changedBuildFile.getName().split("\\.")[0] + "." + inputType.toLowerCase()
        File projectDir = changedBuildFile.getParentFile()
        outputFile = "zCEE2/${projectDir.getPath()}/${outputFileName}"
        deployType = "zCEE2-$inputType"
        parameters = "-f ${props.buildOutDir}/$outputFile -p $buildFile"
    }
    Path outputDirPath = Paths.get("${props.buildOutDir}/${outputFile}").getParent()
    File outputDir = outputDirPath.toFile()
    outputDir.mkdirs()

    Properties ARAproperties = new Properties()
    File dataStructuresLocation
    File apiInfoFileLocation
    File logFileDirectory
    if (inputType == "ARA") {
        File ARApropertiesFile = new File(buildFile)
        ARApropertiesFile.withInputStream {
            ARAproperties.load(it)
        }
        if (props.verbose) {
            println("*** dataStructuresLocation: ${ARAproperties.dataStructuresLocation}")
            println("*** apiInfoFileLocation: ${ARAproperties.apiInfoFileLocation}")
            println("*** logFileDirectory: ${ARAproperties.logFileDirectory}")
        }
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

    String zconbtPath = props.zcee2_zconbtPath

    if (buildUtils.fileExists(zconbtPath, "z/OS Connect EE OpenAPI 2 processing")) {
        String encoding = props.logEncoding ?: 'IBM-1047'
        ArrayList<String> optionsList = new ArrayList<String>(Arrays.asList(parameters))
        if (props.verbose)
            println("*** Executing command '${zconbtPath}' with options '${optionsList}'")

        UnixExec zCEE2Execution = new UnixExec().command(zconbtPath)
        zCEE2Execution.setOptions(optionsList)
        zCEE2Execution.output(logFile.getAbsolutePath()).mergeErrors(true)
        zCEE2Execution.setFile(buildFile)
        zCEE2Execution.setOutputEncoding(encoding)

        zCEE2Execution.addOutput(props.buildOutDir, outputFile, deployType)
        int returnCode = zCEE2Execution.execute()
    
        if (inputType == "ARA") {
            def ARA_packageArtifacts = props.getFileProperty('zcee2_ARA_packageArtifacts', buildFile)
            if (ARA_packageArtifacts && ARA_packageArtifacts.toBoolean()) {
                // if building an API Requester project, we package the generated artifacts
                // if told to do so

                def unixRecords = BuildReportFactory.getBuildReport().getRecords().findAll() { record ->
                    record.getType().equals("UNIX")
                }
                if (unixRecords) {
                    def unixRecord = unixRecords.find() { record ->
                        record.getFile().equals(buildFile)
                    }
                    if (unixRecord) {
                        // Captures the generated data structures
                        File targetDataStructuresLocationDir = new File("${props.buildOutDir}/zCEE2/${buildFile.replace(".properties", "")}/dataStructures")
                        targetDataStructuresLocationDir.mkdirs()
                        dataStructuresLocation.eachFile(FileType.FILES) { file ->
                            Path targetFile = Paths.get(targetDataStructuresLocationDir.getPath() + "/" + file.getName())
                            Files.copy(file.toPath(), targetFile, StandardCopyOption.COPY_ATTRIBUTES)
                            Path relativeOutputFilePath = Paths.get(props.buildOutDir).relativize(targetFile)
                            unixRecord.addOutput(props.buildOutDir, relativeOutputFilePath.toString(), "zCEE2-Copy")
                        }
                        // Captures the generated API info files
                        File targetApiInfoFileLocationDir = new File("${props.buildOutDir}/zCEE2/${buildFile.replace(".properties", "")}/apiInfoFiles")
                        targetApiInfoFileLocationDir.mkdirs()
                        apiInfoFileLocation.eachFile(FileType.FILES) { file ->
                            Path targetFile = Paths.get(targetApiInfoFileLocationDir.getPath() + "/" + file.getName())
                            Files.copy(file.toPath(), targetFile, StandardCopyOption.COPY_ATTRIBUTES)
                            Path relativeOutputFilePath = Paths.get(props.buildOutDir).relativize(targetFile)
                            unixRecord.addOutput(props.buildOutDir, relativeOutputFilePath.toString(), "zCEE2-Info")
                        }
                        // Captures the generated log files
                        File targetLogFileDirectoryDir = new File("${props.buildOutDir}/zCEE2/${buildFile.replace(".properties", "")}/logs")
                        targetLogFileDirectoryDir.mkdirs()
                        logFileDirectory.eachFile(FileType.FILES) { file ->
                            Path targetFile = Paths.get(targetLogFileDirectoryDir.getPath() + "/" + file.getName())
                            Files.copy(file.toPath(), targetFile, StandardCopyOption.COPY_ATTRIBUTES)
                            Path relativeOutputFilePath = Paths.get(props.buildOutDir).relativize(targetFile)
                            unixRecord.addOutput(props.buildOutDir, relativeOutputFilePath.toString(), "zCEE2-Logs")
                        }
                    }
                }
            }
        }

        if (props.verbose)
            println("** Exit value for the zconbt process: $returnCode")

        if (returnCode != 0) {
            def errorMsg = "*! Error during the zconbt process - Please check the log file at '${logFile.getAbsolutePath()}'."
            println(errorMsg)
            props.error = "true"
            buildUtils.updateBuildResult(errorMsg:errorMsg)
        }
    }
}