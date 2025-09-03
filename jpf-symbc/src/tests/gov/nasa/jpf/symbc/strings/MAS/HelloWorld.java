package gov.nasa.jpf.symbc.strings.MAS;

public class HelloWorld {

	public static void hello(String var_1) {
		if (var_1.equals("HelloWorld")){ //| var_1.charAt(3) == 'r') {
			System.out.println(var_1);
		}
		//assert(var_1.equals("Hello, World!"));
	}
}
