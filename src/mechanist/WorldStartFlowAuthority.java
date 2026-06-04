package mechanist;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
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
            worldIndex = 0;
            worldGenerationOption = 0;
            characterOption = 0;
            nameEditing = false;
            status = worlds.isEmpty()
                    ? "No generated arcology worlds found. Generate a new world to continue."
                    : "Select an existing generated world, or choose Generate New World.";
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
            // Create/save the generated world definition now, then character creation uses that world.
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
            if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) { worldIndex = Math.floorMod(worldIndex - 1, Math.max(1, rows)); return true; }
            if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) { worldIndex = Math.floorMod(worldIndex + 1, Math.max(1, rows)); return true; }
            if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE || code == KeyEvent.VK_E) {
                if (worldIndex >= worlds.size()) openWorldGeneration(); else acceptExistingWorld();
                return true;
            }
            return true;
        }

        boolean handleWorldGenerationKey(int code) {
            if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_Q) { openWorldPicker("back from world generation"); return true; }
            if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) { worldGenerationOption = Math.floorMod(worldGenerationOption - 1, 7); return true; }
            if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) { worldGenerationOption = Math.floorMod(worldGenerationOption + 1, 7); return true; }
            if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A || code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D || code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) { cycleWorldSetupSelected(); return true; }
            if (code == KeyEvent.VK_G) { acceptGeneratedWorld(); return true; }
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

        void handleMouse(MouseEvent e) {
            if (stage == Stage.CLOSED || e == null) return;
            Point p = e.getPoint();
            if (stage == Stage.WORLD_PICKER) {
                List<Rectangle> rows = worldPickerRows();
                for (int i = 0; i < rows.size(); i++) if (rows.get(i).contains(p)) { worldIndex = i; if (i >= worlds.size()) openWorldGeneration(); else acceptExistingWorld(); e.consume(); return; }
                if (buttonRect(0).contains(p)) { openWorldGeneration(); e.consume(); return; }
                if (buttonRect(1).contains(p)) { closeToMainMenu(); e.consume(); return; }
            } else if (stage == Stage.WORLD_GENERATION) {
                List<Rectangle> rows = optionRows(7);
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
            drawText(g, status, r.x + 24, r.y + 92, r.width - 48, main());
            List<Rectangle> rows = worldPickerRows();
            for (int i = 0; i < rows.size(); i++) {
                Rectangle row = rows.get(i);
                boolean selected = i == worldIndex;
                fillRow(g, row, selected);
                String label = i < worlds.size() ? worlds.get(i).summaryLine() : "+ Generate New World";
                drawText(g, (selected ? "> " : "  ") + label, row.x + 12, row.y + 28, row.width - 24, selected ? highlight() : main());
            }
            drawButton(g, buttonRect(0), "Generate New", true);
            drawButton(g, buttonRect(1), "Back", false);
            drawFooter(g, "Enter selects. N/G generates a new world. Esc returns to main menu.");
        }

        void paintWorldGeneration(Graphics2D g, Rectangle r) {
            if (panel.worldSetup == null) panel.worldSetup = WorldSetupSettings.standard();
            drawText(g, "World Generation Options", r.x + 24, r.y + 64, r.width - 48, highlight());
            drawText(g, panel.worldSetup.shortSummary(), r.x + 24, r.y + 92, r.width - 48, main());
            ArrayList<String> lines = panel.worldSetup.detailLines();
            List<Rectangle> rows = optionRows(lines.size());
            for (int i = 0; i < lines.size() && i < rows.size(); i++) {
                Rectangle row = rows.get(i);
                boolean selected = i == worldGenerationOption;
                fillRow(g, row, selected);
                drawText(g, (selected ? "> " : "  ") + lines.get(i), row.x + 12, row.y + 27, row.width - 24, selected ? highlight() : main());
            }
            drawButton(g, buttonRect(0), "Generate World", true);
            drawButton(g, buttonRect(1), "Back", false);
            drawFooter(g, "Up/Down chooses an option. Left/Right/Enter cycles. G generates and opens character creation.");
        }

        void paintCharacterCreation(Graphics2D g, Rectangle r) {
            Candidate c = panel.selectedNewGameCandidate();
            drawText(g, "Character Generation", r.x + 24, r.y + 64, r.width - 48, highlight());
            drawText(g, status, r.x + 24, r.y + 92, r.width - 48, main());
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

        List<Rectangle> worldPickerRows() {
            Rectangle p = panelRect(getWidth(), getHeight());
            ArrayList<Rectangle> rows = new ArrayList<>();
            int count = Math.max(1, worlds.size() + 1);
            int top = p.y + 122;
            int rowH = 42;
            int max = Math.min(count, Math.max(1, (p.height - 220) / (rowH + 8)));
            for (int i = 0; i < max; i++) rows.add(new Rectangle(p.x + 24, top + i * (rowH + 8), p.width - 48, rowH));
            return rows;
        }

        List<Rectangle> optionRows(int count) {
            Rectangle p = panelRect(getWidth(), getHeight());
            ArrayList<Rectangle> rows = new ArrayList<>();
            int top = p.y + 124;
            for (int i = 0; i < count; i++) rows.add(new Rectangle(p.x + 24, top + i * 42, p.width - 48, 34));
            return rows;
        }

        Rectangle panelRect(int w, int h) {
            int pw = Math.max(720, Math.min(1060, w - 72));
            int ph = Math.max(540, Math.min(720, h - 72));
            return new Rectangle(Math.max(20, w / 2 - pw / 2), Math.max(28, h / 2 - ph / 2), pw, ph);
        }

        Rectangle buttonRect(int index) {
            Rectangle p = panelRect(getWidth(), getHeight());
            int bw = index == 0 ? 178 : 120;
            int gap = 16;
            int x = index == 0 ? p.x + p.width - bw - 28 : p.x + p.width - bw - 28 - 178 - gap;
            return new Rectangle(x, p.y + p.height - 58, bw, 34);
        }

        String title() {
            return switch (stage) {
                case WORLD_PICKER -> "WORLD SELECTION";
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
            g.setColor(primary ? new Color(76, 62, 32, 232) : new Color(16, 18, 16, 224));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g.setColor(primary ? highlight() : new Color(145, 118, 64, 140));
            g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            g.setColor(primary ? highlight() : main());
            BufferedImage icon = panel.systemButtonIconForLabel(label);
            FontMetrics fm = g.getFontMetrics();
            int iconSize = icon == null ? 0 : Math.max(18, Math.min(r.height - 8, 28));
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
            g.setColor(color);
            FontMetrics fm = g.getFontMetrics();
            drawBackedText(g, GuiLayoutApi.fitLabel(text == null ? "" : text, fm, width), x, y, false);
        }

        void drawLines(Graphics2D g, List<String> lines, int x, int y, int width, Color color) {
            g.setFont(panel.smallFont);
            g.setColor(color);
            FontMetrics fm = g.getFontMetrics();
            int yy = y;
            if (lines == null) return;
            for (String line : lines) {
                drawBackedText(g, GuiLayoutApi.fitLabel(line == null ? "" : line, fm, width), x, yy, false);
                yy += Math.max(18, fm.getHeight() + 3);
            }
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
            drawBackedText(g, s, x - fm.stringWidth(s) / 2, y, true);
        }

        void drawBackedText(Graphics2D g, String text, int x, int y, boolean centered) {
            if (g == null || text == null || text.isBlank()) return;
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(text);
            int th = fm.getHeight();
            int bx = centered ? x - 5 : x - 5;
            int by = y - fm.getAscent() - 3;
            Color old = g.getColor();
            g.setColor(new Color(0, 0, 0, 176));
            g.fillRoundRect(bx, by, tw + 10, th + 5, 7, 7);
            g.setColor(new Color(128, 105, 58, 120));
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
