package nlidb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * This class gets access to Winter Olympics database 
 * The main functionality of this class is to execute the queries and return the results
 * 
 * @author Mehrdad Alizadeh
 */

public class WinterOlympicsDB {
	
	public static String errorMessage = "I cannot answer that question!";

	public WinterOlympicsDB() {
		// TODO Auto-generated constructor stub
	}
	
	// Main method for testing the class functions
	public static void main(String[] args){
		// A sample SQL query string
		String query = "SELECT count(*) FROM competitions C JOIN results R ON C.comp_id = R.comp_id WHERE null";
		WinterOlympicsDB wodb = new WinterOlympicsDB(); 
		// print the result of executed query
		System.out.println(wodb.executeQuery( query)); 
	}
	
	/**
	 * executeQuery method
	 * First it connects to local sqlite winter olympics database 
	 * Then it executes the SQL query 
	 * @param query: SQL query 
	 * @return the String that contains the results of the query execution 
	 */
	public String executeQuery( String query){
		Connection c = null;
	    Statement stmt = null;
	    String result = "";
	    
	    try {
	      Class.forName("org.sqlite.JDBC");
	      /*
	      // Get connection when calling database calling from JAR file
	      c = DriverManager.getConnection("jdbc:sqlite::resource:olympics-db-2014.db");
	      */
	      
	      // Get connection when calling database calling from IDE
	      c = DriverManager.getConnection("jdbc:sqlite:olympics-db-2014.db");
	      
	      stmt = c.createStatement();
	      ResultSet rs = stmt.executeQuery( query );
	      
	      // Check if query asks for the winner
	      boolean askedForWinner = false;	      
	      try
	      {
	    	  rs.findColumn("winner");
	    	  askedForWinner = true;
	      } catch (SQLException sqlex){}
	      
	      // Check if query asks whether a person win a specific competition 
	      boolean askedForCount = false; 
	      try
	      {
	    	  rs.findColumn("COUNT(*)");
	    	  askedForCount = true;
	      } catch (SQLException sqlex){}
	      
	      // if asked for winner of medal 
	      // Then set the result as the name of athlete(s) 
	      // winners from different competition then add the type in parenthesis 
	      if(askedForWinner){
	    	  String name = "";
	    	  String cType = "";
	    	  
	    	  if( rs.next() ){
	    		  name = rs.getString("winner");
	    		  cType = rs.getString("type");
	    		  
	    		  // The first character of a name is small letter in DB and should be changed to capital
	    		  name = name.replaceFirst("" + name.charAt(0), ("" + name.charAt(0)).toUpperCase());
	    		  
	    		  result = name;
	    		  
	    		  // if more than one winner for different types
	    		  // add the type of first winner later in While loop
	    		  boolean firstRecord = true;
	    		  
	    		  // While loops add other winners (if exist) to the result String 
	    		  while ( rs.next() ) {
	    			  
		    		  name = rs.getString("winner");
		    		  name = name.replaceFirst("" + name.charAt(0), ("" + name.charAt(0)).toUpperCase());
		    		  // No TYPE
		    		  if(cType == null) {
		    			  result +=  " and " + name ; 
		    		  } else if(cType.isEmpty()) {
		    			  result +=  " and " + name ;
		    		  } 
		    		  	// There is TYPE
		    		  	else {
		    			  if(firstRecord) { // add the TYPE of first winner
		    				  result += " (" + cType + ")";
		    				  firstRecord = false;
		    			  }
			    		  cType = rs.getString("type");
		    			  result += " and " + name + " (" + cType + ")" ;
		    		  }
		    	  }
	    	  }

	      }
	      // if there is a record of athlete with asked conditions
	      // Then YES 
	      // else NO
	      else if(askedForCount)
	    	  result = rs.getInt("COUNT(*)")>0 ? "Yes" : "No";
	      
	      
	      stmt.close();
	      c.close();  
	      
	    } catch(SQLException e){ 
	    	return errorMessage;
	    } catch ( Exception e ) {
	      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	      System.exit(0);
	    }
	    if(result.isEmpty()) {
	    	return errorMessage;
	    }
		return result;
	}

}
