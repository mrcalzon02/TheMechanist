package mechanist.launcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

final class GitInstallService {
    private final LauncherConfig config;
    private final Consumer<String> output;

    GitInstallService(LauncherConfig config, Consumer<String> output) {
        this.config = config;
        this.output = output;
    }

    boolean gitAvailable() {
        try {
            int exit = ProcessRunner.run(null, output, "git", "--version");
            return exit == 0;
        } catch (Exception ex) {
            output.accept("Git is not available: " + ex.getMessage());
            return false;
        }
    }

    void ensureDirectories() throws IOException {
        Files.createDirectories(config.installRoot);
        Files.createDirectories(config.saveDir);
        Files.createDirectories(config.settingsDir);
        Files.createDirectories(config.logsDir);
    }

    void installOrUpdate(String branch) throws IOException, InterruptedException {
        ensureDirectories();
        if (!Files.isDirectory(config.repoDir.resolve(".git"))) {
            output.accept("Cloning game repository into " + config.repoDir);
            Files.createDirectories(config.repoDir.getParent());
            int exit = ProcessRunner.run(config.installRoot, output,
                    "git", "clone", "--branch", branch, LauncherConfig.REPO_URL, config.repoDir.toString());
            if (exit != 0) throw new IOException("git clone failed with exit code " + exit);
            return;
        }

        output.accept("Updating existing game repository...");
        int fetch = ProcessRunner.run(config.repoDir, output, "git", "fetch", "origin", branch);
        if (fetch != 0) throw new IOException("git fetch failed with exit code " + fetch);

        int checkout = ProcessRunner.run(config.repoDir, output, "git", "checkout", branch);
        if (checkout != 0) throw new IOException("git checkout failed with exit code " + checkout);

        int pull = ProcessRunner.run(config.repoDir, output, "git", "pull", "--ff-only", "origin", branch);
        if (pull != 0) throw new IOException("git pull --ff-only failed with exit code " + pull + ". Resolve local repo changes or use Repair.");
    }

    void repair(String branch) throws IOException, InterruptedException {
        ensureDirectories();
        if (!Files.isDirectory(config.repoDir.resolve(".git"))) {
            installOrUpdate(branch);
            return;
        }
        output.accept("Repairing repository from origin/" + branch + "...");
        int fetch = ProcessRunner.run(config.repoDir, output, "git", "fetch", "origin", branch);
        if (fetch != 0) throw new IOException("git fetch failed with exit code " + fetch);
        int clean = ProcessRunner.run(config.repoDir, output, "git", "clean", "-fd");
        if (clean != 0) throw new IOException("git clean failed with exit code " + clean);
        int reset = ProcessRunner.run(config.repoDir, output, "git", "reset", "--hard", "origin/" + branch);
        if (reset != 0) throw new IOException("git reset failed with exit code " + reset);
    }

    boolean gameLauncherPresent() {
        return Files.isRegularFile(config.repoDir.resolve("RUN_THE_MECHANIST_WINDOWS.bat"))
                || Files.isRegularFile(config.repoDir.resolve("RUN_THE_MECHANIST_WINDOWS.ps1"))
                || Files.isRegularFile(config.repoDir.resolve("PLAY_THE_MECHANIST_LINUX.sh"));
    }
}
