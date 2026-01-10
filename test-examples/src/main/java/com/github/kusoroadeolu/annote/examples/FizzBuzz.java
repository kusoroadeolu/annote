package com.github.kusoroadeolu.annote.examples;

import com.github.kusoroadeolu.annote.annotations.*;

/**
 * FizzBuzz implementation using Annote annotations.
 * The annotation processor captures the source order at compile time.
 */
public class FizzBuzz {

    @Var(name = "i", value = "1", type = "num")
    @Loop(condition = "i <= 100")
        @If(condition = "i % 15 == 0")
            @Print(value = "FizzBuzz")
        @Else
            @If(condition = "i % 3 == 0")
                @Print(value = "Fizz")
            @Else
                @If(condition = "i % 5 == 0")
                    @Print(value = "Buzz")
                @Else
                    @Print(value = "i", type = "num")
                @End
            @End
        @End
        @Var(name = "i", value = "i + 1", type = "num")
    @End
    public void run() {}
}
