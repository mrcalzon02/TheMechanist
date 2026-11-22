package mechanist;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves one honest build identity for client, server, diagnostics, and release surfaces. */
final class BuildIdentityAuthority {
    private static final String VERSION_PROPERTY = "mechanist.version";
    private static final String MAVEN_PROPERTIES = "META-INF/maven/local.mechanist/the-mechanist/pom.properties";
    private static final String DEVELOPMENT_VERSION = "development";
    private static final int MAX_MANIFEST_BYTES = 1024 * 1024;
    private static final Pattern MANIFEST_VERSION = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern MANIFEST_COMMIT = Pattern.compile("\"commit\"\\s*:\\s*\"([a-fA-F0-9]{40})\"");

    static String version() {
        return Holder.VERSION;
    }

    static String commit() {
        return Holder.COMMIT;
    }

    static String shortCommit() {
        return commit().isEmpty() ? "" : commit().substring(0, 12);
    }

    static String clientWindowTitle() {
        String suffix = shortCommit();
        return "The Mechanist " + version() + (suffix.isEmpty() ? "" : " [" + suffix + "]");
    }

    static String componentVersion(String component) {
        String role = normalize(component);
        if (role.isBlank()) role = "runtime";
        return "the-mechanist-" + role + "-" + version();
    }

    static String debugBuildTag(String component) {
        String role = normalize(component);
        if (role.isBlank()) role = "runtime";
        return version() + "-" + role;
    }

    static String auditSummary() {
        return "version=" + version()
                + " commit=" + (commit().isEmpty() ? "unknown" : commit())
                + " source=" + Holder.SOURCE
                + " java=" + System.getProperty("java.version", "unknown");
    }

    static Resolution resolve(String explicit, String implementation, Properties maven) {
        String candidate = normalize(explicit);
        if (!candidate.isBlank()) return new Resolution(candidate, "system-property");

        candidate = normalize(implementation);
        if (!candidate.isBlank()) return new Resolution(candidate, "jar-manifest");

        if (maven != null) {
            candidate = normalize(maven.getProperty("version"));
            if (!candidate.isBlank()) return new Resolution(candidate, "maven-properties");
        }
        return new Resolution(DEVELOPMENT_VERSION, "development-fallback");
    }

    private static Resolution resolveRuntime() {
        Properties maven = new Properties();
        try (InputStream in = BuildIdentityAuthority.class.getClassLoader()
                .getResourceAsStream(MAVEN_PROPERTIES)) {
            if (in != null) maven.load(in);
        } catch (Exception ignored) {
            maven.clear();
        }
        Package ownPackage = BuildIdentityAuthority.class.getPackage();
        String implementation = ownPackage == null ? null : ownPackage.getImplementationVersion();
        return resolve(System.getProperty(VERSION_PROPERTY), implementation, maven);
    }

    /**
     * A SHA is displayed only when the installed distribution manifest agrees
     * with the running JAR's version. A stale or unrelated install never gets
     * presented as the currently executing binary.
     */
    static String parseManifestCommit(String json, String expectedVersion) {
        if (json == null || json.length() > MAX_MANIFEST_BYTES || normalize(expectedVersion).isEmpty()) {
            return "";
        }
        Matcher versionField = MANIFEST_VERSION.matcher(json);
        Matcher commitField = MANIFEST_COMMIT.matcher(json);
        if (!versionField.find() || !expectedVersion.equals(versionField.group(1))
                || !commitField.find()) {
            return "";
        }
        return commitField.group(1).toLowerCase(Locale.ROOT);
    }

    static String readInstalledCommit(Path installRoot, String expectedVersion) {
        if (installRoot == null) return "";
        try {
            Path manifest = installRoot.resolve("manifests").resolve("runtime-manifest.json");
            if (!Files.isRegularFile(manifest) || Files.size(manifest) > MAX_MANIFEST_BYTES) return "";
            return parseManifestCommit(Files.readString(manifest, StandardCharsets.UTF_8), expectedVersion);
        } catch (IOException | RuntimeException ignored) {
            return "";
        }
    }

    private static Path configuredRoot(String property) {
        String raw = System.getProperty(property, "");
        if (raw.isBlank()) return null;
        try {
            return Path.of(raw).toAbsolutePath().normalize();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String resolveInstalledCommit(String expectedVersion) {
        String fromLauncher = readInstalledCommit(configuredRoot("mechanist.launcher.appHome"), expectedVersion);
        if (!fromLauncher.isEmpty()) return fromLauncher;
        String fromInstall = readInstalledCommit(configuredRoot("mechanist.installRoot"), expectedVersion);
        if (!fromInstall.isEmpty()) return fromInstall;
        // Direct package launches may not have a thin-launcher handoff property.
        try {
            URI location = BuildIdentityAuthority.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path jar = Path.of(location).toAbsolutePath().normalize();
            Path client = jar.getParent();
            if (Files.isRegularFile(jar) && jar.getFileName().toString().endsWith(".jar")
                    && client != null && "client".equals(client.getFileName().toString())
                    && client.getParent() != null
                    && "packages".equals(client.getParent().getFileName().toString())) {
                return readInstalledCommit(client.getParent().getParent(), expectedVersion);
            }
        } catch (Exception ignored) {
            // Local IDE runs have no installed release SHA.
        }
        return "";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    record Resolution(String version, String source) { }

    private static final class Holder {
        private static final Resolution RESOLUTION = resolveRuntime();
        private static final String VERSION = RESOLUTION.version();
        private static final String SOURCE = RESOLUTION.source();
        private static final String COMMIT = resolveInstalledCommit(VERSION);
    }

    private BuildIdentityAuthority() { }
}
