## fullBuild.groovy
This script is called by test.groovy to run a full build by creating a new “automation” branch from the feature branch specified in the command line argument. It verifies the below requirments
- Full build ran clean
- Number of expected build files (is calculated from the `fullFiles` argument passed from the command line) matches the number of files build during the full build   in the console.
- Build files expected (passsed via command line argument `fullFiles`) matches the build files during the full build in the console.

```
Optional arguments:
 -z --zRepoPath <arg>   Path to the cloned/forked zAppBuild repository

Required arguments that must be present during each invocation of `test.groovy`
 -b --branchName <arg> Feature Branch that needs to be tested 
 -a --app <arg> Application that is being tested (example: MortgageApplication)
 -s --serverURL <arg> Server URL
 -q --hlq <arg> hlq to delete segments from (example: IBMDBB.ZAPP.BUILD)
 -u --userName <arg> User for server
 -p --password <arg> Password for server
 -f --fullFiles <arg> Full build files for verification
```

## impactBuild.groovy
This script that is called by test.groovy to run an impact build against the program file specified in the command line argument. It verifies the below requirments
- Impact build ran clean
- Number of expected build files(is calculated from the `impactFiles` argument passed from the command line) matches the number of files build during the impact 
  build in the console.
- Build files expected(passsed via command line argument `impactFiles`) matches the build files during the impact build in the console.

```
Optional arguments:
 -z --zRepoPath <arg>   Path to the cloned/forked zAppBuild repository

Required arguments that must be present during each invocation of `test.groovy`
 -b --branchName <arg> Feature Branch that needs to be tested 
 -a --app <arg> Application that is being tested (example: MortgageApplication)
 -s --serverURL <arg> Server URL
 -q --hlq <arg> hlq to delete segments from (example: IBMDBB.ZAPP.BUILD)
 -u --userName <arg> User for server
 -p --password <arg> Password for server
 -i --impactFiles <arg> Impact build files for verification
 -c --programFile <arg> Folder of the program to edit (example: /bms/epsmort.bms)
```
