# File Property Management

## Table of contents

- [Introduction](#introduction)
- [zAppBuild's hierarchy to configure the build parameters for application artifacts](#zappbuilds-hierarchy-to-configure-the-build-parameters-for-application-artifacts)
- [1. DBB file property syntax](#1-dbb-file-property-syntax)
- [2. Individual File Property](#2-individual-file-property)
- [3. Language Definition Property](#3-language-definition-property)
- [4. Default properties](#4-default-properties)

## Introduction

Building mainframe application programs requires configuring various parameters and options for the different build steps, such as the pre-compile, compile, or link-edit step. For example, an application can contain COBOL programs that need to be link edited with the option `NCAL` or without the option `NCAL` for different purposes. This may be required for any build parameter for the various build steps like compile parameters, bind parameters, link edit parameters, and so on.

In existing mainframe toolchains, this customization is performed by assigning a type to the application artifact. This *type* is often used to specify the build parameters and options for the entire **subgroup** of application artifacts. Obviously, it allows that an application program might have some individual overrides as well.

## zAppBuild's hierarchy to configure the build parameters for application artifacts

Dependency Based Build comes with its own [APIs](https://www.ibm.com/docs/api/v1/content/SS6T76_2.0.0/javadoc/index.html) to manage build properties, which extends the standard key-value pair strategy of *java.util.Properties*. DBB refers to the term *File properties* to allow overriding the corresponding default build properties using the DBB file property path syntax. See [IBM DBB Docs - Build properties](https://www.ibm.com/docs/en/dbb/2.0.0?topic=apis-build-properties#file-properties) for more details.

zAppBuild's implementation supports overriding the majority of build properties. The full list can be viewed at [application-conf/README.md](../samples/application-conf/README.md).

zAppBuild leverages DBB's API and allows you to define build parameters on three different levels for each language script:

  1. General defaults in corresponding the Language property files - for example, defining the compile options for building COBOL programs in [application-conf/Cobol.properties](../samples/application-conf/Cobol.properties)
  2. A group definition, using a mapping to a language definition file to override the defaults using [application-conf/file.properties](../samples/application-conf/file.properties).
  3. An individual file-level definition to override the parameters leveraging Dependency Based Builds file properties syntax.

In order to handle the above scenarios to override the default file properties for specific file or set of files, zAppBuild comes with various strategies that can be combined via an order of precedence. The following table summarizes the strategies for overriding file properties from highest to lowest precedence:

||Strategy|Use case|
|-|-|-|
|1.|Individual file properties|Override build parameters for individual files|
|2.|Language definition mapping|Override  build parameters for a group of mapped application artifacts|
|3.|DBB file properties|Override a single build parameter for individual files or a grouped list of application artifacts|
|4.|Default properties|General build properties used when no overrides are defined|

To understand the order of precedence, think of this as a merge of the property configurations. For example, if both individual file properties and a language definition mapping are configured for a file, then the properties defined through the individual file property definition take precedence, but are also merged with other properties defined by the language definition mapping and the default properties.

## 1. DBB file property syntax

To override a single build parameter, you can make use of the DBB file property syntax.  

For example, to use a DBB file property to override the default COBOL compile parameters for build artifact `MortgageApplication/cobol/epsnbrvl.cbl`, follow the DBB file property syntax in `file.properties`, which is the default property file for configuring file property overrides:

```properties
cobol_compileParms=LIB,SOURCE :: **/cobol/epsnbrvl.cbl
```

For merging file properties of this file-level override with the default setting, you can specify the following syntax:

```properties
cobol_compileParms=${cobol_compileParms},SOURCE :: **/cobol/epsnbrvl.cbl
```

The file property path syntax also allows you to override the build parameters for a group of files using wildcards. For example, let's assusme that you are storing all CICS modules in `cobol_cics` subfolder. Using the following sample will ensure that the file flag `isCICS` is set to `true` for all files in this subfolder. However, it is recommended not to store information about the build configuration within the layout of folders.

```properties
isCICS = true :: **/cobol_cics/*epsmlist*.cbl
```

The MortgageApplication sample contains a good sample of how the DBB file property can be used. Typically these overrides are defined in [application-conf/file.properties](../samples/MortgageApplication/application-conf/file.properties).

## 2. Individual File Property

The approach of using the DBB file property syntax might become cumbersome if you want to manage multiple property overrides for a given application artifact. To change the way of specifying the properties and manage multiple property overrides together for a given build artifact or set of build artifacts, you can enable zAppbuild to look for an individual properties file.

This functionality to load properties from an individual properties file can be activated by setting the property `loadFileLevelProperties` in `application-conf/application.properties` file to `true`.  To enable this feature to look for a specific file or a subset of application artifacts, leverage a DBB file property in `application-conf/file.properties` file to set `loadFileLevelProperties` to `true`. Below is a sample to enable Individual File Property for all the programs starting with `eps` and `lga` in `application-conf/file.properties` file.

```properties
loadFileLevelProperties = true :: **/cobol/eps*.cbl, **/cobol/lga*.cbl` 
```

Individual property files are resolved using the pattern `<propertyFilePath directory>/<sourceFile>.<propertyFileExtension>`. The `propertyFilePath` and `propertyFileExtension` can be customized in [application-conf/application.properties](../samples/MortgageApplication/application-conf/application.properties). For example, for the source file `epsmlist.cbl`, the process searches for a file in the propertyFilePath directory. If no corresponding property file is found, the build will use the default build values or, if any file properties were defined using the DBB file property path syntax, then the build will use those.

Once the `loadFileLevelProperties` property is enabled, create a property file for each application artifact for which the Individual File Properties need to be defined. For example: to override file parameters for file `epsmlist.cbl` create the properties file `epsmlist.cbl.properties` in the defined folder. The name of the properties file needs to have the entire file name including the extension i.e. the properties file for `epsmlist.cbl` needs to be `epsmlist.cbl.properties` and not `epsmlist.properties`.

The individual properties file allows you to define build properties using the standard property syntax; for instance, in `epsmlist.cbl.properties`, you can define the following properties:

```properties
cobol_compileParms=LIB,SOURCE
isCICS = true
```

With the above configuration, zAppBuild will import these properties and add them as DBB file properties.

You can view a sample properties file, [epsmlist.cbl.properties](../samples/MortgageApplication/properties/epsmlist.cbl.properties), within the MortgageApplication sample.

## 3. Language Definition Property

An alternative way to define build properties for a subgroup of files leverages a mapping approach. Rather than specifying individual parameters or properties for an individual application artifact, the application artifact is mapped to a language definition, which defines multiple build parameters.

This approach requires:

- a mapping of build artifact to a language definition
- a property file for the language definitions

The Language Definition Property approach can be enabled by setting the property `loadLanguageDefinitionProperties` in `application-conf/application.properties` file to `true`. To enable this option for a specific file or a set of files the property, use the DBB file property syntax and set  `loadLanguageDefinitionProperties` set as `true` in the `application-conf/file.properties` file. Below is a sample to enable Language Definition Property for all programs starting with `eps` and `lga` in `application-conf/file.properties` file:

```properties
loadLanguageDefinitionProperties = true :: **/cobol/eps*.cbl, **/cobol/lga*.cbl
```

The Language Definition Property files need to be created in the `build-conf` folder. Specify the file properties in the Language Definition Property file. You can create multiple Language Definition Property files under `build-conf` folder. A sample Language Definition Property file can be found at [langDefProps01.properties](../build-conf/langDefProps01.properties).  

The language definition property file allows you to centrally specify build properties for a the group of mapped application artifacts. So, if a build property needs to be updated, you update that centrally.

In the following sample language definition, *langDefProps01.properties* is overriding the default COBOL compile parameters and the file flag:

```properties
cobol_compileParms=LIB,SOURCE
isCICS = true
```

To map files to the language definition, create a `languageDefinitionMapping.properties` file in the `application-conf` folder of your application repo and specify the language definition mapping. For example:

```properties
epsnbrvl.cbl=langDefProps01
epsmlist.cbl=langDefProps01
```

maps both files `epsnbrvl.cbl` and `epsmlist.cbl` to use the `build-conf/langDefProps01.properties` for Language Definition Property overrides.

See [languageDefinitionMapping.properties](../samples/MortgageApplication/application-conf/languageDefinitionMapping.properties) for a sample language definition mapping file.

The implementation again leverages DBB's file property concept to define these settings only for the mapped files.

## 4. Default properties

Default properties can be set in the corresponding language properties file.  For example, the COBOL file properties can be set in [application-conf/Cobol.properties](../samples/application-conf/Cobol.properties), while the Assembler file properties can be set in [application-conf/Assembler.properties](../samples/application-conf/Assembler.properties), and so on.
