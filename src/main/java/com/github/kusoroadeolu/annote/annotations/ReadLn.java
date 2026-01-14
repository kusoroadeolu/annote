package com.github.kusoroadeolu.annote.annotations;

import com.github.kusoroadeolu.annote.annotations.containers.ReadLns;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ReadLns.class)
public @interface ReadLn {
    String prompt();
    String assignTo();
    String type();
}
