package mechanist;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * Tiny command-line preflight used by RUN_THE_MECHANIST_WINDOWS.bat.
 * It intentionally avoids constructing Swing widgets so startup failures are reported
 * before the full game attempts to create a JFrame.
 */
public final class WindowsLaunchHealthCheck {
    public static void main(String[] args) {
        int exit = run();
        if (exit != 0) System.exit(exit);
    }

    static int run() {
        String javaVersion = System.getProperty("java.version", "unknown");
        String osName = System.getProperty("os.name", "unknown");
        String userDir = System.getProperty("user.dir", "unknown");
        System.out.println("The Mechanist Windows launch preflight");
        System.out.println("java.version=" + javaVersion);
        System.out.println("os.name=" + osName);
        System.out.println("user.dir=" + userDir);
        int major = parseJavaMajor(javaVersion);
        if (major < 17) {
            System.err.println("ERROR: Java 17 or newer is required. Detected: " + javaVersion);
            return 17;
        }
        Path jar = Path.of("TheMechanist.jar").toAbsolutePath().normalize();
        if (!Files.isRegularFile(jar)) {
            System.err.println("ERROR: TheMechanist.jar was not found at " + jar);
            return 2;
        }
        LwjglRenderBackendProbe.ProbeResult lwjgl = LwjglRenderBackendProbe.result();
        System.out.println("lwjgl.classpath=" + (lwjgl.available() ? "present" : "missing"));
        System.out.println("lwjgl.status=" + lwjgl.statusLine());
        System.out.println("doom.renderer=java2d-software");
        boolean doomEnabled = savedDoomModeEnabled();
        boolean requireLwjgl = Boolean.getBoolean("mechanist.requireLwjgl")
                || Boolean.parseBoolean(System.getenv().getOrDefault("MECHANIST_REQUIRE_LWJGL", "false"));
        System.out.println("doom.saved_enabled=" + doomEnabled);
        System.out.println("lwjgl.required=" + requireLwjgl);
        if (requireLwjgl && !lwjgl.available()) {
            System.err.println("ERROR: LWJGL was required but not found on the runtime classpath. Keep lib/lwjgl/*.jar beside the launcher or disable the require flag.");
            return 23;
        }
        try {
            boolean headless = GraphicsEnvironment.isHeadless();
            System.out.println("graphics.headless=" + headless);
            if (headless) {
                System.err.println("ERROR: Java reports a headless graphics environment. The desktop client needs a Windows display session.");
                return 3;
            }
        } catch (Throwable t) {
            System.err.println("ERROR: Graphics environment check failed: " + t.getClass().getName() + ": " + t.getMessage());
            return 4;
        }
        System.out.println("preflight=OK");
        return 0;
    }


    private static boolean savedDoomModeEnabled() {
        Path options = Path.of("settings", "options.properties");
        if (!Files.isRegularFile(options)) return false;
        Properties pr = new Properties();
        try (var in = Files.newInputStream(options)) {
            pr.load(in);
            return Boolean.parseBoolean(pr.getProperty("doomModeEnabled", "false"));
        } catch (IOException ex) {
            return false;
        }
    }

    public static int parseJavaMajor(String version) {
        if (version == null || version.isBlank()) return 0;
        String clean = version.trim().toLowerCase(Locale.ROOT);
        try {
            if (clean.startsWith("1.")) {
                int dot = clean.indexOf('.', 2);
                String part = dot > 0 ? clean.substring(2, dot) : clean.substring(2);
                return Integer.parseInt(part.replaceAll("[^0-9].*$", ""));
            }
            StringBuilder digits = new StringBuilder();
            for (int i = 0; i < clean.length(); i++) {
                char c = clean.charAt(i);
                if (Character.isDigit(c)) digits.append(c); else break;
            }
            return digits.length() == 0 ? 0 : Integer.parseInt(digits.toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private WindowsLaunchHealthCheck() {}
}
