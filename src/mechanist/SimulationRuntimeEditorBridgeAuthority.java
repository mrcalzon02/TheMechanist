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
        return SimulationToolSuiteRegistry.ROOM_EDITOR.equals(editorName)
                || SimulationToolSuiteRegistry.FACTION_EDITOR.equals(editorName)
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
        int liveWorldRecords = 0;
        int catalogDefinitionRecords = 0;
        for (SimulationEditorRepository.EditableEntity record : records) {
            if (record != null && Boolean.FALSE.equals(
                    record.properties().get("liveWorldDependent"))) {
                catalogDefinitionRecords++;
            } else {
                liveWorldRecords++;
            }
        }
        return new RefreshResult(true, added, "Refreshed " + added + " "
                + editorName.toLowerCase() + " record" + (added == 1 ? "" : "s")
                + " at world turn " + game.worldTurn + " (" + liveWorldRecords
                + " live-world, " + catalogDefinitionRecords + " catalog/definition)."
                + " Changes remain mod-local until exported.");
    }

    static List<SimulationEditorRepository.EditableEntity> snapshot(GamePanel game, String editorName) {
        if (game == null || game.world == null || !supports(editorName)) return List.of();
        World world = game.world;
        ArrayList<SimulationEditorRepository.EditableEntity> out = new ArrayList<>();
        if (SimulationToolSuiteRegistry.ROOM_EDITOR.equals(editorName)) addRoomParitySnapshots(out, game);
        else if (SimulationToolSuiteRegistry.FACTION_EDITOR.equals(editorName)) addFactionSnapshots(out, game);
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

    private static void addRoomParitySnapshots(
            ArrayList<SimulationEditorRepository.EditableEntity> out, GamePanel game) {
        World world = game.world;
        int mappedRooms = 0;
        int nonAcquirableRooms = 0;
        int unmappedRooms = 0;
        int controllerDivergences = 0;
        int controllerFamilyDivergences = 0;
        int specialRooms = 0;
        int missingProfiles = 0;
        int declaredPurposeRooms = 0;
        int operatingPurposeRooms = 0;
        int blockedPurposeRooms = 0;
        int roomCount = world.rooms == null ? 0 : world.rooms.size();
        for (int roomId = 0; roomId < roomCount; roomId++) {
            java.awt.Rectangle bounds = world.roomRect(roomId);
            RoomProfile profile = roomId < world.roomProfiles.size()
                    ? world.roomProfiles.get(roomId) : null;
            Faction profileDeclared = profile == null || profile.faction == null
                    ? Faction.NONE : profile.faction;
            Faction profileFamily = FactionIdentityAuthority.strategicFamily(profileDeclared);
            Faction controller = world.roomFaction(roomId);
            if (controller == null) controller = Faction.NONE;
            Faction controllerFamily = FactionIdentityAuthority.strategicFamily(controller);
            boolean special = roomId < world.roomSpecials.size()
                    && Boolean.TRUE.equals(world.roomSpecials.get(roomId));
            RoomConstructionParityAuthority.RoomParityEntry parity =
                    RoomConstructionParityAuthority.liveEntry(profile, world.zoneType);
            String acquisition = safe(parity.playerAcquisitionStatus());
            boolean nonAcquirable = acquisition.toLowerCase().contains("non-acquirable");
            BuildRecipe mappedRecipe = nonAcquirable
                    ? null : RoomConstructionParityAuthority.liveMatchingRecipe(profile);
            String mappingStatus;
            if (nonAcquirable) {
                mappingStatus = "NON_ACQUIRABLE_EXCEPTION";
                nonAcquirableRooms++;
            } else if (mappedRecipe != null) {
                mappingStatus = "MAPPED";
                mappedRooms++;
            } else {
                mappingStatus = "UNMAPPED_GAP";
                unmappedRooms++;
            }
            boolean differs = profileDeclared != controller;
            boolean familyDiffers = profileFamily != controllerFamily;
            if (differs) controllerDivergences++;
            if (familyDiffers) controllerFamilyDivergences++;
            if (special) specialRooms++;
            if (profile == null) missingProfiles++;
            ExplicitRoomTypeRequirementAuthority.Assessment purpose =
                    ExplicitRoomTypeRequirementAuthority.assessRoom(world, roomId);
            if (purpose.declared()) {
                declaredPurposeRooms++;
                if (purpose.operating()) operatingPurposeRooms++;
                else blockedPurposeRooms++;
            }

            LinkedHashMap<String, Object> props = runtimeProperties(
                    "RoomConstructionParityInspection", game.worldTurn);
            props.put("liveWorldDependent", true);
            props.put("worldLocationKey", world.locationKey());
            props.put("sectorX", world.sectorX);
            props.put("sectorY", world.sectorY);
            props.put("zoneX", world.zoneX);
            props.put("zoneY", world.zoneY);
            props.put("floor", world.floor);
            props.put("sewerLayer", world.sewerLayer);
            props.put("roomId", roomId);
            props.put("roomName", safe(parity.roomName()));
            props.put("zoneType", world.zoneType == null ? "UNKNOWN" : world.zoneType.name());
            props.put("sourceZone", safe(parity.sourceZone()));
            props.put("x", bounds == null ? -1 : bounds.x);
            props.put("y", bounds == null ? -1 : bounds.y);
            props.put("width", bounds == null ? 0 : bounds.width);
            props.put("height", bounds == null ? 0 : bounds.height);
            props.put("roomProfilePresent", profile != null);
            props.put("roomBoundsPresent", bounds != null);
            props.put("specialRoom", special);
            props.put("profileDeclaredFaction", profileDeclared.name());
            props.put("profileDeclaredFactionLabel", profileDeclared.label);
            props.put("profileDeclaredStrategicFamily", profileFamily.name());
            props.put("profileDeclaredStrategicFamilyLabel", profileFamily.label);
            props.put("currentControllerFaction", controller.name());
            props.put("currentControllerLabel", controller.label);
            props.put("currentControllerStrategicFamily", controllerFamily.name());
            props.put("currentControllerStrategicFamilyLabel", controllerFamily.label);
            props.put("controllerDiffersFromProfileDeclaration", differs);
            props.put("controllerFamilyDiffersFromProfileFamily", familyDiffers);
            props.put("blueprintMappingStatus", mappingStatus);
            props.put("matchingBlueprint", mappedRecipe == null
                    ? "No exact blueprint" : mappedRecipe.name);
            props.put("blueprintMappingValid", mappedRecipe != null);
            props.put("blueprintAcquisitionStatus", acquisition);
            props.put("profileDeclaredFactionUseStatus", safe(parity.factionUseStatus()));
            props.put("exceptionNote", profile == null && safe(parity.exceptionNote()).isBlank()
                    ? "No room profile is available for this live room index."
                    : safe(parity.exceptionNote()));
            props.put("declaredPurposeId", purpose.definition() == null
                    ? "" : purpose.definition().id());
            props.put("declaredPurposeLabel", purpose.definition() == null
                    ? "No explicit purpose" : purpose.definition().label());
            props.put("declaredPurposeSource", purpose.declarationSource());
            props.put("roomPurposeStatus", purpose.status().name());
            props.put("roomPurposeOperating", purpose.operating());
            props.put("roomPurposePhysicallyQualified", purpose.physicallyQualified());
            props.put("roomPurposeBlockers", purpose.blockers().isEmpty()
                    ? "none" : String.join(" | ", purpose.blockers()));
            props.put("roomPurposeEvidence", purpose.declared()
                    ? purpose.requirementSummary() + " | witnesses: " + purpose.witnessSummary()
                    : "No explicit room-purpose definition is declared; prose and glyphs are not qualification evidence.");
            props.put("roomPurposeAssignedStaff", purpose.assignedStaff());
            props.put("roomPurposeRawCapacityUnits", purpose.capacityUnits());
            props.put("roomPurposeOperatingCapacity", purpose.operatingCapacity());
            props.put("roomPurposeCapacityUnitLabel", purpose.capacityUnitLabel());
            props.put("roomPurposeCapacitySummary", purpose.operatingCapacity() + " "
                    + purpose.capacityUnitLabel());
            String purposeId = purpose.definition() == null ? "" : purpose.definition().id();
            if (ExplicitRoomTypeRequirementAuthority.CRECHE_ID.equals(purposeId)) {
                // Retain the original crèche-specific key for existing editor
                // consumers while keeping it off unrelated purpose rows.
                props.put("roomPurposeChildCapacity", purpose.childCapacity());
                props.put("roomPurposeAssignedCareProviders", purpose.assignedCareProviders());
                props.put("roomPurposeChildBedUnits", purpose.childBedUnits());
            }
            if (ExplicitRoomTypeRequirementAuthority.BARRACKS_ID.equals(purposeId)) {
                props.put("roomPurposeAssignedDutyStaff", purpose.assignedDutyStaff());
                props.put("roomPurposeDutyBerthUnits", purpose.dutyBerthUnits());
                props.put("roomPurposeDutyCapacity", purpose.dutyCapacity());
                props.put("roomPurposeMusterCapacity", purpose.operatingCapacity());
                props.put("roomPurposeBarracksMusterEligible", purpose.operating());
            }
            out.add(entity(SimulationToolSuiteRegistry.ROOM_EDITOR,
                    "room-parity-" + world.locationKey() + "-" + roomId,
                    "Room " + roomId + " - " + safe(parity.roomName()), props));
        }

        LinkedHashMap<String, Object> audit = runtimeProperties(
                "RoomConstructionParityAudit", game.worldTurn);
        audit.put("liveWorldDependent", true);
        audit.put("worldLocationKey", world.locationKey());
        audit.put("roomCount", roomCount);
        audit.put("mappedRoomCount", mappedRooms);
        audit.put("nonAcquirableExceptionCount", nonAcquirableRooms);
        audit.put("unmappedGapCount", unmappedRooms);
        audit.put("controllerDivergenceCount", controllerDivergences);
        audit.put("controllerFamilyDivergenceCount", controllerFamilyDivergences);
        audit.put("specialRoomCount", specialRooms);
        audit.put("missingProfileCount", missingProfiles);
        audit.put("roomPurposeDefinitionCount",
                ExplicitRoomTypeRequirementAuthority.definitions().size());
        audit.put("roomPurposeDeclaredCount", declaredPurposeRooms);
        audit.put("roomPurposeOperatingCount", operatingPurposeRooms);
        audit.put("roomPurposeBlockedCount", blockedPurposeRooms);
        audit.put("roomPurposeDefinitionAudit",
                ExplicitRoomTypeRequirementAuthority.definitionAuditLine());
        audit.put("boundary",
                "Read-only mod-local room snapshot. Explicit-purpose fields assess live room geometry, reachable semantic installed-fixture capabilities, assigned living personnel, control, and recorded hazards. Capacity is type-specific: child-care capacity for creches and duty/muster capacity for security barracks. Fixture presence does not prove current food, water, arms, or ammunition contents; assignment does not prove an active work shift. Blueprint parity still does not prove plan ownership, vendor stock, or price, and editor changes do not apply to the live world.");
        out.add(entity(SimulationToolSuiteRegistry.ROOM_EDITOR,
                "room-parity-audit-" + world.locationKey(),
                "Room Construction Parity Audit", audit));
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
        addConstructionParitySnapshots(out, game.worldTurn);
    }

    private static void addConstructionParitySnapshots(
            ArrayList<SimulationEditorRepository.EditableEntity> out, long worldTurn) {
        for (ConstructionParityInspectionAuthority.RecipeInspection inspection
                : ConstructionParityInspectionAuthority.inspectAll()) {
            LinkedHashMap<String, Object> props = runtimeProperties(
                    "ConstructionParityInspection", worldTurn);
            props.put("sourceMode",
                    "refreshable construction catalog inspection; not live ownership, vendor, site, or price state");
            props.put("liveWorldDependent", false);
            props.put("recipeName", inspection.recipeName());
            props.put("category", inspection.category());
            props.put("playerCapability", inspection.playerCapability().name());
            props.put("factionCapability", inspection.factionCapability().name());
            props.put("blueprintName", inspection.blueprintName());
            props.put("blueprintMappingValid", inspection.blueprintMappingValid());
            props.put("issuingFaction", inspection.issuingFaction());
            props.put("vendorCategory", inspection.vendorCategory());
            props.put("acquisitionPath", inspection.acquisitionPath());
            props.put("accessGate", inspection.accessGate());
            props.put("legalClass", inspection.legalClass());
            props.put("materialSummary", inspection.materialSummary());
            props.put("workforceSummary", inspection.workforceSummary());
            props.put("exceptionClass", inspection.exceptionClass());
            props.put("exceptionReason", inspection.exceptionReason());
            out.add(entity(SimulationToolSuiteRegistry.ECONOMY_EDITOR,
                    "construction-parity-" + inspection.recipeName(),
                    inspection.recipeName() + " Construction Parity", props));
        }
        LinkedHashMap<String, Object> summary = runtimeProperties(
                "ConstructionParityAudit", worldTurn);
        summary.put("sourceMode",
                "refreshable construction catalog inspection; not live ownership, vendor, site, or price state");
        summary.put("liveWorldDependent", false);
        summary.put("recipeCount", ConstructionParityInspectionAuthority.inspectAll().size());
        summary.put("auditLines", String.join(" | ",
                ConstructionParityInspectionAuthority.auditLines()));
        summary.put("interpretation",
                "Player-only, faction-only, conditional, unsupported, and invalid mappings remain visible until their live owner is implemented.");
        out.add(entity(SimulationToolSuiteRegistry.ECONOMY_EDITOR,
                "construction-parity-audit", "Construction Parity Audit", summary));
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
