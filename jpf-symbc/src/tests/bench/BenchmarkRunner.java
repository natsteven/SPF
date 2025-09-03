package bench;

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

    // Configure your benchmarks here: program FQCN and symbolic method signature
    // Example: methodSig must be the JPF "symbolic.method" suffix like ".test(sym#sym)"
    private static List<Benchmark> programs = new ArrayList<>();

    // Solvers to compare
    private static final List<String> SOLVERS = Arrays.asList("z3str3", "MAS");

    // Per-run timeout (seconds)
    private static final long TIMEOUT_SEC = Long.getLong("bench.timeoutSec", 30L);
    private static final long KILL_SEC = 5L;

    // Status tracker
    private static HashMap<String, int[]> statusCounts = new HashMap<>(); // Solver : OK, SAT, UNSAT, TIMEOUT, ERROR
    private static List<String> errors = new ArrayList<>();
    private static HashMap<String, Integer> runtimes = new HashMap<>();

    // Where to write results
    private static final Path OUT_DIR = Paths.get("../benchmarks");
    private static final Path OUT_CSV = OUT_DIR.resolve("results.csv");

    // JPF options common to all runs
    private static final List<String> COMMON_JPF_OPTS = Arrays.asList(
            "+symbolic.dp=choco",
            "+symbolic.strings=true",
            "+symbolic.debug=true",
            "+symbolic.string_dp_timeout_ms=0",
            "+search.depth_limit=23"
    );

    public static void main(String[] args) throws Exception {
        // load program benchmarks from a directory specified in args
        if (args.length > 0) {
            Path path = Paths.get(args[0]);
            if (Files.isDirectory(path)) {
                System.out.println("Processing directory: " + path);
                parseDirectoryBenchmarks(path);
            } else if (Files.isRegularFile(path)) {
                System.out.println("Processing file: " + path);
                String root = System.getProperty("user.dir");
                String fqcn = path.toString().substring(root.length() + 11); // +11 to skip "src/tests/" TODO: make more robust
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
            System.exit(1);
        }
        // Initialize status counts
        for (String solver : SOLVERS) {
            statusCounts.put(solver, new int[5]);
            runtimes.put(solver, 0);
        }

        Files.createDirectories(OUT_DIR);
//        boolean newFile = Files.notExists(OUT_CSV);
        try (BufferedWriter w = Files.newBufferedWriter(OUT_CSV, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
//            if (newFile) {
                w.write("timestamp,program,method,solver,status,wall_ms,exitCode\n");
//            }
            for (Benchmark b : programs) {
                for (String solver : SOLVERS) {
                    Result r = runOnce(b, solver);
                    w.write(String.format(Locale.ROOT, "%s,%s,%s,%s,%s,%d,%d%n",
                            Instant.now(), b.fqcn, b.methodSig, solver, r.status, r.wallMs, r.exitCode));
                    w.flush();
                    System.out.printf("-> %s in %d ms (exit %d)%n",
                            r.status, r.wallMs, r.exitCode);
                }
            }
        }
        System.out.println("Status counts: ");
        for (String solver : SOLVERS) {
            int[] counts = statusCounts.get(solver);
            System.out.printf("  %s: OK=%d, SAT=%d, UNSAT=%d, TIMEOUT=%d, ERROR=%d%n",
                    solver, counts[0], counts[1], counts[2], counts[3], counts[4]);
        }
        for (String err : errors) {
            System.err.println("Error in : " + err);
        }
        System.out.println("Results written to: " + OUT_CSV.toAbsolutePath());
        System.out.println("Total runtimes (ms): " + runtimes);
        if (statusCounts.get("MAS")[0]>0) System.out.println("MAS avg: " + (runtimes.get("MAS")/statusCounts.get("MAS")[0]) + " ms");
        if (statusCounts.get("MAS")[0]>0) System.out.println("Z3 avg: " + (runtimes.get("z3str3")/statusCounts.get("z3str3")[0]) + " ms");
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

        // Classify
        String status = finished ? classify(out, exit, solver) : "TIMEOUT";

        // Optional: count TIMEOUT explicitly
        if (!finished) {
            statusCounts.get(solver)[3]++;
        } else if ("ERROR".equals(status)) {
            errors.add(String.format("%s %s %s", b.fqcn, b.methodSig, solver));
        }

        if (status.equals("OK")){
            runtimes.put(solver, runtimes.get(solver) + (int) wall);
        }
        try (BufferedWriter w = Files.newBufferedWriter(log, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
            w.write("Runtime(ms) :" + String.valueOf(wall));
        }
        return new Result(status, wall, exit);
    }

    private static void parseDirectoryBenchmarks(Path dir) throws Exception {
        List<Benchmark> loaded = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.java")) {
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
    //
//    private static List<String> methodSignaturesFromFile(Path file) throws IOException {
//        Pattern methodPattern = Pattern.compile("public static void (\\w+)\\s*\\(([^)]*)\\)");
//        List<String> methodSigs = new ArrayList<>();
//        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
//
//        try {
//            Files.lines(file).forEach(line -> {
//                Matcher m = methodPattern.matcher(line);
//                if (m.find()) {
//                    String methodName = m.group(1);
//                    String argSignature = m.group(2);
//                    methodSigs.add(methodName + argSignature);
//                }
//
//            });
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return methodSigs;
//    }
    // TODO: parse output for specific path results, including solutions
    private static String classify(String out, int exit, String solver) {
//        if (out.contains("Unsatisfiable") || out.contains("unsat")) return "UNSAT";
//        if (out.contains("Satisfiable") || out.contains("sat")) return "SAT";
        if (out.contains("[SEVERE]") || out.contains("ERROR")) {
            statusCounts.get(solver)[4]++;
            return "ERROR";
        }
        if (exit == 0) {
            statusCounts.get(solver)[0]++;
            return "OK";
        }
        statusCounts.get(solver)[4]++;
        return "ERROR";
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

        Result(String status, long wallMs, int exitCode) {
            this.status = status;
            this.wallMs = wallMs;
            this.exitCode = exitCode;
        }
    }
}