package mechanist;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

/**
 * Experimental first-person renderer for the hidden QoL "doom" mode. This is a
 * Java2D software ray/raster path that keeps the normal Swing UI alive; LWJGL is
 * now packaged by Maven for a later native OpenGL backend, but this code path is
 * deliberately dependency-free so the base game can still compile and run in
 * fallback environments.
 */
final class FirstPersonRenderViewport {
    private final FirstPersonCamera camera = new FirstPersonCamera();
    private final FirstPersonMouseInput mouseInput = new FirstPersonMouseInput();
    private final ViewportRaycaster raycaster = new ViewportRaycaster();
    private final BillboardSpriteRenderer billboardRenderer = new BillboardSpriteRenderer();
    private final LightDecayEngine lightDecay = new LightDecayEngine();
    private final DoomDepthFog fog = new DoomDepthFog();
    private final ThreeDimensionalProjectileRenderer projectileRenderer = new ThreeDimensionalProjectileRenderer();
    private final ParticleSplashSystem particles = new ParticleSplashSystem();
    private final CameraJuiceSystem cameraJuice = new CameraJuiceSystem();
    private final ScreenGlitchEffect glitchEffect = new ScreenGlitchEffect();
    private final GameHudOverlay hudOverlay = new GameHudOverlay();
    private final ContinuousGridPlayer continuousPlayer = new ContinuousGridPlayer(0.5, 0.5);
    private final ArrayList<RenderableSprite> spriteScratch = new ArrayList<>(256);
    private final java.util.HashMap<Long, BufferedImage> firstPersonFloorTextureCache = new java.util.HashMap<>();
    private final java.util.HashMap<Long, BufferedImage> firstPersonWallTextureCache = new java.util.HashMap<>();
    private BufferedImage frame;
    private int[] pixels = new int[0];
    private long lastFrameMillis = System.currentTimeMillis();
    private long lastContinuousMotionNanos = System.nanoTime();
    private int lastPlayerX = Integer.MIN_VALUE, lastPlayerY = Integer.MIN_VALUE;
    private boolean[] wallLightScratch = new boolean[0];
    private int wallLightScratchW = -1, wallLightScratchH = -1;
    private boolean continuousMovementInitialized = false;
    private boolean sprintHeld = false;
    private boolean crouchHeld = false;
    private int movementModeBeforeHold = GamePanel.MOTION_WALK;
    private String lastStatus = "doom mode dormant";

    boolean isUnlocked(GameOptions options) { return options != null && options.doomModeEnabled; }

    String statusLine(GameOptions options) {
        return isUnlocked(options)
                ? "doom mode ON / Java2D raster backend / FOV " + Math.max(60, Math.min(110, options.doomModeFovDegrees)) + " deg / fog " + options.doomFogModeLabel()
                : "doom mode OFF / renderer locked";
    }

    boolean handleMouseMoved(GamePanel panel, int x, int y) {
        if (!active(panel)) return false;
        mouseInput.handleMove(camera, panel, x, y);
        continuousPlayer.setLookAngleRadians(camera.yawRadians());
        return true;
    }

    boolean handleMouseDragged(GamePanel panel, int x, int y) { return handleMouseMoved(panel, x, y); }

    boolean handleMouseClicked(GamePanel panel, MouseEvent e, int virtualX, int virtualY) {
        if (!active(panel) || e == null) return false;
        if (SwingUtilities.isRightMouseButton(e)) {
            DoomRayTarget target = acquireTarget(panel);
            if (aimWeaponAtTarget(panel, target)) {
                cameraJuice.applyWeaponRecoil();
                glitchEffect.trigger(System.currentTimeMillis(), 95L);
                panel.repaint();
                return true;
            }
            panel.logEvent(target.weaponHint());
            return true;
        }
        if (SwingUtilities.isLeftMouseButton(e)) {
            interactWithTarget(panel, acquireTarget(panel));
            return true;
        }
        return false;
    }

    boolean handleKeyPressed(GamePanel panel, int code) {
        if (!active(panel)) return false;
        ensureContinuousPlayerSynced(panel);
        switch (code) {
            case KeyEvent.VK_W, KeyEvent.VK_UP -> { continuousPlayer.setForward(true); return true; }
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> { continuousPlayer.setBackward(true); return true; }
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> { continuousPlayer.setStrafeLeft(true); return true; }
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> { continuousPlayer.setStrafeRight(true); return true; }
            case KeyEvent.VK_SHIFT -> { setMovementModifier(panel, true, true); return true; }
            case KeyEvent.VK_CONTROL, KeyEvent.VK_C -> { setMovementModifier(panel, false, true); return true; }
            case KeyEvent.VK_L -> { lookAtTarget(panel, acquireTarget(panel)); return true; }
            case KeyEvent.VK_E, KeyEvent.VK_SPACE -> { interactWithTarget(panel, acquireTarget(panel)); return true; }
            case KeyEvent.VK_F -> {
                DoomRayTarget target = acquireTarget(panel);
                if (!aimWeaponAtTarget(panel, target)) panel.logEvent(target.weaponHint());
                return true;
            }
            case KeyEvent.VK_Q, KeyEvent.VK_TAB -> { panel.cycleEquippedWeaponHand(); return true; }
            case KeyEvent.VK_X -> { panel.reloadCurrentRangedWeapon(); return true; }
            case KeyEvent.VK_OPEN_BRACKET -> { panel.options.doomModeFovDegrees = Math.max(60, panel.options.doomModeFovDegrees - 5); panel.options.save(); return true; }
            case KeyEvent.VK_CLOSE_BRACKET -> { panel.options.doomModeFovDegrees = Math.min(110, panel.options.doomModeFovDegrees + 5); panel.options.save(); return true; }
            default -> { return false; }
        }
    }

    boolean handleKeyReleased(GamePanel panel, int code) {
        if (!active(panel)) return false;
        return switch (code) {
            case KeyEvent.VK_W, KeyEvent.VK_UP -> { continuousPlayer.setForward(false); yield true; }
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> { continuousPlayer.setBackward(false); yield true; }
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> { continuousPlayer.setStrafeLeft(false); yield true; }
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> { continuousPlayer.setStrafeRight(false); yield true; }
            case KeyEvent.VK_SHIFT -> { setMovementModifier(panel, true, false); yield true; }
            case KeyEvent.VK_CONTROL, KeyEvent.VK_C -> { setMovementModifier(panel, false, false); yield true; }
            default -> false;
        };
    }

    void updateContinuousMotion(GamePanel panel) {
        if (!active(panel)) {
            mouseInput.release(panel);
            continuousPlayer.setMovementIntent(false, false, false, false);
            if (panel != null && (sprintHeld || crouchHeld)) panel.selectedMovementModeIndex = movementModeBeforeHold;
            sprintHeld = false;
            crouchHeld = false;
            continuousMovementInitialized = false;
            lastContinuousMotionNanos = System.nanoTime();
            return;
        }
        mouseInput.ensureLock(panel);
        ensureContinuousPlayerSynced(panel);
        applyMovementModeTuning(panel);
        long now = System.nanoTime();
        double dt = Math.max(0.0, Math.min(0.10, (now - lastContinuousMotionNanos) / 1_000_000_000.0));
        lastContinuousMotionNanos = now;
        if (mouseInput.lookIdleSeconds(now) >= 0.85) camera.driftTowardCardinal(dt);
        continuousPlayer.setLookAngleRadians(camera.yawRadians());
        ContinuousPlayerState state = continuousPlayer.update(new MechanistContinuousCollisionGrid(panel.world, panel.playerX, panel.playerY), dt);
        if (state.velocityX() * state.velocityX() + state.velocityY() * state.velocityY() > 0.000625) cameraJuice.noteMovementImpulse();
        LogicalTile tile = state.logicalTile();
        if (panel.world != null && panel.world.inBounds(tile.x(), tile.y()) && panel.world.walkable(tile.x(), tile.y()) && panel.world.npcAt(tile.x(), tile.y()) == null) {
            if (tile.x() != panel.playerX || tile.y() != panel.playerY) {
                panel.setFacingToward(tile.x(), tile.y(), "doom-continuous-grid-motion");
                panel.playerX = tile.x();
                panel.playerY = tile.y();
                panel.lookX = panel.playerX;
                panel.lookY = panel.playerY;
                panel.lookCursorActive = false;
                panel.interactCursorActive = false;
                panel.enforceEntityOccupancy("doom-continuous-grid-motion");
            }
            camera.setPosition(state.posX(), cameraHeightFor(panel), state.posY());
            lastPlayerX = panel.playerX;
            lastPlayerY = panel.playerY;
        }
    }

    void render(Graphics2D g, GamePanel panel) {
        if (!active(panel) || g == null || panel == null || panel.world == null) return;
        int w = Math.max(160, panel.getWidth());
        int h = Math.max(120, panel.getHeight());
        ensureFrame(w, h);
        long now = System.currentTimeMillis();
        double dt = Math.max(0.0, Math.min(0.1, (now - lastFrameMillis) / 1000.0));
        lastFrameMillis = now;
        syncCameraToPlayer(panel);
        CameraJuiceState juice = cameraJuice.update(dt);
        camera.applyVisualOffsets(juice);
        glitchEffect.setForced(panel.active != null && panel.active.body != null && lowHealthRatio(panel) < 0.24);
        particles.update(now);
        projectileRenderer.update(now);
        rasterizeWorld(panel, w, h, now);
        glitchEffect.apply(pixels, w, h, now);
        Graphics2D gg = (Graphics2D) g.create();
        try {
            gg.drawImage(frame, 0, 0, null);
            renderSpritesAndEffects(gg, panel, w, h, now, dt);
            renderGhostMovementProjection(gg, panel, w, h);
            renderHud(gg, panel, w, h);
            hudOverlay.renderHUD(gg, PlayerState.fromPanel(panel), w, h);
        } finally {
            gg.dispose();
        }
    }

    private boolean active(GamePanel panel) {
        return panel != null && panel.options != null && panel.options.doomModeEnabled && panel.screen == GamePanel.Screen.GAME && panel.world != null;
    }

    private void ensureFrame(int w, int h) {
        if (frame == null || frame.getWidth() != w || frame.getHeight() != h) {
            frame = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
            pixels = ((java.awt.image.DataBufferInt) frame.getRaster().getDataBuffer()).getData();
        }
    }

    private double lowHealthRatio(GamePanel panel) {
        if (panel == null || panel.active == null || panel.active.body == null || panel.active.body.isEmpty()) return 1.0;
        double cur = 0.0, max = 0.0;
        for (BodyPart bp : panel.active.body.values()) { if (bp != null) { cur += bp.currentHealth(); max += Math.max(1.0, bp.maxHealth()); } }
        return max <= 0.0 ? 1.0 : Math.max(0.0, Math.min(1.0, cur / max));
    }

    private void syncCameraToPlayer(GamePanel panel) {
        ensureContinuousPlayerSynced(panel);
        camera.setPosition(continuousPlayer.posX(), cameraHeightFor(panel), continuousPlayer.posY());
        lastPlayerX = panel.playerX;
        lastPlayerY = panel.playerY;
    }

    private double cameraHeightFor(GamePanel panel) {
        if (panel != null && panel.selectedMovementModeIndex == GamePanel.MOTION_SNEAK) return 0.42;
        return 0.58;
    }

    private void setMovementModifier(GamePanel panel, boolean sprint, boolean held) {
        if (held && !sprintHeld && !crouchHeld) movementModeBeforeHold = panel.selectedMovementModeIndex;
        if (sprint) sprintHeld = held;
        else crouchHeld = held;
        panel.selectedMovementModeIndex = heldMovementMode(movementModeBeforeHold, sprintHeld, crouchHeld);
        if (panel.world != null) {
            panel.world.dirtyVisionRevision++;
            panel.world.dirtyNoiseRevision++;
        }
    }

    private void applyMovementModeTuning(GamePanel panel) {
        MotionTuning tuning = motionTuningForMode(panel.selectedMovementModeIndex);
        continuousPlayer.setMotionTuning(tuning.maxSpeed(), tuning.acceleration(), tuning.damping());
    }

    static int heldMovementMode(int baseMode, boolean sprintHeld, boolean crouchHeld) {
        if (crouchHeld) return GamePanel.MOTION_SNEAK;
        if (sprintHeld) return GamePanel.MOTION_SPRINT;
        return baseMode;
    }

    static MotionTuning motionTuningForMode(int mode) {
        return switch (mode) {
            case GamePanel.MOTION_SNEAK -> new MotionTuning(1.35, 8.5, 11.0);
            case GamePanel.MOTION_RUN -> new MotionTuning(4.15, 21.0, 8.0);
            case GamePanel.MOTION_SPRINT -> new MotionTuning(5.65, 27.0, 7.0);
            default -> new MotionTuning(3.10, 18.0, 9.0);
        };
    }

    private void ensureContinuousPlayerSynced(GamePanel panel) {
        if (panel == null || panel.world == null) return;
        LogicalTile current = continuousPlayer.currentLogicalTile();
        boolean tileMismatch = current.x() != panel.playerX || current.y() != panel.playerY;
        if (!continuousMovementInitialized || tileMismatch) {
            continuousPlayer.snapToTileCenter(panel.playerX, panel.playerY);
            continuousPlayer.setRadius(0.28);
            applyMovementModeTuning(panel);
            continuousPlayer.setLookAngleRadians(camera.yawRadians());
            continuousMovementInitialized = true;
            lastContinuousMotionNanos = System.nanoTime();
        }
    }

    private void rasterizeWorld(GamePanel panel, int w, int h, long now) {
        World world = panel.world;
        firstPersonFloorTextureCache.clear();
        firstPersonWallTextureCache.clear();
        int horizon = h / 2 + (int)(camera.pitchRadians() * h * 0.55);
        int ceiling = 0xFF07080A;
        int floor = 0xFF11100D;
        for (int y = 0; y < h; y++) {
            int base = y < horizon ? ceiling : floor;
            int shade = y < horizon ? Math.max(0, 38 - (horizon - y) / 7) : Math.max(0, 28 - (y - horizon) / 10);
            int r = Math.min(255, ((base >> 16) & 255) + shade);
            int gr = Math.min(255, ((base >> 8) & 255) + shade);
            int b = Math.min(255, (base & 255) + shade / 2);
            int color = 0xFF000000 | (r << 16) | (gr << 8) | b;
            int row = y * w;
            for (int x = 0; x < w; x++) pixels[row + x] = color;
        }
        double fov = Math.toRadians(Math.max(60, Math.min(110, panel.options.doomModeFovDegrees)));
        rasterizeFloorAndCeiling(panel, world, w, h, horizon, fov);
        DoomFogSettings fogSettings = DoomFogSettings.fromOptions(panel.options);
        boolean radialFog = fogSettings.mode() == DoomFogMode.RADIAL_DISTANCE;
        panel.ensureSensoryModelCurrent("first-person render");
        prepareWallLightScratch(panel, world);
        double yaw = camera.yawRadians();
        double scale = 1.0 / Math.tan(fov / 2.0);
        double aspect = (double) w / Math.max(1.0, h);
        for (int sx = 0; sx < w; sx++) {
            double cameraX = (2.0 * sx / Math.max(1.0, w - 1.0) - 1.0) * aspect / scale;
            double rayAngle = yaw + Math.atan(cameraX);
            RaycastHit hit = raycaster.cast(world, camera.x(), camera.z(), rayAngle, 32.0, false);
            if (!hit.hit()) continue;
            double dist = Math.max(0.05, hit.distance() * Math.cos(rayAngle - yaw));
            int wallH = Math.min(h * 3, (int) Math.round(h / dist));
            int y0 = Math.max(0, horizon - wallH / 2);
            int y1 = Math.min(h - 1, horizon + wallH / 2);
            double wallU = hit.side() == 0
                    ? hit.impactZ() - Math.floor(hit.impactZ())
                    : hit.impactX() - Math.floor(hit.impactX());
            BufferedImage wallTexture = firstPersonTileTexture(
                    panel, world, hit.tileX(), hit.tileY(), hit.tile(), true);
            double light = lightDecay.brightness(camera.x(), camera.z(), hit.impactX(), hit.impactZ(), panel, now);
            int baseColor = tileBaseColor(hit.tile(), hit.kind(), hit.side(), hit.tileX(), hit.tileY());
            if (hasWallLightAt(hit.tileX(), hit.tileY())) baseColor = wallLightOverlayColor(baseColor, sx, y0, y1);
            float fogDistance = (float)(radialFog ? hit.distance() : dist);
            for (int y = y0; y <= y1; y++) {
                double wallV = (y - y0) / Math.max(1.0, y1 - y0 + 1.0);
                int color = sampleImageArgb(wallTexture, wallU, wallV, baseColor);
                color = applyBrightness(color, light);
                if (hit.side() == 1) color = darken(color, 0.78);
                color = fog.applyFogArgb(color, fogSettings, fogDistance);
                int stripe = (((y - y0) / 6) + sx / 9) & 1;
                pixels[y * w + sx] = stripe == 0 ? color : darken(color, 0.94);
            }
        }
    }

    private void rasterizeFloorAndCeiling(GamePanel panel, World world, int w, int h, int horizon, double fov) {
        if (panel == null || panel.images == null || world == null || pixels == null || pixels.length < w * h) return;
        double yaw = camera.yawRadians();
        double dirX = Math.cos(yaw);
        double dirY = Math.sin(yaw);
        double planeLen = Math.tan(fov / 2.0);
        double planeX = -dirY * planeLen;
        double planeY = dirX * planeLen;
        BufferedImage ceilingTex = panel.images.getTile('#');
        for (int y = 0; y < h; y++) {
            int p = y - horizon;
            if (Math.abs(p) < 2) continue;
            boolean floorSide = p > 0;
            double cameraHeight = floorSide ? 0.58 : 0.42;
            double rowDistance = (cameraHeight * h) / Math.max(1.0, Math.abs(p) * 2.0);
            if (rowDistance > 36.0) rowDistance = 36.0;
            double rayDirX0 = dirX - planeX;
            double rayDirY0 = dirY - planeY;
            double rayDirX1 = dirX + planeX;
            double rayDirY1 = dirY + planeY;
            double stepX = rowDistance * (rayDirX1 - rayDirX0) / Math.max(1, w);
            double stepY = rowDistance * (rayDirY1 - rayDirY0) / Math.max(1, w);
            double worldX = camera.x() + rowDistance * rayDirX0;
            double worldY = camera.z() + rowDistance * rayDirY0;
            int row = y * w;
            for (int x = 0; x < w; x++) {
                int tx = (int)Math.floor(worldX);
                int ty = (int)Math.floor(worldY);
                double u = worldX - Math.floor(worldX);
                double v = worldY - Math.floor(worldY);
                int color;
                if (world.inBounds(tx, ty)) {
                    if (floorSide) {
                        char tile = world.tiles[tx][ty];
                        BufferedImage tex = firstPersonTileTexture(panel, world, tx, ty, tile, false);
                        color = sampleImageArgb(tex, u, v, tile == '#' ? 0xFF2D2923 : 0xFF171613);
                    } else {
                        color = sampleImageArgb(ceilingTex, u, v, 0xFF08090B);
                    }
                } else color = 0xFF000000;
                double shade = floorSide ? Math.max(0.18, Math.min(1.0, 1.35 / Math.sqrt(Math.max(0.35, rowDistance))))
                                         : Math.max(0.10, Math.min(0.55, 0.85 / Math.sqrt(Math.max(0.45, rowDistance))));
                if (world.inBounds(tx, ty)) {
                    double localLight = panel.lightLevelAt(tx, ty) / 100.0;
                    shade = Math.max(0.04, Math.min(1.0, shade * (0.30 + localLight * 1.15)));
                    if (!panel.isVisible(tx, ty)) shade *= 0.34;
                }
                pixels[row + x] = darken(color | 0xFF000000, shade);
                worldX += stepX;
                worldY += stepY;
            }
        }
    }

    private int sampleImageArgb(BufferedImage img, double u, double v, int fallback) {
        if (img == null || img.getWidth() <= 0 || img.getHeight() <= 0) return fallback;
        int ix = Math.floorMod((int)Math.floor(u * img.getWidth()), img.getWidth());
        int iy = Math.floorMod((int)Math.floor(v * img.getHeight()), img.getHeight());
        int argb = img.getRGB(ix, iy);
        int a = (argb >>> 24) & 255;
        if (a < 24) return fallback;
        return 0xFF000000 | (argb & 0x00FFFFFF);
    }

    private BufferedImage firstPersonTileTexture(GamePanel panel, World world, int tileX, int tileY,
                                                 char fallbackGlyph, boolean wallSurface) {
        if (panel == null || panel.images == null || world == null || !world.inBounds(tileX, tileY)) return null;
        java.util.HashMap<Long, BufferedImage> cache = wallSurface
                ? firstPersonWallTextureCache : firstPersonFloorTextureCache;
        long key = (((long) tileX) << 32) ^ (tileY & 0xffffffffL);
        if (cache.containsKey(key)) return cache.get(key);
        BufferedImage image = panel.images.getCompiledTileImage(
                world, tileX, tileY, fallbackGlyph, !wallSurface);
        cache.put(key, image);
        return image;
    }

    private void prepareWallLightScratch(GamePanel panel, World world) {
        if (world == null) return;
        int need = Math.max(1, world.w * world.h);
        if (wallLightScratch.length < need || wallLightScratchW != world.w || wallLightScratchH != world.h) {
            wallLightScratch = new boolean[need];
            wallLightScratchW = world.w;
            wallLightScratchH = world.h;
        } else {
            java.util.Arrays.fill(wallLightScratch, 0, need, false);
        }
        for (int x = 0; x < world.w; x++) {
            for (int y = 0; y < world.h; y++) {
                if (world.walkable(x, y)) continue;
                if (panel != null && panel.lightLevelAt(x, y) >= 34) wallLightScratch[y * world.w + x] = true;
            }
        }
    }

    private boolean hasWallLightAt(int tx, int ty) {
        return tx >= 0 && ty >= 0 && tx < wallLightScratchW && ty < wallLightScratchH
                && ty * wallLightScratchW + tx < wallLightScratch.length
                && wallLightScratch[ty * wallLightScratchW + tx];
    }

    private int wallLightOverlayColor(int baseColor, int screenX, int y0, int y1) {
        int pulse = 28 + ((screenX + y0 + y1) & 15);
        int r = Math.min(255, ((baseColor >> 16) & 255) + pulse);
        int g = Math.min(255, ((baseColor >> 8) & 255) + pulse + 18);
        int b = Math.min(255, (baseColor & 255) + pulse / 2);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private int tileBaseColor(char tile, RaycastHitKind kind, int side, int tx, int ty) {
        if (kind == RaycastHitKind.DOOR) return switch (tile) {
            case 'X' -> 0xFF5D6570;
            case 'L' -> 0xFF5B4A2E;
            case 'V' -> 0xFF2E5B58;
            case '|' -> 0xFF6B573B;
            default -> 0xFF4F4A42;
        };
        if (tile == '#') return ((tx + ty) & 1) == 0 ? 0xFF514838 : 0xFF3D362C;
        if (tile == 'X') return 0xFF5A6670;
        return 0xFF353C34;
    }

    private int applyBrightness(int argb, double brightness) {
        double b = Math.max(0.0, Math.min(1.0, brightness));
        int r = (int)(((argb >> 16) & 255) * b);
        int g = (int)(((argb >> 8) & 255) * b);
        int bl = (int)((argb & 255) * b);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private int darken(int argb, double factor) {
        int r = (int)(((argb >> 16) & 255) * factor);
        int g = (int)(((argb >> 8) & 255) * factor);
        int b = (int)((argb & 255) * factor);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private void renderSpritesAndEffects(Graphics2D g, GamePanel panel, int w, int h, long now, double dt) {
        spriteScratch.clear();
        World world = panel.world;
        if (world != null) {
            for (NpcEntity n : world.npcs) {
                if (n == null || n.hp <= 0) continue;
                BufferedImage img = panel.images == null ? null : panel.images.getNpcPortraitFor(n);
                spriteScratch.add(new RenderableSprite(n.x + 0.5, 0.55, n.y + 0.5, n.symbol, n.name == null ? "entity" : n.name, 0xFFE2C46B, true, img));
            }
            for (MapObjectState o : world.mapObjects) {
                if (o == null) continue;
                if (o.glyph == '*' && world.inBounds(o.x, o.y) && !world.walkable(o.x, o.y)) continue;
                char glyph = o.glyph == 0 ? 'o' : o.glyph;
                BufferedImage img = ObjectSemanticAssetAuthority.imageForMapObject(o);
                spriteScratch.add(new RenderableSprite(o.x + 0.5, 0.38, o.y + 0.5, glyph, o.label == null ? "object" : o.label, 0xFF8FD0A6, false, img));
            }
        }
        spriteScratch.sort(Comparator.comparingDouble(s -> -dist2(s.x(), s.z())));
        billboardRenderer.renderSprites(g, camera, spriteScratch, Math.toRadians(panel.options.doomModeFovDegrees), w, h, DoomFogSettings.fromOptions(panel.options), fog);
        projectileRenderer.render(g, camera, Math.toRadians(panel.options.doomModeFovDegrees), w, h, now);
        particles.render(g, camera, Math.toRadians(panel.options.doomModeFovDegrees), w, h, now);
    }

    private double dist2(double x, double z) {
        double dx = x - camera.x(); double dz = z - camera.z(); return dx * dx + dz * dz;
    }

    private void renderGhostMovementProjection(Graphics2D g, GamePanel panel, int w, int h) {
        if (panel == null || panel.options == null || panel.options.doomModeEnabled) return;
        if (!panel.mouseMovePreviewActive || panel.mouseMovePreviewPath.isEmpty()) return;
        g.setStroke(new BasicStroke(3f));
        g.setColor(panel.mouseMovePreviewValid ? new Color(255, 230, 90, 170) : new Color(255, 60, 40, 150));
        Point prev = null;
        for (Point tile : panel.mouseMovePreviewPath) {
            ScreenPoint sp = project(tile.x + 0.5, 0.03, tile.y + 0.5, Math.toRadians(panel.options.doomModeFovDegrees), w, h);
            if (sp.visible()) {
                g.fillOval(sp.x() - 4, sp.y() - 4, 8, 8);
                if (prev != null) {
                    ScreenPoint pp = project(prev.x + 0.5, 0.03, prev.y + 0.5, Math.toRadians(panel.options.doomModeFovDegrees), w, h);
                    if (pp.visible()) g.drawLine(pp.x(), pp.y(), sp.x(), sp.y());
                }
            }
            prev = tile;
        }
        g.setStroke(new BasicStroke(1f));
    }

    private String formatDistance2(double value) {
        double clamped = Math.max(0.0, Math.min(999.0, value));
        long scaled = Math.round(clamped * 100.0);
        return (scaled / 100) + "." + ((scaled % 100) < 10 ? "0" : "") + (scaled % 100);
    }

    private void renderHud(Graphics2D g, GamePanel panel, int w, int h) {
        Point center = DoomRayTarget.reticleCenter(w, h);
        int cx = center.x, cy = center.y;
        DoomRayTarget targetSolution = acquireTarget(panel);
        renderTargetReticle(g, targetSolution, cx, cy);
        RaycastHit hit = targetSolution.hit();
        String target = hit.hit() ? hit.kind() + " " + hit.label() + " " + hit.tileX() + "," + hit.tileY() + " d=" + formatDistance2(hit.distance()) : "no ray target";
        LogicalTile tile = continuousPlayer.currentLogicalTile();
        lastStatus = statusLine(panel.options) + " / tile " + tile.label() + " / " + target + " / LWJGL: " + LwjglRenderBackendProbe.statusLine();
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        int boxW = Math.min(w - 32, Math.max(360, fm.stringWidth(lastStatus) + 20));
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRoundRect(16, 16, boxW, 28, 8, 8);
        g.setColor(new Color(232, 210, 132));
        g.drawString(lastStatus, 26, 35);
        renderTurnCountdown(g, panel, w);
    }

    private void renderTurnCountdown(Graphics2D g, GamePanel panel, int width) {
        String countdown = panel.passiveTurnCountdownLabel(System.currentTimeMillis())
                + " / MOVE " + panel.movementModeLabel(panel.selectedMovementModeIndex);
        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        int tw = g.getFontMetrics().stringWidth(countdown);
        int x = Math.max(16, width - tw - 28);
        g.setColor(new Color(0, 0, 0, 175));
        g.fillRoundRect(x - 8, 50, tw + 16, 28, 8, 8);
        g.setColor(panel.options != null && panel.options.passiveSinglePlayerTicking()
                ? new Color(245, 214, 118) : new Color(145, 154, 148));
        g.drawString(countdown, x, 69);
    }

    private DoomRayTarget acquireTarget(GamePanel panel) {
        RaycastHit hit = raycaster.cast(panel.world, camera, Math.toRadians(panel.options.doomModeFovDegrees), DoomRayTarget.MAX_RANGE, true);
        return DoomRayTarget.from(hit);
    }

    private void lookAtTarget(GamePanel panel, DoomRayTarget target) {
        if (!target.hasTarget()) { panel.logEvent(target.lookHint()); return; }
        panel.lookX = target.hit().tileX();
        panel.lookY = target.hit().tileY();
        panel.lookCursorActive = true;
        panel.examineSelectedLookTarget();
    }

    private void interactWithTarget(GamePanel panel, DoomRayTarget target) {
        if (!target.interactable()) { panel.logEvent(target.interactionHint()); return; }
        panel.lookX = target.hit().tileX();
        panel.lookY = target.hit().tileY();
        panel.interactCursorActive = true;
        panel.confirmInteraction();
    }

    private boolean aimWeaponAtTarget(GamePanel panel, DoomRayTarget target) {
        if (!target.weaponTarget()) return false;
        panel.combatX = target.hit().tileX();
        panel.combatY = target.hit().tileY();
        panel.lookX = panel.combatX;
        panel.lookY = panel.combatY;
        panel.lastTargetingReport = panel.targetingSolutionAt(panel.combatX, panel.combatY).summary;
        panel.logEvent("Weapon aim locked through the center reticle: " + target.label() + ".");
        return true;
    }

    private void renderTargetReticle(Graphics2D g, DoomRayTarget target, int cx, int cy) {
        Color color = target.reticleColor();
        g.setColor(new Color(0, 0, 0, 190));
        g.fillOval(cx - 3, cy - 3, 7, 7);
        g.setColor(color);
        g.fillOval(cx - 1, cy - 1, 3, 3);
        g.drawLine(cx - 15, cy, cx - 5, cy);
        g.drawLine(cx + 5, cy, cx + 15, cy);
        g.drawLine(cx, cy - 15, cx, cy - 5);
        g.drawLine(cx, cy + 5, cx, cy + 15);
        g.drawRect(cx - 20, cy - 20, 40, 40);
        g.setFont(new Font("Monospaced", Font.BOLD, 11));
        String label = target.reticleLabel();
        int tw = g.getFontMetrics().stringWidth(label);
        g.setColor(new Color(0, 0, 0, 165));
        g.fillRoundRect(cx - tw / 2 - 6, cy + 27, tw + 12, 18, 6, 6);
        g.setColor(color);
        g.drawString(label, cx - tw / 2, cy + 40);
    }

    ScreenPoint project(double wx, double wy, double wz, double fov, int w, int h) {
        double dx = wx - camera.x();
        double dy = wy - camera.y();
        double dz = wz - camera.z();
        double sin = Math.sin(-camera.yawRadians());
        double cos = Math.cos(-camera.yawRadians());
        double rx = dx * cos - dz * sin;
        double rz = dx * sin + dz * cos;
        double pitchSin = Math.sin(-camera.pitchRadians());
        double pitchCos = Math.cos(-camera.pitchRadians());
        double ry = dy * pitchCos - rz * pitchSin;
        double rz2 = dy * pitchSin + rz * pitchCos;
        if (rz2 <= 0.04) return new ScreenPoint(0, 0, false, rz2);
        double scale = 1.0 / Math.tan(fov / 2.0);
        int sx = (int)Math.round(w * 0.5 + (rx / rz2) * (w * 0.5) * scale);
        int sy = (int)Math.round(h * 0.5 - (ry / rz2) * (h * 0.5) * scale);
        return new ScreenPoint(sx, sy, sx >= -64 && sx <= w + 64 && sy >= -64 && sy <= h + 64, rz2);
    }
}

record Vec3(double x, double y, double z) {
    Vec3 add(Vec3 o) { return new Vec3(x + o.x, y + o.y, z + o.z); }
    Vec3 scale(double s) { return new Vec3(x * s, y * s, z * s); }
    double dot(Vec3 o) { return x * o.x + y * o.y + z * o.z; }
    double length() { return Math.sqrt(dot(this)); }
    Vec3 normalized() { double l = length(); return l <= 1.0e-9 ? new Vec3(0, 0, 0) : scale(1.0 / l); }
}

record ScreenPoint(int x, int y, boolean visible, double depth) {}
record RenderableSprite(double x, double y, double z, char glyph, String label, int color, boolean entity, BufferedImage image) {}
record InteractionRequestRecord(String sessionId, int tileX, int tileY, long clientTimestampMillis, double distance) {}
record MotionTuning(double maxSpeed, double acceleration, double damping) {}

record DoomRayTarget(RaycastHit hit, boolean interactable, boolean weaponTarget, String label,
                     String lookHint, String interactionHint, String weaponHint) {
    static final double MAX_RANGE = 8.0;
    static final double INTERACTION_RANGE = 1.65;

    static DoomRayTarget from(RaycastHit hit) {
        RaycastHit safe = hit == null ? RaycastHit.none() : hit;
        boolean has = safe.hit();
        boolean interact = has && safe.distance() <= INTERACTION_RANGE
                && (safe.kind() == RaycastHitKind.ENTITY || safe.kind() == RaycastHitKind.DOOR || safe.kind() == RaycastHitKind.OBJECT);
        boolean weapon = has && safe.kind() == RaycastHitKind.ENTITY;
        String label = has ? safe.kind().name().toLowerCase(Locale.ROOT) + " " + safe.label() : "clear sightline";
        String look = has ? "Look target: " + label + "." : "Look found no target in the center sightline.";
        String use = interact ? "Interact with " + label + "." : has ? "Target is not usable from this range." : "No interaction target in the center sightline.";
        String weaponHint = weapon ? "Aim at " + label + "." : "No valid weapon target in the center sightline.";
        return new DoomRayTarget(safe, interact, weapon, label, look, use, weaponHint);
    }

    boolean hasTarget() { return hit != null && hit.hit(); }
    String reticleLabel() {
        if (!hasTarget()) return "L LOOK / E USE / F AIM";
        String action = weaponTarget ? "F AIM" : interactable ? "E USE" : "L LOOK";
        return action + " / " + label;
    }
    Color reticleColor() {
        if (weaponTarget) return new Color(235, 82, 68, 235);
        if (interactable) return new Color(124, 230, 144, 235);
        if (hasTarget()) return new Color(116, 188, 235, 235);
        return new Color(245, 220, 110, 190);
    }
    static Point reticleCenter(int width, int height) {
        return new Point(Math.max(0, width) / 2, Math.max(0, height) / 2);
    }
}

enum RaycastHitKind { NONE, WALL, DOOR, ENTITY, OBJECT }

record RaycastHit(boolean hit, RaycastHitKind kind, int tileX, int tileY, char tile, double distance,
                  double impactX, double impactZ, double normalX, double normalZ, int side, String label) {
    static RaycastHit none() { return new RaycastHit(false, RaycastHitKind.NONE, -1, -1, '\0', 0, 0, 0, 0, 0, 0, "none"); }
}

final class FirstPersonCamera {
    private static final double CARDINAL_STEP = Math.PI / 2.0;
    private static final double YAW_DRIFT_RATE = Math.toRadians(20.0);
    private static final double PITCH_DRIFT_RATE = Math.toRadians(14.0);
    private double baseX = 0.5, baseY = 0.58, baseZ = 0.5;
    private double yaw = 0.0, pitch = 0.0;
    private double lookX = 1.0, lookY = 0.0, lookZ = 0.0;
    private double viewX = 0.5, viewY = 0.58, viewZ = 0.5, viewPitch = 0.0;

    void setPosition(double x, double y, double z) { this.baseX = x; this.baseY = y; this.baseZ = z; applyVisualOffsets(new CameraJuiceState(0,0,0)); }
    void applyVisualOffsets(CameraJuiceState s) {
        if (s == null) s = new CameraJuiceState(0,0,0);
        double flatLen = Math.max(0.0001, Math.hypot(lookX, lookZ));
        viewX = baseX - (lookX / flatLen) * s.backwardOffset();
        viewY = baseY + s.verticalOffset();
        viewZ = baseZ - (lookZ / flatLen) * s.backwardOffset();
        viewPitch = Math.max(Math.toRadians(-60), Math.min(Math.toRadians(60), pitch + s.pitchOffsetRadians()));
    }
    void addYawPitch(double dyaw, double dpitch) {
        yaw = normalizeAngle(yaw + dyaw);
        pitch = Math.max(Math.toRadians(-55), Math.min(Math.toRadians(55), pitch + dpitch));
        updateLookVector();
    }
    void driftTowardCardinal(double dtSeconds) {
        double dt = Math.max(0.0, Math.min(0.10, dtSeconds));
        if (dt <= 0.0) return;
        double targetYaw = Math.rint(yaw / CARDINAL_STEP) * CARDINAL_STEP;
        double yawDelta = normalizeAngle(targetYaw - yaw);
        yaw = normalizeAngle(yaw + clamp(yawDelta, -YAW_DRIFT_RATE * dt, YAW_DRIFT_RATE * dt));
        pitch += clamp(-pitch, -PITCH_DRIFT_RATE * dt, PITCH_DRIFT_RATE * dt);
        if (Math.abs(yawDelta) < Math.toRadians(0.08)) yaw = normalizeAngle(targetYaw);
        if (Math.abs(pitch) < Math.toRadians(0.08)) pitch = 0.0;
        updateLookVector();
    }
    double nearestCardinalYawRadians() {
        return normalizeAngle(Math.rint(yaw / CARDINAL_STEP) * CARDINAL_STEP);
    }
    private void updateLookVector() {
        double cp = Math.cos(pitch);
        lookX = Math.cos(yaw) * cp;
        lookY = Math.sin(pitch);
        lookZ = Math.sin(yaw) * cp;
        applyVisualOffsets(new CameraJuiceState(0,0,0));
    }
    private static double normalizeAngle(double angle) {
        double a = angle % (Math.PI * 2.0);
        if (a <= -Math.PI) a += Math.PI * 2.0;
        if (a > Math.PI) a -= Math.PI * 2.0;
        return a;
    }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    double x() { return viewX; } double y() { return viewY; } double z() { return viewZ; }
    double baseX() { return baseX; } double baseY() { return baseY; } double baseZ() { return baseZ; }
    double yawRadians() { return yaw; } double pitchRadians() { return viewPitch; }
    double lookX() { return lookX; } double lookY() { return lookY; } double lookZ() { return lookZ; }
}

final class FirstPersonMouseInput {
    private boolean centered = false;
    private int lastX, lastY;
    private final double sensitivity = 0.0035;
    private Robot robot;
    private boolean recentering = false;
    private boolean cursorHidden = false;
    private long lastLookInputNanos = System.nanoTime();

    void ensureLock(GamePanel panel) {
        if (panel == null || !panel.isShowing()) return;
        hideCursor(panel);
        if (robot == null) {
            try { robot = new Robot(); robot.setAutoDelay(0); }
            catch (Throwable t) { robot = null; return; }
        }
        if (!centered) centerMouse(panel);
    }

    void release(GamePanel panel) {
        centered = false;
        recentering = false;
        lastLookInputNanos = System.nanoTime();
        if (panel != null && cursorHidden) {
            panel.setCursor(Cursor.getDefaultCursor());
            cursorHidden = false;
        }
    }

    void handleMove(FirstPersonCamera camera, GamePanel panel, int x, int y) {
        Objects.requireNonNull(camera, "camera");
        int viewportW = Math.max(1, panel == null ? 1 : panel.getWidth());
        int viewportH = Math.max(1, panel == null ? 1 : panel.getHeight());
        ensureLock(panel);
        if (recentering) {
            recentering = false;
            lastX = x; lastY = y; centered = true;
            return;
        }
        if (!centered) { lastX = x; lastY = y; centered = true; return; }
        int dx = x - lastX;
        int dy = y - lastY;
        lastX = x;
        lastY = y;
        if (Math.abs(dx) > viewportW / 3 || Math.abs(dy) > viewportH / 3) { centerMouse(panel); return; }
        if (dx != 0 || dy != 0) {
            lastLookInputNanos = System.nanoTime();
            camera.addYawPitch(dx * sensitivity, -dy * sensitivity);
        }
        centerMouse(panel);
    }

    double lookIdleSeconds(long nowNanos) {
        return Math.max(0.0, (nowNanos - lastLookInputNanos) / 1_000_000_000.0);
    }

    private void centerMouse(GamePanel panel) {
        if (panel == null || robot == null || !panel.isShowing()) return;
        try {
            Point p = panel.getLocationOnScreen();
            int cx = Math.max(1, panel.getWidth()) / 2;
            int cy = Math.max(1, panel.getHeight()) / 2;
            recentering = true;
            lastX = cx; lastY = cy; centered = true;
            robot.mouseMove(p.x + cx, p.y + cy);
        } catch (Throwable ignored) { centered = false; }
    }

    private void hideCursor(GamePanel panel) {
        if (panel == null || cursorHidden) return;
        try {
            BufferedImage blank = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Cursor c = Toolkit.getDefaultToolkit().createCustomCursor(blank, new Point(0, 0), "mechanist-doom-lock");
            panel.setCursor(c);
            cursorHidden = true;
        } catch (Throwable ignored) {}
    }
}

final class ViewportRaycaster {
    RaycastHit cast(World world, FirstPersonCamera camera, double fov, double maxDistance, boolean includeEntities) {
        if (world == null || camera == null) return RaycastHit.none();
        if (includeEntities) {
            RaycastHit entity = findEntityAlongRay(world, camera, maxDistance);
            RaycastHit object = findObjectAlongRay(world, camera, maxDistance);
            RaycastHit actorOrObject = nearer(entity, object);
            RaycastHit tile = cast(world, camera.x(), camera.z(), Math.atan2(camera.lookZ(), camera.lookX()), maxDistance, true);
            if (actorOrObject.hit() && (!tile.hit() || actorOrObject.distance() <= tile.distance() + 0.15)) return actorOrObject;
            return tile;
        }
        return cast(world, camera.x(), camera.z(), Math.atan2(camera.lookZ(), camera.lookX()), maxDistance, true);
    }

    RaycastHit cast(World world, double startX, double startZ, double rayAngle, double maxDistance, boolean stopAtDoors) {
        if (world == null) return RaycastHit.none();
        double rayDirX = Math.cos(rayAngle);
        double rayDirZ = Math.sin(rayAngle);
        int mapX = (int)Math.floor(startX);
        int mapY = (int)Math.floor(startZ);
        double deltaDistX = Math.abs(rayDirX) < 1.0e-9 ? 1.0e30 : Math.abs(1.0 / rayDirX);
        double deltaDistY = Math.abs(rayDirZ) < 1.0e-9 ? 1.0e30 : Math.abs(1.0 / rayDirZ);
        int stepX; int stepY;
        double sideDistX; double sideDistY;
        if (rayDirX < 0) { stepX = -1; sideDistX = (startX - mapX) * deltaDistX; }
        else { stepX = 1; sideDistX = (mapX + 1.0 - startX) * deltaDistX; }
        if (rayDirZ < 0) { stepY = -1; sideDistY = (startZ - mapY) * deltaDistY; }
        else { stepY = 1; sideDistY = (mapY + 1.0 - startZ) * deltaDistY; }
        int side = 0;
        double traveled = 0.0;
        while (traveled <= maxDistance) {
            if (sideDistX < sideDistY) { traveled = sideDistX; sideDistX += deltaDistX; mapX += stepX; side = 0; }
            else { traveled = sideDistY; sideDistY += deltaDistY; mapY += stepY; side = 1; }
            if (!world.inBounds(mapX, mapY)) return RaycastHit.none();
            char tile = world.tiles[mapX][mapY];
            boolean door = isDoor(tile);
            boolean wall = tile == '#';
            if (door && tile == '/') continue;
            if (door && stopAtDoors) {
                DoorPlaneHit dph = intersectDoorPlane(startX, startZ, rayDirX, rayDirZ, mapX, mapY, side, maxDistance);
                if (dph.hit()) return new RaycastHit(true, RaycastHitKind.DOOR, mapX, mapY, tile, dph.distance(), dph.x(), dph.z(), dph.nx(), dph.nz(), side, "door");
            }
            if (wall || (door && !stopAtDoors)) {
                double impactX = startX + rayDirX * traveled;
                double impactZ = startZ + rayDirZ * traveled;
                double nx = side == 0 ? -stepX : 0;
                double nz = side == 1 ? -stepY : 0;
                return new RaycastHit(true, wall ? RaycastHitKind.WALL : RaycastHitKind.DOOR, mapX, mapY, tile, traveled, impactX, impactZ, nx, nz, side, wall ? "wall" : "door");
            }
        }
        return RaycastHit.none();
    }

    private DoorPlaneHit intersectDoorPlane(double sx, double sz, double dx, double dz, int tx, int ty, int side, double maxDistance) {
        double planeX0 = tx + 0.5;
        double planeZ0 = ty + 0.5;
        double denom;
        double t;
        double nx;
        double nz;
        if (side == 0) {
            denom = dx;
            if (Math.abs(denom) < 1.0e-9) return DoorPlaneHit.none();
            t = (planeX0 - sx) / denom;
            nx = dx > 0 ? -1 : 1; nz = 0;
            double z = sz + dz * t;
            if (z < ty || z > ty + 1.0) return DoorPlaneHit.none();
            return t > 0 && t <= maxDistance ? new DoorPlaneHit(true, t, planeX0, z, nx, nz) : DoorPlaneHit.none();
        } else {
            denom = dz;
            if (Math.abs(denom) < 1.0e-9) return DoorPlaneHit.none();
            t = (planeZ0 - sz) / denom;
            nx = 0; nz = dz > 0 ? -1 : 1;
            double x = sx + dx * t;
            if (x < tx || x > tx + 1.0) return DoorPlaneHit.none();
            return t > 0 && t <= maxDistance ? new DoorPlaneHit(true, t, x, planeZ0, nx, nz) : DoorPlaneHit.none();
        }
    }

    private RaycastHit findEntityAlongRay(World world, FirstPersonCamera camera, double maxDistance) {
        double lx = camera.lookX(); double lz = camera.lookZ();
        double best = maxDistance + 1.0;
        RaycastHit out = RaycastHit.none();
        for (NpcEntity n : world.npcs) {
            if (n == null || n.hp <= 0) continue;
            double cx = n.x + 0.5, cz = n.y + 0.5;
            double vx = cx - camera.x(), vz = cz - camera.z();
            double along = vx * lx + vz * lz;
            if (along < 0.05 || along > maxDistance) continue;
            double px = camera.x() + lx * along;
            double pz = camera.z() + lz * along;
            double offX = cx - px;
            double offZ = cz - pz;
            if (offX * offX + offZ * offZ <= 0.1156 && along < best) {
                best = along;
                out = new RaycastHit(true, RaycastHitKind.ENTITY, n.x, n.y, world.inBounds(n.x,n.y) ? world.tiles[n.x][n.y] : '?', along, cx, cz, -lx, -lz, 0, n.name == null ? "entity" : n.name);
            }
        }
        return out;
    }

    private RaycastHit findObjectAlongRay(World world, FirstPersonCamera camera, double maxDistance) {
        if (world.mapObjects == null) return RaycastHit.none();
        double lx = camera.lookX(); double lz = camera.lookZ();
        double best = maxDistance + 1.0;
        RaycastHit out = RaycastHit.none();
        for (MapObjectState object : world.mapObjects) {
            if (object == null) continue;
            double cx = object.x + 0.5, cz = object.y + 0.5;
            double vx = cx - camera.x(), vz = cz - camera.z();
            double along = vx * lx + vz * lz;
            if (along < 0.05 || along > maxDistance) continue;
            double px = camera.x() + lx * along;
            double pz = camera.z() + lz * along;
            double offX = cx - px;
            double offZ = cz - pz;
            if (offX * offX + offZ * offZ <= 0.1156 && along < best) {
                best = along;
                String label = object.label == null || object.label.isBlank() ? "object" : object.label;
                out = new RaycastHit(true, RaycastHitKind.OBJECT, object.x, object.y,
                        world.inBounds(object.x, object.y) ? world.tiles[object.x][object.y] : '?',
                        along, cx, cz, -lx, -lz, 0, label);
            }
        }
        return out;
    }

    private RaycastHit nearer(RaycastHit a, RaycastHit b) {
        if (a == null || !a.hit()) return b == null ? RaycastHit.none() : b;
        if (b == null || !b.hit()) return a;
        return a.distance() <= b.distance() ? a : b;
    }

    private boolean isDoor(char c) { return c == '/' || c == '|' || c == 'L' || c == 'X' || c == 'V' || c == 'Z'; }
    record DoorPlaneHit(boolean hit, double distance, double x, double z, double nx, double nz) { static DoorPlaneHit none(){ return new DoorPlaneHit(false,0,0,0,0,0); } }
}

final class BillboardSpriteRenderer {
    void renderSprites(Graphics2D g, FirstPersonCamera camera, List<RenderableSprite> sprites, double fov, int w, int h, DoomFogSettings fogSettings, DoomDepthFog fog) {
        if (sprites == null || sprites.isEmpty()) return;
        double scale = 1.0 / Math.tan(fov / 2.0);
        Font font = new Font("Monospaced", Font.BOLD, 13);
        g.setFont(font);
        for (RenderableSprite s : sprites) {
            ScreenPoint p = project(camera, s.x(), s.y(), s.z(), fov, w, h);
            if (!p.visible()) continue;
            int size = Math.max(10, Math.min(72, (int)Math.round((h / Math.max(0.2, p.depth())) * 0.30 * scale)));
            int alpha = Math.max(45, Math.min(230, (int)(230.0 / Math.max(1.0, p.depth() * 0.45))));
            int foggedColor;
            if (fog == null) foggedColor = s.color();
            else if (fogSettings != null && fogSettings.mode() == DoomFogMode.RADIAL_DISTANCE) {
                double dx = s.x() - camera.x();
                double dz = s.z() - camera.z();
                foggedColor = fog.applyFogArgbDistanceSquared(s.color(), fogSettings, (float)(dx * dx + dz * dz));
            } else {
                foggedColor = fog.applyFogArgb(s.color(), fogSettings, (float)p.depth());
            }
            Color c = new Color((foggedColor >> 16) & 255, (foggedColor >> 8) & 255, foggedColor & 255, alpha);
            g.setColor(new Color(0, 0, 0, Math.min(180, alpha)));
            g.fillOval(p.x() - size / 2, p.y() + size / 3, size, size / 5);
            BufferedImage img = s.image();
            if (img != null) {
                Composite oldComposite = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255f));
                int drawH = Math.max(size, (int)Math.round(size * (s.entity() ? 1.35 : 1.0)));
                int drawW = Math.max(size, (int)Math.round(drawH * (img.getWidth() / (double)Math.max(1, img.getHeight()))));
                g.drawImage(img, p.x() - drawW / 2, p.y() - drawH, drawW, drawH, null);
                g.setComposite(oldComposite);
            } else {
                g.setColor(c);
                g.fillRoundRect(p.x() - size / 2, p.y() - size, size, size, 8, 8);
                g.setColor(new Color(255, 245, 190, alpha));
                String label = String.valueOf(s.glyph());
                FontMetrics fm = g.getFontMetrics();
                g.drawString(label, p.x() - fm.stringWidth(label) / 2, p.y() - size / 2 + fm.getAscent() / 2);
            }
        }
    }

    private ScreenPoint project(FirstPersonCamera camera, double wx, double wy, double wz, double fov, int w, int h) {
        double dx = wx - camera.x(), dy = wy - camera.y(), dz = wz - camera.z();
        double sin = Math.sin(-camera.yawRadians()), cos = Math.cos(-camera.yawRadians());
        double rx = dx * cos - dz * sin;
        double rz = dx * sin + dz * cos;
        double ps = Math.sin(-camera.pitchRadians()), pc = Math.cos(-camera.pitchRadians());
        double ry = dy * pc - rz * ps;
        double rz2 = dy * ps + rz * pc;
        if (rz2 <= 0.05) return new ScreenPoint(0,0,false,rz2);
        double scale = 1.0 / Math.tan(fov / 2.0);
        int sx = (int)Math.round(w * 0.5 + (rx / rz2) * w * 0.5 * scale);
        int sy = (int)Math.round(h * 0.5 - (ry / rz2) * h * 0.5 * scale);
        return new ScreenPoint(sx, sy, sx > -80 && sx < w + 80 && sy > -120 && sy < h + 80, rz2);
    }
}

enum DoomFogMode {
    LINEAR_DEPTH, RADIAL_DISTANCE;

    static DoomFogMode fromIndex(int index) {
        return switch (index) {
            case 1 -> RADIAL_DISTANCE;
            default -> LINEAR_DEPTH;
        };
    }
}

record DoomFogSettings(int fogColorArgb, float startDistance, float maxVisibilityDistance, DoomFogMode mode) {
    DoomFogSettings {
        fogColorArgb = 0xFF090A0D | (fogColorArgb & 0x00FFFFFF);
        startDistance = Math.max(0.0f, startDistance);
        maxVisibilityDistance = Math.max(startDistance + 0.25f, maxVisibilityDistance);
        mode = mode == null ? DoomFogMode.LINEAR_DEPTH : mode;
    }

    static DoomFogSettings fromOptions(GameOptions options) {
        int modeIndex = options == null ? 0 : options.doomFogModeIndex;
        return new DoomFogSettings(0xFF090A0D, 2.0f, 17.5f, DoomFogMode.fromIndex(modeIndex));
    }
}

final class DoomDepthFog {
    private static final int TABLE_SIZE = 2048;
    private final float[] linearFogTable = new float[TABLE_SIZE];
    private DoomFogSettings lastSettings = new DoomFogSettings(0xFF090A0D, 2.0f, 17.5f, DoomFogMode.LINEAR_DEPTH);

    DoomDepthFog() { rebuildTable(lastSettings); }

    Color applyFog(Color originalColor, float distance, boolean useRadial) {
        if (originalColor == null) originalColor = Color.BLACK;
        DoomFogSettings settings = new DoomFogSettings(0xFF090A0D, 2.0f, 17.5f, useRadial ? DoomFogMode.RADIAL_DISTANCE : DoomFogMode.LINEAR_DEPTH);
        int argb = (originalColor.getAlpha() << 24) | (originalColor.getRed() << 16) | (originalColor.getGreen() << 8) | originalColor.getBlue();
        int out = applyFogArgb(argb, settings, distance);
        return new Color((out >> 16) & 255, (out >> 8) & 255, out & 255, (out >>> 24) & 255);
    }

    int applyFogArgb(int argb, DoomFogSettings settings, float distance) {
        DoomFogSettings safe = settings == null ? lastSettings : settings;
        ensureTable(safe);
        float fog = fogFactor(distance, safe);
        int a = (argb >>> 24) & 255;
        int fr = (safe.fogColorArgb() >> 16) & 255;
        int fg = (safe.fogColorArgb() >> 8) & 255;
        int fb = safe.fogColorArgb() & 255;
        int r = (argb >> 16) & 255;
        int g = (argb >> 8) & 255;
        int b = argb & 255;
        int rr = r + Math.round((fr - r) * fog);
        int gg = g + Math.round((fg - g) * fog);
        int bb = b + Math.round((fb - b) * fog);
        return (a << 24) | (VisualJuiceFramework.clamp255(rr) << 16) | (VisualJuiceFramework.clamp255(gg) << 8) | VisualJuiceFramework.clamp255(bb);
    }

    int applyFogArgbDistanceSquared(int argb, DoomFogSettings settings, float distanceSquared) {
        DoomFogSettings safe = settings == null ? lastSettings : settings;
        ensureTable(safe);
        float start2 = safe.startDistance() * safe.startDistance();
        float max2 = safe.maxVisibilityDistance() * safe.maxVisibilityDistance();
        float d2 = Math.max(0.0f, distanceSquared);
        float fog;
        if (d2 <= start2) fog = 0.0f;
        else if (d2 >= max2) fog = 1.0f;
        else {
            int idx = Math.max(0, Math.min(TABLE_SIZE - 1, Math.round((d2 - start2) * (TABLE_SIZE - 1) / Math.max(0.001f, max2 - start2))));
            fog = linearFogTable[idx];
        }
        int a = (argb >>> 24) & 255;
        int fr = (safe.fogColorArgb() >> 16) & 255;
        int fg = (safe.fogColorArgb() >> 8) & 255;
        int fb = safe.fogColorArgb() & 255;
        int r = (argb >> 16) & 255;
        int g = (argb >> 8) & 255;
        int b = argb & 255;
        int rr = r + Math.round((fr - r) * fog);
        int gg = g + Math.round((fg - g) * fog);
        int bb = b + Math.round((fb - b) * fog);
        return (a << 24) | (VisualJuiceFramework.clamp255(rr) << 16) | (VisualJuiceFramework.clamp255(gg) << 8) | VisualJuiceFramework.clamp255(bb);
    }

    private float fogFactor(float distance, DoomFogSettings settings) {
        float d = Math.max(0.0f, distance);
        float start = settings.startDistance();
        float max = settings.maxVisibilityDistance();
        if (d <= start) return 0.0f;
        if (d >= max) return 1.0f;
        int idx = Math.max(0, Math.min(TABLE_SIZE - 1, Math.round((d - start) * (TABLE_SIZE - 1) / Math.max(0.001f, max - start))));
        return linearFogTable[idx];
    }

    private void ensureTable(DoomFogSettings settings) {
        if (settings.fogColorArgb() == lastSettings.fogColorArgb()
                && Math.abs(settings.startDistance() - lastSettings.startDistance()) < 0.0001f
                && Math.abs(settings.maxVisibilityDistance() - lastSettings.maxVisibilityDistance()) < 0.0001f
                && settings.mode() == lastSettings.mode()) return;
        rebuildTable(settings);
    }

    private void rebuildTable(DoomFogSettings settings) {
        lastSettings = settings;
        for (int i = 0; i < TABLE_SIZE; i++) {
            float t = i / (float)(TABLE_SIZE - 1);
            linearFogTable[i] = Math.max(0.0f, Math.min(1.0f, t));
        }
    }
}

final class LightDecayEngine {
    private final double linearFactor = 0.10;
    private final double quadraticFactor = 0.055;

    double brightness(double cameraX, double cameraZ, double surfaceX, double surfaceZ, GamePanel panel, long now) {
        if (panel != null && panel.world != null) {
            int tx = (int)Math.floor(surfaceX);
            int ty = (int)Math.floor(surfaceZ);
            panel.ensureSensoryModelCurrent("first-person light sample");
            double field = panel.lightLevelAt(tx, ty) / 100.0;
            double sight = panel.isVisible(tx, ty) ? 1.0 : 0.34;
            double cameraFalloff = 1.0 / (1.0 + linearFactor * Math.max(0.0, (surfaceX - cameraX) * (surfaceX - cameraX) + (surfaceZ - cameraZ) * (surfaceZ - cameraZ)) * 0.35);
            return Math.max(0.08, Math.min(1.0, (0.10 + field * 1.05) * sight + cameraFalloff * 0.06));
        }
        double dx = surfaceX - cameraX;
        double dz = surfaceZ - cameraZ;
        double d2 = dx * dx + dz * dz;
        double b = 1.0 / (1.0 + linearFactor * d2 * 0.35 + quadraticFactor * d2);
        return Math.max(0.12, Math.min(1.0, b));
    }
}

final class ThreeDimensionalProjectileRenderer {
    private double rollRadians = 0.0;
    void update(long now) { rollRadians = (now % 1200L) / 1200.0 * Math.PI * 2.0; }
    void render(Graphics2D g, FirstPersonCamera camera, double fov, int w, int h, long now) {
        // Lightweight cross-quad sightline tracer. It gives the software backend visible weapon energy
        // without requiring a mesh system: two crossed quads rotate around the forward axis.
        double lx = camera.lookX(), lz = camera.lookZ();
        double sideX = -lz, sideZ = lx;
        double c = Math.cos(rollRadians), s = Math.sin(rollRadians);
        double centerX = camera.x() + lx * 1.15;
        double centerZ = camera.z() + lz * 1.15;
        double centerY = camera.y() - 0.05;
        drawQuad(g, camera, fov, w, h, centerX, centerY, centerZ, sideX * c, 0.18 * s, sideZ * c, new Color(95, 255, 220, 115));
        drawQuad(g, camera, fov, w, h, centerX, centerY, centerZ, sideX * -s, 0.18 * c, sideZ * -s, new Color(120, 210, 255, 90));
    }
    private void drawQuad(Graphics2D g, FirstPersonCamera camera, double fov, int w, int h, double cx, double cy, double cz, double ax, double ay, double az, Color color) {
        ScreenPoint p1 = project(camera, cx - ax, cy - ay, cz - az, fov, w, h);
        ScreenPoint p2 = project(camera, cx + ax, cy + ay, cz + az, fov, w, h);
        if (!p1.visible() || !p2.visible()) return;
        g.setColor(color);
        g.setStroke(new BasicStroke(3f));
        g.drawLine(p1.x(), p1.y(), p2.x(), p2.y());
        g.setStroke(new BasicStroke(1f));
    }
    private ScreenPoint project(FirstPersonCamera camera, double wx, double wy, double wz, double fov, int w, int h) {
        double dx = wx - camera.x(), dy = wy - camera.y(), dz = wz - camera.z();
        double sin = Math.sin(-camera.yawRadians()), cos = Math.cos(-camera.yawRadians());
        double rx = dx * cos - dz * sin;
        double rz = dx * sin + dz * cos;
        if (rz <= 0.04) return new ScreenPoint(0,0,false,rz);
        double scale = 1.0 / Math.tan(fov / 2.0);
        int sx = (int)Math.round(w * 0.5 + (rx / rz) * w * 0.5 * scale);
        int sy = (int)Math.round(h * 0.5 - (dy / rz) * h * 0.5 * scale);
        return new ScreenPoint(sx, sy, sx > -32 && sx < w + 32 && sy > -32 && sy < h + 32, rz);
    }
}

final class ParticleSplashSystem {
    private final ArrayDeque<Particle> particles = new ArrayDeque<>(160);
    private final Random random = new Random(42L);

    void spawnDoorImpact(double x, double z, double nx, double nz, long now) {
        for (int i = 0; i < 18; i++) {
            double spread = (random.nextDouble() - 0.5) * 0.7;
            double vx = nx * (0.8 + random.nextDouble() * 1.2) + -nz * spread;
            double vz = nz * (0.8 + random.nextDouble() * 1.2) + nx * spread;
            particles.addLast(new Particle(x, 0.52, z, vx, 0.20 + random.nextDouble() * 0.7, vz, now, 500L, 0xFFFFC45A));
        }
        while (particles.size() > 160) particles.removeFirst();
    }

    void update(long now) {
        while (!particles.isEmpty() && now - particles.peekFirst().bornMillis() > particles.peekFirst().lifeMillis()) particles.removeFirst();
    }

    void render(Graphics2D g, FirstPersonCamera camera, double fov, int w, int h, long now) {
        for (Particle p : particles) {
            double age = Math.max(0, now - p.bornMillis()) / 1000.0;
            double life = Math.max(1.0, p.lifeMillis());
            double alpha = 1.0 - Math.max(0.0, now - p.bornMillis()) / life;
            if (alpha <= 0) continue;
            ScreenPoint sp = project(camera, p.x() + p.vx() * age, p.y() + p.vy() * age - age * age * 0.7, p.z() + p.vz() * age, fov, w, h);
            if (!sp.visible()) continue;
            int a = Math.max(0, Math.min(220, (int)(alpha * 220)));
            g.setColor(new Color(255, 198, 90, a));
            int s = Math.max(2, (int)Math.round(8.0 / Math.max(0.6, sp.depth())));
            g.fillOval(sp.x() - s, sp.y() - s, s * 2, s * 2);
        }
    }

    private ScreenPoint project(FirstPersonCamera camera, double wx, double wy, double wz, double fov, int w, int h) {
        double dx = wx - camera.x(), dy = wy - camera.y(), dz = wz - camera.z();
        double sin = Math.sin(-camera.yawRadians()), cos = Math.cos(-camera.yawRadians());
        double rx = dx * cos - dz * sin;
        double rz = dx * sin + dz * cos;
        if (rz <= 0.05) return new ScreenPoint(0,0,false,rz);
        double scale = 1.0 / Math.tan(fov / 2.0);
        int sx = (int)Math.round(w * 0.5 + (rx / rz) * w * 0.5 * scale);
        int sy = (int)Math.round(h * 0.5 - (dy / rz) * h * 0.5 * scale);
        return new ScreenPoint(sx, sy, sx > -40 && sx < w + 40 && sy > -40 && sy < h + 40, rz);
    }

    record Particle(double x, double y, double z, double vx, double vy, double vz, long bornMillis, long lifeMillis, int color) {}
}

final class LwjglRenderBackendProbe {
    private static final String[] REQUIRED_CLASSES = {
            "org.lwjgl.Version",
            "org.lwjgl.glfw.GLFW",
            "org.lwjgl.opengl.GL"
    };
    private static volatile ProbeResult cachedResult;

    private LwjglRenderBackendProbe() {}

    static boolean available() { return result().available(); }

    static String statusLine() { return result().statusLine(); }

    static ProbeResult result() {
        ProbeResult cached = cachedResult;
        if (cached != null) return cached;
        StringBuilder missing = new StringBuilder();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        for (String className : REQUIRED_CLASSES) {
            try {
                Class.forName(className, false, loader);
            } catch (ClassNotFoundException ex) {
                if (missing.length() > 0) missing.append(", ");
                missing.append(className);
            } catch (LinkageError err) {
                if (missing.length() > 0) missing.append(", ");
                missing.append(className).append(" linkage ").append(err.getClass().getSimpleName());
            }
        }
        String version = "unknown";
        if (missing.length() == 0) {
            try {
                Class<?> versionClass = Class.forName("org.lwjgl.Version", false, loader);
                Object value = versionClass.getMethod("getVersion").invoke(null);
                if (value != null) version = String.valueOf(value);
            } catch (Throwable ignored) {
                version = "present";
            }
        }
        cachedResult = new ProbeResult(missing.length() == 0, version, missing.toString());
        return cachedResult;
    }

    record ProbeResult(boolean available, String version, String missing) {
        String statusLine() {
            return available ? "present " + version : "missing " + missing;
        }
    }
}
