package edu.boisestate.cs;

import java.util.logging.Logger;
import java.util.HashMap;

import edu.boisestate.cs.modelling.MASOutput;
import gov.nasa.jpf.util.LogManager;
import gov.nasa.jpf.symbc.string.StringPathCondition;

public class TranslateToMAS {
    static Logger logger = LogManager.getLogger("TranslateToMAS");

    public static MASOutput solve(StringPathCondition pc) {
        MASOutput output = null;

        final MASTranslator translator = new MASTranslator();
        MASProcessor processor = new MASProcessor();
        processor.query(translator.translate(pc));

        output = processor.getOutput();


    }
 }
