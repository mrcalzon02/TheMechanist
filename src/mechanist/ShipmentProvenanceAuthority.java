package mechanist;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Persisted incoming shipment manifest with route availability and exact delivered cargo. */
final class ShipmentProvenanceRecord {
    String id="shipment.unassigned";
    String sourceFaction="unrecorded supplier faction";
    String supplier="unrecorded supplier";
    String sourceSite="unrecorded source site";
    Faction destinationFaction=Faction.NONE;
    String destinationFacility="unassigned destination";
    String arrivalNode="unassigned arrival node";
    String cargoManifest="unrecorded cargo";
    String cargoItem="Mixed cargo";
    int quantity=1;
    int remaining=1;
    int cargoValue=1;
    String legality="ordinary legal freight";
    String qualityRisk="ordinary handling risk";
    String eventModifier="no active shipment event";
    int interceptionRiskPct=10;
    int delayRiskPct=10;
    long departureWorldTurn=0L;
    long earliestArrivalWorldTurn=0L;
    long latestArrivalWorldTurn=0L;
    String status="ARRIVED";
    String simulationMode="player-visible operational";
    boolean playerVisible=true;
    String route="supplier -> arrival node -> destination";
    String linkedReserveKind="unlinked cargo";
    String linkedReserveId="";
    int deliverySequence=1;
    String procurementPolicy="ordinary merchant procurement";
    int sourceProcurementCost=1;
    int sourceCooldownBaseTurns=GamePanel.TURNS_PER_HOUR*GamePanel.HOURS_PER_DAY*9;
    int sourceCooldownVarianceTurns=0;
    int sourceCooldownTurns=sourceCooldownBaseTurns;
    long nextSourceAvailableWorldTurn=sourceCooldownTurns;

    String saveLine(){return enc(id)+"|"+enc(sourceFaction)+"|"+enc(supplier)+"|"+enc(sourceSite)+"|"
            +(destinationFaction==null?Faction.NONE.name():destinationFaction.name())+"|"+enc(destinationFacility)+"|"
            +enc(arrivalNode)+"|"+enc(cargoManifest)+"|"+enc(cargoItem)+"|"+quantity+"|"+remaining+"|"
            +cargoValue+"|"+enc(legality)+"|"+enc(qualityRisk)+"|"+enc(eventModifier)+"|"
            +interceptionRiskPct+"|"+delayRiskPct+"|"+departureWorldTurn+"|"+earliestArrivalWorldTurn+"|"
            +latestArrivalWorldTurn+"|"+enc(status)+"|"+enc(simulationMode)+"|"+playerVisible+"|"+enc(route)+"|"
            +enc(linkedReserveKind)+"|"+enc(linkedReserveId)+"|"+deliverySequence+"|"+enc(procurementPolicy)+"|"
            +sourceProcurementCost+"|"+sourceCooldownBaseTurns+"|"+sourceCooldownVarianceTurns+"|"
            +sourceCooldownTurns+"|"+nextSourceAvailableWorldTurn;}
    static ShipmentProvenanceRecord parse(String line){try{String[]a=line.split("\\|",33);if(a.length<27)return null;
        ShipmentProvenanceRecord r=new ShipmentProvenanceRecord();r.id=dec(a[0]);r.sourceFaction=dec(a[1]);r.supplier=dec(a[2]);r.sourceSite=dec(a[3]);r.destinationFaction=Faction.valueOf(a[4]);
        r.destinationFacility=dec(a[5]);r.arrivalNode=dec(a[6]);r.cargoManifest=dec(a[7]);r.cargoItem=dec(a[8]);r.quantity=Math.max(1,Integer.parseInt(a[9]));r.remaining=Math.max(0,Math.min(r.quantity,Integer.parseInt(a[10])));r.cargoValue=Math.max(0,Integer.parseInt(a[11]));
        r.legality=dec(a[12]);r.qualityRisk=dec(a[13]);r.eventModifier=dec(a[14]);r.interceptionRiskPct=clamp(Integer.parseInt(a[15]));r.delayRiskPct=clamp(Integer.parseInt(a[16]));r.departureWorldTurn=Math.max(0L,Long.parseLong(a[17]));r.earliestArrivalWorldTurn=Math.max(0L,Long.parseLong(a[18]));r.latestArrivalWorldTurn=Math.max(r.earliestArrivalWorldTurn,Long.parseLong(a[19]));
        r.status=dec(a[20]);r.simulationMode=dec(a[21]);r.playerVisible=Boolean.parseBoolean(a[22]);r.route=dec(a[23]);r.linkedReserveKind=dec(a[24]);r.linkedReserveId=dec(a[25]);r.deliverySequence=Math.max(1,Integer.parseInt(a[26]));
        if(a.length>=33){r.procurementPolicy=dec(a[27]);r.sourceProcurementCost=Math.max(1,Integer.parseInt(a[28]));r.sourceCooldownBaseTurns=Math.max(1,Integer.parseInt(a[29]));r.sourceCooldownVarianceTurns=Integer.parseInt(a[30]);r.sourceCooldownTurns=Math.max(1,Integer.parseInt(a[31]));r.nextSourceAvailableWorldTurn=Math.max(0L,Long.parseLong(a[32]));}
        return r;}catch(Exception ignored){return null;}}
    String playerLine(){return "Shipment "+id+": "+cargoManifest+" from "+supplier+" / "+sourceFaction+" to "+destinationFacility+" for "+destinationFaction.label+" via "+arrivalNode+"; cargo value "+cargoValue+" script; source procurement cost "+sourceProcurementCost+" script under "+procurementPolicy+"; source cooldown "+sourceCooldownTurns+" turns (base "+sourceCooldownBaseTurns+", variance "+signed(sourceCooldownVarianceTurns)+"); next off-map source available at world turn "+nextSourceAvailableWorldTurn+"; "+legality+"; "+qualityRisk+"; "+eventModifier+"; interception risk "+interceptionRiskPct+"%; delay risk "+delayRiskPct+"%; arrival window "+earliestArrivalWorldTurn+"-"+latestArrivalWorldTurn+"; status "+status+"; "+simulationMode+"; "+remaining+"/"+quantity+" cargo unit(s) remain.";}
    private static String signed(int value){return value>=0?"+"+value:String.valueOf(value);}
    private static int clamp(int v){return Math.max(0,Math.min(100,v));}
    private static String enc(String v){return Base64.getUrlEncoder().withoutPadding().encodeToString((v==null?"":v).getBytes(StandardCharsets.UTF_8));}
    private static String dec(String v){return new String(Base64.getUrlDecoder().decode(v),StandardCharsets.UTF_8);}
}

final class ShipmentProvenanceAuthority {
    private record ProcurementPolicy(String label,int baseCooldownDays,int variancePct,int cargoCostPct,int fixedRouteCost){}
    private ShipmentProvenanceAuthority(){}

    static void apply(TraderSession trader,World world,Faction destination,long worldTurn,int localTurn){
        if(trader==null||world==null)return;Faction faction=destination==null?Faction.NONE:destination;
        materializeLedgerShipments(trader,world,faction,worldTurn);
        ArrayList<TradeOffer> external=new ArrayList<>();for(TradeOffer offer:trader.offers)if(isExternalOffer(offer))external.add(offer);
        int arrived=0,withheld=0;
        for(TradeOffer offer:external){String reserveId=linkedReserveId(offer);String reserveKind=linkedReserveKind(offer);
            ShipmentProvenanceRecord shipment=shipmentForOffer(world,faction,trader.name,offer.name,reserveId);
            if(shipment!=null&&shipment.status.equals("DELIVERED")&&availableQuantity(world,offer)>0
                    &&worldTurn<shipment.nextSourceAvailableWorldTurn){offer.shipmentRecordId=shipment.id;offer.shipmentProcurementUnitCost=unitProcurementCost(shipment);trader.offers.remove(offer);withheld++;appendSummary(trader,shipment.playerLine());continue;}
            if(shipment==null||(shipment.status.equals("DELIVERED")&&availableQuantity(world,offer)>0
                    &&worldTurn>=shipment.nextSourceAvailableWorldTurn)){
                int sequence=nextSequence(world,faction,trader.name,offer.name,reserveId);shipment=createForOffer(world,faction,trader,offer,reserveKind,reserveId,worldTurn,sequence);world.shipmentRecords.add(shipment);}
            refresh(shipment,worldTurn);offer.shipmentRecordId=shipment.id;
            if(!shipment.status.equals("ARRIVED")||shipment.remaining<=0){trader.offers.remove(offer);withheld++;appendSummary(trader,shipment.playerLine());continue;}
            attach(offer,shipment,world,localTurn);arrived+=shipment.remaining;appendSummary(trader,shipment.playerLine());
        }
        long visible=world.shipmentRecords.stream().filter(s->s!=null&&s.playerVisible&&s.destinationFaction==faction).count();
        if(!external.isEmpty()||visible>0)appendSummary(trader,"Incoming shipment control: "+external.size()+" linked offer(s), "+arrived+" arrived cargo unit(s), "+withheld+" delayed/intercepted/depleted offer(s) withheld, "+visible+" player-visible manifest(s).");
    }

    static String purchaseBlock(World world,TradeOffer offer,long worldTurn){if(offer==null||safe(offer.shipmentRecordId).isBlank())return"";ShipmentProvenanceRecord s=byId(world,offer.shipmentRecordId);if(s==null)return"its shipment manifest is unavailable";refresh(s,worldTurn);if(s.status.equals("INTERCEPTED"))return"shipment "+s.id+" was intercepted before reaching "+s.arrivalNode;if(s.status.equals("DELAYED")||s.status.equals("SCHEDULED"))return"shipment "+s.id+" is "+s.status.toLowerCase(Locale.ROOT)+" until its arrival window at world turn "+s.earliestArrivalWorldTurn;if(!s.status.equals("ARRIVED")||s.remaining<=0)return"shipment "+s.id+" has no delivered cargo remaining";return"";}
    static boolean consume(World world,TradeOffer offer,long worldTurn){if(offer==null||safe(offer.shipmentRecordId).isBlank())return true;ShipmentProvenanceRecord s=byId(world,offer.shipmentRecordId);if(s==null)return false;refresh(s,worldTurn);if(!s.status.equals("ARRIVED")||s.remaining<=0)return false;s.remaining--;if(s.remaining<=0)s.status="DELIVERED";return true;}
    static void updateSessionAfterPurchase(TraderSession trader,World world,TradeOffer offer){if(trader==null||offer==null||safe(offer.shipmentRecordId).isBlank())return;ShipmentProvenanceRecord s=byId(world,offer.shipmentRecordId);if(s==null)return;appendSummary(trader,s.playerLine());if(s.remaining<=0)trader.offers.removeIf(o->o!=null&&s.id.equals(o.shipmentRecordId));}
    static int adjustBuyPrice(TradeOffer offer,int currentPrice){if(offer==null||offer.shipmentProcurementUnitCost<=0)return currentPrice;return Math.max(currentPrice,offer.shipmentProcurementUnitCost);}
    static ShipmentProvenanceRecord byId(World world,String id){if(world==null||safe(id).isBlank())return null;for(ShipmentProvenanceRecord s:world.shipmentRecords)if(s!=null&&id.equals(s.id))return s;return null;}
    static ShipmentProvenanceRecord shipmentForCargo(World world,Faction destination,String item){if(world==null)return null;return world.shipmentRecords.stream().filter(s->s!=null&&s.destinationFaction==(destination==null?Faction.NONE:destination)&&ItemQuality.namesMatch(s.cargoItem,item)).max(Comparator.comparingInt(s->s.deliverySequence)).orElse(null);}
    static void refreshForInspection(World world,long worldTurn){if(world==null)return;for(ShipmentProvenanceRecord shipment:world.shipmentRecords)refresh(shipment,worldTurn);}

    private static ShipmentProvenanceRecord createForOffer(World world,Faction faction,TraderSession trader,TradeOffer offer,String reserveKind,String reserveId,long turn,int sequence){
        ShipmentProvenanceRecord s=new ShipmentProvenanceRecord();ItemProvenanceRecord p=offer.provenance;String trace=traceText(offer);
        s.id="shipment.offer."+Math.abs(Objects.hash(world.seed,faction.name(),safe(trader.name),ItemQuality.stripQuality(offer.name),reserveId,sequence));
        s.sourceFaction=p==null||safe(p.makerFaction).isBlank()||safe(p.makerFaction).equalsIgnoreCase(faction.name())?sourceFactionFromTrace(trace):p.makerFaction;s.supplier=p==null||safe(p.maker).isBlank()?supplierFromTrace(trace):p.maker;
        s.sourceSite=p==null||safe(p.producingFacility).isBlank()?s.supplier:p.producingFacility;s.destinationFaction=faction;s.destinationFacility=safe(trader.name).isBlank()?faction.label+" receiving store":trader.name;
        s.arrivalNode=arrivalNode(world,faction,trace);s.cargoItem=offer.name;s.quantity=Math.max(1,availableQuantity(world,offer));s.remaining=s.quantity;s.cargoManifest=offer.name+" x"+s.quantity;s.cargoValue=Math.max(1,offer.basePrice)*s.quantity;
        s.legality=legality(faction,offer,trace);s.qualityRisk=qualityRisk(offer,trace);s.eventModifier=eventModifier(world);
        s.interceptionRiskPct=interceptionRisk(s.legality,s.eventModifier,trace);s.delayRiskPct=delayRisk(s.eventModifier,trace);s.departureWorldTurn=Math.max(0L,turn-days(2));
        boolean intercepted=contains(s.eventModifier,"shipment interception","freight interception","cargo piracy","raided shipment","shipment seizure");boolean delayed=contains(s.eventModifier,"shipment delay","freight delay","customs delay","rail rerouting","delayed freight");
        if(intercepted){s.status="INTERCEPTED";s.earliestArrivalWorldTurn=turn+days(30);s.latestArrivalWorldTurn=s.earliestArrivalWorldTurn;}
        else if(delayed){s.status="DELAYED";s.departureWorldTurn=turn;s.earliestArrivalWorldTurn=turn+days(2);s.latestArrivalWorldTurn=turn+days(5);}
        else{s.status="ARRIVED";s.earliestArrivalWorldTurn=Math.max(0L,turn-days(1));s.latestArrivalWorldTurn=turn+days(1);}
        TopDownWorldEventAuthority.applyToNewShipment(world,s,turn);
        applyProcurementPolicy(s,world,faction,offer.name,sequence);
        s.simulationMode="player-visible operational";s.playerVisible=true;s.route=route(p,trace,s.arrivalNode,s.destinationFacility);s.linkedReserveKind=reserveKind;s.linkedReserveId=reserveId;s.deliverySequence=sequence;return s;}

    private static void materializeLedgerShipments(TraderSession trader,World world,Faction faction,long turn){for(ZoneStockMovementRecord m:ProductionDistributionApi.parseStockMovementLedger(world.zoneStockMovementHistory)){
        String text=(safe(m.sourceFacilityId)+" "+safe(m.destination)+" "+safe(m.movementKind)+" "+safe(m.itemSamples)+" "+safe(m.historyNote)).toLowerCase(Locale.ROOT);if(!externalMovement(text)||safe(m.sourceFacilityId).contains("unmatched"))continue;
        String id="shipment.ledger."+Math.abs(Objects.hash(world.seed,m.id,faction.name(),m.destination));if(byId(world,id)!=null)continue;ArrayList<String> items=ProductionFacilityOutputSimulationApi.sampleList(m.itemSamples);String cargo=items.isEmpty()?"Mixed external cargo":String.join(", ",items);String item=items.isEmpty()?"Mixed cargo":items.get(0);int quantity=Math.max(1,items.size());int value=0;for(String i:items)value+=Math.max(1,ItemCatalog.priceFor(i));
        ShipmentProvenanceRecord s=new ShipmentProvenanceRecord();s.id=id;s.sourceFaction=safe(m.controller).isBlank()?"external supplier":m.controller;s.supplier=s.sourceFaction;s.sourceSite=m.sourceFacilityId;s.destinationFaction=faction;s.destinationFacility=m.destination;s.arrivalNode=arrivalNode(world,faction,text);s.cargoManifest=cargo;s.cargoItem=item;s.quantity=quantity;s.remaining=quantity;s.cargoValue=Math.max(1,value);s.legality=contains(text,"smuggl","black-market","illicit")?"illicit freight":"ordinary legal freight";s.qualityRisk=contains(text,"counterfeit","contaminat","tainted")?"counterfeit or contamination risk":"ordinary handling risk";s.eventModifier=eventModifier(world);s.interceptionRiskPct=interceptionRisk(s.legality,s.eventModifier,text);s.delayRiskPct=delayRisk(s.eventModifier,text);s.departureWorldTurn=Math.max(0L,turn-days(2));s.earliestArrivalWorldTurn=Math.max(0L,turn-days(1));s.latestArrivalWorldTurn=turn+days(1);s.status="ARRIVED";applyProcurementPolicy(s,world,faction,item,1);s.simulationMode=contains(text,"abstract","distant")?"abstract operational":"operational ledger shipment";s.playerVisible=contains(text,"market","trader","storefront","counter","player-visible");s.route=m.sourceFacilityId+" -> "+m.movementKind+" -> "+s.arrivalNode+" -> "+m.destination;s.linkedReserveKind="stock-movement ledger";s.linkedReserveId=m.id;world.shipmentRecords.add(s);if(s.playerVisible)appendSummary(trader,s.playerLine());}}

    private static void attach(TradeOffer offer,ShipmentProvenanceRecord s,World world,int turn){offer.shipmentRecordId=s.id;offer.shipmentProcurementUnitCost=unitProcurementCost(s);ItemProvenanceRecord p=offer.provenance;if(p==null)p=ItemProvenanceRecord.of(offer.name,s.destinationFaction,s.supplier,world,turn,s.cargoManifest,s.route);if(!safe(p.chain).contains(s.id))p.chain=(safe(p.chain).isBlank()?p.maker+" -> "+p.route:p.chain)+" -> shipment "+s.id+" -> "+s.arrivalNode+" -> "+s.destinationFacility;p.productionLegalStatus=append(p.productionLegalStatus,s.legality);p.batchIssueTags=append(p.batchIssueTags,s.qualityRisk+"; "+s.eventModifier);offer.provenance=p;String note=" Shipment "+s.id+" arrived via "+s.arrivalNode+"; manifest "+s.cargoManifest+"; cargo value "+s.cargoValue+" script; source procurement cost "+s.sourceProcurementCost+" script; landed unit cost "+offer.shipmentProcurementUnitCost+" script; "+s.procurementPolicy+"; source cooldown "+s.sourceCooldownTurns+" turns with variance "+s.sourceCooldownVarianceTurns+"; next source turn "+s.nextSourceAvailableWorldTurn+"; "+s.legality+"; "+s.qualityRisk+"; arrival window "+s.earliestArrivalWorldTurn+"-"+s.latestArrivalWorldTurn+"; cargo "+s.remaining+"/"+s.quantity+".";if(offer.description==null)offer.description=note.trim();else if(!offer.description.contains("Shipment "+s.id))offer.description+=note;}
    private static void refresh(ShipmentProvenanceRecord s,long turn){if(s==null||s.status.equals("INTERCEPTED")||s.status.equals("DELIVERED"))return;if((s.status.equals("DELAYED")||s.status.equals("SCHEDULED"))&&turn>=s.earliestArrivalWorldTurn)s.status="ARRIVED";}
    private static ShipmentProvenanceRecord shipmentForOffer(World world,Faction faction,String destination,String item,String reserveId){if(world==null)return null;return world.shipmentRecords.stream().filter(s->s!=null&&s.destinationFaction==faction&&safe(s.destinationFacility).equals(safe(destination))&&ItemQuality.namesMatch(s.cargoItem,item)&&safe(s.linkedReserveId).equals(safe(reserveId))).max(Comparator.comparingInt(s->s.deliverySequence)).orElse(null);}
    private static int nextSequence(World world,Faction faction,String destination,String item,String reserveId){int max=0;for(ShipmentProvenanceRecord s:world.shipmentRecords)if(s!=null&&s.destinationFaction==faction&&safe(s.destinationFacility).equals(safe(destination))&&ItemQuality.namesMatch(s.cargoItem,item)&&safe(s.linkedReserveId).equals(safe(reserveId)))max=Math.max(max,s.deliverySequence);return max+1;}
    private static int availableQuantity(World world,TradeOffer offer){if(offer==null)return 1;if(!safe(offer.rawMaterialReserveId).isBlank()){RawMaterialSupplyReserveRecord r=RawMaterialSupplyProvenanceAuthority.reserveById(world,offer.rawMaterialReserveId);return r==null?1:Math.max(1,r.remaining);}if(!safe(offer.animalAgricultureReserveId).isBlank()){AnimalAgricultureSupplyReserveRecord r=AnimalAgricultureSupplyProvenanceAuthority.reserveById(world,offer.animalAgricultureReserveId);return r==null?1:Math.max(1,r.remaining);}if(!safe(offer.medicalSupplyReserveId).isBlank()){MedicalSupplyReserveRecord r=MedicalSupplyProvenanceAuthority.reserveById(world,offer.medicalSupplyReserveId);return r==null?1:Math.max(1,r.remaining);}if(!safe(offer.securitySupplyReserveId).isBlank()){SecuritySupplyReserveRecord r=SecuritySupplyProvenanceAuthority.reserveById(world,offer.securitySupplyReserveId);return r==null?1:Math.max(1,r.remaining);}if(!safe(offer.nobleLuxuryReserveId).isBlank()){NobleLuxuryReserveRecord r=NobleLuxuryProvenanceAuthority.reserveById(world,offer.nobleLuxuryReserveId);return r==null?1:Math.max(1,r.remaining);}if(!safe(offer.essentialSupplyReserveId).isBlank()){EssentialSupplyReserveRecord r=EssentialSupplyProvenanceAuthority.reserveById(world,offer.essentialSupplyReserveId);return r==null?1:Math.max(1,r.remaining);}return 1;}
    private static String linkedReserveId(TradeOffer o){if(!safe(o.rawMaterialReserveId).isBlank())return o.rawMaterialReserveId;if(!safe(o.animalAgricultureReserveId).isBlank())return o.animalAgricultureReserveId;if(!safe(o.medicalSupplyReserveId).isBlank())return o.medicalSupplyReserveId;if(!safe(o.securitySupplyReserveId).isBlank())return o.securitySupplyReserveId;if(!safe(o.nobleLuxuryReserveId).isBlank())return o.nobleLuxuryReserveId;if(!safe(o.essentialSupplyReserveId).isBlank())return o.essentialSupplyReserveId;if(!safe(o.verticalTradeReserveId).isBlank())return o.verticalTradeReserveId;return safe(o.draughtCustodyId);}
    private static String linkedReserveKind(TradeOffer o){if(!safe(o.rawMaterialReserveId).isBlank())return"raw material reserve";if(!safe(o.animalAgricultureReserveId).isBlank())return"animal/agriculture reserve";if(!safe(o.medicalSupplyReserveId).isBlank())return"medical reserve";if(!safe(o.securitySupplyReserveId).isBlank())return"security reserve";if(!safe(o.nobleLuxuryReserveId).isBlank())return"luxury reserve";if(!safe(o.essentialSupplyReserveId).isBlank())return"essential reserve";if(!safe(o.verticalTradeReserveId).isBlank())return"vertical trade reserve";if(!safe(o.draughtCustodyId).isBlank())return"protected custody";return"unlinked trader cargo";}
    private static boolean isExternalOffer(TradeOffer o){return o!=null&&contains(traceText(o),"outside-sector","off-world","rail intake","freight train","merchant import","material freight","pharmaceutical shipment","spire merchant freight","agricultural import","imported living stock","black-market import","military import","noble material import");}
    private static String traceText(TradeOffer o){ItemProvenanceRecord p=o==null?null:o.provenance;return(safe(o==null?null:o.description)+" "+safe(p==null?null:p.maker)+" "+safe(p==null?null:p.makerFaction)+" "+safe(p==null?null:p.route)+" "+safe(p==null?null:p.chain)+" "+safe(p==null?null:p.productionSource)+" "+safe(p==null?null:p.producingFacility)+" "+safe(p==null?null:p.batchIssueTags)).toLowerCase(Locale.ROOT);}
    private static String sourceFactionFromTrace(String trace){if(contains(trace,"noble"))return"noble supplier";if(contains(trace,"military","guard"))return"military supplier";if(contains(trace,"black-market","smuggl"))return"illicit supplier";return"outside-sector supplier";}
    private static String supplierFromTrace(String trace){if(contains(trace,"off-world"))return"off-world merchant";if(contains(trace,"black-market","smuggl"))return"black-market freight broker";if(contains(trace,"military"))return"military quartermaster freight";if(contains(trace,"noble"))return"noble import broker";return"outside-sector freight supplier";}
    private static String arrivalNode(World world,Faction faction,String trace){MapObjectState node=FactionImportNodeGenerationAuthority.primaryNode(world,faction);if(node!=null)return node.label;if(contains(trace,"off-world","spire"))return"off-world rail customs at "+world.zoneName;if(contains(trace,"smuggl","black-market"))return"concealed rail freight intake at "+world.zoneName;return"rail freight intake at "+world.zoneName;}
    private static String legality(Faction f,TradeOffer o,String trace){if(f==Faction.BANDIT||f.name().startsWith("GANGER_")||contains(trace,"black-market","smuggl","stolen","illicit"))return"illicit or black-market freight";String cat=safe(o.category).toLowerCase(Locale.ROOT);if(contains(cat,"weapon","ammo","medical","genetic","rare-campaign")||contains(trace,"controlled","restricted","military"))return"restricted controlled freight";return"ordinary legal freight";}
    private static String qualityRisk(TradeOffer o,String trace){if(contains(trace,"counterfeit"))return"counterfeit cargo risk";if(contains(trace,"contaminat","tainted"))return"contamination risk";if(contains(trace,"living stock","cloning","culture","seed"))return"viability and handling risk";if(contains(trace,"defective","damaged"))return"damage and defect risk";return"ordinary handling risk";}
    private static int interceptionRisk(String legality,String event,String trace){int n=10;if(contains(legality,"illicit","black-market"))n+=35;if(contains(legality,"restricted"))n+=15;if(contains(event,"interception","piracy","seizure","blockade","interdiction"))n+=30;if(contains(trace,"off-world"))n+=5;return Math.min(95,n);}
    private static int delayRisk(String event,String trace){int n=10;if(contains(event,"delay","rerouting","closure","blockade","interdiction","customs"))n+=45;if(contains(trace,"off-world"))n+=15;if(contains(trace,"living stock","cloning"))n+=10;return Math.min(95,n);}
    private static void applyProcurementPolicy(ShipmentProvenanceRecord s,World world,Faction faction,String item,int sequence){ProcurementPolicy p=policyFor(faction);int base=days(p.baseCooldownDays());int span=Math.max(days(1),base*p.variancePct()/100);int hash=Objects.hash(world==null?0L:world.seed,faction==null?Faction.NONE.name():faction.name(),ItemQuality.stripQuality(item),sequence,"off-map-cooldown");int variance=Math.floorMod(hash,span*2+1)-span;if(variance==0)variance=(hash&1)==0?Math.max(1,span/2):-Math.max(1,span/2);s.procurementPolicy=p.label();s.sourceProcurementCost=Math.max(1,(int)Math.ceil(s.cargoValue*p.cargoCostPct()/100.0)+p.fixedRouteCost());s.sourceCooldownBaseTurns=base;s.sourceCooldownVarianceTurns=variance;s.sourceCooldownTurns=Math.max(days(1),base+variance);s.nextSourceAvailableWorldTurn=Math.max(s.latestArrivalWorldTurn,s.departureWorldTurn)+s.sourceCooldownTurns;}
    private static ProcurementPolicy policyFor(Faction faction){Faction f=faction==null?Faction.NONE:faction;String n=f.name();if(f==Faction.IMPERIAL_GUARD||f==Faction.SORORITAS)return new ProcurementPolicy("military logistics procurement",6,45,85,10);if(f==Faction.NOBLE||n.startsWith("NOBLE_"))return new ProcurementPolicy("noble private import procurement",8,65,140,20);if(f==Faction.BANDIT||n.startsWith("GANGER_"))return new ProcurementPolicy("black-market broker procurement",5,80,165,15);if(f==Faction.MECHANICUS||f==Faction.MECHANIST_COLLEGIA||n.startsWith("MECHANICUS_"))return new ProcurementPolicy("Mechanist requisition procurement",7,55,90,12);if(f==Faction.SCAVENGER||f==Faction.MUTANT)return new ProcurementPolicy("irregular salvage-route procurement",12,70,75,2);if(f==Faction.HIVER||n.startsWith("HIVER_"))return new ProcurementPolicy("civilian merchant procurement",10,50,100,5);return new ProcurementPolicy("ordinary merchant procurement",9,60,100,5);}
    private static int unitProcurementCost(ShipmentProvenanceRecord s){return s==null?0:Math.max(1,(int)Math.ceil(s.sourceProcurementCost/(double)Math.max(1,s.quantity)));}
    private static String eventModifier(World world){String text=safe(world==null?null:world.zoneConflictLossHistory).toLowerCase(Locale.ROOT);ArrayList<String> parts=new ArrayList<>();String structured=TopDownWorldEventAuthority.shipmentModifierSummary(world);if(!structured.isBlank())parts.add(structured);for(String term:List.of("shipment interception","freight interception","cargo piracy","raided shipment","shipment seizure","shipment delay","freight delay","customs delay","rail rerouting","delayed freight","blockade","interdiction","route closure","contamination","counterfeit"))if(text.contains(term))parts.add(term);return parts.isEmpty()?"no active shipment event":String.join(", ",parts);}
    private static String route(ItemProvenanceRecord p,String trace,String node,String destination){String base=p!=null&&!safe(p.route).isBlank()?p.route:(contains(trace,"off-world")?"off-world supplier -> rail customs":"outside-sector supplier -> rail freight");return base+" -> "+node+" -> "+destination;}
    private static boolean externalMovement(String text){return contains(text,"outside-sector","off-world","import shipment","incoming shipment","merchant import","noble import","military import","black-market import","external freight");}
    private static String append(String prior,String next){if(safe(next).isBlank())return safe(prior);if(safe(prior).isBlank())return next;if(prior.contains(next))return prior;return prior+"; "+next;}
    private static void appendSummary(TraderSession t,String line){if(t==null||safe(line).isBlank())return;if(safe(t.supplyChainSummary).isBlank())t.supplyChainSummary=line;else if(!t.supplyChainSummary.contains(line))t.supplyChainSummary+=" | "+line;}
    private static int days(int d){return GamePanel.TURNS_PER_HOUR*GamePanel.HOURS_PER_DAY*Math.max(1,d);}private static String safe(String s){return s==null?"":s;}private static boolean contains(String text,String...terms){String low=safe(text).toLowerCase(Locale.ROOT);for(String term:terms)if(term!=null&&!term.isBlank()&&low.contains(term.toLowerCase(Locale.ROOT)))return true;return false;}
}
