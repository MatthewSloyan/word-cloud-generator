package ie.gmit.sw;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import ie.gmit.sw.ai.cloud.LogarithmicSpiralPlacer;
import ie.gmit.sw.ai.cloud.WeightedFont;
import ie.gmit.sw.ai.cloud.WordFrequency;
import ie.gmit.sw.ai.search.NodeParser;
import ie.gmit.sw.ai.search.models.Files;
import ie.gmit.sw.ai.search.models.Options;

/*
 * -------------------------------------------------------------------------------------------------------------------
 * PLEASE READ THE FOLLOWING CAREFULLY. MOST OF THE "ISSUES" STUDENTS HAVE WITH DEPLOYMENT ARISE FROM NOT READING
 * AND FOLLOWING THE INSTRUCTIONS BELOW.
 * -------------------------------------------------------------------------------------------------------------------
 *
 * To compile this servlet, open a command prompt in the web application directory and execute the following commands:
 *
 * Linux/Mac													Windows
 * ---------													---------	
 * cd WEB-INF/classes/											cd WEB-INF\classes\
 * javac -cp .:$TOMCAT_HOME/lib/* ie/gmit/sw/*.java				javac -cp .:%TOMCAT_HOME%/lib/* ie/gmit/sw/*.java
 * cd ../../													cd ..\..\
 * jar -cf wcloud.war *											jar -cf wcloud.war *
 * 
 * Drag and drop the file ngrams.war into the webapps directory of Tomcat to deploy the application. It will then be 
 * accessible from http://localhost:8080. The ignore words file at res/ignorewords.txt will be located using the
 * IGNORE_WORDS_FILE_LOCATION mapping in web.xml. This works perfectly, so don't change it unless you know what
 * you are doing...
 * 
*/

public class ServiceHandler extends HttpServlet {
	private List<String> fileNames = new ArrayList<>();
	
	// Run when application is setup on server. Add all files paths and set instance (Singlton).
	public void init() throws ServletException {
		ServletContext ctx = getServletContext(); //Get a handle on the application context
		
		//Reads the value from the <context-param> in web.xml
		String ignoreWords = getServletContext().getRealPath(File.separator) + ctx.getInitParameter("IGNORE_WORDS_FILE_LOCATION"); 
		fileNames.add(ignoreWords);
		
		String heuristic = getServletContext().getRealPath(File.separator) + ctx.getInitParameter("HEURISTIC_FILE_LOCATION");
		fileNames.add(heuristic);
		
		String encog = getServletContext().getRealPath(File.separator) + ctx.getInitParameter("ENCOG_FILE_LOCATION");
		fileNames.add(encog);
		
		String customNN = getServletContext().getRealPath(File.separator) + ctx.getInitParameter("NN_FILE_LOCATION");
		fileNames.add(customNN);
		
		Files.getInstance().setFileNames(fileNames);
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html"); //Output the MIME type
		PrintWriter out = resp.getWriter(); //Write out text. We can write out binary too and change the MIME type...
		
		//Get variables from forms. These are local to this method and thread safe...
		int optionSearch = Integer.parseInt(req.getParameter("optionsSearch")); 
		int optionHeuristic = Integer.parseInt(req.getParameter("optionsHeuristic")); 
		int optionScoring = Integer.parseInt(req.getParameter("optionsScoring")); 
		int optionGoal = Integer.parseInt(req.getParameter("optionsGoal")); 
		int optionWcNum = Integer.parseInt(req.getParameter("optionsWcNum")); 
		String s = req.getParameter("query");
		
		// == OPTIONS ==
		Options options = new Options(optionSearch, optionHeuristic, optionScoring, optionGoal, optionWcNum);
		
		long startTime = System.nanoTime();
		
		// == SEARCH ==
		WordFrequency[] words = null;
		try {
			words = new WeightedFont().getFontSizes(getWordFrequencyKeyValue(s, options));
			//Arrays.sort(words, Comparator.comparing(WordFrequency::getFrequency, Comparator.reverseOrder()));
			//Arrays.stream(words).forEach(System.out::println);
		} catch (Exception e) {
			System.out.println("Couldn't find suitable results");
		}
		
		//running time
		System.out.println("\nRunning time (ms): " + (System.nanoTime() - startTime));
		final long usedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		System.out.println("Used memory: " + usedMem + "\n");
		
		out.print("<html><head><title>Artificial Intelligence Assignment</title>");		
		out.print("<link rel=\"stylesheet\" href=\"includes/style.css\">");
		
		out.print("</head>");		
		out.print("<body>");		
		out.print("<div style=\"font-size:48pt; font-family:arial; color:#990000; font-weight:bold\">Web Opinion Visualiser</div>");
		
		// Handle if no results are found.
		// Else create word cloud.
		if (words == null) {
			out.print("<h2>Couldn't find suitable results for your query or you have been locked out of DuckDuckGo, please try again.</h2>");
		}
		else {
			out.print("<p><fieldset><legend><h3>Result</h3></legend>");
			
			//Spira Mirabilis
			LogarithmicSpiralPlacer placer = new LogarithmicSpiralPlacer(800, 600);
			
			for (WordFrequency word : words) {
				placer.place(word); //Place each word on the canvas starting with the largest
			}

			BufferedImage cloud = placer.getImage(); //Get a handle on the word cloud graphic
			out.print("<img src=\"data:image/png;base64," + encodeToString(cloud) + "\" alt=\"Word Cloud\">");
			
			out.print("</fieldset>");	
			out.print("<p>Branching factor = 12<p>");
			out.print("<p>Time taken (ns) = " + (System.nanoTime() - startTime) + "<p>");
			
			out.print("<p><b>Your Options</b><p>");
			out.print("<p>Search: " + optionSearch + " (1 = BFS, 2 = RDFS, 3 = BS)<p>");
			out.print("<p>Heuristic: " + optionHeuristic + " (1 = Fuzzy, 2 = Encog, 3 = Custom NN)<p>");
			out.print("<p>Scoring: " + optionScoring + " (1 = Frequency, 2 = Levenshtein)<p>");
			out.print("<p>Goal: " + optionGoal + " (1 = Max Words, 2 = Max Nodes)<p>");
			out.print("<p>Word Cloud Number: " + optionWcNum + "<p>");

		}

		out.print("<a href=\"./\">Return to Start Page</a>");
		out.print("</body>");	
		out.print("</html>");	
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
 	}

	// Starts search and returns results as WordFrequency Array.
	private WordFrequency[] getWordFrequencyKeyValue(String query, Options options) {
		try {
			// Start search and get results.
			NodeParser n = new NodeParser("https://duckduckgo.com/html/?q=" + query, query, options);
			
			return n.getResults();
		} catch (Exception e) {
			return null;
		}
	}
	
	private String encodeToString(BufferedImage image) {
	    String s = null;
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();

	    try {
	        ImageIO.write(image, "png", bos);
	        byte[] bytes = bos.toByteArray();

	        Base64.Encoder encoder = Base64.getEncoder();
	        s = encoder.encodeToString(bytes);
	        bos.close();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	    return s;
	}
	
	private BufferedImage decodeToImage(String imageString) {
	    BufferedImage image = null;
	    byte[] bytes;
	    try {
	        Base64.Decoder decoder = Base64.getDecoder();
	        bytes = decoder.decode(imageString);
	        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
	        image = ImageIO.read(bis);
	        bis.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return image;
	}
}