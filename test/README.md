# zAppBuild/test
Test folder is designed to help test samples like the Mortgage Application against ZAppBuild.

## Repository Legend
Folder/File | Description | Documentation Link
--- | --- | ---
samples/MortgageApplication | This folder contains modified language scripts used to execute impact build by replacing these modified files with the original language files | [MortgageApplication/README.md](samples/MortgageApplication/README.md)
test-conf | This folder contains global configuration properties used by test.groovy | [test-conf/README.md](test-conf/README.md)   
test.groovy  | This is the main build script that is called to start the test process | [test.groovy](/test/README.md#testing-applications-with-zappbuild)
initialization.groovy | This script that is called by test.groovy to clean “automation” test branch created for testing purposes from the feature branch that‘s to be tested and hlq from the previous run | [initialization.groovy](/test/README.md#initializationgroovy)
fullBuild.groovy | This script is called by test.groovy to run a full build by creating an “automation” branch from the feature branch | [fullBuild.groovy](/test/README.md#fullBuildgroovy)
impactBuild.groovy | This script that is called by test.groovy to run an impact build against the program file provided via command line arguments | [impactBuild.groovy](/test/README.md#impactBuildgroovy)

# Testing Applications with zAppBuild
The main script for testing applications against zAppBuild is `test.groovy`. It takes most of its input from the command line to run full and impact builds. `test.groovy` once executed from the command line calls `initialization.groovy`, `fullBuild.groovy` and `impactBuild.groovy` scripts to perform an end to end test on the given feature branch with the program specified for impact build. 

test.groovy script has one optional argument that can be present during each invocation:
* --zRepoPath <arg> - Optional path to ZAppBuild Repo

test.groovy script has nine required arguments that must be present during each invocation:
* --branchName <arg> - Feature branch that needs to be tested
* --app <arg> - Application that is being tested (example: MortgageApplication)
* --serverURL <arg> - Server URL 
* --hlq <arg> - hlq to delete segments from (example: IBMDBB.ZAPP.BUILD)
* --userName <arg> - User for server
* --password <arg> - Password for server
* --fullFiles <arg> - Full build files for verification
* --impactFiles <arg> - Impact build files for verification
* --programFile <arg> - Folder of the program to edit (example: /bms/epsmort.bms)


# Examples of running an end to end test:

With zRepoPath passed in as an argument
```
$DBB_HOME/bin/groovyz ${repoPath}/test/test.groovy -z ${repoPath} -b AutomationTest -a MortgageApplication -q IBMDBB.ZAPPB.BUILD -s https://dbbdev.rtp.raleigh.ibm.com:19443/dbb/ -u ADMIN -p ADMIN -f epsmort.bms,epsmlis.bms,epsnbrvl.cbl,epscsmrt.cbl,epsmlist.cbl,epsmpmt.cbl,epscmort.cbl,epscsmrd.cbl,epsmlist.lnk -c /bms/epsmort.bms -i epsmort.bms,epscmort.cbl
``` 
Without zRepoPath passed in as an argument
```
$DBB_HOME/bin/groovyz ${repoPath}/test/test.groovy -b AutomationTest -a MortgageApplication -q IBMDBB.ZAPPB.BUILD -s https://dbbdev.rtp.raleigh.ibm.com:19443/dbb/ -u ADMIN -p ADMIN -f epsmort.bms,epsmlis.bms,epsnbrvl.cbl,epscsmrt.cbl,epsmlist.cbl,epsmpmt.cbl,epscmort.cbl,epscsmrd.cbl,epsmlist.lnk -c /bms/epsmort.bms -i epsmort.bms,epscmort.cbl
```

# Examples of outputs to be expected:

Successful test run
```
** Invoking test scripts according to test list order: initialization.groovy,fullBuild.groovy,impactBuild.groovy

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
optional arguments:
-z --zRepoPath <arg>   Path to the cloned/forked zAppBuild repository

required arguments:
 -b --branchName <arg> Feature Branch that needs to be tested 
 -a --app <arg> Application that is being tested (example: MortgageApplication)
 -s --serverURL <arg> Server URL
 -q --hlq <arg> hlq to delete segments from (example: IBMDBB.ZAPP.BUILD)
 -u --userName <arg> User for server
 -p --password <arg> Password for server
 -f --fullFiles <arg> Full build files for verification
 -i --impactFiles <arg> Impact build files for verification
 -c --programFile <arg> Folder of the program to edit (example: /bms/epsmort.bms)

utility arguments:
 -h ,--help           Shows usage information, like above
 ```
