package ie.gmit.sw.ai.search.heuristics;

import ie.gmit.sw.ai.search.Scoring;
import ie.gmit.sw.ai.search.models.Files;
import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import net.sourceforge.jFuzzyLogic.rule.Variable;

/**
* Class that handles Fuzzy Heuristic implementation.
* 
* @see Heuristicable
* @see Scoring
* @author Matthew Sloyan
*/
public class FuzzyHeuristic implements Heuristicable{
	
	/**
	* Using the heuristic.fcl file get the fuzzy value for the parameters passed in
	* (Meta, title, heading and body score) and return result to Scoring.
	* 
	* @see Heuristicable
	* @see Scoring
	* @author Matthew Sloyan
	*/
	public int getHeuristicScore(int meta, int title, int headings, int body) {
		String fileName = Files.getInstance().getFileNames().get(1);
		
		// Load from 'FCL' file
		FIS fis = FIS.load(fileName, true);

		// Error while loading?
		if (fis == null) {
			System.err.println("Can't load file: '" + fileName + "'");
		}
		
		FunctionBlock fb = fis.getFunctionBlock("scores");

		// Set inputs
		fis.setVariable("meta", meta);
		fis.setVariable("title", title);
		fis.setVariable("headings", headings);
		fis.setVariable("body", body);

		// Evaluate
		fis.evaluate();
		
		// Show output variable's chart
        Variable fuzzyScore = fb.getVariable("score");
        
        // Get and round final result.
        int score = (int) Math.round(fuzzyScore.getLatestDefuzzifiedValue());
        
        // Return number that can be used for Encog too without checks.
        if (score >= 50) {
        	// High
        	return 2;
        }
        else if (score >= 30) {
        	// Medium
        	return 1;
        }
        else {
        	// Low
        	return 0;
        }
	}
	
	// Testing
//	public static void main(String[] args) {
//		new FuzzyHeuristic().getHeuristicScore(100, 50, 100, 100);
//	}
}
