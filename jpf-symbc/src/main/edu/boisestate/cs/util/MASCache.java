package edu.boisestate.cs.util;

import edu.boisestate.cs.automatonModel.Model_Acyclic_Inverse;
import edu.boisestate.cs.graph.SolutionSet;
import edu.boisestate.cs.graph.SolutionSet.Solution;
import gov.nasa.jpf.symbc.numeric.Constraint;
import gov.nasa.jpf.symbc.numeric.LinearIntegerConstraint;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.string.StringConstraint;
import gov.nasa.jpf.symbc.string.StringPathCondition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * now we are going to cache based on predicates, as handled by our PathConstraintAnalysis
 * basically when we query the cache we will get some number of solutions back, and some number of remaining predicates to solve
 * so we need to handle storing our predicate objects when they are valid (respectively independent symVar and pred)
 * and returning any remaing predicates to be solved (without the ands..)
 * so we just use ands to traverse from the original.
 *
 * in fact we store valid preds, and our pca itself can give us the set of valid or set of invalid preds
 * we query the cache on the valid preds and get back solutions for those preds, and any remaining preds to solve
 *
 * the invalid pred set from pca we know we need to solve, and the non-cached valid preds we also need to solve.
 * properly connecting these will be an issue essentially. I think because preds are based on the object that SPF gives
 * us we dont want to chagne that. instead we will take all our preds and clone them with our own links (ands)
 * then pass that to translation/solver.
 *
 * note then we need ot make sure we are outputting our solutions from MAS as well as the solutions from cache.
 */

public class MASCache {
	// note that this quickly breaks with related preds as pred equals pred doesn't include ands
	final HashMap<Object, Model_Acyclic_Inverse> cache = new HashMap<>();
	final int MAX_CACHE_SIZE = 1024;

	/**
	 * These needs to go through the predicates and find ones that are valid and in cache and return those solutions
	 * and also return the remaining predicates to be solved. IMPORTANT to note that it returns complement on predicates
	 * that are negations
	 * @param pca
	 * @return
	 */
	public CacheResult get(PathConstraintAnalysis pca) {
		CacheResult result = new CacheResult();
		if (cache.isEmpty() || pca.isEmpty()) {
			return result; // empty result
		}

		Set<Object> validPreds = pca.getValidPredicates();
		Set<Object> toSolve = new HashSet<>(pca.getAllPredicates());
		toSolve.removeAll(validPreds);
		// add back any that didnt get solutions from Cache

		for (Object pred : validPreds) {
			// search cache :/ cause for now we dont want ot adjust SPF clases too much
			// here we check for equivalence or contradiciton and add solutions accordingly
			String varName = pca.getSymVarNameForValidPred(pred);
			boolean foundInCache = false;
			for (Object cachedPred : cache.keySet()) {
				if (pred.equals(cachedPred)) {
					result.addSolution(varName, cache.get(pred));
					foundInCache = true;
					break;
				} else if (pred instanceof StringConstraint && cachedPred instanceof StringConstraint) {
					StringConstraint predSc = (StringConstraint) pred;
					StringConstraint cachedPredSC = (StringConstraint)  cachedPred;
					if (!predSc.contradicts(cachedPredSC)) {
						continue;
					}
				} else if (pred instanceof LinearIntegerConstraint && cachedPred instanceof LinearIntegerConstraint) {
					LinearIntegerConstraint predSc = (LinearIntegerConstraint) pred;
					LinearIntegerConstraint cachedPredSC = (LinearIntegerConstraint)  cachedPred;
					if (!predSc.contradicts(cachedPredSC)) {
						continue;
					}

				} else {
					continue;
				}
				// add complement as solution for contradicting predicates
				Model_Acyclic_Inverse complement = cache.get(cachedPred).complement();
				result.addSolution(varName, complement);
				foundInCache = true;
				break;
			}
			if (!foundInCache) {
				toSolve.add(pred);
			}
		}

		// generate new SPC for remaining preds with help from original SPC (trying to edit SPF as little as possible)
		StringPathCondition newSPC = createRemainingSPC(toSolve, pca.getSPC());

		result.setRemainingSPC(newSPC);
		result.setSolutionSet();
		return result;
	}

	public void put(PathConstraintAnalysis pca, SolutionSet<Model_Acyclic_Inverse> solSet) {
		if (cache.size() >= MAX_CACHE_SIZE) {
			// TODO: better eviction policy
			cache.remove(cache.keySet().iterator().next());
		}
		//given a pca, store valid variables and their solutions
		for (Object pred : pca.getValidPredicates()){
			String varName = pca.getSymVarNameForValidPred(pred);
			Solution sol = solSet.getSolutionForVar(varName);
			if (varName != null && sol != null)
				cache.put(pred, (Model_Acyclic_Inverse) sol.model);
//				Object negation;
//				if (pred instanceof StringConstraint) {
//					StringConstraint predSC = (StringConstraint) pred;
//					Object negation = predSC.getContradiction();
//				}
//				Model_Acyclic_Inverse model = (Model_Acyclic_Inverse) sol.model;
//				Model_Acyclic_Inverse complement = model.complement();
//				cache.put(negation, complement);
		}
	}

	private StringPathCondition createRemainingSPC(Set<Object> toSolve, StringPathCondition originalSPC) {
		StringPathCondition newSPC = new StringPathCondition(originalSPC.getNpc());
		if (originalSPC.header != null) {
			StringConstraint currSC = new StringConstraint(originalSPC.header);
			while (currSC != null && !toSolve.contains(currSC)) { // could have only numeric :)
				currSC = currSC.and();
			}
			newSPC.header = currSC;
			// now our header is set and necessary to solve
			for (; currSC != null; currSC = currSC.and()) {
				StringConstraint next = currSC.and();
				while (next != null && !toSolve.contains(next)) {
					next = next.and();
				}
				currSC.setAnd(next);
			}
		}
		//now need to do numeric constraints
		PathCondition npc = newSPC.getNpc();
		Constraint currC = npc == null ? null : npc.header;
		while (currC != null && !toSolve.contains(currC)) {
			currC = currC.and;
		}
		if (npc != null)
			npc.header = currC;
		for (; currC != null; currC = currC.and) {
			Constraint next = currC.and;
			while (next != null && !toSolve.contains(next)) {
				next = next.and;
			}
			currC.and = next;
		}

		return newSPC;
	}

	public boolean isEmpty() {
		return cache.isEmpty();
	}

	/*
	 * Result per query that provides solutions, if any, and remaining predicates to solve if any
	 */
	public class CacheResult {

		private final HashMap<String, Model_Acyclic_Inverse> solutions = new HashMap<>();
		private SolutionSet<Model_Acyclic_Inverse> solSet;
		private StringPathCondition remainingSPC;

		private void addSolution(String varName, Model_Acyclic_Inverse sol) {
			if (varName != null && sol != null)
				solutions.put(varName, sol);
		}

		public SolutionSet<Model_Acyclic_Inverse> getSolutions() {
			return solSet;
		}

		private void setSolutionSet() {
			solSet = new SolutionSet<>(solutions.size());
			int id=0;
			for (String varName : solutions.keySet()) {
				Model_Acyclic_Inverse model = solutions.get(varName);
				solSet.add(id++, varName, model);
			}
		}

		private void setRemainingSPC(StringPathCondition spc) {
			this.remainingSPC = spc;
		}

		public StringPathCondition getRemainingSPC() {
			return this.remainingSPC;
		}

		public boolean isEmpty() {
			return solutions.isEmpty();
		}
	}
}
