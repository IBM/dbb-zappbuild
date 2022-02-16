@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.repository.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*
import groovy.json.JsonParserType
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import com.ibm.dbb.build.DBBConstants.CopyMode
import com.ibm.dbb.build.report.records.*
import com.ibm.jzos.FileAttribute
import groovy.ant.FileNameFinder

// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field HashSet<String> copiedFileCache = new HashSet<String>()
@Field def gitUtils = loadScript(new File("GitUtilities.groovy"))



/*
 * assertBuildProperties - verify that required build properties for a script exist
 */
def assertBuildProperties(String requiredProps) {
	if (props.verbose) println "required props = $requiredProps"
	if (requiredProps) {
		String[] buildProps = requiredProps.split(',')

		buildProps.each { buildProp ->
			buildProp = buildProp.trim()
			assert props."$buildProp" : "*! Missing required build property '$buildProp'"
		}
	}
}

/*
 * createFullBuildList() - returns all existing files of the build workspace for the --fullBuild build type
 * 
 */
def createFullBuildList() {
	Set<String> buildSet = new HashSet<String>()
	
	// PropertyMappings
	PropertyMappings githashBuildableFilesMap = new PropertyMappings("githashBuildableFilesMap")
	
	// create the list of build directories
	List<String> srcDirs = []
	if (props.applicationSrcDirs)
		srcDirs.addAll(props.applicationSrcDirs.split(','))

	srcDirs.each{ dir ->
		dir = getAbsolutePath(dir)
		Set<String> fileSet =getFileSet(dir, true, '**/*.*', props.excludeFileList)
		buildSet.addAll(fileSet)
		
		// capture abbreviated gitHash for all buildable files
		String abbrevHash = gitUtils.getCurrentGitHash(dir, true)
		buildSet.forEach { buildableFile ->
			githashBuildableFilesMap.addFilePattern(abbrevHash, buildableFile)
		}
		
	}

	return buildSet
}

/*
 * getFileSet - create a list of files for a directory
 */
def getFileSet(String dir, boolean relativePaths, String includeFileList, String excludeFileList) {
	Set<String> fileSet = new HashSet<String>()

	def files = new FileNameFinder().getFileNames(dir, includeFileList, excludeFileList)
	files.each { file ->
		if (relativePaths)
			fileSet.add(relativizePath(file))
		else
			fileSet.add(file)
	}

	return fileSet
}

/*
 * copySourceFiles - copies both the program being built and the program
 * dependencies from USS directories to data sets
 * 
 * parameters:
 *  - build file
 *  - target dataset for build file
 *  - name of the DBB PropertyMapping for dependencies (optional)
 *  - name of the map for alternate library names for PLI and COBOL (optional)
 *  - DependencyResolver to resolve dependencies
 */

def copySourceFiles(String buildFile, String srcPDS, String dependencyDatasetMapping, String dependenciesAlternativeLibraryNameMapping, DependencyResolver dependencyResolver) {
	// only copy the build file once
	if (!copiedFileCache.contains(buildFile)) {
		copiedFileCache.add(buildFile)
		new CopyToPDS().file(new File(getAbsolutePath(buildFile)))
				.dataset(srcPDS)
				.member(CopyToPDS.createMemberName(buildFile))
				.execute()
	}
	
	if (dependencyDatasetMapping && props.userBuildDependencyFile && props.userBuild) {
		if (props.verbose) println "*** User Build Dependency File Detected. Skipping DBB Dependency Resolution."
		// userBuildDependencyFile present (passed from the IDE)
		// Skip dependency resolution, extract dependencies from userBuildDependencyFile, and copy directly dataset
		// Load property mapping containing the map of targetPDS and dependencyfile
		PropertyMappings dependenciesDatasetMapping = new PropertyMappings(dependencyDatasetMapping)
		
		// parse JSON and validate fields of userBuildDependencyFile
		def depFileData = validateDependencyFile(buildFile, props.userBuildDependencyFile)

		// Manually create logical file for the user build program
		String lname = CopyToPDS.createMemberName(buildFile)
		String language = props.getFileProperty('dbb.DependencyScanner.languageHint', buildFile) ?: 'UNKN'
		LogicalFile lfile = new LogicalFile(lname, buildFile, language, depFileData.isCICS, depFileData.isSQL, depFileData.isDLI)
		// save logical file to dependency resolver
		if (dependencyResolver)
			dependencyResolver.setLogicalFile(lfile)

		// get list of dependencies from userBuildDependencyFile
		List<String> dependencyPaths = depFileData.dependencies

		// copy each dependency from USS to member of depedencyPDS
		dependencyPaths.each { dependencyPath ->
			// if dependency is relative, convert to absolute path
			String dependencyLoc = getAbsolutePath(dependencyPath)

			// Assume library is SYSLIB for all dependencies
			String dependencyPDS = props.getProperty(dependenciesDatasetMapping.getValue(dependencyPath))

			// only copy the dependency file once per script invocation
			if (!copiedFileCache.contains(dependencyLoc)) {
				copiedFileCache.add(dependencyLoc)
				// create member name
				String memberName = CopyToPDS.createMemberName(dependencyPath)
				// retrieve zUnit playback file extension
				zunitFileExtension = (props.zunit_playbackFileExtension) ? props.zunit_playbackFileExtension : null
				// get index of last '.' in file path to extract the file extension
				def extIndex = dependencyLoc.lastIndexOf('.')
				if( zunitFileExtension && !zunitFileExtension.isEmpty() && (dependencyLoc.substring(extIndex).contains(zunitFileExtension))){
					new CopyToPDS().file(new File(dependencyLoc))
							.copyMode(CopyMode.BINARY)
							.dataset(dependencyPDS)
							.member(memberName)
							.execute()
				}
				else
				{
					new CopyToPDS().file(new File(dependencyLoc))
							.dataset(dependencyPDS)
							.member(memberName)
							.execute()
				}
			}
		}
	}
	else if (dependencyDatasetMapping && dependencyResolver) {
		// resolve the logical dependencies to physical files to copy to data sets
		List<PhysicalDependency> physicalDependencies = dependencyResolver.resolve()
		if (props.verbose) {
			println "*** Resolution rules for $buildFile:"
			
			if (props.formatConsoleOutput && props.formatConsoleOutput.toBoolean()) {
				printResolutionRules(dependencyResolver.getResolutionRules())
			} else {
				dependencyResolver.getResolutionRules().each{ rule -> println rule }
			}
		}
		if (props.verbose) println "*** Physical dependencies for $buildFile:"

		// Load property mapping containing the map of targetPDS and dependencyfile
		PropertyMappings dependenciesDatasetMapping = new PropertyMappings(dependencyDatasetMapping)
		
		if (physicalDependencies.size() != 0) {
			if (props.verbose && props.formatConsoleOutput && props.formatConsoleOutput.toBoolean()) {
				printPhysicalDependencies(physicalDependencies)
				}
		}
		
		physicalDependencies.each { physicalDependency ->
			if (props.verbose && !props.formatConsoleOutput && !props.formatConsoleOutput.toBoolean()) 	println physicalDependency
			
			if (physicalDependency.isResolved()) {

				// obtain target dataset based on Mappings
				// Order :
				//    1. langprefix_dependenciesAlternativeLibraryNameMapping based on the library setting recognized by DBB (COBOL and PLI)
				//    2. langprefix_dependenciesDatasetMapping as a manual overwrite to determine an alternative library used in the default dd concatentation 
				String dependencyPDS 
				if (!physicalDependency.getLibrary().equals("SYSLIB") && dependenciesAlternativeLibraryNameMapping) {
					dependencyPDS = props.getProperty(evaluate(dependenciesAlternativeLibraryNameMapping).get(physicalDependency.getLibrary()))
				}
				if (dependencyPDS == null && dependenciesDatasetMapping){
					dependencyPDS = props.getProperty(dependenciesDatasetMapping.getValue(physicalDependency.getFile()))
				}

				String physicalDependencyLoc = "${physicalDependency.getSourceDir()}/${physicalDependency.getFile()}"

				if (dependencyPDS != null) {

					// only copy the dependency file once per script invocation
					if (!copiedFileCache.contains(physicalDependencyLoc)) {
						copiedFileCache.add(physicalDependencyLoc)
						// create member name
						String memberName = CopyToPDS.createMemberName(physicalDependency.getFile())
						//retrieve zUnitFileExtension plbck
						zunitFileExtension = (props.zunit_playbackFileExtension) ? props.zunit_playbackFileExtension : null

						if( zunitFileExtension && !zunitFileExtension.isEmpty() && ((physicalDependency.getFile().substring(physicalDependency.getFile().indexOf("."))).contains(zunitFileExtension))){
							new CopyToPDS().file(new File(physicalDependencyLoc))
									.copyMode(CopyMode.BINARY)
									.dataset(dependencyPDS)
									.member(memberName)
									.execute()
						} else
						{
							new CopyToPDS().file(new File(physicalDependencyLoc))
									.dataset(dependencyPDS)
									.member(memberName)
									.execute()
						}
					}
				} else {
					String errorMsg = "*! Target dataset mapping for dependency ${physicalDependency.getFile()} could not be found in either in dependenciesAlternativeLibraryNameMapping (COBOL and PLI) or PropertyMapping $dependencyDatasetMapping"
					println(errorMsg)
					props.error = "true"
					updateBuildResult(errorMsg:errorMsg)
				}
			}
		}
	}
}

/*
 * sortBuildList - sorts a build list by rank property values
 */
def sortBuildList(List<String> buildList, String rankPropertyName) {
	List<String> sortedList = []
	TreeMap<Integer,List<String>> rankings = new TreeMap<Integer,List<String>>()
	List<String> unranked = new ArrayList<String>()

	// sort buildFiles by rank
	buildList.each { buildFile ->
		String rank = props.getFileProperty(rankPropertyName, buildFile)
		if (rank) {
			Integer rankNum = rank.toInteger()
			List<String> ranking = rankings.get(rankNum)
			if (!ranking) {
				ranking = new ArrayList<String>()
				rankings.put(rankNum,ranking)
			}
			ranking << buildFile
		}
		else {
			unranked << buildFile
		}
	}

	// loop through rank keys adding sub lists (TreeMap automatically sorts keySet)
	rankings.keySet().each { key ->
		List<String> ranking = rankings.get(key)
		if (ranking)
			sortedList.addAll(ranking)
	}

	// finally add unranked buildFiles
	sortedList.addAll(unranked)

	return sortedList
}

/*
 * updateBuildResult - used by language scripts to update the build result after a build step
 */
def updateBuildResult(Map args) {
	// args : errorMsg / warningMsg, logs[logName:logFile], client:repoClient

	// update build results only in non-userbuild scenarios
	if (args.client && !props.userBuild) {
		def buildResult = args.client.getBuildResult(props.applicationBuildGroup, props.applicationBuildLabel)
		if (!buildResult) {
			println "*! No build result found for BuildGroup '${props.applicationBuildGroup}' and BuildLabel '${props.applicationBuildLabel}'"
			return
		}

		// add error message
		if (args.errorMsg) {
			buildResult.setStatus(buildResult.ERROR)
			buildResult.addProperty("error", args.errorMsg)

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

		// save result
		buildResult.save()
	}
}

/*
 * createDependencyResolver - Creates a dependency resolver using resolution rules declared
 * in a build or file property (json format).
 */
def createDependencyResolver(String buildFile, String rules) {
	if (props.verbose) println "*** Creating dependency resolver for $buildFile with $rules rules"

	// create a dependency resolver for the build file
	DependencyResolver resolver = new DependencyResolver().file(buildFile)
			.sourceDir(props.workspace)
	
	// add scanner if userBuild Dep File not provided, or not a user build
	if (!props.userBuildDependencyFile || !props.userBuild)
		resolver.setScanner(getScanner(buildFile))

	// add resolution rules
	if (rules)
		resolver.setResolutionRules(parseResolutionRules(rules))

	return resolver
}

def parseResolutionRules(String json) {
	List<ResolutionRule> rules = new ArrayList<ResolutionRule>()
	JsonSlurper slurper = new groovy.json.JsonSlurper()
	List jsonRules = slurper.parseText(json)
	if (jsonRules) {
		jsonRules.each { jsonRule ->
			ResolutionRule resolutionRule = new ResolutionRule()
			resolutionRule.library(jsonRule.library)
			resolutionRule.lname(jsonRule.lname)
			resolutionRule.category(jsonRule.category)
			if (jsonRule.searchPath) {
				jsonRule.searchPath.each { jsonPath ->
					DependencyPath dependencyPath = new DependencyPath()
					dependencyPath.collection(jsonPath.collection)
					dependencyPath.sourceDir(jsonPath.sourceDir)
					dependencyPath.directory(jsonPath.directory)
					resolutionRule.path(dependencyPath)
				}
			}
			rules << resolutionRule
		}
	}
	return rules
}



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

/*
 * isDLI - tests to see if the program is a DL/I program. If the logical file is false, then
 * check to see if there is a file property.
 */
def isDLI(LogicalFile logicalFile) {
	boolean isDLI = logicalFile.isDLI()
	if (!isDLI) {
		String dliFlag = props.getFileProperty('isDLI', logicalFile.getFile())
		if (dliFlag)
			isDLI = dliFlag.toBoolean()
	}

	return isDLI
}

/*
 * getAbsolutePath - returns the absolute path of a relative (to workspace) file or directory
 */
def getAbsolutePath(String path) {
	path = path.trim()
	if (path.startsWith('/'))
		return path

	String workspace = props.workspace.trim()
	if (!workspace.endsWith('/'))
		workspace = "${workspace}/"

	return "${workspace}${path}"
}

/*
 * relativizePath - converts an absolute path to a relative path from the workspace directory
 */
def relativizePath(String path) {
	if (!path.startsWith('/'))
		return path
	String relPath = new File(props.workspace).toURI().relativize(new File(path.trim()).toURI()).getPath()
	// Directories have '/' added to the end.  Lets remove it.
	if (relPath.endsWith('/'))
		relPath = relPath.take(relPath.length()-1)
	return relPath
}

/*
 * relativizeFolderPath - converts a path to a relative path from folder
 */
def relativizeFolderPath(String folder, String path) {
	String fullPath = getAbsolutePath(path)
	String fullFolderPath = folder
	if (!folder.startsWith('/'))
		fullFolderPath = getAbsolutePath(folder)
	if (fullPath.startsWith(fullFolderPath))
		return fullPath.substring(fullFolderPath.length()+1)
	return path
}

/*
 * getScannerInstantiates - returns the mapped scanner or default scanner
 */
def getScanner(String buildFile){
	if (props.runzTests && props.runzTests.toBoolean()) {
		scannerUtils= loadScript(new File("ScannerUtilities.groovy"))
		scanner = scannerUtils.getScanner(buildFile)
	}
	else {
		if (props.verbose) println("*** Scanning file with the default scanner")
		scanner = new DependencyScanner()
	}
}

/*
 * createLanguageDatasets - gets the language used to create the datasets
 */
def createLanguageDatasets(String lang) {
	if (props."${lang}_srcDatasets")
		createDatasets(props."${lang}_srcDatasets".split(','), props."${lang}_srcOptions")

	if (props."${lang}_loadDatasets")
		createDatasets(props."${lang}_loadDatasets".split(','), props."${lang}_loadOptions")

	if (props."${lang}_reportDatasets")
		createDatasets(props."${lang}_reportDatasets".split(','), props."${lang}_reportOptions")

	if (props."${lang}_cexecDatasets")
		createDatasets(props."${lang}_cexecDatasets".split(','), props."${lang}_cexecOptions")
}

/*
 * createDatasets - creates the dataset for a particular language
 */
def createDatasets(String[] datasets, String options) {
	if (datasets && options) {
		datasets.each { dataset ->
			new CreatePDS().dataset(dataset.trim()).options(options.trim()).create()
			if (props.verbose)
				println "** Creating / verifying build dataset ${dataset}"
		}
	}
}

/*
 * returns languagePrefix for language script name or null if not defined.
 */
def getLangPrefix(String scriptName){
	def langPrefix = null
	switch(scriptName) {
		case "Cobol.groovy":
			langPrefix = 'cobol'
			break;
		case "LinkEdit.groovy" :
			langPrefix = 'linkedit'
			break;
		case "PLI.groovy":
			langPrefix = 'pli'
			break;
		case "Assembler.groovy":
			langPrefix = 'assembler'
			break;
		case "BMS.groovy":
			langPrefix = 'bms'
			break;
		case "DBDgen.groovy":
			langPrefix = 'dbdgen'
			break;
		case "MFS.groovy":
			langPrefix = 'mfs'
			break;
		case "PSBgen.groovy":
			langPrefix = 'psbgen'
			break;
		default:
			if (props.verbose) println ("*** ! No language prefix defined for $scriptName.")
			break;
	}
	return langPrefix
}

/*
 * retrieveLastBuildResult(RepositoryClient)
 * returns last successful build result
 *
 */
def retrieveLastBuildResult(RepositoryClient repositoryClient){

	// get the last build result
	def lastBuildResult = repositoryClient.getLastBuildResult(props.applicationBuildGroup, BuildResult.COMPLETE, BuildResult.CLEAN)

	if (lastBuildResult == null && props.topicBranchBuild){
		// if this is the first topic branch build get the main branch build result
		if (props.verbose) println "** No previous successful topic branch build result. Retrieving last successful main branch build result."
		String mainBranchBuildGroup = "${props.application}-${props.mainBuildBranch}"
		lastBuildResult = repositoryClient.getLastBuildResult(mainBranchBuildGroup, BuildResult.COMPLETE, BuildResult.CLEAN)
	}

	if (lastBuildResult == null) {
		println "*! No previous topic branch build result or main branch build result exists. Cannot calculate file changes."
	}

	return lastBuildResult
}

/*
 * returns the deployType for a logicalFile depending on the isCICS, isDLI setting
 */
def getDeployType(String langQualifier, String buildFile, LogicalFile logicalFile){
	// getDefault
	String deployType = props.getFileProperty("${langQualifier}_deployType", buildFile)
	if(deployType == null )
		deployType = 'LOAD'

	if (props."${langQualifier}_deployType" == deployType){ // check if a file level overwrite was used
		if (logicalFile != null){
			if(isCICS(logicalFile)){ // if CICS
				String cicsDeployType = props.getFileProperty("${langQualifier}_deployTypeCICS", buildFile)
				if (cicsDeployType != null) deployType = cicsDeployType
			} else if (isDLI(logicalFile)){
				String dliDeployType = props.getFileProperty("${langQualifier}_deployTypeDLI", buildFile)
				if (dliDeployType != null) deployType = dliDeployType
			}
		}
	} else{
		// a file level overwrite was used
	}
	return deployType
}

/*
 * Creates a Generic PropertyRecord with the provided db2 information in bind.properties
 */
def generateDb2InfoRecord(String buildFile){
	
	// New Generic Property Record
	PropertiesRecord db2BindInfo = new PropertiesRecord("db2BindInfo:${buildFile}")
	
	// Link to buildFile
	db2BindInfo.addProperty("file", buildFile)

	// Iterate over list of Db2InfoRecord properties
	if (props.generateDb2BindInfoRecordProperties) {
		String[] generateDb2InfoRecordPropertiesList = props.getFileProperty("generateDb2BindInfoRecordProperties", buildFile).split(',')
		generateDb2InfoRecordPropertiesList.each { db2Prop ->
			// Add all properties, which are defined for bind - see application-conf/bind.properties
			String bindPropertyValue = props.getFileProperty("${db2Prop}", buildFile)
			if (bindPropertyValue != null ) db2BindInfo.addProperty("${db2Prop}",bindPropertyValue)
		}
	}
		
	return db2BindInfo		
}

/*
 * Parses and validates the user build dependency file 
 * returns a parsed json object 
 */
def validateDependencyFile(String buildFile, String depFilePath) {
	String[] allowedEncodings = ["UTF-8", "IBM-1047"]
	String[] reqDepFileProps = ["fileName", "isCICS", "isSQL", "isDLI", "isMQ", "dependencies", "schemaVersion"]
	
	// Load dependency file and verify existance
	File depFile = new File(getAbsolutePath(depFilePath))
	assert depFile.exists() : "*! Dependency file not found: ${depFile.getAbsolutePath()}"
	
	// Parse the JSON file
	String encoding = retrieveHFSFileEncoding(depFile) // Determine the encoding from filetag
	JsonSlurper slurper = new JsonSlurper().setType(JsonParserType.INDEX_OVERLAY) // Use INDEX_OVERLAY, fastest parser
	def depFileData
	if (encoding) {
		if (props.verbose) println "Parsing dependency file as ${encoding}: "
		assert allowedEncodings.contains(encoding) : "*! Dependency file must be encoded and tagged as either UTF-8 or IBM-1047 but was ${encoding}"
		depFileData = slurper.parse(depFile, encoding) // Parse dependency file with encoding
	}
	else {
		if (props.verbose) println "[WARNING] Dependency file is untagged. \nParsing dependency file with default system encoding: "
		depFileData = slurper.parse(depFile) // Assume default encoding for system
	}
	if (props.verbose) println new JsonBuilder(depFileData).toPrettyString() // Pretty print if verbose
	
	// Validate JSON structure
	reqDepFileProps.each { depFileProp ->
		assert depFileData."${depFileProp}" != null : "*! Missing required dependency file field '$depFileProp'"
	}
	// Validate depFileData.fileName == buildFile
	assert getAbsolutePath(depFileData.fileName) == getAbsolutePath(buildFile) : "*! Dependency file mismatch: fileName does not match build file"
	return depFileData // return the parsed JSON object
}

/*
 * Validates the current Dbb Toolkit version
 * exits the process, if it does not meet the minimum required version of zAppBuild.
 * 
 */
def assertDbbBuildToolkitVersion(String currentVersion){

	try {
		// Tokenize current version
		List currentVersionList = currentVersion.tokenize(".")
		List requiredVersionList = props.requiredDBBToolkitVersion.tokenize(".")

		// validate the version formats, current version is allowed have more labels.
		assert currentVersionList.size() >= requiredVersionList.size() : "Version syntax does not match."

		// validate each label
		currentVersionList.eachWithIndex{ it, i ->
			if(requiredVersionList.size() >= i +1 )  assert (it as int) >= ((requiredVersionList[i]) as int)
		}

	} catch(AssertionError e) {
		println "Current DBB Toolkit Version $currentVersion does not meet the minimum required version $requiredVersion. EXIT."
		println e.getMessage()
		System.exit(1)
	}
}

/*
 * Returns a string representation of a file's encoding calculated from its tag.
 * 
 */
def retrieveHFSFileEncoding(File file) {
	FileAttribute.Stat stat = FileAttribute.getStat(file.getAbsolutePath())
    FileAttribute.Tag tag = stat.getTag()
	int i = 0
	if (tag != null)
	{
  		char x = tag.getCodeCharacterSetID()
  		i = (int) x
	}

	switch(i) {
		case 0: return null // Return null if file is untagged
		case 1208: return "UTF-8"
		default: return "IBM-${i}"
	}
	
}

/*
 * Logs the resolution rules of the DependencyResolver in a table format
 * 
 */
def printResolutionRules(List<ResolutionRule> rules) {

	println("*** Configured resulution rules:")
	
	// Print header of table
	println("    " + "Library".padRight(10) + "Category".padRight(12) + "SourceDir/File".padRight(50) + "Directory".padRight(36) + "Collection".padRight(24) + "Archive".padRight(20))
	println("    " + " ".padLeft(10,"-") + " ".padLeft(12,"-") + " ".padLeft(50,"-") + " ".padLeft(36,"-") + " ".padLeft(24,"-") + " ".padLeft(20,"-"))

	// iterate over rules configured for the dependencyResolver
	rules.each{ rule ->
		searchPaths = rule.getSearchPath()
		searchPaths.each { DependencyPath searchPath ->
			def libraryName = (rule.getLibrary() != null) ? rule.getLibrary().padRight(10) : "N/A".padRight(10)
			def categoryName = (rule.getCategory() != null) ? rule.getCategory().padRight(12) : "N/A".padRight(12)
			def srcDir = (searchPath.getSourceDir() != null) ? searchPath.getSourceDir().padRight(50) : "N/A".padRight(50)
			def directory = (searchPath.getDirectory() != null) ? searchPath.getDirectory().padRight(36) : "N/A".padRight(36)
			def collection = (searchPath.getCollection() != null) ? searchPath.getCollection().padRight(24) : "N/A".padRight(24)
			def archiveFile = (searchPath.getArchive() != null) ? searchPath.getArchive().padRight(20) : "N/A".padRight(20)
			println("    " + libraryName + categoryName + srcDir + directory + collection + archiveFile)

		}
	}
}

/*
 * Logs information about the physical dependencies in a table format
 */
def printPhysicalDependencies(List<PhysicalDependency> physicalDependencies) {
	// Print header of table
	println("    " + "Library".padRight(10) + "Category".padRight(16) + "Name".padRight(10) + "Status".padRight(14) + "SourceDir/File".padRight(36))
	println("    " + " ".padLeft(10,"-") + " ".padLeft(16,"-") + " ".padLeft(10,"-") + " ".padLeft(14,"-") + " ".padLeft(36,"-"))

	// iterate over list and display info about the physical dependency
	physicalDependencies.each { physicalDependency ->
		def resolvedStatus = (physicalDependency.isResolved()) ? 'RESOLVED' : 'NOT RESOLVED'
		def resolvedFlag = (physicalDependency.isResolved()) ? ' ' : '*'
		def depFile = (physicalDependency.getFile()) ? physicalDependency.getFile() : "N/A"
		println(resolvedFlag.padLeft(4) + physicalDependency.getLibrary().padRight(10) + physicalDependency.getCategory().padRight(16) + physicalDependency.getLname().padRight(10) + resolvedStatus.padRight(14) + depFile.padRight(36))
	}
}

/*
 * Obtain the abbreviated git hash from the PropertyMappings table
 *  returns null if no hash was found
 */
def getShortGitHash(String buildFile) {
	def abbrevGitHash
	PropertyMappings githashChangedFilesMap = new PropertyMappings("githashBuildableFilesMap")
	abbrevGitHash = githashChangedFilesMap.getValue(buildFile)
	if (abbrevGitHash != null ) return abbrevGitHash
	if (props.verbose) println "*! Could not obtain abbreviated githash for buildFile $buildFile"
	return null
}
