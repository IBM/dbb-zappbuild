# Reporting capabilities within zAppBuild

zAppBuild provides a set of reporting capabilities, which are part of the build framework itself to address some common demands of mainframe development teams.

Developers are particularly interested if
* a change of an element impacts other application components which are managed in a different repository.
* their changes on an isolated feature branch potentially cause a conflict with some concurrent development done by others within the same repository.

While during the analysis phase of a new task, the developer leverages application dependency understanding tools such as IBM Application Discovery, it is beneficial to enable also the build framework to automatically generate reports to answer the above questions.

By default these reporting capabilities are turned off, while they don't belong to the core functionalities of a build framewoek.

## Report External Impacts 

### Purpose

The _Reporting External Impacts_ feature enables the build framework to generate reports about impacted files in other application contexts to document cross application impacts for files that were changed within your application scope.

### Pre-requisites

Technically, this feature analyzes the list of changed files and queries the collection of other applications within the DBB WebApp, so it requires that the other applications are also processed through the CI/CD pipeline with DBB. It requires a repository connection.

### Configuration

You can configure the feature through [application-conf/reports.properties](samples/application-conf/reports.properties). Within the property file, you can activate it and further configure a filter to run the analysis only for a subset of files in your repository see `reportExternalImpactsAnalysisFileFilter` and limit the scope of the external collections (see `reportExternalImpactsCollectionPatterns`), which should be queried.

### Sample invocation

In the below sample, the MortgageApplication was split into two applications App-EPSC and App-EPSM to demonstrate the feature from a user perspective:

App-EPSM provides a shared copybook `epsmtcom.cpy`, which is included in programs of App-EPSC. While the overall build strategy does not intend to force an automated rebuild in App-EPSC for the changed shared artifact, however would like the capabilitiy to provide a report to the application team, which owns this shared element.

The build console output for App-EPSM will contain the below section (with `--verbose`):

```
** Analyse and report external impacted files.
*** Identified external impacted files for changed file App-EPSM/copybooks-public/epsmtcom.cpy
*** Writing report of external impacts to file /var/jenkins/workspace/App-EPSM-S4/outputs/build.20210722.032455.024/externalImpacts_App-EPSC-master.txt
```

For each collection in scoope of the analysis with impacted files a report is generated and written (in this case `externalImpacts_App-EPSC-master.txt`) to the `workdir`. A report will contain three columns (logicalFile, filePath and collectionName) with the external impacted files: 

```
EPSCMORT 	 App-EPSC/cobol/epscmort.cbl 	 App-EPSC-master
```


**Important considerations**
- Use the pattern configuration to limit the query to those collections which are relevant and avoid unnecessary processing.
- In most cases it is sufficient to run with the **simple** mode, which performs much faster, because it does not resolve impacts recursively. **deep** mode in fact requires searchPaths with the rules been correctly configured for the ImpactResolver API.

## Report potential conflicts

### Purpose

Concurrent development activies in separated isolated branches on the same source code, lead to the need to merge the code at some point in time. Git does an excellent job for this task and supports the developer for this task. 

While pessimistic locking is a common practise on the mainframe, developer will need to keep an eye on what is happening on the repository.

The _Report potential conflicts_ feature can be activated to generate reports to document changes in the common code base configuration.

### Functionality

This feature compares two different configurations via a `git diff`. It runs a git diff between the `mainBuildBranch` and the current configuration to capture changes on the mainBuildBranch. These changes are reported within the build console output. 

Additionally to the reporting, it verifies if the list of the current build files intersect with the identified changes of the mainBuildBranch. If the lists intesect, another notification is reported in the build log which can make the build be marked as failed and force the development team to integrate changes and rebase the code before they move on.  

### Pre-requisites

The feature relies on git functionality. Therefore it is only available in pipeline builds and not for user build scenarios.

It requires that the cloned repository in the build workspace contains the git references (git-refs) to function.

### Sample invocation

to be documented