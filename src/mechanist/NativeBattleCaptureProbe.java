package mechanist;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.SwingUtilities;

/** Offscreen GamePanel recordings: shipped art/render paths and the same PCM mixer as live sound. */
final class NativeBattleCaptureProbe {
    static final long ORIGIN=1_000_000;
    private record Case(String id,int size,String preset,int cover) { }
    private static final Case[] CASES={new Case("20v20-rifles-open",20,"rifles",0),new Case("20v20-rifles-cover",20,"rifles",16),new Case("20v20-mixed-cover",20,"mixed",16),new Case("10v10-pursuit-cover",10,"pursuit",12)};
    private static final class Audio implements ActorCombatFeedbackAuthority.AudioSink {
        final GamePanel game;final SoundManager manager=new SoundManager();final float[] samples=new float[44100*2*95];
        final StringBuilder events=new StringBuilder("millis\tcue\tx\ty\toccluded\troom\tasset\trate\tgain\tdelayMillis\n");int count,missing;
        Audio(GamePanel game){this.game=game;manager.load();}
        public void cue(String key,int x,int y,boolean occluded,boolean room,double strength) {cue(key,x,y,occluded,room,strength,WeaponSoundVariationAuthority.Variation.NEUTRAL);}
        public void cue(String key,int x,int y,boolean occluded,boolean room,double strength,WeaponSoundVariationAuthority.Variation variation) {
            try {
                int volume=(int)Math.round(Math.min(game.options.sfxVolume,62)*Math.max(0,Math.min(1,strength))*game.options.volume/100.0);
                var mix=SpatialAudioAuthority.mix(game.playerX,game.playerY,game.facingDx,game.facingDy,x,y,volume,16,occluded);
                var pcm=manager.renderPositioned(key,mix,room,variation);if(pcm==null)return;
                int offset=(int)((ActorCombatFeedbackAuthority.now(game)-ORIGIN+variation.delayMillis())*44100/1000)*2,frames=pcm.samples().length/4;
                int resampled=Math.round(frames*44100/pcm.sampleRate());
                for(int i=0;i<resampled;i++){int source=Math.min(frames-1,(int)(i*pcm.sampleRate()/44100));for(int ch=0;ch<2;ch++){int from=source*4+ch*2,at=offset+i*2+ch;if(at>=0 && at<samples.length)samples[at]+=(short)((pcm.samples()[from]&255)|((pcm.samples()[from+1]&255)<<8));}}
                File asset=manager.sounds.get(key);if(asset==null)missing++;count++;
                events.append(ActorCombatFeedbackAuthority.now(game)-ORIGIN).append('\t').append(key).append('\t').append(x).append('\t').append(y).append('\t').append(occluded).append('\t').append(room).append('\t').append(asset==null?"generated fallback":asset.getPath()).append('\t').append(variation.rate()).append('\t').append(variation.gain()).append('\t').append(variation.delayMillis()).append('\n');
            }catch(Exception error){throw new IllegalStateException("Native cue recording failed: "+key,error);}
        }
        void write(Path folder,int milliseconds)throws Exception {
            int length=Math.min(samples.length,Math.max(1,milliseconds)*44100/1000*2);float peak=1;for(int i=0;i<length;i++)peak=Math.max(peak,Math.abs(samples[i]));
            double scale=Math.min(1,30000.0/peak);byte[] pcm=new byte[length*2];for(int i=0;i<length;i++){short s=(short)Math.max(-32768,Math.min(32767,Math.round(samples[i]*scale)));pcm[i*2]=(byte)s;pcm[i*2+1]=(byte)(s>>8);}
            AudioFormat format=new AudioFormat(44100,16,2,true,false);try(AudioInputStream stream=new AudioInputStream(new ByteArrayInputStream(pcm),format,length/2)){AudioSystem.write(stream,AudioFileFormat.Type.WAVE,folder.resolve("battle.wav").toFile());}
            Files.writeString(folder.resolve("audio-events.tsv"),events);Files.writeString(folder.resolve("audio-summary.json"),"{\"cues\":"+count+",\"generatedFallbacks\":"+missing+",\"peakLimiterScale\":"+scale+"}\n");
        }
    }
    public static void main(String[] args)throws Exception {
        Path out=Path.of(args.length==0?"diagnostics/native-battles-20261026":args[0]);Files.createDirectories(out);int maximum=args.length>1?Integer.parseInt(args[1]):100;
        GamePanel campaign=new GamePanel();campaign.timer.stop();campaign.world=new World(19191,32,24);campaign.world.zoneType=ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        for(char[] c:campaign.world.tiles)Arrays.fill(c,'.');campaign.active=Candidate.random(new Random(3));campaign.seed=19191;campaign.singlePlayerSectorBridge=new SinglePlayerSectorRuntimeBridge(new SectorManager());
        DeveloperFacilityAuthority.Session session=null;StringBuilder summary=new StringBuilder("case,size,preset,cover,seed,turns,winner,movementSteps,movingActors,coverAffectedAttacks,audioCues,audioFallbacks\n");
        try {
            session=DeveloperFacilityAuthority.enter(campaign);GamePanel g=session.facility();g.timer.stop();g.setSize(1280,800);g.options.musicEnabled=false;g.options.worldZoomIndex=2;g.options.soundEnabled=true;g.options.volume=85;g.options.sfxVolume=65;
            for(Case c:CASES) {
                g.turn=0;g.worldTurn=0;g.lastStaffedProductionBackgroundTurn=0;g.combatSupport.restore("");var layout=DeveloperFacilityAuthority.layout(g);layout.cyberneticPercent=25;layout.coverCount=c.cover;layout.coverSeed=81000;
                DeveloperCombatScenarioAuthority.prepare(g,c.size,c.preset,71000);Rectangle room=layout.rooms.get("combat");int cx=room.x+25,cy=room.y+13;
                g.playerX=cx+2;g.playerY=cy-4;g.active.stats.put("Vision",42);g.setFacingToward(cx+2,cy,"native arena observer");g.screen=GamePanel.Screen.GAME;g.panelMode=GamePanel.PanelMode.NONE;g.options.doomModeFovDegrees=110;g.firstPersonRenderViewport=new FirstPersonRenderViewport();
                List<NpcEntity> actors=g.world.npcs.stream().filter(n->!n.combatTeam.isBlank()).toList();Path folder=out.resolve(c.id);Files.createDirectories(folder.resolve("2d"));Files.createDirectories(folder.resolve("doom"));
                StringBuilder positions=new StringBuilder("turn\tid\tteam\tx\ty\thp\tammo\n"),events=new StringBuilder("turn\tattacker\ttarget\tweapon\toutcome\tchance\troll\tdamage\tregion\tcoverPenalty\n"),cover=new StringBuilder("seed\tname\tx\ty\tsymbol\tcoverPenalty\tassetId\tassetPath\n");
                for(BaseObject b:layout.coverObjects){String asset=ObjectSemanticAssetAuthority.runtimeAssetIdForBaseObject(b).orElse("");cover.append(layout.coverSeed).append('\t').append(b.name).append('\t').append(b.x).append('\t').append(b.y).append('\t').append(b.symbol).append('\t').append(PassiveDefenseEffectsAuthority.coverPenalty(b)).append('\t').append(asset).append('\t').append(mechanist.assets.AssetManager.metadata(asset).map(mechanist.assets.AssetMetadata::pathOrUri).orElse("missing")).append('\n');}
                Audio audio=new Audio(g);ActorCombatFeedbackAuthority.audioSink(g,audio);Set<String> moved=new HashSet<>();int steps=0,covered=0,frames=0;
                for(int turn=0;turn<=maximum;turn++) {
                    g.putClientProperty("mechanist.captureClock",ORIGIN+turn*500L);
                    if(turn>0){Map<String,Point> before=new HashMap<>();for(NpcEntity n:actors)before.put(n.id,new Point(n.x,n.y));g.advanceTurnBody("");for(NpcEntity n:actors)if(!before.get(n.id).equals(new Point(n.x,n.y))){steps++;moved.add(n.id);}}
                    for(NpcEntity n:actors)positions.append(g.turn).append('\t').append(n.id).append('\t').append(n.combatTeam).append('\t').append(n.x).append('\t').append(n.y).append('\t').append(n.hp).append('\t').append(n.loadedShots).append('\n');
                    for(var e:ActorCombatAuthority.diagnostics(g).recent)if(e.turn()==g.turn){int protection=e.cover();if(protection>0 && Set.of(ActorCombatAuthority.Outcome.HIT,ActorCombatAuthority.Outcome.KILL,ActorCombatAuthority.Outcome.MISS).contains(e.outcome()))covered++;events.append(e.turn()).append('\t').append(e.attacker()).append('\t').append(e.target()).append('\t').append(e.weapon()).append('\t').append(e.outcome()).append('\t').append(e.chance()).append('\t').append(e.roll()).append('\t').append(e.damage()).append('\t').append(e.region()).append('\t').append(protection).append('\n');}
                    for(int sub=0;sub<3;sub++){g.putClientProperty("mechanist.captureClock",ORIGIN+turn*500L+sub*167L);for(boolean doom:new boolean[]{false,true}){g.options.doomModeEnabled=doom;BufferedImage image=new BufferedImage(1280,800,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();g.paint(graphics);graphics.dispose();ImageIO.write(image,"png",folder.resolve(doom?"doom":"2d").resolve(String.format(Locale.ROOT,"%05d.png",frames)).toFile());}frames++;}
                    if(turn>0 && !DeveloperCombatScenarioAuthority.result(g).endsWith("active"))break;
                }
                ActorCombatFeedbackAuthority.audioSink(g,null);audio.write(folder,frames*1000/6);Files.writeString(folder.resolve("positions.tsv"),positions);Files.writeString(folder.resolve("events.tsv"),events);Files.writeString(folder.resolve("cover.tsv"),cover);
                String result=DeveloperCombatScenarioAuthority.result(g);summary.append(String.join(",",c.id,""+c.size,c.preset,""+c.cover,"71000",""+g.turn,result,""+steps,""+moved.size(),""+covered,""+audio.count,""+audio.missing)).append('\n');Files.writeString(out.resolve("results.csv"),summary);
                System.out.println(c.id+" / "+result+" / movement steps "+steps+" / moving actors "+moved.size()+" / cover attacks "+covered+" / native cues "+audio.count+" / frames "+frames);
            }
        }finally{if(session!=null){ActorCombatFeedbackAuthority.audioSink(session.facility(),null);DeveloperFacilityAuthority.leave(session.facility());SwingUtilities.invokeAndWait(()->{});}campaign.singlePlayerSectorBridge.close();campaign.shutdownRuntime();}
    }
}
