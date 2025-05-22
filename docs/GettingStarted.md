# Getting started with zAppbuild

zAppBuild is a free, generic mainframe application build framework that customers can extend to meet their DevOps needs. It is available under the Apache 2.0 license, and is a sample to get you started with building Git-based source code on z/OS UNIX System Services (z/OS UNIX). It is made up of property files to configure the build behavior, and Groovy scripts that invoke the DBB toolkit APIs.

Build properties can span across all applications (enterprise-level), one application (application-level), or individual programs. Properties that cross all applications are managed by administrators and define enterprise-wide settings such as the PDS name of the compiler, data set allocation attributes, and more. Application- and program-level properties are typically managed within the application repository itself.

The zAppBuild framework is invoked either by a developer using the "User Build" capability in their integrated development environment (IDE), or by an automated CI/CD pipeline. It supports different build types.

## Making zAppBuild available in your Git provider

Before you start your customization of zAppBuild, you must first create a clone of IBM's zAppBuild repository and store the clone in your Git provider of choice. This could be any Git provider, such as GitHub, GitLab, Bitbucket or Azure Repos, and so on. If you have done this already, feel free to move to the next section.

Here are the steps to make the zAppBuild repository available in a central repository on your Git provider:

1. On your local workstation, use your browser and log on to your Git provider. Follow the instructions in your Git provider to create a new repository, which will be the new **home** of your customized version of zAppBuild.
    1. We suggest **dbb-zappbuild** as the new repository/project name, but you can use another name if you prefer.
    2. Set the repository's visibility according to your needs.
    3. Do not initialize the repository yet.
        - Your Git provider creates the repository, but it is not yet initialized. On most Git providers, the repository creation process ends on a page with information on how to share an existing Git repository. Leave the browser open.
2. Clone IBM's public [zAppBuild](https://github.com/IBM/dbb-zappbuild) repository to your local workstation. You can use your local workstation's terminal to complete this step (for example, Git Bash in Windows, or Terminal on MacOS).
    - If you are using IBM Developer for z/OS (IDz) as your IDE, you can use its [Local Shell](https://www.ibm.com/docs/en/developer-for-zos/16.0?topic=view-running-viewing-commands-using-remote-shell). Wazi for VS Code and Wazi for Dev Spaces also both have Terminal windows. (We documented the steps in this guide using a terminal.)
    1. In the terminal, navigate to the folder where you would like to clone the repository.
    2. Retrieve the Git repository URL or SSH path from IBM's public [zAppBuild repository](https://github.com/IBM/dbb-zappbuild):

        ![Retrieving the Git repository URL from IBM's public zAppBuild repository](../static/img/dbb-zappbuild-code.png)
    3. In your terminal, enter the command for cloning the repository. (The following command uses the Git repository URL, but the SSH path can also be used if you have SSH keys set up.):

        ```shell
        git clone https://github.com/IBM/dbb-zappbuild.git
        ```

        - Example Git clone command with output in a terminal:

          ![Example Git clone command with output in a terminal](../static/img/dbb-zappbuild-clone-output.png)
3. Follow the instructions of your Git provider to push the contents of your newly-cloned local repository (from Step 2) to the central repository on your Git provider (created in Step 1). (Note: Exact instructions may vary from Git provider to Git provider.)
    1. Within the terminal session, execute the following commands to push an existing Git repository:
        - Replace `<existing_repo>` with the path to your newly-cloned local repository.
        - Replace `<Your central Git repository>` with the URL to the new central repository on your Git provider. (For example, with GitLab as the Git provider, the URL might look similar to `git@gitlab.dat.ibm.com:DAT/dbb-zappbuild.git`.)

        ```shell
        cd <existing_repo>
        git remote rename origin old-origin
        git remote add origin <Your central Git repository>
        git push -u origin –all
        git push -u origin --tags
        ```

    2. On the Git provider's webpage for your new central repository in the browser, you will find that the repository is now populated with all of zAppBuild's files and history, just like on IBM's public [zAppBuild](https://github.com/IBM/dbb-zappbuild) repository.
        - The following screenshot shows an example of a populated central zAppBuild repository with GitLab as the Git provider:

            ![Example of a populated central zAppBuild repository in GitLab's web UI](../static/img/dbb-zappbuild-pushed.png)

## Updating your customized version of zAppBuild

Once you have a clone of zAppBuild in your Git provider, you can customize the properties and scripts within it according to your organization's needs. The [zAppBuild wiki](https://github.com/IBM/dbb-zappbuild/wiki) documents some possible zAppBuild customization scenarios. Additionally, IBM regularly releases new versions of the official zAppBuild repository to deliver enhancements and bug fixes. To integrate the latest official zAppBuild updates into your customized version of zAppBuild, you can use the following process:

1. Locate the internal Git repository and create a new Git branch. This is a good practice to validate the changes first. In this example, the new branch is called `update-zappbuild`.
2. Add a new Git remote definition to connect to IBM's official public zAppBuild GitHub repository. (Note: This step requires internet connectivity.)
    1. First, list the remotes by issuing `git remote -v`:

        ![Output from initial listing of the remote tracked repositories](../static/img/dbb-zappbuild-remote-v1.png)

        - For more on Git remotes, see the [git-remote](https://git-scm.com/docs/git-remote) documentation.

    2. Add a new remote named `zappbuild-official` to connect to GitHub by issuing the following command:

        ```shell
        git remote add zappbuild-official https://github.com/IBM/dbb-zappbuild.git
        ```

    3. Verify that the new remote is available by issuing the command to list the remotes again: `git remote -v`:

        ![Output from listing the remote tracked repositories after adding the zappbuild-official remote](../static/img/dbb-zappbuild-remote-v2.png)

    4. Fetch the latest information from the official repository, by executing a Git fetch for the official dbb-zappubuild repository:

        ```shell
        git fetch zappbuild-official
        ```

        ![Output from Git fetch of the zappbuild-official remote](../static/img/dbb-zappbuild-fetch.png)

    5. Make sure that your feature branch is checked out, before attempting to merge the changes from zappbuild-official. To merge the changes run into your branch `update-zappbuild`, run the following command:

        ```shell
        git merge zappbuild-official/main
        ```

        ![Merge conflict when attempting to merge in changes from zappbuild-official](../static/img/dbb-zappbuild-conflict.png)

        Potentially, you face merge conflicts. In the above case, the merge processor could not automatically resolve the `utilities/ImpactUtilities.groovy`.

        Run the command `git status` to see which files changed:

        ![Output of Git status to view files changed during the merge attempt](../static/img/dbb-zappbuild-status.png)

    6. Open the unmerged files and resolve them manually. Either use the terminal, or an IDE for this task.

        :::tip

        The Git integration in many modern IDEs (for example, VS Code) is able to provide a side-by-side comparison highlighting the diff between your feature branch and the incoming changes from the merge attempt (in this case, from `zappbuild-official`). This can help make manual resolution of any merge conflicts much easier.

        :::

    7. Commit the changes and verify them with a sample application before committing it (or opening a pull request to commit it) to your main branch that is used for all your production DBB builds.
