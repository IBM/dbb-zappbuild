@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.dependency.*
import com.ibm.dbb.repository.*
import com.ibm.dbb.build.*
import groovy.transform.*

@Field BuildProperties props = BuildProperties.getInstance()

// Externalized Method to preserve backward compatibility with older DBB toolkit versions.
// TODO: Refactoring as soon as the deprecated API is dropped.

/**
 * Method to create the logical file using SearchPathDependencyResolver
 * 
 *  evaluates if it should resolve file flags for resolved dependencies
 * 
 * @param spDependencyResolver
 * @param buildFile
 * @return
 */

def createLogicalFile(SearchPathDependencyResolver spDependencyResolver, String buildFile) {
	
	LogicalFile logicalFile
	
	if (props.resolveSubsystems && props.resolveSubsystems.toBoolean()) // include resolved dependencies to define file flags of logicalFile
		logicalFile = spDependencyResolver.resolveSubsystems(buildFile,props.workspace)
	else
		logicalFile = SearchPathDependencyResolver.getLogicalFile(buildFile,props.workspace)

	return logicalFile

}

/**
 * 
 * @param dependencySearch
 * @return SearchPathDependencyResolver
 */
def createSearchPathDependencyResolver(String dependencySearch) {
	return new SearchPathDependencyResolver(dependencySearch)
}

def findImpactedFiles(String impactSearch, String changedFile, RepositoryClient repositoryClient) {
	
	List<String> collections = new ArrayList<String>()
	collections.add(props.applicationCollectionName)
	collections.add(props.applicationOutputsCollectionName)
	
	if (props.verbose)
		println ("*** Creating SearchPathImpactFinder with collections " + collections + " and impactSearch configuration " + impactSearch)
	
	def finder = new SearchPathImpactFinder(impactSearch, collections, repositoryClient)
	
	// Find all files impacted by the changed file
	impacts = finder.findImpactedFiles(changedFile, props.workspace)
	return impacts
}

def resolveDependencies(SearchPathDependencyResolver dependencyResolver, String buildFile) {
	if (props.verbose) {
		println "*** Resolution rules for $buildFile:"
		println dependencyResolver.getSearchPath()
	}
	return dependencyResolver.resolveDependencies(buildFile, props.workspace)
}