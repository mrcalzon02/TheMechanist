package mechanist;

import mechanist.ProGuardMapParser.ClassMapping;
import mechanist.ProGuardMapParser.MethodMapping;
import mechanist.ProGuardMapParser.ProGuardMapping;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Asynchronously reconstructs obfuscated crash stack traces using a parsed ProGuard map. */
public final class StackTraceDeobfuscator {
    private static final Pattern STACK_FRAME_PATTERN = Pattern.compile("^(\\s*at\\s+)([\\w.$/]+)\\.([\\w$<>]+)\\(([^)]*)\\)(.*)$");
    private static final Pattern SOURCE_PATTERN = Pattern.compile("^([^:)]*?)(?::(\\d+))?$");
    private static final Pattern CAUSED_BY_PATTERN = Pattern.compile("^(\\s*(?:Caused by:|Exception in thread \\\"[^\\\"]+\\\")\\s+)([\\w.$/]+)(.*)$");
    private static final Executor DEFAULT_EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread t = new Thread(runnable, "mechanist-stacktrace-deobfuscator");
        t.setDaemon(true);
        return t;
    });

    public CompletableFuture<DeobfuscationResult> deobfuscateAsync(String rawTrace, String buildVersion, ProGuardMapping mapping) {
        return deobfuscateAsync(rawTrace, buildVersion, mapping, DEFAULT_EXECUTOR);
    }

    public CompletableFuture<DeobfuscationResult> deobfuscateAsync(String rawTrace, String buildVersion, ProGuardMapping mapping, Executor executor) {
        Objects.requireNonNull(rawTrace, "rawTrace");
        Objects.requireNonNull(mapping, "mapping");
        Objects.requireNonNull(executor, "executor");
        String safeVersion = buildVersion == null || buildVersion.isBlank() ? "unknown-build" : buildVersion.trim();
        return CompletableFuture.supplyAsync(() -> deobfuscate(rawTrace, safeVersion, mapping), executor);
    }

    public DeobfuscationResult deobfuscate(String rawTrace, String buildVersion, ProGuardMapping mapping) {
        if (rawTrace == null || rawTrace.isBlank()) {
            return new DeobfuscationResult(buildVersion, "[MappingMissingAnomalie empty-input]", 0, 0, 1);
        }
        String[] lines = rawTrace.split("\\R", -1);
        StringBuilder out = new StringBuilder(rawTrace.length() + Math.max(128, lines.length * 16));
        int rebuilt = 0;
        int unchanged = 0;
        int missing = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            LineRewrite rewrite = rewriteLine(line, mapping);
            switch (rewrite.kind()) {
                case STACK_FRAME, THROWABLE_HEADER -> rebuilt++;
                case MISSING_MAPPING -> missing++;
                case UNCHANGED -> unchanged++;
            }
            out.append(rewrite.line());
            if (i + 1 < lines.length) out.append(System.lineSeparator());
        }
        return new DeobfuscationResult(buildVersion, out.toString(), rebuilt, unchanged, missing);
    }

    private LineRewrite rewriteLine(String line, ProGuardMapping mapping) {
        Matcher frame = STACK_FRAME_PATTERN.matcher(line);
        if (frame.matches()) {
            return rewriteStackFrame(line, frame, mapping);
        }
        Matcher header = CAUSED_BY_PATTERN.matcher(line);
        if (header.matches()) {
            String original = ProGuardMapParser.normalizeClassToken(header.group(2));
            Optional<ClassMapping> classMapping = mapping.findClassByObfuscatedName(original);
            if (classMapping.isPresent()) {
                return new LineRewrite(header.group(1) + classMapping.get().originalClassName() + header.group(3), LineKind.THROWABLE_HEADER);
            }
            return new LineRewrite(line, LineKind.UNCHANGED);
        }
        return new LineRewrite(line, LineKind.UNCHANGED);
    }

    private LineRewrite rewriteStackFrame(String originalLine, Matcher frame, ProGuardMapping mapping) {
        String prefix = frame.group(1);
        String obfuscatedClass = ProGuardMapParser.normalizeClassToken(frame.group(2));
        String obfuscatedMethod = frame.group(3);
        SourceRef source = parseSource(frame.group(4));
        String suffix = frame.group(5) == null ? "" : frame.group(5);
        Optional<ClassMapping> classMapping = mapping.findClassByObfuscatedName(obfuscatedClass);
        if (classMapping.isEmpty()) {
            return new LineRewrite(originalLine + " [MappingMissingAnomalie class=" + obfuscatedClass + "]", LineKind.MISSING_MAPPING);
        }
        ClassMapping mappedClass = classMapping.get();
        Optional<MethodMapping> methodMapping = mappedClass.findBestMethod(obfuscatedMethod, source.lineNumber());
        if (methodMapping.isEmpty()) {
            String rebuilt = prefix + mappedClass.originalClassName() + "." + obfuscatedMethod + "(" + source.raw() + ")" + suffix
                    + " [MappingMissingAnomalie method=" + obfuscatedClass + "." + obfuscatedMethod + "]";
            return new LineRewrite(rebuilt, LineKind.MISSING_MAPPING);
        }
        MethodMapping method = methodMapping.get();
        int translatedLine = source.lineNumber() >= 0 ? method.translateLine(source.lineNumber()) : -1;
        String fileName = ProGuardMapParser.simpleJavaFileName(mappedClass.originalClassName());
        String location = source.kind() == SourceKind.NATIVE_METHOD ? "Native Method"
                : translatedLine >= 0 ? fileName + ":" + translatedLine : fileName;
        String rebuilt = prefix + mappedClass.originalClassName() + "." + method.originalName() + "(" + location + ")" + suffix;
        return new LineRewrite(rebuilt, LineKind.STACK_FRAME);
    }

    private static SourceRef parseSource(String text) {
        if (text == null || text.isBlank()) return new SourceRef("Unknown Source", -1, SourceKind.UNKNOWN_SOURCE);
        String trimmed = text.trim();
        if ("Native Method".equals(trimmed)) return new SourceRef(trimmed, -1, SourceKind.NATIVE_METHOD);
        if ("Unknown Source".equals(trimmed)) return new SourceRef(trimmed, -1, SourceKind.UNKNOWN_SOURCE);
        Matcher matcher = SOURCE_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            int line = -1;
            if (matcher.group(2) != null) {
                try { line = Integer.parseInt(matcher.group(2)); } catch (NumberFormatException ignored) { line = -1; }
            }
            return new SourceRef(trimmed, line, SourceKind.FILE_LINE);
        }
        return new SourceRef(trimmed, -1, SourceKind.UNKNOWN_SOURCE);
    }

    public enum LineKind { STACK_FRAME, THROWABLE_HEADER, MISSING_MAPPING, UNCHANGED }
    private enum SourceKind { FILE_LINE, UNKNOWN_SOURCE, NATIVE_METHOD }

    private record SourceRef(String raw, int lineNumber, SourceKind kind) { }
    private record LineRewrite(String line, LineKind kind) { }

    public record DeobfuscationResult(String buildVersion, String reconstructedTrace, int rebuiltLines, int unchangedLines, int missingMappings) {
        public String auditLine() {
            return "build=" + buildVersion + " rebuilt=" + rebuiltLines + " unchanged=" + unchangedLines + " missing=" + missingMappings;
        }
    }
}
