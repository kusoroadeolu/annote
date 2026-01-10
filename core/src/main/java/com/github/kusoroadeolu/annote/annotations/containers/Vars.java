package com.github.kusoroadeolu.annote.annotations.containers;

import com.github.kusoroadeolu.annote.annotations.Var;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Vars {
    Var[] value();  // This is what holds multiple @Var annotations
}