package com.github.kusoroadeolu.annote.annotations.processor;

import com.google.auto.service.AutoService;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;


@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedAnnotationTypes({"com.github.kusoroadeolu.annote.annotations.*"})
@AutoService(Processor.class)
public class AnnotationProcessor extends AbstractProcessor {
    public static final Gson GSON = new GsonBuilder().registerTypeAdapter(AnnotationProcessor.ValueDTO.class, new AnnotationProcessor.ValueDTODeserializer()).setPrettyPrinting().create();

    // fqcn -> class representation
    private static final Map<String, List<ElementRepresentation>> map = new HashMap<>();

    private Trees trees;
    private Elements elementUtils;
    private Types typeUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.trees = Trees.instance(processingEnv);
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annos, RoundEnvironment roundEnv) {
        Set<TypeElement> classes = new HashSet<>();

        for (TypeElement anno : annos) {
            for (var element : roundEnv.getElementsAnnotatedWith(anno)) {
                if (element.getKind() == ElementKind.METHOD) {
                    ExecutableElement method = (ExecutableElement) element;
                    if (method.getEnclosingElement() instanceof TypeElement typeElement)
                        classes.add(typeElement);
                    else
                        println(method.getEnclosingElement().getClass().getName());
                } else if (element.getKind() == ElementKind.CLASS) {
                    if (element instanceof TypeElement typeElement)
                        classes.add(typeElement);
                    else
                        println(element.getClass().getName());
                }
            }
        }

        for (TypeElement typeElement : classes) {
            String fqcn = elementUtils.getBinaryName(typeElement).toString();
            List<ElementRepresentation> classRepresentation = new ArrayList<>();

            TreePath typePath = trees.getPath(typeElement);

            ClassTree classTree = (ClassTree) typePath.getLeaf();
            List<? extends Tree> members = classTree.getMembers();

            for (Tree memberTree : members) {
                javax.lang.model.element.Element memberElement = trees.getElement(new TreePath(typePath, memberTree));

                if (memberElement == null) {
                    continue;
                }

                List<? extends AnnotationMirror> annotations = getAnnotations(memberElement);

                List<AnnotationDTO> annotationStrings = new ArrayList<>();

                for (AnnotationMirror annotation : annotations) {
                    annotationStrings.add(toDTO(annotation, elementUtils));
                }

                switch (memberElement) {
                    case VariableElement field -> {
                        ElementRepresentation representation = new ElementRepresentation("field", field.getSimpleName().toString());
                        representation.annotations = annotationStrings;
                        classRepresentation.add(representation);
                    }
                    case ExecutableElement method -> {
                        ElementRepresentation representation = new ElementRepresentation("method", method.getSimpleName().toString());
                        representation.annotations = annotationStrings;
                        for (VariableElement x : method.getParameters()) {
                            TypeMirror tm = x.asType();
                            TypeElement te = (TypeElement) typeUtils.asElement(tm);
                            representation.parameterNames.add(x.getSimpleName().toString());
                            representation.types.add(te.getQualifiedName().toString());
                        }
                        classRepresentation.add(representation);
                    }
                    case TypeElement inner -> {
                        String binaryName = elementUtils.getBinaryName(inner).toString();
                        ElementRepresentation representation = new ElementRepresentation("innerClass", binaryName);
                        representation.annotations = annotationStrings;
                        classRepresentation.add(representation);
                    }
                    default -> {

                    }
                }
            }

            map.put(fqcn, classRepresentation);
        }

        try {
            if (roundEnv.processingOver()) {
                Filer filer = processingEnv.getFiler();
                FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/kr1v/index.json");
                try (Writer w = file.openWriter()) {
                    GSON.toJson(map, w);
                }
                println("Written map. classes processed: " + map.size());
            }
        } catch (Throwable t) {
            processingEnv.getMessager().printError("Failed to write resource: " + t);
        }

        return false;
    }

    private List<AnnotationMirror> getAnnotations(javax.lang.model.element.Element element) {
        TreePath elementPath = trees.getPath(element);

        if (elementPath == null) {
            return element.getAnnotationMirrors().stream().map(m -> (AnnotationMirror) m).collect(Collectors.toList());
        }

        Tree leaf = elementPath.getLeaf();
        List<? extends AnnotationMirror> mirrors = element.getAnnotationMirrors();
        if (mirrors.isEmpty()) return List.of();

        List<? extends AnnotationTree> annotationTrees = switch (leaf) {
            case ClassTree ct -> ct.getModifiers().getAnnotations();
            case VariableTree vt -> vt.getModifiers().getAnnotations();
            case MethodTree mt -> mt.getModifiers().getAnnotations();
            default -> List.of();
        };

        if (annotationTrees.isEmpty()) {
            return mirrors.stream().map(m -> (AnnotationMirror) m).collect(Collectors.toList());
        }

        Map<String, Deque<AnnotationMirror>> mirrorsByType = new HashMap<>();
        for (AnnotationMirror am : mirrors) {
            String topType = am.getAnnotationType().toString();
            mirrorsByType.computeIfAbsent(topType, _ -> new ArrayDeque<>()).addLast(am);

            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> ev : am.getElementValues().entrySet()) {
                Object val = ev.getValue().getValue();
                if (val instanceof List<?>) {
                    for (Object inner : (List<?>) val) {
                        if (inner instanceof AnnotationMirror innerAm) {
                            String innerType = innerAm.getAnnotationType().toString();
                            mirrorsByType.computeIfAbsent(innerType, _ -> new ArrayDeque<>()).addLast(innerAm);
                        }
                    }
                }
            }
        }

        List<AnnotationMirror> ordered = new ArrayList<>(annotationTrees.size());

        for (AnnotationTree at : annotationTrees) {
            try {
                boolean expanded = false;

                for (ExpressionTree arg : at.getArguments()) {
                    ExpressionTree expr = arg;
                    if (expr instanceof AssignmentTree asg) {
                        expr = asg.getExpression();
                    }

                    if (expr instanceof NewArrayTree newArr) {
                        for (ExpressionTree init : newArr.getInitializers()) {
                            if (init instanceof AnnotationTree innerAt) {
                                TypeMirror innerAnnType = trees.getTypeMirror(new TreePath(elementPath, innerAt.getAnnotationType()));
                                AnnotationMirror matched = null;
                                if (innerAnnType != null) {
                                    for (Deque<AnnotationMirror> dq : mirrorsByType.values()) {
                                        if (dq == null || dq.isEmpty()) continue;
                                        AnnotationMirror candidate = dq.peekFirst();
                                        if (candidate != null && typeUtils.isSameType(candidate.getAnnotationType(), innerAnnType)) {
                                            matched = dq.removeFirst();
                                            break;
                                        }
                                    }
                                }

                                if (matched == null) {
                                    String innerName = innerAt.getAnnotationType().toString();
                                    Deque<AnnotationMirror> dq2 = mirrorsByType.get(innerName);
                                    if (dq2 != null && !dq2.isEmpty()) matched = dq2.removeFirst();
                                }

                                if (matched != null) ordered.add(matched);
                            }
                        }
                        expanded = true;
                        break;
                    }
                }

                if (expanded) continue;

                TypeMirror annType = trees.getTypeMirror(new TreePath(elementPath, at.getAnnotationType()));
                AnnotationMirror matched = null;
                if (annType != null) {
                    for (Deque<AnnotationMirror> dq : mirrorsByType.values()) {
                        if (dq == null || dq.isEmpty()) continue;
                        AnnotationMirror candidate = dq.peekFirst();
                        if (candidate != null && typeUtils.isSameType(candidate.getAnnotationType(), annType)) {
                            matched = dq.removeFirst();
                            break;
                        }
                    }
                }

                if (matched == null) {
                    String annName = at.getAnnotationType().toString();
                    Deque<AnnotationMirror> dq = mirrorsByType.get(annName);
                    if (dq != null && !dq.isEmpty()) matched = dq.removeFirst();
                }

                if (matched != null) ordered.add(matched);
            } catch (Exception ex) {
                ordered.add(null);
            }
        }


        for (Deque<AnnotationMirror> dq : mirrorsByType.values()) {
            while (!dq.isEmpty()) {
                ordered.add(dq.removeFirst());
            }
        }

        return ordered;
    }

    private void println(Object o) {
        processingEnv.getMessager().printNote(o == null ? "null" : o.toString());
    }

    public static class Element {
        public final Field field;
        public final Method method;
        public final Class<?> aClass;

        public final List<Annotation> annotations = new ArrayList<>();

        public Element(Field field, Method method, Class<?> aClass) {
            this.field = field;
            this.method = method;
            this.aClass = aClass;
        }
    }

    public static class ElementRepresentation {
        public String type;
        public String name;
        public List<AnnotationDTO> annotations = new ArrayList<>();
        public List<String> types = new ArrayList<>();
        public List<String> parameterNames = new ArrayList<>();

        public ElementRepresentation(String type, String name) {
            this.type = type;
            this.name = name;
        }
    }

    public static final class AnnotationDTO {
        public String annotationType;
        public Map<String, ValueDTO> values;
    }
    public static final class ValueDTO {
        public String kind;
        public Object value;
    }
    public static class ArrayDTO {
        public List<ValueDTO> values;
    }
    public static class EnumDTO {
        public String enumType;
        public String constant;
    }
    public static class ClassDTO {
        public String className;
    }

    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A toAnnotation(AnnotationDTO dto, Class<?> annotationClass) throws ClassNotFoundException {
        if (dto == null) throw new IllegalArgumentException("dto == null");
        if (annotationClass == null) throw new IllegalArgumentException("annotationClass == null");
        if (!annotationClass.isAnnotation()) throw new IllegalArgumentException(annotationClass + " is not an annotation type");

        Map<String, Object> values = new LinkedHashMap<>();
        ClassLoader cl = annotationClass.getClassLoader();

        for (Method m : annotationClass.getDeclaredMethods()) {
            String name = m.getName();
            ValueDTO vdto = dto.values == null ? null : dto.values.get(name);
            Object value;
            if (vdto != null) {
                value = convertValue(vdto, m.getReturnType(), cl);
            } else {
                Object def = m.getDefaultValue();
                if (def != null) {
                    value = def;
                } else {
                    throw new IllegalArgumentException("No value provided for annotation member '" + name + "' of " + annotationClass);
                }
            }
            values.put(name, value);
        }

        InvocationHandler handler = new AnnotationHandler((Class<? extends Annotation>) annotationClass, values);
        return (A) Proxy.newProxyInstance(cl, new Class[]{annotationClass, Annotation.class}, handler);
    }

    private static Object convertValue(ValueDTO vdto, Class<?> expectedType, ClassLoader cl) throws ClassNotFoundException {
        if (vdto == null) return null;
        String kind = vdto.kind;
        Object raw = vdto.value;

        if (expectedType.isArray()) {
            if (!"array".equals(kind)) throw new IllegalArgumentException("Expected array for " + expectedType + " but got " + kind);
            ArrayDTO arr = (ArrayDTO) raw;
            Class<?> comp = expectedType.getComponentType();
            Object array = Array.newInstance(comp, arr.values.size());
            for (int i = 0; i < arr.values.size(); i++) {
                ValueDTO elem = arr.values.get(i);
                Object converted = convertValue(elem, comp, cl);
                Array.set(array, i, converted);
            }
            return array;
        }

        switch (kind) {
            case "string":
                return raw;
            case "primitive":
                return coercePrimitive(raw, expectedType);
            case "enum": {
                EnumDTO e = (EnumDTO) raw;
                Class<?> enumClazz = Class.forName(e.enumType, false, cl);
                @SuppressWarnings({"rawtypes", "unchecked"})
                Enum<?> enumVal = Enum.valueOf((Class) enumClazz, e.constant);
                return enumVal;
            }
            case "class": {
                ClassDTO c = (ClassDTO) raw;
                return Class.forName(c.className, false, cl);
            }
            case "annotation": {
                AnnotationDTO nested = (AnnotationDTO) raw;
                // expectedType should be an annotation type
                @SuppressWarnings("unchecked")
                Class<? extends Annotation> annType = (Class<? extends Annotation>) Class.forName(nested.annotationType, false, cl);
                return toAnnotation(nested, annType);
            }
            case "array":
                ArrayDTO arr = (ArrayDTO) raw;
                Object[] out = new Object[arr.values.size()];
                for (int i = 0; i < arr.values.size(); i++) out[i] = convertValue(arr.values.get(i), Object.class, cl);
                return out;
            default:
                throw new IllegalArgumentException("Unknown ValueDTO.kind: " + kind);
        }
    }

    private static Object coercePrimitive(Object raw, Class<?> expectedType) {
        if (expectedType == String.class) return String.valueOf(raw);
        if (expectedType == boolean.class || expectedType == Boolean.class) return raw;
        if (expectedType == byte.class || expectedType == Byte.class) return ((Number) raw).byteValue();
        if (expectedType == short.class || expectedType == Short.class) return ((Number) raw).shortValue();
        if (expectedType == int.class || expectedType == Integer.class) return ((Number) raw).intValue();
        if (expectedType == long.class || expectedType == Long.class) return ((Number) raw).longValue();
        if (expectedType == float.class || expectedType == Float.class) return ((Number) raw).floatValue();
        if (expectedType == double.class || expectedType == Double.class) return ((Number) raw).doubleValue();
        if (expectedType == char.class || expectedType == Character.class) {
            if (raw instanceof Character) return raw;
            String s = String.valueOf(raw);
            if (s.isEmpty()) return '\u0000';
            return s.charAt(0);
        }
        return raw;
    }

    private static final class AnnotationHandler implements InvocationHandler {
        private final Class<? extends Annotation> type;
        private final Map<String, Object> values; // member name -> value

        AnnotationHandler(Class<? extends Annotation> type, Map<String, Object> values) {
            this.type = type;
            this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if (name.equals("annotationType") && method.getParameterCount() == 0) return type;
            if (name.equals("toString") && method.getParameterCount() == 0) return buildToString();
            if (name.equals("hashCode") && method.getParameterCount() == 0) return buildHashCode();
            if (name.equals("equals") && method.getParameterCount() == 1) return buildEquals(args[0]);

            // annotation member accessor
            if (values.containsKey(name)) return values.get(name);
            throw new IllegalStateException("Unknown method on annotation proxy: " + method);
        }

        private boolean buildEquals(Object other) {
            if (other == this) return true;
            if (!(other instanceof Annotation oAnn)) return false;
            if (!oAnn.annotationType().equals(type)) return false;

            for (Method m : type.getDeclaredMethods()) {
                try {
                    Object v1 = values.get(m.getName());
                    Object v2 = m.invoke(oAnn);
                    if (!memberEquals(v1, v2)) return false;
                } catch (Exception ex) {
                    return false;
                }
            }
            return true;
        }

        private int buildHashCode() {
            int result = 0;
            for (Method m : type.getDeclaredMethods()) {
                Object v = values.get(m.getName());
                int nameHash = 127 * m.getName().hashCode();
                int valueHash = memberHash(v);
                result += (nameHash ^ valueHash);
            }
            return result;
        }

        private String buildToString() {
            String members = Arrays.stream(type.getDeclaredMethods())
                    .map(m -> {
                        Object v = values.get(m.getName());
                        return m.getName() + "=" + memberToString(v);
                    })
                    .collect(Collectors.joining(", "));
            return "@" + type.getName() + "(" + members + ")";
        }

        private static boolean memberEquals(Object a, Object b) {
            if (a == null) return b == null;
            if (a.getClass().isArray()) {
                if (b == null || !b.getClass().isArray()) return false;
                return deepArrayEquals(a, b);
            }
            return Objects.equals(a, b);
        }

        private static boolean deepArrayEquals(Object a, Object b) {
            Class<?> ac = a.getClass().getComponentType();
            if (ac.isPrimitive()) {
                return switch (a) {
                    case int[] ints -> Arrays.equals(ints, (int[]) b);
                    case long[] longs -> Arrays.equals(longs, (long[]) b);
                    case short[] shorts -> Arrays.equals(shorts, (short[]) b);
                    case byte[] bytes -> Arrays.equals(bytes, (byte[]) b);
                    case char[] chars -> Arrays.equals(chars, (char[]) b);
                    case boolean[] booleans -> Arrays.equals(booleans, (boolean[]) b);
                    case float[] floats -> Arrays.equals(floats, (float[]) b);
                    case double[] doubles -> Arrays.equals(doubles, (double[]) b);
                    default -> false;
                };
            }
            return Arrays.deepEquals((Object[]) a, (Object[]) b);
        }

        private static int memberHash(Object v) {
            if (v == null) return 0;
            if (v.getClass().isArray()) {
                Class<?> comp = v.getClass().getComponentType();
                if (comp.isPrimitive()) {
                    switch (v) {
                        case int[] ints -> {
                            return Arrays.hashCode(ints);
                        }
                        case long[] longs -> {
                            return Arrays.hashCode(longs);
                        }
                        case short[] shorts -> {
                            return Arrays.hashCode(shorts);
                        }
                        case byte[] bytes -> {
                            return Arrays.hashCode(bytes);
                        }
                        case char[] chars -> {
                            return Arrays.hashCode(chars);
                        }
                        case boolean[] booleans -> {
                            return Arrays.hashCode(booleans);
                        }
                        case float[] floats -> {
                            return Arrays.hashCode(floats);
                        }
                        case double[] doubles -> {
                            return Arrays.hashCode(doubles);
                        }
                        default -> {
                        }
                    }
                } else {
                    return Arrays.deepHashCode((Object[]) v);
                }
            }
            return Objects.hashCode(v);
        }

        private static String memberToString(Object v) {
            if (v == null) return "null";
            if (v.getClass().isArray()) {
                Class<?> comp = v.getClass().getComponentType();
                if (comp.isPrimitive()) {
                    switch (v) {
                        case int[] ints -> {
                            return Arrays.toString(ints);
                        }
                        case long[] longs -> {
                            return Arrays.toString(longs);
                        }
                        case short[] shorts -> {
                            return Arrays.toString(shorts);
                        }
                        case byte[] bytes -> {
                            return Arrays.toString(bytes);
                        }
                        case char[] chars -> {
                            return Arrays.toString(chars);
                        }
                        case boolean[] booleans -> {
                            return Arrays.toString(booleans);
                        }
                        case float[] floats -> {
                            return Arrays.toString(floats);
                        }
                        case double[] doubles -> {
                            return Arrays.toString(doubles);
                        }
                        default -> {
                        }
                    }
                }
                return Arrays.toString((Object[]) v);
            }
            return String.valueOf(v);
        }
    }

    public static final class ValueDTODeserializer implements JsonDeserializer<ValueDTO> {
        @Override
        public ValueDTO deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String kind = obj.has("kind") ? obj.get("kind").getAsString() : null;
            ValueDTO v = new ValueDTO();
            v.kind = kind;

            if (!obj.has("value") || obj.get("value").isJsonNull()) {
                v.value = null;
                return v;
            }

            JsonElement valueElem = obj.get("value");

            try {
                assert kind != null;
                switch (kind) {
                    case "string" -> v.value = valueElem.getAsString();
                    case "primitive" -> {
                        JsonPrimitive p = valueElem.getAsJsonPrimitive();
                        if (p.isBoolean()) v.value = p.getAsBoolean();
                        else if (p.isNumber()) {
                            v.value = p.getAsNumber();
                        } else {
                            v.value = p.getAsString();
                        }
                    }
                    case "enum" -> v.value = ctx.deserialize(valueElem, EnumDTO.class);
                    case "class" -> v.value = ctx.deserialize(valueElem, ClassDTO.class);
                    case "annotation" -> v.value = ctx.deserialize(valueElem, AnnotationDTO.class);
                    case "array" -> v.value = ctx.deserialize(valueElem, ArrayDTO.class);
                    default -> v.value = ctx.deserialize(valueElem, Object.class);
                }
            } catch (JsonParseException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new JsonParseException("Failed to deserialize ValueDTO kind=" + kind, ex);
            }
            return v;
        }
    }


    AnnotationDTO toDTO(AnnotationMirror mirror, Elements elements) {
        AnnotationDTO dto = new AnnotationDTO();
        TypeElement annType = (TypeElement) mirror.getAnnotationType().asElement();
        dto.annotationType = elements.getBinaryName(annType).toString();
        dto.values = new LinkedHashMap<>();

        for (var entry : mirror.getElementValues().entrySet()) {
            String name = entry.getKey().getSimpleName().toString();
            dto.values.put(name, toValueDTO(entry.getValue(), elements));
        }
        return dto;
    }

    ValueDTO toValueDTO(AnnotationValue av, Elements elements) {
        Object v = av.getValue();
        ValueDTO dto = new ValueDTO();

        if (v instanceof String s) {
            dto.kind = "string";
            dto.value = s;
        } else if (v instanceof Integer || v instanceof Long ||
                v instanceof Short   || v instanceof Byte ||
                v instanceof Boolean || v instanceof Character ||
                v instanceof Double  || v instanceof Float) {
            dto.kind = "primitive";
            dto.value = v;
        } else if (v instanceof VariableElement ve) {
            EnumDTO e = new EnumDTO();
            TypeElement enumType = (TypeElement) ve.getEnclosingElement();
            e.enumType = elements.getBinaryName(enumType).toString();
            e.constant = ve.getSimpleName().toString();
            dto.kind = "enum";
            dto.value = e;
        } else if (v instanceof TypeMirror tm) {
            ClassDTO c = new ClassDTO();

            if (tm.getKind() == TypeKind.DECLARED) {
                DeclaredType dt = (DeclaredType) tm;
                TypeElement te = (TypeElement) dt.asElement();
                c.className = elements.getBinaryName(te).toString();
            } else {
                c.className = tm.toString();
            }

            dto.kind = "class";
            dto.value = c;
        } else if (v instanceof AnnotationMirror nested) {
            dto.kind = "annotation";
            dto.value = toDTO(nested, elements);
        } else if (v instanceof List<?> list) {
            ArrayDTO arr = new ArrayDTO();
            arr.values = new ArrayList<>();
            for (Object o : list) {
                arr.values.add(toValueDTO((AnnotationValue) o, elements));
            }
            dto.kind = "array";
            dto.value = arr;
        } else {
            throw new IllegalStateException("Unknown annotation member type: " + v);
        }
        return dto;
    }

    // === runtime stuff ===

    public static void init() {
        Type type = new TypeToken<Map<String, List<ElementRepresentation>>>() {}.getType();
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> mods = cl.getResources("META-INF/kr1v/index.json");
            while (mods.hasMoreElements()) {
                URL mod = mods.nextElement();
                try (Reader reader = new InputStreamReader(mod.openStream(), StandardCharsets.UTF_8)) {
                    Map<String, List<ElementRepresentation>> map = GSON.fromJson(reader, type);

                    for (String clazz : map.keySet()) {
                        classToRepresentation.put(Class.forName(clazz), map.get(clazz));
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static final Map<Class<?>, List<ElementRepresentation>> classToRepresentation = new HashMap<>();

    public static List<Element> getDeclaredElements(Class<?> clazz) {
        List<Element> elementsOfClass = new ArrayList<>();
        List<ElementRepresentation> elementRepresentations = classToRepresentation.get(clazz);

        try {
            assert elementRepresentations != null;
            for (ElementRepresentation el : elementRepresentations) {
                Field f = null;
                Method m = null;
                Class<?> klass = null;
                switch (el.type) {
                    case "field" -> f = clazz.getDeclaredField(el.name);
                    case "innerClass" -> klass = Class.forName(el.name);
                    case "method" -> {
                        if (el.name.contains("<")) continue;
                        try {
                            List<Class<?>> typeList = new ArrayList<>();
                            for (String type : el.types) {
                                typeList.add(Class.forName(type));
                            }
                            Class<?>[] types = typeList.toArray(new Class[]{});
                            m = clazz.getDeclaredMethod(el.name, types);
                        } catch (ClassNotFoundException ignored) {
                            // cant really do anything.... mapping issues, compare and method name and parameter names/length
                            outer: for (Method method : clazz.getDeclaredMethods()) {
                                if (method.getParameterCount() != el.types.size()) continue;
                                if (!method.getName().equals(el.name)) continue;

                                var params = method.getParameters();
                                for (int i = 0; i < params.length; i++) {
                                    if (!params[i].getName().equals(el.parameterNames.get(i))) {
                                        continue outer;
                                    }
                                }

                                m = method;
                                break;
                            }
                        }
                    }
                    default -> {}
                }

                Element element = new Element(f, m, klass);
                for (AnnotationDTO ann : el.annotations) {
                    element.annotations.add(toAnnotation(ann, Class.forName(ann.annotationType)));
                }
                elementsOfClass.add(element);
            }
        } catch (NoSuchFieldException | ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        return elementsOfClass;
    }

    public static Annotation[] getAnnotations(Method m) {
        Class<?> declaring = m.getDeclaringClass();
        var elements = AnnotationProcessor.getDeclaredElements(declaring);
        for (Element element : elements) {
            if (element.method != null) {
                Method method = element.method;
                if (method.equals(m)) {
                    return element.annotations.toArray(new Annotation[0]);
                }
            }
        }
        throw new IllegalArgumentException();
    }
}
