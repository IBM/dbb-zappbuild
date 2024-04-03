@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.dependency.*
import com.ibm.dbb.metadata.*
import com.ibm.dbb.build.*
import com.ibm.dbb.build.report.*
import com.ibm.dbb.build.html.*
import com.ibm.dbb.build.report.records.*
import groovy.util.*
import groovy.transform.*
import groovy.time.*
import groovy.xml.*
import groovy.cli.commons.*


// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def gitUtils= loadScript(new File("utilities/GitUtilities.groovy"))
@Field def buildUtils= loadScript(new File("utilities/BuildUtilities.groovy"))
@Field def impactUtils= loadScript(new File("utilities/ImpactUtilities.groovy"))
@Field def reportingUtils= loadScript(new File("utilities/ReportingUtilities.groovy"))
@Field def filePropUtils= loadScript(new File("utilities/FilePropUtilities.groovy"))
@Field def dependencyScannerUtils= loadScript(new File("utilities/DependencyScannerUtilities.groovy"))
@Field def validationUtils= loadScript(new File("utilities/DatasetValidationUtilities.groovy"))
@Field String hashPrefix = ':githash:'
@Field String giturlPrefix = ':giturl:'
@Field String gitchangedfilesPrefix = ':gitchangedfiles:'
@Field MetadataStore metadataStore
@Field startTime = new Date()

// start time message
props.startTime = startTime.format("yyyyMMdd.HHmmss.SSS")
println("\n** Build start at $props.startTime")

// initialize build
try {
	initializeBuildProcess(args)
} catch ( AssertionError e ) {
	String errorMsg = e.getMessage()
	println(errorMsg)
	props.error = "true"
	buildUtils.updateBuildResult(errorMsg:errorMsg)
	finalizeBuildProcess(start:startTime, 0)
}

// create build list
List<String> buildList = new ArrayList() 

buildList = createBuildList()

// build programs in the build list
def processCounter = 0
def scriptPath = ""
if (buildList.size() == 0)
	println("*! No files in build list.  Nothing to do.")
else {
	if (!props.scanOnly && !props.scanLoadmodules) {
		println("** Invoking build scripts according to build order: ${props.buildOrder}")
		String[] buildOrderList = props.buildOrder.split(',')
		String[] testOrderList;
		if (props.runzTests && props.runzTests.toBoolean()) {
			println("** Invoking test scripts according to test order: ${props.testOrder}")
			testOrderList = props.testOrder.split(',')
		}
		buildOrder = buildOrderList + testOrderList
		buildOrder.each { script ->
			scriptPath = script
			// Use the ScriptMappings class to get the files mapped to the build script
			def buildFiles = ScriptMappings.getMappedList(script, buildList)
			try {
				if (buildFiles.size() > 0) {
					if (scriptPath.startsWith('/'))
						runScript(new File("${scriptPath}"), ['buildList':buildFiles])
					else
						runScript(new File("languages/${scriptPath}"), ['buildList':buildFiles])
				}
				processCounter = processCounter + buildFiles.size()
			} catch (BuildException | AssertionError e ) {
				String errorMsg = e.getMessage()
				println(errorMsg)
				props.error = "true"
				buildUtils.updateBuildResult(errorMsg:errorMsg)
				finalizeBuildProcess(start:startTime, count:processCounter)
			}
		}
	} else if(props.scanLoadmodules && props.scanLoadmodules.toBoolean()){
		println ("** Scanning load modules.")
		impactUtils.scanOnlyStaticDependencies(buildList)
	}
}

// finalize build process
if (processCounter == 0)
	processCounter = buildList.size()

finalizeBuildProcess(start:startTime, count:processCounter)

// end script


//********************************************************************
//* Method definitions
//********************************************************************

def initializeBuildProcess(String[] args) {
    println "**************** Initialization of the build process ****************"

	def opts = parseArgs(args) // parse incoming options and arguments
	populateBuildProperties(opts) // build properties initial set
	
	// print and store property dbb toolkit version in use
	def dbbToolkitVersion = VersionInfo.getInstance().getVersion()
	def dbbToolkitBuildDate = VersionInfo.getInstance().getDate()
	props.dbbToolkitVersion = dbbToolkitVersion
	props.dbbToolkitBuildDate = dbbToolkitBuildDate
	
	File versionFile = new File("${props.zAppBuildDir}/version.properties")
	if (versionFile.exists()) {
		props.load(versionFile)
		if (props.zappbuild_version) println "** Running zAppBuild Version ${props.zappbuild_version} "
	}
	if (props.verbose) println "** Running DBB Toolkit Version ${dbbToolkitVersion} ${dbbToolkitBuildDate} "

	// verify required dbb toolkit
	buildUtils.assertDbbBuildToolkitVersion(props.dbbToolkitVersion, props.requiredDBBToolkitVersion)

	// verify required build properties
	buildUtils.assertBuildProperties(props.requiredBuildProperties)

	// evaluate preview flag to set the reportOnly
	if (props.preview) {
		println "** Running in reportOnly mode. Will process build options but not execute any steps."
		props.put("dbb.command.reportOnly","true")
	}
	
	// create metadata store for this script
	if (!props.userBuild) {
		if (props.metadataStoreType == 'file')
			metadataStore = MetadataStoreFactory.createFileMetadataStore(props.metadataStoreFileLocation)
		else if (props.metadataStoreType == 'db2') {
			// Get ID
			String id
			if (opts.id)
				id = opts.id

			//Get password file or encrypted password from command line
			String password
			File passwordFile
			if (opts.pf)  
				passwordFile = new File(opts.pf)
			else if (opts.pw)
				password = opts.pw

			if (props.metadataStoreDb2ConnectionConf) { // Db2 Configuration properties file
			
				// Check if the configuration properties file exists
				File propertiesFile = new File(props.metadataStoreDb2ConnectionConf)
				if (!propertiesFile.exists()) {
					println("Db2 Connection Configuration property file does not exist: ${propertiesFile.getAbsolutePath()}\nPlease verify that the provided path is correct")
					System.exit(1)
				}
				// Load properties file into Properties object
				Properties db2ConnectionProps = new Properties()
				db2ConnectionProps.load(new FileInputStream(propertiesFile))
				
				// Call correct Db2 MetadataStore constructor
				if (passwordFile)
					metadataStore = MetadataStoreFactory.createDb2MetadataStore(id, passwordFile, db2ConnectionProps)
				else
					metadataStore = MetadataStoreFactory.createDb2MetadataStore(id, password as String, db2ConnectionProps)
			}
			else { // Not using Db2 Config Properties file
				
				// Assert URL property is defined
				if (!props.metadataStoreDb2Url) {
					println("For Db2 MetadataStore, please define the metadataStoreDb2Url build property or pass '-url jdbc:db2:<Db2 server location>' on the command line." )
					System.exit(1)
				}
				
				/// Call correct Db2 MetadataStore constructor
				if (passwordFile)
					metadataStore = MetadataStoreFactory.createDb2MetadataStore(props.metadataStoreDb2Url, id, passwordFile)
				else 
					metadataStore = MetadataStoreFactory.createDb2MetadataStore(props.metadataStoreDb2Url, id, password as String)
			}
			
		}
		else {
			println("Invalid MetadataStore Type: ${props.metadataStoreType}.\nOnly valid options for 'metadataStoreType' are 'file' and 'db2'.")
			System.exit(1)
		}

		if (props.verbose) println "** ${props.metadataStoreType.capitalize()} MetadataStore initialized"
	}

	// handle -r,--reset option
	if (props.reset && props.reset.toBoolean())  {
		println("** Reset option selected")

		println("* Deleting collection ${props.applicationCollectionName}")
		metadataStore.deleteCollection(props.applicationCollectionName)

		println("* Deleting collection ${props.applicationOutputsCollectionName}")
		metadataStore.deleteCollection(props.applicationOutputsCollectionName)

		println("* Deleting build result group ${props.applicationBuildGroup}")
		metadataStore.deleteBuildResults(props.applicationBuildGroup)
	
		System.exit(0)
	}

	// create the work directory (build output)
	new File(props.buildOutDir).mkdirs()
	println("** Build output located at ${props.buildOutDir}")

	// initialize build report
	BuildReportFactory.createDefaultReport()

	// initialize build result (requires MetadataStore)
	if (metadataStore) {
		def buildResult = metadataStore.createBuildResult(props.applicationBuildGroup, props.applicationBuildLabel)
		// set build state and status
		buildResult.setState(buildResult.PROCESSING)
		if (props.preview) buildResult.setStatus(4)
		
		if (props.scanOnly) buildResult.setProperty('scanOnly', 'true')
		if (props.fullBuild) buildResult.setProperty('fullBuild', 'true')
		if (props.impactBuild) buildResult.setProperty('impactBuild', 'true')
		if (props.topicBranchBuild) buildResult.setProperty('topicBranchBuild', 'true')
		if (props.preview) buildResult.setProperty('preview', 'true')
		
		if (props.buildFile) buildResult.setProperty('buildFile', XmlUtil.escapeXml(props.buildFile))
				
		println("** Build result created for BuildGroup:${props.applicationBuildGroup} BuildLabel:${props.applicationBuildLabel}")
	}

	// verify/create/clone the collections for this build
	impactUtils.verifyCollections()
	
	// loading the scanner mapping to fill the DependencyScannerRegistry  
	dependencyScannerUtils.populateDependencyScannerRegistry()
	
}

/*
 * parseArgs - parses build.groovy input options and arguments
 */
def parseArgs(String[] args) {
	String usage = 'build.groovy [options] buildfile'
	String header =  '''buildFile (optional):  Path of the file to build. \
If buildFile is a text file (*.txt) then it is assumed to be a build list file.
options:
	'''

	def cli = new CliBuilder(usage:usage,header:header,stopAtNonOption: false)
	// required sandbox options
	cli.w(longOpt:'workspace', args:1, 'Absolute path to workspace (root) directory containing all required source directories')
	cli.a(longOpt:'application', args:1, required:true, 'Application directory name (relative to workspace)')
	cli.o(longOpt:'outDir', args:1, 'Absolute path to the build output root directory')
	cli.h(longOpt:'hlq', args:1, required:true, 'High level qualifier for partition data sets')

	// build options
	cli.p(longOpt:'propFiles', args:1, 'Comma separated list of additional property files to load. Absolute paths or relative to workspace.')
	cli.po(longOpt:'propOverwrites', args:1, 'Comma separated list of key=value pairs for set and overwrite build properties.')
	cli.l(longOpt:'logEncoding', args:1, 'Encoding of output logs. Default is EBCDIC')
	cli.f(longOpt:'fullBuild', 'Flag indicating to build all programs for application')
	cli.i(longOpt:'impactBuild', 'Flag indicating to build only programs impacted by changed files since last successful build.')
	cli.b(longOpt:'baselineRef',args:1,'Comma seperated list of git references to overwrite the baselineHash hash in an impactBuild scenario.')
	cli.m(longOpt:'mergeBuild', 'Flag indicating to build only changes which will be merged back to the mainBuildBranch.')	
	cli.r(longOpt:'reset', 'Deletes the dependency collections and build result group from the MetadataStore')
	cli.v(longOpt:'verbose', 'Flag to turn on script trace')
	cli.pv(longOpt:'preview', 'Supplemental flag indicating to run build in preview mode without processing the execute commands')
	cli.cd(longOpt:'checkDatasets', 'Optional flag to validate the presense of the defined system datasets. ')
	
	// scan options
	cli.s(longOpt:'scanOnly', 'Flag indicating to only scan source files for application without building anything (deprecated use --scanSource)')
	cli.ss(longOpt:'scanSource', 'Flag indicating to only scan source files for application without building anything')
	cli.sl(longOpt:'scanLoad', 'Flag indicating to only scan load modules for application without building anything')
	cli.sa(longOpt:'scanAll', 'Flag indicating to scan both source files and load modules for application without building anything')

	// DBB metadatastore credentials (overrides properties in build.properties)
	cli.url(longOpt:'url', args:1, 'Db2 JDBC URL for the MetadataStore. Example: jdbc:db2:<Db2 server location>')
	cli.id(longOpt:'id', args:1, 'Db2 user id for the MetadataStore')
	cli.pw(longOpt:'pw', args:1,  'Db2 password (encrypted with DBB Password Utility) for the MetadataStore')
	cli.pf(longOpt:'pwFile', args:1, 'Absolute or relative (from workspace) path to file containing Db2 password')

	// IDz/ZOD User build options
	cli.u(longOpt:'userBuild', 'Flag indicating running a user build')
	cli.e(longOpt:'errPrefix', args:1, 'Unique id used for IDz error message datasets')
	cli.srcDir(longOpt:'sourceDir', args:1, 'Absolute path to workspace (root) directory containing all required source directories for user build')
	cli.wrkDir(longOpt:'workDir', args:1, 'Absolute path to the build output root directory for user build')
	cli.t(longOpt:'team', args:1, argName:'hlq', 'Team build hlq for user build syslib concatenations')
	cli.zTest(longOpt:'runzTests', 'Specify if zUnit Tests should be executed')

	// debug option
	cli.d(longOpt:'debug', 'Flag to indicate a build for debugging')
	cli.dz(longOpt:'debugzUnitTestcase', 'Flag to indicate if zUnit Tests should launch a debug session')

	// code coverage options
	cli.cc(longOpt:'ccczUnit', 'Flag to indicate to collect code coverage reports during zUnit step')
	cli.cch(longOpt:'cccHost', args:1, argName:'cccHost', 'Headless Code Coverage Collector host (if not specified IDz will be used for reporting)')
	cli.ccp(longOpt:'cccPort', args:1, argName:'cccPort', 'Headless Code Coverage Collector port (if not specified IDz will be used for reporting)')
	cli.cco(longOpt:'cccOptions', args:1, argName:'cccOptions', 'Headless Code Coverage Collector Options')

	// build framework options
	cli.re(longOpt:'reportExternalImpacts', 'Flag to activate analysis and report of external impacted files within DBB collections')
	
	// IDE user build dependency file options
	cli.df(longOpt:'dependencyFile', args:1, 'Absolute or relative (from workspace) path to user build JSON file containing dependency information.')

	// utility options
	cli.help(longOpt:'help', 'Prints this message')

	def opts = cli.parse(args)
	if (!opts) {
		System.exit(1)
	}

	if(opts.v && args.size() > 0)
		println "** Input args = ${args[0..-1].join(' ')}"

	if( (!opts.cch && opts.ccp) || (opts.cch && !opts.ccp) ) {
		println "** --cccHost and --cccPort options are mutual"
		System.exit(1)
	}

	// if help option used, print usage and exit
	if (opts.help) {
		cli.usage()
		System.exit(0)
	}

	return opts
}

/*
 * populateBuildProperties - loads all build property files, creates properties for command line
 * arguments and sets calculated propertied for the build process
 */
def populateBuildProperties(def opts) {

	def zAppBuildDir =  getScriptDir()
	props.zAppBuildDir = zAppBuildDir

	// set required command line arguments
	if (opts.w) props.workspace = opts.w
	if (opts.o) props.outDir = opts.o
	if (opts.a) props.application = opts.a
	if (opts.h) props.hlq = opts.h

	// need to support IDz user build parameters
	if (opts.srcDir) props.workspace = opts.srcDir
	if (opts.wrkDir) props.outDir = opts.wrkDir

	// assert workspace
	buildUtils.assertBuildProperties('workspace,outDir')

	// Validate that workspace exists 
	if (!(new File (props.workspace).exists())) {
		println "!! The specified workspace folder ${props.workspace} does not exist. Build exits."
		System.exit(1)
	}
	
	// Check read/write permission of specified out/log dir if already existing 
	if (new File (props.outDir).exists() && !(new File(props.outDir).canWrite())) {
		println "!! User does not have WRITE permission to work output directory ${props.outDir}. Build exits."
		System.exit(1)
	}
	
	// load build.properties
	def buildConf = "${zAppBuildDir}/build-conf"
	if (opts.v) println "** Loading property file ${buildConf}/build.properties"
	buildUtils.loadBuildProperties("${buildConf}/build.properties")

	// load additional build property files
	if (opts.v) println "** Loading zAppBuild build properties"
	if (props.buildPropFiles) {
		String[] buildPropFiles = props.buildPropFiles.split(',')
		buildPropFiles.each { propFile ->
			if (!propFile.startsWith('/'))
				propFile = "${buildConf}/${propFile}"
			
			if (opts.v) println "** Loading property file ${propFile}"
			buildUtils.loadBuildProperties(propFile)
		}
	}
	
	// load additional build property files
	if (opts.v) println "** Loading default application properties"
	if (props.applicationDefaultPropFiles) {
		String[] applicationDefaultPropFiles = props.applicationDefaultPropFiles.split(',')
		applicationDefaultPropFiles.each { propFile ->
			if (!propFile.startsWith('/'))
				propFile = "${buildConf}/${propFile}"

			if (opts.v) println "** Loading property file ${propFile}"
			buildUtils.loadBuildProperties(propFile)
		}
	}


	// load application.properties
	String appConf = props.applicationConfDir
	if (appConf.endsWith('/'))
		appConf = appConf.substring(0, appConf.length() - 1)
		
	if (opts.v) println "** Loading application specific properties"
	if (opts.v) println "** applicationConfDir = ${appConf}"
	
	applicationProperties = "${appConf}/application.properties"
	applicationPropertiesFile = new File(applicationProperties)
	
	if (applicationPropertiesFile.exists()) {
		if (opts.v) println "** Loading property file ${applicationProperties}"
		buildUtils.loadBuildProperties(applicationProperties)

		// load additional application property files
		if (props.applicationPropFiles) {
			String[] applicationPropFiles = props.applicationPropFiles.split(',')
			applicationPropFiles.each { propFile ->
				if (!propFile.startsWith('/'))
					propFile = "${appConf}/${propFile}"
				
				if (opts.v) println "** Loading property file ${propFile}"
				buildUtils.loadBuildProperties(propFile)
			}
		}
	} else {
		if (opts.v) println "*! Properties file ${applicationProperties} was not found. Build continues."
	}

	// load property files from argument list
	if (opts.p) props.propFiles = opts.p
	if (props.propFiles) {
		String[] propFiles = props.propFiles.split(',')
		propFiles.each { propFile ->
			if (!propFile.startsWith('/'))
				propFile = "${props.workspace}/${propFile}"

			if (opts.v) println "** Loading property file ${propFile}"	
			buildUtils.loadBuildProperties(propFile)
		}
	}
	
	// populate property overwrites from argument list
	if (opts.po) props.propOverwrites = opts.po
	if (props.propOverwrites) {
		String[] propOverwrites = props.propOverwrites.split(',')
		propOverwrites.each { buildPropertyOverwrite ->
			(key, value) = buildPropertyOverwrite.tokenize('=')
			if (key && value) {
				if (opts.v) println "** Overwriting build property ${key} from cli argument --propOverwrite with value ${value}"
				props.put(key, value)
			}
			else {
				println "*! Overwriting build property from cli argument --propOverwrite failed due a null value ( key: $key , value :$value )"
			}
		}
	}
	

	// set flag indicating to run unit tests
	if (opts.zTest) props.runzTests = 'true'

	// set optional command line arguments
	if (opts.l) props.logEncoding = opts.l
	if (opts.f) props.fullBuild = 'true'
	if (opts.i) props.impactBuild = 'true'
	if (opts.r) props.reset = 'true'
	if (opts.v) props.verbose = 'true'
	if (opts.b) props.baselineRef = opts.b
	if (opts.m) props.mergeBuild = 'true'
	if (opts.pv) props.preview = 'true'
	if (opts.cd) props.checkDatasets = 'true'
		
	// scan options
	if (opts.s) props.scanOnly = 'true'
	if (opts.ss) props.scanOnly = 'true'
	if (opts.sl) props.scanLoadmodules = 'true'
	if (opts.sa) {
		props.scanOnly = 'true'
		props.scanLoadmodules = 'true'
	}

	// set debug flag
	if (opts.d) props.debug = 'true'

	if (opts.dz) props.debugzUnitTestcase = 'true'
	
	// set code coverage flag
	if (opts.cc) {
		props.codeZunitCoverage = 'true'
		if (opts.cch && opts.ccp) {
			props.codeCoverageHeadlessHost = opts.cch
			props.codeCoverageHeadlessPort = opts.ccp
		}
		if (opts.cco) {
			props.codeCoverageOptions = opts.cco
		}
	}
	
	// set buildframe options
	if (opts.re) props.reportExternalImpacts = 'true'

	// set Db2 URL configuration properties
	if (opts.url) props.metadataStoreDb2Url = opts.url
	// db2 id, password, and passwordFile are no longer properties

	// set IDz/ZOD user build options
	if (opts.e) props.errPrefix = opts.e
	if (opts.u) props.userBuild = 'true'
	if (opts.t) props.team = opts.t
	// support IDE passing dependency file parameter
	if (opts.df) props.userBuildDependencyFile = opts.df

	// set build file from first non-option argument
	if (opts.arguments()) props.buildFile = opts.arguments()[0].trim()

	// set calculated properties
	if (!props.userBuild) {
		def gitDir = buildUtils.getAbsolutePath(props.application)
		if ( gitUtils.isGitDetachedHEAD(gitDir) )
			props.applicationCurrentBranch = gitUtils.getCurrentGitDetachedBranch(gitDir)
		else
			props.applicationCurrentBranch = gitUtils.getCurrentGitBranch(gitDir)
	}

	props.topicBranchBuild = (props.applicationCurrentBranch.equals(props.mainBuildBranch)) ? null : 'true'
	props.applicationBuildGroup = ((props.applicationCurrentBranch) ? "${props.application}-${props.applicationCurrentBranch}" : "${props.application}") as String
	props.applicationBuildLabel = ("build.${props.startTime}") as String
	props.applicationCollectionName = ((props.applicationCurrentBranch) ? "${props.application}-${props.applicationCurrentBranch}" : "${props.application}") as String
	props.applicationOutputsCollectionName = "${props.applicationCollectionName}-outputs" as String

	if (props.userBuild) {	// do not create a subfolder for user builds
		props.buildOutDir = "${props.outDir}" as String }
	else {// validate createBuildOutputSubfolder build property
		props.buildOutDir = ((props.createBuildOutputSubfolder && props.createBuildOutputSubfolder.toBoolean()) ? "${props.outDir}/${props.applicationBuildLabel}" : "${props.outDir}") as String
	}
	
	// Validate User Build Dependency file is used only with user build
	if (props.userBuildDependencyFile) assert (props.userBuild) : "*! User Build Dependency File requires User Build option."

	// Validate Build Properties  
	if(props.reportExternalImpactsAnalysisDepths) assert (props.reportExternalImpactsAnalysisDepths == 'simple' || props.reportExternalImpactsAnalysisDepths == 'deep' ) : "*! Build Property props.reportExternalImpactsAnalysisDepths has an invalid value"
	if(props.baselineRef) assert (props.impactBuild) : "*! Build Property props.baselineRef is exclusive to an impactBuild scenario"
	
	// Validate system datasets
	if (props.checkDatasets && props.systemDatasets) validationUtils.validateSystemDatasets(props.systemDatasets, props.verbose)
	
	// Print all build properties + some envionment variables 
	if (props.verbose) {
		println("java.version="+System.getProperty("java.runtime.version"))
		println("java.home="+System.getProperty("java.home"))
		println("user.dir="+System.getProperty("user.dir"))
		println ("** Build properties at start up:\n${props.list()}")
	}

}


/*
 * createBuildList - creates the list of programs to build. Build list calculated four ways:
 *   - full build : Contains all programs in application and external directories. Use script option --fullBuild
 *   - impact build : Contains impacted programs from calculated changed files. Use script option --impactBuild
 *   - build file : Contains one program. Provide a build file argument.
 *   - build text file: Contains a list of programs from a text file. Provide a *.txt build file argument.
 */
def createBuildList() {
    println "************* Creation and processing of the build list *************"

	// using a set to create build list to eliminate duplicate files
	Set<String> buildSet = new HashSet<String>()
	Set<String> changedFiles = new HashSet<String>()
	Set<String> deletedFiles = new HashSet<String>()
	Set<String> renamedFiles = new HashSet<String>() // not yet used for any post-processing
	Set<String> changedBuildProperties = new HashSet<String>() // not yet used for any post-processing
	String action = (props.scanOnly) || (props.scanLoadmodules) ? 'Scanning' : 'Building'

	// check if preview sub-option
	if (props.preview) { println "** --preview cli option provided. Processing all phases of the supplied build option, but will not execute the commands." }
			
	// check if full build
	if (props.fullBuild) {
		println "** --fullBuild option selected. $action all programs for application ${props.application}"

		buildSet = buildUtils.createFullBuildList()
	}
	// check if impact build
	else if (props.impactBuild) {
		println "** --impactBuild option selected. $action impacted programs for application ${props.application} "
		if (metadataStore) {
			(buildSet, changedFiles, deletedFiles, renamedFiles, changedBuildProperties) = impactUtils.createImpactBuildList()		}
		else {
			println "*! Impact build requires a Filesystem or Db2 MetadataStore"
		}
	}
	else if (props.mergeBuild){
		println "** --mergeBuild option selected. $action changed programs for application ${props.application} flowing back to ${props.mainBuildBranch}"
		if (metadataStore) {
			assert (props.topicBranchBuild) : "*! Build type --mergeBuild can only be run on for topic branch builds."
				(buildSet, changedFiles, deletedFiles, renamedFiles, changedBuildProperties) = impactUtils.createMergeBuildList()		}
		else {
			println "*! Merge build requires a Filesystem or Db2 MetadataStore"
		}
	}
		
	// if build file present add additional files to build list (mandatory build list)
	if (props.buildFile) {

		// handle list file
		if (props.buildFile.endsWith(props.buildListFileExt)) {
			if (!props.buildFile.trim().startsWith('/'))
				props.buildFile = "${props.workspace}/${props.buildFile}" as String
			println "** Adding files listed in ${props.buildFile} to $action build list"

			File jBuildFile = new File(props.buildFile)
			List<String> files = jBuildFile.readLines()
			files.each { file ->
				String relFile = buildUtils.relativizePath(file)
				if (relFile)
					buildSet.add(relFile)
			}
		}
		// else it's a single file to build
		else {
			println "** Adding ${props.buildFile} to $action build list"
			String relFile = buildUtils.relativizePath(props.buildFile)
			if (relFile)
				buildSet.add(relFile)
		}
	}

	// now that we are done adding to the build list convert the set to a list
	List<String> buildList = new ArrayList<String>()
	buildList.addAll(buildSet)

	// convert set of deleted files to a list 
	List<String> deleteList = new ArrayList<String>()
	deleteList.addAll(deletedFiles)
	
	// write out build list to file (for documentation, not actually used by build scripts)
	String buildListFileLoc = "${props.buildOutDir}/buildList.${props.buildListFileExt}"
	println "** Writing build list file to $buildListFileLoc"
	File buildListFile = new File(buildListFileLoc)
	String enc = props.logEncoding ?: 'IBM-1047'
	buildListFile.withWriter(enc) { writer ->
		buildList.each { file ->
			if (props.verbose) println file
			writer.write("$file\n")
		}
	}

	// write out list of deleted files (for documentation, not actually used by build scripts)
	if (deletedFiles.size() > 0){
		String deletedFilesListLoc = "${props.buildOutDir}/deletedFilesList.${props.buildListFileExt}"
		println "** Writing list of deleted files to $deletedFilesListLoc"
		File deletedFilesListFile = new File(deletedFilesListLoc)
		deletedFilesListFile.withWriter(enc) { writer ->
			deletedFiles.each { file ->
				if (props.verbose) println file
				writer.write("$file\n")
			}
		}
	}

	// scan and update source collection with build list files for non-impact builds
	// since impact build list creation already scanned the incoming changed files
	// we do not need to scan them again
	if (!props.impactBuild && !props.userBuild && !props.mergeBuild) {
		println "** Scanning source code."
		impactUtils.updateCollection(buildList, null, null)
	}
	
	// Loading file/member level properties from member specific properties files
	if (props.filePropertyValueKeySet().getAt("loadFileLevelProperties") 
	    || props.filePropertyValueKeySet().getAt("loadLanguageConfigurationProperties") 
	    || (props.loadFileLevelProperties && props.loadFileLevelProperties.toBoolean())
		|| (props.loadLanguageConfigurationProperties && props.loadLanguageConfigurationProperties.toBoolean())) {
		filePropUtils.loadFileLevelPropertiesFromConfigFiles(buildList)
	}
	
	// Perform analysis and build report of external impacts
	// Prereq: Metadatastore Connection
	if (metadataStore && props.reportExternalImpacts && props.reportExternalImpacts.toBoolean()){
		if (buildSet && changedFiles) {
			println "** Perform analysis and reporting of external impacted files for the build list including changed files."
			reportingUtils.reportExternalImpacts(buildSet.plus(changedFiles))
		}
		else if(buildSet) {
			println "** Perform analysis and reporting of external impacted files for the build list."
			reportingUtils.reportExternalImpacts(buildSet)
		}
	}
	
	// Document and validate concurrent changes
	// Prereq: Workspace containing git repos. Skipped for --userBuild build type
	if (!props.userBuild && props.reportConcurrentChanges && props.reportConcurrentChanges.toBoolean()){
		println "** Calculate and document concurrent changes."
		reportingUtils.calculateConcurrentChanges(buildSet)
	}
	
	// document deletions in build report
	if (deleteList.size() != 0 && props.documentDeleteRecords && props.documentDeleteRecords.toBoolean()) {
		println("** Document deleted files in Build Report.")
		if (buildUtils.assertDbbBuildToolkitVersion(props.dbbToolkitVersion, "1.1.3")) {
			buildReportUtils = loadScript(new File("utilities/BuildReportUtilities.groovy"))
			buildReportUtils.processDeletedFilesList(deleteList)
		}
	}

	return buildList
}

def finalizeBuildProcess(Map args) {
    println "***************** Finalization of the build process *****************"

	def buildReport = BuildReportFactory.getBuildReport()
	def buildResult = null

	// update repository artifacts
	if (metadataStore) {
		buildResult = metadataStore.getBuildResult(props.applicationBuildGroup, props.applicationBuildLabel)

		// add git hashes for each build directory
		List<String> srcDirs = []
		if (props.applicationSrcDirs)
			srcDirs.addAll(props.applicationSrcDirs.trim().split(','))

		srcDirs.each { dir ->
			dir = buildUtils.getAbsolutePath(dir)
			if (props.verbose) println "*** Obtaining hash for directory $dir"
			if (gitUtils.isGitDir(dir)) {
				// store current hash
				String key = "$hashPrefix${buildUtils.relativizePath(dir)}"
				String currenthash = gitUtils.getCurrentGitHash(dir, false)
				if (props.verbose) println "** Setting property $key : $currenthash"
				buildResult.setProperty(key, currenthash)
				// store gitUrl
				String giturlkey = "$giturlPrefix${buildUtils.relativizePath(dir)}"
				String url = gitUtils.getCurrentGitUrl(dir)
				if (props.verbose) println "** Setting property $giturlkey : $url"
				buildResult.setProperty(giturlkey, url)
				// document changed files - Git compare link
				if (props.impactBuild && props.gitRepositoryURL && props.gitRepositoryCompareService){
					String gitchangedfilesKey = "$gitchangedfilesPrefix${buildUtils.relativizePath(dir)}"
					def lastBuildResult= buildUtils.retrieveLastBuildResult()
					if (lastBuildResult){
						String baselineHash = lastBuildResult.getProperty(key)
						String gitchangedfilesLink = props.gitRepositoryURL << "/" << props.gitRepositoryCompareService <<"/" << baselineHash << ".." << currenthash
						String gitchangedfilesLinkUrl = new URI(gitchangedfilesLink).normalize().toString()
						if (props.verbose) println "** Setting property $gitchangedfilesKey : $gitchangedfilesLinkUrl"
						buildResult.setProperty(gitchangedfilesKey, gitchangedfilesLink)
					}
				}
			}
			else {
				if (props.verbose) println "**! Directory $dir is not a Git repository"
			}
		}

		// add files processed and set state
		buildResult.setProperty("filesProcessed", String.valueOf(args.count))
		buildResult.setState(buildResult.COMPLETE)

		// add zAppBuild and DBB toolkit version info
		if (props.zappbuild_version) buildResult.setProperty("zAppBuildVersion", props.zappbuild_version)
		buildResult.setProperty("DBBToolkitVersion" , "${props.dbbToolkitVersion} ${props.dbbToolkitBuildDate}")
		
		// store build result properties in BuildReport.json
		PropertiesRecord buildReportRecord = new PropertiesRecord("DBB.BuildResultProperties")
		def buildResultProps = buildResult.getPropertyNames()
		buildResultProps.each { buildResultPropName ->
			buildReportRecord.addProperty(buildResultPropName, buildResult.getProperty(buildResultPropName))
		}
		// def buildResultProps = buildResult.getProperties()
		// buildResultProps.each { name, value ->
		// 	buildReportRecord.addProperty(name, value)
		// }
		buildReport.addRecord(buildReportRecord)
	}
		
	// create build report data file
	def jsonOutputFile = new File("${props.buildOutDir}/BuildReport.json")
	def buildReportEncoding = "UTF-8"

	// save json file
	println "** Writing build report data to ${jsonOutputFile}"
	buildReport.save(jsonOutputFile, buildReportEncoding)

	// create build report html file
	def htmlOutputFile = new File("${props.buildOutDir}/BuildReport.html")
	println "** Writing build report to ${htmlOutputFile}"
	buildReport.generateHTML(htmlOutputFile)


	// attach build report & result
	if (metadataStore) {
		buildReport.save(jsonOutputFile, buildReportEncoding)
		// Save build report & build report data
		buildResult.setBuildReport(new FileInputStream(htmlOutputFile))
		buildResult.setBuildReportData(new FileInputStream(jsonOutputFile))
		println "** Updating build result BuildGroup:${props.applicationBuildGroup} BuildLabel:${props.applicationBuildLabel}"
	}

	// print end build message
	def endTime = new Date()
	def duration = TimeCategory.minus(endTime, args.start)
	//buildResult.setProperty("buildDuration", duration.toString())
	def state = (props.error) ? "ERROR" : "CLEAN"
	println("** Build ended at $endTime")
	println("** Build State : $state")
	if (props.preview) println("** Build ran in preview mode.")
	println("** Total files processed : ${args.count}")
	if (props.errorSummary) {
		println("** Summary of error messages")
		println("${props.errorSummary}")
	}
	println("** Total build time  : $duration\n")


	
	// if error occurred signal process error
	if (props.error)
		System.exit(1)
}



