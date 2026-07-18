package mechanist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Persistent faction route-control orders over authoritative motor-pool vehicles.
 * This authority selects and reserves a real fleet asset for a mission, but does
 * not move it between worlds. Cross-zone movement remains owned by
 * VehicleStrategicTransitAuthority and its commit authority.
 */
final class FactionVehicleRouteControlAuthority {
    enum Mission {
        PATROL("route patrol"),
        CONVOY_ESCORT("convoy escort"),
        CHECKPOINT_REINFORCEMENT("checkpoint reinforcement"),
        ROUTE_CONTEST("contested route operation");

        final String label;
        Mission(String label) { this.label = label; }
    }

    record Request(Mission mission, String destinationKey,
                   Faction targetFaction,
                   Set<VehicleStrategicTransitAuthority.Infrastructure> infrastructure,
                   int routeDistance, int destinationParkingCapacity,
                   boolean gateOpen, boolean checkpointOpen,
                   boolean securityClosure, int aggression, int ambition,
                   String reason) {
        Request {
            mission = mission == null ? Mission.PATROL : mission;
            destinationKey = clean(destinationKey,
                    "unselected route-control destination");
            targetFaction = targetFaction == null ? Faction.NONE : targetFaction;
            infrastructure = infrastructure == null
                    ? Set.of() : Set.copyOf(infrastructure);
            routeDistance = Math.max(0, routeDistance);
            destinationParkingCapacity = Math.max(0,
                    destinationParkingCapacity);
            aggression = Math.max(0, aggression);
            ambition = Math.max(0, ambition);
            reason = clean(reason, mission.label);
        }
    }

    record Snapshot(boolean assigned, String orderId, Mission mission,
                    String destinationKey, Faction targetFaction,
                    String state, int strength, int requiredFuel,
                    String siteName, String reason,
                    List<String> history) { }

    record Result(boolean success, boolean changed, String action,
                  String message, MapObjectState vehicle,
                  Snapshot snapshot, List<String> blockers) {
        static Result blocked(String action, String message,
                              MapObjectState vehicle,
                              List<String> blockers) {
            return new Result(false, false, clean(action,
                    "route-control order"), clean(message,
                    "The route-control order was blocked."), vehicle,
                    inspect(vehicle), List.copyOf(new LinkedHashSet<>(
                    blockers == null ? List.of() : blockers)));
        }
    }

    private FactionVehicleRouteControlAuthority() { }

    static Result assign(GamePanel game, NpcFactionSite site,
                         Request request) {
        ArrayList<String> blockers = new ArrayList<>();
        if (game == null || game.world == null) {
            blockers.add("a loaded world is required");
            return Result.blocked("assign route control",
                    "Faction route control requires a loaded world.",
                    null, blockers);
        }
        if (!localSite(site, game.world)) {
            blockers.add("a local faction motor pool is required");
            return Result.blocked("assign route control",
                    "Faction route control requires a local motor pool.",
                    null, blockers);
        }
        Request order = request == null ? new Request(Mission.PATROL,
                "unselected route-control destination", Faction.NONE,
                Set.of(), 0, 0, false, false, false,
                0, 0, "route patrol") : request;
        validateOrderShape(order, blockers);
        if (!blockers.isEmpty()) {
            return Result.blocked("assign route control",
                    "The route-control order is incomplete: "
                            + String.join("; ", blockers) + ".",
                    null, blockers);
        }

        MapObjectState existing = matchingOrder(game, site, order);
        if (existing != null) {
            return new Result(true, false, "assign route control",
                    "The same route-control order is already assigned to "
                            + displayName(game.world, existing) + ".",
                    existing, inspect(existing), List.of());
        }

        if (order.mission() == Mission.ROUTE_CONTEST
                && order.targetFaction() != Faction.NONE
                && !FactionIdentityAuthority.sameFamily(
                order.targetFaction(), site.faction)) {
            FactionVehicleBalanceAuthority.Contest contest =
                    FactionVehicleBalanceAuthority.compare(game,
                            site.faction, order.targetFaction(), site);
            if (!contest.canEscalate(order.aggression(), order.ambition())) {
                blockers.add("leadership commitment "
                        + contest.commitment(order.aggression(),
                        order.ambition()) + "/"
                        + contest.escalationThreshold()
                        + " is below the fleet escalation requirement");
                blockers.add(contest.summary());
                return Result.blocked("assign route control",
                        site.faction.label
                                + " declined the contested route order. "
                                + contest.summary(), null, blockers);
            }
        }

        ArrayList<Candidate> candidates = new ArrayList<>();
        ArrayList<String> fleetBlockers = new ArrayList<>();
        for (MapObjectState vehicle : game.world.mapObjects) {
            if (!VehicleRuntimeAuthority.isVehicle(vehicle)
                    || !VehicleRuntimeAuthority.factionOwns(
                    vehicle, site.faction)) continue;
            VehicleRuntimeAuthority.ensureInitialized(game.world, vehicle);
            VehicleFuelAuthority.ensureInitialized(game.world, vehicle);
            Candidate candidate = candidate(game, vehicle, site, order);
            if (candidate.allowed()) candidates.add(candidate);
            else fleetBlockers.addAll(candidate.blockers());
        }
        candidates.sort(Comparator
                .comparingInt(Candidate::score).reversed()
                .thenComparing(candidate -> displayName(
                        game.world, candidate.vehicle()))
                .thenComparing(candidate -> clean(
                        candidate.vehicle().id,
                        candidate.vehicle().label)));
        if (candidates.isEmpty()) {
            blockers.addAll(fleetBlockers);
            if (blockers.isEmpty()) {
                blockers.add("the local faction owns no eligible motor-pool vehicle");
            }
            return Result.blocked("assign route control",
                    site.name + " has no vehicle ready for "
                            + order.mission().label + ": "
                            + String.join("; ", new LinkedHashSet<>(blockers))
                            + ".", null, blockers);
        }

        Candidate selected = candidates.get(0);
        MapObjectState vehicle = selected.vehicle();
        String id = "ROUTE-" + Math.abs((clean(vehicle.id,
                vehicle.label) + "|" + order.mission().name() + "|"
                + order.destinationKey() + "|" + game.worldTurn).hashCode());
        set(vehicle, "routeControlOrderState", "assigned");
        set(vehicle, "routeControlOrderId", id);
        set(vehicle, "routeControlOrderMission", order.mission().name());
        set(vehicle, "routeControlOrderDestination",
                order.destinationKey());
        set(vehicle, "routeControlOrderTargetFaction",
                order.targetFaction().name());
        set(vehicle, "routeControlOrderDistance",
                Integer.toString(order.routeDistance()));
        set(vehicle, "routeControlOrderParkingCapacity",
                Integer.toString(order.destinationParkingCapacity()));
        set(vehicle, "routeControlOrderInfrastructure",
                infrastructureText(order.infrastructure()));
        set(vehicle, "routeControlOrderRequiredFuel",
                Integer.toString(selected.requiredFuel()));
        set(vehicle, "routeControlOrderStrength",
                Integer.toString(selected.strength()));
        set(vehicle, "routeControlOrderSiteName", site.name);
        set(vehicle, "routeControlOrderReason", order.reason());
        set(vehicle, "routeControlOrderAssignedTurn",
                Long.toString(Math.max(0L, game.worldTurn)));
        append(vehicle, "routeControlOrderHistory", "Assigned "
                + order.mission().label + " to "
                + order.destinationKey() + " / strength "
                + selected.strength() + " / fuel "
                + selected.requiredFuel() + " / " + order.reason()
                + turn(game));
        append(vehicle, "deploymentHistory", "Route-control order "
                + id + " / " + order.mission().label + " / destination "
                + order.destinationKey() + " / strength "
                + selected.strength());
        return new Result(true, true, "assign route control",
                "ROUTE CONTROL: " + displayName(game.world, vehicle)
                        + " assigned to " + order.mission().label + " at "
                        + order.destinationKey() + "; deployment strength "
                        + selected.strength() + ".",
                vehicle, inspect(vehicle), List.of());
    }

    static VehicleStrategicTransitAuthority.Request transitRequest(
            GamePanel game, MapObjectState vehicle, Request request) {
        Request order = request == null ? new Request(Mission.PATROL,
                "unselected route-control destination", Faction.NONE,
                Set.of(), 0, 0, false, false, false,
                0, 0, "route patrol") : request;
        World world = game == null ? null : game.world;
        VehicleManifestAuthority.Snapshot manifest =
                VehicleManifestAuthority.inspect(world, vehicle);
        VehicleFuelAuthority.Snapshot fuel =
                VehicleFuelAuthority.inspect(world, vehicle);
        int requiredFuel = intValue(value(vehicle,
                "routeControlOrderRequiredFuel"),
                estimatedFuel(VehicleRuntimeAuthority.vehicleClass(
                        vehicle == null ? null : vehicle.type),
                        order.routeDistance()));
        return new VehicleStrategicTransitAuthority.Request(
                order.destinationKey(), order.infrastructure(),
                order.routeDistance(), order.destinationParkingCapacity(),
                manifest.assignedCrew(), !manifest.driver().isBlank(),
                fuel.current(), requiredFuel, order.gateOpen(),
                order.checkpointOpen(), order.securityClosure(),
                order.targetFaction(), "faction " + order.mission().label
                + " / " + order.reason());
    }

    static Result activate(GamePanel game, NpcFactionSite site,
                           MapObjectState vehicle) {
        Snapshot before = inspect(vehicle);
        if (!before.assigned()) {
            return Result.blocked("activate route control",
                    "The vehicle has no assigned route-control order.",
                    vehicle, List.of("assign a route-control order first"));
        }
        VehicleMotorPoolAuthority.Snapshot pool =
                VehicleMotorPoolAuthority.inspect(game, vehicle, site);
        if (!pool.siteLocal() || !pool.ownerAligned()) {
            return Result.blocked("activate route control",
                    "Only the assigned local motor pool may activate this order.",
                    vehicle, List.of("restore local motor-pool custody"));
        }
        String transitState = value(vehicle, "strategicTransitState")
                .toLowerCase(Locale.ROOT);
        String transitDestination = value(vehicle,
                "strategicTransitDestination");
        if (!transitState.equals("reserved")
                || !before.destinationKey().equals(transitDestination)) {
            return Result.blocked("activate route control",
                    "Create the matching strategic-transit reservation before activation.",
                    vehicle, List.of("reserve strategic transit to "
                    + before.destinationKey()));
        }
        if ("active".equalsIgnoreCase(before.state())) {
            return new Result(true, false, "activate route control",
                    "The route-control order is already active.",
                    vehicle, before, List.of());
        }
        set(vehicle, "routeControlOrderState", "active");
        set(vehicle, "routeControlOrderActivatedTurn",
                Long.toString(game == null ? 0L
                        : Math.max(0L, game.worldTurn)));
        append(vehicle, "routeControlOrderHistory", "Activated "
                + before.mission().label + " / transit reservation "
                + clean(value(vehicle, "strategicTransitReservationId"),
                "unrecorded") + turn(game));
        append(vehicle, "deploymentHistory", "Route-control order "
                + before.orderId() + " activated");
        return new Result(true, true, "activate route control",
                "ROUTE CONTROL ACTIVE: " + displayName(
                game == null ? null : game.world, vehicle)
                        + " is committed to " + before.destinationKey() + ".",
                vehicle, inspect(vehicle), List.of());
    }

    static Result cancel(GamePanel game, NpcFactionSite site,
                         MapObjectState vehicle, String reason) {
        Snapshot before = inspect(vehicle);
        if (!before.assigned()) {
            return new Result(true, false, "cancel route control",
                    "The vehicle has no active route-control order.",
                    vehicle, before, List.of());
        }
        VehicleMotorPoolAuthority.Snapshot pool =
                VehicleMotorPoolAuthority.inspect(game, vehicle, site);
        if (!pool.siteLocal() || !pool.ownerAligned()) {
            return Result.blocked("cancel route control",
                    "Only the assigned local motor pool may cancel this order.",
                    vehicle, List.of("restore local motor-pool custody"));
        }
        String transit = value(vehicle, "strategicTransitState")
                .toLowerCase(Locale.ROOT);
        if (transit.equals("reserved") || transit.equals("committing")) {
            return Result.blocked("cancel route control",
                    "Cancel or complete the strategic-transit reservation first.",
                    vehicle, List.of("close strategic transit before cancelling the order"));
        }
        set(vehicle, "routeControlOrderState", "cancelled");
        set(vehicle, "routeControlOrderCancelledTurn",
                Long.toString(game == null ? 0L
                        : Math.max(0L, game.worldTurn)));
        append(vehicle, "routeControlOrderHistory", "Cancelled "
                + before.mission().label + " / "
                + clean(reason, "route-control order cancelled")
                + turn(game));
        append(vehicle, "deploymentHistory", "Route-control order "
                + before.orderId() + " cancelled / "
                + clean(reason, "order cancelled"));
        return new Result(true, true, "cancel route control",
                "ROUTE CONTROL CANCELLED: " + clean(reason,
                "the deployment order ended") + ".",
                vehicle, inspect(vehicle), List.of());
    }

    static Result complete(GamePanel game, NpcFactionSite site,
                           MapObjectState vehicle, String outcome) {
        Snapshot before = inspect(vehicle);
        if (!before.assigned()) {
            return Result.blocked("complete route control",
                    "The vehicle has no route-control order to complete.",
                    vehicle, List.of("assign and activate a route-control order"));
        }
        VehicleMotorPoolAuthority.Snapshot pool =
                VehicleMotorPoolAuthority.inspect(game, vehicle, site);
        if (!pool.ownerAligned()) {
            return Result.blocked("complete route control",
                    "The vehicle is no longer aligned with its motor-pool faction.",
                    vehicle, List.of("restore aligned faction ownership"));
        }
        String transit = value(vehicle, "strategicTransitState")
                .toLowerCase(Locale.ROOT);
        if (transit.equals("reserved") || transit.equals("committing")) {
            return Result.blocked("complete route control",
                    "The strategic transfer must finish before the mission closes.",
                    vehicle, List.of("complete the strategic transfer"));
        }
        set(vehicle, "routeControlOrderState", "completed");
        set(vehicle, "routeControlOrderCompletedTurn",
                Long.toString(game == null ? 0L
                        : Math.max(0L, game.worldTurn)));
        append(vehicle, "routeControlOrderHistory", "Completed "
                + before.mission().label + " / "
                + clean(outcome, "route objective secured") + turn(game));
        append(vehicle, "deploymentHistory", "Route-control order "
                + before.orderId() + " completed / "
                + clean(outcome, "objective secured"));
        return new Result(true, true, "complete route control",
                "ROUTE CONTROL COMPLETE: " + clean(outcome,
                "the route objective was secured") + ".",
                vehicle, inspect(vehicle), List.of());
    }

    static Snapshot inspect(MapObjectState vehicle) {
        String state = clean(value(vehicle, "routeControlOrderState"),
                "unassigned");
        Mission mission = parseMission(value(vehicle,
                "routeControlOrderMission"));
        return new Snapshot(state.equalsIgnoreCase("assigned")
                || state.equalsIgnoreCase("active"),
                value(vehicle, "routeControlOrderId"), mission,
                value(vehicle, "routeControlOrderDestination"),
                parseFaction(value(vehicle,
                        "routeControlOrderTargetFaction")),
                state, intValue(value(vehicle,
                "routeControlOrderStrength"), 0),
                intValue(value(vehicle,
                "routeControlOrderRequiredFuel"), 0),
                value(vehicle, "routeControlOrderSiteName"),
                value(vehicle, "routeControlOrderReason"),
                history(vehicle));
    }

    static List<String> inspectionLines(MapObjectState vehicle) {
        Snapshot snapshot = inspect(vehicle);
        ArrayList<String> lines = new ArrayList<>();
        if (!snapshot.assigned()
                && !"completed".equalsIgnoreCase(snapshot.state())
                && !"cancelled".equalsIgnoreCase(snapshot.state())) {
            lines.add("Route-control order: none assigned.");
            return List.copyOf(lines);
        }
        lines.add("Route-control order: " + snapshot.mission().label
                + " to " + clean(snapshot.destinationKey(),
                "an unrecorded destination") + "; state "
                + snapshot.state() + ".");
        lines.add("Deployment strength: " + snapshot.strength()
                + "; reserved fuel or power requirement: "
                + snapshot.requiredFuel() + ".");
        if (snapshot.targetFaction() != Faction.NONE) {
            lines.add("Opposing route interest: "
                    + snapshot.targetFaction().label + ".");
        }
        return List.copyOf(lines);
    }

    private record Candidate(MapObjectState vehicle, boolean allowed,
                             int score, int strength, int requiredFuel,
                             List<String> blockers) { }

    private static Candidate candidate(GamePanel game,
                                       MapObjectState vehicle,
                                       NpcFactionSite site,
                                       Request request) {
        ArrayList<String> blockers = new ArrayList<>();
        VehicleMotorPoolAuthority.Snapshot pool =
                VehicleMotorPoolAuthority.inspect(game, vehicle, site);
        VehicleManifestAuthority.Snapshot manifest =
                VehicleManifestAuthority.inspect(game.world, vehicle);
        VehicleFuelAuthority.Snapshot fuel =
                VehicleFuelAuthority.inspect(game.world, vehicle);
        VehicleRuntimeAuthority.Snapshot runtime =
                VehicleRuntimeAuthority.inspect(game.world, vehicle);
        FactionVehicleDoctrineAuthority.VehicleAssessment assessment =
                FactionVehicleDoctrineAuthority.assess(game, vehicle, site);
        Snapshot order = inspect(vehicle);
        int requiredFuel = estimatedFuel(runtime.vehicleClass(),
                request.routeDistance());

        if (!pool.assigned() || !pool.siteLocal()) {
            blockers.add(displayName(game.world, vehicle)
                    + " is not assigned to this local motor pool");
        }
        if (!pool.ownerAligned() || !pool.operational()) {
            blockers.add(displayName(game.world, vehicle)
                    + " lacks aligned operational custody");
        }
        if (!manifest.driver().isBlank()) {
            // driver present
        } else {
            blockers.add(displayName(game.world, vehicle)
                    + " has no assigned driver");
        }
        if (manifest.assignedCrew() < pool.requiredCrew()) {
            blockers.add(displayName(game.world, vehicle)
                    + " has " + manifest.assignedCrew() + "/"
                    + pool.requiredCrew() + " required crew");
        }
        if (fuel.current() < requiredFuel) {
            blockers.add(displayName(game.world, vehicle)
                    + " has " + fuel.current() + "/" + requiredFuel
                    + " required fuel or power units");
        }
        if (assessment.readiness() < 75) {
            blockers.add(displayName(game.world, vehicle)
                    + " readiness is only " + assessment.readiness()
                    + "%");
        }
        if (order.assigned()) {
            blockers.add(displayName(game.world, vehicle)
                    + " already has an active route-control order");
        }
        String transit = value(vehicle, "strategicTransitState")
                .toLowerCase(Locale.ROOT);
        if (transit.equals("reserved") || transit.equals("committing")) {
            blockers.add(displayName(game.world, vehicle)
                    + " already has a strategic-transit commitment");
        }
        if ("running".equalsIgnoreCase(runtime.operationState())) {
            blockers.add(displayName(game.world, vehicle)
                    + " is already operating");
        }
        if (VehicleLossAuthority.isRoadBlocker(vehicle)
                || VehicleLossAuthority.hasLeakHazard(vehicle)) {
            blockers.add(displayName(game.world, vehicle)
                    + " has an unresolved obstruction or leak hazard");
        }
        validateVehicleInfrastructure(runtime.vehicleClass(), request,
                blockers);

        int missionValue = missionValue(assessment, request.mission());
        int score = assessment.strategicValue() * 2
                + assessment.readiness() + assessment.doctrineFit()
                + missionValue * 6;
        int strength = Math.max(1, missionValue * 2
                + assessment.readiness() / 5
                + assessment.strategicValue() / 4);
        return new Candidate(vehicle, blockers.isEmpty(), score,
                strength, requiredFuel,
                List.copyOf(new LinkedHashSet<>(blockers)));
    }

    private static int missionValue(
            FactionVehicleDoctrineAuthority.VehicleAssessment assessment,
            Mission mission) {
        Map<FactionVehicleDoctrineAuthority.Dimension, Integer> values =
                assessment.contribution();
        return switch (mission) {
            case PATROL -> value(values,
                    FactionVehicleDoctrineAuthority.Dimension.PATROL) * 5
                    + value(values,
                    FactionVehicleDoctrineAuthority.Dimension.ROUTE_CONTROL) * 4
                    + value(values,
                    FactionVehicleDoctrineAuthority.Dimension.DEFENSE) * 2;
            case CONVOY_ESCORT -> value(values,
                    FactionVehicleDoctrineAuthority.Dimension.CONVOY) * 5
                    + value(values,
                    FactionVehicleDoctrineAuthority.Dimension.DEFENSE) * 3
                    + value(values,
                    FactionVehicleDoctrineAuthority.Dimension.ROUTE_CONTROL) * 2;
            case CHECKPOINT_REINFORCEMENT -> value(values,
                    FactionVehicleDoctrineAuthority.Dimension.ROUTE_CONTROL) * 5
                    + value(values,
                    FactionVehicleDoctrineAuthority.Dimension.DEFENSE) * 4
                    + value(values,
                    FactionVehicleDoctrineAuthority.Dimension.INTIMIDATION) * 2;
            case ROUTE_CONTEST -> value(values,
                    FactionVehicleDoctrineAuthority.Dimension.ROUTE_CONTROL) * 5
                    + value(values,
                    FactionVehicleDoctrineAuthority.Dimension.ASSAULT) * 3
                    + value(values,
                    FactionVehicleDoctrineAuthority.Dimension.STRATEGIC_PROJECTION) * 3;
        };
    }

    private static int value(
            Map<FactionVehicleDoctrineAuthority.Dimension, Integer> values,
            FactionVehicleDoctrineAuthority.Dimension dimension) {
        return values.getOrDefault(dimension, 0);
    }

    private static void validateOrderShape(Request request,
                                           List<String> blockers) {
        if (request.destinationKey().equals(
                "unselected route-control destination")) {
            blockers.add("select a destination route or checkpoint");
        }
        if (request.routeDistance() <= 0) {
            blockers.add("the route distance must be positive");
        }
        if (request.destinationParkingCapacity() <= 0) {
            blockers.add("declare destination vehicle capacity");
        }
        if (request.infrastructure().isEmpty()) {
            blockers.add("declare route infrastructure");
        }
        if (request.securityClosure()) {
            blockers.add("the route is under an active security closure");
        }
        if (request.infrastructure().contains(
                VehicleStrategicTransitAuthority.Infrastructure.GATE)
                && !request.gateOpen()) {
            blockers.add("the route gate is closed");
        }
        if (request.infrastructure().contains(
                VehicleStrategicTransitAuthority.Infrastructure.CHECKPOINT)
                && !request.checkpointOpen()) {
            blockers.add("the route checkpoint is closed");
        }
        if (request.mission() == Mission.ROUTE_CONTEST
                && request.targetFaction() == Faction.NONE) {
            blockers.add("identify the faction contesting the route");
        }
    }

    private static void validateVehicleInfrastructure(
            VehicleRuntimeAuthority.VehicleClass definition,
            Request request, List<String> blockers) {
        Set<VehicleStrategicTransitAuthority.Infrastructure> infrastructure =
                request.infrastructure();
        boolean throughRoute = infrastructure.contains(
                VehicleStrategicTransitAuthority.Infrastructure.ROAD)
                || infrastructure.contains(
                VehicleStrategicTransitAuthority.Infrastructure.VEHICLE_LANE)
                || infrastructure.contains(
                VehicleStrategicTransitAuthority.Infrastructure.ALLEY);
        if (!throughRoute) {
            blockers.add("the mission has no road, lane, or legal alley through-route");
        }
        if ((definition == VehicleRuntimeAuthority.VehicleClass.CARGO_TRUCK
                || definition == VehicleRuntimeAuthority.VehicleClass.ARMORED_CAR
                || definition == VehicleRuntimeAuthority.VehicleClass.TANK)
                && infrastructure.contains(
                VehicleStrategicTransitAuthority.Infrastructure.ALLEY)
                && !infrastructure.contains(
                VehicleStrategicTransitAuthority.Infrastructure.VEHICLE_LANE)) {
            blockers.add(displayClass(definition)
                    + " cannot rely on an alley without a vehicle lane");
        }
        if (definition == VehicleRuntimeAuthority.VehicleClass.TANK
                && !infrastructure.contains(
                VehicleStrategicTransitAuthority.Infrastructure.DEPOT)
                && !infrastructure.contains(
                VehicleStrategicTransitAuthority.Infrastructure.VEHICLE_YARD)) {
            blockers.add("armored crawler deployment requires a depot or vehicle yard endpoint");
        }
        if (request.destinationParkingCapacity() < footprint(definition)) {
            blockers.add("destination capacity "
                    + request.destinationParkingCapacity()
                    + " is below the " + displayClass(definition)
                    + " footprint " + footprint(definition));
        }
    }

    private static MapObjectState matchingOrder(GamePanel game,
                                                NpcFactionSite site,
                                                Request request) {
        if (game == null || game.world == null
                || game.world.mapObjects == null) return null;
        for (MapObjectState vehicle : game.world.mapObjects) {
            if (!VehicleRuntimeAuthority.isVehicle(vehicle)
                    || !VehicleRuntimeAuthority.factionOwns(
                    vehicle, site.faction)) continue;
            Snapshot snapshot = inspect(vehicle);
            if (snapshot.assigned()
                    && snapshot.mission() == request.mission()
                    && snapshot.destinationKey().equals(
                    request.destinationKey())
                    && snapshot.siteName().equals(site.name)) {
                return vehicle;
            }
        }
        return null;
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

    private static String infrastructureText(
            Set<VehicleStrategicTransitAuthority.Infrastructure> values) {
        ArrayList<String> names = new ArrayList<>();
        for (VehicleStrategicTransitAuthority.Infrastructure value : values) {
            names.add(value.name());
        }
        names.sort(String::compareTo);
        return String.join(",", names);
    }

    private static int activeStrength(GamePanel game, Faction faction) {
        if (game == null || game.world == null
                || game.world.mapObjects == null) return 0;
        int total = 0;
        for (MapObjectState vehicle : game.world.mapObjects) {
            if (!VehicleRuntimeAuthority.isVehicle(vehicle)
                    || !VehicleRuntimeAuthority.factionOwns(vehicle,
                    faction)) continue;
            Snapshot snapshot = inspect(vehicle);
            if (snapshot.state().equalsIgnoreCase("active")) {
                total += snapshot.strength();
            } else if (snapshot.state().equalsIgnoreCase("assigned")) {
                total += Math.max(1, snapshot.strength() / 2);
            }
        }
        return total;
    }

    static int committedStrength(GamePanel game, Faction faction) {
        return activeStrength(game,
                FactionIdentityAuthority.strategicFamily(faction));
    }

    private static boolean localSite(NpcFactionSite site, World world) {
        return site != null && world != null
                && site.sectorX == world.sectorX
                && site.sectorY == world.sectorY
                && site.zoneX == world.zoneX
                && site.zoneY == world.zoneY
                && site.floor == world.floor;
    }

    private static Mission parseMission(String value) {
        try {
            return Mission.valueOf(clean(value, "PATROL")
                    .toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return Mission.PATROL;
        }
    }

    private static Faction parseFaction(String value) {
        try {
            return Faction.valueOf(clean(value, "NONE")
                    .toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return Faction.NONE;
        }
    }

    private static List<String> history(MapObjectState vehicle) {
        ArrayList<String> history = new ArrayList<>();
        String text = value(vehicle, "routeControlOrderHistory");
        if (!text.isBlank()) {
            for (String token : text.split("~")) {
                String cleaned = clean(token, "");
                if (!cleaned.isBlank()) history.add(cleaned);
            }
        }
        return List.copyOf(history);
    }

    private static String displayName(World world,
                                      MapObjectState vehicle) {
        VehicleRuntimeAuthority.Snapshot snapshot =
                VehicleRuntimeAuthority.inspect(world, vehicle);
        if (snapshot == null) return "vehicle";
        return (clean(snapshot.manufacturer(), "") + " "
                + clean(snapshot.model(),
                snapshot.vehicleClass().label)).trim();
    }

    private static String displayClass(
            VehicleRuntimeAuthority.VehicleClass definition) {
        return definition == null ? "vehicle" : definition.label;
    }

    private static void append(MapObjectState vehicle, String key,
                               String entry) {
        String existing = value(vehicle, key);
        set(vehicle, key, existing.isBlank() ? clean(entry, "")
                : existing + "~" + clean(entry, ""));
    }

    private static String value(MapObjectState vehicle, String key) {
        return vehicle == null ? ""
                : MapObjectState.stockValue(vehicle.stockState, key);
    }

    private static void set(MapObjectState vehicle, String key,
                            String value) {
        vehicle.stockState = MapObjectState.setStockFlag(
                vehicle.stockState, key,
                clean(value, "").replace(';', ',').replace('|', '/'));
    }

    private static int intValue(String value, int fallback) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String turn(GamePanel game) {
        return " / turn " + (game == null ? 0
                : Math.max(0, game.turn));
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? ""
                : value.trim().replaceAll("\\s+", " ");
        return cleaned.isBlank() ? (fallback == null ? "" : fallback)
                : cleaned;
    }
}
