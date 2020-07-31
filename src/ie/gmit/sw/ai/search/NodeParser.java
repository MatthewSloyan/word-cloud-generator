package ie.gmit.sw.ai.search;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import ie.gmit.sw.ai.cloud.WordFrequency;
import ie.gmit.sw.ai.search.database.ResultsDatabaseProxy;
import ie.gmit.sw.ai.search.models.DocumentNode;
import ie.gmit.sw.ai.search.models.Files;
import ie.gmit.sw.ai.search.models.Options;
import ie.gmit.sw.ai.search.searches.BeamSearch;
import ie.gmit.sw.ai.search.searches.BestFirstSearch;
import ie.gmit.sw.ai.search.searches.RecursiveDepthFirstSearch;

/**
* Parsers all links from duck duck go search and runs search algorithms using an ExecutorService.
* Also gets results from database to pass back to ServiceHandler.
* 
* @see Options
* @see ResultsDatabaseProxy
* @see BestFirstSearch
* @see BeamSearch
* @see RecursiveDepthFirstSearch
* @author Matthew Sloyan
*/
public class NodeParser implements Parseable{
	
	static final int BRANCING_FACTOR = 12;

	// Search queues and sets.
	// Used a PriorityBlockingQueue for the best first search, as it's concurrent and is sorted by score.
	private Set<String> closed = new ConcurrentSkipListSet<>();
	private Queue<DocumentNode> queue = new PriorityBlockingQueue<>(20, Comparator.comparing(DocumentNode::getScore).reversed());
	private LinkedList<DocumentNode> queueBeam = new LinkedList<>();
	
	// Thread pool
	private ExecutorService es = Executors.newCachedThreadPool();
	
	// Database Proxy to be passed into searches.
	private ResultsDatabaseProxy db;
	
	// Instance variables
	private String url;
	private String searchTerm;
	private Options options; // Composition

	public NodeParser(String url, String searchTerm, Options options) throws Exception {
		this.url = url;
		this.searchTerm = searchTerm;
		this.options = options;
		db = new ResultsDatabaseProxy();
		
		parse();
	}

	/**
	* Parsers all links from duck duck go search and runs search algorithms using an ExecutorService.
	* Handles multiple search terms using an ArrayList.
	* 
	* @see BestFirstSearch
	* @see BeamSearch
	* @see RecursiveDepthFirstSearch
	*/
	public void parse() {
		System.out.println("Search started. Please wait...\n");
		
		try {
			// Get links from duck duck go search.
			Document doc = Jsoup.connect(url).get();
			Elements res = doc.getElementById("links").getElementsByClass("results_links");
			
			// Handle multiple search terms.
			List<String> searchTerms = new ArrayList<String>();
			
			// If multiple search terms add all words as list, or else just add one word.
			String split[] = searchTerm.split(" "); 
			if (split == null) {
				searchTerms.add(searchTerm.toLowerCase());
			}
			else {
				for (String string : split) {
					searchTerms.add(string.toLowerCase());
				}
			}
			
			int branchFactor = 0;
			for(Element r: res){
				
				//A branching factor for each node is implemented so that the number of nodes that can be expanded for each node is limited.
				//This was implemented from testing to increase the speed of the algorithm, the accuracy and so it doesn't go down one path for too long.
				if (branchFactor == BRANCING_FACTOR) {
					break;
				}
				
				Element title = r.getElementsByClass("links_main").first().getElementsByTag("a").first();
				String link = title.attr("href");
				
				// Run search algorithm depending on option selected by the user.
				switch (options.getSearch()) {
					case 1:
						es.execute(new BestFirstSearch(closed, queue, link, searchTerms, options, db));
						break;
					case 2:
						es.execute(new RecursiveDepthFirstSearch(closed, link, searchTerms, options, db));
						break;
					case 3:
						es.execute(new BeamSearch(closed, queueBeam, link, searchTerms, options, db));
						break;
					default:
						break;
				}
				
				branchFactor++;
			}
		} catch (Exception e) {
			System.out.println("Error, please try again.");
		}
		
		// Shutdown ExecutorService
		es.shutdown();
	}

	/**
	* Wait for threads to finish and get sorted results from database to pass back to ServiceHandler.
	* 
	* @see ResultsDatabaseProxy
	*/
	public WordFrequency[] getResults() {
		try {
		  es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
		}
		
		return db.getResults(options.getWordCloudNum());
	}

	// Testing.
	public static void main(String[] args) throws Exception {
		//start the running time of program to be printed out for user
		
		List<String> fileNames = new ArrayList<>();
		fileNames.add("WebContent/res/ignorewords.txt");
		fileNames.add("WebContent/res/heuristic.fcl");
		fileNames.add("WebContent/res/model.eg");
		fileNames.add("WebContent/res/model.ser");
		Files.getInstance().setFileNames(fileNames);
		
		// search = Search algo used (BFS, DFS, BS)
		// heuristic = AI heuristic used (Fuzzy, Encog)
		// scoring = Use of frequency of Levenshtein distance.
		// goal = Search goal type (Max nodes or Max words)
		// wordCloudNum = Number of words to display in wordcloud
		Options options = new Options(1, 1, 1, 2, 30);
		
		long startTime = System.nanoTime();
				
		// url, search term.
		NodeParser n = new NodeParser("https://duckduckgo.com/html/?q=Messi", "Messi", options);
		
		n.getResults();
		
		//running time
		System.out.println("\nRunning time (ms): " + (System.nanoTime() - startTime));
		final long usedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		System.out.println("Used memory: " + usedMem + "\n");
	}

}