package mechanist;

import java.awt.*;
import java.util.*;
import java.util.List;

/** Presentation emitted by confirmed attacks; no gameplay RNG, damage or movement lives here. */
final class ActorCombatFeedbackAuthority {
    record Shot(int x,int y,int tx,int ty,String weapon,boolean ranged,boolean hit,long born) { }
    interface AudioSink {void cue(String key,int x,int y,boolean occluded,boolean enclosed,double strength);}
    private static final Map<GamePanel,ArrayDeque<Shot>> SHOTS=new WeakHashMap<>();
    private static final Map<GamePanel,AudioSink> AUDIO=new WeakHashMap<>();
    private static final PrimitiveParticleEmitter2D PARTICLES=new PrimitiveParticleEmitter2D();
    static long now(GamePanel game) {return game.getClientProperty("mechanist.captureClock") instanceof Long t?t:System.currentTimeMillis();}
    static void clear(GamePanel game) {SHOTS.remove(game);}
    static void audioSink(GamePanel game,AudioSink sink) {if(sink==null)AUDIO.remove(game);else AUDIO.put(game,sink);}
    static void recordAudio(GamePanel game,String key,int x,int y,boolean occluded,boolean enclosed,double strength) {AudioSink sink=AUDIO.get(game);if(sink!=null)sink.cue(key,x,y,occluded,enclosed,strength);}
    static void attack(GamePanel game,int x,int y,int tx,int ty,String weapon,boolean ranged,boolean hit) {
        var queue=SHOTS.computeIfAbsent(game,g->new ArrayDeque<>());queue.add(new Shot(x,y,tx,ty,weapon,ranged,hit,now(game)));while(queue.size()>128)queue.removeFirst();
        WorldPositionalSoundAuthority.playAttackAt(game,weapon,ranged,x,y);
    }
    static List<Shot> shots(GamePanel game) {var queue=SHOTS.get(game);if(queue==null)return List.of();long time=now(game);queue.removeIf(s->time-s.born>450);return List.copyOf(queue);}
    private record Point(double x,double y,double depth,boolean visible) { }
    private interface Project {Point at(double x,double y,double height);}
    static void render2D(Graphics2D g,GamePanel game,Rectangle viewport,int tile) {
        Graphics2D gg=(Graphics2D)g.create();
        try {gg.clip(viewport);
            render(gg,game,(x,y,z)->new Point(game.lastWorldViewOriginX+(x-game.lastWorldViewMinX)*tile,game.lastWorldViewOriginY+(y-game.lastWorldViewMinY)*tile,0,true),null);
        } finally {gg.dispose();}
    }
    static void renderDoom(Graphics2D g,GamePanel game,FirstPersonCamera camera,int w,int h,double[] walls) {
        render(g,game,(x,y,height)->{ScreenPoint p=FirstPersonBillboardAuthority.project(camera,x,height,y,Math.toRadians(game.options.doomModeFovDegrees),w,h);return new Point(p.x(),p.y(),p.depth(),p.visible());},walls);
    }
    private static void render(Graphics2D g,GamePanel game,Project project,double[] walls) {
        for(Shot s:shots(game)) {
            long age=Math.max(0,now(game)-s.born);int color=switch(WeaponAudioAuthority.cue(s.weapon,s.ranged)){case "weapon_las"->0xFFFF6860;case "weapon_plasma"->0xFF62EBD5;default->0xFFFFC46B;};
            Point previous=null;
            if(s.ranged && age<230)for(int i=0;i<=40;i++) {
                double t=i/40.0,x=s.x+.5+(s.tx-s.x)*t,y=s.y+.5+(s.ty-s.y)*t;
                Point p=project.at(x,y,.65);boolean visible=game.isVisible((int)x,(int)y) && visible(p,walls);
                if(visible && previous!=null){g.setColor(new Color(color,true));g.setStroke(new BasicStroke(2));g.drawLine((int)previous.x,(int)previous.y,(int)p.x,(int)p.y);}previous=visible?p:null;
            }
            if(age<200 && game.isVisible(s.x,s.y)) emit(g,project.at(s.x+.5,s.y+.5,.65),walls,age,false,color,s.tx-s.x,s.ty-s.y);
            if(s.hit && game.isVisible(s.tx,s.ty)) emit(g,project.at(s.tx+.5,s.ty+.5,.65),walls,age,true,color,0,0);
        }
        g.setStroke(new BasicStroke(1));
    }
    private static boolean visible(Point p,double[] walls) {if(!p.visible)return false;if(walls==null)return true;int x=(int)p.x;return x>=0 && x<walls.length && p.depth<=walls[x]+.08;}
    private static void emit(Graphics2D g,Point p,double[] walls,long age,boolean impact,int color,int dx,int dy) {
        if(!visible(p,walls))return;PARTICLES.clear();
        PARTICLES.spawnWeaponFire(impact?new WeaponFireProfile.ImpactBurst(12,55,color):new WeaponFireProfile.LasBeam(6,35,color),(float)p.x,(float)p.y,dx,dy);
        float remaining=age/1000f;while(remaining>0){float step=Math.min(.1f,remaining);PARTICLES.update(step);remaining-=step;}PARTICLES.render(g);
    }
}
