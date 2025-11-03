package edu.boisestate.cs;

import edu.boisestate.cs.automatonModel.A_Model;
import edu.boisestate.cs.graph.InvDefaultDirectedGraph;
import edu.boisestate.cs.util.MASCache;
import edu.boisestate.cs.util.MASProcessor;
import edu.boisestate.cs.util.MASTranslator;
import edu.boisestate.cs.automatonModel.Model_Acyclic_Inverse;
import edu.boisestate.cs.graph.SolutionSet;
import edu.boisestate.cs.graph.SolutionSet.Solution;
import edu.boisestate.cs.util.Tuple;
import edu.ucsb.cs.vlab.translate.smtlib.from.z3str3.Z3Translator;
import gov.nasa.jpf.symbc.SymbolicInstructionFactory;
import gov.nasa.jpf.symbc.string.StringPathCondition;

import java.util.HashMap;

public class MASInterface {
	private static MASCache cache = new MASCache();

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

		if (!cache.isEmpty()){
			Tuple<StringPathCondition, String> hit = cache.findSingleNegation(pc);
			if (hit != null) {
				System.out.println("---------------------------------------\n---------------------------------------\n\t\t\tCACHE HIT\n---------------------------------------\n---------------------------------------");
				// for now we will check that the negated constraints related input has no other predicate dependencies...
				SolutionSet<Model_Acyclic_Inverse> sol = cache.get(hit.get1()).clone();
				String var = hit.get2();
				// so we can just take the complement model (assuming no partitioning inverses)
				// need to find the solution that matches the input we negate :D
				// could clone and put in new cache entry?
				Solution s = sol.getSolutionForVar(var);
				// change solution to complement
				A_Model tmp = s.model;
				s.model = s.comp;
				s.comp = tmp;

				// check complement model exists
				if (s.model.isEmpty()) {
					sol.setSAT(false);
				}

				s.example = s.model.getAcceptedStringExample();
				System.out.println(sol.getResult());

				// add new slightly different pc to cache.... TODO: should we do this?
				cache.put(pc, sol);

				return sol;
			}
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
		SolutionSet<Model_Acyclic_Inverse> sol = processor.query(graph);
		cache.put(pc, sol);

		return sol;
	}

}
