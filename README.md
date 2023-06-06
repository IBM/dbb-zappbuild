# zAppBuild
zAppBuild is a generic build solution for building z/OS applications using Apache Groovy build scripts and IBM Dependency Based Build (DBB) APIs.

## Resources
* [IBM Dependency Based Build Product Page](https://www.ibm.com/products/dependency-based-build)
* [IBM DBB Knowledge Center](https://www.ibm.com/docs/en/dbb/1.1.0)
* [IBM/dbb Repository](https://github.com/IBM/dbb/)
* [IBM IDZ Community](https://community.ibm.com/community/user/ibmz-and-linuxone/groups/topic-home?CommunityKey=f461c55d-159c-4a94-b708-9f7fe11d972b)
* [IBM DBB Community](https://community.ibm.com/community/user/ibmz-and-linuxone/groups/topic-home?CommunityKey=20c9b889-9450-4ab6-8f11-8a5eb2b3342d)


## Contributing
For instructions on how to contribute enhancements and bug fixes to zAppBuild, please read the [Contributions Guidelines](CONTRIBUTIONS.md).

## How zAppBuild works
The zAppBuild repository is intended to be cloned to a single location on Unix Systems Services (USS) and used to build all of your z/OS applications. This is done by simply copying the supplied `application-conf` folder (located in the [samples folder](samples)) to the application source repository you want to build and then verify/update the contained default configuration property values to ensure they meet the build requirements of your application. See the included [MortgageApplication](samples/MortgageApplication) sample for an example of an application that has been modified to be built by zAppBuild.  

**IMPORTANT** : The [datasets.properties](build-conf/datasets.properties) must be configured for your build machine before executing a build!  See [build-conf/README.md](build-conf/README.md) for more information.

## Supported Languages
The zAppBuild sample provides the following *language* build scripts by default:
* Assembler.groovy
* BMS.groovy
* Cobol.groovy
* LinkEdit.groovy (for building link cards)
* PLI.groovy
* DBDgen.groovy
* PSBgen.groovy
* MFS.groovy
* ZunitConfig.groovy
* Transfer.groovy (for transport non-buildable files like JCL or PROC into build libraries and register them as build output)

All language scripts both compile and optionally link-edit programs. The language build scripts are intended to be useful out of the box but depending on the complexity of your applications' build requirements, may require modifications to meet your development team's needs.  By following the examples used in the existing language build scripts of keeping all application specific references out of the build scripts and instead using configuration properties with strong default values, the zAppBuild sample can continue to be a generic build solution for all of your specific applications.

## Build Scope
The build scope of zAppBuild is an application which is loosely defined as one or more Git repositories containing all the z/OS source files required to build the application.  There are no specific rules as to the structure of the repositories except that one repository must contain the high level `application-conf` folder provided by zAppBuild which contains all of the configuration properties for building the application programs.  

**NOTE:** All source repositories that make up the application must be cloned on the build machine under a common *workspace*  directory prior to calling build.groovy.

zAppBuild supports a number of build scenarios:
* **Single Program** - Build a single program in the application.
* **List of Programs** - Build a list of programs provided by a text file.
* **Full Build** - Build all programs (or buildable files) of an application.
* **Impact Build** - Build only programs impacted by source files that have changed since the last successful build.
* **Impact Build with baseline reference** - Build only programs impacted by source files that have changed by diff'ing to a previous configuration reference.
* **Topic Branch Build** - Detects when building a topic branch for the first time and will automatically clone the dependency data collections from the main build branch in order to avoid having to rescan the entire application.
* **Merge Build** - Build only changed programs which will be merged back into the mainBuildBranch by using a triple-dot git diff. 
* **Scan Source** - Skip the actual building and only scan source files to store dependency data in collection (migration scenario).
* **Scan Source + Outputs** - Skip the actual building and only scan source files and existing load modules to dependency data in source and output collection (migration scenario with static linkage scenarios).
* **Build Preview** - Supplemental build option. Process all phases of the supplied build option, but will not execute the commands. A build report and a build result are generated with a specific status that excludes them in subsequent impact build calculations. 


Instructions on invoking a zAppBuild is included in [docs/BUILD.md](docs/BUILD.md) as well as invocation samples for the above mentioned build scenarios including sample console logs.

Application-level build properties such as compile and link options can be defined in various ways to set global defaults, application-level overrides or even file level (member-level) overrides. The various supported strategies are documented in [docs/FilePropertyManagement.md](docs/FilePropertyManagement.md).

zAppBuild comes with a set of reporting features. It helps development teams to understand the impact of changed files across multiple applications. Another feature helps to identify conflicts due to concurrent development activities within their application. An overview of these features are documented in [docs/REPORTS.md](docs/REPORTS.md).

Links to additional documentation is provided in the table below.  

## Repository Legend
Folder/File | Description | Documentation Link
--- | --- | ---
build-conf | This folder contains global configuration properties used by build.groovy and language build scripts. | [build-conf/README.md](build-conf/README.md)
docs | This folder contains supplemental documentation to help explain the implementation and usage of zAppBuild features. | [docs/README.md](docs/README.md)
languages | This folder contains the language specific build scripts that are associated to build files via script mappings (see `samples/application-conf/files.properties`) and called by build.groovy. | [languages/README.md](languages/README.md)
samples/application-conf | The `application-conf` folder contains application specific configuration properties used by build.groovy and language build scripts.  It is intended to be copied as a high level folder to the application repository and configured to meet the build requirments of the application. Ex. `myAppRepository/application-conf` | [samples/application-conf/README.md](samples/application-conf/README.md)
samples/MortgageApplication | This is an updated version of the original [MortgageApplication](https://github.com/IBM/dbb/tree/master/Build/MortgageApplication) sample designed to be built by zAppBuild. | [samples/MortgageApplication/README.md](samples/MortgageApplication/README.md)
utilities | This folder contains utility scripts which provide common utility functions used by the various zAppBuild build scripts. | [utilities/README.md](utilities/README.md)
build.groovy | This is the main build script that is called to start the build process. | [BUILD.md](BUILD.md)
test | This folder contains testing framework for ZAppBuild which includes test scripts and related test content.| [test/README.md](/test/README.md)
