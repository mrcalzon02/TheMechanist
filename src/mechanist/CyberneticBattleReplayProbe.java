package mechanist;

import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.swing.SwingUtilities;

/** Records the live arena turn path. Playback never simulates attacks or changes their results. */
final class CyberneticBattleReplayProbe {
    private static String quote(String s) {return "\""+s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","")+"\"";}
    private static void frame(StringBuilder json,GamePanel game,List<NpcEntity> actors) {
        json.append("{\"turn\":").append(game.turn).append(",\"actors\":[");
        for(int i=0;i<actors.size();i++) {if(i>0) json.append(',');NpcEntity n=actors.get(i);
            json.append("{\"id\":").append(quote(n.id)).append(",\"team\":").append(quote(n.combatTeam)).append(",\"name\":").append(quote(n.name)).append(",\"x\":").append(n.x).append(",\"y\":").append(n.y).append(",\"hp\":").append(n.hp).append(",\"ammo\":").append(n.loadedShots).append(",\"weapon\":").append(quote(n.equippedRangedWeapon.isBlank()?n.equippedMeleeWeapon:n.equippedRangedWeapon)).append(",\"drug\":").append(!n.combatSupport.effects.isEmpty()).append(",\"limbs\":[");
            int j=0;for(var l:n.combatSupport.limbs.values()){if(j++>0)json.append(',');json.append("{\"root\":").append(quote(l.root)).append(",\"item\":").append(quote(l.item)).append(",\"quality\":").append(l.quality).append(",\"integrity\":").append(l.integrity).append(",\"power\":").append(l.power).append(",\"calibration\":").append(l.calibration).append(",\"online\":").append(l.online()).append('}');}json.append("]}");
        }
        json.append("],\"events\":[");int i=0;
        for(var e:ActorCombatAuthority.diagnostics(game).recent) if(e.turn()==game.turn) {
            if(i++>0)json.append(',');json.append("{\"attacker\":").append(quote(e.attacker())).append(",\"target\":").append(quote(e.target())).append(",\"outcome\":").append(quote(e.outcome().name())).append(",\"weapon\":").append(quote(e.weapon())).append(",\"damage\":").append(e.damage()).append(",\"chance\":").append(e.chance()).append(",\"roll\":").append(e.roll()).append(",\"region\":").append(quote(e.region())).append('}');
        }json.append("]}");
    }
    public static void main(String[] args) throws Exception {
        Path out=Path.of(args.length==0?"diagnostics/cybernetics-20261026":args[0]);Files.createDirectories(out);
        GamePanel campaign=new GamePanel();campaign.timer.stop();campaign.world=new World(19191,32,24);campaign.world.zoneType=ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        for(char[] c:campaign.world.tiles)Arrays.fill(c,'.');campaign.active=Candidate.random(new Random(3));campaign.seed=19191;campaign.singlePlayerSectorBridge=new SinglePlayerSectorRuntimeBridge(new SectorManager());
        DeveloperFacilityAuthority.Session session=null;StringBuilder csv=new StringBuilder("id,size,preset,seed,cyberneticChance,fittedA,fittedB,turns,survivorsA,survivorsB,winner,damage,shots,healing,hardwareEvents,disabledEvents,missingLivingTicks\n");
        try {
            session=DeveloperFacilityAuthority.enter(campaign);GamePanel g=session.facility();g.timer.stop();g.options.soundEnabled=false;g.options.musicEnabled=false;
            for(int size:new int[]{1,5,10,20}) for(String preset:List.of("rifles","mixed")) for(int percent:new int[]{0,25,100}) {
                long seed=71000;g.turn=0;g.worldTurn=0;g.lastStaffedProductionBackgroundTurn=0;g.combatSupport.restore("");DeveloperFacilityAuthority.layout(g).cyberneticPercent=percent;
                DeveloperCombatScenarioAuthority.prepare(g,size,preset,seed);List<NpcEntity> actors=g.world.npcs.stream().filter(n->!n.combatTeam.isBlank()).toList();
                for(NpcEntity n:actors){n.combatSupport.maximumHp=30;n.combatSupport.items.addAll(List.of("Medkit","Street Stimm","Augmetic repair kit","Augmetic power cell","Augmetic calibration tools"));n.intellect=6;}
                long fittedA=actors.stream().filter(n->n.combatTeam.equals("A") && !n.combatSupport.limbs.isEmpty()).count(),fittedB=actors.stream().filter(n->n.combatTeam.equals("B") && !n.combatSupport.limbs.isEmpty()).count();
                String id=size+"v"+size+"-"+preset+"-cyber"+percent;StringBuilder replay=new StringBuilder("{\"id\":"+quote(id)+",\"size\":"+size+",\"preset\":"+quote(preset)+",\"seed\":"+seed+",\"chance\":"+percent+",\"frames\":[");
                frame(replay,g,actors);int hardware=0,missing=0;
                for(int turn=1;turn<=180;turn++) {
                    g.advanceTurnBody("");replay.append(',');frame(replay,g,actors);
                    for(NpcEntity n:actors)if(n.hp>0 && n.lastTickTurn!=g.turn)missing++;
                    for(var e:ActorCombatAuthority.diagnostics(g).recent)if(e.turn()==g.turn && e.weapon().startsWith("HARDWARE "))hardware++;
                    long a=actors.stream().filter(n->n.hp>0 && n.combatTeam.equals("A")).count(),b=actors.stream().filter(n->n.hp>0 && n.combatTeam.equals("B")).count();if(a==0 || b==0)break;
                }
                long a=actors.stream().filter(n->n.hp>0 && n.combatTeam.equals("A")).count(),b=actors.stream().filter(n->n.hp>0 && n.combatTeam.equals("B")).count();String winner=a>0 && b>0?"timeout":a>0?"A":b>0?"B":"mutual";
                replay.append("],\"winner\":").append(quote(winner)).append('}');Files.writeString(out.resolve(id+".json"),replay);
                var d=ActorCombatAuthority.diagnostics(g);csv.append(String.join(",",id,""+size,preset,""+seed,""+percent,""+fittedA,""+fittedB,""+g.turn,""+a,""+b,winner,""+d.damage,""+d.shots,""+d.healingRecovered,""+hardware,""+d.counts.getOrDefault(ActorCombatAuthority.Outcome.DISABLED,0L),""+missing)).append('\n');
                System.out.println(id+" / "+winner+" / turns "+g.turn+" / hardware hits "+hardware+" / missed updates "+missing);
            }
            Files.writeString(out.resolve("battles.csv"),csv);
        } finally {if(session!=null){DeveloperFacilityAuthority.leave(session.facility());SwingUtilities.invokeAndWait(()->{});}campaign.singlePlayerSectorBridge.close();campaign.shutdownRuntime();}
    }
}
