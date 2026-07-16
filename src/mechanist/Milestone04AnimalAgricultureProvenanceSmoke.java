package mechanist;

import java.util.Properties;

/** End-to-end smoke for living animal, room, handler, event, import, and finite-stock provenance. */
final class Milestone04AnimalAgricultureProvenanceSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");

        World farm = world(98001L, ZoneType.HAB_STACK);
        addRoom(farm, "Starch Hog Pen and Feed Store", "faction farm, fodder bins, animal pen, and breeding ledger", Faction.HIVER);
        addRoom(farm, "Fungus Garden", "managed fungus beds beside the faction farm", Faction.HIVER);
        addRoom(farm, "Cistern Water Station", "clean animal water store and pump station", Faction.HIVER);
        addRoom(farm, "Veterinary Care Room", "animal clinic, dosing bench, and isolation stall", Faction.HIVER);
        NpcEntity hog = animal("ANIMAL-HOG-1", "Bess the starch hog", "farm-animal", "starch-hog", Faction.HIVER,
                "Starch Hog Pen and Feed Store", "Hiver Cooperative breeder ledger");
        NpcEntity handler = handler("HANDLER-1", "Mara Venn", "Animal Breeder and Handler", Faction.HIVER);
        farm.npcs.add(hog); farm.npcs.add(handler);

        TraderSession farmTrader = trader("Hiver Farm Counter");
        AnimalAgricultureSupplyProvenanceAuthority.apply(farmTrader, farm, Faction.HIVER, 500L, 500);
        TradeOffer product = offer(farmTrader, "Farm animal product crate");
        require(product != null && offer(farmTrader, "Animal feed sack") != null
                        && offer(farmTrader, "Veterinary care kit") != null
                        && offer(farmTrader, "Seed culture tray") != null
                        && offer(farmTrader, "Fungus starter mat") != null,
                "operating faction farm should expose animal, seed, fungus, feed, and care stock");
        AnimalAgricultureSupplyReserveRecord productReserve = reserve(farm, Faction.HIVER, "Farm animal product crate");
        require(productReserve != null && "Bess the starch hog".equals(productReserve.animalLabel)
                        && productReserve.breederOrOwner.contains("Hiver Cooperative breeder ledger")
                        && productReserve.penOwner.contains("Hiver")
                        && productReserve.handlerLabel.contains("Mara Venn")
                        && productReserve.feedSource.contains("Starch Hog Pen")
                        && productReserve.waterSource.contains("Cistern Water Station")
                        && productReserve.careSource.contains("Veterinary Care Room"),
                "animal product should name its living source, breeder, owner, handler, feed, water, and care chain");
        require(product.provenance != null && product.provenance.chain.contains("Bess the starch hog")
                        && product.provenance.chain.contains("Mara Venn"),
                "animal and handler chain should reach purchased item provenance");
        requireContains(farmTrader.supplyChainSummary, "Animal/agriculture supply:", "farm supply readback");

        productReserve.remaining = 1;
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        try {
            game.world = farm; game.worldTurn = 500L; game.turn = 500;
            game.activeTraderSession = farmTrader; game.activeInteractionTitle = "Hiver Farm Counter";
            game.panelMode = GamePanel.PanelMode.TRADE; game.screen = GamePanel.Screen.PANEL;
            game.selectedTradeOfferIndex = farmTrader.offers.indexOf(product);
            game.carriedScript = 0; game.setSize(1280, 720); render(game);
            int turnBefore = game.turn;
            runButton(game, "Buy");
            require(productReserve.remaining == 1 && game.turn == turnBefore,
                    "failed animal-product purchase must not consume stock or time");
            game.carriedScript = 100;
            runButton(game, "Buy");
            require(productReserve.remaining == 0 && game.turn == turnBefore + 1,
                    "successful animal-product purchase should consume one exact unit and one turn");
            require(game.inventory.stream().anyMatch(item -> ItemQuality.namesMatch(item, "Farm animal product crate")),
                    "purchased animal product should reach inventory");
        } finally { game.shutdownRuntime(); }

        TraderSession reopened = trader("Reopened Hiver Farm Counter");
        AnimalAgricultureSupplyProvenanceAuthority.apply(reopened, farm, Faction.HIVER, 501L, 501);
        require(offer(reopened, "Farm animal product crate") == null && offer(reopened, "Animal feed sack") != null,
                "reopened farm should preserve animal-product depletion independently from feed stock");
        Properties saved = new Properties();
        Persistence.writeWorldState(farm, saved);
        World loaded = world(98001L, ZoneType.HAB_STACK);
        Persistence.readWorldState(loaded, saved);
        AnimalAgricultureSupplyReserveRecord loadedProduct = reserve(loaded, Faction.HIVER, "Farm animal product crate");
        require(loadedProduct != null && loadedProduct.remaining == 0
                        && loadedProduct.handlerLabel.contains("Mara Venn")
                        && loadedProduct.waterSource.contains("Cistern Water Station"),
                "animal supply depletion and care chain should survive save/load");

        World petMarket = world(98002L, ZoneType.NEUTRAL_CIVILIAN_FLOOR);
        addRoom(petMarket, "Companion Animal Room", "pet bedding, kennel wash point, and handler counter", Faction.HIVER);
        petMarket.npcs.add(animal("PET-1", "Pip the dust-mouse", "pet", "dust-mouse", Faction.HIVER,
                "Companion Animal Room", "Hiver household pet ledger"));
        petMarket.npcs.add(handler("PET-HANDLER-1", "Sera Coil", "Pet Handler", Faction.HIVER));
        TraderSession petTrader = trader("Companion Animal Vendor");
        AnimalAgricultureSupplyProvenanceAuthority.apply(petTrader, petMarket, Faction.HIVER, 60L, 60);
        AnimalAgricultureSupplyReserveRecord petReserve = reserve(petMarket, Faction.HIVER, "Pet care bundle");
        require(petReserve != null && petReserve.animalLabel.contains("Pip")
                        && petReserve.handlerLabel.contains("Sera Coil")
                        && offer(petTrader, "Pet care bundle") != null,
                "pet vendor stock should bind to a living owned pet and handler");

        World pressure = world(98003L, ZoneType.HAB_STACK);
        addRoom(pressure, "Ploin Fowl Farm and Feed Store", "animal pen, farm feed, water trough, and veterinary bench", Faction.HIVER);
        pressure.npcs.add(animal("FOWL-1", "Ploin fowl flock", "farm-animal", "ploin-fowl", Faction.HIVER,
                "Ploin Fowl Farm and Feed Store", "Hiver fowl breeder ledger"));
        pressure.npcs.add(handler("FOWL-HANDLER-1", "Orrin Vale", "Farm Animal Handler", Faction.HIVER));
        pressure.zoneConflictLossHistory = "L1: source=fowl-pen :: event=animal disease outbreak and feed shortage"
                + " :: actor=local husbandry office :: affected=farm animal products and fodder"
                + " :: destination=veterinary screening stall :: severity=major :: output rationed";
        TraderSession pressureTrader = trader("Pressured Farm Counter");
        AnimalAgricultureSupplyProvenanceAuthority.apply(pressureTrader, pressure, Faction.HIVER, 70L, 70);
        AnimalAgricultureSupplyReserveRecord pressuredProduct = reserve(pressure, Faction.HIVER, "Farm animal product crate");
        require(pressuredProduct != null && pressuredProduct.capacity == 1
                        && pressuredProduct.goodsClass.contains("disease-screened")
                        && pressuredProduct.eventPressure.contains("animal disease screening")
                        && pressuredProduct.eventPressure.contains("feed shortage"),
                "disease and feed shortage should visibly ration animal output");

        World rail = world(98004L, ZoneType.TRAIN_SERVICE_YARD);
        TraderSession railTrader = trader("Agricultural Freight Counter");
        AnimalAgricultureSupplyProvenanceAuthority.apply(railTrader, rail, Faction.HIVER, 80L, 80);
        AnimalAgricultureSupplyReserveRecord importedSample = reserve(rail, Faction.HIVER, "Cloning sample ampoule");
        require(importedSample != null && "outside-sector agricultural import".equals(importedSample.sourceKind)
                        && importedSample.route.contains("rail intake"),
                "open rail route should provide traceable agricultural and cloning imports");

        World restrictedRail = world(98005L, ZoneType.TRAIN_SERVICE_YARD);
        restrictedRail.zoneConflictLossHistory = "L1: source=agricultural-freight :: event=import restriction and route closure"
                + " :: actor=sector customs :: affected=seed, breeding, and cloning stock"
                + " :: destination=closed rail intake :: severity=major :: imports withheld";
        TraderSession restrictedTrader = trader("Restricted Freight Counter");
        AnimalAgricultureSupplyProvenanceAuthority.apply(restrictedTrader, restrictedRail, Faction.HIVER, 90L, 90);
        require(offer(restrictedTrader, "Cloning sample ampoule") == null
                        && offer(restrictedTrader, "Seed culture tray") == null,
                "import restriction should not create replacement cloning or seed stock from nowhere");
        requireContains(restrictedTrader.supplyChainSummary, "imports restricted", "import restriction readback");

        World production = world(98006L, ZoneType.HAB_STACK);
        production.zoneProductionHistory = "P0: facility=rival-gene-farm :: purpose=agricultural cloning nursery"
                + " :: controller=BANDIT :: focus=seed and genetic samples :: cadence=daily :: batches=8"
                + " :: retained=4 :: samples=Seed culture tray, Cloning sample ampoule :: rival output;;"
                + "P1: facility=hiver-garden-coop :: purpose=hydroponic farm and cloning nursery"
                + " :: controller=HIVER :: focus=seed, fungus, crops, and genetic samples :: cadence=daily :: batches=3"
                + " :: retained=2 :: samples=Seed culture tray, Fungus starter mat, Cloning sample ampoule :: local output";
        TraderSession productionTrader = trader("Cooperative Garden Counter");
        AnimalAgricultureSupplyProvenanceAuthority.apply(productionTrader, production, Faction.HIVER, 100L, 100);
        AnimalAgricultureSupplyReserveRecord producedSeed = reserve(production, Faction.HIVER, "Seed culture tray");
        require(producedSeed != null && "hiver-garden-coop".equals(producedSeed.sourceLabel),
                "agricultural production should reject rival output and use faction-controlled stock");

        World rivalOnly = world(98007L, ZoneType.HAB_STACK);
        addRoom(rivalOnly, "Bandit Animal Farm", "animal pens, feed store, garden, and breeder room", Faction.BANDIT);
        rivalOnly.npcs.add(animal("RIVAL-HOG", "Rival starch hog", "farm-animal", "starch-hog", Faction.BANDIT,
                "Bandit Animal Farm", "Bandit breeder ledger"));
        TraderSession rivalTrader = trader("Hiver Market Beside Rival Farm");
        AnimalAgricultureSupplyProvenanceAuthority.apply(rivalTrader, rivalOnly, Faction.HIVER, 110L, 110);
        require(rivalTrader.offers.isEmpty() && rivalOnly.animalAgricultureSupplyReserves.isEmpty(),
                "rival-owned farm rooms and animals must not supply another faction's market");

        System.out.println("Milestone 04 animal and agriculture provenance smoke passed.");
    }

    private static World world(long seed, ZoneType zone) { World world = new World(seed, 40, 40); world.zoneType = zone; return world; }
    private static void addRoom(World world, String name, String description, Faction faction) {
        world.roomProfiles.add(new RoomProfile(name, description, 50, faction, new String[]{"animal feed sack"}, new char[]{'b'}));
        world.roomFactions.add(faction);
    }
    private static NpcEntity animal(String id, String name, String kind, String profile, Faction faction,
                                    String room, String pool) {
        NpcEntity npc = new NpcEntity(); npc.id=id; npc.name=name; npc.creatureKind=kind; npc.animalProfileId=profile;
        npc.role="Managed Animal"; npc.state="Penned"; npc.faction=faction; npc.hp=12; npc.x=5; npc.y=5;
        npc.idleBias=3; npc.routineOffset=1; npc.numericId=Math.abs(id.hashCode());
        npc.provenance = new PersonnelProvenanceRecord(); npc.provenance.originRoom=room;
        npc.provenance.originSiteId="animal-site."+id; npc.provenance.populationPool=pool;
        return npc;
    }
    private static NpcEntity handler(String id, String name, String role, Faction faction) {
        NpcEntity npc = new NpcEntity(); npc.id=id; npc.name=name; npc.role=role; npc.state="Work";
        npc.faction=faction; npc.hp=12; npc.numericId=Math.abs(id.hashCode()); npc.idleBias=3; npc.routineOffset=1;
        return npc;
    }
    private static TraderSession trader(String name) { TraderSession trader=new TraderSession(); trader.name=name; trader.archetype="animal and agriculture vendor"; trader.zoneLabel="Local Market"; return trader; }
    private static TradeOffer offer(TraderSession trader, String item) { for(TradeOffer offer:trader.offers) if(offer!=null&&ItemQuality.namesMatch(offer.name,item)) return offer; return null; }
    private static AnimalAgricultureSupplyReserveRecord reserve(World world, Faction faction, String item) { return AnimalAgricultureSupplyProvenanceAuthority.reserveFor(world,faction,item); }
    private static void render(GamePanel game) { java.awt.image.BufferedImage canvas=new java.awt.image.BufferedImage(1280,720,java.awt.image.BufferedImage.TYPE_INT_ARGB); java.awt.Graphics2D g=canvas.createGraphics(); game.paintComponent(g); g.dispose(); }
    private static void runButton(GamePanel game, String label) { for(ButtonBox button:game.buttons) if(button!=null&&label.equals(button.label)&&button.action!=null){button.action.run();return;} throw new AssertionError("Button not found: "+label); }
    private static void requireContains(String actual,String expected,String label){require(actual!=null&&actual.contains(expected),label+" missing '"+expected+"': "+actual);}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private Milestone04AnimalAgricultureProvenanceSmoke() { }
}
