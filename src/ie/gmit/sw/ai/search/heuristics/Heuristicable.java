package ie.gmit.sw.ai.search.heuristics;

/**
* Interface used for all heuristic predictions. (Fuzzy & Encog)
* More could be added easily.
*/
public interface Heuristicable {
	public int getHeuristicScore(int meta, int title, int headings, int body);
	
	default void trainNeuralNetwork() {
	}
}
