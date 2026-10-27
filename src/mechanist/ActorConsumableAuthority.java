package mechanist;

import java.util.*;

/** Shared finite field treatment and temporary combat augmentation. Values are initial game tuning. */
final class ActorConsumableAuthority {
    record Profile(String family,int heal,int wounds,int bleeding,int infection,int pain,int fatigue,
                   int melee,int firearms,int damage,int defense,int duration,int crash) { }
    record Effect(String item,String family,long expires,int melee,int firearms,int damage,int defense,int crash) { }
    static final class State {
        int maximumHp,pain,bleeding,infection,fatigue;
        final ArrayList<String> items=new ArrayList<>();
        final LinkedHashMap<String,Effect> effects=new LinkedHashMap<>();
        final LinkedHashMap<String,MedicalTreatmentAuthority.Recovery> recoveries=new LinkedHashMap<>();
        final LinkedHashSet<String> lostParts=new LinkedHashSet<>();
        final LinkedHashMap<String,CyberneticLimbAuthority.Limb> limbs=new LinkedHashMap<>();
        final EnumMap<CharacterEquipmentAndMedicalAuthority.EquipmentSlot,String> worn=new EnumMap<>(CharacterEquipmentAndMedicalAuthority.EquipmentSlot.class);
        String save() {
            ArrayList<String> parts=new ArrayList<>(List.of("1",""+maximumHp,""+pain,""+bleeding,""+infection,""+fatigue));
            parts.add(NpcEntity.enc(String.join("\n",items)));
            ArrayList<String> rows=new ArrayList<>();
            for(Effect e:effects.values()) rows.add(NpcEntity.enc(e.item)+","+e.family+","+e.expires+","+e.melee+","+e.firearms+","+e.damage+","+e.defense+","+e.crash);
            parts.add(NpcEntity.enc(String.join("\n",rows)));rows.clear();
            for(var e:worn.entrySet()) rows.add(e.getKey().name()+","+NpcEntity.enc(e.getValue()));
            parts.add(NpcEntity.enc(String.join("\n",rows)));rows.clear();
            for(var r:recoveries.values()) rows.add(NpcEntity.enc(r.item())+","+r.remaining()+","+r.pulses()+","+r.nextTurn());
            parts.add(NpcEntity.enc(String.join("\n",rows)));parts.add(NpcEntity.enc(String.join("\n",lostParts)));rows.clear();for(var limb:limbs.values()) rows.add(limb.save());parts.add(NpcEntity.enc(String.join("\n",rows)));return String.join("|",parts);
        }
        void restore(String value) {
            items.clear();effects.clear();worn.clear();recoveries.clear();lostParts.clear();limbs.clear();maximumHp=0;pain=bleeding=infection=fatigue=0;
            if(value==null || value.isBlank()) return;
            try {
                String[] p=value.split("\\|",-1);maximumHp=Math.max(0,Integer.parseInt(p[1]));pain=Math.max(0,Integer.parseInt(p[2]));bleeding=Math.max(0,Integer.parseInt(p[3]));infection=Math.max(0,Integer.parseInt(p[4]));fatigue=Math.max(0,Integer.parseInt(p[5]));
                String decoded=NpcEntity.dec(p[6]);if(!decoded.isBlank()) items.addAll(Arrays.asList(decoded.split("\n")));
                decoded=NpcEntity.dec(p[7]);if(!decoded.isBlank()) for(String row:decoded.split("\n")) {String[] e=row.split(",");Effect effect=new Effect(NpcEntity.dec(e[0]),e[1],Long.parseLong(e[2]),Integer.parseInt(e[3]),Integer.parseInt(e[4]),Integer.parseInt(e[5]),Integer.parseInt(e[6]),Integer.parseInt(e[7]));effects.put(effect.family,effect);}
                decoded=NpcEntity.dec(p[8]);if(!decoded.isBlank()) for(String row:decoded.split("\n")) {String[] e=row.split(",");worn.put(CharacterEquipmentAndMedicalAuthority.EquipmentSlot.valueOf(e[0]),NpcEntity.dec(e[1]));}
                if(p.length>9) {decoded=NpcEntity.dec(p[9]);if(!decoded.isBlank()) for(String row:decoded.split("\n")) {String[] e=row.split(",");recoveries.put("TISSUE",new MedicalTreatmentAuthority.Recovery(NpcEntity.dec(e[0]),Integer.parseInt(e[1]),Integer.parseInt(e[2]),Long.parseLong(e[3])));}}
                if(p.length>10) {decoded=NpcEntity.dec(p[10]);if(!decoded.isBlank()) lostParts.addAll(Arrays.asList(decoded.split("\n")));}
                if(p.length>11) {decoded=NpcEntity.dec(p[11]);if(!decoded.isBlank()) for(String row:decoded.split("\n")) {var limb=CyberneticLimbAuthority.Limb.read(row);limbs.put(limb.root,limb);}}
            } catch(RuntimeException malformed) {items.clear();effects.clear();worn.clear();recoveries.clear();lostParts.clear();limbs.clear();}
        }
    }
    private ActorConsumableAuthority() { }
    private static Profile profile(int heal,int wounds,int bleed,int infection,int pain,int fatigue) {return new Profile("",heal,wounds,bleed,infection,pain,fatigue,0,0,0,0,0,0);}
    static Profile profile(String item) {
        ItemDef def=ItemCatalog.get(item);if(def==null) return null;
        String name=def.name.toLowerCase(Locale.ROOT),category=def.category;
        if(category.equals("stimulant")) return new Profile("WAKEFUL",0,0,0,0,0,6,1,1,0,0,6,3);
        if(category.equals("medical")) return switch(name) {
            case "bandage roll" -> profile(0,1,2,0,1,0);
            case "field dressings" -> profile(0,1,3,0,1,2);
            case "antiseptic vial" -> profile(0,0,0,6,0,0);
            case "splint kit" -> profile(0,1,0,0,3,0);
            case "medkit" -> profile(10,2,4,4,3,3);
            case "injector case" -> profile(5,1,1,0,3,4);
            default -> null;
        };
        if(category.equals("medical/chem")) return switch(name) {
            case "medi-stimm" -> new Profile("RECOVERY",4,1,1,0,2,4,1,1,0,0,4,2);
            case "white mercy","nerve lace","saint’s anesthetic" -> new Profile("ANALGESIC",0,0,0,0,6,0,-1,-1,0,-1,5,1);
            case "red waker","wakewire solution" -> new Profile("RECOVERY",0,0,0,0,1,6,1,2,0,0,5,3);
            case "clotfoam ampoule" -> profile(0,1,6,0,1,0);
            case "stitchdream" -> profile(6,2,2,2,3,1);
            case "pale nurse" -> profile(5,1,1,4,2,1);
            case "mercy spoil" -> profile(3,1,0,-2,1,0);
            default -> null;
        };
        if(category.equals("chem/ganger-combat")) {
            if(List.of("slaught","frenzon","knuckle joy","hate salt","chainbite","ripper tabs").contains(name)) return new Profile("COMBAT",0,0,0,0,4,0,3,0,2,-2,6,5);
            if(List.of("bolt rush","smokeghost","wirewake").contains(name)) return new Profile("COMBAT",0,0,0,0,1,2,0,3,0,1,6,4);
            return new Profile("COMBAT",0,0,0,0,2,4,2,2,1,0,6,4);
        }
        if(category.equals("chem/everyday-stimulant")) return new Profile("WAKEFUL",0,0,0,0,0,4,0,1,0,0,8,1);
        if(category.equals("chem/labor-control")) return new Profile("WAKEFUL",0,0,0,0,1,5,0,1,0,0,7,2);
        return null;
    }
    static boolean supported(String item) {return profile(item)!=null;}
    static void seedNpcSupplies(NpcEntity npc) {
        if(npc.isAnimalActor() || npc.developerStationary) return;
        String role=npc.role==null?"":npc.role.toLowerCase(Locale.ROOT);
        if(role.contains("medic") || role.contains("doctor")) {npc.combatSupport.items.add("Medkit");npc.combatSupport.items.add("Bandage roll");}
        else if(npc.faction==Faction.IMPERIAL_GUARD || npc.faction==Faction.CIVIC_WARDENS || npc.faction==Faction.SORORITAS) npc.combatSupport.items.add("Field dressings");
        else if((npc.faction==Faction.BANDIT || npc.faction==Faction.CULTIST) && Math.floorMod(Objects.hash(npc.id,npc.numericId),2)==0) npc.combatSupport.items.add("Street Stimm");
    }
    static void ensure(NpcEntity npc) {npc.combatSupport.maximumHp=Math.max(npc.combatSupport.maximumHp,npc.hp);}
    static int legacyMaximumHp(NpcEntity npc) {
        if(npc.isAnimalActor()) {
            for(AnimalProfile[] group:new AnimalProfile[][]{AnimalPopulationApi.SEWER_WILD,AnimalPopulationApi.URBAN_WILD,AnimalPopulationApi.FARM,AnimalPopulationApi.KENNEL,AnimalPopulationApi.PETS})
                for(AnimalProfile profile:group) if(profile.id.equals(npc.animalProfileId)) return Math.max(npc.hp,profile.hp);
            return Math.max(1,npc.hp);
        }
        int base=npc.nameLockedProfileKey!=null && !npc.nameLockedProfileKey.isBlank()?18:12;
        if(npc.faction==Faction.SORORITAS && npc.role!=null && npc.role.toLowerCase(Locale.ROOT).contains("guard")) base=24;
        return Math.max(base,npc.hp);
    }
    static String rejection(GamePanel game,NpcEntity npc,String item) {
        Profile p=profile(item);if(p==null) return "No active field-treatment or combat-effect profile for this item.";
        if(npc!=null && (npc.hp<=0 || npc.developerStationary)) return "This actor cannot use combat supplies.";
        if(npc!=null && npc.faction==Faction.ROGUE_MACHINE) return "Organic medicine cannot repair a machine.";
        if(npc==null && !ActorCombatAuthority.playerAlive(game)) return "Field treatment cannot revive a fatal injury.";
        State state=npc==null?game.combatSupport:npc.combatSupport;
        if(MedicalTreatmentAuthority.category(item)==MedicalTreatmentAuthority.Category.RECOVERY_OVER_TIME && !state.recoveries.isEmpty()) return "A tissue-recovery course is already active; courses do not stack.";
        Effect effect=state.effects.get(p.family);
        if(!p.family.isEmpty() && effect!=null && effect.expires>game.worldTurn) return "An effect from this family is already active; doses do not stack.";
        if(p.duration==0) {
            boolean needs=npc==null?
                    p.heal>0 && MedicalTreatmentAuthority.hasRepairableTissue(game) || p.wounds>0 && game.wounds>0 || p.bleeding>0 && game.bleeding>0 || p.infection>0 && game.infectionRisk>0 || p.pain>0 && game.pain>0 || p.fatigue>0 && game.fatigue>0:
                    p.heal>0 && npc.hp<state.maximumHp || p.bleeding>0 && state.bleeding>0 || p.infection>0 && state.infection>0 || p.pain>0 && state.pain>0 || p.fatigue>0 && state.fatigue>0;
            if(!needs) return "No injury or condition this treatment can improve.";
        }
        return "";
    }
    static boolean usePlayer(GamePanel game,String item) {
        String refused=rejection(game,null,item);if(!refused.isEmpty()) {game.logEvent(refused);return false;}
        int index=game.inventory.indexOf(item);if(index<0) {game.logEvent("The item must be carried.");return false;}
        // Remove one physical inventory instance as well as its legacy view.
        consumePlayerItem(game,index,item);
        apply(game,null,item);game.moveInventorySelection(0);game.advanceTurn("uses "+item+".");return true;
    }
    static void consumePlayerItem(GamePanel game,int index,String item) {
        ContainerRecord container=game.itemContainers.get(GamePanel.CONTAINER_PLAYER_INVENTORY);
        if(container!=null) for(Iterator<String> it=container.itemInstanceIds.iterator();it.hasNext();) {String id=it.next();ItemInstance instance=game.itemInstances.get(id);if(instance!=null && instance.displayName.equals(item)) {it.remove();game.itemInstances.remove(id);break;}}
        game.inventory.remove(index);game.takeProvenanceForItem(item);
    }
    static boolean useNpc(GamePanel game,NpcEntity npc,String item) {
        if(npc==null || npc.lastTickTurn==game.turn || !npc.combatSupport.items.contains(item) || !rejection(game,npc,item).isEmpty()) return false;
        npc.combatSupport.items.remove(item);apply(game,npc,item);npc.lastTickTurn=game.turn;return true;
    }
    private static void apply(GamePanel game,NpcEntity npc,String item) {
        Profile p=profile(item);State state=npc==null?game.combatSupport:npc.combatSupport;
        int quality=ItemQuality.tierIndex(item),heal=p.heal==0?0:Math.max(1,p.heal+(quality-2));
        int recovered=0;
        if(npc!=null) {
            ensure(npc);
            state.pain=Math.max(0,state.pain-p.pain);state.bleeding=Math.max(0,state.bleeding-p.bleeding);state.infection=Math.max(0,state.infection-p.infection);state.fatigue=Math.max(0,state.fatigue-p.fatigue);
            if(ItemCatalog.get(item).category.equals("stimulant")) npc.sleepDebt=Math.max(0,npc.sleepDebt-(ItemCatalog.get(item).name.equals("Coffee tin")?4:8));
        } else {
            game.wounds=Math.max(0,game.wounds-p.wounds);game.bleeding=Math.max(0,game.bleeding-p.bleeding);game.infectionRisk=Math.max(0,game.infectionRisk-p.infection);game.pain=Math.max(0,game.pain-p.pain);game.fatigue=Math.max(0,game.fatigue-p.fatigue);
            game.stimulantStrain+=p.crash>0?1:0;
            if(ItemCatalog.get(item).category.equals("stimulant")) game.sleepNeed=Math.max(0,game.sleepNeed-(ItemCatalog.get(item).name.equals("Coffee tin")?4:8));
        }
        if(MedicalTreatmentAuthority.category(item)==MedicalTreatmentAuthority.Category.RECOVERY_OVER_TIME) MedicalTreatmentAuthority.schedule(game,npc,item,heal);
        else recovered=MedicalTreatmentAuthority.heal(game,npc,heal);
        if(p.duration>0) state.effects.put(p.family,new Effect(item,p.family,game.worldTurn+Math.max(2,p.duration+quality-2)+1,p.melee,p.firearms,p.damage,p.defense,p.crash));
        ActorCombatAuthority.diagnostics(game).healingRecovered+=recovered;
        ActorCombatAuthority.supportEvent(game,npc==null?"PLAYER":npc.id,"USE "+item+" heal="+recovered+" requested="+heal+" duration="+p.duration);
        if(npc==null) game.logEvent("Used "+item+". "+description(item)+" "+summary(state,game.worldTurn));
    }
    static boolean autoUse(GamePanel game,NpcEntity npc,boolean inCombat) {
        if(npc.lastTickTurn==game.turn) return false;
        ensure(npc);State state=npc.combatSupport;
        for(String item:new ArrayList<>(state.items)) {Profile p=profile(item);if(p==null) continue;
            if(p.family.equals("COMBAT")) continue;
            boolean care=(p.heal>0 && npc.hp<=state.maximumHp*0.6) || p.bleeding>0 && state.bleeding>0 || p.infection>0 && state.infection>0 || p.pain>0 && state.pain>=5 || p.fatigue>0 && state.fatigue>=10 || ItemCatalog.get(item).category.equals("stimulant") && npc.sleepDebt>=8;
            if(care && useNpc(game,npc,item)) return true;
        }
        for(String item:new ArrayList<>(state.items)) if(ActorArmorAuthority.wearable(item)) {
            var slot=ActorArmorAuthority.slot(item);String current=slot==CharacterEquipmentAndMedicalAuthority.EquipmentSlot.CLOTHES?npc.equippedArmor:state.worn.get(slot);
            if(ActorArmorAuthority.protectionScore(item)>ActorArmorAuthority.protectionScore(current) && equipNpc(game,npc,item)) return true;
        }
        if(inCombat) for(String item:new ArrayList<>(state.items)) {Profile p=profile(item);if(p!=null && (p.family.equals("COMBAT") || p.duration>0 && (p.melee>0 || p.firearms>0)) && useNpc(game,npc,item)) return true;}
        return false;
    }
    static boolean equipNpc(GamePanel game,NpcEntity npc,String item) {
        if(npc==null || npc.hp<=0 || npc.developerStationary || npc.lastTickTurn==game.turn || !npc.combatSupport.items.contains(item) || !ActorArmorAuthority.wearable(item)) return false;
        State state=npc.combatSupport;var slot=ActorArmorAuthority.slot(item);String old;
        if(slot==CharacterEquipmentAndMedicalAuthority.EquipmentSlot.CLOTHES) {old=npc.equippedArmor;npc.equippedArmor=item;}
        else old=state.worn.put(slot,item);
        state.items.remove(item);if(old!=null && !old.isBlank()) state.items.add(old);
        npc.lastTickTurn=game.turn;ActorCombatAuthority.supportEvent(game,npc.id,"EQUIP "+item);return true;
    }
    static void tick(GamePanel game) {
        CyberneticLimbAuthority.tick(game,null);MedicalTreatmentAuthority.tick(game,null);expire(game,null);
        if(game.world!=null) for(NpcEntity npc:game.world.npcs) {CyberneticLimbAuthority.tick(game,npc);MedicalTreatmentAuthority.tick(game,npc);if(npc.hp>0) expire(game,npc);}
    }
    private static void expire(GamePanel game,NpcEntity npc) {
        State state=npc==null?game.combatSupport:npc.combatSupport;
        if(state.effects.isEmpty()) return;
        for(Iterator<Effect> it=state.effects.values().iterator();it.hasNext();) {Effect e=it.next();if(e.expires>game.worldTurn) continue;it.remove();
            if(npc==null) {game.fatigue+=e.crash;game.pain+=e.crash/2;game.logEvent(e.item+" wears off. Fatigue +"+e.crash+"; pain +"+e.crash/2+".");}
            else {state.fatigue+=e.crash;state.pain+=e.crash/2;}
            ActorCombatAuthority.supportEvent(game,npc==null?"PLAYER":npc.id,"EXPIRED "+e.item+" crash="+e.crash);
        }
    }
    static int skill(GamePanel game,NpcEntity npc,boolean ranged,int base) {
        State state=npc==null?game.combatSupport:npc.combatSupport;int bonus=0;
        for(Effect e:state.effects.values()) if(e.expires>game.worldTurn) bonus+=ranged?e.firearms:e.melee;
        int pain=npc==null?game.pain:state.pain,fatigue=npc==null?game.fatigue:state.fatigue;
        return Math.max(0,base+CyberneticLimbAuthority.bonus(state,ranged)+Math.max(-6,Math.min(6,bonus))-Math.min(3,pain/5)-Math.min(2,fatigue/10));
    }
    static int bonus(GamePanel game,NpcEntity npc,boolean defense) {
        State state=npc==null?game.combatSupport:npc.combatSupport;int value=0;
        for(Effect e:state.effects.values()) if(e.expires>game.worldTurn) value+=defense?e.defense:e.damage;
        return Math.max(-4,Math.min(4,value));
    }
    static String summary(State state,long now) {
        ArrayList<String> effects=new ArrayList<>();for(Effect e:state.effects.values()) if(e.expires>now) effects.add(e.item+" ("+(e.expires-now)+" turns)");
        for(var recovery:state.recoveries.values()) effects.add(recovery.item()+" recovery ("+recovery.pulses()+" pulses remaining)");
        return effects.isEmpty()?"No active combat drug effect or recovery course.":"Active: "+String.join(", ",effects)+".";
    }
    static String description(String item) {Profile p=profile(item);if(p==null) return "No active combat-use effect.";
        return "Base effects at Common quality: healing "+p.heal+"; wounds "+signed(-p.wounds)+"; bleeding "+signed(-p.bleeding)+"; infection "+signed(-p.infection)+"; pain "+signed(-p.pain)+"; fatigue "+signed(-p.fatigue)+"; Melee "+signed(p.melee)+"; Firearms "+signed(p.firearms)+"; damage "+signed(p.damage)+"; defense "+signed(p.defense)+"; augmentation duration "+p.duration+" turns; fatigue crash "+p.crash+". Quality changes recovery amounts and active drug durations where applicable. Same-family doses do not stack.";}
    private static String signed(int value) {return value>=0?"+"+value:""+value;}
}
