@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import groovy.transform.*
import com.ibm.dbb.*
import com.ibm.dbb.build.*
import com.ibm.jzos.ZFile

@Field BuildProperties props = BuildProperties.getInstance()
println "\n** Executing test script fullBuild.groovy"

// Get the DBB_HOME location
def dbbHome = EnvVars.getHome()
if (props.verbose) println "** DBB_HOME = ${dbbHome}"

// Create reset build command
def resetBuildCommand = [] 
resetBuildCommand << "${dbbHome}/bin/groovyz"
resetBuildCommand << "${props.zAppBuildDir}/build.groovy"
resetBuildCommand << "--workspace ${props.workspace}"
resetBuildCommand << "--application ${props.app}"
resetBuildCommand << "--outDir ${props.zAppBuildDir}/out"
resetBuildCommand << "--hlq ${props.hlq}"
resetBuildCommand << "--logEncoding UTF-8"
resetBuildCommand << "--url ${props.url}"
resetBuildCommand << "--id ${props.id}"
resetBuildCommand << (props.pw ? "--pw ${props.pw}" : "--pwFile ${props.pwFile}")
resetBuildCommand << (props.verbose ? "--verbose" : "")
resetBuildCommand << "--reset"

// Run reset build 
println "** Executing ${resetBuildCommand.join(" ")}"
def process = ['bash', '-c', resetBuildCommand.join(" ")].execute()
def outputStream = new StringBuffer();
process.waitForProcessOutput(outputStream, System.err)

// Validate reset build
println "** Validating reset build"

// Validate clean reset build
assert outputStream.contains("Build State : CLEAN") : "*! RESET OF THE BUILD FAILED\nOUTPUT STREAM:\n$outputStream\n"
  
println "**"
println "** RESET OF THE BUILD : PASSED **"
println "**"
