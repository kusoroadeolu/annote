package com.github.kusoroadeolu.annote.conditions;


import com.github.kusoroadeolu.annote.Expression;
import com.github.kusoroadeolu.annote.Utils;
import com.github.kusoroadeolu.annote.Value;
import com.github.kusoroadeolu.annote.conditions.ConditionExpr.BoolValue;
import com.github.kusoroadeolu.annote.math.ArithmeticExpr;
import com.github.kusoroadeolu.annote.math.ArithmeticExpr.ArithmeticValue;
import com.github.kusoroadeolu.annote.tokenizer.Condition;
import com.github.kusoroadeolu.annote.tokenizer.Operator;
import com.github.kusoroadeolu.annote.tokenizer.SymbolTokenizer;
import com.github.kusoroadeolu.annote.tokenizer.Token;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static com.github.kusoroadeolu.annote.Utils.asBoolean;

public class ConditionParser {

    private static final SymbolTokenizer TOKENIZER = new SymbolTokenizer();

    public static Expression parse(String s) {
        if (s.equals("true")) return new BoolValue(true);
        else if(s.equals("false")) return new BoolValue(false);

        List<Token> tokens = TOKENIZER.reform(s);
        Deque<Expression> dq = new ArrayDeque<>();
        for (Token t : tokens){
            if (t.isNumber()) dq.push(new ArithmeticValue(t.o()));
            else if (t.isOperator()){
                ArithmeticExpr e2 = (ArithmeticExpr) dq.pop();
                ArithmeticExpr e1 = (ArithmeticExpr) dq.pop();
                dq.push(Utils.eval(e1, e2, (Operator) t.o()));
            }else if (t.isCondition()){
                Condition cond = (Condition) t.o();
                if (cond == Condition.NOT) {
                    Expression e1 = dq.pop();
                    dq.push(new ConditionExpr.Not(e1));
                }
                else if (cond == Condition.AND || cond == Condition.OR) {
                    Expression e2 = dq.pop();
                    Expression e1 = dq.pop();
                    dq.push(evaluate(e1, e2, cond));
                }
                else {
                    Expression e2 = dq.pop();
                    Expression e1 = dq.pop();
                    dq.push(evaluateComparison(e1, e2, cond));
                }
            }
        }


        return dq.pop();
    }


    static Expression evaluate(Expression e1, Expression e2, Condition condition){
        return switch (condition){
            case OR -> new ConditionExpr.Or(e1, e2);
            case AND -> new ConditionExpr.And(e1, e2);
            case NOT -> new ConditionExpr.Not(e1);
            default -> throw new IllegalArgumentException("expected '||', '&&', '!'");
        };
    }

    static Expression evaluateComparison(Expression e1, Expression e2, Condition condition){
        return switch (condition){
            case GREATER_THAN -> new ConditionExpr.GreaterThan(e1, e2);
            case GREATER_THAN_OR_EQUALS -> new ConditionExpr.GreaterThanOrEquals(e1, e2);
            case LESS_THAN -> new ConditionExpr.LessThan(e1, e2);
            case LESS_THAN_OR_EQUALS -> new ConditionExpr.LessThanOrEquals(e1, e2);
            case EQUALS -> new ConditionExpr.Equals(e1, e2);
            default -> throw new IllegalArgumentException("expected '>', '>=', '<', '<=', '!'");

        };
    }
}
