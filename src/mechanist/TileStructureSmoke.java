package mechanist;

import java.util.Arrays;
import java.util.Properties;

/** Vertical structure semantics, native ray geometry, persistence and snapshot compatibility. */
final class TileStructureSmoke {
    public static void main(String[] args) {
        World world=new World(91731,8,8);for(char[] column:world.tiles)Arrays.fill(column,'.');
        world.tiles[2][2]=RoadGridIntegrationAuthority.ROAD_LANE;
        TileStructureAuthority.roof(world,2,2,TileStructureAuthority.Roof.COVERED,2.5);
        require(WorldAmbientLightAuthority.at(world,2,2,4)==4,"Covered road must not receive open-space ambient");
        var roof=DeveloperBattleMapAuthority.cast(world,2.5,.58,2.5,0,1,0,12);
        require(roof!=null && close(roof.height(),2.5),"Covered road needs its authored roof height");
        var roofTop=DeveloperBattleMapAuthority.cast(world,2.5,4,2.5,0,-1,0,12);
        require(roofTop!=null && close(roofTop.height(),2.6),"Roof must occlude downward viewing from above");
        TileStructureAuthority.roof(world,2,2,TileStructureAuthority.Roof.OPEN,2.5);
        require(WorldAmbientLightAuthority.at(world,2,2,4)==28,"Open road must receive ambient light");
        roof=DeveloperBattleMapAuthority.cast(world,2.5,.58,2.5,0,1,0,12);
        require(roof!=null && close(roof.height(),10),"Open road must see distant next-level underside");
        TileStructureAuthority.roof(world,4,4,TileStructureAuthority.Roof.OPEN,1);
        world.roomIds[4][4]=0;
        require(WorldAmbientLightAuthority.at(world,4,4,4)==28,"Room identity must not force a roof over an open room");
        RoomProfile park=new RoomProfile("District Park","Open civic park",0,Faction.NONE,new String[0],new char[0]);
        require(!park.hasRoof,"Parks must default to no roof");
        park.withRoof(true);require(park.hasRoof,"Explicitly covered park must override open default");
        world.tiles[3][3]='#';
        require(close(TileStructureAuthority.at(world,3,3).wallTopElevation(),10),"Generic wall must reach the next main level");
        var wall=DeveloperBattleMapAuthority.cast(world,1.5,6,3.5,1,0,0,3);
        require(wall!=null && wall.x()==3,"Generic wall must still block six storeys above ground");
        TileStructureAuthority.modifyWallTop(world,3,3,.3);
        wall=DeveloperBattleMapAuthority.cast(world,1.5,6,3.5,1,0,0,3);
        require(wall==null,"Authored later-floor override must shorten the generic column");
        world.tiles[3][3]='D';
        require(DeveloperBattleMapAuthority.cast(world,1.5,6,3.5,1,0,0,3)==null,"Door must not become a full-height generic wall column");
        require(DeveloperBattleMapAuthority.cast(world,1.5,.58,3.5,1,0,0,3)!=null,"Closed door must still block at actor height");
        var grid=StructuredZoneGridRuntime.rebuildGridForWorld(world);
        require(grid.requireTile(4,4).structure().roof()==TileStructureAuthority.Roof.OPEN,"Structured tile lost explicit roof state");
        TileStructureAuthority.roof(world,4,4,TileStructureAuthority.Roof.COVERED,3);
        require(grid.requireTile(4,4).structure().roof()==TileStructureAuthority.Roof.COVERED,"Live structure edits must update cached structured tiles");
        grid.requireTile(5,5).setStructure(new TileStructureAuthority.Structure(TileStructureAuthority.Roof.OPEN,2,4));
        require(StructuredZoneGridRuntime.syncLegacyTiles(world),"Structured grid sync failed");
        require(TileStructureAuthority.at(world,5,5).roof()==TileStructureAuthority.Roof.OPEN,"Structured editor roof state must reach runtime world");
        Properties saved=new Properties();Persistence.writeWorldState(world,saved);
        World restored=new World(91731,8,8);for(char[] column:restored.tiles)Arrays.fill(column,'.');
        Persistence.readWorldState(restored,saved);
        require(TileStructureAuthority.at(restored,4,4).equals(TileStructureAuthority.at(world,4,4)),"Roof did not round-trip through world persistence");
        require(close(TileStructureAuthority.at(restored,3,3).wallTopElevation(),.3),"Wall override did not round-trip");
        var snapshot=TileSnapshot.fromDescriptor(world,4,4,'.',true,true,null);
        require(snapshot.roofCoverage().equals("COVERED") && close(snapshot.roofClearance(),3),"Tile snapshot lost vertical metadata");
        TileStructureAuthority.read(restored,new Properties());
        require(close(TileStructureAuthority.at(restored,3,3).wallTopElevation(),.3),"Legacy property set must preserve existing generated structure");
        TileStructureAuthority.read(restored,with("world.tileStructures.v1",""));
        require(restored.tileStructures.isEmpty(),"Empty saved overrides must restore defaults");
        boolean rejected=false;try{new TileStructureAuthority.Structure(TileStructureAuthority.Roof.OPEN,Double.NaN,10);}catch(IllegalArgumentException expected){rejected=true;}
        require(rejected,"Non-finite structure height must be rejected");
        System.out.println("TileStructureSmoke PASS coveredRoad=true openRoom=true fullHeightWalls=true floorOverride=true saveLoad=true snapshot=true");
    }
    private static Properties with(String key,String value){Properties p=new Properties();p.setProperty(key,value);return p;}
    private static boolean close(double a,double b){return Math.abs(a-b)<1e-8;}
    private static void require(boolean okay,String message){if(!okay)throw new AssertionError(message);}
}
