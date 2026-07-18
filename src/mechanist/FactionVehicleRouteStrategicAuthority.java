package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Adapts ordinary faction strategy plans into authoritative vehicle route-control
 * deployments. The strategic plan remains the scheduler, the route-control
 * authority remains the order owner, and the strategic-transit authority remains
 * the reservation owner. Failed staging restores every touched vehicle ledger.
 */
final class FactionVehicleRouteStrategicAuthority {
    static final String VEHICLE_ROUTE_CONTROL_GOAL =
            "deploy vehicle route control";

    record Suggestion(String goal, String target, String reason) {
        static Suggestion none() { return new Suggestion("", "", ""); }
        boolean available() { return goal != null && !goal.isBlank(); }
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
            posture = contest.posture().label + " fleet posture with commitment "
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

    static FactionStrategicAssetAuthority.Outcome attempt(
            GamePanel game, FactionStrategicPlan plan, NpcFactionSite site) {
        if (!handles(plan)) {
            return FactionStrategicAssetAuthority.Outcome.notHandled(
                    "Faction route-control strategy does not handle this goal.");
        }
        FactionStrategicAssetAuthority.Outcome preflight =
                preflight(game, plan, site);
        if (preflight != null) return preflight;

        FactionVehicleRouteControlAuthority.Request request =
                request(plan);
        int cost = deploymentCost(request);
        if (site.stock < cost) {
            return blocked("insufficient-route-control-stock",
                    site.name + " needs " + cost
                            + " stock to stage " + request.mission().label
                            + " at " + request.destinationKey()
                            + ", but has " + site.stock + ".", site);
        }

        ArrayList<VehicleLedger> beforeVehicles = snapshotVehicles(game);
        int stockBefore = site.stock;
        FactionVehicleRouteControlAuthority.Result assignment =
                FactionVehicleRouteControlAuthority.assign(
                        game, site, request);
        if (!assignment.success() || assignment.vehicle() == null) {
            restore(beforeVehicles);
            return blocked("route-control-assignment-blocked",
                    clean(assignment.message(),
                    "No faction vehicle could accept the route-control order."),
                    site);
        }

        MapObjectState vehicle = assignment.vehicle();
        VehicleStrategicTransitAuthority.Reservation reservation =
                FactionVehicleRouteControlAuthority.reserveTransit(
                        game, site, vehicle, request);
        if (!reservation.success()) {
            restore(beforeVehicles);
            return blocked("route-control-transit-blocked",
                    clean(reservation.message(),
                    "The selected vehicle could not reserve strategic transit."),
                    site);
        }

        FactionVehicleRouteControlAuthority.Result activation =
                FactionVehicleRouteControlAuthority.activate(
                        game, site, vehicle);
        if (!activation.success()) {
            restore(beforeVehicles);
            return blocked("route-control-activation-blocked",
                    clean(activation.message(),
                    "The selected route-control order could not activate."),
                    site);
        }

        boolean changed = assignment.changed()
                || reservation.changed() || activation.changed();
        if (changed) site.stock -= cost;
        Faction target = targetFaction(plan);
        if (changed
                && request.mission()
                == FactionVehicleRouteControlAuthority.Mission.ROUTE_CONTEST
                && target != Faction.NONE
                && !FactionIdentityAuthority.sameFamily(
                target, site.faction)) {
            game.addFactionMarketPressure(target,
                    1 + Math.max(0, plan.aggression) / 50,
                    site.faction.label + " staged "
                            + request.mission().label + " at "
                            + request.destinationKey());
        }

        FactionVehicleRouteControlAuthority.Snapshot order =
                FactionVehicleRouteControlAuthority.inspect(vehicle);
        FactionVehicleBalanceAuthority.Contest balance =
                FactionVehicleBalanceAuthority.compare(game,
                        site.faction, target, site);
        return new FactionStrategicAssetAuthority.Outcome(
                true, true, "",
                site.faction.label + " staged " + request.mission().label
                        + " at " + request.destinationKey() + " using "
                        + displayName(game.world, vehicle)
                        + "; order state " + order.state()
                        + ", deployment strength " + order.strength()
                        + ", route-control balance "
                        + signed(balance.routeControlDelta())
                        + "; site stock " + stockBefore + " -> "
                        + site.stock + ".",
                -1, stockBefore, site.stock, null, null);
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

    private static String clean(String value, String fallback) {
        String text = value == null ? ""
                : value.trim().replaceAll("\\s+", " ");
        return text.isBlank() ? (fallback == null ? "" : fallback)
                : text;
    }
}
