$ErrorActionPreference = 'Stop'
$panelPath = Join-Path (Split-Path -Parent $PSScriptRoot) 'src\mechanist\LegacyPanelContext.java'
if (-not (Test-Path -LiteralPath $panelPath -PathType Leaf)) { throw "Missing $panelPath" }

$text = Get-Content -LiteralPath $panelPath -Raw

$old = @'
final class LegacyImageSurface {
    void reloadArtQuality(GameOptions options) {}
    BufferedImage get(String key) { return null; }
    BufferedImage getTile(char tile) { return null; }
    BufferedImage getNpcPortraitFor(Object npc) { return null; }
}
'@
$new = @'
final class LegacyImageSurface {
    private final java.util.Map<String, BufferedImage> cache = new java.util.HashMap<>();
    void reloadArtQuality(GameOptions options) { cache.clear(); }
    BufferedImage get(String key) {
        if (key == null || key.isBlank()) return null;
        return cache.computeIfAbsent(key, this::loadByKey);
    }
    BufferedImage getTile(char tile) { return get("tile_" + ((int) tile)); }
    BufferedImage getNpcPortraitFor(Object npc) { return get("portrait_" + (npc == null ? "unknown" : npc.getClass().getSimpleName())); }

    private BufferedImage loadByKey(String key) {
        java.util.List<String> names = candidateNames(key);
        java.util.List<java.nio.file.Path> roots = assetRoots();
        for (java.nio.file.Path root : roots) {
            for (String name : names) {
                java.nio.file.Path found = findCaseInsensitive(root, name);
                if (found != null) {
                    try {
                        BufferedImage image = javax.imageio.ImageIO.read(found.toFile());
                        if (image != null) return image;
                    } catch (java.io.IOException ignored) {}
                }
            }
        }
        if ("title_mechanist_rebase".equalsIgnoreCase(key) || "title_mechanist".equalsIgnoreCase(key)) return generatedTitle();
        if ("subtitle_rebase".equalsIgnoreCase(key)) return generatedSubtitle();
        return null;
    }

    private java.util.List<String> candidateNames(String key) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        String k = key.trim();
        String lower = k.toLowerCase(java.util.Locale.ROOT);
        for (String base : new String[] { k, lower, k.replace('_', '-'), lower.replace('_', '-') }) {
            out.add(base);
            for (String ext : new String[] { ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp" }) out.add(base + ext);
        }
        return out;
    }

    private java.util.List<java.nio.file.Path> assetRoots() {
        java.util.ArrayList<java.nio.file.Path> roots = new java.util.ArrayList<>();
        java.nio.file.Path cwd = java.nio.file.Paths.get("").toAbsolutePath().normalize();
        roots.add(cwd.resolve("assets"));
        roots.add(cwd.resolve("PACKAGE_client").resolve("assets"));
        roots.add(cwd.resolve("client").resolve("assets"));
        roots.add(cwd.resolve("resources").resolve("assets"));
        String configured = System.getProperty("mechanist.assetRoot", "");
        if (configured != null && !configured.isBlank()) roots.add(java.nio.file.Paths.get(configured).toAbsolutePath().normalize().resolve("assets"));
        return roots;
    }

    private java.nio.file.Path findCaseInsensitive(java.nio.file.Path root, String wanted) {
        if (root == null || wanted == null || !java.nio.file.Files.isDirectory(root)) return null;
        java.nio.file.Path direct = root.resolve(wanted);
        if (java.nio.file.Files.isRegularFile(direct)) return direct;
        String target = wanted.replace('\\', '/').toLowerCase(java.util.Locale.ROOT);
        try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.walk(root)) {
            return stream.filter(java.nio.file.Files::isRegularFile)
                    .filter(p -> p.getFileName() != null)
                    .filter(p -> {
                        String file = p.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
                        String rel = root.relativize(p).toString().replace('\\', '/').toLowerCase(java.util.Locale.ROOT);
                        return file.equals(target) || rel.equals(target) || file.startsWith(target + ".");
                    })
                    .findFirst().orElse(null);
        } catch (java.io.IOException ignored) { return null; }
    }

    private BufferedImage generatedTitle() {
        BufferedImage img = new BufferedImage(760, 130, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new java.awt.Color(20, 18, 14, 230));
        g.fillRoundRect(0, 0, img.getWidth(), img.getHeight(), 22, 22);
        g.setColor(new java.awt.Color(190, 155, 80));
        g.setStroke(new java.awt.BasicStroke(4f));
        g.drawRoundRect(4, 4, img.getWidth() - 9, img.getHeight() - 9, 22, 22);
        g.setFont(new java.awt.Font("Serif", java.awt.Font.BOLD, 64));
        g.setColor(new java.awt.Color(225, 205, 140));
        String title = "THE MECHANIST";
        java.awt.FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (img.getWidth() - fm.stringWidth(title)) / 2, 84);
        g.dispose();
        return img;
    }

    private BufferedImage generatedSubtitle() {
        BufferedImage img = new BufferedImage(500, 46, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 20));
        g.setColor(new java.awt.Color(170, 150, 105));
        String text = "UNFOLDED CLIENT BRIDGE";
        java.awt.FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (img.getWidth() - fm.stringWidth(text)) / 2, 30);
        g.dispose();
        return img;
    }
}
'@

if ($text.Contains($old)) {
    $text = $text.Replace($old, $new)
} else {
    $pattern = '(?s)final class LegacyImageSurface \{.*?\}\s*\r?\n\r?\nfinal class LegacyFirstPersonRenderViewport'
    if ($text -notmatch $pattern) { throw 'Could not locate LegacyImageSurface block.' }
    $text = [regex]::Replace($text, $pattern, $new + "`r`n`r`nfinal class LegacyFirstPersonRenderViewport", 1)
}

Set-Content -LiteralPath $panelPath -Value $text
Write-Host 'Applied real image surface loader to LegacyImageSurface.'
