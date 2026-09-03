package mechanist;

import java.awt.Desktop;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class UserProfileAuthority {
    static final String VERSION = "profile-authority-0.9.10fd";
    private static final Map<String, String> SESSION_FALLBACK_IDENTIFIERS = new HashMap<>();

    static final class Profile {
        final String provider;
        final String displayName;
        final String identifier;
        final String externalUri;
        final boolean wrapperDetected;

        Profile(String provider, String displayName, String identifier, String externalUri, boolean wrapperDetected) {
            this.provider = clean(provider, "Internal");
            this.displayName = clean(displayName, "Local Operator");
            this.identifier = clean(identifier, "MECH-UNKNOWN");
            this.externalUri = clean(externalUri, "");
            this.wrapperDetected = wrapperDetected;
        }

        String compactLabel() {
            return provider.toUpperCase(Locale.ROOT) + " // " + displayName;
        }

        String shortId() {
            return identifier.length() <= 18 ? identifier : identifier.substring(0, 18);
        }

        List<String> lines() {
            ArrayList<String> out = new ArrayList<>();
            out.add("Profile provider: " + provider);
            out.add("Display name: " + displayName);
            out.add("Identifier: " + identifier);
            out.add("Wrapper detected: " + wrapperDetected);
            out.add("External link: " + (externalUri.isEmpty() ? "internal profile only" : externalUri));
            return out;
        }
    }

    static Profile detect() {
        Map<String, String> env = System.getenv();
        boolean steamDetected = containsKeyToken(env, "STEAM");
        if (steamDetected) {
            String steamId = firstEnv(env, "SteamID", "STEAM_ID", "SteamUserID", "STEAM_USER_ID", "SteamAppUser", "SteamUser");
            String name = firstEnv(env, "SteamPersonaName", "STEAM_PERSONA_NAME", "SteamUser", "STEAM_USER", "USER", "USERNAME");
            String id = looksUseful(steamId) ? steamId : internalIdentifier("steam-wrapper");
            String uri = id.matches("[0-9]{8,}") ? "https://steamcommunity.com/profiles/" + id : "steam://open/friends";
            return new Profile("Steam", name.isEmpty() ? "Steam Operator" : name, id, uri, true);
        }
        boolean gogDetected = containsKeyToken(env, "GOG") || containsKeyToken(env, "GALAXY");
        if (gogDetected) {
            String gogId = firstEnv(env, "GOG_USER_ID", "GOGGALAXY_USER_ID", "GALAXY_USER_ID", "GOG_USER", "USERNAME", "USER");
            String name = firstEnv(env, "GOG_USERNAME", "GOGGALAXY_USERNAME", "GALAXY_USERNAME", "GOG_USER", "USERNAME", "USER");
            String id = looksUseful(gogId) ? gogId : internalIdentifier("gog-wrapper");
            return new Profile("GOG", name.isEmpty() ? "GOG Operator" : name, id, "goggalaxy://open", true);
        }
        String id = internalIdentifier("local-profile");
        return new Profile("Internal", "Operator " + id.substring(Math.max(0, id.length() - 8)), id, "", false);
    }

    static String openProfile(Profile profile) {
        Profile p = profile == null ? detect() : profile;
        if (p.externalUri != null && !p.externalUri.isBlank()) {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create(p.externalUri));
                    return "Opened " + p.provider + " profile link.";
                }
            } catch (Throwable t) {
                DebugLog.warn("PROFILE_OPEN", "Could not open external profile URI for " + p.provider + ": " + t.getMessage());
            }
            return p.provider + " wrapper detected; external profile link is not available to this desktop session.";
        }
        return "Internal profile active: " + p.identifier + ".";
    }

    static String auditSummary(Profile profile) {
        Profile p = profile == null ? detect() : profile;
        return "authority=" + VERSION + " provider=" + p.provider + " wrapperDetected=" + p.wrapperDetected + " id=" + p.shortId();
    }

    private static boolean containsKeyToken(Map<String, String> env, String token) {
        if (env == null || token == null) return false;
        String needle = token.toUpperCase(Locale.ROOT);
        for (String k : env.keySet()) if (k != null && k.toUpperCase(Locale.ROOT).contains(needle)) return true;
        return false;
    }

    private static String firstEnv(Map<String, String> env, String... keys) {
        if (env == null || keys == null) return "";
        for (String key : keys) {
            for (Map.Entry<String, String> e : env.entrySet()) {
                if (e.getKey() != null && e.getKey().equalsIgnoreCase(key) && looksUseful(e.getValue())) return e.getValue().trim();
            }
        }
        return "";
    }

    private static boolean looksUseful(String value) {
        return value != null && !value.trim().isEmpty() && !"none".equalsIgnoreCase(value.trim());
    }

    private static synchronized String internalIdentifier(String salt) {
        try {
            Path seedFile = GameStorageManager.get().resolveProfilePath("profile.seed");
            String seed;
            if (Files.exists(seedFile)) {
                seed = Files.readString(seedFile, StandardCharsets.UTF_8).trim();
            } else {
                seed = migrateLegacySeed(seedFile);
            }
            if (seed.isBlank()) {
                seed = UUID.randomUUID().toString();
                Files.writeString(seedFile, seed + "\n", StandardCharsets.UTF_8);
            }

            Path identifierFile = GameStorageManager.get().resolveProfilePath(
                    "profile-" + salt + ".identifier");
            if (Files.isRegularFile(identifierFile)) {
                String persisted = Files.readString(identifierFile, StandardCharsets.UTF_8).trim();
                if (persisted.matches("MECH-[0-9A-F]{16}")) {
                    return persisted;
                }
            }

            String material = seed + "|" + salt + "|" + System.getProperty("user.name", "user") + "|" + System.getProperty("os.name", "os");
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(material.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("MECH-");
            for (int i = 0; i < 8 && i < digest.length; i++) sb.append(String.format(Locale.ROOT, "%02X", digest[i]));
            String identifier = sb.toString();
            Files.writeString(identifierFile, identifier + "\n", StandardCharsets.UTF_8);
            return identifier;
        } catch (Throwable t) {
            String fallbackSalt = clean(salt, "local-profile");
            String existing = SESSION_FALLBACK_IDENTIFIERS.get(fallbackSalt);
            if (existing != null) return existing;

            String generated = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
            String fallback = "MECH-" + generated.substring(0, 16);
            SESSION_FALLBACK_IDENTIFIERS.put(fallbackSalt, fallback);
            DebugLog.warn("PROFILE_IDENTITY", "Canonical profile identity storage is unavailable; using a session-stable fallback for " + fallbackSalt + ": " + t.getMessage());
            return fallback;
        }
    }

    private static String migrateLegacySeed(Path canonicalSeedFile) throws Exception {
        Path legacySeedFile = Paths.get("settings").resolve("profile.seed").toAbsolutePath().normalize();
        if (Files.isRegularFile(legacySeedFile)) {
            String legacySeed = Files.readString(legacySeedFile, StandardCharsets.UTF_8).trim();
            if (!legacySeed.isBlank()) {
                Files.writeString(canonicalSeedFile, legacySeed + "\n", StandardCharsets.UTF_8);
                return legacySeed;
            }
        }
        String seed = UUID.randomUUID().toString();
        Files.writeString(canonicalSeedFile, seed + "\n", StandardCharsets.UTF_8);
        return seed;
    }

    private static String clean(String value, String fallback) {
        if (value == null) return fallback;
        String t = value.trim();
        return t.isEmpty() ? fallback : t;
    }

    private UserProfileAuthority() {}
}
