package mechanist;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

/** Smoke for Phase 16.2 room-plan seller reachability and normal Look navigation. */
final class Milestone05RoomPlanSellerGuidanceSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = guidanceWorld(16020L);
            Point security = pointInRoom(game.world, 0);
            Point market = pointInRoom(game.world, 1);
            require(security != null && market != null, "seller-guidance rooms should have open points");
            game.playerX = security.x;
            game.playerY = security.y;

            BuildRecipe recipe = RoomConstructionParityAuthority.liveMatchingRecipe(game.world.roomProfile(0));
            require(recipe != null && "Security Sensor Mast".equals(recipe.name),
                    "security room should map to the licensed sensor blueprint");
            NpcEntity seller = NpcEntity.create(Faction.CIVIC_WARDENS, game.world.zoneType,
                    market.x, market.y, new Random(6));
            seller.name = "Quartermaster Vey";
            seller.role = FactionCriticalVendorPlacementAuthority.Category.ARMORY.role;
            seller.state = "Trade";
            seller.hp = 12;
            NpcEntity wrongCategory = NpcEntity.create(Faction.CIVIC_WARDENS, game.world.zoneType,
                    security.x + 1, security.y, new Random(4));
            wrongCategory.name = "Closer Civic Medicae";
            wrongCategory.role = FactionCriticalVendorPlacementAuthority.Category.MEDICAL.role;
            wrongCategory.state = "Trade";
            wrongCategory.hp = 12;
            NpcEntity deadArmory = NpcEntity.create(Faction.ARBITES, game.world.zoneType,
                    security.x + 2, security.y, new Random(5));
            deadArmory.name = "Fallen Armory Clerk";
            deadArmory.role = FactionCriticalVendorPlacementAuthority.Category.ARMORY.role;
            deadArmory.state = "Trade";
            deadArmory.hp = 0;
            game.world.npcs.add(wrongCategory);
            game.world.npcs.add(deadArmory);
            game.world.npcs.add(seller);

            RoomBlueprintVendorGuidanceAuthority.Guidance guidance =
                    RoomBlueprintVendorGuidanceAuthority.forRoom(game, 0);
            require(guidance.locatable() && guidance.seller() == seller,
                    "licensed room plan should locate its living same-family vendor");
            String guidanceText = String.join(" | ", guidance.lines());
            requireContains(guidanceText, "Plan seller: Quartermaster Vey", "seller identity");
            requireContains(guidanceText, "Armory Trader", "seller role");
            requireContains(guidanceText, "tiles east", "seller direction and distance");
            requireContains(guidanceText, "Security Sensor Mast licensed blueprint", "exact blueprint folio");
            requireContains(guidanceText, "catalog price", "catalog price qualification");

            TraderSession stock = TraderTradeActionAuthority.createSessionForNpc(seller, game.world.zoneType, new Random(7));
            String blueprintId = ConstructionBlueprintOwnershipAuthority.blueprintId(recipe);
            require(stock.offers.stream().anyMatch(offer -> offer != null && blueprintId.equals(offer.constructionBlueprintId)),
                    "located physical vendor should actually stock the mapped licensed plan");

            String look = String.join(" | ", ProgressiveLookAuthority.tileStackAt(game, security.x, security.y, 2));
            requireContains(look, "Construction plan: Security Sensor Mast", "Look mapped plan");
            requireContains(look, "Plan seller: Quartermaster Vey", "Look live seller guidance");

            openLookAndRender(game, security.x, security.y);
            require(hasButton(game, "Locate Seller"),
                    "Look should expose Locate Seller for a live plan vendor: " + buttonLabels(game));
            int turnBeforeLocate = game.turn;
            runButton(game, "Locate Seller");
            require(game.turn == turnBeforeLocate, "locating a seller should not spend a turn");
            require(game.panelMode == GamePanel.PanelMode.MAP && game.screen == GamePanel.Screen.PANEL,
                    "Locate Seller should open the normal Map panel");
            QuestObjectiveGuidanceAuthority.ObjectiveGuidance marker = planSellerMarker(game);
            require(marker != null && marker.kind() == QuestObjectiveGuidanceAuthority.GuidanceKind.EXACT
                            && marker.targetX() == seller.x && marker.targetY() == seller.y,
                    "Locate Seller should create an exact marker at the physical vendor");
            requireContains(marker.detail(), "Walkable interaction route confirmed", "reachable route marker");
            requireContains(game.lastTargetingReport, "Plan seller marked on Map: Quartermaster Vey", "locate result readback");

            openLookAndRender(game, security.x, security.y);
            runButton(game, "Locate Seller");
            require(planSellerMarkerCount(game) == 1, "repeated seller location should replace its prior marker");

            game.world.tiles[8][3] = '#';
            openLookAndRender(game, security.x, security.y);
            runButton(game, "Locate Seller");
            marker = planSellerMarker(game);
            require(marker != null, "blocked seller should retain an exact known-location marker");
            requireContains(marker.detail(), "no currently walkable route", "blocked route marker");

            game.playerX = seller.x + 1;
            game.playerY = seller.y;
            game.lookX = seller.x;
            game.lookY = seller.y;
            game.panelMode = GamePanel.PanelMode.INTERACT;
            game.screen = GamePanel.Screen.PANEL;
            game.confirmInteraction();
            require(game.panelMode == GamePanel.PanelMode.TRADE && game.activeTraderSession != null,
                    "ordinary adjacent Interact should open the marked specialist's Trade panel");
            require(game.activeTraderSession.offers.stream().anyMatch(offer -> offer != null
                            && blueprintId.equals(offer.constructionBlueprintId)),
                    "the marked specialist's live Trade panel should expose the requested licensed folio");

            game.world.npcs.clear();
            String unavailable = String.join(" | ", RoomBlueprintVendorGuidanceAuthority.forRoom(game, 0).lines());
            requireContains(unavailable, "Plan seller unavailable here", "missing live vendor readback");
            requireContains(unavailable, "no living Civic Wardens Armory Trader", "missing vendor requirement");
            openLookAndRender(game, security.x, security.y);
            require(!hasButton(game, "Locate Seller"), "missing vendor should remove Locate Seller action");

            game.unlockedConstructionBlueprints.add(blueprintId);
            RoomBlueprintVendorGuidanceAuthority.Guidance owned =
                    RoomBlueprintVendorGuidanceAuthority.forRoom(game, 0);
            require(!owned.locatable(), "owned plan should no longer need seller navigation");
            requireContains(String.join(" | ", owned.lines()), "already recorded in your construction library",
                    "owned plan readiness");

            System.out.println("Milestone 05 room plan seller guidance smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static World guidanceWorld(long seed) {
        World world = new World(seed, 18, 9);
        world.npcs.clear();
        for (int x = 0; x < world.w; x++) for (int y = 0; y < world.h; y++) world.tiles[x][y] = '#';
        addRoom(world, new java.awt.Rectangle(1, 1, 5, 6),
                new RoomProfile("Security Annex", "a controlled alarm and checkpoint room",
                        30, Faction.ARBITES, new String[]{"wire bundle"}, new char[]{'N'}), Faction.ARBITES);
        addRoom(world, new java.awt.Rectangle(11, 1, 5, 6),
                new RoomProfile("Civic Armory Market", "a licensed security-goods counter",
                        20, Faction.CIVIC_WARDENS, new String[]{"trade chit"}, new char[]{'T'}), Faction.CIVIC_WARDENS);
        for (int x = 6; x <= 10; x++) world.tiles[x][3] = '.';
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

    private static void openLookAndRender(GamePanel game, int x, int y) {
        game.lookCursorActive = true;
        game.lookX = x;
        game.lookY = y;
        game.lookFocusX = x;
        game.lookFocusY = y;
        game.lookFocusDepth = 2;
        game.panelMode = GamePanel.PanelMode.LOOK;
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

    private static QuestObjectiveGuidanceAuthority.ObjectiveGuidance planSellerMarker(GamePanel game) {
        for (QuestObjectiveGuidanceAuthority.ObjectiveGuidance objective : game.activeQuestGuidance) {
            if (objective != null && RoomBlueprintVendorGuidanceAuthority.PLAN_SELLER_OBJECTIVE_LABEL.equals(objective.label())) {
                return objective;
            }
        }
        return null;
    }

    private static int planSellerMarkerCount(GamePanel game) {
        int count = 0;
        for (QuestObjectiveGuidanceAuthority.ObjectiveGuidance objective : game.activeQuestGuidance) {
            if (objective != null && RoomBlueprintVendorGuidanceAuthority.PLAN_SELLER_OBJECTIVE_LABEL.equals(objective.label())) count++;
        }
        return count;
    }

    private static void requireContains(String text, String expected, String label) {
        require(text != null && text.contains(expected), label + " missing '" + expected + "': " + text);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone05RoomPlanSellerGuidanceSmoke() { }
}
