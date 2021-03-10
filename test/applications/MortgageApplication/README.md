# MortgageApplication
This contains the modified program files to execute an impact build of the MortgageApplication sample that's built by zAppBuild. The structure of this folder is the same the Mortagage Application in ZAppBuild. In addition to what provided here, modified files of other supported languages for testing this application can be added.

#### Refers for additional information about testing applications using zAppBuild.
- [TestGroovy/README.md](/test/README.md) 
- [Full-Impact/README.md](/test/testScripts/README.md) 

# test.properties
This properties file is invoked by test.groovy and it contains the properties below

Property | Description
--- | ---
test_testOrder | Comma separated list of the test script processing order
fullBuild_expectedFilesBuilt | List of programs should be built for a full build for this application
fullBuild_datasetsToCleanUp | List of source datasets (LLQ) that should be deleted during fullBuild.groovy cleanUp
impactBuild_changedFiles | List of changed source files to test impact builds
impactBuild_datasetsToCleanUp | List of source datasets (LLQ) that should be deleted during impactBuild.groovy cleanUp
impactBuild_expectedFilesBuilt | Uses file properties to associate expected files built to changed files
