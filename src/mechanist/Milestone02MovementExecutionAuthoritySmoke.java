package mechanist;

/** Smoke for unified movement execution authority without Swing runtime mutation. */
final class Milestone02MovementExecutionAuthoritySmoke {
    public static void main(String[] args) {
        testOpenTileResolution();
        testOccupiedTileRequiresResolver();
        testPushSqueezeResolution();
        testBlockedTileRejection();
        requireContains(MovementExecutionAuthority.auditSummary(), "chain=planning+actorLayerResolver+singleCommit", "execution audit chain");
        requireContains(MovementExecutionAuthority.auditSummary(), "applies=player+npcActorOutcomes", "execution audit application");
        requireContains(MovementExecutionAuthority.auditSummary(), "paths=keyboard+mouse+manual+controller+queued+scripted", "execution path coverage");
        requireContains(MovementExecutionAuditAuthority.milestoneSummary(), "actor-layer-runtime-routing=active", "runtime routing audit");
        for (MovementExecutionAuditAuthority.MovementRouteAudit audit : MovementExecutionAuditAuthority.currentAuditSnapshot()) {
            if (audit.channel() != MovementExecutionAuditAuthority.MovementChannel.RECOVERY) {
                require(audit.usesActorLayerResolution(), "runtime channel should use actor-layer resolution: " + audit);
            }
        }
    }

    private static void testOpenTileResolution() {
        ZoneTileState[][] tiles = floorGrid(3, 1);
        tiles[0][0].setOccupantEntityId(MovementExecutionAuthority.PLAYER_ACTOR_ID);
        MovementExecutionAuthority.MovementExecutionResult result = MovementExecutionAuthority.resolveActorStepOnTiles(
                tiles, MovementExecutionAuthority.PLAYER_ACTOR_ID, 0, 0, 1, 0, true);
        require(result.success(), "open movement should succeed: " + result);
        require(!result.applied(), "tile-only resolution should not mutate runtime state");
        require(result.toX() == 1 && result.toY() == 0, "open movement should target 1,0: " + result);
    }

    private static void testOccupiedTileRequiresResolver() {
        ZoneTileState[][] tiles = floorGrid(3, 1);
        tiles[0][0].setOccupantEntityId(MovementExecutionAuthority.PLAYER_ACTOR_ID);
        tiles[1][0].setOccupantEntityId("npc-0");
        MovementExecutionAuthority.MovementExecutionResult result = MovementExecutionAuthority.resolveActorStepOnTiles(
                tiles, MovementExecutionAuthority.PLAYER_ACTOR_ID, 0, 0, 1, 0, false);
        require(!result.success(), "occupied movement should fail without actor resolver");
        requireContains(result.reason(), "no actor-layer push/squeeze resolver", "occupied no-resolver reason");
    }

    private static void testPushSqueezeResolution() {
        ZoneTileState[][] tiles = floorGrid(3, 1);
        tiles[0][0].setOccupantEntityId(MovementExecutionAuthority.PLAYER_ACTOR_ID);
        tiles[1][0].setOccupantEntityId("npc-0");
        MovementExecutionAuthority.MovementExecutionResult result = MovementExecutionAuthority.resolveActorStepOnTiles(
                tiles, MovementExecutionAuthority.PLAYER_ACTOR_ID, 0, 0, 1, 0, true);
        require(result.success(), "occupied movement should resolve with actor resolver: " + result);
        require(result.toX() == 1 && result.toY() == 0, "player should move into requested tile after push/squeeze: " + result);
        requireContains(result.debugSummary(), "actor-layer push/squeeze", "push/squeeze debug summary");
    }

    private static void testBlockedTileRejection() {
        ZoneTileState[][] tiles = floorGrid(2, 1);
        tiles[0][0].setOccupantEntityId(MovementExecutionAuthority.PLAYER_ACTOR_ID);
        tiles[1][0].addFlag(ZoneTileState.TileFlag.BLOCKS_MOVEMENT);
        MovementExecutionAuthority.MovementExecutionResult result = MovementExecutionAuthority.resolveActorStepOnTiles(
                tiles, MovementExecutionAuthority.PLAYER_ACTOR_ID, 0, 0, 1, 0, true);
        require(!result.success(), "blocked tile should reject movement");
        requireContains(result.reason(), "not walkable", "blocked tile reason");
    }

    private static ZoneTileState[][] floorGrid(int w, int h) {
        ZoneTileState[][] tiles = new ZoneTileState[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) tiles[x][y] = ZoneTileState.fromLegacyGlyph('.');
        }
        return tiles;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text == null || !text.contains(expected)) {
            throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
        }
    }

    private Milestone02MovementExecutionAuthoritySmoke() { }
}
