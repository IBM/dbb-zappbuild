# Installation and Maintenance

This section provides a set of instructions on how you can make zAppBuild available in your Git provider and how to synchronize new features of zAppBuild into your customized fork.

Be aware that zAppBuild releases new versions through the main branch. New contributions are first added to the develop branch which will then be merged into the main branch.

## Make the zAppBuild repository available in your preferred Git provider

Before you start your customization of zAppBuild, clone the repository and store it in your Git provider of choice. This could be any Git provider, like GitHub, GitLab, Bitbucket or AzureDevOps etc. If you have done this already, feel free to move to the next section.

Here are the sample steps to make the zAppBuild repository available in an on-premise GitLab environment.

1. Use your Browser and log on to your Git provider. Create a new repository in GitLab, which will be the new “home” of your customized version of zAppBuild.
   1. In GitLab, navigate to the Group you would like to use.
   2. Create a New Project with your preferred name. We use “dbb-zappbuild”. Don’t initialize the repository. Set the visibility according to your needs.
   3. Your Git provider will create the repository, but it is not yet initialized.
2. Clone the public dbb-zappbuild repository. You can use IDz or a terminal to complete this step. We document the steps using a terminal.
   1. In the terminal, navigate to the folder where you would like to clone the repository.
   2. Retrieve the Git repository URL or SSH path from the public zAppBuild repository at https://github.com/IBM/dbb-zappbuild.
   3. Clone the repository by running the following command:
      ```sh
      git clone https://github.com/IBM/dbb-zappbuild.git
      ```
3. Follow the instructions of your Git provider to push the existing repository. (\* instructions may vary from Git provider to Git provider)
   1. Within the terminal session, execute the provided commands to Push an existing Git repository.
   2. You will find dbb-zappbuild with the entire history of the public git repository.

## Update your customized version of zAppBuild with latest official zAppBuild enhancements

It is assumed that you have now enhanced your fork and would like to integrate the latest version of zAppBuild into your repository.

1. Locate your internal git repository and create a new Git branch. It is a good practice to validate the updates first. In our sample it is called update-zappbuild.
2. Add a new Git remote definition to connect to the official zAppBuild repository. (\* Requires internet connectivity)
   1. List your repository's remotes by running the following command:
      ```sh
      git remote -v
      ```
      For more documentation see https://git-scm.com/docs/git-remote.
   2. Add the official zAppBuild repository as a new remote by issuing:
      ```sh
      git remote add zappbuild-official https://github.com/IBM/dbb-zappbuild.git
      ```
   3. Verify that it is available by issuing the previous command again.
      ```sh
      git remote -v
      ```
   4. Fetch the latest version from the official repository:
      ```sh
      git fetch zappbuild-official
      ```
   5. Ensure that your feature branch is checked out before you merge the changes from zappbuild-official. To merge the changes into your update-zappbuild branch issue:
      ```sh
      git merge zappbuild-official/main
      ```
      You may face merge conflicts. Run the following command to see which files changed:
      ```sh
      git status
      ```
   6. Open the unmerged files and resolve them manually using either the terminal or an IDE.
   7. Commit the changes and verify them by building a sample application before committing the updates to your main branch.

## References

- [zAppBuild Introduction and Custom Version Maintenance Strategy](https://www.ibm.com/support/pages/system/files/inline-files/zAppBuild_Introduction_and_Custom_Version_Maintenance_Strategy_20220321.pdf)
