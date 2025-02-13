package edu.boisestate.cs.util;

import edu.boisestate.cs.graph.InvDefaultDirectedGraph;
import edu.boisestate.cs.modelling.MASOutput;
import edu.boisestate.cs.SolveMain;

import static edu.boisestate.cs.InputSolver.run_Acyclic_Inverse_r3;

// this class will actualy run the MAS query using run acyclic method from SolveMain.
public class MASProcessor {
    private MASOutput output;

    public MASProcessor() {}

    public static MASOutput query(Object g) {
        InvDefaultDirectedGraph graph = (InvDefaultDirectedGraph) g;

        run_Acyclic_Inverse_r3(graph);


        return null;
    }
}
