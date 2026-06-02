$ErrorActionPreference = 'Stop'
$panelPath = Join-Path (Split-Path -Parent $PSScriptRoot) 'src\mechanist\LegacyPanelContext.java'
if (-not (Test-Path -LiteralPath $panelPath -PathType Leaf)) { throw "Missing $panelPath" }

$text = Get-Content -LiteralPath $panelPath -Raw

# Ensure the temporary GamePanel facade actually paints something while the real
# renderer is being reconnected. A bare JPanel paints white by default, which
# looks like a frozen boot even when the process is alive.
if ($text -notmatch 'protected void paintComponent\(java\.awt\.Graphics graphics\)') {
    $paint = @'

    @Override
    protected void paintComponent(java.awt.Graphics graphics) {
        super.paintComponent(graphics);
        java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
        try {
            int w = Math.max(1, getWidth());
            int h = Math.max(1, getHeight());
            g.setColor(new java.awt.Color(18, 17, 14));
            g.fillRect(0, 0, w, h);
            g.setColor(new java.awt.Color(72, 61, 38));
            for (int x = 0; x < w; x += 32) g.drawLine(x, 0, x, h);
            for (int y = 0; y < h; y += 32) g.drawLine(0, y, w, y);
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
        } finally {
            g.dispose();
        }
    }

    private void drawVisibleBootStatus(java.awt.Graphics2D g, int w, int h) {
        g.setFont(titleFont.deriveFont(java.awt.Font.BOLD, Math.max(28f, Math.min(52f, h / 10f))));
        g.setColor(new java.awt.Color(218, 198, 126));
        center(g, "THE MECHANIST", w / 2, Math.max(78, h / 5));
        g.setFont(uiFont);
        g.setColor(new java.awt.Color(205, 210, 195));
        int y = Math.max(140, h / 5 + 54);
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("Compatibility bridge boot surface active.");
        lines.add("Screen: " + screen + "  Panel: " + panelMode + "  Turn: " + turn);
        lines.add("Runtime: " + (jvmRuntimeProfile == null ? "none" : jvmRuntimeProfile.compactLine()));
        lines.add("Events: " + eventLog.size() + "  Assets facade: " + (images == null ? "missing" : "ready"));
        lines.add("This fallback confirms Swing is painting while the full client surface is reconnected.");
        for (String line : lines) {
            center(g, line, w / 2, y);
            y += Math.max(22, g.getFontMetrics().getHeight() + 4);
        }
    }

    private void drawBridgeException(java.awt.Graphics2D g, int w, int h, Throwable t) {
        g.setColor(new java.awt.Color(36, 8, 8));
        g.fillRect(0, 0, w, h);
        g.setFont(uiFont);
        g.setColor(new java.awt.Color(255, 180, 150));
        int y = 48;
        g.drawString("The Mechanist bridge renderer caught an exception:", 32, y);
        y += 28;
        g.drawString(t.getClass().getName() + ": " + String.valueOf(t.getMessage()), 32, y);
        y += 28;
        for (StackTraceElement element : t.getStackTrace()) {
            if (y > h - 24) break;
            g.drawString("  at " + element.toString(), 32, y);
            y += 18;
        }
    }
'@
    $needle = '    void runGuarded(String tag, String reason, Runnable body)'
    $idx = $text.IndexOf($needle, [System.StringComparison]::Ordinal)
    if ($idx -lt 0) { throw "Could not find GamePanel method insertion point." }
    $text = $text.Substring(0, $idx) + $paint + "`r`n" + $text.Substring($idx)
}

# Make the panel opaque/focusable so Swing repaints it reliably after being set
# as frame content.
$text = $text.Replace('    GamePanel() {}', @'
    GamePanel() {
        setOpaque(true);
        setBackground(new java.awt.Color(18, 17, 14));
        setFocusable(true);
        logEvent("GamePanel compatibility bridge initialized.");
    }
'@)
$text = $text.Replace('    GamePanel(RuntimeProfile runtimeProfile) {
        if (runtimeProfile != null) logEvent("Runtime profile attached: " + runtimeProfile.compactLine());
    }', @'
    GamePanel(RuntimeProfile runtimeProfile) {
        this();
        if (runtimeProfile != null) logEvent("Runtime profile attached: " + runtimeProfile.compactLine());
    }
'@)
$text = $text.Replace('    GamePanel(JvmRuntimeProfileAuthority.RuntimeConfig runtimeProfile) {
        if (runtimeProfile != null) this.jvmRuntimeProfile = runtimeProfile;
    }', @'
    GamePanel(JvmRuntimeProfileAuthority.RuntimeConfig runtimeProfile) {
        this();
        if (runtimeProfile != null) this.jvmRuntimeProfile = runtimeProfile;
    }
'@)

Set-Content -LiteralPath $panelPath -Value $text
Write-Host 'Applied visible boot fallback to GamePanel compatibility bridge.'
