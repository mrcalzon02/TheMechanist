package mechanist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry for recovered development-tool editors and their default data scaffolds. */
final class SimulationToolSuiteRegistry {
    static final String VERSION = "simulation-tool-suite-registry-0.9.10ir";

    static final String SECTOR_EDITOR = "Sector Editor";
    static final String ZONE_EDITOR = "Zone Editor";
    static final String ROOM_EDITOR = "Room Editor";
    static final String FACTION_EDITOR = "Faction Editor";
    static final String QUEST_EDITOR = "Quest Editor";
    static final String ITEM_EDITOR = "Item Editor";
    static final String TILE_EDITOR = "Tile Editor";
    static final String OBJECT_EDITOR = "Object Editor";
    static final String ENTITY_EDITOR = "Entity Editor";
    static final String INFOPEDIA_EDITOR = "Infopedia Editor";
    static final String KNOWLEDGE_EDITOR = "Knowledge Editor";
    static final String SKILL_EDITOR = "Skill Editor";
    static final String MOD_PACKAGING_EDITOR = "Mod Packaging Editor";

    private static final List<ToolSpec> SPECS = List.of(
            new ToolSpec(SECTOR_EDITOR, true, 10, "world bracket", "legacy sector editor tab", "Dedicated whole-sector world-generation editor with sector map, generation walk, tuning, and overlay contracts.", "coordinates,hazards,faction control,sector map,spawn zone,zone-by-zone generation,room-by-room generation,objects,interactions,light zones,mod export"),
            new ToolSpec(ZONE_EDITOR, false, 20, "zone bracket", "new zone development surface", "Zone biome, density, transit, generated-slice audit, lighting, and street-light setup.", "zone type,density,route,audit replay,lighting profile,street lights,mod export"),
            new ToolSpec(ROOM_EDITOR, true, 30, "room bracket", "legacy room editor tab", "Room footprint, seal, terminal, layout, and palette records.", "dimensions,oxygen seal,terminal,layout palette,placement nodes,mod export"),
            new ToolSpec(FACTION_EDITOR, true, 40, "faction bracket", "legacy faction editor tab", "Faction alignment, resources, diplomacy, command culture, scheme posture, and leadership intelligence records.", "alignment,resources,diplomacy,aggression,culture,schemes,leaders,journals,mod export"),
            new ToolSpec(QUEST_EDITOR, true, 45, "quest bracket", "legacy quest editor pointer / Milestone 07 quest editor doctrine", "Quest identity, localization, lifecycle, objectives, evidence, rewards, consequences, guidance, and validation records.", "quest id,localization,lifecycle,objectives,evidence,rewards,consequences,objective guidance,validation,preview,mod export"),
            new ToolSpec(ITEM_EDITOR, true, 50, "item bracket", "legacy item editor tab", "Item tier, mass, component, durability, unlock, and provenance records.", "tech tier,mass,components,durability,decay,unlock,mod export"),
            new ToolSpec(TILE_EDITOR, true, 55, "tile bracket", "legacy tile/audit descriptor pointer", "Tile glyph, family, walkability, opacity, road/room/boundary role, semantic asset, lighting, hazard, and interaction records.", "glyph,family,walkable,opaque,road role,room role,boundary role,semantic asset,light,hazard,interaction,mod export"),
            new ToolSpec(OBJECT_EDITOR, false, 60, "object bracket", "new object development surface", "Placeable object footprint, material, interaction, lighting hook, and infopedia records.", "object type,material,footprint,interaction,lighting hook,infopedia,mod export"),
            new ToolSpec(ENTITY_EDITOR, false, 70, "entity bracket", "new entity development surface", "Actor archetype, faction, behavior, inventory, hostility, and spawn records.", "archetype,faction,behavior,spawn zone,inventory,hostility,mod export"),
            new ToolSpec(INFOPEDIA_EDITOR, true, 80, "information bracket", "legacy infopedia editor tab", "Nested player reference, lore, taxonomy, revision, and search records.", "category,body,tags,revision,searchability,mod export"),
            new ToolSpec(KNOWLEDGE_EDITOR, true, 90, "knowledge bracket", "legacy knowledge editor tab", "Progression, blueprint, dependency, and unlock records.", "parent,breakthrough cost,blueprint,unlocks,tier,mod export"),
            new ToolSpec(SKILL_EDITOR, false, 100, "skill bracket", "new skill development surface", "Skill tree, governing attribute, cooldown, rank, and knowledge prerequisite records.", "attribute,rank cap,cooldown,knowledge prerequisite,unlock,mod export")
    );
    private static final List<String> EDITOR_NAMES = SPECS.stream().map(ToolSpec::editorName).toList();

    private SimulationToolSuiteRegistry() { }

    static List<ToolSpec> specs() {
        return Collections.unmodifiableList(SPECS);
    }

    static List<String> editorNames() {
        return Collections.unmodifiableList(EDITOR_NAMES);
    }

    static boolean isKnownEditor(String editorName) {
        return EDITOR_NAMES.contains(editorName);
    }

    static String fallbackEditor() {
        return SECTOR_EDITOR;
    }

    static String panelHelpText(String editorName) {
        for (ToolSpec spec : SPECS) {
            if (spec.editorName().equals(editorName)) {
                String recovery = spec.recovered() ? "Recovered from " + spec.recoveredFrom() + ". " : "New interface scaffold after recovery pass. ";
                return recovery + spec.purpose() + " Priority " + spec.priority() + "; " + spec.bracket() + "; capabilities: " + spec.capabilities() + ".";
            }
        }
        return "Edit structured simulation data through the shared repository and undoable commands.";
    }

    static LinkedHashMap<String, Object> defaultPropertiesFor(String editorName) {
        return switch (editorName) {
            case SECTOR_EDITOR -> withToolMetadata(editorName, props("x", 0, "y", 0, "z", 0, "sectorWidthZones", 5, "sectorHeightZones", 5, "spawnZoneX", 2, "spawnZoneY", 2, "routeSpine", "main transit spine", "worldgenTuning", "standard", "generationWalk", "spawn-zone -> zone-grid -> roads -> rooms -> objects -> lights -> entities -> audit", "overlaySet", "zones,rooms,objects,interactions,lights,transitions,findings", "hazard", "None", "factionControl", "Unassigned", "radiation", 0, "piracy", 0));
            case ZONE_EDITOR -> withToolMetadata(editorName, props("zoneType", "industrial", "density", "medium", "sector", "Hive Core Sector", "route", "main transit spine", "auditOverlay", "zones", "lightProfile", "civil hab functional", "streetLightSpacing", 5, "expectedFindings", 0));
            case ROOM_EDITOR -> withToolMetadata(editorName, props("width", 8, "height", 6, "oxygenSeal", true, "securityTerminal", "None", "placementNodes", 0, "floorMaterial", "worn-plasteel", "layoutCells", ""));
            case FACTION_EDITOR -> withToolMetadata(editorName, props("lawful", 5, "mercantile", 5, "technocratic", 5, "resources", 100, "aggression", 3, "culture", "Order / Deterrence", "diplomacyProfile", "neutral", "schemePosture", "consolidation", "leaderRole", "cell leader", "journalPolicy", "office ledger", "homeRooms", "barracks office, private apartment", "standingEffects", "standard reputation"));
            case QUEST_EDITOR -> withToolMetadata(editorName, props("questFamily", "scheme quest", "lifecycleState", "planning", "sourceFaction", "Adeptus Arbites", "questGiver", "faction representative", "objectiveType", "recover evidence", "objectiveGuidance", "exact target highlight", "targetRef", "leadership journal", "evidenceRule", "zero-weight proof item", "reward", "money,reputation", "consequence", "heat,scheme disruption", "timerDays", 2, "missedWindowRule", "neutral if unaccepted", "localizationKey", "quest.scheme.recover_journal", "validationNotes", "requires objective, evidence, reward, failure rule"));
            case ITEM_EDITOR -> withToolMetadata(editorName, props("techTier", 1, "massKg", 1.0, "components", "steel, wire", "durability", 50, "decayRate", 0.01, "isUnlocked", false, "description", "Player-facing item description.", "infopediaEntry", "Item Handling"));
            case TILE_EDITOR -> withToolMetadata(editorName, props("glyph", ".", "family", "floor", "walkable", true, "opaque", false, "roadRole", "none", "roomRole", "interior floor", "boundaryRole", "none", "semanticAssetId", "TILE-A01", "lightProfile", "none", "hazardProfile", "none", "interactionHint", "inspect"));
            case OBJECT_EDITOR -> withToolMetadata(editorName, props("objectType", "workstation", "material", "plasteel", "footprint", "1x1", "interaction", "Inspect", "functionHook", "inspect", "properties", "durable,serviceable", "infopediaEntry", "Object Handling", "lightingHook", "none", "lightRadius", 0, "lightColor", "warm", "blocksMovement", true));
            case ENTITY_EDITOR -> withToolMetadata(editorName, props("archetype", "civilian", "faction", "Unassigned", "behaviorProfile", "idle", "spawnZone", "Hive Core Zone", "inventory", "none", "hostility", 0));
            case KNOWLEDGE_EDITOR -> withToolMetadata(editorName, props("parent", "root", "breakthroughCost", 1, "blueprint", "none", "unlocks", "new doctrine", "tier", 1, "isUnlocked", false));
            case INFOPEDIA_EDITOR -> withToolMetadata(editorName, props("category", "World", "body", "New entry body.", "tags", "new", "revision", 1));
            case SKILL_EDITOR -> withToolMetadata(editorName, props("attribute", "Technical", "rankCap", 5, "cooldownTurns", 0, "knowledgePrerequisite", "Underhive Basics", "unlocks", "none", "active", true));
            default -> props("name", "New Entry", "isUnlocked", false);
        };
    }

    static List<SimulationEditorRepository.EditableEntity> seedEntitiesFor(String editorName) {
        List<SimulationEditorRepository.EditableEntity> raw = switch (editorName) {
            case SECTOR_EDITOR -> entities(
                    entity("sector-hive-core", "Hive Core Sector", props("x", 0, "y", 0, "z", 12, "sectorWidthZones", 5, "sectorHeightZones", 5, "spawnZoneX", 2, "spawnZoneY", 2, "routeSpine", "main transit spine", "worldgenTuning", "dense civic-industrial", "generationWalk", "spawn-zone -> zone-grid -> roads -> rooms -> objects -> lights -> entities -> audit", "overlaySet", "zones,rooms,objects,interactions,lights,transitions,findings", "hazard", "Industrial Smog", "factionControl", "Adeptus Administratum", "radiation", 2, "piracy", 3)),
                    entity("sector-ash-wastes", "Ash Wastes Spur", props("x", -4, "y", 7, "z", 1, "sectorWidthZones", 4, "sectorHeightZones", 3, "spawnZoneX", 1, "spawnZoneY", 1, "routeSpine", "exposed service road", "worldgenTuning", "sparse hazardous", "generationWalk", "spawn-zone -> zone-grid -> roads -> rooms -> objects -> lights -> entities -> audit", "overlaySet", "zones,rooms,objects,interactions,lights,hazards,findings", "hazard", "Ash Storms", "factionControl", "Scavenger Clans", "radiation", 4, "piracy", 7)),
                    entity("sector-dock-ring", "Orbital Dock Ring", props("x", 2, "y", -3, "z", 35, "sectorWidthZones", 6, "sectorHeightZones", 4, "spawnZoneX", 3, "spawnZoneY", 1, "routeSpine", "cargo transit ring", "worldgenTuning", "dock-heavy", "generationWalk", "spawn-zone -> zone-grid -> roads -> rooms -> objects -> lights -> entities -> audit", "overlaySet", "zones,rooms,objects,interactions,lights,transitions,findings", "hazard", "Vacuum Breach", "factionControl", "Guild Factors", "radiation", 1, "piracy", 5))
            );
            case ZONE_EDITOR -> entities(
                    entity("zone-hive-core", "Hive Core Zone", props("zoneType", "industrial", "density", "high", "sector", "Hive Core Sector", "route", "main transit spine", "auditOverlay", "zones", "expectedFindings", 4)),
                    entity("zone-ash-service", "Ash Wastes Service Zone", props("zoneType", "wastes", "density", "low", "sector", "Ash Wastes Spur", "route", "exposed service road", "auditOverlay", "hazards", "expectedFindings", 6))
            );
            case ROOM_EDITOR -> entities(
                    entity("room-hab-cell", "Hab Cell Block", props("width", 4, "height", 4, "oxygenSeal", true, "securityTerminal", "None", "placementNodes", 3, "floorMaterial", "worn-plasteel")),
                    entity("room-generator", "Utility Generator Room", props("width", 8, "height", 6, "oxygenSeal", true, "securityTerminal", "Local Panel", "placementNodes", 9, "floorMaterial", "oil-stained iron")),
                    entity("room-chapel", "Cult Imperialis Chapel", props("width", 12, "height", 8, "oxygenSeal", false, "securityTerminal", "Reliquary Seal", "placementNodes", 11, "floorMaterial", "stone and brass"))
            );
            case FACTION_EDITOR -> entities(
                    entity("faction-arbites", "Adeptus Arbites", props("lawful", 9, "mercantile", 2, "technocratic", 4, "resources", 650, "aggression", 6, "culture", "Order / Deterrence", "diplomacyProfile", "lawful enforcement", "schemePosture", "containment patrol", "leaderRole", "precinct marshal", "journalPolicy", "sealed evidence ledger", "homeRooms", "barracks office, precinct archive", "standingEffects", "law reputation, heat reduction if sanctioned")),
                    entity("faction-guild", "Dock Guild", props("lawful", 4, "mercantile", 9, "technocratic", 6, "resources", 820, "aggression", 3, "culture", "Contracts / Leverage", "diplomacyProfile", "contract pressure", "schemePosture", "market expansion", "leaderRole", "factor-master", "journalPolicy", "trade ledger", "homeRooms", "counting office, dock apartment", "standingEffects", "trade discounts, debt pressure")),
                    entity("faction-scav", "Scavenger Clans", props("lawful", 2, "mercantile", 5, "technocratic", 3, "resources", 210, "aggression", 7, "culture", "Survival / Salvage", "diplomacyProfile", "opportunist", "schemePosture", "raid preparation", "leaderRole", "crew boss", "journalPolicy", "stolen notebook", "homeRooms", "stash room, camp office", "standingEffects", "black-market access, ambush risk"))
            );
            case QUEST_EDITOR -> entities(
                    entity("quest-recover-journal", "Recover Leadership Journal", props("questFamily", "scheme quest", "lifecycleState", "planning", "sourceFaction", "Adeptus Arbites", "questGiver", "precinct marshal", "objectiveType", "recover evidence", "objectiveGuidance", "exact target highlight", "targetRef", "sealed evidence ledger", "evidenceRule", "zero-weight proof item", "reward", "money,law reputation", "consequence", "reduced scheme heat,scavenger retaliation", "timerDays", 2, "missedWindowRule", "neutral if unaccepted", "localizationKey", "quest.scheme.recover_journal", "validationNotes", "objective target, evidence item, reward, and failure text required")),
                    entity("quest-sabotage-production", "Sabotage Production Surge", props("questFamily", "sabotage", "lifecycleState", "execution", "sourceFaction", "Dock Guild", "questGiver", "rival factor", "objectiveType", "disable machine", "objectiveGuidance", "approximate search area", "targetRef", "workshop machine", "evidenceRule", "optional copied plan", "reward", "money,trade leverage", "consequence", "production delay,guard search", "timerDays", 3, "missedWindowRule", "scheme completes if ignored", "localizationKey", "quest.scheme.sabotage_production", "validationNotes", "machine target and alternate resolution required")),
                    entity("quest-proof-of-death", "Confirm Raid Leader Death", props("questFamily", "kill/evidence", "lifecycleState", "active", "sourceFaction", "Scavenger Clans", "questGiver", "crew boss", "objectiveType", "recover proof-of-death", "objectiveGuidance", "target corpse highlight", "targetRef", "raid leader corpse", "evidenceRule", "proof item appears regardless of killer", "reward", "money,scavenger standing", "consequence", "target faction hostility", "timerDays", 2, "missedWindowRule", "aftermath recovery remains available", "localizationKey", "quest.evidence.confirm_death", "validationNotes", "proof item must be zero weight and recoverable"))
            );
            case ITEM_EDITOR -> entities(
                    entity("item-stubcarbine", "Stubcarbine", props("techTier", 2, "massKg", 3.4, "components", "steel, springs, firing pin", "durability", 85, "decayRate", 0.04, "description", "A compact ballistic weapon for patrols, escorts, and short industrial corridors.", "infopediaEntry", "Stubcarbine")),
                    entity("item-sealed-ration", "Sealed Ration", props("techTier", 1, "massKg", 0.45, "components", "nutrient brick, wax paper", "durability", 60, "decayRate", 0.01, "description", "A stamped ration pack intended to survive dust, damp, and indifferent quartermasters.", "infopediaEntry", "Sealed Ration")),
                    entity("item-cogitator-core", "Cogitator Core", props("techTier", 6, "massKg", 18.0, "components", "copper, crystal matrix, logic stack", "durability", 92, "decayRate", 0.02, "description", "A heavy logic assembly that can anchor terminals, security systems, and machine records.", "infopediaEntry", "Cogitator Core"))
            );
            case TILE_EDITOR -> entities(
                    entity("tile-floor", "Worn Plasteel Floor", props("glyph", ".", "family", "floor", "walkable", true, "opaque", false, "roadRole", "none", "roomRole", "interior floor", "boundaryRole", "none", "semanticAssetId", "TILE-A01", "lightProfile", "none", "hazardProfile", "none", "interactionHint", "inspect")),
                    entity("tile-wall", "Bulkhead Wall", props("glyph", "#", "family", "wall", "walkable", false, "opaque", true, "roadRole", "none", "roomRole", "room shell", "boundaryRole", "solid", "semanticAssetId", "TILE-W01", "lightProfile", "blocks", "hazardProfile", "none", "interactionHint", "inspect wall")),
                    entity("tile-road", "Road Lane", props("glyph", "=", "family", "road", "walkable", true, "opaque", false, "roadRole", "lane", "roomRole", "none", "boundaryRole", "none", "semanticAssetId", "TILE-R01", "lightProfile", "street", "hazardProfile", "vehicle traffic", "interactionHint", "inspect road"))
            );
            case OBJECT_EDITOR -> entities(
                    entity("object-terminal", "Security Terminal", props("objectType", "terminal", "material", "brass and glass", "footprint", "1x1", "interaction", "Hack", "functionHook", "security-terminal", "properties", "locked,powered,inspectable", "infopediaEntry", "Security Terminal", "lightingHook", "intrinsic glow", "lightRadius", 2, "lightColor", "green", "blocksMovement", true)),
                    entity("object-recycler", "Water Recycler", props("objectType", "utility", "material", "sealed steel", "footprint", "2x1", "interaction", "Repair", "functionHook", "water-service", "properties", "repairable,utility,pipe-sound", "infopediaEntry", "Water Recycler", "lightingHook", "zone fixture", "lightRadius", 1, "lightColor", "warm", "blocksMovement", true))
            );
            case ENTITY_EDITOR -> entities(
                    entity("entity-arbites-patrol", "Arbites Patrol", props("archetype", "guard", "faction", "Adeptus Arbites", "behaviorProfile", "patrol", "spawnZone", "Hive Core Zone", "inventory", "stubcarbine, baton", "hostility", 2)),
                    entity("entity-scav-trader", "Scavenger Trader", props("archetype", "trader", "faction", "Scavenger Clans", "behaviorProfile", "merchant", "spawnZone", "Ash Wastes Service Zone", "inventory", "scrap bundle, ration", "hostility", 0))
            );
            case KNOWLEDGE_EDITOR -> entities(
                    entity("knowledge-basic-underhive", "Underhive Basics", props("parent", "root", "breakthroughCost", 0, "blueprint", "none", "unlocks", "movement, scavenge, inspection", "tier", 0)),
                    entity("knowledge-machinery", "Applied Machinery", props("parent", "Underhive Basics", "breakthroughCost", 45, "blueprint", "micro-assembler", "unlocks", "machine inspection, repair queue", "tier", 2)),
                    entity("knowledge-void-safety", "Void Safety Protocols", props("parent", "Applied Machinery", "breakthroughCost", 90, "blueprint", "sealed hatch", "unlocks", "vacuum hazard mitigation", "tier", 4))
            );
            case INFOPEDIA_EDITOR -> entities(
                    entity("info-sector-taxonomy", "Sector Taxonomy", props("category", "World", "body", "Classification rules for hive sectors, spurs, transit layers, and void-facing infrastructure.", "tags", "sector,world,hive", "revision", 1)),
                    entity("info-faction-index", "Faction Index", props("category", "Factions", "body", "A cross-reference for lawful, mercantile, technocratic, insurgent, and devotional entities.", "tags", "faction,politics,economy", "revision", 1)),
                    entity("info-lore-crawl", "Historical Crawl", props("category", "Lore", "body", "A long-form historical text node for sector provenance and grim industrial memory.", "tags", "history,lore,provenance", "revision", 1))
            );
            case SKILL_EDITOR -> entities(
                    entity("skill-machine-sense", "Machine Sense", props("attribute", "Technical", "rankCap", 5, "cooldownTurns", 0, "knowledgePrerequisite", "Applied Machinery", "unlocks", "diagnose machines", "active", true)),
                    entity("skill-zone-reader", "Zone Reader", props("attribute", "Awareness", "rankCap", 3, "cooldownTurns", 8, "knowledgePrerequisite", "Underhive Basics", "unlocks", "surface zone hazards", "active", true))
            );
            default -> List.of();
        };
        return withSeedMetadata(editorName, raw);
    }

    static List<SimulationEditorRepository.EntityRef> defaultSelectedRefs() {
        return List.of(
                new SimulationEditorRepository.EntityRef(SECTOR_EDITOR, "sector-hive-core"),
                new SimulationEditorRepository.EntityRef(ZONE_EDITOR, "zone-hive-core"),
                new SimulationEditorRepository.EntityRef(ROOM_EDITOR, "room-generator"),
                new SimulationEditorRepository.EntityRef(FACTION_EDITOR, "faction-arbites"),
                new SimulationEditorRepository.EntityRef(QUEST_EDITOR, "quest-recover-journal"),
                new SimulationEditorRepository.EntityRef(ITEM_EDITOR, "item-cogitator-core"),
                new SimulationEditorRepository.EntityRef(TILE_EDITOR, "tile-floor"),
                new SimulationEditorRepository.EntityRef(OBJECT_EDITOR, "object-terminal"),
                new SimulationEditorRepository.EntityRef(ENTITY_EDITOR, "entity-arbites-patrol"),
                new SimulationEditorRepository.EntityRef(INFOPEDIA_EDITOR, "info-sector-taxonomy"),
                new SimulationEditorRepository.EntityRef(KNOWLEDGE_EDITOR, "knowledge-basic-underhive"),
                new SimulationEditorRepository.EntityRef(SKILL_EDITOR, "skill-machine-sense")
        );
    }

    static Map<String, List<String>> linkOptionsFor(String editorName, SimulationEditorRepository repository) {
        return switch (editorName) {
            case SECTOR_EDITOR -> LinkCatalog.sectorLinks(repository);
            case ZONE_EDITOR -> LinkCatalog.zoneLinks(repository);
            case FACTION_EDITOR -> LinkCatalog.factionLinks(repository);
            case QUEST_EDITOR -> LinkCatalog.questLinks(repository);
            case ITEM_EDITOR -> LinkCatalog.itemLinks(repository);
            case TILE_EDITOR -> LinkCatalog.tileLinks(repository);
            case OBJECT_EDITOR -> LinkCatalog.objectLinks(repository);
            case ENTITY_EDITOR -> LinkCatalog.entityLinks(repository);
            case KNOWLEDGE_EDITOR -> LinkCatalog.knowledgeLinks(repository);
            case INFOPEDIA_EDITOR -> LinkCatalog.infopediaLinks(repository);
            case SKILL_EDITOR -> LinkCatalog.skillLinks(repository);
            default -> Map.of();
        };
    }

    static String auditSummary() {
        long recovered = SPECS.stream().filter(ToolSpec::recovered).count();
        long fresh = SPECS.size() - recovered;
        return "authority=" + VERSION + " recovered-first=true editors=" + SPECS.size()
                + " recovered=" + recovered + " new-ui-scaffolds=" + fresh
                + " priority-fields=true bracket-capabilities=true external-mod-commit=true"
                + " primary-window-integration=true subsystems=registry+repository+swing";
    }

    private static List<SimulationEditorRepository.EditableEntity> entities(SimulationEditorRepository.EditableEntity... entities) {
        ArrayList<SimulationEditorRepository.EditableEntity> out = new ArrayList<>();
        Collections.addAll(out, entities);
        return Collections.unmodifiableList(out);
    }

    private static SimulationEditorRepository.EditableEntity entity(String id, String name, LinkedHashMap<String, Object> props) {
        return new SimulationEditorRepository.EditableEntity(id, name, props);
    }

    private static LinkedHashMap<String, Object> withToolMetadata(String editorName, LinkedHashMap<String, Object> props) {
        ToolSpec spec = specFor(editorName);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("priority", spec == null ? 999 : spec.priority());
        out.put("editingBracket", spec == null ? "general bracket" : spec.bracket());
        out.put("includedInformation", spec == null ? "general structured data" : spec.purpose());
        out.put("featureCapabilities", spec == null ? "basic table edits,mod export" : spec.capabilities());
        out.put("externalModCommit", true);
        out.put("sourceMode", "live project defaults; edits are isolated to mod scope");
        out.put("sourcePointer", spec == null ? "current project registry" : spec.recoveredFrom());
        out.putAll(props);
        return out;
    }

    private static List<SimulationEditorRepository.EditableEntity> withSeedMetadata(String editorName, List<SimulationEditorRepository.EditableEntity> raw) {
        ArrayList<SimulationEditorRepository.EditableEntity> out = new ArrayList<>();
        if (raw != null) {
            for (SimulationEditorRepository.EditableEntity entity : raw) {
                if (entity == null) continue;
                out.add(new SimulationEditorRepository.EditableEntity(entity.id(), entity.name(), withToolMetadata(editorName, entity.properties())));
            }
        }
        return Collections.unmodifiableList(out);
    }

    private static ToolSpec specFor(String editorName) {
        for (ToolSpec spec : SPECS) if (spec.editorName().equals(editorName)) return spec;
        return null;
    }

    private static LinkedHashMap<String, Object> props(Object... pairs) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) out.put(String.valueOf(pairs[i]), normalizeValue(pairs[i + 1]));
        return out;
    }

    private static Object normalizeValue(Object value) {
        if (value instanceof String s) return s.trim();
        return value;
    }

    record ToolSpec(String editorName, boolean recovered, int priority, String bracket, String recoveredFrom, String purpose, String capabilities) { }
}
