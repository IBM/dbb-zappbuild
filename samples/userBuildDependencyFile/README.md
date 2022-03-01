## User Build Dependency File

In order to increase performance of User Build running on ZD&T, the IDEs can **optionally** pass dependency information about the program being built to zAppBuild allowing it to skip running dependency resolution which depending on the size and number of build dependencies the program references can be time consuming on ZD&T platforms.

### Option
Providing the following option when calling *build.groovy* will skip scanning and dependency resolution within zAppBuild.

    --dependencyFile <pathToFile>
    -df <pathToFile>
If not provided, zAppBuild will run the traditional scan and dependency resolution on the build file.
If it is provided, zAppBuild will skip scanning and resolution and refer to the dependencies and information from the file. 
  
### Location

The location of the user build dependency file on USS is unimportant, as long as that path is correctly specified when passing the **-\-dependencyFile \<path>** option to zAppBuild. 

### Encoding and Tagging

The user build dependency file functionality supports three encoding scenarios when uploaded:
 1. Encoded as UTF-8 and tagged as UTF-8. 
 2. Encoded as IBM-1047 and tagged as IBM-1047.
 3. Encoded as IBM-1047 and untagged.  

### Additional Resources
View the user build dependency file schema and a sample file using the links below. 
##### [Dependency File Schema](schema.json)
##### [Sample Dependency File](sample.json)
