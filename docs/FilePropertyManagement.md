# File Property Management

## Table of contents

- [File Property Management](#file-property-management)
  - [Table of contents](#table-of-contents)
  - [Introduction](#introduction)
  - [Overriding build properties with DBB and zAppBuild](#overriding-build-properties-with-dbb-and-zappbuild)
  - [Default properties](#default-properties)
  - [Overriding properties](#overriding-properties)
    - [DBB file properties](#dbb-file-properties)
    - [Individual File Property](#individual-file-property)
    - [Language Definition Mapping](#language-definition-mapping)

## Introduction

Building mainframe application programs requires configuring various parameters and options for the different build steps, such as the pre-compile, compile, or link-edit step. Additionally, an application can contain COBOL programs that need to be link edited with the option `NCAL` or without the option `NCAL` for different purposes. An override may be required for any build parameter for the various build steps like compile parameters, bind parameters, link edit parameters, and so on.

In existing mainframe toolchains, this customization is performed by assigning a type to the application artifact. This *type* is often used to specify the build parameters and options for the entire **subgroup** of application artifacts. Obviously, it allows that an application program might have some individual overrides as well.

Generally, think of these settings as either as a default value or as an override of the build parameter for an application artifact.

## Overriding build properties with DBB and zAppBuild

Dependency Based Build comes with its own [APIs](https://www.ibm.com/docs/api/v1/content/SS6T76_2.0.0/javadoc/index.html) to manage build properties, which extends the standard key-value pair strategy of *java.util.Properties*. DBB refers to the term *File properties* to allow overriding the corresponding default build properties using the DBB file property path syntax. See [IBM DBB Docs - Build properties](https://www.ibm.com/docs/en/dbb/2.0.0?topic=apis-build-properties#file-properties) for more details.

zAppBuild's implementation supports overriding the majority of build properties. The full list can be viewed at [application-conf/README.md](../samples/application-conf/README.md).

zAppBuild leverages DBB's API and allows you to define build parameters on three different levels for each language script:

  1. General defaults in corresponding the language property files - for example, defining the compile options for building COBOL programs in [application-conf/Cobol.properties](../samples/application-conf/Cobol.properties). Property keys make use of a language prefix, for instance for COBOL programs this is `cobol_`.
  2. A group definition, to overriding the default by specifying using DBB's file property syntax using a pattern filter; or through a mapping to a language definition file to override the defaults, like [application-conf/languageDefinitionMapping.properties](../samples/MortgageApplication/application-conf/languageDefinitionMapping.properties). 
  3. An individual file-level definition to override a single parameter leveraging Dependency Based Builds file properties syntax using [application-conf/file.properties](../samples/application-conf/file.properties) or multiple parameters for a specific file using the individual property file, like [epsmlist.cbl.properties](../samples/MortgageApplication/properties/epsmlist.cbl.properties).

zAppBuild comes with various strategies that can be combined via an order of precedence. The following table summarizes the strategies for overriding file properties from highest to lowest precedence:

||Strategy|Use case|
|-|-|-|
|1.|Individual file properties|Override one or multiple build parameters for individual files|
|2.|Language definition mapping|Override and define one or multiple build parameters for a group of mapped application artifacts|
|3.|DBB file properties|Override a single build parameter for individual files or a grouped list of application artifacts|
|4.|Default properties|General build properties used when no overrides are defined|

To understand the order of precedence, think of this as a merge of the property configurations. For example, if both individual file properties and a language definition mapping are configured for a file, then the properties defined through the individual file property definition take precedence, but are also merged with other properties defined by the language definition mapping and the default properties.



## Default properties

Default properties can be set in the corresponding language properties file.  For example, the COBOL file properties can be set in [application-conf/Cobol.properties](../samples/application-conf/Cobol.properties), while the Assembler file properties can be set in [application-conf/Assembler.properties](../samples/application-conf/Assembler.properties), and so on.

zAppBuild is currently proposing to store these properties within the application repository and let the application team have control over these files. If you are looking for a more centralized way to manage the default options for all applications, you can move these definitions into the zAppBuild build framework itself by either merging them into the appropriate language property file under [build-conf](../build-conf/) or store them in a separate directory within zAppBuild itself and leverage the `applicationConfRootDir` property in [build-conf/build.propertiees](../build-conf/build.properties).

## Overriding properties

The following section describes the various strategies to override the default value. The DBB file property syntax is the most commonly used approach within the zAppBuild samples. Two alternate approaches to override build properties are implemented in zAppBuild to serve the different needs and requirement to simplify the adoption of zAppBuild by either leveraging an individual properties file per application artifact or by defining a language definition mapping.

### DBB file properties

The most common way to override a single build parameter, makes use of the DBB file property syntax.

For example, to use a DBB file property to override the default COBOL compile parameters for build artifact `MortgageApplication/cobol/epsnbrvl.cbl`, follow the DBB file property syntax in `file.properties`, which is the default property file for configuring file property overrides:

```properties
cobol_compileParms=LIB,SOURCE :: **/cobol/epsnbrvl.cbl
```

For merging, rather than overriding, the property value of this file-level override with the default setting, you can specify the following syntax:

```properties
cobol_compileParms=${cobol_compileParms},SOURCE :: **/cobol/epsnbrvl.cbl
```

The file property path syntax also allows you to override the a build parameter for a set of files using wildcards. For example, let's assume that you are storing all CICS modules in `cobol_cics` subfolder. Using the following sample will ensure that the file flag `isCICS` is set to `true` for all COBOL files in this subfolder with the file extension `*.cbl`. However, it is recommended not to store information about the build configuration within the layout of folders, because an update would require to move files into different directories.

```properties
isCICS = true :: **/cobol_cics/*.cbl
```

The MortgageApplication sample contains a good sample of how the DBB file property can be used. Typically, these overrides are defined in [application-conf/file.properties](../samples/MortgageApplication/application-conf/file.properties).

### Individual File Property

The approach of using the DBB file property syntax might become cumbersome if you want to manage multiple property overrides for a given application artifact.

This strategy is changing the way of specifying the properties by allowing to manage multiple property overrides together  within an **individual properties file** for a given build artifact. It focusses on the build artifact rather than the build property.

The functionality to load properties from an individual properties file can be activated by setting the configuration property `loadFileLevelProperties` in `application-conf/application.properties` file to `true`. To enable this feature for a specific file or a subset of application artifacts, leverage a DBB file property in `application-conf/file.properties` file to set `loadFileLevelProperties` to `true`. The below sample configures zAppBuild to look for an individual properties file for all the programs starting with `eps` and `lga` in `application-conf/file.properties` file.

```properties
loadFileLevelProperties = true :: **/cobol/eps*.cbl, **/cobol/lga*.cbl` 
```

Individual properties files are resolved using the pattern `<propertyFilePath directory>/<sourceFile>.<propertyFileExtension>`. The `propertyFilePath` and `propertyFileExtension` can be customized in [application-conf/application.properties](../samples/MortgageApplication/application-conf/application.properties). For example, for the source file `epsmlist.cbl`, the process searches for a file in the propertyFilePath directory. If no corresponding property file is found, the build will use the default build values or, if any file properties were defined using the DBB file property path syntax or an alternate approach, then the build will use those.

Once the `loadFileLevelProperties` property functionality is enabled, create a property file for each application artifact for which the Individual File Properties need to be defined. For example: to override build parameters for file `epsmlist.cbl` create the properties file `epsmlist.cbl.properties` in the defined folder. The name of the properties file needs to have the entire file name including the extension i.e. the properties file for `epsmlist.cbl` needs to be `epsmlist.cbl.properties`.

The individual properties file allows you to define multiple build properties using the standard property syntax; for instance, in `epsmlist.cbl.properties`, you can define the following properties:

```properties
cobol_compileParms=LIB,SOURCE
isCICS = true
```

With the above configuration, zAppBuild will import these properties and set them as DBB file properties.

You can view a sample properties file, [epsmlist.cbl.properties](../samples/MortgageApplication/properties/epsmlist.cbl.properties), within the MortgageApplication sample.

Please note that overriding the same build property using the DBB file property syntax using a group filter, may cause contingencies. 

### Language Definition Mapping

An alternative way to define build properties for a **subgroup of files** is leveraging a mapping approach. Rather than specifying individual parameters or properties for an individual application artifact, the application artifact is mapped to a language definition, which can define multiple build parameters at a central properties file. All mapped application artifacts will inherit the defined build parameters. 

This approach requires:

- a mapping of application artifact to a language definition
- a property file for defining the build parameters for each language definition

The Language Definition Property approach can be enabled by setting the property `loadLanguageDefinitionProperties` in `application-conf/application.properties` file to `true`. To enable this option for a specific file or a set of files the property, use the DBB file property syntax and set  `loadLanguageDefinitionProperties` set as `true` in the `application-conf/file.properties` file. Below is a sample to enable Language Definition Property for all programs starting with `eps` and `lga` in `application-conf/file.properties` file:

```properties
loadLanguageDefinitionProperties = true :: **/cobol/eps*.cbl, **/cobol/lga*.cbl
```

The Language Definition Property files need to be created in the `build-conf` folder. You can now specify file properties which support the overriding in the Language Definition Property file. You can create multiple Language Definition Property files under `build-conf` folder to serve different variations / types. A sample Language Definition Property file can be found at [langDefProps01.properties](../build-conf/langDefProps01.properties).  

The language definition property file allows you to centrally specify build properties for a the group of mapped application artifacts. All mapped files will inherit the build properties. However, in the case of combining the language definition mapping with an individual property file override, the settings in the individual property file will take precedence. 

In the following sample language definition, *langDefProps01.properties* is overriding the default COBOL compile parameters,  the file flag isCICS and the linkEdit statement:

```properties
cobol_compileParms=LIB,SOURCE
isCICS = true
cobol_linkEditStream=    INCLUDE OBJECT(@{member})\n    INCLUDE SYSLIB(CUSTOBJ)
```

To map files to the language definition, create a `languageDefinitionMapping.properties` file in the `application-conf` folder of your application repo and specify the language definition mapping. For example

```properties
epsnbrvl.cbl=langDefProps01
epsmlist.cbl=langDefProps01
```

maps both files `epsnbrvl.cbl` and `epsmlist.cbl` to use the `build-conf/langDefProps01.properties` for Language Definition Property overrides.

See [languageDefinitionMapping.properties](../samples/MortgageApplication/application-conf/languageDefinitionMapping.properties) for a sample language definition mapping file.

The implementation again leverages DBB's file property concept to define these settings only for the mapped files.

