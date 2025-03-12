package gov.nasa.jpf.symbc.strings.MAS;

public class IsEmptyTest {
    public static void main(String[] args) {
        String s1 = "HelloWorld";
        String s2 = "";
        test(s1);
    }

    public static void test(String s1) {
        if (s1.isEmpty()) {
            System.out.println("s1 is empty");
        } else {
            System.out.println("s1 is not empty");
        }
    }
}
