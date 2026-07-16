package mechanist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

/**
 * Symmetric faction vehicle operations over the same persistent vehicle records
 * used by player purchase, ownership, repair, and salvage.
 */
final class FactionVehicleStrategicAuthority {
    static final String VEHICLE_SEIZURE_GOAL = "seize a vehicle";
    static final String VEHICLE_REPAIR_GOAL = "repair damaged vehicles";
    static final String VEHICLE_SALVAGE_GOAL = "salvage captured vehicles";

    record Suggestion(String goal, String target, String reason) {
        static Suggestion none() { return new Suggestion("", "", ""); }
        boolean available() { return goal != null && !goal.isBlank(); }
    }

    private FactionVehicleStrategicAuthority() { }

    static boolean handles(FactionStrategicPlan plan) {
        if (plan == null || plan.immediateGoal == null) return false;
        String goal = plan.immediateGoal.trim();
        return VEHICLE_SEIZURE_GOAL.equalsIgnoreCase(goal)
                || VEHICLE_REPAIR_GOAL.equalsIgnoreCase(goal)
                || VEHICLE_SALVAGE_GOAL.equalsIgnoreCase(goal);
    }

    static FactionStrategicAssetAuthority.Outcome attempt(
            GamePanel game, FactionStrategicPlan plan, NpcFactionSite site) {
        if (!handles(plan)) {
            return FactionStrategicAssetAuthority.Outcome.notHandled(
                    "Faction vehicle strategy does not handle this goal.");
        }
        FactionStrategicAssetAuthority.Outcome preflight = preflight(game, plan, site);
        if (preflight != null) return preflight;
        if (VEHICLE_SEIZURE_GOAL.equalsIgnoreCase(plan.immediateGoal.trim())) {
            return seize(game, plan, site);
        }
        if (VEHICLE_REPAIR_GOAL.equalsIgnoreCase(plan.immediateGoal.trim())) {
            return repair(game, plan, site);
        }
        return salvage(game, plan, site);
    }

    static Suggestion nextSuggestion(GamePanel game, NpcFactionSite site,
                                     FactionStrategicPlan plan) {
        if (!localSite(site, game == null ? null : game.world)) return Suggestion.none();
        MapObjectState captured = capturedVehicle(game, site);
        if (captured != null) {
            return new Suggestion(VEHICLE_SALVAGE_GOAL,
                    VehicleRuntimeAuthority.inspectionLine(game.world, captured),
                    "a seized same-family vehicle is ready for accountable salvage");
        }
        MapObjectState damaged = damagedVehicle(game, site);
        if (damaged != null) {
            return new Suggestion(VEHICLE_REPAIR_GOAL,
                    displayName(damaged),
                    "a same-family vehicle has damaged components");
        }
        if (plan != null && plan.scheme != null && !plan.scheme.isBlank()) {
            MapObjectState target = seizableVehicle(game, plan, site);
            if (target != null) {
                return new Suggestion(VEHICLE_SEIZURE_GOAL,
                        displayName(target),
                        "the active scheme has a physical non-player vehicle target");
            }
        }
        return Suggestion.none();
    }

    private static FactionStrategicAssetAuthority.Outcome seize(
            GamePanel game, FactionStrategicPlan plan, NpcFactionSite site) {
        if (plan.scheme == null || plan.scheme.isBlank()) {
            return blocked("no-vehicle-seizure-scheme",
                    faction(plan.faction) + " cannot seize a vehicle without a recorded scheme.",
                    site);
        }
        MapObjectState vehicle = seizableVehicle(game, plan, site);
        if (vehicle == null) {
            return blocked("no-seizable-vehicle",
                    faction(plan.faction)
                            + " found no non-player vehicle eligible for the current scheme.",
                    site);
        }
        VehicleRuntimeAuthority.Snapshot snapshot =
                VehicleRuntimeAuthority.inspect(game.world, vehicle);
        int cost = 2 + Math.max(0, snapshot.purchasePrice()) / 180;
        if (site.stock < cost) {
            return blocked("insufficient-vehicle-seizure-stock",
                    site.name + " needs " + cost
                            + " stock to execute the vehicle seizure, but has "
                            + site.stock + ".", site);
        }
        int before = site.stock;
        Faction formerFaction = snapshot.ownerFaction();
        String formerOwner = snapshot.ownerName();
        site.stock -= cost;
        VehicleRuntimeAuthority.Result result =
                VehicleRuntimeAuthority.transferToFaction(vehicle, site.faction,
                        game.turn, plan.scheme);
        if (!result.success()) {
            site.stock = before;
            return blocked("vehicle-seizure-transfer-failed", result.message(), site);
        }
        if (formerFaction != null && formerFaction != Faction.NONE
                && !FactionIdentityAuthority.sameFamily(formerFaction, site.faction)) {
            game.addFactionMarketPressure(formerFaction,
                    2 + Math.max(0, plan.aggression) / 35,
                    site.faction.label + " seized " + displayName(vehicle));
        }
        return new FactionStrategicAssetAuthority.Outcome(true, true, "",
                site.faction.label + " seized " + displayName(vehicle)
                        + " from " + clean(formerOwner, faction(formerFaction))
                        + " through " + plan.scheme + "; site stock "
                        + before + " -> " + site.stock + ".",
                -1, before, site.stock, null, null);
    }

    private static FactionStrategicAssetAuthority.Outcome repair(
            GamePanel game, FactionStrategicPlan plan, NpcFactionSite site) {
        MapObjectState vehicle = damagedVehicle(game, site);
        if (vehicle == null) {
            return blocked("no-damaged-faction-vehicle",
                    site.name + " has no damaged same-family vehicle to repair.", site);
        }
        VehicleRuntimeAuthority.Component worst = worstComponent(vehicle);
        int deficit = worst == null ? 0 : 100 - component(vehicle, worst);
        int cost = Math.max(1, Math.min(8, 1 + deficit / 20));
        if (site.stock < cost) {
            return blocked("insufficient-vehicle-repair-stock",
                    site.name + " needs " + cost
                            + " stock to repair " + displayName(vehicle)
                            + ", but has " + site.stock + ".", site);
        }
        int before = site.stock;
        site.stock -= cost;
        VehicleRuntimeAuthority.Result result =
                VehicleRuntimeAuthority.repairForFaction(vehicle, 35, game.turn,
                        site.faction, site.name + " motor-pool repair");
        if (!result.success()) {
            site.stock = before;
            return blocked("vehicle-repair-failed", result.message(), site);
        }
        return new FactionStrategicAssetAuthority.Outcome(true, true, "",
                result.message() + " Site stock " + before + " -> "
                        + site.stock + ".", -1, before, site.stock, null, null);
    }

    private static FactionStrategicAssetAuthority.Outcome salvage(
            GamePanel game, FactionStrategicPlan plan, NpcFactionSite site) {
        MapObjectState vehicle = capturedVehicle(game, site);
        if (vehicle == null) {
            return blocked("no-captured-faction-vehicle",
                    site.name + " has no seized vehicle available for salvage.", site);
        }
        int yield = VehicleRuntimeAuthority.salvageYield(vehicle);
        if (site.stock > 160 - yield) {
            return blocked("vehicle-salvage-stock-capacity",
                    site.name + " lacks capacity for " + yield
                            + " recovered vehicle stock units; the vehicle remains intact.",
                    site);
        }
        int before = site.stock;
        VehicleRuntimeAuthority.Result result =
                VehicleRuntimeAuthority.salvageForFaction(vehicle, site.faction,
                        game.turn, plan.immediateGoal);
        if (!result.success()) return blocked("vehicle-salvage-failed", result.message(), site);
        site.stock += yield;
        FactionStrategySimulationApi.materializeFactionStock(game, site,
                "Machine part", Math.max(1, yield / 3),
                "salvaged from seized " + displayName(vehicle));
        return new FactionStrategicAssetAuthority.Outcome(true, true, "",
                result.message() + " Site stock " + before + " -> "
                        + site.stock + " with provenance-aware Machine part recovery.",
                -1, before, site.stock, null, null);
    }

    private static FactionStrategicAssetAuthority.Outcome preflight(
            GamePanel game, FactionStrategicPlan plan, NpcFactionSite site) {
        if (game == null || game.world == null || plan == null) {
            return blocked("no-loaded-vehicle-world",
                    "Faction vehicle strategy requires a loaded world and plan.", site);
        }
        if (site == null || !localSite(site, game.world)) {
            return blocked("vehicle-site-not-local",
                    faction(plan.faction)
                            + " has no linked local site for vehicle operations.", site);
        }
        if (!FactionIdentityAuthority.sameFamily(site.faction, plan.faction)) {
            return blocked("vehicle-site-faction-mismatch",
                    site.name + " does not belong to the plan's faction family.", site);
        }
        int workers = Math.max(0,
                FactionSiteWorkforceAuthority.evaluate(site, game.world).effectiveWorkers());
        if (workers <= 0) {
            return blocked("vehicle-site-unstaffed",
                    site.name + " has no effective workers for vehicle operations.", site);
        }
        return null;
    }

    private static MapObjectState capturedVehicle(GamePanel game,
                                                   NpcFactionSite site) {
        return first(game, vehicle -> VehicleRuntimeAuthority.factionOwns(vehicle, site.faction)
                && VehicleRuntimeAuthority.seized(vehicle)
                && !"salvaged".equals(MapObjectState.stockValue(
                vehicle.stockState, "condition")));
    }

    private static MapObjectState damagedVehicle(GamePanel game,
                                                  NpcFactionSite site) {
        return first(game, vehicle -> VehicleRuntimeAuthority.factionOwns(vehicle, site.faction)
                && VehicleRuntimeAuthority.damaged(vehicle));
    }

    private static MapObjectState seizableVehicle(GamePanel game,
                                                  FactionStrategicPlan plan,
                                                  NpcFactionSite site) {
        return first(game, vehicle -> {
            VehicleRuntimeAuthority.Snapshot snapshot =
                    VehicleRuntimeAuthority.inspect(game.world, vehicle);
            if (snapshot == null
                    || snapshot.ownerType() == VehicleRuntimeAuthority.OwnerType.PLAYER
                    || snapshot.ownerType() == VehicleRuntimeAuthority.OwnerType.SALVAGE
                    || "salvaged".equals(snapshot.condition())
                    || VehicleRuntimeAuthority.factionOwns(vehicle, site.faction)) return false;
            Faction target = plan.schemeTargetFaction == null
                    ? Faction.NONE : plan.schemeTargetFaction;
            if (target == Faction.NONE) return true;
            return snapshot.ownerFaction() != Faction.NONE
                    && FactionIdentityAuthority.sameFamily(snapshot.ownerFaction(), target);
        });
    }

    private interface Predicate {
        boolean test(MapObjectState vehicle);
    }

    private static MapObjectState first(GamePanel game, Predicate predicate) {
        if (game == null || game.world == null || game.world.mapObjects == null) return null;
        ArrayList<MapObjectState> candidates = new ArrayList<>();
        for (MapObjectState object : game.world.mapObjects) {
            if (!VehicleRuntimeAuthority.isVehicle(object)) continue;
            VehicleRuntimeAuthority.ensureInitialized(game.world, object);
            if (predicate.test(object)) candidates.add(object);
        }
        candidates.sort(Comparator
                .comparing((MapObjectState object) -> displayName(object))
                .thenComparing(object -> clean(object.id, object.label)));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private static VehicleRuntimeAuthority.Component worstComponent(
            MapObjectState vehicle) {
        VehicleRuntimeAuthority.Component worst = null;
        int lowest = 101;
        for (VehicleRuntimeAuthority.Component component
                : VehicleRuntimeAuthority.Component.values()) {
            int value = component(vehicle, component);
            if (value < lowest) {
                lowest = value;
                worst = component;
            }
        }
        return lowest >= 100 ? null : worst;
    }

    private static int component(MapObjectState vehicle,
                                 VehicleRuntimeAuthority.Component component) {
        String key = "vehicleComponent" + component.name().charAt(0)
                + component.name().substring(1).toLowerCase(Locale.ROOT);
        try {
            return Integer.parseInt(MapObjectState.stockValue(vehicle.stockState, key));
        } catch (Exception ignored) {
            return 100;
        }
    }

    private static FactionStrategicAssetAuthority.Outcome blocked(
            String blocker, String message, NpcFactionSite site) {
        int stock = site == null ? 0 : Math.max(0, site.stock);
        return new FactionStrategicAssetAuthority.Outcome(true, false,
                clean(blocker, "vehicle-operation-blocked"),
                clean(message, "Faction vehicle operation was blocked."),
                -1, stock, stock, null, null);
    }

    private static boolean localSite(NpcFactionSite site, World world) {
        return site != null && world != null
                && site.sectorX == world.sectorX && site.sectorY == world.sectorY
                && site.zoneX == world.zoneX && site.zoneY == world.zoneY
                && site.floor == world.floor;
    }

    private static String displayName(MapObjectState vehicle) {
        VehicleRuntimeAuthority.Snapshot snapshot =
                VehicleRuntimeAuthority.inspect(null, vehicle);
        if (snapshot == null) return "vehicle";
        return (clean(snapshot.manufacturer(), "") + " "
                + clean(snapshot.model(), snapshot.vehicleClass().label)).trim();
    }

    private static String faction(Faction faction) {
        return faction == null || faction == Faction.NONE
                ? "Unaligned" : faction.label;
    }

    private static String clean(String value, String fallback) {
        String text = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return text.isBlank() ? fallback : text;
    }
}
