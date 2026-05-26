package mechanist.launcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Properties;

/**
 * Launcher-owned fallback profile generator.
 *
 * The client may consume the generated profile, but profile creation and wrapper
 * attribution happen before the client starts so the client is not responsible
 * for guessing store/wrapper context after launch.
 */
public final class LauncherFallbackProfileAuthority {
    public record LauncherProfile(
            String profileId,
            String profileHash,
            String portraitPackage,
            String portraitId,
            String celebrityPackage,
            String celebrityNamePackage,
            Path profileFile
    ) {}

    public static LauncherProfile ensureFallbackProfile(
            Path appHome,
            Path userRoot,
            LauncherWrapperDetector.WrapperEnvironment wrapper
    ) throws IOException {
        Path root = userRoot.resolve("launcher").resolve("profiles").normalize();
        Files.createDirectories(root);

        String machineSeed = System.getProperty("user.name", "unknown")
                + "|" + System.getProperty("os.name", "unknown")
                + "|" + System.getProperty("os.arch", "unknown")
                + "|" + cleanPath(appHome)
                + "|" + wrapper.kind()
                + "|" + wrapper.steamAppId()
                + "|" + wrapper.gogGameId();
        String profileHash = sha256(machineSeed);
        String profileId = "fallback-" + profileHash.substring(0, 16);
        int portraitOrdinal = Math.floorMod(profileHash.hashCode(), 64);
        String portraitId = String.format(Locale.ROOT, "human8x8-%02d", portraitOrdinal);

        Path file = root.resolve(profileId + ".properties");
        Properties p = new Properties();
        if (Files.isRegularFile(file)) {
            try (var in = Files.newInputStream(file)) {
                p.load(in);
            } catch (Exception ignored) {
                p.clear();
            }
        }
        p.setProperty("schema", "1");
        p.setProperty("owner", "thin-launcher");
        p.setProperty("profile.id", profileId);
        p.setProperty("profile.hash", profileHash);
        p.setProperty("profile.generated_at", p.getProperty("profile.generated_at", Instant.now().toString()));
        p.setProperty("profile.refreshed_at", Instant.now().toString());
        p.setProperty("wrapper.kind", wrapper.kind().name());
        p.setProperty("wrapper.steam_app_id", wrapper.steamAppId());
        p.setProperty("wrapper.steam_game_id", wrapper.steamGameId());
        p.setProperty("wrapper.gog_game_id", wrapper.gogGameId());
        p.setProperty("wrapper.evidence", String.join(",", wrapper.evidence()));
        p.setProperty("portrait.package", "launcher-human-8x8-v1");
        p.setProperty("portrait.id", portraitId);
        p.setProperty("celebrity.portrait.package", "launcher-celebrity-portraits-v1");
        p.setProperty("celebrity.name.package", "launcher-celebrity-name-detection-v1");
        try (var out = Files.newOutputStream(file)) {
            p.store(out, "The Mechanist launcher fallback profile");
        }
        return new LauncherProfile(
                profileId,
                profileHash,
                "launcher-human-8x8-v1",
                portraitId,
                "launcher-celebrity-portraits-v1",
                "launcher-celebrity-name-detection-v1",
                file
        );
    }

    public static Path defaultUserRoot() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            String local = System.getenv("LOCALAPPDATA");
            if (notBlank(local)) return Path.of(local, "TheMechanist");
            String user = System.getProperty("user.home", ".");
            return Path.of(user, "AppData", "Local", "TheMechanist");
        }
        String xdg = System.getenv("XDG_DATA_HOME");
        if (notBlank(xdg)) return Path.of(xdg, "TheMechanist");
        return Path.of(System.getProperty("user.home", "."), ".local", "share", "TheMechanist");
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private static String cleanPath(Path path) {
        return path == null ? "" : path.toAbsolutePath().normalize().toString();
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private LauncherFallbackProfileAuthority() {}
}
