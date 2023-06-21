@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import groovy.transform.*
import com.ibm.dbb.build.*
import groovy.cli.commons.*

println "** Executing zAppBuild test framework test/test.groovy"

// Parse test script arguments and load build properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def testUtils = loadScript(new File("utils/testUtilities.groovy"))

// load test properties
props = loadBuildProperties(args)

// create a test branch to run under
testUtils.createTestBranch()

// flag to control test process
props.testsSucceeded = 'true'

// run the test scripts
try {
	if (props.test_testOrder) {
		println("** Invoking test scripts according to test list order: ${props.test_testOrder}")
		
		String[] testOrder = props.test_testOrder.split(',')
		
		testOrder.each { script ->
		   // run the test script	
		   runScript(new File("testScripts/$script"), [:])
	    }
	}
	else {
		println("*! No test scripts to run in application ${props.app}.  Nothing to do.")
	}
}
finally {
	// delete test branch
	testUtils.deleteTestBranch()
	
	// if error occurred signal process error
	if (props.testsSucceeded.toBoolean() == false) {
		println "\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
		println("*! Not all test scripts completed successfully. Please check console outputs. Send exit signal.")
		println "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
		System.exit(1)
	} else {
		println "\n================================================================================================"
		println("* ZAPPBUILD TESTFRAMEWORK COMPLETED.\n   All tests (${props.test_testOrder}) completed successfully.")
		println "================================================================================================"
		
	}
	
}
// end script


//************************************************************
// Method definitions
//************************************************************

/*
 * Parse command line arguments and store in Build Properties
 */
def loadBuildProperties(String [] args) {
	// use CliBuilder to parse test.groovy input options
	def cli = new CliBuilder(
	   usage: '$DBB_HOME/bin/groovyz test.groovy <options>',
	   header: '\nAvailable options (use -h for help):\n',
	   footer: '\nInformation provided via above options is used to execute build against ZAppBuild.\n')
	
	cli.with
	{
	   h(longOpt: 'help', 'Show usage information')
	   
	   // test framework options
	   b(longOpt: 'branch', 'zAppBuild branch to test', args: 1, required: true)

	   // zAppBuild options
	   a(longOpt: 'app', 'Application that is being tested (example: MortgageApplication)', args: 1, required: true)
	   q(longOpt: 'hlq', 'HLQ for dataset reation / deletion (example: USER.BUILD)', args: 1, required: true)
	   u(longOpt: 'url', 'Db2 JDBC URL for the MetadataStore. \n Example: jdbc:db2:<Db2 server location>', args: 1)
	   i(longOpt: 'id', 'Db2 user id for the MetadataStore', args: 1)
	   p(longOpt: 'pw', 'Db2 password (encrypted with DBB Password Utility) for the MetadataStore', args: 1)
	   P(longOpt: 'pwFile', 'Absolute or relative (from workspace) path to file containing Db2 password', args: 1)
	   v(longOpt: 'verbose', 'Flag indicating to print trace statements')
	   f(longOpt: 'propFiles', 'Commas spearated list of additional property files to load. Absolute paths or relative to workspace', args:1)
       o(longOpt: 'outDir', 'Absolute path to the build output root directory', args:1)
	}
	
	def options = cli.parse(args)
	
	// show usage text when -h or --help option is used.
	if (options.h) {
		cli.usage()
		System.exit(0)
	}
	
	// load build properties
	BuildProperties props = BuildProperties.getInstance()
	
	// store the command line arguments in the Build Properties for all scripts to access
	if (options.b) props.branch = options.b
	if (options.a) props.app = options.a
	if (options.q) props.hlq = options.q
	if (options.u) props.url = options.u
	if (options.i) props.id = options.i
	if (options.p) props.pw = options.p
	if (options.P) props.pwFile = options.P
	if (options.v) props.verbose = 'true'
	if (options.f) props.propFiles = options.f
	if (options.o) props.outDir = options.o
	
	// add some additional properties
	props.testBranch = 'zAppBuildTesting'
	props.zAppBuildDir = new File(getScriptDir()).getParent()
	
	// load property files from argument list
	if (options.f) props.propFiles = options.f
	if (props.propFiles) {
		String[] propFiles = props.propFiles.split(',')
		propFiles.each { propFile ->
			if (!propFile.startsWith('/'))
				propFile = "${props.workspace}/${propFile}"

			if (options.v) println "** Loading property file ${propFile}"
			props.load(new File(propFile))
		}
	}
	
	// zAppBuild repo locations
	props.appLocation = "${props.zAppBuildDir}/samples/${props.app}" as String
	props.workspace = "${props.zAppBuildDir}/samples" as String

	// load application test.properties file
	props.load(new File("${getScriptDir()}/applications/${props.app}/test.properties"))

	
	// print properties
	if (props.verbose) {
		println "** Properties args and applications/${props.app}/test.properties"
		println props.list()
	}
		
	return props
}
