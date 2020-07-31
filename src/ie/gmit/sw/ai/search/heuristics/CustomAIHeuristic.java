package ie.gmit.sw.ai.search.heuristics;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import ie.gmit.sw.ai.nn.BackpropagationTrainer;
import ie.gmit.sw.ai.nn.NeuralNetwork;
import ie.gmit.sw.ai.nn.Utils;
import ie.gmit.sw.ai.nn.activator.Activator;
import ie.gmit.sw.ai.search.Scoring;
import ie.gmit.sw.ai.search.models.Files;

/**
 * Class that handles a custom backpropagation Neural Network. 
 * Code to train neural network was supplied by our lecturer, and can be found in the ie.gmit.sw.ai.nn package.
 * 
 * @see Heuristicable
 * @see Scoring
 * @see NeuralNetwork
 * @author Matthew Sloyan
 */
public class CustomAIHeuristic implements Heuristicable {

	public static final String FILENAME = "WebContent/res/model.ser";

	// Sample training data
	double[][] input = {
			// Low
			{ 0, 0, 0, 18 }, { 0, 0, 0, 12 }, { 0, 0, 10, 84 }, { 0, 0, 0, 6 }, { 0, 0, 0, 12 }, { 0, 0, 20, 246 },
			{ 0, 50, 0, 78 }, { 0, 50, 0, 174 }, { 0, 0, 50, 28 }, { 0, 0, 30, 48 }, { 0, 0, 0, 0 },

			// Medium
			{ 100, 0, 70, 48 }, { 100, 0, 100, 78 }, { 0, 50, 0, 1620 }, { 0, 0, 100, 35 }, { 0, 0, 50, 77 },
			{ 0, 0, 100, 36 }, { 0, 0, 50, 236 },

			// High
			{ 400, 50, 400, 600 }, { 500, 50, 500, 198 }, { 100, 50, 100, 288 }, { 200, 50, 200, 108 },
			{ 100, 50, 100, 78 }, { 100, 70, 50, 98 }, { 1300, 50, 1300, 156 }, { 300, 50, 300, 216 } };

	double expected[][] = {
			// Low
			{ 1, 0, 0 }, { 1, 0, 0 }, { 1, 0, 0 }, { 1, 0, 0 }, { 1, 0, 0 }, { 1, 0, 0 }, { 1, 0, 0 }, { 1, 0, 0 },
			{ 1, 0, 0 }, { 1, 0, 0 }, { 1, 0, 0 },
			// Medium
			{ 0, 1, 0 }, { 0, 1, 0 }, { 0, 1, 0 }, { 0, 1, 0 }, { 0, 1, 0 }, { 0, 1, 0 }, { 0, 1, 0 },
			// High
			{ 0, 0, 1 }, { 0, 0, 1 }, { 0, 0, 1 }, { 0, 0, 1 }, { 0, 0, 1 }, { 0, 0, 1 }, { 0, 0, 1 }, { 0, 0, 1 } };

	/**
	 * Using the serialized model.ser file get the predicted results for the parameters passed
	 * in (Meta, title, heading and body score) and return result to Scoring.
	 * 
	 * @see Heuristicable
	 * @see Scoring
	 * @author Matthew Sloyan
	 */
	public int getHeuristicScore(int meta, int title, int headings, int body) {
		String fileName = Files.getInstance().getFileNames().get(3);

		// Read in the serialized NeuralNetwork object.
		NeuralNetwork nn = null;
		try {
			FileInputStream fileIn = new FileInputStream(fileName);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			nn = (NeuralNetwork) in.readObject();
			in.close();
			fileIn.close();
		} catch (Exception i) {}

		// Get index of returned results consistent with Fuzzy.
		// Low = 0, Medium = 1, High = 2;
		double[] result = null;
		try {
			double[] test = { meta, title, headings, body};
			result = nn.process(test);
		} catch (Exception e) {
		}

		return Utils.getMaxIndex(result);
	}

	/**
	 * Used to train the neural network myself using the training data I created
	 * below. On average it trains with a 100% accuracy and does it within a second.
	 * I tested some of the other activation functions, number of hidden layers to come to the final result.
	 * 
	 * To save the neural network I used serialization which converts the object to a byte stream. 
	 * This could then be read in by the prediction method.
	 * 
	 * @see NeuralNetwork
	 * @see Activator
	 * @see Serializable
	 * @author Matthew Sloyan
	 */
	public void trainNeuralNetwork() {

		// Step 1: Declare a Network Topology
		NeuralNetwork nn = new NeuralNetwork(Activator.ActivationFunction.Sigmoid, 4, 5, 3);

		// Tried to normalize the data but was unsuccessful.
		// input = Utils.normalize(input, 0, 100);
		// System.out.println(Arrays.deepToString(input));

		// Step 2: Train the Neural Network
		BackpropagationTrainer trainer = new BackpropagationTrainer(nn);
		trainer.train(input, expected, 0.01, 2000);

		// Step 3: Serialize the NN
		// Code adapted from: https://www.tutorialspoint.com/java/java_serialization.htm
		try {
			FileOutputStream fileOut = new FileOutputStream(FILENAME);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(nn);

			out.close();
			fileOut.close();
		} catch (IOException i) {
			i.printStackTrace();
		}

		// Step 4: Test the Neural Network
		try {
			double[] test = { 0, 0, 0, 0 };
			double[] result = nn.process(test);
			System.out.println(Utils.getMaxIndex(result));
		} catch (Exception e) {
		}
	}

	// Testing to train NN
//	public static void main(String[] args) {
//		// TODO Auto-generated method stub
//		//new CustomAIHeuristic().trainNeuralNetwork();
//		//System.out.println(new CustomAIHeuristic().getHeuristicScore(0, 0, 50, 60));
//	}

}
