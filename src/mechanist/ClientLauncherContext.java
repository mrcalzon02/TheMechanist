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
    private static volatile JoinIdentity joinIdentityCached;

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
        public boolean isSteamWrapped() { return "STEAM".equalsIgnoreCase(wrapperKind); }
        public boolean isGogWrapped() { return "GOG".equalsIgnoreCase(wrapperKind); }
        public boolean isWrapped() { return isSteamWrapped() || isGogWrapped(); }
    }

    public record JoinIdentity(
            boolean present,
            Path identityFile,
            String profileId,
            String profileHash,
            String auxiliaryProfileHash,
            String wrapperKind,
            String steamAppId,
            String gogGameId,
            String launcherContextFile
    ) {}

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
            joinIdentityCached = null;
            return cached;
        }
    }

    public static JoinIdentity joinIdentity() {
        JoinIdentity local = joinIdentityCached;
        if (local == null) {
            synchronized (ClientLauncherContext.class) {
                local = joinIdentityCached;
                if (local == null) {
                    local = loadJoinIdentity(current());
                    joinIdentityCached = local;
                }
            }
        }
        return local;
    }

    public static Optional<Path> contextPathFromSystemProperty() {
        String value = System.getProperty("mechanist.launcher.context", "").trim();
        if (value.isEmpty()) return Optional.empty();
        try { return Optional.of(Path.of(value).toAbsolutePath().normalize()); }
        catch (RuntimeException ex) { return Optional.empty(); }
    }

    public static Optional<Path> joinIdentityPathFromSystemProperty() {
        String value = System.getProperty("mechanist.launcher.joinIdentity", "").trim();
        if (value.isEmpty()) return Optional.empty();
        try { return Optional.of(Path.of(value).toAbsolutePath().normalize()); }
        catch (RuntimeException ex) { return Optional.empty(); }
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

    private static JoinIdentity loadJoinIdentity(Snapshot snapshot) {
        Optional<Path> identityPath = joinIdentityPathFromSystemProperty();
        if (identityPath.isEmpty() || !Files.isRegularFile(identityPath.get())) {
            return new JoinIdentity(
                    false,
                    identityPath.orElse(null),
                    snapshot.profileId(),
                    snapshot.profileHash(),
                    snapshot.profileHash(),
                    snapshot.wrapperKind(),
                    snapshot.steamAppId(),
                    snapshot.gogGameId(),
                    snapshot.contextFile() == null ? "" : snapshot.contextFile().toAbsolutePath().normalize().toString()
            );
        }
        Properties p = new Properties();
        try (var in = Files.newInputStream(identityPath.get())) {
            p.load(in);
            return new JoinIdentity(
                    true,
                    identityPath.get(),
                    prop(p, "server_join.profile_id", prop(p, "profile.id", snapshot.profileId())),
                    prop(p, "server_join.profile_hash", prop(p, "profile.hash", snapshot.profileHash())),
                    prop(p, "security.auxiliary_profile_hash", snapshot.profileHash()),
                    prop(p, "server_join.wrapper_kind", snapshot.wrapperKind()),
                    prop(p, "launcher.wrapper.steam_app_id", snapshot.steamAppId()),
                    prop(p, "launcher.wrapper.gog_game_id", snapshot.gogGameId()),
                    prop(p, "launcher.context.file", snapshot.contextFile() == null ? "" : snapshot.contextFile().toAbsolutePath().normalize().toString())
            );
        } catch (IOException ex) {
            return new JoinIdentity(
                    false,
                    identityPath.get(),
                    snapshot.profileId(),
                    snapshot.profileHash(),
                    snapshot.profileHash(),
                    snapshot.wrapperKind(),
                    snapshot.steamAppId(),
                    snapshot.gogGameId(),
                    snapshot.contextFile() == null ? "" : snapshot.contextFile().toAbsolutePath().normalize().toString()
            );
        }
    }

    private static Snapshot fromSystemPropertiesOnly(Path path) {
        return new Snapshot(
                false,
                path,
                normalizeWrapper(System.getProperty("mechanist.launcher.wrapper", "NONE")),
                "", "", "",
                "system-properties-only",
                System.getProperty("mechanist.launcher.profile", ""),
                System.getProperty("mechanist.launcher.profileHash", ""),
                "", "", "", "", ""
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
