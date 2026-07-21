package mechanist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Builds player-facing vehicle dashboard and Infopedia text from live authorities. */
final class VehicleInfopediaBridgeAuthority {
    record ClassDossier(String key, String title, List<String> lines) { }

    private VehicleInfopediaBridgeAuthority() { }

    static String key(VehicleRuntimeAuthority.VehicleClass definition) {
        VehicleRuntimeAuthority.VehicleClass safe = safe(definition);
        return "vehicle/" + safe.name().toLowerCase();
    }

    static String sourceLine(VehicleRuntimeAuthority.VehicleClass definition) {
        VehicleRuntimeAuthority.VehicleClass safe = safe(definition);
        return String.join(", ", safe.manufacturers)
                + "; faction motor pools, public fleets, private ownership, abandoned recovery, and salvage custody may also place physical examples";
    }

    static String description(VehicleRuntimeAuthority.VehicleClass definition) {
        VehicleRuntimeAuthority.VehicleClass safe = safe(definition);
        return "Vehicle Infopedia dossier for " + safe.label + ". Role: "
                + safe.role + ". Durability: " + safe.durabilityTier
                + ". Legal class: " + safe.legalClass + ". Seats: "
                + safe.seats + ". Operational crew: " + safe.crewRequired
                + ". Cargo capacity: " + safe.cargoCapacity + " unit(s).";
    }

    static String useLine(VehicleRuntimeAuthority.VehicleClass definition) {
        VehicleRuntimeAuthority.VehicleClass safe = safe(definition);
        return "Inspect a physical " + safe.label
                + " to open its live dashboard. Operation, passenger, cargo, repair, refuel, deployment, seizure, routing, loss, and salvage permissions remain independently checked; this dossier grants no ownership or authority.";
    }

    static List<ClassDossier> catalogDossiers() {
        ArrayList<ClassDossier> dossiers = new ArrayList<>();
        for (VehicleRuntimeAuthority.VehicleClass definition
                : VehicleRuntimeAuthority.catalog()) {
            dossiers.add(new ClassDossier(key(definition),
                    definition.label + " vehicle dossier",
                    List.copyOf(dossierLines(definition))));
        }
        dossiers.sort(Comparator.comparing(ClassDossier::title));
        return List.copyOf(dossiers);
    }

    static ArrayList<String> dossierLines(
            VehicleRuntimeAuthority.VehicleClass definition) {
        VehicleRuntimeAuthority.VehicleClass safe = safe(definition);
        ArrayList<String> lines = new ArrayList<>();
        lines.add(safe.label + " vehicle dossier");
        lines.add("Role and scale: " + safe.role + "; durability "
                + safe.durabilityTier + "; legal class " + safe.legalClass + ".");
        lines.add("Capacity: " + safe.seats + " seat(s), "
                + safe.crewRequired + " operational crew required, "
                + safe.cargoCapacity + " cargo unit(s).");
        lines.add("Energy capacity: " + VehicleFuelAuthority.capacity(safe)
                + " fuel or power unit(s).");
        lines.add("Ordinary manufacturers: "
                + String.join(", ", safe.manufacturers) + ".");
        lines.add("Registered models: " + String.join(", ", safe.models) + ".");
        lines.add("Reference value: " + safe.purchasePrice
                + " script purchase baseline; " + safe.salvageBase
                + " machine-part salvage baseline before condition and custody rules.");
        lines.add("Sensory profile: headlight range up to "
                + referenceHeadlightRange(safe) + " tile(s); ambient operating sound carries up to "
                + referenceAudibleRange(safe) + " tile(s) when enabled and unobstructed by mute settings.");
        lines.add("Authority boundary: a dossier grants no title, permit, driver assignment, cargo custody, deployment order, seizure warrant, or route reservation.");
        return lines;
    }

    static List<String> liveDossier(GamePanel game, MapObjectState vehicle) {
        if (vehicle == null || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            return List.of("No live vehicle dashboard is available.");
        }
        World world = game == null ? null : game.world;
        VehicleRuntimeAuthority.ensureInitialized(world, vehicle);
        VehicleRuntimeAuthority.Snapshot runtime =
                VehicleRuntimeAuthority.inspect(world, vehicle);
        if (runtime == null) return List.of("No live vehicle dashboard is available.");
        VehicleFuelAuthority.Snapshot fuel = VehicleFuelAuthority.inspect(world, vehicle);
        VehicleManifestAuthority.Snapshot manifest =
                VehicleManifestAuthority.inspect(world, vehicle);
        VehicleMotorPoolAuthority.Snapshot pool =
                VehicleMotorPoolAuthority.inspect(game, vehicle, null);

        ArrayList<String> lines = new ArrayList<>();
        lines.add("VEHICLE DASHBOARD — " + clean(runtime.manufacturer(), "Unknown maker")
                + " " + clean(runtime.model(), runtime.vehicleClass().label));
        lines.add("Class: " + runtime.vehicleClass().label + " / "
                + runtime.vehicleClass().role + " / " + runtime.legalClass()
                + "; variant " + clean(runtime.variant(), "standard") + ".");
        lines.add("Identity: " + clean(runtime.productionBatch(), "unrecorded batch")
                + "; fixture " + clean(vehicle.id, "unrecorded") + ".");
        lines.add("Custody: " + runtime.ownerType().name().toLowerCase()
                + " / " + clean(runtime.ownerName(), "unrecorded owner")
                + (runtime.ownerFaction() == null || runtime.ownerFaction() == Faction.NONE
                ? "" : " / " + runtime.ownerFaction().label) + ".");
        lines.add("Condition: " + runtime.condition() + "; operation "
                + runtime.operationState() + "; overall integrity "
                + runtime.integrity() + "%; " + componentSummary(runtime.components()) + ".");
        lines.add("Energy: " + fuel.current() + "/" + fuel.capacity() + " "
                + fuel.energyType() + " unit(s); " + fuel.reserved()
                + " reserved; state " + fuel.state() + ".");
        lines.add("Crew and seating: driver "
                + clean(manifest.driver(), "unassigned") + "; operational crew "
                + manifest.assignedCrew() + "/" + runtime.vehicleClass().crewRequired
                + "; occupied seats " + manifest.occupiedSeats() + "/"
                + manifest.seatCapacity() + ".");
        lines.add("Passengers: " + (manifest.passengers().isEmpty()
                ? "none" : String.join(", ", manifest.passengers())) + ".");
        lines.add("Cargo: " + manifest.cargoUnits() + "/"
                + manifest.cargoCapacity() + " unit(s); "
                + cargoSummary(manifest.cargo()) + ".");
        lines.add(pool.assigned()
                ? "Motor pool: " + clean(pool.siteName(), "unnamed motor pool")
                + "; role " + clean(pool.role(), "unrecorded")
                + "; readiness " + pool.state() + "."
                : "Motor pool: no active assignment.");
        lines.add(strategicLine(vehicle));
        lines.add(lossLine(vehicle));
        lines.add("Sensory feedback: "
                + VehicleOperationFeedbackAuthority.soundCue(vehicle)
                + " operating cue; audible range "
                + VehicleOperationFeedbackAuthority.ambientAudibleRange(vehicle)
                + " tile(s); working headlight range "
                + VehicleOperationFeedbackAuthority.headlightRange(vehicle)
                + " tile(s).");
        lines.add("Access summary: " + clean(runtime.accessSummary(),
                "inspect individual operation, passenger, cargo, repair, refuel, deployment, and seizure permissions") + ".");
        lines.add("Infopedia reference: " + key(runtime.vehicleClass())
                + ". This live dashboard reflects the physical fixture and does not grant authority.");
        return List.copyOf(lines);
    }

    private static String componentSummary(
            Map<VehicleRuntimeAuthority.Component, Integer> components) {
        if (components == null || components.isEmpty()) return "components unrecorded";
        VehicleRuntimeAuthority.Component worst = null;
        int lowest = 101;
        int damaged = 0;
        for (VehicleRuntimeAuthority.Component component
                : VehicleRuntimeAuthority.Component.values()) {
            int value = Math.max(0, Math.min(100,
                    components.getOrDefault(component, 100)));
            if (value < 100) damaged++;
            if (value < lowest) {
                lowest = value;
                worst = component;
            }
        }
        if (worst == null || lowest >= 100) return "all major component groups at 100%";
        return damaged + " damaged component group(s), lowest "
                + worst.label + " at " + lowest + "%";
    }

    private static String cargoSummary(List<VehicleManifestAuthority.CargoEntry> cargo) {
        if (cargo == null || cargo.isEmpty()) return "no registered cargo custody";
        ArrayList<String> entries = new ArrayList<>();
        for (VehicleManifestAuthority.CargoEntry entry : cargo) {
            if (entry == null || entry.units() <= 0) continue;
            entries.add(entry.label() + " / " + entry.owner() + " / "
                    + entry.units());
        }
        return entries.isEmpty() ? "no registered cargo custody"
                : String.join("; ", entries);
    }

    private static String strategicLine(MapObjectState vehicle) {
        String state = value(vehicle, "strategicTransitState");
        if (state.isBlank()) return "Strategic transit: no reservation recorded.";
        return "Strategic transit: " + state + "; destination "
                + clean(value(vehicle, "strategicTransitDestination"), "not recorded")
                + "; fuel or power reserved "
                + clean(value(vehicle, "strategicTransitFuelReserved"), "0") + ".";
    }

    private static String lossLine(MapObjectState vehicle) {
        ArrayList<String> consequences = new ArrayList<>();
        String tags = value(vehicle, "lossOutcomeTags");
        if (!tags.isBlank()) consequences.add("outcomes " + tags.replace(',', '/'));
        if (VehicleLossAuthority.isRoadBlocker(vehicle)) consequences.add("road obstruction");
        if (VehicleLossAuthority.hasLeakHazard(vehicle)) consequences.add("fuel or power leak");
        if ("true".equals(value(vehicle, "questObjective"))) consequences.add("recovery objective");
        if ("true".equals(value(vehicle, "strategicAssetLost"))) consequences.add("strategic asset loss");
        return consequences.isEmpty()
                ? "Loss and hazard state: no active persistent consequence recorded."
                : "Loss and hazard state: " + String.join(", ", consequences) + ".";
    }

    private static int referenceHeadlightRange(
            VehicleRuntimeAuthority.VehicleClass definition) {
        return switch (safe(definition)) {
            case UTILITY_BIKE -> 2;
            case CIVILIAN_CAR -> 3;
            case CARGO_TRUCK, ARMORED_CAR -> 4;
            case TANK -> 5;
        };
    }

    private static int referenceAudibleRange(
            VehicleRuntimeAuthority.VehicleClass definition) {
        return switch (safe(definition)) {
            case UTILITY_BIKE -> 12;
            case CIVILIAN_CAR -> 16;
            case CARGO_TRUCK -> 20;
            case ARMORED_CAR -> 22;
            case TANK -> 24;
        };
    }

    private static VehicleRuntimeAuthority.VehicleClass safe(
            VehicleRuntimeAuthority.VehicleClass definition) {
        return definition == null
                ? VehicleRuntimeAuthority.VehicleClass.CIVILIAN_CAR : definition;
    }

    private static String value(MapObjectState vehicle, String key) {
        return vehicle == null ? ""
                : MapObjectState.stockValue(vehicle.stockState, key);
    }

    private static String clean(String value, String fallback) {
        String text = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return text.isBlank() ? fallback : text.replace('|', '/');
    }
}
