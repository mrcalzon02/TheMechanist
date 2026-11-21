package mechanist;

/** Shared physical ray geometry. Optical openings never change movement support. */
final class StructureSurfaceAuthority {
    enum Kind { FLOOR, WALL, ROOF, RAIL, CLUTTER }
    private StructureSurfaceAuthority() { }

    static boolean supported(World w,int x,int y) {
        return w!=null && w.inBounds(x,y) && TileStructureAuthority.at(w,x,y).floor()!=TileStructureAuthority.Floor.VOID
                && w.tiles[x][y]!=InterstitialInfrastructureApi.VOID_SPACE;
    }
    static boolean step(World w,int x,int y,int nx,int ny) {
        if(!supported(w,nx,ny))return false;
        if(x==nx && y==ny)return true;
        if(Math.abs(nx-x)>1 || Math.abs(ny-y)>1)return false;
        if(x!=nx && y!=ny)return step(w,x,y,nx,y) && step(w,x,y,x,ny)
                && step(w,nx,y,nx,ny) && step(w,x,ny,nx,ny);
        return edgeAllowed(TileStructureAuthority.at(w,x,y),TileStructureAuthority.at(w,nx,ny),nx-x,ny-y);
    }
    static boolean edgeAllowed(TileStructureAuthority.Structure a,TileStructureAuthority.Structure b,int dx,int dy) {
        if(b.floor()==TileStructureAuthority.Floor.VOID)return false;
        if(dx>0 && ((a.railings()&2)!=0 || (b.railings()&8)!=0))return false;
        if(dx<0 && ((a.railings()&8)!=0 || (b.railings()&2)!=0))return false;
        if(dy>0 && ((a.railings()&4)!=0 || (b.railings()&1)!=0))return false;
        return dy>=0 || (a.railings()&1)==0 && (b.railings()&4)==0;
    }
    static boolean grate(double u,double v) {
        return fraction(u*8)<.18 || fraction(v*8)<.18;
    }
    private static double fraction(double n){return n-Math.floor(n);}
    static boolean glass(World w,DeveloperBattleMapAuthority.Surface s) {
        var material=TileStructureAuthority.at(w,s.x(),s.y());
        double localCeiling=DeveloperBattleMapAuthority.height(w,s.x(),s.y())+material.roofClearance();
        return s.kind()==Kind.ROOF?material.roof()==TileStructureAuthority.Roof.GLASS && s.height()>=localCeiling-1e-8 && s.height()<=localCeiling+.10000001:
                s.kind()==Kind.WALL && material.wall()==TileStructureAuthority.Wall.GLASS;
    }

    static DeveloperBattleMapAuthority.Surface cast(World w,double x,double h,double y,double dx,double dh,double dy,double limit,double minimum,boolean optical) {
        int tx=(int)Math.floor(x),ty=(int)Math.floor(y),sx=dx<0?-1:1,sy=dy<0?-1:1;
        double ddx=Math.abs(dx)<1e-12?Double.POSITIVE_INFINITY:Math.abs(1/dx);
        double ddy=Math.abs(dy)<1e-12?Double.POSITIVE_INFINITY:Math.abs(1/dy);
        double nextX=Double.isInfinite(ddx)?ddx:(dx<0?x-tx:tx+1-x)*ddx;
        double nextY=Double.isInfinite(ddy)?ddy:(dy<0?y-ty:ty+1-y)*ddy;
        double enter=0;
        for(int count=0;count<384 && enter<limit && w.inBounds(tx,ty);count++) {
            double exit=Math.min(limit,Math.min(nextX,nextY));
            if(exit>=minimum) {
                var m=TileStructureAuthority.at(w,tx,ty);double ground=DeveloperBattleMapAuthority.height(w,tx,ty);
                char glyph=w.tiles[tx][ty];boolean wall=glyph=='#' || glyph=='W',door="|LVXD".indexOf(glyph)>=0;
                DeveloperBattleMapAuthority.Surface best=null;
                if(m.floor()!=TileStructureAuthority.Floor.VOID && glyph!=InterstitialInfrastructureApi.VOID_SPACE) {
                    var floor=box(x,h,y,dx,dh,dy,tx,ground-(m.floor()==TileStructureAuthority.Floor.GRATING?.1:10000),ty,tx+1,ground,ty+1,minimum,exit,tx,ty,Kind.FLOOR,false);
                    if(floor!=null && (m.floor()!=TileStructureAuthority.Floor.GRATING || grate(x+dx*floor.distance(),y+dy*floor.distance())))best=floor;
                }
                if(wall || door) {
                    double radius=wall && m.wall()==TileStructureAuthority.Wall.PILLAR?.23:wall && m.wall()==TileStructureAuthority.Wall.PIPE?.11:.5;
                    var face=box(x,h,y,dx,dh,dy,tx+.5-radius,ground,ty+.5-radius,tx+.5+radius,wall?m.wallTopElevation():ground+m.roofClearance(),ty+.5+radius,minimum,exit,tx,ty,Kind.WALL,optical && m.wall()==TileStructureAuthority.Wall.GLASS);
                    if(wall && m.wall()==TileStructureAuthority.Wall.PIPE)
                        face=pipe(x,h,y,dx,dh,dy,tx,ty,ground,m.wallTopElevation(),radius,minimum,exit);
                    if(face!=null && (!wall || m.wall()!=TileStructureAuthority.Wall.GRATING || grate(face.axis()==0?y+dy*face.distance():x+dx*face.distance(),face.height())))best=nearer(best,face);
                }
                double ceiling=m.hasRoof()?ground+m.roofClearance():TileStructureAuthority.NEXT_LEVEL_UNDERSIDE;
                var roof=box(x,h,y,dx,dh,dy,tx,ceiling,ty,tx+1,ceiling+.1,ty+1,minimum,exit,tx,ty,Kind.ROOF,optical && m.roof()==TileStructureAuthority.Roof.GLASS);
                if(optical && m.roof()==TileStructureAuthority.Roof.GLASS) {
                    // A continuous canopy has no internal glass faces along tile boundaries.
                    double plane=dh>0?ceiling:ceiling+.1,t=Math.abs(dh)<1e-12?Double.POSITIVE_INFINITY:(plane-h)/dh;
                    roof=t>=Math.max(minimum,enter) && t<=exit?new DeveloperBattleMapAuthority.Surface(t,tx,ty,plane,2,Kind.ROOF):null;
                }
                if(roof!=null && (m.roof()!=TileStructureAuthority.Roof.GRATING || grate(x+dx*roof.distance(),y+dy*roof.distance())))best=nearer(best,roof);
                if(m.hasRoof() && Math.abs(ceiling-TileStructureAuthority.NEXT_LEVEL_UNDERSIDE)>.11)
                    best=nearer(best,box(x,h,y,dx,dh,dy,tx,TileStructureAuthority.NEXT_LEVEL_UNDERSIDE,ty,tx+1,TileStructureAuthority.NEXT_LEVEL_UNDERSIDE+.1,ty+1,minimum,exit,tx,ty,Kind.ROOF,false));
                // Thin edge barriers with open spans, two horizontal bars and upright posts.
                for(int edge=1;edge<=8;edge*=2)if((m.railings()&edge)!=0) {
                    boolean horizontal=edge==1 || edge==4;
                    double edgeX=edge==2?tx+.97:tx+.03,edgeY=edge==4?ty+.97:ty+.03;
                    for(int part=0;part<5;part++) {
                        boolean bar=part<2;double along=part==2?.05:part==3?.5:.95;
                        double loX=horizontal?(bar?tx:tx+along-.025):edgeX-.025;
                        double hiX=horizontal?(bar?tx+1:tx+along+.025):edgeX+.025;
                        double loY=horizontal?edgeY-.025:(bar?ty:ty+along-.025);
                        double hiY=horizontal?edgeY+.025:(bar?ty+1:ty+along+.025);
                        double loH=ground+(bar?(part==0?.42:.82):0),hiH=bar?loH+.055:ground+.88;
                        best=nearer(best,box(x,h,y,dx,dh,dy,loX,loH,loY,hiX,hiH,hiY,minimum,exit,tx,ty,Kind.RAIL,false));
                    }
                }
                if(w.architecturalClutterVersion!=0)for(var piece:ArchitecturalClutterAuthority.pieces(w,tx,ty)) {
                    var clutter=box(x,h,y,dx,dh,dy,piece.x0(),piece.h0(),piece.z0(),piece.x1(),piece.h1(),piece.z1(),minimum,exit,tx,ty,Kind.CLUTTER,false);
                    if(clutter!=null && piece.round())clutter=roundClutter(piece,x,h,y,dx,dh,dy,minimum,exit,tx,ty);
                    if(clutter!=null && (best==null || clutter.distance()<best.distance()))best=new DeveloperBattleMapAuthority.Surface(clutter.distance(),tx,ty,clutter.height(),clutter.axis(),Kind.CLUTTER,piece);
                }
                if(best!=null && best.distance()>=enter-1e-8)return best;
            }
            enter=exit;
            if(nextX<nextY){tx+=sx;nextX+=ddx;}else{ty+=sy;nextY+=ddy;}
        }
        return null;
    }
    private static DeveloperBattleMapAuthority.Surface nearer(DeveloperBattleMapAuthority.Surface a,DeveloperBattleMapAuthority.Surface b) {
        return b==null?a:a==null || b.distance()<a.distance()?b:a;
    }
    private static DeveloperBattleMapAuthority.Surface roundClutter(ArchitecturalClutterAuthority.Piece p,double x,double h,double z,double dx,double dh,double dz,double minimum,double limit,int tx,int tz) {
        int axis=p.longAxis();
        double u=axis==0?h:x,du=axis==0?dh:dx,cu=axis==0?(p.h0()+p.h1())*.5:(p.x0()+p.x1())*.5;
        double v=axis==1?h:z,dv=axis==1?dh:dz,cv=axis==1?(p.h0()+p.h1())*.5:(p.z0()+p.z1())*.5;
        double along=axis==0?x:axis==1?z:h,da=axis==0?dx:axis==1?dz:dh;
        double lo=axis==0?p.x0():axis==1?p.z0():p.h0(),hi=axis==0?p.x1():axis==1?p.z1():p.h1();
        double radius=.5*Math.min(axis==0?p.h1()-p.h0():p.x1()-p.x0(),axis==1?p.h1()-p.h0():p.z1()-p.z0());
        double ox=u-cu,oy=v-cv,a=du*du+dv*dv,b=2*(ox*du+oy*dv),c=ox*ox+oy*oy-radius*radius;
        double near=Double.NEGATIVE_INFINITY,far=Double.POSITIVE_INFINITY;int normalAxis=axis==0?2:0;
        if(a<1e-12){if(c>0)return null;}
        else {double disc=b*b-4*a*c;if(disc<0)return null;near=(-b-Math.sqrt(disc))/(2*a);far=(-b+Math.sqrt(disc))/(2*a);}
        if(Math.abs(da)<1e-12){if(along<lo || along>hi)return null;}
        else {double t0=(lo-along)/da,t1=(hi-along)/da,n=Math.min(t0,t1);if(n>near){near=n;normalAxis=axis;}far=Math.min(far,Math.max(t0,t1));}
        double t=Math.max(minimum,near);
        return t<=far && t<=limit?new DeveloperBattleMapAuthority.Surface(t,tx,tz,h+dh*t,normalAxis,Kind.CLUTTER):null;
    }
    private static DeveloperBattleMapAuthority.Surface pipe(double x,double h,double y,double dx,double dh,double dy,int tx,int ty,double bottom,double top,double radius,double minimum,double limit) {
        double ox=x-tx-.5,oy=y-ty-.5,a=dx*dx+dy*dy,b=2*(ox*dx+oy*dy),c=ox*ox+oy*oy-radius*radius;
        double near=Double.NEGATIVE_INFINITY,far=Double.POSITIVE_INFINITY;int axis=0;
        if(a<1e-12){if(c>0)return null;}
        else {double disc=b*b-4*a*c;if(disc<0)return null;near=(-b-Math.sqrt(disc))/(2*a);far=(-b+Math.sqrt(disc))/(2*a);}
        if(Math.abs(dh)<1e-12){if(h<bottom || h>top)return null;}
        else {double t0=(bottom-h)/dh,t1=(top-h)/dh,n=Math.min(t0,t1);if(n>near){near=n;axis=2;}far=Math.min(far,Math.max(t0,t1));}
        double t=Math.max(minimum,near);
        if(axis!=2)axis=Math.abs(ox+dx*t)>Math.abs(oy+dy*t)?0:1;
        return t<=far && t<=limit?new DeveloperBattleMapAuthority.Surface(t,tx,ty,h+dh*t,axis,Kind.WALL):null;
    }
    private static DeveloperBattleMapAuthority.Surface box(double x,double h,double y,double dx,double dh,double dy,
            double lx,double lh,double ly,double hx,double hh,double hy,double minimum,double limit,int tx,int ty,Kind kind,boolean singleEntry) {
        if(hh<lh)return null;
        double near=Double.NEGATIVE_INFINITY,far=Double.POSITIVE_INFINITY;int axis=0;
        if(Math.abs(dx)<1e-12){if(x<lx || x>hx)return null;}
        else {double a=(lx-x)/dx,b=(hx-x)/dx;near=Math.min(a,b);far=Math.max(a,b);}
        if(Math.abs(dy)<1e-12){if(y<ly || y>hy)return null;}
        else {double a=(ly-y)/dy,b=(hy-y)/dy,n=Math.min(a,b);if(n>near){near=n;axis=1;}far=Math.min(far,Math.max(a,b));}
        if(Math.abs(dh)<1e-12){if(h<lh || h>hh)return null;}
        else {double a=(lh-h)/dh,b=(hh-h)/dh,n=Math.min(a,b);if(n>near){near=n;axis=2;}far=Math.min(far,Math.max(a,b));}
        if(singleEntry && near<minimum)return null;
        double t=Math.max(minimum,near);
        return t<=far+1e-9 && t<=limit+1e-9?new DeveloperBattleMapAuthority.Surface(t,tx,ty,h+dh*t,axis,kind):null;
    }
}
