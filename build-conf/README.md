# Build Configuration
This folder contains general build configuration properties used by the zAppBuild Groovy build and utility scripts. They are separated from the application specific configuration properties contained in `samples/application-conf` which should be copied and modified in application repositories.

***NOTE : datasets.properties (described below) must be configured for your build machine!***

At the beginning of the build, the `build-conf/build.properties` file will automatically be loaded into the [DBB BuildProperties class](https://www.ibm.com/support/knowledgecenter/SS6T76_1.0.4/scriptorg.html#build-properties-class). Use the `buildPropFiles`  property (see table below) to load additional build property files.
## Property File Descriptions
Since all properties will be loaded into a single static instance of BuildProperties, the organization and naming convention of the *property files* are somewhat arbitrary and targeted more for self documentation and understanding.

### datasets.properties
Build properties for Partition Data Sets (PDS) used by zAppBuild language build scripts. ***Must be configured for your build machine!***

Property | Description
--- | ---
MACLIB | z/OS macro library. Example: SYS1.MACLIB
SCEEMAC | Assembler macro library. Example: CEE.SCEEMAC
SCEELKED | LE (Language Environment) load library. Example: CEE.SCEELKED
SASMMOD1 | High Level Assembler (HLASM) load library. Example: ASM.SASMMOD1
SIGYCOMP_V4 | Cobol Compiler Data Set for version 4.x.x. Example: COBOL.V4R1M0.SIGYCOMP
SIGYCOMP_V6 | Cobol Compiler Data Set for version 6.x.x. Example: IGY.V6R1M0.SIGYCOMP
IBMZPLI_V52 | PLI Compiler Data Set for version 5.2. Example: PLI.V5R2M0.SIBMZCMP
IBMZPLI_V51 | PLI Compiler Data Set for version 5.1. Example: PLI.V5R1M0.SIBMZCMP
SDFHMAC | CICS Macro Library. Example: CICSTS.V3R2M0.CICS.SDFHMAC
SDFHLOAD | CICS Load Library. Example: CICSTS.V3R2M0.CICS.SDFHLOAD
SDFHCOB | CICS COBOL Library. Example: CICSTS.V3R2M0.CICS.SDFHCOB
SCSQCOBC | MQ COBOL Library. Example: CSQ.V9R1M0.SCSQCOBC
SCSQLOAD | MQ Load Library. Example: CSQ.V9R1M0.SCSQLOAD
SDSNLOAD | DB2 Load Library. Example: DB2.V9R1M0.SDSNLOAD
SFELLOAD | Optional IDz Load Library. Example: FEL.V14R0M0.SFELLOAD
SBZUSAMP | Optional z/OS Dynamic Test Runner IDz zUnit / WAZI VTP library containing necessary copybooks. Example : FEL.V14R2.SBZUSAMP

### build.properties
General properties used mainly by `build.groovy` but can also be a place to declare properties used by multiple language scripts.

Property | Description
--- | ---
buildPropFiles | Comma separated list of additional build property files to load. Supports both absolute and relative file paths.  Relative paths assumed to be relative to `zAppBuild/build-conf/`.
buildListFileExt | File extension that indicates the build file is really a build list.
languagePropertyQualifiers | List of language script property qualifiers. Each language script property has a unique qualifier to avoid collision with other language script properties.
applicationConfRootDir | Alternate root directory for application-conf location.  Allows for the deployment of the application-conf directories to a static location.  Defaults to ${workspace}/${application}
requiredDBBToolkitVersion | Minimum required DBB ToolkitVersion to run this version of zAppBuild.
requiredBuildProperties | Comma separated list of required build properties for zAppBuild/build.groovy. Build and language scripts will validate that *required* build properties have been set before the script runs.  If any are missing or empty, then a validation error will be thrown.
formatConsoleOutput | Flag to log output in table views instead of printing raw JSON data
impactBuildOnBuildPropertyChanges | Boolean property to activate impact builds on changes of build properties within the application repository
impactBuildOnBuildPropertyList | List of build property lists referencing which language properties should cause an impact build when the given property is changed 
continueOnScanFailure | Determine the behavior when facing a scanner failure. true (default) to continue scanning. false will terminate the process. 
createBuildOutputSubfolder | Option to create a subfolder with the build label within the build output dir (outDir). Default: true.
documentDeleteRecords | Option determine if the build framework should document deletions of outputs in DBB Build Report. Default: false. Requires DBB Toolkit 1.1.3 and higher.
generateDb2BindInfoRecord | Flag to control the generation of a generic DBB build record for a build file to document the configured db2 bind information (application-conf/bind.properties). Default: false ** Can be overridden by a file property. 
dbb.file.tagging | Controls compile log and build report file tagging. Default: true.
dbb.LinkEditScanner.excludeFilter | DBB configuration property used by the link edit scanner to exclude load module entries
dbb.RepositoryClient.url | DBB configuration property for web application URL.  ***Can be overridden by build.groovy option -url, --url***
dbb.RepositoryClient.userId | DBB configuration property for web application logon id.  ***Can be overridden by build.groovy option -id, --id***
dbb.RepositoryClient.password | DBB configuration property for web application logon password.  ***Can be overridden by build.groovy option -pw, --pw***
dbb.RepositoryClient.passwordFile | DBB configuration property for web application logon password file.  ***Can be overridden by build.groovy option -pf, --pf***

### dependencyReport.properties
Properties used by the impact utilities to generate a report of external impacted files

--- | ---
reportExternalImpacts | Flag to indicate if an *impactBuild* should analyze and report external impacted files in other collections ***Can be overridden by build.groovy option -re, --reportExternalImpacts***
reportExternalImpactsAnalysisDepths | Configuration of the analysis depths when performing impact analysis for external impacts (simple|deep) *** Can be overridden by application-conf ***
reportExternalImpactsAnalysisFileFilter | Comma-separated list of pathMatcher filters to limit the analysis of external impacts to a subset of the changed files *** Can be overridden by application-conf ***
reportExternalImpactsCollectionPatterns | Comma-separated list of regex patterns of DBB collection names for which external impacts should be documented *** Can be overridden by application-conf ***

### Assembler.properties
Build properties used by zAppBuild/language/Assembler.groovy

Property | Description
--- | ---
assembler_requiredBuildProperties | Comma separated list of required build properties for language/Assembler.groovy
assembler_srcPDS | Dataset to move assembler source files to from USS
assembler_macroPDS | Dataset to move macro files to from USS
assembler_objPDS | Dataset to create object decks in from Assembler step
assembler_dbrmPDS | Dataset to create DB2 DBRM modules in from Assembler step
assembler_loadPDS | Dataset to create load modules in from link edit step
assembler_srcDataSets | Comma separated list of 'source' type data sets
assembler_srcOptions | BPXWDYN creation options for creating 'source' type data sets
assembler_loadDatasets | Comma separated list of 'load module' type data sets
assembler_loadOptions | BPXWDYN creation options for 'load module' type data sets
assembler_tempOptions | BPXWDYN creation options for temporary data sets
assembler_compileErrorFeedbackXmlOptions | BPXWDYN creation options for SYSXMLSD data set
assembler_pgm | MVS program name of the high level assembler
assembler_linkEditor | MVS program name of the link editor
dbb.DependencyScanner.languageHint | DBB configuration property used by the dependency scanner to disambiguate a source file's language
assembler_dependenciesDatasetMapping | DBB property mapping to map dependencies to different target datasets

### BMS.properties
Build properties used by zAppBuild/language/BMS.groovy

Property | Description
--- | ---
bms_requiredBuildProperties | Comma separated list of required build properties for language/BMS.groovy
bms_srcPDS | Dataset to move bms source files to from USS
bms_copyPDS | Dataset to create generated BMS copybooks in from copy gen step
bms_loadPDS | Dataset to create load modules in from link edit step
bms_srcDataSets | Comma separated list of 'source' type data sets
bms_srcOptions | BPXWDYN creation options for creating 'source' type data sets
bms_loadDatasets | Comma separated list of 'load module' type data sets
bms_loadOptions | BPXWDYN creation options for 'load module' type data sets
bms_tempOptions | BPXWDYN creation options for temporary data sets
bms_assembler | MVS program name of the high level assembler
bms_linkEditor | MVS program name of the link editor

### Cobol.properties
Build properties used by zAppBuild/language/Cobol.groovy

Property | Description
--- | ---
cobol_requiredBuildProperties | Comma separated list of required build properties for language/Cobol.groovy
cobol_srcPDS | Dataset to move COBOL source files to from USS
cobol_cpyPDS | Dataset to move COBOL copybooks to from USS
cobol_objPDS | Dataset to create object decks in from compile step
cobol_dbrmPDS | Dataset to create DB2 DBRM modules in from compile step
cobol_loadPDS | Dataset to create load modules in from link edit step
cobol_srcDataSets | Comma separated list of 'source' type data sets
cobol_srcOptions | BPXWDYN creation options for creating 'source' type data sets
cobol_loadDatasets | Comma separated list of 'load module' type data sets
cobol_loadOptions | BPXWDYN creation options for 'load module' type data sets
cobol_tempOptions | BPXWDYN creation options for temporary data sets
cobol_test_case_srcPDS | Dataset to move COBOL test source files to from USS
cobol_test_case_loadPDS | Dataset to create load modules in from link edit step
cobol_test_srcDatasets | Comma separated list of test 'source' type data sets
cobol_test_srcOptions | BPXWDYN creation options for creating 'source' type data sets
cobol_test_loadDatasets | Comma separated list of test 'load module' type data sets
cobol_test_loadOptions | BPXWDYN creation options for creating 'load module' type data sets
cobol_compileErrorFeedbackXmlOptions | BPXWDYN creation options for SYSXMLSD data set
cobol_compiler | MVS program name of the COBOL compiler
cobol_linkEditor | MVS program name of the link editor
cobol_dependenciesAlternativeLibraryNameMapping | a map to define target dataset definition for alternate include libraries
cobol_dependenciesDatasetMapping | dbb property mapping to map dependencies to different target datasets

dbb.DependencyScanner.languageHint | DBB configuration property used by the dependency scanner to disambiguate a source file's language

### LinkEdit.properties
Build properties used by zAppBuild/language/LinkEdit.groovy

Property | Description
--- | ---
linkedit_requiredBuildProperties | Comma separated list of required build properties for language/Cobol.groovy
linkedit_linkEditor | MVS program name of the link editor
linkedit_srcPDS | Dataset to move COBOL source files to from USS
linkedit_objPDS | Dataset to create object decks in from compile step
linkedit_loadPDS | Dataset to create load modules in from link edit step
linkedit_srcDataSets | Comma separated list of 'source' type data sets
linkedit_srcOptions | BPXWDYN creation options for creating 'source' type data sets
linkedit_loadDatasets | Comma separated list of 'load module' type data sets
linkedit_loadOptions | BPXWDYN creation options for 'load module' type data sets
linkedit_tempOptions | BPXWDYN creation options for temporary data sets

### PLI.properties
Build properties used by zAppBuild/language/PLI.groovy

Property | Description
--- | ---
pli_requiredBuildProperties | Comma separated list of required build properties for language/Cobol.groovy
pli_compiler | MVS program name of the COBOL compiler
pli_linkEditor | MVS program name of the link editor
pli_srcPDS | Dataset to move PLI source files to from USS
pli_incPDS | Dataset to move PLI include files to from USS
pli_objPDS | Dataset to create object decks in from compile step
pli_dbrmPDS | Dataset to create DB2 DBRM modules in from compile step
pli_loadPDS | Dataset to create load modules in from link edit step
pli_srcDataSets | Comma separated list of 'source' type data sets
pli_srcOptions | BPXWDYN creation options for creating 'source' type data sets
pli_loadDatasets | Comma separated list of 'load module' type data sets
pli_loadOptions | BPXWDYN creation options for 'load module' type data sets
pli_tempOptions | BPXWDYN creation options for temporary data sets
pli_test_case_srcPDS | Dataset to move PLI test source files to from USS
pli_test_case_loadPDS | Dataset to create load modules in from link edit step
pli_test_srcDatasets | Comma separated list of test 'source' type data sets
pli_test_srcOptions | BPXWDYN creation options for creating 'source' type data sets
pli_test_loadDatasets | Comma separated list of test 'load module' type data sets
pli_test_loadOptions | BPXWDYN creation options for creating 'load module' type data sets
pli_compileErrorFeedbackXmlOptions | BPXWDYN creation options for SYSXMLSD data set
pli_listOptions | BPXWDYN creation options for LIST data sets
pli_dependenciesAlternativeLibraryNameMapping | a map to define target dataset definition for alternate include libraries
pli_dependenciesDatasetMapping | dbb property mapping to map dependencies to different target datasets
dbb.DependencyScanner.languageHint | DBB configuration property used by the dependency scanner to disambiguate a source file's language

### MFS.properties
Build properties used by zAppBuild/language/MFS.groovy

Property | Description
--- | ---
mfs_requiredBuildProperties | Comma separated list of required build properties for language/MFS.groovy
mfs_srcPDS | Dataset to move mfs source files to from USS
mfs_tformatPDS | Dataset to create format set from phase 2 step
mfs_srcDatasets | Comma separated list of 'source' type data sets
mfs_srcOptions | BPXWDYN creation options for creating 'source' type data sets
mfs_loadDatasets | Comma separated list of 'load module' type data sets
mfs_loadOptions | BPXWDYN creation options for 'load module' type data sets
mfs_tempOptions | BPXWDYN creation options for temporary data sets
mfs_phase1processor | MVS program name of MFSgen utility phase 1
mfs_phase2processor | MVS program name of MFSgen utility phase 2
mfs_deployType | deploy Type of format set

### DBDgen.properties
Build properties used by zAppBuild/language/DBDgen.groovy

Property | Description
--- | ---
dbdgen_requiredBuildProperties | Comma separated list of required build properties for language/DBDgen.groovy
dbdgen_srcPDS | Dataset to move assembler source files to from USS
dbdgen_objPDS | Dataset to create object decks in from Assembler step
dbdgen_loadPDS | Dataset to create load modules in from link edit step
dbdgen_srcDatasets | Comma separated list of 'source' type data sets
dbdgen_srcOptions | BPXWDYN creation options for creating 'source' type data sets
dbdgen_loadDatasets | Comma separated list of 'load module' type data sets
dbdgen_loadOptions | BPXWDYN creation options for 'load module' type data sets
dbdgen_tempOptions | BPXWDYN creation options for temporary data sets
dbdgen_compileErrorFeedbackXmlOptions | BPXWDYN creation options for SYSXMLSD data set
dbdgen_pgm | MVS program name of the high level assembler
dbdgen_linkEditor | MVS program name of the link editor
dbdgen_deployType | Deploy Type of build outputs
dbb.DependencyScanner.languageHint | DBB configuration property used by the dependency scanner to disambiguate a source file's language

### PSBgen.properties
Build properties used by zAppBuild/language/PSBgen.groovy

Property | Description
--- | ---
psbgen_requiredBuildProperties | Comma separated list of required build properties for language/PSBgen.groovy
psbgen_srcPDS | Dataset to move assembler source files to from USS
psbgen_objPDS | Dataset to create object decks in from Assembler step
psbgen_loadPDS | Dataset to create load modules in from link edit step
psbgen_srcDatasets | Comma separated list of 'source' type data sets
psbgen_srcOptions | BPXWDYN creation options for creating 'source' type data sets
psbgen_loadDatasets | Comma separated list of 'load module' type data sets
psbgen_loadOptions | BPXWDYN creation options for 'load module' type data sets
psbgen_tempOptions | BPXWDYN creation options for temporary data sets
psbgen_compileErrorFeedbackXmlOptions | BPXWDYN creation options for SYSXMLSD data set
psbgen_pgm | MVS program name of the high level assembler
psbgen_linkEditor | MVS program name of the link editor
psbgen_deployType | Deploy Type of build outputs
dbb.DependencyScanner.languageHint | DBB configuration property used by the dependency scanner to disambiguate a source file's language

### ACBgen.properties
Build properties used by zAppBuild/language/PSBgen.groovy; ACBgen is part of the PSBgen process

Property | Description
--- | ---
acbgen_requiredBuildProperties | Comma separated list of required build properties for language/PSBgen.groovy
acbgen_psbPDS | Dataset to of PSBgen output
acbgen_dbdPDS | Dataset to of DBDgen output
acbgen_loadPDS | Dataset to create acbgen modules
acbgen_loadDatasets | Comma separated list of 'load module' type data sets
acbgen_loadOptions | BPXWDYN creation options for 'load module' type data sets
acbgen_tempOptions | BPXWDYN creation options for temporary data sets
acbgen_pgm | MVS program name of the acbgen pgm
acbgen_deployType | Deploy Type of build outputs

### ZunitConfig.properties
Build properties used by zAppBuild/language/ZunitConfig.groovy

Property | Description
--- | ---
zunit_bzucfgPDS | Dataset to move BZUCFG files to from USS
zunit_bzureportPDS | Dataset where BZUCRPT files are stored
zunit_bzuplayPDS | Dataset to move zUnit Playback files to from USS
zunit_srcDatasets | Comma separated list of 'source' type data sets
zunit_srcOptions | BPXWDYN creation options for creating 'source' type data sets
zunit_loadDatasets | Comma separated list of 'load module' type data sets
zunit_loadOptions | BPXWDYN creation options for creating 'load module' type data sets
zunit_reportDatasets | Comma separated list of 'report' type data sets
zunit_reportOptions | BPXWDYN creation options for creating 'report' type data sets
zunit_dependenciesDatasetMapping | DBB property mapping to map dependencies to different target datasets

### Transfer.properties
Build properties used by zAppBuild/language/Transfer.groovy

Property | Description 
--- | --- 
transfer_requiredBuildProperties | Comma separated list of required build properties for language/Transfer.groovy
transfer_srcPDS | Dataset of any type of source
transfer_jclPDS | Sample dataset for JCL members
transfer_xmlPDS | Sample dataset for xml members
transfer_srcOptions | BPXWDYN creation options for creating 'source' type data sets