@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.metadata.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*
import com.ibm.dbb.build.report.*
import com.ibm.dbb.build.report.records.*


// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def buildUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BuildUtilities.groovy"))
@Field def impactUtils= loadScript(new File("${props.zAppBuildDir}/utilities/ImpactUtilities.groovy"))

println("** Building ${argMap.buildList.size()} ${argMap.buildList.size() == 1 ? 'file' : 'files'} mapped to ${this.class.getName()}.groovy script")

// verify required build properties
buildUtils.assertBuildProperties(props.assembler_requiredBuildProperties)

def langQualifier = "assembler"
buildUtils.createLanguageDatasets(langQualifier)

// create sysadata dataset used in errorPrefix and debug
if (props.errPrefix || props.debug) {
	buildUtils.createDatasets(props.assembler_sysadataPDS.split(), props.assembler_sysadataOptions)
}

// create debug dataset for the sidefile
if (props.debug) {
	buildUtils.createDatasets(props.assembler_debugPDS.split(), props.assembler_sidefileOptions)
}

// sort the build list based on build file rank if provided
List<String> sortedList = buildUtils.sortBuildList(argMap.buildList.sort(), 'assembler_fileBuildRank')
int currentBuildFileNumber = 1

// iterate through build list
sortedList.each { buildFile ->
	println "*** (${currentBuildFileNumber++}/${sortedList.size()}) Building file $buildFile"

	// Configure dependency resolution
	String dependencySearch = props.getFileProperty('assembler_dependencySearch', buildFile)
	SearchPathDependencyResolver dependencyResolver = new SearchPathDependencyResolver(dependencySearch)
	
	// Copy build file and dependency files to data sets
	buildUtils.copySourceFiles(buildFile, props.assembler_srcPDS, 'assembler_dependenciesDatasetMapping', null ,dependencyResolver)

	// Create logical file
	LogicalFile logicalFile = buildUtils.createLogicalFile(dependencyResolver, buildFile)

	// print logicalFile details and overrides
	if (props.verbose) buildUtils.printLogicalFileAttributes(logicalFile)
	
	// create mvs commands
	String member = CopyToPDS.createMemberName(buildFile)
	String needsLinking = props.getFileProperty('assembler_linkEdit', buildFile)
	
	File logFile = new File( props.userBuild ? "${props.buildOutDir}/${member}.log" : "${props.buildOutDir}/${member}.asm.log")
	if (logFile.exists())
		logFile.delete()
	MVSExec assembler_SQLTranslator = createAssemblerSQLTranslatorCommand(buildFile, logicalFile, member, logFile)
	MVSExec assembler_CICSTranslator = createAssemblerCICSTranslatorCommand(buildFile, logicalFile, member, logFile)
	MVSExec assembler = createAssemblerCommand(buildFile, logicalFile, member, logFile)
	MVSExec debugSideFile = createDebugSideFile(buildFile, logicalFile, member, logFile)
	MVSExec linkEdit 
	if (needsLinking.toBoolean()) linkEdit = createLinkEditCommand(buildFile, logicalFile, member, logFile)

	// execute mvs commands in a mvs job
	MVSJob job = new MVSJob()
	job.start()

	// initialize return codes
	int rc = 0
	
	int maxRC = props.getFileProperty('assembler_maxRC', buildFile).toInteger()

	// SQL preprocessor
	if (buildUtils.isSQL(logicalFile)){
		rc = assembler_SQLTranslator.execute()
		maxRC = props.getFileProperty('assembler_maxSQLTranslatorRC', buildFile).toInteger()
		
		if (rc > maxRC) {
			String errorMsg = "*! The assembler sql translator return code ($rc) for $buildFile exceeded the maximum return code allowed ($maxRC)"
			println(errorMsg)
			props.error = "true"
			buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile])
		} else {
			// Store db2 bind information as a generic property record in the BuildReport
			String generateDb2BindInfoRecord = props.getFileProperty('generateDb2BindInfoRecord', buildFile)
			if (generateDb2BindInfoRecord.toBoolean()){
				PropertiesRecord db2BindInfoRecord = buildUtils.generateDb2InfoRecord(buildFile)
				BuildReportFactory.getBuildReport().addRecord(db2BindInfoRecord)
			}
		}
	}

	// CICS preprocessor
	if (rc <= maxRC && buildUtils.isCICS(logicalFile)){
		rc = assembler_CICSTranslator.execute()
		maxRC = props.getFileProperty('assembler_maxCICSTranslatorRC', buildFile).toInteger()
		
		if (rc > maxRC) {
			String errorMsg = "*! The assembler cics translator return code ($rc) for $buildFile exceeded the maximum return code allowed ($maxRC)"
			println(errorMsg)
			props.error = "true"
			buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile])
		}
	}

	// Assembler
	if (rc <= maxRC) {
		rc = assembler.execute()
		maxRC = props.getFileProperty('assembler_maxRC', buildFile).toInteger()

		if (rc > maxRC) {
			String errorMsg = "*! The assembler return code ($rc) for $buildFile exceeded the maximum return code allowed ($maxRC)"
			println(errorMsg)
			props.error = "true"
			buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile])
		}
	}
	
	// create sidefile
	if (rc <= maxRC && props.debug) {
		rc = debugSideFile.execute()
		maxRC = props.getFileProperty('assembler_maxIDILANGX_RC', buildFile).toInteger()
		
		if (rc > maxRC) {
			String errorMsg = "*! The preparation step of the sidefile EQALANX return code ($rc) for $buildFile exceeded the maximum return code allowed ($maxRC)"
			println(errorMsg)
			props.error = "true"
			buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile])
		}
	}

	
	// linkedit
	if (rc <= maxRC && needsLinking && needsLinking.toBoolean()) {

		rc = linkEdit.execute()
		maxRC = props.getFileProperty('assembler_linkEditMaxRC', buildFile).toInteger()

		if (rc > maxRC) {
			String errorMsg = "*! The link edit return code ($rc) for $buildFile exceeded the maximum return code allowed ($maxRC)"
			println(errorMsg)
			props.error = "true"
			buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile])
		}
		else {
			// only scan the load module if load module scanning turned on for file
			if(!props.userBuild){
				String scanLoadModule = props.getFileProperty('assembler_scanLoadModule', buildFile)
				if (scanLoadModule && scanLoadModule.toBoolean()) {
					String assembler_loadPDS = props.getFileProperty('assembler_loadPDS', buildFile)
					impactUtils.saveStaticLinkDependencies(buildFile, assembler_loadPDS, logicalFile)
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

/**
 * createAssemblerTranslator for SQL
 *
 */

//*            PRE-COMPILE THE ASSEMBLER PROGRAM
//**********************************************************************
//PC      EXEC PGM=DSNHPC,PARM='HOST(ASM)'
// DBRMLIB  DD DISP=OLD,DSN=&USER..DBRMLIB.DATA(&MEM)
//>STEPLIB  DD DISP=SHR,DSN=DSN!!0.SDSNEXIT
//          DD DISP=SHR,DSN=DSN!!0.SDSNLOAD
// SYSCIN   DD  DSN=&&DSNHOUT,DISP=(MOD,PASS),UNIT=SYSDA,
//             SPACE=(800,(&WSPC,&WSPC))
//SYSLIB   DD  DISP=SHR,DSN=&USER..SRCLIB.DATA
// SYSPRINT DD  SYSOUT=*
//SYSTERM  DD  SYSOUT=*
//SYSUDUMP DD  SYSOUT=*
//SYSUT1   DD  SPACE=(800,(&WSPC,&WSPC),,,ROUND),UNIT=SYSDA
//*
//*            ASSEMBLE IF THE PRECOMPILE RETURN CODE
//*            IS 4 OR LESS
//*

def createAssemblerSQLTranslatorCommand(String buildFile, LogicalFile logicalFile, String member, File logFile) {

	// TODO -> build-conf/assembler.properties: Externalise pgm 
	// TODO: Externalise parm
	String assembler_db2precompiler = props.getFileProperty('assembler_db2precompiler', buildFile)
	String assembler_db2precompilerParms = props.getFileProperty('assembler_db2precompilerParms', buildFile)
	
			
	MVSExec assembler_SQLtranslator = new MVSExec().file(buildFile).pgm(assembler_db2precompiler).parm(assembler_db2precompilerParms)

	// add DD statements to the compile command
	String assembler_srcPDS = props.getFileProperty('assembler_srcPDS', buildFile)

	// input file
	assembler_SQLtranslator.dd(new DDStatement().name("SYSIN").dsn("${assembler_srcPDS}($member)").options('shr'))

	// outputs dbrmlib + temp dataset
	assembler_SQLtranslator.dd(new DDStatement().name("DBRMLIB").dsn("$props.assembler_dbrmPDS($member)").options('shr').output(true).deployType('DBRM'))
	assembler_SQLtranslator.dd(new DDStatement().name("SYSCIN").dsn("&&SYSCIN").options('cyl space(5,5) unit(vio) new').pass(true))

	// steplib
	if (props.SDSNEXIT) {
		assembler_SQLtranslator.dd(new DDStatement().name("TASKLIB").dsn(props.SDSNEXIT).options("shr"))
		assembler_SQLtranslator.dd(new DDStatement().dsn(props.SDSNLOAD).options("shr"))

	} else {
		assembler_SQLtranslator.dd(new DDStatement().name("TASKLIB").dsn(props.SDSNLOAD).options("shr"))
	}
	
	assembler_SQLtranslator.dd(new DDStatement().name("SYSUT1").options(props.assembler_tempOptions))

	// sysprint
	assembler_SQLtranslator.dd(new DDStatement().name("SYSPRINT").options(props.assembler_tempOptionsTranslator))

	// add a copy command to the compile command to copy the SYSPRINT from the temporary dataset to an HFS log file
	assembler_SQLtranslator.copy(new CopyToHFS().ddName("SYSPRINT").file(logFile).hfsEncoding(props.logEncoding).append(true))

	return assembler_SQLtranslator
}

/**
 * createAssemblerTranslator for CICS
 *
 */

//TRN    EXEC PGM=DFHEAP&SUFFIX,
//            REGION=&REG
//STEPLIB  DD DSN=&INDEX..SDFHLOAD,DISP=SHR
//SYSPRINT DD SYSOUT=&OUTC
//SYSPUNCH DD DSN=&&SYSCIN,
//            DISP=(,PASS),UNIT=&WORK,
//            DCB=BLKSIZE=400,
//            SPACE=(400,(400,100))

def createAssemblerCICSTranslatorCommand(String buildFile, LogicalFile logicalFile, String member, File logFile) {

	// TODO: Externalise DFH pgm
	String assember_cicsprecompiler = props.getFileProperty('assember_cicsprecompiler', buildFile)
	String assember_cicsprecompilerParms = props.getFileProperty('assember_cicsprecompilerParms', buildFile)
	
	MVSExec assembler_CICStranslator = new MVSExec().file(buildFile).pgm(assember_cicsprecompiler).parm(assember_cicsprecompilerParms)

	// add DD statements to the compile command
	String assembler_srcPDS = props.getFileProperty('assembler_srcPDS', buildFile)

	if (buildUtils.isSQL(logicalFile)) assembler_CICStranslator.setDdnames("SYSLIN,,,SYSLIB,SYSCIN,,,,,,,,,,,,,,")
	else assembler_CICStranslator.dd(new DDStatement().name("SYSIN").dsn("${assembler_srcPDS}($member)").options('shr'))
	//assembler_translator.dd(new DDStatement().name("SYSPRINT").options('cyl space(5,5) unit(vio) blksize(400) lrecl(80) recfm(f,b) new'))
	assembler_CICStranslator.dd(new DDStatement().name("SYSPRINT").options(props.assembler_tempOptionsTranslator))


	assembler_CICStranslator.dd(new DDStatement().name("SYSPUNCH").dsn("&&SYSCICS").options('cyl space(5,5) unit(vio) blksize(400) lrecl(80) recfm(f,b) new').pass(true))

	assembler_CICStranslator.dd(new DDStatement().name("TASKLIB").dsn(props.SDFHLOAD).options("shr"))

	// add a copy command to the compile command to copy the SYSPRINT from the temporary dataset to an HFS log file
	assembler_CICStranslator.copy(new CopyToHFS().ddName("SYSPRINT").file(logFile).hfsEncoding(props.logEncoding).append(true))

	return assembler_CICStranslator
}

/*
 * createCompileCommand - creates a MVSExec command for compiling the source code
 */
def createAssemblerCommand(String buildFile, LogicalFile logicalFile, String member, File logFile) {
	
	def errPrefixOptions = props.getFileProperty('assembler_compileErrorPrefixParms', buildFile) ?: ""
	def debugOptions = props.getFileProperty('assembler_debugParms', buildFile) ?: ""
		
	String parameters = props.getFileProperty('assembler_pgmParms', buildFile)

	if (props.errPrefix)
		parameters = "$parameters,$errPrefixOptions"

	if (props.debug)
		parameters = "$parameters,$debugOptions"	

	if (props.verbose) println "*** Assembler options for $buildFile = $parameters"
				
	// define the MVSExec command to compile the BMS map
	MVSExec assembler = new MVSExec().file(buildFile).pgm(props.assembler_pgm).parm(parameters)

	// asma options file
	def asmaOpts = props.getFileProperty('assembler_asmaOptFile', buildFile) ?: ""
	if (asmaOpts) assembler.dd(new DDStatement().name("ASMAOPT").dsn("${asmaOpts}").options("shr"))
	
	// add DD statements to the compile command
	String assembler_srcPDS = props.getFileProperty('assembler_srcPDS', buildFile)

	// Pass different input allocations
	if (buildUtils.isCICS(logicalFile)) {  	// Case: CICS - translator overwrite Ddnames
		assembler.setDdnames("SYSLIN,,,SYSLIB,SYSPUNCH,,,,,,,,,,,,,,")
	} else if (buildUtils.isSQL(logicalFile)) { // Case: Db2 - translator overwrite Ddnames
		assembler.setDdnames("SYSLIN,,,SYSLIB,SYSCIN,,,,,,,,,,,,,,")
	} else { // Case: Plain batch
		assembler.dd(new DDStatement().name("SYSIN").dsn("${assembler_srcPDS}($member)").options('shr'))
	}

	assembler.dd(new DDStatement().name("SYSPRINT").options(props.assembler_tempOptions))
	assembler.dd(new DDStatement().name("SYSUT1").options(props.assembler_tempOptions))

	// define object dataset allocation
	assembler.dd(new DDStatement().name("SYSLIN").dsn("${props.assembler_objPDS}($member)").options('shr').output(true))

	// create a SYSLIB concatenation with optional MACLIB and MODGEN
	assembler.dd(new DDStatement().name("SYSLIB").dsn(props.assembler_macroPDS).options("shr"))
	
	// add additional datasets with dependencies based on the dependenciesDatasetMapping
	PropertyMappings dsMapping = new PropertyMappings('assembler_dependenciesDatasetMapping')
	dsMapping.getValues().each { targetDataset ->
		// exclude the defaults assembler_macroPDS
		if (targetDataset != 'assembler_macroPDS')
			assembler.dd(new DDStatement().dsn(props.getProperty(targetDataset)).options("shr"))
	}
	
	// add custom external concatenations
	def assemblySyslibConcatenation = props.getFileProperty('assembler_assemblySyslibConcatenation', buildFile) ?: ""
	if (assemblySyslibConcatenation) {
		def String[] syslibDatasets = assemblySyslibConcatenation.split(',');
		for (String syslibDataset : syslibDatasets )
			assembler.dd(new DDStatement().dsn(syslibDataset).options("shr"))
	}
	if (props.SCEEMAC)
		assembler.dd(new DDStatement().dsn(props.SCEEMAC).options("shr"))
	if (props.MACLIB)
		assembler.dd(new DDStatement().dsn(props.MACLIB).options("shr"))
	if (props.MODGEN)
		assembler.dd(new DDStatement().dsn(props.MODGEN).options("shr"))
	if (buildUtils.isCICS(logicalFile))
		assembler.dd(new DDStatement().dsn(props.SDFHMAC).options("shr"))
	//if (buildUtils.isSQL(logicalFile))
	if (buildUtils.isMQ(logicalFile)) 		
		assembler.dd(new DDStatement().dsn(props.SCSQMACS).options("shr"))
	if (props.SDFSMAC)
		assembler.dd(new DDStatement().dsn(props.SDFSMAC).options("shr"))

	// SYSADATA allocation
	if (props.errPrefix || props.debug) {	
		assembler.dd(new DDStatement().name("SYSADATA").dsn("${props.assembler_sysadataPDS}($member)").options("shr"))
	}	
	
	// add IDz User Build Error Feedback DDs
	if (props.errPrefix) {
		// SYSXMLSD.XML suffix is mandatory for IDZ/ZOD to populate remote error list
		assembler.dd(new DDStatement().name("SYSXMLSD").dsn("${props.hlq}.${props.errPrefix}.SYSXMLSD.XML").options(props.assembler_compileErrorFeedbackXmlOptions))
	}

	// add a copy command to the compile command to copy the SYSPRINT from the temporary dataset to an HFS log file
	assembler.copy(new CopyToHFS().ddName("SYSPRINT").file(logFile).hfsEncoding(props.logEncoding).append(true))
	return assembler
}


/*
 * createDebugSideFileCommand - creates a MVSExec command creating a IDILANGX side file
 * https://www.ibm.com/docs/en/developer-for-zos/16.0?topic=program-creating-eqalangx-file-assembler
 * 
 */
def createDebugSideFile(String buildFile, LogicalFile logicalFile, String member, File logFile) {

	String parameters = props.getFileProperty('assembler_eqalangxParms', buildFile)
	
	MVSExec generateSidefile = new MVSExec().file(buildFile).pgm(props.assembler_eqalangx).parm(parameters)
	generateSidefile.dd(new DDStatement().name("TASKLIB").dsn("${props.PDTCCMOD}").options("shr"))
	generateSidefile.dd(new DDStatement().name("SYSADATA").dsn("${props.assembler_sysadataPDS}($member)").options("shr"))
	generateSidefile.dd(new DDStatement().name("IDILANGX").dsn("${props.assembler_debugPDS}($member)").options("shr").output(true).deployType("EQALANGX"))
	return generateSidefile
}

/*
 * createLinkEditCommand - creates a MVSExec xommand for link editing the assembler object module produced by the compile
 */
def createLinkEditCommand(String buildFile, LogicalFile logicalFile, String member, File logFile) {
	String parameters = props.getFileProperty('assembler_linkEditParms', buildFile)
	String linkEditStream = props.getFileProperty('assembler_linkEditStream', buildFile)
	
	
	// obtain githash for buildfile
	String assembler_storeSSI = props.getFileProperty('assembler_storeSSI', buildFile)
	if (assembler_storeSSI && assembler_storeSSI.toBoolean() && (props.mergeBuild || props.impactBuild || props.fullBuild)) {
		String ssi = buildUtils.getShortGitHash(buildFile)
		if (ssi != null) parameters = parameters + ",SSI=$ssi"
	}
	
	if (props.verbose) println "*** Link-Edit parms for $buildFile = $parameters"
	
	// define the MVSExec command to link edit the program
	MVSExec linkedit = new MVSExec().file(buildFile).pgm(props.assembler_linkEditor).parm(parameters)
	
	// add DD statements to the linkedit command
	String assembler_loadPDS = props.getFileProperty('assembler_loadPDS', buildFile)
	String deployType = buildUtils.getDeployType("assembler", buildFile, logicalFile)
	linkedit.dd(new DDStatement().name("SYSLMOD").dsn("${assembler_loadPDS}($member)").options('shr').output(true).deployType(deployType))
	
	linkedit.dd(new DDStatement().name("SYSPRINT").options(props.assembler_tempOptions))
	linkedit.dd(new DDStatement().name("SYSUT1").options(props.assembler_tempOptions))

	// Create linkEditInstream
	String sysin_linkEditInstream = ''
	// linkEdit stream specified
	if (linkEditStream) {
		sysin_linkEditInstream += "  " + linkEditStream.replace("\\n","\n").replace('@{member}',member)
	}
	
	// appending IDENTIFY statement to link phase for traceability of load modules
	// this adds an IDRU record, which can be retrieved with amblist
	def identifyLoad = props.getFileProperty('assembler_identifyLoad', buildFile)
	if (identifyLoad && identifyLoad.toBoolean()) {
		String identifyStatement = buildUtils.generateIdentifyStatement(buildFile, props.assembler_loadOptions)
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

	// Define SYSIN dd as instream data
	if (sysin_linkEditInstream) {
		if (props.verbose) println("*** Generated linkcard input stream: \n $sysin_linkEditInstream")
		linkedit.dd(new DDStatement().name("SYSIN").instreamData(sysin_linkEditInstream))
	}

	// add SYSLIN along the reference to SYSIN if configured through sysin_linkEditInstream
	linkedit.dd(new DDStatement().name("SYSLIN").dsn("${props.assembler_objPDS}($member)").options('shr'))
	if (sysin_linkEditInstream) linkedit.dd(new DDStatement().ddref("SYSIN"))
	
	
	// add a syslib to the linkedit command
	linkedit.dd(new DDStatement().name("SYSLIB").dsn(props.assembler_objPDS).options("shr"))
	// add custom concatenation
	def linkEditSyslibConcatenation = props.getFileProperty('assembler_linkEditSyslibConcatenation', buildFile) ?: ""
	if (linkEditSyslibConcatenation) {
		def String[] syslibDatasets = linkEditSyslibConcatenation.split(',');
		for (String syslibDataset : syslibDatasets )
			linkedit.dd(new DDStatement().dsn(syslibDataset).options("shr"))
	}
	linkedit.dd(new DDStatement().dsn(props.SCEELKED).options("shr"))

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

