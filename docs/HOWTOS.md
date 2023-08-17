# How-to and Frequently Asked Questions

This page collects information to configure and use specific features of zAppBuild. Available How-tos:

- [How-to and Frequently Asked Questions](#how-to-and-frequently-asked-questions)
  - [Signing load modules and program objects](#signing-load-modules-and-program-objects)
  - [Reduce necessary **application-conf** configurations within the application repository](#reduce-necessary-application-conf-configurations-within-the-application-repository)


## Signing load modules and program objects

zAppBuild can be configured to automatically insert a reference of the version of the source code (the githash), which was used to build the executables. At the moment, the feature is only available for build files which are mapped to `Assembler.groovy`, `Cobol.groovy` and `PLI.groovy`. It is available for pipeline builds.

It leverages the [IDENTIFY statement](https://www.ibm.com/docs/en/zos/2.5.0?topic=reference-identify-statement) of the linkage editor.

**How to enable the signing**

The feature is enabled by default and is controlled via the `assembler_identifyLoad`, `cobol_identifyLoad` and the `pli_identifyLoad` properties managed in the corresponding property files of the language scripts.

**What to expect**

The feature will generate the linker statement `IDENTIFY` for the build file. It follows the structure:
```
   IDENTIFY BUILDMEMBER('APPLICATION/SHORT-GIT-HASH')
```

For instance for MortgageApplication/cobol/epscmort.cbl:
```
   IDENTIFY EPSCMORT('MortgageApplication/a21b6ab0')
```

With verbose tracing of zAppBuild, you can find the information in the build log:

```
*** Cobol compiler parms for MortgageApplication/cobol/epscmort.cbl = LIB,CICS,SQL
*** Link-Edit parms for MortgageApplication/cobol/epscmort.cbl = MAP,RENT,COMPAT(PM5),SSI=0f2caa66
*** Generated linkcard input stream: 
   IDENTIFY EPSCMORT('MortgageApplication/a21b6ab0')
```

Alternatively, you can also find the information in the link listing:
```
1z/OS V2 R4 BINDER     11:29:22 MONDAY JUNE 19, 2023
 BATCH EMULATOR  JOB(DBEHM4  ) STEP(*OMVSEX ) PGM= IEWBLINK
 IEW2278I B352 INVOCATION PARAMETERS - MAP,RENT,COMPAT(PM5),SSI=a21b6ab0
 IEW2322I 1220  1     IDENTIFY EPSCMORT('MortgageApplication/a21b6ab0')

1                         *** M O D U L E  M A P ***
...
```

**Known limitations**
* It does not generate the information for link cards, that are managed in the git repository
* It does not manage scenarios where multiple object decks are assembled into the load module


**When is this useful?**

If you want to quickly understand the version of the source code which is running in a runtime, you can use the [amblist service aid](https://www.ibm.com/docs/en/zos/2.5.0?topic=sets-amblist-service-aid) to retrieve the information for a load module or a program object. The IDENTIFY information can help you *to understand to which application the module belongs and which version of the source code was used to build the executed module.*

The following sample JCL showcases the use of AMBLIST to display load modules' or program objects' information:

```jcl
//AMBLIST1 JOB (8550,030A,20,30),'AMBLIST JOB', 
// CLASS=A,MSGCLASS=T,NOTIFY=&SYSUID
//*
//ABML EXEC PGM=AMBLIST
//SYSPRINT DD SYSOUT=*
//SYSOUT DD SYSOUT=*
//STEPLIB DD DSN=SYS1.LINKLIB,DISP=SHR
//*MYLOAD DD DSN=JENKINS.ZAPP.CLEAN.LOAD,DISP=SHR
//MYLOAD DD DSN=IBMUSER.DBB.BUILD.LOAD,DISP=SHR
//SYSIN DD *
    LISTIDR DDN=MYLOAD,MEMBER=EPSCMORT
/*
//*
```

In the output of amblist, locate the user data section and find the data that the linkage-editor inserted:

```
          DATE         USER DATA
CSECT:    EPSCMORT
          06/19/2023   MortgageApplication/a21b6ab0
```

## Reduce necessary **application-conf** configurations within the application repository

**The 3 properties for loading configuration files to configure the build framework**

zAppBuild provides 3 properties `buildPropFiles`, `applicationDefaultPropFiles` and the `applicationPropFiles` that each reference a list of properties files, which contain important configuration parameters to configure the build process. The referenced property files are loaded the below order of precedence: 

* `buildPropFiles` managed in [build-conf/build.properties](../build-conf/build.properties) references properties files in [build-conf](../build-conf/) for core zAppBuild settings for the language scripts such as system datasets, naming conventions of build datasets, dataset characteristics and various core properties of the build framework as well, like the reporting facility.
* The property `applicationDefaultPropFiles` is managed in [build-conf/build.properties](../build-conf/build.properties) as well and allows the user to define default application (and also language script specific related) properties, that are centrally managed. 
* `applicationPropFiles` is providing application specific settings. This property is managed is within the applications' `application-conf/application.properties` file of the applications [application-conf](../samples/application-conf/) repository. These settings define compiler and link options, deploy types. A sample is provided in [application-conf](../samples/application-conf/). `application-conf/application.properties` also defines the search path configurations for the DBB dependency and impact analysis. The default location for zAppBuild to locate the `application-conf` directory including the `application.properties` file is defined via the `applicationConfDir` setting that is managed in [build-conf/build.properties](../build-conf/build.properties). The default mandates the a `application-conf` directory to be present in the applications' git repository.

Historically, a lot of application-level build properties are configured and provided via the `applicationPropFiles` within the `application-conf` directory. However, users have reported that zAppBuild is exposing far too many properties to the application team. 

This how-to outlines the changes to manage default application settings more centrally. 

**Centrally defining application related properties** 

Return codes, deploy types or even search path configurations for the DBB dependency and impact analysis can rather be centrally configured especially when application teams follow the similar repository layout and application architecture. This avoids having multiple copies of files with the same definitions spread across multiple locations.

Loading common properties files or properties via the `applicationDefaultPropFiles` setting, helps to achieve this easily. For instance, the below configuration of `applicationDefaultPropFiles` is loading properties that define the search path configurations, script mappings, and various language settings.

```properties
applicationDefaultPropFiles=defaultzAppBuildConf.properties,\
default-application-conf/searchPaths.properties,\
default-application-conf/scriptMappings.properties,\
default-application-conf/Cobol.properties,\
default-application-conf/BMS.properties,\
default-application-conf/PLI.properties,\
default-application-conf/Transfer.properties,\
default-application-conf/LinkEdit.properties,\
default-application-conf/ZunitConfig.properties
```
This allows to reduce the necessary definitions within the `application-conf` directory within the application repository, which now only contains the `application.properties` and `file.properties` files to define the application specific settings and exceptions, such as [file properties](../docs/FilePropertyManagement.md#dbb-file-properties) for a particular build file.

If the application does not need to specify any application specifics, zAppBuild executes the build even without finding an `application-conf/application.properties` at the `applicationConfDir` location.