package mechanist;

import java.awt.*;
import java.util.*;
import java.util.List;

/** Presentation emitted by confirmed attacks; no gameplay RNG, damage or movement lives here. */
final class ActorCombatFeedbackAuthority {
    record Shot(int x,int y,int tx,int ty,String weapon,boolean ranged,boolean hit,long born) { }
    interface AudioSink {void cue(String key,int x,int y,boolean occluded,boolean enclosed,double strength);default void cue(String key,int x,int y,boolean occluded,boolean enclosed,double strength,WeaponSoundVariationAuthority.Variation variation){cue(key,x,y,occluded,enclosed,strength);}}
    private static final Map<GamePanel,ArrayDeque<Shot>> SHOTS=new WeakHashMap<>();
    private static final Map<GamePanel,AudioSink> AUDIO=new WeakHashMap<>();
    private static final PrimitiveParticleEmitter2D PARTICLES=new PrimitiveParticleEmitter2D();
    static long now(GamePanel game) {return game.getClientProperty("mechanist.captureClock") instanceof Long t?t:System.currentTimeMillis();}
    static void clear(GamePanel game) {SHOTS.remove(game);WeaponSoundVariationAuthority.reset(game);}
    static void audioSink(GamePanel game,AudioSink sink) {if(sink==null)AUDIO.remove(game);else AUDIO.put(game,sink);}
    static void recordAudio(GamePanel game,String key,int x,int y,boolean occluded,boolean enclosed,double strength,WeaponSoundVariationAuthority.Variation variation) {AudioSink sink=AUDIO.get(game);if(sink!=null)sink.cue(key,x,y,occluded,enclosed,strength,variation);}
    static void attack(GamePanel game,int x,int y,int tx,int ty,String weapon,boolean ranged,boolean hit) {
        var variation=WorldPositionalSoundAuthority.playAttackAt(game,weapon,ranged,x,y);
        var queue=SHOTS.computeIfAbsent(game,g->new ArrayDeque<>());queue.add(new Shot(x,y,tx,ty,weapon,ranged,hit,now(game)+variation.delayMillis()));while(queue.size()>128)queue.removeFirst();
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
    private static final BasicStroke CORE=new BasicStroke(1.3f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND),BODY=new BasicStroke(3f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND),GLOW=new BasicStroke(7f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
    private static void render(Graphics2D original,GamePanel game,Project project,double[] walls) {
        Graphics2D g=(Graphics2D)original.create();VisualJuiceFramework.applyHighQualityHints(g);
        try {for(Shot s:shots(game)) {
            long age=now(game)-s.born;if(age<0 || age>430)continue;
            String cue=WeaponAudioAuthority.cue(s.weapon,s.ranged);String name=s.weapon.toLowerCase(Locale.ROOT);
            int seed=Objects.hash(s.x,s.y,s.tx,s.ty,s.weapon,s.born);
            int color=cue.equals("weapon_las")?0xFFFF4A38:cue.equals("weapon_plasma")?0xFF58E5F0:cue.equals("weapon_lightning")?0xFF9DCCFF:0xFFFFB95D;
            double distance=Math.max(.01,Math.hypot(s.tx-s.x,s.ty-s.y)),nx=(s.tx-s.x)/distance,ny=(s.ty-s.y)/distance;
            double miss=s.hit?0:((seed&1)==0?.35:-.35);
            if(!s.ranged) {
                if(age<155)for(int i=0;i<12;i++){double phase=(age/155.0+i/50.0),t=Math.max(0,Math.min(1,phase));double lateral=Math.sin(t*Math.PI)*.24;Point p=at(project,s,.30+t*.60,lateral,nx,ny);if(game.isVisible(s.tx,s.ty) && visible(p,walls))dot(g,p,2.4,color,1-age/170.0);}
                if(s.hit && age>=55)impact(g,game,project,walls,s,age-55,color,nx,ny);continue;
            }
            if(age<65 && game.isVisible(s.x,s.y)){Point muzzle=at(project,s,.04,0,nx,ny);if(visible(muzzle,walls))flash(g,muzzle,color,1-age/65.0);}
            if(cue.equals("weapon_las")) {
                if(age<85)path(g,game,project,walls,s,0,1,miss,nx,ny,color,1-age/85.0,true,seed,0);
                if(s.hit && age>=20)impact(g,game,project,walls,s,age-20,color,nx,ny);
            } else if(cue.equals("weapon_lightning")) {
                if(age<125)path(g,game,project,walls,s,0,1,miss,nx,ny,color,1-age/125.0,true,seed,.075);
                if(s.hit && age>=35)impact(g,game,project,walls,s,age-35,color,nx,ny);
            } else if(cue.equals("weapon_flame")) {
                if(age<320){double advance=Math.min(1,age/130.0);for(int i=0;i<24;i++){double t=advance*(i+1)/24,spread=Math.sin(seed+i*3.71+age*.014)*t*.20;Point p=at(project,s,t,spread,nx,ny);int x=(int)(s.x+.5+(s.tx-s.x)*t),y=(int)(s.y+.5+(s.ty-s.y)*t);if(game.isVisible(x,y) && visible(p,walls))dot(g,p,3+t*8,i%3==0?0xFFFFE1A0:0xFFFF702A,(1-age/340.0)*.65);}}
                if(s.hit && age>=130)impact(g,game,project,walls,s,age-130,0xFFFFA040,nx,ny);
            } else {
                double flight=cue.equals("weapon_plasma")?Math.min(250,100+distance*14):Math.min(150,40+distance*7),t=Math.min(1,age/flight);
                if(age<flight){
                    if(cue.equals("weapon_plasma")){for(int i=6;i>=0;i--){double tail=Math.max(0,t-i*.025);Point p=at(project,s,tail,miss*tail,nx,ny);if(visible(p,walls)&&game.isVisible((int)(s.x+.5+(s.tx-s.x)*tail),(int)(s.y+.5+(s.ty-s.y)*tail)))dot(g,p,i==0?8:Math.max(1,4-i*.4),color,i==0?1:.32);} }
                    else if(name.contains("shotgun")){for(int pellet=0;pellet<7;pellet++)path(g,game,project,walls,s,Math.max(0,t-.16),t,miss+(pellet-3)*.045,nx,ny,color,1,false,seed,0);}
                    else path(g,game,project,walls,s,Math.max(0,t-(cue.equals("weapon_thundering")?.18:.10)),t,miss,nx,ny,color,1,false,seed,0);
                }
                if(s.hit && age>=flight)impact(g,game,project,walls,s,(long)(age-flight),color,nx,ny);
            }
        }}finally{g.dispose();}
    }
    private static Point at(Project project,Shot s,double t,double lateral,double nx,double ny){return project.at(s.x+.5+(s.tx-s.x)*t-ny*lateral,s.y+.5+(s.ty-s.y)*t+nx*lateral,.62);}
    private static void path(Graphics2D g,GamePanel game,Project project,double[] walls,Shot s,double from,double to,double lateral,double nx,double ny,int color,double alpha,boolean glow,int seed,double jagged) {
        Point previous=null;
        for(int i=0;i<=32;i++){double t=from+(to-from)*i/32,wiggle=jagged*Math.sin(i*2.41+seed)*Math.sin(t*Math.PI);Point p=at(project,s,t,lateral*t+wiggle,nx,ny);int x=(int)(s.x+.5+(s.tx-s.x)*t-ny*(lateral*t+wiggle)),y=(int)(s.y+.5+(s.ty-s.y)*t+nx*(lateral*t+wiggle));boolean draw=game.isVisible(x,y)&&visible(p,walls);if(draw && previous!=null){if(glow){line(g,previous,p,GLOW,color,alpha*.13);line(g,previous,p,BODY,color,alpha*.6);}line(g,previous,p,CORE,glow?0xFFFFF4DD:color,alpha);}previous=draw?p:null;}
    }
    private static void line(Graphics2D g,Point a,Point b,BasicStroke stroke,int color,double alpha){g.setStroke(stroke);g.setColor(new Color((color&0xffffff)|((int)(Math.max(0,Math.min(1,alpha))*255)<<24),true));g.drawLine((int)a.x,(int)a.y,(int)b.x,(int)b.y);}
    private static boolean visible(Point p,double[] walls){if(!p.visible)return false;if(walls==null)return true;int x=(int)p.x;return x>=0 && x<walls.length && p.depth<=walls[x]+.08;}
    private static void dot(Graphics2D g,Point p,double radius,int color,double alpha){if(alpha<=0)return;float r=(float)Math.max(1,Math.min(18,p.depth>0?radius/Math.sqrt(Math.max(.5,p.depth)):radius));float a=(float)Math.max(0,Math.min(1,alpha));g.setPaint(new RadialGradientPaint((float)p.x,(float)p.y,r*2,new float[]{0,.22f,1},new Color[]{new Color(255,250,225,(int)(220*a)),new Color((color>>16)&255,(color>>8)&255,color&255,(int)(180*a)),new Color(color&0xffffff,true)}));g.fillOval((int)(p.x-r*2),(int)(p.y-r*2),(int)(r*4),(int)(r*4));}
    private static void flash(Graphics2D g,Point p,int color,double alpha){dot(g,p,9,color,alpha);g.setColor(new Color(255,239,181,(int)(220*alpha)));int r=Math.max(2,(int)(7*alpha));g.fillPolygon(new int[]{(int)p.x-r,(int)p.x,(int)p.x+r,(int)p.x},new int[]{(int)p.y,(int)p.y-r,(int)p.y,(int)p.y+r},4);}
    private static void impact(Graphics2D g,GamePanel game,Project project,double[] walls,Shot s,long age,int color,double nx,double ny){if(age>190 || !game.isVisible(s.tx,s.ty))return;Point p=at(project,s,1,0,nx,ny);if(!visible(p,walls))return;dot(g,p,5,color,(1-age/200.0)*.65);PARTICLES.clear();PARTICLES.spawnWeaponFire(new WeaponFireProfile.ImpactBurst(8,45,color),(float)p.x,(float)p.y,0,0);float remaining=age/1000f;while(remaining>0){float step=Math.min(.1f,remaining);PARTICLES.update(step);remaining-=step;}PARTICLES.render(g);}
}
