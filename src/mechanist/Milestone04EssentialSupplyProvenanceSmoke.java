package mechanist;

import java.util.Properties;

/** End-to-end smoke for finite population food/water reserves and source provenance. */
final class Milestone04EssentialSupplyProvenanceSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        World local = populatedWorld(94001L, ZoneType.HAB_STACK);
        local.zoneProductionHistory = "P0: facility=rival-hydroponics :: purpose=hydroponics and water recycler"
                + " :: controller=BANDIT :: focus=food and potable water :: cadence=daily :: batches=9"
                + " :: retained=4 :: samples=Emergency rations, Water bottle :: rival-controlled output;;"
                + "P1: facility=hydroponics-17 :: purpose=hydroponics and water recycler"
                + " :: controller=HIVER :: focus=food and potable water :: cadence=daily :: batches=3"
                + " :: retained=2 :: samples=Emergency rations, Water bottle :: local provisioning output";
        TraderSession localTrader = basicTrader();
        PopulationMarketPressureAuthority.apply(localTrader, local, Faction.HIVER, 100L, 100);

        TradeOffer food = offer(localTrader, "Emergency rations");
        TradeOffer water = offer(localTrader, "Water bottle");
        EssentialSupplyReserveRecord foodReserve = EssentialSupplyProvenanceAuthority.reserveFor(local, Faction.HIVER, "food");
        EssentialSupplyReserveRecord waterReserve = EssentialSupplyProvenanceAuthority.reserveFor(local, Faction.HIVER, "water");
        require(food != null && water != null && foodReserve != null && waterReserve != null,
                "population provisioning should create food and water reserves");
        require("faction production site".equals(foodReserve.sourceKind)
                        && "hydroponics-17".equals(foodReserve.sourceLabel),
                "food should resolve to the named hydroponics production ledger");
        require("hydroponics-17".equals(waterReserve.sourceLabel),
                "water should resolve to the named recycler production ledger");
        require(food.essentialSupplyReserveId.equals(foodReserve.id)
                        && food.provenance.shortChain().contains("hydroponics-17")
                        && food.provenance.route.contains("essential local vendor stock"),
                "food offer should bind finite stock and readable source provenance");
        requireContains(localTrader.supplyChainSummary, "Food supply:", "food reserve readback");
        requireContains(localTrader.supplyChainSummary, "Water supply:", "water reserve readback");

        foodReserve.remaining = 1;
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        try {
            game.world = local;
            game.worldTurn = 100L;
            game.turn = 100;
            game.activeTraderSession = localTrader;
            game.activeInteractionTitle = "Hydroponics Provisioner";
            game.panelMode = GamePanel.PanelMode.TRADE;
            game.screen = GamePanel.Screen.PANEL;
            game.selectedTradeOfferIndex = localTrader.offers.indexOf(food);
            game.carriedScript = 0;
            game.setSize(1280, 720);
            render(game);

            int turnBefore = game.turn;
            runButton(game, "Buy");
            require(foodReserve.remaining == 1 && game.turn == turnBefore,
                    "failed purchase must not consume finite food reserve or time");

            game.carriedScript = 100;
            runButton(game, "Buy");
            require(foodReserve.remaining == 0, "successful purchase should consume exactly one reserve unit");
            require(game.inventory.stream().anyMatch(item -> ItemQuality.namesMatch(item, "Emergency rations")),
                    "successful finite-reserve purchase should reach inventory");
            require(localTrader.offers.stream().noneMatch(o -> foodReserve.id.equals(o.essentialSupplyReserveId)),
                    "depleted reserve offers should leave the active shelf");
        } finally {
            game.shutdownRuntime();
        }

        TraderSession reopened = basicTrader();
        PopulationMarketPressureAuthority.apply(reopened, local, Faction.HIVER, 101L, 101);
        require(offer(reopened, "Emergency rations") == null,
                "reopening a trader must not recreate depleted food before its refill turn");
        require(offer(reopened, "Water bottle") != null,
                "depleting food must not erase the separate water reserve");

        Properties saved = new Properties();
        Persistence.writeWorldState(local, saved);
        World loaded = populatedWorld(94001L, ZoneType.HAB_STACK);
        Persistence.readWorldState(loaded, saved);
        EssentialSupplyReserveRecord loadedFood = EssentialSupplyProvenanceAuthority.reserveFor(loaded, Faction.HIVER, "food");
        require(loadedFood != null && loadedFood.remaining == 0
                        && "hydroponics-17".equals(loadedFood.sourceLabel),
                "depletion and source identity should survive world save/load");

        World rail = populatedWorld(94002L, ZoneType.TRAIN_SERVICE_YARD);
        rail.roomPopulationLedgers.get(0).sourceKind = "rail intake roster";
        TraderSession railTrader = basicTrader();
        PopulationMarketPressureAuthority.apply(railTrader, rail, Faction.HIVER, 50L, 50);
        EssentialSupplyReserveRecord railFood = EssentialSupplyProvenanceAuthority.reserveFor(rail, Faction.HIVER, "food");
        require(railFood != null && "outside-sector rail shipment".equals(railFood.sourceKind)
                        && railFood.route.contains("outside-sector train"),
                "rail-connected populations should receive named outside-sector shipments");

        World emergency = populatedWorld(94003L, ZoneType.TRASH_WARREN);
        TraderSession emergencyTrader = basicTrader();
        PopulationMarketPressureAuthority.apply(emergencyTrader, emergency, Faction.HIVER, 60L, 60);
        EssentialSupplyReserveRecord emergencyFood = EssentialSupplyProvenanceAuthority.reserveFor(emergency, Faction.HIVER, "food");
        require(emergencyFood != null && emergencyFood.capacity == 2
                        && "disaster relief stock".equals(emergencyFood.stockClass),
                "unsupported populations should receive only a small, explicit emergency allotment");

        System.out.println("Milestone 04 essential supply provenance smoke passed.");
    }

    private static World populatedWorld(long seed, ZoneType zone) {
        World world = new World(seed, 40, 40);
        world.zoneType = zone;
        RoomPopulationLedger ledger = new RoomPopulationLedger();
        ledger.id = "population." + seed;
        ledger.roomName = "Ordinary Habitation Block";
        ledger.sourceKind = "local population roster";
        ledger.sourceLabel = "resident provisioning ledger";
        ledger.faction = Faction.HIVER;
        ledger.capacity = 24;
        ledger.available = 16;
        ledger.assigned = 8;
        world.roomPopulationLedgers.add(ledger);
        return world;
    }

    private static TraderSession basicTrader() {
        TraderSession trader = new TraderSession();
        trader.name = "Provisioning Counter";
        trader.archetype = "civilian trader";
        trader.zoneLabel = "Hab Stack";
        trader.offers.add(new TradeOffer("Emergency rations", "food", 4, "ordinary ration stock."));
        trader.offers.add(new TradeOffer("Water bottle", "water", 3, "ordinary water stock."));
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

    private Milestone04EssentialSupplyProvenanceSmoke() { }
}
