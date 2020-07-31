package ie.gmit.sw.ai.search.models;

/**
* Class that holds data for a single webpage. I decided to use an object to return to each webpage
* as it was breaking the SRP indexing the data in the Scoring class. Scoring now just scores and returns
* data to be passed to the db if score is high enough. 
* 
* @author Matthew Sloyan
*/
public class WebPage {
	private String metas;
	private String title;
	private String headings;
	private String body;
	private int score;
	
	public WebPage(String metas, String title, String headings, String body, int score) {
		super();
		this.metas = metas;
		this.title = title;
		this.headings = headings;
		this.body = body;
		this.score = score;
	}

	public String getMetas() {
		return metas;
	}

	public String getTitle() {
		return title;
	}

	public String getHeadings() {
		return headings;
	}

	public String getBody() {
		return body;
	}

	public int getScore() {
		return score;
	}
}
