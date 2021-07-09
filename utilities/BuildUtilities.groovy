@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.repository.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*
import groovy.json.JsonSlurper
import com.ibm.dbb.build.DBBConstants.CopyMode

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

def createFullBuildList() {
	Set<String> buildSet = new HashSet<String>()
	// create the list of build directories
	List<String> srcDirs = []
	if (props.applicationSrcDirs)
		srcDirs.addAll(props.applicationSrcDirs.split(','))

	srcDirs.each{ dir ->
		dir = getAbsolutePath(dir)
		buildSet.addAll(getFileSet(dir, true, '**/*.*', props.excludeFileList))
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
 */

def copySourceFiles(String buildFile, String srcPDS, String dependencyPDS, DependencyResolver dependencyResolver) {
	// only copy the build file once
	if (!copiedFileCache.contains(buildFile)) {
		copiedFileCache.add(buildFile)
		new CopyToPDS().file(new File(getAbsolutePath(buildFile)))
				.dataset(srcPDS)
				.member(CopyToPDS.createMemberName(buildFile))
				.execute()
	}

	// resolve the logical dependencies to physical files to copy to data sets
	if (dependencyPDS && dependencyResolver) {
		List<PhysicalDependency> physicalDependencies = dependencyResolver.resolve()
		if (props.verbose) {
			println "*** Resolution rules for $buildFile:"
			dependencyResolver.getResolutionRules().each{ rule -> println rule }
		}
		if (props.verbose) println "*** Physical dependencies for $buildFile:"

		physicalDependencies.each { physicalDependency ->
			if (props.verbose) println physicalDependency
			if (physicalDependency.isResolved()) {
				String physicalDependencyLoc = "${physicalDependency.getSourceDir()}/${physicalDependency.getFile()}"

				// only copy the dependency file once per script invocation
				if (!copiedFileCache.contains(physicalDependencyLoc)) {
					copiedFileCache.add(physicalDependencyLoc)

					//retrieve zUnitFileExtension plbck
					zunitFileExtension = (props.zunit_playbackFileExtension) ? props.zunit_playbackFileExtension : null

					if( zunitFileExtension && !zunitFileExtension.isEmpty() && ((physicalDependency.getFile().substring(physicalDependency.getFile().indexOf("."))).contains(zunitFileExtension))){
						new CopyToPDS().file(new File(physicalDependencyLoc))
								.copyMode(CopyMode.BINARY)
								.dataset(dependencyPDS)
								.member(CopyToPDS.createMemberName(physicalDependency.getFile()))
								.execute()
					} else
					{
						new CopyToPDS().file(new File(physicalDependencyLoc))
								.dataset(dependencyPDS)
								.member(CopyToPDS.createMemberName(physicalDependency.getFile()))
								.execute()
					}
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

	def scanner = getScanner(buildFile)

	// create a dependency resolver for the build file
	DependencyResolver resolver = new DependencyResolver().file(buildFile)
			.sourceDir(props.workspace)
			.scanner(scanner)
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
 * returns the deployType for a logicalFile depending on the isCICS, isDLI setting
 */
def getDeployType(String langQualifier, String buildFile, LogicalFile logicalFile){
	// getDefault
	String deployType = props.getFileProperty("${langQualifier}_deployType", buildFile)
	if(deployType == null )
		deployType = 'LOAD'

	if (props."${langQualifier}_deployType" != deployType){ // check if a file level overwrite was used
		if(isCICS(logicalFile)){ // if CICS
			String cicsDeployType = props.getFileProperty("${langQualifier}_deployTypeCICS", buildFile)
			println "xxxx + $cicsDeployType"
			if (cicsDeployType != null) deployType = cicsDeployType
		} else if (isDLI(logicalFile)){
			String dliDeployType = props.getFileProperty("${langQualifier}_deployTypeDLI", buildFile)
			if (dliDeployType != null) deployType = dliDeployType
		}
	}
	
	return deployType

}