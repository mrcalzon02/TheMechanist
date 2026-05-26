package mechanist.launcher;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Thin launcher entrypoint.
 *
 * This is the process boundary that prepares wrapper/profile launch context before
 * the graphical client starts. It is intentionally still in the same source tree
 * during the migration, but packaging should point at this class rather than the
 * client main once the thin-launcher path is active.
 */
public final class ThinLauncherMain {
    public static void main(String[] args) throws Exception {
        Path appHome = detectAppHome();
        Path userRoot = LauncherFallbackProfileAuthority.defaultUserRoot();
        Files.createDirectories(userRoot);

        LauncherWrapperDetector.WrapperEnvironment wrapper = LauncherWrapperDetector.detect(appHome);
        LauncherFallbackProfileAuthority.LauncherProfile profile = LauncherFallbackProfileAuthority.ensureFallbackProfile(appHome, userRoot, wrapper);
        Path contextFile = writeLaunchContext(appHome, userRoot, wrapper, profile);
        LauncherServerJoinIdentityBridge.JoinIdentityRecord joinIdentity =
                LauncherServerJoinIdentityBridge.write(userRoot, contextFile, wrapper, profile);

        System.setProperty("mechanist.launcher.context", contextFile.toAbsolutePath().normalize().toString());
        System.setProperty("mechanist.launcher.wrapper", wrapper.kind().name());
        System.setProperty("mechanist.launcher.profile", profile.profileId());
        System.setProperty("mechanist.launcher.profileHash", profile.profileHash());
        System.setProperty("mechanist.launcher.joinIdentity", joinIdentity.primaryFile().toAbsolutePath().normalize().toString());

        System.out.println("The Mechanist thin launcher context prepared");
        System.out.println("app.home=" + appHome.toAbsolutePath().normalize());
        System.out.println("user.root=" + userRoot.toAbsolutePath().normalize());
        System.out.println("wrapper.kind=" + wrapper.kind());
        System.out.println("profile.id=" + profile.profileId());
        System.out.println("launch.context=" + contextFile.toAbsolutePath().normalize());
        System.out.println("join.identity=" + joinIdentity.primaryFile().toAbsolutePath().normalize());

        launchClientInProcess(args);
    }

    private static Path writeLaunchContext(
            Path appHome,
            Path userRoot,
            LauncherWrapperDetector.WrapperEnvironment wrapper,
            LauncherFallbackProfileAuthority.LauncherProfile profile
    ) throws IOException {
        Path contextDir = userRoot.resolve("launcher").resolve("context").normalize();
        Files.createDirectories(contextDir);
        Path context = contextDir.resolve("last-launch-context.properties");
        Properties p = new Properties();
        p.setProperty("schema", "2");
        p.setProperty("created_at", Instant.now().toString());
        p.setProperty("app.home", appHome.toAbsolutePath().normalize().toString());
        p.setProperty("user.root", userRoot.toAbsolutePath().normalize().toString());
        p.setProperty("wrapper.kind", wrapper.kind().name());
        p.setProperty("wrapper.steam_app_id", wrapper.steamAppId());
        p.setProperty("wrapper.steam_game_id", wrapper.steamGameId());
        p.setProperty("wrapper.gog_game_id", wrapper.gogGameId());
        p.setProperty("wrapper.evidence", String.join(",", wrapper.evidence()));
        p.setProperty("profile.id", profile.profileId());
        p.setProperty("profile.hash", profile.profileHash());
        p.setProperty("profile.file", profile.profileFile().toAbsolutePath().normalize().toString());
        p.setProperty("profile.portrait.package", profile.portraitPackage());
        p.setProperty("profile.portrait.id", profile.portraitId());
        p.setProperty("profile.special.portrait.package", profile.specialPortraitPackage());
        p.setProperty("profile.special.name.package", profile.specialNamePackage());
        p.setProperty("profile.special.publish_status", "quarantined-until-cleared");
        // Compatibility aliases only. Do not treat these as active package identities.
        p.setProperty("profile.celebrity.portrait.package.legacy", LauncherFallbackProfileAuthority.LEGACY_CELEBRITY_PORTRAIT_PACKAGE);
        p.setProperty("profile.celebrity.name.package.legacy", LauncherFallbackProfileAuthority.LEGACY_CELEBRITY_NAME_PACKAGE);
        try (var out = Files.newOutputStream(context)) {
            p.store(out, "The Mechanist launcher context handoff");
        }
        return context;
    }

    private static void launchClientInProcess(String[] args) throws Exception {
        Class<?> client = Class.forName("mechanist.TheMechanist");
        Method main = client.getMethod("main", String[].class);
        List<String> forwarded = new ArrayList<>();
        if (args != null) {
            for (String arg : args) forwarded.add(arg);
        }
        main.invoke(null, (Object) forwarded.toArray(String[]::new));
    }

    private static Path detectAppHome() {
        try {
            File codeSource = new File(ThinLauncherMain.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path path = codeSource.toPath().toAbsolutePath().normalize();
            if (Files.isRegularFile(path)) {
                Path parent = path.getParent();
                return parent == null ? Path.of(".").toAbsolutePath().normalize() : parent;
            }
            return path;
        } catch (URISyntaxException | RuntimeException ex) {
            return Path.of(".").toAbsolutePath().normalize();
        }
    }

    private ThinLauncherMain() {}
}
