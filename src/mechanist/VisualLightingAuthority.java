package mechanist;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Render-only lighting authority. Gameplay lighting remains owned by the
 * turn-stable lightLevelAt cache; this class only produces deterministic
 * per-frame visual flicker and low-resolution bloom for the Swing renderer.
 */
final class VisualLightingAuthority {
    static final String VERSION = "0.9.10jh";

    enum FlickerKind { STEADY, TORCH, NEON }

    static final class LightSample {
        final int gridX, gridY;
        final Color color;
        final float baseIntensity;
        final float radius;
        final FlickerKind flicker;
        final int seed;
        LightSample(int gridX, int gridY, Color color, float baseIntensity, float radius, FlickerKind flicker, int seed) {
            this.gridX = gridX;
            this.gridY = gridY;
            this.color = color == null ? new Color(230, 190, 110) : color;
            this.baseIntensity = Math.max(0.0f, baseIntensity);
            this.radius = Math.max(1.0f, radius);
            this.flicker = flicker == null ? FlickerKind.STEADY : flicker;
            this.seed = seed;
        }
    }

    private int gridWidth = -1;
    private int gridHeight = -1;
    private BufferedImage lightMapImage;
    private BufferedImage bloomImage;
    private int[] lightMapPixels;
    private int[] bloomPixels;
    private int[] blurScratch;
    private long renderedFrames;
    private String lastSummary = "visual lighting not rendered yet";

    void render(GamePanel game, Graphics2D g, int camX, int camY, int cols, int rows, int ox, int oy, int cellW, int cellH) {
        if (game == null || g == null || game.world == null || cols <= 0 || rows <= 0 || cellW <= 0 || cellH <= 0) return;
        int mode = game.options == null ? 0 : Math.max(0, Math.min(GameOptions.LIGHTING_FX_LABELS.length - 1, game.options.lightingFxIndex));
        if (mode <= 0) return;
        ensureBuffers(cols, rows);
        List<LightSample> lights = collectLights(game, camX, camY, cols, rows, mode >= 2);
        compute(game, lights, mode, camX, camY);

        Composite oldComposite = g.getComposite();
        Object oldInterpolation = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        Shape oldClip = g.getClip();
        Graphics2D gg = (Graphics2D) g.create();
        try {
            gg.setClip(new Rectangle(ox, oy, Math.max(1, cols * cellW), Math.max(1, rows * cellH)));
            gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.78f));
            gg.drawImage(lightMapImage, ox, oy, Math.max(1, cols * cellW), Math.max(1, rows * cellH), null);
            if (mode >= 2) {
                gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.48f));
                gg.drawImage(bloomImage, ox, oy, Math.max(1, cols * cellW), Math.max(1, rows * cellH), null);
            }
        } finally {
            gg.dispose();
            g.setComposite(oldComposite);
            g.setClip(oldClip);
            if (oldInterpolation != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
        }
        renderedFrames++;
        if ((renderedFrames & 63L) == 0L) {
            lastSummary = "visualLighting version=" + VERSION + " mode=" + GameOptions.LIGHTING_FX_LABELS[mode]
                    + " grid=" + cols + "x" + rows + " samples=" + lights.size()
                    + " frame=" + renderedFrames + " gameplayLightStable=true";
        }
    }

    private void ensureBuffers(int w, int h) {
        if (w == gridWidth && h == gridHeight && lightMapImage != null && bloomImage != null) return;
        gridWidth = Math.max(1, w);
        gridHeight = Math.max(1, h);
        lightMapImage = new BufferedImage(gridWidth, gridHeight, BufferedImage.TYPE_INT_ARGB);
        bloomImage = new BufferedImage(gridWidth, gridHeight, BufferedImage.TYPE_INT_ARGB);
        lightMapPixels = ((DataBufferInt) lightMapImage.getRaster().getDataBuffer()).getData();
        bloomPixels = ((DataBufferInt) bloomImage.getRaster().getDataBuffer()).getData();
        blurScratch = new int[gridWidth * gridHeight];
    }

    private List<LightSample> collectLights(GamePanel game, int camX, int camY, int cols, int rows, boolean allowFlicker) {
        ArrayList<LightSample> out = new ArrayList<>();
        int r = game.activePortableLightRadius();
        if (r > 0) out.add(new LightSample(game.playerX - camX, game.playerY - camY, new Color(255,230,150), 1.10f, r, FlickerKind.STEADY, stableSeed("active-portable", game.playerX, game.playerY)));
        for (PortableLightInstance l : game.portableLights) {
            if (l == null || l.expiresTurn <= game.turn || !game.sameWorldLocation(l.worldKey)) continue;
            if (!nearViewport(l.x, l.y, camX, camY, cols, rows, Math.max(1, l.radius))) continue;
            PortableLightProfile p = PortableLightProfile.profile(l.itemName);
            int rr = p == null ? l.radius : p.radius;
            out.add(new LightSample(l.x - camX, l.y - camY, colorForPortable(l.itemName), intensity01(game.portableLightIntensity(l.itemName)), rr,
                    allowFlicker && itemLooksFlickery(l.itemName) ? FlickerKind.TORCH : FlickerKind.STEADY, stableSeed(l.itemName, l.x, l.y)));
        }
        if (game.world.lightSources != null) {
            for (ZoneLightSourceRecord z : game.world.lightSources) {
                if (z == null || !z.on || !z.powered) continue;
                if (!nearViewport(z.x, z.y, camX, camY, cols, rows, Math.max(1, z.radius))) continue;
                FlickerKind kind = !allowFlicker || !z.flicker ? FlickerKind.STEADY : flickerKindFor(z);
                out.add(new LightSample(z.x - camX, z.y - camY, z.color(), intensity01(z.intensity), Math.max(1, z.radius), kind, stableSeed(z.id, z.x, z.y) + z.phase * 37));
            }
        }
        for (WorldLightEmitterAuthority.Emitter emitter : WorldLightEmitterAuthority.viewportEmitters(game, camX, camY, cols, rows)) {
            FlickerKind kind = emitter.flicker && allowFlicker ? FlickerKind.TORCH : FlickerKind.STEADY;
            out.add(new LightSample(emitter.x - camX, emitter.y - camY, emitter.color, intensity01(emitter.intensity), emitter.radius, kind, stableSeed(emitter.id, emitter.x, emitter.y)));
        }
        return out;
    }

    private void compute(GamePanel game, List<LightSample> lights, int mode, int camX, int camY) {
        int ambient = Math.max(0, Math.min(100, game.ambientLightLevelForWorld()));
        int ambientAlpha = Math.max(72, Math.min(218, 230 - ambient * 2));
        int ambientR = Math.max(3, ambient / 5);
        int ambientG = Math.max(3, ambient / 5);
        int ambientB = Math.max(6, ambient / 3);
        int ambientPixel = (ambientAlpha << 24) | (ambientR << 16) | (ambientG << 8) | ambientB;
        for (int i = 0; i < lightMapPixels.length; i++) {
            lightMapPixels[i] = ambientPixel;
            bloomPixels[i] = 0x00000000;
        }
        long now = System.nanoTime();
        for (LightSample light : lights) {
            float dynamic = light.baseIntensity * 1.16f * visualModifier(light, now, mode >= 2 && !game.options.reducedMotion);
            int lightWorldX = camX + light.gridX;
            int lightWorldY = camY + light.gridY;
            if (!game.world.inBounds(lightWorldX, lightWorldY)) continue;
            int startX = Math.max(0, (int)Math.floor(light.gridX - light.radius));
            int endX = Math.min(gridWidth - 1, (int)Math.ceil(light.gridX + light.radius));
            int startY = Math.max(0, (int)Math.floor(light.gridY - light.radius));
            int endY = Math.min(gridHeight - 1, (int)Math.ceil(light.gridY + light.radius));
            for (int y = startY; y <= endY; y++) {
                for (int x = startX; x <= endX; x++) {
                    int worldX = camX + x;
                    int worldY = camY + y;
                    if (!game.world.inBounds(worldX, worldY)) continue;
                    // Render-only glow must never reveal remote rooms: only the player's
                    // current visible tile set receives brightening. Remembered/unseen
                    // cells keep their existing darkness.
                    if (!game.isVisible(worldX, worldY)) continue;
                    if (!game.hasLineOfSight(lightWorldX, lightWorldY, worldX, worldY)) continue;
                    float dx = (x + 0.5f) - (light.gridX + 0.5f);
                    float dy = (y + 0.5f) - (light.gridY + 0.5f);
                    float dist = (float)Math.sqrt(dx * dx + dy * dy);
                    if (dist > light.radius + 0.001f) continue;
                    float norm = Math.max(0.0f, Math.min(1.0f, dist / Math.max(1.0f, light.radius)));
                    float smooth = norm * norm * (3.0f - 2.0f * norm);
                    float attenuation = Math.max(0.0f, (1.0f - smooth) * 0.82f + 0.22f / (1.0f + 2.5f * norm * norm));
                    float brightness = dynamic * attenuation;
                    if (brightness > 0.014f) blendLight(x, y, light.color, brightness);
                }
            }
        }
        if (mode >= 2) {
            extractBrightSpots(150);
            boxBlur(bloomPixels, blurScratch, gridWidth, gridHeight, 1);
        }
    }

    private float visualModifier(LightSample light, long now, boolean animate) {
        if (!animate || light.flicker == FlickerKind.STEADY) return 1.0f;
        double t = (now / 1_000_000_000.0) + (light.seed & 0xFFFF) * 0.017;
        if (light.flicker == FlickerKind.NEON) {
            double base = Math.sin(t * 9.0 + light.seed * 0.001);
            if (base > 0.92) return 0.20f;
            return (float)(1.0 + base * 0.035);
        }
        double wave = Math.sin(t * 2.1) * Math.cos(t * 4.7 + light.seed * 0.003) * Math.sin(t * 1.3 + 0.4);
        return (float)(1.0 + wave * 0.16);
    }

    private void blendLight(int x, int y, Color color, float brightness) {
        int index = x + y * gridWidth;
        int p = lightMapPixels[index];
        int a = (p >>> 24) & 0xFF;
        int r = (p >>> 16) & 0xFF;
        int g = (p >>> 8) & 0xFF;
        int b = p & 0xFF;
        int nr = Math.min(255, r + (int)(color.getRed() * brightness));
        int ng = Math.min(255, g + (int)(color.getGreen() * brightness));
        int nb = Math.min(255, b + (int)(color.getBlue() * brightness));
        int na = Math.max(0, a - (int)(brightness * 190));
        lightMapPixels[index] = (na << 24) | (nr << 16) | (ng << 8) | nb;
    }

    private void extractBrightSpots(int threshold) {
        for (int i = 0; i < lightMapPixels.length; i++) {
            int p = lightMapPixels[i];
            int r = (p >>> 16) & 0xFF;
            int g = (p >>> 8) & 0xFF;
            int b = p & 0xFF;
            int lum = (int)(0.2126f * r + 0.7152f * g + 0.0722f * b);
            if (lum > threshold) {
                bloomPixels[i] = (112 << 24) | (Math.min(255, (int)(r * 1.20f)) << 16) | (Math.min(255, (int)(g * 1.20f)) << 8) | Math.min(255, (int)(b * 1.20f));
            }
        }
    }

    private static void boxBlur(int[] pixels, int[] scratch, int w, int h, int radius) {
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int a = 0, r = 0, g = 0, b = 0, count = 0;
            for (int k = -radius; k <= radius; k++) {
                int xx = Math.max(0, Math.min(w - 1, x + k));
                int p = pixels[xx + y * w];
                a += (p >>> 24) & 0xFF; r += (p >>> 16) & 0xFF; g += (p >>> 8) & 0xFF; b += p & 0xFF; count++;
            }
            scratch[x + y * w] = ((a / count) << 24) | ((r / count) << 16) | ((g / count) << 8) | (b / count);
        }
        for (int x = 0; x < w; x++) for (int y = 0; y < h; y++) {
            int a = 0, r = 0, g = 0, b = 0, count = 0;
            for (int k = -radius; k <= radius; k++) {
                int yy = Math.max(0, Math.min(h - 1, y + k));
                int p = scratch[x + yy * w];
                a += (p >>> 24) & 0xFF; r += (p >>> 16) & 0xFF; g += (p >>> 8) & 0xFF; b += p & 0xFF; count++;
            }
            pixels[x + y * w] = ((a / count) << 24) | ((r / count) << 16) | ((g / count) << 8) | (b / count);
        }
    }

    private static boolean nearViewport(int x, int y, int camX, int camY, int cols, int rows, int radius) {
        return x >= camX - radius && y >= camY - radius && x < camX + cols + radius && y < camY + rows + radius;
    }

    private static float intensity01(int intensity) { return Math.max(0.15f, Math.min(1.45f, intensity / 72.0f)); }

    private static FlickerKind flickerKindFor(ZoneLightSourceRecord z) {
        String s = ((z.profile == null ? "" : z.profile) + " " + (z.colorName == null ? "" : z.colorName)).toLowerCase(Locale.ROOT);
        if (s.contains("neon") || s.contains("auspex") || s.contains("terminal") || s.contains("blue") || s.contains("green")) return FlickerKind.NEON;
        return FlickerKind.TORCH;
    }

    private static boolean itemLooksFlickery(String item) {
        String s = item == null ? "" : item.toLowerCase(Locale.ROOT);
        return s.contains("lantern") || s.contains("torch") || s.contains("phosphor") || s.contains("glow");
    }

    private static Color colorForPortable(String item) {
        String s = item == null ? "" : item.toLowerCase(Locale.ROOT);
        if (s.contains("flashlight") || s.contains("helmet")) return new Color(235, 245, 210);
        if (s.contains("glow") || s.contains("phosphor")) return new Color(170, 235, 170);
        return new Color(250, 220, 130);
    }

    private static int stableSeed(String id, int x, int y) {
        int h = 0x5F3759DF;
        String s = id == null ? "light" : id;
        for (int i = 0; i < s.length(); i++) h = 31 * h + s.charAt(i);
        h = 31 * h + x;
        h = 31 * h + y;
        return h;
    }

    String auditSummary() { return lastSummary; }

    static List<String> infopediaLines(GameOptions options) {
        ArrayList<String> lines = new ArrayList<>();
        int mode = options == null ? 0 : Math.max(0, Math.min(GameOptions.LIGHTING_FX_LABELS.length - 1, options.lightingFxIndex));
        lines.add("Visual lighting authority " + VERSION + ".");
        lines.add("Gameplay light remains turn-stable; combat and vision use the existing lightLevelAt cache.");
        lines.add("Visual light is render-only, clipped to the map pane, and line-of-sight blocked so wall illumination does not leak deeper than the struck wall tile.");
        lines.add("Current graphics lighting mode: " + GameOptions.LIGHTING_FX_LABELS[mode] + ".");
        lines.add("Bloom is low-resolution grid based and composited over the map pane without mutating world state.");
        lines.add("Reduced motion disables live flicker animation while preserving static lighting density.");
        return lines;
    }
}
