package com.github.kusoroadeolu.annote.tokenizer;

import com.github.kusoroadeolu.annote.exception.AnnoteException;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum Condition implements Symbol{
    OR(new Infix("||", -4)),

    AND(new Infix("&&", -3)),

    EQUALS(new Infix("==", -2)),

    GREATER_THAN(new Infix(">", -1)),
    LESS_THAN(new Infix("<", -1)),
    GREATER_THAN_OR_EQUALS(new Infix(">=", -1)),
    LESS_THAN_OR_EQUALS(new Infix("<=", -1)),
    NOT(new Infix("!", 0)),
    LEFT_BRACKET(new Infix("(", 4)),
    RIGHT_BRACKET(new Infix(")", 4));

    private final Infix infix;
    static final Map<String, Condition> CONDITION_MAP = Arrays
            .stream(values())
            .collect(Collectors.toMap(c -> c.infix.symbol(), Function.identity()));

    Condition(Infix infix) {
        this.infix = infix;
    }

    public String getSymbol() {
        return infix.symbol();
    }

    public int getPrecedence() {
        return infix.precedence();
    }

    public static Condition fromString(String symbol) {
        Condition c = CONDITION_MAP.get(symbol);
        if (c == null) throw new AnnoteException("Symbol: %s, not found".formatted(symbol));
        return c;
    }

    public static boolean isCondition(char c){
        return CONDITION_MAP.containsKey(String.valueOf(c));
    }

    public static Condition fromChar(char symbol) {
        return fromString(String.valueOf(symbol));
    }

    public Infix infix() {
        return this.infix;
    }


}

