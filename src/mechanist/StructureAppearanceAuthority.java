package mechanist;

import java.awt.*;

/** Material treatments over the game's native textures, shared with the planner. */
final class StructureAppearanceAuthority {
    private StructureAppearanceAuthority() { }
    static int tint(int base,int tint,double amount) {
        int r=(int)(((base>>16)&255)*(1-amount)+((tint>>16)&255)*amount);
        int g=(int)(((base>>8)&255)*(1-amount)+((tint>>8)&255)*amount);
        int b=(int)((base&255)*(1-amount)+(tint&255)*amount);
        return 0xFF000000|r<<16|g<<8|b;
    }
    static int color(int nativeColor,TileStructureAuthority.Structure m,StructureSurfaceAuthority.Kind kind,double u,double v) {
        if(kind==StructureSurfaceAuthority.Kind.RAIL)return tint(nativeColor,0x8A9EAA,.7);
        if(kind==StructureSurfaceAuthority.Kind.ROOF && m.roof()==TileStructureAuthority.Roof.GLASS
                || kind==StructureSurfaceAuthority.Kind.WALL && m.wall()==TileStructureAuthority.Wall.GLASS)return 0xFF80BDCD;
        if(kind!=StructureSurfaceAuthority.Kind.WALL)return nativeColor;
        return switch(m.finish()) {
            case PLAIN -> nativeColor;
            case STAINED -> tint(nativeColor,0x6D4532,.2+.24*(.5+.5*Math.sin(u*13+Math.sin(v*5))));
            case PANELS -> fraction(u)<.045 || fraction(v*2)<.045?tint(nativeColor,0x101A25,.65):tint(nativeColor,0x647581,.15);
            case LIT -> lamp(u,v)?0xFFB9EBF5:tint(nativeColor,0x303E48,.12);
        };
    }
    static boolean lamp(double u,double v){return fraction(u)>.18 && fraction(u)<.82 && fraction(v)>.72 && fraction(v)<.8;}
    private static double fraction(double n){return n-Math.floor(n);}
    static void planner(Graphics2D g,World w,int wx,int wy,int x,int y,int size) {
        ArchitecturalClutterAuthority.planner(g,w,wx,wy,x,y,size);
        var m=TileStructureAuthority.at(w,wx,wy);char glyph=w.tiles[wx][wy];
        if(m.floor()==TileStructureAuthority.Floor.VOID) {g.setColor(new Color(0,26,51));g.fillRect(x,y,size,size);}
        if(m.floor()==TileStructureAuthority.Floor.GRATING || m.roof()==TileStructureAuthority.Roof.GRATING) {
            g.setColor(new Color(140,165,174,150));
            for(int n=2;n<size;n+=Math.max(3,size/5)){g.drawLine(x+n,y,x+n,y+size);g.drawLine(x,y+n,x+size,y+n);}
        }
        if(m.roof()==TileStructureAuthority.Roof.GLASS) {
            g.setColor(new Color(110,200,225,55));g.fillRect(x,y,size,size);
            g.setColor(new Color(140,215,238,150));g.drawRect(x+1,y+1,size-3,size-3);g.drawLine(x+2,y+size-3,x+size-3,y+2);
        }
        if(glyph=='#' || glyph=='W') {
            if(m.wall()==TileStructureAuthority.Wall.PILLAR || m.wall()==TileStructureAuthority.Wall.PIPE) {
                g.setColor(new Color(25,37,48));g.fillRect(x,y,size,size);g.setColor(new Color(140,150,157));
                int radius=m.wall()==TileStructureAuthority.Wall.PIPE?Math.max(2,size/5):Math.max(3,size/2);
                g.fillOval(x+(size-radius)/2,y+(size-radius)/2,radius,radius);
            }
            if(m.wall()==TileStructureAuthority.Wall.GLASS){g.setColor(new Color(100,200,220,170));g.drawRect(x+2,y+2,size-4,size-4);}
            if(m.wall()==TileStructureAuthority.Wall.GRATING){g.setColor(new Color(130,160,175));for(int n=2;n<size;n+=4)g.drawLine(x+n,y,x+n,y+size);}
            if(m.finish()==TileStructureAuthority.Finish.STAINED){g.setColor(new Color(115,65,30,90));g.fillRect(x,y,size,size);}
            if(m.finish()==TileStructureAuthority.Finish.PANELS){g.setColor(new Color(15,25,35));g.drawRect(x+2,y+2,size-4,size-4);}
            if(m.finish()==TileStructureAuthority.Finish.LIT){g.setColor(new Color(185,235,245));g.fillRect(x+size/4,y+2,Math.max(1,size/2),2);}
        }
        g.setColor(new Color(200,222,230));
        if((m.railings()&1)!=0)g.drawLine(x+1,y+1,x+size-2,y+1);
        if((m.railings()&2)!=0)g.drawLine(x+size-2,y+1,x+size-2,y+size-2);
        if((m.railings()&4)!=0)g.drawLine(x+1,y+size-2,x+size-2,y+size-2);
        if((m.railings()&8)!=0)g.drawLine(x+1,y+1,x+1,y+size-2);
    }
}
