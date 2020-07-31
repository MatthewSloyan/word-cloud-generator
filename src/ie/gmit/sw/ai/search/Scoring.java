package ie.gmit.sw.ai.search;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import ie.gmit.sw.ai.search.heuristics.CustomAIHeuristic;
import ie.gmit.sw.ai.search.heuristics.EncogHeuristic;
import ie.gmit.sw.ai.search.heuristics.FuzzyHeuristic;
import ie.gmit.sw.ai.search.models.Options;
import ie.gmit.sw.ai.search.models.WebPage;

/**
* Scores a document using a AI heuristic search method (Fuzzy, Encog or Custom NN).
* Scoring is determined on four types (Meta tags, title, headings and the body.)
* More information on these can be found below.
* 
* @author Matthew Sloyan
*/
public class Scoring {
	// Static weights, use once across all instances.
	private static final int META_WEIGHT = 100;
	private static final int TITLE_WEIGHT = 50;
	private static final int HEADING_WEIGHT = 10;
	private static final int BODY_WEIGHT = 5;

	// Instance variables
	private Document doc;
	private String term;
	private Options options;

	// Constructor
	public Scoring(Document doc, String term, Options options) {
		super();
		this.doc = doc;
		this.term = term;
		this.options = options;
	}

	/**
	* Gets a score from 0-2 for a document. 
	* First try get the meta tags and get the frequency using the search term.
	* The frequency can use two methods, which are determined by the user options (Frequency of word, or Levenshtein distance.)
	* 
	* Then get the title and check for frequency.
	* Then get all headings (h1, h2 and h3) and check for frequency.
	* Then get the body and check for frequency.
	* 
	* Using the frequency for each, pass into either Fuzzy, Encog or Custom NN and return the predicted score as WebPage object.
	* I decided to use a WebPage object to return to each webpage
	* as it was breaking the SRP indexing the data in the Scoring class. Scoring now just scores and returns
	* data to the search to be passed to the db if score is high enough. 
	* 
	* I decided to implement an additional feature if the score is medium (1) just to use the words close to the search term in the body, as
	* if the result is medium it might not be the most relevant page. If the score is high however all words are indexed as
	* it is very relevant.
	* 
	* @see FuzzyHeuristic
	* @see EncogHeuristic
	* @return WebPage
	*/
	public WebPage getHeuristicScore() {
		String metas = "";
		String title = "";
		String body = "";
		StringBuilder headerSb = new StringBuilder();
		int metaScore = 0;
		int titleScore = 0;
		int headingScore = 0;
		int bodyScore = 0;
		int score = 0;

		// == Meta Tags ==
		try {
			// https://www.javatpoint.com/jsoup-example-print-meta-data-of-an-url
			String keywords = doc.select("meta[name=keywords]").attr("content");
			metaScore += getFrequency(keywords) * META_WEIGHT;
			
			String description = doc.select("meta[name=description]").attr("content");
			metaScore += getFrequency(description) * META_WEIGHT;

			metas = description + keywords;
		} catch (Exception e1) {

		}

		// == Title ==
		title = doc.title();
		titleScore = getFrequency(title) * TITLE_WEIGHT;

		// == Headings == 
		Elements headings = doc.select("h1, h2, h3");
		for (Element heading : headings) {
			String h = heading.text();
			headingScore += getFrequency(h) * HEADING_WEIGHT;
			headerSb.append(" " + h);
		}

		// == Body ==
		try {
			body = doc.body().text(); // Check for null
			bodyScore = getFrequency(body) * BODY_WEIGHT;
			
		} catch (Exception e) {
			body = "";
		}
		
		//System.out.println("{" + metaScore + ", " + titleScore + ", " + metaScore + ", " + bodyScore + "},");

		// == Get Heuristic Score ==
		if (options.getHeuristic() == 1) {
			score = new FuzzyHeuristic().getHeuristicScore(metaScore, titleScore, headingScore, bodyScore);
		} 
		else if (options.getHeuristic() == 2) {
			score = new EncogHeuristic().getHeuristicScore(metaScore, titleScore, headingScore, bodyScore);
		} 
		else if (options.getHeuristic() == 3) {
			score = new CustomAIHeuristic().getHeuristicScore(metaScore, titleScore, headingScore, bodyScore);
		}
		
		// Only add certain words if score is medium.
		// Update body with just close words to search term.
		if (score == 1) {
			body = getCloseWords(body);
		}

		// Return webpage details to 
		return new WebPage(metas, title, headerSb.toString(), body, score);
	}

	/**
	* Gets a score for a given string based on the search term.
	* The frequency can use two methods, which are determined by the user options (Frequency of word, or Levenshtein distance.)
	* 
	* If option 1 is selected, the score is based on how many number of times the word occurs in the string (Faster)
	* If option 2 is selected, the score is based on if the Levenshtein distance between a word and the search term is <= 3. 
	* This will give similar words to the search term.
	* 
	* @return frequency
	*/
	private int getFrequency(String s) {
		// Check for the frequency of "term" in s
		int frequency = 0;
		for (String word : s.split(" ")) {
			// Frequency
			if (options.getScoring() == 1) {
				if (term.equalsIgnoreCase(word)) {
					frequency++;
				}
			}
			// Levenshtein
			if (options.getScoring() == 2){
				if (calculateLevenshtein(word, term) <= 3) {
					frequency++;
				}
			}
		}
		
		return frequency;
	}

	/**
	* Gets the words close to the found search term, with a distance of 3. So if the search term was "Java" and
	* the sentence was "Code can run on all platforms that support Java without the need for recompilation."
	* it would return "platforms that support Java without the need".
	*
	* This is used for the body if the score is medium (1) as if the result is medium it might not be the most relevant page. 
	* 
	* @return string of close words.
	*/
	private String getCloseWords(String s) {
		StringBuilder words = new StringBuilder();
		int wordDistance = 3;
		
		String[] strings = s.split(" ");
		
		for (int i = 0; i < strings.length; i++) { 
			
		    if (term.equalsIgnoreCase(strings[i])) {
		    	try {
		    		// Add the three words to the left and right of found word.
					for (int j = i - wordDistance; j < i + wordDistance; j++) {
						words.append(strings[j]);
					}
				} catch (Exception e) {
				}
		    }
		}

		return words.toString();
	}

	/**
	* Calculate the Levenshtein distance between two words. 
	* The Levenshtein distance between "kitten" and "sitting" is 3, as only three changes are needed.
	* Code Adapted from: https://rosettacode.org/wiki/Levenshtein_distance#Java
	* 
	* @return distance
	*/
	private int calculateLevenshtein(String word, String term) {
		word = word.toLowerCase();
		term = term.toLowerCase();

		int[] cost = new int[term.length() + 1];
		for (int j = 0; j < cost.length; j++)
			cost[j] = j;
		for (int i = 1; i <= word.length(); i++) {
			cost[0] = i;

			int nw = i - 1;
			for (int j = 1; j <= term.length(); j++) {
				int cj = Math.min(1 + Math.min(cost[j], cost[j - 1]), word.charAt(i - 1) == term.charAt(j - 1) ? nw : nw + 1);
				nw = cost[j];
				cost[j] = cj;
			}
		}
		return cost[term.length()];
	}
}
