$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$panelPath = Join-Path $root 'src\mechanist\LegacyPanelContext.java'
$menuPath = Join-Path $root 'src\mechanist\MainMenuSurfacePainter.java'
foreach ($p in @($panelPath, $menuPath)) { if (-not (Test-Path -LiteralPath $p -PathType Leaf)) { throw "Missing $p" } }

$menu = @'
package mechanist;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

final class MainMenuSurfacePainter implements ScreenPainter {
    @Override
    public void paint(Graphics2D g, GamePanel panel) {
        int W = Math.max(1, panel.getWidth());
        int H = Math.max(1, panel.getHeight());
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(16, 15, 12), 0, H, new Color(34, 28, 18)));
        g.fillRect(0, 0, W, H);
        drawBackdrop(g, W, H);

        int cx = W / 2;
        int titleY = panel.mainMenuTitleTop(H) + 72;
        g.setFont(panel.titleFont.deriveFont(Font.BOLD, Math.max(42f, Math.min(72f, H / 8f))));
        FontMetrics titleFm = g.getFontMetrics();
        String title = "THE MECHANIST";
        g.setColor(new Color(45, 32, 14, 190));
        g.drawString(title, cx - titleFm.stringWidth(title) / 2 + 3, titleY + 3);
        g.setColor(new Color(231, 204, 132));
        g.drawString(title, cx - titleFm.stringWidth(title) / 2, titleY);

        g.setFont(panel.smallFont.deriveFont(Font.BOLD, 14f));
        FontMetrics small = g.getFontMetrics();
        String subtitle = "UNFOLDED CLIENT PACKAGE  ·  INDEXED 32PX ASSET RUNTIME";
        g.setColor(new Color(172, 150, 102));
        g.drawString(subtitle, cx - small.stringWidth(subtitle) / 2, titleY + 30);

        Rectangle frame = panel.mainMenuButtonFrameRect();
        frame = new Rectangle(frame.x, Math.max(titleY + 62, frame.y - 40), frame.width, Math.max(300, frame.height + 96));
        g.setColor(new Color(0, 0, 0, 178));
        g.fillRoundRect(frame.x, frame.y, frame.width, frame.height, 18, 18);
        g.setColor(new Color(139, 106, 50));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(frame.x, frame.y, frame.width, frame.height, 18, 18);
        panel.stampUiFrameId(g, "F", "main-menu-route-frame", frame.x, frame.y, frame.width, frame.height);

        java.util.List<String> labels = panel.mainMenuRouteLabels();
        for (int i = 0; i < labels.size(); i++) drawRouteButton(g, panel, labels.get(i), panel.mainMenuRouteRect(i), i == panel.selectedButton);

        java.util.List<String> shellLines = panel.launcherShell.displayLines(panel.launcherRuntime, panel.userProfile);
        int panelW = Math.min(W - 220, 460);
        int panelH = Math.max(28, Math.min(42, H / 18));
        int panelX = (W - panelW) / 2;
        int panelY = H - panelH - 34;
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(panelX, panelY, panelW, panelH, 9, 9);
        g.setColor(new Color(130, 105, 55, 150));
        g.drawRoundRect(panelX, panelY, panelW, panelH, 9, 9);
        g.setFont(panel.smallFont.deriveFont(Font.BOLD, 12f));
        FontMetrics fm = g.getFontMetrics();
        String line = shellLines.isEmpty() ? "LOCAL RUNTIME READY" : shellLines.get(0);
        panel.center(g, GuiLayoutApi.fitLabel(line, fm, panelW - 24), cx, panelY + panelH / 2 + fm.getAscent() / 2 - 2);
    }

    private void drawBackdrop(Graphics2D g, int W, int H) {
        g.setColor(new Color(75, 61, 35, 95));
        for (int x = -H; x < W + H; x += 64) g.drawLine(x, H, x + H, 0);
        g.setColor(new Color(180, 142, 64, 38));
        for (int y = 0; y < H; y += 32) g.drawLine(0, y, W, y);
    }

    private void drawRouteButton(Graphics2D g, GamePanel panel, String label, Rectangle r, boolean selected) {
        Color fill = selected ? new Color(88, 64, 25, 225) : new Color(24, 22, 18, 224);
        Color edge = selected ? new Color(231, 204, 132) : new Color(119, 94, 48);
        g.setColor(fill);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g.setColor(edge);
        g.setStroke(new BasicStroke(selected ? 2.4f : 1.2f));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g.setFont(panel.uiFont.deriveFont(Font.BOLD, selected ? 17f : 16f));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(selected ? new Color(245, 226, 154) : new Color(209, 202, 176));
        panel.center(g, label, r.x + r.width / 2, r.y + r.height / 2 + fm.getAscent() / 2 - 3);
        panel.stampUiFrameId(g, "B", "main-menu-" + label.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-"), r.x, r.y, r.width, r.height);
    }
}
'@
Set-Content -LiteralPath $menuPath -Value $menu

$panel = Get-Content -LiteralPath $panelPath -Raw
if ($panel -notmatch 'installMainMenuInputBridge\(\)') {
    $panel = $panel.Replace('        logEvent("GamePanel compatibility bridge initialized.");', '        logEvent("GamePanel compatibility bridge initialized.");' + "`r`n" + '        installMainMenuInputBridge();')
    $insert = @'

    void installMainMenuInputBridge() {
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override public void mouseMoved(java.awt.event.MouseEvent e) {
                mouseX = e.getX(); mouseY = e.getY();
                if (screen == Screen.MENU || screen == Screen.MAIN || screen == Screen.BOOT) {
                    int idx = mainMenuRouteIndexAt(mouseX, mouseY);
                    if (idx >= 0 && idx != selectedButton) { selectedButton = idx; repaint(); }
                }
            }
        });
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!(screen == Screen.MENU || screen == Screen.MAIN || screen == Screen.BOOT)) return;
                int idx = mainMenuRouteIndexAt(e.getX(), e.getY());
                if (idx >= 0) { selectedButton = idx; activateMainMenuRouteIndex(idx); repaint(); requestFocusInWindow(); }
            }
        });
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent e) {
                if (!(screen == Screen.MENU || screen == Screen.MAIN || screen == Screen.BOOT)) return;
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_UP || e.getKeyCode() == java.awt.event.KeyEvent.VK_W) { selectedButton = Math.floorMod(selectedButton - 1, mainMenuRouteLabels().size()); repaint(); return; }
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN || e.getKeyCode() == java.awt.event.KeyEvent.VK_S) { selectedButton = Math.floorMod(selectedButton + 1, mainMenuRouteLabels().size()); repaint(); return; }
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER || e.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) { activateMainMenuRouteIndex(selectedButton); repaint(); return; }
                int digit = e.getKeyCode() - java.awt.event.KeyEvent.VK_1;
                if (digit >= 0 && digit < mainMenuRouteLabels().size()) { selectedButton = digit; activateMainMenuRouteIndex(digit); repaint(); }
            }
        });
    }

    java.util.List<String> mainMenuRouteLabels() {
        return java.util.List.of("New Game", "Continue", "Load Game", "Options", "Knowledge", "Multiplayer", "Quit");
    }

    Rectangle mainMenuRouteRect(int index) {
        Rectangle frame = mainMenuButtonFrameRect();
        int buttonW = Math.max(220, frame.width - 88);
        int buttonH = 36;
        int gap = 10;
        int top = Math.max(mainMenuTitleTop(Math.max(1, getHeight())) + 142, frame.y + 30);
        int x = getWidth() / 2 - buttonW / 2;
        return new Rectangle(x, top + index * (buttonH + gap), buttonW, buttonH);
    }

    int mainMenuRouteIndexAt(int x, int y) {
        java.util.List<String> labels = mainMenuRouteLabels();
        for (int i = 0; i < labels.size(); i++) if (mainMenuRouteRect(i).contains(x, y)) return i;
        return -1;
    }

    void activateMainMenuRouteIndex(int index) {
        java.util.List<String> labels = mainMenuRouteLabels();
        if (labels.isEmpty()) return;
        index = Math.floorMod(index, labels.size());
        String route = labels.get(index);
        logEvent("Main menu route selected: " + route);
        switch (route) {
            case "New Game" -> startPackagedClientNewGame();
            case "Continue", "Load Game" -> { logEvent(route + " is visible; save/load bridge restoration is pending."); }
            case "Options" -> setScreen(Screen.OPTIONS);
            case "Knowledge" -> openKnowledgeMenu();
            case "Multiplayer" -> setScreen(Screen.MULTIPLAYER);
            case "Quit" -> requestApplicationExit("main menu quit route");
            default -> logEvent("Unhandled main menu route: " + route);
        }
    }

    void startPackagedClientNewGame() {
        screen = Screen.GAME;
        panelMode = PanelMode.NONE;
        playerX = Math.max(1, playerX == 0 ? 8 : playerX);
        playerY = Math.max(1, playerY == 0 ? 8 : playerY);
        lookX = playerX;
        lookY = playerY;
        food = food == 0 ? MAX_FOOD_WATER : food;
        water = water == 0 ? MAX_FOOD_WATER : water;
        if (inventory.isEmpty()) inventory.add("Ration pack");
        logEvent("New game bridge started from package client menu.");
    }
'@
    $anchor = '    @Override' + "`r`n" + '    protected void paintComponent(java.awt.Graphics graphics) {'
    $idx = $panel.IndexOf($anchor, [System.StringComparison]::Ordinal)
    if ($idx -lt 0) { throw 'Could not find paintComponent anchor.' }
    $panel = $panel.Substring(0, $idx) + $insert + "`r`n" + $panel.Substring($idx)
}

# Prevent menu title keys from grabbing an arbitrary indexed gameplay tile such as a sandbag.
$panel = $panel.Replace('        java.nio.file.Path indexed = findIndexedPackageAsset(key);' + "`r`n" + '        BufferedImage indexedImage = readImage(indexed);' + "`r`n" + '        if (indexedImage != null) return indexedImage;', '        if (!("title_mechanist_rebase".equalsIgnoreCase(key) || "title_mechanist".equalsIgnoreCase(key) || "subtitle_rebase".equalsIgnoreCase(key))) {' + "`r`n" + '            java.nio.file.Path indexed = findIndexedPackageAsset(key);' + "`r`n" + '            BufferedImage indexedImage = readImage(indexed);' + "`r`n" + '            if (indexedImage != null) return indexedImage;' + "`r`n" + '        }')
Set-Content -LiteralPath $panelPath -Value $panel
Write-Host 'Applied main menu route surface and input rewire.'
