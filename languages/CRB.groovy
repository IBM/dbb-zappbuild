@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.metadata.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import com.ibm.dbb.build.report.records.*
import com.ibm.dbb.build.report.*
import groovy.transform.*

/*
The language script is for running the CICS tool (zrb) on CICS definitions in YML (yaml)
format.
The crb.properties file should provide 
 - the zrb location on the host
 - the path to the Model file 
 - the application constraints file
The script will get the resource CICS yml file names from the buildlist and proceed to execute 
the zrb executable on them. This will create a CSD formatted file, which can be stored as an artifact 
or can be run on Z/OS through DFHCSDUP or CicsDef.groovy script
*/

@Field BuildProperties props = BuildProperties.getInstance()
@Field def buildUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BuildUtilities.groovy"))

// verify required build properties
buildUtils.assertBuildProperties(props.crb_requiredBuildProperties)

List<String> buildList = argMap.buildList

println("** Processing files mapped to ${this.class.getName()}.groovy script")

// iterate through build list
buildList.each { buildFile ->
	println("*** Building file $buildFile")

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
    def zrb_cmd = zrbPath + " build --model $resourceModelFile $applicationParm --resources ${props.workspace}/${buildFile} --output ${props.buildOutDir}/$outputFile"

    // Execute the command and save the console output in sout & serr
    def process = zrb_cmd.execute()
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
        AnyTypeRecord ussRecord = new AnyTypeRecord("USS_RECORD")
        // Set attributes
        ussRecord.setAttribute("file", buildFile)
        ussRecord.setAttribute("label", "CICS Resource Builder YAML file")
        ussRecord.setAttribute("outputFile", "${props.buildOutDir}/$outputFile")
        ussRecord.setAttribute("deployType", "CSD")

        // Add new record to build report
        if (props.verbose) 
            println("*** Adding USS_RECORD for $buildFile")
        BuildReportFactory.getBuildReport().addRecord(ussRecord)
    }
}