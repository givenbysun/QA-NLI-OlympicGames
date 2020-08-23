package nlidb;

import java.util.*;
import java.io.*;

import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.parser.lexparser.*;
import edu.stanford.nlp.process.*;
import static nlidb.Utils.*;

/**
 * The purpose of this class is to parse an English question and to build the correspondent SQL query from the parse tree
 * @author Luca Graziani, Mehrdad Alizadeh
 *
 */
public class QueryGenerator {
	
	private String select;
	private String from;
	private String where;
	private Tree root = null;
	
	private static LexicalizedParser lp = LexicalizedParser.loadModel("lib/englishPCFG.ser.gz");
	private static TokenizerFactory<CoreLabel> tokenizerFactory = PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
	public static final String errorMessage = "Unrecognized syntactic structure"; // an error message returned in place of the query if generateQuery failed
	
	/**
	 * 
	 * @param question a natural language (english) question
	 * @return an SQL query representing the semantics of the question
	 */
	public String generateQuery(String question) {
		// clears the previous query
		reset();
		lp.setOptionFlags(new String[]{"-maxLength", "80", "-retainTmpSubcategories"});
		// tokenizes the input question
		List<CoreLabel> tokens = tokenizerFactory.getTokenizer(new StringReader(question)).tokenize();
		// parses the question
		root = lp.parseTree(tokens);
		// generates the SQL query query
		try{
			visitTree(root);
		} catch(NullPointerException e){
			return errorMessage;
		} catch(IndexOutOfBoundsException e){
			return errorMessage;
		}
		// concatenates the three clauses to obtain the SQL query
		return select + "\n" + from + "\n" + where+ "\n" ;
	}
	
	/**
	 * 
	 * @param node a node of the parse tree
	 * @return the String representing the semantics of the input node
	 */
	private String visitTree(Tree node) {
		if(isA(node, ROOT)) {
			if(node.children().length == 1) {
				// ROOT -> something
				visitTree(node.children()[0]);
			}
		} else if(isA(node, SQ)) {
			if(node.children().length == 4) {
				if(isA(node.children()[0], VBD) && isA(node.children()[1], NP) &&
						isA(node.children()[2], VP)) {
					// SQ -> VBD NP VP '?'
					// this is a 'Did' question, create the select clause accordingly
					select += "count(*)";
					// we build the semantics of VP by binding the Lambda accordingly
					Lambda lambda = buildLambda(node.children()[2]);
					// we bind the last Lambda variable with the athlete description
					lambda.bind(visitTree(node.children()[1]));
					// the where clause is obtained from the lambda
					where += lambda.buildWhereClause();
				}
			} else if(node.children().length == 1) {
				if(isA(node.children()[0], VP)) {
					// SQ -> VP
					// we build the semantics of VP by binding the Lambda accordingly
					Lambda lambda = buildLambda(node.children()[0]);
					// this is a WH question, there is no athlete description (empty binding)
					lambda.bind("");
					// the where clause is obtained from the lambda
					where += lambda.buildWhereClause();
				}
			}
		} else if(isA(node, S)) {
			if(node.children().length == 1) {
				if(isA(node.children()[0], VP)) {
					// S -> VP
					// the semantics of S are derived from the semantics of VP
					return visitTree(node.children()[0]);
				}
			}
			
		} else if(isA(node, SBARQ)) {
			if(node.children().length == 3) {
				if(isA(node.children()[0], WHNP) && isA(node.children()[1], SQ)){
					// SBARQ -> WHNP SQ ?
					// this is a WH - question, build the select clause accordingly (C.type needed for multiple competition types)
					select += "R.winner, C.type";
					// the from shape can be decided here
					from += "competitions C JOIN results R ON C.comp_id = R.comp_id";
					// build the rest of the query (WHERE) from the semantics of SQ
					visitTree(node.children()[1]);
				}
			}
		} else if(isA(node, VP)) {
			if(node.children().length == 1) {
				if(isA(node.children()[0], VBG)) {
					// VP -> VBG
					// the semantics of VP are those of VBG (this occurs with speedskating, skijumping,...)
					return visitTree(node.children()[0]);
				}
			}
		} else if(isA(node, NP)) {
			if(node.children().length == 1) {
				if(isA(node.children()[0], NN)) {
					// NP -> NN
					// the semantics of NP are derived from those of NN
					return visitTree(node.children()[0]);	
				} else if(isA(node.children()[0], NNP) || isA(node.children()[0], NNPS)) {
					// NP -> NNP
					// this is a 'Did' question and we are asking for an athlete name, build the from clause acordingly
					from += "competitions C JOIN results R ON C.comp_id = R.comp_id";
					// returns the semantics of NNP
					return visitTree(node.children()[0]);
				} else if(isA(node.children()[0], JJ)){
					// NP -> JJ
					// returns the semantics of JJ
					return visitTree(node.children()[0]);
				}
			} else if(node.children().length == 2) {
				if(isA(node.parent(root), PP)) {
					// NP -> NN NN | CD NN | JJ NN in the role of "<type> <competition>"
					// this is an NP of length two but, since the parent is a PP, we know this refers to a competition type
					// returns the semantics in the format "<type;competition>"
					return "<" + visitTree(node.children()[0]) + ";" + visitTree(node.children()[1]) + ">";
				}
			} else if(node.children().length == 3) {
				if(isA(node.children()[0], DT) && isA(node.children()[2], NN)) {
					if(isA(node.parent(root), PP)) {
						// NP -> DT CD NN | DT NN NN | DT JJ NN in the role of "the <type> <competition>"
						// same as before but here we also have a determiner (e.g. "the 500 skijumping"), the DT is ignored anyway
						return "<" + visitTree(node.children()[1]) + ";" + visitTree(node.children()[2]) + ">";
					} else {
						// NP -> DT JJ NN | DT NN NN | DT NNP NN in the role of "(a|an) <nationality> <gender>"
						// the parent is not a PP, hence we can assume this refers to an athlete's nationality and gender
						// we build the from clause accordingly (we have to join with athletes in this case)
						from += "competitions C JOIN results R ON C.comp_id = R.comp_id JOIN athletes A ON R.winner = A.name";
						// we return the semantics in the format "<nation, gender>",
						// getNation is used to map e.g. 'American' -> 'usa' etc., getGender maps 'man' -> 'M', 'woman' -> 'F'
						return "<" + getNation(visitTree(node.children()[1])) + ";" + getGender(visitTree(node.children()[2])) + ">";
					}
				}
			}
		} else if(isA(node, NN)) {
			// NN -> string
			// we return the semantics of the leaf (string)
			return leafValue(node);
			
		} else if(isA(node, NNP) || isA(node, NNPS)) {
			// NNP -> string, NNPS -> string
			return leafValue(node);
		} else if(isA(node, PP)){
			if(node.children().length == 2){
				if(isA(node.children()[0], IN) && isA(node.children()[1], NP))
					// PP -> IN NP
					// the semantics of the PP are built from the semantics of the NP
					return visitTree(node.children()[1]);				
				
				if(isA(node.children()[0], IN) && isA(node.children()[1], ADJP))
					// PP -> IN ADJP
					// the semantics of the PP are built from the semantics of the ADJP
					return visitTree(node.children()[1]);	
				if(isA(node.children()[0], IN) && isA(node.children()[1], S)) {
					// PP -> IN S
					// the semantics of the PP are built from the semantics of the S
					return visitTree(node.children()[1]);
				}
			}
		} else if(isA(node, ADJP)){
			// we return the semantics of the (only) child		
			return visitTree(node.children()[0]);
		} else if(isA(node, ADVP)) {
			// we return the semantics of the (only) child
			return visitTree(node.children()[0]);
		} else if(isA(node, VBG)){	
			// we return the semantics of the leaf (string)	
			return leafValue(node);
		} else if(isA(node, JJ)){	
			// we return the semantics of the leaf (string)	
			return leafValue(node);
		} else if(isA(node, RB)) {
			// we return the semantics of the leaf (string)
			return leafValue(node);
		} else if(isA(node, VB)) {
			// we return the semantics of the leaf (string)
			return leafValue(node);
		} else if(isA(node, VBD)) {
			// we return the semantics of the leaf (string)
			return leafValue(node);
		} else if(isA(node, CD)){
			// we return the semantics of the leaf (string)
			return leafValue(node);
		}
		// default case, if none of the above rules applied (or if no return was called)
		return null;
	}

	/**
	 * 
	 * @param node the subtree whose semantics are going to be binded in the Lambda
	 * @return the binded Lambda
	 */
	private Lambda buildLambda(Tree node) {
		if(isA(node, VP)) {
			if(node.children().length == 3) {
				if((isA(node.children()[0], VB) || isA(node.children()[0], VBD)) 
						&& (isA(node.children()[1], NP) || isA(node.children()[1], ADVP) || isA(node.children()[1], ADJP))
						&& isA(node.children()[2], PP)) {
					// we are in one of the following cases
					// VP -> VB NP PP | VBD NP PP
					// VP -> VB ADVP PP | VBD ADVP PP
					// VP -> VB ADJP PP | VBD ADJP PP
					// we build the Lambda from the verb information, the middle child (standing for medal/rank) and the PP (standing for the competition)
					Lambda lambda = bindVariables(node.children()[0], node.children()[1], node.children()[2]);
					return lambda;
				}
			}else if(node.children().length == 2) {
				if((isA(node.children()[0], VB) || isA(node.children()[0], VBD)) 
						&& (isA(node.children()[1], NP) || isA(node.children()[1], ADVP) || isA(node.children()[1], ADJP))) {
					// we are in one of the following cases
					// VP -> VB NP | VBD NP where NP -> NP PP
					// VP -> VB ADVP |VBD ADVP where ADVP -> RB PP 
					// VP -> VB ADJP | VBD ADJP where ADJP -> JJ PP
					// we build the Lambda from the verb information and  its sibling's children (medal/rank and competition respectively)
					Lambda lambda = bindVariables(node.children()[0], node.children()[1].children()[0], node.children()[1].children()[1]);
					return lambda;
				}
				
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param vb the Verb node
	 * @param nounPhrase the node acting as medal/rank
	 * @param prepPhrase the node acting as competition specification
	 * @return The Lambda obtained by binding the nounPhrase and the prepPhrase
	 */
	private Lambda bindVariables(Tree vb, Tree nounPhrase, Tree prepPhrase) {
		
		Lambda lambda = new Lambda();
		// we build the semantics of the prepPhrase (competition)
		String z = visitTree(prepPhrase);
		String y = null;
		// we build the semantics of the verb (string)
		String verb = visitTree(vb);
		if(verb.matches("arrive|arrived")) {
			// we convert the rank (obtained from the semantics of the nounPhrase) to the corresponding medal
			y = getMedal(visitTree(nounPhrase));
		} else if(verb.matches("win|won")) {
			// we obtain the semantics of the noun phrase (which will be a medal, since the verb was "to win")
			y = visitTree(nounPhrase);
		}
		// we bind the competition first
		lambda.bind(z);
		// then we bind the medal
		lambda.bind(y);
		// we return the resulting Lambda (var. x is still to be binded)
		return lambda;
	}

	/**
	 * resets select, from and where to the initial value
	 */
	private void reset() {
		select = "SELECT ";
		from = "FROM ";
		where = "WHERE ";		
	}
	
}
