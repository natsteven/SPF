package edu.boisestate.cs.util;

import edu.boisestate.cs.modelling.MASOutput;

import java.util.Arrays;

public class ParseResult {
    private String result;

    public ParseResult(String result) {
        this.result = result;
    }

    public MASOutput parse() {
        // Parse the SAT result and solution if satisfiable

        if (result.startsWith("unsat")) {
            return new MASOutput(false, result);
        } else if (result.startsWith("sat")) {
            // Parse the model
            MASOutput output = new MASOutput(true, result);
            String[] lines = result.split("\n");
            String[] assignments = Arrays.copyOf(lines,1);
            for (String line : assignments) {
                    String[] parts = line.split(":");
                    String var = parts[1].trim();
                    String value = parts[2].trim();

                    output.addSolution(var, value);
            }
        }


        return new MASOutput(true, result);
    }
}
