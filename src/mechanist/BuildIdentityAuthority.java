package mechanist;

import java.io.InputStream;
import java.util.Properties;

/** Resolves one honest build identity for client, server, diagnostics, and release surfaces. */
final class BuildIdentityAuthority {
    private static final String VERSION_PROPERTY = "mechanist.version";
    private static final String MAVEN_PROPERTIES = "META-INF/maven/local.mechanist/the-mechanist/pom.properties";
    private static final String DEVELOPMENT_VERSION = "development";

    static String version() {
        return Holder.VERSION;
    }

    static String clientWindowTitle() {
        return "The Mechanist " + version();
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

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    record Resolution(String version, String source) { }

    private static final class Holder {
        private static final Resolution RESOLUTION = resolveRuntime();
        private static final String VERSION = RESOLUTION.version();
        private static final String SOURCE = RESOLUTION.source();
    }

    private BuildIdentityAuthority() { }
}
