package edu.boisestate.cs.MAS.svcomp;

import gov.nasa.jpf.util.test.TestJPF;

public class TestSV extends TestJPF {
	String methodSignature;
	String clas;
	String path = "edu.boisestate.cs.MAS.";
	String[] options = {"+classpath=build/tests", // unspecified values are set in setOptions()
			"+symbolic.method=Main",    //symbolic method info and signature
			"+symbolic.dp=z3bitvector",
			"+symbolic.string_dp=", // symbolic string decision procedure
			"+symbolic.string_dp_timeout_ms=3000",
			"+target=", //target class/method
			"+search.depth_limit =13 ",
//			"+listener=gov.nasa.jpf.symbc.sequences.SymbolicSequenceListener,gov.nasa.jpf.symbc.SymbolicListener",
			"+symbolic.debug=true",
			"+sourcepath=src/tests",
			"+symbolic.lazy=true", //unsure what this does tbh
			"+symbolic.strings=true"
	};

	public static void main(String[] args) {
		runTestsOfThisClass(args);
	}
}
