package mechanist.launcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ModStateDetector {
    private ModStateDetector() {}

    static ModState inspect(LauncherConfig config) {
        ArrayList<String> evidence = new ArrayList<>();
        inspectPath(evidence, config.repoDir.resolve("mods"), "game payload mods directory");
        inspectPath(evidence, config.installRoot.resolve("mods"), "install-root mods directory");
        Path saveParent = config.saveDir.getParent();
        if (saveParent != null) inspectPath(evidence, saveParent.resolve("mods"), "user-data mods directory");
        inspectOptions(evidence, config.repoDir.resolve("settings/options.properties"));
        inspectOptions(evidence, config.settingsDir.resolve("options.properties"));
        return new ModState(!evidence.isEmpty(), List.copyOf(evidence));
    }

    private static void inspectPath(ArrayList<String> evidence, Path path, String label) {
        if (path == null || !Files.isDirectory(path)) return;
        try (var stream = Files.list(path)) {
            boolean active = stream.anyMatch(p -> {
                String name = p.getFileName().toString();
                if (name.startsWith(".")) return false;
                String lower = name.toLowerCase(Locale.ROOT);
                return Files.isDirectory(p) || lower.endsWith(".jar") || lower.endsWith(".zip") || lower.endsWith(".json") || lower.endsWith(".toml") || lower.endsWith(".yaml") || lower.endsWith(".yml");
            });
            if (active) evidence.add(label + ": " + redactHome(path));
        } catch (IOException ex) {
            evidence.add(label + " unreadable: " + redactHome(path));
        }
    }

    private static void inspectOptions(ArrayList<String> evidence, Path path) {
        if (path == null || !Files.isRegularFile(path)) return;
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
            if (text.contains("modsenabled=true") || text.contains("mods.enabled=true") || text.contains("modded=true") || text.contains("loadedmods")) {
                evidence.add("mod-related option marker: " + redactHome(path));
            }
        } catch (IOException ex) {
            evidence.add("options unreadable while checking mod state: " + redactHome(path));
        }
    }

    static String supportWarning() {
        return "Modified content appears to be enabled or present. Base-game diagnostic logs and error reports that include modified content are not accepted or reviewed for base-game triage. Sorry, but support time has to remain focused on the base game. If you are a mod author, please support your own mods. Best of luck.";
    }

    private static String redactHome(Path p) {
        if (p == null) return "<null>";
        String s = p.toAbsolutePath().normalize().toString();
        String home = System.getProperty("user.home");
        if (home != null && !home.isBlank()) s = s.replace(home, "<USER_HOME>");
        return s;
    }

    record ModState(boolean modded, List<String> evidence) {}
}
