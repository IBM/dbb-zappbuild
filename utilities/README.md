# Utility Files
This folder contains common utility and helper files used by the zAppBuild `build.groovy` script and language scripts.

File | Description
--- | --- 
[BindUtilities.groovy](BindUtilities.groovy) | Use for the DB2 binding.
[BuildReportUtilities.groovy](BuildReportUtilities.groovy) | Helper to populate additional build report entries.
[BuildUtilities.groovy](BuildUtilities.groovy) | Common build utility methods.
[DatasetValidationUtilities.groovy](DatasetValidationUtilities.groovy) | Helper to validate that system dataset definitions are correctly defined.
[DependencyScannerUtilities.groovy](DependencyScannerUtilities.groovy) | Populates the DependencyScannerRegistry and returns the mapped dependency scanner for a build file.
[FilePropUtilities.groovy](FilePropUtilities.groovy) | Helper util to load and validate file level properties overrides.
[GitUtilities.groovy](GitUtilities.groovy) | Git command methods.
[ImpactUtilities.groovy](ImpactUtilities.groovy) | Methods used for ImpactBuilds.

## Stand-alone utils

A few utilities are used both in the zAppBuild build process, but can also be invoked stand-alone:
* Dataset Validation Utilities
* Bind Utilities

### Dataset Validation Utilities

The [DatasetValidationUtilities.groovy](DatasetValidationUtilities.groovy) can be used as a standalone script to validate that the configured system datasets such as the [dataset.properties](../build-conf/datasets.properties) are available on the system where this script is executed. Using the script as a standalone utility targets situations where builds runs into allocation exceptions such as `BGZTK0016E An error occurred running BPXWDYN command 'alloc dd(TASKLIB) dsn(COBOL.V6R1.SIGYCOM) shr'` and helps the build script administrator to customize zAppBuild. 

#### Usage

```
groovyz /u/ibmuser/zAppBuild/utilities/DatasetValidationUtilities.groovy --help

usage: DatasetValidationUtilites.groovy [options]

 -d,--systemDatasetDefinition <arg>   List of property files containing
                                      system dataset definitions.
 -h,--help                            Flag to print the Help message.
```

#### Sample invocation

```
groovyz /u/ibmuser/zAppBuild/utilities/DatasetValidationUtilities.groovy -d /var/dbb/dbb-zappbuild-config/datasets.properties

** The dataset PLI.V5R2.SIBMZCMP referenced for property IBMZPLI_V52 was found.
*! No dataset defined for property IBMZPLI_V51 specified in /var/dbb/dbb-zappbuild-config/datasets.properties.
** The dataset WMQ.V9R2M4.SCSQPLIC referenced for property SCSQPLIC was found.
** The dataset COBOL.V6R1.SIGYCOMP referenced for property SIGYCOMP_V6 was found.
** The dataset CICSTS.V5R4.CICS.SDFHCOB referenced for property SDFHCOB was found.
*! No dataset defined for property SIGYCOMP_V4 specified in /var/dbb/dbb-zappbuild-config/datasets.properties.
** The dataset HLASM.SASMMOD1 referenced for property SASMMOD1 was found.
** The dataset SYS1.MACLIB referenced for property MACLIB was found.
** The dataset PDTCC.V1R8.SIPVMODA referenced for property PDTCCMOD was found.
** The dataset CICSTS.V5R4.CICS.SDFHLOAD referenced for property SDFHLOAD was found.
** The dataset CICSTS.V5R4.CICS.SDFHMAC referenced for property SDFHMAC was found.
** The dataset CEE.SCEEMAC referenced for property SCEEMAC was found.
** The dataset WMQ.V9R2M4.SCSQCOBC referenced for property SCSQCOBC was found.
** The dataset IMS.V15R1.SDFSMAC referenced for property SDFSMAC was found.
** The dataset RDZ.V14R1.SFELLOAD referenced for property SFELLOAD was found.
** The dataset DBC0CFG.DB2.V12.SDSNLOAD referenced for property SDSNLOAD was found.
** The dataset CICSTS.V5R4.CICS.SDFHPL1 referenced for property SDFHPL1 was found.
** The dataset WMQ.V9R2M4.SCSQLOAD referenced for property SCSQLOAD was found.
** The dataset IMSCFG.IMSC.REFERAL referenced for property REFERAL was found.
** The dataset DEBUG.V14R1.SEQAMOD referenced for property SEQAMOD was found.
** The dataset DBC0CFG.SDSNEXIT referenced for property SDSNEXIT was found.
** The dataset IMS.V15R1.SDFSRESL referenced for property SDFSRESL was found.
** The dataset RATCFG.ZUNIT.SBZUSAMP referenced for property SBZUSAMP was found.
** The dataset CEE.SCEELKED referenced for property SCEELKED was found.
```
