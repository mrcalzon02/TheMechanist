package mechanist;

import java.util.*;

/** Limb replacement, fitting and upkeep shared by player and NPC support state. No wireless control. */
final class CyberneticLimbAuthority {
    record Part(String item,String segment,boolean powered,int grade) { }
    static final LinkedHashMap<String,Part> PARTS=new LinkedHashMap<>();
    static {
        for(String segment:List.of("Arm","Leg","Hand","Foot")) {
            register("Basic prosthetic "+segment.toLowerCase(Locale.ROOT),segment,false,0);
            register("Medical prosthetic "+segment.toLowerCase(Locale.ROOT),segment,false,1);
            register("Industrial cybernetic "+segment.toLowerCase(Locale.ROOT),segment,true,2);
            register("Military cybernetic "+segment.toLowerCase(Locale.ROOT),segment,true,3);
        }
    }
    private static void register(String item,String segment,boolean powered,int grade) {PARTS.put(item,new Part(item,segment,powered,grade));}
    static final class Limb {
        String root,item,serial;int quality,integrity=100,power=100,calibration=100;long lastTick,wear;boolean jammed;
        Limb(String root,String item,String serial,int quality,long turn) {this.root=root;this.item=item;this.serial=serial;this.quality=quality;lastTick=turn;}
        boolean online() {Part p=part(item);return integrity>0 && calibration>0 && !jammed && (p!=null && (!p.powered || power>0));}
        String save() {return String.join(",",NpcEntity.enc(root),NpcEntity.enc(item),NpcEntity.enc(serial),""+quality,""+integrity,""+power,""+calibration,""+lastTick,""+wear,""+jammed);}
        static Limb read(String row) {
            String[] p=row.split(",",-1);Limb l=new Limb(NpcEntity.dec(p[0]),NpcEntity.dec(p[1]),NpcEntity.dec(p[2]),Integer.parseInt(p[3]),Long.parseLong(p[7]));
            if(part(l.item)==null || !fits(part(l.item),l.root) || l.quality<0 || l.quality>7) throw new IllegalArgumentException("Unknown fitted limb");
            l.integrity=clamp(Integer.parseInt(p[4]));l.power=clamp(Integer.parseInt(p[5]));l.calibration=clamp(Integer.parseInt(p[6]));l.wear=Math.max(0,Long.parseLong(p[8]));l.jammed=Boolean.parseBoolean(p[9]);return l;
        }
    }
    private CyberneticLimbAuthority() { }
    private static int clamp(int n) {return Math.max(0,Math.min(100,n));}
    static Part part(String item) {if(item==null) return null;return PARTS.get(ItemQuality.stripManufacturingIdentity(item));}
    static void catalog(LinkedHashMap<String,ItemDef> items) {
        for(Part p:PARTS.values()) ItemCatalog.add(items,p.item,"medical/prosthetic",120+p.grade*280,"medicae and augmetic suppliers","A direct-interface "+p.segment.toLowerCase(Locale.ROOT)+" replacement. Quality controls precision and service life.","Medical tab: sterile surgery, compatible mount, surgical kit, calibration tools and trained operator required. Powered limbs need cells; ordinary medicine cannot repair hardware.",false);
        for(String name:List.of("Sterile surgical kit","Augmetic calibration tools","Augmetic repair kit","Augmetic power cell"))
            ItemCatalog.add(items,name,"medical/augmetic-service",name.contains("tools")?180:45,"medicae and augmetic suppliers","Limb installation or maintenance supply.","Use the Medical tab to fit, calibrate, repair or recharge a replacement. Tools are reusable; kits and cells are finite.",false);
        ItemCatalog.add(items,"Removed augmetic scrap","salvage",12,"removed or destroyed prosthetic","Unfitted damaged hardware; no sterile interface or functioning replacement assembly.","Salvage only; cannot be reinstalled.",false);
    }
    static boolean fits(Part p,String root) {
        if(p==null || root==null || !(root.startsWith("L ") || root.startsWith("R "))) return false;
        return root.equals(root.substring(0,2)+switch(p.segment){case "Arm"->"Upper Arm";case "Leg"->"Upper Leg";default->p.segment;});
    }
    static ActorConsumableAuthority.State state(GamePanel game,NpcEntity npc) {return npc==null?game.combatSupport:npc.combatSupport;}
    static Limb covering(ActorConsumableAuthority.State state,String region) {for(Limb l:state.limbs.values()) if(MedicalTreatmentAuthority.descendants(l.root).contains(region)) return l;return null;}
    static boolean available(ActorConsumableAuthority.State state,String region) {Limb l=covering(state,region);return l==null?!state.lostParts.contains(region):l.online();}
    static boolean mobile(GamePanel game) {
        if(game.active==null) return true;
        for(String side:List.of("L ","R ")) {boolean works=true;for(String segment:List.of("Upper Leg","Lower Leg","Foot")){BodyPart b=game.active.body.get(side+segment);if(b!=null && (b.destroyed() || !available(game.combatSupport,side+segment))) works=false;}if(works) return true;}return false;
    }
    static boolean mobile(ActorConsumableAuthority.State state) {return available(state,"L Foot") || available(state,"R Foot");}
    static int bonus(ActorConsumableAuthority.State state,boolean ranged) {
        int bonus=0;for(Limb l:state.limbs.values()) if(l.online()) {Part p=part(l.item);if(p.powered && (l.root.contains("Arm") || l.root.contains("Hand"))) bonus+=Math.max(0,p.grade-1+(l.quality-2)/3);}
        return Math.min(4,bonus);
    }
    static void sync(GamePanel game,NpcEntity npc) {
        var state=state(game,npc);
        for(Limb l:state.limbs.values()) for(String region:MedicalTreatmentAuthority.descendants(l.root)) {
            state.lostParts.remove(region);
            if(npc==null && game.active!=null) {BodyPart b=game.active.body.get(region);if(b!=null){b.missing=false;b.cybernetic=true;b.health=b.maxHealth()*l.integrity/100.0;}}
        }
    }
    static void tick(GamePanel game,NpcEntity npc) {
        var state=state(game,npc);
        for(Limb l:state.limbs.values()) {
            long elapsed=Math.max(0,game.worldTurn-l.lastTick);if(elapsed==0) continue;l.lastTick=game.worldTurn;
            long before=l.wear;l.wear=Math.min(Long.MAX_VALUE/2,l.wear+elapsed);
            Part p=part(l.item);int interval=20+l.quality*10;
            long services=l.wear/interval-before/interval;
            if(p.powered) l.power=clamp(l.power-(int)Math.min(100,l.wear/8-before/8));
            l.calibration=clamp(l.calibration-(int)Math.min(100,services));
            l.integrity=clamp(l.integrity-(int)Math.min(100,services));
            // Fault rolls derive from serial and elapsed service boundaries, never from rendering or inspection.
            for(long boundary=before/interval+1;boundary<=Math.min(l.wear/interval,before/interval+100);boundary++)
                if(l.quality<3 && Math.floorMod(Objects.hash(l.serial,boundary),16)<3-l.quality) l.jammed=true;
        }
        sync(game,npc);
    }
    static void damage(GamePanel game,NpcEntity npc,String region,int trauma) {
        Limb l=covering(state(game,npc),region);if(l==null) return;
        l.integrity=clamp(l.integrity-Math.max(1,trauma*3-Math.max(0,l.quality-2)));sync(game,npc);
        ActorCombatAuthority.supportEvent(game,npc==null?"PLAYER":npc.id,"HARDWARE "+l.serial+" integrity="+l.integrity);
    }
    static boolean has(GamePanel game,NpcEntity npc,String name) {return stock(game,npc).stream().anyMatch(i->canonical(i).equals(name));}
    private static String canonical(String item) {ItemDef d=ItemCatalog.get(item);return d==null?item:d.name;}
    private static List<String> stock(GamePanel game,NpcEntity npc) {return npc==null?game.inventory:npc.combatSupport.items;}
    private static void consume(GamePanel game,NpcEntity npc,String name) {
        for(String item:new ArrayList<>(stock(game,npc))) if(canonical(item).equals(name)) {if(npc==null) ActorConsumableAuthority.consumePlayerItem(game,game.inventory.indexOf(item),item);else npc.combatSupport.items.remove(item);return;}
        throw new IllegalStateException("Surgery resource disappeared: "+name);
    }
    private static void scrap(GamePanel game,NpcEntity npc) {
        stock(game,npc).add("Removed augmetic scrap");
        if(npc==null) game.addItemToContainerResult(GamePanel.CONTAINER_PLAYER_INVENTORY,"Player inventory","Removed augmetic scrap",ItemProvenanceRecord.found("Removed augmetic scrap",game.world,game.turn,"Surgical removal"),null,"removed augmetic salvage");
    }
    static boolean clinic(GamePanel game,NpcEntity npc) {
        int x=npc==null?game.playerX:npc.x,y=npc==null?game.playerY:npc.y;
        for(BaseObject b:game.baseObjects) if(b.integrity>0 && !b.underConstruction && Math.abs(b.x-x)+Math.abs(b.y-y)<=3 && (b.symbol=='M' || Objects.toString(b.name,"").toLowerCase(Locale.ROOT).contains("sterile"))) return true;
        return false;
    }
    static String rejection(GamePanel game,NpcEntity npc,String action,String root,String item) {
        if(game==null || game.world==null || game.active==null) return "No patient is available.";
        if(npc==null?!ActorCombatAuthority.playerAlive(game):npc.hp<=0 || npc.developerStationary || npc.isAnimalActor() || npc.faction==Faction.ROGUE_MACHINE) return "A living compatible human patient is required.";
        var state=state(game,npc);Limb l=covering(state,root);
        if(!Set.of("install","remove","repair","recharge","calibrate").contains(action)) return "Unknown limb procedure.";
        if(action.equals("install") || action.equals("remove")) {
            if(!clinic(game,npc)) return "Surgery requires a nearby medicae stall or sterile bench (within three tiles).";
            if(!game.unlockedKnowledges.contains("Cybernetic Surgery") || game.stat("Mechanics",0)<6 || game.stat("Intellect",0)<6) return "Operator needs Cybernetic Surgery, Mechanics 6 and Intellect 6.";
            if(!has(game,npc,"Sterile surgical kit") || !has(game,npc,"Augmetic calibration tools")) return "Carry a sterile surgical kit and reusable augmetic calibration tools.";
            int x=npc==null?game.playerX:npc.x,y=npc==null?game.playerY:npc.y;
            for(NpcEntity other:game.world.npcs) if(other!=npc && other.hp>0 && !other.developerStationary && Math.abs(other.x-x)+Math.abs(other.y-y)<=6 && (other.state.equals("Hostile") || !other.combatTeam.isBlank())) return "Secure the operating area before surgery.";
            if(npc!=null && Math.abs(npc.x-game.playerX)+Math.abs(npc.y-game.playerY)>2) return "The operator must stand next to the patient.";
            if(action.equals("install")) {
                Part p=part(item);if(!fits(p,root) || !stock(game,npc).contains(item)) return "Carry a compatible replacement for the selected mount.";
                if(npc==null && !game.active.body.containsKey(root)) return "Patient lacks that anatomical mount.";
                if(l!=null && !l.root.equals(root)) return "Remove the enclosing limb assembly first.";
                if(root.endsWith("Hand") || root.endsWith("Foot")) {
                    String side=root.substring(0,2);String upper=side+(root.endsWith("Hand")?"Upper Arm":"Upper Leg"),lower=side+(root.endsWith("Hand")?"Lower Arm":"Lower Leg");
                    if(!available(state,upper) || !available(state,lower) || npc==null && (game.active.body.get(upper).destroyed() || game.active.body.get(lower).destroyed())) return "Restore the parent limb before fitting an end part.";
                }
                if(p.grade==3 && !game.unlockedKnowledges.contains("Emergency Medical Authority")) return "Military hardware requires Emergency Medical Authority.";
            } else if(l==null || !l.root.equals(root)) return "Select the root of an installed assembly to remove it.";
        } else {
            if(l==null) return "No replacement is installed in this region.";
            if(action.equals("repair") && l.integrity>=100 || action.equals("recharge") && (!part(l.item).powered || l.power>=100) || action.equals("calibrate") && l.calibration>=100 && !l.jammed) return "This assembly does not need that service.";
            String resource=action.equals("repair")?"Augmetic repair kit":action.equals("recharge")?"Augmetic power cell":"Augmetic calibration tools";
            if(!has(game,npc,resource)) return "Carry "+resource+".";
            if(!action.equals("recharge") && (npc==null?game.stat("Mechanics",0):npc.intellect)<4) return "Service requires Mechanics 4 (NPC technical Intellect 4).";
        }
        return "";
    }
    static boolean execute(GamePanel game,NpcEntity npc,String action,String root,String item) {
        String refused=rejection(game,npc,action,root,item);if(!refused.isEmpty()){game.logEvent(refused);return false;}
        var state=state(game,npc);Limb l=covering(state,root);
        if(action.equals("install") || action.equals("remove")) {
            consume(game,npc,"Sterile surgical kit");
            if(action.equals("install")) {
                if(npc==null) ActorConsumableAuthority.consumePlayerItem(game,game.inventory.indexOf(item),item);else npc.combatSupport.items.remove(item);
                for(Limb old:new ArrayList<>(state.limbs.values())) if(MedicalTreatmentAuthority.descendants(root).contains(old.root)) {state.limbs.remove(old.root);scrap(game,npc);}
                l=new Limb(root,item,(npc==null?"PLAYER":npc.id)+"/"+game.worldTurn+"/"+root,ItemQuality.tierIndex(item),game.worldTurn);state.limbs.put(root,l);
                if(l.quality<2) {l.calibration=70+l.quality*10;if(npc==null) game.infectionRisk+=2-l.quality;else state.infection+=2-l.quality;}
                if(npc==null) CharacterEquipmentAndMedicalAuthority.installMedicalRecord(game.installedBodyModifications,root,CharacterEquipmentAndMedicalAuthority.MedicalLayer.CYBERNETIC,item);
            } else {
                state.limbs.remove(l.root);state.lostParts.addAll(MedicalTreatmentAuthority.descendants(l.root));scrap(game,npc);
                if(npc==null) {MedicalTreatmentAuthority.sever(game.active,root);for(String region:MedicalTreatmentAuthority.descendants(root)) game.active.body.get(region).cybernetic=false;CharacterEquipmentAndMedicalAuthority.installMedicalRecord(game.installedBodyModifications,root,CharacterEquipmentAndMedicalAuthority.MedicalLayer.CYBERNETIC,"");}
            }
        } else if(action.equals("repair")) {consume(game,npc,"Augmetic repair kit");l.integrity=Math.min(100,l.integrity+40);}
        else if(action.equals("recharge")) {consume(game,npc,"Augmetic power cell");l.power=Math.min(100,l.power+60);}
        else {l.calibration=100;l.jammed=false;}
        sync(game,npc);ActorCombatAuthority.supportEvent(game,npc==null?"PLAYER":npc.id,"LIMB "+action+" "+root+" "+(l==null?"":l.serial));
        if(npc==null) game.advanceTurn("Limb procedure: "+action+" "+root+".");else npc.lastTickTurn=game.turn;
        return true;
    }
    static boolean autoService(GamePanel game,NpcEntity npc) {
        if(npc.lastTickTurn==game.turn) return false;
        for(Limb l:npc.combatSupport.limbs.values()) for(String action:List.of("recharge","repair","calibrate"))
            if((action.equals("recharge")?l.power<=20:action.equals("repair")?l.integrity<=50:l.calibration<=20 || l.jammed) && rejection(game,npc,action,l.root,"").isEmpty()) return execute(game,npc,action,l.root,"");
        return false;
    }
    /** Explicit test-fixture fitting, not campaign generation or a surgery bypass exposed to gameplay. */
    static boolean seedCandidate(GamePanel game,NpcEntity npc,int percent,long seed) {
        if(percent<0 || percent>100) throw new IllegalArgumentException("Cybernetic percentage 0..100");
        Random rng=new Random(seed);if(rng.nextInt(100)>=percent || npc!=null && (npc.isAnimalActor() || npc.faction==Faction.ROGUE_MACHINE)) return false;
        String root=(rng.nextBoolean()?"L ":"R ")+(rng.nextBoolean()?"Upper Arm":"Upper Leg");
        String item=(rng.nextBoolean()?"Industrial":"Military")+" cybernetic "+(root.endsWith("Arm")?"arm":"leg");
        Limb l=new Limb(root,item,"TEST/"+seed,2+rng.nextInt(6),game.worldTurn);state(game,npc).limbs.put(root,l);sync(game,npc);return true;
    }
    static void offers(TraderSession trader,NpcEntity npc) {
        String role=Objects.toString(npc==null?trader.archetype:npc.role,"").toLowerCase(Locale.ROOT);
        if(!role.contains("medic") && !role.contains("clinic") && !role.contains("doctor") && !role.contains("augmet")) return;
        for(ItemDef def:ItemCatalog.ITEMS.values()) if(def.category.equals("medical/prosthetic") || def.category.equals("medical/augmetic-service")) {
            if(trader.offers.stream().noneMatch(o->ItemQuality.stripQuality(o.name).equals(def.name))) trader.offers.add(new TradeOffer(def.name,def.category,def.basePrice,def.description));
        }
    }
    static String summary(ActorConsumableAuthority.State state,String region) {
        Limb l=covering(state,region);return l==null?"No functional replacement fitted. Legacy records are descriptive only.":l.item+" / "+l.root+" / "+ItemQuality.NAMES[l.quality]+" / "+(l.online()?"online":"OFFLINE")+" / integrity "+l.integrity+"%, power "+l.power+"%, calibration "+l.calibration+"% / "+l.serial;
    }
}
