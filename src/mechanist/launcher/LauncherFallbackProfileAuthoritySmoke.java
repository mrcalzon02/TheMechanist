package mechanist.launcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/** Focused source/runtime smoke for launcher fallback-profile portrait persistence. */
public final class LauncherFallbackProfileAuthoritySmoke {
    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("mechanist-launcher-profile-smoke");
        Path appHome = temp.resolve("app");
        Path userRoot = temp.resolve("user");
        Files.createDirectories(appHome);

        var wrapper = new LauncherWrapperDetector.WrapperEnvironment(
                LauncherWrapperDetector.WrapperKind.NONE,
                "",
                "",
                "",
                List.of("smoke-no-wrapper")
        );

        var first = LauncherFallbackProfileAuthority.ensureFallbackProfile(appHome, userRoot, wrapper);
        require(LauncherFallbackProfileAuthority.isValidHumanPortraitId(first.portraitId()), "default portrait must be valid");

        Properties profile = load(first.profileFile());
        profile.setProperty("portrait.package", LauncherFallbackProfileAuthority.HUMAN_8X8_PACKAGE);
        profile.setProperty("portrait.id", "human8x8-17");
        store(first.profileFile(), profile);

        var preserved = LauncherFallbackProfileAuthority.ensureFallbackProfile(appHome, userRoot, wrapper);
        require("human8x8-17".equals(preserved.portraitId()), "valid stored portrait selection must survive refresh");
        require("human8x8-17".equals(load(first.profileFile()).getProperty("portrait.id")), "preserved portrait must remain persisted");

        profile = load(first.profileFile());
        profile.setProperty("portrait.id", "human8x8-99");
        store(first.profileFile(), profile);
        var invalid = LauncherFallbackProfileAuthority.ensureFallbackProfile(appHome, userRoot, wrapper);
        require(!"human8x8-99".equals(invalid.portraitId()), "invalid portrait ordinal must be rejected");
        require(LauncherFallbackProfileAuthority.isValidHumanPortraitId(invalid.portraitId()), "invalid portrait must fall back to valid default");

        profile = load(first.profileFile());
        profile.setProperty("portrait.package", "wrong-portrait-partition");
        profile.setProperty("portrait.id", "human8x8-17");
        store(first.profileFile(), profile);
        var wrongPartition = LauncherFallbackProfileAuthority.ensureFallbackProfile(appHome, userRoot, wrapper);
        require(!"human8x8-17".equals(wrongPartition.portraitId()), "portrait from wrong package must not cross semantic partition");
        require(LauncherFallbackProfileAuthority.isValidHumanPortraitId(wrongPartition.portraitId()), "wrong package must fall back to valid default");

        System.out.println("LauncherFallbackProfileAuthoritySmoke PASS preserved=human8x8-17 invalidRejected=true partitionRejected=true");
    }

    private static Properties load(Path file) throws Exception {
        Properties profile = new Properties();
        try (var in = Files.newInputStream(file)) {
            profile.load(in);
        }
        return profile;
    }

    private static void store(Path file, Properties profile) throws Exception {
        try (var out = Files.newOutputStream(file)) {
            profile.store(out, "LauncherFallbackProfileAuthoritySmoke");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private LauncherFallbackProfileAuthoritySmoke() {}
}
