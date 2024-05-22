# Application Configuration
This folder contains application specific configuration properties used by the zAppBuild Groovy build and utility scripts. It is intended to be copied as a high level folder in the application repository or main application repository if the application source files are distributed across multiple repositories. Once copied to the application repository, users should review the default property files and modify any values as needed. 

At the beginning of the build, the `application-conf/application.properties` file will automatically be loaded into the [DBB BuildProperties class](https://www.ibm.com/support/knowledgecenter/SS6T76_1.0.4/scriptorg.html#build-properties-class). Use the `applicationPropFiles` property (see table below) to load additional application property files.

## Property File Descriptions
Since all properties will be loaded into a single static instance of BuildProperties, the organization and naming convention of the *property files* are somewhat arbitrary and targeted more for self documentation and understanding.

### application.properties
This property file is loaded automatically at the beginning of the build and contains application specific properties used mainly by `build.groovy` but can also be a place to declare properties used by multiple language scripts. Additional property files are loaded based on the content of the `applicationPropFiles` property.

Property | Description
--- | ---
runzTests | Boolean value to specify if zUnit tests should be run.  Defaults to `false`, to enable zUnit Tests, set value to `true`.
applicationPropFiles | Comma separated list of additional application property files to load. Supports both absolute and relative file paths.  Relative paths assumed to be relative to ${workspace}/${application}/application-conf/.
applicationSrcDirs | Comma separated list of all source directories included in application build. Each directory is assumed to be a local Git repository clone. Supports both absolute and relative paths though for maximum reuse of collected dependency data relative paths should be used.  Relative paths assumed to be relative to ${workspace}.
buildOrder | Comma separated list of the build script processing order.
mainBuildBranch | The main build branch of the main application repository.  Used for cloning collections for topic branch builds instead of rescanning the entire application.
gitRepositoryURL | git repository URL of the application repository to establish links to the changed files in the build result properties | false
excludeFileList | Files to exclude when scanning or running full build.
skipImpactCalculationList | Files for which the impact analysis should be skipped in impact build
jobCard | JOBCARD for JCL execs
resolveSubsystems | boolean flag to configure the SearchPathDependencyResolver to evaluate if resolved dependencies impact the file flags isCICS, isSQL, isDLI, isMQ when creating the LogicalFile
impactSearch | Impact finder resolution search configuration leveraging the SearchPathImpactFinder API. Sample configurations are inlcuded below, next to the previous rule definitions.


### file.properties
Location of file properties, script mappings and file level property overrides.  All file properties for the entire application, including source files in distributed repositories of the application need to be contained either in this file or in other property files in the `application-conf` directory. Look for column 'Overridable' in the tables below for build properties that can have file level property overrides. 

Property | Description 
--- | --- 
dbb.scriptMapping | DBB configuration file properties association build files to language scripts
dbb.scannerMapping | zAppBuild configuration to map files extensions to DBB dependency scanner configurations
cobol_testcase | File property to indicate a generated zUnit cobol test case to use a different set of source and output libraries

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
cobol_dependencySearch | Cobol dependencySearch configuration to configure the SearchPathDependencyResolver. Format is a concatenated string of searchPath configurations. Strings representing the SearchPaths defined in `application-conf/application.properties`. | true
cobol_compilerVersion | Default Cobol compiler version. | true
cobol_compileMaxRC | Default compile maximum RC allowed. | true
cobol_linkEditMaxRC | Default link edit maximum RC allowed. | true
cobol_compileParms | Default base compile parameters. | true
cobol_compileCICSParms | Default CICS compile parameters. Appended to base parameters if has value.| true
cobol_compileSQLParms | Default SQL compile parameters. Appended to base parameters if has value. | true
cobol_compileErrorPrefixParms | IDz user build parameters. Appended to base parameters if has value. | true
cobol_linkEditParms | Default link edit parameters. | true
cobol_impactPropertyList | List of build properties causing programs to rebuild when changed | false
cobol_impactPropertyListCICS | List of CICS build properties causing programs to rebuild when changed | false
cobol_impactPropertyListSQL | List of SQL build properties causing programs to rebuild when changed | false
cobol_linkEdit | Flag indicating to execute the link edit step to produce a load module for the source file.  If false then a object deck will be created instead for later linking. | true
cobol_storeSSI | Flag to store abbrev git hash in ssi field in link step | true
cobol_isMQ | Flag indicating that the program contains MQ calls | true
cobol_deployType | default deployType for build output | true
cobol_deployTypeCICS | deployType for build output for build files where isCICS=true | true
cobol_deployTypeDLI | deployType for build output for build files with isDLI=true | true
cobol_scanLoadModule | Flag indicating to scan the load module for link dependencies and store in the application's outputs collection. | true
cobol_compileSyslibConcatenation | A comma-separated list of libraries to be concatenated in syslib during compile step | true
cobol_linkEditSyslibConcatenation | A comma-separated list of libraries to be concatenated in syslib during linkEdit step | true

### CRB.properties
Application properties used by zAppBuild/language/CRB.groovy

Property | Description | Overridable
--- | --- | ---
crb_maxRC | CICS Resource Builder maximum acceptable return code (default is 4 if not specified) | true

### LinkEdit.properties
Application properties used by zAppBuild/language/LinkEdit.groovy

Property | Description | Overridable
--- | --- | ---
linkedit_fileBuildRank | Default link card build rank. Used to sort link card build sub-list. Leave empty. | true
linkedit_maxRC | Default link edit maximum RC allowed. | true
linkedit_parms | Default link edit parameters. | true
linkedit_impactPropertyList | List of build properties causing programs to rebuild when changed | false
linkedit_storeSSI | Flag to store abbrev git hash in ssi field in link step | true
linkedit_deployType | default deployType for build output | true
linkedit_deployTypeCICS | deployType for build output for build files where isCICS=true set as file property | true
linkedit_deployTypeDLI | deployType for build output for build files with isDLI=true set as file property | true
linkedit_scanLoadModule | Flag indicating to scan the load module for link dependencies and store in the application's outputs collection. | true
linkEdit_linkEditSyslibConcatenation | A comma-separated list of libraries to be concatenated in syslib during linkEdit step | true

### languageConfigurationMapping.properties
Sample Language Configuration mapping properties used by dbb-zappbuild/utilities/BuildUtilities.groovy.

This contains the mapping of the files and their corresponding Language Configuration properties files residing in `zAppBuild/build-conf/language-conf` to override the default file properties.

Example: The entry - `epsnbrvl.cbl=languageConfigProps01`, means the file properties of file `epsnbrvl.cbl` will be overridden by the properties mentioned in `dbb-zappbuild/build-conf/language-conf/languageConfigProps01.properties`

See the [language configuration mapping documentation](../../../docs/FilePropertyManagement.md#language-configuration-mapping) for more details on how to enable and use language configuration mapping.
