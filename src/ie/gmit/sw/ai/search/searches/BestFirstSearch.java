package ie.gmit.sw.ai.search.searches;

import java.io.IOException;
import java.util.List;
import java.util.Queue;
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
* Best First Search implementation.
* 
* It works by adding each webpage (node) and it's score to a queue which is sorted by score. 
* The node with the highest score is taken from the queue and searched adding it's children to the queue in the same way.
//A branching factor for each node is implemented so that the number of nodes that can be expanded for each node is limited.
//This was implemented from testing to increase the speed of the algorithm, the accuracy and so it doesn't go down one path for too long.
* 
* I wanted to uphold the SRP with this class, so all database indexing and scoring are all handled in separate classes. 
* This class solely handles this search algorithm.
* 
* I have used PriorityBlockingQueue with a comparator based on the score. This instance is used across all threads in the thread pool
* as it's passed in from the NodeParser class. So that any running instance of this search is always taking the highest scored node from
* the front of the queue.
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
public class BestFirstSearch implements Runnable, Searchable{
	
	// Instance variables
	private Set<String> closed;
	private Queue<DocumentNode> queue;
	private String url;
	private List<String> searchTerms;
	private Options options;
	private ResultsDatabaseProxy db;

	// Constructor
	public BestFirstSearch(Set<String> closed, Queue<DocumentNode> queue, String url, 
			List<String> searchTerms, Options options, ResultsDatabaseProxy db) {
		super();
		this.closed = closed;
		this.queue = queue;
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
			DocumentNode dn = null;
			WebPage page = null;
			boolean relevantPage = false;
			int higestScore = 0;
			
			// Add url to closed set, so it won't be searched again.
			closed.add(url);
			
			// Handle multiple search words.
			// Loop through the search terms and score each page for each search term. I have implemented this so multiples 
			// of the same nodes aren't added to the queue, or the same words indexed twice.
			// If there is just one search term this is linear O(1).
			for (String term : searchTerms) {
				
				// Score each page, and return contents to pass to db if score is high or medium. (SRP)
				page = new Scoring(doc, term, options).getHeuristicScore();
				int pageScore = page.getScore();
				
				// Check if page is worth adding to queue or indexing.
				if (pageScore >= 1) {
					relevantPage = true;
					
					// Get highest scoring page (For multiple search terms).
					// So that the highest scoring search word is added to the queue.
					// This is so if the last search term scored badly it would be at the back of the queue.
					if (pageScore >= higestScore) {
						higestScore = page.getScore();
						dn = new DocumentNode(doc, pageScore);
					}
				}
			}
			
			// Only add once if score is high enough.
			if (relevantPage) {
				queue.offer(dn);
				db.index(searchTerms, page.getMetas(), page.getTitle(), page.getHeadings(), page.getBody());
			}
		} catch (IOException e) {}
		
		// Start searching through more nodes.
		process();
	}

	/**
	* Loop that takes the best node from the queue and searches it's children. 
	* If score is high add to queue to check the children of that node, and add words to db.
	* 
	* @see ResultsDatabaseProxy
	* @see DocumentNode
	* @see Scoring
	* @see WebPage
	*/
	public void process() {
		// start bfs, and loop until goal conditions are met or queue is empty.
		while (!queue.isEmpty() && checkForGoal(options.getGoal(), db.checkSize(), closed.size())) {
			DocumentNode node = queue.poll();
			Document doc = node.getDoc();

			Elements edges = doc.select("a[href]"); // a with href links
			for (Element e : edges) {
				
				String link = e.absUrl("href");
				
				// Check if goal conditions are met, or if link has already been searched.
				if (link != null && !closed.contains(link) && checkForGoal(options.getGoal(), db.checkSize(), closed.size())) {
					
					DocumentNode dn = null;
					WebPage page = null;
					boolean relevantPage = false;
					int higestScore = 0;
					
					// Add url to closed set, so it won't be searched again.
					closed.add(link);
					
					try {
						Document child = Jsoup.connect(link).get();
						
						for (String term : searchTerms) {
							page = new Scoring(child, term, options).getHeuristicScore();
							int pageScore = page.getScore();
							
							//System.out.println(pageScore);
							
							// Check if page is worth adding to queue or indexing.
							if (pageScore >= 1) {
								relevantPage = true;
								
								// Get highest scoring page (For multiple search terms).
								// So that the highest scoring search word is added to the queue.
								if (pageScore >= higestScore) {
									higestScore = page.getScore();
									dn = new DocumentNode(child, pageScore);
								}
							}
						}
						
						// Only add once if score is high enough.
						if (relevantPage) {
							queue.offer(dn);
							db.index(searchTerms, page.getMetas(), page.getTitle(), page.getHeadings(), page.getBody());
						}

					} catch (Exception e2) {}
				}
			}
		}
	}
}
