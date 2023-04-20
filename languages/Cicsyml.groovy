@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.metadata.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import com.ibm.dbb.build.report.records.*
import com.ibm.dbb.build.report.*
import groovy.transform.*
/*
The language script is for running the CICS tool (zrb) on CICS definitions in YML (yaml)
format.
The system should have a Env variable ZRB_LOC which would point to the zrb executable.
The script will get the CICS yml file names from the buildlist and proceed to execute 
the zrb executable on them. This will create a CSD formatted file, which can be stored as an artifact 
or can be run on Z/OS through DFHCSDUP or CicsDef.groovy script
*/
// define script properties
@Field BuildProperties props = BuildProperties.getInstance()

//Get the value of the ZRB_LOC environment variable
def zrbPath = System.getenv("ZRB_LOC")
def modelFile, resFile, appFile
// zrb command to execute
def zrb_cmd = ""
//Set the o/p file name 
def outputFile = "commands.out"

List<String> buildList = argMap.buildList

def artiServerName = 'jfrog-tass-sachin1'
def serverA = Artifactory.server artiServerName

File zrbExe = new File(zrbPath)
if (!zrbExe.exists()){
    String errorMsg = "*! The zrb tool is not found "
	println(errorMsg)
	props.error = "true"
//	buildUtils.updateBuildResult(errorMsg:errorMsg)
}



println("** Processing files mapped to ${this.class.getName()}.groovy script")

// typically to run zrb we need 3 files 
assert buildList.size() == 3

// iterate through build list
buildList.each { buildFile ->
	println "*** Documenting changed file $buildFile in DBB Build report"

	String absolutePath = buildUtils.getAbsolutePath(buildFile)
	println absolutePath
	
	File absoluteFile = new File("$absolutePath")

	if (absoluteFile.exists()){
        // Best guess on the file type
        //absoluteFile.eachLine('ISO8859-1') 
         absoluteFile.eachLine { line ->
            line = line.trim()
            if (line.startsWith('#'))  
                skip
            else if(line.startsWith("resourceModel:"))
                modelFile = absolutePath
            else if(line.startsWith("resourceDefinitions:"))
                 resFile = absolutePath
            else if(line.startsWith("application:"))
                 appFile = absolutePath        
        }
    }else {
		println "$absolutePath does not exist."
	}

    //Build the shell command to execute  
    zrb_cmd = zrbPath + " build --model"
    zrb_cmd = zrb_cmd + " " + modelFile + " --application " + appFile + " --resources " + resFile + " --output " + outputFile

    def sout = new StringBuilder()
    def serr = new StringBuilder()
    //execute the command
    def pExe = zrb_cmd.execute()
    pExe.consumeProcessOutput(sout, serr)
    pExe.waitFor()
    println sout + "  " + pExe.exitValue() 
    if (pExe.exitValue()  > 4){
            println("!!! ERROR !!!! " + pExe.exitValue() )
            println pExe.text
    }
}

// iterate through build list for build record
buildList.each { buildFile ->        
    String absolutePath = buildUtils.getAbsolutePath(buildFile)
    //create a new record of type AnyTypeRecord
    AnyTypeRecord ussRecord = new AnyTypeRecord("USS_RECORD")
    // set attributes
    ussRecord.setAttribute("file", buildFile)
    ussRecord.setAttribute("label", "CICS YAML")
    ussRecord.setAttribute("outputfile", absolutePath)

    // add new record to build report
    if(props.verbose) "* Adding USS_RECORD for $buildFile"
    BuildReportFactory.getBuildReport().addRecord(ussRecord)
} 

