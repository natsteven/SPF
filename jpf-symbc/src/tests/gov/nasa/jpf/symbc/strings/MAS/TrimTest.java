package gov.nasa.jpf.symbc.strings.MAS;

public class TrimTest {
    public static void test(String s) {
        if (s.contains(" ")) System.out.println("String contains spaces: "+s);
        String trimmed = s.trim();
        if (trimmed.equals("Hi")) {
            System.out.println("Trimmed string is correct: " + trimmed);
        } else {
            System.out.println("Trimmed string is incorrect: " + trimmed);
        }
    }
}
