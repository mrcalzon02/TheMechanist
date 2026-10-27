package mechanist;

import java.util.*;

/** Turn-owned actor attacks; renderers and inspection never execute or replenish combat. */
final class ActorCombatAuthority {
    enum Outcome { NO_TARGET, PROTECTED, DEAD, DISABLED, RANGE, WALL, ALLY_IN_LINE, EMPTY, RELOAD, MOVE, MISS, HIT, KILL, SUPPORT }
    record Event(int turn,String attacker,String target,String weapon,Outcome outcome,int chance,int roll,int damage,int ammo,String region,int armor,int cover) { }
    static final class Diagnostics {
        final EnumMap<Outcome,Long> counts=new EnumMap<>(Outcome.class);
        final ArrayDeque<Event> recent=new ArrayDeque<>();
        long damage,shots;
        long healingRecovered;
        final LinkedHashMap<String,Long> supportUses=new LinkedHashMap<>();
        String summary() { return "Actor combat "+counts+" damage="+damage+" shots="+shots+" healing="+healingRecovered+" supplies="+supportUses; }
    }
    private static final Map<GamePanel,Diagnostics> DIAGNOSTICS=Collections.synchronizedMap(new WeakHashMap<>());
    static Diagnostics diagnostics(GamePanel game) { return DIAGNOSTICS.computeIfAbsent(game,g->new Diagnostics()); }
    static void resetDiagnostics(GamePanel game) { DIAGNOSTICS.remove(game);ActorCombatFeedbackAuthority.clear(game); }
    private ActorCombatAuthority() { }

    static boolean alive(NpcEntity npc) { return npc!=null && npc.hp>0; }
    static boolean eligible(NpcEntity npc) { return alive(npc) && !npc.isUntargetableAnchor() && !npc.isMinorActor(); }
    static boolean enemies(NpcEntity a,NpcEntity b) {
        if(a==b || !eligible(a) || !eligible(b)) return false;
        if(!a.combatTeam.isBlank() || !b.combatTeam.isBlank()) return !a.combatTeam.isBlank() && !b.combatTeam.isBlank() && !a.combatTeam.equals(b.combatTeam);
        if(b.id!=null && b.id.equals(a.combatTargetId) || a.id!=null && a.id.equals(b.combatTargetId)) return true;
        return !FactionIdentityAuthority.sameFamily(a.faction,b.faction)
                && a.faction!=Faction.NONE && b.faction!=Faction.NONE
                && ("Hostile".equalsIgnoreCase(a.state) || "Hostile".equalsIgnoreCase(b.state));
    }
    static boolean takeNpcTurn(GamePanel game,NpcEntity actor) {
        if(!eligible(actor)) return false;
        ActorConsumableAuthority.ensure(actor);
        NpcEntity target=null;int best=Integer.MAX_VALUE;
        for(NpcEntity other:game.world.npcs) if(enemies(actor,other)) {
            int distance=distance(actor.x,actor.y,other.x,other.y);
            if(distance<=NpcTurnBudgetScheduler.ACTIVE_RADIUS && (distance<best || distance==best && other.id.compareTo(target.id)<0)) {target=other;best=distance;}
        }
        boolean playerEnemy=playerAlive(game) && actor.combatTeam.isBlank() && ("Hostile".equalsIgnoreCase(actor.state) || "PLAYER".equals(actor.combatTargetId));
        int playerDistance=distance(actor.x,actor.y,game.playerX,game.playerY);
        boolean player=playerEnemy && playerDistance<=12 && playerDistance<best;
        if(target==null && !player) {
            if(!actor.combatTeam.isBlank()) {actor.lastTickTurn=game.turn;event(game,actor.id,"","",Outcome.NO_TARGET,0,0,0,0);return true;}
            return false;
        }
        if(actor.lastTickTurn==game.turn) return true;
        actor.updateNeeds(game.turn,actor.lastTickTurn);
        if(ActorConsumableAuthority.autoUse(game,actor,true)) return true;
        actor.lastTickTurn=game.turn;
        actor.activeMotionStateIndex=GamePanel.MOTION_STATIONARY;
        actor.selectedMovementModeIndex=actor.chooseMovementModeIndex(game.turn);
        int tx=player?game.playerX:target.x,ty=player?game.playerY:target.y;
        String targetId=player?"PLAYER":target.id;
        int d=distance(actor.x,actor.y,tx,ty);
        String weapon=d<=1?melee(actor):actor.equippedRangedWeapon;
        if(weapon==null || weapon.isBlank()) weapon=melee(actor);
        var profile=StructuralScaleCombatAuthority.weaponProfile(weapon,"SNAP");
        if(!actor.isAnimalActor() && MedicalTreatmentAuthority.hands(actor)<MedicalTreatmentAuthority.requiredHands(profile)) {
            if(MedicalTreatmentAuthority.hands(actor)==0) {event(game,actor.id,targetId,weapon,Outcome.DISABLED,0,0,0,0);return true;}
            weapon=melee(actor);profile=StructuralScaleCombatAuthority.weaponProfile(weapon,"SNAP");
        }
        boolean line=StructuralScaleCombatAuthority.clearLine(game.world,actor.x,actor.y,tx,ty);
        if(d>profile.range() || !line) {
            event(game,actor.id,targetId,weapon,line?Outcome.RANGE:Outcome.WALL,0,0,0,0);
            int x=actor.x,y=actor.y;stepCombat(game,actor,tx,ty,profile);
            if(x!=actor.x || y!=actor.y) event(game,actor.id,targetId,weapon,Outcome.MOVE,0,0,0,0);
            return true;
        }
        if(profile.firearm() && actor.loadedShots<=0) {
            if(actor.ammoReloadsRemaining>0) {
                actor.ammoReloadsRemaining--;actor.loadedShots=actor.magazineCapacityLocal(weapon);
                WorldPositionalSoundAuthority.play(game,"weapon_reload",actor.x,actor.y,.7);
                event(game,actor.id,targetId,weapon,Outcome.RELOAD,0,0,0,0);
            } else {
                event(game,actor.id,targetId,weapon,Outcome.EMPTY,0,0,0,0);
                int x=actor.x,y=actor.y;stepCombat(game,actor,tx,ty,StructuralScaleCombatAuthority.weaponProfile(melee(actor),"SNAP"));
                if(x!=actor.x || y!=actor.y) event(game,actor.id,targetId,weapon,Outcome.MOVE,0,0,0,0);
            }
            return true;
        }
        if(profile.firearm() && occupiedLine(game,actor,tx,ty)) {event(game,actor.id,targetId,weapon,Outcome.ALLY_IN_LINE,0,0,0,0);int x=actor.x,y=actor.y;stepCombat(game,actor,tx,ty,profile);if(x!=actor.x || y!=actor.y) event(game,actor.id,targetId,weapon,Outcome.MOVE,0,0,0,0);return true;}
        int skill=ActorConsumableAuthority.skill(game,actor,profile.firearm(),profile.firearm()?actor.firearmsSkill:actor.meleeSkill);
        int quality=ItemQuality.tierIndex(weapon);
        int defense=(player?game.stat("Agility",5):target.meleeSkill)+ActorConsumableAuthority.bonus(game,player?null:target,true);
        int chance=hitChance(skill,defense,d,quality,cover(game,tx,ty));
        int roll=game.rng.nextInt(100)+1,ammo=profile.firearm()?1:0;
        actor.loadedShots-=ammo;
        actor.facingDx=Integer.compare(tx,actor.x);actor.facingDy=Integer.compare(ty,actor.y);
        ActorCombatFeedbackAuthority.attack(game,actor.x,actor.y,tx,ty,weapon,profile.firearm(),roll<=chance);
        if(roll>chance) {event(game,actor.id,targetId,weapon,Outcome.MISS,chance,roll,0,ammo);return true;}
        String partName=ActorArmorAuthority.hitPart(game.rng);
        int armor=player?ActorArmorAuthority.player(game,partName):ActorArmorAuthority.npc(target,partName);
        int damage=Math.max(1,damage(profile,skill,quality,armor)+ActorConsumableAuthority.bonus(game,actor,false));
        if(player) {
            BodyPart part=game.active==null?null:game.active.body.get(partName);
            if(CyberneticLimbAuthority.covering(game.combatSupport,partName)!=null) CyberneticLimbAuthority.damage(game,null,partName,damage);
            else if(part!=null) part.health=Math.max(0,part.health-damage*10.0);
            game.wounds++;game.pain+=damage;
            if(part!=null && part.destroyed() && (partName.equals("Head") || partName.equals("Torso"))) game.triggerPlayerDeath("combat trauma",actor.name,weapon,game.playerX+","+game.playerY);
            game.logEvent(actor.name+" hits your "+partName+" with "+weapon+" for "+damage+" trauma (armor "+armor+").");
            event(game,actor.id,targetId,weapon,part!=null && part.destroyed() && (partName.equals("Head") || partName.equals("Torso"))?Outcome.KILL:Outcome.HIT,chance,roll,damage,ammo,partName,armor);
        } else hurtNpc(game,actor.id,target,weapon,chance,roll,damage,ammo,partName,armor);
        return true;
    }

    static Event confirmPlayer(GamePanel game) {
        if(game==null || game.world==null) return null;
        if(!playerAlive(game)) return refusal(game,"","",Outcome.DEAD,"You cannot attack with a destroyed vital body region.");
        NpcEntity target=game.world.npcAt(game.combatX,game.combatY);
        String weapon=game.activeWeaponName();var profile=StructuralScaleCombatAuthority.weaponProfile(weapon,game.fireModeLabel());
        if(target==null) return refusal(game,"",weapon,Outcome.NO_TARGET,"No living actor at the selected target.");
        if(!eligible(target)) return refusal(game,target.id,weapon,Outcome.PROTECTED,"Target is a protected noncombatant.");
        String bodyRefusal=MedicalTreatmentAuthority.attackRejection(game,profile);
        if(!bodyRefusal.isBlank()) return refusal(game,target.id,weapon,Outcome.DISABLED,bodyRefusal);
        int d=distance(game.playerX,game.playerY,target.x,target.y);
        if(d>profile.range()) return refusal(game,target.id,weapon,Outcome.RANGE,"Target outside weapon range ("+d+"/"+profile.range()+").");
        if(!StructuralScaleCombatAuthority.clearLine(game.world,game.playerX,game.playerY,target.x,target.y)) return refusal(game,target.id,weapon,Outcome.WALL,"Attack blocked by a wall or closed door.");
        int required=profile.firearm() && "BURST".equalsIgnoreCase(game.fireModeLabel())?3:profile.firearm()?1:0;
        int loaded=game.loadedWeaponShots.getOrDefault(weapon,0);
        if(loaded<required) return refusal(game,target.id,weapon,Outcome.EMPTY,"Weapon needs reloading: "+loaded+"/"+required+" shots.");
        if(profile.firearm() && occupiedLine(game,null,target.x,target.y)) return refusal(game,target.id,weapon,Outcome.ALLY_IN_LINE,"Another actor blocks the firing lane.");
        int skill=ActorConsumableAuthority.skill(game,null,profile.firearm(),game.stat(profile.firearm()?"Firearms":"Melee",5));
        int quality=ItemQuality.tierIndex(weapon);
        int chance=hitChance(skill,target.meleeSkill+ActorConsumableAuthority.bonus(game,target,true),d,quality,cover(game,target.x,target.y));
        int roll=game.rng.nextInt(100)+1;
        if(required>0) game.loadedWeaponShots.put(weapon,loaded-required);
        target.combatTargetId="PLAYER";target.state="Hostile";
        Event result;
        if(roll>chance) result=event(game,"PLAYER",target.id,weapon,Outcome.MISS,chance,roll,0,required);
        else {
            String part=ActorArmorAuthority.hitPart(game.rng);int armor=ActorArmorAuthority.npc(target,part);
            result=hurtNpc(game,"PLAYER",target,weapon,chance,roll,Math.max(1,damage(profile,skill,quality,armor)+ActorConsumableAuthority.bonus(game,null,false)),required,part,armor);
        }
        game.lastTargetingReport="Attack "+result.outcome()+": "+target.name+" / "+weapon+" / hit "+chance+"%, roll "+roll+" / damage "+result.damage()+" / ammo "+required+".";
        ActorCombatFeedbackAuthority.attack(game,game.playerX,game.playerY,target.x,target.y,weapon,profile.firearm(),roll<=chance);
        game.advanceTurn(game.lastTargetingReport);game.repaint();return result;
    }
    static int hitChance(int skill,int defense,int distance,int quality,int cover) {
        return Math.max(5,Math.min(95,40+Math.max(0,skill)*4-Math.max(0,defense)*2-Math.max(0,distance-1)*2+Math.max(0,quality)*2-cover));
    }
    private static int damage(StructuralScaleCombatAuthority.WeaponProfile weapon,int skill,int quality,int armor) {
        // Actor HP scale, using the shared weapon force profile; initial tuning, not balance certification.
        return Math.max(1,weapon.force()+Math.max(0,skill)/4+Math.max(0,quality)-Math.max(0,armor-weapon.penetration()/2));
    }
    private static int cover(GamePanel game,int x,int y) {
        int value=0;for(BaseObject object:game.baseObjects) if(object.integrity>0 && distance(x,y,object.x,object.y)<=1) value=Math.max(value,PassiveDefenseEffectsAuthority.coverPenalty(object));
        return Math.min(30,value*2);
    }
    private static String melee(NpcEntity npc) { return npc.equippedMeleeWeapon==null || npc.equippedMeleeWeapon.isBlank()?"Bare hands":npc.equippedMeleeWeapon; }
    static boolean playerAlive(GamePanel game) {return game.active!=null && (game.active.body.get("Torso")==null || !game.active.body.get("Torso").destroyed()) && (game.active.body.get("Head")==null || !game.active.body.get("Head").destroyed());}
    private static boolean occupiedLine(GamePanel game,NpcEntity shooter,int tx,int ty) {
        return occupiedLineFrom(game,shooter==null?game.playerX:shooter.x,shooter==null?game.playerY:shooter.y,tx,ty);
    }
    private static boolean occupiedLineFrom(GamePanel game,int x,int y,int tx,int ty) {
        int dx=Math.abs(tx-x),dy=Math.abs(ty-y),sx=x<tx?1:-1,sy=y<ty?1:-1,err=dx-dy;
        while(x!=tx || y!=ty) {int e=2*err;if(e>-dy){err-=dy;x+=sx;}if(e<dx){err+=dx;y+=sy;}if(x==tx && y==ty) break;if(game.world.npcAt(x,y)!=null || x==game.playerX && y==game.playerY) return true;}
        return false;
    }
    /** Bounded combat routing around occupied firing lanes; never pushes or walks through an actor. */
    static boolean stepCombat(GamePanel game,NpcEntity actor,int tx,int ty,StructuralScaleCombatAuthority.WeaponProfile weapon) {
        if(!CyberneticLimbAuthority.mobile(actor.combatSupport) || actor.developerStationary)return false;
        record Node(int x,int y,int firstX,int firstY) { }
        ArrayDeque<Node> queue=new ArrayDeque<>();Set<Long> seen=new HashSet<>();queue.add(new Node(actor.x,actor.y,actor.x,actor.y));
        seen.add(((long)actor.x<<32)^(actor.y&0xffffffffL));int visited=0;
        while(!queue.isEmpty() && visited++<512) {
            Node at=queue.removeFirst();int d=distance(at.x,at.y,tx,ty);
            if((at.x!=actor.x || at.y!=actor.y) && d>0 && d<=weapon.range() && StructuralScaleCombatAuthority.clearLine(game.world,at.x,at.y,tx,ty) && (!weapon.firearm() || !occupiedLineFrom(game,at.x,at.y,tx,ty))) {actor.moveTo(at.firstX,at.firstY);return true;}
            int dx=Integer.compare(tx,at.x),dy=Integer.compare(ty,at.y);
            int[][] directions=Math.abs(tx-at.x)>=Math.abs(ty-at.y)?new int[][]{{dx,0},{0,dy},{0,-1},{0,1},{-1,0},{1,0}}:new int[][]{{0,dy},{dx,0},{-1,0},{1,0},{0,-1},{0,1}};
            for(int[] direction:directions) {int nx=at.x+direction[0],ny=at.y+direction[1];if(nx==at.x && ny==at.y || distance(nx,ny,actor.x,actor.y)>12 || !actor.canNpcStep(game.world,nx,ny,game.playerX,game.playerY))continue;
                long key=((long)nx<<32)^(ny&0xffffffffL);if(!seen.add(key))continue;boolean first=at.x==actor.x && at.y==actor.y;queue.add(new Node(nx,ny,first?nx:at.firstX,first?ny:at.firstY));}
        }
        return false;
    }
    private static Event hurtNpc(GamePanel game,String attacker,NpcEntity target,String weapon,int chance,int roll,int amount,int ammo,String region,int armor) {
        ActorConsumableAuthority.ensure(target);
        CyberneticLimbAuthority.damage(game,target,region,amount);
        int applied=Math.min(target.hp,amount);target.hp=Math.max(0,target.hp-amount);
        target.combatSupport.pain+=applied;
        if(target.hp==0) {target.state="Dead";target.activeMotionStateIndex=GamePanel.MOTION_STATIONARY;if("PLAYER".equals(attacker)) game.runKills++;}
        else target.combatTargetId=attacker;
        game.world.dirtyVisionRevision++;
        return event(game,attacker,target.id,weapon,target.hp==0?Outcome.KILL:Outcome.HIT,chance,roll,applied,ammo,region,armor);
    }
    private static Event refusal(GamePanel game,String target,String weapon,Outcome outcome,String message) {
        game.lastTargetingReport=message;game.logEvent(message);return event(game,"PLAYER",target,weapon,outcome,0,0,0,0);
    }
    private static Event event(GamePanel game,String attacker,String target,String weapon,Outcome outcome,int chance,int roll,int damage,int ammo) {
        return event(game,attacker,target,weapon,outcome,chance,roll,damage,ammo,"",0);
    }
    static void supportEvent(GamePanel game,String actor,String detail) {
        if(detail.startsWith("USE ") && detail.contains(" heal=")) {
            String item=detail.substring(4,detail.indexOf(" heal="));Diagnostics d=diagnostics(game);
            if(d.supportUses.size()>=256 && !d.supportUses.containsKey(item)) item="other supplies";
            d.supportUses.merge(item,1L,Long::sum);
        }
        event(game,actor,actor,detail,Outcome.SUPPORT,0,0,0,0);
    }
    private static Event event(GamePanel game,String attacker,String target,String weapon,Outcome outcome,int chance,int roll,int damage,int ammo,String region,int armor) {
        int appliedCover=0;
        if(outcome==Outcome.HIT || outcome==Outcome.KILL || outcome==Outcome.MISS) {
            if("PLAYER".equals(target)) appliedCover=cover(game,game.playerX,game.playerY);
            else for(NpcEntity npc:game.world.npcs) if(npc.id.equals(target)){appliedCover=cover(game,npc.x,npc.y);break;}
        }
        Event event=new Event(game.turn,attacker,target,weapon,outcome,chance,roll,damage,ammo,region,armor,appliedCover);Diagnostics d=diagnostics(game);
        d.counts.merge(outcome,1L,Long::sum);d.damage+=damage;d.shots+=ammo;d.recent.addLast(event);while(d.recent.size()>256)d.recent.removeFirst();
        if(Boolean.getBoolean("mechanist.combat.trace")) DebugLog.audit("ACTOR_COMBAT",event.toString());
        return event;
    }
    static int distance(int x,int y,int tx,int ty) { return Math.max(Math.abs(tx-x),Math.abs(ty-y)); }
}
