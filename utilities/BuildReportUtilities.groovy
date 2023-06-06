@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*
import com.ibm.dbb.build.report.*
import com.ibm.dbb.build.report.records.*
import com.ibm.jzos.ZFile

// define script properties
@Field BuildProperties props = BuildProperties.getInstance()
@Field def buildUtils= loadScript(new File("BuildUtilities.groovy"))

/*
 * Method to iterate over deleted files list to generate Delete Records in BuildReport
 * using the AnyTypeRecord which got introduced in DBB 1.1.3 .
 * 
 *  zAppBuild is capable to document deletions based on the calculated deletedFileList.
 *  
 *  While the logicalFile no longer exists, it has to validate if a potential output exists.
 *  This is based on the languagePrefix, which is used to obtain the property
 *  <langprefix>_outputDatasets, which contains a comma-separated list of libraries
 *  containing build outputs. Supports file overwrites
 *  
 *  A decision was taken not to validate if the file exists on the dataset before 
 *  capturing the record, while on featureBranches build libraries, the outputs most likely
 *  don't exist.  
 * 
 */
def processDeletedFilesList(List deletedList){

	deletedList.each { deletedFile ->
		def scriptMapping = ScriptMappings.getScriptName(deletedFile)
		if(scriptMapping != null){
			langPrefix = buildUtils.getLangPrefix(scriptMapping)
			if(langPrefix != null){

				String member = CopyToPDS.createMemberName(deletedFile)

				String isLinkEdited = props.getFileProperty("${langPrefix}_linkEdit", deletedFile)
				if ((isLinkEdited && isLinkEdited.toBoolean()) || scriptMapping == "LinkEdit.groovy" || isLinkEdited == null){

					if (props.verbose) println "** Create deletion record for file ${deletedFile}"
					AnyTypeRecord deleteRecord = new AnyTypeRecord("DELETE_RECORD")
					deleteRecord.setAttribute("file", deletedFile)

					List<String> deletedOutputsList = new ArrayList<String>() 

					String outputLibs
					// obtain output libraries
					if (langPrefix == "transfer") {
						// obtain the mapped dataset of the target dataset		
						PropertyMappings dsMapping = new PropertyMappings("transfer_datasetMapping")
						def mappedDatesetDef = dsMapping.getValue(deletedFile)
						outputLibs = props.getProperty(mappedDatesetDef)
					} else {
						// the defaul evaluates {langPrefix}_outputDatasets
						outputLibs = props.getFileProperty("${langPrefix}_outputDatasets", deletedFile)
					}
					
					if (outputLibs != null) {
						outputLibs.split(',').each{ outputDS ->
							// record for deleted dataset(member)
							String outputRecord = "$outputDS"+"($member)"
							if (props.verbose) println "** Document deletion ${outputRecord} for file ${deletedFile}"
							deletedOutputsList.add(outputRecord)

							// delete outputRecord from build datasets
							if (ZFile.dsExists("//'$outputRecord'")) {
								if (props.verbose) println "** Deleting ${outputRecord}"
								ZFile.remove("//'$outputRecord'")
							}

						}
					} else {
						println "** No output library found for $deletedFile"
					}
						

					if(deletedOutputsList.size() > 0 ) { 
						deleteRecord.setAttribute("deletedBuildOutputs",deletedOutputsList)
						BuildReportFactory.getBuildReport().addRecord(deleteRecord)
					}

				}
				else {
					if (props.verbose) println ("*** Skipped $deletedFile.")
				}
			} else {
				if (props.verbose) println ("*** No Delete Record generated for $deletedFile. No language prefix found.")
			}
		}
	}
}