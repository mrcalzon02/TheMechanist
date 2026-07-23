package mechanist;

import java.util.Locale;

/**
 * Resolved-world bridge between a pending faction route plan and the existing
 * atomic vehicle transfer owner. This authority does not load or cache worlds.
 * The caller supplies both resolved worlds and legal destination coordinates.
 */
final class FactionVehicleRouteTransitCommitAuthority {
    enum Status {
        COMMITTED,
        BLOCKED,
        ROLLED_BACK,
        PENDING_FINALIZATION
    }

    record Result(Status status, boolean success, boolean changed,
                  boolean finalized, int resolvedPlans,
                  String message, MapObjectState vehicle,
                  VehicleStrategicTransitCommitAuthority.Result transfer,
                  FactionVehicleRouteStrategicAuthority.Progress progress) {
        static Result blocked(MapObjectState vehicle, String message) {
            return new Result(Status.BLOCKED, false, false,
                    false, 0, clean(message,
                    "The strategic route transfer was blocked."),
                    vehicle, null, null);
        }
    }

    private FactionVehicleRouteTransitCommitAuthority() { }

    static Result commit(GamePanel game, FactionStrategicPlan plan,
                         NpcFactionSite site, MapObjectState vehicle,
                         VehicleStrategicTransitCommitAuthority.TransferRequest request) {
        Result preflight = preflight(game, plan, site, vehicle, request);
        if (preflight != null) return preflight;

        World loadedWorld = game.world;
        World source = request.sourceWorld();
        World destination = request.destinationWorld();
        String transitState = value(vehicle, "strategicTransitState")
                .toLowerCase(Locale.ROOT);
        VehicleStrategicTransitCommitAuthority.Result transfer = null;
        boolean transferChanged = false;

        if (transitState.equals("completed")) {
            if (!destination.mapObjects.contains(vehicle)
                    || source.mapObjects.contains(vehicle)) {
                return Result.blocked(vehicle,
                        "The completed strategic transfer ledger does not match physical world placement.");
            }
        } else {
            transfer = VehicleStrategicTransitCommitAuthority.commit(
                    game, vehicle, request);
            if (!transfer.success()) {
                Status status = transfer.status()
                        == VehicleStrategicTransitCommitAuthority.Status.ROLLED_BACK
                        ? Status.ROLLED_BACK : Status.BLOCKED;
                return new Result(status, false, false,
                        false, 0, transfer.message(), vehicle,
                        transfer, null);
            }
            transferChanged = transfer.changed();
        }

        FactionVehicleRouteStrategicAuthority.Progress progress;
        try {
            // Completion must be evaluated where the exact vehicle now exists.
            game.world = destination;
            progress = FactionVehicleRouteStrategicAuthority.advance(
                    game, plan, site);
        } finally {
            // Background faction transit never replaces the player's loaded world.
            game.world = loadedWorld;
        }

        if (progress == null || !progress.handled()) {
            return new Result(Status.PENDING_FINALIZATION, true,
                    transferChanged, false, 0,
                    "The physical transfer completed, but its strategic plan owner did not accept the completion result.",
                    vehicle, transfer, progress);
        }
        if (progress.pending()) {
            return new Result(Status.PENDING_FINALIZATION, true,
                    transferChanged || progress.changed(), false, 0,
                    progress.message(), vehicle, transfer, progress);
        }

        int resolved = FactionStrategicAssetTickAuthority
                .finalizeRouteProgress(game, plan, site, progress);
        boolean finalized = resolved == 1;
        return new Result(Status.COMMITTED,
                finalized && progress.success(),
                transferChanged || progress.changed() || finalized,
                finalized, resolved,
                finalized ? progress.message()
                        : "The physical transfer completed, but the strategic plan did not enter terminal accounting.",
                vehicle, transfer, progress);
    }

    private static Result preflight(
            GamePanel game, FactionStrategicPlan plan,
            NpcFactionSite site, MapObjectState vehicle,
            VehicleStrategicTransitCommitAuthority.TransferRequest request) {
        if (game == null || game.world == null) {
            return Result.blocked(vehicle,
                    "A loaded source world is required for faction route transfer.");
        }
        if (plan == null
                || !FactionVehicleRouteStrategicAuthority.handles(plan)
                || !"EXECUTION".equals(plan.phase)) {
            return Result.blocked(vehicle,
                    "Only an executing faction route-control plan may commit strategic transit.");
        }
        if (site == null || request == null
                || request.sourceWorld() == null
                || request.destinationWorld() == null) {
            return Result.blocked(vehicle,
                    "The route transfer requires its motor pool and both resolved worlds.");
        }
        if (request.sourceWorld() != game.world) {
            return Result.blocked(vehicle,
                    "The resolved source world must be the currently loaded route origin.");
        }
        if (request.sourceWorld() == request.destinationWorld()) {
            return Result.blocked(vehicle,
                    "The route transfer requires distinct source and destination worlds.");
        }
        if (!localSite(site, request.sourceWorld())
                || !FactionIdentityAuthority.sameFamily(
                site.faction, plan.faction)) {
            return Result.blocked(vehicle,
                    "The executing plan requires its local same-family motor pool at the source.");
        }
        if (vehicle == null || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            return Result.blocked(vehicle,
                    "The route transfer requires the physical vehicle bound to the plan.");
        }
        String expectedPlan = planId(plan);
        if (!expectedPlan.equals(value(vehicle,
                "routeControlStrategicPlanId"))) {
            return Result.blocked(vehicle,
                    "The selected vehicle is not bound to this strategic route plan.");
        }
        FactionVehicleRouteControlAuthority.Snapshot order =
                FactionVehicleRouteControlAuthority.inspect(vehicle);
        if (!order.assigned()
                || !"active".equalsIgnoreCase(order.state())) {
            return Result.blocked(vehicle,
                    "The bound route-control order must be active before transfer.");
        }
        if (!order.siteName().equals(site.name)) {
            return Result.blocked(vehicle,
                    "The active route order belongs to a different motor pool.");
        }
        String destinationKey = Integer.toString(
                request.destinationWorld().locationKey());
        if (!destinationKey.equals(order.destinationKey())
                || !destinationKey.equals(clean(plan.targetRoom, ""))
                || !destinationKey.equals(value(vehicle,
                "strategicTransitDestination"))) {
            return Result.blocked(vehicle,
                    "The resolved destination does not match the plan, route order, and transit reservation.");
        }
        VehicleMotorPoolAuthority.Snapshot pool =
                VehicleMotorPoolAuthority.inspect(game, vehicle, site);
        if (!pool.assigned() || !pool.siteLocal()
                || !pool.ownerAligned()) {
            return Result.blocked(vehicle,
                    "The vehicle must retain aligned local motor-pool custody before departure.");
        }
        String transit = value(vehicle, "strategicTransitState")
                .toLowerCase(Locale.ROOT);
        if (!transit.equals("reserved")
                && !transit.equals("completed")) {
            return Result.blocked(vehicle,
                    "The strategic transit ledger must be reserved or already completed for finalization.");
        }
        if (transit.equals("reserved")
                && !request.sourceWorld().mapObjects.contains(vehicle)) {
            return Result.blocked(vehicle,
                    "The reserved physical vehicle is no longer present in the source world.");
        }
        return null;
    }

    private static String planId(FactionStrategicPlan plan) {
        String explicit = clean(plan == null ? "" : plan.id, "");
        if (!explicit.isBlank()) return explicit;
        String faction = plan == null || plan.faction == null
                || plan.faction == Faction.NONE
                ? "Unaligned" : plan.faction.label;
        String destination = clean(plan == null ? "" : plan.targetRoom, "");
        String scheme = clean(plan == null ? "" : plan.scheme, "");
        return "ROUTE-PLAN-" + Math.abs((faction + "|"
                + destination + "|" + scheme).hashCode());
    }

    private static boolean localSite(NpcFactionSite site, World world) {
        return site != null && world != null
                && site.sectorX == world.sectorX
                && site.sectorY == world.sectorY
                && site.zoneX == world.zoneX
                && site.zoneY == world.zoneY
                && site.floor == world.floor;
    }

    private static String value(MapObjectState vehicle, String key) {
        return vehicle == null ? ""
                : MapObjectState.stockValue(vehicle.stockState, key);
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? ""
                : value.trim().replaceAll("\\s+", " ");
        return cleaned.isBlank() ? (fallback == null ? "" : fallback)
                : cleaned;
    }
}