package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Persistent motor-pool assignment and readiness over the authoritative vehicle
 * fixture. Assignment truth remains inside MapObjectState so existing world/save
 * serialization owns it; readiness is derived from live ownership, location,
 * condition, crew, fuel, and operation state.
 */
final class VehicleMotorPoolAuthority {
    record Snapshot(boolean assigned, String siteKey, String siteName,
                    Faction faction, String role, String state,
                    boolean ownerAligned, boolean siteLocal,
                    boolean driverAssigned, int assignedCrew,
                    int requiredCrew, int fuel, int fuelCapacity,
                    boolean operational, List<String> blockers,
                    List<String> history) { }

    record Result(boolean success, boolean changed, String action,
                  String message, Snapshot snapshot) {
        static Result blocked(String action, String message,
                              Snapshot snapshot) {
            return new Result(false, false, clean(action,
                    "motor-pool action"), clean(message,
                    "Motor-pool action was blocked."), snapshot);
        }
    }

    private VehicleMotorPoolAuthority() { }

    static Result assign(GamePanel game, MapObjectState vehicle,
                         NpcFactionSite site, String role, String reason) {
        Snapshot before = inspect(game, vehicle, site);
        if (game == null || game.world == null || vehicle == null
                || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            return Result.blocked("assign motor pool",
                    "A physical vehicle in the loaded world is required.", before);
        }
        if (!localSite(site, game.world)) {
            return Result.blocked("assign motor pool",
                    "A local faction site is required for motor-pool assignment.",
                    before);
        }
        VehicleRuntimeAuthority.ensureInitialized(game.world, vehicle);
        VehicleRuntimeAuthority.Snapshot vehicleState =
                VehicleRuntimeAuthority.inspect(game.world, vehicle);
        if (vehicleState == null
                || vehicleState.ownerType()
                != VehicleRuntimeAuthority.OwnerType.FACTION
                || vehicleState.ownerFaction() == Faction.NONE
                || !FactionIdentityAuthority.sameFamily(
                vehicleState.ownerFaction(), site.faction)) {
            return Result.blocked("assign motor pool",
                    "The vehicle must be owned by the assigning faction family.",
                    before);
        }
        if ("salvaged".equalsIgnoreCase(vehicleState.condition())) {
            return Result.blocked("assign motor pool",
                    "A stripped vehicle hulk cannot receive an active motor-pool assignment.",
                    before);
        }
        String assignmentRole = clean(role,
                vehicleState.vehicleClass().role);
        String targetKey = siteKey(site);
        String priorKey = value(vehicle, "motorPoolSiteKey");
        String priorRole = value(vehicle, "motorPoolRole");
        boolean changed = !targetKey.equals(priorKey)
                || !assignmentRole.equalsIgnoreCase(priorRole);
        if (!changed) {
            Result reconciled = reconcile(game, vehicle, site,
                    "assignment rechecked");
            return new Result(true, reconciled.changed(),
                    "assign motor pool",
                    reconciled.changed()
                            ? "MOTOR POOL: existing assignment was refreshed."
                            : "The same motor-pool assignment is already active.",
                    reconciled.snapshot());
        }
        if (!priorKey.isBlank() && !priorKey.equals(targetKey)) {
            append(vehicle, "motorPoolHistory", "Reassigned from "
                    + clean(value(vehicle, "motorPoolSiteName"),
                    "another motor pool") + " / role "
                    + clean(priorRole, "unrecorded") + turn(game));
        }
        set(vehicle, "motorPoolSiteKey", targetKey);
        set(vehicle, "motorPoolSiteName", clean(site.name,
                site.faction.label + " motor pool"));
        set(vehicle, "motorPoolFaction", site.faction.name());
        set(vehicle, "motorPoolRole", assignmentRole);
        set(vehicle, "motorPoolAssignedTurn",
                Integer.toString(game.turn));
        set(vehicle, "motorPoolAssignmentReason", clean(reason,
                "local faction fleet registration"));
        String state = derivedState(game, vehicle, site);
        set(vehicle, "motorPoolState", state);
        append(vehicle, "motorPoolHistory", "Assigned to "
                + clean(site.name, site.faction.label + " motor pool")
                + " / role " + assignmentRole + " / state " + state
                + " / " + clean(reason,
                "local faction fleet registration") + turn(game));
        append(vehicle, "deploymentHistory", "Motor-pool assignment "
                + clean(site.name, site.faction.label + " motor pool")
                + " / role " + assignmentRole + " / state " + state);
        return new Result(true, true, "assign motor pool",
                "MOTOR POOL: " + displayName(game.world, vehicle)
                        + " assigned to " + clean(site.name,
                        site.faction.label + " motor pool") + " as "
                        + assignmentRole + "; readiness " + state + ".",
                inspect(game, vehicle, site));
    }

    static Result reconcile(GamePanel game, MapObjectState vehicle,
                            NpcFactionSite site, String reason) {
        Snapshot before = inspect(game, vehicle, site);
        if (!before.assigned()) {
            return Result.blocked("reconcile motor pool",
                    "The vehicle has no active motor-pool assignment.", before);
        }
        if (game == null || game.world == null || !localSite(site, game.world)
                || !before.siteKey().equals(siteKey(site))) {
            return Result.blocked("reconcile motor pool",
                    "The active motor-pool assignment does not belong to this local site.",
                    before);
        }
        String next = derivedState(game, vehicle, site);
        String prior = clean(value(vehicle, "motorPoolState"),
                before.state());
        boolean changed = !next.equalsIgnoreCase(prior);
        if (changed) {
            set(vehicle, "motorPoolState", next);
            append(vehicle, "motorPoolHistory", "Readiness " + prior
                    + " -> " + next + " / " + clean(reason,
                    "fleet state reconciled") + turn(game));
        }
        return new Result(true, changed, "reconcile motor pool",
                changed ? "MOTOR POOL: readiness changed from " + prior
                        + " to " + next + "."
                        : "Motor-pool readiness remains " + next + ".",
                inspect(game, vehicle, site));
    }

    static Result release(GamePanel game, MapObjectState vehicle,
                          NpcFactionSite site, String reason) {
        Snapshot before = inspect(game, vehicle, site);
        if (!before.assigned()) {
            return new Result(true, false, "release motor pool",
                    "The vehicle has no active motor-pool assignment.", before);
        }
        if (site != null && !before.siteKey().equals(siteKey(site))) {
            return Result.blocked("release motor pool",
                    "Only the currently assigned motor pool may release this vehicle.",
                    before);
        }
        String transit = value(vehicle, "strategicTransitState")
                .toLowerCase(Locale.ROOT);
        String operation = value(vehicle, "operationState")
                .toLowerCase(Locale.ROOT);
        if (transit.equals("reserved") || transit.equals("committing")
                || operation.equals("running")) {
            return Result.blocked("release motor pool",
                    "Cancel or complete the active vehicle deployment before releasing its motor-pool assignment.",
                    before);
        }
        return releaseInternal(game, vehicle, clean(reason,
                "fleet assignment ended"));
    }

    static int reconcileSiteFleet(GamePanel game, NpcFactionSite site,
                                  String reason) {
        if (game == null || game.world == null || !localSite(site, game.world)
                || game.world.mapObjects == null) return 0;
        int changed = 0;
        String activeSiteKey = siteKey(site);
        for (MapObjectState vehicle : game.world.mapObjects) {
            if (!VehicleRuntimeAuthority.isVehicle(vehicle)) continue;
            VehicleRuntimeAuthority.ensureInitialized(game.world, vehicle);
            Snapshot current = inspect(game, vehicle, site);
            String condition = value(vehicle, "condition")
                    .toLowerCase(Locale.ROOT);
            if (current.assigned() && current.siteKey().equals(activeSiteKey)
                    && condition.equals("salvaged")) {
                if (releaseInternal(game, vehicle,
                        "vehicle removed from active fleet after salvage").changed()) {
                    changed++;
                }
                continue;
            }
            if (current.assigned() && !current.siteKey().equals(activeSiteKey)) {
                continue;
            }
            if (!VehicleRuntimeAuthority.factionOwns(vehicle, site.faction)) {
                continue;
            }
            Result result;
            if (!current.assigned()) {
                result = assign(game, vehicle, site,
                        VehicleRuntimeAuthority.vehicleClass(vehicle.type).role,
                        clean(reason, "successful faction vehicle operation"));
            } else {
                result = reconcile(game, vehicle, site,
                        clean(reason, "successful faction vehicle operation"));
            }
            if (result.success() && result.changed()) changed++;
        }
        return changed;
    }

    static Snapshot inspect(GamePanel game, MapObjectState vehicle,
                            NpcFactionSite site) {
        if (vehicle == null || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            return new Snapshot(false, "", "", Faction.NONE, "",
                    "unassigned", false, false, false, 0, 0,
                    0, 0, false, List.of("no physical vehicle"),
                    List.of());
        }
        World world = game == null ? null : game.world;
        VehicleRuntimeAuthority.ensureInitialized(world, vehicle);
        VehicleRuntimeAuthority.Snapshot vehicleState =
                VehicleRuntimeAuthority.inspect(world, vehicle);
        VehicleManifestAuthority.Snapshot manifest =
                VehicleManifestAuthority.inspect(world, vehicle);
        VehicleFuelAuthority.Snapshot fuel =
                VehicleFuelAuthority.inspect(world, vehicle);
        String key = value(vehicle, "motorPoolSiteKey");
        String name = value(vehicle, "motorPoolSiteName");
        Faction faction = parseFaction(value(vehicle, "motorPoolFaction"));
        String role = value(vehicle, "motorPoolRole");
        boolean assigned = !key.isBlank();
        boolean ownerAligned = vehicleState != null
                && vehicleState.ownerType()
                == VehicleRuntimeAuthority.OwnerType.FACTION
                && faction != Faction.NONE
                && vehicleState.ownerFaction() != Faction.NONE
                && FactionIdentityAuthority.sameFamily(
                vehicleState.ownerFaction(), faction);
        boolean local = site != null && localSite(site, world)
                && key.equals(siteKey(site));
        int required = vehicleState == null ? 0
                : Math.max(1, vehicleState.vehicleClass().crewRequired);
        boolean operational = vehicleState != null
                && !"wreck".equalsIgnoreCase(vehicleState.condition())
                && !"salvaged".equalsIgnoreCase(vehicleState.condition())
                && !"disabled".equalsIgnoreCase(vehicleState.operationState())
                && !"dismantled".equalsIgnoreCase(vehicleState.operationState());
        ArrayList<String> blockers = blockers(vehicle, site,
                assigned, ownerAligned, local, manifest, fuel,
                required, operational);
        String state = assigned ? derivedState(game, vehicle, site)
                : clean(value(vehicle, "motorPoolState"), "unassigned");
        return new Snapshot(assigned, key, name, faction, role, state,
                ownerAligned, local, !manifest.driver().isBlank(),
                manifest.assignedCrew(), required, fuel.current(),
                fuel.capacity(), operational, List.copyOf(blockers),
                history(vehicle));
    }

    static List<String> inspectionLines(GamePanel game,
                                        MapObjectState vehicle,
                                        NpcFactionSite site) {
        Snapshot snapshot = inspect(game, vehicle, site);
        ArrayList<String> lines = new ArrayList<>();
        if (!snapshot.assigned()) {
            lines.add("Motor pool: no active assignment.");
        } else {
            lines.add("Motor pool: " + clean(snapshot.siteName(),
                    "unnamed faction site") + "; role "
                    + clean(snapshot.role(), "unrecorded") + "; readiness "
                    + snapshot.state() + ".");
        }
        lines.add("Fleet ownership: "
                + (snapshot.ownerAligned() ? "aligned with the assigned faction"
                : "not aligned with the assigned faction") + ".");
        lines.add("Crew readiness: driver "
                + (snapshot.driverAssigned() ? "assigned" : "unassigned")
                + "; operational crew " + snapshot.assignedCrew() + "/"
                + snapshot.requiredCrew() + ".");
        lines.add("Fleet energy: " + snapshot.fuel() + "/"
                + snapshot.fuelCapacity() + " fuel or power unit(s).");
        for (String blocker : snapshot.blockers()) {
            lines.add("Motor-pool blocker: " + blocker + ".");
        }
        return List.copyOf(lines);
    }

    private static Result releaseInternal(GamePanel game,
                                          MapObjectState vehicle,
                                          String reason) {
        String priorSite = clean(value(vehicle, "motorPoolSiteName"),
                "unnamed motor pool");
        String priorRole = clean(value(vehicle, "motorPoolRole"),
                "unrecorded");
        set(vehicle, "lastMotorPoolSiteKey",
                value(vehicle, "motorPoolSiteKey"));
        set(vehicle, "lastMotorPoolSiteName", priorSite);
        set(vehicle, "lastMotorPoolRole", priorRole);
        set(vehicle, "motorPoolSiteKey", "");
        set(vehicle, "motorPoolSiteName", "");
        set(vehicle, "motorPoolFaction", "");
        set(vehicle, "motorPoolRole", "");
        set(vehicle, "motorPoolState", "released");
        set(vehicle, "motorPoolReleasedTurn",
                Integer.toString(game == null ? 0 : game.turn));
        append(vehicle, "motorPoolHistory", "Released from " + priorSite
                + " / role " + priorRole + " / " + reason + turn(game));
        append(vehicle, "deploymentHistory", "Motor-pool release "
                + priorSite + " / " + reason);
        return new Result(true, true, "release motor pool",
                "MOTOR POOL: vehicle released from " + priorSite
                        + "; " + reason + ".",
                inspect(game, vehicle, null));
    }

    private static String derivedState(GamePanel game,
                                       MapObjectState vehicle,
                                       NpcFactionSite site) {
        if (vehicle == null || value(vehicle, "motorPoolSiteKey").isBlank()) {
            return "unassigned";
        }
        VehicleRuntimeAuthority.Snapshot vehicleState =
                VehicleRuntimeAuthority.inspect(game == null ? null : game.world,
                        vehicle);
        VehicleManifestAuthority.Snapshot manifest =
                VehicleManifestAuthority.inspect(game == null ? null : game.world,
                        vehicle);
        VehicleFuelAuthority.Snapshot fuel =
                VehicleFuelAuthority.inspect(game == null ? null : game.world,
                        vehicle);
        if (site == null || !localSite(site,
                game == null ? null : game.world)
                || !value(vehicle, "motorPoolSiteKey").equals(siteKey(site))) {
            return "detached";
        }
        if (vehicleState == null
                || vehicleState.ownerType()
                != VehicleRuntimeAuthority.OwnerType.FACTION
                || vehicleState.ownerFaction() == Faction.NONE
                || !FactionIdentityAuthority.sameFamily(
                vehicleState.ownerFaction(), site.faction)) {
            return "ownership-blocked";
        }
        String transit = value(vehicle, "strategicTransitState")
                .toLowerCase(Locale.ROOT);
        if (transit.equals("reserved") || transit.equals("committing")) {
            return "deployment-reserved";
        }
        if ("running".equalsIgnoreCase(vehicleState.operationState())) {
            return "deployed";
        }
        if ("wreck".equalsIgnoreCase(vehicleState.condition())
                || "salvaged".equalsIgnoreCase(vehicleState.condition())
                || "disabled".equalsIgnoreCase(vehicleState.operationState())
                || VehicleRuntimeAuthority.damaged(vehicle)) {
            return "maintenance";
        }
        int required = Math.max(1, vehicleState.vehicleClass().crewRequired);
        if (manifest.driver().isBlank()
                || manifest.assignedCrew() < required) {
            return "uncrewed";
        }
        if (fuel.current() <= 0) return "refuel";
        return "ready";
    }

    private static ArrayList<String> blockers(
                                              MapObjectState vehicle,
                                              NpcFactionSite site,
                                              boolean assigned,
                                              boolean ownerAligned,
                                              boolean local,
                                              VehicleManifestAuthority.Snapshot manifest,
                                              VehicleFuelAuthority.Snapshot fuel,
                                              int required,
                                              boolean operational) {
        ArrayList<String> blockers = new ArrayList<>();
        if (!assigned) blockers.add("assign the vehicle to a motor pool");
        if (assigned && !ownerAligned) {
            blockers.add("restore aligned faction ownership");
        }
        if (assigned && site != null && !local) {
            blockers.add("load or select the assigned local motor pool");
        }
        if (!operational) blockers.add("restore the vehicle to an operational state");
        if (VehicleRuntimeAuthority.damaged(vehicle)) {
            blockers.add("complete required vehicle maintenance");
        }
        if (manifest.driver().isBlank()) blockers.add("assign a driver");
        if (manifest.assignedCrew() < required) {
            blockers.add("assign " + required + " operational crew member(s); only "
                    + manifest.assignedCrew() + " are assigned");
        }
        if (fuel.current() <= 0) blockers.add("refuel or recharge the vehicle");
        return blockers;
    }

    private static boolean localSite(NpcFactionSite site, World world) {
        return site != null && world != null
                && site.sectorX == world.sectorX
                && site.sectorY == world.sectorY
                && site.zoneX == world.zoneX
                && site.zoneY == world.zoneY
                && site.floor == world.floor;
    }

    private static String siteKey(NpcFactionSite site) {
        if (site == null) return "";
        return site.faction.name() + "@" + site.sectorX + ","
                + site.sectorY + "," + site.zoneX + "," + site.zoneY
                + "," + site.floor + "#" + clean(site.name,
                "motor-pool").toLowerCase(Locale.ROOT);
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
        String text = value(vehicle, "motorPoolHistory");
        if (!text.isBlank()) {
            for (String token : text.split("~")) {
                String cleaned = clean(token, "");
                if (!cleaned.isBlank()) history.add(cleaned);
            }
        }
        return List.copyOf(history);
    }

    private static String displayName(World world, MapObjectState vehicle) {
        VehicleRuntimeAuthority.Snapshot snapshot =
                VehicleRuntimeAuthority.inspect(world, vehicle);
        if (snapshot == null) return "vehicle";
        return (clean(snapshot.manufacturer(), "") + " "
                + clean(snapshot.model(), snapshot.vehicleClass().label)).trim();
    }

    private static String turn(GamePanel game) {
        return " / turn " + (game == null ? 0 : Math.max(0, game.turn));
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
