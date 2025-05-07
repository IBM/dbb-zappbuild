@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import com.ibm.dbb.build.report.records.*
import com.ibm.dbb.build.report.*
import groovy.transform.*

/*
 The language script is invoking the 'CICS TS resource builder utility' for the CICS definitions in YAML (yml) format.
 The script processes CICS definition files stored in the git repository with suffix .yml or .yaml to produce a CSD formatted file.
 The model file and the application constraints file, required by the CICS TS resource builder, are referenced through the CRB.properties file.
 The script expects that the CICS TS resource builder is installed on the build machine in Z/OS UNIX. The location of it is also referenced in the properties file.
 The output of the process is a CSD formatted file, which is reported in the DBB build report for further post-build processing such as packaging and deployment into target environment
 Further information on the CICS TS resource builder tool and can be found at 
 https://www.ibm.com/docs/en/cics-resource-builder/1.0?topic=overview
 */

@Field BuildProperties props = BuildProperties.getInstance()
@Field def buildUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BuildUtilities.groovy"))

// verify required build properties
buildUtils.assertBuildProperties(props.crb_requiredBuildProperties)

List<String> buildList = argMap.buildList

println("** Building ${argMap.buildList.size()} ${argMap.buildList.size() == 1 ? 'file' : 'files'} mapped to ${this.class.getName()}.groovy script")
int currentBuildFileNumber = 1


// iterate through build list
buildList.each { buildFile ->
    println "*** (${currentBuildFileNumber++}/${buildList.size()}) Building file $buildFile"

    // Get the path to the zrb executable
    String zrbPath = props.getFileProperty('crb_zrbLocation', buildFile)
    // Set the model and appl constraint paths
    String resourceModelFile = props.getFileProperty('crb_resourceModelFile', buildFile)
    String applicationConstraintsFile = props.getFileProperty('crb_applicationConstraintsFile', buildFile)
    // If maxRc is null or blank, set a default maxRC of 4
    int maxRC = (props.getFileProperty('crb_maxRC', buildFile) ?: "4").toInteger()

    // validate that zrb and model file exist at the provided location
    if (fileExists(zrbPath) && fileExists(resourceModelFile)) {

        // log file
        String member = CopyToPDS.createMemberName(buildFile)
        File logFile = new File("${props.buildOutDir}/${member}.zrb.log")
        if (logFile.exists())
            logFile.delete()

        // Generate the file name for the CSD formatted file
        def extIndex = buildFile.lastIndexOf('.')
        def slashIndex = buildFile.lastIndexOf('/')
        if (slashIndex < 0) slashIndex = 0

        def outputFile = "CICSResourceBuilder/" + buildFile.substring(slashIndex + 1, extIndex) + ".csd"

        File outputDir = new File(props.buildOutDir + '/CICSResourceBuilder');
        if (!outputDir.exists())
            outputDir.mkdirs()

        // Build the shell command to execute
        String optionsString = "build --model ${resourceModelFile} --resources ${props.workspace}/${buildFile}"
        optionsString += " --output ${props.buildOutDir}/${outputFile}"
        if (applicationConstraintsFile && fileExists(applicationConstraintsFile)) {
            optionsString += " --application ${applicationConstraintsFile}"
        }
        ArrayList<String> optionsList = new ArrayList<String>(Arrays.asList(optionsString.split(" ")))
        if (props.verbose)
            println("*** Executing command '$zrbPath' with options '${optionsList}'")
        String encoding = props.logEncoding ?: 'IBM-1047'

        UnixExec zrbExecution = new UnixExec().command(zrbPath)
        zrbExecution.setOptions(optionsList)
        zrbExecution.output(logFile.getAbsolutePath()).mergeErrors(true);
        zrbExecution.setFile(buildFile)
        zrbExecution.setOutputEncoding(encoding)
        zrbExecution.addOutput(props.buildOutDir, outputFile, "CSD")
        int returnCode = zrbExecution.execute()

        // evaluate return code
        if (returnCode > maxRC) {
            String errorMsg = "*! Error executing zrb: $returnCode"
            println(errorMsg)
            props.error = "true"
            buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.zrb.log":logFile])
        } else {
            if (props.verbose) println("*** zrb return code: $returnCode")
            println("*** Output file is ${props.buildOutDir}/$outputFile.")
        }
    }
}

def fileExists(String fileLoc){
    File file = new File(fileLoc)
    if (!file.exists()) {
        String errorMsg = "*! CICS Resource Builder process - $fileLoc not found."
        println(errorMsg)
        props.error = "true"
        buildUtils.updateBuildResult(errorMsg:errorMsg)
        return false
    } else {
        return true
    }
}