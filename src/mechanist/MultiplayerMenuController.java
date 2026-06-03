package mechanist;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

/** Swing-safe model/controller for the launcher multiplayer menu. */
final class MultiplayerMenuController implements AutoCloseable {
    static final String VERSION = "multiplayer-menu-controller-0.9.10hs";
    private static final int MAX_HISTORY = 12;
    private static final int MAX_FAVORITES = 24;
    private final ArrayList<ConnectionHistoryItem> history = new ArrayList<>();
    private final ArrayList<FavoriteServer> favorites = new ArrayList<>();
    private int historyIndex = 0;
    private int favoriteIndex = 0;
    private boolean inputActive = false;
    private String directInput = "127.0.0.1:" + NetworkPortAuthority.DEFAULT_GAME_PORT;
    private String status = "Enter a direct address, select a recent server, save a favorite, or host from the selected world settings.";
    private HostBindingResult activeHost = null;

    MultiplayerMenuController() { load(); }

    void activate(UserProfileAuthority.Profile profile) {
        SteamNetworkingBridge.SteamEnvironment steam = SteamNetworkingBridge.detect();
        status = "Multiplayer surface active. Steam=" + steam.steamLaunchEnvironment() + " wrapper=" + steam.wrapperAvailable()
                + "; direct TCP supports IPv4 and bracketed IPv6 addresses.";
    }

    List<ConnectionHistoryItem> history() { return List.copyOf(history); }
    List<FavoriteServer> favorites() { return List.copyOf(favorites); }
    int historyIndex() { return historyIndex; }
    int favoriteIndex() { return favoriteIndex; }
    boolean inputActive() { return inputActive; }
    String directInput() { return directInput; }
    String status() { return status; }
    boolean hasActiveHost() { return activeHost != null && activeHost.success(); }
    String activeHostLine() { return activeHost == null ? "No local host bound." : activeHost.compactLine(); }
    void setStatus(String status) { this.status = status == null || status.isBlank() ? this.status : status; }

    void beginDirectEdit() { inputActive = true; status = "Editing direct server address. Examples: 192.168.1.10:25565 or [2001:db8::1]:25565."; }
    void endDirectEdit() { inputActive = false; }

    boolean handleKeyPressed(int code) {
        if (!inputActive) return false;
        if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_ENTER) { inputActive = false; return true; }
        if (code == KeyEvent.VK_BACK_SPACE) {
            if (!directInput.isEmpty()) directInput = directInput.substring(0, directInput.length() - 1);
            return true;
        }
        if (code == KeyEvent.VK_DELETE) { directInput = ""; return true; }
        return false;
    }

    void keyTyped(char ch) {
        if (!inputActive) return;
        if (directInput.length() >= 96) return;
        if (Character.isLetterOrDigit(ch) || ch == '.' || ch == ':' || ch == '[' || ch == ']' || ch == '-' || ch == '_' ) {
            directInput += ch;
        }
    }

    void cycleHistory(int delta) {
        if (history.isEmpty()) { status = "No recent server history yet."; return; }
        historyIndex = Math.floorMod(historyIndex + delta, history.size());
        directInput = history.get(historyIndex).endpoint();
        status = "Selected recent server: " + directInput;
    }

    void cycleFavorite(int delta) {
        if (favorites.isEmpty()) { status = "No favorite servers saved yet."; return; }
        favoriteIndex = Math.floorMod(favoriteIndex + delta, favorites.size());
        FavoriteServer fav = favorites.get(favoriteIndex);
        directInput = fav.endpoint();
        status = "Selected favorite: " + fav.name() + " / " + fav.endpoint();
    }

    NetworkPortAuthority.Endpoint joinDirect() {
        NetworkPortAuthority.Endpoint endpoint = NetworkPortAuthority.parseEndpoint(directInput);
        remember(endpoint.display(), "Direct server");
        status = "Prepared direct join endpoint " + endpoint.display() + ". Transport handoff will use encrypted chat/session packets when the client connector opens.";
        save();
        return endpoint;
    }

    void addFavoriteFromDirect(String name) {
        NetworkPortAuthority.Endpoint endpoint = NetworkPortAuthority.parseEndpoint(directInput);
        String cleanName = name == null || name.isBlank() ? "Favorite " + (favorites.size() + 1) : name.trim();
        String id = UUID.nameUUIDFromBytes((cleanName + "|" + endpoint.normalizedHostKey()).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        favorites.removeIf(f -> f.id().equals(id) || f.endpoint().equalsIgnoreCase(endpoint.display()));
        favorites.add(0, new FavoriteServer(id, cleanName, endpoint.host(), endpoint.port(), endpoint.display(), false, Instant.now().toString()));
        while (favorites.size() > MAX_FAVORITES) favorites.remove(favorites.size() - 1);
        favoriteIndex = 0;
        status = "Saved favorite: " + cleanName + " / " + endpoint.display();
        save();
    }

    NetworkPortAuthority.Endpoint joinFavorite() {
        if (favorites.isEmpty()) {
            status = "No favorite server is selected.";
            return NetworkPortAuthority.parseEndpoint(directInput);
        }
        FavoriteServer fav = favorites.get(Math.max(0, Math.min(favoriteIndex, favorites.size() - 1)));
        directInput = fav.endpoint();
        remember(fav.endpoint(), fav.name());
        status = "Prepared favorite join endpoint " + fav.name() + " / " + fav.endpoint() + ".";
        save();
        return NetworkPortAuthority.parseEndpoint(fav.endpoint());
    }

    void joinViaSteamFriend() {
        SteamNetworkingBridge.SteamEnvironment env = SteamNetworkingBridge.detect();
        if (!env.steamLaunchEnvironment() || !env.wrapperAvailable()) {
            status = "Steam friend joining is hidden by environment: " + env.reason() + ".";
            return;
        }
        status = "Steam friend lobby handoff requested. Steam wrapper detected; socket adapter must be supplied by the packaged Steamworks integration.";
    }

    HostBindingResult hostFromWorld(long seed, String worldName, String worldId, WorldSetupSettings settings, int maxPlayers) {
        closeActiveHostQuietly();
        int port = NetworkPortAuthority.firstAvailableGamePort();
        boolean preferSteam = SteamNetworkingBridge.detect().steamLaunchEnvironment();
        ServerConfig config = MultiplayerHostBindingService.configFromWorld(seed, worldName, worldId, settings, maxPlayers, port, preferSteam);
        activeHost = MultiplayerHostBindingService.bind(config);
        status = activeHost.compactLine();
        save();
        return activeHost;
    }

    void remember(String endpoint, String label) {
        if (endpoint == null || endpoint.isBlank()) return;
        history.removeIf(h -> h.endpoint().equalsIgnoreCase(endpoint.trim()));
        history.add(0, new ConnectionHistoryItem(endpoint.trim(), label == null || label.isBlank() ? "Server" : label.trim(), Instant.now().toString()));
        while (history.size() > MAX_HISTORY) history.remove(history.size() - 1);
        historyIndex = 0;
    }

    private void load() {
        Properties p = new Properties();
        Path file = settingsFile();
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) { p.load(in); }
            catch (IOException ex) { DebugLog.warn("MULTIPLAYER_MENU", "Could not load multiplayer settings: " + ex.getMessage()); }
        }
        directInput = p.getProperty("direct.input", directInput);
        int historyCount = parseSmallCount(p.getProperty("history.count"), MAX_HISTORY);
        for (int i = 0; i < historyCount && i < MAX_HISTORY; i++) {
            String endpoint = p.getProperty("history." + i + ".endpoint", "");
            if (!endpoint.isBlank()) history.add(new ConnectionHistoryItem(endpoint, p.getProperty("history." + i + ".label", "Server"), p.getProperty("history." + i + ".lastJoined", Instant.now().toString())));
        }
        int favoriteCount = parseSmallCount(p.getProperty("favorite.count"), MAX_FAVORITES);
        for (int i = 0; i < favoriteCount && i < MAX_FAVORITES; i++) {
            String endpoint = p.getProperty("favorite." + i + ".endpoint", "");
            if (!endpoint.isBlank()) {
                NetworkPortAuthority.Endpoint ep = NetworkPortAuthority.parseEndpoint(endpoint);
                favorites.add(new FavoriteServer(p.getProperty("favorite." + i + ".id", UUID.randomUUID().toString()),
                        p.getProperty("favorite." + i + ".name", "Favorite"), ep.host(), ep.port(), ep.display(),
                        Boolean.parseBoolean(p.getProperty("favorite." + i + ".steam", "false")),
                        p.getProperty("favorite." + i + ".created", Instant.now().toString())));
            }
        }
    }

    void save() {
        Properties p = new Properties();
        p.setProperty("version", VERSION);
        p.setProperty("direct.input", directInput == null ? "" : directInput);
        p.setProperty("history.count", String.valueOf(history.size()));
        for (int i = 0; i < history.size(); i++) {
            ConnectionHistoryItem h = history.get(i);
            p.setProperty("history." + i + ".endpoint", h.endpoint());
            p.setProperty("history." + i + ".label", h.label());
            p.setProperty("history." + i + ".lastJoined", h.lastJoinedIso());
        }
        p.setProperty("favorite.count", String.valueOf(favorites.size()));
        for (int i = 0; i < favorites.size(); i++) {
            FavoriteServer f = favorites.get(i);
            p.setProperty("favorite." + i + ".id", f.id());
            p.setProperty("favorite." + i + ".name", f.name());
            p.setProperty("favorite." + i + ".endpoint", f.endpoint());
            p.setProperty("favorite." + i + ".steam", String.valueOf(f.steamLobby()));
            p.setProperty("favorite." + i + ".created", f.createdIso());
        }
        try {
            Files.createDirectories(settingsFile().getParent());
            try (OutputStream out = Files.newOutputStream(settingsFile())) { p.store(out, "The Mechanist multiplayer menu state"); }
        } catch (IOException ex) {
            DebugLog.warn("MULTIPLAYER_MENU", "Could not save multiplayer settings: " + ex.getMessage());
        }
    }

    private static Path settingsFile() { return Paths.get("settings", "multiplayer_servers.properties"); }

    private static int parseSmallCount(String raw, int cap) {
        try { return Math.max(0, Math.min(cap, Integer.parseInt(raw == null ? "0" : raw.trim()))); }
        catch (NumberFormatException ex) { return 0; }
    }

    private void closeActiveHostQuietly() {
        if (activeHost != null) {
            try { activeHost.close(); } catch (Exception ex) { DebugLog.warn("MULTIPLAYER_HOST_CLOSE", ex.getClass().getSimpleName() + ": " + ex.getMessage()); }
            activeHost = null;
        }
    }

    @Override public void close() {
        closeActiveHostQuietly();
        save();
    }

    static String auditSummary() {
        SteamNetworkingBridge.SteamEnvironment env = SteamNetworkingBridge.detect();
        return "authority=" + VERSION + " steamEnv=" + env.steamLaunchEnvironment() + " steamWrapper=" + env.wrapperAvailable()
                + " portRange=" + NetworkPortAuthority.CUSTOM_GAME_PORT_MIN + "-" + NetworkPortAuthority.CUSTOM_GAME_PORT_MAX;
    }

    record ConnectionHistoryItem(String endpoint, String label, String lastJoinedIso) {
        ConnectionHistoryItem {
            endpoint = endpoint == null || endpoint.isBlank() ? "127.0.0.1:" + NetworkPortAuthority.DEFAULT_GAME_PORT : endpoint.trim();
            label = label == null || label.isBlank() ? "Server" : label.trim();
            lastJoinedIso = lastJoinedIso == null || lastJoinedIso.isBlank() ? Instant.now().toString() : lastJoinedIso.trim();
        }
        String shortLine() { return label + " / " + endpoint; }
    }

    record FavoriteServer(String id, String name, String host, int port, String endpoint, boolean steamLobby, String createdIso) {
        FavoriteServer {
            id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id.trim();
            name = name == null || name.isBlank() ? "Favorite" : name.trim();
            NetworkPortAuthority.Endpoint ep = NetworkPortAuthority.parseEndpoint(endpoint == null || endpoint.isBlank() ? host + ":" + port : endpoint);
            host = ep.host();
            port = ep.port();
            endpoint = ep.display();
            createdIso = createdIso == null || createdIso.isBlank() ? Instant.now().toString() : createdIso.trim();
        }
        String shortLine() { return name + " / " + endpoint + (steamLobby ? " / STEAM" : ""); }
    }
}
