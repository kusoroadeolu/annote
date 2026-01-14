package com.github.kusoroadeolu.annote.test;

import com.github.kusoroadeolu.annote.AnnoteRunner;
import com.github.kusoroadeolu.annote.Runner;
import com.github.kusoroadeolu.annote.annotations.processor.AnnotationProcessor;
import org.junit.jupiter.api.Test;

class MainTest {
    {
        AnnotationProcessor.init(); // ugh... I don't know....
    }
    @Test
    void fizzbuzz() {
        Runner r = AnnoteRunner.newRunner(TestClass.class);
        r.run("fizzbuzz");  // 17 annotations to prove divisibility
    }
    @Test
    void call() {
        Runner r = AnnoteRunner.newRunner(TestClass.class);
        r.run("call");      // Recursion: because one bad idea enables another
    }
    @Test
    void tryFields() {
        Runner r = AnnoteRunner.newRunner(TestClass.class);
        r.run("tryFields"); // State management via metadata
    }
    @Test
    void print() {
        Runner r = AnnoteRunner.newRunner(TestClass.class);
        r.run("print");     // Your IDE's syntax highlighter is crying
    }
//    @Test
//    void repl() {
//        Runner r = AnnoteRunner.newRunner(TestClass.class);
//        r.run("repl");      // A calculator that shouldn't exist but does
//    }
}