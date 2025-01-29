package edu.boisestate.cs.util;

import java.io.*;

public class NaiveIntegration {

        /* This will take a string specified and will write it to a file? then build a process to translate
         * it into JSON using SMT-Parser-Generator. It will then build a process to solve that JSON using MAS
         * Then it will read from stdout and parse the results. This will eventually be integrate into an SPF
         * build
         */
        public String solve(String query) throws IOException, InterruptedException {

            // example query that SPF would provide (after z3 translation)

            // We write to file because the SMT-Parser-Generator accepts a directory of files as input
            try (FileWriter fw = new FileWriter(new File("input/test.smt2"))) {
                fw.write(query);
            } catch (IOException e) {
                System.out.println("Error writing to file");
                e.printStackTrace();
            }

            // build a process for the translation
            ProcessBuilder pb = new ProcessBuilder("java", "-cp", "../lib/SMT-Parser-Generator.jar", "edu.boisestate.cs.MainJSON", "input");
            Process p = pb.start();

            // Capture and print the error stream
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    System.err.println(line);
                }
            }

            p.waitFor();
            System.out.println("Translation process finished");

            pb.command("java", "-cp", "..lib/MAS.jar", "edu.boisestate.cs.SolveMain", "output_input/test.smt2.json", "-s", "Inverse", "-v", "2", "-l", "4");
            p = pb.start();

            // Capture and print the error stream
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    System.err.println(line);
                }

            }

            // capture output of solver
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                    sb.append(System.lineSeparator());
                }
            } catch (IOException e) {
                System.out.println("Error reading from MAS");
                e.printStackTrace();
            }

            return sb.toString();
        }

}
