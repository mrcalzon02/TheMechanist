package mechanist;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds editable mod-local snapshots from the active world's persisted simulation ledgers. */
final class SimulationRuntimeEditorBridgeAuthority {
    record RefreshResult(boolean supported, int records, String message) { }

    private SimulationRuntimeEditorBridgeAuthority() { }

    static boolean supports(String editorName) {
        return SimulationToolSuiteRegistry.FACTION_EDITOR.equals(editorName)
                || SimulationToolSuiteRegistry.POPULATION_EDITOR.equals(editorName)
                || SimulationToolSuiteRegistry.ECONOMY_EDITOR.equals(editorName)
                || SimulationToolSuiteRegistry.REINFORCEMENT_EDITOR.equals(editorName)
                || SimulationToolSuiteRegistry.DEFERRED_NETWORK_EDITOR.equals(editorName)
                || SimulationToolSuiteRegistry.WORLD_EVENT_EDITOR.equals(editorName);
    }

    static RefreshResult refresh(GamePanel game, SimulationEditorRepository repository, String editorName) {
        if (repository == null || !supports(editorName)) {
            return new RefreshResult(false, 0, "This editor has no live-world ledger source.");
        }
        if (game == null || game.world == null) {
            repository.replaceRuntimeEntities(editorName, List.of());
            return new RefreshResult(true, 0, "No world is loaded; editable templates remain available.");
        }
        List<SimulationEditorRepository.EditableEntity> records = snapshot(game, editorName);
        int added = repository.replaceRuntimeEntities(editorName, records);
        return new RefreshResult(true, added, "Loaded " + added + " live " + editorName.toLowerCase()
                + " record" + (added == 1 ? "" : "s") + " at world turn " + game.worldTurn
                + ". Changes remain mod-local until exported.");
    }

    static List<SimulationEditorRepository.EditableEntity> snapshot(GamePanel game, String editorName) {
        if (game == null || game.world == null || !supports(editorName)) return List.of();
        World world = game.world;
        ArrayList<SimulationEditorRepository.EditableEntity> out = new ArrayList<>();
        if (SimulationToolSuiteRegistry.FACTION_EDITOR.equals(editorName)) addFactionSnapshots(out, game);
        else if (SimulationToolSuiteRegistry.POPULATION_EDITOR.equals(editorName)) addPopulationSnapshots(out, game);
        else if (SimulationToolSuiteRegistry.ECONOMY_EDITOR.equals(editorName)) addEconomySnapshots(out, game);
        else if (SimulationToolSuiteRegistry.REINFORCEMENT_EDITOR.equals(editorName)) addReinforcementSnapshots(out, game);
        else if (SimulationToolSuiteRegistry.DEFERRED_NETWORK_EDITOR.equals(editorName)) addRecords(out, editorName, world.deferredFactionLedgers, game.worldTurn);
        else if (SimulationToolSuiteRegistry.WORLD_EVENT_EDITOR.equals(editorName)) {
            addRecords(out, editorName, world.topDownWorldEvents, game.worldTurn);
            LinkedHashMap<String, Object> scheduler = runtimeProperties("WorldEventScheduler", game.worldTurn);
            scheduler.put("nextEligibilityWorldTurn", world.nextTopDownWorldEventCheckTurn);
            scheduler.put("generationCount", world.topDownWorldEventGenerationCount);
            scheduler.put("activeEvents", TopDownWorldEventAuthority.activeEvents(world).size());
            out.add(entity(editorName, "world-event-scheduler", "World Event Scheduler", scheduler));
        }
        return List.copyOf(out);
    }

    private static void addFactionSnapshots(ArrayList<SimulationEditorRepository.EditableEntity> out, GamePanel game) {
        World world = game.world;
        for (Faction faction : Faction.visibleFactions()) {
            Faction family = FactionIdentityAuthority.strategicFamily(faction);
            int living = 0;
            for (NpcEntity npc : world.npcs) if (npc != null && npc.hp > 0 && FactionIdentityAuthority.sameFamily(npc.faction, faction)) living++;
            int rooms = 0;
            for (Faction owner : world.roomFactions) if (FactionIdentityAuthority.sameFamily(owner, faction)) rooms++;
            int capacity = PersonnelPopulationApi.replacementCapacityForFaction(world, faction);
            FactionReinforcementAuthority.Status reinforcement = FactionReinforcementAuthority.status(world, faction, game.turn);
            LinkedHashMap<String, Object> props = runtimeProperties("FactionRuntime", game.worldTurn);
            props.put("faction", faction.name());
            props.put("displayName", faction.label);
            props.put("strategicFamily", family.name());
            props.put("standing", game.factionStanding.getOrDefault(faction, 0));
            props.put("marketPressure", game.factionMarketPressure.getOrDefault(family, 0));
            props.put("happinessBoost", world.factionHappinessBoost.getOrDefault(family, 0));
            props.put("controlledRooms", rooms);
            props.put("livingPersonnel", living);
            props.put("populationCapacity", capacity);
            props.put("reinforcementRequested", reinforcement.requested());
            props.put("reinforcementReady", reinforcement.ready());
            props.put("reinforcementStatus", reinforcement.line());
            out.add(entity(SimulationToolSuiteRegistry.FACTION_EDITOR, "faction-" + faction.name(), faction.label, props));
        }
    }

    private static void addPopulationSnapshots(ArrayList<SimulationEditorRepository.EditableEntity> out, GamePanel game) {
        addRecords(out, SimulationToolSuiteRegistry.POPULATION_EDITOR, game.world.roomPopulationLedgers, game.worldTurn);
        addRecords(out, SimulationToolSuiteRegistry.POPULATION_EDITOR, game.world.crecheCohorts, game.worldTurn);
        for (NpcEntity npc : game.world.npcs) {
            if (npc == null || npc.isAnimalActor()) continue;
            LinkedHashMap<String, Object> props = runtimeProperties("NpcPopulation", game.worldTurn);
            props.put("npcId", safe(npc.id));
            props.put("name", safe(npc.name));
            props.put("faction", npc.faction == null ? Faction.NONE.name() : npc.faction.name());
            props.put("role", safe(npc.role));
            props.put("ageYears", npc.ageYears);
            props.put("ageBand", safe(npc.ageBand));
            props.put("happiness", npc.happiness);
            props.put("happinessReason", safe(npc.happinessReason));
            props.put("lastPaidWorldTurn", npc.lastPaidWorldTurn);
            props.put("pregnancyDueWorldTurn", npc.pregnancyDueWorldTurn);
            if (npc.provenance != null) {
                props.put("originMode", safe(npc.provenance.originMode));
                props.put("originZone", safe(npc.provenance.originZone));
                props.put("originRoom", safe(npc.provenance.originRoom));
                props.put("originSiteId", safe(npc.provenance.originSiteId));
                props.put("arrivalRoute", safe(npc.provenance.arrivalRoute));
                props.put("populationPool", safe(npc.provenance.populationPool));
            }
            out.add(entity(SimulationToolSuiteRegistry.POPULATION_EDITOR, "npc-" + safe(npc.id), safe(npc.name), props));
        }
    }

    private static void addEconomySnapshots(ArrayList<SimulationEditorRepository.EditableEntity> out, GamePanel game) {
        World world = game.world;
        addRecords(out, SimulationToolSuiteRegistry.ECONOMY_EDITOR, world.essentialSupplyReserves, game.worldTurn);
        addRecords(out, SimulationToolSuiteRegistry.ECONOMY_EDITOR, world.verticalTradeReserves, game.worldTurn);
        addRecords(out, SimulationToolSuiteRegistry.ECONOMY_EDITOR, world.securitySupplyReserves, game.worldTurn);
        addRecords(out, SimulationToolSuiteRegistry.ECONOMY_EDITOR, world.medicalSupplyReserves, game.worldTurn);
        addRecords(out, SimulationToolSuiteRegistry.ECONOMY_EDITOR, world.nobleLuxuryReserves, game.worldTurn);
        addRecords(out, SimulationToolSuiteRegistry.ECONOMY_EDITOR, world.draughtCustodyRecords, game.worldTurn);
        addRecords(out, SimulationToolSuiteRegistry.ECONOMY_EDITOR, world.animalAgricultureSupplyReserves, game.worldTurn);
        addRecords(out, SimulationToolSuiteRegistry.ECONOMY_EDITOR, world.rawMaterialSupplyReserves, game.worldTurn);
        addRecords(out, SimulationToolSuiteRegistry.ECONOMY_EDITOR, world.shipmentRecords, game.worldTurn);
        addRecords(out, SimulationToolSuiteRegistry.ECONOMY_EDITOR, game.factionContracts, game.worldTurn);
        for (Map.Entry<Faction, Integer> pressure : game.factionMarketPressure.entrySet()) {
            LinkedHashMap<String, Object> props = runtimeProperties("FactionMarketPressure", game.worldTurn);
            props.put("faction", pressure.getKey().name());
            props.put("marketPressure", pressure.getValue());
            props.put("contractHook", "market pressure relief");
            out.add(entity(SimulationToolSuiteRegistry.ECONOMY_EDITOR, "market-pressure-" + pressure.getKey().name(),
                    pressure.getKey().label + " Market Pressure", props));
        }
    }

    private static void addReinforcementSnapshots(ArrayList<SimulationEditorRepository.EditableEntity> out, GamePanel game) {
        addRecords(out, SimulationToolSuiteRegistry.REINFORCEMENT_EDITOR, game.world.replacementQueue, game.worldTurn);
        for (FactionReinforcementAuthority.SourceMethod method : FactionReinforcementAuthority.SourceMethod.values()) {
            LinkedHashMap<String, Object> props = runtimeProperties("ReinforcementSourcePolicy", game.worldTurn);
            props.put("sourceMode", method.id);
            props.put("label", method.label);
            props.put("minimumDelayTurns", method.minimumDelay);
            props.put("delaySpreadTurns", method.delaySpread);
            props.put("scriptCost", method.scriptCost);
            props.put("arrivalWindowTurns", method.arrivalWindow);
            props.put("prerequisite", method.prerequisite);
            out.add(entity(SimulationToolSuiteRegistry.REINFORCEMENT_EDITOR, "source-policy-" + method.id, method.label, props));
        }
        for (MapObjectState object : game.world.mapObjects) {
            if (!FactionImportNodeGenerationAuthority.isImportNode(object)) continue;
            out.add(snapshotEntity(SimulationToolSuiteRegistry.REINFORCEMENT_EDITOR, object, game.worldTurn));
        }
    }

    private static void addRecords(ArrayList<SimulationEditorRepository.EditableEntity> out, String editorName,
                                   Collection<?> records, long worldTurn) {
        if (records == null) return;
        for (Object record : records) if (record != null) out.add(snapshotEntity(editorName, record, worldTurn));
    }

    private static SimulationEditorRepository.EditableEntity snapshotEntity(String editorName, Object source, long worldTurn) {
        LinkedHashMap<String, Object> props = runtimeProperties(source.getClass().getSimpleName(), worldTurn);
        for (Field field : source.getClass().getDeclaredFields()) {
            if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) continue;
            try {
                field.setAccessible(true);
                Object value = editorValue(field.get(source));
                if (value != null) props.put(field.getName(), value);
            } catch (ReflectiveOperationException | RuntimeException ignored) { }
        }
        String rawId = first(props, "id", "deadNpcId", "npcId", "sourceMode", "title", "itemName");
        String name = first(props, "title", "name", "roomName", "itemName", "cargoItem", "sourceLabel", "label", "id");
        if (name.isBlank()) name = source.getClass().getSimpleName();
        if (rawId.isBlank()) rawId = name;
        return entity(editorName, source.getClass().getSimpleName() + "-" + rawId, name, props);
    }

    private static SimulationEditorRepository.EditableEntity entity(String editorName, String rawId, String name,
                                                                      LinkedHashMap<String, Object> props) {
        String id = "runtime-" + SimulationEditorRepository.slug(editorName + "-" + rawId);
        return new SimulationEditorRepository.EditableEntity(id, name == null || name.isBlank() ? id : name, props);
    }

    private static LinkedHashMap<String, Object> runtimeProperties(String recordClass, long worldTurn) {
        LinkedHashMap<String, Object> props = new LinkedHashMap<>();
        props.put("runtimeSnapshot", true);
        props.put("recordClass", recordClass);
        props.put("snapshotWorldTurn", worldTurn);
        props.put("sourceMode", "live world snapshot; edits remain mod-local");
        return props;
    }

    private static Object editorValue(Object value) {
        if (value == null) return "";
        if (value instanceof String || value instanceof Number || value instanceof Boolean) return value;
        if (value instanceof Character character) return Character.toString(character);
        if (value instanceof Enum<?> enumeration) return enumeration.name();
        if (value instanceof Collection<?> collection) {
            ArrayList<String> values = new ArrayList<>();
            for (Object item : collection) if (item != null) values.add(String.valueOf(item));
            return String.join(" | ", values);
        }
        return null;
    }

    private static String first(Map<String, Object> values, String... keys) {
        for (String key : keys) {
            Object value = values.get(key);
            if (value != null && !String.valueOf(value).isBlank()) return String.valueOf(value);
        }
        return "";
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
