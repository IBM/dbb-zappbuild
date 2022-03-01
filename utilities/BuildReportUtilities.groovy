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
 * Method to iterate over deleted files list to generate Delete Records in BuildReport.
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

					AnyTypeRecord deleteRecord = new AnyTypeRecord("DELETE_RECORD")
					deleteRecord.setAttribute("file", deletedFile)

					Set<String> deletedOutputsList = new HashSet<String>() 
					
					props."${langPrefix}_outputDatasets".split(',').each{ outputDS ->
						// record for deleted dataset(member)
						String outputRecord = "$outputDS"+"($member)"
						deletedOutputsList.add(outputRecord)

						// delete outputRecord from build datasets
						if (ZFile.dsExists("//'$outputRecord'")) {
							if (props.verbose) println "** Deleting ${outputRecord}"
							ZFile.remove("//'$outputRecord'")
						}

					}

					if(deletedOutputsList.size() > 0 ) 
						BuildReportFactory.getBuildReport().addRecord(deleteRecord)

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