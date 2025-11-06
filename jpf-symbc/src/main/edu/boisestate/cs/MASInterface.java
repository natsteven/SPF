package edu.boisestate.cs;

import edu.boisestate.cs.automatonModel.A_Model;
import edu.boisestate.cs.graph.InvDefaultDirectedGraph;
import edu.boisestate.cs.util.*;
import edu.boisestate.cs.automatonModel.Model_Acyclic_Inverse;
import edu.boisestate.cs.graph.SolutionSet;
import edu.boisestate.cs.graph.SolutionSet.Solution;
import edu.ucsb.cs.vlab.translate.smtlib.from.z3str3.Z3Translator;
import gov.nasa.jpf.symbc.SymbolicInstructionFactory;
import gov.nasa.jpf.symbc.string.StringPathCondition;
import gov.nasa.jpf.symbc.string.StringSymbolic;

import java.util.HashMap;
import java.util.Set;

public class MASInterface {
	private static MASCache cache = new MASCache();
	private static int runCount = 0;
	private static int cacheHits = 0;
	private static HashMap<String, Integer> cacheMisses = new HashMap<>();

	public static SolutionSet<Model_Acyclic_Inverse> solve(StringPathCondition pc) {
		runCount++;
//		if (SymbolicInstructionFactory.debugMode) {
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
//		}

		PathConstraintAnalysis pca = new PathConstraintAnalysis(pc);
//		pca.printInfo();

		// currently check exact pca, wouldnt be hard to change to subset/superset
		Tuple<SolutionSet<Model_Acyclic_Inverse>, Set<StringSymbolic>> cacheHit = cache.findCacheHit(pca);
		if (cacheHit != null) {
			cacheHits++;
			System.out.println();
			System.out.println("################################################");
			System.out.println("################################################");
			System.out.println("################	CACHE HIT	################");
			System.out.println("################################################");
			System.out.println("################################################");
			System.out.println();
			SolutionSet<Model_Acyclic_Inverse> solSet = cacheHit.get1();
			Set<StringSymbolic> toComplement = cacheHit.get2();
			// complement solutions for each variable involved in negated constraints
			//todo: have method for getting solution given var in SolutionSet
			for (StringSymbolic symVar : toComplement) {
				for (Solution s : solSet.getSolutions()) {
					String n = s.originalName;
					String nm = symVar.getName().replace("_SYMSTRING", "");
					if (n.equals(nm)) {
						// complement model
						A_Model tmp = s.model;
						s.model = s.comp;
						s.comp = tmp;
						if (s.model.isEmpty()) {
							solSet.setSAT(false);
						}
						s.example = s.model.getAcceptedStringExample();
						break;
					}
				}
			}
			System.out.println(solSet.getResult());
			cache.put(pca, solSet);
			return solSet;
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
			System.out.println("Using Alphabet: " + alpha.getCharSetString());
			System.out.println("Using bound: " + bound);
		}
		System.out.println("*****************************");

		MASProcessor processor = new MASProcessor(false, alpha, bound);
		SolutionSet<Model_Acyclic_Inverse> sol = processor.query(graph);
		cache.put(pca, sol);

		return sol;
	}

}
