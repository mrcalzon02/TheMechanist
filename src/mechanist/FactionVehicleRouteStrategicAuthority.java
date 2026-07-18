package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Adapts ordinary faction strategy plans into authoritative vehicle route-control
 * deployments. The strategic plan remains the scheduler, the route-control
 * authority remains the order owner, and the strategic-transit authority remains
 * the reservation owner. Staging is pending work; only an authoritative completed
 * transfer may close the strategic plan successfully.
 */
final class FactionVehicleRouteStrategicAuthority {
    static final String VEHICLE_ROUTE_CONTROL_GOAL =
            "deploy vehicle route control";

    record Suggestion(String goal, String target, String reason) {
        static Suggestion none() { return new Suggestion("", "", ""); }
        boolean available() { return goal != null && !goal.isBlank(); }
    }

    record Progress(boolean handled, boolean terminal, boolean success,
                    boolean changed, String blocker, String message,
                    int stockBefore, int stockAfter,
                    MapObjectState vehicle) {
        static Progress notHandled(String message) {
            return new Progress(false, false, false, false,
                    "not-handled", clean(message,
                    "Faction route-control strategy did not handle this plan."),
                    0, 0, null);
        }

        static Progress pending(boolean changed, String message,
                                int stockBefore, int stockAfter,
                                MapObjectState vehicle) {
            return new Progress(true, false, false, changed,
                    "route-control-pending", clean(message,
                    "The route-control deployment remains in progress."),
                    stockBefore, stockAfter, vehicle);
        }

        static Progress terminal(boolean success, boolean changed,
                                 String blocker, String message,
                                 int stockBefore, int stockAfter,
                                 MapObjectState vehicle) {
            return new Progress(true, true, success, changed,
                    clean(blocker, success ? "" :
                    "route-control-operation-blocked"),
                    clean(message, success
                            ? "The route-control deployment completed."
                            : "The route-control deployment failed."),
                    stockBefore, stockAfter, vehicle);
        }

        boolean pending() { return handled && !terminal; }

        FactionStrategicAssetAuthority.Outcome outcome() {
            if (!handled) {
                return FactionStrategicAssetAuthority.Outcome.notHandled(
                        message);
            }
            return new FactionStrategicAssetAuthority.Outcome(
                    true, terminal && success, blocker, message,
                    -1, stockBefore, stockAfter, null, null);
        }
    }

    private record VehicleLedger(MapObjectState vehicle, String stockState) { }

    private FactionVehicleRouteStrategicAuthority() { }

    static boolean handles(FactionStrategicPlan plan) {
        return plan != null && plan.immediateGoal != null
                && VEHICLE_ROUTE_CONTROL_GOAL.equalsIgnoreCase(
                plan.immediateGoal.trim());
    }

    static Suggestion nextSuggestion(GamePanel game, NpcFactionSite site,
                                     FactionStrategicPlan plan) {
        if (game == null || game.world == null || plan == null
                || !localSite(site, game.world)
                || !routeIntent(plan)) return Suggestion.none();
        String destination = destination(plan);
        if (destination.isBlank()) return Suggestion.none();

        FactionVehicleDoctrineAuthority.FleetSnapshot fleet =
                FactionVehicleDoctrineAuthority.fleet(
                game, site.faction, site);
        if (fleet.readyVehicles() <= 0) return Suggestion.none();

        FactionVehicleRouteControlAuthority.Request request = request(plan);
        MapObjectState conflict = objectiveVehicle(game, site, request);
        if (conflict != null) return Suggestion.none();

        FactionVehicleRouteControlAuthority.Mission mission = mission(plan);
        Faction target = targetFaction(plan);
        String posture = "local route presence";
        if (mission == FactionVehicleRouteControlAuthority.Mission.ROUTE_CONTEST
                && target != Faction.NONE
                && !FactionIdentityAuthority.sameFamily(
                target, site.faction)) {
            FactionVehicleBalanceAuthority.Contest contest =
                    FactionVehicleBalanceAuthority.compare(
                    game, site.faction, target, site);
            if (!contest.canEscalate(plan.aggression, plan.ambition)) {
                return Suggestion.none();
            }
            posture = contest.posture().label
                    + " fleet posture with commitment "
                    + contest.commitment(plan.aggression, plan.ambition)
                    + "/" + contest.escalationThreshold();
        }

        return new Suggestion(VEHICLE_ROUTE_CONTROL_GOAL, destination,
                "the active scheme calls for " + mission.label + " at "
                        + destination + "; " + fleet.readyVehicles()
                        + " ready fleet asset(s), route-control power "
                        + fleet.power(
                        FactionVehicleDoctrineAuthority.Dimension.ROUTE_CONTROL)
                        + ", " + posture);
    }

    /**
     * Compatibility surface for direct callers. Pending staging is deliberately
     * represented as an unsuccessful nonterminal outcome; the strategic tick uses
     * {@link #advance(GamePanel, FactionStrategicPlan, NpcFactionSite)} so pending
     * work is not counted as failure.
     */
    static FactionStrategicAssetAuthority.Outcome attempt(
            GamePanel game, FactionStrategicPlan plan, NpcFactionSite site) {
        return advance(game, plan, site).outcome();
    }

    static Progress advance(GamePanel game, FactionStrategicPlan plan,
                            NpcFactionSite site) {
        if (!handles(plan)) {
            return Progress.notHandled(
                    "Faction route-control strategy does not handle this goal.");
        }
        String id = planId(plan);
        MapObjectState bound = boundVehicle(game, id);
        if (bound != null) {
            return advanceBound(game, plan, site, bound);
        }

        FactionStrategicAssetAuthority.Outcome preflight =
                preflight(game, plan, site);
        if (preflight != null) return fromOutcome(preflight, null);

        FactionVehicleRouteControlAuthority.Request request = request(plan);
        MapObjectState conflict = objectiveVehicle(game, site, request);
        if (conflict != null) {
            String existingPlan = value(conflict,
                    "routeControlStrategicPlanId");
            FactionVehicleRouteControlAuthority.Snapshot order =
                    FactionVehicleRouteControlAuthority.inspect(conflict);
            if (existingPlan.isBlank()
                    && clean(order.reason(), "").equalsIgnoreCase(
                    clean(plan.scheme, ""))) {
                bind(conflict, plan, 0, "pending", game);
                return advanceBound(game, plan, site, conflict);
            }
            return Progress.terminal(false, false,
                    "route-control-objective-already-staged",
                    order.mission().label + " at "
                            + order.destinationKey()
                            + " is already assigned to another strategic operation.",
                    site.stock, site.stock, conflict);
        }

        int cost = deploymentCost(request);
        if (site.stock < cost) {
            return Progress.terminal(false, false,
                    "insufficient-route-control-stock",
                    site.name + " needs " + cost
                            + " stock to stage " + request.mission().label
                            + " at " + request.destinationKey()
                            + ", but has " + site.stock + ".",
                    site.stock, site.stock, null);
        }

        ArrayList<VehicleLedger> beforeVehicles = snapshotVehicles(game);
        int stockBefore = site.stock;
        FactionVehicleRouteControlAuthority.Result assignment =
                FactionVehicleRouteControlAuthority.assign(
                        game, site, request);
        if (!assignment.success() || assignment.vehicle() == null) {
            restore(beforeVehicles);
            return Progress.terminal(false, false,
                    "route-control-assignment-blocked",
                    clean(assignment.message(),
                    "No faction vehicle could accept the route-control order."),
                    stockBefore, site.stock, null);
        }

        MapObjectState vehicle = assignment.vehicle();
        VehicleStrategicTransitAuthority.Reservation reservation =
                FactionVehicleRouteControlAuthority.reserveTransit(
                        game, site, vehicle, request);
        if (!reservation.success()) {
            restore(beforeVehicles);
            return Progress.terminal(false, false,
                    "route-control-transit-blocked",
                    clean(reservation.message(),
                    "The selected vehicle could not reserve strategic transit."),
                    stockBefore, site.stock, vehicle);
        }

        FactionVehicleRouteControlAuthority.Result activation =
                FactionVehicleRouteControlAuthority.activate(
                        game, site, vehicle);
        if (!activation.success()) {
            restore(beforeVehicles);
            return Progress.terminal(false, false,
                    "route-control-activation-blocked",
                    clean(activation.message(),
                    "The selected route-control order could not activate."),
                    stockBefore, site.stock, vehicle);
        }

        boolean operationalChange = assignment.changed()
                || reservation.changed() || activation.changed();
        if (operationalChange) site.stock -= cost;
        bind(vehicle, plan, operationalChange ? cost : 0,
                "pending", game);
        FactionVehicleRouteControlAuthority.Snapshot order =
                FactionVehicleRouteControlAuthority.inspect(vehicle);
        return Progress.pending(true,
                site.faction.label + " staged " + request.mission().label
                        + " at " + request.destinationKey() + " using "
                        + displayName(game.world, vehicle)
                        + "; order state " + order.state()
                        + ", deployment strength " + order.strength()
                        + ", awaiting authoritative strategic transfer; site stock "
                        + stockBefore + " -> " + site.stock + ".",
                stockBefore, site.stock, vehicle);
    }

    static FactionVehicleRouteControlAuthority.Request request(
            FactionStrategicPlan plan) {
        FactionVehicleRouteControlAuthority.Mission mission = mission(plan);
        int aggression = plan == null ? 0 : Math.max(0, plan.aggression);
        int ambition = plan == null ? 0 : Math.max(0, plan.ambition);
        int distance = Math.max(6, Math.min(40,
                8 + ambition / 8 + aggression / 16));
        return new FactionVehicleRouteControlAuthority.Request(
                mission, destination(plan), targetFaction(plan),
                Set.of(VehicleStrategicTransitAuthority.Infrastructure.ROAD,
                        VehicleStrategicTransitAuthority.Infrastructure.VEHICLE_LANE,
                        VehicleStrategicTransitAuthority.Infrastructure.GATE,
                        VehicleStrategicTransitAuthority.Infrastructure.CHECKPOINT,
                        VehicleStrategicTransitAuthority.Infrastructure.PARKING_LOT,
                        VehicleStrategicTransitAuthority.Infrastructure.VEHICLE_YARD),
                distance, 4, true, true, false,
                aggression, ambition,
                clean(plan == null ? "" : plan.scheme,
                        mission.label));
    }

    private static Progress advanceBound(GamePanel game,
                                         FactionStrategicPlan plan,
                                         NpcFactionSite site,
                                         MapObjectState vehicle) {
        int stock = site == null ? 0 : Math.max(0, site.stock);
        String strategicState = value(vehicle,
                "routeControlStrategicState").toLowerCase(Locale.ROOT);
        FactionVehicleRouteControlAuthority.Snapshot order =
                FactionVehicleRouteControlAuthority.inspect(vehicle);
        if (strategicState.equals("completed")
                || order.state().equalsIgnoreCase("completed")) {
            if (!strategicState.equals("completed")) {
                set(vehicle, "routeControlStrategicState", "completed");
                set(vehicle, "routeControlStrategicCompletedTurn",
                        Long.toString(game == null ? 0L
                                : Math.max(0L, game.worldTurn)));
            }
            return Progress.terminal(true,
                    !strategicState.equals("completed"), "",
                    completionMessage(game, plan, vehicle, order),
                    stock, stock, vehicle);
        }
        if (strategicState.equals("failed")
                || order.state().equalsIgnoreCase("cancelled")) {
            return Progress.terminal(false, false,
                    "route-control-transit-failed",
                    "The route-control deployment to "
                            + clean(order.destinationKey(), destination(plan))
                            + " ended before the objective was secured.",
                    stock, stock, vehicle);
        }

        String transit = value(vehicle, "strategicTransitState")
                .toLowerCase(Locale.ROOT);
        if (transit.equals("completed")) {
            FactionVehicleRouteControlAuthority.Result completed =
                    FactionVehicleRouteControlAuthority.complete(
                            game, site, vehicle,
                            "the assigned vehicle reached "
                                    + clean(order.destinationKey(),
                                    destination(plan))
                                    + " and secured the route objective");
            if (!completed.success()) {
                return Progress.pending(false,
                        "The strategic transfer completed, but route-control closure is waiting: "
                                + completed.message(), stock, stock, vehicle);
            }
            set(vehicle, "routeControlStrategicState", "completed");
            set(vehicle, "routeControlStrategicCompletedTurn",
                    Long.toString(game == null ? 0L
                            : Math.max(0L, game.worldTurn)));
            boolean pressure = applyCompletionPressure(
                    game, plan, vehicle, order);
            return Progress.terminal(true,
                    completed.changed() || pressure, "",
                    completionMessage(game, plan, vehicle,
                            completed.snapshot()), stock, stock, vehicle);
        }

        if (transit.equals("cancelled")
                || transit.equals("failed")
                || transit.equals("blocked")) {
            FactionVehicleRouteControlAuthority.Result cancelled =
                    FactionVehicleRouteControlAuthority.cancel(
                            game, site, vehicle,
                            "the authoritative strategic transfer ended with state "
                                    + transit);
            if (!cancelled.success()) {
                return Progress.pending(false,
                        "The strategic transfer ended, but the route order cannot close yet: "
                                + cancelled.message(), stock, stock, vehicle);
            }
            set(vehicle, "routeControlStrategicState", "failed");
            set(vehicle, "routeControlStrategicFailedTurn",
                    Long.toString(game == null ? 0L
                            : Math.max(0L, game.worldTurn)));
            return Progress.terminal(false, true,
                    "route-control-transit-" + transit,
                    "The " + order.mission().label + " to "
                            + order.destinationKey()
                            + " failed because the strategic transfer was "
                            + transit + ".",
                    stock, stock, vehicle);
        }

        if (order.state().equalsIgnoreCase("assigned")) {
            FactionVehicleRouteControlAuthority.Request request = request(plan);
            VehicleStrategicTransitAuthority.Reservation reservation =
                    FactionVehicleRouteControlAuthority.reserveTransit(
                            game, site, vehicle, request);
            if (!reservation.success()) {
                return Progress.pending(false,
                        "The assigned route order is waiting for a valid strategic reservation: "
                                + reservation.message(), stock, stock, vehicle);
            }
            FactionVehicleRouteControlAuthority.Result activation =
                    FactionVehicleRouteControlAuthority.activate(
                            game, site, vehicle);
            if (!activation.success()) {
                return Progress.pending(reservation.changed(),
                        "The route order is reserved but cannot activate yet: "
                                + activation.message(), stock, stock, vehicle);
            }
            set(vehicle, "routeControlStrategicState", "pending");
            return Progress.pending(reservation.changed()
                            || activation.changed(),
                    "The route-control order is active and awaiting authoritative strategic transfer.",
                    stock, stock, vehicle);
        }

        if (order.state().equalsIgnoreCase("active")
                && (transit.equals("reserved")
                || transit.equals("committing"))) {
            return Progress.pending(false,
                    order.mission().label + " to "
                            + order.destinationKey() + " is "
                            + (transit.equals("committing")
                            ? "crossing between zones"
                            : "waiting on its reserved strategic transfer")
                            + ".",
                    stock, stock, vehicle);
        }

        return Progress.pending(false,
                "The bound route-control deployment is waiting for its authoritative transit ledger to become reserved, committing, completed, or cancelled.",
                stock, stock, vehicle);
    }

    private static FactionStrategicAssetAuthority.Outcome preflight(
            GamePanel game, FactionStrategicPlan plan, NpcFactionSite site) {
        if (game == null || game.world == null || plan == null) {
            return blocked("no-loaded-route-control-world",
                    "Faction route-control strategy requires a loaded world and plan.",
                    site);
        }
        if (site == null || !localSite(site, game.world)) {
            return blocked("route-control-site-not-local",
                    faction(plan.faction)
                            + " has no linked local motor pool for route control.",
                    site);
        }
        if (!FactionIdentityAuthority.sameFamily(
                site.faction, plan.faction)) {
            return blocked("route-control-site-faction-mismatch",
                    site.name + " does not belong to the plan's faction family.",
                    site);
        }
        int workers = Math.max(0,
                FactionSiteWorkforceAuthority.evaluate(
                        site, game.world).effectiveWorkers());
        if (workers <= 0) {
            return blocked("route-control-site-unstaffed",
                    site.name
                            + " has no effective workers for vehicle deployment.",
                    site);
        }
        if (destination(plan).isBlank()) {
            return blocked("route-control-destination-missing",
                    "The route-control plan has no concrete destination.", site);
        }
        return null;
    }

    private static boolean routeIntent(FactionStrategicPlan plan) {
        String text = clean(plan == null ? "" : plan.scheme, "")
                .toLowerCase(Locale.ROOT);
        return contains(text, "route", "road", "checkpoint", "gate",
                "convoy", "patrol", "corridor", "transit", "freight",
                "interdict", "escort");
    }

    private static FactionVehicleRouteControlAuthority.Mission mission(
            FactionStrategicPlan plan) {
        String text = clean(plan == null ? "" : plan.scheme, "")
                .toLowerCase(Locale.ROOT);
        Faction target = targetFaction(plan);
        if (target != Faction.NONE
                || contains(text, "contest", "interdict", "deny",
                "pressure", "blockade")) {
            return FactionVehicleRouteControlAuthority.Mission.ROUTE_CONTEST;
        }
        if (contains(text, "checkpoint", "gate", "reinforce")) {
            return FactionVehicleRouteControlAuthority.Mission
                    .CHECKPOINT_REINFORCEMENT;
        }
        if (contains(text, "convoy", "escort", "freight")) {
            return FactionVehicleRouteControlAuthority.Mission.CONVOY_ESCORT;
        }
        return FactionVehicleRouteControlAuthority.Mission.PATROL;
    }

    private static String destination(FactionStrategicPlan plan) {
        String destination = clean(plan == null ? "" : plan.targetRoom, "");
        if (destination.equalsIgnoreCase("none")
                || destination.equalsIgnoreCase("unassigned")) return "";
        return destination;
    }

    private static Faction targetFaction(FactionStrategicPlan plan) {
        return plan == null || plan.schemeTargetFaction == null
                ? Faction.NONE : plan.schemeTargetFaction;
    }

    private static int deploymentCost(
            FactionVehicleRouteControlAuthority.Request request) {
        int cost = 1 + Math.max(0, request.routeDistance()) / 12;
        if (request.mission()
                == FactionVehicleRouteControlAuthority.Mission
                .CHECKPOINT_REINFORCEMENT
                || request.mission()
                == FactionVehicleRouteControlAuthority.Mission.CONVOY_ESCORT) {
            cost++;
        }
        if (request.mission()
                == FactionVehicleRouteControlAuthority.Mission.ROUTE_CONTEST) {
            cost += 2;
        }
        return Math.max(1, Math.min(8, cost));
    }

    private static MapObjectState boundVehicle(GamePanel game, String planId) {
        if (game == null || game.world == null
                || game.world.mapObjects == null || planId.isBlank()) return null;
        for (MapObjectState object : game.world.mapObjects) {
            if (VehicleRuntimeAuthority.isVehicle(object)
                    && planId.equals(value(object,
                    "routeControlStrategicPlanId"))) return object;
        }
        return null;
    }

    private static MapObjectState objectiveVehicle(
            GamePanel game, NpcFactionSite site,
            FactionVehicleRouteControlAuthority.Request request) {
        if (game == null || game.world == null
                || game.world.mapObjects == null || site == null) return null;
        for (MapObjectState object : game.world.mapObjects) {
            if (!VehicleRuntimeAuthority.isVehicle(object)
                    || !VehicleRuntimeAuthority.factionOwns(
                    object, site.faction)) continue;
            FactionVehicleRouteControlAuthority.Snapshot snapshot =
                    FactionVehicleRouteControlAuthority.inspect(object);
            if (!snapshot.assigned()) continue;
            if (snapshot.mission() == request.mission()
                    && snapshot.destinationKey().equals(
                    request.destinationKey())
                    && sameTarget(snapshot.targetFaction(),
                    request.targetFaction())) return object;
        }
        return null;
    }

    private static boolean sameTarget(Faction left, Faction right) {
        Faction safeLeft = left == null ? Faction.NONE : left;
        Faction safeRight = right == null ? Faction.NONE : right;
        if (safeLeft == Faction.NONE || safeRight == Faction.NONE) {
            return safeLeft == safeRight;
        }
        return FactionIdentityAuthority.sameFamily(safeLeft, safeRight);
    }

    private static void bind(MapObjectState vehicle,
                             FactionStrategicPlan plan,
                             int stockCost, String state,
                             GamePanel game) {
        set(vehicle, "routeControlStrategicPlanId", planId(plan));
        set(vehicle, "routeControlStrategicState",
                clean(state, "pending"));
        set(vehicle, "routeControlStrategicStockCost",
                Integer.toString(Math.max(0, stockCost)));
        set(vehicle, "routeControlStrategicBoundTurn",
                Long.toString(game == null ? 0L
                        : Math.max(0L, game.worldTurn)));
        append(vehicle, "deploymentHistory",
                "Strategic route plan " + planId(plan)
                        + " bound to " + destination(plan));
    }

    private static boolean applyCompletionPressure(
            GamePanel game, FactionStrategicPlan plan,
            MapObjectState vehicle,
            FactionVehicleRouteControlAuthority.Snapshot order) {
        String id = planId(plan);
        if (game == null || plan == null || vehicle == null
                || id.equals(value(vehicle,
                "routeControlStrategicPressurePlanId"))) return false;
        Faction target = targetFaction(plan);
        if (order.mission()
                != FactionVehicleRouteControlAuthority.Mission.ROUTE_CONTEST
                || target == Faction.NONE
                || FactionIdentityAuthority.sameFamily(
                target, plan.faction)) return false;
        game.addFactionMarketPressure(target,
                1 + Math.max(0, plan.aggression) / 50,
                faction(plan.faction) + " completed "
                        + order.mission().label + " at "
                        + order.destinationKey());
        set(vehicle, "routeControlStrategicPressurePlanId", id);
        return true;
    }

    private static String completionMessage(
            GamePanel game, FactionStrategicPlan plan,
            MapObjectState vehicle,
            FactionVehicleRouteControlAuthority.Snapshot order) {
        Faction target = targetFaction(plan);
        FactionVehicleBalanceAuthority.Contest balance =
                FactionVehicleBalanceAuthority.compare(
                        game, plan == null ? Faction.NONE : plan.faction,
                        target, null);
        return faction(plan == null ? Faction.NONE : plan.faction)
                + " completed " + order.mission().label + " at "
                + clean(order.destinationKey(), destination(plan))
                + " using " + displayName(
                game == null ? null : game.world, vehicle)
                + "; deployment strength " + order.strength()
                + ", route-control balance "
                + signed(balance.routeControlDelta()) + ".";
    }

    private static String planId(FactionStrategicPlan plan) {
        String explicit = clean(plan == null ? "" : plan.id, "");
        if (!explicit.isBlank()) return explicit;
        String seed = faction(plan == null ? Faction.NONE : plan.faction)
                + "|" + destination(plan) + "|"
                + clean(plan == null ? "" : plan.scheme, "");
        return "ROUTE-PLAN-" + Math.abs(seed.hashCode());
    }

    private static ArrayList<VehicleLedger> snapshotVehicles(GamePanel game) {
        ArrayList<VehicleLedger> snapshots = new ArrayList<>();
        if (game == null || game.world == null
                || game.world.mapObjects == null) return snapshots;
        for (MapObjectState object : game.world.mapObjects) {
            if (VehicleRuntimeAuthority.isVehicle(object)) {
                snapshots.add(new VehicleLedger(object, object.stockState));
            }
        }
        return snapshots;
    }

    private static void restore(List<VehicleLedger> snapshots) {
        if (snapshots == null) return;
        for (VehicleLedger snapshot : snapshots) {
            if (snapshot != null && snapshot.vehicle() != null) {
                snapshot.vehicle().stockState = snapshot.stockState();
            }
        }
    }

    private static Progress fromOutcome(
            FactionStrategicAssetAuthority.Outcome outcome,
            MapObjectState vehicle) {
        if (outcome == null) {
            return Progress.notHandled(
                    "Faction route-control preflight returned no result.");
        }
        return Progress.terminal(outcome.success(), false,
                outcome.blocker(), outcome.message(),
                outcome.stockBefore(), outcome.stockAfter(), vehicle);
    }

    private static FactionStrategicAssetAuthority.Outcome blocked(
            String blocker, String message, NpcFactionSite site) {
        int stock = site == null ? 0 : Math.max(0, site.stock);
        return new FactionStrategicAssetAuthority.Outcome(
                true, false, clean(blocker,
                "route-control-operation-blocked"),
                clean(message,
                "Faction route-control operation was blocked."),
                -1, stock, stock, null, null);
    }

    private static boolean localSite(NpcFactionSite site, World world) {
        return site != null && world != null
                && site.sectorX == world.sectorX
                && site.sectorY == world.sectorY
                && site.zoneX == world.zoneX
                && site.zoneY == world.zoneY
                && site.floor == world.floor;
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

    private static String faction(Faction faction) {
        return faction == null || faction == Faction.NONE
                ? "Unaligned" : faction.label;
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    private static boolean contains(String text, String... needles) {
        if (text == null || needles == null) return false;
        for (String needle : needles) {
            if (needle != null && !needle.isBlank()
                    && text.contains(needle)) return true;
        }
        return false;
    }

    private static String value(MapObjectState vehicle, String key) {
        return vehicle == null ? ""
                : MapObjectState.stockValue(vehicle.stockState, key);
    }

    private static void set(MapObjectState vehicle, String key,
                            String value) {
        if (vehicle == null) return;
        vehicle.stockState = MapObjectState.setStockFlag(
                vehicle.stockState, key,
                clean(value, "").replace(';', ',').replace('|', '/'));
    }

    private static void append(MapObjectState vehicle, String key,
                               String entry) {
        String existing = value(vehicle, key);
        set(vehicle, key, existing.isBlank() ? clean(entry, "")
                : existing + "~" + clean(entry, ""));
    }

    private static String clean(String value, String fallback) {
        String text = value == null ? ""
                : value.trim().replaceAll("\\s+", " ");
        return text.isBlank() ? (fallback == null ? "" : fallback)
                : text;
    }
}
