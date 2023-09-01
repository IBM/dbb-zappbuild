# zAppBuild
zAppBuild is a generic build solution for building z/OS applications using Apache Groovy build scripts and IBM Dependency Based Build (DBB) APIs.

## Resources
* [IBM Dependency Based Build Product Page](https://www.ibm.com/products/dependency-based-build)
* [IBM DBB Knowledge Center](https://www.ibm.com/docs/en/dbb/1.1.0)
* [IBM/dbb Repository](https://github.com/IBM/dbb/)
* [IBM IDZ Community](https://community.ibm.com/community/user/ibmz-and-linuxone/groups/topic-home?CommunityKey=f461c55d-159c-4a94-b708-9f7fe11d972b)
* [IBM DBB Community](https://community.ibm.com/community/user/ibmz-and-linuxone/groups/topic-home?CommunityKey=20c9b889-9450-4ab6-8f11-8a5eb2b3342d)


## Contributing
For instructions on how to contribute enhancements and bug fixes to zAppBuild, please read the [Contributions Guidelines](CONTRIBUTIONS.md).

## How zAppBuild works
The zAppBuild repository is intended to be cloned to a single location on Unix Systems Services (USS) and used to build all of your z/OS applications. Global configuration properties are configured in the properties files in the [build-conf](build-conf/) directory. Specifying application-level properties is done by simply copying the supplied `application-conf` folder (located in the [samples folder](samples)) to the application source repository you want to build and then verify/update the contained default configuration property values to ensure they meet the build requirements of your application. See the included [MortgageApplication](samples/MortgageApplication) sample for an example of an application that has been modified to be built by zAppBuild.  

