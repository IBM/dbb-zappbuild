@groovy.transform.BaseScript com.ibm.dbb.groovy.ScriptLoader baseScript
import com.ibm.dbb.dependency.*

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
