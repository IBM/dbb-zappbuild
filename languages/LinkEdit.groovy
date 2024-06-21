@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.metadata.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*


// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def buildUtils= loadScript(new File("${props.zAppBuildDir}/utilities/BuildUtilities.groovy"))
@Field def impactUtils= loadScript(new File("${props.zAppBuildDir}/utilities/ImpactUtilities.groovy"))

println("** Building ${argMap.buildList.size()} ${argMap.buildList.size() == 1 ? 'file' : 'files'} mapped to ${this.class.getName()}.groovy script")

// verify required build properties
buildUtils.assertBuildProperties(props.linkedit_requiredBuildProperties)
if (props.createBuildMaps) assert props.linkSearch : "*! Missing required build property for build map creation 'linkSearch'" // verify required prop for build map generation


def langQualifier = "linkedit"
buildUtils.createLanguageDatasets(langQualifier)

// sort the build list based on build file rank if provided
List<String> sortedList = buildUtils.sortBuildList(argMap.buildList.sort(), 'linkedit_fileBuildRank')
int currentBuildFileNumber = 1

// iterate through build list
sortedList.each { buildFile ->
	println "*** (${currentBuildFileNumber++}/${sortedList.size()}) Building file $buildFile"

	// copy build file to input data set
	buildUtils.copySourceFiles(buildFile, props.linkedit_srcPDS, null, null, null)

	// Get logical file
	LogicalFile logicalFile = SearchPathDependencyResolver.getLogicalFile(buildFile,props.workspace)

	// create mvs commands
	String member = CopyToPDS.createMemberName(buildFile)
	
	File logFile = new File( props.userBuild ? "${props.buildOutDir}/${member}.log" : "${props.buildOutDir}/${member}.linkedit.log")
	if (logFile.exists())
		logFile.delete()
	MVSExec linkEdit = createLinkEditCommand(buildFile, logicalFile, member, logFile)

	// execute mvs commands in a mvs job
	MVSJob job = new MVSJob()
	job.start()

	boolean clean = true

	rc = linkEdit.execute()
	maxRC = props.getFileProperty('linkedit_maxRC', buildFile).toInteger()

	if (rc > maxRC) {
		clean = false
		String errorMsg = "*! The link edit return code ($rc) for $buildFile exceeded the maximum return code allowed ($maxRC)"
		println(errorMsg)
		buildUtils.updateBuildResult(errorMsg:errorMsg,logs:["${member}.log":logFile])
	}
	else { // success
		if(!props.userBuild){
			// only scan the load module if load module scanning turned on for file
			String scanLoadModule = props.getFileProperty('linkedit_scanLoadModule', buildFile)
			if (scanLoadModule && scanLoadModule.toBoolean())
				impactUtils.saveStaticLinkDependencies(buildFile, props.linkedit_loadPDS, logicalFile)
		}
	}

	job.stop()

	if (clean) { // success
		if (props.createBuildMaps) {
			// create build map for each build file upon success
			BuildGroup group = MetadataStoreFactory.getMetadataStore().getBuildGroup(props.applicationBuildGroup)
			if (group.buildMapExists(buildFile)) {
				if (props.verbose) println("* Replacing existing build map for $buildFile")
				group.deleteBuildMap(buildFile)
			}

			BuildMap buildMap = group.createBuildMap(buildFile) // build map creation
			// populate outputs with IExecutes
			List<IExecute> execs = new ArrayList<IExecute>()
			if (linkEdit) execs.add(linkEdit)
			buildMap.populateOutputs(execs)
			// populate inputs using dependency resolution
			buildMap.populateInputsFromGit(props.workspace, props.bmsSearch)
			// populate binary inputs from load module scanning
			if (linkEdit) { 
				String scanLoadModule = props.getFileProperty('linkedit_scanLoadModule', buildFile)
				if (scanLoadModule && scanLoadModule.toBoolean())
					buildMap.populateBinaryInputsFromGit(props.cobol_loadPDS, member)
			}
		}
	}
	else { // error
		props.error = "true"
	}
}

// end script


//********************************************************************
//* Method definitions
//********************************************************************

/*
 * createLinkEditCommand - creates a MVSExec xommand for link editing the object module produced by link file
 */
def createLinkEditCommand(String buildFile, LogicalFile logicalFile, String member, File logFile) {
	String parms = props.getFileProperty('linkEdit_parms', buildFile)
	String linker = props.getFileProperty('linkedit_linkEditor', buildFile)

	// obtain githash for buildfile
	String linkedit_storeSSI = props.getFileProperty('linkedit_storeSSI', buildFile) 
	if (linkedit_storeSSI && linkedit_storeSSI.toBoolean() && (props.mergeBuild || props.impactBuild || props.fullBuild)) {
		String ssi = buildUtils.getShortGitHash(buildFile)
		if (ssi != null) parms = parms + ",SSI=$ssi"
	}
	
	if (props.verbose) println "Link-Edit parms for $buildFile = $parms"
	
	// define the MVSExec command to link edit the program
	MVSExec linkedit = new MVSExec().file(buildFile).pgm(linker).parm(parms)

	// add DD statements to the linkedit command
	// deployType requires a file level overwrite to define isCICS and isDLI, while the linkcard does not carry isCICS, isDLI attributes
	String deployType = buildUtils.getDeployType("linkedit", buildFile, logicalFile)
	linkedit.dd(new DDStatement().name("SYSLIN").dsn("${props.linkedit_srcPDS}($member)").options("shr").report(true))
	linkedit.dd(new DDStatement().name("SYSLMOD").dsn("${props.linkedit_loadPDS}($member)").options('shr').output(true).deployType(deployType))
	linkedit.dd(new DDStatement().name("SYSPRINT").options(props.linkedit_tempOptions))
	linkedit.dd(new DDStatement().name("SYSUT1").options(props.linkedit_tempOptions))

	// add a syslib to the compile command with optional CICS concatenation
	linkedit.dd(new DDStatement().name("SYSLIB").dsn(props.linkedit_objPDS).options("shr"))
	// add custom concatenation
	def linkEditSyslibConcatenation = props.getFileProperty('linkedit_linkEditSyslibConcatenation', buildFile) ?: ""
	if (linkEditSyslibConcatenation) {
		def String[] syslibDatasets = linkEditSyslibConcatenation.split(',');
		for (String syslibDataset : syslibDatasets )
		linkedit.dd(new DDStatement().dsn(syslibDataset).options("shr"))
	}
	linkedit.dd(new DDStatement().dsn(props.SCEELKED).options("shr"))
	
	if (props.debug && props.SEQAMOD)
		linkedit.dd(new DDStatement().dsn(props.SEQAMOD).options("shr"))
		
	if (buildUtils.isCICS(logicalFile))
		linkedit.dd(new DDStatement().dsn(props.SDFHLOAD).options("shr"))
		
	if (buildUtils.isIMS(logicalFile))
		linkedit.dd(new DDStatement().dsn(props.SDFSRESL).options("shr"))
			
	if (props.SDSNLOAD)
		linkedit.dd(new DDStatement().dsn(props.SDSNLOAD).options("shr"))

	if (props.SCSQLOAD)
		linkedit.dd(new DDStatement().dsn(props.SCSQLOAD).options("shr"))
		
	// add a copy command to the linkedit command to append the SYSPRINT from the temporary dataset to the HFS log file
	linkedit.copy(new CopyToHFS().ddName("SYSPRINT").file(logFile).hfsEncoding(props.logEncoding))

	return linkedit
}




