package edu.boisestate.cs.MAS;

import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;

public class TestMAS extends TestJPF {
    String methodSignature;
    String clas;
    String path = "edu.boisestate.cs.MAS.";
    String[] solvers = {"MAS"};
    String[] options = {"+classpath=build/tests", // unspecified values are set in setOptions()
            "+symbolic.method=",    //symbolic method info and signature
            "+symbolic.dp=choco",
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


//    @Test
//    public void testMAS() {
//        concatTest();
//        containsTest();
//        endsWithTest();
//        helloWorld();
//        isEmptyTest();
//        lowerCaseTest();
//        replaceTest();
//        startsWithTest();
//        subStringTest();
//        upperCaseTest();
//    }

    public static void main(String[] args) {
        runTestsOfThisClass(args);
    }


    @Test
    public void concatTest(){
            clas = "ConcatTest";
            methodSignature = ".test(sym#sym)";
            setOptions(clas, methodSignature);

            String a = "Hello";
            String b = "World";

//        Config cf = JPF.createConfig(options);
//        JPF jpf = new JPF(cf);
//        jpf.run();
//        runTests(ConcatTest.class, "test");

            if (verifyNoPropertyViolation(options)) {
                ConcatTest.test(a, b);
            }
    }

    @Test
    public void containsTest(){
            clas = "ContainsTest";
            methodSignature = ".testSym(sym#sym)";
            setOptions(clas, methodSignature);

            String a = "HelloWorld";
            String b = "World";

            if (verifyNoPropertyViolation(options)) {
                ContainsTest.testSym(a, b);
            }

            methodSignature = ".testConc(sym)";
            setOptions(clas, methodSignature);

            if (verifyNoPropertyViolation(options)) {
                ContainsTest.testConc(b);
            }
    }

    @Test
    public void endsWithTest() {
        clas = "EndsWithTest";
        methodSignature = ".testSym(sym#sym)";
        setOptions(clas, methodSignature);

        String a = "Hello";
        String b = "World";

        if(verifyNoPropertyViolation(options)){
            EndsWithTest.testSym(a, b);
        }

        methodSignature = ".testConc(sym)";
        setOptions(clas, methodSignature);

        if(verifyNoPropertyViolation(options)){
            EndsWithTest.testConc(b);
        }

    }

    @Test
    public void helloWorld() {
        clas = "HelloWorld";
        methodSignature = "hello(sym)";
        setOptions(clas, methodSignature);

        String a = "HelloWorld";

        if(verifyNoPropertyViolation(options)){
            HelloWorld.hello(a);
        }
    }

    @Test
    public void isEmptyTest(){
            clas = "IsEmptyTest";
            System.out.println(System.getProperty("java.library.path"));
            methodSignature = ".test(sym)";
            setOptions(clas, methodSignature);

            String a = "HelloWorld";

            if (verifyNoPropertyViolation(options)) {
                IsEmptyTest.test(a);
            }
            methodSignature = ".testMixed(sym#sym)";
            setOptions(clas, methodSignature);

            String b = "";
            if (verifyNoPropertyViolation(options)) {
                IsEmptyTest.testMixed(a, b);
            }
    }

    @Test
    public void lowerCaseTest(){
        clas = "LowerCaseTest";
        methodSignature = ".test(sym)";
        setOptions(clas, methodSignature);
        String a = "HelloWorld";
        if(verifyNoPropertyViolation(options)){
            LowerCaseTest.test(a);
        }

    }

    @Test
    public void replaceTest(){
        clas = "ReplaceTest";
        methodSignature = ".testFirst(sym)";
        setOptions(clas, methodSignature);

        String a = "HelloWorld";

        if(verifyNoPropertyViolation(options)){
            ReplaceTest.testFirst(a);
        }

        methodSignature = ".testAll(sym)";
        setOptions(clas, methodSignature);

        if(verifyNoPropertyViolation(options)){
            ReplaceTest.testAll(a);
        }
    }

    @Test
    public void startsWithTest(){
        clas = "StartsWithTest";
        methodSignature = ".testSym(sym#sym)";
        setOptions(clas, methodSignature);

        String a = "Hello";
        String b = "HelloWorld";

        if(verifyNoPropertyViolation(options)){
            StartsWithTest.testSym(a, b);
        }

        methodSignature = ".testConc(sym)";
        setOptions(clas, methodSignature);

        if(verifyNoPropertyViolation(options)){
            StartsWithTest.testConc(b);
        }
    }

//    @Test
    public void subStringTest(){
        clas = "SubstringTest";
        methodSignature = ".test(sym#sym)";
        setOptions(clas, methodSignature);

        String a = "HelloWorld";
        String b = "World";

        if(verifyNoPropertyViolation(options)){
            SubstringTest.test(a,b);
        }
    }

    @Test
    public void upperCaseTest(){
        clas = "UpperCaseTest";
        methodSignature = ".test(sym)";
        setOptions(clas, methodSignature);

        String a = "HelloWorld";

        if(verifyNoPropertyViolation(options)){
            UpperCaseTest.test(a);
        }
    }

    @Test
    public void deleteTest() {
        clas = "DeleteTest";
        methodSignature = ".test(sym)";
        setOptions(clas, methodSignature);

        String a = "Hello";

        if(verifyNoPropertyViolation(options)){
            DeleteTest.test(a);
        }
    }

    @Test
    public void charAtTest() {
            clas = "CharAtTest";
            methodSignature = ".test(sym)";
            setOptions(clas, methodSignature);

            String a = "Hello";

            if (verifyNoPropertyViolation(options)) {
                CharAtTest.test(a);
            }
    }

    @Test
    public void indexOfTest() {
        clas = "IndexOfTest";
        methodSignature = ".testConc(sym)";
        setOptions(clas, methodSignature);

        String a = "HelloWorld";

        if(verifyNoPropertyViolation(options)){
            IndexOfTest.testConc(a);
        }

        methodSignature = ".testSym(sym#sym)";
        setOptions(clas, methodSignature);

        String b = "Hello";

        if(verifyNoPropertyViolation(options)){
            IndexOfTest.testSym(a, b);
        }
    }

    @Test
    public void valueOfTest() {
        clas = "ValueOfTest";
        methodSignature = ".test(sym)";
        setOptions(clas, methodSignature);

        if(verifyNoPropertyViolation(options)){
            ValueOfTest.test("Hello");
        }

    }

    @Test
    public void trimTest() {
        clas = "TrimTest";
        methodSignature = ".test(sym)";
        setOptions(clas, methodSignature);

        String a = " Hi ";

        if(verifyNoPropertyViolation(options)){
            TrimTest.test(a);
        }
    }

    @Test
    public void insertTest() {
        clas = "InsertTest";
        methodSignature = ".testConc(sym)";
        setOptions(clas, methodSignature);

        String a = "Hello";
        if(verifyNoPropertyViolation(options)){
            InsertTest.testConc(a);
        }
        methodSignature = ".testSym(sym#sym)";
        setOptions(clas, methodSignature);
        String b = "World";
        if(verifyNoPropertyViolation(options)){
            InsertTest.testSym(a, b);
        }
    }

    @Test
    public void reverseTest() {
        clas = "ReverseTest";
        methodSignature = ".testConc(sym)";
        setOptions(clas, methodSignature);

        String a = "stressed";
        if(verifyNoPropertyViolation(options)){
            ReverseTest.testConc(a);
        }
        methodSignature = ".testSym(sym#sym)";
        setOptions(clas, methodSignature);
        String b = "desserts";
        if(verifyNoPropertyViolation(options)){
            ReverseTest.testSym(a, b);
        }
    }

    // set target method and symbolic method info
    public void setOptions(String clas, String methodSignature, String solver){
        options[1] = "+symbolic.method="+ path + clas + methodSignature;
        options[5] = "+target="+path + clas;
        options[3] = "+symbolic.string_dp=" + solver;
    }

    public void setOptions(String clas, String methodSignature) { // default solver is MAS
        setOptions(clas, methodSignature, "MAS");
    }
}
