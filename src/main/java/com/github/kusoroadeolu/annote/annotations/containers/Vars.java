package com.github.kusoroadeolu.annote.annotations.containers;

import com.github.kusoroadeolu.annote.annotations.Var;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Vars {
    Var[] value();  // This is what holds multiple @Var annotations
}