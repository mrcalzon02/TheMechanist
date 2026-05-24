package mechanist;

import java.util.*;

/** Runtime startup envelope shared by the current desktop shell and future launch handoff. */
final class RuntimeProfile {
    static final String VERSION = "runtime-profile-0.9.10fc";

    final ApplicationRuntimeMode requestedMode;
    final ApplicationRuntimeMode effectiveMode;
    final String profileName;
    final String saveId;
    final String worldId;
    final String modManifestPath;
    final java.util.List<String> enabledMods;
    final boolean internalServerRequested;
    final boolean launcherOrchestrationAvailable;
    final boolean headlessServerAvailable;
    final boolean hotRestartAvailable;

    private RuntimeProfile(ApplicationRuntimeMode requestedMode,
                           ApplicationRuntimeMode effectiveMode,
                           String profileName,
                           String saveId,
                           String worldId,
                           String modManifestPath,
                           java.util.List<String> enabledMods,
                           boolean internalServerRequested,
                           boolean launcherOrchestrationAvailable,
                           boolean headlessServerAvailable,
                           boolean hotRestartAvailable) {
        this.requestedMode = requestedMode == null ? ApplicationRuntimeMode.CLIENT : requestedMode;
        this.effectiveMode = effectiveMode == null ? ApplicationRuntimeMode.CLIENT : effectiveMode;
        this.profileName = clean(profileName, "desktop-default");
        this.saveId = clean(saveId, "none");
        this.worldId = clean(worldId, "none");
        this.modManifestPath = clean(modManifestPath, "none");
        this.enabledMods = java.util.Collections.unmodifiableList(new ArrayList<>(enabledMods == null ? java.util.Collections.emptyList() : enabledMods));
        this.internalServerRequested = internalServerRequested;
        this.launcherOrchestrationAvailable = launcherOrchestrationAvailable;
        this.headlessServerAvailable = headlessServerAvailable;
        this.hotRestartAvailable = hotRestartAvailable;
    }

    static RuntimeProfile defaultProfile() {
        return new RuntimeProfile(ApplicationRuntimeMode.CLIENT, ApplicationRuntimeMode.CLIENT,
                "desktop-default", "none", "none", "none", java.util.Collections.emptyList(), false, false, false, false);
    }

    static RuntimeProfile fromArgs(String[] args) {
        ApplicationRuntimeMode requested = ApplicationRuntimeMode.CLIENT;
        String profile = "desktop-default";
        String save = "none";
        String world = "none";
        String manifest = "none";
        java.util.List<String> mods = new ArrayList<>();
        boolean internalServer = false;
        if (args != null) {
            for (String arg : args) {
                if (arg == null) continue;
                String trimmed = arg.trim();
                if (trimmed.startsWith("--mode=")) {
                    requested = ApplicationRuntimeMode.parse(trimmed.substring("--mode=".length()));
                } else if (trimmed.startsWith("--profile=")) {
                    profile = trimmed.substring("--profile=".length());
                } else if (trimmed.startsWith("--save=")) {
                    save = trimmed.substring("--save=".length());
                } else if (trimmed.startsWith("--world=")) {
                    world = trimmed.substring("--world=".length());
                } else if (trimmed.startsWith("--mod-manifest=")) {
                    manifest = trimmed.substring("--mod-manifest=".length());
                } else if (trimmed.startsWith("--mods=")) {
                    String csv = trimmed.substring("--mods=".length());
                    for (String part : csv.split(",")) {
                        String mod = part.trim();
                        if (!mod.isEmpty()) mods.add(mod);
                    }
                } else if (trimmed.equals("--internal-server")) {
                    internalServer = true;
                }
            }
        }
        ApplicationRuntimeMode effective = requested == ApplicationRuntimeMode.TEST_BATCH ? ApplicationRuntimeMode.TEST_BATCH : ApplicationRuntimeMode.CLIENT;
        return new RuntimeProfile(requested, effective, profile, save, world, manifest, mods, internalServer, false, false, false);
    }

    String compactLine() {
        return "requested=" + requestedMode + " effective=" + effectiveMode + " profile=" + profileName
                + " mods=" + enabledMods.size() + " manifest=" + modManifestPath
                + " internalServerRequested=" + internalServerRequested;
    }

    java.util.List<String> lines() {
        ArrayList<String> out = new ArrayList<>();
        out.add("Runtime profile " + VERSION);
        out.add("Requested mode: " + requestedMode);
        out.add("Effective mode: " + effectiveMode);
        out.add("Profile name: " + profileName);
        out.add("Save id: " + saveId);
        out.add("World id: " + worldId);
        out.add("Mod manifest path: " + modManifestPath);
        out.add("Enabled mod tokens: " + (enabledMods.isEmpty() ? "none" : String.join(", ", enabledMods)));
        out.add("Internal server requested: " + internalServerRequested);
        out.add("Launcher orchestration available: " + launcherOrchestrationAvailable);
        out.add("Headless server available: " + headlessServerAvailable);
        out.add("Hot restart available: " + hotRestartAvailable);
        return out;
    }

    private static String clean(String value, String fallback) {
        if (value == null) return fallback;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}
