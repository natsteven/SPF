package gov.nasa.jpf.symbc.strings.MAS;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.symbc.strings.ExSymExeStringsDemo;
import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;

public class TestMAS extends TestJPF {
    String methodSignature;
    String clas;
    String path = "gov.nasa.jpf.symbc.strings.MAS.";
    String[] options = {"+classpath=build/tests",
            "+symbolic.method=",    //symbolic method info and signature
            "+symbolic.dp=choco",
            "+symbolic.string_dp=" + "MAS",
            "+symbolic.string_dp_timeout_ms=0",
            "+target=", //target class/method
            "+search.depth_limit = 23 ",
//            "+listener = gov.nasa.jpf.symbc.sequences.SymbolicSequenceListener", //still unsure what this does
            "+symbolic.debug=true",
            "+sourcepath=src/tests",
            "+symbolic.strings=true"};


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

        if(verifyNoPropertyViolation(options)){
            ConcatTest.test(a,b);
        }
    }

    @Test
    public void containsTest(){
        clas = "ContainsTest";
        methodSignature = ".testSym(sym#sym)";
        setOptions(clas, methodSignature);

        String a = "Hello World!";
        String b = "World";

        if(verifyNoPropertyViolation(options)){
            ContainsTest.testSym(a, b);
        }

        methodSignature = ".testConc(sym)";
        setOptions(clas, methodSignature);

        if(verifyNoPropertyViolation(options)){
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

        String a = "Hello World!";

        if(verifyNoPropertyViolation(options)){
            HelloWorld.hello(a);
        }
    }

    @Test
    public void isEmptyTest(){
        clas = "IsEmptyTest";
        methodSignature = ".test(sym)";
        setOptions(clas, methodSignature);

        String a = "Hello World!";

        if(verifyNoPropertyViolation(options)){
            IsEmptyTest.test(a);
        }
    }

    @Test
    public void lowerCaseTest(){
        clas = "LowerCaseTest";
        methodSignature = ".test(sym)";
        setOptions(clas, methodSignature);

        String a = "Hello World!";

        if(verifyNoPropertyViolation(options)){
            LowerCaseTest.test(a);
        }
    }

    @Test
    public void replaceTest(){
        clas = "ReplaceTest";
        methodSignature = ".testFirst(sym)";
        setOptions(clas, methodSignature);

        String a = "Hello World!";

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
        String b = "Hello World!";

        if(verifyNoPropertyViolation(options)){
            StartsWithTest.testSym(a, b);
        }

        methodSignature = ".testConc(sym)";
        setOptions(clas, methodSignature);

        if(verifyNoPropertyViolation(options)){
            StartsWithTest.testConc(b);
        }
    }

    @Test
    public void subStringTest(){
        clas = "SubstringTest";
        methodSignature = ".test(sym)";
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

        String a = "Hello World!";

        if(verifyNoPropertyViolation(options)){
            UpperCaseTest.test(a);
        }
    }

    // set target method and symbolic method info
    public void setOptions(String clas, String methodSignature){
        options[1] = "+symbolic.method="+ path + clas + methodSignature;
        options[5] = "+target="+path + clas;
    }
}
