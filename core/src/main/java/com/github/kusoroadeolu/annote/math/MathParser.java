package com.github.kusoroadeolu.annote.math;

import com.github.kusoroadeolu.annote.Expression;
import com.github.kusoroadeolu.annote.Utils;
import com.github.kusoroadeolu.annote.tokenizer.Operator;
import com.github.kusoroadeolu.annote.tokenizer.SymbolTokenizer;
import com.github.kusoroadeolu.annote.tokenizer.Token;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static com.github.kusoroadeolu.annote.Utils.asDouble;

public class MathParser {

    private final static SymbolTokenizer TOKENIZER = new SymbolTokenizer();

    public static Expression parse(String s) {
        List<Token> tokens = TOKENIZER.reform(s);
        Deque<ArithmeticExpr> dq = new ArrayDeque<>();
        for (Token t : tokens){
            if (t.isNumber()) dq.push(new ArithmeticExpr.ArithmeticValue(asDouble(t.o())));
            else if (t.isOperator()){
                ArithmeticExpr e2 = dq.pop();
                ArithmeticExpr e1 = dq.pop();
                dq.push(Utils.eval(e1, e2, (Operator) t.o()));
            }
        }
        return dq.pop();
    }


}
