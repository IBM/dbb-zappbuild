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
The script expects that the CICS TS resource builder is installed on the build machin in Z/OS UNIX. The location of it is also referenced in the properties file.
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


    File zrbExe = new File(zrbPath)
    if (!zrbExe.exists()) {
        String errorMsg = "*! The zrb utility was not found at $zrbPath."
        println(errorMsg)
        props.error = "true"
        buildUtils.updateBuildResult(errorMsg:errorMsg)
    }

    // Generate the file name for the CSD formatted file
    def extIndex = buildFile.lastIndexOf('.')
    def slashIndex = buildFile.lastIndexOf('/')
    if (slashIndex < 0) slashIndex = 0
	def outputFile = buildFile.substring(slashIndex + 1, extIndex) + ".csd"

    println("*** Output file is ${props.buildOutDir}/$outputFile.")
    // Build the shell command to execute
    def applicationParm = ""
    if (applicationConstraintsFile) 
        applicationParm = "--application $applicationConstraintsFile"
    def commandString = zrbPath + " build --model $resourceModelFile $applicationParm --resources ${props.workspace}/${buildFile} --output ${props.buildOutDir}/$outputFile"
    if (props.verbose)
        println("*** Executing zrb command: $commandString")    

    // Execute the command and save the console output and error streams
    def process = commandString.execute()
    process.waitForProcessOutput(System.out, System.err)
    def returnCode = process.exitValue()
    if (returnCode > maxRC) {
        String errorMsg = "*! Error executing zrb: $returnCode"
        println(errorMsg)
        props.error = "true"
        buildUtils.updateBuildResult(errorMsg:errorMsg)
    } else {
        if (props.verbose)
            println("*** zrb return code: $returnCode")
        // Create a new record of type AnyTypeRecord
        AnyTypeRecord CRBRecord = new AnyTypeRecord("USS_RECORD")
        CRBRecord.setAttribute("file", buildFile)
        CRBRecord.setAttribute("label", "CICS Resource Builder YAML file")
        CRBRecord.setAttribute("outputs", "[${props.buildOutDir}/$outputFile, CSD]")
	    CRBRecord.setAttribute("command", commandString);

        // Add new record to build report
        if (props.verbose) 
            println("*** Adding USS_RECORD for $buildFile")
        BuildReportFactory.getBuildReport().addRecord(CRBRecord)
    }
}
