package edu.boisestate.cs.util;

import edu.boisestate.cs.graph.*;

import gov.nasa.jpf.symbc.string.StringConstraint;
import gov.nasa.jpf.symbc.string.StringPathCondition;
import org.jgrapht.DirectedGraph;

import java.util.List;

public class MASTranslator {

    // keep track of constraints using id?
    private int id;

    public MASTranslator() {
        id = 0;
    }

    public DirectedGraph<PrintConstraint, SymbolicEdge> translate(StringPathCondition spc) {
        // this will be essentially what SolveMain.loadGraph() does in MAS

        InvDefaultDirectedGraph invGraph = new InvDefaultDirectedGraph(SymbolicEdge.class);
        // alphabet and bounds. do we need an alphabet?
        int initialBound = 2;
        // string path condition object has place for count and solution....
        StringConstraint strc = spc.header;

        ConstraintTranslator ct = new ConstraintTranslator(this);
        // a string constraint has a comparator, a left, and a right
        // TODO: will need to handle cases of multiple arg constraints

        do {
            // takes a String Constraint and returns three PrintConstraints
            List<PrintConstraint> constraints = ct.translate(strc);

            for (PrintConstraint pc : constraints) {

                for (PrintConstraint source : pc.sourceConstraints){ // not source constraints are not used in MAS but we use them to hold incmonig edge data
                    invGraph.addEdge(source, pc);
                }
                invGraph.addVertex(pc);
            }
            strc = strc.and();
        } while (strc != null);

        invGraph.computePredicateDependencies();

        return invGraph;
    }

    public int getNextID() {
        return id++;
    }
}
