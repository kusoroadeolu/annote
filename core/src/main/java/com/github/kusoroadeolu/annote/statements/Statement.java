package com.github.kusoroadeolu.annote.statements;

import com.github.kusoroadeolu.annote.Expression;
import com.github.kusoroadeolu.annote.Type;
import com.github.kusoroadeolu.annote.Utils;
import com.github.kusoroadeolu.annote.conditions.ConditionParser;
import com.github.kusoroadeolu.annote.exception.AnnoteException;
import com.github.kusoroadeolu.annote.math.ArithmeticExpr;
import com.github.kusoroadeolu.annote.math.ArithmeticExpr.ArithmeticValue;
import com.github.kusoroadeolu.annote.math.MathParser;
import com.github.kusoroadeolu.annote.statements.Result.None;
import com.github.kusoroadeolu.annote.statements.Result.ReturnValue;

import java.util.List;

import static com.github.kusoroadeolu.annote.Type.*;
import static com.github.kusoroadeolu.annote.Utils.asBoolean;
import static com.github.kusoroadeolu.annote.Utils.print;
import static java.lang.String.valueOf;

//Strings are immutable, cannot be concat, how to
public interface Statement {
   Result execute(Scope scope);

   default Expression getExpr(String string, Scope scope  , Type type){
       String rebuilt = Utils.insertVariables(string, scope);
       return switch (type){
           case NUMBER -> MathParser.parse(rebuilt);
           case BOOLEAN -> ConditionParser.parse(rebuilt);
           case STRING -> new ArithmeticValue(string);
       };
   }

    default String getValue(String str, Scope s){
        Variable v = s.get(str);
        return (v == null) ? str : valueOf(v.obj());
    }


     record VarDeclaration(String name, Type type, String string) implements Statement{
        @Override
        public Result execute(Scope scope) {
            Expression expr = getExpr(string, scope, type);
            scope.put(name, new Variable(type, expr.evaluate().value()));
            return new None();
        }
    }

    record IfStatement(String string, List<Statement> ifBlock, List<Statement> elseBlock) implements Statement {
        @Override
        public Result execute(Scope scope) {
            Expression condition = getExpr(string, scope, BOOLEAN);
            List<Statement> block = asBoolean(condition.evaluate().value()) ? ifBlock : elseBlock;
            Scope inner = scope.newScope();
            for (Statement s : block) {
                Result result = s.execute(inner);
                if (result instanceof ReturnValue) {
                    return result;
                }
            }

            return new None();
        }
    }
    record PrintStatement(String string, Type type) implements Statement {
        @Override
        public Result execute(Scope scope) {
            Expression expression = getExpr(string, scope, type);
            Object o = expression.evaluate().value();
            if (o instanceof String s) print(getValue(s, scope));
            else print(o);
            return new None();
        }
    }
    record LoopStatement(String string, List<Statement> body) implements Statement {
        @Override
        public Result execute(Scope scope) {
            Scope inner = scope.newScope();
            Expression condition = getExpr(string, inner, BOOLEAN);
            while (asBoolean(condition.evaluate().value())){
                for (Statement s : body) {
                    Result result = s.execute(inner);
                    if (result instanceof ReturnValue) {
                        return result;
                    }
                }
                condition = getExpr(string, inner, BOOLEAN);
            }

            return new None();
        }
    }

    record ReturnStatement(String string, Type type) implements Statement{
        @Override
        public Result execute(Scope scope) {
            Expression expression = getExpr(string, scope, type);
            return new ReturnValue(expression.evaluate().value());
        }
    }

    record YeetStatement(String value) implements Statement{
        @Override
        public Result execute(Scope scope) {
            throw new RuntimeException(value);
        }
    }

    record ReadLnStatement(String prompt, String name, Type type) implements Statement{
        @Override
        public Result execute(Scope scope) {
            String s = IO.readln(prompt);
            Expression e = getExpr(s, scope, type);
            scope.put(name, new Variable(type, e.evaluate().value()));
            return new None();
        }
    }

    record ConcatStatement(String[] strings, String name) implements Statement{

        @Override
        public Result execute(Scope scope) {
            if (strings.length == 0) return new None();
            String s = getValue(strings[0], scope);
            ArithmeticExpr expr = new ArithmeticValue(s);

            for (int i = 1; i < strings.length; ++i){
                String val = getValue(strings[i], scope);
                expr = new ArithmeticExpr.Add(expr, new ArithmeticValue(val));
            }

            scope.put(name, new Variable(STRING, expr.evaluate().value()));
            return new None();
        }
    }

    record CallStatement(String methodName, String type, String assignTo, Class<?> clazz) implements Statement{
        @Override
        public Result execute(Scope scope) {
            if (methodName.isBlank()) throw new AnnoteException("Method name cannot be empty");
            AnnotationParser ap = new AnnotationParser(clazz);
            Result r = ap.run(methodName);
            if (!type.equalsIgnoreCase("void") && (r instanceof ReturnValue(Object value))){
                Type t = Type.fromString(type);
                scope.put(assignTo, new Variable(t, value));
                return r;
            }

            return new None();
        }
    }
}
