package com.github.kusoroadeolu.annote;

import com.github.kusoroadeolu.annote.statements.AnnotationParser;

public class AnnoteRunner {
    private AnnoteRunner(){throw new AssertionError("can't instantiate this, i think?");}

    public static Runner newRunner(Class<?> clazz){
        return new AnnotationParser(clazz);
    }

}
