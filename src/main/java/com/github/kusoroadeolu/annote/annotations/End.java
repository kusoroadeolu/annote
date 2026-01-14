package com.github.kusoroadeolu.annote.annotations;

import com.github.kusoroadeolu.annote.annotations.containers.Ends;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Ends.class)
public @interface End {
}
