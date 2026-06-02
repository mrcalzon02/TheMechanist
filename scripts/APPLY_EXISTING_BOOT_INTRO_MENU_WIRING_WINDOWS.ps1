$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$panelPath = Join-Path $root 'src\mechanist\LegacyPanelContext.java'
$menuPath = Join-Path $root 'src\mechanist\MainMenuSurfacePainter.java'
foreach ($p in @($panelPath, $menuPath)) { if (-not (Test-Path -LiteralPath $p -PathType Leaf)) { throw "Missing $p" } }

# Restore the established MainMenuSurfacePainter. Do not beautify or replace it with a temporary route menu.
$mainMenu = @'
package mechanist;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

final class MainMenuSurfacePainter implements ScreenPainter {
    @Override
    public void paint(Graphics2D g, GamePanel panel) {
        int W = panel.getWidth();
        int H = panel.getHeight();
        
        BufferedImage title = panel.images.get("title_mechanist_rebase");
        if (title == null) title = panel.images.get("title_mechanist");
        int cx = W / 2;
        int titleTop = panel.mainMenuTitleTop(H);
        int drawnTitleBottom = titleTop + 112;
        
        if (title != null) {
            Dimension titleSize = panel.mainMenuTitleDrawSize(title, W, H);
            int tw = titleSize.width;
            int th = titleSize.height;
            g.drawImage(title, cx - tw / 2, titleTop, tw, th, null);
            panel.stampUiFrameId(g, "I", "title-mechanist", cx - tw / 2, titleTop, tw, th);
            drawnTitleBottom = titleTop + th;
        } else {
            g.setFont(panel.titleFont.deriveFont(Font.BOLD, Math.max(34f, Math.min(56f, H / 9f))));
            g.setColor(new Color(200, 184, 132));
            panel.center(g, "THE MECHANIST", cx, titleTop + 68);
        }
        
        BufferedImage subtitle = panel.images.get("subtitle_rebase");
        if (subtitle != null) {
            Dimension subSize = panel.mainMenuSubtitleDrawSize(subtitle, W, H);
            int sw = subSize.width;
            int sh = subSize.height;
            int sy = drawnTitleBottom + 3;
            g.drawImage(subtitle, cx - sw / 2, sy, sw, sh, null);
            panel.stampUiFrameId(g, "I", "subtitle", cx - sw / 2, sy, sw, sh);
        }
        
        Rectangle buttonFrame = panel.mainMenuButtonFrameRect();
        g.setColor(new Color(0, 0, 0, 172));
        g.fillRoundRect(buttonFrame.x, buttonFrame.y, buttonFrame.width, buttonFrame.height, 14, 14);
        panel.drawSlicedFrame(g, buttonFrame.x, buttonFrame.y, buttonFrame.width, buttonFrame.height, "inner");
        panel.stampUiFrameId(g, "F", "main-menu-route-frame", buttonFrame.x, buttonFrame.y, buttonFrame.width, buttonFrame.height);
        
        java.util.List<String> shellLines = panel.launcherShell.displayLines(panel.launcherRuntime, panel.userProfile);
        int panelW = Math.min(W - 220, 245);
        int panelH = Math.max(22, Math.min(26, H / 24));
        int panelX = (W - panelW) / 2;
        int panelY = H - panelH - 38;
        
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(panelX, panelY, panelW, panelH, 9, 9);
        g.setColor(new Color(130, 105, 55, 150));
        g.drawRoundRect(panelX, panelY, panelW, panelH, 9, 9);
        panel.stampUiFrameId(g, "F", "launcher-runtime-compact", panelX, panelY, panelW, panelH);
        
        g.setFont(panel.smallFont.deriveFont(Math.max(7f, Math.min(8.5f, panel.smallFont.getSize2D() - 4f))));
        FontMetrics fm = g.getFontMetrics();
        int lineY = panelY + 14;
        int maxLines = Math.max(1, (panelH - 8) / Math.max(10, fm.getHeight()));
        
        for (int i = 0; i < shellLines.size() && i < maxLines; i++) {
            g.setColor(i == 0 ? panel.optionColor(GameOptions.TEXT_HIGHLIGHT) : panel.optionColor(GameOptions.TEXT_DIM));
            panel.center(g, GuiLayoutApi.fitLabel(shellLines.get(i), fm, panelW - 20), cx, lineY);
            lineY += Math.max(10, fm.getHeight());
        }
        g.setFont(panel.smallFont);
    }
}
'@
Set-Content -LiteralPath $menuPath -Value $mainMenu

$panel = Get-Content -LiteralPath $panelPath -Raw

# Start at the established boot screen, not directly at menu.
$panel = $panel.Replace('    Screen screen = Screen.MENU;', '    Screen screen = Screen.BOOT;')

# Remove temporary menu-route input installation if the earlier placeholder route patch was applied.
$panel = $panel.Replace('        installMainMenuInputBridge();' + "`r`n", '')
$panel = $panel.Replace('        installMainMenuInputBridge();' + "`n", '')
$panel = $panel.Replace('        installMainMenuInputBridge();', '')

# Route screens to the established extracted surface painters.
$old1 = @'
                if (screen == Screen.MENU || screen == Screen.MAIN || screen == Screen.BOOT) {
                    new MainMenuSurfacePainter().paint(g, this);
                    return;
                }
                if (screen == Screen.OPTIONS) {
'@
$new1 = @'
                if (screen == Screen.BOOT) {
                    new BootSurfacePainter().paint(g, this);
                    return;
                }
                if (screen == Screen.INTRO_CRAWL) {
                    new IntroCrawlSurfacePainter().paint(g, this);
                    return;
                }
                if (screen == Screen.ZONE_SPLASH || screen == Screen.CAPTURE) {
                    new LoadingSurfacePainter().paint(g, this);
                    return;
                }
                if (screen == Screen.MENU || screen == Screen.MAIN) {
                    new MainMenuSurfacePainter().paint(g, this);
                    return;
                }
                if (screen == Screen.OPTIONS) {
'@
if ($panel.Contains($old1)) { $panel = $panel.Replace($old1, $new1) }

# Key/mouse early-screen controllers already exist. Reconnect them at the bridge level.
if ($panel -notmatch 'installExistingEarlyScreenInputBridge\(\)') {
    $panel = $panel.Replace('        logEvent("GamePanel compatibility bridge initialized.");', '        logEvent("GamePanel compatibility bridge initialized.");' + "`r`n" + '        installExistingEarlyScreenInputBridge();')
    $insert = @'

    void installExistingEarlyScreenInputBridge() {
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent e) {
                if (KeyEarlyScreenController.handleEarlyKey(GamePanel.this, e.getKeyCode())) {
                    repaint();
                    requestFocusInWindow();
                }
            }
        });
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (MouseEarlyScreenController.handleIntroZoneEditorAndAudit(GamePanel.this, e, e.getX(), e.getY())) {
                    repaint();
                    requestFocusInWindow();
                }
            }
        });
    }
'@
    $anchor = '    @Override' + "`r`n" + '    protected void paintComponent(java.awt.Graphics graphics) {'
    $idx = $panel.IndexOf($anchor, [System.StringComparison]::Ordinal)
    if ($idx -lt 0) { throw 'Could not find paintComponent anchor.' }
    $panel = $panel.Substring(0, $idx) + $insert + "`r`n" + $panel.Substring($idx)
}

# Preserve the intended progression: boot -> intro crawl -> main menu.
$panel = $panel.Replace('    void continueFromIntroCrawl() { setScreen(Screen.GAME); }', '    void continueFromIntroCrawl() { setScreen(Screen.MENU); logEvent("Intro crawl completed."); }')
$panel = $panel.Replace('    void finishBootSequence(String source) { setScreen(Screen.MENU); logEvent("Boot sequence finished by " + source + "."); }', '    void finishBootSequence(String source) { setScreen(Screen.INTRO_CRAWL); logEvent("Boot sequence finished by " + source + "."); }')

Set-Content -LiteralPath $panelPath -Value $panel
Write-Host 'Applied existing boot -> intro -> menu wiring and restored established MainMenuSurfacePainter.'
