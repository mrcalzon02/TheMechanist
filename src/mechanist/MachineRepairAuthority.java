package mechanist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Bounded player and faction repair workflows for physical production machines. */
final class MachineRepairAuthority {
    static final int SERVICEABLE_INTEGRITY = 3;
    static final int PART_COST = 1;
    static final int RESTORE_AMOUNT = 2;
    static final int TRAINED_RESTORE_AMOUNT = 3;
    static final String FACTION_REPAIR_GOAL = "repair a damaged facility";
    private static final String FACTION_REPAIR_PREFIX = "FACTION_FACILITY_REPAIR";

    record RepairPreview(boolean available, int partCost, int currentIntegrity, int projectedIntegrity, String summary) { }

    record FactionRepairStage(boolean handled, boolean success, String blocker, String message,
                              BaseObject facility, int roomId, int workers,
                              int stockBefore, int stockAfter, int partCost,
                              int integrityBefore, int projectedIntegrity,
                              boolean replacement) {
        static FactionRepairStage notHandled(String message) {
            return new FactionRepairStage(false, false, "not-handled", safe(message),
                    null, -1, 0, 0, 0, 0, 0, 0, false);
        }

        static FactionRepairStage blocked(String blocker, String message, NpcFactionSite site,
                                          BaseObject facility, int roomId, int workers,
                                          int partCost, int integrityBefore,
                                          int projectedIntegrity, boolean replacement) {
            int stock = site == null ? 0 : Math.max(0, site.stock);
            return new FactionRepairStage(true, false, safe(blocker), safe(message),
                    facility, roomId, Math.max(0, workers), stock, stock,
                    Math.max(0, partCost), Math.max(0, integrityBefore),
                    Math.max(0, projectedIntegrity), replacement);
        }
    }

    record FactionRepairCompletion(boolean completed, int integrityBefore,
                                   int integrityAfter, int partCost,
                                   boolean replacement, String message) {
        static FactionRepairCompletion unavailable(String message) {
            return new FactionRepairCompletion(false, 0, 0, 0, false, safe(message));
        }
    }

    private record RepairOrder(int targetIntegrity, int integrityBefore, int partCost,
                               boolean replacement, String originalRecipe) { }

    private MachineRepairAuthority() { }

    static RepairPreview preview(BaseObject machine, int availableParts) {
        return preview(null, machine, availableParts);
    }

    static RepairPreview preview(GamePanel game, BaseObject machine, int availableParts) {
        if (machine == null) return new RepairPreview(false, PART_COST, 0, 0, "Repair unavailable: no owned machine selected.");
        if (FactionPhysicalConstructionAuthority.isFactionManaged(machine)) {
            int current = Math.max(0, machine.integrity);
            return new RepairPreview(false, 0, current, current,
                    "Repair unavailable: this faction facility retains its assigned maintenance roster.");
        }
        int current = Math.max(0, machine.integrity);
        if (current >= SERVICEABLE_INTEGRITY) {
            return new RepairPreview(false, 0, current, current, "Repair unnecessary: machine condition is already serviceable.");
        }
        boolean trained = game != null && SkillTreeProgressionAuthority.hasCapability(
                game.unlockedSkillNodes, "fab-repair-forge-tutoring");
        int restore = trained ? TRAINED_RESTORE_AMOUNT : RESTORE_AMOUNT;
        int projected = Math.min(SERVICEABLE_INTEGRITY, current + restore);
        if (availableParts < PART_COST) {
            return new RepairPreview(false, PART_COST, current, projected,
                    "Repair unavailable: need 1 machine part; available " + Math.max(0, availableParts) + ".");
        }
        return new RepairPreview(true, PART_COST, current, projected,
                "Repair ready: spend 1 machine part and 1 turn; integrity " + current + " -> " + projected
                        + (trained ? " with Forge-Tutored Repair." : "."));
    }

    static List<String> detailLines(BaseObject machine, int availableParts) {
        return detailLines(null, machine, availableParts);
    }

    static List<String> detailLines(GamePanel game, BaseObject machine, int availableParts) {
        RepairPreview preview = preview(game, machine, availableParts);
        boolean trained = game != null && SkillTreeProgressionAuthority.hasCapability(
                game.unlockedSkillNodes, "fab-repair-forge-tutoring");
        return List.of(preview.summary(),
                "Field repair restores at most " + (trained ? TRAINED_RESTORE_AMOUNT : RESTORE_AMOUNT)
                        + " integrity and stops at serviceable integrity "
                        + SERVICEABLE_INTEGRITY + "; it does not rebuild original maximum integrity.");
    }

    static boolean handlesFactionRepair(FactionStrategicPlan plan) {
        return plan != null && FACTION_REPAIR_GOAL.equalsIgnoreCase(
                plan.immediateGoal == null ? "" : plan.immediateGoal.trim());
    }

    static boolean isFactionRepairSite(BaseObject facility) {
        return facility != null && facility.assignedRecipe != null
                && facility.assignedRecipe.startsWith(FACTION_REPAIR_PREFIX + "|");
    }

    /**
     * Reserves faction-site stock and converts the exact damaged facility into a
     * resumable staged maintenance job. The object identity, world position,
     * faction custody, and completed construction receipt remain unchanged.
     */
    static FactionRepairStage stageFactionRepair(GamePanel game, FactionStrategicPlan plan,
                                                  NpcFactionSite site) {
        if (!handlesFactionRepair(plan)) {
            String goal = plan == null || plan.immediateGoal == null ? "none" : plan.immediateGoal;
            return FactionRepairStage.notHandled(
                    "Faction repair authority does not handle strategic goal: " + goal + ".");
        }
        if (game == null || game.world == null) {
            return FactionRepairStage.blocked("no-world",
                    "Physical faction repair requires a loaded world.", site,
                    null, -1, 0, 0, 0, 0, false);
        }
        if (site == null) {
            return FactionRepairStage.blocked("no-site",
                    factionLabel(plan.faction) + " has no local production site to fund repairs.",
                    null, null, -1, 0, 0, 0, 0, false);
        }
        if (!FactionIdentityAuthority.sameFamily(plan.faction, site.faction)) {
            return FactionRepairStage.blocked("wrong-faction-site",
                    factionLabel(plan.faction) + " cannot repair a facility held by "
                            + factionLabel(site.faction) + ".", site,
                    null, -1, 0, 0, 0, 0, false);
        }
        if (!sameLocation(site, game.world)) {
            return FactionRepairStage.blocked("site-not-local",
                    siteLabel(site) + " is not in the loaded zone.", site,
                    null, -1, 0, 0, 0, 0, false);
        }

        BaseObject active = activeFactionRepair(game, site);
        if (active != null) {
            RepairOrder order = repairOrder(active);
            int roomId = game.world.roomIdAt(active.x, active.y);
            int workers = assignedWorkers(game.world, roomId, site);
            int stock = Math.max(0, site.stock);
            return new FactionRepairStage(true, true, "",
                    "IN PROGRESS: " + cleanFacilityName(active.name)
                            + " remains under faction repair at " + active.x + "," + active.y
                            + "; no additional site stock or player resources were spent.",
                    active, roomId, workers, stock, stock,
                    order == null ? 0 : order.partCost(),
                    order == null ? Math.max(0, active.integrity) : order.integrityBefore(),
                    order == null ? SERVICEABLE_INTEGRITY : order.targetIntegrity(),
                    order != null && order.replacement());
        }

        BaseObject facility = damagedFacility(game, site);
        if (facility == null) {
            return FactionRepairStage.blocked("no-damaged-facility",
                    siteLabel(site) + " has no damaged completed physical machine requiring repair.",
                    site, null, -1, 0, 0, 0, 0, false);
        }
        int roomId = game.world.roomIdAt(facility.x, facility.y);
        int workers = assignedWorkers(game.world, roomId, site);
        if (workers <= 0) {
            return FactionRepairStage.blocked("facility-unstaffed",
                    RoomOwnershipAuthority.roomName(game.world, roomId)
                            + " has no assigned same-family workers available for maintenance.",
                    site, facility, roomId, 0, 0,
                    Math.max(0, facility.integrity), SERVICEABLE_INTEGRITY,
                    facility.integrity <= 0);
        }

        int beforeIntegrity = Math.max(0, facility.integrity);
        boolean replacement = beforeIntegrity <= 0;
        int missing = Math.max(1, SERVICEABLE_INTEGRITY - beforeIntegrity);
        int partCost = missing + (replacement ? 1 : 0);
        if (site.stock < partCost) {
            return FactionRepairStage.blocked("insufficient-site-stock",
                    siteLabel(site) + " needs " + partCost + " maintenance stock to "
                            + (replacement ? "replace" : "repair") + " "
                            + cleanFacilityName(facility.name) + ", but has " + site.stock + ".",
                    site, facility, roomId, workers, partCost,
                    beforeIntegrity, SERVICEABLE_INTEGRITY, replacement);
        }

        int stockBefore = site.stock;
        String originalName = cleanFacilityName(facility.name);
        String originalRecipe = facility.assignedRecipe == null ? "" : facility.assignedRecipe;
        char originalSymbol = facility.symbol == 0 ? '?' : facility.symbol;
        site.stock = stockBefore - partCost;
        facility.underConstruction = true;
        facility.finalSymbol = originalSymbol;
        facility.symbol = '?';
        facility.name = "Under construction: " + originalName;
        facility.constructionRequiredItems = "Machine part=" + partCost;
        facility.constructionInsertedItems = facility.constructionRequiredItems;
        facility.constructionLaborRequired = Math.max(2, partCost * 2);
        facility.constructionLaborDone = 0;
        facility.constructionVisualProgress = 65;
        facility.assignedRecipe = encodeRepairOrder(SERVICEABLE_INTEGRITY,
                beforeIntegrity, partCost, replacement, originalRecipe);
        facility.constructionMaterialSource = siteLabel(site)
                + " reserved faction maintenance stock";
        facility.constructionPlanSource = "Faction maintenance order "
                + safe(plan.id) + (replacement ? " / replacement authorization" : " / repair authorization");
        facility.constructionLinkedPlanId = safe(plan.id);
        facility.assignedWorker = maintenanceCrew(site, game.world, roomId, workers);
        facility.description = "Faction-managed " + (replacement ? "replacement" : "repair")
                + " work for " + originalName + ". " + partCost
                + " maintenance stock is prepaid from " + siteLabel(site)
                + "; " + workers + " assigned room worker(s) provide labor; player parts, tools, labor, XP, and turns are not used.";
        ProgressiveConstructionAuthority.syncSiteTile(game, facility);

        return new FactionRepairStage(true, true, "",
                "Physical faction " + (replacement ? "replacement" : "repair")
                        + " staged for " + originalName + " in "
                        + RoomOwnershipAuthority.roomName(game.world, roomId)
                        + " at " + facility.x + "," + facility.y
                        + "; maintenance stock " + stockBefore + " -> " + site.stock
                        + ", labor 0/" + facility.constructionLaborRequired
                        + " from " + workers + " assigned worker(s).",
                facility, roomId, workers, stockBefore, site.stock,
                partCost, beforeIntegrity, SERVICEABLE_INTEGRITY, replacement);
    }

    /** Restores the original machine role after generic staged completion. */
    static FactionRepairCompletion completeFactionRepair(BaseObject facility, int turn,
                                                          String crewLabel) {
        RepairOrder order = repairOrder(facility);
        if (facility == null || order == null || facility.underConstruction) {
            return FactionRepairCompletion.unavailable(
                    "No completed faction repair work order was available.");
        }
        facility.integrity = Math.max(0, order.targetIntegrity());
        facility.assignedRecipe = order.originalRecipe();
        facility.machineRepairHistory = MachineRepairHistoryAuthority.recordFactionLine(
                facility, order.integrityBefore(), facility.integrity,
                order.partCost(), turn, crewLabel, order.replacement());
        String action = order.replacement() ? "replacement" : "repair";
        facility.description = append(facility.description,
                "Faction " + action + " completed at turn " + Math.max(0, turn)
                        + " using " + order.partCost() + " maintenance stock by "
                        + safe(crewLabel) + ".");
        return new FactionRepairCompletion(true, order.integrityBefore(), facility.integrity,
                order.partCost(), order.replacement(),
                "Faction facility " + action + " complete: "
                        + cleanFacilityName(facility.name) + " integrity "
                        + order.integrityBefore() + " -> " + facility.integrity
                        + " using " + order.partCost() + " prepaid maintenance stock.");
    }

    private static BaseObject activeFactionRepair(GamePanel game, NpcFactionSite site) {
        if (game == null || game.baseObjects == null || site == null) return null;
        for (BaseObject facility : game.baseObjects) {
            if (facility != null && facility.underConstruction
                    && isFactionRepairSite(facility)
                    && FactionPhysicalConstructionAuthority.belongsToSite(facility, site)) return facility;
        }
        return null;
    }

    private static BaseObject damagedFacility(GamePanel game, NpcFactionSite site) {
        if (game == null || game.world == null || game.baseObjects == null || site == null) return null;
        ArrayList<BaseObject> candidates = new ArrayList<>();
        for (BaseObject facility : game.baseObjects) {
            if (facility == null || facility.underConstruction
                    || !FactionPhysicalConstructionAuthority.isFactionManaged(facility)
                    || !FactionPhysicalConstructionAuthority.belongsToSite(facility, site)
                    || !MachineTierAuthority.isMachineOrFacilitySymbol(facility.symbol)
                    || Math.max(0, facility.integrity) >= SERVICEABLE_INTEGRITY) continue;
            int roomId = game.world.roomIdAt(facility.x, facility.y);
            if (roomId < 0 || !FactionIdentityAuthority.sameFamily(
                    game.world.roomFaction(roomId), site.faction)) continue;
            candidates.add(facility);
        }
        candidates.sort(Comparator.comparingInt((BaseObject facility) -> Math.max(0, facility.integrity))
                .thenComparingInt(facility -> facility.y)
                .thenComparingInt(facility -> facility.x)
                .thenComparing(facility -> cleanFacilityName(facility.name)));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private static int assignedWorkers(World world, int roomId, NpcFactionSite site) {
        if (world == null || site == null || roomId < 0) return 0;
        if (world.roomPopulationLedgers == null || world.roomPopulationLedgers.isEmpty()) {
            return Math.max(0, FactionSiteWorkforceAuthority.evaluate(site, world).effectiveWorkers());
        }
        long assigned = 0;
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (ledger == null || ledger.roomId != roomId
                    || !FactionIdentityAuthority.sameFamily(ledger.faction, site.faction)) continue;
            assigned += Math.max(0, ledger.assigned);
        }
        return (int)Math.min(Integer.MAX_VALUE, assigned);
    }

    private static String maintenanceCrew(NpcFactionSite site, World world,
                                          int roomId, int workers) {
        return ("Faction maintenance crew: " + siteLabel(site) + "; "
                + Math.max(0, workers) + " assigned worker(s) from the "
                + RoomOwnershipAuthority.roomName(world, roomId) + " roster").replace('|', '/');
    }

    private static String encodeRepairOrder(int targetIntegrity, int integrityBefore,
                                            int partCost, boolean replacement,
                                            String originalRecipe) {
        return FACTION_REPAIR_PREFIX + "|" + Math.max(0, targetIntegrity)
                + "|" + Math.max(0, integrityBefore) + "|" + Math.max(0, partCost)
                + "|" + replacement + "|" + safe(originalRecipe).replace('|', '/');
    }

    private static RepairOrder repairOrder(BaseObject facility) {
        if (!isFactionRepairSite(facility)) return null;
        String[] fields = facility.assignedRecipe.split("\\|", 6);
        if (fields.length < 6) return null;
        try {
            return new RepairOrder(Math.max(0, Integer.parseInt(fields[1])),
                    Math.max(0, Integer.parseInt(fields[2])),
                    Math.max(0, Integer.parseInt(fields[3])),
                    Boolean.parseBoolean(fields[4]), fields[5]);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static boolean sameLocation(NpcFactionSite site, World world) {
        return site != null && world != null
                && site.sectorX == world.sectorX && site.sectorY == world.sectorY
                && site.zoneX == world.zoneX && site.zoneY == world.zoneY
                && site.floor == world.floor;
    }

    private static String cleanFacilityName(String value) {
        String name = safe(value).replaceFirst("^Under construction: ", "").trim();
        return name.isBlank() ? "faction facility" : name;
    }

    private static String factionLabel(Faction faction) {
        return faction == null ? Faction.NONE.label : faction.label;
    }

    private static String siteLabel(NpcFactionSite site) {
        return site == null || site.name == null || site.name.isBlank()
                ? "Unnamed faction site" : site.name.trim().replace('|', '/');
    }

    private static String append(String current, String line) {
        if (line == null || line.isBlank()) return safe(current);
        if (current == null || current.isBlank()) return line;
        return current.contains(line) ? current : current + " " + line;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
