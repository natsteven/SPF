package strings.MAS;

public class ReplaceTest {
    public static void main(String[] args) {
        String s1 = "HelloWorld";
        testFirst(s1);
        testAll(s1);
    }

    public static void testFirst(String s1) {
        String s2 = s1.replaceFirst("H", "h");
        if (s2.equals("helloWorld")) {
            System.out.println("s2 equals \"helloWorld\"");
        } else {
            System.out.println("s2 does not equal \"helloWorld\"");
        }
    }

    public static void testAll(String s1) {
        String s2 = s1.replace("l", "L");
        if (s2.equals("HeLLoWorLd")) {
            System.out.println("s2 equals \"HeLLoWorLd\"");
        } else {
            System.out.println("s2 does not equal \"HeLLoWorLd\"");
        }
    }
}
