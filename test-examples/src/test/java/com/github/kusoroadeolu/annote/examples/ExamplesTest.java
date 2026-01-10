package com.github.kusoroadeolu.annote.examples;

import com.github.kusoroadeolu.annote.AnnoteRunner;
import com.github.kusoroadeolu.annote.statements.Result;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Annote examples.
 * These tests verify that the annotation processor correctly captures
 * source order and the examples execute as expected.
 */
class ExamplesTest {

    /**
     * Captures stdout during execution and returns the output.
     */
    private String captureOutput(Runnable action) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        try {
            action.run();
            return baos.toString().trim();
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testFizzBuzz() {
        String output = captureOutput(() -> {
            AnnoteRunner.newRunner(FizzBuzz.class).run("run");
        });

        String[] lines = output.split("\n");
        assertEquals(100, lines.length, "FizzBuzz should output 100 lines");

        // Check some specific values (numbers print as doubles)
        assertEquals("1.0", lines[0].trim());
        assertEquals("2.0", lines[1].trim());
        assertEquals("Fizz", lines[2].trim());     // 3
        assertEquals("4.0", lines[3].trim());
        assertEquals("Buzz", lines[4].trim());     // 5
        assertEquals("Fizz", lines[5].trim());     // 6
        assertEquals("FizzBuzz", lines[14].trim()); // 15
        assertEquals("FizzBuzz", lines[29].trim()); // 30
        assertEquals("Buzz", lines[99].trim());    // 100
    }

    @Test
    void testConditionalBigNumber() {
        String output = captureOutput(() -> {
            AnnoteRunner.newRunner(SimpleExamples.class).run("conditional");
        });

        assertEquals("Big number", output, "x=10 should print 'Big number'");
    }

    @Test
    void testSimpleLoop() {
        String output = captureOutput(() -> {
            AnnoteRunner.newRunner(SimpleExamples.class).run("simpleLoop");
        });

        String[] lines = output.split("\n");
        assertEquals(5, lines.length, "Loop should output 5 lines");
        assertEquals("0.0", lines[0].trim());
        assertEquals("1.0", lines[1].trim());
        assertEquals("2.0", lines[2].trim());
        assertEquals("3.0", lines[3].trim());
        assertEquals("4.0", lines[4].trim());
    }

    @Test
    void testAddition() {
        String output = captureOutput(() -> {
            AnnoteRunner.newRunner(SimpleExamples.class).run("addition");
        });

        assertEquals("8.0", output, "5 + 3 should equal 8.0");
    }

    @Test
    void testReturnValue() {
        Result result = AnnoteRunner.newRunner(SimpleExamples.class).run("returnValue");

        assertInstanceOf(Result.ReturnValue.class, result);
        Result.ReturnValue rv = (Result.ReturnValue) result;
        assertEquals(42.0, rv.value(), "Return value should be 42.0");
    }
}
