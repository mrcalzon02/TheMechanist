package mechanist;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Cosmetic variation only: never reads or advances combat RNG or any simulation clock. */
final class WeaponSoundVariationAuthority {
    static final class VoiceBudget {
        static final class Ticket {
            final double distance;
            volatile boolean cancelled;
            volatile Runnable onCancel=()->{};
            Ticket(double distance){this.distance=distance;}
        }
        private final int maximum;
        private final ArrayList<Ticket> active=new ArrayList<>();
        VoiceBudget(int maximum){this.maximum=Math.max(1,maximum);}
        Ticket acquire(double distance) {
            Ticket evicted=null,result;
            synchronized(this) {
                if(active.size()>=maximum){Ticket worst=active.stream().max(Comparator.comparingDouble(t->t.distance)).orElseThrow();if(distance>worst.distance && distance>1)return null;active.remove(worst);worst.cancelled=true;evicted=worst;}
                result=new Ticket(distance);active.add(result);
            }
            if(evicted!=null)evicted.onCancel.run();return result;
        }
        synchronized void release(Ticket ticket){active.remove(ticket);}
        synchronized int active(){return active.size();}
    }
    record Variation(double rate,double gain,int delayMillis) {
        static final Variation NEUTRAL=new Variation(1,1,0);
    }
    private static final Map<GamePanel,Long> ORDINALS=new WeakHashMap<>();
    static void reset(GamePanel game){ORDINALS.remove(game);}
    static Variation next(GamePanel game,String cue,int x,int y) {
        if(!weapon(cue))return Variation.NEUTRAL;
        long ordinal=ORDINALS.merge(game,1L,Long::sum);
        return fromSeed(cue,Objects.hash(game.seed,game.worldTurn,cue,x,y,ordinal));
    }
    static Variation next(String cue){return weapon(cue)?fromSeed(cue,ThreadLocalRandom.current().nextLong()):Variation.NEUTRAL;}
    static boolean weapon(String cue){return cue!=null && cue.startsWith("weapon_");}
    static Variation fromSeed(String cue,long seed) {
        if(!weapon(cue))return Variation.NEUTRAL;
        long mixed=seed+0x9E3779B97F4A7C15L;mixed=(mixed^(mixed>>>30))*0xBF58476D1CE4E5B9L;mixed=(mixed^(mixed>>>27))*0x94D049BB133111EBL;
        Random cosmetic=new Random(mixed^(mixed>>>31));
        double spread=cue.equals("weapon_chain_motor")?.02:cue.equals("weapon_reload")?.03:.06;
        return new Variation(1+(cosmetic.nextDouble()*2-1)*spread,.95+cosmetic.nextDouble()*.10,cosmetic.nextInt(25));
    }
    /** Linear resampling changes pitch and playback speed together, retaining standard device sample rates. */
    static byte[] varyStereo(byte[] pcm,Variation variation) {
        if(pcm==null || pcm.length%4!=0)throw new IllegalArgumentException("Stereo 16-bit PCM required");
        if(pcm.length==0)return pcm;
        double rate=Math.max(.90,Math.min(1.10,variation.rate)),gain=Math.max(.85,Math.min(1.15,variation.gain));
        int sourceFrames=pcm.length/4,frames=Math.max(1,(int)Math.round(sourceFrames/rate));byte[] result=new byte[frames*4];
        for(int i=0;i<frames;i++){double source=Math.min(sourceFrames-1,i*rate);int a=(int)source,b=Math.min(sourceFrames-1,a+1);double t=source-a;
            for(int ch=0;ch<2;ch++){int ai=a*4+ch*2,bi=b*4+ch*2;short av=sample(pcm,ai),bv=sample(pcm,bi);int value=(int)Math.round((av+(bv-av)*t)*gain);value=Math.max(-32768,Math.min(32767,value));result[i*4+ch*2]=(byte)value;result[i*4+ch*2+1]=(byte)(value>>8);}}
        return result;
    }
    private static short sample(byte[] pcm,int at){return(short)((pcm[at]&255)|((pcm[at+1]&255)<<8));}
    private WeaponSoundVariationAuthority() { }
}
