package mechanist;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.sound.sampled.*;

/** Pitch/rate variation is cosmetic, repeatable when recorded, bounded, and independent of turn/combat state. */
final class WeaponSoundVariationSmoke {
    private static int checks;
    private static void require(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static double frequency(byte[] pcm){int crossings=0;short previous=0;for(int i=0;i<pcm.length;i+=4){short sample=(short)((pcm[i]&255)|((pcm[i+1]&255)<<8));if(previous<=0 && sample>0)crossings++;previous=sample;}return crossings/(pcm.length/4.0/44100);}
    public static void main(String[] args)throws Exception {
        Path out=Path.of(args.length==0?"diagnostics/native-battles-20261026":args[0]).resolve("weapon-audio/variation");Files.createDirectories(out);
        StringBuilder rows=new StringBuilder("ordinal\trate\tgain\tdelayMillis\n");int above=0,below=0;double mean=0;
        for(int i=0;i<1000;i++){var v=WeaponSoundVariationAuthority.fromSeed("weapon_las",i);require(v.rate()>=.94 && v.rate()<=1.06 && v.gain()>=.95 && v.gain()<=1.05 && v.delayMillis()>=0 && v.delayMillis()<=24,"Variation bounds");require(v.equals(WeaponSoundVariationAuthority.fromSeed("weapon_las",i)),"Recorded seed replay");if(v.rate()>1)above++;else below++;mean+=v.rate();rows.append(i).append('\t').append(v.rate()).append('\t').append(v.gain()).append('\t').append(v.delayMillis()).append('\n');}
        require(above>430 && above<570 && below>430 && below<570 && Math.abs(mean/1000-1)<.003,"Pitch/rate distribution must vary both above and below the source");
        require(WeaponSoundVariationAuthority.fromSeed("button",3).equals(WeaponSoundVariationAuthority.Variation.NEUTRAL),"UI and non-weapon cues remain neutral");
        byte[] sine=new byte[44100*4];for(int i=0;i<44100;i++){short value=(short)(Math.sin(2*Math.PI*1000*i/44100)*10000);for(int ch=0;ch<2;ch++){sine[i*4+ch*2]=(byte)value;sine[i*4+ch*2+1]=(byte)(value>>8);}}
        byte[] fast=WeaponSoundVariationAuthority.varyStereo(sine,new WeaponSoundVariationAuthority.Variation(1.06,1,0)),slow=WeaponSoundVariationAuthority.varyStereo(sine,new WeaponSoundVariationAuthority.Variation(.94,1,0));
        require(fast.length<sine.length && slow.length>sine.length && Math.abs(frequency(fast)-1060)<3 && Math.abs(frequency(slow)-940)<3,"Resampling must actually change duration and pitch");
        var budget=new WeaponSoundVariationAuthority.VoiceBudget(12);ArrayList<WeaponSoundVariationAuthority.VoiceBudget.Ticket> tickets=new ArrayList<>();for(int i=0;i<12;i++)tickets.add(budget.acquire(i+2));require(budget.active()==12 && budget.acquire(100)==null,"Live voice count is bounded");
        int[] cancelled={0};tickets.get(11).onCancel=()->cancelled[0]++;var close=budget.acquire(0);require(close!=null && tickets.get(11).cancelled && cancelled[0]==1 && budget.active()==12,"Near/player sounds replace a farther voice");for(var ticket:tickets)budget.release(ticket);budget.release(close);budget.release(close);require(budget.active()==0,"Voice release is idempotent");
        GamePanel game=new GamePanel();game.timer.stop();game.seed=41041;game.worldTurn=77;game.turn=77;game.rng=new Random(144);
        try {var expected=new Random(144);ArrayList<WeaponSoundVariationAuthority.Variation> first=new ArrayList<>();for(int i=0;i<100;i++)first.add(WeaponSoundVariationAuthority.next(game,"weapon_las",10,10));require(game.rng.nextLong()==expected.nextLong() && game.turn==77 && game.worldTurn==77,"Cosmetic randomness cannot advance combat RNG or simulation time");WeaponSoundVariationAuthority.reset(game);for(int i=0;i<100;i++)require(first.get(i).equals(WeaponSoundVariationAuthority.next(game,"weapon_las",10,10)),"Recording reset must reproduce each cue variation");}finally{game.shutdownRuntime();}
        SoundManager manager=new SoundManager();manager.load();var mix=SpatialAudioAuthority.mix(0,0,1,0,0,0,65,16,false);float[][] audio={new float[44100*4*2],new float[44100*4*2]};
        for(int i=0;i<12;i++)for(int variant=0;variant<2;variant++){var v=variant==0?WeaponSoundVariationAuthority.Variation.NEUTRAL:WeaponSoundVariationAuthority.fromSeed("weapon_las",i);var pcm=manager.renderPositioned("weapon_las",mix,true,v);int offset=(i*200+v.delayMillis())*44100/1000*2;for(int f=0;f<pcm.samples().length/4;f++)for(int ch=0;ch<2;ch++){int from=f*4+ch*2,at=offset+f*2+ch;if(at<audio[variant].length)audio[variant][at]+=(short)((pcm.samples()[from]&255)|((pcm.samples()[from+1]&255)<<8));}}
        float peak=1;for(var samples:audio)for(float sample:samples)peak=Math.max(peak,Math.abs(sample));double gain=Math.min(1,28000.0/peak);
        for(int variant=0;variant<2;variant++){byte[] pcm=new byte[audio[variant].length*2];for(int i=0;i<audio[variant].length;i++){short value=(short)(audio[variant][i]*gain);pcm[i*2]=(byte)value;pcm[i*2+1]=(byte)(value>>8);}try(AudioInputStream stream=new AudioInputStream(new ByteArrayInputStream(pcm),new AudioFormat(44100,16,2,true,false),pcm.length/4)){AudioSystem.write(stream,AudioFileFormat.Type.WAVE,out.resolve(variant==0?"identical.wav":"varied.wav").toFile());}}
        Files.writeString(out.resolve("variation-audit.tsv"),rows);String result="WeaponSoundVariationSmoke PASS checks="+checks+" above="+above+" below="+below+" meanRate="+mean/1000+" maxLiveVoices=12 simulationUnchanged=true";Files.writeString(out.resolve("results.txt"),result+"\n");
        Files.writeString(out.resolve("index.html"),"<!doctype html><meta charset=utf-8><title>Weapon sound variation</title><style>body{background:#142029;color:#e6e7d8;font:18px system-ui;max-width:900px;margin:30px auto;padding:20px}audio{width:100%}section{background:#263640;padding:20px;margin:18px 0}a{color:#a6d7ef}</style><h1>Repeated weapon sounds: identical and varied</h1><p>Same existing las WAV, same firing intervals and common peak-limiter gain. Variation changes pitch and playback speed together within +/-6%, volume within +/-5%, and onset by 0-24 ms. It never changes combat timing, movement allowance, turn advancement or combat RNG.</p><section><h2>Identical</h2><audio controls src=identical.wav></audio></section><section><h2>Varied</h2><audio controls src=varied.wav></audio></section><p>"+result+"</p><p><a href=variation-audit.tsv>Per-cue parameters</a> / <a href=../index.html>Weapon library</a></p>");System.out.println(result);
    }
}
