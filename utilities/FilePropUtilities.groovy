@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.build.*
import groovy.transform.*
import com.ibm.dbb.build.report.records.*
import com.ibm.dbb.build.report.*

// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def buildUtils= loadScript(new File("BuildUtilities.groovy"))

/*
 * Loading file level properties for all files on the buildList or list which is passed to this method.
 */
def loadFileLevelPropertiesFromFile(List<String> buildList) {
     
	 if (props.verbose) println "* Populating file level properties overrides."

	 buildList.each { String buildFile ->
	     if (props.verbose) println "** Checking file property overrides for $buildFile "
	     String propertyFilePath = props.getFileProperty('propertyFilePath', buildFile)
	     String propertyExtention = props.getFileProperty('propertyFileExtension', buildFile)
	     String member = new File(buildFile).getName()
	     def filePropMap = [:]
	        
	     // check for language configuration group level overwrite
	     loadLanguageConfigurationProperties = props.getFileProperty('loadLanguageConfigurationProperties', buildFile)
	     if (loadLanguageConfigurationProperties && loadLanguageConfigurationProperties.toBoolean()) {
	         String languageConfigurationPropertyFileName = props."$member"
	         if (languageConfigurationPropertyFileName != null) {
	                    
	             // String languageConfigurationPropertyFilePath = buildUtils.getAbsolutePath(props.application) + "/${propertyFilePath}/${languageConfigurationPropertyFileName}.${propertyExtention}"                    
	             String languageConfigurationPropertyFilePath = "${props.zAppBuildDir}/build-conf/language-conf/${languageConfigurationPropertyFileName}.${propertyExtention}"

	             File languageConfigurationPropertyFile = new File(languageConfigurationPropertyFilePath)

	             if (languageConfigurationPropertyFile.exists()) {
	                 filePropMap = loadProgramTypeProperties(languageConfigurationPropertyFileName, languageConfigurationPropertyFilePath, buildFile)                            
	             } else {
	                 if (props.verbose) println "***! No language configuration properties file found for ${languageConfigurationPropertyFileName}.${propertyExtention}. Defaults or already defined file properties mapped to $buildFile."
	             }
	                   
	         } else {
	             if (props.verbose) println "***! No language configuration properties file defined for $buildFile"
	         }                    
	            
	     }
	    
	     // check for file level overwrite
	     loadFileLevelProperties = props.getFileProperty('loadFileLevelProperties', buildFile)
	     if (loadFileLevelProperties && loadFileLevelProperties.toBoolean()) {

             String propertyFile = buildUtils.getAbsolutePath(props.application) + "/${propertyFilePath}/${member}.${propertyExtention}"
             File fileLevelPropFile = new File(propertyFile)

	         if (fileLevelPropFile.exists()) {
	             if (props.verbose) println "*** $buildFile has an individual artifact properties file defined in ${propertyFilePath}/${member}.${propertyExtention}"
	             InputStream propertyFileIS = new FileInputStream(propertyFile)
	             Properties fileLevelProps = new Properties()
	             fileLevelProps.load(propertyFileIS)

	             fileLevelProps.entrySet().each { entry ->
	                 if (props.verbose) println "    Found file property ${entry.key} = ${entry.value}"
	                 filePropMap[entry.key] = entry.value 
	             }
	         } else {
	             if (props.verbose) println "***! No property file found for $buildFile. Build will take the defaults or already defined file properties."
	         }
	     }
	        
	     // Add the file patterns from file property map after checking the existence of the file patterns
	     if (props.verbose) println "*** Checking for existing file property overrides"
	     filePropMap.each { entry ->
	         // Check if the file property definition already exists
	         (filePatternIsMapped, filePatternIsMappedAtFileName, noChangeFilePattern, currValue) = checkExistingFilesPropertyDefinition(buildFile, member, entry.key)
	            
	         // If buildFile is already mapped to a value delete all the file patterns and add the backed-up file patterns
	         if (filePatternIsMapped ) {
	            if (filePatternIsMappedAtFileName) {
	                props.removeFileProperty(entry.key)
	                noChangeFilePattern.each { noChangeFile ->
	                    if (props.verbose) println "       Retaining file property override ${entry.key} = ${noChangeFile.value} for ${noChangeFile.key}"
	                    props.addFilePattern(entry.key, noChangeFile.value, noChangeFile.key)
	                 }
	                 // Add the buildFile file pattern with new value    
	                 if (props.verbose) println "    Updating file property override ${entry.key} = ${entry.value} for ${buildFile}"
	                 props.addFilePattern(entry.key, entry.value, buildFile)
	            } else {
	                println("    *! Warning: $buildFile is already mapped as a file pattern as part of a file group wildcard possibly in file.properties.")
	                println("    *! Warning: Please check the existing file property list below and fix it to contain the correct file properties.")
	            	println("    *! Existing File Property value and patterns :")
	            	noChangeFilePattern.each { noChangeFile ->
	            	    println("    *!     ${entry.key} = ${noChangeFile.value} for ${noChangeFile.key}")
	            	}
	                println("    *! Warning: Override for $buildFile could not be applied. Existing file property value will be used: ${entry.key} = ${currValue}")
	                
	                // Add file property warning messages to BuildReport.json
	                addFilePropWarningRecord(buildFile, noChangeFilePattern, entry.key, entry.value, currValue)
	            }
	         } else {
	             // Add the buildFile file pattern with new value    
	             if (props.verbose) println "    Adding file property override ${entry.key} = ${entry.value} for ${buildFile}"
	             props.addFilePattern(entry.key, entry.value, buildFile)
	         }
	     }
	 }
}

/*
 * Add the language configuration properties to the file property map
 */ 
def loadProgramTypeProperties(String languageConfigurationPropertyFileName, String languageConfigurationPropertyFile, String buildFile) {
	    
    def filePropMap = [:]
    String propertyExtention = props.getFileProperty('propertyFileExtension', buildFile)
	    
    if (props.verbose) println "*** $buildFile is mapped to ${languageConfigurationPropertyFileName}.${propertyExtention}"
	    
    InputStream languageConfigurationPropertyFileIS = new FileInputStream(languageConfigurationPropertyFile)
    Properties languageConfigProps = new Properties()
    languageConfigProps.load(languageConfigurationPropertyFileIS)
	    
    languageConfigProps.entrySet().each { entry ->
       if (props.verbose) println "    Found language configuration property ${entry.key} = ${entry.value}"
       filePropMap[entry.key] = entry.value
    }
    return filePropMap
}

/*
 * Check if file is already mapped to the build property
 * Return the file property map flag and backed-up file patterns
 */
def checkExistingFilesPropertyDefinition(String buildFile, String member, String entryKey){
	    
    if (props.verbose) println "    Checking build property ${entryKey}"
    PropertyMappings propertyMapping = new PropertyMappings(entryKey)
    def propertyMappingValues = propertyMapping.getValues()
    String expValue = ""
    String currValue = ""
    boolean filePatternIsMapped = false
    boolean filePatternIsMappedAtFileName = false
    def noChangeFilePattern = [:]
	            
    propertyMappingValues.each { value ->
        StringBuffer expandedValue = new StringBuffer()
	            
        //Expand property references
        value.split(",").each{ str ->
            if (str.indexOf('${',0) == 0) {
                str = str.substring(2, str.length())
                str = str.replaceAll("}","")
                expandedValue.append(props.getProperty(str) + ",") 
            } else {
                expandedValue.append(str + ",") 
            }
        }
        if (expandedValue.length() > 0) expandedValue.deleteCharAt(expandedValue.length()-1);
	                
        expValue = expandedValue.toString()
	                
        // If build file is already mapped to a value
        if (propertyMapping.isMapped(expValue, buildFile)) {
            if (props.verbose) println("    *! An existing file property was detected for $buildFile ") 
            if (props.verbose) println("       Existing value ${entryKey}=${value}")
            filePatternIsMapped = true
            currValue = value
        }
	            
        ArrayList filePropertyPatterns = props.getFilePropertyPatterns(entryKey,value)
            
        // Check and backup the mapped value for other file patterns 
        filePropertyPatterns.each{ filePattern ->
            if (filePattern.toLowerCase().contains(member.toLowerCase())) {
                filePatternIsMappedAtFileName = true   
            } else { 
                noChangeFilePattern[filePattern] = value
            }
        }
    }   
    return [filePatternIsMapped, filePatternIsMappedAtFileName, noChangeFilePattern, currValue]
}

/*
 * Add file property warning messages to BuildReport.json
 */
def addFilePropWarningRecord(String buildFile, Map noChangeFilePattern, String entryKey, String entryValue, String currValue){
	
	if (props.verbose) println("    *! Inserting warning message in BuildReport.json")
	
	// New Generic Property Record
	def currPropList = []
	PropertiesRecord filePropWarningInfo = new PropertiesRecord()
	

	// Add file property override warning info
	def warningMsg1 = "$buildFile is already mapped as a file pattern as part of a file group wildcard possibly in file.properties."
	def warningMsg2 = "Please check the existing file property list below and fix it to contain the correct file properties."
	def warningMsg3 = "Override for $buildFile could not be applied. Existing file property value will be used: ${entryKey} = ${currValue}."
	def warningMsg4 =  "Existing File Property value and patterns in existingFilePropList:"
	noChangeFilePattern.each { noChangeFile ->
	    currPropList.add("${entryKey} = ${noChangeFile.value} for ${noChangeFile.key}")
	}
	
	// Add warning messages to BuildReport.json
	filePropWarningInfo.addProperty("warningMsg1",warningMsg1)	
	filePropWarningInfo.addProperty("warningMsg2",warningMsg2)
	filePropWarningInfo.addProperty("warningMsg3",warningMsg3)
	filePropWarningInfo.addProperty("warningMsg4",warningMsg4)
	filePropWarningInfo.addProperty("existingFilePropList",currPropList.toString())
	filePropWarningInfo.addProperty("skippedFileProperty","${entryKey} = ${entryValue}")
	
	def warning = warningMsg3 + " Check log or build report for more details."
	
	BuildReportFactory.getBuildReport().addRecord(filePropWarningInfo)
	buildUtils.updateBuildResult(warningMsg:warning)
}
