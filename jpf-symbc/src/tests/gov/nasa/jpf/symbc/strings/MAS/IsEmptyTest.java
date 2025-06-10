package gov.nasa.jpf.symbc.strings.MAS;

public class IsEmptyTest {
    public static void main(String[] args) {
        String s1 = "HelloWorld";
        String s2 = "";
        test(s1);
    }

    public static void test(String s1) {
        if (s1.isEmpty()) {
            System.out.println("String is empty");
        }
    }

    public static void testMixed(String s1, String s2) {
        if (!s1.contains(s2)) {
            System.out.println("s1 doesn't contain" + s2);
        }
        if (s2.isEmpty()) {
            System.out.println("s2 is empty");
        }
    }
}
