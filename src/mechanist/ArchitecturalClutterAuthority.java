package mechanist;

import java.awt.*;
import java.util.*;
import java.util.List;

/** Seeded architectural accretion, attached to existing walls/undersides rather than actor slots. */
final class ArchitecturalClutterAuthority {
    static final int VERSION=1;
    enum Feature { RISER, SERVICE_PIPE, CABLE, SUPPORT, SEALED_DOOR, PATCH, VENT, LAMP, DUCT, HANGER, SWAG }
    record Piece(Feature feature,double x0,double h0,double z0,double x1,double h1,double z1,int tint,char art,int generation) {
        boolean emissive(){return feature==Feature.LAMP;}
        boolean round(){return feature==Feature.RISER || feature==Feature.SERVICE_PIPE || feature==Feature.CABLE || feature==Feature.SWAG;}
        int longAxis(){return h1-h0>x1-x0 && h1-h0>z1-z0?2:x1-x0>z1-z0?0:1;}
    }
    private static final Piece[] EMPTY=new Piece[0];
    static final class Layout {
        final Piece[][] cells;
        int revision=Integer.MIN_VALUE;
        Layout(World w){cells=new Piece[w.w*w.h][];}
    }
    private ArchitecturalClutterAuthority() { }
    static void enable(World w,boolean enabled) {
        w.architecturalClutterVersion=enabled?VERSION:0;
        w.architecturalClutter=null;w.dirtyVisionRevision++;w.dirtyLightRevision++;
        synchronizeLights(w);
    }
    static void prepare(World w) {
        if(w.architecturalClutterVersion==0)return;
        if(w.architecturalClutter==null)w.architecturalClutter=new Layout(w);
        Layout layout=w.architecturalClutter;
        if(layout.revision!=w.dirtyVisionRevision){Arrays.fill(layout.cells,null);layout.revision=w.dirtyVisionRevision;}
    }
    static Piece[] pieces(World w,int x,int z) {
        if(w.architecturalClutterVersion==0 || !w.inBounds(x,z))return EMPTY;
        prepare(w);int key=z*w.w+x;Piece[] cached=w.architecturalClutter.cells[key];
        if(cached==null)w.architecturalClutter.cells[key]=cached=derive(w,x,z);
        return cached;
    }
    private static boolean host(World w,int x,int z) {
        if(!w.inBounds(x,z) || (w.tiles[x][z]!='#' && w.tiles[x][z]!='W'))return false;
        return TileStructureAuthority.at(w,x,z).wall()==TileStructureAuthority.Wall.SOLID;
    }
    private static boolean floor(World w,int x,int z) {
        return w.inBounds(x,z) && w.walkable(x,z) && StairTileArtAuthority.direction(w,x,z)==null
                && TileStructureAuthority.at(w,x,z).floor()!=TileStructureAuthority.Floor.VOID;
    }
    private static long hash(World w,int a,int b,int salt) {
        long v=w.seed^((long)a*0x632BE59BD9B4E019L)^((long)b*0x9E3779B97F4A7C15L)^((long)salt*0xD1B54A32D192ED03L);
        v=(v^(v>>>30))*0xBF58476D1CE4E5B9L;v=(v^(v>>>27))*0x94D049BB133111EBL;return v^(v>>>31);
    }
    private static int mod(long value,int divisor){return (int)Math.floorMod(value,divisor);}
    private static int tint(int generation){return switch(generation%4){case 0->0x716154;case 1->0x526F68;case 2->0x455666;default->0xAF8A48;};}
    private static Piece[] derive(World w,int x,int z) {
        if(!floor(w,x,z))return EMPTY;
        ArrayList<Piece> out=new ArrayList<>();double ground=DeveloperBattleMapAuthority.height(w,x,z);
        for(int face=0;face<4;face++) {
            int wx=x+(face==1?1:face==3?-1:0),wz=z+(face==2?1:face==0?-1:0);
            if(!host(w,wx,wz))continue;
            var m=TileStructureAuthority.at(w,wx,wz);
            double wallBase=DeveloperBattleMapAuthority.height(w,wx,wz),top=Math.min(10,m.wallTopElevation());
            if(Math.abs(wallBase-ground)>.26 || top<=ground+.3)continue;
            // A face shares route/age choices with its neighbors along the same wall line.
            int along=face%2==0?x:z,line=face%2==0?wz:wx;
            long route=hash(w,line,face,31);int offset=mod(route,6);
            if(Math.floorMod(along-offset,6)==0) {
                add(out,x,z,face,Feature.RISER,.12,ground+.1,.03,.22,top-.04,.15,tint(1),'#',1);
                for(double h=ground+.3;h<top-.1;h+=1.15)
                    add(out,x,z,face,Feature.SUPPORT,.07,h,.015,.27,h+.065,.19,0x7C7D75,'#',2);
            }
            // Legacy lower service run and newer upper runs remain connected across tile boundaries.
            for(int band=0;band<4;band++) {
                double h=ground+(band==0?.24:band==1?1.25:band==2?3.45:6.65);
                if(h+.14>=top)continue;int age=mod(route+band,4);
                add(out,x,z,face,Feature.SERVICE_PIPE,0,h,.025,1,h+.09,.115,tint(age),'#',age);
                if(band>0)add(out,x,z,face,Feature.CABLE,0,h+.14,.03,1,h+.17,.085,0x272F35,'#',3);
                if(Math.floorMod(along,3)==offset%3)
                    add(out,x,z,face,Feature.SUPPORT,.46,h-.025,.01,.53,h+.21,.16,0x8A8171,'#',age);
            }
            int segment=Math.floorDiv(along,3),slot=Math.floorMod(along,3);long old=hash(w,line,segment,face+107);
            if(slot==mod(old,3)) {
                int choice=mod(old>>>8,4);
                if(choice==0 && top>ground+1.05) {
                    // Permanently sealed relief over an existing solid wall, never a usable door.
                    add(out,x,z,face,Feature.SEALED_DOOR,.19,ground+.04,.005,.81,Math.min(top-.08,ground+1.02),.045,tint(0),'D',0);
                    add(out,x,z,face,Feature.PATCH,.15,ground+.45,.045,.85,ground+.55,.075,0x92734B,'#',2);
                } else if(choice==1) {
                    double h=ground+.52;
                    if(h+.35<top) {
                        add(out,x,z,face,Feature.VENT,.3,h,.015,.85,h+.35,.11,0x303F47,'#',1);
                        for(int rib=0;rib<4;rib++)add(out,x,z,face,Feature.SUPPORT,.31,h+.03+rib*.08,.11,.84,h+.05+rib*.08,.13,0x8C9189,'#',2);
                    }
                } else {
                    double h=ground+.66;
                    if(h+.3<top)add(out,x,z,face,Feature.PATCH,.28,h,.015,.88,h+.3,.055,tint(choice),'#',choice);
                }
            }
            for(int band=1;band<=3;band++) {
                double h=ground+band*2.3;if(h+.45>=top)break;
                if(mod(hash(w,line,segment,face+band*19),3)==slot)
                    add(out,x,z,face,Feature.PATCH,.3,h,.005,.94,h+.42,.05,tint(band),'#',band);
            }
            if(lamp(w,x,z,face) && top>ground+1.03) {
                add(out,x,z,face,Feature.SUPPORT,.3,ground+.88,.01,.75,ground+1.04,.15,0x3C4348,'#',3);
                add(out,x,z,face,Feature.LAMP,.34,ground+.91,.15,.71,ground+.98,.18,0xBAE8EB,'#',3);
            }
        }
        var m=TileStructureAuthority.at(w,x,z);
        double ceiling=m.roof()==TileStructureAuthority.Roof.OPEN?TileStructureAuthority.NEXT_LEVEL_UNDERSIDE:ground+m.roofClearance();
        // Underside services attach to opaque slabs; glass and open grating keep their sight apertures.
        if((m.roof()==TileStructureAuthority.Roof.COVERED || m.roof()==TileStructureAuthority.Roof.OPEN) && ceiling-ground>1.12) {
            int overheadStart=out.size();
            boolean eastWest=mod(hash(w,Math.floorDiv(x,8),Math.floorDiv(z,8),53),2)==0;
            int along=eastWest?x:z,cross=eastWest?z:x;
            long route=hash(w,Math.floorDiv(cross,4),eastWest?0:1,71);
            if(Math.floorMod(cross,4)==mod(route,4)) {
                horizontal(out,x,z,eastWest,Feature.DUCT,0,.34,1,.62,ceiling-.19,ceiling-.04,0x5E696C,2);
                if(Math.floorMod(along,3)==0) {
                    horizontal(out,x,z,eastWest,Feature.HANGER,.45,.28,.51,.68,ceiling-.25,ceiling-.2,0x948F7C,3);
                    horizontal(out,x,z,eastWest,Feature.HANGER,.46,.29,.5,.32,ceiling-.21,ceiling,0x948F7C,3);
                    horizontal(out,x,z,eastWest,Feature.HANGER,.46,.64,.5,.67,ceiling-.21,ceiling,0x948F7C,3);
                }
            }
            if(Math.floorMod(cross,5)==mod(route>>>8,5)) {
                for(int part=0;part<6;part++) {
                    double sag=.08+.15*Math.sin((part+.5)*Math.PI/6);
                    horizontal(out,x,z,eastWest,Feature.SWAG,part/6.0,.76,(part+1)/6.0,.79,ceiling-sag-.025,ceiling-sag,0x292F34,1);
                }
            }
            if(Math.floorMod(along,5)==mod(route>>>16,5))
                horizontal(out,x,z,!eastWest,Feature.SUPPORT,0,.06,1,.14,ceiling-.105,ceiling-.02,0x82765E,0);
            // Existing character billboards stand 1.1 high. Flatten services in low rooms,
            // preserving at least 1.12 clear units rather than clipping characters' heads.
            double scale=Math.min(1,(ceiling-ground-1.12)/.25);
            if(scale<1)for(int i=overheadStart;i<out.size();i++) {
                Piece p=out.get(i);
                out.set(i,new Piece(p.feature(),p.x0(),ceiling-(ceiling-p.h0())*scale,p.z0(),p.x1(),ceiling-(ceiling-p.h1())*scale,p.z1(),p.tint(),p.art(),p.generation()));
            }
        }
        return out.isEmpty()?EMPTY:out.toArray(Piece[]::new);
    }
    private static void horizontal(List<Piece> out,int x,int z,boolean eastWest,Feature feature,double a0,double b0,double a1,double b1,double bottom,double top,int tint,int age) {
        out.add(new Piece(feature,x+(eastWest?a0:b0),bottom,z+(eastWest?b0:a0),x+(eastWest?a1:b1),top,z+(eastWest?b1:a1),tint,'#',age));
    }
    /** Face-relative coordinates: u follows wall, d protrudes into this cell. */
    private static void add(List<Piece> out,int x,int z,int face,Feature feature,double u0,double bottom,double d0,double u1,double top,double d1,int tint,char art,int age) {
        if(top<=bottom)return;
        double x0,x1,z0,z1;
        if(face%2==0){x0=x+u0;x1=x+u1;z0=z+(face==0?d0:1-d1);z1=z+(face==0?d1:1-d0);}
        else {z0=z+u0;z1=z+u1;x0=x+(face==3?d0:1-d1);x1=x+(face==3?d1:1-d0);}
        out.add(new Piece(feature,x0,bottom,z0,x1,top,z1,tint,art,age));
    }
    private static boolean lamp(World w,int x,int z,int face) {
        int along=face%2==0?x:z,line=face%2==0?z+(face==0?-1:1):x+(face==3?-1:1);
        return Math.floorMod(along,11)==mod(hash(w,line,face,97),11);
    }
    static void synchronizeLights(World w) {
        w.lightSources.removeIf(l->l.id!=null && l.id.startsWith("CLUTTER-LIGHT-"));
        if(w.architecturalClutterVersion==0)return;
        // Bounded fixture budget; all lamp meshes remain emissive even beyond the diffuse-light budget.
        int count=0;
        for(int z=0;z<w.h && count<96;z++)for(int x=0;x<w.w && count<96;x++)if(floor(w,x,z)) {
            for(int face=0;face<4 && count<96;face++) {
                int wx=x+(face==1?1:face==3?-1:0),wz=z+(face==2?1:face==0?-1:0);
                if(!host(w,wx,wz) || !lamp(w,x,z,face))continue;
                double ground=DeveloperBattleMapAuthority.height(w,x,z);
                if(TileStructureAuthority.at(w,wx,wz).wallTopElevation()<=ground+1.03 || Math.abs(DeveloperBattleMapAuthority.height(w,wx,wz)-ground)>.26)continue;
                String id="CLUTTER-LIGHT-"+x+"-"+z+"-"+face;
                w.lightSources.add(new ZoneLightSourceRecord(id,"Retrofitted wall lumen",x,z,w.roomIds[x][z],3,48,"blue",true,true,false,99,0,false,"architecture"));count++;
            }
        }
        w.dirtyLightRevision++;
    }
    static int color(Piece p,int nativeColor,double u,double v) {
        if(p.emissive())return 0xFF000000|p.tint();
        double amount=p.feature()==Feature.SEALED_DOOR?.2:p.feature()==Feature.CABLE || p.feature()==Feature.SWAG?.8:.52;
        int color=StructureAppearanceAuthority.tint(nativeColor,p.tint(),amount);
        if(p.feature()==Feature.SEALED_DOOR && (u<.06 || u>.94 || v<.05 || v>.95))color=StructureAppearanceAuthority.tint(color,0x96816B,.55);
        return color;
    }
    static double roundShade(Piece p,double x,double h,double z,double dx,double dh,double dz) {
        if(!p.round())return 1;
        int axis=p.longAxis();double nx=axis==0?0:x-(p.x0()+p.x1())*.5;
        double nh=axis==2?0:h-(p.h0()+p.h1())*.5,nz=axis==1?0:z-(p.z0()+p.z1())*.5;
        double denominator=Math.sqrt((nx*nx+nh*nh+nz*nz)*(dx*dx+dh*dh+dz*dz));
        return denominator<1e-10?1:.52+.48*Math.sqrt(Math.min(1,Math.abs(nx*dx+nh*dh+nz*dz)/denominator));
    }
    static void planner(Graphics2D g,World w,int x,int z,int sx,int sy,int size) {
        if(w.architecturalClutterVersion==0 || size<10)return;
        boolean lamp=false,overhead=false;
        for(Piece p:pieces(w,x,z)){lamp|=p.emissive();overhead|=p.feature()==Feature.DUCT || p.feature()==Feature.SWAG;}
        if(overhead){g.setColor(new Color(115,142,152,120));g.drawLine(sx+2,sy+2,sx+size-3,sy+2);}
        if(lamp){g.setColor(new Color(190,236,240));g.fillRect(sx+size-5,sy+3,2,2);}
    }
}
