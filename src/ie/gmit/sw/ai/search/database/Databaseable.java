package ie.gmit.sw.ai.search.database;

import java.util.List;

import ie.gmit.sw.ai.cloud.WordFrequency;

/**
* Interface designed to abstract common functionality from ResultsDatabase class, 
* and allow it to work with the ResultsDatabaseProxy.
* Also it could allow multiple different types of databases to be created if needed.
* 
* @see ResultsDatabase
* @see ResultsDatabaseProxy
* @author Matthew Sloyan
*/
public interface Databaseable {
	
	public void index(List<String> searchTerms, String ...text);
	public WordFrequency[] getResults(int limit);
	public int checkSize();
}
