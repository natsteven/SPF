package edu.boisestate.cs.util;

import gov.nasa.jpf.symbc.numeric.Constraint;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.LinearIntegerConstraint;
import gov.nasa.jpf.symbc.string.DerivedStringExpression;
import gov.nasa.jpf.symbc.string.StringConstraint;
import gov.nasa.jpf.symbc.string.StringConstant;
import gov.nasa.jpf.symbc.string.StringExpression;
import gov.nasa.jpf.symbc.string.StringOperator;
import gov.nasa.jpf.symbc.string.StringPathCondition;
import gov.nasa.jpf.symbc.string.StringSymbolic;
import gov.nasa.jpf.symbc.string.SymbolicCharAtInteger;
import gov.nasa.jpf.symbc.string.SymbolicIndexOfInteger;
import gov.nasa.jpf.symbc.string.SymbolicLengthInteger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class PathConstraintAnalysis {
	private final Set<Object> predicates; // each entry is a concrete constraint object (string or numeric)
	private final HashMap<String, Object> predicateStringToObject;

	private final HashMap<StringSymbolic, Set<Object>> symVarToPredicates;
	private final HashMap<Object, Set<StringSymbolic>> predicateToSymVars;
	private final HashMap<Tuple<StringSymbolic, Object>, Set<StringOperator>> symVarAndPredToOperations;

	private Object currentPredicate;
	private Set<StringSymbolic> currentSymVars;
	private final Deque<StringOperator> opStack;

	public PathConstraintAnalysis(StringPathCondition spc) {
		predicates = new HashSet<>();
		predicateStringToObject = new HashMap<>();
		symVarToPredicates = new HashMap<>();
		predicateToSymVars = new HashMap<>();
		symVarAndPredToOperations = new HashMap<>();
		opStack = new ArrayDeque<>();

		// Walk string constraints
		for (StringConstraint sc = spc.header; sc != null; sc = sc.and()) {
			analyseStringConstraint(sc);
		}

		// Walk numeric constraints (guard npc == null)
		Constraint nc = (spc.getNpc() != null) ? spc.getNpc().header : null;
		while (nc != null) {
			analyseNumericConstraint(nc);
			nc = nc.and;
		}
	}

	private void analyseStringConstraint(StringConstraint sc) {
		currentPredicate = sc;
		currentSymVars = new HashSet<>();
		opStack.clear();

		predicates.add(currentPredicate);
		predicateStringToObject.put(sc.toString(), currentPredicate);

		for (StringExpression se : sc.getOperands()) {
			analyseStringExpression(se);
		}

		predicateToSymVars.put(currentPredicate, currentSymVars);
	}

	private void analyseNumericConstraint(Constraint nc) {
		if (nc instanceof LinearIntegerConstraint) {
			LinearIntegerConstraint lic = (LinearIntegerConstraint) nc;
			currentPredicate = lic; // use the actual constraint object as key
			currentSymVars = new HashSet<>();
			opStack.clear();

			predicates.add(currentPredicate);
			predicateStringToObject.put(lic.toString(), currentPredicate);

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
			// push op
			opStack.push(dse.op);
			for (Expression e : dse.getOperands()) {
				analyseExpression(e);
			}
			// pop op
			opStack.pop();
			return;
		}

		if (se instanceof StringSymbolic) {
			StringSymbolic ss = (StringSymbolic) se;
			currentSymVars.add(ss);

			// update symVar -> predicates
			symVarToPredicates.computeIfAbsent(ss, k -> new HashSet<>()).add(currentPredicate);

			// capture current operation path as a set (order not required)
			Set<StringOperator> opsForPath = new HashSet<>(opStack);

			Tuple<StringSymbolic, Object> key = new Tuple<>(ss, currentPredicate);
			symVarAndPredToOperations
					.computeIfAbsent(key, k -> new HashSet<>())
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
		// String-dependent integer expressions
		if (ie instanceof SymbolicCharAtInteger) {
			analyseStringExpression(((SymbolicCharAtInteger) ie).getExpression());
			return;
		}
		if (ie instanceof SymbolicLengthInteger) {
			analyseStringExpression(((SymbolicLengthInteger) ie).getExpression());
			return;
		}
		if (ie instanceof SymbolicIndexOfInteger) {
			SymbolicIndexOfInteger sio = (SymbolicIndexOfInteger) ie;
			// source string
			analyseStringExpression(sio.getSource());
			// argument can be string expr or constant
			Expression arg = sio.getExpression();
			analyseExpression(arg);
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

	// Optional getters
	public Set<Object> getPredicates() {
		return predicates;
	}

	public HashMap<Object, Set<StringSymbolic>> getPredicateToSymVars() {
		return predicateToSymVars;
	}

	public HashMap<StringSymbolic, Set<Object>> getSymVarToPredicates() {
		return symVarToPredicates;
	}

	public HashMap<Tuple<StringSymbolic, Object>, Set<StringOperator>> getSymVarAndPredToOperations() {
		return symVarAndPredToOperations;
	}
}