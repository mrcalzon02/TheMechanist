package mechanist;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Offscreen evidence only; does not create or automate desktop windows. */
public final class BillboardFixtureCapture {
    public static void main(String[] args) throws Exception {
        Path output=Path.of(args[0]).toAbsolutePath();
        SwingUtilities.invokeAndWait(()-> {
            GamePanel panel=new GamePanel();
            panel.timer.stop();
            try {
                panel.options.doomModeEnabled=true;
                panel.options.doomModeFovDegrees=80;
                panel.options.soundEnabled=false;
                panel.options.musicEnabled=false;
                panel.options.lightingFxIndex=1;
                panel.setSize(1280,800);
                panel.world=new World(406L,24,18);
                panel.world.floor=4;
                panel.world.zoneType=ZoneType.NEUTRAL_CIVILIAN_FLOOR;
                for(char[] column:panel.world.tiles) java.util.Arrays.fill(column,'.');
                for(int y=0;y<18;y++) panel.world.tiles[10][y]='#';
                panel.playerX=4;panel.playerY=5;
                panel.active=Candidate.random(new Random(406L));
                panel.food=100;panel.water=100;
                panel.activePortableLightItem="Flashlight";
                panel.activePortableLightExpiresTurn=100;
                panel.screen=GamePanel.Screen.GAME;
                panel.world.tiles[8][4]='u';
                panel.baseObjects.add(new BaseObject("Inspection workbench",'w',7,5,0,0));
                MapObjectState debris=new MapObjectState();
                debris.x=6;debris.y=4;debris.glyph='*';debris.label="Floor salvage";debris.type="debris";
                panel.world.mapObjects.add(debris);
                MapObjectState crate=new MapObjectState();
                crate.x=6;crate.y=6;crate.glyph='s';crate.label="Supply crate";crate.type="container";
                panel.world.mapObjects.add(crate);
                Field cameraField=FirstPersonRenderViewport.class.getDeclaredField("camera");
                cameraField.setAccessible(true);
                FirstPersonCamera camera=(FirstPersonCamera)cameraField.get(panel.firstPersonRenderViewport);
                Method acquire=FirstPersonRenderViewport.class.getDeclaredMethod("acquireTarget",GamePanel.class);
                acquire.setAccessible(true);
                StringBuilder targets=new StringBuilder("file\ttarget\tx\ty\n");
                for(int i=0;i<2;i++) {
                    if(i==1) camera.addYawPitch(Math.atan2(-1,2),Math.atan2(.13-.58,Math.sqrt(5)));
                    panel.updateSensoryModel("billboard fixture capture");
                    BufferedImage image=new BufferedImage(1280,800,BufferedImage.TYPE_INT_RGB);
                    Graphics2D g=image.createGraphics();panel.paint(g);g.dispose();
                    String name=i==0?"fixture-workbench.png":"fixture-floor-salvage.png";
                    ImageIO.write(image,"png",output.resolve(name).toFile());
                    DoomRayTarget target=(DoomRayTarget)acquire.invoke(panel.firstPersonRenderViewport,panel);
                    targets.append(name).append('\t').append(target.label()).append('\t')
                            .append(target.hit().tileX()).append('\t').append(target.hit().tileY()).append('\n');
                    if(!target.hit().label().equals(i==0?"Inspection workbench":"Floor salvage"))
                        throw new AssertionError("Wrong visible billboard target: "+target.label());
                }
                Files.writeString(output.resolve("fixture-targets.tsv"),targets.toString());
            } catch(Exception failure) { throw new RuntimeException(failure); }
            finally { panel.shutdownRuntime(); }
        });
        System.out.println("PASS: built-object and pitched floor-salvage reticles match billboards");
        System.exit(0);
    }
}
