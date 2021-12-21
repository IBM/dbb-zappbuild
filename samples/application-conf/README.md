# Application Configuration
This folder contains application specific configuration properties used by the zAppBuild Groovy build and utility scripts. It is intended to be copied as a high level folder in the application repository or main application repository if the application source files are distributed across multiple repositories. Once copied to the application repository, users should review the default property files and modify any values as needed.

At the beginning of the build, the `application-conf/application.properties` file will automatically be loaded into the [DBB BuildProperties class](https://www.ibm.com/support/knowledgecenter/SS6T76_1.0.4/scriptorg.html#build-properties-class). Use the `applicationPropFiles` property (see table below) to load additional application property files.

Properties can be overwritten on a per file basis through DBB Build Properties file properties. The tables below indicate which properties keys can be overwritten. It is recommended to manage these overwrites in file.properties.

## Property File Descriptions
Since all properties will be loaded into a single static instance of BuildProperties, the organization and naming convention of the *property files* are somewhat arbitrary and targeted more for self documentation and understanding.

### application.properties
This property file is loaded automatically at the beginning of the build and contains application specific properties used mainly by `build.groovy` but can also be a place to declare properties used by multiple language scripts. Additional property files are loaded based on the content of the `applicationPropFiles` property.

Property | Description | Overridable
--- | --- | ---
runzTests | Boolean value to specify if zUnit tests should be run.  Defaults to `false`, to enable zUnit Tests, set value to `true`. | false
applicationPropFiles | Comma separated list of additional application property files to load. Supports both absolute and relative file paths.  Relative paths assumed to be relative to ${workspace}/${application}/application-conf/. | false
applicationSrcDirs | Comma separated list of all source directories included in application build. Each directory is assumed to be a local Git repository clone. Supports both absolute and relative paths though for maximum reuse of collected dependency data relative paths should be used.  Relative paths assumed to be relative to ${workspace}. | false
buildOrder | Comma separated list of the build script processing order. | false
mainBuildBranch | The main build branch of the main application repository.  Used for cloning collections for topic branch builds instead of rescanning the entire application. | false
gitRepositoryURL | git repository URL of the application repository to establish links to the changed files in the build result properties | false
excludeFileList | Files to exclude when scanning or running full build. | false
skipImpactCalculationList | Files for which the impact analysis should be skipped in impact build | false
jobCard | JOBCARD for JCL execs | false
impactResolutionRules | Comma separated list of resolution rule properties used for impact builds.  Sample resolution rule properties (in JSON format) are included below. | true, recommended in file.properties

### file.properties
Location of file properties, script mappings and file level property overrides.  All file properties for the entire application, including source files in distributed repositories of the application need to be contained either in this file or in other property files in the `application-conf` directory. Look for column 'Overridable' in the tables below for build properties that can have file level property overrides.

Property | Description
--- | ---
dbb.scriptMapping | DBB configuration file properties association build files to language scripts
dbb.scannerMapping | DBB scanner mapping to overwrite the file scanner. File property
isSQL | File property overwrite to indicate that a file requires to include SQL parameters
isCICS | File property overwrite to indicate that a file requires to include CICS parameters
isMQ | File property overwrite to indicate that a file requires to include MQ parameters
isDLI | File property overwrite to indicate that a file requires to include DLI parameters
cobol_testcase | File property to indicate a generated zUnit cobol test case to use a different set of source and output libraries

### reports.properties
Properties used by the build framework to generate reports. Sample properties file to all application-conf to overwrite central build-conf configuration.

Property | Description 
--- | ---
reportExternalImpacts | Flag to indicate if an *impactBuild* should analyze and report external impacted files in other collections
reportExternalImpactsAnalysisDepths | Configuration of the analysis depths when performing impact analysis for external impacts (simple|deep)
reportExternalImpactsAnalysisFileFilter | Comma-separated list of pathMatcher filters to limit the analysis of external impacts to a subset of the changed files
reportExternalImpactsCollectionPatterns | Comma-separated list of regex patterns of DBB collection names for which external impacts should be documented
reportUpstreamChanges | Flag to indicate if a topic branch build creates reports of upstream changes to understand recent changes on the mainBuildBranch which don't exist on your configuration

### Assembler.properties
Application properties used by zAppBuild/language/Assembler.groovy

Property | Description | Overridable
--- | --- | ---
assembler_fileBuildRank | Default Assemble program build rank. Used to sort Assembler build file sub-list. Leave empty. | true
assembler_pgmParms | Default Assembler parameters. | true
assembler_linkEditParms | Default parameters for the link edit step. | true
assembler_compileErrorPrefixParms | Default parameters to support remote error feedback in user build scenarios | true
assembler_linkEdit | Flag indicating to execute the link edit step to produce a load module for the source file.  If false then a object deck will be created instead for later linking. | true
assembler_maxRC | Default Assembler maximum RC allowed. | true
assembler_linkEditMaxRC | Default link edit maximum RC allowed. | true
assembler_impactPropertyList | List of build properties causing programs to rebuild when changed | false
assembler_impactPropertyListCICS | List of CICS build properties causing programs to rebuild when changed | false
assembler_impactPropertyListSQL | List of SQL build properties causing programs to rebuild when changed | false
assembler_resolutionRules | Assembler dependency resolution rules used to create a Assmebler dependency resolver.  Format is a JSON array of resolution rule property keys.  Resolution rule properties are defined in `application-conf/application.properties`. | true
assembler_deployType | default deployType for build output | true
assembler_deployTypeCICS | deployType for build output for build files where isCICS=true | true
assembler_deployTypeDLI | deployType for build output for build files with isDLI=true | true
assembler_scanLoadModule | Flag indicating to scan the load module for link dependencies and store in the application's outputs collection. | true
assembler_assemblySyslibConcatenation | A comma-separated list of libraries to be concatenated in syslib during assembly step | true
assembler_linkEditSyslibConcatenation | A comma-separated list of libraries to be concatenated in syslib during linkEdit step | true

### BMS.properties
Application properties used by zAppBuild/language/BMS.groovy

Property | Description | Overridable
--- | --- | ---
bms_fileBuildRank | Default BMS program build rank. Used to sort BMS build file sub-list. Leave empty. | true
bms_maxRC | Default BMS maximum RC allowed. | true
bms_copyGenParms | Default parameters for the copybook generation step. | true
bms_compileParms | Default parameters for the compilation step. | true
bms_linkEditParms | Default parameters for the link edit step. | true
bms_impactPropertyList | List of build properties causing programs to rebuild when changed | false
bms_deployType | deployType for build output | true
bms_copy_deployType | deployType for generated copybooks | true


### Cobol.properties
Application properties used by zAppBuild/language/Cobol.groovy

Property | Description | Overridable
--- | --- | ---
cobol_fileBuildRank | Default Cobol program build rank. Used to sort Cobol build file sub-list. Leave empty. | true
cobol_resolutionRules | Cobol dependency resolution rules used to create a Cobol dependency resolver.  Format is a JSON array of resolution rule property keys.  Resolution rule properties are defined in `application-conf/application.properties`. | true
cobol_compilerVersion | Default Cobol compiler version. | true
cobol_compileMaxRC | Default compile maximum RC allowed. | true
cobol_linkEditMaxRC | Default link edit maximum RC allowed. | true
cobol_compileParms | Default base compile parameters. | true
cobol_compileCICSParms | Default CICS compile parameters. Appended to base parameters if has value.| true
cobol_compileSQLParms | Default SQL compile parameters. Appended to base parameters if has value. | true
cobol_compileErrorPrefixParms | IDz user build parameters. Appended to base parameters if has value. | true
cobol_linkEditParms | Default link edit parameters. | true
cobol_compileDebugParms | Default Debug compile parameters. Appended to base parameters if running with debug flag set. | true
cobol_impactPropertyList | List of build properties causing programs to rebuild when changed | false
cobol_impactPropertyListCICS | List of CICS build properties causing programs to rebuild when changed | false
cobol_impactPropertyListSQL | List of SQL build properties causing programs to rebuild when changed | false
cobol_linkEdit | Flag indicating to execute the link edit step to produce a load module for the source file.  If false then a object deck will be created instead for later linking. | true
cobol_isMQ | Flag indicating that the program contains MQ calls | true
cobol_deployType | default deployType for build output | true
cobol_deployTypeCICS | deployType for build output for build files where isCICS=true | true
cobol_deployTypeDLI | deployType for build output for build files with isDLI=true | true
cobol_scanLoadModule | Flag indicating to scan the load module for link dependencies and store in the application's outputs collection. | true
cobol_compileSyslibConcatenation | A comma-separated list of libraries to be concatenated in syslib during compile step | true
cobol_linkEditSyslibConcatenation | A comma-separated list of libraries to be concatenated in syslib during linkEdit step | true

### LinkEdit.properties
Application properties used by zAppBuild/language/LinkEdit.groovy

Property | Description | Overridable
--- | --- | ---
linkedit_fileBuildRank | Default link card build rank. Used to sort link card build sub-list. Leave empty. | true
linkedit_maxRC | Default link edit maximum RC allowed. | true
linkedit_parms | Default link edit parameters. | true
linkedit_impactPropertyList | List of build properties causing programs to rebuild when changed | false
linkedit_deployType | default deployType for build output | true
linkedit_deployTypeCICS | deployType for build output for build files where isCICS=true set as file property | true
linkedit_deployTypeDLI | deployType for build output for build files with isDLI=true set as file property | true
linkedit_scanLoadModule | Flag indicating to scan the load module for link dependencies and store in the application's outputs collection. | true
linkEdit_linkEditSyslibConcatenation | A comma-separated list of libraries to be concatenated in syslib during linkEdit step | true

### PLI.properties
Application properties used by zAppBuild/language/LinkEdit.groovy

Property | Description | Overridable
--- | --- | ---
pli_fileBuildRank | Default PLI program build rank. Used to sort PLI program sub-list. Leave empty. | true
pli_resolutionRules | PLI dependency resolution rules used to create a PLI dependency resolver.  Format is a JSON array of resolution rule property keys.  Resolution rule properties are defined in `application-conf/application.properties`. | true
pli_compilerVersion | Default PLI compiler version. | true
pli_compileMaxRC | Default compile maximum RC allowed. | true
pli_linkEditMaxRC | Default link edit maximum RC allowed. | true
pli_compileParms | Default base compile parameters. | true
pli_compileCICSParms | Default CICS compile parameters. Appended to base parameters if has value.| true
pli_compileSQLParms | Default SQL compile parameters. Appended to base parameters if has value. | true
pli_compileDebugParms | Default Debug compile parameters. Appended to base parameters if running with debug flag set. | true
pli_compileErrorPrefixParms | IDz user build parameters. Appended to base parameters if has value. | true
pli_impactPropertyList | List of build properties causing programs to rebuild when changed | false
pli_impactPropertyListCICS | List of CICS build properties causing programs to rebuild when changed | false
pli_impactPropertyListSQL | List of SQL build properties causing programs to rebuild when changed | false
pli_linkEditParms | Default link edit parameters. | true
pli_impactPropertyList | List of build properties causing programs to rebuild when changed | false
pli_impactPropertyListCICS | List of CICS build properties causing programs to rebuild when changed | false
pli_impactPropertyListSQL | List of SQL build properties causing programs to rebuild when changed | false
pli_linkEdit | Flag indicating to execute the link edit step to produce a load module for the source file.  If false then a object deck will be created instead for later linking. | true
pli_deployType | default deployType for build output | true
pli_deployTypeCICS | deployType for build output for build files where isCICS=true | true
pli_deployTypeDLI | deployType for build output for build files with isDLI=true | true
pli_scanLoadModule | Flag indicating to scan the load module for link dependencies and store in the application's outputs collection. | true
pli_compileSyslibConcatenation | A comma-separated list of libraries to be concatenated in syslib during compile step | true
pli_linkEditSyslibConcatenation | A comma-separated list of libraries to be concatenated in syslib during linkEdit step | true

### bind.properties
Application properties used by zAppBuild/language/COBOL.groovy

Property | Description | Overridable
--- | --- | ---
bind_performBindPackage | Default variable to perform DB2 bind as part of a DBB User Build (default value:false) | true
bind_runIspfConfDir | |
bind_db2Location | The name of the DB2 subsystem | true
bind_collectionID | The DB2 collection (Package) name | true
bind_packageOwner | The owner of the package, if left empty the use executing the command will be used | true
bind_qualifier | The value of the implicit qualifier | true
bind_maxRC | Default bind maximum RC allowed. | true

### MFS.properties
Application properties used by zAppBuild/language/MFS.groovy

Property | Description | Overridable
--- | --- | ---
mfs_fileBuildRank | Default MFS program build rank. Used to sort MFS build file sub-list. Leave empty. | true
mfs_phase1MaxRC | Default MFS Phase 1 maximum RC allowed. | true
mfs_phase2MaxRC | Default MFS Phase 2 maximum RC allowed. | true
mfs_phase1Parms | Default parameters for the phase 1 step. | true
mfs_phase2Parms | Default parameters for the phase 2 step. | true
mfs_impactPropertyList | List of build properties causing programs to rebuild when changed | false
mfs_deployType | default deployType for build output | true

### DBDgen.properties
Application properties used by zAppBuild/language/DBDgen.groovy

Property | Description | Overridable
--- | --- | ---
dbdgen_fileBuildRank | Default build program build rank. Used to sort DBDgen build file sub-list. Leave empty. | true
dbdgen_pgmParms | Default DBDgen parameters. | true
dbdgen_linkEditParms | Default parameters for the link edit step. | true
dbdgen_compileErrorPrefixParms | Default parameters to support remote error feedback in user build scenarios | true
dbdgen_assemblerMaxRC | Default link edit maximum RC allowed. | true
dbdgen_linkEditMaxRC | Default link edit maximum RC allowed. | true
dbdgen_impactPropertyList | List of build properties causing programs to rebuild when changed | false
dbdgen_deployType | default deployType for build output | true


### PSBgen.properties
Application properties used by zAppBuild/language/PSBgen.groovy

Property | Description | Overridable
--- | --- | ---
psbgen_fileBuildRank | Default build program build rank. Used to sort DBDgen build file sub-list. Leave empty. | true
psbgen_pgmParms | Default PSBgen parameters. | true
psbgen_linkEditParms | Default parameters for the link edit step. | true
psbgen_compileErrorPrefixParms | Default parameters to support remote error feedback in user build scenarios | true
psbgen_runACBgen | Parameter if ACBgen should be executed right after PSBgen (default: true) | true
psbgen_assemblerMaxRC | Default link edit maximum RC allowed. | true
psbgen_linkEditMaxRC | Default link edit maximum RC allowed. | true
psbgen_impactPropertyList | List of build properties causing programs to rebuild when changed | false
psbgen_deployType | default deployType for build output | true

### ACBgen.properties
Application properties used by zAppBuild/language/PSBgen.groovy

Property | Description | Overridable
--- | --- | ---
acbgen_pgmParms | Default ACBgen parameters. | true
acbgen_pgmMaxRC | Default ACBgen maximum RC allowed. | true
acbgen_deployType | default deployType for build output | true

### ZunitConfig.properties
Application properties used by zAppBuild/language/ZunitConfig.groovy

Property | Description | Overridable
--- | --- | ---
zunit_maxPassRC | Default zUnit maximum RC allowed for a Pass. | true
zunit_maxWarnRC | Default zUnit maximum RC allowed for a Warninig (everything beyond this value will Fail). | true
zunit_playbackFileExtension | Default zUnit Playback File Extension. | true
zunit_resolutionRules | Default resolution rules for zUnit. | true
zunit_CodeCoverageHost | Headless Code Coverage Collector host (if not specified IDz will be used for reporting) | true
zunit_CodeCoveragePort | Headless Code Coverage Collector port (if not specified IDz will be used for reporting) | true
zunit_CodeCoverageOptions | Headless Code Coverage Collector Options | true

### REXX.properties
Application properties used by zAppBuild/language/REXX.groovy

Property | Description | Overridable
--- | --- | ---
rexx_compileMaxRC | Default compile maximum RC allowed. | true
rexx_linkEditMaxRC | Default link edit maximum RC allowed. | true
rexx_resolutionRules | Default resolution rules for zUnit. | true
rexx_compileParms | Default base compile parameters. | true
rexx_compiler | Default REXX compiler | true
rexx_linkEdit | Flag indicating to execute the link edit step to produce a compiled rexx for the source file. | true
rexx_linkEditParms | Default link edit parameters. | true
rexx_deployType | default deployType | true
rexx_cexec_deployType | default deployType CEXEC | true
rexx_compileSyslibConcatenation | A comma-separated list of libraries to be concatenated in syslib during compile step | true
rexx_linkEditSyslibConcatenation | A comma-separated list of libraries to be concatenated in syslib during linkEdit step | true

### nonBuildable.properties
Application properties used by zAppBuild/language/Transfer.groovy

Property | Description | Overridable
--- | --- | ---
transfer_deployType | deployType | true
