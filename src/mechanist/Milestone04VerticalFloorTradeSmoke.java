package mechanist;

import java.util.Properties;

/** Gameplay smoke for floor-to-sewer exports and sewer import demand. */
final class Milestone04VerticalFloorTradeSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        World surface = world(95001L, false);
        TraderSession surfaceTrader = trader("Floor Four Provisioner");
        VerticalFloorTradeAuthority.apply(surfaceTrader, surface, Faction.HIVER, 200L, 200);
        TradeOffer fertilizer = offer(surfaceTrader, "Fertilizer");
        TradeOffer reagent = offer(surfaceTrader, "Chemical reagent bottle");
        require(fertilizer != null && reagent != null,
                "surface traders should receive finite fertilizer and basic reagent exports from the sewer below");
        requireContains(fertilizer.provenance.shortChain(), "sewer reclamation -> freight lift",
                "upward fertilizer route");
        requireContains(surfaceTrader.supplyChainSummary, "Floor 4 sewer below exports",
                "surface inter-floor readback");

        World sewer = world(95002L, true);
        TraderSession sewerTrader = trader("Floor Four Sump Exchange");
        VerticalFloorTradeAuthority.apply(sewerTrader, sewer, Faction.HIVER, 200L, 200);
        VerticalTradeReserveRecord sewerFertilizer = VerticalFloorTradeAuthority.reserveFor(
                sewer, Faction.HIVER, "Fertilizer");
        require(sewerFertilizer != null && sewerFertilizer.capacity == 10,
                "sewer processing should produce a larger local fertilizer reserve");
        requireContains(sewerFertilizer.sourceLabel, "runoff from the floor above",
                "downward waste-runoff source");
        requireContains(sewerTrader.supplyChainSummary, "Sewer import demand:",
                "sewer import demand readback");

        TraderSession ordinary = trader("Ordinary Counter");
        TradeOffer ration = new TradeOffer("Emergency rations", "food", 4, "ordinary ration stock.");
        require(sewerTrader.sellPrice("Emergency rations") > ordinary.sellPrice("Emergency rations"),
                "sewer traders should pay more for food brought down from above");
        require(sewerTrader.buyPrice(ration) > ordinary.buyPrice(ration),
                "scarce upstairs provisions should cost more on sewer shelves");

        VerticalTradeReserveRecord surfaceFertilizer = VerticalFloorTradeAuthority.reserveFor(
                surface, Faction.HIVER, "Fertilizer");
        surfaceFertilizer.remaining = 1;
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        try {
            game.world = surface;
            game.worldTurn = 200L;
            game.turn = 200;
            game.activeTraderSession = surfaceTrader;
            game.activeInteractionTitle = "Floor Four Provisioner";
            game.panelMode = GamePanel.PanelMode.TRADE;
            game.screen = GamePanel.Screen.PANEL;
            game.selectedTradeOfferIndex = surfaceTrader.offers.indexOf(fertilizer);
            game.carriedScript = 100;
            game.setSize(1280, 720);
            render(game);
            runButton(game, "Buy");
            require(surfaceFertilizer.remaining == 0,
                    "successful inter-floor purchase should consume exactly one persisted route unit; selected="
                            + (game.activeTraderSession == null ? "no trader" : game.selectedTradeOfferIndex)
                            + "; script=" + game.carriedScript + "; events=" + game.eventLog);
            require(game.inventory.stream().anyMatch(item -> ItemQuality.namesMatch(item, "Fertilizer")),
                    "purchased sewer fertilizer should reach carried inventory");
            require(offer(surfaceTrader, "Fertilizer") == null,
                    "depleted vertical trade goods should leave the active shelf");
        } finally {
            game.shutdownRuntime();
        }

        TraderSession reopened = trader("Reopened Provisioner");
        VerticalFloorTradeAuthority.apply(reopened, surface, Faction.HIVER, 201L, 201);
        require(offer(reopened, "Fertilizer") == null && offer(reopened, "Chemical reagent bottle") != null,
                "reopening the surface trader should preserve fertilizer depletion without hiding other exports");

        Properties saved = new Properties();
        Persistence.writeWorldState(surface, saved);
        World loaded = world(95001L, false);
        Persistence.readWorldState(loaded, saved);
        VerticalTradeReserveRecord loadedFertilizer = VerticalFloorTradeAuthority.reserveFor(
                loaded, Faction.HIVER, "Fertilizer");
        require(loadedFertilizer != null && loadedFertilizer.remaining == 0
                        && loadedFertilizer.route.contains("freight lift"),
                "vertical route identity and depletion should survive world save/load");

        System.out.println("Milestone 04 vertical floor trade smoke passed.");
    }

    private static World world(long seed, boolean sewer) {
        World world = new World(seed, 40, 40);
        world.floor = 4;
        world.sewerLayer = sewer;
        world.zoneType = sewer ? ZoneType.SEWER_CONDUIT : ZoneType.HAB_STACK;
        return world;
    }

    private static TraderSession trader(String name) {
        TraderSession trader = new TraderSession();
        trader.name = name;
        trader.archetype = "inter-floor trader";
        trader.zoneLabel = "Floor Four";
        return trader;
    }

    private static TradeOffer offer(TraderSession trader, String item) {
        for (TradeOffer offer : trader.offers) if (offer != null && ItemQuality.namesMatch(offer.name, item)) return offer;
        return null;
    }

    private static void render(GamePanel game) {
        java.awt.image.BufferedImage canvas = new java.awt.image.BufferedImage(1280, 720,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = canvas.createGraphics();
        game.paintComponent(graphics);
        graphics.dispose();
    }

    private static void runButton(GamePanel game, String label) {
        for (ButtonBox button : game.buttons) {
            if (button != null && label.equals(button.label) && button.action != null) {
                button.action.run();
                return;
            }
        }
        throw new AssertionError("Button not found: " + label);
    }

    private static void requireContains(String actual, String expected, String label) {
        require(actual != null && actual.contains(expected), label + " missing '" + expected + "': " + actual);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone04VerticalFloorTradeSmoke() { }
}
