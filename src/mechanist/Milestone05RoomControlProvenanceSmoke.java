package mechanist;

import java.awt.Point;

/** Smoke for Phase 16.1 live room origin, controller, blueprint, seller, and exception readback. */
final class Milestone05RoomControlProvenanceSmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = provenanceWorld(16010L);
            Point warehouse = pointInRoom(game.world, 0);
            Point plaza = pointInRoom(game.world, 1);
            require(warehouse != null && plaza != null, "provenance rooms should have open interior points");
            game.playerX = warehouse.x;
            game.playerY = warehouse.y;

            String warehouseRead = String.join(" | ",
                    ProgressiveLookAuthority.tileStackAt(game, warehouse.x, warehouse.y, 2));
            requireContains(warehouseRead, "Room: Component Warehouse", "room identity");
            requireContains(warehouseRead, "Control: Civic Wardens", "current controller");
            requireContains(warehouseRead, "Room origin: built to Mechanist Collegia standards", "different origin faction");
            requireContains(warehouseRead, "Construction plan: Storage Crate", "mapped construction plan");
            requireContains(warehouseRead, "available by default", "mapped public-plan availability");
            requireContains(warehouseRead, "Plan access: Storage Crate is public", "public plan readiness");
            requireNotContains(warehouseRead, "unmapped", "internal mapping sentinel");

            String plazaRead = String.join(" | ",
                    ProgressiveLookAuthority.tileStackAt(game, plaza.x, plaza.y, 2));
            requireContains(plazaRead, "Room origin: built to Civic Wardens standards", "civic origin");
            requireContains(plazaRead, "Construction plan: no ordinary blueprint is offered", "non-acquirable plan readback");
            requireContains(plazaRead, "Non-acquirable exception", "non-acquirable reason");
            requireNotContains(plazaRead, "future room-to-blueprint", "development-process exception prose");

            System.out.println("Milestone 05 room control provenance smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static World provenanceWorld(long seed) {
        World world = new World(seed, 14, 8);
        for (int x = 0; x < world.w; x++) for (int y = 0; y < world.h; y++) world.tiles[x][y] = '#';
        addRoom(world, new java.awt.Rectangle(1, 1, 4, 5),
                new RoomProfile("Component Warehouse", "an industrial component warehouse with controlled storage",
                        30, Faction.MECHANIST_COLLEGIA, new String[]{"machine parts"}, new char[]{'b'}),
                Faction.CIVIC_WARDENS);
        addRoom(world, new java.awt.Rectangle(8, 1, 4, 5),
                new RoomProfile("Civic Transit Plaza", "a public transition plaza connecting local corridors",
                        5, Faction.CIVIC_WARDENS, new String[]{"paper scrap"}, new char[]{'q'}),
                Faction.CIVIC_WARDENS);
        return world;
    }

    private static void addRoom(World world, java.awt.Rectangle room, RoomProfile profile, Faction controller) {
        int roomId = world.rooms.size();
        world.rooms.add(room);
        world.roomProfiles.add(profile);
        world.roomFactions.add(controller);
        world.roomSpecials.add(Boolean.FALSE);
        for (int x = room.x; x < room.x + room.width; x++) {
            for (int y = room.y; y < room.y + room.height; y++) {
                world.tiles[x][y] = '.';
                world.roomIds[x][y] = roomId;
            }
        }
    }

    private static Point pointInRoom(World world, int roomId) {
        java.awt.Rectangle room = world.roomRect(roomId);
        if (room == null) return null;
        for (int x = room.x; x < room.x + room.width; x++) {
            for (int y = room.y; y < room.y + room.height; y++) {
                if (world.inBounds(x, y) && world.walkable(x, y) && world.roomIdAt(x, y) == roomId) return new Point(x, y);
            }
        }
        return null;
    }

    private static void requireContains(String text, String expected, String label) {
        require(text != null && text.contains(expected), label + " missing '" + expected + "': " + text);
    }
    private static void requireNotContains(String text, String expected, String label) {
        require(text == null || !text.contains(expected), label + " leaked '" + expected + "': " + text);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone05RoomControlProvenanceSmoke() { }
}
