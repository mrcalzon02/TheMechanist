package mechanist;

import java.awt.Rectangle;

/** Smoke for live construction self-entombment placement guards. */
final class Milestone03SelfEntombmentRuntimeGuardSmoke {
    public static void main(String[] args) {
        GamePanel game = linearClaimedRoom();
        BuildRecipe blocker = new BuildRecipe("Access Blocking Wall", 'W', 0, 0, 0, 0, 0, 1, 0, false,
                Faction.NONE, null, "test fixture for access-blocking construction");
        game.pendingBuildRecipe = blocker;

        requireContains(game.rawCanPlacePendingBuildAt(game.playerX, game.playerY),
                "move off the target tile", "player tile guard");

        NpcEntity npc = new NpcEntity();
        npc.name = "Guarded Tile Fixture";
        npc.x = 1;
        npc.y = 2;
        game.world.npcs.add(npc);
        requireContains(game.rawCanPlacePendingBuildAt(1, 2),
                "occupied by an NPC", "npc tile guard");

        String blocked = game.rawCanPlacePendingBuildAt(3, 2);
        requireContains(blocked, "No-self-entombment rule", "claimed-room exit guard");
        requireContains(blocked, "no valid access path", "access path explanation");
        requireContains(ActionDenialGuidanceAuthority.explain(ActionDenialGuidanceAuthority.DenialKind.CONSTRUCTION, blocked),
                "Move it away from the only route", "guided denial text");

        String open = game.rawCanPlacePendingBuildAt(1, 2);
        requireContains(open, "occupied by an NPC", "npc still blocks until moved");
        game.world.npcs.clear();
        require("ok".equalsIgnoreCase(game.rawCanPlacePendingBuildAt(1, 2)), "non-exit-side placement should remain valid");

        String summary = SelfEntombmentConstructionAuthority.auditSummary();
        requireContains(summary, "runtimeGuards=player-tile+npc-tile+claimed-room-exit", "runtime guard audit");
        requireContains(summary, "placement-only", "placement-only audit");
        rejectLeaks(blocked);
        rejectLeaks(summary);
    }

    private static GamePanel linearClaimedRoom() {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.world = new World(101L, 8, 5);
        for (int x = 0; x < game.world.w; x++) {
            for (int y = 0; y < game.world.h; y++) {
                game.world.tiles[x][y] = '#';
                game.world.roomIds[x][y] = -1;
            }
        }
        for (int x = 1; x <= 5; x++) {
            game.world.tiles[x][2] = x == 5 ? '/' : '.';
            game.world.roomIds[x][2] = 0;
        }
        game.world.rooms.add(new Rectangle(1, 2, 5, 1));
        game.baseClaimed = true;
        game.claimedRoomId = 0;
        game.baseX = 2;
        game.baseY = 2;
        game.playerX = 2;
        game.playerY = 2;
        game.supplies = 99;
        game.machineParts = 99;
        game.baseObjects.clear();
        return game;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.toLowerCase().contains(expected.toLowerCase())) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void rejectLeaks(String line) {
        if (PlayerFacingText.containsLikelyLeak(line)) {
            throw new AssertionError("Self-entombment guard leaked implementation text: " + line);
        }
    }

    private Milestone03SelfEntombmentRuntimeGuardSmoke() { }
}
