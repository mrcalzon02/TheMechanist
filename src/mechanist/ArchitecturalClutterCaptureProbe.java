package mechanist;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Same facility, native artwork and GamePanel. Matching before/after cameras plus elevated detail views. */
final class ArchitecturalClutterCaptureProbe {
    public static void main(String[] args)throws Exception {
        Path output=Path.of(args.length==0?"../diagnostics/architecture-clutter-gallery-20261102":args[0]);Files.createDirectories(output);
        GamePanel campaign=new GamePanel();campaign.timer.stop();campaign.world=new World(19191,32,24);
        for(char[] c:campaign.world.tiles)Arrays.fill(c,'.');campaign.active=Candidate.random(new Random(3));campaign.seed=19191;
        campaign.singlePlayerSectorBridge=new SinglePlayerSectorRuntimeBridge(new SectorManager());
        DeveloperFacilityAuthority.Session session=null;
        try {
            session=DeveloperFacilityAuthority.enter(campaign);GamePanel g=session.facility();g.timer.stop();
            g.options.musicEnabled=false;g.options.soundEnabled=false;g.options.tileIconRendering=true;g.options.doomModeEnabled=true;
            g.screen=GamePanel.Screen.GAME;g.panelMode=GamePanel.PanelMode.NONE;g.active.stats.put("Vision",42);g.setSize(1280,800);
            DeveloperFacilityAuthority.execute(g,"battlemap",java.util.List.of("clutter"));
            Rectangle r=DeveloperFacilityAuthority.layout(g).rooms.get("combat");
            StringBuilder poses=new StringBuilder("image\tx\tz\teyeHeight\tyaw\tpitch\tclutterVersion\n");
            ArchitecturalClutterAuthority.enable(g.world,false);
            capture(g,output,poses,"01-before-service-aisle",r.x+9,r.y+2.5,.58,0,12);
            ArchitecturalClutterAuthority.enable(g.world,true);
            capture(g,output,poses,"02-after-service-aisle",r.x+9,r.y+2.5,.58,0,12);
            capture(g,output,poses,"03-wall-pipes-and-sealed-openings",r.x+18,r.y+1.1,.58,-95,5);
            capture(g,output,poses,"04-upper-wall-strata",r.x+23,r.y+12,3.4,-90,18);
            capture(g,output,poses,"05-underhang-looking-up",r.x+18,r.y+3,.58,0,53);
            capture(g,output,poses,"06-upper-services-inspection",r.x+13,r.y+3,6.0,0,5);
            g.options.doomModeEnabled=false;g.firstPersonRenderViewport.clearInspectionPose();g.playerX=r.x+18;g.playerY=r.y+3;
            snapshot(g,output.resolve("07-normal-planner.png"));
            Files.writeString(output.resolve("vantage-points.tsv"),poses);
            StringBuilder inventory=new StringBuilder("tileX\ttileZ\tfeature\tgeneration\tx0\theight0\tz0\tx1\theight1\tz1\ttintRGB\tnativeArtGlyph\n");
            EnumMap<ArchitecturalClutterAuthority.Feature,Integer> counts=new EnumMap<>(ArchitecturalClutterAuthority.Feature.class);
            for(int x=r.x;x<r.x+r.width;x++)for(int z=r.y;z<r.y+r.height;z++)for(var p:ArchitecturalClutterAuthority.pieces(g.world,x,z)) {
                counts.merge(p.feature(),1,Integer::sum);
                inventory.append(x).append('\t').append(z).append('\t').append(p.feature()).append('\t').append(p.generation()).append('\t').append(p.x0()).append('\t').append(p.h0()).append('\t').append(p.z0()).append('\t').append(p.x1()).append('\t').append(p.h1()).append('\t').append(p.z1()).append('\t').append(Integer.toHexString(p.tint())).append('\t').append(p.art()).append('\n');
            }
            Files.writeString(output.resolve("clutter-pieces.tsv"),inventory);
            Files.writeString(output.resolve("capture-data.txt"),"worldSeed="+g.world.seed+"\nrecipeVersion="+g.world.architecturalClutterVersion+"\nfeatureCounts="+counts+"\nrenderer=existing GamePanel/FirstPersonRenderViewport\nassets=shipped wall and closed-door art\nElevated inspection poses do not certify traversable local upper storeys.\n");
            DeveloperFacilityAuthority.execute(g,"battlemap",java.util.List.of("flat"));
            if(g.world.architecturalClutterVersion!=0 || g.world.lightSources.stream().anyMatch(l->l.id.startsWith("CLUTTER-LIGHT-")))throw new AssertionError("Clutter bay reset leaked state");
            System.out.println("ArchitecturalClutterCaptureProbe PASS images=7 features="+counts+" reset=true");
        } finally {
            if(session!=null){DeveloperFacilityAuthority.leave(session.facility());SwingUtilities.invokeAndWait(()->{});}
            campaign.singlePlayerSectorBridge.close();campaign.shutdownRuntime();
        }
    }
    private static void capture(GamePanel g,Path output,StringBuilder poses,String name,double x,double z,double eye,double yaw,double pitch)throws Exception {
        g.playerX=(int)x;g.playerY=(int)z;g.firstPersonRenderViewport.setInspectionPose(x,eye,z,Math.toRadians(yaw),Math.toRadians(pitch));
        snapshot(g,output.resolve(name+".png"));poses.append(name).append(".png\t").append(x).append('\t').append(z).append('\t').append(eye).append('\t').append(yaw).append('\t').append(pitch).append('\t').append(g.world.architecturalClutterVersion).append('\n');
    }
    private static void snapshot(GamePanel g,Path path)throws Exception {
        BufferedImage image=new BufferedImage(1280,800,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();
        try{g.paint(graphics);}finally{graphics.dispose();}ImageIO.write(image,"png",path.toFile());
    }
}
