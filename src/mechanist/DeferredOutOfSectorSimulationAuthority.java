package mechanist;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Persisted faction summaries for distant activity that should not receive local actor/item ticks. */
final class DeferredFactionLedgerRecord {
    String id = "distant-network.unassigned";
    Faction faction = Faction.NONE;
    String sourceRegion = "unrecorded distant network";
    int strength = 50;
    int influence = 50;
    int wealth = 50;
    int populationPressure = 25;
    int personnelLosses = 0;
    int reinforcementDemand = 0;
    int supplierReliability = 50;
    int routeSafety = 50;
    int shipmentPressure = 0;
    int warehouseVulnerability = 20;
    int rawMaterialAvailability = 50;
    int machineReliability = 50;
    int productQuality = 50;
    int productionEfficiency = 50;
    int importExportCapacity = 40;
    int rivalInterference = 20;
    int leadershipCompetence = 50;
    int schemePressure = 10;
    int eventPressure = 0;
    int playerHeat = 0;
    long lastResolutionWorldTurn = -1L;
    long nextResolutionWorldTurn = 0L;
    int resolutionCount = 0;
    int lastChancePct = 0;
    int lastRoll = 0;
    String lastFocus = "network establishment";
    String lastOutcome = "No distant outcome has resolved yet.";
    String factorSummary = "Factors have not been calculated yet.";
    final ArrayList<String> recentEvents = new ArrayList<>();

    String saveLine() {
        return enc(id) + "|" + (faction == null ? Faction.NONE.name() : faction.name()) + "|" + enc(sourceRegion)
                + "|" + strength + "|" + influence + "|" + wealth + "|" + populationPressure
                + "|" + personnelLosses + "|" + reinforcementDemand + "|" + supplierReliability
                + "|" + routeSafety + "|" + shipmentPressure + "|" + warehouseVulnerability
                + "|" + rawMaterialAvailability + "|" + machineReliability + "|" + productQuality
                + "|" + productionEfficiency + "|" + importExportCapacity + "|" + rivalInterference
                + "|" + leadershipCompetence + "|" + schemePressure + "|" + eventPressure + "|" + playerHeat
                + "|" + lastResolutionWorldTurn + "|" + nextResolutionWorldTurn + "|" + resolutionCount
                + "|" + lastChancePct + "|" + lastRoll + "|" + enc(lastFocus) + "|" + enc(lastOutcome)
                + "|" + enc(factorSummary) + "|" + enc(String.join("~~", recentEvents));
    }

    static DeferredFactionLedgerRecord parse(String line) {
        try {
            String[] a = line.split("\\|", 32);
            if (a.length < 32) return null;
            DeferredFactionLedgerRecord r = new DeferredFactionLedgerRecord();
            r.id=dec(a[0]); r.faction=Faction.valueOf(a[1]); r.sourceRegion=dec(a[2]);
            r.strength=pct(a[3]); r.influence=pct(a[4]); r.wealth=pct(a[5]); r.populationPressure=pct(a[6]);
            r.personnelLosses=pct(a[7]); r.reinforcementDemand=pct(a[8]); r.supplierReliability=pct(a[9]);
            r.routeSafety=pct(a[10]); r.shipmentPressure=pct(a[11]); r.warehouseVulnerability=pct(a[12]);
            r.rawMaterialAvailability=pct(a[13]); r.machineReliability=pct(a[14]); r.productQuality=pct(a[15]);
            r.productionEfficiency=pct(a[16]); r.importExportCapacity=pct(a[17]); r.rivalInterference=pct(a[18]);
            r.leadershipCompetence=pct(a[19]); r.schemePressure=pct(a[20]); r.eventPressure=pct(a[21]);
            r.playerHeat=pct(a[22]); r.lastResolutionWorldTurn=Long.parseLong(a[23]);
            r.nextResolutionWorldTurn=Math.max(0L,Long.parseLong(a[24])); r.resolutionCount=Math.max(0,Integer.parseInt(a[25]));
            r.lastChancePct=pct(a[26]); r.lastRoll=pct(a[27]); r.lastFocus=dec(a[28]);
            r.lastOutcome=dec(a[29]); r.factorSummary=dec(a[30]);
            String events=dec(a[31]); if(!events.isBlank()) for(String event:events.split("~~")) if(!event.isBlank()) r.recentEvents.add(event);
            return r;
        } catch (Exception ignored) { return null; }
    }

    private static int pct(String value) { return Math.max(0,Math.min(100,Integer.parseInt(value))); }
    private static String enc(String value) { return Base64.getUrlEncoder().withoutPadding().encodeToString((value==null?"":value).getBytes(StandardCharsets.UTF_8)); }
    private static String dec(String value) { return new String(Base64.getUrlDecoder().decode(value),StandardCharsets.UTF_8); }
}

final class DeferredOutOfSectorSimulationAuthority {
    static final int RESOLUTION_BASE_TURNS = 6 * GamePanel.TURNS_PER_HOUR;
    static final int RESOLUTION_VARIANCE_TURNS = 2 * GamePanel.TURNS_PER_HOUR;
    private static final int MAX_FACTION_LEDGERS = 12;
    private static final int MAX_RESOLUTIONS_PER_TICK = 4;
    private static final int MAX_RECENT_EVENTS = 6;

    record Probability(int chancePct, int supportScore, int pressureScore, String explanation) { }
    record TickResult(int seeded, int resolved, long nextResolutionWorldTurn, String summary) { }

    private DeferredOutOfSectorSimulationAuthority() { }

    static int ensureLedgers(World world, GamePanel game, long worldTurn) {
        if (world == null) return 0;
        deduplicateFamilyLedgers(world);
        LinkedHashSet<Faction> factions = participatingFactions(world);
        int seeded = 0;
        for (Faction faction : factions) {
            if (faction == null || faction == Faction.NONE || ledgerFor(world,faction) != null) continue;
            DeferredFactionLedgerRecord ledger = create(world,faction,worldTurn);
            observe(world,game,ledger);
            world.deferredFactionLedgers.add(ledger);
            seeded++;
            if (world.deferredFactionLedgers.size() >= MAX_FACTION_LEDGERS) break;
        }
        return seeded;
    }

    static TickResult tick(GamePanel game, boolean sleeping) {
        if (game == null || game.world == null) return new TickResult(0,0,0L,"No distant faction network is active.");
        TickResult result = tick(game.world,game,game.worldTurn);
        game.lastDeferredFactionSimulationReport = result.summary();
        if (result.resolved() > 0) {
            game.logEvent(result.summary());
            DebugLog.audit("DEFERRED_FACTION_SIMULATION", result.summary());
        }
        return result;
    }

    static TickResult tick(World world, GamePanel game, long worldTurn) {
        if (world == null) return new TickResult(0,0,0L,"No distant faction network is active.");
        int seeded = ensureLedgers(world,game,worldTurn);
        int resolved = 0;
        ArrayList<String> outcomes = new ArrayList<>();
        long next = Long.MAX_VALUE;
        for (DeferredFactionLedgerRecord ledger : world.deferredFactionLedgers) {
            if (ledger == null) continue;
            if (resolved < MAX_RESOLUTIONS_PER_TICK && worldTurn >= ledger.nextResolutionWorldTurn) {
                observe(world,game,ledger);
                outcomes.add(resolve(world,ledger,worldTurn));
                resolved++;
            }
            next = Math.min(next,ledger.nextResolutionWorldTurn);
        }
        String summary = resolved == 0
                ? "Distant faction networks: " + world.deferredFactionLedgers.size() + " ledger(s); next review at world turn " + (next==Long.MAX_VALUE?"unavailable":next) + "."
                : "Distant network update: " + String.join(" ",outcomes);
        return new TickResult(seeded,resolved,next==Long.MAX_VALUE?0L:next,summary);
    }

    static Probability calculateProbability(DeferredFactionLedgerRecord ledger) {
        if (ledger == null) return new Probability(5,0,100,"No faction factors are available.");
        int support = average(ledger.strength,ledger.influence,ledger.wealth,ledger.supplierReliability,
                ledger.routeSafety,ledger.rawMaterialAvailability,ledger.machineReliability,ledger.productQuality,
                ledger.productionEfficiency,ledger.importExportCapacity,ledger.leadershipCompetence);
        int pressure = average(ledger.populationPressure,ledger.personnelLosses,ledger.reinforcementDemand,
                ledger.shipmentPressure,ledger.warehouseVulnerability,ledger.rivalInterference,
                ledger.schemePressure,ledger.eventPressure,ledger.playerHeat);
        int chance = clamp((int)Math.round(50.0 + (support-50)*0.78 - (pressure-35)*0.58));
        chance = Math.max(5,Math.min(95,chance));
        String factors = "support " + support + " [strength " + ledger.strength + ", influence " + ledger.influence
                + ", wealth " + ledger.wealth + ", suppliers " + ledger.supplierReliability + ", route safety "
                + ledger.routeSafety + ", raw materials " + ledger.rawMaterialAvailability + ", machinery "
                + ledger.machineReliability + ", product quality " + ledger.productQuality + ", efficiency "
                + ledger.productionEfficiency + ", import capacity " + ledger.importExportCapacity + ", leadership "
                + ledger.leadershipCompetence + "]; pressure " + pressure + " [population " + ledger.populationPressure
                + ", losses " + ledger.personnelLosses + ", reinforcement demand " + ledger.reinforcementDemand
                + ", shipments " + ledger.shipmentPressure + ", warehouse risk " + ledger.warehouseVulnerability
                + ", rivals " + ledger.rivalInterference + ", schemes " + ledger.schemePressure + ", events "
                + ledger.eventPressure + ", player disruption " + ledger.playerHeat + "]";
        return new Probability(chance,support,pressure,factors);
    }

    static DeferredFactionLedgerRecord ledgerFor(World world, Faction faction) {
        if (world == null) return null;
        Faction wanted = faction == null ? Faction.NONE : faction;
        DeferredFactionLedgerRecord exact = findExact(world,wanted);
        if (exact != null) return exact;
        for (DeferredFactionLedgerRecord ledger : world.deferredFactionLedgers) {
            if (ledger != null && sameFamily(ledger.faction,wanted)) return ledger;
        }
        return null;
    }

    static List<String> summaryLines(World world, Faction faction, long worldTurn) {
        if (world == null) return List.of("Distant network: no world state is available.");
        ensureLedgers(world,null,worldTurn);
        Faction wanted = faction == null || faction == Faction.NONE ? preferredFaction(world) : faction;
        DeferredFactionLedgerRecord ledger = ledgerFor(world,wanted);
        if (ledger == null) return List.of("Distant network: no participating faction ledger is available.");
        Probability probability = calculateProbability(ledger);
        int chance = ledger.resolutionCount > 0 ? ledger.lastChancePct : probability.chancePct();
        String factors = ledger.resolutionCount > 0 ? ledger.factorSummary : probability.explanation();
        return List.of(
                "Distant network: " + ledger.faction.label + " through " + ledger.sourceRegion + "; next review at world turn " + ledger.nextResolutionWorldTurn + ".",
                "Distant resolution: " + chance + "% chance; " + factors + ".",
                "Distant outcome: " + ledger.lastOutcome);
    }

    static String commandReport(World world, long worldTurn) {
        return String.join(" | ",summaryLines(world,Faction.NONE,worldTurn));
    }

    private static DeferredFactionLedgerRecord create(World world, Faction faction, long worldTurn) {
        DeferredFactionLedgerRecord ledger = new DeferredFactionLedgerRecord();
        ledger.faction = faction;
        ledger.id = "DISTANT-" + faction.name() + "-" + Math.abs(Objects.hash(world.seed,world.locationKey(),faction.name()));
        MapObjectState node = FactionImportNodeGenerationAuthority.primaryNode(world,faction);
        ledger.sourceRegion = node == null ? "outside-sector suppliers serving " + safe(world.zoneName,world.zoneType.label) : node.label;
        int basis = Objects.hash(world.seed,world.locationKey(),faction.name(),"distant-ledger");
        ledger.strength = 42 + Math.floorMod(basis,19);
        ledger.influence = 40 + Math.floorMod(basis/7,21);
        ledger.wealth = 38 + Math.floorMod(basis/13,25);
        ledger.supplierReliability = 45 + Math.floorMod(basis/17,21);
        ledger.routeSafety = 45 + Math.floorMod(basis/23,26);
        ledger.rawMaterialAvailability = 42 + Math.floorMod(basis/29,23);
        ledger.machineReliability = 45 + Math.floorMod(basis/31,21);
        ledger.productQuality = 45 + Math.floorMod(basis/37,21);
        ledger.productionEfficiency = 45 + Math.floorMod(basis/41,21);
        ledger.importExportCapacity = node == null ? 35 : 65;
        ledger.leadershipCompetence = 42 + Math.floorMod(basis/43,24);
        ledger.nextResolutionWorldTurn = worldTurn + cadence(world,ledger,0);
        return ledger;
    }

    private static void observe(World world, GamePanel game, DeferredFactionLedgerRecord ledger) {
        int rooms=0,rivalRooms=0,people=0,popCapacity=0,popUsed=0,dead=0,requests=0;
        for(Faction owner:world.roomFactions){if(sameFamily(owner,ledger.faction))rooms++;else if(owner!=null&&owner!=Faction.NONE)rivalRooms++;}
        for(NpcEntity npc:world.npcs)if(npc!=null&&sameFamily(npc.faction,ledger.faction))people++;
        for(RoomPopulationLedger population:world.roomPopulationLedgers)if(population!=null&&sameFamily(population.faction,ledger.faction)){
            popCapacity+=Math.max(0,population.capacity);popUsed+=Math.max(0,population.assigned);dead+=Math.max(0,population.dead);
        }
        for(PersonnelReplacementRequest request:world.replacementQueue)if(request!=null&&sameFamily(request.faction,ledger.faction))requests++;

        int shipments=0,arrived=0,delayed=0,intercepted=0,cargoValue=0;
        for(ShipmentProvenanceRecord shipment:world.shipmentRecords)if(shipment!=null&&sameFamily(shipment.destinationFaction,ledger.faction)){
            shipments++;cargoValue+=Math.max(0,shipment.cargoValue);
            if("ARRIVED".equals(shipment.status)||"DELIVERED".equals(shipment.status))arrived++;
            else if("DELAYED".equals(shipment.status)||"SCHEDULED".equals(shipment.status))delayed++;
            else if("INTERCEPTED".equals(shipment.status))intercepted++;
        }
        int rawCapacity=0,rawRemaining=0;
        for(RawMaterialSupplyReserveRecord reserve:world.rawMaterialSupplyReserves)if(reserve!=null&&sameFamily(reserve.faction,ledger.faction)){
            rawCapacity+=Math.max(0,reserve.capacity);rawRemaining+=Math.max(0,reserve.remaining);
        }
        String events=(safe(world.zoneConflictLossHistory,"")+" "+safe(world.zoneHistory,"")).toLowerCase(Locale.ROOT);
        String production=safe(world.zoneProductionHistory,"").toLowerCase(Locale.ROOT);
        int eventSeverity=Math.max(termScore(events,"blockade","interdiction","closure","quake","collapse","fire","outbreak","quarantine","seizure"),
                TopDownWorldEventAuthority.pressureFor(world,ledger.faction));
        int warehouseRisk=termScore(events,"warehouse","stock stolen","spoiled","burned","confiscated","contaminated");
        int machineDamage=termScore(events+" "+production,"sabotage","damaged machine","breakdown","degraded","inefficient","waste");
        int qualityDamage=termScore(events+" "+production,"counterfeit","contaminated","adulterated","defective","quality dropped");
        int productionSupport=termScore(production,"produced","completed","operational","output","export");
        int heat=game==null?0:Math.min(100,Math.max(0,game.suspicion)+Math.max(0,game.gangHeat)/2
                +Math.max(0,game.factionMarketPressure.getOrDefault(ledger.faction,0)));
        int leader=leadership(game,ledger.faction);
        int schemes=schemePressure(game,ledger.faction);
        boolean importNode=FactionImportNodeGenerationAuthority.primaryNode(world,ledger.faction)!=null;

        ledger.strength=blend(ledger.strength,clamp(24+rooms*7+people*3));
        ledger.influence=blend(ledger.influence,clamp(25+rooms*8+Math.min(20,people*2)));
        ledger.wealth=blend(ledger.wealth,clamp(30+rooms*3+Math.min(45,cargoValue/100)));
        ledger.populationPressure=blend(ledger.populationPressure,popCapacity<=0?Math.min(100,people*8):clamp(popUsed*100/Math.max(1,popCapacity)));
        ledger.personnelLosses=blend(ledger.personnelLosses,clamp(dead*12+requests*8));
        ledger.reinforcementDemand=blend(ledger.reinforcementDemand,clamp(requests*18));
        ledger.supplierReliability=blend(ledger.supplierReliability,clamp(55+arrived*7-delayed*9-intercepted*20));
        ledger.routeSafety=blend(ledger.routeSafety,clamp(68+arrived*4-delayed*8-intercepted*22-eventSeverity));
        ledger.shipmentPressure=blend(ledger.shipmentPressure,clamp(shipments*4+delayed*16+intercepted*24));
        ledger.warehouseVulnerability=blend(ledger.warehouseVulnerability,clamp(15+warehouseRisk+rivalRooms*2));
        ledger.rawMaterialAvailability=blend(ledger.rawMaterialAvailability,rawCapacity<=0?45:clamp(rawRemaining*100/Math.max(1,rawCapacity)));
        ledger.machineReliability=blend(ledger.machineReliability,clamp(62-machineDamage+productionSupport/2));
        ledger.productQuality=blend(ledger.productQuality,clamp(60-qualityDamage+productionSupport/3));
        ledger.productionEfficiency=blend(ledger.productionEfficiency,clamp(55-machineDamage/2+productionSupport));
        ledger.importExportCapacity=blend(ledger.importExportCapacity,clamp((importNode?62:30)+Math.min(25,shipments*5)));
        ledger.rivalInterference=blend(ledger.rivalInterference,clamp(10+rivalRooms*4+eventSeverity/2));
        ledger.leadershipCompetence=blend(ledger.leadershipCompetence,leader);
        ledger.schemePressure=blend(ledger.schemePressure,schemes);
        ledger.eventPressure=blend(ledger.eventPressure,clamp(eventSeverity));
        ledger.playerHeat=blend(ledger.playerHeat,heat);
    }

    private static String resolve(World world, DeferredFactionLedgerRecord ledger, long worldTurn) {
        Probability probability=calculateProbability(ledger);
        String focus=chooseFocus(world,ledger);
        int roll=1+Math.floorMod(Objects.hash(world.seed,ledger.id,ledger.resolutionCount,worldTurn,focus),100);
        boolean success=roll<=probability.chancePct();
        int magnitude=2+Math.floorMod(Objects.hash(ledger.id,worldTurn,"magnitude"),4);
        String consequence=applyConsequence(world,ledger,focus,success,magnitude,worldTurn);
        ledger.resolutionCount++;
        ledger.lastResolutionWorldTurn=worldTurn;
        ledger.lastChancePct=probability.chancePct();
        ledger.lastRoll=roll;
        ledger.lastFocus=focus;
        ledger.factorSummary=probability.explanation();
        ledger.lastOutcome=(success?"SUCCESS":"SETBACK")+": "+ledger.faction.label+" "+focus+" resolved with roll "+roll
                +" against "+probability.chancePct()+"%; "+consequence;
        ledger.nextResolutionWorldTurn=worldTurn+cadence(world,ledger,ledger.resolutionCount);
        addEvent(ledger,"T"+worldTurn+" "+ledger.lastOutcome);
        return ledger.lastOutcome+" Next review turn "+ledger.nextResolutionWorldTurn+".";
    }

    private static String chooseFocus(World world, DeferredFactionLedgerRecord ledger) {
        int reinforcement=Math.max(ledger.personnelLosses,ledger.reinforcementDemand);
        int shipment=Math.max(ledger.shipmentPressure,100-ledger.routeSafety);
        int production=Math.max(100-ledger.rawMaterialAvailability,Math.max(100-ledger.machineReliability,100-ledger.productionEfficiency));
        int influence=Math.max(ledger.rivalInterference,Math.max(ledger.schemePressure,Math.max(ledger.eventPressure,ledger.playerHeat)));
        if(hasRequests(world,ledger.faction)&&reinforcement>=shipment&&reinforcement>=production&&reinforcement>=influence)return"reinforcement network";
        if(hasActiveShipments(world,ledger.faction)&&shipment>=production&&shipment>=influence)return"shipment route";
        if(hasRawReserves(world,ledger.faction)&&production>=influence)return"production supply";
        return"faction influence";
    }

    private static String applyConsequence(World world, DeferredFactionLedgerRecord ledger, String focus, boolean success,
                                           int magnitude, long worldTurn) {
        int shift=magnitude*GamePanel.TURNS_PER_HOUR;
        if("shipment route".equals(focus)){
            int affected=0;
            for(ShipmentProvenanceRecord shipment:world.shipmentRecords)if(shipment!=null&&sameFamily(shipment.destinationFaction,ledger.faction)
                    &&("SCHEDULED".equals(shipment.status)||"DELAYED".equals(shipment.status))){
                long delta=success?-shift:shift;
                shipment.earliestArrivalWorldTurn=Math.max(worldTurn,shipment.earliestArrivalWorldTurn+delta);
                shipment.latestArrivalWorldTurn=Math.max(shipment.earliestArrivalWorldTurn,shipment.latestArrivalWorldTurn+delta);
                if(success&&shipment.earliestArrivalWorldTurn<=worldTurn)shipment.status="ARRIVED";
                else if(!success)shipment.status="DELAYED";
                shipment.delayRiskPct=clamp(shipment.delayRiskPct+(success?-magnitude:magnitude));
                affected++;
            }
            ledger.routeSafety=clamp(ledger.routeSafety+(success?magnitude:-magnitude));
            ledger.supplierReliability=clamp(ledger.supplierReliability+(success?magnitude:-magnitude));
            ledger.shipmentPressure=clamp(ledger.shipmentPressure+(success?-magnitude:magnitude));
            return affected+" incoming shipment(s) "+(success?"advanced":"delayed")+" by "+shift+" turn(s).";
        }
        if("reinforcement network".equals(focus)){
            int affected=0;
            for(PersonnelReplacementRequest request:world.replacementQueue)if(request!=null&&sameFamily(request.faction,ledger.faction)){
                int old=request.dueTurn;
                request.dueTurn=success?Math.max((int)Math.min(Integer.MAX_VALUE,worldTurn),request.dueTurn-shift):request.dueTurn+shift;
                request.expiresTurn=Math.max(request.dueTurn+1,request.expiresTurn+(request.dueTurn-old));
                affected++;
            }
            ledger.reinforcementDemand=clamp(ledger.reinforcementDemand+(success?-magnitude:magnitude));
            ledger.personnelLosses=clamp(ledger.personnelLosses+(success?-magnitude:0));
            return affected+" reinforcement arrival(s) "+(success?"advanced":"delayed")+" by "+shift+" turn(s).";
        }
        if("production supply".equals(focus)){
            int affected=0;
            for(RawMaterialSupplyReserveRecord reserve:world.rawMaterialSupplyReserves)if(reserve!=null&&sameFamily(reserve.faction,ledger.faction)){
                if(success&&reserve.remaining<reserve.capacity){reserve.remaining++;affected++;}
                else if(!success){reserve.nextRestockWorldTurn+=shift;if(reserve.remaining>0&&isExternal(reserve)){reserve.remaining--;affected++;}}
            }
            ledger.rawMaterialAvailability=clamp(ledger.rawMaterialAvailability+(success?magnitude:-magnitude));
            ledger.machineReliability=clamp(ledger.machineReliability+(success?magnitude:-magnitude));
            ledger.productQuality=clamp(ledger.productQuality+(success?Math.max(1,magnitude/2):-Math.max(1,magnitude/2)));
            ledger.productionEfficiency=clamp(ledger.productionEfficiency+(success?magnitude:-magnitude));
            return affected+" material reserve(s) "+(success?"improved":"lost stock or received a later refill")+".";
        }
        ledger.strength=clamp(ledger.strength+(success?magnitude:-magnitude));
        ledger.influence=clamp(ledger.influence+(success?magnitude:-magnitude));
        ledger.wealth=clamp(ledger.wealth+(success?Math.max(1,magnitude/2):-Math.max(1,magnitude/2)));
        ledger.rivalInterference=clamp(ledger.rivalInterference+(success?-magnitude:magnitude));
        return "persistent strength, influence, wealth, and rival-pressure values shifted by "+magnitude+" point(s).";
    }

    private static LinkedHashSet<Faction> participatingFactions(World world) {
        LinkedHashSet<Faction> factions=new LinkedHashSet<>();
        for(MapObjectState node:world.mapObjects)if(FactionImportNodeGenerationAuthority.isImportNode(node))addFaction(factions,parseFaction(MapObjectState.stockValue(node.stockState,"faction")));
        for(Faction faction:world.roomFactions)addFaction(factions,faction);
        for(NpcEntity npc:world.npcs)if(npc!=null)addFaction(factions,npc.faction);
        for(ShipmentProvenanceRecord shipment:world.shipmentRecords)if(shipment!=null)addFaction(factions,shipment.destinationFaction);
        for(PersonnelReplacementRequest request:world.replacementQueue)if(request!=null)addFaction(factions,request.faction);
        for(RawMaterialSupplyReserveRecord reserve:world.rawMaterialSupplyReserves)if(reserve!=null)addFaction(factions,reserve.faction);
        if(factions.isEmpty())addFaction(factions,preferredFaction(world));
        return factions;
    }

    private static Faction preferredFaction(World world) {
        MapObjectState node=FactionImportNodeGenerationAuthority.primaryNode(world,Faction.NONE);
        if(node!=null){Faction parsed=parseFaction(MapObjectState.stockValue(node.stockState,"faction"));if(parsed!=Faction.NONE)return parsed;}
        Faction dominant=world.dominantContinuityFactionForZone();
        if(dominant!=null&&dominant!=Faction.NONE)return dominant;
        Faction byZone=FactionInventoryStockAuthority.factionForZone(world.zoneType);
        return byZone==null?Faction.NONE:byZone;
    }

    private static void deduplicateFamilyLedgers(World world) {
        if (world == null || world.deferredFactionLedgers == null || world.deferredFactionLedgers.size() < 2) return;
        ArrayList<DeferredFactionLedgerRecord> kept = new ArrayList<>();
        for (DeferredFactionLedgerRecord candidate : world.deferredFactionLedgers) {
            if (candidate == null) continue;
            int familyIndex = -1;
            for (int i = 0; i < kept.size(); i++) {
                if (sameFamily(kept.get(i).faction, candidate.faction)) {
                    familyIndex = i;
                    break;
                }
            }
            if (familyIndex < 0) {
                kept.add(candidate);
                continue;
            }
            DeferredFactionLedgerRecord existing = kept.get(familyIndex);
            DeferredFactionLedgerRecord preferred = candidate.lastResolutionWorldTurn > existing.lastResolutionWorldTurn
                    ? candidate : existing;
            DeferredFactionLedgerRecord secondary = preferred == candidate ? existing : candidate;
            preferred.resolutionCount = Math.max(preferred.resolutionCount, secondary.resolutionCount);
            for (String event : secondary.recentEvents) if (!preferred.recentEvents.contains(event)) addEvent(preferred, event);
            kept.set(familyIndex, preferred);
        }
        if (kept.size() != world.deferredFactionLedgers.size()) {
            world.deferredFactionLedgers.clear();
            world.deferredFactionLedgers.addAll(kept);
        }
    }

    private static DeferredFactionLedgerRecord findExact(World world,Faction faction){for(DeferredFactionLedgerRecord ledger:world.deferredFactionLedgers)if(ledger!=null&&ledger.faction==faction)return ledger;return null;}
    private static void addFaction(LinkedHashSet<Faction> factions,Faction faction){
        if(faction==null||faction==Faction.NONE||factions.size()>=MAX_FACTION_LEDGERS)return;
        for(Faction existing:factions)if(sameFamily(existing,faction))return;
        factions.add(faction);
    }
    private static Faction parseFaction(String value){try{return Faction.valueOf(value);}catch(Exception ignored){return Faction.NONE;}}
    private static boolean sameFamily(Faction a,Faction b){return FactionIdentityAuthority.sameFamily(a,b);}
    private static boolean hasRequests(World world,Faction faction){for(PersonnelReplacementRequest request:world.replacementQueue)if(request!=null&&sameFamily(request.faction,faction))return true;return false;}
    private static boolean hasActiveShipments(World world,Faction faction){for(ShipmentProvenanceRecord shipment:world.shipmentRecords)if(shipment!=null&&sameFamily(shipment.destinationFaction,faction)&&("SCHEDULED".equals(shipment.status)||"DELAYED".equals(shipment.status)))return true;return false;}
    private static boolean hasRawReserves(World world,Faction faction){for(RawMaterialSupplyReserveRecord reserve:world.rawMaterialSupplyReserves)if(reserve!=null&&sameFamily(reserve.faction,faction))return true;return false;}
    private static boolean isExternal(RawMaterialSupplyReserveRecord reserve){String text=(safe(reserve.sourceLocality,"")+" "+safe(reserve.route,"")+" "+safe(reserve.sourceClass,"")).toLowerCase(Locale.ROOT);return text.contains("outside")||text.contains("import")||text.contains("rail");}
    private static int leadership(GamePanel game,Faction faction){if(game==null)return 50;int total=0,count=0;for(FactionStrategicPlan plan:game.factionStrategicPlans)if(plan!=null&&sameFamily(plan.faction,faction)){total+=clamp(30+plan.ambition/2+plan.success*2-plan.failure*2);count++;}return count==0?50:total/count;}
    private static int schemePressure(GamePanel game,Faction faction){if(game==null)return 10;int value=0;for(FactionStrategicPlan plan:game.factionStrategicPlans)if(plan!=null&&sameFamily(plan.faction,faction)){if(plan.scheme!=null&&!plan.scheme.isBlank())value+=18;if("EXECUTION".equals(plan.phase))value+=15;}return clamp(value);}
    private static int cadence(World world,DeferredFactionLedgerRecord ledger,int cycle){int variance=Math.floorMod(Objects.hash(world.seed,ledger.id,cycle,"distant-cadence"),RESOLUTION_VARIANCE_TURNS*2+1)-RESOLUTION_VARIANCE_TURNS;return Math.max(2*GamePanel.TURNS_PER_HOUR,RESOLUTION_BASE_TURNS+variance);}
    private static int termScore(String text,String...terms){String low=safe(text,"").toLowerCase(Locale.ROOT);int score=0;for(String term:terms)if(low.contains(term.toLowerCase(Locale.ROOT)))score+=12;return clamp(score);}
    private static int blend(int prior,int observed){return clamp((prior*3+observed)/4);}
    private static int average(int...values){if(values.length==0)return 0;int total=0;for(int value:values)total+=value;return total/values.length;}
    private static int clamp(int value){return Math.max(0,Math.min(100,value));}
    private static String safe(String value,String fallback){return value==null||value.isBlank()?fallback:value;}
    private static void addEvent(DeferredFactionLedgerRecord ledger,String event){ledger.recentEvents.add(event);while(ledger.recentEvents.size()>MAX_RECENT_EVENTS)ledger.recentEvents.remove(0);}
}
