@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.metadata.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import com.ibm.dbb.build.report.records.*
import com.ibm.dbb.build.report.*
import groovy.transform.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/***
 * 
 * Language script, which transfers files to the defined target USS directory 
 * and reports the file as a build output file in the build report.
 * 
 * Can be used for configuration files, scripts and any other type of source code
 * which needs to be packaged and processed by the pipeline in USS.
 * 
 *   build-conf/Transfer_USS.properties
 *     to define the target path for deployment 
 * 
 *   application-conf/file.properties
 *     to map files and define the deployType
 *     
 *   application-conf/Transfer_USS.properties  
 *     to specify the target path for deployment for the application
 */

// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def buildUtils = loadScript(new File("${props.zAppBuildDir}/utilities/BuildUtilities.groovy"))
// Set to keep information about which USS directories were already checked/created
@Field HashSet<String> verifiedUSSPaths = new HashSet<String>()

println("** Building ${argMap.buildList.size()} ${argMap.buildList.size() == 1 ? 'file' : 'files'} mapped to ${this.class.getName()}.groovy script")
// verify required build properties
buildUtils.assertBuildProperties(props.transfer_uss_requiredBuildProperties)

List<String> buildList = argMap.buildList.sort()
int currentBuildFileNumber = 1

// iterate through build list
buildList.each { buildFile ->
    println "*** (${currentBuildFileNumber++}/${buildList.size()}) Transferring file $buildFile to USS"

    // obtain target directory: check file-level property first, then global property, then fallback
    String targetDir = (props.getFileProperty('transfer_uss_targetDir', buildFile) ?: "${props.workspace}/ussfiles").replace('${props.workspace}', props.workspace)    

    String deployType = buildUtils.getDeployType("transfer_uss", buildFile, null)

    // Ensure the target directory exists
    if (!verifiedUSSPaths.contains(targetDir)) {
        verifiedUSSPaths.add(targetDir)
        new File(targetDir).mkdirs()
        if (props.verbose) println "** Verified USS directory $targetDir"
    }

    try {
        File sourceFile = new File(buildUtils.getAbsolutePath(buildFile))
        File destFile = new File("${targetDir}/${sourceFile.getName()}")

        // Use NIO copy for USS file transfer
        Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        if (props.verbose) println "** Copied $buildFile to $targetDir with deployType $deployType"

        // Register the output in the build report using CopyToUnixRecord
        CopyToUnixRecord record = new CopyToUnixRecord()
        record.setSource(sourceFile.getAbsolutePath())
        record.setTarget(destFile.getAbsolutePath())
        record.setDeployType(deployType)
        record.setOutput(true)
        record.setRc(0)
        BuildReportFactory.getBuildReport().addRecord(record)
    } catch (Exception e) {
        String errorMsg = "*! (Transfer_USS.groovy) USS copy of file ${buildFile} failed with an exception \n ${e.getMessage()}."
        println(errorMsg)
        props.error = "true"
        buildUtils.updateBuildResult(errorMsg: errorMsg)
    }
}
