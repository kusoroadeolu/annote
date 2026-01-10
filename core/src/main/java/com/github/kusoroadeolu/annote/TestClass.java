package com.github.kusoroadeolu.annote;

import com.github.kusoroadeolu.annote.annotations.*;

@Field(name = "i", value = "1", type = "num")
@Field(name = "j", value = "1", type = "num")
public class TestClass {

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
    public void fizzbuzz() {}

    @Var(name = "i", value = "1", type = "num")
    @Var(name = "i", value = "i + 1", type = "num")
    @Print(value = "i", type = "num")
    public void print(){

    }

    @Call(methodName = "print", clazz = TestClass.class)
    public void call(){

    }

    @Print(value = "i + j", type = "num")
    public void tryFields(){

    }
}
