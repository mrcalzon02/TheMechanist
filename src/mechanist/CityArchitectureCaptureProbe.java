package mechanist;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Actual seeded city generation and GamePanel viewport captures, with no alternate renderer. */
final class CityArchitectureCaptureProbe {
    public static void main(String[] args)throws Exception {
        Path output=Path.of(args.length==0?"diagnostics/city-architecture-20261029":args[0]);Files.createDirectories(output);
        long seed=466713;WorldSetupSettings setup=WorldSetupSettings.standard();setup.zoneDensity=0;setup.npcDensity=0;setup.simulationAge=0;
        WorldGenerationApi.setActiveSettings(setup);
        GamePanel game=new GamePanel();game.timer.stop();
        try {
            World city=new World(seed,500,500);city.configureGenerationSettings(setup);city.floor=4;city.zoneX=2;city.zoneY=2;
            city.zoneType=ZoneType.NEUTRAL_CIVILIAN_FLOOR;city.zoneName="Seed 466713 architecture inspection";
            city.buildingSites=MultiFloorBuildingPlacementAuthority.planSites(seed,1,1,2,2,city.w,city.h);
            long start=System.nanoTime();
            // Run the game's real structural generation stages; population/economy is irrelevant to roof inspection.
            WorldGenerationPipelineRunState state=new WorldGenerationPipelineRunState();
            TileGenerationManager.resetSubstrate(city,state);city.worldgenPhasePlazaAndRoads(state);
            RoomGenerationManager.placeRoadFirstRoomsAndClaims(city,state);
            ZoneFixturePlacementManager.placeRoadAndRoomFixtures(city,state);
            city.worldgenPhaseBoundaryInterwallAndMetadata(state);TileStructureAuthority.authorRoomRoofs(city);TileStructureAuthority.authorOpenRoads(city);ArchitecturalClutterAuthority.enable(city,true);
            System.out.println("Generated actual city in "+((System.nanoTime()-start)/1e9)+"s; rooms="+city.rooms.size());
            game.world=city;game.seed=seed;game.active=Candidate.random(new Random(seed));game.active.stats.put("Vision",42);
            game.screen=GamePanel.Screen.GAME;game.panelMode=GamePanel.PanelMode.NONE;game.setSize(1280,800);
            game.options.soundEnabled=false;game.options.musicEnabled=false;game.options.doomModeEnabled=true;
            game.food=100;game.water=100;
            StringBuilder manifest=new StringBuilder("image\tseed\tmainFloor\tx\tz\teyeHeight\tyawDegrees\tpitchDegrees\n");
            Rectangle plaza=city.rooms.get(0);double px=plaza.getCenterX(),pz=plaza.getCenterY();
            capture(game,output,manifest,"01-plaza-street",px,pz,.58,0,0);
            capture(game,output,manifest,"02-plaza-look-up",px,pz,.58,0,65);
            int index=3;
            for(var site:city.buildingSites) {
                if(!site.blueprint().containsFloor(city.floor))continue;
                Rectangle footprint=site.footprint();double bx=footprint.getCenterX(),bz=footprint.getCenterY();
                capture(game,output,manifest,String.format("%02d-building-roof-oblique",index++),footprint.x+2,footprint.y+2,8,45,-35);
                capture(game,output,manifest,String.format("%02d-building-roof-down",index++),bx,bz,5,0,-85);
                capture(game,output,manifest,String.format("%02d-building-facade",index++),footprint.x+2,footprint.y+14,.58,0,12);
            }
            game.firstPersonRenderViewport.clearInspectionPose();game.options.doomModeEnabled=false;
            game.playerX=(int)px;game.playerY=(int)pz;snapshot(game,output.resolve("09-planner-generated-city.png"));
            long open=0,covered=0,roadOpen=0,roadCovered=0;
            for(int x=0;x<city.w;x++)for(int y=0;y<city.h;y++) {
                boolean roof=TileStructureAuthority.at(city,x,y).roof()==TileStructureAuthority.Roof.COVERED;
                if(roof)covered++;else open++;
                char tile=city.tiles[x][y];
                if(tile==RoadGridIntegrationAuthority.ROAD_LANE || tile==RoadGridIntegrationAuthority.SIDEWALK || tile==RoadGridIntegrationAuthority.PARKING_SPACE){if(roof)roadCovered++;else roadOpen++;}
            }
            Files.writeString(output.resolve("vantage-points.tsv"),manifest);
            Files.writeString(output.resolve("generation-audit.txt"),"seed="+seed+" mainFloor=4 dimensions=500x500\nrooms="+city.rooms.size()+" buildingSites="+city.buildingSites.size()+" stampedBuildingRooms="+city.buildingRoomIds.size()+"\nopenTiles="+open+" coveredTiles="+covered+" openRoadTiles="+roadOpen+" coveredRoadTiles="+roadCovered+"\n"
                    +"Captured with actual GamePanel/FirstPersonRenderViewport and shipped graphics. Normal structural-generation stages; population/economy omitted. Elevated inspection cameras do not certify traversable upper storeys.\n"
                    +"Current limitation: existing building blueprints link main atlas floors; local room-over-room storeys still require a distinct geometry/occupancy model.\n");
            System.out.println("City captures saved: "+output+" / open road tiles "+roadOpen+" covered roads "+roadCovered);
        } finally {game.shutdownRuntime();}
    }
    private static void capture(GamePanel game,Path output,StringBuilder manifest,String name,double x,double z,double height,double yaw,double pitch)throws Exception {
        game.playerX=(int)x;game.playerY=(int)z;
        game.firstPersonRenderViewport.setInspectionPose(x,height,z,Math.toRadians(yaw),Math.toRadians(pitch));
        snapshot(game,output.resolve(name+".png"));
        manifest.append(name).append(".png\t").append(game.seed).append("\t4\t").append(x).append('\t').append(z).append('\t').append(height).append('\t').append(yaw).append('\t').append(pitch).append('\n');
    }
    private static void snapshot(GamePanel game,Path path)throws Exception {
        BufferedImage image=new BufferedImage(1280,800,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        try{game.paint(g);}finally{g.dispose();}ImageIO.write(image,"png",path.toFile());
    }
}
