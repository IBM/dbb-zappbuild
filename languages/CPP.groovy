@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.metadata.*
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
@Field def bindUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BindUtilities.groovy"))

println("** Building files mapped to ${this.class.getName()}.groovy script")

// verify required build properties
buildUtils.assertBuildProperties(props.cpp_requiredBuildProperties)

// create language datasets
def langQualifier = "cpp"
buildUtils.createLanguageDatasets(langQualifier)

// sort the build list based on build file rank if provided
List<String> sortedList = buildUtils.sortBuildList(argMap.buildList, 'cpp_fileBuildRank')
int currentBuildFileNumber = 1

// iterate through build list
sortedList.each { buildFile ->
	println "*** (${currentBuildFileNumber++}/${sortedList.size()}) Building file $buildFile"

    // configure dependency resolution and create logical file
    String dependencySearch = props.getFileProperty('cpp_dependencySearch', buildFile)
    SearchPathDependencyResolver dependencyResolver = new SearchPathDependencyResolver(dependencySearch)

    // copy build file and dependency files to data sets
    buildUtils.copySourceFiles(buildFile, props.cpp_srcPDS, 'cpp_dependenciesDatasetMapping', null, 'cpp_dependenciesCopyMode', dependencyResolver)

    // Get logical file
    LogicalFile logicalFile = buildUtils.createLogicalFile(dependencyResolver, buildFile)

	// print logicalFile details and overrides
	if (props.verbose) buildUtils.printLogicalFileAttributes(logicalFile)
	
    // create mvs commands
    String member = CopyToPDS.createMemberName(buildFile)
    File logFile = new File( props.userBuild ? "${props.buildOutDir}/${member}.log" : "${props.buildOutDir}/${member}.c.log")
    if (logFile.exists())
        logFile.delete()
    
	String needsLinking = props.getFileProperty('cpp_linkEdit', buildFile)
			
	MVSExec compile = createCompileCommand(buildFile, logicalFile, member, logFile)
    MVSExec linkEdit
	if (needsLinking.toBoolean()) linkEdit = createLinkEditCommand(buildFile, logicalFile, member, logFile)

    // execute mvs commands in a mvs job
    MVSJob job = new MVSJob()
    job.start()

    // compile the c program
    int rc = compile.execute()
    int maxRC = props.getFileProperty('cpp_compileMaxRC', buildFile).toInteger()

    boolean bindFlag = true

    if (rc > maxRC) {
        bindFlag = false
        String errorMsg = "*! The compile return code ($rc) for $buildFile exceeded the maximum return code allowed ($maxRC)"
        println(errorMsg)
        props.error = "true"
        buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile])
    }
    else { // if this program needs to be link edited . . .

		// Store db2 bind information as a generic property record in the BuildReport
		String generateDb2BindInfoRecord = props.getFileProperty('generateDb2BindInfoRecord', buildFile)
		if (buildUtils.isSQL(logicalFile) && generateDb2BindInfoRecord.toBoolean() ){
			PropertiesRecord db2BindInfoRecord = buildUtils.generateDb2InfoRecord(buildFile)
			BuildReportFactory.getBuildReport().addRecord(db2BindInfoRecord)
		}
		
		if (needsLinking.toBoolean()) {
            rc = linkEdit.execute()
            maxRC = props.getFileProperty('cpp_linkEditMaxRC', buildFile).toInteger()

            if (rc > maxRC) {
                bindFlag = false
                String errorMsg = "*! The link edit return code ($rc) for $buildFile exceeded the maximum return code allowed ($maxRC)"
                println(errorMsg)
                props.error = "true"
                buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile])
            }
            else {
                if(!props.userBuild){
                    // only scan the load module if load module scanning turned on for file
                    String scanLoadModule = props.getFileProperty('cpp_scanLoadModule', buildFile)
                    if (scanLoadModule && scanLoadModule.toBoolean())
                        impactUtils.saveStaticLinkDependencies(buildFile, props.linkedit_loadPDS, logicalFile)
                }
            }
        }
    }
	
	//perform Db2 binds on userbuild
	if (rc <= maxRC && buildUtils.isSQL(logicalFile) && props.userBuild) {

		//perform Db2 Bind Pkg
		bind_performBindPackage = props.getFileProperty('bind_performBindPackage', buildFile)
		if ( bind_performBindPackage && bind_performBindPackage.toBoolean()) {
			int bindMaxRC = props.getFileProperty('bind_maxRC', buildFile).toInteger()
			def (bindRc, bindLogFile) = bindUtils.bindPackage(buildFile, props.cpp_dbrmPDS);
			if ( bindRc > bindMaxRC) {
				String errorMsg = "*! The bind package return code ($bindRc) for $buildFile exceeded the maximum return code allowed ($props.bind_maxRC)"
				println(errorMsg)
				props.error = "true"
				buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}_bind_pkg.log":bindLogFile])
			}
		}

		//perform Db2 Bind Plan
		bind_performBindPlan = props.getFileProperty('bind_performBindPlan', buildFile)
		if (bind_performBindPlan && bind_performBindPlan.toBoolean()) {
			int bindMaxRC = props.getFileProperty('bind_maxRC', buildFile).toInteger()
			def (bindRc, bindLogFile) = bindUtils.bindPlan(buildFile);
			if ( bindRc > bindMaxRC) {
				String errorMsg = "*! The bind plan return code ($bindRc) for $buildFile exceeded the maximum return code allowed ($props.bind_maxRC)"
				println(errorMsg)
				props.error = "true"
				buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}_bind_plan.log":bindLogFile])
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
 * createCParms - Builds up the c compiler parameter list from build and file properties
 */
def createCParms(String buildFile, LogicalFile logicalFile) {
    def parms = props.getFileProperty('cpp_compileParms', buildFile) ?: ""
    def cics = props.getFileProperty('cpp_compileCICSParms', buildFile) ?: ""
    def sql = props.getFileProperty('cpp_compileSQLParms', buildFile) ?: ""
    def compileDebugParms = props.getFileProperty('cpp_compileDebugParms', buildFile)

    if (buildUtils.isCICS(logicalFile))
        parms = "$parms,$cics"

    if (buildUtils.isSQL(logicalFile))
        parms = "$parms,$sql"

    // add debug options
    if (props.debug)  {
        parms = "$parms,$compileDebugParms"
    }

    if (parms.startsWith(','))
        parms = parms.drop(1)

    if (props.verbose) println " C/CPP compiler parms for $buildFile = $parms"
    return parms
}

/*
 * createCompileCommand - creates a MVSExec command for compiling the c program (buildFile)
 */
def createCompileCommand(String buildFile, LogicalFile logicalFile, String member, File logFile) {
    String parms = createCParms(buildFile, logicalFile)
    String compiler = props.getFileProperty('cpp_compiler', buildFile)

    // define the MVSExec command to compile the program
    MVSExec compile = new MVSExec().file(buildFile).pgm(compiler).parm(parms)

    // add DD statements to the compile command
    compile.dd(new DDStatement().name("SYSIN").dsn("${props.cpp_srcPDS}($member)").options('shr').report(true))

    compile.dd(new DDStatement().name("SYSOUT").options(props.cpp_tempOptions))
    compile.dd(new DDStatement().name("SYSPRINT").options(props.cpp_tempListOptions))
	compile.dd(new DDStatement().name("SYSCPRT").options(props.cpp_tempListOptions))
	compile.dd(new DDStatement().name("SYSMSGS").options(props.cpp_tempListOptions))
	
    (1..17).toList().each { num ->
        compile.dd(new DDStatement().name("SYSUT$num").options("cyl space(1,1) unit(sysallda) new"))
    }
	
	// define object dataset allocation
	compile.dd(new DDStatement().name("SYSLIN").dsn("${props.cpp_objPDS}($member)").options('shr').output(true).deployType("OBJ"))

    // add a syslib to the compile command
    compile.dd(new DDStatement().name("SYSLIB").dsn(props.cpp_headerPDS).options("shr"))

    // add custom concatenation
    def compileSyslibConcatenation = props.getFileProperty('cpp_compileSyslibConcatenation', buildFile) ?: ""
    if (compileSyslibConcatenation) {
        def String[] syslibDatasets = compileSyslibConcatenation.split(',');
		for (String syslibDataset : syslibDatasets )
		compile.dd(new DDStatement().dsn(syslibDataset).options("shr"))
    }
	
	if (props.SCEEH)
		compile.dd(new DDStatement().dsn(props.SCEEH).options("shr"))

	// add ASMLIB concatenation for C programs using the ASM option
	def asmSyslibConcatenation = props.getFileProperty('cpp_assemblySyslibConcatenation', buildFile) ?: ""
	if (asmSyslibConcatenation) {
		def firstASMLIB = asmSyslibConcatenation.tokenize(",")[0]
		cpp.dd(new DDStatement().name("ASMLIB").dsn(firstASMLIB).options("shr"))
		def String[] syslibDatasets = asmSyslibConcatenation.split(',');
		for (String syslibDataset : syslibDatasets )
			cpp.dd(new DDStatement().dsn(syslibDataset).options("shr"))
	}

	// add subsystem libraries
	if (buildUtils.isCICS(logicalFile))
		compile.dd(new DDStatement().dsn(props.SDFHC370).options("shr"))

	if (buildUtils.isMQ(logicalFile))
		compile.dd(new DDStatement().dsn(props.SCSQCPPS).options("shr"))
	
	// add optional DBRMLIB if build file contains DB2 code
	if (buildUtils.isSQL(logicalFile))
		compile.dd(new DDStatement().name("DBRMLIB").dsn("$props.cpp_dbrmPDS($member)").options('shr').output(true).deployType('DBRM'))

	//  add a tasklib to the compile command with optional CICS, DB2
	compile.dd(new DDStatement().name("TASKLIB").dsn(props.SCCNCMP).options("shr"))
	if (buildUtils.isCICS(logicalFile))
		compile.dd(new DDStatement().dsn(props.SDFHLOAD).options("shr"))
	if (buildUtils.isSQL(logicalFile)) {
		if (props.SDSNEXIT) compile.dd(new DDStatement().dsn(props.SDSNEXIT).options("shr"))
		compile.dd(new DDStatement().dsn(props.SDSNLOAD).options("shr"))
	}
		
    // add a copy command to the compile command to copy the SYSPRINT from the temporary dataset to an HFS log file
    compile.copy(new CopyToHFS().ddName("SYSOUT").file(logFile).hfsEncoding(props.logEncoding))
	compile.copy(new CopyToHFS().ddName("SYSCPRT").file(logFile).hfsEncoding(props.logEncoding).append(true))
	compile.copy(new CopyToHFS().ddName("SYSMSGS").file(logFile).hfsEncoding(props.logEncoding).append(true))
	

    return compile
}


/*
 * createLinkEditCommand - creates a MVSExec xommand for link editing the C/CPP object module produced by the compile
 */
def createLinkEditCommand(String buildFile, LogicalFile logicalFile, String member, File logFile) {
	String parms = props.getFileProperty('cpp_linkEditParms', buildFile)
	String linker = props.getFileProperty('cpp_linkEditor', buildFile)
	String linkEditStream = props.getFileProperty('cpp_linkEditStream', buildFile)

	// obtain githash for buildfile
	String cpp_storeSSI = props.getFileProperty('cpp_storeSSI', buildFile)
	if (cpp_storeSSI && cpp_storeSSI.toBoolean() && (props.mergeBuild || props.impactBuild || props.fullBuild)) {
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
	def identifyLoad = props.getFileProperty('cpp_identifyLoad', buildFile)
	
	if (identifyLoad && identifyLoad.toBoolean()) {
		String identifyStatement = buildUtils.generateIdentifyStatement(buildFile, props.cpp_loadOptions)
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
		linkedit.dd(new DDStatement().name("SYSIN").instreamData(sysin_linkEditInstream).options(props.global_instreamDataTempAllocation))
	}

	// add SYSLIN along the reference to SYSIN if configured through sysin_linkEditInstream
	linkedit.dd(new DDStatement().name("SYSLIN").dsn("${props.cpp_objPDS}($member)").options('shr'))
	if (sysin_linkEditInstream) linkedit.dd(new DDStatement().ddref("SYSIN"))
			
	// add DD statements to the linkedit command
	String deployType = buildUtils.getDeployType("cpp", buildFile, logicalFile)
	linkedit.dd(new DDStatement().name("SYSLMOD").dsn("${props.cpp_loadPDS}($member)").options('shr').output(true).deployType(deployType))

	linkedit.dd(new DDStatement().name("SYSPRINT").options(props.cpp_printTempOptions))
	linkedit.dd(new DDStatement().name("SYSUT1").options(props.cpp_tempOptions))

	// add RESLIB if needed
	if ( props.RESLIB ) {
		linkedit.dd(new DDStatement().name("RESLIB").dsn(props.RESLIB).options("shr"))
	}

	// add a syslib to the compile command with optional CICS concatenation
	linkedit.dd(new DDStatement().name("SYSLIB").dsn(props.cpp_objPDS).options("shr"))
	
	// add custom concatenation
	def linkEditSyslibConcatenation = props.getFileProperty('cpp_linkEditSyslibConcatenation', buildFile) ?: ""
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