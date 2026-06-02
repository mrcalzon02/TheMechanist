$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$panelPath = Join-Path $root 'src\mechanist\LegacyPanelContext.java'
$packageScriptPath = Join-Path $root 'scripts\PACKAGE_CLIENT_WINDOWS.ps1'
if (-not (Test-Path -LiteralPath $panelPath -PathType Leaf)) { throw "Missing $panelPath" }
if (-not (Test-Path -LiteralPath $packageScriptPath -PathType Leaf)) { throw "Missing $packageScriptPath" }

$panel = Get-Content -LiteralPath $panelPath -Raw

# Fix compile errors introduced by the visible boot/image bridge patches.
$panel = $panel.Replace('jvmRuntimeProfile.compactLine()', 'jvmRuntimeProfile.targetLabel()')
$panel = $panel.Replace('Graphics2D g = img.createGraphics();', 'java.awt.Graphics2D g = img.createGraphics();')

# Ensure the compatibility panel has a visible real boot/menu surface, not a blank JPanel.
if ($panel -notmatch 'private void paintGameBridgeSurface\(java\.awt\.Graphics2D g, int w, int h\)') {
    $insert = @'

    private void paintGameBridgeSurface(java.awt.Graphics2D g, int w, int h) {
        g.setColor(new java.awt.Color(12, 12, 10));
        g.fillRect(0, 0, w, h);
        int tile = Math.max(16, Math.min(32, Math.min(w, h) / 32));
        for (int y = 0; y < h; y += tile) {
            for (int x = 0; x < w; x += tile) {
                boolean alt = ((x / tile) + (y / tile)) % 2 == 0;
                g.setColor(alt ? new java.awt.Color(28, 29, 25) : new java.awt.Color(22, 23, 20));
                g.fillRect(x, y, tile, tile);
            }
        }
        g.setColor(new java.awt.Color(94, 76, 42));
        for (int x = 0; x < w; x += tile) g.drawLine(x, 0, x, h);
        for (int y = 0; y < h; y += tile) g.drawLine(0, y, w, y);
        g.setFont(uiFont.deriveFont(java.awt.Font.BOLD, 18f));
        g.setColor(new java.awt.Color(225, 205, 140));
        g.drawString("THE MECHANIST - CLIENT SURFACE", 24, 36);
        g.setFont(smallFont);
        g.setColor(new java.awt.Color(205, 210, 195));
        int y = 66;
        for (String line : java.util.List.of(
                "Game surface bridge is painting from GamePanel state.",
                "Screen=" + screen + " Panel=" + panelMode + " Turn=" + turn + " WorldTurn=" + worldTurn,
                "Position=" + playerX + "," + playerY + "  Inventory=" + inventory.size() + "  Log=" + eventLog.size(),
                "Assets root property=" + System.getProperty("mechanist.assetRoot", "."),
                "Generated asset root property=" + System.getProperty("mechanist.generatedAssetRoot", "."))) {
            g.drawString(line, 24, y);
            y += 20;
        }
    }
'@
    $anchor = '    private void drawVisibleBootStatus(java.awt.Graphics2D g, int w, int h) {'
    $idx = $panel.IndexOf($anchor, [System.StringComparison]::Ordinal)
    if ($idx -lt 0) { throw 'Could not find drawVisibleBootStatus anchor.' }
    $panel = $panel.Substring(0, $idx) + $insert + "`r`n" + $panel.Substring($idx)
}

# Route actual known surfaces before fallback text. Main menu and options are existing real surface classes.
$oldPaint = @'
            try {
                if (screen == Screen.MENU || screen == Screen.MAIN || screen == Screen.BOOT) {
                    new MainMenuSurfacePainter().paint(g, this);
                    drawVisibleBootStatus(g, w, h);
                    return;
                }
            } catch (Throwable t) {
                drawBridgeException(g, w, h, t);
                return;
            }
            drawVisibleBootStatus(g, w, h);
'@
$newPaint = @'
            try {
                if (screen == Screen.MENU || screen == Screen.MAIN || screen == Screen.BOOT) {
                    new MainMenuSurfacePainter().paint(g, this);
                    return;
                }
                if (screen == Screen.OPTIONS) {
                    OptionsScreenPainter.paintShell(this, g);
                    OptionsScreenPainter.paintBody(this, g);
                    OptionsScreenPainter.paintGraphicsDropdownPopup(this, g);
                    return;
                }
                if (screen == Screen.GAME || screen == Screen.PANEL || screen == Screen.INVENTORY || screen == Screen.CHARACTER || screen == Screen.INFO || screen == Screen.MAP || screen == Screen.KNOWLEDGE) {
                    paintGameBridgeSurface(g, w, h);
                    return;
                }
            } catch (Throwable t) {
                drawBridgeException(g, w, h, t);
                return;
            }
            drawVisibleBootStatus(g, w, h);
'@
if ($panel.Contains($oldPaint)) {
    $panel = $panel.Replace($oldPaint, $newPaint)
}

Set-Content -LiteralPath $panelPath -Value $panel

$pkg = Get-Content -LiteralPath $packageScriptPath -Raw
$pkg = $pkg.Replace('java -cp "classes;." mechanist.TheMechanist', 'java -Dmechanist.assetRoot=. -Dmechanist.generatedAssetRoot=. -Dmechanist.assetTier=low_32 -cp "classes;." mechanist.TheMechanist')
$pkg = $pkg.Replace('& java -cp "classes;." mechanist.TheMechanist', '& java -Dmechanist.assetRoot=. -Dmechanist.generatedAssetRoot=. -Dmechanist.assetTier=low_32 -cp "classes;." mechanist.TheMechanist')
$pkg = $pkg.Replace('Manual launch: java -cp "classes;." mechanist.TheMechanist', 'Manual launch: java -Dmechanist.assetRoot=. -Dmechanist.generatedAssetRoot=. -Dmechanist.assetTier=low_32 -cp "classes;." mechanist.TheMechanist')
Set-Content -LiteralPath $packageScriptPath -Value $pkg

Write-Host 'Applied client boot surface rewire and package launch asset-root flags.'
