package mechanist;

import java.util.List;

/** Smoke for actor-layer push/squeeze movement in confined spaces and crowds. */
final class Milestone02ZoneTilePushSqueezeMovementSmoke {
    public static void main(String[] args) {
        testOpenMove();
        testPushIntoReliefSpace();
        testChainPushInConfinedCorridor();
        testBlockedCrowdFailsafe();
        testRoutingDebugPlan();
    }

    private static void testOpenMove() {
        ZoneTileState[][] tiles = floorGrid(3, 1);
        tiles[0][0].setOccupantEntityId("player");
        ZoneTileMovementResolutionAuthority.Resolution resolution = ZoneTileMovementResolutionAuthority.resolve(tiles,
                List.of(new ZoneTileMovementResolutionAuthority.MoveIntent("player", 0, 0, 1, 0, 10)));
        requireMovedTo(resolution, "player", 1, 0, "open move");
        requireContains(resolution.summary(), "actor-layer push/squeeze", "open move summary");
        requireContains(resolution.debugLog().toString(), "Movement routing start", "open move debug start");
    }

    private static void testPushIntoReliefSpace() {
        ZoneTileState[][] tiles = floorGrid(3, 2);
        tiles[0][0].setOccupantEntityId("player");
        tiles[1][0].setOccupantEntityId("worker");
        ZoneTileMovementResolutionAuthority.Resolution resolution = ZoneTileMovementResolutionAuthority.resolve(tiles,
                List.of(new ZoneTileMovementResolutionAuthority.MoveIntent("player", 0, 0, 1, 0, 10)));
        requireMovedTo(resolution, "player", 1, 0, "player should push into target");
        ZoneTileMovementResolutionAuthority.MoveOutcome worker = resolution.outcomeFor("worker");
        require(worker != null && worker.moved(), "worker should be displaced into relief space");
        requireContains(resolution.debugLog().toString(), "Push/squeeze", "push debug log");
        require(!resolution.failsafeUsed(), "push into relief space should not use failsafe");
    }

    private static void testChainPushInConfinedCorridor() {
        ZoneTileState[][] tiles = floorGrid(4, 1);
        tiles[0][0].setOccupantEntityId("player");
        tiles[1][0].setOccupantEntityId("worker-a");
        tiles[2][0].setOccupantEntityId("worker-b");
        ZoneTileMovementResolutionAuthority.Resolution resolution = ZoneTileMovementResolutionAuthority.resolve(tiles,
                List.of(new ZoneTileMovementResolutionAuthority.MoveIntent("player", 0, 0, 1, 0, 10)));
        requireMovedTo(resolution, "player", 1, 0, "player chain push");
        requireMovedTo(resolution, "worker-a", 2, 0, "first worker chain push");
        requireMovedTo(resolution, "worker-b", 3, 0, "second worker chain push into end space");
        requireContains(resolution.debugLog().toString(), "Relief candidate", "chain push relief debug");
    }

    private static void testBlockedCrowdFailsafe() {
        ZoneTileState[][] tiles = floorGrid(3, 1);
        tiles[0][0].setOccupantEntityId("player");
        tiles[1][0].setOccupantEntityId("worker-a");
        tiles[2][0].setOccupantEntityId("worker-b");
        ZoneTileMovementResolutionAuthority.Resolution resolution = ZoneTileMovementResolutionAuthority.resolve(tiles,
                List.of(new ZoneTileMovementResolutionAuthority.MoveIntent("player", 0, 0, 1, 0, 10)));
        ZoneTileMovementResolutionAuthority.MoveOutcome player = resolution.outcomeFor("player");
        require(player != null && !player.moved(), "player should hold when corridor has no relief space");
        require(resolution.failsafeUsed(), "blocked crowd should mark failsafe");
        requireContains(player.reason(), "no shove or squeeze relief", "blocked crowd reason");
    }

    private static void testRoutingDebugPlan() {
        ZoneTileState[][] tiles = floorGrid(2, 1);
        tiles[0][0].setOccupantEntityId("player");
        tiles[1][0].addFlag(ZoneTileState.TileFlag.BLOCKS_MOVEMENT);
        ZoneTileMovementResolutionAuthority.RoutingDebugPlan plan = ZoneTileMovementResolutionAuthority.debugPlanFor(tiles,
                new ZoneTileMovementResolutionAuthority.MoveIntent("player", 0, 0, 1, 0, 10));
        require(!plan.routeSafe(), "debug plan should mark blocked route unsafe");
        requireContains(plan.lines().toString(), "blocks movement", "debug blocked tile report");
        requireContains(ZoneTileMovementResolutionAuthority.auditSummary(), "debug=routingTrace+failsafeReasons", "audit debug summary");
        requireContains(ZoneTileMovementResolutionAuthority.auditSummary(), "legacyGlyph=notMovementAuthority", "audit legacy glyph summary");
    }

    private static ZoneTileState[][] floorGrid(int w, int h) {
        ZoneTileState[][] tiles = new ZoneTileState[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) tiles[x][y] = ZoneTileState.fromLegacyGlyph('.');
        }
        return tiles;
    }

    private static void requireMovedTo(ZoneTileMovementResolutionAuthority.Resolution resolution, String actorId, int x, int y, String label) {
        ZoneTileMovementResolutionAuthority.MoveOutcome outcome = resolution.outcomeFor(actorId);
        require(outcome != null, label + " missing outcome");
        require(outcome.moved(), label + " should move: " + outcome);
        require(outcome.finalX() == x && outcome.finalY() == y, label + " expected " + x + "," + y + " but was " + outcome.finalX() + "," + outcome.finalY());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text == null || !text.contains(expected)) {
            throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
        }
    }

    private Milestone02ZoneTilePushSqueezeMovementSmoke() { }
}
