@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.metadata.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher
import groovy.json.JsonSlurper
import groovy.transform.*
import java.util.regex.*
import com.ibm.dbb.dependency.internal.*

// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def gitUtils= loadScript(new File("GitUtilities.groovy"))
@Field def buildUtils= loadScript(new File("BuildUtilities.groovy"))
@Field def dependencyScannerUtils= loadScript(new File("DependencyScannerUtilities.groovy"))
@Field String hashPrefix = ':githash:'
@Field def resolverUtils


def createImpactBuildList() {
	MetadataStore metadataStore = MetadataStoreFactory.getMetadataStore()
	
	// local variables
	Set<String> changedFiles = new HashSet<String>()
	Set<String> deletedFiles = new HashSet<String>()
	Set<String> renamedFiles = new HashSet<String>()
	Set<String> changedBuildProperties = new HashSet<String>()
	Set<String> buildSet = new HashSet<String>()
	
	boolean calculatedChanges = true 

	// get the last build result to get the baseline hashes
	def lastBuildResult = buildUtils.retrieveLastBuildResult()

	// calculate changed files
	if (lastBuildResult || props.baselineRef) {
		(changedFiles, deletedFiles, renamedFiles, changedBuildProperties) = calculateChangedFiles(lastBuildResult)
	}
	else {
		// else create a fullBuild list
		println "*! No prior build result located.  Creating a full build list."
		changedFiles = buildUtils.createFullBuildList()
		buildSet = changedFiles
		
		// skip impact calculation and return the generated build list
		calculatedChanges = false
	}

	// scan files and update source collection for impact analysis
	updateCollection(changedFiles, deletedFiles, renamedFiles)


	if (calculatedChanges) {

		// create build list using impact analysis
		if (props.verbose) println "*** Perform impacted analysis for changed files."

		PropertyMappings githashBuildableFilesMap = new PropertyMappings("githashBuildableFilesMap")


		changedFiles.each { changedFile ->
			// if the changed file has a build script then add to build list
			if (ScriptMappings.getScriptName(changedFile)) {
				// skip adding generated test cases, when the testing is disabled 
				if (buildUtils.isGeneratedzUnitTestCaseProgram(changedFile) && !(props.runzTests && props.runzTests.toBoolean())) {
					if (props.verbose) println "** Identified $changedFile as a generated zunit test case program. Processing zUnit tests is not enabled for this build. Skip building this program."
				} else {
					buildSet.add(changedFile)
					if (props.verbose) println "** Found build script mapping for $changedFile. Adding to build list"
				}
			}

			// check if impact calculation should be performed, default true
			if (shouldCalculateImpacts(changedFile)){

				// perform impact analysis on changed file
				if (props.verbose) println "** Performing impact analysis on changed file $changedFile"

				// get exclude list
				List<PathMatcher> excludeMatchers = buildUtils.createPathMatcherPattern(props.excludeFileList)

				// list of impacts
				String impactSearch = props.getFileProperty('impactSearch', changedFile)
				def impacts = findImpactedFiles(impactSearch, changedFile)
				

				impacts.each { impact ->
					def impactFile = impact.getFile()
					if (props.verbose) println "** Found impacted file $impactFile"
					// only add impacted files that have a build script mapped to it
					if (ScriptMappings.getScriptName(impactFile)) {
						// only add impacted files, that are in scope of the build.
						if (!buildUtils.matches(impactFile, excludeMatchers)){

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
							if (props.verbose) println "*! $impactFile is impacted by changed file $changedFile, but it is excluded from the build scope. See excludeFileList configuration. Not added to build list."
						}
					} else {
						String warningMsg = "*! $impactFile is impacted by changed file $changedFile, but is not added to build list, because it is not mapped to a language script."
						buildUtils.updateBuildResult(warningMsg:warningMsg)
						println(warningMsg)
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
					logicalFileList = metadataStore.getCollection(props.applicationCollectionName).getLogicalFiles(lDependency)


					// get excludeListe
					List<PathMatcher> excludeMatchers = buildUtils.createPathMatcherPattern(props.excludeFileList)

					logicalFileList.each { logicalFile ->
						def impactFile = logicalFile.getFile()
						if (props.verbose) println "** Found impacted file $impactFile"
						// only add impacted files that have a build script mapped to it
						if (ScriptMappings.getScriptName(impactFile)) {
							// only add impacted files, that are in scope of the build.
							if (!buildUtils.matches(impactFile, excludeMatchers)){
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

	}

	return [buildSet, changedFiles, deletedFiles, renamedFiles, changedBuildProperties]
}


/*
 * createMergeBuildList - calculates the changed and deleted files flowing back to the mainBuildBranch
 *  implements the build type --mergeBuild
 *
 */

def createMergeBuildList(){
	Set<String> changedFiles = new HashSet<String>()
	Set<String> deletedFiles = new HashSet<String>()
	Set<String> renamedFiles = new HashSet<String>()
	Set<String> changedBuildProperties = new HashSet<String>()

	(changedFiles, deletedFiles, renamedFiles, changedBuildProperties) = calculateChangedFiles(null)

	// scan files and update source collection
	updateCollection(changedFiles, deletedFiles, renamedFiles)

	// iterate over changed file and add them to the buildSet

	Set<String> buildSet = new HashSet<String>()


	changedFiles.each { changedFile ->
		// if the changed file has a build script then add to build list
		if (ScriptMappings.getScriptName(changedFile)) {
			buildSet.add(changedFile)
			if (props.verbose) println "** Found build script mapping for $changedFile. Adding to build list"
		}
	}

	return [buildSet, changedFiles, deletedFiles, renamedFiles, changedBuildProperties]
}

/*
 * findImpactedFiles -
 *  method to configure and invoke SearchPathImpactFinder
 *
 *  @return list of impacted files
 *
 */
def findImpactedFiles(String impactSearch, String changedFile) {
	
	List<String> collections = new ArrayList<String>()
	collections.add(props.applicationCollectionName)
	collections.add(props.applicationOutputsCollectionName)
	
	if (props.verbose)
		println ("*** Creating SearchPathImpactFinder with collections " + collections + " and impactSearch configuration " + impactSearch)
	
	def finder = new SearchPathImpactFinder(impactSearch, collections)
	
	// Find all files impacted by the changed file
	impacts = finder.findImpactedFiles(changedFile, props.workspace)
	return impacts
}


/*
 * calculateChangedFiles - method to caluclate the the changed files
 *
 */

def calculateChangedFiles(BuildResult lastBuildResult) {
	return calculateChangedFiles(lastBuildResult, false, null)
}

/*
 * calculateChangedFiles -
 *   this method is used for zAppBuild built modes to
 * 	  calculate changed files and
 *    return a list of identified changed, renamed, deleted and modified build properties
 *
 *  High-Level flow for
 *   - impactBuild
 *  	parms: requires to pass the lastBuildResult
 *      flow:  obtains current hash for directories
 *      	   obtains the baseline hash/es for the different referenced directories
 *             performs a git diff between current and baseline
 *             calculates the correct offset from the git diff
 *             stores the abbreviated hash for changed files in PropertyMapping
 *
 *   - impactBuild with baselineReference
 *      parms: requires to pass the lastBuildResult
 *      flow:  obtains current hash for directories
 *             obtains the baseline hash for the directories
 *             performs a git diff between current and baselineReference
 *             calculates the correct offset from the git diff
 *             stores the abbreviated hash for changed files in PropertyMapping
 *
 *   - mergeBuild
 *      parms: no build result is passed to the method
 *      flow:  obtains current hash for directories
 *             no calculation of baseline hash for the different directories
 *             performs a git diff between current and mainBuildBranch
 *             calculates the correct offset from the git diff
 *             stores the abbreviated hash for changed files in PropertyMapping
 *
 *   - concurrentChangesAnalysis
 *      parms: no build result is passed to the method, calculateConcurrentChanges=true, gitReference containing the git configuration
 *      flow:  no calculation of baseline hash for the directories
 *             performs a git diff between HEAD and the passed gitReference
 *             calculates the correct offset from the git diff
 *
 *   @return the set for changed, renamed, deleted and modified build properties to caller
 */

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
		List<PathMatcher> excludeMatchers = buildUtils.createPathMatcherPattern(props.excludeFileList)

		if (props.verbose) println "*** Changed files for directory $dir $msg:"
		changed.each { file ->
			(file, mode) = fixGitDiffPath(file, dir, true, null)
			if ( file != null ) {
				// filter excluded files
				if ( !buildUtils.matches(file, excludeMatchers)) {
					changedFiles << file
					if (!calculateConcurrentChanges) githashBuildableFilesMap.addFilePattern(abbrevCurrent, file)
					if (props.verbose) println "**** $file"
				} else {
					if (props.verbose) println "**** $file is changed, but is excluded from build scope. See excludeFileList configuration."
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
			if ( !buildUtils.matches(file, excludeMatchers)) {
				(file, mode) = fixGitDiffPath(file, dir, false, mode)
				deletedFiles << file
				if (props.verbose) println "**** $file"
			} else {
				if (props.verbose) println "**** $file is deleted, but is excluded from build scope. See excludeFileList configuration. No follow-up processing."
			}
		}

		if (props.verbose) println "*** Renamed files for directory $dir $msg:"
		renamed.each { file ->
			if ( !buildUtils.matches(file, excludeMatchers)) {
				(file, mode) = fixGitDiffPath(file, dir, false, mode)
				renamedFiles << file
				if (props.verbose) println "**** $file"
			} else {
				if (props.verbose) println "**** $file is renamed, but is excluded from build scope. See excludeFileList configuration. No follow-up processing."
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
def scanOnlyStaticDependencies(List buildList){
	buildList.each { buildFile ->
		def scriptMapping = ScriptMappings.getScriptName(buildFile)
		if(scriptMapping != null){
			langPrefix = buildUtils.getLangPrefix(scriptMapping)
			if(langPrefix != null){
				String isLinkEdited = props.getFileProperty("${langPrefix}_linkEdit", buildFile)

				def scanner = dependencyScannerUtils.getScanner(buildFile)
				LogicalFile logicalFile = scanner.scan(buildFile, props.workspace)

				String member = CopyToPDS.createMemberName(buildFile)
				String loadPDSMember = props."${langPrefix}_loadPDS"+"($member)"

				if ((isLinkEdited && isLinkEdited.toBoolean()) || scriptMapping == "LinkEdit.groovy"){
					try{
						if (props.verbose) println ("*** Scanning load module $loadPDSMember of $buildFile")
						saveStaticLinkDependencies(buildFile, props."${langPrefix}_loadPDS", logicalFile)
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





def updateCollection(changedFiles, deletedFiles, renamedFiles) {

	if (!MetadataStoreFactory.metadataStoreExists()) {
		if (props.verbose) println "** Unable to update collections. No Metadata Store."
		return
	}
	MetadataStore metadataStore = MetadataStoreFactory.getMetadataStore()

	if (props.verbose) println "** Updating collections ${props.applicationCollectionName} and ${props.applicationOutputsCollectionName}"
	//def scanner = new DependencyScanner()
	List<LogicalFile> logicalFiles = new ArrayList<LogicalFile>()
	List<PathMatcher> excludeMatchers = buildUtils.createPathMatcherPattern(props.excludeFileList)

	verifyCollections()

	// remove deleted files from collection
	deletedFiles.each { file ->
		// files in a collection are stored as relative paths from a source directory
		if (props.verbose) println "*** Deleting logical file for $file"
		logicalFile = buildUtils.relativizePath(file)
		metadataStore.getCollection(props.applicationCollectionName).deleteLogicalFile(logicalFile)
		metadataStore.getCollection(props.applicationOutputsCollectionName).deleteLogicalFile(logicalFile)
	}

	// remove renamed files from collection
	renamedFiles.each { file ->
		// files in a collection are stored as relative paths from a source directory
		if (props.verbose) println "*** Deleting renamed logical file for $file"
		logicalFile = buildUtils.relativizePath(file)
		metadataStore.getCollection(props.applicationCollectionName).deleteLogicalFile(logicalFile)
		metadataStore.getCollection(props.applicationOutputsCollectionName).deleteLogicalFile(logicalFile)
	}

	if (props.createTestcaseDependency && props.createTestcaseDependency.toBoolean() && changedFiles && changedFiles.size() > 1) {
		changedFiles = sortFileList(changedFiles);
		if (props.verbose) println "*** Sorted list of changed files: $changedFiles"
	}

	// scan changed files
	changedFiles.each { file ->

		// make sure file is not an excluded file
		if ( new File("${props.workspace}/${file}").exists() && !buildUtils.matches(file, excludeMatchers)) {
			// files in a collection are stored as relative paths from a source directory

			def scanner = dependencyScannerUtils.getScanner(file)
			try {
				def logicalFile
				if (scanner != null) {
					if (props.verbose) println "*** Scanning file $file (${props.workspace}/${file} with ${scanner.getClass()})"
					logicalFile = scanner.scan(file, props.workspace)
				} else {
					// The below logic should be replaced with Registration Scanner when available
					// See reported idea: https://ibm-z-software-portal.ideas.ibm.com/ideas/DBB-I-48
					if (props.verbose) println "*** Skipped scanning file $file (${props.workspace}/${file})"
					
					// New logical file with Membername, buildfile, language set to file extension
					logicalFile = new LogicalFile(CopyToPDS.createMemberName(file), file, file.substring(file.lastIndexOf(".") + 1).toUpperCase(), false, false, false)
					
					// Add logicalFile to LogicalFileCache
					LogicalFileCache.add(props.workspace, logicalFile)
				}
				if (props.verbose) println "*** Logical file for $file =\n$logicalFile"

				// Update logical file with dependencies to build properties
				if (props.impactBuildOnBuildPropertyChanges && props.impactBuildOnBuildPropertyChanges.toBoolean()){
					createPropertyDependency(file, logicalFile)
				}

				// If configured, update test case program dependencies
				if (props.createTestcaseDependency && props.createTestcaseDependency.toBoolean()) {
					// If the file is a zUnit configuration file (BZUCFG)
					if (scanner != null && scanner.getClass() == com.ibm.dbb.dependency.ZUnitConfigScanner) {

						def logicalDependencies = logicalFile.getLogicalDependencies()

						def sysTestDependency = logicalDependencies.find{it.getLibrary().equals("SYSTEST")} // Get the test case program from testcfg
						def sysProgDependency = logicalDependencies.find{it.getLibrary().equals("SYSPROG")} // Get the application program name from testcfg

						if (sysTestDependency){
							// find in local list of logical files first (batch processing)
							def testCaseFiles = logicalFiles.findAll{it.getLname().equals(sysTestDependency.getLname())}
							if (!testCaseFiles){ // alternate retrieve it from the collection
								testCaseFiles = metadataStore.getCollection(props.applicationCollectionName).getLogicalFiles(sysTestDependency.getLname()).find{
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
				buildUtils.updateBuildResult(warningMsg:warningMsg)
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
					println "** Storing ${logicalFiles.size()} logical files in MetadataStore collection '$props.applicationCollectionName'"
				metadataStore.getCollection(props.applicationCollectionName).addLogicalFiles(logicalFiles)
				logicalFiles.clear()
			}
		}
	}

	// save logical files
	if (props.verbose)
		println "** Storing ${logicalFiles.size()} logical files in MetadataStore collection '$props.applicationCollectionName'"
	metadataStore.getCollection(props.applicationCollectionName).addLogicalFiles(logicalFiles)
	
}

/*
 * saveStaticLinkDependencies - Scan the load module to determine LINK dependencies. Impact resolver can use
 * these to determine that this file gets rebuilt if a LINK dependency changes.
 */
def saveStaticLinkDependencies(String buildFile, String loadPDS, LogicalFile logicalFile) {
	MetadataStore metadataStore = MetadataStoreFactory.getMetadataStore()
	if (metadataStore && !props.error && !props.preview) {
		LinkEditScanner scanner = new LinkEditScanner()
		if (props.verbose) println "*** Scanning load module for $buildFile"
		LogicalFile scannerLogicalFile = scanner.scan(buildUtils.relativizePath(buildFile), loadPDS)
		if (props.verbose) println "*** Logical file = \n$scannerLogicalFile"

		// overwrite original logicalDependencies with load module dependencies
		logicalFile.setLogicalDependencies(scannerLogicalFile.getLogicalDependencies())

		// Store logical file and indirect dependencies to the outputs collection
		metadataStore.getCollection("${props.applicationOutputsCollectionName}").addLogicalFile( logicalFile );
	}
}

/*
 * verifyCollections - verifies that the application collections exists. If not it will
 * create or clone the collections.
 * Uses build properties
 */
def verifyCollections() {
	MetadataStore metadataStore = MetadataStoreFactory.getMetadataStore()
	if (!metadataStore) {
		if (props.verbose) println "** Unable to verify collections. No metadata store."
		return
	}

	String mainCollectionName = "${props.application}-${props.mainBuildBranch}"
	String mainOutputsCollectionName = "${props.application}-${props.mainBuildBranch}-outputs"

	// check source collection
	if (!metadataStore.collectionExists(props.applicationCollectionName)) {
		if (props.topicBranchBuild) {
			if (metadataStore.collectionExists(mainCollectionName)) {
				metadataStore.copyCollection(mainCollectionName, props.applicationCollectionName)
				if (props.verbose) println "** Cloned collection ${props.applicationCollectionName} from $mainCollectionName"
			}
			else {
				metadataStore.createCollection(props.applicationCollectionName)
				if (props.verbose) println "** Created collection ${props.applicationCollectionName}"
			}
		}
		else {
			metadataStore.createCollection(props.applicationCollectionName)
			if (props.verbose) println "** Created collection ${props.applicationCollectionName}"
		}
	}

	// check outputs collection
	if (!metadataStore.collectionExists(props.applicationOutputsCollectionName)) {
		if (props.topicBranchBuild) {
			if (metadataStore.collectionExists(mainOutputsCollectionName)) {
				metadataStore.copyCollection("${mainOutputsCollectionName}", props.applicationOutputsCollectionName)
				if (props.verbose) println "** Cloned collection ${props.applicationOutputsCollectionName} from $mainOutputsCollectionName"
			}
			else {
				metadataStore.createCollection(props.applicationOutputsCollectionName)
				if (props.verbose) println "** Created collection ${props.applicationOutputsCollectionName}"
			}
		}
		else {
			metadataStore.createCollection(props.applicationOutputsCollectionName)
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
 *  4 - Combination 2 (reponame as application root dir, no common root) and scoped applicationSrcDirs
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

	// Scenario 4:
	//    Repository name is used as application root directory and
	//      applicationSrcDirs is scoping the build scope by filtering on a subdirectory
	//        applicationSrcDirs=nazare-demo-genapp/src
	fixedFileName = "${props.application}/$file"
	if ( new File("${props.workspace}/${fixedFileName}").exists())
		return [fixedFileName, 4];
	if (mode==4 && !mustExist) return [fixedFileName, 4]
	
	// returns null or assumed fullPath to file
	if (mustExist){
		if (props.verbose) println "*! (ImpactUtilities.fixGitDiffPath) directory offset for file $file in dir $dir not found."
		return [null, null]
	}

	if (props.verbose) println "*! (ImpactUtilities.fixGitDiffPath) Mode could not be determined. Returning default."
	return [defaultValue, null]
}

/**
 *  shouldCalculateImpacts
 *
 *  Method to calculate if impact analysis should be performed for a changedFile in an impactBuild scenario
 *   returns a boolean - default true
 */
def boolean shouldCalculateImpacts(String changedFile){
	// retrieve Pathmaters from property and check
	List<PathMatcher> nonImpactingFiles = buildUtils.createPathMatcherPattern(props.skipImpactCalculationList)
	onskipImpactCalculationList = buildUtils.matches(changedFile, nonImpactingFiles)

	// return false if changedFile found in skipImpactCalculationList
	if (onskipImpactCalculationList) return false
	
	// return false if the changed file is a generated test case program but testing is disabled
	if (buildUtils.isGeneratedzUnitTestCaseProgram(changedFile) && !(props.runzTests && props.runzTests.toBoolean())) {
		return false
	}
	
	return true //default
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
 * sortFileList
 * sort a list, putting the lines that defines files mapped as zUnit CFG files to the end
 */
def sortFileList(list) {
	
	return list.sort{s1, s2 ->
		if (isMappedAsZUnitConfigFile(s1)) {
			if (isMappedAsZUnitConfigFile(s2)) {
				return 0;
			} else {
				return 1;
			}
		} else {
			if (isMappedAsZUnitConfigFile(s2)) {
				return -1;
			} else {
				return 0;
			}
		}
	}
}

/**
 * isMappedAsZUnitConfigFile
 * method to check if a file is mapped with the zUnitConfigScanner, indicating it's a zUnit CFG file
 */
def isMappedAsZUnitConfigFile(String file) {
	return (dependencyScannerUtils.getScanner(file).getClass() == com.ibm.dbb.dependency.ZUnitConfigScanner)
}