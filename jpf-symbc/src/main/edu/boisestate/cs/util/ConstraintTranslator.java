package edu.boisestate.cs.util;

import edu.boisestate.cs.graph.PrintConstraint;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
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
        comparatorConstraint.sourceConstraints.add(rightConstraint);

        constraints.add(rightConstraint);
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
            System.out.println(se.getClass());
            System.out.println(se.getName());
            System.err.println("Unhandled StringExpression: " + se);
            System.exit(1);
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
                System.exit(1);
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
            default:
                System.err.println("Unhandled DerivedStringExpression: " + dse);
                System.exit(1);
                return null;
        }
    }

    public PrintConstraint translate(IntegerConstant ic) {
        return new PrintConstraint(translator.getNextID(), ic.toString().replace("CONST_", ""), "\"" + ic.value() + "\"!:!<init>");
    }

    public void clearConstraintsList() {
        constraints.clear();
    }


}
