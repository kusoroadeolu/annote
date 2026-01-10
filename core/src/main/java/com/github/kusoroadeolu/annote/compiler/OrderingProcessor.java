package com.github.kusoroadeolu.annote.compiler;

import com.sun.source.tree.*;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Annotation processor that generates ordering metadata for Annote annotations.
 * This allows users to write annotations without explicit order= parameters,
 * as the order is captured from source position at compile time.
 *
 * Uses the Compiler Tree API to get actual source positions of annotations,
 * which preserves interleaved order correctly.
 */
@SupportedAnnotationTypes({
    "com.github.kusoroadeolu.annote.annotations.Var",
    "com.github.kusoroadeolu.annote.annotations.Print",
    "com.github.kusoroadeolu.annote.annotations.If",
    "com.github.kusoroadeolu.annote.annotations.Else",
    "com.github.kusoroadeolu.annote.annotations.End",
    "com.github.kusoroadeolu.annote.annotations.Loop",
    "com.github.kusoroadeolu.annote.annotations.Return",
    "com.github.kusoroadeolu.annote.annotations.Yeet",
    "com.github.kusoroadeolu.annote.annotations.ReadLn",
    "com.github.kusoroadeolu.annote.annotations.Concat",
    "com.github.kusoroadeolu.annote.annotations.Call",
    "com.github.kusoroadeolu.annote.annotations.containers.Vars",
    "com.github.kusoroadeolu.annote.annotations.containers.Prints",
    "com.github.kusoroadeolu.annote.annotations.containers.Ifs",
    "com.github.kusoroadeolu.annote.annotations.containers.Elses",
    "com.github.kusoroadeolu.annote.annotations.containers.Ends",
    "com.github.kusoroadeolu.annote.annotations.containers.Loops",
    "com.github.kusoroadeolu.annote.annotations.containers.Returns",
    "com.github.kusoroadeolu.annote.annotations.containers.Yeets",
    "com.github.kusoroadeolu.annote.annotations.containers.ReadLns",
    "com.github.kusoroadeolu.annote.annotations.containers.Concats",
    "com.github.kusoroadeolu.annote.annotations.containers.Calls"
})
@SupportedSourceVersion(SourceVersion.RELEASE_25)
public class OrderingProcessor extends AbstractProcessor {

    private static final Set<String> ANNOTE_ANNOTATION_SIMPLE_NAMES = Set.of(
        "Var", "Print", "If", "Else", "End", "Loop", "Return", "Yeet", "ReadLn", "Concat", "Call"
    );

    // Map: className -> (methodName -> list of annotation signatures in order)
    private final Map<String, Map<String, List<String>>> classOrderings = new LinkedHashMap<>();
    private Trees trees;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        trees = Trees.instance(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            generateOrderingClass();
            return true;
        }

        // Collect all methods with our annotations
        Set<ExecutableElement> methods = new LinkedHashSet<>();
        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element.getKind() == ElementKind.METHOD) {
                    methods.add((ExecutableElement) element);
                }
            }
        }

        // Process each method using tree API
        for (ExecutableElement method : methods) {
            processMethodWithTree(method);
        }

        return true;
    }

    private void processMethodWithTree(ExecutableElement method) {
        MethodTree methodTree = trees.getTree(method);
        if (methodTree == null || methodTree.getModifiers() == null) {
            return;
        }

        CompilationUnitTree compilationUnit = trees.getPath(method).getCompilationUnit();
        SourcePositions sourcePositions = trees.getSourcePositions();

        // Collect and sort annotations by source position
        record AnnotationWithPosition(String signature, long position) {}

        List<String> orderedSignatures = methodTree.getModifiers().getAnnotations().stream()
            .map(annotationTree -> {
                String signature = createSignatureFromTree(annotationTree);
                long pos = sourcePositions.getStartPosition(compilationUnit, annotationTree);
                return signature != null ? new AnnotationWithPosition(signature, pos) : null;
            })
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingLong(AnnotationWithPosition::position))
            .map(AnnotationWithPosition::signature)
            .toList();

        if (!orderedSignatures.isEmpty()) {
            TypeElement classElement = (TypeElement) method.getEnclosingElement();
            String className = classElement.getQualifiedName().toString();
            String methodName = method.getSimpleName().toString();

            classOrderings
                .computeIfAbsent(className, k -> new LinkedHashMap<>())
                .put(methodName, orderedSignatures);
        }
    }

    private String createSignatureFromTree(AnnotationTree annotationTree) {
        String annotationType = annotationTree.getAnnotationType().toString();

        // Extract simple name from potentially qualified type
        int lastDot = annotationType.lastIndexOf('.');
        String simpleName = lastDot >= 0 ? annotationType.substring(lastDot + 1) : annotationType;

        if (!ANNOTE_ANNOTATION_SIMPLE_NAMES.contains(simpleName)) {
            return null;
        }

        // Build sorted key=value pairs
        String values = annotationTree.getArguments().stream()
            .filter(arg -> arg instanceof AssignmentTree)
            .map(arg -> (AssignmentTree) arg)
            .map(a -> a.getVariable() + "=" + a.getExpression())
            .sorted()
            .collect(Collectors.joining(","));

        return "com.github.kusoroadeolu.annote.annotations." + simpleName + "{" + values + "}";
    }

    private void generateOrderingClass() {
        if (classOrderings.isEmpty()) {
            return;
        }

        try {
            JavaFileObject file = processingEnv.getFiler().createSourceFile(
                "com.github.kusoroadeolu.annote.generated.AnnotationOrdering");

            try (PrintWriter out = new PrintWriter(file.openWriter())) {
                out.println("package com.github.kusoroadeolu.annote.generated;");
                out.println();
                out.println("import java.util.*;");
                out.println();
                out.println("/**");
                out.println(" * Generated class containing annotation ordering metadata.");
                out.println(" * This allows annotations to be processed in source order at runtime.");
                out.println(" */");
                out.println("public final class AnnotationOrdering {");
                out.println("    private AnnotationOrdering() {}");
                out.println();
                out.println("    private static final Map<String, Map<String, List<String>>> ORDERINGS = new HashMap<>();");
                out.println();
                out.println("    static {");

                for (var classEntry : classOrderings.entrySet()) {
                    String className = classEntry.getKey();
                    out.println("        Map<String, List<String>> " +
                        sanitizeClassName(className) + "_methods = new HashMap<>();");

                    for (var methodEntry : classEntry.getValue().entrySet()) {
                        String methodName = methodEntry.getKey();
                        List<String> signatures = methodEntry.getValue();

                        String quotedSignatures = signatures.stream()
                            .map(s -> "\"" + escapeString(s) + "\"")
                            .collect(Collectors.joining(", "));

                        out.println("        " + sanitizeClassName(className) + "_methods.put(\"" +
                            methodName + "\", List.of(" + quotedSignatures + "));");
                    }

                    out.println("        ORDERINGS.put(\"" + className + "\", " +
                        sanitizeClassName(className) + "_methods);");
                }

                out.println("    }");
                out.println();
                out.println("    public static List<String> getOrdering(String className, String methodName) {");
                out.println("        Map<String, List<String>> methods = ORDERINGS.get(className);");
                out.println("        if (methods == null) return List.of();");
                out.println("        return methods.getOrDefault(methodName, List.of());");
                out.println("    }");
                out.println("}");
            }

            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                "Generated AnnotationOrdering class with " + classOrderings.size() + " classes");

        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                "Failed to generate AnnotationOrdering: " + e.getMessage());
        }
    }

    private String sanitizeClassName(String className) {
        return className.replace('.', '_').replace('$', '_');
    }

    private String escapeString(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
