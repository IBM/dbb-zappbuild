@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import groovy.transform.*
import com.ibm.dbb.*
import com.ibm.dbb.build.*

@Field BuildProperties props = BuildProperties.getInstance()
@Field def assertionList = []

println "\n**************************************************************"
println "** Executing test script ${this.class.getName()}.groovy"
println "**************************************************************"

// Get the DBB_HOME location
def dbbHome = EnvVars.getHome()
if (props.verbose) println "** DBB_HOME = ${dbbHome}"

// Create reset build command
def resetBuildCommand = [] 
resetBuildCommand << "${dbbHome}/bin/groovyz"
resetBuildCommand << "${props.zAppBuildDir}/build.groovy"
resetBuildCommand << "--workspace ${props.workspace}"
resetBuildCommand << "--application ${props.app}"
resetBuildCommand << (props.outDir ? "--outDir ${props.outDir}" : "--outDir ${props.zAppBuildDir}/out")
resetBuildCommand << "--hlq ${props.hlq}"
resetBuildCommand << "--logEncoding UTF-8"
resetBuildCommand << (props.url ? "--url ${props.url}" : "")
resetBuildCommand << (props.id ? "--id ${props.id}" : "")
resetBuildCommand << (props.pw ? "--pw ${props.pw}" : "") 
resetBuildCommand << (props.pwFile ? "--pwFile ${props.pwFile}" : "")
resetBuildCommand << (props.verbose ? "--verbose" : "")
resetBuildCommand << (props.propFiles ? "--propFiles ${props.propFiles}" : "")
resetBuildCommand << "--reset"

// Run reset build 
println "** Executing ${resetBuildCommand.join(" ")}"
def process = ['bash', '-c', resetBuildCommand.join(" ")].execute()
def outputStream = new StringBuffer();
process.waitForProcessOutput(outputStream, System.err)


try {
	// Validate reset build
	println "** Validating reset build"

	// Validate clean reset build
	assert (outputStream.contains("Build finished")) && (process.exitValue() == 0) : "*! RESET OF THE BUILD FAILED\nOUTPUT STREAM:\n$outputStream\n"

	println "**"
	println "** RESET OF THE BUILD : PASSED **"
	println "**"

}catch(AssertionError e) {
	def result = e.getMessage()
	assertionList << result;
	props.testsSucceeded = 'false'
}
