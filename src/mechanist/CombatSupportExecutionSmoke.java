package mechanist;

import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Registry-wide use/coverage checks and real actor attack comparisons. */
final class CombatSupportExecutionSmoke {
    private static int checks;
    private static final StringBuilder AUDIT=new StringBuilder("kind\titem\tquality\tplayer\tnpc\tdetail\n");
    private static final class HitRandom extends Random {
        private final int region;private int draws;
        HitRandom(int region) {this.region=region;}
        @Override public int nextInt(int bound) {return draws++==0?0:Math.floorMod(region,bound);}
    }
    public static void main(String[] args) throws Exception {
        Path output=Path.of(args.length==0?"diagnostics/combat-support-20261026":args[0]);Files.createDirectories(output);
        GamePanel campaign=new GamePanel();campaign.timer.stop();campaign.world=new World(19191L,32,24);
        campaign.world.zoneType=ZoneType.NEUTRAL_CIVILIAN_FLOOR;for(char[] column:campaign.world.tiles) Arrays.fill(column,'.');
        campaign.active=Candidate.random(new Random(19191L));campaign.seed=19191L;campaign.playerX=4;campaign.playerY=5;
        campaign.singlePlayerSectorBridge=new SinglePlayerSectorRuntimeBridge(new SectorManager());
        DeveloperFacilityAuthority.Session session=null;
        try {
            session=DeveloperFacilityAuthority.enter(campaign);GamePanel game=session.facility();game.timer.stop();game.options.soundEnabled=false;game.options.musicEnabled=false;
            DeveloperFacilityAuthority.clearArena(game);
            List<String> medical=ItemCatalog.ITEMS.values().stream().filter(d->ActorConsumableAuthority.supported(d.name)).map(d->d.name).toList();
            for(ItemDef definition:ItemCatalog.ITEMS.values()) if(definition.category.equals("medical") || definition.category.equals("medical/chem") || definition.category.equals("stimulant") || definition.category.equals("chem/ganger-combat"))
                require(medical.contains(definition.name),"Missing medical/combat drug profile: "+definition.name);
            for(String name:medical) for(int tier=0;tier<ItemQuality.NAMES.length;tier++) {
                resetPlayer(game);String item=tier==2?name:ItemQuality.NAMES[tier]+" "+name;
                game.inventory.add(item);game.inventory.add(item);game.selectedInventoryIndex=0;
                for(BodyPart part:game.active.body.values()) part.health=part.maxHealth()*0.25;
                game.wounds=3;game.bleeding=3;game.infectionRisk=3;game.pain=8;game.fatigue=15;game.sleepNeed=12;
                double health=bodyHealth(game);long turn=game.worldTurn;var profile=ActorConsumableAuthority.profile(item);
                game.useSelectedInventoryItemBody();
                require(game.inventory.size()==1 && game.worldTurn==turn+1,"One actual player dose and action: "+item);
                require(bodyHealth(game)>=health,"Treatment must not lower body health: "+item);
                if(profile.heal()>0) require(MedicalTreatmentAuthority.category(item)==MedicalTreatmentAuthority.Category.RECOVERY_OVER_TIME?bodyHealth(game)==health && !game.combatSupport.recoveries.isEmpty():bodyHealth(game)>health,"Body recovery schedule/execution missing: "+item);
                if(profile.bleeding()>0) require(game.bleeding<3,"Bleeding treatment missing: "+item);
                if(profile.infection()>0) require(game.infectionRisk<3,"Infection treatment missing: "+item);
                if(ItemCatalog.get(item).category.equals("stimulant")) require(game.sleepNeed<12,"Legacy stimulant must reduce actual sleep need: "+item);
                require(game.active.body.values().stream().allMatch(p->p.health<=p.maxHealth()),"No player overheal: "+item);
                NpcEntity npc=actor("USE-NPC",30,28,32);npc.hp=8;npc.combatSupport.pain=8;npc.combatSupport.bleeding=3;npc.combatSupport.infection=3;npc.combatSupport.fatigue=15;
                npc.combatSupport.items.add(item);npc.combatSupport.items.add(item);npc.sleepDebt=12;
                require(ActorConsumableAuthority.useNpc(game,npc,item),"NPC must use supported dose: "+item);
                require(npc.combatSupport.items.size()==1 && npc.lastTickTurn==game.turn,"NPC dose and action must be finite: "+item);
                if(profile.heal()>0) require(MedicalTreatmentAuthority.category(item)==MedicalTreatmentAuthority.Category.RECOVERY_OVER_TIME?npc.hp==8 && !npc.combatSupport.recoveries.isEmpty():npc.hp>8,"NPC health recovery schedule/execution missing: "+item);
                if(ItemCatalog.get(item).category.equals("stimulant")) require(npc.sleepDebt<12,"NPC stimulant must reduce actual sleep debt: "+item);
                require(npc.hp<=30,"NPC may not exceed recorded maximum HP: "+item);
                NpcEntity restored=NpcEntity.parseLine(npc.saveLine(),game.world);
                require(restored.combatSupport.save().equals(npc.combatSupport.save()),"NPC supplies/effects/condition persistence: "+item);
                if(profile.duration()>0) {
                    int count=game.inventory.size();turn=game.worldTurn;
                    require(!ActorConsumableAuthority.usePlayer(game,item) && game.inventory.size()==count && game.worldTurn==turn,"Same-family dose must refuse atomically: "+item);
                    npc.lastTickTurn=-1;require(!ActorConsumableAuthority.useNpc(game,npc,item) && npc.combatSupport.items.size()==1,"NPC same-family dose must refuse atomically: "+item);
                }
                AUDIT.append("dose\t").append(name).append('\t').append(ItemQuality.NAMES[tier]).append("\tPASS\tPASS\t").append(ActorConsumableAuthority.description(item)).append('\n');
            }
            List<String> armor=ItemCatalog.ITEMS.values().stream().filter(d->ActorArmorAuthority.wearable(d.name)).map(d->d.name).toList();
            for(String name:armor) for(int tier=0;tier<8;tier++) {
                resetPlayer(game);String item=tier==2?name:ItemQuality.NAMES[tier]+" "+name;
                game.inventory.add(item);game.selectedInventoryIndex=0;game.useSelectedInventoryItemBody();
                NpcEntity npc=actor("ARMOR-NPC",30,28,32);npc.equippedArmor="";npc.combatSupport.items.add(item);
                require(ActorConsumableAuthority.equipNpc(game,npc,item),"NPC armor equip: "+item);
                require(!npc.combatSupport.items.contains(item),"Worn armor leaves carried list: "+item);
                for(String region:List.of("Head","Torso","L Upper Arm","L Upper Leg")) require(ActorArmorAuthority.player(game,region)==ActorArmorAuthority.npc(npc,region),"Player/NPC armor mismatch: "+item+" / "+region);
                NpcEntity restored=NpcEntity.parseLine(npc.saveLine(),game.world);
                require(restored.combatSupport.worn.equals(npc.combatSupport.worn) && restored.equippedArmor.equals(npc.equippedArmor),"Worn NPC armor must persist: "+item);
                AUDIT.append("armor\t").append(name).append('\t').append(ItemQuality.NAMES[tier]).append("\tPASS\tPASS\trating ").append(ActorArmorAuthority.rating(item)).append('\n');
            }
            verifyEffects(game,campaign);
            require(!ActorArmorAuthority.coversPart("Shock gauntlets","L Upper Arm") && ActorArmorAuthority.coversPart("Shock gauntlets","R Hand"),"Gauntlets must not protect upper arms.");
            require(!ActorArmorAuthority.coversPart("Armored boots","L Upper Leg") && ActorArmorAuthority.coversPart("Armored boots","R Foot"),"Boots must not protect thighs.");
            require(!ActorArmorAuthority.coversPart("Guard flak helmet","Torso"),"Head protection must not reduce torso damage.");
            verifyPhysicalConsumption(game);
            verifyAutomaticUse(game);
            StringBuilder comparisons=new StringBuilder("actor\tweapon\tregionRoll\tbareDamage\tarmoredDamage\tarmor\tstatus\n");
            for(int region:new int[]{5,30,75,95}) {
                String item=region==5?"Guard flak helmet":region==30?"Guard flak vest":region==75?"Shock gauntlets":"Armored boots";
                for(boolean playerAttacker:new boolean[]{true,false}) {
                    var bare=shot(game,playerAttacker,"Knife","",region,false);
                    var protectedHit=shot(game,playerAttacker,"Knife",item,region,false);
                    require(protectedHit.armor()>bare.armor() && protectedHit.damage()<bare.damage(),"Worn protection must reduce actual "+bare.region()+" damage for both actor types.");
                    require(protectedHit.region().equals(bare.region()),"Compared hits need identical locations.");
                    comparisons.append(playerAttacker?"NPC defender":"player defender").append("\tKnife\t").append(region).append('\t').append(bare.damage()).append('\t').append(protectedHit.damage()).append('\t').append(item).append("\tPASS\n");
                }
                var playerHit=shot(game,true,"Knife",item,region,false);var npcHit=shot(game,false,"Knife",item,region,false);
                require(playerHit.damage()==npcHit.damage() && playerHit.chance()==npcHit.chance() && playerHit.armor()==npcHit.armor(),"Equal gear, skill, quality and region must produce equal player/NPC attack effects.");
            }
            var baseline=shot(game,true,"Stub pistol","",30,false);var boosted=shot(game,true,"Stub pistol","",30,true);
            require(boosted.chance()>baseline.chance() && boosted.damage()>baseline.damage(),"Player stim must improve actual attack resolution.");
            baseline=shot(game,false,"Stub pistol","",30,false);boosted=shot(game,false,"Stub pistol","",30,true);
            require(boosted.chance()>baseline.chance() && boosted.damage()>baseline.damage(),"NPC stim must improve actual attack resolution.");
            Files.writeString(output.resolve("item-quality-audit.tsv"),AUDIT);
            Files.writeString(output.resolve("actual-armor-comparisons.tsv"),comparisons);
            Files.writeString(output.resolve("summary.json"),"{\"checks\":"+checks+",\"consumableProfiles\":"+medical.size()+",\"armorItems\":"+armor.size()+",\"qualities\":8,\"actorTypes\":2,\"status\":\"PASS\"}\n");
            System.out.println("CombatSupportExecutionSmoke PASS checks="+checks+" profiles="+medical.size()+" armor="+armor.size()+" qualities=8 player=true npc=true health=true actual-attacks=true finite=true expiry=true persistence=true");
        } finally {if(session!=null){DeveloperFacilityAuthority.leave(session.facility());SwingUtilities.invokeAndWait(()->{});}campaign.singlePlayerSectorBridge.close();campaign.shutdownRuntime();}
    }
    private static void verifyEffects(GamePanel game,GamePanel campaign) throws Exception {
        resetPlayer(game);game.inventory.add("Street Stimm");game.inventory.add("Slaught");game.selectedInventoryIndex=0;var stats=new LinkedHashMap<>(game.active.stats);
        require(ActorConsumableAuthority.usePlayer(game,"Street Stimm"),"Player stim activation.");long end=game.combatSupport.effects.get("COMBAT").expires();
        long turn=game.worldTurn;require(!ActorConsumableAuthority.usePlayer(game,"Slaught") && game.worldTurn==turn && game.inventory.contains("Slaught"),"Different named drugs in one family cannot stack.");
        campaign.combatSupport.restore(game.combatSupport.save());campaign.turn=game.turn;campaign.worldTurn=game.worldTurn;Properties saved=new Properties();Persistence.writeCore(campaign,saved);
        var restored=new ActorConsumableAuthority.State();restored.restore(saved.getProperty("run.combatSupport"));
        require(restored.save().equals(game.combatSupport.save()),"Player effects use the real save property.");
        while(game.worldTurn<end) game.advanceTurnBody("");
        require(game.combatSupport.effects.isEmpty() && game.fatigue==4 && game.pain==2,"Effect must expire at its deadline and apply one crash.");
        game.advanceTurnBody("");require(game.fatigue==4 && game.pain==2 && game.active.stats.equals(stats),"No repeated crash or permanent stat mutation.");
        NpcEntity npc=actor("EXPIRY-NPC",30,28,32);npc.combatTeam="A";npc.combatSupport.items.add("Street Stimm");game.world.npcs.add(npc);
        require(ActorConsumableAuthority.useNpc(game,npc,"Street Stimm"),"NPC stim activation.");end=npc.combatSupport.effects.get("COMBAT").expires();
        while(game.worldTurn<end) game.advanceTurnBody("");require(npc.combatSupport.effects.isEmpty() && npc.combatSupport.fatigue==4 && npc.combatSupport.pain==2,"NPC expiry/crash parity.");
        resetPlayer(game);game.inventory.add("Medkit");turn=game.worldTurn;
        require(!ActorConsumableAuthority.usePlayer(game,"Medkit") && game.inventory.size()==1 && game.worldTurn==turn,"Useless treatment refusal is atomic.");
        game.active.body.get("Torso").health=0;require(!ActorConsumableAuthority.usePlayer(game,"Medkit") && game.inventory.size()==1,"Treatment must not resurrect a fatal injury.");
        resetPlayer(game);game.inventory.add("Obscura");game.selectedInventoryIndex=0;turn=game.worldTurn;game.useSelectedInventoryItemBody();
        require(game.inventory.size()==1 && game.worldTurn==turn,"Unsupported catalog narcotics must not pretend to be active combat medicine.");
    }
    private static void verifyPhysicalConsumption(GamePanel game) {
        resetPlayer(game);game.wounds=2;game.active.body.get("Torso").health=20;
        game.inventory.add("Medkit");game.addItemToContainerResult(GamePanel.CONTAINER_PLAYER_INVENTORY,"Player inventory","Medkit",null,null,"test supply");
        int instances=game.itemInstances.size();int units=game.itemContainers.get(GamePanel.CONTAINER_PLAYER_INVENTORY).itemInstanceIds.size();
        require(ActorConsumableAuthority.usePlayer(game,"Medkit"),"Physical medkit use.");
        require(game.inventory.isEmpty() && game.itemInstances.size()==instances-1 && game.itemContainers.get(GamePanel.CONTAINER_PLAYER_INVENTORY).itemInstanceIds.size()==units-1,"Consumption must remove one item instance and its legacy view.");
        NpcEntity legacy=actor("LEGACY-INJURY",12,28,32);legacy.hp=3;
        String[] fields=legacy.saveLine().split("\\|",-1);
        NpcEntity migrated=NpcEntity.parseLine(String.join("|",Arrays.copyOf(fields,57)),game.world);
        migrated.combatSupport.items.add("Medkit");migrated.lastTickTurn=-1;
        require(migrated.combatSupport.maximumHp==12 && ActorConsumableAuthority.useNpc(game,migrated,"Medkit") && migrated.hp>3 && migrated.hp<=12,"Legacy wounded NPCs need a spawn-profile healing cap, not their current injured HP.");
    }
    private static void verifyAutomaticUse(GamePanel game) {
        resetPlayer(game);DeveloperFacilityAuthority.layout(game).cyberneticPercent=0;DeveloperCombatScenarioAuthority.prepare(game,1,"rifles",71000);
        NpcEntity npc=game.world.npcs.stream().filter(n->"A".equals(n.combatTeam)).findFirst().orElseThrow();
        game.world.npcs.stream().filter(n->"B".equals(n.combatTeam)).forEach(n->{n.equippedRangedWeapon="";n.equippedMeleeWeapon="Knife";});
        npc.combatSupport.maximumHp=30;npc.hp=10;npc.combatSupport.items.add("Medkit");int loaded=npc.loadedShots;
        game.advanceTurnBody("");require(!npc.combatSupport.items.contains("Medkit") && npc.loadedShots==loaded,"Healing must consume the NPC's action instead of firing too.");
        DeveloperFacilityAuthority.layout(game).cyberneticPercent=0;DeveloperCombatScenarioAuthority.prepare(game,1,"rifles",71000);npc=game.world.npcs.stream().filter(n->"A".equals(n.combatTeam)).findFirst().orElseThrow();
        game.world.npcs.stream().filter(n->"B".equals(n.combatTeam)).forEach(n->{n.equippedRangedWeapon="";n.equippedMeleeWeapon="Knife";});
        npc.combatSupport.items.add("Street Stimm");loaded=npc.loadedShots;game.advanceTurnBody("");
        require(npc.combatSupport.effects.containsKey("COMBAT") && npc.combatSupport.items.isEmpty() && npc.loadedShots==loaded,"Automatic stim use is a finite, single action.");
    }
    private static ActorCombatAuthority.Event shot(GamePanel game,boolean playerAttacker,String weapon,String armor,int region,boolean stim) {
        resetPlayer(game);game.playerX=27;game.playerY=32;game.equippedRightHandItem=weapon;game.activeWeaponHandIndex=1;game.loadedWeaponShots.put(weapon,10);
        NpcEntity npc=actor("ATTACK-NPC",1000,28,32);npc.equippedArmor="";npc.equippedRangedWeapon=weapon;npc.equippedMeleeWeapon=weapon;npc.loadedShots=10;npc.ammoReloadsRemaining=0;
        game.world.npcs.add(npc);
        if(playerAttacker) {
            npc.combatTeam="B";if(!armor.isBlank()){npc.combatSupport.items.add(armor);require(ActorConsumableAuthority.equipNpc(game,npc,armor),"Compared NPC armor equip.");}
            if(stim){game.inventory.add("Street Stimm");require(ActorConsumableAuthority.usePlayer(game,"Street Stimm"),"Player attack stim dose.");}
            game.combatX=npc.x;game.combatY=npc.y;game.rng=new HitRandom(region);return ActorCombatAuthority.confirmPlayer(game);
        }
        if(!armor.isBlank()){game.inventory.add(armor);game.selectedInventoryIndex=0;game.useSelectedInventoryItemBody();}
        if(stim){npc.combatSupport.items.add("Street Stimm");require(ActorConsumableAuthority.useNpc(game,npc,"Street Stimm"),"NPC attack stim dose.");game.turn++;game.worldTurn++;}
        game.rng=new HitRandom(region);ActorCombatAuthority.takeNpcTurn(game,npc);
        return ActorCombatAuthority.diagnostics(game).recent.getLast();
    }
    private static NpcEntity actor(String id,int hp,int x,int y) {
        NpcEntity n=NpcEntity.create(Faction.CULTIST,ZoneType.GANGER_TURF,x,y,new Random(10));n.id=id;n.role="Combatant";n.state="Hostile";n.ageYears=35;n.ageBand="adult";n.hp=hp;n.combatSupport.maximumHp=hp;n.combatSupport.items.clear();return n;
    }
    private static void resetPlayer(GamePanel game) {
        DeveloperFacilityAuthority.clearArena(game);game.inventory.clear();game.equippedWearableSlots.clear();game.equippedClothing=null;game.combatSupport.restore("");
        game.active=Candidate.random(new Random(5));game.active.stats.put("Firearms",5);game.active.stats.put("Melee",5);game.active.stats.put("Agility",5);
        game.pain=game.fatigue=game.wounds=game.bleeding=game.infectionRisk=game.stimulantStrain=game.sleepNeed=0;game.selectedInventoryIndex=0;game.inventoryTargetColumnActive=false;game.playerX=27;game.playerY=32;
        game.equippedLeftHandItem="";game.equippedRightHandItem="";game.activeWeaponHandIndex=1;ActorCombatAuthority.resetDiagnostics(game);
    }
    private static double bodyHealth(GamePanel game) {return game.active.body.values().stream().mapToDouble(p->p.health).sum();}
    private static void require(boolean value,String message) {checks++;if(!value) throw new AssertionError(message);}
}
