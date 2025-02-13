package edu.boisestate.cs.util;

import edu.boisestate.cs.graph.PrintConstraint;
import gov.nasa.jpf.symbc.string.*;

import java.util.ArrayList;
import java.util.List;

public class ConstraintTranslator {
    private final MASTranslator translator;

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

        comparatorConstraint.sourceConstraints.add(leftConstraint);
        comparatorConstraint.sourceConstraints.add(rightConstraint);

        List<PrintConstraint> constraints = new ArrayList<>();
        constraints.add(leftConstraint);
        constraints.add(rightConstraint);
        constraints.add(comparatorConstraint);

        return constraints;
    }

    public PrintConstraint translate(StringExpression se) {
        if (se instanceof StringConstant) {
            StringConstant stringConstant = (StringConstant) se;
            return new PrintConstraint(translator.getNextID(), stringConstant.toString(), "\"" + stringConstant.value() + "\"!:!<init>");
        } else if (se instanceof StringSymbolic) {
            StringSymbolic stringSymbolic = (StringSymbolic) se;
            int id = translator.getNextID();
            String val = "r" + id + "!:!getStringValue!!";
            return new PrintConstraint(id, stringSymbolic.toString(), val);
        } else {
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
            default:
                System.out.println("Unhandled StringComparator: " + comparator);
                op = "";
        }

        return new PrintConstraint(translator.getNextID(), value, op);
    }


}
