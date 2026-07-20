package mechanist.launcher;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

final class DiagnosticReporter {
    private static final String ISSUE_URL = "https://github.com/mrcalzon02/TheMechanist/issues/new";
    private final LauncherConfig config;

    DiagnosticReporter(LauncherConfig config) {
        this.config = config;
    }

    DiagnosticReport prepare(PackageTier graphicsTier, PackageTier audioTier,
                             String packageSource, String recentLauncherLog) throws IOException {
        Files.createDirectories(config.logsDir);
        Files.createDirectories(config.cacheDir);
        Files.createDirectories(config.settingsDir);
        String clientHash = clientHash();
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        Path reportDir = config.cacheDir.resolve("diagnostics")
                .resolve(timestamp.replace(':', '-'));
        Files.createDirectories(reportDir);
        Path reportFile = reportDir.resolve("diagnostic-report.md");

        ModStateDetector.ModState modState = ModStateDetector.inspect(config);
        BuildReadout build = readBuildIdentity();
        PackageReadout packages = readPackageIdentity();
        String body = buildReport(
                clientHash,
                timestamp,
                graphicsTier,
                audioTier,
                packageSource,
                recentLauncherLog,
                modState,
                build,
                packages);
        Files.writeString(reportFile, body, StandardCharsets.UTF_8);
        return new DiagnosticReport(
                clientHash,
                reportFile,
                body,
                modState.modded(),
                build.version,
                build.commit,
                build.platform,
                build.releaseHardened);
    }

    void openIssueDraft(DiagnosticReport report) throws IOException {
        String prefix = report.modded ? "Modded limited-alpha report " : "Limited-alpha report ";
        String title = prefix + report.version + " / " + shortCommit(report.commit)
                + " / " + report.clientHash;
        String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
        String encodedBody = URLEncoder.encode(report.body, StandardCharsets.UTF_8);
        URI uri = URI.create(ISSUE_URL + "?title=" + encodedTitle + "&body=" + encodedBody);
        if (!Desktop.isDesktopSupported()) {
            throw new IOException("Desktop integration is unavailable; report written to "
                    + report.reportFile);
        }
        Desktop.getDesktop().browse(uri);
    }

    private String buildReport(
            String clientHash,
            String timestamp,
            PackageTier graphicsTier,
            PackageTier audioTier,
            String packageSource,
            String recentLauncherLog,
            ModStateDetector.ModState modState,
            BuildReadout build,
            PackageReadout packages
    ) {
        StringBuilder sb = new StringBuilder(20_000);
        sb.append("## The Mechanist limited-alpha diagnostic report\n\n");
        sb.append("> Attach this report only to the private/limited playtest channel approved for this build. ")
                .append("Review redaction before posting.\n\n");
        if (modState.modded()) {
            sb.append("## Modified-content support notice\n\n");
            sb.append(ModStateDetector.supportWarning()).append("\n\n");
            sb.append("Detected mod evidence:\n");
            for (String item : modState.evidence()) {
                sb.append("- `").append(safe(item)).append("`\n");
            }
            sb.append("\n");
        }
        sb.append("Generated: `").append(timestamp).append("`\n\n");

        sb.append("### Candidate build identity\n\n");
        sb.append("- Version: `").append(safe(build.version)).append("`\n");
        sb.append("- Source commit: `").append(safe(build.commit)).append("`\n");
        sb.append("- Platform package: `").append(safe(build.platform)).append("`\n");
        sb.append("- Java release target: `").append(build.javaRelease).append("`\n");
        sb.append("- Release hardened: `").append(build.releaseHardened).append("`\n");
        sb.append("- Distribution model: `").append(safe(build.distributionModel)).append("`\n");
        sb.append("- Canonical identity available: `").append(build.canonical).append("`\n");
        sb.append("- Identity manifest: `").append(redactHome(build.manifest)).append("`\n");
        if (!build.error.isBlank()) {
            sb.append("- Identity read error: `").append(safe(build.error)).append("`\n");
        }
        sb.append("\n");

        sb.append("### Package verification\n\n");
        sb.append("- Ready: `").append(packages.ready).append("`\n");
        sb.append("- Manifest: `").append(redactHome(packages.manifest)).append("`\n");
        sb.append("- Support libraries declared: `").append(packages.supportLibraries).append("`\n");
        sb.append("- Verification summary: `").append(safe(packages.summary)).append("`\n\n");

        sb.append("### Client identity\n\n");
        sb.append("- Client hash: `").append(clientHash).append("`\n");
        sb.append("- Raw client UUID is stored locally and is not included in this report.\n\n");

        sb.append("### Runtime\n\n");
        sb.append("- OS: `").append(safe(System.getProperty("os.name"))).append(" ")
                .append(safe(System.getProperty("os.version"))).append("`\n");
        sb.append("- Architecture: `").append(safe(System.getProperty("os.arch"))).append("`\n");
        sb.append("- Java: `").append(safe(System.getProperty("java.version"))).append("`\n");
        sb.append("- Java vendor: `").append(safe(System.getProperty("java.vendor"))).append("`\n\n");

        sb.append("### Launcher selections\n\n");
        sb.append("- Package source: `").append(safe(packageSource)).append("`\n");
        sb.append("- Remote acquisition advertised: `")
                .append(LauncherDistributionPolicy.remoteAcquisitionEnabled()).append("`\n");
        sb.append("- Distribution policy: `")
                .append(safe(LauncherDistributionPolicy.auditSummary(config))).append("`\n");
        sb.append("- Graphics tier: `")
                .append(graphicsTier == null ? "<none>" : graphicsTier.id).append("`\n");
        sb.append("- Audio tier: `")
                .append(audioTier == null ? "<none>" : audioTier.id).append("`\n");
        sb.append("- Modified content detected: `").append(modState.modded()).append("`\n\n");

        sb.append("### Paths\n\n");
        sb.append("- Install root: `").append(redactHome(config.installRoot)).append("`\n");
        sb.append("- Package root: `").append(redactHome(config.packageRoot)).append("`\n");
        sb.append("- Saves: `").append(redactHome(config.saveDir)).append("`\n");
        sb.append("- Settings: `").append(redactHome(config.settingsDir)).append("`\n");
        sb.append("- Logs: `").append(redactHome(config.logsDir)).append("`\n");
        sb.append("- Cache: `").append(redactHome(config.cacheDir)).append("`\n");
        sb.append("- Package seed: `").append(redactHome(config.packageSeedRoot)).append("`\n\n");

        sb.append("### File checks\n\n");
        appendExists(sb, "Runtime manifests", config.manifestDir);
        appendExists(sb, "Client package root", config.clientPackageDir);
        appendExists(sb, "Server package root", config.serverPackageDir);
        appendExists(sb, "Support libraries", config.supportLibraryDir);
        appendExists(sb, "Client jar", config.clientPackageDir.resolve("TheMechanist.jar"));
        appendExists(sb, "Server jar", config.serverPackageDir.resolve("TheMechanistServer.jar"));
        appendExists(sb, "Canonical native-image certification",
                config.installRoot.resolve("certification").resolve("canonical-runtime-manifest.json"));
        appendExists(sb, "Client music manifest",
                config.clientPackageDir.resolve("assets/music/music_manifest.tsv"));
        appendExists(sb, "Client music WAV root",
                config.clientPackageDir.resolve("assets/music/wav"));
        sb.append("\n");

        sb.append("### Reproduction\n\n");
        sb.append("- What were you trying to do?\n");
        sb.append("- What did you expect?\n");
        sb.append("- What happened instead?\n");
        sb.append("- Can you reproduce it after a clean restart?\n");
        sb.append("- Does it reproduce in a new save?\n");
        sb.append("- Severity: blocker / major / moderate / minor / cosmetic\n\n");

        sb.append("### Recent launcher log excerpt\n\n```text\n");
        sb.append(redactSecrets(tail(recentLauncherLog, 12_000)));
        sb.append("\n```\n\n");

        sb.append("### Recent local log files\n\n");
        for (Path path : recentFiles(config.logsDir, 6)) {
            sb.append("#### `").append(redactHome(path)).append("`\n\n```text\n");
            sb.append(redactSecrets(tailFile(path, 8_000)));
            sb.append("\n```\n\n");
        }
        return sb.toString();
    }

    private BuildReadout readBuildIdentity() {
        try {
            LauncherBuildIdentity.Identity identity = LauncherBuildIdentity.read(config);
            return new BuildReadout(
                    identity.version(),
                    identity.commit(),
                    identity.platform(),
                    identity.javaRelease(),
                    identity.releaseHardened(),
                    identity.distributionModel(),
                    identity.manifest(),
                    identity.canonical(),
                    "");
        } catch (Exception exception) {
            return new BuildReadout(
                    "unknown",
                    "unknown",
                    "unknown",
                    0,
                    false,
                    "unknown",
                    config.manifestDir,
                    false,
                    exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
        }
    }

    private PackageReadout readPackageIdentity() {
        try {
            PackageInstallService.PackageIdentity identity =
                    new PackageInstallService(config).verifyInstalledPackages();
            return new PackageReadout(
                    identity.ready(),
                    identity.manifest(),
                    identity.supportLibraries() == null ? 0 : identity.supportLibraries().size(),
                    identity.summary());
        } catch (Exception exception) {
            return new PackageReadout(
                    false,
                    config.manifestDir,
                    0,
                    exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
        }
    }

    private String clientHash() throws IOException {
        Path idFile = config.settingsDir.resolve("launcher-client.id");
        String id;
        if (Files.isRegularFile(idFile)) {
            id = Files.readString(idFile, StandardCharsets.UTF_8).trim();
        } else {
            id = UUID.randomUUID().toString();
            Files.writeString(idFile, id, StandardCharsets.UTF_8);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(id.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes).substring(0, 16);
        } catch (Exception exception) {
            throw new IOException("Could not generate client hash", exception);
        }
    }

    private static void appendExists(StringBuilder sb, String label, Path path) {
        sb.append("- ").append(label).append(": `")
                .append(Files.exists(path) ? "present" : "missing")
                .append("` — `").append(redactHome(path)).append("`\n");
    }

    private static List<Path> recentFiles(Path root, int limit) {
        if (root == null || !Files.isDirectory(root)) return List.of();
        try (Stream<Path> stream = Files.walk(root, 2)) {
            return stream.filter(Files::isRegularFile)
                    .sorted(Comparator.comparingLong(DiagnosticReporter::modified).reversed())
                    .limit(limit)
                    .toList();
        } catch (Exception exception) {
            return List.of();
        }
    }

    private static long modified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static String tailFile(Path path, int maxChars) {
        try {
            return tail(Files.readString(path, StandardCharsets.UTF_8), maxChars);
        } catch (Exception exception) {
            return "<could not read log: " + exception.getMessage() + ">";
        }
    }

    private static String tail(String text, int maxChars) {
        if (text == null) return "";
        if (text.length() <= maxChars) return text;
        return "<truncated to last " + maxChars + " characters>\n"
                + text.substring(text.length() - maxChars);
    }

    private static String redactHome(Path path) {
        if (path == null) return "<unavailable>";
        String value = path.toAbsolutePath().normalize().toString();
        String home = System.getProperty("user.home");
        if (home != null && !home.isBlank()) value = value.replace(home, "<USER_HOME>");
        return value;
    }

    private static String redactSecrets(String value) {
        if (value == null) return "";
        return value.replaceAll(
                "(?i)(token|password|secret|authorization|cookie|session[_-]?key|api[_-]?key)\\s*[:=]\\s*\\S+",
                "$1=<REDACTED>");
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('`', '\'').replace('\n', ' ').replace('\r', ' ');
    }

    private static String shortCommit(String commit) {
        if (commit == null || commit.isBlank() || "unknown".equals(commit)) return "unknown";
        return commit.length() <= 12 ? commit : commit.substring(0, 12);
    }

    private record BuildReadout(
            String version,
            String commit,
            String platform,
            int javaRelease,
            boolean releaseHardened,
            String distributionModel,
            Path manifest,
            boolean canonical,
            String error
    ) { }

    private record PackageReadout(
            boolean ready,
            Path manifest,
            int supportLibraries,
            String summary
    ) { }

    record DiagnosticReport(
            String clientHash,
            Path reportFile,
            String body,
            boolean modded,
            String version,
            String commit,
            String platform,
            boolean releaseHardened
    ) { }
}
