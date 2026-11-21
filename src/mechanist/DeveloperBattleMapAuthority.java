package mechanist;

import java.awt.Rectangle;
import java.util.*;

/** Physical elevations for the existing facility arena. No separate world, viewport, or actors. */
final class DeveloperBattleMapAuthority {
    private static final Map<World,double[][]> HEIGHTS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<World,Map<Long,TileStructureAuthority.Structure>> ORIGINAL_STRUCTURE=new WeakHashMap<>();
    private static final Map<World,Map<Long,Character>> ORIGINAL_TILES=new WeakHashMap<>();
    private DeveloperBattleMapAuthority() { }
    static boolean active(World world) { return HEIGHTS.containsKey(world); }
    static double height(World world,int x,int y) {
        double[][] heights=HEIGHTS.get(world);
        return heights!=null && world.inBounds(x,y)?heights[x][y]:0;
    }
    static boolean step(World world,int x,int y,int nx,int ny) {
        if(!StructureSurfaceAuthority.step(world,x,y,nx,ny))return false;
        if(!active(world))return true;
        if(Math.abs(height(world,x,y)-height(world,nx,ny))>.251)return false;
        return x==nx || y==ny || Math.abs(height(world,x,ny)-height(world,x,y))<=.251
                && Math.abs(height(world,nx,y)-height(world,x,y))<=.251;
    }
    static void build(GamePanel game,boolean enabled) {
        DeveloperFacilityAuthority.clearArena(game);
        ArchitecturalClutterAuthority.enable(game.world,false);
        var originalTiles=ORIGINAL_TILES.remove(game.world);
        if(originalTiles!=null)for(var tile:originalTiles.entrySet())game.world.tiles[(int)(tile.getKey()>>32)][(int)(long)tile.getKey()]=tile.getValue();
        var oldStructure=ORIGINAL_STRUCTURE.get(game.world);
        if(oldStructure!=null){game.world.tileStructures.clear();game.world.tileStructures.putAll(oldStructure);}
        StairTileArtAuthority.clear(game.world);
        PlannerLevelViewAuthority.reset(game);
        if(!enabled) {
            HEIGHTS.remove(game.world);
            var original=ORIGINAL_STRUCTURE.remove(game.world);
            if(original!=null){game.world.tileStructures.clear();game.world.tileStructures.putAll(original);}
        }
        else {
            Rectangle room=DeveloperFacilityAuthority.layout(game).rooms.get("combat");
            ORIGINAL_STRUCTURE.putIfAbsent(game.world,new LinkedHashMap<>(game.world.tileStructures));
            TileStructureAuthority.open(game.world,room);
            int cx=room.x+25,cy=room.y+13;
            double[][] heights=new double[game.world.w][game.world.h];
            for(int x=room.x+2;x<room.x+room.width-2;x++)for(int y=room.y+9;y<room.y+room.height-2;y++) {
                heights[x][y]=y>=cy+10?2:x<=cx-5 || x>=cx+4?1:0;
            }
            // Eight quarter-storey steps to the far gallery, independent of the terraces.
            for(int x=cx-3;x<=cx+2;x++)for(int i=0;i<8;i++) {
                heights[x][cy+2+i]=(i+1)*.25;
                StairTileArtAuthority.mark(game.world,x,cy+2+i,StairTileArtAuthority.Direction.SOUTH);
            }
            // Four-step lateral stairs connect the ground fighting lane to both terraces.
            for(int y=cy-2;y<=cy+1;y++)for(int i=0;i<4;i++) {
                heights[cx+i+1][y]=(i+1)*.25;
                heights[cx-i-2][y]=(i+1)*.25;
                StairTileArtAuthority.mark(game.world,cx+i+1,y,StairTileArtAuthority.Direction.EAST);
                StairTileArtAuthority.mark(game.world,cx-i-2,y,StairTileArtAuthority.Direction.WEST);
            }
            HEIGHTS.put(game.world,heights);
            // A covered firing terrace beside the open fighting lane exercises independent roof coverage.
            for(int x=cx-10;x<=cx-5;x++)for(int y=cy-1;y<=cy+5;y++)
                TileStructureAuthority.roof(game.world,x,y,TileStructureAuthority.Roof.COVERED,1.5);
        }
        game.world.dirtyVisionRevision++;game.firstPersonRenderViewport=new FirstPersonRenderViewport();
        game.logEvent(enabled?"Battle map: ground lane, +1 firing terraces, +2 rear gallery. Quarter-storey stairs connect all heights; ledges block movement.":"Combat arena restored to flat terrain.");
        TileStructureAuthority.synchronizeLights(game.world);
        if(StructuredZoneGridRuntime.worldHasStructuredGrid(game.world))StructuredZoneGridRuntime.rebuildGridForWorld(game.world);
    }
    static void clutter(GamePanel game) {
        build(game,true);
        Rectangle r=DeveloperFacilityAuthority.layout(game).rooms.get("combat");
        for(int x=r.x;x<r.x+r.width;x++)for(int z=r.y;z<r.y+7;z++)
            TileStructureAuthority.roof(game.world,x,z,TileStructureAuthority.Roof.COVERED,1.6);
        ArchitecturalClutterAuthority.enable(game.world,true);
        game.logEvent("Layered architecture: old sealed thresholds, repairs, pipe risers, later cable runs, supported ducts and hanging services. North aisle has a low service canopy; the upper wall remains visible from the open arena.");
    }
    /** Authored material/catwalk examples inside the existing combat facility. */
    static void materials(GamePanel game) {
        build(game,true);World w=game.world;
        Rectangle r=DeveloperFacilityAuthority.layout(game).rooms.get("combat");int cx=r.x+25,cy=r.y+13;
        // Transparent canopies on the west terrace, using the same supported roof slab geometry.
        for(int x=cx-10;x<=cx-5;x++)for(int y=cy-1;y<=cy+5;y++)
            TileStructureAuthority.roof(w,x,y,x<cx-7?TileStructureAuthority.Roof.GLASS:TileStructureAuthority.Roof.GRATING,1.5);
        Map<Long,Character> tiles=new LinkedHashMap<>();ORIGINAL_TILES.put(w,tiles);
        TileStructureAuthority.Wall[] forms={TileStructureAuthority.Wall.SOLID,TileStructureAuthority.Wall.SOLID,TileStructureAuthority.Wall.SOLID,TileStructureAuthority.Wall.SOLID,TileStructureAuthority.Wall.GLASS,TileStructureAuthority.Wall.GRATING,TileStructureAuthority.Wall.PILLAR,TileStructureAuthority.Wall.PIPE};
        for(int i=0;i<forms.length;i++) {
            int x=cx-13+i*2,y=cy-4;long key=((long)x<<32)^(y&0xffffffffL);
            tiles.put(key,w.tiles[x][y]);w.tiles[x][y]='#';
            var m=TileStructureAuthority.at(w,x,y).withWall(forms[i],TileStructureAuthority.Finish.values()[i%4]);
            TileStructureAuthority.set(w,x,y,m);TileStructureAuthority.modifyWallTop(w,x,y,2.2);
        }
        // Guarded grating bridge over authored unsupported tiles. Ends stay on the terrace.
        for(int x=cx+10;x<=cx+18;x++)for(int y=cy;y<=cy+4;y++) {
            var m=TileStructureAuthority.at(w,x,y);
            TileStructureAuthority.set(w,x,y,m.withFloor(y==cy+2?TileStructureAuthority.Floor.GRATING:TileStructureAuthority.Floor.VOID,y==cy+2?5:0));
        }
        if(StructuredZoneGridRuntime.worldHasStructuredGrid(w))StructuredZoneGridRuntime.rebuildGridForWorld(w);
        game.logEvent("Material bay: glass/grating canopies west, wall forms north, guarded grating catwalk east. Unsupported gaps reject movement.");
    }
    static void distribute(GamePanel game) {
        if(!active(game.world))return;
        Rectangle r=DeveloperFacilityAuthority.layout(game).rooms.get("combat");int cx=r.x+25,cy=r.y+13;
        for(NpcEntity actor:game.world.npcs)if(actor.id.startsWith("MATCH-")) {
            int i=Integer.parseInt(actor.id.substring(actor.id.lastIndexOf('-')+1));
            boolean a="A".equals(actor.combatTeam);int tier=i%3;
            actor.moveTo(tier==0?cx+(a?-4:3):cx+(a?-7:6),tier==2?cy+11+i/3:tier==0?cy+2+i/3:cy+i/3);
        }
    }
    static int distance(World world,int x,int y,int tx,int ty) {
        return (int)Math.ceil(Math.sqrt((x-tx)*(double)(x-tx)+(y-ty)*(double)(y-ty)
                +Math.pow(height(world,x,y)-height(world,tx,ty),2)));
    }
    static boolean clearLine(World world,int x,int y,int tx,int ty) {
        double start=height(world,x,y)+.58,end=height(world,tx,ty)+.58;
        var hit=cast(world,x+.5,start,y+.5,tx-x,end-start,ty-y,1.00001);
        return hit==null || hit.distance()>=.9999;
    }
    record Surface(double distance,int x,int y,double height,int axis,StructureSurfaceAuthority.Kind kind,ArchitecturalClutterAuthority.Piece clutter) {
        Surface(double distance,int x,int y,double height,int axis,StructureSurfaceAuthority.Kind kind){this(distance,x,y,height,axis,kind,null);}
    }
    static Surface cast(World world,double x,double height,double y,double dx,double dh,double dy,double limit) {
        return StructureSurfaceAuthority.cast(world,x,height,y,dx,dh,dy,limit,.0001,false);
    }
}
