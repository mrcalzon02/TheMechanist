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
            verifyEarlyScreenGraphicBindings();
            await("natural boot to main menu", () -> panel.screen == GamePanel.Screen.MENU, 60);
            snapshot("02-main-menu");
            edt(() -> { panel.screen = GamePanel.Screen.ZONE_SPLASH; return null; });
            snapshot("02a-graphical-zone-loading");
            edt(() -> { panel.screen = GamePanel.Screen.MENU; return null; });
            // Keep the normal client and menu route, but select the game's smallest
            // supported world-size option in this synthetic capture profile. The
            // default (larger) world size is unchanged for players.
            edt(() -> {
                WorldSetupSettings probeSetup = WorldSetupSettings.standard();
                probeSetup.zoneSize = 0;
                panel.worldSetup = probeSetup;
                panel.seed = 466713L;
                note("graphicsProbeWorldSize=" + probeSetup.zoneSizeTiles()
                        + " seed=" + panel.seed + " mode=smallest-supported");
                return null;
            });
            key(KeyEvent.VK_1);
            awaitStage(WorldStartFlowAuthority.Stage.WORLD_PICKER);
            snapshot("02b-world-picker-loading");
            long previewStarted = System.nanoTime();
            await("world picker preview image", () -> flow().previewWorld != null
                    && flow().previewOverviewImage != null && !flow().previewWorkerRunning, 420);
            note("worldPickerPreviewSeconds="
                    + TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - previewStarted));
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
            long worldGenerationStarted = System.nanoTime();
            key(KeyEvent.VK_G);
            awaitStage(WorldStartFlowAuthority.Stage.CHARACTER_CREATION);
            note("acceptedWorldGenerationSeconds="
                    + TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - worldGenerationStarted));
            edt(() -> {
                Rectangle body = flow().characterBodyRect();
                Rectangle footer = WorldStartLayoutAuthority.footer(flow().panelRect(panel.getWidth(), panel.getHeight()));
                require(!body.intersects(footer), "Character content overlaps help text");
                require(flow().CHARACTER_ACTION_LABELS.length == flow().CHARACTER_ACTION_COUNT,
                        "Visible character controls and keyboard action count disagree");
                return null;
            });
            snapshot("05-character-creation");
            verifyAlternatePortraitRaster();
            edt(() -> {
                java.lang.reflect.Field mediaField = LegacyImageSurface.class.getDeclaredField("media");
                mediaField.setAccessible(true);
                ImageCache media = (ImageCache) mediaField.get(panel.images);
                note("loadedTileImages=" + media.tileArt.getRegistry().loadedCount()
                        + " authoredHumanPortraits=" + media.playerHumanPortraitCells.size()
                        + " authoredNpcPortraits=" + media.npcPortraitCells.size()
                        + " npcFamilies=" + media.npcPortraitRanges.size());
                require(!media.playerHumanPortraitCells.isEmpty(), "Authored human portrait pool is empty");
                require(media.npcPortraitCells.size() >= 250,
                        "Shipped NPC portraits have not been rebound to the runtime image pool");
                for (String family : java.util.List.of("gangers", "mutants", "medicae",
                        "nobles", "enforcer_arebites", "pets", "administratum")) {
                    require(media.npcPortraitRanges.containsKey(family),
                            "Authored NPC image family is unavailable: " + family);
                }
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
        if (name.contains("preview") && flow() != null) {
            note("previewTimeoutDetails=" + edt(() -> "stage=" + flow().stage
                    + " running=" + flow().previewWorkerRunning
                    + " requested=" + flow().previewRequestedKey
                    + " ready=" + flow().previewKey
                    + " status=" + flow().previewStatus));
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
        verifyGeneratedWorldArtCensus(draw);
        verifyWorldSpriteArtCensus();
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


    /**
     * Uses the actual compiled tile-image draw path over distributed world cells.
     * This is a contact sheet from the generated world, not a gameplay screenshot
     * or an alternate renderer. It catches asset gaps outside the small spawn FOV.
     */
    private static void verifyGeneratedWorldArtCensus(Method draw) throws Exception {
        final int sampleColumns = 48, sampleRows = 32, size = 22;
        BufferedImage sheet = new BufferedImage(sampleColumns * size, sampleRows * size,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D canvas = sheet.createGraphics();
        java.util.Set<String> descriptorFamilies = new java.util.HashSet<>();
        int checked = 0, resolved = 0;
        StringBuilder missing = new StringBuilder();
        try {
            for (int row = 0; row < sampleRows; row++) {
                int wy = Math.min(panel.world.h - 1,
                        (int) ((2L * row + 1L) * panel.world.h / (2L * sampleRows)));
                for (int column = 0; column < sampleColumns; column++) {
                    int wx = Math.min(panel.world.w - 1,
                            (int) ((2L * column + 1L) * panel.world.w / (2L * sampleColumns)));
                    char glyph = panel.world.tiles[wx][wy];
                    CompiledTileDescriptor descriptor = TileDataCompilationAuthority.resolve(
                            panel.world, wx, wy, glyph);
                    descriptorFamilies.add(descriptor == null
                            ? "unknown:" + (int) glyph
                            : descriptor.baseLayer + ":" + descriptor.primaryArtKey + ":"
                                    + descriptor.overlayArtKey);
                    int sx = column * size, sy = row * size;
                    canvas.setColor(new Color(8, 9, 10));
                    canvas.fillRect(sx, sy, size, size);
                    checked++;
                    boolean drawn = (Boolean) draw.invoke(panel, canvas, panel.world,
                            wx, wy, descriptor, glyph, sx, sy, size);
                    if (drawn) {
                        resolved++;
                    } else {
                        canvas.setColor(new Color(237, 85, 68));
                        canvas.fillRect(sx, sy, size, size);
                        canvas.setColor(Color.BLACK);
                        canvas.drawLine(sx + 2, sy + 2, sx + size - 3, sy + size - 3);
                        canvas.drawLine(sx + size - 3, sy + 2, sx + 2, sy + size - 3);
                        if (missing.length() < 600) missing.append("[")
                                .append(wx).append(',').append(wy)
                                .append(" glyph=").append((int) glyph).append("]");
                    }
                }
            }
        } finally {
            canvas.dispose();
        }
        require(ImageIO.write(sheet, "png", output.resolve("09-generated-world-art-census.png").toFile()),
                "PNG writer failed for generated world art census");
        note("generatedWorldArtCensus=" + resolved + "/" + checked
                + " descriptorFamilies=" + descriptorFamilies.size()
                + " missing=" + missing);
        require(checked == sampleColumns * sampleRows, "Incomplete generated world art census");
        require(resolved == checked,
                "Generated world tile imagery has unresolved bindings beyond the spawn viewport: " + missing);
    }


    /**
     * The actual visible-world count can be 0/0 immediately after spawning.
     * Inspect the generated world records and rasterize representative loaded
     * portrait/object imagery independently of initial fog of war.
     */
    private static void verifyWorldSpriteArtCensus() throws Exception {
        final int columns = 12, rows = 8, cell = 72, maxPerType = 32;
        BufferedImage sheet = new BufferedImage(columns * cell, rows * cell, BufferedImage.TYPE_INT_RGB);
        Graphics2D painter = sheet.createGraphics();
        int cells = 0, sampledNpc = 0, sampledObjects = 0, sampledBuildings = 0;
        int unresolved = 0;
        StringBuilder missing = new StringBuilder();
        java.lang.reflect.Field mediaField = LegacyImageSurface.class.getDeclaredField("media");
        mediaField.setAccessible(true);
        ImageCache media = (ImageCache) mediaField.get(panel.images);
        try {
            painter.setColor(new Color(12, 14, 13));
            painter.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
            BufferedImage playerPortrait = panel.images.getPlayerPortrait(panel.active);
            require(playerPortrait != null, "Player sprite has no actual portrait image");
            painter.drawImage(playerPortrait, 4, 4, cell - 8, cell - 8, null);
            cells++;
            if (panel.world.npcs != null) {
                for (NpcEntity npc : panel.world.npcs) {
                    if (npc == null || sampledNpc == maxPerType || cells == columns * rows) break;
                    BufferedImage art = panel.images.getNpcPortraitFor(npc);
                    if (art == null) {
                        unresolved++;
                        if (missing.length() < 350) missing.append("npc:").append(npc.name).append(";");
                    }
                    paintSpriteCensusCell(painter, art, cells++, cell);
                    sampledNpc++;
                }
            }
            if (panel.world.mapObjects != null) {
                for (MapObjectState object : panel.world.mapObjects) {
                    if (object == null || sampledObjects == maxPerType || cells == columns * rows) break;
                    BufferedImage art = panel.images.getMapObjectImage(object);
                    if (art == null) {
                        unresolved++;
                        if (missing.length() < 350) missing.append("object:").append(object.label).append(";");
                    }
                    paintSpriteCensusCell(painter, art, cells++, cell);
                    sampledObjects++;
                }
            }
            if (panel.baseObjects != null) {
                for (BaseObject building : panel.baseObjects) {
                    if (building == null || sampledBuildings == maxPerType || cells == columns * rows) break;
                    BufferedImage art = panel.images.getBaseObjectImage(building);
                    if (art == null) {
                        unresolved++;
                        if (missing.length() < 350) missing.append("building:").append(building.name).append(";");
                    }
                    paintSpriteCensusCell(painter, art, cells++, cell);
                    sampledBuildings++;
                }
            }
            int authoredNpcPortraitSamples = 0;
            for (BufferedImage authored : media.npcPortraitCells) {
                if (cells == columns * rows || authoredNpcPortraitSamples >= 16) break;
                paintSpriteCensusCell(painter, authored, cells++, cell);
                authoredNpcPortraitSamples++;
            }
            require(authoredNpcPortraitSamples > 0, "Packaged client has no authored NPC portraits");
            note("generatedWorldSpriteCensus=npcs=" + sampledNpc
                    + " mapObjects=" + sampledObjects + " baseObjects=" + sampledBuildings
                    + " npcPortraitPool=" + media.npcPortraitCells.size()
                    + " authoredNpcSamples=" + authoredNpcPortraitSamples
                    + " missing=" + unresolved + " detail=" + missing);
        } finally {
            painter.dispose();
        }
        require(ImageIO.write(sheet, "png", output.resolve("10-world-sprite-art-census.png").toFile()),
                "Could not save world sprite image census");
        require(unresolved == 0, "Generated world entities have unresolved image assets: " + missing);
    }

    private static void paintSpriteCensusCell(Graphics2D painter, BufferedImage art,
                                              int position, int cell) {
        int x = (position % 12) * cell, y = (position / 12) * cell;
        painter.setColor(new Color(36, 41, 40));
        painter.fillRect(x + 1, y + 1, cell - 2, cell - 2);
        if (art == null) {
            painter.setColor(new Color(220, 58, 46));
            painter.fillRect(x + 4, y + 4, cell - 8, cell - 8);
            painter.setColor(Color.BLACK);
            painter.drawLine(x + 6, y + 6, x + cell - 6, y + cell - 6);
            painter.drawLine(x + cell - 6, y + 6, x + 6, y + cell - 6);
        } else {
            painter.drawImage(art, x + 4, y + 4, cell - 8, cell - 8, null);
        }
    }


    private static void verifyEarlyScreenGraphicBindings() throws Exception {
        edt(() -> {
            require(panel.options.tileIconRendering,
                    "Default graphical configuration unexpectedly disables tile images");
            for (String key : java.util.List.of("title_mechanist_rebase",
                    "subtitle_rebase", "new_world_backdrop_rebase",
                    "clouds_slow_rebase", "clouds_fast_rebase")) {
                BufferedImage art = panel.images.get(key);
                require(art != null && art.getWidth() > 0 && art.getHeight() > 0,
                        "Missing authored early-screen artwork: " + key);
                note("uiArt." + key + "=" + art.getWidth() + "x" + art.getHeight());
            }
            BufferedImage spinnerArt = panel.images.getCompiledSystemIcon(4, 2, 1);
            require(spinnerArt != null && spinnerArt.getWidth() > 0,
                    "Packaged boot/loading spinner system image missing");
            note("uiArt.bootLoadingSpinner=" + spinnerArt.getWidth() + "x" + spinnerArt.getHeight());
            int menuIcons = 0;
            for (String label : panel.mainMenuRouteLabels()) {
                require(panel.systemButtonIconForLabel(label) != null,
                        "Main menu icon artwork is missing for: " + label);
                menuIcons++;
            }
            note("uiArt.mainMenuRouteIcons=" + menuIcons + "/" + panel.mainMenuRouteLabels().size());
            note("uiArt.menuButtonSkin=" + (panel.images.get("button_normal") != null
                    && panel.images.get("button_hover") != null ? "authored" : "drawn-fallback"));
            return null;
        });
    }

    private static void verifyAlternatePortraitRaster() throws Exception {
        int previous = edt(() -> panel.candidateIndex);
        require(panel.candidates.size() >= 2, "Need two candidates for live portrait switch test");
        BufferedImage first = edt(() -> panel.images.getPlayerPortrait(panel.candidates.get(previous)));
        int alternate = (previous + 1) % panel.candidates.size();
        BufferedImage second = edt(() -> {
            panel.candidateIndex = alternate;
            return panel.images.getPlayerPortrait(panel.selectedNewGameCandidate());
        });
        try {
            require(first != null && second != null, "Candidate portrait switch has no image");
            int different = 0;
            for (int y = 0; y < 64; y += 2) {
                for (int x = 0; x < 64; x += 2) {
                    int x1 = Math.min(first.getWidth() - 1, x * first.getWidth() / 64);
                    int y1 = Math.min(first.getHeight() - 1, y * first.getHeight() / 64);
                    int x2 = Math.min(second.getWidth() - 1, x * second.getWidth() / 64);
                    int y2 = Math.min(second.getHeight() - 1, y * second.getHeight() / 64);
                    if (first.getRGB(x1, y1) != second.getRGB(x2, y2)) different++;
                }
            }
            note("uiArt.alternatePortraitDifferentPixels=" + different);
            require(different > 100, "Switching roster candidates does not change portrait art");
            snapshot("05b-character-creation-alternate-portrait");
        } finally {
            edt(() -> { panel.candidateIndex = previous; return null; });
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void note(String value) { evidence.append(value).append('\n'); System.out.println(value); }
}
