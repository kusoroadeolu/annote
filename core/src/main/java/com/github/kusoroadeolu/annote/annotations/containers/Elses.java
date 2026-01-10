package com.github.kusoroadeolu.annote.annotations.containers;

import com.github.kusoroadeolu.annote.annotations.Else;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Elses {
    Else[] value();
}
