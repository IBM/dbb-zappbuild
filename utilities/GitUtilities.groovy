@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.repository.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*

/*
 * Tests if directory is in a local git repository
 *
 * @param  String dir  		Directory to test
 * @return boolean		
 */

def isGitDir(String dir) {
	String cmd = "git -C $dir rev-parse --is-inside-work-tree"
	StringBuffer gitResponse = new StringBuffer()
	StringBuffer gitError = new StringBuffer()
	boolean isGit = false

	Process process = cmd.execute()
	process.waitForProcessOutput(gitResponse, gitError)
	if (gitError) {
		println("*? Warning executing isGitDir($dir). Git command: $cmd error: $gitError")
	}
	else if (gitResponse) {
		isGit = gitResponse.toString().trim().toBoolean()
	}

	return isGit
}

/*
 * Returns the current Git branch
 *
 * @param  String gitDir  		Local Git repository directory
 * @return String gitBranch     The current Git branch
 */
def getCurrentGitBranch(String gitDir) {
	String cmd = "git -C $gitDir rev-parse --abbrev-ref HEAD"
	StringBuffer gitBranch = new StringBuffer()
	StringBuffer gitError = new StringBuffer()

	Process process = cmd.execute()
	process.waitForProcessOutput(gitBranch, gitError)
	if (gitError) {
		println("*! Error executing Git command: $cmd error: $gitError")
	}
	return gitBranch.toString().trim()
}

/*
 * Returns the current Git branch in detached HEAD state
 *
 * @param  String gitDir  		Local Git repository directory
 * @return String gitBranch     The current Git branch
 */
def getCurrentGitDetachedBranch(String gitDir) {
	String cmd = "git -C $gitDir show -s --pretty=%D HEAD"
	StringBuffer gitBranch = new StringBuffer()
	StringBuffer gitError = new StringBuffer()

	Process process = cmd.execute();
	process.waitForProcessOutput(gitBranch, gitError)
	if (gitError) {
		println("*! Error executing Git command: $cmd error: $gitError")
	}

	String gitBranchString = gitBranch.toString()
	def gitBranchArr = gitBranchString.split(',')
	def solution = ""
	for (i = 0; i < gitBranchArr.length; i++) {
		if (gitBranchArr[i].contains("origin/")) {
			solution = gitBranchArr[i].replaceAll(".*?/", "").trim()
		}
	}

	return (solution != "") ? solution : println("*! Error parsing branch name: $gitBranch")
}

/*
 * Returns true if this is a detached HEAD
 *
 * @param  String gitDir  		Local Git repository directory
 */
def isGitDetachedHEAD(String gitDir) {
	String cmd = "git -C $gitDir status"
	StringBuffer gitStatus = new StringBuffer()
	StringBuffer gitError = new StringBuffer()

	Process process = cmd.execute()
	process.waitForProcessOutput(gitStatus, gitError)
	if (gitError) {
		println("*! Error executing Git command: $cmd error $gitError")
	}

	return gitStatus.toString().contains("HEAD detached at")
}

/*
 * Returns the current Git hash
 *
 * @param  String gitDir  		Local Git repository directory
 * @return String gitHash       The current Git hash
 */
def getCurrentGitHash(String gitDir) {
	String cmd = "git -C $gitDir rev-parse HEAD"
	StringBuffer gitHash = new StringBuffer()
	StringBuffer gitError = new StringBuffer()

	Process process = cmd.execute()
	process.waitForProcessOutput(gitHash, gitError)
	if (gitError) {
		print("*! Error executing Git command: $cmd error: $gitError")
	}
	return gitHash.toString().trim()
}

/*
 * Returns the current Git hash for this file path
 *
 * @param  String gitDir  		Local Git repository directory
 * @param  String filePath		filePath relative to gitDir
 * @return String gitHash       The current Git hash
 */
def getFileCurrentGitHash(String gitDir, String filePath) {
	String cmd = "git -C $gitDir rev-list -1 HEAD " + filePath
	StringBuffer gitHash = new StringBuffer()
	StringBuffer gitError = new StringBuffer()

	Process process = cmd.execute()
	process.waitForProcessOutput(gitHash, gitError)
	if (gitError) {
		print("*! Error executing Git command: $cmd error: $gitError")
	}
	return gitHash.toString().trim()
}

/*
 * Returns the current Git url
 *
 * @param  String gitDir  		Local Git repository directory
 * @return String gitUrl       The current Git url
 */
def getCurrentGitUrl(String gitDir) {
	String cmd = "git -C $gitDir config --get remote.origin.url"
	StringBuffer gitUrl = new StringBuffer()
	StringBuffer gitError = new StringBuffer()

	Process process = cmd.execute()
	process.waitForProcessOutput(gitUrl, gitError)

	if (gitError) {
		print("*! Error executing Git command: $cmd error: $gitError")
	}
	return gitUrl.toString().trim()
}


/*
 * Returns the lst previous Git commit hash
 * 
 * @param String gitDir       Local Git repository directory
 * @return String gitHash     The previous Git commit hash
 */
def getPreviousGitHash(String gitDir) {
	String cmd = "git -C $gitDir --no-pager log -n 1 --skip=1"
	StringBuffer gitStdout = new StringBuffer()
	StringBuffer gitError = new StringBuffer()

	Process process = cmd.execute()
	process.waitForProcessOutput(gitStdout, gitError)
	if (gitError) {
		print("*! Error executing Git command: $cmd error: $gitError")
	}
	else {
		return gitStdout.toString().minus('commit').trim().split()[0]
	}
}

/*
 * getChangedFiles - assembles a git diff command to support the impactBuild for a given directory
 *  returns the changed, deleted and renamed files.
 * 
 */
def getChangedFiles(String gitDir, String baseHash, String currentHash) {
	String gitCmd = "git -C $gitDir --no-pager diff --name-status $baseHash $currentHash"
	return getChangedFiles(gitCmd)
}

/*
 * getMergeChanges - assembles a git triple-dot diff command to support mergeBuild scenarios 
 *  returns the changed, deleted and renamed files between current HEAD and the provided baseline.
 *  
 */
def getMergeChanges(String gitDir, String baselineReference) {
	String gitCmd = "git -C $gitDir --no-pager diff --name-status remotes/origin/$baselineReference...HEAD"
	return getChangedFiles(gitCmd)
}

/*
 * getChangedFiles - internal method to submit the a gitDiff command and calucate and classify the idenfified changes 
 */
def getChangedFiles(String cmd) {
	def git_diff = new StringBuffer()
	def git_error = new StringBuffer()
	def changedFiles = []
	def deletedFiles = []
	def renamedFiles = []

	def process = cmd.execute()
	process.waitForProcessOutput(git_diff, git_error)

	// handle command error
	if (git_error.size() > 0) {
		println("*! Error executing Git command: $cmd error: $git_error")
		println ("*! Attempting to parse unstable git command for changed files...")
	}

	for (line in git_diff.toString().split("\n")) {
		// process files from git diff
		try {
			gitDiffOutput = line.split()
			action = gitDiffOutput[0]
			file = gitDiffOutput[1]

			if (action == "M" || action == "A") { // handle changed and new added files
				changedFiles.add(file)
			} else if (action == "D") {// handle deleted files
				deletedFiles.add(file)
			} else if (action.startsWith("R")) { // handle renamed file
				renamedFile = gitDiffOutput[1]
				newFileName = gitDiffOutput[2]
				changedFiles.add(newFileName) // will rebuild file
				renamedFiles.add(renamedFile)
				//evaluate similarity score
				similarityScore = action.substring(1) as int
				if (similarityScore < 50){
					println ("*! (GitUtils.getChangedFiles - Renaming Scenario) Low similarity score for renamed file $renamedFile : $similarityScore with new file $newFileName. ")
				}

			}
			else {
				println ("*! (GitUtils.getChangedFiles) Error in determining action in git diff. ")
				println ("*! (GitUtils.getChangedFiles) Git diff output: $line. ")
			}
		}
		catch (Exception e) {
			// no changes or unhandled format
		}
	}

	return [
		changedFiles,
		deletedFiles,
		renamedFiles
	]
}

def getCurrentChangedFiles(String gitDir, String currentHash, String verbose) {
	if (verbose) println "** Running git command: git -C $gitDir show --pretty=format: --name-status $currentHash"
	String cmd = "git -C $gitDir show --pretty=format: --name-status $currentHash"
	def gitDiff = new StringBuffer()
	def gitError = new StringBuffer()
	def changedFiles = []
	def deletedFiles = []
	def renamedFiles = []

	Process process = cmd.execute()
	process.waitForProcessOutput(gitDiff, gitError)

	// handle command error
	if (gitError.size() > 0) {
		println("*! Error executing Git command: $cmd error: $gitError")
		println ("*! Attempting to parse unstable git command for changed files...")
	}

	for (line in gitDiff.toString().split("\n")) {
		if (verbose) println "** Git command line: $line"
		// process files from git diff
		try {
			gitDiffOutput = line.split()
			action = gitDiffOutput[0]
			file = gitDiffOutput[1]

			if (action == "M" || action == "A") { // handle changed and new added files
				changedFiles.add(file)
			} else if (action == "D") {// handle deleted files
				deletedFiles.add(file)
			} else if (action == "R100") { // handle renamed file
				renamedFile = gitDiffOutput[1]
				newFileName = gitDiffOutput[2]
				changedFiles.add(newFileName) // will rebuild file
				renamedFiles.add(renamedFile)
			}
			else {
				println ("*! (GitUtils.getChangedFiles) Error in determining action in git diff. ")
				println ("*! (GitUtils.getChangedFiles) Git diff output: $line. ")
			}
		}
		catch (Exception e) {
			// no changes or unhandled format
		}
	}

	return [
		changedFiles,
		deletedFiles,
		renamedFiles
	]
}

def getChangedProperties(String gitDir, String baseline, String currentHash, String propertiesFile) {
	String cmd = "git -C $gitDir diff --ignore-all-space --no-prefix -U0 $baseline $currentHash $propertiesFile"

	def gitDiff = new StringBuffer()
	def gitError = new StringBuffer()
	Properties changedProperties = new Properties()

	Process process = cmd.execute()
	process.waitForProcessOutput(gitDiff, gitError)

	for (line in gitDiff.toString().split("\n")) {
		if (line.startsWith("+") && line.contains("=")){
			try {
				gitDiffOutput = line.substring(1)
				changedProperties.load(new StringReader(gitDiffOutput));
			}
			catch (Exception e) {
				// no changes or unhandled format
			}
		}
	}

	return changedProperties.propertyNames()
}