package mechanist;

import java.util.Map;

/**
 * Live Phase 17.2 acquisition path for Mechanist faction factory upgrades.
 *
 * <p>The authority only handles the exact strategic goal named by
 * {@link #FACTORY_UPGRADE_GOAL}. It evaluates every acquisition, workforce,
 * material, and level-cap blocker before changing the faction site's plan
 * ledger or construction state.</p>
 */
final class FactionFacilityBlueprintUpgradeAuthority {
    static final String FACTORY_UPGRADE_GOAL = "build or upgrade a factory";
    static final int BASE_LEVEL_CAP = 6;
    static final int MACHINE_LEVEL_CAP = 7;

    record Outcome(boolean handled, boolean success, String message, String blocker,
                   String blueprintId, String blueprintName, String acquisitionSource,
                   boolean acquiredBlueprint, boolean reusedKnownBlueprint,
                   int effectiveWorkers, int materialStockCost,
                   int stockBefore, int stockAfter,
                   int baseLevelBefore, int baseLevelAfter,
                   int machineLevelBefore, int machineLevelAfter) {
        static Outcome notHandled(String message) {
            return new Outcome(false, false, safe(message), "not-handled",
                    "", "", "", false, false, 0, 0,
                    0, 0, 0, 0, 0, 0);
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }
    }

    private FactionFacilityBlueprintUpgradeAuthority() { }

    static boolean handles(FactionStrategicPlan plan) {
        return plan != null && FACTORY_UPGRADE_GOAL.equalsIgnoreCase(
                plan.immediateGoal == null ? "" : plan.immediateGoal.trim());
    }

    /**
     * Attempts one atomic faction factory upgrade.
     *
     * <p>The caller owns strategic-plan success/failure counters and history.
     * When {@link Outcome#handled()} is true, the caller can use
     * {@link Outcome#success()} and prefix {@link Outcome#message()} with its
     * ordinary SUCCESS/FAILURE marker.</p>
     */
    static Outcome attempt(GamePanel game, FactionStrategicPlan plan, NpcFactionSite site) {
        return attemptInternal(game, plan, site, true);
    }

    /** Reserves the known plan and exact site stock while physical labor remains pending. */
    static Outcome reserveForPhysicalConstruction(GamePanel game, FactionStrategicPlan plan,
                                                  NpcFactionSite site) {
        return attemptInternal(game, plan, site, false);
    }

    private static Outcome attemptInternal(GamePanel game, FactionStrategicPlan plan,
                                           NpcFactionSite site, boolean applyLevels) {
        if (!handles(plan)) {
            String goal = plan == null || plan.immediateGoal == null ? "none" : plan.immediateGoal;
            return Outcome.notHandled("Factory blueprint upgrade authority does not handle goal: " + goal + ".");
        }

        BuildRecipe recipe = BuildRecipe.microForge();
        String blueprintId = ConstructionBlueprintOwnershipAuthority.blueprintId(recipe);
        String blueprintName = recipe.name;
        int materialCost = aggregateStockCost(recipe);
        SiteSnapshot before = SiteSnapshot.of(site);

        if (game == null) {
            return blocked("no-game", "The factory plan cannot resolve without a live faction simulation context.",
                    blueprintId, blueprintName, "", false, false, 0, materialCost, before);
        }
        if (site == null) {
            return blocked("no-site", plan.faction.label
                            + " has no faction production site available for the factory upgrade.",
                    blueprintId, blueprintName, "", false, false, 0, materialCost, before);
        }
        if (!mechanistFamily(plan.faction) || !mechanistFamily(site.faction)
                || !FactionIdentityAuthority.sameFamily(plan.faction, site.faction)) {
            return blocked("wrong-faction-site", factionLabel(plan.faction)
                            + " requires a Mechanist/Mechanicus-family production site to use "
                            + blueprintName + ".",
                    blueprintId, blueprintName, "", false, false, 0, materialCost, before);
        }

        boolean known = site.knowsConstructionBlueprint(blueprintId);
        NpcEntity vendor = known ? null : acquisitionVendor(game, recipe);
        String acquisitionSource = known ? "known faction plan" : vendorLabel(vendor);
        if (!known && vendor == null) {
            String issuer = ConstructionBlueprintOwnershipAuthority.issuingFactionFor(recipe).label;
            String role = ConstructionBlueprintOwnershipAuthority.vendorRoleFor(recipe);
            return blocked("missing-blueprint-vendor", siteLabel(site) + " cannot requisition "
                            + ConstructionBlueprintOwnershipAuthority.blueprintItemName(recipe)
                            + "; no living " + issuer + " " + role + " is staffed in this zone.",
                    blueprintId, blueprintName, "", false, false, 0, materialCost, before);
        }

        FactionSiteWorkforceAuthority.Status workforce =
                FactionSiteWorkforceAuthority.evaluate(site, game.world);
        int effectiveWorkers = Math.max(0, workforce.effectiveWorkers());
        if (effectiveWorkers <= 0) {
            return blocked("no-workforce", siteLabel(site) + " cannot construct " + blueprintName
                            + ": no effective workers are assigned (" + workforce.reason() + ").",
                    blueprintId, blueprintName, acquisitionSource, false, known,
                    effectiveWorkers, materialCost, before);
        }
        if (site.stock < materialCost) {
            return blocked("insufficient-site-stock", siteLabel(site) + " cannot construct " + blueprintName
                            + ": requires " + materialCost + " site material stock, has " + site.stock + ".",
                    blueprintId, blueprintName, acquisitionSource, false, known,
                    effectiveWorkers, materialCost, before);
        }
        if (site.baseLevel >= BASE_LEVEL_CAP && site.machineLevel >= MACHINE_LEVEL_CAP) {
            return blocked("facility-level-cap", siteLabel(site) + " cannot upgrade further: base level "
                            + site.baseLevel + " and machine level " + site.machineLevel
                            + " are already at their supported caps.",
                    blueprintId, blueprintName, acquisitionSource, false, known,
                    effectiveWorkers, materialCost, before);
        }

        int nextBaseLevel = applyLevels && site.baseLevel < BASE_LEVEL_CAP ? site.baseLevel + 1 : site.baseLevel;
        int nextMachineLevel = applyLevels && site.machineLevel < MACHINE_LEVEL_CAP ? site.machineLevel + 1 : site.machineLevel;
        if (!known && !site.learnConstructionBlueprint(blueprintId)) {
            return blocked("blueprint-ledger-rejected", siteLabel(site) + " could not record the stable "
                            + blueprintName + " plan before construction; no resources were spent.",
                    blueprintId, blueprintName, acquisitionSource, false, false,
                    effectiveWorkers, materialCost, before);
        }

        site.stock = before.stock() - materialCost;
        site.baseLevel = nextBaseLevel;
        site.machineLevel = nextMachineLevel;
        SiteSnapshot after = SiteSnapshot.of(site);
        boolean acquired = !known;
        String acquisition = acquired
                ? "requisitioned " + blueprintName + " from " + acquisitionSource
                : "reused known faction plan " + blueprintName;
        String action = applyLevels ? "upgraded " : "reserved physical construction for ";
        String message = factionLabel(plan.faction) + " " + acquisition + " and " + action
                + siteLabel(site) + " with " + effectiveWorkers + " effective worker(s) and "
                + materialCost + " site material stock; stock " + before.stock() + " -> " + after.stock()
                + (applyLevels ? ", base level " + before.baseLevel() + " -> " + after.baseLevel()
                + ", machine level " + before.machineLevel() + " -> " + after.machineLevel() : "") + ".";
        return new Outcome(true, true, message, "", blueprintId, blueprintName, acquisitionSource,
                acquired, known, effectiveWorkers, materialCost,
                before.stock(), after.stock(), before.baseLevel(), after.baseLevel(),
                before.machineLevel(), after.machineLevel());
    }

    /** Applies the operational level gain exactly once after the linked physical site completes. */
    static boolean applyCompletedPhysicalUpgrade(NpcFactionSite site, String jobKey) {
        if (site == null || jobKey == null || jobKey.isBlank() || site.hasCompletedConstructionJob(jobKey)) {
            return false;
        }
        if (!site.recordCompletedConstructionJob(jobKey)) return false;
        if (site.baseLevel < BASE_LEVEL_CAP) site.baseLevel++;
        if (site.machineLevel < MACHINE_LEVEL_CAP) site.machineLevel++;
        return true;
    }

    static int aggregateStockCost(BuildRecipe recipe) {
        if (recipe == null) return 0;
        long total = Math.max(0, recipe.supplyCost) + (long) Math.max(0, recipe.partCost);
        if (recipe.componentCosts != null) {
            for (Map.Entry<String, Integer> entry : recipe.componentCosts.entrySet()) {
                Integer count = entry.getValue();
                if (count != null) total += Math.max(0, count);
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    private static Outcome blocked(String blocker, String message,
                                   String blueprintId, String blueprintName, String acquisitionSource,
                                   boolean acquired, boolean reused, int effectiveWorkers,
                                   int materialCost, SiteSnapshot site) {
        return new Outcome(true, false, message, blocker, blueprintId, blueprintName,
                acquisitionSource, acquired, reused, effectiveWorkers, materialCost,
                site.stock(), site.stock(), site.baseLevel(), site.baseLevel(),
                site.machineLevel(), site.machineLevel());
    }

    private static NpcEntity acquisitionVendor(GamePanel game, BuildRecipe recipe) {
        if (game == null || game.world == null || game.world.npcs == null || recipe == null) return null;
        FactionCriticalVendorPlacementAuthority.Category category =
                ConstructionBlueprintOwnershipAuthority.vendorCategoryFor(recipe);
        if (category != FactionCriticalVendorPlacementAuthority.Category.INDUSTRIAL) return null;
        NpcEntity best = null;
        for (NpcEntity npc : game.world.npcs) {
            if (!ConstructionBlueprintOwnershipAuthority.isLiveVendorFor(npc, recipe)) continue;
            if (best == null || vendorKey(npc).compareTo(vendorKey(best)) < 0) best = npc;
        }
        return best;
    }

    private static boolean mechanistFamily(Faction faction) {
        return FactionIdentityAuthority.sameFamily(faction, Faction.MECHANIST_COLLEGIA);
    }

    private static String factionLabel(Faction faction) {
        return faction == null ? Faction.NONE.label : faction.label;
    }

    private static String siteLabel(NpcFactionSite site) {
        return site == null || site.name == null || site.name.isBlank() ? "Unnamed faction site" : site.name;
    }

    private static String vendorLabel(NpcEntity vendor) {
        if (vendor == null) return "";
        String name = vendor.name == null || vendor.name.isBlank() ? "Unnamed works factor" : vendor.name;
        String role = vendor.role == null || vendor.role.isBlank()
                ? FactionCriticalVendorPlacementAuthority.Category.INDUSTRIAL.role : vendor.role;
        return name + " / " + role;
    }

    private static String vendorKey(NpcEntity vendor) {
        if (vendor == null) return "~";
        return (vendor.name == null ? "" : vendor.name) + "|"
                + (vendor.id == null ? "" : vendor.id) + "|" + vendor.x + "|" + vendor.y;
    }

    private record SiteSnapshot(int stock, int baseLevel, int machineLevel) {
        static SiteSnapshot of(NpcFactionSite site) {
            return site == null ? new SiteSnapshot(0, 0, 0)
                    : new SiteSnapshot(site.stock, site.baseLevel, site.machineLevel);
        }
    }
}
