package mechanist;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;

/** Finite noble luxuries plus protected, normally non-tradeable rare draught custody. */
final class NobleLuxuryReserveRecord {
    String id = "luxury.unassigned";
    String itemName = "Noble preserved delicacy";
    Faction faction = Faction.NONE;
    String stockClass = "house-certified luxury";
    String sourceKind = "estate store";
    String sourceLabel = "noble household store";
    String sourceFacilityId = "";
    String route = "estate store -> luxury counter";
    String eventStatus = "no active tax, seizure, or blockade";
    String purpose = "prestige and gifting";
    int capacity = 2;
    int remaining = 2;
    int restockIntervalTurns = GamePanel.TURNS_PER_HOUR * GamePanel.HOURS_PER_DAY * 7;
    long nextRestockWorldTurn = restockIntervalTurns;

    String saveLine() {
        return enc(id)+"|"+enc(itemName)+"|"+(faction==null?Faction.NONE.name():faction.name())+"|"
                +enc(stockClass)+"|"+enc(sourceKind)+"|"+enc(sourceLabel)+"|"+enc(sourceFacilityId)+"|"
                +enc(route)+"|"+enc(eventStatus)+"|"+enc(purpose)+"|"+capacity+"|"+remaining+"|"
                +restockIntervalTurns+"|"+nextRestockWorldTurn;
    }
    static NobleLuxuryReserveRecord parse(String line) {
        try {
            String[] a=line.split("\\|",14); if(a.length<14)return null;
            NobleLuxuryReserveRecord r=new NobleLuxuryReserveRecord();
            r.id=dec(a[0]);r.itemName=dec(a[1]);r.faction=Faction.valueOf(a[2]);r.stockClass=dec(a[3]);
            r.sourceKind=dec(a[4]);r.sourceLabel=dec(a[5]);r.sourceFacilityId=dec(a[6]);r.route=dec(a[7]);
            r.eventStatus=dec(a[8]);r.purpose=dec(a[9]);r.capacity=Math.max(1,Integer.parseInt(a[10]));
            r.remaining=Math.max(0,Math.min(r.capacity,Integer.parseInt(a[11])));
            r.restockIntervalTurns=Math.max(1,Integer.parseInt(a[12]));r.nextRestockWorldTurn=Math.max(0L,Long.parseLong(a[13]));
            return r;
        } catch(Exception ignored){return null;}
    }
    String playerLine(){return itemName+" luxury: "+stockClass+" from "+sourceLabel+"; purpose "+purpose+"; "
            +remaining+"/"+capacity+" unit(s); "+eventStatus+"; "+(remaining>0?"next estate release":"depleted until release")
            +" at world turn "+nextRestockWorldTurn+".";}
    private static String enc(String v){return Base64.getUrlEncoder().withoutPadding().encodeToString((v==null?"":v).getBytes(StandardCharsets.UTF_8));}
    private static String dec(String v){return new String(Base64.getUrlDecoder().decode(v),StandardCharsets.UTF_8);}
}

final class DraughtCustodyRecord {
    String id="draught.unassigned";
    String itemName="Black Sun Draught";
    Faction ownerFaction=Faction.NONE;
    String houseOwner="unrecorded noble house";
    String offWorldOrigin="unknown off-world origin";
    String importRoute="unrecorded import route";
    String sourceRole="estate steward";
    String vaultLabel="sealed house vault";
    String authenticity="house-certified genuine";
    String eventStatus="no active blockade, tax, seizure, or tithe";
    String purpose="household hoarding";
    int heldQuantity=1;
    boolean releasedForSale=false;

    String saveLine(){return enc(id)+"|"+enc(itemName)+"|"+(ownerFaction==null?Faction.NONE.name():ownerFaction.name())+"|"
            +enc(houseOwner)+"|"+enc(offWorldOrigin)+"|"+enc(importRoute)+"|"+enc(sourceRole)+"|"+enc(vaultLabel)+"|"
            +enc(authenticity)+"|"+enc(eventStatus)+"|"+enc(purpose)+"|"+heldQuantity+"|"+releasedForSale;}
    static DraughtCustodyRecord parse(String line){try{String[]a=line.split("\\|",13);if(a.length<13)return null;DraughtCustodyRecord r=new DraughtCustodyRecord();
        r.id=dec(a[0]);r.itemName=dec(a[1]);r.ownerFaction=Faction.valueOf(a[2]);r.houseOwner=dec(a[3]);r.offWorldOrigin=dec(a[4]);
        r.importRoute=dec(a[5]);r.sourceRole=dec(a[6]);r.vaultLabel=dec(a[7]);r.authenticity=dec(a[8]);r.eventStatus=dec(a[9]);
        r.purpose=dec(a[10]);r.heldQuantity=Math.max(0,Integer.parseInt(a[11]));r.releasedForSale=Boolean.parseBoolean(a[12]);return r;}catch(Exception ignored){return null;}}
    String playerLine(){return "Draught custody: "+itemName+"; owner "+houseOwner+"; origin "+offWorldOrigin+"; route "+importRoute
            +"; source "+sourceRole+"; vault "+vaultLabel+"; "+authenticity+"; "+eventStatus+"; purpose "+purpose+"; quantity "
            +heldQuantity+"; "+(releasedForSale?"exceptionally released for sale":"protected household property, not for sale")+".";}
    private static String enc(String v){return Base64.getUrlEncoder().withoutPadding().encodeToString((v==null?"":v).getBytes(StandardCharsets.UTF_8));}
    private static String dec(String v){return new String(Base64.getUrlDecoder().decode(v),StandardCharsets.UTF_8);}
}

final class NobleLuxuryProvenanceAuthority {
    private static final String[] DRAUGHTS={"Black Sun Draught","Lucid Null","Ghost Orchid","Ebon Lotus","Angel Engine","Machine Rapture","Sorrowglass","Kingmaker Serum","Red Choir","The Beautiful Error"};
    private record Source(String kind,String label,String facilityId,String route,String stockClass,String eventStatus,String purpose,int capacity,int restockTurns){}
    private NobleLuxuryProvenanceAuthority(){}

    static void apply(TraderSession trader,World world,Faction faction,long worldTurn,int localTurn){
        if(trader==null||world==null)return;
        Faction owner=safeFaction(faction);
        ensureCustodyRecords(world,owner);
        boolean noble=isNoble(owner), luxurySource=hasLuxurySource(world,owner);
        boolean existingLuxury=trader.offers.stream().anyMatch(NobleLuxuryProvenanceAuthority::isOrdinaryLuxury);
        if(noble||luxurySource||existingLuxury){
            ensureOffer(trader,"Noble preserved delicacy","estate luxury food released for prestige trade.");
            ensureOffer(trader,"High-quality amasec bottle","estate cellar stock released for prestige trade.");
        }
        ArrayList<TradeOffer> ordinary=new ArrayList<>();
        for(TradeOffer offer:trader.offers)if(isOrdinaryLuxury(offer))ordinary.add(offer);
        for(TradeOffer offer:ordinary){
            NobleLuxuryReserveRecord reserve=reserveFor(world,owner,offer.name);
            if(reserve==null){reserve=createReserve(world,owner,offer.name,worldTurn);world.nobleLuxuryReserves.add(reserve);}
            refresh(reserve,worldTurn);
            if(reserve.remaining<=0){trader.offers.remove(offer);continue;}
            attachLuxury(offer,reserve,world,owner,localTurn);appendSummary(trader,reserve.playerLine());
        }
        ArrayList<TradeOffer> draughtOffers=new ArrayList<>();
        for(TradeOffer offer:trader.offers)if(isDraught(offer.name))draughtOffers.add(offer);
        for(TradeOffer offer:draughtOffers)trader.offers.remove(offer);
        for(DraughtCustodyRecord custody:world.draughtCustodyRecords){
            if(custody==null||custody.ownerFaction!=owner)continue;
            appendSummary(trader,custody.playerLine());
            if(custody.releasedForSale&&custody.heldQuantity>0){
                TradeOffer offer=new TradeOffer(custody.itemName,"chem/rare-campaign",ItemCatalog.priceFor(custody.itemName),
                        "exceptional draught release; custody and event trace required.");
                attachDraught(offer,custody,world,localTurn);trader.offers.add(offer);
            }
        }
        if(!draughtOffers.isEmpty()&&world.draughtCustodyRecords.stream().noneMatch(c->c!=null&&c.ownerFaction==owner&&c.releasedForSale))
            appendSummary(trader,"Protected draught offers withheld: rare house custody is not ordinary trader stock.");
    }

    static String purchaseBlock(World world,TradeOffer offer,long worldTurn){
        if(offer==null)return"";
        if(offer.nobleLuxuryReserveId!=null&&!offer.nobleLuxuryReserveId.isBlank()){
            NobleLuxuryReserveRecord r=reserveById(world,offer.nobleLuxuryReserveId);if(r==null)return"its luxury reserve ledger is unavailable";
            refresh(r,worldTurn);if(r.remaining<=0)return r.sourceLabel+" is depleted until world turn "+r.nextRestockWorldTurn;
        }
        if(offer.draughtCustodyId!=null&&!offer.draughtCustodyId.isBlank()){
            DraughtCustodyRecord c=custodyById(world,offer.draughtCustodyId);
            if(c==null)return"its draught custody record is unavailable";
            if(!c.releasedForSale)return c.houseOwner+" has not released this protected draught for sale";
            if(c.heldQuantity<=0)return"the released draught custody is empty";
        }
        return"";
    }
    static boolean consume(World world,TradeOffer offer,long worldTurn){
        if(offer==null)return true;
        if(offer.nobleLuxuryReserveId!=null&&!offer.nobleLuxuryReserveId.isBlank()){NobleLuxuryReserveRecord r=reserveById(world,offer.nobleLuxuryReserveId);if(r==null)return false;refresh(r,worldTurn);if(r.remaining<=0)return false;r.remaining--;}
        if(offer.draughtCustodyId!=null&&!offer.draughtCustodyId.isBlank()){DraughtCustodyRecord c=custodyById(world,offer.draughtCustodyId);if(c==null||!c.releasedForSale||c.heldQuantity<=0)return false;c.heldQuantity--;if(c.heldQuantity<=0)c.releasedForSale=false;}
        return true;
    }
    static void updateSessionAfterPurchase(TraderSession trader,World world,TradeOffer offer){
        if(trader==null||offer==null)return;
        if(offer.nobleLuxuryReserveId!=null&&!offer.nobleLuxuryReserveId.isBlank()){NobleLuxuryReserveRecord r=reserveById(world,offer.nobleLuxuryReserveId);if(r!=null){appendSummary(trader,r.playerLine());if(r.remaining<=0)trader.offers.removeIf(o->o!=null&&r.id.equals(o.nobleLuxuryReserveId));}}
        if(offer.draughtCustodyId!=null&&!offer.draughtCustodyId.isBlank()){DraughtCustodyRecord c=custodyById(world,offer.draughtCustodyId);if(c!=null){appendSummary(trader,c.playerLine());if(c.heldQuantity<=0)trader.offers.removeIf(o->o!=null&&c.id.equals(o.draughtCustodyId));}}
    }

    static NobleLuxuryReserveRecord reserveFor(World world,Faction faction,String item){if(world==null)return null;Faction f=safeFaction(faction);for(NobleLuxuryReserveRecord r:world.nobleLuxuryReserves)if(r!=null&&r.faction==f&&ItemQuality.namesMatch(r.itemName,item))return r;return null;}
    static DraughtCustodyRecord custodyFor(World world,Faction faction,String item){if(world==null)return null;Faction f=safeFaction(faction);for(DraughtCustodyRecord c:world.draughtCustodyRecords)if(c!=null&&c.ownerFaction==f&&ItemQuality.namesMatch(c.itemName,item))return c;return null;}
    static NobleLuxuryReserveRecord reserveById(World world,String id){if(world==null||id==null)return null;for(NobleLuxuryReserveRecord r:world.nobleLuxuryReserves)if(r!=null&&id.equals(r.id))return r;return null;}
    static DraughtCustodyRecord custodyById(World world,String id){if(world==null||id==null)return null;for(DraughtCustodyRecord c:world.draughtCustodyRecords)if(c!=null&&id.equals(c.id))return c;return null;}

    private static NobleLuxuryReserveRecord createReserve(World world,Faction faction,String item,long worldTurn){Source s=resolveLuxurySource(world,faction,item);NobleLuxuryReserveRecord r=new NobleLuxuryReserveRecord();r.id="luxury."+Math.abs(Objects.hash(world.seed,faction.name(),item));r.itemName=item;r.faction=faction;r.stockClass=s.stockClass();r.sourceKind=s.kind();r.sourceLabel=s.label();r.sourceFacilityId=s.facilityId();r.route=s.route();r.eventStatus=s.eventStatus();r.purpose=s.purpose();r.capacity=s.capacity();r.remaining=s.capacity();r.restockIntervalTurns=s.restockTurns();r.nextRestockWorldTurn=Math.max(0L,worldTurn)+s.restockTurns();return r;}
    private static Source resolveLuxurySource(World world,Faction faction,String item){
        String history=safe(world.zoneConflictLossHistory).toLowerCase(Locale.ROOT);
        for(ZoneConflictLossRecord e:HistoricalConflictLossApi.parseConflictLossLedger(world.zoneConflictLossHistory)){String text=(safe(e.eventType)+" "+safe(e.actor)+" "+safe(e.affectedStock)+" "+safe(e.destination)+" "+safe(e.historyNote)).toLowerCase(Locale.ROOT);if(!luxuryText(text))continue;String cls=authenticity(text);String status=eventStatus(text);String purpose=purpose(text);String label=safe(e.destination).isBlank()?e.eventType:e.destination;return new Source("event-diverted luxury",label,safe(e.sourceFacilityId),safe(e.sourceFacilityId)+" -> "+e.eventType+" by "+e.actor+" -> "+label+" -> luxury counter",cls,status,purpose,2,days(12));}
        for(ZoneProductionOutputRecord o:ProductionFacilityOutputSimulationApi.parseProductionLedger(world.zoneProductionHistory)){String text=safe(o.facilityId)+" "+safe(o.facilityPurpose)+" "+safe(o.outputFocus)+" "+safe(o.sampleItems);if(!luxuryText(text.toLowerCase(Locale.ROOT))||!controllerCompatible(faction,o.controller))continue;String label=safe(o.facilityId).isBlank()?o.facilityPurpose:o.facilityId;return new Source("estate luxury production",label,safe(o.facilityId),label+" -> estate store -> luxury counter","house-certified luxury",eventStatus(history),"prestige and gifting",Math.max(2,Math.min(8,o.batches*2)),days(4));}
        for(int i=0;i<world.roomProfiles.size();i++){RoomProfile room=world.roomProfiles.get(i);Faction owner=i<world.roomFactions.size()?world.roomFactions.get(i):Faction.NONE;if(room==null||!factionCompatible(faction,owner))continue;String text=(safe(room.name)+" "+safe(room.descriptor)).toLowerCase(Locale.ROOT);if(!luxuryText(text))continue;return new Source("estate luxury store",room.name,"room."+i,room.name+" -> estate steward -> luxury counter","house-certified luxury",eventStatus(history),"prestige, gifting, and household use",3,days(7));}
        boolean blocked=contains(history,"blockade","interdiction","route closure");if(!blocked&&hasRailAccess(world))return new Source("off-world luxury import","spire merchant freight","rail.luxury.intake","off-world merchant -> rail intake -> estate broker -> luxury counter","off-world imported luxury","import route open","prestige and bargaining",2,days(10));
        return new Source("household luxury reserve",faction.label+" estate store","house.luxury.reserve",faction.label+" estate store -> luxury counter",blocked?"blockade-restricted luxury":"house-certified luxury",blocked?"blockade restricts imports; household stock only":eventStatus(history),"household use and gifting",blocked?1:2,days(blocked?14:8));
    }

    private static void ensureCustodyRecords(World world,Faction faction){
        for(ZoneConflictLossRecord e:HistoricalConflictLossApi.parseConflictLossLedger(world.zoneConflictLossHistory)){String text=safe(e.eventType)+" "+safe(e.actor)+" "+safe(e.affectedStock)+" "+safe(e.destination)+" "+safe(e.historyNote);String item=draughtNamedIn(text);if(item==null)continue;if(custodyFor(world,faction,item)!=null)continue;DraughtCustodyRecord c=new DraughtCustodyRecord();c.id="draught.event."+Math.abs(Objects.hash(world.seed,faction.name(),item,e.id));c.itemName=item;c.ownerFaction=faction;c.houseOwner=contains(text.toLowerCase(Locale.ROOT),"ashbourne")?"House Ashbourne":faction.label;c.offWorldOrigin="off-world origin recorded in disputed cargo papers";c.importRoute=safe(e.sourceFacilityId)+" -> "+e.eventType+" -> "+e.destination;c.sourceRole=sourceRole(text);c.vaultLabel=safe(e.destination).isBlank()?"disputed custody cache":e.destination;c.authenticity=authenticity(text.toLowerCase(Locale.ROOT));c.eventStatus=eventStatus(text.toLowerCase(Locale.ROOT));c.purpose=purpose(text.toLowerCase(Locale.ROOT));c.heldQuantity=1;c.releasedForSale=!isNoble(faction)&&contains(text.toLowerCase(Locale.ROOT),"theft","smuggl","black-market","bargain","sale");world.draughtCustodyRecords.add(c);}
        if(!isNoble(faction))return;
        String vault=findDraughtVault(world,faction);if(vault==null)return;
        for(DraughtCustodyRecord c:world.draughtCustodyRecords)if(c!=null&&c.ownerFaction==faction)return;
        String item=DRAUGHTS[Math.floorMod((int)(world.seed^(world.seed>>>32)),DRAUGHTS.length)];DraughtCustodyRecord c=new DraughtCustodyRecord();c.id="draught.vault."+Math.abs(Objects.hash(world.seed,faction.name(),item));c.itemName=item;c.ownerFaction=faction;c.houseOwner=vault.toLowerCase(Locale.ROOT).contains("ashbourne")?"House Ashbourne":faction.label;c.offWorldOrigin="certified off-world noble import";c.importRoute="off-world merchant -> house broker -> sealed estate intake";c.sourceRole="estate merchant and physician";c.vaultLabel=vault;c.authenticity="house-certified genuine";c.eventStatus=eventStatus(safe(world.zoneConflictLossHistory).toLowerCase(Locale.ROOT));c.purpose=purpose(safe(world.zoneConflictLossHistory).toLowerCase(Locale.ROOT));c.heldQuantity=1;c.releasedForSale=false;world.draughtCustodyRecords.add(c);
    }

    private static void attachLuxury(TradeOffer offer,NobleLuxuryReserveRecord reserve,World world,Faction faction,int turn){offer.nobleLuxuryReserveId=reserve.id;ItemProvenanceRecord p=ItemProvenanceRecord.of(offer.name,faction,reserve.sourceLabel,world,turn,reserve.stockClass+"; finite reserve "+reserve.remaining+"/"+reserve.capacity,reserve.route);p.productionSource=reserve.sourceKind;p.producingFacility=reserve.sourceFacilityId;p.batchIssueTags=reserve.stockClass.contains("counterfeit")||reserve.stockClass.contains("stolen")?reserve.stockClass:"house-certified";p.chain=reserve.route;offer.provenance=p;String prior=offer.description==null?"":offer.description+" ";offer.description=prior+reserve.stockClass+"; reserve "+reserve.remaining+"/"+reserve.capacity+"; "+reserve.eventStatus+".";}
    private static void attachDraught(TradeOffer offer,DraughtCustodyRecord c,World world,int turn){offer.draughtCustodyId=c.id;offer.medicalSupplyReserveId="";ItemProvenanceRecord p=ItemProvenanceRecord.of(c.itemName,c.ownerFaction,c.sourceRole,world,turn,c.authenticity+"; purpose "+c.purpose,c.importRoute+" -> "+c.vaultLabel+" -> exceptional sale release");p.productionSource="protected draught custody";p.producingFacility=c.vaultLabel;p.productionLegalStatus="exceptional controlled transfer";p.batchIssueTags=c.authenticity+"; "+c.eventStatus;p.chain=c.houseOwner+" -> "+p.route;offer.provenance=p;}
    private static void ensureOffer(TraderSession trader,String item,String desc){if(ItemCatalog.get(item)==null)return;for(TradeOffer o:trader.offers)if(o!=null&&ItemQuality.namesMatch(o.name,item))return;ItemDef d=ItemCatalog.get(item);trader.offers.add(new TradeOffer(item,d.category,d.basePrice,desc));}
    private static void refresh(NobleLuxuryReserveRecord r,long turn){if(r!=null&&turn>=r.nextRestockWorldTurn){r.remaining=r.capacity;r.nextRestockWorldTurn=Math.max(0L,turn)+r.restockIntervalTurns;}}
    private static boolean isOrdinaryLuxury(TradeOffer o){if(o==null||isDraught(o.name)||!safe(o.securitySupplyReserveId).isBlank())return false;ItemDef d=ItemCatalog.get(o.name);String c=(d==null?safe(o.category):safe(d.category)).toLowerCase(Locale.ROOT);return c.contains("luxury")||c.contains("noble");}
    private static boolean isDraught(String item){ItemDef d=ItemCatalog.get(item);return d!=null&&safe(d.category).equalsIgnoreCase("chem/rare-campaign");}
    private static String draughtNamedIn(String text){String low=safe(text).toLowerCase(Locale.ROOT);for(String item:DRAUGHTS)if(low.contains(item.toLowerCase(Locale.ROOT)))return item;return low.contains("draught")?"Black Sun Draught":null;}
    private static String findDraughtVault(World world,Faction faction){for(int i=0;i<world.roomProfiles.size();i++){RoomProfile r=world.roomProfiles.get(i);Faction f=i<world.roomFactions.size()?world.roomFactions.get(i):Faction.NONE;if(r==null||!factionCompatible(faction,f))continue;String t=(safe(r.name)+" "+safe(r.descriptor)).toLowerCase(Locale.ROOT);if(contains(t,"vault","house product warehouse","secure storage","private cellar","trophy security"))return r.name;}return null;}
    private static boolean hasLuxurySource(World world,Faction faction){if(isNoble(faction))return true;for(int i=0;i<world.roomProfiles.size();i++){RoomProfile r=world.roomProfiles.get(i);Faction f=i<world.roomFactions.size()?world.roomFactions.get(i):Faction.NONE;if(r!=null&&factionCompatible(faction,f)&&luxuryText((safe(r.name)+" "+safe(r.descriptor)).toLowerCase(Locale.ROOT)))return true;}return false;}
    private static boolean luxuryText(String t){return contains(t,"noble","luxury","salon","estate","orchard","bio-garden","perfum","amasec","gildwine","truffle","signet","prestige");}
    private static String authenticity(String t){if(contains(t,"diluted"))return"diluted";if(contains(t,"counterfeit"))return"counterfeit";if(contains(t,"contaminated","tainted"))return"contaminated";if(contains(t,"stolen","theft"))return"stolen";if(contains(t,"misdeclared"))return"misdeclared cargo";return"house-certified genuine";}
    private static String eventStatus(String t){ArrayList<String>s=new ArrayList<>();if(contains(t,"blockade","interdiction"))s.add("blockade affected");if(contains(t,"tax"))s.add("taxed");if(contains(t,"seizure","confiscation"))s.add("seizure affected");if(contains(t,"tithe"))s.add("tithe edict affected");return s.isEmpty()?"no active blockade, tax, seizure, or tithe":String.join(", ",s);}
    private static String purpose(String t){if(contains(t,"blackmail"))return"blackmail";if(contains(t,"gift"))return"gifting";if(contains(t,"bargain"))return"bargaining";if(contains(t,"medical privilege","physician"))return"medical privilege";if(contains(t,"indulgence"))return"private indulgence";if(contains(t,"inheritance"))return"inheritance";if(contains(t,"hoard"))return"hoarding";return"household use and prestige";}
    private static String sourceRole(String t){String l=t.toLowerCase(Locale.ROOT);if(l.contains("smuggler"))return"smuggler";if(l.contains("broker"))return"broker";if(l.contains("physician"))return"physician";if(l.contains("merchant"))return"merchant";return"disputed estate custodian";}
    private static boolean hasRailAccess(World w){if(w.zoneType==ZoneType.NEUTRAL_RAIL_DEPOT||w.zoneType==ZoneType.TRAIN_SERVICE_YARD)return true;for(RoomPopulationLedger l:w.roomPopulationLedgers){String t=(safe(l.sourceKind)+" "+safe(l.sourceLabel)+" "+safe(l.roomName)).toLowerCase(Locale.ROOT);if(contains(t,"rail intake","train","rail hub"))return true;}return false;}
    private static boolean controllerCompatible(Faction f,String c){String low=safe(c).toLowerCase(Locale.ROOT).trim();if(low.isBlank()||contains(low,"unknown","unrecorded","none","neutral"))return true;return compact(low).equals(compact(f.name()))||compact(low).equals(compact(f.label));}
    private static boolean factionCompatible(Faction a,Faction b){if(b==null||b==Faction.NONE||a==Faction.NONE||a==b)return true;return a.name().split("_")[0].equals(b.name().split("_")[0]);}
    private static boolean isNoble(Faction f){return f==Faction.NOBLE||f.name().startsWith("NOBLE_");}
    private static int days(int d){return Math.max(1,d)*GamePanel.TURNS_PER_HOUR*GamePanel.HOURS_PER_DAY;}
    private static void appendSummary(TraderSession t,String line){if(line==null||line.isBlank())return;String c=t.supplyChainSummary==null?"":t.supplyChainSummary;if(!c.contains(line))t.supplyChainSummary=c.isBlank()?line:c+" "+line;}
    private static boolean contains(String t,String...n){if(t==null)return false;for(String x:n)if(x!=null&&!x.isBlank()&&t.contains(x))return true;return false;}
    private static String compact(String v){return safe(v).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
    private static Faction safeFaction(Faction f){return f==null?Faction.NONE:f;}
    private static String safe(String v){return v==null?"":v;}
}
