# How-to and Frequently Asked Questions

This page collects information to configure and use specific features of zAppBuild. Available How-tos:

- [How-to and Frequently Asked Questions](#how-to-and-frequently-asked-questions)
  - [Signing load modules and program objects](#signing-load-modules-and-program-objects)


## Signing load modules and program objects

zAppBuild is configured to automatically insert a reference of the version of the source code (the githash), which was used to build the executables. At the moment, the feature is only available for build files which are mapped to `Assembler.groovy`, `Cobol.groovy` and `PLI.groovy`. It is available for pipeline builds.

It leverages the [IDENTIFY statement](https://www.ibm.com/docs/en/zos/2.5.0?topic=reference-identify-statement) of the linkage editor.

**How to enable the signing**

The feature is enabled by default and is controlled via the `assembler_identifyLoad`, `cobol_identifyLoad` and the `cobol_identifyLoad` properties managed the corresponding property files of the language scripts.

**What to expect**

The feature, will generate the linker statement `IDENTIFY` for the build file. It follows the structure:
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


**When is this useful**

If you want to quickly understand the version of the source code which is running in a runtime, you can use the [amblist service aid](https://www.ibm.com/docs/en/zos/2.5.0?topic=sets-amblist-service-aid) to retrieve the information for a load module to answer which application system the modules belongs to and which version of the code is currently executed.

Using the sample JCL, helps to obtain the information

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