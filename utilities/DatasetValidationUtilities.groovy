@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.build.*
import groovy.transform.*
import groovy.cli.commons.*
import java.util.Properties
import com.ibm.jzos.ZFile

// define script properties

@Field BuildProperties props = BuildProperties.getInstance()

validateDatasets(args)

def validateSystemDatasets(String propertyFiles, String verbose) {

	propertyFiles.split(",").each { propertyFile ->

		// load property file using java.util.Properties
		def properties = new Properties()
		
		// convert to absolute path based on zAppBuild conventions and structure
		if (props && !propertyFile.startsWith('/')) propertyFile = "${props.zAppBuildDir}/build-conf/$propertyFile"
		
		File propFile = new File(propertyFile)
		if (propFile.exists()) {
			
			propFile.withInputStream { properties.load(it) }
			properties.each { key,dataset ->
				if (dataset) {
					if (ZFile.dsExists("'$dataset'")) {
						if (verbose.toBoolean()) println "** The dataset $dataset referenced for property $key was found."
					} else  {
						println "*! The dataset $dataset referenced for property $key was not found. Process exits."
						System.exit(1)
					}
				} else {
					if (verbose.toBoolean()) println "*! No dataset defined for property $key specified in $propertyFile."
				}
			}
		} else {
			println "*!* The specified $propertyFile (in the list [$propertyFiles]) does not exist."
		}
	}
}

def validateDatasets(String[] cliArgs)
{
	def cli = new CliBuilder(usage: "DatasetValidationUtilites.groovy [options]", header: '', stopAtNonOption: false)
	cli.d(longOpt:'systemDatasetDefinition', args:1, required:true, 'List of property files containing system dataset definitions.')
	cli.h(longOpt:'help', 'Flag to print the Help message.')
	
	def opts = cli.parse(cliArgs)

	// if opt parse fail exit.
	if (! opts) {
		System.exit(1)
	}

	if (opts.help)
	{
		cli.usage()
		System.exit(0)
	}
	
	validateSystemDatasets(opts.d, "true")
}