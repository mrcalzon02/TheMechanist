package mechanist;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/** Smoke for Phase 12.4 normal Look readback and Map room-control actions. */
final class Milestone05RoomOwnershipUiSmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = ownershipWorld(12056L);
            Point vacant = pointInRoom(game.world, 0);
            Point rival = pointInRoom(game.world, 1);
            require(vacant != null && rival != null, "test rooms should have walkable interior points");
            game.playerX = vacant.x;
            game.playerY = vacant.y;
            game.world.roomFactions.set(0, Faction.NONE);
            game.world.roomFactions.set(1, Faction.ARBITES);
            game.baseClaimed = false;
            game.claimedRoomId = -1;
            game.roomOwnershipHistory.clear();

            String surface = joined(ProgressiveLookAuthority.tileStackAt(game, vacant.x, vacant.y, 0));
            requireContains(surface, "Vacant Machine Annex", "surface room identity");
            requireContains(surface, "Control: unowned", "surface room controller");
            requireContains(surface, "Access: abandoned and unowned", "surface room access category");
            requireNotContains(surface, "room 0", "internal room id");

            String examined = joined(ProgressiveLookAuthority.tileStackAt(game, vacant.x, vacant.y, 1));
            requireContains(examined, "Room use: a recognized machine room", "deeper room description");
            requireContains(examined, "Acquisition available: Claim Room", "claim guidance");

            openMapAndRender(game);
            require(hasButton(game, "Claim Room"), "Map should expose Claim Room: " + buttonLabels(game));
            int turnBeforeClaim = game.turn;
            runButton(game, "Claim Room");
            require(game.turn == turnBeforeClaim + 1, "successful Map claim should spend one turn");
            require(game.world.roomFaction(0) == Faction.HIVER, "Map claim should mutate authoritative room control");
            require(game.baseClaimed && game.claimedRoomId == 0, "Map claim should establish the current room as base");
            require(game.roomOwnershipHistory.size() == 1, "Map claim should append one ownership receipt");
            requireContains(game.lastTargetingReport, "Room claimed:", "Map claim result readback");

            String owned = joined(ProgressiveLookAuthority.tileStackAt(game, vacant.x, vacant.y, 2));
            requireContains(owned, "Access: player-owned claimed base", "claimed-room access category");
            requireContains(owned, "Latest room control change:", "claimed-room receipt readback");
            requireContains(owned, "abandonment claim", "claimed-room acquisition method");

            openMapAndRender(game);
            require(hasButton(game, "Abandon Room"), "Map should replace Claim with Abandon: " + buttonLabels(game));

            game.playerX = rival.x;
            game.playerY = rival.y;
            String rivalRead = joined(ProgressiveLookAuthority.tileStackAt(game, rival.x, rival.y, 1));
            requireContains(rivalRead, "Control: " + Faction.ARBITES.label, "rival controller readback");
            requireContains(rivalRead, "Room purchase quote:", "rival purchase quote");
            requireContains(rivalRead, "Other paths may include lease, faction grant, legal permit, or conquest", "rival acquisition paths");
            openMapAndRender(game);
            require(hasButton(game, "Buy Room"), "rival room should expose purchase/denial action");
            int turnBeforeBlocked = game.turn;
            int historyBeforeBlocked = game.roomOwnershipHistory.size();
            runButton(game, "Buy Room");
            require(game.turn == turnBeforeBlocked, "blocked rival control attempt should not spend time");
            require(game.world.roomFaction(1) == Faction.ARBITES && game.roomOwnershipHistory.size() == historyBeforeBlocked,
                    "blocked rival control attempt should not mutate ownership");
            requireContains(game.lastTargetingReport, "Room purchase blocked", "blocked rival action guidance");

            game.playerX = vacant.x;
            game.playerY = vacant.y;
            openMapAndRender(game);
            int turnBeforeAbandon = game.turn;
            runButton(game, "Abandon Room");
            require(game.turn == turnBeforeAbandon + 1, "successful Map abandonment should spend one turn");
            require(game.world.roomFaction(0) == Faction.NONE && !game.baseClaimed && game.claimedRoomId == -1,
                    "Map abandonment should release authoritative room and base control");
            require(game.roomOwnershipHistory.size() == 2, "Map abandonment should preserve claim and loss receipts");

            System.out.println("Milestone 05 room ownership UI smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static World ownershipWorld(long seed) {
        World world = new World(seed, 12, 8);
        for (int x = 0; x < world.w; x++) for (int y = 0; y < world.h; y++) world.tiles[x][y] = '#';
        addRoom(world, new java.awt.Rectangle(1, 1, 4, 5), "Vacant Machine Annex", "a recognized machine room");
        addRoom(world, new java.awt.Rectangle(7, 1, 4, 5), "Arbites Records Office", "a controlled civic records room");
        return world;
    }

    private static void addRoom(World world, java.awt.Rectangle room, String name, String description) {
        int roomId = world.rooms.size();
        world.rooms.add(room);
        world.roomProfiles.add(new RoomProfile(name, description, 20, Faction.NONE,
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

    private static void openMapAndRender(GamePanel game) {
        game.panelMode = GamePanel.PanelMode.MAP;
        game.screen = GamePanel.Screen.PANEL;
        game.setSize(1280, 720);
        BufferedImage canvas = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = canvas.createGraphics();
        game.paintComponent(graphics);
        graphics.dispose();
    }

    private static boolean hasButton(GamePanel game, String label) {
        for (ButtonBox button : game.buttons) if (button != null && label.equals(button.label)) return true;
        return false;
    }

    private static void runButton(GamePanel game, String label) {
        for (ButtonBox button : game.buttons) {
            if (button != null && label.equals(button.label) && button.action != null) {
                button.action.run();
                return;
            }
        }
        throw new AssertionError("Button not found: " + label + " / " + buttonLabels(game));
    }

    private static String buttonLabels(GamePanel game) {
        ArrayList<String> labels = new ArrayList<>();
        for (ButtonBox button : game.buttons) if (button != null) labels.add(button.label);
        return labels.toString();
    }

    private static String joined(java.util.List<String> lines) { return String.join(" | ", lines); }
    private static void requireContains(String text, String expected, String label) {
        require(text != null && text.contains(expected), label + " missing '" + expected + "': " + text);
    }
    private static void requireNotContains(String text, String expected, String label) {
        require(text == null || !text.contains(expected), label + " leaked '" + expected + "': " + text);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone05RoomOwnershipUiSmoke() { }
}
