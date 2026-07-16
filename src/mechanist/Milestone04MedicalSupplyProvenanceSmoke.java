package mechanist;

import java.util.Properties;

/** End-to-end smoke for medical source, legality, risk, route restrictions, and finite stock. */
final class Milestone04MedicalSupplyProvenanceSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        World clinic = world(97001L, ZoneType.HAB_STACK);
        addRoom(clinic, "Block Clinic Room", "sealed medical cabinet, stained cot, boiled instruments", Faction.HIVER);
        TraderSession clinicTrader = trader("Block Clinic Dispensary");
        MedicalSupplyProvenanceAuthority.apply(clinicTrader, clinic, Faction.HIVER, 400L, 400);
        TradeOffer bandage = offer(clinicTrader, "Bandage roll");
        TradeOffer antiseptic = offer(clinicTrader, "Antiseptic vial");
        MedicalSupplyReserveRecord bandageReserve = MedicalSupplyProvenanceAuthority.reserveFor(
                clinic, Faction.HIVER, "Bandage roll");
        require(bandage != null && antiseptic != null && bandageReserve != null,
                "operating clinic should supply baseline treatment and infection medicine");
        require("local clinic".equals(bandageReserve.sourceKind)
                        && "Block Clinic Room".equals(bandageReserve.sourceLabel),
                "ordinary medicine should resolve to the faction clinic room");
        require("legal clinic medicine".equals(bandageReserve.stockClass)
                        && "ordinary medical sale".equals(bandageReserve.legality),
                "clinic medicine should preserve legal treatment identity");
        requireContains(clinicTrader.supplyChainSummary, "Medical supply policy: ordinary clinic supply",
                "clinic policy readback");

        bandageReserve.remaining = 1;
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        try {
            game.world = clinic; game.worldTurn = 400L; game.turn = 400;
            game.activeTraderSession = clinicTrader;
            game.activeInteractionTitle = "Block Clinic Dispensary";
            game.panelMode = GamePanel.PanelMode.TRADE; game.screen = GamePanel.Screen.PANEL;
            game.selectedTradeOfferIndex = clinicTrader.offers.indexOf(bandage);
            game.carriedScript = 0; game.setSize(1280, 720); render(game);
            int turnBefore = game.turn;
            runButton(game, "Buy");
            require(bandageReserve.remaining == 1 && game.turn == turnBefore,
                    "failed medical purchase must not consume stock or time");
            game.carriedScript = 100;
            runButton(game, "Buy");
            require(bandageReserve.remaining == 0 && game.turn == turnBefore + 1,
                    "successful medical purchase should consume one unit and one turn");
            require(game.inventory.stream().anyMatch(item -> ItemQuality.namesMatch(item, "Bandage roll")),
                    "purchased treatment should reach inventory");
        } finally { game.shutdownRuntime(); }

        TraderSession reopened = trader("Reopened Clinic Dispensary");
        MedicalSupplyProvenanceAuthority.apply(reopened, clinic, Faction.HIVER, 401L, 401);
        require(offer(reopened, "Bandage roll") == null && offer(reopened, "Antiseptic vial") != null,
                "reopened clinic should preserve treatment depletion independently from antiseptic stock");
        Properties saved = new Properties();
        Persistence.writeWorldState(clinic, saved);
        World loaded = world(97001L, ZoneType.HAB_STACK);
        Persistence.readWorldState(loaded, saved);
        MedicalSupplyReserveRecord loadedBandage = MedicalSupplyProvenanceAuthority.reserveFor(
                loaded, Faction.HIVER, "Bandage roll");
        require(loadedBandage != null && loadedBandage.remaining == 0
                        && "ordinary treatment risk".equals(loadedBandage.riskClass),
                "medical depletion, source, legality, and risk should survive save/load");

        World gang = world(97002L, ZoneType.GANGER_TURF);
        gang.zoneConflictLossHistory = "L1: source=illicit-chem-lab :: event=counterfeit contaminated drug diversion"
                + " :: actor=gang pharmacy ring :: affected=Stim vial, Bandage roll"
                + " :: destination=black-market clinic :: severity=major :: tainted batch warning";
        TraderSession gangTrader = trader("Gang Back-Room Clinic");
        MedicalSupplyProvenanceAuthority.apply(gangTrader, gang, Faction.GANGER_RED_GRIN, 40L, 40);
        MedicalSupplyReserveRecord gangStim = MedicalSupplyProvenanceAuthority.reserveFor(
                gang, Faction.GANGER_RED_GRIN, "Stim vial");
        require(gangStim != null && "counterfeit medicine".equals(gangStim.stockClass)
                        && "black-market drug sale".equals(gangStim.legality)
                        && gangStim.riskClass.contains("counterfeit or contamination"),
                "gang stimulant should preserve counterfeit, illicit, and risk identity");
        require(offer(gangTrader, "Stim vial").provenance.batchIssueTags.contains("counterfeit"),
                "counterfeit batch warning should reach item provenance");

        World noble = world(97003L, ZoneType.SECTOR_GOVERNORS_MANSION);
        addRoom(noble, "House Medicae Suite", "private physician supply and sealed medicine cabinets", Faction.NOBLE);
        TraderSession nobleTrader = trader("House Physician Counter");
        MedicalSupplyProvenanceAuthority.apply(nobleTrader, noble, Faction.NOBLE, 50L, 50);
        MedicalSupplyReserveRecord nobleKit = MedicalSupplyProvenanceAuthority.reserveFor(noble, Faction.NOBLE, "Medkit");
        require(nobleKit != null && "private physician supply".equals(nobleKit.sourceKind)
                        && "noble physician medicine".equals(nobleKit.stockClass)
                        && "household physician authorization".equals(nobleKit.legality),
                "noble medicine should retain private physician and household authorization");

        World relief = world(97004L, ZoneType.NEUTRAL_CIVILIAN_FLOOR);
        RoomPopulationLedger displaced = new RoomPopulationLedger();
        displaced.sourceKind = "displaced relief intake"; displaced.sourceLabel = "evacuation aid station";
        displaced.roomName = "Refugee Triage Hall"; displaced.faction = Faction.HIVER; displaced.capacity = 20;
        relief.roomPopulationLedgers.add(displaced);
        TraderSession reliefTrader = trader("Relief Dispensary");
        MedicalSupplyProvenanceAuthority.apply(reliefTrader, relief, Faction.HIVER, 60L, 60);
        MedicalSupplyReserveRecord reliefBandage = MedicalSupplyProvenanceAuthority.reserveFor(
                relief, Faction.HIVER, "Bandage roll");
        require(reliefBandage != null && "disaster relief shipment".equals(reliefBandage.sourceKind)
                        && "priority relief issue".equals(reliefBandage.routeRestriction),
                "displaced population should receive explicit relief medicine");

        World rail = world(97005L, ZoneType.TRAIN_SERVICE_YARD);
        TraderSession railTrader = trader("Rail Pharmacy");
        MedicalSupplyProvenanceAuthority.apply(railTrader, rail, Faction.HIVER, 70L, 70);
        MedicalSupplyReserveRecord railBandage = MedicalSupplyProvenanceAuthority.reserveFor(
                rail, Faction.HIVER, "Bandage roll");
        require(railBandage != null && "outside-sector pharmaceutical shipment".equals(railBandage.sourceKind)
                        && "route open".equals(railBandage.routeRestriction),
                "open rail route should provide outside-sector medicine");

        World blockade = world(97006L, ZoneType.TRAIN_SERVICE_YARD);
        blockade.zoneConflictLossHistory = "L1: source=rail-medical-intake :: event=blockade route closure"
                + " :: actor=interdiction force :: affected=medical freight and medicine"
                + " :: destination=closed freight gate :: severity=major :: quarantine halted shipments";
        TraderSession blockadeTrader = trader("Blockaded Pharmacy");
        MedicalSupplyProvenanceAuthority.apply(blockadeTrader, blockade, Faction.HIVER, 80L, 80);
        MedicalSupplyReserveRecord blockedBandage = MedicalSupplyProvenanceAuthority.reserveFor(
                blockade, Faction.HIVER, "Bandage roll");
        require(blockedBandage != null && "blockade-restricted medicine".equals(blockedBandage.stockClass)
                        && blockedBandage.capacity == 1
                        && blockedBandage.routeRestriction.contains("outside shipments blocked"),
                "blockade should leave only one local medical reserve");

        World production = world(97007L, ZoneType.HAB_STACK);
        production.zoneProductionHistory = "P0: facility=rival-pharmacy :: purpose=medical laboratory"
                + " :: controller=BANDIT :: focus=medicine and drugs :: cadence=daily :: batches=8"
                + " :: retained=3 :: samples=Bandage roll, Antiseptic vial :: rival output;;"
                + "P1: facility=hiver-clinic-lab :: purpose=medical laboratory :: controller=HIVER"
                + " :: focus=medicine and drugs :: cadence=daily :: batches=3 :: retained=2"
                + " :: samples=Bandage roll, Antiseptic vial :: local output";
        TraderSession productionTrader = trader("Clinic Laboratory Counter");
        productionTrader.offers.add(new TradeOffer("Bandage roll", "medical", 4, "clinic stock."));
        MedicalSupplyProvenanceAuthority.apply(productionTrader, production, Faction.HIVER, 90L, 90);
        MedicalSupplyReserveRecord producedBandage = MedicalSupplyProvenanceAuthority.reserveFor(
                production, Faction.HIVER, "Bandage roll");
        require(producedBandage != null && "hiver-clinic-lab".equals(producedBandage.sourceLabel),
                "medical production should reject rival output and use faction-controlled laboratory stock");

        System.out.println("Milestone 04 medical supply provenance smoke passed.");
    }

    private static World world(long seed, ZoneType zone) { World w = new World(seed, 40, 40); w.zoneType = zone; return w; }
    private static void addRoom(World world, String name, String description, Faction faction) {
        world.roomProfiles.add(new RoomProfile(name, description, 50, faction, new String[]{"bandage roll"}, new char[]{'u'}));
        world.roomFactions.add(faction);
    }
    private static TraderSession trader(String name) { TraderSession t = new TraderSession(); t.name=name; t.archetype="medical trader"; t.zoneLabel="Medical Market"; return t; }
    private static TradeOffer offer(TraderSession trader, String item) { for(TradeOffer o:trader.offers) if(o!=null&&ItemQuality.namesMatch(o.name,item)) return o; return null; }
    private static void render(GamePanel game) { java.awt.image.BufferedImage c=new java.awt.image.BufferedImage(1280,720,java.awt.image.BufferedImage.TYPE_INT_ARGB); java.awt.Graphics2D g=c.createGraphics(); game.paintComponent(g); g.dispose(); }
    private static void runButton(GamePanel game, String label) { for(ButtonBox b:game.buttons) if(b!=null&&label.equals(b.label)&&b.action!=null){b.action.run();return;} throw new AssertionError("Button not found: "+label); }
    private static void requireContains(String actual,String expected,String label){require(actual!=null&&actual.contains(expected),label+" missing '"+expected+"': "+actual);}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private Milestone04MedicalSupplyProvenanceSmoke() { }
}
