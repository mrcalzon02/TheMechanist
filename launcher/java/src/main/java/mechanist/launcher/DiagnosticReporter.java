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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

final class DiagnosticReporter {
    private static final String ISSUE_URL = "https://github.com/mrcalzon02/TheMechanist/issues/new";
    private final LauncherConfig config;

    DiagnosticReporter(LauncherConfig config) {
        this.config = config;
    }

    DiagnosticReport prepare(PackageTier graphicsTier, PackageTier audioTier, String channel, String recentLauncherLog) throws IOException {
        Files.createDirectories(config.logsDir);
        Files.createDirectories(config.cacheDir);
        Files.createDirectories(config.settingsDir);
        String clientHash = clientHash();
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        Path reportDir = config.cacheDir.resolve("diagnostics").resolve(timestamp.replace(':', '-'));
        Files.createDirectories(reportDir);
        Path reportFile = reportDir.resolve("diagnostic-report.md");

        String body = buildReport(clientHash, timestamp, graphicsTier, audioTier, channel, recentLauncherLog);
        Files.writeString(reportFile, body, StandardCharsets.UTF_8);
        return new DiagnosticReport(clientHash, reportFile, body);
    }

    void openIssueDraft(DiagnosticReport report) throws IOException {
        String title = "Launcher diagnostic report " + report.clientHash;
        String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
        String encodedBody = URLEncoder.encode(report.body, StandardCharsets.UTF_8);
        URI uri = URI.create(ISSUE_URL + "?title=" + encodedTitle + "&body=" + encodedBody);
        if (!Desktop.isDesktopSupported()) throw new IOException("Desktop integration is unavailable; report written to " + report.reportFile);
        Desktop.getDesktop().browse(uri);
    }

    private String buildReport(String clientHash, String timestamp, PackageTier graphicsTier, PackageTier audioTier, String channel, String recentLauncherLog) {
        StringBuilder sb = new StringBuilder(16_384);
        sb.append("## The Mechanist launcher diagnostic report\n\n");
        sb.append("Generated: `").append(timestamp).append("`\n\n");
        sb.append("### Client identity\n\n");
        sb.append("- Client hash: `").append(clientHash).append("`\n");
        sb.append("- Raw client UUID is stored locally and is not included in this report.\n\n");

        sb.append("### Runtime\n\n");
        sb.append("- OS: `").append(safe(System.getProperty("os.name"))).append(" ").append(safe(System.getProperty("os.version"))).append("`\n");
        sb.append("- Architecture: `").append(safe(System.getProperty("os.arch"))).append("`\n");
        sb.append("- Java: `").append(safe(System.getProperty("java.version"))).append("`\n");
        sb.append("- Java vendor: `").append(safe(System.getProperty("java.vendor"))).append("`\n\n");

        sb.append("### Launcher selections\n\n");
        sb.append("- Channel: `").append(safe(channel)).append("`\n");
        sb.append("- Graphics tier: `").append(graphicsTier == null ? "<none>" : graphicsTier.id).append("`\n");
        sb.append("- Audio tier: `").append(audioTier == null ? "<none>" : audioTier.id).append("`\n\n");

        sb.append("### Paths\n\n");
        sb.append("- Install root: `").append(redactHome(config.installRoot)).append("`\n");
        sb.append("- Game payload: `").append(redactHome(config.repoDir)).append("`\n");
        sb.append("- Saves: `").append(redactHome(config.saveDir)).append("`\n");
        sb.append("- Settings: `").append(redactHome(config.settingsDir)).append("`\n");
        sb.append("- Logs: `").append(redactHome(config.logsDir)).append("`\n");
        sb.append("- Cache: `").append(redactHome(config.cacheDir)).append("`\n\n");

        sb.append("### File checks\n\n");
        appendExists(sb, "Game repo", config.repoDir);
        appendExists(sb, "Windows BAT launcher", config.repoDir.resolve("RUN_THE_MECHANIST_WINDOWS.bat"));
        appendExists(sb, "Windows PS1 launcher", config.repoDir.resolve("RUN_THE_MECHANIST_WINDOWS.ps1"));
        appendExists(sb, "Linux launcher", config.repoDir.resolve("PLAY_THE_MECHANIST_LINUX.sh"));
        appendExists(sb, "Music manifest", config.repoDir.resolve("assets/music/music_manifest.tsv"));
        appendExists(sb, "Music WAV root", config.repoDir.resolve("assets/music/wav"));
        appendExists(sb, "Package graphics manifest", config.repoDir.resolve("config/packages/graphics_tiers.tsv"));
        appendExists(sb, "Package audio manifest", config.repoDir.resolve("config/packages/audio_tiers.tsv"));
        sb.append("\n");

        sb.append("### Recent launcher log excerpt\n\n```text\n");
        sb.append(redactSecrets(tail(recentLauncherLog, 12000)));
        sb.append("\n```\n\n");

        sb.append("### Recent local log files\n\n");
        for (Path p : recentFiles(config.logsDir, 6)) {
            sb.append("#### `").append(redactHome(p)).append("`\n\n```text\n");
            sb.append(redactSecrets(tailFile(p, 8000)));
            sb.append("\n```\n\n");
        }
        return sb.toString();
    }

    private String clientHash() throws IOException {
        Path idFile = config.settingsDir.resolve("launcher-client.id");
        String id;
        if (Files.isRegularFile(idFile)) id = Files.readString(idFile, StandardCharsets.UTF_8).trim();
        else {
            id = UUID.randomUUID().toString();
            Files.writeString(idFile, id, StandardCharsets.UTF_8);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(id.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes).substring(0, 16);
        } catch (Exception ex) {
            throw new IOException("Could not generate client hash", ex);
        }
    }

    private static void appendExists(StringBuilder sb, String label, Path path) {
        sb.append("- ").append(label).append(": `").append(Files.exists(path) ? "present" : "missing").append("` — `").append(redactHome(path)).append("`\n");
    }

    private static List<Path> recentFiles(Path root, int limit) {
        if (root == null || !Files.isDirectory(root)) return List.of();
        try (Stream<Path> s = Files.walk(root, 2)) {
            return s.filter(Files::isRegularFile)
                    .sorted(Comparator.comparingLong((Path p) -> modified(p)).reversed())
                    .limit(limit)
                    .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static long modified(Path p) {
        try { return Files.getLastModifiedTime(p).toMillis(); }
        catch (Exception ignored) { return 0L; }
    }

    private static String tailFile(Path p, int maxChars) {
        try { return tail(Files.readString(p, StandardCharsets.UTF_8), maxChars); }
        catch (Exception ex) { return "<could not read log: " + ex.getMessage() + ">"; }
    }

    private static String tail(String text, int maxChars) {
        if (text == null) return "";
        if (text.length() <= maxChars) return text;
        return "<truncated to last " + maxChars + " characters>\n" + text.substring(text.length() - maxChars);
    }

    private static String redactHome(Path p) {
        if (p == null) return "<null>";
        String s = p.toAbsolutePath().normalize().toString();
        String home = System.getProperty("user.home");
        if (home != null && !home.isBlank()) s = s.replace(home, "<USER_HOME>");
        return s;
    }

    private static String redactSecrets(String s) {
        if (s == null) return "";
        return s.replaceAll("(?i)(token|password|secret|authorization|cookie)\\s*[:=]\\s*\\S+", "$1=<REDACTED>");
    }

    private static String safe(String s) { return s == null ? "" : s.replace('`', '\''); }

    record DiagnosticReport(String clientHash, Path reportFile, String body) {}
}
