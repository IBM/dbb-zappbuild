## fullBuild.groovy
This script is called by test.groovy to run a full build by creating a new “automation” branch from the feature branch specified in the command line argument. It verifies the below requirments
- Full build ran clean
- Number of expected build files equals the number of files build during the full build in the console.
- Build files expected is the same as build files during the full build in the console.

## impactBuild.groovy
This script that is called by test.groovy to run an impact build against the program file specified in the command line argument. It verifies the below requirments
- Impact build ran clean
- Number of expected build files equals the number of files build during the impact build in the console.
- Build files expected is the same as build files during the impact build in the console.