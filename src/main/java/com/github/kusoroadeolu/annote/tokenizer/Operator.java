package com.github.kusoroadeolu.annote.tokenizer;

import com.github.kusoroadeolu.annote.exception.AnnoteException;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum Operator implements Symbol{
    PLUS(new Infix("+", 1)),
    MINUS(new Infix("-", 1)),
    MULTIPLY(new Infix("*", 2)),
    DIVISION(new Infix("/", 2)),
    MODULO(new Infix("%", 2)),
    EXPONENTIAL(new Infix("^", 3)),
    LEFT_BRACKET(new Infix("(", 4)),
    RIGHT_BRACKET(new Infix(")", 4));


    private final Infix infix;
    public static final Map<String, Operator> OPERATOR_MAP = Arrays
            .stream(values())
            .collect(Collectors.toMap(c -> c.infix.symbol(), Function.identity()));
    Operator(Infix infix) {
        this.infix = infix;
    }

    boolean hasLessOrEqualPrecedence(Operator o){
        return this.infix.precedence() <= o.infix.precedence() && !o.isBracket(); //Ensure this isnt a bracket too so we dont add it to our outputs
    }

    public static Operator fromChar(char c){
        Operator o = OPERATOR_MAP.get(String.valueOf(c));
        if (o == null) throw new AnnoteException("Operator of symbol: %s not found".formatted(c));
        return o;
    }

    @Override
    public String toString() {
        return String.valueOf(infix);
    }

    public static boolean isOperator(char c){
        return OPERATOR_MAP.containsKey(String.valueOf(c));
    }


    public Infix infix() {
        return this.infix;
    }
}