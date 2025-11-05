package edu.boisestate.cs.MAS;

public class ConcatTest {

    public static void main(String[] args) {
        test(args[0], args[1]);
    }

    public static void test(String var_1, String var_2) {
        String var_3 = var_1.concat(var_2);
        if (var_3.equals("HelloWorld")) {
            System.out.println(var_3);
        }
    }
}
