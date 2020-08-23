package nlidb;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

/**
 * This class was written just for testing purposes, it generates "every possible" allowed question from the project database 
 * @author Luca Graziani
 *
 */
public class QuestionGenerator {
	
	public static Set<String> sports = new HashSet<String>();
	public static Map<String, String> whVerbToPos = new HashMap<String, String>();
	public static Map<String, String> didVerbToPos = new HashMap<String, String>();
	public static Set<String> athletes = new HashSet<String>();
	public static Set<String> nationalities = new HashSet<String>();
	public static Set<String> genders = new HashSet<String>();
	public static Set<String> dets = new HashSet<String>();
	public static QueryGenerator gen = new QueryGenerator();
	public static WinterOlympicsDB db = new WinterOlympicsDB();
	public static final String whTemplate = "Who <whVerb> <rank> in <sport>?";
	public static final String nounTemplate = "Did <name> <didVerb> <rank> in <sport>?";
	public static final String genatTemplate = "Did <det> <nationality> <gender> <didVerb> <rank> in <sport>?";
	
	public static void queryDB() {
		Connection c = null;
	    Statement stmt = null;
	    try {
		      Class.forName("org.sqlite.JDBC");
		      c = DriverManager.getConnection("jdbc:sqlite:olympics-db-2014.db");
		      
		      stmt = c.createStatement();
		      ResultSet rs = stmt.executeQuery("select nationality from athletes");
		      
		      while(rs.next()) {
		    	  System.out.println(rs.getString("nationality"));
		      }
		      
		      stmt.close();
		      c.close();  
		      
		    } catch ( Exception e ) {
		      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
		      System.exit(0);
		    }
	}
	
	public static void generateTypeTrees() {
		/*
		 * Who won gold in the 10000 biathlon?
Who won gold in the nh skijumping?
Who won gold in the 1000 speedskating?
Who won gold in the 500 speedskating?
Who won gold in the 1500 shorttrack?
Who won gold in the lh skijumping?
Who won gold in the 7500 biathlon?
		 */
		
		sports.add("the 10000m biathlon");
		sports.add("the nh skijumping");
		sports.add("the 1000m speedskating");
		sports.add("the 500m speedskating");
		sports.add("the 1500m shorttrack");
		sports.add("the lh skijumping");
		sports.add("the 7500m biathlon");
		
		whVerbToPos.put("gold","won");
		//whVerbToPos.put("silver","won");
		//whVerbToPos.put("bronze","won");
		whVerbToPos.put("first","arrived");
		whVerbToPos.put("second","arrived");
		whVerbToPos.put("third","arrived");
		
		didVerbToPos.put("gold", "win");
		//didVerbToPos.put("silver", "win");
		//didVerbToPos.put("bronze", "win");
		didVerbToPos.put("first", "arrive");
		didVerbToPos.put("second", "arrive");
		didVerbToPos.put("third", "arrive");
		
		for(String sport : sports) {
			for(Map.Entry<String, String> vp : whVerbToPos.entrySet()) {
				String test = whTemplate;
				test = test.replaceAll("<sport>", sport);
				test = test.replaceAll("<whVerb>", vp.getValue());
				test = test.replaceAll("<rank>", vp.getKey());
				gen.generateQuery(test);//runTest(test);
			}
		}
		
		for(String sport : sports) {
			for(Map.Entry<String, String> vp : didVerbToPos.entrySet()) {
				String test = nounTemplate;
				test = test.replaceAll("<name>", "Luca");
				test = test.replaceAll("<sport>", sport);
				test = test.replaceAll("<didVerb>", vp.getValue());
				test = test.replaceAll("<rank>", vp.getKey());
				gen.generateQuery(test);//runTest(test);
			}
		}
		
		for(String sport : sports) {
			for(Map.Entry<String, String> vp : didVerbToPos.entrySet()) {
				String test = genatTemplate;
				test = test.replaceAll("<det>", "a");
				test = test.replaceAll("<nationality>", "Canadian");
				test = test.replaceAll("<gender>", "woman");
				test = test.replaceAll("<sport>", sport);
				test = test.replaceAll("<didVerb>", vp.getValue());
				test = test.replaceAll("<rank>", vp.getKey());
				gen.generateQuery(test);//runTest(test);
			}
		}
		
		System.out.println("Test was successful");
		
		
	}
	
	public static void runTest(String test) {
		System.out.println("\n\n Q: " + test);
		String query = gen.generateQuery(test);
		if(query.equals(QueryGenerator.errorMessage)) {
			System.exit(0);
		}
		System.out.println("A: " + query);
		String answer = db.executeQuery(query);
		if(answer.equals(WinterOlympicsDB.errorMessage)) {
			System.exit(0);
		}
		System.out.println(answer);
	}
	
	public static void standardTest() {
		
		//  DATA
		loadData();
		
		// templ. 1
		for(Map.Entry<String, String> vp : whVerbToPos.entrySet()) {
			for(String sport : sports) {
				String test = whTemplate;
				test = test.replaceAll("<whVerb>", vp.getValue());
				test = test.replaceAll("<rank>", vp.getKey());
				test = test.replaceAll("<sport>", sport);
				runTest(test);
			}
		}
		
		// templ. 2
		for(Map.Entry<String, String> vp : didVerbToPos.entrySet()) {
			for(String athlete : athletes) {
				for(String sport : sports) {
					String test = nounTemplate;
					test = test.replaceAll("<name>", athlete);
					test = test.replaceAll("<didVerb>", vp.getValue());
					test = test.replaceAll("<rank>", vp.getKey());
					test = test.replaceAll("<sport>", sport);
					runTest(test);
				}
			}
		}
		// Did <det> <nationality> <gender> <didVerb> <rank> in <sport>?
		for(Map.Entry<String, String> vp : didVerbToPos.entrySet()) {
			for(String gender : genders) {
				for(String nationality : nationalities) {
					for(String det : dets) {
						for(String sport : sports) {
							String test = genatTemplate;
							test = test.replaceAll("<didVerb>", vp.getValue());
							test = test.replaceAll("<rank>", vp.getKey());
							test = test.replaceAll("<gender>", gender);
							test = test.replaceAll("<nationality>", nationality);
							test = test.replaceAll("<det>", det);
							test = test.replaceAll("<sport>", sport);
							runTest(test);
						}
					}
				}
			}
		}
		
		System.out.println("Test was successful");
		
	}

	private static void loadData() {
		
		genders.add("man");
		genders.add("woman");
		
		dets.add("a");
		dets.add("an");
		
		whVerbToPos.put("gold","won");
		whVerbToPos.put("silver","won");
		whVerbToPos.put("bronze","won");
		whVerbToPos.put("first","arrived");
		whVerbToPos.put("second","arrived");
		whVerbToPos.put("third","arrived");
		
		didVerbToPos.put("gold", "win");
		didVerbToPos.put("silver", "win");
		didVerbToPos.put("bronze", "win");
		didVerbToPos.put("first", "arrive");
		didVerbToPos.put("second", "arrive");
		didVerbToPos.put("third", "arrive");
		
		Connection c = null;
	    Statement stmt = null;
	    try {
	      Class.forName("org.sqlite.JDBC");
	      c = DriverManager.getConnection("jdbc:sqlite:olympics-db-2014.db");
	      
	      stmt = c.createStatement();
	      ResultSet rs = stmt.executeQuery("select name from athletes");
	      
	      while(rs.next()) {
	    	  String name = rs.getString("name");
		      athletes.add(name.replaceFirst("" + name.charAt(0), ("" + name.charAt(0)).toUpperCase()));
	      }
	      
	      rs = stmt.executeQuery("select name from competitions");
	      
	      while(rs.next()) {
	    	  String name = rs.getString("name");
		      sports.add(name);
	      }
	      
	      stmt.close();
	      c.close();  
	      
	    } catch ( Exception e ) {
	      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	      System.exit(0);
	    }
	    
	    sports.add("the 10000 biathlon");
		sports.add("the nh skijumping");
		sports.add("the 1000 speedskating");
		sports.add("the 500 speedskating");
		sports.add("the 1500 shorttrack");
		sports.add("the lh skijumping");
		sports.add("the 7500 biathlon");
		
		sports.add("the 10000m biathlon");
		sports.add("the 1000m speedskating");
		sports.add("the 500m speedskating");
		sports.add("the 1500m shorttrack");
		sports.add("the 7500m biathlon");
		
		sports.add("10000 biathlon");
		sports.add("nh skijumping");
		sports.add("1000 speedskating");
		sports.add("500 speedskating");
		sports.add("1500 shorttrack");
		sports.add("lh skijumping");
		sports.add("7500 biathlon");
		
		sports.add("10000m biathlon");
		sports.add("1000m speedskating");
		sports.add("500m speedskating");
		sports.add("1500m shorttrack");
		sports.add("7500m biathlon");
	    
	    	    
		    BufferedReader buffer;
			try {
				buffer = new BufferedReader(new FileReader("nationality.csv"));
				String line = "";
			    while((line=buffer.readLine()) != null){
			  	  if(line.length()<2)
			  		  continue;
//			  	  System.out.println(line);
			  	  String[] items = line.split(",");
			  	  nationalities.add(items[1]);
			  	  nationalities.add(items[1].replaceFirst("" + items[1].charAt(0), ("" + items[1].charAt(0)).toUpperCase()));
			    }
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
	}
	
	public static void main(String[] args) {
		standardTest();
	}

}
