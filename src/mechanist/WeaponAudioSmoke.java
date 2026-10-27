package mechanist;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import javax.sound.sampled.*;

/** Family routing, real attack emitters, file-backed PCM and mute/gain contracts. */
final class WeaponAudioSmoke {
    private static int checks;
    private static void require(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        Path output=Path.of(args.length==0?"diagnostics/native-battles-20261026":args[0]).resolve("weapon-audio");Files.createDirectories(output);
        SoundManager manager=new SoundManager();manager.load();
        LinkedHashMap<String,String> expected=new LinkedHashMap<>();
        expected.put("Light Rifle","weapon_las");expected.put("Guard lascarbine","weapon_las");expected.put("Lasgun","weapon_las");
        expected.put("Plasma Pistol","weapon_plasma");expected.put("Hand Flamer","weapon_flame");expected.put("Meltagun","weapon_flame");
        expected.put("Arc rifle","weapon_lightning");expected.put("Shock baton","weapon_lightning");expected.put("Chainsword","weapon_chain");expected.put("Chainaxe","weapon_chain");
        expected.put("Knife","weapon_slice");expected.put("Thunder Hammer","weapon_thundering");expected.put("Heavy stubber","weapon_thundering");expected.put("Bolter","weapon_thundering");
        expected.put("Autogun","weapon_shot");expected.put("Stub pistol","weapon_shot");expected.put("Bare hands","weapon_bash");expected.put("Frag grenade","weapon_thundering");expected.put("Unknown blast sword","weapon_slice");
        for(var e:expected.entrySet())for(String quality:ItemQuality.NAMES)require(manager.weaponEffectKey(quality+" "+e.getKey(),StructuralScaleCombatAuthority.weaponProfile(e.getKey(),"SNAP").firearm()).equals(e.getValue()),"Weapon family must survive quality prefixes: "+quality+" "+e.getKey());
        StringBuilder audit=new StringBuilder("weapon\tcategory\tcue\tasset\n");int weapons=0;
        for(ItemDef def:ItemCatalog.ITEMS.values())if(def.weapon){weapons++;String cue=manager.weaponEffectKey(def.name,StructuralScaleCombatAuthority.weaponProfile(def.name,"SNAP").firearm());require(manager.sounds.containsKey(cue),"Catalog weapon requires a real packaged effect: "+def.name);audit.append(def.name).append('\t').append(def.category).append('\t').append(cue).append('\t').append(manager.sounds.get(cue).getPath()).append('\n');}
        Files.writeString(output.resolve("weapon-bindings.tsv"),audit);
        StringBuilder pcmAudit=new StringBuilder("cue\tasset\tframes\tsampleRate\tpeak\trms\tpcmSha256\n"),cards=new StringBuilder();Set<String> digests=new HashSet<>();
        for(String cue:List.of("weapon_las","weapon_plasma","weapon_flame","weapon_lightning","weapon_shot","weapon_thundering","weapon_slice","weapon_bash","weapon_chain","weapon_chain_motor","weapon_reload")) {
            require(manager.sounds.containsKey(cue) && !manager.generatedCueKeys.contains(cue),"Existing asset must be loaded: "+cue);
            var pcm=manager.renderPositioned(cue,SpatialAudioAuthority.mix(0,0,1,0,0,0,80,16,false),false);
            long sum=0;int peak=0;for(int i=0;i<pcm.samples().length;i+=2){int sample=(short)((pcm.samples()[i]&255)|((pcm.samples()[i+1]&255)<<8));peak=Math.max(peak,Math.abs(sample));sum+=(long)sample*sample;}
            double rms=Math.sqrt(sum/(double)(pcm.samples().length/2));require(peak>100 && rms>10,"Packaged cue contains audible PCM: "+cue);
            String digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pcm.samples()));require(digests.add(digest),"Different weapon families must not resolve to the same PCM: "+cue);
            AudioFormat format=new AudioFormat(pcm.sampleRate(),16,2,true,false);try(AudioInputStream stream=new AudioInputStream(new ByteArrayInputStream(pcm.samples()),format,pcm.samples().length/4)){AudioSystem.write(stream,AudioFileFormat.Type.WAVE,output.resolve(cue+".wav").toFile());}
            pcmAudit.append(cue).append('\t').append(manager.sounds.get(cue).getPath()).append('\t').append(pcm.samples().length/4).append('\t').append(pcm.sampleRate()).append('\t').append(peak).append('\t').append(rms).append('\t').append(digest).append('\n');
            cards.append("<section><h2>").append(cue.replace("weapon_","").replace('_',' ')).append("</h2><audio controls preload=\"metadata\" src=\"").append(cue).append(".wav\"></audio><p>").append(manager.sounds.get(cue).getName()).append("</p></section>");
        }
        Files.writeString(output.resolve("pcm-audit.tsv"),pcmAudit);
        GameOptions options=new GameOptions();options.volume=50;options.sfxVolume=80;options.soundEnabled=true;require(SoundManager.effectiveCueVolume("weapon_las",options)==40,"Master and SFX gains combine");options.sfxVolume=0;require(SoundManager.effectiveCueVolume("weapon_las",options)==0,"SFX mute must be silent, not a one-percent floor");options.sfxVolume=80;options.volume=0;require(SoundManager.effectiveCueVolume("weapon_las",options)==0,"Master mute");options.volume=100;options.soundEnabled=false;require(SoundManager.effectiveCueVolume("weapon_las",options)==0,"SFX disabled");
        GamePanel game=new GamePanel();game.timer.stop();game.world=new World(17017,32,24);game.world.zoneType=ZoneType.NEUTRAL_CIVILIAN_FLOOR;for(char[] column:game.world.tiles)Arrays.fill(column,'.');game.active=Candidate.random(new Random(3));game.screen=GamePanel.Screen.GAME;game.options.soundEnabled=true;game.options.musicEnabled=false;game.options.volume=80;game.options.sfxVolume=70;
        try {
            for(String weapon:List.of("Light Rifle","Plasma Pistol","Hand Flamer","Shock baton","Chainsword","Knife","Bolter","Stub pistol","Bare hands")) {
                boolean ranged=StructuralScaleCombatAuthority.weaponProfile(weapon,"SNAP").firearm();String expectedCue=expected.get(weapon);game.world.npcs.clear();game.combatSupport.restore("");game.playerX=4;game.playerY=10;
                NpcEntity shooter=actor("AUDIO-SHOOTER","A",10,10),target=actor("AUDIO-TARGET","B",ranged?12:11,10);shooter.equippedMeleeWeapon=weapon;shooter.equippedRangedWeapon=ranged?weapon:"";shooter.loadedShots=8;game.world.npcs.add(shooter);game.world.npcs.add(target);game.turn++;shooter.lastTickTurn=-1;
                ArrayList<String> npcCues=new ArrayList<>();ActorCombatFeedbackAuthority.audioSink(game,(cue,x,y,occluded,room,strength)->{if(x==shooter.x && y==shooter.y)npcCues.add(cue);});
                ActorCombatAuthority.takeNpcTurn(game,shooter);require(npcCues.equals(List.of(expectedCue)),"Actual NPC attack must emit its family: "+weapon+" "+npcCues);
                game.world.npcs.remove(shooter);game.playerX=8;target.x=ranged?10:9;target.y=10;target.hp=10000;game.combatX=target.x;game.combatY=target.y;game.equippedLeftHandItem="";game.equippedRightHandItem=weapon;game.activeWeaponHandIndex=1;game.loadedWeaponShots.put(weapon,8);ArrayList<String> playerCues=new ArrayList<>();
                ActorCombatFeedbackAuthority.audioSink(game,(cue,x,y,occluded,room,strength)->{if(x==game.playerX && y==game.playerY)playerCues.add(cue);});ActorCombatAuthority.confirmPlayer(game);
                require(playerCues.equals(List.of(expectedCue)),"Actual player attack must emit the same family: "+weapon+" "+playerCues);
            }
        }finally{ActorCombatFeedbackAuthority.audioSink(game,null);game.shutdownRuntime();}
        String result="WeaponAudioSmoke PASS checks="+checks+" catalogWeapons="+weapons+" fileBackedCues=11 realPlayerNpcFamilies=9 masterAndSfxMute=true";Files.writeString(output.resolve("results.txt"),result+"\n");
        Files.writeString(output.resolve("index.html"),"<!doctype html><meta charset=\"utf-8\"><title>The Mechanist weapon sounds</title><style>body{background:#111a20;color:#e3e9df;font:17px system-ui;max-width:1000px;margin:28px auto;padding:0 22px}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:16px}section{background:#22303a;padding:16px;border:1px solid #4e666c}h2{text-transform:capitalize}p{line-height:1.5}audio{width:100%}a{color:#a8d7f2}</style><h1>Existing weapon sound library</h1><p>Actual packaged WAVs, decoded through the game's positional stereo renderer. Light Rifle now uses las; chain weapons use the existing chainsaw strike recording. The motor sample is available for audition; automatic motor-loop state is not implemented.</p><p>"+result+"</p><main>"+cards+"</main><p><a href=\"weapon-bindings.tsv\">Full catalog bindings</a> / <a href=\"pcm-audit.tsv\">PCM energy and hashes</a> / <a href=\"../index.html\">Native battle videos</a></p>");System.out.println(result);
    }
    private static NpcEntity actor(String id,String team,int x,int y){NpcEntity n=NpcEntity.create(team.equals("A")?Faction.IMPERIAL_GUARD:Faction.CULTIST,ZoneType.GANGER_TURF,x,y,new Random(7));n.id=id;n.combatTeam=team;n.role="Combatant";n.state="Hostile";n.ageYears=35;n.ageBand="adult";n.hp=10000;n.combatLoadoutInitialized=true;n.combatSupport.items.clear();return n;}
}
