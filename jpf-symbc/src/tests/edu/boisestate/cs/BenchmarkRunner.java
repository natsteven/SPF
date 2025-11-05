package edu.boisestate.cs;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BenchmarkRunner {

    private static List<Benchmark> programs = new ArrayList<>();

    private static final List<String> SOLVERS = Arrays.asList("MAS");
    private static boolean z3OK = true; //for average runtime calc only accumulate when z3 succeeds

    private static final long TIMEOUT_SEC = Long.getLong("bench.timeoutSec", 30L);
    private static final long KILL_SEC = 5L;

    private static HashMap<String, int[]> statusCounts = new HashMap<>(); // Solver : OK, UNSUPPORTED, TIMEOUT, ERROR
    private static List<String> errors = new ArrayList<>();
    private static HashMap<String, Integer> runtimes = new HashMap<>();

    private static final Path OUT_DIR = Paths.get("../benchmarks");
    private static final Path OUT_CSV = OUT_DIR.resolve("results.csv");

    private static final List<String> COMMON_JPF_OPTS = Arrays.asList(
            "+symbolic.dp=choco",
            "+symbolic.strings=true",
            "+symbolic.debug=true",
            "+symbolic.string_dp_timeout_ms=0",
            "+search.depth_limit=23",
			"+listener=gov.nasa.jpf.symbc.sequences.SymbolicSequenceListener"
    );

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && args[0].equals("--file")) {
            Path listFile = Paths.get(args[1]);
            List<String> lines = Files.readAllLines(listFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                Path path = Paths.get(trimmed);
                if (Files.isRegularFile(path)) {
                    // Existing logic for a single file
                    String root = System.getProperty("user.dir");
                    String fqcn = null;
                    if (path.toString().contains("tests")) {
                        fqcn = path.toString().substring(10);
                    } else if (path.toString().contains("examples")) {
                        fqcn = path.toString().substring(13);
                    }
                    fqcn = fqcn.replace(File.separatorChar, '.').replace(".java", "");
                    for (String sig : methodSignaturesFromReflection(fqcn)) {
                        programs.add(new Benchmark(fqcn, sig));
                    }
                }
            }
        // load program benchmarks from a directory specified in args
        } else if (args.length == 1) {
            Path path = Paths.get(args[0]);
            if (Files.isDirectory(path)) {
                System.out.println("Processing directory: " + path);
                parseDirectoryBenchmarks(path);
            } else if (Files.isRegularFile(path)) {
                System.out.println("Processing file: " + path);
                String root = System.getProperty("user.dir");
                String fqcn = null;
                if (path.toString().contains("tests")) {
                    fqcn = path.toString().substring(root.length() + 11); // +11 to skip "src/tests/"
                } else if (path.toString().contains("examples")) {
                    fqcn = path.toString().substring(root.length() + 14); // +13 to skip "src/examples/"
                }
                fqcn = fqcn.replace(File.separatorChar, '.').replace(".java", "");
                for (String sig : methodSignaturesFromReflection(fqcn)) {
                    programs.add(new Benchmark(fqcn, sig));
                }
            } else {
                System.out.println("Invalid path: " + path);
                System.exit(1);
            }
        } else {
            System.out.println("Usage: java bench.BenchmarkRunner <benchmarks_directory | benchmark_file>");
            System.out.println("   or: java bench.BenchmarkRunner --file <list_of_benchmark_files.txt>");
            System.exit(1);
        }
        // Initialize status counts
        for (String solver : SOLVERS) {
            statusCounts.put(solver, new int[4]);
            runtimes.put(solver, 0);
        }

        Files.createDirectories(OUT_DIR);
        Path solutionsDir = OUT_DIR.resolve("solutions");
        Files.createDirectories(solutionsDir);
		boolean newFile = !Files.exists(OUT_CSV);

        try (BufferedWriter w = Files.newBufferedWriter(OUT_CSV, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (newFile) {
            	w.write("timestamp,program,method,solver,status,wall_ms,exitCode\n");
            }
            int count = 1;
            for (Benchmark b : programs) {
                String methodFile = (b.fqcn.substring(b.fqcn.lastIndexOf('.') + 1) + b.methodSig).replaceAll("[^A-Za-z0-9_]+", "_") + "__" + ".txt";
                HashMap<String, ArrayList<String>> solutions = new HashMap<>();

                for (String solver : SOLVERS) {
                    Result r = runOnce(b, solver);
                    w.write(String.format(Locale.ROOT, "%s,%s,%s,%s,%s,%d,%d%n", Instant.now(), b.fqcn, b.methodSig, solver, r.status, r.wallMs, r.exitCode));
                    w.flush();
                    System.out.printf("(%d/%d) -> %s in %d ms (exit %d)%n", count, programs.size(), r.status, r.wallMs, r.exitCode);
                    for (Map.Entry<String, String> e : r.sols.entrySet()) {
                        solutions.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(solver + ":\n" + e.getValue());
                    }
                }
                try (BufferedWriter solW = Files.newBufferedWriter(solutionsDir.resolve(methodFile), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    solW.write(printSolutions(solutions));
                }
                count++;
            }
        }

        System.out.println("Status counts: ");
        for (String solver : SOLVERS) {
            int[] counts = statusCounts.get(solver);
            System.out.printf("  %s: OK=%d, UNSUPPORTED=%d, TIMEOUT=%d, ERROR=%d%n",
                    solver, counts[0], counts[1], counts[2], counts[3]);
        }
        for (String err : errors) {
            System.err.println("Error in : " + err);
        }
        System.out.println("Results written to: " + OUT_CSV.toAbsolutePath());
        System.out.println("Total runtimes (ms): " + runtimes);
        for (String solver : SOLVERS) {
            if (statusCounts.get(solver)[0] > 0)
                System.out.println(solver + " avg: " + (runtimes.get(solver) / statusCounts.get(solver)[0]) + " ms");
        }
    }


    private static Result runOnce(Benchmark b, String solver) throws IOException, InterruptedException {
        System.out.println("Running: " + b.fqcn + b.methodSig + " with solver " + solver + " at " + Instant.now().toString().split("T")[1].split("\\.")[0]);
        // Reuse current classpath so child sees jpf-core, jpf-symbc, tests, and deps
        String parentCp = System.getProperty("java.class.path");

        List<String> cmd = new ArrayList<>();
        cmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        cmd.add("-Xmx1024m");
        cmd.add("-ea");

        cmd.add("-cp");
        cmd.add(parentCp);

        cmd.add("gov.nasa.jpf.tool.RunJPF");

        // Build JPF options
        cmd.addAll(COMMON_JPF_OPTS);
        cmd.add("+symbolic.string_dp=" + solver);
        cmd.add("+symbolic.method=" + b.fqcn + b.methodSig);
        cmd.add("+target=" + b.fqcn);
        // Ensure target classes are visible to JPF's internal classloader
        cmd.add("+classpath=build/tests:build/examples");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        // Pre-create per-run log path and redirect output there
        String className = b.fqcn.substring(b.fqcn.lastIndexOf('.') + 1);
        Path logsDir = OUT_DIR.resolve("logs");
        Files.createDirectories(logsDir);
        String methodFile = (className + b.methodSig).replaceAll("[^A-Za-z0-9_]+", "_") + "__" + solver + ".log";
        Path log = logsDir.resolve(methodFile);
        pb.redirectOutput(log.toFile());

        Instant t0 = Instant.now();
        Process p = pb.start();

        // Wait up to timeout
        boolean finished = p.waitFor(TIMEOUT_SEC, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            boolean died = p.waitFor(KILL_SEC, TimeUnit.SECONDS); // ensure the process is actually terminated
            if (!died) {
                System.err.println("Failed to kill process after timeout: " + String.join(" ", cmd));
            }
        }

        // Take end time only after termination (natural or forced)
        Instant tEnd = Instant.now();

        int exit = finished ? p.exitValue() : 124; // conventional timeout code
        long wall = Duration.between(t0, tEnd).toMillis();

        // Read output from the log file (keeps wall independent from I/O time)
        String out = new String(Files.readAllBytes(log), StandardCharsets.UTF_8);

        // Classify and get solutions
        String status = finished ? classify(out, exit, solver) : "TIMEOUT";
        HashMap<String, String> solutions = getSolutions(out);

        // Optional: count TIMEOUT explicitly
        if (!finished) {
            statusCounts.get(solver)[2]++;
            if (solver.equals("z3str3")) z3OK = false; // do not accumulate runtime if z3 unsupported
        } else if (status.equals("ERROR") || status.equals("UNSUPPORTED")) {
            errors.add(String.format("%s %s %s", b.fqcn, b.methodSig, solver));
        }

        if (status.equals("OK")) {
            if ((solver.equals("MAS") && z3OK) || solver.equals("z3str3")) {
                z3OK = true;
                // accumulate runtime only if z3 was OK or we are measuring z3
                runtimes.put(solver, runtimes.get(solver) + (int) wall);
            }
        }
        try (BufferedWriter w = Files.newBufferedWriter(log, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
            w.write("Runtime(ms) :" + String.valueOf(wall));
        }
        return new Result(status, wall, exit, solutions);
    }

    private static void parseDirectoryBenchmarks(Path dir) throws Exception {
        List<Benchmark> loaded = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.java")) { // FILTER with glob as necessary!!
            for (Path entry : stream) {
                String root = System.getProperty("user.dir");
                String fqcn = null;
                if (entry.toString().contains("tests")) {
                    fqcn = entry.toString().substring(root.length() + 11); // +11 to skip "src/tests/"
                } else if (entry.toString().contains("examples")) {
                    fqcn = entry.toString().substring(root.length() + 14); // +13 to skip "src/examples/"
                }
                fqcn = fqcn.replace(File.separatorChar, '.').replace(".java", "");
                for (String sig : methodSignaturesFromReflection(fqcn)) {
//					if (fqcn.contains("Test")) continue; // skip test classes
                    loaded.add(new Benchmark(fqcn, sig));
                }
            }
        }
        if (!loaded.isEmpty()) {
            System.out.println("Loaded " + loaded.size() + " benchmarks from " + dir);
            programs = loaded;
        } else {
            System.out.println("No valid benchmark files found in " + dir);
            System.exit(1);
        }
    }

    private static List<String> methodSignaturesFromReflection(String cls) throws Exception {
        List<String> sigs = new ArrayList<>();
        try {
            Class<?> clss = Class.forName(cls);
            for (Method m : clss.getDeclaredMethods()) {
                String name = m.getName();
                if (name.equals("main")) continue; // skip main method
                int params = m.getParameterCount();
                StringBuilder sig = new StringBuilder(".").append(name).append("(");
                for (int i = 0; i < params; i++) {
                    if (i > 0) sig.append("#");
                    sig.append("sym"); // assume all params symbolic
                }
                sig.append(")");
                sigs.add(sig.toString());
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return sigs;
    }

    private static String classify(String out, int exit, String solver) {
        if (out.contains("unsupported")) {
            statusCounts.get(solver)[1]++;
            if (solver.equals("z3str3")) z3OK = false; // do not accumulate runtime if z3 unsupported
            return "UNSUPPORTED";
        }
        if (out.contains("[SEVERE]") || out.contains("ERROR")) {
            statusCounts.get(solver)[3]++;
            if (solver.equals("z3str3")) z3OK = false; // do not accumulate runtime if z3 unsupported
            return "ERROR";
        }
        if (exit == 0) {
            statusCounts.get(solver)[0]++;
            return "OK";
        }
        statusCounts.get(solver)[3]++;
        if (solver.equals("z3str3")) z3OK = false; // do not accumulate runtime if z3 unsupported
        return "ERROR";
    }

    private static HashMap<String, String> getSolutions(String out) {
        // first grab the smt-lib part which starts after a line with "query" and ends at "==="
        // then grab the solutions between "****"
        HashMap<String, String> solutions = new HashMap<>();
        String[] lines = out.split("\n");
        StringBuilder smt = new StringBuilder();
        StringBuilder sol = new StringBuilder();
        Iterator<String> it = Arrays.asList(lines).iterator();
        while (it.hasNext()) {
            // we will grab the smt and then the sol and then add it to the map
            String line = it.next();
            if (line.contains("query")) {
                String next = it.hasNext()? it.next() : "";
                while (!next.contains("===") && it.hasNext()) {
                    smt.append(next).append("\n");
                    next = it.next();
                }
            }
            if (line.contains("****")) {
                String next = it.hasNext()? it.next() : "";
                while (!next.contains("****") && it.hasNext()) {
                    sol.append(next).append("\n");
                    next = it.next();
                }
                solutions.put(smt.toString().trim(), sol.toString());
                smt.setLength(0);
                sol.setLength(0);
            }
        }
        return solutions;
    }

    private static String printSolutions(HashMap<String, ArrayList<String>> solutions) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ArrayList<String>> e : solutions.entrySet()) {
            sb.append("************************************\n");
            sb.append(e.getKey()).append("\n");
            sb.append("------------------------------------\n");
            for (String s : e.getValue()) {
                sb.append(s).append("\n");
            }
        }
        return sb.toString();
    }

    private static final class Benchmark {
        final String fqcn;
        final String methodSig;

        Benchmark(String fqcn, String methodSig) {
            this.fqcn = fqcn;
            this.methodSig = methodSig;
        }

    }

    private static final class Result {
        final String status;
        final long wallMs;
        final int exitCode;
        final HashMap<String, String> sols;

        Result(String status, long wallMs, int exitCode, HashMap<String, String> solutions) {
            this.status = status;
            this.wallMs = wallMs;
            this.exitCode = exitCode;
            this.sols = solutions;
        }
    }
}