package com.github.kusoroadeolu.annote.tokenizer;

public record Token(Object o) {
    public boolean isNumber(){
        return o instanceof Number;
    }

    public boolean isOperator(){
        return o instanceof Operator;
    }

    public boolean isCondition(){
        return o instanceof Condition;
    }
}
