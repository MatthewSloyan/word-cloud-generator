package ie.gmit.sw.ai.search.searches;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import ie.gmit.sw.ai.search.NodeParser;
import ie.gmit.sw.ai.search.Scoring;
import ie.gmit.sw.ai.search.database.ResultsDatabaseProxy;
import ie.gmit.sw.ai.search.models.DocumentNode;
import ie.gmit.sw.ai.search.models.Options;
import ie.gmit.sw.ai.search.models.WebPage;

/**
* Recursive Depth First Search implementation (Fastest).
* 
* It works by recursively searching children of each node until the score is low or goal conditions are met.
* If the score is low it moves back to the parent and goes down another route.
* This algorithm is the fastest and it very accurate, however accuracy isn't always insured as it could go down unrelated paths.
* 
* I wanted to uphold the SRP with this class, so all database indexing and scoring are all handled in separate classes. 
* This class solely handles this search algorithm.
* 
* Unlike other searches it doesn't require any queues to pull nodes from, making it very fast.
* 
* @see NodeParser
* @see Searchable
* @see Options
* @see ResultsDatabaseProxy
* @see DocumentNode
* @see Scoring
* @see WebPage
* @author Matthew Sloyan
*/
public class RecursiveDepthFirstSearch implements Runnable, Searchable{
	
	private Set<String> closed;
	private String url;
	private List<String> searchTerms;
	private Options options;
	private ResultsDatabaseProxy db;
	
	public RecursiveDepthFirstSearch(Set<String> closed, String url, 
			List<String> searchTerms, Options options, ResultsDatabaseProxy db) {
		super();
		this.closed = closed;
		this.url = url;
		this.searchTerms = searchTerms;
		this.options = options;
		this.db = db;
	}

	/**
	* Runnable thread that is spawned from the thread pool in NodeParser.
	* 
	* Initially adds the first link to the queue if the score is high enough (>= 1). This first link is one of the results returned
	* from the duck duck go search. If the initial webpage doesn't score high or medium it's cut out.
	* Running time: O(N) - Determined by number of search terms.
	* 
	* @see ResultsDatabaseProxy
	* @see DocumentNode
	* @see Scoring
	* @see WebPage
	*/
	public void run() {
		try {
			// Get the document from the duckduckgo search (url).
			Document doc = Jsoup.connect(url).get();
			WebPage page = null;
			boolean relevantPage = false;
			
			// Add url to closed set, so it won't be searched again.
			closed.add(url);
			
			// Handle multiple search words.
			// Loop through the search terms and score each page for each search term. I have implemented this so multiples 
			// of the same nodes aren't added to the queue, or the same words indexed twice.
			// If there is just one search term this is linear O(1).
			for (String term : searchTerms) {
				page = new Scoring(doc, term, options).getHeuristicScore(); // score page
				
				// Check if page is worth adding to queue or indexing.
				if (page.getScore() >= 1) {
					relevantPage = true;
				}
			}
			
			// Only add once if score is high enough.
			// This is done here so that the same nodes are not searched twice. Also so the same words aren't indexed again.
			if (relevantPage) {
				db.index(searchTerms, page.getMetas(), page.getTitle(), page.getHeadings(), page.getBody());
				search(doc);
			}
		} catch (IOException e) {
			
		}
	}

	/**
	* Method that searches a node, and can recursively search the children of the node until conditions are met.
	* Running time: O(N^2) but inner loop is determined by number of search terms.
	* 
	* @see ResultsDatabaseProxy
	* @see DocumentNode
	* @see Scoring
	* @see WebPage
	*/
	public void search(Document doc) {
		Elements edges = doc.select("a[href]"); // a with href links
		
		for (Element e : edges) {
			String link = e.absUrl("href");
			
			// Check if goal conditions are met, or if link has already been searched.
			// Goal condition check is implemented in interface.
			if (link != null && !closed.contains(link) && checkForGoal(options.getGoal(), db.checkSize(), closed.size())) {
				Document child = null;
				WebPage page = null;
				boolean relevantPage = false;

				// Add url to closed set, so it won't be searched again.
				closed.add(link);
				
				try {
					child = Jsoup.connect(link).get();
				
					for (String term : searchTerms) {
						// Score page and get page data.
						page = new Scoring(child, term, options).getHeuristicScore();
						
						if (page.getScore() >= 1) {
							relevantPage = true;
						}
					}
				} catch (Exception e1) {}
				
				// Only add once if score is high enough.
				// This is done here so that the same nodes are not searched twice. Also so the same words aren't indexed again.
				if (relevantPage) {
					db.index(searchTerms, page.getMetas(), page.getTitle(), page.getHeadings(), page.getBody());
					search(child);
				}
			} // if
		} // for (links)
	}
}
