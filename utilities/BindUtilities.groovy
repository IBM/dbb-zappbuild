@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.build.*
import com.ibm.dbb.metadata.*
import com.ibm.dbb.dependency.*
import groovy.transform.*
import groovy.cli.commons.*
import com.ibm.dbb.build.JobExec

/**
 * This script builds a DB2 application package for SQL programs in the application.
 */

@Field BuildProperties props = BuildProperties.getInstance()

bind(args)

def bindPackage(String buildFile, String dbrmPDS) {

	// Retrieve file overrides	
	bind_jobcard = props.getFileProperty('bind_jobCard', buildFile)
	db2_subsys = props.getFileProperty('bind_db2Location', buildFile)
	db2_collection = props.getFileProperty('bind_collectionID', buildFile)
	db2_qualifier = props.getFileProperty('bind_qualifier', buildFile)
	db2_package_owner = ( !props.bind_packageOwner ) ? System.getProperty("user.name") : props.getFileProperty('bind_packageOwner', buildFile)
	
	// execute Bind Package Job
	def (bindRc, bindLogFile) = executeBindPackage(buildFile, bind_jobcard, dbrmPDS, props.buildOutDir, db2_subsys, db2_collection, db2_package_owner, db2_qualifier, props.SDSNLOAD, props.verbose && props.verbose.toBoolean())
	return [bindRc, bindLogFile]
		
}

def executeBindPackage(String file, String jobCard, String dbrmPDS, String workDir, String db2_subsys, String db2_collection, String db2_package_owner, String db2_qualifier, String sdsn_lib,  boolean verbose) {

	def dbrm_member = CopyToPDS.createMemberName(file)
	
	println("*** Generate Bind Job for $file")
	
	String jcl = jobCard.replace("\\n", "\n")
	jcl += """\
\n//*
//**PKGBIND
//PKGBIND EXEC PGM=IKJEFT01,DYNAMNBR=20,COND=(4,LT)
//STEPLIB  DD  DSN=${sdsn_lib},DISP=SHR
//SYSTSPRT DD SYSOUT=*
//SYSPRINT DD SYSOUT=*
//SYSUDUMP DD SYSOUT=*
//SYSIN DD DUMMY
//SYSTSIN DD *
DSN SYSTEM(${db2_subsys})                                       
BIND PACKAGE(${db2_collection})    +                                
     MEMBER(${dbrm_member})        +                                
     LIBRARY('${dbrmPDS}')         +                                
     OWNER(${db2_package_owner})   +                                
     QUALIFIER(${db2_qualifier})   +                                
     ACTION(REPLACE)               +                                
     ISOLATION(CS)                                        
END  
//
"""
	// bind the build file
	if ( verbose ) {
	println("*** Executing Package Bind for program $file using \n$jcl")}
	
	jobExec = new JobExec().text(jcl).buildFile(file) 
	def rc = jobExec.execute()
	
	if ( verbose ) {
	println("*** Bind Job(${jobExec.getSubmittedJobId()}) for $file completed with RC=$rc")}
	
	jobExec.saveOutput(new File("${workDir}/${dbrm_member}_bind.log"))
	
	return [rc,"${workDir}/${dbrm_member}_bind.log"]

}

//Parse the command line and bind
def bind(String[] cliArgs)
{
	def cli = new CliBuilder(usage: "BindUtilities.groovy [options]", header: '', stopAtNonOption: false)
	cli.f(longOpt:'file', args:1, required:true, 'The build file name.')
	cli.d(longOpt:'dbrmHLQ', args:1, required:true, 'DBRM partition data sets')	
	cli.w(longOpt:'workDir', args:1, required:true, 'Absolute path to the working directory')
	cli.c(longOpt:'confDir', args:1, required:true, 'Absolute path to runIspf.sh folder')
	
	cli.s(longOpt:'subSys', args:1, required:true, 'The name of the DB2 subsystem')
	cli.p(longOpt:'collId', args:1, required:true, 'Specify the DB2 collection (Package)')
	cli.o(longOpt:'owner', args:1, required:true, 'The owner of the package')
	cli.q(longOpt:'qual', args:1, required:true, 'The value of the implicit qualifier')
	cli.m(longOpt:'maxRc', args:1, 'The maximun return value')
	
	cli.v(longOpt:'verbose', 'Flag to turn on script trace')
	
	def opts = cli.parse(cliArgs)
	
	// if opt parse fail exit.
	if (! opts) {
		System.exit(1)
	}
	
	if (opts.help)
	{
		cli.usage()
		System.exit(0)
	}
	
	def maxRC = opts.m ? opts.m.toInteger() : 0
	def (rc, logFile) = executeBindPackage(opts.f, opts.d, opts.w, opts.c, opts.s, opts.p, opts.o, opts.q, opts.v)
	if ( rc > maxRC ) {
		String errorMsg = "*! The bind return code ($rc) for $opts.f exceeded the maximum return code allowed ($maxRC)\n** See: $logFile"
		println(errorMsg)
		System.exit(1)
	}
}

