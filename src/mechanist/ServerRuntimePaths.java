package mechanist;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Owns separated runtime save/world directories for desktop single-player and server worlds. */
final class ServerRuntimePaths {
    static final String VERSION = "server-runtime-paths-0.9.10ib";

    enum SaveDomain { SINGLE_PLAYER, SERVER }

    static Path root() { return GameStorageManager.get().savesDir(); }
    static Path singlePlayerRoot() { return root().resolve("singleplayer"); }
    static Path serverRoot() { return root().resolve("server"); }
    static Path singlePlayerWorldDir() { return singlePlayerRoot().resolve("worlds"); }
    static Path serverWorldDir() { return serverRoot().resolve("worlds"); }
    static Path serverSlotDir() { return serverRoot().resolve("slots"); }
    static Path serverStateFile() { return serverRoot().resolve("server_state.properties"); }
    static Path remoteSessionDir() { return serverRoot().resolve("remote-sessions"); }

    static Path remoteSessionLedgerPath(String worldId) {
        return remoteSessionDir().resolve(cleanWorldId(worldId) + ".sessions.properties");
    }

    static Path saveRoot(SaveDomain domain) {
        return domain == SaveDomain.SERVER ? serverRoot() : singlePlayerRoot();
    }

    static Path worldDir(SaveDomain domain) {
        return domain == SaveDomain.SERVER ? serverWorldDir() : singlePlayerWorldDir();
    }

    static Path slotPath(SaveDomain domain, int slot) {
        if (domain == SaveDomain.SERVER) return serverSlotPath(slot);
        String name;
        if (slot == GamePanel.AUTOSAVE_HOURLY_SLOT) name = "autosave_hourly.mechsave";
        else if (slot == GamePanel.AUTOSAVE_ZONE_SLOT) name = "autosave_zone_transition.mechsave";
        else name = "slot" + Math.max(1, Math.min(GamePanel.SAVE_SLOT_COUNT, slot)) + ".mechsave";
        return singlePlayerRoot().resolve(name);
    }

    static Path serverSlotPath(int slot) {
        return serverSlotDir().resolve("server_slot" + Math.max(1, Math.min(GamePanel.SAVE_SLOT_COUNT, slot)) + ".mechsave");
    }

    static void ensureSinglePlayerDirectories() throws IOException {
        Files.createDirectories(singlePlayerRoot());
        Files.createDirectories(singlePlayerWorldDir());
    }

    static void ensureServerDirectories() throws IOException {
        Files.createDirectories(serverRoot());
        Files.createDirectories(serverWorldDir());
        Files.createDirectories(serverSlotDir());
        Files.createDirectories(remoteSessionDir());
    }

    static String singlePlayerWorldReference(String worldId) {
        return "singleplayer/worlds/" + cleanWorldId(worldId) + ".mechworld";
    }

    static String serverWorldReference(String worldId) {
        return "server/worlds/" + cleanWorldId(worldId) + ".mechworld";
    }

    static String auditSummary() {
        return "authority=" + VERSION
                + " " + GameStorageManager.get().auditSummary()
                + " singlePlayer=" + singlePlayerRoot()
                + " server=" + serverRoot()
                + " serverState=" + serverStateFile()
                + " serverWorlds=" + serverWorldDir()
                + " remoteSessions=" + remoteSessionDir();
    }

    private static String cleanWorldId(String worldId) {
        String id = worldId == null || worldId.isBlank() ? "unknown-world" : worldId.trim();
        String cleaned = id.replace('/', '_').replace('\\', '_');
        cleaned = cleaned.replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.substring(0, Math.min(120, cleaned.length()));
    }

    private ServerRuntimePaths() { }
}
