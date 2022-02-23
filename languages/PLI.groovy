@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.repository.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*
import com.ibm.jzos.ZFile
import com.ibm.dbb.build.report.*
import com.ibm.dbb.build.report.records.*


// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def buildUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BuildUtilities.groovy"))
@Field def impactUtils= loadScript(new File("${props.zAppBuildDir}/utilities/ImpactUtilities.groovy"))
@Field RepositoryClient repositoryClient

println("** Building files mapped to ${this.class.getName()}.groovy script")

// verify required build properties
buildUtils.assertBuildProperties(props.pli_requiredBuildProperties)

def langQualifier = "pli"
buildUtils.createLanguageDatasets(langQualifier)

// sort the build list based on build file rank if provided
List<String> sortedList = buildUtils.sortBuildList(argMap.buildList, 'pli_fileBuildRank')

if (buildListContainsTests(sortedList)) {
	langQualifier = "pli_test"
	buildUtils.createLanguageDatasets(langQualifier)
}

// iterate through build list
sortedList.each { buildFile ->
	println "*** Building file $buildFile"

	// Check if this a testcase
	isZUnitTestCase = (props.getFileProperty('pli_testcase', buildFile).equals('true')) ? true : false

	// copy build file to input data set
	// copy build file and dependency files to data sets
	String rules = props.getFileProperty('pli_resolutionRules', buildFile)
	DependencyResolver dependencyResolver = buildUtils.createDependencyResolver(buildFile, rules)
	
	if(isZUnitTestCase){
		buildUtils.copySourceFiles(buildFile, props.pli_testcase_srcPDS, null, null, null)
	}else{
		buildUtils.copySourceFiles(buildFile, props.pli_srcPDS, 'pli_dependenciesDatasetMapping', props.pli_dependenciesAlternativeLibraryNameMapping, dependencyResolver)
	}

	// create mvs commands
	LogicalFile logicalFile = dependencyResolver.getLogicalFile()
	String member = CopyToPDS.createMemberName(buildFile)
	File logFile = new File( props.userBuild ? "${props.buildOutDir}/${member}.log" : "${props.buildOutDir}/${member}.pli.log")
	if (logFile.exists())
		logFile.delete()
	MVSExec compile = createCompileCommand(buildFile, logicalFile, member, logFile)
	MVSExec linkEdit = createLinkEditCommand(buildFile, logicalFile, member, logFile)

	// execute mvs commands in a mvs job
	MVSJob job = new MVSJob()
	job.start()

	// compile the program
	int rc = compile.execute()
	int maxRC = props.getFileProperty('pli_compileMaxRC', buildFile).toInteger()

	if (rc > maxRC) {
		String errorMsg = "*! The compile return code ($rc) for $buildFile exceeded the maximum return code allowed ($maxRC)"
		println(errorMsg)
		props.error = "true"
		buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile],client:getRepositoryClient())
	}
	else {
		// if this program needs to be link edited . . .

		// Store db2 bind information as a generic property record in the BuildReport
		String generateDb2BindInfoRecord = props.getFileProperty('generateDb2BindInfoRecord', buildFile)
		if (buildUtils.isSQL(logicalFile) && generateDb2BindInfoRecord.toBoolean() ){
			PropertiesRecord db2BindInfoRecord = buildUtils.generateDb2InfoRecord(buildFile)
			BuildReportFactory.getBuildReport().addRecord(db2BindInfoRecord)
		}

		String needsLinking = props.getFileProperty('pli_linkEdit', buildFile)
		if (needsLinking.toBoolean()) {
			rc = linkEdit.execute()
			maxRC = props.getFileProperty('pli_linkEditMaxRC', buildFile).toInteger()

			if (rc > maxRC) {
				String errorMsg = "*! The link edit return code ($rc) for $buildFile exceeded the maximum return code allowed ($maxRC)"
				println(errorMsg)
				props.error = "true"
				buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile],client:getRepositoryClient())
			}
			else {
				// only scan the load module if load module scanning turned on for file
				if(!props.userBuild && !isZUnitTestCase){
					String scanLoadModule = props.getFileProperty('pli_scanLoadModule', buildFile)
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
 * createPLIParms - Builds up the PLI compiler parameter list from build and file properties
 */
def createPLIParms(String buildFile, LogicalFile logicalFile) {
	def parms = props.getFileProperty('pli_compileParms', buildFile) ?: ""
	def cics = props.getFileProperty('pli_compileCICSParms', buildFile) ?: ""
	def sql = props.getFileProperty('pli_compileSQLParms', buildFile) ?: ""
	def errPrefixOptions = props.getFileProperty('pli_compileErrorPrefixParms', buildFile) ?: ""
	def compileDebugParms = props.getFileProperty('pli_compileDebugParms', buildFile)


	if (buildUtils.isCICS(logicalFile))
		parms = "$parms,$cics"

	if (buildUtils.isSQL(logicalFile))
		parms = "$parms,$sql"

	if (props.errPrefix)
		parms = "$parms,$errPrefixOptions"

	// add debug options
	if (props.debug)  {
		parms = "$parms,$compileDebugParms"
	}

    if (parms.startsWith(','))
		parms = parms.drop(1)

	if (props.verbose) println "PLI compiler parms for $buildFile = $parms"
	return parms
}

/*
 * createCompileCommand - creates a MVSExec command for compiling the PLI program (buildFile)
 */
def createCompileCommand(String buildFile, LogicalFile logicalFile, String member, File logFile) {
	String parms = createPLIParms(buildFile, logicalFile)
	String compiler = props.getFileProperty('pli_compiler', buildFile)

	// define the MVSExec command to compile the program
	MVSExec compile = new MVSExec().file(buildFile).pgm(compiler).parm(parms)

	// add DD statements to the compile command
	
	if (isZUnitTestCase){
		compile.dd(new DDStatement().name("SYSIN").dsn("${props.pli_testcase_srcPDS}($member)").options('shr').report(true))
	}
	else
	{
		compile.dd(new DDStatement().name("SYSIN").dsn("${props.pli_srcPDS}($member)").options('shr').report(true))
	}
	compile.dd(new DDStatement().name("SYSPRINT").options(props.pli_listOptions))
	compile.dd(new DDStatement().name("SYSMDECK").options(props.pli_tempOptions))
	(1..17).toList().each { num ->
		compile.dd(new DDStatement().name("SYSUT$num").options(props.pli_tempOptions))
	}

	// Write SYSLIN to temporary dataset if performing link edit
	String doLinkEdit = props.getFileProperty('pli_linkEdit', buildFile)
	String linkEditStream = props.getFileProperty('pli_linkEditStream', buildFile)
	if (linkEditStream == null && doLinkEdit && doLinkEdit.toBoolean())
		compile.dd(new DDStatement().name("SYSLIN").dsn("&&TEMPOBJ").options(props.pli_tempOptions).pass(true))
	else
		compile.dd(new DDStatement().name("SYSLIN").dsn("${props.pli_objPDS}($member)").options('shr').output(true))

	// add a syslib to the compile command with optional bms output copybook and CICS concatenation
	compile.dd(new DDStatement().name("SYSLIB").dsn(props.pli_incPDS).options("shr"))
	// adding bms copybook libraries only when it exists
	if (props.bms_cpyPDS && ZFile.dsExists("'${props.bms_cpyPDS}'"))
		compile.dd(new DDStatement().dsn(props.bms_cpyPDS).options("shr"))
	if(props.team)
		compile.dd(new DDStatement().dsn(props.pli_BMS_PDS).options("shr"))
	
	// add additional datasets with dependencies based on the dependenciesDatasetMapping
	PropertyMappings dsMapping = new PropertyMappings('pli_dependenciesDatasetMapping')
	dsMapping.getValues().each { targetDataset ->
		// exclude the defaults cobol_cpyPDS and any overwrite in the alternativeLibraryNameMap
		if (targetDataset != 'pli_incPDS')
			compile.dd(new DDStatement().dsn(props.getProperty(targetDataset)).options("shr"))
	}

	// add custom concatenation
	def compileSyslibConcatenation = props.getFileProperty('pli_compileSyslibConcatenation', buildFile) ?: ""
	if (compileSyslibConcatenation) {
		def String[] syslibDatasets = compileSyslibConcatenation.split(',');
		for (String syslibDataset : syslibDatasets )
		compile.dd(new DDStatement().dsn(syslibDataset).options("shr"))
	}
	
	if (buildUtils.isCICS(logicalFile))
		compile.dd(new DDStatement().dsn(props.SDFHCOB).options("shr"))
	
	// add additional zunit libraries
	if (isZUnitTestCase)
		compile.dd(new DDStatement().dsn(props.SBZUSAMP).options("shr"))
	
	// add a tasklib to the compile command with optional CICS, DB2, and IDz concatenations
	String compilerVer = props.getFileProperty('pli_compilerVersion', buildFile)
	compile.dd(new DDStatement().name("TASKLIB").dsn(props."IBMZPLI_$compilerVer").options("shr"))
	if (buildUtils.isCICS(logicalFile))
		compile.dd(new DDStatement().dsn(props.SDFHLOAD).options("shr"))
	if (buildUtils.isSQL(logicalFile))
		compile.dd(new DDStatement().dsn(props.SDSNLOAD).options("shr"))
	if (props.SFELLOAD)
		compile.dd(new DDStatement().dsn(props.SFELLOAD).options("shr"))

	// add optional DBRMLIB if build file contains DB2 code
	if (buildUtils.isSQL(logicalFile))
		compile.dd(new DDStatement().name("DBRMLIB").dsn("$props.pli_dbrmPDS($member)").options('shr').output(true).deployType('DBRM'))

	// adding alternate library definitions
	if (props.cobol_dependenciesAlternativeLibraryNameMapping) {
		alternateLibraryNameAllocations = evaluate(props.pli_dependenciesAlternativeLibraryNameMapping)
		alternateLibraryNameAllocations.each { libraryName, datasetDSN ->
			datasetDSN = props.getProperty(datasetDSN)
			if (datasetDSN) compile.dd(new DDStatement().name(libraryName).dsn(datasetDSN).options("shr"))
		}
	}
		
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
 * createLinkEditCommand - creates a MVSExec xommand for link editing the PLI object module produced by the compile
 */
def createLinkEditCommand(String buildFile, LogicalFile logicalFile, String member, File logFile) {
	String parms = props.getFileProperty('pli_linkEditParms', buildFile)
	String linker = props.getFileProperty('pli_linkEditor', buildFile)
	String linkEditStream = props.getFileProperty('pli_linkEditStream', buildFile)

	// obtain githash for buildfile
	String pli_storeSSI = props.getFileProperty('pli_storeSSI', buildFile)
	if (pli_storeSSI && pli_storeSSI.toBoolean() && (props.mergeBuild || props.impactBuild || props.fullBuild)) {
		String ssi = buildUtils.getShortGitHash(buildFile)
		if (ssi != null) parms = parms + ",SSI=$ssi"
	}
	
	// Create the link stream if needed
	if ( linkEditStream != null ) {
		def langQualifier = "linkedit"
		buildUtils.createLanguageDatasets(langQualifier)
		def lnkFile = new File("${props.buildOutDir}/linkCard.lnk")
		if (lnkFile.exists())
			lnkFile.delete()

		lnkFile << "  " + linkEditStream.replace("\\n","\n").replace('@{member}',member)
		if (props.verbose)
			println("Copying ${props.buildOutDir}/linkCard.lnk to ${props.linkedit_srcPDS}($member)")
		new CopyToPDS().file(lnkFile).dataset(props.linkedit_srcPDS).member(member).execute()

	}

	MVSExec linkedit = new MVSExec().file(buildFile).pgm(linker).parm(parms)

	// add DD statements to the linkedit command
	String deployType = buildUtils.getDeployType("pli", buildFile, logicalFile)
	if(isZUnitTestCase){
		linkedit.dd(new DDStatement().name("SYSLMOD").dsn("${props.pli_testcase_loadPDS}($member)").options('shr').output(true).deployType('ZUNIT-TESTCASE'))
	}
	else {
		linkedit.dd(new DDStatement().name("SYSLMOD").dsn("${props.pli_loadPDS}($member)").options('shr').output(true).deployType(deployType))
	}
	linkedit.dd(new DDStatement().name("SYSPRINT").options(props.pli_tempOptions))
	linkedit.dd(new DDStatement().name("SYSUT1").options(props.pli_tempOptions))

	// add the link source code
	if ( linkEditStream != null ) {
		linkedit.dd(new DDStatement().name("SYSLIN").dsn("${props.linkedit_srcPDS}($member)").options("shr"))
	}

	// add RESLIB
	if ( props.RESLIB )
		linkedit.dd(new DDStatement().name("RESLIB").dsn(props.RESLIB).options("shr"))

	// add a syslib to the compile command with optional CICS concatenation
	linkedit.dd(new DDStatement().name("SYSLIB").dsn(props.pli_objPDS).options("shr"))
	// add custom concatenation
	def linkEditSyslibConcatenation = props.getFileProperty('pli_linkEditSyslibConcatenation', buildFile) ?: ""
	if (linkEditSyslibConcatenation) {
		def String[] syslibDatasets = syslibConcatenation.split(',');
		for (String syslibDataset : syslibDatasets )
		linkedit.dd(new DDStatement().dsn(syslibDataset).options("shr"))
	}
	linkedit.dd(new DDStatement().dsn(props.SCEELKED).options("shr"))
	if (buildUtils.isCICS(logicalFile))
		linkedit.dd(new DDStatement().dsn(props.SDFHLOAD).options("shr"))

	if (buildUtils.isSQL(logicalFile))
		linkedit.dd(new DDStatement().dsn(props.SDSNLOAD).options("shr"))

	// add dummy SYSDEFSD to avoid IEW2689W 4C40 DEFINITION SIDE FILE IS NOT DEFINED message from program binder
	if (isZUnitTestCase)
		linkedit.dd(new DDStatement().name("SYSDEFSD").options("DUMMY"))

	// add a copy command to the linkedit command to append the SYSPRINT from the temporary dataset to the HFS log file
	linkedit.copy(new CopyToHFS().ddName("SYSPRINT").file(logFile).hfsEncoding(props.logEncoding).append(true))
		
	return linkedit
}


def getRepositoryClient() {
	if (!repositoryClient && props."dbb.RepositoryClient.url")
		repositoryClient = new RepositoryClient().forceSSLTrusted(true)

	return repositoryClient
}

boolean buildListContainsTests(List<String> buildList) {
	boolean containsZUnitTestCase = buildList.find { buildFile -> props.getFileProperty('pli_testcase', buildFile).equals('true')}
	return containsZUnitTestCase ? true : false
}
