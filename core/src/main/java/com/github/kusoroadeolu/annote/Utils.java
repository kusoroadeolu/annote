package com.github.kusoroadeolu.annote;

import com.github.kusoroadeolu.annote.exception.AnnoteException;
import com.github.kusoroadeolu.annote.math.ArithmeticExpr;
import com.github.kusoroadeolu.annote.statements.Scope;
import com.github.kusoroadeolu.annote.statements.Variable;
import com.github.kusoroadeolu.annote.tokenizer.Operator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.lang.IO.println;

public class Utils {

    private static final Pattern BOUNDARY = Pattern.compile("\\b");
    private static final Pattern NEW_LINE = Pattern.compile("\\s+");

    private Utils(){throw new AssertionError("can't instantiate this, i think?");}

    public static boolean isDoubleInstance(Object o){
        return o instanceof Double;
    }

    public static boolean isStringInstance(Object o){
        return o instanceof String;
    }

    public static boolean isBoolInstance(Object o){
        return o instanceof Boolean;
    }


    public static void ensureNotString(Object o1, Object o2){
        if (isStringInstance(o1) || isStringInstance(o2)) throw new IllegalArgumentException("string cannot perform this op");
    }

    public static void ensureNotBoolean(Object o1, Object o2){
        if (isBoolInstance(o1) || isBoolInstance(o2)) throw new IllegalArgumentException("string cannot perform this op");
    }

    public static void print(Object o){
        println(o);
    }

    public static double asDouble(Object o){
        return ((Number)o).doubleValue();
    }


    public static String asString(Object o){
        return String.valueOf(o);
    }

    public static boolean asBoolean(Object o){
        return Boolean.parseBoolean(asString(o));
    }


    public static ArithmeticExpr eval(ArithmeticExpr e1, ArithmeticExpr e2, Operator operator){
        return switch (operator){
            case PLUS -> new ArithmeticExpr.Add(e1, e2);
            case MINUS -> new ArithmeticExpr.Subtract(e1, e2);
            case DIVISION -> new ArithmeticExpr.Divide(e1, e2);
            case MULTIPLY -> new ArithmeticExpr.Multiply(e1, e2);
            case MODULO -> new ArithmeticExpr.Modulo(e1, e2);
            case EXPONENTIAL -> new ArithmeticExpr.Exponential(e1, e2);
            default -> throw new AnnoteException("Operator: %s not found".formatted(operator));
        };
    }


    public static String insertVariables(String original, Scope scope) {
        original = original.replaceAll(NEW_LINE.pattern(), "");

        List<String> varNames = new ArrayList<>(scope.keySet());
        varNames.sort((a, b) -> b.length() - a.length());

        for (String varName : varNames) {
            Variable variable = scope.get(varName);
            if (variable == null) throw new AnnoteException("Variable: %s doesnt exist in this scope".formatted(varName));
            Object o = variable.obj();
            original = original.replaceAll(
                    BOUNDARY.pattern() + Pattern.quote(varName) + BOUNDARY.pattern(),
                    o.toString()
            );
        }

        return original;
    }

}
