package com.github.kusoroadeolu.annote.annotations;

import com.github.kusoroadeolu.annote.annotations.containers.Returns;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Returns.class)
public @interface Return {
    String value();
    String type();
}
