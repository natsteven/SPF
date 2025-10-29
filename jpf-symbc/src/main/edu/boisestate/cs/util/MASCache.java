package edu.boisestate.cs.util;

import edu.boisestate.cs.automatonModel.Model_Acyclic_Inverse;
import edu.boisestate.cs.graph.SolutionSet;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.string.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Initial simple cache for integration with SPF
 * Cache of PCs to see if we can do simple automaton operations for results
 */

public class MASCache {
	HashMap<StringPathCondition, SolutionSet<Model_Acyclic_Inverse>> cache = new HashMap<>();
	final int MAX_CACHE_SIZE = 1024;

	// starting simple, check whether the pc is equals except for one negated comparator
	// returns old pc and var involved in negation
	public Tuple<StringPathCondition, String> findSingleNegation(StringPathCondition pc) {
		if (cache.isEmpty()) {
			return null;
		}
		for (StringPathCondition old : cache.keySet()) {
			int contradictions = 0;
			String var = null;
			int varRefs = 0;
			StringConstraint SC = pc.header;
			StringConstraint oldSC = old.header;
			if (SC == null)
				System.out.println("SC is null");
			//check for negation
			if (oldSC.contradicts(SC)) {
				// set var ref if only one variable involved
				Set<String> vars = findVars(SC);
				if (vars.size() == 1) {
					var = vars.iterator().next();
					varRefs++;
				}
				contradictions++;
			}
			// compare each SC in old and new SPC
			while (oldSC.and() != null) {
				oldSC = oldSC.and();
				SC = pc.header;
				while (SC.and() != null) {
					SC = SC.and();
					if (oldSC.contradicts(SC)) {
						// set var ref if first contradiction
						if (contradictions == 0) {
							Set<String> vars = findVars(SC);
							if (vars.size() == 1) {
								var = vars.iterator().next();
								varRefs++;
							}
						}
						contradictions++;
					}
					Set<String> vars = findVars(SC);
					if (vars.contains(var)) {
						varRefs++;
					}
				}
			}
			// only return if exactly one negated predicate and related variable is not in other predicates
			if (contradictions == 1 && varRefs == 1) {
				return new Tuple<>(old, var);
			}
		}
		return null;
	}

	private Set<String> findVars(StringConstraint sc) {
		// get variable involved in contradiction
		Set<String> vars = new HashSet<>();
		Set<StringExpression> ops = sc.getOperands();
		// TODO: should be recursive?
		for (StringExpression se : ops) {
			if (se instanceof StringSymbolic) {
				vars.add(se.toString().replace("_SYMSTRING", ""));
			} else if(se instanceof DerivedStringExpression) {
				// recurse if needed
				vars.addAll(findVars(se));
			}
		}
		return vars;
	}

	private Set<String> findVars(StringExpression SE){
		DerivedStringExpression dSE = (DerivedStringExpression) SE;
		Set<String> vars = new HashSet<>();
		Set<Expression> ops = dSE.getOperands();
		for (Expression e : ops) {
			if (e instanceof StringSymbolic) {
				vars.add(e.toString().replace("_SYMSTRING", ""));
			} else if (e instanceof DerivedStringExpression) {
				vars.addAll(findVars((StringExpression) e));
			}
		}
		return vars;
	}

	public SolutionSet<Model_Acyclic_Inverse> get(StringPathCondition pc) {
		SolutionSet<Model_Acyclic_Inverse> sol = cache.get(pc);
		return sol.clone();
	}

	public void put(StringPathCondition pc, SolutionSet<Model_Acyclic_Inverse> solSet) {
		if (cache.size() >= MAX_CACHE_SIZE) {
			// TODO: better eviction policy
			cache.remove(cache.keySet().iterator().next());
		}
		cache.put(pc, solSet);
	}

	public boolean isEmpty() {
		return cache.isEmpty();
	}
}
