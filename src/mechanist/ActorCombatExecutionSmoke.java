package mechanist;

import java.util.*;
import javax.swing.SwingUtilities;

/** Regression checks for execution, refusal atomicity, finite ammo and persisted combat state. */
final class ActorCombatExecutionSmoke {
    public static void main(String[] args) throws Exception {
        GamePanel campaign=new GamePanel();campaign.timer.stop();campaign.world=new World(19191,32,24);
        campaign.world.zoneType=ZoneType.NEUTRAL_CIVILIAN_FLOOR;for(char[] column:campaign.world.tiles) Arrays.fill(column,'.');
        campaign.active=Candidate.random(new Random(19191));campaign.seed=19191;campaign.playerX=4;campaign.playerY=5;
        campaign.singlePlayerSectorBridge=new SinglePlayerSectorRuntimeBridge(new SectorManager());
        DeveloperFacilityAuthority.Session session=null;
        try {
            session=DeveloperFacilityAuthority.enter(campaign);GamePanel game=session.facility();game.timer.stop();
            game.options.soundEnabled=false;game.options.musicEnabled=false;
            DeveloperFacilityAuthority.layout(game).cyberneticPercent=0;DeveloperCombatScenarioAuthority.prepare(game,1,"knives",71000);
            int turns=0;while(DeveloperCombatScenarioAuthority.result(game).endsWith("active") && turns++<100) game.advanceTurnBody("");
            require(!DeveloperCombatScenarioAuthority.result(game).endsWith("active"),"Adjacent NPC combat must resolve.");
            require(ActorCombatAuthority.diagnostics(game).counts.getOrDefault(ActorCombatAuthority.Outcome.KILL,0L)==1,"Exactly one 1v1 death is recorded.");
            NpcEntity dead=game.world.npcs.stream().filter(n->!n.combatTeam.isBlank() && n.hp==0).findFirst().orElseThrow();
            require(game.world.npcAt(dead.x,dead.y)!=dead,"Dead actors must not occupy living actor tiles.");
            int lastTick=dead.lastTickTurn;game.advanceTurnBody("");require(dead.lastTickTurn==lastTick,"Dead actors must not receive turns.");
            require(game.world.npcs.stream().filter(n->n.developerStationary).allMatch(n->n.hp==9999),"Pinned traders must remain unharmed.");

            DeveloperFacilityAuthority.clearArena(game);
            for(int y=34;y<60;y++) game.world.tiles[27][y]='.';
            NpcEntity target=NpcEntity.create(Faction.CULTIST,ZoneType.GANGER_TURF,28,32,new Random(2));
            target.id="SMOKE-TARGET";target.combatTeam="B";target.hp=30;target.ageYears=35;target.ageBand="adult";target.equippedArmor="";
            game.world.npcs.add(target);game.playerX=26;game.playerY=32;game.equippedLeftHandItem="";game.equippedRightHandItem="Light Rifle";game.activeWeaponHandIndex=1;
            game.loadedWeaponShots.put("Light Rifle",8);game.active.stats.put("Firearms",20);game.rng=new Random(1);
            target.x=50;int rangeTurn=game.turn;
            game.singlePlayerSectorBridge.submitCommand(game,new ConfirmCombatCommand(SinglePlayerSectorRuntimeBridge.LOCAL_PLAYER_ID,50,32));
            require(target.hp==30 && game.loadedWeaponShots.get("Light Rifle")==8 && game.turn==rangeTurn,"Range refusal must not spend health, ammo or time.");target.x=28;
            game.world.tiles[27][32]='#';game.world.tiles[26][33]='#';
            require(!StructuralScaleCombatAuthority.clearLine(game.world,26,32,27,33),"Two solid corner tiles must block diagonal attacks.");game.world.tiles[26][33]='.';
            game.world.tiles[27][32]='#';int beforeTurn=game.turn;
            game.singlePlayerSectorBridge.submitCommand(game,new ConfirmCombatCommand(SinglePlayerSectorRuntimeBridge.LOCAL_PLAYER_ID,28,32));
            require(game.combatX==28 && game.combatY==32,"Command must honor its supplied coordinates.");
            require(target.hp==30 && game.loadedWeaponShots.get("Light Rifle")==8 && game.turn==beforeTurn,"Wall refusals must not spend health, ammo or a turn.");
            require(ActorCombatAuthority.diagnostics(game).recent.getLast().outcome()==ActorCombatAuthority.Outcome.WALL,"Wall reason must be diagnostic.");
            game.world.tiles[27][32]='.';
            game.loadedWeaponShots.put("Light Rifle",0);game.singlePlayerSectorBridge.submitCommand(game,new ConfirmCombatCommand(SinglePlayerSectorRuntimeBridge.LOCAL_PLAYER_ID,28,32));
            require(target.hp==30 && game.turn==beforeTurn,"Empty attack must be atomic.");
            game.inventory.clear();game.reloadCurrentRangedWeapon();require(game.loadedWeaponShots.get("Light Rifle")==0 && game.turn==beforeTurn,"Reload must require actual carried ammunition.");
            require(LootDropSystemAuthority.ammoItemFor("Light Rifle").equals("Las charge pack"),"Ammo family must follow the catalog, not just weapon spelling.");
            game.inventory.add("Las charge pack");game.reloadCurrentRangedWeapon();
            require(game.loadedWeaponShots.get("Light Rifle")>0 && game.inventory.isEmpty(),"Reload must consume the carried ammo unit.");
            int kills=game.runKills;
            for(int i=0;i<8 && target.hp>0;i++) game.singlePlayerSectorBridge.submitCommand(game,new ConfirmCombatCommand(SinglePlayerSectorRuntimeBridge.LOCAL_PLAYER_ID,28,32));
            require(target.hp==0 && game.runKills==kills+1,"Player attacks must apply damage and award one kill.");
            require(game.loadedWeaponShots.get("Light Rifle")<new NpcEntity().magazineCapacityLocal("Light Rifle"),"Executed attacks must consume rounds.");

            NpcEntity depleted=NpcEntity.create(Faction.IMPERIAL_GUARD,ZoneType.GANGER_TURF,20,40,new Random(3));
            depleted.loadedShots=0;depleted.ammoReloadsRemaining=0;depleted.meleeSkill=3;depleted.firearmsSkill=12;
            depleted.equipmentSummary();require(depleted.loadedShots==0 && depleted.ammoReloadsRemaining==0,"Inspecting depleted gear must not create ammo.");
            NpcEntity restored=NpcEntity.parseLine(depleted.saveLine(),game.world);
            require(restored.loadedShots==0 && restored.ammoReloadsRemaining==0 && restored.meleeSkill==3 && restored.firearmsSkill==12,"Empty ammo and explicit skills must survive reload.");
            require(ActorCombatAuthority.hitChance(12,5,5,2,0)>ActorCombatAuthority.hitChance(2,5,5,2,0),"Skill must affect actual hit chances.");
            require(new NpcEntity().magazineCapacityLocal("Heavy stubber")==20 && ItemCatalog.get("Heavy stubber")==ItemCatalog.get("Heavy Stubber"),"Case variants must resolve the same weapon and magazine.");

            DeveloperFacilityAuthority.clearArena(game);game.playerX=26;game.playerY=32;
            NpcEntity retaliator=NpcEntity.create(Faction.CULTIST,ZoneType.GANGER_TURF,28,32,new Random(10));
            retaliator.id="RETALIATOR";retaliator.role="Combatant";retaliator.state="Hostile";retaliator.ageYears=35;retaliator.ageBand="adult";
            retaliator.firearmsSkill=20;retaliator.equippedRangedWeapon="Light Rifle";retaliator.loadedShots=1;game.world.npcs.add(retaliator);
            retaliator.combatSupport.items.clear();
            for(BodyPart part:game.active.body.values()) part.health=part.maxHealth();double health=game.active.body.values().stream().mapToDouble(p->p.health).sum();
            game.turn++;game.rng=new Random(1);ActorCombatAuthority.takeNpcTurn(game,retaliator);
            require(retaliator.loadedShots==0 && game.active.body.values().stream().mapToDouble(p->p.health).sum()<health && game.wounds>0,"Hostile NPC fire must consume ammunition and injure the actual player body.");
            for(BodyPart part:game.active.body.values()) part.health=part.maxHealth();game.wounds=0;game.pain=0;

            DeveloperFacilityAuthority.layout(game).cyberneticPercent=0;DeveloperCombatScenarioAuthority.prepare(game,1,"rifles",71003);
            var actors=game.world.npcs.stream().filter(n->!n.combatTeam.isBlank()).toList();
            NpcEntity shooter=actors.get(0),opponent=actors.get(1);shooter.loadedShots=0;shooter.ammoReloadsRemaining=1;
            game.turn++;ActorCombatAuthority.takeNpcTurn(game,shooter);
            require(shooter.ammoReloadsRemaining==0 && shooter.loadedShots>0,"NPC reload must consume one finite reserve and use a turn.");
            shooter.loadedShots=0;shooter.equipmentSummary();require(shooter.loadedShots==0 && shooter.ammoReloadsRemaining==0,"Inspection must not undo reserve exhaustion.");
            require(ActorCombatAuthority.enemies(shooter,opponent),"Explicit opposing test teams must engage.");opponent.combatTeam=shooter.combatTeam;
            require(!ActorCombatAuthority.enemies(shooter,opponent),"Same team must not be attacked.");
            NpcEntity vendor=game.world.npcs.stream().filter(n->n.developerStationary).findFirst().orElseThrow();
            game.combatX=vendor.x;game.combatY=vendor.y;game.playerX=vendor.x+1;game.playerY=vendor.y;
            require(ActorCombatAuthority.confirmPlayer(game).outcome()==ActorCombatAuthority.Outcome.PROTECTED,"Player must not damage protected test traders.");
            System.out.println("ActorCombatExecutionSmoke PASS npc-death=true player-damage=true command-coordinates=true walls=true empty-atomic=true finite-ammo=true persisted-skills=true pinned-traders=true");
        } finally {if(session!=null){DeveloperFacilityAuthority.leave(session.facility());SwingUtilities.invokeAndWait(()->{});}campaign.singlePlayerSectorBridge.close();campaign.shutdownRuntime();}
    }
    private static void require(boolean value,String message) {if(!value) throw new AssertionError(message);}
}
