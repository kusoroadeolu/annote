package com.github.kusoroadeolu.annote.statements;

public sealed interface Result {
    record None() implements Result {}
    record ReturnValue(Object value) implements Result {}
}