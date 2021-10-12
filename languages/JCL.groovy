@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.repository.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*
import com.ibm.jzos.ZFile

// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def buildUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BuildUtilities.groovy"))
@Field def impactUtils= loadScript(new File("${props.zAppBuildDir}/utilities/ImpactUtilities.groovy"))
@Field def bindUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BindUtilities.groovy"))
@Field RepositoryClient repositoryClient

int rc = 0
String errorMsg = ""

println("** Building files mapped to ${this.class.getName()}.groovy script")

// verify required build properties
buildUtils.assertBuildProperties(props.jcl_requiredBuildProperties)


// create language datasets
def langQualifier = "jcl"
buildUtils.createLanguageDatasets(langQualifier)

// sort the build list based on build file rank if provided
List<String> sortedList = buildUtils.sortBuildList(argMap.buildList, 'jcl_fileBuildRank')

// iterate through build list
sortedList.each { buildFile ->
	println "*** Building file $buildFile"
	
	int maxRC = props.getFileProperty('jcl_maxRC', buildFile).toInteger()
	String dataset = props.jcl_srcPDS
	
	// Perform Vanilla Copy
	String member = CopyToPDS.createMemberName(buildFile)
	File logFile = new File( props.userBuild ? "${props.buildOutDir}/${member}.log" : "${props.buildOutDir}/${member}.jcl.log")
	if (logFile.exists())
		logFile.delete()
	
	memberlen = member.size()
	
	if (memberlen > 8) {
		errorMsg = "*! Warning. Resulting ${member} length exceeds 8 characters.  Bypassing build."
		println(errorMsg)
		rc = 4
	} else {
		
		rc = new CopyToPDS().file(new File(buildFile)).dataset(dataset).member(member).execute()
	
		if (props.verbose) println "CopyToPDS for $buildFile to $dataset($member) = $rc"
	}
	
	if (rc > maxRC) {
		errorMsg = "*! The build return code ($rc) for $buildFile exceeded the maximum return code allowed ($maxRC)"
		println(errorMsg)
		props.error = "true"
		buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile],client:getRepositoryClient())
	}
}

// end script

//********************************************************************
 //* Method definitions
 //********************************************************************

def getRepositoryClient() {
	if (!repositoryClient && props."dbb.RepositoryClient.url")
		repositoryClient = new RepositoryClient().forceSSLTrusted(true)

	return repositoryClient
}