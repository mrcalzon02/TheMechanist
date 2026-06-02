$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$panelPath = Join-Path $root 'src\mechanist\LegacyPanelContext.java'
$bootSmokePath = Join-Path $root 'scripts\BOOT_SMOKE_PACKAGE_CLIENT_WINDOWS.ps1'
if (-not (Test-Path -LiteralPath $panelPath -PathType Leaf)) { throw "Missing $panelPath" }
if (-not (Test-Path -LiteralPath $bootSmokePath -PathType Leaf)) { throw "Missing $bootSmokePath" }

$panel = Get-Content -LiteralPath $panelPath -Raw

$old = @'
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
        java.awt.Graphics2D g = img.createGraphics();
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
        java.awt.Graphics2D g = img.createGraphics();
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

$new = @'
final class LegacyImageSurface {
    private final java.util.Map<String, BufferedImage> cache = new java.util.HashMap<>();
    private java.util.List<PackageAssetIndexRow> packageIndex;
    void reloadArtQuality(GameOptions options) { cache.clear(); packageIndex = null; }
    BufferedImage get(String key) {
        if (key == null || key.isBlank()) return null;
        return cache.computeIfAbsent(key, this::loadByKey);
    }
    BufferedImage getTile(char tile) { return get("tile_" + ((int) tile)); }
    BufferedImage getNpcPortraitFor(Object npc) { return get("portrait_" + (npc == null ? "unknown" : npc.getClass().getSimpleName())); }

    private BufferedImage loadByKey(String key) {
        java.nio.file.Path indexed = findIndexedPackageAsset(key);
        BufferedImage indexedImage = readImage(indexed);
        if (indexedImage != null) return indexedImage;
        java.util.List<String> names = candidateNames(key);
        for (java.nio.file.Path root : assetRoots()) {
            for (String name : names) {
                BufferedImage image = readImage(findCaseInsensitive(root, name));
                if (image != null) return image;
            }
        }
        if ("title_mechanist_rebase".equalsIgnoreCase(key) || "title_mechanist".equalsIgnoreCase(key)) return generatedTitle("THE MECHANIST", "PACKAGE ASSET FALLBACK");
        if ("subtitle_rebase".equalsIgnoreCase(key)) return generatedSubtitle("INDEXED 32PX ASSET PACKAGE ACTIVE");
        return null;
    }

    private BufferedImage readImage(java.nio.file.Path path) {
        if (path == null || !java.nio.file.Files.isRegularFile(path)) return null;
        try { return javax.imageio.ImageIO.read(path.toFile()); } catch (java.io.IOException ignored) { return null; }
    }

    private java.nio.file.Path findIndexedPackageAsset(String key) {
        String wanted = normalizeToken(key);
        java.util.List<PackageAssetIndexRow> rows = packageIndexRows();
        if (rows.isEmpty()) return null;
        for (PackageAssetIndexRow row : rows) if (normalizeToken(row.assetId).equals(wanted)) return row.path;
        for (PackageAssetIndexRow row : rows) if (normalizeToken(row.assetId).contains(wanted) || normalizeToken(row.path.getFileName().toString()).contains(wanted)) return row.path;
        if (wanted.contains("title") || wanted.contains("subtitle") || wanted.contains("menu")) {
            for (PackageAssetIndexRow row : rows) {
                String hay = normalizeToken(row.assetId + " " + row.category + " " + row.tags + " " + row.path.getFileName());
                if (hay.contains("interface") || hay.contains("ui") || hay.contains("icon")) return row.path;
            }
        }
        return null;
    }

    private java.util.List<PackageAssetIndexRow> packageIndexRows() {
        if (packageIndex != null) return packageIndex;
        java.util.ArrayList<PackageAssetIndexRow> rows = new java.util.ArrayList<>();
        java.util.LinkedHashSet<java.nio.file.Path> indexes = new java.util.LinkedHashSet<>();
        for (java.nio.file.Path root : assetRoots()) {
            indexes.add(root.resolve("indexes").resolve("asset_content_index_32px.tsv"));
            indexes.add(root.resolve("indexes").resolve("asset_content_index_256px.tsv"));
        }
        for (java.nio.file.Path index : indexes) readIndex(index, rows);
        packageIndex = rows;
        return packageIndex;
    }

    private void readIndex(java.nio.file.Path index, java.util.List<PackageAssetIndexRow> rows) {
        if (index == null || !java.nio.file.Files.isRegularFile(index)) return;
        java.nio.file.Path assetRoot = index.getParent() == null ? null : index.getParent().getParent();
        if (assetRoot == null) return;
        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(index);
            boolean header = true;
            for (String line : lines) {
                if (header) { header = false; continue; }
                if (line == null || line.isBlank()) continue;
                String[] cols = line.split("\t", -1);
                if (cols.length < 2) continue;
                String rel = rewritePackagePath(cols[1]);
                java.nio.file.Path path = assetRoot.resolve("compiled_assets").resolve(rel).normalize();
                String assetId = cols.length > 0 ? cols[0] : "";
                String category = cols.length > 7 ? cols[7] : "";
                String tags = cols.length > 11 ? cols[11] : "";
                if (java.nio.file.Files.isRegularFile(path)) rows.add(new PackageAssetIndexRow(assetId, category, tags, path));
            }
        } catch (java.io.IOException ignored) {}
    }

    private String rewritePackagePath(String path) {
        if (path == null) return "";
        String resolution = System.getProperty("mechanist.assetResolution", "32");
        String p = path.trim().replace('\\', '/');
        p = p.replaceFirst("^256px/", resolution + "px/");
        p = p.replace("_256px.", "_" + resolution + "px.");
        p = p.replaceFirst("^" + java.util.regex.Pattern.quote(resolution + "px/") , "");
        return p;
    }

    private String normalizeToken(String value) {
        if (value == null) return "";
        return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
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
        String configured = System.getProperty("mechanist.assetRoot", "");
        if (configured != null && !configured.isBlank()) roots.add(java.nio.file.Paths.get(configured).toAbsolutePath().normalize().resolve("assets"));
        roots.add(cwd.resolve("assets"));
        roots.add(cwd.resolve("PACKAGE_client").resolve("assets"));
        roots.add(cwd.resolve("client").resolve("assets"));
        roots.add(cwd.resolve("resources").resolve("assets"));
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

    private BufferedImage generatedTitle(String title, String subtitle) {
        BufferedImage img = new BufferedImage(760, 130, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new java.awt.Color(20, 18, 14, 230));
        g.fillRoundRect(0, 0, img.getWidth(), img.getHeight(), 22, 22);
        g.setColor(new java.awt.Color(190, 155, 80));
        g.setStroke(new java.awt.BasicStroke(4f));
        g.drawRoundRect(4, 4, img.getWidth() - 9, img.getHeight() - 9, 22, 22);
        g.setFont(new java.awt.Font("Serif", java.awt.Font.BOLD, 56));
        g.setColor(new java.awt.Color(225, 205, 140));
        java.awt.FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (img.getWidth() - fm.stringWidth(title)) / 2, 74);
        g.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 14));
        fm = g.getFontMetrics();
        g.drawString(subtitle, (img.getWidth() - fm.stringWidth(subtitle)) / 2, 102);
        g.dispose();
        return img;
    }

    private BufferedImage generatedSubtitle(String text) {
        BufferedImage img = new BufferedImage(500, 46, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 20));
        g.setColor(new java.awt.Color(170, 150, 105));
        java.awt.FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (img.getWidth() - fm.stringWidth(text)) / 2, 30);
        g.dispose();
        return img;
    }

    private static final class PackageAssetIndexRow {
        final String assetId;
        final String category;
        final String tags;
        final java.nio.file.Path path;
        PackageAssetIndexRow(String assetId, String category, String tags, java.nio.file.Path path) {
            this.assetId = assetId == null ? "" : assetId;
            this.category = category == null ? "" : category;
            this.tags = tags == null ? "" : tags;
            this.path = path;
        }
    }
}
'@

if (-not $panel.Contains($old)) { throw 'LegacyImageSurface block did not match expected package-resolver source. Fetch and patch manually.' }
$panel = $panel.Replace($old, $new)
Set-Content -LiteralPath $panelPath -Value $panel

$boot = Get-Content -LiteralPath $bootSmokePath -Raw
$boot = $boot.Replace('$argLine = ''-cp "classes;." mechanist.TheMechanist''', '$argLine = ''-Dmechanist.assetRoot=. -Dmechanist.generatedAssetRoot=. -Dmechanist.assetTier=low_32 -Dmechanist.assetResolution=32 -cp "classes;." mechanist.TheMechanist''')
$boot = $boot.Replace("@('-cp', 'classes;.', 'mechanist.TheMechanist')", "@('-Dmechanist.assetRoot=.', '-Dmechanist.generatedAssetRoot=.', '-Dmechanist.assetTier=low_32', '-Dmechanist.assetResolution=32', '-cp', 'classes;.', 'mechanist.TheMechanist')")
Set-Content -LiteralPath $bootSmokePath -Value $boot

Write-Host 'Applied package asset index resolver and boot-smoke asset flags.'
