package ie.gmit.sw.ai.search.database;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import ie.gmit.sw.ai.search.models.Files;

/**
* Loads all words from the ignorewords.txt file into a HashSet. This is used by the ResultsDatabase 
* to compare word against set.
* I have used a HashSet for the words for speed (search, insert and delete functions are O(1)).
* 
* @see ResultsDatabase
* @author Matthew Sloyan
*/
public class IgnoreWords {
	
	public Set<String> loadIgnoreWords() {
		BufferedReader br;
		Set<String> ignoreWords = new HashSet<String>();
		
		try {
			// Load in ignore words file (index 0).
			br = new BufferedReader(new FileReader(Files.getInstance().getFileNames().get(0)));
			
	        String line = "";
	        while ((line = br.readLine()) != null) {
	        	// Add each line to the hashset.
	        	ignoreWords.add(line);
	        }
	        br.close();
		} catch (IOException e) {
			System.out.println("File not found.");
		}
		
		return ignoreWords;
	}
}
