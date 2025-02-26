package edu.boisestate.cs.util;

import edu.boisestate.cs.Alphabet;
import edu.boisestate.cs.Parser_2;
import edu.boisestate.cs.automatonModel.Model_Acyclic_Inverse;
import edu.boisestate.cs.automatonModel.Model_Acyclic_Inverse_Manager;
import edu.boisestate.cs.graph.InvDefaultDirectedGraph;
import edu.boisestate.cs.graph.SolutionSet;
import edu.boisestate.cs.modelling.MASOutput;
import edu.boisestate.cs.reporting.Reporter_Inverse;
import edu.boisestate.cs.reporting.Reporter_Inverse_BFS;
import edu.boisestate.cs.solvers.Solver_Inverse;

import static edu.boisestate.cs.InputSolver.run_Acyclic_Inverse_r3;

// this class will actualy run the MAS query using run acyclic method from SolveMain.
public class MASProcessor {
    private MASOutput output;

    public MASProcessor() {}

    public static SolutionSet<Model_Acyclic_Inverse> query(Object g) {
        boolean debug = true;
        int initialBound = 14;

        InvDefaultDirectedGraph graph = (InvDefaultDirectedGraph) g;

        //define default alphabet (may want to try out limiting to present characters)
        Alphabet alpha = new Alphabet("H,e,l,o,W,d,r"); //also may want ot expand? def need ot search concretes ofr addtnls

        // TODO: derive or use default bounds

        Model_Acyclic_Inverse_Manager mFactory 					= new Model_Acyclic_Inverse_Manager(alpha, initialBound);
        Solver_Inverse<Model_Acyclic_Inverse> mSolver 		= new Solver_Inverse<Model_Acyclic_Inverse>(mFactory,	initialBound);
        Parser_2<Model_Acyclic_Inverse> mParser 				= new Parser_2<Model_Acyclic_Inverse>(mSolver, debug);
        Reporter_Inverse<Model_Acyclic_Inverse> mReporter 	= new Reporter_Inverse_BFS<Model_Acyclic_Inverse>(graph, mParser, mSolver, debug);
        mSolver.setReduce(true);
        mReporter.run();

        return mReporter.getSolutionSet();
    }
}
