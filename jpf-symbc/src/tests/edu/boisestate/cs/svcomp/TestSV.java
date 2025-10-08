package edu.boisestate.cs.svcomp;

import gov.nasa.jpf.util.test.TestJPF;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.security.Permission;

public class TestSV extends TestJPF {

	String[] options = {"+classpath=build/tests", // unspecified values are set in setOptions()
			"+symbolic.dp=z3bitvector",
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
	public void charSequenceToString() {
		if (verifyUnhandledException("java.lang.AssertionError", options)) edu.boisestate.cs.svcomp.CharSequenceToString.Main.main(null);
	}

	@Test
	public void overapproximationString01() {
		if (verifyUnhandledException("java.lang.AssertionError", options)) edu.boisestate.cs.svcomp.OverapproximationString01.Main.main(null);
	}

	@Test
	public void stringBuilderCapLen02() {
		if (verifyUnhandledException("java.lang.AssertionError", options)) edu.boisestate.cs.svcomp.StringBuilderCapLen02.Main.main(null);
	}

	@Test
	public void stringBuilderChars02() {
		if (verifyUnhandledException("java.lang.AssertionError", options)) edu.boisestate.cs.svcomp.StringBuilderChars02.Main.main(null);
	}


	@Test
	public void stringBuilderChars06() {
		if (verifyUnhandledException("java.lang.AssertionError", options)) edu.boisestate.cs.svcomp.StringBuilderChars06.Main.main(null);
	}

	@Test
	public void stringConcatenation01() {
		if (verifyNoPropertyViolation(options)) edu.boisestate.cs.svcomp.StringConcatenation01.Main.main(null);
	}

	@Test
	public void stringConcatenation02() {
		if (verifyUnhandledException("java.lang.AssertionError", options)) edu.boisestate.cs.svcomp.StringConcatenation02.Main.main(null);
	}

	@Test
	public void stringConcatenation03() {
		if (verifyUnhandledException("java.lang.AssertionError", options)) edu.boisestate.cs.svcomp.StringConcatenation03.Main.main(null);
	}

	@Test
	public void stringConcatenation04() {
		if (verifyUnhandledException("java.lang.AssertionError", options)) edu.boisestate.cs.svcomp.StringConcatenation04.Main.main(null);
	}

	@Test
	public void stringConstructors02() {
		if (verifyUnhandledException("java.lang.AssertionError", options)) edu.boisestate.cs.svcomp.StringConstructors02.Main.main(null);
	}

	@Test
	public void stringConstructors03() {
		if (verifyUnhandledException("java.lang.AssertionError", options)) edu.boisestate.cs.svcomp.StringConstructors03.Main.main(null);
	}

	@Test
	public void stringConstructors04() {
		if (verifyUnhandledException("java.lang.AssertionError", options)) edu.boisestate.cs.svcomp.StringConstructors04.Main.main(null);
	}

	@Test
	public void stringConstructors05() {
		if (verifyUnhandledException("java.lang.AssertionError", options)) edu.boisestate.cs.svcomp.StringConstructors05.Main.main(null);
	}

	@Test
	public void stringContains01() {
		if (verifyUnhandledException("java.lang.AssertionError", options)) edu.boisestate.cs.svcomp.StringContains01.Main.main(null);
	}

	@Test
	public void stringContains02() {
		if (verifyUnhandledException("java.lang.AssertionError", options)) edu.boisestate.cs.svcomp.StringContains02.Main.main(null);
	}

//	@Test
//	public void stringMiscellaneous03() {
//		if (verifyNoPropertyViolation(options)) edu.boisestate.cs.svcomp.StringMiscellaneous03.Main.main(null);
//	}

	@Test
	public void stringStartEnd02() {
		if (verifyUnhandledException("java.lang.AssertionError", options)) edu.boisestate.cs.svcomp.StringStartEnd02.Main.main(null);
	}

	@Test
	public void stringStartEnd03() {
		if (verifyUnhandledException("java.lang.AssertionError", options)) edu.boisestate.cs.svcomp.StringStartEnd03.Main.main(null);
	}

	@Test
	public void stringValueOf02() {
		if (verifyUnhandledException("java.lang.AssertionError", options)) edu.boisestate.cs.svcomp.StringValueOf02.Main.main(null);
	}

	@Test
	public void stringValueOf04() {
		if (verifyUnhandledException("java.lang.AssertionError", options)) edu.boisestate.cs.svcomp.StringValueOf04.Main.main(null);
	}

//	@Test
//	public void stringValueOf05() {
//		if (verifyNoPropertyViolation(options)) edu.boisestate.cs.svcomp.StringValueOf05.Main.main(null);
//	}

	@Test
	public void stringValueOf06() {
		if (verifyUnhandledException("java.lang.AssertionError", options)) edu.boisestate.cs.svcomp.StringValueOf06.Main.main(null);
	}


	@Test
	public void stringValueOf10() {
		if (verifyUnhandledException("java.lang.AssertionError", options)) edu.boisestate.cs.svcomp.StringValueOf10.Main.main(null);
	}

	// MAS hangs on this one atm
//	@Test
//	public void subString02() {
//		if (verifyNoPropertyViolation(options)) edu.boisestate.cs.svcomp.SubString02.Main.main(null);
//	}

//	@Test
//	public void subString03() {
//		if (verifyUnhandledException("java.lang.AssertionError", options)) edu.boisestate.cs.svcomp.SubString03.Main.main(null);
//	}

	// note: use for single runs as it manipulates class field
	private void setStringDP(String dp) {
		options[2] = "+symbolic.string_dp=" + dp;
	}

}
//CharSequenceToString
//OverapproximationString01
//StringBuilderAppend02
//StringBuilderCapLen02
//StringBuilderCapLen04
//StringBuilderChars02
//StringBuilderChars03
//StringBuilderChars04
//StringBuilderChars05
//StringBuilderChars06
//StringBuilderConstructors02
//StringBuilderInsertDelete02
//StringBuilderInsertDelete03
//StringCompare02
//StringCompare03
//StringCompare04
//StringConcatenation01
//StringConcatenation02
//StringConcatenation03
//StringConcatenation04
//StringConstructors02
//StringConstructors03
//StringConstructors04
//StringConstructors05
//StringContains01
//StringContains02
//StringMiscellaneous03
//StringStartEnd02
//StringStartEnd03
//StringValueOf02
//StringValueOf04
//StringValueOf05
//StringValueOf06
//StringValueOf07
//StringValueOf08
//StringValueOf09
//StringValueOf10
//SubString02
//SubString03