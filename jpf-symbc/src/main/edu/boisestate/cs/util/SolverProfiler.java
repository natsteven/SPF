package edu.boisestate.cs.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SolverProfiler {
    private static final String CSV_FILE = "solver_timings.csv";
    private static final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private static final String benchmarkName;
    private static boolean isInitialized = false;

    static {
        // Grab the name passed from BenchExec, default to "Unknown" if missing
        benchmarkName = System.getProperty("benchmark.name", "Unknown_Benchmark");
        
        if (threadBean.isCurrentThreadCpuTimeSupported()) {
            threadBean.setThreadCpuTimeEnabled(true);
        }
        
        // Write CSV Header if the file doesn't exist yet
        try {
            if (!Files.exists(Paths.get(CSV_FILE))) {
                try (PrintWriter out = new PrintWriter(new FileWriter(CSV_FILE, true))) {
                    out.println("Benchmark,Solver,WallTime_ns,CPUTime_ns");
                }
            }
            isInitialized = true;
        } catch (IOException e) {
            System.err.println("Failed to initialize SolverProfiler: " + e.getMessage());
        }
    }

    public static void recordCall(String solver, long startWall, long startCpu, long endWall, long endCpu) {
        if (!isInitialized) return;

        long wallElapsed = Math.round((endWall - startWall)/ 1_000_000.0); // Convert to milliseconds
        long cpuElapsed = Math.round((endCpu - startCpu)/ 1_000_000.0); // Convert to milliseconds

        // Synchronized to prevent garbled lines if SPF runs multiple threads
        synchronized (SolverProfiler.class) {
            // true = append mode. We write and close immediately so BenchExec SIGKILLs don't lose data.
            try (PrintWriter out = new PrintWriter(new FileWriter(CSV_FILE, true))) {
                out.printf("%s,%s,%d,%d%n", benchmarkName, solver, wallElapsed, cpuElapsed);
                out.flush(); // Force write to OS
            } catch (IOException e) {
                System.err.println("Failed to write to solver profiling CSV.");
            }
        }
    }

    public static long getCpuTime() {
        return threadBean.getCurrentThreadCpuTime();
    }
}
