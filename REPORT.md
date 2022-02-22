# Reporting capabilities within zAppBuild

zAppBuild provides a set of reporting capabilities, which are part of the build framework itself to address some common demands of mainframe development teams.

Developers are particularly interested if:
* A change of an element impacts other application components which are managed in a different repository.
* Their changes on an isolated feature branch potentially cause a conflict with some concurrent development done by others within the same repository.

During the analysis phase, while a developer might leverage tools for application understanding such as IBM Application Discovery, it is also beneficial to enable the build framework to automatically generate reports to answer the above questions.

By default these reporting capabilities are turned off since it is normally not part of the standard build framework workflow.

## Report External Impacts 

### Purpose

The _Reporting External Impacts_ feature enables the build framework to generate reports about impacted files in other application contexts to document cross application impacts for files that were changed within your application scope.

The reports is meant to be used to start the collaboration process with the other application teams about their adoption process of the shared resource, typically a copybook. A future idea is to use the reports to add files to the build list so that the consuming application can built their impacted files.

### Pre-requisites

Technically, this feature analyzes the list of changed files for the current application and then queries the collections of other applications within the DBB WebApp. Therefore, to fully analyze cross application impacts, the other applications must also be part of and processed through the CI/CD pipeline with DBB. It requires a DBB WebApp repository connection.

### Configuration

You can configure the feature through [application-conf/reports.properties](samples/application-conf/reports.properties). Please check out the description of the properties in [README.md](samples/application-conf/README.md#reportsproperties)

Use the build property in property file to activate the feature and further configure it like defining a filter to run the analysis only for a subset of files in your repository see `reportExternalImpactsAnalysisFileFilter` and to limit the scope of the external collections (see `reportExternalImpactsCollectionPatterns`), which should be queried.
### Sample invocation

In the below sample, the MortgageApplication was split into two applications App-EPSC and App-EPSM to demonstrate the feature from a user perspective:

App-EPSM provides a shared copybook `epsmtcom.cpy`, which is included in programs of App-EPSC. While the overall build strategy does not intend to force an automated rebuild in App-EPSC for the changed shared artifact, however would like the capability to provide a report to the application team, which owns this shared element.

The build console output for App-EPSM will contain the below section (with `--verbose`):

```
** Analyse and report external impacted files.
*** Identified external impacted files for changed file App-EPSM/copybooks-public/epsmtcom.cpy
*** Writing report of external impacts to file /var/jenkins/workspace/App-EPSM-S4/outputs/build.20210722.032455.024/externalImpacts_App-EPSC-master.txt
```

For each collection in scope of the analysis with impacted files a report is generated and written (in this case `externalImpacts_App-EPSC-master.txt`) to the `workdir`. A report will contain three columns (logicalFile, filePath and collectionName) with the external impacted files: 

```
EPSCMORT 	 App-EPSC/cobol/epscmort.cbl 	 App-EPSC-master
```


**Important considerations**
- Use the pattern configuration to limit the query to those collections which are relevant and avoid unnecessary processing.
- In most cases it is sufficient to run with the **simple** mode, which performs much faster, because it does not resolve impacts recursively. **deep** mode in fact requires searchPaths with the rules been correctly configured for the ImpactResolver API.

## Report concurrent changes

### Purpose

Concurrent development activies in separated isolated branches on the same source code, lead to the need to merge the code at some point in time. Git does an excellent job for this task and supports the developer for this task. While pessimistic locking is a common practise on the mainframe, developers will need to keep an eye on what is happening in the git repository, which follows the optimistic locking approach.

The _Report concurrent changes_ feature can be activated to generate a report to document changes in other configurations within the repository. Additionally, it validates if the current build list intersects with those changes.

### Functionality

This feature compares the current configuration to several other configurations via a `git diff`. It runs a git diff between current and the configured concurrent branches (`reportConcurrentChangesGitBranchReferencePatterns` - a list of regex patterns) to capture ongoing activities in concurrent configurations, which are not applied to the current configuration which is built. A report file within the build output directory is produced to document the changes and potential conflicts.

Additionally to the reporting, it verifies if the list of the current build files intersect with the identified changes of the concurrent branches. If the lists intersect, an additional notification is reported in the build log and the build result. Depending on the configuration, it can make the build be marked as failed and force the development team to integrate changes and rebase the code before they move on.  

### Pre-requisites

The feature relies on git functionality. Therefore it is only available in incremental pipeline builds for the build types `--impactBuild` and `--mergeBuild` and not for user build or full build scenarios.

It requires that the cloned repository in the build workspace contains the git references (git-refs) to function. Please verify this in a test environment first, while not all pipeline orchestrators fetch the git references by default and might require additional configuration of the fetch process. 

### Configuration

Please review the build properties defined in [application-conf/reports.properties](samples/application-conf/reports.properties) to configure the reporting of concurrent changes. 

You can specify a list of regex patterns for those git references (branches) which should be considered in the analysis of potential conflicts. It also takes fully qualified names. Please note, that the implementation performs a `git branch -r` to dynamically obtain other branches based on the applicationSrcDirs . Limitation: It does not support the analysis across multiple git repositories configured in the build scope!

In the below sample configuration for `reportConcurrentChangesGitBranchReferencePatterns`, the analysis will run for the mainBuildBranch, all branches containing the word `main`, the branches `main` and `release`, and all branches starting with `feature`.

```
reportConcurrentChangesGitBranchReferencePatterns=${mainBuildBranch},.*main.*,main,release,feature.*
```

The results of the analysis are written to a file called `report_concurrentChanges.txt' within the build workspace within the build out directory.   

### Sample invocation

To document the functionality of the feature, the source code `MortgageApplication/cobol/epscmort.cbl` was changed on the main branch after the feature branch was forked to simulate concurrent development activities. 

On the feature branch `feature-1122`, the communication copybook `MortgageApplication/copybook/epsmtcom.cpy` was changed. During the impactBuild the the below build list is itendified: 

```
MortgageApplication/cobol/epsmlist.cbl
MortgageApplication/cobol/epscsmrt.cbl
MortgageApplication/cobol/epscmort.cbl
MortgageApplication/link/epsmlist.lnk 
```
While the above build list intersects with the change on the branch main and the setting `reportConcurrentChangesIntersectionFailsBuild=true` is activated, a warning is written to the build console output and the build state is flagged as Error:
```
** Build start at 20211221.110944.009
** Repository client created for https://10.3.20.96:10443/dbb
** Build output located at /u/ibmuser/outDir/mortgageout/build.20211221.110944.009
** Build result created for BuildGroup:MortgageApplication-feature-1122 BuildLabel:build.20211221.110944.009 at https://10.3.20.96:10443/dbb/rest/buildResult/62001
** --impactBuild option selected. Building impacted programs for application MortgageApplication
* MortgageApplication/cobol/epscmort.cbl is changed on branch main and intersects with the current build list.
** Writing build list file to /u/ibmuser/outDir/mortgageout/build.20211221.110944.009/buildList.txt
** Invoking build scripts according to build order: BMS.groovy,Cobol.groovy,LinkEdit.groovy
** Building files mapped to Cobol.groovy script
*** Building file MortgageApplication/cobol/epsmlist.cbl
*** Building file MortgageApplication/cobol/epscsmrt.cbl
*** Building file MortgageApplication/cobol/epscmort.cbl
** Building files mapped to LinkEdit.groovy script
*** Building file MortgageApplication/link/epsmlist.lnk
** Writing build report data to /u/ibmuser/outDir/mortgageout/build.20211221.110944.009/BuildReport.json
** Writing build report to /u/ibmuser/outDir/mortgageout/build.20211221.110944.009/BuildReport.html
** Updating build result BuildGroup:MortgageApplication-feature-1122 BuildLabel:build.20211221.110944.009 at https://10.3.20.96:10443/dbb/rest/buildResult/62001
** Build ended at Tue Dec 21 23:09:56 GMT+01:00 2021
** Build State : ERROR
** Total files processed : 4
** Total build time  : 11.836 seconds

** Build finished
````
Contents of the report_concurrentChanges.txt file look like:
```
===============================================
** Report for configuration: main
========
** Changed Files
* MortgageApplication/cobol/epscmort.cbl is changed and intersects with the current build list.                       
````

In a mergeBuild scenario, where the build list does not intersect with the changes on the concurrent branch, the build passes as expected. The report for concurrent development activities is written to the build output directory.

```
** Build start at 20211221.111003.010
** Repository client created for https://10.3.20.96:10443/dbb
** Build output located at /u/ibmuser/outDir/mortgageout/build.20211221.111003.010
** Build result created for BuildGroup:MortgageApplication-feature-1122 BuildLabel:build.20211221.111003.010 at https://10.3.20.96:10443/dbb/rest/buildResult/62013
** --mergeBuild option selected. Building changed programs for application MortgageApplication flowing back to main
** Writing build list file to /u/ibmuser/outDir/mortgageout/build.20211221.111003.010/buildList.txt
** Invoking build scripts according to build order: BMS.groovy,Cobol.groovy,LinkEdit.groovy
** Building files mapped to Cobol.groovy script
*** Building file MortgageApplication/cobol/epsmlist.cbl
** Writing build report data to /u/ibmuser/outDir/mortgageout/build.20211221.111003.010/BuildReport.json
** Writing build report to /u/ibmuser/outDir/mortgageout/build.20211221.111003.010/BuildReport.html
** Updating build result BuildGroup:MortgageApplication-feature-1122 BuildLabel:build.20211221.111003.010 at https://10.3.20.96:10443/dbb/rest/buildResult/62013
** Build ended at Tue Dec 21 23:10:09 GMT+01:00 2021
** Build State : CLEAN
** Total files processed : 1
** Total build time  : 5.158 seconds

** Build finished
```
Contents of the reported report_concurrentChanges_main.txt file look like:
```
===============================================
** Report for configuration: main
========
** Changed Files
  MortgageApplication/cobol/epscmort.cbl

````