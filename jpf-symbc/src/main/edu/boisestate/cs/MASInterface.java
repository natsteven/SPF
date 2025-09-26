package edu.boisestate.cs;

import edu.boisestate.cs.graph.InvDefaultDirectedGraph;
import edu.boisestate.cs.modelling.MASOutput;
import edu.boisestate.cs.util.MASProcessor;
import edu.boisestate.cs.util.MASTranslator;
import edu.boisestate.cs.automatonModel.Model_Acyclic_Inverse;
import edu.boisestate.cs.graph.SolutionSet;
import edu.boisestate.cs.util.NaiveIntegration;
import edu.ucsb.cs.vlab.translate.smtlib.from.z3str3.Z3Translator;
import gov.nasa.jpf.symbc.SymbolicInstructionFactory;
import gov.nasa.jpf.symbc.string.StringPathCondition;
import gov.nasa.jpf.util.LogManager;
import org.jgrapht.DirectedGraph;

import java.util.logging.Logger;

public class MASInterface {

	public static SolutionSet<Model_Acyclic_Inverse> solve(StringPathCondition pc) {

		if (SymbolicInstructionFactory.debugMode) {
			// get smtlib query for debugging
			final Z3Translator t = new Z3Translator();
			final String smtlibQuery = t.translate(pc);
			String[] lines = smtlibQuery.split("\n");

			System.out.print("The smtlib query is: ");
			int i = 0;
			while (i < lines.length - 2) { // remove (check-sat) and (get-model) lines
				System.out.println(lines[i++]);
			}
			System.out.println("=======================================");
		}

		MASTranslator translator = new MASTranslator();
		InvDefaultDirectedGraph graph = (InvDefaultDirectedGraph) translator.translate(pc);
		if (graph == null) {
			System.out.println("No graph was created from the StringPathCondition.");
			return null;
		}

		String alph = translator.getAlpha();
		Alphabet alpha;
		if (alph.isEmpty()) {
			alpha = new Alphabet("A,B,C");
		} else {
			// ugg that was dumb
			translator.addWildCardToAlph(); // add character not in queries concrete strings to alphabet
			alph = translator.getAlpha();
			alpha = new Alphabet(alph);
		}
		int bound = translator.getSuggestedBound();
		if (bound < 4) bound = 4;// could also reason about concats but for now this is fine
		// maybe the depth of the tree, i.e. we can reason about how long strings can/would be given the number of operations/type of ops

		if (SymbolicInstructionFactory.debugMode) {
			System.out.println("Using Alphabet: " + alpha.getCharSet());
			System.out.println("Using bound: " + bound);
		}
		System.out.println("*****************************");
		MASProcessor processor = new MASProcessor(false, alpha, bound);
		return processor.query(graph);
	}

}
