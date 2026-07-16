package mechanist;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Persisted sector conditions generated independently from faction schemes. */
final class TopDownWorldEventRecord {
    String id = "world-event.unassigned";
    String eventType = "CIVIC_OBSERVANCE";
    String title = "Civic observance";
    String status = "SCHEDULED";
    String scope = "sector";
    Faction targetFaction = Faction.NONE;
    String marketCategory = "all local commerce";
    long startWorldTurn = 0L;
    long endWorldTurn = 0L;
    int severity = 1;
    String eligibilitySummary = "No eligibility explanation was recorded.";
    String newsExposure = "Civic notices and local conversation.";
    int economySupplyPct = 0;
    int populationPressureDelta = 0;
    int shipmentDelayTurns = 0;
    int shipmentRiskDelta = 0;
    int reinforcementDelayTurns = 0;
    int deferredPressureDelta = 0;
    boolean importClosed = false;
    boolean exportClosed = false;
    boolean offMapSalesClosed = false;
    String roomMutationHook = "No physical room consequence is expected.";
    String vendorRestriction = "No vendor restriction.";
    String vendorException = "Local internal trade remains open.";
    String aftermath = "Normal operations resume when the event ends.";
    String consequenceSummary = "The event has not started.";
    long appliedWorldTurn = -1L;
    long recoveryWorldTurn = -1L;
    boolean positive = false;
    int generationIndex = 0;
    String mutationObjectId = "";
    int mutationRoomId = -1;
    String mutationMode = "none";
    String physicalStatus = "UNAPPLIED";
    String originalObjectLabel = "";
    String originalObjectStockState = "";
    int originalObjectGlyph = 0;
    String originalRoomName = "";
    String originalRoomDescriptor = "";
    String originalRoomFeatureText = "";
    String mutationSummary = "No physical facility mutation has been applied.";
    boolean createdMutationObject = false;

    String saveLine() {
        return enc(id) + "|" + enc(eventType) + "|" + enc(title) + "|" + enc(status) + "|" + enc(scope)
                + "|" + (targetFaction == null ? Faction.NONE.name() : targetFaction.name()) + "|" + enc(marketCategory)
                + "|" + startWorldTurn + "|" + endWorldTurn + "|" + severity + "|" + enc(eligibilitySummary)
                + "|" + enc(newsExposure) + "|" + economySupplyPct + "|" + populationPressureDelta
                + "|" + shipmentDelayTurns + "|" + shipmentRiskDelta + "|" + reinforcementDelayTurns
                + "|" + deferredPressureDelta + "|" + importClosed + "|" + exportClosed + "|" + offMapSalesClosed
                + "|" + enc(roomMutationHook) + "|" + enc(vendorRestriction) + "|" + enc(vendorException)
                + "|" + enc(aftermath) + "|" + enc(consequenceSummary) + "|" + appliedWorldTurn
                + "|" + recoveryWorldTurn + "|" + positive + "|" + generationIndex
                + "|" + enc(mutationObjectId) + "|" + mutationRoomId + "|" + enc(mutationMode)
                + "|" + enc(physicalStatus) + "|" + enc(originalObjectLabel) + "|" + enc(originalObjectStockState)
                + "|" + originalObjectGlyph + "|" + enc(originalRoomName) + "|" + enc(originalRoomDescriptor)
                + "|" + enc(originalRoomFeatureText) + "|" + enc(mutationSummary) + "|" + createdMutationObject;
    }

    static TopDownWorldEventRecord parse(String line) {
        try {
            String[] a = line.split("\\|", -1);
            if (a.length < 30) return null;
            TopDownWorldEventRecord r = new TopDownWorldEventRecord();
            r.id=dec(a[0]); r.eventType=dec(a[1]); r.title=dec(a[2]); r.status=dec(a[3]); r.scope=dec(a[4]);
            r.targetFaction=Faction.valueOf(a[5]); r.marketCategory=dec(a[6]); r.startWorldTurn=Long.parseLong(a[7]);
            r.endWorldTurn=Long.parseLong(a[8]); r.severity=Math.max(1,Math.min(5,Integer.parseInt(a[9])));
            r.eligibilitySummary=dec(a[10]); r.newsExposure=dec(a[11]); r.economySupplyPct=Integer.parseInt(a[12]);
            r.populationPressureDelta=Integer.parseInt(a[13]); r.shipmentDelayTurns=Integer.parseInt(a[14]);
            r.shipmentRiskDelta=Integer.parseInt(a[15]); r.reinforcementDelayTurns=Integer.parseInt(a[16]);
            r.deferredPressureDelta=Integer.parseInt(a[17]); r.importClosed=Boolean.parseBoolean(a[18]);
            r.exportClosed=Boolean.parseBoolean(a[19]); r.offMapSalesClosed=Boolean.parseBoolean(a[20]);
            r.roomMutationHook=dec(a[21]); r.vendorRestriction=dec(a[22]); r.vendorException=dec(a[23]);
            r.aftermath=dec(a[24]); r.consequenceSummary=dec(a[25]); r.appliedWorldTurn=Long.parseLong(a[26]);
            r.recoveryWorldTurn=Long.parseLong(a[27]); r.positive=Boolean.parseBoolean(a[28]);
            r.generationIndex=Math.max(0,Integer.parseInt(a[29]));
            if (a.length >= 42) {
                r.mutationObjectId=dec(a[30]); r.mutationRoomId=Integer.parseInt(a[31]); r.mutationMode=dec(a[32]);
                r.physicalStatus=dec(a[33]); r.originalObjectLabel=dec(a[34]); r.originalObjectStockState=dec(a[35]);
                r.originalObjectGlyph=Integer.parseInt(a[36]); r.originalRoomName=dec(a[37]);
                r.originalRoomDescriptor=dec(a[38]); r.originalRoomFeatureText=dec(a[39]);
                r.mutationSummary=dec(a[40]); r.createdMutationObject=Boolean.parseBoolean(a[41]);
            }
            return r;
        } catch (Exception ignored) { return null; }
    }

    String playerLine() {
        String faction = targetFaction == null || targetFaction == Faction.NONE ? "faction-neutral" : targetFaction.label;
        return title + " [" + status + "]: severity " + severity + "/5; " + scope + " scope, " + faction
                + ", market category " + marketCategory + "; starts turn " + startWorldTurn + ", ends turn " + endWorldTurn + ".";
    }

    String effectsLine() {
        return "Effects: economy supply " + signed(economySupplyPct) + "%; population pressure "
                + signed(populationPressureDelta) + "; shipment timing " + signed(shipmentDelayTurns)
                + " turn(s), risk " + signed(shipmentRiskDelta) + "%; reinforcement timing "
                + signed(reinforcementDelayTurns) + " turn(s); distant pressure " + signed(deferredPressureDelta) + ".";
    }

    private static String signed(int value) { return value > 0 ? "+" + value : Integer.toString(value); }
    private static String enc(String value) { return Base64.getUrlEncoder().withoutPadding().encodeToString((value==null?"":value).getBytes(StandardCharsets.UTF_8)); }
    private static String dec(String value) { return new String(Base64.getUrlDecoder().decode(value),StandardCharsets.UTF_8); }
}

final class TopDownWorldEventAuthority {
    static final int DAY_TURNS = GamePanel.TURNS_PER_HOUR * GamePanel.HOURS_PER_DAY;
    private static final int MAX_EVENT_RECORDS = 10;

    record TickResult(int generated, int activated, int recovered, long nextCheckWorldTurn, String summary) { }

    private enum Template {
        RELIEF_SHIPMENT("Relief shipment",true,"sector","food, water, and medicine",2,14,-10,-6,-12,-8,-12,false,false,false,
                "Relief intake space may be temporarily assigned at an import room.","Relief stock is rationed to local residents and faction dependants.","Ordinary internal vendors remain open while relief distribution is active.","Relief intake closes after reserves stabilize."),
        INFRASTRUCTURE_REPAIR("Infrastructure repair period",true,"level and import route","freight and reinforcement traffic",2,8,-3,-4,-10,-6,-8,false,false,false,
                "Damaged intake and service rooms become eligible for repair work.","Through traffic may queue around active repair crews.","Local internal vendors remain open and completed routes resume early.","Repair crews withdraw after route reliability is restored."),
        TRAIN_OUTAGE("Train outage",false,"sector import network","external freight",3,-12,5,12,18,10,22,true,false,false,
                "Train platforms and import halls may close until service is restored.","New outside-sector imports wait while train service is unavailable.","Local stock and internal faction vendors remain open while supplies last.","Rail service resumes gradually and delayed manifests retain their revised arrival windows."),
        EXPORT_BAN("External-sector export ban",false,"outside-sector trade","off-map exports",2,-8,2,4,6,0,12,false,true,true,
                "Customs and freight offices may be repurposed for inspection queues.","Off-map sales and export settlement are suspended.","Local and internal faction commerce remains open.","Held export cargo returns to ordinary trade after the decree expires."),
        TITHING_DECREE("Sector tithing decree",false,"sector","general commerce",2,-10,4,3,8,2,14,false,false,false,
                "Market and faction store rooms may receive temporary tithe collection points.","Market reserves face a temporary civic tithe.","Food relief and direct internal issue remain exempt.","Tithe collection ends at the stated turn and ordinary reserve policy resumes."),
        QUARANTINE("Public-health quarantine",false,"level","medicine and passenger traffic",3,-14,8,10,12,14,24,true,false,false,
                "Clinics and access halls may become quarantine checkpoints.","Passenger imports and unrestricted medical resale are suspended.","Clinic treatment, relief medicine, and internal food distribution remain available.","Checkpoints stand down after the observation period and delayed arrivals are re-evaluated."),
        SUPPLY_SHOCK("External supply shock",false,"outside-sector suppliers","raw materials and critical supplies",3,-16,5,8,15,5,20,false,false,false,
                "Warehouses and receiving rooms may receive shortage-control duties.","Outside-sector restocking slows and high-risk freight is withheld.","Existing local production and internal reserves remain usable.","Restocking cadence normalizes after supplier reliability recovers."),
        CIVIC_OBSERVANCE("Civic observance",true,"sector","local services",1,2,-1,0,-2,0,-2,false,false,false,
                "Public rooms may host observance notices without losing their ordinary purpose.","Some civic counters operate reduced hours.","Food, water, medicine, and internal faction issue remain open.","Ordinary schedules resume at the end of the observance.");

        final String title,scope,category,roomHook,restriction,exception,aftermath;
        final boolean positive,importClosed,exportClosed,offMapClosed;
        final int baseSeverity,economyPct,populationDelta,shipmentHours,shipmentRisk,reinforcementHours,deferredPressure;

        Template(String title,boolean positive,String scope,String category,int baseSeverity,int economyPct,
                 int populationDelta,int shipmentHours,int shipmentRisk,int reinforcementHours,int deferredPressure,
                 boolean importClosed,boolean exportClosed,boolean offMapClosed,String roomHook,String restriction,
                 String exception,String aftermath) {
            this.title=title;this.positive=positive;this.scope=scope;this.category=category;this.baseSeverity=baseSeverity;
            this.economyPct=economyPct;this.populationDelta=populationDelta;this.shipmentHours=shipmentHours;
            this.shipmentRisk=shipmentRisk;this.reinforcementHours=reinforcementHours;this.deferredPressure=deferredPressure;
            this.importClosed=importClosed;this.exportClosed=exportClosed;this.offMapClosed=offMapClosed;
            this.roomHook=roomHook;this.restriction=restriction;this.exception=exception;this.aftermath=aftermath;
        }
    }

    private TopDownWorldEventAuthority() { }

    static TickResult tick(GamePanel game, boolean sleeping) {
        if (game == null || game.world == null) return new TickResult(0,0,0,0L,"No world-event ledger is active.");
        TickResult result=tick(game.world,game,game.worldTurn);
        if (result.generated()>0||result.activated()>0||result.recovered()>0) {
            game.logEvent(result.summary());
            DebugLog.audit("TOP_DOWN_WORLD_EVENT",result.summary());
        }
        return result;
    }

    static TickResult tick(World world, GamePanel game, long worldTurn) {
        if (world == null) return new TickResult(0,0,0,0L,"No world-event ledger is active.");
        int activated=0,recovered=0,generated=0;
        ArrayList<String> changes=new ArrayList<>();
        for (TopDownWorldEventRecord event:world.topDownWorldEvents) {
            if (event==null) continue;
            if ("ACTIVE".equals(event.status)) WorldEventFacilityMutationAuthority.ensureActive(world,event);
            if ("SCHEDULED".equals(event.status)&&worldTurn>=event.startWorldTurn) {
                event.status="ACTIVE";apply(world,event,worldTurn);activated++;
                publishNews(game,world,event,"active");
                changes.add(event.title+" began: "+event.consequenceSummary);
            }
            if ("ACTIVE".equals(event.status)&&worldTurn>=event.endWorldTurn) {
                event.status="RECOVERED";event.recoveryWorldTurn=worldTurn;recovered++;
                WorldEventFacilityMutationAuthority.recover(world,event);
                publishNews(game,world,event,"recovered");
                changes.add(event.title+" ended. "+event.aftermath);
            }
        }
        if (!hasActiveOrScheduled(world)&&world.nextTopDownWorldEventCheckTurn<=0L)
            world.nextTopDownWorldEventCheckTurn=worldTurn+initialCadence(world);
        if (!hasActiveOrScheduled(world)&&worldTurn>=world.nextTopDownWorldEventCheckTurn) {
            TopDownWorldEventRecord event=generateCurated(world,worldTurn);
            world.topDownWorldEvents.add(event);event.status="ACTIVE";apply(world,event,worldTurn);
            publishNews(game,world,event,"active");
            activated++;generated++;changes.add(event.title+" began: "+event.consequenceSummary);
            world.nextTopDownWorldEventCheckTurn=event.endWorldTurn+recoveryCadence(world,event.generationIndex);
            prune(world);
        }
        String summary=changes.isEmpty()
                ?"World events: "+activeEvents(world).size()+" active; next eligibility review at world turn "+world.nextTopDownWorldEventCheckTurn+"."
                :"World event update: "+String.join(" ",changes)+" Next eligibility review at world turn "+world.nextTopDownWorldEventCheckTurn+".";
        return new TickResult(generated,activated,recovered,world.nextTopDownWorldEventCheckTurn,summary);
    }

    static TopDownWorldEventRecord scheduleCurated(World world,String eventType,long startWorldTurn) {
        if (world==null) return null;
        Template template=template(eventType);
        TopDownWorldEventRecord event=create(world,template,Math.max(0L,startWorldTurn),eligibility(template));
        world.topDownWorldEvents.add(event);
        world.nextTopDownWorldEventCheckTurn=Math.max(world.nextTopDownWorldEventCheckTurn,event.endWorldTurn+recoveryCadence(world,event.generationIndex));
        prune(world);
        return event;
    }

    static List<TopDownWorldEventRecord> activeEvents(World world) {
        if (world==null) return List.of();
        ArrayList<TopDownWorldEventRecord> active=new ArrayList<>();
        for (TopDownWorldEventRecord event:world.topDownWorldEvents)
            if (event!=null&&("ACTIVE".equals(event.status)||"SCHEDULED".equals(event.status))) active.add(event);
        active.sort(Comparator.comparingLong(event->event.startWorldTurn));
        return List.copyOf(active);
    }

    static int pressureFor(World world,Faction faction) {
        int pressure=0;
        for (TopDownWorldEventRecord event:activeEvents(world)) if (applies(event,faction)) pressure+=event.deferredPressureDelta;
        return clamp(pressure,0,100);
    }

    static String shipmentModifierSummary(World world) {
        ArrayList<String> parts=new ArrayList<>();
        for (TopDownWorldEventRecord event:activeEvents(world))
            if (event.shipmentDelayTurns!=0||event.shipmentRiskDelta!=0||event.importClosed||event.exportClosed)
                parts.add(event.title.toLowerCase(Locale.ROOT)+" (timing "+signed(event.shipmentDelayTurns)
                        +" turns, risk "+signed(event.shipmentRiskDelta)+"%)");
        return parts.isEmpty()?"":String.join(", ",parts);
    }

    static void applyToNewShipment(World world,ShipmentProvenanceRecord shipment,long worldTurn) {
        if (world==null||shipment==null) return;
        for (TopDownWorldEventRecord event:activeEvents(world)) if (applies(event,shipment.destinationFaction))
            applyShipment(event,shipment,worldTurn);
    }

    static void applyToNewReinforcement(World world,PersonnelReplacementRequest request) {
        if (world==null||request==null) return;
        for (TopDownWorldEventRecord event:activeEvents(world)) if (applies(event,request.faction)) shiftReinforcement(event,request);
    }

    static List<String> summaryLines(World world) {
        if (world==null) return List.of("World events: no world state is available.");
        List<TopDownWorldEventRecord> active=activeEvents(world);
        ArrayList<String> lines=new ArrayList<>();
        lines.add("World events: "+active.size()+" active or scheduled; next eligibility review at world turn "+world.nextTopDownWorldEventCheckTurn+".");
        if (active.isEmpty()) {
            TopDownWorldEventRecord latest=world.topDownWorldEvents.stream().filter(Objects::nonNull)
                    .max(Comparator.comparingLong(event->Math.max(event.recoveryWorldTurn,event.startWorldTurn))).orElse(null);
            lines.add(latest==null?"No top-down sector event has been recorded.":"Latest recovery: "+latest.title+". "+latest.aftermath);
            return lines;
        }
        for (TopDownWorldEventRecord event:active) {
            lines.add(event.playerLine());lines.add(event.effectsLine());
            lines.add("Notice: "+event.newsExposure+" Restriction: "+event.vendorRestriction+" Exception: "+event.vendorException);
            lines.add("Physical state: "+event.mutationSummary+" Aftermath: "+event.aftermath);
        }
        return lines;
    }

    static String commandReport(World world) { return String.join(" | ",summaryLines(world)); }

    private static void publishNews(GamePanel game,World world,TopDownWorldEventRecord event,String stage) {
        if(game==null||event==null)return;
        int day=game.currentInnDay();
        String zone=world==null||world.zoneType==null?"unknown zone":world.zoneType.label;
        String detail;
        if("recovered".equals(stage)) detail=event.title+" has ended. "+event.aftermath;
        else detail=event.title+" is active through world turn "+event.endWorldTurn+". Restriction: "
                +event.vendorRestriction+" Exception: "+event.vendorException+" Notice source: "+event.newsExposure;
        PlayerNewsEvent news=PlayerNewsEvent.create(game.turn,day,"world-event "+stage,event.title,
                event.targetFaction,zone,detail,Math.max(1,event.severity),game.seed);
        news.publicDay=day;
        game.playerNewsEvents.removeIf(existing->existing!=null&&Objects.equals(existing.category,news.category)
                &&Objects.equals(existing.subject,news.subject)&&Objects.equals(existing.detail,news.detail));
        game.playerNewsEvents.add(news);
        game.lastPlayerNewsReport="World-event notice: "+detail;
    }

    private static TopDownWorldEventRecord generateCurated(World world,long worldTurn) {
        ArrayList<Template> eligible=new ArrayList<>();int best=Integer.MIN_VALUE;
        for (Template template:Template.values()) {
            int score=eligibilityScore(world,template);if(score<0)continue;
            if(score>best){best=score;eligible.clear();}if(score==best)eligible.add(template);
        }
        Template chosen=eligible.isEmpty()?Template.CIVIC_OBSERVANCE:eligible.get(Math.floorMod(
                Objects.hash(world.seed,world.locationKey(),world.topDownWorldEventGenerationCount,"world-event"),eligible.size()));
        return create(world,chosen,worldTurn,eligibility(chosen));
    }

    private static TopDownWorldEventRecord create(World world,Template template,long start,String eligibility) {
        int index=++world.topDownWorldEventGenerationCount;
        int severity=clamp(template.baseSeverity+Math.floorMod(Objects.hash(world.seed,world.locationKey(),template.name(),index),3)-1,1,5);
        int durationDays=Math.max(1,severity+(template.positive?0:1));
        TopDownWorldEventRecord event=new TopDownWorldEventRecord();
        event.id="WORLD-EVENT-"+index+"-"+Math.abs(Objects.hash(world.seed,world.locationKey(),template.name(),start));
        event.eventType=template.name();event.title=template.title;event.scope=template.scope;event.marketCategory=template.category;
        event.startWorldTurn=start;event.endWorldTurn=start+(long)durationDays*DAY_TURNS;event.severity=severity;
        event.eligibilitySummary=eligibility;event.newsExposure=noticeFor(template,severity);
        event.economySupplyPct=scale(template.economyPct,severity);event.populationPressureDelta=scale(template.populationDelta,severity);
        event.shipmentDelayTurns=scale(template.shipmentHours*GamePanel.TURNS_PER_HOUR,severity);
        event.shipmentRiskDelta=scale(template.shipmentRisk,severity);
        event.reinforcementDelayTurns=scale(template.reinforcementHours*GamePanel.TURNS_PER_HOUR,severity);
        event.deferredPressureDelta=scale(template.deferredPressure,severity);
        event.importClosed=template.importClosed;event.exportClosed=template.exportClosed;event.offMapSalesClosed=template.offMapClosed;
        event.roomMutationHook=template.roomHook;event.vendorRestriction=template.restriction;event.vendorException=template.exception;
        event.aftermath=template.aftermath;event.positive=template.positive;event.generationIndex=index;
        return event;
    }

    private static void apply(World world,TopDownWorldEventRecord event,long worldTurn) {
        if(event.appliedWorldTurn>=0L)return;
        int shipments=0,reinforcements=0,reserves=0;
        for(ShipmentProvenanceRecord shipment:world.shipmentRecords)if(shipment!=null&&applies(event,shipment.destinationFaction)&&applyShipment(event,shipment,worldTurn))shipments++;
        for(PersonnelReplacementRequest request:world.replacementQueue)if(request!=null&&applies(event,request.faction)&&shiftReinforcement(event,request))reinforcements++;
        if(event.economySupplyPct>0){
            for(EssentialSupplyReserveRecord reserve:world.essentialSupplyReserves)if(reserve!=null&&applies(event,reserve.faction)&&reserve.remaining<reserve.capacity){reserve.remaining++;reserves++;}
            for(RawMaterialSupplyReserveRecord reserve:world.rawMaterialSupplyReserves)if(reserve!=null&&applies(event,reserve.faction)&&reserve.remaining<reserve.capacity){reserve.remaining++;reserves++;}
        }
        event.appliedWorldTurn=worldTurn;
        WorldEventFacilityMutationAuthority.activate(world,event);
        event.consequenceSummary=shipments+" shipment(s) adjusted, "+reinforcements+" reinforcement manifest(s) adjusted, "+reserves
                +" depleted reserve(s) replenished; "+event.mutationSummary+" "+event.vendorRestriction+" "+event.vendorException;
    }

    private static boolean applyShipment(TopDownWorldEventRecord event,ShipmentProvenanceRecord shipment,long worldTurn) {
        if("INTERCEPTED".equals(shipment.status)||"DELIVERED".equals(shipment.status))return false;
        int shift=event.shipmentDelayTurns;
        if(shift!=0){
            shipment.earliestArrivalWorldTurn=Math.max(worldTurn,shipment.earliestArrivalWorldTurn+shift);
            shipment.latestArrivalWorldTurn=Math.max(shipment.earliestArrivalWorldTurn,shipment.latestArrivalWorldTurn+shift);
            if(shift>0)shipment.status="DELAYED";else if(shipment.earliestArrivalWorldTurn<=worldTurn)shipment.status="ARRIVED";
        }
        shipment.delayRiskPct=clamp(shipment.delayRiskPct+event.shipmentRiskDelta,0,95);
        shipment.eventModifier=append(shipment.eventModifier,event.title.toLowerCase(Locale.ROOT));
        return shift!=0||event.shipmentRiskDelta!=0;
    }

    private static boolean shiftReinforcement(TopDownWorldEventRecord event,PersonnelReplacementRequest request) {
        int shift=event.reinforcementDelayTurns;if(shift==0)return false;
        request.dueTurn=clampLongToInt((long)request.dueTurn+shift);
        request.expiresTurn=Math.max(request.dueTurn+1,clampLongToInt((long)request.expiresTurn+shift));
        request.reason=append(request.reason,event.title.toLowerCase(Locale.ROOT));return true;
    }

    private static int eligibilityScore(World world,Template template) {
        int delayed=0,shipments=0,requests=world.replacementQueue.size(),shortages=0,people=world.npcs.size(),damage=0;
        for(ShipmentProvenanceRecord shipment:world.shipmentRecords)if(shipment!=null){shipments++;if("DELAYED".equals(shipment.status)||"INTERCEPTED".equals(shipment.status))delayed++;}
        for(EssentialSupplyReserveRecord reserve:world.essentialSupplyReserves)if(reserve!=null&&reserve.remaining<=0)shortages++;
        for(RawMaterialSupplyReserveRecord reserve:world.rawMaterialSupplyReserves)if(reserve!=null&&reserve.remaining<=0)shortages++;
        String history=(safe(world.zoneConflictLossHistory)+" "+safe(world.zoneHistory)).toLowerCase(Locale.ROOT);
        for(String term:List.of("quake","collapse","fire","damaged","outbreak"))if(history.contains(term))damage++;
        boolean importNode=FactionImportNodeGenerationAuthority.primaryNode(world,Faction.NONE)!=null;
        return switch(template){
            case RELIEF_SHIPMENT->shortages+requests+damage>0?80+shortages*10+requests*4+damage*5:-1;
            case INFRASTRUCTURE_REPAIR->delayed+damage>0?70+delayed*10+damage*8:-1;
            case TRAIN_OUTAGE->importNode&&shipments>0?38+shipments*3:-1;
            case EXPORT_BAN->shipments>0?34+shipments*2:-1;
            case TITHING_DECREE->world.rooms.isEmpty()?-1:28+world.rooms.size();
            case QUARANTINE->people>=8?30+Math.min(25,people):-1;
            case SUPPLY_SHOCK->shipments>0&&shortages==0?32+shipments*2:-1;
            case CIVIC_OBSERVANCE->24;
        };
    }

    private static String eligibility(Template template) {
        return switch(template){
            case RELIEF_SHIPMENT->"Recorded shortages, losses, delayed intake, or structural harm make relief suitable.";
            case INFRASTRUCTURE_REPAIR->"Delayed freight or recorded structural harm makes a repair period suitable.";
            case TRAIN_OUTAGE->"An operating import node and active freight create exposure to a train outage.";
            case EXPORT_BAN->"Active outside-sector freight makes an external export decree relevant.";
            case TITHING_DECREE->"Established rooms and commerce make a sector tithe collectible.";
            case QUARANTINE->"The local population is large enough for a public-health restriction to matter.";
            case SUPPLY_SHOCK->"Active outside suppliers create exposure to a distant supply disruption.";
            case CIVIC_OBSERVANCE->"A faction-neutral civic calendar condition is suitable for the sector.";
        };
    }

    private static String noticeFor(Template template,int severity) {
        String channel=template==Template.TRAIN_OUTAGE||template==Template.INFRASTRUCTURE_REPAIR?"train-station notices and freight-worker conversation"
                :template==Template.QUARANTINE?"clinic notices and civic announcements":"market notices, civic announcements, and local conversation";
        return template.title+" is exposed through "+channel+"; severity "+severity+"/5 and the end turn are stated.";
    }

    private static boolean hasActiveOrScheduled(World world){return!activeEvents(world).isEmpty();}
    private static boolean applies(TopDownWorldEventRecord event,Faction faction){return event!=null&&(event.targetFaction==null||event.targetFaction==Faction.NONE||FactionIdentityAuthority.sameFamily(event.targetFaction,faction));}
    private static Template template(String value){if(value!=null)try{return Template.valueOf(value.trim().toUpperCase(Locale.ROOT).replace(' ','_').replace('-','_'));}catch(Exception ignored){}return Template.CIVIC_OBSERVANCE;}
    private static int scale(int value,int severity){return(int)Math.round(value*(0.65+Math.max(1,severity)*0.18));}
    private static int initialCadence(World world){return DAY_TURNS+Math.floorMod(Objects.hash(world.seed,world.locationKey(),"world-event-initial"),DAY_TURNS+1);}
    private static int recoveryCadence(World world,int cycle){return DAY_TURNS+Math.floorMod(Objects.hash(world.seed,world.locationKey(),cycle,"world-event-recovery"),DAY_TURNS*2+1);}
    private static int clamp(int value,int min,int max){return Math.max(min,Math.min(max,value));}
    private static int clampLongToInt(long value){return(int)Math.max(0L,Math.min(Integer.MAX_VALUE,value));}
    private static String signed(int value){return value>0?"+"+value:Integer.toString(value);}
    private static String safe(String value){return value==null?"":value;}
    private static String append(String prior,String next){if(safe(next).isBlank())return safe(prior);if(safe(prior).isBlank())return next;if(prior.contains(next))return prior;return prior+"; "+next;}
    private static void prune(World world){while(world.topDownWorldEvents.size()>MAX_EVENT_RECORDS){int index=-1;for(int i=0;i<world.topDownWorldEvents.size();i++)if(world.topDownWorldEvents.get(i)!=null&&"RECOVERED".equals(world.topDownWorldEvents.get(i).status)){index=i;break;}if(index<0)index=0;world.topDownWorldEvents.remove(index);}}
}
