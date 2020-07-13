# MortgageApplication
#
# Added by Regi - July 13, 2020
# 1. Created JENKSFILE at /Mortgage

# 2. Update datasets.properties  at    /zAppBuild/build-conf
# --> specify correct data set names

# 3. Updated build.properties at /zAppBuild/build-conf
# --> # updated applicationConfRootDir=${zAppBuildDir}/samples/
# ** otherwise..
# ** appConf = /var/jenkins/workspace/dbb-zappbuild-1/dbb-zappbuild-1/samples/MortgageApplication/MortgageApplication/application-conf
# Caught: com.ibm.dbb.build.ValidationException: BGZTK0045E Could not load because the file :
#  /var/jenkins/workspace/dbb-zappbuild-1/dbb-zappbuild-1/samples/MortgageApplication/MortgageApplication/application-conf/application.properties
#  does not exist in this directory.
#
# 4. Updated build.properties at /zAppBuild/build-conf
# --> Updated dbb.RepositoryClient.url=https://10.1.1.1:11043/dbb
#
# 5. Be sure that MortgageApplication/application-conf/Cobol.properties has the option below:
# cobol_compileDebugParms=TEST
# ----> For UCD
# 6. Add deploy.groovy to zAppBuild/utilities/
# 7. Replace build.groovy by build2.groovy
# 8. Add /utilities/PackageUtilities.groovy to  zAppBuild/utilities/
#
#
#
This version of the MortgageApplication sample is designed to be built by zAppBuild.

**Example showing how to build all programs in MortgageApplication**
```
$DBB_HOME/bin/groovyz build.groovy --workspace /u/build/repos/dbb-zappbuild/samples --application MortgageApplication --outDir /u/build/out --hlq BUILD.MORTAPP --fullBuild
```
See [BUILD.md](../../../BUILD.md) for additional information about building applications using zAppBuild.
