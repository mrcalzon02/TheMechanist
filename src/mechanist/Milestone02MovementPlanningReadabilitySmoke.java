package mechanist;

import java.awt.Point;
import java.util.List;

/** Smoke for Milestone 02 movement planning refusal, route-summary language, and safe-tile recovery search. */
final class Milestone02MovementPlanningReadabilitySmoke {
    public static void main(String[] args) {
        MovementPlanningAuthority.MovementPlanReadout selected = MovementPlanningAuthority.describePlan(
                "Walk", 4, 3, 2, List.of(new Point(2, 2), new Point(3, 2)), true, true, false);
        require(selected.reachable(), "selected route should be reachable");
        require(selected.exact(), "selected route should be exact");
        requireContains(selected.summary(), "Movement target selected", "selected route action");
        rejectLeaks(selected.summary(), "selected route");

        MovementPlanningAuthority.MovementPlanReadout partial = MovementPlanningAuthority.describePlan(
                "Run", 2, 5, 2, List.of(new Point(2, 2), new Point(3, 2)), true, true, false);
        require(partial.reachable(), "partial route should still be reachable");
        require(!partial.exact(), "partial route should not be exact");
        requireContains(partial.summary(), "Partial route", "partial route label");
        requireContains(partial.summary(), "Too far for one movement chain", "partial route distance reason");
        rejectLeaks(partial.summary(), "partial route");

        MovementPlanningAuthority.MovementPlanReadout occupied = MovementPlanningAuthority.describePlan(
                "Sprint", 7, 4, 4, List.of(), true, true, true);
        require(!occupied.reachable(), "occupied destination should be refused without resolver");
        requireContains(occupied.summary(), "Destination occupied", "occupied destination reason");
        rejectLeaks(occupied.summary(), "occupied destination");

        MovementPlanningAuthority.MovementPlanReadout occupiedPushSqueeze = MovementPlanningAuthority.describePlan(
                "Sprint", 7, 4, 4, List.of(), true, true, true, true);
        require(occupiedPushSqueeze.reachable(), "occupied destination should be routeable with push/squeeze resolver");
        require(occupiedPushSqueeze.exact(), "occupied push/squeeze destination should be exact pending commit resolution");
        requireContains(occupiedPushSqueeze.summary(), "shove/squeeze", "occupied push/squeeze routing text");
        rejectLeaks(occupiedPushSqueeze.summary(), "occupied push/squeeze destination");

        MovementPlanningAuthority.OccupiedTileRoutingReadout pushRouting = MovementPlanningAuthority.occupiedTileRoutingForPlanning(true, true, true, true);
        require(!pushRouting.hardBlocked(), "push/squeeze route should not hard block occupied tile");
        require(pushRouting.pushSqueezeEligible(), "push/squeeze route should be eligible");
        require(MovementPlanningAuthority.canEnterForMovementCommit(true, true, true, true), "movement commit bridge should allow resolver-backed occupied entry");

        MovementPlanningAuthority.MovementPlanReadout blocked = MovementPlanningAuthority.describePlan(
                "Sneak", 1, 1, 1, List.of(), true, false, false);
        require(!blocked.reachable(), "blocked destination should be refused");
        requireContains(blocked.summary(), "Path blocked", "blocked destination reason");

        MovementPlanningAuthority.MovementPlanReadout outside = MovementPlanningAuthority.describePlan(
                "Walk", 1, -1, 8, List.of(), false, false, false);
        require(!outside.reachable(), "outside destination should be refused");
        requireContains(outside.summary(), "outside the current area", "outside destination reason");

        MovementPlanningAuthority.MovementPlanReadout unreachable = MovementPlanningAuthority.describePlan(
                "Walk", 1, 2, 2, List.of(), true, true, false);
        require(!unreachable.reachable(), "empty path should be refused");
        requireContains(unreachable.summary(), "Cannot reach from here", "unreachable destination reason");
        rejectContains(unreachable.summary(), "targetZoneKey", "movement denial should not expose raw route keys");

        ZoneTileState[][] alreadySafe = floorGrid(3, 3);
        MovementPlanningAuthority.StandableTileSearchResult noRecovery = MovementPlanningAuthority.nearestStandableTile(alreadySafe, 1, 1, 1, 4);
        require(!noRecovery.found(), "safe current tile should not require recovery destination");
        requireContains(noRecovery.summary(), "already standable", "safe current tile summary");

        ZoneTileState[][] trapped = floorGrid(7, 7);
        trapped[3][3].addFlag(ZoneTileState.TileFlag.BLOCKS_MOVEMENT);
        blockRing(trapped, 3, 3, 1);
        trapped[5][3].setOccupantEntityId("npc-blocker");
        MovementPlanningAuthority.StandableTileSearchResult recovery = MovementPlanningAuthority.nearestStandableTile(trapped, 3, 3, 1, 4);
        require(recovery.found(), "blocked current tile should find nearby standable recovery tile");
        require(recovery.radiusUsed() >= 2, "recovery search should expand past blocked first ring");
        require(MovementPlanningAuthority.standableZoneTile(trapped, recovery.x(), recovery.y()), "recovery destination should be standable");
        require(!(recovery.x() == 5 && recovery.y() == 3), "occupied candidate should not be selected");
        requireContains(recovery.summary(), "Nearest standable tile selected", "recovery destination summary");
        requireContains(MovementPlanningAuthority.movementRecoveryAuditSummary(), "expandingRadius", "recovery audit summary");
        requireContains(MovementPlanningAuthority.movementRecoveryAuditSummary(), "runtimeBridge=applyNearestStandableRecovery", "runtime bridge audit summary");

        MovementPlanningAuthority.MovementRecoveryApplicationResult nullRecovery = MovementPlanningAuthority.applyNearestStandableRecovery(null, 1, 2);
        require(!nullRecovery.applied(), "null game recovery should fail safely");
        requireContains(nullRecovery.summary(), "No world is loaded", "null game recovery summary");

        ZoneTileState[][] noCandidate = floorGrid(3, 3);
        for (int x = 0; x < noCandidate.length; x++) {
            for (int y = 0; y < noCandidate[x].length; y++) noCandidate[x][y].addFlag(ZoneTileState.TileFlag.BLOCKS_MOVEMENT);
        }
        MovementPlanningAuthority.StandableTileSearchResult failed = MovementPlanningAuthority.nearestStandableTile(noCandidate, 1, 1, 1, 2);
        require(!failed.found(), "fully blocked grid should not find recovery destination");
        requireContains(failed.summary(), "No standable tile found", "failed recovery summary");

        World hazardWorld = new World(7L, 5, 5);
        hazardWorld.hazardWarnings.add(new EnvironmentalHazardRecord("test-hazard", "thermal hazard", "Steam wash",
                "visible steam", 2, 1, -1, 58, 0));
        hazardWorld.hazardWarnings.add(new EnvironmentalHazardRecord("test-hazard-2", "shorted wires", "Live wires",
                "visible sparks", 3, 1, -1, 34, 0));
        MovementPlanningAuthority.HazardRouteReadout hazardRoute = MovementPlanningAuthority.inspectRouteHazards(
                hazardWorld, List.of(new Point(1, 1), new Point(2, 1), new Point(3, 1)));
        require(hazardRoute.hazardous(), "route crossing recorded hazards should be marked hazardous");
        require(hazardRoute.hazardousTiles() == 2, "hazard route should count hazardous tiles once each");
        requireContains(hazardRoute.summary(), "Steam wash (severe)", "highest route hazard");
        requireContains(hazardRoute.summary(), "Movement remains available", "hazard warning must not imply automatic refusal");
        rejectLeaks(hazardRoute.summary(), "hazard route warning");
        require(MovementPlanningAuthority.requiresHazardConfirmation(hazardWorld, 2, 1),
                "direct movement should require confirmation for a recorded hazard tile");
        require(!MovementPlanningAuthority.requiresHazardConfirmation(hazardWorld, 1, 1),
                "direct movement should remain immediate for an ordinary tile");
    }

    private static ZoneTileState[][] floorGrid(int w, int h) {
        ZoneTileState[][] tiles = new ZoneTileState[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) tiles[x][y] = ZoneTileState.fromLegacyGlyph('.');
        }
        return tiles;
    }

    private static void blockRing(ZoneTileState[][] tiles, int cx, int cy, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) continue;
                int x = cx + dx;
                int y = cy + dy;
                if (x >= 0 && y >= 0 && x < tiles.length && y < tiles[x].length) tiles[x][y].addFlag(ZoneTileState.TileFlag.BLOCKS_MOVEMENT);
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text == null || !text.contains(expected)) {
            throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
        }
    }

    private static void rejectContains(String text, String forbidden, String label) {
        if (text != null && text.contains(forbidden)) throw new AssertionError(label + ": " + text);
    }

    private static void rejectLeaks(String text, String label) {
        if (PlayerFacingText.containsLikelyLeak(text)) {
            throw new AssertionError("Player-facing leak in " + label + ": " + text);
        }
    }

    private Milestone02MovementPlanningReadabilitySmoke() { }
}
