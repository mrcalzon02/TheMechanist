package mechanist;

import mechanist.assets.AssetManager;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class OptionsScreenPainter {
    private OptionsScreenPainter() {}

    private static final String[] TAB_LABELS = {"Display", "Text/UI", "Audio", "Controls", "Graphics", "JVM", "Access", "QOL"};

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
        int h = Math.max(180, Math.min(292, layout.panelH / 3));
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
        g.setColor(AccessibilityVisualOptionsRuntime.optionColor(panel, GameOptions.BACKGROUND));
        g.fillRect(0, 0, layout.width, layout.height);
        panel.drawSlicedFrame(g, 20, 20, layout.width - 40, layout.height - 40, "outer");
        panel.drawPanelBox(g, layout.panelX, layout.panelY, layout.panelW, layout.panelH, "OPTIONS");
        g.setFont(panel.smallFont.deriveFont(Font.BOLD, Math.max(10f, Math.min(14f, layout.height / 64f))));
        g.setColor(AccessibilityVisualOptionsRuntime.optionColor(panel, GameOptions.TEXT_TITLE));
        panel.drawUiTextLine(g, "OPTIONS", layout.panelX + 18, layout.panelY + 26);
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
        java.awt.Rectangle controls = controlsBox(layout);
        panel.buttons.clear();
        buildOptionButtons(panel, controls);

        java.awt.Rectangle info = infoBox(layout);
        g.setFont(panel.smallFont);
        List<String> lines = linesForTab(panel);
        int h = panel.optionsTab == 4 ? Math.max(96, info.height - 72) : info.height;
        panel.drawTextPanel(g, info.x, info.y, info.width, h, lines, false);
        if (panel.optionsTab == 4) paintColorSwatches(panel, g, info);
        drawOptionButtons(panel, g, false);
    }

    static void paintColorSwatches(GamePanel panel, Graphics2D g, java.awt.Rectangle info) {
        int swatchY = info.y + Math.max(96, info.height - 66);
        int sx = info.x + 18;
        for (int i = 0; i < GameOptions.COLOR_KEYS.length; i++) {
            int px = sx + i * Math.max(128, (info.width - 36) / GameOptions.COLOR_KEYS.length);
            g.setColor(new Color(panel.options.colorValue(i)));
            g.fillRect(px, swatchY, 28, 18);
            g.setColor(i == panel.options.colorTarget ? AccessibilityVisualOptionsRuntime.optionColor(panel, GameOptions.TEXT_HIGHLIGHT) : AccessibilityVisualOptionsRuntime.optionColor(panel, GameOptions.TEXT_MAIN));
            g.drawRect(px, swatchY, 28, 18);
            g.setFont(panel.smallFont);
            for (String line : TextSurfaceApi.wrap(GameOptions.COLOR_KEYS[i], 11)) {
                panel.drawUiTextLine(g, line, px + 34, swatchY + 13);
                break;
            }
        }
    }

    static void paintGraphicsDropdownPopup(GamePanel panel, Graphics2D g) {
        if (panel.screen != GamePanel.Screen.OPTIONS || panel.graphicsDropdown < 0 || (panel.optionsTab != 0 && panel.optionsTab != 1 && panel.optionsTab != 4 && panel.optionsTab != 6)) return;
        java.awt.Rectangle r = panel.graphicsDropdownOuterRect();
        if (r.width <= 0 || r.height <= 0) return;
        g.setColor(new Color(0, 0, 0, 218));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        panel.drawSlicedFrame(g, r.x, r.y, r.width, r.height, "inner");
        panel.stampUiFrameId(g, "F", "graphics-dropdown-popup", r.x, r.y, r.width, r.height);
        drawOptionButtons(panel, g, true);
    }

    static void buildOptionButtons(GamePanel panel, Rectangle controls) {
        int gap = Math.max(5, Math.min(8, controls.height / 28));
        int tabH = Math.max(24, Math.min(30, controls.height / 7));
        int tabY = controls.y + gap;
        int tabW = Math.max(44, (controls.width - gap * (TAB_LABELS.length - 1)) / TAB_LABELS.length);
        for (int i = 0; i < TAB_LABELS.length; i++) {
            final int tab = i;
            int x = controls.x + i * (tabW + gap);
            int w = (i == TAB_LABELS.length - 1) ? Math.max(44, controls.x + controls.width - x) : tabW;
            panel.buttons.add(new ButtonBox(TAB_LABELS[i], x, tabY, w, tabH, "Open the " + TAB_LABELS[i] + " options panel.", () -> selectOptionsTab(panel, tab)));
        }

        ArrayList<OptionCommand> commands = commandsForTab(panel);
        int commandTop = tabY + tabH + gap + 4;
        int commandBottom = controls.y + controls.height - gap;
        int availableH = Math.max(1, commandBottom - commandTop);
        int cols = controls.width < 680 ? 2 : (commands.size() > 9 ? 4 : 3);
        int rows = Math.max(1, (commands.size() + cols - 1) / cols);
        int rowH = Math.max(24, Math.min(32, (availableH - gap * Math.max(0, rows - 1)) / rows));
        int commandW = Math.max(82, (controls.width - gap * (cols - 1)) / cols);
        for (int i = 0; i < commands.size(); i++) {
            OptionCommand command = commands.get(i);
            int col = i % cols;
            int row = i / cols;
            int x = controls.x + col * (commandW + gap);
            int y = commandTop + row * (rowH + gap);
            int w = col == cols - 1 ? Math.max(82, controls.x + controls.width - x) : commandW;
            panel.buttons.add(new ButtonBox(command.label, x, y, w, rowH, command.tip, command.action));
        }

        int firstDropdown = panel.buttons.size();
        GraphicsDropdownOptionsRuntime.addGraphicsDropdownButtons(panel, 0, 0, 0, 0);
        clampSelectedButton(panel, firstDropdown);
    }

    static void drawOptionButtons(GamePanel panel, Graphics2D g, boolean dropdownOnly) {
        Font buttonFont = panel.uiFont.deriveFont(Font.BOLD, Math.max(9f, Math.min(12.5f, panel.getHeight() / 62f)));
        for (int i = 0; i < panel.buttons.size(); i++) {
            ButtonBox button = panel.buttons.get(i);
            boolean dropdown = GraphicsDropdownOptionsRuntime.isGraphicsDropdownButton(panel, button);
            if (dropdownOnly != dropdown) continue;
            panel.ensureButtonSystemIcon(button);
            boolean selected = i == panel.selectedButton || (button != null && button.contains(panel.mouseX, panel.mouseY));
            button.draw(g, buttonFont, selected, null, panel.options);
        }
    }

    private static void clampSelectedButton(GamePanel panel, int firstDropdown) {
        if (panel.buttons.isEmpty()) {
            panel.selectedButton = 0;
            return;
        }
        if (panel.selectedButton < 0 || panel.selectedButton >= panel.buttons.size()) panel.selectedButton = 0;
        if (panel.graphicsDropdown >= 0) {
            boolean selectedDropdown = GraphicsDropdownOptionsRuntime.isGraphicsDropdownButton(panel, panel.buttons.get(panel.selectedButton));
            if (!selectedDropdown && firstDropdown < panel.buttons.size()) panel.selectedButton = firstDropdown;
        }
    }

    private static void selectOptionsTab(GamePanel panel, int tab) {
        panel.optionsTab = Math.max(0, Math.min(TAB_LABELS.length - 1, tab));
        panel.graphicsDropdown = -1;
        panel.selectedButton = panel.optionsTab;
        panel.sounds.play("tab", panel.options);
        panel.repaint();
    }

    private static ArrayList<OptionCommand> commandsForTab(GamePanel panel) {
        if (panel.optionsTab == 0) return displayCommands(panel);
        if (panel.optionsTab == 1) return textCommands(panel);
        if (panel.optionsTab == 2) return audioCommands(panel);
        if (panel.optionsTab == 3) return controlsCommands(panel);
        if (panel.optionsTab == 4) return graphicsCommands(panel);
        if (panel.optionsTab == 5) return jvmCommands(panel);
        if (panel.optionsTab == 6) return accessibilityCommands(panel);
        return qolCommands(panel);
    }

    private static ArrayList<OptionCommand> displayCommands(GamePanel panel) {
        ArrayList<OptionCommand> out = new ArrayList<>();
        out.add(cmd("Mode: " + panel.options.windowModeLabel(), "Select windowed, borderless, or exclusive fullscreen.", () -> GraphicsDropdownOptionsRuntime.toggleGraphicsDropdown(panel, 0)));
        out.add(cmd("Resolution: " + panel.options.resolutionLabel(), "Select a detected or safe display mode.", () -> GraphicsDropdownOptionsRuntime.toggleGraphicsDropdown(panel, 1)));
        out.add(cmd("Apply Window", "Apply the pending window mode and resolution.", () -> GraphicsDropdownOptionsRuntime.applyWindowMode(panel)));
        out.add(cmd("Screensaver " + onOff(panel.options.screenSaver), "Choose whether the idle screensaver is enabled.", () -> radio(panel, "Screensaver", panel.options.screenSaver, () -> {
            panel.options.screenSaver = !panel.options.screenSaver;
            saveFlag(panel, "Screensaver", panel.options.screenSaver);
        })));
        out.add(backCommand(panel));
        return out;
    }

    private static ArrayList<OptionCommand> textCommands(GamePanel panel) {
        ArrayList<OptionCommand> out = new ArrayList<>();
        out.add(cmd("Text Scale: " + panel.options.fontScale + "%", "Adjust menu and body text with a slider.",
                () -> SwingOptionsEditorAuthority.editInt(panel, "Text Scale", panel.options.fontScale, 50, 200, 5, "%",
                        value -> DisplayScaleOptionsRuntime.changeFontScale(panel, (value - panel.options.fontScale) / 5))));
        out.add(cmd("UI Scale: " + panel.options.uiScale + "%", "Adjust interface chrome with a slider.",
                () -> SwingOptionsEditorAuthority.editInt(panel, "UI Scale", panel.options.uiScale, 50, 200, 5, "%",
                        value -> DisplayScaleOptionsRuntime.changeUiScale(panel, (value - panel.options.uiScale) / 5))));
        out.add(cmd("Crispness: " + panel.options.renderQualityLabel(), "Select the Java2D text and render hint profile.", () -> GraphicsDropdownOptionsRuntime.toggleGraphicsDropdown(panel, 5)));
        out.add(cmd("Hover Help " + onOff(panel.options.hoverHelp), "Choose whether floating hover help is enabled.", () -> radio(panel, "Hover Help", panel.options.hoverHelp, () -> {
            panel.options.hoverHelp = !panel.options.hoverHelp;
            saveFlag(panel, "Hover help", panel.options.hoverHelp);
        })));
        out.add(backCommand(panel));
        return out;
    }

    private static ArrayList<OptionCommand> audioCommands(GamePanel panel) {
        ArrayList<OptionCommand> out = new ArrayList<>();
        out.add(cmd("SFX " + onOff(panel.options.soundEnabled), "Choose whether sound effects are enabled.", () -> radio(panel, "Sound Effects", panel.options.soundEnabled, () -> {
            panel.options.soundEnabled = !panel.options.soundEnabled;
            panel.options.save();
            panel.logEvent("Sound effects " + onOff(panel.options.soundEnabled) + ".");
            panel.repaint();
        })));
        out.add(cmd("SFX Volume: " + panel.options.sfxVolume + "%", "Adjust sound effects volume with a slider.",
                () -> SwingOptionsEditorAuthority.editInt(panel, "Sound Effects Volume", panel.options.sfxVolume, 0, 100, 5, "%",
                        value -> panel.logEvent(OptionsBoundaryAuthority.changeSfxVolume(panel.options, value - panel.options.sfxVolume)))));
        out.add(cmd("Music " + onOff(panel.options.musicEnabled), "Choose whether dynamic music is enabled.", () -> radio(panel, "Dynamic Music", panel.options.musicEnabled, () -> {
            panel.options.musicEnabled = !panel.options.musicEnabled;
            panel.options.save();
            if (panel.options.musicEnabled) panel.sounds.requestMusic("MAIN_MENU", panel.options);
            else panel.sounds.stopMusic("music disabled from options");
            panel.logEvent("Music " + onOff(panel.options.musicEnabled) + ".");
            panel.repaint();
        })));
        out.add(cmd("Music Volume: " + panel.options.musicVolume + "%", "Adjust music volume with a slider.",
                () -> SwingOptionsEditorAuthority.editInt(panel, "Music Volume", panel.options.musicVolume, 0, 100, 5, "%",
                        value -> panel.logEvent(OptionsBoundaryAuthority.changeMusicVolume(panel.options, value - panel.options.musicVolume)))));
        out.add(cmd("Voice " + onOff(panel.options.conversationSound), "Choose whether voice and conversation sounds are enabled.", () -> radio(panel, "Voice and Conversation Audio", panel.options.conversationSound, () -> {
            panel.options.conversationSound = !panel.options.conversationSound;
            saveFlag(panel, "Voice and conversation audio", panel.options.conversationSound);
        })));
        out.add(cmd("Voice Volume: " + panel.options.conversationVolume + "%", "Adjust voice volume with a slider.",
                () -> SwingOptionsEditorAuthority.editInt(panel, "Voice Volume", panel.options.conversationVolume, 0, 100, 5, "%",
                        value -> panel.logEvent(OptionsBoundaryAuthority.changeConversationVolume(panel.options, value - panel.options.conversationVolume)))));
        out.add(cmd("Test SFX", "Play the menu button sound through the live audio bridge.", () -> {
            panel.sounds.play("button", panel.options);
            panel.logEvent("SFX test requested.");
            panel.repaint();
        }));
        out.add(cmd("Menu Music", "Request the main menu music playlist.", () -> {
            panel.sounds.requestMusic("MAIN_MENU", panel.options);
            panel.logEvent("Main menu music requested.");
            panel.repaint();
        }));
        out.add(backCommand(panel));
        return out;
    }

    private static ArrayList<OptionCommand> controlsCommands(GamePanel panel) {
        ArrayList<OptionCommand> out = new ArrayList<>();
        out.add(cmd("Keyboard/Mouse", "Show keyboard and mouse bindings.", () -> selectControlsTab(panel, 0)));
        out.add(cmd("Xbox", "Show Xbox controller prompts.", () -> selectControlsTab(panel, 1)));
        out.add(cmd("PlayStation", "Show PlayStation controller prompts.", () -> selectControlsTab(panel, 2)));
        out.add(cmd("Steam Deck", "Show Steam Deck controller prompts.", () -> selectControlsTab(panel, 3)));
        out.add(cmd("Generic Pad", "Show generic controller prompts.", () -> selectControlsTab(panel, 4)));
        out.add(cmd("Pad Status", "Log the current controller runtime status.", () -> {
            panel.logEvent(panel.gamepadInputEngine == null ? "Controller runtime is not started." : panel.gamepadInputEngine.status());
            panel.repaint();
        }));
        out.add(backCommand(panel));
        return out;
    }

    private static ArrayList<OptionCommand> graphicsCommands(GamePanel panel) {
        ArrayList<OptionCommand> out = new ArrayList<>();
        out.add(cmd("Downscale: " + panel.options.downscaleLabel(), "Select internal render resolution scaling.", () -> GraphicsDropdownOptionsRuntime.toggleGraphicsDropdown(panel, 3)));
        out.add(cmd("FPS: " + panel.options.targetFpsLabel(), "Select target frame pacing.", () -> GraphicsDropdownOptionsRuntime.toggleGraphicsDropdown(panel, 4)));
        out.add(cmd("Quality: " + panel.options.renderQualityLabel(), "Select render quality.", () -> GraphicsDropdownOptionsRuntime.toggleGraphicsDropdown(panel, 5)));
        out.add(cmd("Lighting: " + panel.options.lightingFxLabel(), "Cycle visual lighting effects.", () -> DisplayPerformanceOptionsRuntime.cycleLightingFx(panel)));
        out.add(boolCmd(panel, "Frame Limit", panel.options.isFrameLimited, "Choose whether frame pacing is limited.", () -> DisplayPerformanceOptionsRuntime.toggleFrameLimiter(panel)));
        out.add(boolCmd(panel, "Reduced Motion", panel.options.reducedMotion, "Choose reduced-motion behavior.", () -> DisplayPerformanceOptionsRuntime.toggleReducedMotion(panel)));
        out.add(boolCmd(panel, "Diagnostics", panel.options.diagnosticsOverlay, "Choose whether the F3 performance overlay is enabled.", () -> AccessibilityVisualOptionsRuntime.togglePerformanceDiagnostics(panel)));
        out.add(cmd("Stress Test", "Toggle the render stress test overlay.", () -> DisplayPerformanceOptionsRuntime.toggleRenderStressTest(panel)));
        out.add(cmd("Viewport Tile: " + panel.options.mapTileSizeLabel(), "Cycle map tile display footprint.", () -> ViewportAssetOptionsRuntime.cycleMapTileSize(panel)));
        out.add(cmd("World Zoom: " + panel.options.worldZoomPercent() + "%", "Adjust tactical viewport zoom with a slider.",
                () -> SwingOptionsEditorAuthority.editInt(panel, "World Zoom", panel.options.worldZoomIndex, 0,
                        GameOptions.WORLD_ZOOM_LABELS.length - 1, 1, "", value -> {
                            int delta = value - panel.options.worldZoomIndex;
                            if (delta != 0) ViewportAssetOptionsRuntime.changeWorldZoom(panel, delta, "Options slider");
                        })));
        out.add(boolCmd(panel, "Doom Mode", panel.options.doomModeEnabled, "Choose whether the first-person renderer is enabled.", () -> DoomQualityOfLifeOptionsRuntime.requestDoomModeToggle(panel)));
        out.add(cmd("Doom FOV: " + panel.options.doomModeFovDegrees, "Adjust first-person field of view with a slider.",
                () -> SwingOptionsEditorAuthority.editInt(panel, "Doom Mode Field of View", panel.options.doomModeFovDegrees,
                        50, 120, 5, " degrees", value -> DoomQualityOfLifeOptionsRuntime.changeDoomFov(panel,
                                value - panel.options.doomModeFovDegrees))));
        out.add(cmd("Doom Fog: " + panel.options.doomFogModeLabel(), "Cycle first-person fog distance mode.", () -> DoomQualityOfLifeOptionsRuntime.cycleDoomFogMode(panel)));
        out.add(cmd("Tile Icons " + onOff(panel.options.tileIconRendering), "Choose whether compiled tile icons are enabled.", () -> radio(panel, "Tile Icon Rendering", panel.options.tileIconRendering, () -> {
            panel.options.tileIconRendering = !panel.options.tileIconRendering;
            saveFlag(panel, "Tile icon rendering", panel.options.tileIconRendering);
        })));
        out.add(cmd("Portraits " + onOff(panel.options.importedPortraits), "Choose whether imported portrait sheets are loaded.", () -> radio(panel, "Imported Portraits", panel.options.importedPortraits, () -> {
            panel.options.importedPortraits = !panel.options.importedPortraits;
            panel.options.save();
            panel.images.reloadArtQuality(panel.options);
            panel.logEvent("Imported portrait sheets " + onOff(panel.options.importedPortraits) + ".");
            panel.repaint();
        })));
        out.add(cmd("Textures: " + panel.options.artQualityResolutionLabel(), "Cycle compiled texture package size.", () -> ViewportAssetOptionsRuntime.cycleArtQuality(panel)));
        out.add(cmd("Payload Root", "Choose an external generated-art payload root.", () -> ViewportAssetOptionsRuntime.chooseGeneratedAssetPayloadRoot(panel)));
        out.add(cmd("Clear Payload", "Clear the external generated-art payload root.", () -> ViewportAssetOptionsRuntime.clearGeneratedAssetPayloadRoot(panel)));
        out.add(cmd("Palette: " + GameOptions.PALETTE_NAMES[panel.options.colorPreset], "Select a color palette.", () -> GraphicsDropdownOptionsRuntime.toggleGraphicsDropdown(panel, 2)));
        out.add(cmd("Color Key: " + panel.options.colorTargetLabel(), "Cycle the color being edited.", () -> AccessibilityVisualOptionsRuntime.cycleColorTarget(panel)));
        out.add(cmd("Choose Color...", "Open the Java color chooser for the selected text or panel color.",
                () -> SwingOptionsEditorAuthority.editColor(panel)));
        out.add(backCommand(panel));
        return out;
    }

    private static ArrayList<OptionCommand> jvmCommands(GamePanel panel) {
        ArrayList<OptionCommand> out = new ArrayList<>();
        out.add(cmd("Runtime: " + panel.jvmRuntimeProfile.targetLabel(), "Cycle client/server/thin-client runtime profile.", () -> JvmRuntimeOptionsSubsystem.cycleJvmRuntimeProfile(panel)));
        out.add(cmd("Heap: " + panel.jvmRuntimeProfile.maxRamMb + " MB", "Adjust the saved JVM maximum heap with a slider.",
                () -> SwingOptionsEditorAuthority.editInt(panel, "JVM Maximum Heap", panel.jvmRuntimeProfile.maxRamMb,
                        1024, 16384, 256, " MB", value -> JvmRuntimeOptionsSubsystem.changeJvmMemory(panel,
                                value - panel.jvmRuntimeProfile.maxRamMb))));
        out.add(cmd("GC: " + panel.jvmRuntimeProfile.gc.label, "Cycle garbage collector profile.", () -> JvmRuntimeOptionsSubsystem.cycleJvmGarbageCollector(panel)));
        out.add(cmd("Java2D: " + panel.jvmRuntimeProfile.pipelineLabel(), "Cycle Java2D pipeline profile.", () -> JvmRuntimeOptionsSubsystem.cycleJvmPipelineProfile(panel)));
        out.add(boolCmd(panel, "String Dedup", panel.jvmRuntimeProfile.stringDeduplication, "Choose saved string deduplication.", () -> JvmRuntimeOptionsSubsystem.toggleJvmStringDeduplication(panel)));
        out.add(boolCmd(panel, "Trans Blit", panel.jvmRuntimeProfile.transparentAcceleration, "Choose transparent blit acceleration.", () -> JvmRuntimeOptionsSubsystem.toggleJvmTransparentAcceleration(panel)));
        out.add(boolCmd(panel, "No AA", panel.jvmRuntimeProfile.disableVectorAntialiasing, "Choose vector antialias suppression.", () -> JvmRuntimeOptionsSubsystem.toggleJvmNoAa(panel)));
        out.add(cmd("Accept + Restart", "Restart the client with the saved JVM profile.", () -> JvmRuntimeOptionsSubsystem.acceptJvmSettingsAndRestart(panel)));
        out.add(backCommand(panel));
        return out;
    }

    private static ArrayList<OptionCommand> accessibilityCommands(GamePanel panel) {
        ArrayList<OptionCommand> out = new ArrayList<>();
        out.add(cmd("CVD: " + AccessibilityCompatibilityAuthority.cvdLabel(panel.options.cvdModeIndex), "Cycle color vision correction.", () -> AccessibilityVisualOptionsRuntime.cycleCvdMode(panel)));
        out.add(boolCmd(panel, "High Contrast", panel.options.highContrastText, "Choose high contrast text containers.", () -> AccessibilityVisualOptionsRuntime.toggleHighContrastText(panel)));
        out.add(boolCmd(panel, "Instant Text", panel.options.instantDialogueText, "Choose instant conversation text.", () -> AccessibilityVisualOptionsRuntime.toggleInstantDialogueText(panel)));
        out.add(cmd("Screen Shake: " + panel.options.screenShakePercent + "%", "Adjust screen shake with a slider.",
                () -> SwingOptionsEditorAuthority.editInt(panel, "Screen Shake", panel.options.screenShakePercent,
                        0, 100, 10, "%", value -> AccessibilityVisualOptionsRuntime.adjustScreenShake(panel,
                                value - panel.options.screenShakePercent))));
        out.add(cmd("Narrate Screen", "Push a screen narration event.", () -> AccessibilityVisualOptionsRuntime.pushCurrentScreenNarration(panel)));
        out.add(boolCmd(panel, "Subtitles", panel.options.subtitlesEnabled, "Choose subtitle display.", () -> applyQoL(panel, GameplayQualityOfLifeAuthority.toggleSubtitles(panel.options))));
        out.add(boolCmd(panel, "Reduced Motion", panel.options.reducedMotion, "Choose reduced-motion behavior.", () -> DisplayPerformanceOptionsRuntime.toggleReducedMotion(panel)));
        out.add(cmd("Palette", "Select an accessibility-friendly palette.", () -> GraphicsDropdownOptionsRuntime.toggleGraphicsDropdown(panel, 2)));
        out.add(cmd("Color Key", "Cycle the color being edited.", () -> AccessibilityVisualOptionsRuntime.cycleColorTarget(panel)));
        out.add(cmd("Choose Color...", "Open the Java color chooser for the selected text or panel color.",
                () -> SwingOptionsEditorAuthority.editColor(panel)));
        out.add(backCommand(panel));
        return out;
    }

    private static ArrayList<OptionCommand> qolCommands(GamePanel panel) {
        ArrayList<OptionCommand> out = new ArrayList<>();
        out.add(boolCmd(panel, "Skip Logos", panel.options.skipRepeatLogoSplashes, "Choose repeat logo skipping.", () -> applyQoL(panel, GameplayQualityOfLifeAuthority.toggleSkipSplashes(panel.options))));
        out.add(boolCmd(panel, "Auto Loot", panel.options.autoLootEnabled, "Choose auto-loot preference.", () -> applyQoL(panel, GameplayQualityOfLifeAuthority.toggleAutoLoot(panel.options))));
        out.add(boolCmd(panel, "Smart Storage", panel.options.smartStorageFilters, "Choose smart storage filters.", () -> applyQoL(panel, GameplayQualityOfLifeAuthority.toggleSmartStorage(panel.options))));
        out.add(boolCmd(panel, "Proxy Craft", panel.options.proxyCraftingFromLinkedStorage, "Choose crafting from linked storage.", () -> applyQoL(panel, GameplayQualityOfLifeAuthority.toggleProxyCrafting(panel.options))));
        out.add(boolCmd(panel, "Output Route", panel.options.machineOutputAutoRouting, "Choose machine output auto-routing.", () -> {
            panel.options.machineOutputAutoRouting = !panel.options.machineOutputAutoRouting;
            saveFlag(panel, "Machine output auto-routing", panel.options.machineOutputAutoRouting);
        }));
        out.add(boolCmd(panel, "Build Repeat", panel.options.holdToRepeatConstruction, "Choose hold-to-repeat construction.", () -> applyQoL(panel, GameplayQualityOfLifeAuthority.toggleHoldRepeatBuild(panel.options))));
        out.add(boolCmd(panel, "Omni Ghost", panel.options.omniDirectionalGhostBuild, "Choose omni-directional ghost build.", () -> applyQoL(panel, GameplayQualityOfLifeAuthority.toggleOmniGhostBuild(panel.options))));
        out.add(boolCmd(panel, "Prod Warnings", panel.options.productionBlockerWarnings, "Choose production blocker warnings.", () -> applyQoL(panel, GameplayQualityOfLifeAuthority.toggleProductionWarnings(panel.options))));
        out.add(boolCmd(panel, "Scarcity", panel.options.globalScarcityWarnings, "Choose global scarcity alerts.", () -> {
            panel.options.globalScarcityWarnings = !panel.options.globalScarcityWarnings;
            saveFlag(panel, "Global scarcity alerts", panel.options.globalScarcityWarnings);
        }));
        out.add(boolCmd(panel, "Recipe Pins", panel.options.recipeHudPinning, "Choose recipe HUD pinning.", () -> {
            panel.options.recipeHudPinning = !panel.options.recipeHudPinning;
            saveFlag(panel, "Recipe HUD pinning", panel.options.recipeHudPinning);
        }));
        out.add(boolCmd(panel, "Favored Safe", panel.options.favoredItemProtection, "Choose favored-item protection.", () -> applyQoL(panel, GameplayQualityOfLifeAuthority.toggleFavoredProtection(panel.options))));
        out.add(boolCmd(panel, "Quality Warn", panel.options.lowQualityPickupWarnings, "Choose low-quality pickup warnings.", () -> {
            panel.options.lowQualityPickupWarnings = !panel.options.lowQualityPickupWarnings;
            saveFlag(panel, "Low-quality pickup warnings", panel.options.lowQualityPickupWarnings);
        }));
        out.add(boolCmd(panel, "Mixed Stacks", panel.options.noMixedQualityStacking, "Choose mixed-quality stack prevention.", () -> {
            panel.options.noMixedQualityStacking = !panel.options.noMixedQualityStacking;
            saveFlag(panel, "Mixed-quality stack prevention", panel.options.noMixedQualityStacking);
        }));
        out.add(cmd("Safety: " + GameplayQualityOfLifeAuthority.protectionLabel(panel.options.itemSafetyProfileIndex), "Cycle item safety profile.", () -> applyQoL(panel, GameplayQualityOfLifeAuthority.cycleItemSafetyProfile(panel.options))));
        out.add(boolCmd(panel, "Market Alerts", panel.options.economicDisruptionAlerts, "Choose market disruption alerts.", () -> applyQoL(panel, GameplayQualityOfLifeAuthority.toggleMarketAlerts(panel.options))));
        out.add(boolCmd(panel, "Price Hints", panel.options.localGlobalPriceHints, "Choose local/global price hints.", () -> {
            panel.options.localGlobalPriceHints = !panel.options.localGlobalPriceHints;
            saveFlag(panel, "Local/global price hints", panel.options.localGlobalPriceHints);
        }));
        out.add(cmd("Solo Time: " + panel.options.singlePlayerTickModeLabel(), "Cycle single-player world simulation between turn-locked and passive ticking.", () -> applyQoL(panel, GameplayQualityOfLifeAuthority.cycleSinglePlayerTickMode(panel.options))));
        out.add(boolCmd(panel, "Death Alerts", panel.options.namedDeathAlerts, "Choose named death alerts.", () -> {
            panel.options.namedDeathAlerts = !panel.options.namedDeathAlerts;
            saveFlag(panel, "Named death alerts", panel.options.namedDeathAlerts);
        }));
        out.add(boolCmd(panel, "Doom Mode", panel.options.doomModeEnabled, "Choose whether the first-person renderer is enabled.", () -> DoomQualityOfLifeOptionsRuntime.requestDoomModeToggle(panel)));
        out.add(cmd("Doom FOV: " + panel.options.doomModeFovDegrees, "Adjust first-person field of view with a slider.",
                () -> SwingOptionsEditorAuthority.editInt(panel, "Doom Mode Field of View", panel.options.doomModeFovDegrees,
                        50, 120, 5, " degrees", value -> DoomQualityOfLifeOptionsRuntime.changeDoomFov(panel,
                                value - panel.options.doomModeFovDegrees))));
        out.add(cmd("Doom Fog: " + panel.options.doomFogModeLabel(), "Cycle first-person fog distance mode.", () -> DoomQualityOfLifeOptionsRuntime.cycleDoomFogMode(panel)));
        out.add(backCommand(panel));
        return out;
    }

    private static void selectControlsTab(GamePanel panel, int tab) {
        panel.controlsTab = Math.max(0, Math.min(4, tab));
        panel.logEvent(ControlReferenceTextSubsystem.controlProfileTitle(panel.controlsTab));
        panel.sounds.play("tab", panel.options);
        panel.repaint();
    }

    private static void radio(GamePanel panel, String title, boolean current, Runnable toggle) {
        SwingOptionsEditorAuthority.editBoolean(panel, title, current, toggle);
    }

    private static OptionCommand boolCmd(GamePanel panel, String label, boolean current, String tip, Runnable toggle) {
        return cmd(label + " " + onOff(current), tip,
                () -> SwingOptionsEditorAuthority.editBoolean(panel, label, current, toggle));
    }

    private static void applyQoL(GamePanel panel, String message) {
        panel.logEvent(message);
        DebugLog.audit("GAMEPLAY_QOL_OPTIONS", GameplayQualityOfLifeAuthority.auditSummary(panel.options));
        panel.repaint();
    }

    private static void saveFlag(GamePanel panel, String label, boolean enabled) {
        panel.options.save();
        panel.logEvent(label + " " + onOff(enabled) + ".");
        panel.repaint();
    }

    private static OptionCommand backCommand(GamePanel panel) {
        return cmd("Back", "Return to the previous menu.", panel::closeOptionsScreen);
    }

    private static OptionCommand cmd(String label, String tip, Runnable action) {
        return new OptionCommand(label, tip, action);
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private static final class OptionCommand {
        final String label;
        final String tip;
        final Runnable action;

        OptionCommand(String label, String tip, Runnable action) {
            this.label = label;
            this.tip = tip;
            this.action = action;
        }
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
        lines.add("STARTUP SFX: OFF");
        lines.add("Music has a dedicated channel even before music assets are imported. Defaults begin around 80% so the first audible implementation is not a screaming logic Engine.");
        return lines;
    }

    static List<String> controlsLines(GamePanel panel) {
        ArrayList<String> lines = new ArrayList<>(ControlReferenceTextSubsystem.controlsReferenceLines(panel));
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
        lines.add("Texture Package: " + panel.options.artQualityResolutionLabel() + " compiled assets / tier " + panel.options.artQualityFolder());
        lines.add("Generated Art Payload: " + panel.options.generatedAssetPayloadRootLabel());
        try {
            var runtime = AssetManager.generatedAssetRuntime();
            lines.add("Generated Runtime: " + (runtime.runtimeManifestPresent() && runtime.tierManifestPresent() ? "ready" : "partial") + " / payload roots " + runtime.generatedPayloadRoots().size());
        } catch (Throwable ignored) {
        lines.add("Generated Runtime: unavailable");
        }
        lines.add("Viewport Tile Size: " + panel.options.mapTileSizeLabel() + " / " + panel.options.mapTilePixelSize() + "px screen footprint before GUI scale");
        lines.add("Viewport Zoom: " + panel.options.worldZoomLabel() + " / +/- or mouse wheel adjust active 2D world views.");
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
