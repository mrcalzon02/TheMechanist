package mechanist;

import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Treatments must not silently become regeneration or mechanical repair. */
final class MedicalTreatmentBoundarySmoke {
    private static int checks;
    public static void main(String[] args) throws Exception {
        GamePanel campaign=new GamePanel();campaign.timer.stop();campaign.world=new World(19191,32,24);
        campaign.world.zoneType=ZoneType.NEUTRAL_CIVILIAN_FLOOR;for(char[] column:campaign.world.tiles) Arrays.fill(column,'.');
        campaign.active=Candidate.random(new Random(3));campaign.seed=19191;campaign.singlePlayerSectorBridge=new SinglePlayerSectorRuntimeBridge(new SectorManager());
        DeveloperFacilityAuthority.Session session=null;
        try {
            session=DeveloperFacilityAuthority.enter(campaign);GamePanel game=session.facility();game.timer.stop();game.options.soundEnabled=false;game.options.musicEnabled=false;
            DeveloperFacilityAuthority.clearArena(game);game.inventory.clear();game.combatSupport.restore("");game.active=Candidate.random(new Random(3));
            MedicalTreatmentAuthority.sever(game.active,"R Upper Arm");
            BodyPart arm=game.active.body.get("R Upper Arm"),hand=game.active.body.get("R Hand"),torso=game.active.body.get("Torso");
            require(arm.missing && hand.missing,"Loss must include downstream parts.");
            game.bleeding=4;game.pain=5;game.wounds=3;double body=health(game);
            dose(game,"Archeotech Bandage roll");
            require(health(game)==body && game.bleeding<4 && arm.missing && arm.health==0,"Bandaging controls bleeding without regrowing even at highest quality.");
            dose(game,"Splint kit");require(arm.missing && arm.health==0,"Splints cannot reconstruct missing limbs.");
            torso.health=torso.maxHealth()*0.25;body=health(game);dose(game,"Medkit");
            require(health(game)>body && arm.missing && hand.missing,"Medkit heals remaining tissue, never the absent arm.");
            for(BodyPart part:game.active.body.values()) if(!part.missing) part.health=part.maxHealth();
            BodyPart destroyed=game.active.body.get("L Lower Arm");destroyed.health=0;
            BodyPart cybernetic=game.active.body.get("L Upper Arm");cybernetic.cybernetic=true;cybernetic.health=cybernetic.maxHealth()*0.25;
            double machineHealth=cybernetic.health;game.pain=6;dose(game,"Archeotech Medkit");
            require(destroyed.health==0 && !destroyed.missing,"Destroyed attached parts cannot be revived by ordinary medicine.");
            require(cybernetic.health==machineHealth && cybernetic.cybernetic,"Biological medicine cannot repair cybernetic tissue.");
            Properties bodySave=new Properties();Persistence.writeCandidate(game.active,bodySave);Candidate restored=Persistence.readCandidate(bodySave,1);
            require(restored.body.get("R Upper Arm").missing && restored.body.get("R Hand").health==0,"Loading must not regrow a severed limb.");
            require(restored.body.get("L Lower Arm").health==0 && restored.body.get("L Upper Arm").cybernetic && restored.body.get("L Upper Arm").health==machineHealth,"Destroyed/mechanical injury state must persist.");
            require(restored.copy().body.get("R Hand").missing,"Character copying must preserve limb loss.");
            require(!MedicalTreatmentAuthority.functionalHand(restored,"R") && !MedicalTreatmentAuthority.functionalHand(restored,"L"),"Destroyed parents must disable downstream hands.");
            NpcEntity target=NpcEntity.create(Faction.CULTIST,ZoneType.GANGER_TURF,game.playerX+1,game.playerY,new Random(9));target.id="DISABLED-ARM-TARGET";target.role="Combatant";target.ageYears=35;target.ageBand="adult";game.world.npcs.add(target);
            game.equippedRightHandItem="Stub pistol";game.activeWeaponHandIndex=1;game.loadedWeaponShots.put("Stub pistol",8);game.combatX=target.x;game.combatY=target.y;
            long actionTurn=game.worldTurn;int targetHp=target.hp;
            require(ActorCombatAuthority.confirmPlayer(game).outcome()==ActorCombatAuthority.Outcome.DISABLED && game.loadedWeaponShots.get("Stub pistol")==8 && game.worldTurn==actionTurn && target.hp==targetHp,"Destroyed holding arms must block actual attacks without spending ammo or time.");
            game.world.npcs.remove(target);

            game.active=Candidate.random(new Random(5));game.combatSupport.restore("");game.inventory.clear();game.pain=game.wounds=game.bleeding=0;
            torso=game.active.body.get("Torso");torso.health=torso.maxHealth()*0.25;double initial=torso.health;
            dose(game,"Stitchdream");require(torso.health==initial && !game.combatSupport.recoveries.isEmpty(),"Recovery-over-time must not be an instant heal.");
            game.inventory.add("Pale Nurse");long clock=game.worldTurn;int stock=game.inventory.size();
            require(!ActorConsumableAuthority.usePlayer(game,"Pale Nurse") && game.worldTurn==clock && game.inventory.size()==stock,"Recovery courses must not stack or consume rejected doses.");
            game.advanceTurnBody("");require(torso.health==initial+10,"First recovery pulse occurs on a later turn.");
            Properties mid=new Properties();Persistence.writeCandidate(game.active,mid);String course=game.combatSupport.save();
            game.active=Persistence.readCandidate(mid,999);game.combatSupport.restore(course);torso=game.active.body.get("Torso");double afterPulse=torso.health;
            ActorConsumableAuthority.tick(game);require(torso.health==afterPulse,"Reloading/ticking the same clock must not duplicate a recovery pulse.");
            for(int i=0;i<12 && !game.combatSupport.recoveries.isEmpty();i++) game.advanceTurnBody("");
            require(torso.health==initial+60 && game.combatSupport.recoveries.isEmpty(),"Course budget is finite and survives mid-course save/load.");
            game.advanceTurnBody("");require(torso.health==initial+60,"Completed recovery must stop.");
            torso.health=torso.maxHealth()*0.25;initial=torso.health;dose(game,"Pale Nurse");
            game.worldTurn+=20;ActorConsumableAuthority.tick(game);
            require(torso.health==initial+50 && game.combatSupport.recoveries.isEmpty(),"Elapsed world time must settle all due pulses, not restart the course.");
            ActorConsumableAuthority.tick(game);require(torso.health==initial+50,"Catch-up healing must not repeat at the same clock.");

            NpcEntity npc=NpcEntity.create(Faction.CULTIST,ZoneType.GANGER_TURF,28,32,new Random(5));npc.id="MEDICAL-BOUNDARY";npc.role="Combatant";npc.combatTeam="A";npc.hp=10;npc.combatSupport.maximumHp=30;npc.combatSupport.items.clear();
            MedicalTreatmentAuthority.sever(npc,"R Upper Arm");npc.combatSupport.items.add("Stitchdream");game.world.npcs.add(npc);
            require(ActorConsumableAuthority.useNpc(game,npc,"Stitchdream") && npc.hp==10,"NPC recovery is also deferred.");
            NpcEntity loaded=NpcEntity.parseLine(npc.saveLine(),game.world);
            require(loaded.combatSupport.lostParts.contains("R Hand") && !loaded.combatSupport.recoveries.isEmpty(),"NPC loss and scheduled recovery persist independently.");
            require(MedicalTreatmentAuthority.hands(loaded)==1,"NPC loss must remove the lost arm's weapon capability.");
            for(int i=0;i<9;i++) game.advanceTurnBody("");
            require(npc.hp==16 && npc.combatSupport.lostParts.contains("R Upper Arm"),"Restoring NPC vital HP must not erase lost-limb markers.");
            npc.faction=Faction.ROGUE_MACHINE;npc.combatSupport.items.add("Medkit");npc.lastTickTurn=-1;
            require(!ActorConsumableAuthority.useNpc(game,npc,"Medkit") && npc.combatSupport.items.contains("Medkit"),"Organic treatments do not repair machines.");
            require(MedicalTreatmentAuthority.category("Bandage roll")==MedicalTreatmentAuthority.Category.STABILIZATION,"Bandage category.");
            require(MedicalTreatmentAuthority.category("Medkit")==MedicalTreatmentAuthority.Category.INSTANT_RECOVERY,"Instant category.");
            require(MedicalTreatmentAuthority.category("Stitchdream")==MedicalTreatmentAuthority.Category.RECOVERY_OVER_TIME,"Timed recovery category.");
            require(MedicalTreatmentAuthority.category("Black Sun Draught")==MedicalTreatmentAuthority.Category.REGENERATION && !ActorConsumableAuthority.supported("Black Sun Draught"),"Draught regeneration remains reserved, not a fabricated ordinary heal.");
            String result="MedicalTreatmentBoundarySmoke PASS checks="+checks+" stabilization=true missing-limbs=true destroyed-parts=true no-cybernetic-healing=true overtime=true finite=true body-save=true npc=true draught-reserved=true";
            if(args.length>0) {Path folder=Path.of(args[0]);Files.createDirectories(folder);Files.writeString(folder.resolve("medical-boundaries.txt"),result+"\n");}
            System.out.println(result);
        } finally {if(session!=null){DeveloperFacilityAuthority.leave(session.facility());SwingUtilities.invokeAndWait(()->{});}campaign.singlePlayerSectorBridge.close();campaign.shutdownRuntime();}
    }
    private static void dose(GamePanel game,String item) {game.inventory.add(item);require(ActorConsumableAuthority.usePlayer(game,item),"Expected usable dose: "+item);}
    private static double health(GamePanel game) {return game.active.body.values().stream().mapToDouble(p->p.health).sum();}
    private static void require(boolean condition,String message) {checks++;if(!condition) throw new AssertionError(message);}
}
