# File Property Management

## Table of contents

- [File Property Management](#file-property-management)
  - [Table of contents](#table-of-contents)
  - [Introduction](#introduction)
  - [Overriding build properties with DBB and zAppBuild](#overriding-build-properties-with-dbb-and-zappbuild)
  - [Default properties](#default-properties)
  - [Overriding properties](#overriding-properties)
    - [DBB file properties](#dbb-file-properties)
    - [Individual artifact properties file](#individual-artifact-properties-file)
    - [Language configuration mapping](#language-configuration-mapping)

## Introduction

This document explains how to define compiler and other options when building a program or subset of programs with zAppBuild. Building mainframe application programs requires configuring various parameters and options for the different build steps, such as the pre-compile, compile, or link-edit step. For example, an application can contain COBOL programs that need to be link edited with the option `NCAL` or without the option `NCAL` for different purposes. An override may be required for any build parameter for the various build steps like compile parameters, bind parameters, link edit parameters, and so on.

In existing mainframe toolchains, this customization is performed by assigning a type to the application artifact[^1]. This *type* is often used to specify the build parameters and options for an entire **subgroup** of application artifacts. Additionally, it allows that an application program might have some individual overrides as well.

Generally, each build parameter for an application artifact will either have a default value or an override of the default value.

## Overriding build properties with DBB and zAppBuild

Dependency Based Build comes with its own [APIs](https://www.ibm.com/docs/api/v1/content/SS6T76_2.0.0/javadoc/index.html) to manage build properties, which extend the standard key-value pair strategy of *java.util.Properties*. DBB refers to the term *File properties* to allow overriding the corresponding default build properties using the DBB file property path syntax. See [IBM DBB Docs - Build properties](https://www.ibm.com/docs/en/dbb/latest?topic=apis-build-properties#file-properties) for more details about this syntax.

zAppBuild supports overriding the majority of build properties defined within its framework. The full list can be viewed at [application-conf/README.md](../samples/application-conf/README.md).

zAppBuild leverages DBB's API and allows you to define build parameters on three different levels for each language script:

1. General defaults in the corresponding language properties files: For example, you can define the compile options for building COBOL programs in [application-conf/Cobol.properties](../samples/application-conf/Cobol.properties). Property keys make use of a language prefix; for instance, COBOL property keys are prefixed with `cobol_`.
2. A group definition that overrides the general default in one of three ways:
   - By using DBB's file property syntax in [application-conf/file.properties](../samples/application-conf/file.properties), and specifying the application artifact group via a pattern filter on the path name(s)
   - By mapping to a *language configuration* file to override the defaults, such as in [application-conf/languageConfigurationMapping.properties](../samples/MortgageApplication/application-conf/languageConfigurationMapping.properties) 
   - By mapping the build file to a *language configuration* file using the DBB file property syntax file to specify the property `languageConfiguration` in [application-conf/file.properties](../samples/MortgageApplication/application-conf/file.properties#L46-L53)
3. An individual file-level definition that overwrites the general default in one of two ways:
   - By using DBB's file properties syntax in [application-conf/file.properties](../samples/application-conf/file.properties), and specifying the application artifact's path as the file path pattern
   - By specifying multiple parameters for a specific file using the individual artifact properties file. For example: [epsmlist.cbl.properties](../samples/MortgageApplication/properties/epsmlist.cbl.properties).

zAppBuild comes with various build property strategies that can be combined via an order of precedence. The following table summarizes the strategies for overriding file properties from highest to lowest precedence:

|Precedence|Strategy|Use case|Implementation|
|-|-|-|-|
|1.|Individual artifact properties file|Override one or multiple build parameters for individual files|DBB file property defining an override for the specific file[^2]|
|2.|Language configuration mapping|Override and define one or multiple build parameters for a group of mapped application artifacts|DBB file property defining an override for the specific file(s)[^2]|
|3.|DBB file properties|Override a single build parameter for individual files or a grouped list of application artifacts|DBB file property defining an override for the specific[^2]  or grouped list[^3] of application artifacts|
|4.|Default properties|General build properties used when no overrides are defined|Build property defining the default value for all files|

To understand the order of precedence, think of this as a merge of the property configurations. For example, if both an individual artifact properties file and a language configuration mapping are configured for a file, then the properties defined through the individual artifact properties file take precedence, but are also merged with other properties defined by the language configuration mapping and the default properties.

The following sections explain these build property strategies in more detail.

## Default properties

Default properties can be set in the corresponding language properties file. For example, the COBOL file properties can be set in [application-conf/Cobol.properties](../samples/application-conf/Cobol.properties), while the Assembler file properties can be set in [application-conf/Assembler.properties](../samples/application-conf/Assembler.properties), and so on.

By default, zAppBuild applies the properties stored in the [application-conf](../samples/application-conf) folder of the application repository, which allows the application team to have control over these files. These property definitions in [application-conf](../samples/application-conf) take precedence over corresponding properties defined in the [build-conf](../build-conf/) folder. If you are looking for a more centralized way to manage the default options for all applications, you can move the relevant property definitions into the zAppBuild build framework itself by taking them out of the [application-conf](../samples/application-conf) folder, and then only storing them in one of the following locations:

- In the appropriate language properties file(s) under [build-conf](../build-conf/)
- In a separate directory within zAppBuild itself, while using the applicationConfRootDir property in [build-conf/build.properties](../build-conf/build.properties)

## Overriding properties

The following section describes the various strategies to override default build property values. The DBB file property syntax is the most commonly used approach within the zAppBuild samples. Two alternate approaches to override build properties are implemented in zAppBuild to serve the different use cases, and can be used to simplify the adoption of zAppBuild by either leveraging an individual artifact properties file per application artifact, or by defining a language configuration mapping.

### DBB file properties

The most common way to override a single build parameter makes use of the [DBB file property syntax](https://www.ibm.com/docs/en/dbb/latest?topic=apis-build-properties#file-properties). This strategy can be applied to individual artifacts or groups of artifacts.

For example, to use a DBB file property to override the default COBOL compile parameters for build artifact `MortgageApplication/cobol/epsnbrvl.cbl`, follow the DBB file property syntax in `file.properties`, which is the default property file for configuring file property overrides:

```properties
cobol_compileParms=LIB,SOURCE :: **/cobol/epsnbrvl.cbl
```

For merging the property values of this file-level override with the default COBOL compile parameters (rather than just overriding them), you can specify the following syntax:

```properties
cobol_compileParms=${cobol_compileParms},SOURCE :: **/cobol/epsnbrvl.cbl
```

In order to override the build parameter for a group of files, you can use wildcards when specifying the file path pattern in DBB's file property path syntax. For example, let's assume that you are storing all CICS modules in the `cobol_cics` subfolder. Using the following sample will ensure that the file flag `isCICS` is set to `true` for all COBOL files in this subfolder with the file extension `*.cbl`.

```properties
isCICS = true :: **/cobol_cics/*.cbl
```

- **Note:** We do not recommend organizing the layout of repository folders/files based on build property management, because future updates to the build information of a file could require it to be moved into different folders. Instead, we recommend that the repository's folder layout represent the functional context of the files.

The MortgageApplication sample contains a good example of how the DBB file property can be used. Typically, these overrides are defined in [application-conf/file.properties](../samples/MortgageApplication/application-conf/file.properties).

### Individual artifact properties file

The approach of using the DBB file property syntax might become cumbersome if you want to manage multiple property overrides for a given application artifact.

The "individual artifact properties file" strategy changes the way of specifying the properties by allowing you to manage multiple property overrides together within an individual properties file for a given build artifact. It centers around the build artifact rather than the build property.

The functionality to load properties from an individual artifact properties file can be activated by setting the configuration property `loadFileLevelProperties` in the `application-conf/application.properties` file to `true`. To enable this feature for a specific artifact or a subset of application artifacts, use the DBB file property syntax in `application-conf/file.properties` to set `loadFileLevelProperties` to `true`. The following snippet from a sample `application-conf/file.properties` file configures zAppBuild to look for an individual artifact properties file for all the programs starting with `eps` and `lga`:

```properties
loadFileLevelProperties = true :: **/cobol/eps*.cbl, **/cobol/lga*.cbl` 
```

Individual artifact properties files are resolved using the pattern `<propertyFilePath directory>/<sourceFile>.<propertyFileExtension>`. The `propertyFilePath` and `propertyFileExtension` can be customized in [application-conf/application.properties](../samples/MortgageApplication/application-conf/application.properties). For example, for the source file `epsmlist.cbl`, the process searches for an individual artifact properties file in the defined `propertyFilePath` directory. If no corresponding properties file is found, the build will use the default build values or, if any file properties were defined using the DBB file property path syntax or an alternate approach, then the build will use those.

Once the `loadFileLevelProperties` property functionality is enabled, create a properties file for each application artifact for which individual artifact properties need to be defined. For example, to override build parameters for the file `epsmlist.cbl`, create the properties file `epsmlist.cbl.properties` in the defined `propertyFilePath` folder. The name of the properties file needs to have the entire source file name including the extension; hence, the properties file for `epsmlist.cbl` needs to be named `epsmlist.cbl.properties`.

The individual artifact properties file allows you to define multiple build properties using the standard property syntax. For instance, in `epsmlist.cbl.properties`, you can define the following properties:

```properties
cobol_compileParms=LIB,SOURCE
isCICS = true
```

With the above configuration, zAppBuild will import these properties and set them as DBB file properties.

You can view a sample individual artifact properties file, [epsmlist.cbl.properties](../samples/MortgageApplication/properties/epsmlist.cbl.properties), within the MortgageApplication sample.

**Note:** Overrides for a given build property should be managed either via the DBB file property path syntax or in the individual artifact properties files, but not both at the same time (as this can cause unpredictable behavior). The following example shows how both approaches for defining file properties can be combined to specify a set of build properties for the same source file:

- Example using the DBB file property path syntax and an individual artifact properties file to define build properties for a source file named `app/cobol/AB123456.cbl`:
  - You can use the DBB file property path syntax to define a file property for a group of files. The below defines the `deployType` for all source files in the folder cobol beginning with `AB*` to be `BATCHLOAD`:

    ```properties
    cobol_deployType = BATCHLOAD :: **/cobol/AB*.cbl
    ```

  - At the same time, you can define an individual artifact properties file for `app/cobol/AB123456.cbl` with the following *different* build property:

    ```properties
    cobol_compileParms = LIB,SOURCE
    ```

  - During the build, the file `app/cobol/AB123456.cbl` will have the `deployType` `BATCHLOAD` and the COBOL compile parameters `LIB` and `SOURCE`.

### Language configuration mapping

An alternative way to define build properties for a **subgroup of files** is by leveraging a mapping approach. Rather than specifying individual parameters or properties for an individual application artifact, the application artifacts are mapped to a language configuration, which can then define multiple build parameters in a central language configuration properties file for the language script. All mapped application artifacts will inherit those defined build parameters.

This approach consists of:

- Language configuration mapping: Either via a mapping file of the application artifact(s) to the language configuration file or alternatively by using the DBB file property syntax to map the build file to the language configuration file.
- Language configuration properties file(s): For each language configuration, a properties file defining that language's build parameters.

The "language configuration mapping" approach can be enabled by setting the property `loadLanguageConfigurationProperties` in the `application-conf/application.properties` file to `true`. To enable this option for a specific file or a set of files, use the DBB file property syntax and set  `loadLanguageConfigurationProperties` to `true` in the `application-conf/file.properties` file. Below is a sample to enable language configuration mapping for all programs starting with `eps` and `lga` via the `application-conf/file.properties` file:

```properties
loadLanguageConfigurationProperties = true :: **/cobol/eps*.cbl, **/cobol/lga*.cbl
```

You can specify build properties for a language in a language configuration properties file, which should be created in the `build-conf/language-conf` folder. zAppBuild will import these properties from the language configuration properties file and set them as DBB file properties for the mapped artifacts. You can implement multiple language configurations to serve different variations or types by creating multiple language configuration properties files under the `build-conf/language-conf` folder. A sample language configuration properties file can be found at [languageConfigProps01.properties](../build-conf/language-conf/languageConfigProps01.properties).  

A language configuration properties file allows you to centrally specify build properties for the group of mapped application artifacts. All mapped files will inherit those build properties. However, in the case of combining the language configuration mapping with an individual artifact properties file override, for any build property that is defined in both places, the property definition in the individual artifact properties file will take precedence and be applied. Properties that are not specified in the individual artifact properties file will be defined by lower precedence strategies - that is, from the language configuration mapping if defined there, or if not, then from the default properties.

In the following sample language configuration properties file, the properties defined in this snippet are overriding the default COBOL compile parameters (`cobol_compileParms`), the file flag `isCICS`, and the linkEdit statement (`cobol_linkEditStream`):

```properties
cobol_compileParms=LIB,SOURCE
isCICS = true
cobol_linkEditStream=    INCLUDE OBJECT(@{member})\n    INCLUDE SYSLIB(CUSTOBJ)
```

Two options exist to map build files to the language configuration:

Either create a `languageConfigurationMapping.properties` file in the `application-conf` folder of your application repository. Then, within this new language configuration mapping file, map each artifact to its corresponding language configuration using the syntax `<sourceFileName.extension>=<languageConfigurationPropertiesFileName>`, or leverage the DBB file property syntax with the build property `languageConfiguration`.

- For example, the following snippet in [application-conf/languageConfigurationMapping.properties](../samples/MortgageApplication/application-conf/languageConfigurationMapping.properties) maps both source files `epsnbrvl.cbl` and `epsmlist.cbl` to use the properties defined in `build-conf/language-conf/languageConfigProps01.properties`,  while the source file `epscmort.cbl` is mapped to use the properties defined in `build-conf/language-conf/languageConfigProps02.properties` for language configuration mapping overrides:

  ```properties
  epsnbrvl.cbl=languageConfigProps01
  epsmlist.cbl=languageConfigProps01
  epscmort.cbl=languageConfigProps02
  ```

  See [languageConfigurationMapping.properties](../samples/MortgageApplication/application-conf/languageConfigurationMapping.properties) for a sample language configuration mapping file.

- The alternative mapping approach using the DBB file property syntax allows you to leverage file patterns similarly to other file properties in the zAppBuild framework. The below configuration is providing the same information like the above approach using the `languageConfigurationMapping.properties` file.

  ```properties
  languageConfiguration = languageConfigProps01 :: **/cobol/epsnbrvl.cbl, **/cobol/epsmpmt.cbl
  languageConfiguration = languageConfigProps02 ::  **/cobol/epscmort.cbl
  ```
  
  See [application-conf/file.properties](../samples/MortgageApplication/application-conf/file.properties#L46-L53) for a sample language configuration mapping via the DBB file property syntax.


[^1]: The term "artifact" and "file" in this document refer to program source code that will built (as opposed to JCL or other non-buildable items), for example by DBB.
[^2]: DBB is managing the DBB file properties in its separate internal table compared to the default properties. This table leverages the combination of [property name + file pattern] as the key of the internal table. When the same key is declared a second time, it overrides the first one.
[^3]: Because of managing DBB file properties is done in a single table, you can experience unpredictable behaviour when mixing qualified file path pattern definitions and file path patterns containing wildcards for the same property name.
