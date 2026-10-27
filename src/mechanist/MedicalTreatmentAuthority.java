package mechanist;

import java.util.*;

/** Field care repairs living organic tissue; it is not limb replacement or regeneration. */
final class MedicalTreatmentAuthority {
    enum Category { STABILIZATION, INSTANT_RECOVERY, RECOVERY_OVER_TIME, CONDITION_RELIEF, AUGMENTATION, REGENERATION, NONE }
    record Recovery(String item,int remaining,int pulses,long nextTurn) { }
    private MedicalTreatmentAuthority() { }
    static Category category(String item) {
        ItemDef def=ItemCatalog.get(item);if(def==null) return Category.NONE;
        return switch(def.name) {
            case "Bandage roll","Field dressings","Splint kit","Antiseptic vial","Clotfoam Ampoule" -> Category.STABILIZATION;
            case "Stitchdream","Pale Nurse" -> Category.RECOVERY_OVER_TIME;
            // The protected Draught line is reserved for regeneration, not a generic healing fallback.
            case "Black Sun Draught" -> Category.REGENERATION;
            default -> {var p=ActorConsumableAuthority.profile(item);yield p==null?Category.NONE:p.heal()>0?Category.INSTANT_RECOVERY:p.duration()>0 && (p.melee()>0 || p.firearms()>0)?Category.AUGMENTATION:Category.CONDITION_RELIEF;}
        };
    }
    static boolean repairable(BodyPart part) {return part!=null && !part.missing && !part.cybernetic && part.health>0 && part.health<part.maxHealth();}
    static boolean hasRepairableTissue(GamePanel game) {return game.active!=null && game.active.body.values().stream().anyMatch(MedicalTreatmentAuthority::repairable);}
    static int heal(GamePanel game,NpcEntity npc,int amount) {
        if(amount<=0) return 0;
        if(npc!=null) {
            if(npc.hp<=0 || npc.faction==Faction.ROGUE_MACHINE) return 0;
            ActorConsumableAuthority.ensure(npc);int before=npc.hp;
            npc.hp=Math.min(npc.combatSupport.maximumHp,npc.hp+amount);
            return npc.hp-before; // Abstract vital HP; lost-part markers remain untouched.
        }
        if(!ActorCombatAuthority.playerAlive(game)) return 0;
        BodyPart worst=null;
        for(BodyPart part:game.active.body.values()) if(repairable(part) && (worst==null || part.health/part.maxHealth()<worst.health/worst.maxHealth())) worst=part;
        if(worst==null) return 0;double before=worst.health;
        worst.health=Math.min(worst.maxHealth(),worst.health+amount*10.0);
        return (int)Math.round((worst.health-before)/10.0);
    }
    static void schedule(GamePanel game,NpcEntity npc,String item,int total) {
        var state=npc==null?game.combatSupport:npc.combatSupport;
        int pulses=ItemCatalog.get(item).name.equals("Stitchdream")?6:5;
        state.recoveries.put("TISSUE",new Recovery(item,total,pulses,game.worldTurn+2));
    }
    static void tick(GamePanel game,NpcEntity npc) {
        var state=npc==null?game.combatSupport:npc.combatSupport;
        if(state.recoveries.isEmpty()) return;
        if(npc==null?!ActorCombatAuthority.playerAlive(game):npc.hp<=0) {state.recoveries.clear();return;}
        for(String key:new ArrayList<>(state.recoveries.keySet())) {
            Recovery r=state.recoveries.get(key);int budget=64;
            while(r.nextTurn<=game.worldTurn && r.remaining>0 && r.pulses>0 && budget-->0) {
                int requested=Math.max(1,(r.remaining+r.pulses-1)/r.pulses);
                int recovered=heal(game,npc,requested);ActorCombatAuthority.diagnostics(game).healingRecovered+=recovered;
                ActorCombatAuthority.supportEvent(game,npc==null?"PLAYER":npc.id,"RECOVERY "+r.item+" heal="+recovered);
                r=new Recovery(r.item,r.remaining-requested,r.pulses-1,r.nextTurn+1);
            }
            if(r.pulses<=0 || r.remaining<=0) state.recoveries.remove(key);else state.recoveries.put(key,r);
        }
    }
    static void sever(Candidate candidate,String part) {
        if(candidate==null) return;
        for(String name:descendants(part)) {BodyPart b=candidate.body.get(name);if(b!=null) {b.missing=true;b.health=0;}}
    }
    static void sever(NpcEntity npc,String part) {if(npc!=null) {for(var l:new ArrayList<>(npc.combatSupport.limbs.values())) if(descendants(part).contains(l.root) || descendants(l.root).contains(part)) {npc.combatSupport.limbs.remove(l.root);npc.combatSupport.lostParts.addAll(descendants(l.root));}npc.combatSupport.lostParts.addAll(descendants(part));}}
    static Set<String> descendants(String part) {
        if(part==null || part.isBlank()) return Set.of();
        LinkedHashSet<String> result=new LinkedHashSet<>();result.add(part);
        if(part!=null && (part.startsWith("L ") || part.startsWith("R "))) {
            String side=part.substring(0,2);
            if(part.endsWith("Upper Arm")) {result.add(side+"Lower Arm");result.add(side+"Hand");}
            if(part.endsWith("Lower Arm")) result.add(side+"Hand");
            if(part.endsWith("Upper Leg")) {result.add(side+"Lower Leg");result.add(side+"Foot");}
            if(part.endsWith("Lower Leg")) result.add(side+"Foot");
        }
        return result;
    }
    static boolean functionalHand(Candidate candidate,String side) {
        if(candidate==null) return false;
        for(String segment:List.of("Upper Arm","Lower Arm","Hand")) {BodyPart part=candidate.body.get(side+" "+segment);if(part==null || part.destroyed()) return false;}
        return true;
    }
    static int hands(NpcEntity npc) {return (CyberneticLimbAuthority.available(npc.combatSupport,"L Hand")?1:0)+(CyberneticLimbAuthority.available(npc.combatSupport,"R Hand")?1:0);}
    static int requiredHands(StructuralScaleCombatAuthority.WeaponProfile weapon) {
        return switch(weapon.tier()) {case RIFLE,HEAVY_HANDHELD,ANTI_ARMOR,MOUNTED -> 2;default -> 1;};
    }
    static String attackRejection(GamePanel game,StructuralScaleCombatAuthority.WeaponProfile weapon) {
        if(game.active==null) return "";
        boolean left=functionalHand(game.active,"L") && CyberneticLimbAuthority.available(game.combatSupport,"L Hand"),right=functionalHand(game.active,"R") && CyberneticLimbAuthority.available(game.combatSupport,"R Hand");
        String hand=game.activeWeaponHandLabel();
        if(hand.equals("LEFT") && !left || hand.equals("RIGHT") && !right) return "The weapon's holding arm is destroyed or missing; replacement or regeneration is required.";
        if((left?1:0)+(right?1:0)<requiredHands(weapon)) return "Not enough functioning arms for this weapon.";
        return "";
    }
    static String description(String item) {
        return switch(category(item)) {
            case STABILIZATION -> "Stabilization: controls the listed conditions; does not restore tissue or replace limbs.";
            case INSTANT_RECOVERY -> "Instant recovery: living, present organic tissue only. Cannot repair destroyed parts, missing limbs or cybernetics.";
            case RECOVERY_OVER_TIME -> "Gradual recovery: starts on a later turn; one active tissue-recovery course. Cannot regrow limbs or repair cybernetics.";
            case REGENERATION -> "Regeneration is reserved for rare high-grade Draught medicine. Limb-regeneration execution is not active yet.";
            default -> "";
        };
    }
}
