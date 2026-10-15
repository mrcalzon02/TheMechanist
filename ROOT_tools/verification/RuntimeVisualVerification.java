package mechanist;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Developer-only driver. Compiled separately against the freshly packaged client. */
public final class RuntimeVisualVerification {
    private static Path output;
    private static Robot robot;
    private static JFrame frame;
    private static GamePanel panel;
    private static final StringBuilder evidence = new StringBuilder();

    public static void main(String[] args) throws Exception {
        output = Path.of(args[0]).toAbsolutePath();
        Files.createDirectories(output);
        int result = 1;
        try {
            require(!GraphicsEnvironment.isHeadless(), "A real virtual desktop is required");
            WorldStartLayoutAuthoritySmoke.main(new String[0]);
            CharacterCreationPortraitSelectionSmoke.main(new String[0]);
            robot = new Robot();
            robot.setAutoDelay(100);
            note("commit=" + System.getenv("GITHUB_SHA"));
            note("java=" + System.getProperty("java.version"));
            note("runtime=" + Path.of(".").toAbsolutePath().normalize());
            note("client=" + TheMechanist.class.getProtectionDomain().getCodeSource().getLocation());
            TheMechanist.main(new String[0]);
            await("client window", () -> {
                for (Frame candidate : Frame.getFrames()) {
                    if (candidate instanceof JFrame f && f.isShowing()
                            && f.getContentPane() instanceof GamePanel p) {
                        frame = f; panel = p; return true;
                    }
                }
                return false;
            }, 90);
            resize(1280, 800);
            snapshot("01-boot");
            await("natural boot to main menu", () -> panel.screen == GamePanel.Screen.MENU, 60);
            snapshot("02-main-menu");
            edt(() -> {
                require(panel.options.tileIconRendering, "Default configuration disables tile images");
                require(panel.images.get("title_mechanist_rebase") != null,
                        "Original title image is missing from the runtime");
                return null;
            });
            key(KeyEvent.VK_1);
            awaitStage(WorldStartFlowAuthority.Stage.WORLD_PICKER);
            await("world picker preview image", () -> flow().previewWorld != null
                    && flow().previewOverviewImage != null && !flow().previewWorkerRunning, 180);
            snapshot("03-world-picker");
            World readyPickerPreview = edt(() -> flow().previewWorld);
            long readyDraftSeed = edt(() -> panel.seed);
            key(KeyEvent.VK_G);
            awaitStage(WorldStartFlowAuthority.Stage.WORLD_GENERATION);
            edt(() -> {
                require(panel.seed == readyDraftSeed && flow().previewWorld == readyPickerPreview,
                        "Opening generation discarded the ready draft preview");
                note("draftPreviewReused=true");
                return null;
            });
            // The preceding assertion proves the ready picker draft survived the stage handoff.
            // Do not regenerate a second full-size 600x600 world for this graphics test.
            await("generation preview image", () -> flow().previewWorld != null
                    && flow().previewOverviewImage != null && !flow().previewWorkerRunning, 30);
            snapshot("04-world-generation");
            resize(960, 600);
            snapshot("04b-world-generation-960x600");
            resize(1280, 800);
            key(KeyEvent.VK_G);
            awaitStage(WorldStartFlowAuthority.Stage.CHARACTER_CREATION);
            edt(() -> {
                Rectangle body = flow().characterBodyRect();
                Rectangle footer = WorldStartLayoutAuthority.footer(flow().panelRect(panel.getWidth(), panel.getHeight()));
                require(!body.intersects(footer), "Character content overlaps help text");
                require(flow().CHARACTER_ACTION_LABELS.length == flow().CHARACTER_ACTION_COUNT,
                        "Visible character controls and keyboard action count disagree");
                return null;
            });
            snapshot("05-character-creation");
            edt(() -> {
                java.lang.reflect.Field mediaField = LegacyImageSurface.class.getDeclaredField("media");
                mediaField.setAccessible(true);
                ImageCache media = (ImageCache) mediaField.get(panel.images);
                note("loadedTileImages=" + media.tileArt.getRegistry().loadedCount()
                        + " authoredHumanPortraits=" + media.playerHumanPortraitCells.size());
                require(!media.playerHumanPortraitCells.isEmpty(), "Authored human portrait pool is empty");
                require(panel.images.getPlayerPortrait(panel.selectedNewGameCandidate()) != null,
                        "Character creation has no image portrait");
                note("characterPortraitImage=true seed=" + panel.seed);
                int boundRosterPortraits = 0;
                for (Candidate candidate : panel.candidates) {
                    require(panel.images.getPlayerPortrait(candidate) != null,
                            "Roster candidate " + (boundRosterPortraits + 1) + " lacks authored portrait art");
                    boundRosterPortraits++;
                }
                require(boundRosterPortraits > 0, "No candidate portraits were checked");
                note("rosterPortraitBindings=" + boundRosterPortraits);
                return null;
            });
            edt(() -> {
                BufferedImage missingArt = new BufferedImage(112, 112, BufferedImage.TYPE_INT_ARGB);
                Graphics2D painter = missingArt.createGraphics();
                try { flow().drawMissingPortrait(painter, new Rectangle(0, 0, 112, 112)); }
                finally { painter.dispose(); }
                int graphicPixels = 0;
                for (int y = 0; y < missingArt.getHeight(); y++) {
                    for (int x = 0; x < missingArt.getWidth(); x++) {
                        if ((missingArt.getRGB(x, y) >>> 24) != 0) graphicPixels++;
                    }
                }
                require(graphicPixels > 300, "Missing-art fallback did not render a graphical marker");
                note("missingPortraitGraphicPixels=" + graphicPixels);
                Method missingMarker = GamePanel.class.getDeclaredMethod("drawMissingWorldArtMarker",
                        Graphics2D.class, int.class, int.class, int.class);
                missingMarker.setAccessible(true);
                BufferedImage tileMarker = new BufferedImage(56, 56, BufferedImage.TYPE_INT_ARGB);
                Graphics2D painter2 = tileMarker.createGraphics();
                try { missingMarker.invoke(panel, painter2, 0, 0, 56); }
                finally { painter2.dispose(); }
                int markerPixels = 0;
                for (int y = 0; y < tileMarker.getHeight(); y++) {
                    for (int x = 0; x < tileMarker.getWidth(); x++) {
                        if ((tileMarker.getRGB(x, y) >>> 24) != 0) markerPixels++;
                    }
                }
                require(markerPixels > 1000, "Missing tile/object art has no graphical marker");
                note("missingWorldArtMarkerPixels=" + markerPixels);
                return null;
            });
            resize(960, 600);
            snapshot("06-character-creation-960x600");
            resize(1280, 800);
            Point startButton = edt(() -> {
                Rectangle bounds = flow().buttonRect(0);
                Point origin = flow().getLocationOnScreen();
                return new Point(origin.x + bounds.x + bounds.width / 2,
                        origin.y + bounds.y + bounds.height / 2);
            });
            robot.mouseMove(startButton.x, startButton.y);
            robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
            robot.waitForIdle();
            await("normal start-run mouse handoff", () -> panel.screen == GamePanel.Screen.GAME
                    && panel.world != null && !WorldStartFlowAuthority.isActive(panel), 180);
            snapshot("07-gameplay");
            edt(() -> { verifyWorldArt(); return null; });
            resize(960, 600);
            snapshot("08-gameplay-960x600");
            note("status=PASS");
            note("scope=Linux Java17 development package; manual screenshot review still required; no installer certification");
            result = 0;
        } catch (Throwable failure) {
            failure.printStackTrace();
            note("status=FAIL reason=" + failure);
            if (robot != null) {
                ImageIO.write(robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize())),
                        "png", output.resolve("failure-desktop.png").toFile());
            }
        } finally {
            Files.writeString(output.resolve("runtime-evidence.txt"), evidence.toString());
        }
        System.exit(result);
    }

    private static WorldStartFlowAuthority.WorldStartFlowOverlay flow() {
        return (WorldStartFlowAuthority.WorldStartFlowOverlay) panel.getClientProperty(
                WorldStartFlowAuthority.WorldStartFlowOverlay.CLIENT_PROPERTY);
    }

    private static void awaitStage(WorldStartFlowAuthority.Stage wanted) throws Exception {
        await(wanted.name(), () -> flow() != null && flow().stage == wanted, 180);
    }

    private static void await(String name, BooleanSupplier condition, int seconds) throws Exception {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        while (System.nanoTime() < until) {
            if (edt(condition::getAsBoolean)) { note("reached=" + name); return; }
            Thread.sleep(200);
        }
        throw new AssertionError("Timed out waiting for " + name);
    }

    private static <T> T edt(Callable<T> task) throws Exception {
        FutureTask<T> future = new FutureTask<>(task);
        SwingUtilities.invokeLater(future);
        return future.get(90, TimeUnit.SECONDS);
    }

    private static void key(int code) throws Exception {
        edt(() -> { frame.toFront(); panel.requestFocusInWindow(); return null; });
        robot.keyPress(code);
        robot.keyRelease(code);
        robot.waitForIdle();
    }

    private static void resize(int width, int height) throws Exception {
        edt(() -> {
            frame.setExtendedState(Frame.NORMAL);
            panel.setPreferredSize(new Dimension(width, height));
            frame.pack();
            frame.setLocation(20, 20);
            frame.toFront();
            panel.requestFocusInWindow();
            return null;
        });
        robot.waitForIdle();
    }

    private static void snapshot(String name) throws Exception {
        edt(() -> { panel.paintImmediately(0, 0, panel.getWidth(), panel.getHeight()); return null; });
        robot.waitForIdle();
        Toolkit.getDefaultToolkit().sync();
        Rectangle area = edt(() -> new Rectangle(panel.getLocationOnScreen(), panel.getSize()));
        BufferedImage image = robot.createScreenCapture(area);
        require(ImageIO.write(image, "png", output.resolve(name + ".png").toFile()), "PNG writer missing");
        note("snapshot=" + name + " size=" + area.width + "x" + area.height);
    }

    private static BufferedImage paint() {
        BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try { panel.paint(graphics); } finally { graphics.dispose(); }
        return image;
    }

    private static void verifyWorldArt() throws Exception {
        require(panel.options.tileIconRendering, "Image rendering was disabled during gameplay");
        BufferedImage normal = paint();
        int tile = panel.lastWorldViewTileSize;
        require(tile >= 13, "Tile viewport was not established");
        Method draw = GamePanel.class.getDeclaredMethod("drawCompiledTile", Graphics2D.class,
                World.class, int.class, int.class, CompiledTileDescriptor.class, char.class,
                int.class, int.class, int.class);
        draw.setAccessible(true);
        Method viewport = GamePanel.class.getDeclaredMethod("gameWorldViewportRect", int.class, int.class);
        viewport.setAccessible(true);
        Rectangle map = (Rectangle) viewport.invoke(panel, panel.getWidth(), panel.getHeight());
        int cols = Math.max(1, map.width / tile), rows = Math.max(1, map.height / tile);
        int checked = 0, imaged = 0;
        StringBuilder missing = new StringBuilder();
        BufferedImage probe = new BufferedImage(tile, tile, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = probe.createGraphics();
        try {
            for (int y = panel.lastWorldViewMinY; y < Math.min(panel.world.h, panel.lastWorldViewMinY + rows); y++) {
                int sy = panel.lastWorldViewOriginY + (y - panel.lastWorldViewMinY) * tile;
                if (sy + tile > panel.getHeight()) break;
                for (int x = panel.lastWorldViewMinX; x < Math.min(panel.world.w, panel.lastWorldViewMinX + cols); x++) {
                    int sx = panel.lastWorldViewOriginX + (x - panel.lastWorldViewMinX) * tile;
                    if (sx + tile > panel.getWidth()) break;
                    if (!panel.isRemembered(x, y)) continue;
                    char glyph = panel.world.tiles[x][y];
                    CompiledTileDescriptor descriptor = TileDataCompilationAuthority.resolve(panel.world, x, y, glyph);
                    checked++;
                    if ((Boolean) draw.invoke(panel, graphics, panel.world, x, y, descriptor, glyph, 0, 0, tile)) imaged++;
                    else if (missing.length() < 1000) missing.append("[").append(x).append(',').append(y)
                            .append(" glyph=").append((int)glyph).append("]");
                }
            }
        } finally { graphics.dispose(); }
        note("visibleTileImageBindings=" + imaged + "/" + checked + " missing=" + missing);
        require(panel.images.getPlayerPortrait(panel.active) != null,
                "Gameplay player portrait is missing; live sprite would use a missing-art marker");
        note("gameplayPlayerPortraitImage=true");
        int visibleSprites = 0;
        int missingSprites = 0;
        int minX = panel.lastWorldViewMinX, minY = panel.lastWorldViewMinY;
        int maxX = minX + cols, maxY = minY + rows;
        if (panel.world.mapObjects != null) {
            for (MapObjectState object : panel.world.mapObjects) {
                if (object == null || object.x < minX || object.x >= maxX
                        || object.y < minY || object.y >= maxY || !panel.isVisible(object.x, object.y)) continue;
                visibleSprites++;
                if (panel.images.getMapObjectImage(object) == null) missingSprites++;
            }
        }
        if (panel.baseObjects != null) {
            for (BaseObject object : panel.baseObjects) {
                if (object == null || object.x < minX || object.x >= maxX
                        || object.y < minY || object.y >= maxY || !panel.isVisible(object.x, object.y)) continue;
                visibleSprites++;
                if (panel.images.getBaseObjectImage(object) == null) missingSprites++;
            }
        }
        if (panel.world.npcs != null) {
            for (NpcEntity npc : panel.world.npcs) {
                if (npc == null || npc.x < minX || npc.x >= maxX
                        || npc.y < minY || npc.y >= maxY || !panel.isVisible(npc.x, npc.y)) continue;
                visibleSprites++;
                if (panel.images.getNpcPortraitFor(npc) == null) missingSprites++;
            }
        }
        note("visibleWorldSpriteImageBindings=" + (visibleSprites - missingSprites)
                + "/" + visibleSprites + " missing=" + missingSprites);
        require(missingSprites == 0, "Visible world entities or objects lack authored images");
        BufferedImage negative;
        try { panel.options.tileIconRendering = false; negative = paint(); }
        finally { panel.options.tileIconRendering = true; }
        long changed = 0;
        for (int y = Math.max(0, panel.lastWorldViewOriginY); y < Math.min(normal.getHeight(), map.y + map.height); y++) {
            for (int x = Math.max(0, panel.lastWorldViewOriginX); x < Math.min(normal.getWidth(), map.x + map.width); x++) {
                if (normal.getRGB(x, y) != negative.getRGB(x, y)) changed++;
            }
        }
        note("mapPixelsChangedWithImagesDisabled=" + changed);
        require(checked >= 10, "Insufficient remembered world tiles for graphics proof");
        require(imaged == checked, "Visible terrain still requires glyph fallback: " + missing);
        require(changed > (long) tile * tile, "Disabling tile art did not materially change the map");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void note(String value) { evidence.append(value).append('\n'); System.out.println(value); }
}
