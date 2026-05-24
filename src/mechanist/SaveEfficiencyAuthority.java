package mechanist;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/** Owns save/world-file persistence cataloging and slot/world definition separation. */
final class SaveEfficiencyAuthority {
    static final String VERSION = "save-efficiency-authority-0.9.10gu";
    static final String SLOT_SCHEMA = "slot-v3-character-world-reference";
    static final String SINGLE_PLAYER_BUNDLED_SLOT_SCHEMA = "slot-v4-singleplayer-bundled-world-snapshot";
    static final String WORLD_SCHEMA = "world-definition-v3-runtime-ledger-state";

    /**
     * Namespaces that belong to the mutable world authority for server/multiplayer persistence.
     * Single-player bundled slots still embed these as part of their save-time world snapshot.
     */
    private static final String[] WORLD_OWNED_RUNTIME_PREFIXES = new String[] {
            "base.", "factions.", "npc.", "inn.", "bank.", "crime.",
            "scavenge.", "machine.", "logistics."
    };

    static void writeWorldReference(GamePanel game, Properties slot) {
        if (game == null || slot == null || game.atlas == null || game.atlas.hiveWorld == null) return;
        HiveWorldDefinition d = game.atlas.hiveWorld;
        slot.setProperty("save.schema", SLOT_SCHEMA);
        slot.setProperty("save.worldId", d.worldId);
        slot.setProperty("save.worldSeed", Long.toString(d.seed));
        slot.setProperty("save.worldFile", ServerRuntimePaths.singlePlayerWorldReference(d.worldId));
        slot.setProperty("save.worldDefinitionEmbedded", "false");
        slot.setProperty("save.persistenceModel", "character-slot-plus-world-state");
    }

    static Properties worldDefinitionProperties(HiveWorldDefinition definition) {
        Properties p = new Properties();
        if (definition != null) {
            definition.writeTo(p);
            p.setProperty("worlddef.schema", WORLD_SCHEMA);
            writeWorldCatalogMetadata(p);
        }
        return p;
    }

    static Properties worldRuntimeProperties(GamePanel game) {
        Properties p = game == null || game.atlas == null ? new Properties() : worldDefinitionProperties(game.atlas.hiveWorld);
        if (game != null) {
            Persistence.writeWorldState(game.world, p);
            writeWorldRuntimeMetadata(game, p);
            writeWorldCatalogMetadata(p);
        }
        return p;
    }

    static void writeWorldRuntimeMetadata(GamePanel game, Properties world) {
        if (game == null || world == null) return;
        world.setProperty("world.schema", "runtime-world-state-v1");
        world.setProperty("world.persistenceModel", "world-definition-plus-mutable-state");
        world.setProperty("world.runtimeSavedTurn", Integer.toString(game.turn));
        world.setProperty("world.runtimeSavedWorldTurn", Long.toString(game.worldTurn));
        world.setProperty("world.currentCharacterPlayerId", playerIdFor(game));
        PlayerFactionWorldAuthority.writeWorldFactionLedger(game, world);
        PlayerFactionAutonomyAuthority.writeAutonomyLedger(game, world);
        PlayerFactionAutonomousTickAuthority.writeTickLedger(game, world);
    }

    static void writePlayerFactionContinuityLedger(GamePanel game, Properties world) {
        if (game == null || world == null) return;
        String playerId = playerIdFor(game);
        String factionId = playerFactionIdFor(game, playerId);
        String playerName = game.active == null ? "unknown" : cleanRecord(game.active.name);
        String factionName = factionId.equals("none") ? "none" : cleanRecord(playerName + "'s Freehold");
        String memberRole = game.baseClaimed ? "leader" : "unaffiliated";
        world.setProperty("world.faction.playerFactionId", factionId);
        world.setProperty("world.faction.playerFactionName", factionName);
        world.setProperty("world.faction.playerMember." + playerId + ".name", playerName);
        world.setProperty("world.faction.playerMember." + playerId + ".role", memberRole);
        world.setProperty("world.faction.playerMember." + playerId + ".reserved", "true");
        world.setProperty("world.faction.playerMember." + playerId + ".rankTrack", "player-command");
        world.setProperty("world.faction.npcCommandTrack", "separate-npc-command-structure");
        world.setProperty("world.faction.autonomyPolicy", game.baseClaimed ? "continues-without-player-present" : "no-player-faction-founded");
    }

    static String playerIdFor(GamePanel game) {
        if (game == null || game.active == null) return "unknown-player";
        String base = game.active.name + "|" + game.active.job + "|" + game.active.birthWorldTurn + "|" + game.seed;
        return "player-" + Integer.toUnsignedString(base.hashCode(), 36);
    }

    static String playerFactionMembershipRecord(GamePanel game) {
        if (game == null || game.active == null) return "none";
        String playerId = playerIdFor(game);
        String name = cleanRecord(game.active.name);
        String role = game.baseClaimed ? "founder-leader" : "unaffiliated";
        String factionId = playerFactionIdFor(game, playerId);
        return playerId + "|" + name + "|" + factionId + "|" + role + "|npc-chain=separate|reserved=true";
    }

    static String playerFactionIdFor(GamePanel game, String playerId) {
        if (game == null || !game.baseClaimed) return "none";
        String basis = (playerId == null ? "unknown-player" : playerId) + "|" + game.claimedRoomId + "|" + game.baseX + "," + game.baseY;
        return "player-faction-" + Integer.toUnsignedString(basis.hashCode(), 36);
    }

    static String cleanRecord(String text) {
        if (text == null || text.isBlank()) return "unknown";
        return text.replace('|', '/').replace('\n', ' ').replace('\r', ' ').trim();
    }

    static void stripEmbeddedWorldDefinition(Properties slot) {
        if (slot == null) return;
        ArrayList<String> remove = new ArrayList<>();
        for (String key : slot.stringPropertyNames()) {
            if (key.startsWith("worlddef.")) remove.add(key);
        }
        for (String key : remove) slot.remove(key);
    }

    static void stripMutableWorldStateFromSlot(Properties slot) {
        if (slot == null) return;
        ArrayList<String> remove = new ArrayList<>();
        for (String key : slot.stringPropertyNames()) {
            if (key.startsWith("world.")) remove.add(key);
        }
        for (String key : remove) slot.remove(key);
    }

    static void copyWorldOwnedRuntimeNamespaces(Properties fromSlot, Properties toWorld) {
        if (fromSlot == null || toWorld == null) return;
        for (String key : fromSlot.stringPropertyNames()) {
            if (isWorldOwnedRuntimeKey(key)) toWorld.setProperty(key, fromSlot.getProperty(key, ""));
        }
    }

    static void stripWorldOwnedRuntimeNamespacesFromCharacterSlot(Properties slot) {
        if (slot == null) return;
        ArrayList<String> remove = new ArrayList<>();
        for (String key : slot.stringPropertyNames()) if (isWorldOwnedRuntimeKey(key)) remove.add(key);
        for (String key : remove) slot.remove(key);
    }

    static boolean isWorldOwnedRuntimeKey(String key) {
        if (key == null) return false;
        for (String prefix : WORLD_OWNED_RUNTIME_PREFIXES) if (key.startsWith(prefix)) return true;
        return false;
    }

    static void prepareCharacterSlot(GamePanel game, Properties slot, Properties worldRuntime) {
        if (slot == null) return;
        writeWorldReference(game, slot);
        if (game != null) {
            PlayerFactionWorldAuthority.writeCharacterFactionAttachment(game, slot);
            slot.setProperty("char.factionResumePolicy", "world-file-membership-authority");
            slot.setProperty("char.serverSlotRole", "character-attachment-only");
        }
        copyWorldOwnedRuntimeNamespaces(slot, worldRuntime);
        stripEmbeddedWorldDefinition(slot);
        stripMutableWorldStateFromSlot(slot);
        stripWorldOwnedRuntimeNamespacesFromCharacterSlot(slot);
        writeSlotCatalogMetadata(slot, worldRuntime);
    }



    static void prepareSinglePlayerBundledSlot(GamePanel game, Properties slot, Properties worldRuntime) {
        if (slot == null) return;
        writeWorldReference(game, slot);
        if (game != null) {
            PlayerFactionWorldAuthority.writeCharacterFactionAttachment(game, slot);
            slot.setProperty("char.factionResumePolicy", "embedded-world-membership-authority");
        }
        copyWorldOwnedRuntimeNamespaces(slot, worldRuntime);
        if (worldRuntime != null) {
            for (String key : worldRuntime.stringPropertyNames()) {
                if (key.startsWith("worlddef.") || key.startsWith("world.")) {
                    slot.setProperty(key, worldRuntime.getProperty(key, ""));
                }
            }
        }
        slot.setProperty("save.schema", SINGLE_PLAYER_BUNDLED_SLOT_SCHEMA);
        slot.setProperty("save.persistenceModel", "singleplayer-character-plus-embedded-world-snapshot");
        slot.setProperty("save.worldDefinitionEmbedded", "true");
        slot.setProperty("save.mutableWorldStateEmbedded", "true");
        slot.setProperty("save.worldFileRole", "external-world-reference-and-server-continuity-copy");
        writeSinglePlayerBundledSlotCatalogMetadata(slot, worldRuntime);
    }

    static void writeSinglePlayerBundledSlotCatalogMetadata(Properties slot, Properties worldRuntime) {
        if (slot == null) return;
        SaveCatalog slotCatalog = catalog("singleplayer-bundled-slot", slot);
        SaveCatalog worldCatalog = catalog("world-runtime", worldRuntime);
        int embeddedWorld = 0;
        int embeddedWorldDef = 0;
        for (String key : slot.stringPropertyNames()) {
            if (key.startsWith("world.")) embeddedWorld++;
            if (key.startsWith("worlddef.")) embeddedWorldDef++;
        }
        slot.setProperty("save.catalog.authority", VERSION);
        slot.setProperty("save.catalog.slotSchema", SINGLE_PLAYER_BUNDLED_SLOT_SCHEMA);
        slot.setProperty("save.catalog.worldSchema", WORLD_SCHEMA);
        slot.setProperty("save.catalog.embeddedWorldDefinition", Boolean.toString(embeddedWorldDef > 0));
        slot.setProperty("save.catalog.embeddedWorldDefinitionKeys", Integer.toString(embeddedWorldDef));
        slot.setProperty("save.catalog.embeddedMutableWorldState", Boolean.toString(embeddedWorld > 0));
        slot.setProperty("save.catalog.embeddedMutableWorldStateKeys", Integer.toString(embeddedWorld));
        slot.setProperty("save.catalog.slotKeys", Integer.toString(slotCatalog.keyCount()));
        slot.setProperty("save.catalog.slotApproxChars", Integer.toString(slotCatalog.approxChars()));
        slot.setProperty("save.catalog.slotWorldDefKeys", Integer.toString(slotCatalog.worldDefinitionKeys()));
        slot.setProperty("save.catalog.worldKeys", Integer.toString(worldCatalog.keyCount()));
        slot.setProperty("save.catalog.worldApproxChars", Integer.toString(worldCatalog.approxChars()));
        slot.setProperty("save.catalog.slotNamespaces", namespaceSummary(slotCatalog.namespaces()));
        slot.setProperty("save.catalog.worldNamespaces", namespaceSummary(worldCatalog.namespaces()));
    }

    static void saveRuntimeWorldFile(GamePanel game, Properties worldRuntime, ServerRuntimePaths.SaveDomain domain) throws IOException {
        if (game == null || game.atlas == null || game.atlas.hiveWorld == null || worldRuntime == null) return;
        Path dir = ServerRuntimePaths.worldDir(domain == null ? ServerRuntimePaths.SaveDomain.SINGLE_PLAYER : domain);
        Files.createDirectories(dir);
        Path file = dir.resolve(game.atlas.hiveWorld.worldId + ".mechworld");
        try (OutputStream out = Files.newOutputStream(file)) {
            worldRuntime.store(out, "The Mechanist generated hive world and mutable world state");
        }
    }

    static void mergeReferencedWorldState(Properties slot) {
        if (slot == null) return;
        String schema = slot.getProperty("save.schema", "").trim();
        boolean embeddedMutable = Boolean.parseBoolean(slot.getProperty("save.mutableWorldStateEmbedded", "false"));
        boolean hasEmbeddedWorld = false;
        for (String key : slot.stringPropertyNames()) {
            if (key.startsWith("world.")) { hasEmbeddedWorld = true; break; }
        }
        if (SINGLE_PLAYER_BUNDLED_SLOT_SCHEMA.equals(schema) || embeddedMutable || hasEmbeddedWorld) {
            slot.setProperty("save.worldStateMerged", "embedded-singleplayer-snapshot");
            return;
        }
        String ref = slot.getProperty("save.worldFile", "").trim();
        if (ref.isEmpty()) return;
        Path file = ref.startsWith("saves/") || ref.startsWith("saves\\") ? Path.of(ref) : ServerRuntimePaths.root().resolve(ref);
        if (!Files.exists(file)) return;
        Properties world = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            world.load(in);
        } catch (IOException ex) {
            DebugLog.warn("WORLD_STATE_MERGE", "Could not read referenced world file " + file + ": " + ex.getMessage());
            return;
        }
        int merged = 0;
        for (String key : world.stringPropertyNames()) {
            if (key.startsWith("world.") || isWorldOwnedRuntimeKey(key)) {
                slot.setProperty(key, world.getProperty(key, ""));
                merged++;
            }
        }
        slot.setProperty("save.worldStateMerged", Boolean.toString(merged > 0));
        slot.setProperty("save.worldStateMergedKeys", Integer.toString(merged));
        slot.setProperty("save.worldStateMergeModel", "world-plus-world-owned-runtime-namespaces");
    }
    static void writeSlotCatalogMetadata(Properties slot, Properties worldDefinition) {
        if (slot == null) return;
        stripEmbeddedWorldDefinition(slot);
        stripMutableWorldStateFromSlot(slot);
        SaveCatalog slotCatalog = catalog("slot", slot);
        SaveCatalog worldCatalog = catalog("world-definition", worldDefinition);
        slot.setProperty("save.catalog.authority", VERSION);
        slot.setProperty("save.catalog.slotSchema", SLOT_SCHEMA);
        slot.setProperty("save.catalog.worldSchema", WORLD_SCHEMA);
        slot.setProperty("save.catalog.embeddedWorldDefinition", "false");
        slot.setProperty("save.catalog.embeddedMutableWorldState", "false");
        slot.setProperty("save.catalog.slotKeys", Integer.toString(slotCatalog.keyCount()));
        slot.setProperty("save.catalog.slotApproxChars", Integer.toString(slotCatalog.approxChars()));
        slot.setProperty("save.catalog.slotWorldDefKeys", Integer.toString(slotCatalog.worldDefinitionKeys()));
        slot.setProperty("save.catalog.worldKeys", Integer.toString(worldCatalog.keyCount()));
        slot.setProperty("save.catalog.worldApproxChars", Integer.toString(worldCatalog.approxChars()));
        slot.setProperty("save.catalog.slotNamespaces", namespaceSummary(slotCatalog.namespaces()));
        slot.setProperty("save.catalog.worldNamespaces", namespaceSummary(worldCatalog.namespaces()));
    }

    static void writeWorldCatalogMetadata(Properties worldDefinition) {
        if (worldDefinition == null) return;
        SaveCatalog c = catalog("world-definition", worldDefinition);
        worldDefinition.setProperty("worlddef.catalog.authority", VERSION);
        worldDefinition.setProperty("worlddef.catalog.schema", WORLD_SCHEMA);
        worldDefinition.setProperty("worlddef.catalog.keys", Integer.toString(c.keyCount()));
        worldDefinition.setProperty("worlddef.catalog.approxChars", Integer.toString(c.approxChars()));
        worldDefinition.setProperty("worlddef.catalog.namespaces", namespaceSummary(c.namespaces()));
    }

    static SaveCatalog catalog(String label, Properties properties) {
        LinkedHashMap<String, Integer> namespaces = new LinkedHashMap<>();
        int keys = 0;
        int chars = 0;
        int worldDefKeys = 0;
        if (properties != null) {
            for (String key : properties.stringPropertyNames()) {
                keys++;
                if (key.startsWith("worlddef.")) worldDefKeys++;
                String value = properties.getProperty(key, "");
                chars += key.length() + value.length();
                namespaces.merge(namespaceOf(key), 1, Integer::sum);
            }
        }
        return new SaveCatalog(label == null ? "unknown" : label, keys, chars, worldDefKeys, namespaces);
    }

    static String auditLine(String label, Properties slot, Properties worldDefinition) {
        SaveCatalog s = catalog("slot", slot);
        SaveCatalog w = catalog("world-definition", worldDefinition);
        return (label == null ? "save" : label)
                + " authority=" + VERSION
                + " slotKeys=" + s.keyCount()
                + " slotApproxChars=" + s.approxChars()
                + " slotWorldDefKeys=" + s.worldDefinitionKeys()
                + " slotNamespaces=" + namespaceSummary(s.namespaces())
                + " worldKeys=" + w.keyCount()
                + " worldApproxChars=" + w.approxChars()
                + " worldNamespaces=" + namespaceSummary(w.namespaces());
    }

    static String compactRuntimeCatalog(GamePanel game) {
        Properties slot = new Properties();
        Persistence.writeCore(game, slot);
        Properties world = worldRuntimeProperties(game);
        prepareSinglePlayerBundledSlot(game, slot, world);
        return auditLine("runtime", slot, world);
    }


    static String itemizedRuntimeCatalog(GamePanel game) {
        Properties slot = new Properties();
        Persistence.writeCore(game, slot);
        Properties world = worldRuntimeProperties(game);
        prepareSinglePlayerBundledSlot(game, slot, world);
        return itemizedCatalog(slot, world);
    }

    static String itemizedCatalog(Properties slot, Properties worldDefinition) {
        StringBuilder sb = new StringBuilder();
        sb.append("Save itemization authority=").append(VERSION).append('\n');
        appendLayer(sb, "SLOT .mechsave", slot, slotOwnershipNotes());
        appendLayer(sb, "WORLD .mechworld", worldDefinition, worldOwnershipNotes());
        return sb.toString().trim();
    }

    private static void appendLayer(StringBuilder sb, String title, Properties props, Map<String, String> notes) {
        SaveCatalog c = catalog(title, props);
        sb.append(title).append(" keys=").append(c.keyCount()).append(" approxChars=").append(c.approxChars()).append('\n');
        LinkedHashMap<String, PrefixStat> stats = prefixStats(props);
        int index = 1;
        for (Map.Entry<String, PrefixStat> e : stats.entrySet()) {
            PrefixStat st = e.getValue();
            String prefix = e.getKey();
            sb.append(index++).append(". ").append(prefix)
                    .append(" - keys=").append(st.keys)
                    .append(" approxChars=").append(st.chars)
                    .append("; owns ").append(notes.getOrDefault(prefix, genericOwnership(prefix)))
                    .append("; examples=").append(st.examples())
                    .append('\n');
        }
    }

    private static LinkedHashMap<String, PrefixStat> prefixStats(Properties props) {
        LinkedHashMap<String, PrefixStat> out = new LinkedHashMap<>();
        if (props == null) return out;
        ArrayList<String> keys = new ArrayList<>(props.stringPropertyNames());
        keys.sort(String::compareTo);
        for (String key : keys) {
            String prefix = namespaceOf(key);
            String value = props.getProperty(key, "");
            PrefixStat stat = out.computeIfAbsent(prefix, k -> new PrefixStat());
            stat.keys++;
            stat.chars += key.length() + value.length();
            if (stat.examples.size() < 6) stat.examples.add(key);
        }
        return out;
    }

    private static Map<String, String> slotOwnershipNotes() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        m.put("save", "save metadata, schema, world-file reference, persistence model, and catalog metadata; resume-critical, small, should remain in slot");
        m.put("run", "current run counters, player position, condition meters, defeat stats, and unlocked knowledge; resume-critical");
        m.put("atlas", "current sector/zone/floor coordinate and atlas seed; resume-critical navigation pointer, not full world definition");
        m.put("char", "selected character identity, stable playerId, portrait, job, stats, and visited type memory; resume-critical player identity");
        m.put("clothing", "legacy equipped-clothing bridge; transitional until equipment containers fully replace it");
        m.put("inventory", "legacy carried inventory list; resume-critical but should eventually defer to item instances/containers");
        m.put("base", "single-player bundled world snapshot base state; stripped from server character slots and owned by the world file");
        m.put("script", "legacy Imperial Script carried/stashed counts; resume-critical until fully ledger-backed");
        m.put("light", "portable light state and expiring light instances; resume-critical environmental gameplay state");
        m.put("combat", "loaded weapon shots and terrain integrity overrides; resume-critical combat/current-slice state");
        m.put("item", "item provenance, instance registry, container registry, and audit report; mixed resume-critical registry plus audit text");
        m.put("visited", "visited zone instance/type memory; resume-critical exploration and unlock state");
        m.put("machine", "single-player bundled world snapshot machine queue history; stripped from server character slots and owned by the world file");
        m.put("logistics", "single-player bundled world snapshot logistics state; stripped from server character slots and owned by the world file");
        m.put("factions", "single-player bundled world snapshot faction state; stripped from server character slots and owned by the world file");
        m.put("npc", "single-player bundled world snapshot NPC faction sites; stripped from server character slots and owned by the world file");
        m.put("inn", "single-player bundled world snapshot news and broadcast state; stripped from server character slots and owned by the world file");
        m.put("bank", "single-player bundled world snapshot bank/vault state; stripped from server character slots and owned by the world file");
        m.put("crime", "single-player bundled world snapshot crime/custody state; stripped from server character slots and owned by the world file");
        m.put("scavenge", "single-player bundled world snapshot scavenge cooldown state; stripped from server character slots and owned by the world file");
        m.put("world", "current mutable zone slice, NPCs, map objects, environmental records, and zone summary text; resume-critical current-slice state but should be separated from generated world definition");
        m.put("settings", "options summary snapshot; useful compatibility/debug context, not authoritative settings store");
        return m;
    }

    private static Map<String, String> worldOwnershipNotes() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        m.put("worlddef", "generated hive-world identity, setup, names, histories, facility/provenance/production/stock/loss/materialization/labor ledgers, progress, schema, and catalog metadata; embedded in single-player bundled slots and authoritative in .mechworld for server/multiplayer");
        m.put("world", "current mutable active-world state: zone identity, objects, NPCs, light/noise/hazard/trap records, room factions, population ledgers, player-faction membership continuity, and runtime turn marker; embedded in single-player bundled slots and authoritative in .mechworld for server/multiplayer");
        m.put("base", "world-owned player-faction base/room/object/recruit state; server slots reference it instead of storing it");
        m.put("factions", "world-owned faction standing, pressure, contracts, strategic plans, simulation reports, and public bulletin state");
        m.put("npc", "world-owned NPC faction sites and strategic world simulation anchors");
        m.put("inn", "world-owned news, daily issue, broadcast, player-news, and inspection records");
        m.put("bank", "world-owned bank accounts, balances, vault, alarm, and lockbox state");
        m.put("crime", "world-owned custody, punishment, patrol, and Arbites cooldown state");
        m.put("scavenge", "world-owned object/target scavenge cooldown records");
        m.put("machine", "world-owned active or recent machine queue state");
        m.put("logistics", "world-owned logistics intent/source/route/contract/preflight/lifecycle/execution state");
        return m;
    }

    private static String genericOwnership(String prefix) {
        return "uncategorized persistence namespace; review before pruning";
    }

    private static final class PrefixStat {
        int keys;
        int chars;
        final ArrayList<String> examples = new ArrayList<>();
        String examples() { return examples.isEmpty() ? "none" : String.join(",", examples); }
    }


    static String architectureSummary(GamePanel game) {
        Properties slot = new Properties();
        Persistence.writeCore(game, slot);
        Properties world = worldRuntimeProperties(game);
        prepareSinglePlayerBundledSlot(game, slot, world);
        SaveCatalog s = catalog("singleplayer-bundled-slot", slot);
        SaveCatalog w = catalog("world", world);
        return "Save architecture " + VERSION
                + "\nSINGLE-PLAYER .mechsave: bundled character plus embedded world snapshot; keys=" + s.keyCount() + " approxChars=" + s.approxChars()
                + "; carries char.playerId, inventory/equipment/account/knowledge/current position plus worlddef.* and world.* as save-time world state."
                + "\nSERVER/MULTIPLAYER .mechsave: character/player attachment and world-file reference only; server does not require player slots to run; world-owned namespaces are stripped from server character slots."
                + "\nWORLD .mechworld: generated world plus mutable world state; keys=" + w.keyCount() + " approxChars=" + w.approxChars()
                + "; carries worlddef.*, world.*, NPC/object/light/noise/hazard/trap/population records, player-faction membership continuity, and faction autonomy plans and autonomous faction tick ledger."
                + "\nEfficiency options: single-player deliberately stores a large exact world snapshot; server/multiplayer keeps character slots separated from world authority.";
    }
    static String auditSummary() {
        return "authority=" + VERSION
                + " slotSchema=" + SLOT_SCHEMA
                + " worldSchema=" + WORLD_SCHEMA
                + " singlePlayerSlot=character-plus-embedded-world-snapshot"
                + " serverSlot=character-player-run-attachment-only-with-world-owned-namespaces-stripped"
                + " worldFile=generated-definition-plus-mutable-world-state";
    }

    private static String namespaceOf(String key) {
        if (key == null || key.isBlank()) return "unknown";
        int dot = key.indexOf('.');
        return dot <= 0 ? key : key.substring(0, dot);
    }

    private static String namespaceSummary(LinkedHashMap<String, Integer> namespaces) {
        if (namespaces == null || namespaces.isEmpty()) return "none";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> e : namespaces.entrySet()) {
            if (!first) sb.append(';');
            first = false;
            sb.append(e.getKey()).append(':').append(e.getValue());
        }
        return sb.toString();
    }

    record SaveCatalog(String label, int keyCount, int approxChars, int worldDefinitionKeys, LinkedHashMap<String, Integer> namespaces) { }

    private SaveEfficiencyAuthority() { }
}
