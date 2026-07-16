package mechanist;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;

/** Finite raw material stock with explicit extraction, salvage, shipment, or assumed-source records. */
final class RawMaterialSupplyReserveRecord {
    String id = "raw-material.unassigned";
    String itemName = "Raw earth";
    Faction faction = Faction.NONE;
    String sourceClass = "bounded faction reserve";
    String sourceLabel = "unassigned material source";
    String sourceFacilityId = "";
    String sourceLocality = "local";
    String supplier = "unrecorded supplier";
    String assumptionReason = "no source assumption required";
    String eventModifier = "no active material-route event";
    String route = "material source -> trader shelf";
    boolean sourceReviewRequired = false;
    int capacity = 3;
    int remaining = 3;
    int restockIntervalTurns = GamePanel.TURNS_PER_HOUR * GamePanel.HOURS_PER_DAY * 5;
    long nextRestockWorldTurn = restockIntervalTurns;

    String saveLine() {
        return enc(id) + "|" + enc(itemName) + "|" + (faction == null ? Faction.NONE.name() : faction.name())
                + "|" + enc(sourceClass) + "|" + enc(sourceLabel) + "|" + enc(sourceFacilityId) + "|"
                + enc(sourceLocality) + "|" + enc(supplier) + "|" + enc(assumptionReason) + "|"
                + enc(eventModifier) + "|" + enc(route) + "|" + sourceReviewRequired + "|" + capacity
                + "|" + remaining + "|" + restockIntervalTurns + "|" + nextRestockWorldTurn;
    }

    static RawMaterialSupplyReserveRecord parse(String line) {
        try {
            String[] a = line.split("\\|", 16);
            if (a.length < 16) return null;
            RawMaterialSupplyReserveRecord r = new RawMaterialSupplyReserveRecord();
            r.id=dec(a[0]); r.itemName=dec(a[1]); r.faction=Faction.valueOf(a[2]); r.sourceClass=dec(a[3]);
            r.sourceLabel=dec(a[4]); r.sourceFacilityId=dec(a[5]); r.sourceLocality=dec(a[6]);
            r.supplier=dec(a[7]); r.assumptionReason=dec(a[8]); r.eventModifier=dec(a[9]); r.route=dec(a[10]);
            r.sourceReviewRequired=Boolean.parseBoolean(a[11]); r.capacity=Math.max(1,Integer.parseInt(a[12]));
            r.remaining=Math.max(0,Math.min(r.capacity,Integer.parseInt(a[13])));
            r.restockIntervalTurns=Math.max(1,Integer.parseInt(a[14]));
            r.nextRestockWorldTurn=Math.max(0L,Long.parseLong(a[15]));
            return r;
        } catch (Exception ignored) { return null; }
    }

    String playerLine() {
        return itemName + " material supply: " + sourceClass + " from " + sourceLabel + "; " + sourceLocality
                + "; supplier " + supplier + "; " + remaining + "/" + capacity + " unit(s); "
                + eventModifier + "; " + assumptionReason + (sourceReviewRequired ? "; source review required" : "")
                + "; " + (remaining > 0 ? "next refill" : "depleted until refill") + " at world turn "
                + nextRestockWorldTurn + ".";
    }

    private static String enc(String value) { return Base64.getUrlEncoder().withoutPadding().encodeToString((value==null?"":value).getBytes(StandardCharsets.UTF_8)); }
    private static String dec(String value) { return new String(Base64.getUrlDecoder().decode(value),StandardCharsets.UTF_8); }
}

final class RawMaterialSupplyProvenanceAuthority {
    private record Source(String sourceClass, String label, String facilityId, String locality, String supplier,
                          String assumption, String event, String route, boolean review, int capacity,
                          int restockTurns) { }

    private RawMaterialSupplyProvenanceAuthority() { }

    static void apply(TraderSession trader, World world, Faction faction, long worldTurn, int localTurn) {
        if (trader == null || world == null) return;
        Faction owner = safeFaction(faction);
        boolean extraction = findRoom(world,owner,"mine","mining","quarry","excavat","raw earth","mineral") >= 0
                || hasLedgerSource(world,owner,"mine","mining","quarry","excavat","ore","raw earth");
        boolean salvage = findRoom(world,owner,"salvage","recycl","scrap","junk","waste yard") >= 0
                || hasLedgerSource(world,owner,"salvage","recycl","scrap","scaveng");
        boolean biomass = findRoom(world,owner,"waste biomass","compost","reclamation","fungus farm","algae vat") >= 0;
        boolean rail = hasRailAccess(world);
        boolean blocked = importRestricted(world);
        if (extraction) {
            ensureOffer(trader,"Raw earth","finite local extraction stock.");
            ensureOffer(trader,"Quarried stone aggregate","finite local quarry stock.");
        }
        if (salvage) {
            ensureOffer(trader,"Ferric scrap","finite locally recovered metal stock.");
            ensureOffer(trader,"Recovered industrial salvage","finite sorted salvage stock.");
        }
        if (biomass) ensureOffer(trader,"Waste biomass","finite reclaimed organic feedstock.");
        if (rail && !blocked) ensureOffer(trader,"Refined metal stock","finite imported industrial feedstock.");
        if (rail && blocked) appendSummary(trader,"Raw material imports restricted: train and merchant feedstock routes are closed; only local extraction, salvage, event stock, or an explicitly assumed faction reserve may supply the shelf.");

        ArrayList<TradeOffer> managed=new ArrayList<>();
        for(TradeOffer offer:trader.offers) if(isManaged(offer)) managed.add(offer);
        int available=0,removed=0;
        for(TradeOffer offer:managed){
            RawMaterialSupplyReserveRecord reserve=reserveFor(world,owner,offer.name);
            if(reserve==null){
                Source source=resolve(world,owner,offer.name);
                if(source==null){trader.offers.remove(offer);removed++;continue;}
                reserve=create(world,owner,offer.name,source,worldTurn);world.rawMaterialSupplyReserves.add(reserve);
            }
            refresh(reserve,worldTurn);
            if(reserve.remaining<=0){trader.offers.remove(offer);removed++;continue;}
            attach(offer,reserve,world,owner,localTurn);available+=reserve.remaining;appendSummary(trader,reserve.playerLine());
        }
        if(!managed.isEmpty()||extraction||salvage||rail) appendSummary(trader,"Raw material supply: "+managed.size()
                +" traced offer(s), "+available+" reserve unit(s) available"
                +(removed>0?", "+removed+" unavailable or depleted offer(s) withheld":"")+".");
    }

    static String purchaseBlock(World world,TradeOffer offer,long worldTurn){
        if(offer==null||safe(offer.rawMaterialReserveId).isBlank())return"";
        RawMaterialSupplyReserveRecord r=reserveById(world,offer.rawMaterialReserveId);if(r==null)return"its raw-material supply ledger is unavailable";
        refresh(r,worldTurn);return r.remaining>0?"":r.sourceLabel+" is depleted until world turn "+r.nextRestockWorldTurn;
    }
    static boolean consume(World world,TradeOffer offer,long worldTurn){
        if(offer==null||safe(offer.rawMaterialReserveId).isBlank())return true;
        RawMaterialSupplyReserveRecord r=reserveById(world,offer.rawMaterialReserveId);if(r==null)return false;refresh(r,worldTurn);if(r.remaining<=0)return false;r.remaining--;return true;
    }
    static void updateSessionAfterPurchase(TraderSession trader,World world,TradeOffer offer){
        if(trader==null||offer==null||safe(offer.rawMaterialReserveId).isBlank())return;
        RawMaterialSupplyReserveRecord r=reserveById(world,offer.rawMaterialReserveId);if(r==null)return;appendSummary(trader,r.playerLine());
        if(r.remaining<=0)trader.offers.removeIf(o->o!=null&&r.id.equals(o.rawMaterialReserveId));
    }
    static RawMaterialSupplyReserveRecord reserveFor(World world,Faction faction,String item){
        if(world==null)return null;Faction owner=safeFaction(faction);for(RawMaterialSupplyReserveRecord r:world.rawMaterialSupplyReserves)
            if(r!=null&&r.faction==owner&&ItemQuality.namesMatch(r.itemName,item))return r;return null;
    }
    static RawMaterialSupplyReserveRecord reserveById(World world,String id){
        if(world==null||safe(id).isBlank())return null;for(RawMaterialSupplyReserveRecord r:world.rawMaterialSupplyReserves)if(r!=null&&id.equals(r.id))return r;return null;
    }

    private static RawMaterialSupplyReserveRecord create(World world,Faction faction,String item,Source source,long turn){
        RawMaterialSupplyReserveRecord r=new RawMaterialSupplyReserveRecord();r.id="raw-material."+Math.abs(Objects.hash(world.seed,faction.name(),item));
        r.itemName=item;r.faction=faction;r.sourceClass=source.sourceClass();r.sourceLabel=source.label();r.sourceFacilityId=source.facilityId();
        r.sourceLocality=source.locality();r.supplier=source.supplier();r.assumptionReason=source.assumption();r.eventModifier=source.event();
        r.route=source.route();r.sourceReviewRequired=source.review();r.capacity=source.capacity();r.remaining=source.capacity();
        r.restockIntervalTurns=source.restockTurns();r.nextRestockWorldTurn=Math.max(0L,turn)+source.restockTurns();return r;
    }

    private static Source resolve(World world,Faction faction,String item){
        String low=ItemQuality.stripQuality(item).toLowerCase(Locale.ROOT);String history=safe(world.zoneConflictLossHistory).toLowerCase(Locale.ROOT);
        boolean blocked=importRestricted(world);String event=blocked?"outside material routes blocked":"no active material-route event";
        for(ZoneConflictLossRecord e:HistoricalConflictLossApi.parseConflictLossLedger(world.zoneConflictLossHistory)){
            String text=(safe(e.eventType)+" "+safe(e.actor)+" "+safe(e.affectedStock)+" "+safe(e.destination)+" "+safe(e.historyNote)).toLowerCase(Locale.ROOT);
            if(!rawText(text)||!contains(text,"relief","seizure","confiscat","recovery","salvage","emergency"))continue;
            String label=safe(e.destination).isBlank()?e.eventType:e.destination;String cls=contains(text,"seizure","confiscat")?"event-seized material":"world-event relief material";
            return new Source(cls,label,safe(e.sourceFacilityId),"event-supplied",safe(e.actor),"no source assumption required",e.eventType,
                    safe(e.sourceFacilityId)+" -> "+e.eventType+" by "+e.actor+" -> "+label+" -> raw-material shelf",false,3,days(10));
        }
        for(ZoneStockMovementRecord m:ProductionDistributionApi.parseStockMovementLedger(world.zoneStockMovementHistory)){
            String text=(safe(m.sourceFacilityId)+" "+safe(m.destination)+" "+safe(m.movementKind)+" "+safe(m.itemSamples)+" "+safe(m.historyNote)).toLowerCase(Locale.ROOT);
            if((safe(m.sourceFacilityId).contains("unmatched")||safe(m.sourceFacilityId).isBlank())
                    &&(safe(m.destination).contains("unassigned")||safe(m.destination).isBlank()))continue;
            if(!itemMatchesLedger(low,text)||!controllerCompatible(faction,m.controller))continue;
            String movement=safe(m.movementKind).toLowerCase(Locale.ROOT);
            String cls=contains(movement,"recycl")?"local recycling":contains(movement,"scaveng")?"local scavenging":contains(movement,"salvage")?"local salvage":"facility stockpile";
            return new Source(cls,safe(m.destination),safe(m.sourceFacilityId),"local",safe(m.controller),"no source assumption required",event,
                    safe(m.sourceFacilityId)+" -> "+m.movementKind+" -> "+m.destination+" -> raw-material shelf",false,6,days(4));
        }
        for(ZoneProductionOutputRecord o:ProductionFacilityOutputSimulationApi.parseProductionLedger(world.zoneProductionHistory)){
            String text=(safe(o.facilityId)+" "+safe(o.facilityPurpose)+" "+safe(o.outputFocus)+" "+safe(o.sampleItems)).toLowerCase(Locale.ROOT);
            if(safe(o.facilityId).contains("unmatched")&&(safe(o.controller).contains("unknown")||safe(o.controller).contains("unrecorded")))continue;
            if(!itemMatchesLedger(low,text)||!controllerCompatible(faction,o.controller))continue;String label=safe(o.facilityId).isBlank()?o.facilityPurpose:o.facilityId;
            String cls=contains(text,"mine","ore","excavat")?"local mining":contains(text,"quarr","stone")?"local quarrying":contains(text,"salvage","scrap")?"local salvage":"local material processing";
            return new Source(cls,label,safe(o.facilityId),"local",safe(o.controller),"no source assumption required",event,
                    label+" -> faction material stockpile -> raw-material shelf",false,Math.max(3,Math.min(16,o.batches*3)),days(3));
        }
        int roomId=findRoomForItem(world,faction,low);if(roomId>=0){RoomProfile room=world.roomProfiles.get(roomId);String text=(safe(room.name)+" "+safe(room.descriptor)).toLowerCase(Locale.ROOT);
            String cls=contains(text,"mine","mining","excavat")?"local mining":contains(text,"quarr","stone")?"local quarrying":contains(text,"recycl")?"local recycling":contains(text,"salvage","scrap","junk")?"local salvage":"facility stockpile";
            return new Source(cls,room.name,"room."+roomId,"local",faction.label+" room owner","no source assumption required",event,
                    room.name+" -> faction material stockpile -> raw-material shelf",false,5,days(5));}
        if(!blocked&&hasRailAccess(world)){
            String cls=importClass(faction);return new Source(cls,"arcology material freight train","rail.material.intake","outside-sector",faction.label+" freight supplier",
                    "no source assumption required","import route open","outside-sector supplier -> rail intake -> faction stockpile -> raw-material shelf",false,5,days(8));}
        return new Source("bounded faction reserve",faction.label+" assumed material reserve","faction.raw.reserve","assumed local reserve",faction.label,
                "local production or trade requested this material before its extraction chain was fully simulated",event,
                faction.label+" assumed reserve -> raw-material shelf",true,1,days(blocked?14:10));
    }

    private static void attach(TradeOffer offer,RawMaterialSupplyReserveRecord r,World world,Faction faction,int turn){offer.rawMaterialReserveId=r.id;
        ItemProvenanceRecord p=ItemProvenanceRecord.of(offer.name,faction,r.sourceLabel,world,turn,r.sourceClass+"; finite reserve "+r.remaining+"/"+r.capacity,r.route);
        p.productionSource=r.sourceClass;p.producingFacility=r.sourceFacilityId;p.batchIssueTags=r.sourceReviewRequired?"assumed source; review required":r.eventModifier;
        p.chain=r.route+"; locality="+r.sourceLocality+"; supplier="+r.supplier+"; assumption="+r.assumptionReason;offer.provenance=p;
        String note=" "+r.sourceClass+" from "+r.sourceLabel+"; "+r.sourceLocality+"; reserve "+r.remaining+"/"+r.capacity+"; "+r.eventModifier+(r.sourceReviewRequired?"; assumed source requires review":"")+".";
        if(offer.description==null)offer.description=note.trim();else if(!offer.description.contains("reserve "+r.remaining+"/"+r.capacity))offer.description+=note;}
    private static int findRoomForItem(World world,Faction faction,String low){
        if(low.contains("earth")||low.contains("stone"))return findRoom(world,faction,"mine","mining","quarry","excavat","raw earth","mineral");
        if(low.contains("biomass"))return findRoom(world,faction,"waste biomass","compost","reclamation","fungus farm","algae vat");
        if(low.contains("scrap")||low.contains("salvage"))return findRoom(world,faction,"salvage","recycl","scrap","junk","waste yard");
        return findRoom(world,faction,"smelter","forge","material stockpile","warehouse","cargo");}
    private static int findRoom(World world,Faction faction,String...terms){for(int i=0;i<world.roomProfiles.size();i++){RoomProfile room=world.roomProfiles.get(i);Faction owner=i<world.roomFactions.size()?world.roomFactions.get(i):Faction.NONE;
        if(room==null||!factionCompatible(faction,owner))continue;String text=(safe(room.name)+" "+safe(room.descriptor)+" "+safe(room.featureText)).toLowerCase(Locale.ROOT);if(contains(text,terms))return i;}return-1;}
    private static boolean hasLedgerSource(World world,Faction faction,String...terms){for(ZoneProductionOutputRecord o:ProductionFacilityOutputSimulationApi.parseProductionLedger(world.zoneProductionHistory)){if(safe(o.facilityId).contains("unmatched"))continue;String text=(safe(o.facilityId)+" "+safe(o.facilityPurpose)+" "+safe(o.outputFocus)+" "+safe(o.sampleItems)).toLowerCase(Locale.ROOT);if(contains(text,terms)&&controllerCompatible(faction,o.controller))return true;}
        for(ZoneStockMovementRecord m:ProductionDistributionApi.parseStockMovementLedger(world.zoneStockMovementHistory)){if(safe(m.sourceFacilityId).contains("unmatched")&&safe(m.destination).contains("unassigned"))continue;String text=(safe(m.sourceFacilityId)+" "+safe(m.destination)+" "+safe(m.movementKind)+" "+safe(m.itemSamples)).toLowerCase(Locale.ROOT);if(contains(text,terms)&&controllerCompatible(faction,m.controller))return true;}return false;}
    private static boolean itemMatchesLedger(String item,String text){if(item.contains("earth")||item.contains("stone"))return contains(text,"earth","stone","quarr","mine","mining","excavat","mineral");if(item.contains("biomass"))return contains(text,"biomass","waste","compost","reclamation");if(item.contains("scrap")||item.contains("salvage"))return contains(text,"scrap","salvage","recycl","scaveng");return contains(text,"metal","smelt","forge","material","stockpile");}
    private static boolean rawText(String text){return contains(text,"raw material","earth","stone","ore","metal","scrap","salvage","biomass","feedstock");}
    private static String importClass(Faction faction){String name=faction.name();if(faction==Faction.NOBLE||name.startsWith("NOBLE_"))return"noble material import";if(faction==Faction.IMPERIAL_GUARD||faction==Faction.SORORITAS)return"military material import";if(faction==Faction.BANDIT||name.startsWith("GANGER_"))return"black-market material import";return"outside-sector merchant import";}
    private static boolean hasRailAccess(World world){return world.zoneType==ZoneType.NEUTRAL_RAIL_DEPOT||world.zoneType==ZoneType.TRAIN_SERVICE_YARD||findRoom(world,Faction.NONE,"rail intake","freight platform","train","rail hub")>=0;}
    private static boolean importRestricted(World world){String text=safe(world.zoneConflictLossHistory).toLowerCase(Locale.ROOT);return contains(text,"import restriction","import ban","blockade","interdiction","route closure","freight closure");}
    private static boolean controllerCompatible(Faction faction,String controller){String low=safe(controller).toLowerCase(Locale.ROOT).trim();if(low.isBlank()||contains(low,"unknown","unrecorded","none","neutral"))return true;String compact=compactIdentity(low);return compact.equals(compactIdentity(faction.name()))||compact.equals(compactIdentity(faction.label));}
    private static boolean factionCompatible(Faction wanted,Faction owner){if(owner==null||owner==Faction.NONE||wanted==Faction.NONE||owner==wanted)return true;return wanted.name().split("_")[0].equals(owner.name().split("_")[0]);}
    private static void ensureOffer(TraderSession trader,String item,String desc){ItemDef d=ItemCatalog.get(item);if(d==null)return;for(TradeOffer o:trader.offers)if(o!=null&&ItemQuality.namesMatch(o.name,item))return;trader.offers.add(new TradeOffer(item,d.category,d.basePrice,desc));}
    private static boolean isManaged(TradeOffer o){if(o==null)return false;String n=ItemQuality.stripQuality(o.name).toLowerCase(Locale.ROOT);return n.equals("raw earth")||n.equals("waste biomass")||n.equals("ferric scrap")||n.equals("refined metal stock")||n.equals("quarried stone aggregate")||n.equals("recovered industrial salvage");}
    private static void refresh(RawMaterialSupplyReserveRecord r,long turn){if(r!=null&&turn>=r.nextRestockWorldTurn){r.remaining=r.capacity;r.nextRestockWorldTurn=Math.max(0L,turn)+r.restockIntervalTurns;}}
    private static void appendSummary(TraderSession t,String line){if(t==null||safe(line).isBlank())return;if(safe(t.supplyChainSummary).isBlank())t.supplyChainSummary=line;else if(!t.supplyChainSummary.contains(line))t.supplyChainSummary+=" | "+line;}
    private static Faction safeFaction(Faction f){return f==null?Faction.NONE:f;}private static int days(int d){return GamePanel.TURNS_PER_HOUR*GamePanel.HOURS_PER_DAY*Math.max(1,d);}private static String safe(String s){return s==null?"":s;}
    private static boolean contains(String text,String...terms){String low=safe(text).toLowerCase(Locale.ROOT);for(String term:terms)if(term!=null&&!term.isBlank()&&low.contains(term.toLowerCase(Locale.ROOT)))return true;return false;}private static String compactIdentity(String s){return safe(s).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
}
