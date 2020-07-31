package ie.gmit.sw.ai.search.searches;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
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
* Beam Search implementation.
* 
* It works by adding a highest scoring webpages (node) to the queue limited by the beamwidth.
* The node with the highest score is taken from the queue and searched adding it's children to the queue in the same way.
* 
* It is similar to the best first search, however every child is searched from each node and only the best three are taken
* and added to the queue. Whereas the BFS takes all options.
* 
* I wanted to uphold the SRP with this class, so all database indexing and scoring are all handled in separate classes. 
* This class solely handles this search algorithm.
* 
* I have used LinkedList which is sorted by a comparator. This instance is used across all threads in the thread pool
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
public class BeamSearch implements Runnable, Searchable{
	
	// Instance variables
	private Set<String> closed;
	private LinkedList<DocumentNode> queue;
	private String url;
	private List<String> searchTerms;
	private Options options;
	private ResultsDatabaseProxy db;
	
	// Constructor
	public BeamSearch(Set<String> closed, LinkedList<DocumentNode> queue, String url, 
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
				page = new Scoring(doc, term, options).getHeuristicScore(); // score page
				int pageScore = page.getScore();
				
				// Check if page is worth adding to queue or indexing.
				if (pageScore >= 1) {
					relevantPage = true;
					
					// Get highest scoring page (For multiple search terms).
					// So that the highest scoring search word node is added to the queue.
					// This is so if the last search term scored badly it would be at the back of the queue.
					if (pageScore >= higestScore) {
						higestScore = page.getScore();
						dn = new DocumentNode(doc, pageScore);
					}
				}
			}
			
			// Only add once if score is high enough.
			if (relevantPage) {
				queue.addLast(dn);
				db.index(searchTerms, page.getMetas(), page.getTitle(), page.getHeadings(), page.getBody());
			}
		} catch (IOException e) {}
		
		// Start searching through more nodes.
		process();
	}

	/**
	* Loop that takes the best node from the queue and searches it's children. 
	* If score is high add to queue to check the children of that node, and add words to db.
	* Only the top nodes are added to the queue limited by the beam width.
	* 
	* @see ResultsDatabaseProxy
	* @see DocumentNode
	* @see Scoring
	* @see WebPage
	*/
	public void process() {
		// start beam search
		while (!queue.isEmpty()) {
			DocumentNode node = queue.poll();
			Document doc = node.getDoc();
			
			Elements edges = doc.select("a[href]"); // a with href links
			ArrayList<DocumentNode> children = new ArrayList<>();
			
			for (Element e : edges) {
				String link = e.absUrl("href");
				
				// Check if goal conditions are met, or if link has already been searched.
				// Goal condition check is implemented in interface.
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
							// Search child and if score is high index and add children to arraylist to sort.
							page = new Scoring(child, term, options).getHeuristicScore(); // score page
							int pageScore = page.getScore();
							//System.out.println(page.getScore());
							
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
					} catch (Exception e1) {}
					
					// Only add once if score is high enough.
					if (relevantPage) {
						children.add(dn);
						db.index(searchTerms, page.getMetas(), page.getTitle(), page.getHeadings(), page.getBody());
					}
				}
			}
			
			// Get all children and sort by score.
			try {
				Collections.sort(children, Comparator.comparing(DocumentNode::getScore).reversed()); //Sort the children
				
				int bound = 0;
				if (children.size() < BEAM_WIDTH){
					bound = children.size();
				}else{
					bound = BEAM_WIDTH;
				}
				
				//Add highest scored children to the queue limited by bound.
				for (int i = 0; i < bound; i++) {
					queue.addLast(children.get(i)); //Like BFS
				}
				
				// Sort the queue with the new children.
				Collections.sort(queue, Comparator.comparing(DocumentNode::getScore).reversed());
			} catch (Exception e1) {}
		}
	}
}
