package com.github.kusoroadeolu.annote.statements;

import com.github.kusoroadeolu.annote.Runner;
import com.github.kusoroadeolu.annote.Type;
import com.github.kusoroadeolu.annote.annotations.*;
import com.github.kusoroadeolu.annote.annotations.containers.*;
import com.github.kusoroadeolu.annote.exception.AnnoteException;
import com.github.kusoroadeolu.annote.statements.Result.None;
import com.github.kusoroadeolu.annote.statements.Result.ReturnValue;
import com.github.kusoroadeolu.annote.statements.Statement.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

import static com.github.kusoroadeolu.annote.Type.fromString;

public record AnnotationParser(Class<?> clazz) implements Runner {

    @Override
    public Result run(String methodName) {
        return this.read(methodName, new Scope(new HashMap<>(), null), true);
    }

    // Cache for generated ordering class
    private static final Class<?> orderingClass;
    private static final Method getOrderingMethod;

    static {
        Class<?> clazz = null;
        Method method = null;
        try {
            clazz = Class.forName("com.github.kusoroadeolu.annote.generated.AnnotationOrdering");
            method = clazz.getMethod("getOrdering", String.class, String.class);
        } catch (Exception e) {
            // Generated class not available - will use fallback
        }
        orderingClass = clazz;
        getOrderingMethod = method;
    }

    public Result read(String methodName, Scope rootScope, boolean addFields) {
        Method method;
        try {
            method = clazz.getMethod(methodName);
        } catch (NoSuchMethodException e) {
            throw new AnnoteException(e);
        }

        Annotation[] rawAnnotations = method.getDeclaredAnnotations();

        // Get ordered annotations using generated ordering or fallback
        Annotation[] annotations = getOrderedAnnotations(clazz.getName(), methodName, rawAnnotations);

        Annotation[] fields = clazz.getDeclaredAnnotations();
        if (addFields) {
            addFields(fields, rootScope);
        }

        var ls = parseAnnotations(annotations);
        for (Statement stmt : ls) {
            Result result = stmt.execute(rootScope);
            if (result instanceof ReturnValue rv) {
                return rv;
            }
        }

        return new None();
    }

    /**
     * Gets annotations in the correct order using generated ordering metadata.
     */
    static Annotation[] getOrderedAnnotations(String className, String methodName, Annotation[] rawAnnotations) {
        // First, flatten all container annotations into individual annotations
        List<Annotation> flatList = flattenAnnotations(rawAnnotations);

        // Use generated ordering
        if (orderingClass != null && getOrderingMethod != null) {
            try {
                @SuppressWarnings("unchecked")
                List<String> ordering = (List<String>) getOrderingMethod.invoke(null, className, methodName);

                if (!ordering.isEmpty()) {
                    return orderByGeneratedMetadata(flatList, ordering);
                }
            } catch (Exception e) {
                // Fall through
            }
        }

        // If no generated ordering available, return in container order (within each type)
        return flatList.toArray(new Annotation[0]);
    }

    /**
     * Flattens container annotations into their individual annotations.
     */
    static List<Annotation> flattenAnnotations(Annotation[] annotations) {
        List<Annotation> result = new ArrayList<>();
        for (Annotation a : annotations) {
            if (a instanceof Vars vars) {
                result.addAll(Arrays.asList(vars.value()));
            } else if (a instanceof Prints prints) {
                result.addAll(Arrays.asList(prints.value()));
            } else if (a instanceof Ifs ifs) {
                result.addAll(Arrays.asList(ifs.value()));
            } else if (a instanceof Ends ends) {
                result.addAll(Arrays.asList(ends.value()));
            } else if (a instanceof Loops loops) {
                result.addAll(Arrays.asList(loops.value()));
            } else if (a instanceof Returns returns) {
                result.addAll(Arrays.asList(returns.value()));
            } else if (a instanceof Elses elses) {
                result.addAll(Arrays.asList(elses.value()));
            } else if (a instanceof Yeets yeets) {
                result.addAll(Arrays.asList(yeets.value()));
            } else if (a instanceof ReadLns readLns) {
                result.addAll(Arrays.asList(readLns.value()));
            } else if (a instanceof Concats concats) {
                result.addAll(Arrays.asList(concats.value()));
            } else if (a instanceof Calls calls) {
                result.addAll(Arrays.asList(calls.value()));
            } else if (isAnnoteAnnotation(a)) {
                result.add(a);
            }
        }
        return result;
    }

    /**
     * Orders annotations based on generated metadata signatures.
     * Uses partial matching to handle default values - generated signatures
     * only contain explicitly specified values, while runtime signatures
     * include defaults.
     */
    static Annotation[] orderByGeneratedMetadata(List<Annotation> annotations, List<String> ordering) {
        List<Annotation> available = new ArrayList<>(annotations);
        List<Annotation> result = new ArrayList<>();

        for (String generatedSig : ordering) {
            for (int i = 0; i < available.size(); i++) {
                if (signatureMatches(available.get(i), generatedSig)) {
                    result.add(available.remove(i));
                    break;
                }
            }
        }

        // Add any remaining annotations not in ordering
        result.addAll(available);
        return result.toArray(new Annotation[0]);
    }

    /**
     * Checks if a runtime annotation matches a generated signature.
     * The generated signature may omit default values, so we check that
     * all values in the generated signature match the annotation's values.
     */
    static boolean signatureMatches(Annotation a, String generatedSig) {
        // Parse the generated signature
        String annotationType = generatedSig.substring(0, generatedSig.indexOf('{'));
        if (!a.annotationType().getName().equals(annotationType)) {
            return false;
        }

        // Parse the values from the generated signature
        String valuesStr = generatedSig.substring(generatedSig.indexOf('{') + 1, generatedSig.length() - 1);
        if (valuesStr.isEmpty()) {
            return true; // No values to check (e.g., @Else{})
        }

        Map<String, String> generatedValues = parseSignatureValues(valuesStr);

        // Check each generated value matches the annotation
        for (var entry : generatedValues.entrySet()) {
            String key = entry.getKey();
            String expectedValue = entry.getValue();

            try {
                Method m = a.annotationType().getMethod(key);
                Object actualValue = m.invoke(a);
                String formattedActual = formatValue(actualValue);

                if (!expectedValue.equals(formattedActual)) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    /**
     * Parses signature values like: name="x",type="num",value="10"
     * Handles quoted strings and array values that may contain commas.
     */
    static Map<String, String> parseSignatureValues(String valuesStr) {
        Map<String, String> values = new HashMap<>();
        int pos = 0;

        while (pos < valuesStr.length()) {
            int eqPos = valuesStr.indexOf('=', pos);
            if (eqPos < 0) break;

            String key = valuesStr.substring(pos, eqPos);
            int valueStart = eqPos + 1;
            int valueEnd = findValueEnd(valuesStr, valueStart);

            values.put(key, valuesStr.substring(valueStart, valueEnd));

            pos = valueEnd;
            if (pos < valuesStr.length() && valuesStr.charAt(pos) == ',') pos++;
        }

        return values;
    }

    /**
     * Finds the end position of a value in a signature string.
     * Handles quoted strings, arrays with brackets, and simple values.
     */
    private static int findValueEnd(String str, int start) {
        if (start >= str.length()) return start;

        char firstChar = str.charAt(start);

        if (firstChar == '"') {
            // Quoted string - find unescaped closing quote
            for (int i = start + 1; i < str.length(); i++) {
                if (str.charAt(i) == '"' && str.charAt(i - 1) != '\\') {
                    return i + 1;
                }
            }
            return str.length();
        }

        if (firstChar == '[') {
            // Array - find matching closing bracket
            int depth = 1;
            for (int i = start + 1; i < str.length() && depth > 0; i++) {
                if (str.charAt(i) == '[') depth++;
                else if (str.charAt(i) == ']') depth--;
                if (depth == 0) return i + 1;
            }
            return str.length();
        }

        // Unquoted value - find comma or end
        int commaPos = str.indexOf(',', start);
        return commaPos < 0 ? str.length() : commaPos;
    }

    static String formatValue(Object value) {
        if (value instanceof String s) {
            return "\"" + s + "\"";
        } else if (value instanceof Class<?> c) {
            return c.getName() + ".class";
        } else if (value.getClass().isArray()) {
            StringBuilder sb = new StringBuilder("[");
            int len = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < len; i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatValue(java.lang.reflect.Array.get(value, i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return String.valueOf(value);
    }

    static boolean isAnnoteAnnotation(Annotation a) {
        String name = a.annotationType().getName();
        return name.startsWith("com.github.kusoroadeolu.annote.annotations.") &&
               !name.contains(".containers.");
    }

    public static List<Statement> parseAnnotations(Annotation[] annotations) {
        List<Statement> program = new ArrayList<>();
        Deque<Block> blockStack = new ArrayDeque<>();
        blockStack.push(new Block(program, null));

        for (Annotation a : annotations) {
            if (a instanceof If i) {
                handleIfStmt(i, blockStack);
            } else if (a instanceof Else) {
                handleElse(blockStack);
            } else if (a instanceof End) {
                blockStack.pop();
            } else if (a instanceof Var v) {
                handleVar(blockStack, v);
            } else if (a instanceof Print p) {
                blockStack.peek().add(new Statement.PrintStatement(p.value(), fromString(p.type())));
            } else if (a instanceof Loop l) {
                handleLoop(blockStack, l);
            } else if (a instanceof Return r) {
                handleReturn(blockStack, r);
            } else if (a instanceof Yeet y) {
                blockStack.peek().add(new YeetStatement(y.value()));
            } else if (a instanceof ReadLn r) {
                blockStack.peek().add(new ReadLnStatement(r.prompt(), r.assignTo(), fromString(r.type())));
            } else if (a instanceof Concat c) {
                blockStack.peek().add(new ConcatStatement(c.value(), c.assignTo()));
            } else if (a instanceof Call c) {
                blockStack.peek().add(new CallStatement(c.methodName(), c.returnType(), c.assignTo(), c.clazz()));
            }
        }
        return program;
    }

    static void addFields(Annotation[] fields, Scope rootScope) {
        for (Annotation a : fields) {
            if (a instanceof Fields fs) {
                for (Field f : fs.value()) {
                    rootScope.put(f.name(), new Variable(Type.fromString(f.type()), f.value()));
                }
            } else if (a instanceof Field f) {
                rootScope.put(f.name(), new Variable(Type.fromString(f.type()), f.value()));
            }
        }
    }

    static void handleIfStmt(If i, Deque<Block> blockStack) {
        List<Statement> ifBlock = new ArrayList<>();
        List<Statement> elseBlock = new ArrayList<>();
        IfStatement ifStmt = new IfStatement(i.condition(), ifBlock, elseBlock);
        blockStack.peek().add(ifStmt);
        blockStack.push(new Block(ifStmt.ifBlock(), ifStmt));
    }

    static void handleElse(Deque<Block> blockStack) {
        Block ifBlock = blockStack.pop();
        IfStatement ifStmt = (IfStatement) ifBlock.parentStatement();
        blockStack.push(new Block(ifStmt.elseBlock(), ifStmt));
    }

    static void handleVar(Deque<Block> blockStack, Var v) {
        Statement varDecl = new Statement.VarDeclaration(v.name(), fromString(v.type()), v.value());
        blockStack.peek().add(varDecl);
    }

    static void handleLoop(Deque<Block> blockStack, Loop l) {
        List<Statement> body = new ArrayList<>();
        LoopStatement loopStmt = new LoopStatement(l.condition(), body);
        blockStack.peek().add(loopStmt);
        blockStack.push(new Block(body, loopStmt));
    }

    static void handleReturn(Deque<Block> blockStack, Return r) {
        ReturnStatement statement = new ReturnStatement(r.value(), fromString(r.type()));
        blockStack.peek().add(statement);
    }
}
