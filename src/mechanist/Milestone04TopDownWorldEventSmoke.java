package mechanist;

import java.awt.Rectangle;
import java.util.Properties;
import java.util.Random;

/** End-to-end smoke for structured top-down event generation, consequences, and recovery. */
final class Milestone04TopDownWorldEventSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless","true");
        testDisruptiveEventLifecycle();
        testReliefAndEligibilityGeneration();
        System.out.println("Milestone 04 top-down world event smoke passed.");
    }

    private static void testDisruptiveEventLifecycle() {
        World world=world(16801L);
        ShipmentProvenanceRecord shipment=shipment("event.train.existing",Faction.HIVER,900L);
        world.shipmentRecords.add(shipment);

        PersonnelReplacementRequest request=new PersonnelReplacementRequest();
        request.deadNpcId="EVENT-CASUALTY";request.deadName="Delayed Route Worker";request.faction=Faction.HIVER;
        request.requestedTurn=50;request.dueTurn=800;request.expiresTurn=1100;request.sourceMode="train-import";
        request.reason="recorded route casualty";world.replacementQueue.add(request);

        DeferredFactionLedgerRecord ledger=new DeferredFactionLedgerRecord();
        ledger.id="event.distant.hiver";ledger.faction=Faction.HIVER;world.deferredFactionLedgers.add(ledger);

        GamePanel game=new GamePanel();
        if(game.timer!=null)game.timer.stop();
        try {
            game.world=world;game.turn=100;game.worldTurn=100;
            game.factionStrategicPlans.clear();
            FactionStrategicPlan plan=FactionStrategicPlan.create(Faction.HIVER,new Random(7),0);
            plan.phase="PLANNING";plan.scheme="recruit a rival specialist";
            int planDeadline=plan.phaseUntilTurn;
            game.factionStrategicPlans.add(plan);

            TopDownWorldEventRecord event=TopDownWorldEventAuthority.scheduleCurated(world,"TRAIN_OUTAGE",game.worldTurn);
            require(event!=null&&"SCHEDULED".equals(event.status)&&event.importClosed,
                    "train outage should be a scheduled faction-neutral import closure");
            require(event.endWorldTurn>event.startWorldTurn&&event.severity>=1&&event.severity<=5,
                    "structured event should retain bounded severity and duration");
            requireContains(event.eligibilitySummary,"import node","event eligibility explanation");
            requireContains(event.newsExposure,"train-station notices","event notice channel");
            requireContains(event.roomMutationHook,"Train platforms","physical consequence hook");

            long arrivalBefore=shipment.earliestArrivalWorldTurn;
            int requestBefore=request.dueTurn;
            TopDownWorldEventAuthority.TickResult tick=TopDownWorldEventAuthority.tick(world,game,game.worldTurn);
            require(tick.activated()==1&&"ACTIVE".equals(event.status)&&event.appliedWorldTurn==game.worldTurn,
                    "scheduled event should activate once at its start turn");
            require(shipment.earliestArrivalWorldTurn>arrivalBefore&&"DELAYED".equals(shipment.status)
                            &&shipment.delayRiskPct>10,
                    "train outage should delay existing cargo and raise its risk");
            require(request.dueTurn>requestBefore&&request.expiresTurn>request.dueTurn,
                    "train outage should delay reinforcement availability without destroying its arrival window");
            require(TopDownWorldEventAuthority.pressureFor(world,Faction.HIVER)>0,
                    "active disruption should contribute structured distant-network pressure");
            require(game.factionStrategicPlans.size()==1&&game.factionStrategicPlans.get(0)==plan
                            &&plan.phaseUntilTurn==planDeadline&&"PLANNING".equals(plan.phase),
                    "top-down events must not masquerade as or mutate faction officer schemes");
            require(TopDownWorldEventAuthority.tick(world,game,game.worldTurn).activated()==0,
                    "same-turn event ticks must not apply consequences twice");

            ShipmentProvenanceRecord newShipment=shipment("event.train.new",Faction.HIVER,1000L);
            long newBefore=newShipment.earliestArrivalWorldTurn;
            TopDownWorldEventAuthority.applyToNewShipment(world,newShipment,game.worldTurn);
            require(newShipment.earliestArrivalWorldTurn>newBefore&&newShipment.eventModifier.contains("train outage"),
                    "shipments created during an active outage should inherit its timing and provenance modifier");

            require(GameplayConsoleCommandAuthority.isKnown("world_events"),
                    "player-rank world-events command should be registered");
            requireContains(GameplayConsoleCommandAuthority.help(new String[]{"world_events"}),
                    "scope","world-events help");
            InternalServerSessionAuthority.CommandContext player=
                    new InternalServerSessionAuthority.CommandContext("player","local-user",false,"local-world","local-server");
            String report=GameplayConsoleCommandAuthority.execute(game,player,"world_events",new String[0]);
            requireContains(report,"Train outage [ACTIVE]","world-events command title and state");
            requireContains(report,"shipment timing","world-events command effects");
            requireContains(report,"Local stock and internal faction vendors remain open","world-events local exception");
            requireContains(report,"Physical state:","world-events physical consequence");

            Properties saved=new Properties();Persistence.writeWorldState(world,saved);
            World restored=world(16801L);Persistence.readWorldState(restored,saved);
            require(restored.topDownWorldEvents.size()==1,"world-event ledger should survive save/load");
            TopDownWorldEventRecord restoredEvent=restored.topDownWorldEvents.get(0);
            require(restoredEvent.id.equals(event.id)&&restoredEvent.status.equals("ACTIVE")
                            &&restoredEvent.appliedWorldTurn==event.appliedWorldTurn
                            &&restoredEvent.endWorldTurn==event.endWorldTurn,
                    "event identity, active state, application turn, and duration should persist");
            require(restored.nextTopDownWorldEventCheckTurn==world.nextTopDownWorldEventCheckTurn
                            &&restored.topDownWorldEventGenerationCount==world.topDownWorldEventGenerationCount,
                    "event generation cadence and sequence should persist");

            TopDownWorldEventAuthority.TickResult recovery=
                    TopDownWorldEventAuthority.tick(world,game,event.endWorldTurn);
            require(recovery.recovered()==1&&"RECOVERED".equals(event.status)
                            &&event.recoveryWorldTurn==event.endWorldTurn,
                    "event should enter a persisted recovery state at its stated end turn");
            require(TopDownWorldEventAuthority.pressureFor(world,Faction.HIVER)==0,
                    "recovered event should stop contributing distant pressure");
            requireContains(TopDownWorldEventAuthority.commandReport(world),"Latest recovery: Train outage",
                    "recovered event command readback");
        } finally { game.shutdownRuntime(); }
    }

    private static void testReliefAndEligibilityGeneration() {
        World reliefWorld=world(16802L);
        EssentialSupplyReserveRecord food=new EssentialSupplyReserveRecord();
        food.id="event.relief.food";food.faction=Faction.HIVER;food.capacity=3;food.remaining=0;
        reliefWorld.essentialSupplyReserves.add(food);
        RawMaterialSupplyReserveRecord raw=new RawMaterialSupplyReserveRecord();
        raw.id="event.relief.raw";raw.faction=Faction.HIVER;raw.capacity=2;raw.remaining=0;
        reliefWorld.rawMaterialSupplyReserves.add(raw);
        ShipmentProvenanceRecord shipment=shipment("event.relief.shipment",Faction.HIVER,900L);
        reliefWorld.shipmentRecords.add(shipment);
        long arrivalBefore=shipment.earliestArrivalWorldTurn;

        TopDownWorldEventRecord relief=TopDownWorldEventAuthority.scheduleCurated(reliefWorld,"RELIEF_SHIPMENT",100L);
        TopDownWorldEventAuthority.TickResult reliefTick=TopDownWorldEventAuthority.tick(reliefWorld,null,100L);
        require(reliefTick.activated()==1&&relief.positive&&relief.economySupplyPct>0,
                "relief shipment should activate as a positive supply event");
        require(food.remaining==1&&raw.remaining==1,
                "relief should replenish one unit in each depleted finite reserve without creating infinite stock");
        require(shipment.earliestArrivalWorldTurn<arrivalBefore&&shipment.delayRiskPct<10,
                "relief should advance eligible cargo and lower shipment risk");
        requireContains(relief.consequenceSummary,"2 depleted reserve(s) replenished","relief consequence summary");

        World generated=world(16803L);
        EssentialSupplyReserveRecord empty=new EssentialSupplyReserveRecord();
        empty.id="event.generated.shortage";empty.faction=Faction.HIVER;empty.capacity=2;empty.remaining=0;
        generated.essentialSupplyReserves.add(empty);
        generated.nextTopDownWorldEventCheckTurn=50L;
        TopDownWorldEventAuthority.TickResult generatedTick=TopDownWorldEventAuthority.tick(generated,null,50L);
        require(generatedTick.generated()==1&&generated.topDownWorldEvents.size()==1,
                "due eligibility review should generate one structured event");
        TopDownWorldEventRecord generatedEvent=generated.topDownWorldEvents.get(0);
        require("RELIEF_SHIPMENT".equals(generatedEvent.eventType)&&generatedEvent.positive,
                "severe shortage should curate relief instead of another arbitrary penalty");
        require(generated.nextTopDownWorldEventCheckTurn>generatedEvent.endWorldTurn,
                "generated event should schedule a post-recovery eligibility review");
    }

    private static ShipmentProvenanceRecord shipment(String id,Faction faction,long arrival) {
        ShipmentProvenanceRecord shipment=new ShipmentProvenanceRecord();
        shipment.id=id;shipment.destinationFaction=faction;shipment.sourceFaction="outside-sector supplier";
        shipment.supplier="Outer Belt Freight";shipment.sourceSite="Outer Belt Depot";
        shipment.destinationFacility="Local Receiving Store";shipment.arrivalNode="Sector Import Hall";
        shipment.cargoItem="Emergency rations";shipment.cargoManifest="Emergency rations x2";
        shipment.quantity=2;shipment.remaining=2;shipment.status="SCHEDULED";shipment.delayRiskPct=10;
        shipment.earliestArrivalWorldTurn=arrival;shipment.latestArrivalWorldTurn=arrival+200L;
        shipment.eventModifier="no active shipment event";return shipment;
    }

    private static World world(long seed) {
        World world=new World(seed,52,42);world.zoneType=ZoneType.NEUTRAL_RAIL_DEPOT;
        world.zoneName="World Event Exchange";world.floor=5;world.zoneX=2;world.zoneY=2;
        for(int x=0;x<world.w;x++)for(int y=0;y<world.h;y++)world.tiles[x][y]='#';
        Rectangle room=new Rectangle(4,4,20,14);world.carve(room);world.rooms.add(room);
        world.roomProfiles.set(0,new RoomProfile("Sector Import Hall","rail-linked receiving hall",60,Faction.HIVER,new String[]{"Trade chit"},new char[]{'Q'}));
        world.roomFactions.set(0,Faction.HIVER);world.roomSpecials.set(0,Boolean.FALSE);
        for(int x=room.x+1;x<room.x+room.width-1;x++)for(int y=room.y+1;y<room.y+room.height-1;y++)world.tiles[x][y]='.';
        return world;
    }

    private static void requireContains(String actual,String expected,String label){require(actual!=null&&actual.contains(expected),label+" missing '"+expected+"': "+actual);}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private Milestone04TopDownWorldEventSmoke(){}
}
