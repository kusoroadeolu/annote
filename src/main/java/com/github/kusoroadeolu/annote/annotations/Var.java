package com.github.kusoroadeolu.annote.annotations;

import com.github.kusoroadeolu.annote.annotations.containers.Vars;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Vars.class)  // Points to the container
public @interface Var {
    String name(); //Name of the variable
    String value();
    String type();
}
