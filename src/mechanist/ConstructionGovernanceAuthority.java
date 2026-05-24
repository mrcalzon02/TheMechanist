package mechanist;

import java.util.*;

/**
 * 0.9.08t — Construction coherence / infrastructure governance foundation.
 *
 * Lightweight data authority for room metadata, blueprint validation,
 * utility tagging, placement checks, and AI-readable construction rules. This is
 * intentionally non-invasive: it records canonical validation categories and
 * common blueprint families without running full-map construction scans.
 */
final class ConstructionGovernanceAuthority {
    static final String VERSION = "0.9.09a";

    enum RoomRole {
        HAB,
        MEDICAE,
        LABORATORY,
        CHEMICAL,
        FORGE,
        STORAGE,
        CIVIC,
        ROAD_FRONTAGE,
        UTILITY,
        TEMPLE,
        SECURITY,
        AGRICULTURE,
        TUTORIAL_HELD,
        EDITOR_HELD
    }

    enum UtilityTag {
        NONE,
        POWER,
        WATER,
        WASTE,
        VENTILATION,
        EXHAUST,
        LIGHT,
        DATA,
        ROAD_ACCESS,
        STORAGE_ACCESS,
        STAFFING_ACCESS
    }

    enum ValidationTag {
        PASSABILITY,
        DOORWAY,
        CLEARANCE,
        UTILITY,
        ADJACENCY,
        ROOM_ROLE,
        HAZARD,
        OWNERSHIP,
        STAFFING,
        DECOMPOSITION,
        LOCALIZATION,
        SAVE_LOAD,
        MULTIPLAYER_HELD
    }

    static final class RoomGovernanceSpec {
        final RoomRole role;
        final String id;
        final String label;
        final EnumSet<UtilityTag> requiredUtilities;
        final EnumSet<ValidationTag> validationTags;
        final boolean supportsPlayerConstruction;
        final boolean supportsFactionConstruction;
        final String notes;

        RoomGovernanceSpec(RoomRole role, String id, String label, EnumSet<UtilityTag> requiredUtilities,
                           EnumSet<ValidationTag> validationTags, boolean supportsPlayerConstruction,
                           boolean supportsFactionConstruction, String notes) {
            this.role = role == null ? RoomRole.HAB : role;
            this.id = clean(id, this.role.name().toLowerCase(Locale.ROOT));
            this.label = clean(label, this.id);
            this.requiredUtilities = requiredUtilities == null ? EnumSet.noneOf(UtilityTag.class) : EnumSet.copyOf(requiredUtilities);
            this.validationTags = validationTags == null ? EnumSet.noneOf(ValidationTag.class) : EnumSet.copyOf(validationTags);
            this.supportsPlayerConstruction = supportsPlayerConstruction;
            this.supportsFactionConstruction = supportsFactionConstruction;
            this.notes = clean(notes, "no notes");
        }

        String compactLine() {
            return id + " role=" + role + " utilities=" + requiredUtilities + " validation=" + validationTags
                    + " player=" + supportsPlayerConstruction + " faction=" + supportsFactionConstruction;
        }
    }

    static final class BlueprintGovernanceSpec {
        final String id;
        final String label;
        final RoomRole preferredRoomRole;
        final EnumSet<UtilityTag> utilityHooks;
        final EnumSet<ValidationTag> requiredValidation;
        final boolean passabilityCritical;
        final boolean shouldExposeFailureReason;
        final String implementationBoundary;

        BlueprintGovernanceSpec(String id, String label, RoomRole preferredRoomRole, EnumSet<UtilityTag> utilityHooks,
                                EnumSet<ValidationTag> requiredValidation, boolean passabilityCritical,
                                boolean shouldExposeFailureReason, String implementationBoundary) {
            this.id = clean(id, "blueprint");
            this.label = clean(label, this.id);
            this.preferredRoomRole = preferredRoomRole == null ? RoomRole.HAB : preferredRoomRole;
            this.utilityHooks = utilityHooks == null ? EnumSet.noneOf(UtilityTag.class) : EnumSet.copyOf(utilityHooks);
            this.requiredValidation = requiredValidation == null ? EnumSet.noneOf(ValidationTag.class) : EnumSet.copyOf(requiredValidation);
            this.passabilityCritical = passabilityCritical;
            this.shouldExposeFailureReason = shouldExposeFailureReason;
            this.implementationBoundary = clean(implementationBoundary, "metadata only");
        }

        String compactLine() {
            return id + " role=" + preferredRoomRole + " utilities=" + utilityHooks + " validation=" + requiredValidation
                    + " passabilityCritical=" + passabilityCritical + " explainFailure=" + shouldExposeFailureReason;
        }
    }

    private final LinkedHashMap<String, RoomGovernanceSpec> roomSpecs = new LinkedHashMap<>();
    private final LinkedHashMap<String, BlueprintGovernanceSpec> blueprintSpecs = new LinkedHashMap<>();
    private long validationRequests = 0L;
    private long rejectedRequests = 0L;
    private final ArrayDeque<String> recentValidationNotes = new ArrayDeque<>();
    private static final int MAX_RECENT_NOTES = 24;

    ConstructionGovernanceAuthority() {
        registerDefaults();
    }

    private void registerDefaults() {
        EnumSet<ValidationTag> common = EnumSet.of(ValidationTag.PASSABILITY, ValidationTag.DOORWAY, ValidationTag.ROOM_ROLE, ValidationTag.SAVE_LOAD);
        registerRoom(new RoomGovernanceSpec(RoomRole.HAB, "hab_room", "Hab Room", EnumSet.of(UtilityTag.LIGHT), common, true, true,
                "Baseline living/tenant spaces require clear entry, passability, and ownership/staffing hooks."));
        registerRoom(new RoomGovernanceSpec(RoomRole.MEDICAE, "medicae_room", "Medicae Room", EnumSet.of(UtilityTag.POWER, UtilityTag.WATER, UtilityTag.LIGHT, UtilityTag.STAFFING_ACCESS),
                EnumSet.of(ValidationTag.PASSABILITY, ValidationTag.DOORWAY, ValidationTag.UTILITY, ValidationTag.STAFFING, ValidationTag.SAVE_LOAD), true, true,
                "Medicae construction must remain compatible with treatment queues, staffing, and corpse/patient routing."));
        registerRoom(new RoomGovernanceSpec(RoomRole.LABORATORY, "laboratory_room", "Laboratory", EnumSet.of(UtilityTag.POWER, UtilityTag.WATER, UtilityTag.DATA, UtilityTag.VENTILATION),
                EnumSet.of(ValidationTag.PASSABILITY, ValidationTag.UTILITY, ValidationTag.HAZARD, ValidationTag.DECOMPOSITION, ValidationTag.SAVE_LOAD), true, true,
                "Laboratory rooms should route machinery through shared queues and hazard/knowledge gates."));
        registerRoom(new RoomGovernanceSpec(RoomRole.FORGE, "forge_room", "Forge / Workshop", EnumSet.of(UtilityTag.POWER, UtilityTag.EXHAUST, UtilityTag.STORAGE_ACCESS, UtilityTag.STAFFING_ACCESS),
                EnumSet.of(ValidationTag.PASSABILITY, ValidationTag.UTILITY, ValidationTag.ADJACENCY, ValidationTag.HAZARD, ValidationTag.STAFFING, ValidationTag.SAVE_LOAD), true, true,
                "Forge construction requires exhaust, storage, and worker access checks for production readiness."));
        registerRoom(new RoomGovernanceSpec(RoomRole.ROAD_FRONTAGE, "road_frontage", "Road Frontage", EnumSet.of(UtilityTag.ROAD_ACCESS, UtilityTag.LIGHT),
                EnumSet.of(ValidationTag.PASSABILITY, ValidationTag.ADJACENCY, ValidationTag.OWNERSHIP, ValidationTag.SAVE_LOAD), false, true,
                "Road-adjacent alcoves, alleys, parking lots, and civic hooks should remain shared infrastructure, not bespoke decorations."));
        registerRoom(new RoomGovernanceSpec(RoomRole.UTILITY, "utility_room", "Utility Room", EnumSet.of(UtilityTag.POWER, UtilityTag.WATER, UtilityTag.WASTE, UtilityTag.EXHAUST),
                EnumSet.of(ValidationTag.PASSABILITY, ValidationTag.UTILITY, ValidationTag.HAZARD, ValidationTag.SAVE_LOAD), true, true,
                "Utility rooms are anchors for generators, water recyclers, waste machinery, vents, and service ledgers."));

        registerBlueprint(new BlueprintGovernanceSpec("basic_room_stamp", "Basic Room Stamp", RoomRole.HAB, EnumSet.of(UtilityTag.LIGHT),
                EnumSet.of(ValidationTag.PASSABILITY, ValidationTag.DOORWAY, ValidationTag.CLEARANCE, ValidationTag.SAVE_LOAD), true, true,
                "Metadata only; builder exposes invalid doorway/clearance reasons through UniversalWindow."));
        registerBlueprint(new BlueprintGovernanceSpec("machine_placement", "Machine Placement", RoomRole.FORGE, EnumSet.of(UtilityTag.POWER, UtilityTag.STORAGE_ACCESS, UtilityTag.STAFFING_ACCESS),
                EnumSet.of(ValidationTag.PASSABILITY, ValidationTag.UTILITY, ValidationTag.ADJACENCY, ValidationTag.DECOMPOSITION, ValidationTag.LOCALIZATION, ValidationTag.SAVE_LOAD), true, true,
                "Machine placement binds to MachineOperationQueue profiles, decomposition class, and faction/player parity metadata."));
        registerBlueprint(new BlueprintGovernanceSpec("hazard_feature", "Hazard Feature", RoomRole.UTILITY, EnumSet.of(UtilityTag.VENTILATION, UtilityTag.EXHAUST),
                EnumSet.of(ValidationTag.HAZARD, ValidationTag.PASSABILITY, ValidationTag.LOCALIZATION, ValidationTag.SAVE_LOAD), false, true,
                "Hazards expose readable warnings instead of silent trap states and route through shared hazard authority."));
        registerBlueprint(new BlueprintGovernanceSpec("road_parking_hook", "Road / Parking Hook", RoomRole.ROAD_FRONTAGE, EnumSet.of(UtilityTag.ROAD_ACCESS),
                EnumSet.of(ValidationTag.PASSABILITY, ValidationTag.ADJACENCY, ValidationTag.OWNERSHIP, ValidationTag.SAVE_LOAD), true, true,
                "Vehicle/parking infrastructure uses passive vehicle-staging metadata until full vehicle entities exist."));

        registerBlueprint(new BlueprintGovernanceSpec("defense_obstruction", "Defense Obstruction", RoomRole.SECURITY, EnumSet.of(UtilityTag.STAFFING_ACCESS),
                EnumSet.of(ValidationTag.PASSABILITY, ValidationTag.CLEARANCE, ValidationTag.OWNERSHIP, ValidationTag.LOCALIZATION, ValidationTag.SAVE_LOAD), true, true,
                "Barricades and sandbags are physical in-world defenses; live combat bonuses should consume DefenseSemanticIntegration profiles."));
        registerBlueprint(new BlueprintGovernanceSpec("defense_wall", "Defensive Wall", RoomRole.SECURITY, EnumSet.of(UtilityTag.NONE),
                EnumSet.of(ValidationTag.PASSABILITY, ValidationTag.CLEARANCE, ValidationTag.ADJACENCY, ValidationTag.OWNERSHIP, ValidationTag.SAVE_LOAD), true, true,
                "Wall segments remain physical, inspectable map objects; perimeter/gate logic must not become invisible modifiers."));
        registerBlueprint(new BlueprintGovernanceSpec("defense_area_denial", "Area-Denial Defense", RoomRole.SECURITY, EnumSet.of(UtilityTag.NONE),
                EnumSet.of(ValidationTag.HAZARD, ValidationTag.PASSABILITY, ValidationTag.LOCALIZATION, ValidationTag.SAVE_LOAD), false, true,
                "Wire and other area denial assets should later route through hazard authority and movement rules."));
        registerBlueprint(new BlueprintGovernanceSpec("defense_sensor", "Defense Sensor", RoomRole.SECURITY, EnumSet.of(UtilityTag.POWER, UtilityTag.DATA),
                EnumSet.of(ValidationTag.UTILITY, ValidationTag.OWNERSHIP, ValidationTag.LOCALIZATION, ValidationTag.SAVE_LOAD), false, true,
                "Sensor objects are visible hardware for alarm/LOS routing; no per-frame scan loop should be added here."));
        registerBlueprint(new BlueprintGovernanceSpec("defense_access_control", "Defense Access Control", RoomRole.SECURITY, EnumSet.of(UtilityTag.DATA),
                EnumSet.of(ValidationTag.PASSABILITY, ValidationTag.DOORWAY, ValidationTag.OWNERSHIP, ValidationTag.LOCALIZATION, ValidationTag.SAVE_LOAD), true, true,
                "Defensive doors and gates should reuse access/door semantics rather than bespoke obstruction code."));
        registerBlueprint(new BlueprintGovernanceSpec("defense_turret", "Defense Turret", RoomRole.SECURITY, EnumSet.of(UtilityTag.POWER, UtilityTag.DATA, UtilityTag.STAFFING_ACCESS),
                EnumSet.of(ValidationTag.PASSABILITY, ValidationTag.UTILITY, ValidationTag.OWNERSHIP, ValidationTag.STAFFING, ValidationTag.LOCALIZATION, ValidationTag.SAVE_LOAD, ValidationTag.MULTIPLAYER_HELD), true, true,
                "Turrets are semantic/buildable first; live autonomous combat must wait for ownership, ammo, power, hostility, and multiplayer-safe authority."));
        registerBlueprint(new BlueprintGovernanceSpec("defense_precinct_fixture", "Precinct Defensive Fixture", RoomRole.SECURITY, EnumSet.of(UtilityTag.LIGHT, UtilityTag.DATA),
                EnumSet.of(ValidationTag.ROOM_ROLE, ValidationTag.OWNERSHIP, ValidationTag.LOCALIZATION, ValidationTag.SAVE_LOAD), false, true,
                "Arbites precinct fixtures bind to room identity and infopedia before becoming service machines or security infrastructure."));

        registerBlueprint(new BlueprintGovernanceSpec("tutorial_hint_anchor", "Tutorial / Hint Anchor", RoomRole.TUTORIAL_HELD, EnumSet.of(UtilityTag.NONE),
                EnumSet.of(ValidationTag.ROOM_ROLE, ValidationTag.LOCALIZATION, ValidationTag.SAVE_LOAD), false, true,
                "Reserved searchable/clickable guidance anchors should reuse room/entity metadata and never create tutorial-only parallel systems."));
    }

    void registerRoom(RoomGovernanceSpec spec) {
        if (spec != null) roomSpecs.put(spec.id, spec);
    }

    void registerBlueprint(BlueprintGovernanceSpec spec) {
        if (spec != null) blueprintSpecs.put(spec.id, spec);
    }


    String explainPlacementResult(String blueprintId, boolean ok, String rawReason, String context) {
        validationRequests++;
        String id = clean(blueprintId, "basic_room_stamp");
        BlueprintGovernanceSpec spec = blueprintSpecs.get(id);
        String reason = clean(rawReason, ok ? "OK" : "blocked");
        String ctx = clean(context, "placement");
        if (ok || "OK".equals(reason)) {
            String line = (spec == null ? id : spec.id) + " ok: placement pathway accepted for " + ctx;
            remember(line);
            return "OK";
        }
        rejectedRequests++;
        String category = categorizeRawPlacementReason(reason);
        String label = spec == null ? id : spec.label;
        String line = label + " blocked [" + category + "]: " + reason + " context=" + ctx;
        remember(line);
        return line;
    }

    String advisoryForBuild(String blueprintId, String recipeName, String context) {
        String id = clean(blueprintId, "basic_room_stamp");
        BlueprintGovernanceSpec spec = blueprintSpecs.get(id);
        if (spec == null) return "Construction governance: " + id + " uses legacy placement rules until a blueprint spec is promoted.";
        String line = "Construction governance: " + clean(recipeName, spec.label) + " uses " + spec.id
                + " validation; hooks=" + spec.utilityHooks + "; checks=" + spec.requiredValidation + ".";
        remember(line + " context=" + clean(context, "build"));
        return line;
    }

    List<String> recentValidationNotes() {
        return Collections.unmodifiableList(new ArrayList<>(recentValidationNotes));
    }

    private String categorizeRawPlacementReason(String reason) {
        String r = clean(reason, "").toLowerCase(Locale.ROOT);
        if (r.contains("wall") || r.contains("walkable") || r.contains("move off") || r.contains("already contains")) return ValidationTag.PASSABILITY.name();
        if (r.contains("claimed room") || r.contains("outside")) return ValidationTag.OWNERSHIP.name();
        if (r.contains("room") || r.contains("facility")) return ValidationTag.ROOM_ROLE.name();
        if (r.contains("knowledge") || r.contains("decomposition")) return ValidationTag.DECOMPOSITION.name();
        if (r.contains("utility") || r.contains("power") || r.contains("water") || r.contains("exhaust")) return ValidationTag.UTILITY.name();
        return ValidationTag.CLEARANCE.name();
    }

    String validateBlueprintMetadata(String blueprintId, RoomRole roomRole, EnumSet<UtilityTag> availableUtilities, boolean passable, String context) {
        validationRequests++;
        BlueprintGovernanceSpec spec = blueprintSpecs.get(clean(blueprintId, ""));
        if (spec == null) return reject("unknown blueprint: " + clean(blueprintId, "null"), context);
        if (spec.passabilityCritical && !passable) return reject(spec.id + " blocked: passability check failed", context);
        if (roomRole != null && spec.preferredRoomRole != RoomRole.TUTORIAL_HELD && roomRole != spec.preferredRoomRole && spec.requiredValidation.contains(ValidationTag.ROOM_ROLE)) {
            return reject(spec.id + " blocked: expected room role " + spec.preferredRoomRole + " but found " + roomRole, context);
        }
        EnumSet<UtilityTag> available = availableUtilities == null ? EnumSet.noneOf(UtilityTag.class) : EnumSet.copyOf(availableUtilities);
        for (UtilityTag hook : spec.utilityHooks) {
            if (hook != UtilityTag.NONE && !available.contains(hook)) return reject(spec.id + " blocked: missing utility " + hook, context);
        }
        String ok = spec.id + " ok: metadata validation passed for " + clean(context, "unspecified context");
        remember(ok);
        return ok;
    }

    private String reject(String message, String context) {
        rejectedRequests++;
        String line = clean(message, "validation rejected") + " context=" + clean(context, "none");
        remember(line);
        return line;
    }

    private void remember(String line) {
        recentValidationNotes.addLast(clean(line, "validation note"));
        while (recentValidationNotes.size() > MAX_RECENT_NOTES) recentValidationNotes.removeFirst();
    }

    Collection<RoomGovernanceSpec> roomSpecs() {
        return Collections.unmodifiableCollection(roomSpecs.values());
    }

    Collection<BlueprintGovernanceSpec> blueprintSpecs() {
        return Collections.unmodifiableCollection(blueprintSpecs.values());
    }

    String migrationChecklist() {
        StringBuilder sb = new StringBuilder();
        sb.append("Construction governance migration checklist:");
        for (RoomGovernanceSpec spec : roomSpecs.values()) sb.append("\n- room ").append(spec.compactLine()).append(" :: ").append(spec.notes);
        for (BlueprintGovernanceSpec spec : blueprintSpecs.values()) sb.append("\n- blueprint ").append(spec.compactLine()).append(" :: ").append(spec.implementationBoundary);
        return sb.toString();
    }

    String auditSummary() {
        return "constructionGovernance version=" + VERSION + " roomSpecs=" + roomSpecs.size()
                + " blueprintSpecs=" + blueprintSpecs.size() + " validations=" + validationRequests
                + " rejected=" + rejectedRequests + " recentNotes=" + recentValidationNotes.size();
    }

    static String clean(String value, String fallback) {
        if (value == null) return fallback;
        String v = value.trim();
        return v.isEmpty() ? fallback : v;
    }
}
