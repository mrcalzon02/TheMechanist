package mechanist;

import java.awt.Point;
import java.awt.Rectangle;
import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Data-owned room-purpose requirements and live physical evidence.
 *
 * The registry deliberately separates a room's declared purpose from the uses
 * its current contents support and from whether that use can operate now. The
 * creche and security barracks are the first complete definitions; later room
 * types should extend this registry instead of adding new name/prose gates.
 */
final class ExplicitRoomTypeRequirementAuthority {
    static final String VERSION = "explicit-room-purpose-0.9.10jc";
    static final String CRECHE_ID = "civic_creche";
    static final int CRECHE_MIN_WIDTH = 6;
    static final int CRECHE_MIN_HEIGHT = 6;
    static final int CRECHE_MIN_REACHABLE_INTERIOR = 12;
    static final int CRECHE_REQUIRED_CHILD_BED_UNITS = 3;
    static final String BARRACKS_ID = "security_barracks";
    static final int BARRACKS_MIN_WIDTH = 6;
    static final int BARRACKS_MIN_HEIGHT = 4;
    static final int BARRACKS_MIN_REACHABLE_INTERIOR = 6;
    static final int BARRACKS_REQUIRED_DUTY_BERTH_UNITS = 4;

    enum Capability {
        SECURE_FOOD_STORAGE,
        POTABLE_WATER_STORAGE,
        CHILD_BERTH,
        TEACHING_STATION,
        DUTY_BERTH,
        DUTY_EQUIPMENT_STORAGE,
        MUSTER_ANCHOR
    }

    enum EvidenceSource {
        ROOM_WIDTH,
        ROOM_HEIGHT,
        REACHABLE_INTERIOR,
        REACHABLE_ENTRANCE,
        SEMANTIC_CAPABILITY,
        ASSIGNED_CARE_PROVIDER,
        ASSIGNED_DUTY_STAFF,
        LIVE_CONTROL_ALIGNMENT,
        SEVERE_ROOM_HAZARD,
        CONFLICTING_MACHINERY
    }

    enum Status {
        UNDESIGNATED,
        PLANNED_BLOCKED,
        STRUCTURALLY_READY,
        UNSTAFFED,
        CONTROL_BLOCKED,
        OPERATING
    }

    record Requirement(String id, String label, EvidenceSource source,
                       int minimum, int maximum, Set<Capability> anyCapability,
                       boolean physicalQualification) {
        Requirement {
            id = safeId(id);
            label = clean(label, id);
            source = source == null ? EvidenceSource.SEMANTIC_CAPABILITY : source;
            minimum = Math.max(0, minimum);
            maximum = maximum < 0 ? -1 : Math.max(minimum, maximum);
            anyCapability = anyCapability == null ? Set.of() : Set.copyOf(anyCapability);
        }

        boolean satisfiedBy(int observed) {
            return observed >= minimum && (maximum < 0 || observed <= maximum);
        }
    }

    record RoomTypeDefinition(String id, String label, String category,
                              Set<ConstructionGovernanceAuthority.RoomRole> roles,
                              Set<ConstructionGovernanceAuthority.UtilityTag> utilities,
                              Set<ConstructionGovernanceAuthority.ValidationTag> validation,
                              List<Requirement> requirements,
                              String enabledUse, String blueprintStatus) {
        RoomTypeDefinition {
            id = safeId(id);
            label = clean(label, id);
            category = clean(category, "room purpose");
            roles = roles == null ? Set.of() : Set.copyOf(roles);
            utilities = utilities == null ? Set.of() : Set.copyOf(utilities);
            validation = validation == null ? Set.of() : Set.copyOf(validation);
            requirements = requirements == null ? List.of() : List.copyOf(requirements);
            enabledUse = clean(enabledUse, "no runtime use registered");
            blueprintStatus = clean(blueprintStatus, "unmapped");
        }
    }

    record RequirementResult(String requirementId, String label, int observed,
                             int minimum, int maximum, boolean satisfied,
                             boolean physicalQualification, List<String> witnesses,
                             String line) {
        RequirementResult {
            witnesses = witnesses == null ? List.of() : List.copyOf(witnesses);
            line = clean(line, label);
        }
    }

    record Assessment(RoomTypeDefinition definition, int roomId, boolean declared,
                      String declarationSource, Status status,
                      boolean physicallyQualified, boolean operating,
                      int width, int height, int footprintArea,
                      int reachableInteriorCells, int entrances,
                      int assignedCareProviders, int childBedUnits,
                      int childCapacity, Faction liveController,
                      List<RequirementResult> results, List<String> blockers,
                      List<String> witnesses, String line) {
        Assessment {
            declarationSource = clean(declarationSource, "none");
            status = status == null ? Status.UNDESIGNATED : status;
            liveController = liveController == null ? Faction.NONE : liveController;
            results = results == null ? List.of() : List.copyOf(results);
            blockers = blockers == null ? List.of() : List.copyOf(blockers);
            witnesses = witnesses == null ? List.of() : List.copyOf(witnesses);
            line = clean(line, "Room purpose assessment unavailable.");
        }

        int observed(String requirementId) {
            for (RequirementResult result : results) {
                if (result != null && Objects.equals(result.requirementId(), requirementId)) {
                    return result.observed();
                }
            }
            return 0;
        }

        String requirementSummary() {
            ArrayList<String> lines = new ArrayList<>();
            for (RequirementResult result : results) {
                if (result != null) lines.add((result.satisfied() ? "PASS " : "FAIL ") + result.line());
            }
            return String.join("; ", lines);
        }

        String witnessSummary() {
            return witnesses.isEmpty() ? "none" : String.join(", ", witnesses);
        }

        int assignedDutyStaff() {
            return observed("barracks.duty-staff");
        }

        int dutyBerthUnits() {
            return observed("barracks.duty-berths");
        }

        int dutyCapacity() {
            return dutyBerthUnits();
        }

        int assignedStaff() {
            if (definition == null) return 0;
            if (CRECHE_ID.equals(definition.id())) return assignedCareProviders;
            if (BARRACKS_ID.equals(definition.id())) return assignedDutyStaff();
            return 0;
        }

        int capacityUnits() {
            if (definition == null) return 0;
            if (CRECHE_ID.equals(definition.id())) return childBedUnits;
            if (BARRACKS_ID.equals(definition.id())) return dutyBerthUnits();
            return 0;
        }

        int operatingCapacity() {
            if (!operating || definition == null) return 0;
            if (CRECHE_ID.equals(definition.id())) return childCapacity;
            if (BARRACKS_ID.equals(definition.id())) return dutyCapacity();
            return 0;
        }

        String capacityUnitLabel() {
            if (definition == null) return "units";
            if (CRECHE_ID.equals(definition.id())) return "children";
            if (BARRACKS_ID.equals(definition.id())) return "duty personnel";
            return "units";
        }
    }

    private record FixtureSpec(Capability capability, int units, String type,
                               char glyph, String label) { }

    private static final class LayoutSnapshot {
        final Rectangle room;
        final boolean[][] reachable;
        final ArrayList<Point> reachablePoints;
        final int entrances;

        LayoutSnapshot(Rectangle room, boolean[][] reachable,
                       ArrayList<Point> reachablePoints, int entrances) {
            this.room = room;
            this.reachable = reachable;
            this.reachablePoints = reachablePoints;
            this.entrances = entrances;
        }
    }

    private static final RoomTypeDefinition CRECHE = new RoomTypeDefinition(
            CRECHE_ID,
            "Civic Creche",
            "child care and generational continuity",
            EnumSet.of(ConstructionGovernanceAuthority.RoomRole.HAB,
                    ConstructionGovernanceAuthority.RoomRole.CIVIC),
            EnumSet.of(ConstructionGovernanceAuthority.UtilityTag.WATER,
                    ConstructionGovernanceAuthority.UtilityTag.STORAGE_ACCESS,
                    ConstructionGovernanceAuthority.UtilityTag.STAFFING_ACCESS),
            EnumSet.of(ConstructionGovernanceAuthority.ValidationTag.PASSABILITY,
                    ConstructionGovernanceAuthority.ValidationTag.DOORWAY,
                    ConstructionGovernanceAuthority.ValidationTag.CLEARANCE,
                    ConstructionGovernanceAuthority.ValidationTag.UTILITY,
                    ConstructionGovernanceAuthority.ValidationTag.HAZARD,
                    ConstructionGovernanceAuthority.ValidationTag.OWNERSHIP,
                    ConstructionGovernanceAuthority.ValidationTag.STAFFING,
                    ConstructionGovernanceAuthority.ValidationTag.SAVE_LOAD),
            List.of(
                    requirement("creche.width", "room width", EvidenceSource.ROOM_WIDTH,
                            CRECHE_MIN_WIDTH, -1, true),
                    requirement("creche.height", "room height", EvidenceSource.ROOM_HEIGHT,
                            CRECHE_MIN_HEIGHT, -1, true),
                    requirement("creche.reachable-interior", "reachable interior cells",
                            EvidenceSource.REACHABLE_INTERIOR, CRECHE_MIN_REACHABLE_INTERIOR, -1, true),
                    requirement("creche.entrance", "reachable entrance",
                            EvidenceSource.REACHABLE_ENTRANCE, 1, -1, true),
                    capabilityRequirement("creche.food-storage", "secure food storage", 1,
                            Capability.SECURE_FOOD_STORAGE),
                    capabilityRequirement("creche.water-storage", "potable water storage", 1,
                            Capability.POTABLE_WATER_STORAGE),
                    capabilityRequirement("creche.child-beds", "child bed units",
                            CRECHE_REQUIRED_CHILD_BED_UNITS, Capability.CHILD_BERTH),
                    capabilityRequirement("creche.teaching", "teaching station", 1,
                            Capability.TEACHING_STATION),
                    requirement("creche.hazard-conflict", "severe room hazards",
                            EvidenceSource.SEVERE_ROOM_HAZARD, 0, 0, true),
                    requirement("creche.machine-conflict", "hazardous or industrial machinery",
                            EvidenceSource.CONFLICTING_MACHINERY, 0, 0, true),
                    requirement("creche.care-provider", "assigned care provider",
                            EvidenceSource.ASSIGNED_CARE_PROVIDER, 1, -1, false),
                    requirement("creche.control", "live faction control alignment",
                            EvidenceSource.LIVE_CONTROL_ALIGNMENT, 1, -1, false)
            ),
            "faction happiness, newborn/ward cohort intake, child-care capacity, and mature cohort muster source",
            "unmapped: no whole-creche player construction blueprint is registered"
    );

    private static final RoomTypeDefinition BARRACKS = new RoomTypeDefinition(
            BARRACKS_ID,
            "Security Barracks",
            "Guard, PDF, and Civic Wardens duty housing and muster",
            EnumSet.of(ConstructionGovernanceAuthority.RoomRole.HAB,
                    ConstructionGovernanceAuthority.RoomRole.SECURITY),
            EnumSet.of(ConstructionGovernanceAuthority.UtilityTag.LIGHT,
                    ConstructionGovernanceAuthority.UtilityTag.STORAGE_ACCESS,
                    ConstructionGovernanceAuthority.UtilityTag.STAFFING_ACCESS),
            EnumSet.of(ConstructionGovernanceAuthority.ValidationTag.PASSABILITY,
                    ConstructionGovernanceAuthority.ValidationTag.DOORWAY,
                    ConstructionGovernanceAuthority.ValidationTag.CLEARANCE,
                    ConstructionGovernanceAuthority.ValidationTag.HAZARD,
                    ConstructionGovernanceAuthority.ValidationTag.OWNERSHIP,
                    ConstructionGovernanceAuthority.ValidationTag.STAFFING,
                    ConstructionGovernanceAuthority.ValidationTag.SAVE_LOAD),
            List.of(
                    requirement("barracks.width", "room width", EvidenceSource.ROOM_WIDTH,
                            BARRACKS_MIN_WIDTH, -1, true),
                    requirement("barracks.height", "room height", EvidenceSource.ROOM_HEIGHT,
                            BARRACKS_MIN_HEIGHT, -1, true),
                    requirement("barracks.reachable-interior", "reachable interior cells",
                            EvidenceSource.REACHABLE_INTERIOR, BARRACKS_MIN_REACHABLE_INTERIOR, -1, true),
                    requirement("barracks.entrance", "reachable entrance",
                            EvidenceSource.REACHABLE_ENTRANCE, 1, -1, true),
                    capabilityRequirement("barracks.duty-berths", "reachable duty berth capacity",
                            BARRACKS_REQUIRED_DUTY_BERTH_UNITS, Capability.DUTY_BERTH),
                    capabilityRequirement("barracks.equipment-store", "duty equipment store", 1,
                            Capability.DUTY_EQUIPMENT_STORAGE),
                    capabilityRequirement("barracks.muster-anchor", "muster anchor", 1,
                            Capability.MUSTER_ANCHOR),
                    requirement("barracks.hazard-conflict", "severe room hazards",
                            EvidenceSource.SEVERE_ROOM_HAZARD, 0, 0, true),
                    requirement("barracks.machine-conflict", "laboratory or forge machinery",
                            EvidenceSource.CONFLICTING_MACHINERY, 0, 0, true),
                    requirement("barracks.duty-staff", "assigned duty staff",
                            EvidenceSource.ASSIGNED_DUTY_STAFF, 1, -1, false),
                    requirement("barracks.control", "live faction control alignment",
                            EvidenceSource.LIVE_CONTROL_ALIGNMENT, 1, -1, false)
            ),
            "barracks reserve muster eligibility, capacity, and room-state readback",
            "partial: Guard Barracks construction supplies an anchor, not a whole qualifying room"
    );

    private static final List<RoomTypeDefinition> DEFINITIONS = List.of(CRECHE, BARRACKS);

    private ExplicitRoomTypeRequirementAuthority() { }

    static List<RoomTypeDefinition> definitions() { return DEFINITIONS; }

    static RoomTypeDefinition crecheDefinition() { return CRECHE; }

    static RoomTypeDefinition barracksDefinition() { return BARRACKS; }

    static RoomTypeDefinition definition(String id) {
        if (id == null) return null;
        for (RoomTypeDefinition definition : DEFINITIONS) {
            if (definition.id().equalsIgnoreCase(id.trim())) return definition;
        }
        return null;
    }

    static String purposeIdForStamp(String kind, String name, String descriptor) {
        String normalizedKind = normalize(kind);
        if (normalizedKind.contains("daycare") || normalizedKind.contains("creche")) return CRECHE_ID;
        if (normalizedKind.equals("barracks")) return BARRACKS_ID;
        return inferDeclaredPurposeId(name, descriptor);
    }

    static String inferDeclaredPurposeId(String... values) {
        String text = normalize(values);
        if (contains(text, "creche", "daycare", "child care", "child-care", "childcare")) {
            return CRECHE_ID;
        }
        if (text.contains("nursery")
                && !contains(text, "plant nursery", "cloning nursery", "gene nursery", "agricultural nursery",
                "hydroponic nursery", "fungal nursery", "animal nursery")) {
            return CRECHE_ID;
        }
        if (contains(text, "guard barracks", "guard billet barracks", "pdf duty barracks",
                "civic wardens duty barracks", "wardens duty barracks")) {
            return BARRACKS_ID;
        }
        return "";
    }

    static String declaredPurposeId(World world, int roomId, RoomPopulationLedger ledger) {
        if (ledger != null && definition(ledger.declaredRoomPurposeId) != null) {
            return ledger.declaredRoomPurposeId;
        }
        RoomProfile profile = profile(world, roomId);
        if (profile != null) {
            if (definition(profile.declaredPurposeId) != null) return profile.declaredPurposeId;
            return inferDeclaredPurposeId(profile.name, profile.descriptor, profile.featureText);
        }
        if (ledger != null) {
            return inferDeclaredPurposeId(ledger.roomName, ledger.facilityPurpose,
                    ledger.facilityHistoricNote);
        }
        return "";
    }

    static RoomPopulationLedger ledgerForRoom(World world, int roomId, String purposeId) {
        if (world == null || world.roomPopulationLedgers == null) return null;
        RoomPopulationLedger fallback = null;
        Faction owner = world.roomFaction(roomId);
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (ledger == null || ledger.roomId != roomId) continue;
            String declared = declaredPurposeId(world, roomId, ledger);
            if (purposeId != null && !purposeId.equals(declared)) continue;
            if (fallback == null) fallback = ledger;
            if (FactionIdentityAuthority.sameFamily(owner, ledger.faction)) return ledger;
        }
        return fallback;
    }

    static Assessment assess(World world, RoomPopulationLedger ledger) {
        int roomId = ledger == null ? -1 : ledger.roomId;
        return assess(world, roomId, ledger);
    }

    static Assessment assessRoom(World world, int roomId) {
        String declared = declaredPurposeId(world, roomId, null);
        RoomPopulationLedger ledger = ledgerForRoom(world, roomId, declared);
        return assess(world, roomId, ledger);
    }

    private static Assessment assess(World world, int roomId, RoomPopulationLedger ledger) {
        String declaredId = declaredPurposeId(world, roomId, ledger);
        RoomTypeDefinition definition = definition(declaredId);
        Rectangle room = world == null ? null : world.roomRect(roomId);
        int width = room == null ? 0 : Math.max(0, room.width);
        int height = room == null ? 0 : Math.max(0, room.height);
        int area = width * height;
        Faction liveController = world == null ? Faction.NONE : world.roomFaction(roomId);
        if (definition == null) {
            return new Assessment(null, roomId, false, "none", Status.UNDESIGNATED,
                    false, false, width, height, area, 0, 0, 0, 0, 0,
                    liveController, List.of(), List.of(), List.of(),
                    "No explicit room purpose is declared for room " + roomId + ".");
        }

        String declarationSource = declarationSource(world, roomId, ledger);
        LayoutSnapshot layout = layoutSnapshot(world, roomId);
        ArrayList<RequirementResult> results = new ArrayList<>();
        ArrayList<String> blockers = new ArrayList<>();
        LinkedHashSet<String> witnesses = new LinkedHashSet<>();

        for (Requirement requirement : definition.requirements()) {
            Evidence evidence = observe(world, roomId, ledger, liveController, layout,
                    definition, requirement);
            boolean satisfied = requirement.satisfiedBy(evidence.count);
            String line = resultLine(requirement, evidence.count);
            RequirementResult result = new RequirementResult(requirement.id(), requirement.label(),
                    evidence.count, requirement.minimum(), requirement.maximum(), satisfied,
                    requirement.physicalQualification(), evidence.witnesses, line);
            results.add(result);
            witnesses.addAll(evidence.witnesses);
            if (!satisfied) blockers.add(line);
        }

        boolean physical = true;
        boolean all = true;
        for (RequirementResult result : results) {
            if (!result.satisfied()) {
                all = false;
                if (result.physicalQualification()) physical = false;
            }
        }
        boolean creche = CRECHE_ID.equals(definition.id());
        int providers = creche ? observed(results, "creche.care-provider") : 0;
        int bedUnits = creche ? observed(results, "creche.child-beds") : 0;
        int assignedStaff = creche ? providers : observed(results, "barracks.duty-staff");
        int rawCapacityUnits = creche ? bedUnits : observed(results, "barracks.duty-berths");
        int capacity = creche
                ? Math.min(Math.max(0, providers) * 12, Math.max(0, bedUnits) * 4)
                : Math.max(0, rawCapacityUnits);
        String controlRequirement = creche ? "creche.control" : "barracks.control";
        Status status;
        if (!physical) status = Status.PLANNED_BLOCKED;
        else if (assignedStaff < 1) status = Status.UNSTAFFED;
        else if (observed(results, controlRequirement) < 1) status = Status.CONTROL_BLOCKED;
        else if (all) status = Status.OPERATING;
        else status = Status.STRUCTURALLY_READY;
        boolean operating = status == Status.OPERATING;
        String line;
        if (!operating) {
            line = "Planned " + definition.label() + " " + status + ": "
                    + String.join(", ", blockers) + ".";
        } else if (creche) {
            line = "Operating " + definition.label() + ": room " + roomId + " is " + width + "x" + height
                    + " with " + layout.reachablePoints.size() + " reachable interior cells, " + layout.entrances
                    + " entrance" + plural(layout.entrances) + ", " + providers + " assigned care provider"
                    + plural(providers) + ", " + bedUnits + " child bed units, and capacity " + capacity + ".";
        } else {
            line = "Operating " + definition.label() + ": room " + roomId + " is " + width + "x" + height
                    + " with " + layout.reachablePoints.size() + " reachable interior cells, " + layout.entrances
                    + " entrance" + plural(layout.entrances) + ", " + assignedStaff + " assigned duty staff, "
                    + rawCapacityUnits + " reachable duty berths, and muster capacity " + capacity + ".";
        }
        return new Assessment(definition, roomId, true, declarationSource, status,
                physical, operating, width, height, area, layout.reachablePoints.size(),
                layout.entrances, providers, bedUnits, capacity, liveController,
                results, blockers, List.copyOf(witnesses), line);
    }

    private record Evidence(int count, List<String> witnesses) {
        Evidence { witnesses = witnesses == null ? List.of() : List.copyOf(witnesses); }
    }

    private static Evidence observe(World world, int roomId, RoomPopulationLedger ledger,
                                    Faction liveController, LayoutSnapshot layout,
                                    RoomTypeDefinition definition, Requirement requirement) {
        String purposeId = definition == null ? "" : definition.id();
        return switch (requirement.source()) {
            case ROOM_WIDTH -> new Evidence(layout.room == null ? 0 : layout.room.width, List.of());
            case ROOM_HEIGHT -> new Evidence(layout.room == null ? 0 : layout.room.height, List.of());
            case REACHABLE_INTERIOR -> new Evidence(layout.reachablePoints.size(), List.of());
            case REACHABLE_ENTRANCE -> new Evidence(layout.entrances, List.of());
            case SEMANTIC_CAPABILITY -> capabilityEvidence(world, roomId, purposeId,
                    layout, requirement.anyCapability());
            case ASSIGNED_CARE_PROVIDER -> careProviderEvidence(world, roomId, ledger,
                    liveController);
            case ASSIGNED_DUTY_STAFF -> dutyStaffEvidence(world, roomId, ledger,
                    liveController);
            case LIVE_CONTROL_ALIGNMENT -> controlEvidence(ledger, liveController);
            case SEVERE_ROOM_HAZARD -> hazardEvidence(world, roomId);
            case CONFLICTING_MACHINERY -> machineryConflictEvidence(world, roomId, purposeId);
        };
    }

    private static Evidence capabilityEvidence(World world, int roomId, String purposeId,
                                                 LayoutSnapshot layout, Set<Capability> accepted) {
        if (world == null || world.mapObjects == null || accepted == null || accepted.isEmpty()) {
            return new Evidence(0, List.of());
        }
        int count = 0;
        ArrayList<String> witnesses = new ArrayList<>();
        for (MapObjectState object : world.mapObjects) {
            if (!isPurposeFixture(world, roomId, purposeId, object)
                    || !fixtureReachable(layout, object)) continue;
            Capability capability = capability(object);
            if (capability == null || !accepted.contains(capability)) continue;
            int units = fixtureUnits(object);
            count += units;
            witnesses.add(safeObjectId(object) + "@" + object.x + "," + object.y + " x" + units);
        }
        return new Evidence(count, witnesses);
    }

    private static Evidence careProviderEvidence(World world, int roomId,
                                                  RoomPopulationLedger ledger,
                                                  Faction liveController) {
        if (world == null || world.npcs == null) return new Evidence(0, List.of());
        int count = 0;
        ArrayList<String> witnesses = new ArrayList<>();
        for (NpcEntity npc : world.npcs) {
            if (!isCareProvider(npc) || npc.hp <= 0 || npc.isAnimalActor()) continue;
            if (FactionIdentityAuthority.aligned(liveController)
                    && !FactionIdentityAuthority.sameFamily(npc.faction, liveController)) continue;
            boolean assigned = ledger != null && npc.provenance != null
                    && Objects.equals(ledger.id, npc.provenance.originSiteId);
            boolean homeLinked = world.inBounds(npc.homeX, npc.homeY)
                    && world.roomIdAt(npc.homeX, npc.homeY) == roomId;
            // Presence alone is not an assignment: a visitor walking through the
            // room must not silently become its care provider.
            if (!assigned && !homeLinked) continue;
            count++;
            witnesses.add(clean(npc.id, clean(npc.name, "care-provider")) + " / "
                    + clean(npc.name, "care provider"));
        }
        return new Evidence(count, witnesses);
    }

    private static Evidence dutyStaffEvidence(World world, int roomId,
                                               RoomPopulationLedger ledger,
                                               Faction liveController) {
        if (world == null || world.npcs == null) return new Evidence(0, List.of());
        int count = 0;
        ArrayList<String> witnesses = new ArrayList<>();
        for (NpcEntity npc : world.npcs) {
            if (!isDutyStaff(npc) || npc.hp <= 0 || npc.isAnimalActor()
                    || npc.isMinorActor() || npc.isUntargetableAnchor()) continue;
            if (ledger != null && FactionIdentityAuthority.aligned(ledger.faction)
                    && !FactionIdentityAuthority.sameFamily(npc.faction, ledger.faction)) continue;
            if (FactionIdentityAuthority.aligned(liveController)
                    && !FactionIdentityAuthority.sameFamily(npc.faction, liveController)) continue;
            boolean assigned = ledger != null && npc.provenance != null
                    && Objects.equals(ledger.id, npc.provenance.originSiteId);
            boolean homeLinked = world.inBounds(npc.homeX, npc.homeY)
                    && world.roomIdAt(npc.homeX, npc.homeY) == roomId;
            if (!assigned && !homeLinked) continue;
            count++;
            witnesses.add(clean(npc.id, clean(npc.name, "duty-staff")) + " / "
                    + clean(npc.name, "duty staff"));
        }
        return new Evidence(count, witnesses);
    }

    private static Evidence controlEvidence(RoomPopulationLedger ledger, Faction liveController) {
        if (ledger == null || !FactionIdentityAuthority.aligned(ledger.faction)
                || !FactionIdentityAuthority.aligned(liveController)
                || !FactionIdentityAuthority.sameFamily(ledger.faction, liveController)) {
            return new Evidence(0, List.of());
        }
        return new Evidence(1, List.of("controller=" + liveController.name()));
    }

    private static Evidence hazardEvidence(World world, int roomId) {
        if (world == null || world.hazardWarnings == null) return new Evidence(0, List.of());
        int count = 0;
        ArrayList<String> witnesses = new ArrayList<>();
        for (EnvironmentalHazardRecord hazard : world.hazardWarnings) {
            if (hazard == null || hazard.roomId != roomId || hazard.severity < 30) continue;
            count++;
            witnesses.add(clean(hazard.id, "hazard") + " / " + clean(hazard.label, hazard.family));
        }
        return new Evidence(count, witnesses);
    }

    private static Evidence machineryConflictEvidence(World world, int roomId, String purposeId) {
        if (world == null || world.mapObjects == null) return new Evidence(0, List.of());
        int count = 0;
        ArrayList<String> witnesses = new ArrayList<>();
        for (MapObjectState object : world.mapObjects) {
            if (object == null || !world.inBounds(object.x, object.y)
                    || world.roomIdAt(object.x, object.y) != roomId
                    || isPurposeFixture(world, roomId, purposeId, object)) {
                continue;
            }
            String type = AssetIntegrationDisciplineAuthority.canonicalType(object.type);
            boolean conflict = IndustrialForgeFixtureAuthority.isFamilyType(type)
                    || LabChemicalFixtureAuthority.isFamilyType(type)
                    || "martian-emergency-machine".equals(type)
                    || "planted-explosive".equals(type)
                    || "thrown-explosive".equals(type);
            if (!conflict) continue;
            count++;
            witnesses.add(safeObjectId(object) + " / " + clean(object.label, type));
        }
        return new Evidence(count, witnesses);
    }

    static int materializeGeneratedFixtures(World world) {
        if (world == null || world.rooms == null) return 0;
        int placed = 0;
        for (int roomId = 0; roomId < world.rooms.size(); roomId++) {
            String purposeId = declaredPurposeId(world, roomId, null);
            if (CRECHE_ID.equals(purposeId)) {
                placed += installCrecheFixtures(world, roomId);
            } else if (BARRACKS_ID.equals(purposeId)) {
                placed += installBarracksFixtures(world, roomId);
            }
        }
        return placed;
    }

    static int installCrecheFixtures(World world, int roomId) {
        if (world == null || !CRECHE_ID.equals(declaredPurposeId(world, roomId, null))) return 0;
        List<FixtureSpec> specs = List.of(
                new FixtureSpec(Capability.SECURE_FOOD_STORAGE, 1,
                        AssetIntegrationDisciplineAuthority.DOMESTIC_REFRIGERATOR_FIXTURE,
                        DomesticHabFixtureAuthority.glyphForType(
                                AssetIntegrationDisciplineAuthority.DOMESTIC_REFRIGERATOR_FIXTURE),
                        "Locked creche food-storage cabinet"),
                new FixtureSpec(Capability.POTABLE_WATER_STORAGE, 1,
                        AssetIntegrationDisciplineAuthority.DOMESTIC_WATER_STORAGE_FIXTURE,
                        DomesticHabFixtureAuthority.glyphForType(
                                AssetIntegrationDisciplineAuthority.DOMESTIC_WATER_STORAGE_FIXTURE),
                        "Clean creche water-storage tank"),
                new FixtureSpec(Capability.CHILD_BERTH, CRECHE_REQUIRED_CHILD_BED_UNITS,
                        AssetIntegrationDisciplineAuthority.BUNK_BED_FIXTURE,
                        DomesticHabFixtureAuthority.glyphForType(
                                AssetIntegrationDisciplineAuthority.BUNK_BED_FIXTURE),
                        "Stacked creche child-berth bank (12 places)"),
                new FixtureSpec(Capability.TEACHING_STATION, 1,
                        RoomFixtureInteractionAuthority.CIVIC_FIXTURE,
                        RoomFixtureInteractionAuthority.glyphFor(RoomFixtureInteractionAuthority.CIVIC_FIXTURE),
                        "Creche teaching station")
        );
        return installPurposeFixtures(world, roomId, CRECHE_ID, specs);
    }

    static int installBarracksFixtures(World world, int roomId) {
        if (world == null || !BARRACKS_ID.equals(declaredPurposeId(world, roomId, null))) return 0;
        List<FixtureSpec> specs = List.of(
                new FixtureSpec(Capability.DUTY_BERTH, BARRACKS_REQUIRED_DUTY_BERTH_UNITS,
                        AssetIntegrationDisciplineAuthority.GUARD_COT_FIXTURE,
                        DomesticHabFixtureAuthority.glyphForType(
                                AssetIntegrationDisciplineAuthority.GUARD_COT_FIXTURE),
                        "Barracks duty-berth bank (four places)"),
                new FixtureSpec(Capability.DUTY_EQUIPMENT_STORAGE, 1,
                        AssetIntegrationDisciplineAuthority.GUARD_SUPPLY_POST_FIXTURE,
                        GuardPdfDefenseFixtureAuthority.glyphForType(
                                AssetIntegrationDisciplineAuthority.GUARD_SUPPLY_POST_FIXTURE),
                        "Secured barracks duty-equipment store"),
                new FixtureSpec(Capability.MUSTER_ANCHOR, 1,
                        AssetIntegrationDisciplineAuthority.GUARD_BARRACKS_ANCHOR_FIXTURE,
                        GuardPdfDefenseFixtureAuthority.glyphForType(
                                AssetIntegrationDisciplineAuthority.GUARD_BARRACKS_ANCHOR_FIXTURE),
                        "Barracks muster anchor")
        );
        return installPurposeFixtures(world, roomId, BARRACKS_ID, specs);
    }

    private static int installPurposeFixtures(World world, int roomId, String purposeId,
                                              List<FixtureSpec> specs) {
        RoomProfile profile = profile(world, roomId);
        LayoutSnapshot layout = layoutSnapshot(world, roomId);
        ArrayList<Point> candidates = new ArrayList<>();
        for (Point point : layout.reachablePoints) {
            if (point == null || !strictInterior(layout.room, point.x, point.y)) continue;
            if (world.mapObjectAt(point.x, point.y) != null || world.npcAt(point.x, point.y) != null) continue;
            if (world.isDoorAccessReservedForObject(point.x, point.y)) continue;
            candidates.add(point);
        }
        candidates.sort(Comparator.comparingInt((Point p) -> p.y).thenComparingInt(p -> p.x));

        int placed = 0;
        int spread = 0;
        for (FixtureSpec spec : specs) {
            if (capabilityFixtureInRoom(world, roomId, purposeId,
                    spec.capability(), layout, true) != null) continue;
            MapObjectState existing = capabilityFixtureInRoom(
                    world, roomId, purposeId, spec.capability(), layout, false);
            Point point = takeSpreadPoint(candidates, spread++);
            if (point == null) continue;
            if (existing != null) {
                relocatePurposeFixture(world, existing, point);
                placed++;
                continue;
            }
            char under = world.tiles[point.x][point.y];
            String stock = fixtureStock(spec, purposeId, profile, world, roomId, under);
            String roomLabel = profile == null ? "room " + roomId : clean(profile.name, "room " + roomId);
            String zoneLabel = world.zoneType == null ? "Unknown zone" : world.zoneType.label;
            MapObjectState object = RoomFixtureInteractionAuthority.roomFixture(point.x, point.y,
                    spec.type(), spec.label() + " / " + roomLabel + " / " + zoneLabel,
                    stock, spec.glyph());
            object.id = "ROOM-PURPOSE-" + world.locationKey() + "-" + roomId + "-"
                    + spec.capability().name();
            world.tiles[point.x][point.y] = object.glyph;
            world.mapObjects.add(object);
            placed++;
        }
        return placed;
    }

    static int ensureGeneratedCareProviders(World world, Random random) {
        if (world == null || world.roomPopulationLedgers == null) return 0;
        Random rng = random == null ? new Random(world.seed ^ 0xC2EC4EL) : random;
        int createdOrAssigned = 0;
        LinkedHashSet<Integer> handledRooms = new LinkedHashSet<>();
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (ledger == null || !CRECHE_ID.equals(declaredPurposeId(world, ledger.roomId, ledger))
                    || handledRooms.contains(ledger.roomId)) continue;
            Faction owner = world.roomFaction(ledger.roomId);
            if (!FactionIdentityAuthority.aligned(owner)
                    || !FactionIdentityAuthority.sameFamily(owner, ledger.faction)) continue;
            handledRooms.add(ledger.roomId);
            // Generated staffing is an operational completion step, not a way
            // to make an unsafe or physically incomplete declaration look real.
            if (!assess(world, ledger).physicallyQualified()) continue;
            if (careProviderEvidence(world, ledger.roomId, ledger, owner).count > 0) continue;

            NpcEntity provider = residentCandidate(world, ledger.roomId, owner);
            if (provider == null) {
                Point point = staffPoint(world, ledger.roomId);
                if (point == null) continue;
                provider = PersonnelPopulationApi.createResidentFromRoom(world, ledger.roomId,
                        owner, point.x, point.y, rng);
                world.npcs.add(provider);
            }
            provider.role = "Creche Care Provider";
            provider.state = "Creche Duty";
            provider.homeX = provider.x;
            provider.homeY = provider.y;
            if (provider.provenance == null
                    || !Objects.equals(ledger.id, provider.provenance.originSiteId)) {
                PersonnelPopulationApi.attachProvenance(provider, world, ledger.roomId, ledger,
                        "assigned creche care provider", rng);
            }
            createdOrAssigned++;
        }
        synchronizeCompatibilityCounters(world);
        return createdOrAssigned;
    }

    static int ensureGeneratedBarracksDutyStaff(World world, Random random) {
        if (world == null || world.roomPopulationLedgers == null) return 0;
        Random rng = random == null ? new Random(world.seed ^ 0xBA22AC5L) : random;
        int createdOrAssigned = 0;
        LinkedHashSet<Integer> handledRooms = new LinkedHashSet<>();
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (ledger == null || !BARRACKS_ID.equals(declaredPurposeId(world, ledger.roomId, ledger))
                    || handledRooms.contains(ledger.roomId)) continue;
            Faction owner = world.roomFaction(ledger.roomId);
            if (!FactionIdentityAuthority.aligned(owner)
                    || !FactionIdentityAuthority.sameFamily(owner, ledger.faction)) continue;
            handledRooms.add(ledger.roomId);
            // Staffing completes an already safe, reachable room. It never
            // turns a blocked declaration into working military housing.
            if (!assess(world, ledger).physicallyQualified()) continue;
            if (dutyStaffEvidence(world, ledger.roomId, ledger, owner).count > 0) continue;

            NpcEntity dutyStaff = residentCandidate(world, ledger.roomId, owner);
            if (dutyStaff == null) {
                Point point = staffPoint(world, ledger.roomId);
                if (point == null) continue;
                dutyStaff = PersonnelPopulationApi.createResidentFromRoom(world, ledger.roomId,
                        owner, point.x, point.y, rng);
                world.npcs.add(dutyStaff);
            }
            dutyStaff.role = "Barracks Duty Guard";
            dutyStaff.state = "Barracks Duty";
            if (world.inBounds(dutyStaff.x, dutyStaff.y)
                    && world.roomIdAt(dutyStaff.x, dutyStaff.y) == ledger.roomId) {
                dutyStaff.homeX = dutyStaff.x;
                dutyStaff.homeY = dutyStaff.y;
            } else {
                Point home = staffPoint(world, ledger.roomId);
                if (home != null) {
                    dutyStaff.homeX = home.x;
                    dutyStaff.homeY = home.y;
                }
            }
            if (dutyStaff.provenance == null
                    || !Objects.equals(ledger.id, dutyStaff.provenance.originSiteId)) {
                PersonnelPopulationApi.attachProvenance(dutyStaff, world, ledger.roomId, ledger,
                        "assigned barracks duty staff", rng);
            }
            createdOrAssigned++;
        }
        return createdOrAssigned;
    }

    static void synchronizeCompatibilityCounters(World world) {
        if (world == null || world.roomPopulationLedgers == null) return;
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (ledger == null || !CRECHE_ID.equals(declaredPurposeId(world, ledger.roomId, ledger))) continue;
            Assessment assessment = assess(world, ledger);
            ledger.careProviders = assessment.observed("creche.care-provider");
            ledger.crecheFoodStorageUnits = assessment.observed("creche.food-storage");
            ledger.crecheWaterStorageUnits = assessment.observed("creche.water-storage");
            ledger.crecheBedUnits = assessment.observed("creche.child-beds");
            ledger.crecheTeachingStations = assessment.observed("creche.teaching");
        }
    }

    static String crecheInfopediaRequirementLine() {
        return "A declared Civic Creche operates only in a room at least " + CRECHE_MIN_WIDTH + "x"
                + CRECHE_MIN_HEIGHT + " with " + CRECHE_MIN_REACHABLE_INTERIOR
                + " reachable interior cells and a reachable entrance. Live room-local evidence must include secure food storage, potable water storage, "
                + CRECHE_REQUIRED_CHILD_BED_UNITS + " child bed units (twelve places), a teaching station, a living assigned care provider, aligned live faction control, and no severe hazard or industrial-machine conflict. A name, descriptor, glyph, or saved legacy counter cannot satisfy those requirements.";
    }

    static String barracksInfopediaRequirementLine() {
        return "A declared Security Barracks operates only in a room at least "
                + BARRACKS_MIN_WIDTH + "x" + BARRACKS_MIN_HEIGHT + " with "
                + BARRACKS_MIN_REACHABLE_INTERIOR
                + " reachable interior cells and a reachable entrance. Live room-local evidence must include "
                + BARRACKS_REQUIRED_DUTY_BERTH_UNITS
                + " reachable duty berths, a duty-equipment store, a muster anchor, living assigned same-faction-family duty staff, aligned live faction control, and no severe hazard or laboratory/forge machinery conflict. The exact BARRACKS stamp or a narrow Guard, PDF, or Civic Wardens duty-barracks declaration identifies the purpose; generic security, rail-intake, evidence-store, dormitory, or gang wording does not.";
    }

    static String definitionAuditLine() {
        return "Explicit room-purpose registry: version=" + VERSION + " definitions=" + DEFINITIONS.size()
                + " ids=" + DEFINITIONS.stream().map(RoomTypeDefinition::id).toList()
                + " crecheRequirements=" + CRECHE.requirements().size()
                + " barracksRequirements=" + BARRACKS.requirements().size()
                + " evidence=room geometry + reachable doors/cells + persisted semantic MapObjectState capabilities + named NPC assignments + live control + hazards; legacyCountersAuthoritative=false.";
    }

    private static Requirement requirement(String id, String label, EvidenceSource source,
                                           int minimum, int maximum, boolean physical) {
        return new Requirement(id, label, source, minimum, maximum, Set.of(), physical);
    }

    private static Requirement capabilityRequirement(String id, String label, int minimum,
                                                     Capability capability) {
        return new Requirement(id, label, EvidenceSource.SEMANTIC_CAPABILITY,
                minimum, -1, Set.of(capability), true);
    }

    private static int observed(List<RequirementResult> results, String id) {
        if (results == null) return 0;
        for (RequirementResult result : results) {
            if (result != null && Objects.equals(id, result.requirementId())) return result.observed();
        }
        return 0;
    }

    private static String resultLine(Requirement requirement, int observed) {
        if (requirement.maximum() == 0) {
            return requirement.label() + " " + observed + "/0 allowed";
        }
        return requirement.label() + " " + observed + "/" + requirement.minimum();
    }

    private static LayoutSnapshot layoutSnapshot(World world, int roomId) {
        Rectangle room = world == null ? null : world.roomRect(roomId);
        int ww = world == null ? 0 : world.w;
        int hh = world == null ? 0 : world.h;
        boolean[][] reachable = new boolean[Math.max(0, ww)][Math.max(0, hh)];
        ArrayList<Point> points = new ArrayList<>();
        if (world == null || room == null || world.tiles == null) {
            return new LayoutSnapshot(room, reachable, points, 0);
        }
        ArrayDeque<Point> queue = new ArrayDeque<>();
        int entrances = 0;
        for (int x = room.x; x < room.x + room.width; x++) {
            for (int y = room.y; y < room.y + room.height; y++) {
                if (!world.inBounds(x, y) || world.roomIdAt(x, y) != roomId
                        || !isBoundary(room, x, y) || !isEntranceGlyph(world, world.tiles[x][y])) continue;
                Point inside = inwardPoint(room, x, y);
                if (inside == null || !world.inBounds(inside.x, inside.y)
                        || world.roomIdAt(inside.x, inside.y) != roomId || !world.walkable(inside.x, inside.y)) {
                    continue;
                }
                entrances++;
                if (!reachable[inside.x][inside.y]) {
                    reachable[inside.x][inside.y] = true;
                    queue.add(inside);
                }
            }
        }
        int[][] directions = {{1,0},{-1,0},{0,1},{0,-1}};
        while (!queue.isEmpty()) {
            Point point = queue.removeFirst();
            points.add(point);
            for (int[] direction : directions) {
                int nx = point.x + direction[0], ny = point.y + direction[1];
                if (!world.inBounds(nx, ny) || reachable[nx][ny]
                        || world.roomIdAt(nx, ny) != roomId || !world.walkable(nx, ny)) continue;
                reachable[nx][ny] = true;
                queue.add(new Point(nx, ny));
            }
        }
        return new LayoutSnapshot(room, reachable, points, entrances);
    }

    private static boolean fixtureReachable(LayoutSnapshot layout, MapObjectState object) {
        if (layout == null || object == null || layout.reachable.length == 0
                || object.x < 0 || object.x >= layout.reachable.length
                || object.y < 0 || layout.reachable[object.x].length == 0
                || object.y >= layout.reachable[object.x].length) return false;
        if (layout.reachable[object.x][object.y]) return true;
        int[][] directions = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] direction : directions) {
            int nx = object.x + direction[0], ny = object.y + direction[1];
            if (nx >= 0 && nx < layout.reachable.length && ny >= 0
                    && ny < layout.reachable[nx].length && layout.reachable[nx][ny]) return true;
        }
        return false;
    }

    private static boolean isPurposeFixture(World world, int roomId, String purposeId,
                                            MapObjectState object) {
        return object != null && world != null && world.inBounds(object.x, object.y)
                && world.roomIdAt(object.x, object.y) == roomId
                && Objects.equals(purposeId,
                MapObjectState.stockValue(object.stockState, "roomPurpose"))
                && "true".equalsIgnoreCase(MapObjectState.stockValue(object.stockState,
                "requirementWitness"));
    }

    private static Capability capability(MapObjectState object) {
        if (object == null) return null;
        String value = MapObjectState.stockValue(object.stockState, "capability");
        try { return Capability.valueOf(value); }
        catch (RuntimeException ignored) { return null; }
    }

    private static int fixtureUnits(MapObjectState object) {
        if (object == null) return 0;
        try { return Math.max(0, Integer.parseInt(
                MapObjectState.stockValue(object.stockState, "units"))); }
        catch (RuntimeException ignored) { return 0; }
    }

    private static MapObjectState capabilityFixtureInRoom(World world, int roomId,
                                                           String purposeId, Capability capability,
                                                           LayoutSnapshot layout,
                                                           boolean requireReachable) {
        if (world == null || world.mapObjects == null) return null;
        for (MapObjectState object : world.mapObjects) {
            if (!isPurposeFixture(world, roomId, purposeId, object)
                    || capability(object) != capability) continue;
            if (!requireReachable || fixtureReachable(layout, object)) return object;
        }
        return null;
    }

    private static void relocatePurposeFixture(World world, MapObjectState object, Point target) {
        if (world == null || object == null || target == null || !world.inBounds(target.x, target.y)) return;
        char oldUnder = MapObjectState.underlyingTileFromStock(object.stockState);
        if (world.inBounds(object.x, object.y) && world.tiles[object.x][object.y] == object.glyph) {
            world.tiles[object.x][object.y] = world.fallbackUnderForObject(
                    object.x, object.y, oldUnder == 0 ? '.' : oldUnder);
        }
        char newUnder = world.tiles[target.x][target.y];
        object.x = target.x;
        object.y = target.y;
        object.stockState = MapObjectState.setStockFlag(
                object.stockState, "under", Integer.toString((int) newUnder));
        world.tiles[target.x][target.y] = object.glyph;
    }

    private static String fixtureStock(FixtureSpec spec, String purposeId,
                                       RoomProfile profile, World world, int roomId, char under) {
        String base;
        if (DomesticHabFixtureAuthority.isFamilyType(spec.type())) {
            base = DomesticHabFixtureAuthority.roomStock(spec.type(), profile, world, roomId, under);
        } else {
            base = RoomFixtureInteractionAuthority.stockFor(spec.type(), profile, world, roomId, under);
        }
        return base + ";roomPurpose=" + purposeId + ";capability=" + spec.capability().name()
                + ";units=" + spec.units() + ";requirementWitness=true";
    }

    private static Point takeSpreadPoint(ArrayList<Point> candidates, int salt) {
        if (candidates == null || candidates.isEmpty()) return null;
        int index = switch (Math.floorMod(salt, 4)) {
            case 0 -> 0;
            case 1 -> candidates.size() - 1;
            case 2 -> candidates.size() / 3;
            default -> (candidates.size() * 2) / 3;
        };
        index = Math.max(0, Math.min(candidates.size() - 1, index));
        return candidates.remove(index);
    }

    private static NpcEntity residentCandidate(World world, int roomId, Faction owner) {
        if (world == null || world.npcs == null) return null;
        for (NpcEntity npc : world.npcs) {
            if (npc == null || npc.hp <= 0 || npc.isAnimalActor() || npc.isFactionRepresentative()
                    || npc.isMinorActor() || !ordinaryResidentRole(npc.role)
                    || !FactionIdentityAuthority.sameFamily(npc.faction, owner)) continue;
            boolean inRoom = world.inBounds(npc.x, npc.y) && world.roomIdAt(npc.x, npc.y) == roomId;
            boolean homeRoom = world.inBounds(npc.homeX, npc.homeY)
                    && world.roomIdAt(npc.homeX, npc.homeY) == roomId;
            if (inRoom || homeRoom) return npc;
        }
        return null;
    }

    private static boolean ordinaryResidentRole(String role) {
        String normalized = normalize(role);
        return normalized.equals("resident") || normalized.equals("civilian")
                || normalized.equals("local");
    }

    private static Point staffPoint(World world, int roomId) {
        if (world == null) return null;
        LayoutSnapshot layout = layoutSnapshot(world, roomId);
        for (Point point : layout.reachablePoints) {
            if (point != null && strictInterior(layout.room, point.x, point.y)
                    && world.mapObjectAt(point.x, point.y) == null
                    && world.npcAt(point.x, point.y) == null) return point;
        }
        return null;
    }

    private static boolean isCareProvider(NpcEntity npc) {
        if (npc == null) return false;
        String role = normalize(npc.role, npc.state);
        return contains(role, "care provider", "child minder", "creche caregiver", "creche duty");
    }

    private static boolean isDutyStaff(NpcEntity npc) {
        if (npc == null) return false;
        String role = normalize(npc.role, npc.state);
        return contains(role, "barracks duty", "duty guard", "guard", "warden", "security",
                "patrol", "officer", "soldier", "trooper", "armsman");
    }

    private static String declarationSource(World world, int roomId, RoomPopulationLedger ledger) {
        if (ledger != null && definition(ledger.declaredRoomPurposeId) != null) return "population-ledger explicit purpose";
        RoomProfile profile = profile(world, roomId);
        if (profile != null && definition(profile.declaredPurposeId) != null) return "room-profile explicit purpose";
        return "legacy declaration inference";
    }

    private static RoomProfile profile(World world, int roomId) {
        if (world == null || roomId < 0 || roomId >= world.roomProfiles.size()) return null;
        return world.roomProfiles.get(roomId);
    }

    private static Point inwardPoint(Rectangle room, int x, int y) {
        if (room == null) return null;
        if (x == room.x) return new Point(x + 1, y);
        if (x == room.x + room.width - 1) return new Point(x - 1, y);
        if (y == room.y) return new Point(x, y + 1);
        if (y == room.y + room.height - 1) return new Point(x, y - 1);
        return null;
    }

    private static boolean isBoundary(Rectangle room, int x, int y) {
        return room != null && (x == room.x || y == room.y
                || x == room.x + room.width - 1 || y == room.y + room.height - 1);
    }

    private static boolean strictInterior(Rectangle room, int x, int y) {
        return room != null && x > room.x && y > room.y
                && x < room.x + room.width - 1 && y < room.y + room.height - 1;
    }

    private static boolean isEntranceGlyph(World world, char glyph) {
        return world != null && (world.isDoorSymbol(glyph)
                || glyph == 'D' || glyph == 'S' || glyph == 'v' || glyph == 'E');
    }

    private static boolean contains(String text, String... needles) {
        if (text == null) return false;
        for (String needle : needles) if (needle != null && !needle.isBlank() && text.contains(needle)) return true;
        return false;
    }

    private static String normalize(String... values) {
        StringBuilder out = new StringBuilder();
        if (values != null) {
            for (String value : values) if (value != null) out.append(value).append(' ');
        }
        String normalized = Normalizer.normalize(out, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized.toLowerCase(Locale.ROOT).trim();
    }

    private static String safeObjectId(MapObjectState object) {
        return clean(object == null ? null : object.id,
                clean(object == null ? null : object.type, "room-fixture"));
    }

    private static String safeId(String value) {
        String id = value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace(' ', '_');
        return id.isBlank() ? "room_purpose" : id;
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String plural(int count) { return count == 1 ? "" : "s"; }
}
