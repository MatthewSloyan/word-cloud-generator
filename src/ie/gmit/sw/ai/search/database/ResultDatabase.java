package ie.gmit.sw.ai.search.database;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import ie.gmit.sw.ai.cloud.WordFrequency;

/**
* This is the implementation of the real results database.
* This class is also composed in the ResultsDatabaseProxy, so that the client or other classes will not be able to access it directly.
* Databaseable is implemented which could be used with other types of databases.
* 
* I initially decided there should only be one instance of this to provide easy access to multiple classes. However from testing
* this would only allow one request to work at one time otherwise the results would merge into one. This isn't ideal for a multi-user
* web application so I decided to use a new instance for each request and pass the same instance into the classes required. To keep the coupling
* loose I used to a proxy design pattern, and only composed the proxy where it is needed. 
* 
* I wanted to uphold the SRP with this class, so all database functionality such as indexing words, checking against the ignore words file
* and sorting the final map are handled here rather than other classes.
* 
* I have used a ConcurrentHashMap for the local db as it's the fastest type (search, insert and delete functions are O(1)).
* Also it allows for concurrency between multiple threads as an Thread Pool is used to process links.
* 
* @see Databaseable
* @see ResultsDatabaseProxy
* @author Matthew Sloyan
*/
public class ResultDatabase implements Databaseable {

	private Map<String, Integer> wordMap = new ConcurrentHashMap<>();
	private static Set<String> ignoreWords;
	
	/**
	* Constructor method. When initialised load ignore words file into HashSet.
	* 
	* @see IgnoreWords
	*/
	public ResultDatabase() {
		super();
		ignoreWords = new IgnoreWords().loadIgnoreWords();
	}

	/**
	* Check the current size of the hash map, this is used as one of the goal conditions in searches.
	*/
	public int checkSize() {
		return wordMap.size();
	}

	/**
	* Method to index all strings found in a webpage if it was found that it was relevant.
	* Meta, title, headings and body strings are passed in and added to the hashmap word by word.
	* Handles any number of params using variable arguments.
	* 
	* A number of checks are done on each word which is described below. A lot of testing went into this to ensure better results.
	* Running time: O(N^2) however first loop runs four times in current build.
	* 
	* @param term search term to compare against.
	* @param text meta, title, headings and body strings to add to map.
	*/
	public void index(List<String> searchTerms, String ...text) {
		//pattern used for additional speed on split
		Pattern pattern = Pattern.compile(" ");
		
		// Loop through all inputs (headers, title, body, meta etc)
		for (String s : text) {
			String[] words = pattern.split(s.replaceAll("[^A-Za-z ]", "").toLowerCase().trim());
			
			// Loop through all split words from title, headings body.
			for (String word : words) {
				
				// Checks for empty words, if the word is not equal to the search term,
				// if word is in ignore words set, and if the word is longer than 2 characters. 
				// If so ignore word.
				if (!word.isEmpty() && !searchTerms.contains(word) && !ignoreWords.contains(word) && word.length() >= 2) {
					addToResults(word);
				}
			}
		}
	}

	/**
	* Method add each valid word from index to HashMap.
	* Running time: O(1) as HashMap's search, insert and delete functions are O(1)
	* 
	* @param word to add to map.
	*/
	private void addToResults(String word) {
		// Check if the word is in the map already, if so increase frequency by one.
		// Else create a new key/value and set frequency to 1.
		if (wordMap.containsKey(word)) {
			wordMap.put(word, wordMap.get(word) + 1);
		} else {
			wordMap.put(word, 1);
		}
	}
	
	/**
	* Method which gets sorted map as a list and returns array of the top N words.
	* Running time: O(N).
	* 
	* @param limit number of words in final word cloud.
	* @return array of the top words.
	* @see WordFrequency
	*/
	public WordFrequency[] getResults(int limit) {
		WordFrequency[] wf = new WordFrequency[limit];
		
		System.out.println("TOP " + limit + " RESULTS\n");

		// Get sorted map by value.
		List<Map.Entry<String, Integer>> list = sortMapByValue();

		// Add to array until limit is reached.
		for (int i = 0; i < limit; i++) {
			wf[i] = new WordFrequency(list.get(i).getKey(), list.get(i).getValue());
			System.out.println(wf[i]);
		}

		return wf;
	}

	/**
	* Sort map by value in descending order, and return as a list to get number of words to display.
	* The top N number of words are displayed in a word cloud to the user.
	* A comparator is used to sort by value, which compares value 1 and 2.
	* Code for comparator adapted from: https://www.geeksforgeeks.org/sorting-a-hashmap-according-to-values/
	* Running time: O(n log(n)) because of sort.
	* 
	* @return list of map entries sorted by value in descending order.
	*/
	private List<Map.Entry<String, Integer>> sortMapByValue() {
		// Create a list from elements of HashMap
		List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(wordMap.entrySet());

		// Sort the list in decending order.
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> word1, Map.Entry<String, Integer> word2) {
				return word2.getValue().compareTo(word1.getValue());
			}
		});
		
		return list;
	}
}
