package mechanist;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Archive the real facility and existing GamePanel renderer; poses are inspection cameras. */
final class StructureMaterialsCaptureProbe {
    public static void main(String[] args)throws Exception {
        Path output=Path.of(args.length==0?"../diagnostics/structure-materials-gallery-20261102":args[0]);Files.createDirectories(output);
        GamePanel campaign=new GamePanel();campaign.timer.stop();campaign.world=new World(19191,32,24);
        for(char[] c:campaign.world.tiles)Arrays.fill(c,'.');campaign.active=Candidate.random(new Random(3));campaign.seed=19191;
        campaign.singlePlayerSectorBridge=new SinglePlayerSectorRuntimeBridge(new SectorManager());
        DeveloperFacilityAuthority.Session session=null;
        try {
            session=DeveloperFacilityAuthority.enter(campaign);GamePanel g=session.facility();g.timer.stop();
            g.options.musicEnabled=false;g.options.soundEnabled=false;g.options.tileIconRendering=true;
            g.screen=GamePanel.Screen.GAME;g.panelMode=GamePanel.PanelMode.NONE;g.active.stats.put("Vision",42);g.setSize(1280,800);
            DeveloperFacilityAuthority.execute(g,"battlemap",java.util.List.of("materials"));
            DeveloperCombatScenarioAuthority.prepare(g,10,"rifles",71000);
            Rectangle r=DeveloperFacilityAuthority.layout(g).rooms.get("combat");int cx=r.x+25,cy=r.y+13;
            StringBuilder poses=new StringBuilder("image\tx\tz\teyeHeight\tyawDegrees\tpitchDegrees\n");
            capture(g,output,poses,"01-wall-finishes",cx-10,cy-8,1.58,90,0);
            capture(g,output,poses,"02-glass-grating-pillar-pipe",cx-2,cy-7,1.58,90,0);
            capture(g,output,poses,"03-glass-canopy-up",cx-9,cy+2,1.58,90,60);
            capture(g,output,poses,"04-grating-canopy-up",cx-6,cy+2,1.58,90,60);
            capture(g,output,poses,"05-catwalk-along",cx+10,cy+2.5,1.58,0,-12);
            capture(g,output,poses,"06-catwalk-oblique",cx+14,cy-2,2.8,90,-25);
            g.options.doomModeEnabled=false;g.firstPersonRenderViewport.clearInspectionPose();g.playerX=cx+12;g.playerY=cy+2;
            snapshot(g,output.resolve("07-planner-materials.png"));
            Files.writeString(output.resolve("vantage-points.tsv"),poses);
            StringBuilder tiles=new StringBuilder("x\tz\tgroundHeight\tglyph\troof\troofClearance\twallTopElevation\twallForm\tfinish\tfloor\trailingsNESW\n");
            for(int x=r.x;x<r.x+r.width;x++)for(int y=r.y;y<r.y+r.height;y++) {
                var m=TileStructureAuthority.at(g.world,x,y);
                if(m.wall()==TileStructureAuthority.Wall.SOLID && m.finish()==TileStructureAuthority.Finish.PLAIN
                        && m.floor()==TileStructureAuthority.Floor.SOLID && m.railings()==0
                        && m.roof()!=TileStructureAuthority.Roof.GLASS && m.roof()!=TileStructureAuthority.Roof.GRATING)continue;
                tiles.append(x).append('\t').append(y).append('\t').append(DeveloperBattleMapAuthority.height(g.world,x,y)).append('\t').append(g.world.tiles[x][y])
                        .append('\t').append(m.roof()).append('\t').append(m.roofClearance()).append('\t').append(m.wallTopElevation())
                        .append('\t').append(m.wall()).append('\t').append(m.finish()).append('\t').append(m.floor()).append('\t').append(m.railings()).append('\n');
            }
            Files.writeString(output.resolve("material-tiles.tsv"),tiles);
            StringBuilder recipes=new StringBuilder("recipe\tsupplies\tmachineParts\tlaborTurns\tcomponents\n");
            for(BuildRecipe recipe:java.util.List.of(BuildRecipe.roofPanel(),BuildRecipe.glassRoofPanel(),BuildRecipe.gratingRoofPanel()))
                recipes.append(recipe.name).append('\t').append(recipe.supplyCost).append('\t').append(recipe.partCost).append('\t').append(recipe.baseTurns).append('\t').append(recipe.componentCosts).append('\n');
            Files.writeString(output.resolve("construction-costs.tsv"),recipes);
            Files.writeString(output.resolve("capture-metadata.properties"),"worldSeed="+g.world.seed+"\ncombatSeed=71000\ncombatPreset=rifles\ncombatTeamSize=10\nrenderer=GamePanel/FirstPersonRenderViewport\nimageSize=1280x800\ncommand=battlemap materials\n");
            DeveloperFacilityAuthority.execute(g,"battlemap",java.util.List.of("flat"));
            if(g.world.tileStructures.values().stream().anyMatch(m->m.floor()==TileStructureAuthority.Floor.VOID || m.railings()!=0))throw new AssertionError("Material bay did not restore flat arena");
            if(g.world.lightSources.stream().anyMatch(l->l.id.startsWith("STRUCTURE-LIGHT-")))throw new AssertionError("Material bay leaked wall lights after reset");
            System.out.println("StructureMaterialsCaptureProbe PASS images=7 nativeGamePanel=true nativeAssets=true materialBayReset=true");
        } finally {
            if(session!=null){DeveloperFacilityAuthority.leave(session.facility());SwingUtilities.invokeAndWait(()->{});}
            campaign.singlePlayerSectorBridge.close();campaign.shutdownRuntime();
        }
    }
    private static void capture(GamePanel g,Path output,StringBuilder poses,String name,double x,double z,double eye,double yaw,double pitch)throws Exception {
        g.options.doomModeEnabled=true;g.playerX=(int)x;g.playerY=(int)z;
        g.firstPersonRenderViewport.setInspectionPose(x,eye,z,Math.toRadians(yaw),Math.toRadians(pitch));
        snapshot(g,output.resolve(name+".png"));
        poses.append(name).append(".png\t").append(x).append('\t').append(z).append('\t').append(eye).append('\t').append(yaw).append('\t').append(pitch).append('\n');
    }
    private static void snapshot(GamePanel g,Path output)throws Exception {
        BufferedImage image=new BufferedImage(1280,800,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();
        try{g.paint(graphics);}finally{graphics.dispose();}ImageIO.write(image,"png",output.toFile());
    }
}
