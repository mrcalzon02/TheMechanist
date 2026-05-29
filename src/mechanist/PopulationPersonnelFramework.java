package mechanist;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

class PersonnelProvenanceRecord {
    String originMode = "arcology-born";
    String originZone = "unknown zone";
    String originRoom = "unrecorded room";
    String originSiteId = "local-population";
    String upbringing = "unrecorded upbringing";
    String arrivalRoute = "local corridor assignment";
    String populationPool = "unassigned pool";
    String backstory = "No personnel provenance recorded.";

    String summary(){ return "Origin: " + backstory; }
    String save(){ return join(originMode, originZone, originRoom, originSiteId, upbringing, arrivalRoute, populationPool, backstory); }
    static PersonnelProvenanceRecord parse(String text){
        PersonnelProvenanceRecord p = new PersonnelProvenanceRecord();
        if(text == null || text.isEmpty()) return p;
        String[] a = text.split("\\t", -1);
        if(a.length>0) p.originMode=a[0]; if(a.length>1) p.originZone=a[1]; if(a.length>2) p.originRoom=a[2]; if(a.length>3) p.originSiteId=a[3];
        if(a.length>4) p.upbringing=a[4]; if(a.length>5) p.arrivalRoute=a[5]; if(a.length>6) p.populationPool=a[6]; if(a.length>7) p.backstory=a[7];
        return p;
    }
    static String join(String... parts){ StringBuilder sb=new StringBuilder(); for(int i=0;i<parts.length;i++){ if(i>0) sb.append('\t'); sb.append(parts[i]==null?"":parts[i].replace('\t',' ')); } return sb.toString(); }
}


class PersonnelReplacementRequest {
    String deadNpcId, deadName, source, sourceLedgerId, reason; Faction faction; int dueTurn; int x,y, sourceRoomId = -1;
    String saveLine(){ return enc(deadNpcId)+"|"+enc(deadName)+"|"+(faction==null?Faction.NONE.name():faction.name())+"|"+dueTurn+"|"+x+"|"+y+"|"+enc(source)+"|"+enc(reason)+"|"+sourceRoomId+"|"+enc(sourceLedgerId); }
    static PersonnelReplacementRequest parse(String s){ try{ String[] a=s.split("\\|",10); if(a.length<8) return null; PersonnelReplacementRequest r=new PersonnelReplacementRequest(); r.deadNpcId=dec(a[0]); r.deadName=dec(a[1]); r.faction=Faction.valueOf(a[2]); r.dueTurn=Integer.parseInt(a[3]); r.x=Integer.parseInt(a[4]); r.y=Integer.parseInt(a[5]); r.source=dec(a[6]); r.reason=dec(a[7]); if(a.length>=9) try{ r.sourceRoomId=Integer.parseInt(a[8]); }catch(Exception ignored){} if(a.length>=10) r.sourceLedgerId=dec(a[9]); return r; }catch(Exception e){ return null; } }
    static String enc(String s){ return Base64.getUrlEncoder().withoutPadding().encodeToString((s==null?"":s).getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    static String dec(String s){ try{return new String(Base64.getUrlDecoder().decode(s), java.nio.charset.StandardCharsets.UTF_8);}catch(Exception e){return "";} }
}



class RoomPopulationLedger {
    String id = "population.unassigned";
    int roomId = -1;
    String roomName = "unrecorded room";
    Faction faction = Faction.NONE;
    String sourceKind = "local roster";
    String sourceLabel = "unassigned population ledger";
    String facilityId = "";
    String facilityPurpose = "";
    String facilityEstablishedBy = "";
    String facilityProductFocus = "";
    String facilityHistoricNote = "";
    int capacity = 0;
    int available = 0;
    int assigned = 0;
    int dead = 0;

    String saveLine(){
        return enc(id)+"|"+roomId+"|"+enc(roomName)+"|"+(faction==null?Faction.NONE.name():faction.name())+"|"+enc(sourceKind)+"|"+enc(sourceLabel)+"|"+capacity+"|"+available+"|"+assigned+"|"+dead
            +"|"+enc(facilityId)+"|"+enc(facilityPurpose)+"|"+enc(facilityEstablishedBy)+"|"+enc(facilityProductFocus)+"|"+enc(facilityHistoricNote);
    }
    static RoomPopulationLedger parse(String s){
        try{
            String[] a=s.split("\\|",15); if(a.length<10) return null;
            RoomPopulationLedger l=new RoomPopulationLedger();
            l.id=dec(a[0]); l.roomId=Integer.parseInt(a[1]); l.roomName=dec(a[2]); l.faction=Faction.valueOf(a[3]); l.sourceKind=dec(a[4]); l.sourceLabel=dec(a[5]); l.capacity=Integer.parseInt(a[6]); l.available=Integer.parseInt(a[7]); l.assigned=Integer.parseInt(a[8]); l.dead=Integer.parseInt(a[9]);
            if(a.length>=15){ l.facilityId=dec(a[10]); l.facilityPurpose=dec(a[11]); l.facilityEstablishedBy=dec(a[12]); l.facilityProductFocus=dec(a[13]); l.facilityHistoricNote=dec(a[14]); }
            return l;
        }catch(Exception e){ return null; }
    }
    String facilitySummary(){
        if(facilityId == null || facilityId.isBlank()) return "no named facility linked";
        return facilityId + " / " + facilityPurpose + " / established by " + facilityEstablishedBy + " / focus " + facilityProductFocus;
    }
    static String enc(String s){ return Base64.getUrlEncoder().withoutPadding().encodeToString((s==null?"":s).getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    static String dec(String s){ try{return new String(Base64.getUrlDecoder().decode(s), java.nio.charset.StandardCharsets.UTF_8);}catch(Exception e){return "";} }
}



class AnimalProfile {
    final String id, displayName, category, role, state, factionBias;
    final int hp, portraitBase, danger;
    AnimalProfile(String id, String displayName, String category, String role, String state, String factionBias, int hp, int portraitBase, int danger) {
        this.id=id; this.displayName=displayName; this.category=category; this.role=role; this.state=state; this.factionBias=factionBias; this.hp=hp; this.portraitBase=portraitBase; this.danger=danger;
    }
}


class AnimalPopulationApi {
    private AnimalPopulationApi(){}
    static final AnimalProfile[] SEWER_WILD = new AnimalProfile[]{
        new AnimalProfile("sump-rat-swarm","Sump rat swarm","wild-animal","Wild Creature - sump rat swarm","Hostile","sewer",4,350,2),
        new AnimalProfile("blind-sewer-eel","Blind sewer eel","wild-animal","Wild Creature - blind sewer eel","Hostile","sewer",7,351,3),
        new AnimalProfile("corpse-feeder","Corpse feeder","wild-animal","Wild Creature - corpse feeder","Hostile","sewer",9,352,4),
        new AnimalProfile("fungus-sick-hound","Fungus-sick hound","wild-animal","Wild Creature - fungus-sick hound","Hostile","sewer",10,353,4),
        new AnimalProfile("sump-beast-juvenile","Juvenile sump-beast","wild-animal","Wild Creature - juvenile sump-beast","Hostile","sewer",16,354,5)
    };
    static final AnimalProfile[] URBAN_WILD = new AnimalProfile[]{
        new AnimalProfile("feral-arcology-dog","Feral arcology dog","wild-animal","Wild Creature - feral arcology dog","Wander","urban",8,355,2),
        new AnimalProfile("ash-crow","Ash crow","wild-animal","Wild Creature - ash crow","Wander","urban",3,356,1),
        new AnimalProfile("wire-louse-swarm","Wire-louse swarm","wild-animal","Wild Creature - wire-louse swarm","Wander","urban",5,357,2),
        new AnimalProfile("trash-mite-cloud","Trash mite cloud","wild-animal","Wild Creature - trash mite cloud","Wander","urban",4,358,1)
    };
    static final AnimalProfile[] FARM = new AnimalProfile[]{
        new AnimalProfile("starch-hog","Starch hog","farm-animal","Farm Animal - starch hog","Penned","farm",16,359,1),
        new AnimalProfile("vat-goat","Vat goat","farm-animal","Farm Animal - vat goat","Penned","farm",10,360,1),
        new AnimalProfile("ploin-fowl","Ploin fowl","farm-animal","Farm Animal - ploin fowl","Penned","farm",4,361,1),
        new AnimalProfile("corpse-starch-grub-vat","Corpse-starch grub vat beast","farm-animal","Farm Animal - grub stock","Penned","farm",6,362,1)
    };
    static final AnimalProfile[] KENNEL = new AnimalProfile[]{
        new AnimalProfile("civic Wardens-cyber-mastiff","Cyber-mastiff","kennel-animal","Kennel Animal - cyber-mastiff","Guard","civic Wardens",18,363,5),
        new AnimalProfile("guard-voidhound","Guard voidhound","kennel-animal","Kennel Animal - voidhound","Guard","guard",14,364,4),
        new AnimalProfile("noble-gene-hound","Noble gene-hound","kennel-animal","Kennel Animal - gene-hound","Guard","noble",12,365,3)
    };
    static final AnimalProfile[] PETS = new AnimalProfile[]{
        new AnimalProfile("lap-malamute","Spire lap-malamute","pet","Pet - lap-malamute","Following","pet",7,366,1),
        new AnimalProfile("servo-cat","Servo-cat","pet","Pet - servo-cat","Following","pet",5,367,1),
        new AnimalProfile("song-rat","Caged song-rat","pet","Pet - song-rat","Following","pet",2,368,0),
        new AnimalProfile("mutant-scrag-pup","Mutant scrag-pup","pet","Pet - scrag-pup","Following","pet",7,369,2),
        new AnimalProfile("heretic-eye-moth","Heretic eye-moth","pet","Pet - eye-moth","Following","pet",3,370,2),
        new AnimalProfile("hiver-tin-lizard","Tin lizard","pet","Pet - tin lizard","Following","pet",4,371,1),
        new AnimalProfile("noble-glowfish-bowl","Noble glowfish bowl","pet","Pet - glowfish servobowl","Following","pet",2,372,0)
    };
    static final String[] SERVANT_ROLES = {"Household Servant","Kitchen Servant","Chef","Butler","Laundry Attendant","Retainer Porter","Pantry Clerk","Manners Tutor"};

    static class Result { int wild, farm, kennel, pets, servants; String summary(){ return "wild="+wild+" farm="+farm+" kennel="+kennel+" pets="+pets+" servants="+servants; } }

    static Result seedAnimalsAndServants(World w, Random r) {
        Result result = new Result();
        if(w == null || r == null || w.rooms == null || w.rooms.isEmpty()) return result;
        result.wild = seedWild(w, r);
        result.farm = seedFarmStock(w, r);
        result.kennel = seedKennels(w, r);
        result.servants = seedNobleServants(w, r);
        result.pets = seedPetsForResidents(w, r);
        if(result.wild+result.farm+result.kennel+result.pets+result.servants > 0) DebugLog.audit("ANIMAL_POPULATION", "zone="+w.zoneType.label+" layer="+w.layerText()+" "+result.summary()+" npcs="+w.npcs.size());
        return result;
    }

    static int seedWild(World w, Random r) {
        int target = 0;
        if(w.sewerLayer || w.zoneType == ZoneType.SEWER_CONDUIT) target += 2 + r.nextInt(4);
        if(w.zoneType == ZoneType.MUTANT_SEWER_CAMP || w.zoneType == ZoneType.CULTIST_SEWER_CAMP || w.zoneType == ZoneType.MUTANT_WARRENS) target += 3 + r.nextInt(5);
        if(w.zoneType == ZoneType.TRASH_WARREN || w.zoneType == ZoneType.GANGER_TURF) target += 1 + r.nextInt(3);
        if(w.zoneType == ZoneType.NEUTRAL_CIVILIAN_FLOOR || w.zoneType == ZoneType.HAB_STACK) target += r.nextDouble() < 0.35 ? 1 : 0;
        target = Math.min(target, Math.max(1, w.rooms.size()/2));
        int made=0;
        for(int i=0; i<target; i++) {
            AnimalProfile p = pick(r, (w.sewerLayer || w.zoneType==ZoneType.SEWER_CONDUIT || w.zoneType==ZoneType.MUTANT_SEWER_CAMP || w.zoneType==ZoneType.CULTIST_SEWER_CAMP) ? SEWER_WILD : URBAN_WILD);
            Point pt = randomAnimalPoint(w, r, true);
            if(pt == null) continue;
            NpcEntity n = createAnimal(p, Faction.NONE, w.zoneType, pt.x, pt.y, r, null);
            if(p.danger >= 3 || w.zoneType == ZoneType.MUTANT_SEWER_CAMP || w.zoneType == ZoneType.CULTIST_SEWER_CAMP || w.zoneType == ZoneType.MUTANT_WARRENS) { n.faction = Faction.MUTANT; n.state = "Hostile"; }
            attachAnimalProvenance(n, w, Math.max(0, w.roomIdAt(pt.x, pt.y)), "wild/feral ecological spawn", r);
            w.npcs.add(n); made++;
        }
        return made;
    }

    static int seedFarmStock(World w, Random r) {
        int made=0;
        for(int i=1; i<w.rooms.size(); i++) {
            RoomProfile rp = i < w.roomProfiles.size() ? w.roomProfiles.get(i) : null;
            String low = roomText(rp).toLowerCase(Locale.ROOT);
            boolean farmish = low.contains("food") || low.contains("kitchen") || low.contains("galley") || low.contains("pantry") || low.contains("cafeteria") || low.contains("hydro") || low.contains("garden") || low.contains("nursery");
            if(!farmish && !(w.zoneType==ZoneType.HAB_STACK && r.nextDouble()<0.05)) continue;
            if(r.nextDouble() > farmChanceFor(w, i)) continue;
            int count = 1 + (r.nextDouble()<0.25 ? 1 : 0);
            for(int c=0; c<count; c++) {
                Point pt = w.randomOpenPointInRoom(w.rooms.get(i));
                if(pt == null) continue;
                Faction owner = w.roomFaction(i);
                AnimalProfile p = pick(r, FARM);
                NpcEntity n = createAnimal(p, owner == null ? Faction.NONE : owner, w.zoneType, pt.x, pt.y, r, null);
                attachAnimalProvenance(n, w, i, "farm/food-room animal stock", r);
                w.npcs.add(n); made++;
            }
        }
        return made;
    }

    static int seedKennels(World w, Random r) {
        int made=0;
        for(int i=1; i<w.rooms.size(); i++) {
            RoomProfile rp = i < w.roomProfiles.size() ? w.roomProfiles.get(i) : null;
            Faction f = w.roomFaction(i);
            String low = roomText(rp).toLowerCase(Locale.ROOT);
            boolean kennelish = low.contains("security") || low.contains("barracks") || low.contains("guard") || low.contains("watch") || low.contains("training") || low.contains("kennel");
            if(!kennelish && !(f==Faction.CIVIC_WARDENS || f==Faction.IMPERIAL_GUARD || f==Faction.SORORITAS || (f!=null && f.name().startsWith("NOBLE")))) continue;
            double chance = (f==Faction.CIVIC_WARDENS ? 0.36 : f==Faction.IMPERIAL_GUARD ? 0.22 : (f!=null && f.name().startsWith("NOBLE")) ? 0.18 : 0.08);
            if(r.nextDouble() > chance) continue;
            Point pt = w.randomOpenPointInRoom(w.rooms.get(i));
            if(pt == null) continue;
            AnimalProfile p = f==Faction.CIVIC_WARDENS ? KENNEL[0] : (f==Faction.IMPERIAL_GUARD ? KENNEL[1] : KENNEL[Math.floorMod(r.nextInt(), KENNEL.length)]);
            NpcEntity n = createAnimal(p, f == null ? Faction.NONE : f, w.zoneType, pt.x, pt.y, r, null);
            n.state = "Guard";
            attachAnimalProvenance(n, w, i, "kennel/security animal stock", r);
            w.npcs.add(n); made++;
        }
        return made;
    }

    static int seedNobleServants(World w, Random r) {
        if(!(w.zoneType == ZoneType.NOBLE_SERVICE_SPINE || w.zoneType == ZoneType.SECTOR_GOVERNORS_MANSION)) return 0;
        int made=0;
        for(int i=1; i<w.rooms.size(); i++) {
            RoomProfile rp = i < w.roomProfiles.size() ? w.roomProfiles.get(i) : null;
            String low = roomText(rp).toLowerCase(Locale.ROOT);
            boolean serviceRoom = low.contains("servant") || low.contains("kitchen") || low.contains("pantry") || low.contains("dining") || low.contains("laundry") || low.contains("apartment") || low.contains("dormitory") || low.contains("gallery") || low.contains("salon");
            if(!serviceRoom && r.nextDouble() > 0.10) continue;
            int count = 1 + (r.nextDouble()<0.35 ? 1 : 0);
            for(int c=0; c<count; c++) {
                Point pt = w.randomOpenPointInRoom(w.rooms.get(i));
                if(pt == null) continue;
                Faction f = w.roomFaction(i);
                if(f == null || f == Faction.NONE || !f.name().startsWith("NOBLE")) f = w.npcFactionForRoom(i);
                NpcEntity n = NpcEntity.create(f, w.zoneType, pt.x, pt.y, r);
                n.role = SERVANT_ROLES[Math.floorMod(r.nextInt(), SERVANT_ROLES.length)];
                n.state = n.role.contains("Chef") ? "Kitchen Service" : (n.role.contains("Butler") ? "Household Attendance" : "Service");
                n.symbol = 'n';
                n.name = servantName(n, r);
                n.portraitIndex = 425 + Math.floorMod(r.nextInt(), 50);
                n.factionRank = n.role.contains("Butler") || n.role.contains("Chef") ? 6 : 8;
                n.factionRankTitle = n.role.contains("Butler") ? "House Butler" : (n.role.contains("Chef") ? "House Chef" : "Household Servant");
                n.factionRankScope = n.role.contains("Butler") ? "controls household service access" : "assigned to noble household service";
                n.equippedRangedWeapon = ""; n.equippedExplosive = ""; n.equippedMeleeWeapon = "Knife"; n.equippedArmor = "Household uniform";
                PersonnelPopulationApi.attachExistingNpcToRoomLedger(n, w, i, r);
                w.npcs.add(n); made++;
            }
        }
        return made;
    }

    static int seedPetsForResidents(World w, Random r) {
        if(w.npcs == null || w.npcs.isEmpty()) return 0;
        ArrayList<NpcEntity> owners = new ArrayList<>(w.npcs);
        int made=0, cap=Math.max(2, w.rooms.size()/2);
        for(NpcEntity owner : owners) {
            if(owner == null || owner.isAnimalActor()) continue;
            double chance = petChanceFor(w, owner);
            if(r.nextDouble() > chance) continue;
            Point pt = adjacentOpen(w, owner.x, owner.y, r);
            if(pt == null) continue;
            AnimalProfile p = petProfileFor(owner, r);
            NpcEntity pet = createAnimal(p, owner.faction, w.zoneType, pt.x, pt.y, r, owner);
            attachAnimalProvenance(pet, w, Math.max(0, w.roomIdAt(pt.x, pt.y)), "companion pet owned/kept by " + owner.name, r);
            w.npcs.add(pet); made++;
            if(made >= cap) break;
        }
        return made;
    }

    static double petChanceFor(World w, NpcEntity owner) {
        if(owner == null) return 0;
        Faction f = owner.faction;
        if(f == Faction.NOBLE || (f != null && f.name().startsWith("NOBLE")) || w.zoneType==ZoneType.SECTOR_GOVERNORS_MANSION || w.zoneType==ZoneType.NOBLE_SERVICE_SPINE) return 0.34;
        if(f == Faction.CIVIC_WARDENS) return 0.12;
        if(f == Faction.HIVER || (f != null && f.name().startsWith("HIVER")) || w.zoneType==ZoneType.HAB_STACK || w.zoneType==ZoneType.NEUTRAL_CIVILIAN_FLOOR) return 0.08;
        if(f == Faction.MUTANT || f == Faction.CULTIST || f == Faction.HERETIC) return 0.10;
        if(f == Faction.BANDIT || (f != null && f.name().startsWith("GANGER"))) return 0.06;
        return 0.035;
    }

    static AnimalProfile petProfileFor(NpcEntity owner, Random r) {
        if(owner != null && (owner.faction == Faction.MUTANT)) return PETS[3];
        if(owner != null && (owner.faction == Faction.CULTIST || owner.faction == Faction.HERETIC)) return PETS[4];
        if(owner != null && (owner.faction == Faction.NOBLE || (owner.faction != null && owner.faction.name().startsWith("NOBLE")))) return pick(r, new AnimalProfile[]{PETS[0],PETS[1],PETS[6]});
        if(owner != null && owner.faction == Faction.CIVIC_WARDENS) return KENNEL[0];
        return pick(r, PETS);
    }

    static double farmChanceFor(World w, int roomId) {
        Faction f = w.roomFaction(roomId);
        double base = 0.10;
        if(w.zoneType==ZoneType.NOBLE_SERVICE_SPINE || w.zoneType==ZoneType.SECTOR_GOVERNORS_MANSION) base = 0.20;
        if(w.zoneType==ZoneType.HAB_STACK || w.zoneType==ZoneType.NEUTRAL_CIVILIAN_FLOOR) base = 0.12;
        if(f == Faction.NOBLE || (f != null && f.name().startsWith("NOBLE"))) base += 0.06;
        return Math.min(0.40, base);
    }

    static Point randomAnimalPoint(World w, Random r, boolean allowCorridor) {
        if(w == null) return null;
        if(!w.rooms.isEmpty()) {
            for(int tries=0; tries<80; tries++) {
                Rectangle rr = w.rooms.get(Math.floorMod(r.nextInt(), w.rooms.size()));
                Point p = w.randomOpenPointInRoom(rr);
                if(p != null) return p;
            }
        }
        if(allowCorridor) return w.randomOpenPoint(r);
        return null;
    }

    static Point adjacentOpen(World w, int x, int y, Random r) {
        int[][] dirs={{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,-1},{1,-1},{-1,1}};
        int start = r == null ? 0 : Math.floorMod(r.nextInt(), dirs.length);
        for(int k=0; k<dirs.length; k++) {
            int[] d = dirs[(start+k)%dirs.length];
            int nx=x+d[0], ny=y+d[1];
            if(w.inBounds(nx,ny) && w.walkable(nx,ny) && w.npcAt(nx,ny)==null) return new Point(nx,ny);
        }
        return null;
    }

    static NpcEntity createAnimal(AnimalProfile p, Faction f, ZoneType z, int x, int y, Random r, NpcEntity owner) {
        if(r == null) r = new Random();
        NpcEntity n = new NpcEntity();
        n.faction = f == null ? Faction.NONE : f;
        n.x=x; n.y=y; n.homeX=x; n.homeY=y;
        n.creatureKind = p == null ? "wild-animal" : p.category;
        n.animalProfileId = p == null ? "unknown-animal" : p.id;
        n.role = p == null ? "Wild Creature" : p.role;
        n.state = p == null ? "Wander" : p.state;
        n.name = p == null ? "Uncatalogued animal" : p.displayName;
        n.symbol = n.creatureKind.equals("pet") ? 'p' : (n.creatureKind.equals("farm-animal") ? 'f' : 'w');
        n.hp = p == null ? 6 : Math.max(1, p.hp);
        n.intellect = n.creatureKind.equals("pet") ? 3 : 2;
        n.portraitIndex = (p == null ? 350 : p.portraitBase) + Math.floorMod(r.nextInt(), 25);
        n.numericId = Math.abs(Objects.hash(n.name, n.animalProfileId, x, y, r.nextInt()));
        n.id = "ANIMAL-" + n.animalProfileId + "-" + n.numericId;
        n.companionOf = owner == null ? "" : owner.id;
        n.factionRank = 8; n.factionRankTitle = "Unranked animal"; n.factionRankScope = "not part of faction command";
        n.equippedMeleeWeapon=""; n.equippedRangedWeapon=""; n.equippedArmor=""; n.equippedExplosive=""; n.loadedShots=0; n.ammoReloadsRemaining=0;
        n.idleBias = 2 + r.nextInt(5); n.routineOffset = r.nextInt(24);
        return n;
    }

    static void attachAnimalProvenance(NpcEntity n, World w, int roomId, String assignment, Random r) {
        if(n == null || w == null) return;
        PersonnelProvenanceRecord p = PersonnelProvenanceApi.create(n.faction, w.zoneType, PersonnelProvenanceApi.roomLabel(w, roomId), w.sectorX, w.sectorY, w.zoneX, w.zoneY, w.floor, w.sewerLayer, r == null ? new Random(n.numericId) : r);
        p.originMode = n.creatureKind;
        p.originSiteId = "animal." + w.locationKey() + ".r" + roomId + "." + n.animalProfileId;
        p.populationPool = animalPoolFor(n, w);
        p.upbringing = "Animal ledger entry: " + n.role + "; kept/spawned by " + p.populationPool + "; assignment=" + assignment + ".";
        p.arrivalRoute = "seeded through AnimalPopulationApi at " + w.zoneType.label + "; room=" + PersonnelProvenanceApi.roomLabel(w, roomId);
        p.backstory = n.name + " is tracked as " + n.creatureKind + " profile " + n.animalProfileId + "; " + p.upbringing;
        n.provenance = p;
    }

    static String animalPoolFor(NpcEntity n, World w) {
        if(n == null) return "unrecorded animal ledger";
        if(n.creatureKind.equals("pet")) return (n.faction == null ? "unaligned" : n.faction.label) + " household/personal pet ledger";
        if(n.creatureKind.equals("farm-animal")) return (n.faction == null ? "local" : n.faction.label) + " farm/fodder stock ledger";
        if(n.creatureKind.equals("kennel-animal")) return (n.faction == null ? "security" : n.faction.label) + " kennel/security animal ledger";
        return (w == null ? "arcology" : w.zoneType.label) + " wild ecology ledger";
    }

    static String servantName(NpcEntity n, Random r) {
        String base = CharacterCreationAuthority.randomNpcName(n == null ? Faction.NOBLE : n.faction, r == null ? new Random() : r);
        String first = base == null ? "Bound Labor Automaton" : base.split(" ")[0];
        String role = n == null || n.role == null ? "Servant" : n.role;
        if(role.contains("Chef")) return "Chef " + first;
        if(role.contains("Butler")) return "Butler " + first;
        if(role.contains("Tutor")) return "Tutor " + first;
        return "Servant " + first;
    }

    static String roomText(RoomProfile rp) { return rp == null ? "" : ((rp.name==null?"":rp.name)+" "+(rp.descriptor==null?"":rp.descriptor)+" "+(rp.featureText==null?"":rp.featureText)); }
    static AnimalProfile pick(Random r, AnimalProfile[] a) { return a[Math.floorMod(r == null ? 0 : r.nextInt(), a.length)]; }
}


class PersonnelPopulationApi {
    private PersonnelPopulationApi(){}

    static void ensureLedgers(World w, Random r){
        if(w == null) return;
        if(w.roomPopulationLedgers == null) w.roomPopulationLedgers = new ArrayList<>();
        if(!w.roomPopulationLedgers.isEmpty()) return;
        for(int i=0; i<w.rooms.size(); i++){
            RoomProfile rp = i < w.roomProfiles.size() ? w.roomProfiles.get(i) : null;
            Faction f = i < w.roomFactions.size() ? w.roomFactions.get(i) : Faction.NONE;
            if(f == Faction.NONE && rp != null && rp.faction != null) f = rp.faction;
            String name = rp == null ? ("room #" + i) : rp.name;
            String low = (name + " " + (rp==null?"":rp.descriptor) + " " + (rp==null?"":rp.featureText)).toLowerCase(Locale.ROOT);
            int cap = capacityFor(low, w.zoneType, f, i == 0);
            if(cap <= 0) continue;
            RoomPopulationLedger l = new RoomPopulationLedger();
            l.roomId = i;
            l.roomName = name;
            l.faction = normalizeFaction(f, w.zoneType);
            l.sourceKind = sourceKindFor(low, w.zoneType, l.faction);
            l.sourceLabel = labelFor(l.sourceKind, l.faction, name, w.zoneType);
            l.capacity = cap;
            int reserve = Math.max(1, cap/3);
            l.available = Math.max(0, cap - reserve);
            l.assigned = 0;
            l.dead = 0;
            l.id = "pop." + w.locationKey() + ".r" + i + "." + l.sourceKind.replace(' ','-');
            linkLedgerToFacility(w, l);
            w.roomPopulationLedgers.add(l);
        }
        if(w.roomPopulationLedgers.isEmpty()){
            RoomPopulationLedger l = new RoomPopulationLedger();
            l.roomId = 0; l.roomName = w.roomProfiles.isEmpty()?"central plaza":w.roomProfiles.get(0).name; l.faction = normalizeFaction(Faction.NONE, w.zoneType);
            l.sourceKind = "transient corridor population"; l.sourceLabel = "transient corridor population for " + w.zoneType.label; l.capacity = 6; l.available = 4; l.id = "pop." + w.locationKey() + ".fallback";
            linkLedgerToFacility(w, l);
            w.roomPopulationLedgers.add(l);
        }
        DebugLog.audit("PERSONNEL_POP_LEDGER_SEED", "zone="+w.zoneType.label+" ledgers="+w.roomPopulationLedgers.size()+" rooms="+w.rooms.size()+" facilityLinked="+facilityLinkedCount(w));
    }

    static void linkLedgerToFacility(World w, RoomPopulationLedger l){
        if(w == null || l == null) return;
        ZoneFacilityLedgerEntry e = ZoneFacilityHistoryApi.matchFacilityForRoom(w, l);
        if(e == null) return;
        l.facilityId = e.id;
        l.facilityPurpose = e.purpose;
        l.facilityEstablishedBy = e.establishedBy;
        l.facilityProductFocus = e.productFocus;
        l.facilityHistoricNote = e.historicNote;
        if(l.sourceLabel == null || l.sourceLabel.isBlank() || l.sourceLabel.startsWith("unassigned")){
            l.sourceLabel = e.populationSource + " at " + e.roomType;
        } else if(!l.sourceLabel.contains(e.id)) {
            l.sourceLabel = l.sourceLabel + " via " + e.id + " " + e.roomType;
        }
    }

    static NpcEntity createResidentFromRoom(World w, int roomId, Faction faction, int x, int y, Random r){
        ensureLedgers(w, r);
        RoomPopulationLedger ledger = drawLedgerFor(w, roomId, faction, r, false);
        Faction f = faction == null || faction == Faction.NONE ? (ledger == null ? Faction.NONE : ledger.faction) : faction;
        NpcEntity n = NpcEntity.create(f, w==null?ZoneType.NEUTRAL_CIVILIAN_FLOOR:w.zoneType, x, y, r==null?new Random():r);
        attachProvenance(n, w, roomId, ledger, "initial staffed resident", r);
        if(ledger != null) { ledger.assigned++; ledger.available = Math.max(0, ledger.available - 1); }
        return n;
    }

    static void attachExistingNpcToRoomLedger(NpcEntity npc, World w, int roomId, Random r){
        if(npc == null || w == null) return;
        ensureLedgers(w, r);
        RoomPopulationLedger ledger = drawLedgerFor(w, roomId, npc.faction, r, false);
        attachProvenance(npc, w, roomId, ledger, "attached service actor", r);
        if(ledger != null) { ledger.assigned++; ledger.available = Math.max(0, ledger.available - 1); }
    }

    static PersonnelReplacementRequest fillReplacementRequest(World w, PersonnelReplacementRequest req, Random r){
        if(w == null || req == null) return req;
        ensureLedgers(w, r);
        RoomPopulationLedger l = drawLedgerFor(w, -1, req.faction, r, true);
        if(l != null){ req.source = l.sourceLabel + (l.facilityId == null || l.facilityId.isBlank() ? "" : " / " + l.facilitySummary()); req.sourceRoomId = l.roomId; req.sourceLedgerId = l.id; }
        return req;
    }

    static Point spawnPointForReplacement(World w, PersonnelReplacementRequest req, Random r){
        if(w == null) return null;
        ensureLedgers(w, r);
        int rid = req == null ? -1 : req.sourceRoomId;
        if(rid >= 0 && rid < w.rooms.size()){
            Point p = w.randomOpenPointInRoom(w.rooms.get(rid));
            if(p != null) return p;
        }
        RoomPopulationLedger l = ledgerById(w, req == null ? null : req.sourceLedgerId);
        if(l != null && l.roomId >= 0 && l.roomId < w.rooms.size()){
            Point p = w.randomOpenPointInRoom(w.rooms.get(l.roomId));
            if(p != null) return p;
        }
        return w.randomOpenPoint(r==null?new Random():r);
    }

    static void consumeReplacementSlot(World w, PersonnelReplacementRequest req, Random r){
        if(w == null || req == null) return;
        ensureLedgers(w, r);
        RoomPopulationLedger l = ledgerById(w, req.sourceLedgerId);
        if(l == null) l = drawLedgerFor(w, req.sourceRoomId, req.faction, r, true);
        if(l != null){ l.available = Math.max(0, l.available - 1); l.assigned++; }
    }

    static void recordDeath(World w, NpcEntity dead){
        if(w == null || dead == null || dead.provenance == null) return;
        RoomPopulationLedger l = ledgerById(w, dead.provenance.originSiteId);
        if(l != null) l.dead++;
    }

    static int pruneReplacementQueueToCapacity(World w){
        if(w == null || w.replacementQueue == null || w.replacementQueue.isEmpty()) return 0;
        ensureLedgers(w, new Random(w.seed));
        HashMap<Faction,Integer> demand = new HashMap<>();
        ArrayList<PersonnelReplacementRequest> kept = new ArrayList<>();
        int pruned = 0;
        for(PersonnelReplacementRequest req : w.replacementQueue){
            if(req == null || req.faction == null || req.faction == Faction.NONE){ pruned++; continue; }
            int roomCap = replacementCapacityForFaction(w, req.faction);
            int current = countLivingFactionActors(w, req.faction);
            int allowedQueue = Math.max(0, roomCap - current);
            int used = demand.getOrDefault(req.faction, 0);
            RoomPopulationLedger linked = ledgerById(w, req.sourceLedgerId);
            boolean linkedStillValid = linked != null && linked.capacity > 0 && linked.available > 0 && (linked.faction == req.faction || linked.faction == Faction.NONE || req.faction == Faction.NONE);
            if(used < allowedQueue && (linkedStillValid || allowedQueue > 0)){
                kept.add(req);
                demand.put(req.faction, used + 1);
            } else pruned++;
        }
        if(pruned > 0){
            w.replacementQueue.clear();
            w.replacementQueue.addAll(kept);
        }
        return pruned;
    }

    static int replacementCapacityForFaction(World w, Faction faction){
        if(w == null || faction == null) return 0;
        int cap = 0;
        ensureLedgers(w, new Random(w.seed));
        for(RoomPopulationLedger l: w.roomPopulationLedgers){
            if(l == null || l.capacity <= 0) continue;
            if(l.faction == faction || l.faction == Faction.NONE || faction == Faction.NONE) cap += Math.max(0, l.capacity);
        }
        // Continuity floor: a faction reduced to its protected bar/representative should not be destroyed as a concept.
        return Math.max(1, cap);
    }

    static int countLivingFactionActors(World w, Faction faction){
        if(w == null || w.npcs == null || faction == null) return 0;
        int n = 0;
        for(NpcEntity npc: w.npcs) if(npc != null && npc.faction == faction && !npc.isUntargetableAnchor()) n++;
        return n;
    }

    static int facilityLinkedCount(World w){
        if(w == null || w.roomPopulationLedgers == null) return 0;
        int n = 0;
        for(RoomPopulationLedger l: w.roomPopulationLedgers) if(l.facilityId != null && !l.facilityId.isBlank()) n++;
        return n;
    }

    static RoomPopulationLedger ledgerById(World w, String id){
        if(w == null || id == null) return null;
        for(RoomPopulationLedger l: w.roomPopulationLedgers) if(id.equals(l.id)) return l;
        return null;
    }

    static RoomPopulationLedger drawLedgerFor(World w, int roomId, Faction f, Random r, boolean replacement){
        if(w == null || w.roomPopulationLedgers == null || w.roomPopulationLedgers.isEmpty()) return null;
        ArrayList<RoomPopulationLedger> preferred = new ArrayList<>();
        ArrayList<RoomPopulationLedger> factional = new ArrayList<>();
        ArrayList<RoomPopulationLedger> any = new ArrayList<>();
        Faction nf = normalizeFaction(f, w.zoneType);
        for(RoomPopulationLedger l: w.roomPopulationLedgers){
            if(l.available <= 0 && replacement) continue;
            if(roomId >= 0 && l.roomId == roomId) preferred.add(l);
            if(l.faction == nf || nf == Faction.NONE || l.faction == Faction.NONE) factional.add(l);
            any.add(l);
        }
        ArrayList<RoomPopulationLedger> src = !preferred.isEmpty()?preferred:(!factional.isEmpty()?factional:any);
        if(src.isEmpty()) return null;
        return src.get((r==null?0:Math.floorMod(r.nextInt(), src.size())));
    }

    static void attachProvenance(NpcEntity npc, World w, int roomId, RoomPopulationLedger ledger, String assignment, Random r){
        if(npc == null || w == null) return;
        PersonnelProvenanceRecord p = PersonnelProvenanceApi.create(npc.faction, w.zoneType, PersonnelProvenanceApi.roomLabel(w, roomId), w.sectorX, w.sectorY, w.zoneX, w.zoneY, w.floor, w.sewerLayer, r);
        if(ledger != null){
            p.originRoom = ledger.roomName + " #" + ledger.roomId;
            p.originSiteId = ledger.id;
            p.populationPool = ledger.sourceLabel;
            p.originMode = originModeFor(ledger.sourceKind);
            p.upbringing = upbringingFromLedger(ledger, w.zoneType);
            p.arrivalRoute = ledger.sourceKind.contains("rail") ? "documented through " + ledger.sourceLabel : "drawn from " + ledger.sourceLabel;
            String facility = (ledger.facilityId == null || ledger.facilityId.isBlank()) ? "no named facility" : ledger.facilitySummary() + "; history: " + ledger.facilityHistoricNote;
            p.backstory = (p.originMode.contains("rail") ? "Arrived through arcology rail intake" : "Born, raised, trained, or retained by a local arcology population room") + "; source ledger " + ledger.sourceLabel + "; linked facility " + facility + "; assignment: " + assignment + "; origin room: " + p.originRoom + ".";
        }
        npc.provenance = p;
    }

    static int capacityFor(String low, ZoneType z, Faction f, boolean plaza){
        if(plaza) return 6;
        if(low.contains("barracks") || low.contains("billet") || low.contains("dormitory") || low.contains("duty barracks")) return 10;
        if(low.contains("creche") || low.contains("daycare") || low.contains("rookery")) return 8;
        if(low.contains("apartment") || low.contains("household") || low.contains("servant") || low.contains("rest cell") || low.contains("sleeper") || low.contains("sleep")) return 6;
        if(low.contains("rail") || low.contains("platform") || low.contains("ticket")) return 7;
        if(low.contains("cafeteria") || low.contains("mess") || low.contains("canteen") || low.contains("kitchen") || low.contains("galley")) return 4;
        if(low.contains("security") || low.contains("holding") || low.contains("checkpoint") || low.contains("watch") || low.contains("training") || low.contains("drill")) return 4;
        if(z == ZoneType.MUTANT_SEWER_CAMP || z == ZoneType.CULTIST_SEWER_CAMP || z == ZoneType.GANGER_TURF) return 5;
        return 0;
    }
    static String sourceKindFor(String low, ZoneType z, Faction f){
        if(low.contains("rail") || z == ZoneType.NEUTRAL_RAIL_DEPOT || z == ZoneType.TRAIN_SERVICE_YARD) return "rail intake roster";
        if(low.contains("creche") || low.contains("daycare")) return "creche population ledger";
        if(low.contains("barracks") || low.contains("billet") || low.contains("drill") || f == Faction.IMPERIAL_GUARD || f == Faction.CIVIC_WARDENS) return "barracks duty roster";
        if(low.contains("household") || f == Faction.NOBLE || (f != null && f.name().startsWith("NOBLE"))) return "household servant ledger";
        if(f == Faction.MECHANIST_COLLEGIA || (f != null && f.name().startsWith("MECHANIST COLLEGIA"))) return "forge creche ledger";
        if(f == Faction.CULTIST || f == Faction.HERETIC) return "hidden congregation roster";
        if(f != null && f.name().startsWith("GANGER")) return "gang crash-barracks roster";
        if(f == Faction.MUTANT) return "sump brood ledger";
        return "local hab work roster";
    }
    static String labelFor(String kind, Faction f, String roomName, ZoneType z){ return (f==null?"Unaligned":f.label) + " " + kind + " from " + roomName + " in " + (z==null?"unknown zone":z.label); }
    static Faction normalizeFaction(Faction f, ZoneType z){ if(f != null && f != Faction.NONE) return f; if(z == ZoneType.IMPERIAL_NEWS_NETWORK) return Faction.INN; if(z == ZoneType.HAB_STACK || z == ZoneType.NEUTRAL_CIVILIAN_FLOOR) return Faction.HIVER; if(z == ZoneType.NEUTRAL_RAIL_DEPOT || z == ZoneType.TRAIN_SERVICE_YARD) return Faction.CIVIC_LEDGER_OFFICE; return Faction.NONE; }
    static String originModeFor(String kind){ if(kind == null) return "arcology-born"; if(kind.contains("rail")) return "rail-arrival"; if(kind.contains("creche")) return "creche-raised"; if(kind.contains("barracks")) return "barracks-raised"; if(kind.contains("household")) return "household-born"; if(kind.contains("gang")) return "gang-raised"; return "arcology-born"; }
    static String upbringingFromLedger(RoomPopulationLedger l, ZoneType z){ return "managed by " + l.sourceLabel + "; facility " + l.facilitySummary() + "; capacity " + l.capacity + ", available reserve " + l.available + ", assigned " + l.assigned + ", dead " + l.dead + "."; }
}


class PersonnelProvenanceApi {
    private PersonnelProvenanceApi(){}

    static void assignWorldProvenance(NpcEntity npc, World world, int roomId, Random r){
        if(npc == null || world == null) return;
        npc.provenance = create(npc.faction, world.zoneType, roomLabel(world, roomId), world.sectorX, world.sectorY, world.zoneX, world.zoneY, world.floor, world.sewerLayer, r);
    }

    static void assignReplacementProvenance(NpcEntity npc, World world, PersonnelReplacementRequest req, Random r){
        if(npc == null || world == null || req == null) return;
        PersonnelProvenanceRecord p = create(npc.faction, world.zoneType, "replacement intake point", world.sectorX, world.sectorY, world.zoneX, world.zoneY, world.floor, world.sewerLayer, r);
        p.originMode = "replacement-arrival"; p.arrivalRoute = req.source; p.originSiteId = (req.sourceLedgerId==null||req.sourceLedgerId.isBlank()?"replacement-for:" + req.deadNpcId:req.sourceLedgerId); p.originRoom = req.sourceRoomId >= 0 ? PersonnelProvenanceApi.roomLabel(world, req.sourceRoomId) : p.originRoom; p.populationPool = req.source == null ? p.populationPool : req.source;
        RoomPopulationLedger ledger = PersonnelPopulationApi.ledgerById(world, req.sourceLedgerId);
        String facility = ledger == null ? "no named facility linked" : ledger.facilitySummary() + "; history: " + ledger.facilityHistoricNote;
        p.upbringing = ledger == null ? p.upbringing : PersonnelPopulationApi.upbringingFromLedger(ledger, world.zoneType);
        p.backstory = "Arrived from " + req.source + " to replace " + req.deadName + " after " + req.reason + "; replacement source room=" + req.sourceRoomId + "; linked facility=" + facility + "; current pool=" + p.populationPool + ".";
        npc.provenance = p;
    }

    static PersonnelProvenanceRecord create(Faction faction, ZoneType zone, String room, int sx, int sy, int zx, int zy, int floor, boolean sewer, Random r){
        PersonnelProvenanceRecord p = new PersonnelProvenanceRecord();
        String coord = "sector " + sx + "," + sy + " zone " + zx + "," + zy + " floor " + floor + (sewer?"B":"");
        p.originZone = zone == null ? coord : zone.label + " / " + coord;
        p.originRoom = room == null ? "unrecorded room" : room;
        p.originMode = modeFor(faction, zone, r);
        p.populationPool = poolFor(faction, zone);
        p.originSiteId = siteFor(faction, zone, coord);
        p.upbringing = upbringingFor(faction, zone, p.originMode);
        p.arrivalRoute = p.originMode.contains("rail") ? "rail hub intake manifest" : "local barracks/creche roster";
        p.backstory = backstoryFor(faction, zone, p);
        return p;
    }

    static PersonnelReplacementRequest recordDeathAndScheduleReplacement(World world, NpcEntity dead, int turn, Random r, String reason){
        if(world == null || dead == null) return null;
        PersonnelReplacementRequest req = new PersonnelReplacementRequest();
        req.deadNpcId = dead.id; req.deadName = dead.name; req.faction = dead.faction; req.x = dead.x; req.y = dead.y; req.reason = reason == null ? "unrecorded death" : reason;
        req.source = replacementSource(dead.faction, world.zoneType, r);
        PersonnelPopulationApi.recordDeath(world, dead);
        PersonnelPopulationApi.fillReplacementRequest(world, req, r);
        req.dueTurn = turn + replacementDelay(dead.faction, world.zoneType, r);
        world.replacementQueue.add(req);
        DebugLog.audit("PERSONNEL_DEATH_SCHEDULE", "dead=" + dead.id + " name=" + dead.name + " faction=" + dead.faction.label + " source=" + req.source + " due=" + req.dueTurn + " reason=" + req.reason + " provenance=" + dead.originSummary());
        return req;
    }

    static String roomLabel(World w, int roomId){
        if(w == null || roomId < 0 || roomId >= w.rooms.size()) return "unassigned corridor";
        String profile = roomId < w.roomProfiles.size() && w.roomProfiles.get(roomId) != null ? w.roomProfiles.get(roomId).name : "room " + roomId;
        return profile + " #" + roomId;
    }
    static String modeFor(Faction f, ZoneType z, Random r){
        int roll = r == null ? 0 : r.nextInt(100);
        if(z == ZoneType.NEUTRAL_RAIL_DEPOT || z == ZoneType.TRAIN_SERVICE_YARD || roll < 22) return "rail-arrival";
        if(f == Faction.IMPERIAL_GUARD || f == Faction.CIVIC_WARDENS) return roll < 45 ? "rail-mustered" : "barracks-raised";
        if(f == Faction.MECHANIST_COLLEGIA || (f != null && f.name().startsWith("MECHANIST COLLEGIA"))) return roll < 35 ? "rail-transferred" : "creche-raised";
        if(f == Faction.NOBLE || (f != null && f.name().startsWith("NOBLE"))) return "household-born";
        return roll < 70 ? "arcology-born" : "rail-arrival";
    }
    static String poolFor(Faction f, ZoneType z){
        if(f == Faction.INN) return "Concord News Network newsroom roster";
        if(f == Faction.IMPERIAL_GUARD) return "billet barracks replacement pool";
        if(f == Faction.CIVIC_WARDENS) return "precinct duty roster";
        if(f == Faction.MECHANIST_COLLEGIA || (f != null && f.name().startsWith("MECHANIST COLLEGIA"))) return "forge creche and lay-bound Labor Automaton pool";
        if(f == Faction.MUTANT) return "sump brood hollow";
        if(f == Faction.CULTIST || f == Faction.HERETIC) return "hidden congregation cell";
        if(f != null && f.name().startsWith("GANGER")) return "gang crash-barracks roster";
        if(f == Faction.NOBLE || (f != null && f.name().startsWith("NOBLE"))) return "household servant and retainers ledger";
        if(z == ZoneType.NEUTRAL_RAIL_DEPOT || z == ZoneType.TRAIN_SERVICE_YARD) return "rail hub intake pool";
        return "local hab creche and work roster";
    }
    static String siteFor(Faction f, ZoneType z, String coord){ return (f==null?"NONE":f.name()) + ":" + (z==null?"UNKNOWN":z.name()) + ":" + coord; }
    static String upbringingFor(Faction f, ZoneType z, String mode){
        if(mode != null && mode.contains("rail")) return "arrived through a rail hub from elsewhere in the arcology and was absorbed into the local roster";
        if(f == Faction.IMPERIAL_GUARD) return "raised or processed through barracks discipline, mess lines, drill rooms, and quartermaster issue";
        if(f == Faction.CIVIC_WARDENS) return "processed through precinct dormitories, duty halls, holding blocks, and procedural obedience";
        if(f == Faction.MECHANIST_COLLEGIA || (f != null && f.name().startsWith("MECHANIST COLLEGIA"))) return "grown through a forge creche among nutrient galleys, machine chapels, and anti-human maintenance spaces";
        if(f == Faction.NOBLE || (f != null && f.name().startsWith("NOBLE"))) return "kept within household service corridors, rich rooms, and hereditary obligation";
        return "born into local arcology rooms, creches, kitchens, queues, and corridor work";
    }
    static String backstoryFor(Faction f, ZoneType z, PersonnelProvenanceRecord p){ return (p.originMode.contains("rail") ? "Arrived from the rail hub" : "Born or raised inside the arcology") + "; assigned to " + p.populationPool + "; upbringing: " + p.upbringing + "; current origin record: " + p.originRoom + " in " + p.originZone + "."; }
    static String replacementSource(Faction f, ZoneType z, Random r){
        String pool = poolFor(f,z);
        if(r != null && r.nextInt(100) < 25) return "rail hub intake pool";
        return pool;
    }
    static int replacementDelay(Faction f, ZoneType z, Random r){ int base = 36 + (r==null?0:r.nextInt(49)); if(f==Faction.MUTANT || f==Faction.CULTIST || f==Faction.HERETIC) base = 18 + (r==null?0:r.nextInt(36)); return base; }
}


class NpcEntity {
    String id, name, state, role;
    int numericId;
    Point needTarget;
    String needTargetKind = "none";
    Faction faction;
    char symbol;
    int x, y, homeX, homeY, portraitIndex;
    int idleBias;
    int hp = 12;
    boolean hostileBarked = false;
    int suspiciousBarkCooldown = 0;
    int crowdShuffleCooldown = 0;
    int selectedMovementModeIndex = GamePanel.MOTION_WALK;
    int activeMotionStateIndex = GamePanel.MOTION_STATIONARY;
    int intellect = 5;
    int equipmentTier = 0;
    String equippedMeleeWeapon = "";
    String equippedRangedWeapon = "";
    String equippedArmor = "";
    String equippedExplosive = "";
    String creatureKind = "humanoid";
    String animalProfileId = "";
    String companionOf = "";
    int loadedShots = 0;
    int ammoReloadsRemaining = 0;
    int factionRank = 8;
    int ageYears = 0;
    long birthWorldTurn = 0L;
    String ageBand = "adult";
    String nameLockedProfileKey = "";
    String factionRankTitle = "";
    String factionRankScope = "";
    int hunger, thirst, sleepDebt, routineOffset;
    int lastTickTurn = -1;
    PersonnelProvenanceRecord provenance;

    static NpcEntity create(Faction f, ZoneType z, int x, int y, Random r) {
        NpcEntity n = new NpcEntity();
        n.faction = f == null ? Faction.NONE : f;
        n.x = x; n.y = y; n.homeX = x; n.homeY = y;
        n.symbol = symbolFor(n.faction, z);
        n.name = nameFor(n.faction, r);
        n.state = defaultState(n.faction, r);
        n.role = roleFor(n.faction, z, r);
        AgeAndWorldTimeAuthority.initializeNpcAge(n, z, r);
        n.ensureRankIdentity(r);
        n.portraitIndex = r.nextInt(256);
        n.idleBias = 2 + r.nextInt(5);
        n.hunger = r.nextInt(4);
        n.thirst = r.nextInt(4);
        n.sleepDebt = r.nextInt(6);
        n.routineOffset = r.nextInt(24);
        n.numericId = Math.abs(Objects.hash(n.faction.name(), n.name, x, y, n.portraitIndex, r.nextInt()));
        n.id = n.faction.name() + "-" + n.numericId;
        n.provenance = PersonnelProvenanceApi.create(n.faction, z, "unassigned spawn point", 0,0,0,0,0,false, r);
        n.ensureCombatLoadout(r);
        return n;
    }

    static NpcEntity factionRepresentative(Faction f, int px, int py, Random r) {
        NpcEntity n = create(f == null ? Faction.NONE : f, ZoneType.NEUTRAL_CIVILIAN_FLOOR, px, py, r == null ? new Random() : r);
        n.role = "Faction Representative";
        n.state = "Contract Desk";
        n.name = (f == null ? "Faction" : f.label) + " Representative";
        n.factionRank = 5;
        n.factionRankTitle = FactionRosterAuthority.titleForRank(n.faction, n.factionRank);
        n.factionRankScope = FactionRosterAuthority.scopeForRank(n.factionRank);
        n.hp = 9999;
        n.symbol = 'R';
        n.id = "REP-" + (f == null ? "NONE" : f.name());
        return n;
    }

    static NpcEntity headClericFor(ZoneType z, int x, int y, Random r) {
        NpcEntity n = create(Faction.MINISTORUM, z, x, y, r == null ? new Random() : r);
        n.role = "Head Cleric";
        n.state = "Sanctuary";
        n.symbol = 'C';
        n.name = "Head Cleric " + nameFor(Faction.MINISTORUM, r == null ? new Random() : r).replace("Father ", "").replace("Sister ", "");
        n.factionRank = 1;
        n.factionRankTitle = "Head Cleric";
        n.factionRankScope = "immortal sanctuary authority for this local temple";
        n.hp = 9999;
        n.id = "MINISTORUM-HEAD-CLERIC-" + Math.abs(Objects.hash(z == null ? "zone" : z.label, x, y, n.name));
        return n;
    }

    static NpcEntity ministorumPriestOrPilgrim(ZoneType z, int x, int y, Random r) {
        NpcEntity n = create(Faction.MINISTORUM, z, x, y, r == null ? new Random() : r);
        n.role = (r != null && r.nextBoolean()) ? "Priest" : "Pilgrim";
        n.state = n.role.equals("Priest") ? "Prayer" : "Pilgrim Service";
        n.symbol = n.role.equals("Priest") ? 'c' : 'p';
        return n;
    }

    void ensureRankIdentity(Random r) {
        if (isMinorActor()) {
            factionRank = 8;
            factionRankTitle = "Faction member";
            factionRankScope = "child/youth member; no formal rank or command authority";
            return;
        }
        if (r == null) r = new Random(numericId == 0 ? Objects.hash(name, faction) : numericId);
        FactionRankEntry inferred = FactionRosterAuthority.inferRankForName(faction, name);
        if ((factionRank < 1 || factionRank > 8) && inferred != null) factionRank = inferred.rank;
        if (factionRank < 1 || factionRank > 8) {
            FactionRankEntry rolled = FactionRosterAuthority.get(faction).rankFor(r);
            factionRank = rolled.rank;
        }
        if (factionRankTitle == null || factionRankTitle.isBlank()) factionRankTitle = inferred != null && inferred.rank == factionRank ? inferred.title : FactionRosterAuthority.titleForRank(faction, factionRank);
        if (factionRankScope == null || factionRankScope.isBlank()) factionRankScope = FactionRosterAuthority.scopeForRank(factionRank);
    }

    String rankLine() {
        ensureRankIdentity(new Random(numericId == 0 ? Objects.hash(name, faction) : numericId));
        return "Rank " + factionRank + " — " + factionRankTitle + " / " + factionRankScope;
    }

    boolean isAnimalActor() { return creatureKind != null && (creatureKind.equals("wild-animal") || creatureKind.equals("farm-animal") || creatureKind.equals("kennel-animal") || creatureKind.equals("pet")); }
    boolean isMinorActor() {
        if (isAnimalActor()) return false;
        if (ageYears > 0) return AgeAndWorldTimeAuthority.isMinorAge(ageYears);
        String r = ((role == null ? "" : role) + " " + (name == null ? "" : name) + " " + (provenance == null ? "" : provenance.populationPool)).toLowerCase(Locale.ROOT);
        return r.contains("child") || r.contains("schola") || r.contains("youth") || r.contains("teen");
    }
    boolean isChildActor() { return isMinorActor(); }
    boolean isYoungAdultActor() { return !isAnimalActor() && AgeAndWorldTimeAuthority.isYoungAdultAge(ageYears); }
    String ageLine() { return isAnimalActor() ? "non-human life-stage" : (ageYears + " years / " + ageBand); }
    boolean isPetActor() { return creatureKind != null && creatureKind.equals("pet"); }
    boolean isServantActor() { return role != null && role.toLowerCase(Locale.ROOT).contains("servant"); }
    String animalLine() {
        String kind = creatureKind == null || creatureKind.isBlank() ? "animal" : creatureKind.replace('-', ' ');
        String owner = companionOf == null || companionOf.isBlank() ? "no direct handler" : "companion of " + companionOf;
        String behavior = state == null ? "unrecorded" : state;
        return name + " / " + kind + " / " + behavior + " / " + owner;
    }

    void ensureCombatLoadout(Random r) {
        if (isAnimalActor()) { equippedMeleeWeapon=""; equippedRangedWeapon=""; equippedArmor=""; equippedExplosive=""; loadedShots=0; ammoReloadsRemaining=0; return; }
        if (r == null) r = new Random(numericId == 0 ? 1 : numericId);
        ensureRankIdentity(r);
        if (intellect <= 0) intellect = 3 + Math.floorMod(Objects.hash(id, name, faction), 8);
        if (equippedMeleeWeapon == null) equippedMeleeWeapon = "";
        if (equippedRangedWeapon == null) equippedRangedWeapon = "";
        if (equippedArmor == null) equippedArmor = "";
        if (equippedExplosive == null) equippedExplosive = "";
        if (equippedMeleeWeapon.isBlank()) equippedMeleeWeapon = chooseMeleeWeapon(r);
        if (equippedRangedWeapon.isBlank()) equippedRangedWeapon = chooseRangedWeapon(r);
        if (equippedArmor.isBlank()) equippedArmor = chooseArmor(r);
        if (equippedExplosive.isBlank()) equippedExplosive = chooseExplosive(r);
        equipmentTier = Math.max(equipmentTier, estimateEquipmentTier(equippedMeleeWeapon, equippedRangedWeapon, equippedArmor));
        if (ItemCatalog.isFirearmLike(equippedRangedWeapon)) {
            int cap = magazineCapacityLocal(equippedRangedWeapon);
            if (loadedShots <= 0) loadedShots = cap;
            if (ammoReloadsRemaining <= 0) ammoReloadsRemaining = reserveReloadsForFaction();
        }
    }

    int reserveReloadsForFaction() {
        boolean armedProfessional = faction == Faction.IMPERIAL_GUARD || faction == Faction.CIVIC_WARDENS || faction == Faction.SORORITAS || faction == Faction.NOBLE || (faction != null && faction.name().startsWith("GANGER")) || faction == Faction.BANDIT;
        int base = armedProfessional ? 2 : 1;
        if (intellect >= 8) base++;
        if (faction == Faction.IMPERIAL_GUARD || faction == Faction.SORORITAS) base++;
        return Math.max(0, Math.min(5, base));
    }

    int estimateEquipmentTier(String melee, String ranged, String armor) {
        int best = 0;
        best = Math.max(best, ItemCatalog.priceFor(melee) / 25);
        best = Math.max(best, ItemCatalog.priceFor(ranged) / 35);
        best = Math.max(best, ItemCatalog.priceFor(armor) / 24);
        return Math.max(0, Math.min(6, best));
    }

    String chooseMeleeWeapon(Random r) {
        String[] pool;
        if (faction == Faction.SORORITAS) pool = new String[]{"Chainsword","Power Sword","Power Maul","Knife"};
        else if (faction == Faction.CIVIC_WARDENS) pool = new String[]{"Civic Wardens shock maul","Power Maul","Knife"};
        else if (faction == Faction.IMPERIAL_GUARD) pool = new String[]{"Guard trench knife","Munitorum Shovel","Chainsword","Knife"};
        else if (faction == Faction.MECHANIST_COLLEGIA || (faction != null && faction.name().startsWith("MECHANIST COLLEGIA"))) pool = new String[]{"Mechanist Collegia arc prod","Ritual wrench","Omnissian Axe","Broken servo-arm club"};
        else if (faction == Faction.MUTANT) pool = new String[]{"Mutant scrap axe","Mutant tusk club","Mutant bone maul","Trash-hook spear"};
        else if (faction == Faction.CULTIST || faction == Faction.HERETIC) pool = new String[]{"Cult ritual blade","Heretic nail flail","Toxic Knife","Knife"};
        else if (faction == Faction.NOBLE || (faction != null && faction.name().startsWith("NOBLE"))) pool = new String[]{"Duelling Sword","Power Sword","Monoblade sliver","Sword"};
        else if (faction == Faction.BANDIT || (faction != null && faction.name().startsWith("GANGER"))) pool = new String[]{"Ganger chain cleaver","Ganger buzz-cleaver","Chain-wrapped club","Sump hook blade","Knife"};
        else pool = new String[]{"Knife","Cargo hook","Rebar maul","Industrial Hammer"};
        return chooseByIntelligence(pool, r, false);
    }

    String chooseRangedWeapon(Random r) {
        String[] pool;
        if (faction == Faction.SORORITAS) pool = new String[]{"Mass-Reactive Carbine","Bolt Pistol","Flamer","Hand Flamer","Laspistol"};
        else if (faction == Faction.CIVIC_WARDENS) pool = new String[]{"Shotgun","Webber","Laspistol","Stub Revolver","Bolt Pistol"};
        else if (faction == Faction.IMPERIAL_GUARD) pool = new String[]{"Light Rifle","Guard lascarbine","Laspistol","Autogun","Longlas","Shotgun"};
        else if (faction == Faction.MECHANIST_COLLEGIA || (faction != null && faction.name().startsWith("MECHANIST COLLEGIA"))) pool = new String[]{"Arc Rifle","Laspistol","Jury-rigged laslock","Plasma Pistol","Plasma Gun"};
        else if (faction == Faction.MUTANT) pool = new String[]{"Chem sprayer","Acid Spitter","Pipe shotgun","Zip pistol"};
        else if (faction == Faction.CULTIST || faction == Faction.HERETIC) pool = new String[]{"Cult martyr pistol","Stub pistol","Autopistol","Hand Flamer"};
        else if (faction == Faction.NOBLE || (faction != null && faction.name().startsWith("NOBLE"))) pool = new String[]{"Noble dueling pistol","Needle Pistol","Plasma Pistol","Stub Revolver"};
        else if (faction == Faction.BANDIT || (faction != null && faction.name().startsWith("GANGER"))) pool = new String[]{"Autopistol","Stub pistol","Sawed-off stub shotgun","Pipe shotgun","Autogun","Scrap autogun"};
        else pool = new String[]{"Zip pistol","Stub pistol","Hunting Rifle"};
        return chooseByIntelligence(pool, r, true);
    }

    String chooseArmor(Random r) {
        String[] pool;
        if (faction == Faction.SORORITAS) pool = new String[]{"Guard flak vest","Civic Wardens riot visor","Battered helmet"};
        else if (faction == Faction.CIVIC_WARDENS) pool = new String[]{"Civic Wardens riot visor","Guard flak vest","Baton-dented vest"};
        else if (faction == Faction.IMPERIAL_GUARD) pool = new String[]{"Guard flak vest","Battered helmet","Rail worker hazard coat"};
        else if (faction == Faction.MECHANIST_COLLEGIA || (faction != null && faction.name().startsWith("MECHANIST COLLEGIA"))) pool = new String[]{"Mechanist Collegia rubberized apron","Battered helmet","Work gloves"};
        else if (faction == Faction.NOBLE || (faction != null && faction.name().startsWith("NOBLE"))) pool = new String[]{"Noble fur-lined coat","Guard flak vest","Battered helmet"};
        else if (faction == Faction.BANDIT || (faction != null && faction.name().startsWith("GANGER"))) pool = new String[]{"Damaged ganger coat","Battered helmet","Padded coat"};
        else pool = new String[]{"Padded coat","Battered helmet","Plain civilian coat"};
        return chooseByIntelligence(pool, r, false);
    }

    String chooseExplosive(Random r) {
        String[] pool;
        if (faction == Faction.IMPERIAL_GUARD) pool = new String[]{"Frag grenade","Krak grenade","Smoke grenade"};
        else if (faction == Faction.CIVIC_WARDENS) pool = new String[]{"Smoke grenade","Frag grenade","Web cartridge"};
        else if (faction == Faction.SORORITAS) pool = new String[]{"Frag grenade","Melta grenade"};
        else if (faction == Faction.MECHANIST_COLLEGIA || (faction != null && faction.name().startsWith("MECHANIST COLLEGIA"))) pool = new String[]{"Krak grenade","Melta grenade","Plasma bomb"};
        else if (faction == Faction.BANDIT || (faction != null && faction.name().startsWith("GANGER"))) pool = new String[]{"Tripwire mine","Frag grenade","Satchel charge"};
        else if (faction == Faction.CULTIST || faction == Faction.HERETIC) pool = new String[]{"Satchel charge","Plasma bomb","Frag grenade"};
        else if (faction == Faction.NOBLE || (faction != null && faction.name().startsWith("NOBLE"))) pool = new String[]{"Motion claymore","Smoke grenade","Melta grenade"};
        else pool = new String[]{"Frag grenade"};
        if (intellect < 5 && r.nextInt(100) < 65) return "";
        return chooseByIntelligence(pool, r, false);
    }

    String chooseByIntelligence(String[] pool, Random r, boolean ranged) {
        if (pool == null || pool.length == 0) return "";
        if (intellect <= 5) return pool[Math.floorMod(r.nextInt(), pool.length)];
        String best = pool[0]; int bestScore = -9999;
        for (String item : pool) {
            if (ItemCatalog.get(item) == null) continue;
            int score = ItemCatalog.priceFor(item);
            if (ranged) score += magazineCapacityLocal(item) * 2 + weaponRangeLocal(item) * 3;
            if (intellect < 8) score += r.nextInt(20) - 10;
            if (score > bestScore) { bestScore = score; best = item; }
        }
        return best;
    }

    int weaponRangeLocal(String weapon) {
        if (weapon == null || !ItemCatalog.isFirearmLike(weapon)) return 1;
        String low = ItemQuality.stripManufacturingIdentity(weapon).toLowerCase(Locale.ROOT);
        if (low.contains("shotgun")) return 5;
        if (low.contains("flamer") || low.contains("melta")) return 4;
        if (low.contains("pistol") || low.contains("revolver")) return 7;
        if (low.contains("stubber") || low.contains("carbine") || low.contains("webber")) return 9;
        if (low.contains("rifle") || low.contains("las") || low.contains("mass-Reactive Carbine")) return 12;
        if (low.contains("autocannon") || low.contains("long")) return 14;
        return 8;
    }

    int magazineCapacityLocal(String weapon) {
        String w = ItemQuality.stripManufacturingIdentity(weapon == null ? "" : weapon).toLowerCase(Locale.ROOT);
        if (!ItemCatalog.isFirearmLike(weapon)) return 0;
        if (w.contains("zip pistol") || w.contains("laslock") || w.contains("martyr pistol")) return 1;
        if (w.contains("melta") || w.contains("inferno")) return 2;
        if (w.contains("flamer") || w.contains("chem sprayer") || w.contains("acid spitter")) return 4;
        if (w.contains("plasma")) return w.contains("heavy") ? 6 : 4;
        if (w.contains("shotgun")) return 5;
        if (w.contains("revolver") || w.contains("dueling")) return 6;
        if (w.contains("pistol")) return 8;
        if (w.contains("bolt")) return w.contains("heavy") || w.contains("storm") ? 16 : 8;
        if (w.contains("autocannon")) return 6;
        if (w.contains("heavy stubber")) return 20;
        if (w.contains("autogun") || w.contains("auto")) return 20;
        if (w.contains("las")) return 12;
        if (w.contains("rifle") || w.contains("carbine") || w.contains("stubber")) return 10;
        return 8;
    }

    String equipmentSummary() {
        ensureCombatLoadout(new Random(numericId == 0 ? 1 : numericId));
        String ranged = equippedRangedWeapon == null || equippedRangedWeapon.isBlank() ? "none" : equippedRangedWeapon + " " + loadedShots + "/" + magazineCapacityLocal(equippedRangedWeapon) + " loaded, reserve reloads " + ammoReloadsRemaining;
        String explosive = equippedExplosive == null || equippedExplosive.isBlank() ? "none" : equippedExplosive;
        return "melee=" + equippedMeleeWeapon + "; ranged=" + ranged + "; armor=" + equippedArmor + "; explosive=" + explosive + "; intellect=" + intellect + "; tier=" + equipmentTier;
    }

    static NpcEntity sororitasGuardFor(ZoneType z, int x, int y, Random r) {
        NpcEntity n = create(Faction.SORORITAS, z, x, y, r == null ? new Random() : r);
        n.role = "Sister of Battle Guard";
        n.state = "Guard";
        n.symbol = 'S';
        n.hp = Math.max(n.hp, 24);
        return n;
    }

    void tick(World w, int px, int py, int turn) {
        if (turn == lastTickTurn) return;
        lastTickTurn = turn;
        if (crowdShuffleCooldown > 0) crowdShuffleCooldown--;
        activeMotionStateIndex = GamePanel.MOTION_STATIONARY;
        selectedMovementModeIndex = chooseMovementModeIndex(turn);
        if (isAnimalActor()) { tickAnimal(w, px, py, turn); return; }
        updateNeeds(turn);
        updateRoutine(turn);
        if (turn % idleBias != 0) return;
        if (state.equals("Sleep")) { sleepDebt = Math.max(0, sleepDebt - 1); return; }
        if (state.equals("Search Needs")) { seekNeedProvider(w, turn, px, py); return; }
        if (state.equals("Guard") && Math.abs(x-homeX)+Math.abs(y-homeY) > 2) { stepToward(w, homeX, homeY, px, py); return; }
        if ("Hostile".equalsIgnoreCase(state) && Math.abs(x-px)+Math.abs(y-py) <= 8) { stepToward(w, px, py, px, py); return; }
        if (state.equals("Patrol") || state.equals("Wander") || state.equals("Idle") || state.equals("Inspect")) wander(w, turn, px, py);
    }

    void tickAnimal(World w, int px, int py, int turn) {
        if (w == null) return;
        if (isPetActor() && companionOf != null && !companionOf.isBlank()) {
            NpcEntity owner = w.npcById(companionOf);
            if (owner != null) {
                int d = Math.abs(x-owner.x)+Math.abs(y-owner.y);
                if (d > 1) { stepToward(w, owner.x, owner.y, px, py); return; }
                if (turn % Math.max(2, idleBias) == 0) wanderNear(w, owner.x, owner.y, 2, px, py);
                return;
            }
        }
        if ("Hostile".equalsIgnoreCase(state) && Math.abs(x-px)+Math.abs(y-py) <= 8) { stepToward(w, px, py, px, py); return; }
        if (turn % Math.max(2, idleBias) == 0) wander(w, turn, px, py);
    }

    void wanderNear(World w, int cx, int cy, int radius, int blockX, int blockY) {
        if (w == null) return;
        int[][] dirs={{1,0},{-1,0},{0,1},{0,-1},{0,0}};
        int[] d=dirs[Math.floorMod(Objects.hash(id, w.seed, cx, cy, System.nanoTime()), dirs.length)];
        int nx=x+d[0], ny=y+d[1];
        if(Math.abs(nx-cx)+Math.abs(ny-cy) <= Math.max(1,radius) && canNpcStep(w,nx,ny,blockX,blockY)) moveTo(nx,ny);
    }

    void updateNeeds(int turn) {
        if (turn > 0 && turn % 18 == 0) sleepDebt = Math.min(30, sleepDebt + 1);
        if (turn > 0 && turn % 24 == 0) hunger = Math.min(20, hunger + 1);
        if (turn > 0 && turn % 18 == 0) thirst = Math.min(20, thirst + 1);
        if (hunger > 14 || thirst > 14) state = "Search Needs";
    }

    void updateRoutine(int turn) {
        if (isProtectedCleric()) { state = "Sanctuary"; return; }
        if ("Hostile".equalsIgnoreCase(state) || state.equals("Search Needs")) return;
        int hour = ((turn / 6) + routineOffset) % 24;
        if (sleepDebt > 18 || (hour >= 1 && hour <= 5 && !isTrader())) { state = "Sleep"; return; }
        if (isTrader() && hour >= 7 && hour <= 20) { state = "Trade"; return; }
        if (faction == Faction.MINISTORUM) { state = (hour % 5 == 0) ? "Prayer" : "Pilgrim Service"; return; }
        if (faction == Faction.SORORITAS) { state = "Guard"; return; }
        if (faction == Faction.CIVIC_WARDENS || faction == Faction.IMPERIAL_GUARD) { state = (hour % 3 == 0) ? "Guard" : "Patrol"; return; }
        if (faction == Faction.MECHANIST_COLLEGIA || faction.name().startsWith("MECHANIST COLLEGIA")) { state = (hour % 4 == 0) ? "Inspect" : "Patrol"; return; }
        if (faction == Faction.MUTANT || faction == Faction.CULTIST || faction == Faction.HERETIC) { state = "Hostile"; return; }
        state = (hour >= 8 && hour <= 18) ? "Wander" : "Idle";
    }

    int chooseMovementModeIndex(int turn) {
        if (isAnimalActor()) return "Hostile".equalsIgnoreCase(state) ? GamePanel.MOTION_RUN : (isPetActor() ? GamePanel.MOTION_WALK : GamePanel.MOTION_SNEAK);
        if ("Sleep".equalsIgnoreCase(state) || "Trade".equalsIgnoreCase(state) || "Prayer".equalsIgnoreCase(state) || "Pilgrim Service".equalsIgnoreCase(state) || "Contract Desk".equalsIgnoreCase(state) || "Sanctuary".equalsIgnoreCase(state)) return GamePanel.MOTION_WALK;
        if ("Hostile".equalsIgnoreCase(state)) return (faction == Faction.ROGUE_MACHINE || faction == Faction.MUTANT) ? GamePanel.MOTION_SPRINT : GamePanel.MOTION_RUN;
        if (faction == Faction.CULTIST || faction == Faction.HERETIC || faction == Faction.MUTANT) return Math.floorMod(Objects.hash(id, turn/4), 3) == 0 ? GamePanel.MOTION_SNEAK : GamePanel.MOTION_WALK;
        if ("Guard".equalsIgnoreCase(state) || "Patrol".equalsIgnoreCase(state)) return Math.floorMod(Objects.hash(id, turn/5), 4) == 0 ? GamePanel.MOTION_RUN : GamePanel.MOTION_WALK;
        int roll = Math.floorMod(Objects.hash(id, turn/7), 12);
        if (roll == 0) return GamePanel.MOTION_SNEAK;
        if (roll >= 10) return GamePanel.MOTION_RUN;
        return GamePanel.MOTION_WALK;
    }

    String activeMotionLabel() {
        switch(activeMotionStateIndex) {
            case GamePanel.MOTION_SNEAK: return "SNEAK";
            case GamePanel.MOTION_WALK: return "WALK";
            case GamePanel.MOTION_RUN: return "RUN";
            case GamePanel.MOTION_SPRINT: return "SPRINT";
            default: return "STATIONARY";
        }
    }

    void moveTo(int nx, int ny) {
        if (nx != x || ny != y) activeMotionStateIndex = selectedMovementModeIndex;
        x = nx; y = ny;
    }

    String needLine() {
        if (state.equals("Sleep")) return "I need sleep more than sermon or coin. Debt: " + sleepDebt + ".";
        if (hunger > 14 || thirst > 14) return "Food and water are not ideas down here. Hunger " + hunger + ", thirst " + thirst + ".";
        if (sleepDebt > 12) return "I am upright by habit, not by mercy. Sleep debt " + sleepDebt + ".";
        return "Still breathing. Hunger " + hunger + ", thirst " + thirst + ", sleep debt " + sleepDebt + ".";
    }

    void seekNeedProvider(World w, int turn, int blockX, int blockY) {
        if (w == null) { return; }
        if (needTarget == null || !w.inBounds(needTarget.x, needTarget.y) || turn % 12 == 0) {
            needTarget = w.findNeedProvider(x, y, hunger >= thirst && hunger > 10 ? "food" : (thirst > 10 ? "water" : "sleep"));
            needTargetKind = needTarget == null ? "none" : (hunger > 10 ? "food" : (thirst > 10 ? "water" : "sleep"));
        }
        if (needTarget != null) {
            int d = Math.abs(x-needTarget.x)+Math.abs(y-needTarget.y);
            if (d <= 1) {
                if (hunger > 0) hunger = Math.max(0, hunger - 4);
                if (thirst > 0) thirst = Math.max(0, thirst - 4);
                if (sleepDebt > 0 && needTargetKind.equals("sleep")) sleepDebt = Math.max(0, sleepDebt - 5);
                if (hunger < 10 && thirst < 10 && sleepDebt < 15) { state = "Idle"; needTarget = null; needTargetKind = "none"; }
                return;
            }
            stepToward(w, needTarget.x, needTarget.y, blockX, blockY);
        } else wander(w, turn, blockX, blockY);
    }

    void wander(World w, int turn, int blockX, int blockY) {
        int[][] dirs={{1,0},{-1,0},{0,1},{0,-1},{0,0}};
        int[] d = dirs[Math.abs((id.hashCode()+turn*31)) % dirs.length];
        int nx=x+d[0], ny=y+d[1];
        if (canNpcStep(w,nx,ny,blockX,blockY)) { moveTo(nx, ny); }
        else if (d[0] != 0 || d[1] != 0) tryNpcPushPastCrowd(w, nx, ny, blockX, blockY);
    }

    void stepToward(World w, int tx, int ty, int blockX, int blockY) {
        int dx = Integer.compare(tx, x), dy = Integer.compare(ty, y);
        if (Math.abs(tx-x) > Math.abs(ty-y)) {
            if (canNpcStep(w,x+dx,y,blockX,blockY)) { moveTo(x+dx, y); return; }
            if (x+dx == blockX && y == blockY && tryNpcShuffleAroundBlockedActor(w, dx, 0, blockX, blockY)) return;
            if (tryNpcPushPastCrowd(w, x+dx, y, blockX, blockY)) return;
        }
        if (canNpcStep(w,x,y+dy,blockX,blockY)) { moveTo(x, y+dy); return; }
        if (x == blockX && y+dy == blockY && tryNpcShuffleAroundBlockedActor(w, 0, dy, blockX, blockY)) return;
        if (tryNpcPushPastCrowd(w, x, y+dy, blockX, blockY)) return;
        if (canNpcStep(w,x+dx,y,blockX,blockY)) { moveTo(x+dx, y); return; }
        if (x+dx == blockX && y == blockY) tryNpcShuffleAroundBlockedActor(w, dx, 0, blockX, blockY);
        else tryNpcPushPastCrowd(w, x+dx, y, blockX, blockY);
    }

    boolean tryNpcShuffleAroundBlockedActor(World w, int dx, int dy, int blockX, int blockY) {
        if (w == null || "Hostile".equalsIgnoreCase(state) || isProtectedCleric()) return false;
        int[][] sides = dx != 0 ? new int[][]{{0,-1},{0,1},{-dx,0}} : new int[][]{{-1,0},{1,0},{0,-dy}};
        for(int[] s: sides){
            int nx = x + s[0], ny = y + s[1];
            if(canNpcStep(w,nx,ny,blockX,blockY)) { moveTo(nx, ny); state = "Jostled"; return true; }
        }
        return false;
    }

    boolean tryNpcPushPastCrowd(World w, int nx, int ny, int blockX, int blockY) {
        if (w == null || crowdShuffleCooldown > 0 || "Hostile".equalsIgnoreCase(state) || isProtectedCleric()) return false;
        if (!w.inBounds(nx, ny) || !w.walkable(nx, ny) || (nx == blockX && ny == blockY)) return false;
        NpcEntity other = w.npcAt(nx, ny);
        if (other == null || other == this || other.isProtectedCleric() || "Hostile".equalsIgnoreCase(other.state)) return false;
        int oldX = x, oldY = y;
        other.moveTo(oldX, oldY);
        other.state = "Jostled";
        other.crowdShuffleCooldown = 2;
        moveTo(nx, ny);
        state = "Jostled";
        crowdShuffleCooldown = 2;
        DebugLog.audit("NPC_PUSH_PAST_CROWD", "npc=" + id + " other=" + other.id + " from=" + oldX + "," + oldY + " to=" + x + "," + y + " zone=" + (w.zoneType == null ? "unknown" : w.zoneType.label));
        return true;
    }

    boolean canNpcStep(World w, int nx, int ny, int blockX, int blockY) {
        return w != null && w.walkable(nx,ny) && w.npcAt(nx,ny)==null && !(nx == blockX && ny == blockY);
    }


    boolean isProtectedCleric() { return faction == Faction.MINISTORUM && role != null && role.toLowerCase(Locale.ROOT).contains("head cleric"); }
    boolean isFactionRepresentative() { return role != null && role.toLowerCase(Locale.ROOT).contains("faction representative"); }
    boolean isUntargetableAnchor() { return isProtectedCleric() || isFactionRepresentative(); }

    boolean isTrader() { return role != null && role.contains("Trader"); }

    static NpcEntity tradeClerkFor(ZoneType z, int x, int y, Random r) {
        NpcEntity n = create(Faction.CIVIC_LEDGER_OFFICE, z, x, y, r);
        n.role = "Trader-Clerk";
        n.state = "Trade";
        n.symbol = 'T';
        n.name = "Counterfactor " + n.name.split(" ")[0];
        return n;
    }

    static String roleFor(Faction f, ZoneType z, Random r) {
        if (f == Faction.MINISTORUM) return r.nextDouble() < 0.35 ? "Priest" : "Pilgrim";
        if (f == Faction.SORORITAS) return "Sister of Battle Guard";
        if (f == Faction.INN) return r.nextDouble() < 0.34 ? "Reporter" : (r.nextDouble() < 0.55 ? "Editor" : "Broadcaster");
        if (z == ZoneType.NEUTRAL_RAIL_DEPOT || z == ZoneType.SUMP_MARKET) return r.nextDouble() < 0.45 ? "Trader" : "Local";
        if (z == ZoneType.NEUTRAL_CIVILIAN_FLOOR && (f == Faction.CIVIC_LEDGER_OFFICE || f == Faction.NONE || f == Faction.SCAVENGER)) return r.nextDouble() < 0.18 ? "Trader" : "Civilian";
        if (z == ZoneType.MECHANICUS_FORGE_CLOISTER && (f == Faction.MECHANIST_COLLEGIA || f.name().startsWith("MECHANIST COLLEGIA"))) return r.nextDouble() < 0.22 ? "Trader-Machinist" : "Adept";
        if (z == ZoneType.GANGER_TURF && (f == Faction.BANDIT || f.name().startsWith("GANGER"))) return r.nextDouble() < 0.16 ? "Trader-Fence" : "Ganger";
        return "Resident";
    }

    static char symbolFor(Faction f, ZoneType z) {
        if (f == Faction.MINISTORUM) return 'c';
        if (f == Faction.SORORITAS) return 'S';
        if (f == Faction.INN) return 'q';
        if (f == Faction.CIVIC_WARDENS) return 'A';
        if (f == Faction.IMPERIAL_GUARD) return 'M';
        if (f == Faction.MUTANT) return 'm';
        if (f == Faction.CULTIST || f == Faction.HERETIC) return 'H';
        if (f == Faction.MECHANIST_COLLEGIA || f == Faction.MECHANICUS_CLOISTER_RED || f == Faction.MECHANICUS_CLOISTER_RUST || f == Faction.MECHANICUS_CLOISTER_VOID) return 'q';
        if (f == Faction.BANDIT || f.name().startsWith("GANGER")) return 'g';
        if (f == Faction.NOBLE || f.name().startsWith("NOBLE")) return 'n';
        if (f == Faction.HIVER || f.name().startsWith("HIVER")) return 'h';
        return 'c';
    }

    static String defaultState(Faction f, Random r) {
        if (f == Faction.MINISTORUM) return r.nextBoolean()?"Prayer":"Pilgrim Service";
        if (f == Faction.SORORITAS) return "Guard";
        if (f == Faction.INN) return r.nextBoolean()?"Reporting":"Editing";
        if (f == Faction.CIVIC_WARDENS || f == Faction.IMPERIAL_GUARD) return r.nextBoolean()?"Patrol":"Guard";
        if (f == Faction.MUTANT || f == Faction.CULTIST || f == Faction.HERETIC) return r.nextBoolean()?"Hostile":"Wander";
        if (f == Faction.MECHANIST_COLLEGIA || f.name().startsWith("MECHANIST COLLEGIA")) return r.nextBoolean()?"Inspect":"Patrol";
        return r.nextBoolean()?"Idle":"Wander";
    }

    String originSummary() { return provenance == null ? "Origin: unrecorded personnel provenance." : provenance.summary(); }

    String saveLine(){
        ensureRankIdentity(new Random(numericId == 0 ? Objects.hash(name, faction) : numericId));
        return enc(id)+"|"+numericId+"|"+enc(name)+"|"+(faction==null?Faction.NONE.name():faction.name())+"|"+symbol+"|"+x+"|"+y+"|"+homeX+"|"+homeY+"|"+enc(state)+"|"+enc(role)+"|"+hunger+"|"+thirst+"|"+sleepDebt+"|"+portraitIndex+"|"+enc(provenance==null?"":provenance.save())+"|"+intellect+"|"+equipmentTier+"|"+enc(equippedMeleeWeapon)+"|"+enc(equippedRangedWeapon)+"|"+enc(equippedArmor)+"|"+enc(equippedExplosive)+"|"+loadedShots+"|"+ammoReloadsRemaining+"|"+factionRank+"|"+enc(factionRankTitle)+"|"+enc(factionRankScope)+"|"+enc(creatureKind)+"|"+enc(animalProfileId)+"|"+enc(companionOf)+"|"+ageYears+"|"+birthWorldTurn+"|"+enc(ageBand)+"|"+enc(nameLockedProfileKey);
    }

    static NpcEntity parseLine(String s, World w){
        String[] a=s.split("\\|",-1);
        if(a.length<15) return null;
        NpcEntity n=new NpcEntity();
        n.id=dec(a[0]); n.numericId=Integer.parseInt(a[1]); n.name=dec(a[2]); n.faction=Faction.valueOf(a[3]); n.symbol=a[4].isEmpty()?'c':a[4].charAt(0);
        n.x=Integer.parseInt(a[5]); n.y=Integer.parseInt(a[6]); n.homeX=Integer.parseInt(a[7]); n.homeY=Integer.parseInt(a[8]); n.state=dec(a[9]); n.role=dec(a[10]);
        n.hunger=Integer.parseInt(a[11]); n.thirst=Integer.parseInt(a[12]); n.sleepDebt=Integer.parseInt(a[13]); n.portraitIndex=Integer.parseInt(a[14]); n.idleBias=3; n.routineOffset=0;
        n.provenance = a.length>=16 && !dec(a[15]).isEmpty() ? PersonnelProvenanceRecord.parse(dec(a[15])) : PersonnelProvenanceApi.create(n.faction, w==null?ZoneType.NEUTRAL_CIVILIAN_FLOOR:w.zoneType, "legacy save import", w==null?0:w.sectorX, w==null?0:w.sectorY, w==null?0:w.zoneX, w==null?0:w.zoneY, w==null?0:w.floor, w!=null && w.sewerLayer, new Random(n.numericId));
        if (a.length >= 24) {
            try { n.intellect = Integer.parseInt(a[16]); } catch(Exception ignored) {}
            try { n.equipmentTier = Integer.parseInt(a[17]); } catch(Exception ignored) {}
            n.equippedMeleeWeapon = dec(a[18]);
            n.equippedRangedWeapon = dec(a[19]);
            n.equippedArmor = dec(a[20]);
            n.equippedExplosive = dec(a[21]);
            try { n.loadedShots = Integer.parseInt(a[22]); } catch(Exception ignored) {}
            try { n.ammoReloadsRemaining = Integer.parseInt(a[23]); } catch(Exception ignored) {}
        }
        if (a.length >= 27) {
            try { n.factionRank = Integer.parseInt(a[24]); } catch(Exception ignored) {}
            n.factionRankTitle = dec(a[25]);
            n.factionRankScope = dec(a[26]);
        }
        if (a.length >= 30) {
            n.creatureKind = dec(a[27]);
            n.animalProfileId = dec(a[28]);
            n.companionOf = dec(a[29]);
        }
        if (a.length >= 33) {
            try { n.ageYears = Integer.parseInt(a[30]); } catch(Exception ignored) {}
            try { n.birthWorldTurn = Long.parseLong(a[31]); } catch(Exception ignored) {}
            n.ageBand = dec(a[32]);
        }
        if (a.length >= 34) n.nameLockedProfileKey = dec(a[33]);
        if (n.creatureKind == null || n.creatureKind.isBlank()) n.creatureKind = "humanoid";
        if (!n.isAnimalActor() && n.ageYears <= 0) AgeAndWorldTimeAuthority.initializeNpcAge(n, w == null ? ZoneType.NEUTRAL_CIVILIAN_FLOOR : w.zoneType, new Random(n.numericId == 0 ? 1 : n.numericId));
        AgeAndWorldTimeAuthority.synchronizeNpc(n, 0L);
        n.ensureRankIdentity(new Random(n.numericId == 0 ? 1 : n.numericId));
        n.ensureCombatLoadout(new Random(n.numericId == 0 ? 1 : n.numericId));
        return n;
    }

    static String enc(String v){ return Base64.getUrlEncoder().withoutPadding().encodeToString((v==null?"":v).getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    static String dec(String v){ try{return new String(Base64.getUrlDecoder().decode(v), java.nio.charset.StandardCharsets.UTF_8);}catch(Exception e){return v==null?"":v;} }

    static String nameFor(Faction f, Random r) {
        return CharacterCreationAuthority.randomNpcName(f, r == null ? new Random() : r);
    }
}




