package mechanist;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Focused Phase 18.1 smoke for live Room Editor construction-parity inspection. */
final class Milestone05RoomConstructionParityEditorSmoke {
    private static final String ROOM_RECORD = "RoomConstructionParityInspection";
    private static final String AUDIT_RECORD = "RoomConstructionParityAudit";

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        try {
            game.world = compactParityWorld(18051L);
            game.turn = 851;
            game.worldTurn = 851L;

            verifyInitialSnapshot(game);
            verifyRefreshIsolationAndLiveChanges(game);
            verifyInGameRoomEditor(game);
        } finally {
            game.shutdownRuntime();
        }
        System.out.println("Milestone 05 room construction parity editor smoke passed.");
    }

    private static void verifyInitialSnapshot(GamePanel game) {
        require(SimulationRuntimeEditorBridgeAuthority.supports(
                        SimulationToolSuiteRegistry.ROOM_EDITOR),
                "Room Editor should be backed by live-world room parity snapshots");

        List<SimulationEditorRepository.EditableEntity> snapshot =
                SimulationRuntimeEditorBridgeAuthority.snapshot(
                        game, SimulationToolSuiteRegistry.ROOM_EDITOR);
        List<SimulationEditorRepository.EditableEntity> rooms = roomRecords(snapshot);
        require(rooms.size() == game.world.rooms.size(),
                "Room Editor should expose exactly one parity row per live room: rows="
                        + rooms.size() + " rooms=" + game.world.rooms.size());
        require(countByClass(snapshot, AUDIT_RECORD) == 1,
                "Room Editor should expose one live room parity audit row");
        require(snapshot.size() == game.world.rooms.size() + 1,
                "Room Editor live snapshot should contain only room rows plus its audit");
        require(uniqueIds(snapshot).size() == snapshot.size(),
                "Room Editor live snapshot ids should be unique");

        verifyWarehouseRow(game, requireRoom(rooms, 0), Faction.CIVIC_WARDENS);
        verifyPlazaRow(game, requireRoom(rooms, 1));
        verifyUnmappedGapRow(game, requireRoom(rooms, 2));
        verifyAudit(requireClass(snapshot, AUDIT_RECORD), 3, 1, 1, 1, 1, 1);
    }

    private static void verifyRefreshIsolationAndLiveChanges(GamePanel game) {
        SimulationEditorRepository repository = new SimulationEditorRepository();
        SimulationRuntimeEditorBridgeAuthority.RefreshResult first =
                SimulationRuntimeEditorBridgeAuthority.refresh(
                        game, repository, SimulationToolSuiteRegistry.ROOM_EDITOR);
        require(first.supported(), "Room Editor live refresh should be supported");
        require(first.records() == game.world.rooms.size() + 1,
                "Room Editor refresh count should equal live rooms plus its audit");
        requireContains(first.message(), "Refreshed " + first.records()
                        + " room editor records", "Room Editor refresh count readback");
        requireContains(first.message(), first.records()
                        + " live-world, 0 catalog/definition",
                "Room Editor live/static refresh boundary");

        List<SimulationEditorRepository.EditableEntity> firstRuntime = runtimeRecords(repository);
        require(firstRuntime.size() == first.records(),
                "repository should contain exactly the refreshed Room Editor runtime rows");
        Set<String> initialIds = uniqueIds(firstRuntime);
        require(initialIds.size() == firstRuntime.size(),
                "first Room Editor refresh should have unique runtime ids");

        SimulationEditorRepository.EditableEntity warehouse = requireRoom(
                roomRecords(firstRuntime), 0);
        String warehouseId = warehouse.id();
        require(expectedRoomId(game.world, 0).equals(warehouseId),
                "warehouse row should use the stable world-location and room-index id: "
                        + warehouseId);
        RoomProfile profile = game.world.roomProfile(0);
        String profileNameBefore = profile.name;
        Faction profileFactionBefore = profile.faction;
        Faction controllerBefore = game.world.roomFaction(0);
        String mappedBlueprintBefore = RoomConstructionParityAuthority
                .liveMatchingRecipe(profile).name;

        SimulationEditorRepository.EntityRef warehouseRef =
                new SimulationEditorRepository.EntityRef(
                        SimulationToolSuiteRegistry.ROOM_EDITOR, warehouseId);
        repository.setProperty(warehouseRef, "profileDeclaredFaction", Faction.NOBLE.name());
        repository.setProperty(warehouseRef, "currentControllerFaction", Faction.NOBLE.name());
        repository.setProperty(warehouseRef, "matchingBlueprint", BuildRecipe.microForge().name);
        repository.setProperty(warehouseRef, "blueprintMappingStatus", "UNMAPPED_GAP");

        require(profileNameBefore.equals(profile.name)
                        && profileFactionBefore == profile.faction,
                "editing a Room Editor snapshot must not mutate its RoomProfile source");
        require(controllerBefore == game.world.roomFaction(0),
                "editing a Room Editor snapshot must not mutate live room control");
        require(mappedBlueprintBefore.equals(
                        RoomConstructionParityAuthority.liveMatchingRecipe(profile).name),
                "editing a Room Editor snapshot must not mutate room-to-blueprint mapping");
        require(BuildRecipe.storage().name.equals(mappedBlueprintBefore),
                "warehouse fixture should still resolve through the BuildRecipe catalog");

        SimulationRuntimeEditorBridgeAuthority.RefreshResult reset =
                SimulationRuntimeEditorBridgeAuthority.refresh(
                        game, repository, SimulationToolSuiteRegistry.ROOM_EDITOR);
        List<SimulationEditorRepository.EditableEntity> resetRuntime = runtimeRecords(repository);
        require(reset.records() == first.records()
                        && resetRuntime.size() == firstRuntime.size(),
                "repeat refresh should replace Room Editor rows without duplicating them");
        require(uniqueIds(resetRuntime).equals(initialIds),
                "repeat refresh should preserve the stable live room and audit ids");
        SimulationEditorRepository.EditableEntity resetWarehouse = requireRoom(
                roomRecords(resetRuntime), 0);
        verifyWarehouseRow(game, resetWarehouse, Faction.CIVIC_WARDENS);
        require(warehouseId.equals(resetWarehouse.id()),
                "repeat refresh should retain the warehouse runtime id");

        game.world.roomFactions.set(0, Faction.HIVER);
        SimulationRuntimeEditorBridgeAuthority.refresh(
                game, repository, SimulationToolSuiteRegistry.ROOM_EDITOR);
        List<SimulationEditorRepository.EditableEntity> controlledRuntime =
                runtimeRecords(repository);
        SimulationEditorRepository.EditableEntity controlledWarehouse = requireRoom(
                roomRecords(controlledRuntime), 0);
        verifyWarehouseRow(game, controlledWarehouse, Faction.HIVER);
        require(Faction.MECHANIST_COLLEGIA.name().equals(text(
                        controlledWarehouse.properties(), "profileDeclaredFaction")),
                "live controller changes should not rewrite the room profile declaration");
        require(warehouseId.equals(controlledWarehouse.id()),
                "live control changes should not change the room runtime id");

        Set<String> idsBeforeAppend = uniqueIds(controlledRuntime);
        addRoom(game.world, new Rectangle(4, 9, 5, 4), forgeAnnexProfile(),
                Faction.MECHANIST_COLLEGIA, false);
        SimulationRuntimeEditorBridgeAuthority.RefreshResult appended =
                SimulationRuntimeEditorBridgeAuthority.refresh(
                        game, repository, SimulationToolSuiteRegistry.ROOM_EDITOR);
        List<SimulationEditorRepository.EditableEntity> appendedRuntime =
                runtimeRecords(repository);
        List<SimulationEditorRepository.EditableEntity> appendedRooms =
                roomRecords(appendedRuntime);
        require(appended.records() == first.records() + 1
                        && appendedRooms.size() == 4
                        && countByClass(appendedRuntime, AUDIT_RECORD) == 1,
                "appending one live room should add exactly one Room Editor parity row");
        Set<String> appendedIds = uniqueIds(appendedRuntime);
        require(appendedIds.size() == appendedRuntime.size()
                        && appendedIds.containsAll(idsBeforeAppend),
                "appending a room should retain prior ids and add one unique id");

        SimulationEditorRepository.EditableEntity forge = requireRoom(appendedRooms, 3);
        verifyForgeAnnexRow(game, forge);
        verifyAudit(requireClass(appendedRuntime, AUDIT_RECORD), 4, 2, 1, 1, 2, 1);
    }

    private static void verifyInGameRoomEditor(GamePanel game) {
        game.openInGameEditor(SimulationToolSuiteRegistry.ROOM_EDITOR);
        require(game.screen == GamePanel.Screen.EDITOR,
                "live room parity rows should be reachable through the in-game Room Editor");
        require(game.inGameEditorStatus.contains("Refreshed ")
                        && game.inGameEditorStatus.contains("room editor records")
                        && game.inGameEditorStatus.contains("0 catalog/definition"),
                "opening the Room Editor should refresh the active world's room rows: "
                        + game.inGameEditorStatus);

        game.setSize(1280, 820);
        BufferedImage image = new BufferedImage(1280, 820, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        game.paintComponent(graphics);
        graphics.dispose();
        require(game.buttons.stream().anyMatch(button -> button != null
                        && "Refresh Live World".equals(button.label)),
                "in-game Room Editor should expose Refresh Live World");
    }

    private static void verifyWarehouseRow(
            GamePanel game, SimulationEditorRepository.EditableEntity entity,
            Faction expectedController) {
        Map<String, Object> props = entity.properties();
        verifyCommonRoomFields(game, entity, 0, "Component Warehouse",
                new Rectangle(1, 1, 5, 5));
        require(Faction.MECHANIST_COLLEGIA.name().equals(text(
                        props, "profileDeclaredFaction")),
                "warehouse should preserve its exact profile-declared faction");
        require(Faction.MECHANIST_COLLEGIA.label.equals(text(
                        props, "profileDeclaredFactionLabel")),
                "warehouse should expose its profile-declared faction label");
        require(Faction.MECHANIST_COLLEGIA.name().equals(text(
                        props, "profileDeclaredStrategicFamily")),
                "warehouse should expose its profile faction's strategic family separately");
        require(expectedController.name().equals(text(props, "currentControllerFaction"))
                        && expectedController.label.equals(text(props, "currentControllerLabel")),
                "warehouse should expose its live current controller");
        require(Boolean.TRUE.equals(props.get("controllerDiffersFromProfileDeclaration")),
                "warehouse profile declaration and current controller should remain visibly distinct");
        require(Boolean.TRUE.equals(props.get("controllerFamilyDiffersFromProfileFamily")),
                "warehouse controller family should differ from its declared Mechanist family");
        require("MAPPED".equals(text(props, "blueprintMappingStatus")),
                "warehouse should be an explicit mapped room");
        require(BuildRecipe.storage().name.equals(text(props, "matchingBlueprint"))
                        && Boolean.TRUE.equals(props.get("blueprintMappingValid")),
                "warehouse should map to the live Storage Crate recipe");
        requireContains(text(props, "blueprintAcquisitionStatus"),
                "available by default", "warehouse public-plan availability");
        requireContains(text(props, "profileDeclaredFactionUseStatus"),
                Faction.MECHANIST_COLLEGIA.label, "warehouse profile-faction use");
        require(text(props, "exceptionNote").isBlank(),
                "mapped warehouse should not carry an exception note");
    }

    private static void verifyPlazaRow(
            GamePanel game, SimulationEditorRepository.EditableEntity entity) {
        Map<String, Object> props = entity.properties();
        verifyCommonRoomFields(game, entity, 1, "Civic Transit Plaza",
                new Rectangle(8, 1, 5, 5));
        require("NON_ACQUIRABLE_EXCEPTION".equals(text(
                        props, "blueprintMappingStatus")),
                "transition plaza should remain an explicit non-acquirable exception");
        require(!Boolean.TRUE.equals(props.get("blueprintMappingValid")),
                "transition plaza should not report a valid ordinary blueprint mapping");
        requireContains(text(props, "blueprintAcquisitionStatus"),
                "non-acquirable exception", "transition plaza acquisition boundary");
        requireContains(text(props, "exceptionNote"),
                "documented exception", "transition plaza exception note");
        require(Faction.CIVIC_WARDENS.name().equals(text(
                        props, "profileDeclaredFaction"))
                        && Faction.CIVIC_WARDENS.name().equals(text(
                        props, "currentControllerFaction"))
                        && Boolean.FALSE.equals(props.get(
                        "controllerDiffersFromProfileDeclaration"))
                        && Boolean.FALSE.equals(props.get(
                        "controllerFamilyDiffersFromProfileFamily")),
                "transition plaza should expose matching profile declaration and control");
    }

    private static void verifyUnmappedGapRow(
            GamePanel game, SimulationEditorRepository.EditableEntity entity) {
        Map<String, Object> props = entity.properties();
        verifyCommonRoomFields(game, entity, 2, "INN Editorial Bullpen",
                new Rectangle(15, 1, 5, 5));
        require("UNMAPPED_GAP".equals(text(props, "blueprintMappingStatus")),
                "editorial bullpen should remain an explicit unmapped gap");
        require(!Boolean.TRUE.equals(props.get("blueprintMappingValid")),
                "unmapped editorial bullpen must not report a valid blueprint mapping");
        requireContains(text(props, "blueprintAcquisitionStatus"),
                "restricted player path", "unmapped faction-room acquisition guidance");
        requireContains(text(props, "exceptionNote"),
                "documented gap", "unmapped room exception note");
        require(Faction.INN.name().equals(text(props, "profileDeclaredFaction"))
                        && Faction.INN.name().equals(text(props, "currentControllerFaction")),
                "editorial bullpen should expose its INN declaration and controller");
    }

    private static void verifyForgeAnnexRow(
            GamePanel game, SimulationEditorRepository.EditableEntity entity) {
        Map<String, Object> props = entity.properties();
        verifyCommonRoomFields(game, entity, 3, "Basic Forge Annex",
                new Rectangle(4, 9, 5, 4));
        require(expectedRoomId(game.world, 3).equals(entity.id()),
                "appended forge annex should use its stable live room id");
        require("MAPPED".equals(text(props, "blueprintMappingStatus"))
                        && BuildRecipe.microForge().name.equals(text(
                        props, "matchingBlueprint"))
                        && Boolean.TRUE.equals(props.get("blueprintMappingValid")),
                "Basic Forge Annex should map to the EMM Micro Forge plan");
        require(Faction.MECHANICUS_CLOISTER_RED.name().equals(text(
                        props, "profileDeclaredFaction"))
                        && Faction.MECHANIST_COLLEGIA.name().equals(text(
                        props, "currentControllerFaction"))
                        && Boolean.TRUE.equals(props.get(
                        "controllerDiffersFromProfileDeclaration"))
                        && Faction.MECHANIST_COLLEGIA.name().equals(text(
                        props, "profileDeclaredStrategicFamily"))
                        && Faction.MECHANIST_COLLEGIA.name().equals(text(
                        props, "currentControllerStrategicFamily"))
                        && Boolean.FALSE.equals(props.get(
                        "controllerFamilyDiffersFromProfileFamily")),
                "forge annex should preserve exact cloister identity while exposing its shared Mechanist family");
        requireContains(text(props, "blueprintAcquisitionStatus"),
                "Mechanist Collegia vendor", "forge annex plan acquisition");
        require(text(props, "exceptionNote").isBlank(),
                "mapped forge annex should not carry an exception note");
    }

    private static void verifyCommonRoomFields(
            GamePanel game, SimulationEditorRepository.EditableEntity entity,
            int roomId, String roomName, Rectangle rectangle) {
        Map<String, Object> props = entity.properties();
        require(Boolean.TRUE.equals(props.get("runtimeSnapshot")),
                "room parity row should be a runtime snapshot");
        require(ROOM_RECORD.equals(props.get("recordClass")),
                "room parity row should expose the exact record class");
        require(Long.valueOf(game.worldTurn).equals(props.get("snapshotWorldTurn")),
                "room parity row should expose its snapshot world turn");
        requireContains(text(props, "sourceMode"), "edits remain mod-local",
                "room parity snapshot isolation label");
        require(number(props, "worldLocationKey").intValue() == game.world.locationKey()
                        && number(props, "sectorX").intValue() == game.world.sectorX
                        && number(props, "sectorY").intValue() == game.world.sectorY
                        && number(props, "zoneX").intValue() == game.world.zoneX
                        && number(props, "zoneY").intValue() == game.world.zoneY
                        && number(props, "floor").intValue() == game.world.floor
                        && Boolean.valueOf(game.world.sewerLayer).equals(
                        props.get("sewerLayer")),
                "room parity row should expose exact live world location fields");
        require(number(props, "roomId").intValue() == roomId
                        && roomName.equals(text(props, "roomName")),
                "room parity row should expose exact room identity");
        require(game.world.zoneType.name().equals(text(props, "zoneType"))
                        && game.world.zoneType.label.equals(text(props, "sourceZone")),
                "room parity row should expose exact zone type and player-facing label");
        require(number(props, "x").intValue() == rectangle.x
                        && number(props, "y").intValue() == rectangle.y
                        && number(props, "width").intValue() == rectangle.width
                        && number(props, "height").intValue() == rectangle.height,
                "room parity row should expose exact live rectangle geometry");
        require(Boolean.TRUE.equals(props.get("roomProfilePresent"))
                        && Boolean.TRUE.equals(props.get("roomBoundsPresent"))
                        && Boolean.FALSE.equals(props.get("specialRoom")),
                "fixture room should expose complete synchronized non-special room data");
        require(expectedRoomId(game.world, roomId).equals(entity.id()),
                "room parity row should use its stable world-location and room-index id");
    }

    private static void verifyAudit(
            SimulationEditorRepository.EditableEntity audit, int rooms, int mapped,
            int nonAcquirable, int gaps, int controllerDivergences,
            int controllerFamilyDivergences) {
        Map<String, Object> props = audit.properties();
        require(Boolean.TRUE.equals(props.get("runtimeSnapshot"))
                        && AUDIT_RECORD.equals(props.get("recordClass")),
                "room parity audit should be a typed runtime snapshot");
        require(number(props, "roomCount").intValue() == rooms
                        && number(props, "mappedRoomCount").intValue() == mapped
                        && number(props, "nonAcquirableExceptionCount").intValue()
                        == nonAcquirable
                        && number(props, "unmappedGapCount").intValue() == gaps
                        && number(props, "controllerDivergenceCount").intValue()
                        == controllerDivergences
                        && number(props, "controllerFamilyDivergenceCount").intValue()
                        == controllerFamilyDivergences
                        && number(props, "specialRoomCount").intValue() == 0
                        && number(props, "missingProfileCount").intValue() == 0,
                "room parity audit should summarize the exact live mapping states");
    }

    private static World compactParityWorld(long seed) {
        World world = new World(seed, 24, 16);
        world.sectorX = 2;
        world.sectorY = 3;
        world.zoneX = 2;
        world.zoneY = 1;
        world.floor = 6;
        world.sewerLayer = false;
        world.zoneType = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        for (int x = 0; x < world.w; x++) {
            for (int y = 0; y < world.h; y++) {
                world.tiles[x][y] = '#';
                world.roomIds[x][y] = -1;
            }
        }
        addRoom(world, new Rectangle(1, 1, 5, 5),
                new RoomProfile("Component Warehouse",
                        "sorted machine limbs, cable bundles, logic Engine plates, and jagged shelving hostile to soft hands",
                        60, Faction.MECHANIST_COLLEGIA,
                        new String[]{"machine parts", "wire bundle"},
                        new char[]{'b', 'N'}),
                Faction.CIVIC_WARDENS, false);
        addRoom(world, new Rectangle(8, 1, 5, 5),
                new RoomProfile("Civic Transit Plaza",
                        "a public transition plaza connecting local corridors",
                        5, Faction.CIVIC_WARDENS,
                        new String[]{"paper scrap"}, new char[]{'q'}),
                Faction.CIVIC_WARDENS, false);
        addRoom(world, new Rectangle(15, 1, 5, 5),
                new RoomProfile("INN Editorial Bullpen",
                        "ranks of desks, copy spikes, censor marks, hot recaf, and reporters converting bloodshed into printable civic posture",
                        38, Faction.INN,
                        new String[]{"Fresh INN newspaper", "Primer slate"},
                        new char[]{'q', 'b'}),
                Faction.INN, false);
        return world;
    }

    private static RoomProfile forgeAnnexProfile() {
        return new RoomProfile("Basic Forge Annex",
                "a compact faction-built 5x4 forge annex with a two-door one-cell connector returning to the staffed source room",
                22, Faction.MECHANICUS_CLOISTER_RED,
                new String[]{"machine scrap", "rivet set", "wire bundle"},
                new char[]{'N', 'q'}).withFeatures(
                "Faction-built shell reserved for an EMM Micro Forge. The completion plaque records prepaid stock, exact source-room labor, connector geometry, and the worker-ledger transfer that staffed this annex.");
    }

    private static void addRoom(World world, Rectangle room, RoomProfile profile,
                                Faction controller, boolean special) {
        int roomId = world.rooms.size();
        world.rooms.add(new Rectangle(room));
        world.roomProfiles.add(profile);
        world.roomFactions.add(controller == null ? Faction.NONE : controller);
        world.roomSpecials.add(special);
        for (int x = room.x; x < room.x + room.width; x++) {
            for (int y = room.y; y < room.y + room.height; y++) {
                world.tiles[x][y] = '.';
                world.roomIds[x][y] = roomId;
            }
        }
    }

    private static List<SimulationEditorRepository.EditableEntity> runtimeRecords(
            SimulationEditorRepository repository) {
        return repository.entities(SimulationToolSuiteRegistry.ROOM_EDITOR).stream()
                .filter(entity -> entity != null
                        && Boolean.TRUE.equals(entity.properties().get("runtimeSnapshot")))
                .toList();
    }

    private static List<SimulationEditorRepository.EditableEntity> roomRecords(
            List<SimulationEditorRepository.EditableEntity> entities) {
        return entities.stream()
                .filter(entity -> entity != null
                        && ROOM_RECORD.equals(entity.properties().get("recordClass")))
                .toList();
    }

    private static SimulationEditorRepository.EditableEntity requireRoom(
            List<SimulationEditorRepository.EditableEntity> entities, int roomId) {
        for (SimulationEditorRepository.EditableEntity entity : entities) {
            Object value = entity.properties().get("roomId");
            if (value instanceof Number number && number.intValue() == roomId) return entity;
        }
        throw new AssertionError("missing Room Editor parity row for room " + roomId);
    }

    private static SimulationEditorRepository.EditableEntity requireClass(
            List<SimulationEditorRepository.EditableEntity> entities, String recordClass) {
        SimulationEditorRepository.EditableEntity found = null;
        for (SimulationEditorRepository.EditableEntity entity : entities) {
            if (entity != null && recordClass.equals(entity.properties().get("recordClass"))) {
                require(found == null, "duplicate Room Editor record class " + recordClass);
                found = entity;
            }
        }
        if (found != null) return found;
        throw new AssertionError("missing Room Editor record class " + recordClass);
    }

    private static int countByClass(
            List<SimulationEditorRepository.EditableEntity> entities, String recordClass) {
        int count = 0;
        for (SimulationEditorRepository.EditableEntity entity : entities) {
            if (entity != null && recordClass.equals(entity.properties().get("recordClass"))) count++;
        }
        return count;
    }

    private static Set<String> uniqueIds(
            List<SimulationEditorRepository.EditableEntity> entities) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (SimulationEditorRepository.EditableEntity entity : entities) {
            if (entity != null) ids.add(entity.id());
        }
        return ids;
    }

    private static String expectedRoomId(World world, int roomId) {
        return "runtime-" + SimulationEditorRepository.slug(
                SimulationToolSuiteRegistry.ROOM_EDITOR + "-room-parity-"
                        + world.locationKey() + "-" + roomId);
    }

    private static Number number(Map<String, Object> values, String key) {
        Object value = values.get(key);
        require(value instanceof Number, "expected numeric Room Editor field " + key
                + ": " + value);
        return (Number) value;
    }

    private static String text(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static void requireContains(String text, String expected, String label) {
        require(text != null && text.contains(expected),
                label + " missing '" + expected + "': " + text);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone05RoomConstructionParityEditorSmoke() { }
}
