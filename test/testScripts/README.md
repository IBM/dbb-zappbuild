# zAppBuild test scripts

Each test script is expected to be able to run on its own without any dependency. It is expected, that a test script resets the environment during finalizing phase of the test.

## fullBuild.groovy
This script is called by test.groovy to run a full build by creating a new “automation” branch from the feature branch specified in the command line argument. It verifies the below requirements
- Full build ran clean
- Number of expected build files equal the number of files build during the full build in the console.
- Build files expected is the same as build files during the full build in the console.

## fullBuild_languageConfigurations.groovy
This script is called by test.groovy to run a full build and multiple user builds specifically to validate the the language configuration override capability. It verifies on
- fullBuild to see the expected overwrite for cobol_compileParms
- userBuild TC1 to have a successful override of a previously defined file level property
- userBuild TC2 to validate a failing override and check on an expected warning message

## fullBuild_debug.groovy
This script is called by test.groovy to run a full build by creating a new “automation” branch from the feature branch specified in the command line argument. It verifies the below requirements
- Full build with the `--debug` cli option ran clean
- Number of expected build files equal the number of files build during the full build in the console.
- Checks if output files are documented in the BuildReport.json. Specifically use it to check if the SIDEFILE for Assembler is present. Cobol and PLI don't produce an additional output file.

## impactBuild.groovy
This script that is called by test.groovy to run an impact build against all the program file specified in the [test.properties](/test/applications/MortgageApplication/test.properties). It verifies the below requirements
- Impact build ran clean
- Number of expected build files equal the number of files build during the impact build in the console.
- Build files expected is the same as build files during the impact build in the console.

## impactBuild_properties.groovy
This script is called by test.groovy to run an impact build on an update of the Cobol.properties. It verifies
- that all expected files got build

## imimpactBuild_renaming.groovy
This script is called by test.groovy to run an impact build on a renamed source file. It verifies
- clean build
- that the file with the new name is built
- that the logical file with the original name is deleted from the metadata store and the test branch is reset

## impactBuild_deletion.groovy
This script is called by test.groovy. It runs a fullbuild first to set the baseline and then to execute an impact build after a source file was deleted. It verifies
- that the deleted file is correctly detected
- that the deleted output file is removed from the build library and the test branch is reset
  
## resetBuild.groovy
This is a maintenance script that runs at the end of this test pipeline to delete collections and build result groups.
