package mechanist;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Turns the Mechanist factory-upgrade strategic action into a physical staged
 * construction site in the controlled world.
 *
 * <p>All world-placement checks happen before the blueprint upgrade authority
 * can acquire a plan or debit faction stock. The completed preflight therefore
 * makes a blocked placement mutation-free, while successful construction is
 * prepaid entirely from the linked faction site's stock.</p>
 */
final class FactionPhysicalConstructionAuthority {
    static final String FACTION_OWNER_MODE = "FACTION";
    private static final String CREW_PREFIX = "Faction construction crew: ";

    record Outcome(boolean handled, boolean success, String blocker, String message,
                   int roomId, String roomName, int tileX, int tileY,
                   int stockBefore, int stockAfter, int effectiveWorkers,
                   FactionFacilityBlueprintUpgradeAuthority.Outcome upgrade,
                   BaseObject constructionSite) {
        static Outcome notHandled(NpcFactionSite site, String message) {
            int stock = stock(site);
            return new Outcome(false, false, "not-handled", safe(message),
                    -1, "", -1, -1, stock, stock, workers(site, null), null, null);
        }

        static Outcome blocked(String blocker, String message, NpcFactionSite site,
                               int effectiveWorkers, RoomSelection room,
                               FactionFacilityBlueprintUpgradeAuthority.Outcome upgrade) {
            int stockBefore = upgrade == null ? stock(site) : upgrade.stockBefore();
            int stockAfter = upgrade == null ? stock(site) : upgrade.stockAfter();
            return new Outcome(true, false, safe(blocker), safe(message),
                    room == null ? -1 : room.roomId(),
                    room == null ? "" : room.roomName(),
                    room == null || room.tile() == null ? -1 : room.tile().x,
                    room == null || room.tile() == null ? -1 : room.tile().y,
                    stockBefore, stockAfter, Math.max(0, effectiveWorkers), upgrade, null);
        }

        private static int stock(NpcFactionSite site) {
            return site == null ? 0 : Math.max(0, site.stock);
        }

        private static int workers(NpcFactionSite site, World world) {
            return site == null ? 0
                    : Math.max(0, FactionSiteWorkforceAuthority.evaluate(site, world).effectiveWorkers());
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }
    }

    private record RoomSelection(int roomId, String roomName, Point tile,
                                 boolean controlledRoomFound, boolean roomWorkforceFound,
                                 int workers, boolean localRosterMode) { }

    private FactionPhysicalConstructionAuthority() { }

    /** Only the exact factory goal for the Mechanist strategic family is handled. */
    static boolean handles(FactionStrategicPlan plan) {
        return FactionFacilityBlueprintUpgradeAuthority.handles(plan)
                && mechanistFamily(plan.faction);
    }

    /**
     * Preflights and stages one prepaid EMM Micro Forge construction site.
     * Player supplies, parts, components, tools, XP, and craft totals are never
     * read or changed by this path.
     */
    static Outcome attempt(GamePanel game, FactionStrategicPlan plan, NpcFactionSite site) {
        if (!handles(plan)) {
            String goal = plan == null || plan.immediateGoal == null ? "none" : plan.immediateGoal;
            return Outcome.notHandled(site,
                    "Physical Mechanist construction does not handle strategic goal: " + goal + ".");
        }

        int effectiveWorkers = workforce(site, game == null ? null : game.world);
        if (game == null) {
            return Outcome.blocked("no-game",
                    "Physical faction construction requires a live simulation context.",
                    site, effectiveWorkers, null, null);
        }
        if (game.world == null) {
            return Outcome.blocked("no-world",
                    "Physical faction construction requires a loaded world.",
                    site, effectiveWorkers, null, null);
        }
        if (site == null) {
            return Outcome.blocked("no-site",
                    factionLabel(plan.faction) + " has no production site to link to this construction crew.",
                    null, 0, null, null);
        }
        if (!mechanistFamily(site.faction)
                || !FactionIdentityAuthority.sameFamily(plan.faction, site.faction)) {
            return Outcome.blocked("wrong-faction-site",
                    factionLabel(plan.faction)
                            + " requires a same-family Mechanist production site for physical construction.",
                    site, effectiveWorkers, null, null);
        }
        if (!sameLocation(site, game.world)) {
            return Outcome.blocked("site-not-local",
                    siteLabel(site) + " is at " + site.locationKey()
                            + "; load that site before placing its physical construction.",
                    site, effectiveWorkers, null, null);
        }

        BaseObject active = activeSite(game, site);
        if (active != null) {
            int activeRoomId = game.world.roomIdAt(active.x, active.y);
            String activeRoomName = activeRoomId < 0 ? "linked construction area"
                    : RoomOwnershipAuthority.roomName(game.world, activeRoomId);
            boolean localRosterMode = hasLocalPopulationLedgers(game.world);
            effectiveWorkers = localRosterMode
                    ? roomWorkers(game.world, activeRoomId, site.faction) : effectiveWorkers;
            int stock = Math.max(0, site.stock);
            return new Outcome(true, true, "",
                    "IN PROGRESS: " + siteLabel(site) + " resumes " + objectLabel(active)
                            + " in " + activeRoomName + " at " + active.x + "," + active.y
                            + "; no additional faction stock or player resources were spent.",
                    activeRoomId, activeRoomName, active.x, active.y, stock, stock,
                    effectiveWorkers, null, active);
        }

        BaseObject activeRoomShell = FactionRoomShellConstructionAuthority.activeSite(game, site);
        if (activeRoomShell != null) {
            return fromRoomShell(FactionRoomShellConstructionAuthority.attempt(game, plan, site));
        }

        RoomSelection room = selectRoomAndTile(game, plan.faction, effectiveWorkers);
        if (!room.controlledRoomFound()) {
            return Outcome.blocked("no-controlled-room",
                    siteLabel(site) + " has no same-family controlled, non-special room in this zone.",
                    site, effectiveWorkers, null, null);
        }
        if (!room.roomWorkforceFound()) {
            return Outcome.blocked("no-room-workforce",
                    siteLabel(site) + " has controlled rooms, but none has assigned same-family workers"
                            + " in its local population ledger.",
                    site, 0, room, null);
        }
        if (room.tile() == null) {
            FactionRoomShellConstructionAuthority.Outcome shell =
                    FactionRoomShellConstructionAuthority.attempt(game, plan, site);
            if (shell.handled()) {
                return fromRoomShell(shell);
            }
            return Outcome.blocked("no-legal-construction-tile",
                    siteLabel(site) + " has no legal empty interior construction tile in its controlled rooms.",
                    site, room.workers(), room, null);
        }
        effectiveWorkers = room.workers();

        // Placement is now proven. This is the first call allowed to mutate the
        // faction blueprint ledger or reserve faction stock. Facility levels
        // remain unchanged until the physical build produces its receipt.
        FactionFacilityBlueprintUpgradeAuthority.Outcome upgrade =
                FactionFacilityBlueprintUpgradeAuthority.reserveForPhysicalConstruction(game, plan, site);
        if (!upgrade.success()) {
            return Outcome.blocked(upgrade.blocker(), upgrade.message(), site,
                    effectiveWorkers, room, upgrade);
        }

        BuildRecipe recipe = BuildRecipe.microForge();
        BaseObject construction = ProgressiveConstructionAuthority.createPrepaidSite(
                recipe, room.tile().x, room.tile().y);
        String linkedSite = linkedSiteName(site);
        String jobKey = jobKey(site);
        String crew = crewLabel(site, room.roomName(), effectiveWorkers, room.localRosterMode());
        construction.faction = site.faction;
        construction.assignedWorker = crew;
        construction.constructionOwnerMode = FACTION_OWNER_MODE;
        construction.constructionMaterialSource = linkedSite + " reserved faction-site stock";
        construction.constructionPlanSource = planSource(upgrade);
        construction.constructionLinkedSiteName = jobKey;
        construction.constructionLinkedPlanId = plan.id == null ? "" : plan.id;
        construction.description = "Faction-managed construction for " + linkedSite
                + ". Plan: " + upgrade.blueprintName() + " via " + acquisitionSource(upgrade)
                + ". Workforce: " + effectiveWorkers + " effective worker(s), assigned as " + crew
                + ". Materials are prepaid from faction-site stock; player resources are not used.";

        game.baseObjects.add(construction);
        ProgressiveConstructionAuthority.syncSiteTile(game, construction);

        String message = upgrade.message() + " Physical construction staged in " + room.roomName()
                + " at " + construction.x + "," + construction.y + " for " + crew
                + "; workforce " + effectiveWorkers + ", labor 0/"
                + construction.constructionLaborRequired + ".";
        return new Outcome(true, true, "", message, room.roomId(), room.roomName(),
                construction.x, construction.y, upgrade.stockBefore(), upgrade.stockAfter(),
                effectiveWorkers, upgrade, construction);
    }

    /** True only for construction explicitly owned by faction simulation. */
    static boolean isFactionManaged(BaseObject object) {
        return object != null && FACTION_OWNER_MODE.equalsIgnoreCase(
                object.constructionOwnerMode == null ? "" : object.constructionOwnerMode.trim());
    }

    /** Stable human custody readback shared by construction and machine controls. */
    static String crewReadback(BaseObject object) {
        if (!isFactionManaged(object)) return "No faction construction crew is assigned";
        String crew = object.assignedWorker == null ? "" : object.assignedWorker.trim();
        if (!crew.isBlank()) return crew;
        String source = object.constructionMaterialSource == null
                ? "" : object.constructionMaterialSource.trim();
        return source.isBlank() ? "Assigned faction construction crew"
                : "Assigned faction construction crew from " + source;
    }

    /** Tests the persisted faction-site link rather than transient object identity. */
    static boolean belongsToSite(BaseObject object, NpcFactionSite site) {
        if (!isFactionManaged(object) || site == null) return false;
        String linked = object.constructionLinkedSiteName == null
                ? "" : object.constructionLinkedSiteName.trim();
        return !linked.isBlank() && linked.equals(jobKey(site));
    }

    /** Returns the linked site's current unfinished construction, if any. */
    static BaseObject activeSite(GamePanel game, NpcFactionSite site) {
        if (game == null || site == null || game.baseObjects == null) return null;
        BaseObject best = null;
        for (BaseObject object : game.baseObjects) {
            if (object == null || !object.underConstruction || !belongsToSite(object, site)) continue;
            if (best == null || constructionKey(object).compareTo(constructionKey(best)) < 0) best = object;
        }
        return best;
    }

    /** Advances one hour of faction labor without touching the player crafting path. */
    static ProgressiveConstructionAuthority.FactionWorkResult advanceHourly(
            GamePanel game, NpcFactionSite site) {
        BaseObject construction = activeSite(game, site);
        int roomId = game == null || game.world == null || construction == null
                ? -1 : game.world.roomIdAt(construction.x, construction.y);
        boolean localRosterMode = game != null && hasLocalPopulationLedgers(game.world);
        int laborTurns = localRosterMode
                ? roomWorkers(game.world, roomId, site == null ? Faction.NONE : site.faction)
                : workforce(site, game == null ? null : game.world);
        String roomName = game == null || game.world == null || roomId < 0
                ? "linked construction room" : RoomOwnershipAuthority.roomName(game.world, roomId);
        String crew = crewLabel(site, roomName, laborTurns, localRosterMode);
        if (construction != null) construction.assignedWorker = crew;
        ProgressiveConstructionAuthority.FactionWorkResult work =
                ProgressiveConstructionAuthority.contributeFaction(
                game, construction, laborTurns, crew);
        if (!work.completed() || construction == null) return work;

        String receiptKey = construction.constructionLinkedSiteName == null
                ? "" : construction.constructionLinkedSiteName.trim();
        boolean applied = FactionFacilityBlueprintUpgradeAuthority
                .applyCompletedPhysicalUpgrade(site, receiptKey);
        if (!applied) {
            return new ProgressiveConstructionAuthority.FactionWorkResult(
                    work.advanced(), work.laborAdded(), true,
                    work.summary() + " The linked facility upgrade receipt was already applied or invalid.");
        }

        FactionStrategicPlan plan = linkedPlan(game, construction);
        if (plan != null) {
            plan.success++;
            plan.lastOutcome = "SUCCESS: " + siteLabel(site) + " completed physical "
                    + recipeLabel(construction) + " construction at " + construction.x + ","
                    + construction.y + "; the reserved factory upgrade is now operational.";
            plan.addHistory(game == null ? 0 : game.turn, plan.lastOutcome);
        }
        String planReadback = plan == null
                ? " No loaded strategic plan matched the persisted completion link."
                : " The linked strategic plan recorded one completed success.";
        FactionCriticalVendorPlacementAuthority.FacilityActivation vendorActivation =
                FactionCriticalVendorPlacementAuthority.activateCompletedFacility(
                        game, site, construction);
        String vendorReadback = vendorActivation.handled()
                ? " " + vendorActivation.message() : "";
        return new ProgressiveConstructionAuthority.FactionWorkResult(
                work.advanced(), work.laborAdded(), true,
                work.summary() + " Linked faction facility levels advanced once."
                        + planReadback + vendorReadback);
    }

    private static RoomSelection selectRoomAndTile(GamePanel game, Faction faction,
                                                   int fallbackWorkers) {
        World world = game.world;
        boolean localRosterMode = hasLocalPopulationLedgers(world);
        boolean controlledRoomFound = false;
        boolean roomWorkforceFound = !localRosterMode;
        int firstControlledRoom = -1;
        String firstControlledName = "";
        int firstWorkforceRoom = -1;
        String firstWorkforceName = "";
        int firstWorkforceWorkers = 0;
        for (int roomId = 0; roomId < world.rooms.size(); roomId++) {
            if (!FactionIdentityAuthority.sameFamily(world.roomFaction(roomId), faction)) continue;
            if (roomId < world.roomSpecials.size()
                    && Boolean.TRUE.equals(world.roomSpecials.get(roomId))) continue;
            Rectangle room = world.roomRect(roomId);
            if (room == null) continue;
            controlledRoomFound = true;
            if (firstControlledRoom < 0) {
                firstControlledRoom = roomId;
                firstControlledName = RoomOwnershipAuthority.roomName(world, roomId);
            }
            int workers = localRosterMode ? roomWorkers(world, roomId, faction) : fallbackWorkers;
            if (localRosterMode && workers <= 0) continue;
            roomWorkforceFound = true;
            if (firstWorkforceRoom < 0) {
                firstWorkforceRoom = roomId;
                firstWorkforceName = RoomOwnershipAuthority.roomName(world, roomId);
                firstWorkforceWorkers = workers;
            }
            Point tile = firstLegalInteriorTile(game, roomId, room);
            if (tile != null) {
                return new RoomSelection(roomId, RoomOwnershipAuthority.roomName(world, roomId),
                        tile, true, true, workers, localRosterMode);
            }
        }
        int reportedRoom = roomWorkforceFound && firstWorkforceRoom >= 0
                ? firstWorkforceRoom : firstControlledRoom;
        String reportedName = roomWorkforceFound && firstWorkforceRoom >= 0
                ? firstWorkforceName : firstControlledName;
        int reportedWorkers = roomWorkforceFound && firstWorkforceRoom >= 0
                ? firstWorkforceWorkers : 0;
        return new RoomSelection(reportedRoom, reportedName, null, controlledRoomFound,
                roomWorkforceFound, reportedWorkers, localRosterMode);
    }

    private static Point firstLegalInteriorTile(GamePanel game, int roomId, Rectangle room) {
        World world = game.world;
        int xStart = Math.max(0, room.x + 1);
        int xEnd = Math.min(world.w, room.x + room.width - 1);
        int yStart = Math.max(0, room.y + 1);
        int yEnd = Math.min(world.h, room.y + room.height - 1);
        for (int x = xStart; x < xEnd; x++) {
            for (int y = yStart; y < yEnd; y++) {
                if (world.roomIdAt(x, y) != roomId || !world.walkable(x, y)) continue;
                if (x == game.playerX && y == game.playerY) continue;
                if (world.npcAt(x, y) != null || world.mapObjectAt(x, y) != null) continue;
                if (game.baseObjectAt(x, y) != null || world.isDoorAccessReservedForObject(x, y)) continue;
                return new Point(x, y);
            }
        }
        return null;
    }

    private static boolean sameLocation(NpcFactionSite site, World world) {
        return site != null && world != null
                && site.sectorX == world.sectorX && site.sectorY == world.sectorY
                && site.zoneX == world.zoneX && site.zoneY == world.zoneY
                && site.floor == world.floor;
    }

    private static int workforce(NpcFactionSite site, World world) {
        if (site == null) return 0;
        return Math.max(0, FactionSiteWorkforceAuthority.evaluate(site, world).effectiveWorkers());
    }

    private static boolean hasLocalPopulationLedgers(World world) {
        return world != null && world.roomPopulationLedgers != null
                && !world.roomPopulationLedgers.isEmpty();
    }

    private static int roomWorkers(World world, int roomId, Faction faction) {
        if (world == null || roomId < 0 || world.roomPopulationLedgers == null) return 0;
        long assigned = 0;
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (ledger == null || ledger.roomId != roomId
                    || !FactionIdentityAuthority.sameFamily(ledger.faction, faction)) continue;
            assigned += Math.max(0, ledger.assigned);
        }
        return (int) Math.min(Integer.MAX_VALUE, assigned);
    }

    private static boolean mechanistFamily(Faction faction) {
        return FactionIdentityAuthority.sameFamily(faction, Faction.MECHANIST_COLLEGIA);
    }

    private static String linkedSiteName(NpcFactionSite site) {
        if (site == null) return "Unlinked faction site";
        String name = site.name == null ? "" : site.name.trim();
        return name.isBlank() ? "Unnamed faction site at " + site.locationKey() : name;
    }

    private static String crewLabel(NpcFactionSite site, String roomName, int workers,
                                    boolean localRosterMode) {
        String room = roomName == null || roomName.isBlank() ? "linked construction room" : roomName;
        String source = localRosterMode
                ? Math.max(0, workers) + " assigned worker(s) from the " + room + " roster"
                : Math.max(0, workers) + " site worker(s) under abstract roster accounting";
        // assignedWorker is an older unescaped save field, so keep the human
        // custody label delimiter-safe even when a modded site/room uses '|'.
        return (CREW_PREFIX + linkedSiteName(site) + "; " + source).replace('|', '/');
    }

    private static String jobKey(NpcFactionSite site) {
        String blueprintId = ConstructionBlueprintOwnershipAuthority.blueprintId(BuildRecipe.microForge());
        if (site == null) return "Unlinked faction site at unknown location blueprint " + blueprintId;
        // NpcFactionSite persists completed job receipts in a comma-delimited
        // ledger, so the key spells out coordinates without commas or pipes.
        return linkedSiteName(site).replace('|', '/').replace(',', '/')
                + " at sector " + site.sectorX + "-" + site.sectorY
                + " zone " + site.zoneX + "-" + site.zoneY + " floor " + site.floor
                + " blueprint " + blueprintId
                + " target levels " + Math.min(FactionFacilityBlueprintUpgradeAuthority.BASE_LEVEL_CAP, site.baseLevel + 1)
                + "-" + Math.min(FactionFacilityBlueprintUpgradeAuthority.MACHINE_LEVEL_CAP, site.machineLevel + 1);
    }

    private static String planSource(FactionFacilityBlueprintUpgradeAuthority.Outcome upgrade) {
        if (upgrade == null) return "unrecorded faction construction plan";
        return upgrade.blueprintName() + " licensed plan / " + acquisitionSource(upgrade);
    }

    private static String acquisitionSource(FactionFacilityBlueprintUpgradeAuthority.Outcome upgrade) {
        if (upgrade == null || upgrade.acquisitionSource() == null
                || upgrade.acquisitionSource().isBlank()) return "known faction plan";
        return upgrade.acquisitionSource();
    }

    private static FactionStrategicPlan linkedPlan(GamePanel game, BaseObject construction) {
        if (game == null || construction == null || game.factionStrategicPlans == null) return null;
        String planId = safePlanId(construction);
        if (planId.isBlank()) return null;
        for (FactionStrategicPlan plan : game.factionStrategicPlans) {
            if (plan != null && planId.equals(plan.id)) return plan;
        }
        return null;
    }

    private static String safePlanId(BaseObject construction) {
        return construction == null || construction.constructionLinkedPlanId == null
                ? "" : construction.constructionLinkedPlanId.trim();
    }

    private static String recipeLabel(BaseObject construction) {
        return construction == null || construction.assignedRecipe == null
                || construction.assignedRecipe.isBlank()
                ? "factory" : construction.assignedRecipe;
    }

    private static String factionLabel(Faction faction) {
        return faction == null ? Faction.NONE.label : faction.label;
    }

    private static String siteLabel(NpcFactionSite site) {
        return site == null ? "Unnamed faction site" : linkedSiteName(site);
    }

    private static String objectLabel(BaseObject object) {
        return object == null || object.name == null || object.name.isBlank()
                ? "unnamed construction" : object.name;
    }

    private static String constructionKey(BaseObject object) {
        if (object == null) return "~";
        return String.format("%08d|%08d|%s", object.x, object.y, objectLabel(object));
    }

    private static Outcome fromRoomShell(FactionRoomShellConstructionAuthority.Outcome shell) {
        if (shell == null) {
            return Outcome.notHandled(null, "No faction room-shell construction result was available.");
        }
        return new Outcome(shell.handled(), shell.success(), shell.blocker(), shell.message(),
                shell.sourceRoomId(), shell.sourceRoomName(), shell.originX(), shell.originY(),
                shell.stockBefore(), shell.stockAfter(), shell.workers(), null, shell.marker());
    }
}
