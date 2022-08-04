@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.repository.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher
import groovy.json.JsonSlurper
import groovy.transform.*
import java.util.regex.*

// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def buildUtils= loadScript(new File("BuildUtilities.groovy"))
@Field def gitUtils= loadScript(new File("GitUtilities.groovy"))

/*
 * Method to query the DBB collections with a list of changed files
 * Configured through reportExternalImpacts* build properties
 */

def reportExternalImpacts(RepositoryClient repositoryClient, Set<String> changedFiles){
	// query external collections to produce externalImpactList

	Map<String,HashSet> collectionImpactsSetMap = new HashMap<String,HashSet>() // <collection><List impactRecords>
	Set<String> impactedFiles = new HashSet<String>()

	if (props.reportExternalImpactsAnalysisDepths == "simple" || props.reportExternalImpactsAnalysisDepths == "deep"){

		// calculate and collect external impacts
		changedFiles.each{ changedFile ->

			List<PathMatcher> fileMatchers = createPathMatcherPattern(props.reportExternalImpactsAnalysisFileFilter)

			// check that file is on reportExternalImpactsAnalysisFileFilter
			if(matches(changedFile, fileMatchers)){

				// get directly impacted candidates first
				(collectionImpactsSetMap, impactedFiles) = calculateLogicalImpactedFiles(changedFile, collectionImpactsSetMap, repositoryClient)

				// get impacted files of idenfied impacted files
				if (props.reportExternalImpactsAnalysisDepths == "deep") {
					impactedFiles.each{ impactedFile ->
						(collectionImpactsSetMap, impactedFiles) = calculateLogicalImpactedFiles(impactedFile, collectionImpactsSetMap, repositoryClient)
					}
				}

				// generate reports by collection / application
				collectionImpactsSetMap.each{ entry ->
					externalImpactList = entry.value
					if (externalImpactList.size()!=0){
						// write impactedFiles per application to build workspace
						String impactListFileLoc = "${props.buildOutDir}/externalImpacts_${entry.key}.${props.buildListFileExt}"
						if (props.verbose) println("*** Writing report of external impacts to file $impactListFileLoc")
						File impactListFile = new File(impactListFileLoc)
						String enc = props.logEncoding ?: 'IBM-1047'
						impactListFile.withWriter(enc) { writer ->
							externalImpactList.each { file ->
								// if (props.verbose) println file
								writer.write("$file\n")
							}
						}
					}
				}

			} else {
				if (props.verbose) println("*** Analysis and reporting has been skipped for changed file $changedFile due to build framework configuration (see configuration of build property reportExternalImpactsAnalysisFileFilter)")
			}
		}
	}
	else {
		println("*! build property reportExternalImpactsAnalysisDepths has an invalid value : ${props.reportExternalImpactsAnaylsisDepths} , valid: simple | deep")
	}
}


def calculateLogicalImpactedFiles(String changedFile, Map<String,HashSet> collectionImpactsSetMap, RepositoryClient repositoryClient) {

	// local matchers to inspect files and collections
	List<Pattern> collectionMatcherPatterns = createMatcherPatterns(props.reportExternalImpactsCollectionPatterns)

	// will be returned
	Set<String> impactedFiles = new HashSet<String>()

	String memberName = CopyToPDS.createMemberName(changedFile)

	def ldepFile = new LogicalDependency(memberName, null, null);
	repositoryClient.getAllCollections().each{ collection ->
		String cName = collection.getName()
		if(matchesPattern(cName,collectionMatcherPatterns)){ // find matching collection names
			def Set<String> externalImpactList = collectionImpactsSetMap.get(cName) ?: new HashSet<String>()
			def logicalImpactedFiles = repositoryClient.getAllLogicalFiles(cName, ldepFile);

			logicalImpactedFiles.each{ logicalFile ->
				if (props.verbose) println("*** Changed file $changedFile has a potential external impact on logical file ${logicalFile.getLname()} (${logicalFile.getFile()}) in collection ${cName} ")
				def impactRecord = "${logicalFile.getLname()} \t ${logicalFile.getFile()} \t ${cName}"

				if (cName != props.applicationCollectionName && cName != props.applicationOutputsCollectionName){ // we can exclude internal impacted files
					externalImpactList.add(impactRecord)
				}
				impactedFiles.add(logicalFile.getFile())
			}
			// adding updated record
			collectionImpactsSetMap.put(cName, externalImpactList)
		}
		else{
			//if (props.verbose) println("$cName does not match pattern: $collectionMatcherPatterns")
		}
	}

	return [
		collectionImpactsSetMap,
		impactedFiles
	]
}

def matches(String file, List<PathMatcher> pathMatchers) {
	def result = pathMatchers.any { matcher ->
		Path path = FileSystems.getDefault().getPath(file);
		if ( matcher.matches(path) )
		{
			return true
		}
	}
	return result
}

/**
 * createPathMatcherPattern
 * Generic method to build PathMatcher from a build property
 */

def createPathMatcherPattern(String property) {
	List<PathMatcher> pathMatchers = new ArrayList<PathMatcher>()
	if (property) {
		property.split(',').each{ filePattern ->
			if (!filePattern.startsWith('glob:') || !filePattern.startsWith('regex:'))
				filePattern = "glob:$filePattern"
			PathMatcher matcher = FileSystems.getDefault().getPathMatcher(filePattern)
			pathMatchers.add(matcher)
		}
	}
	return pathMatchers
}

/**
 * create List of Regex Patterns
 */

def createMatcherPatterns(String property) {
	List<Pattern> patterns = new ArrayList<Pattern>()
	if (property) {
		property.split(',').each{ patternString ->
			Pattern pattern = Pattern.compile(patternString);
			patterns.add(pattern)
		}
	}
	return patterns
}

/**
 * match a String against a list of patterns
 */
def matchesPattern(String name, List<Pattern> patterns) {
	def result = patterns.any { pattern ->
		if (pattern.matcher(name).matches())
		{
			return true
		}
	}
	return result
}
