package mechanist;

import java.awt.image.BufferedImage;
import java.util.*;

/** Authored ascent direction is shared by the planner and first-person floor sampler. */
final class StairTileArtAuthority {
    enum Direction { NORTH, EAST, SOUTH, WEST }
    private static final Map<World,Map<Long,Direction>> FLIGHTS=new WeakHashMap<>();
    private static final Map<BufferedImage,BufferedImage[]> ROTATIONS=new WeakHashMap<>();
    private StairTileArtAuthority() { }
    static void mark(World world,int x,int y,Direction ascent) {
        if(world.inBounds(x,y))FLIGHTS.computeIfAbsent(world,w->new HashMap<>()).put(key(x,y),ascent);
    }
    static void clear(World world) {FLIGHTS.remove(world);}
    static Direction direction(World world,int x,int y) {
        Map<Long,Direction> flights=FLIGHTS.get(world);return flights==null?null:flights.get(key(x,y));
    }
    private static long key(int x,int y){return ((long)x<<32)^(y&0xffffffffL);}
    static BufferedImage image(LegacyImageSurface images,World world,int x,int y) {
        Direction direction=direction(world,x,y);if(direction==null)return null;
        BufferedImage base=images.getTile("stair_flight",'.');
        if(base==null)return null;
        return rotated(base,direction);
    }
    static BufferedImage rotated(BufferedImage base,Direction direction) {
        BufferedImage[] variants=ROTATIONS.computeIfAbsent(base,b->new BufferedImage[4]);
        int turns=direction.ordinal();if(variants[turns]!=null)return variants[turns];
        int size=base.getWidth();
        if(base.getHeight()!=size)throw new IllegalArgumentException("Stair tile must be square");
        BufferedImage out=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<size;y++)for(int x=0;x<size;x++) {
            int xx=x,yy=y;
            for(int n=0;n<turns;n++){int old=xx;xx=size-1-yy;yy=old;}
            out.setRGB(xx,yy,base.getRGB(x,y));
        }
        return variants[turns]=out;
    }
}
