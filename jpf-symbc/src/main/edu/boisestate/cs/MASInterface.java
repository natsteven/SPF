package edu.boisestate.cs;

import edu.boisestate.cs.graph.InvDefaultDirectedGraph;
import edu.boisestate.cs.modelling.MASOutput;
import edu.boisestate.cs.util.MASProcessor;
import edu.boisestate.cs.util.MASTranslator;
import edu.boisestate.cs.automatonModel.Model_Acyclic_Inverse;
import edu.boisestate.cs.graph.SolutionSet;
import edu.boisestate.cs.util.NaiveIntegration;
import edu.ucsb.cs.vlab.translate.smtlib.from.z3str3.Z3Translator;
import gov.nasa.jpf.symbc.string.StringPathCondition;
import gov.nasa.jpf.util.LogManager;
import org.jgrapht.DirectedGraph;

import java.util.logging.Logger;

public class MASInterface {

    public static SolutionSet<Model_Acyclic_Inverse> solve(StringPathCondition pc) {

        // get smtlib query for debugging
        final Z3Translator t = new Z3Translator();
        final String smtlibQuery = t.translate(pc);
        System.out.println("*************************************");
        System.out.println("The smtlib query is: " + smtlibQuery);
//

//        System.out.println("*************************************");
//        System.out.println("Using NaiveIntegration to solve the query");
//        NaiveIntegration ni = new NaiveIntegration();
//        try {
//            String masOutput = ni.solve(smtlibQuery);
//            System.out.println("MAS returned: ------------------------------\n" + masOutput + "---------------------------------");
//            output = new MASOutput(masOutput);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        System.out.println("*************************************");
//        System.out.println("Using Native Integration to solve the query");
//        System.out.println("PATH CONSTRAINT FROM SPF: " + pc);

        MASTranslator translator = new MASTranslator();
        InvDefaultDirectedGraph graph = (InvDefaultDirectedGraph) translator.translate(pc);
        if (graph == null) {
            System.out.println("No graph was created from the StringPathCondition.");
            return null;
        }

        String alph = translator.getAlpha();
        Alphabet alpha;
        if (alph.isEmpty()){
            alpha = new Alphabet("A,B,C");
        }else {
            // ugg that was dumb
            translator.addWildCardToAlph(); // add character not in queries concrete strings to alphabet
            alph = translator.getAlpha();
            alpha = new Alphabet(alph);
        }
        int bound = translator.getLongestConcreteStringLength() + 1;
        if (bound < 4) bound = 4;// could also reason about concats but for now this is fine
        // maybe the depth of the tree, i.e. we can reason about how long strings can/would be given the number of operations/type of ops

        MASProcessor processor = new MASProcessor(false, alpha, bound);
        return processor.query(graph);
    }

}
