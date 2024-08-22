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

/**
 * Method invoked by zAppBuild language scripts
 */
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

/**
 * Method invoked by zAppBuild language scripts
 */
def bindPlan(String buildFile) {

	// Retrieve file overrides
	bind_jobcard = props.getFileProperty('bind_jobCard', buildFile)
	db2_subsys = props.getFileProperty('bind_db2Location', buildFile)
	db2_plan = props.getFileProperty('bind_plan', buildFile)
	db2_plan_pklist = props.getFileProperty('bind_plan_pklist', buildFile)
	db2_qualifier = props.getFileProperty('bind_qualifier', buildFile)
	// Do we need a different plan owner?
	db2_plan_owner = ( !props.bind_packageOwner ) ? System.getProperty("user.name") : props.getFileProperty('bind_packageOwner', buildFile)

	// execute Bind Package Job
	def (bindRc, bindLogFile) = executeBindPlan(buildFile, bind_jobcard, props.buildOutDir, db2_subsys, db2_plan, db2_plan_pklist, db2_plan_owner, db2_qualifier, props.SDSNLOAD, props.verbose && props.verbose.toBoolean())
	return [bindRc, bindLogFile]
}

/**
 * Execute Bind Package Job
 */
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

	jobExec.saveOutput(new File("${workDir}/${dbrm_member}_bind_pkg.log"))

	return [
		rc,
		"${workDir}/${dbrm_member}_bind_pkg.log"
	]

}

/**
 * Execute Bind Plan Job
 */

def executeBindPlan(String file, String jobCard, String workDir, String db2_subsys, String db2_plan, String db2_package_list, String db2_plan_owner, String db2_qualifier, String sdsn_lib,  boolean verbose) {

	println("*** Generate Bind Job for $file")

	String jcl = jobCard.replace("\\n", "\n")
	jcl += """\
\n//*
//**PLNBIND
//PLNBIND EXEC PGM=IKJEFT01,DYNAMNBR=20,COND=(4,LT)
//STEPLIB  DD  DSN=${sdsn_lib},DISP=SHR
//SYSTSPRT DD SYSOUT=*
//SYSPRINT DD SYSOUT=*
//SYSUDUMP DD SYSOUT=*
//SYSIN DD DUMMY
//SYSTSIN DD *
DSN SYSTEM(${db2_subsys})         
BIND PLAN(${db2_plan})            +
	PKLIST(${db2_package_list})   +
	OWNER(${db2_plan_owner})      +
	QUALIFIER(${db2_qualifier})   +
	ACTION(REPLACE)               +
	ISOLATION(CS)                 +
	RELEASE(COMMIT)               +
	ENCODING(EBCDIC)
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

	jobExec.saveOutput(new File("${workDir}/${db2_plan}_bind_plan.log"))

	return [
		rc,
		"${workDir}/${db2_plan}_bind_plan.log"
	]

}


//Parse the command line and bind
def bind(String[] cliArgs)
{

	// Use BindUtilities.groovy as a standalone util

	//		groovyz BindUtilities.groovy --file /u/dbehm/userBuild/MortgageApplication/cobol/epscmort.cbl \
	//			--dbrmPDS DBEHM.UB.DBRM  \
	//			--workDir /u/dbehm/userBuild/work \
	//			--jobCard "//BINDPKG JOB 'DBB-PKGBIND',MSGLEVEL=(1,1),MSGCLASS=R,NOTIFY=&SYSUID" \
	//			--subSys DBC1 \
	//			--collId MORTCL \
	//			--owner DBEHM \
	//			--qual MORT \
	//			--maxRc 4 \
	//			--dsnLibrary DBC0CFG.DB2.V12.SDSNLOAD \
	//			--packageList '*.MORTGAGE.*' \
	//			--plan MORTPL \
	//			--bindPackage \
	//		    --bindPlan

	def cli = new CliBuilder(usage: "BindUtilities.groovy [options]", header: '', stopAtNonOption: false)

	cli.f(longOpt:'file', args:1, required:true, 'The build file name.')
	cli.d(longOpt:'dbrmPDS', args:1, required:true, 'DBRM partition data sets')
	cli.w(longOpt:'workDir', args:1, required:true, 'Absolute path to the working directory')
	cli.j(longOpt:'jobCard', args:1, required:true, 'Jobcard for Bind JCL')
	cli.l(longOpt:'dsnLibrary', args:1, required:true, 'SDSN Load library')
	cli.s(longOpt:'subSys', args:1, required:true, 'The name of the DB2 subsystem')
	cli.p(longOpt:'collId', args:1, required:true, 'Specify the DB2 collection (Package)')
	cli.o(longOpt:'owner', args:1, required:true, 'The owner of the package')
	cli.q(longOpt:'qual', args:1, required:true, 'The value of the implicit qualifier')
	cli.m(longOpt:'maxRc', args:1, 'The maximun return value')

	cli.bpkg(longOpt:'bindPackage', 'Flag execute bind package')
	cli.bpla(longOpt:'bindPlan', 'Flag execute bind plan')
	cli.pl(longOpt:'plan', args:1, 'The Db2 Plan name')


	cli.pkl(longOpt:'packageList', args:1, 'Package List for Bind Plan')
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

	// perform bind package
	if (opts.bpkg) {

		def (rc, logFile) = executeBindPackage(opts.f, opts.j, opts.d, opts.w, opts.s, opts.p, opts.o, opts.q, opts.l, opts.v)
		if ( rc > maxRC ) {
			String errorMsg = "*! The bind package return code ($rc) for $opts.f exceeded the maximum return code allowed ($maxRC)\n** See: $logFile"
			println(errorMsg)
			System.exit(1)
		}
	}

	// perform bind plan
	if (opts.bpla) {

		def (rc, logFile) = executeBindPlan(opts.f, opts.j, opts.w, opts.s, opts.pl, opts.pkl, opts.o, opts.q, opts.l, opts.v)
		if ( rc > maxRC ) {
			String errorMsg = "*! The bind plan return code ($rc) for $opts.f exceeded the maximum return code allowed ($maxRC)\n** See: $logFile"
			println(errorMsg)
			System.exit(1)
		}
	}

}

