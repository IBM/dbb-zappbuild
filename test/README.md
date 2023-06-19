# zAppBuild/test
Test folder is designed to help test samples like the Mortgage Application against ZAppBuild.

## Repository Legend
Folder/File | Description | Documentation Link
--- | --- | ---
applications/MortgageApplication | This folder contains modified language scripts used to execute impact build by replacing these modified files with the original language files | [MortgageApplication](applications/MortgageApplication/README.md)
applications/HelloWorld | This folder contains sample programs for Assembler | [HelloWorld](applications/HelloWorld/)
test.groovy  | This is the main build script that is called to start the test process | [test.groovy](/test/README.md#testing-applications-with-zappbuild)
testScripts  | This folder contains test scripts to execute full and impact builds | [testScripts](/test/testScripts/README.md)

# Testing Applications with zAppBuild
The main script for testing applications against zAppBuild is `test.groovy`. It takes most of its input from the command line to run full and impact builds. `test.groovy` once executed from the command line calls [fullBuild.groovy](/test/testScripts/fullBuild.groovy) and [impactBuild.groovy](/test/testScripts/impactBuild.groovy) scripts to perform an end to end test on the given feature branch with the program specified for impact build. 

test.groovy script has required arguments that must be present during each invocation:
* --branch <arg> - zAppBuild branch to test
* --app <arg> - Application that is being tested (example: MortgageApplication)
* --hlq <arg> - HLQ for dataset reation / deletion (example: USER.BUILD)

test.groovy script has optional argument that can be present during each invocation
* --id <arg> - Db2 user id for the MetadataStore
* --url <arg> - Db2 JDBC URL for the MetadataStore.
* --pw <arg> - Db2 password (encrypted with DBB Password Utility) for the MetadataStore
* --pwFile <arg> - Absolute or relative (from workspace) path to file containing Db2 password
* --verbose <arg> - Flag indicating to print trace statements
* --propFiles <arg> - Absolute path to the location of the datasets.properties and other configuration files.
* --outDir <arg> - Absolute path to out directory

# Examples of running an end to end test:

It is recommended to leverage the `--propFiles` to pass in any environment specific property files for your environment, such as the `dataset.properties`, the `build.properties` to configure the Metadatastore connection. 
This avoids that you need to commit any environment specific configuration to the branch: 

```
$DBB_HOME/bin/groovyz ${repoPath}/test/test.groovy  \ 
                      --branch testBranch   \ 
					  --app MortgageApplication  \ 
					  --hlq USER.BUILD  \ 
					  --url jdbc:db2://system1.company.com:5040/DBB1  \ 
					  --id JDBCID  \ 
					  --pwFile /var/dbb/pwdFile.txt  \ 
					  --propFiles /pathToDatasets/datasets.properties  \ 
					  --outDir /pathToOutDir/out
```

# Examples of outputs to be expected:

Successful test run
```
.. <test properties>
** Creating and checking out branch zAppBuildTesting
Your branch is up to date with 'origin/testBranch'.
On branch zAppBuildTesting
nothing to commit, working tree clean

** Invoking test scripts according to test list order: resetBuild.groovy,fullBuild.groovy,fullBuild_debug.groovy,resetBuild.groovy

**************************************************************
** Executing test script resetBuild.groovy
**************************************************************
** DBB_HOME = /usr/lpp/dbb/v2r0
** Executing /usr/lpp/dbb/v2r0/bin/groovyz /u/ibmuser/test-zapp/dbb-zappbuild/build.groovy --workspace /u/ibmuser/test-zapp/dbb-zappbuild/test/applications --application HelloWorld --outDir /u/ibmuser/test-zapp-app-out/testframework_out --hlq USER.DBB.TEST.BUILD --logEncoding UTF-8 --url jdbc:db2://10.3.20.201:4740/MOPDBC0 --id ibmuser  --pwFile /var/dbb/config/db2-pwd-file.xml --verbose --propFiles /var/dbb/dbb-zappbuild-config/build.properties,/var/dbb/dbb-zappbuild-config/datasets.properties,/var/dbb/dbb-zappbuild-config/ibmuser.properties --reset
** Validating reset build
**
** RESET OF THE BUILD : PASSED **
**

**************************************************************
** Executing test script fullBuild.groovy
**************************************************************
** DBB_HOME = /usr/lpp/dbb/v2r0
** Executing /usr/lpp/dbb/v2r0/bin/groovyz /u/ibmuser/test-zapp/dbb-zappbuild/build.groovy --workspace /u/ibmuser/test-zapp/dbb-zappbuild/test/applications --application HelloWorld --outDir /u/ibmuser/test-zapp-app-out/testframework_out --hlq USER.DBB.TEST.BUILD --logEncoding UTF-8 --url jdbc:db2://10.3.20.201:4740/MOPDBC0 --id ibmuser  --pwFile /var/dbb/config/db2-pwd-file.xml --verbose --propFiles /var/dbb/dbb-zappbuild-config/build.properties,/var/dbb/dbb-zappbuild-config/datasets.properties,/var/dbb/dbb-zappbuild-config/ibmuser.properties --fullBuild
** Validating full build results
**
** FULL BUILD TEST : PASSED **
**

** Deleting build PDSEs [ASM, MACRO, DBRM, OBJ, LOAD]
** Deleting 'USER.DBB.TEST.BUILD.ASM'
** Deleting 'USER.DBB.TEST.BUILD.MACRO'
** Deleting 'USER.DBB.TEST.BUILD.DBRM'
** Deleting 'USER.DBB.TEST.BUILD.OBJ'
** Deleting 'USER.DBB.TEST.BUILD.LOAD'

**************************************************************
** Executing test script fullBuild_debug.groovy
**************************************************************
** DBB_HOME = /usr/lpp/dbb/v2r0
** Executing /usr/lpp/dbb/v2r0/bin/groovyz /u/ibmuser/test-zapp/dbb-zappbuild/build.groovy --workspace /u/ibmuser/test-zapp/dbb-zappbuild/test/applications --application HelloWorld --outDir /u/ibmuser/test-zapp-app-out/testframework_out --hlq USER.DBB.TEST.BUILD --logEncoding UTF-8 --url jdbc:db2://10.3.20.201:4740/MOPDBC0 --id ibmuser  --pwFile /var/dbb/config/db2-pwd-file.xml --verbose --propFiles /var/dbb/dbb-zappbuild-config/build.properties,/var/dbb/dbb-zappbuild-config/datasets.properties,/var/dbb/dbb-zappbuild-config/ibmuser.properties --fullBuild --debug
** Validating full build results
**
** FULL BUILD TEST : PASSED **
**

** Deleting build PDSEs [ASM, MACRO, DBRM, OBJ, LOAD, SYSADATA, EQALANGX]
** Deleting 'USER.DBB.TEST.BUILD.ASM'
** Deleting 'USER.DBB.TEST.BUILD.MACRO'
** Deleting 'USER.DBB.TEST.BUILD.DBRM'
** Deleting 'USER.DBB.TEST.BUILD.OBJ'
** Deleting 'USER.DBB.TEST.BUILD.LOAD'
** Deleting 'USER.DBB.TEST.BUILD.SYSADATA'
** Deleting 'USER.DBB.TEST.BUILD.EQALANGX'

**************************************************************
** Executing test script resetBuild.groovy
**************************************************************
** DBB_HOME = /usr/lpp/dbb/v2r0
** Executing /usr/lpp/dbb/v2r0/bin/groovyz /u/ibmuser/test-zapp/dbb-zappbuild/build.groovy --workspace /u/ibmuser/test-zapp/dbb-zappbuild/test/applications --application HelloWorld --outDir /u/ibmuser/test-zapp-app-out/testframework_out --hlq USER.DBB.TEST.BUILD --logEncoding UTF-8 --url jdbc:db2://10.3.20.201:4740/MOPDBC0 --id ibmuser  --pwFile /var/dbb/config/db2-pwd-file.xml --verbose --propFiles /var/dbb/dbb-zappbuild-config/build.properties,/var/dbb/dbb-zappbuild-config/datasets.properties,/var/dbb/dbb-zappbuild-config/ibmuser.properties --reset
** Validating reset build
**
** RESET OF THE BUILD : PASSED **
**

** Deleting test branch zAppBuildTesting
HEAD is now at 68f9a2d make the credentials optional
Your branch is up to date with 'origin/testBranch'.
Deleted branch zAppBuildTesting (was 68f9a2d).
On branch testBranch
Your branch is up to date with 'origin/testBranch'.

nothing to commit, working tree clean


================================================================================================
* ZAPPBUILD TESTFRAMEWORK COMPLETED.
   All tests (resetBuild.groovy,fullBuild.groovy,fullBuild_debug.groovy,resetBuild.groovy) completed successfully.
================================================================================================
** Build finished
```

When an error is detected, the test framework will print the entire log of the failed test (failed assertion) for the analysis by the build script engineer:

```

...
** Executing test script impactBuild_renaming.groovy
** DBB_HOME = /usr/lpp/dbb/v2r0
** Rename cobol/epscsmrt.cbl to cobol/epscsmr2.cbl

** Running impact after renaming file cobol/epscsmrt.cbl to cobol/epscsmr2.cbl
** Executing /usr/lpp/dbb/v2r0/bin/groovyz /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/build.groovy --workspace /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/samples --application MortgageApplication --outDir /var/jenkins/workspace/dbb-zappbuild-testframework-withParms/logs_testframework_MortgageApp --hlq JENKINS.DBB.TEST.BUILD.T367ENHA --logEncoding UTF-8 --url jdbc:db2:somelocation --id DBEHM --pwFile /var/dbb/config/db2-pwd-file.xml --verbose --propFiles /var/dbb/dbb-zappbuild-config/build.properties,/var/dbb/dbb-zappbuild-config/datasets.properties,/var/dbb/dbb-zappbuild-config/dbb-db2-metadatastore-jenkins.properties --impactBuild
** Validating impact build results
Deleting impact build PDSEs [BMS, COBOL, LINK, COPY, BMS.COPY, DBRM, LOAD, MFS, OBJ, TFORMAT]

***
**START OF FAILED IMPACT BUILD TEST RESULTS**

*FAILED IMPACT BUILD TEST RESULTS*
[*! IMPACT BUILD FOR cobol/epscsmrt.cbl TOTAL FILES PROCESSED ARE NOT EQUAL TO 1
OUTPUT STREAM:

** Build start at 20230615.012609.026
** Input args = /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/samples --application MortgageApplication --outDir /var/jenkins/workspace/dbb-zappbuild-testframework-withParms/logs_testframework_MortgageApp --hlq JENKINS.DBB.TEST.BUILD.T367ENHA --logEncoding UTF-8 --url jdbc:db2:somelocation --id DBEHM --pwFile /var/dbb/config/db2-pwd-file.xml --verbose --propFiles /var/dbb/dbb-zappbuild-config/build.properties,/var/dbb/dbb-zappbuild-config/datasets.properties,/var/dbb/dbb-zappbuild-config/dbb-db2-metadatastore-jenkins.properties --impactBuild
** Loading property file /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/build-conf/datasets.properties
** Loading property file /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/build-conf/dependencyReport.properties
** Loading property file /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/build-conf/Assembler.properties
** Loading property file /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/build-conf/BMS.properties
** Loading property file /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/build-conf/MFS.properties
** Loading property file /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/build-conf/PSBgen.properties
** Loading property file /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/build-conf/DBDgen.properties
** Loading property file /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/build-conf/ACBgen.properties
** Loading property file /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/build-conf/Cobol.properties
** Loading property file /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/build-conf/LinkEdit.properties
** Loading property file /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/build-conf/PLI.properties
** Loading property file /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/build-conf/REXX.properties
** Loading property file /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/build-conf/ZunitConfig.properties
** Loading property file /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/build-conf/Transfer.properties
** Loading property file /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/build-conf/defaultzAppBuildConf.properties
** appConf = /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/samples/MortgageApplication/application-conf
** Loading property file /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/samples/MortgageApplication/application-conf/file.properties
** Loading property file /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/samples/MortgageApplication/application-conf/BMS.properties
** Loading property file /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/samples/MortgageApplication/application-conf/Cobol.properties
** Loading property file /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/samples/MortgageApplication/application-conf/LinkEdit.properties
** Loading property file /ZT01/var/jenkins/workspace/dbb-zappbuild-testframework-withParms/samples/MortgageApplication/application-conf/languageConfigurationMapping.properties
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
-u, --url       Db2 JDBC URL for the MetadataStore.
            	Example: jdbc:db2:<Db2 server location>
-i, --id        Db2 user id for the MetadataStore
-p, --pw        Db2 password (encrypted with DBB Password Utility) for the MetadataStore
-P, --pwFile    Absolute or relative (from workspace) path to file containing Db2 password
-f, --propFiles Absolute path to the location of the datasets.properties 
-o, --outDir    Absolute path to out directory
```
