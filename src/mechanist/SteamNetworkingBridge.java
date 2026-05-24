package mechanist;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;

/** Optional Steamworks bridge. Uses reflection so non-Steam, no-wrapper builds still compile and run. */
final class SteamNetworkingBridge {
    static final String VERSION = "steam-networking-bridge-0.9.10hs";

    private SteamNetworkingBridge() { }

    static SteamEnvironment detect() {
        boolean launchEnv = false;
        StringBuilder reason = new StringBuilder();
        Map<String, String> env = System.getenv();
        for (String key : new String[]{"SteamAppId", "SteamGameId", "SteamClientLaunch", "STEAM_COMPAT_CLIENT_INSTALL_PATH"}) {
            if (env.containsKey(key) || System.getProperty(key) != null) {
                launchEnv = true;
                reason.append(key).append(' ');
            }
        }
        boolean apiClass = classPresent("com.codedisaster.steamworks.SteamAPI");
        boolean networkingClass = classPresent("com.codedisaster.steamworks.SteamNetworking")
                || classPresent("com.codedisaster.steamworks.SteamNetworkingSockets")
                || classPresent("com.codedisaster.steamworks.SteamGameServerNetworking");
        return new SteamEnvironment(launchEnv, apiClass, networkingClass,
                apiClass ? "steamworks4j" : "none",
                reason.length() == 0 ? "no Steam launch environment variables detected" : reason.toString().trim());
    }

    static HostBindingResult trySteamRelay(ServerConfig config) {
        SteamEnvironment env = detect();
        if (!env.steamLaunchEnvironment() && !env.wrapperAvailable()) {
            return HostBindingResult.failure(config, MultiplayerProtocolState.STEAM_RELAY,
                    "Steam environment and steamworks4j wrapper are not present.");
        }
        if (!env.wrapperAvailable()) {
            return HostBindingResult.failure(config, MultiplayerProtocolState.STEAM_RELAY,
                    "Steam launch hints were detected, but steamworks4j is not on the classpath.");
        }
        SteamInitResult init = initializeSteamApi();
        if (!init.success()) {
            return HostBindingResult.failure(config, MultiplayerProtocolState.STEAM_RELAY,
                    "SteamAPI initialization failed: " + init.message());
        }
        if (!env.networkingSocketsClassAvailable()) {
            return HostBindingResult.failure(config, MultiplayerProtocolState.STEAM_RELAY,
                    "SteamAPI is available, but no Steam networking socket class was found in the Java wrapper.");
        }
        return HostBindingResult.failure(config, MultiplayerProtocolState.STEAM_RELAY,
                "Steam networking socket class is visible, but this build does not hold a verified adapter for that wrapper version.");
    }

    private static SteamInitResult initializeSteamApi() {
        try {
            Class<?> steamApi = Class.forName("com.codedisaster.steamworks.SteamAPI");
            Method init = findNoArgMethod(steamApi, "init");
            if (init == null) return new SteamInitResult(false, "SteamAPI.init() not found");
            Object value = init.invoke(null);
            if (value instanceof Boolean ok) return new SteamInitResult(ok, ok ? "initialized" : "init returned false");
            return new SteamInitResult(true, "init invoked");
        } catch (ClassNotFoundException ex) {
            return new SteamInitResult(false, "SteamAPI class not found");
        } catch (IllegalAccessException ex) {
            return new SteamInitResult(false, "SteamAPI.init() not accessible");
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            return new SteamInitResult(false, cause.getClass().getSimpleName() + ": " + cause.getMessage());
        } catch (LinkageError ex) {
            return new SteamInitResult(false, "Steam native linkage error: " + ex.getMessage());
        }
    }

    private static Method findNoArgMethod(Class<?> type, String name) {
        for (Method m : type.getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == 0) return m;
        }
        return null;
    }

    private static boolean classPresent(String className) {
        try {
            Class.forName(className, false, SteamNetworkingBridge.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ex) {
            return false;
        }
    }

    static String auditSummary() {
        SteamEnvironment env = detect();
        return "authority=" + VERSION + " launchEnv=" + env.steamLaunchEnvironment()
                + " wrapper=" + env.wrapperAvailable() + " socketsClass=" + env.networkingSocketsClassAvailable()
                + " provider=" + env.provider().toLowerCase(Locale.ROOT);
    }

    record SteamEnvironment(boolean steamLaunchEnvironment,
                            boolean wrapperAvailable,
                            boolean networkingSocketsClassAvailable,
                            String provider,
                            String reason) { }

    record SteamInitResult(boolean success, String message) { }
}
