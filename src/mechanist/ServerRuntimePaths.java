package mechanist;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Owns separated runtime save/world directories for desktop single-player and server worlds. */
final class ServerRuntimePaths {
    static final String VERSION = "server-runtime-paths-0.9.10ic";

    enum SaveDomain { SINGLE_PLAYER, SERVER }

    static Path root() { return GameStorageManager.get().savesDir(); }
    static Path singlePlayerRoot() { return root().resolve("singleplayer"); }
    static Path serverRoot() { return root().resolve("server"); }
    static Path singlePlayerWorldDir() { return singlePlayerRoot().resolve("worlds"); }
    static Path serverWorldDir() { return serverRoot().resolve("worlds"); }
    static Path serverSlotDir() { return serverRoot().resolve("slots"); }
    static Path serverStateFile() { return serverRoot().resolve("server_state.properties"); }
    static Path remoteSessionDir() { return serverRoot().resolve("remote-sessions"); }
    static Path remoteWorldAuthorityDir() { return serverRoot().resolve("remote-world-authority"); }

    static Path remoteSessionLedgerPath(String worldId) {
        return remoteSessionDir().resolve(cleanWorldId(worldId) + ".sessions.properties");
    }

    static Path remoteTurnLedgerPath(String worldId) {
        return remoteWorldAuthorityDir().resolve(cleanWorldId(worldId) + ".turns.properties");
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
        Files.createDirectories(remoteWorldAuthorityDir());
    }

    static String singlePlayerWorldReference(String worldId) {
        return "singleplayer/worlds/" + cleanWorldId(worldId) + ".mechworld";
    }

    static String serverWorldReference(String worldId) {
        return "server/worlds/" + cleanWorldId(worldId) + ".mechworld";
    }


    /**
     * Preflight only: validate a pre-existing canonical server-world definition.
     * This deliberately does not generate a missing world or mount a remote map.
     */
    static HiveWorldDefinition loadServerWorldDefinitionIfPresent(ServerConfig config)
            throws IOException {
        String reference = serverWorldReference(config.worldId());
        Path file = checkedServerWorldFile(serverWorldDir(), reference, config);
        if (!Files.exists(file, java.nio.file.LinkOption.NOFOLLOW_LINKS)) return null;
        return loadServerWorldDefinition(serverWorldDir(), reference, config);
    }

    /** Required read for a server-domain reference; never falls back to single-player or a seed rebuild. */
    static HiveWorldDefinition loadServerWorldDefinition(
            Path serverWorldDirectory, String reference, ServerConfig config) throws IOException {
        Path file = checkedServerWorldFile(serverWorldDirectory, reference, config);
        if (!Files.isRegularFile(file, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Server world definition is missing or not a regular file: " + file);
        }
        long size = Files.size(file);
        if (size == 0L || size > 64L * 1024L * 1024L) {
            throw new IOException("Server world definition has invalid size: " + size);
        }
        // Properties.load normally accepts duplicate keys with last-value-wins.
        // A hosted-world identity must not depend on the order of conflicting fields.
        java.util.Properties properties = new java.util.Properties() {
            @Override
            public synchronized Object put(Object key, Object value) {
                if (containsKey(key)) {
                    throw new IllegalArgumentException(
                            "Duplicate server world field: " + key);
                }
                return super.put(key, value);
            }
        };
        try (java.io.InputStream input = Files.newInputStream(file)) {
            properties.load(input);
        } catch (IllegalArgumentException malformed) {
            throw new IOException("Malformed server world properties", malformed);
        }

        requireWorldField(properties, "worlddef.schema", SaveEfficiencyAuthority.WORLD_SCHEMA);
        requireWorldField(properties, "worlddef.version", Persistence.VERSION);
        long seed = strictWorldLong(properties, "worlddef.seed");
        if (seed != config.seed()) throw new IOException("Server world seed does not match host configuration");
        String canonicalName = WorldNamingApi.hiveName(seed);
        requireWorldField(properties, "worlddef.hiveName", canonicalName);
        String canonicalId = WorldNamingApi.worldId(canonicalName, seed);
        if (!canonicalId.equals(config.worldId())) {
            throw new IOException("Host world ID is not the canonical identity for its seed");
        }
        requireWorldField(properties, "worlddef.worldId", canonicalId);
        String encodedSettings = properties.getProperty("worlddef.setup");
        if (encodedSettings == null || !encodedSettings.matches(
                "[0-3]:[0-5]:[0-3]:[0-3]:[0-3]:[0-3]:(true|false)")) {
            throw new IOException("Server world setup is missing or malformed");
        }
        requireWorldField(properties, "worlddef.setup", config.worldSetupEncoded());
        requireWorldField(properties, "worlddef.setupExplicit", "true");
        String stage = properties.getProperty("worlddef.progressStage");
        if (stage == null || stage.isBlank()) throw new IOException("Missing server world progress stage");
        long completed = strictWorldLong(properties, "worlddef.progressCompleted");
        long total = strictWorldLong(properties, "worlddef.progressTotal");
        if (completed < 0L || total < 0L || completed > total
                || completed > Integer.MAX_VALUE || total > Integer.MAX_VALUE) {
            throw new IOException("Invalid server world progress counters");
        }
        for (String key : new String[] {
                "sectorNames", "zoneNames", "zoneHistory", "zoneEpochs",
                "zoneFacilities", "zoneProduction", "zoneStockMovements",
                "zoneConflictLosses", "zoneMaterializedItems",
                "zoneLaborAssignments", "progressLedger"
        }) {
            validateWorldMap(properties, "worlddef." + key);
        }
        HiveWorldDefinition definition;
        try {
            definition = HiveWorldDefinition.readFrom(properties, seed);
        } catch (RuntimeException malformed) {
            throw new IOException("Canonical server world definition could not be decoded", malformed);
        }
        if (definition.seed != seed || !canonicalId.equals(definition.worldId)
                || !encodedSettings.equals(definition.settings().encode())
                || !definition.hasExplicitSettings()) {
            throw new IOException("Decoded server world identity or setup differs from the stored definition");
        }
        return definition;
    }

    private static Path checkedServerWorldFile(
            Path directory, String reference, ServerConfig config) throws IOException {
        if (directory == null || config == null) throw new IOException("Server world reference is incomplete");
        String id = config.worldId();
        if (id == null || !id.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,119}")
                || !id.equals(id.trim())) {
            throw new IOException("Unsafe server world ID");
        }
        if (!serverWorldReference(id).equals(reference)) {
            throw new IOException("Server world reference does not match its server-domain identity");
        }
        Path dir = directory.toAbsolutePath().normalize();
        Path parent = dir.getParent();
        if (parent == null || dir.getFileName() == null
                || !"worlds".equals(dir.getFileName().toString())
                || parent.getFileName() == null
                || !"server".equals(parent.getFileName().toString())
                || Files.isSymbolicLink(parent) || Files.isSymbolicLink(dir)) {
            throw new IOException("Server world path is outside a regular server/worlds domain");
        }
        Path file = dir.resolve(id + ".mechworld");
        if (Files.isSymbolicLink(file)) throw new IOException("Symbolic-link server world is not allowed");
        return file;
    }

    private static void requireWorldField(
            java.util.Properties properties, String key, String expected) throws IOException {
        if (!expected.equals(properties.getProperty(key))) {
            throw new IOException("Server world field mismatch: " + key);
        }
    }

    private static long strictWorldLong(java.util.Properties properties, String key)
            throws IOException {
        String value = properties.getProperty(key);
        if (value == null || !value.matches("-?[0-9]+")) {
            throw new IOException("Missing or malformed server world integer: " + key);
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException invalid) {
            throw new IOException("Server world integer overflow: " + key, invalid);
        }
    }

    /** Persistence.decList is forgiving for single-player; the server boundary must not be. */
    private static void validateWorldMap(java.util.Properties properties, String key)
            throws IOException {
        String encoded = properties.getProperty(key);
        if (encoded == null) throw new IOException("Missing server world map: " + key);
        if (encoded.isEmpty()) return;
        String[] entries = encoded.split(",", -1);
        if (entries.length > 100_000) throw new IOException("Oversized server world map: " + key);
        java.util.HashSet<String> keys = new java.util.HashSet<>();
        for (String entry : entries) {
            try {
                byte[] bytes = java.util.Base64.getUrlDecoder().decode(entry);
                if (!java.util.Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(bytes).equals(entry)) {
                    throw new IllegalArgumentException("noncanonical base64");
                }
                String row = java.nio.charset.StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                        .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                        .decode(java.nio.ByteBuffer.wrap(bytes)).toString();
                int equals = row.indexOf('=');
                if (equals <= 0 || !keys.add(row.substring(0, equals))) {
                    throw new IllegalArgumentException("missing or repeated map key");
                }
            } catch (IllegalArgumentException | java.nio.charset.CharacterCodingException malformed) {
                throw new IOException("Malformed server world map: " + key, malformed);
            }
        }
    }

    static String auditSummary() {
        return "authority=" + VERSION
                + " " + GameStorageManager.get().auditSummary()
                + " singlePlayer=" + singlePlayerRoot()
                + " server=" + serverRoot()
                + " serverState=" + serverStateFile()
                + " serverWorlds=" + serverWorldDir()
                + " remoteSessions=" + remoteSessionDir()
                + " remoteWorldAuthority=" + remoteWorldAuthorityDir();
    }

    private static String cleanWorldId(String worldId) {
        String id = worldId == null || worldId.isBlank() ? "unknown-world" : worldId.trim();
        String cleaned = id.replace('/', '_').replace('\\', '_');
        cleaned = cleaned.replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.substring(0, Math.min(120, cleaned.length()));
    }

    private ServerRuntimePaths() { }
}
