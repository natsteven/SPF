//package edu.boisestate.cs;
//
//import javax.tools.JavaCompiler;
//import javax.tools.ToolProvider;
//import java.io.*;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.*;
//import java.time.Instant;
//import java.util.*;
//import java.util.concurrent.TimeUnit;
//
//public class SingleBenchmarkRunner {
//
//    private static final long TIMEOUT_SEC = Long.getLong("single.timeoutSec", 30L);
//
//    public static void main(String[] args) throws Exception {
//        if (args.length < 1) {
//            System.out.println("Usage: java SingleBenchmarkRunner <PathToJavaFile> [solver=MAS] [symbolicMethodSignature]");
//            System.exit(1);
//        }
//        Path srcFile = Paths.get(args[0]).toAbsolutePath();
//        if (!Files.isRegularFile(srcFile)) {
//            System.err.println("Not a file: " + srcFile);
//            System.exit(2);
//        }
//
//        String solver = args.length > 1 ? args[1] : "MAS"; // or z3str3
//        String userSig = args.length > 2 ? args[2] : null; // like .someMethod(sym#sym)
//
//        // Derive package + FQCN
//        String pkg = extractPackage(srcFile);
//        String simpleName = srcFile.getFileName().toString().replace(".java", "");
//        String fqcn = (pkg == null || pkg.isEmpty()) ? simpleName : pkg + "." + simpleName;
//
//        // Create temp compile dir
//        Path tmpRoot = Files.createTempDirectory("single-spf-");
//        Path classesDir = tmpRoot.resolve("classes");
//        Files.createDirectories(classesDir);
//
//        // (Optional) compile all *.java in the same directory to satisfy dependencies
//        List<String> sources = new ArrayList<>();
//        try (DirectoryStream<Path> ds = Files.newDirectoryStream(srcFile.getParent(), "*.java")) {
//            for (Path p : ds) sources.add(p.toString());
//        }
//
//        System.out.println("Compiling sources: " + sources);
//        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
//        if (jc == null) {
//            System.err.println("No system Java compiler (are you using a JRE?).");
//            System.exit(3);
//        }
//        List<String> javacArgs = new ArrayList<>();
//        javacArgs.add("-g");
//        javacArgs.add("-d");
//        javacArgs.add(classesDir.toString());
//        // Add needed classpath pieces (adjust as needed)
//        String parentCp = System.getProperty("java.class.path");
//        javacArgs.add("-cp");
//        javacArgs.add(parentCp);
//        javacArgs.addAll(sources);
//        int c = jc.run(null, null, null, javacArgs.toArray(new String[0]));
//        if (c != 0) {
//            System.err.println("Compilation failed.");
//            System.exit(4);
//        }
//
//        // Build JPF command
//        List<String> cmd = new ArrayList<>();
//        cmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
//        cmd.add("-Xmx1024m");
//        cmd.add("-ea"); // enable assertions
//        cmd.add("-cp");
//        cmd.add(parentCp); // must include jpf-core, jpf-symbc, etc.
//        cmd.add("gov.nasa.jpf.tool.RunJPF");
//
//        // Core options (tweak depth as needed)
//        cmd.add("+target=" + fqcn);
//        cmd.add("+classpath=" + classesDir); // make compiled benchmark visible
//        cmd.add("+symbolic.strings=true");
//        cmd.add("+symbolic.string_dp=" + solver);
//        cmd.add("+symbolic.dp=choco"); // or z3 if needed for numeric
//        cmd.add("+search.depth_limit=25");
//        cmd.add("+symbolic.debug=true");
//        cmd.add("+listener=gov.nasa.jpf.symbc.sequences.SymbolicSequenceListener");
//
//        if (userSig != null) {
//            // expect form .method(sym#sym) just like BenchmarkRunner
//            cmd.add("+symbolic.method=" + fqcn + userSig);
//        } else {
//            // If you want symbolic main args: uncomment next line (example with one symbolic arg)
//            // cmd.add("+symbolic.method=" + fqcn + ".main(sym)");
//        }
//
//        // Logging
//        Path log = tmpRoot.resolve(simpleName + "-" + solver + ".log");
//        ProcessBuilder pb = new ProcessBuilder(cmd);
//        pb.redirectErrorStream(true);
//        pb.redirectOutput(log.toFile());
//
//        System.out.println("Running: " + fqcn + " solver=" + solver + " at " +
//                Instant.now().toString().split("T")[1].split("\\.")[0]);
//        System.out.println("Cmd: " + String.join(" ", cmd));
//        Process p = pb.start();
//
//        boolean finished = p.waitFor(TIMEOUT_SEC, TimeUnit.SECONDS);
//        if (!finished) {
//            p.destroyForcibly();
//            p.waitFor(5, TimeUnit.SECONDS);
//            System.out.println("STATUS=TIMEOUT");
//            System.out.println("Log: " + log);
//            return;
//        }
//        int exit = p.exitValue();
//        String out = Files.readString(log, StandardCharsets.UTF_8);
//
//        String status;
//        if (out.contains("unsupported")) {
//            status = "UNSUPPORTED";
//        } else if (out.matches("(?s).*error.*AssertionError.*")) {
//            status = "UNSAFE"; // assertion failed
//        } else if (out.contains("no errors detected") && exit == 0) {
//            status = "SAFE";
//        } else {
//            status = (exit == 0) ? "SAFE?" : "ERROR";
//        }
//
//        System.out.println("STATUS=" + status + " exit=" + exit);
//        System.out.println("Log: " + log);
//        // Optional: print last lines
//        List<String> lines = Files.readAllLines(log);
//        lines.stream().skip(Math.max(0, lines.size() - 30)).forEach(System.out::println);
//    }
//
//    private static String extractPackage(Path file) throws IOException {
//        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                line = line.trim();
//                if (line.startsWith("package ")) {
//                    return line.substring(8, line.indexOf(';')).trim();
//                }
//                if (line.startsWith("public class") || line.startsWith("class ")) {
//                    break; // no package
//                }
//            }
//        }
//        return null;
//    }
//}
