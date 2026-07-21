package mechanist;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Cross-zone vehicle departure readiness and reservation. This authority owns
 * the persistent reservation ledger but deliberately does not load or replace
 * worlds; the existing transition commit authority consumes a READY reservation.
 */
final class VehicleStrategicTransitAuthority {
    private static final int HISTORY_LIMIT = 12;

    enum Infrastructure {
        ROAD,
        ALLEY,
        VEHICLE_LANE,
        GARAGE,
        DEPOT,
        RAMP,
        GATE,
        CHECKPOINT,
        PARKING_LOT,
        VEHICLE_YARD
    }

    enum Status {
        READY,
        BLOCKED,
        RESERVED
    }

    record Request(String destinationKey,
                   Set<Infrastructure> infrastructure,
                   int routeDistance,
                   int destinationParkingCapacity,
                   int assignedCrew,
                   boolean driverAssigned,
                   int fuelOrPowerAvailable,
                   int fuelOrPowerRequired,
                   boolean gateOpen,
                   boolean checkpointOpen,
                   boolean securityClosure,
                   Faction routeController,
                   String routeReason) {
        Request {
            destinationKey = clean(destinationKey, "unselected destination");
            infrastructure = infrastructure == null
                    ? Set.of() : Set.copyOf(infrastructure);
            routeDistance = Math.max(0, routeDistance);
            destinationParkingCapacity = Math.max(0,
                    destinationParkingCapacity);
            assignedCrew = Math.max(0, assignedCrew);
            fuelOrPowerAvailable = Math.max(0, fuelOrPowerAvailable);
            fuelOrPowerRequired = Math.max(0, fuelOrPowerRequired);
            routeController = routeController == null
                    ? Faction.NONE : routeController;
            routeReason = clean(routeReason, "vehicle transit");
        }
    }

    record Readiness(Status status, boolean allowed,
                     List<String> blockers, List<String> requirements,
                     int crewRequired, int fuelRequired,
                     String summary) { }

    record Reservation(boolean success, boolean changed,
                       String reservationId, Readiness readiness,
                       String message) { }

    private enum AuthorityMode { PLAYER, FACTION }

    private VehicleStrategicTransitAuthority() { }

    static Readiness evaluate(GamePanel game, MapObjectState vehicle,
                              Request request) {
        return evaluateInternal(game, vehicle, request,
                AuthorityMode.PLAYER, null);
    }

    static Readiness evaluateForFaction(GamePanel game,
                                        MapObjectState vehicle,
                                        Request request,
                                        NpcFactionSite site) {
        return evaluateInternal(game, vehicle, request,
                AuthorityMode.FACTION, site);
    }

    private static Readiness evaluateInternal(GamePanel game,
                                              MapObjectState vehicle,
                                              Request request,
                                              AuthorityMode authorityMode,
                                              NpcFactionSite site) {
        ArrayList<String> blockers = new ArrayList<>();
        ArrayList<String> requirements = new ArrayList<>();
        if (game == null || game.world == null || vehicle == null
                || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            blockers.add("a physical vehicle in the loaded zone is required");
            return result(false, blockers, requirements, 0, 0,
                    "Strategic vehicle transit is blocked because no loaded vehicle is available.");
        }
        VehicleRuntimeAuthority.ensureInitialized(game.world, vehicle);
        VehicleFuelAuthority.ensureInitialized(game.world, vehicle);
        Request route = safeRequest(request);
        VehicleRuntimeAuthority.Snapshot snapshot =
                VehicleRuntimeAuthority.inspect(game.world, vehicle);
        VehicleRuntimeAuthority.VehicleClass definition =
                snapshot.vehicleClass();
        int crewRequired = Math.max(1, definition.crewRequired);
        int fuelRequired = route.fuelOrPowerRequired() > 0
                ? route.fuelOrPowerRequired()
                : estimatedFuel(definition, route.routeDistance());

        if (authorityMode == AuthorityMode.PLAYER) {
            VehicleAccessAuthority.Decision operation =
                    VehicleAccessAuthority.evaluate(game, vehicle,
                            VehicleAccessAuthority.Permission.OPERATION);
            VehicleAccessAuthority.Decision deployment =
                    VehicleAccessAuthority.evaluate(game, vehicle,
                            VehicleAccessAuthority.Permission.DEPLOYMENT);
            if (!operation.allowed()) blockers.add(operation.requirement());
            if (!deployment.allowed()) blockers.add(deployment.requirement());
            if (route.routeController() != Faction.NONE
                    && !FactionIdentityAuthority.sameFamily(
                    route.routeController(), game.playerFaction())
                    && !credential(game, "Road transit permit")
                    && !credential(game,
                    route.routeController().label + " transit permit")) {
                blockers.add(route.routeController().label
                        + " controls the route; carry a matching road transit permit");
            }
            requirements.add("operation and deployment authority");
        } else {
            if (!localSite(site, game.world)) {
                blockers.add("a local faction motor pool is required");
            } else {
                VehicleMotorPoolAuthority.Snapshot pool =
                        VehicleMotorPoolAuthority.inspect(game, vehicle, site);
                if (snapshot.ownerType()
                        != VehicleRuntimeAuthority.OwnerType.FACTION
                        || snapshot.ownerFaction() == Faction.NONE
                        || !FactionIdentityAuthority.sameFamily(
                        snapshot.ownerFaction(), site.faction)) {
                    blockers.add("the vehicle must be owned by the dispatching faction family");
                }
                if (!pool.assigned() || !pool.siteLocal()) {
                    blockers.add("the vehicle must belong to the dispatching local motor pool");
                }
                if (!pool.ownerAligned()) {
                    blockers.add("restore aligned faction ownership before departure");
                }
                if (!pool.operational()) {
                    blockers.add("restore the vehicle to an operational condition");
                }
            }
            requirements.add("local faction motor-pool authority");
        }

        if (route.destinationKey().equals("unselected destination")) {
            blockers.add("select a destination zone or depot");
        }
        if (route.routeDistance() <= 0) {
            blockers.add("the route must have a positive infrastructure distance");
        }
        if (!route.driverAssigned()) {
            blockers.add("assign a driver before departure");
        }
        if (route.assignedCrew() < crewRequired) {
            blockers.add("assign " + crewRequired + " crew member(s); only "
                    + route.assignedCrew() + " are assigned");
        }
        if (route.fuelOrPowerAvailable() < fuelRequired) {
            blockers.add("reserve " + fuelRequired
                    + " fuel or power units; only "
                    + route.fuelOrPowerAvailable() + " are available");
        }
        if (route.securityClosure()) {
            blockers.add("the route is closed by an active security condition");
        }
        if (route.infrastructure().isEmpty()) {
            blockers.add("declare the road, lane, depot, gate, or yard infrastructure used by the route");
        } else {
            validateInfrastructure(definition, route, blockers, requirements);
        }
        if (!route.gateOpen()
                && route.infrastructure().contains(Infrastructure.GATE)) {
            blockers.add("the route gate is closed");
        }
        if (!route.checkpointOpen()
                && route.infrastructure().contains(
                Infrastructure.CHECKPOINT)) {
            blockers.add("the route checkpoint is closed");
        }
        if (route.destinationParkingCapacity() < footprint(definition)) {
            blockers.add("destination parking capacity "
                    + route.destinationParkingCapacity()
                    + " is below the vehicle footprint "
                    + footprint(definition));
        }
        if (VehicleLossAuthority.isRoadBlocker(vehicle)
                || VehicleLossAuthority.hasLeakHazard(vehicle)) {
            blockers.add("resolve the vehicle's active obstruction or leak hazard before departure");
        }

        requirements.add("driver assigned");
        requirements.add(crewRequired + " crew member(s)");
        requirements.add(fuelRequired + " fuel or power units");
        requirements.add("destination parking capacity "
                + footprint(definition));
        requirements.add("open gates and checkpoints where present");

        boolean allowed = blockers.isEmpty();
        String authorityLabel = authorityMode == AuthorityMode.FACTION
                ? "Faction strategic vehicle route"
                : "Strategic vehicle route";
        String summary = allowed
                ? authorityLabel + " is ready for reservation: "
                + displayName(vehicle) + " may travel "
                + route.routeDistance() + " infrastructure unit(s) to "
                + route.destinationKey() + " with "
                + route.assignedCrew() + " assigned crew and "
                + fuelRequired + " fuel or power units reserved."
                : authorityLabel + " is blocked: "
                + String.join("; ", blockers) + ".";
        return result(allowed, blockers, requirements, crewRequired,
                fuelRequired, summary);
    }

    static Reservation reserve(GamePanel game, MapObjectState vehicle,
                               Request request) {
        return reserveReady(game, vehicle, safeRequest(request),
                evaluate(game, vehicle, request));
    }

    static Reservation reserveForFaction(GamePanel game,
                                         MapObjectState vehicle,
                                         Request request,
                                         NpcFactionSite site) {
        return reserveReady(game, vehicle, safeRequest(request),
                evaluateForFaction(game, vehicle, request, site));
    }

    private static Reservation reserveReady(GamePanel game,
                                            MapObjectState vehicle,
                                            Request request,
                                            Readiness readiness) {
        if (!readiness.allowed()) {
            return new Reservation(false, false, "", readiness,
                    readiness.summary());
        }
        String existing = value(vehicle, "strategicTransitState");
        if ("reserved".equals(existing)) {
            String existingDestination = value(vehicle,
                    "strategicTransitDestination");
            if (!existingDestination.equals(request.destinationKey())) {
                Readiness blocked = result(false,
                        List.of("the vehicle already has a reservation to "
                                + clean(existingDestination,
                                "another destination")),
                        readiness.requirements(), readiness.crewRequired(),
                        readiness.fuelRequired(),
                        "Strategic vehicle route is blocked by a conflicting active reservation.");
                return new Reservation(false, false,
                        value(vehicle, "strategicTransitReservationId"),
                        blocked, blocked.summary());
            }
            return new Reservation(true, false,
                    value(vehicle, "strategicTransitReservationId"),
                    new Readiness(Status.RESERVED, true,
                            readiness.blockers(), readiness.requirements(),
                            readiness.crewRequired(), readiness.fuelRequired(),
                            "The vehicle already has the matching strategic transit reservation."),
                    "STRATEGIC TRANSIT: the matching reservation remains active; no fuel or world state was changed.");
        }
        String id = "TRANSIT-" + Math.abs((vehicle.id + "|"
                + request.destinationKey() + "|" + game.worldTurn).hashCode());
        set(vehicle, "strategicTransitState", "reserved");
        set(vehicle, "strategicTransitReservationId", id);
        set(vehicle, "strategicTransitOrigin", game.world.locationKey());
        set(vehicle, "strategicTransitDestination",
                request.destinationKey());
        set(vehicle, "strategicTransitDistance",
                Integer.toString(request.routeDistance()));
        set(vehicle, "strategicTransitCrew",
                Integer.toString(request.assignedCrew()));
        set(vehicle, "strategicTransitFuelReserved",
                Integer.toString(readiness.fuelRequired()));
        set(vehicle, "strategicTransitInfrastructure",
                infrastructureText(request.infrastructure()));
        set(vehicle, "strategicTransitController",
                request.routeController().name());
        set(vehicle, "strategicTransitReason", request.routeReason());
        set(vehicle, "strategicTransitReservedTurn",
                Long.toString(Math.max(0L, game.worldTurn)));
        append(vehicle, "deploymentHistory", "Strategic route reserved "
                + game.world.locationKey() + " -> "
                + request.destinationKey() + " / distance "
                + request.routeDistance() + " / fuel "
                + readiness.fuelRequired() + " / crew "
                + request.assignedCrew());
        return new Reservation(true, true, id,
                new Readiness(Status.RESERVED, true,
                        readiness.blockers(), readiness.requirements(),
                        readiness.crewRequired(), readiness.fuelRequired(),
                        "Strategic route reservation is ready for the world-transition owner."),
                "STRATEGIC TRANSIT RESERVED: " + displayName(vehicle)
                        + " is reserved from " + game.world.locationKey()
                        + " to " + request.destinationKey()
                        + ". Fuel or power is reserved in the vehicle ledger but is not consumed until the authoritative transition commits.");
    }

    static Reservation cancel(GamePanel game, MapObjectState vehicle,
                              String reason) {
        if (vehicle == null || !VehicleRuntimeAuthority.isVehicle(vehicle)
                || !"reserved".equals(value(vehicle,
                "strategicTransitState"))) {
            Readiness blocked = new Readiness(Status.BLOCKED, false,
                    List.of("no active strategic transit reservation"),
                    List.of(), 0, 0,
                    "No strategic transit reservation is active.");
            return new Reservation(false, false, "", blocked,
                    blocked.summary());
        }
        String id = value(vehicle, "strategicTransitReservationId");
        set(vehicle, "strategicTransitState", "cancelled");
        set(vehicle, "strategicTransitFuelReserved", "0");
        set(vehicle, "strategicTransitCancelledTurn",
                Long.toString(game == null ? 0L
                        : Math.max(0L, game.worldTurn)));
        append(vehicle, "deploymentHistory", "Strategic route reservation "
                + id + " cancelled / "
                + clean(reason, "route cancelled"));
        Readiness readiness = new Readiness(Status.BLOCKED, false,
                List.of("reservation cancelled"), List.of(), 0, 0,
                "The strategic transit reservation was cancelled before departure.");
        return new Reservation(true, true, id, readiness,
                "STRATEGIC TRANSIT CANCELLED: no fuel or world-transition state was consumed.");
    }

    static List<String> inspectionLines(GamePanel game,
                                        MapObjectState vehicle,
                                        Request request) {
        Readiness readiness = evaluate(game, vehicle, request);
        return inspectionLines(vehicle, readiness);
    }

    static List<String> inspectionLinesForFaction(GamePanel game,
                                                  MapObjectState vehicle,
                                                  Request request,
                                                  NpcFactionSite site) {
        Readiness readiness = evaluateForFaction(game, vehicle, request, site);
        return inspectionLines(vehicle, readiness);
    }

    private static List<String> inspectionLines(MapObjectState vehicle,
                                                Readiness readiness) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(readiness.summary());
        lines.add("Crew requirement: " + readiness.crewRequired()
                + "; fuel or power requirement: "
                + readiness.fuelRequired() + ".");
        if (!readiness.blockers().isEmpty()) {
            for (String blocker : readiness.blockers()) {
                lines.add("Route blocker: " + blocker + ".");
            }
        }
        String state = value(vehicle, "strategicTransitState");
        if (!state.isBlank()) {
            lines.add("Reservation state: " + state + "; destination "
                    + clean(value(vehicle,
                    "strategicTransitDestination"), "not recorded") + ".");
        }
        return List.copyOf(lines);
    }

    private static Request safeRequest(Request request) {
        return request == null
                ? new Request("unselected destination", Set.of(), 0, 0,
                0, false, 0, 0, false, false, false,
                Faction.NONE, "vehicle transit") : request;
    }

    private static void validateInfrastructure(
            VehicleRuntimeAuthority.VehicleClass definition,
            Request request, List<String> blockers,
            List<String> requirements) {
        Set<Infrastructure> infrastructure = request.infrastructure();
        boolean throughRoute = infrastructure.contains(Infrastructure.ROAD)
                || infrastructure.contains(Infrastructure.VEHICLE_LANE)
                || infrastructure.contains(Infrastructure.ALLEY);
        if (!throughRoute) {
            blockers.add("the route needs a road, vehicle lane, or legal alley through-route");
        }
        if ((definition == VehicleRuntimeAuthority.VehicleClass.CARGO_TRUCK
                || definition == VehicleRuntimeAuthority.VehicleClass.ARMORED_CAR
                || definition == VehicleRuntimeAuthority.VehicleClass.TANK)
                && infrastructure.contains(Infrastructure.ALLEY)
                && !infrastructure.contains(Infrastructure.VEHICLE_LANE)) {
            blockers.add("this heavy vehicle cannot rely on an alley without a declared vehicle lane");
        }
        if (definition == VehicleRuntimeAuthority.VehicleClass.TANK
                && !infrastructure.contains(Infrastructure.DEPOT)
                && !infrastructure.contains(Infrastructure.VEHICLE_YARD)) {
            blockers.add("the armored crawler route requires a depot or vehicle yard endpoint");
        }
        if (!infrastructure.contains(Infrastructure.PARKING_LOT)
                && !infrastructure.contains(Infrastructure.GARAGE)
                && !infrastructure.contains(Infrastructure.DEPOT)
                && !infrastructure.contains(Infrastructure.VEHICLE_YARD)) {
            blockers.add("declare a legal parking, garage, depot, or vehicle-yard destination");
        }
        if (infrastructure.contains(Infrastructure.RAMP)) {
            requirements.add("ramp load rating compatible with "
                    + definition.durabilityTier.toLowerCase(Locale.ROOT)
                    + " vehicle scale");
        }
    }

    private static Readiness result(boolean allowed,
                                    List<String> blockers,
                                    List<String> requirements,
                                    int crewRequired, int fuelRequired,
                                    String summary) {
        return new Readiness(allowed ? Status.READY : Status.BLOCKED,
                allowed, List.copyOf(new LinkedHashSet<>(blockers)),
                List.copyOf(new LinkedHashSet<>(requirements)),
                crewRequired, fuelRequired, summary);
    }

    private static int estimatedFuel(
            VehicleRuntimeAuthority.VehicleClass definition,
            int distance) {
        int divisor = switch (definition) {
            case UTILITY_BIKE -> 8;
            case CIVILIAN_CAR -> 6;
            case CARGO_TRUCK -> 4;
            case ARMORED_CAR -> 3;
            case TANK -> 2;
        };
        return Math.max(1, (Math.max(1, distance) + divisor - 1)
                / divisor);
    }

    private static int footprint(
            VehicleRuntimeAuthority.VehicleClass definition) {
        return switch (definition) {
            case UTILITY_BIKE -> 1;
            case CIVILIAN_CAR -> 2;
            case CARGO_TRUCK -> 3;
            case ARMORED_CAR -> 3;
            case TANK -> 4;
        };
    }

    private static boolean credential(GamePanel game, String expected) {
        if (game == null || expected == null || expected.isBlank()) return false;
        String needle = expected.toLowerCase(Locale.ROOT);
        for (String item : game.inventory) {
            if (item != null
                    && item.toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean localSite(NpcFactionSite site, World world) {
        return site != null && world != null
                && site.sectorX == world.sectorX
                && site.sectorY == world.sectorY
                && site.zoneX == world.zoneX
                && site.zoneY == world.zoneY
                && site.floor == world.floor;
    }

    private static String infrastructureText(Set<Infrastructure> values) {
        ArrayList<String> names = new ArrayList<>();
        for (Infrastructure value : values) names.add(value.name());
        names.sort(String::compareTo);
        return String.join(",", names);
    }

    private static String displayName(MapObjectState vehicle) {
        VehicleRuntimeAuthority.Snapshot snapshot =
                VehicleRuntimeAuthority.inspect(null, vehicle);
        if (snapshot == null) return "vehicle";
        return (clean(snapshot.manufacturer(), "") + " "
                + clean(snapshot.model(),
                snapshot.vehicleClass().label)).trim();
    }

    private static void append(MapObjectState vehicle, String key,
                               String entry) {
        ArrayList<String> entries = new ArrayList<>();
        String existing = value(vehicle, key);
        if (!existing.isBlank()) {
            for (String token : existing.split("~")) {
                String cleaned = clean(token, "");
                if (!cleaned.isBlank()) entries.add(cleaned);
            }
        }
        String cleanedEntry = clean(entry, "");
        if (!cleanedEntry.isBlank()) entries.add(cleanedEntry);
        while (entries.size() > HISTORY_LIMIT) entries.remove(0);
        set(vehicle, key, String.join("~", entries));
    }

    private static String value(MapObjectState vehicle, String key) {
        return vehicle == null ? ""
                : MapObjectState.stockValue(vehicle.stockState, key);
    }

    private static void set(MapObjectState vehicle, String key,
                            String value) {
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
