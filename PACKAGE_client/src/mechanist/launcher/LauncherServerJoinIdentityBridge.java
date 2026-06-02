package mechanist.launcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;

/**
 * Writes the launcher-owned fallback profile hash into the internal profile area
 * used by server/join/security mechanics.
 *
 * This bridge does not replace the eventual server-authoritative identity
 * service. It creates a stable, inspectable auxiliary identity record so the
 * server join path can consume launcher wrapper/profile context without forcing
 * the running client to invent that identity after launch.
 */
public final class LauncherServerJoinIdentityBridge {
    public record JoinIdentityRecord(
            Path primaryFile,
            Path profileMirrorFile,
            String profileId,
            String profileHash
    ) {}

    public static JoinIdentityRecord write(
            Path userRoot,
            Path contextFile,
            LauncherWrapperDetector.WrapperEnvironment wrapper,
            LauncherFallbackProfileAuthority.LauncherProfile profile
    ) throws IOException {
        Path profileRoot = userRoot.resolve("saves").resolve("data").resolve("profiles").normalize();
        Files.createDirectories(profileRoot);

        Path primary = profileRoot.resolve("launcher-join-identity.properties");
        Path mirror = profileRoot.resolve(profile.profileId() + "-join-identity.properties");

        Properties p = new Properties();
        p.setProperty("schema", "1");
        p.setProperty("owner", "thin-launcher");
        p.setProperty("purpose", "server-join-security-auxiliary-identity");
        p.setProperty("updated_at", Instant.now().toString());
        p.setProperty("launcher.context.file", normalize(contextFile));
        p.setProperty("launcher.profile.id", profile.profileId());
        p.setProperty("launcher.profile.hash", profile.profileHash());
        p.setProperty("launcher.profile.file", normalize(profile.profileFile()));
        p.setProperty("launcher.profile.portrait.package", profile.portraitPackage());
        p.setProperty("launcher.profile.portrait.id", profile.portraitId());
        p.setProperty("launcher.profile.special.portrait.package", profile.specialPortraitPackage());
        p.setProperty("launcher.profile.special.name.package", profile.specialNamePackage());
        p.setProperty("launcher.profile.special.publish_status", "quarantined-until-cleared");
        // Compatibility aliases only. Do not treat these as active package identities.
        p.setProperty("launcher.profile.celebrity.portrait.package.legacy", LauncherFallbackProfileAuthority.LEGACY_CELEBRITY_PORTRAIT_PACKAGE);
        p.setProperty("launcher.profile.celebrity.name.package.legacy", LauncherFallbackProfileAuthority.LEGACY_CELEBRITY_NAME_PACKAGE);
        p.setProperty("launcher.wrapper.kind", wrapper.kind().name());
        p.setProperty("launcher.wrapper.steam_app_id", wrapper.steamAppId());
        p.setProperty("launcher.wrapper.steam_game_id", wrapper.steamGameId());
        p.setProperty("launcher.wrapper.gog_game_id", wrapper.gogGameId());
        p.setProperty("launcher.wrapper.evidence", String.join(",", wrapper.evidence()));

        // Redundant aliases for older or future join/security consumers that
        // expect direct profile/security key names rather than launcher-prefixed names.
        p.setProperty("profile.id", profile.profileId());
        p.setProperty("profile.hash", profile.profileHash());
        p.setProperty("security.auxiliary_profile_hash", profile.profileHash());
        p.setProperty("server_join.profile_hash", profile.profileHash());
        p.setProperty("server_join.profile_id", profile.profileId());
        p.setProperty("server_join.wrapper_kind", wrapper.kind().name());

        try (var out = Files.newOutputStream(primary)) {
            p.store(out, "The Mechanist launcher server-join identity bridge");
        }
        try (var out = Files.newOutputStream(mirror)) {
            p.store(out, "The Mechanist launcher server-join identity bridge mirror");
        }
        return new JoinIdentityRecord(primary, mirror, profile.profileId(), profile.profileHash());
    }

    private static String normalize(Path path) {
        return path == null ? "" : path.toAbsolutePath().normalize().toString();
    }

    private LauncherServerJoinIdentityBridge() {}
}
