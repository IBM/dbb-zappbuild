# HelloWorld
This contains program files and build configuration to execute the test scenarios with the HelloWorld appplication. 

Modified files of other supported languages for testing this application can be added.

#### References for additional information about testing applications using zAppBuild
- [zAppBuild test framework documentation](/test/README.md) 
- [Available test scenarios](/test/testScripts/README.md) that can be configured for a test application

# test.properties
This application specify properties file is invoked by [test.groovy](/test/test.groovy) and it contains the below properties to configure the test scenarios for testing the HelloWorld application.

Property | Description
--- | ---
test_testOrder | Comma separated list of the test script processing order
fullBuild_* | Test properties for `--fullBuild` build scenario
fullBuild_debug* | Test properties for `--fullBuild --debug` build scenario
reset_* | Properties for `--reset` option to cleanup DBB metadatastore 


