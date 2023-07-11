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
		scannerMapping.getValues().each{ scannerConfig ->

			Map scannerConfigMap = parseConfigStringToMap(scannerConfig)
			if (scannerConfigMap) {
				scannerClass = scannerConfigMap.scannerClass
				languageHint = scannerConfigMap.languageHint ? scannerConfigMap.languageHint : "none"

				// get file patterns / extensions
				fileExtensionsPatterns = props.getFilePropertyPatterns("dbb.scannerMapping", scannerConfig)
				if (fileExtensionsPatterns) {
					fileExtensionsPatterns.each{ fileExt ->

						// define scanner
						def scanner

						// evaluate configuration
						if (scannerClass == "DependencyScanner") {
							scanner = new DependencyScanner()
							if (scannerConfigMap.languageHint) scanner.setLanguageHint(languageHint)
						} else if (scannerClass == "ZUnitConfigScanner") {
							scanner = new ZUnitConfigScanner()
						} else {
							errorMsg = "*! DependencyScannerUtilities.populateDependencyScannerRegistry() - Specified Dependency Scanner class $scannerClass does not exist. Process exiting."
							println errorMsg
							System.exit(3)
						}

						// adding scanner mapping
						// if (props.verbose) println("*** Adding scanner mapping for file extension $fileExt : (languageHint: $languageHint, Scanner Class: $scannerClass)")
						DependencyScannerRegistry.addScanner(fileExt, scanner)
					}
				}
				else {
					println("**! Warning - No Patterns found for $scannerConfig for build property mapping dbb.scannerMapping.")
				}
			}
			else {
				println("**! The scanner configuration $scannerConfig could not successfully be parsed and is skipped.")
			} 
		}
	}
	else {
		println("**! Warning - Build configuration is not specifying the scanner mapping configuration - dbb.scannerMapping . Using default map of file extensions to IDependencyScanner instances. See DBB toolkit Javadoc.")
	}
}

/*
 * Helper Method for populateDependencyScannerRegistry() to read through the string
 * 
 * Decision was to not use JSON for this.
 * 
 * Tests:
 * 
 *    "languageHint:COB,scannerClass:DependencyScanner"
 *    "languageHint:COB, scannerClass:DependencyScanner"
 *    "languageHint : COB, scannerClass : DependencyScanner"
 *    "languageHint : COB, scannerClass : DependencyScanner"
 *    "scannerClass : DependencyScanner  ,languageHint : COB"
 *    "'scannerClass' : 'DependencyScanner'  ,'languageHint' : 'COB'"
 *    '"scannerClass" : "DependencyScanner"  ,"languageHint" : "COB"'
 *    '"scannerClass" : "DependencyScanner"  ,"languageHint" : "COB"'
 *    '"scannerClass" : "DependencyScanner"'
 * 
 */
def parseConfigStringToMap(String configString) {
	// map
	Map<String,String> scannerConfigMap = new HashMap<String,String>()

	// string parsing
	configString.replaceAll("'","").replaceAll('"','').split(',').each(){ entry ->
		def pair = entry.split(':')
		if(pair.size() == 2) {
			scannerConfigMap.put(pair[0].trim(),pair[1].trim())
		}
	}
	
	if (scannerConfigMap.scannerClass == null) {
		println "*! The provided scanner mapping configuration ($configString) is not formed correctly and skipped."
		println "*! Sample syntax: 'dbb.scannerMapping = \"scannerClass\":\"DependencyScanner\", \"languageHint\":\"COB\" :: cbl,cpy,cob'"
		return null
	}
	
	return scannerConfigMap
}