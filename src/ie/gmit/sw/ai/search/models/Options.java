package ie.gmit.sw.ai.search.models;

/**
* Class that holds user information selected. This is used across all classes to determine the options to pick.
* Like the Database, it had to be unique to each user so a Singleton couldn't be used.
* 
* search = Search algo used (BFS, DFS, BS)
* heuristic = AI heuristic used (Fuzzy, Encog)
* scoring = Use of frequency of Levenshtein distance.
* goal = Search goal type (Max nodes or Max words)
* wordCloudNum = Number of words to display in wordcloud
* 
* @author Matthew Sloyan
*/
public class Options  {
	private int search;
	private int heuristic;
	private int scoring;
	private int goal;
	private int wordCloudNum;
	
	public Options(int search, int heuristic, int scoring, int goal, int wordCloudNum) {
		super();
		this.search = search;
		this.heuristic = heuristic;
		this.scoring = scoring;
		this.goal = goal;
		this.wordCloudNum = wordCloudNum;
	}

	public int getSearch() {
		return search;
	}

	public int getHeuristic() {
		return heuristic;
	}

	public int getScoring() {
		return scoring;
	}

	public int getGoal() {
		return goal;
	}

	public int getWordCloudNum() {
		return wordCloudNum;
	}
}
