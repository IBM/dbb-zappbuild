# Building Applications with zAppBuild
The main or start build script for zAppBuild is `build.groovy`. Dependency Based Build (DBB) requires that the DBB_HOME environment variable be set when executing a Groovy script that uses DBB APIs.  In order to build an application using zAppBuild, change directory to the zAppBuild directory on USS and type `$DBB_HOME/bin/groovyz build.groovy`.

However this will result in an error message because the build.groovy script has four required arguments that must be present during each invocation:
* --workspace <arg> - Absolute path to workspace (root) directory containing all required source directories or local Git repositories to build the application.
* --application <arg> - Application local repository directory name (relative to workspace).
* --outDir <arg> - Absolute path to the build output root directory on USS
* --hlq <arg> -  High level qualifier for created build partition data sets


Example:
```
$DBB_HOME/bin/groovyz build.groovy --workspace /u/build/repos --application app1 --outDir /u/build/out --hlq BUILD.APP1
```
Since we are still missing a build target or calculated build option, the build will run successfully but not actually build any programs.  

## Common Invocation Examples

**Build one program**
```
$DBB_HOME/bin/groovyz build.groovy --workspace /u/build/repos --application app1 --outDir /u/build/out --hlq BUILD.APP1 app1/cobol/epsmpmt.cbl
```
**Build a list of programs contained in a text file**
```
$DBB_HOME/bin/groovyz build.groovy --workspace /u/build/repos --application app1 --outDir /u/build/out --hlq BUILD.APP1 /u/usr1/buildList.txt
```
**Build all programs in the application**
```
$DBB_HOME/bin/groovyz build.groovy --workspace /u/build/repos --application app1 --outDir /u/build/out --hlq BUILD.APP1 --fullBuild
```
**Build only programs that have changed or are impacted by changed copybooks or include files since the last successful build**
```
$DBB_HOME/bin/groovyz build.groovy --workspace /u/build/repos --application app1 --outDir /u/build/out --hlq BUILD.APP1 --impactBuild
```
**Only scan source files in the application to collect dependency data without actually creating load modules**
```
$DBB_HOME/bin/groovyz build.groovy --workspace /u/build/repos --application app1 --outDir /u/build/out --hlq BUILD.APP1 --fullBuild --scanOnly
```
**Scan source files and existing load modules for the application to collect dependency data for source and outputs without actually creating load modules**
```
$DBB_HOME/bin/groovyz build.groovy --workspace /u/build/repos --application app1 --outDir /u/build/out --hlq BUILD.APP1 --fullBuild --scanAll
```
**Use Code Coverage Headless Collector in zUnit Tests and specify parameters through command-line options (which override properties defined in ZunitConfig.properties)**
```
$DBB_HOME/bin/groovyz build.groovy --workspace /u/build/repos --application app1 --outDir /u/build/out --hlq BUILD.APP1 --fullBuild --cc --cch localhost --ccp 8009 --cco "e=CCPDF"
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
 -p,--propFiles               Comma separated list of additional property files 
                              to load. Absolute paths or relative to workspace
 -f,--fullBuild               Flag indicating to build all programs for
                              the application
 -i,--impactBuild             Flag indicating to build only programs impacted
                              by changed files since last successful build.
                          
 -s,--scanOnly                Flag indicating to only scan source files for application without building anything (deprecated use --scanSource)
 -ss,--scanSource             Flag indicating to only scan source files for application without building anything
 -sl,--scanLoad               Flag indicating to only scan load modules for application without building anything
 -sa,--scanAll                Flag indicating to scan both source files and load modules for application without building anything
 
 -r,--reset                   Deletes the application's dependency collections 
                              and build result group from the DBB repository
 -v,--verbose                 Flag to turn on script trace
 -d,--debug                   Flag to build modules for debugging with
                              IBM Debug for z/OS
 -l,--logEncoding <arg>       Encoding of output logs. Default is EBCDIC 
                              directory for user build
 -zTest,--runzTests           Specify if zUnit Tests should be run
 
 -cc,--ccczUnit               Flag to indicate to collect code coverage reports during zUnit step
 -cch,--cccHost               Headless Code Coverage Collector host (if not specified IDz will be used for reporting)
 -ccp,--cccPort               Headless Code Coverage Collector port (if not specified IDz will be used for reporting)
 -cco,--cccOptions            Headless Code Coverage Collector Options

 -re,--reportExternalImpacts  Flag to activate analysis and report of external impacted files within DBB collections
 

web application credentials
 -url,--url <arg>             DBB repository URL
 -id,--id <arg>               DBB repository id
 -pw,--pw <arg>               DBB repository password
 -pf,--pwFile <arg>           Absolute or relative (from workspace) path to
                              file containing DBB password

IDz/ZOD User Build options
 -u,--userBuild               Flag indicating running a user build
 -e,--errPrefix <arg>         Unique id used for IDz error message datasets

utility options
 -help,--help                 Prints this message
 ```
