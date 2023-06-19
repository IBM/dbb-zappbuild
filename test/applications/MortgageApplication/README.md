# MortgageApplication
This contains the modified program files and build configuration to execute an impact build of the MortgageApplication sample that's built by zAppBuild. 

The structure of this folder is the same the MortagageApplication in zAppBuild. Modified files of other supported languages for testing this application can be added.

#### References for additional information about testing applications using zAppBuild
- [zAppBuild test framework documentation](/test/README.md) 
- [Available test scenarios](/test/testScripts/README.md) that can be configured for a test application

# test.properties
This application specify properties file is invoked by [test.groovy](/test/test.groovy) and it contains the below properties to configure the test scenarios for testing the mortgage application.

Property | Description
--- | ---
test_testOrder | Comma separated list of the test script processing order
fullBuild_* | Test properties for `--fullBuild` build scenario
fullBuild_languageConfigurations_* | Test properties for full build scenario with [language configuration settings](/docs/FilePropertyManagement.md) 
mergeBuild_* | Test properties for `--mergeBuild` build scenario
impactBuild_* | Test properties for `--impactBuild` build scenario
impactBuild_rename* | Test properties for `--impactBuild` build scenario when renaming a file
impactBuild_properties* | Test properties for `--impactBuild` build scenario on property changes
impactBuild_deletion* | Test properties for `--impactBuild` build scenario on deleting files
impactBuild_preview* | Test properties for `--impactBuild --preview` build scenario
impactBuild_preview* | Test properties for `--impactBuild --preview` build scenario


