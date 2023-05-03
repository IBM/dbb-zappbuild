@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.metadata.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*

// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def buildUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BuildUtilities.groovy"))
@Field def impactUtils= loadScript(new File("${props.zAppBuildDir}/utilities/ImpactUtilities.groovy"))
@Field def bindUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BindUtilities.groovy"))


println("** Building ${argMap.buildList.size()} ${argMap.buildList.size() == 1 ? 'file' : 'files'} mapped to ${this.class.getName()}.groovy script")

// verify required build properties
buildUtils.assertBuildProperties(props.rexx_requiredBuildProperties)

// create language datasets
def langQualifier = "rexx"
buildUtils.createLanguageDatasets(langQualifier)

// sort the build list based on build file rank if provided
List<String> sortedList = buildUtils.sortBuildList(argMap.buildList.sort(), 'rexx_fileBuildRank')
int currentBuildFileNumber = 1

// iterate through build list
sortedList.each { buildFile ->
	println "*** (${currentBuildFileNumber++}/${sortedList.size()}) Building file $buildFile"

	
	// configure dependency resolution and create logical file	
	String dependencySearch = props.getFileProperty('rexx_dependencySearch', buildFile)
	SearchPathDependencyResolver dependencyResolver = new SearchPathDependencyResolver(dependencySearch)
	
	// copy build file and dependency files to data sets
	buildUtils.copySourceFiles(buildFile, props.rexx_srcPDS, 'rexx_dependenciesDatasetMapping', null, dependencyResolver)

	// Get logical file
	LogicalFile logicalFile = buildUtils.createLogicalFile(dependencyResolver, buildFile)

	// create mvs commands
	String member = CopyToPDS.createMemberName(buildFile)
	File logFile = new File( props.userBuild ? "${props.buildOutDir}/${member}.log" : "${props.buildOutDir}/${member}.REXX.log")
	if (logFile.exists())
		logFile.delete()
	MVSExec compile = createCompileCommand(buildFile, logicalFile, member, logFile)
	File linkEditLogFile = new File( props.userBuild ? "${props.buildOutDir}/${member}.LinkEdit.log" : "${props.buildOutDir}/${member}.REXX.LinkEdit.log")
	if (linkEditLogFile.exists())
		linkEditLogFile.delete()
	MVSExec linkEdit = createLinkEditCommand(buildFile, logicalFile, member, linkEditLogFile)


	// execute mvs commands in a mvs job
	MVSJob job = new MVSJob()
	job.start()

	// compile the cobol program
	int rc = compile.execute()
	int maxRC = props.getFileProperty('rexx_compileMaxRC', buildFile).toInteger()

	boolean bindFlag = true

	if (rc > maxRC) {
		bindFlag = false
		String errorMsg = "*! The compile return code ($rc) for $buildFile exceeded the maximum return code allowed ($maxRC)"
		println(errorMsg)
		props.error = "true"
		buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile])
	}
	else {
		// if this program needs to be link edited . . .
		String needsLinking = props.getFileProperty('rexx_linkEdit', buildFile)
		if (needsLinking.toBoolean()) {
			rc = linkEdit.execute()
			maxRC = props.getFileProperty('rexx_linkEditMaxRC', buildFile).toInteger()

			if (rc > maxRC) {
				String errorMsg = "*! The link edit return code ($rc) for $buildFile exceeded the maximum return code allowed ($maxRC)"
				println(errorMsg)
				props.error = "true"
				buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile])
			}
			else {
				if (!props.userBuild){
					// only scan the load module if load module scanning turned on for file
					String scanLoadModule = props.getFileProperty('rexx_scanLoadModule', buildFile)
					if (scanLoadModule && scanLoadModule.toBoolean())
						impactUtils.saveStaticLinkDependencies(buildFile, props.rexx_loadPDS, logicalFile)
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
	def parms = props.getFileProperty('rexx_compileParms', buildFile) ?: ""
	String compiler = props.getFileProperty('rexx_compiler', buildFile)

	// define the MVSExec command to compile the program
	MVSExec compile = new MVSExec().file(buildFile).pgm(compiler).parm(parms)

	// add DD statements to the compile command
	compile.dd(new DDStatement().name("SYSIN").dsn("${props.rexx_srcPDS}($member)").options('shr').report(true))
	
	compile.dd(new DDStatement().name("SYSPRINT").options(props.rexx_rexxPrintTempOptions))
	compile.dd(new DDStatement().name("SYSTERM").options(props.rexx_tempOptions))
	
	// Write SYSLIN to temporary dataset if performing link edit or to physical dataset
	String doLinkEdit = props.getFileProperty('rexx_linkEdit', buildFile)
	String linkEditStream = props.getFileProperty('rexx_linkEditStream', buildFile)
	String linkDebugExit = props.getFileProperty('rexx_linkDebugExit', buildFile)

	compile.dd(new DDStatement().name("SYSPUNCH").dsn("${props.rexx_objPDS}($member)").options('shr').output(true))
	String deployType = buildUtils.getDeployType("rexx_cexec", buildFile, null)
	compile.dd(new DDStatement().name("SYSCEXEC").dsn("${props.rexx_cexecPDS}($member)").options('shr').output(true).deployType(deployType))
	
	// add a syslib to the compile command with optional bms output copybook and CICS concatenation
	compile.dd(new DDStatement().name("SYSLIB").dsn(props.rexx_srcPDS).options("shr"))

	// add additional datasets with dependencies based on the dependenciesDatasetMapping
	PropertyMappings dsMapping = new PropertyMappings('rexx_dependenciesDatasetMapping')
	dsMapping.getValues().each { targetDataset ->
		// exclude the defaults rexx_srcPDS
		if (targetDataset != 'rexx_srcPDS')
			compile.dd(new DDStatement().dsn(props.getProperty(targetDataset)).options("shr"))
	}
			
	// add custom concatenation
	def compileSyslibConcatenation = props.getFileProperty('rexx_compileSyslibConcatenation', buildFile) ?: ""
	if (compileSyslibConcatenation) {
		def String[] syslibDatasets = compileSyslibConcatenation.split(',');
		for (String syslibDataset : syslibDatasets )
		compile.dd(new DDStatement().dsn(syslibDataset).options("shr"))
	}
		
	// add a tasklib to the compile command 
	compile.dd(new DDStatement().name("TASKLIB").dsn(props.SFANLMD).options("shr"))

	if (props.SFELLOAD)
		compile.dd(new DDStatement().dsn(props.SFELLOAD).options("shr"))

	// add a copy command to the compile command to copy the SYSPRINT from the temporary dataset to an HFS log file
	compile.copy(new CopyToHFS().ddName("SYSPRINT").file(logFile).hfsEncoding(props.logEncoding))

	return compile
}


/*
 * createLinkEditCommand - creates a MVSExec command for link editing the REXX object module produced by the compile
 */
def createLinkEditCommand(String buildFile, LogicalFile logicalFile, String member, File logFile) {
	String parms = props.getFileProperty('rexx_linkEditParms', buildFile)
	String linker = props.getFileProperty('rexx_linkEditor', buildFile)
	String linkEditStream = props.getFileProperty('rexx_linkEditStream', buildFile)
	String linkDebugExit = props.getFileProperty('rexx_linkDebugExit', buildFile)

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
		linkedit.dd(new DDStatement().name("OBJECT").dsn("${props.rexx_objPDS}($member)").options('shr'))

	} else { // no debug && no link card
		linkedit.dd(new DDStatement().name("SYSLIN").dsn("${props.rexx_objPDS}($member)").options('shr'))
	}

	// add DD statements to the linkedit command
	String deployType = buildUtils.getDeployType("rexx", buildFile, logicalFile)
	linkedit.dd(new DDStatement().name("SYSLMOD").dsn("${props.rexx_loadPDS}($member)").options('shr').output(true).deployType(deployType))
	
	linkedit.dd(new DDStatement().name("SYSPRINT").options(props.rexx_printTempOptions))
	linkedit.dd(new DDStatement().name("SYSUT1").options(props.rexx_tempOptions))

	// add RESLIB if needed
	if ( props.RESLIB ) {
		linkedit.dd(new DDStatement().name("RESLIB").dsn(props.RESLIB).options("shr"))
	}

	// add a syslib to the compile command with optional CICS concatenation
	linkedit.dd(new DDStatement().name("SYSLIB").dsn(props.rexx_objPDS).options("shr"))
	
	// add custom concatenation
	def linkEditSyslibConcatenation = props.getFileProperty('rexx_linkEditSyslibConcatenation', buildFile) ?: ""
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

