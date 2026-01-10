package com.github.kusoroadeolu.annote.annotations;

import com.github.kusoroadeolu.annote.annotations.containers.Calls;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Calls.class)
public @interface Call {
    String methodName();
    String returnType() default "void";
    String assignTo() default "";
    Class<?> clazz();
}
