package com.github.kusoroadeolu.annote.conditions;

import com.github.kusoroadeolu.annote.Expression;
import com.github.kusoroadeolu.annote.Value;

import static com.github.kusoroadeolu.annote.Utils.*;

public interface ConditionExpr extends Expression {
    Value evaluate();

    record BoolValue(Object value) implements ConditionExpr, Value{
        @Override
        public BoolValue evaluate() {
            return this;
        }
    }

    record Equals(Expression e1, Expression e2) implements ConditionExpr{
        @Override
        public BoolValue evaluate() {
            Object o1 = e1.evaluate().value();
            Object o2 = e2.evaluate().value();
            return new BoolValue(o1.equals(o2));
        }
    }

    record GreaterThan(Expression e1, Expression e2) implements ConditionExpr {
        @Override
        public BoolValue evaluate() {
            Object o1 = e1.evaluate().value();
            Object o2 = e2.evaluate().value();
            ensureNotBoolean(o1, o2);
            ensureNotString(o1, o2);
            return new BoolValue(asDouble(o1) > asDouble(o2));
        }
    }

    record LessThan(Expression e1, Expression e2) implements ConditionExpr {
        @Override
        public BoolValue evaluate() {
            Object o1 = e1.evaluate().value();
            Object o2 = e2.evaluate().value();
            ensureNotBoolean(o1, o2);
            ensureNotString(o1, o2);
            return new BoolValue(asDouble(o1) < asDouble(o2));
        }
    }



    record GreaterThanOrEquals(Expression e1, Expression e2) implements ConditionExpr {
        @Override
        public BoolValue evaluate() {
            Object o1 = e1.evaluate().value();
            Object o2 = e2.evaluate().value();
            ensureNotBoolean(o1, o2);
            ensureNotString(o1, o2);
            return new BoolValue(asDouble(o1) >= asDouble(o2));
        }
    }

    record LessThanOrEquals(Expression e1, Expression e2) implements ConditionExpr {
        @Override
        public BoolValue evaluate() {
            Object o1 = e1.evaluate().value();
            Object o2 = e2.evaluate().value();
            ensureNotBoolean(o1, o2);
            ensureNotString(o1, o2);
            return new BoolValue(asDouble(o1) <= asDouble(o2));
        }
    }

    record Not(Expression e1) implements ConditionExpr {
        @Override
        public BoolValue evaluate() {
            return new BoolValue(!asBoolean(e1.evaluate().value()));
        }
    }

    record And(Expression e1, Expression e2) implements ConditionExpr{
        @Override
        public BoolValue evaluate() {
            boolean b1 = asBoolean(e1.evaluate().value());
            boolean b2 = asBoolean(e2.evaluate().value());
            return new BoolValue(b1 && b2);
        }
    }

    record Or(Expression e1, Expression e2) implements ConditionExpr{
        @Override
        public BoolValue evaluate() {
            boolean b1 = asBoolean(e1.evaluate().value());
            boolean b2 = asBoolean(e2.evaluate().value());
            return new BoolValue(b1 || b2);
        }
    }
}