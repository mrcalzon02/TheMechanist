package mechanist;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.KeyEventDispatcher;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Recovered staged start-flow controller.
 *
 * This restores the intended flow that existed conceptually before the temporary
 * two-in-one bridge screen:
 *
 * Main Menu -> World Picker -> World Generation Options -> Character Creation -> Run.
 * Existing .mechworld definitions go directly from World Picker to Character Creation.
 */
final class WorldStartFlowAuthority {
    static final String VERSION = "world-start-flow-authority-0.9.10la";

    enum Stage { CLOSED, WORLD_PICKER, WORLD_GENERATION, CHARACTER_CREATION }

    private WorldStartFlowAuthority() {}

    static void install(GamePanel panel) {
        if (panel == null) return;
        if (panel.getClientProperty(WorldStartFlowOverlay.CLIENT_PROPERTY) instanceof WorldStartFlowOverlay) return;
        WorldStartFlowOverlay overlay = new WorldStartFlowOverlay(panel);
        panel.putClientProperty(WorldStartFlowOverlay.CLIENT_PROPERTY, overlay);
        panel.setLayout(null);
        panel.add(overlay);
        overlay.setBounds(0, 0, Math.max(1, panel.getWidth()), Math.max(1, panel.getHeight()));
        panel.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                overlay.setBounds(0, 0, Math.max(1, panel.getWidth()), Math.max(1, panel.getHeight()));
            }
        });
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(overlay);
        DebugLog.audit("WORLD_START_FLOW", "installed " + VERSION);
    }

    static void openWorldPicker(GamePanel panel, String reason) {
        WorldStartFlowOverlay overlay = overlay(panel);
        if (overlay != null) overlay.openWorldPicker(reason);
    }

    static boolean isActive(GamePanel panel) {
        WorldStartFlowOverlay overlay = overlay(panel);
        return overlay != null && overlay.stage != Stage.CLOSED;
    }

    private static WorldStartFlowOverlay overlay(GamePanel panel) {
        Object value = panel == null ? null : panel.getClientProperty(WorldStartFlowOverlay.CLIENT_PROPERTY);
        return value instanceof WorldStartFlowOverlay o ? o : null;
    }

    static final class WorldStartFlowOverlay extends JComponent implements KeyEventDispatcher {
        static final String CLIENT_PROPERTY = "mechanist.worldStartFlowOverlay";

        final GamePanel panel;
        Stage stage = Stage.CLOSED;
        ArrayList<WorldSaveInfo> worlds = new ArrayList<>();
        int worldIndex;
        int worldGenerationOption;
        int characterOption;
        String status = "";
        boolean nameEditing;
        boolean seedEditing;
        String seedDraft = "";
        String pendingWorldDeleteKey = "";
        WorldAtlas previewAtlas;
        World previewWorld;
        String previewKey = "";
        String previewStatus = "";

        WorldStartFlowOverlay(GamePanel panel) {
            this.panel = panel;
            setOpaque(false);
            setVisible(false);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { handleMouse(e); }
                @Override public void mousePressed(MouseEvent e) { if (stage != Stage.CLOSED) e.consume(); }
                @Override public void mouseReleased(MouseEvent e) { if (stage != Stage.CLOSED) e.consume(); }
            });
        }

        void openWorldPicker(String reason) {
            worlds = WorldSaveInfo.listExistingWorlds();
            worldIndex = Math.max(0, Math.min(worldIndex, Math.max(0, worlds.size())));
            worldGenerationOption = 0;
            characterOption = 0;
            nameEditing = false;
            seedEditing = false;
            pendingWorldDeleteKey = "";
            clearPreviewCache();
            status = worlds.isEmpty()
                    ? "No generated arcology worlds found. Generate a new world to continue."
                    : "Select an existing generated world, inspect its spawn preview, or choose Generate New World.";
            stage = Stage.WORLD_PICKER;
            setVisible(true);
            panel.screen = GamePanel.Screen.MENU;
            panel.panelMode = GamePanel.PanelMode.NONE;
            panel.selectedButton = 0;
            panel.logEvent("World selection opened: " + safe(reason));
            DebugLog.audit("WORLD_START_FLOW", "open picker worlds=" + worlds.size() + " reason=" + safe(reason));
            repaintPanel();
        }

        void closeToMainMenu() {
            stage = Stage.CLOSED;
            setVisible(false);
            nameEditing = false;
            seedEditing = false;
            pendingWorldDeleteKey = "";
            panel.newGameSetupActive = false;
            panel.characterNameEditActive = false;
            panel.screen = GamePanel.Screen.MENU;
            panel.panelMode = GamePanel.PanelMode.NONE;
            panel.selectedButton = 0;
            repaintPanel();
        }

        void openWorldGeneration() {
            panel.seed = System.currentTimeMillis();
            panel.rng = new Random(panel.seed ^ 0x4E475345545550L);
            panel.worldSetup = WorldSetupSettings.standard();
            worldGenerationOption = 0;
            seedEditing = false;
            seedDraft = Long.toString(panel.seed);
            pendingWorldDeleteKey = "";
            clearPreviewCache();
            status = "Configure and generate a new arcology world. This stage does not pick the character.";
            stage = Stage.WORLD_GENERATION;
            panel.newGameSetupActive = false;
            panel.characterNameEditActive = false;
            panel.screen = GamePanel.Screen.MENU;
            panel.logEvent("World generation options opened.");
            DebugLog.audit("WORLD_START_FLOW", "open generation seed=" + panel.seed + " setup=" + panel.worldSetup.shortSummary());
            repaintPanel();
        }

        void acceptGeneratedWorld() {
            if (panel.worldSetup == null) panel.worldSetup = WorldSetupSettings.standard();
            panel.seed = panel.seed == 0L ? System.currentTimeMillis() : panel.seed;
            panel.atlas = new WorldAtlas(panel.seed, panel.worldSetup.copy());
            panel.atlas.generateScaffold();
            panel.world = null;
            openCharacterCreation(panel.seed, panel.worldSetup.copy(), "generated world " + panel.atlas.hiveWorld.hiveName);
        }

        void acceptExistingWorld() {
            if (worlds.isEmpty()) {
                status = "No existing world is available. Generate a new world first.";
                repaintPanel();
                return;
            }
            worldIndex = Math.max(0, Math.min(worldIndex, worlds.size() - 1));
            WorldSaveInfo info = worlds.get(worldIndex);
            panel.seed = info.seed;
            panel.worldSetup = info.settings == null ? WorldSetupSettings.standard() : info.settings.copy();
            panel.atlas = new WorldAtlas(panel.seed, panel.worldSetup.copy());
            panel.atlas.generateScaffold();
            panel.world = null;
            openCharacterCreation(panel.seed, panel.worldSetup.copy(), "existing world " + info.hiveName);
        }

        void openCharacterCreation(long rosterSeed, WorldSetupSettings setup, String source) {
            panel.seed = rosterSeed == 0L ? System.currentTimeMillis() : rosterSeed;
            panel.rng = new Random(panel.seed ^ 0x4E475345545550L);
            panel.worldSetup = setup == null ? WorldSetupSettings.standard() : setup.copy();
            panel.prepareNewGameRoster(panel.seed);
            panel.candidateIndex = 0;
            panel.active = panel.selectedNewGameCandidate();
            panel.newGameSetupActive = true;
            panel.characterNameEditActive = false;
            nameEditing = false;
            seedEditing = false;
            pendingWorldDeleteKey = "";
            characterOption = 0;
            stage = Stage.CHARACTER_CREATION;
            panel.screen = GamePanel.Screen.CHARACTER;
            panel.panelMode = GamePanel.PanelMode.NONE;
            status = "Choose the character for " + source + ".";
            panel.logEvent("Character generation opened for " + source + ".");
            DebugLog.audit("WORLD_START_FLOW", "open character source=" + source + " candidates=" + panel.candidates.size() + " setup=" + panel.worldSetup.shortSummary());
            repaintPanel();
        }

        void startRun() {
            Candidate chosen = panel.selectedNewGameCandidate();
            if (chosen != null) chosen = chosen.copy();
            WorldSetupSettings setup = panel.worldSetup == null ? WorldSetupSettings.standard() : panel.worldSetup.copy();
            stage = Stage.CLOSED;
            setVisible(false);
            nameEditing = false;
            seedEditing = false;
            pendingWorldDeleteKey = "";
            panel.characterNameEditActive = false;
            panel.startPackagedClientNewGameWith(chosen, setup);
            repaintPanel();
        }

        @Override public boolean dispatchKeyEvent(KeyEvent e) {
            if (panel == null || e == null) return false;
            if (e.getID() == KeyEvent.KEY_PRESSED && (panel.screen == GamePanel.Screen.MENU || panel.screen == GamePanel.Screen.MAIN) && stage == Stage.CLOSED) {
                int digit = e.getKeyCode() - KeyEvent.VK_1;
                if (digit == 0 || (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) && panel.selectedButton == 0) {
                    openWorldPicker("main menu route");
                    e.consume();
                    return true;
                }
            }
            if (e.getID() == KeyEvent.KEY_PRESSED && panel.screen == GamePanel.Screen.SAVE_LOAD && e.getKeyCode() == KeyEvent.VK_N) {
                openWorldPicker("save/load new game route");
                e.consume();
                return true;
            }
            if (stage == Stage.CLOSED) return false;
            if (e.getID() == KeyEvent.KEY_TYPED) {
                if (stage == Stage.WORLD_GENERATION && seedEditing) {
                    appendSeedChar(e.getKeyChar());
                    e.consume();
                    return true;
                }
                if (stage == Stage.CHARACTER_CREATION && nameEditing) {
                    char ch = e.getKeyChar();
                    if (!Character.isISOControl(ch)) appendNameChar(ch);
                    e.consume();
                    return true;
                }
                return true;
            }
            if (e.getID() != KeyEvent.KEY_PRESSED) return true;
            boolean handled = switch (stage) {
                case WORLD_PICKER -> handleWorldPickerKey(e.getKeyCode());
                case WORLD_GENERATION -> handleWorldGenerationKey(e.getKeyCode());
                case CHARACTER_CREATION -> handleCharacterKey(e.getKeyCode());
                default -> false;
            };
            if (handled) {
                e.consume();
                repaintPanel();
            }
            return handled;
        }

        boolean handleWorldPickerKey(int code) {
            int rows = worlds.size() + 1;
            if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_Q) { closeToMainMenu(); return true; }
            if (code == KeyEvent.VK_N || code == KeyEvent.VK_G) { openWorldGeneration(); return true; }
            if (code == KeyEvent.VK_DELETE || code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_D) { requestDeleteSelectedWorld("key"); return true; }
            if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) { worldIndex = Math.floorMod(worldIndex - 1, Math.max(1, rows)); pendingWorldDeleteKey = ""; return true; }
            if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) { worldIndex = Math.floorMod(worldIndex + 1, Math.max(1, rows)); pendingWorldDeleteKey = ""; return true; }
            if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE || code == KeyEvent.VK_E) {
                if (worldIndex >= worlds.size()) openWorldGeneration(); else acceptExistingWorld();
                return true;
            }
            return true;
        }

        boolean handleWorldGenerationKey(int code) {
            if (seedEditing) return handleSeedEditKey(code);
            if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_Q) { openWorldPicker("back from world generation"); return true; }
            if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) { worldGenerationOption = Math.floorMod(worldGenerationOption - 1, 7); return true; }
            if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) { worldGenerationOption = Math.floorMod(worldGenerationOption + 1, 7); return true; }
            if (code == KeyEvent.VK_E) { beginSeedEdit(); return true; }
            if (code == KeyEvent.VK_R) { randomizeSeed(); return true; }
            if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A || code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D || code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) { cycleWorldSetupSelected(); return true; }
            if (code == KeyEvent.VK_G) { acceptGeneratedWorld(); return true; }
            return true;
        }

        boolean handleSeedEditKey(int code) {
            if (code == KeyEvent.VK_ENTER) { applySeedDraft(); return true; }
            if (code == KeyEvent.VK_ESCAPE) { cancelSeedEdit(); return true; }
            if (code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_DELETE) { backspaceSeed(); return true; }
            return true;
        }

        boolean handleCharacterKey(int code) {
            if (nameEditing) {
                if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_ESCAPE) { nameEditing = false; panel.characterNameEditActive = false; return true; }
                if (code == KeyEvent.VK_BACK_SPACE) { backspaceName(); return true; }
                return true;
            }
            if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_Q) { openWorldPicker("back from character generation"); return true; }
            if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) { characterOption = Math.floorMod(characterOption - 1, 5); return true; }
            if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) { characterOption = Math.floorMod(characterOption + 1, 5); return true; }
            if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) { if (characterOption == 0) panel.cycleSelectedCandidate(-1); else if (characterOption == 1) panel.cycleSelectedCandidateJob(-1); return true; }
            if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) { if (characterOption == 0) panel.cycleSelectedCandidate(1); else if (characterOption == 1) panel.cycleSelectedCandidateJob(1); return true; }
            if (code == KeyEvent.VK_R) { panel.rerollSelectedCandidate(); return true; }
            if (code == KeyEvent.VK_E || code == KeyEvent.VK_N) { beginNameEdit(); return true; }
            if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE || code == KeyEvent.VK_G) { if (characterOption == 4 || code == KeyEvent.VK_G) startRun(); else if (characterOption == 2) panel.rerollSelectedCandidate(); else if (characterOption == 3) beginNameEdit(); return true; }
            return true;
        }

        void cycleWorldSetupSelected() {
            if (panel.worldSetup == null) panel.worldSetup = WorldSetupSettings.standard();
            panel.worldSetupSelection = worldGenerationOption;
            panel.cycleWorldSetupOption(worldGenerationOption);
            clearPreviewCache();
        }

        void beginSeedEdit() {
            seedEditing = true;
            seedDraft = Long.toString(panel.seed == 0L ? System.currentTimeMillis() : panel.seed);
            status = "Editing world seed. Type digits, Backspace, Enter to apply, Esc to cancel.";
            requestFocusInWindow();
        }

        void appendSeedChar(char ch) {
            if (Character.isISOControl(ch)) return;
            if (ch == '-' && seedDraft.isEmpty()) { seedDraft = "-"; return; }
            if (!Character.isDigit(ch)) return;
            if (seedDraft.length() < 18) seedDraft += ch;
        }

        void backspaceSeed() {
            if (!seedDraft.isEmpty()) seedDraft = seedDraft.substring(0, seedDraft.length() - 1);
        }

        void applySeedDraft() {
            String text = seedDraft == null ? "" : seedDraft.trim();
            if (text.isBlank() || text.equals("-")) {
                status = "Seed cannot be blank. Type a number or press Esc to cancel.";
                return;
            }
            try {
                panel.seed = Long.parseLong(text);
                panel.rng = new Random(panel.seed ^ 0x4E475345545550L);
                seedEditing = false;
                status = "World seed set to " + panel.seed + ".";
                clearPreviewCache();
                DebugLog.audit("WORLD_START_FLOW", "seed edited seed=" + panel.seed);
            } catch (NumberFormatException ex) {
                status = "Seed is too large for a signed 64-bit value.";
            }
        }

        void cancelSeedEdit() {
            seedEditing = false;
            seedDraft = Long.toString(panel.seed == 0L ? System.currentTimeMillis() : panel.seed);
            status = "Seed edit cancelled.";
        }

        void randomizeSeed() {
            panel.seed = System.currentTimeMillis();
            panel.rng = new Random(panel.seed ^ 0x4E475345545550L);
            seedDraft = Long.toString(panel.seed);
            seedEditing = false;
            status = "Randomized world seed: " + panel.seed + ".";
            clearPreviewCache();
        }

        void beginNameEdit() {
            Candidate c = panel.selectedNewGameCandidate();
            if (c == null) return;
            nameEditing = true;
            panel.characterNameEditActive = true;
            status = "Editing name. Type, Backspace, then Enter to accept.";
        }

        void appendNameChar(char ch) {
            Candidate c = panel.selectedNewGameCandidate();
            if (c == null || ch == '\n' || ch == '\r') return;
            String next = (c.name == null ? "" : c.name) + ch;
            if (next.length() <= CharacterCreationAuthority.MAX_PLAYER_NAME_LENGTH) c.name = next;
            panel.active = c;
        }

        void backspaceName() {
            Candidate c = panel.selectedNewGameCandidate();
            if (c == null || c.name == null || c.name.isEmpty()) return;
            c.name = c.name.substring(0, c.name.length() - 1);
            panel.active = c;
        }

        void requestDeleteSelectedWorld(String reason) {
            if (worldIndex < 0 || worldIndex >= worlds.size()) {
                status = "Select an existing world to delete. The Generate New row is not a world file.";
                pendingWorldDeleteKey = "";
                return;
            }
            WorldSaveInfo info = worlds.get(worldIndex);
            String key = info.seed + ":" + safe(info.hiveName);
            if (!key.equals(pendingWorldDeleteKey)) {
                pendingWorldDeleteKey = key;
                status = "Confirm delete " + safe(info.hiveName) + ": press Delete/D again or click Delete World again.";
                return;
            }
            java.nio.file.Path path = findWorldDefinitionFile(info);
            if (path == null) {
                status = "Could not locate a world definition file for " + safe(info.hiveName) + ".";
                pendingWorldDeleteKey = "";
                DebugLog.warn("WORLD_START_FLOW", status);
                return;
            }
            try {
                java.nio.file.Files.deleteIfExists(path);
                status = "Deleted world " + safe(info.hiveName) + ".";
                panel.logEvent(status);
                DebugLog.audit("WORLD_START_FLOW", "deleted world reason=" + safe(reason) + " path=" + path);
                worlds = WorldSaveInfo.listExistingWorlds();
                worldIndex = Math.max(0, Math.min(worldIndex, Math.max(0, worlds.size())));
                pendingWorldDeleteKey = "";
                clearPreviewCache();
            } catch (Throwable t) {
                status = "Could not delete " + safe(info.hiveName) + ": " + t.getMessage();
                pendingWorldDeleteKey = "";
                DebugLog.error("WORLD_START_FLOW", status, t);
            }
        }

        java.nio.file.Path findWorldDefinitionFile(WorldSaveInfo info) {
            java.nio.file.Path direct = reflectivePath(info);
            if (direct != null && java.nio.file.Files.isRegularFile(direct)) return direct;
            java.nio.file.Path dir;
            try { dir = java.nio.file.Path.of(String.valueOf(CampaignWorldApi.worldDir())); }
            catch (Throwable t) { return null; }
            if (!java.nio.file.Files.isDirectory(dir)) return null;
            String seedText = Long.toString(info.seed);
            String nameText = safe(info.hiveName);
            java.nio.file.Path best = null;
            int bestScore = 0;
            try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(dir)) {
                for (java.nio.file.Path path : stream.filter(java.nio.file.Files::isRegularFile).toList()) {
                    int score = 0;
                    String file = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
                    if (file.endsWith(".mechworld") || file.endsWith(".properties") || file.endsWith(".txt")) score++;
                    try {
                        String text = java.nio.file.Files.readString(path);
                        if (text.contains(seedText)) score += 4;
                        if (!nameText.isBlank() && text.contains(nameText)) score += 3;
                        if (info.settings != null && text.contains(info.settings.encode())) score += 2;
                    } catch (Throwable ignored) {}
                    if (score > bestScore) { bestScore = score; best = path; }
                }
            } catch (Throwable t) {
                DebugLog.warn("WORLD_START_FLOW", "Could not scan world directory for delete: " + t.getMessage());
            }
            return bestScore >= 4 ? best : null;
        }

        java.nio.file.Path reflectivePath(WorldSaveInfo info) {
            if (info == null) return null;
            for (String name : List.of("path", "file", "worldFile", "sourcePath", "definitionPath")) {
                try {
                    java.lang.reflect.Field f = info.getClass().getDeclaredField(name);
                    f.setAccessible(true);
                    java.nio.file.Path p = pathFromObject(f.get(info));
                    if (p != null) return p;
                } catch (Throwable ignored) {}
                try {
                    java.lang.reflect.Method m = info.getClass().getDeclaredMethod(name);
                    m.setAccessible(true);
                    java.nio.file.Path p = pathFromObject(m.invoke(info));
                    if (p != null) return p;
                } catch (Throwable ignored) {}
            }
            return null;
        }

        java.nio.file.Path pathFromObject(Object value) {
            if (value instanceof java.nio.file.Path p) return p;
            if (value instanceof java.io.File f) return f.toPath();
            if (value instanceof CharSequence s && !s.toString().isBlank()) return java.nio.file.Path.of(s.toString());
            return null;
        }

        void handleMouse(MouseEvent e) {
            if (stage == Stage.CLOSED || e == null) return;
            Point p = e.getPoint();
            if (stage == Stage.WORLD_PICKER) {
                if (buttonRect(0).contains(p)) { openWorldGeneration(); e.consume(); return; }
                if (buttonRect(1).contains(p)) { closeToMainMenu(); e.consume(); return; }
                if (buttonRect(2).contains(p)) { requestDeleteSelectedWorld("mouse"); e.consume(); repaintPanel(); return; }
                List<Rectangle> rows = worldPickerRows();
                for (int i = 0; i < rows.size(); i++) if (rows.get(i).contains(p)) { worldIndex = i; pendingWorldDeleteKey = ""; if (i >= worlds.size() && e.getClickCount() >= 2) openWorldGeneration(); else if (i < worlds.size() && e.getClickCount() >= 2) acceptExistingWorld(); e.consume(); repaintPanel(); return; }
            } else if (stage == Stage.WORLD_GENERATION) {
                if (seedEditRect().contains(p)) { beginSeedEdit(); e.consume(); repaintPanel(); return; }
                List<Rectangle> rows = worldGenerationRows();
                for (int i = 0; i < rows.size(); i++) if (rows.get(i).contains(p)) { worldGenerationOption = i; cycleWorldSetupSelected(); e.consume(); return; }
                if (buttonRect(0).contains(p)) { acceptGeneratedWorld(); e.consume(); return; }
                if (buttonRect(1).contains(p)) { openWorldPicker("mouse back"); e.consume(); return; }
            } else if (stage == Stage.CHARACTER_CREATION) {
                List<Rectangle> rows = optionRows(5);
                for (int i = 0; i < rows.size(); i++) if (rows.get(i).contains(p)) { characterOption = i; if (i == 0) panel.cycleSelectedCandidate(1); else if (i == 1) panel.cycleSelectedCandidateJob(1); else if (i == 2) panel.rerollSelectedCandidate(); else if (i == 3) beginNameEdit(); else startRun(); e.consume(); return; }
                if (buttonRect(0).contains(p)) { startRun(); e.consume(); return; }
                if (buttonRect(1).contains(p)) { openWorldPicker("mouse back"); e.consume(); return; }
            }
            e.consume();
            repaintPanel();
        }

        @Override protected void paintComponent(Graphics graphics) {
            if (stage == Stage.CLOSED) return;
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = Math.max(1, getWidth());
                int h = Math.max(1, getHeight());
                g.setColor(new Color(0, 0, 0, 208));
                g.fillRect(0, 0, w, h);
                Rectangle panelRect = panelRect(w, h);
                drawFrame(g, panelRect, title());
                if (stage == Stage.WORLD_PICKER) paintWorldPicker(g, panelRect);
                else if (stage == Stage.WORLD_GENERATION) paintWorldGeneration(g, panelRect);
                else if (stage == Stage.CHARACTER_CREATION) paintCharacterCreation(g, panelRect);
            } finally {
                g.dispose();
            }
        }

        void paintWorldPicker(Graphics2D g, Rectangle r) {
            drawText(g, "Generated world definitions are read from: " + CampaignWorldApi.worldDir(), r.x + 24, r.y + 64, r.width - 48, muted());
            drawText(g, status, r.x + 24, r.y + 94, r.width - 48, main());
            List<Rectangle> rows = worldPickerRows();
            for (int i = 0; i < rows.size(); i++) {
                Rectangle row = rows.get(i);
                boolean selected = i == worldIndex;
                fillRow(g, row, selected);
                String label = i < worlds.size() ? worlds.get(i).summaryLine() : "+ Generate New World";
                drawText(g, (selected ? "> " : "  ") + label, row.x + 12, row.y + 28, row.width - 24, selected ? highlight() : main());
            }
            drawWorldManagerPreview(g, worldManagerPreviewRect());
            drawButton(g, buttonRect(0), "Generate New", true);
            drawButton(g, buttonRect(1), "Back", false);
            drawDangerButton(g, buttonRect(2), "Delete World");
            drawFooter(g, "Enter opens selected. Double-click row opens. N/G generates. Delete/D deletes selected world. Esc returns.");
        }

        void paintWorldGeneration(Graphics2D g, Rectangle r) {
            if (panel.worldSetup == null) panel.worldSetup = WorldSetupSettings.standard();
            drawText(g, "World Generation Options", r.x + 24, r.y + 64, r.width - 48, highlight());
            drawText(g, panel.worldSetup.shortSummary(), r.x + 24, r.y + 94, r.width - 48, main());
            drawSeedEditor(g);

            ArrayList<String> lines = panel.worldSetup.detailLines();
            List<Rectangle> rows = worldGenerationRows();
            for (int i = 0; i < lines.size() && i < rows.size(); i++) {
                Rectangle row = rows.get(i);
                boolean selected = i == worldGenerationOption;
                fillRow(g, row, selected);
                drawText(g, (selected ? "> " : "  ") + lines.get(i), row.x + 12, row.y + 27, row.width - 24, selected ? highlight() : main());
            }
            drawSpawnPreview(g, spawnPreviewRect());
            drawButton(g, buttonRect(0), "Generate World", true);
            drawButton(g, buttonRect(1), "Back", false);
            drawFooter(g, "Up/Down chooses options. Left/Right cycles. Click seed or E edits. R rerolls seed. G generates.");
        }

        void paintCharacterCreation(Graphics2D g, Rectangle r) {
            Candidate c = panel.selectedNewGameCandidate();
            drawText(g, "Character Generation", r.x + 24, r.y + 64, r.width - 48, highlight());
            drawText(g, status, r.x + 24, r.y + 94, r.width - 48, main());
            Rectangle portrait = new Rectangle(r.x + 26, r.y + 122, 180, 180);
            g.setColor(new Color(12, 14, 13, 230));
            g.fillRoundRect(portrait.x, portrait.y, portrait.width, portrait.height, 8, 8);
            g.setColor(new Color(145, 118, 64, 170));
            g.drawRoundRect(portrait.x, portrait.y, portrait.width, portrait.height, 8, 8);
            if (c != null) {
                java.awt.image.BufferedImage img = panel.images.getPlayerPortrait(c);
                if (img != null) g.drawImage(img, portrait.x + 10, portrait.y + 10, portrait.width - 20, portrait.height - 20, null);
                else drawAsciiPortrait(g, c, portrait);
            }
            Rectangle info = new Rectangle(portrait.x + portrait.width + 24, portrait.y, r.x + r.width - portrait.x - portrait.width - 50, 230);
            ArrayList<String> detail = new ArrayList<>();
            if (c != null) {
                detail.add("Name: " + safe(c.name) + (nameEditing ? " _" : ""));
                detail.add("Job: " + safe(c.job));
                JobProfile profile = JobProfile.get(c.job);
                if (profile != null) {
                    detail.add("Identity: " + profile.shortIdentity());
                    detail.add("Bonus: " + profile.bonusText());
                    detail.add("Penalty: " + profile.penaltyText());
                    detail.add("Requirements: " + profile.requirementText() + " / missing " + profile.missingText(c));
                }
                detail.add("Age: " + c.ageBand + " " + c.ageYears + " years");
            }
            drawLines(g, detail, info.x, info.y, info.width, main());
            ArrayList<String> stats = new ArrayList<>();
            if (c != null) for (var e : c.stats.entrySet()) stats.add(e.getKey() + " " + e.getValue());
            drawLines(g, stats, r.x + 26, portrait.y + portrait.height + 28, 320, muted());
            List<String> opts = List.of("Candidate", "Job", "Reroll Candidate", "Edit Name", "Start Run");
            for (int i = 0; i < opts.size(); i++) {
                Rectangle row = new Rectangle(info.x, r.y + 372 + i * 42, info.width, 34);
                boolean selected = i == characterOption;
                fillRow(g, row, selected);
                drawText(g, (selected ? "> " : "  ") + opts.get(i), row.x + 12, row.y + 24, row.width - 24, selected ? highlight() : main());
            }
            drawButton(g, buttonRect(0), "Start Run", true);
            drawButton(g, buttonRect(1), "Back", false);
            drawFooter(g, "Left/Right changes candidate/job. R rerolls. E edits name. G starts run. Esc returns to world picker.");
        }

        void drawSeedEditor(Graphics2D g) {
            Rectangle seed = seedEditRect();
            g.setFont(panel.smallFont);
            g.setColor(muted());
            drawBackedText(g, "Seed", seed.x, seed.y - 8, false);
            g.setColor(seedEditing ? new Color(54, 47, 31, 238) : new Color(16, 18, 16, 224));
            g.fillRoundRect(seed.x, seed.y, seed.width, seed.height, 8, 8);
            g.setColor(seedEditing ? highlight() : new Color(145, 118, 64, 140));
            g.drawRoundRect(seed.x, seed.y, seed.width, seed.height, 8, 8);
            String text = seedEditing ? seedDraft + "_" : Long.toString(panel.seed);
            drawText(g, text, seed.x + 10, seed.y + 24, seed.width - 20, seedEditing ? highlight() : main());
        }

        void drawWorldManagerPreview(Graphics2D g, Rectangle r) {
            WorldSaveInfo info = selectedWorldInfo();
            if (info != null) ensurePreviewFor(info.seed, info.settings == null ? WorldSetupSettings.standard() : info.settings.copy(), "existing:" + info.seed + ":" + safe(info.hiveName), safe(info.hiveName) + " / seed " + info.seed);
            else {
                long seed = panel.seed == 0L ? System.currentTimeMillis() : panel.seed;
                WorldSetupSettings setup = panel.worldSetup == null ? WorldSetupSettings.standard() : panel.worldSetup.copy();
                ensurePreviewFor(seed, setup, "new:" + seed + ":" + setup.encode(), "Generate New World / seed " + seed);
            }
            drawSpawnPreviewBody(g, r, info == null ? "Preview: Generate New World" : "Selected World Spawn Preview");
        }

        WorldSaveInfo selectedWorldInfo() {
            return worldIndex >= 0 && worldIndex < worlds.size() ? worlds.get(worldIndex) : null;
        }

        void drawSpawnPreview(Graphics2D g, Rectangle r) {
            if (panel.worldSetup == null) panel.worldSetup = WorldSetupSettings.standard();
            long safeSeed = panel.seed == 0L ? System.currentTimeMillis() : panel.seed;
            ensurePreviewFor(safeSeed, panel.worldSetup.copy(), "generation:" + safeSeed + ":" + panel.worldSetup.encode(), "Generated hive / seed " + safeSeed);
            drawSpawnPreviewBody(g, r, "Spawn Sector Preview");
        }

        void drawSpawnPreviewBody(Graphics2D g, Rectangle r, String title) {
            g.setColor(new Color(12, 14, 13, 230));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
            g.setColor(new Color(145, 118, 64, 170));
            g.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);
            drawText(g, title, r.x + 14, r.y + 28, r.width - 28, highlight());
            drawText(g, previewStatus, r.x + 14, r.y + 54, r.width - 28, muted());
            if (previewWorld == null) return;

            Rectangle map = new Rectangle(r.x + 14, r.y + 78, r.width - 28, Math.max(120, r.height - 122));
            g.setColor(new Color(4, 5, 5, 238));
            g.fillRect(map.x, map.y, map.width, map.height);
            Point spawn = previewWorld.startPoint();
            int cols = Math.max(8, Math.min(previewWorld.w, 32));
            int rows = Math.max(8, Math.min(previewWorld.h, 24));
            int tile = Math.max(3, Math.min(map.width / cols, map.height / rows));
            int drawW = tile * cols;
            int drawH = tile * rows;
            int ox = map.x + Math.max(0, (map.width - drawW) / 2);
            int oy = map.y + Math.max(0, (map.height - drawH) / 2);
            int minX = Math.max(0, Math.min(Math.max(0, previewWorld.w - cols), spawn.x - cols / 2));
            int minY = Math.max(0, Math.min(Math.max(0, previewWorld.h - rows), spawn.y - rows / 2));
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    int wx = minX + x;
                    int wy = minY + y;
                    if (!previewWorld.inBounds(wx, wy)) continue;
                    char ch = previewWorld.tiles[wx][wy];
                    g.setColor(previewTileColor(ch));
                    g.fillRect(ox + x * tile, oy + y * tile, tile, tile);
                }
            }
            if (previewWorld.inBounds(spawn.x, spawn.y)) {
                int sx = ox + (spawn.x - minX) * tile;
                int sy = oy + (spawn.y - minY) * tile;
                if (sx >= ox && sy >= oy && sx < ox + drawW && sy < oy + drawH) {
                    g.setColor(new Color(245, 214, 118));
                    g.setStroke(new BasicStroke(Math.max(1f, tile / 4f)));
                    g.drawRect(sx, sy, Math.max(2, tile - 1), Math.max(2, tile - 1));
                }
            }
            g.setColor(new Color(0, 0, 0, 90));
            for (int y = oy; y < oy + drawH; y += Math.max(1, tile)) g.drawLine(ox, y, ox + drawW, y);
            for (int x = ox; x < ox + drawW; x += Math.max(1, tile)) g.drawLine(x, oy, x, oy + drawH);
            drawText(g, "Spawn " + spawn.x + "," + spawn.y + " / " + previewWorld.zoneType.label, r.x + 14, r.y + r.height - 28, r.width - 28, main());
        }

        void ensurePreviewFor(long seed, WorldSetupSettings setup, String key, String label) {
            WorldSetupSettings safeSetup = setup == null ? WorldSetupSettings.standard() : setup;
            if (key.equals(previewKey) && previewWorld != null) return;
            try {
                WorldAtlas atlas = new WorldAtlas(seed, safeSetup.copy());
                atlas.generateScaffold();
                previewAtlas = atlas;
                previewWorld = atlas.currentWorld();
                previewKey = key;
                String name = label == null || label.isBlank() ? (atlas.hiveWorld == null ? "generated hive" : atlas.hiveWorld.hiveName) : label;
                previewStatus = name;
            } catch (Throwable t) {
                previewAtlas = null;
                previewWorld = null;
                previewKey = key;
                previewStatus = "Preview unavailable: " + t.getMessage();
                DebugLog.warn("WORLD_START_FLOW", previewStatus);
            }
        }

        void clearPreviewCache() {
            previewAtlas = null;
            previewWorld = null;
            previewKey = "";
            previewStatus = "";
        }

        Color previewTileColor(char ch) {
            if (ch == '#') return new Color(38, 39, 36);
            if (ch == '.' || ch == ',' || ch == ':' || ch == ';' || ch == '=') return new Color(47, 48, 43);
            if (ch == '+' || ch == '/' || ch == '\\' || ch == 'D') return new Color(91, 76, 43);
            if (ch == '~') return new Color(35, 57, 58);
            if (Character.isUpperCase(ch)) return new Color(72, 58, 36);
            return new Color(28, 29, 27);
        }

        List<Rectangle> worldPickerRows() {
            Rectangle p = panelRect(getWidth(), getHeight());
            ArrayList<Rectangle> rows = new ArrayList<>();
            int count = Math.max(1, worlds.size() + 1);
            int top = p.y + 130;
            int rowH = 42;
            int max = Math.min(count, Math.max(1, (p.height - 228) / (rowH + 8)));
            for (int i = 0; i < max; i++) rows.add(new Rectangle(p.x + 24, top + i * (rowH + 8), worldPickerListWidth(p), rowH));
            return rows;
        }

        int worldPickerListWidth(Rectangle p) {
            int previewW = Math.max(300, Math.min(430, p.width / 3));
            return Math.max(280, p.width - 72 - previewW - 22);
        }

        Rectangle worldManagerPreviewRect() {
            Rectangle p = panelRect(getWidth(), getHeight());
            int leftW = worldPickerListWidth(p);
            int x = p.x + 24 + leftW + 22;
            int w = Math.max(300, p.x + p.width - 28 - x);
            return new Rectangle(x, p.y + 130, w, Math.max(300, p.height - 220));
        }

        List<Rectangle> worldGenerationRows() {
            Rectangle p = panelRect(getWidth(), getHeight());
            ArrayList<Rectangle> rows = new ArrayList<>();
            int top = p.y + 166;
            int width = Math.max(300, Math.min(540, p.width - 420));
            for (int i = 0; i < 7; i++) rows.add(new Rectangle(p.x + 24, top + i * 42, width, 34));
            return rows;
        }

        List<Rectangle> optionRows(int count) {
            Rectangle p = panelRect(getWidth(), getHeight());
            ArrayList<Rectangle> rows = new ArrayList<>();
            int top = p.y + 124;
            for (int i = 0; i < count; i++) rows.add(new Rectangle(p.x + 24, top + i * 42, p.width - 48, 34));
            return rows;
        }

        Rectangle seedEditRect() {
            Rectangle p = panelRect(getWidth(), getHeight());
            int width = Math.max(300, Math.min(540, p.width - 420));
            return new Rectangle(p.x + 24, p.y + 118, width, 34);
        }

        Rectangle spawnPreviewRect() {
            Rectangle p = panelRect(getWidth(), getHeight());
            int leftW = Math.max(300, Math.min(540, p.width - 420));
            int x = p.x + 24 + leftW + 24;
            int w = Math.max(300, p.x + p.width - 28 - x);
            return new Rectangle(x, p.y + 118, w, Math.max(320, p.height - 206));
        }

        Rectangle panelRect(int w, int h) {
            int pw = Math.max(720, Math.min(1060, w - 72));
            int ph = Math.max(540, Math.min(720, h - 72));
            return new Rectangle(Math.max(20, w / 2 - pw / 2), Math.max(28, h / 2 - ph / 2), pw, ph);
        }

        Rectangle buttonRect(int index) {
            Rectangle p = panelRect(getWidth(), getHeight());
            if (index == 2) return new Rectangle(p.x + 28, p.y + p.height - 58, 146, 34);
            int bw = index == 0 ? 178 : 120;
            int gap = 16;
            int x = index == 0 ? p.x + p.width - bw - 28 : p.x + p.width - bw - 28 - 178 - gap;
            return new Rectangle(x, p.y + p.height - 58, bw, 34);
        }

        String title() {
            return switch (stage) {
                case WORLD_PICKER -> "WORLD MANAGEMENT";
                case WORLD_GENERATION -> "NEW WORLD GENERATION";
                case CHARACTER_CREATION -> "CHARACTER GENERATION";
                default -> "START FLOW";
            };
        }

        void drawFrame(Graphics2D g, Rectangle r, String title) {
            g.setColor(new Color(9, 10, 9, 238));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 14, 14);
            g.setColor(new Color(120, 96, 48, 210));
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(r.x, r.y, r.width, r.height, 14, 14);
            g.setFont(panel.titleFont.deriveFont(Font.BOLD, 30f));
            g.setColor(highlight());
            center(g, title, r.x + r.width / 2, r.y + 38);
        }

        void fillRow(Graphics2D g, Rectangle r, boolean selected) {
            g.setColor(selected ? new Color(76, 62, 32, 232) : new Color(16, 18, 16, 224));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g.setColor(selected ? highlight() : new Color(145, 118, 64, 120));
            g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        }

        void drawButton(Graphics2D g, Rectangle r, String label, boolean primary) {
            drawButtonWithColor(g, r, label, primary ? new Color(76, 62, 32, 232) : new Color(16, 18, 16, 224), primary ? highlight() : main(), primary ? highlight() : new Color(145, 118, 64, 140));
        }

        void drawDangerButton(Graphics2D g, Rectangle r, String label) {
            drawButtonWithColor(g, r, label, new Color(58, 18, 14, 232), new Color(236, 168, 142), new Color(192, 74, 58, 180));
        }

        void drawButtonWithColor(Graphics2D g, Rectangle r, String label, Color fill, Color text, Color border) {
            g.setColor(fill);
            g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g.setColor(border);
            g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g.setColor(text);
            BufferedImage icon = panel.systemButtonIconForLabel(label);
            FontMetrics fm = g.getFontMetrics();
            int iconSize = icon == null ? 0 : Math.max(18, Math.min(r.height - 6, 30));
            int labelW = fm.stringWidth(label);
            int groupW = labelW + (iconSize > 0 ? iconSize + 8 : 0);
            int x = r.x + Math.max(8, (r.width - groupW) / 2);
            if (icon != null) {
                g.drawImage(icon, x, r.y + (r.height - iconSize) / 2, iconSize, iconSize, null);
                x += iconSize + 8;
            }
            drawBackedText(g, label, x, r.y + 23, false);
        }

        void drawFooter(Graphics2D g, String text) {
            Rectangle p = panelRect(getWidth(), getHeight());
            g.setFont(panel.smallFont);
            g.setColor(muted());
            center(g, text, p.x + p.width / 2, p.y + p.height - 78);
        }

        void drawText(Graphics2D g, String text, int x, int y, int width, Color color) {
            g.setFont(panel.smallFont);
            FontMetrics fm = g.getFontMetrics();
            List<String> lines = GuiLayoutApi.wrapText(text == null ? "" : text, fm, Math.max(8, width));
            drawWrappedTextBlock(g, lines, x, y, width, color, false);
        }

        void drawLines(Graphics2D g, List<String> lines, int x, int y, int width, Color color) {
            g.setFont(panel.smallFont);
            ArrayList<String> wrapped = new ArrayList<>();
            FontMetrics fm = g.getFontMetrics();
            if (lines != null) for (String line : lines) wrapped.addAll(GuiLayoutApi.wrapText(line == null ? "" : line, fm, Math.max(8, width)));
            drawWrappedTextBlock(g, wrapped, x, y, width, color, false);
        }

        void drawWrappedTextBlock(Graphics2D g, List<String> lines, int x, int firstBaseline, int width, Color color, boolean centered) {
            if (g == null || lines == null || lines.isEmpty()) return;
            FontMetrics fm = g.getFontMetrics();
            int lineH = Math.max(15, fm.getHeight() + 2);
            int maxW = 0;
            for (String line : lines) maxW = Math.max(maxW, fm.stringWidth(line == null ? "" : line));
            int bx = centered ? x - maxW / 2 - 7 : x - 7;
            int by = firstBaseline - fm.getAscent() - 5;
            int bw = Math.max(12, Math.min(Math.max(width, maxW), maxW) + 14);
            int bh = lineH * lines.size() + 8;
            Color old = g.getColor();
            g.setColor(new Color(0, 0, 0, 118));
            g.fillRoundRect(bx, by, bw, bh, 8, 8);
            g.setColor(new Color(128, 105, 58, 88));
            g.drawRoundRect(bx, by, bw, bh, 8, 8);
            g.setColor(color == null ? old : color);
            int yy = firstBaseline;
            for (String raw : lines) {
                String line = raw == null ? "" : raw;
                int tx = centered ? x - fm.stringWidth(line) / 2 : x;
                g.drawString(line, tx, yy);
                yy += lineH;
            }
            g.setColor(old);
        }

        void drawAsciiPortrait(Graphics2D g, Candidate c, Rectangle r) {
            g.setFont(panel.asciiFont.deriveFont(Font.BOLD, 15f));
            g.setColor(main());
            int y = r.y + 38;
            if (c == null || c.portrait == null) return;
            for (String line : c.portrait) {
                center(g, line, r.x + r.width / 2, y);
                y += 20;
            }
        }

        void center(Graphics2D g, String text, int x, int y) {
            FontMetrics fm = g.getFontMetrics();
            String s = text == null ? "" : text;
            drawWrappedTextBlock(g, List.of(s), x, y, fm.stringWidth(s), g.getColor(), true);
        }

        void drawBackedText(Graphics2D g, String text, int x, int y, boolean centered) {
            if (g == null || text == null || text.isBlank()) return;
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(text);
            int th = fm.getHeight();
            int bx = x - 5;
            int by = y - fm.getAscent() - 3;
            Color old = g.getColor();
            g.setColor(new Color(0, 0, 0, 104));
            g.fillRoundRect(bx, by, tw + 10, th + 5, 7, 7);
            g.setColor(new Color(128, 105, 58, 76));
            g.drawRoundRect(bx, by, tw + 10, th + 5, 7, 7);
            g.setColor(old);
            g.drawString(text, x, y);
        }

        void repaintPanel() {
            revalidate();
            repaint();
            if (panel != null) panel.repaint();
        }

        Color highlight() { return panel.optionColor(GameOptions.TEXT_HIGHLIGHT); }
        Color main() { return panel.optionColor(GameOptions.TEXT_MAIN); }
        Color muted() { return panel.optionColor(GameOptions.TEXT_DIM); }
        static String safe(String s) { return s == null || s.isBlank() ? "unspecified" : s.replace('\n', ' ').trim(); }
    }
}
