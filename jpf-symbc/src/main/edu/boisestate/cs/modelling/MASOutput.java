package edu.boisestate.cs.modelling;

import java.util.HashMap;

public class MASOutput {
    private boolean isSAT;
    private String model;
    private HashMap<String, String> solutions;

    public MASOutput(boolean isSAT, String model) {
        this.isSAT = isSAT;
        this.model = model;
        this.solutions = new HashMap<>();
    }

    public void addSolution(String key, String value) {
        solutions.put(key, value);
    }

    public HashMap<String, String> getSolutions() {
        return solutions;
    }

    public String getModel() {
        return model;
    }

    public boolean isSAT() {
        return isSAT;
    }
}
