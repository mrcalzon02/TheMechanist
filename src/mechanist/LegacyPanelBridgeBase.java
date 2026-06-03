package mechanist;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.swing.JPanel;

/**
 * Inherited compatibility surface for the temporary GamePanel bridge.
 *
 * Keep this class boring and transitional: it exists to let extracted systems
 * compile while each dependency is retargeted to a real subsystem/context.
 */
@SuppressWarnings({"serial", "unused"})
class LegacyPanelBridgeBase extends JPanel {
    static final int SAVE_SLOT_COUNT = 3;

    MachineOperationQueue machineOperationQueue = new MachineOperationQueue();
    UniversalWindowAuthority universalWindowAuthority = new UniversalWindowAuthority();
    World auditWorld;
    int auditCursorX;
    int auditCursorY;
    boolean auditZoneDropdownOpen;
    Rectangle characterNameEditRect = new Rectangle(0, 0, 1, 1);
    LegacyLauncherRuntime launcherRuntime = new LegacyLauncherRuntime();
    LegacyLauncherShell launcherShell = new LegacyLauncherShell();

    int countProductionInput(String item) { return legacyCount(item); }
    int countCraftInput(String item) { return legacyCount(item); }
    private int legacyCount(String item) {
        if (!(this instanceof GamePanel g) || item == null) return 0;
        int n = 0;
        for (String s : g.inventory) if (ItemQuality.namesMatch(s, item)) n++;
        for (String s : g.baseStorage) if (ItemQuality.namesMatch(s, item)) n++;
        return n;
    }

    BaseObject selectedWorkerMachine() {
        ArrayList<BaseObject> machines = recruitOperableMachines();
        return machines.isEmpty() ? null : machines.get(0);
    }

    ArrayList<BaseObject> recruitOperableMachines() {
        ArrayList<BaseObject> out = new ArrayList<>();
        if (this instanceof GamePanel g) out.addAll(g.baseObjects);
        return out;
    }

    BaseObject selectedStaffingStation() { return selectedWorkerMachine(); }
    RecruitWorker selectedStaffingWorker() {
        if (!(this instanceof GamePanel g) || g.factionRecruits.isEmpty()) return null;
        return g.factionRecruits.get(0);
    }
    ArrayList<BaseObject> staffableStations() { return recruitOperableMachines(); }

    int mainMenuTitleTop(int height) { return Math.max(24, height / 10); }
    Dimension mainMenuTitleDrawSize(BufferedImage img, int width, int height) {
        if (img == null) return new Dimension(Math.max(280, width / 2), 96);
        return aspectFit(img, Math.max(160, width - 110), Math.max(48, height / 4));
    }
    Dimension mainMenuSubtitleDrawSize(BufferedImage img, int width, int height) {
        if (img == null) return new Dimension(Math.max(220, width / 3), 48);
        return aspectFit(img, Math.max(120, width - 160), Math.max(24, height / 8));
    }
    private Dimension aspectFit(BufferedImage img, int maxW, int maxH) {
        if (img == null || img.getWidth() <= 0 || img.getHeight() <= 0) return new Dimension(Math.max(1, maxW), Math.max(1, maxH));
        double scale = Math.min(maxW / (double) img.getWidth(), maxH / (double) img.getHeight());
        if (!Double.isFinite(scale) || scale <= 0) scale = 1.0;
        int w = Math.max(1, (int)Math.round(img.getWidth() * scale));
        int h = Math.max(1, (int)Math.round(img.getHeight() * scale));
        return new Dimension(w, h);
    }
    Rectangle mainMenuButtonFrameRect() {
        int count = 7;
        if (this instanceof GamePanel g) count = Math.max(1, g.mainMenuRouteLabels().size());
        int frameW = Math.max(320, Math.min(480, getWidth() - 64));
        int y = Math.max(176, getHeight() / 2);
        int maxH = Math.max(180, getHeight() - y - 46);
        int neededH = 36 + count * 28 + Math.max(0, count - 1) * 6;
        int frameH = Math.max(180, Math.min(neededH, maxH));
        return new Rectangle(Math.max(20, getWidth() / 2 - frameW / 2), y, frameW, frameH);
    }
    void stampUiFrameId(Graphics2D g, String kind, String id, int x, int y, int w, int h) {}
    void drawSlicedFrame(Graphics2D g, int x, int y, int w, int h, String style) { if (g != null) g.drawRect(x, y, Math.max(1, w), Math.max(1, h)); }
    void center(Graphics2D g, String text, int x, int y) {
        if (g == null || text == null) return;
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, x - fm.stringWidth(text) / 2, y);
    }
    Color optionColor(int key) {
        return switch (key) {
            case GameOptions.TEXT_HIGHLIGHT -> new Color(225, 208, 140);
            case GameOptions.TEXT_DIM -> new Color(130, 130, 120);
            default -> new Color(205, 210, 195);
        };
    }

    Rectangle uiLayout() { return new Rectangle(0, 0, Math.max(1, getWidth()), Math.max(1, getHeight())); }
    Rectangle zoneAuditDropdownOuterRect(Rectangle layout) { return layout == null ? new Rectangle(0,0,1,1) : new Rectangle(layout.x + 20, layout.y + 20, 260, 320); }
    boolean isZoneAuditDropdownButton(ButtonBox button) { return button != null && button.label != null && button.label.startsWith("ZONE:"); }
    Point screenPointToAuditTile(int mx, int my) { return new Point(Math.max(0, mx / 16), Math.max(0, my / 16)); }
    Point screenPointToWorldTile(int mx, int my) { return new Point(Math.max(0, mx / 16), Math.max(0, my / 16)); }
    boolean handleInGameEditorGridPaint(int mx, int my, boolean erase) { return false; }

    void clearMouseMovementPreview(String reason) {
        if (this instanceof GamePanel g) {
            g.mouseMovePreviewActive = false;
            g.mouseMovePreviewValid = false;
            g.mouseMovePreviewPath.clear();
        }
    }
    void updateMouseMovementPreviewTo(int x, int y) {
        if (this instanceof GamePanel g) {
            g.mouseMovePreviewActive = true;
            g.mouseMovePreviewValid = true;
            g.mouseMovePreviewPath.clear();
            g.mouseMovePreviewPath.add(new Point(x, y));
        }
    }
    void executeMouseMovementPreview() { clearMouseMovementPreview("executed mouse movement preview"); }
    boolean handleInventoryStackPanelClick(int mx, int my) { return false; }
    Object findScrollRegion(int mx, int my) { return null; }
    void handleScrollbarClick(int mx, int my) {}
    boolean handleKnowledgeTreeClick(int mx, int my) { return false; }

    Rectangle multiplayerMenuPanelRect() { return new Rectangle(Math.max(20, getWidth()/2 - 320), 60, 640, Math.max(420, getHeight() - 120)); }
    Rectangle multiplayerContentRect(Rectangle main) { return new Rectangle(main.x + 24, main.y + 72, Math.max(1, main.width - 48), Math.max(1, main.height - 160)); }
    Rectangle multiplayerActionRect(Rectangle main) { return new Rectangle(main.x + 24, main.y + main.height - 72, Math.max(1, main.width - 48), 48); }
    void drawPanelBox(Graphics2D g, int x, int y, int w, int h, String title) {
        if (g == null) return;
        g.drawRect(x, y, Math.max(1, w), Math.max(1, h));
        if (title != null && !title.isBlank()) g.drawString(title, x + 12, y + 22);
    }
    void drawTextPanel(Graphics2D g, int x, int y, int w, int h, java.util.List<String> lines, boolean highlighted) {
        drawPanelBox(g, x, y, w, h, null);
        if (g == null || lines == null) return;
        int yy = y + 22;
        for (String line : lines) {
            if (yy > y + h - 8) break;
            drawUiTextLine(g, line, x + 12, yy);
            yy += Math.max(12, g.getFontMetrics().getHeight());
        }
    }
    void drawUiTextLine(Graphics2D g, String line, int x, int y) {
        if (g != null && line != null) g.drawString(line, x, y);
    }
    Rectangle graphicsDropdownOuterRect() { return new Rectangle(Math.max(20, getWidth()/2 - 220), 96, 440, 260); }
}

final class LegacyLauncherRuntime {}

final class LegacyLauncherShell {
    java.util.List<String> displayLines(LegacyLauncherRuntime runtime, UserProfileAuthority.Profile profile) {
        ArrayList<String> out = new ArrayList<>();
        out.add("LOCAL RUNTIME READY");
        out.add(profile == null ? "PROFILE: UNKNOWN" : "PROFILE: " + profile.compactLabel());
        return out;
    }
}


