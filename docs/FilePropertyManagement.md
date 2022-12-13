## File Property Management

### Introduction

Building mainframe application programs requires to configure various parameters and options for the various build steps, like the pre-compile, compile or link-edit step. For example, within an application, it can have COBOL programs that needs to be link edited with option `NCAL` ar without the option NCAL for different purposes. This may be required for any build parameter for the various build steps like compile parameters, bind parameters, link edit parameters etc. 

In existing mainframe toolchains, this customization is performed by assigning a type to the application artifact. This *type* is often used to specify the build parameters and options for the entire **subgroup** of application artifacts. Obviously it allows that an application program might have some individual overwrites as well.

### zAppBuild's hierarchy to configure the build parameters for application artifacts

Dependency Based Build comes with its own API to manage build properties, which is extending the standard key-value pair strategy of *java.util.Properties*. DBB refers to the term *File properties* to allow overriding the corresponding default build properties using the DBB file property path syntax , see [IBM DBB Docs - Build properties](https://www.ibm.com/docs/en/dbb/2.0.0?topic=apis-build-properties#file-properties).

zAppBuild's implementation supports to overwrite the majority of build properties. The full list can be looked up at [application-conf/README.md](../samples/application-conf/README.md).

zAppBuild leverage this API and allows to define build parameters on three different levels for each language script:
  1. General defaults - e.q. defining the compile options in [application-conf/Cobol.properties](../samples/application-conf/Cobol.properties)
  2. A group definition, using a mapping to a language definition file to overwrite the defaults.
  3. An individual file level definition to overwrite the parameters leveraging Dependency Based Builds file properties syntax.

In order to handle the above scenarios to override the default file properties for specific file/set of files, zAppBuild has comes with various strategies that can be combined:

  1. *DBB file property syntax* - allowing to override a single build parameter for individual files or a grouped list of application artifacts 
  2. *Language Definition mapping* - allowing to override  build parameters for a group of mapped application artifacts
  3. *Individual file properties* - allowing to override build parameters for individual files

If both Individual file property and Language Definition mapping is enabled for a file, then the individual file property will take precedence over language definition mapping. The order of precedence of adding the file property is:

  1. Individual file property
  2. Language Definition property
  3. Default properties

Think of this as a merge of the property configurations, i.e. if both individual file property and language definition mapping is configured for a file, then file properties defined through the individual file property definition take precedence, are merged with those properties defined by the language definition property and the default properties. 

### 1. DBB file property syntax

To overwrite a single build parameter, you can make use of the DBB file property syntax.  

Using a DBB file property to overwrite, for instance, the default COBOL compile parameters for build artifact `MortgageApplication/cobol/epsnbrvl.cbl` , follow the DBB file property syntax:

```properties
cobol_compileParms=LIB,SOURCE :: **/cobol/epsnbrvl.cbl
```
in `file.properties` , which is the default location for file configuring file overwrites.

For merging merge properties of this file level overwrite with the default setting, you can specify 
```properties
cobol_compileParms=${cobol_compileParms},SOURCE :: **/cobol/epsnbrvl.cbl
```

The file property path syntax, also allows to overwrite the build parameters for a group of files using wildcards. Let's assume, you are storing all cics modules in `cobol_cics` subfolder. Using the below sample will make sure, that the file flag isCICS is set to true for all files in this subfolder. However, it is recommended not to store information about the build configuration within the layout of folders.
```properties
isCICS = true :: **/cobol_cics/*epsmlist*.cbl
```

The MortgageApplication sample contains a good sample of how the DBB file property can be used. Typically the overwrites are defined in [application-conf/file.properties](../samples/MortgageApplication/application-conf/file.properties)

### 2. Individual File Property

The approach of using the DBB file property syntax might become cumbersome, when you manage multiple overwrites for a given application artifact. If you would like to change the way of specifying the properties and manage overwrites together for a build artifact, zAppbuild can be enabled to look for an individual properties file. 

This functionality to load a properties from an individual properties file can be activated by setting the property `loadFileLevelProperties` in `application-conf/application.properties` file to `true`.  To enable this feature to look for a specific file or a subset of application artifacts, leverage a DBB file property in `application-conf/file.properties` file to set `loadFileLevelProperties` to `true`. Below is a sample to enable Individual File Property for all the programs starting with `eps` and `lga` in `application-conf/file.properties` file.

```properties
loadFileLevelProperties = true :: **/cobol/eps*.cbl, **/cobol/lga*.cbl` 
```

Individual property files are resolved using the pattern `<propertyFilePath directory>/<sourceFile>.<propertyFileExtension>`. For example, for the source file `epsmlist.cbl`, the process searches for a file in the propertyFilePath directory. If no corresponding property file is found, the build will use the default build values or, if any file properties were defined using the DBB file property path syntax, then the build will use those.

Once the `loadFileLevelProperties` property is enabled, create a property file for each application artifact for which the Individual File Properties need to be defined. For example: to override file parameters for file `epsmlist.cbl` create the properties file `epsmlist.cbl.properties` in the defined folder. The name of the properties file needs to have the entire file name including the extension i.e. the properties file for `epsmlist.cbl` needs to be `epsmlist.cbl.properties` and not `epsmlist.properties`.

The individual properties file allows to define build properties using the standard property syntax; for instance `epsmlist.cbl.properties` define: 
```properties
cobol_compileParms=LIB,SOURCE
isCICS = true
```

With the above configuration, zAppBuild will import these properties and add them as DBB file properties. 

Please refer to a sample properties [epsmlist.cbl.properties](../samples/MortgageApplication/properties/epsmlist.cbl.properties) file within the MortgageApplication sample.


### 3. Language Definition Property

An alternative way to define build properties for a subgroup of files, leverages a mapping approach. Rather than specifying individual parameters or properties for an individual application artifact, the application artifact is mapped to a language definition, which defines multiple build parameters.

This approach requires:

* a mapping of build artifact to a language definition
* a property file for the language definitions

The Language Definition Property approach can be enabled by setting the property `loadLanguageDefinitionProperties` in `application-conf/application.properties` file to `true`. To enable this option for a specific file or a set of files the property, use the DBB file property syntax and set  `loadLanguageDefinitionProperties` set as `true` in the `application-conf/file.properties` file. Below is a sample to enable Language Definition Property for all the programs starting with `eps` and `lga` in `application-conf/file.properties` file:

```properties
loadLanguageDefinitionProperties = true :: **/cobol/eps*.cbl, **/cobol/lga*.cbl
```

The Language Definition Property files need to be created in `build-conf` folder. Specify the file properties in the Language Definition Property file. We can create multiple Language Definition Property files under `build-conf` folder. Please refer sample Language Definition Property file [langDefProps01.properties](../build-conf/langDefProps01.properties).  

The language definition property file now allows you to centrally specify build properties for a the group of mapped application artifacts. So, if a build property needs to be updated, you update that centrally.

The below sample language definition *langDefProps01.properties* is overwriting the default compile parameters and the file flag.
```properties
cobol_compileParms=LIB,SOURCE
isCICS = true
```
To map files to the language definition, create a `languageDefinitionMapping.properties` file in `application-conf` folder of your application repo and specify the language definition mapping. For instance:
```properties
epsnbrvl.cbl=langDefProps01
epsmlist.cbl=langDefProps01
```
maps both files `epsnbrvl.cbl` and `epsmlist.cbl` to use the `build-conf/langDefProps01.properties` for Language Definition Property overrides. 

Please refer the sample language definition mapping file [languageDefinitionMapping.properties](../samples/MortgageApplication/application-conf/languageDefinitionMapping.properties).

The implementation again leverages DBBs file property concept to define these settings only for the mapped files.