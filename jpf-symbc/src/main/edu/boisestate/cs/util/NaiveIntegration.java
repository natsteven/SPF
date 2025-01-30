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
            String tempPath = "jpf-symbc/src/main/edu/boisestate/cs/temp/";

            // We write to file because the SMT-Parser-Generator accepts a directory of files as input
            try (FileWriter fw = new FileWriter(tempPath + "temp.smt2")) {
                fw.write(query);
            } catch (IOException e) {
                System.out.println("Error writing to file");
                e.printStackTrace();
            }

            System.out.println("Translating smtlib to MAS json......\n");
            // build a process for the translation
            ProcessBuilder pb = new ProcessBuilder("java", "-cp", "jpf-symbc/src/main/edu/boisestate/cs/lib/SMT-Parser-Generator.jar", "edu.boisestate.cs.MainJSON", tempPath);
            Process p = pb.start();

            // Capture and print the error stream
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    System.err.println(line);
                }
            }

            p.waitFor();
            System.out.println("Translation process finished\nSolving query with MAS......\n");

            pb.command("java", "-cp", "jpf-symbc/src/main/edu/boisestate/cs/lib/MAS.jar", "edu.boisestate.cs.SolveMain", "output_temp/temp.smt2.json", "-s", "Inverse", "-v", "2", "-l", "15");
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
