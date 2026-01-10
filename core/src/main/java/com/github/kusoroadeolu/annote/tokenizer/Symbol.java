package com.github.kusoroadeolu.annote.tokenizer;

public interface Symbol {
    default boolean hasLessOrEqualPrecedence(Symbol s){
        return this.infix().precedence() <= s.infix().precedence() && !s.isBracket();
    }
    Infix infix();
    default boolean isLeftBracket(){
        return this.infix().symbol().equals("(");
    }

    default boolean isRightBracket(){
        return this.infix().symbol().equals(")");
    }

    default boolean isBracket(){
        return this.isLeftBracket() || this.isRightBracket();
    }
}
