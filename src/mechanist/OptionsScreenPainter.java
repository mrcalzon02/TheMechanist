package mechanist;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class OptionsScreenPainter {
    private OptionsScreenPainter() {}

    static final class Layout {
        final int width;
        final int height;
        final int panelX;
        final int panelY;
        final int panelW;
        final int panelH;

        Layout(int width, int height, int panelX, int panelY, int panelW, int panelH) {
            this.width = width;
            this.height = height;
            this.panelX = panelX;
            this.panelY = panelY;
            this.panelW = panelW;
            this.panelH = panelH;
        }
    }

    static Layout layout(GamePanel panel) {
        int width = panel.getWidth();
        int height = panel.getHeight();
        int panelX = Math.max(34, width / 2 - Math.min(620, width / 2 - 34));
        int panelY = Math.max(34, height / 22);
        int panelW = Math.min(width - panelX * 2, Math.max(980, width - 90));
        int panelH = Math.max(430, height - panelY - 64);
        return new Layout(width, height, panelX, panelY, panelW, panelH);
    }

    static java.awt.Rectangle subtitleBox(Layout layout) {
        return new java.awt.Rectangle(layout.panelX + 44, layout.panelY + 74, layout.panelW - 88, 28);
    }

    static java.awt.Rectangle controlsBox(Layout layout) {
        int x = layout.panelX + 44;
        int y = layout.panelY + 108;
        int w = layout.panelW - 88;
        int h = Math.max(116, Math.min(188, layout.panelH / 4));
        return new java.awt.Rectangle(x, y, w, h);
    }

    static java.awt.Rectangle infoBox(Layout layout) {
        java.awt.Rectangle controls = controlsBox(layout);
        int x = layout.panelX + 44;
        int y = controls.y + controls.height + 12;
        int w = layout.panelW - 88;
        int h = Math.max(96, layout.panelY + layout.panelH - y - 50);
        return new java.awt.Rectangle(x, y, w, h);
    }

    static void paintShell(GamePanel panel, Graphics2D g) {
        Layout layout = layout(panel);
        g.setColor(LayerF.optionColor(panel, GameOptions.BACKGROUND));
        g.fillRect(0, 0, layout.width, layout.height);
        panel.drawSlicedFrame(g, 20, 20, layout.width - 40, layout.height - 40, "outer");
        panel.drawPanelBox(g, layout.panelX, layout.panelY, layout.panelW, layout.panelH, "OPTIONS");
        g.setFont(panel.smallFont.deriveFont(Font.BOLD, Math.max(10f, Math.min(14f, layout.height / 64f))));
        g.setColor(LayerF.optionColor(panel, GameOptions.TEXT_TITLE));
        g.drawString("OPTIONS", layout.panelX + 18, layout.panelY + 26);
        java.awt.Rectangle sub = subtitleBox(layout);
        panel.drawTextPanel(g, sub.x, sub.y, sub.width, sub.height, Collections.singletonList(subtitle(panel.optionsTab)), true);
        java.awt.Rectangle controls = controlsBox(layout);
        g.setColor(new Color(0, 0, 0, 202));
        g.fillRoundRect(controls.x, controls.y, controls.width, controls.height, 12, 12);
        panel.drawSlicedFrame(g, controls.x, controls.y, controls.width, controls.height, "inner");
        panel.stampUiFrameId(g, "T", "options-controls", controls.x, controls.y, controls.width, controls.height);
    }

    static void paintBody(GamePanel panel, Graphics2D g) {
        Layout layout = layout(panel);
        java.awt.Rectangle info = infoBox(layout);
        g.setFont(panel.smallFont);
        List<String> lines = linesForTab(panel);
        int h = panel.optionsTab == 4 ? Math.max(96, info.height - 72) : info.height;
        panel.drawTextPanel(g, info.x, info.y, info.width, h, lines, false);
        if (panel.optionsTab == 4) paintColorSwatches(panel, g, info);
    }

    static void paintColorSwatches(GamePanel panel, Graphics2D g, java.awt.Rectangle info) {
        int swatchY = info.y + Math.max(96, info.height - 66);
        int sx = info.x + 18;
        for (int i = 0; i < GameOptions.COLOR_KEYS.length; i++) {
            int px = sx + i * Math.max(128, (info.width - 36) / GameOptions.COLOR_KEYS.length);
            g.setColor(new Color(panel.options.colorValue(i)));
            g.fillRect(px, swatchY, 28, 18);
            g.setColor(i == panel.options.colorTarget ? LayerF.optionColor(panel, GameOptions.TEXT_HIGHLIGHT) : LayerF.optionColor(panel, GameOptions.TEXT_MAIN));
            g.drawRect(px, swatchY, 28, 18);
            g.setFont(panel.smallFont);
            for (String line : TextSurfaceApi.wrap(GameOptions.COLOR_KEYS[i], 11)) {
                panel.drawUiTextLine(g, line, px + 34, swatchY + 13);
                break;
            }
        }
    }

    static void paintGraphicsDropdownPopup(GamePanel panel, Graphics2D g) {
        if (panel.screen != GamePanel.Screen.OPTIONS || panel.graphicsDropdown < 0 || (panel.optionsTab != 0 && panel.optionsTab != 1 && panel.optionsTab != 4)) return;
        java.awt.Rectangle r = panel.graphicsDropdownOuterRect();
        if (r.width <= 0 || r.height <= 0) return;
        g.setColor(new Color(0, 0, 0, 218));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        panel.drawSlicedFrame(g, r.x, r.y, r.width, r.height, "inner");
        panel.stampUiFrameId(g, "F", "graphics-dropdown-popup", r.x, r.y, r.width, r.height);
    }

    static List<String> linesForTab(GamePanel panel) {
        if (panel.optionsTab == 0) return displayLines(panel);
        if (panel.optionsTab == 1) return textUiLines(panel);
        if (panel.optionsTab == 2) return audioLines(panel);
        if (panel.optionsTab == 3) return controlsLines(panel);
        if (panel.optionsTab == 4) return graphicsLines(panel);
        if (panel.optionsTab == 5) return jvmLines(panel);
        if (panel.optionsTab == 6) return accessibilityLines(panel);
        return qolLines(panel);
    }

    static List<String> displayLines(GamePanel panel) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Window Mode: " + panel.options.windowModeLabel());
        lines.add("Resolution: " + panel.options.resolutionLabel());
        lines.add("Detected/Safe Display Modes: " + DisplayResolutionAuthority.choiceCount());
        lines.add("Screensaver: " + (panel.options.screenSaver ? "ON" : "OFF"));
        lines.add("Display owns windowed, borderless windowed, exclusive fullscreen, resolution selection, and applying those monitor/window settings.");
        return lines;
    }

    static List<String> textUiLines(GamePanel panel) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Font/Text Scale: " + panel.options.fontScale + "%");
        lines.add("GUI/Chrome Scale: " + panel.options.uiScale + "%");
        lines.add("Text Crispness: " + panel.options.renderQualityLabel());
        lines.add("Floating Hover Help: " + (panel.options.hoverHelp ? "ON" : "OFF"));
        lines.add("Compact text is the default; larger text is an opt-in accessibility setting. Crispness changes Java2D text-rendering hints without pretending to be text size.");
        return lines;
    }

    static List<String> audioLines(GamePanel panel) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("SFX: " + (panel.options.soundEnabled ? "ON" : "OFF") + " / " + panel.options.sfxVolume + "%");
        lines.add("MUSIC: " + (panel.options.musicEnabled ? "ON" : "OFF") + " / " + panel.options.musicVolume + "%");
        lines.add("VOICE / CONVERSATION: " + (panel.options.conversationSound ? "ON" : "OFF") + " / " + panel.options.conversationVolume + "%");
        lines.add("BOOT SOUND: " + (panel.options.bootSound ? "ON" : "OFF"));
        lines.add("Music has a dedicated channel even before music assets are imported. Defaults begin around 80% so the first audible implementation is not a screaming logic Engine.");
        return lines;
    }

    static List<String> controlsLines(GamePanel panel) {
        ArrayList<String> lines = new ArrayList<>(ControlReferenceTextSubsystem.controlReferenceLines(panel.controlsTab));
        if (panel.controlsTab == 4) {
            lines.add("Runtime controller status: " + (panel.gamepadInputEngine == null ? "not started" : panel.gamepadInputEngine.status()));
        }
        return lines;
    }

    static List<String> graphicsLines(GamePanel panel) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Render Downscale: " + panel.options.downscaleLabel() + " / internal " + panel.renderScaling.internalWidth() + "x" + panel.renderScaling.internalHeight());
        lines.add("Target FPS: " + panel.options.targetFpsLabel() + " / Frame limiter " + panel.options.frameLimitLabel() + " / timer delay " + panel.options.targetTimerDelayMs() + "ms");
        lines.add("Render Quality: " + panel.options.renderQualityLabel());
        lines.add("Visual Lighting FX: " + panel.options.lightingFxLabel() + " / render-only deterministic lightmap");
        lines.add("Runtime Profile: " + panel.jvmRuntimeProfile.targetLabel() + " / memory " + panel.jvmRuntimeProfile.initialRamMb + "-" + panel.jvmRuntimeProfile.maxRamMb + " MB / " + panel.jvmRuntimeProfile.gc.label + ".");
        lines.add("Reduced Motion: " + (panel.options.reducedMotion ? "ON" : "OFF"));
        lines.add("F3 Performance Overlay: " + (panel.options.diagnosticsOverlay ? "ON" : "OFF") + ".");
        lines.add("Frame pacing: " + panel.frameLimiter.snapshot(panel.renderStressTest.active()).compactLine());
        lines.add("Imported Portrait Sheets: " + (panel.options.importedPortraits ? "ON" : "OFF"));
        lines.add("Tile Icon Rendering: " + (panel.options.tileIconRendering ? "ON" : "OFF"));
        lines.add("Art Quality Cache: " + panel.options.artQualityLabel() + " / folder " + panel.options.artQualityFolder());
        lines.add("Generated Art Payload: " + panel.options.generatedAssetPayloadRootLabel());
        try {
            var runtime = AssetManager.generatedAssetRuntime();
            lines.add("Generated Runtime: " + (runtime.runtimeManifestPresent() && runtime.tierManifestPresent() ? "ready" : "partial") + " / payload roots " + runtime.generatedPayloadRoots().size());
        } catch (Throwable ignored) {
            lines.add("Generated Runtime: unavailable");
        }
        lines.add("Map Tile Size: " + panel.options.mapTileSizeLabel() + " / " + panel.options.mapTilePixelSize() + "px before GUI scale");
        lines.add("Render Scaling Profile: " + panel.renderScaling.profileLabel() + " / option downscale " + panel.renderScaling.downscaleLabel() + " / F10 cycles profiles.");
        lines.add("Color Preset: " + GameOptions.PALETTE_NAMES[panel.options.colorPreset] + " / Editing " + panel.options.colorTargetLabel());
        return lines;
    }

    static List<String> jvmLines(GamePanel panel) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(panel.jvmRuntimeRestartPending ? "RESTART PENDING: selected JVM profile has been saved; use ACCEPT + RESTART to relaunch now." : "RESTART REQUIRED: selected JVM options do not fully affect the active process until restart.");
        lines.add("Current profile: " + panel.jvmRuntimeProfile.targetLabel() + " / " + panel.jvmRuntimeProfile.mode);
        lines.add("Memory profile: -Xms" + panel.jvmRuntimeProfile.initialRamMb + "M / -Xmx" + panel.jvmRuntimeProfile.maxRamMb + "M.");
        lines.add("Garbage collector: " + panel.jvmRuntimeProfile.gc.label + "; string dedupe: " + (panel.jvmRuntimeProfile.stringDeduplication ? "ON" : "OFF") + ".");
        lines.add("Java2D pipeline: " + panel.jvmRuntimeProfile.pipelineLabel() + "; transparent blit: " + (panel.jvmRuntimeProfile.transparentAcceleration ? "ON" : "OFF") + "; noaa: " + (panel.jvmRuntimeProfile.disableVectorAntialiasing ? "ON" : "OFF") + ".");
        lines.add("Compiled restart flags: " + String.join(" ", panel.jvmRuntimeProfile.buildJvmArgs()));
        lines.add("Launcher/main menu, graphical client, thin network client, single-player combined, and headless server are separate profiles of the same program.");
        lines.add("Single-player combined profiles assume local client, local host, and authoritative server lane share one JVM process.");
        lines.add("The multiplayer server remains headless/status-initializer only until the server/network layer is fully examined and opened.");
        lines.add(panel.jvmRuntimeNotice);
        return lines;
    }

    static List<String> accessibilityLines(GamePanel panel) {
        ArrayList<String> lines = new ArrayList<>(AccessibilityCompatibilityAuthority.optionLines(panel.options));
        lines.add("Additional diegetic palettes now include Protan Ember, Deutan Steel, Tritan Brass, and Legibility Slate.");
        lines.add("Visual-check path: enable the performance overlay, activate a color-vision correction, and watch frame pacing for the extra backbuffer pass.");
        return lines;
    }

    static List<String> qolLines(GamePanel panel) {
        ArrayList<String> lines = new ArrayList<>(GameplayQualityOfLifeAuthority.optionLines(panel.options));
        lines.add("These switches are preferences and integration contracts: supported systems should consume them instead of reintroducing hard-coded hostile defaults.");
        return lines;
    }

    static String subtitle(int optionsTab) {
        if (optionsTab == 0) return "Display mode, detected resolution, text density, and interface scale";
        if (optionsTab == 1) return "Text size, text crispness, interface scale, and hover-help density";
        if (optionsTab == 2) return "Sound channels and volumes";
        if (optionsTab == 3) return "Controls and command bindings";
        if (optionsTab == 4) return "Graphics rendering, frame pacing, art quality, motion, and color treatment";
        if (optionsTab == 5) return "RESTART REQUIRED: JVM heap, GC, Java2D pipeline, client/server/thin-client/single-player profiles";
        if (optionsTab == 6) return "Accessibility compatibility, color vision correction, readable text, narration hooks, and reduced motion";
        return "Quality-of-life defaults for storage, construction, logistics, item safety, production, market, and notification friction";
    }
}
