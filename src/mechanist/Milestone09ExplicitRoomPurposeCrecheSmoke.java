package mechanist;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

/** Focused Milestone 09 smoke for explicit room-purpose evidence, using the creche first. */
final class Milestone09ExplicitRoomPurposeCrecheSmoke {
    private static final String ROOM_RECORD = "RoomConstructionParityInspection";
    private static final int ROOM_ID = 0;

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");

        verifyLegacyInferenceRejectsMachineRookery();
        verifyLegacyClaimsAreNotPhysicalEvidence();
        verifyCompletePhysicalCrecheOperates();
        verifyFinalTopologyFixtureReconciliation();
        verifyGeneratedStaffingRespectsQualificationAndProtectedRoles();
        verifyGeneratedPopulationRetainsCrecheEvidence();
        verifyMovedAndRemovedEvidenceBlocks();
        verifyHazardMachineryAndControlBlock();
        verifyDuplicateLedgersDoNotDoubleCount();
        verifySaveLoadRecomputesAssessment();
        verifyLookAndRoomEditorReadback();

        System.out.println("Milestone 09 explicit room-purpose creche smoke passed.");
    }

    private static void verifyLegacyInferenceRejectsMachineRookery() {
        RoomProfile machineRoom = new RoomProfile(
                "Servo-Skull Rookery",
                "charging perches, tiny tool nests, data droppings, and the anxious flutter of loyal dead hardware",
                44, Faction.MECHANIST_COLLEGIA,
                new String[]{"logic Engine shard", "machine parts", "wire bundle"},
                new char[]{'R', 'q', 'N'});
        require(machineRoom.declaredPurposeId == null || machineRoom.declaredPurposeId.isBlank(),
                "a Mechanicus Servo-Skull Rookery must remain undesignated machine storage, not infer child care");
        require(ExplicitRoomTypeRequirementAuthority.inferDeclaredPurposeId(
                        machineRoom.name, machineRoom.descriptor, machineRoom.featureText).isBlank(),
                "legacy purpose inference must require actual child-care language, not the generic word rookery");
    }

    private static void verifyLegacyClaimsAreNotPhysicalEvidence() {
        World world = declaredCrecheWorld(190901L);
        RoomPopulationLedger ledger = world.roomPopulationLedgers.get(0);
        ledger.careProviders = Integer.MAX_VALUE;
        ledger.crecheFoodStorageUnits = Integer.MAX_VALUE;
        ledger.crecheWaterStorageUnits = Integer.MAX_VALUE;
        ledger.crecheBedUnits = Integer.MAX_VALUE;
        ledger.crecheTeachingStations = Integer.MAX_VALUE;

        ExplicitRoomTypeRequirementAuthority.Assessment assessment =
                ExplicitRoomTypeRequirementAuthority.assess(world, ledger);
        require(assessment.declared(),
                "the explicit creche profile and ledger should declare the intended room purpose");
        require(!assessment.physicallyQualified() && !assessment.operating(),
                "a creche name, descriptive prose, and maximum legacy counters must not operate without live physical evidence: "
                        + assessment.line());
        require(assessment.observed("creche.food-storage") == 0
                        && assessment.observed("creche.water-storage") == 0
                        && assessment.observed("creche.child-beds") == 0
                        && assessment.observed("creche.teaching") == 0
                        && assessment.observed("creche.care-provider") == 0,
                "legacy counters must not be treated as observed fixtures or assigned living staff: "
                        + assessment.requirementSummary());
        requireContains(assessment.blockers().toString(), "secure food storage 0/1",
                "missing physical food-storage blocker");
        requireContains(assessment.blockers().toString(), "potable water storage 0/1",
                "missing physical water-storage blocker");
        requireContains(assessment.blockers().toString(), "child bed units 0/3",
                "missing physical child-bed blocker");
        requireContains(assessment.blockers().toString(), "teaching station 0/1",
                "missing physical teaching blocker");
        require(FactionCrecheAuthority.tick(world, 0L) == 0
                        && world.crecheCohorts.isEmpty()
                        && world.factionHappinessBoost.getOrDefault(Faction.CIVIC_WARDENS, 0) == 0,
                "unsubstantiated legacy claims must create no cohort or faction happiness");
        require(!FactionCrecheAuthority.buildingReadiness(world, ledger).operating(),
                "the gameplay creche authority must consume the physical assessment");
    }

    private static void verifyCompletePhysicalCrecheOperates() {
        World world = operatingCrecheWorld(190902L);
        RoomPopulationLedger ledger = world.roomPopulationLedgers.get(0);
        ExplicitRoomTypeRequirementAuthority.Assessment assessment =
                ExplicitRoomTypeRequirementAuthority.assess(world, ledger);

        require(assessment.declared() && assessment.physicallyQualified()
                        && assessment.operating()
                        && assessment.status() == ExplicitRoomTypeRequirementAuthority.Status.OPERATING,
                "a complete declared 6x6 creche should operate: " + assessment.line());
        require(assessment.width() == 6 && assessment.height() == 6
                        && assessment.reachableInteriorCells()
                        >= ExplicitRoomTypeRequirementAuthority.CRECHE_MIN_REACHABLE_INTERIOR
                        && assessment.entrances() >= 1,
                "the operating creche should prove 6x6 geometry, reachable interior, and an entrance: "
                        + assessment.requirementSummary());
        require(assessment.observed("creche.food-storage") == 1
                        && assessment.observed("creche.water-storage") == 1
                        && assessment.observed("creche.child-beds") == 3
                        && assessment.observed("creche.teaching") == 1
                        && assessment.assignedCareProviders() == 1,
                "the complete creche should observe its exact physical fixture and caregiver evidence: "
                        + assessment.requirementSummary());
        require(assessment.childCapacity() == 12,
                "one assigned caregiver and three four-place child-bed units should produce capacity 12");
        require(!assessment.witnesses().isEmpty()
                        && assessment.witnessSummary().contains("ROOM-PURPOSE-"),
                "the operating assessment should retain stable physical witnesses: "
                        + assessment.witnessSummary());
        require(FactionCrecheAuthority.buildingReadiness(world, ledger).operating(),
                "the gameplay readiness bridge should accept the complete physical creche");
        require(FactionCrecheAuthority.childCareCapacity(world, ledger) == 12,
                "the gameplay capacity bridge should derive capacity from live staff and bed evidence");
    }

    private static void verifyFinalTopologyFixtureReconciliation() {
        World repaired = declaredCrecheWorld(190913L);
        Rectangle room = repaired.roomRect(ROOM_ID);
        repaired.tiles[room.x][room.y + 2] = '#';
        require(ExplicitRoomTypeRequirementAuthority.materializeGeneratedFixtures(repaired) == 0,
                "a detached declaration must not receive unreachable requirement fixtures");
        repaired.tiles[room.x][room.y + 2] = 'D';
        require(ExplicitRoomTypeRequirementAuthority.materializeGeneratedFixtures(repaired) == 4,
                "materialization after topology repair should install all four reachable fixture groups");
        require(ExplicitRoomTypeRequirementAuthority.materializeGeneratedFixtures(repaired) == 0,
                "final-topology fixture reconciliation should be idempotent");

        World relocated = operatingCrecheWorld(190914L);
        int fixtureCount = relocated.mapObjects.size();
        MapObjectState food = requireCapability(relocated,
                ExplicitRoomTypeRequirementAuthority.Capability.SECURE_FOOD_STORAGE);
        char oldUnder = MapObjectState.underlyingTileFromStock(food.stockState);
        relocated.tiles[food.x][food.y] = oldUnder == 0 ? '.' : oldUnder;
        Rectangle relocatedRoom = relocated.roomRect(ROOM_ID);
        food.x = relocatedRoom.x;
        food.y = relocatedRoom.y;
        require(ExplicitRoomTypeRequirementAuthority.assessRoom(relocated, ROOM_ID)
                        .observed("creche.food-storage") == 0,
                "an unreachable tagged fixture must not satisfy physical qualification");
        require(ExplicitRoomTypeRequirementAuthority.installCrecheFixtures(relocated, ROOM_ID) == 1,
                "reconciliation should relocate the one unreachable required fixture");
        require(relocated.mapObjects.size() == fixtureCount
                        && ExplicitRoomTypeRequirementAuthority.assessRoom(relocated, ROOM_ID)
                        .observed("creche.food-storage") == 1,
                "relocation should restore reachable evidence without duplicating the fixture");
        require(ExplicitRoomTypeRequirementAuthority.installCrecheFixtures(relocated, ROOM_ID) == 0,
                "a relocated reachable fixture should remain idempotent");
    }

    private static void verifyGeneratedStaffingRespectsQualificationAndProtectedRoles() {
        World blocked = declaredCrecheWorld(190915L);
        Point blockedPoint = requireOpenInterior(blocked, ROOM_ID);
        NpcEntity ordinary = NpcEntity.create(Faction.CIVIC_WARDENS, blocked.zoneType,
                blockedPoint.x, blockedPoint.y, new Random(190915L));
        ordinary.role = "Resident";
        blocked.npcs.add(ordinary);
        require(ExplicitRoomTypeRequirementAuthority.ensureGeneratedCareProviders(
                        blocked, new Random(190916L)) == 0
                        && "Resident".equals(ordinary.role),
                "an incomplete declared room must neither receive nor re-role generated care staff");

        World qualified = declaredCrecheWorld(190917L);
        require(ExplicitRoomTypeRequirementAuthority.installCrecheFixtures(qualified, ROOM_ID) == 4,
                "staff-protection fixture setup should physically qualify the room");
        Point leaderPoint = requireOpenInterior(qualified, ROOM_ID);
        NpcEntity leader = NpcEntity.create(Faction.CIVIC_WARDENS, qualified.zoneType,
                leaderPoint.x, leaderPoint.y, new Random(190918L));
        leader.role = "Faction Leader";
        leader.state = "Base of Operations";
        qualified.npcs.add(leader);
        Point vendorPoint = requireOpenInterior(qualified, ROOM_ID);
        NpcEntity vendor = NpcEntity.create(Faction.CIVIC_WARDENS, qualified.zoneType,
                vendorPoint.x, vendorPoint.y, new Random(190919L));
        vendor.role = "Trader-Quartermaster";
        vendor.state = "Trade";
        qualified.npcs.add(vendor);
        int before = qualified.npcs.size();
        require(ExplicitRoomTypeRequirementAuthority.ensureGeneratedCareProviders(
                        qualified, new Random(190920L)) == 1,
                "a physically qualified generated creche should receive one dedicated provider");
        require(qualified.npcs.size() == before + 1
                        && "Faction Leader".equals(leader.role)
                        && "Trader-Quartermaster".equals(vendor.role),
                "generated staffing must preserve leaders and vendors instead of silently re-roling them");
        require(ExplicitRoomTypeRequirementAuthority.assessRoom(qualified, ROOM_ID).operating(),
                "the dedicated generated provider should complete the qualified room's live operation");
    }

    private static void verifyGeneratedPopulationRetainsCrecheEvidence() {
        World world = declaredCrecheWorld(190912L);
        world.mapObjects.clear();
        world.npcs.clear();
        int placed = ExplicitRoomTypeRequirementAuthority.materializeGeneratedFixtures(world);
        require(placed == 4,
                "generated room-purpose materialization should install the four semantic creche fixture groups");

        world.populate();
        ExplicitRoomTypeRequirementAuthority.Assessment assessment =
                ExplicitRoomTypeRequirementAuthority.assessRoom(world, ROOM_ID);
        require(assessment.operating()
                        && assessment.observed("creche.food-storage") == 1
                        && assessment.observed("creche.water-storage") == 1
                        && assessment.observed("creche.child-beds") == 3
                        && assessment.observed("creche.teaching") == 1
                        && assessment.assignedCareProviders() == 1,
                "population generation must preserve earlier semantic fixtures and assign one live provider: "
                        + assessment.requirementSummary());
    }

    private static void verifyMovedAndRemovedEvidenceBlocks() {
        World moved = operatingCrecheWorld(190903L);
        MapObjectState food = requireCapability(moved,
                ExplicitRoomTypeRequirementAuthority.Capability.SECURE_FOOD_STORAGE);
        int oldFoodX = food.x;
        int oldFoodY = food.y;
        moved.tiles[oldFoodX][oldFoodY] = '.';
        food.x = moved.w - 2;
        food.y = moved.h - 2;
        ExplicitRoomTypeRequirementAuthority.Assessment movedAssessment =
                ExplicitRoomTypeRequirementAuthority.assessRoom(moved, ROOM_ID);
        require(!movedAssessment.operating()
                        && movedAssessment.observed("creche.food-storage") == 0,
                "moving required food storage outside the declared room must block operation");
        requireContains(movedAssessment.blockers().toString(), "secure food storage 0/1",
                "moved food-storage blocker");

        World removed = operatingCrecheWorld(190904L);
        MapObjectState teaching = requireCapability(removed,
                ExplicitRoomTypeRequirementAuthority.Capability.TEACHING_STATION);
        removed.mapObjects.remove(teaching);
        removed.tiles[teaching.x][teaching.y] = '.';
        ExplicitRoomTypeRequirementAuthority.Assessment removedAssessment =
                ExplicitRoomTypeRequirementAuthority.assessRoom(removed, ROOM_ID);
        require(!removedAssessment.operating()
                        && removedAssessment.observed("creche.teaching") == 0,
                "removing the teaching fixture must block operation");
        requireContains(removedAssessment.blockers().toString(), "teaching station 0/1",
                "removed teaching-station blocker");
    }

    private static void verifyHazardMachineryAndControlBlock() {
        World hazardous = operatingCrecheWorld(190905L);
        hazardous.hazardWarnings.add(new EnvironmentalHazardRecord(
                "HZ-CRECHE-STEAM", "thermal hazard", "Scalding steam leak",
                "Severe steam crosses the child-care floor.", 4, 4, ROOM_ID, 50, 0));
        ExplicitRoomTypeRequirementAuthority.Assessment hazardAssessment =
                ExplicitRoomTypeRequirementAuthority.assessRoom(hazardous, ROOM_ID);
        require(!hazardAssessment.operating()
                        && hazardAssessment.observed("creche.hazard-conflict") == 1,
                "a severe room-local hazard must block creche operation");
        requireContains(hazardAssessment.blockers().toString(), "severe room hazards 1/0 allowed",
                "severe hazard blocker");

        World industrial = operatingCrecheWorld(190906L);
        Point machinePoint = requireOpenInterior(industrySafeFloor(industrial), ROOM_ID);
        MapObjectState forge = new MapObjectState();
        forge.id = "CRECHE-CONFLICT-FORGE";
        forge.type = AssetIntegrationDisciplineAuthority.EMM_MICRO_FORGE_FIXTURE;
        forge.label = "Live micro forge in child-care room";
        forge.x = machinePoint.x;
        forge.y = machinePoint.y;
        forge.glyph = IndustrialForgeFixtureAuthority.glyphForType(forge.type);
        forge.stockState = "operational=true";
        industrial.mapObjects.add(forge);
        ExplicitRoomTypeRequirementAuthority.Assessment machineryAssessment =
                ExplicitRoomTypeRequirementAuthority.assessRoom(industrial, ROOM_ID);
        require(!machineryAssessment.operating()
                        && machineryAssessment.observed("creche.machine-conflict") == 1,
                "industrial machinery in the creche must block operation");
        requireContains(machineryAssessment.blockers().toString(),
                "hazardous or industrial machinery 1/0 allowed", "industrial machinery blocker");

        World captured = operatingCrecheWorld(190907L);
        captured.roomFactions.set(ROOM_ID, Faction.MECHANIST_COLLEGIA);
        ExplicitRoomTypeRequirementAuthority.Assessment controlAssessment =
                ExplicitRoomTypeRequirementAuthority.assessRoom(captured, ROOM_ID);
        require(!controlAssessment.operating()
                        && controlAssessment.observed("creche.control") == 0,
                "a live controller outside the creche ledger's faction family must block operation");
        requireContains(controlAssessment.blockers().toString(),
                "live faction control alignment 0/1", "controller mismatch blocker");
    }

    private static void verifyDuplicateLedgersDoNotDoubleCount() {
        World world = operatingCrecheWorld(190908L);
        RoomPopulationLedger duplicate = ledger("pop.creche.duplicate");
        duplicate.roomName = "Duplicate accounting row for the same physical creche";
        duplicate.sourceLabel = "duplicate creche accounting row";
        world.roomPopulationLedgers.add(duplicate);

        int created = FactionCrecheAuthority.tick(world, 0L);
        FactionCrecheAuthority.Status status = FactionCrecheAuthority.status(
                world, Faction.CIVIC_WARDENS, 0L);
        require(created == 1 && world.crecheCohorts.size() == 1,
                "duplicate ledgers for one room must create only one annual cohort: created="
                        + created + " cohorts=" + world.crecheCohorts.size());
        require(status.creches() == 1 && status.happinessBoost() == 3,
                "duplicate ledgers for one room must count one operating creche and one +3 happiness contribution: "
                        + status.line());
        require(status.careProviders() == 1 && status.childCareCapacity() == 12,
                "duplicate ledgers must not double the physical caregiver or child capacity: "
                        + status.line());
    }

    private static void verifySaveLoadRecomputesAssessment() {
        World original = operatingCrecheWorld(190909L);
        RoomPopulationLedger originalLedger = original.roomPopulationLedgers.get(0);
        originalLedger.careProviders = 0;
        originalLedger.crecheFoodStorageUnits = 0;
        originalLedger.crecheWaterStorageUnits = 0;
        originalLedger.crecheBedUnits = 0;
        originalLedger.crecheTeachingStations = 0;
        ExplicitRoomTypeRequirementAuthority.Assessment before =
                ExplicitRoomTypeRequirementAuthority.assessRoom(original, ROOM_ID);
        require(before.operating() && before.childCapacity() == 12,
                "zeroed compatibility counters must not erase live physical qualification before save");

        Properties saved = new Properties();
        Persistence.writeWorldState(original, saved);
        World restored = declaredCrecheWorld(190910L);
        Persistence.readWorldState(restored, saved);
        ExplicitRoomTypeRequirementAuthority.Assessment after =
                ExplicitRoomTypeRequirementAuthority.assessRoom(restored, ROOM_ID);

        require(after.operating()
                        && after.status() == before.status()
                        && after.childCapacity() == before.childCapacity()
                        && after.assignedCareProviders() == before.assignedCareProviders(),
                "save/load should recompute the same operating assessment from persisted world evidence: before="
                        + before.line() + " after=" + after.line());
        require(after.observed("creche.food-storage") == 1
                        && after.observed("creche.water-storage") == 1
                        && after.observed("creche.child-beds") == 3
                        && after.observed("creche.teaching") == 1,
                "persisted semantic fixtures should remain the assessment witnesses after load: "
                        + after.requirementSummary());
        require(restored.roomPopulationLedgers.size() == 1
                        && ExplicitRoomTypeRequirementAuthority.CRECHE_ID.equals(
                        restored.roomPopulationLedgers.get(0).declaredRoomPurposeId),
                "the stable explicit room-purpose declaration should survive ledger persistence");
        require(!after.witnesses().isEmpty(),
                "the recomputed assessment should expose persisted fixture and caregiver evidence");
    }

    private static void verifyLookAndRoomEditorReadback() {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        try {
            game.world = operatingCrecheWorld(190911L);
            game.worldTurn = 911L;
            Point lookPoint = requireOpenInterior(game.world, ROOM_ID);
            game.playerX = lookPoint.x;
            game.playerY = lookPoint.y;

            String look = String.join(" | ",
                    ProgressiveLookAuthority.tileStackAt(game, lookPoint.x, lookPoint.y, 4));
            requireContains(look, "Declared purpose: Civic Creche",
                    "Room Look declared-purpose readback");
            requireContains(look, "Room-purpose status: OPERATING",
                    "Room Look operating-status readback");
            requireContainsIgnoreCase(look, "Capacity: 12 children",
                    "Room Look derived child capacity");
            requireContainsIgnoreCase(look, "evidence",
                    "Room Look physical-evidence readback");

            List<SimulationEditorRepository.EditableEntity> snapshot =
                    SimulationRuntimeEditorBridgeAuthority.snapshot(
                            game, SimulationToolSuiteRegistry.ROOM_EDITOR);
            SimulationEditorRepository.EditableEntity row = requireRoomRecord(snapshot, ROOM_ID);
            Map<String, Object> props = row.properties();
            require(ExplicitRoomTypeRequirementAuthority.CRECHE_ID.equals(
                            text(props, "declaredPurposeId"))
                            && "Civic Creche".equals(text(props, "declaredPurposeLabel")),
                    "Room Editor should expose the stable declared purpose and player-facing label: " + props);
            require("OPERATING".equals(text(props, "roomPurposeStatus"))
                            && Boolean.TRUE.equals(props.get("roomPurposeOperating"))
                            && Boolean.TRUE.equals(props.get("roomPurposePhysicallyQualified"))
                            && number(props, "roomPurposeChildCapacity").intValue() == 12,
                    "Room Editor should expose the live operating assessment and capacity: " + props);
            require(!text(props, "roomPurposeEvidence").isBlank(),
                    "Room Editor should expose concrete physical evidence witnesses");

            MapObjectState teaching = requireCapability(game.world,
                    ExplicitRoomTypeRequirementAuthority.Capability.TEACHING_STATION);
            game.world.mapObjects.remove(teaching);
            game.world.tiles[teaching.x][teaching.y] = '.';
            List<SimulationEditorRepository.EditableEntity> blockedSnapshot =
                    SimulationRuntimeEditorBridgeAuthority.snapshot(
                            game, SimulationToolSuiteRegistry.ROOM_EDITOR);
            Map<String, Object> blockedProps = requireRoomRecord(
                    blockedSnapshot, ROOM_ID).properties();
            require("PLANNED_BLOCKED".equals(text(blockedProps, "roomPurposeStatus"))
                            && Boolean.FALSE.equals(blockedProps.get("roomPurposeOperating")),
                    "Room Editor refresh should observe the removed teaching fixture: " + blockedProps);
            requireContains(text(blockedProps, "roomPurposeBlockers"), "teaching station 0/1",
                    "Room Editor live blocker readback");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static World operatingCrecheWorld(long seed) {
        World world = declaredCrecheWorld(seed);
        int placed = ExplicitRoomTypeRequirementAuthority.installCrecheFixtures(world, ROOM_ID);
        require(placed == 4,
                "the creche fixture materializer should place food, water, bed, and teaching witnesses exactly once: "
                        + placed);
        RoomPopulationLedger ledger = world.roomPopulationLedgers.get(0);
        Point point = requireOpenInterior(world, ROOM_ID);
        NpcEntity provider = NpcEntity.create(
                Faction.CIVIC_WARDENS, world.zoneType, point.x, point.y, new Random(seed ^ 0xCA4EL));
        provider.id = "CRECHE-PROVIDER-" + seed;
        provider.name = "Mara Child-Minder";
        provider.role = "Creche Care Provider";
        provider.state = "Creche Duty";
        provider.homeX = point.x;
        provider.homeY = point.y;
        PersonnelPopulationApi.attachProvenance(provider, world, ROOM_ID, ledger,
                "assigned creche care provider", new Random(seed ^ 0x51AFFL));
        world.npcs.add(provider);
        return world;
    }

    private static World declaredCrecheWorld(long seed) {
        World world = new World(seed, 18, 14);
        world.zoneType = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        world.sectorX = 1;
        world.sectorY = 2;
        world.zoneX = 1;
        world.zoneY = 1;
        world.floor = 4;
        world.npcs.clear();
        world.mapObjects.clear();
        world.hazardWarnings.clear();
        world.roomPopulationLedgers.clear();
        world.crecheCohorts.clear();
        world.factionHappinessBoost.clear();
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

        Rectangle room = new Rectangle(2, 2, 6, 6);
        RoomProfile profile = new RoomProfile(
                "Civic Faction Creche",
                "a minimum six-by-six child-care room claiming food, water, three child-bed units, teaching, and care staff",
                36, Faction.CIVIC_WARDENS,
                new String[]{"Child creche snack pack", "Water bottle", "Creche lesson toy"},
                new char[]{'b', 'u', 'c', 'q'});
        profile.declaredPurposeId = ExplicitRoomTypeRequirementAuthority.CRECHE_ID;
        world.rooms.add(room);
        world.roomProfiles.add(profile);
        world.roomFactions.add(Faction.CIVIC_WARDENS);
        world.roomSpecials.add(Boolean.FALSE);
        for (int x = room.x; x < room.x + room.width; x++) {
            for (int y = room.y; y < room.y + room.height; y++) {
                boolean boundary = x == room.x || y == room.y
                        || x == room.x + room.width - 1
                        || y == room.y + room.height - 1;
                world.tiles[x][y] = boundary ? '#' : '.';
                world.roomIds[x][y] = ROOM_ID;
            }
        }
        world.tiles[room.x][room.y + 2] = 'D';
        world.roomPopulationLedgers.add(ledger("pop.creche.primary"));
        return world;
    }

    private static RoomPopulationLedger ledger(String id) {
        RoomPopulationLedger ledger = new RoomPopulationLedger();
        ledger.id = id;
        ledger.roomId = ROOM_ID;
        ledger.roomName = "Civic Faction Creche";
        ledger.faction = Faction.CIVIC_WARDENS;
        ledger.sourceKind = "creche population ledger";
        ledger.sourceLabel = "Civic Wardens creche population ledger";
        ledger.facilityPurpose = "child care and generational continuity";
        ledger.declaredRoomPurposeId = ExplicitRoomTypeRequirementAuthority.CRECHE_ID;
        ledger.capacity = 12;
        ledger.available = 12;
        return ledger;
    }

    private static MapObjectState requireCapability(
            World world, ExplicitRoomTypeRequirementAuthority.Capability capability) {
        for (MapObjectState object : world.mapObjects) {
            if (object != null && capability.name().equals(
                    MapObjectState.stockValue(object.stockState, "capability"))) return object;
        }
        throw new AssertionError("missing room-purpose fixture capability " + capability);
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

    private static World industrySafeFloor(World world) {
        require(world != null, "industrial conflict fixture requires a world");
        return world;
    }

    private static SimulationEditorRepository.EditableEntity requireRoomRecord(
            List<SimulationEditorRepository.EditableEntity> entities, int roomId) {
        for (SimulationEditorRepository.EditableEntity entity : entities) {
            if (entity == null || !ROOM_RECORD.equals(entity.properties().get("recordClass"))) continue;
            Object value = entity.properties().get("roomId");
            if (value instanceof Number number && number.intValue() == roomId) return entity;
        }
        throw new AssertionError("missing Room Editor live room-purpose record for room " + roomId);
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

    private Milestone09ExplicitRoomPurposeCrecheSmoke() { }
}
