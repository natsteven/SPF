package edu.boisestate.cs;

import org.junit.Test;
import org.sosy_lab.sv_benchmarks.Verifier;

public class TestSPF extends gov.nasa.jpf.util.test.TestJPF {
	String[] options = {"+classpath=build/tests", // unspecified values are set in setOptions()
			"+symbolic.dp=choco",
			"+symbolic.string_dp=MAS", // symbolic string decision procedure
			"+symbolic.string_dp_timeout_ms=3000",
			"+search.depth_limit =13 ",
			"+listener=.symbc.SymbolicListener",
			"+symbolic.debug=true",
			"+sourcepath=src/tests",
			"+symbolic.lazy=true", //unsure what this does tbh
			"+nullPointer.exception=false",
			"+runtime.exception=true",
			"+symbolic.arrays=true",
			"+symbolic.strings=true"
	};

	@Test
	public void testExeSymExeStrings40() {
		if (verifyNoPropertyViolation(options)) {
			gov.nasa.jpf.symbc.strings.ExSymExeStrings40.test(Verifier.nondetString(), Verifier.nondetString(), 1);
		}
	}

	@Test
	public void testExeSymExeStrings13() {
		if (verifyUnhandledException("java.lang.RuntimeException", options))
			gov.nasa.jpf.symbc.strings.ExSymExeStrings13.test(Verifier.nondetString(), Verifier.nondetString());
	}

	@Test
	public void testExeSymExeStrings39() {
		if (verifyNoPropertyViolation(options))
			gov.nasa.jpf.symbc.strings.ExSymExeStrings39.test(Verifier.nondetString(), Verifier.nondetString(), 1);
	}

	@Test
	public void testExeSymExeStrings44() {
		if (verifyNoPropertyViolation(options))
			gov.nasa.jpf.symbc.strings.ExSymExeStrings44.test(Verifier.nondetString(), Verifier.nondetString(), 1);
	}

	@Test
	public void testExeSymExeStrings52() {
		if (verifyNoPropertyViolation(options))
			gov.nasa.jpf.symbc.strings.ExSymExeStrings52.test(Verifier.nondetString(), Verifier.nondetString(), 1);
	}

	@Test
	public void testExeSymExeStrings01() {
		if (verifyNoPropertyViolation(options))
			gov.nasa.jpf.symbc.strings.ExSymExeStrings01.test(Verifier.nondetString(), Verifier.nondetString());
	}

	@Test
	public void testExeSymExeStrings20() {
		if (verifyNoPropertyViolation(options))
			gov.nasa.jpf.symbc.strings.ExSymExeStrings20.test(Verifier.nondetString(), Verifier.nondetString());
	}
}
