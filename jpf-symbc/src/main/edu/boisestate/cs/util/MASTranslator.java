package edu.boisestate.cs.util;

import edu.boisestate.cs.Alphabet;
import edu.boisestate.cs.graph.*;

import gov.nasa.jpf.symbc.string.StringConstraint;
import gov.nasa.jpf.symbc.string.StringPathCondition;
import gov.nasa.jpf.util.LogManager;
import org.jgrapht.DirectedGraph;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class MASTranslator {

    static Logger logger = LogManager.getLogger("TranslateToMAS");
    // keep track of constraints using id?
    private int id;
    // Alphabet is just String of characters that the Alphabet class can make into an alphabet object
    private Set<Character> alpha;
    private int longestConcreteStringLength = 0;

    public MASTranslator() {
        id = 0;
        alpha= new HashSet<>();
    }

    public DirectedGraph<PrintConstraint, SymbolicEdge> translate(StringPathCondition spc) {
        // this will be essentially what SolveMain.loadGraph() does in MAS

        //System.out.println("CLASSPATH: " + System.getProperty("java.class.path"));

        InvDefaultDirectedGraph invGraph = new InvDefaultDirectedGraph(SymbolicEdge.class);

        // string path condition object has place for count and solution....
        StringConstraint strc = spc.header;

        if (spc.getNpc().header != null) {
            String npc = spc.getNpc().header.toString();
            System.out.println("Numeric Path Condition Exists: " + npc);
            if (npc.equals("Length_0_ != CONST_0")) {
                System.out.println("isEmpty");
            }
        }

        if (strc == null) {
            System.out.println("No String Constraints");
//            System.exit(1);
            return null;
        }

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
                            int typ = source.getType();
                            if (typ == 0) {
                                edge.setType("t");
                            } else if (typ == 1) {
                                edge.setType("s1");
                            } else if (typ == 2) {
                                edge.setType("s2");
                            } else {
                                System.out.println("ERROR WITH TYPE " + typ + " FOR " + source);
                                System.exit(1);
                            }
                        }
                    }
                }
            }
            ct.clearConstraintsList();
            strc = strc.and();
        } while (strc != null);

        invGraph.computePredicateDependencies();

        return invGraph;
    }

    public int getNextID() {
        return id++;
    }

    public void addCharToAlph(char c) {
        alpha.add(c);
    }

    public void addWildCardToAlph() {
        for (int i = 48; i < 122; i++) {
            char currentSearch = (char) i;
            boolean found = false;
            for (Character c : alpha) {
                if (c == currentSearch) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                alpha.add(currentSearch);
                break;
            }
        }
    }

    public String getAlpha() {
        StringBuilder sb = new StringBuilder();
        for (Character c : alpha) {
            sb.append(c);
            sb.append(",");
        }
        return sb.deleteCharAt(sb.length()-1).toString();
    }

    public void setLongestConcreteStringLength(int longestConcreteStringLength) {
        this.longestConcreteStringLength = longestConcreteStringLength;
    }

    public int getLongestConcreteStringLength() {
        return longestConcreteStringLength;
    }
}
