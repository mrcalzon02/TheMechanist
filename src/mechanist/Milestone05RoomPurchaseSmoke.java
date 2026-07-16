package mechanist;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

/** Smoke for Phase 12.4 permanent room purchase, denial atomicity, UI action, and persistence. */
final class Milestone05RoomPurchaseSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = purchaseWorld(12057L);
            Point office = pointInRoom(game.world, 0);
            Point lobby = pointInRoom(game.world, 1);
            require(office != null && lobby != null, "purchase test rooms should have open interior points");
            game.playerX = office.x;
            game.playerY = office.y;
            game.turn = 30;
            game.worldTurn = 30L;
            game.carriedScript = 0;
            game.baseClaimed = false;
            game.claimedRoomId = -1;
            game.roomOwnershipHistory.clear();
            game.world.npcs.clear();

            require(GameplayConsoleCommandAuthority.isKnown("room_buy"), "room buy command should be registered");
            requireContains(GameplayConsoleCommandAuthority.help(new String[]{"room_buy"}),
                    "quoted price and authorization requirements", "room buy help");
            RoomOwnershipAuthority.PurchaseQuote initial = RoomOwnershipAuthority.purchaseQuote(game);
            require(initial.price() == 60 && initial.standingRequired() == 5,
                    "4x5 ordinary room should quote 60 script and standing 5");
            requireContains(initial.readback(), "no living Civic Wardens representative", "missing representative blocker");
            requireContains(initial.readback(), "standing is 0/5", "missing standing blocker");
            requireContains(initial.readback(), "need 60 script, have 0", "missing funds blocker");

            int untouchedTurn = game.turn;
            String usage = GameplayConsoleCommandAuthority.execute(game, null, "room_buy", new String[]{"extra"});
            requireContains(usage, "Usage: room_buy", "room buy argument rejection");
            requireUnchanged(game, untouchedTurn, 0, Faction.ARBITES, "invalid room buy arguments");

            String initiallyBlocked = GameplayConsoleCommandAuthority.execute(game, null, "room_buy", new String[0]);
            requireContains(initiallyBlocked, "Room purchase blocked", "initial purchase denial");
            requireContains(initiallyBlocked, "no living Civic Wardens representative", "aggregate purchase denial");
            requireContains(initiallyBlocked, "standing is 0/5", "aggregate standing denial");
            requireContains(initiallyBlocked, "need 60 script, have 0", "aggregate funds denial");
            requireUnchanged(game, untouchedTurn, 0, Faction.ARBITES, "blocked aggregate purchase");

            NpcEntity representative = NpcEntity.factionRepresentative(Faction.CIVIC_WARDENS,
                    lobby.x, lobby.y, new Random(4));
            game.world.npcs.add(representative);
            game.factionStanding.put(Faction.CIVIC_WARDENS, 4);
            game.carriedScript = 59;
            RoomOwnershipAuthority.PurchaseQuote almost = RoomOwnershipAuthority.purchaseQuote(game);
            requireNotContains(almost.readback(), "no living", "same-family representative authorization");
            requireContains(almost.readback(), "standing is 4/5", "almost-ready standing blocker");
            requireContains(almost.readback(), "need 60 script, have 59", "almost-ready funds blocker");

            game.factionStanding.put(Faction.CIVIC_WARDENS, 5);
            game.carriedScript = 60;
            game.temporaryHostileTurns.put(Faction.ARBITES, 40);
            String hostility = GameplayConsoleCommandAuthority.execute(game, null, "room_buy", new String[0]);
            requireContains(hostility, "active hostility continues until turn 40", "same-family hostility blocker");
            requireUnchanged(game, untouchedTurn, 60, Faction.ARBITES, "hostile purchase denial");
            game.temporaryHostileTurns.put(Faction.ARBITES, 0);

            game.world.roomSpecials.set(0, Boolean.TRUE);
            String special = GameplayConsoleCommandAuthority.execute(game, null, "room_buy", new String[0]);
            requireContains(special, "protected or special-purpose room", "special room sale blocker");
            requireUnchanged(game, untouchedTurn, 60, Faction.ARBITES, "special-room purchase denial");
            game.world.roomSpecials.set(0, Boolean.FALSE);

            NpcEntity occupant = NpcEntity.create(Faction.ARBITES, game.world.zoneType,
                    office.x + 1, office.y, new Random(5));
            occupant.name = "Records Clerk";
            game.world.npcs.add(occupant);
            String occupied = GameplayConsoleCommandAuthority.execute(game, null, "room_buy", new String[0]);
            requireContains(occupied, "1 living occupant", "occupied room sale blocker");
            requireUnchanged(game, untouchedTurn, 60, Faction.ARBITES, "occupied-room purchase denial");
            game.world.npcs.remove(occupant);

            game.baseClaimed = true;
            game.claimedRoomId = 2;
            game.baseX = pointInRoom(game.world, 2).x;
            game.baseY = pointInRoom(game.world, 2).y;
            game.world.roomFactions.set(2, Faction.HIVER);
            String secondBase = GameplayConsoleCommandAuthority.execute(game, null, "room_buy", new String[0]);
            requireContains(secondBase, "before buying another base", "single-base purchase blocker");
            require(game.turn == untouchedTurn && game.carriedScript == 60
                            && game.world.roomFaction(0) == Faction.ARBITES
                            && game.baseClaimed && game.claimedRoomId == 2 && game.roomOwnershipHistory.isEmpty(),
                    "second-base purchase denial should preserve time, script, both room owners, base state, and history");
            game.baseClaimed = false;
            game.claimedRoomId = -1;
            game.baseX = -1;
            game.baseY = -1;
            game.world.roomFactions.set(2, Faction.NONE);

            RoomOwnershipAuthority.PurchaseQuote ready = RoomOwnershipAuthority.purchaseQuote(game);
            require(ready.ready(), "valid purchase should have no blockers: " + ready.readback());
            requireContains(ready.readback(), "Ready: Buy Room", "ready purchase preview");
            String look = String.join(" | ", ProgressiveLookAuthority.tileStackAt(game, office.x, office.y, 1));
            requireContains(look, "Room purchase quote:", "Look purchase preview");
            requireContains(look, "60 script", "Look exact purchase price");

            openMapAndRender(game);
            require(hasButton(game, "Buy Room"), "Map should expose Buy Room: " + buttonLabels(game));
            int purchaseTurn = game.turn;
            runButton(game, "Buy Room");
            require(game.turn == purchaseTurn + 1, "successful room purchase should spend one turn");
            require(game.carriedScript == 0, "successful room purchase should spend the exact quote once");
            require(game.world.roomFaction(0) == Faction.HIVER, "purchase should transfer the world room ledger");
            require(game.baseClaimed && game.claimedRoomId == 0, "purchase should establish the bought room as base");
            require(game.world.roomProfile(0).faction == Faction.ARBITES,
                    "purchase should preserve the room profile's builder/origin faction");
            require(game.roomOwnershipHistory.size() == 1, "purchase should append one durable transfer receipt");
            requireContains(game.roomOwnershipHistory.peekLast().method(),
                    "purchase from Civic Wardens for 60 script", "purchase receipt method and price");
            requireContains(game.lastTargetingReport, "Room purchased:", "Map purchase result readback");

            Properties saved = new Properties();
            Persistence.writeCore(game, saved);
            GamePanel restored = new GamePanel();
            restored.shutdownRuntime();
            try {
                restored.world = purchaseWorld(12057L);
                restored.turn = Integer.parseInt(saved.getProperty("run.turn", "0"));
                restored.carriedScript = Integer.parseInt(saved.getProperty("script.carried", "0"));
                restored.baseClaimed = Boolean.parseBoolean(saved.getProperty("base.claimed", "false"));
                restored.baseX = Integer.parseInt(saved.getProperty("base.x", "-1"));
                restored.baseY = Integer.parseInt(saved.getProperty("base.y", "-1"));
                restored.claimedRoomId = Integer.parseInt(saved.getProperty("base.room", "-1"));
                RoomOwnershipAuthority.restoreHistory(restored,
                        Persistence.decList(saved.getProperty("run.roomOwnershipHistory", "")));
                Persistence.readWorldState(restored.world, saved);
                require(restored.carriedScript == 0 && restored.baseClaimed && restored.claimedRoomId == 0,
                        "paid currency and bought-base pointer should persist");
                require(restored.world.roomFaction(0) == Faction.HIVER,
                        "purchased world room control should persist");
                require(restored.roomOwnershipHistory.size() == 1,
                        "purchase receipt should persist");
                requireContains(RoomOwnershipAuthority.status(restored), "purchase from Civic Wardens for 60 script",
                        "restored purchase method and price readback");
            } finally {
                restored.shutdownRuntime();
            }

            System.out.println("Milestone 05 room purchase smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static World purchaseWorld(long seed) {
        World world = new World(seed, 20, 8);
        world.npcs.clear();
        for (int x = 0; x < world.w; x++) for (int y = 0; y < world.h; y++) world.tiles[x][y] = '#';
        addRoom(world, new java.awt.Rectangle(1, 1, 4, 5), "Civic Records Annex",
                "a vacant records office offered through local civic administration", Faction.ARBITES);
        addRoom(world, new java.awt.Rectangle(7, 1, 4, 5), "Civic Service Lobby",
                "a public representative lobby", Faction.CIVIC_WARDENS);
        addRoom(world, new java.awt.Rectangle(13, 1, 4, 5), "Vacant Hab Cell",
                "an unowned hab cell", Faction.NONE);
        world.roomFactions.set(0, Faction.ARBITES);
        world.roomFactions.set(1, Faction.CIVIC_WARDENS);
        world.roomFactions.set(2, Faction.NONE);
        return world;
    }

    private static void addRoom(World world, java.awt.Rectangle room, String name, String descriptor, Faction origin) {
        int roomId = world.rooms.size();
        world.rooms.add(room);
        world.roomProfiles.add(new RoomProfile(name, descriptor, 20, origin,
                new String[]{"paper scrap"}, new char[]{'b'}));
        world.roomFactions.add(origin == null ? Faction.NONE : origin);
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

    private static void requireUnchanged(GamePanel game, int turn, int script, Faction owner, String label) {
        require(game.turn == turn && game.carriedScript == script && game.world.roomFaction(0) == owner
                        && !game.baseClaimed && game.claimedRoomId == -1 && game.roomOwnershipHistory.isEmpty(),
                label + " should preserve time, script, room owner, base state, and history");
    }

    private static void requireContains(String text, String expected, String label) {
        require(text != null && text.contains(expected), label + " missing '" + expected + "': " + text);
    }
    private static void requireNotContains(String text, String expected, String label) {
        require(text == null || !text.contains(expected), label + " still contains '" + expected + "': " + text);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone05RoomPurchaseSmoke() { }
}
