package mechanist;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.Locale;
import mechanist.assets.AssetManager;
import mechanist.assets.AssetType;

class PortraitSheetProfile {
    String label, path;
    int width, height, cellW, cellH, cols, rows, total, offsetX, offsetY;
    boolean playerAllowed;
    PortraitSheetProfile(String label, String path, int width, int height, int cellW, int cellH, int cols, int rows, int offsetX, int offsetY, boolean playerAllowed) {
        this.label=label; this.path=path; this.width=width; this.height=height; this.cellW=cellW; this.cellH=cellH; this.cols=cols; this.rows=rows; this.offsetX=offsetX; this.offsetY=offsetY; this.total=cols*rows; this.playerAllowed=playerAllowed;
    }
    static PortraitSheetProfile infer(String label, String path, int width, int height, boolean playerAllowed) {
        int[] candidates = new int[]{64, 96, 112, 120, 124, 128, 160};
        int bestW=128, bestH=128, bestCols=Math.max(1,width/128), bestRows=Math.max(1,height/128), bestWaste=Integer.MAX_VALUE;
        for (int cw: candidates) for (int ch: candidates) {
            int cols = width / cw, rows = height / ch;
            if (cols < 1 || rows < 1) continue;
            int waste = (width - cols*cw) + (height - rows*ch);
            int cells = cols*rows;
            int score = waste + Math.abs(cw-ch)*2 + Math.abs(80-cells);
            if (score < bestWaste) { bestWaste=score; bestW=cw; bestH=ch; bestCols=cols; bestRows=rows; }
        }
        // For the normal human sheet we deliberately rely on pre-sliced verified cells.
        // Inference here is diagnostic only, used for logs and reserved NPC sheet correction.
        int ox = Math.max(0, (width - bestCols*bestW)/2);
        int oy = Math.max(0, (height - bestRows*bestH)/2);
        return new PortraitSheetProfile(label, path, width, height, bestW, bestH, bestCols, bestRows, ox, oy, playerAllowed);
    }
    String toAuditLine() {
        return label + " path=" + path + " image=" + width + "x" + height + " cell=" + cellW + "x" + cellH + " cols=" + cols + " rows=" + rows + " total=" + total + " offset=" + offsetX + "," + offsetY + " playerAllowed=" + playerAllowed;
    }
}

class AudioPackManager {
    static String prepareAndResolveMusicRoot(String packDirName, String cacheDirName, String bundledFallbackRoot) {
        try {
            Path packDir = Paths.get(packDirName);
            Path cacheDir = Paths.get(cacheDirName);
            Files.createDirectories(packDir);
            Files.createDirectories(cacheDir);
            ArrayList<Path> zips = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(packDir, "*.zip")) {
                for (Path p : stream) zips.add(p);
            } catch (Throwable ignored) {}
            Collections.sort(zips, Comparator.comparing(Path::toString));
            for (Path zip : zips) unpackIfNeeded(zip, cacheDir.resolve(safeStem(zip.getFileName().toString())));
            String resolved = findWavRoot(cacheDir);
            if (resolved != null) { DebugLog.audit("AUDIO_PACK", "Resolved external music root=" + resolved); return resolved; }
        } catch (Throwable t) {
            DebugLog.error("AUDIO_PACK", "Audio-pack prepare failed; falling back to bundled music if present.", t);
        }
        String fallbackRoot = resolveBundledMusicRoot(bundledFallbackRoot);
        if (Files.isDirectory(Paths.get(fallbackRoot))) {
            DebugLog.audit("AUDIO_PACK", "Using bundled music root=" + fallbackRoot);
            return fallbackRoot;
        }
        DebugLog.warn("AUDIO_PACK", "No external or bundled music pack found. Dynamic music will remain silent unless music assets are installed.");
        return fallbackRoot;
    }

    static String resolveBundledMusicRoot(String bundledFallbackRoot) {
        LinkedHashSet<Path> candidates = new LinkedHashSet<>();
        addMusicRootCandidate(candidates, bundledFallbackRoot);

        String normalized = normalizeSlashes(bundledFallbackRoot);
        if (normalized.startsWith("packages/client/")) {
            addMusicRootCandidate(candidates, normalized.substring("packages/client/".length()));
        }
        if (normalized.startsWith("client/")) {
            addMusicRootCandidate(candidates, normalized.substring("client/".length()));
        }
        if (normalized.startsWith("PACKAGE_client/")) {
            addMusicRootCandidate(candidates, normalized.substring("PACKAGE_client/".length()));
        }

        addMusicRootCandidate(candidates, "assets/music/wav");
        addMusicRootCandidate(candidates, "PACKAGE_client/assets/music/wav");
        addMusicRootCandidate(candidates, "client/assets/music/wav");
        addMusicRootCandidate(candidates, "packages/client/assets/music/wav");

        for (Path candidate : candidates) {
            if (hasDirectWavFiles(candidate)) {
                return candidate.toAbsolutePath().normalize().toString();
            }
        }

        File resolved = RuntimePathResolver.resolveAssetFile(bundledFallbackRoot);
        return resolved.getPath();
    }

    private static void addMusicRootCandidate(LinkedHashSet<Path> candidates, String rawPath) {
        if (rawPath == null || rawPath.isBlank()) return;
        candidates.add(Paths.get(rawPath));
        candidates.add(RuntimePathResolver.resolveAssetFile(rawPath).toPath());
    }

    private static boolean hasDirectWavFiles(Path root) {
        if (root == null || !Files.isDirectory(root)) return false;
        try (DirectoryStream<Path> wavs = Files.newDirectoryStream(root, "*.wav")) {
            return wavs.iterator().hasNext();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static String normalizeSlashes(String path) {
        return path == null ? "" : path.replace('\\', '/').replaceAll("^/+", "");
    }

    static String findWavRoot(Path cacheDir) {
        if (!Files.isDirectory(cacheDir)) return null;
        ArrayList<Path> hits = new ArrayList<>();
        try (java.util.stream.Stream<Path> stream = Files.walk(cacheDir, 7)) {
            stream.filter(Files::isDirectory).forEach(p -> {
                try (DirectoryStream<Path> wavs = Files.newDirectoryStream(p, "*.wav")) {
                    Iterator<Path> it = wavs.iterator();
                    if (it.hasNext()) hits.add(p);
                } catch (Throwable ignored) {}
            });
        } catch (Throwable ignored) {}
        if (hits.isEmpty()) return null;
        hits.sort(Comparator.comparing(Path::toString));
        return hits.get(hits.size()-1).toString();
    }

    static String safeStem(String filename) {
        String s = filename == null ? "audiopack" : filename.replaceAll("(?i)\\.zip$", "");
        s = s.replaceAll("[^A-Za-z0-9._-]+", "_");
        return s.isBlank() ? "audiopack" : s;
    }

    static void unpackIfNeeded(Path zip, Path dest) {
        try {
            Files.createDirectories(dest);
            Path marker = dest.resolve(".unpacked.marker");
            String stamp = zip.toAbsolutePath() + "|" + Files.size(zip) + "|" + Files.getLastModifiedTime(zip).toMillis();
            if (Files.exists(marker) && Files.readString(marker).equals(stamp)) return;
            deleteTreeContents(dest);
            Files.createDirectories(dest);
            try (java.util.zip.ZipInputStream zin = new java.util.zip.ZipInputStream(Files.newInputStream(zip))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zin.getNextEntry()) != null) {
                    Path out = dest.resolve(entry.getName()).normalize();
                    if (!out.startsWith(dest)) { DebugLog.warn("AUDIO_PACK", "Skipped suspicious zip entry " + entry.getName()); continue; }
                    if (entry.isDirectory()) Files.createDirectories(out);
                    else {
                        Files.createDirectories(out.getParent());
                        Files.copy(zin, out, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zin.closeEntry();
                }
            }
            Files.writeString(marker, stamp);
            DebugLog.audit("AUDIO_PACK", "Unpacked " + zip + " -> " + dest);
        } catch (Throwable t) {
            DebugLog.error("AUDIO_PACK", "Could not unpack audio pack " + zip, t);
        }
    }

    static void deleteTreeContents(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        ArrayList<Path> paths = new ArrayList<>();
        try (java.util.stream.Stream<Path> stream = Files.walk(dir)) { stream.forEach(paths::add); }
        paths.sort(Comparator.reverseOrder());
        for (Path p : paths) if (!p.equals(dir)) Files.deleteIfExists(p);
    }
}

class ImageCache {
    static final int PLAYER_BASELINE_HUMAN_POOL = 0;
    static final int NPC_AUGMENTED_POOL = 1;
    final Map<String, BufferedImage> cache = new HashMap<>();
    final ArrayList<BufferedImage> bootFrames = new ArrayList<>();
    final ArrayList<BufferedImage> portraitSheets = new ArrayList<>();
    final ArrayList<BufferedImage> playerHumanPortraitCells = new ArrayList<>();
    private BufferedImage generatedPlayerHumanFallbackPortrait;
    final ArrayList<BufferedImage> npcPortraitCells = new ArrayList<>();
    final LinkedHashMap<String, BufferedImage> nameLockedProfilePortraits = new LinkedHashMap<>();
    final LinkedHashMap<String,int[]> npcPortraitRanges = new LinkedHashMap<>();
    final ArrayList<PortraitSheetProfile> portraitProfiles = new ArrayList<>();
    final TileArtSystem tileArt = new TileArtSystem(new TileImageRegistry());
    final Map<String, BufferedImage> semanticAssetImageCache = new HashMap<>();
    String artRootPath = "assets/a/r";
    final String base = "assets/imported_tech_priests/graphics/gui/";

    void load(GameOptions options) {
        semanticAssetImageCache.clear();
        String sliceBase = base + "cogitator_frame_0536/slices_384/";
        String[] keys = {
            "corner_top_left", "corner_top_right", "corner_bottom_left", "corner_bottom_right",
            "top_rail_left_mid", "bottom_rail_left_mid", "left_column_mid", "right_column_mid",
            "inner_bezel_t", "inner_bezel_b", "inner_bezel_l", "inner_bezel_r", "inner_display_center"
        };
        for (String k : keys) load(k, sliceBase + k + ".png");
        String medallionBase = base + "rough-assets/medallion_spin/frame_";
        for (int i=1;i<=8;i++) {
            BufferedImage img = read(String.format(Locale.US, "%s%02d.png", medallionBase, i));
            if (img != null) bootFrames.add(img);
        }
        // Fallback only: the red gear is no longer the intended spinner.
        BufferedImage emblem = read(base + "rough-assets/source_sheets_cleaned/medallion_spin_sheet.png");
        if (emblem != null && bootFrames.isEmpty()) cache.put("mechanical_skull_gear_emblem", emblem);
        loadPortraitSheet("assets/imported_tech_priests/graphics/lean/gui/tech_priest_augmented_portrait_sheet_a__lean50.png");
        loadPortraitSheet("assets/imported_tech_priests/graphics/lean/gui/baseline_human_portrait_sheet__lean50.png");
        loadPortraitSheet("assets/imported_tech_priests/graphics/lean/gui/alternative_human_augmented_portrait_sheet_c__lean50.png");
        loadPortraitSheet("assets/imported_tech_priests/graphics/lean/gui/planetary_magos_portrait_sheet_a__lean50.png");
        loadPortraitSheet("assets/imported_tech_priests/graphics/gui/portraits/tech_priest_augmented_portrait_sheet_a.png");
        loadPortraitSheet("assets/imported_tech_priests/graphics/gui/portraits/baseline_human_portrait_sheet.png");
        loadPortraitSheet("assets/imported_tech_priests/graphics/gui/portraits/alternative_human_augmented_portrait_sheet_c.png");
        loadPortraitSheet("assets/imported_tech_priests/graphics/gui/portraits/planetary_magos_portrait_sheet_a.png");
        load("title_mechanist", "assets/generated/the_mechanist_title.png");
        String artQuality = options == null ? "low_32" : options.artQualityFolder();
        tileArt.loadTileArt("packages/client/assets/artpacks", "cache/artpacks", "packages/client/assets/a/r", artQuality);
        artRootPath = tileArt.getActiveArtRoot();
        load("title_mechanist_rebase", artRootPath + "/source/Title/TITEL.png");
        load("subtitle_rebase", artRootPath + "/source/Title/Sub title.png");
        load("new_world_backdrop_rebase", artRootPath + "/source/Background/Backdrop.png");
        load("clouds_slow_rebase", artRootPath + "/source/Background/CLOUDS1slow.png");
        load("clouds_fast_rebase", artRootPath + "/source/Background/Clouds2fast.png");
        String portraitQuality = options == null ? "low_32" : options.artQualityFolder();
        loadPortraitCellTree(ArtPackManager.resolveQualityCellsRoot(artRootPath, portraitQuality) + "/Protraits");
        loadExplicitPlayerHumanPortraitPool(ArtPackManager.resolveQualityCellsRoot(artRootPath, portraitQuality) + "/Protraits");
        // Button authority: use the imported Tech Priests GUI controls folder.
        load("button_normal", base + "controls/normal/03_rect_button_off.png");
        load("button_hover", base + "controls/normal/04_rect_button_on.png");
        load("button_disabled", base + "controls/disabled/03_rect_button_off.png");
        load("button_round_normal", base + "controls/normal/01_round_button_off.png");
        load("button_round_hover", base + "controls/normal/02_round_button_on.png");
        loadPortraitCells("assets/imported_tech_priests/graphics/gui/portraits/cells_0560");
        loadNameLockedProfilePortraits();
        inspectPortraitSheet("PLAYER_BASELINE_ONLY", "assets/imported_tech_priests/graphics/gui/portraits/baseline_human_portrait_sheet.png", true);
        inspectPortraitSheet("NPC_ALT_AUGMENTED_C_DIAGNOSTIC", "assets/imported_tech_priests/graphics/gui/portraits/alternative_human_augmented_portrait_sheet_c.png", false);
        inspectPortraitSheet("NPC_PLANETARY_MAGOS_DIAGNOSTIC", "assets/imported_tech_priests/graphics/gui/portraits/planetary_magos_portrait_sheet_a.png", false);
        inspectPortraitSheet("NPC_TECH_PRIEST_AUGMENTED_DIAGNOSTIC", "assets/imported_tech_priests/graphics/gui/portraits/tech_priest_augmented_portrait_sheet_a.png", false);
        DebugLog.audit("ASSETS", "Loaded GUI frame slices=" + cache.size() + " bootFrames=" + bootFrames.size() + " portraitSheets=" + portraitSheets.size() + " playerHumanPortraitCells=" + playerHumanPortraitCells.size() + " npcPortraitCells=" + npcPortraitCells.size() + " tileArt=" + tileArt.getRegistry().loadedCount());
    }

    void load(String key, String path) { BufferedImage img = read(path); if (img != null) cache.put(key, img); }
    void loadPortraitSheet(String path) { BufferedImage img = read(path); if (img != null) portraitSheets.add(img); }
    void loadPortraitCells(String dirPath) {
        File dir = new File(dirPath);
        File[] files = dir.exists() ? dir.listFiles((d,n) -> n.toLowerCase(Locale.US).endsWith(".png")) : null;
        if (files == null) return;
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File f: files) {
            try {
                BufferedImage img = ImageIO.read(f);
                if (img == null) continue;
                String name = f.getName().toLowerCase(Locale.ROOT);
                if (name.contains("baseline-human") || name.contains("baseline_human") || name.contains("base-human") || name.contains("base_human")) {
                    playerHumanPortraitCells.add(img);
                } else {
                    // NPC-only until source sheet slicing is verified.
                    npcPortraitCells.add(img);
                }
            } catch (Exception ex) {
                DebugLog.error("PORTRAIT_CELL_LOAD", "failed loading portrait cell " + f.getName(), ex);
            }
        }
        DebugLog.audit("PORTRAIT_AUTHORITY", "PLAYER_POOL=baseline-human-only count=" + playerHumanPortraitCells.size() + "; NPC_POOL=non-baseline count=" + npcPortraitCells.size());
    }

    void loadNameLockedProfilePortraits() {
        nameLockedProfilePortraits.clear();
        int unavailable = 0;
        int index = 0;
        for (NameLockedProfilePortraitAuthority.Entry e : NameLockedProfilePortraitAuthority.entries()) {
            BufferedImage img = read(e.assetPath);
            if (img == null) img = readSpecialProfilePortrait(index);
            if (img != null) nameLockedProfilePortraits.put(e.key, img);
            else unavailable++;
            index++;
        }
        DebugLog.audit("NAME_LOCKED_PROFILE_PORTRAIT", NameLockedProfilePortraitAuthority.auditSummary()
                + " loaded=" + nameLockedProfilePortraits.size()
                + " unavailable=" + unavailable
                + " status=" + (nameLockedProfilePortraits.isEmpty() ? "no-special-profile-cells-loaded" : "compiled-special-profile-cells"));
    }

    BufferedImage readSpecialProfilePortrait(int index) {
        if (index < 0) return null;
        int row = index / 5 + 1;
        int col = index % 5 + 1;
        LinkedHashSet<Integer> resolutions = new LinkedHashSet<>();
        resolutions.add(assetResolutionProperty());
        resolutions.add(resolutionForQualityTier(System.getProperty("mechanist.assetTier", System.getProperty("mechanist.graphicsTier", ""))));
        resolutions.add(32);
        resolutions.add(64);
        resolutions.add(128);
        resolutions.add(256);
        for (int resolution : resolutions) {
            if (resolution <= 0) continue;
            String filename = String.format(Locale.US, "Specialprofiles_r%02dc%02d_%dpx.png", row, col, resolution);
            BufferedImage img = read("assets/compiled_assets/" + resolution + "px/Protraits/" + filename);
            if (img != null) return img;
        }
        return null;
    }

    int assetResolutionProperty() {
        String raw = System.getProperty("mechanist.assetResolution", "");
        try {
            return Integer.parseInt(raw.trim().replaceAll("[^0-9]", ""));
        } catch (Exception ignored) {
            return 32;
        }
    }

    int resolutionForQualityTier(String tier) {
        String t = tier == null ? "" : tier.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (t) {
            case "standard_64", "standard64", "64" -> 64;
            case "intermediate_128", "intermediate128", "128" -> 128;
            case "high_native", "native", "high", "256" -> 256;
            default -> 32;
        };
    }

    BufferedImage getNameLockedProfilePortrait(String key) {
        if (key == null || key.isBlank()) return null;
        return nameLockedProfilePortraits.get(key);
    }
    void loadPortraitCellTree(String dirPath) {
        File root = new File(dirPath);
        if (!root.exists()) return;
        ArrayList<File> dirs = new ArrayList<>();
        File[] children = root.listFiles();
        if (children != null) for (File f : children) if (f.isDirectory()) dirs.add(f);
        Collections.sort(dirs, Comparator.comparing(File::getPath));
        int loaded = 0;
        for (File dir : dirs) {
            ArrayList<File> files = new ArrayList<>();
            collectPngFiles(dir, files);
            Collections.sort(files, Comparator.comparing(File::getPath));
            int start = npcPortraitCells.size();
            for (File f : files) {
                try {
                    BufferedImage img = ImageIO.read(f);
                    if (img != null) { npcPortraitCells.add(img); loaded++; }
                } catch (Exception ex) {
                    DebugLog.error("PORTRAIT_REBASE_LOAD", "failed loading portrait cell " + f.getPath(), ex);
                }
            }
            int end = npcPortraitCells.size();
            String rangeKey = dir.getName().toLowerCase(Locale.ROOT);
            if (end > start) {
                npcPortraitRanges.put(rangeKey, new int[]{start, end});
            }
        }
        DebugLog.audit("PORTRAIT_REBASE", "loaded CRT portrait cells=" + loaded + " ranges=" + npcPortraitRanges.keySet() + " playerHumanPool=" + playerHumanPortraitCells.size());
    }

    void loadExplicitPlayerHumanPortraitPool(String protraitsRootPath) {
        // PLAYER PROFILE/CHARACTER PORTRAIT AUTHORITY:
        // Do not promote every entity/faction portrait folder into the player pool.
        // The player-human/profile pool is loaded only from explicit human/profile buckets.
        // The administratum bucket is the packaged ordinary-human fallback only when no
        // baseline_human/player_human/profile_human folder exists in the art pack.
        if (protraitsRootPath == null) return;
        File root = new File(protraitsRootPath);
        if (!root.isDirectory()) return;
        String[] preferred = new String[]{
            "human_profiles", "humans", "human", "baseline_human", "base_human",
            "player_human", "player_humans", "standard_human", "standard_profiles",
            "profile_human", "profile_humans", "profiles"
        };
        boolean loaded = false;
        for (String key : preferred) loaded |= addPortraitDirectoryToPlayerPool(new File(root, key));
        if (!loaded) {
            // Ordinary human fallback. This is still a single named folder, not the whole entity bin.
            loaded = addPortraitDirectoryToPlayerPool(new File(root, "administratum"));
        }
        DebugLog.audit("PLAYER_HUMAN_PORTRAIT_POOL", "explicitFolderLoaded=" + loaded + " count=" + playerHumanPortraitCells.size());
    }

    boolean addPortraitDirectoryToPlayerPool(File dir) {
        if (dir == null || !dir.isDirectory()) return false;
        ArrayList<File> files = new ArrayList<>();
        collectPngFiles(dir, files);
        Collections.sort(files, Comparator.comparing(File::getPath));
        int before = playerHumanPortraitCells.size();
        for (File f : files) {
            try {
                BufferedImage img = ImageIO.read(f);
                if (img != null) playerHumanPortraitCells.add(img);
            } catch (Exception ex) {
                DebugLog.error("PLAYER_HUMAN_PORTRAIT_LOAD", "failed loading player-human portrait " + f.getPath(), ex);
            }
        }
        return playerHumanPortraitCells.size() > before;
    }

    boolean isPlayerHumanPortraitDirectory(String key) {
        if (key == null) return false;
        String k = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        // Player creation is restricted to an explicit baseline/base-human bucket only.
        // Faction folders such as administratum, gangers, nobles, PDF, Arbites,
        // Mechanicus, pets, beasts, mutants, and cultists are NPC/faction authority
        // pools and must never be silently promoted into the player pool.
        return k.equals("baseline_human") || k.equals("base_human") || k.equals("player_baseline_human") || k.equals("normal_human") || k.equals("humans_base");
    }

    void collectPngFiles(File dir, ArrayList<File> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) collectPngFiles(f, out);
            else if (f.getName().toLowerCase(Locale.US).endsWith(".png")) out.add(f);
        }
    }

    void inspectPortraitSheet(String label, String path, boolean playerAllowed) {
        BufferedImage img = read(path);
        if (img == null) {
            DebugLog.audit("PORTRAIT_PROFILE", label + " status=optional-sheet-not-present path=" + path);
            return;
        }
        PortraitSheetProfile best = PortraitSheetProfile.infer(label, path, img.getWidth(), img.getHeight(), playerAllowed);
        portraitProfiles.add(best);
        DebugLog.audit("PORTRAIT_PROFILE", best.toAuditLine());
    }
    void reloadArtQuality(GameOptions options) {
        semanticAssetImageCache.clear();
        String artQuality = options == null ? "low_32" : options.artQualityFolder();
        tileArt.loadTileArt("packages/client/assets/artpacks", "cache/artpacks", "packages/client/assets/a/r", artQuality);
        artRootPath = tileArt.getActiveArtRoot();
        npcPortraitCells.clear();
        npcPortraitRanges.clear();
        playerHumanPortraitCells.clear();
        String portraitQuality = options == null ? "low_32" : options.artQualityFolder();
        loadPortraitCellTree(ArtPackManager.resolveQualityCellsRoot(artRootPath, portraitQuality) + "/Protraits");
        loadExplicitPlayerHumanPortraitPool(ArtPackManager.resolveQualityCellsRoot(artRootPath, portraitQuality) + "/Protraits");
        loadPortraitCells("assets/imported_tech_priests/graphics/gui/portraits/cells_0560");
        loadNameLockedProfilePortraits();
        DebugLog.audit("ART_QUALITY", "Reloaded art cache root=" + artRootPath + " quality=" + (options == null ? "low_32" : options.artQualityFolder()) + " tileGlyphs=" + tileArt.getRegistry().loadedCount() + " semantic=" + tileArt.getRegistry().semanticCount() + " npcPortraitCells=" + npcPortraitCells.size() + " nameLockedProfilePortraits=" + nameLockedProfilePortraits.size());
    }
    BufferedImage getPortrait(int sheetIndex, int portraitIndex) {
        // PLAYER CHARACTER/PROFILE DEFAULTS ARE LOCKED TO THE PLAYER-HUMAN POOL ONLY.
        // Never fall through into NPC faction buckets or name_locked celebrity portraits.
        if (!playerHumanPortraitCells.isEmpty()) return playerHumanPortraitCells.get(Math.floorMod(portraitIndex, playerHumanPortraitCells.size()));
        BufferedImage legacy = getLegacyPlayerHumanPortrait(portraitIndex);
        if (legacy != null) return legacy;
        return generatedPlayerHumanFallbackPortrait();
    }

    BufferedImage generatedPlayerHumanFallbackPortrait() {
        if (generatedPlayerHumanFallbackPortrait != null) return generatedPlayerHumanFallbackPortrait;
        BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(18, 20, 18, 255));
            g.fillRect(0, 0, 128, 128);
            g.setColor(new Color(70, 62, 42));
            g.fillOval(42, 20, 44, 44);
            g.setColor(new Color(115, 95, 58));
            g.fillRect(32, 70, 64, 44);
            g.setColor(new Color(210, 185, 105));
            g.drawOval(42, 20, 44, 44);
            g.drawRect(32, 70, 64, 44);
        } finally { g.dispose(); }
        generatedPlayerHumanFallbackPortrait = img;
        return img;
    }

    BufferedImage getLegacyPlayerHumanPortrait(int portraitIndex) {
        if (portraitSheets.isEmpty()) return null;
        BufferedImage sheet = null;
        if (portraitSheets.size() > 1) sheet = portraitSheets.get(1);
        else sheet = portraitSheets.get(0);
        if (sheet == null) return null;
        int cellW = 128, cellH = 128;
        int cols = Math.max(1, sheet.getWidth()/cellW);
        int rows = Math.max(1, sheet.getHeight()/cellH);
        int idx = Math.floorMod(portraitIndex, cols*rows);
        int sx = (idx % cols) * cellW, sy = (idx / cols) * cellH;
        if (sx+cellW > sheet.getWidth() || sy+cellH > sheet.getHeight()) return null;
        return sheet.getSubimage(sx, sy, cellW, cellH);
    }
    BufferedImage getNpcPortraitFor(NpcEntity npc) {
        if (npc == null) return getNpcPortrait(0);
        if (npc.nameLockedProfileKey != null && !npc.nameLockedProfileKey.isBlank()) {
            BufferedImage locked = getNameLockedProfilePortrait(npc.nameLockedProfileKey);
            if (locked != null) return locked;
        }
        int[] range = portraitRangeForNpc(npc);
        if (range != null && range[1] > range[0] && !npcPortraitCells.isEmpty()) return npcPortraitCells.get(range[0] + Math.floorMod(npc.portraitIndex, range[1]-range[0]));
        return getNpcPortrait(npc.portraitIndex);
    }

    int[] portraitRangeForNpc(NpcEntity npc) {
        if (npc == null) return null;
        String role = ((npc.creatureKind == null ? "" : npc.creatureKind) + " " + (npc.animalProfileId == null ? "" : npc.animalProfileId) + " " + (npc.role == null ? "" : npc.role) + " " + (npc.name == null ? "" : npc.name)).toLowerCase(Locale.ROOT);
        if (role.contains("servant") || role.contains("chef") || role.contains("butler") || role.contains("household") || role.contains("laundry") || role.contains("retainer") || role.contains("pantry")) return firstPortraitRangeContaining("servants_butlers_and_chefs");
        if (role.contains("medicae") || role.contains("hospital") || role.contains("clinic")) return firstPortraitRangeContaining("medicae", "sisters_hospital");
        if (npc.isAnimalActor()) {
            if (role.contains("farm") || role.contains("hog") || role.contains("goat") || role.contains("fowl") || role.contains("grub")) return firstPortraitRangeContaining("farm_beasts");
            if (role.contains("sump") || role.contains("sewer") || role.contains("swamp") || role.contains("eel") || role.contains("leech") || role.contains("corpse-feeder") || role.contains("fungus")) return firstPortraitRangeContaining("exotic_pets_swamp_creatures", "pets");
            if (role.contains("kennel") || role.contains("mastiff") || role.contains("hound") || role.contains("guard") || role.contains("pet") || role.contains("cat") || role.contains("rat") || role.contains("lizard") || role.contains("moth") || role.contains("glowfish")) return firstPortraitRangeContaining("pets", "exotic_pets_swamp_creatures");
            return firstPortraitRangeContaining("pets", "exotic_pets_swamp_creatures", "farm_beasts");
        }
        if (npc.isChildActor()) return firstPortraitRangeContaining("schola_children", "administratum");
        Faction f = npc.faction == null ? Faction.NONE : npc.faction;
        String fn = f.name().toLowerCase(Locale.ROOT);
        if (f == Faction.ADMINISTRATUM || f == Faction.INN) return firstPortraitRangeContaining("administratum");
        if (f == Faction.ARBITES) return firstPortraitRangeContaining("enforcer_arebites");
        if (f == Faction.IMPERIAL_GUARD) return firstPortraitRangeContaining("pdf_military");
        if (f == Faction.MINISTORUM) return firstPortraitRangeContaining("ecclesiarch");
        if (f == Faction.SORORITAS) return firstPortraitRangeContaining("sisters_hospital", "ecclesiarch");
        if (f == Faction.MECHANICUS || fn.startsWith("mechanicus")) return firstPortraitRangeContaining("mechanicus", "rogue_automata_servitors");
        if (f == Faction.ROGUE_MACHINE) return firstPortraitRangeContaining("rogue_automata_servitors", "mechanicus");
        if (f == Faction.MUTANT) return firstPortraitRangeContaining("mutants");
        if (f == Faction.CULTIST) return firstPortraitRangeContaining("cultists", "genestealer_cult", "heretics");
        if (f == Faction.HERETIC) return firstPortraitRangeContaining("heretics", "cultists");
        if (fn.startsWith("ganger") || f == Faction.BANDIT) return firstPortraitRangeContaining("gangers");
        if (fn.startsWith("noble") || f == Faction.NOBLE) return firstPortraitRangeContaining("nobles");
        if (fn.startsWith("hiver") || f == Faction.HIVER || f == Faction.SCAVENGER || f == Faction.NONE) return firstPortraitRangeContaining("administratum", "gangers");
        return firstPortraitRangeContaining("administratum", "nobles", "gangers");
    }

    int[] firstPortraitRangeContaining(String... keys) {
        if (keys == null) return null;
        for (String key : keys) {
            if (key == null) continue;
            String k = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
            int[] exact = npcPortraitRanges.get(k);
            if (exact != null) return exact;
            // Hard folder authority: do not use substring matching here.
            // A request for pets must not accidentally match every folder with
            // the letters p/e/t, and a missing faction bucket must fail closed
            // rather than searching the whole portrait bin.
        }
        return null;
    }

    BufferedImage getAnyNameLockedProfilePortrait(int portraitIndex) {
        if (nameLockedProfilePortraits == null || nameLockedProfilePortraits.isEmpty()) return null;
        java.util.ArrayList<String> keys = new java.util.ArrayList<>(nameLockedProfilePortraits.keySet());
        java.util.Collections.sort(keys);
        return nameLockedProfilePortraits.get(keys.get(Math.floorMod(portraitIndex, keys.size())));
    }

    BufferedImage getNpcPortrait(int portraitIndex) {
        // Generic NPC fallback is deliberately conservative and celebrity-safe.
        // Name-locked portraits must never leak onto ordinary entities. Only an
        // NPC carrying nameLockedProfileKey, created by the noble-zone seeding rule,
        // may draw from the name_locked partition.
        int[] neutral = firstPortraitRangeContaining("administratum");
        if (neutral != null && neutral[1] > neutral[0] && !npcPortraitCells.isEmpty()) return npcPortraitCells.get(neutral[0] + Math.floorMod(portraitIndex, neutral[1]-neutral[0]));
        return generatedPlayerHumanFallbackPortrait();
    }
        ArrayList<String> loadIntroCrawlLines() {
        ArrayList<String> lines = new ArrayList<>();
        Path p = Paths.get(artRootPath, "source", "new game Intro crawl text", "Text crawl.txt");
        if (artRootPath != null && artRootPath.replace('\\', '/').startsWith("assets/")) {
            p = Paths.get("packages", "client", artRootPath, "source", "new game Intro crawl text", "Text crawl.txt");
            if (!Files.exists(p)) {
                p = Paths.get("client", artRootPath, "source", "new game Intro crawl text", "Text crawl.txt");
            }
            if (!Files.exists(p)) {
                p = Paths.get(artRootPath, "source", "new game Intro crawl text", "Text crawl.txt");
            }
        }
        try {
            if (Files.exists(p)) {
                for (String raw : Files.readAllLines(p)) {
                    String line = raw == null ? "" : raw.replace("\uFEFF", "").trim();
                    if (!line.isEmpty()) lines.add(line);
                }
            }
        } catch (Throwable t) {
            DebugLog.error("INTRO_CRAWL_TEXT", "Failed to read " + p, t);
        }
        if (lines.isEmpty()) {
            lines.add("The hive world resolves beneath the cloud layer: towers, ducts, drowned lights, and the small official fiction that any of this is governed.");
            lines.add("Your name has been entered into a ledger that will outlive you, misfile you, and perhaps tax your remains.");
            lines.add("Somewhere below, a door opens. Somewhere above, nobody important notices.");
        }
        return lines;
    }

    BufferedImage read(String path) {
        try {
            File f = new File(path);
            if (!f.exists() && path != null && path.replace('\\', '/').startsWith("assets/")) {
                File packagedClientOwned = new File("packages/client", path);
                if (packagedClientOwned.exists()) f = packagedClientOwned;
                File clientOwned = new File("client", path);
                if (!f.exists() && clientOwned.exists()) f = clientOwned;
            }
            if (!f.exists() && path != null) {
                File shortRoot = new File(path.replace('\\', '/').replace("assets/art/rebase_0_9_06d", "assets/a/r"));
                if (shortRoot.exists()) f = shortRoot;
            }
            if (!f.exists()) return null;
            return ImageIO.read(f);
        } catch (Throwable t) {
            DebugLog.error("ASSET_LOAD", "Failed to load image " + path, t);
            return null;
        }
    }
    BufferedImage get(String key) { return cache.get(key); }
    boolean hasFrameSlices() { return cache.get("corner_top_left") != null && cache.get("inner_display_center") != null; }
    boolean hasTileArt() { return !tileArt.getRegistry().isEmpty(); }
    BufferedImage getTile(char ch) { return tileArt.getRegistry().getTile(ch); }
    BufferedImage getTile(String semanticKey, char fallback) { return tileArt.getRegistry().getTile(semanticKey, fallback); }

    BufferedImage getSemanticAssetImage(String assetId) {
        if (assetId == null || assetId.isBlank()) return null;
        String id = assetId.trim().toUpperCase(Locale.ROOT);
        BufferedImage cached = semanticAssetImageCache.get(id);
        if (cached != null) return cached;
        ImageIcon icon = AssetManager.getAsset(id);
        BufferedImage img = null;
        if (icon != null && !AssetManager.isMissingAssetIcon(icon)) {
            img = imageIconToBufferedImage(icon);
        }
        if (img == null) {
            img = tileArt.getRegistry().getSemantic(id);
        }
        if (img != null) semanticAssetImageCache.put(id, img);
        return img;
    }

    BufferedImage getItemIcon(String itemName) {
        String assetId = ItemSemanticAssetAuthority.semanticAssetIdForItemName(itemName);
        BufferedImage atlasSemantic = getSemanticAssetImage(assetId);
        if (atlasSemantic != null) return atlasSemantic;
        AssetType expectedType = AssetManager.metadata(assetId).map(m -> m.type()).orElse(AssetType.ITEM_ICON);
        ImageIcon icon = AssetManager.getAsset(assetId, expectedType);
        BufferedImage semantic = imageIconToBufferedImage(icon);
        return semantic != null ? semantic : AssetManager.missingAssetImage(expectedType);
    }

    private static BufferedImage imageIconToBufferedImage(ImageIcon icon) {
        if (icon == null || icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) return null;
        if (icon.getImage() instanceof BufferedImage bi) return bi;
        BufferedImage out = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(icon.getImage(), 0, 0, null);
        g.dispose();
        return out;
    }
    boolean corridorArtUsesNorthSouth(char ch) { return tileArt.getRegistry().corridorArtUsesNorthSouth(ch); }
    BufferedImage getBootSpinnerFrame(long tick) {
        if (!bootFrames.isEmpty()) return bootFrames.get((int)(Math.floorMod(tick, bootFrames.size())));
        return cache.get("mechanical_skull_gear_emblem");
    }
}
