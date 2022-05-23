@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.repository.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*
import com.ibm.dbb.build.report.*
import com.ibm.dbb.build.report.records.*


// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def buildUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BuildUtilities.groovy"))
@Field def impactUtils= loadScript(new File("${props.zAppBuildDir}/utilities/ImpactUtilities.groovy"))
@Field RepositoryClient repositoryClient

@Field def resolverUtils
// Conditionally load the ResolverUtilities.groovy which require at least DBB 1.1.2
if (props.useSearchConfiguration && props.useSearchConfiguration.toBoolean() && buildUtils.assertDbbBuildToolkitVersion(props.dbbToolkitVersion, "1.1.2")) {
	resolverUtils = loadScript(new File("${props.zAppBuildDir}/utilities/ResolverUtilities.groovy"))}

println("** Building files mapped to ${this.class.getName()}.groovy script")

// verify required build properties
buildUtils.assertBuildProperties(props.assembler_requiredBuildProperties)

def langQualifier = "assembler"
buildUtils.createLanguageDatasets(langQualifier)

// sort the build list based on build file rank if provided
List<String> sortedList = buildUtils.sortBuildList(argMap.buildList, 'assembler_fileBuildRank')

// iterate through build list
sortedList.each { buildFile ->
	println "*** Building file $buildFile"

	// configure dependency resolution and create logical file 	
	def dependencyResolver
	LogicalFile logicalFile
	
	if (props.useSearchConfiguration && props.useSearchConfiguration.toBoolean() && props.assembler_dependencySearch && buildUtils.assertDbbBuildToolkitVersion(props.dbbToolkitVersion, "1.1.2")) { // use new SearchPathDependencyResolver
		String dependencySearch = props.getFileProperty('assembler_dependencySearch', buildFile)
		dependencyResolver = resolverUtils.createSearchPathDependencyResolver(dependencySearch)
		logicalFile = resolverUtils.createLogicalFile(dependencyResolver, buildFile)
	} else { // use deprecated DependencyResolver
		String rules = props.getFileProperty('assembler_resolutionRules', buildFile)
		dependencyResolver = buildUtils.createDependencyResolver(buildFile, rules)
		logicalFile = dependencyResolver.getLogicalFile()
	}
	
	// copy build file and dependency files to data sets
	buildUtils.copySourceFiles(buildFile, props.assembler_srcPDS, 'assembler_dependenciesDatasetMapping', null ,dependencyResolver)

	// create mvs commands
	String member = CopyToPDS.createMemberName(buildFile)
	File logFile = new File( props.userBuild ? "${props.buildOutDir}/${member}.log" : "${props.buildOutDir}/${member}.asm.log")
	if (logFile.exists())
		logFile.delete()
	MVSExec assembler_SQLTranslator = createAssemblerSQLTranslatorCommand(buildFile, logicalFile, member, logFile)
	MVSExec assembler_CICSTranslator = createAssemblerCICSTranslatorCommand(buildFile, logicalFile, member, logFile)
	MVSExec assembler = createAssemblerCommand(buildFile, logicalFile, member, logFile)
	MVSExec linkEdit = createLinkEditCommand(buildFile, logicalFile, member, logFile)

	// execute mvs commands in a mvs job
	MVSJob job = new MVSJob()
	job.start()

	// initialize return codes
	int rc = 0
	int maxRC = props.getFileProperty('assembler_maxRC', buildFile).toInteger()

	// SQL preprocessor
	if (buildUtils.isSQL(logicalFile)){
		rc = assembler_SQLTranslator.execute()
		if (rc > maxRC) {
			String errorMsg = "*! The assembler sql translator return code ($rc) for $buildFile exceeded the maximum return code allowed ($maxRC)"
			println(errorMsg)
			props.error = "true"
			buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile],client:getRepositoryClient())
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
	if (buildUtils.isCICS(logicalFile)){
		rc = assembler_CICSTranslator.execute()
		if (rc > maxRC) {
			String errorMsg = "*! The assembler cics translator return code ($rc) for $buildFile exceeded the maximum return code allowed ($maxRC)"
			println(errorMsg)
			props.error = "true"
			buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile],client:getRepositoryClient())
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
			buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile],client:getRepositoryClient())
		}
		else {
			// if this program needs to be link edited . . .
			String needsLinking = props.getFileProperty('assembler_linkEdit', buildFile)
			if (needsLinking && needsLinking.toBoolean()) {
				rc = linkEdit.execute()
				maxRC = props.getFileProperty('assembler_linkEditMaxRC', buildFile).toInteger()

				if (rc > maxRC) {
					String errorMsg = "*! The link edit return code ($rc) for $buildFile exceeded the maximum return code allowed ($maxRC)"
					println(errorMsg)
					props.error = "true"
					buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile],client:getRepositoryClient())
				}
				else {
					// only scan the load module if load module scanning turned on for file
					if(!props.userBuild){
						String scanLoadModule = props.getFileProperty('assembler_scanLoadModule', buildFile)
						if (scanLoadModule && scanLoadModule.toBoolean() && getRepositoryClient()) {
							String assembler_loadPDS = props.getFileProperty('assembler_loadPDS', buildFile)
							impactUtils.saveStaticLinkDependencies(buildFile, assembler_loadPDS, logicalFile, repositoryClient)
						}
					}
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
	assembler_SQLtranslator.dd(new DDStatement().name("TASKLIB").dsn(props.SDSNLOAD).options("shr"))

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
 * createCompileCommand - creates a MVSExec command for compiling the BMS Map (buildFile)
 */
def createAssemblerCommand(String buildFile, LogicalFile logicalFile, String member, File logFile) {
	def errPrefixOptions = props.getFileProperty('assembler_compileErrorPrefixParms', buildFile) ?: ""

	String parameters = props.getFileProperty('assembler_pgmParms', buildFile)

	if (props.errPrefix)
		parameters = "$parameters,$errPrefixOptions"

	// define the MVSExec command to compile the BMS map
	MVSExec assembler = new MVSExec().file(buildFile).pgm(props.assembler_pgm).parm(parameters)

	// add DD statements to the compile command
	String assembler_srcPDS = props.getFileProperty('assembler_srcPDS', buildFile)

	// Pass different allocations
	// Case: BATCH - allocation SYSIN
	if (!buildUtils.isCICS(logicalFile) && !buildUtils.isSQL(logicalFile)) assembler.dd(new DDStatement().name("SYSIN").dsn("${assembler_srcPDS}($member)").options('shr'))
	//	else assembler.dd(new DDStatement().name("SYSCIN").ddref("SYSIN"))

	// Case: CICS - translator overwrite Ddnames
	if (buildUtils.isCICS(logicalFile)) assembler.setDdnames("SYSLIN,,,SYSLIB,SYSPUNCH,,,,,,,,,,,,,,")
	else if (buildUtils.isSQL(logicalFile)) assembler.setDdnames("SYSLIN,,,SYSLIB,SYSCIN,,,,,,,,,,,,,,")

	assembler.dd(new DDStatement().name("SYSPRINT").options(props.assembler_tempOptions))
	assembler.dd(new DDStatement().name("SYSUT1").options(props.assembler_tempOptions))


	// Write SYSLIN to temporary dataset if performing link edit
	String doLinkEdit = props.getFileProperty('assembler_linkEdit', buildFile)
	if (doLinkEdit && doLinkEdit.toBoolean())
		assembler.dd(new DDStatement().name("SYSLIN").dsn("&&TEMPOBJ").options(props.assembler_tempOptions).pass(true))
	else
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
	if (buildUtils.isSQL(logicalFile))
		assembler.dd(new DDStatement().dsn("DBC0CFG.DB2.V12.SDSNSAMP").options("shr"))
	if (props.SDFSMAC)
		assembler.dd(new DDStatement().dsn(props.SDFSMAC).options("shr"))

	// add IDz User Build Error Feedback DDs
	if (props.errPrefix) {
		assembler.dd(new DDStatement().name("SYSADATA").options("DUMMY"))
		// SYSXMLSD.XML suffix is mandatory for IDZ/ZOD to populate remote error list
		assembler.dd(new DDStatement().name("SYSXMLSD").dsn("${props.hlq}.${props.errPrefix}.SYSXMLSD.XML").options(props.assembler_compileErrorFeedbackXmlOptions))
	}

	// add a copy command to the compile command to copy the SYSPRINT from the temporary dataset to an HFS log file
	assembler.copy(new CopyToHFS().ddName("SYSPRINT").file(logFile).hfsEncoding(props.logEncoding).append(true))
	return assembler
}


/*
 * createLinkEditCommand - creates a MVSExec xommand for link editing the assembler object module produced by the compile
 */
def createLinkEditCommand(String buildFile, LogicalFile logicalFile, String member, File logFile) {
	String parameters = props.getFileProperty('assembler_linkEditParms', buildFile)

	// obtain githash for buildfile
	String assembler_storeSSI = props.getFileProperty('assembler_storeSSI', buildFile)
	if (assembler_storeSSI && assembler_storeSSI.toBoolean() && (props.mergeBuild || props.impactBuild || props.fullBuild)) {
		String ssi = buildUtils.getShortGitHash(buildFile)
		if (ssi != null) parameters = parameters + ",SSI=$ssi"
	}
	
	// define the MVSExec command to link edit the program
	MVSExec linkedit = new MVSExec().file(buildFile).pgm(props.assembler_linkEditor).parm(parameters)

	// add DD statements to the linkedit command
	String assembler_loadPDS = props.getFileProperty('assembler_loadPDS', buildFile)
	String deployType = buildUtils.getDeployType("assembler", buildFile, logicalFile)
	linkedit.dd(new DDStatement().name("SYSLMOD").dsn("${assembler_loadPDS}($member)").options('shr').output(true).deployType(deployType))
	linkedit.dd(new DDStatement().name("SYSPRINT").options(props.assembler_tempOptions))
	linkedit.dd(new DDStatement().name("SYSUT1").options(props.assembler_tempOptions))

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

	if (buildUtils.isSQL(logicalFile))
		linkedit.dd(new DDStatement().dsn(props.SDSNLOAD).options("shr"))

	// add a copy command to the linkedit command to append the SYSPRINT from the temporary dataset to the HFS log file
	linkedit.copy(new CopyToHFS().ddName("SYSPRINT").file(logFile).hfsEncoding(props.logEncoding).append(true))
	return linkedit
}

def getRepositoryClient() {
	if (!repositoryClient && props."dbb.RepositoryClient.url")
		repositoryClient = new RepositoryClient().forceSSLTrusted(true)
	return repositoryClient
}

