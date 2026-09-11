package mechanist;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Font;
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

/**
 * Restores direct player-portrait browsing during staged character creation.
 *
 * Candidate.portraitIndex remains the sole persistent portrait-selection state;
 * this authority exposes keyboard and compact mouse controls over that existing
 * identity without creating a second character/profile selection model.
 */
final class CharacterCreationPortraitSelectionAuthority implements KeyEventDispatcher {
    static final String VERSION = "character-creation-portrait-selection-1.2-semantic-index-wrap";
    static final String CLIENT_PROPERTY = "mechanist.characterCreationPortraitSelectionAuthority";
    static final String CONTROLS_PROPERTY = "mechanist.characterCreationPortraitSelectionControls";

    private final GamePanel panel;

    private CharacterCreationPortraitSelectionAuthority(GamePanel panel) {
        this.panel = panel;
    }

    static void installBeforeWorldStartFlow(GamePanel panel) {
        if (panel == null) return;
        if (panel.getClientProperty(CLIENT_PROPERTY) instanceof CharacterCreationPortraitSelectionAuthority) return;
        CharacterCreationPortraitSelectionAuthority authority = new CharacterCreationPortraitSelectionAuthority(panel);
        panel.putClientProperty(CLIENT_PROPERTY, authority);
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(authority);
        SwingUtilities.invokeLater(() -> attachVisibleControls(panel));
        DebugLog.audit("CHARACTER_PORTRAIT_SELECTION", "installed " + VERSION + " keys=[/] visibleControls=true");
    }

    private static void attachVisibleControls(GamePanel panel) {
        if (panel == null) return;
        Object value = panel.getClientProperty(WorldStartFlowAuthority.WorldStartFlowOverlay.CLIENT_PROPERTY);
        if (!(value instanceof WorldStartFlowAuthority.WorldStartFlowOverlay overlay)) return;
        if (overlay.getClientProperty(CONTROLS_PROPERTY) instanceof PortraitControlsLayer) return;

        PortraitControlsLayer controls = new PortraitControlsLayer(panel, overlay);
        overlay.putClientProperty(CONTROLS_PROPERTY, controls);
        overlay.add(controls);
        controls.setBounds(0, 0, Math.max(1, overlay.getWidth()), Math.max(1, overlay.getHeight()));
        overlay.setComponentZOrder(controls, 0);
        overlay.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) {
                controls.setBounds(0, 0, Math.max(1, overlay.getWidth()), Math.max(1, overlay.getHeight()));
            }
        });
        overlay.revalidate();
        overlay.repaint();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event == null || event.getID() != KeyEvent.KEY_PRESSED) return false;
        if (panel == null || panel.screen != GamePanel.Screen.CHARACTER || !panel.newGameSetupActive) return false;
        if (!WorldStartFlowAuthority.isActive(panel)) return false;

        int direction = directionForKey(event.getKeyCode());
        if (direction == 0) return false;
        if (!cycleSelectedPortrait(panel, direction)) return false;

        event.consume();
        return true;
    }

    static int directionForKey(int keyCode) {
        if (keyCode == KeyEvent.VK_OPEN_BRACKET) return -1;
        if (keyCode == KeyEvent.VK_CLOSE_BRACKET) return 1;
        return 0;
    }

    static boolean cycleSelectedPortrait(GamePanel panel, int direction) {
        if (panel == null || direction == 0) return false;
        Candidate candidate = panel.selectedNewGameCandidate();
        if (candidate == null) return false;

        candidate.portraitIndex = shiftedPortraitIndex(candidate.portraitIndex, direction);
        panel.active = candidate;
        updateStartFlowStatus(panel);
        panel.repaint();
        return true;
    }

    static int shiftedPortraitIndex(int current, int direction) {
        if (direction > 0) return current == Integer.MAX_VALUE ? Integer.MIN_VALUE : current + 1;
        if (direction < 0) return current == Integer.MIN_VALUE ? Integer.MAX_VALUE : current - 1;
        return current;
    }

    static Rectangle portraitRectForSheet(Rectangle sheet) {
        if (sheet == null) return new Rectangle();
        int portraitSize = Math.max(88, Math.min(128, Math.min(sheet.width - 24, sheet.height / 3)));
        return new Rectangle(sheet.x + 12, sheet.y + 34, portraitSize, portraitSize);
    }

    static Rectangle previousControlRect(Rectangle portrait) {
        if (portrait == null) return new Rectangle();
        int size = Math.max(24, Math.min(34, portrait.width / 4));
        return new Rectangle(portrait.x + 5, portrait.y + (portrait.height - size) / 2, size, size);
    }

    static Rectangle nextControlRect(Rectangle portrait) {
        if (portrait == null) return new Rectangle();
        int size = Math.max(24, Math.min(34, portrait.width / 4));
        return new Rectangle(portrait.x + portrait.width - size - 5, portrait.y + (portrait.height - size) / 2, size, size);
    }

    static int portraitControlDirection(Rectangle portrait, Point point) {
        if (portrait == null || point == null) return 0;
        if (previousControlRect(portrait).contains(point)) return -1;
        if (nextControlRect(portrait).contains(point)) return 1;
        return 0;
    }

    private static void updateStartFlowStatus(GamePanel panel) {
        Object value = panel.getClientProperty(WorldStartFlowAuthority.WorldStartFlowOverlay.CLIENT_PROPERTY);
        if (value instanceof WorldStartFlowAuthority.WorldStartFlowOverlay overlay) {
            overlay.status = "Portrait selection changed. Use [ and ] or the portrait arrows to browse.";
        }
    }

    static final class PortraitControlsLayer extends JComponent {
        private final GamePanel panel;
        private final WorldStartFlowAuthority.WorldStartFlowOverlay overlay;

        PortraitControlsLayer(GamePanel panel, WorldStartFlowAuthority.WorldStartFlowOverlay overlay) {
            this.panel = panel;
            this.overlay = overlay;
            setOpaque(false);
            setFocusable(false);
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent event) { handleMouse(event); }
                @Override public void mouseReleased(MouseEvent event) { if (active()) event.consume(); }
                @Override public void mouseClicked(MouseEvent event) { if (active()) event.consume(); }
            });
        }

        private boolean active() {
            return panel != null
                    && overlay != null
                    && overlay.stage == WorldStartFlowAuthority.Stage.CHARACTER_CREATION
                    && panel.screen == GamePanel.Screen.CHARACTER
                    && panel.newGameSetupActive;
        }

        private Rectangle portraitRect() {
            return portraitRectForSheet(overlay.characterSheetRect());
        }

        @Override public boolean contains(int x, int y) {
            return active() && portraitControlDirection(portraitRect(), new Point(x, y)) != 0;
        }

        private void handleMouse(MouseEvent event) {
            if (event == null || !active()) return;
            int direction = portraitControlDirection(portraitRect(), event.getPoint());
            if (direction == 0) return;
            if (cycleSelectedPortrait(panel, direction)) {
                event.consume();
                repaint();
            }
        }

        @Override protected void paintComponent(Graphics graphics) {
            if (!active()) return;
            Candidate candidate = panel.selectedNewGameCandidate();
            if (candidate == null) return;

            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Rectangle portrait = portraitRect();
                drawControl(g, previousControlRect(portrait), "<");
                drawControl(g, nextControlRect(portrait), ">");
                String label = "Portrait";
                g.setFont(panel.smallFont.deriveFont(Font.BOLD, Math.max(10f, panel.smallFont.getSize2D())));
                int textW = g.getFontMetrics().stringWidth(label);
                int labelW = Math.min(portrait.width - 16, textW + 14);
                int labelX = portrait.x + (portrait.width - labelW) / 2;
                int labelY = portrait.y + portrait.height - 25;
                g.setColor(new Color(8, 10, 9, 205));
                g.fillRoundRect(labelX, labelY, labelW, 20, 8, 8);
                g.setColor(new Color(232, 202, 126));
                g.drawString(label, labelX + (labelW - textW) / 2, labelY + 15);
            } finally {
                g.dispose();
            }
        }

        private void drawControl(Graphics2D g, Rectangle rect, String label) {
            g.setColor(new Color(8, 10, 9, 220));
            g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);
            g.setColor(new Color(180, 145, 70, 230));
            g.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 8, 8);
            g.setFont(panel.smallFont.deriveFont(Font.BOLD, 18f));
            int textW = g.getFontMetrics().stringWidth(label);
            int baseline = rect.y + (rect.height + g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2;
            g.setColor(new Color(245, 220, 158));
            g.drawString(label, rect.x + (rect.width - textW) / 2, baseline);
        }
    }

    private CharacterCreationPortraitSelectionAuthority() {
        this.panel = null;
    }
}
