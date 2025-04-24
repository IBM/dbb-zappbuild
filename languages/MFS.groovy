@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.metadata.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*

// docs: https://www.ibm.com/docs/en/ims/14.1.0?topic=dfsupaa0-running-utility-in-standard-mode

// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def buildUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BuildUtilities.groovy"))

println("** Building ${argMap.buildList.size()} ${argMap.buildList.size() == 1 ? 'file' : 'files'} mapped to ${this.class.getName()}.groovy script")

// verify required build properties
buildUtils.assertBuildProperties(props.mfs_requiredBuildProperties)

def langQualifier = "mfs"
buildUtils.createLanguageDatasets(langQualifier)

// sort the build list based on build file rank if provided
List<String> sortedList = buildUtils.sortBuildList(argMap.buildList.sort(), 'mfs_fileBuildRank')
int currentBuildFileNumber = 1

// iterate through build list
sortedList.each { buildFile ->
	println "*** (${currentBuildFileNumber++}/${sortedList.size()}) Building file $buildFile"

	// copy build file to input data set
	buildUtils.copySourceFiles(buildFile, props.mfs_srcPDS, null, null, null)

	// create mvs commands
	String member = CopyToPDS.createMemberName(buildFile)
	File logFile = new File( props.userBuild ? "${props.buildOutDir}/${member}.log" : "${props.buildOutDir}/${member}.mfs.log")
	if (logFile.exists())
		logFile.delete()
		
	// execution flags
	phase2Execution = props.getFileProperty('mfs_phase2Execution', buildFile)
	
	// execute mvs commands in a mvs job
	MVSJob job = new MVSJob()
	job.start()

	// generate phase1 command
	MVSExec phase1 = createPhase1Command(buildFile, member, logFile)

	int rc = phase1.execute()
	int maxRC = props.getFileProperty('mfs_phase1MaxRC', buildFile).toInteger()


	if (rc > maxRC) {
		String errorMsg = "*! The phase1 return code ($rc) for $buildFile exceeded the maximum return code allowed ($maxRC)"
		println(errorMsg)
		props.error = "true"
		buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile])
	}
	else {
		// generate phase2 command
		if (phase2Execution && phase2Execution.toBoolean()) {

			MVSExec phase2 = createPhase2Command(buildFile, member, logFile)

			rc = phase2.execute()
			maxRC = props.getFileProperty('mfs_phase2MaxRC', buildFile).toInteger()

			if (rc > maxRC) {
				String errorMsg = "*! The phase 2 return code ($rc) for $buildFile exceeded the maximum return code allowed ($maxRC)"
				println(errorMsg)
				props.error = "true"
				buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile])
			}

		}
	}
	
	// clean up passed DD statements
	job.stop()

}

// end script


//********************************************************************
//* Method definitions
//********************************************************************


/*
 * createPhase1Command - creates a MVSExec command for preprocessing the MFS Map (buildFile)
 * 
 * defines the MFS map as output.
 * 
 */
def createPhase1Command(String buildFile, String member, File logFile) {
	
	String parameters = props.getFileProperty('mfs_phase1Parms', buildFile)

	if (props.verbose) println "MFS Phase 1 Options $buildFile = $parameters"
	
	// define the MVSExec command to compile the mfs map
	MVSExec mfsPhase1 = new MVSExec().file(buildFile).pgm(props.mfs_phase1processor).parm(parameters)

	// add DD statements to the mfsPhase2 command
	String deployType = buildUtils.getDeployType("mfs", buildFile, null)
	
	mfsPhase1.dd(new DDStatement().name("SYSIN").dsn("${props.mfs_srcPDS}($member)").options("shr").report(true).output(true).deployType(deployType))
	
	mfsPhase1.dd(new DDStatement().name("REFIN").dsn("&&REFERAL").options("${props.mfs_tempOptions} dir(10) lrecl(80) blksize(800) recfm(f,b)"))
	mfsPhase1.dd(new DDStatement().name("REFOUT").dsn("&&TEMPPDS").options("${props.mfs_tempOptions} dir(5) lrecl(80) recfm(f,b)"))
	mfsPhase1.dd(new DDStatement().name("REFRD").dsn("&&TEMPPDS").options("cyl space(5,5) unit(vio) old"))
	mfsPhase1.dd(new DDStatement().dsn("&&REFERAL").options("cyl space(5,5) unit(vio) old"))
	
	mfsPhase1.dd(new DDStatement().name("SYSPRINT").options(props.mfs_tempOptions))
	mfsPhase1.dd(new DDStatement().name("SEQBLKS").dsn("&&SEQBLK").options(props.mfs_tempOptions).pass(true))
//	mfsPhase1.dd(new DDStatement().name("SYSLIB").dsn(props.SDFSMAC).options("shr"))
	mfsPhase1.dd(new DDStatement().name("TASKLIB").dsn(props.SDFSRESL).options("shr"))
	mfsPhase1.dd(new DDStatement().name("SYSTEXT").dsn("&&TXTPASS").options(props.mfs_tempOptions))
//	mfsPhase1.dd(new DDStatement().name("DUMMY").dsn("${props.PROCLIB}(REFCPY)").options("shr"))
	
	mfsPhase1.dd(new DDStatement().name("SYSUT3").options(props.mfs_tempOptions))
	mfsPhase1.dd(new DDStatement().name("SYSUT4").options(props.mfs_tempOptions))
	
	mfsPhase1.dd(new DDStatement().name("UTPRINT").options(props.mfs_tempOptions))

	// add a copy command to the compile command to copy the SYSPRINT from the temporary dataset to an HFS log file
	mfsPhase1.copy(new CopyToHFS().ddName("SYSPRINT").file(logFile).hfsEncoding(props.logEncoding))

	return mfsPhase1
}


/*
 * createPhase2Command - creates a MVSExec xommand for running phase 2 of the mfs object module produced by phase 1
 */
def createPhase2Command(String buildFile, String member, File logFile) {
	
	String parameters = props.getFileProperty('mfs_phase2Parms', buildFile)
	
	if (props.verbose) println "MFS Phase 2 Options $buildFile = $parameters"

	// define the MVSExec command for MFS Language Utility - Phase 2
	MVSExec mfsPhase2 = new MVSExec().file(buildFile).pgm(props.mfs_phase2processor).parm(parameters)
	
	mfsPhase2.dd(new DDStatement().name("FORMAT").dsn(props.mfs_tformatPDS).options("shr"))
	// mfsPhase2.dd(new DDStatement().name("DUMMY").dsn("${props.PROCLIB}(FMTCPY)").options("shr"))
	mfsPhase2.dd(new DDStatement().name("TASKLIB").dsn(props.SDFSRESL).options("shr"))
	
	// output DD statements
	mfsPhase2.dd(new DDStatement().name("UTPRINT").options(props.mfs_tempOptions))
	mfsPhase2.dd(new DDStatement().name("SYSPRINT").options(props.mfs_tempOptions))
	
	// add a copy command to the compile command to copy the SYSPRINT from the temporary dataset to an HFS log file
	mfsPhase2.copy(new CopyToHFS().ddName("SYSPRINT").file(logFile).hfsEncoding(props.logEncoding).append(true))
	mfsPhase2.copy(new CopyToHFS().ddName("UTPRINT").file(logFile).hfsEncoding(props.logEncoding).append(true))
	
	return mfsPhase2
}
