## fullBuild.groovy
This script is called by test.groovy to run a full build by creating a new “automation” branch from the feature branch specified in the command line argument. It verifies the below requirements
- Full build ran clean
- Number of expected build files equal the number of files build during the full build in the console.
- Build files expected is the same as build files during the full build in the console.

## impactBuild.groovy
This script that is called by test.groovy to run an impact build against all the program file specified in the [test.properties](/test/applications/MortgageApplication/test.properties). It verifies the below requirements
- Impact build ran clean
- Number of expected build files equal the number of files build during the impact build in the console.
- Build files expected is the same as build files during the impact build in the console.

## resetBuild.groovy
This is a maintenance script that runs at the end of this test pipeline to delete collections and build result groups.
