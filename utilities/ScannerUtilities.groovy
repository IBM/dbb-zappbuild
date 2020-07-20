@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.repository.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*
import groovy.json.JsonSlurper
import com.ibm.dbb.scanner.zUnit.*


// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
//@Field def buildUtils= loadScript(new File("BuildUtilities.groovy"))


/*
 * getScanner - get the appropriate Scanner for a given file type (Defaults to DependencyScanner)
 */
def getzUnitScanner() {
	return new ZUnitConfigScanner()
}
