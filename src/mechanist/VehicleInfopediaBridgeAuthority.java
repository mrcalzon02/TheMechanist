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

    static String title(VehicleRuntimeAuthority.VehicleClass definition) {
        return safe(definition).label + " vehicle dossier";
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
                    title(definition), List.copyOf(dossierLines(definition))));
        }
        dossiers.sort(Comparator.comparing(ClassDossier::title));
        return List.copyOf(dossiers);
    }

    static ArrayList<String> dossierLines(
            VehicleRuntimeAuthority.VehicleClass definition) {
        VehicleRuntimeAuthority.VehicleClass safe = safe(definition);
        VehicleOperationFeedbackAuthority.FeedbackProfile feedback =
                VehicleOperationFeedbackAuthority.profileForClass(safe);
        ArrayList<String> lines = new ArrayList<>();
        lines.add(title(safe));
        lines.add("Role and scale: " + safe.role + "; durability "
                + safe.durabilityTier + "; legal class " + safe.legalClass + ".");
        lines.add("Capacity: " + safe.seats + " seat(s), "
                + safe.crewRequired + " operational crew required, "
                + safe.cargoCapacity + " cargo unit(s).");
        lines.add("Energy capacity: " + VehicleFuelAuthority.capacity(safe)
                + " fuel or power unit(s).");
        lines.add("Local road efficiency: up to "
                + VehicleFuelAuthority.tilesPerEnergyUnit(safe)
                + " road tile(s) per fuel or power unit, subject to road and parking access.");
        lines.add("Ordinary manufacturers: "
                + String.join(", ", safe.manufacturers) + ".");
        lines.add("Registered models: " + String.join(", ", safe.models) + ".");
        lines.add("Reference value: " + safe.purchasePrice
                + " script purchase baseline; " + safe.salvageBase
                + " machine-part salvage baseline before condition and custody rules.");
        lines.add("Sensory profile: headlight range up to "
                + feedback.headlightRange() + " tile(s); ambient operating sound carries up to "
                + feedback.audibleRange() + " tile(s) when enabled and unobstructed by mute settings.");
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
        lines.add("Production identity: "
                + clean(runtime.productionBatch(), "unrecorded batch") + ".");
        lines.add("Custody: " + runtime.ownerType().name().toLowerCase()
                + " / " + clean(runtime.ownerName(), "unrecorded owner")
                + (runtime.ownerFaction() == null || runtime.ownerFaction() == Faction.NONE
                ? "" : " / " + runtime.ownerFaction().label) + ".");
        lines.add("Condition: " + runtime.condition() + "; operation "
                + runtime.operationState() + "; overall integrity "
                + runtime.integrity() + "%; " + componentSummary(runtime.components()) + ".");
        lines.add("Energy: " + fuel.current() + "/" + fuel.capacity() + " "
                + fuel.energyType() + " unit(s); " + fuel.reserved()
                + " reserved for strategic transit; " + fuel.available()
                + " available for local and new strategic routes; state "
                + fuel.state() + ".");
        lines.add("Estimated local road range: up to "
                + VehicleFuelAuthority.localRoadRangeTiles(runtime.vehicleClass(), fuel.available())
                + " tile(s) on " + fuel.available()
                + " unreserved unit(s); reachable roads, parking and condition may limit travel.");
        lines.add("Refill: after repairs, use a nearby service garage for up to "
                + VehicleFuelAuthority.GARAGE_REFUEL_BATCH_UNITS
                + " fuel or power unit(s) per visit at "
                + VehicleFuelAuthority.GARAGE_REFUEL_SCRIPT_PER_UNIT
                + " script each; partial purchases cost only what you can afford.");
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
        lines.add(strategicLine(vehicle, fuel));
        lines.add(cancellationGuidance(game, vehicle, runtime));
        if ("reserved".equals(value(vehicle, "strategicTransitState"))) {
            String origin = value(vehicle, "strategicTransitOrigin");
            if (game != null && game.world != null && !origin.isBlank()
                    && !origin.equals(Integer.toString(game.world.locationKey()))) {
                lines.add("Reservation recovery: saved departure does not match this zone. "
                        + "An authorized owner or registered faction yard may cancel and "
                        + "rebook; cancellation does not consume fuel or move the vehicle.");
            }
            if (fuel.reserved() <= 0) {
                lines.add("Departure warning: no fuel or power is held for this reservation. "
                        + "Cancel the stale reservation and reserve the route again.");
            } else if (fuel.current() < fuel.reserved()) {
                lines.add("Departure warning: reserved route is underfunded by "
                        + (fuel.reserved() - fuel.current())
                        + " fuel or power unit(s) (" + fuel.current()
                        + " in the vehicle, " + fuel.reserved()
                        + " reserved). Refuel before departure.");
            } else {
                lines.add("Reservation note: held fuel is not spent until the transfer "
                        + "commits; destination parking and transfer conditions "
                        + "must still be valid at departure.");
            }
        }
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
        lines.add("Infopedia reference: " + title(runtime.vehicleClass())
                + ". This live dashboard reflects the physical vehicle and does not grant authority.");
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

    private static String strategicLine(MapObjectState vehicle,
                                        VehicleFuelAuthority.Snapshot fuel) {
        String state = value(vehicle, "strategicTransitState");
        if (state.isBlank()) {
            return "Strategic transit: no active reservation. "
                    + "Select a destination and confirm departure readiness.";
        }
        String destination = clean(value(vehicle,
                "strategicTransitDestination"), "not recorded");
        if (!"reserved".equals(state)) {
            return "Strategic transit: " + state
                    + "; last selected destination " + destination
                    + "; no active fuel reservation.";
        }
        String distance = clean(value(vehicle, "strategicTransitDistance"),
                "unrecorded");
        String crew = clean(value(vehicle, "strategicTransitCrew"), "unrecorded");
        return "Strategic transit: reserved; destination " + destination
                + "; planned distance " + distance + " infrastructure unit(s)"
                + "; recorded crew " + crew
                + "; " + fuel.reserved()
                + " fuel or power unit(s) held, not spent.";
    }

    /**
     * Reuse the same read-only authorization checks as the real cancellation
     * commands. A registered faction booking is not a player-owned booking.
     */
    private static String cancellationGuidance(GamePanel game,
                                               MapObjectState vehicle,
                                               VehicleRuntimeAuthority.Snapshot runtime) {
        if (!"reserved".equals(value(vehicle, "strategicTransitState"))) {
            return "Cancellation eligibility: no active strategic reservation to cancel.";
        }
        VehicleStrategicTransitAuthority.CancellationEligibility player =
                VehicleStrategicTransitAuthority.playerCancellationEligibility(
                        game, vehicle);
        if (player.allowed()) {
            return "Cancellation eligibility: recorded player owner may cancel "
                    + "this reservation; held fuel is released without being spent.";
        }
        if (runtime.ownerType() == VehicleRuntimeAuthority.OwnerType.FACTION) {
            if (game != null && game.npcFactionSites != null) {
                for (NpcFactionSite site : game.npcFactionSites) {
                    if (VehicleStrategicTransitAuthority.factionCancellationEligibility(
                            game, vehicle, site).allowed()) {
                        return "Cancellation eligibility: registered local faction motor "
                                + "pool may cancel; player cancellation is not authorized.";
                    }
                }
            }
            return "Cancellation eligibility: player cancellation is not authorized; "
                    + "the registered local faction motor pool must be present.";
        }
        return "Cancellation eligibility: unavailable to current player. "
                + player.reason();
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
