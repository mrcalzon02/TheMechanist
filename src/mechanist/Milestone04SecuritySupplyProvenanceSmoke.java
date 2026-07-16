package mechanist;

import java.util.Properties;

/** End-to-end smoke for faction, legality, event, route, and finite security stock. */
final class Milestone04SecuritySupplyProvenanceSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        World guard = world(96001L, ZoneType.IMPERIAL_GUARD_BILLET);
        addRoom(guard, "Munition Warehouse", "ammo crates, las-cell racks, military issue cages",
                Faction.IMPERIAL_GUARD);
        TraderSession guardTrader = trader("Concord Guard Quartermaster");
        guardTrader.marketFaction = Faction.IMPERIAL_GUARD;
        guardTrader.marketCategory = "armory";
        SecuritySupplyProvenanceAuthority.apply(guardTrader, guard, Faction.IMPERIAL_GUARD, 300L, 300);

        TradeOffer lascarbine = offer(guardTrader, "Guard lascarbine");
        TradeOffer chargePack = offer(guardTrader, "Las charge pack");
        SecuritySupplyReserveRecord ammoReserve = SecuritySupplyProvenanceAuthority.reserveFor(
                guard, Faction.IMPERIAL_GUARD, "Las charge pack");
        require(lascarbine != null && chargePack != null && ammoReserve != null,
                "Guard quartermaster should receive doctrine weapon and ammunition");
        require("armory or munition store".equals(ammoReserve.sourceKind)
                        && "Munition Warehouse".equals(ammoReserve.sourceLabel),
                "Guard ammunition should resolve to the faction-owned munition room");
        require("military ammunition".equals(ammoReserve.stockClass)
                        && "restricted military issue".equals(ammoReserve.legality),
                "Guard ammunition should expose military class and legality");
        requireContains(chargePack.provenance.shortChain(), "Munition Warehouse -> issue counter",
                "Guard shelf route");
        requireContains(guardTrader.supplyChainSummary, "Security supply doctrine: disciplined military issue",
                "Guard doctrine readback");

        ammoReserve.remaining = 1;
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        try {
            game.world = guard;
            game.worldTurn = 300L;
            game.turn = 300;
            game.activeTraderSession = guardTrader;
            game.activeInteractionTitle = "Concord Guard Quartermaster";
            game.panelMode = GamePanel.PanelMode.TRADE;
            game.screen = GamePanel.Screen.PANEL;
            game.selectedTradeOfferIndex = guardTrader.offers.indexOf(chargePack);
            game.factionStanding.put(Faction.IMPERIAL_GUARD, 10);
            game.carriedScript = 0;
            game.setSize(1280, 720);
            render(game);

            int turnBefore = game.turn;
            runButton(game, "Buy");
            require(ammoReserve.remaining == 1 && game.turn == turnBefore,
                    "failed security purchase must not consume reserve or time");
            game.carriedScript = 100;
            runButton(game, "Buy");
            require(ammoReserve.remaining == 0 && game.turn == turnBefore + 1,
                    "successful ammunition purchase should consume one reserve unit and one turn");
            require(game.inventory.stream().anyMatch(item -> ItemQuality.namesMatch(item, "Las charge pack")),
                    "purchased ammunition should reach inventory");
            require(offer(guardTrader, "Las charge pack") == null,
                    "depleted ammunition should leave the active shelf");
        } finally {
            game.shutdownRuntime();
        }

        TraderSession reopened = trader("Reopened Guard Counter");
        SecuritySupplyProvenanceAuthority.apply(reopened, guard, Faction.IMPERIAL_GUARD, 301L, 301);
        require(offer(reopened, "Las charge pack") == null && offer(reopened, "Guard lascarbine") != null,
                "reopening should preserve ammunition depletion without erasing separate weapon stock");

        Properties saved = new Properties();
        Persistence.writeWorldState(guard, saved);
        World loaded = world(96001L, ZoneType.IMPERIAL_GUARD_BILLET);
        Persistence.readWorldState(loaded, saved);
        SecuritySupplyReserveRecord loadedAmmo = SecuritySupplyProvenanceAuthority.reserveFor(
                loaded, Faction.IMPERIAL_GUARD, "Las charge pack");
        require(loadedAmmo != null && loadedAmmo.remaining == 0
                        && "restricted military issue".equals(loadedAmmo.legality),
                "security depletion, class, and legality should survive save/load");

        World gang = world(96002L, ZoneType.GANGER_TURF);
        gang.zoneConflictLossHistory = "L1: source=guard-armory :: event=gang theft :: actor=Ash Market Teeth"
                + " :: affected=Sawed-off stub shotgun, Shot shell handful :: destination=black-market shelf"
                + " :: severity=major :: stolen shipment diverted after a corridor raid";
        TraderSession gangTrader = trader("Back-Room Arms Fence");
        SecuritySupplyProvenanceAuthority.apply(gangTrader, gang, Faction.GANGER_ASH_MARKET, 40L, 40);
        SecuritySupplyReserveRecord gangWeapon = SecuritySupplyProvenanceAuthority.reserveFor(
                gang, Faction.GANGER_ASH_MARKET, "Sawed-off stub shotgun");
        require(gangWeapon != null && "stolen black-market stock".equals(gangWeapon.stockClass)
                        && "contraband".equals(gangWeapon.legality),
                "gang arms should preserve theft, black-market, and contraband identity");
        requireContains(gangWeapon.route, "gang theft by Ash Market Teeth", "gang theft route");

        World wardens = world(96003L, ZoneType.ARBITES_PRECINCT_EDGE);
        wardens.zoneConflictLossHistory = "L1: source=checkpoint-armory :: event=confiscation transfer"
                + " :: actor=Civic Wardens evidence detail :: affected=Shotgun, Arbites suppression shells"
                + " :: destination=evidence locker :: severity=moderate :: controlled seizure record";
        TraderSession wardenTrader = trader("Evidence Issue Counter");
        SecuritySupplyProvenanceAuthority.apply(wardenTrader, wardens, Faction.CIVIC_WARDENS, 50L, 50);
        SecuritySupplyReserveRecord wardenAmmo = SecuritySupplyProvenanceAuthority.reserveFor(
                wardens, Faction.CIVIC_WARDENS, "Arbites suppression shells");
        require(wardenAmmo != null && "emergency confiscated stock".equals(wardenAmmo.stockClass)
                        && "controlled security issue".equals(wardenAmmo.legality),
                "Warden evidence stock should remain confiscated and controlled");

        World rail = world(96004L, ZoneType.TRAIN_SERVICE_YARD);
        TraderSession railTrader = trader("Rail Arms Broker");
        SecuritySupplyProvenanceAuthority.apply(railTrader, rail, Faction.HIVER, 60L, 60);
        SecuritySupplyReserveRecord railAmmo = SecuritySupplyProvenanceAuthority.reserveFor(
                rail, Faction.HIVER, "Stub cartridge box");
        require(railAmmo != null && "outside-sector arms shipment".equals(railAmmo.sourceKind)
                        && "route open".equals(railAmmo.eventRestriction),
                "open rail access should create an outside-sector civilian arms shipment");

        World blockade = world(96005L, ZoneType.TRAIN_SERVICE_YARD);
        blockade.zoneConflictLossHistory = "L1: source=rail-arms-intake :: event=blockade route closure"
                + " :: actor=unknown interdiction force :: affected=weapons and ammunition"
                + " :: destination=closed freight gate :: severity=major :: outside shipments halted";
        TraderSession blockadeTrader = trader("Blockaded Rail Counter");
        SecuritySupplyProvenanceAuthority.apply(blockadeTrader, blockade, Faction.HIVER, 70L, 70);
        SecuritySupplyReserveRecord blockedAmmo = SecuritySupplyProvenanceAuthority.reserveFor(
                blockade, Faction.HIVER, "Stub cartridge box");
        require(blockedAmmo != null && "blockade-restricted stock".equals(blockedAmmo.stockClass)
                        && blockedAmmo.capacity == 1
                        && blockedAmmo.eventRestriction.contains("outside shipments blocked"),
                "blockade should replace outside shipments with one local restricted reserve");

        World production = world(96006L, ZoneType.IMPERIAL_GUARD_BILLET);
        production.zoneProductionHistory = "P0: facility=rival-arms-workshop :: purpose=weapon factory"
                + " :: controller=BANDIT :: focus=weapons and ammo :: cadence=daily :: batches=6"
                + " :: retained=2 :: samples=Guard lascarbine, Las charge pack :: rival output;;"
                + "P1: facility=guard-arms-workshop :: purpose=weapon factory :: controller=IMPERIAL_GUARD"
                + " :: focus=weapons and ammo :: cadence=daily :: batches=3 :: retained=2"
                + " :: samples=Guard lascarbine, Las charge pack :: controlled output";
        TraderSession productionTrader = trader("Guard Factory Counter");
        SecuritySupplyProvenanceAuthority.apply(productionTrader, production, Faction.IMPERIAL_GUARD, 80L, 80);
        SecuritySupplyReserveRecord producedAmmo = SecuritySupplyProvenanceAuthority.reserveFor(
                production, Faction.IMPERIAL_GUARD, "Las charge pack");
        require(producedAmmo != null && "guard-arms-workshop".equals(producedAmmo.sourceLabel),
                "security production should skip rival-controlled output and use faction-controlled arms work");

        System.out.println("Milestone 04 security supply provenance smoke passed.");
    }

    private static World world(long seed, ZoneType zone) {
        World world = new World(seed, 40, 40);
        world.zoneType = zone;
        return world;
    }

    private static void addRoom(World world, String name, String description, Faction faction) {
        world.roomProfiles.add(new RoomProfile(name, description, 50, faction,
                new String[]{"ammo scrap"}, new char[]{'b'}));
        world.roomFactions.add(faction);
    }

    private static TraderSession trader(String name) {
        TraderSession trader = new TraderSession();
        trader.name = name;
        trader.archetype = "security trader";
        trader.zoneLabel = "Security Market";
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

    private Milestone04SecuritySupplyProvenanceSmoke() { }
}
