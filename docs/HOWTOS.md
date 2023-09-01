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

zAppBuild proposes 3 properties called `buildPropFiles`, `applicationDefaultPropFiles` and `applicationPropFiles`, each referencing a list of properties files that contain important parameters to configure the build process. The referenced properties files are loaded in the below order of precedence: 

1. `buildPropFiles`, managed in [build-conf/build.properties](../build-conf/build.properties), references properties files in the [build-conf](../build-conf/) directory for core zAppBuild settings for the language scripts such as system datasets, naming conventions of build datasets, dataset characteristics and various core properties of the build framework, like the reporting features.
2. `applicationDefaultPropFiles`, managed in [build-conf/build.properties](../build-conf/build.properties) as well, allows the user to define default application-related (and "language script"-related) properties, that are centrally managed and shared across applications.
3. `applicationPropFiles` is referencing properties files providing application-level settings. This property is managed in the applications' `application-conf/application.properties` file which is by default located in the applications' [application-conf](../samples/application-conf/) folder. These settings define compiler and link options, deploy types, script mappings and search path configurations for the DBB dependency and impact analysis. A sample is provided in [application-conf](../samples/application-conf/). The location where zAppBuild searches for the `application.properties` file is defined via the `applicationConfDir` setting that is managed in [build-conf/build.properties](../build-conf/build.properties). The default location mandates an `application-conf` folder including the `application.properties` file to be present in the applications' git repository.

The `buildPropFiles` and `applicationDefaultPropFiles` settings define enterprise-level, centrally-controlled properties, that are used by all applications using the build framework. They are shared across all applications.

Historically, a lot of application-level properties are configured and provided via the `applicationPropFiles` within the `application-conf` directory. However, users have reported that zAppBuild is exposing far too many properties to the application team, which also makes it hard to control the update process for new or modified properties. For example, most customers prefer to manage compiler and binder options in the centrally-controlled settings.

This how-to outlines the changes to centrally manage default application settings.

**Centrally defining application-related properties** 

Return codes, deploy types, script mappings or search path configurations for the DBB dependency and impact analysis can be centrally configured especially when application teams follow the similar repository layout and have similar application architectures. It is desireable to avoid having multiple copies of files with the same definitions spread across multiple locations.

Loading common properties files via the `applicationDefaultPropFiles` setting helps to achieve this easily. For instance, the below configuration of `applicationDefaultPropFiles` is loading properties that define the search path configurations, script mappings, and various language settings (such as compiler or linker options) that can be applied to all applications using zAppBuild.

```properties
..
# Extended list of applicationDefaultPropFiles to include
# default application settings for language scripts
applicationDefaultPropFiles=defaultzAppBuildConf.properties,\
default-application-conf/searchPaths.properties,\
default-application-conf/scriptMappings.properties,\
default-application-conf/Cobol.properties,\
default-application-conf/BMS.properties,\
default-application-conf/PLI.properties,\
default-application-conf/Transfer.properties,\
default-application-conf/LinkEdit.properties,\
default-application-conf/ZunitConfig.properties
..
```
This allows to reduce the necessary definitions within the `application-conf` directory of the application repository. This `application-conf` directory now only contains the `application.properties` and `file.properties` files to define the application-specific settings and exceptions, such as [file properties](../docs/FilePropertyManagement.md#dbb-file-properties) for particular build files:

```properties
# Reduced list of applicationPropFiles
applicationPropFiles=file.properties
```
Potentially, you could go farther and merge the properties defined in `file.properties` into the `application.properties` file.

If an application doesn't need to specify any application-specific settings, there is no need to create an `application.properties` file and zAppBuild will execute the build, even without finding the `application.properties` file expected at the `applicationConfDir` location.

Please note that moving property files to the central build framework implementation disables the capability to perform impactBuilds on a property change for these properties - see `impactBuildOnBuildPropertyChanges` setting at [default](../build-conf/defaultzAppBuildConf.properties).
