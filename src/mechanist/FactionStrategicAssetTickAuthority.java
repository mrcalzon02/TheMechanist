package mechanist;

/**
 * Resolves physical room, machine, specialist, vehicle, and route-control plans
 * as their execution phase opens. Completed attempts advance directly into
 * cooldown before the later abstract execution deadline can award a second
 * result.
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
            // same physical resolution before the abstract execution deadline.
            if ("PLANNING".equals(plan.phase)
                    && game.turn >= plan.phaseUntilTurn) {
                plan.advancePhase(game.rng, game.turn);
            }
            if (!"EXECUTION".equals(plan.phase)) continue;

            NpcFactionSite site = game.siteForFaction(
                    plan.faction, game.world.zoneType);
            FactionStrategicAssetAuthority.Outcome outcome;
            if (facilityPlan
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
            } else if (routePlan) {
                outcome = FactionVehicleRouteStrategicAuthority.attempt(
                        game, plan, site);
            } else if (vehiclePlan) {
                outcome = FactionVehicleStrategicAuthority.attempt(
                        game, plan, site);
            } else {
                outcome = FactionStrategicAssetAuthority.attempt(
                        game, plan, site);
            }
            if (!outcome.handled()) continue;

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
                            + " success=" + outcome.success()
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

            // EXECUTION -> COOLDOWN here prevents the later abstract strategy
            // resolver from mutating the same plan or asset a second time.
            plan.advancePhase(game.rng, game.turn);
            resolved++;
        }
        return resolved;
    }

    /**
     * Converts ordinary planning into the next available physical follow-up:
     * captured machinery, concrete route-control deployment, seized/damaged or
     * scheme-targeted vehicles, then a visible specialist for an operational
     * facility that still lacks one.
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

        FactionVehicleStrategicAuthority.Suggestion vehicle =
                FactionVehicleStrategicAuthority.nextSuggestion(
                        game, site, plan);
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
