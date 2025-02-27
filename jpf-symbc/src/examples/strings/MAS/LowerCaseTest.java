package strings.MAS;

public class LowerCaseTest {
    public static void main(String[] args) {
        String s1 = "HelloWorld";
        test(s1);
    }

    public static void test(String s1) {
        if (s1.toLowerCase().equals("helloworld")) {
            System.out.println("s1.toLowerCase() equals \"helloworld\"");
        } else {
            System.out.println("s1.toLowerCase() does not equal \"helloworld\"");
        }
    }
}
