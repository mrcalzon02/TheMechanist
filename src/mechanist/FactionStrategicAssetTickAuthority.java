package mechanist;

/**
 * Resolves physical room, machine, specialist, vehicle, and route-control plans
 * as their execution phase opens. Ordinary physical attempts are terminal in one
 * pass. Route-control deployment remains in execution while its authoritative
 * strategic transfer is reserved or committing, then closes exactly once after
 * completion or cancellation.
 */
final class FactionStrategicAssetTickAuthority {
    private static final String SPECIALIST_ID_PREFIX =
            "FACTION-FACILITY-SPECIALIST-";

    private FactionStrategicAssetTickAuthority() { }

    static int tick(GamePanel game) {
        if (game == null || game.world == null
                || game.factionStrategicPlans == null) return 0;
        int resolved = 0;
        for (FactionStrategicPlan plan : game.factionStrategicPlans) {
            promoteSequentialPhysicalGoal(game, plan);
            boolean facilityPlan =
                    FactionStrategicAssetAuthority.handles(plan);
            boolean vehiclePlan =
                    FactionVehicleStrategicAuthority.handles(plan);
            boolean routePlan =
                    FactionVehicleRouteStrategicAuthority.handles(plan);
            if (plan == null
                    || (!facilityPlan && !vehiclePlan && !routePlan)) continue;

            // If this tick reaches a due physical plan immediately before the
            // legacy strategy tick, open execution here. If the legacy tick ran
            // first, the plan is already in EXECUTION. Either order reaches the
            // physical authority before the abstract execution deadline.
            if ("PLANNING".equals(plan.phase)
                    && game.turn >= plan.phaseUntilTurn) {
                plan.advancePhase(game.rng, game.turn);
            }
            if (!"EXECUTION".equals(plan.phase)) continue;

            NpcFactionSite site = game.siteForFaction(
                    plan.faction, game.world.zoneType);
            FactionStrategicAssetAuthority.Outcome outcome;
            if (routePlan) {
                FactionVehicleRouteStrategicAuthority.Progress progress =
                        FactionVehicleRouteStrategicAuthority.advance(
                                game, plan, site);
                if (!progress.handled()) continue;
                if (progress.pending()) {
                    deferPendingRoutePlan(plan, game.turn);
                    if (progress.changed()) {
                        int motorPoolChanges = site == null ? 0
                                : VehicleMotorPoolAuthority.reconcileSiteFleet(
                                game, site, plan.immediateGoal);
                        int fleetPower = site == null ? -1
                                : FactionVehicleDoctrineAuthority.fleet(
                                game, site.faction, site).totalPower();
                        plan.lastOutcome = "IN PROGRESS: "
                                + progress.message();
                        plan.addHistory(game.turn, plan.lastOutcome);
                        DebugLog.audit(
                                "FACTION_STRATEGIC_ASSET_OPERATION",
                                "faction=" + plan.faction.label
                                        + " goal=" + plan.immediateGoal
                                        + " terminal=false success=false"
                                        + " blocker=" + progress.blocker()
                                        + " stock=" + progress.stockBefore()
                                        + "->" + progress.stockAfter()
                                        + " motorPoolChanges="
                                        + motorPoolChanges
                                        + " fleetPower=" + fleetPower);
                    }
                    continue;
                }
                outcome = progress.outcome();
            } else if (facilityPlan
                    && FactionStrategicAssetAuthority
                    .CAPTURED_ASSET_SALVAGE_GOAL.equalsIgnoreCase(
                    plan.immediateGoal == null
                            ? "" : plan.immediateGoal.trim())
                    && site != null && site.stock >= 160) {
                outcome = FactionStrategicAssetAuthority.Outcome.blocked(
                        "faction-stock-capacity",
                        site.name
                                + " is at its 160-unit stock cap; captured machinery remains intact until storage capacity is available.",
                        site, -1, null);
            } else if (vehiclePlan) {
                outcome = FactionVehicleStrategicAuthority.attempt(
                        game, plan, site);
            } else {
                outcome = FactionStrategicAssetAuthority.attempt(
                        game, plan, site);
            }
            if (!outcome.handled()) continue;
            resolved += resolveTerminal(game, plan, site,
                    vehiclePlan, routePlan, outcome);
        }
        return resolved;
    }

    /**
     * Completes a terminal route progress result produced by a resolved-world
     * transfer bridge. Keeping this accounting here guarantees that direct
     * transfer completion and ordinary tick completion share the same counters,
     * audit, rumor, motor-pool reconciliation, market pressure, and cooldown.
     */
    static int finalizeRouteProgress(
            GamePanel game, FactionStrategicPlan plan,
            NpcFactionSite site,
            FactionVehicleRouteStrategicAuthority.Progress progress) {
        if (progress == null || !progress.handled()
                || progress.pending()) return 0;
        return resolveTerminal(game, plan, site,
                false, true, progress.outcome());
    }

    private static int resolveTerminal(
            GamePanel game, FactionStrategicPlan plan,
            NpcFactionSite site, boolean vehiclePlan,
            boolean routePlan,
            FactionStrategicAssetAuthority.Outcome outcome) {
        if (game == null || plan == null || outcome == null
                || !outcome.handled()) return 0;
        int motorPoolChanges = 0;
        int fleetPower = -1;
        if (outcome.success() && (vehiclePlan || routePlan)
                && site != null) {
            motorPoolChanges =
                    VehicleMotorPoolAuthority.reconcileSiteFleet(
                            game, site, plan.immediateGoal);
            fleetPower = FactionVehicleDoctrineAuthority.fleet(
                    game, site.faction, site).totalPower();
        }
        if (outcome.success()) {
            plan.success++;
        } else {
            plan.failure++;
            game.addFactionMarketPressure(plan.faction, 1,
                    "blocked physical strategic asset operation: "
                            + outcome.blocker());
        }
        String prefix = outcome.success()
                ? "SUCCESS: " : "FAILURE: ";
        plan.lastOutcome = prefix + outcome.message();
        plan.addHistory(game.turn, plan.lastOutcome);
        DebugLog.audit("FACTION_STRATEGIC_ASSET_OPERATION",
                "faction=" + plan.faction.label
                        + " goal=" + plan.immediateGoal
                        + " terminal=true success=" + outcome.success()
                        + " blocker=" + outcome.blocker()
                        + " room=" + outcome.roomId()
                        + " stock=" + outcome.stockBefore()
                        + "->" + outcome.stockAfter()
                        + " motorPoolChanges=" + motorPoolChanges
                        + " fleetPower=" + fleetPower);
        if (game.rng.nextInt(100)
                < Math.max(10, 65 - plan.secrecy / 2)) {
            game.logEvent("RUMOR: " + plan.publicLine());
        }

        // Terminal physical resolution advances to cooldown before the
        // later abstract strategy resolver can mutate the same plan again.
        plan.advancePhase(game.rng, game.turn);
        return 1;
    }

    private static void deferPendingRoutePlan(FactionStrategicPlan plan,
                                               int turn) {
        if (plan == null) return;
        int next = Math.max(0, turn) + 2;
        plan.phaseUntilTurn = Math.max(plan.phaseUntilTurn, next);
        plan.nextDecisionTurn = Math.max(plan.nextDecisionTurn, next);
    }

    /**
     * Converts ordinary planning into the next available physical follow-up:
     * captured machinery, captured or damaged faction vehicles, concrete route
     * deployment, opportunistic vehicle seizure, then a visible specialist for
     * an operational facility that still lacks one.
     */
    private static void promoteSequentialPhysicalGoal(
            GamePanel game, FactionStrategicPlan plan) {
        if (plan == null || !"PLANNING".equals(plan.phase)
                || FactionStrategicAssetAuthority.handles(plan)
                || FactionVehicleStrategicAuthority.handles(plan)
                || FactionVehicleRouteStrategicAuthority.handles(plan)) {
            return;
        }
        NpcFactionSite site = game.siteForFaction(
                plan.faction, game.world.zoneType);
        if (!localSite(site, game.world)) return;

        int salvageRoom = capturedAssetRoom(game, site);
        if (salvageRoom >= 0) {
            plan.immediateGoal = FactionStrategicAssetAuthority
                    .CAPTURED_ASSET_SALVAGE_GOAL;
            plan.targetRoom = RoomOwnershipAuthority.roomName(
                    game.world, salvageRoom);
            plan.targetItem = "Machine part";
            plan.addHistory(game.turn,
                    "Planning redirected to physical salvage because "
                            + plan.targetRoom
                            + " contains captured foreign machinery with assigned workers.");
            return;
        }

        FactionVehicleStrategicAuthority.Suggestion vehicle =
                FactionVehicleStrategicAuthority.nextSuggestion(
                        game, site, plan);
        if (vehicle.available()
                && urgentVehicleMaintenanceGoal(vehicle.goal())) {
            plan.immediateGoal = vehicle.goal();
            plan.targetRoom = vehicle.target();
            plan.targetItem = "Machine part";
            plan.addHistory(game.turn,
                    "Planning redirected to " + vehicle.goal() + " because "
                            + vehicle.reason() + ". Target: "
                            + vehicle.target() + ".");
            return;
        }

        FactionVehicleRouteStrategicAuthority.Suggestion route =
                FactionVehicleRouteStrategicAuthority.nextSuggestion(
                        game, site, plan);
        if (route.available()) {
            plan.immediateGoal = route.goal();
            plan.targetRoom = route.target();
            plan.targetItem = "Fuel reserve";
            plan.addHistory(game.turn,
                    "Planning redirected to " + route.goal() + " because "
                            + route.reason() + ". Target route: "
                            + route.target() + ".");
            return;
        }

        if (vehicle.available()) {
            plan.immediateGoal = vehicle.goal();
            plan.targetRoom = vehicle.target();
            plan.targetItem = "Machine part";
            plan.addHistory(game.turn,
                    "Planning redirected to " + vehicle.goal() + " because "
                            + vehicle.reason() + ". Target: "
                            + vehicle.target() + ".");
            return;
        }

        int specialistRoom = facilityNeedingSpecialistRoom(game, site);
        if (specialistRoom >= 0) {
            plan.immediateGoal = FactionStrategicAssetAuthority
                    .FACILITY_SPECIALIST_GOAL;
            plan.targetRoom = RoomOwnershipAuthority.roomName(
                    game.world, specialistRoom);
            plan.targetItem = "Tool bundle";
            plan.addHistory(game.turn,
                    "Planning redirected to specialist deployment because "
                            + plan.targetRoom
                            + " has an operational staffed facility without a visible specialist.");
        }
    }

    private static boolean urgentVehicleMaintenanceGoal(String goal) {
        String safe = goal == null ? "" : goal.trim();
        return FactionVehicleStrategicAuthority.VEHICLE_REPAIR_GOAL
                .equalsIgnoreCase(safe)
                || FactionVehicleStrategicAuthority.VEHICLE_SALVAGE_GOAL
                .equalsIgnoreCase(safe);
    }

    private static int capturedAssetRoom(GamePanel game,
                                         NpcFactionSite site) {
        if (game == null || game.world == null
                || game.baseObjects == null) return -1;
        int best = Integer.MAX_VALUE;
        for (BaseObject object : game.baseObjects) {
            if (object == null || object.underConstruction
                    || !MachineTierAuthority
                    .isMachineOrFacilitySymbol(object.symbol)) continue;
            int roomId = game.world.roomIdAt(object.x, object.y);
            if (roomId < 0 || roomId >= best
                    || !FactionIdentityAuthority.sameFamily(
                    game.world.roomFaction(roomId), site.faction)
                    || assignedWorkers(game.world, roomId,
                    site.faction) <= 0) continue;
            Faction custodian = object.faction == null
                    ? Faction.NONE : object.faction;
            if (custodian == Faction.HIVER
                    || FactionIdentityAuthority.sameFamily(
                    custodian, site.faction)) continue;
            best = roomId;
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    private static int facilityNeedingSpecialistRoom(
            GamePanel game, NpcFactionSite site) {
        if (game == null || game.world == null
                || game.baseObjects == null) return -1;
        int best = Integer.MAX_VALUE;
        for (BaseObject object : game.baseObjects) {
            if (object == null || object.underConstruction
                    || !MachineTierAuthority
                    .isMachineOrFacilitySymbol(object.symbol)) continue;
            int roomId = game.world.roomIdAt(object.x, object.y);
            if (roomId < 0 || roomId >= best
                    || !FactionIdentityAuthority.sameFamily(
                    game.world.roomFaction(roomId), site.faction)
                    || assignedWorkers(game.world, roomId,
                    site.faction) <= 0
                    || hasSpecialist(game.world, roomId,
                    site.faction)) continue;
            Faction custodian = object.faction == null
                    ? Faction.NONE : object.faction;
            if (custodian != Faction.NONE
                    && !FactionIdentityAuthority.sameFamily(
                    custodian, site.faction)) continue;
            best = roomId;
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    private static boolean hasSpecialist(World world, int roomId,
                                         Faction faction) {
        if (world == null || world.npcs == null) return false;
        for (NpcEntity npc : world.npcs) {
            if (npc == null || npc.id == null
                    || !npc.id.startsWith(
                    SPECIALIST_ID_PREFIX)) continue;
            if (world.roomIdAt(npc.x, npc.y) == roomId
                    && FactionIdentityAuthority.sameFamily(
                    npc.faction, faction)) return true;
        }
        return false;
    }

    private static int assignedWorkers(World world, int roomId,
                                       Faction faction) {
        if (world == null
                || world.roomPopulationLedgers == null) return 0;
        long assigned = 0;
        for (RoomPopulationLedger ledger
                : world.roomPopulationLedgers) {
            if (ledger == null || ledger.roomId != roomId
                    || !FactionIdentityAuthority.sameFamily(
                    ledger.faction, faction)) continue;
            assigned += Math.max(0, ledger.assigned);
        }
        return (int)Math.min(Integer.MAX_VALUE, assigned);
    }

    private static boolean localSite(NpcFactionSite site, World world) {
        return site != null && world != null
                && site.sectorX == world.sectorX
                && site.sectorY == world.sectorY
                && site.zoneX == world.zoneX
                && site.zoneY == world.zoneY
                && site.floor == world.floor;
    }
}
