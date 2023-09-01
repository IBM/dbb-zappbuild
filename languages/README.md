# Language Build Scripts
zAppBuild comes with a number of language specific build scripts.  These script are designed to be invoked by `build.groovy` and should not be invoked directly as they require the initialized environment provided by `build.groovy`.

## Included Language Scripts
* Assembler.groovy
* BMS.groovy
* Cobol.groovy
* LinkEdit.groovy (for building link cards)
* PLI.groovy
* DBDgen.groovy
* PSBgen.groovy
* MFS.groovy
* zCEE3.groovy
* ZunitConfig.groovy

All language scripts both compile and optionally link-edit programs. The language build scripts are intended to be useful out of the box but depending on the complexity of your applications' build requirements, may require modifications to meet your development team's needs.  By following the examples used in the existing language build scripts of keeping all application specific references out of the build scripts and instead using configuration properties with strong default values, the zAppBuild sample can continue to be a generic build solution for all of your specific applications.

## Convention for properties for language scripts

Central properties files for languages scripts are managed in [build-conf](../build-conf/) referencing the name of the language script - such as `Cobol.properties`. These files contain dataset naming conventions and allocation options. Properties that may be application specific and may be overridden are managed as the group of application-conf properties files either centrally configured via the `applicationConfRootDir` property in [build-conf/build.properties](../build-conf/build.properties) or as part of the application repository - see [samples/application-conf/](../samples/application-conf/).  

All properties for language scripts are defined by prefixing the property with a `language prefix` - e.q. `cobol_` to group and identify properties belonging to the Cobol.groovy languages script. The language prefix is computed based on the lower-case basename of the name of the language script, respectively until the first `_` character in the name of the language script. 

## Script Mappings
Source files are mapped to language scripts via ***script mapping file properties***. Though script mappings can be defined at any level, zAppBuild relegates script mapping declarations to the application configuration folder (see [samples/application-conf/file.properties](../samples/application-conf/file.properties) allowing the application owner to determine what is best for the application. 
