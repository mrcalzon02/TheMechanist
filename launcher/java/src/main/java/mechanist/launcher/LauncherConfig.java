package mechanist.launcher;

import java.nio.file.Path;

final class LauncherConfig {
    static final String APP_NAME = "The Mechanist";
    static final String STUDIO_NAME = "StellarCore";
    static final String REPO_URL = "https://github.com/mrcalzon02/TheMechanist.git";
    static final String DEFAULT_BRANCH = "main";

    final Path installRoot;
    final Path launcherDir;
    final Path gameRoot;
    final Path repoDir;
    final Path runtimeDir;
    final Path manifestDir;
    final Path uninstallDir;
    final Path saveDir;
    final Path settingsDir;
    final Path profilesDir;
    final Path logsDir;
    final Path cacheDir;

    private LauncherConfig(Path installRoot, Path userDataRoot, Path roamingConfigRoot, Path localStateRoot) {
        this.installRoot = installRoot;
        this.launcherDir = installRoot.resolve("Launcher");
        this.gameRoot = installRoot.resolve("Game");
        this.repoDir = gameRoot.resolve("current");
        this.runtimeDir = installRoot.resolve("Runtime");
        this.manifestDir = installRoot.resolve("Manifest");
        this.uninstallDir = installRoot.resolve("Uninstall");
        this.saveDir = userDataRoot.resolve("saves");
        this.settingsDir = roamingConfigRoot.resolve("settings");
        this.profilesDir = roamingConfigRoot.resolve("profiles");
        this.logsDir = localStateRoot.resolve("logs");
        this.cacheDir = localStateRoot.resolve("cache");
    }

    static LauncherConfig defaults() {
        Path installRoot = defaultInstallRoot();
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

    private static Path defaultUserDataRoot() {
        String home = System.getProperty("user.home");
        if (isWindows()) return Path.of(home, "Saved Games", STUDIO_NAME, APP_NAME);
        String xdg = System.getenv("XDG_DATA_HOME");
        if (xdg != null && !xdg.isBlank()) return Path.of(xdg, STUDIO_NAME, "TheMechanist");
        return Path.of(home, ".local", "share", STUDIO_NAME, "TheMechanist");
    }

    private static Path defaultRoamingConfigRoot() {
        if (isWindows()) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) return Path.of(appData, STUDIO_NAME, APP_NAME);
        }
        String xdg = System.getenv("XDG_CONFIG_HOME");
        if (xdg != null && !xdg.isBlank()) return Path.of(xdg, STUDIO_NAME, "TheMechanist");
        return Path.of(System.getProperty("user.home"), ".config", STUDIO_NAME, "TheMechanist");
    }

    private static Path defaultLocalStateRoot() {
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
