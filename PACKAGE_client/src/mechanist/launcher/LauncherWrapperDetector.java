package mechanist.launcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Detects store/wrapper launch context before the game client starts. */
public final class LauncherWrapperDetector {
    public enum WrapperKind {
        STEAM,
        GOG,
        NONE,
        UNKNOWN
    }

    public record WrapperEnvironment(
            WrapperKind kind,
            String steamAppId,
            String steamGameId,
            String gogGameId,
            List<String> evidence
    ) {
        public boolean isWrapped() {
            return kind == WrapperKind.STEAM || kind == WrapperKind.GOG;
        }
    }

    public static WrapperEnvironment detect(Path appHome) {
        Map<String, String> env = System.getenv();
        List<String> evidence = new ArrayList<>();

        String steamAppId = firstNonBlank(
                env.get("SteamAppId"),
                env.get("STEAM_APP_ID"),
                env.get("SteamOverlayGameId"),
                env.get("STEAM_OVERLAY_GAME_ID")
        );
        String steamGameId = firstNonBlank(
                env.get("SteamGameId"),
                env.get("STEAM_GAME_ID"),
                env.get("SteamOverlayGameId"),
                env.get("STEAM_OVERLAY_GAME_ID")
        );
        if (notBlank(steamAppId)) evidence.add("steam-env-app-id");
        if (notBlank(steamGameId)) evidence.add("steam-env-game-id");
        if (truthy(env.get("SteamClientLaunch")) || truthy(env.get("STEAM_CLIENT_LAUNCH"))) {
            evidence.add("steam-client-launch-env");
        }
        if (appHome != null && Files.isRegularFile(appHome.resolve("steam_appid.txt"))) {
            evidence.add("steam-appid-file");
            if (!notBlank(steamAppId)) {
                steamAppId = readFirstLine(appHome.resolve("steam_appid.txt")).orElse(null);
            }
        }
        if (containsIgnoreCase(env.get("PATH"), "steam") || containsIgnoreCase(env.get("LD_LIBRARY_PATH"), "steam")) {
            evidence.add("steam-path-hint");
        }
        if (notBlank(steamAppId) || notBlank(steamGameId) || evidence.stream().anyMatch(s -> s.startsWith("steam"))) {
            return new WrapperEnvironment(WrapperKind.STEAM, clean(steamAppId), clean(steamGameId), "", List.copyOf(evidence));
        }

        String gogGameId = firstNonBlank(
                env.get("GOG_GAME_ID"),
                env.get("GOGGALAXY_GAME_ID"),
                env.get("GOG_GALAXY_GAME_ID")
        );
        if (notBlank(gogGameId)) evidence.add("gog-env-game-id");
        if (truthy(env.get("GOG_GALAXY_LAUNCHED")) || truthy(env.get("GOGGALAXY_LAUNCHED"))) {
            evidence.add("gog-galaxy-env");
        }
        if (containsIgnoreCase(env.get("PATH"), "gog") || containsIgnoreCase(env.get("PROGRAMFILES"), "gog")) {
            evidence.add("gog-path-hint");
        }
        if (appHome != null) {
            try (var stream = Files.list(appHome)) {
                if (stream.anyMatch(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).startsWith("goggame-"))) {
                    evidence.add("goggame-info-file");
                }
            } catch (Exception ignored) {
                // Directory probing is a best-effort wrapper hint only.
            }
        }
        if (notBlank(gogGameId) || evidence.stream().anyMatch(s -> s.startsWith("gog"))) {
            return new WrapperEnvironment(WrapperKind.GOG, "", "", clean(gogGameId), List.copyOf(evidence));
        }

        return new WrapperEnvironment(WrapperKind.NONE, "", "", "", List.of("no-wrapper-evidence"));
    }

    private static Optional<String> readFirstLine(Path path) {
        try {
            return Files.readAllLines(path).stream().findFirst().map(LauncherWrapperDetector::clean).filter(LauncherWrapperDetector::notBlank);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (notBlank(value)) return clean(value);
        }
        return "";
    }

    private static boolean truthy(String value) {
        if (!notBlank(value)) return false;
        String v = value.trim().toLowerCase(Locale.ROOT);
        return v.equals("1") || v.equals("true") || v.equals("yes") || v.equals("on");
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && needle != null && haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private LauncherWrapperDetector() {}
}
