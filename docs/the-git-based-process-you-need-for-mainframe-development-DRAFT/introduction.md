>**September 2023 - Release 1.0**
>
>**The Git-based process you need for mainframe development**
>
>**(Draft Document Sept 1,2023)**  



**Table of contents**

[Introduction](#introduction)

[The Git-based development process for Mainframe development](the-git-based-development-process-for-mainframe-development.md#the-git-based-development-process-for-mainframe-development)

[Pipeline design and implementation supporting the workflows](pipeline-design-and-implementation-supporting-the-workflows.md#pipeline-design-and-implementation-supporting-the-workflows)

[Conclusion](conclusion.md#conclusion)

[Appendix](appendix.md#appendix)

# Introduction

Git branching strategies are patterns to enable development teams using Git to manage their code in a streamlined manner. Since Git is established as a de-facto standard for source-code management in the developers community, several approaches were designed to fulfill users' requirements and manage applications source code lifecycle, with advantages and drawbacks depending on use cases. Based on the experience gained designing branching strategies, this document describes a blueprint implementation of mainline-based development approach for mainframe applications using feature branches with an early integration pattern. This setup leverages a standardized development toolset based on an enterprise-wide Git provider and a Continuous Integration/Continuous Deployment (CI/CD) toolchain.

A characteristic of this integration pattern is that developers are implementing changes for a planned release and integrate their changes into a common permanent branch (the shared configuration) that is built, tested, and released together as one consistent entity.

The purpose of streamlining both the DevOps solutions and the delivery workflow is to simplify the process for development teams to deliver quality product releases on time. This enables agile development practices that allow the teams to respond more effectively to changes in the market and customer needs. The first part of the document introduces the model and outlines the development workflow from the developer's perspective. The details of the technical implementation with IBM Dependency Based Build and zAppBuild as well as packaging and deployment are discussed in the second part. All branching models are adaptable to the needs of specific teams and their applications. This document advocates for best practices and indicates where variations can be applied.

The target audience of this document are Mainframe DevOps architects and SCM specialists interested in learning how to design and implement a CI/CD pipeline with a robust and state-of-the-art development workflow.

## Aims and Assumptions

Some aims and assumptions which guided our recommendations include:

- The workflow and branching scheme should both scale-up and scale-down.
  - Small teams with simple and infrequent changes will be able to easily understand, adopt and have a good experience. 
  - Large busy teams with many concurrent activities will be able to plan, track and execute with maximum agility using the same fundamental principles.

- Planning and design activities as well as code development aim to align to a regular release cadence.

- There is no magic answer to managing large numbers of 'in-flight' changes, so planning assumptions should aim as much as possible to complete changes quickly, ideally within one release cycle.

- We know it is sometimes unavoidable for work to take longer than one release cycle and we accommodate that as a variant of the base workflow.

## Choosing a workflow and branching scheme

Your choice of workflow and the branching scheme which supports it need to take account of your team's needs and characteristics.

Aspects to consider include:

- Size of the team
- Frequency of change
- Granularity of change
- Amount of parallel development
- Formality of release process

Our recommended workflow and branching scheme is flexible enough to scale from small teams with an infrequent pattern of small changes to
large and busier teams with many concurrent projects and projects spanning multiple cycles of a formal release cadence.

## Summary... all of it in one diagram

```mermaid
%%{init: { 'fontFamily': 'IBM Plex Sans', 'logLevel': 'debug', 'theme': 'base', 'gitGraph': {'showBranches': true, 'showCommitLabel':true,'mainBranchOrder': 4}} }%%
      gitGraph
      commit
      branch release-2.1.0 order: 3
      commit tag: "2.1.0"
      checkout main
      commit
      checkout release-2.1.0
      commit tag: "hotfix-2.1.0.1"
      checkout main
      branch feature-002 order: 5
      commit
      checkout main
      branch feature-003 order: 6
      commit
      commit
      checkout main
      merge feature-002
      branch hot-fix-2.1.0.1 order: 6
      commit id:"hf-2.1.0.1"
      checkout main
      merge hot-fix-2.1.0.1
      checkout release-2.1.0
      cherry-pick id:"hf-2.1.0.1"
      %% there is an oddity in colour and routing of the line
      %% if there is not a commit before the merge.
      checkout feature-003
      commit
      checkout main
      merge feature-003
      branch release-2.2.0 order: 2
      commit tag: "2.2.0"
      checkout main
      commit
      branch epic-004 order: 7
      commit
      branch feature-004-1 order: 8
      commit
      checkout main
      branch release-2.3.0 order: 1
      commit tag: "2.3.0"
      checkout epic-004
      merge feature-004-1
      branch feature-004-2 order: 8
      commit
      checkout epic-004
      merge feature-004-2
      checkout main
      merge epic-004
      branch release-2.4.0 order: 0
      commit tag:"2.4.0"
```
And then, of course, you can build it up incrementally too...

- Everything starts with a branch:
  
```mermaid
%%{init: { 'fontFamily': 'IBM Plex Sans', 'logLevel': 'debug', 'theme': 'base', 'gitGraph': {'showBranches': true, 'showCommitLabel':true,'mainBranchOrder': 4}} }%%
      gitGraph
      commit
      commit
      branch feature-001 order: 5
      commit
```
- When on a branch, developers commit changes in isolation:
  
```mermaid
%%{init: { 'fontFamily': 'IBM Plex Sans', 'logLevel': 'debug', 'theme': 'base', 'gitGraph': {'showBranches': true, 'showCommitLabel':true,'mainBranchOrder': 4}} }%%
      gitGraph
      commit
      commit
      branch feature-001 order: 5
      commit
      commit
```
- Every branch can be built and tested.
- When ready, the feature branch can be reviewed and approved to merge into `main`:
```mermaid
%%{init: { 'fontFamily': 'IBM Plex Sans',
           'logLevel': 'debug', 'theme': 'base', 'gitGraph': {'showBranches': true, 'showCommitLabel':true,'mainBranchOrder': 4}} }%%
      gitGraph
      commit
      commit
      branch feature-001 order: 5
      commit
      commit
      checkout main
      merge feature-001
```
- Multiple features can be developed concurrently:
```mermaid
%%{init: { 'fontFamily': 'IBM Plex Sans',
           'logLevel': 'debug', 'theme': 'base', 'gitGraph': {'showBranches': true, 'showCommitLabel':true,'mainBranchOrder': 4}} }%%
      gitGraph
      commit
      commit
      branch feature-001 order: 5
      commit
      commit
      checkout main
      branch feature-002 order: 6
      commit
      checkout main
      merge feature-001
      checkout feature-002
      commit
      checkout main
      merge feature-002
```
- or serially:
```mermaid
%%{init: { 'fontFamily': 'IBM Plex Sans',
           'logLevel': 'debug', 'theme': 'base', 'gitGraph': {'showBranches': true, 'showCommitLabel':true,'mainBranchOrder': 4}} }%%
      gitGraph
      commit
      commit
      branch feature-001 order: 5
      commit
      commit
      checkout main
      branch feature-002 order: 6
      commit
      checkout main
      merge feature-001
      branch feature-003 order: 6
      commit
      commit
      checkout main
      merge feature-003
      checkout feature-002
      commit
      checkout main
      merge feature-002
```
- And then a release candidate can be created:
```mermaid
%%{init: { 'fontFamily': 'IBM Plex Sans',
           'logLevel': 'debug', 'theme': 'base', 'gitGraph': {'showBranches': true, 'showCommitLabel':true,'mainBranchOrder': 4}} }%%
      gitGraph
      commit
      commit
      commit
      branch release-2.1.0 order: 2
      commit id:"rel-2.1.0"
      checkout main
      commit
 ```
 *(note: commits to `main` here are, of course, merges of approved branches)*

 - Fixes are handled as branches from `main`, which are tested, approved, merged to `main` and *cherry-picked* to add to the release branch:
~~~mermaid
%%{init: { 'fontFamily': 'IBM Plex Sans',
           'logLevel': 'debug', 'theme': 'base', 'gitGraph': {'showBranches': true, 'showCommitLabel':true,'mainBranchOrder': 4}} }%%
      gitGraph
      commit
      commit
      commit
      branch release-2.1.0 order: 2
      commit id:"rel-2.1.0"
      checkout main
      branch hot-fix-2.1.0.1 order: 5
      commit id:"hf-2.1.0.1"
      checkout main
      merge hot-fix-2.1.0.1
      checkout release-2.1.0
      cherry-pick id:"hf-2.1.0.1"
      checkout main
      commit
~~~
- Features can run over release boundaries:
```mermaid
%%{init: { 'fontFamily': 'IBM Plex Sans',
           'logLevel': 'debug', 'theme': 'base', 'gitGraph': {'showBranches': true, 'showCommitLabel':true,'mainBranchOrder': 4}} }%%
      gitGraph
      commit
      commit
      branch feature-001 order: 5
      commit
      commit
      checkout main
      branch feature-002 order: 6
      commit
      commit
      checkout main
      merge feature-001
      branch release-2.1.0 order: 2
      commit id:"rel-2.1.0"
      checkout feature-002
      commit
      commit
      checkout main
      merge feature-002
```
