package mechanist;

/**
 * Resolves physical strategic-asset plans as their execution phase opens.
 * Completed attempts advance directly into cooldown before the later abstract
 * execution deadline can award a second scripted result.
 */
final class FactionStrategicAssetTickAuthority {
    private static final String SPECIALIST_ID_PREFIX = "FACTION-FACILITY-SPECIALIST-";

    private FactionStrategicAssetTickAuthority() { }

    static int tick(GamePanel game) {
        if (game == null || game.world == null || game.factionStrategicPlans == null) return 0;
        int resolved = 0;
        for (FactionStrategicPlan plan : game.factionStrategicPlans) {
            promoteSequentialPhysicalGoal(game, plan);
            if (plan == null || !FactionStrategicAssetAuthority.handles(plan)) continue;

            // If this tick reaches a due physical plan immediately before the
            // legacy strategy tick, open execution here. If the legacy tick ran
            // first, the plan is already in EXECUTION. Either order reaches the
            // same physical resolution before the abstract execution deadline.
            if ("PLANNING".equals(plan.phase) && game.turn >= plan.phaseUntilTurn) {
                plan.advancePhase(game.rng, game.turn);
            }
            if (!"EXECUTION".equals(plan.phase)) continue;

            NpcFactionSite site = game.siteForFaction(plan.faction, game.world.zoneType);
            FactionStrategicAssetAuthority.Outcome outcome =
                    FactionStrategicAssetAuthority.attempt(game, plan, site);
            if (!outcome.handled()) continue;

            if (outcome.success()) {
                plan.success++;
            } else {
                plan.failure++;
                game.addFactionMarketPressure(plan.faction, 1,
                        "blocked physical strategic asset operation: " + outcome.blocker());
            }
            String prefix = outcome.success() ? "SUCCESS: " : "FAILURE: ";
            plan.lastOutcome = prefix + outcome.message();
            plan.addHistory(game.turn, plan.lastOutcome);
            DebugLog.audit("FACTION_STRATEGIC_ASSET_OPERATION",
                    "faction=" + plan.faction.label
                            + " goal=" + plan.immediateGoal
                            + " success=" + outcome.success()
                            + " blocker=" + outcome.blocker()
                            + " room=" + outcome.roomId()
                            + " stock=" + outcome.stockBefore() + "->" + outcome.stockAfter());
            if (game.rng.nextInt(100) < Math.max(10, 65 - plan.secrecy / 2)) {
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
     * first salvage foreign machinery in a controlled room, then materialize a
     * specialist for an operational facility that still lacks one.
     */
    private static void promoteSequentialPhysicalGoal(GamePanel game,
                                                      FactionStrategicPlan plan) {
        if (plan == null || !"PLANNING".equals(plan.phase)
                || FactionStrategicAssetAuthority.handles(plan)) return;
        NpcFactionSite site = game.siteForFaction(plan.faction, game.world.zoneType);
        if (!localSite(site, game.world)) return;

        int salvageRoom = capturedAssetRoom(game, site);
        if (salvageRoom >= 0) {
            plan.immediateGoal = FactionStrategicAssetAuthority.CAPTURED_ASSET_SALVAGE_GOAL;
            plan.targetRoom = RoomOwnershipAuthority.roomName(game.world, salvageRoom);
            plan.targetItem = "Machine part";
            plan.addHistory(game.turn,
                    "Planning redirected to physical salvage because " + plan.targetRoom
                            + " contains captured foreign machinery with assigned workers.");
            return;
        }

        int specialistRoom = facilityNeedingSpecialistRoom(game, site);
        if (specialistRoom >= 0) {
            plan.immediateGoal = FactionStrategicAssetAuthority.FACILITY_SPECIALIST_GOAL;
            plan.targetRoom = RoomOwnershipAuthority.roomName(game.world, specialistRoom);
            plan.targetItem = "Tool bundle";
            plan.addHistory(game.turn,
                    "Planning redirected to specialist deployment because "
                            + plan.targetRoom
                            + " has an operational staffed facility without a visible specialist.");
        }
    }

    private static int capturedAssetRoom(GamePanel game, NpcFactionSite site) {
        if (game == null || game.world == null || game.baseObjects == null) return -1;
        int best = Integer.MAX_VALUE;
        for (BaseObject object : game.baseObjects) {
            if (object == null || object.underConstruction
                    || !MachineTierAuthority.isMachineOrFacilitySymbol(object.symbol)) continue;
            int roomId = game.world.roomIdAt(object.x, object.y);
            if (roomId < 0 || roomId >= best
                    || !FactionIdentityAuthority.sameFamily(
                    game.world.roomFaction(roomId), site.faction)
                    || assignedWorkers(game.world, roomId, site.faction) <= 0) continue;
            Faction custodian = object.faction == null ? Faction.NONE : object.faction;
            if (custodian == Faction.HIVER
                    || FactionIdentityAuthority.sameFamily(custodian, site.faction)) continue;
            best = roomId;
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    private static int facilityNeedingSpecialistRoom(GamePanel game,
                                                     NpcFactionSite site) {
        if (game == null || game.world == null || game.baseObjects == null) return -1;
        int best = Integer.MAX_VALUE;
        for (BaseObject object : game.baseObjects) {
            if (object == null || object.underConstruction
                    || !MachineTierAuthority.isMachineOrFacilitySymbol(object.symbol)) continue;
            int roomId = game.world.roomIdAt(object.x, object.y);
            if (roomId < 0 || roomId >= best
                    || !FactionIdentityAuthority.sameFamily(
                    game.world.roomFaction(roomId), site.faction)
                    || assignedWorkers(game.world, roomId, site.faction) <= 0
                    || hasSpecialist(game.world, roomId, site.faction)) continue;
            Faction custodian = object.faction == null ? Faction.NONE : object.faction;
            if (custodian != Faction.NONE
                    && !FactionIdentityAuthority.sameFamily(custodian, site.faction)) continue;
            best = roomId;
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    private static boolean hasSpecialist(World world, int roomId, Faction faction) {
        if (world == null || world.npcs == null) return false;
        for (NpcEntity npc : world.npcs) {
            if (npc == null || npc.id == null
                    || !npc.id.startsWith(SPECIALIST_ID_PREFIX)) continue;
            if (world.roomIdAt(npc.x, npc.y) == roomId
                    && FactionIdentityAuthority.sameFamily(npc.faction, faction)) return true;
        }
        return false;
    }

    private static int assignedWorkers(World world, int roomId, Faction faction) {
        if (world == null || world.roomPopulationLedgers == null) return 0;
        long assigned = 0;
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (ledger == null || ledger.roomId != roomId
                    || !FactionIdentityAuthority.sameFamily(ledger.faction, faction)) continue;
            assigned += Math.max(0, ledger.assigned);
        }
        return (int)Math.min(Integer.MAX_VALUE, assigned);
    }

    private static boolean localSite(NpcFactionSite site, World world) {
        return site != null && world != null
                && site.sectorX == world.sectorX && site.sectorY == world.sectorY
                && site.zoneX == world.zoneX && site.zoneY == world.zoneY
                && site.floor == world.floor;
    }
}
