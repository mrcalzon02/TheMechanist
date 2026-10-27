package mechanist;

import java.awt.Rectangle;
import java.util.*;
import javax.swing.Timer;

/** Controlled arena setup only; attacks execute through the normal turn authority. */
final class DeveloperCombatScenarioAuthority {
    static final String[] PRESETS={"knives","rifles","mixed","heavy","intellect","wall","skills","pursuit"};
    private static final Map<GamePanel,Timer> WATCHERS=new WeakHashMap<>();
    private DeveloperCombatScenarioAuthority() { }
    static boolean valid(int size,String preset) {return Set.of(1,5,10,20).contains(size) && Arrays.asList(PRESETS).contains(preset);}
    static void prepare(GamePanel game,int size,String preset,long seed) {
        stop(game);DeveloperFacilityAuthority.clearArena(game);
        Rectangle room=DeveloperFacilityAuthority.layout(game).rooms.get("combat");
        int cx=room.x+25,cy=room.y+13;
        for(int y=room.y+10;y<room.y+room.height-2;y++) game.world.tiles[cx][y]=preset.equals("wall")?'#':'.';
        game.rng=new Random(seed);game.npcTurnBudgetScheduler=new NpcTurnBudgetScheduler();
        WorldCommandRuntimeContexts.fromGamePanel(game).teleportPlayer(cx+2,room.y+9,"combat observation");
        game.active.stats.put("Vision",42);
        game.setFacingToward(cx+2,cy,"arena observation facing");
        game.firstPersonRenderViewport=new FirstPersonRenderViewport();
        for(int side=0;side<2;side++) for(int i=0;i<size;i++) {
            int gap=preset.equals("pursuit")?8:preset.equals("rifles")?6:preset.equals("mixed") || preset.equals("heavy")?4:0;
            int x=side==0?cx-1-i/5:cx+(preset.equals("wall")?1:0)+i/5+gap,y=cy+i%5;
            NpcEntity n=NpcEntity.create(side==0?Faction.IMPERIAL_GUARD:Faction.CULTIST,ZoneType.GANGER_TURF,x,y,new Random(seed+side*40+i));
            n.id="MATCH-"+side+"-"+i;n.name=(side==0?"A":"B")+" "+(i+1);n.role="Arena combatant";n.state="Hostile";
            n.hp=30;n.ageYears=35;n.ageBand="adult";n.equipmentTier=2;n.intellect=preset.equals("intellect")?(side==0?2:12):5;
            n.meleeSkill=n.firearmsSkill=preset.equals("skills")?(side==0?2:12):5;
            n.equippedMeleeWeapon=preset.equals("heavy") && side==1?"Chainsword":"Knife";
            n.equippedRangedWeapon=preset.equals("knives") || preset.equals("pursuit") || preset.equals("mixed") && side==1?"":preset.equals("heavy")?(side==0?"Stub pistol":"Heavy stubber"):"Light Rifle";
            n.equippedArmor=n.equippedRangedWeapon.isBlank() || preset.equals("heavy") && side==0?"Plain civilian coat":"Guard flak vest";
            n.equippedExplosive="";n.loadedShots=n.magazineCapacityLocal(n.equippedRangedWeapon);n.ammoReloadsRemaining=4;
            n.combatLoadoutInitialized=true;n.combatTeam=side==0?"A":"B";
            n.combatSupport.items.clear();
            CyberneticLimbAuthority.seedCandidate(game,n,DeveloperFacilityAuthority.layout(game).cyberneticPercent,seed+100000+side*40+i);
            game.world.npcs.add(n);DeveloperFacilityAuthority.layout(game).arenaActors.add(n.id);
        }
        DeveloperCoverLayoutAuthority.scatter(game);
        DeveloperCombatFixtureAuthority.scatter(game);
        ActorCombatAuthority.resetDiagnostics(game);game.world.dirtyVisionRevision++;
        game.logEvent("Arena "+size+"v"+size+" / "+preset+" / seed "+seed+". A=Guard, B=Cultist; player observes. Cybernetic candidate chance "+DeveloperFacilityAuthority.layout(game).cyberneticPercent+"%. Use watch or step controls.");
    }
    static String result(GamePanel game) {
        long a=game.world.npcs.stream().filter(n->n.hp>0 && "A".equals(n.combatTeam)).count();
        long b=game.world.npcs.stream().filter(n->n.hp>0 && "B".equals(n.combatTeam)).count();
        return "Arena A="+a+" B="+b+" / "+(a>0 && b>0?"active":a>0?"A wins":b>0?"B wins":"no surviving team");
    }
    static void toggleWatch(GamePanel game) {
        if(WATCHERS.containsKey(game)) {stop(game);game.logEvent("Arena observation paused.");return;}
        Timer timer=new Timer(350,event->{
            if(DeveloperFacilityAuthority.session(game)==null || !result(game).endsWith("active")) {stop(game);game.logEvent(result(game));return;}
            game.singlePlayerSectorBridge.submitCommand(game,new WaitCommand(SinglePlayerSectorRuntimeBridge.LOCAL_PLAYER_ID));game.repaint();
        });WATCHERS.put(game,timer);timer.start();game.logEvent("Watching arena: one authoritative turn every 350ms.");
    }
    static void stop(GamePanel game) {Timer timer=WATCHERS.remove(game);if(timer!=null) timer.stop();}
}
