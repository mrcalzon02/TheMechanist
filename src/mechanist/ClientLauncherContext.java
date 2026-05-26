package mechanist;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

/**
 * Client-side consumer for the thin-launcher handoff context.
 *
 * The thin launcher prepares wrapper/profile/portrait context before the client
 * starts. This client-owned reader makes that context explicit and reusable by
 * client systems without forcing those systems to know launcher file layout.
 */
public final class ClientLauncherContext {
    private static volatile Snapshot cached;

    public record Snapshot(
            boolean present,
            Path contextFile,
            String wrapperKind,
            String steamAppId,
            String steamGameId,
            String gogGameId,
            String wrapperEvidence,
            String profileId,
            String profileHash,
            String profileFile,
            String portraitPackage,
            String portraitId,
            String celebrityPortraitPackage,
            String celebrityNamePackage
    ) {
        public boolean isSteamWrapped() {
            return "STEAM".equalsIgnoreCase(wrapperKind);
        }

        public boolean isGogWrapped() {
            return "GOG".equalsIgnoreCase(wrapperKind);
        }

        public boolean isWrapped() {
            return isSteamWrapped() || isGogWrapped();
        }
    }

    public static Snapshot current() {
        Snapshot local = cached;
        if (local == null) {
            synchronized (ClientLauncherContext.class) {
                local = cached;
                if (local == null) {
                    local = load();
                    cached = local;
                }
            }
        }
        return local;
    }

    public static Snapshot reload() {
        synchronized (ClientLauncherContext.class) {
            cached = load();
            return cached;
        }
    }

    public static Optional<Path> contextPathFromSystemProperty() {
        String value = System.getProperty("mechanist.launcher.context", "").trim();
        if (value.isEmpty()) return Optional.empty();
        try {
            return Optional.of(Path.of(value).toAbsolutePath().normalize());
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private static Snapshot load() {
        Optional<Path> context = contextPathFromSystemProperty();
        if (context.isEmpty() || !Files.isRegularFile(context.get())) {
            return fromSystemPropertiesOnly(context.orElse(null));
        }
        Properties p = new Properties();
        try (var in = Files.newInputStream(context.get())) {
            p.load(in);
            return new Snapshot(
                    true,
                    context.get(),
                    prop(p, "wrapper.kind", System.getProperty("mechanist.launcher.wrapper", "NONE")),
                    prop(p, "wrapper.steam_app_id", ""),
                    prop(p, "wrapper.steam_game_id", ""),
                    prop(p, "wrapper.gog_game_id", ""),
                    prop(p, "wrapper.evidence", ""),
                    prop(p, "profile.id", System.getProperty("mechanist.launcher.profile", "")),
                    prop(p, "profile.hash", System.getProperty("mechanist.launcher.profileHash", "")),
                    prop(p, "profile.file", ""),
                    prop(p, "profile.portrait.package", ""),
                    prop(p, "profile.portrait.id", ""),
                    prop(p, "profile.celebrity.portrait.package", ""),
                    prop(p, "profile.celebrity.name.package", "")
            );
        } catch (IOException ex) {
            return fromSystemPropertiesOnly(context.get());
        }
    }

    private static Snapshot fromSystemPropertiesOnly(Path path) {
        return new Snapshot(
                false,
                path,
                normalizeWrapper(System.getProperty("mechanist.launcher.wrapper", "NONE")),
                "",
                "",
                "",
                "system-properties-only",
                System.getProperty("mechanist.launcher.profile", ""),
                System.getProperty("mechanist.launcher.profileHash", ""),
                "",
                "",
                "",
                "",
                ""
        );
    }

    private static String normalizeWrapper(String value) {
        if (value == null || value.isBlank()) return "NONE";
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String prop(Properties p, String key, String fallback) {
        String value = p.getProperty(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private ClientLauncherContext() {}
}
