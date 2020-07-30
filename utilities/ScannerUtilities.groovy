@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.repository.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*
import groovy.json.JsonSlurper
import com.ibm.dbb.scanner.zUnit.*


// define script properties
@Field BuildProperties props = BuildProperties.getInstance()

/*
 * getScanner - get the appropriate Scanner for a given file type (Defaults to DependencyScanner)
 */
def getScanner(String buildFile) {
	def mapping = new PropertyMappings("dbb.scannerMapping")
	if (mapping.isMapped("ZUnitConfigScanner", buildFile)) {
		if (props.verbose) println("*** Scanning file with the ZUnitConfigScanner")
		scanner = new ZUnitConfigScanner()
	}
	else {
		if (props.verbose) println("*** Scanning file with the default scanner")
		scanner = new DependencyScanner()
	}
	return scanner
}

