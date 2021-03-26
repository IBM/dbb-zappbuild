@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import groovy.transform.*
import com.ibm.dbb.build.*

println "** Executing zAppBuild test framework test/test.groovy"

// Parse test script arguments and load build properties
BuildProperties props = loadBuildProperties(args)

// create a test branch to run under
createTestBranch(props)

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
	deleteTestBranch(props)
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
	   u(longOpt: 'url', 'DBB Web Application server URL', args: 1, required: true)
	   i(longOpt: 'id', 'DBB Web Application user id', args: 1, required: true)
	   p(longOpt: 'pw', 'DBB Web Application user password', args: 1)
	   P(longOpt: 'pwFile', 'DBB Web Application user password file', args: 1)
	   v(longOpt: 'verbose', 'Flag indicating to print trace statements')
	}
	
	def options = cli.parse(args)
	
	// Show usage text when -h or --help option is used.
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
	
	// Load application test.properties file
	props.load(new File("${getScriptDir()}/applications/${props.app}/test.properties"))
	
	// add some additional properties
	props.testBranch = 'zAppBuildTesting'
	props.zAppBuildDir = new File(getScriptDir()).getParent()
	
	// zAppBuild repo locations
	props.appLocation = "${props.zAppBuildDir}/samples/${props.app}" as String
	props.workspace = "${props.zAppBuildDir}/samples" as String

	// print properties
	if (props.verbose) {
		println "** Properties args and applications/${props.app}/test.properties"
		println props.list()
	}
		
	return props
}

/*
 * Create and checkout a local test branch for testing
 */
def createTestBranch(BuildProperties props) {
	println "** Creating and checking out branch ${props.testBranch}"
	def createTestBranch = """
    cd ${props.zAppBuildDir}
    git checkout ${props.branch}
    git checkout -b ${props.testBranch} ${props.branch}
    git status
"""
	def job = ['bash', '-c', createTestBranch].execute()
	job.waitFor()
	def createBranch = job.in.text
	println "$createBranch"
}

/*
 * Deletes test branch
 */
def deleteTestBranch(BuildProperties props) {
	println "\n** Deleting test branch ${props.testBranch}"
	def deleteTestBranch = """
    cd ${props.zAppBuildDir}
    rm -r out
    git reset --hard ${props.testBranch}
    git checkout ${props.branch}
    git branch -D ${props.testBranch}
    git status
"""
	def job = ['bash', '-c', deleteTestBranch].execute()
	job.waitFor()
	def deleteBranch = job.in.text
	println "$deleteBranch"
}
