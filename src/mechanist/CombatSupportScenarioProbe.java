package mechanist;

import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.swing.SwingUtilities;

/** Seeded support-item encounter matrix. Provisioning is setup; every action uses the live turn path. */
final class CombatSupportScenarioProbe {
    static final String[] VARIANTS={"control","A-medkit","B-medkit","A-stimm","B-stimm","A-armor","B-armor","both-all"};
    public static void main(String[] args) throws Exception {
        Path output=Path.of(args.length==0?"diagnostics/combat-support-20261026":args[0]);Files.createDirectories(output);
        GamePanel campaign=new GamePanel();campaign.timer.stop();campaign.world=new World(19191L,32,24);
        campaign.world.zoneType=ZoneType.NEUTRAL_CIVILIAN_FLOOR;for(char[] column:campaign.world.tiles) Arrays.fill(column,'.');
        campaign.active=Candidate.random(new Random(19191));campaign.seed=19191;campaign.playerX=4;campaign.playerY=5;
        campaign.singlePlayerSectorBridge=new SinglePlayerSectorRuntimeBridge(new SectorManager());
        DeveloperFacilityAuthority.Session session=null;
        StringBuilder rows=new StringBuilder("mode,variant,size,seed,mirror,turns,winner,survivorsA,survivorsB,remainingHpA,remainingHpB,medkitsUsed,stimmsUsed,hpRestored,shots,attacks,hits,kills,missingLivingTicks\n");
        int runs=0,resolved=0;long restored=0,doses=0;
        try {
            session=DeveloperFacilityAuthority.enter(campaign);GamePanel game=session.facility();game.timer.stop();game.options.soundEnabled=false;game.options.musicEnabled=false;
            for(String mode:List.of("knives","rifles")) for(String variant:VARIANTS) for(int size:new int[]{1,5,10,20}) for(int seed=71000;seed<71010;seed++) for(int mirror=0;mirror<2;mirror++) {
                game.turn=0;game.worldTurn=0;game.lastStaffedProductionBackgroundTurn=0;game.combatSupport.restore("");
                DeveloperFacilityAuthority.layout(game).cyberneticPercent=0;DeveloperCombatScenarioAuthority.prepare(game,size,mode,seed);
                List<NpcEntity> actors=game.world.npcs.stream().filter(n->!n.combatTeam.isBlank()).toList();
                int min=actors.stream().mapToInt(n->n.x).min().orElseThrow(),max=actors.stream().mapToInt(n->n.x).max().orElseThrow();
                for(NpcEntity n:actors) {
                    n.equippedArmor="";n.combatSupport.maximumHp=30;n.combatSupport.items.clear();
                    if(mirror==1) {n.x=min+max-n.x;n.homeX=n.x;}
                    boolean selected=variant.startsWith(n.combatTeam+"-") || variant.equals("both-all");
                    if(selected && (variant.endsWith("medkit") || variant.equals("both-all"))) n.combatSupport.items.add("Medkit");
                    if(selected && (variant.endsWith("stimm") || variant.equals("both-all"))) n.combatSupport.items.add("Street Stimm");
                    if(selected && (variant.endsWith("armor") || variant.equals("both-all"))) {n.equippedArmor="Arbites suppression armor";n.combatSupport.worn.put(CharacterEquipmentAndMedicalAuthority.EquipmentSlot.HEADGEAR,"Guard flak helmet");}
                }
                ActorCombatAuthority.resetDiagnostics(game);boolean trace=seed==71000 && mirror==0;
                String name=mode+"-"+variant+"-"+size;
                StringBuilder frames=new StringBuilder("{\"id\":\""+name+"\",\"mode\":\""+mode+"\",\"variant\":\""+variant+"\",\"size\":"+size+",\"frames\":[");
                StringBuilder events=new StringBuilder("turn\tactor\ttarget\tweapon\toutcome\tchance\troll\tdamage\tammo\tregion\tarmor\n");
                int elapsed=0,missing=0;String winner="none";
                if(trace) frame(frames,game,actors,0);
                for(int turn=1;turn<=180;turn++) {
                    game.advanceTurnBody("");elapsed=turn;
                    for(NpcEntity n:actors) if(n.hp>0 && n.lastTickTurn!=game.turn) missing++;
                    if(trace) {
                        frames.append(',');frame(frames,game,actors,turn);
                        for(var e:ActorCombatAuthority.diagnostics(game).recent) if(e.turn()==game.turn)
                            events.append(e.turn()).append('\t').append(e.attacker()).append('\t').append(e.target()).append('\t').append(e.weapon()).append('\t').append(e.outcome()).append('\t').append(e.chance()).append('\t').append(e.roll()).append('\t').append(e.damage()).append('\t').append(e.ammo()).append('\t').append(e.region()).append('\t').append(e.armor()).append('\n');
                    }
                    long a=actors.stream().filter(n->n.hp>0 && n.combatTeam.equals("A")).count(),b=actors.stream().filter(n->n.hp>0 && n.combatTeam.equals("B")).count();
                    if(a==0 || b==0) {winner=a>0?"A":b>0?"B":"mutual";break;}
                }
                var d=ActorCombatAuthority.diagnostics(game);long medkits=d.supportUses.getOrDefault("Medkit",0L),stimms=d.supportUses.getOrDefault("Street Stimm",0L);
                long kills=d.counts.getOrDefault(ActorCombatAuthority.Outcome.KILL,0L),hits=d.counts.getOrDefault(ActorCombatAuthority.Outcome.HIT,0L),misses=d.counts.getOrDefault(ActorCombatAuthority.Outcome.MISS,0L);
                long aliveA=actors.stream().filter(n->n.hp>0 && n.combatTeam.equals("A")).count(),aliveB=actors.stream().filter(n->n.hp>0 && n.combatTeam.equals("B")).count();
                int hpA=actors.stream().filter(n->n.combatTeam.equals("A")).mapToInt(n->n.hp).sum(),hpB=actors.stream().filter(n->n.combatTeam.equals("B")).mapToInt(n->n.hp).sum();
                rows.append(String.join(",",mode,variant,""+size,""+seed,""+mirror,""+elapsed,winner,""+aliveA,""+aliveB,""+hpA,""+hpB,""+medkits,""+stimms,""+d.healingRecovered,""+d.shots,""+(hits+kills+misses),""+(hits+kills),""+kills,""+missing)).append('\n');
                if(trace) {frames.append("]}");Files.writeString(output.resolve(name+".json"),frames);Files.writeString(output.resolve(name+"-events.tsv"),events);}
                runs++;if(!winner.equals("none")) resolved++;restored+=d.healingRecovered;doses+=medkits+stimms;
                if(runs%160==0) System.out.println("CombatSupportScenarioProbe runs="+runs+" resolved="+resolved+" restored="+restored+" used="+doses);
            }
            Files.writeString(output.resolve("encounters.csv"),rows);
            Files.writeString(output.resolve("encounter-summary.json"),"{\"runs\":"+runs+",\"resolved\":"+resolved+",\"maximumTurns\":180,\"healthRestored\":"+restored+",\"dosesUsed\":"+doses+"}\n");
            System.out.println("CombatSupportScenarioProbe COMPLETE runs="+runs+" resolved="+resolved+" healthRestored="+restored+" dosesUsed="+doses);
        } finally {if(session!=null){DeveloperFacilityAuthority.leave(session.facility());SwingUtilities.invokeAndWait(()->{});}campaign.singlePlayerSectorBridge.close();campaign.shutdownRuntime();}
    }
    private static void frame(StringBuilder json,GamePanel game,List<NpcEntity> actors,int turn) {
        json.append("{\"turn\":").append(turn).append(",\"actors\":[");
        for(int i=0;i<actors.size();i++) {if(i>0) json.append(',');NpcEntity n=actors.get(i);
            long active=n.combatSupport.effects.values().stream().filter(e->e.expires()>game.worldTurn).count();
            json.append('[').append(n.x).append(',').append(n.y).append(',').append(n.hp).append(',').append(n.loadedShots).append(',').append(active).append(',').append(n.combatSupport.items.size()).append(']');}
        json.append("]}");
    }
}
