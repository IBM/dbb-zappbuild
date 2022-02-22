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
@Field def gitUtils= loadScript(new File("GitUtilities.groovy"))
@Field def buildUtils= loadScript(new File("BuildUtilities.groovy"))
@Field String hashPrefix = ':githash:'


def createImpactBuildList(RepositoryClient repositoryClient) {
	// local variables
	Set<String> changedFiles = new HashSet<String>()
	Set<String> deletedFiles = new HashSet<String>()
	Set<String> renamedFiles = new HashSet<String>()
	Set<String> changedBuildProperties = new HashSet<String>()

	// get the last build result to get the baseline hashes
	def lastBuildResult = buildUtils.retrieveLastBuildResult(repositoryClient)

	// calculate changed files
	if (lastBuildResult || props.baselineRef) {
		(changedFiles, deletedFiles, renamedFiles, changedBuildProperties) = calculateChangedFiles(lastBuildResult)
	}
	else {
		// else create a fullBuild list
		println "*! No prior build result located.  Building all programs"
		changedFiles = buildUtils.createFullBuildList()
	}

	// scan files and update source collection for impact analysis
	updateCollection(changedFiles, deletedFiles, renamedFiles, repositoryClient)



	// create build list using impact analysis
	if (props.verbose) println "*** Perform impacted analysis for changed files."

	Set<String> buildSet = new HashSet<String>()
	Set<String> changedBuildPropertyFiles = new HashSet<String>()
	
	PropertyMappings githashBuildableFilesMap = new PropertyMappings("githashBuildableFilesMap")
	
	
	changedFiles.each { changedFile ->
		// if the changed file has a build script then add to build list
		if (ScriptMappings.getScriptName(changedFile)) {
			buildSet.add(changedFile)
			if (props.verbose) println "** Found build script mapping for $changedFile. Adding to build list"
		}

		// check if impact calculation should be performed, default true
		if (shouldCalculateImpacts(changedFile)){

			// perform impact analysis on changed file
			if (props.verbose) println "** Performing impact analysis on changed file $changedFile"

			String impactResolutionRules = props.getFileProperty('impactResolutionRules', changedFile)
			ImpactResolver impactResolver = createImpactResolver(changedFile, impactResolutionRules, repositoryClient)

			// get excludeListe
			List<PathMatcher> excludeMatchers = createPathMatcherPattern(props.excludeFileList)

			// Print impactResolverConfiguration
			if (props.formatConsoleOutput && props.formatConsoleOutput.toBoolean()) {
				// print collection information
				println("    " + "Collection".padRight(20) )
				println("    " + " ".padLeft(20,"-"))
				impactResolver.getCollections().each{ collectionName ->
					println("    " + collectionName)
				}
				// print impact resolution rule in table format
				buildUtils.printResolutionRules(impactResolver.getResolutionRules())
			}
			
			// resolving impacts
			def impacts = impactResolver.resolve()
			impacts.each { impact ->
				def impactFile = impact.getFile()
				if (props.verbose) println "** Found impacted file $impactFile"
				// only add impacted files that have a build script mapped to it
				if (ScriptMappings.getScriptName(impactFile)) {
					// only add impacted files, that are in scope of the build.
					if (!matches(impactFile, excludeMatchers)){
						
						// calculate abbreviated gitHash for impactFile
						filePattern = FileSystems.getDefault().getPath(impactFile).getParent().toString()
						if (filePattern != null && githashBuildableFilesMap.getValue(impactFile) == null) {
							abbrevCurrentHash = gitUtils.getCurrentGitHash(buildUtils.getAbsolutePath(filePattern), true)
							githashBuildableFilesMap.addFilePattern(abbrevCurrentHash, filePattern+"/*")
						}
						
						// add file to buildset
						buildSet.add(impactFile)
						if (props.verbose) println "** $impactFile is impacted by changed file $changedFile. Adding to build list."
					}
					else {
						// impactedFile found, but on Exclude List
						//   Possible reasons: Exclude of file was defined after building the collection.
						//   Rescan/Rebuild Collection to synchronize it with defined build scope.
						if (props.verbose) println "!! $impactFile is impacted by changed file $changedFile, but is on Exlude List. Not added to build list."
					}
				}
			}

		}else {
			if (props.verbose) println "** Impact analysis for $changedFile has been skipped due to configuration."
		}
	}

	// Perform impact analysis for property changes
	if (props.impactBuildOnBuildPropertyChanges && props.impactBuildOnBuildPropertyChanges.toBoolean()){
		if (props.verbose) println "*** Perform impacted analysis for property changes."

		changedBuildProperties.each { changedProp ->

			if (props.impactBuildOnBuildPropertyList.contains(changedProp.toString())){

				// perform impact analysis on changed property
				if (props.verbose) println "** Performing impact analysis on property $changedProp"

				// create logical dependency and query collections for logical files with this dependency
				LogicalDependency lDependency = new LogicalDependency("$changedProp","BUILDPROPERTIES","PROPERTY")
				logicalFileList = repositoryClient.getAllLogicalFiles(props.applicationCollectionName, lDependency)


				// get excludeListe
				List<PathMatcher> excludeMatchers = createPathMatcherPattern(props.excludeFileList)

				logicalFileList.each { logicalFile ->
					def impactFile = logicalFile.getFile()
					if (props.verbose) println "** Found impacted file $impactFile"
					// only add impacted files that have a build script mapped to it
					if (ScriptMappings.getScriptName(impactFile)) {
						// only add impacted files, that are in scope of the build.
						if (!matches(impactFile, excludeMatchers)){
							buildSet.add(impactFile)
							if (props.verbose) println "** $impactFile is impacted by changed property $changedProp. Adding to build list."
						}
						else {
							// impactedFile found, but on Exclude List
							//   Possible reasons: Exclude of file was defined after building the collection.
							//   Rescan/Rebuild Collection to synchronize it with defined build scope.
							if (props.verbose) println "!! $impactFile is impacted by changed property $changedProp, but is on Exlude List. Not added to build list."
						}
					}
				}
			}else {
				if (props.verbose) println "** Calculation of impacted files by changed property $changedProp has been skipped due to configuration. "
			}
		}
	}else {
		if (props.verbose) println "** Calculation of impacted files by changed properties has been skipped due to configuration. "
	}

	// Perform analysis and build report of external impacts
	if (props.reportExternalImpacts && props.reportExternalImpacts.toBoolean()){
		if (props.verbose) println "*** Analyze and report external impacted files."
		reportExternalImpacts(repositoryClient, changedFiles)
	}

	// Document and validate concurrent changes
	if (props.reportConcurrentChanges && props.reportConcurrentChanges.toBoolean()){
		if (props.verbose) println "*** Calculate and document concurrent changes."
		calculateConcurrentChanges(repositoryClient, buildSet)
	}

	return [buildSet, deletedFiles]
}


/*
 * createMergeBuildList - calculates the changed and deleted files flowing back to the mainBuildBranch
 *  implements the build type --mergeBuild
 *
 */

def createMergeBuildList(RepositoryClient repositoryClient){
	Set<String> changedFiles = new HashSet<String>()
	Set<String> deletedFiles = new HashSet<String>()
	Set<String> renamedFiles = new HashSet<String>()
	Set<String> changedBuildProperties = new HashSet<String>()

	(changedFiles, deletedFiles, renamedFiles, changedBuildProperties) = calculateChangedFiles(null)

	// scan files and update source collection
	updateCollection(changedFiles, deletedFiles, renamedFiles, repositoryClient)

	// iterate over changed file and add them to the buildSet

	Set<String> buildSet = new HashSet<String>()


	changedFiles.each { changedFile ->
		// if the changed file has a build script then add to build list
		if (ScriptMappings.getScriptName(changedFile)) {
			buildSet.add(changedFile)
			if (props.verbose) println "** Found build script mapping for $changedFile. Adding to build list"
		}
	}

	// Document and validate concurrent changes
	if (props.reportConcurrentChanges && props.reportConcurrentChanges.toBoolean()){
		if (props.verbose) println "*** Calculate and document concurrent changes."
		calculateConcurrentChanges(repositoryClient, buildSet)
	}

	return [buildSet, deletedFiles]
}


/*
 * calculateChangedFiles - method to caluclate the the changed files
 * 
 *  If a BuildResult is provided, this method will diff between the different states in the repository
 *   if no BuildResult is provided, the method will diff the changes from the topic branch to the main build branch
 * 
 */

def calculateChangedFiles(BuildResult lastBuildResult) {
	return calculateChangedFiles(lastBuildResult, false, null)
}

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

def calculateChangedFiles(BuildResult lastBuildResult, boolean calculateConcurrentChanges, String gitReference) {
	String msg = ""
	if (calculateConcurrentChanges.toBoolean()) {
		msg = "in configuration $gitReference"
	}

	// local variables
	Map<String,String> currentHashes = new HashMap<String,String>()
	Map<String,String> currentAbbrevHashes = new HashMap<String,String>()
	Map<String,String> baselineHashes = new HashMap<String,String>()
	Set<String> changedFiles = new HashSet<String>()
	Set<String> deletedFiles = new HashSet<String>()
	Set<String> renamedFiles = new HashSet<String>()
	Set<String> changedBuildProperties = new HashSet<String>()

	// DBB property map to store changed files with their abbreviated git hash
	PropertyMappings githashBuildableFilesMap = new PropertyMappings("githashBuildableFilesMap")
	
	// create a list of source directories to search
	List<String> directories = []
	if (props.applicationSrcDirs)
		directories.addAll(props.applicationSrcDirs.split(','))

	// get the current Git hash for all build directories
	directories.each { dir ->
		dir = buildUtils.getAbsolutePath(dir)
		if (props.verbose) println "** Getting current hash for directory $dir"
		String hash = null
		String abbrevHash = null
		if (gitUtils.isGitDir(dir)) {
			hash = gitUtils.getCurrentGitHash(dir, false)
			abbrevHash = gitUtils.getCurrentGitHash(dir, true)
		}
		String relDir = buildUtils.relativizePath(dir)
		if (props.verbose) println "** Storing $relDir : $hash"
		currentHashes.put(relDir,hash)
		currentAbbrevHashes.put(relDir, abbrevHash)
	}

	// when a build result is provided, calculate the baseline hash for each directory
	if (lastBuildResult != null){
		// get the baseline hash for all build directories
		directories.each { dir ->
			dir = buildUtils.getAbsolutePath(dir)
			if (props.verbose) println "** Getting baseline hash for directory $dir"
			String key = "$hashPrefix${buildUtils.relativizePath(dir)}"
			String relDir = buildUtils.relativizePath(dir)
			String hash
			// retrieve baseline reference overwrite if set
			if (props.baselineRef){
				String[] baselineMap = (props.baselineRef).split(",")
				baselineMap.each{
					// case: baselineRef (gitref)
					if(it.split(":").size()==1 && relDir.equals(props.application)){
						if (props.verbose) println "*** Baseline hash for directory $relDir retrieved from overwrite."
						hash = it
					}
					// case: baselineRef (folder:gitref)
					else if(it.split(":").size()>1){
						(appSrcDir, gitReference) = it.split(":")
						if (appSrcDir.equals(relDir)){
							if (props.verbose) println "*** Baseline hash for directory $relDir retrieved from overwrite."
							hash = gitReference
						}
					}
				}
				// for build directories which are not specified in baselineRef mapping, return the info from lastBuildResult
				if (hash == null && lastBuildResult) {
					hash = lastBuildResult.getProperty(key)
				}
			} else if (lastBuildResult){
				// return from lastBuildResult
				hash = lastBuildResult.getProperty(key)
			}
			if (hash == null){
				println "!** Could not obtain the baseline hash for directory $relDir."
			}

			if (props.verbose) println "** Storing $relDir : $hash"
			baselineHashes.put(relDir,hash)
		}
	}

	// calculate the changed and deleted files by diff'ing the current and baseline hashes
	directories.each { dir ->
		dir = buildUtils.getAbsolutePath(dir)
		if (props.verbose) println "** Calculating changed files for directory $dir"
		def changed = []
		def deleted = []
		def renamed = []
		String baseline
		String current
		String abbrevCurrent
		
		if (gitUtils.isGitDir(dir)){
			// obtain git hashes for directory
			baseline = baselineHashes.get(buildUtils.relativizePath(dir))
			current = currentHashes.get(buildUtils.relativizePath(dir))
			abbrevCurrent = currentAbbrevHashes.get(buildUtils.relativizePath(dir))
			
			// when a build result is provided and build type impactBuild,
			//   calculate changed between baseline and current state of the repository
			if (lastBuildResult != null && props.impactBuild && !calculateConcurrentChanges){
				baseline = baselineHashes.get(buildUtils.relativizePath(dir))
				current = currentHashes.get(buildUtils.relativizePath(dir))
				if (!baseline || !current) {
					if (props.verbose) println "*! Skipping directory $dir because baseline or current hash does not exist.  baseline : $baseline current : $current"
				}
				else {
					if (props.verbose) println "** Diffing baseline $baseline -> current $current"
					(changed, deleted, renamed) = gitUtils.getChangedFiles(dir, baseline, current)
				}
			}
			// when no build result is provided but the outgoingChangesBuild, calculate the outgoing changes
			else if(props.mergeBuild && !calculateConcurrentChanges) {
				// set git references
				baseline = props.mainBuildBranch
				current = "HEAD"

				if (props.verbose) println "** Triple-dot diffing configuration baseline remotes/origin/$baseline -> current HEAD"
				(changed, deleted, renamed) = gitUtils.getMergeChanges(dir, baseline)
			}
			// calculate concurrent changes
			else if (calculateConcurrentChanges) {
				(changed, deleted, renamed) = gitUtils.getConcurrentChanges(dir, gitReference)
			}
		}
		else {
			if (props.verbose) println "*! Directory $dir not a local Git repository. Skipping."
		}

		// Understand repository setup for offsets
		def mode = null

		// make sure file is not an excluded file
		List<PathMatcher> excludeMatchers = createPathMatcherPattern(props.excludeFileList)

		if (props.verbose) println "*** Changed files for directory $dir $msg:"
		changed.each { file ->
			(file, mode) = fixGitDiffPath(file, dir, true, null)
			if ( file != null ) {
				if ( !matches(file, excludeMatchers)) {
					changedFiles << file
					githashBuildableFilesMap.addFilePattern(abbrevCurrent, file)
					if (props.verbose) println "**** $file"
				}
				//retrieving changed build properties
				if (props.impactBuildOnBuildPropertyChanges && props.impactBuildOnBuildPropertyChanges.toBoolean() && file.endsWith(".properties")){
					if (props.verbose) println "**** $file"
					String gitDir = new File(buildUtils.getAbsolutePath(file)).getParent()
					String pFile =  new File(buildUtils.getAbsolutePath(file)).getName()
					changedBuildProperties.addAll(gitUtils.getChangedProperties(gitDir, baseline, current, pFile))
				}
			}
		}

		if (props.verbose) println "*** Deleted files for directory $dir $msg:"
		deleted.each { file ->
			if ( !matches(file, excludeMatchers)) {
				(file, mode) = fixGitDiffPath(file, dir, false, mode)
				deletedFiles << file
				if (props.verbose) println "**** $file"
			}
		}

		if (props.verbose) println "*** Renamed files for directory $dir $msg:"
		renamed.each { file ->
			if ( !matches(file, excludeMatchers)) {
				(file, mode) = fixGitDiffPath(file, dir, false, mode)
				renamedFiles << file
				if (props.verbose) println "**** $file"
			}
		}

	}

	return [
		changedFiles,
		deletedFiles,
		renamedFiles,
		changedBuildProperties
	]
}


/*
 * Method to populate the output collection in a scanOnly + scanLoadmodules build scenario.
 * Scenario: Migrate Source to Git and scan against existing set of loadmodules.
 * Limitation: Sample for cobol
 */
def scanOnlyStaticDependencies(List buildList, RepositoryClient repositoryClient){
	buildList.each { buildFile ->
		def scriptMapping = ScriptMappings.getScriptName(buildFile)
		if(scriptMapping != null){
			langPrefix = buildUtils.getLangPrefix(scriptMapping)
			if(langPrefix != null){
				String isLinkEdited = props.getFileProperty("${langPrefix}_linkEdit", buildFile)

				def scanner = buildUtils.getScanner(buildFile)
				LogicalFile logicalFile = scanner.scan(buildFile, props.workspace)

				String member = CopyToPDS.createMemberName(buildFile)
				String loadPDSMember = props."${langPrefix}_loadPDS"+"($member)"

				if ((isLinkEdited && isLinkEdited.toBoolean()) || scriptMapping == "LinkEdit.groovy"){
					try{
						if (props.verbose) println ("*** Scanning load module $loadPDSMember of $buildFile")
						saveStaticLinkDependencies(buildFile, props."${langPrefix}_loadPDS", logicalFile, repositoryClient)
					}
					catch (com.ibm.dbb.build.ValidationException e){
						println ("!* Error scanning output file for $buildFile  : $loadPDSMember")
						println e
					}
				}
				else {
					if (props.verbose) println ("*** Skipped scanning module $loadPDSMember of $buildFile.")
				}
			} else {
				if (props.verbose) println ("*** Skipped scanning outputs of $buildFile. No language prefix found.")
			}
		}
	}
}

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

			if (props.reportExternalImpactsAnalysisDepths == "simple"){
				// Simple resolution without recursive resolution
				String memberName = CopyToPDS.createMemberName(changedFile)

				def ldepFile = new LogicalDependency(memberName, null, null);
				repositoryClient.getAllCollections().each{ collection ->
					String cName = collection.getName()
					if(matchesPattern(cName,collectionMatcherPatterns)){ // find matching collection names
						if (cName != props.applicationCollectionName && cName != props.applicationOutputsCollectionName){
							def Set<String> externalImpactList = collectionImpactsSetMap.get(cName) ?: new HashSet<String>()
							def logicalFiles = repositoryClient.getAllLogicalFiles(cName, ldepFile);
							logicalFiles.each{ logicalFile ->
								def impactRecord = "${logicalFile.getLname()} \t ${logicalFile.getFile()} \t ${cName}"
								// if (props.verbose) println("*** $impactRecord")
								externalImpactList.add(impactRecord)
							}
							collectionImpactsSetMap.put(cName, externalImpactList)
						}
					}
					else{
						//if (props.verbose) println("$cName does not match pattern: $collectionMatcherPatterns")
					}
				}
			}
			else if(props.reportExternalImpactsAnalysisDepths == "deep"){
				// Recursive analysis to support nested scenarios

				// Configure impact resolver
				ImpactResolver impactResolver = new ImpactResolver().file(changedFile).repositoryClient(repositoryClient)

				String impactResolutionRules = props.getFileProperty('impactResolutionRules', changedFile)
				impactResolver.setResolutionRules(buildUtils.parseResolutionRules(impactResolutionRules))

				repositoryClient.getAllCollections().each{ collection ->
					String cName = collection.getName()
					if(matchesPattern(cName,collectionMatcherPatterns)){ // find matching collection names
						if (cName != props.applicationCollectionName && cName != props.applicationOutputsCollectionName){
							impactResolver.addCollection(cName) // add collection of foreign application
						}
					}
					else{
						//if (props.verbose) println("$cName does not match pattern: $collectionMatcherPatterns")
					}
				}
				// resolve external impacted files
				def externalImpactedFiles = impactResolver.resolve()

				// report scanning results
				if (externalImpactedFiles.size()!=0) if (props.verbose) println("*** Identified external impacted files for changed file $changedFile")
				externalImpactedFiles.each{ externalImpact ->
					def Set<String> externalImpactList = collectionImpactsSetMap.get(externalImpact.getCollection()) ?: new HashSet<String>()
					def impactRecord = "${externalImpact.getLname()} \t ${externalImpact.getFile()} \t ${externalImpact.getCollection()}"
					// if (props.verbose) println("*** $impactRecord")
					externalImpactList.add(impactRecord)
					collectionImpactsSetMap.put(externalImpact.getCollection(), externalImpactList) // <collection,list of impacted files>
				}
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
						String msg = "* $file is changed on branch $gitReference and intersects with the current build list."
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
						String msg = "* $file is renamed on branch $gitReference and intersects with the current build list."
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
						String msg = "* $file is deleted on branch $gitReference and intersects with the current build list."
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

def createImpactResolver(String changedFile, String rules, RepositoryClient repositoryClient) {
	if (props.verbose) println "*** Creating impact resolver for $changedFile with $rules rules"

	// create an impact resolver for the changed file
	ImpactResolver resolver = new ImpactResolver().file(changedFile)
			.collection(props.applicationCollectionName)
			.collection(props.applicationOutputsCollectionName)
			.repositoryClient(repositoryClient)
	// add resolution rules
	if (rules)
		resolver.setResolutionRules(buildUtils.parseResolutionRules(rules))

	return resolver
}

def updateCollection(changedFiles, deletedFiles, renamedFiles, RepositoryClient repositoryClient) {
	if (!repositoryClient) {
		if (props.verbose) println "** Unable to update collections. No repository client."
		return
	}

	if (props.verbose) println "** Updating collections ${props.applicationCollectionName} and ${props.applicationOutputsCollectionName}"
	//def scanner = new DependencyScanner()
	List<LogicalFile> logicalFiles = new ArrayList<LogicalFile>()
	List<PathMatcher> excludeMatchers = createPathMatcherPattern(props.excludeFileList)

	verifyCollections(repositoryClient)

	// remove deleted files from collection
	deletedFiles.each { file ->
		// files in a collection are stored as relative paths from a source directory
		if (props.verbose) println "*** Deleting logical file for $file"
		logicalFile = buildUtils.relativizePath(file)
		repositoryClient.deleteLogicalFile(props.applicationCollectionName, logicalFile)
		repositoryClient.deleteLogicalFile(props.applicationOutputsCollectionName, logicalFile)
	}

	// remove renamed files from collection
	renamedFiles.each { file ->
		// files in a collection are stored as relative paths from a source directory
		if (props.verbose) println "*** Deleting renamed logical file for $file"
		logicalFile = buildUtils.relativizePath(file)
		repositoryClient.deleteLogicalFile(props.applicationCollectionName, logicalFile)
		repositoryClient.deleteLogicalFile(props.applicationOutputsCollectionName, logicalFile)
	}

	if (props.createTestcaseDependency && props.createTestcaseDependency.toBoolean() && changedFiles && changedFiles.size() > 1) {
		sortFileList(changedFiles);
		if (props.verbose) println "*** Sorted list of changed files: $changedFiles"
	}

	// scan changed files
	changedFiles.each { file ->

		// make sure file is not an excluded file
		if ( new File("${props.workspace}/${file}").exists() && !matches(file, excludeMatchers)) {
			// files in a collection are stored as relative paths from a source directory
			if (props.verbose) println "*** Scanning file $file (${props.workspace}/${file})"

			def scanner = buildUtils.getScanner(file)
			try {
				def logicalFile = scanner.scan(file, props.workspace)
				if (props.verbose) println "*** Logical file for $file =\n$logicalFile"

				// Update logical file with dependencies to build properties
				if (props.impactBuildOnBuildPropertyChanges && props.impactBuildOnBuildPropertyChanges.toBoolean()){
					createPropertyDependency(file, logicalFile)
				}

				// If configured, update test case program dependencies
				if (props.createTestcaseDependency && props.createTestcaseDependency.toBoolean()) {
					// If the file is a zUnit configuration file (BZUCFG)
					if (scanner.getClass() == com.ibm.dbb.dependency.ZUnitConfigScanner) {

						def logicalDependencies = logicalFile.getLogicalDependencies()

						def sysTestDependency = logicalDependencies.find{it.getLibrary().equals("SYSTEST")} // Get the test case program from testcfg
						def sysProgDependency = logicalDependencies.find{it.getLibrary().equals("SYSPROG")} // Get the application program name from testcfg

						if (sysTestDependency){
							// find in local list of logical files first (batch processing)
							def testCaseFiles = logicalFiles.findAll{it.getLname().equals(sysTestDependency.getLname())}
							if (!testCaseFiles){ // alternate retrieve it from the collection
								testCaseFiles = repositoryClient.getAllLogicalFiles(props.applicationCollectionName, sysTestDependency.getLname()).find{
									it.getLanguage().equals("COB")
								}
							}
							testCaseFiles.each{
								it.addLogicalDependency(new LogicalDependency(sysProgDependency.getLname(),"SYSPROG","PROGRAMDEPENDENCY"))
								if (props.verbose) println "*** Updating dependencies for test case program ${it.getFile()} =\n$it"
								logicalFiles.add(it)
							}
						}
					}
				}

				logicalFiles.add(logicalFile)

			} catch (Exception e) {

				String warningMsg = "***** Scanning failed for file $file (${props.workspace}/${file})"
				buildUtils.updateBuildResult(warningMsg:warningMsg,client:getRepositoryClient())
				println(warningMsg)
				e.printStackTrace()

				// terminate when continueOnScanFailure is not set to true
				if(!(props.continueOnScanFailure == 'true')){
					println "***** continueOnScan Failure set to false. Build terminates."
					System.exit(1)
				}
			}

			// save logical files in batches of 500 to avoid running out of heap space
			if (logicalFiles.size() == 500) {
				if (props.verbose)
					println "** Storing ${logicalFiles.size()} logical files in repository collection '$props.applicationCollectionName'"
				repositoryClient.saveLogicalFiles(props.applicationCollectionName, logicalFiles);
				if (props.verbose) println(repositoryClient.getLastStatus())
				logicalFiles.clear()
			}
		}
	}

	// save logical files
	if (props.verbose)
		println "** Storing ${logicalFiles.size()} logical files in repository collection '$props.applicationCollectionName'"
	repositoryClient.saveLogicalFiles(props.applicationCollectionName, logicalFiles);
	if (props.verbose) println(repositoryClient.getLastStatus())
}

/*
 * saveStaticLinkDependencies - Scan the load module to determine LINK dependencies. Impact resolver can use
 * these to determine that this file gets rebuilt if a LINK dependency changes.
 */
def saveStaticLinkDependencies(String buildFile, String loadPDS, LogicalFile logicalFile, RepositoryClient repositoryClient) {
	if (repositoryClient) {
		LinkEditScanner scanner = new LinkEditScanner()
		if (props.verbose) println "*** Scanning load module for $buildFile"
		LogicalFile scannerLogicalFile = scanner.scan(buildUtils.relativizePath(buildFile), loadPDS)
		if (props.verbose) println "*** Logical file = \n$scannerLogicalFile"

		// overwrite original logicalDependencies with load module dependencies
		logicalFile.setLogicalDependencies(scannerLogicalFile.getLogicalDependencies())

		// Store logical file and indirect dependencies to the outputs collection
		repositoryClient.saveLogicalFile("${props.applicationOutputsCollectionName}", logicalFile );
	}
}

/*
 * verifyCollections - verifies that the application collections exists. If not it will
 * create or clone the collections.
 * Uses build properties
 */
def verifyCollections(RepositoryClient repositoryClient) {
	if (!repositoryClient) {
		if (props.verbose) println "** Unable to verify collections. No repository client."
		return
	}

	String mainCollectionName = "${props.application}-${props.mainBuildBranch}"
	String mainOutputsCollectionName = "${props.application}-${props.mainBuildBranch}-outputs"

	// check source collection
	if (!repositoryClient.collectionExists(props.applicationCollectionName)) {
		if (props.topicBranchBuild) {
			if (repositoryClient.collectionExists(mainCollectionName)) {
				repositoryClient.copyCollection(mainCollectionName, props.applicationCollectionName)
				if (props.verbose) println "** Cloned collection ${props.applicationCollectionName} from $mainCollectionName"
			}
			else {
				repositoryClient.createCollection(props.applicationCollectionName)
				if (props.verbose) println "** Created collection ${props.applicationCollectionName}"
			}
		}
		else {
			repositoryClient.createCollection(props.applicationCollectionName)
			if (props.verbose) println "** Created collection ${props.applicationCollectionName}"
		}
	}

	// check outputs collection
	if (!repositoryClient.collectionExists(props.applicationOutputsCollectionName)) {
		if (props.topicBranchBuild) {
			if (repositoryClient.collectionExists(mainOutputsCollectionName)) {
				repositoryClient.copyCollection("${mainOutputsCollectionName}", props.applicationOutputsCollectionName)
				if (props.verbose) println "** Cloned collection ${props.applicationOutputsCollectionName} from $mainOutputsCollectionName"
			}
			else {
				repositoryClient.createCollection(props.applicationOutputsCollectionName)
				if (props.verbose) println "** Created collection ${props.applicationOutputsCollectionName}"
			}
		}
		else {
			repositoryClient.createCollection(props.applicationOutputsCollectionName)
			if (props.verbose) println "** Created collection ${props.applicationOutputsCollectionName}"
		}
	}

}

/* 
 *  calculates the correct filepath from the git diff, due to different offsets in the directory path
 *  like nested projects, projects at root level, no root folder
 *  
 *  returns null if file not found + mustExist
 *  
 *  scenarios / mode
 *  1 - Application projects are nested (e.q Mortgage in zAppBuild), Projects on Rootlevel
 *  2 - Repository name is used as Application Root dir
 *  3 - $dir is not the root directory of the file
 *
 */

def fixGitDiffPath(String file, String dir, boolean mustExist, mode) {

	// default value, relevant for non-existent files (like deletions)
	String defaultValue

	// Scenario 1: Nested projects, like MortgageApplication and projects with a top-level dir
	String relPath = new File(props.workspace).toURI().relativize(new File((dir).trim()).toURI()).getPath()
	String fixedFileName= file.indexOf(relPath) >= 0 ? file.substring(file.indexOf(relPath)) : file
	defaultValue = fixedFileName

	if ( new File("${props.workspace}/${fixedFileName}").exists())
		return [fixedFileName, 1];
	if (mode==1 && !mustExist) return [fixedFileName, 1]

	// Scenario 2: Repository name is used as Application Root directory
	String dirName = new File(dir).getName()
	if (new File("${dir}/${file}").exists())
		return [
			"$dirName/$file" as String,
			2
		]
	if (mode==2 && !mustExist) return [
			"$dirName/$file" as String,
			2
		]

	// Scenario 3: Directory ${dir} is not the root directory of the file
	// Example :
	//   - applicationSrcDirs=nazare-demo-genapp/base/src/cobol,nazare-demo-genapp/base/src/bms
	fixedFileName = buildUtils.relativizePath(dir) + ( file.indexOf ("/") >= 0 ? file.substring(file.lastIndexOf("/")) : file )
	if ( new File("${props.workspace}/${fixedFileName}").exists())
		return [fixedFileName, 3];
	if (mode==3 && !mustExist) return [fixedFileName, 3]

	// returns null or assumed fullPath to file
	if (mustExist){
		if (props.verbose) println "!! (fixGitDiffPath) File not found."
		return [null, null]
	}

	if (props.verbose) println "!! (fixGitDiffPath) Mode could not be determined. Returning default."
	return [defaultValue, null]
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
 *  shouldCalculateImpacts
 *  
 *  Method to calculate if impact analysis should be performed for a changedFile in an impactBuild scenario
 *   returns a boolean - default true
 */
def boolean shouldCalculateImpacts(String changedFile){
	// retrieve Pathmaters from property and check
	List<PathMatcher> nonImpactingFiles = createPathMatcherPattern(props.skipImpactCalculationList)
	onskipImpactCalculationList = matches(changedFile, nonImpactingFiles)

	// return false if changedFile found in skipImpactCalculationList
	if (onskipImpactCalculationList) return false
	return true //default
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

/**
 * createPropertyDependency
 * method to add a dependency to a property key
 */
def createPropertyDependency(String buildFile, LogicalFile logicalFile){
	if (props.verbose) println "*** Adding LogicalDependencies for Build Properties for $buildFile"
	// get language prefix
	def scriptMapping = ScriptMappings.getScriptName(buildFile)
	if(scriptMapping != null){
		def langPrefix = buildUtils.getLangPrefix(scriptMapping)
		// language COB
		if (langPrefix != null ){
			// generic properties
			if (props."${langPrefix}_impactPropertyList"){
				addBuildPropertyDependencies(props."${langPrefix}_impactPropertyList", logicalFile)
			}
			// cics properties
			if (buildUtils.isCICS(logicalFile) && props."${langPrefix}_impactPropertyListCICS") {
				addBuildPropertyDependencies(props."${langPrefix}_impactPropertyListCICS", logicalFile)
			}
			// sql properties
			if (buildUtils.isSQL(logicalFile) && props."${langPrefix}_impactPropertyListSQL") {
				addBuildPropertyDependencies(props."${langPrefix}_impactPropertyListSQL", logicalFile)
			}
		}

	}
}

/**
 * addBuildPropertyDependencies
 * method to logical dependencies records to a logical file for a DBB build property
 */
def addBuildPropertyDependencies(String buildProperties, LogicalFile logicalFile){
	String[] buildProps = buildProperties.split(',')

	buildProps.each { buildProp ->
		buildProp = buildProp.trim()
		if (props.verbose) println "*** Adding LogicalDependency for build prop $buildProp for $logicalFile.file"
		logicalFile.addLogicalDependency(new LogicalDependency("$buildProp","BUILDPROPERTIES","PROPERTY"))
	}
}

/**
 * isMappedAsZUnitConfigFile
 * method to check if a file is mapped with the zUnitConfigScanner, indicating it's a zUnit CFG file
 */
def isMappedAsZUnitConfigFile(mapping, file) {
	return (mapping.isMapped("ZUnitConfigScanner", file))
}

/**
 * sortFileList
 * sort a list, putting the lines that defines files mapped as zUnit CFG files to the end
 */
def sortFileList(list) {
	def mapping = new PropertyMappings("dbb.scannerMapping")
	list.sort{s1, s2 ->
		if (isMappedAsZUnitConfigFile(mapping, s1)) {
			if (isMappedAsZUnitConfigFile(mapping, s2)) {
				return 0;
			} else {
				return 1;
			}
		} else {
			if (isMappedAsZUnitConfigFile(mapping, s2)) {
				return -1;
			} else {
				return 0;
			}
		}
	}
}
