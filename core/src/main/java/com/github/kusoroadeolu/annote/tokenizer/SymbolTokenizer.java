package com.github.kusoroadeolu.annote.tokenizer;

import java.util.*;

import static com.github.kusoroadeolu.annote.tokenizer.Operator.fromChar;
import static java.lang.Double.parseDouble;

//So while the head of the stack has a less or equal precedence to the current symbol, we pop them off the stack onto the list

public class SymbolTokenizer {
    private final static char DOT = '.';
    private final static Set<Character> SET = Set.of('+', '-', '/', '%', '^', '*', ')', '(');
    private final Deque<Symbol> symbols = new ArrayDeque<>();
    private final StringBuilder builder = new StringBuilder();


    // Note: brackets handled separately
    public List<Token> reform(String str){
        final List<Token> output = new ArrayList<>();
        for (int i = 0; i < str.length(); ++i){
            char c = str.charAt(i);
            if (c == ' ') continue;

            if (isNum(c)){
                builder.append(c);
            } else { //Is a symbol

                if (!builder.isEmpty()){
                    double d = parseDouble(builder.toString());
                    output.add(new Token(d));
                    builder.setLength(0);
                }

                Symbol curr;
                if (isOperator(c)) {
                    curr = fromChar(c);
                }else if(isCondition(c + "" + str.charAt(i + 1))){
                    char next = str.charAt(i + 1);
                    String ss = c + "" + next; //Check if the next char forms a condition
                    ++i; //Move forward
                    curr = Condition.fromString(ss);

                }else if (isCondition(c)){
                    curr = Condition.fromChar(c); //Else just stick to this char

                }else{
                    throw new IllegalArgumentException("Symbol: %s not found ".formatted(c));
                }
                //END OF IF STATEMENT


                if (curr.isLeftBracket()){
                    symbols.push(curr);
                    continue;
                }

                Symbol op = symbols.peek();
                if (op != null && curr.isRightBracket()){ //If this a right bracket,
                    // push and then loop through the deque popping symbols until we reach the left bracket
                    symbols.push(curr);
                    while (op != null && !op.isLeftBracket()){
                        Symbol add = symbols.pop();
                        if (add.isBracket()) continue;
                        output.add(new Token(add));
                        op = symbols.peek();
                    }
                    continue;
                }


                while (op != null && curr.hasLessOrEqualPrecedence(op)){
                    output.add(new Token(symbols.pop()));
                    op = symbols.peek();
                }

                symbols.push(curr);
            }
        }
        //END OF LOOP


        if (!builder.isEmpty()){
            double d = parseDouble(builder.toString());
            builder.setLength(0);
            output.add(new Token(d)); //Add the last number
        }

        while (!symbols.isEmpty()){
            Symbol o = symbols.pop(); //Pop the remaining ops onto the list
            if (o.isBracket()) continue;
            output.add(new Token(o));
        }

        return output;

    }

    public static boolean isOperator(char c){
        return SET.contains(c);
    }

    public static boolean isCondition(String s){
        return Condition.CONDITION_MAP.containsKey(s);
    }

    public static boolean isCondition(char c){
        return isCondition(String.valueOf(c));
    }

    public static boolean isNum(char c){
        return Character.isDigit(c) || c == DOT;
    }
}
