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
@Field def resolverUtils = loadScript(new File("${props.zAppBuildDir}/utilities/ResolverUtilities.groovy"))

println("** Building files mapped to ${this.class.getName()}.groovy script")

// verify required build properties
buildUtils.assertBuildProperties(props.cc_requiredBuildProperties)

// create language datasets
def langQualifier = "cc"
buildUtils.createLanguageDatasets(langQualifier)

// sort the build list based on build file rank if provided
List<String> sortedList = buildUtils.sortBuildList(argMap.buildList, 'cc_fileBuildRank')

if (buildListContainsTests(sortedList)) {
    langQualifier = "cc_test"
    buildUtils.createLanguageDatasets(langQualifier)
}

// iterate through build list
sortedList.each { buildFile ->
    println "*** Building file $buildFile"

    // Check if this a testcase
    isZUnitTestCase = (props.getFileProperty('cc_testcase', buildFile).equals('true')) ? true : false

    // configure dependency resolution and create logical file
    String dependencySearch = props.getFileProperty('cc_dependencySearch', buildFile)
    def dependencyResolver = resolverUtils.createSearchPathDependencyResolver(dependencySearch)

    // copy build file and dependency files to data sets
    if(isZUnitTestCase){
        buildUtils.copySourceFiles(buildFile, props.cc_testcase_srcPDS, null, null, null)
    }else{
        buildUtils.copySourceFiles(buildFile, props.cc_srcPDS, 'cc_dependenciesDatasetMapping', props.cc_dependenciesAlternativeLibraryNameMapping, dependencyResolver)
    }


    // Get logical file
    LogicalFile logicalFile = resolverUtils.createLogicalFile(dependencyResolver, buildFile)

    // create mvs commands
    String member = CopyToPDS.createMemberName(buildFile)
    File logFile = new File( props.userBuild ? "${props.buildOutDir}/${member}.log" : "${props.buildOutDir}/${member}.c.log")
    if (logFile.exists())
        logFile.delete()
    MVSExec compile = createCompileCommand(buildFile, logicalFile, member, logFile)
    MVSExec linkEdit = createLinkEditCommand(buildFile, logicalFile, member, logFile)

    // execute mvs commands in a mvs job
    MVSJob job = new MVSJob()
    job.start()

    // compile the c program
    int rc = compile.execute()
    int maxRC = props.getFileProperty('cc_compileMaxRC', buildFile).toInteger()

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

        String needsLinking = props.getFileProperty('cc_linkEdit', buildFile)
        if (needsLinking.toBoolean()) {
            rc = linkEdit.execute()
            maxRC = props.getFileProperty('cc_linkEditMaxRC', buildFile).toInteger()

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
                    String scanLoadModule = props.getFileProperty('cc_scanLoadModule', buildFile)
                    if (scanLoadModule && scanLoadModule.toBoolean())
                        impactUtils.saveStaticLinkDependencies(buildFile, props.linkedit_loadPDS, logicalFile)
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
 * createCParms - Builds up the c compiler parameter list from build and file properties
 */
def createCParms(String buildFile, LogicalFile logicalFile) {
    def parms = props.getFileProperty('cc_compileParms', buildFile) ?: ""
    def cics = props.getFileProperty('cc_compileCICSParms', buildFile) ?: ""
    def sql = props.getFileProperty('cc_compileSQLParms', buildFile) ?: ""
    def errPrefixOptions = props.getFileProperty('cc_compileErrorPrefixParms', buildFile) ?: ""
    def compileDebugParms = props.getFileProperty('cc_compileDebugParms', buildFile)

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

    if (props.verbose) println "c compiler parms for $buildFile = $parms"
    return parms
}

/*
 * createCompileCommand - creates a MVSExec command for compiling the c program (buildFile)
 */
def createCompileCommand(String buildFile, LogicalFile logicalFile, String member, File logFile) {
    String parms = createCParms(buildFile, logicalFile)
    String compiler = props.getFileProperty('cc_compiler', buildFile)

    // define the MVSExec command to compile the program
    MVSExec compile = new MVSExec().file(buildFile).pgm(compiler).parm(parms)

    // add DD statements to the compile command
    if (isZUnitTestCase){
        compile.dd(new DDStatement().name("SYSIN").dsn("${props.cc_testcase_srcPDS}($member)").options('shr').report(true))
    }
    else
    {
        compile.dd(new DDStatement().name("SYSIN").dsn("${props.cc_srcPDS}($member)").options('shr').report(true))
    }

    compile.dd(new DDStatement().name("SYSOUT").options(props.cc_tempOptions))
    compile.dd(new DDStatement().name("SYSPRINT").options(props.cc_tempOptions))

    compile.dd(new DDStatement().name("SYSMDECK").options(props.cc_tempOptions))
    (1..17).toList().each { num ->
        compile.dd(new DDStatement().name("SYSUT$num").options(props.cc_tempOptions))
    }

    // Write SYSLIN to temporary dataset if performing link edit or to physical dataset
    String doLinkEdit = props.getFileProperty('cc_linkEdit', buildFile)
    String linkEditStream = props.getFileProperty('cc_linkEditStream', buildFile)
    String linkDebugExit = props.getFileProperty('cc_linkDebugExit', buildFile)

    if (props.debug && linkDebugExit && doLinkEdit.toBoolean()){
        compile.dd(new DDStatement().name("SYSLIN").dsn("${props.cc_objPDS}($member)").options('shr').output(true))
    } else if (doLinkEdit && doLinkEdit.toBoolean() && ( !linkEditStream || linkEditStream.isEmpty())) {
        compile.dd(new DDStatement().name("SYSLIN").dsn("&&TEMPOBJ").options(props.cc_tempOptions).pass(true))
    } else {
        compile.dd(new DDStatement().name("SYSLIN").dsn("${props.cc_objPDS}($member)").options('shr').output(true))
    }

    // add a syslib to the compile command
    compile.dd(new DDStatement().name("SYSLIB").dsn(props.cc_incPDS).options("shr"))
    compile.dd(new DDStatement().dsn(props.SCEEH).options("shr"))
    compile.dd(new DDStatement().dsn(props.SCEEHS).options("shr"))
    compile.dd(new DDStatement().name("TASKLIB").dsn("${props.SCEERUN2}").options('shr'))
    compile.dd(new DDStatement().dsn("${props.SCCNCMP}").options('shr'))

    // add additional datasets with dependencies based on the dependenciesDatasetMapping
    PropertyMappings dsMapping = new PropertyMappings('cc_dependenciesDatasetMapping')
    dsMapping.getValues().each { targetDataset ->
        // exclude the defaults cc_cpyPDS and any overwrite in the alternativeLibraryNameMap
        if (targetDataset != 'cc_incPDS')
            compile.dd(new DDStatement().dsn(props.getProperty(targetDataset)).options("shr"))
    }

    // add custom concatenation
    def compileSyslibConcatenation = props.getFileProperty('cc_compileSyslibConcatenation', buildFile) ?: ""
    if (compileSyslibConcatenation) {
        def String[] syslibDatasets = compileSyslibConcatenation.split(',');
        def DDStatement statement = null
        for (String syslibDataset : syslibDatasets ) {
            if (statement == null){
                statement = new DDStatement().name("SYSLIB").dsn(syslibDataset).options("shr")
            } else {
                statement.concatenate(new DDStatement().dsn(syslibDataset).options("shr"))
            }
        }
        if (statement != null){
            compile.dd(statement)
        }
    }

    def assemblySyslibConcatenation = props.getFileProperty('cc_assemblySyslibConcatenation', buildFile) ?: ""
    if (assemblySyslibConcatenation) {
        def String[] syslibDatasets = assemblySyslibConcatenation.split(',');
        for (String syslibDataset : syslibDatasets )
            compile.dd(new DDStatement().dsn(syslibDataset).options("shr"))
    }
    if (props.SCEEH)
        compile.dd(new DDStatement().dsn(props.SCEEH).options("shr"))
    if (props.SCEEHS)
        compile.dd(new DDStatement().dsn(props.SCEEHS).options("shr"))

    // add additional zunit libraries
    if (isZUnitTestCase)
        compile.dd(new DDStatement().dsn(props.SBZUSAMP).options("shr"))

    // add a tasklib to the compile command with optional CICS, DB2, and IDz concatenations
    //String compilerVer = props.getFileProperty('cc_compilerVersion', buildFile)
    //compile.dd(new DDStatement().name("TASKLIB").dsn(props."SIGYCOMP_$compilerVer").options("shr"))

    if (buildUtils.isCICS(logicalFile))
        compile.dd(new DDStatement().dsn(props.SDFHLOAD).options("shr"))
    if (buildUtils.isSQL(logicalFile)) {
        compile.dd(new DDStatement().dsn(props.SDSNLOAD).options("shr"))
        if (props.SDSNEXIT)
            compile.dd(new DDStatement().dsn(props.SDSNEXIT).options("shr"))
    }

    if (props.SFELLOAD)
        compile.dd(new DDStatement().dsn(props.SFELLOAD).options("shr"))

    // adding alternate library definitions
    if (props.cc_dependenciesAlternativeLibraryNameMapping) {
        alternateLibraryNameAllocations = buildUtils.parseJSONStringToMap(props.cc_dependenciesAlternativeLibraryNameMapping)
        alternateLibraryNameAllocations.each { libraryName, datasetDefinition ->
            datasetName = props.getProperty(datasetDefinition)
            if (datasetName) {
                compile.dd(new DDStatement().name(libraryName).dsn(datasetName).options("shr"))
            }
            else {
                String errorMsg = "*! C.groovy. The dataset definition $datasetDefinition could not be resolved from the DBB Build properties."
                println(errorMsg)
                props.error = "true"
                buildUtils.updateBuildResult(errorMsg:errorMsg)
            }
        }
    }

    // add IDz User Build Error Feedback DDs
    if (props.errPrefix) {
        compile.dd(new DDStatement().name("SYSADATA").options("DUMMY"))
        // SYSXMLSD.XML suffix is mandatory for IDZ/ZOD to populate remote error list
        compile.dd(new DDStatement().name("SYSXMLSD").dsn("${props.hlq}.${props.errPrefix}.SYSXMLSD.XML").options(props.cc_compileErrorFeedbackXmlOptions))
    }

    // add a copy command to the compile command to copy the SYSPRINT from the temporary dataset to an HFS log file
    compile.copy(new CopyToHFS().ddName("SYSOUT").file(logFile).hfsEncoding(props.logEncoding))

    return compile
}


/*
 * createLinkEditCommand - creates a MVSExec xommand for link editing the c object module produced by the compile
 */
def createLinkEditCommand(String buildFile, LogicalFile logicalFile, String member, File logFile) {

    String parms = props.getFileProperty('cc_linkEditParms', buildFile)
    String linker = props.getFileProperty('cc_linkEditor', buildFile)
    String linkEditStream = props.getFileProperty('cc_linkEditStream', buildFile)
    String linkDebugExit = props.getFileProperty('cc_linkDebugExit', buildFile)

    // obtain githash for buildfile
    String cc_storeSSI = props.getFileProperty('cc_storeSSI', buildFile)
    if (cc_storeSSI && cc_storeSSI.toBoolean() && (props.mergeBuild || props.impactBuild || props.fullBuild)) {
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

    // define the MVSExec command to link edit the program
    MVSExec linkedit = new MVSExec().file(buildFile).pgm(linker).parm(parms)

    // Create a physical link card

    // add DD statements to the linkedit command
    String deployType = buildUtils.getDeployType("cc", buildFile, logicalFile)
    if(isZUnitTestCase){
        linkedit.dd(new DDStatement().name("SYSLMOD").dsn("${props.cc_testcase_loadPDS}($member)").options('shr').output(true).deployType('ZUNIT-TESTCASE'))
    }
    else {
        linkedit.dd(new DDStatement().name("SYSLMOD").dsn("${props.cc_loadPDS}($member)").options('shr').output(true).deployType(deployType))
    }
    linkedit.dd(new DDStatement().name("TASKLIB").dsn("${props.SCEERUN2}").options('shr'))
    linkedit.dd(new DDStatement().dsn("${props.SCEERUN}").options('shr'))
    linkedit.dd(new DDStatement().name("SYSPRINT").options(props.cc_tempOptions))
    linkedit.dd(new DDStatement().name("SYSUT1").options(props.cc_tempOptions))

    // add the link source code
    if ( linkEditStream != null ) {
        linkedit.dd(new DDStatement().name("SYSLIN").dsn("${props.linkedit_srcPDS}($member)").options("shr"))
    }

    // add RESLIB if needed
    if ( props.RESLIB ) {
        linkedit.dd(new DDStatement().name("RESLIB").dsn(props.RESLIB).options("shr"))
    }

    // add a syslib to the compile command with optional CICS concatenation
    //linkedit.dd(new DDStatement().name("SYSLIB").dsn(props.cc_objPDS).options("shr"))
    linkedit.dd(new DDStatement().name("SYSLIB").dsn(props.SCEELKEX).options("shr"))
    linkedit.dd(new DDStatement().dsn(props.SCEECPP).options("shr"))

    // add custom concatenation
    def linkEditSyslibConcatenation = props.getFileProperty('cc_linkEditSyslibConcatenation', buildFile) ?: ""
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

    if (buildUtils.isSQL(logicalFile))
        linkedit.dd(new DDStatement().dsn(props.SDSNLOAD).options("shr"))


    // add a copy command to the linkedit command to append the SYSPRINT from the temporary dataset to the HFS log file
    linkedit.copy(new CopyToHFS().ddName("SYSPRINT").file(logFile).hfsEncoding(props.logEncoding).append(true))

    return linkedit

}

boolean buildListContainsTests(List<String> buildList) {
    boolean containsZUnitTestCase = buildList.find { buildFile -> props.getFileProperty('cc_testcase', buildFile).equals('true')}
    return containsZUnitTestCase ? true : false
}
