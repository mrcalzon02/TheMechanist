package mechanist;

import java.awt.Rectangle;
import java.util.Properties;

/** Per-tile structure is independent of surface art and road/room classification. */
final class TileStructureAuthority {
    enum Roof { OPEN, COVERED, GLASS, GRATING }
    enum Wall { SOLID, GLASS, GRATING, PILLAR, PIPE }
    enum Finish { PLAIN, STAINED, PANELS, LIT }
    enum Floor { SOLID, GRATING, VOID }
    static final int NORTH=1,EAST=2,SOUTH=4,WEST=8;
    record Structure(Roof roof,double roofClearance,double wallTopElevation,boolean explicitRoof,
                     Wall wall,Finish finish,Floor floor,int railings) {
        Structure(Roof roof,double roofClearance,double wallTopElevation,boolean explicitRoof) {
            this(roof,roofClearance,wallTopElevation,explicitRoof,Wall.SOLID,Finish.PLAIN,Floor.SOLID,0);
        }
        Structure(Roof roof,double roofClearance,double wallTopElevation){this(roof,roofClearance,wallTopElevation,true);}
        Structure {
            if(roof==null || wall==null || finish==null || floor==null || railings<0 || railings>15 || !Double.isFinite(roofClearance) || roofClearance<=0
                    || !Double.isFinite(wallTopElevation) || wallTopElevation<0)
                throw new IllegalArgumentException("Invalid vertical structure");
        }
        boolean hasRoof(){return roof!=Roof.OPEN;}
        Structure withRoof(Roof value,double clearance,boolean explicit){return new Structure(value,clearance,wallTopElevation,explicit,wall,finish,floor,railings);}
        Structure withWall(Wall value,Finish treatment){return new Structure(roof,roofClearance,wallTopElevation,explicitRoof,value,treatment,floor,railings);}
        Structure withFloor(Floor value,int edges){return new Structure(roof,roofClearance,wallTopElevation,explicitRoof,wall,finish,value,edges);}
    }
    static final double NEXT_LEVEL_UNDERSIDE=10;
    static final Structure DEFAULT=new Structure(Roof.COVERED,1,NEXT_LEVEL_UNDERSIDE,false);
    private TileStructureAuthority() { }
    private static long key(int x,int y){return ((long)x<<32)^(y&0xffffffffL);}
    static Structure at(World world,int x,int y) {
        return world==null || !world.inBounds(x,y)?DEFAULT:world.tileStructures.getOrDefault(key(x,y),DEFAULT);
    }
    static void set(World world,int x,int y,Structure value) {
        if(world==null || !world.inBounds(x,y))throw new IllegalArgumentException("Structure outside world");
        Structure before=at(world,x,y);
        world.tileStructures.put(key(x,y),java.util.Objects.requireNonNull(value));world.dirtyVisionRevision++;world.dirtyLightRevision++;
        if(before.finish()==Finish.LIT || value.finish()==Finish.LIT)syncLight(world,x,y,value);
        if(StructuredZoneGridRuntime.worldHasStructuredGrid(world))StructuredZoneGridRuntime.gridForWorld(world).requireTile(x,y).setStructure(value);
    }
    private static void syncLight(World world,int x,int y,Structure value) {
        String id="STRUCTURE-LIGHT-"+x+"-"+y;
        world.lightSources.removeIf(light->id.equals(light.id));
        if(value.finish()==Finish.LIT)world.lightSources.add(new ZoneLightSourceRecord(id,"Wall light panel",x,y,world.roomIds[x][y],4,65,"blue",true,true,false,99,0,false,"structure"));
    }
    static void synchronizeLights(World world) {
        world.lightSources.removeIf(light->light.id!=null && light.id.startsWith("STRUCTURE-LIGHT-"));
        for(var entry:world.tileStructures.entrySet())if(entry.getValue().finish()==Finish.LIT)
            syncLight(world,(int)(entry.getKey()>>32),(int)(long)entry.getKey(),entry.getValue());
        world.dirtyLightRevision++;
    }
    static void roof(World world,int x,int y,Roof roof,double clearance) {
        Structure previous=at(world,x,y);set(world,x,y,previous.withRoof(roof,clearance,true));
    }
    /** An authored later floor may replace the otherwise full-height wall column here. */
    static void modifyWallTop(World world,int x,int y,double top) {
        Structure p=at(world,x,y);set(world,x,y,new Structure(p.roof(),p.roofClearance(),top,p.explicitRoof(),p.wall(),p.finish(),p.floor(),p.railings()));
    }
    static void open(World world,Rectangle bounds) {
        for(int x=Math.max(0,bounds.x);x<Math.min(world.w,bounds.x+bounds.width);x++)
            for(int y=Math.max(0,bounds.y);y<Math.min(world.h,bounds.y+bounds.height);y++)roof(world,x,y,Roof.OPEN,1);
    }
    static void authorOpenRoads(World world) {
        if(world.sewerLayer)return;
        for(int x=0;x<world.w;x++)for(int y=0;y<world.h;y++) {
            char tile=world.tiles[x][y];
            if((tile==RoadGridIntegrationAuthority.ROAD_LANE || tile==RoadGridIntegrationAuthority.SIDEWALK || tile==RoadGridIntegrationAuthority.PARKING_SPACE) && !at(world,x,y).explicitRoof()) {
                Structure before=at(world,x,y);set(world,x,y,before.withRoof(Roof.OPEN,1,false));
            }
        }
    }
    static void authorRoomRoofs(World world) {
        for(int i=0;i<Math.min(world.rooms.size(),world.roomProfiles.size());i++) {
            RoomProfile profile=world.roomProfiles.get(i);if(profile==null)continue;
            Rectangle bounds=world.rooms.get(i);
            boolean roofed=profile.hasRoof;
            for(int x=Math.max(0,bounds.x);x<Math.min(world.w,bounds.x+bounds.width);x++)
                for(int y=Math.max(0,bounds.y);y<Math.min(world.h,bounds.y+bounds.height);y++) {
                    char tile=world.tiles[x][y];
                    boolean road=tile==RoadGridIntegrationAuthority.ROAD_LANE || tile==RoadGridIntegrationAuthority.SIDEWALK || tile==RoadGridIntegrationAuthority.PARKING_SPACE;
                    if(road && !profile.roofExplicit)continue;
                    Structure previous=at(world,x,y);
                    set(world,x,y,previous.withRoof(roofed?Roof.COVERED:Roof.OPEN,RoofConstructionAuthority.CLEARANCE,profile.roofExplicit));
                }
        }
    }
    static void write(World world,Properties p) {
        StringBuilder data=new StringBuilder();
        for(var entry:world.tileStructures.entrySet()) {
            long position=entry.getKey();Structure value=entry.getValue();
            data.append((int)(position>>32)).append(',').append((int)position).append(',').append(value.roof()).append(',')
                    .append(value.roofClearance()).append(',').append(value.wallTopElevation()).append(',').append(value.explicitRoof()).append(',')
                    .append(value.wall()).append(',').append(value.finish()).append(',').append(value.floor()).append(',').append(value.railings()).append(';');
        }
        p.setProperty("world.tileStructures.v1",data.toString());
    }
    static void read(World world,Properties p) {
        String data=p.getProperty("world.tileStructures.v1");if(data==null)return; // Legacy saves retain generated structure.
        world.tileStructures.clear();
        world.lightSources.removeIf(light->light.id!=null && light.id.startsWith("STRUCTURE-LIGHT-"));
        for(String record:data.split(";"))if(!record.isBlank()) {
            String[] fields=record.split(",");
            try{if(fields.length!=5 && fields.length!=6 && fields.length!=10)throw new IllegalArgumentException("Incomplete structure record");
                set(world,Integer.parseInt(fields[0]),Integer.parseInt(fields[1]),new Structure(Roof.valueOf(fields[2]),Double.parseDouble(fields[3]),Double.parseDouble(fields[4]),fields.length<6 || Boolean.parseBoolean(fields[5]),
                    fields.length<10?Wall.SOLID:Wall.valueOf(fields[6]),fields.length<10?Finish.PLAIN:Finish.valueOf(fields[7]),
                    fields.length<10?Floor.SOLID:Floor.valueOf(fields[8]),fields.length<10?0:Integer.parseInt(fields[9])));}
            catch(IllegalArgumentException | IndexOutOfBoundsException invalid){DebugLog.warn("TILE_STRUCTURE_LOAD","Ignored invalid tile structure record.");}
        }
        world.dirtyVisionRevision++;
        if(StructuredZoneGridRuntime.worldHasStructuredGrid(world))StructuredZoneGridRuntime.rebuildGridForWorld(world);
    }
}
