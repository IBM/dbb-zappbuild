@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.metadata.*
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*
import groovy.xml.MarkupBuilder
import groovy.json.JsonParserType
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

// define script properties
@Field BuildProperties props = BuildProperties.getInstance()

/**
 * DependencyScannerUtilties
 * 
 *  a collection of helper methods to
 *  
 *  * retrieve the scanner for a build file -getScanner()
 *  * populate the scanner registry from the configuration in 
 *  		build-conf or application-conf
 * 
 */

/*
 * getScanner()
 *
 *  returns the mapped scanner or null if build file is not mapped
 *
 */
def getScanner(String buildFile){

	def scanner = null

	// check scannerMapping
	scanner = DependencyScannerRegistry.getScanner(buildFile)

	if (scanner){
		if (scanner instanceof com.ibm.dbb.dependency.DependencyScanner) {
			// Workaround - if no language hint exists the registry returned the default
			//   Scanner, and the file is not mapped
			if (((DependencyScanner) scanner).getLanguageHint() == null) {
				if (props.verbose) println("*** $buildFile is not mapped to a DBB Dependency scanner.")
				scanner = null
			}
		}
	}
	else {
		if (props.verbose) println("*** No scanner specified for $buildFile")
	}

	return scanner
}

/*
 *  populate DependencyScannerRegistry()
 *  
 *   this method is populating the DBB scanner registry based on the dbb.scannerMapping property
 *   
 *   also see application-conf/file.properties
 *  
 */

def populateDependencyScannerRegistry() {

	println("** Loading DBB scanner mapping configuration dbb.scannerMapping")

	// loading scannerMappings
	PropertyMappings scannerMapping = new PropertyMappings("dbb.scannerMapping")

	if (scannerMapping) {
		// get all values
		scannerMapping.getValues().each{ scannerConfigJson ->

			Map scannerConfig = parseJSONStringToMap(scannerConfigJson)
			scannerClass = scannerConfig["scannerClass"]
			languageHint = scannerConfig["languageHint"]

			// get file patterns / extensions
			fileExtensionsPatterns = props.getFilePropertyPatterns("dbb.scannerMapping", scannerConfigJson)
			if (fileExtensionsPatterns) {
				fileExtensionsPatterns.each{ fileExt ->

					// define scanner
					def scanner

					// evaluate configuration
					if (scannerClass == "DependencyScanner") {
						scanner = new DependencyScanner()
						scanner.setLanguageHint(languageHint)
					} else if (scannerClass == "ZUnitConfigScanner") {
						scanner = new ZUnitConfigScanner()
					} else {
						errorMsg = "*! DependencyScannerUtilities.populateDependencyScannerRegistry() - Specified Dependency Scanner class $scannerClass does not exist. Process exiting."
						println errorMsg
						System.exit(3)
					}

					// adding scanner mapping
					if (props.verbose) println("*** Adding scanner mapping for file extension $fileExt : (languageHint: $languageHint, Scanner Class: $scannerClass)")
					DependencyScannerRegistry.addScanner(fileExt, scanner)
				}
			}
			else {
				println("**! Warning - No Patterns found for $scannerConfigJson for build property mapping dbb.scannerMapping.")
			}
		}
	}
	else {
		println("**! Warning - Build configuration is not specifying the scanner mapping configuration - dbb.scannerMapping . Using default map of file extensions to IDependencyScanner instances. See DBB toolkit Javadoc.")
	}
}
/*
 *  This is a helper method which parses a JSON String representing a map of key value pairs to a proper map
 */
def parseJSONStringToMap(String mappingString) {
	Map map = [:]
	try {
		JsonSlurper slurper = new groovy.json.JsonSlurper()
		map = slurper.parseText(mappingString)
	} catch (Exception e) {
		errorMsg = "*! DependencyScannerUtilities.parseJSONStringToMap() - Converting String $mappingString a Map object failed. Process exiting."
		println errorMsg
		println e.getMessage()
		System.exit(3)
	}
	return map
}