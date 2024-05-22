# Building Applications with zAppBuild
The main or start build script for zAppBuild is `build.groovy`. Dependency Based Build (DBB) requires that the DBB_HOME environment variable be set when executing a Groovy script that uses DBB APIs.  In order to build an application using zAppBuild, change directory to the zAppBuild directory on USS and type `$DBB_HOME/bin/groovyz build.groovy`.

However this will result in an error message because the build.groovy script has four required arguments that must be present during each invocation:
* --workspace <arg> - Absolute path to workspace (root) directory containing all required source directories or local Git repositories to build the application.
* --application <arg> - Application local repository directory name (relative to workspace).
* --outDir <arg> - Absolute path to the build output root directory on USS
* --hlq <arg> -  High level qualifier for created build partition data sets


Example:
```sh
$DBB_HOME/bin/groovyz build.groovy \
                      --workspace /u/build/repos \
                      --application app1 \
                      --outDir /u/build/out \
                      --hlq BUILD.APP1
```
Since we are still missing a build target or calculated build option, the build will run successfully but not actually build any programs.

## Common Pipeline Invocation Examples

**Build one program**
```sh
$DBB_HOME/bin/groovyz build.groovy \
                      --workspace /u/build/repos \
                      --application app1 \
                      --outDir /u/build/out \
                      --hlq BUILD.APP1 \
                      app1/cobol/epsmpmt.cbl
```
**Build a list of programs contained in a text file**
```
$DBB_HOME/bin/groovyz build.groovy \
                      --workspace /u/build/repos \
                      --application app1 \
                      --outDir /u/build/out \
                      --hlq BUILD.APP1 \
                      /u/usr1/buildList.txt
```
**Build all programs in the application**
```
$DBB_HOME/bin/groovyz build.groovy \
                      --workspace /u/build/repos \
                      --application app1 \
                      --outDir /u/build/out \
                      --hlq BUILD.APP1 \
                      --fullBuild
```
**Build only programs that have changed or are impacted by changed copybooks or include files since the last successful build**
```
$DBB_HOME/bin/groovyz build.groovy \
                      --workspace /u/build/repos \
                      --application app1 \
                      --outDir /u/build/out \
                      --hlq BUILD.APP1 \
                      --impactBuild
```
**Build only the changes which will be merged back to the main build branch. No calculation of impacted files.**
```
$DBB_HOME/bin/groovyz build.groovy \
                      --workspace /u/build/repos \
                      --application app1 \
                      --outDir /u/build/out \
                      --hlq BUILD.APP1 \
                      --mergeBuild
```
**Only scan source files in the application to collect dependency data without actually creating load modules**
```
$DBB_HOME/bin/groovyz build.groovy \
                      --workspace /u/build/repos \
                      --application app1 \
                      --outDir /u/build/out \
                      --hlq BUILD.APP1 \
                      --fullBuild \
                      --scanOnly
```
**Scan source files and existing load modules for the application to collect dependency data for source and outputs without actually creating load modules**
```
$DBB_HOME/bin/groovyz build.groovy \
                      --workspace /u/build/repos \
                      --application app1 \
                      --outDir /u/build/out \
                      --hlq BUILD.APP1 \
                      --fullBuild \
                      --scanAll
```
**Build programs with the 'Test' Options for debugging**
```
$DBB_HOME/bin/groovyz build.groovy  \
                      --workspace /u/build/repos \
                      --application app1 \
                      --outDir /u/build/out \
                      --hlq BUILD.APP1 \
                      --debug \
                      --impactBuild
```
**Use Code Coverage Headless Collector in zUnit Tests and specify parameters through command-line options (which override properties defined in ZunitConfig.properties)**
```
$DBB_HOME/bin/groovyz build.groovy \
                      --workspace /u/build/repos \
                      --application app1 \
                      --outDir /u/build/out \
                      --hlq BUILD.APP1 \
                      --fullBuild \
                      --cc \
                      --cch localhost \
                      --ccp 8009 \
                      --cco "e=CCPDF"
```
## Common User Build Invocation Examples
**Build one program**

Build a single program in a user build context. Does not require use of the MetadataStore (filesystem or Db2)
```
$DBB_HOME/bin/groovyz build.groovy \
                      --workspace /u/build/repos \
                      --application app1 \
                      --outDir /u/build/out \
                      --hlq BUILD.APP1 \
                      --userBuild app1/cobol/epsmpmt.cbl
```
**Build one program using a [user build dependency file](samples/userBuildDependencyFile) predefining dependency information to skip DBB scans and dependency resolution.**

Build a single program in a user build context and provide the dependency information from the IDE to skip scanning the files on USS. Useful when building on IBM ZD&T or Wazi Sandbox environments.
```
$DBB_HOME/bin/groovyz build.groovy \
                      --workspace /u/build/repos \
                      --application app1 \
                      --outDir /u/build/out \
                      --hlq BUILD.APP1 \
                      --userBuild \
                      --dependencyFile userBuildDependencyFile.json \
                      app1/cobol/epsmpmt.cbl
```
**Build one program with Debug Options**

Build a single program in a user build context including the configured TEST compile time options.
```
$DBB_HOME/bin/groovyz build.groovy \
                      --workspace /u/build/repos \
                      --application app1 \
                      --debug \
                      --outDir /u/build/out \
                      --hlq BUILD.APP1 \
                      --userBuild app1/cobol/epsmpmt.cbl
```
**Build (Process) the zUnit Config file and start a debug session**

Process the zUnit bzucfg file in a user build context and initialize a debug session of the application under test. Requires the program under test to be compiled with Debug Options.
```
$DBB_HOME/bin/groovyz build.groovy \
                      --workspace /u/build/repos \
                      --application app1 \
                      --debugzUnitTestcase \
                      --outDir /u/build/out \
                      --hlq BUILD.APP1 \
                      --userBuild app1/testcfg/epsmpmt.bzucfg
```
**Build (Process) the zUnit Config file and collect code coverage data**

Process the zUnit bzucfg file in a user build context and direct the code coverage report to the user. Requires the program under test to be compiled with Debug Options.
```
$DBB_HOME/bin/groovyz build.groovy \
                      --workspace /u/build/repos \
                      --application app1 \
                      --ccczUnit \
                      --outDir /u/build/out \
                      --hlq BUILD.APP1 \
                      --userBuild app1/testcfg/epsmpmt.bzucfg
```
## Command Line Options Summary
```
$DBB_HOME/bin/groovyz <zAppBuildLocation>/build.groovy [options] buildfile

buildFile (optional):  Path of the source file to build (absolute or relative to workspace).
If buildFile is a text file (*.txt), then it is assumed to be a build list file.

Options:

required options:
 -w,--workspace <arg>         Absolute path to workspace (root) directory
                              containing all required source directories
 -a,--application <arg>       Application directory name (relative to workspace)
 -o,--outDir <arg>            Absolute path to the build output root directory
 -h,--hlq <arg>               High level qualifier for partition data sets

build options:

 -f,--fullBuild               Flag indicating to build all programs for
                              the application
 -i,--impactBuild             Flag indicating to build only programs impacted
                              by changed files since last successful build.
 -b,--baselineRef             Comma seperated list of git references to overwrite
                              the baselineHash hash in an impactBuild scenario.
 -m,--mergeBuild              Flag indicating to build only source code changes which will be
                              merged back to the mainBuildBranch.

 -s,--scanOnly                Flag indicating to only scan source files for application without building anything (deprecated use --scanSource)
 -ss,--scanSource             Flag indicating to only scan source files for application without building anything
 -sl,--scanLoad               Flag indicating to only scan load modules for application without building anything
 -sa,--scanAll                Flag indicating to scan both source files and load modules for application without building anything
 -pv,--preview                Supplemental flag indicating to run build in preview mode without processing the execute commands


 -r,--reset                   Deletes the application's dependency collections
                              and build result group from the DBB repository
 -v,--verbose                 Flag to turn on script trace
 -d,--debug                   Flag to build modules for debugging with
                              IBM Debug for z/OS
 -l,--logEncoding <arg>       Encoding of output logs. Default is EBCDIC
                              directory for user build
 -zTest,--runzTests           Specify if zUnit Tests should be run

 -p,--propFiles               Comma separated list of additional property files
                              to load. Absolute paths or relative to workspace
 -po,--propOverwrites         Comma separated list of key=value pairs for set and overwrite build properties

 -cc,--ccczUnit               Flag to indicate to collect code coverage reports during zUnit step
 -cch,--cccHost               Headless Code Coverage Collector host (if not specified IDz will be used for reporting)
 -ccp,--cccPort               Headless Code Coverage Collector port (if not specified IDz will be used for reporting)
 -cco,--cccOptions            Headless Code Coverage Collector Options

 -re,--reportExternalImpacts  Flag to activate analysis and report of external impacted files within DBB collections
 -cd,--checkDatasets          Flag to enable validation of the defined system dataset definitions.

Db2 MetadataStore configuration options
 -url,--url <arg>             Db2 JDBC URL for the MetadataStore.
                              Example: jdbc:db2:<Db2 server location>
 -id,--id <arg>               Db2 user id for the MetadataStore
 -pw,--pw <arg>               Db2 password (encrypted with DBB Password Utility) for the MetadataStore
 -pf,--pwFile <arg>           Absolute or relative (from workspace) path to file containing Db2 password

IDz/ZOD User Build options
 -u,--userBuild               Flag indicating running a user build
 -dz,--debugzUnitTestcase     Flag indicating to start a debug session for zUnit Test configurations as part of user build
 -e,--errPrefix <arg>         Unique id used for IDz error message datasets
 -df,--dependencyFile <arg>   Absolute or relative path (from workspace) to user build JSON file containing dependency information.

utility options
 -help,--help                 Prints this message
```

## Invocation Samples including console log

<!-- TOC depthFrom:3 depthTo:3 orderedList:false anchorMode:github.com -->

- [Build a Single Program](#build-a-single-program)
- [Build a List of Programs](#build-a-list-of-programs)
- [Perform Full Build to build all files](#perform-full-build-to-build-all-files)
- [Perform Impact Build](#perform-impact-build)
- [Perform Impact Build for topic branches](#perform-impact-build-for-topic-branches)
- [Perform Impact Build by providing baseline reference for the analysis of changed files](#perform-impact-build-by-providing-baseline-reference-for-the-analysis-of-changed-files)
- [Perform a Merge build](#perform-a-merge-build)
- [Perform a Build in Preview Mode](#perform-a-build-in-preview-mode)
- [Perform a Scan Source build](#perform-a-scan-source-build)
- [Perform a Scan Source + Outputs build](#perform-a-scan-source--outputs-build)
- [Dynamically Overwrite build properties](#dynamically-overwrite-build-properties)
- [Validate System Datasets](#validate-system-datasets)

<!-- /TOC -->
### Build a Single Program

Build a single program in the application.

By leveraging `--userBuild` zAppBuild does not intialize the MetadataStore and also does not store a build result.

```
groovyz dbb-zappbuild/build.groovy \
                      --workspace /var/dbb/dbb-zappbuild/samples \
                      --hlq USER.ZAPP.CLEAN.MASTER \
                      --workDir /var/dbb/out/MortgageApplication \
                      --application MortgageApplication \
                      --logEncoding UTF-8 \
                      --userBuild \
                      --verbose \
                      MortgageApplication/cobol/epsnbrvl.cbl
```
<details>
  <summary>Build log</summary>

```

** Build start at 20210622.080042.000
** Input args = /var/dbb/dbb-zappbuild/samples --hlq USER.ZAPP.CLEAN.MASTER --workDir /var/dbb/out/MortgageApplication --application MortgageApplication --logEncoding UTF-8 --userBuild --verbose MortgageApplication/cobol/epsnbrvl.cbl
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/datasets.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/Assembler.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/BMS.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/MFS.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/PSBgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/DBDgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/ACBgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/Cobol.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/LinkEdit.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/PLI.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/ZunitConfig.properties
** appConf = /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/file.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/BMS.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/Cobol.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/LinkEdit.properties
java.version=8.0.6.20 - pmz6480sr6fp20-20201120_02(SR6 FP20)
java.home=/V2R4/usr/lpp/java/J8.0_64
user.dir=/ZT01/var/dbb
** Build properties at start up:
..... // lists of all build properties
** Build output located at /var/dbb/out/MortgageApplication
** Unable to verify collections. No repository client.
** Adding MortgageApplication/cobol/epsnbrvl.cbl to Building build list
** Writing build list file to /var/dbb/out/MortgageApplication/buildList.txt
MortgageApplication/cobol/epsnbrvl.cbl
** Invoking build scripts according to build order: BMS.groovy,Cobol.groovy,LinkEdit.groovy
** Building files mapped to Cobol.groovy script
required props = cobol_srcPDS,cobol_cpyPDS,cobol_objPDS,cobol_loadPDS,cobol_compiler,cobol_linkEditor,cobol_tempOptions,applicationOutputsCollectionName,  SDFHCOB,SDFHLOAD,SDSNLOAD,SCEELKED
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.COBOL
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.COPY
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.OBJ
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.DBRM
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.LOAD
*** Building file MortgageApplication/cobol/epsnbrvl.cbl
*** Creating dependency resolver for MortgageApplication/cobol/epsnbrvl.cbl with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                }] rules
*** Scanning file with the default scanner
*** Resolution rules for MortgageApplication/cobol/epsnbrvl.cbl:
{"library":"SYSLIB","searchPath":[{"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","directory":"MortgageApplication\/copybook"}]}
*** Physical dependencies for MortgageApplication/cobol/epsnbrvl.cbl:
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSNBRPM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsnbrpm.cpy","category":"COPY","resolved":true}
Cobol compiler parms for MortgageApplication/cobol/epsnbrvl.cbl = LIB
** Writing build report data to /var/dbb/out/MortgageApplication/BuildReport.json
** Writing build report to /var/dbb/out/MortgageApplication/BuildReport.html
** Build ended at Tue Jun 22 08:00:44 GMT+01:00 2021
** Build State : CLEAN
** Total files processed : 1
** Total build time  : 2.547 seconds
```
</details>

### Build a List of Programs

In this build scenario, the build scope is defined through a text file, which references files to be build. The files are scanned and dependency metadata and a build result are stored in the DBB WebApp.
Either provide an absolute path to the build list file; otherwise it is assumed to be relative path within the workspace

Sample build list stored at `/var/dbb/MortgageApplication/myBuildList.txt` contains:
```
MortgageApplication/bms/epsmort.bms
MortgageApplication/cobol/epscmort.cbl
```

```
groovyz dbb-zappbuild/build.groovy \
                      --workspace /var/dbb/dbb-zappbuild/samples \
                      --hlq DBB.ZAPP.CLEAN.MASTER \
                      --workDir /var/dbb/out/MortgageApplication \
                      --application MortgageApplication \
                      --logEncoding UTF-8 \
                      --verbose \
                      /var/dbb/MortgageApplication/myBuildList.txt

```
<details>
  <summary>Build log</summary>

```
** Build start at 20210622.081915.019
** Input args = /var/dbb/dbb-zappbuild/samples --hlq DBB.ZAPP.CLEAN.MASTER --workDir /var/dbb/out/MortgageApplication --application MortgageApplication --logEncoding UTF-8 --verbose /var/dbb/MortgageApplication/myBuildList.txt
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/datasets.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/Assembler.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/BMS.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/MFS.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/PSBgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/DBDgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/ACBgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/Cobol.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/LinkEdit.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/PLI.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/ZunitConfig.properties
** appConf = /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/file.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/BMS.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/Cobol.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/LinkEdit.properties
..... // lists of all build properties
required props = buildOrder,buildListFileExt
** Repository client created for https://dbb-webapp:8080/dbb
** Build output located at /var/dbb/out/MortgageApplication/build.20210622.081915.019
** Build result created for BuildGroup:MortgageApplication-master BuildLabel:build.20210622.081915.019 at https://dbb-webapp:8080/dbb/rest/buildResult/46992
** Adding files listed in /var/dbb/MortgageApplication/myBuildList.txt to Building build list
** Writing build list file to /var/dbb/out/MortgageApplication/build.20210622.081915.019/buildList.txt
MortgageApplication/bms/epsmort.bms
MortgageApplication/cobol/epscmort.cbl
** Scanning source code.
** Updating collections MortgageApplication-master and MortgageApplication-master-outputs
*** Scanning file MortgageApplication/bms/epsmort.bms (/var/dbb/dbb-zappbuild/samples/MortgageApplication/bms/epsmort.bms)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/bms/epsmort.bms =
{"dli":false,"lname":"EPSMORT","file":"MortgageApplication\/bms\/epsmort.bms","mq":false,"cics":false,"language":"ASM","sql":false}
*** Scanning file MortgageApplication/cobol/epscmort.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epscmort.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epscmort.cbl =
{"dli":false,"lname":"EPSCMORT","file":"MortgageApplication\/cobol\/epscmort.cbl","mq":false,"cics":true,"logicalDependencies":[{"lname":"DFHAID","library":"SYSLIB","category":"COPY"},{"lname":"EPSMORT","library":"SYSLIB","category":"COPY"},{"lname":"EPSMTCOM","library":"SYSLIB","category":"COPY"},{"lname":"EPSNBRPM","library":"SYSLIB","category":"COPY"},{"lname":"SQLCA","library":"SYSLIB","category":"SQL INCLUDE"}],"language":"COB","sql":true}
** Storing 2 logical files in repository collection 'MortgageApplication-master'
HTTP/1.1 200 OK
** Invoking build scripts according to build order: BMS.groovy,Cobol.groovy,LinkEdit.groovy
** Building files mapped to BMS.groovy script
required props = bms_srcPDS,bms_cpyPDS,bms_loadPDS, bms_assembler,bms_linkEditor,bms_tempOptions,bms_maxRC,   SASMMOD1,SDFHLOAD,SDFHMAC,MACLIB,SCEELKED
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.BMS
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.BMS.COPY
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.LOAD
*** Building file MortgageApplication/bms/epsmort.bms
** Building files mapped to Cobol.groovy script
required props = cobol_srcPDS,cobol_cpyPDS,cobol_objPDS,cobol_loadPDS,cobol_compiler,cobol_linkEditor,cobol_tempOptions,applicationOutputsCollectionName,  SDFHCOB,SDFHLOAD,SDSNLOAD,SCEELKED
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.COBOL
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.COPY
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.OBJ
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.DBRM
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.LOAD
*** Building file MortgageApplication/cobol/epscmort.cbl
*** Creating dependency resolver for MortgageApplication/cobol/epscmort.cbl with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                }] rules
*** Scanning file with the default scanner
*** Resolution rules for MortgageApplication/cobol/epscmort.cbl:
{"library":"SYSLIB","searchPath":[{"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","directory":"MortgageApplication\/copybook"}]}
*** Physical dependencies for MortgageApplication/cobol/epscmort.cbl:
{"excluded":false,"lname":"DFHAID","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"lname":"EPSMORT","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTINP","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtinp.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTOUT","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtout.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTCOM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtcom.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSNBRPM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsnbrpm.cpy","category":"COPY","resolved":true}
{"excluded":false,"lname":"SQLCA","library":"SYSLIB","category":"SQL INCLUDE","resolved":false}
Cobol compiler parms for MortgageApplication/cobol/epscmort.cbl = LIB,CICS,SQL
*** Scanning load module for MortgageApplication/cobol/epscmort.cbl
*** Logical file =
{"dli":false,"lname":"EPSCMORT","file":"MortgageApplication\/cobol\/epscmort.cbl","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSNBRVL","library":"DBB.ZAPP.CLEAN.MASTER.OBJ","category":"LINK"}],"language":"ZBND","sql":false}
*** Obtaining hash for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Setting property :githash:MortgageApplication : 75e13783f2197e12772cec64a16937707ea623a5
** Setting property :giturl:MortgageApplication : git@github.ibm.com:zDevOps-Acceleration/dbb-zappbuild.git
** Writing build report data to /var/dbb/out/MortgageApplication/build.20210622.081915.019/BuildReport.json
** Writing build report to /var/dbb/out/MortgageApplication/build.20210622.081915.019/BuildReport.html
** Updating build result BuildGroup:MortgageApplication-master BuildLabel:build.20210622.081915.019 at https://dbb-webapp:8080/dbb/rest/buildResult/46992
** Build ended at Tue Jun 22 08:19:52 GMT+01:00 2021
** Build State : CLEAN
** Total files processed : 2
** Total build time  : 36.978 seconds
```

</details>

### Perform Full Build to build all files

The zAppBuild build option `--fullBuild` builds all files within the build scope which have a build script mapping defined in file.properties

```
groovyz dbb-zappbuild/build.groovy \
                      --workspace /var/dbb/dbb-zappbuild/samples \
                      --hlq DBB.ZAPP.CLEAN.MASTER \
                      --workDir /var/dbb/out/MortgageApplication \
                      --application MortgageApplication \
                      --logEncoding UTF-8 \
                      --fullBuild \
                      --verbose

```

<details>
  <summary>Build log</summary>

```
** Build start at 20210622.082224.022
** Input args = /var/dbb/dbb-zappbuild/samples --hlq DBB.ZAPP.CLEAN.MASTER --workDir /var/dbb/out/MortgageApplication --application MortgageApplication --logEncoding UTF-8 --fullBuild --verbose
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/datasets.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/Assembler.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/BMS.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/MFS.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/PSBgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/DBDgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/ACBgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/Cobol.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/LinkEdit.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/PLI.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/ZunitConfig.properties
** appConf = /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/file.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/BMS.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/Cobol.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/LinkEdit.properties
..... // lists of all build properties
** Repository client created for https://dbb-webapp:8080/dbb
** Build output located at /var/dbb/out/MortgageApplication/build.20210622.082224.022
** Build result created for BuildGroup:MortgageApplication-master BuildLabel:build.20210622.082224.022 at https://dbb-webapp:8080/dbb/rest/buildResult/47002
** --fullBuild option selected. Building all programs for application MortgageApplication
** Writing build list file to /var/dbb/out/MortgageApplication/build.20210622.082224.022/buildList.txt
MortgageApplication/copybook/epsmtout.cpy
MortgageApplication/cobol/epsnbrvl.cbl
MortgageApplication/cobol/epscsmrt.cbl
MortgageApplication/bms/epsmort.bms
MortgageApplication/link/epsmlist.lnk
MortgageApplication/copybook/epsmortf.cpy
MortgageApplication/copybook/epsnbrpm.cpy
MortgageApplication/bms/epsmlis.bms
MortgageApplication/copybook/epsmtcom.cpy
MortgageApplication/cobol/epsmlist.cbl
MortgageApplication/copybook/epsmtinp.cpy
MortgageApplication/copybook/epspdata.cpy
MortgageApplication/cobol/epsmpmt.cbl
MortgageApplication/cobol/epscmort.cbl
MortgageApplication/cobol/epscsmrd.cbl
** Scanning source code.
** Updating collections MortgageApplication-master and MortgageApplication-master-outputs
*** Scanning file MortgageApplication/copybook/epsmtout.cpy (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/epsmtout.cpy)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/epsmtout.cpy =
{"dli":false,"lname":"EPSMTOUT","file":"MortgageApplication\/copybook\/epsmtout.cpy","mq":false,"cics":false,"language":"COB","sql":false}
*** Scanning file MortgageApplication/cobol/epsnbrvl.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epsnbrvl.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epsnbrvl.cbl =
{"dli":false,"lname":"EPSNBRVL","file":"MortgageApplication\/cobol\/epsnbrvl.cbl","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSNBRPM","library":"SYSLIB","category":"COPY"}],"language":"COB","sql":false}
*** Scanning file MortgageApplication/cobol/epscsmrt.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epscsmrt.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epscsmrt.cbl =
{"dli":false,"lname":"EPSCSMRT","file":"MortgageApplication\/cobol\/epscsmrt.cbl","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSMTCOM","library":"SYSLIB","category":"COPY"},{"lname":"EPSPDATA","library":"SYSLIB","category":"COPY"}],"language":"COB","sql":false}
*** Scanning file MortgageApplication/bms/epsmort.bms (/var/dbb/dbb-zappbuild/samples/MortgageApplication/bms/epsmort.bms)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/bms/epsmort.bms =
{"dli":false,"lname":"EPSMORT","file":"MortgageApplication\/bms\/epsmort.bms","mq":false,"cics":false,"language":"ASM","sql":false}
*** Scanning file MortgageApplication/link/epsmlist.lnk (/var/dbb/dbb-zappbuild/samples/MortgageApplication/link/epsmlist.lnk)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/link/epsmlist.lnk =
{"dli":false,"lname":"EPSMLIST","file":"MortgageApplication\/link\/epsmlist.lnk","mq":false,"cics":false,"language":"ZBND","sql":false}
*** Scanning file MortgageApplication/copybook/epsmortf.cpy (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/epsmortf.cpy)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/epsmortf.cpy =
{"dli":false,"lname":"EPSMORTF","file":"MortgageApplication\/copybook\/epsmortf.cpy","mq":false,"cics":false,"language":"COB","sql":false}
*** Scanning file MortgageApplication/copybook/epsnbrpm.cpy (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/epsnbrpm.cpy)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/epsnbrpm.cpy =
{"dli":false,"lname":"EPSNBRPM","file":"MortgageApplication\/copybook\/epsnbrpm.cpy","mq":false,"cics":false,"language":"COB","sql":false}
*** Scanning file MortgageApplication/bms/epsmlis.bms (/var/dbb/dbb-zappbuild/samples/MortgageApplication/bms/epsmlis.bms)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/bms/epsmlis.bms =
{"dli":false,"lname":"EPSMLIS","file":"MortgageApplication\/bms\/epsmlis.bms","mq":false,"cics":false,"language":"ASM","sql":false}
*** Scanning file MortgageApplication/copybook/epsmtcom.cpy (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/epsmtcom.cpy)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/epsmtcom.cpy =
{"dli":false,"lname":"EPSMTCOM","file":"MortgageApplication\/copybook\/epsmtcom.cpy","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSMTINP","library":"SYSLIB","category":"COPY"},{"lname":"EPSMTOUT","library":"SYSLIB","category":"COPY"}],"language":"COB","sql":false}
*** Scanning file MortgageApplication/cobol/epsmlist.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epsmlist.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epsmlist.cbl =
{"dli":false,"lname":"EPSMLIST","file":"MortgageApplication\/cobol\/epsmlist.cbl","mq":false,"cics":true,"logicalDependencies":[{"lname":"DFHAID","library":"SYSLIB","category":"COPY"},{"lname":"EPSMLIS","library":"SYSLIB","category":"COPY"},{"lname":"EPSMORTF","library":"SYSLIB","category":"COPY"},{"lname":"EPSMTCOM","library":"SYSLIB","category":"COPY"},{"lname":"EPSNBRPM","library":"SYSLIB","category":"COPY"}],"language":"COB","sql":false}
*** Scanning file MortgageApplication/copybook/epsmtinp.cpy (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/epsmtinp.cpy)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/epsmtinp.cpy =
{"dli":false,"lname":"EPSMTINP","file":"MortgageApplication\/copybook\/epsmtinp.cpy","mq":false,"cics":false,"language":"COB","sql":false}
*** Scanning file MortgageApplication/copybook/epspdata.cpy (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/epspdata.cpy)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/epspdata.cpy =
{"dli":false,"lname":"EPSPDATA","file":"MortgageApplication\/copybook\/epspdata.cpy","mq":false,"cics":false,"language":"COB","sql":false}
*** Scanning file MortgageApplication/cobol/epsmpmt.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epsmpmt.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epsmpmt.cbl =
{"dli":false,"lname":"EPSMPMT","file":"MortgageApplication\/cobol\/epsmpmt.cbl","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSPDATA","library":"SYSLIB","category":"COPY"}],"language":"COB","sql":false}
*** Scanning file MortgageApplication/cobol/epscmort.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epscmort.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epscmort.cbl =
{"dli":false,"lname":"EPSCMORT","file":"MortgageApplication\/cobol\/epscmort.cbl","mq":false,"cics":true,"logicalDependencies":[{"lname":"DFHAID","library":"SYSLIB","category":"COPY"},{"lname":"EPSMORT","library":"SYSLIB","category":"COPY"},{"lname":"EPSMTCOM","library":"SYSLIB","category":"COPY"},{"lname":"EPSNBRPM","library":"SYSLIB","category":"COPY"},{"lname":"SQLCA","library":"SYSLIB","category":"SQL INCLUDE"}],"language":"COB","sql":true}
*** Scanning file MortgageApplication/cobol/epscsmrd.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epscsmrd.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epscsmrd.cbl =
{"dli":false,"lname":"EPSCSMRD","file":"MortgageApplication\/cobol\/epscsmrd.cbl","mq":false,"cics":true,"language":"COB","sql":false}
** Storing 15 logical files in repository collection 'MortgageApplication-master'
HTTP/1.1 200 OK
** Invoking build scripts according to build order: BMS.groovy,Cobol.groovy,LinkEdit.groovy
** Building files mapped to BMS.groovy script
required props = bms_srcPDS,bms_cpyPDS,bms_loadPDS, bms_assembler,bms_linkEditor,bms_tempOptions,bms_maxRC,   SASMMOD1,SDFHLOAD,SDFHMAC,MACLIB,SCEELKED
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.BMS
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.BMS.COPY
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.LOAD
*** Building file MortgageApplication/bms/epsmort.bms
*** Building file MortgageApplication/bms/epsmlis.bms
** Building files mapped to Cobol.groovy script
required props = cobol_srcPDS,cobol_cpyPDS,cobol_objPDS,cobol_loadPDS,cobol_compiler,cobol_linkEditor,cobol_tempOptions,applicationOutputsCollectionName,  SDFHCOB,SDFHLOAD,SDSNLOAD,SCEELKED
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.COBOL
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.COPY
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.OBJ
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.DBRM
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.LOAD
*** Building file MortgageApplication/cobol/epsnbrvl.cbl
*** Creating dependency resolver for MortgageApplication/cobol/epsnbrvl.cbl with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                }] rules
*** Scanning file with the default scanner
*** Resolution rules for MortgageApplication/cobol/epsnbrvl.cbl:
{"library":"SYSLIB","searchPath":[{"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","directory":"MortgageApplication\/copybook"}]}
*** Physical dependencies for MortgageApplication/cobol/epsnbrvl.cbl:
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSNBRPM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsnbrpm.cpy","category":"COPY","resolved":true}
Cobol compiler parms for MortgageApplication/cobol/epsnbrvl.cbl = LIB
*** Building file MortgageApplication/cobol/epscsmrt.cbl
*** Creating dependency resolver for MortgageApplication/cobol/epscsmrt.cbl with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                }] rules
*** Scanning file with the default scanner
*** Resolution rules for MortgageApplication/cobol/epscsmrt.cbl:
{"library":"SYSLIB","searchPath":[{"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","directory":"MortgageApplication\/copybook"}]}
*** Physical dependencies for MortgageApplication/cobol/epscsmrt.cbl:
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTINP","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtinp.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTOUT","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtout.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTCOM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtcom.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSPDATA","library":"SYSLIB","file":"MortgageApplication\/copybook\/epspdata.cpy","category":"COPY","resolved":true}
Cobol compiler parms for MortgageApplication/cobol/epscsmrt.cbl = LIB,CICS
*** Scanning load module for MortgageApplication/cobol/epscsmrt.cbl
*** Logical file =
{"dli":false,"lname":"EPSCSMRT","file":"MortgageApplication\/cobol\/epscsmrt.cbl","mq":false,"cics":false,"language":"ZBND","sql":false}
*** Building file MortgageApplication/cobol/epsmlist.cbl
*** Creating dependency resolver for MortgageApplication/cobol/epsmlist.cbl with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                }] rules
*** Scanning file with the default scanner
*** Resolution rules for MortgageApplication/cobol/epsmlist.cbl:
{"library":"SYSLIB","searchPath":[{"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","directory":"MortgageApplication\/copybook"}]}
*** Physical dependencies for MortgageApplication/cobol/epsmlist.cbl:
{"excluded":false,"lname":"DFHAID","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"lname":"EPSMLIS","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMORTF","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmortf.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTINP","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtinp.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTOUT","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtout.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTCOM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtcom.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSNBRPM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsnbrpm.cpy","category":"COPY","resolved":true}
Cobol compiler parms for MortgageApplication/cobol/epsmlist.cbl = LIB,CICS
*** Building file MortgageApplication/cobol/epsmpmt.cbl
*** Creating dependency resolver for MortgageApplication/cobol/epsmpmt.cbl with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                }] rules
*** Scanning file with the default scanner
*** Resolution rules for MortgageApplication/cobol/epsmpmt.cbl:
{"library":"SYSLIB","searchPath":[{"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","directory":"MortgageApplication\/copybook"}]}
*** Physical dependencies for MortgageApplication/cobol/epsmpmt.cbl:
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSPDATA","library":"SYSLIB","file":"MortgageApplication\/copybook\/epspdata.cpy","category":"COPY","resolved":true}
Cobol compiler parms for MortgageApplication/cobol/epsmpmt.cbl = LIB
*** Scanning load module for MortgageApplication/cobol/epsmpmt.cbl
*** Logical file =
{"dli":false,"lname":"EPSMPMT","file":"MortgageApplication\/cobol\/epsmpmt.cbl","mq":false,"cics":false,"language":"ZBND","sql":false}
*** Building file MortgageApplication/cobol/epscmort.cbl
*** Creating dependency resolver for MortgageApplication/cobol/epscmort.cbl with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                }] rules
*** Scanning file with the default scanner
*** Resolution rules for MortgageApplication/cobol/epscmort.cbl:
{"library":"SYSLIB","searchPath":[{"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","directory":"MortgageApplication\/copybook"}]}
*** Physical dependencies for MortgageApplication/cobol/epscmort.cbl:
{"excluded":false,"lname":"DFHAID","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"lname":"EPSMORT","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTINP","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtinp.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTOUT","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtout.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTCOM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtcom.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSNBRPM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsnbrpm.cpy","category":"COPY","resolved":true}
{"excluded":false,"lname":"SQLCA","library":"SYSLIB","category":"SQL INCLUDE","resolved":false}
Cobol compiler parms for MortgageApplication/cobol/epscmort.cbl = LIB,CICS,SQL
*** Scanning load module for MortgageApplication/cobol/epscmort.cbl
*** Logical file =
{"dli":false,"lname":"EPSCMORT","file":"MortgageApplication\/cobol\/epscmort.cbl","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSNBRVL","library":"DBB.ZAPP.CLEAN.MASTER.OBJ","category":"LINK"}],"language":"ZBND","sql":false}
*** Building file MortgageApplication/cobol/epscsmrd.cbl
*** Creating dependency resolver for MortgageApplication/cobol/epscsmrd.cbl with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                }] rules
*** Scanning file with the default scanner
*** Resolution rules for MortgageApplication/cobol/epscsmrd.cbl:
{"library":"SYSLIB","searchPath":[{"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","directory":"MortgageApplication\/copybook"}]}
*** Physical dependencies for MortgageApplication/cobol/epscsmrd.cbl:
Cobol compiler parms for MortgageApplication/cobol/epscsmrd.cbl = LIB,CICS
*** Scanning load module for MortgageApplication/cobol/epscsmrd.cbl
*** Logical file =
{"dli":false,"lname":"EPSCSMRD","file":"MortgageApplication\/cobol\/epscsmrd.cbl","mq":false,"cics":false,"language":"ZBND","sql":false}
** Building files mapped to LinkEdit.groovy script
required props = linkedit_srcPDS,linkedit_objPDS,linkedit_loadPDS,linkedit_linkEditor,linkedit_tempOptions,applicationOutputsCollectionName,  SDFHLOAD,SCEELKED
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.LINK
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.OBJ
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.LOAD
*** Building file MortgageApplication/link/epsmlist.lnk
*** Creating dependency resolver for MortgageApplication/link/epsmlist.lnk with null rules
*** Scanning file with the default scanner
*** Scanning load module for MortgageApplication/link/epsmlist.lnk
*** Logical file =
{"dli":false,"lname":"EPSMLIST","file":"MortgageApplication\/link\/epsmlist.lnk","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSMPMT","library":"DBB.ZAPP.CLEAN.MASTER.LOAD","category":"LINK"},{"lname":"EPSMLIST","library":"DBB.ZAPP.CLEAN.MASTER.OBJ","category":"LINK"}],"language":"ZBND","sql":false}
*** Obtaining hash for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Setting property :githash:MortgageApplication : 75e13783f2197e12772cec64a16937707ea623a5
** Setting property :giturl:MortgageApplication : git@github.ibm.com:zDevOps-Acceleration/dbb-zappbuild.git
** Writing build report data to /var/dbb/out/MortgageApplication/build.20210622.082224.022/BuildReport.json
** Writing build report to /var/dbb/out/MortgageApplication/build.20210622.082224.022/BuildReport.html
** Updating build result BuildGroup:MortgageApplication-master BuildLabel:build.20210622.082224.022 at https://dbb-webapp:8080/dbb/rest/buildResult/47002
** Build ended at Tue Jun 22 08:22:44 GMT+01:00 2021
** Build State : CLEAN
** Total files processed : 9
** Total build time  : 20.217 seconds

** Build finished
```

</details>

### Perform Impact Build

`--impactBuild` builds only programs impacted by source files that have changed since the last successful build.

This build scenario identifies the changed files based on diffing the git baseline hash and the current hash; then the list of changed files is passed into the impact analysis phase, which will detect the impacted files based on the `impactResolutionRules` which are defined in application.properties.

```
groovyz dbb-zappbuild/build.groovy \
                      --workspace /var/dbb/dbb-zappbuild/samples \
                      --hlq DBB.ZAPP.CLEAN.MASTER \
                      --workDir /var/dbb/out/MortgageApplication \
                      --application MortgageApplication \
                      --logEncoding UTF-8 \
                      --impactBuild \
                      --verbose

```
<details>
  <summary>Build log</summary>

```
** Build start at 20210622.082942.029
** Input args = /var/dbb/dbb-zappbuild/samples --hlq DBB.ZAPP.CLEAN.MASTER --workDir /var/dbb/out/MortgageApplication --application MortgageApplication --logEncoding UTF-8 --impactBuild --verbose
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/datasets.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/Assembler.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/BMS.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/MFS.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/PSBgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/DBDgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/ACBgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/Cobol.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/LinkEdit.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/PLI.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/ZunitConfig.properties
** appConf = /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/file.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/BMS.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/Cobol.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/LinkEdit.properties
..... // lists of all build properties
** Repository client created for https://dbb-webapp:8080/dbb
** Build output located at /var/dbb/out/MortgageApplication/build.20210622.082942.029
** Build result created for BuildGroup:MortgageApplication-master BuildLabel:build.20210622.082942.029 at https://dbb-webapp:8080/dbb/rest/buildResult/47012
** --impactBuild option selected. Building impacted programs for application MortgageApplication
** Getting current hash for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Storing MortgageApplication : 857266a44a6e859c4f949adb7e32cfbc4a8bd736
** Getting baseline hash for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Storing MortgageApplication : 75e13783f2197e12772cec64a16937707ea623a5
** Calculating changed files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Diffing baseline 75e13783f2197e12772cec64a16937707ea623a5 -> current 857266a44a6e859c4f949adb7e32cfbc4a8bd736
*** Changed files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication:
**** MortgageApplication/copybook/epsmtcom.cpy
*** Deleted files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication:
*** Renamed files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication:
** Updating collections MortgageApplication-master and MortgageApplication-master-outputs
*** Scanning file MortgageApplication/copybook/epsmtcom.cpy (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/epsmtcom.cpy)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/epsmtcom.cpy =
{"dli":false,"lname":"EPSMTCOM","file":"MortgageApplication\/copybook\/epsmtcom.cpy","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSMTINP","library":"SYSLIB","category":"COPY"},{"lname":"EPSMTOUT","library":"SYSLIB","category":"COPY"}],"language":"COB","sql":false}
** Storing 1 logical files in repository collection 'MortgageApplication-master'
HTTP/1.1 200 OK
** Performing impact analysis on changed file MortgageApplication/copybook/epsmtcom.cpy
*** Creating impact resolver for MortgageApplication/copybook/epsmtcom.cpy with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                },{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/bms"} ]             },{"category": "LINK", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/cobol"}, {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/link"} ]             }] rules
** Found impacted file MortgageApplication/cobol/epscsmrt.cbl
** MortgageApplication/cobol/epscsmrt.cbl is impacted by changed file MortgageApplication/copybook/epsmtcom.cpy. Adding to build list.
** Found impacted file MortgageApplication/link/epsmlist.lnk
** MortgageApplication/link/epsmlist.lnk is impacted by changed file MortgageApplication/copybook/epsmtcom.cpy. Adding to build list.
** Found impacted file MortgageApplication/cobol/epscmort.cbl
** MortgageApplication/cobol/epscmort.cbl is impacted by changed file MortgageApplication/copybook/epsmtcom.cpy. Adding to build list.
** Found impacted file MortgageApplication/cobol/epsmlist.cbl
** MortgageApplication/cobol/epsmlist.cbl is impacted by changed file MortgageApplication/copybook/epsmtcom.cpy. Adding to build list.
** Found impacted file MortgageApplication/cobol/epscsmrt.cbl
** MortgageApplication/cobol/epscsmrt.cbl is impacted by changed file MortgageApplication/copybook/epsmtcom.cpy. Adding to build list.
** Found impacted file MortgageApplication/cobol/epscmort.cbl
** MortgageApplication/cobol/epscmort.cbl is impacted by changed file MortgageApplication/copybook/epsmtcom.cpy. Adding to build list.
** Writing build list file to /var/dbb/out/MortgageApplication/build.20210622.082942.029/buildList.txt
MortgageApplication/cobol/epsmlist.cbl
MortgageApplication/cobol/epscsmrt.cbl
MortgageApplication/cobol/epscmort.cbl
MortgageApplication/link/epsmlist.lnk
** Invoking build scripts according to build order: BMS.groovy,Cobol.groovy,LinkEdit.groovy
** Building files mapped to Cobol.groovy script
required props = cobol_srcPDS,cobol_cpyPDS,cobol_objPDS,cobol_loadPDS,cobol_compiler,cobol_linkEditor,cobol_tempOptions,applicationOutputsCollectionName,  SDFHCOB,SDFHLOAD,SDSNLOAD,SCEELKED
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.COBOL
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.COPY
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.OBJ
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.DBRM
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.LOAD
*** Building file MortgageApplication/cobol/epsmlist.cbl
*** Creating dependency resolver for MortgageApplication/cobol/epsmlist.cbl with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                }] rules
*** Scanning file with the default scanner
*** Resolution rules for MortgageApplication/cobol/epsmlist.cbl:
{"library":"SYSLIB","searchPath":[{"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","directory":"MortgageApplication\/copybook"}]}
*** Physical dependencies for MortgageApplication/cobol/epsmlist.cbl:
{"excluded":false,"lname":"DFHAID","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"lname":"EPSMLIS","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMORTF","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmortf.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTINP","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtinp.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTOUT","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtout.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTCOM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtcom.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSNBRPM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsnbrpm.cpy","category":"COPY","resolved":true}
Cobol compiler parms for MortgageApplication/cobol/epsmlist.cbl = LIB,CICS
*** Building file MortgageApplication/cobol/epscsmrt.cbl
*** Creating dependency resolver for MortgageApplication/cobol/epscsmrt.cbl with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                }] rules
*** Scanning file with the default scanner
*** Resolution rules for MortgageApplication/cobol/epscsmrt.cbl:
{"library":"SYSLIB","searchPath":[{"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","directory":"MortgageApplication\/copybook"}]}
*** Physical dependencies for MortgageApplication/cobol/epscsmrt.cbl:
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTINP","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtinp.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTOUT","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtout.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTCOM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtcom.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSPDATA","library":"SYSLIB","file":"MortgageApplication\/copybook\/epspdata.cpy","category":"COPY","resolved":true}
Cobol compiler parms for MortgageApplication/cobol/epscsmrt.cbl = LIB,CICS
*** Scanning load module for MortgageApplication/cobol/epscsmrt.cbl
*** Logical file =
{"dli":false,"lname":"EPSCSMRT","file":"MortgageApplication\/cobol\/epscsmrt.cbl","mq":false,"cics":false,"language":"ZBND","sql":false}
*** Building file MortgageApplication/cobol/epscmort.cbl
*** Creating dependency resolver for MortgageApplication/cobol/epscmort.cbl with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                }] rules
*** Scanning file with the default scanner
*** Resolution rules for MortgageApplication/cobol/epscmort.cbl:
{"library":"SYSLIB","searchPath":[{"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","directory":"MortgageApplication\/copybook"}]}
*** Physical dependencies for MortgageApplication/cobol/epscmort.cbl:
{"excluded":false,"lname":"DFHAID","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"lname":"EPSMORT","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTINP","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtinp.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTOUT","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtout.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTCOM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtcom.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSNBRPM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsnbrpm.cpy","category":"COPY","resolved":true}
{"excluded":false,"lname":"SQLCA","library":"SYSLIB","category":"SQL INCLUDE","resolved":false}
Cobol compiler parms for MortgageApplication/cobol/epscmort.cbl = LIB,CICS,SQL
*** Scanning load module for MortgageApplication/cobol/epscmort.cbl
*** Logical file =
{"dli":false,"lname":"EPSCMORT","file":"MortgageApplication\/cobol\/epscmort.cbl","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSNBRVL","library":"DBB.ZAPP.CLEAN.MASTER.OBJ","category":"LINK"}],"language":"ZBND","sql":false}
** Building files mapped to LinkEdit.groovy script
required props = linkedit_srcPDS,linkedit_objPDS,linkedit_loadPDS,linkedit_linkEditor,linkedit_tempOptions,applicationOutputsCollectionName,  SDFHLOAD,SCEELKED
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.LINK
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.OBJ
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.LOAD
*** Building file MortgageApplication/link/epsmlist.lnk
*** Creating dependency resolver for MortgageApplication/link/epsmlist.lnk with null rules
*** Scanning file with the default scanner
*** Scanning load module for MortgageApplication/link/epsmlist.lnk
*** Logical file =
{"dli":false,"lname":"EPSMLIST","file":"MortgageApplication\/link\/epsmlist.lnk","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSMPMT","library":"DBB.ZAPP.CLEAN.MASTER.LOAD","category":"LINK"},{"lname":"EPSMLIST","library":"DBB.ZAPP.CLEAN.MASTER.OBJ","category":"LINK"}],"language":"ZBND","sql":false}
*** Obtaining hash for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Setting property :githash:MortgageApplication : 857266a44a6e859c4f949adb7e32cfbc4a8bd736
** Setting property :giturl:MortgageApplication : git@github.ibm.com:zDevOps-Acceleration/dbb-zappbuild.git
** Writing build report data to /var/dbb/out/MortgageApplication/build.20210622.082942.029/BuildReport.json
** Writing build report to /var/dbb/out/MortgageApplication/build.20210622.082942.029/BuildReport.html
** Updating build result BuildGroup:MortgageApplication-master BuildLabel:build.20210622.082942.029 at https://dbb-webapp:8080/dbb/rest/buildResult/47012
** Build ended at Tue Jun 22 08:29:59 GMT+01:00 2021
** Build State : CLEAN
** Total files processed : 4
** Total build time  : 17.239 seconds
```

</details>

### Perform Impact Build for topic branches

zAppBuild is able to detect when building a topic branch for the first time. It will automatically clone the dependency data collections from the main build branch (see `mainBuildBranch` build property in application.properties) in order to avoid having to rescan the entire application.

It also leverages the last successful build result from the buildgroup of the `mainBuildBranch` configuration to obtain the baseline hash to calculate the changed files.

The invocation is similar to other impact builds (you might want to consider a dedicated set of build libraries):
```
groovyz dbb-zappbuild/build.groovy \
                      --workspace /var/dbb/dbb-zappbuild/samples \
                      --hlq DBB.ZAPP.CLEAN.FEAT \
                      --workDir /var/dbb/out/MortgageApplication \
                      --application MortgageApplication \
                      --logEncoding UTF-8 \
                      --impactBuild \
                      --verbose
```
Please see the output provided in verbose mode when setting up the collections as well as the calculation of changed files:
<details>
  <summary>Build log</summary>

```
** Build start at 20210622.085830.058
** Input args = /var/dbb/dbb-zappbuild/samples --hlq DBB.ZAPP.CLEAN.FEAT --workDir /var/dbb/out/MortgageApplication --application MortgageApplication --logEncoding UTF-8 --impactBuild --verbose
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/datasets.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/Assembler.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/BMS.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/MFS.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/PSBgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/DBDgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/ACBgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/Cobol.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/LinkEdit.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/PLI.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/ZunitConfig.properties
** appConf = /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/file.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/BMS.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/Cobol.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/LinkEdit.properties
..... // lists of all build properties
** Repository client created for https://dbb-webapp:8080/dbb
** Build output located at /var/dbb/out/MortgageApplication/build.20210622.085830.058
** Build result created for BuildGroup:MortgageApplication-topic200 BuildLabel:build.20210622.085830.058 at https://dbb-webapp:8080/dbb/rest/buildResult/47056
** Cloned collection MortgageApplication-topic200 from MortgageApplication-master
** Cloned collection MortgageApplication-topic200-outputs from MortgageApplication-master-outputs
** --impactBuild option selected. Building impacted programs for application MortgageApplication
** No previous topic branch successful build result. Retrieving last successful main branch build result.
** Getting current hash for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Storing MortgageApplication : bee818488554ec76ebb5caffb2139cd1cd9edea2
** Getting baseline hash for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Storing MortgageApplication : 857266a44a6e859c4f949adb7e32cfbc4a8bd736
** Calculating changed files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Diffing baseline 857266a44a6e859c4f949adb7e32cfbc4a8bd736 -> current bee818488554ec76ebb5caffb2139cd1cd9edea2
*** Changed files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication:
**** MortgageApplication/cobol/epsnbrvl.cbl
*** Deleted files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication:
*** Renamed files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication:
** Updating collections MortgageApplication-topic200 and MortgageApplication-topic200-outputs
*** Scanning file MortgageApplication/cobol/epsnbrvl.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epsnbrvl.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epsnbrvl.cbl =
{"dli":false,"lname":"EPSNBRVL","file":"MortgageApplication\/cobol\/epsnbrvl.cbl","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSNBRPM","library":"SYSLIB","category":"COPY"}],"language":"COB","sql":false}
** Storing 1 logical files in repository collection 'MortgageApplication-topic200'
HTTP/1.1 200 OK
** Found build script mapping for MortgageApplication/cobol/epsnbrvl.cbl. Adding to build list
** Performing impact analysis on changed file MortgageApplication/cobol/epsnbrvl.cbl
*** Creating impact resolver for MortgageApplication/cobol/epsnbrvl.cbl with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                },{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/bms"} ]             },{"category": "LINK", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/cobol"}, {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/link"} ]             }] rules
** Found impacted file MortgageApplication/cobol/epscmort.cbl
** MortgageApplication/cobol/epscmort.cbl is impacted by changed file MortgageApplication/cobol/epsnbrvl.cbl. Adding to build list.
** Writing build list file to /var/dbb/out/MortgageApplication/build.20210622.085830.058/buildList.txt
MortgageApplication/cobol/epsnbrvl.cbl
MortgageApplication/cobol/epscmort.cbl
** Invoking build scripts according to build order: BMS.groovy,Cobol.groovy,LinkEdit.groovy
** Building files mapped to Cobol.groovy script
required props = cobol_srcPDS,cobol_cpyPDS,cobol_objPDS,cobol_loadPDS,cobol_compiler,cobol_linkEditor,cobol_tempOptions,applicationOutputsCollectionName,  SDFHCOB,SDFHLOAD,SDSNLOAD,SCEELKED
** Creating / verifying build dataset DBB.ZAPP.CLEAN.FEAT.COBOL
** Creating / verifying build dataset DBB.ZAPP.CLEAN.FEAT.COPY
** Creating / verifying build dataset DBB.ZAPP.CLEAN.FEAT.OBJ
** Creating / verifying build dataset DBB.ZAPP.CLEAN.FEAT.DBRM
** Creating / verifying build dataset DBB.ZAPP.CLEAN.FEAT.LOAD
*** Building file MortgageApplication/cobol/epsnbrvl.cbl
*** Creating dependency resolver for MortgageApplication/cobol/epsnbrvl.cbl with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                }] rules
*** Scanning file with the default scanner
*** Resolution rules for MortgageApplication/cobol/epsnbrvl.cbl:
{"library":"SYSLIB","searchPath":[{"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","directory":"MortgageApplication\/copybook"}]}
*** Physical dependencies for MortgageApplication/cobol/epsnbrvl.cbl:
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSNBRPM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsnbrpm.cpy","category":"COPY","resolved":true}
Cobol compiler parms for MortgageApplication/cobol/epsnbrvl.cbl = LIB
*** Building file MortgageApplication/cobol/epscmort.cbl
*** Creating dependency resolver for MortgageApplication/cobol/epscmort.cbl with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                }] rules
*** Scanning file with the default scanner
*** Resolution rules for MortgageApplication/cobol/epscmort.cbl:
{"library":"SYSLIB","searchPath":[{"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","directory":"MortgageApplication\/copybook"}]}
*** Physical dependencies for MortgageApplication/cobol/epscmort.cbl:
{"excluded":false,"lname":"DFHAID","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"lname":"EPSMORT","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTINP","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtinp.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTOUT","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtout.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTCOM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtcom.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSNBRPM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsnbrpm.cpy","category":"COPY","resolved":true}
{"excluded":false,"lname":"SQLCA","library":"SYSLIB","category":"SQL INCLUDE","resolved":false}
Cobol compiler parms for MortgageApplication/cobol/epscmort.cbl = LIB,CICS,SQL
*! The compile return code (12) for MortgageApplication/cobol/epscmort.cbl exceeded the maximum return code allowed (4)
*** Obtaining hash for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Setting property :githash:MortgageApplication : bee818488554ec76ebb5caffb2139cd1cd9edea2
** Setting property :giturl:MortgageApplication : git@github.ibm.com:zDevOps-Acceleration/dbb-zappbuild.git
** Writing build report data to /var/dbb/out/MortgageApplication/build.20210622.085830.058/BuildReport.json
** Writing build report to /var/dbb/out/MortgageApplication/build.20210622.085830.058/BuildReport.html
** Updating build result BuildGroup:MortgageApplication-topic200 BuildLabel:build.20210622.085830.058 at https://dbb-webapp:8080/dbb/rest/buildResult/47056
** Build ended at Tue Jun 22 08:59:15 GMT+01:00 2021
** Build State : ERROR
** Total files processed : 2
** Total build time  : 44.702 seconds
```

</details>

### Perform Impact Build by providing baseline reference for the analysis of changed files

Implementing a release-based approach will lead to combining several features into the release candidate, which is formed in a release branch. For more information please have a look to the documentation about git flow.

To create an incremental release candidate, the build framework needs to perform a consolidated build that includes all changed files and their impacts (potentially this build can use optimized compile options). In this situation, the first build on the release branch must be slightly different from the typical impact build of topic branches.

The invocation for this consolidated build is performed through the `--impactBuild` parameter with the use of an additional option, called `--baselineRef`. The command-line option `--baselineRef` allows you to specify the baseline hash/tag for each directory when running an impact analysis: each file that was changed, renamed or deleted between the baseline hash/tag and the current hash will be managed by the build framework, and will impact the scope of the impact build. The referenced directory needs to be in the list of the `applicationSrcDirs` build property.

The syntax for `--baselineRef` is a comma-seperated list of mappings for each application source dir. Each mapping is seperated by a colon:
```
--baselineRef <application source dir>:<gitReference>,<application source dir>:<gitReference>,...
```

Alternatively, for the main application directory reference, it is sufficient to specify `--baselineRef <gitReference>`.

`gitReference` can either be a git commit hash or a git tag in the history.

Another scenario of this build setup is to run the build with the DBB reportOnly option to build a cumulative deployment package without rebuilding the binaries.

```
groovyz dbb-zappbuild/build.groovy \
                      --workspace /var/dbb/dbb-zappbuild/samples \
                      --hlq DBB.ZAPP.REL \
                      --workDir /var/dbb/out/MortgageApplication \
                      --application MortgageApplication \
                      --logEncoding UTF-8 \
                      --impactBuild \
                      --baselineRef 6db56f7eecb420b56b69ca0ab7fcc2f1d9a7e5a8 \
                      --verbose
```
<details>
  <summary>Build log</summary>

```
** Build start at 20210830.095350.053
** Input args = /var/dbb/dbb-zappbuild/samples --workDir /var/dbb/out/MortgageApplication --hlq DBB.ZAPP.REL --application MortgageApplication --verbose --propFiles /var/dbb/dbb-zappbuild-config/build.properties,/var/dbb/dbb-zappbuild-config/datasets.properties,/var/jenkins/zappbuild_config/zappbuild.jenkins.properties --impactBuild --baselineRef 6db56f7eecb420b56b69ca0ab7fcc2f1d9a7e5a8
** Loading property file /var/dbb/dbb-zappbuild/build-conf/datasets.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/dependencyReport.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/Assembler.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/BMS.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/MFS.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/PSBgen.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/DBDgen.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/ACBgen.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/Cobol.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/LinkEdit.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/PLI.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/REXX.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/ZunitConfig.properties
** appConf = /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/file.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/BMS.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/Cobol.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/LinkEdit.properties
** Loading property file /var/dbb/dbb-zappbuild-config/build.properties
** Loading property file /var/dbb/dbb-zappbuild-config/datasets.properties
** Loading property file /var/jenkins/zappbuild_config/zappbuild.jenkins.properties
java.version=8.0.6.20 - pmz6480sr6fp20-20201120_02(SR6 FP20)
java.home=/V2R4/usr/lpp/java/J8.0_64
user.dir=/var/dbb/dbb-zappbuild
** Build properties at start up:
..... // lists of all build properties
** Repository client created for https://dbb-webapp:8080/dbb
** Build output located at /var/dbb/out/MortgageApplication/build.20210830.095350.053
** Build result created for BuildGroup:MortgageApplication-baselineBranch BuildLabel:build.20210830.095350.053 at https://dbb-webapp:8080/dbb/rest/buildResult/54806
** --impactBuild option selected. Building impacted programs for application MortgageApplication
** Getting current hash for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Storing MortgageApplication : 192adb8568b8179c7e537a339f1d8df7f2932f4a
** Getting baseline hash for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
*** Baseline hash for directory MortgageApplication retrieved from overwrite.
** Storing MortgageApplication : 6db56f7eecb420b56b69ca0ab7fcc2f1d9a7e5a8
** Calculating changed files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Diffing baseline 6db56f7eecb420b56b69ca0ab7fcc2f1d9a7e5a8 -> current 192adb8568b8179c7e537a339f1d8df7f2932f4a
*** Changed files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication:
**** MortgageApplication/cobol/epscmort.cbl
**** MortgageApplication/cobol/epsmpmt.cbl
!! (fixGitDiffPath) File not found.
*** Deleted files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication:
*** Renamed files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication:
** Updating collections MortgageApplication-baselineBranch and MortgageApplication-baselineBranch-outputs
*** Sorted list of changed files: [MortgageApplication/cobol/epsmpmt.cbl, MortgageApplication/cobol/epscmort.cbl]
*** Scanning file MortgageApplication/cobol/epsmpmt.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epsmpmt.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epsmpmt.cbl =
{"dli":false,"lname":"EPSMPMT","file":"MortgageApplication\/cobol\/epsmpmt.cbl","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSPDATA","library":"SYSLIB","category":"COPY"}],"language":"COB","sql":false}
*** Scanning file MortgageApplication/cobol/epscmort.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epscmort.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epscmort.cbl =
{"dli":false,"lname":"EPSCMORT","file":"MortgageApplication\/cobol\/epscmort.cbl","mq":false,"cics":true,"logicalDependencies":[{"lname":"DFHAID","library":"SYSLIB","category":"COPY"},{"lname":"EPSMORT","library":"SYSLIB","category":"COPY"},{"lname":"EPSMTCOM","library":"SYSLIB","category":"COPY"},{"lname":"EPSNBRPM","library":"SYSLIB","category":"COPY"},{"lname":"SQLCA","library":"SYSLIB","category":"SQL INCLUDE"}],"language":"COB","sql":true}
** Storing 2 logical files in repository collection 'MortgageApplication-baselineBranch'
HTTP/1.1 200 OK
*** Perform impacted analysis for changed files.
** Found build script mapping for MortgageApplication/cobol/epsmpmt.cbl. Adding to build list
** Performing impact analysis on changed file MortgageApplication/cobol/epsmpmt.cbl
*** Creating impact resolver for MortgageApplication/cobol/epsmpmt.cbl with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                },{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/bms"} ]             },{"category": "LINK", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/cobol"}, {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/link"} ]             },{"category": "PROPERTY"}] rules
** Found impacted file MortgageApplication/link/epsmlist.lnk
** MortgageApplication/link/epsmlist.lnk is impacted by changed file MortgageApplication/cobol/epsmpmt.cbl. Adding to build list.
** Found build script mapping for MortgageApplication/cobol/epscmort.cbl. Adding to build list
** Performing impact analysis on changed file MortgageApplication/cobol/epscmort.cbl
*** Creating impact resolver for MortgageApplication/cobol/epscmort.cbl with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                },{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/bms"} ]             },{"category": "LINK", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/cobol"}, {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/link"} ]             },{"category": "PROPERTY"}] rules
*** Perform impacted analysis for property changes.
** Writing build list file to /var/dbb/out/MortgageApplication/build.20210830.095350.053/buildList.txt
MortgageApplication/cobol/epsmpmt.cbl
MortgageApplication/cobol/epscmort.cbl
MortgageApplication/link/epsmlist.lnk
** Invoking build scripts according to build order: BMS.groovy,Cobol.groovy,LinkEdit.groovy
** Building files mapped to Cobol.groovy script
required props = cobol_srcPDS,cobol_cpyPDS,cobol_objPDS,cobol_loadPDS,cobol_compiler,cobol_linkEditor,cobol_tempOptions,applicationOutputsCollectionName,  SDFHCOB,SDFHLOAD,SDSNLOAD,SCEELKED
** Creating / verifying build dataset DBB.ZAPP.REL.COBOL
** Creating / verifying build dataset DBB.ZAPP.REL.COPY
** Creating / verifying build dataset DBB.ZAPP.REL.OBJ
** Creating / verifying build dataset DBB.ZAPP.REL.DBRM
** Creating / verifying build dataset DBB.ZAPP.REL.LOAD
*** Building file MortgageApplication/cobol/epsmpmt.cbl
*** Creating dependency resolver for MortgageApplication/cobol/epsmpmt.cbl with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                }] rules
*** Scanning file with the default scanner
*** Resolution rules for MortgageApplication/cobol/epsmpmt.cbl:
{"library":"SYSLIB","searchPath":[{"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","directory":"MortgageApplication\/copybook"}]}
*** Physical dependencies for MortgageApplication/cobol/epsmpmt.cbl:
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSPDATA","library":"SYSLIB","file":"MortgageApplication\/copybook\/epspdata.cpy","category":"COPY","resolved":true}
Cobol compiler parms for MortgageApplication/cobol/epsmpmt.cbl = LIB
*** Scanning load module for MortgageApplication/cobol/epsmpmt.cbl
*** Logical file =
{"dli":false,"lname":"EPSMPMT","file":"MortgageApplication\/cobol\/epsmpmt.cbl","mq":false,"cics":false,"language":"ZBND","sql":false}
*** Building file MortgageApplication/cobol/epscmort.cbl
*** Creating dependency resolver for MortgageApplication/cobol/epscmort.cbl with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                }] rules
*** Scanning file with the default scanner
*** Resolution rules for MortgageApplication/cobol/epscmort.cbl:
{"library":"SYSLIB","searchPath":[{"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","directory":"MortgageApplication\/copybook"}]}
*** Physical dependencies for MortgageApplication/cobol/epscmort.cbl:
{"excluded":false,"lname":"DFHAID","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"lname":"EPSMORT","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTINP","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtinp.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTOUT","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtout.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTCOM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtcom.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSNBRPM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsnbrpm.cpy","category":"COPY","resolved":true}
{"excluded":false,"lname":"SQLCA","library":"SYSLIB","category":"SQL INCLUDE","resolved":false}
Cobol compiler parms for MortgageApplication/cobol/epscmort.cbl = LIB,CICS,SQL
*** Scanning load module for MortgageApplication/cobol/epscmort.cbl
*** Logical file =
{"dli":false,"lname":"EPSCMORT","file":"MortgageApplication\/cobol\/epscmort.cbl","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSNBRVL","library":"DBB.ZAPP.REL.OBJ","category":"LINK"}],"language":"ZBND","sql":false}
** Building files mapped to LinkEdit.groovy script
required props = linkedit_srcPDS,linkedit_objPDS,linkedit_loadPDS,linkedit_linkEditor,linkedit_tempOptions,applicationOutputsCollectionName,  SDFHLOAD,SCEELKED
** Creating / verifying build dataset DBB.ZAPP.REL.LINK
** Creating / verifying build dataset DBB.ZAPP.REL.OBJ
** Creating / verifying build dataset DBB.ZAPP.REL.LOAD
*** Building file MortgageApplication/link/epsmlist.lnk
*** Creating dependency resolver for MortgageApplication/link/epsmlist.lnk with null rules
*** Scanning file with the default scanner
*** Scanning load module for MortgageApplication/link/epsmlist.lnk
*** Logical file =
{"dli":false,"lname":"EPSMLIST","file":"MortgageApplication\/link\/epsmlist.lnk","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSMPMT","library":"DBB.ZAPP.REL.LOAD","category":"LINK"},{"lname":"EPSMLIST","library":"DBB.ZAPP.REL.OBJ","category":"LINK"}],"language":"ZBND","sql":false}
*** Obtaining hash for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Setting property :githash:MortgageApplication : 192adb8568b8179c7e537a339f1d8df7f2932f4a
** Setting property :giturl:MortgageApplication : git@github.com:dennis-behm/dbb-zappbuild.git
** Setting property :gitchangedfiles:MortgageApplication : https://github.com/ibm/dbb-zappbuild/compare/192adb8568b8179c7e537a339f1d8df7f2932f4a..192adb8568b8179c7e537a339f1d8df7f2932f4a
** Writing build report data to /var/dbb/out/MortgageApplication/build.20210830.095350.053/BuildReport.json
** Writing build report to /var/dbb/out/MortgageApplication/build.20210830.095350.053/BuildReport.html
** Updating build result BuildGroup:MortgageApplication-baselineBranch BuildLabel:build.20210830.095350.053 at https://dbb-webapp:8080/dbb/rest/buildResult/54806
** Build ended at Mon Aug 30 09:53:59 GMT+01:00 2021
** Build State : CLEAN
** Total files processed : 3
** Total build time  : 8.546 seconds
```

</details>


### Perform a Merge build

`--mergeBuild` calculate the changes of a topic branch flowing back into the `mainBuildBranch` reference. This build type does not perform calculation of impacted files.

The scenario is targeting for builds on topic branches. The scope of the build is focussing on the outgoing changes. It is not incremental. Any time you invoke this build, it will the changes which will be merged to the target reference.

It leverages the git triple-dot diff syntax to identify the changes, similar to what can be seen in a pull/merge request.

In the below case both `MortgageApplication/cobol/epsmlist.cbl` and `MortgageApplication/copybook/epsnbrpm.cpy` are changed, but only the `epsmlist.cbl` is built because it is mapped to a build script.

```
groovyz dbb-zappbuild/build.groovy \
                      --workspace /var/dbb/dbb-zappbuild/samples \
                      --hlq DBB.ZAPP.CLEAN.MASTER \
                      --workDir /var/dbb/out/MortgageApplication \
                      --application MortgageApplication \
                      --logEncoding UTF-8 \
                      --mergeBuild \
                      --verbose
```
<details>
  <summary>Build log</summary>

```
+ /usr/lpp/dbb/v1r0/bin/groovyz /var/dbb/dbb-zappbuild/build.groovy --sourceDir /var/dbb/dbb-zappbuild/samples --workDir /var/dbb/out/MortgageApplication --hlq DBB.ZAPP.MERGE.BUILD --application MortgageApplication --verbose --mergeBuild --propFiles /var/dbb/dbb-zappbuild-config/build.properties,/var/dbb/dbb-zappbuild-config/datasets.properties

** Build start at 20211116.104234.042
** Input args = /var/dbb/dbb-zappbuild/samples --workDir /var/dbb/out/MortgageApplication --hlq DBB.ZAPP.MERGE.BUILD --application MortgageApplication --verbose --mergeBuild --propFiles /var/dbb/dbb-zappbuild-config/build.properties,/var/dbb/dbb-zappbuild-config/datasets.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/datasets.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/dependencyReport.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/Assembler.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/BMS.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/MFS.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/PSBgen.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/DBDgen.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/ACBgen.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/Cobol.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/LinkEdit.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/PLI.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/REXX.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/ZunitConfig.properties
** appConf = /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/file.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/BMS.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/Cobol.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/LinkEdit.properties
** Loading property file /var/dbb/dbb-zappbuild-config/build.properties
** Loading property file /var/dbb/dbb-zappbuild-config/datasets.properties
java.version=8.0.6.36 - pmz6480sr6fp36-20210913_01(SR6 FP36)
java.home=/V2R4/usr/lpp/java/J8.0_64
user.dir=/u/dbehm
** Build properties at start up:
..... // lists of all build properties
** Repository client created for https://10.3.20.96:10443/dbb
** Build output located at /var/dbb/out/MortgageApplication/build.20211116.104234.042
** Build result created for BuildGroup:MortgageApplication-outgoingChangesBuild BuildLabel:build.20211116.104234.042 at https://10.3.20.96:10443/dbb/rest/buildResult/58773
** --mergeBuild option selected. Building changed programs for application MortgageApplication flowing back to main
** Calculating changed files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Triple-dot Diffing configuration baseline main -> current HEAD
*** Changed files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication:
!! (fixGitDiffPath) File not found.
**** MortgageApplication/application-conf/application.properties
**** MortgageApplication/cobol/epsmlist.cbl
**** MortgageApplication/copybook/epsnbrpm.cpy
!! (fixGitDiffPath) File not found.
!! (fixGitDiffPath) File not found.
!! (fixGitDiffPath) File not found.
*** Deleted files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication:
*** Renamed files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication:
** Updating collections MortgageApplication-outgoingChangesBuild and MortgageApplication-outgoingChangesBuild-outputs
*** Sorted list of changed files: [MortgageApplication/cobol/epsmlist.cbl, MortgageApplication/copybook/epsnbrpm.cpy]
*** Scanning file MortgageApplication/cobol/epsmlist.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epsmlist.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epsmlist.cbl =
{"dli":false,"lname":"EPSMLIST","file":"MortgageApplication\/cobol\/epsmlist.cbl","mq":false,"cics":true,"logicalDependencies":[{"lname":"DFHAID","library":"SYSLIB","category":"COPY"},{"lname":"EPSMLIS","library":"SYSLIB","category":"COPY"},{"lname":"EPSMORTF","library":"SYSLIB","category":"COPY"},{"lname":"EPSMTCOM","library":"SYSLIB","category":"COPY"},{"lname":"EPSNBRPM","library":"SYSLIB","category":"COPY"}],"language":"COB","sql":false}
*** Scanning file MortgageApplication/copybook/epsnbrpm.cpy (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/epsnbrpm.cpy)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/epsnbrpm.cpy =
{"dli":false,"lname":"EPSNBRPM","file":"MortgageApplication\/copybook\/epsnbrpm.cpy","mq":false,"cics":false,"language":"COB","sql":false}
** Storing 2 logical files in repository collection 'MortgageApplication-outgoingChangesBuild'
HTTP/1.1 200 OK
** Found build script mapping for MortgageApplication/cobol/epsmlist.cbl. Adding to build list
** Writing build list file to /var/dbb/out/MortgageApplication/build.20211116.104234.042/buildList.txt
MortgageApplication/cobol/epsmlist.cbl
** Updating collections MortgageApplication-outgoingChangesBuild and MortgageApplication-outgoingChangesBuild-outputs
*** Scanning file MortgageApplication/cobol/epsmlist.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epsmlist.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epsmlist.cbl =
{"dli":false,"lname":"EPSMLIST","file":"MortgageApplication\/cobol\/epsmlist.cbl","mq":false,"cics":true,"logicalDependencies":[{"lname":"DFHAID","library":"SYSLIB","category":"COPY"},{"lname":"EPSMLIS","library":"SYSLIB","category":"COPY"},{"lname":"EPSMORTF","library":"SYSLIB","category":"COPY"},{"lname":"EPSMTCOM","library":"SYSLIB","category":"COPY"},{"lname":"EPSNBRPM","library":"SYSLIB","category":"COPY"}],"language":"COB","sql":false}
** Storing 1 logical files in repository collection 'MortgageApplication-outgoingChangesBuild'
HTTP/1.1 200 OK
** Invoking build scripts according to build order: BMS.groovy,Cobol.groovy,LinkEdit.groovy
** Building files mapped to Cobol.groovy script
required props = cobol_srcPDS,cobol_cpyPDS,cobol_objPDS,cobol_loadPDS,cobol_compiler,cobol_linkEditor,cobol_tempOptions,applicationOutputsCollectionName,  SDFHCOB,SDFHLOAD,SDSNLOAD,SCEELKED
** Creating / verifying build dataset DBB.ZAPP.MERGE.BUILD.COBOL
** Creating / verifying build dataset DBB.ZAPP.MERGE.BUILD.COPY
** Creating / verifying build dataset DBB.ZAPP.MERGE.BUILD.OBJ
** Creating / verifying build dataset DBB.ZAPP.MERGE.BUILD.DBRM
** Creating / verifying build dataset DBB.ZAPP.MERGE.BUILD.LOAD
*** Building file MortgageApplication/cobol/epsmlist.cbl
*** Creating dependency resolver for MortgageApplication/cobol/epsmlist.cbl with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                }] rules
*** Scanning file with the default scanner
*** Resolution rules for MortgageApplication/cobol/epsmlist.cbl:
{"library":"SYSLIB","searchPath":[{"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","directory":"MortgageApplication\/copybook"}]}
*** Physical dependencies for MortgageApplication/cobol/epsmlist.cbl:
{"excluded":false,"lname":"DFHAID","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"lname":"EPSMLIS","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMORTF","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmortf.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTINP","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtinp.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTOUT","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtout.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTCOM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtcom.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSNBRPM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsnbrpm.cpy","category":"COPY","resolved":true}
Cobol compiler parms for MortgageApplication/cobol/epsmlist.cbl = LIB,CICS
*** Obtaining hash for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Setting property :githash:MortgageApplication : d03087c5e4583be84cbe5c03a5fc7113074f46d2
** Setting property :giturl:MortgageApplication : https://github.com/dennis-behm/dbb-zappbuild.git
** Writing build report data to /var/dbb/out/MortgageApplication/build.20211116.104234.042/BuildReport.json
** Writing build report to /var/dbb/out/MortgageApplication/build.20211116.104234.042/BuildReport.html
** Updating build result BuildGroup:MortgageApplication-outgoingChangesBuild BuildLabel:build.20211116.104234.042 at https://10.3.20.96:10443/dbb/rest/buildResult/58773
** Build ended at Tue Nov 16 22:42:40 GMT+01:00 2021
** Build State : CLEAN
** Total files processed : 1
** Total build time  : 5.468 seconds
```


</details>

### Perform a Build in Preview Mode

`--preview` is a supplemental option to the various build types of zAppBuild. This supplemental option will let the build process run through all the build phases of the specified build option, but instead of executing the build commands such as copying the source files to the datasets or invoking the compile and link commands, it just documents what would be done and sets the return codes of these commands to 0. Please note, that file are scanned and, depending on the primary build option, dependency information is stored in the DBB Metadatastore.

For instance, use the `--preview` flag with the `--impactBuild` option to obtain a preview of the impact build actions such as identified changed files, the calculated impacted files, the build list, the build flow, the applied build properties and option including the outputs which would be produced.

Use the `--preview` flag with the `--fullBuild` option to produce the full bill of material (documented in a build report) for the artifacts that could be generated in the datasets pointed by the `hlq` parameter.

The build will generate a build report, which, depending of the provided build option, will be stored in the build group. However, the build result status is set to `4` and does not impact the calculation of changed file of subsequent impact builds.

The below sample build log is documenting an `--impactBuild --preview` with the reporting capablities activated to what the build would do and any potential conflicts of concurrent development activities.

```
groovyz dbb-zappbuild/build.groovy \
                      --workspace /var/dbb/dbb-zappbuild/samples \
                      --hlq DBB.ZAPP.CLEAN.MASTER \
                      --workDir /var/dbb/out/MortgageApplication \
                      --application MortgageApplication \
                      --logEncoding UTF-8 \
                      --impactBuild \
                      --preview \
                      --verbose
```
<details>
  <summary>Build log</summary>

```
+ /usr/lpp/dbb/v2r0/bin/groovyz /var/dbb/dbb-zappbuild/build.groovy --sourceDir /var/dbb/dbb-zappbuild/samples --workDir /var/dbb/work/mortgageout --url jdbc:db2://10.3.20.201:4740/MOPDBC0 --id DB2ID --pwFile /var/dbb/config/db2-pwd-file.xml --hlq DBEHM.DBB.BUILD --application MortgageApplication --verbose --impactBuild --preview --propFiles /var/dbb/dbb-zappbuild-config/build.properties,/var/dbb/dbb-zappbuild-config/datasets.properties

** Build start at 20230425.040722.007
** Input args = /var/dbb/dbb-zappbuild/samples --workDir /var/dbb/work/mortgageout --url jdbc:db2://10.3.20.201:4740/MOPDBC0 --id DB2ID --pwFile /var/dbb/config/db2-pwd-file.xml --hlq DBEHM.DBB.BUILD --application MortgageApplication --verbose --impactBuild --preview --propFiles /var/dbb/dbb-zappbuild-config/build.properties,/var/dbb/dbb-zappbuild-config/datasets.properties,/var/dbb/dbb-zappbuild-config/dbehm.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/datasets.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/dependencyReport.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/Assembler.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/BMS.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/MFS.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/PSBgen.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/DBDgen.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/ACBgen.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/Cobol.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/LinkEdit.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/PLI.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/REXX.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/ZunitConfig.properties
** Loading property file /var/dbb/dbb-zappbuild/build-conf/Transfer.properties
** appConf = /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/file.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/BMS.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/Cobol.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/LinkEdit.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/languageConfigurationMapping.properties
java.version=8.0.7.20 - pmz6480sr7fp20-20221020_01(SR7 FP20)
java.home=/V2R4/usr/lpp/java/J8.0_64
user.dir=/u/builduser
** Build properties at start up:
...
preview=true
...
** zAppBuild running on DBB Toolkit Version 2.0.0 20-Mar-2023 10:36:28
required props = buildOrder,buildListFileExt
** Running in reportOnly mode. Will process build options but not execute any steps.
** Db2 MetadataStore initialized
** Build output located at /var/dbb/work/mortgageout/build.20230425.160722.007
** Build result created for BuildGroup:MortgageApplication-350_preview_builds BuildLabel:build.20230425.160722.007
** --preview cli option provided. Processing all phases of the supplied build option, but will not execute the commands.
** --impactBuild option selected. Building impacted programs for application MortgageApplication
** Getting current hash for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Storing MortgageApplication : 2b3add1e85a8124ff1d7af6ab1de2e5463325d7a
** Getting baseline hash for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Storing MortgageApplication : cf6bc732bd717b404c5cf71a8f8d14458138a2d0
** Calculating changed files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Diffing baseline cf6bc732bd717b404c5cf71a8f8d14458138a2d0 -> current 2b3add1e85a8124ff1d7af6ab1de2e5463325d7a
*** Changed files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication :
**** MortgageApplication/jcl/MYSAMP.jcl
**** MortgageApplication/copybook/epsmtcom.cpy
*** Deleted files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication :
*** Renamed files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication :
** Updating collections MortgageApplication-350_preview_builds and MortgageApplication-350_preview_builds-outputs
*** Sorted list of changed files: [MortgageApplication/jcl/MYSAMP.jcl, MortgageApplication/copybook/epsmtcom.cpy]
*** Scanning file MortgageApplication/jcl/MYSAMP.jcl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/MYSAMP.jcl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/MYSAMP.jcl =
{
   "cics": false,
   "dli": false,
   "file": "MortgageApplication\/jcl\/MYSAMP.jcl",
   "language": "JCL",
   "lname": "MYSAMP",
   "logicalDependencies": [
      {
         "category": "COPY",
         "library": "SYSLIB",
         "lname": "EPSMTINP"
      },
      {
         "category": "COPY",
         "library": "SYSLIB",
         "lname": "EPSMTOUT"
      }
   ],
   "mq": false,
   "sql": false
}
*** Scanning file MortgageApplication/copybook/epsmtcom.cpy (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/epsmtcom.cpy)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/epsmtcom.cpy =
{
   "cics": false,
   "dli": false,
   "file": "MortgageApplication\/copybook\/epsmtcom.cpy",
   "language": "COB",
   "lname": "EPSMTCOM",
   "logicalDependencies": [
      {
         "category": "COPY",
         "library": "SYSLIB",
         "lname": "EPSMTINP"
      },
      {
         "category": "COPY",
         "library": "SYSLIB",
         "lname": "EPSMTOUT"
      }
   ],
   "mq": false,
   "sql": false
}
** Storing 2 logical files in MetadataStore collection 'MortgageApplication-350_preview_builds'
*** Perform impacted analysis for changed files.
** Found build script mapping for MortgageApplication/jcl/MYSAMP.jcl. Adding to build list
** Performing impact analysis on changed file MortgageApplication/jcl/MYSAMP.jcl
*** Creating SearchPathImpactFinder with collections [MortgageApplication-350_preview_builds, MortgageApplication-350_preview_builds-outputs] and impactSearch configuration search:/var/dbb/dbb-zappbuild/samples/?path=MortgageApplication/copybook/*.cpysearch:/var/dbb/dbb-zappbuild/samples/?path=MortgageApplication/bms/*.bmssearch:[:LINK]/var/dbb/dbb-zappbuild/samples/?path=MortgageApplication/cobol/*.cbl
** Performing impact analysis on changed file MortgageApplication/copybook/epsmtcom.cpy
*** Creating SearchPathImpactFinder with collections [MortgageApplication-350_preview_builds, MortgageApplication-350_preview_builds-outputs] and impactSearch configuration search:/var/dbb/dbb-zappbuild/samples/?path=MortgageApplication/copybook/*.cpysearch:/var/dbb/dbb-zappbuild/samples/?path=MortgageApplication/bms/*.bmssearch:[:LINK]/var/dbb/dbb-zappbuild/samples/?path=MortgageApplication/cobol/*.cbl
** Found impacted file MortgageApplication/cobol/epscmort.cbl
** MortgageApplication/cobol/epscmort.cbl is impacted by changed file MortgageApplication/copybook/epsmtcom.cpy. Adding to build list.
** Found impacted file MortgageApplication/cobol/epscsmrt.cbl
** MortgageApplication/cobol/epscsmrt.cbl is impacted by changed file MortgageApplication/copybook/epsmtcom.cpy. Adding to build list.
** Found impacted file MortgageApplication/cobol/epsmlist.cbl
** MortgageApplication/cobol/epsmlist.cbl is impacted by changed file MortgageApplication/copybook/epsmtcom.cpy. Adding to build list.
** Found impacted file MortgageApplication/cobol/epscsmrt.cbl
** MortgageApplication/cobol/epscsmrt.cbl is impacted by changed file MortgageApplication/copybook/epsmtcom.cpy. Adding to build list.
** Found impacted file MortgageApplication/cobol/epscmort.cbl
** MortgageApplication/cobol/epscmort.cbl is impacted by changed file MortgageApplication/copybook/epsmtcom.cpy. Adding to build list.
** Found impacted file MortgageApplication/link/epsmlist.lnk
** MortgageApplication/link/epsmlist.lnk is impacted by changed file MortgageApplication/copybook/epsmtcom.cpy. Adding to build list.
** Calculation of impacted files by changed properties has been skipped due to configuration.
** Writing build list file to /var/dbb/work/mortgageout/build.20230425.160722.007/buildList.txt
MortgageApplication/cobol/epsmlist.cbl
MortgageApplication/cobol/epscsmrt.cbl
MortgageApplication/cobol/epscmort.cbl
MortgageApplication/link/epsmlist.lnk
MortgageApplication/jcl/MYSAMP.jcl
** Populating file level properties from individual artifact properties files.
* Populating file level properties overrides.
** Checking file property overrides for MortgageApplication/cobol/epsmlist.cbl
*** MortgageApplication/cobol/epsmlist.cbl has an individual artifact properties file defined in properties/epsmlist.cbl.properties
    Found file property cobol_compileParms = ${cobol_compileParms},SOURCE
*** Checking for existing file property overrides
    Checking build property cobol_compileParms
    Adding file property override cobol_compileParms = ${cobol_compileParms},SOURCE for MortgageApplication/cobol/epsmlist.cbl
** Checking file property overrides for MortgageApplication/cobol/epscsmrt.cbl
*** Checking for existing file property overrides
** Checking file property overrides for MortgageApplication/cobol/epscmort.cbl
*** Checking for existing file property overrides
** Checking file property overrides for MortgageApplication/link/epsmlist.lnk
*** Checking for existing file property overrides
** Checking file property overrides for MortgageApplication/jcl/MYSAMP.jcl
*** Checking for existing file property overrides
** Perform analysis and reporting of external impacted files for the build list including changed files.
*** Running external impact analysis with file filter **/* and collection patterns .*-main.* with analysis mode deep
*** Running external impact analysis for files
     MortgageApplication/cobol/epscmort.cbl
     MortgageApplication/cobol/epsmlist.cbl
     MortgageApplication/link/epsmlist.lnk
     MortgageApplication/jcl/MYSAMP.jcl
     MortgageApplication/cobol/epscsmrt.cbl
     MortgageApplication/copybook/epsmtcom.cpy
*** Writing report of external impacts to file /var/dbb/work/mortgageout/build.20230425.160722.007/externalImpacts_MortgageApplication-main-outputs.log
*** Potential external impact found EPSCSMRT (MortgageApplication/cobol/epscsmrt.cbl) in collection MortgageApplication-main-outputs
*** Potential external impact found EPSCMORT (MortgageApplication/cobol/epscmort.cbl) in collection MortgageApplication-main-outputs
*** Potential external impact found EPSMLIST (MortgageApplication/link/epsmlist.lnk) in collection MortgageApplication-main-outputs
*** Writing report of external impacts to file /var/dbb/work/mortgageout/build.20230425.160722.007/externalImpacts_MortgageApplication-main-patch.log
*** Potential external impact found EPSCSMRT (MortgageApplication/cobol/epscsmrt.cbl) in collection MortgageApplication-main-patch
*** Potential external impact found EPSMLIST (MortgageApplication/cobol/epsmlist.cbl) in collection MortgageApplication-main-patch
*** Potential external impact found EPSCMORT (MortgageApplication/cobol/epscmort.cbl) in collection MortgageApplication-main-patch
*** Writing report of external impacts to file /var/dbb/work/mortgageout/build.20230425.160722.007/externalImpacts_MortgageApplication-main.log
*** Potential external impact found EPSCSMRT (MortgageApplication/cobol/epscsmrt.cbl) in collection MortgageApplication-main
*** Potential external impact found EPSMLIST (MortgageApplication/cobol/epsmlist.cbl) in collection MortgageApplication-main
*** Potential external impact found EPSCMORT (MortgageApplication/cobol/epscmort.cbl) in collection MortgageApplication-main
*** Writing report of external impacts to file /var/dbb/work/mortgageout/build.20230425.160722.007/externalImpacts_MortgageApplication-main-patch-outputs.log
*** Potential external impact found EPSMLIST (MortgageApplication/link/epsmlist.lnk) in collection MortgageApplication-main-patch-outputs
**** Running external impact analysis for identified external impacted files as dependent files of the initial set.
     MortgageApplication/cobol/epscsmrt.cbl
     MortgageApplication/cobol/epscmort.cbl
     MortgageApplication/link/epsmlist.lnk
     MortgageApplication/cobol/epscsmrt.cbl
     MortgageApplication/cobol/epsmlist.cbl
     MortgageApplication/cobol/epscmort.cbl
     MortgageApplication/cobol/epscsmrt.cbl
     MortgageApplication/cobol/epsmlist.cbl
     MortgageApplication/cobol/epscmort.cbl
     MortgageApplication/link/epsmlist.lnk
** Calculate and document concurrent changes.
***  Analysing and validating changes for branch : main
** Getting current hash for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Storing MortgageApplication : 2b3add1e85a8124ff1d7af6ab1de2e5463325d7a
** Calculating changed files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
*** Changed files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication in configuration main:
**** MortgageApplication/cobol/epscmort.cbl
*** Deleted files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication in configuration main:
*** Renamed files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication in configuration main:
** Writing report of concurrent changes to /var/dbb/work/mortgageout/build.20230425.160722.007/report_concurrentChanges.txt for configuration main
 Changed: MortgageApplication/cobol/epscmort.cbl
*!! MortgageApplication/cobol/epscmort.cbl is changed on branch main and intersects with the current build list.
** Invoking build scripts according to build order: BMS.groovy,Cobol.groovy,LinkEdit.groovy,Transfer.groovy
** Building files mapped to Cobol.groovy script
required props = cobol_srcPDS,cobol_cpyPDS,cobol_objPDS,cobol_loadPDS,cobol_compiler,cobol_linkEditor,cobol_tempOptions,applicationOutputsCollectionName,SDFHCOB,SDFHLOAD,SDSNLOAD,SCEELKED,   cobol_dependencySearch
** Creating / verifying build dataset DBEHM.DBB.BUILD.COBOL
** Creating / verifying build dataset DBEHM.DBB.BUILD.COPY
** Creating / verifying build dataset DBEHM.DBB.BUILD.OBJ
** Creating / verifying build dataset DBEHM.DBB.BUILD.DBRM
** Creating / verifying build dataset DBEHM.DBB.BUILD.LOAD
*** Building file MortgageApplication/cobol/epsmlist.cbl
*** Resolution rules for MortgageApplication/cobol/epsmlist.cbl:
search:/var/dbb/dbb-zappbuild/samples/?path=MortgageApplication/copybook/*.cpy
*** Physical dependencies for MortgageApplication/cobol/epsmlist.cbl:
{"excluded":false,"lname":"DFHAID","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"lname":"EPSMLIS","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"sourceDir":"\/u\/dbehm\/test-zapp\/dbb-zappbuild\/samples\/","lname":"EPSMORTF","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmortf.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/u\/dbehm\/test-zapp\/dbb-zappbuild\/samples\/","lname":"EPSMTCOM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtcom.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/u\/dbehm\/test-zapp\/dbb-zappbuild\/samples\/","lname":"EPSMTINP","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtinp.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/u\/dbehm\/test-zapp\/dbb-zappbuild\/samples\/","lname":"EPSMTOUT","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtout.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/u\/dbehm\/test-zapp\/dbb-zappbuild\/samples\/","lname":"EPSNBRPM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsnbrpm.cpy","category":"COPY","resolved":true}
Program attributes: CICS=true, SQL=false, DLI=false, MQ=false
Cobol compiler parms for MortgageApplication/cobol/epsmlist.cbl = LIB,SOURCE,CICS
Link-Edit parms for MortgageApplication/cobol/epsmlist.cbl = MAP,RENT,COMPAT(PM5),SSI=2b3add1e
*** Building file MortgageApplication/cobol/epscsmrt.cbl
*** Resolution rules for MortgageApplication/cobol/epscsmrt.cbl:
search:/var/dbb/dbb-zappbuild/samples/?path=MortgageApplication/copybook/*.cpy
*** Physical dependencies for MortgageApplication/cobol/epscsmrt.cbl:
{"excluded":false,"sourceDir":"\/u\/dbehm\/test-zapp\/dbb-zappbuild\/samples\/","lname":"EPSMTCOM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtcom.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/u\/dbehm\/test-zapp\/dbb-zappbuild\/samples\/","lname":"EPSMTINP","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtinp.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/u\/dbehm\/test-zapp\/dbb-zappbuild\/samples\/","lname":"EPSMTOUT","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtout.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/u\/dbehm\/test-zapp\/dbb-zappbuild\/samples\/","lname":"EPSPDATA","library":"SYSLIB","file":"MortgageApplication\/copybook\/epspdata.cpy","category":"COPY","resolved":true}
Program attributes: CICS=true*, SQL=false, DLI=false, MQ=false
Cobol compiler parms for MortgageApplication/cobol/epscsmrt.cbl = LIB,CICS
Link-Edit parms for MortgageApplication/cobol/epscsmrt.cbl = MAP,RENT,COMPAT(PM5),SSI=2b3add1e
*** Scanning load module for MortgageApplication/cobol/epscsmrt.cbl
*** Logical file =
{
   "cics": false,
   "dli": false,
   "file": "MortgageApplication\/cobol\/epscsmrt.cbl",
   "language": "ZBND",
   "lname": "EPSCSMRT",
   "logicalDependencies": [
      {
         "category": "LINK",
         "library": "DBEHM.DBB.BUILD.OBJ",
         "lname": "EPSCSMRT"
      }
   ],
   "mq": false,
   "sql": false
}
*** Building file MortgageApplication/cobol/epscmort.cbl
*** Resolution rules for MortgageApplication/cobol/epscmort.cbl:
search:/var/dbb/dbb-zappbuild/samples/?path=MortgageApplication/copybook/*.cpy
*** Physical dependencies for MortgageApplication/cobol/epscmort.cbl:
{"excluded":false,"lname":"DFHAID","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"lname":"EPSMORT","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"sourceDir":"\/u\/dbehm\/test-zapp\/dbb-zappbuild\/samples\/","lname":"EPSMTCOM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtcom.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/u\/dbehm\/test-zapp\/dbb-zappbuild\/samples\/","lname":"EPSMTINP","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtinp.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/u\/dbehm\/test-zapp\/dbb-zappbuild\/samples\/","lname":"EPSMTOUT","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtout.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/u\/dbehm\/test-zapp\/dbb-zappbuild\/samples\/","lname":"EPSNBRPM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsnbrpm.cpy","category":"COPY","resolved":true}
{"excluded":false,"lname":"SQLCA","library":"SYSLIB","category":"SQL INCLUDE","resolved":false}
Program attributes: CICS=true, SQL=true, DLI=false, MQ=false
Cobol compiler parms for MortgageApplication/cobol/epscmort.cbl = LIB,CICS,SQL
Link-Edit parms for MortgageApplication/cobol/epscmort.cbl = MAP,RENT,COMPAT(PM5),SSI=2b3add1e
*** Scanning load module for MortgageApplication/cobol/epscmort.cbl
*** Logical file =
{
   "cics": false,
   "dli": false,
   "file": "MortgageApplication\/cobol\/epscmort.cbl",
   "language": "ZBND",
   "lname": "EPSCMORT",
   "logicalDependencies": [
      {
         "category": "LINK",
         "library": "DBEHM.DBB.BUILD.OBJ",
         "lname": "EPSCMORT"
      },
      {
         "category": "LINK",
         "library": "DBEHM.DBB.BUILD.OBJ",
         "lname": "EPSNBRVL"
      }
   ],
   "mq": false,
   "sql": false
}
** Building files mapped to LinkEdit.groovy script
required props = linkedit_srcPDS,linkedit_objPDS,linkedit_loadPDS,linkedit_linkEditor,linkedit_tempOptions,applicationOutputsCollectionName,  SDFHLOAD,SCEELKED
** Creating / verifying build dataset DBEHM.DBB.BUILD.LINK
** Creating / verifying build dataset DBEHM.DBB.BUILD.OBJ
** Creating / verifying build dataset DBEHM.DBB.BUILD.LOAD
*** Building file MortgageApplication/link/epsmlist.lnk
Link-Edit parms for MortgageApplication/link/epsmlist.lnk = MAP,RENT,COMPAT(PM5),SSI=2b3add1e
*** Scanning load module for MortgageApplication/link/epsmlist.lnk
*** Logical file =
{
   "cics": false,
   "dli": false,
   "file": "MortgageApplication\/link\/epsmlist.lnk",
   "language": "ZBND",
   "lname": "EPSMLIST",
   "logicalDependencies": [
      {
         "category": "LINK",
         "library": "DBEHM.DBB.BUILD.LOAD",
         "lname": "EPSMPMT"
      },
      {
         "category": "LINK",
         "library": "DBEHM.DBB.BUILD.OBJ",
         "lname": "EPSMLIST"
      }
   ],
   "mq": false,
   "sql": false
}
** Building files mapped to Transfer.groovy script
required props = transfer_srcPDS,transfer_dsOptions,  transfer_deployType
*** Transferring file MortgageApplication/jcl/MYSAMP.jcl
** Creating / verifying build dataset DBEHM.DBB.BUILD.JCL
** Copied MortgageApplication/jcl/MYSAMP.jcl to DBEHM.DBB.BUILD.JCL with deployType JCL (rc = 0)
*** Obtaining hash for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Setting property :githash:MortgageApplication : 2b3add1e85a8124ff1d7af6ab1de2e5463325d7a
** Setting property :giturl:MortgageApplication : https://github.com/dennis-behm/dbb-zappbuild.git
** Setting property :gitchangedfiles:MortgageApplication : https://github.com/ibm/dbb-zappbuild/compare/cf6bc732bd717b404c5cf71a8f8d14458138a2d0..2b3add1e85a8124ff1d7af6ab1de2e5463325d7a
** Writing build report data to /var/dbb/work/mortgageout/build.20230425.160722.007/BuildReport.json
** Writing build report to /var/dbb/work/mortgageout/build.20230425.160722.007/BuildReport.html
** Updating build result BuildGroup:MortgageApplication-350_preview_builds BuildLabel:build.20230425.160722.007
** Build ended at Tue Apr 25 16:07:34 GMT+01:00 2023
** Build State : CLEAN
** Build ran in preview mode.
** Total files processed : 5
** Total build time  : 12.204 seconds

** Build finished
```


</details>

### Perform a Scan Source build

`--fullBuild --scanSource` skips the actual building and only scan source files to store dependency data in the collection (migration scenario). Please be aware that it scans all programs including the copybooks, which is required to perform proper impact analysis.

This build type also stores a build result to build a baseline for following impact builds.

```
groovyz dbb-zappbuild/build.groovy \
                      --workspace /var/dbb/dbb-zappbuild/samples \
                      --hlq DBB.ZAPP.CLEAN.MASTER \
                      --workDir /var/dbb/out/MortgageApplication \
                      --application MortgageApplication \
                      --logEncoding UTF-8 \
                      --fullBuild \
                      --scanSource \
                      --verbose
```
<details>
  <summary>Build log</summary>

```
** Build start at 20210622.104821.048
** Input args = /var/dbb/dbb-zappbuild/samples --hlq DBB.ZAPP.CLEAN.MASTER --workDir /var/dbb/out/MortgageApplication --application MortgageApplication --logEncoding UTF-8 --fullBuild --scanSource --verbose
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/datasets.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/Assembler.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/BMS.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/MFS.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/PSBgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/DBDgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/ACBgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/Cobol.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/LinkEdit.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/PLI.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/ZunitConfig.properties
** appConf = /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/file.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/BMS.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/Cobol.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/LinkEdit.properties
java.version=8.0.6.20 - pmz6480sr6fp20-20201120_02(SR6 FP20)
java.home=/V2R4/usr/lpp/java/J8.0_64
user.dir=/ZT01/var/dbb
** Build properties at start up:
..... // lists of all build properties
** Repository client created for https://dbb-webapp:8080/dbb
** Build output located at /var/dbb/out/MortgageApplication/build.20210622.104821.048
** Build result created for BuildGroup:MortgageApplication-master BuildLabel:build.20210622.104821.048 at https://dbb-webapp:8080/dbb/rest/buildResult/47074
** --fullBuild option selected. Scanning all programs for application MortgageApplication
** Writing build list file to /var/dbb/out/MortgageApplication/build.20210622.104821.048/buildList.txt
MortgageApplication/copybook/epsmtout.cpy
MortgageApplication/cobol/epsnbrvl.cbl
MortgageApplication/cobol/epscsmrt.cbl
MortgageApplication/bms/epsmort.bms
MortgageApplication/link/epsmlist.lnk
MortgageApplication/copybook/epsmortf.cpy
MortgageApplication/copybook/epsnbrpm.cpy
MortgageApplication/bms/epsmlis.bms
MortgageApplication/copybook/epsmtcom.cpy
MortgageApplication/cobol/epsmlist.cbl
MortgageApplication/copybook/epsmtinp.cpy
MortgageApplication/copybook/epspdata.cpy
MortgageApplication/cobol/epsmpmt.cbl
MortgageApplication/cobol/epscmort.cbl
MortgageApplication/cobol/epscsmrd.cbl
** Scanning source code.
** Updating collections MortgageApplication-master and MortgageApplication-master-outputs
*** Scanning file MortgageApplication/copybook/epsmtout.cpy (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/epsmtout.cpy)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/epsmtout.cpy =
{"dli":false,"lname":"EPSMTOUT","file":"MortgageApplication\/copybook\/epsmtout.cpy","mq":false,"cics":false,"language":"COB","sql":false}
*** Scanning file MortgageApplication/cobol/epsnbrvl.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epsnbrvl.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epsnbrvl.cbl =
{"dli":false,"lname":"EPSNBRVL","file":"MortgageApplication\/cobol\/epsnbrvl.cbl","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSNBRPM","library":"SYSLIB","category":"COPY"}],"language":"COB","sql":false}
*** Scanning file MortgageApplication/cobol/epscsmrt.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epscsmrt.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epscsmrt.cbl =
{"dli":false,"lname":"EPSCSMRT","file":"MortgageApplication\/cobol\/epscsmrt.cbl","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSMTCOM","library":"SYSLIB","category":"COPY"},{"lname":"EPSPDATA","library":"SYSLIB","category":"COPY"}],"language":"COB","sql":false}
*** Scanning file MortgageApplication/bms/epsmort.bms (/var/dbb/dbb-zappbuild/samples/MortgageApplication/bms/epsmort.bms)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/bms/epsmort.bms =
{"dli":false,"lname":"EPSMORT","file":"MortgageApplication\/bms\/epsmort.bms","mq":false,"cics":false,"language":"ASM","sql":false}
*** Scanning file MortgageApplication/link/epsmlist.lnk (/var/dbb/dbb-zappbuild/samples/MortgageApplication/link/epsmlist.lnk)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/link/epsmlist.lnk =
{"dli":false,"lname":"EPSMLIST","file":"MortgageApplication\/link\/epsmlist.lnk","mq":false,"cics":false,"language":"ZBND","sql":false}
*** Scanning file MortgageApplication/copybook/epsmortf.cpy (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/epsmortf.cpy)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/epsmortf.cpy =
{"dli":false,"lname":"EPSMORTF","file":"MortgageApplication\/copybook\/epsmortf.cpy","mq":false,"cics":false,"language":"COB","sql":false}
*** Scanning file MortgageApplication/copybook/epsnbrpm.cpy (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/epsnbrpm.cpy)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/epsnbrpm.cpy =
{"dli":false,"lname":"EPSNBRPM","file":"MortgageApplication\/copybook\/epsnbrpm.cpy","mq":false,"cics":false,"language":"COB","sql":false}
*** Scanning file MortgageApplication/bms/epsmlis.bms (/var/dbb/dbb-zappbuild/samples/MortgageApplication/bms/epsmlis.bms)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/bms/epsmlis.bms =
{"dli":false,"lname":"EPSMLIS","file":"MortgageApplication\/bms\/epsmlis.bms","mq":false,"cics":false,"language":"ASM","sql":false}
*** Scanning file MortgageApplication/copybook/epsmtcom.cpy (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/epsmtcom.cpy)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/epsmtcom.cpy =
{"dli":false,"lname":"EPSMTCOM","file":"MortgageApplication\/copybook\/epsmtcom.cpy","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSMTINP","library":"SYSLIB","category":"COPY"},{"lname":"EPSMTOUT","library":"SYSLIB","category":"COPY"}],"language":"COB","sql":false}
*** Scanning file MortgageApplication/cobol/epsmlist.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epsmlist.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epsmlist.cbl =
{"dli":false,"lname":"EPSMLIST","file":"MortgageApplication\/cobol\/epsmlist.cbl","mq":false,"cics":true,"logicalDependencies":[{"lname":"DFHAID","library":"SYSLIB","category":"COPY"},{"lname":"EPSMLIS","library":"SYSLIB","category":"COPY"},{"lname":"EPSMORTF","library":"SYSLIB","category":"COPY"},{"lname":"EPSMTCOM","library":"SYSLIB","category":"COPY"},{"lname":"EPSNBRPM","library":"SYSLIB","category":"COPY"}],"language":"COB","sql":false}
*** Scanning file MortgageApplication/copybook/epsmtinp.cpy (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/epsmtinp.cpy)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/epsmtinp.cpy =
{"dli":false,"lname":"EPSMTINP","file":"MortgageApplication\/copybook\/epsmtinp.cpy","mq":false,"cics":false,"language":"COB","sql":false}
*** Scanning file MortgageApplication/copybook/epspdata.cpy (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/epspdata.cpy)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/epspdata.cpy =
{"dli":false,"lname":"EPSPDATA","file":"MortgageApplication\/copybook\/epspdata.cpy","mq":false,"cics":false,"language":"COB","sql":false}
*** Scanning file MortgageApplication/cobol/epsmpmt.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epsmpmt.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epsmpmt.cbl =
{"dli":false,"lname":"EPSMPMT","file":"MortgageApplication\/cobol\/epsmpmt.cbl","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSPDATA","library":"SYSLIB","category":"COPY"}],"language":"COB","sql":false}
*** Scanning file MortgageApplication/cobol/epscmort.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epscmort.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epscmort.cbl =
{"dli":false,"lname":"EPSCMORT","file":"MortgageApplication\/cobol\/epscmort.cbl","mq":false,"cics":true,"logicalDependencies":[{"lname":"DFHAID","library":"SYSLIB","category":"COPY"},{"lname":"EPSMORT","library":"SYSLIB","category":"COPY"},{"lname":"EPSMTCOM","library":"SYSLIB","category":"COPY"},{"lname":"EPSNBRPM","library":"SYSLIB","category":"COPY"},{"lname":"SQLCA","library":"SYSLIB","category":"SQL INCLUDE"}],"language":"COB","sql":true}
*** Scanning file MortgageApplication/cobol/epscsmrd.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epscsmrd.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epscsmrd.cbl =
{"dli":false,"lname":"EPSCSMRD","file":"MortgageApplication\/cobol\/epscsmrd.cbl","mq":false,"cics":true,"language":"COB","sql":false}
** Storing 15 logical files in repository collection 'MortgageApplication-master'
HTTP/1.1 200 OK
*** Obtaining hash for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Setting property :githash:MortgageApplication : 857266a44a6e859c4f949adb7e32cfbc4a8bd736
** Setting property :giturl:MortgageApplication : git@github.ibm.com:zDevOps-Acceleration/dbb-zappbuild.git
** Writing build report data to /var/dbb/out/MortgageApplication/build.20210622.104821.048/BuildReport.json
** Writing build report to /var/dbb/out/MortgageApplication/build.20210622.104821.048/BuildReport.html
** Updating build result BuildGroup:MortgageApplication-master BuildLabel:build.20210622.104821.048 at https://dbb-webapp:8080/dbb/rest/buildResult/47074
** Build ended at Tue Jun 22 10:48:36 GMT+01:00 2021
** Build State : CLEAN
** Total files processed : 15
** Total build time  : 15.319 seconds

** Build finished
```

</details>

### Perform a Scan Source + Outputs build

`--fullBuild --scanAll` skips the actual building and only scan source files and existing load modules to dependency data in source and output collection (migration scenario with static linkage scenarios). This build type also stores a build result to build a baseline for following impact builds.

Please see also the [TechDoc for Advanced Build and Migration recipes](https://www.ibm.com/support/pages/node/6427617)

```
groovyz dbb-zappbuild/build.groovy \
                      --workspace /var/dbb/dbb-zappbuild/samples \
                      --hlq DBB.ZAPP.CLEAN.MASTER \
                      --workDir /var/dbb/out/MortgageApplication \
                      --application MortgageApplication \
                      --logEncoding UTF-8 \
                      --fullBuild \
                      --scanAll \
                      --verbose
```
<details>
  <summary>Build log</summary>

```
** Build start at 20210622.105915.059
** Input args = /var/dbb/dbb-zappbuild/samples --hlq DBB.ZAPP.CLEAN.MASTER --workDir /var/dbb/out/MortgageApplication --application MortgageApplication --logEncoding UTF-8 --fullBuild --scanAll --verbose
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/datasets.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/Assembler.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/BMS.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/MFS.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/PSBgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/DBDgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/ACBgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/Cobol.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/LinkEdit.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/PLI.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/ZunitConfig.properties
** appConf = /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/file.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/BMS.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/Cobol.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/LinkEdit.properties
java.version=8.0.6.20 - pmz6480sr6fp20-20201120_02(SR6 FP20)
java.home=/V2R4/usr/lpp/java/J8.0_64
user.dir=/ZT01/var/dbb
** Build properties at start up:
..... // lists of all build properties
** Repository client created for https://dbb-webapp:8080/dbb
** Build output located at /var/dbb/out/MortgageApplication/build.20210622.105915.059
** Build result created for BuildGroup:MortgageApplication-master BuildLabel:build.20210622.105915.059 at https://dbb-webapp:8080/dbb/rest/buildResult/47085
** Created collection MortgageApplication-master
** Created collection MortgageApplication-master-outputs
** --fullBuild option selected. Scanning all programs for application MortgageApplication
** Writing build list file to /var/dbb/out/MortgageApplication/build.20210622.105915.059/buildList.txt
MortgageApplication/copybook/epsmtout.cpy
MortgageApplication/cobol/epsnbrvl.cbl
MortgageApplication/cobol/epscsmrt.cbl
MortgageApplication/bms/epsmort.bms
MortgageApplication/link/epsmlist.lnk
MortgageApplication/copybook/epsmortf.cpy
MortgageApplication/copybook/epsnbrpm.cpy
MortgageApplication/bms/epsmlis.bms
MortgageApplication/copybook/epsmtcom.cpy
MortgageApplication/cobol/epsmlist.cbl
MortgageApplication/copybook/epsmtinp.cpy
MortgageApplication/copybook/epspdata.cpy
MortgageApplication/cobol/epsmpmt.cbl
MortgageApplication/cobol/epscmort.cbl
MortgageApplication/cobol/epscsmrd.cbl
** Scanning source code.
** Updating collections MortgageApplication-master and MortgageApplication-master-outputs
*** Scanning file MortgageApplication/copybook/epsmtout.cpy (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/epsmtout.cpy)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/epsmtout.cpy =
{"dli":false,"lname":"EPSMTOUT","file":"MortgageApplication\/copybook\/epsmtout.cpy","mq":false,"cics":false,"language":"COB","sql":false}
*** Scanning file MortgageApplication/cobol/epsnbrvl.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epsnbrvl.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epsnbrvl.cbl =
{"dli":false,"lname":"EPSNBRVL","file":"MortgageApplication\/cobol\/epsnbrvl.cbl","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSNBRPM","library":"SYSLIB","category":"COPY"}],"language":"COB","sql":false}
*** Scanning file MortgageApplication/cobol/epscsmrt.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epscsmrt.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epscsmrt.cbl =
{"dli":false,"lname":"EPSCSMRT","file":"MortgageApplication\/cobol\/epscsmrt.cbl","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSMTCOM","library":"SYSLIB","category":"COPY"},{"lname":"EPSPDATA","library":"SYSLIB","category":"COPY"}],"language":"COB","sql":false}
*** Scanning file MortgageApplication/bms/epsmort.bms (/var/dbb/dbb-zappbuild/samples/MortgageApplication/bms/epsmort.bms)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/bms/epsmort.bms =
{"dli":false,"lname":"EPSMORT","file":"MortgageApplication\/bms\/epsmort.bms","mq":false,"cics":false,"language":"ASM","sql":false}
*** Scanning file MortgageApplication/link/epsmlist.lnk (/var/dbb/dbb-zappbuild/samples/MortgageApplication/link/epsmlist.lnk)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/link/epsmlist.lnk =
{"dli":false,"lname":"EPSMLIST","file":"MortgageApplication\/link\/epsmlist.lnk","mq":false,"cics":false,"language":"ZBND","sql":false}
*** Scanning file MortgageApplication/copybook/epsmortf.cpy (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/epsmortf.cpy)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/epsmortf.cpy =
{"dli":false,"lname":"EPSMORTF","file":"MortgageApplication\/copybook\/epsmortf.cpy","mq":false,"cics":false,"language":"COB","sql":false}
*** Scanning file MortgageApplication/copybook/epsnbrpm.cpy (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/epsnbrpm.cpy)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/epsnbrpm.cpy =
{"dli":false,"lname":"EPSNBRPM","file":"MortgageApplication\/copybook\/epsnbrpm.cpy","mq":false,"cics":false,"language":"COB","sql":false}
*** Scanning file MortgageApplication/bms/epsmlis.bms (/var/dbb/dbb-zappbuild/samples/MortgageApplication/bms/epsmlis.bms)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/bms/epsmlis.bms =
{"dli":false,"lname":"EPSMLIS","file":"MortgageApplication\/bms\/epsmlis.bms","mq":false,"cics":false,"language":"ASM","sql":false}
*** Scanning file MortgageApplication/copybook/epsmtcom.cpy (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/epsmtcom.cpy)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/epsmtcom.cpy =
{"dli":false,"lname":"EPSMTCOM","file":"MortgageApplication\/copybook\/epsmtcom.cpy","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSMTINP","library":"SYSLIB","category":"COPY"},{"lname":"EPSMTOUT","library":"SYSLIB","category":"COPY"}],"language":"COB","sql":false}
*** Scanning file MortgageApplication/cobol/epsmlist.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epsmlist.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epsmlist.cbl =
{"dli":false,"lname":"EPSMLIST","file":"MortgageApplication\/cobol\/epsmlist.cbl","mq":false,"cics":true,"logicalDependencies":[{"lname":"DFHAID","library":"SYSLIB","category":"COPY"},{"lname":"EPSMLIS","library":"SYSLIB","category":"COPY"},{"lname":"EPSMORTF","library":"SYSLIB","category":"COPY"},{"lname":"EPSMTCOM","library":"SYSLIB","category":"COPY"},{"lname":"EPSNBRPM","library":"SYSLIB","category":"COPY"}],"language":"COB","sql":false}
*** Scanning file MortgageApplication/copybook/epsmtinp.cpy (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/epsmtinp.cpy)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/epsmtinp.cpy =
{"dli":false,"lname":"EPSMTINP","file":"MortgageApplication\/copybook\/epsmtinp.cpy","mq":false,"cics":false,"language":"COB","sql":false}
*** Scanning file MortgageApplication/copybook/epspdata.cpy (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/epspdata.cpy)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/epspdata.cpy =
{"dli":false,"lname":"EPSPDATA","file":"MortgageApplication\/copybook\/epspdata.cpy","mq":false,"cics":false,"language":"COB","sql":false}
*** Scanning file MortgageApplication/cobol/epsmpmt.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epsmpmt.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epsmpmt.cbl =
{"dli":false,"lname":"EPSMPMT","file":"MortgageApplication\/cobol\/epsmpmt.cbl","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSPDATA","library":"SYSLIB","category":"COPY"}],"language":"COB","sql":false}
*** Scanning file MortgageApplication/cobol/epscmort.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epscmort.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epscmort.cbl =
{"dli":false,"lname":"EPSCMORT","file":"MortgageApplication\/cobol\/epscmort.cbl","mq":false,"cics":true,"logicalDependencies":[{"lname":"DFHAID","library":"SYSLIB","category":"COPY"},{"lname":"EPSMORT","library":"SYSLIB","category":"COPY"},{"lname":"EPSMTCOM","library":"SYSLIB","category":"COPY"},{"lname":"EPSNBRPM","library":"SYSLIB","category":"COPY"},{"lname":"SQLCA","library":"SYSLIB","category":"SQL INCLUDE"}],"language":"COB","sql":true}
*** Scanning file MortgageApplication/cobol/epscsmrd.cbl (/var/dbb/dbb-zappbuild/samples/MortgageApplication/cobol/epscsmrd.cbl)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/cobol/epscsmrd.cbl =
{"dli":false,"lname":"EPSCSMRD","file":"MortgageApplication\/cobol\/epscsmrd.cbl","mq":false,"cics":true,"language":"COB","sql":false}
** Storing 15 logical files in repository collection 'MortgageApplication-master'
HTTP/1.1 200 OK
** Scanning load modules.
*** Scanning file with the default scanner
*** Skipped scanning module DBB.ZAPP.CLEAN.MASTER.LOAD(EPSNBRVL) of MortgageApplication/cobol/epsnbrvl.cbl.
*** Scanning file with the default scanner
*** Scanning load module DBB.ZAPP.CLEAN.MASTER.LOAD(EPSCSMRT) of MortgageApplication/cobol/epscsmrt.cbl
*** Scanning load module for MortgageApplication/cobol/epscsmrt.cbl
*** Logical file =
{"dli":false,"lname":"EPSCSMRT","file":"MortgageApplication\/cobol\/epscsmrt.cbl","mq":false,"cics":false,"language":"ZBND","sql":false}
*** No language prefix defined for BMS.groovy.
*** Skipped scanning outputs of MortgageApplication/bms/epsmort.bms. No language prefix found.
*** Scanning file with the default scanner
*** Scanning load module DBB.ZAPP.CLEAN.MASTER.LOAD(EPSMLIST) of MortgageApplication/link/epsmlist.lnk
*** Scanning load module for MortgageApplication/link/epsmlist.lnk
*** Logical file =
{"dli":false,"lname":"EPSMLIST","file":"MortgageApplication\/link\/epsmlist.lnk","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSMPMT","library":"DBB.ZAPP.CLEAN.MASTER.LOAD","category":"LINK"},{"lname":"EPSMLIST","library":"DBB.ZAPP.CLEAN.MASTER.OBJ","category":"LINK"}],"language":"ZBND","sql":false}
*** No language prefix defined for BMS.groovy.
*** Skipped scanning outputs of MortgageApplication/bms/epsmlis.bms. No language prefix found.
*** Scanning file with the default scanner
*** Skipped scanning module DBB.ZAPP.CLEAN.MASTER.LOAD(EPSMLIST) of MortgageApplication/cobol/epsmlist.cbl.
*** Scanning file with the default scanner
*** Scanning load module DBB.ZAPP.CLEAN.MASTER.LOAD(EPSMPMT) of MortgageApplication/cobol/epsmpmt.cbl
*** Scanning load module for MortgageApplication/cobol/epsmpmt.cbl
*** Logical file =
{"dli":false,"lname":"EPSMPMT","file":"MortgageApplication\/cobol\/epsmpmt.cbl","mq":false,"cics":false,"language":"ZBND","sql":false}
*** Scanning file with the default scanner
*** Scanning load module DBB.ZAPP.CLEAN.MASTER.LOAD(EPSCMORT) of MortgageApplication/cobol/epscmort.cbl
*** Scanning load module for MortgageApplication/cobol/epscmort.cbl
*** Logical file =
{"dli":false,"lname":"EPSCMORT","file":"MortgageApplication\/cobol\/epscmort.cbl","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSNBRVL","library":"DBB.ZAPP.CLEAN.MASTER.OBJ","category":"LINK"}],"language":"ZBND","sql":false}
*** Scanning file with the default scanner
*** Scanning load module DBB.ZAPP.CLEAN.MASTER.LOAD(EPSCSMRD) of MortgageApplication/cobol/epscsmrd.cbl
*** Scanning load module for MortgageApplication/cobol/epscsmrd.cbl
*** Logical file =
{"dli":false,"lname":"EPSCSMRD","file":"MortgageApplication\/cobol\/epscsmrd.cbl","mq":false,"cics":false,"language":"ZBND","sql":false}
*** Obtaining hash for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Setting property :githash:MortgageApplication : 857266a44a6e859c4f949adb7e32cfbc4a8bd736
** Setting property :giturl:MortgageApplication : git@github.ibm.com:zDevOps-Acceleration/dbb-zappbuild.git
** Writing build report data to /var/dbb/out/MortgageApplication/build.20210622.105915.059/BuildReport.json
** Writing build report to /var/dbb/out/MortgageApplication/build.20210622.105915.059/BuildReport.html
** Updating build result BuildGroup:MortgageApplication-master BuildLabel:build.20210622.105915.059 at https://dbb-webapp:8080/dbb/rest/buildResult/47085
** Build ended at Tue Jun 22 10:59:39 GMT+01:00 2021
** Build State : CLEAN
** Total files processed : 15
** Total build time  : 23.718 seconds
```

</details>

### Dynamically Overwrite build properties

Dependending on where are in your development process, you might want to dynamically overwrite a build property. One scenario is for example to set the `mainBuildBranch` option which is used also to clone collections and initialize the DBB collections. See [Perform Impact Build for topic branches](#perform-impact-build-for-topic-branches)

To dynamically overwrite any build property, you can make use of the `--propOverwrite` cli argument. To overwrite a build property, pass it in as a key-value pair via the CLI. Several key-value pairs can be passed by comma-separating them :`--propOverwrite mainBuildBranch=develop,cobol_compilerVersion=V6`. Please note, that values which contain a comma (`,`) cannot be passed into the list. Please also make sure that you don't run into any limitations with regards of the length of the overall build command. For mass overwrites, use the `--propFiles` option to pass in one or several property files.


```
groovyz dbb-zappbuild/build.groovy \
                      --workspace /var/dbb/dbb-zappbuild/samples \
                      --hlq DBB.ZAPP.CLEAN.MASTER \
                      --workDir /var/dbb/out/MortgageApplication \
                      --application MortgageApplication \
                      --logEncoding UTF-8 \
                      --impactBuild \
                      --verbose \
                      --propOverwrite mainBuildBranch=develop
```
<details>
  <summary>Build log</summary>

```
** Build start at 20210622.082942.029
** Input args = /var/dbb/dbb-zappbuild/samples --hlq DBB.ZAPP.CLEAN.MASTER --workDir /var/dbb/out/MortgageApplication --application MortgageApplication --logEncoding UTF-8 --impactBuild --verbose --propOverwrite mainBuildBranch=develop
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/datasets.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/Assembler.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/BMS.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/MFS.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/PSBgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/DBDgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/ACBgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/Cobol.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/LinkEdit.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/PLI.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/ZunitConfig.properties
** appConf = /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/file.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/BMS.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/Cobol.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/LinkEdit.properties
** Overwriting build property mainBuildBranch from cli argument --propOverwrite with value develop
..... // lists of all build properties
** Repository client created for https://dbb-webapp:8080/dbb
** Build output located at /var/dbb/out/MortgageApplication/build.20210622.082942.029
** Build result created for BuildGroup:MortgageApplication-master BuildLabel:build.20210622.082942.029 at https://dbb-webapp:8080/dbb/rest/buildResult/47012
** --impactBuild option selected. Building impacted programs for application MortgageApplication
** Getting current hash for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Storing MortgageApplication : 857266a44a6e859c4f949adb7e32cfbc4a8bd736
** Getting baseline hash for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Storing MortgageApplication : 75e13783f2197e12772cec64a16937707ea623a5
** Calculating changed files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Diffing baseline 75e13783f2197e12772cec64a16937707ea623a5 -> current 857266a44a6e859c4f949adb7e32cfbc4a8bd736
*** Changed files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication:
**** MortgageApplication/copybook/epsmtcom.cpy
*** Deleted files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication:
*** Renamed files for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication:
** Updating collections MortgageApplication-master and MortgageApplication-master-outputs
*** Scanning file MortgageApplication/copybook/epsmtcom.cpy (/var/dbb/dbb-zappbuild/samples/MortgageApplication/copybook/epsmtcom.cpy)
*** Scanning file with the default scanner
*** Logical file for MortgageApplication/copybook/epsmtcom.cpy =
{"dli":false,"lname":"EPSMTCOM","file":"MortgageApplication\/copybook\/epsmtcom.cpy","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSMTINP","library":"SYSLIB","category":"COPY"},{"lname":"EPSMTOUT","library":"SYSLIB","category":"COPY"}],"language":"COB","sql":false}
** Storing 1 logical files in repository collection 'MortgageApplication-master'
HTTP/1.1 200 OK
** Performing impact analysis on changed file MortgageApplication/copybook/epsmtcom.cpy
*** Creating impact resolver for MortgageApplication/copybook/epsmtcom.cpy with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                },{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/bms"} ]             },{"category": "LINK", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/cobol"}, {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/link"} ]             }] rules
** Found impacted file MortgageApplication/cobol/epscsmrt.cbl
** MortgageApplication/cobol/epscsmrt.cbl is impacted by changed file MortgageApplication/copybook/epsmtcom.cpy. Adding to build list.
** Found impacted file MortgageApplication/link/epsmlist.lnk
** MortgageApplication/link/epsmlist.lnk is impacted by changed file MortgageApplication/copybook/epsmtcom.cpy. Adding to build list.
** Found impacted file MortgageApplication/cobol/epscmort.cbl
** MortgageApplication/cobol/epscmort.cbl is impacted by changed file MortgageApplication/copybook/epsmtcom.cpy. Adding to build list.
** Found impacted file MortgageApplication/cobol/epsmlist.cbl
** MortgageApplication/cobol/epsmlist.cbl is impacted by changed file MortgageApplication/copybook/epsmtcom.cpy. Adding to build list.
** Found impacted file MortgageApplication/cobol/epscsmrt.cbl
** MortgageApplication/cobol/epscsmrt.cbl is impacted by changed file MortgageApplication/copybook/epsmtcom.cpy. Adding to build list.
** Found impacted file MortgageApplication/cobol/epscmort.cbl
** MortgageApplication/cobol/epscmort.cbl is impacted by changed file MortgageApplication/copybook/epsmtcom.cpy. Adding to build list.
** Writing build list file to /var/dbb/out/MortgageApplication/build.20210622.082942.029/buildList.txt
MortgageApplication/cobol/epsmlist.cbl
MortgageApplication/cobol/epscsmrt.cbl
MortgageApplication/cobol/epscmort.cbl
MortgageApplication/link/epsmlist.lnk
** Invoking build scripts according to build order: BMS.groovy,Cobol.groovy,LinkEdit.groovy
** Building files mapped to Cobol.groovy script
required props = cobol_srcPDS,cobol_cpyPDS,cobol_objPDS,cobol_loadPDS,cobol_compiler,cobol_linkEditor,cobol_tempOptions,applicationOutputsCollectionName,  SDFHCOB,SDFHLOAD,SDSNLOAD,SCEELKED
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.COBOL
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.COPY
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.OBJ
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.DBRM
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.LOAD
*** Building file MortgageApplication/cobol/epsmlist.cbl
*** Creating dependency resolver for MortgageApplication/cobol/epsmlist.cbl with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                }] rules
*** Scanning file with the default scanner
*** Resolution rules for MortgageApplication/cobol/epsmlist.cbl:
{"library":"SYSLIB","searchPath":[{"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","directory":"MortgageApplication\/copybook"}]}
*** Physical dependencies for MortgageApplication/cobol/epsmlist.cbl:
{"excluded":false,"lname":"DFHAID","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"lname":"EPSMLIS","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMORTF","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmortf.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTINP","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtinp.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTOUT","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtout.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTCOM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtcom.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSNBRPM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsnbrpm.cpy","category":"COPY","resolved":true}
Cobol compiler parms for MortgageApplication/cobol/epsmlist.cbl = LIB,CICS
*** Building file MortgageApplication/cobol/epscsmrt.cbl
*** Creating dependency resolver for MortgageApplication/cobol/epscsmrt.cbl with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                }] rules
*** Scanning file with the default scanner
*** Resolution rules for MortgageApplication/cobol/epscsmrt.cbl:
{"library":"SYSLIB","searchPath":[{"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","directory":"MortgageApplication\/copybook"}]}
*** Physical dependencies for MortgageApplication/cobol/epscsmrt.cbl:
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTINP","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtinp.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTOUT","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtout.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTCOM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtcom.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSPDATA","library":"SYSLIB","file":"MortgageApplication\/copybook\/epspdata.cpy","category":"COPY","resolved":true}
Cobol compiler parms for MortgageApplication/cobol/epscsmrt.cbl = LIB,CICS
*** Scanning load module for MortgageApplication/cobol/epscsmrt.cbl
*** Logical file =
{"dli":false,"lname":"EPSCSMRT","file":"MortgageApplication\/cobol\/epscsmrt.cbl","mq":false,"cics":false,"language":"ZBND","sql":false}
*** Building file MortgageApplication/cobol/epscmort.cbl
*** Creating dependency resolver for MortgageApplication/cobol/epscmort.cbl with [{"library": "SYSLIB", "searchPath": [ {"sourceDir": "/var/dbb/dbb-zappbuild/samples", "directory": "MortgageApplication/copybook"} ]                }] rules
*** Scanning file with the default scanner
*** Resolution rules for MortgageApplication/cobol/epscmort.cbl:
{"library":"SYSLIB","searchPath":[{"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","directory":"MortgageApplication\/copybook"}]}
*** Physical dependencies for MortgageApplication/cobol/epscmort.cbl:
{"excluded":false,"lname":"DFHAID","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"lname":"EPSMORT","library":"SYSLIB","category":"COPY","resolved":false}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTINP","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtinp.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTOUT","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtout.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSMTCOM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsmtcom.cpy","category":"COPY","resolved":true}
{"excluded":false,"sourceDir":"\/var\/dbb\/dbb-zappbuild\/samples","lname":"EPSNBRPM","library":"SYSLIB","file":"MortgageApplication\/copybook\/epsnbrpm.cpy","category":"COPY","resolved":true}
{"excluded":false,"lname":"SQLCA","library":"SYSLIB","category":"SQL INCLUDE","resolved":false}
Cobol compiler parms for MortgageApplication/cobol/epscmort.cbl = LIB,CICS,SQL
*** Scanning load module for MortgageApplication/cobol/epscmort.cbl
*** Logical file =
{"dli":false,"lname":"EPSCMORT","file":"MortgageApplication\/cobol\/epscmort.cbl","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSNBRVL","library":"DBB.ZAPP.CLEAN.MASTER.OBJ","category":"LINK"}],"language":"ZBND","sql":false}
** Building files mapped to LinkEdit.groovy script
required props = linkedit_srcPDS,linkedit_objPDS,linkedit_loadPDS,linkedit_linkEditor,linkedit_tempOptions,applicationOutputsCollectionName,  SDFHLOAD,SCEELKED
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.LINK
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.OBJ
** Creating / verifying build dataset DBB.ZAPP.CLEAN.MASTER.LOAD
*** Building file MortgageApplication/link/epsmlist.lnk
*** Creating dependency resolver for MortgageApplication/link/epsmlist.lnk with null rules
*** Scanning file with the default scanner
*** Scanning load module for MortgageApplication/link/epsmlist.lnk
*** Logical file =
{"dli":false,"lname":"EPSMLIST","file":"MortgageApplication\/link\/epsmlist.lnk","mq":false,"cics":false,"logicalDependencies":[{"lname":"EPSMPMT","library":"DBB.ZAPP.CLEAN.MASTER.LOAD","category":"LINK"},{"lname":"EPSMLIST","library":"DBB.ZAPP.CLEAN.MASTER.OBJ","category":"LINK"}],"language":"ZBND","sql":false}
*** Obtaining hash for directory /var/dbb/dbb-zappbuild/samples/MortgageApplication
** Setting property :githash:MortgageApplication : 857266a44a6e859c4f949adb7e32cfbc4a8bd736
** Setting property :giturl:MortgageApplication : git@github.ibm.com:zDevOps-Acceleration/dbb-zappbuild.git
** Writing build report data to /var/dbb/out/MortgageApplication/build.20210622.082942.029/BuildReport.json
** Writing build report to /var/dbb/out/MortgageApplication/build.20210622.082942.029/BuildReport.html
** Updating build result BuildGroup:MortgageApplication-master BuildLabel:build.20210622.082942.029 at https://dbb-webapp:8080/dbb/rest/buildResult/47012
** Build ended at Tue Jun 22 08:29:59 GMT+01:00 2021
** Build State : CLEAN
** Total files processed : 4
** Total build time  : 17.239 seconds
```

</details>


### Validate System Datasets

During the initialization phase of the build, a validation of the defined system datasets can be performed. The system datasets are configured with the build property `systemDatasets` in [build-conf/build.properties](../build-conf/build.properties), which contains one or multiple references to build property files defining key-value pairs listing the system datasets. In zAppBuild the default file is called [datasets.properties](../build-conf/datasets.properties) managed in the `build-conf` folder.

To enable validation of system datasets specify the option `--checkDatasets`. It is available in any build scenario. Be aware that this functionality is also available as a stand-alone script and find the instructions [here](../utilities/README.md#dataset-validation-utilities)

```
groovyz dbb-zappbuild/build.groovy \
                      --workspace /var/dbb/dbb-zappbuild/samples \
                      --hlq DBB.ZAPP.CLEAN.MASTER \
                      --workDir /var/dbb/out/MortgageApplication \
                      --application MortgageApplication \
                      --logEncoding UTF-8 \
                      --impactBuild \
                      --verbose \
                      --checkDatasets
```
<details>
  <summary>Build log</summary>

```
** Build start at 20210622.082942.029
** Input args = /var/dbb/dbb-zappbuild/samples --hlq DBB.ZAPP.CLEAN.MASTER --workDir /var/dbb/out/MortgageApplication --application MortgageApplication --logEncoding UTF-8 --impactBuild --verbose --propOverwrite mainBuildBranch=develop
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/datasets.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/Assembler.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/BMS.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/MFS.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/PSBgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/DBDgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/ACBgen.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/Cobol.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/LinkEdit.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/PLI.properties
** Loading property file /ZT01/var/dbb/dbb-zappbuild/build-conf/ZunitConfig.properties
** appConf = /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/file.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/BMS.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/Cobol.properties
** Loading property file /var/dbb/dbb-zappbuild/samples/MortgageApplication/application-conf/LinkEdit.properties
** Overwriting build property mainBuildBranch from cli argument --propOverwrite with value develop
** The dataset PLI.V5R2.SIBMZCMP referenced for property IBMZPLI_V52 was found.
*! No dataset defined for property IBMZPLI_V51 specified in /ZT01/var/dbb/dbb-zappbuild/build-conf/datasets.properties.
** The dataset WMQ.V9R2M4.SCSQPLIC referenced for property SCSQPLIC was found.
** The dataset COBOL.V6R1.SIGYCOMP referenced for property SIGYCOMP_V6 was found.
** The dataset CICSTS.V5R4.CICS.SDFHCOB referenced for property SDFHCOB was found.
*! No dataset defined for property SIGYCOMP_V4 specified in /ZT01/var/dbb/dbb-zappbuild/build-conf/datasets.properties.
** The dataset HLASM.SASMMOD1 referenced for property SASMMOD1 was found.
** The dataset SYS1.MACLIB referenced for property MACLIB was found.
** The dataset PDTCC.V1R8.SIPVMODA referenced for property PDTCCMOD was found.
** The dataset CICSTS.V5R4.CICS.SDFHLOAD referenced for property SDFHLOAD was found.
** The dataset CICSTS.V5R4.CICS.SDFHMAC referenced for property SDFHMAC was found.
** The dataset CEE.SCEEMAC referenced for property SCEEMAC was found.
** The dataset WMQ.V9R2M4.SCSQCOBC referenced for property SCSQCOBC was found.
** The dataset IMS.V15R1.SDFSMAC referenced for property SDFSMAC was found.
** The dataset RDZ.V14R1.SFELLOAD referenced for property SFELLOAD was found.
** The dataset DBC0CFG.DB2.V12.SDSNLOAD referenced for property SDSNLOAD was found.
** The dataset CICSTS.V5R4.CICS.SDFHPL1 referenced for property SDFHPL1 was found.
** The dataset WMQ.V9R2M4.SCSQLOAD referenced for property SCSQLOAD was found.
** The dataset IMSCFG.IMSC.REFERAL referenced for property REFERAL was found.
** The dataset DEBUG.V14R1.SEQAMOD referenced for property SEQAMOD was found.
** The dataset DBC0CFG.SDSNEXIT referenced for property SDSNEXIT was found.
** The dataset IMS.V15R1.SDFSRESL referenced for property SDFSRESL was found.
** The dataset RATCFG.ZUNIT.SBZUSAMP referenced for property SBZUSAMP was found.
** The dataset CEE.SCEELKED referenced for property SCEELKED was found.
..... // lists of all build properties
...
...
```

</details>


