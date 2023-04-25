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
The crb.properties file should provide 
 - the zrb location on the host
 - the path to the Model file 
 - the application constraints file
The script will get the resource CICS yml file names from the buildlist and proceed to execute 
the zrb executable on them. This will create a CSD formatted file, which can be stored as an artifact 
or can be run on Z/OS through DFHCSDUP or CicsDef.groovy script
*/

// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
// verify required build properties
buildUtils.assertBuildProperties(props.CRB_requiredBuildProperties)

//Get the path to the zrb executable
def zrbPath = props.CRB_LOC
//set the model and appl constraint paths
def modelFile = props.CRB_App
def appFile = props.CRB_Model

def resFile, extIndex

// zrb command to execute
def zrb_cmd = ""
//Set the o/p file name 
def outputFile
//if MaxRc is null or blank set a default maxRC of 4
def maxRC = props.CRB_MAXRC ?: "4"


File zrbExe = new File(zrbPath)
if (!zrbExe.exists()){
    String errorMsg = "*! The zrb tool is not found "
	println(errorMsg)
	props.error = "true"
//	buildUtils.updateBuildResult(errorMsg:errorMsg)
}

List<String> buildList = argMap.buildList

println("** Processing files mapped to ${this.class.getName()}.groovy script")


def sout = new StringBuilder()
def serr = new StringBuilder()

// iterate through build list
buildList.each { buildFile ->
	println "*** Documenting changed file $buildFile in DBB Build report"

	String absolutePath = buildUtils.getAbsolutePath(buildFile)
	println absolutePath
	
	File absoluteFile = new File("$absolutePath")

	if (absoluteFile.exists()){
        // Best guess on the file type
         absoluteFile.eachLine { line ->
            line = line.trim()
            if (line.startsWith('#'))  
                skip
            else if(line.startsWith("resourceDefinitions:"))
                 resFile = absolutePath
        }
    }else {
		println "$absolutePath does not exist."
	}

    // generate the file name for the CSD formatted file
    extIndex = buildFile.lastIndexOf('.')
	outputFile = buildFile.substring(0, extIndex) + ".csd"

    //Build the shell command to execute  
    zrb_cmd = zrbPath + " build --model"
    zrb_cmd = zrb_cmd + " " + modelFile + " --application " + appFile + " --resources " + resFile + " --output " + outputFile


    //execute the command and save the console o/p in sout & serr
    def pExe = zrb_cmd.execute()
    pExe.consumeProcessOutput(sout, serr)
    pExe.waitFor()
    println sout + "  " + pExe.exitValue() 
    if (pExe.exitValue()  > maxRC){
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
    ussRecord.setAttribute("deployType", "CSD")

    // add new record to build report
    if(props.verbose) "* Adding USS_RECORD for $buildFile"
    BuildReportFactory.getBuildReport().addRecord(ussRecord)
} 

