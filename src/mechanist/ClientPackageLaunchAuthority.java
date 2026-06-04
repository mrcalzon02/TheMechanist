package mechanist;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Guarantees that the packaged client runs with PACKAGE_client as its process
 * working directory even when invoked from a parent folder with:
 *
 *   java -jar PACKAGE_client/TheMechanist.jar
 *
 * The current asset runtime still has legacy package-relative lookups. This
 * authority makes the direct jar path behave like the known-good launch form:
 *
 *   cd PACKAGE_client
 *   java -jar TheMechanist.jar
 */
final class ClientPackageLaunchAuthority {
    private static final String RELAUNCHED_PROPERTY = "mechanist.client.packageRootRelaunched";

    private ClientPackageLaunchAuthority() {}

    static boolean relaunchFromPackageRootIfRequired(String[] args) {
        if (Boolean.getBoolean(RELAUNCHED_PROPERTY)) return false;
        Path jarPath = currentJarPath();
        if (jarPath == null) return false;
        if (!jarPath.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) return false;
        Path runtimeRoot = jarPath.getParent();
        if (runtimeRoot == null) return false;
        if (!isClientRuntimeRoot(runtimeRoot)) return false;
        Path cwd = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        if (cwd.equals(runtimeRoot.toAbsolutePath().normalize())) return false;

        try {
            int exit = relaunch(jarPath, runtimeRoot, args == null ? new String[0] : args);
            System.exit(exit);
            return true;
        } catch (Throwable t) {
            System.err.println("The Mechanist client could not relaunch from its package root.");
            System.err.println("Jar: " + jarPath);
            System.err.println("Runtime root: " + runtimeRoot);
            System.err.println("Current directory: " + cwd);
            t.printStackTrace(System.err);
            System.exit(90);
            return true;
        }
    }

    private static boolean isClientRuntimeRoot(Path runtimeRoot) {
        File assets = runtimeRoot.resolve("assets").toFile();
        File jar = runtimeRoot.resolve("TheMechanist.jar").toFile();
        File title = runtimeRoot.resolve("assets/a/r/source/Title/TITEL.png").toFile();
        File sound = runtimeRoot.resolve("assets/sound").toFile();
        return jar.isFile() && assets.isDirectory() && (title.isFile() || sound.isDirectory());
    }

    private static int relaunch(Path jarPath, Path runtimeRoot, String[] args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(javaBinary());
        command.add("-D" + RELAUNCHED_PROPERTY + "=true");
        command.add("-jar");
        command.add(jarPath.getFileName().toString());
        for (String arg : args) command.add(arg);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(runtimeRoot.toFile());
        if (!isWindows()) pb.inheritIO();
        Process process = pb.start();
        return isWindows() ? 0 : process.waitFor();
    }

    private static String javaBinary() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null && !javaHome.isBlank()) {
            String exe = isWindows() ? "javaw.exe" : "java";
            File candidate = Paths.get(javaHome, "bin", exe).toFile();
            if (candidate.isFile()) return candidate.getAbsolutePath();
        }
        return isWindows() ? "javaw.exe" : "java";
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        return os.contains("win");
    }

    private static Path currentJarPath() {
        try {
            URI uri = TheMechanist.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path p = Paths.get(uri).toAbsolutePath().normalize();
            if (p.toFile().isFile()) return p;
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
