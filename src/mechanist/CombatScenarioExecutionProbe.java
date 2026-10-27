package mechanist;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Observes the actual facility turn path; deliberately supplies no simulated attacks or damage. */
final class CombatScenarioExecutionProbe {
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
        new Scenario("rifles-wall-separated",RIFLE,RIFLE,0,true),
        new Scenario("skill-2-vs-12",RIFLE,RIFLE,0,false)
    };
    public static void main(String[] args) throws Exception {
        Path output=Path.of(args.length==0?"diagnostics/combat-execution-20261026":args[0]);Files.createDirectories(output);
        GamePanel campaign=new GamePanel();campaign.timer.stop();campaign.world=new World(19191L,32,24);
        campaign.world.zoneType=ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        for(char[] column:campaign.world.tiles) Arrays.fill(column,'.');
        campaign.active=Candidate.random(new Random(19191L));campaign.seed=19191;campaign.playerX=4;campaign.playerY=5;
        campaign.singlePlayerSectorBridge=new SinglePlayerSectorRuntimeBridge(new SectorManager());
        DeveloperFacilityAuthority.Session session=null;
        StringBuilder rows=new StringBuilder("scenario,size,seed,mirror,turns,survivorsA,survivorsB,hpLostA,hpLostB,ammoSpent,movementSteps,actorTicks,missingTicks,winner,status,elapsedMs,attacks,hits,kills,reloads,wallRefusals,actorLineRefusals\n");
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
                    npc.combatSupport.items.clear();
                    npc.hp=30;npc.intellect=kit.intellect;npc.combatTeam=side==0?"A":"B";
                    npc.meleeSkill=npc.firearmsSkill=scenario.name.equals("skill-2-vs-12")?(side==0?2:12):5;npc.equipmentTier=2;npc.ageYears=35;npc.ageBand="adult";
                    npc.equippedMeleeWeapon=kit.melee;npc.equippedRangedWeapon=kit.ranged;npc.equippedArmor=kit.armor;npc.equippedExplosive="";
                    npc.loadedShots=npc.magazineCapacityLocal(kit.ranged);npc.ammoReloadsRemaining=4;
                    game.world.npcs.add(npc);DeveloperFacilityAuthority.layout(game).arenaActors.add(npc.id);actors.add(npc);
                }
                game.world.dirtyVisionRevision++;game.world.dirtyLightRevision++;
                boolean trace=seed==0 && mirror==0;
                String id=scenario.name+"-"+size;
                StringBuilder frames=new StringBuilder("{\"id\":\""+id+"\",\"scenario\":\""+scenario.name+"\",\"size\":"+size+",\"wall\":"+scenario.wall+",\"frames\":[");
                ActorCombatAuthority.resetDiagnostics(game);
                int[] ammo=actors.stream().mapToInt(n->n.loadedShots).toArray();int moves=0,ticks=0,missing=0,elapsedTurns=0;
                StringBuilder events=new StringBuilder("turn\tattacker\ttarget\tweapon\toutcome\tchance\troll\tdamage\tammo\n");
                if(trace) frame(frames,game,actors,0);
                for(int turn=1;turn<=120;turn++) {
                    int[] xs=actors.stream().mapToInt(n->n.x).toArray(),ys=actors.stream().mapToInt(n->n.y).toArray();
                    game.advanceTurnBody("");elapsedTurns=turn;
                    if(trace) for(var e:ActorCombatAuthority.diagnostics(game).recent) if(e.turn()==game.turn) events.append(e.turn()).append('\t').append(e.attacker()).append('\t').append(e.target()).append('\t').append(e.weapon()).append('\t').append(e.outcome()).append('\t').append(e.chance()).append('\t').append(e.roll()).append('\t').append(e.damage()).append('\t').append(e.ammo()).append('\n');
                    for(int i=0;i<actors.size();i++) { NpcEntity n=actors.get(i);if(n.x!=xs[i] || n.y!=ys[i]) moves++;if(n.lastTickTurn==game.turn) ticks++;else if(n.hp>0) missing++; }
                    if(trace && turn%5==0) { frames.append(',');frame(frames,game,actors,turn); }
                    boolean finished=actors.subList(0,size).stream().noneMatch(n->n.hp>0) || actors.subList(size,size*2).stream().noneMatch(n->n.hp>0);
                    if(trace && scenario==SCENARIOS[0] && turn==1) snapshot(game,output,id+"-turn-1");
                    if(finished) {if(trace && turn%5!=0) {frames.append(',');frame(frames,game,actors,turn);}break;}
                }
                int aliveA=0,aliveB=0,lostA=0,lostB=0,spent=0;
                for(int i=0;i<actors.size();i++) {NpcEntity n=actors.get(i);boolean a=i<size;if(n.hp>0) {if(a) aliveA++;else aliveB++;}if(a) lostA+=30-n.hp;else lostB+=30-n.hp;spent+=ammo[i]-n.loadedShots;}
                var diagnostics=ActorCombatAuthority.diagnostics(game);spent=(int)diagnostics.shots;
                long hits=diagnostics.counts.getOrDefault(ActorCombatAuthority.Outcome.HIT,0L),kills=diagnostics.counts.getOrDefault(ActorCombatAuthority.Outcome.KILL,0L),misses=diagnostics.counts.getOrDefault(ActorCombatAuthority.Outcome.MISS,0L);
                String winner=aliveA==0 && aliveB>0?"B":aliveB==0 && aliveA>0?"A":aliveA==0 && aliveB==0?"mutual":"none";
                boolean damage=lostA>0 || lostB>0;if(damage) damaging++;if(!winner.equals("none")) resolved++;
                String status=winner.equals("none")?(damage?"TIMEOUT":"NO_ACTOR_DAMAGE_OBSERVED"):"RESOLVED";
                rows.append(String.join(",",scenario.name,""+size,""+(71000+seed),""+mirror,""+elapsedTurns,""+aliveA,""+aliveB,""+lostA,""+lostB,""+spent,""+moves,""+ticks,""+missing,winner,status,""+((System.nanoTime()-started)/1_000_000),""+(hits+kills+misses),""+(hits+kills),""+kills,""+diagnostics.counts.getOrDefault(ActorCombatAuthority.Outcome.RELOAD,0L),""+diagnostics.counts.getOrDefault(ActorCombatAuthority.Outcome.WALL,0L),""+diagnostics.counts.getOrDefault(ActorCombatAuthority.Outcome.ALLY_IN_LINE,0L))).append('\n');
                if(trace) {frames.append("]}");Files.writeString(output.resolve(id+".json"),frames);Files.writeString(output.resolve(id+"-events.tsv"),events);snapshot(game,output,id+"-final");}
                runs++;totalSteps+=moves;totalTicks+=ticks;
                if(runs%40==0) System.out.println("CombatScenarioExecutionProbe progress runs="+runs+" damaging="+damaging+" resolved="+resolved);
            }
            Files.writeString(output.resolve("results.csv"),rows);
            Files.writeString(output.resolve("summary.json"),"{\"runs\":"+runs+",\"turnsPerRun\":120,\"damagingRuns\":"+damaging+",\"resolvedRuns\":"+resolved+",\"movementSteps\":"+totalSteps+",\"actorTicks\":"+totalTicks+",\"combatSkillsAvailable\":true,\"status\":\"ACTOR_COMBAT_EXECUTION\"}\n");
            System.out.println("CombatScenarioExecutionProbe COMPLETE runs="+runs+" damaging="+damaging+" resolved="+resolved+" movement="+totalSteps+" actorTicks="+totalTicks);
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
