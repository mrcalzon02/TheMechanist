package mechanist;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

/** Focused Milestone 09 smoke for the security-barracks room-purpose definition. */
final class Milestone09ExplicitRoomPurposeBarracksSmoke {
    private static final String ROOM_RECORD = "RoomConstructionParityInspection";
    private static final int BARRACKS_ROOM = 0;
    private static final int OTHER_ROOM = 1;

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");

        verifyDeclarationBoundary();
        verifyCompleteBarracksOperates();
        verifyEvidenceMustBeLocalAndReachable();
        verifyDutyStaffMustBeLivingAssignedAndAligned();
        verifyHazardMachineryAndControlBlock();
        verifyReinforcementSourceAndArrivalRecheck();
        verifySaveLoadRecomputesAssessment();
        verifyGeneratedReconciliationIsIdempotent();
        verifyLookAndRoomEditorReadback();

        System.out.println("Milestone 09 explicit room-purpose security barracks smoke passed.");
    }

    private static void verifyDeclarationBoundary() {
        String[][] fuzzyRooms = {
                {"Security Office", "desks, incident forms, and a locked records cabinet"},
                {"Duty Records Room", "shift ledgers, watch schedules, and archived patrol reports"},
                {"Precinct Evidence Store", "sealed evidence lockers and contraband tags"}
        };
        for (String[] room : fuzzyRooms) {
            RoomProfile profile = new RoomProfile(room[0], room[1], 30,
                    Faction.CIVIC_WARDENS, new String[]{"permit stub"}, new char[]{'q'});
            require(profile.declaredPurposeId == null || profile.declaredPurposeId.isBlank(),
                    "fuzzy security/duty/precinct prose must not declare a barracks: " + room[0]);
            require(ExplicitRoomTypeRequirementAuthority.inferDeclaredPurposeId(
                            profile.name, profile.descriptor, profile.featureText).isBlank(),
                    "legacy inference must reject fuzzy barracks-adjacent prose: " + room[0]);
        }

        RoomProfile tight = new RoomProfile(
                "Civic Wardens Duty Barracks",
                "a guarded barracks with duty berths, an equipment issue post, and a muster anchor",
                45, Faction.CIVIC_WARDENS,
                new String[]{"Flak vest", "Shock maul"}, new char[]{'c', 'q', 'A'});
        require(ExplicitRoomTypeRequirementAuthority.BARRACKS_ID.equals(tight.declaredPurposeId),
                "the tightly authored Duty Barracks phrase should declare the stable barracks purpose");

        World fuzzy = barracksWorld(191001L, false);
        RoomPopulationLedger ledger = fuzzy.roomPopulationLedgers.get(0);
        ledger.roomName = "Precinct Security Duty Room";
        ledger.sourceKind = "precinct duty roster";
        ledger.sourceLabel = "security watch ledger";
        ExplicitRoomTypeRequirementAuthority.Assessment assessment =
                ExplicitRoomTypeRequirementAuthority.assess(fuzzy, ledger);
        require(!assessment.declared() && !assessment.operating(),
                "a fuzzy ledger and empty room must remain undesignated: " + assessment.line());
        require(!FactionReinforcementAuthority.availableMethods(fuzzy, Faction.CIVIC_WARDENS)
                        .contains(FactionReinforcementAuthority.SourceMethod.BARRACKS_MUSTER),
                "fuzzy security/duty/precinct ledger text must not unlock barracks reinforcement");
    }

    private static void verifyCompleteBarracksOperates() {
        World world = operatingBarracksWorld(191002L);
        ExplicitRoomTypeRequirementAuthority.Assessment assessment =
                ExplicitRoomTypeRequirementAuthority.assessRoom(world, BARRACKS_ROOM);

        require(assessment.declared() && assessment.physicallyQualified()
                        && assessment.operating()
                        && assessment.status() == ExplicitRoomTypeRequirementAuthority.Status.OPERATING,
                "a complete declared security barracks should operate: " + assessment.line());
        require(assessment.width() >= ExplicitRoomTypeRequirementAuthority.BARRACKS_MIN_WIDTH
                        && assessment.height() >= ExplicitRoomTypeRequirementAuthority.BARRACKS_MIN_HEIGHT
                        && assessment.reachableInteriorCells()
                        >= ExplicitRoomTypeRequirementAuthority.BARRACKS_MIN_REACHABLE_INTERIOR
                        && assessment.entrances() >= 1,
                "the operating barracks should prove its exact geometry and entrance: "
                        + assessment.requirementSummary());
        require(assessment.observed("barracks.duty-berths") == 4
                        && assessment.observed("barracks.equipment-store") == 1
                        && assessment.observed("barracks.muster-anchor") == 1
                        && assessment.assignedDutyStaff() == 1,
                "the operating barracks should observe exact berth, equipment, muster, and duty-staff evidence: "
                        + assessment.requirementSummary());
        require(assessment.dutyBerthUnits() == 4 && assessment.dutyCapacity() == 4
                        && assessment.capacityUnits() == 4
                        && assessment.operatingCapacity() == 4
                        && "duty personnel".equals(assessment.capacityUnitLabel()),
                "barracks capacity must be four duty personnel, derived from reachable berth evidence");
        require(ExplicitRoomTypeRequirementAuthority.barracksDefinition().requirements().size() == 11,
                "the barracks registry should expose its complete eleven-requirement contract");
        require(FactionReinforcementAuthority.availableMethods(world, Faction.CIVIC_WARDENS)
                        .contains(FactionReinforcementAuthority.SourceMethod.BARRACKS_MUSTER),
                "an operating barracks should unlock the bounded muster source");
    }

    private static void verifyEvidenceMustBeLocalAndReachable() {
        World otherRoom = operatingBarracksWorld(191003L);
        MapObjectState equipment = requireCapability(otherRoom,
                ExplicitRoomTypeRequirementAuthority.Capability.DUTY_EQUIPMENT_STORAGE);
        moveObject(otherRoom, equipment, requireOpenInterior(otherRoom, OTHER_ROOM));
        ExplicitRoomTypeRequirementAuthority.Assessment otherAssessment =
                ExplicitRoomTypeRequirementAuthority.assessRoom(otherRoom, BARRACKS_ROOM);
        require(!otherAssessment.operating()
                        && otherAssessment.observed("barracks.equipment-store") == 0,
                "equipment stored in another room must not qualify the barracks: "
                        + otherAssessment.requirementSummary());

        World unreachable = operatingBarracksWorld(191004L);
        MapObjectState muster = requireCapability(unreachable,
                ExplicitRoomTypeRequirementAuthority.Capability.MUSTER_ANCHOR);
        Rectangle room = unreachable.roomRect(BARRACKS_ROOM);
        moveObject(unreachable, muster, new Point(room.x, room.y));
        ExplicitRoomTypeRequirementAuthority.Assessment unreachableAssessment =
                ExplicitRoomTypeRequirementAuthority.assessRoom(unreachable, BARRACKS_ROOM);
        require(!unreachableAssessment.operating()
                        && unreachableAssessment.observed("barracks.muster-anchor") == 0,
                "an unreachable tagged muster fixture must not qualify the barracks: "
                        + unreachableAssessment.requirementSummary());
    }

    private static void verifyDutyStaffMustBeLivingAssignedAndAligned() {
        World world = barracksWorld(191005L, true);
        require(ExplicitRoomTypeRequirementAuthority.installBarracksFixtures(
                        world, BARRACKS_ROOM) == 3,
                "duty-staff evidence setup should install all three fixture groups");
        RoomPopulationLedger ledger = world.roomPopulationLedgers.get(0);

        NpcEntity unassigned = dutyActor(world, Faction.CIVIC_WARDENS,
                requireOpenInterior(world, OTHER_ROOM), 191006L);
        world.npcs.add(unassigned);

        NpcEntity wrongFamily = dutyActor(world, Faction.MECHANIST_COLLEGIA,
                requireOpenInterior(world, BARRACKS_ROOM), 191007L);
        PersonnelPopulationApi.attachProvenance(wrongFamily, world, BARRACKS_ROOM, ledger,
                "wrong-family duty assignment", new Random(191007L));
        world.npcs.add(wrongFamily);

        NpcEntity dead = dutyActor(world, Faction.CIVIC_WARDENS,
                requireOpenInterior(world, BARRACKS_ROOM), 191008L);
        PersonnelPopulationApi.attachProvenance(dead, world, BARRACKS_ROOM, ledger,
                "fallen duty assignment", new Random(191008L));
        dead.hp = 0;
        world.npcs.add(dead);

        ExplicitRoomTypeRequirementAuthority.Assessment blocked =
                ExplicitRoomTypeRequirementAuthority.assessRoom(world, BARRACKS_ROOM);
        require(!blocked.operating() && blocked.assignedDutyStaff() == 0,
                "dead, unassigned, and wrong-family duty actors must not satisfy staffing: "
                        + blocked.requirementSummary());

        NpcEntity assigned = dutyActor(world, Faction.CIVIC_WARDENS,
                requireOpenInterior(world, BARRACKS_ROOM), 191009L);
        PersonnelPopulationApi.attachProvenance(assigned, world, BARRACKS_ROOM, ledger,
                "assigned barracks duty guard", new Random(191009L));
        world.npcs.add(assigned);
        ExplicitRoomTypeRequirementAuthority.Assessment operating =
                ExplicitRoomTypeRequirementAuthority.assessRoom(world, BARRACKS_ROOM);
        require(operating.operating() && operating.assignedDutyStaff() == 1,
                "one living, assigned, aligned duty guard should complete operation: " + operating.line());
    }

    private static void verifyHazardMachineryAndControlBlock() {
        World hazardous = operatingBarracksWorld(191010L);
        hazardous.hazardWarnings.add(new EnvironmentalHazardRecord(
                "HZ-BARRACKS-FIRE", "thermal hazard", "Barracks fire",
                "Severe fire crosses the duty floor.", 4, 3, BARRACKS_ROOM, 50, 0));
        ExplicitRoomTypeRequirementAuthority.Assessment hazard =
                ExplicitRoomTypeRequirementAuthority.assessRoom(hazardous, BARRACKS_ROOM);
        require(!hazard.operating() && hazard.observed("barracks.hazard-conflict") == 1,
                "a severe local hazard must block barracks operation");

        World laboratory = operatingBarracksWorld(191011L);
        addConflictingMachine(laboratory, AssetIntegrationDisciplineAuthority.LABORATORY_ROOM_FIXTURE,
                "BARRACKS-CONFLICT-LAB");
        ExplicitRoomTypeRequirementAuthority.Assessment lab =
                ExplicitRoomTypeRequirementAuthority.assessRoom(laboratory, BARRACKS_ROOM);
        require(!lab.operating() && lab.observed("barracks.machine-conflict") == 1,
                "a laboratory fixture on the duty floor must block barracks operation");

        World forgeWorld = operatingBarracksWorld(191012L);
        addConflictingMachine(forgeWorld, AssetIntegrationDisciplineAuthority.EMM_MICRO_FORGE_FIXTURE,
                "BARRACKS-CONFLICT-FORGE");
        ExplicitRoomTypeRequirementAuthority.Assessment forge =
                ExplicitRoomTypeRequirementAuthority.assessRoom(forgeWorld, BARRACKS_ROOM);
        require(!forge.operating() && forge.observed("barracks.machine-conflict") == 1,
                "a forge fixture on the duty floor must block barracks operation");

        World captured = operatingBarracksWorld(191013L);
        captured.roomFactions.set(BARRACKS_ROOM, Faction.MECHANIST_COLLEGIA);
        ExplicitRoomTypeRequirementAuthority.Assessment control =
                ExplicitRoomTypeRequirementAuthority.assessRoom(captured, BARRACKS_ROOM);
        require(!control.operating() && control.observed("barracks.control") == 0,
                "a controller outside the ledger faction family must block barracks operation");
    }

    private static void verifyReinforcementSourceAndArrivalRecheck() {
        World world = operatingBarracksWorld(191014L);
        RoomPopulationLedger ledger = world.roomPopulationLedgers.get(0);
        require(FactionReinforcementAuthority.availableMethods(world, Faction.CIVIC_WARDENS)
                        .contains(FactionReinforcementAuthority.SourceMethod.BARRACKS_MUSTER),
                "the operating physical barracks should advertise muster availability");

        PersonnelReplacementRequest request = new PersonnelReplacementRequest();
        request.deadNpcId = "BARRACKS-RECHECK-CASUALTY";
        request.deadName = "Officer Arrival Recheck";
        request.faction = Faction.CIVIC_WARDENS;
        request.source = "Barracks reserve muster through " + ledger.sourceLabel;
        request.sourceMode = FactionReinforcementAuthority.SourceMethod.BARRACKS_MUSTER.id;
        request.sourceLedgerId = ledger.id;
        request.sourceRoomId = BARRACKS_ROOM;
        request.sourcePrerequisite = FactionReinforcementAuthority.SourceMethod.BARRACKS_MUSTER.prerequisite;
        request.scriptCost = FactionReinforcementAuthority.SourceMethod.BARRACKS_MUSTER.scriptCost;
        request.requestedTurn = 0;
        request.dueTurn = 20;
        request.expiresTurn = 100;
        request.x = 4;
        request.y = 3;
        world.replacementQueue.add(request);

        world.roomFactions.set(BARRACKS_ROOM, Faction.MECHANIST_COLLEGIA);
        require(!FactionReinforcementAuthority.availableMethods(world, Faction.CIVIC_WARDENS)
                        .contains(FactionReinforcementAuthority.SourceMethod.BARRACKS_MUSTER)
                        && !FactionReinforcementAuthority.routeReady(world, request),
                "breaking the bound barracks before arrival must remove current availability and route readiness");
        FactionReinforcementAuthority.ReceptionResult blocked = FactionReinforcementAuthority.receive(
                world, Faction.CIVIC_WARDENS, 20, 6, new Random(191014L));
        require(!blocked.success() && blocked.arrived() == 0 && blocked.scriptCost() == 0
                        && world.replacementQueue.contains(request),
                "a broken selected muster must remain queued and unpaid at arrival: " + blocked.message());

        world.roomFactions.set(BARRACKS_ROOM, Faction.CIVIC_WARDENS);
        require(FactionReinforcementAuthority.routeReady(world, request),
                "restoring the same barracks evidence should restore the bound route");
        FactionReinforcementAuthority.ReceptionResult received = FactionReinforcementAuthority.receive(
                world, Faction.CIVIC_WARDENS, 20, 6, new Random(191015L));
        require(received.success() && received.arrived() == 1 && received.scriptCost() == 6
                        && !world.replacementQueue.contains(request),
                "restored evidence should allow exactly one paid barracks arrival: " + received.message());
    }

    private static void verifySaveLoadRecomputesAssessment() {
        World original = operatingBarracksWorld(191016L);
        ExplicitRoomTypeRequirementAuthority.Assessment before =
                ExplicitRoomTypeRequirementAuthority.assessRoom(original, BARRACKS_ROOM);
        Properties saved = new Properties();
        Persistence.writeWorldState(original, saved);

        World restored = barracksWorld(191017L, true);
        Persistence.readWorldState(restored, saved);
        ExplicitRoomTypeRequirementAuthority.Assessment after =
                ExplicitRoomTypeRequirementAuthority.assessRoom(restored, BARRACKS_ROOM);
        require(before.operating() && after.operating()
                        && after.status() == before.status()
                        && after.dutyCapacity() == before.dutyCapacity()
                        && after.assignedDutyStaff() == before.assignedDutyStaff(),
                "save/load should recompute the same operating barracks from persisted evidence: before="
                        + before.line() + " after=" + after.line());
        require(after.observed("barracks.duty-berths") == 4
                        && after.observed("barracks.equipment-store") == 1
                        && after.observed("barracks.muster-anchor") == 1,
                "semantic barracks fixtures should survive save/load: " + after.requirementSummary());
        require(restored.roomPopulationLedgers.size() == 1
                        && ExplicitRoomTypeRequirementAuthority.BARRACKS_ID.equals(
                        restored.roomPopulationLedgers.get(0).declaredRoomPurposeId),
                "the explicit security-barracks declaration should survive ledger persistence");
    }

    private static void verifyGeneratedReconciliationIsIdempotent() {
        World world = barracksWorld(191018L, true);
        require(ExplicitRoomTypeRequirementAuthority.materializeGeneratedFixtures(world) == 3,
                "generated reconciliation should install berth, equipment, and muster fixture groups");
        require(ExplicitRoomTypeRequirementAuthority.materializeGeneratedFixtures(world) == 0,
                "generated barracks fixture reconciliation should be idempotent");
        require(ExplicitRoomTypeRequirementAuthority.ensureGeneratedBarracksDutyStaff(
                        world, new Random(191018L)) == 1,
                "a physically qualified generated barracks should receive one duty guard");
        require(ExplicitRoomTypeRequirementAuthority.ensureGeneratedBarracksDutyStaff(
                        world, new Random(191019L)) == 0,
                "generated barracks staff reconciliation should be idempotent");
        require(ExplicitRoomTypeRequirementAuthority.assessRoom(world, BARRACKS_ROOM).operating(),
                "idempotent generated reconciliation should leave one operating barracks");

        int objectCount = world.mapObjects.size();
        MapObjectState equipment = requireCapability(world,
                ExplicitRoomTypeRequirementAuthority.Capability.DUTY_EQUIPMENT_STORAGE);
        Rectangle room = world.roomRect(BARRACKS_ROOM);
        moveObject(world, equipment, new Point(room.x, room.y));
        require(ExplicitRoomTypeRequirementAuthority.installBarracksFixtures(
                        world, BARRACKS_ROOM) == 1,
                "reconciliation should relocate one unreachable barracks witness");
        require(world.mapObjects.size() == objectCount
                        && ExplicitRoomTypeRequirementAuthority.assessRoom(world, BARRACKS_ROOM)
                        .observed("barracks.equipment-store") == 1,
                "unreachable-witness reconciliation should restore evidence without duplication");
    }

    private static void verifyLookAndRoomEditorReadback() {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        try {
            game.world = operatingBarracksWorld(191020L);
            Point point = requireOpenInterior(game.world, BARRACKS_ROOM);
            game.playerX = point.x;
            game.playerY = point.y;

            String look = String.join(" | ",
                    ProgressiveLookAuthority.tileStackAt(game, point.x, point.y, 4));
            requireContains(look, "Declared purpose: Security Barracks",
                    "Room Look barracks declaration");
            requireContains(look, "Room-purpose status: OPERATING",
                    "Room Look barracks operating status");
            requireContainsIgnoreCase(look, "Capacity: 4 duty personnel",
                    "Room Look duty capacity");
            require(!look.toLowerCase(Locale.ROOT).contains("capacity: 4 children"),
                    "Room Look must not describe barracks capacity as children: " + look);

            List<SimulationEditorRepository.EditableEntity> snapshot =
                    SimulationRuntimeEditorBridgeAuthority.snapshot(
                            game, SimulationToolSuiteRegistry.ROOM_EDITOR);
            Map<String, Object> props = requireRoomRecord(snapshot, BARRACKS_ROOM).properties();
            require(ExplicitRoomTypeRequirementAuthority.BARRACKS_ID.equals(
                            text(props, "declaredPurposeId"))
                            && "Security Barracks".equals(text(props, "declaredPurposeLabel")),
                    "Room Editor should expose the stable barracks purpose: " + props);
            require("OPERATING".equals(text(props, "roomPurposeStatus"))
                            && Boolean.TRUE.equals(props.get("roomPurposeOperating"))
                            && number(props, "roomPurposeOperatingCapacity").intValue() == 4
                            && "duty personnel".equals(text(props, "roomPurposeCapacityUnitLabel")),
                    "Room Editor should expose generic duty capacity without child semantics: " + props);
            require(!text(props, "roomPurposeEvidence").isBlank()
                            && !text(props, "roomPurposeCapacityUnitLabel").toLowerCase(Locale.ROOT)
                            .contains("child"),
                    "Room Editor should expose concrete evidence and truthful barracks capacity units");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static World operatingBarracksWorld(long seed) {
        World world = barracksWorld(seed, true);
        require(ExplicitRoomTypeRequirementAuthority.installBarracksFixtures(
                        world, BARRACKS_ROOM) == 3,
                "barracks fixture materialization should place three exact fixture groups");
        RoomPopulationLedger ledger = world.roomPopulationLedgers.get(0);
        NpcEntity guard = dutyActor(world, Faction.CIVIC_WARDENS,
                requireOpenInterior(world, BARRACKS_ROOM), seed ^ 0xBA44ACL);
        PersonnelPopulationApi.attachProvenance(guard, world, BARRACKS_ROOM, ledger,
                "assigned barracks duty guard", new Random(seed ^ 0xD071L));
        world.npcs.add(guard);
        return world;
    }

    private static World barracksWorld(long seed, boolean declared) {
        World world = new World(seed, 20, 14);
        world.zoneType = ZoneType.ARBITES_PRECINCT_EDGE;
        world.sectorX = 1;
        world.sectorY = 2;
        world.zoneX = 1;
        world.zoneY = 1;
        world.floor = 5;
        world.npcs.clear();
        world.mapObjects.clear();
        world.hazardWarnings.clear();
        world.roomPopulationLedgers.clear();
        world.replacementQueue.clear();
        world.rooms.clear();
        world.roomProfiles.clear();
        world.roomFactions.clear();
        world.roomSpecials.clear();
        for (int x = 0; x < world.w; x++) {
            for (int y = 0; y < world.h; y++) {
                world.tiles[x][y] = '#';
                world.roomIds[x][y] = -1;
            }
        }

        RoomProfile barracks = new RoomProfile(
                declared ? "Civic Wardens Duty Barracks" : "Security Office",
                declared
                        ? "a guarded barracks with four duty berths, an equipment issue post, and a muster anchor"
                        : "desks, incident forms, duty records, and a precinct evidence cabinet",
                45, Faction.CIVIC_WARDENS,
                new String[]{"Flak vest", "Shock maul"}, new char[]{'c', 'q', 'A'});
        barracks.declaredPurposeId = declared
                ? ExplicitRoomTypeRequirementAuthority.BARRACKS_ID : "";
        addRoom(world, new Rectangle(2, 2, 6, 4), barracks, Faction.CIVIC_WARDENS);

        RoomProfile store = new RoomProfile(
                "Civic Records Annex",
                "an ordinary records room with shelves and permit boxes",
                25, Faction.CIVIC_WARDENS,
                new String[]{"permit stub"}, new char[]{'q'});
        addRoom(world, new Rectangle(10, 2, 6, 4), store, Faction.CIVIC_WARDENS);

        RoomPopulationLedger ledger = new RoomPopulationLedger();
        ledger.id = "pop.barracks.primary";
        ledger.roomId = BARRACKS_ROOM;
        ledger.roomName = barracks.name;
        ledger.faction = Faction.CIVIC_WARDENS;
        ledger.sourceKind = declared ? "security barracks muster roster" : "security duty roster";
        ledger.sourceLabel = "Civic Wardens " + ledger.sourceKind;
        ledger.facilityPurpose = declared ? "guard readiness and bounded reserve muster" : "records administration";
        ledger.declaredRoomPurposeId = declared
                ? ExplicitRoomTypeRequirementAuthority.BARRACKS_ID : "";
        ledger.capacity = 8;
        ledger.available = 8;
        world.roomPopulationLedgers.add(ledger);
        return world;
    }

    private static void addRoom(World world, Rectangle room, RoomProfile profile, Faction faction) {
        int roomId = world.rooms.size();
        world.rooms.add(room);
        world.roomProfiles.add(profile);
        world.roomFactions.add(faction);
        world.roomSpecials.add(Boolean.FALSE);
        for (int x = room.x; x < room.x + room.width; x++) {
            for (int y = room.y; y < room.y + room.height; y++) {
                boolean boundary = x == room.x || y == room.y
                        || x == room.x + room.width - 1
                        || y == room.y + room.height - 1;
                world.tiles[x][y] = boundary ? '#' : '.';
                world.roomIds[x][y] = roomId;
            }
        }
        world.tiles[room.x][room.y + 1] = 'D';
    }

    private static NpcEntity dutyActor(World world, Faction faction, Point point, long seed) {
        NpcEntity actor = NpcEntity.create(faction, world.zoneType,
                point.x, point.y, new Random(seed));
        actor.id = "BARRACKS-DUTY-" + seed;
        actor.name = "Duty Guard " + Math.abs(seed);
        actor.role = "Barracks Duty Guard";
        actor.state = "Barracks Duty";
        actor.homeX = point.x;
        actor.homeY = point.y;
        return actor;
    }

    private static void addConflictingMachine(World world, String type, String id) {
        Point point = requireOpenInterior(world, BARRACKS_ROOM);
        MapObjectState machine = new MapObjectState();
        machine.id = id;
        machine.type = type;
        machine.label = "Conflicting machine on barracks duty floor";
        machine.x = point.x;
        machine.y = point.y;
        machine.glyph = LabChemicalFixtureAuthority.isFamilyType(type)
                ? LabChemicalFixtureAuthority.glyphForType(type)
                : IndustrialForgeFixtureAuthority.glyphForType(type);
        machine.stockState = "operational=true";
        world.tiles[point.x][point.y] = machine.glyph;
        world.mapObjects.add(machine);
    }

    private static MapObjectState requireCapability(
            World world, ExplicitRoomTypeRequirementAuthority.Capability capability) {
        for (MapObjectState object : world.mapObjects) {
            if (object != null && capability.name().equals(
                    MapObjectState.stockValue(object.stockState, "capability"))) return object;
        }
        throw new AssertionError("missing room-purpose fixture capability " + capability);
    }

    private static void moveObject(World world, MapObjectState object, Point destination) {
        char underlying = MapObjectState.underlyingTileFromStock(object.stockState);
        if (world.inBounds(object.x, object.y)) {
            world.tiles[object.x][object.y] = underlying == 0 ? '.' : underlying;
        }
        object.x = destination.x;
        object.y = destination.y;
        if (world.inBounds(object.x, object.y)) world.tiles[object.x][object.y] = object.glyph;
    }

    private static Point requireOpenInterior(World world, int roomId) {
        Rectangle room = world.roomRect(roomId);
        if (room != null) {
            for (int x = room.x + 1; x < room.x + room.width - 1; x++) {
                for (int y = room.y + 1; y < room.y + room.height - 1; y++) {
                    if (world.inBounds(x, y) && world.roomIdAt(x, y) == roomId
                            && world.walkable(x, y) && world.mapObjectAt(x, y) == null
                            && world.npcAt(x, y) == null) return new Point(x, y);
                }
            }
        }
        throw new AssertionError("missing open interior point for room " + roomId);
    }

    private static SimulationEditorRepository.EditableEntity requireRoomRecord(
            List<SimulationEditorRepository.EditableEntity> entities, int roomId) {
        for (SimulationEditorRepository.EditableEntity entity : entities) {
            if (entity == null || !ROOM_RECORD.equals(entity.properties().get("recordClass"))) continue;
            Object value = entity.properties().get("roomId");
            if (value instanceof Number number && number.intValue() == roomId) return entity;
        }
        throw new AssertionError("missing Room Editor live barracks record for room " + roomId);
    }

    private static Number number(Map<String, Object> values, String key) {
        Object value = values.get(key);
        require(value instanceof Number,
                "expected numeric Room Editor property " + key + ": " + value);
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

    private static void requireContainsIgnoreCase(String text, String expected, String label) {
        String haystack = text == null ? "" : text.toLowerCase(Locale.ROOT);
        String needle = expected == null ? "" : expected.toLowerCase(Locale.ROOT);
        require(haystack.contains(needle),
                label + " missing '" + expected + "': " + text);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone09ExplicitRoomPurposeBarracksSmoke() { }
}
