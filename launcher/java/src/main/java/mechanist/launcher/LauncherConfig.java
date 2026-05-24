package mechanist.launcher;

import java.nio.file.Path;

final class LauncherConfig {
    static final String APP_NAME = "The Mechanist";
    static final String REPO_URL = "https://github.com/mrcalzon02/TheMechanist.git";
    static final String DEFAULT_BRANCH = "main";

    final Path installRoot;
    final Path repoDir;
    final Path saveDir;
    final Path settingsDir;
    final Path logsDir;

    private LauncherConfig(Path installRoot) {
        this.installRoot = installRoot;
        this.repoDir = installRoot.resolve("repo");
        this.saveDir = installRoot.resolve("saves");
        this.settingsDir = installRoot.resolve("settings");
        this.logsDir = installRoot.resolve("logs");
    }

    static LauncherConfig defaults() {
        String local = System.getenv("LOCALAPPDATA");
        Path root;
        if (local != null && !local.isBlank()) {
            root = Path.of(local, "TheMechanist");
        } else {
            root = Path.of(System.getProperty("user.home"), ".local", "share", "TheMechanist");
        }
        return new LauncherConfig(root.toAbsolutePath().normalize());
    }
}
