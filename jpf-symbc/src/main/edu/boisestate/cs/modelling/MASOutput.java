package edu.boisestate.cs.modelling;

import java.util.HashMap;

public class MASOutput {
    private final boolean isSAT;
    private final String model;
    private final HashMap<String, String> solutions;

    public MASOutput(String model) {
        this.isSAT = model.startsWith("sat");
        this.model = model;
        this.solutions = new HashMap<>();
        model = this.isSAT ? model.replaceFirst("sat,\n", "") : model.replaceFirst("unsat\n", "");
        //System.out.println("Model: " + model);
        String[] lines = model.split("\n");
        for (String line : lines) {
            String[] parts = line.split(":");
            if (parts.length == 2) {
                String var = parts[0].trim();
                String value = parts[1].trim();
                solutions.put(var, value);
            }
        }
    }

    public boolean isSAT() {
        return isSAT;
    }
    public String getModel() {
        return model;
    }
    public HashMap<String, String> getSolutions() {
        return solutions;
    }
}
