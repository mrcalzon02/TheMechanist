package mechanist;

import java.awt.Point;
import java.awt.Rectangle;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the smallest physical room shell needed when a local Mechanist
 * factory plan has staffed controlled space but no legal Micro Forge tile.
 *
 * <p>The shell is a prepaid 5x4 Hollow Box with one door, one external
 * connector, and a second door cut into the source room. Staging reserves a
 * collision marker only; room lists, room ownership, room ids, and shell tiles
 * are changed together only after 35 exact room-local labor has accumulated
 * and completion preflight succeeds again.</p>
 */
final class FactionRoomShellConstructionAuthority {
    static final String ROOM_NAME = "Basic Forge Annex";
    static final String MARKER_RECIPE = "Basic Forge Annex room shell";
    static final int WIDTH = 5;
    static final int HEIGHT = 4;
    static final int MATERIAL_STOCK_COST = 49;
    static final int LABOR_REQUIRED = 35;

    private static final String BLUEPRINT_ID = "basic-forge-annex";
    private static final String RECEIPT_PREFIX = "receipt=v1:";
    private static final char MARKER_SYMBOL = '?';
    private static final char COMPLETED_MARKER_SYMBOL = 'N';

    record RoomShellSpec(
            int expectedRoomId,
            int sourceRoomId,
            String sourceRoomName,
            int originX,
            int originY,
            int width,
            int height,
            int sourceDoorX,
            int sourceDoorY,
            int connectorX,
            int connectorY,
            int annexDoorX,
            int annexDoorY,
            String annexDoorDirection) {

        RoomShellSpec {
            sourceRoomName = safe(sourceRoomName);
            annexDoorDirection = safe(annexDoorDirection).toUpperCase(Locale.ROOT);
        }

        Rectangle footprint() {
            return new Rectangle(originX, originY, width, height);
        }

        Point markerTile() {
            return new Point(originX + 1, originY + 1);
        }

        String readback() {
            return ROOM_NAME + " 5x4 footprint at " + originX + "," + originY
                    + "; source " + sourceRoomName + " (room " + sourceRoomId + ")"
                    + "; source door " + sourceDoorX + "," + sourceDoorY
                    + "; connector " + connectorX + "," + connectorY
                    + "; annex door " + annexDoorX + "," + annexDoorY
                    + " facing " + annexDoorDirection + "; expected room id "
                    + expectedRoomId + ".";
        }
    }

    record Preflight(
            boolean handled,
            boolean canStage,
            boolean resume,
            String blocker,
            String message,
            int sourceRoomId,
            String sourceRoomName,
            int originX,
            int originY,
            int stockBefore,
            int workers,
            int materialStockCost,
            int laborRequired,
            BaseObject marker,
            RoomShellSpec spec,
            BlueprintConstructionAuthority.ValidationResult blueprintValidation) {
    }

    record Outcome(
            boolean handled,
            boolean success,
            String blocker,
            String message,
            int sourceRoomId,
            String sourceRoomName,
            int originX,
            int originY,
            int stockBefore,
            int stockAfter,
            int workers,
            int materialStockCost,
            int laborRequired,
            BaseObject marker,
            RoomShellSpec spec) {
    }

    private record StaffedRoom(int roomId, String roomName, Rectangle room, int workers) {
    }

    private record Candidate(RoomShellSpec spec,
                             BlueprintConstructionAuthority.ValidationResult validation) {
    }

    private record CompletionPreflight(boolean canComplete, String blocker, String message,
                                       int workers, RoomShellSpec spec) {
    }

    private record StampResult(boolean success, boolean alreadyStamped, int roomId,
                               int transferredWorkers, String blocker, String message) {
    }

    private enum Side {
        SOUTH("N"), NORTH("S"), EAST("W"), WEST("E");

        final String annexDoorDirection;

        Side(String annexDoorDirection) {
            this.annexDoorDirection = annexDoorDirection;
        }
    }

    private FactionRoomShellConstructionAuthority() {
    }

    static boolean handles(FactionStrategicPlan plan) {
        return FactionFacilityBlueprintUpgradeAuthority.handles(plan)
                && mechanistFamily(plan == null ? Faction.NONE : plan.faction);
    }

    static BlueprintConstructionAuthority.RoomBlueprint basicForgeAnnexBlueprint() {
        return blueprintFor("N");
    }

    static int materialStockCost() {
        return MATERIAL_STOCK_COST;
    }

    static int laborRequired() {
        return LABOR_REQUIRED;
    }

    /**
     * Determines whether the room-shell fallback owns the current factory
     * action and produces its deterministic placement without mutating state.
     */
    static Preflight preflight(GamePanel game, FactionStrategicPlan plan, NpcFactionSite site) {
        int stock = stock(site);
        if (!handles(plan)) {
            String goal = plan == null || plan.immediateGoal == null ? "none" : plan.immediateGoal;
            return preflight(false, false, false, "not-handled",
                    "Basic Forge Annex construction does not handle strategic goal: " + goal + ".",
                    stock, 0, null, null, null);
        }
        if (game == null) {
            return preflight(true, false, false, "no-game",
                    "Basic Forge Annex construction requires a live simulation context.",
                    stock, 0, null, null, null);
        }
        if (game.world == null) {
            return preflight(true, false, false, "no-world",
                    "Basic Forge Annex construction requires a loaded world.",
                    stock, 0, null, null, null);
        }
        if (site == null) {
            return preflight(true, false, false, "no-site",
                    factionLabel(plan.faction) + " has no production site to pay for the annex shell.",
                    0, 0, null, null, null);
        }
        if (!mechanistFamily(site.faction)
                || !FactionIdentityAuthority.sameFamily(plan.faction, site.faction)) {
            return preflight(true, false, false, "wrong-faction-site",
                    factionLabel(plan.faction)
                            + " requires a same-family Mechanist production site for its forge annex.",
                    stock, 0, null, null, null);
        }
        if (!sameLocation(site, game.world)) {
            return preflight(true, false, false, "site-not-local",
                    siteLabel(site) + " is at " + site.locationKey()
                            + "; load that site before staging its forge annex.",
                    stock, 0, null, null, null);
        }

        BaseObject active = activeSite(game, site);
        if (active != null) {
            RoomShellSpec spec = completedRoomSpec(active);
            int workers = spec == null ? 0
                    : roomWorkers(game.world, spec.sourceRoomId(), site.faction);
            String message = "IN PROGRESS: " + siteLabel(site) + " resumes " + MARKER_RECIPE
                    + (spec == null ? "" : " at " + spec.originX() + "," + spec.originY())
                    + "; no additional faction stock or player resources were spent."
                    + (spec == null ? "" : " Labor " + active.constructionLaborDone + "/"
                    + active.constructionLaborRequired + " from " + workers
                    + " assigned source-room worker(s).");
            return preflight(true, true, true, "", message, stock, workers,
                    spec, active, null);
        }

        ArrayList<StaffedRoom> staffedRooms = staffedControlledRooms(game.world, plan.faction);
        if (staffedRooms.isEmpty()) {
            return preflight(true, false, false, "no-controlled-staffed-room",
                    siteLabel(site) + " has no same-family controlled, non-special room with"
                            + " assigned local workers for a forge annex crew.",
                    stock, 0, null, null, null);
        }
        for (StaffedRoom room : staffedRooms) {
            if (hasLegalMicroForgeTile(game, room.roomId(), room.room())) {
                return preflight(false, false, false, "interior-tile-available",
                        room.roomName() + " already has a legal interior tile for the EMM Micro Forge;"
                                + " no room shell is needed.",
                        stock, room.workers(), null, null, null);
            }
        }

        StaffedRoom first = staffedRooms.get(0);
        Candidate candidate = null;
        for (StaffedRoom room : staffedRooms) {
            candidate = firstCandidate(game, room);
            if (candidate != null) {
                first = room;
                break;
            }
        }
        if (candidate == null) {
            return preflight(true, false, false, "no-legal-annex-footprint",
                    siteLabel(site) + " needs a forge annex, but no clear external 5x4 footprint"
                            + " with a source door and one-cell connector is available.",
                    stock, first.workers(), specForRoomOnly(game.world, first), null, null);
        }

        if (game.world.rooms.size() != candidate.spec().expectedRoomId()) {
            return preflight(true, false, false, "room-index-drift",
                    "Forge annex placement expected room id " + candidate.spec().expectedRoomId()
                            + " but the live room list now ends at " + game.world.rooms.size()
                            + "; placement was not reserved.",
                    stock, first.workers(), candidate.spec(), null, candidate.validation());
        }
        if (stock < MATERIAL_STOCK_COST) {
            return preflight(true, false, false, "insufficient-site-stock",
                    siteLabel(site) + " cannot stage " + ROOM_NAME + ": requires exactly "
                            + MATERIAL_STOCK_COST + " site material stock, has " + stock + ".",
                    stock, first.workers(), candidate.spec(), null, candidate.validation());
        }

        String message = "READY: " + siteLabel(site) + " can stage " + ROOM_NAME + " from "
                + first.roomName() + " with " + first.workers() + " assigned worker(s), "
                + MATERIAL_STOCK_COST + " prepaid site stock, and " + LABOR_REQUIRED
                + " room-local labor. " + candidate.spec().readback();
        return preflight(true, true, false, "", message, stock, first.workers(),
                candidate.spec(), null, candidate.validation());
    }

    /** Stages one prepaid marker; duplicate calls resume it without another debit. */
    static Outcome attempt(GamePanel game, FactionStrategicPlan plan, NpcFactionSite site) {
        Preflight check = preflight(game, plan, site);
        if (!check.handled() || !check.canStage()) return outcome(check, false, stock(site));
        if (check.resume()) return outcome(check, true, stock(site));

        RoomShellSpec spec = check.spec();
        if (spec == null || game == null || game.world == null || site == null) {
            Preflight invalid = preflight(true, false, false, "missing-room-spec",
                    "Forge annex preflight did not produce a durable room specification; nothing was spent.",
                    stock(site), check.workers(), spec, null, check.blueprintValidation());
            return outcome(invalid, false, stock(site));
        }

        BaseObject marker = createMarker(plan, site, spec, check.workers());
        int before = site.stock;
        char originalTile = game.world.tiles[marker.x][marker.y];
        boolean added = false;
        try {
            site.stock = before - MATERIAL_STOCK_COST;
            game.baseObjects.add(marker);
            added = true;
            ProgressiveConstructionAuthority.syncSiteTile(game, marker);
        } catch (RuntimeException failure) {
            site.stock = before;
            if (added) game.baseObjects.remove(marker);
            if (game.world.inBounds(marker.x, marker.y)) game.world.tiles[marker.x][marker.y] = originalTile;
            Preflight blocked = preflight(true, false, false, "stage-commit-failed",
                    "Forge annex staging could not commit atomically; faction stock and the world tile were restored.",
                    before, check.workers(), spec, null, check.blueprintValidation());
            return outcome(blocked, false, before);
        }

        String message = "IN PROGRESS: " + siteLabel(site) + " prepaid and staged " + ROOM_NAME
                + " at " + spec.originX() + "," + spec.originY() + " from "
                + spec.sourceRoomName() + "; stock " + before + " -> " + site.stock
                + ", assigned workforce " + check.workers() + ", labor 0/" + LABOR_REQUIRED
                + ". Room arrays remain unchanged until completion re-preflight succeeds.";
        return new Outcome(true, true, "", message, spec.sourceRoomId(),
                spec.sourceRoomName(), spec.originX(), spec.originY(), before, site.stock,
                check.workers(), MATERIAL_STOCK_COST, LABOR_REQUIRED, marker, spec);
    }

    static boolean isRoomShellMarker(BaseObject object) {
        return object != null
                && FactionPhysicalConstructionAuthority.isFactionManaged(object)
                && MARKER_RECIPE.equalsIgnoreCase(safe(object.assignedRecipe).trim())
                && completedRoomSpec(object) != null;
    }

    static BaseObject activeSite(GamePanel game, NpcFactionSite site) {
        if (game == null || site == null || game.baseObjects == null) return null;
        String key = siteKey(site);
        BaseObject best = null;
        for (BaseObject object : game.baseObjects) {
            if (object == null || !object.underConstruction || !isRoomShellMarker(object)) continue;
            if (!key.equals(safe(object.constructionLinkedSiteName))) continue;
            if (best == null || markerKey(object).compareTo(markerKey(best)) < 0) best = object;
        }
        return best;
    }

    /** Advances exactly the workers still assigned to the persisted source-room ledger. */
    static ProgressiveConstructionAuthority.FactionWorkResult advanceHourly(
            GamePanel game, NpcFactionSite site) {
        BaseObject marker = activeSite(game, site);
        if (marker == null) {
            return new ProgressiveConstructionAuthority.FactionWorkResult(false, 0, false,
                    "No active Basic Forge Annex room-shell marker is linked to this faction site.");
        }
        RoomShellSpec spec = completedRoomSpec(marker);
        if (spec == null || game == null || game.world == null) {
            return new ProgressiveConstructionAuthority.FactionWorkResult(false, 0, false,
                    "The active forge-annex marker has no readable room receipt; labor is paused.");
        }
        Faction faction = site == null ? marker.faction : site.faction;
        int workers = roomWorkers(game.world, spec.sourceRoomId(), faction);
        marker.assignedWorker = crewLabel(site, spec.sourceRoomName(), workers);
        if (workers <= 0) {
            return new ProgressiveConstructionAuthority.FactionWorkResult(false, 0, false,
                    MARKER_RECIPE + " is paused: " + spec.sourceRoomName()
                            + " has no assigned same-family workers in its exact room ledger.");
        }

        int before = Math.max(0, marker.constructionLaborDone);
        int remaining = Math.max(0, LABOR_REQUIRED - before);
        if (remaining <= 0) {
            return new ProgressiveConstructionAuthority.FactionWorkResult(false, 0, false,
                    MARKER_RECIPE + " has full labor but awaits a successful completion re-preflight.");
        }
        int added = Math.min(remaining, workers);
        if (added < remaining) {
            marker.constructionLaborDone = before + added;
            marker.constructionVisualProgress = progressPercent(marker.constructionLaborDone);
            return new ProgressiveConstructionAuthority.FactionWorkResult(true, added, false,
                    MARKER_RECIPE + " advanced by " + added + " exact room-local labor from "
                            + marker.assignedWorker + "; labor " + marker.constructionLaborDone + "/"
                            + LABOR_REQUIRED + ", progress " + marker.constructionVisualProgress + "%. ");
        }

        CompletionPreflight completion = completionPreflight(game, marker, faction, false);
        if (!completion.canComplete()) {
            return new ProgressiveConstructionAuthority.FactionWorkResult(false, 0, false,
                    "Forge annex completion paused at " + before + "/" + LABOR_REQUIRED + " labor: "
                            + completion.message());
        }
        StampResult stamp = commitStamp(game, marker, faction, completion.spec(), true, false);
        if (!stamp.success()) {
            return new ProgressiveConstructionAuthority.FactionWorkResult(false, 0, false,
                    "Forge annex completion stayed staged: " + stamp.message());
        }

        marker.constructionLaborDone = LABOR_REQUIRED;
        marker.constructionVisualProgress = 100;
        marker.underConstruction = false;
        marker.name = ROOM_NAME + " construction plaque";
        marker.symbol = COMPLETED_MARKER_SYMBOL;
        marker.finalSymbol = COMPLETED_MARKER_SYMBOL;
        marker.description = "Completion plaque for a faction-built " + ROOM_NAME + ". "
                + spec.readback() + " Assigned workers were transferred from the source room ledger;"
                + " no population was created.";
        game.configureBaseObject(marker);
        reoverlayRuntimeObjects(game, spec);
        if (game.world != null) game.world.dirtyVisionRevision++;
        game.logEvent("Faction room construction complete: " + ROOM_NAME + " at "
                + spec.originX() + "," + spec.originY() + " for " + factionLabel(faction) + ".");
        DebugLog.audit("FACTION_ROOM_SHELL_COMPLETE", "room=" + stamp.roomId()
                + " sourceRoom=" + spec.sourceRoomId() + " workers=" + stamp.transferredWorkers()
                + " origin=" + spec.originX() + "," + spec.originY());
        return new ProgressiveConstructionAuthority.FactionWorkResult(true, added, true,
                "Faction room construction complete: " + ROOM_NAME + " stamped as room "
                        + stamp.roomId() + " after " + LABOR_REQUIRED + " labor; "
                        + stamp.transferredWorkers() + " assigned worker(s) transferred from "
                        + spec.sourceRoomName() + " without creating population.");
    }

    /**
     * Replays completed plaque receipts after the generated world, base objects,
     * map objects, and population ledgers have loaded. Appends only at the exact
     * persisted room id and preserves every loaded object glyph afterward.
     *
     * @return number of missing room shells appended during this call
     */
    static int restoreCompletedRooms(GamePanel game) {
        if (game == null || game.world == null || game.baseObjects == null) return 0;
        ArrayList<BaseObject> receipts = new ArrayList<>();
        for (BaseObject object : game.baseObjects) {
            if (isRoomShellMarker(object) && !object.underConstruction) receipts.add(object);
        }
        receipts.sort(Comparator.comparingInt(object -> {
            RoomShellSpec spec = completedRoomSpec(object);
            return spec == null ? Integer.MAX_VALUE : spec.expectedRoomId();
        }));

        int restored = 0;
        for (BaseObject receipt : receipts) {
            RoomShellSpec spec = completedRoomSpec(receipt);
            if (spec == null) continue;
            Faction faction = receipt.faction == null ? Faction.NONE : receipt.faction;
            int exact = exactStampedRoomId(game, spec, faction);
            if (exact == spec.expectedRoomId()) {
                bindPersistedAnnexLedger(game.world, receipt, spec, faction, exact);
                reoverlayRuntimeObjects(game, spec);
                continue;
            }
            if (game.world.rooms.size() != spec.expectedRoomId()) {
                DebugLog.warn("FACTION_ROOM_SHELL_RESTORE", "refused room-index drift expected="
                        + spec.expectedRoomId() + " actual=" + game.world.rooms.size()
                        + " origin=" + spec.originX() + "," + spec.originY());
                continue;
            }
            CompletionPreflight replay = completionPreflight(game, receipt, faction, true);
            if (!replay.canComplete()) {
                DebugLog.warn("FACTION_ROOM_SHELL_RESTORE", "receipt blocked=" + replay.blocker()
                        + " detail=" + replay.message());
                continue;
            }
            StampResult stamp = commitStamp(game, receipt, faction, spec, false, true);
            if (stamp.success() && !stamp.alreadyStamped()) restored++;
        }
        if (restored > 0) game.world.dirtyVisionRevision++;
        return restored;
    }

    /** Returns the deterministic completion receipt persisted on a shell marker. */
    static RoomShellSpec completedRoomSpec(BaseObject marker) {
        if (marker == null) return null;
        String source = safe(marker.constructionPlanSource);
        int at = source.indexOf(RECEIPT_PREFIX);
        if (at < 0) return null;
        String encoded = source.substring(at + RECEIPT_PREFIX.length()).trim();
        int space = encoded.indexOf(' ');
        if (space >= 0) encoded = encoded.substring(0, space);
        try {
            String payload = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            String[] fields = payload.split("\\|", -1);
            if (fields.length != 14 || !"v1".equals(fields[0])) return null;
            return new RoomShellSpec(
                    Integer.parseInt(fields[1]), Integer.parseInt(fields[2]), decodeText(fields[13]),
                    Integer.parseInt(fields[3]), Integer.parseInt(fields[4]), WIDTH, HEIGHT,
                    Integer.parseInt(fields[5]), Integer.parseInt(fields[6]),
                    Integer.parseInt(fields[7]), Integer.parseInt(fields[8]),
                    Integer.parseInt(fields[9]), Integer.parseInt(fields[10]), fields[11]);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    static String custodyReadback(BaseObject marker) {
        if (!isRoomShellMarker(marker)) return "No faction room-shell crew is assigned.";
        RoomShellSpec spec = completedRoomSpec(marker);
        String crew = safe(marker.assignedWorker).trim();
        if (crew.isBlank()) crew = "Assigned faction room-shell crew";
        return crew + "; " + (spec == null ? "unreadable room receipt" : spec.readback());
    }

    static List<String> inspectionLines(BaseObject marker) {
        if (!isRoomShellMarker(marker)) return List.of();
        RoomShellSpec spec = completedRoomSpec(marker);
        ArrayList<String> lines = new ArrayList<>();
        lines.add(marker.underConstruction
                ? "Room-shell status: staged faction construction; room arrays are unchanged."
                : "Room-shell status: completed Basic Forge Annex receipt.");
        lines.add("Room-shell owner: " + factionLabel(marker.faction) + "; "
                + safe(marker.assignedWorker) + ".");
        lines.add("Room-shell materials: exactly " + MATERIAL_STOCK_COST
                + " prepaid faction-site stock; player resources excluded.");
        lines.add("Room-shell labor: " + Math.max(0, marker.constructionLaborDone) + "/"
                + LABOR_REQUIRED + " exact source-room labor.");
        if (spec != null) lines.add("Room-shell receipt: " + spec.readback());
        return List.copyOf(lines);
    }

    static String statusLine(GamePanel game, NpcFactionSite site) {
        BaseObject active = activeSite(game, site);
        if (active != null) {
            RoomShellSpec spec = completedRoomSpec(active);
            int workers = spec == null || game == null || game.world == null ? 0
                    : roomWorkers(game.world, spec.sourceRoomId(), site == null ? active.faction : site.faction);
            return "Forge annex: IN PROGRESS; labor " + active.constructionLaborDone + "/"
                    + LABOR_REQUIRED + ", assigned source-room workforce " + workers
                    + (spec == null ? "." : ", footprint " + spec.originX() + "," + spec.originY() + ".");
        }
        if (game != null && game.baseObjects != null && site != null) {
            String key = siteKey(site);
            for (BaseObject object : game.baseObjects) {
                if (isRoomShellMarker(object) && !object.underConstruction
                        && key.equals(safe(object.constructionLinkedSiteName))) {
                    RoomShellSpec spec = completedRoomSpec(object);
                    return "Forge annex: COMPLETE"
                            + (spec == null ? "." : "; room " + spec.expectedRoomId()
                            + " at " + spec.originX() + "," + spec.originY() + ".");
                }
            }
        }
        return "Forge annex: no staged or completed shell is linked to this faction site.";
    }

    private static Preflight preflight(boolean handled, boolean canStage, boolean resume,
                                       String blocker, String message, int stock, int workers,
                                       RoomShellSpec spec, BaseObject marker,
                                       BlueprintConstructionAuthority.ValidationResult validation) {
        return new Preflight(handled, canStage, resume, safe(blocker), safe(message),
                spec == null ? -1 : spec.sourceRoomId(),
                spec == null ? "" : spec.sourceRoomName(),
                spec == null ? -1 : spec.originX(), spec == null ? -1 : spec.originY(),
                Math.max(0, stock), Math.max(0, workers), MATERIAL_STOCK_COST,
                LABOR_REQUIRED, marker, spec, validation);
    }

    private static Outcome outcome(Preflight check, boolean success, int stockAfter) {
        return new Outcome(check.handled(), success, check.blocker(), check.message(),
                check.sourceRoomId(), check.sourceRoomName(), check.originX(), check.originY(),
                check.stockBefore(), Math.max(0, stockAfter), check.workers(),
                MATERIAL_STOCK_COST, LABOR_REQUIRED, check.marker(), check.spec());
    }

    private static ArrayList<StaffedRoom> staffedControlledRooms(World world, Faction faction) {
        ArrayList<StaffedRoom> rooms = new ArrayList<>();
        if (world == null) return rooms;
        for (int roomId = 0; roomId < world.rooms.size(); roomId++) {
            if (!FactionIdentityAuthority.sameFamily(world.roomFaction(roomId), faction)) continue;
            if (roomId < world.roomSpecials.size()
                    && Boolean.TRUE.equals(world.roomSpecials.get(roomId))) continue;
            Rectangle room = world.roomRect(roomId);
            if (room == null) continue;
            int workers = roomWorkers(world, roomId, faction);
            if (workers <= 0) continue;
            rooms.add(new StaffedRoom(roomId, RoomOwnershipAuthority.roomName(world, roomId),
                    new Rectangle(room), workers));
        }
        return rooms;
    }

    private static boolean hasLegalMicroForgeTile(GamePanel game, int roomId, Rectangle room) {
        if (game == null || game.world == null || room == null) return false;
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
                if (baseObjectAt(game, x, y, null) != null
                        || world.isDoorAccessReservedForObject(x, y)) continue;
                return true;
            }
        }
        return false;
    }

    private static Candidate firstCandidate(GamePanel game, StaffedRoom source) {
        for (Side side : Side.values()) {
            for (int edgeCoordinate : centeredEdgeCoordinates(source.room(), side)) {
                RoomShellSpec spec = specFor(game.world, source, side, edgeCoordinate);
                if (spec == null) continue;
                BlueprintConstructionAuthority.ValidationResult validation =
                        validateCandidate(game, spec, null, false);
                if (validation.canPlace() && connectorClear(game, spec, null, false)) {
                    return new Candidate(spec, validation);
                }
            }
        }
        return null;
    }

    private static RoomShellSpec specFor(World world, StaffedRoom source, Side side,
                                         int edgeCoordinate) {
        if (world == null || source == null || side == null) return null;
        Rectangle room = source.room();
        int ox;
        int oy;
        int sourceDoorX;
        int sourceDoorY;
        int connectorX;
        int connectorY;
        int annexDoorX;
        int annexDoorY;
        switch (side) {
            case SOUTH -> {
                ox = edgeCoordinate - WIDTH / 2;
                oy = room.y + room.height + 1;
                sourceDoorX = edgeCoordinate;
                sourceDoorY = room.y + room.height - 1;
                connectorX = edgeCoordinate;
                connectorY = sourceDoorY + 1;
                annexDoorX = edgeCoordinate;
                annexDoorY = oy;
            }
            case NORTH -> {
                ox = edgeCoordinate - WIDTH / 2;
                oy = room.y - HEIGHT - 1;
                sourceDoorX = edgeCoordinate;
                sourceDoorY = room.y;
                connectorX = edgeCoordinate;
                connectorY = sourceDoorY - 1;
                annexDoorX = edgeCoordinate;
                annexDoorY = oy + HEIGHT - 1;
            }
            case EAST -> {
                ox = room.x + room.width + 1;
                oy = edgeCoordinate - HEIGHT / 2;
                sourceDoorX = room.x + room.width - 1;
                sourceDoorY = edgeCoordinate;
                connectorX = sourceDoorX + 1;
                connectorY = edgeCoordinate;
                annexDoorX = ox;
                annexDoorY = edgeCoordinate;
            }
            case WEST -> {
                ox = room.x - WIDTH - 1;
                oy = edgeCoordinate - HEIGHT / 2;
                sourceDoorX = room.x;
                sourceDoorY = edgeCoordinate;
                connectorX = sourceDoorX - 1;
                connectorY = edgeCoordinate;
                annexDoorX = ox + WIDTH - 1;
                annexDoorY = edgeCoordinate;
            }
            default -> throw new IllegalStateException("Unhandled annex side " + side);
        }
        return new RoomShellSpec(world.rooms.size(), source.roomId(), source.roomName(),
                ox, oy, WIDTH, HEIGHT, sourceDoorX, sourceDoorY, connectorX, connectorY,
                annexDoorX, annexDoorY, side.annexDoorDirection);
    }

    private static RoomShellSpec specForRoomOnly(World world, StaffedRoom source) {
        return world == null || source == null ? null
                : new RoomShellSpec(world.rooms.size(), source.roomId(), source.roomName(),
                -1, -1, WIDTH, HEIGHT, -1, -1, -1, -1, -1, -1, "");
    }

    private static List<Integer> centeredEdgeCoordinates(Rectangle room, Side side) {
        if (room == null) return List.of();
        int low = side == Side.SOUTH || side == Side.NORTH ? room.x + 1 : room.y + 1;
        int high = side == Side.SOUTH || side == Side.NORTH
                ? room.x + room.width - 2 : room.y + room.height - 2;
        if (low > high) return List.of();
        int center = (low + high) / 2;
        ArrayList<Integer> values = new ArrayList<>();
        values.add(center);
        for (int distance = 1; values.size() < high - low + 1; distance++) {
            if (center - distance >= low) values.add(center - distance);
            if (center + distance <= high) values.add(center + distance);
        }
        return values;
    }

    private static BlueprintConstructionAuthority.ValidationResult validateCandidate(
            GamePanel game, RoomShellSpec spec, BaseObject ignored, boolean replay) {
        if (game == null || game.world == null || spec == null) {
            return new BlueprintConstructionAuthority.ValidationResult(false, List.of(
                    new BlueprintConstructionAuthority.ValidationIssue(
                            BlueprintConstructionAuthority.ValidationSeverity.BLOCKED,
                            spec == null ? -1 : spec.originX(), spec == null ? -1 : spec.originY(),
                            "No live world or room-shell specification.")), 0,
                    "Placement blocked: no live room-shell specification.");
        }
        World world = game.world;
        ArrayList<BlueprintConstructionAuthority.TargetTile> targets = new ArrayList<>();
        BlueprintConstructionAuthority.RoomBlueprint blueprint =
                blueprintFor(spec.annexDoorDirection());
        for (BlueprintConstructionAuthority.BlueprintCell cell : blueprint.cells()) {
            int x = spec.originX() + cell.x();
            int y = spec.originY() + cell.y();
            boolean inBounds = world.inBounds(x, y);
            boolean occupied = inBounds && !replay && occupied(game, x, y, ignored);
            boolean transition = inBounds && BoundedOuterHiveWallApi.isTransitionTile(world.tiles[x][y]);
            boolean buildable = inBounds && world.roomIdAt(x, y) < 0 && !transition;
            String descriptor = !inBounds ? "outside loaded world"
                    : world.roomIdAt(x, y) >= 0 ? "existing room " + world.roomIdAt(x, y)
                    : transition ? "critical transition tile " + world.tiles[x][y]
                    : occupied ? "occupied external construction tile"
                    : "clear unassigned external construction tile";
            targets.add(new BlueprintConstructionAuthority.TargetTile(x, y, occupied,
                    false, false, transition, buildable, descriptor));
        }
        return BlueprintConstructionAuthority.preflight(blueprint, spec.originX(), spec.originY(),
                targets, BlueprintConstructionAuthority.estimateItemCost(blueprint));
    }

    private static boolean connectorClear(GamePanel game, RoomShellSpec spec,
                                          BaseObject ignored, boolean replay) {
        if (game == null || game.world == null || spec == null) return false;
        World world = game.world;
        if (!world.inBounds(spec.sourceDoorX(), spec.sourceDoorY())
                || !world.inBounds(spec.connectorX(), spec.connectorY())
                || !world.inBounds(spec.annexDoorX(), spec.annexDoorY())) return false;
        Rectangle source = world.roomRect(spec.sourceRoomId());
        if (source == null || !source.contains(spec.sourceDoorX(), spec.sourceDoorY())) return false;
        if (!FactionIdentityAuthority.sameFamily(world.roomFaction(spec.sourceRoomId()),
                ignored == null ? world.roomFaction(spec.sourceRoomId()) : ignored.faction)) return false;
        Point inward = inwardSourceTile(spec);
        boolean belongs = world.roomIdAt(spec.sourceDoorX(), spec.sourceDoorY()) == spec.sourceRoomId()
                || (inward != null && world.inBounds(inward.x, inward.y)
                && world.roomIdAt(inward.x, inward.y) == spec.sourceRoomId());
        if (!belongs) return false;
        if (world.roomIdAt(spec.connectorX(), spec.connectorY()) >= 0) return false;
        if (BoundedOuterHiveWallApi.isTransitionTile(world.tiles[spec.sourceDoorX()][spec.sourceDoorY()])
                || BoundedOuterHiveWallApi.isTransitionTile(world.tiles[spec.connectorX()][spec.connectorY()])) return false;
        if (!replay && (occupied(game, spec.sourceDoorX(), spec.sourceDoorY(), ignored)
                || occupied(game, spec.connectorX(), spec.connectorY(), ignored))) return false;
        return !(spec.sourceDoorX() == game.playerX && spec.sourceDoorY() == game.playerY)
                && !(spec.connectorX() == game.playerX && spec.connectorY() == game.playerY);
    }

    private static Point inwardSourceTile(RoomShellSpec spec) {
        if (spec == null) return null;
        return switch (spec.annexDoorDirection()) {
            case "N" -> new Point(spec.sourceDoorX(), spec.sourceDoorY() - 1);
            case "S" -> new Point(spec.sourceDoorX(), spec.sourceDoorY() + 1);
            case "W" -> new Point(spec.sourceDoorX() - 1, spec.sourceDoorY());
            case "E" -> new Point(spec.sourceDoorX() + 1, spec.sourceDoorY());
            default -> null;
        };
    }

    private static CompletionPreflight completionPreflight(GamePanel game, BaseObject marker,
                                                           Faction faction, boolean replay) {
        RoomShellSpec spec = completedRoomSpec(marker);
        if (game == null || game.world == null || spec == null) {
            return new CompletionPreflight(false, "missing-room-spec",
                    "the durable room-shell receipt is missing or unreadable", 0, spec);
        }
        World world = game.world;
        int exact = exactStampedRoomId(game, spec, faction);
        if (exact == spec.expectedRoomId()) {
            return new CompletionPreflight(true, "", "the exact room shell already exists",
                    roomWorkers(world, spec.sourceRoomId(), faction), spec);
        }
        if (world.rooms.size() != spec.expectedRoomId()) {
            return new CompletionPreflight(false, "room-index-drift",
                    "expected room id " + spec.expectedRoomId() + " but rooms.size is "
                            + world.rooms.size(), 0, spec);
        }
        if (spec.sourceRoomId() < 0 || spec.sourceRoomId() >= world.rooms.size()
                || !FactionIdentityAuthority.sameFamily(world.roomFaction(spec.sourceRoomId()), faction)) {
            return new CompletionPreflight(false, "source-room-changed",
                    "the persisted source room is missing or no longer same-family controlled", 0, spec);
        }
        if (spec.sourceRoomId() < world.roomSpecials.size()
                && Boolean.TRUE.equals(world.roomSpecials.get(spec.sourceRoomId()))) {
            return new CompletionPreflight(false, "source-room-special",
                    "the persisted source room is now special-purpose and cannot be cut through", 0, spec);
        }
        int workers = roomWorkers(world, spec.sourceRoomId(), faction);
        RoomPopulationLedger persistedAnnex = annexLedger(world, marker, spec);
        if (!replay && workers <= 0) {
            return new CompletionPreflight(false, "no-room-workforce",
                    "the source room no longer has assigned same-family workers", 0, spec);
        }
        if (replay && workers <= 0 && persistedAnnex == null) {
            return new CompletionPreflight(false, "missing-workforce-receipt",
                    "neither source-room workers nor the persisted annex workforce ledger exists",
                    0, spec);
        }
        BlueprintConstructionAuthority.ValidationResult validation =
                validateCandidate(game, spec, marker, replay);
        if (!validation.canPlace()) {
            return new CompletionPreflight(false, "footprint-obstructed",
                    validation.summary(), workers, spec);
        }
        if (!connectorClear(game, spec, marker, replay)) {
            return new CompletionPreflight(false, "connector-obstructed",
                    "the source door or one-cell connector is no longer clear", workers, spec);
        }
        return new CompletionPreflight(true, "", "completion preflight valid", workers, spec);
    }

    private static StampResult commitStamp(GamePanel game, BaseObject marker, Faction faction,
                                           RoomShellSpec spec, boolean transferWorkers,
                                           boolean replay) {
        if (game == null || game.world == null || spec == null) {
            return new StampResult(false, false, -1, 0, "missing-room-spec",
                    "No live room specification was available.");
        }
        World world = game.world;
        int exact = exactStampedRoomId(game, spec, faction);
        if (exact == spec.expectedRoomId()) {
            int bound = bindPersistedAnnexLedger(world, marker, spec, faction, exact);
            reoverlayRuntimeObjects(game, spec);
            return new StampResult(true, true, exact, bound, "",
                    "The exact persisted annex room was already stamped.");
        }
        if (world.rooms.size() != spec.expectedRoomId()) {
            return new StampResult(false, false, -1, 0, "room-index-drift",
                    "Expected room id " + spec.expectedRoomId() + " but rooms.size is "
                            + world.rooms.size() + ".");
        }

        CompletionPreflight completion = completionPreflight(game, marker, faction, replay);
        if (!completion.canComplete()) {
            return new StampResult(false, false, -1, 0, completion.blocker(), completion.message());
        }

        int roomsSize = world.rooms.size();
        int profilesSize = world.roomProfiles.size();
        int factionsSize = world.roomFactions.size();
        int specialsSize = world.roomSpecials.size();
        int ledgersSize = world.roomPopulationLedgers.size();
        HashMap<String, Character> oldTiles = new HashMap<>();
        HashMap<String, Integer> oldRoomIds = new HashMap<>();
        snapshotCell(world, spec.sourceDoorX(), spec.sourceDoorY(), oldTiles, oldRoomIds);
        snapshotCell(world, spec.connectorX(), spec.connectorY(), oldTiles, oldRoomIds);
        for (int x = spec.originX(); x < spec.originX() + spec.width(); x++) {
            for (int y = spec.originY(); y < spec.originY() + spec.height(); y++) {
                snapshotCell(world, x, y, oldTiles, oldRoomIds);
            }
        }
        HashMap<RoomPopulationLedger, Integer> oldAssigned = new HashMap<>();
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (ledger != null) oldAssigned.put(ledger, ledger.assigned);
        }
        RoomPopulationLedger existingAnnex = annexLedger(world, marker, spec);
        int existingRoomId = existingAnnex == null ? -1 : existingAnnex.roomId;
        String existingRoomName = existingAnnex == null ? "" : existingAnnex.roomName;
        Faction existingFaction = existingAnnex == null ? Faction.NONE : existingAnnex.faction;

        try {
            int roomId = world.rooms.size();
            RoomProfile profile = annexProfile(faction);
            world.rooms.add(spec.footprint());
            world.roomProfiles.add(profile);
            world.roomFactions.add(faction == null ? Faction.NONE : faction);
            world.roomSpecials.add(Boolean.FALSE);

            BlueprintConstructionAuthority.RoomBlueprint blueprint =
                    blueprintFor(spec.annexDoorDirection());
            for (BlueprintConstructionAuthority.BlueprintCell cell : blueprint.cells()) {
                int x = spec.originX() + cell.x();
                int y = spec.originY() + cell.y();
                world.roomIds[x][y] = roomId;
                world.tiles[x][y] = switch (cell.kind()) {
                    case WALL -> '#';
                    case DOOR -> '/';
                    default -> '.';
                };
            }
            world.roomIds[spec.sourceDoorX()][spec.sourceDoorY()] = spec.sourceRoomId();
            world.tiles[spec.sourceDoorX()][spec.sourceDoorY()] = '/';
            world.roomIds[spec.connectorX()][spec.connectorY()] = -1;
            world.tiles[spec.connectorX()][spec.connectorY()] = BoundedOuterHiveWallApi.HIVEWALL_CORRIDOR;

            int transferred = transferWorkers
                    ? transferAssignedWorkers(world, marker, spec, faction, roomId)
                    : bindPersistedAnnexLedger(world, marker, spec, faction, roomId);
            reoverlayRuntimeObjects(game, spec);
            return new StampResult(true, false, roomId, transferred, "",
                    "Room shell and workforce ledger committed atomically.");
        } catch (RuntimeException failure) {
            trimTo(world.rooms, roomsSize);
            trimTo(world.roomProfiles, profilesSize);
            trimTo(world.roomFactions, factionsSize);
            trimTo(world.roomSpecials, specialsSize);
            trimTo(world.roomPopulationLedgers, ledgersSize);
            for (Map.Entry<String, Character> entry : oldTiles.entrySet()) {
                int[] point = parseCellKey(entry.getKey());
                world.tiles[point[0]][point[1]] = entry.getValue();
                world.roomIds[point[0]][point[1]] = oldRoomIds.get(entry.getKey());
            }
            for (Map.Entry<RoomPopulationLedger, Integer> entry : oldAssigned.entrySet()) {
                entry.getKey().assigned = entry.getValue();
            }
            if (existingAnnex != null) {
                existingAnnex.roomId = existingRoomId;
                existingAnnex.roomName = existingRoomName;
                existingAnnex.faction = existingFaction;
            }
            return new StampResult(false, false, -1, 0, "stamp-commit-failed",
                    "Room shell commit failed and every room, tile, and workforce mutation was restored.");
        }
    }

    private static int transferAssignedWorkers(World world, BaseObject marker, RoomShellSpec spec,
                                               Faction faction, int newRoomId) {
        RoomPopulationLedger annex = annexLedger(world, marker, spec);
        if (annex != null) {
            annex.roomId = newRoomId;
            annex.roomName = ROOM_NAME;
            annex.faction = faction == null ? Faction.NONE : faction;
            return Math.max(0, annex.assigned);
        }
        int transferred = 0;
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (ledger == null || ledger.roomId != spec.sourceRoomId()
                    || !FactionIdentityAuthority.sameFamily(ledger.faction, faction)) continue;
            int assigned = Math.max(0, ledger.assigned);
            transferred += assigned;
            ledger.assigned = Math.max(0, ledger.assigned - assigned);
        }
        RoomPopulationLedger created = annexLedgerRecord(marker, spec, faction, newRoomId, transferred);
        world.roomPopulationLedgers.add(created);
        return transferred;
    }

    private static int bindPersistedAnnexLedger(World world, BaseObject marker, RoomShellSpec spec,
                                                Faction faction, int roomId) {
        RoomPopulationLedger annex = annexLedger(world, marker, spec);
        if (annex == null) {
            annex = annexLedgerRecord(marker, spec, faction, roomId, 0);
            world.roomPopulationLedgers.add(annex);
        } else {
            annex.roomId = roomId;
            annex.roomName = ROOM_NAME;
            annex.faction = faction == null ? Faction.NONE : faction;
        }
        return Math.max(0, annex.assigned);
    }

    private static RoomPopulationLedger annexLedgerRecord(BaseObject marker, RoomShellSpec spec,
                                                           Faction faction, int roomId,
                                                           int assigned) {
        RoomPopulationLedger ledger = new RoomPopulationLedger();
        String receipt = receiptKey(marker, spec);
        ledger.id = "population.forge-annex." + Integer.toUnsignedString(receipt.hashCode(), 36);
        ledger.roomId = roomId;
        ledger.roomName = ROOM_NAME;
        ledger.faction = faction == null ? Faction.NONE : faction;
        ledger.sourceKind = "faction construction transfer";
        ledger.sourceLabel = "assigned workers transferred from " + spec.sourceRoomName();
        ledger.facilityId = receipt;
        ledger.facilityPurpose = "staff and fit the Basic Forge Annex";
        ledger.facilityEstablishedBy = factionLabel(faction) + " room-shell construction";
        ledger.facilityProductFocus = "EMM Micro Forge installation space";
        ledger.facilityHistoricNote = "Transferred from room " + spec.sourceRoomId()
                + " without creating population.";
        ledger.capacity = Math.max(0, assigned);
        ledger.available = 0;
        ledger.assigned = Math.max(0, assigned);
        ledger.dead = 0;
        return ledger;
    }

    private static RoomPopulationLedger annexLedger(World world, BaseObject marker,
                                                     RoomShellSpec spec) {
        if (world == null || world.roomPopulationLedgers == null || spec == null) return null;
        String receipt = receiptKey(marker, spec);
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (ledger != null && receipt.equals(safe(ledger.facilityId))) return ledger;
        }
        return null;
    }

    private static int exactStampedRoomId(GamePanel game, RoomShellSpec spec, Faction faction) {
        if (game == null || game.world == null || spec == null) return -1;
        World world = game.world;
        int roomId = spec.expectedRoomId();
        if (roomId < 0 || roomId >= world.rooms.size()
                || roomId >= world.roomProfiles.size()
                || roomId >= world.roomFactions.size()
                || roomId >= world.roomSpecials.size()) return -1;
        Rectangle room = world.roomRect(roomId);
        RoomProfile profile = world.roomProfiles.get(roomId);
        if (room == null || !room.equals(spec.footprint()) || profile == null
                || !ROOM_NAME.equals(profile.name)
                || !FactionIdentityAuthority.sameFamily(world.roomFaction(roomId), faction)
                || Boolean.TRUE.equals(world.roomSpecials.get(roomId))) return -1;
        BlueprintConstructionAuthority.RoomBlueprint blueprint =
                blueprintFor(spec.annexDoorDirection());
        for (BlueprintConstructionAuthority.BlueprintCell cell : blueprint.cells()) {
            int x = spec.originX() + cell.x();
            int y = spec.originY() + cell.y();
            if (!world.inBounds(x, y) || world.roomIdAt(x, y) != roomId) return -1;
            if (overlayAt(game, x, y)) continue;
            char expected = cell.kind() == BlueprintConstructionAuthority.CellKind.WALL ? '#'
                    : cell.kind() == BlueprintConstructionAuthority.CellKind.DOOR ? '/' : '.';
            if (world.tiles[x][y] != expected) return -1;
        }
        if (world.roomIdAt(spec.sourceDoorX(), spec.sourceDoorY()) != spec.sourceRoomId()
                || world.roomIdAt(spec.connectorX(), spec.connectorY()) >= 0) return -1;
        if (!overlayAt(game, spec.sourceDoorX(), spec.sourceDoorY())
                && world.tiles[spec.sourceDoorX()][spec.sourceDoorY()] != '/') return -1;
        if (!overlayAt(game, spec.connectorX(), spec.connectorY())
                && world.tiles[spec.connectorX()][spec.connectorY()] != BoundedOuterHiveWallApi.HIVEWALL_CORRIDOR) return -1;
        return roomId;
    }

    private static BaseObject createMarker(FactionStrategicPlan plan, NpcFactionSite site,
                                           RoomShellSpec spec, int workers) {
        BuildRecipe recipe = new BuildRecipe(MARKER_RECIPE, COMPLETED_MARKER_SYMBOL,
                MATERIAL_STOCK_COST, 0, 0, 0, 0, LABOR_REQUIRED, 0,
                false, site.faction, null,
                "A prepaid faction room-shell marker for a 5x4 Basic Forge Annex.");
        Point markerTile = spec.markerTile();
        BaseObject marker = ProgressiveConstructionAuthority.createSite(
                recipe, markerTile.x, markerTile.y, LABOR_REQUIRED);
        marker.constructionInsertedItems = marker.constructionRequiredItems;
        marker.constructionVisualProgress = 0;
        marker.faction = site.faction;
        marker.symbol = MARKER_SYMBOL;
        marker.finalSymbol = COMPLETED_MARKER_SYMBOL;
        marker.assignedRecipe = MARKER_RECIPE;
        marker.assignedWorker = crewLabel(site, spec.sourceRoomName(), workers);
        marker.constructionOwnerMode = FactionPhysicalConstructionAuthority.FACTION_OWNER_MODE;
        marker.constructionMaterialSource = siteLabel(site)
                + " prepaid faction-site stock; exactly " + MATERIAL_STOCK_COST + " units";
        marker.constructionPlanSource = encodeSpec(spec);
        marker.constructionLinkedSiteName = siteKey(site);
        marker.constructionLinkedPlanId = plan == null || plan.id == null ? "" : plan.id;
        marker.description = "Faction-managed room-shell construction for " + siteLabel(site)
                + ". " + spec.readback() + " Materials are prepaid from faction-site stock;"
                + " only assigned source-room workers add labor; player resources and labor are excluded.";
        return marker;
    }

    private static String encodeSpec(RoomShellSpec spec) {
        String payload = String.join("|", "v1", Integer.toString(spec.expectedRoomId()),
                Integer.toString(spec.sourceRoomId()), Integer.toString(spec.originX()),
                Integer.toString(spec.originY()), Integer.toString(spec.sourceDoorX()),
                Integer.toString(spec.sourceDoorY()), Integer.toString(spec.connectorX()),
                Integer.toString(spec.connectorY()), Integer.toString(spec.annexDoorX()),
                Integer.toString(spec.annexDoorY()), spec.annexDoorDirection(),
                BLUEPRINT_ID, encodeText(spec.sourceRoomName()));
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return "Basic Forge Annex hollow-box plan; " + RECEIPT_PREFIX + encoded;
    }

    private static BlueprintConstructionAuthority.RoomBlueprint blueprintFor(String direction) {
        BlueprintConstructionAuthority.RoomBlueprint base = BlueprintConstructionAuthority.hollowBox(
                BLUEPRINT_ID, ROOM_NAME, WIDTH, HEIGHT, false);
        int doorX;
        int doorY;
        String normalized = safe(direction).toUpperCase(Locale.ROOT);
        switch (normalized) {
            case "S" -> {
                doorX = WIDTH / 2;
                doorY = HEIGHT - 1;
            }
            case "W" -> {
                doorX = 0;
                doorY = HEIGHT / 2;
            }
            case "E" -> {
                doorX = WIDTH - 1;
                doorY = HEIGHT / 2;
            }
            default -> {
                normalized = "N";
                doorX = WIDTH / 2;
                doorY = 0;
            }
        }
        ArrayList<BlueprintConstructionAuthority.BlueprintCell> cells = new ArrayList<>();
        for (BlueprintConstructionAuthority.BlueprintCell cell : base.cells()) {
            if (cell.x() == doorX && cell.y() == doorY) {
                cells.add(new BlueprintConstructionAuthority.BlueprintCell(doorX, doorY,
                        BlueprintConstructionAuthority.CellKind.DOOR, "door_basic"));
            } else {
                cells.add(cell);
            }
        }
        return new BlueprintConstructionAuthority.RoomBlueprint(BLUEPRINT_ID, ROOM_NAME,
                "forge annex", "mechanist-structure", WIDTH, HEIGHT, cells,
                List.of(new BlueprintConstructionAuthority.AnchorPoint(doorX, doorY,
                        BlueprintConstructionAuthority.AnchorKind.DOORWAY, normalized)),
                List.of(), List.of("#Room", "#HollowBox", "#Mechanist", "#ForgeAnnex"));
    }

    private static RoomProfile annexProfile(Faction faction) {
        return new RoomProfile(ROOM_NAME,
                "a compact faction-built 5x4 forge annex with a two-door one-cell connector"
                        + " returning to the staffed source room",
                22, faction == null ? Faction.NONE : faction,
                new String[]{"machine scrap", "rivet set", "wire bundle"},
                new char[]{'N', 'q'}).withFeatures(
                "Faction-built shell reserved for an EMM Micro Forge. The completion plaque"
                        + " records prepaid stock, exact source-room labor, connector geometry,"
                        + " and the worker-ledger transfer that staffed this annex.");
    }

    private static void reoverlayRuntimeObjects(GamePanel game, RoomShellSpec spec) {
        if (game == null || game.world == null || spec == null) return;
        World world = game.world;
        if (world.mapObjects != null) {
            for (MapObjectState object : world.mapObjects) {
                if (object != null && world.inBounds(object.x, object.y)
                        && touchesSpec(spec, object.x, object.y)) {
                    world.tiles[object.x][object.y] = object.glyph;
                }
            }
        }
        if (game.baseObjects != null) {
            for (BaseObject object : game.baseObjects) {
                if (object != null && world.inBounds(object.x, object.y)
                        && touchesSpec(spec, object.x, object.y)) {
                    world.tiles[object.x][object.y] = object.symbol;
                }
            }
        }
    }

    private static boolean touchesSpec(RoomShellSpec spec, int x, int y) {
        return spec.footprint().contains(x, y)
                || (x == spec.sourceDoorX() && y == spec.sourceDoorY())
                || (x == spec.connectorX() && y == spec.connectorY());
    }

    private static boolean overlayAt(GamePanel game, int x, int y) {
        if (game == null || game.world == null) return false;
        if (game.baseObjects != null) {
            for (BaseObject object : game.baseObjects) {
                if (object != null && object.x == x && object.y == y) return true;
            }
        }
        if (game.world.mapObjects != null) {
            for (MapObjectState object : game.world.mapObjects) {
                if (object != null && object.x == x && object.y == y) return true;
            }
        }
        return false;
    }

    private static boolean occupied(GamePanel game, int x, int y, BaseObject ignored) {
        if (game == null || game.world == null) return true;
        if (x == game.playerX && y == game.playerY) return true;
        if (game.world.npcAt(x, y) != null || game.world.mapObjectAt(x, y) != null) return true;
        return baseObjectAt(game, x, y, ignored) != null;
    }

    private static BaseObject baseObjectAt(GamePanel game, int x, int y, BaseObject ignored) {
        if (game == null || game.baseObjects == null) return null;
        for (BaseObject object : game.baseObjects) {
            if (object != null && object != ignored && object.x == x && object.y == y) return object;
        }
        return null;
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

    private static boolean sameLocation(NpcFactionSite site, World world) {
        return site != null && world != null
                && site.sectorX == world.sectorX && site.sectorY == world.sectorY
                && site.zoneX == world.zoneX && site.zoneY == world.zoneY
                && site.floor == world.floor;
    }

    private static boolean mechanistFamily(Faction faction) {
        return FactionIdentityAuthority.sameFamily(faction, Faction.MECHANIST_COLLEGIA);
    }

    private static int progressPercent(int laborDone) {
        return Math.max(0, Math.min(100, (Math.max(0, laborDone) * 100) / LABOR_REQUIRED));
    }

    private static int stock(NpcFactionSite site) {
        return site == null ? 0 : Math.max(0, site.stock);
    }

    private static String crewLabel(NpcFactionSite site, String sourceRoomName, int workers) {
        return ("Faction room-shell crew: " + siteLabel(site) + "; " + Math.max(0, workers)
                + " assigned worker(s) from " + safe(sourceRoomName)).replace('|', '/');
    }

    private static String siteKey(NpcFactionSite site) {
        if (site == null) return "room-shell:unlinked-site";
        return ("room-shell:" + siteLabel(site) + ":sector-" + site.sectorX + "-" + site.sectorY
                + ":zone-" + site.zoneX + "-" + site.zoneY + ":floor-" + site.floor)
                .replace('|', '/').replace(',', '/');
    }

    private static String receiptKey(BaseObject marker, RoomShellSpec spec) {
        return safe(marker == null ? "" : marker.constructionLinkedSiteName)
                + ":annex-room-" + (spec == null ? -1 : spec.expectedRoomId());
    }

    private static String markerKey(BaseObject object) {
        if (object == null) return "~";
        return String.format(Locale.ROOT, "%08d|%08d|%s", object.x, object.y, safe(object.name));
    }

    private static String siteLabel(NpcFactionSite site) {
        return site == null || site.name == null || site.name.isBlank()
                ? "Unnamed faction site" : site.name.trim();
    }

    private static String factionLabel(Faction faction) {
        return faction == null ? Faction.NONE.label : faction.label;
    }

    private static String encodeText(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(safe(value).getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeText(String value) {
        return new String(Base64.getUrlDecoder().decode(safe(value)), StandardCharsets.UTF_8);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void snapshotCell(World world, int x, int y,
                                     Map<String, Character> tiles,
                                     Map<String, Integer> roomIds) {
        if (world == null || !world.inBounds(x, y)) return;
        String key = cellKey(x, y);
        tiles.putIfAbsent(key, world.tiles[x][y]);
        roomIds.putIfAbsent(key, world.roomIds[x][y]);
    }

    private static String cellKey(int x, int y) {
        return x + "," + y;
    }

    private static int[] parseCellKey(String key) {
        int cut = key.indexOf(',');
        return new int[]{Integer.parseInt(key.substring(0, cut)),
                Integer.parseInt(key.substring(cut + 1))};
    }

    private static <T> void trimTo(List<T> list, int size) {
        while (list.size() > size) list.remove(list.size() - 1);
    }
}
