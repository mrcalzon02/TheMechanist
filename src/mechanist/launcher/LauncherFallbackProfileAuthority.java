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
    public static final String HUMAN_8X8_PACKAGE = "launcher-human-8x8-v1";
    public static final String SPECIAL_PORTRAIT_PACKAGE = "launcher-special-portraits-v1";
    public static final String SPECIAL_NAME_PACKAGE = "launcher-special-name-detection-v1";
    public static final String LEGACY_CELEBRITY_PORTRAIT_PACKAGE = "launcher-celebrity-portraits-v1";
    public static final String LEGACY_CELEBRITY_NAME_PACKAGE = "launcher-celebrity-name-detection-v1";
    private static final String HUMAN_8X8_ID_PREFIX = "human8x8-";
    private static final int HUMAN_8X8_PORTRAIT_COUNT = 64;

    public record LauncherProfile(
            String profileId,
            String profileHash,
            String portraitPackage,
            String portraitId,
            String specialPortraitPackage,
            String specialNamePackage,
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
        int portraitOrdinal = Math.floorMod(profileHash.hashCode(), HUMAN_8X8_PORTRAIT_COUNT);
        String defaultPortraitId = humanPortraitId(portraitOrdinal);

        Path file = root.resolve(profileId + ".properties");
        Properties p = new Properties();
        if (Files.isRegularFile(file)) {
            try (var in = Files.newInputStream(file)) {
                p.load(in);
            } catch (Exception ignored) {
                p.clear();
            }
        }
        String portraitId = resolvePortraitId(p, defaultPortraitId);
        p.setProperty("schema", "2");
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
        p.setProperty("portrait.package", HUMAN_8X8_PACKAGE);
        p.setProperty("portrait.id", portraitId);
        p.setProperty("special.portrait.package", SPECIAL_PORTRAIT_PACKAGE);
        p.setProperty("special.name.package", SPECIAL_NAME_PACKAGE);
        p.setProperty("special.publish_status", "quarantined-until-cleared");
        // Compatibility aliases for old launcher context readers. Do not use as active package identity.
        p.setProperty("celebrity.portrait.package.legacy", LEGACY_CELEBRITY_PORTRAIT_PACKAGE);
        p.setProperty("celebrity.name.package.legacy", LEGACY_CELEBRITY_NAME_PACKAGE);
        try (var out = Files.newOutputStream(file)) {
            p.store(out, "The Mechanist launcher fallback profile");
        }
        return new LauncherProfile(
                profileId,
                profileHash,
                HUMAN_8X8_PACKAGE,
                portraitId,
                SPECIAL_PORTRAIT_PACKAGE,
                SPECIAL_NAME_PACKAGE,
                file
        );
    }

    static String resolvePortraitId(Properties profile, String defaultPortraitId) {
        if (profile != null && HUMAN_8X8_PACKAGE.equals(profile.getProperty("portrait.package", "").trim())) {
            String storedPortraitId = profile.getProperty("portrait.id", "").trim();
            if (isValidHumanPortraitId(storedPortraitId)) {
                return storedPortraitId;
            }
        }
        return defaultPortraitId;
    }

    static boolean isValidHumanPortraitId(String portraitId) {
        return humanPortraitOrdinal(portraitId) >= 0;
    }

    static int humanPortraitCount() {
        return HUMAN_8X8_PORTRAIT_COUNT;
    }

    static String humanPortraitId(int ordinal) {
        int wrapped = Math.floorMod(ordinal, HUMAN_8X8_PORTRAIT_COUNT);
        return String.format(Locale.ROOT, HUMAN_8X8_ID_PREFIX + "%02d", wrapped);
    }

    static int humanPortraitOrdinal(String portraitId) {
        if (portraitId == null
                || !portraitId.startsWith(HUMAN_8X8_ID_PREFIX)
                || portraitId.length() != HUMAN_8X8_ID_PREFIX.length() + 2) {
            return -1;
        }
        int offset = HUMAN_8X8_ID_PREFIX.length();
        char tens = portraitId.charAt(offset);
        char ones = portraitId.charAt(offset + 1);
        if (!Character.isDigit(tens) || !Character.isDigit(ones)) {
            return -1;
        }
        int ordinal = (tens - '0') * 10 + (ones - '0');
        return ordinal >= 0 && ordinal < HUMAN_8X8_PORTRAIT_COUNT ? ordinal : -1;
    }

    public static LauncherProfile selectHumanPortrait(
            LauncherProfile profile,
            String portraitId
    ) throws IOException {
        if (profile == null) throw new IllegalArgumentException("profile is required");
        if (!isValidHumanPortraitId(portraitId)) {
            throw new IllegalArgumentException("invalid human portrait id: " + portraitId);
        }
        Path file = profile.profileFile();
        if (file == null || !Files.isRegularFile(file)) {
            throw new IOException("launcher profile file is unavailable");
        }

        Properties p = new Properties();
        try (var in = Files.newInputStream(file)) {
            p.load(in);
        }
        if (!profile.profileId().equals(p.getProperty("profile.id", "").trim())) {
            throw new IOException("launcher profile identity does not match persisted profile");
        }

        p.setProperty("portrait.package", HUMAN_8X8_PACKAGE);
        p.setProperty("portrait.id", portraitId);
        p.setProperty("profile.refreshed_at", Instant.now().toString());
        try (var out = Files.newOutputStream(file)) {
            p.store(out, "The Mechanist launcher fallback profile");
        }

        return new LauncherProfile(
                profile.profileId(),
                profile.profileHash(),
                HUMAN_8X8_PACKAGE,
                portraitId,
                profile.specialPortraitPackage(),
                profile.specialNamePackage(),
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
