@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.repository.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import com.ibm.dbb.build.report.records.*
import com.ibm.dbb.build.report.*
import groovy.transform.*

/***
 * 
 * 
 * 
 */

// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def buildUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BuildUtilities.groovy"))
@Field RepositoryClient repositoryClient

println("** Building files mapped to ${this.class.getName()}.groovy script")

// verify required build properties
buildUtils.assertBuildProperties(props.cobol_requiredBuildProperties)

List<String> buildList = argMap.buildList

// iterate through build list
buildList.each { buildFile ->
	println "*** Building file $buildFile"

	// local variables and log file
	String member = CopyToPDS.createMemberName(buildFile)

	// evaluate the datasetmapping, which maps build files to targetDataset defintions 
	PropertyMappings dsMapping = new PropertyMappings("nonbuildable_datasetMapping")
	
	// obtain the target dataset based on the mapped dataset key
	String targetDataset = props.getProperty(dsMapping.getValue(buildFile))
	
	if (targetDataset != null) {

		// allocate target dataset
		// TODO: Create a cache of targets which got allocated
		buildUtils.createDatasets(targetDataset.split(), props.nonbuildable_srcOptions)

		// copy the file to the target dataset
		String deployType = buildUtils.getDeployType("nonbuildable", buildFile, null)
		int rc = new CopyToPDS().file(new File(buildUtils.getAbsolutePath(buildFile))).dataset(targetDataset).member(member).output(true).deployType(deployType).execute()
		
		if (props.verbose) println "** Copyied $buildFile to $targetDataset with deployTyoe $deployType; rc = $rc"
		
		if (rc!=0){
			String errorMsg = "*! The CopyToPDS return code ($rc) for $buildFile exceeded the maximum return code allowed (0)."
			println(errorMsg)
			props.error = "true"
			buildUtils.updateBuildResult(errorMsg:errorMsg,client:getRepositoryClient())
		}
	} else {
		String errorMsg =  "*! Target dataset for $buildFile could not be obtained. "
		println(errorMsg)
		props.error = "true"
		buildUtils.updateBuildResult(errorMsg:errorMsg,client:getRepositoryClient())
	}
}

// internal methoda
 
def getRepositoryClient() {
	if (!repositoryClient && props."dbb.RepositoryClient.url")
		repositoryClient = new RepositoryClient().forceSSLTrusted(true)

	return repositoryClient
}
