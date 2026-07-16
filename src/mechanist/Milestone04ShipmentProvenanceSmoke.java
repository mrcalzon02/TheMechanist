package mechanist;

import java.util.Properties;

/** End-to-end smoke for incoming manifests, route status, delivery, and persistence. */
final class Milestone04ShipmentProvenanceSmoke {
    public static void main(String[] args){
        System.setProperty("java.awt.headless","true");
        World arrived=world(100001L,ZoneType.TRAIN_SERVICE_YARD);arrived.zoneName="East Freight Concourse";
        TraderSession arrivedTrader=trader("Hiver Industrial Import Counter");
        RawMaterialSupplyProvenanceAuthority.apply(arrivedTrader,arrived,Faction.HIVER,500L,500);
        ShipmentProvenanceAuthority.apply(arrivedTrader,arrived,Faction.HIVER,500L,500);
        TradeOffer metal=offer(arrivedTrader,"Refined metal stock");
        RawMaterialSupplyReserveRecord metalReserve=RawMaterialSupplyProvenanceAuthority.reserveFor(arrived,Faction.HIVER,"Refined metal stock");
        ShipmentProvenanceRecord manifest=ShipmentProvenanceAuthority.shipmentForCargo(arrived,Faction.HIVER,"Refined metal stock");
        require(metal!=null&&metalReserve!=null&&manifest!=null&&"ARRIVED".equals(manifest.status),"open external route should place arrived finite cargo on the shelf");
        require(manifest.sourceFaction.contains("outside-sector")&&manifest.supplier.contains("freight train")
                        &&"Hiver Industrial Import Counter".equals(manifest.destinationFacility)
                        &&manifest.arrivalNode.contains("East Freight Concourse")&&manifest.destinationFaction==Faction.HIVER,
                "shipment should preserve source supplier, destination faction/facility, and arrival node");
        require(manifest.cargoManifest.contains("Refined metal stock")&&manifest.cargoValue>0
                        &&"ordinary legal freight".equals(manifest.legality)&&"ordinary handling risk".equals(manifest.qualityRisk)
                        &&manifest.earliestArrivalWorldTurn<=500L&&manifest.latestArrivalWorldTurn>=500L
                        &&"player-visible operational".equals(manifest.simulationMode)&&manifest.playerVisible,
                "shipment should expose cargo, value, legality, quality, window, and visibility");
        require(manifest.interceptionRiskPct>0&&manifest.delayRiskPct>0&&metal.provenance.chain.contains(manifest.id),"shipment risks and manifest id should reach item provenance");
        require("civilian merchant procurement".equals(manifest.procurementPolicy)
                        &&manifest.sourceProcurementCost>manifest.cargoValue
                        &&manifest.sourceCooldownBaseTurns>0&&manifest.sourceCooldownVarianceTurns!=0
                        &&manifest.sourceCooldownTurns==manifest.sourceCooldownBaseTurns+manifest.sourceCooldownVarianceTurns
                        &&manifest.nextSourceAvailableWorldTurn>manifest.latestArrivalWorldTurn
                        &&arrivedTrader.buyPrice(metal)>=metal.shipmentProcurementUnitCost,
                "Hiver off-map supply should expose faction cost, large signed cooldown variance, next-source turn, and landed price floor");

        metalReserve.remaining=1;manifest.quantity=1;manifest.remaining=1;
        GamePanel game=new GamePanel();if(game.timer!=null)game.timer.stop();
        try{game.world=arrived;game.worldTurn=500L;game.turn=500;game.activeTraderSession=arrivedTrader;game.activeInteractionTitle="Hiver Industrial Import Counter";
            game.panelMode=GamePanel.PanelMode.TRADE;game.screen=GamePanel.Screen.PANEL;game.selectedTradeOfferIndex=arrivedTrader.offers.indexOf(metal);game.carriedScript=0;game.setSize(1280,720);render(game);int before=game.turn;
            runButton(game,"Buy");require(metalReserve.remaining==1&&manifest.remaining==1&&game.turn==before,"failed shipment purchase must consume neither reserve nor cargo nor time");
            game.carriedScript=100;runButton(game,"Buy");require(metalReserve.remaining==0&&manifest.remaining==0&&"DELIVERED".equals(manifest.status)&&game.turn==before+1,"successful purchase should consume underlying reserve and shipment cargo exactly once");
            require(game.inventory.stream().anyMatch(i->ItemQuality.namesMatch(i,"Refined metal stock")),"delivered cargo should reach player inventory");
        }finally{game.shutdownRuntime();}
        Properties saved=new Properties();Persistence.writeWorldState(arrived,saved);World loaded=world(100001L,ZoneType.TRAIN_SERVICE_YARD);Persistence.readWorldState(loaded,saved);
        ShipmentProvenanceRecord loadedManifest=ShipmentProvenanceAuthority.shipmentForCargo(loaded,Faction.HIVER,"Refined metal stock");
        require(loadedManifest!=null&&loadedManifest.remaining==0&&"DELIVERED".equals(loadedManifest.status)
                        &&loadedManifest.arrivalNode.contains("East Freight Concourse")
                        &&loadedManifest.sourceProcurementCost==manifest.sourceProcurementCost
                        &&loadedManifest.sourceCooldownVarianceTurns==manifest.sourceCooldownVarianceTurns
                        &&loadedManifest.nextSourceAvailableWorldTurn==manifest.nextSourceAvailableWorldTurn,
                "shipment route, status, depletion, faction cost, and cooldown should survive save/load");

        metalReserve.remaining=1;TraderSession cooldownTrader=trader("Hiver Industrial Import Counter");
        RawMaterialSupplyProvenanceAuthority.apply(cooldownTrader,arrived,Faction.HIVER,501L,501);ShipmentProvenanceAuthority.apply(cooldownTrader,arrived,Faction.HIVER,501L,501);
        require(offer(cooldownTrader,"Refined metal stock")==null&&arrived.shipmentRecords.size()==1,
                "delivered off-map cargo must not recreate before the faction source cooldown expires");
        long nextSourceTurn=manifest.nextSourceAvailableWorldTurn;TraderSession nextShipmentTrader=trader("Hiver Industrial Import Counter");
        RawMaterialSupplyProvenanceAuthority.apply(nextShipmentTrader,arrived,Faction.HIVER,nextSourceTurn,(int)Math.min(Integer.MAX_VALUE,nextSourceTurn));ShipmentProvenanceAuthority.apply(nextShipmentTrader,arrived,Faction.HIVER,nextSourceTurn,(int)Math.min(Integer.MAX_VALUE,nextSourceTurn));
        ShipmentProvenanceRecord secondManifest=ShipmentProvenanceAuthority.shipmentForCargo(arrived,Faction.HIVER,"Refined metal stock");
        require(secondManifest!=null&&secondManifest.deliverySequence==2&&offer(nextShipmentTrader,"Refined metal stock")!=null
                        &&secondManifest.sourceCooldownVarianceTurns!=0&&secondManifest.sourceProcurementCost>0,
                "expired faction cooldown should permit a newly costed shipment with a fresh large variance");

        World delayed=world(100002L,ZoneType.TRAIN_SERVICE_YARD);delayed.zoneName="Customs Spur";
        delayed.zoneConflictLossHistory="L1: source=outer-freight :: event=shipment delay and customs delay :: actor=rail customs :: affected=industrial imports :: destination=Customs Spur :: severity=major :: rail rerouting caused delayed freight";
        TraderSession delayedTrader=trader("Delayed Import Counter");RawMaterialSupplyProvenanceAuthority.apply(delayedTrader,delayed,Faction.HIVER,600L,600);ShipmentProvenanceAuthority.apply(delayedTrader,delayed,Faction.HIVER,600L,600);
        ShipmentProvenanceRecord delayedManifest=ShipmentProvenanceAuthority.shipmentForCargo(delayed,Faction.HIVER,"Refined metal stock");
        require(delayedManifest!=null&&"DELAYED".equals(delayedManifest.status)&&offer(delayedTrader,"Refined metal stock")==null
                        &&delayedManifest.delayRiskPct>=55&&delayedManifest.earliestArrivalWorldTurn>600L,
                "customs delay should persist a future arrival window and withhold cargo");
        TraderSession beforeWindow=trader("Delayed Import Counter");RawMaterialSupplyProvenanceAuthority.apply(beforeWindow,delayed,Faction.HIVER,601L,601);ShipmentProvenanceAuthority.apply(beforeWindow,delayed,Faction.HIVER,601L,601);
        require(offer(beforeWindow,"Refined metal stock")==null,"reopening before the window must not recreate delayed cargo");
        long arrivalTurn=delayedManifest.earliestArrivalWorldTurn;TraderSession afterWindow=trader("Delayed Import Counter");RawMaterialSupplyProvenanceAuthority.apply(afterWindow,delayed,Faction.HIVER,arrivalTurn,(int)Math.min(Integer.MAX_VALUE,arrivalTurn));ShipmentProvenanceAuthority.apply(afterWindow,delayed,Faction.HIVER,arrivalTurn,(int)Math.min(Integer.MAX_VALUE,arrivalTurn));
        require("ARRIVED".equals(delayedManifest.status)&&offer(afterWindow,"Refined metal stock")!=null,"delayed cargo should become purchasable when its arrival window opens");

        World intercepted=world(100003L,ZoneType.TRAIN_SERVICE_YARD);intercepted.zoneName="Outer Rail Gate";
        intercepted.zoneConflictLossHistory="L1: source=merchant-consist :: event=shipment interception and cargo piracy :: actor=route raiders :: affected=metal shipment :: destination=Outer Rail Gate :: severity=major :: raided shipment";
        TraderSession interceptedTrader=trader("Interrupted Import Counter");RawMaterialSupplyProvenanceAuthority.apply(interceptedTrader,intercepted,Faction.HIVER,700L,700);ShipmentProvenanceAuthority.apply(interceptedTrader,intercepted,Faction.HIVER,700L,700);
        ShipmentProvenanceRecord interceptedManifest=ShipmentProvenanceAuthority.shipmentForCargo(intercepted,Faction.HIVER,"Refined metal stock");
        require(interceptedManifest!=null&&"INTERCEPTED".equals(interceptedManifest.status)&&offer(interceptedTrader,"Refined metal stock")==null
                        &&interceptedManifest.interceptionRiskPct>=40,"intercepted shipment should never appear as delivered shelf stock");

        World illicit=world(100004L,ZoneType.TRAIN_SERVICE_YARD);TraderSession gangTrader=trader("Ash Market Freight Broker");
        RawMaterialSupplyProvenanceAuthority.apply(gangTrader,illicit,Faction.GANGER_ASH_MARKET,800L,800);ShipmentProvenanceAuthority.apply(gangTrader,illicit,Faction.GANGER_ASH_MARKET,800L,800);
        ShipmentProvenanceRecord illicitManifest=ShipmentProvenanceAuthority.shipmentForCargo(illicit,Faction.GANGER_ASH_MARKET,"Refined metal stock");
        require(illicitManifest!=null&&illicitManifest.legality.contains("black-market")&&illicitManifest.interceptionRiskPct>=45
                        &&"black-market broker procurement".equals(illicitManifest.procurementPolicy)
                        &&illicitManifest.sourceCooldownBaseTurns!=manifest.sourceCooldownBaseTurns
                        &&illicitManifest.sourceProcurementCost>illicitManifest.cargoValue,
                "gang imports should retain illicit legality, elevated interception risk, and distinct faction cost/cooldown policy");

        World tainted=world(100005L,ZoneType.TRAIN_SERVICE_YARD);tainted.zoneName="Bio-Import Platform";TraderSession taintedTrader=trader("Bio-Import Inspector");
        TradeOffer sample=offerDef("Cloning sample ampoule");sample.provenance=ItemProvenanceRecord.of(sample.name,Faction.NONE,"off-world gene merchant",tainted,20,"sealed living sample","off-world nursery -> rail intake -> import inspection");sample.provenance.producingFacility="off-world cloning nursery";sample.provenance.batchIssueTags="counterfeit label; contamination warning";taintedTrader.offers.add(sample);
        ShipmentProvenanceAuthority.apply(taintedTrader,tainted,Faction.HIVER,900L,900);ShipmentProvenanceRecord taintedManifest=ShipmentProvenanceAuthority.shipmentForCargo(tainted,Faction.HIVER,"Cloning sample ampoule");
        require(taintedManifest!=null&&"restricted controlled freight".equals(taintedManifest.legality)
                        &&"counterfeit cargo risk".equals(taintedManifest.qualityRisk)&&sample.provenance.batchIssueTags.contains("counterfeit cargo risk"),
                "controlled living imports should preserve counterfeit or contamination risk into item provenance");

        World ledger=world(100006L,ZoneType.NEUTRAL_RAIL_DEPOT);ledger.zoneName="Distant Cargo Terminal";
        ledger.zoneStockMovementHistory="M9: source=off-world-mine-77 :: destination=Hiver Cargo Warehouse :: kind=incoming shipment and merchant import :: controller=Outer Belt Cooperative :: samples=Raw earth, Refined metal stock :: abstract distant freight";
        TraderSession ledgerTrader=trader("Warehouse Clerk");ShipmentProvenanceAuthority.apply(ledgerTrader,ledger,Faction.HIVER,1000L,1000);
        require(ledger.shipmentRecords.size()==1,"external stock-movement ledger should create one shipment manifest");ShipmentProvenanceRecord ledgerManifest=ledger.shipmentRecords.get(0);
        require(ledgerManifest.cargoManifest.contains("Raw earth")&&ledgerManifest.cargoValue>0
                        &&"abstract operational".equals(ledgerManifest.simulationMode)&&!ledgerManifest.playerVisible
                        &&"Outer Belt Cooperative".equals(ledgerManifest.sourceFaction),
                "distant ledger shipment should retain cargo, value, supplier, and abstract visibility state");
        System.out.println("Milestone 04 shipment provenance smoke passed.");
    }
    private static World world(long seed,ZoneType zone){World w=new World(seed,40,40);w.zoneType=zone;return w;}
    private static TraderSession trader(String name){TraderSession t=new TraderSession();t.name=name;t.archetype="import freight trader";t.zoneLabel="Rail Freight Market";return t;}
    private static TradeOffer offerDef(String item){ItemDef d=ItemCatalog.get(item);return new TradeOffer(item,d.category,d.basePrice,"imported shipment stock.");}
    private static TradeOffer offer(TraderSession t,String item){for(TradeOffer o:t.offers)if(o!=null&&ItemQuality.namesMatch(o.name,item))return o;return null;}
    private static void render(GamePanel g){java.awt.image.BufferedImage c=new java.awt.image.BufferedImage(1280,720,java.awt.image.BufferedImage.TYPE_INT_ARGB);java.awt.Graphics2D x=c.createGraphics();g.paintComponent(x);x.dispose();}
    private static void runButton(GamePanel g,String label){for(ButtonBox b:g.buttons)if(b!=null&&label.equals(b.label)&&b.action!=null){b.action.run();return;}throw new AssertionError("Button not found: "+label);}
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}private Milestone04ShipmentProvenanceSmoke(){}
}
