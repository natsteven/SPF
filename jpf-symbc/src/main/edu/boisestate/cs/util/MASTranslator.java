package edu.boisestate.cs.util;

import edu.boisestate.cs.graph.*;
import edu.ucsb.cs.vlab.translate.NormalFormTranslator;
import gov.nasa.jpf.symbc.string.StringComparator;
import gov.nasa.jpf.symbc.string.StringConstraint;
import gov.nasa.jpf.symbc.string.StringExpression;
import gov.nasa.jpf.symbc.string.StringPathCondition;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MASTranslator {

    // keep track of constraints using id?
    private int id;

    public MASTranslator() {
        id = 0;
    }

    public DirectedGraph<PrintConstraint, SymbolicEdge> translate(StringPathCondition spc) {
        // this will be essentially what SolveMain.loadGraph() does in MAS

        DirectedGraph<PrintConstraint, SymbolicEdge> graph = new DefaultDirectedGraph<>(SymbolicEdge.class);
        InvDefaultDirectedGraph invGraph = new InvDefaultDirectedGraph(SymbolicEdge.class);

        Map<Integer, PrintConstraint> constraintMap = new HashMap<>();
		Map<PrintConstraint, List<Integer>> sourceConstraintMap = new HashMap<>();
		List<Map<String, Object>> edgeData = new LinkedList<>();

        // alphabet and bounds. do we need an alphabet?
        int initialBound = 2;
        // string path condition object has place for count and solution....
        final StringConstraint strc = spc.header;

        // a string constraint has a comparator, a left, and a right
        // each of these become their own PrintConstraint
        // the left and righ tare string expressions we can be symbolic or constant

        ConstraintTranslator ct = ;
        List<PrintConstraint> constraints = ct.translate(strc);





        return null;
    }

    public String getMASConstraint(StringExpression l) {
        switch(l.toString()) {
            case "CONSTANT":
                return "CONSTANT";
            case "SYMBOLIC":
                return "SYMBOLIC";
            default:
                return "UNKNOWN";
        }
    }
}
