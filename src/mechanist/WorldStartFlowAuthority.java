package mechanist;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
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
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        static final int CHARACTER_ACTION_COUNT = 8;
        static final int WORLD_PICKER_ROW_HEIGHT = 88;
        static final int WORLD_PICKER_ROW_GAP = 10;
        static final String[] CHARACTER_ACTION_LABELS = {
                "Candidate <",
                "Candidate >",
                "Job <",
                "Job >",
                "Reroll",
                "New Roster",
                "Edit Name",
                "Start Run"
        };

        final GamePanel panel;
        Stage stage = Stage.CLOSED;
        ArrayList<WorldSaveInfo> worlds = new ArrayList<>();
        int worldIndex;
        int worldPickerScroll;
        int worldGenerationOption;
        int characterOption;
        String status = "";
        boolean nameEditing;
        boolean seedEditing;
        String seedDraft = "";
        String pendingWorldDeleteKey = "";
        WorldAtlas previewAtlas;
        World previewWorld;
        BufferedImage previewOverviewImage;
        String previewKey = "";
        String previewRequestedKey = "";
        String previewStatus = "";
        long previewRequestSerial;
        boolean previewWorkerRunning;
        PreviewRequest pendingPreviewRequest;

        static final class PreviewRequest {
            final long seed;
            final WorldSetupSettings setup;
            final String key;
            final String label;
            final long serial;
            PreviewRequest(long seed, WorldSetupSettings setup, String key, String label, long serial) {
                this.seed = seed;
                this.setup = setup;
                this.key = key;
                this.label = label;
                this.serial = serial;
            }
        }

        WorldStartFlowOverlay(GamePanel panel) {
            this.panel = panel;
            setOpaque(false);
            setVisible(false);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { handleMouse(e); }
                @Override public void mousePressed(MouseEvent e) { if (stage != Stage.CLOSED) e.consume(); }
                @Override public void mouseReleased(MouseEvent e) { if (stage != Stage.CLOSED) e.consume(); }
            });
            addMouseWheelListener(e -> {
                if (stage != Stage.WORLD_PICKER) return;
                scrollWorldPicker(e.getWheelRotation());
                e.consume();
                repaintPanel();
            });
        }

        void openWorldPicker(String reason) {
            worlds = WorldSaveInfo.listExistingWorlds();
            worldIndex = worlds.isEmpty() ? 0 : Math.max(0, Math.min(worldIndex, worlds.size() - 1));
            clampWorldPickerSelection();
            worldGenerationOption = 0;
            characterOption = 0;
            nameEditing = false;
            seedEditing = false;
            pendingWorldDeleteKey = "";
            clearPreviewCache();
            status = worlds.isEmpty()
                    ? MenuTextAuthority.text("menu.world_manager.empty", "No generated arcology worlds found. Generate a new world to continue.")
                    : MenuTextAuthority.text("menu.world_manager.status", "Select an existing generated world, inspect its spawn preview, or choose Generate New World.");
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
            panel.atlas = WorldAtlas.createNew(panel.seed, panel.worldSetup.copy());
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
            panel.atlas = WorldAtlas.loadExisting(panel.seed, panel.worldSetup.copy());
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
            if (chosen != null) {
                chosen.name = CharacterCreationAuthority.sanitizePlayerName(chosen.name, panel.rng);
                panel.refreshNameLockedCandidateState(chosen);
                chosen = chosen.copy();
            }
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
            if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) { worldIndex = Math.floorMod(worldIndex - 1, Math.max(1, rows)); pendingWorldDeleteKey = ""; ensureWorldPickerSelectionVisible(); return true; }
            if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) { worldIndex = Math.floorMod(worldIndex + 1, Math.max(1, rows)); pendingWorldDeleteKey = ""; ensureWorldPickerSelectionVisible(); return true; }
            if (code == KeyEvent.VK_PAGE_UP) { worldIndex = Math.max(0, worldIndex - worldPickerVisibleCapacity(panelRect(getWidth(), getHeight()))); pendingWorldDeleteKey = ""; ensureWorldPickerSelectionVisible(); return true; }
            if (code == KeyEvent.VK_PAGE_DOWN) { worldIndex = Math.min(Math.max(0, rows - 1), worldIndex + worldPickerVisibleCapacity(panelRect(getWidth(), getHeight()))); pendingWorldDeleteKey = ""; ensureWorldPickerSelectionVisible(); return true; }
            if (code == KeyEvent.VK_HOME) { worldIndex = 0; pendingWorldDeleteKey = ""; ensureWorldPickerSelectionVisible(); return true; }
            if (code == KeyEvent.VK_END) { worldIndex = Math.max(0, rows - 1); pendingWorldDeleteKey = ""; ensureWorldPickerSelectionVisible(); return true; }
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
                if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_ESCAPE) { finishNameEdit(); return true; }
                if (code == KeyEvent.VK_BACK_SPACE) { backspaceName(); return true; }
                if (code == KeyEvent.VK_DELETE) { clearName(); return true; }
                return true;
            }
            if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_Q) { openWorldPicker("back from character generation"); return true; }
            if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) { characterOption = Math.floorMod(characterOption - 1, CHARACTER_ACTION_COUNT); return true; }
            if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) { characterOption = Math.floorMod(characterOption + 1, CHARACTER_ACTION_COUNT); return true; }
            if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) { if (characterOption == 2 || characterOption == 3) panel.cycleSelectedCandidateJob(-1); else panel.cycleSelectedCandidate(-1); return true; }
            if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) { if (characterOption == 2 || characterOption == 3) panel.cycleSelectedCandidateJob(1); else panel.cycleSelectedCandidate(1); return true; }
            if (code == KeyEvent.VK_J) { panel.cycleSelectedCandidateJob(1); return true; }
            if (code == KeyEvent.VK_K) { panel.cycleSelectedCandidateJob(-1); return true; }
            if (code == KeyEvent.VK_R) { panel.rerollSelectedCandidate(); return true; }
            if (code == KeyEvent.VK_E || code == KeyEvent.VK_N) { beginNameEdit(); return true; }
            if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) { triggerCharacterAction(characterOption); return true; }
            if (code == KeyEvent.VK_G) { startRun(); return true; }
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

        void backspaceSeed() { if (!seedDraft.isEmpty()) seedDraft = seedDraft.substring(0, seedDraft.length() - 1); }

        void applySeedDraft() {
            String text = seedDraft == null ? "" : seedDraft.trim();
            if (text.isBlank() || text.equals("-")) { status = "Seed cannot be blank. Type a number or press Esc to cancel."; return; }
            try {
                panel.seed = Long.parseLong(text);
                panel.rng = new Random(panel.seed ^ 0x4E475345545550L);
                seedEditing = false;
                status = "World seed set to " + panel.seed + ".";
                clearPreviewCache();
                DebugLog.audit("WORLD_START_FLOW", "seed edited seed=" + panel.seed);
            } catch (NumberFormatException ex) { status = "Seed is too large for a signed 64-bit value."; }
        }

        void cancelSeedEdit() { seedEditing = false; seedDraft = Long.toString(panel.seed == 0L ? System.currentTimeMillis() : panel.seed); status = "Seed edit cancelled."; }
        void randomizeSeed() { panel.seed = System.currentTimeMillis(); panel.rng = new Random(panel.seed ^ 0x4E475345545550L); seedDraft = Long.toString(panel.seed); seedEditing = false; status = "Randomized world seed: " + panel.seed + "."; clearPreviewCache(); }
        void beginNameEdit() { Candidate c = panel.selectedNewGameCandidate(); if (c == null) return; nameEditing = true; panel.characterNameEditActive = true; status = "Editing name. Type, Backspace, Delete to clear, Enter to accept."; requestFocusInWindow(); }
        void finishNameEdit() { Candidate c = panel.selectedNewGameCandidate(); if (c != null) { c.name = CharacterCreationAuthority.sanitizePlayerName(c.name, panel.rng); panel.refreshNameLockedCandidateState(c); panel.active = c; } nameEditing = false; panel.characterNameEditActive = false; status = "Name accepted."; }
        void appendNameChar(char ch) { Candidate c = panel.selectedNewGameCandidate(); if (c == null || ch == '\n' || ch == '\r') return; if (!CharacterCreationAuthority.acceptNameChar(ch) && !Character.isDigit(ch)) return; String next = (c.name == null ? "" : c.name) + ch; if (next.length() <= CharacterCreationAuthority.MAX_PLAYER_NAME_LENGTH) { c.name = next; panel.refreshNameLockedCandidateState(c); } panel.active = c; }
        void backspaceName() { Candidate c = panel.selectedNewGameCandidate(); if (c == null || c.name == null || c.name.isEmpty()) return; c.name = c.name.substring(0, c.name.length() - 1); panel.refreshNameLockedCandidateState(c); panel.active = c; }
        void clearName() { Candidate c = panel.selectedNewGameCandidate(); if (c == null) return; c.name = ""; panel.refreshNameLockedCandidateState(c); panel.active = c; }
        void triggerCharacterAction(int action) {
            switch (Math.floorMod(action, CHARACTER_ACTION_COUNT)) {
                case 0 -> panel.cycleSelectedCandidate(-1);
                case 1 -> panel.cycleSelectedCandidate(1);
                case 2 -> panel.cycleSelectedCandidateJob(-1);
                case 3 -> panel.cycleSelectedCandidateJob(1);
                case 4 -> panel.rerollSelectedCandidate();
                case 5 -> panel.rerollNewGameRoster();
                case 6 -> beginNameEdit();
                case 7 -> startRun();
                default -> {}
            }
        }

        void requestDeleteSelectedWorld(String reason) {
            if (worldIndex < 0 || worldIndex >= worlds.size()) { status = "Select an existing world to delete. The Generate New row is not a world file."; pendingWorldDeleteKey = ""; return; }
            WorldSaveInfo info = worlds.get(worldIndex);
            String key = info.seed + ":" + safe(info.hiveName);
            if (!key.equals(pendingWorldDeleteKey)) { pendingWorldDeleteKey = key; status = "Confirm delete " + safe(info.hiveName) + ": press Delete/D again or click Delete World again."; return; }
            java.nio.file.Path path = findWorldDefinitionFile(info);
            if (path == null) { status = "Could not locate a world definition file for " + safe(info.hiveName) + "."; pendingWorldDeleteKey = ""; DebugLog.warn("WORLD_START_FLOW", status); return; }
            try {
                java.nio.file.Files.deleteIfExists(path);
                status = "Deleted world " + safe(info.hiveName) + ".";
                panel.logEvent(status);
                DebugLog.audit("WORLD_START_FLOW", "deleted world reason=" + safe(reason) + " path=" + path);
                worlds = WorldSaveInfo.listExistingWorlds();
                worldIndex = worlds.isEmpty() ? 0 : Math.max(0, Math.min(worldIndex, worlds.size() - 1));
                clampWorldPickerSelection();
                pendingWorldDeleteKey = "";
                clearPreviewCache();
            } catch (Throwable t) { status = "Could not delete " + safe(info.hiveName) + ": " + t.getMessage(); pendingWorldDeleteKey = ""; DebugLog.error("WORLD_START_FLOW", status, t); }
        }

        java.nio.file.Path findWorldDefinitionFile(WorldSaveInfo info) {
            if (info != null && info.path != null && java.nio.file.Files.isRegularFile(info.path) && isWorldDefinitionPath(info.path)) return info.path;
            java.nio.file.Path direct = reflectivePath(info);
            if (direct != null && java.nio.file.Files.isRegularFile(direct) && isWorldDefinitionPath(direct)) return direct;
            java.nio.file.Path dir;
            try { dir = java.nio.file.Path.of(String.valueOf(CampaignWorldApi.worldDir())); } catch (Throwable t) { return null; }
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
                    try { String text = java.nio.file.Files.readString(path); if (text.contains(seedText)) score += 4; if (!nameText.isBlank() && text.contains(nameText)) score += 3; if (info.settings != null && text.contains(info.settings.encode())) score += 2; } catch (Throwable ignored) {}
                    if (score > bestScore) { bestScore = score; best = path; }
                }
            } catch (Throwable t) { DebugLog.warn("WORLD_START_FLOW", "Could not scan world directory for delete: " + t.getMessage()); }
            return bestScore >= 4 && best != null && isWorldDefinitionPath(best) ? best : null;
        }

        boolean isWorldDefinitionPath(java.nio.file.Path path) {
            try {
                java.nio.file.Path dir = CampaignWorldApi.worldDir().toAbsolutePath().normalize();
                java.nio.file.Path full = path.toAbsolutePath().normalize();
                String name = full.getFileName() == null ? "" : full.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
                return full.startsWith(dir) && name.endsWith(".mechworld");
            } catch (Throwable ignored) { return false; }
        }

        java.nio.file.Path reflectivePath(WorldSaveInfo info) {
            if (info == null) return null;
            for (String name : List.of("path", "file", "worldFile", "sourcePath", "definitionPath")) {
                try { java.lang.reflect.Field f = info.getClass().getDeclaredField(name); f.setAccessible(true); java.nio.file.Path p = pathFromObject(f.get(info)); if (p != null) return p; } catch (Throwable ignored) {}
                try { java.lang.reflect.Method m = info.getClass().getDeclaredMethod(name); m.setAccessible(true); java.nio.file.Path p = pathFromObject(m.invoke(info)); if (p != null) return p; } catch (Throwable ignored) {}
            }
            return null;
        }

        java.nio.file.Path pathFromObject(Object value) { if (value instanceof java.nio.file.Path p) return p; if (value instanceof java.io.File f) return f.toPath(); if (value instanceof CharSequence s && !s.toString().isBlank()) return java.nio.file.Path.of(s.toString()); return null; }

        void handleMouse(MouseEvent e) {
            if (stage == Stage.CLOSED || e == null) return;
            Point p = e.getPoint();
            if (stage == Stage.WORLD_PICKER) {
                if (buttonRect(0).contains(p)) { openWorldGeneration(); e.consume(); return; }
                if (buttonRect(1).contains(p)) { closeToMainMenu(); e.consume(); return; }
                if (buttonRect(2).contains(p)) { requestDeleteSelectedWorld("mouse"); e.consume(); repaintPanel(); return; }
                List<Rectangle> rows = worldPickerRows();
                for (int i = 0; i < rows.size(); i++) if (rows.get(i).contains(p)) { int actual = worldPickerScroll + i; worldIndex = actual; pendingWorldDeleteKey = ""; ensureWorldPickerSelectionVisible(); if (actual >= worlds.size() && e.getClickCount() >= 2) openWorldGeneration(); else if (actual < worlds.size() && e.getClickCount() >= 2) acceptExistingWorld(); e.consume(); repaintPanel(); return; }
            } else if (stage == Stage.WORLD_GENERATION) {
                if (seedEditRect().contains(p)) { beginSeedEdit(); e.consume(); repaintPanel(); return; }
                List<Rectangle> rows = worldGenerationRows();
                for (int i = 0; i < rows.size(); i++) if (rows.get(i).contains(p)) { worldGenerationOption = i; cycleWorldSetupSelected(); e.consume(); return; }
                if (buttonRect(0).contains(p)) { acceptGeneratedWorld(); e.consume(); return; }
                if (buttonRect(1).contains(p)) { openWorldPicker("mouse back"); e.consume(); return; }
            } else if (stage == Stage.CHARACTER_CREATION) {
                if (panel.characterNameEditRect.contains(p)) { beginNameEdit(); e.consume(); repaintPanel(); return; }
                List<Rectangle> rosterRows = characterRosterRows();
                for (int i = 0; i < rosterRows.size(); i++) if (rosterRows.get(i).contains(p)) { panel.candidateIndex = i; panel.active = panel.selectedNewGameCandidate(); panel.characterNameEditActive = false; nameEditing = false; status = "Selected candidate " + safe(panel.active == null ? "" : panel.active.name) + "."; e.consume(); repaintPanel(); return; }
                List<Rectangle> rows = characterActionRows();
                for (int i = 0; i < rows.size(); i++) if (rows.get(i).contains(p)) { characterOption = i; triggerCharacterAction(i); e.consume(); repaintPanel(); return; }
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
            } finally { g.dispose(); }
        }

        void paintWorldPicker(Graphics2D g, Rectangle r) {
            drawText(g, MenuTextAuthority.text("menu.world_manager.path", "Generated world definitions are read from: {0}").replace("{0}", String.valueOf(CampaignWorldApi.worldDir())), r.x + 24, r.y + 64, r.width - 48, muted());
            drawText(g, status, r.x + 24, r.y + 94, r.width - 48, main());
            List<Rectangle> rows = worldPickerRows();
            for (int i = 0; i < rows.size(); i++) { int actual = worldPickerScroll + i; drawWorldPickerTile(g, rows.get(i), actual, actual == worldIndex); }
            drawWorldPickerScrollBar(g);
            drawWorldManagerPreview(g, worldManagerPreviewRect());
            drawButton(g, buttonRect(0), MenuTextAuthority.text("menu.world_manager.button.generate", "Generate New"), true);
            drawButton(g, buttonRect(1), MenuTextAuthority.text("menu.world_manager.button.back", "Back"), false);
            drawDangerButton(g, buttonRect(2), MenuTextAuthority.text("menu.world_manager.button.delete", "Delete World"));
            drawFooter(g, MenuTextAuthority.text("menu.world_manager.footer", "Enter opens selected. Double-click row opens. N/G generates. Delete/D deletes selected world. Esc returns."));
        }

        void paintWorldGeneration(Graphics2D g, Rectangle r) {
            if (panel.worldSetup == null) panel.worldSetup = WorldSetupSettings.standard();
            drawText(g, MenuTextAuthority.text("menu.world_generation.header", "World Generation Options"), r.x + 24, r.y + 64, r.width - 48, highlight());
            drawText(g, panel.worldSetup.shortSummary(), r.x + 24, r.y + 94, r.width - 48, main());
            drawSeedEditor(g);
            ArrayList<String> lines = panel.worldSetup.detailLines();
            List<Rectangle> rows = worldGenerationRows();
            for (int i = 0; i < lines.size() && i < rows.size(); i++) { Rectangle row = rows.get(i); boolean selected = i == worldGenerationOption; fillRow(g, row, selected); drawText(g, (selected ? "> " : "  ") + lines.get(i), row.x + 12, row.y + 27, row.width - 24, selected ? highlight() : main()); }
            drawSpawnPreview(g, spawnPreviewRect());
            drawButton(g, buttonRect(0), "Generate World", true);
            drawButton(g, buttonRect(1), "Back", false);
            drawFooter(g, MenuTextAuthority.text("menu.world_generation.footer", "Up/Down chooses options. Left/Right cycles. Click seed or E edits. R rerolls seed. G generates."));
        }

        void paintCharacterCreation(Graphics2D g, Rectangle r) {
            Candidate c = panel.selectedNewGameCandidate();
            drawText(g, "Character Generation", r.x + 24, r.y + 64, r.width - 48, highlight());
            CharacterCreationAudit audit = CharacterCreationAuditApi.audit(panel.candidates);
            drawText(g, status + " / " + audit.summaryLine(), r.x + 24, r.y + 94, r.width - 48, main());
            drawCharacterRoster(g, characterRosterRect());
            drawCharacterSheet(g, characterSheetRect(), c);
            drawJobDossier(g, characterDossierRect(), c);
            drawCharacterRangeBar(g, characterRangeBarRect());
            drawButton(g, buttonRect(0), "Start Run", true);
            drawButton(g, buttonRect(1), "Back", false);
            drawFooter(g, MenuTextAuthority.text("menu.character_generation.footer", "Left/Right cycles candidate or job. J/K cycles jobs. R rerolls. E edits name. G starts run. Esc returns."));
        }

        void drawCharacterRoster(Graphics2D g, Rectangle r) {
            drawSubPanel(g, r, "Candidates");
            if (panel.candidates.isEmpty()) panel.prepareNewGameRoster(System.currentTimeMillis());
            List<Rectangle> rows = characterRosterRows();
            for (int i = 0; i < rows.size() && i < panel.candidates.size(); i++) {
                Rectangle row = rows.get(i);
                boolean selected = i == panel.candidateIndex;
                fillRow(g, row, selected);
                String label = (selected ? "> " : "") + (i + 1);
                drawFitted(g, label, row.x + 12, row.y + row.height / 2 + 5, row.width - 24, selected ? highlight() : main());
            }
        }

        void drawCharacterSheet(Graphics2D g, Rectangle r, Candidate c) {
            drawSubPanel(g, r, "Candidate");
            if (c == null) {
                panel.characterNameEditRect = new Rectangle(0, 0, 1, 1);
                drawText(g, "No character candidates are available.", r.x + 12, r.y + 38, r.width - 24, main());
                return;
            }
            int portraitSize = Math.max(88, Math.min(128, Math.min(r.width - 24, r.height / 3)));
            Rectangle portraitRect = new Rectangle(r.x + 12, r.y + 34, portraitSize, portraitSize);
            drawPortraitFrame(g, c, portraitRect);

            int nameX = portraitRect.x + portraitRect.width + 14;
            int nameY = portraitRect.y + 22;
            if (nameX + 150 > r.x + r.width - 12) {
                nameX = r.x + 12;
                nameY = portraitRect.y + portraitRect.height + 24;
            }
            int nameW = Math.max(120, r.x + r.width - nameX - 12);
            panel.characterNameEditRect = new Rectangle(nameX, nameY, nameW, 30);
            drawNameEditor(g, c);
            int metaY = panel.characterNameEditRect.y + 50;
            drawCompactLine(g, "Candidate " + (panel.candidateIndex + 1) + "/" + Math.max(1, panel.candidates.size()) + " / Portrait " + c.portraitIndex, nameX, metaY, nameW, muted());
            drawCompactLine(g, "Age " + c.ageYears + " years / " + safe(c.ageBand), nameX, metaY + 18, nameW, muted());

            int statsY = Math.max(portraitRect.y + portraitRect.height + 16, metaY + 38);
            int statsH = Math.max(216, Math.min(256, r.y + r.height - statsY - 12));
            Rectangle stats = new Rectangle(r.x + 12, statsY, r.width - 24, statsH);
            drawSubPanel(g, stats, "Stats");
            drawStatRanges(g, c, stats);
        }

        void drawJobDossier(Graphics2D g, Rectangle r, Candidate c) {
            drawSubPanel(g, r, "Job Dossier");
            Rectangle actionTop = characterActionRows().isEmpty() ? new Rectangle(r.x, r.y + r.height, 1, 1) : characterActionRows().get(0);
            int detailY = r.y + 34;
            ArrayList<String> lines = new ArrayList<>();
            if (c == null) {
                lines.add("No selected candidate.");
            } else {
                JobProfile profile = JobProfile.get(c.job);
                int jobIdx = c.jobs.indexOf(c.job);
                lines.add("Job " + (jobIdx < 0 ? 1 : jobIdx + 1) + "/" + Math.max(1, c.jobs.size()) + ": " + safe(c.job));
                if (profile != null) {
                    boolean valid = profile.meets(c);
                    Rectangle validity = new Rectangle(r.x + 12, detailY, r.width - 24, valid ? 42 : 58);
                    drawJobValidity(g, validity, valid, profile.missingText(c));
                    detailY += validity.height + 8;
                    lines.add("Faction: " + profile.faction.label);
                    lines.add("Clothing: " + profile.clothingName + " / disguise " + profile.disguiseBase + " / defense " + profile.clothingDefense);
                    lines.add("Requirements: " + profile.requirementText());
                    lines.add("Bonuses: " + profile.bonusText());
                    lines.add("Penalties: " + profile.penaltyText());
                    lines.add("Starting kit: " + joinLimited(profile.startingItems(), 4));
                    lines.add("Profile: " + profile.description);
                }
            }
            Rectangle details = new Rectangle(r.x + 12, detailY, r.width - 24, Math.max(60, actionTop.y - detailY - 8));
            drawLinesClipped(g, lines, details, main());
            drawCharacterActions(g);
        }

        void drawJobValidity(Graphics2D g, Rectangle r, boolean valid, String missing) {
            Color fill = valid ? new Color(18, 46, 28, 230) : new Color(66, 22, 16, 235);
            Color border = valid ? new Color(90, 172, 108, 190) : new Color(224, 92, 66, 210);
            Color text = valid ? new Color(170, 232, 176) : new Color(255, 184, 150);
            g.setColor(fill);
            g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g.setColor(border);
            g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            drawFitted(g, valid ? "JOB VALID" : "INVALID JOB", r.x + 10, r.y + 18, r.width - 20, text);
            drawFitted(g, valid ? "Requirements met." : "Missing: " + safe(missing), r.x + 10, r.y + 36, r.width - 20, text);
        }

        void drawCharacterActions(Graphics2D g) {
            List<Rectangle> rows = characterActionRows();
            g.setFont(panel.smallFont);
            for (int i = 0; i < rows.size() && i < CHARACTER_ACTION_LABELS.length; i++) {
                Rectangle row = rows.get(i);
                boolean selected = i == characterOption;
                fillRow(g, row, selected);
                drawFitted(g, (selected ? "> " : "") + CHARACTER_ACTION_LABELS[i], row.x + 9, row.y + 19, row.width - 18, selected ? highlight() : main());
            }
        }

        void drawPortraitFrame(Graphics2D g, Candidate c, Rectangle r) {
            g.setColor(new Color(12, 14, 13, 230));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g.setColor(new Color(145, 118, 64, 170));
            g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            BufferedImage img = panel.images.getPlayerPortrait(c);
            if (img != null) g.drawImage(img, r.x + 8, r.y + 8, r.width - 16, r.height - 16, null);
            else drawAsciiPortrait(g, c, r);
        }

        void drawNameEditor(Graphics2D g, Candidate c) {
            Rectangle name = panel.characterNameEditRect;
            g.setFont(panel.smallFont);
            g.setColor(muted());
            drawBackedText(g, "Name", name.x, name.y - 8, false);
            g.setColor(nameEditing ? new Color(54, 47, 31, 238) : new Color(16, 18, 16, 224));
            g.fillRoundRect(name.x, name.y, name.width, name.height, 8, 8);
            g.setColor(nameEditing ? highlight() : new Color(145, 118, 64, 140));
            g.drawRoundRect(name.x, name.y, name.width, name.height, 8, 8);
            drawFitted(g, (c.name == null ? "" : c.name) + (nameEditing ? "|" : ""), name.x + 8, name.y + 21, name.width - 16, nameEditing ? highlight() : main());
        }

        void drawStatRanges(Graphics2D g, Candidate c, Rectangle r) {
            if (c == null) return;
            ArrayList<Map.Entry<String, Integer>> entries = new ArrayList<>(c.stats.entrySet());
            int count = Math.max(1, entries.size());
            int rows = count <= 6 ? 1 : 2;
            int cols = Math.max(1, (int)Math.ceil(count / (double)rows));
            int gap = 6;
            int x0 = r.x + 12;
            int y0 = r.y + 34;
            int availableW = Math.max(1, r.width - 24);
            int availableH = Math.max(1, r.height - 46);
            int cellW = Math.max(48, (availableW - Math.max(0, cols - 1) * gap) / cols);
            int cellH = Math.max(44, (availableH - Math.max(0, rows - 1) * gap) / rows);
            for (int idx = 0; idx < entries.size(); idx++) {
                Map.Entry<String, Integer> entry = entries.get(idx);
                int row = idx / cols;
                int col = idx % cols;
                Rectangle cell = new Rectangle(x0 + col * (cellW + gap), y0 + row * (cellH + gap), cellW, cellH);
                drawStatCell(g, cell, entry.getKey(), entry.getValue());
            }
        }

        void drawStatCell(Graphics2D g, Rectangle r, String key, int value) {
            g.setColor(new Color(9, 11, 10, 220));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 7, 7);
            g.setColor(new Color(145, 118, 64, 120));
            g.drawRoundRect(r.x, r.y, r.width, r.height, 7, 7);
            int pad = Math.max(5, Math.min(8, r.width / 12));
            drawFitted(g, key + " " + value, r.x + pad, r.y + 17, r.width - pad * 2, highlight());
            drawFitted(g, statExplanation(key), r.x + pad, r.y + 37, r.width - pad * 2, main());
        }

        void drawCharacterRangeBar(Graphics2D g, Rectangle r) {
            drawSubPanel(g, r, null);
            int gap = 12;
            int colW = Math.max(120, (r.width - 24 - gap) / 2);
            int leftX = r.x + 12;
            int rightX = leftX + colW + gap;
            int baseline = r.y + 24;
            drawCompactLine(g, "Stat ranges: Strength through Hearing roll " + Candidate.statRangeText("Strength") + " before job changes.", leftX, baseline, colW, muted());
            drawCompactLine(g, "Body END/AGI start 1-2; job requirements validate now; bonuses apply after selection.", rightX, baseline, Math.max(80, r.x + r.width - rightX - 12), muted());
        }

        Rectangle characterBodyRect() {
            Rectangle p = panelRect(getWidth(), getHeight());
            return new Rectangle(p.x + 24, p.y + 112, p.width - 48, Math.max(300, p.height - 230));
        }

        Rectangle characterRangeBarRect() {
            Rectangle body = characterBodyRect();
            return new Rectangle(body.x, body.y + body.height + 10, body.width, 34);
        }

        Rectangle characterRosterRect() {
            Rectangle body = characterBodyRect();
            int leftW = Math.max(84, Math.min(118, body.width / 10));
            return new Rectangle(body.x, body.y, leftW, body.height);
        }

        Rectangle characterDossierRect() {
            Rectangle body = characterBodyRect();
            int gap = 14;
            int leftW = characterRosterRect().width;
            int rightW = Math.max(300, Math.min(380, body.width / 3));
            int midW = body.width - leftW - rightW - gap * 2;
            if (midW < 440) rightW = Math.max(260, rightW - (440 - midW));
            return new Rectangle(body.x + body.width - rightW, body.y, rightW, body.height);
        }

        Rectangle characterSheetRect() {
            Rectangle body = characterBodyRect();
            Rectangle roster = characterRosterRect();
            Rectangle dossier = characterDossierRect();
            int gap = 14;
            int x = roster.x + roster.width + gap;
            int w = Math.max(190, dossier.x - x - gap);
            return new Rectangle(x, body.y, w, body.height);
        }

        List<Rectangle> characterRosterRows() {
            Rectangle r = characterRosterRect();
            ArrayList<Rectangle> rows = new ArrayList<>();
            int count = panel == null || panel.candidates == null ? 0 : panel.candidates.size();
            if (count <= 0) return rows;
            int top = r.y + 36;
            int available = Math.max(24, r.height - 48);
            int gap = 5;
            int rowH = Math.max(24, Math.min(34, (available - Math.max(0, count - 1) * gap) / count));
            for (int i = 0; i < count; i++) {
                int y = top + i * (rowH + gap);
                if (y + rowH > r.y + r.height - 8) break;
                rows.add(new Rectangle(r.x + 10, y, r.width - 20, rowH));
            }
            return rows;
        }

        List<Rectangle> characterActionRows() {
            Rectangle r = characterDossierRect();
            ArrayList<Rectangle> rows = new ArrayList<>();
            int cols = 2;
            int rowH = 28;
            int gap = 6;
            int gridRows = (int)Math.ceil(CHARACTER_ACTION_COUNT / (double)cols);
            int gridH = gridRows * rowH + Math.max(0, gridRows - 1) * gap;
            int top = r.y + r.height - gridH - 10;
            int colW = Math.max(88, (r.width - 24 - gap) / cols);
            for (int i = 0; i < CHARACTER_ACTION_COUNT; i++) {
                int row = i / cols;
                int col = i % cols;
                rows.add(new Rectangle(r.x + 12 + col * (colW + gap), top + row * (rowH + gap), colW, rowH));
            }
            return rows;
        }

        void drawSubPanel(Graphics2D g, Rectangle r, String title) {
            g.setColor(new Color(12, 14, 13, 230));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 9, 9);
            g.setColor(new Color(145, 118, 64, 150));
            g.drawRoundRect(r.x, r.y, r.width, r.height, 9, 9);
            if (title != null && !title.isBlank()) {
                g.setFont(panel.smallFont.deriveFont(Font.BOLD));
                g.setColor(highlight());
                drawBackedText(g, title, r.x + 12, r.y + 22, false);
            }
        }

        void drawLinesClipped(Graphics2D g, List<String> lines, Rectangle area, Color color) {
            java.awt.Shape oldClip = g.getClip();
            g.clip(area);
            drawLines(g, lines, area.x, area.y + 16, area.width, color);
            g.setClip(oldClip);
        }

        void drawCompactLine(Graphics2D g, String text, int x, int baseline, int width, Color color) {
            g.setFont(panel.smallFont);
            FontMetrics fm = g.getFontMetrics();
            int h = Math.max(15, fm.getHeight() + 1);
            Color old = g.getColor();
            g.setColor(new Color(0, 0, 0, 96));
            g.fillRoundRect(x - 4, baseline - fm.getAscent() - 3, Math.max(20, width + 8), h + 4, 6, 6);
            g.setColor(color == null ? old : color);
            g.drawString(GuiLayoutApi.fitLabel(text == null ? "" : text, fm, Math.max(8, width)), x, baseline);
            g.setColor(old);
        }

        void drawFitted(Graphics2D g, String text, int x, int baseline, int width, Color color) {
            g.setFont(panel.smallFont);
            Color old = g.getColor();
            g.setColor(color == null ? old : color);
            g.drawString(GuiLayoutApi.fitLabel(text == null ? "" : text, g.getFontMetrics(), Math.max(8, width)), x, baseline);
            g.setColor(old);
        }

        String statExplanation(String key) {
            return switch (key == null ? "" : key) {
                case "Strength" -> "carry / force / labor";
                case "Agility" -> "move / dodge / balance";
                case "Endurance" -> "wounds / fatigue / toxins";
                case "Intellect" -> "research / logic / systems";
                case "Mechanics" -> "repair / craft / machines";
                case "Firearms" -> "aim / reload / ranged";
                case "Melee" -> "close combat / grapples";
                case "Nerve" -> "fear / pain / suppression";
                case "Charm" -> "talk / barter / deception";
                case "Faith" -> "rites / resolve / authority";
                case "Vision" -> "sight / spotting";
                case "Hearing" -> "sound / tracking";
                default -> "general checks";
            };
        }

        String joinLimited(List<String> values, int limit) {
            if (values == null || values.isEmpty()) return "none";
            ArrayList<String> out = new ArrayList<>();
            for (String value : values) {
                if (value == null || value.isBlank()) continue;
                if (out.size() >= limit) break;
                out.add(value);
            }
            int extra = Math.max(0, values.size() - out.size());
            return String.join(", ", out) + (extra > 0 ? " +" + extra : "");
        }

        void drawSeedEditor(Graphics2D g) { Rectangle seed = seedEditRect(); g.setFont(panel.smallFont); g.setColor(muted()); drawBackedText(g, MenuTextAuthority.text("menu.world_generation.seed", "Seed"), seed.x, seed.y - 8, false); g.setColor(seedEditing ? new Color(54, 47, 31, 238) : new Color(16, 18, 16, 224)); g.fillRoundRect(seed.x, seed.y, seed.width, seed.height, 8, 8); g.setColor(seedEditing ? highlight() : new Color(145, 118, 64, 140)); g.drawRoundRect(seed.x, seed.y, seed.width, seed.height, 8, 8); String text = seedEditing ? seedDraft + "_" : Long.toString(panel.seed); drawText(g, text, seed.x + 10, seed.y + 24, seed.width - 20, seedEditing ? highlight() : main()); }

        void drawWorldManagerPreview(Graphics2D g, Rectangle r) { WorldSaveInfo info = selectedWorldInfo(); if (info != null) requestPreviewFor(info.seed, info.settings == null ? WorldSetupSettings.standard() : info.settings.copy(), "existing:" + info.seed + ":" + safe(info.hiveName), safe(info.hiveName) + " / seed " + info.seed); else { long seed = ensureDraftSeed(); WorldSetupSettings setup = panel.worldSetup == null ? WorldSetupSettings.standard() : panel.worldSetup.copy(); requestPreviewFor(seed, setup, "new:" + seed + ":" + setup.encode(), "Generate New World / seed " + seed); } drawSpawnPreviewBody(g, r, info == null ? MenuTextAuthority.text("menu.world_manager.preview.new", "Preview: Generate New World") : MenuTextAuthority.text("menu.world_manager.preview.selected", "Selected World Spawn Preview")); }
        WorldSaveInfo selectedWorldInfo() { return worldIndex >= 0 && worldIndex < worlds.size() ? worlds.get(worldIndex) : null; }
        void drawSpawnPreview(Graphics2D g, Rectangle r) { if (panel.worldSetup == null) panel.worldSetup = WorldSetupSettings.standard(); long safeSeed = ensureDraftSeed(); requestPreviewFor(safeSeed, panel.worldSetup.copy(), "generation:" + safeSeed + ":" + panel.worldSetup.encode(), "Generated hive / seed " + safeSeed); drawSpawnPreviewBody(g, r, MenuTextAuthority.text("menu.world_generation.preview", "Spawn Sector Preview")); }

        void drawSpawnPreviewBody(Graphics2D g, Rectangle r, String title) {
            g.setColor(new Color(12, 14, 13, 230)); g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10); g.setColor(new Color(145, 118, 64, 170)); g.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10); drawText(g, title, r.x + 14, r.y + 28, r.width - 28, highlight()); drawText(g, previewStatus, r.x + 14, r.y + 54, r.width - 28, muted()); if (previewWorld == null) return;
            Rectangle map = new Rectangle(r.x + 14, r.y + 78, r.width - 28, Math.max(120, r.height - 122)); g.setColor(new Color(4, 5, 5, 238)); g.fillRect(map.x, map.y, map.width, map.height); Point spawn = previewWorld.startPoint();
            BufferedImage image = previewOverviewImage;
            if (image != null) {
                int drawW = map.width;
                int drawH = Math.max(1, image.getHeight() * map.width / Math.max(1, image.getWidth()));
                if (drawH > map.height) {
                    drawH = map.height;
                    drawW = Math.max(1, image.getWidth() * map.height / Math.max(1, image.getHeight()));
                }
                int ox = map.x + Math.max(0, (map.width - drawW) / 2);
                int oy = map.y + Math.max(0, (map.height - drawH) / 2);
                Object oldHint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g.drawImage(image, ox, oy, drawW, drawH, null);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldHint == null ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : oldHint);
                if (previewWorld.inBounds(spawn.x, spawn.y)) {
                    int sx = ox + (int)Math.round((spawn.x + 0.5) * drawW / Math.max(1.0, previewWorld.w));
                    int sy = oy + (int)Math.round((spawn.y + 0.5) * drawH / Math.max(1.0, previewWorld.h));
                    int mark = Math.max(4, Math.min(14, Math.min(drawW, drawH) / 28));
                    g.setColor(new Color(245, 214, 118));
                    g.setStroke(new BasicStroke(Math.max(1f, mark / 3f)));
                    g.drawRect(sx - mark / 2, sy - mark / 2, mark, mark);
                }
            }
            drawText(g, "Full zone " + previewWorld.w + "x" + previewWorld.h + " / spawn " + spawn.x + "," + spawn.y + " / " + previewWorld.zoneType.label, r.x + 14, r.y + r.height - 28, r.width - 28, main());
        }

        void requestPreviewFor(long seed, WorldSetupSettings setup, String key, String label) {
            WorldSetupSettings safeSetup = setup == null ? WorldSetupSettings.standard() : setup.copy();
            String safeKey = key == null ? "preview:" + seed + ":" + safeSetup.encode() : key;
            if (safeKey.equals(previewKey)) return;
            if (safeKey.equals(previewRequestedKey) && previewWorkerRunning) return;
            if (pendingPreviewRequest != null && safeKey.equals(pendingPreviewRequest.key)) return;
            previewRequestedKey = safeKey;
            long request = ++previewRequestSerial;
            PreviewRequest previewRequest = new PreviewRequest(seed, safeSetup.copy(), safeKey, label, request);
            if (previewWorkerRunning) {
                pendingPreviewRequest = previewRequest;
                previewStatus = "Loading preview...";
                return;
            }
            startPreviewWorker(previewRequest);
        }

        void startPreviewWorker(PreviewRequest previewRequest) {
            if (previewRequest == null) return;
            previewWorkerRunning = true;
            previewAtlas = null;
            previewWorld = null;
            previewOverviewImage = null;
            previewStatus = "Loading preview...";
            Thread worker = new Thread(() -> {
                WorldAtlas generatedAtlas = null;
                World generatedWorld = null;
                BufferedImage generatedImage = null;
                String generatedStatus;
                try {
                    generatedAtlas = WorldAtlas.preview(previewRequest.seed, previewRequest.setup.copy());
                    generatedAtlas.generateScaffold();
                    generatedWorld = generatedAtlas.currentWorld();
                    generatedImage = buildPreviewOverviewImage(generatedWorld);
                    generatedStatus = previewRequest.label == null || previewRequest.label.isBlank() ? (generatedAtlas.hiveWorld == null ? "generated hive" : generatedAtlas.hiveWorld.hiveName) : previewRequest.label;
                } catch (Throwable t) {
                    generatedStatus = "Preview unavailable: " + t.getMessage();
                    DebugLog.warn("WORLD_START_FLOW", generatedStatus);
                }
                WorldAtlas finalAtlas = generatedAtlas;
                World finalWorld = generatedWorld;
                BufferedImage finalImage = generatedImage;
                String finalStatus = generatedStatus;
                SwingUtilities.invokeLater(() -> {
                    boolean current = previewRequest.serial == previewRequestSerial && previewRequest.key.equals(previewRequestedKey);
                    previewWorkerRunning = false;
                    if (current) {
                        previewAtlas = finalAtlas;
                        previewWorld = finalWorld;
                        previewOverviewImage = finalImage;
                        previewKey = previewRequest.key;
                        previewStatus = finalStatus;
                    }
                    PreviewRequest pending = pendingPreviewRequest;
                    pendingPreviewRequest = null;
                    if (pending != null && !pending.key.equals(previewKey)) startPreviewWorker(pending);
                    repaintPanel();
                });
            }, "mechanist-world-preview-" + previewRequest.serial);
            worker.setDaemon(true);
            worker.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 2));
            worker.start();
        }
        BufferedImage buildPreviewOverviewImage(World world) {
            if (world == null || world.w <= 0 || world.h <= 0) return null;
            BufferedImage image = new BufferedImage(world.w, world.h, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < world.h; y++) {
                for (int x = 0; x < world.w; x++) {
                    image.setRGB(x, y, previewTileColor(world.tiles[x][y]).getRGB());
                }
            }
            return image;
        }
        void clearPreviewCache() { previewRequestSerial++; pendingPreviewRequest = null; previewAtlas = null; previewWorld = null; previewOverviewImage = null; previewKey = ""; previewRequestedKey = ""; previewStatus = ""; }
        Color previewTileColor(char ch) { if (ch == '#') return new Color(38, 39, 36); if (ch == '.' || ch == ',' || ch == ':' || ch == ';' || ch == '=') return new Color(47, 48, 43); if (ch == '+' || ch == '/' || ch == '\\' || ch == 'D') return new Color(91, 76, 43); if (ch == '~') return new Color(35, 57, 58); if (Character.isUpperCase(ch)) return new Color(72, 58, 36); return new Color(28, 29, 27); }
        List<Rectangle> worldPickerRows() {
            Rectangle p = panelRect(getWidth(), getHeight());
            clampWorldPickerScroll(p);
            ArrayList<Rectangle> rows = new ArrayList<>();
            int count = worlds.size() + 1;
            int max = Math.min(count - worldPickerScroll, worldPickerVisibleCapacity(p));
            int top = worldPickerListTop(p);
            for (int i = 0; i < max; i++) rows.add(new Rectangle(p.x + 24, top + i * (WORLD_PICKER_ROW_HEIGHT + WORLD_PICKER_ROW_GAP), worldPickerListWidth(p), WORLD_PICKER_ROW_HEIGHT));
            return rows;
        }
        int worldPickerListTop(Rectangle p) { return p.y + 130; }
        int worldPickerListBottom(Rectangle p) { return p.y + p.height - 112; }
        int worldPickerVisibleCapacity(Rectangle p) { int available = Math.max(WORLD_PICKER_ROW_HEIGHT, worldPickerListBottom(p) - worldPickerListTop(p)); return Math.max(1, Math.min(worlds.size() + 1, (available + WORLD_PICKER_ROW_GAP) / (WORLD_PICKER_ROW_HEIGHT + WORLD_PICKER_ROW_GAP))); }
        void clampWorldPickerSelection() { int count = Math.max(1, worlds.size() + 1); worldIndex = Math.max(0, Math.min(worldIndex, count - 1)); ensureWorldPickerSelectionVisible(); }
        void ensureWorldPickerSelectionVisible() { Rectangle p = panelRect(getWidth(), getHeight()); int visible = worldPickerVisibleCapacity(p); if (worldIndex < worldPickerScroll) worldPickerScroll = worldIndex; if (worldIndex >= worldPickerScroll + visible) worldPickerScroll = worldIndex - visible + 1; clampWorldPickerScroll(p); }
        void clampWorldPickerScroll(Rectangle p) { int visible = worldPickerVisibleCapacity(p); int maxScroll = Math.max(0, worlds.size() + 1 - visible); worldPickerScroll = Math.max(0, Math.min(worldPickerScroll, maxScroll)); }
        void scrollWorldPicker(int wheelRotation) { Rectangle p = panelRect(getWidth(), getHeight()); clampWorldPickerScroll(p); int maxScroll = Math.max(0, worlds.size() + 1 - worldPickerVisibleCapacity(p)); int step = wheelRotation == 0 ? 0 : (wheelRotation > 0 ? 1 : -1); worldPickerScroll = Math.max(0, Math.min(maxScroll, worldPickerScroll + step * Math.max(1, Math.abs(wheelRotation)))); pendingWorldDeleteKey = ""; }
        void drawWorldPickerTile(Graphics2D g, Rectangle row, int rowIndex, boolean selected) {
            fillRow(g, row, selected);
            Shape oldClip = g.getClip();
            g.clip(row);
            Color text = selected ? highlight() : main();
            int x = row.x + 14;
            int y = row.y + 24;
            int width = row.width - 28;
            if (rowIndex < worlds.size()) {
                WorldSaveInfo info = worlds.get(rowIndex);
                drawPlainWrapped(g, (selected ? "> " : "") + safe(info.hiveName), x, y, width, 1, text, true);
                drawPlainWrapped(g, "[" + safe(info.worldId) + "] seed " + info.seed, x, y + 20, width, 1, muted(), false);
                drawPlainWrapped(g, info.settings.shortSummary(), x, y + 40, width, 1, text, false);
                drawPlainWrapped(g, "Progress " + safe(info.progress), x, y + 60, width, 1, muted(), false);
            } else {
                drawPlainWrapped(g, (selected ? "> " : "") + "+ " + MenuTextAuthority.text("menu.world_manager.button.generate", "Generate New World"), x, y, width, 1, highlight(), true);
                drawPlainWrapped(g, "Open the dedicated world generation options before character creation.", x, y + 28, width, 2, text, false);
            }
            g.setClip(oldClip);
        }
        void drawWorldPickerScrollBar(Graphics2D g) {
            Rectangle p = panelRect(getWidth(), getHeight());
            int count = worlds.size() + 1;
            int visible = worldPickerVisibleCapacity(p);
            if (count <= visible) return;
            int x = p.x + 24 + worldPickerListWidth(p) - 8;
            int y = worldPickerListTop(p);
            int h = Math.max(40, visible * WORLD_PICKER_ROW_HEIGHT + Math.max(0, visible - 1) * WORLD_PICKER_ROW_GAP);
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRoundRect(x, y, 6, h, 6, 6);
            int thumbH = Math.max(24, h * visible / count);
            int maxScroll = Math.max(1, count - visible);
            int thumbY = y + (h - thumbH) * worldPickerScroll / maxScroll;
            g.setColor(new Color(180, 145, 70, 210));
            g.fillRoundRect(x, thumbY, 6, thumbH, 6, 6);
        }
        void drawPlainWrapped(Graphics2D g, String text, int x, int y, int width, int maxLines, Color color, boolean bold) {
            Font oldFont = g.getFont();
            g.setFont(bold ? panel.smallFont.deriveFont(Font.BOLD) : panel.smallFont);
            FontMetrics fm = g.getFontMetrics();
            List<String> lines = GuiLayoutApi.wrapText(text == null ? "" : text, fm, Math.max(8, width));
            int lineH = Math.max(15, fm.getHeight() + 1);
            g.setColor(color == null ? main() : color);
            for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
                String line = lines.get(i);
                if (i == maxLines - 1 && lines.size() > maxLines) line = fitText(fm, line + "...", width);
                else line = fitText(fm, line, width);
                g.drawString(line, x, y + i * lineH);
            }
            g.setFont(oldFont);
        }
        String fitText(FontMetrics fm, String text, int width) {
            String value = text == null ? "" : text;
            if (fm.stringWidth(value) <= width) return value;
            String suffix = "...";
            int end = value.length();
            while (end > 0 && fm.stringWidth(value.substring(0, end) + suffix) > width) end--;
            return end <= 0 ? suffix : value.substring(0, end) + suffix;
        }
        long ensureDraftSeed() { if (panel.seed == 0L) { panel.seed = System.currentTimeMillis(); panel.rng = new Random(panel.seed ^ 0x4E475345545550L); seedDraft = Long.toString(panel.seed); } return panel.seed; }
        int worldPickerListWidth(Rectangle p) { int previewW = Math.max(300, Math.min(430, p.width / 3)); return Math.max(280, p.width - 72 - previewW - 22); }
        Rectangle worldManagerPreviewRect() { Rectangle p = panelRect(getWidth(), getHeight()); int leftW = worldPickerListWidth(p); int x = p.x + 24 + leftW + 22; int w = Math.max(300, p.x + p.width - 28 - x); return new Rectangle(x, p.y + 130, w, Math.max(300, p.height - 220)); }
        List<Rectangle> worldGenerationRows() { Rectangle p = panelRect(getWidth(), getHeight()); ArrayList<Rectangle> rows = new ArrayList<>(); int top = p.y + 166; int width = Math.max(300, Math.min(540, p.width - 420)); for (int i = 0; i < 7; i++) rows.add(new Rectangle(p.x + 24, top + i * 42, width, 34)); return rows; }
        List<Rectangle> optionRows(int count) { Rectangle p = panelRect(getWidth(), getHeight()); ArrayList<Rectangle> rows = new ArrayList<>(); int top = p.y + 124; for (int i = 0; i < count; i++) rows.add(new Rectangle(p.x + 24, top + i * 42, p.width - 48, 34)); return rows; }
        Rectangle seedEditRect() { Rectangle p = panelRect(getWidth(), getHeight()); int width = Math.max(300, Math.min(540, p.width - 420)); return new Rectangle(p.x + 24, p.y + 118, width, 34); }
        Rectangle spawnPreviewRect() { Rectangle p = panelRect(getWidth(), getHeight()); int leftW = Math.max(300, Math.min(540, p.width - 420)); int x = p.x + 24 + leftW + 24; int w = Math.max(300, p.x + p.width - 28 - x); return new Rectangle(x, p.y + 118, w, Math.max(320, p.height - 206)); }
        Rectangle panelRect(int w, int h) { int minW = stage == Stage.CHARACTER_CREATION ? 900 : 720; int maxW = stage == Stage.CHARACTER_CREATION ? 1320 : 1060; int margin = stage == Stage.CHARACTER_CREATION ? 48 : 72; int pw = Math.max(minW, Math.min(maxW, w - margin)); int ph = Math.max(stage == Stage.CHARACTER_CREATION ? 620 : 540, Math.min(stage == Stage.CHARACTER_CREATION ? 760 : 720, h - 72)); return new Rectangle(Math.max(20, w / 2 - pw / 2), Math.max(28, h / 2 - ph / 2), pw, ph); }
        Rectangle buttonRect(int index) { Rectangle p = panelRect(getWidth(), getHeight()); if (index == 2) return new Rectangle(p.x + 28, p.y + p.height - 58, 146, 34); int bw = index == 0 ? 178 : 120; int gap = 16; int x = index == 0 ? p.x + p.width - bw - 28 : p.x + p.width - bw - 28 - 178 - gap; return new Rectangle(x, p.y + p.height - 58, bw, 34); }
        String title() { return MenuTextAuthority.text(titleKey(), switch (stage) { case WORLD_PICKER -> "WORLD MANAGEMENT"; case WORLD_GENERATION -> "NEW WORLD GENERATION"; case CHARACTER_CREATION -> "CHARACTER GENERATION"; default -> "START FLOW"; }); }
        String titleKey() { return switch (stage) { case WORLD_PICKER -> "menu.world_manager.title"; case WORLD_GENERATION -> "menu.world_generation.title"; case CHARACTER_CREATION -> "menu.character_generation.title"; default -> ""; }; }
        String menuReferenceId() { return switch (stage) { case WORLD_PICKER -> "M003"; case WORLD_GENERATION -> "M004"; case CHARACTER_CREATION -> "M005"; default -> "M000"; }; }
        void drawFrame(Graphics2D g, Rectangle r, String title) { g.setColor(new Color(9, 10, 9, 238)); g.fillRoundRect(r.x, r.y, r.width, r.height, 14, 14); g.setColor(new Color(120, 96, 48, 210)); g.setStroke(new BasicStroke(2f)); g.drawRoundRect(r.x, r.y, r.width, r.height, 14, 14); g.setFont(panel.titleFont.deriveFont(Font.BOLD, 30f)); g.setColor(highlight()); center(g, title, r.x + r.width / 2, r.y + 38); MenuTextAuthority.drawMenuReference(g, panel, r, menuReferenceId(), titleKey(), title); }
        void fillRow(Graphics2D g, Rectangle r, boolean selected) { g.setColor(selected ? new Color(76, 62, 32, 232) : new Color(16, 18, 16, 224)); g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8); g.setColor(selected ? highlight() : new Color(145, 118, 64, 120)); g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8); }
        void drawButton(Graphics2D g, Rectangle r, String label, boolean primary) { drawButtonWithColor(g, r, label, primary ? new Color(76, 62, 32, 232) : new Color(16, 18, 16, 224), primary ? highlight() : main(), primary ? highlight() : new Color(145, 118, 64, 140)); }
        void drawDangerButton(Graphics2D g, Rectangle r, String label) { drawButtonWithColor(g, r, label, new Color(58, 18, 14, 232), new Color(236, 168, 142), new Color(192, 74, 58, 180)); }
        void drawButtonWithColor(Graphics2D g, Rectangle r, String label, Color fill, Color text, Color border) { g.setColor(fill); g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8); g.setColor(border); g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8); g.setColor(text); BufferedImage icon = panel.systemButtonIconForLabel(label); FontMetrics fm = g.getFontMetrics(); int iconSize = icon == null ? 0 : Math.max(18, Math.min(r.height - 6, 30)); int labelW = fm.stringWidth(label); int groupW = labelW + (iconSize > 0 ? iconSize + 8 : 0); int x = r.x + Math.max(8, (r.width - groupW) / 2); if (icon != null) { g.drawImage(icon, x, r.y + (r.height - iconSize) / 2, iconSize, iconSize, null); x += iconSize + 8; } drawBackedText(g, label, x, r.y + 23, false); }
        void drawFooter(Graphics2D g, String text) { Rectangle p = panelRect(getWidth(), getHeight()); g.setFont(panel.smallFont); g.setColor(muted()); center(g, text, p.x + p.width / 2, p.y + p.height - 78); }
        void drawText(Graphics2D g, String text, int x, int y, int width, Color color) { g.setFont(panel.smallFont); FontMetrics fm = g.getFontMetrics(); List<String> lines = GuiLayoutApi.wrapText(text == null ? "" : text, fm, Math.max(8, width)); drawWrappedTextBlock(g, lines, x, y, width, color, false); }
        void drawLines(Graphics2D g, List<String> lines, int x, int y, int width, Color color) { g.setFont(panel.smallFont); ArrayList<String> wrapped = new ArrayList<>(); FontMetrics fm = g.getFontMetrics(); if (lines != null) for (String line : lines) wrapped.addAll(GuiLayoutApi.wrapText(line == null ? "" : line, fm, Math.max(8, width))); drawWrappedTextBlock(g, wrapped, x, y, width, color, false); }
        void drawWrappedTextBlock(Graphics2D g, List<String> lines, int x, int firstBaseline, int width, Color color, boolean centered) { if (g == null || lines == null || lines.isEmpty()) return; FontMetrics fm = g.getFontMetrics(); int lineH = Math.max(15, fm.getHeight() + 2); int maxW = 0; for (String line : lines) maxW = Math.max(maxW, fm.stringWidth(line == null ? "" : line)); int bx = centered ? x - maxW / 2 - 7 : x - 7; int by = firstBaseline - fm.getAscent() - 5; int bw = Math.max(12, Math.min(Math.max(width, maxW), maxW) + 14); int bh = lineH * lines.size() + 8; Color old = g.getColor(); g.setColor(new Color(0, 0, 0, 118)); g.fillRoundRect(bx, by, bw, bh, 8, 8); g.setColor(new Color(128, 105, 58, 88)); g.drawRoundRect(bx, by, bw, bh, 8, 8); g.setColor(color == null ? old : color); int yy = firstBaseline; for (String raw : lines) { String line = raw == null ? "" : raw; int tx = centered ? x - fm.stringWidth(line) / 2 : x; g.drawString(line, tx, yy); yy += lineH; } g.setColor(old); }
        void drawAsciiPortrait(Graphics2D g, Candidate c, Rectangle r) { g.setFont(panel.asciiFont.deriveFont(Font.BOLD, 15f)); g.setColor(main()); int y = r.y + 38; if (c == null || c.portrait == null) return; for (String line : c.portrait) { center(g, line, r.x + r.width / 2, y); y += 20; } }
        void center(Graphics2D g, String text, int x, int y) { FontMetrics fm = g.getFontMetrics(); String s = text == null ? "" : text; drawWrappedTextBlock(g, List.of(s), x, y, fm.stringWidth(s), g.getColor(), true); }
        void drawBackedText(Graphics2D g, String text, int x, int y, boolean centered) { if (g == null || text == null || text.isBlank()) return; FontMetrics fm = g.getFontMetrics(); int tw = fm.stringWidth(text); int th = fm.getHeight(); int bx = x - 5; int by = y - fm.getAscent() - 3; Color old = g.getColor(); g.setColor(new Color(0, 0, 0, 104)); g.fillRoundRect(bx, by, tw + 10, th + 5, 7, 7); g.setColor(new Color(128, 105, 58, 76)); g.drawRoundRect(bx, by, tw + 10, th + 5, 7, 7); g.setColor(old); g.drawString(text, x, y); }
        void repaintPanel() { revalidate(); repaint(); if (panel != null) panel.repaint(); }
        Color highlight() { return panel.optionColor(GameOptions.TEXT_HIGHLIGHT); }
        Color main() { return panel.optionColor(GameOptions.TEXT_MAIN); }
        Color muted() { return panel.optionColor(GameOptions.TEXT_DIM); }
        static String safe(String s) { return s == null || s.isBlank() ? "unspecified" : s.replace('\n', ' ').trim(); }
    }
}
