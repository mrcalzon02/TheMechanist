package mechanist;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Observes the actual facility turn path; deliberately supplies no simulated attacks or damage. */
final class CombatScenarioBaselineProbe {
    record Kit(String melee, String ranged, String armor, int intellect) { }
    record Scenario(String name, Kit a, Kit b, int gap, boolean wall) { }
    static final Kit KNIFE=new Kit("Knife","","Plain civilian coat",5);
    static final Kit RIFLE=new Kit("Knife","Light Rifle","Guard flak vest",5);
    static final Kit PISTOL=new Kit("Knife","Stub pistol","Plain civilian coat",5);
    static final Kit HEAVY=new Kit("Chainsword","Heavy stubber","Guard flak vest",5);
    static final Scenario[] SCENARIOS={
        new Scenario("equal-knives-contact",KNIFE,KNIFE,0,false),
        new Scenario("equal-rifles-range",RIFLE,RIFLE,6,false),
        new Scenario("rifles-vs-knives",RIFLE,KNIFE,4,false),
        new Scenario("pistols-vs-heavy",PISTOL,HEAVY,4,false),
        new Scenario("intellect-2-vs-12",new Kit("Knife","Light Rifle","Guard flak vest",2),new Kit("Knife","Light Rifle","Guard flak vest",12),0,false),
        new Scenario("rifles-wall-separated",RIFLE,RIFLE,0,true)
    };
    public static void main(String[] args) throws Exception {
        Path output=Path.of(args.length==0?"diagnostics/combat-scenarios-20261026":args[0]);Files.createDirectories(output);
        GamePanel campaign=new GamePanel();campaign.timer.stop();campaign.world=new World(19191L,32,24);
        campaign.world.zoneType=ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        for(char[] column:campaign.world.tiles) Arrays.fill(column,'.');
        campaign.active=Candidate.random(new Random(19191L));campaign.seed=19191;campaign.playerX=4;campaign.playerY=5;
        campaign.singlePlayerSectorBridge=new SinglePlayerSectorRuntimeBridge(new SectorManager());
        DeveloperFacilityAuthority.Session session=null;
        StringBuilder rows=new StringBuilder("scenario,size,seed,mirror,turns,survivorsA,survivorsB,hpLostA,hpLostB,ammoSpent,movementSteps,actorTicks,missingTicks,winner,status,elapsedMs\n");
        int runs=0,damaging=0,resolved=0;long totalSteps=0,totalTicks=0;
        try {
            session=DeveloperFacilityAuthority.enter(campaign);GamePanel game=session.facility();game.timer.stop();game.setSize(1280,800);
            game.options.soundEnabled=false;game.options.musicEnabled=false;game.options.doomModeEnabled=false;
            game.panelMode=GamePanel.PanelMode.NONE;
            for(Scenario scenario:SCENARIOS) for(int size:new int[]{1,5,10,20}) for(int seed=0;seed<10;seed++) for(int mirror=0;mirror<2;mirror++) {
                long started=System.nanoTime();DeveloperFacilityAuthority.clearArena(game);
                for(int y=34;y<60;y++) game.world.tiles[27][y]=scenario.wall?'#':'.';
                game.turn=0;game.worldTurn=0;game.lastStaffedProductionBackgroundTurn=0;game.rng=new Random(71000+seed);
                game.npcTurnBudgetScheduler=new NpcTurnBudgetScheduler();
                WorldCommandRuntimeContexts.fromGamePanel(game).teleportPlayer(27,32,"combat observer");
                List<NpcEntity> actors=new ArrayList<>();
                for(int side=0;side<2;side++) for(int i=0;i<size;i++) {
                    boolean left=(side==mirror);Kit kit=side==0?scenario.a:scenario.b;
                    int x=left?26-i/5:28+i/5+scenario.gap;
                    if(!scenario.wall && scenario.gap==0 && !left) x--;
                    int y=37+i%5;
                    NpcEntity npc=NpcEntity.create(side==0?Faction.IMPERIAL_GUARD:Faction.CULTIST,ZoneType.GANGER_TURF,x,y,new Random(71000L+seed*100+side*40+i));
                    npc.id="PROBE-"+side+"-"+i;npc.name=(side==0?"A":"B")+" "+(i+1);npc.state="Hostile";npc.role="Combat baseline participant";
                    npc.hp=30;npc.intellect=kit.intellect;npc.equipmentTier=2;npc.ageYears=35;npc.ageBand="adult";
                    npc.equippedMeleeWeapon=kit.melee;npc.equippedRangedWeapon=kit.ranged;npc.equippedArmor=kit.armor;npc.equippedExplosive="";
                    npc.loadedShots=npc.magazineCapacityLocal(kit.ranged);npc.ammoReloadsRemaining=4;
                    game.world.npcs.add(npc);DeveloperFacilityAuthority.layout(game).arenaActors.add(npc.id);actors.add(npc);
                }
                game.world.dirtyVisionRevision++;game.world.dirtyLightRevision++;
                boolean trace=seed==0 && mirror==0;
                String id=scenario.name+"-"+size;
                StringBuilder frames=new StringBuilder("{\"id\":\""+id+"\",\"scenario\":\""+scenario.name+"\",\"size\":"+size+",\"wall\":"+scenario.wall+",\"frames\":[");
                int[] ammo=actors.stream().mapToInt(n->n.loadedShots).toArray();int moves=0,ticks=0,missing=0;
                if(trace) frame(frames,game,actors,0);
                for(int turn=1;turn<=120;turn++) {
                    int[] xs=actors.stream().mapToInt(n->n.x).toArray(),ys=actors.stream().mapToInt(n->n.y).toArray();
                    game.advanceTurnBody("");
                    for(int i=0;i<actors.size();i++) { NpcEntity n=actors.get(i);if(n.x!=xs[i] || n.y!=ys[i]) moves++;if(n.lastTickTurn==game.turn) ticks++;else missing++; }
                    if(trace && turn%5==0) { frames.append(',');frame(frames,game,actors,turn); }
                    if(trace && scenario==SCENARIOS[0] && (turn==1 || turn==120)) snapshot(game,output,id+"-turn-"+turn);
                }
                int aliveA=0,aliveB=0,lostA=0,lostB=0,spent=0;
                for(int i=0;i<actors.size();i++) {NpcEntity n=actors.get(i);boolean a=i<size;if(n.hp>0) {if(a) aliveA++;else aliveB++;}if(a) lostA+=30-n.hp;else lostB+=30-n.hp;spent+=ammo[i]-n.loadedShots;}
                String winner=aliveA==0 && aliveB>0?"B":aliveB==0 && aliveA>0?"A":aliveA==0 && aliveB==0?"mutual":"none";
                boolean damage=lostA>0 || lostB>0;if(damage) damaging++;if(!winner.equals("none")) resolved++;
                String status=winner.equals("none")?(damage?"TIMEOUT":"NO_ACTOR_DAMAGE_OBSERVED"):"RESOLVED";
                rows.append(String.join(",",scenario.name,""+size,""+(71000+seed),""+mirror,"120",""+aliveA,""+aliveB,""+lostA,""+lostB,""+spent,""+moves,""+ticks,""+missing,winner,status,""+((System.nanoTime()-started)/1_000_000))).append('\n');
                if(trace) {frames.append("]}");Files.writeString(output.resolve(id+".json"),frames);}
                runs++;totalSteps+=moves;totalTicks+=ticks;
                if(runs%40==0) System.out.println("CombatScenarioBaselineProbe progress runs="+runs+" damaging="+damaging+" resolved="+resolved);
            }
            Files.writeString(output.resolve("results.csv"),rows);
            Files.writeString(output.resolve("summary.json"),"{\"runs\":"+runs+",\"turnsPerRun\":120,\"damagingRuns\":"+damaging+",\"resolvedRuns\":"+resolved+",\"movementSteps\":"+totalSteps+",\"actorTicks\":"+totalTicks+",\"combatSkillsAvailable\":false,\"status\":\"BASELINE_OBSERVATION_ONLY\"}\n");
            DeveloperFacilityAuthority.clearArena(game);
            NpcEntity target=NpcEntity.create(Faction.CULTIST,ZoneType.GANGER_TURF,28,32,new Random(13));
            target.id="PROBE-PLAYER-TARGET";target.hp=30;target.state="Hostile";game.world.npcs.add(target);
            game.equippedRightHandItem="Light Rifle";game.loadedWeaponShots.put("Light Rifle",8);
            int beforeTurn=game.turn,beforeHp=target.hp;
            for(int i=0;i<10;i++) {game.combatX=target.x;game.combatY=target.y;game.confirmCombatTarget();}
            Files.writeString(output.resolve("player-confirm.json"),"{\"confirmations\":10,\"turnsAdvanced\":"+(game.turn-beforeTurn)+",\"targetHpLost\":"+(beforeHp-target.hp)+",\"loadedBefore\":8,\"loadedAfter\":"+game.loadedWeaponShots.get("Light Rifle")+"}\n");
            System.out.println("CombatScenarioBaselineProbe COMPLETE runs="+runs+" damaging="+damaging+" resolved="+resolved+" movement="+totalSteps+" actorTicks="+totalTicks);
        } finally {
            if(session!=null) {DeveloperFacilityAuthority.leave(session.facility());SwingUtilities.invokeAndWait(()->{});}
            campaign.singlePlayerSectorBridge.close();campaign.shutdownRuntime();
        }
    }
    private static void frame(StringBuilder json,GamePanel game,List<NpcEntity> actors,int turn) {
        json.append("{\"turn\":").append(turn).append(",\"actors\":[");
        for(int i=0;i<actors.size();i++) {if(i>0) json.append(',');NpcEntity n=actors.get(i);json.append('[').append(n.x).append(',').append(n.y).append(',').append(n.hp).append(',').append(n.loadedShots).append(',').append(n.lastTickTurn).append(']');}
        json.append("]}");
    }
    private static void snapshot(GamePanel game,Path folder,String name) throws Exception {
        BufferedImage image=new BufferedImage(1280,800,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();game.paint(g);g.dispose();ImageIO.write(image,"png",folder.resolve(name+".png").toFile());
    }
}
