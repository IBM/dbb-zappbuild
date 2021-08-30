@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.dependency.*
import com.ibm.dbb.build.*
import groovy.transform.*
import com.ibm.dbb.build.report.*
import com.ibm.dbb.build.report.records.*
import com.ibm.dbb.extensions.*
import com.ibm.jzos.ZFile

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

				props."${langPrefix}_outputDatasets".split(',').each{ outputDS ->

					// outputRecord
					String outputRecord = "$outputDS"+"($member)"

					String isLinkEdited = props.getFileProperty("${langPrefix}_linkEdit", deletedFile)
					if ((isLinkEdited && isLinkEdited.toBoolean()) || scriptMapping == "LinkEdit.groovy" || isLinkEdited == null){

						DeleteRecord deleteRecord = new DeleteRecord()
						deleteRecord.setFile(deletedFile)
						deleteRecord.addOutput(outputRecord)
						BuildReportFactory.getBuildReport().addRecord(deleteRecord)

						if (ZFile.dsExists("//'$outputRecord'")) {
						   if (props.verbose) println "** Deleting ${outputRecord}"
						   ZFile.remove("//'$outputRecord'")
						}
						
					}
					else {
						if (props.verbose) println ("*** Skipped $deletedFile.")
					}
				}
			} else {
				if (props.verbose) println ("*** No Delete Record generated for $deletedFile. No language prefix found.")
			}
		}
	}
}