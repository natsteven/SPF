package edu.boisestate.cs.util;

import edu.boisestate.cs.automatonModel.Model_Acyclic_Inverse;
import edu.boisestate.cs.graph.SolutionSet;
import gov.nasa.jpf.symbc.numeric.*;
import gov.nasa.jpf.symbc.string.*;
import edu.boisestate.cs.util.PathConstraintAnalysis.ValidationResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Initial simple cache for integration with SPF
 * Cache of PCs to see if we can do simple automaton operations for results
 */

public class MASCache {
	HashMap<PathConstraintAnalysis, SolutionSet<Model_Acyclic_Inverse>> cache = new HashMap<>();
	final int MAX_CACHE_SIZE = 1024;

	public Tuple<SolutionSet<Model_Acyclic_Inverse>, Set<StringSymbolic>> findCacheHit(PathConstraintAnalysis pca) {
		if (cache.isEmpty()) {
			return null;
		}
		PathConstraintAnalysis match = null;
		Set<StringSymbolic> toComplement = null;
		Set<ValidationResult> badresults = new HashSet<>();

		System.out.println("SEARCHING in " + cache.size() + " entries");

		for (PathConstraintAnalysis oldPca : cache.keySet()) {
			// for now check equivalent, but could easily be a superset
			ValidationResult result = pca.equalsIgnoreNegationsValid(oldPca);
			if (result != null) {
				if (result.isValid()){
					//cache hit
					toComplement = result.getRelevantSymVars();
					match = oldPca;
					break;
				} else{
					// invalidated by some condition
					badresults.add(result);
				}
			}
		}
		// now we have a match for the path constraint, and the negated predicates (constraints)
		// we checked they are valid and provided variables involved
		if (match != null) {
			SolutionSet<Model_Acyclic_Inverse> solSet = cache.get(match);
			return new Tuple<>(solSet.clone(), toComplement);
		} else if (!badresults.isEmpty()){
			// for debugging, print why cache didnt hit
			System.out.println("Cache Hit Invalid reason(s):");
			for (ValidationResult vr : badresults){
				System.out.println("\t" + vr);
			}
			System.out.println("---------------------------------------------------------------------");
		}
		return null;
	}
//
//	// returns old pc and var involved in negation
//	public Tuple<StringPathCondition, String> findSingleNegation(StringPathCondition pc) {
//		if (cache.isEmpty()) {
//			return null;
//		}
//		for (StringPathCondition old : cache.keySet()) {
//			int contradictions = 0;
//			String var = null;
//			int varRefs = 0;
//			StringConstraint SC = pc.header;
//			StringConstraint oldSC = old.header;
//			// handle numerics
//			if (pc.getNpc().header != null) {
//				// happens when no string constriant but numeric constraint, i.e. charAt
//				// TODO: doesnt handle multiple numeric constraints...
//				if (old.getNpc().header != null) {
//					// shuold never not be: now check whether it contradicts
//					Constraint oldNpc = old.getNpc().header;
//					Constraint Npc = pc.getNpc().header;
//					Set<String> vars = findVars(Npc);
//					if (numericContradicts(oldNpc, Npc)) {
//						if (vars.size() == 1) {
//							var = vars.iterator().next();
//						} else {
//							// more than one var involved, cannot use cache
//							return null;
//						}
//						contradictions++;
//					}
//					if (vars.contains(var)) {
//						varRefs++;
//					}
//				} else {
//					return null;
//				}
//			}
//			//check for negation
//			if (SC != null && oldSC != null) {
//				Set<String> vars = findVars(SC);
//				if (oldSC.contradicts(SC)) {
//					if (contradictions == 0) { // set var ref if first contradiction
//						if (vars.size() == 1) {
//							var = vars.iterator().next();
//						} else {
//							// more than one var involved, cannot use cache
//							return null;
//						}
//					}
//					contradictions++;
//				}
//				if (vars.contains(var)) {
//					varRefs++;
//				}
//				// compare each SC in old and new SPC
//				while (oldSC.and() != null) {
//					oldSC = oldSC.and();
//					SC = pc.header;
//					while (SC.and() != null) {
//						SC = SC.and();
//						vars = findVars(SC);
//						if (oldSC.contradicts(SC)) {
//							// set var ref if first contradiction
//							if (contradictions == 0) {
//								if (vars.size() == 1) {
//									var = vars.iterator().next();
//								}
//							}
//							contradictions++;
//						}
//						if (vars.contains(var)) {
//							varRefs++;
//						}
//					}
//				}
//			} else if (SC != null || oldSC != null) {
//				return null;
//			}
//			// only return if exactly one negated predicate and related variable is not in other predicates
//			if (contradictions == 1 && varRefs == 1) {
//				return new Tuple<>(old, var);
//			}
//		}
//		lastHitWasCharAt = false;
//		return null;
//	}
//
//	private Set<String> findVars(StringConstraint sc) {
//		// get variable involved in contradiction
//		Set<String> vars = new HashSet<>();
//		Set<StringExpression> ops = sc.getOperands();
//		// TODO: should be recursive?
//		for (StringExpression se : ops) {
//			if (se instanceof StringSymbolic) {
//				vars.add(se.toString().replace("_SYMSTRING", ""));
//			} else if (se instanceof DerivedStringExpression) {
//				// recurse if needed
//				vars.addAll(findVars(se));
//			}
//		}
//		return vars;
//	}
//
//	private Set<String> findVars(StringExpression SE) {
//		Set<String> vars = new HashSet<>();
//		if (SE instanceof DerivedStringExpression) {
//			DerivedStringExpression dSE = (DerivedStringExpression) SE;
//			Set<Expression> ops = dSE.getOperands();
//			for (Expression e : ops) {
//				if (e instanceof StringSymbolic) {
//					vars.add(e.toString().replace("_SYMSTRING", ""));
//				} else if (e instanceof DerivedStringExpression) {
//					vars.addAll(findVars((StringExpression) e));
//				}
//			}
//		} else if (SE instanceof StringSymbolic) {
//			vars.add(SE.toString().replace("_SYMSTRING", ""));
//		} else {
//			System.out.println("Unhandled StringExpression type in findVars: " + SE.getClass().getName());
//			System.err.println("Unhandled var check");
//		}
//
//		return vars;
//	}
//
//	private Set<String> findVars(Constraint c) {
//		if (!(c instanceof LinearIntegerConstraint)) {
//			System.out.println("Unhandled Constraint type in findVars: " + c.getClass().getName());
//			System.err.println("Unhandled var check");
//			return new HashSet<>();
//		}
//		LinearIntegerConstraint lic = (LinearIntegerConstraint) c;
//		Set<String> vars = new HashSet<>();
//		IntegerExpression left = lic.getLeft();
//		IntegerExpression right = lic.getRight();
//		vars.addAll(findVars(left));
//		vars.addAll(findVars(right));
//		return vars;
//	}
//
//	private Set<String> findVars(IntegerExpression ie) {
//		Set<String> vars = new HashSet<>();
//		if (ie instanceof SymbolicCharAtInteger) {
//			lastHitWasCharAt = true;
//			SymbolicCharAtInteger sca = (SymbolicCharAtInteger) ie;
//			StringExpression se = sca.getExpression();
//			vars.addAll(findVars(se));
//		} else if (ie instanceof SymbolicInteger) {
//			SymbolicInteger si = (SymbolicInteger) ie;
//			vars.add(si.toString().replace("_SYMINT", ""));
//		} else if (ie instanceof IntegerConstant) {
//			// do nothing
//		} else {
//			System.out.println("Unhandled IntegerExpression type in findVars: " + ie.getClass().getName());
//			System.err.println("Unhandled var check");
//		}
//		return vars;
//	}

//	public SolutionSet<Model_Acyclic_Inverse> get(StringPathCondition pc) {
//		SolutionSet<Model_Acyclic_Inverse> sol = cache.get(pc);
//		return sol.clone();
//	}

	public void put(PathConstraintAnalysis pca, SolutionSet<Model_Acyclic_Inverse> solSet) {
		if (cache.size() >= MAX_CACHE_SIZE) {
			// TODO: better eviction policy
			cache.remove(cache.keySet().iterator().next());
		}
		cache.put(pca, solSet);
	}

//	public boolean numericContradicts(Constraint c1, Constraint c2) {
//		Comparator comp1 = c1.getComparator();
//		Comparator comp2 = c2.getComparator();
//		if (comp1.not().equals(comp2)) {
//			return c1.getLeft().equals(c2.getLeft()) && c1.getRight().equals(c2.getRight());
//		}
//		return false;
//	}
//
//	public boolean wasLastHitCharAt() {
//		boolean ret = lastHitWasCharAt;
//		lastHitWasCharAt = false;
//		return ret;
//	}

	public boolean isEmpty() {
		return cache.isEmpty();
	}
}
