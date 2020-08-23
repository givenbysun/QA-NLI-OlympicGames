package nlidb;

import java.io.*;
import java.util.HashMap;

import edu.stanford.nlp.trees.Tree;

/**
 * This class provides the basic parameters and values that are regularly used in whole project
 * 
 * @author Luca Graziani, Mehrdad Alizadeh
 */

public class Utils {
	
	public static String ROOT = "ROOT"; 
	public static String S = "S";
	public static String SQ = "SQ";
	public static String SBARQ = "SBARQ";
	public static String VP = "VP";
	public static String NP = "NP";
	public static String NN = "NN";
	public static String NNP = "NNP";
	public static String NNPS = "NNPS";
	public static String VB = "VB";
	public static String ADVP = "ADVP";
	public static String VBD = "VBD";
	public static String VBG = "VBG";
	public static String PP = "PP";
	public static String IN = "IN";
	public static String DT = "DT";
	public static String JJ = "JJ";
	public static String ADJP = "ADJP";
	public static String WHNP = "WHNP";
	public static String WP = "WP";
	public static String RB = "RB";
	public static String CD = "CD";
	
	private static HashMap<String,String> nations = null;
	
	// Main method for testing the class functions
	public static void main(String[] args){
		System.out.println(getNation("german"));
		System.out.println(getNation("american"));
	}

	/**
	 * This methods uses  HashMap (nations) to retrieve the nation of asked nationality
	 * If nation is null Then it is initialize with (nation,nationality) pairs reading from a csv file
	 * @param nationality: for example "Canadian"
	 * @return the String that is the nation which nationality refers to for example "Canada" for "Canadian"
	 */
	public static String getNation(String nationality){
		
		if(nations==null){
			nations = new HashMap<String,String>();		    
		    BufferedReader buffer;
			try {
				/*
				// reading a file from JAR file
				InputStream in = new Utils().getClass().getResourceAsStream("/nationality.csv");
				buffer = new BufferedReader(new InputStreamReader(in));
				*/
				
				// reading a file from IDE
				buffer = new BufferedReader(new FileReader("nationality.csv"));
				String line = "";
			    while((line=buffer.readLine()) != null){
			  	  if(line.length()<2)
			  		  continue;

			  	  String[] items = line.split(",");
			  	  nations.put(items[1], items[0]);
			    }
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return nations.get(nationality);
	}
	
	/**
	 * @param rank: "first" , "second" , "third"
	 * @return "gold" for "first", "silver" for "second", "bronze" for "second"
	 */
	public static String getMedal(String rank) {
		
		String lwr = rank.toLowerCase();
		if(lwr.equals("first")) {
			return "gold";
		} else if(lwr.equals("second")) {
			return "silver";
		} else if(lwr.equals("third")) {
			return "bronze";
		}
		return null;
	}
	
	/**
	 * @param name: "man" , "woman"
	 * @return "M" for "man" , "F" for "woman" 
	 */
	public static String getGender(String name) {
		String lwr = name.toLowerCase();
		if(lwr.equals("man")) {
			return "M";
		} else if(lwr.equals("woman")) {
			return "F";
		}
		return null;
	}
	
	/**
	 * @param node: is a node of the sentence syntactic structure
	 * @param label: any possible value for the syntactic structure
	 * @return true if the node's label is the same as the input label, otherwise false
	 */
	public static boolean isA(Tree node, String label) {
		return node.label().toString().equals(label);
	}
	
	/**
	 * @param node: is a node of the sentence syntactic structure
	 * @return value of the terminal leaf that tag nodes of the Tree points to
	 */
	public static String leafValue(Tree node) {
		if(node.children().length == 1) {
			if(node.children()[0].isLeaf()) {
				return node.children()[0].label().toString().toLowerCase();
			}
		}
		return null;
	}
	
}
