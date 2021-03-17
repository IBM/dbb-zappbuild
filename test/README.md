# zAppBuild/test
Test folder is designed to help test samples like the Mortgage Application against ZAppBuild.

## Repository Legend
Folder/File | Description | Documentation Link
--- | --- | ---
applications/MortgageApplication | This folder contains modified language scripts used to execute impact build by replacing these modified files with the original language files | [MortgageApplication/README.md](applications/MortgageApplication/README.md)
test.groovy  | This is the main build script that is called to start the test process | [test.groovy](/test/README.md#testing-applications-with-zappbuild)
testScripts  | This folder contains test scripts to execute full and impact builds | [testScripts](/test/testScripts/README.md)

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
$DBB_HOME/bin/groovyz ${repoPath}/test/test.groovy -b testBranch -a MortgageApplication -q USER.BUILD -u https://dbbdev.rtp.raleigh.ibm.com:19443/dbb/ -i ADMIN -p ADMIN
``` 

# Examples of outputs to be expected:

Successful test run
```
** Executing zAppBuild test framework test/test.groovy
** Creating and checking out branch zAppBuildTesting
Your branch is up-to-date with 'origin/TestAutomation'.
On branch zAppBuildTesting
nothing to commit, working tree clean

** Invoking test scripts according to test list order: fullBuild.groovy,impactBuild.groovy

** Executing test script fullBuild.groovy
** Executing /u/dbbAutomation/workspace/Automation_Jobs/DBB_All_BuildS/DBBZtoolkitTar/bin/groovyz /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/build.groovy --workspace /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/samples --application MortgageApplication --outDir /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/out --hlq IBMDBB.ZAPPB.BUILD --logEncoding UTF-8 --url https://dbbdev.rtp.raleigh.ibm.com:19443/dbb/ --id ADMIN --pw ADMIN --fullBuild
** Validating full build results
**
** FULL BUILD TEST : PASSED **
**
Deleting full build PDSEs [BMS, COBOL, LINK]

** Executing test script impactBuild.groovy
** Processing changed files from impactBuild_changedFiles property : bms/epsmort.bms,cobol/epsmlist.cbl,copybook/epsmtout.cpy,link/epsmlist.lnk

** Running impact build test for changed file bms/epsmort.bms
** Copying and committing /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/test/applications/MortgageApplication/bms/epsmort.bms to /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/samples/MortgageApplication/bms/epsmort.bms
** Executing /u/dbbAutomation/workspace/Automation_Jobs/DBB_All_BuildS/DBBZtoolkitTar/bin/groovyz /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/build.groovy --workspace /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/samples --application MortgageApplication --outDir /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/out --hlq IBMDBB.ZAPPB.BUILD --logEncoding UTF-8 --url https://dbbdev.rtp.raleigh.ibm.com:19443/dbb/ --id ADMIN --pw ADMIN --impactBuild
** Validating impact build results
**
** IMPACT BUILD TEST : PASSED **
**

** Running impact build test for changed file cobol/epsmlist.cbl
** Copying and committing /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/test/applications/MortgageApplication/cobol/epsmlist.cbl to /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/samples/MortgageApplication/cobol/epsmlist.cbl
** Executing /u/dbbAutomation/workspace/Automation_Jobs/DBB_All_BuildS/DBBZtoolkitTar/bin/groovyz /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/build.groovy --workspace /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/samples --application MortgageApplication --outDir /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/out --hlq IBMDBB.ZAPPB.BUILD --logEncoding UTF-8 --url https://dbbdev.rtp.raleigh.ibm.com:19443/dbb/ --id ADMIN --pw ADMIN --impactBuild
** Validating impact build results
**
** IMPACT BUILD TEST : PASSED **
**

** Running impact build test for changed file copybook/epsmtout.cpy
** Copying and committing /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/test/applications/MortgageApplication/copybook/epsmtout.cpy to /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/samples/MortgageApplication/copybook/epsmtout.cpy
** Executing /u/dbbAutomation/workspace/Automation_Jobs/DBB_All_BuildS/DBBZtoolkitTar/bin/groovyz /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/build.groovy --workspace /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/samples --application MortgageApplication --outDir /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/out --hlq IBMDBB.ZAPPB.BUILD --logEncoding UTF-8 --url https://dbbdev.rtp.raleigh.ibm.com:19443/dbb/ --id ADMIN --pw ADMIN --impactBuild
** Validating impact build results
**
** IMPACT BUILD TEST : PASSED **
**

** Running impact build test for changed file link/epsmlist.lnk
** Copying and committing /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/test/applications/MortgageApplication/link/epsmlist.lnk to /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/samples/MortgageApplication/link/epsmlist.lnk
** Executing /u/dbbAutomation/workspace/Automation_Jobs/DBB_All_BuildS/DBBZtoolkitTar/bin/groovyz /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/build.groovy --workspace /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/samples --application MortgageApplication --outDir /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/out --hlq IBMDBB.ZAPPB.BUILD --logEncoding UTF-8 --url https://dbbdev.rtp.raleigh.ibm.com:19443/dbb/ --id ADMIN --pw ADMIN --impactBuild
** Validating impact build results
**
** IMPACT BUILD TEST : PASSED **
**
Deleting impact build PDSEs [BMS, COBOL, LINK, COPY, BMS.COPY, DBRM, LOAD, MFS, OBJ, TFORMAT]

** Deleting test branch zAppBuildTesting
HEAD is now at 10aa9d5 edited program file
Your branch is up-to-date with 'origin/TestAutomation'.
Deleted branch zAppBuildTesting (was 10aa9d5).
On branch TestAutomation
Your branch is up-to-date with 'origin/TestAutomation'.

nothing to commit, working tree clean

** Build finished
```

Build with errors
```
** Executing test script impactBuild.groovy
** Processing changed files from impactBuild_changedFiles property : bms/epsmort.bms,cobol/epsmlist.cbl,copybook/epsmtout.cpy,link/epsmlist.lnk

** Running impact build test for changed file bms/epsmort.bms
** Copying and committing /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/test/applications/MortgageApplication/bms/epsmort.bms to /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/samples/MortgageApplication/bms/epsmort.bms
** Executing /u/dbbAutomation/workspace/Automation_Jobs/DBB_All_BuildS/DBBZtoolkitTar/bin/groovyz /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/build.groovy --workspace /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/samples --application MortgageApplication --outDir /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/out --hlq IBMDBB.ZAPPB.BUILD --logEncoding UTF-8 --url https://dbbdev.rtp.raleigh.ibm.com:19443/dbb/ --id ADMIN --pw ADMIN --impactBuild
** Validating impact build results
Deleting impact build PDSEs [BMS, COBOL, LINK, COPY, BMS.COPY, DBRM, LOAD, MFS, OBJ, TFORMAT]

** Deleting test branch zAppBuildTesting
HEAD is now at 1242001 edited program file
Your branch is up-to-date with 'origin/TestAutomation'.
Deleted branch zAppBuildTesting (was 1242001).
On branch TestAutomation
Your branch is up-to-date with 'origin/TestAutomation'.

nothing to commit, working tree clean

Caught: java.lang.AssertionError: *! IMPACT BUILD FAILED FOR bms/epsmort.bms
OUTPUT STREAM:

** Build start at 20210310.120307.003
** Repository client created for https://dbbdev.rtp.raleigh.ibm.com:19443/dbb/
** Build output located at /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/out/build.20210310.120307.003
** Build result created for BuildGroup:MortgageApplication-zAppBuildTesting BuildLabel:build.20210310.120307.003 at https://dbbdev.rtp.raleigh.ibm.com:19443/dbb/rest/buildResult/733
** --impactBuild option selected. Building impacted programs for application MortgageApplication 
** Writing build list file to /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/out/build.20210310.120307.003/buildList.txt
** Invoking build scripts according to build order: BMS.groovy,Cobol.groovy,LinkEdit.groovy
** Building files mapped to BMS.groovy script
*** Building file MortgageApplication/bms/epsmort.bms
** Building files mapped to Cobol.groovy script
*** Building file MortgageApplication/cobol/epscmort.cbl
*! The compile return code (12) for MortgageApplication/cobol/epscmort.cbl exceeded the maximum return code allowed (4)
** Writing build report data to /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/out/build.20210310.120307.003/BuildReport.json
** Writing build report to /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/out/build.20210310.120307.003/BuildReport.html
** Build ended at Wed Mar 10 12:03:27 EST 2021
** Build State : ERROR
** Total files processed : 2
** Total build time  : 20.273 seconds

** Build finished

. Expression: outputStream.contains(Build State : CLEAN)
java.lang.AssertionError: *! IMPACT BUILD FAILED FOR bms/epsmort.bms
OUTPUT STREAM:

** Build start at 20210310.120307.003
** Repository client created for https://dbbdev.rtp.raleigh.ibm.com:19443/dbb/
** Build output located at /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/out/build.20210310.120307.003
** Build result created for BuildGroup:MortgageApplication-zAppBuildTesting BuildLabel:build.20210310.120307.003 at https://dbbdev.rtp.raleigh.ibm.com:19443/dbb/rest/buildResult/733
** --impactBuild option selected. Building impacted programs for application MortgageApplication 
** Writing build list file to /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/out/build.20210310.120307.003/buildList.txt
** Invoking build scripts according to build order: BMS.groovy,Cobol.groovy,LinkEdit.groovy
** Building files mapped to BMS.groovy script
*** Building file MortgageApplication/bms/epsmort.bms
** Building files mapped to Cobol.groovy script
*** Building file MortgageApplication/cobol/epscmort.cbl
*! The compile return code (12) for MortgageApplication/cobol/epscmort.cbl exceeded the maximum return code allowed (4)
** Writing build report data to /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/out/build.20210310.120307.003/BuildReport.json
** Writing build report to /u/dbbAutomation/workspace/Automation_Jobs/ZAppBuildTest/ZAppBuild/dbb-zappbuild/out/build.20210310.120307.003/BuildReport.html
** Build ended at Wed Mar 10 12:03:27 EST 2021
** Build State : ERROR
** Total files processed : 2
** Total build time  : 20.273 seconds

** Build finished

. Expression: outputStream.contains(Build State : CLEAN)
	at impactBuild.validateImpactBuild(impactBuild.groovy:81)
	at impactBuild$_run_closure1.doCall(impactBuild.groovy:47)
	at impactBuild.run(impactBuild.groovy:34)
	at impactBuild$run.callCurrent(Unknown Source)
	at fullBuild$run.callCurrent(Unknown Source)
	at com.ibm.dbb.groovy.ScriptLoader._run(ScriptLoader.groovy:124)
	at com.ibm.dbb.groovy.ScriptLoader$_run$1.call(Unknown Source)
	at com.ibm.dbb.groovy.ScriptLoader$_run$1.call(Unknown Source)
	at com.ibm.dbb.groovy.ScriptLoader.runScript(ScriptLoader.groovy:81)
	at test$_run_closure1.doCall(test.groovy:22)
	at test.run(test.groovy:20)
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
