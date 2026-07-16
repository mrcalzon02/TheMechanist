package mechanist;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Resolves vehicle loss into persistent physical outcomes instead of deleting
 * the fixture. Combat may call this later, but this authority does not calculate
 * weapon damage or invent a second vehicle health model.
 */
final class VehicleLossAuthority {
    enum Cause {
        COMPONENT_FAILURE,
        FIRE,
        ABANDONMENT,
        CAPTURE,
        LOOTING,
        SALVAGE_CONVERSION,
        QUEST_MARK,
        STRATEGIC_DEFEAT
    }

    enum Outcome {
        DISABLED_REPAIR_PROJECT("disabled but repairable"),
        BURNED_OUT_WRECK("burned-out wreck"),
        SALVAGE_HULK("salvage hulk"),
        ABANDONED_RECOVERY_ASSET("abandoned recovery asset"),
        CAPTURED_MOTOR_POOL_ASSET("captured motor-pool asset"),
        LOOTED_VEHICLE("looted vehicle"),
        BLOCKED_ROAD_OBSTACLE("blocked-road obstacle"),
        FUEL_OR_POWER_LEAK("fuel or power leak hazard"),
        FACTION_TROPHY("faction trophy"),
        QUEST_OBJECTIVE("quest objective"),
        STRATEGIC_ASSET_LOSS("strategic asset loss");

        final String label;
        Outcome(String label) { this.label = label; }
    }

    record Resolution(boolean success, boolean changed, Cause cause,
                      Outcome primaryOutcome, Set<Outcome> outcomes,
                      String formerOwner, String currentOwner,
                      String message, MapObjectState vehicle) {
        static Resolution blocked(Cause cause, String message,
                                  MapObjectState vehicle) {
            return new Resolution(false, false, cause, null, Set.of(),
                    "", "", message == null || message.isBlank()
                    ? "Vehicle loss resolution was blocked." : message, vehicle);
        }
    }

    private VehicleLossAuthority() { }

    static Resolution resolve(GamePanel game, MapObjectState vehicle,
                              Cause cause, Faction actorFaction,
                              String context) {
        if (game == null || game.world == null || vehicle == null
                || !VehicleRuntimeAuthority.isVehicle(vehicle)
                || !game.world.mapObjects.contains(vehicle)) {
            return Resolution.blocked(cause,
                    "Vehicle loss resolution requires a physical vehicle in the loaded zone.",
                    vehicle);
        }
        Cause selected = cause == null ? Cause.COMPONENT_FAILURE : cause;
        VehicleRuntimeAuthority.ensureInitialized(game.world, vehicle);
        VehicleRuntimeAuthority.Snapshot before =
                VehicleRuntimeAuthority.inspect(game.world, vehicle);
        String formerOwner = ownerLine(before);
        LinkedHashSet<Outcome> outcomes = new LinkedHashSet<>();
        Outcome primary;
        boolean changed;

        switch (selected) {
            case COMPONENT_FAILURE -> {
                int frame = component(vehicle,
                        VehicleRuntimeAuthority.Component.FRAME);
                int power = component(vehicle,
                        VehicleRuntimeAuthority.Component.POWERPLANT);
                int drive = component(vehicle,
                        VehicleRuntimeAuthority.Component.DRIVE);
                int mobility = component(vehicle,
                        VehicleRuntimeAuthority.Component.MOBILITY);
                if (frame > 0 && power > 0 && drive > 0 && mobility > 0) {
                    return Resolution.blocked(selected,
                            "The vehicle still has working frame, powerplant, drive, and mobility assemblies; no loss state was created.",
                            vehicle);
                }
                if (frame <= 0) {
                    primary = Outcome.BURNED_OUT_WRECK;
                    makeWreck(vehicle, false, game.turn,
                            clean(context, "catastrophic frame failure"));
                } else {
                    primary = Outcome.DISABLED_REPAIR_PROJECT;
                    makeDisabled(vehicle, game.turn,
                            clean(context, "critical component failure"));
                }
                changed = true;
            }
            case FIRE -> {
                primary = Outcome.BURNED_OUT_WRECK;
                setComponentAtMost(vehicle,
                        VehicleRuntimeAuthority.Component.POWERPLANT, 0);
                setComponentAtMost(vehicle,
                        VehicleRuntimeAuthority.Component.ARMOR, 20);
                setComponentAtMost(vehicle,
                        VehicleRuntimeAuthority.Component.CARGO, 20);
                makeWreck(vehicle, true, game.turn,
                        clean(context, "vehicle fire"));
                changed = true;
            }
            case ABANDONMENT -> {
                if (before.ownerType()
                        == VehicleRuntimeAuthority.OwnerType.ABANDONED) {
                    return Resolution.blocked(selected,
                            "The vehicle is already recorded as abandoned property.",
                            vehicle);
                }
                appendFormerOwner(vehicle, formerOwner);
                set(vehicle, "ownerType",
                        VehicleRuntimeAuthority.OwnerType.ABANDONED.name());
                set(vehicle, "ownerFaction", Faction.NONE.name());
                set(vehicle, "ownerName", "Abandoned property record");
                set(vehicle, "ownership", "abandoned");
                set(vehicle, "forSale", "false");
                set(vehicle, "headlightsActive", "false");
                if (criticalComponentsOperational(vehicle)) {
                    set(vehicle, "operationState", "parked");
                } else {
                    set(vehicle, "operationState", "disabled");
                    outcomes.add(Outcome.DISABLED_REPAIR_PROJECT);
                }
                append(vehicle, "captureHistory",
                        formerOwner + " abandoned custody at turn "
                                + Math.max(0, game.turn));
                if (component(vehicle,
                        VehicleRuntimeAuthority.Component.FRAME) <= 0) {
                    primary = Outcome.SALVAGE_HULK;
                } else if (!criticalComponentsOperational(vehicle)) {
                    primary = Outcome.DISABLED_REPAIR_PROJECT;
                } else {
                    primary = Outcome.ABANDONED_RECOVERY_ASSET;
                }
                changed = true;
            }
            case CAPTURE -> {
                if (actorFaction == null || actorFaction == Faction.NONE) {
                    return Resolution.blocked(selected,
                            "Vehicle capture requires a named faction receiving custody.",
                            vehicle);
                }
                if (before.ownerType()
                        == VehicleRuntimeAuthority.OwnerType.PLAYER) {
                    return Resolution.blocked(selected,
                            "Background vehicle capture cannot confiscate a player-owned vehicle.",
                            vehicle);
                }
                VehicleRuntimeAuthority.Result transfer =
                        VehicleRuntimeAuthority.transferToFaction(vehicle,
                                actorFaction, game.turn,
                                clean(context, "vehicle capture"));
                if (!transfer.success()) {
                    return Resolution.blocked(selected, transfer.message(), vehicle);
                }
                set(vehicle, "vehicleRole", "captured-motor-pool");
                set(vehicle, "operationState",
                        criticalComponentsOperational(vehicle)
                                ? "parked" : "disabled");
                set(vehicle, "headlightsActive", "false");
                primary = Outcome.CAPTURED_MOTOR_POOL_ASSET;
                if (!criticalComponentsOperational(vehicle)) {
                    outcomes.add(Outcome.DISABLED_REPAIR_PROJECT);
                }
                if (isRestricted(vehicle)) outcomes.add(Outcome.FACTION_TROPHY);
                if (before.ownerFaction() != null
                        && before.ownerFaction() != Faction.NONE
                        && !FactionIdentityAuthority.sameFamily(
                        before.ownerFaction(), actorFaction)) {
                    outcomes.add(Outcome.STRATEGIC_ASSET_LOSS);
                    game.addFactionMarketPressure(before.ownerFaction(),
                            strategicPressure(vehicle),
                            actorFaction.label + " captured "
                                    + displayName(vehicle));
                }
                changed = transfer.changed();
            }
            case LOOTING -> {
                if ("true".equals(value(vehicle, "cargoLooted"))) {
                    return Resolution.blocked(selected,
                            "The vehicle cargo and loose fittings were already looted.",
                            vehicle);
                }
                set(vehicle, "cargoLooted", "true");
                setComponentAtMost(vehicle,
                        VehicleRuntimeAuthority.Component.CARGO, 35);
                set(vehicle, "forSale", "false");
                append(vehicle, "salvageHistory",
                        "Loose cargo and fittings looted at turn "
                                + Math.max(0, game.turn) + " / "
                                + clean(context, "vehicle looting"));
                primary = Outcome.LOOTED_VEHICLE;
                changed = true;
            }
            case SALVAGE_CONVERSION -> {
                if ("salvaged".equals(value(vehicle, "condition"))
                        || "dismantled".equals(
                        value(vehicle, "operationState"))) {
                    return Resolution.blocked(selected,
                            "The vehicle has already been fully stripped.",
                            vehicle);
                }
                appendFormerOwner(vehicle, formerOwner);
                setComponentAtMost(vehicle,
                        VehicleRuntimeAuthority.Component.FRAME, 0);
                setComponentAtMost(vehicle,
                        VehicleRuntimeAuthority.Component.POWERPLANT, 0);
                set(vehicle, "ownerType",
                        VehicleRuntimeAuthority.OwnerType.SALVAGE.name());
                set(vehicle, "ownerFaction", Faction.NONE.name());
                set(vehicle, "ownerName", "Salvage hulk record");
                set(vehicle, "ownership", "salvage-hulk");
                set(vehicle, "operationState", "disabled");
                set(vehicle, "headlightsActive", "false");
                set(vehicle, "forSale", "false");
                set(vehicle, "condition", "wreck");
                vehicle.glyph = 'o';
                if (vehicle.label == null
                        || !vehicle.label.startsWith("Salvage hulk")) {
                    vehicle.label = "Salvage hulk / " + displayName(vehicle);
                }
                append(vehicle, "salvageHistory",
                        "Converted to a recoverable hulk at turn "
                                + Math.max(0, game.turn) + " / "
                                + clean(context, "salvage conversion"));
                primary = Outcome.SALVAGE_HULK;
                changed = true;
            }
            case QUEST_MARK -> {
                if ("true".equals(value(vehicle, "questObjective"))) {
                    return Resolution.blocked(selected,
                            "The vehicle is already marked as a quest objective.",
                            vehicle);
                }
                set(vehicle, "questObjective", "true");
                set(vehicle, "questObjectiveState", "active");
                set(vehicle, "questObjectiveReason",
                        clean(context, "vehicle recovery objective"));
                primary = Outcome.QUEST_OBJECTIVE;
                changed = true;
            }
            case STRATEGIC_DEFEAT -> {
                if ("true".equals(value(vehicle, "strategicAssetLost"))) {
                    return Resolution.blocked(selected,
                            "This vehicle's strategic loss has already been recorded.",
                            vehicle);
                }
                set(vehicle, "strategicAssetLost", "true");
                set(vehicle, "strategicLossReason",
                        clean(context, "vehicle strategic defeat"));
                if (before.ownerFaction() != null
                        && before.ownerFaction() != Faction.NONE) {
                    game.addFactionMarketPressure(before.ownerFaction(),
                            strategicPressure(vehicle),
                            displayName(vehicle)
                                    + " was lost as a strategic asset");
                }
                primary = Outcome.STRATEGIC_ASSET_LOSS;
                changed = true;
            }
            default -> throw new IllegalStateException(
                    "Unhandled vehicle loss cause " + selected);
        }

        outcomes.add(primary);
        addPhysicalConsequences(game.world, vehicle, outcomes);
        if (outcomes.contains(Outcome.BURNED_OUT_WRECK)
                || outcomes.contains(Outcome.DISABLED_REPAIR_PROJECT)
                || outcomes.contains(Outcome.SALVAGE_HULK)) {
            set(vehicle, "headlightsActive", "false");
            VehicleOperationFeedbackAuthority.clearTransientFeedback();
        }
        append(vehicle, "lossHistory", selected.name().toLowerCase(Locale.ROOT)
                + " / " + labels(outcomes) + " / turn "
                + Math.max(0, game.turn) + " / "
                + clean(context, "vehicle loss"));
        append(vehicle, "vehicleHistory", "Loss outcome: "
                + primary.label + " at turn " + Math.max(0, game.turn));
        set(vehicle, "lastLossCause", selected.name());
        set(vehicle, "lastLossOutcome", primary.name());
        set(vehicle, "lossOutcomeTags", outcomeNames(outcomes));
        game.markLocalDirtyRegion("vehicle loss outcome", vehicle.x,
                vehicle.y, 7, false, false, true, false);

        VehicleRuntimeAuthority.Snapshot after =
                VehicleRuntimeAuthority.inspect(game.world, vehicle);
        return new Resolution(true, changed, selected, primary,
                Set.copyOf(outcomes), formerOwner, ownerLine(after),
                "VEHICLE LOSS: " + displayName(vehicle) + " became "
                        + primary.label + ". Consequences: " + labels(outcomes)
                        + ". The physical vehicle remains at " + vehicle.x
                        + "," + vehicle.y + " for repair, recovery, capture,"
                        + " salvage, hazard, or objective handling.", vehicle);
    }

    static List<String> inspectionLines(World world, MapObjectState vehicle) {
        if (vehicle == null || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            return List.of("No vehicle loss record is available.");
        }
        VehicleRuntimeAuthority.ensureInitialized(world, vehicle);
        ArrayList<String> lines = new ArrayList<>();
        String tags = value(vehicle, "lossOutcomeTags");
        lines.add(tags.isBlank()
                ? "No persistent vehicle-loss outcome has been recorded."
                : "Recorded loss outcomes: " + readableTags(tags) + ".");
        lines.add("Current consequence: condition "
                + value(vehicle, "condition") + ", operation "
                + value(vehicle, "operationState") + ", ownership "
                + value(vehicle, "ownership") + ".");
        if (isRoadBlocker(vehicle)) {
            lines.add("Road consequence: this vehicle is a physical route obstruction until recovered, moved, repaired, or dismantled.");
        }
        if (hasLeakHazard(vehicle)) {
            lines.add("Hazard consequence: damaged power or fuel systems are leaking; repair or salvage work must account for the hazard.");
        }
        if ("true".equals(value(vehicle, "questObjective"))) {
            lines.add("Objective consequence: this vehicle is marked for recovery, inspection, capture, or salvage work.");
        }
        if ("true".equals(value(vehicle, "strategicAssetLost"))) {
            lines.add("Strategic consequence: the owning faction has recorded this vehicle as a material loss.");
        }
        lines.add("Recovery paths: repair critical components, transfer lawful custody, convert the wreck to a salvage hulk, or strip it through the existing salvage authority.");
        return List.copyOf(lines);
    }

    static boolean isRoadBlocker(MapObjectState vehicle) {
        return vehicle != null
                && "true".equals(value(vehicle, "roadObstacle"));
    }

    static boolean hasLeakHazard(MapObjectState vehicle) {
        return vehicle != null
                && "true".equals(value(vehicle, "fuelOrPowerLeak"));
    }

    private static void addPhysicalConsequences(World world,
                                                MapObjectState vehicle,
                                                LinkedHashSet<Outcome> outcomes) {
        String condition = value(vehicle, "condition");
        boolean immobile = "disabled".equals(
                value(vehicle, "operationState"))
                || "wreck".equals(condition)
                || "salvaged".equals(condition)
                || "salvage-hulk".equals(value(vehicle, "ownership"));
        if (immobile && roadTile(world, vehicle.x, vehicle.y)) {
            set(vehicle, "roadObstacle", "true");
            outcomes.add(Outcome.BLOCKED_ROAD_OBSTACLE);
        }
        int power = component(vehicle,
                VehicleRuntimeAuthority.Component.POWERPLANT);
        if (power <= 20 && !"salvaged".equals(condition)) {
            set(vehicle, "fuelOrPowerLeak", "true");
            set(vehicle, "hazardState", "vehicle-power-leak");
            outcomes.add(Outcome.FUEL_OR_POWER_LEAK);
        }
        if ("true".equals(value(vehicle, "questObjective"))) {
            outcomes.add(Outcome.QUEST_OBJECTIVE);
        }
        if ("true".equals(value(vehicle, "strategicAssetLost"))) {
            outcomes.add(Outcome.STRATEGIC_ASSET_LOSS);
        }
    }

    private static void makeDisabled(MapObjectState vehicle, int turn,
                                     String reason) {
        set(vehicle, "condition", "disabled");
        set(vehicle, "operationState", "disabled");
        set(vehicle, "headlightsActive", "false");
        set(vehicle, "forSale", "false");
        append(vehicle, "damageHistory", "Disabled at turn "
                + Math.max(0, turn) + " / " + reason);
    }

    private static void makeWreck(MapObjectState vehicle, boolean burned,
                                  int turn, String reason) {
        setComponentAtMost(vehicle,
                VehicleRuntimeAuthority.Component.FRAME, 0);
        set(vehicle, "condition", "wreck");
        set(vehicle, "operationState", "disabled");
        set(vehicle, "headlightsActive", "false");
        set(vehicle, "forSale", "false");
        set(vehicle, "burnedOut", Boolean.toString(burned));
        vehicle.glyph = 'x';
        if (vehicle.label == null
                || !vehicle.label.startsWith("Burned-out wreck")) {
            vehicle.label = (burned ? "Burned-out wreck / " : "Vehicle wreck / ")
                    + displayName(vehicle);
        }
        append(vehicle, "damageHistory", (burned
                ? "Burned out" : "Wrecked") + " at turn "
                + Math.max(0, turn) + " / " + reason);
    }

    private static boolean criticalComponentsOperational(
            MapObjectState vehicle) {
        return component(vehicle, VehicleRuntimeAuthority.Component.FRAME) > 0
                && component(vehicle,
                VehicleRuntimeAuthority.Component.POWERPLANT) > 0
                && component(vehicle, VehicleRuntimeAuthority.Component.DRIVE) > 0
                && component(vehicle,
                VehicleRuntimeAuthority.Component.MOBILITY) > 0;
    }

    private static boolean isRestricted(MapObjectState vehicle) {
        String legal = value(vehicle, "legalClass");
        return "RESTRICTED".equals(legal) || "MILITARY".equals(legal);
    }

    private static int strategicPressure(MapObjectState vehicle) {
        return switch (VehicleRuntimeAuthority.vehicleClass(vehicle.type)) {
            case UTILITY_BIKE -> 1;
            case CIVILIAN_CAR -> 2;
            case CARGO_TRUCK -> 3;
            case ARMORED_CAR -> 5;
            case TANK -> 8;
        };
    }

    private static boolean roadTile(World world, int x, int y) {
        if (world == null || !world.inBounds(x, y)) return false;
        char tile = world.tiles[x][y];
        return tile == RoadGridIntegrationAuthority.ROAD_LANE
                || tile == RoadGridIntegrationAuthority.PARKING_SPACE;
    }

    private static int component(MapObjectState vehicle,
                                 VehicleRuntimeAuthority.Component component) {
        try {
            return Math.max(0, Math.min(100,
                    Integer.parseInt(value(vehicle, componentKey(component)))));
        } catch (Exception ignored) {
            return 100;
        }
    }

    private static void setComponentAtMost(MapObjectState vehicle,
                                           VehicleRuntimeAuthority.Component component,
                                           int maximum) {
        set(vehicle, componentKey(component), Integer.toString(
                Math.min(component(vehicle, component),
                        Math.max(0, Math.min(100, maximum)))));
    }

    private static String componentKey(
            VehicleRuntimeAuthority.Component component) {
        return "vehicleComponent" + component.name().charAt(0)
                + component.name().substring(1).toLowerCase(Locale.ROOT);
    }

    private static void appendFormerOwner(MapObjectState vehicle,
                                          String owner) {
        String current = value(vehicle, "formerOwners");
        if (current.contains(owner)) return;
        append(vehicle, "formerOwners", owner);
    }

    private static String ownerLine(
            VehicleRuntimeAuthority.Snapshot snapshot) {
        if (snapshot == null) return "unrecorded owner";
        String owner = clean(snapshot.ownerName(),
                snapshot.ownerType().name().toLowerCase(Locale.ROOT));
        if (snapshot.ownerFaction() != null
                && snapshot.ownerFaction() != Faction.NONE
                && !owner.contains(snapshot.ownerFaction().label)) {
            owner += " / " + snapshot.ownerFaction().label;
        }
        return owner;
    }

    private static String displayName(MapObjectState vehicle) {
        VehicleRuntimeAuthority.Snapshot snapshot =
                VehicleRuntimeAuthority.inspect(null, vehicle);
        if (snapshot == null) return "vehicle";
        return (clean(snapshot.manufacturer(), "") + " "
                + clean(snapshot.model(), snapshot.vehicleClass().label)).trim();
    }

    private static String labels(Set<Outcome> outcomes) {
        ArrayList<String> labels = new ArrayList<>();
        for (Outcome outcome : outcomes) labels.add(outcome.label);
        return String.join(", ", labels);
    }

    private static String outcomeNames(Set<Outcome> outcomes) {
        ArrayList<String> names = new ArrayList<>();
        for (Outcome outcome : outcomes) names.add(outcome.name());
        return String.join(",", names);
    }

    private static String readableTags(String tags) {
        return clean(tags, "none").toLowerCase(Locale.ROOT)
                .replace('_', ' ').replace(',', ';');
    }

    private static void append(MapObjectState vehicle, String key,
                               String entry) {
        ArrayList<String> entries = new ArrayList<>();
        String existing = value(vehicle, key);
        if (!existing.isBlank()) {
            for (String part : existing.split("~")) {
                String cleaned = clean(part, "");
                if (!cleaned.isBlank()) entries.add(cleaned);
            }
        }
        entries.add(clean(entry, "vehicle loss record"));
        while (entries.size() > 8) entries.remove(0);
        set(vehicle, key, String.join("~", entries));
    }

    private static String value(MapObjectState vehicle, String key) {
        return vehicle == null ? ""
                : MapObjectState.stockValue(vehicle.stockState, key);
    }

    private static void set(MapObjectState vehicle, String key, String value) {
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                key, clean(value, "").replace(';', ',').replace('|', '/'));
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? ""
                : value.trim().replaceAll("\\s+", " ");
        return cleaned.isBlank() ? (fallback == null ? "" : fallback)
                : cleaned;
    }
}
