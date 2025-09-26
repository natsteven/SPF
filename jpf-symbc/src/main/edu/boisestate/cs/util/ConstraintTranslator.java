package edu.boisestate.cs.util;

import edu.boisestate.cs.graph.PrintConstraint;
import gov.nasa.jpf.symbc.numeric.*;
import gov.nasa.jpf.symbc.string.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ConstraintTranslator {
    private final MASTranslator translator;
    private final HashSet<PrintConstraint> constraints = new HashSet<>();
    private final HashMap<String, PrintConstraint> symMap = new HashMap<>();

    public ConstraintTranslator(MASTranslator translator) {
        this.translator = translator;
    }

    // Parsing SPF string constraints into a set of MAS constraints
    // requires keeping track of constraints and their relationships, specifically symbolic strings.
    public HashSet<PrintConstraint> translate(StringConstraint sc) {
        if (sc == null) { //only Nuermic Constaints which were already handled and accumulaterd
            return constraints;
        }
        final StringComparator comparator = sc.getComparator();
        final StringExpression left = sc.getLeft();
        final StringExpression right = sc.getRight();

        PrintConstraint leftConstraint = null;
        PrintConstraint rightConstraint = translate(right);
        final PrintConstraint comparatorConstraint = translate(comparator);


        // setting type based on left vs. right
        if (left!=null){
            leftConstraint = translate(left);
            leftConstraint.setType(0);
            comparatorConstraint.sourceConstraints.add(leftConstraint);
            constraints.add(leftConstraint);
            rightConstraint.setType(1);
        } else {
            rightConstraint.setType(0);
        }
        // in rare case left=right
		if (!comparatorConstraint.sourceConstraints.contains(rightConstraint)) {
			comparatorConstraint.sourceConstraints.add(rightConstraint);
		}

        constraints.add(rightConstraint); // is set so wont add if exists
        constraints.add(comparatorConstraint);

        return constraints;
    }

    public PrintConstraint translate(StringExpression se) {
        // constraints need types for their outgonig edges so we set that here
        // this may need to be changed as i dont remember if and how this is relevant to solving
        if (se instanceof StringConstant) {
            StringConstant stringConstant = (StringConstant) se;
            String val = stringConstant.value.replace("CONST_", "");
            // add concrete strings to alphabet
            for (int i = 0; i < val.length(); i++) {
                translator.addCharToAlph(val.charAt(i));
            }
            if (val.length() > translator.getLongestConcreteStringLength()) {
                translator.setLongestConcreteStringLength(val.length());
            }
            return new PrintConstraint(translator.getNextID(), val, "\"" + val + "\"!:!<init>");
        } else if (se instanceof StringSymbolic) {
            StringSymbolic stringSymbolic = (StringSymbolic) se;
            String sym = stringSymbolic.toString();
            if (symMap.containsKey(sym)) {
                return symMap.get(sym);
            }
            int id = translator.getNextID();
            String val = "r" + id + "!:!getStringValue!!";
            PrintConstraint symConstraint = new PrintConstraint(id, sym, val);
            symMap.put(sym, symConstraint);
            return symConstraint;
        } else if (se instanceof DerivedStringExpression) {
            DerivedStringExpression dse = (DerivedStringExpression) se;
            return translate(dse);
        } else {
            System.err.println(se.getClass());
            System.err.println(se.getName());
            System.err.println("Unhandled StringExpression: " + se);
//            System.exit(1);
        }
        return null;
    }

    public PrintConstraint translate(StringComparator comparator) {
        String op;
        String value = "true";
        switch (comparator) {
            case EQUALS:
            case EQ:
                op = "equals!!Ljava/lang/Object;!:!0";
                break;
            case NOTEQUALS:
            case NE:
                op = "equals!!Ljava/lang/Object;!:!0";
                value = "false";
                break;
            case CONTAINS:
                op = "contains!!Ljava/lang/CharSequence;!:!0";
                break;
            case NOTCONTAINS:
                op = "contains!!Ljava/lang/CharSequence;!:!0";
                value = "false";
                break;
            case ENDSWITH:
                op = "endsWith!!Ljava/lang/String;!:!0";
                break;
            case NOTENDSWITH:
                op = "endsWith!!Ljava/lang/String;!:!0";
                value = "false";
                break;
            case STARTSWITH:
                op = "startsWith!!Ljava/lang/String;!:!0";
                break;
            case NOTSTARTSWITH:
                op = "startsWith!!Ljava/lang/String;!:!0";
                value = "false";
                break;
            case EMPTY:
                op = "isEmpty!!!:!0";
                break;
            case NOTEMPTY:
                op = "isEmpty!!!:!0";
                value = "false";
                break;
            default:
                System.err.println("Unhandled StringComparator: " + comparator);
//                System.exit(1);
                op = "";
        }

        return new PrintConstraint(translator.getNextID(), value, op);
    }

    public PrintConstraint translate(DerivedStringExpression dse) {
        StringOperator op = dse.op;
        switch (op) {
            case CONCAT:
                PrintConstraint left = translate(dse.left);
                left.setType(0);
                PrintConstraint right = translate(dse.right);
                right.setType(1);
                PrintConstraint concat = new PrintConstraint(translator.getNextID(), dse.left.toString() + dse.right.toString(), "concat!!Ljava/lang/String;!:!0");
                concat.sourceConstraints.add(left);
                concat.sourceConstraints.add(right);
                constraints.add(left);
                constraints.add(right);
                return concat;
            case SUBSTRING:
                StringExpression str = (StringExpression) dse.oprlist[0];
                IntegerConstant startIndex = (IntegerConstant) dse.oprlist[2];
                IntegerConstant endIndex = (IntegerConstant) dse.oprlist[1];
                PrintConstraint substring = new PrintConstraint(translator.getNextID(), str.toString().substring(startIndex.value(), endIndex.value()), "substring!!II!:!0");
                PrintConstraint strConstraint = translate(str);
                strConstraint.setType(0);
                PrintConstraint start = translate(startIndex);
                start.setType(1);
                PrintConstraint end = translate(endIndex);
                end.setType(2);

                substring.sourceConstraints.add(strConstraint);
                substring.sourceConstraints.add(start);
                substring.sourceConstraints.add(end);
                constraints.add(strConstraint);
                constraints.add(start);
                constraints.add(end);

                return substring;
            case TOLOWERCASE:
                PrintConstraint toLowerCase = new PrintConstraint(translator.getNextID(), dse.right.toString().toLowerCase(), "toLowerCase!!!:!0");
                PrintConstraint strConstraint2 = translate(dse.right);
                strConstraint2.setType(0);
                toLowerCase.sourceConstraints.add(strConstraint2);
                constraints.add(strConstraint2);
                return toLowerCase;
            case TOUPPERCASE:
                PrintConstraint toUpperCase = new PrintConstraint(translator.getNextID(), dse.right.toString().toUpperCase(), "toUpperCase!!!:!0");
                PrintConstraint strConstraint3 = translate(dse.right);
                strConstraint3.setType(0);
                toUpperCase.sourceConstraints.add(strConstraint3);
                constraints.add(strConstraint3);
                return toUpperCase;
            case REPLACEFIRST:
                StringExpression se = (StringExpression) dse.oprlist[0];
                String find = dse.oprlist[1].toString();
                String replace = dse.oprlist[2].toString();
                PrintConstraint replaceFirst = new PrintConstraint(translator.getNextID(), se.toString().replaceFirst(find, replace), "replaceFirst!!Ljava/lang/String;Ljava/lang/String;!:!0");
                PrintConstraint strConstraint4 = translate(se);
                strConstraint4.setType(0);
                PrintConstraint findConstraint = translate(new StringConstant(find));
                findConstraint.setType(1);
                PrintConstraint replaceConstraint = translate(new StringConstant(replace));
                replaceConstraint.setType(2);

                replaceFirst.sourceConstraints.add(strConstraint4);
                replaceFirst.sourceConstraints.add(findConstraint);
                replaceFirst.sourceConstraints.add(replaceConstraint);
                constraints.add(strConstraint4);
                constraints.add(findConstraint);
                constraints.add(replaceConstraint);
                return replaceFirst;
            case REPLACEALL:
            case REPLACE:
                StringExpression se2 = (StringExpression) dse.oprlist[0];
                String find2 = dse.oprlist[1].toString();
                String replace2 = dse.oprlist[2].toString();
                PrintConstraint replaceAll = new PrintConstraint(translator.getNextID(), se2.toString().replaceAll(find2, replace2), "replaceAll!!Ljava/lang/String;Ljava/lang/String;!:!0");
                PrintConstraint strConstraint5 = translate(se2);
                strConstraint5.setType(0);
                PrintConstraint findConstraint2 = translate(new StringConstant(find2));
                findConstraint2.setType(1);
                PrintConstraint replaceConstraint2 = translate(new StringConstant(replace2));
                replaceConstraint2.setType(2);

                replaceAll.sourceConstraints.add(strConstraint5);
                replaceAll.sourceConstraints.add(findConstraint2);
                replaceAll.sourceConstraints.add(replaceConstraint2);
                constraints.add(strConstraint5);
                constraints.add(findConstraint2);
                constraints.add(replaceConstraint2);
                return replaceAll;
            case DELETE:
                StringExpression se3 = (StringExpression) dse.oprlist[0]; //source
                IntegerConstant index1 = (IntegerConstant) dse.oprlist[1]; // may not always be integer constat... TODO: handle symbolic indices
                IntegerConstant index2 = (IntegerConstant) dse.oprlist[2];

                StringBuilder sb = new StringBuilder(se3.toString());
                sb.delete(index1.value(), index2.value());
                PrintConstraint delete = new PrintConstraint(translator.getNextID(), sb.toString(), "delete!!II!:!0");
                PrintConstraint strConstraint6 = translate(se3);

                // could handle these in translate and likely will need to as IntegerConstant IntegerExpressions, see baove todo
                String val1 = String.valueOf(index1.value());
                String val2 = String.valueOf(index2.value());
                PrintConstraint ind1 = new PrintConstraint(translator.getNextID(), val1, "\"" + val1 + "\"!:!<init>");
                PrintConstraint ind2 = new PrintConstraint(translator.getNextID(), val2, "\"" + val2 + "\"!:!<init>");
                strConstraint6.setType(0);
                ind1.setType(1);
                ind2.setType(2);

                delete.sourceConstraints.add(strConstraint6);
                delete.sourceConstraints.add(ind1);
                delete.sourceConstraints.add(ind2);
                constraints.add(strConstraint6);
                constraints.add(ind1);
                constraints.add(ind2);
                return delete;

            case TRIM:
                StringExpression se4 = (StringExpression) dse.right;
                PrintConstraint trim = new PrintConstraint(translator.getNextID(), se4.toString().trim(), "trim!!!:!0");
                PrintConstraint strConstraint7 = translate(se4);
                strConstraint7.setType(0);
                trim.sourceConstraints.add(strConstraint7);
                constraints.add(strConstraint7);
                return trim;
            case INSERT:
                StringExpression se5 = (StringExpression) dse.oprlist[0]; //source
                IntegerConstant index3 = (IntegerConstant) dse.oprlist[2]; // may not always be integer constant... TODO: handle symbolic indices
                StringExpression insertStr = (StringExpression) dse.oprlist[1]; // string to insert

                StringBuilder sb2 = new StringBuilder(se5.toString());
                sb2.insert(index3.value(), insertStr.toString());
                PrintConstraint insert = new PrintConstraint(translator.getNextID(), sb2.toString(), "insert!!ILjava/lang/CharSequence;!:!0");

                PrintConstraint strConstraint8 = translate(se5);
                PrintConstraint ind3 = translate(index3);
                PrintConstraint insertStrConstraint = translate(insertStr);
                strConstraint8.setType(0);
                ind3.setType(1); // unsure if s1/s2 order matter here
                insertStrConstraint.setType(2);

                insert.sourceConstraints.add(strConstraint8);
                insert.sourceConstraints.add(ind3);
                insert.sourceConstraints.add(insertStrConstraint);
                constraints.add(strConstraint8);
                constraints.add(ind3);
                constraints.add(insertStrConstraint);
                return insert;
            case REVERSE:
                StringExpression se6 = (StringExpression) dse.right;
                StringBuilder rev = new StringBuilder(se6.toString());
                rev.reverse();
                PrintConstraint reverse = new PrintConstraint(translator.getNextID(), rev.toString(), "reverse!!Ljava/lang/String;!:!0");

                PrintConstraint strConstraint9 = translate(se6);
                strConstraint9.setType(0);
                reverse.sourceConstraints.add(strConstraint9);
                constraints.add(strConstraint9);
                return reverse;
            default:
                System.err.println("Unhandled DerivedStringExpression: " + dse);
//                System.exit(1);
                return null;
        }
    }

    // unfortunately we need to handle numeric constraints separately
    // TODO: still unclear how handles actualy mixes of String and Integer PC
    public void translate(Constraint numericConstraint) {
        final Comparator comparator = numericConstraint.getComparator(); // not java.lang.Comparable
        final IntegerExpression left = (IntegerExpression) numericConstraint.getLeft();
        final IntegerExpression right = (IntegerExpression) numericConstraint.getRight();
        PrintConstraint leftConstraint = null;
        PrintConstraint rightConstraint = null;

        // TODO: assumes this/root is predicate
        if (left instanceof SymbolicCharAtInteger) {
            // transform IntegerConstant into String i.e. Character equivalent
            leftConstraint = translate(left);
            rightConstraint = translate(charToString(right));
            leftConstraint.setType(0);
            rightConstraint.setType(1);
        } else if (right instanceof SymbolicCharAtInteger) {
            leftConstraint = translate(right);
            rightConstraint = translate(charToString(left));
            leftConstraint.setType(1);
            rightConstraint.setType(0);
        } else if (left instanceof SymbolicIndexOfInteger) { // hard to generalize this because for example charAt needs to know the integer that is the other arg is a char not an in
            leftConstraint = translate(left);
            rightConstraint = translate(right);
            leftConstraint.setType(0);
            rightConstraint.setType(1);
        } else if (right instanceof SymbolicIndexOfInteger) {
            leftConstraint = translate(right);
            rightConstraint = translate(left);
            leftConstraint.setType(1);
            rightConstraint.setType(0);
        } else {
            System.err.println("Unhandled Numeric Constraint: " + numericConstraint);
//            System.exit(1);
        }

        final PrintConstraint comparatorConstraint = translate(comparator);


        // below may not be necessary for numeric constraints
//        if (left!=null){
//            leftConstraint = translate(left);
//            leftConstraint.setType(0);
            comparatorConstraint.sourceConstraints.add(leftConstraint);
            constraints.add(leftConstraint);
//            rightConstraint.setType(1);
//        } else {
//            rightConstraint.setType(0);
//        }
        comparatorConstraint.sourceConstraints.add(rightConstraint);

        constraints.add(rightConstraint);
        constraints.add(comparatorConstraint);
    }

    private PrintConstraint translate(Comparator comparator) {
        String op;
        String value;
        switch (comparator) {
            case EQ:
                op = "equals!!Ljava/lang/Object;!:!0";
                value = "true";
                break;
            case NE:
                op = "equals!!Ljava/lang/Object;!:!0";
                value = "false";
                break;
            case LT:
                op = "lt!!I!:!0";
                value = "true";
                break;
            case LE:
                op = "le!!I!:!0";
                value = "true";
                break;
            case GT:
                op = "gt!!I!:!0";
                value = "true";
                break;
            case GE:
                op = "ge!!I!:!0";
                value = "true";
                break;
            default:
                System.err.println("Unhandled Numeric Comparator: " + comparator);
//                System.exit(1);
                return null;
        }
        return new PrintConstraint(translator.getNextID(), value, op);
    }


    public PrintConstraint translate(IntegerExpression ie) {
        if (ie instanceof IntegerConstant) {
            IntegerConstant ic = (IntegerConstant) ie;
            if (ic.value() > translator.getLargestIntegerConstant()){
                translator.setLargestIntegerConstant(ic.value());
            }
            return new PrintConstraint(translator.getNextID(), String.valueOf(ic.value()), "\"" + ic.value() + "\"!:!<init>");
        } else if (ie instanceof SymbolicLengthInteger) {
            SymbolicLengthInteger sli = (SymbolicLengthInteger) ie;
            // should probably figure this one out........ ummmmmmmm yeah....
            StringSymbolic sym = (StringSymbolic) sli.getExpression();
            sym.getName();
            System.err.println("Unhandled SymbolicLengthInteger: " + sli);
//            System.exit(1);
        }else if (ie instanceof SymbolicCharAtInteger) {
            // need to create constraint for symbolic var, integer, and actual char op.
            SymbolicCharAtInteger scai = (SymbolicCharAtInteger) ie;
            StringSymbolic sym = (StringSymbolic) scai.getExpression();
            IntegerExpression index = scai.getIndex();

            PrintConstraint symConstraint = translate(sym);
            PrintConstraint indexConstraint = translate(index);
            symConstraint.setType(0);
            indexConstraint.setType(1);

            PrintConstraint charConstraint = new PrintConstraint(translator.getNextID(), scai.toString(), "charAt!!I!:!0");
            charConstraint.sourceConstraints.add(symConstraint);
            charConstraint.sourceConstraints.add(indexConstraint);

            constraints.add(symConstraint);
            constraints.add(indexConstraint);
            return charConstraint;
        } else if (ie instanceof SymbolicIndexOfInteger) {
            SymbolicIndexOfInteger sii = (SymbolicIndexOfInteger) ie;
            StringExpression t = sii.getSource();
            StringExpression s1 = sii.getExpression();

            PrintConstraint tConstraint = translate(t);
            PrintConstraint s1Constraint = translate(s1);
            tConstraint.setType(0);
            s1Constraint.setType(1);

            PrintConstraint indexOfConstraint = new PrintConstraint(translator.getNextID(), sii.toString(), "indexOf!!Ljava/lang/String;!:!0");
            indexOfConstraint.sourceConstraints.add(tConstraint);
            indexOfConstraint.sourceConstraints.add(s1Constraint);

            constraints.add(tConstraint);
            constraints.add(s1Constraint);
            return indexOfConstraint;
        } else {
            System.err.println("Unhandled IntegerExpression: " + ie);
        }
        return null;
    }

    public StringConstant charToString(IntegerExpression ie) {
        // converts IntegerConstant to StringConstant
        // this is used for symbolic charAt expressions
        if (!(ie instanceof IntegerConstant)) {
            System.err.println("Expected IntegerConstant but got: " + ie.getClass());
//            System.exit(1);
        }
        IntegerConstant ic = (IntegerConstant) ie;
        return new StringConstant(String.valueOf((char) ic.value()));
    }

    public void clearConstraintsList() {
        constraints.clear();
    }


}
