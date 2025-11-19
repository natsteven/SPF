package edu.boisestate.cs;

import edu.boisestate.cs.graph.InvDefaultDirectedGraph;
import edu.boisestate.cs.util.*;
import edu.boisestate.cs.automatonModel.Model_Acyclic_Inverse;
import edu.boisestate.cs.graph.SolutionSet;
import edu.boisestate.cs.graph.SolutionSet.Solution;
import edu.ucsb.cs.vlab.translate.smtlib.from.z3str3.Z3Translator;
import gov.nasa.jpf.symbc.SymbolicInstructionFactory;
import gov.nasa.jpf.symbc.string.StringPathCondition;

import java.util.HashMap;

public class MASInterface {
	private static final MASCache cache = new MASCache();
	private static int runCount = 0;
	private static int cacheHits = 0;
	private static HashMap<String, Integer> cacheMisses = new HashMap<>();

	public static SolutionSet<Model_Acyclic_Inverse> solve(StringPathCondition pc) {
		runCount++;

		printSMT(pc);

		PathConstraintAnalysis pca = new PathConstraintAnalysis(pc);
//		pca.printInfo();

		SolutionSet<Model_Acyclic_Inverse> sol = null;

		// so now we actually do both incremental and subset answers
		MASCache.CacheResult cacheHit = cache.get(pca);
		if (!cacheHit.isEmpty()) {
			cacheHits++;
			System.out.println();
			System.out.println("################################################");
			System.out.println("################################################");
			System.out.println("################	CACHE HIT	################");
			System.out.println("################################################");
			System.out.println("################################################");
			System.out.println();

			SolutionSet<Model_Acyclic_Inverse> solutionsFromCache = cacheHit.getSolutions();
			StringPathCondition toSolve = cacheHit.getRemainingSPC();

			System.out.println("****** Solutions from cache ******");
			for (Solution s : solutionsFromCache.getSolutions()) {
				System.out.println(s);
			}
			System.out.println("**********************************");

			if (toSolve.header == null && (toSolve.getNpc() == null || toSolve.getNpc().header == null)) { // cache had all solutions
				return solutionsFromCache;
			}

			System.out.println("Solving remaining predicates ...");
			// otherwise we solve for remaining
			printSMT(toSolve);

			SolutionSet<Model_Acyclic_Inverse> newSolutions = runPC(toSolve);
			// merge solutions

			sol = mergeSolutions(solutionsFromCache, newSolutions);

		} else {
			sol = runPC(pc);
		}

		cache.put(pca, sol);
		return sol;
	}


	private static SolutionSet<Model_Acyclic_Inverse> runPC(StringPathCondition pc) {

//		try {
		MASTranslator translator = new MASTranslator();
		InvDefaultDirectedGraph graph = (InvDefaultDirectedGraph) translator.translate(pc);
		if (graph == null) {
//				System.out.println("No graph was created from the StringPathCondition.");
//				return null;
			throw new RuntimeException("No graph was created from the StringPathCondition.");
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
		long startTime = System.currentTimeMillis();
		SolutionSet<Model_Acyclic_Inverse> result = processor.query(graph);
		long endTime = System.currentTimeMillis();
		System.out.println("*****************************");
		System.out.println("A-Str Solver Time (ms):" + (endTime - startTime));
		return processor.query(graph);
//		}
//		catch (Exception e) {
//			e.printStackTrace();
//			return null;
//		}
	}

	private static SolutionSet<Model_Acyclic_Inverse> mergeSolutions(SolutionSet<Model_Acyclic_Inverse> s1,
																	 SolutionSet<Model_Acyclic_Inverse> s2) {
		SolutionSet<Model_Acyclic_Inverse> merged = new SolutionSet<>(s1.getSolutions().size() + s2.getSolutions().size());
		for (Solution sol : s1.getSolutions()) {
			merged.add(sol.ID, sol.originalName, (Model_Acyclic_Inverse) sol.model);
		}
		for (Solution sol : s2.getSolutions()) {
			merged.add(sol.ID, sol.originalName, (Model_Acyclic_Inverse) sol.model);
		}
		return merged;
	}

	private static void printSMT(StringPathCondition pc) {
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
	}

}
