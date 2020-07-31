package ie.gmit.sw.ai.search.searches;

import org.jsoup.nodes.Document;

/**
* Interface used for all search algorithms.
*/
public interface Searchable {
	
	// Final variables used in searches.
	static final int MAX = 80;
	static final int MAX_WORDS = 5500;
	static final int BRANCING_FACTOR = 8;
	static final int BEAM_WIDTH = 3;
	
	public void run();
	
	/**
	* Method to process any node on queue.
	* Would ideally have these as private but using Java 8.
	*/
	default public void process() {
	}
	
	/**
	* Only required in RecursiveDepthFirstSearch
	* @see RecursiveDepthFirstSearch
	*/
	default public void search(Document doc) {
	}
	
	/**
	* Method consistent across all searches. 
	* Default method to cut down repeated code (DRY), which only passes in the 
	* required integer values to cut down on memory usage.
	
	* Checks if the search has met it's goal condition which is specified by the user.
	* E.g if max words is reached or if max nodes is reached.
	* 
	* @see BestFirstSearch
	* @see BeamSearch
	* @see RecursiveDepthFirstSearch
	*/
	default boolean checkForGoal(int options, int dbSize, int closedSize) {
		if (options == 1) {
			if (dbSize <= MAX_WORDS) {
				return true;
			}
		}
		else if (options == 2) {
			if (closedSize <= MAX) {
				return true;
			}
		}
		return false;
	}
}
