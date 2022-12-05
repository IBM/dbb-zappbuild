## File Property Management

In ZOS application environment, we normally see scenarios like application files/programs of same type being build with different properties. For example, the same application can have COBOL object modules that needs to be link edited with option NCAL and without option NCAL for different purposes. This can happen with any build parameters like compile parameters, bind parameters, link edit parameters etc. This can be customized in legacy SCMs using process type/name used to build the file. When the user check-in the component back to SCM, they can provide the process type/name which will be used to build the component with the correct build parameters. 

This documents talks about how to customize the build properties at a file level in the zAppBuild framework.  In order to handle such scenarios to override default file properties for specific file/set of files, zAppBuild has got two options:
  1. Individual file property -  Override file parameters for individual files
  2. Language Definition property - Override file parameters for group of files

If both Individual file property and Language Definition property is enabled for a file, then the individual file property will take precedence over language definition property. The order of precedence of adding the file property is:

  1. Individual file property
  2. Language Definition property
  3. Default file properties

This will be a file property merge, i.e. if both individual file property and language definition property is set for a file, then file properties mentioned individual file property will be added to the file and those properties which are not mentioned in individual file property will be added from lower levels - Language Definition property and then from default file properties. 


### 1. Individual File Property

The Individual File Property option can be enabled setting the property `loadFileLevelProperties` in `application-conf/application.properties` file as `'true'`.  To enable this option for a specific file/set of files the property `loadFileLevelProperties` needs to be set as `'true'` in `application-conf/file.properties` file. Below is a sample to enable Individual File Property for all the programs starting with `'eps'` and `'lga'` in `application-conf/file.properties` file.

`loadFileLevelProperties = true :: **/cobol/eps*.cbl, **/cobol/lga*.cbl` 

Once the `loadFileLevelProperties` property is enabled, create a properties file for each program for which the Individual File Properties needs to be overridden.  For example: to override file parameters for file `epsmlist.cbl` create the properties file `epsmlist.cbl.properties`.  

This properties file needs to be created in the folder mentioned in `propertyFilePath` in `application-conf/application.properties`. For example: if `propertyFilePath=properties`, the properties file created needs to be kept in `properties` folder. 

The name of the properties file needs to have the entire file name including the extension i.e. the properties file for `epsmlist.cbl` needs to be `epsmlist.cbl.properties` and not `epsmlist.properties`.  

The extension of the properties file needs to be mentioned in `propertyFileExtension` in `application-conf/application.properties` i.e. if `propertyFileExtension=properties`, the properties file extension needs to be kept as `properties`.

Add the individual file properties that needs to be added to the file in the properties file created for that file.  Please refer sample properties file - [epsmlist.cbl.properties](../samples/MortgageApplication/properties/epsmlist.cbl.properties).


### 2. Language Definition Property

The Language Definition Property option can be enabled setting the property `loadLanguageDefinitionProperties` in `application-conf/application.properties` file as `'true'`.  To enable this option for a specific file/set of files the property `loadLanguageDefinitionProperties` needs to be set as `'true'` in `application-conf/file.properties` file. Below is a sample to enable Individual File Property for all the programs starting with `'eps'` and `'lga'` in `application-conf/file.properties` file.

`loadLanguageDefinitionProperties = true :: **/cobol/eps*.cbl, **/cobol/lga*.cbl` 

The Language Definition Property files need to be created in `build-conf` folder.  Add the file properties that needs tobe grouped together in the Language Definition Property files.  We can create multiple property files under `build-conf` folder.  Please refer sample properties file [langDefProps01.properties](../build-conf/langDefProps01.properties).  

Create `languageDefinitionMapping.properties` file in `application-conf` folder and add the program file - property file mapping in the language definition mapping file. For example, the entry `epsnbrvl.cbl=langDefProps01` means the file `epsnbrvl.cbl` uses `build-conf/langDefProps01.properties` for file property override.  Please refer the sample language definition mapping file - [languageDefinitionMapping.properties](../samples/MortgageApplication/application-conf/languageDefinitionMapping.properties).

The extension of the properties file needs to be mentioned in `propertyFileExtension` in `application-conf/application.properties` i.e. if `propertyFileExtension=properties`, the properties file extension needs to be kept as `properties`.

