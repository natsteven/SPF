package edu.boisestate.cs.util;

import gov.nasa.jpf.symbc.numeric.*;
import gov.nasa.jpf.symbc.string.*;

import java.util.*;

public class PathConstraintStats {
    
    private final Set<Object> predicates = new HashSet<>();
    private final Set<StringSymbolic> symVars = new HashSet<>();
    private final Map<StringOperator, Integer> operationCounts = new HashMap<>();
    
    private int totalOperations = 0;
    private int maxDepth = 0;
    private final Map<String, Integer> opsPerSymVar = new HashMap<>();

    public PathConstraintStats(StringPathCondition spc) {
        if (spc == null) return;

        try {
            // Walk string constraints
            for (StringConstraint sc = spc.header; sc != null; sc = sc.and()) {
                analyseStringConstraint(sc);
            }
            // Walk numeric constraints
            Constraint nc = (spc.getNpc() != null) ? spc.getNpc().header : null;
            while (nc != null) {
                analyseNumericConstraint(nc);
                nc = nc.and;
            }
        } catch (RuntimeException e) {
            System.err.println("Error collecting stats: " + e.getMessage());
        }
    }

    private void analyseStringConstraint(StringConstraint sc) {
        predicates.add(sc);

        if (sc.getComparator() == StringComparator.EMPTY || sc.getComparator() == StringComparator.NOTEMPTY) {
            recordOperation(StringOperator.ISEMPTY);
            // ISEMPTY applies to the root level, but we need to track it for its children
            Set<StringSymbolic> childVars = new HashSet<>();
            for (StringExpression se : sc.getOperands()) {
                childVars.addAll(analyseStringExpression(se, 1));
            }
            incrementOpsForVars(childVars);
        } else {
            for (StringExpression se : sc.getOperands()) {
                analyseStringExpression(se, 1);
            }
        }
    }

    private void analyseNumericConstraint(Constraint nc) {
        if (nc instanceof LinearIntegerConstraint) {
            LinearIntegerConstraint lic = (LinearIntegerConstraint) nc;
            predicates.add(lic);
            analyseIntegerExpression(lic.getLeft(), 1);
            analyseIntegerExpression(lic.getRight(), 1);
        }
    }

    // Returns the symbolic variables found in this subtree
    private Set<StringSymbolic> analyseStringExpression(StringExpression se, int currentDepth) {
        maxDepth = Math.max(maxDepth, currentDepth);
        Set<StringSymbolic> foundVars = new HashSet<>();

        if (se instanceof DerivedStringExpression) {
            DerivedStringExpression dse = (DerivedStringExpression) se;
            recordOperation(dse.op);
            
            for (Expression e : dse.getOperands()) {
                foundVars.addAll(analyseExpression(e, currentDepth + 1));
            }
            
            // This operation applies to all symbolic variables found in its operands
            incrementOpsForVars(foundVars);
            return foundVars;
        }

        if (se instanceof StringSymbolic) {
            StringSymbolic ss = (StringSymbolic) se;
            symVars.add(ss);
            foundVars.add(ss);
            opsPerSymVar.putIfAbsent(ss.getName(), 0); // Ensure it exists in the map
            return foundVars;
        }

        return foundVars; // Constants return empty sets
    }

    // Returns the symbolic variables found in this subtree
    private Set<StringSymbolic> analyseIntegerExpression(IntegerExpression ie, int currentDepth) {
        maxDepth = Math.max(maxDepth, currentDepth);
        Set<StringSymbolic> foundVars = new HashSet<>();

        if (ie instanceof IntegerConstant) return foundVars;

        if (ie instanceof SymbolicCharAtInteger) {
            recordOperation(StringOperator.CHARAT);
            foundVars.addAll(analyseStringExpression(((SymbolicCharAtInteger) ie).getExpression(), currentDepth + 1));
            incrementOpsForVars(foundVars);
            return foundVars;
        }
        
        if (ie instanceof SymbolicLengthInteger) {
            recordOperation(StringOperator.LENGTH);
            foundVars.addAll(analyseStringExpression(((SymbolicLengthInteger) ie).getExpression(), currentDepth + 1));
            incrementOpsForVars(foundVars);
            return foundVars;
        }
        
        if (ie instanceof SymbolicIndexOfInteger) {
            recordOperation(StringOperator.INDEXOF);
            SymbolicIndexOfInteger sio = (SymbolicIndexOfInteger) ie;
            foundVars.addAll(analyseStringExpression(sio.getSource(), currentDepth + 1));
            foundVars.addAll(analyseExpression(sio.getExpression(), currentDepth + 1));
            incrementOpsForVars(foundVars);
            return foundVars;
        }
        
        return foundVars;
    }

    private Set<StringSymbolic> analyseExpression(Expression e, int currentDepth) {
        if (e instanceof StringExpression) {
            return analyseStringExpression((StringExpression) e, currentDepth);
        } else if (e instanceof IntegerExpression) {
            return analyseIntegerExpression((IntegerExpression) e, currentDepth);
        }
        return new HashSet<>();
    }

    private void recordOperation(StringOperator op) {
        operationCounts.put(op, operationCounts.getOrDefault(op, 0) + 1);
        totalOperations++;
    }

    private void incrementOpsForVars(Set<StringSymbolic> vars) {
        for (StringSymbolic sv : vars) {
            opsPerSymVar.put(sv.getName(), opsPerSymVar.getOrDefault(sv.getName(), 0) + 1);
        }
    }

    // --- Getters & Printers ---

    public int getMaxDepth() { return maxDepth; }
    public Map<String, Integer> getOpsPerSymVar() { return opsPerSymVar; }

    public void printStats() {
        System.out.println("====== Path Constraint Stats ======");
        System.out.println("Total Predicates: " + predicates.size());
        System.out.println("Total Symbolic Vars: " + symVars.size());
        System.out.println("Max Depth: " + maxDepth);
        System.out.println("Total Operations: " + totalOperations);
        
        if (totalOperations > 0) {
            System.out.println("Operations Per Variable:");
            for (Map.Entry<String, Integer> entry : opsPerSymVar.entrySet()) {
                System.out.println("  - " + entry.getKey() + " -> " + entry.getValue() + " ops");
            }

            System.out.println("Operation Types:");
            for (Map.Entry<StringOperator, Integer> entry : operationCounts.entrySet()) {
                System.out.println("  - " + entry.getKey() + ": " + entry.getValue());
            }
        }
        System.out.println("===================================");
    }

    public void printStatsParseable() {
      StringBuilder sb = new StringBuilder();
      sb.append("PC_STATS:");
      sb.append(predicates.size()).append(":");
      sb.append(symVars.size()).append(":");
      sb.append(maxDepth).append(":");
      sb.append(totalOperations).append(":");
    if (totalOperations > 0) {
      for (Map.Entry<String, Integer> entry : opsPerSymVar.entrySet()) {
          sb.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
      }
      sb.deleteCharAt(sb.length() - 1);
      sb.append(":");
      for (Map.Entry<StringOperator, Integer> entry : operationCounts.entrySet()) {
          sb.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
      }
      sb.deleteCharAt(sb.length() - 1);
    }
      System.out.println(sb.toString());
    }
}
