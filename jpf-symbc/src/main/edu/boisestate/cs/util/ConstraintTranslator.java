package edu.boisestate.cs.util;

import edu.boisestate.cs.graph.PrintConstraint;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.string.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConstraintTranslator {
    private final MASTranslator translator;
    private final List<PrintConstraint> constraints = new ArrayList<>();
    private final HashMap<String, PrintConstraint> constraintsMap = new HashMap<>();

    public ConstraintTranslator(MASTranslator translator) {
        this.translator = translator;
    }

    public List<PrintConstraint> translate(StringConstraint sc) {
        final StringComparator comparator = sc.getComparator();
        final StringExpression left = sc.getLeft();
        final StringExpression right = sc.getRight();
        final PrintConstraint leftConstraint = translate(left);
        final PrintConstraint rightConstraint = translate(right);
        final PrintConstraint comparatorConstraint = translate(comparator);

        // setting type based on left vs. right
        leftConstraint.setType(0);
        rightConstraint.setType(1);
        comparatorConstraint.sourceConstraints.add(leftConstraint);
        comparatorConstraint.sourceConstraints.add(rightConstraint);

        constraints.add(leftConstraint);
        constraints.add(rightConstraint);
        constraints.add(comparatorConstraint);

        return constraints;
    }

    public PrintConstraint translate(StringExpression se) {
        // constraints need types for their outgonig edges so we set that here
        // this may need to be changed as i dont remember if and how this is relevant to solving
        if (se instanceof StringConstant) {
            StringConstant stringConstant = (StringConstant) se;
            return new PrintConstraint(translator.getNextID(), stringConstant.toString().replace("CONST_",""), "\"" + stringConstant.value() + "\"!:!<init>");
        } else if (se instanceof StringSymbolic) {
            StringSymbolic stringSymbolic = (StringSymbolic) se;
            int id = translator.getNextID();
            String val = "r" + id + "!:!getStringValue!!";
            return new PrintConstraint(id, stringSymbolic.toString(), val);
        } else if (se instanceof DerivedStringExpression) {
            DerivedStringExpression dse = (DerivedStringExpression) se;
            return translate(dse);
        } else {
            System.out.println(se.getClass());
            System.out.println(se.getName());
            System.out.println("Unhandled StringExpression: " + se);
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
            default:
                System.out.println("Unhandled StringComparator: " + comparator);
                System.exit(1);
                op = "";
        }

        return new PrintConstraint(translator.getNextID(), value, op);
    }

    public PrintConstraint translate(DerivedStringExpression dse) {
        StringOperator op = dse.op;
        switch(op) {
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
            default:
                System.out.println("Unhandled DerivedStringExpression: " + dse);
                System.exit(1);
                return null;
        }
    }

    public PrintConstraint translate(IntegerConstant ic) {
        return new PrintConstraint(translator.getNextID(), ic.toString().replace("CONST_",""), "\"" + ic.value() + "\"!:!<init>");
    }

    public void clearConstraintsList() {
        constraints.clear();
    }


}
