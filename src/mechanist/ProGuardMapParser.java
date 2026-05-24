package mechanist;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses ProGuard mapping.txt data into fast bi-directional lookup structures. */
public final class ProGuardMapParser {
    private static final Pattern CLASS_PATTERN = Pattern.compile("^\\s*([^\\s].+?)\\s+->\\s+([^\\s:]+):\\s*$");
    private static final Pattern METHOD_PATTERN = Pattern.compile("^\\s+(?:(\\d+):(\\d+):)?(.+?)\\s+([\\w$<>]+)\\(([^)]*)\\)(?::(\\d+):(\\d+))?\\s+->\\s+([\\w$<>]+)\\s*$");
    private static final Pattern FIELD_PATTERN = Pattern.compile("^\\s+(.+?)\\s+([\\w$]+)\\s+->\\s+([\\w$]+)\\s*$");
    private static final Executor DEFAULT_EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread t = new Thread(runnable, "mechanist-proguard-map-parser");
        t.setDaemon(true);
        return t;
    });

    public CompletableFuture<ProGuardMapping> parseAsync(Path mappingFile) {
        return parseAsync(mappingFile, DEFAULT_EXECUTOR);
    }

    public CompletableFuture<ProGuardMapping> parseAsync(Path mappingFile, Executor executor) {
        Objects.requireNonNull(mappingFile, "mappingFile");
        Objects.requireNonNull(executor, "executor");
        return CompletableFuture.supplyAsync(() -> {
            try {
                return parse(mappingFile);
            } catch (IOException ex) {
                throw new ProGuardMapParseException("Unable to parse ProGuard mapping file: " + mappingFile, ex);
            }
        }, executor);
    }

    public ProGuardMapping parse(Path mappingFile) throws IOException {
        Path normalized = mappingFile.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized)) {
            throw new IOException("Mapping file does not exist or is not a regular file: " + normalized);
        }
        Map<String, ClassMappingBuilder> byObfuscated = new ConcurrentHashMap<>();
        Map<String, ClassMappingBuilder> byOriginal = new ConcurrentHashMap<>();
        ClassMappingBuilder current = null;
        int lineNumber = 0;
        int classCount = 0;
        int methodCount = 0;
        int fieldCount = 0;
        int ignoredCount = 0;
        try (BufferedReader reader = Files.newBufferedReader(normalized, StandardCharsets.UTF_8)) {
            String raw;
            while ((raw = reader.readLine()) != null) {
                lineNumber++;
                if (raw.isBlank() || raw.trim().startsWith("#")) {
                    ignoredCount++;
                    continue;
                }
                Matcher classMatcher = CLASS_PATTERN.matcher(raw);
                if (classMatcher.matches()) {
                    String originalClass = normalizeClassToken(classMatcher.group(1));
                    String obfuscatedClass = normalizeClassToken(classMatcher.group(2));
                    current = new ClassMappingBuilder(originalClass, obfuscatedClass);
                    byObfuscated.put(obfuscatedClass, current);
                    byOriginal.put(originalClass, current);
                    classCount++;
                    continue;
                }
                if (current == null) {
                    ignoredCount++;
                    continue;
                }
                Matcher methodMatcher = METHOD_PATTERN.matcher(raw);
                if (methodMatcher.matches()) {
                    MethodMapping method = parseMethod(methodMatcher, current.originalClass(), current.obfuscatedClass(), lineNumber);
                    current.addMethod(method);
                    methodCount++;
                    continue;
                }
                Matcher fieldMatcher = FIELD_PATTERN.matcher(raw);
                if (fieldMatcher.matches() && !raw.contains("(")) {
                    FieldMapping field = new FieldMapping(
                            current.originalClass(), current.obfuscatedClass(),
                            fieldMatcher.group(1).trim(), fieldMatcher.group(2).trim(), fieldMatcher.group(3).trim());
                    current.addField(field);
                    fieldCount++;
                    continue;
                }
                ignoredCount++;
            }
        }
        Map<String, ClassMapping> immutableByObf = new ConcurrentHashMap<>();
        Map<String, ClassMapping> immutableByOrig = new ConcurrentHashMap<>();
        for (ClassMappingBuilder builder : byObfuscated.values()) {
            ClassMapping mapping = builder.toImmutable();
            immutableByObf.put(mapping.obfuscatedClassName(), mapping);
            immutableByOrig.put(mapping.originalClassName(), mapping);
        }
        MappingMetrics metrics = new MappingMetrics(normalized, classCount, methodCount, fieldCount, ignoredCount);
        return new ProGuardMapping(immutableByObf, immutableByOrig, metrics);
    }

    public ProGuardMapping parseText(String mappingText, String syntheticSourceName) throws IOException {
        Path temp = Files.createTempFile("mechanist-proguard-map-", ".txt");
        try {
            Files.writeString(temp, Objects.requireNonNull(mappingText, "mappingText"), StandardCharsets.UTF_8);
            return parse(temp);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static MethodMapping parseMethod(Matcher matcher, String originalClass, String obfuscatedClass, int mappingLineNumber) {
        Integer originalStart = parseIntOrNull(matcher.group(1));
        Integer originalEnd = parseIntOrNull(matcher.group(2));
        String returnType = matcher.group(3).trim();
        String originalMethod = matcher.group(4).trim();
        String args = matcher.group(5).trim();
        Integer obfuscatedStart = parseIntOrNull(matcher.group(6));
        Integer obfuscatedEnd = parseIntOrNull(matcher.group(7));
        String obfuscatedMethod = matcher.group(8).trim();
        return new MethodMapping(originalClass, obfuscatedClass, returnType, originalMethod, args, obfuscatedMethod,
                originalStart, originalEnd, obfuscatedStart, obfuscatedEnd, mappingLineNumber);
    }

    private static Integer parseIntOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    static String normalizeClassToken(String token) {
        return token == null ? "" : token.trim().replace('/', '.');
    }

    public static String simpleJavaFileName(String className) {
        if (className == null || className.isBlank()) return "UnknownSource.java";
        int dot = className.lastIndexOf('.');
        String simple = dot >= 0 ? className.substring(dot + 1) : className;
        int nested = simple.indexOf('$');
        if (nested > 0) simple = simple.substring(0, nested);
        if (simple.isBlank()) simple = "UnknownSource";
        return simple + ".java";
    }

    public record MappingMetrics(Path sourceFile, int classCount, int methodCount, int fieldCount, int ignoredLineCount) {
        public String auditLine() {
            return "source=" + sourceFile + " classes=" + classCount + " methods=" + methodCount + " fields=" + fieldCount + " ignored=" + ignoredLineCount;
        }
    }

    public record FieldMapping(String originalClassName, String obfuscatedClassName, String typeName, String originalName, String obfuscatedName) { }

    public record MethodMapping(
            String originalClassName,
            String obfuscatedClassName,
            String returnType,
            String originalName,
            String originalArguments,
            String obfuscatedName,
            Integer originalStartLine,
            Integer originalEndLine,
            Integer obfuscatedStartLine,
            Integer obfuscatedEndLine,
            int mappingLineNumber) {
        public int translateLine(int obfuscatedLine) {
            if (obfuscatedLine < 0) return -1;
            if (obfuscatedStartLine != null && obfuscatedEndLine != null && originalStartLine != null) {
                if (obfuscatedLine >= obfuscatedStartLine && obfuscatedLine <= obfuscatedEndLine) {
                    return Math.max(1, originalStartLine + (obfuscatedLine - obfuscatedStartLine));
                }
            }
            if (originalStartLine != null) return originalStartLine;
            return obfuscatedLine;
        }

        public String originalSignature() {
            return originalName + "(" + originalArguments + ")";
        }
    }

    public record ClassMapping(
            String originalClassName,
            String obfuscatedClassName,
            Map<String, List<MethodMapping>> methodsByObfuscatedName,
            Map<String, FieldMapping> fieldsByObfuscatedName) {
        public Optional<MethodMapping> findBestMethod(String obfuscatedMethodName, int obfuscatedLine) {
            List<MethodMapping> methods = methodsByObfuscatedName.getOrDefault(obfuscatedMethodName, List.of());
            if (methods.isEmpty()) return Optional.empty();
            if (obfuscatedLine >= 0) {
                for (MethodMapping method : methods) {
                    Integer start = method.obfuscatedStartLine();
                    Integer end = method.obfuscatedEndLine();
                    if (start != null && end != null && obfuscatedLine >= start && obfuscatedLine <= end) {
                        return Optional.of(method);
                    }
                }
            }
            return Optional.of(methods.get(0));
        }
    }

    public record ProGuardMapping(
            Map<String, ClassMapping> classesByObfuscatedName,
            Map<String, ClassMapping> classesByOriginalName,
            MappingMetrics metrics) {
        public ProGuardMapping {
            classesByObfuscatedName = Collections.unmodifiableMap(new ConcurrentHashMap<>(classesByObfuscatedName));
            classesByOriginalName = Collections.unmodifiableMap(new ConcurrentHashMap<>(classesByOriginalName));
        }

        public Optional<ClassMapping> findClassByObfuscatedName(String obfuscatedClassName) {
            return Optional.ofNullable(classesByObfuscatedName.get(normalizeClassToken(obfuscatedClassName)));
        }

        public Optional<ClassMapping> findClassByOriginalName(String originalClassName) {
            return Optional.ofNullable(classesByOriginalName.get(normalizeClassToken(originalClassName)));
        }
    }

    public static final class ProGuardMapParseException extends RuntimeException {
        public ProGuardMapParseException(String message, Throwable cause) { super(message, cause); }
    }

    private static final class ClassMappingBuilder {
        private final String originalClass;
        private final String obfuscatedClass;
        private final Map<String, List<MethodMapping>> methodsByObfuscatedName = new ConcurrentHashMap<>();
        private final Map<String, FieldMapping> fieldsByObfuscatedName = new ConcurrentHashMap<>();

        ClassMappingBuilder(String originalClass, String obfuscatedClass) {
            this.originalClass = originalClass;
            this.obfuscatedClass = obfuscatedClass;
        }

        String originalClass() { return originalClass; }
        String obfuscatedClass() { return obfuscatedClass; }

        void addMethod(MethodMapping method) {
            methodsByObfuscatedName.computeIfAbsent(method.obfuscatedName(), ignored -> Collections.synchronizedList(new ArrayList<>())).add(method);
        }

        void addField(FieldMapping field) {
            fieldsByObfuscatedName.put(field.obfuscatedName(), field);
        }

        ClassMapping toImmutable() {
            Map<String, List<MethodMapping>> methodCopy = new ConcurrentHashMap<>();
            for (Map.Entry<String, List<MethodMapping>> entry : methodsByObfuscatedName.entrySet()) {
                List<MethodMapping> sorted = new ArrayList<>(entry.getValue());
                sorted.sort(Comparator.comparingInt(m -> m.obfuscatedStartLine() == null ? Integer.MAX_VALUE : m.obfuscatedStartLine()));
                methodCopy.put(entry.getKey(), List.copyOf(sorted));
            }
            return new ClassMapping(originalClass, obfuscatedClass, Map.copyOf(methodCopy), Map.copyOf(fieldsByObfuscatedName));
        }
    }
}
