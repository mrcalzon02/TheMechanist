package mechanist.launcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads exact version/commit/platform/hardening identity from packaged manifests. */
final class LauncherBuildIdentity {
    private static final Pattern VERSION = stringField("version");
    private static final Pattern PLATFORM = stringField("platform");
    private static final Pattern COMMIT = stringField("commit");
    private static final Pattern DISTRIBUTION_MODEL = stringField("distributionModel");
    private static final Pattern JAVA_RELEASE = Pattern.compile("\"javaRelease\"\\s*:\\s*(\\d+)");
    private static final Pattern RELEASE_HARDENED = Pattern.compile("\"releaseHardened\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPAT_JAVA_RELEASE = Pattern.compile("\"java_release\"\\s*:\\s*(\\d+)");
    private static final Pattern COMPAT_RELEASE_HARDENED = Pattern.compile("\"release_hardened\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);

    static Identity read(LauncherConfig config) throws IOException {
        if (config == null) throw new IOException("Launcher configuration is unavailable.");
        Path canonical = canonicalManifest(config);
        if (canonical != null) {
            String text = Files.readString(canonical, StandardCharsets.UTF_8);
            return new Identity(
                    field(text, VERSION, "unknown"),
                    field(text, COMMIT, "unknown"),
                    field(text, PLATFORM, "unknown"),
                    intField(text, JAVA_RELEASE, 0),
                    booleanField(text, RELEASE_HARDENED, false),
                    field(text, DISTRIBUTION_MODEL, "installer-thin-launcher-client-server"),
                    canonical,
                    true);
        }

        Path compatibility = config.manifestDir.resolve("launcher-runtime-manifest.json");
        if (Files.isRegularFile(compatibility)) {
            String text = Files.readString(compatibility, StandardCharsets.UTF_8);
            return new Identity(
                    field(text, VERSION, "unknown"),
                    "unknown",
                    field(text, PLATFORM, "unknown"),
                    intField(text, COMPAT_JAVA_RELEASE, 0),
                    booleanField(text, COMPAT_RELEASE_HARDENED, false),
                    "installer-thin-launcher-client-server",
                    compatibility,
                    false);
        }
        throw new IOException("No canonical or launcher-compatible runtime manifest was found under "
                + config.installRoot);
    }

    private static Path canonicalManifest(LauncherConfig config) {
        Path portable = config.manifestDir.resolve("runtime-manifest.json");
        if (Files.isRegularFile(portable)) return portable;
        Path nativeImage = config.installRoot.resolve("certification")
                .resolve("canonical-runtime-manifest.json");
        return Files.isRegularFile(nativeImage) ? nativeImage : null;
    }

    private static Pattern stringField(String name) {
        return Pattern.compile("\\\"" + Pattern.quote(name) + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
    }

    private static String field(String text, Pattern pattern, String fallback) {
        Matcher matcher = pattern.matcher(text == null ? "" : text);
        return matcher.find() && !matcher.group(1).isBlank() ? matcher.group(1).trim() : fallback;
    }

    private static int intField(String text, Pattern pattern, int fallback) {
        Matcher matcher = pattern.matcher(text == null ? "" : text);
        if (!matcher.find()) return fallback;
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static boolean booleanField(String text, Pattern pattern, boolean fallback) {
        Matcher matcher = pattern.matcher(text == null ? "" : text);
        return matcher.find() ? Boolean.parseBoolean(matcher.group(1)) : fallback;
    }

    record Identity(
            String version,
            String commit,
            String platform,
            int javaRelease,
            boolean releaseHardened,
            String distributionModel,
            Path manifest,
            boolean canonical
    ) {
        String compactLine() {
            return "version=" + version
                    + " commit=" + commit
                    + " platform=" + platform
                    + " javaRelease=" + javaRelease
                    + " releaseHardened=" + releaseHardened
                    + " canonical=" + canonical
                    + " manifest=" + manifest;
        }
    }

    private LauncherBuildIdentity() { }
}
