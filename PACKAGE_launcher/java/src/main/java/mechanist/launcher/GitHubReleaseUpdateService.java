package mechanist.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches published, fully assembled automatic releases (including prereleases)
 * without cloning source code or replacing files in a running game. Downloads
 * must match the GitHub asset's SHA-256 digest before an OS installer is opened.
 */
final class GitHubReleaseUpdateService {
    static final URI RELEASE_API = URI.create(
            "https://api.github.com/repos/mrcalzon02/TheMechanist/releases?per_page=30");
    private static final Pattern TAG = Pattern.compile("auto-(\\d+)-([0-9a-f]{12})");
    private static final long MAX_ASSET_SIZE = 2L * 1024 * 1024 * 1024;
    private static final int MAX_API_CHARS = 2_000_000;
    private final HttpClient http;

    GitHubReleaseUpdateService() {
        http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    Candidate check(String platform, String installedCommit) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(RELEASE_API)
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "TheMechanist-LimitedAlpha-Updater")
                .GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("GitHub release lookup returned HTTP " + response.statusCode());
        }
        if (response.body().length() > MAX_API_CHARS) {
            throw new IOException("GitHub release response exceeds the supported size");
        }
        return selectLatest(response.body(), platform, installedCommit);
    }

    /**
     * GitHub's /releases/latest excludes prereleases; intentionally use the
     * releases collection so limited-alpha builds are discoverable.
     */
    static Candidate selectLatest(String json, String platform, String installedCommit) throws IOException {
        if (json == null || !json.stripLeading().startsWith("[")) {
            throw new IOException("GitHub returned an invalid releases response");
        }
        Candidate newest = null;
        long bestRun = -1;
        for (String release : objects(json)) {
            if (boolField(release, "draft") || !boolField(release, "prerelease")) continue;
            String tag = stringField(release, "tag_name");
            Matcher match = TAG.matcher(tag);
            if (!match.matches()) continue;
            long run;
            try {
                run = Long.parseLong(match.group(1));
            } catch (NumberFormatException exception) {
                continue;
            }
            if (run <= bestRun) continue;
            String expectedSuffix = platform.startsWith("windows-") ? ".msi"
                    : platform.startsWith("linux-") ? ".deb" : "";
            if (expectedSuffix.isEmpty()) return null;
            String assetJson = arrayField(release, "assets");
            for (String asset : objects(assetJson)) {
                String name = stringField(asset, "name");
                if (!name.toLowerCase(Locale.ROOT).endsWith(expectedSuffix)) continue;
                String digest = stringField(asset, "digest").toLowerCase(Locale.ROOT);
                if (!digest.matches("sha256:[a-f0-9]{64}")) continue;
                long size = longField(asset, "size");
                if (size < 1024 || size >= MAX_ASSET_SIZE) continue;
                URI url = validatedAssetUrl(stringField(asset, "browser_download_url"), tag, name);
                URI page = URI.create("https://github.com/mrcalzon02/TheMechanist/releases/tag/" + tag);
                newest = new Candidate(tag, "0.9." + run, match.group(2), name, url,
                        page, size, digest.substring(7));
                bestRun = run;
                break;
            }
        }
        if (newest == null) return null;
        if (installedCommit != null && !installedCommit.isBlank()
                && installedCommit.toLowerCase(Locale.ROOT).startsWith(newest.commitPrefix())) {
            return null;
        }
        return newest;
    }

    Path download(Candidate candidate, Path cacheDir) throws IOException, InterruptedException {
        Files.createDirectories(cacheDir);
        Path target = cacheDir.resolve(candidate.assetName()).normalize();
        if (!target.getParent().equals(cacheDir.toAbsolutePath().normalize())) {
            throw new IOException("Unsafe installer filename");
        }
        if (Files.isRegularFile(target) && verified(target, candidate)) return target;
        Path pending = Files.createTempFile(cacheDir, "mechanist-update-", ".pending");
        try {
            HttpRequest request = HttpRequest.newBuilder(candidate.downloadUrl())
                    .timeout(Duration.ofMinutes(45))
                    .header("Accept", "application/octet-stream")
                    .header("User-Agent", "TheMechanist-LimitedAlpha-Updater")
                    .GET().build();
            HttpResponse<Path> response = http.send(request, HttpResponse.BodyHandlers.ofFile(pending));
            if (response.statusCode() != 200 || !verified(pending, candidate)) {
                throw new IOException("Downloaded installer failed HTTP/size/SHA-256 verification");
            }
            try {
                Files.move(pending, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(pending, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target;
        } finally {
            Files.deleteIfExists(pending);
        }
    }

    void launchInstaller(Path verifiedInstaller, Candidate candidate) throws IOException {
        if (!verified(verifiedInstaller, candidate)) {
            throw new IOException("Refusing to open installer with an invalid checksum");
        }
        String platform = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (platform.contains("windows") && candidate.assetName().toLowerCase(Locale.ROOT).endsWith(".msi")) {
            new ProcessBuilder("msiexec.exe", "/i", verifiedInstaller.toAbsolutePath().toString())
                    .start();
        } else if (platform.contains("linux") && candidate.assetName().toLowerCase(Locale.ROOT).endsWith(".deb")) {
            // Let the desktop's package manager request authorization and perform installation.
            new ProcessBuilder("xdg-open", verifiedInstaller.toAbsolutePath().toString())
                    .start();
        } else {
            throw new IOException("This installer is incompatible with the current operating system");
        }
    }

    private static URI validatedAssetUrl(String value, String tag, String name) throws IOException {
        try {
            URI uri = URI.create(value);
            String exactPath = "/mrcalzon02/TheMechanist/releases/download/" + tag + "/" + name;
            if (!"https".equals(uri.getScheme()) || !"github.com".equals(uri.getHost())
                    || !exactPath.equals(uri.getRawPath()) || uri.getUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                throw new IOException("Invalid GitHub installer download URL");
            }
            if (name.contains("/") || name.contains("\\") || name.equals(".") || name.equals("..")) {
                throw new IOException("Invalid installer asset name");
            }
            return uri;
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid GitHub installer URI", ex);
        }
    }

    private static boolean verified(Path path, Candidate candidate) throws IOException {
        if (!Files.isRegularFile(path) || Files.size(path) != candidate.size()) return false;
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] chunk = new byte[1024 * 1024];
                for (int count; (count = input.read(chunk)) != -1;) {
                    hash.update(chunk, 0, count);
                }
            }
            return candidate.sha256().equals(HexFormat.of().formatHex(hash.digest()));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 unavailable", e);
        }
    }

    /** Split only top-level JSON objects; nested asset objects remain in their parent. */
    private static List<String> objects(String json) {
        List<String> found = new ArrayList<>();
        int depth = 0;
        int begin = -1;
        boolean inString = false;
        boolean escape = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (escape) escape = false;
                else if (c == '\\') escape = true;
                else if (c == '"') inString = false;
                continue;
            }
            if (c == '"') { inString = true; continue; }
            if (c == '{') { if (depth++ == 0) begin = i; }
            else if (c == '}' && depth > 0 && --depth == 0 && begin >= 0) {
                found.add(json.substring(begin, i + 1));
                begin = -1;
            }
        }
        return found;
    }

    private static String arrayField(String json, String name) {
        Matcher match = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*\\[")
                .matcher(json);
        if (!match.find()) return "[]";
        int start = match.end() - 1;
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (escape) escape = false;
                else if (c == '\\') escape = true;
                else if (c == '"') inString = false;
                continue;
            }
            if (c == '"') { inString = true; continue; }
            if (c == '[') depth++;
            else if (c == ']' && --depth == 0) return json.substring(start, i + 1);
        }
        return "[]";
    }

    private static String stringField(String json, String name) {
        Matcher match = Pattern.compile("\"" + Pattern.quote(name)
                + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(json);
        return match.find() ? match.group(1).replace("\\/", "/") : "";
    }

    private static boolean boolField(String json, String name) {
        return Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*true")
                .matcher(json).find();
    }

    private static long longField(String json, String name) {
        Matcher match = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*(\\d+)")
                .matcher(json);
        if (!match.find()) return -1;
        try { return Long.parseLong(match.group(1)); }
        catch (NumberFormatException ignored) { return -1; }
    }

    record Candidate(String tag, String version, String commitPrefix, String assetName,
                     URI downloadUrl, URI releasePage, long size, String sha256) { }
}
