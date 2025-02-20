package edu.boisestate.cs.util;

import edu.boisestate.cs.Alphabet;
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

        // string path condition object has place for count and solution....
        StringConstraint strc = spc.header;

        ConstraintTranslator ct = new ConstraintTranslator(this);
        // a string constraint has a comparator, a left, and a right
        // TODO: will need to handle cases of multiple arg constraints

        // TODO: edge type issues
        // currenttly we set a type for the constraints, but this is inaccurate as it may have multiple edges with different types.
        // maybe just order??
        // def need to in future hold maps from SPF constraitn to MAS constraints
        // TODO: Constraints Map

        do {
            // takes a String Constraint and returns three PrintConstraints
            List<PrintConstraint> constraints = ct.translate(strc);

            for (PrintConstraint pc : constraints) {

                invGraph.addVertex(pc);

            }
            // currently this is 3 constraints in order left -> right -> comparator
            for (PrintConstraint pc : constraints) {
                if (pc.sourceConstraints.size() > 1) { //sourceconstraints include themselves though i suppose we don't need to do that here
                    for (PrintConstraint source : pc.sourceConstraints) {
                        if (source != pc){
                            SymbolicEdge edge = invGraph.addEdge(source, pc);
                            String type = source.getType() == 0 ? "t" : "s1";
                            edge.setType(type);
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
