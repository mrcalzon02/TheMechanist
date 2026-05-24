package mechanist;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Restricted mod loader.  It permits the public mod API and a deliberately small
 * set of harmless Java utility types, while refusing file, process, reflection,
 * networking, native, crypto, and internal engine access.
 */
final class SecureModClassLoader extends URLClassLoader {
    private static final Set<String> EXACT_ALLOWED_CLASSES = Set.of(
            "java.lang.Object",
            "java.lang.String",
            "java.lang.Boolean",
            "java.lang.Byte",
            "java.lang.Short",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Float",
            "java.lang.Double",
            "java.lang.Character",
            "java.lang.Enum",
            "java.lang.Record",
            "java.lang.Number",
            "java.lang.Math",
            "java.lang.Comparable",
            "java.lang.CharSequence",
            "java.lang.Iterable",
            "java.lang.Void",
            "java.lang.AssertionError",
            "java.lang.IllegalArgumentException",
            "java.lang.IllegalStateException",
            "java.lang.NullPointerException",
            "java.lang.UnsupportedOperationException",
            "java.util.ArrayList",
            "java.util.Collections",
            "java.util.Comparator",
            "java.util.HashMap",
            "java.util.HashSet",
            "java.util.LinkedHashMap",
            "java.util.LinkedHashSet",
            "java.util.List",
            "java.util.Map",
            "java.util.Objects",
            "java.util.Optional",
            "java.util.Set",
            "java.util.UUID",
            "java.util.stream.Collectors",
            "java.util.stream.Stream",
            "java.time.Duration",
            "java.time.Instant",
            "java.time.LocalDate",
            "java.time.LocalDateTime",
            "java.time.ZoneOffset"
    );

    private static final List<String> ALLOWED_PREFIXES = List.of(
            "mechanist.modapi.",
            "java.util.function.",
            "java.util.stream.",
            "java.lang.annotation."
    );

    private static final List<String> BLOCKED_PREFIXES = List.of(
            "java.io.",
            "java.net.",
            "java.nio.file.",
            "java.lang.ProcessBuilder",
            "java.lang.Runtime",
            "java.lang.System",
            "java.lang.Thread",
            "java.lang.ThreadGroup",
            "java.lang.ClassLoader",
            "java.lang.Module",
            "java.lang.SecurityManager",
            "java.lang.reflect.",
            "java.lang.invoke.",
            "javax.crypto.",
            "javax.net.",
            "sun.",
            "com.sun.",
            "jdk."
    );

    private final String sandboxId;
    private final ConcurrentMap<String, String> approvedClassHashes = new ConcurrentHashMap<>();

    SecureModClassLoader(URL[] urls, ClassLoader parent) {
        super(Objects.requireNonNull(urls, "urls"), parent);
        this.sandboxId = "mod-sandbox-" + UUID.randomUUID();
    }

    String sandboxId() { return sandboxId; }

    ConcurrentMap<String, String> approvedClassHashes() { return approvedClassHashes; }

    void recordApprovedClassHash(String className, byte[] rawClassBytes) throws IOException {
        approvedClassHashes.put(Objects.requireNonNull(className, "className"), ModPackageValidator.sha256Hex(Objects.requireNonNull(rawClassBytes, "rawClassBytes")));
    }

    @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Objects.requireNonNull(name, "name");
        validateClassAccess(name);
        return super.loadClass(name, resolve);
    }

    static void validateClassAccess(String name) throws ClassNotFoundException {
        for (String blocked : BLOCKED_PREFIXES) {
            if (name.equals(blocked) || name.startsWith(blocked)) {
                throw new ClassNotFoundException("Secure mod sandbox blocked class access: " + name);
            }
        }
        if (name.startsWith("mechanist.") && !name.startsWith("mechanist.modapi.")) {
            throw new ClassNotFoundException("Mods may access mechanist.modapi only, not internal engine class " + name);
        }
        if (name.startsWith("java.")) {
            if (EXACT_ALLOWED_CLASSES.contains(name)) return;
            for (String prefix : ALLOWED_PREFIXES) if (name.startsWith(prefix)) return;
            throw new ClassNotFoundException("Java runtime class is not whitelisted for mods: " + name);
        }
    }

    sealed interface ModVerificationResult permits ModVerificationResult.Accepted, ModVerificationResult.Rejected {
        record Accepted(Path jar, int classCount, long totalBytes, String sha256) implements ModVerificationResult { }
        record Rejected(Path jar, List<String> violations) implements ModVerificationResult { public Rejected { violations = List.copyOf(violations); } }
    }

    static ModVerificationResult verifyJar(Path jar) throws IOException {
        ModPackageValidator.ValidationReport report = ModPackageValidator.validate(jar);
        if (!report.accepted()) return new ModVerificationResult.Rejected(jar, report.violations());
        return new ModVerificationResult.Accepted(jar, report.classCount(), report.totalBytes(), report.sha256());
    }
}
