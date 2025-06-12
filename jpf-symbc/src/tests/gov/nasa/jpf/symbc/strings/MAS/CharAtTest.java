package gov.nasa.jpf.symbc.strings.MAS;

public class CharAtTest {

    public static void test(String s1) {
        if (s1.charAt(0) == 'H') {
            System.out.println("First character is H");
        }
        if (s1.contains("Hell")) {
            System.out.println("String contains 'Hell'");
        }
    }
}
