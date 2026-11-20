package mechanist;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.imageio.ImageIO;
import mechanist.assets.AssetManager;
import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetType;

/**
 * Explicit, artist-approved Doom world-body sprites.
 *
 * The manifest connects persistent NPC/species keys to typed semantic asset IDs.
 * Missing or rejected art NEVER triggers a procedural body or a missing-art icon:
 * the caller retains the original portrait fallback until real body art is accepted.
 * All disk reads and PNG validation occur once, never during a hot render frame.
 */
final class WorldActorSpriteResolver {
    static final String MANIFEST = "assets/indexes/world_actor_sprites.tsv";

    record Sprite(BufferedImage image, double widthMeters, double heightMeters) { }
    private record Entry(String assetId, double widthMeters, double heightMeters) { }

    private final Path manifestPath;
    private final Function<String, Optional<AssetMetadata>> metadataLookup;
    private final Function<String, Optional<Path>> pathLookup;
    private final Map<String, Entry> entries = new HashMap<>();
    private final Map<String, Optional<Sprite>> imageCache = new HashMap<>();
    private boolean indexed;
    private int rejectedRows;

    WorldActorSpriteResolver() {
        this(RuntimePathResolver.resolveAssetFile(MANIFEST).toPath(),
                AssetManager::metadata, AssetManager::resolvedAssetPath);
    }

    // Dependency injection makes image acceptance tests independent of real game assets.
    WorldActorSpriteResolver(Path manifestPath,
                             Function<String, Optional<AssetMetadata>> metadataLookup,
                             Function<String, Optional<Path>> pathLookup) {
        this.manifestPath = manifestPath;
        this.metadataLookup = metadataLookup;
        this.pathLookup = pathLookup;
    }

    synchronized Sprite resolve(NpcEntity npc, FirstPersonCamera camera) {
        if (npc == null) return null;
        ensureIndexed();
        if (entries.isEmpty()) return null;
        String key = entityKey(npc);
        String view = viewFor(npc, camera);
        Entry entry = entries.get(key + "|" + view);
        if (entry == null) entry = entries.get(key + "|front");
        if (entry == null && !npc.isAnimalActor() && !"humanoid".equals(key)) {
            entry = entries.get("humanoid|" + view);
            if (entry == null) entry = entries.get("humanoid|front");
        }
        if (entry == null) return null;
        Entry selected = entry;
        String imageKey = selected.assetId + "|" + selected.widthMeters + "|" + selected.heightMeters;
        return imageCache.computeIfAbsent(imageKey, ignored -> load(selected)).orElse(null);
    }

    synchronized void clear() {
        entries.clear();
        imageCache.clear();
        rejectedRows = 0;
        indexed = false;
    }

    synchronized int rejectedRows() { ensureIndexed(); return rejectedRows; }
    synchronized int registeredViews() { ensureIndexed(); return entries.size(); }

    static String entityKey(NpcEntity npc) {
        if (npc == null) return "humanoid";
        if (npc.isAnimalActor()) {
            String id = normalizeKey(npc.animalProfileId);
            return id.isEmpty() ? normalizeKey(npc.creatureKind) : id;
        }
        String role = (npc.role == null ? "" : npc.role).toLowerCase(Locale.ROOT);
        if (containsAny(role, "warden", "guard", "security", "soldier", "militia", "enforcer"))
            return "humanoid-guard";
        if (containsAny(role, "worker", "labor", "miner", "mechanic", "engineer", "fabricator"))
            return "humanoid-worker";
        if (containsAny(role, "medicae", "clinic", "surgeon", "nurse", "doctor"))
            return "humanoid-medical";
        if (containsAny(role, "priest", "cultist", "seer", "cleric"))
            return "humanoid-robed";
        return "humanoid";
    }

    static String viewFor(NpcEntity npc, FirstPersonCamera camera) {
        if (npc == null || camera == null || (npc.facingDx == 0 && npc.facingDy == 0))
            return "front";
        double dx = camera.x() - npc.x - .5;
        double dz = camera.z() - npc.y - .5;
        if (dx * dx + dz * dz < .01) return "front";
        double forward = npc.facingDx * dx + npc.facingDy * dz;
        double side = npc.facingDx * dz - npc.facingDy * dx;
        if (Math.abs(forward) >= Math.abs(side)) return forward >= 0 ? "front" : "back";
        return side > 0 ? "right" : "left";
    }

    private void ensureIndexed() {
        if (indexed) return;
        indexed = true;
        if (manifestPath == null || !Files.isRegularFile(manifestPath)) return;
        try (BufferedReader reader = Files.newBufferedReader(manifestPath, StandardCharsets.UTF_8)) {
            for (String line; (line = reader.readLine()) != null;) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("entity_key\t")) continue;
                String[] parts = line.split("\t", -1);
                if (parts.length != 5) { rejectedRows++; continue; }
                String key = normalizeKey(parts[0]);
                String view = parts[1].trim().toLowerCase(Locale.ROOT);
                String assetId = parts[2].trim().toUpperCase(Locale.ROOT);
                double width;
                double height;
                try {
                    width = Double.parseDouble(parts[3].trim());
                    height = Double.parseDouble(parts[4].trim());
                } catch (NumberFormatException badScale) {
                    rejectedRows++;
                    continue;
                }
                if (key.isEmpty() || !key.equals(parts[0].trim()) || !validView(view)
                        || assetId.length() != 8 || !assetId.matches("[A-Z0-9]{3,4}-[A-Z0-9]{3,4}")
                        || !Double.isFinite(width) || !Double.isFinite(height)
                        || width < .15 || width > 4.0 || height < .15 || height > 4.0) {
                    rejectedRows++;
                    continue;
                }
                if (entries.putIfAbsent(key + "|" + view, new Entry(assetId, width, height)) != null)
                    rejectedRows++;
            }
        } catch (IOException unreadable) {
            entries.clear();
            rejectedRows++;
        }
    }

    private Optional<Sprite> load(Entry entry) {
        try {
            Optional<AssetMetadata> candidate = metadataLookup.apply(entry.assetId);
            // Never silently reuse a portrait, item icon, or the semantic missing icon.
            if (candidate.isEmpty() || candidate.get().type() != AssetType.ENTITY_SPRITE)
                return Optional.empty();
            Optional<Path> path = pathLookup.apply(entry.assetId);
            if (path.isEmpty() || !path.get().getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")
                    || !Files.isRegularFile(path.get())) return Optional.empty();
            BufferedImage image = ImageIO.read(path.get().toFile());
            if (!validBodyImage(image)) return Optional.empty();
            return Optional.of(new Sprite(image, entry.widthMeters, entry.heightMeters));
        } catch (IOException | RuntimeException invalidAsset) {
            return Optional.empty();
        }
    }

    /** Machine-verifiable minimum, not a substitute for a human art review. */
    static boolean validBodyImage(BufferedImage image) {
        if (image == null || !image.getColorModel().hasAlpha()) return false;
        int w = image.getWidth(), h = image.getHeight();
        if (w < 24 || h < 24 || w > 2048 || h > 2048) return false;
        if ((image.getRGB(0, 0) >>> 24) != 0 || (image.getRGB(w-1, 0) >>> 24) != 0
                || (image.getRGB(0, h-1) >>> 24) != 0
                || (image.getRGB(w-1, h-1) >>> 24) != 0) return false;
        long visible = 0L, total = (long) w * h;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++)
            if ((image.getRGB(x, y) >>> 24) != 0) visible++;
        return visible >= Math.max(16, total / 100) && visible <= total * 9 / 10;
    }

    private static boolean containsAny(String role, String... terms) {
        for (String term : terms) if (role.contains(term)) return true;
        return false;
    }
    private static boolean validView(String view) {
        return view.equals("front") || view.equals("back")
                || view.equals("left") || view.equals("right");
    }
    private static String normalizeKey(String raw) {
        if (raw == null) return "";
        return raw.toLowerCase(Locale.ROOT).trim().replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
