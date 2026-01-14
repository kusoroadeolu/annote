package com.github.kusoroadeolu.annote.annotations;

import com.github.kusoroadeolu.annote.annotations.containers.Ifs;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Ifs.class)
public @interface If {
    String condition();
}
