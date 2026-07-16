package mechanist;

import java.awt.Rectangle;
import java.util.List;
import java.util.Properties;
import java.util.Random;

/** End-to-end smoke for persisted distant faction ledgers and factor-driven outcomes. */
final class Milestone04DeferredOutOfSectorSimulationSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless","true");
        testProbabilityFactors();
        testDeferredNetworkLifecycle();
        System.out.println("Milestone 04 deferred out-of-sector simulation smoke passed.");
    }

    private static void testProbabilityFactors() {
        DeferredFactionLedgerRecord supported=new DeferredFactionLedgerRecord();
        supported.faction=Faction.HIVER;
        setSupport(supported,90);setPressure(supported,5);
        DeferredFactionLedgerRecord pressured=new DeferredFactionLedgerRecord();
        pressured.faction=Faction.HIVER;
        setSupport(pressured,10);setPressure(pressured,95);
        DeferredOutOfSectorSimulationAuthority.Probability high=
                DeferredOutOfSectorSimulationAuthority.calculateProbability(supported);
        DeferredOutOfSectorSimulationAuthority.Probability low=
                DeferredOutOfSectorSimulationAuthority.calculateProbability(pressured);
        require(high.chancePct()>=90&&low.chancePct()<=10&&high.chancePct()-low.chancePct()>=80,
                "support and pressure factors should materially change distant-resolution odds");
        requireContains(high.explanation(),"strength 90","support-factor explanation");
        requireContains(low.explanation(),"player disruption 95","pressure-factor explanation");
    }

    private static void testDeferredNetworkLifecycle() {
        World world=world(16601L);
        FactionImportNodeGenerationAuthority.promoteAndPlan(world,new Random(1));
        FactionImportNodeGenerationAuthority.placePhysicalNodes(world,new Random(2));
        MapObjectState node=FactionImportNodeGenerationAuthority.primaryNode(world,Faction.HIVER);
        require(node!=null,"test world should generate a physical Hiver import node");

        NpcEntity worker=NpcEntity.create(Faction.HIVER,world.zoneType,8,8,new Random(3));
        worker.id="DISTANT-SMOKE-WORKER";worker.name="Local Receiving Worker";world.npcs.add(worker);
        int workerX=worker.x,workerY=worker.y;

        ShipmentProvenanceRecord shipment=new ShipmentProvenanceRecord();
        shipment.id="shipment.distant.smoke";shipment.destinationFaction=Faction.HIVER;
        shipment.sourceFaction="outside-sector cooperative";shipment.supplier="Outer Belt Freight Cooperative";
        shipment.sourceSite="Outer Belt Refinery";shipment.destinationFacility="Hiver Receiving Counter";
        shipment.arrivalNode=node.label;shipment.cargoItem="Refined metal stock";shipment.cargoManifest="Refined metal stock x3";
        shipment.quantity=3;shipment.remaining=3;shipment.cargoValue=360;shipment.status="SCHEDULED";
        shipment.departureWorldTurn=400;shipment.earliestArrivalWorldTurn=900;shipment.latestArrivalWorldTurn=1100;
        shipment.delayRiskPct=30;shipment.route=shipment.sourceSite+" -> "+node.label;
        world.shipmentRecords.add(shipment);

        PersonnelReplacementRequest request=new PersonnelReplacementRequest();
        request.deadNpcId="DISTANT-SMOKE-CASUALTY";request.deadName="Lost Route Guard";request.faction=Faction.HIVER;
        request.requestedTurn=400;request.dueTurn=1200;request.expiresTurn=1500;request.sourceMode="train-import";
        request.source=node.label;request.sourceLedgerId="population.distant.smoke";request.reason="route loss";
        request.sourcePrerequisite="generated import intake";world.replacementQueue.add(request);

        RawMaterialSupplyReserveRecord reserve=new RawMaterialSupplyReserveRecord();
        reserve.id="raw-material.distant.smoke";reserve.itemName="Refined metal stock";reserve.faction=Faction.HIVER;
        reserve.sourceClass="outside-sector rail freight";reserve.sourceLabel="Outer Belt Refinery";
        reserve.sourceLocality="outside-sector";reserve.route=reserve.sourceLabel+" -> "+node.label;
        reserve.capacity=3;reserve.remaining=1;reserve.nextRestockWorldTurn=1400;world.rawMaterialSupplyReserves.add(reserve);

        GamePanel game=new GamePanel();
        if(game.timer!=null)game.timer.stop();
        try{
            game.world=world;game.turn=600;game.worldTurn=600;game.suspicion=6;game.gangHeat=4;
            game.factionStrategicPlans.clear();
            FactionStrategicPlan plan=FactionStrategicPlan.create(Faction.HIVER,new Random(4),0);
            plan.ambition=78;plan.success=2;plan.phase="EXECUTION";plan.scheme="open a protected freight route";
            game.factionStrategicPlans.add(plan);

            int seeded=DeferredOutOfSectorSimulationAuthority.ensureLedgers(world,game,game.worldTurn);
            require(seeded==1&&world.deferredFactionLedgers.size()==1,
                    "one compact ledger should represent the participating distant Hiver network");
            DeferredFactionLedgerRecord ledger=DeferredOutOfSectorSimulationAuthority.ledgerFor(world,Faction.HIVER);
            require(ledger!=null&&ledger.nextResolutionWorldTurn>=1000&&ledger.nextResolutionWorldTurn<=1400,
                    "distant ledger should receive a variable six-hour review timer");

            ledger.shipmentPressure=100;ledger.routeSafety=0;ledger.reinforcementDemand=0;
            ledger.personnelLosses=0;ledger.eventPressure=0;ledger.rivalInterference=0;ledger.nextResolutionWorldTurn=600;
            long beforeArrival=shipment.earliestArrivalWorldTurn;
            DeferredOutOfSectorSimulationAuthority.TickResult tick=
                    DeferredOutOfSectorSimulationAuthority.tick(world,game,game.worldTurn);
            require(tick.resolved()==1&&ledger.resolutionCount==1&&"shipment route".equals(ledger.lastFocus),
                    "due distant network should resolve one shipment-focused outcome");
            require(ledger.lastRoll>=1&&ledger.lastRoll<=100&&ledger.lastChancePct>=5&&ledger.lastChancePct<=95,
                    "resolution should retain its deterministic roll and factor-based chance");
            requireContains(ledger.factorSummary,"supplier","resolved factor summary");
            requireContains(ledger.factorSummary,"reinforcement demand","resolved pressure summary");
            boolean success=ledger.lastRoll<=ledger.lastChancePct;
            require(success?shipment.earliestArrivalWorldTurn<beforeArrival:shipment.earliestArrivalWorldTurn>beforeArrival,
                    "resolved shipment outcome should advance success or delay setback cargo timing");
            require(success||"DELAYED".equals(shipment.status),"shipment setback should enter delayed state");
            require(worker.x==workerX&&worker.y==workerY&&world.npcs.size()==1,
                    "deferred simulation must not tick, move, duplicate, or replace local actors");
            require(ledger.nextResolutionWorldTurn>game.worldTurn,
                    "resolved network should schedule a future variable review instead of repeating every turn");
            require(DeferredOutOfSectorSimulationAuthority.tick(world,game,game.worldTurn).resolved()==0,
                    "same-turn calls must not resolve the distant network twice");

            require(GameplayConsoleCommandAuthority.isKnown("distant_network"),
                    "player-rank distant network command should be registered");
            requireContains(GameplayConsoleCommandAuthority.help(new String[]{"distant_network"}),
                    "resolution odds","distant network help");
            InternalServerSessionAuthority.CommandContext player=
                    new InternalServerSessionAuthority.CommandContext("player","local-user",false,"local-world","local-server");
            String report=GameplayConsoleCommandAuthority.execute(game,player,"distant_network",new String[0]);
            requireContains(report,"Distant network:","distant network command status");
            requireContains(report,"Distant resolution:","distant network command odds");
            requireContains(report,"Distant outcome:","distant network command result");
            requireContains(String.join(" ",FactionImportNodeGenerationAuthority.inspectionLines(world,node,game.worldTurn)),
                    "Distant resolution:","import-node distant readback");

            Properties saved=new Properties();Persistence.writeWorldState(world,saved);
            World restored=world(16601L);Persistence.readWorldState(restored,saved);
            DeferredFactionLedgerRecord restoredLedger=
                    DeferredOutOfSectorSimulationAuthority.ledgerFor(restored,Faction.HIVER);
            require(restoredLedger!=null&&restoredLedger.resolutionCount==1
                            &&restoredLedger.lastRoll==ledger.lastRoll
                            &&restoredLedger.lastChancePct==ledger.lastChancePct
                            &&restoredLedger.nextResolutionWorldTurn==ledger.nextResolutionWorldTurn,
                    "distant resolution, odds, roll, and timer should survive save/load");
            require(restoredLedger.lastOutcome.equals(ledger.lastOutcome)
                            &&restoredLedger.factorSummary.equals(ledger.factorSummary)
                            &&restoredLedger.recentEvents.size()==1,
                    "distant causes and outcome history should survive save/load");
        }finally{game.shutdownRuntime();}
    }

    private static World world(long seed){
        World world=new World(seed,78,58);world.zoneType=ZoneType.NEUTRAL_RAIL_DEPOT;
        world.zoneName="Distant Network Exchange";world.floor=5;world.zoneX=2;world.zoneY=2;
        for(int x=0;x<world.w;x++)for(int y=0;y<world.h;y++)world.tiles[x][y]='#';
        addRoom(world,"Central Plaza","neutral transit plaza",Faction.NONE,4,4);
        addRoom(world,"Cargo Warehouse","controlled freight and material store",Faction.HIVER,23,4);
        addRoom(world,"Service Receiving Room","rail-linked loading room",Faction.HIVER,42,4);
        addRoom(world,"Aurel Freight Office","a related block-faction receiving office",Faction.HIVER_BLOCK_AUREL,61,4);
        return world;
    }

    private static void addRoom(World world,String name,String description,Faction faction,int x,int y){
        int index=world.rooms.size();Rectangle room=new Rectangle(x,y,14,10);world.carve(room);world.rooms.add(room);
        world.roomProfiles.set(index,new RoomProfile(name,description,60,faction,new String[]{"Trade chit"},new char[]{'Q'}));
        world.roomFactions.set(index,faction);world.roomSpecials.set(index,Boolean.FALSE);
        for(int px=room.x+1;px<room.x+room.width-1;px++)for(int py=room.y+1;py<room.y+room.height-1;py++)world.tiles[px][py]='.';
    }

    private static void setSupport(DeferredFactionLedgerRecord ledger,int value){
        ledger.strength=value;ledger.influence=value;ledger.wealth=value;ledger.supplierReliability=value;
        ledger.routeSafety=value;ledger.rawMaterialAvailability=value;ledger.machineReliability=value;
        ledger.productQuality=value;ledger.productionEfficiency=value;ledger.importExportCapacity=value;
        ledger.leadershipCompetence=value;
    }

    private static void setPressure(DeferredFactionLedgerRecord ledger,int value){
        ledger.populationPressure=value;ledger.personnelLosses=value;ledger.reinforcementDemand=value;
        ledger.shipmentPressure=value;ledger.warehouseVulnerability=value;ledger.rivalInterference=value;
        ledger.schemePressure=value;ledger.eventPressure=value;ledger.playerHeat=value;
    }

    private static void requireContains(String actual,String expected,String label){require(actual!=null&&actual.contains(expected),label+" missing '"+expected+"': "+actual);}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private Milestone04DeferredOutOfSectorSimulationSmoke(){}
}
