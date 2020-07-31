package ie.gmit.sw.ai.search.heuristics;

import java.io.File;
import org.encog.Encog;
import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import org.encog.persist.EncogDirectoryPersistence;

import ie.gmit.sw.ai.search.Scoring;
import ie.gmit.sw.ai.search.models.Files;

/**
* Class that handles Encog Heuristic implementation.
* 
* @see Heuristicable
* @see Scoring
* @author Matthew Sloyan
*/
public class EncogHeuristic implements Heuristicable {
	
	public static final String FILENAME = "WebContent/res/model.eg";
	
	// Sample training data
	double[][] input = { 
		// Low
		{0, 0, 0, 18}, {0, 0, 0, 12}, {0, 0, 0, 84}, {0, 0, 0, 6}, {0, 0, 0, 12}, {0, 0, 0, 246},
		{0, 50, 0, 78}, {0, 50, 0, 174}, {0, 0, 50, 28}, {0, 0, 0, 48}, {0, 0, 0, 0}, 
		//Medium
		{100, 0, 100, 48}, {100, 0, 100, 78}, {0, 50, 0, 1620}, {0, 0, 100, 35}, {0, 0, 50, 77}, 
		{0, 0, 100, 36}, {0, 0, 50, 236}, 
		// High
		{400, 50, 400, 600}, {500, 50, 500, 198}, {100, 50, 100, 288}, {200, 50, 200, 108},
		{100, 50, 100, 78}, {100, 70, 50, 98}, {1300, 50, 1300, 156}, {300, 50, 300, 216}
	};
	
	double expected[][] = { 
		// Low
		{ 1, 0, 0 }, { 1, 0, 0 }, { 1, 0, 0 },  { 1, 0, 0 }, { 1, 0, 0 }, { 1, 0, 0 }, 
		{ 1, 0, 0 }, { 1, 0, 0 }, { 1, 0, 0 }, { 1, 0, 0 }, { 1, 0, 0 }, 
		// Medium
		{ 0, 1, 0 }, { 0, 1, 0 }, { 0, 1, 0 }, { 0, 1, 0 }, 
		{ 0, 1, 0 }, { 0, 1, 0 }, { 0, 1, 0 }, 
		// High
		{ 0, 0, 1 }, { 0, 0, 1 }, { 0, 0, 1 }, { 0, 0, 1 }, { 0, 0, 1 },
		{ 0, 0, 1 }, { 0, 0, 1 }, { 0, 0, 1 }
	};
	
	/**
	* Using the model.eg file get the predicted results for the parameters passed in
	* (Meta, title, heading and body score) and return result to Scoring.
	* 
	* @see Heuristicable
	* @see Scoring
	* @author Matthew Sloyan
	*/
	public int getHeuristicScore(int meta, int title, int headings, int body) {
		String fileName = Files.getInstance().getFileNames().get(2);
		BasicNetwork network = (BasicNetwork)EncogDirectoryPersistence.loadObject(new File(fileName));
		
		// Set up data and pass in variables.
		MLData input = new BasicMLData(4);
		input.setData(0, meta);
		input.setData(1, title);
		input.setData(2, headings);
		input.setData(3, body);
		MLData output = network.compute(input);
		
		int score = 0;
		
		// Get index of returned results consistent with Fuzzy.
		// Low = 0, Medium = 1, High = 2;
		for (int i = 0; i < 3; i++) {
			if ((int) Math.round(output.getData(i)) == 1) {
				score = i;
			}
		}

		return score;
	}
	
	/**
	* Used to train the neural netword myself using the training data I created below.
	* On average it trains with a 96% accuracy and does it within a second.
	* I tested multiple activation functions, number of hidden layers and different Propagation types
	* and this setup was the fastest and most accurate.
	* 
	* @see Heuristicable
	* @see Scoring
	* @author Matthew Sloyan
	*/
	public void trainNeuralNetwork() {
		
		// Step 1: Declare a Network Topology
		BasicNetwork network = new BasicNetwork();
		network.addLayer(new BasicLayer(null, true, 4));
		network.addLayer(new BasicLayer(new ActivationSigmoid(), true, 30));
		network.addLayer(new BasicLayer(new ActivationSigmoid(), false, 3));
		network.getStructure().finalizeStructure();
		network.reset();
		
		// Tried to normalize the data but was unsuccessful.
		//input = Utils.normalize(input, 0, 100);
		//System.out.println(Arrays.deepToString(input));

		// Step 2: Create the Training Data Set
		MLDataSet trainingSet = new BasicMLDataSet(input, expected);

		// Step 3: Train the Neural Network
		ResilientPropagation train = new ResilientPropagation(network, trainingSet);
		double minError = 0.1; // Change and see the effect on the result.
		int epoch = 1;

		System.out.println("Training started..");
		do {
			train.iteration();
			epoch++;
		} while (train.getError() > minError);
		train.finishTraining();
		
		// Save the network.
		// Code adapted from: https://github.com/jeffheaton/encog-java-examples/blob/master/src/main/java/org/encog/examples/neural/persist/EncogPersistence.java#L73
		//String fileName = Files.getInstance().getFileNames().get(2);
		EncogDirectoryPersistence.saveObject(new File("WebContent/res/model.eg"), network);
		
		System.out.println("Training complete " + epoch + " epocs with e=" + train.getError());

		// Step 4: Test the NN
		double correct = 0;
		double total = 0;
		
		for (MLDataPair pair : trainingSet) {
			total++;
			MLData output = network.compute(pair.getInput());
			
			int expectedValue = (int) Math.round(output.getData(0));
			int actual = (int) pair.getIdeal().getData(0);
			if (expectedValue == actual) {
				correct++;
			}
		}
		System.out.println("Testing complete. Acc=" + ((correct / total) * 100));
		
		// Step 5: Shutdown the NN
		Encog.getInstance().shutdown();
		
		// Tests
//		MLData input = new BasicMLData(4);
//		input.setData(0, 0);
//		input.setData(1, 0);
//		input.setData(2, 0);
//		input.setData(3, 0);
//		MLData output = network.compute(input);
//		
//		System.out.println((int) Math.round(output.getData(0)));
//		System.out.println((int) Math.round(output.getData(1)));
//		System.out.println((int) Math.round(output.getData(2)));
//		
//		MLData input1 = new BasicMLData(4);
//		input1.setData(0, 100);
//		input1.setData(1, 70);
//		input1.setData(2, 100);
//		input1.setData(3, 70);
//		MLData output1 = network.compute(input1);
//		
//		System.out.println((int) Math.round(output1.getData(0)));
//		System.out.println((int) Math.round(output1.getData(1)));
//		System.out.println((int) Math.round(output1.getData(2)));
//		
//		MLData input2 = new BasicMLData(4);
//		input2.setData(0, 0);
//		input2.setData(1, 0);
//		input2.setData(2, 50);
//		input2.setData(3, 30);
//		MLData output2 = network.compute(input2);
//		
//		System.out.println((int) Math.round(output2.getData(0)));
//		System.out.println((int) Math.round(output2.getData(1)));
//		System.out.println((int) Math.round(output2.getData(2)));
	}
	
	// Testing to train NN
//	public static void main(String[] args) {
//		// TODO Auto-generated method stub
//		//new EncogHeuristic().trainNeuralNetwork();
//		System.out.println(new EncogHeuristic().getHeuristicScore(100, 100, 100, 100));
//	}

}
