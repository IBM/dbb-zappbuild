@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.repository.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*
import com.ibm.jzos.ZFile


// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def buildUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BuildUtilities.groovy"))
@Field def impactUtils= loadScript(new File("${props.zAppBuildDir}/utilities/ImpactUtilities.groovy"))
@Field def bindUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BindUtilities.groovy"))
@Field RepositoryClient repositoryClient

println("** Building files mapped to ${this.class.getName()}.groovy script")

// verify required build properties
buildUtils.assertBuildProperties(props.cobol_requiredBuildProperties)

// create language datasets
def langQualifier = "REXX"
buildUtils.createLanguageDatasets(langQualifier)

// sort the build list based on build file rank if provided
List<String> sortedList = buildUtils.sortBuildList(argMap.buildList, 'REXX_fileBuildRank')

// iterate through build list
sortedList.each { buildFile ->
	println "*** Building file $buildFile"

	// copy build file and dependency files to data sets
	String rules = props.getFileProperty('REXX_resolutionRules', buildFile)
	DependencyResolver dependencyResolver = buildUtils.createDependencyResolver(buildFile, rules)
	buildUtils.copySourceFiles(buildFile, props.REXX_srcPDS, props.REXX_srcPDS, dependencyResolver)
	// create mvs commands
	LogicalFile logicalFile = dependencyResolver.getLogicalFile()
	String member = CopyToPDS.createMemberName(buildFile)
	File logFile = new File( props.userBuild ? "${props.buildOutDir}/${member}.log" : "${props.buildOutDir}/${member}.REXX.log")
	if (logFile.exists())
		logFile.delete()
	MVSExec compile = createCompileCommand(buildFile, logicalFile, member, logFile)
	MVSExec linkEdit = createLinkEditCommand(buildFile, logicalFile, member, logFile)

	// execute mvs commands in a mvs job
	MVSJob job = new MVSJob()
	job.start()

	// compile the cobol program
	int rc = compile.execute()
	int maxRC = props.getFileProperty('REXX_compileMaxRC', buildFile).toInteger()

	boolean bindFlag = true

	if (rc > maxRC) {
		bindFlag = false
		String errorMsg = "*! The compile return code ($rc) for $buildFile exceeded the maximum return code allowed ($maxRC)"
		println(errorMsg)
		props.error = "true"
		buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile],client:getRepositoryClient())
	}
	else {
		// if this program needs to be link edited . . .
		String needsLinking = props.getFileProperty('REXX_linkEdit', buildFile)
		if (needsLinking.toBoolean()) {
			rc = linkEdit.execute()
			maxRC = props.getFileProperty('REXX_linkEditMaxRC', buildFile).toInteger()

			if (rc > maxRC) {
				String errorMsg = "*! The link edit return code ($rc) for $buildFile exceeded the maximum return code allowed ($maxRC)"
				println(errorMsg)
				props.error = "true"
				buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile],client:getRepositoryClient())
			}
			else {
				if (!props.userBuild){
					// only scan the load module if load module scanning turned on for file
					String scanLoadModule = props.getFileProperty('REXX_scanLoadModule', buildFile)
					if (scanLoadModule && scanLoadModule.toBoolean() && getRepositoryClient())
						impactUtils.saveStaticLinkDependencies(buildFile, props.linkedit_loadPDS, logicalFile, repositoryClient)
				}
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
 * createCompileCommand - creates a MVSExec command for compiling the REXX program (buildFile)
 */
def createCompileCommand(String buildFile, LogicalFile logicalFile, String member, File logFile) {
	def parms = props.getFileProperty('REXX_compileParms', buildFile) ?: ""
	String compiler = props.getFileProperty('REXX_compiler', buildFile)

	// define the MVSExec command to compile the program
	MVSExec compile = new MVSExec().file(buildFile).pgm(compiler).parm(parms)

	// add DD statements to the compile command
	compile.dd(new DDStatement().name("SYSIN").dsn("${props.REXX_srcPDS}($member)").options('shr').report(true))
	
	compile.dd(new DDStatement().name("SYSPRINT").options(props.REXX_printTempOptions))
	compile.dd(new DDStatement().name("SYSTERM").options(props.REXX_printTempOptions))

	// Write SYSLIN to temporary dataset if performing link edit or to physical dataset
	String doLinkEdit = props.getFileProperty('REXX_linkEdit', buildFile)
	String linkEditStream = props.getFileProperty('REXX_linkEditStream', buildFile)
	String linkDebugExit = props.getFileProperty('REXX_linkDebugExit', buildFile)

	if (props.debug && linkDebugExit && doLinkEdit.toBoolean()){
		compile.dd(new DDStatement().name("SYSPUNCH").dsn("${props.REXX_objPDS}($member)").options('shr').output(true))
	} else if (doLinkEdit && doLinkEdit.toBoolean() && ( !linkEditStream || linkEditStream.isEmpty())) {
		compile.dd(new DDStatement().name("SYSPUNCH").dsn("&&TEMPOBJ").options(props.cobol_tempOptions).pass(true))
	} else {
		compile.dd(new DDStatement().name("SYSPUNCH").dsn("${props.REXX_objPDS}($member)").options('shr').output(true))
	}
	compile.dd(new DDStatement().name("CEXEC").dsn("${props.REXX_loadPDS}($member)").options('shr').output(true))
	
	// add a syslib to the compile command with optional bms output copybook and CICS concatenation
	compile.dd(new DDStatement().name("SYSLIB").dsn(props.REXX_srcPDS).options("shr"))
		
	// add custom concatenation
	def compileSyslibConcatenation = props.getFileProperty('REXX_compileSyslibConcatenation', buildFile) ?: ""
	if (compileSyslibConcatenation) {
		def String[] syslibDatasets = compileSyslibConcatenation.split(',');
		for (String syslibDataset : syslibDatasets )
		compile.dd(new DDStatement().dsn(syslibDataset).options("shr"))
	}
		
	// add a tasklib to the compile command 
	compile.dd(new DDStatement().name("TASKLIB").dsn(props."SFANLMD").options("shr"))

	if (props.SFELLOAD)
		compile.dd(new DDStatement().dsn(props.SFELLOAD).options("shr"))

	// add IDz User Build Error Feedback DDs
	if (props.errPrefix) {
		compile.dd(new DDStatement().name("SYSADATA").options("DUMMY"))
		// SYSXMLSD.XML suffix is mandatory for IDZ/ZOD to populate remote error list
		compile.dd(new DDStatement().name("SYSXMLSD").dsn("${props.hlq}.${props.errPrefix}.SYSXMLSD.XML").options(props.cobol_compileErrorFeedbackXmlOptions))
	}

	// add a copy command to the compile command to copy the SYSPRINT from the temporary dataset to an HFS log file
	compile.copy(new CopyToHFS().ddName("SYSPRINT").file(logFile).hfsEncoding(props.logEncoding))

	return compile
}


/*
 * createLinkEditCommand - creates a MVSExec command for link editing the REXX object module produced by the compile
 */
def createLinkEditCommand(String buildFile, LogicalFile logicalFile, String member, File logFile) {
	String parms = props.getFileProperty('REXX_linkEditParms', buildFile)
	String linker = props.getFileProperty('REXX_linkEditor', buildFile)
	String linkEditStream = props.getFileProperty('REXX_linkEditStream', buildFile)
	String linkDebugExit = props.getFileProperty('REXX_linkDebugExit', buildFile)

	// define the MVSExec command to link edit the program
	MVSExec linkedit = new MVSExec().file(buildFile).pgm(linker).parm(parms)

	// Create a physical link card
	if ( (linkEditStream) || (props.debug && linkDebugExit!= null)) {
		def langQualifier = "linkedit"
		buildUtils.createLanguageDatasets(langQualifier)
		def lnkFile = new File("${props.buildOutDir}/linkCard.lnk")
		if (lnkFile.exists())
			lnkFile.delete()

		if 	(linkEditStream)
			lnkFile << "  " + linkEditStream.replace("\\n","\n").replace('@{member}',member)
		else
			lnkFile << "  " + linkDebugExit.replace("\\n","\n").replace('@{member}',member)

		if (props.verbose)
			println("Copying ${props.buildOutDir}/linkCard.lnk to ${props.linkedit_srcPDS}($member)")
		new CopyToPDS().file(lnkFile).dataset(props.linkedit_srcPDS).member(member).execute()
		// Alloc SYSLIN
		linkedit.dd(new DDStatement().name("SYSLIN").dsn("${props.linkedit_srcPDS}($member)").options("shr"))
		// add the obj DD
		linkedit.dd(new DDStatement().name("OBJECT").dsn("${props.REXX_objPDS}($member)").options('shr'))

	} else { // no debug && no link card
		// Use &&TEMP from Compile
	}

	// add DD statements to the linkedit command
	String linkedit_deployType = props.getFileProperty('linkedit_deployType', buildFile)
	if ( linkedit_deployType == null )
		linkedit_deployType = 'LOAD'
	linkedit.dd(new DDStatement().name("SYSLMOD").dsn("${props.REXX_loadPDS}($member)").options('shr').output(true).deployType(linkedit_deployType))
	
	linkedit.dd(new DDStatement().name("SYSPRINT").options(props.REXX_printTempOptions))
	linkedit.dd(new DDStatement().name("SYSUT1").options(props.REXX_tempOptions))

	// add RESLIB if needed
	if ( props.RESLIB ) {
		linkedit.dd(new DDStatement().name("RESLIB").dsn(props.RESLIB).options("shr"))
	}

	// add a syslib to the compile command with optional CICS concatenation
	linkedit.dd(new DDStatement().name("SYSLIB").dsn(props.REXX_objPDS).options("shr"))
	
	// add custom concatenation
	def linkEditSyslibConcatenation = props.getFileProperty('REXX_linkEditSyslibConcatenation', buildFile) ?: ""
	if (linkEditSyslibConcatenation) {
		def String[] syslibDatasets = linkEditSyslibConcatenation.split(',');
		for (String syslibDataset : syslibDatasets )
		linkedit.dd(new DDStatement().dsn(syslibDataset).options("shr"))
	}
	linkedit.dd(new DDStatement().dsn(props.SCEELKED).options("shr"))

	// Add Debug Dataset to find the debug exit to SYSLIB
	if (props.debug && props.SEQAMOD)
		linkedit.dd(new DDStatement().dsn(props.SEQAMOD).options("shr"))

	// add a copy command to the linkedit command to append the SYSPRINT from the temporary dataset to the HFS log file
	linkedit.copy(new CopyToHFS().ddName("SYSPRINT").file(logFile).hfsEncoding(props.logEncoding).append(true))

	return linkedit
}


def getRepositoryClient() {
	if (!repositoryClient && props."dbb.RepositoryClient.url")
		repositoryClient = new RepositoryClient().forceSSLTrusted(true)

	return repositoryClient
}