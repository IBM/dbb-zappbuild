
# test.properties
This is a sample application specify properties file invoked by [test.groovy](/test/test.groovy) and it contains the properties below

Property | Description
--- | ---
test_testOrder | Comma separated list of the test script processing order
fullBuild_expectedFilesBuilt | List of programs should be built for a full build for an application
fullBuild_datasetsToCleanUp | List of source datasets (LLQ) that should be deleted during fullBuild.groovy cleanUp
impactBuild_changedFiles | List of changed source files to test impact builds for an application
impactBuild_datasetsToCleanUp | List of source datasets (LLQ) that should be deleted during impactBuild.groovy cleanUp
impactBuild_expectedFilesBuilt | Uses file properties to associate expected files built to changed files
