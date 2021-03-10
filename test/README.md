# zAppBuild/test
Test folder is designed to help test samples like the Mortgage Application against ZAppBuild.

## Repository Legend
Folder/File | Description | Documentation Link
--- | --- | ---
samples/MortgageApplication | This folder contains modified language scripts used to execute impact build by replacing these modified files with the original language files | [MortgageApplication/README.md](applications/MortgageApplication/README.md)
test.groovy  | This is the main build script that is called to start the test process | [test.groovy](/test/README.md#testing-applications-with-zappbuild)

# Testing Applications with zAppBuild
The main script for testing applications against zAppBuild is `test.groovy`. It takes most of its input from the command line to run full and impact builds. `test.groovy` once executed from the command line calls [fullBuild.groovy](/test/testScripts/fullBuild.groovy) and [impactBuild.groovy](/test/testScripts/impactBuild.groovy) scripts to perform an end to end test on the given feature branch with the program specified for impact build. 

test.groovy script has five required arguments that must be present during each invocation:
* --branch <arg> - zAppBuild branch to test
* --app <arg> - Application that is being tested (example: MortgageApplication)
* --url <arg> - DBB Web Application server URL
* --hlq <arg> - HLQ for dataset reation / deletion (example: USER.BUILD)
* --id <arg> - DBB Web Application user id

test.groovy script has three optional argument that can be present during each invocation
* --pw <arg> - DBB Web Application user password
* --pwFile <arg> - DBB Web Application user password file
* --verbose <arg> - Flag indicating to print trace statements

# Examples of running an end to end test:

```
$DBB_HOME/bin/groovyz ${repoPath}/test/test.groovy -b testBranch -a MortgageApplication -q IBMDBB.ZAPPB.BUILD -u https://dbbdev.rtp.raleigh.ibm.com:19443/dbb/ -i ADMIN -p ADMIN
``` 

# Examples of outputs to be expected:

Successful test run
```
** Invoking test scripts according to test list order: fullBuild.groovy,impactBuild.groovy

/////********EXECUTING INITIALIZATION SCRIPT USING THESE BUILD PROPERTIES
zRepoPath: Optional path to ZAppBuild Repo
branchName: Feature branch to create a test(automation) branch against

Exit code: 0
Deleting test PDSEs . . .
** deleting 'IBMDBB.ZAPPB.BUILD.BMS'
** deleting 'IBMDBB.ZAPPB.BUILD.COBOL'
** deleting 'IBMDBB.ZAPPB.BUILD.DBRM'
** deleting 'IBMDBB.ZAPPB.BUILD.LINK'
** deleting 'IBMDBB.ZAPPB.BUILD.LOAD'
** deleting 'IBMDBB.ZAPPB.BUILD.OBJ'

/////********EXECUTING FULL BUILD USING THESE BUILD PROPERTIES
zRepoPath: Optional path to ZAppBuild Repo
branchName: Feature branch to create a test(automation) branch against
app: Application that is being tested (example: MortgageApplication)
hlq: hlq to delete segments from (example: IBMDBB.ZAPP.BUILD)
serverURL: Server URL example(https://dbbdev.rtp.raleigh.ibm.com:19443/dbb/)
userName: User for server
password: Password for server
fullFiles: Build files for verification

***This is dbb home****/u/dbbAutomation/workspace/Automation_Jobs/DBB_All_BuildS/DBBZtoolkitTar
Already on 'AutomationTest'
Switched to a new branch 'automation'

/////********EXECUTING IMPACT BUILD USING THESE BUILD PROPERTIES
serverURL: Server URL example(https://dbbdev.rtp.raleigh.ibm.com:19443/dbb/)
zRepoPath: Optional path to ZAppBuild Repo
programFile: Path to the program folder for the file to be edited
app: Application that is being tested (example: MortgageApplication)
hlq: hlq to delete segments from (example: IBMDBB.ZAPP.BUILD)
userName: User for server
password: Password for server
impactFiles: Impact build files for verification

** Build finished
```
Test run with errors
```
Caught: java.lang.AssertionError: ///***EITHER THE FULLBUILD FAILED OR TOTAL FILES PROCESSED ARE NOT EQUAL TO 9.
 HERE IS THE OUTPUT FROM FULLBUILD 
Your branch is up-to-date with 'origin/AutomationTest'.

** Build start at 20210303.101913.019
** Repository client created for https://dbbdev.rtp.raleigh.ibm.com:19443/dbb/
** Build output located at /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/out/build.20210303.101913.019
** Build result created for BuildGroup:MortgageApplication-automation BuildLabel:build.20210303.101913.019 at https://dbbdev.rtp.raleigh.ibm.com:19443/dbb/rest/buildResult/452
** --fullBuild option selected. Building all programs for application MortgageApplication
** Writing build list file to /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/out/build.20210303.101913.019/buildList.txt
** Invoking build scripts according to build order: BMS.groovy,Cobol.groovy,LinkEdit.groovy
** Building files mapped to BMS.groovy script
*** Building file MortgageApplication/bms/epsmort.bms
*** Building file MortgageApplication/bms/epsmlis.bms
** Building files mapped to Cobol.groovy script
*** Building file MortgageApplication/cobol/epsnbrvl.cbl
*** Building file MortgageApplication/cobol/epscsmrt.cbl
*** Building file MortgageApplication/cobol/epsmlist.cbl
*** Building file MortgageApplication/cobol/epsmpmt.cbl
*** Building file MortgageApplication/cobol/epscmort.cbl
*** Building file MortgageApplication/cobol/epscsmrd.cbl
** Building files mapped to LinkEdit.groovy script
*** Building file MortgageApplication/link/epsmlist.lnk
** Writing build report data to /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/out/build.20210303.101913.019/BuildReport.json
** Writing build report to /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/out/build.20210303.101913.019/BuildReport.html
** Build ended at Wed Mar 03 10:19:36 EST 2021
** Build State : CLEAN
** Total files processed : 8
** Total build time  : 23.164 seconds

** Build finished
```

## Command Line Options Summary
```
utility arguments:
-h, --help      Shows usage information, like above
-v, --verbose   Flag indicating to print trace statements
	   
test framework arguments:
-b, --branch    zAppBuild branch to test, required

zAppBuild arguments:
-a, --app       Application that is being tested (example: MortgageApplication), required
-q, --hlq       HLQ for dataset reation / deletion (example: USER.BUILD), required
-u, --url       DBB Web Application server URL, required
-i, --id        DBB Web Application user id, required
-p, --pw        DBB Web Application user password
-P, --pwFile    DBB Web Application user password file
 ```
