package ie.gmit.sw.ai.search;

import ie.gmit.sw.ai.cloud.WordFrequency;

/**
* Interface used for parsing nodes.
* More could be added easily.
*/
public interface Parseable {

	public void parse();
	public WordFrequency[] getResults();
}
