package mechanist;

import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Real attacks bind feedback; occupied firing lanes route; seeded cover stays physical and repeatable. */
final class ActorCombatPresentationSmoke {
    static int checks;
    static void require(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        GamePanel campaign=new GamePanel();campaign.timer.stop();campaign.world=new World(19191,32,24);campaign.world.zoneType=ZoneType.NEUTRAL_CIVILIAN_FLOOR;for(char[] col:campaign.world.tiles)Arrays.fill(col,'.');campaign.active=Candidate.random(new Random(3));campaign.singlePlayerSectorBridge=new SinglePlayerSectorRuntimeBridge(new SectorManager());
        DeveloperFacilityAuthority.Session session=null;
        try {
            session=DeveloperFacilityAuthority.enter(campaign);GamePanel g=session.facility();g.timer.stop();g.options.soundEnabled=true;g.options.musicEnabled=false;g.options.volume=80;g.options.sfxVolume=70;g.screen=GamePanel.Screen.GAME;
            var layout=DeveloperFacilityAuthority.layout(g);layout.cyberneticPercent=0;layout.coverCount=0;DeveloperCombatScenarioAuthority.prepare(g,1,"rifles",71000);
            NpcEntity shooter=g.world.npcs.stream().filter(n->n.combatTeam.equals("A")).findFirst().orElseThrow(),target=g.world.npcs.stream().filter(n->n.combatTeam.equals("B")).findFirst().orElseThrow();
            NpcEntity blocker=NpcEntity.create(Faction.IMPERIAL_GUARD,ZoneType.GANGER_TURF,shooter.x+1,shooter.y,new Random(4));blocker.id="LANE-BLOCKER";blocker.combatTeam="A";g.world.npcs.add(blocker);
            int x=shooter.x,y=shooter.y,ammo=shooter.loadedShots;g.turn++;shooter.lastTickTurn=-1;
            ActorCombatAuthority.takeNpcTurn(g,shooter);require(shooter.x!=x || shooter.y!=y,"Blocked shooter must reposition rather than wait forever");require(shooter.loadedShots==ammo,"Repositioning does not also fire");require(ActorCombatAuthority.diagnostics(g).counts.getOrDefault(ActorCombatAuthority.Outcome.ALLY_IN_LINE,0L)==1,"Blocked firing lane is recorded");
            g.world.npcs.remove(blocker);shooter.x=target.x-2;shooter.y=target.y;g.playerX=target.x;g.playerY=target.y-5;g.setFacingToward(target.x,target.y,"feedback smoke");
            int[] cues={0};ActorCombatFeedbackAuthority.audioSink(g,(key,sx,sy,occ,room,strength)->{if(key.startsWith("weapon_"))cues[0]++;require(sx==shooter.x && sy==shooter.y,"NPC cue originates at the shooter, not the player");});
            ActorCombatAuthority.resetDiagnostics(g);g.putClientProperty("mechanist.captureClock",1000L);g.turn++;shooter.lastTickTurn=-1;ActorCombatAuthority.takeNpcTurn(g,shooter);
            require(cues[0]==1 && ActorCombatFeedbackAuthority.shots(g).size()==1,"Actual NPC attack emits one cue and one visual event");require(shooter.loadedShots==ammo-1,"Feedback binds an actual finite shot");
            String support=shooter.combatSupport.save();int hp=target.hp;int shotAmmo=shooter.loadedShots;ActorCombatFeedbackAuthority.shots(g);ActorCombatFeedbackAuthority.shots(g);
            require(target.hp==hp && shooter.loadedShots==shotAmmo && support.equals(shooter.combatSupport.save()),"Inspecting feedback never changes simulation state");
            g.putClientProperty("mechanist.captureClock",1500L);require(ActorCombatFeedbackAuthority.shots(g).isEmpty(),"Visual event expires");ActorCombatFeedbackAuthority.audioSink(g,null);
            BaseObject coverProof=new BaseObject("Sandbag Line",'S',target.x+1,target.y,0,0);coverProof.integrity=40;g.baseObjects.add(coverProof);target.hp=1000;shooter.loadedShots=8;shooter.lastTickTurn=-1;g.turn++;
            ActorCombatAuthority.takeNpcTurn(g,shooter);require(ActorCombatAuthority.diagnostics(g).recent.getLast().cover()==16,"Attack-time diagnostics retain the actual cover penalty");g.baseObjects.remove(coverProof);
            SoundManager sounds=new SoundManager();sounds.load();var pcm=sounds.renderPositioned("weapon_shot",SpatialAudioAuthority.mix(0,0,1,0,2,0,50,16,false),true);require(pcm!=null && pcm.samples().length>0,"Same live positional PCM renderer works without a device");
            String sandbag=ObjectSemanticAssetAuthority.runtimeAssetIdForName("Sandbag Line").orElseThrow(),barricade=ObjectSemanticAssetAuthority.runtimeAssetIdForName("Scrap Barricade").orElseThrow();
            require(mechanist.assets.AssetManager.metadata(sandbag).orElseThrow().pathOrUri().replace('\\','/').toLowerCase(Locale.ROOT).contains("/defenses/defenses_r01c01_"),"Sandbags must use reviewed sandbag art, never a speaker tower");
            require(mechanist.assets.AssetManager.metadata(barricade).orElseThrow().pathOrUri().replace('\\','/').toLowerCase(Locale.ROOT).contains("/defenses/pdf_defenses_r03c01_"),"Barricades use reviewed damaged defensive cover art");
            layout.coverCount=24;layout.coverSeed=81000;DeveloperCombatScenarioAuthority.prepare(g,10,"pursuit",71000);
            String first=layout.coverObjects.stream().map(b->b.name+":"+b.x+","+b.y).toList().toString();require(layout.coverObjects.size()==24,"Requested cover count placed");
            for(BaseObject b:layout.coverObjects){require(PassiveDefenseEffectsAuthority.coverPenalty(b)>0,"Existing cover profile applies");require(g.world.npcAt(b.x,b.y)==null,"Cover is not placed beneath combatants");if(b.symbol=='S')require(!g.world.walkable(b.x,b.y),"Sandbags retain existing blocked traversal");}
            DeveloperCombatScenarioAuthority.prepare(g,10,"pursuit",71000);require(first.equals(layout.coverObjects.stream().map(b->b.name+":"+b.x+","+b.y).toList().toString()),"Seeded cover exactly repeats");
            Map<String,String> before=new HashMap<>();for(NpcEntity n:g.world.npcs)if(!n.combatTeam.isBlank())before.put(n.id,n.x+","+n.y);
            for(int turn=0;turn<5;turn++)g.advanceTurnBody("");long moving=g.world.npcs.stream().filter(n->before.containsKey(n.id) && !before.get(n.id).equals(n.x+","+n.y)).count();require(moving>0,"Separated melee groups must close through the cover layout");
            for(NpcEntity n:g.world.npcs)if(!n.combatTeam.isBlank())require(g.world.tiles[n.x][n.y]!='S',"Routing cannot cross a sandbag tile");
            DeveloperFacilityAuthority.clearArena(g);require(layout.coverObjects.isEmpty(),"Arena reset removes only test cover");
            g.setFacingToward(g.playerX,g.playerY+1,"initial observer");g.options.doomModeEnabled=true;g.firstPersonRenderViewport=new FirstPersonRenderViewport();g.setSize(320,200);
            java.awt.image.BufferedImage initial=new java.awt.image.BufferedImage(320,200,java.awt.image.BufferedImage.TYPE_INT_RGB);java.awt.Graphics2D graphics=initial.createGraphics();g.firstPersonRenderViewport.render(graphics,g);graphics.dispose();
            require(g.facingDx==0 && g.facingDy==1,"Fresh Doom viewport respects observer facing");
            String result="ActorCombatPresentationSmoke PASS checks="+checks+" npc-sound=true real-shot-feedback=true lane-routing=true pursuit=true cover-seed=true cover-traversal=true";
            if(args.length>0){Path out=Path.of(args[0]);Files.createDirectories(out);Files.writeString(out.resolve("presentation-smoke.txt"),result+"\n");}System.out.println(result);
        } finally {if(session!=null){ActorCombatFeedbackAuthority.audioSink(session.facility(),null);DeveloperFacilityAuthority.leave(session.facility());SwingUtilities.invokeAndWait(()->{});}campaign.singlePlayerSectorBridge.close();campaign.shutdownRuntime();}
    }
}
