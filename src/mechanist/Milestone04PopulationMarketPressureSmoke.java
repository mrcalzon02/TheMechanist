package mechanist;

import java.util.List;

/** Smoke for population-ledger demand changing essential stock and live trade prices. */
final class Milestone04PopulationMarketPressureSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        World highPopulation = worldWithPopulation(120, 32, 2, true);
        TraderSession highTrader = specialistTrader();
        PopulationMarketPressureAuthority.Profile high = PopulationMarketPressureAuthority.apply(
                highTrader, highPopulation, Faction.MECHANICUS, 14);

        TradeOffer food = offerNamed(highTrader, "Emergency rations");
        require(food != null && offerNamed(highTrader, "Water bottle") != null
                        && offerNamed(highTrader, "Bandage roll") != null,
                "high-population specialist should receive food, water, and medical fallback stock");
        require(food.provenance != null && food.provenance.route.contains("essential local vendor stock"),
                "population-allocated stock should carry a readable source record");
        require(high.populationTarget() == 120 && high.assignedWorkers() == 32
                        && high.recordedDeaths() == 2 && high.facilityLinkedLedgers() == 1,
                "market profile should derive its context from population ledgers");

        World lowPopulation = worldWithPopulation(8, 1, 0, false);
        TraderSession lowTrader = specialistTrader();
        PopulationMarketPressureAuthority.apply(lowTrader, lowPopulation, Faction.MECHANICUS, 14);
        TradeOffer lowFood = offerNamed(lowTrader, "Emergency rations");
        require(lowFood != null, "minimum viable population should still allocate food stock");
        require(highTrader.buyPrice(food) > lowTrader.buyPrice(lowFood),
                "higher population demand should increase the same food offer's purchase price");
        require(highTrader.sellPrice("Emergency rations") > lowTrader.sellPrice("Emergency rations"),
                "higher population demand should improve the player's sale value for food");
        requireContains(high.offerLine(food), "severe shortage", "high-population pressure band");
        requireContains(high.offerLine(food), "buy +", "purchase adjustment readback");
        requireContains(high.contextLines().toString(), "target 120 people", "population context");

        RoomPopulationLedger parsed = RoomPopulationLedger.parse(highPopulation.roomPopulationLedgers.get(0).saveLine());
        require(parsed != null && parsed.capacity == 120 && parsed.assigned == 32 && parsed.dead == 2,
                "population inputs that drive market pressure should survive their persisted ledger format");

        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        try {
            game.world = highPopulation;
            NpcEntity traderNpc = new NpcEntity();
            traderNpc.name = "Forge Quartermaster";
            traderNpc.role = "Trader";
            traderNpc.faction = Faction.MECHANICUS;
            game.activeInteractionNpc = traderNpc;
            game.activeTraderSession = highTrader;
            game.panelMode = GamePanel.PanelMode.TRADE;
            game.screen = GamePanel.Screen.PANEL;
            game.selectedTradeOfferIndex = highTrader.offers.indexOf(food);
            game.carriedScript = 100;
            game.setSize(1280, 720);
            render(game);

            require(hasButton(game, "Buy") && hasButton(game, "Sell"),
                    "rendered trade panel should retain purchase and sale controls: " + buttonLabels(game));
            List<String> preview = TradeReadabilityAuthority.offerPreview(highTrader, food,
                    game.carriedScript, game.inventoryWeight(), game.carryCapacity());
            requireContains(preview.toString(), "Population pressure: Food", "selected-offer demand readback");
            List<String> context = TradeReadabilityAuthority.marketContext(highTrader,
                    Faction.MECHANICUS.label, 0);
            requireContains(context.toString(), "Population demand: target 120 people", "trade-panel population context");
            requireContains(context.toString(), "Population allocation added", "essential allocation supply context");

            int price = highTrader.buyPrice(food);
            int turnBefore = game.turn;
            runButton(game, "Buy");
            require(game.carriedScript == 100 - price,
                    "purchase should spend the exact population-adjusted displayed price");
            require(game.inventory.stream().anyMatch(item -> ItemQuality.namesMatch(item, "Emergency rations")),
                    "population-allocated food should enter carried inventory after purchase");
            require(game.turn == turnBefore + 1, "successful market purchase should spend one turn");
            requireContains(lastEventContaining(game, "Bought"), price + " script",
                    "adjusted purchase event readback");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static World worldWithPopulation(int capacity, int assigned, int dead, boolean facilityLinked) {
        World world = new World(84000L + capacity, 40, 40);
        world.zoneType = ZoneType.MECHANICUS_FORGE_CLOISTER;
        RoomPopulationLedger ledger = new RoomPopulationLedger();
        ledger.id = "pop.market.test";
        ledger.roomId = 0;
        ledger.roomName = "Market Habitation Block";
        ledger.faction = Faction.MECHANICUS;
        ledger.capacity = capacity;
        ledger.assigned = assigned;
        ledger.available = Math.max(0, capacity - assigned);
        ledger.dead = dead;
        if (facilityLinked) {
            ledger.facilityId = "local-market-hall";
            ledger.facilityPurpose = "resident provisioning";
        }
        world.roomPopulationLedgers.add(ledger);
        return world;
    }

    private static TraderSession specialistTrader() {
        TraderSession trader = new TraderSession();
        trader.name = "Forge Quartermaster";
        trader.archetype = "Mechanist Collegia specialist trader";
        trader.zoneLabel = "Forge Cloister";
        trader.offers.add(new TradeOffer("Machine part", "mechanical", 3,
                "specialist component stock"));
        return trader;
    }

    private static TradeOffer offerNamed(TraderSession trader, String wanted) {
        for (TradeOffer offer : trader.offers) {
            if (offer != null && ItemQuality.namesMatch(offer.name, wanted)) return offer;
        }
        return null;
    }

    private static void render(GamePanel game) {
        java.awt.image.BufferedImage canvas = new java.awt.image.BufferedImage(1280, 720,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
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
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        for (ButtonBox button : game.buttons) if (button != null) labels.add(button.label);
        return labels.toString();
    }

    private static String lastEventContaining(GamePanel game, String expected) {
        for (int i = game.eventLog.size() - 1; i >= 0; i--) {
            String line = game.eventLog.get(i);
            if (line != null && line.contains(expected)) return line;
        }
        return "";
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private Milestone04PopulationMarketPressureSmoke() { }
}
