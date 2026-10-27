package mechanist;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Diagnostic driver only: uses the existing game start, sensory and paint paths. */
public final class GeneratedWorldRenderBaseline {
    static GamePanel panel;
    static JFrame window;
    static Path output;
    static final StringBuilder evidence = new StringBuilder(
        "file\tseed\tzone\tfloor\tsewer\tpoint\tx\ty\tglyph\tview\tfov\tyaw\tpitch\twidth\theight\tpaintMs\trooms\tnpcs\tobjects\n");

    public static void main(String[] args) throws Exception {
        output = Path.of(args[0]).toAbsolutePath();
        Files.createDirectories(output);
        System.setProperty("mechanist.storage.root", output.resolve("user").toString());
        System.out.println("GAME_CLASSES=" + GamePanel.class.getProtectionDomain().getCodeSource().getLocation());
        Thread watchdog = new Thread(() -> {
            try { Thread.sleep(25000); } catch (InterruptedException ignored) { return; }
            for (var entry : Thread.getAllStackTraces().entrySet()) {
                if (!entry.getKey().getName().equals("main") && !entry.getKey().getName().startsWith("AWT-EventQueue")) continue;
                System.out.println("DIAGNOSTIC_THREAD " + entry.getKey().getName());
                for (StackTraceElement line : entry.getValue()) System.out.println("  " + line);
            }
        }, "baseline-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        try {
            edt(() -> {
                System.out.println("CREATING_GAME_PANEL");
                panel = new GamePanel();
                System.out.println("GAME_PANEL_CREATED");
                panel.timer.stop();
                panel.options.soundEnabled = false;
                panel.options.musicEnabled = false;
                panel.options.screenSaver = false;
                panel.options.singlePlayerTickModeIndex = 0;
                panel.setPreferredSize(new Dimension(1280, 800));
                panel.setSize(1280, 800);
                return null;
            });
            for (long seed : new long[]{466713L, 20261025L, 8675309L}) {
                WorldSetupSettings setup = WorldSetupSettings.standard();
                setup.zoneSize = 0;
                long begin = System.nanoTime();
                WorldAtlas atlas = WorldAtlas.preview(seed, setup);
                atlas.generateScaffold();
                System.out.println("GENERATED seed=" + seed + " seconds=" + (System.nanoTime()-begin)/1e9);
                edt(() -> {
                    panel.seed = seed;
                    panel.atlas = atlas;
                    panel.newGameSetupActive = true;
                    panel.startPackagedClientNewGameWith(Candidate.random(new Random(seed ^ 0xC0FFEE)), setup);
                    panel.timer.stop();
                    return null;
                });
                captureSlice(seed, "insertion");
                if (seed == 466713L) {
                    atlas.floor = 4;
                    atlas.sewer = true;
                    World sewer = atlas.currentWorld();
                    edt(() -> { bind(sewer); return null; });
                    captureSlice(seed, "sewer");
                    atlas.sewer = false;
                    atlas.floor = 8;
                    World upper = atlas.currentWorld();
                    edt(() -> { bind(upper); return null; });
                    captureSlice(seed, "upper");
                }
            }
            Files.writeString(output.resolve("captures.tsv"), evidence.toString());
            System.out.println("BASELINE_CAPTURE_COMPLETE");
        } catch (Throwable failure) {
            Files.writeString(output.resolve("captures.tsv"), evidence.toString());
            failure.printStackTrace();
            System.exit(1);
        }
        edt(() -> { panel.timer.stop(); return null; });
        System.exit(0);
    }

    static void bind(World world) {
        panel.world = world;
        panel.visibleTiles = new boolean[world.w][world.h];
        panel.rememberedTiles = new boolean[world.w][world.h];
        panel.lightLevels = new int[world.w][world.h];
        panel.sensoryWorldRef = null;
        Point p = world.startPoint();
        panel.playerX = p.x;
        panel.playerY = p.y;
        panel.screen = GamePanel.Screen.GAME;
    }

    static void captureSlice(long seed, String slice) throws Exception {
        World world = panel.world;
        LinkedHashMap<String, Point> points = new LinkedHashMap<>();
        points.put("spawn", world.startPoint());
        Point street = nearest(world, world.startPoint(), true);
        if (street != null) points.put("street", street);
        Point room = nearest(world, world.startPoint(), false);
        if (room != null) points.put("room", room);
        for (var entry : points.entrySet()) {
            String name = seed + "-" + slice + "-" + entry.getKey();
            Point p = entry.getValue();
            edt(() -> {
                panel.playerX = p.x; panel.playerY = p.y;
                panel.lookX = p.x; panel.lookY = p.y;
                panel.options.doomModeEnabled = false;
                panel.setFacingToward(p.x + 1, p.y, "baseline capture");
                panel.updateSensoryModel("baseline capture");
                return null;
            });
            shot(name + "-2d", entry.getKey(), "2d", 0, 0);
            edt(() -> {
                panel.options.doomModeEnabled = true;
                panel.options.doomModeFovDegrees = 80;
                panel.firstPersonRenderViewport = new FirstPersonRenderViewport();
                return null;
            });
            shot(name + "-doom", entry.getKey(), "doom", 0, 0);
            if (slice.equals("insertion") && entry.getKey().equals("spawn") && seed == 466713L) {
                for (int fov : new int[]{60,110}) {
                    edt(() -> { panel.options.doomModeFovDegrees = fov; return null; });
                    shot(name + "-doom-fov" + fov, "spawn", "doom", 0, 0);
                }
                edt(() -> { panel.options.doomModeFovDegrees = 80; return null; });
                for (int pitch : new int[]{-60,60}) {
                    edt(() -> {
                        panel.firstPersonRenderViewport = new FirstPersonRenderViewport();
                        Field f = FirstPersonRenderViewport.class.getDeclaredField("camera");
                        f.setAccessible(true);
                        ((FirstPersonCamera)f.get(panel.firstPersonRenderViewport)).addYawPitch(0, Math.toRadians(pitch));
                        return null;
                    });
                    shot(name + "-doom-pitch" + pitch, "spawn", "doom", 0, pitch);
                }
            }
        }
    }

    static Point nearest(World w, Point start, boolean street) {
        Point best = null;
        double score = Double.MAX_VALUE;
        for (int x=2; x<w.w-2; x++) for (int y=2; y<w.h-2; y++) {
            if (!w.walkable(x,y) || w.npcAt(x,y) != null) continue;
            boolean match = street ? RoadGridIntegrationAuthority.isRoadLane(w,x,y)
                : w.roomIds[x][y] > 0 && w.tiles[x][y] == '.';
            if (!match) continue;
            double distance = start.distanceSq(x,y);
            if (!street && distance < 100) continue;
            if (distance < score) { score=distance; best=new Point(x,y); }
        }
        return best;
    }

    static void shot(String name, String point, String view, int yaw, int pitch) throws Exception {
        edt(() -> {
            BufferedImage image = new BufferedImage(1280,800,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();
            long start=System.nanoTime();
            panel.paint(g);
            double elapsed=(System.nanoTime()-start)/1e6;
            g.dispose();
            ImageIO.write(image,"png",output.resolve(name+".png").toFile());
            World w=panel.world;
            evidence.append(String.join("\t",name+".png",Long.toString(panel.seed),w.zoneType.name(),
                Integer.toString(w.floor),Boolean.toString(w.sewerLayer),point,Integer.toString(panel.playerX),
                Integer.toString(panel.playerY),Character.toString(w.tiles[panel.playerX][panel.playerY]),view,
                Integer.toString(panel.options.doomModeFovDegrees),Integer.toString(yaw),Integer.toString(pitch),
                "1280","800",String.format(Locale.US,"%.3f",elapsed),Integer.toString(w.rooms.size()),
                Integer.toString(w.npcs.size()),Integer.toString(w.mapObjects.size()))).append('\n');
            Files.writeString(output.resolve("captures.tsv"), evidence.toString());
            System.out.println("CAPTURE " + name + " x="+panel.playerX+" y="+panel.playerY+" paintMs="+elapsed);
            return null;
        });
    }

    static <T> T edt(Callable<T> action) throws Exception {
        java.util.concurrent.FutureTask<T> task=new java.util.concurrent.FutureTask<>(action);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }
}
