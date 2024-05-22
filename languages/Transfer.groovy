@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.metadata.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import com.ibm.dbb.build.report.records.*
import com.ibm.dbb.build.report.*
import groovy.transform.*

/***
 * 
 * Language script, which transfers files to the defined target dataset 
 * and reports the file as a build output file in the build report.
 * 
 * Can be used for JCL, XML, Shared Copybooks and any other type of source code
 * which needs to be packaged and processed by the pipeline.
 * 
 * Please note:
 * 
 * * Verify the allocation options and adjust to your needs.
 * 
 * * File names cannot exeed more than 8 characters, so they can be stored in
 *   the target dataset.
 * 
 * * Review configurations in 
 * 
 *   build-conf/Transfer.properties
 *     to define target datasets and dataset characteristics  
 * 
 *   application-conf/file.properties
 *     to map files and define the deployType
 *     
 *   application-conf/Transfer.properties  
 *     to specify the default deployType
 *   
 */

// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def buildUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BuildUtilities.groovy"))
// Set to keep information about which datasets where already checked/created
@Field HashSet<String> verifiedBuildDatasets = new HashSet<String>()

println("** Building ${argMap.buildList.size()} ${argMap.buildList.size() == 1 ? 'file' : 'files'} mapped to ${this.class.getName()}.groovy script")
// verify required build properties
buildUtils.assertBuildProperties(props.transfer_requiredBuildProperties)

List<String> buildList = argMap.buildList.sort()
int currentBuildFileNumber = 1

// iterate through build list
buildList.each { buildFile ->
	println "*** (${currentBuildFileNumber++}/${buildList.size()}) Transferring file $buildFile"

	// local variables and log file
	String member = CopyToPDS.createMemberName(buildFile)

	// validate lenght of member name
	def memberLen = member.size()

	if (memberLen > 8) {
		errorMsg = "*! Warning. Member name (${member}) exceeds length of 8 characters. "
		println(errorMsg)
		props.error = "true"
		buildUtils.updateBuildResult(errorMsg:errorMsg)
	} else {

		// evaluate the datasetmapping, which maps build files to targetDataset defintions
		PropertyMappings dsMapping = new PropertyMappings("transfer_datasetMapping")
		PropertyMappings dsOptionsMapping = new PropertyMappings("transfer_dsOptions")

		// obtain the target dataset based on the mapped dataset key
		mappedDatesetDef = dsMapping.getValue(buildFile)
		String targetDataset = props.getProperty(mappedDatesetDef)
		
		if (targetDataset != null) {
			
			// obtain the dataset reference for targetDataset
			String datasetOptions = dsOptionsMapping.getValue(mappedDatesetDef)
			
			if (datasetOptions == null) {
				String errorMsg =  "*! Dataset options for $buildFile could not be obtained PropertyMappings <transfer_dsOptions>. "
				println(errorMsg)
				props.error = "true"
				buildUtils.updateBuildResult(errorMsg:errorMsg)
			} 

            // get copy mode value from Property Mappings
			def copyMode = props.getFileProperty('transfer_copyMode', buildFile)

            DBBConstants.CopyMode transferCopyMode
            if (copyMode != null) {
                transferCopyMode = DBBConstants.CopyMode.valueOf(copyMode)
            } else {
                transferCopyMode = DBBConstants.CopyMode.valueOf("TEXT")
                if (props.verbose) println "** No CopyMode found for file '${buildFile}'. Using 'TEXT' as default."
            }
			
			// allocate target dataset
			if (!verifiedBuildDatasets.contains(targetDataset)) { // using a cache not to allocate all defined datasets
				verifiedBuildDatasets.add(targetDataset)
				buildUtils.createDatasets(targetDataset.split(), datasetOptions)
			}

			// copy the file to the target dataset
			String deployType = buildUtils.getDeployType("transfer", buildFile, null)

			try {				
				int rc = new CopyToPDS().key(buildFile)
                    .file(new File(buildUtils.getAbsolutePath(buildFile)))
                    .copyMode(transferCopyMode)
                    .dataset(targetDataset)
                    .member(member)
                    .output(true)
                    .deployType(deployType)
                    .execute()
                if (props.verbose) println "** Copied $buildFile to $targetDataset with copyMode $transferCopyMode and deployType $deployType (rc = $rc)"				

				if (rc!=0){
					String errorMsg = "*! The CopyToPDS return code ($rc) for $buildFile exceeded the maximum return code allowed (0)."
					println(errorMsg)
					props.error = "true"
					buildUtils.updateBuildResult(errorMsg:errorMsg)
				}
			} catch (BuildException e) { // Catch potential exceptions like file truncation
				String errorMsg = "*! (Transfer.groovy)  CopyToPDS of file ${buildFile} failed with an exception \n ${e.getMessage()}."
				throw new BuildException(errorMsg)
			}
		} else {
			String errorMsg =  "*! Target dataset for $buildFile could not be obtained from file properties. "
			println(errorMsg)
			props.error = "true"
			buildUtils.updateBuildResult(errorMsg:errorMsg)
		}
	}
}

