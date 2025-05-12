@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*


// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def buildUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BuildUtilities.groovy"))
@Field def impactUtils= loadScript(new File("${props.zAppBuildDir}/utilities/ImpactUtilities.groovy"))


/***
 * 
 * Sample language script to compile and link Easytrieve source code
 *  
 *  https://www.ibm.com/docs/en/mufz/5.1?topic=jcl-jcmucl2jtwo-step-translate-link-without-proc
 *  
 */

println("** Building ${argMap.buildList.size()} ${argMap.buildList.size() == 1 ? 'file' : 'files'} mapped to ${this.class.getName()}.groovy script")

//verify required build properties
buildUtils.assertBuildProperties(props.easytrieve_requiredBuildProperties)

// create language datasets
def langQualifier = "easytrieve"
buildUtils.createLanguageDatasets(langQualifier)


//sort the build list based on build file rank if provided
List<String> sortedList = buildUtils.sortBuildList(argMap.buildList, 'easytrieve_fileBuildRank')
int currentBuildFileNumber = 1

//iterate through build list

sortedList.each { buildFile ->
	println "*** (${currentBuildFileNumber++}/${sortedList.size()}) Building file $buildFile"
	
	// configure dependency resolution and create logical file

	String dependencySearch = props.getFileProperty('easytrieve_dependencySearch', buildFile)
	SearchPathDependencyResolver dependencyResolver = new SearchPathDependencyResolver(dependencySearch)

	// copy source files and dependency files to data sets	
	buildUtils.copySourceFiles(buildFile, props.easytrieve_srcPDS, 'easytrieve_dependenciesDatasetMapping', null, dependencyResolver)

	// Get logical file
	LogicalFile logicalFile = buildUtils.createLogicalFile(dependencyResolver, buildFile)

	
	// create mvs commands
	String member = CopyToPDS.createMemberName(buildFile)
	String needsLinking = props.getFileProperty('easytrieve_linkEdit', buildFile)
	
	File logFile = new File( props.userBuild ? "${props.buildOutDir}/${member}.log" : "${props.buildOutDir}/${member}.ezt.log")
	if (logFile.exists())
		logFile.delete()
		
	MVSExec compile = createCompileCommand(buildFile, logicalFile, member, logFile)
	MVSExec linkEdit
	if (needsLinking.toBoolean()) linkEdit = createLinkEditCommand(buildFile, logicalFile, member, logFile)
	
	// execute mvs commands in a mvs job
	MVSJob job = new MVSJob()
	job.start()
	
	// compile the easytrieve program
	int rc = compile.execute()
	//int rc = 0
	int maxRC = props.getFileProperty('easytrieve_compileMaxRC', buildFile).toInteger()
	
	if (rc > maxRC) {
		String errorMsg = "*! The compile return code ($rc) for $buildFile exceeded the maximum return code allowed ($maxRC)"
		println(errorMsg)
		props.error = "true"
		buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile])
	}
	else {
	// if this program needs to be link edited . . .
		
		if (needsLinking.toBoolean()) {
			rc = linkEdit.execute()
			//rc = 0
			maxRC = props.getFileProperty('easytrieve_linkEditMaxRC', buildFile).toInteger()
		
			if (rc > maxRC) {
				String errorMsg = "*! The link edit return code ($rc) for $buildFile exceeded the maximum return code allowed ($maxRC)"
				println(errorMsg)
				props.error = "true"
				buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile])
			}
			else {
				// only scan the load module if load module scanning turned on for file
				String scanLoadModule = props.getFileProperty('easytrieve_scanLoadModule', buildFile)
				if (scanLoadModule && scanLoadModule.toBoolean())
					impactUtils.saveStaticLinkDependencies(buildFile, props.linkedit_loadPDS, logicalFile)
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
 * createEasytrieveParms - Builds up the EZT compiler parameter list from build and file properties
 */
def createEasytrieveParms(String buildFile, LogicalFile logicalFile) {
	def parms = props.getFileProperty('easytrieve_compileParms', buildFile) ?: ""
	
	if (parms.startsWith(','))
		parms = parms.drop(1)
		
	if (props.verbose) println "Easytrieve compiler parms for $buildFile = $parms"
	return parms
}

/*
 * createCompileCommand - creates a MVSExec command for compiling the EZT program (buildFile)
 */
def createCompileCommand(String buildFile, LogicalFile logicalFile, String member, File logFile) {
	String parms = createEasytrieveParms(buildFile, logicalFile)
	String compiler = props.getFileProperty('easytrieve_compiler', buildFile)
	
	// define the MVSExec command to compile the program
	MVSExec compile = new MVSExec().file(buildFile).pgm(compiler).parm(parms)
	
	// add DD statements to the compile command
	compile.dd(new DDStatement().name("SYSIN").dsn("${props.easytrieve_srcPDS}($member)").options('shr').report(true))
	compile.dd(new DDStatement().name("SYSPRINT").options(props.easytrieve_printTempOptions))
	(1..2).toList().each { num ->
		compile.dd(new DDStatement().name("SYSUT$num").options(props.easytrieve_tempOptions))
	}
	
	// define object dataset allocation
	compile.dd(new DDStatement().name("SYSLIN").dsn("${props.easytrieve_objPDS}($member)").options('shr').output(true).deployType("OBJ"))
		
	// add a syslib to the compile command
	compile.dd(new DDStatement().name("PANDD").dsn(props.easytrieve_cpyPDS).options("shr"))
	
	// add custom concatenation
	def easytrieveSyslibConcatenation = props.getFileProperty('easytrieve_easytrieveSyslibConcatenation', buildFile) ?: ""
	if (easytrieveSyslibConcatenation) {
		def String[] syslibDatasets = easytrieveSyslibConcatenation.split(',');
		for (String syslibDataset : syslibDatasets )
		compile.dd(new DDStatement().dsn(syslibDataset).options("shr"))
	}

	// add a tasklib to the compile command with optional concatenations
	compile.dd(new DDStatement().name("TASKLIB").dsn(props.EZETLOAD).options("shr"))
	
	// add optional DBRMLIB if build file contains DB2 code
	if (buildUtils.isSQL(logicalFile))
		compile.dd(new DDStatement().name("DBRMLIB").dsn("$props.easytrieve_dbrmPDS($member)").options('shr').output(true).deployType('DBRM'))
		
	// add a copy command to the compile command to copy the SYSPRINT from the temporary dataset to an HFS log file
	compile.copy(new CopyToHFS().ddName("SYSPRINT").file(logFile).hfsEncoding(props.logEncoding))
	
	return compile
}


/*
 * createLinkEditCommand - creates a MVSExec command for link editing the EZT object module produced by the compile
 */
def createLinkEditCommand(String buildFile, LogicalFile logicalFile, String member, File logFile) {
	String parms = props.getFileProperty('easytrieve_linkEditParms', buildFile)
	String linker = props.getFileProperty('easytrieve_linkEditor', buildFile)
	String linkEditStream = props.getFileProperty('easytrieve_linkEditStream', buildFile)
	
	// obtain githash for buildfile
	String easytrieve_storeSSI = props.getFileProperty('easytrieve_storeSSI', buildFile)
	if (easytrieve_storeSSI && easytrieve_storeSSI.toBoolean() && (props.mergeBuild || props.impactBuild || props.fullBuild)) {
		String ssi = buildUtils.getShortGitHash(buildFile)
		if (ssi != null) parms = parms + ",SSI=$ssi"
	}
	
	if (props.verbose) println "*** Link-Edit parms for $buildFile = $parms"
	
	// define the MVSExec command to link edit the program
	MVSExec linkedit = new MVSExec().file(buildFile).pgm(linker).parm(parms)
	
	// Assemble linkEditInstream to define SYSIN as instreamData
	String sysin_linkEditInstream = ''
	
	// appending configured linkEdit stream if specified
	if (linkEditStream) {
		sysin_linkEditInstream += "  " + linkEditStream.replace("\\n","\n").replace('@{member}',member)
	}
	
	// appending IDENTIFY statement to link phase for traceability of load modules
	// this adds an IDRU record, which can be retrieved with amblist
	def identifyLoad = props.getFileProperty('easytrieve_identifyLoad', buildFile)
	
	if (identifyLoad && identifyLoad.toBoolean()) {
		String identifyStatement = buildUtils.generateIdentifyStatement(buildFile, props.easytrieve_loadOptions)
		if (identifyStatement != null ) {
			sysin_linkEditInstream += identifyStatement
		}
	}
	
	// appending mq stub according to file flags
	if(buildUtils.isMQ(logicalFile)) {
		// include mq stub program
		// https://www.ibm.com/docs/en/ibm-mq/9.3?topic=files-mq-zos-stub-programs
		sysin_linkEditInstream += buildUtils.getMqStubInstruction(logicalFile)
	}

	// appending debug exit to link instructions
	if (props.debug && linkDebugExit!= null) {
		sysin_linkEditInstream += "   " + linkDebugExit.replace("\\n","\n").replace('@{member}',member)
	}

	// Define SYSIN dd as instream data
	if (sysin_linkEditInstream) {
		if (props.verbose) println("*** Generated linkcard input stream: \n $sysin_linkEditInstream")
		linkedit.dd(new DDStatement().name("SYSIN").instreamData(sysin_linkEditInstream))
	}

	// add SYSLIN along the reference to SYSIN if configured through sysin_linkEditInstream
	linkedit.dd(new DDStatement().name("SYSLIN").dsn("${props.easytrieve_objPDS}($member)").options('shr'))
	if (sysin_linkEditInstream) linkedit.dd(new DDStatement().ddref("SYSIN"))
	
	// add DD statements to the linkedit command
	String deployType = buildUtils.getDeployType("easytrieve", buildFile, logicalFile)
	linkedit.dd(new DDStatement().name("SYSLMOD").dsn("${props.easytrieve_loadPDS}($member)").options('shr').output(true).deployType(deployType))
	linkedit.dd(new DDStatement().name("SYSPRINT").options(props.easytrieve_printTempOptions))
	linkedit.dd(new DDStatement().name("SYSUT1").options(props.easytrieve_tempOptions))
	
	// add a syslib to the compile command with optional concatenation
	linkedit.dd(new DDStatement().name("SYSLIB").dsn(props.easytrieve_objPDS).options("shr"))
	
	// add custom concatenation
	def linkEditSyslibConcatenation = props.getFileProperty('easytrieve_linkEditSyslibConcatenation', buildFile) ?: ""
	if (linkEditSyslibConcatenation) {
		def String[] syslibDatasets = linkEditSyslibConcatenation.split(',');
		for (String syslibDataset : syslibDatasets )
		linkedit.dd(new DDStatement().dsn(syslibDataset).options("shr"))
	}

	linkedit.dd(new DDStatement().dsn(props.SCEELKED).options("shr"))
	
	// Add Debug Dataset to find the debug exit to SYSLIB
	if (props.debug && props.SEQAMOD)
		linkedit.dd(new DDStatement().dsn(props.SEQAMOD).options("shr"))

	if (buildUtils.isCICS(logicalFile))
		linkedit.dd(new DDStatement().dsn(props.SDFHLOAD).options("shr"))
	
	if (buildUtils.isIMS(logicalFile))
		linkedit.dd(new DDStatement().dsn(props.SDFSRESL).options("shr"))
			
	if (buildUtils.isSQL(logicalFile))
		linkedit.dd(new DDStatement().dsn(props.SDSNLOAD).options("shr"))

	if (buildUtils.isMQ(logicalFile))
		linkedit.dd(new DDStatement().dsn(props.SCSQLOAD).options("shr"))

	// add a copy command to the linkedit command to append the SYSPRINT from the temporary dataset to the HFS log file
	linkedit.copy(new CopyToHFS().ddName("SYSPRINT").file(logFile).hfsEncoding(props.logEncoding).append(true))
	
	return linkedit
}
