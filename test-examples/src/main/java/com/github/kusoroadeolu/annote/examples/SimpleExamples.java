package com.github.kusoroadeolu.annote.examples;

import com.github.kusoroadeolu.annote.annotations.*;

/**
 * Simple examples demonstrating Annote annotations.
 */
public class SimpleExamples {

    @Var(name = "x", value = "10", type = "num")
    @If(condition = "x > 5")
        @Print(value = "Big number")
    @Else
        @Print(value = "Small number")
    @End
    public void conditional() {}

    @Var(name = "i", value = "0", type = "num")
    @Loop(condition = "i < 5")
        @Print(value = "i", type = "num")
        @Var(name = "i", value = "i + 1", type = "num")
    @End
    public void simpleLoop() {}

    @Var(name = "a", value = "5", type = "num")
    @Var(name = "b", value = "3", type = "num")
    @Var(name = "sum", value = "a + b", type = "num")
    @Print(value = "sum", type = "num")
    public void addition() {}

    @Var(name = "x", value = "42", type = "num")
    @Return(value = "x", type = "num")
    public void returnValue() {}
}
