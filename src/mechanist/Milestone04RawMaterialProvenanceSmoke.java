package mechanist;

import java.util.Properties;

/** End-to-end smoke for extraction, salvage, event, import, assumed-source, and finite raw stock. */
final class Milestone04RawMaterialProvenanceSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless","true");
        World mining=world(99001L,ZoneType.HAB_STACK);
        addRoom(mining,"Faction Excavation and Quarry Room","local mine face, raw earth sorting, and stone stockpile",Faction.HIVER);
        TraderSession mineTrader=trader("Hiver Material Counter");
        RawMaterialSupplyProvenanceAuthority.apply(mineTrader,mining,Faction.HIVER,600L,600);
        RawMaterialSupplyReserveRecord earth=reserve(mining,Faction.HIVER,"Raw earth");
        RawMaterialSupplyReserveRecord stone=reserve(mining,Faction.HIVER,"Quarried stone aggregate");
        require(earth!=null&&stone!=null&&"local mining".equals(earth.sourceClass)
                        &&"Faction Excavation and Quarry Room".equals(earth.sourceLabel),
                "owned extraction room should supply finite earth and quarried stone: earth="
                        +(earth==null?"null":earth.sourceClass+"/"+earth.sourceLabel)+", stone="
                        +(stone==null?"null":stone.sourceClass+"/"+stone.sourceLabel));
        require(offer(mineTrader,"Raw earth").provenance.chain.contains("Faction Excavation and Quarry Room"),
                "extraction room should reach item provenance");

        World salvage=world(99002L,ZoneType.MUTANT_SEWER_CAMP);
        addRoom(salvage,"Salvage Warehouse and Recycler","sorted scrap, recycler grates, and recovered industrial stock",Faction.SCAVENGER);
        TraderSession salvageTrader=trader("Scavenger Salvage Counter");
        RawMaterialSupplyProvenanceAuthority.apply(salvageTrader,salvage,Faction.SCAVENGER,700L,700);
        TradeOffer ferricOffer=offer(salvageTrader,"Ferric scrap");
        RawMaterialSupplyReserveRecord ferric=reserve(salvage,Faction.SCAVENGER,"Ferric scrap");
        require(ferricOffer!=null&&ferric!=null&&"local recycling".equals(ferric.sourceClass)
                        &&offer(salvageTrader,"Recovered industrial salvage")!=null,
                "salvage/recycler room should supply finite metal and industrial salvage: ferric="
                        +(ferric==null?"null":ferric.sourceClass+"/"+ferric.sourceLabel)+", recovered="
                        +(offer(salvageTrader,"Recovered industrial salvage")==null?"missing":"present"));
        ferric.remaining=1;
        GamePanel game=new GamePanel();if(game.timer!=null)game.timer.stop();
        try{game.world=salvage;game.worldTurn=700L;game.turn=700;game.activeTraderSession=salvageTrader;
            game.activeInteractionTitle="Scavenger Salvage Counter";game.panelMode=GamePanel.PanelMode.TRADE;game.screen=GamePanel.Screen.PANEL;
            game.selectedTradeOfferIndex=salvageTrader.offers.indexOf(ferricOffer);game.carriedScript=0;game.setSize(1280,720);render(game);int before=game.turn;
            runButton(game,"Buy");require(ferric.remaining==1&&game.turn==before,"failed raw-material purchase must not consume stock or time");
            game.carriedScript=100;runButton(game,"Buy");require(ferric.remaining==0&&game.turn==before+1,"successful raw-material purchase should consume one unit and one turn");
        }finally{game.shutdownRuntime();}
        TraderSession reopened=trader("Reopened Salvage Counter");RawMaterialSupplyProvenanceAuthority.apply(reopened,salvage,Faction.SCAVENGER,701L,701);
        require(offer(reopened,"Ferric scrap")==null&&offer(reopened,"Recovered industrial salvage")!=null,"reopened market should preserve independent scrap depletion");
        Properties saved=new Properties();Persistence.writeWorldState(salvage,saved);World loaded=world(99002L,ZoneType.MUTANT_SEWER_CAMP);Persistence.readWorldState(loaded,saved);
        require(reserve(loaded,Faction.SCAVENGER,"Ferric scrap")!=null&&reserve(loaded,Faction.SCAVENGER,"Ferric scrap").remaining==0,"raw-material depletion should survive save/load");

        World production=world(99003L,ZoneType.HAB_STACK);
        production.zoneProductionHistory="P0: facility=rival-mine :: purpose=ore mine and quarry :: controller=BANDIT :: focus=earth, stone, ore :: cadence=daily :: batches=8 :: retained=4 :: samples=Raw earth, Quarried stone aggregate :: rival;;"
                +"P1: facility=hiver-excavation :: purpose=local mine and quarry :: controller=HIVER :: focus=earth, stone, ore :: cadence=daily :: batches=3 :: retained=2 :: samples=Raw earth, Quarried stone aggregate :: local";
        TraderSession productionTrader=trader("Extraction Output Counter");RawMaterialSupplyProvenanceAuthority.apply(productionTrader,production,Faction.HIVER,80L,80);
        require("hiver-excavation".equals(reserve(production,Faction.HIVER,"Raw earth").sourceLabel),"production source should reject rival extraction and use faction output");

        World movement=world(99004L,ZoneType.HAB_STACK);
        movement.zoneStockMovementHistory="M1: source=hiver-recycler :: destination=material warehouse :: kind=local recycling retention :: controller=HIVER :: samples=Ferric scrap, Recovered industrial salvage :: sorted from local waste";
        TraderSession movementTrader=trader("Material Warehouse Counter");RawMaterialSupplyProvenanceAuthority.apply(movementTrader,movement,Faction.HIVER,90L,90);
        require("local recycling".equals(reserve(movement,Faction.HIVER,"Ferric scrap").sourceClass),"stock movement should preserve local recycling provenance");

        World event=world(99005L,ZoneType.HAB_STACK);
        event.zoneConflictLossHistory="L1: source=confiscated-freight :: event=material seizure relief release :: actor=Civic Wardens :: affected=raw material and metal feedstock :: destination=public works stockpile :: severity=major :: emergency issue";
        TraderSession eventTrader=trader("Public Works Counter");eventTrader.offers.add(offerDef("Refined metal stock"));RawMaterialSupplyProvenanceAuthority.apply(eventTrader,event,Faction.HIVER,100L,100);
        RawMaterialSupplyReserveRecord seized=reserve(event,Faction.HIVER,"Refined metal stock");
        require(seized!=null&&"event-seized material".equals(seized.sourceClass)&&seized.eventModifier.contains("seizure"),"world event should preserve seizure and relief source");

        World rail=world(99006L,ZoneType.TRAIN_SERVICE_YARD);TraderSession nobleTrader=trader("Noble Freight Broker");
        RawMaterialSupplyProvenanceAuthority.apply(nobleTrader,rail,Faction.NOBLE_HOUSE_VARN,110L,110);
        require("noble material import".equals(reserve(rail,Faction.NOBLE_HOUSE_VARN,"Refined metal stock").sourceClass),"noble rail freight should retain its import class");

        World blocked=world(99007L,ZoneType.TRAIN_SERVICE_YARD);blocked.zoneConflictLossHistory="L1: source=material-freight :: event=blockade import restriction and route closure :: actor=interdiction force :: affected=raw materials :: destination=closed rail gate :: severity=major";
        TraderSession blockedTrader=trader("Blockaded Material Counter");blockedTrader.offers.add(offerDef("Refined metal stock"));RawMaterialSupplyProvenanceAuthority.apply(blockedTrader,blocked,Faction.HIVER,120L,120);
        RawMaterialSupplyReserveRecord assumed=reserve(blocked,Faction.HIVER,"Refined metal stock");
        require(assumed!=null&&assumed.capacity==1&&assumed.sourceReviewRequired&&assumed.eventModifier.contains("blocked"),"blocked existing chain should use one explicit assumed reserve with review flag");
        requireContains(blockedTrader.supplyChainSummary,"imports restricted","blocked material readback");

        World rival=world(99008L,ZoneType.HAB_STACK);addRoom(rival,"Bandit Quarry and Scrap Yard","mine, quarry, salvage, and recycler",Faction.BANDIT);
        TraderSession rivalTrader=trader("Hiver Counter Beside Rival Yard");RawMaterialSupplyProvenanceAuthority.apply(rivalTrader,rival,Faction.HIVER,130L,130);
        require(rivalTrader.offers.isEmpty()&&rival.rawMaterialSupplyReserves.isEmpty(),"rival extraction and salvage rooms must not seed another faction's shelf");
        System.out.println("Milestone 04 raw material provenance smoke passed.");
    }
    private static World world(long seed,ZoneType zone){World w=new World(seed,40,40);w.zoneType=zone;return w;}
    private static void addRoom(World w,String name,String desc,Faction faction){w.roomProfiles.add(new RoomProfile(name,desc,50,faction,new String[]{"raw earth"},new char[]{'b'}));w.roomFactions.add(faction);}
    private static TraderSession trader(String name){TraderSession t=new TraderSession();t.name=name;t.archetype="raw material trader";t.zoneLabel="Material Market";return t;}
    private static TradeOffer offerDef(String item){ItemDef d=ItemCatalog.get(item);return new TradeOffer(item,d.category,d.basePrice,"existing production-chain material request.");}
    private static TradeOffer offer(TraderSession t,String item){for(TradeOffer o:t.offers)if(o!=null&&ItemQuality.namesMatch(o.name,item))return o;return null;}
    private static RawMaterialSupplyReserveRecord reserve(World w,Faction f,String item){return RawMaterialSupplyProvenanceAuthority.reserveFor(w,f,item);}
    private static void render(GamePanel game){java.awt.image.BufferedImage c=new java.awt.image.BufferedImage(1280,720,java.awt.image.BufferedImage.TYPE_INT_ARGB);java.awt.Graphics2D g=c.createGraphics();game.paintComponent(g);g.dispose();}
    private static void runButton(GamePanel game,String label){for(ButtonBox b:game.buttons)if(b!=null&&label.equals(b.label)&&b.action!=null){b.action.run();return;}throw new AssertionError("Button not found: "+label);}
    private static void requireContains(String actual,String expected,String label){require(actual!=null&&actual.contains(expected),label+" missing '"+expected+"': "+actual);}private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}private Milestone04RawMaterialProvenanceSmoke(){}
}
