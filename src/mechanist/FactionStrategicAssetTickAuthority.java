package mechanist;

/**
 * Resolves physical strategic-asset plans before the legacy abstract faction
 * strategy pass. Completed attempts advance directly into cooldown so the same
 * plan cannot also receive a scripted success or failure in the later pass.
 */
final class FactionStrategicAssetTickAuthority {
    private FactionStrategicAssetTickAuthority() { }

    static int tick(GamePanel game) {
        if (game == null || game.world == null || game.factionStrategicPlans == null) return 0;
        int resolved = 0;
        for (FactionStrategicPlan plan : game.factionStrategicPlans) {
            if (plan == null || !FactionStrategicAssetAuthority.handles(plan)
                    || !"EXECUTION".equals(plan.phase)
                    || game.turn < plan.phaseUntilTurn) continue;

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
}
