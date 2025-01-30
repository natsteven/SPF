package edu.boisestate.cs;

import edu.boisestate.cs.modelling.MASOutput;
import edu.boisestate.cs.util.NaiveIntegration;
import edu.ucsb.cs.vlab.translate.smtlib.from.z3str3.Z3Translator;
import gov.nasa.jpf.symbc.string.StringPathCondition;
import gov.nasa.jpf.util.LogManager;

import java.util.logging.Logger;

public class MASInterface {
    static Logger logger = LogManager.getLogger("TranslateToMAS");

    public static MASOutput solve(StringPathCondition pc) {
        MASOutput output = null;

        // Naive implementation of the MAS interface
        // We take the SMTLIB string and put it through translation to MASjson, solve, and return MAS's output
        final Z3Translator translator = new Z3Translator();
        final String smtlibQuery = translator.translate(pc);

        //debug
        System.out.println("*************************************");
        System.out.println("The smtlib query is: " + smtlibQuery);

        NaiveIntegration ni = new NaiveIntegration();
        try {
            String masOutput = ni.solve(smtlibQuery);
            System.out.println("MAS returned: ------------------------------\n" + masOutput + "---------------------------------");
            output = new MASOutput(masOutput);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return output;
    }

}
