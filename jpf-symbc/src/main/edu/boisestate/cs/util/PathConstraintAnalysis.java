package edu.boisestate.cs.util;

import gov.nasa.jpf.symbc.numeric.*;
import gov.nasa.jpf.symbc.string.*;

import java.util.*;

public class PathConstraintAnalysis {
	private final Set<Object> predicates = new HashSet<>();
	private final Set<StringSymbolic> symVars = new HashSet<>();
	private final Set<StringOperator> operations = new HashSet<>();
	private final Set<StringOperator> badOps = new HashSet<>();
	private final StringPathCondition spc;

	private final HashMap<StringSymbolic, Set<Object>> symVarToPredicates = new HashMap<>();
	private final HashMap<Object, Set<StringSymbolic>> predicateToSymVars = new HashMap<>();
	private final HashMap<Tuple<StringSymbolic, Object>, Set<StringOperator>> symVarAndPredToOperations = new HashMap<>();

	private Object currentPredicate;
	private Set<StringSymbolic> currentSymVars;
	private final Deque<StringOperator> opStack = new ArrayDeque<>();

	public PathConstraintAnalysis(StringPathCondition spc) {
		this.spc = spc;
		// Walk string constraints
		try {
			for (StringConstraint sc = spc.header; sc != null; sc = sc.and()) {
				analyseStringConstraint(sc);
			}
			// Walk numeric constraints (guard npc == null)
			Constraint nc = (spc.getNpc() != null) ? spc.getNpc().header : null;
			while (nc != null) {
				analyseNumericConstraint(nc);
				nc = nc.and;
			}
		} catch (RuntimeException e) {
			System.err.println("Error during PathConstraintAnalysis: " + e.getMessage());
		}
		badOps.add(StringOperator.DELETE);
		badOps.add(StringOperator.SUBSTRING);
		badOps.add(StringOperator.INSERT);
		badOps.add(StringOperator.CHARAT);
		badOps.add(StringOperator.LENGTH);
	}

	// check pred is valid and provides info for validation result
	public void validate(Object predicate, ValidationResult result) {
		// we are gathering info for invalid cases
		if (predicate instanceof StringConstraint &&
				(((StringConstraint) predicate).getComparator() == StringComparator.EMPTY ||
						((StringConstraint) predicate).getComparator() == StringComparator.NOTEMPTY)) {
			result.addBadOp(StringOperator.ISEMPTY);
			result.setValid(false);
		}
		Set<StringSymbolic> symVars = predicateToSymVars.get(predicate);

		// if pred->sym map doesnt have key then no sym vars so will just be sat<->unsat flip
		if (symVars == null || symVars.isEmpty()) return;
		if (symVars.size() != 1) {
			result.addMultiSymPred(predicate);
			result.setValid(false);
		}

		for (StringSymbolic symVar : symVars) {
			if (symVarToPredicates.get(symVar).size() != 1) {
				result.addDependentSymVar(symVar);
				result.setValid(false);
			}
			Set<StringOperator> ops = symVarAndPredToOperations.get(new Tuple<>(symVar, predicate));
			for (StringOperator op : badOps) {
				if (ops.contains(op)) {
					result.addBadOp(op);
					result.setValid(false);
				}
			}
		}

		if (result.isValid()) {
			result.addRelevantSymVars(symVars);
		}
	}

	// note this would work when old is superset of new, but for now we require same and return contradictions
	// note other is the cached one
	public ValidationResult equalsIgnoreNegationsValid(PathConstraintAnalysis other) {
		// we check that predicates contradict or are the same
		ValidationResult result = new ValidationResult();

		// proper subset allowing negations/contradictions
		if (this.isSubSetOf(other)) {
			System.out.println("is PROPER SUBSET: " + this.predicates.size() + " / " + other.predicates.size());
//			result.setValid(false);
//			result.setSummary("SUBSET: " + this.predicates.size() + " / " + other.predicates.size());
			return result;
		}
		if (this.isSuperSetOf(other)) {
			System.out.println("is PROPER SUPERSET: " + this.predicates.size() + " / " + other.predicates.size());
//			result.setValid(false);
//			result.setSummary("SUPERSET: " + this.predicates.size() + " / " + other.predicates.size());
			return result;
		}

		if (this.predicates.size() != other.predicates.size()) {
			// sizes differ
			return null;
		}
		// this and above check could be removed for subset/superset checks
		if (!this.symVars.equals(other.symVars)) {
//			result.summaryAdd("Symbolic variable sets differ: " + this.symVars.size() + " vs cached " + other.symVars.size());
//			result.setValid(false);
			return null;
		}
//		if (!result.isValid()) {
//			return result;
//		}
		Set<Object> toFind = new HashSet<>(this.predicates);
		Set<Object> toSearch = new HashSet<>(other.getAllPredicates());


		for (Iterator findIt = toFind.iterator(); findIt.hasNext(); ) {
			Object pred = findIt.next();
			boolean handled = false;
			for (Iterator searchIt = toSearch.iterator(); searchIt.hasNext(); ) {
				Object otherPred = searchIt.next();
				// exact match
				if (pred.equals(otherPred)) {
					searchIt.remove();     // remove from toSearch
					findIt.remove();       // remove from toFind
					handled = true;
					break;                 // done with this pred
				}
				// contradiction?
				boolean isContradiction = false;
				if (pred instanceof StringConstraint && otherPred instanceof StringConstraint) {
					isContradiction = ((StringConstraint) pred).contradicts((StringConstraint) otherPred);
				} else if (pred instanceof LinearIntegerConstraint && otherPred instanceof LinearIntegerConstraint) {
					isContradiction = ((Constraint) pred).contradicts((Constraint) otherPred);
				}
				if (isContradiction) {
					validate(pred, result); // validate and gather info for invalid case
					searchIt.remove();     // remove matched counterpart
					findIt.remove();       // remove current pred
					handled = true;
					break;
				}
				// else continue scanning other elements of toSearch
			}
			if (!handled) {
				// no equal or contradictory counterpart found for this pred
				return null;
			}
		}
		// search over
		assert (toFind.isEmpty() && toSearch.isEmpty());
		// yay its a similar spc
		result.setSummary();
		return result; // may be invalid but has info
	}

	// check if this is proper superset of other
	private boolean isSuperSetOf(PathConstraintAnalysis other) {
		if (!this.symVars.containsAll(other.symVars)) {
			return false;
		}
		if (this.predicates.size() <= other.getAllPredicates().size()) {
			return false;
		}
		// same logic as equalsIgnoreNegationsValid
		// checking to see how big the superset is basically
		Set<Object> toFind = new HashSet<>(this.predicates);
		Set<Object> toSearch = new HashSet<>(other.getAllPredicates());
		for (Iterator<Object> findIt = toFind.iterator(); findIt.hasNext(); ) {
			Object pred = findIt.next();
			for (Iterator<Object> searchIt = toSearch.iterator(); searchIt.hasNext(); ) {
				Object otherPred = searchIt.next();
				if (pred.equals(otherPred)) {
					searchIt.remove();     // remove from toSearch
					findIt.remove();       // remove from toFind
					break;                 // done with this pred
				}
				boolean isContradiction = false;
				if (pred instanceof StringConstraint && otherPred instanceof StringConstraint) {
					isContradiction = ((StringConstraint) pred).contradicts((StringConstraint) otherPred);
				} else if (pred instanceof LinearIntegerConstraint && otherPred instanceof LinearIntegerConstraint) {
					isContradiction = ((Constraint) pred).contradicts((Constraint) otherPred);
				}
				if (isContradiction) {
					searchIt.remove();     // remove matched counterpart
					findIt.remove();       // remove current pred
					break;
				}
				// else continue scanning other elements of toSearch
			}
		}
		assert (!toFind.isEmpty());
		assert (toSearch.isEmpty());
		// doesnt check validity of preds
		return true;
	}


	// check if this is proper subset of other
	private boolean isSubSetOf(PathConstraintAnalysis other) {
		if (!other.symVars.containsAll(this.symVars)) {
			return false;
		}
		if (this.predicates.size() >= other.getAllPredicates().size()) {
			return false;
		}
		// same logic as equalsIgnoreNegationsValid
		// reason its releveant is because using a superset to solve the subset is likely underapproximating
		Set<Object> toFind = new HashSet<>(this.predicates);
		Set<Object> toSearch = new HashSet<>(other.getAllPredicates());
		for (Iterator<Object> findIt = toFind.iterator(); findIt.hasNext(); ) {
			Object pred = findIt.next();
			boolean handled = false;
			for (Iterator<Object> searchIt = toSearch.iterator(); searchIt.hasNext(); ) {
				Object otherPred = searchIt.next();
				// exact match
				if (pred.equals(otherPred)) {
					searchIt.remove();     // remove from toSearch
					findIt.remove();       // remove from toFind
					handled = true;
					break;                 // done with this pred
				}
				// contradiction?
				boolean isContradiction = false;
				if (pred instanceof StringConstraint && otherPred instanceof StringConstraint) {
					isContradiction = ((StringConstraint) pred).contradicts((StringConstraint) otherPred);
				} else if (pred instanceof LinearIntegerConstraint && otherPred instanceof LinearIntegerConstraint) {
					isContradiction = ((Constraint) pred).contradicts((Constraint) otherPred);
				}
				if (isContradiction) {
					searchIt.remove();     // remove matched counterpart
					findIt.remove();       // remove current pred
					handled = true;
					break;
				}
				// else continue scanning other elements of toSearch
			}
			if (!handled) {
				// no equal or contradictory counterpart found for this pred
				return false;
			}
		}
		assert (toFind.isEmpty());
		assert (!toSearch.isEmpty());
		// doesnt check validity of preds
		return true;
	}

	private Set<StringOperator> findBadOps(Object pred) {
		Set<StringOperator> ops = new HashSet<>();
		Set<StringOperator> badOpsFound = new HashSet<>();
		for (StringSymbolic sv : predicateToSymVars.get(pred)) {
			Tuple<StringSymbolic, Object> key = new Tuple<>(sv, pred);
			ops.addAll(symVarAndPredToOperations.get(key));
		}
		for (StringOperator op : badOps) {
			if (ops.contains(op)) {
				badOpsFound.add(op);
			}
		}
		return badOpsFound;
	}

	private void analyseStringConstraint(StringConstraint sc) {
		currentPredicate = sc;
		currentSymVars = new HashSet<>();
		opStack.clear();

		predicates.add(currentPredicate);

		for (StringExpression se : sc.getOperands()) {
			analyseStringExpression(se);
		}

		predicateToSymVars.put(currentPredicate, currentSymVars);
	}

	private void analyseNumericConstraint(Constraint nc) {
		if (nc instanceof LinearIntegerConstraint) {
			LinearIntegerConstraint lic = (LinearIntegerConstraint) nc;
			currentPredicate = lic;
			currentSymVars = new HashSet<>();
			opStack.clear();

			predicates.add(currentPredicate);

			analyseIntegerExpression(lic.getLeft());
			analyseIntegerExpression(lic.getRight());

			predicateToSymVars.put(currentPredicate, currentSymVars);
			return;
		}

		throw new RuntimeException("Unhandled numeric constraint type: " + nc.toString());
	}

	private void analyseStringExpression(StringExpression se) {
		if (se instanceof DerivedStringExpression) {
			DerivedStringExpression dse = (DerivedStringExpression) se;
			StringOperator operator = dse.op;
			operations.add(operator);
			opStack.push(operator);
			for (Expression e : dse.getOperands()) {
				analyseExpression(e);
			}
			opStack.pop();
			return;
		}

		if (se instanceof StringSymbolic) {
			// TODO: make sure this object is shared for other references
			StringSymbolic ss = (StringSymbolic) se;
			currentSymVars.add(ss);
			symVars.add(ss);

			symVarToPredicates.computeIfAbsent(ss, k -> new HashSet<>()).add(currentPredicate);

			Set<StringOperator> opsForPath = new HashSet<>(opStack);

			Tuple<StringSymbolic, Object> key = new Tuple<>(ss, currentPredicate);
			symVarAndPredToOperations
					.computeIfAbsent(key, k -> new HashSet<>()) // in case of multiple paths from sym var to predicate
					.addAll(opsForPath);
			return;
		}

		if (se instanceof StringConstant) {
			return;
		}

		throw new RuntimeException("Unhandled StringExpression type: " + se.toString());
	}

	private void analyseIntegerExpression(IntegerExpression ie) {
		if (ie instanceof IntegerConstant) {
			return;
		}
		if (ie instanceof SymbolicCharAtInteger) {
			opStack.push(StringOperator.CHARAT);
			operations.add(StringOperator.CHARAT);
			analyseStringExpression(((SymbolicCharAtInteger) ie).getExpression());
			opStack.pop();
			return;
		}
		if (ie instanceof SymbolicLengthInteger) {
			opStack.push(StringOperator.LENGTH);
			operations.add(StringOperator.LENGTH);
			analyseStringExpression(((SymbolicLengthInteger) ie).getExpression());
			opStack.pop();
			return;
		}
		if (ie instanceof SymbolicIndexOfInteger) {
			opStack.push(StringOperator.INDEXOF);
			operations.add(StringOperator.INDEXOF);
			SymbolicIndexOfInteger sio = (SymbolicIndexOfInteger) ie;
			analyseStringExpression(sio.getSource());
			analyseExpression(sio.getExpression()); //arg
			opStack.pop();
			return;
		}
		throw new RuntimeException("Unhandled IntegerExpression type: " + ie.toString());
	}

	private void analyseExpression(Expression e) {
		if (e instanceof StringExpression) {
			analyseStringExpression((StringExpression) e);
			return;
		}
		if (e instanceof IntegerExpression) {
			analyseIntegerExpression((IntegerExpression) e);
			return;
		}
		throw new RuntimeException("unexpected expression type: " + e.toString());
	}

	public Set<Object> getAllPredicates() {
		return predicates;
	}

	public void printInfo() {
		System.out.println("Path Constraint Analysis Info:");
		System.out.println("	Predicates: " + predicates.size());
		for (Object p : predicateToSymVars.keySet()) {
			System.out.println("		Pred: " + p.toString() + " involves " + predicateToSymVars.get(p).size() + " symbolic variables.");
		}
		System.out.println("	Symbolic Variables: " + symVars.size());
		for (StringSymbolic sv : symVarToPredicates.keySet()) {
			System.out.println("		SymVar: " + sv.getName() + " involved in " + symVarToPredicates.get(sv).size() + " predicates.");
		}
		for (StringOperator op : operations) {
			System.out.println("	Operations involved: " + op.toString());
		}
	}

	public static class ValidationResult {
		private boolean valid;
		private final Set<StringSymbolic> relevantSymVars;
		private final Set<StringSymbolic> problemSymVars;
		private final Set<Object> badPredicates;
		private final Set<StringOperator> badOps;
		private String summary;

		public ValidationResult() {
			this.valid = true;
			this.relevantSymVars = new HashSet<>();
			this.problemSymVars = new HashSet<>();
			this.badPredicates = new HashSet<>();
			this.badOps = new HashSet<>();
			summary = "";
		}

		public void setValid(boolean valid) {
			this.valid = valid;
		}

		public boolean isValid() {
			return valid;
		}

		public Set<StringSymbolic> getRelevantSymVars() {
			return relevantSymVars;
		}

		public Set<StringSymbolic> getProblemSymVars() {
			return problemSymVars;
		}

		public Set<Object> getBadPredicates() {
			return badPredicates;
		}

		public Set<StringOperator> getBadOps() {
			return badOps;
		}

		public void addBadOp(StringOperator op) {

			badOps.add(op);
		}

		public void addMultiSymPred(Object pred) {
			badPredicates.add(pred);
		}

		public void addDependentSymVar(StringSymbolic symVar) {
			problemSymVars.add(symVar);
		}

		public void addRelevantSymVars(Set<StringSymbolic> symVars) {
			relevantSymVars.addAll(symVars);
		}

		public void setSummary(String summary) {
			this.summary = summary;
		}

		private void setSummary() {
			StringBuilder sb = new StringBuilder(summary);
			if (valid) {
				sb.append("VALID, involving ").append(relevantSymVars.size()).append(" symbolic variables.");
			} else {
				sb.insert(0,"INVALID due to: ");
				if (!problemSymVars.isEmpty()) {
					sb.append("\n\t\t").append(problemSymVars.size()).append(" dependent symbolic variables");
				}
				if (!badPredicates.isEmpty()) {
					sb.append("\n\t\t").append(badPredicates.size()).append(" multi-sym predicates");
				}
				if (!badOps.isEmpty()) {
					sb.append("\n\t\t").append(badOps.size()).append(" bad operations");
				}
			}
			summary = sb.toString();
		}

		@Override
		public String toString() {
			return summary;
		}
	}
}