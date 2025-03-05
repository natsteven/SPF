package strings.MAS;

public class UpperCaseTest {
    public static void main(String[] args) {
        String s1 = "HelloWorld";
        test(s1);
    }

    public static void test(String s1) {
        if (s1.toUpperCase().equals("HELLOWORLD")) {
            System.out.println("s1.toUpperCase() equals \"HELLOWORLD\"");
        } else {
            System.out.println("s1.toUpperCase() does not equal \"HELLOWORLD\"");
        }
    }
}
