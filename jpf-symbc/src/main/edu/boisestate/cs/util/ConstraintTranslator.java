package edu.boisestate.cs.util;

import edu.boisestate.cs.graph.PrintConstraint;
import edu.ucsb.cs.vlab.translate.NormalFormTranslator;
import gov.nasa.jpf.symbc.string.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ConstraintTranslator extends NormalFormTranslator<StringConstraint, StringComparator, PrintConstraint> {

    public ConstraintTranslator() {
        super((x) -> new PrintConstraint("default", "default"), null);
    }

    @Override
    public StringComparator getKeyFrom(StringConstraint instance) {
        return instance.getComparator();
    }

    @Override
    public List<PrintConstraint> transformChain(StringConstraint instance, List<PrintConstraint> collection) {
        if (instance == null)
            return collection;
        collection.clear();
        PrintConstraint leftConstraint = transformStringExpression(instance.getLeft());
        PrintConstraint rightConstraint = transformStringExpression(instance.getRight());
        PrintConstraint comparatorConstraint = new PrintConstraint(instance.getComparator().toString(), instance.getComparator().toString());

        // supposed to be adding constraints to compartor as sources
        //may also need argList stuff and/or sourceMap stuff
        comparatorConstraint.setSource(leftConstraint);
        comparatorConstraint.setSource(rightConstraint);

        collection.add(comparatorConstraint);
        collection.add(leftConstraint);
        collection.add(rightConstraint);
        return collection;
    }

    private PrintConstraint transformStringExpression(StringExpression expression) {
        if (expression instanceof StringConstant) {
            return new PrintConstraint("CONSTANT", expression.toString());
        } else if (expression instanceof StringSymbolic) {
            return new PrintConstraint("SYMBOLIC", expression.toString());
        } else if (expression instanceof DerivedStringExpression) {
            return new PrintConstraint("DERIVED", expression.toString());
        } else {
            return new PrintConstraint("UNKNOWN", expression.toString());
        }
    }

    public List<PrintConstraint> translate(StringConstraint instance) {
        return transformChain(instance, Collections.emptyList());
    }
}
