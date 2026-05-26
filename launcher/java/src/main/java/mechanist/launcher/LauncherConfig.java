package mechanist.launcher;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

final class LauncherConfig {
    static final String APP_NAME = "The Mechanist";
    static final String STUDIO_NAME = "StellarCore";
    static final String DEFAULT_BRANCH = "main";

    final Path installRoot;
    final Path launcherDir;
    final Path gameRoot;
    final Path packageRoot;
    final Path clientPackageDir;
    final Path serverPackageDir;
    final Path supportLibraryDir;
    final Path runtimeDir;
    final Path manifestDir;
    final Path uninstallDir;
    final Path saveDir;
    final Path settingsDir;
    final Path profilesDir;
    final Path logsDir;
    final Path cacheDir;
    final Path packageSeedRoot;

    private LauncherConfig(Path installRoot, Path userDataRoot, Path roamingConfigRoot, Path localStateRoot) {
        this.installRoot = installRoot;
        this.launcherDir = installRoot.resolve("Launcher");
        this.gameRoot = installRoot.resolve("Game");
        this.packageRoot = installRoot.resolve("packages");
        this.clientPackageDir = packageRoot.resolve("client");
        this.serverPackageDir = packageRoot.resolve("server");
        this.supportLibraryDir = packageRoot.resolve("support").resolve("lib");
        this.runtimeDir = installRoot.resolve("Runtime");
        this.manifestDir = installRoot.resolve("manifests");
        this.uninstallDir = installRoot.resolve("Uninstall");
        this.saveDir = userDataRoot.resolve("saves");
        this.settingsDir = roamingConfigRoot.resolve("settings");
        this.profilesDir = roamingConfigRoot.resolve("profiles");
        this.logsDir = localStateRoot.resolve("logs");
        this.cacheDir = localStateRoot.resolve("cache");
        this.packageSeedRoot = packageSeedRoot(installRoot, localStateRoot);
    }

    static LauncherConfig defaults() {
        Path installRoot = installRootOverride();
        if (installRoot == null) installRoot = detectBundledInstallRoot();
        if (installRoot == null) installRoot = defaultInstallRoot();
        Path userDataRoot = defaultUserDataRoot();
        Path roamingConfigRoot = defaultRoamingConfigRoot();
        Path localStateRoot = defaultLocalStateRoot();
        return new LauncherConfig(
                installRoot.toAbsolutePath().normalize(),
                userDataRoot.toAbsolutePath().normalize(),
                roamingConfigRoot.toAbsolutePath().normalize(),
                localStateRoot.toAbsolutePath().normalize());
    }

    private static Path defaultInstallRoot() {
        String programFiles = System.getenv("ProgramFiles");
        if (programFiles != null && !programFiles.isBlank()) {
            return Path.of(programFiles, STUDIO_NAME, APP_NAME);
        }
        String local = System.getenv("LOCALAPPDATA");
        if (local != null && !local.isBlank()) {
            return Path.of(local, "Programs", STUDIO_NAME, APP_NAME);
        }
        return Path.of(System.getProperty("user.home"), "Games", STUDIO_NAME, "TheMechanist");
    }

    private static Path installRootOverride() {
        String override = System.getProperty("mechanist.launcher.installRoot");
        if (override != null && !override.isBlank()) return Path.of(override);
        String envOverride = System.getenv("MECHANIST_LAUNCHER_INSTALL_ROOT");
        if (envOverride != null && !envOverride.isBlank()) return Path.of(envOverride);
        return null;
    }

    private static Path rootOverride(String propertyName, String environmentName) {
        String override = System.getProperty(propertyName);
        if (override != null && !override.isBlank()) return Path.of(override);
        String envOverride = System.getenv(environmentName);
        if (envOverride != null && !envOverride.isBlank()) return Path.of(envOverride);
        return null;
    }

    private static Path packageSeedRoot(Path installRoot, Path localStateRoot) {
        String override = System.getProperty("mechanist.launcher.packageSeedRoot");
        if (override != null && !override.isBlank()) return Path.of(override).toAbsolutePath().normalize();
        String envOverride = System.getenv("MECHANIST_LAUNCHER_PACKAGE_SEED_ROOT");
        if (envOverride != null && !envOverride.isBlank()) return Path.of(envOverride).toAbsolutePath().normalize();
        return localStateRoot.resolve("package-seed").toAbsolutePath().normalize();
    }

    private static Path detectBundledInstallRoot() {
        Path codeSource = codeSourcePath();
        if (codeSource == null) return null;
        Path cursor = Files.isRegularFile(codeSource) ? codeSource.getParent() : codeSource;
        for (int depth = 0; cursor != null && depth < 8; depth++, cursor = cursor.getParent()) {
            if (Files.isDirectory(cursor.resolve("manifests"))
                    && Files.isDirectory(cursor.resolve("packages"))
                    && Files.isDirectory(cursor.resolve("packages").resolve("client"))) {
                return cursor.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    private static Path codeSourcePath() {
        try {
            File file = new File(LauncherConfig.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return file.toPath().toAbsolutePath().normalize();
        } catch (URISyntaxException | RuntimeException ex) {
            return null;
        }
    }

    private static Path defaultUserDataRoot() {
        Path override = rootOverride("mechanist.launcher.userDataRoot", "MECHANIST_LAUNCHER_USER_DATA_ROOT");
        if (override != null) return override;
        String home = System.getProperty("user.home");
        if (isWindows()) return Path.of(home, "Saved Games", STUDIO_NAME, APP_NAME);
        String xdg = System.getenv("XDG_DATA_HOME");
        if (xdg != null && !xdg.isBlank()) return Path.of(xdg, STUDIO_NAME, "TheMechanist");
        return Path.of(home, ".local", "share", STUDIO_NAME, "TheMechanist");
    }

    private static Path defaultRoamingConfigRoot() {
        Path override = rootOverride("mechanist.launcher.roamingConfigRoot", "MECHANIST_LAUNCHER_ROAMING_CONFIG_ROOT");
        if (override != null) return override;
        if (isWindows()) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) return Path.of(appData, STUDIO_NAME, APP_NAME);
        }
        String xdg = System.getenv("XDG_CONFIG_HOME");
        if (xdg != null && !xdg.isBlank()) return Path.of(xdg, STUDIO_NAME, "TheMechanist");
        return Path.of(System.getProperty("user.home"), ".config", STUDIO_NAME, "TheMechanist");
    }

    private static Path defaultLocalStateRoot() {
        Path override = rootOverride("mechanist.launcher.localStateRoot", "MECHANIST_LAUNCHER_LOCAL_STATE_ROOT");
        if (override != null) return override;
        if (isWindows()) {
            String local = System.getenv("LOCALAPPDATA");
            if (local != null && !local.isBlank()) return Path.of(local, STUDIO_NAME, APP_NAME);
        }
        String cache = System.getenv("XDG_CACHE_HOME");
        if (cache != null && !cache.isBlank()) return Path.of(cache, STUDIO_NAME, "TheMechanist");
        return Path.of(System.getProperty("user.home"), ".cache", STUDIO_NAME, "TheMechanist");
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }
}
