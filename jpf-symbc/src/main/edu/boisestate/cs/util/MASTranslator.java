package edu.boisestate.cs.util;

import edu.boisestate.cs.graph.*;

import gov.nasa.jpf.symbc.string.StringConstraint;
import gov.nasa.jpf.symbc.string.StringPathCondition;
import gov.nasa.jpf.util.LogManager;
import org.jgrapht.DirectedGraph;

import java.util.List;
import java.util.logging.Logger;

public class MASTranslator {

    static Logger logger = LogManager.getLogger("TranslateToMAS");
    // keep track of constraints using id?
    private int id;

    public MASTranslator() {
        id = 0;
    }

    public DirectedGraph<PrintConstraint, SymbolicEdge> translate(StringPathCondition spc) {
        // this will be essentially what SolveMain.loadGraph() does in MAS

        //System.out.println("CLASSPATH: " + System.getProperty("java.class.path"));

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

                invGraph.addVertex(pc);

            }
            for (PrintConstraint pc : constraints) {
                if (pc.sourceConstraints.size() > 1) { //sourceconstraints include themselves though i suppose we don't need to do that here
                    for (PrintConstraint source : pc.sourceConstraints) {
                        if (source != pc){
                            invGraph.addEdge(source, pc);
                        }
                    }
                }
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
