package mechanist;

import java.awt.Point;
import java.util.Properties;

/** Smoke for Phase 12.4 live room claim/loss commands and durable world-state control. */
final class Milestone05RoomOwnershipCommandSmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        game.world = ownershipWorld(12054L);
        Point claimPoint = game.world.startPoint();
        game.playerX = claimPoint.x;
        game.playerY = claimPoint.y;
        int roomId = game.world.roomIdAt(game.playerX, game.playerY);
        require(roomId >= 0 && roomId < game.world.roomFactions.size(), "player should stand in a recognized room");
        game.world.roomFactions.set(roomId, Faction.NONE);
        game.baseClaimed = false;
        game.claimedRoomId = -1;
        game.roomOwnershipHistory.clear();

        require(GameplayConsoleCommandAuthority.isKnown("room_status"), "room status command should be registered");
        require(GameplayConsoleCommandAuthority.isKnown("room_claim"), "room claim command should be registered");
        require(GameplayConsoleCommandAuthority.isKnown("room_abandon"), "room abandon command should be registered");
        requireContains(GameplayConsoleCommandAuthority.help(new String[]{"room_claim"}),
                "unowned room you are standing in", "room claim help");
        requireContains(GameplayConsoleCommandAuthority.help(new String[]{"room_abandon"}),
                "while standing inside it", "room abandon help");

        int beforeUsage = game.turn;
        String usage = GameplayConsoleCommandAuthority.execute(game, null, "room_claim", new String[]{"extra"});
        requireContains(usage, "Usage: room_claim", "room claim argument rejection");
        require(game.turn == beforeUsage && game.world.roomFaction(roomId) == Faction.NONE,
                "invalid claim arguments should not mutate time or room control");

        String claimed = GameplayConsoleCommandAuthority.execute(game, null, "room_claim", new String[0]);
        requireContains(claimed, "Room claimed:", "successful claim readback");
        requireContains(claimed, "One turn spent", "successful claim time readback");
        require(game.turn == beforeUsage + 1, "successful claim should spend one turn");
        require(game.world.roomFaction(roomId) == Faction.HIVER, "claim should mutate the world room-faction ledger");
        require(game.baseClaimed && game.claimedRoomId == roomId, "claim should establish the room as the player's base");
        require(game.roomOwnershipHistory.size() == 1, "claim should append one durable ownership change");
        requireContains(game.baseDisplayName(), game.world.roomProfile(roomId).name, "claimed base display name");
        require(game.isInClaimedRoom(game.playerX, game.playerY), "player tile should be inside claimed room");
        Point differentRoom = pointInDifferentRoom(game, roomId);
        require(differentRoom != null && !game.isInClaimedRoom(differentRoom.x, differentRoom.y),
                "ledger-backed claim should use the exact generated room boundary");
        game.world.roomFactions.set(roomId, Faction.NONE);
        require(game.isInClaimedRoom(differentRoom.x, differentRoom.y),
                "legacy claimed pointers without matching room ownership should retain the coordinate fallback");
        game.world.roomFactions.set(roomId, Faction.HIVER);

        int beforeDuplicate = game.turn;
        String duplicate = GameplayConsoleCommandAuthority.execute(game, null, "room_claim", new String[0]);
        requireContains(duplicate, "already controlled", "duplicate claim rejection");
        require(game.turn == beforeDuplicate && game.roomOwnershipHistory.size() == 1,
                "blocked duplicate claim should not spend time or append history");

        Properties claimedSave = new Properties();
        Persistence.writeCore(game, claimedSave);
        GamePanel restored = new GamePanel();
        restored.shutdownRuntime();
        restored.world = ownershipWorld(12054L);
        restoreOwnershipState(restored, claimedSave);
        require(restored.baseClaimed && restored.claimedRoomId == roomId,
                "claimed base pointer should survive save/load");
        require(restored.world.roomFaction(roomId) == Faction.HIVER,
                "claimed world room owner should survive save/load");
        require(restored.roomOwnershipHistory.size() == 1,
                "room ownership history should survive save/load");
        requireContains(GameplayConsoleCommandAuthority.execute(restored, null, "room_status", new String[0]),
                "Latest control change:", "restored room status history");

        Point outside = pointInDifferentRoom(restored, roomId);
        if (outside != null) {
            restored.playerX = outside.x;
            restored.playerY = outside.y;
            int beforeRemoteAbandon = restored.turn;
            String remote = GameplayConsoleCommandAuthority.execute(restored, null, "room_abandon", new String[0]);
            requireContains(remote, "return to", "remote abandonment rejection");
            require(restored.turn == beforeRemoteAbandon && restored.world.roomFaction(roomId) == Faction.HIVER,
                    "remote abandonment should not mutate time or control");
        }

        Point returnPoint = pointInRoom(restored, roomId);
        require(returnPoint != null, "claimed room should remain reachable after restore");
        restored.playerX = returnPoint.x;
        restored.playerY = returnPoint.y;
        int beforeAbandon = restored.turn;
        String abandoned = GameplayConsoleCommandAuthority.execute(restored, null, "room_abandon", new String[0]);
        requireContains(abandoned, "Room abandoned:", "successful abandonment readback");
        requireContains(abandoned, "One turn spent", "successful abandonment time readback");
        require(restored.turn == beforeAbandon + 1, "successful abandonment should spend one turn");
        require(restored.world.roomFaction(roomId) == Faction.NONE, "abandonment should release world room control");
        require(!restored.baseClaimed && restored.claimedRoomId == -1, "abandonment should clear claimed-base state");
        require(restored.roomOwnershipHistory.size() == 2, "abandonment should preserve the acquisition and loss record");
        requireContains(RoomOwnershipAuthority.status(restored), "voluntary abandonment", "ownership loss readback");

        Properties abandonedSave = new Properties();
        Persistence.writeCore(restored, abandonedSave);
        GamePanel lossRestored = new GamePanel();
        lossRestored.shutdownRuntime();
        lossRestored.world = ownershipWorld(12054L);
        restoreOwnershipState(lossRestored, abandonedSave);
        require(!lossRestored.baseClaimed && lossRestored.world.roomFaction(roomId) == Faction.NONE,
                "released room control should survive save/load");
        require(lossRestored.roomOwnershipHistory.size() == 2,
                "acquisition and loss history should survive a second save/load");

        GamePanel legacy = new GamePanel();
        legacy.shutdownRuntime();
        legacy.world = ownershipWorld(12055L);
        legacy.world.roomFactions.set(0, Faction.HIVER);
        legacy.baseClaimed = true;
        legacy.claimedRoomId = 0;
        legacy.baseX = 1;
        legacy.baseY = 1;
        Point legacyNearby = pointInRoom(legacy, 1);
        require(legacyNearby != null && legacy.isInClaimedRoom(legacyNearby.x, legacyNearby.y),
                "pre-receipt claimed saves should retain coordinate behavior even when the room is faction-owned");

        lossRestored.shutdownRuntime();
        restored.shutdownRuntime();
        game.shutdownRuntime();
        legacy.shutdownRuntime();
        System.out.println("Milestone 05 room ownership command smoke passed.");
    }

    private static Point pointInDifferentRoom(GamePanel game, int excludedRoom) {
        for (int roomId = 0; roomId < game.world.rooms.size(); roomId++) {
            if (roomId == excludedRoom) continue;
            Point point = pointInRoom(game, roomId);
            if (point != null) return point;
        }
        return null;
    }

    private static World ownershipWorld(long seed) {
        World world = new World(seed, 12, 8);
        for (int x = 0; x < world.w; x++) for (int y = 0; y < world.h; y++) world.tiles[x][y] = '#';
        addRoom(world, new java.awt.Rectangle(1, 1, 4, 5), "Vacant Machine Annex");
        addRoom(world, new java.awt.Rectangle(7, 1, 4, 5), "Public Transit Waiting Room");
        return world;
    }

    private static void restoreOwnershipState(GamePanel game, Properties saved) {
        game.turn = Integer.parseInt(saved.getProperty("run.turn", "0"));
        game.baseClaimed = Boolean.parseBoolean(saved.getProperty("base.claimed", "false"));
        game.baseX = Integer.parseInt(saved.getProperty("base.x", "-1"));
        game.baseY = Integer.parseInt(saved.getProperty("base.y", "-1"));
        game.claimedRoomId = Integer.parseInt(saved.getProperty("base.room", "-1"));
        RoomOwnershipAuthority.restoreHistory(game,
                Persistence.decList(saved.getProperty("run.roomOwnershipHistory", "")));
        Persistence.readWorldState(game.world, saved);
    }

    private static void addRoom(World world, java.awt.Rectangle room, String name) {
        int roomId = world.rooms.size();
        world.rooms.add(room);
        world.roomProfiles.add(new RoomProfile(name, "a recognized test room", 20, Faction.NONE,
                new String[]{"machine scrap"}, new char[]{'b'}));
        world.roomFactions.add(Faction.NONE);
        world.roomSpecials.add(Boolean.FALSE);
        for (int x = room.x; x < room.x + room.width; x++) {
            for (int y = room.y; y < room.y + room.height; y++) {
                world.tiles[x][y] = '.';
                world.roomIds[x][y] = roomId;
            }
        }
    }

    private static Point pointInRoom(GamePanel game, int roomId) {
        java.awt.Rectangle room = game.world.roomRect(roomId);
        if (room == null) return null;
        for (int x = room.x; x < room.x + room.width; x++) {
            for (int y = room.y; y < room.y + room.height; y++) {
                if (game.world.inBounds(x, y) && game.world.walkable(x, y) && game.world.roomIdAt(x, y) == roomId) {
                    return new Point(x, y);
                }
            }
        }
        return null;
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone05RoomOwnershipCommandSmoke() { }
}
