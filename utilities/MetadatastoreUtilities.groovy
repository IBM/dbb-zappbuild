@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.metadata.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import java.nio.file.PathMatcher
import groovy.transform.*
import com.ibm.dbb.dependency.internal.*

@Field BuildProperties props = BuildProperties.getInstance()
@Field def matcherUtils= loadScript(new File("MatcherUtilities.groovy"))
@Field def dependencyScannerUtils= loadScript(new File("DependencyScannerUtilities.groovy"))

// Utilities to interact with the DBB Metadatastore
//
//  updateCollection(changedFiles, deletedFiles, renamedFiles)
//  saveStaticLinkDependencies(String buildFile, String loadPDS, LogicalFile logicalFile)
//  verifyCollections()
//  updateBuildResult

/*
 * Update DBB Collection in DBB Metadatastore for changed, deleted and renamed file
 * 
 */

def updateCollection(changedFiles, deletedFiles, renamedFiles) {

	if (!MetadataStoreFactory.metadataStoreExists()) {
		if (props.verbose) println "** Unable to update collections. No Metadata Store."
		return
	}

	MetadataStore metadataStore = MetadataStoreFactory.getMetadataStore()

	if (props.verbose) println "** Updating collections ${props.applicationCollectionName} and ${props.applicationOutputsCollectionName}"
	//def scanner = new DependencyScanner()
	List<LogicalFile> logicalFiles = new ArrayList<LogicalFile>()
	List<PathMatcher> excludeMatchers = matcherUtils.createPathMatcherPattern(props.excludeFileList)

	verifyCollections()

	// remove deleted files from collection
	deletedFiles.each { file ->
		// files in a collection are stored as relative paths from a source directory
		if (props.verbose) println "*** Deleting logical file for $file"
		metadataStore.getCollection(props.applicationCollectionName).deleteLogicalFile(file)
		metadataStore.getCollection(props.applicationOutputsCollectionName).deleteLogicalFile(file)
	}

	// remove renamed files from collection
	renamedFiles.each { file ->
		// files in a collection are stored as relative paths from a source directory
		if (props.verbose) println "*** Deleting renamed logical file for $file"
		metadataStore.getCollection(props.applicationCollectionName).deleteLogicalFile(file)
		metadataStore.getCollection(props.applicationOutputsCollectionName).deleteLogicalFile(file)
	}

	if (props.createTestcaseDependency && props.createTestcaseDependency.toBoolean() && changedFiles && changedFiles.size() > 1) {
		changedFiles = sortFileList(changedFiles);
		if (props.verbose) println "*** Sorted list of changed files: $changedFiles"
	}

	// scan changed files
	changedFiles.each { file ->

		// make sure file is not an excluded file
		if ( new File("${props.workspace}/${file}").exists() && !matcherUtils.matches(file, excludeMatchers)) {
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
				updateBuildResult(warningMsg:warningMsg)
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
		LogicalFile scannerLogicalFile = scanner.scan(buildFile, loadPDS)
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
 * updateBuildResult - update the build result after a build step
 */
def updateBuildResult(Map args) {
	// args : errorMsg / warningMsg, logs[logName:logFile]
	MetadataStore metadataStore = MetadataStoreFactory.getMetadataStore()
	// update build results only in non-userbuild scenarios
	if (metadataStore && !props.userBuild) {
		def buildResult = metadataStore.getBuildResult(props.applicationBuildGroup, props.applicationBuildLabel)
		if (!buildResult) {
			println "*! No build result found for BuildGroup '${props.applicationBuildGroup}' and BuildLabel '${props.applicationBuildLabel}'"
			return
		}

		// add error message
		if (args.errorMsg) {
			buildResult.setStatus(buildResult.ERROR)
			buildResult.addProperty("error", args.errorMsg)
			errorSummary = (props.errorSummary) ?  "${props.errorSummary}   ${args.errorMsg}\n" : "   ${args.errorMsg}\n"
			props.put("errorSummary", "$errorSummary")
		}

		// add warning message, but keep result status
		if (args.warningMsg) {
			// buildResult.setStatus(buildResult.WARNING)
			buildResult.addProperty("warning", args.warningMsg)

		}

		// add logs
		if (args.logs) {
			args.logs.each { logName, logFile ->
				if (logFile)
					buildResult.addAttachment(logName, new FileInputStream(logFile))
			}
		}

	}
}


// Internal methods

/**
 * createPropertyDependency
 * method to add a dependency to a property key
 */
def createPropertyDependency(String buildFile, LogicalFile logicalFile){
	if (props.verbose) println "*** Adding LogicalDependencies for Build Properties for $buildFile"
	// get language prefix
	def scriptMapping = ScriptMappings.getScriptName(buildFile)
	if(scriptMapping != null){
		def langPrefix = scriptMapping.takeWhile{it != '.' && it != '_'}.toLowerCase()
		// language COB
		if (langPrefix != null ){
			// generic properties
			if (props."${langPrefix}_impactPropertyList"){
				addBuildPropertyDependencies(props."${langPrefix}_impactPropertyList", logicalFile)
			}
			// cics properties
			if (isCICS(logicalFile) && props."${langPrefix}_impactPropertyListCICS") {
				addBuildPropertyDependencies(props."${langPrefix}_impactPropertyListCICS", logicalFile)
			}
			// sql properties
			if (isSQL(logicalFile) && props."${langPrefix}_impactPropertyListSQL") {
				addBuildPropertyDependencies(props."${langPrefix}_impactPropertyListSQL", logicalFile)
			}
		}

	}
}

// Internal methods

/*
 * isCICS - tests to see if the program is a CICS program. If the logical file is false, then
 * check to see if there is a file property.
 */
def isCICS(LogicalFile logicalFile) {
	boolean isCICS = logicalFile.isCICS()
	if (!isCICS) {
		String cicsFlag = props.getFileProperty('isCICS', logicalFile.getFile())
		if (cicsFlag)
			isCICS = cicsFlag.toBoolean()
	}

	return isCICS
}

/*
 * isSQL - tests to see if the program is an SQL program. If the logical file is false, then
 * check to see if there is a file property.
 */
def isSQL(LogicalFile logicalFile) {
	boolean isSQL = logicalFile.isSQL()
	if (!isSQL) {
		String sqlFlag = props.getFileProperty('isSQL', logicalFile.getFile())
		if (sqlFlag)
			isSQL = sqlFlag.toBoolean()
	}

	return isSQL
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