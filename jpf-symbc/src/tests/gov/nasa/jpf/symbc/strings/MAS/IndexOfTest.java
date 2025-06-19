package gov.nasa.jpf.symbc.strings.MAS;

public class IndexOfTest {
    public static void testConc(String s1) {
        if (s1.indexOf("Hello") == 0) {
            System.out.println("s1 starts with 'Hello'");
        }
    }

    public static void testSym(String s1, String s2) {
        if (s1.indexOf(s2) == 0) {
            System.out.println("s1 starts with s2");
        }
    }
}
