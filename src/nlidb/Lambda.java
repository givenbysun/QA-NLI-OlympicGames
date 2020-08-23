package nlidb;

/**
 * This class represents a Lambda function (in the meaning intended in Lambda calculus), as a template/functions, whose variables
 * can be binded one at the time. Whenever a variable is binded, the result is another Lambda function
 * e.g. consider L(z, y, x), then (L(z, y, x))(z) results in L'(y, x)
 * The first free variable is always the one to be binded 
 * @author Luca Graziani, Mehrdad Alizadeh
 *
 */
public class Lambda {

	// the variables of the Lambda, in order of binding z > y > 
	private String z = null;
	private String y = null;
	private String x = null;
	
	/**
	 * 
	 * @param var The value that will be assigned to the first free variable
	 */
	public void bind(String var) {
		
		// we assign var to the first free variable in the order z, y, x
		if(z == null) {
			z = var;
		} else if(y == null) {
			y = var;
		} else if(x == null) {
			x = var;
		}
	}
	
	/**
	 * 
	 * @return a String representation of the Lambda, representing the WHERE clause for the target SQL query
	 */
	public String buildWhereClause() {
		
		if(x == null || y == null || z == null) {
			// the representation is available only once all variables are binded
			return null;
		}
		// Variable y has the role of medal (2nd variable to be bound in QueryGenerator)
		String where = "R.medal = '" + y +"'";
		
		if(z.matches("<.+;.+>")) {
			// if z is of form <string;string> then it encodes the competition type and name
			// the two strings are extracted from z
			String[] fields = z.replaceAll("<|>", "").split(";");
			// the WHERE clause is extended with the competition name and type specification
			// if the type is like 1000m, the  'm' is removed since it is not present in the database
			where += " AND C.name = '" + fields[1] + "'" +
					" AND C.type = '" + (fields[0].matches("\\d+m") ? fields[0].substring(0, fields[0].length() - 1) : fields[0]) + "'";
		} else {
			// otherwise z is just a regular competition name
			where += " AND C.name = '" + z + "'";
		}
		if(x.isEmpty()) {
			// if x is "" it means we are answering a WH question, and therefore we have nothing else to add to the WHERE clause
			return where;
		}
		if(x.matches("<.+;.+>")) {
			// otherwise if x is on form <string;string>, then it encodes a nation and a gender
			// the two fields are extracted from x
			String[] fields = x.replaceAll("<|>", "").split(";");
			// the WHERE clause is extended with the athlete nationality and gender information
			return where + " AND A.nationality = '" + fields[0] + "' AND A.gender = '" + fields[1] + "'"; 
		} else {
			// x represents an athlete name
			return where + " AND R.winner = '" + x + "'";
		}
	}
	
}
