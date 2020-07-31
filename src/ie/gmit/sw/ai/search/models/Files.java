package ie.gmit.sw.ai.search.models;

import java.util.ArrayList;
import java.util.List;

import ie.gmit.sw.ai.search.database.IgnoreWords;
import ie.gmit.sw.ai.search.heuristics.EncogHeuristic;
import ie.gmit.sw.ai.search.heuristics.FuzzyHeuristic;

/**
* Class that holds file paths for the application. It is used by Heuristic searches and 
* for loading the ignore words file.
* 
* I wanted to have one place to add all the file paths, rather than passing them through classes or hard coding them.
* So the service handler gets the full path for the machine it's running on and adds them all to an array list. 
* Also more files could easily be added without having to add more parameters to each class.
* 
* index 0 - ignorewords.txt
* index 1 - heuristic.fcl
* index 2 - model.eg
*
* I have implemented a Singleton design pattern for this so it can be easily accessed by any class that requires it.
* Also as these paths are the same for anyone using the application it made sense to have one instance.
* 
* @see EncogHeuristic
* @see FuzzyHeuristic
* @see IgnoreWords
* @author Matthew Sloyan
*/
public class Files {
	private List<String> fileNames = new ArrayList<>();
	
	// Singleton design pattern.
	private static Files instance = new Files();
	
	// private constructor, so no other class can create an instance.
	private Files() {}
    
    public static Files getInstance() {
        return instance;
    }

    // Gets and sets
	public List<String> getFileNames() {
		return fileNames;
	}

	public void setFileNames(List<String> fileNames) {
		this.fileNames = fileNames;
	}
}
