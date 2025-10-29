#!/bin/bash
cd /home/nat/Repos/SPF

# Single parameter tests - add one Verifier.nondetString() and replace concrete string with a1

# CharAtTest
sed -i.bak '6s|test("Hello");|String a1 = Verifier.nondetString();\n        test(a1);|' jpf-symbc/src/tests/edu/boisestate/cs/benchexec/CharAtTest/Main.java

# DeleteTest
sed -i.bak '6s|test("Hello");|String a1 = Verifier.nondetString();\n        test(a1);|' jpf-symbc/src/tests/edu/boisestate/cs/benchexec/DeleteTest/Main.java

# HelloWorld
sed -i.bak '5s|hello("HelloWorld");|String a1 = Verifier.nondetString();\n        hello(a1);|' jpf-symbc/src/tests/edu/boisestate/cs/benchexec/HelloWorld/Main.java

# LowerCaseTest
sed -i.bak '5s|test("HelloWorld");|String a1 = Verifier.nondetString();\n        test(a1);|' jpf-symbc/src/tests/edu/boisestate/cs/benchexec/LowerCaseTest/Main.java

# TrimTest
sed -i.bak '5s|test("  Hi  ");|String a1 = Verifier.nondetString();\n        test(a1);|' jpf-symbc/src/tests/edu/boisestate/cs/benchexec/TrimTest/Main.java

# UpperCaseTest
sed -i.bak '5s|test("HelloWorld");|String a1 = Verifier.nondetString();\n        test(a1);|' jpf-symbc/src/tests/edu/boisestate/cs/benchexec/UpperCaseTest/Main.java

# ValueOfTest
sed -i.bak '6s|test("true");|String a1 = Verifier.nondetString();\n        test(a1);|' jpf-symbc/src/tests/edu/boisestate/cs/benchexec/ValueOfTest/Main.java

# Two parameter tests - add two Verifier.nondetString() calls

# ConcatTest
sed -i.bak '6s|test("Hello", "World");|String a1 = Verifier.nondetString();\n        String a2 = Verifier.nondetString();\n        test(a1, a2);|' jpf-symbc/src/tests/edu/boisestate/cs/benchexec/ConcatTest/Main.java

# ContainsTest
sed -i.bak '6,7s|testSym("HelloWorld", "World");|String a1 = Verifier.nondetString();\n        String a2 = Verifier.nondetString();\n        testSym(a1, a2);|; 7s|testConc("Hello");|testConc(a1);|' jpf-symbc/src/tests/edu/boisestate/cs/benchexec/ContainsTest/Main.java

# EndsWithTest
sed -i.bak '5,6s|testSym("Hello", "llo");|String a1 = Verifier.nondetString();\n        String a2 = Verifier.nondetString();\n        testSym(a1, a2);|; 6s|testConc("Hello");|testConc(a1);|' jpf-symbc/src/tests/edu/boisestate/cs/benchexec/EndsWithTest/Main.java

# IndexOfTest
sed -i.bak '5,6s|String s1 = "HelloWorld";|String a1 = Verifier.nondetString();|; 6s|String s2 = "Hello";|String a2 = Verifier.nondetString();|; s|testSym(s1, s2);|testSym(a1, a2);|; s|testConc(s2);|testConc(a2);|' jpf-symbc/src/tests/edu/boisestate/cs/benchexec/IndexOfTest/Main.java

# InsertTest
sed -i.bak '5,6s|String s1 = "Hello";|String a1 = Verifier.nondetString();|; 6s|String s2 = "World";|String a2 = Verifier.nondetString();|; s|testConc(s1);|testConc(a1);|; s|testSym(s1, s2);|testSym(a1, a2);|' jpf-symbc/src/tests/edu/boisestate/cs/benchexec/InsertTest/Main.java

# IsEmptyTest
sed -i.bak '5,6s|String s1 = "";|String a1 = Verifier.nondetString();|; 6s|String s2 = "hello";|String a2 = Verifier.nondetString();|; s|test(s1);|test(a1);|; s|testMixed(s1, s2);|testMixed(a1, a2);|' jpf-symbc/src/tests/edu/boisestate/cs/benchexec/IsEmptyTest/Main.java

# ReplaceTest
sed -i.bak '5,6s|testFirst("aa");|String a1 = Verifier.nondetString();\n        String a2 = Verifier.nondetString();\n        testFirst(a1);|; 6s|testAll("aa");|testAll(a2);|' jpf-symbc/src/tests/edu/boisestate/cs/benchexec/ReplaceTest/Main.java

# ReverseTest
sed -i.bak '5,6s|String s1 = "stressed";|String a1 = Verifier.nondetString();|; 6s|String s2 = "desserts";|String a2 = Verifier.nondetString();|; s|testConc(s1);|testConc(a1);|; s|testSym(s1, s2);|testSym(a1, a2);|' jpf-symbc/src/tests/edu/boisestate/cs/benchexec/ReverseTest/Main.java

# StartsWithTest
sed -i.bak '5,6s|String s1 = "HelloWorld";|String a1 = Verifier.nondetString();|; 6s|String s2 = "Hello";|String a2 = Verifier.nondetString();|; s|testSym(s1, s2);|testSym(a1, a2);|; s|testConc(s2);|testConc(a2);|' jpf-symbc/src/tests/edu/boisestate/cs/benchexec/StartsWithTest/Main.java

# SubstringTest
sed -i.bak '5,6s|String s1 = "HelloWorld";|String a1 = Verifier.nondetString();|; 6s|String s2 = "World";|String a2 = Verifier.nondetString();|; s|test(s1, s2);|test(a1, a2);|' jpf-symbc/src/tests/edu/boisestate/cs/benchexec/SubstringTest/Main.java
