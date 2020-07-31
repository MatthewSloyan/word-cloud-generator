package ie.gmit.sw.ai.search.models;

import org.jsoup.nodes.Document;

/**
* Class that holds data for a document and score to be added to queues in searches.
* 
* @author Matthew Sloyan
*/
public class DocumentNode{
	private Document doc;
	private int score;
	
	// constructor and gets
	public DocumentNode(Document doc, int score) {
		super();
		this.doc = doc;
		this.score = score;
	}
	
	public Document getDoc() {
		return doc;
	}
	public int getScore() {
		return score;
	}
}
