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

/*
 * Method to query the DBB collections with a list of changed files
 * Configured through reportExternalImpacts* build properties
 */

def reportExternalImpacts(RepositoryClient repositoryClient, Set<String> changedFiles){
	// query external collections to produce externalImpactList

	Map<String,HashSet> collectionImpactsSetMap = new HashMap<String,HashSet>() // <collection><List impactRecords>
	List<Pattern> collectionMatcherPatterns = createMatcherPatterns(props.reportExternalImpactsCollectionPatterns)

	// caluclated and collect external impacts
	changedFiles.each{ changedFile ->

		List<PathMatcher> fileMatchers = createPathMatcherPattern(props.reportExternalImpactsAnalysisFileFilter)

		if(matches(changedFile, fileMatchers)){

			// get directly impacted candidates first
			if (props.reportExternalImpactsAnalysisDepths == "simple" || props.reportExternalImpactsAnalysisDepths == "deep"){
				def logicalImpactedFiles = queryImpactedFiles(changedFile, repositoryClient)
				logicalImpactedFiles.each{ logicalFile ->
					def impactRecord = "${logicalFile.getLname()} \t ${logicalFile.getFile()} \t ${cName}"
					if (props.verbose) println("*** impactRecord: $impactRecord")
					externalImpactList.add(impactRecord)

					// get impacted files of idenfied impacted files
					if(props.reportExternalImpactsAnalysisDepths == "deep") {
						// pass identified direct impact into analysis
						def logicalImpactedFilesSecndLvl= queryImpactedFiles(logicalFile.getFile(), repositoryClient)
						logicalImpactedFilesSecndLvl.each{ logicalFileSecndLvl ->
							def impactRecordSecndLvl = "${logicalFileSecndLvl.getLname()} \t ${logicalFileSecndLvl.getFile()} \t ${cName}"
							if (props.verbose) println("*** impactRecordSecndLvl:  $impactRecordSecndLvl")
							externalImpactList.add(impactRecordSecndLvl)
						}
					}
				}
				collectionImpactsSetMap.put(cName, externalImpactList)
			}
			else {
				println("*! build property reportExternalImpactsAnalysisDepths has in invalid value : ${props.reportExternalImpactsAnaylsisDepths} , valid: simple | deep")
			}
		}
		else {
			if (props.verbose) println("*** Analysis and reporting has been skipped for changed file $changedFile due to build framework configuration (see configuration of build property reportExternalImpactsAnalysisFileFilter)")
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

}

def queryImpactedFiles(String changedFile, RepositoryClient repositoryClient) {
	String memberName = CopyToPDS.createMemberName(changedFile)
	def logicalFiles // initialize
	
	def ldepFile = new LogicalDependency(memberName, null, null);
	repositoryClient.getAllCollections().each{ collection ->
		String cName = collection.getName()
		if(matchesPattern(cName,collectionMatcherPatterns)){ // find matching collection names
			if (cName != props.applicationCollectionName && cName != props.applicationOutputsCollectionName){
				def Set<String> externalImpactList = collectionImpactsSetMap.get(cName) ?: new HashSet<String>()
				logicalFiles = repositoryClient.getAllLogicalFiles(cName, ldepFile);
			}
		}
		else{
			//if (props.verbose) println("$cName does not match pattern: $collectionMatcherPatterns")
		}
	}
	return logicalFiles
	
}

/**
 * Method to calculate and report the changes between the current configuration and concurrent configurations;
 * leverages the existing infrastructure to calculateChangedFiles - in this case for concurrent configs.
 *
 * Invokes method generateConcurrentChangesReports to produce the reports
 *
 * @param repositoryClient
 * @param buildSet
 *
 */
def calculateConcurrentChanges(RepositoryClient repositoryClient, Set<String> buildSet) {
	
		// initialize patterns
		List<Pattern> gitRefMatcherPatterns = createMatcherPatterns(props.reportConcurrentChangesGitBranchReferencePatterns)
	
		// obtain all current remote branches
		// TODO: Handle / Exclude branches from other repositories
		Set<String> remoteBranches = new HashSet<String>()
		props.applicationSrcDirs.split(",").each { dir ->
			dir = buildUtils.getAbsolutePath(dir)
			remoteBranches.addAll(gitUtils.getRemoteGitBranches(dir))
		}
		
		// Run analysis for each remoteBranch, which matches the configured criteria
		remoteBranches.each { gitReference ->
	
			if (matchesPattern(gitReference,gitRefMatcherPatterns) && !gitReference.equals(props.applicationCurrentBranch)){
	
				Set<String> concurrentChangedFiles = new HashSet<String>()
				Set<String> concurrentRenamedFiles = new HashSet<String>()
				Set<String> concurrentDeletedFiles = new HashSet<String>()
				Set<String> concurrentBuildProperties = new HashSet<String>()
	
				if (props.verbose) println "***  Analysing and validating changes for branch $gitReference ."
	
				(concurrentChangedFiles, concurrentRenamedFiles, concurrentDeletedFiles, concurrentBuildProperties) = calculateChangedFiles(null, true, gitReference)
	
				// generate reports and verify for intersects
				generateConcurrentChangesReports(buildSet, concurrentChangedFiles, concurrentRenamedFiles, concurrentDeletedFiles, gitReference, repositoryClient)
	
			}
		}
	
	}

/*
 * Method to generate the Concurrent Changes reports and validate if the current build list intersects with concurrent changes
 */

def generateConcurrentChangesReports(Set<String> buildList, Set<String> concurrentChangedFiles, Set<String> concurrentRenamedFiles, Set<String> concurrentDeletedFiles, String gitReference, RepositoryClient repositoryClient){
	String concurrentChangesReportLoc = "${props.buildOutDir}/report_concurrentChanges.txt"

	File concurrentChangesReportFile = new File(concurrentChangesReportLoc)
	String enc = props.logEncoding ?: 'IBM-1047'
	concurrentChangesReportFile.withWriterAppend(enc) { writer ->

		if (!(concurrentChangedFiles.size() == 0 &&  concurrentRenamedFiles.size() == 0 && concurrentDeletedFiles.size() == 0)) {

			if (props.verbose) println("** Writing report of concurrent changes to $concurrentChangesReportLoc for configuration $gitReference")

			writer.write("\n=============================================== \n")
			writer.write("** Report for configuration: $gitReference \n")
			writer.write("========\n")

			if (concurrentChangedFiles.size() != 0) {
				writer.write("** Changed Files \n")
				concurrentChangedFiles.each { file ->
					if (props.verbose) println " Changed: ${file}"
					if (buildList.contains(file)) {
						writer.write("* $file is changed and intersects with the current build list.\n")
						String msg = "*!! $file is changed on branch $gitReference and intersects with the current build list."
						println msg
						
						// update build result
						if (props.reportConcurrentChangesIntersectionFailsBuild && props.reportConcurrentChangesIntersectionFailsBuild.toBoolean()) {
							props.error = "true"
							buildUtils.updateBuildResult(errorMsg:msg,client:repositoryClient)
						} else {
							buildUtils.updateBuildResult(warningMsg:msg,client:repositoryClient)
						}
					}
					else
						writer.write("  $file\n")
				}
			}

			if (concurrentRenamedFiles.size() != 0) {
				writer.write("** Renamed Files \n")
				concurrentRenamedFiles.each { file ->
					if (props.verbose) println " Renamed: ${file}"
					if (buildList.contains(file)) {
						writer.write("* $file got renamed and intersects with the current build list.\n")
						String msg = "*!! $file is renamed on branch $gitReference and intersects with the current build list."
						println msg
						
						// update build result
						if (props.reportConcurrentChangesIntersectionFailsBuild && props.reportConcurrentChangesIntersectionFailsBuild.toBoolean()) {
							props.error = "true"
							buildUtils.updateBuildResult(errorMsg:msg,client:repositoryClient)
						} else {
							buildUtils.updateBuildResult(warningMsg:msg,client:repositoryClient)
						}
					}
					else
						writer.write("  $file\n")
				}
			}

			if (concurrentDeletedFiles.size() != 0) {
				writer.write("** Deleted Files \n")
				concurrentDeletedFiles.each { file ->
					if (props.verbose) println " Deleted: ${file}"
					if (buildList.contains(file)) {
						writer.write("* $file is deleted and intersects with the current build list.\n")
						String msg = "*!! $file is deleted on branch $gitReference and intersects with the current build list."
						println msg
						
						// update build result
						if (props.reportConcurrentChangesIntersectionFailsBuild && props.reportConcurrentChangesIntersectionFailsBuild.toBoolean()) {
							props.error = "true"
							buildUtils.updateBuildResult(errorMsg:msg,client:repositoryClient)
						} else {
							buildUtils.updateBuildResult(warningMsg:msg,client:repositoryClient)
						}
					}
					else
						writer.write("  $file\n")
				}
			}
		}
	}
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
