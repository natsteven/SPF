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
        System.out.println("Using Native Integration to solve the query");

        MASTranslator translator = new MASTranslator();
        InvDefaultDirectedGraph graph = (InvDefaultDirectedGraph) translator.translate(pc);

        translator.addWildCardToAlph(); // add character not in queries concrete strings to alphabet
        String alph = translator.getAlpha();
        Alphabet alpha;
        if (alph.isEmpty()){
            alpha = new Alphabet("A,B,C");
        }else {
            alpha = new Alphabet(alph);
        }
        int bound = translator.getLongestConcreteStringLength() + 1; // could also reason about concats but for now this is fine

        MASProcessor processor = new MASProcessor(false, alpha, bound);
        return processor.query(graph);
    }

}
