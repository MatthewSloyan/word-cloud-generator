package ie.gmit.sw.ai.search.database;

import java.util.List;

import ie.gmit.sw.ai.cloud.WordFrequency;

/**
* I have decided to implement a proxy to access the results database.
* This allows the developer to control the database service without the client knowing as the proxy is implemented as a layer of 
* encapsulation in front of it. 
* The proxy works even if database isn’t ready or is not available.
* Open/Close Principle (OCP). You can introduce new proxies without changing the real database or client.
* You can also make changes to the database or add new databases without affecting the proxy or the client.
* This was also useful as I decided to remove the Singleton design pattern from the ResultsDatabase, 
* as it was stopping multiple requests at the same time as all instances of the web app were sharing the same db. 
* 
* To achieve this I have composed the real database as an instance variable, 
* and then delegate the calls to the real database which will stores the words found in a concurrent hash map.
* When the constructor is called, it gets the only instance of the Database so data is consistent.
* 
* @see Databaseable
* @see ResultsDatabase
* @author Matthew Sloyan
*/
public class ResultsDatabaseProxy implements Databaseable {
	
	private ResultDatabase db;
	
	public ResultsDatabaseProxy() {
		super();
		this.db = new ResultDatabase();
	}

	@Override
	public void index(List<String> searchTerms, String... text) {
		db.index(searchTerms, text);
	}

	@Override
	public WordFrequency[] getResults(int limit) {
		return db.getResults(limit);
	}

	@Override
	public int checkSize() {
		return db.checkSize();
	}
}
