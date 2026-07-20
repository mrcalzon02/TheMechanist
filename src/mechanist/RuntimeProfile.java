package mechanist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Runtime startup envelope shared by the desktop shell and launcher handoff. */
final class RuntimeProfile {
    static final String VERSION = "runtime-profile-0.9.10fd";

    final ApplicationRuntimeMode requestedMode;
    final ApplicationRuntimeMode effectiveMode;
    final String profileName;
    final String saveId;
    final String worldId;
    final String modManifestPath;
    final List<String> enabledMods;
    final boolean internalServerRequested;
    final boolean launcherOrchestrationAvailable;
    final boolean headlessServerAvailable;
    final boolean hotRestartAvailable;
    final String remoteHost;
    final int remotePort;
    final String remoteServerKey;

    private RuntimeProfile(
            ApplicationRuntimeMode requestedMode,
            ApplicationRuntimeMode effectiveMode,
            String profileName,
            String saveId,
            String worldId,
            String modManifestPath,
            List<String> enabledMods,
            boolean internalServerRequested,
            boolean launcherOrchestrationAvailable,
            boolean headlessServerAvailable,
            boolean hotRestartAvailable,
            String remoteHost,
            int remotePort,
            String remoteServerKey
    ) {
        this.requestedMode = requestedMode == null
                ? ApplicationRuntimeMode.CLIENT
                : requestedMode;
        this.effectiveMode = effectiveMode == null
                ? ApplicationRuntimeMode.CLIENT
                : effectiveMode;
        this.profileName = safeProfile(profileName, "desktop-default");
        this.saveId = clean(saveId, "none");
        this.worldId = clean(worldId, "none");
        this.modManifestPath = clean(modManifestPath, "none");
        this.enabledMods = Collections.unmodifiableList(
                new ArrayList<>(enabledMods == null
                        ? Collections.emptyList()
                        : enabledMods));
        this.internalServerRequested = internalServerRequested;
        this.launcherOrchestrationAvailable = launcherOrchestrationAvailable;
        this.headlessServerAvailable = headlessServerAvailable;
        this.hotRestartAvailable = hotRestartAvailable;
        this.remoteHost = safeHost(remoteHost);
        this.remotePort = NetworkPortAuthority.portWithinAllowedGameRange(remotePort)
                ? remotePort
                : NetworkPortAuthority.DEFAULT_GAME_PORT;
        String endpointKey = normalizedEndpointKey(this.remoteHost, this.remotePort);
        this.remoteServerKey = safeServerKey(remoteServerKey, endpointKey);
    }

    static RuntimeProfile defaultProfile() {
        return new RuntimeProfile(
                ApplicationRuntimeMode.CLIENT,
                ApplicationRuntimeMode.CLIENT,
                "desktop-default",
                "none",
                "none",
                "none",
                Collections.emptyList(),
                false,
                false,
                false,
                false,
                "127.0.0.1",
                NetworkPortAuthority.DEFAULT_GAME_PORT,
                "127.0.0.1:" + NetworkPortAuthority.DEFAULT_GAME_PORT);
    }

    static RuntimeProfile fromArgs(String[] args) {
        ApplicationRuntimeMode requested = ApplicationRuntimeMode.CLIENT;
        String profile = "desktop-default";
        String save = "none";
        String world = "none";
        String manifest = "none";
        List<String> mods = new ArrayList<>();
        boolean internalServer = false;
        String remoteHost = "127.0.0.1";
        int remotePort = NetworkPortAuthority.DEFAULT_GAME_PORT;
        String remoteServerKey = "";

        if (args != null) {
            for (String arg : args) {
                if (arg == null) continue;
                String trimmed = arg.trim();
                if (trimmed.startsWith("--mode=")) {
                    requested = ApplicationRuntimeMode.parse(
                            trimmed.substring("--mode=".length()));
                } else if (trimmed.startsWith("--profile=")) {
                    profile = trimmed.substring("--profile=".length());
                } else if (trimmed.startsWith("--save=")) {
                    save = trimmed.substring("--save=".length());
                } else if (trimmed.startsWith("--world=")) {
                    world = trimmed.substring("--world=".length());
                } else if (trimmed.startsWith("--host=")) {
                    remoteHost = trimmed.substring("--host=".length());
                } else if (trimmed.startsWith("--port=")) {
                    remotePort = NetworkPortAuthority.parsePort(
                            trimmed.substring("--port=".length()),
                            NetworkPortAuthority.DEFAULT_GAME_PORT);
                } else if (trimmed.startsWith("--server-key=")) {
                    remoteServerKey = trimmed.substring("--server-key=".length());
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

        ApplicationRuntimeMode effective;
        if (requested == ApplicationRuntimeMode.TEST_BATCH) {
            effective = ApplicationRuntimeMode.TEST_BATCH;
        } else if (requested == ApplicationRuntimeMode.REMOTE_CLIENT) {
            effective = ApplicationRuntimeMode.REMOTE_CLIENT;
        } else {
            effective = ApplicationRuntimeMode.CLIENT;
        }

        return new RuntimeProfile(
                requested,
                effective,
                profile,
                save,
                world,
                manifest,
                mods,
                internalServer && effective != ApplicationRuntimeMode.REMOTE_CLIENT,
                false,
                true,
                false,
                remoteHost,
                remotePort,
                remoteServerKey);
    }

    boolean remoteClientMode() {
        return effectiveMode == ApplicationRuntimeMode.REMOTE_CLIENT;
    }

    String remoteEndpointDisplay() {
        return displayEndpoint(remoteHost, remotePort);
    }

    String compactLine() {
        return "requested=" + requestedMode
                + " effective=" + effectiveMode
                + " profile=" + profileName
                + " mods=" + enabledMods.size()
                + " manifest=" + modManifestPath
                + " internalServerRequested=" + internalServerRequested
                + " remoteClient=" + remoteClientMode()
                + " remoteEndpoint=" + remoteEndpointDisplay()
                + " remoteServerKey=" + remoteServerKey;
    }

    List<String> lines() {
        ArrayList<String> out = new ArrayList<>();
        out.add("Runtime profile " + VERSION);
        out.add("Requested mode: " + requestedMode);
        out.add("Effective mode: " + effectiveMode);
        out.add("Profile name: " + profileName);
        out.add("Save id: " + saveId);
        out.add("World id: " + worldId);
        out.add("Mod manifest path: " + modManifestPath);
        out.add("Enabled mod tokens: "
                + (enabledMods.isEmpty() ? "none" : String.join(", ", enabledMods)));
        out.add("Internal server requested: " + internalServerRequested);
        out.add("Launcher orchestration available: " + launcherOrchestrationAvailable);
        out.add("Headless server available: " + headlessServerAvailable);
        out.add("Hot restart available: " + hotRestartAvailable);
        out.add("Remote client mode: " + remoteClientMode());
        out.add("Remote endpoint: " + remoteEndpointDisplay());
        out.add("Remote server key: " + remoteServerKey);
        return out;
    }

    private static String clean(String value, String fallback) {
        if (value == null) return fallback;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private static String safeHost(String value) {
        String host = clean(value, "127.0.0.1");
        if (host.length() > 255
                || host.indexOf('|') >= 0
                || host.indexOf('\n') >= 0
                || host.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("remote host is invalid");
        }
        return host;
    }

    private static String safeProfile(String value, String fallback) {
        String profile = clean(value, fallback);
        if (profile.length() < 8 || profile.length() > 128) {
            throw new IllegalArgumentException(
                    "profile identity must be between 8 and 128 characters");
        }
        for (int index = 0; index < profile.length(); index++) {
            char c = profile.charAt(index);
            if (!(Character.isLetterOrDigit(c)
                    || c == '.'
                    || c == '_'
                    || c == ':'
                    || c == '-')) {
                throw new IllegalArgumentException(
                        "profile identity contains an unsafe character");
            }
        }
        return profile;
    }

    private static String safeServerKey(String value, String fallback) {
        String key = clean(value, fallback);
        if (key.length() > 256
                || key.indexOf('|') >= 0
                || key.indexOf('\n') >= 0
                || key.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("remote server key is invalid");
        }
        return key;
    }

    private static String normalizedEndpointKey(String host, int port) {
        return safeHost(host).toLowerCase(Locale.ROOT) + ":" + port;
    }

    private static String displayEndpoint(String host, int port) {
        String useHost = Objects.requireNonNullElse(host, "127.0.0.1");
        return useHost.indexOf(':') >= 0
                && !useHost.startsWith("[")
                ? "[" + useHost + "]:" + port
                : useHost + ":" + port;
    }
}
