package mechanist;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.Random;

/**
 * Shared low-allocation visual feedback helpers for the 2D normal view and the
 * experimental first-person doom viewport. These classes avoid per-frame entity
 * allocation in their hot update/render loops by storing particles in primitive
 * arrays and by reusing all paint/composite/color instances that can be cached.
 */
final class VisualJuiceFramework {
    private VisualJuiceFramework() {}

    static void applyHighQualityHints(Graphics2D g) {
        if (g == null) return;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    }

    static void applyGpuFriendlyHints(Graphics2D g) {
        if (g == null) return;
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
    }

    static int clamp255(int v) { return v < 0 ? 0 : Math.min(255, v); }
    static float clamp01(float v) { return v < 0f ? 0f : Math.min(1f, v); }
    static double lerp(double a, double b, double t) { return a + (b - a) * Math.max(0.0, Math.min(1.0, t)); }

    static int shadeArgb(int argb, double brightness) {
        double b = Math.max(0.0, Math.min(1.0, brightness));
        int a = (argb >>> 24) & 255;
        int r = (int)(((argb >> 16) & 255) * b);
        int g = (int)(((argb >> 8) & 255) * b);
        int bl = (int)((argb & 255) * b);
        return (a << 24) | (r << 16) | (g << 8) | bl;
    }

    static int tintArgb(int argb, int tintRgb, float amount) {
        float t = clamp01(amount);
        int a = (argb >>> 24) & 255;
        int r = (int)(((argb >> 16) & 255) * (1f - t) + ((tintRgb >> 16) & 255) * t);
        int g = (int)(((argb >> 8) & 255) * (1f - t) + ((tintRgb >> 8) & 255) * t);
        int b = (int)((argb & 255) * (1f - t) + (tintRgb & 255) * t);
        return (a << 24) | (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b);
    }
}

record PlayerState(double currentHealth, double maxHealth, int food, int water, int fatigue,
                   String equippedLeftWeaponName, String equippedRightWeaponName,
                   int activeWeaponHandIndex, int currentPortraitFrame, BufferedImage portraitImage) {
    PlayerState {
        maxHealth = Math.max(1.0, maxHealth);
        currentHealth = Math.max(0.0, Math.min(currentHealth, maxHealth));
        food = Math.max(0, Math.min(100, food));
        water = Math.max(0, Math.min(100, water));
        fatigue = Math.max(0, Math.min(100, fatigue));
        equippedLeftWeaponName = equippedLeftWeaponName == null || equippedLeftWeaponName.isBlank() ? "LEFT EMPTY" : equippedLeftWeaponName;
        equippedRightWeaponName = equippedRightWeaponName == null || equippedRightWeaponName.isBlank() ? "RIGHT EMPTY" : equippedRightWeaponName;
        activeWeaponHandIndex = activeWeaponHandIndex == 0 ? 0 : 1;
        currentPortraitFrame = Math.max(0, currentPortraitFrame);
    }

    double healthRatio() { return currentHealth / maxHealth; }

    static PlayerState fromPanel(GamePanel panel) {
        double current = 100.0;
        double max = 100.0;
        if (panel != null && panel.active != null && panel.active.body != null && !panel.active.body.isEmpty()) {
            current = 0.0;
            max = 0.0;
            for (BodyPart bp : panel.active.body.values()) {
                if (bp == null) continue;
                current += bp.currentHealth();
                max += Math.max(1.0, bp.maxHealth());
            }
            if (max <= 1.0) { current = 100.0; max = 100.0; }
        }
        String left = panel == null ? "LEFT EMPTY" : panel.equippedLeftHandItem;
        String right = panel == null ? "RIGHT EMPTY" : panel.equippedRightHandItem;
        int frame = panel == null ? 0 : (int)((panel.turn / 10L) & 3L);
        BufferedImage portrait = panel == null || panel.images == null || panel.active == null ? null : panel.images.getPlayerPortrait(panel.active);
        return new PlayerState(current, max,
                panel == null ? 0 : panel.food,
                panel == null ? 0 : panel.water,
                panel == null ? 0 : panel.fatigue,
                left, right, panel == null ? 1 : panel.activeWeaponHandIndex, frame, portrait);
    }
}

final class GameHudOverlay {
    private static final int SEGMENTS = 10;
    private final Color bottomPanel = new Color(12, 14, 18, 215);
    private final Color border = new Color(172, 142, 82, 220);
    private final Color trim = new Color(55, 45, 32, 230);
    private final Color empty = new Color(32, 24, 22, 225);
    private final Color text = new Color(235, 218, 166, 235);
    private final Color portrait = new Color(42, 48, 54, 235);
    private final Color slotBg = new Color(16, 18, 23, 225);
    private final Color portraitEye = new Color(85, 96, 105, 240);
    private final Color slotInner = new Color(88, 70, 43, 230);
    private final Color slotTitle = new Color(190, 166, 105, 235);
    private final Color segmentBorder = new Color(10, 10, 10, 180);
    private final Color segmentGlow = new Color(255, 255, 230, 40);
    private final Font hudFont = new Font("Monospaced", Font.BOLD, 12);
    private final Font smallFont = new Font("Monospaced", Font.PLAIN, 10);

    /** Renders the shared bottom HUD over either normal 2D or doom mode. */
    void renderHUD(Graphics2D g2d, PlayerState state, int width, int height) {
        if (g2d == null || state == null || width <= 0 || height <= 0) return;
        int panelH = Math.max(82, Math.min(124, height / 5));
        int y = height - panelH;
        int centerX = width / 2;
        g2d.setFont(hudFont);
        g2d.setColor(bottomPanel);
        g2d.fillRect(0, y, width, panelH);
        g2d.setColor(trim);
        g2d.fillRect(0, y, width, 5);
        g2d.setColor(border);
        g2d.drawRect(0, y, width - 1, panelH - 1);

        int portraitW = Math.max(74, Math.min(112, panelH - 16));
        int portraitH = portraitW;
        int portraitX = centerX - portraitW / 2;
        int portraitY = y + (panelH - portraitH) / 2;
        g2d.setColor(portrait);
        g2d.fillRect(portraitX, portraitY, portraitW, portraitH);
        if (state.portraitImage() != null) {
            g2d.drawImage(state.portraitImage(), portraitX + 3, portraitY + 3, portraitW - 6, portraitH - 6, null);
        } else {
            g2d.setColor(portraitEye);
            int eyeY = portraitY + portraitH / 3 + (state.currentPortraitFrame() % 2);
            g2d.fillRect(portraitX + portraitW / 4, eyeY, 8, 5);
            g2d.fillRect(portraitX + portraitW - portraitW / 4 - 8, eyeY, 8, 5);
            g2d.setColor(text);
            drawCentered(g2d, "PORTRAIT", portraitX + portraitW / 2, portraitY + portraitH - 10);
        }
        g2d.setColor(border);
        g2d.drawRect(portraitX, portraitY, portraitW, portraitH);

        int slotW = Math.max(150, Math.min(230, width / 5));
        int slotH = Math.max(54, panelH - 28);
        int slotY = y + 14;
        renderWeaponSlot(g2d, 10, slotY, slotW, slotH, "EQUIPPED LEFT WEAPON", state.equippedLeftWeaponName(), state.activeWeaponHandIndex() == 0);
        renderWeaponSlot(g2d, width - slotW - 10, slotY, slotW, slotH, "EQUIPPED RIGHT WEAPON", state.equippedRightWeaponName(), state.activeWeaponHandIndex() == 1);

        int barX = Math.max(172, portraitX - 286);
        int barY = y + 22;
        int barW = Math.max(170, Math.min(260, portraitX - barX - 18));
        renderSegmentedBar(g2d, barX, barY, barW, 28, state.healthRatio());
        g2d.setFont(smallFont);
        g2d.setColor(text);
        g2d.drawString("ENDURANCE " + Math.round(state.currentHealth()) + " / " + Math.round(state.maxHealth()), barX, barY + 45);
        int vitalX = portraitX + portraitW + 18;
        int vitalW = Math.max(150, Math.min(240, width - vitalX - slotW - 28));
        if (vitalW >= 120) {
            renderMiniBar(g2d, vitalX, y + 18, vitalW, "FOOD", state.food(), state.food() / 100.0, new Color(96, 176, 82, 235));
            renderMiniBar(g2d, vitalX, y + 40, vitalW, "WATER", state.water(), state.water() / 100.0, new Color(82, 154, 214, 235));
            renderMiniBar(g2d, vitalX, y + 62, vitalW, "ENERGY", 100 - state.fatigue(), 1.0 - state.fatigue() / 100.0, new Color(218, 178, 72, 235));
        }
    }

    private void renderWeaponSlot(Graphics2D g2d, int x, int y, int w, int h, String title, String item, boolean active) {
        g2d.setColor(slotBg);
        g2d.fillRect(x, y, w, h);
        g2d.setColor(active ? new Color(245, 214, 118, 245) : border);
        g2d.drawRect(x, y, w, h);
        if (active) {
            g2d.setColor(new Color(245, 214, 118, 34));
            g2d.fillRect(x + 2, y + 2, w - 4, h - 4);
        }
        g2d.setColor(active ? new Color(245, 214, 118, 210) : slotInner);
        g2d.drawRect(x + 4, y + 4, w - 8, h - 8);
        g2d.setFont(smallFont);
        g2d.setColor(active ? new Color(255, 236, 154, 245) : slotTitle);
        g2d.drawString((active ? "ACTIVE " : "") + title, x + 8, y + 16);
        g2d.setFont(hudFont);
        g2d.setColor(text);
        String label = item == null || item.isBlank() ? "EMPTY" : item;
        FontMetrics fm = g2d.getFontMetrics();
        while (label.length() > 5 && fm.stringWidth(label) > w - 18) label = label.substring(0, label.length() - 4) + "...";
        g2d.drawString(label, x + 8, y + h - 17);
    }

    private void renderSegmentedBar(Graphics2D g2d, int x, int y, int w, int h, double ratio) {
        double clamped = Math.max(0.0, Math.min(1.0, ratio));
        int gap = 3;
        int segW = Math.max(6, (w - gap * (SEGMENTS - 1)) / SEGMENTS);
        int filled = (int)Math.ceil(clamped * SEGMENTS);
        for (int i = 0; i < SEGMENTS; i++) {
            int sx = x + i * (segW + gap);
            g2d.setColor(i < filled ? colorForRatio(clamped) : empty);
            g2d.fillRect(sx, y, segW, h);
            g2d.setColor(segmentBorder);
            g2d.drawRect(sx, y, segW, h);
            if (i < filled) {
                g2d.setColor(segmentGlow);
                g2d.fillRect(sx + 1, y + 1, Math.max(1, segW - 2), Math.max(1, h / 4));
            }
        }
        g2d.setColor(border);
        g2d.drawRect(x - 3, y - 3, SEGMENTS * segW + (SEGMENTS - 1) * gap + 6, h + 6);
    }

    private void renderMiniBar(Graphics2D g2d, int x, int y, int w, String label, int value, double ratio, Color fill) {
        String readout = label + " " + Math.max(0, Math.min(100, value));
        int labelW = Math.max(64, g2d.getFontMetrics(smallFont).stringWidth(readout) + 8);
        int barW = Math.max(24, w - labelW);
        g2d.setFont(smallFont);
        g2d.setColor(text);
        g2d.drawString(readout, x, y + 10);
        g2d.setColor(empty);
        g2d.fillRect(x + labelW, y, barW, 12);
        g2d.setColor(fill == null ? segmentGlow : fill);
        g2d.fillRect(x + labelW, y, (int)Math.round(barW * Math.max(0.0, Math.min(1.0, ratio))), 12);
        g2d.setColor(border);
        g2d.drawRect(x + labelW, y, barW, 12);
    }

    private Color colorForRatio(double ratio) {
        return ratio > 0.60 ? new Color(58, 178, 74, 235)
                : ratio > 0.30 ? new Color(224, 177, 56, 235)
                : new Color(196, 54, 48, 235);
    }

    private void drawCentered(Graphics2D g, String s, int x, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, x - fm.stringWidth(s) / 2, y);
    }
}

record CameraJuiceState(double verticalOffset, double backwardOffset, double pitchOffsetRadians) {}

final class CameraJuiceSystem {
    private double bobPhase;
    private double movementEnergy;
    private double recoilVertical;
    private double recoilBackward;
    private double recoilPitch;
    private CameraJuiceState state = new CameraJuiceState(0, 0, 0);

    void noteMovementImpulse() { movementEnergy = Math.min(1.0, movementEnergy + 0.45); }

    void applyWeaponRecoil() {
        recoilVertical += 0.060;
        recoilBackward += 0.075;
        recoilPitch += Math.toRadians(2.4);
    }

    CameraJuiceState update(double dtSeconds) {
        double dt = Math.max(0.0, Math.min(0.10, dtSeconds));
        movementEnergy = VisualJuiceFramework.lerp(movementEnergy, 0.0, Math.min(1.0, dt * 3.5));
        bobPhase += dt * (7.0 + movementEnergy * 8.0);
        double bob = Math.sin(bobPhase * Math.PI * 2.0) * 0.026 * movementEnergy;
        recoilVertical = VisualJuiceFramework.lerp(recoilVertical, 0.0, Math.min(1.0, dt * 8.0));
        recoilBackward = VisualJuiceFramework.lerp(recoilBackward, 0.0, Math.min(1.0, dt * 7.0));
        recoilPitch = VisualJuiceFramework.lerp(recoilPitch, 0.0, Math.min(1.0, dt * 10.0));
        state = new CameraJuiceState(bob + recoilVertical, recoilBackward, recoilPitch);
        return state;
    }

    CameraJuiceState state() { return state; }
}

final class ScreenGlitchEffect {
    private final Random random = new Random(0xD00DFEEDL);
    private long glitchUntilMillis;
    private boolean forced;

    void trigger(long now, long durationMillis) { glitchUntilMillis = Math.max(glitchUntilMillis, now + Math.max(1L, durationMillis)); }
    void setForced(boolean forced) { this.forced = forced; }
    boolean active(long now) { return forced || now < glitchUntilMillis; }

    void apply(int[] pixels, int w, int h, long now) {
        if (pixels == null || pixels.length < w * h || w <= 4 || h <= 4 || !active(now)) return;
        int rowCount = Math.max(4, h / 26);
        for (int i = 0; i < rowCount; i++) {
            int y = 1 + random.nextInt(Math.max(1, h - 2));
            int shift = random.nextInt(13) - 6;
            if (shift == 0) shift = 3;
            int row = y * w;
            if (shift > 0) {
                for (int x = w - 1; x >= shift; x--) pixels[row + x] = pixels[row + x - shift];
            } else {
                int s = -shift;
                for (int x = 0; x < w - s; x++) pixels[row + x] = pixels[row + x + s];
            }
        }
        int columns = Math.max(2, w / 90);
        for (int i = 0; i < columns; i++) {
            int x = random.nextInt(w);
            int mask = 0x00202020 | (random.nextBoolean() ? 0x00004060 : 0x00602020);
            for (int y = 0; y < h; y += 2) {
                int idx = y * w + x;
                pixels[idx] = 0xFF000000 | ((pixels[idx] ^ mask) & 0x00FFFFFF);
            }
        }
    }
}

final class PrimitiveParticleEmitter2D {
    static final int MAX = 512;
    private final float[] x = new float[MAX];
    private final float[] y = new float[MAX];
    private final float[] vx = new float[MAX];
    private final float[] vy = new float[MAX];
    private final float[] life = new float[MAX];
    private final float[] maxLife = new float[MAX];
    private final float[] size = new float[MAX];
    private final int[] color = new int[MAX];
    private final boolean[] active = new boolean[MAX];
    private int cursor;
    private final Random random = new Random(0x51A7E11L);

    void update(float dt) {
        float step = Math.max(0f, Math.min(0.1f, dt));
        for (int i = 0; i < MAX; i++) {
            if (!active[i]) continue;
            life[i] -= step;
            if (life[i] <= 0f) { active[i] = false; continue; }
            x[i] += vx[i] * step;
            y[i] += vy[i] * step;
            vy[i] += 80f * step;
        }
    }

    void render(Graphics2D g) {
        if (g == null) return;
        Composite old = g.getComposite();
        for (int i = 0; i < MAX; i++) {
            if (!active[i]) continue;
            float ratio = maxLife[i] <= 0f ? 0f : Math.max(0f, Math.min(1f, life[i] / maxLife[i]));
            int a = (int)(((color[i] >>> 24) & 255) * ratio);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, a / 255f))));
            g.setColor(new Color(color[i] | 0xFF000000, true));
            int s = Math.max(1, (int)(size[i] * (0.35f + ratio * 0.65f)));
            g.fillRect((int)x[i] - s / 2, (int)y[i] - s / 2, s, s);
        }
        g.setComposite(old);
    }

    void spawnWeaponFire(WeaponFireProfile profile, float startX, float startY, float dirX, float dirY) {
        float len = (float)Math.sqrt(dirX * dirX + dirY * dirY);
        float nx = len <= 0.0001f ? 1f : dirX / len;
        float ny = len <= 0.0001f ? 0f : dirY / len;
        if (profile instanceof WeaponFireProfile.LasBeam las) {
            spawnLine(startX, startY, nx, ny, las.count(), las.speed(), las.argb());
        } else if (profile instanceof WeaponFireProfile.Plasma plasma) {
            spawnLine(startX, startY, nx, ny, plasma.count(), plasma.speed(), plasma.argb());
        } else if (profile instanceof WeaponFireProfile.ShotgunBlast shotgun) {
            spawnCone(startX, startY, nx, ny, shotgun.count(), shotgun.spreadRadians(), shotgun.speed(), shotgun.argb());
        } else if (profile instanceof WeaponFireProfile.ImpactBurst impact) {
            spawnRadial(startX, startY, impact.count(), impact.speed(), impact.argb());
        }
    }

    void spawnImpact(float x, float y) { spawnRadial(x, y, 42, 170f, 0xFFFFB14D); }

    private void spawnLine(float sx, float sy, float nx, float ny, int count, float speed, int argb) {
        float tangentX = -ny;
        float tangentY = nx;
        for (int i = 0; i < count; i++) {
            float scatter = (random.nextFloat() - 0.5f) * 7f;
            float sp = speed * (0.86f + random.nextFloat() * 0.26f);
            emit(sx + tangentX * scatter + nx * i * 2f, sy + tangentY * scatter + ny * i * 2f, nx * sp, ny * sp, 0.22f + random.nextFloat() * 0.16f, 3f + random.nextFloat() * 3f, argb);
        }
    }

    private void spawnCone(float sx, float sy, float nx, float ny, int count, float spread, float speed, int argb) {
        float base = (float)Math.atan2(ny, nx);
        for (int i = 0; i < count; i++) {
            float angle = base + (random.nextFloat() - 0.5f) * spread;
            float sp = speed * (0.55f + random.nextFloat() * 0.75f);
            emit(sx, sy, (float)Math.cos(angle) * sp, (float)Math.sin(angle) * sp, 0.18f + random.nextFloat() * 0.22f, 2f + random.nextFloat() * 4f, argb);
        }
    }

    private void spawnRadial(float sx, float sy, int count, float speed, int argb) {
        for (int i = 0; i < count; i++) {
            float angle = (float)(random.nextFloat() * Math.PI * 2.0);
            float sp = speed * (0.25f + random.nextFloat());
            emit(sx, sy, (float)Math.cos(angle) * sp, (float)Math.sin(angle) * sp, 0.24f + random.nextFloat() * 0.32f, 2f + random.nextFloat() * 5f, argb);
        }
    }

    private void emit(float px, float py, float pvx, float pvy, float seconds, float particleSize, int argb) {
        int i = cursor++ & (MAX - 1);
        active[i] = true;
        x[i] = px; y[i] = py; vx[i] = pvx; vy[i] = pvy;
        life[i] = seconds; maxLife[i] = seconds; size[i] = particleSize; color[i] = argb;
    }

    int activeCount() {
        int n = 0;
        for (boolean b : active) if (b) n++;
        return n;
    }
}

sealed interface WeaponFireProfile permits WeaponFireProfile.LasBeam, WeaponFireProfile.Plasma, WeaponFireProfile.ShotgunBlast, WeaponFireProfile.ImpactBurst {
    record LasBeam(int count, float speed, int argb) implements WeaponFireProfile {}
    record Plasma(int count, float speed, int argb) implements WeaponFireProfile {}
    record ShotgunBlast(int count, float spreadRadians, float speed, int argb) implements WeaponFireProfile {}
    record ImpactBurst(int count, float speed, int argb) implements WeaponFireProfile {}
}

record LightSource(float centerX, float centerY, float radius, float brightness, Color color) {
    LightSource {
        radius = Math.max(1f, radius);
        brightness = Math.max(0f, Math.min(1f, brightness));
        color = color == null ? Color.WHITE : color;
    }
}

final class LightmapRenderer2D {
    private VolatileImage volatileLightmap;
    private BufferedImage fallbackLightmap;
    private int width;
    private int height;
    private final Color ambient = new Color(4, 6, 13, 190);
    private final float[] fractions = new float[]{0f, 0.55f, 1f};
    private final Color[] gradientColors = new Color[]{new Color(255,255,255,230), new Color(255,255,255,100), new Color(255,255,255,0)};
    private final float[] singleWarmLight = new float[4];

    void render(Graphics2D target, int w, int h, LightSource[] sources, int count) {
        if (target == null || w <= 0 || h <= 0) return;
        int safeW = Math.max(1, w);
        int safeH = Math.max(1, h);
        GraphicsConfiguration gc = target.getDeviceConfiguration();
        if (gc != null && !GraphicsEnvironment.isHeadless()) {
            renderVolatile(target, gc, safeW, safeH, sources, count);
        } else {
            renderFallback(target, safeW, safeH, sources, count);
        }
    }

    void renderSingleWarmLight(Graphics2D target, int w, int h, float centerX, float centerY, float radius, float brightness) {
        singleWarmLight[0] = centerX;
        singleWarmLight[1] = centerY;
        singleWarmLight[2] = Math.max(1f, radius);
        singleWarmLight[3] = Math.max(0f, Math.min(1f, brightness));
        if (target == null || w <= 0 || h <= 0) return;
        int safeW = Math.max(1, w);
        int safeH = Math.max(1, h);
        GraphicsConfiguration gc = target.getDeviceConfiguration();
        if (gc != null && !GraphicsEnvironment.isHeadless()) renderVolatilePrimitive(target, gc, safeW, safeH);
        else renderFallbackPrimitive(target, safeW, safeH);
    }

    String surfaceStatus() {
        return volatileLightmap != null ? "volatile-vram" : "buffered-fallback";
    }

    private void renderVolatile(Graphics2D target, GraphicsConfiguration gc, int w, int h, LightSource[] sources, int count) {
        ensureVolatile(gc, w, h);
        boolean drawn = false;
        for (int attempt = 0; attempt < 3; attempt++) {
            int status = volatileLightmap.validate(gc);
            if (status == VolatileImage.IMAGE_INCOMPATIBLE) {
                volatileLightmap.flush();
                volatileLightmap = gc.createCompatibleVolatileImage(w, h, Transparency.TRANSLUCENT);
            }
            Graphics2D lg = volatileLightmap.createGraphics();
            try {
                renderLightLayer(lg, w, h, sources, count);
            } finally {
                lg.dispose();
            }
            if (!volatileLightmap.contentsLost()) {
                target.drawImage(volatileLightmap, 0, 0, null);
                drawn = true;
                break;
            }
        }
        if (!drawn) renderFallback(target, w, h, sources, count);
    }

    private void renderFallback(Graphics2D target, int w, int h, LightSource[] sources, int count) {
        ensureFallback(w, h);
        Graphics2D lg = fallbackLightmap.createGraphics();
        try {
            renderLightLayer(lg, w, h, sources, count);
        } finally {
            lg.dispose();
        }
        target.drawImage(fallbackLightmap, 0, 0, null);
    }

    private void renderLightLayer(Graphics2D g, int w, int h, LightSource[] sources, int count) {
        VisualJuiceFramework.applyGpuFriendlyHints(g);
        g.setComposite(AlphaComposite.Src);
        g.setColor(ambient);
        g.fillRect(0, 0, w, h);
        g.setComposite(AlphaComposite.DstOut);
        int safeCount = Math.max(0, Math.min(count, sources == null ? 0 : sources.length));
        for (int i = 0; i < safeCount; i++) {
            LightSource ls = sources[i];
            if (ls == null) continue;
            float r = ls.radius();
            RadialGradientPaint paint = new RadialGradientPaint(ls.centerX(), ls.centerY(), r, fractions, gradientColors);
            g.setPaint(paint);
            g.fillOval((int)(ls.centerX() - r), (int)(ls.centerY() - r), (int)(r * 2f), (int)(r * 2f));
        }
        g.setComposite(AlphaComposite.SrcOver);
    }

    private void renderVolatilePrimitive(Graphics2D target, GraphicsConfiguration gc, int w, int h) {
        ensureVolatile(gc, w, h);
        boolean drawn = false;
        for (int attempt = 0; attempt < 3; attempt++) {
            int status = volatileLightmap.validate(gc);
            if (status == VolatileImage.IMAGE_INCOMPATIBLE) {
                volatileLightmap.flush();
                volatileLightmap = gc.createCompatibleVolatileImage(w, h, Transparency.TRANSLUCENT);
            }
            Graphics2D lg = volatileLightmap.createGraphics();
            try { renderPrimitiveLightLayer(lg, w, h); }
            finally { lg.dispose(); }
            if (!volatileLightmap.contentsLost()) { target.drawImage(volatileLightmap, 0, 0, null); drawn = true; break; }
        }
        if (!drawn) renderFallbackPrimitive(target, w, h);
    }

    private void renderFallbackPrimitive(Graphics2D target, int w, int h) {
        ensureFallback(w, h);
        Graphics2D lg = fallbackLightmap.createGraphics();
        try { renderPrimitiveLightLayer(lg, w, h); }
        finally { lg.dispose(); }
        target.drawImage(fallbackLightmap, 0, 0, null);
    }

    private void renderPrimitiveLightLayer(Graphics2D g, int w, int h) {
        VisualJuiceFramework.applyGpuFriendlyHints(g);
        g.setComposite(AlphaComposite.Src);
        g.setColor(ambient);
        g.fillRect(0, 0, w, h);
        g.setComposite(AlphaComposite.DstOut);
        float r = singleWarmLight[2];
        RadialGradientPaint paint = new RadialGradientPaint(singleWarmLight[0], singleWarmLight[1], r, fractions, gradientColors);
        g.setPaint(paint);
        g.fillOval((int)(singleWarmLight[0] - r), (int)(singleWarmLight[1] - r), (int)(r * 2f), (int)(r * 2f));
        g.setComposite(AlphaComposite.SrcOver);
    }

    private void ensureVolatile(GraphicsConfiguration gc, int w, int h) {
        if (volatileLightmap != null && width == w && height == h) return;
        width = w;
        height = h;
        if (volatileLightmap != null) volatileLightmap.flush();
        volatileLightmap = gc.createCompatibleVolatileImage(w, h, Transparency.TRANSLUCENT);
    }

    private void ensureFallback(int w, int h) {
        if (fallbackLightmap != null && width == w && height == h) return;
        width = w;
        height = h;
        fallbackLightmap = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
    }
}

record Collidable(int x, int y, int width, int height) {}

final class AabbCollisionSystem {
    static boolean intersects(Collidable a, Collidable b) {
        return a != null && b != null
                && a.x() < b.x() + b.width()
                && a.x() + a.width() > b.x()
                && a.y() < b.y() + b.height()
                && a.y() + a.height() > b.y();
    }

    static int updateProjectiles(Projectile2DPool pool, Collidable[] solids, int solidCount, PrimitiveParticleEmitter2D emitter, float dt) {
        if (pool == null) return 0;
        int impacts = 0;
        int safeCount = Math.max(0, Math.min(solidCount, solids == null ? 0 : solids.length));
        for (int i = 0; i < pool.capacity(); i++) {
            if (!pool.active(i)) continue;
            pool.integrate(i, dt);
            int ax = (int)pool.x(i) - 2;
            int ay = (int)pool.y(i) - 2;
            for (int s = 0; s < safeCount; s++) {
                Collidable b = solids[s];
                if (b != null && ax < b.x() + b.width() && ax + 4 > b.x() && ay < b.y() + b.height() && ay + 4 > b.y()) {
                    pool.deactivate(i);
                    if (emitter != null) emitter.spawnImpact(pool.x(i), pool.y(i));
                    impacts++;
                    break;
                }
            }
        }
        return impacts;
    }
}

final class Projectile2DPool {
    private static final int MAX = 128;
    private final float[] x = new float[MAX];
    private final float[] y = new float[MAX];
    private final float[] vx = new float[MAX];
    private final float[] vy = new float[MAX];
    private final boolean[] active = new boolean[MAX];
    private int cursor;

    int capacity() { return MAX; }
    boolean active(int i) { return i >= 0 && i < MAX && active[i]; }
    float x(int i) { return x[i]; }
    float y(int i) { return y[i]; }

    void spawn(float px, float py, float pvx, float pvy) {
        int i = cursor++ % MAX;
        active[i] = true; x[i] = px; y[i] = py; vx[i] = pvx; vy[i] = pvy;
    }

    void integrate(int i, float dt) {
        if (!active(i)) return;
        float step = Math.max(0f, Math.min(0.1f, dt));
        x[i] += vx[i] * step;
        y[i] += vy[i] * step;
    }

    void deactivate(int i) { if (i >= 0 && i < MAX) active[i] = false; }
}
