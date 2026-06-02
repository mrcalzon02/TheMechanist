package mechanist.assets;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central semantic-image cache.
 *
 * Gameplay and UI code should request images by semantic ID here instead of
 * loading files by path. Stage 9 hardens the missing-art behavior: callers that
 * know the expected semantic family can request a typed missing icon so failures
 * are obvious and cannot collapse back into a semantically wrong legacy sprite.
 */
public final class AssetManager {
    private static final Map<String, ImageIcon> CACHE = new ConcurrentHashMap<>();
    private static final ImageIcon MISSING_ICON = createMissingIcon(AssetType.UNKNOWN);
    private static final Map<AssetType, ImageIcon> TYPED_MISSING_ICONS = createTypedMissingIcons();
    private static final Set<ImageIcon> ALL_MISSING_ICONS = createMissingIconSet();

    private static volatile AssetRegistry registry = AssetRegistry.empty();
    private static volatile GeneratedAssetRuntime generatedRuntime;
    private static volatile boolean attemptedDefaultLoad;

    private AssetManager() {
    }

    public static void installRegistry(AssetRegistry newRegistry) {
        registry = newRegistry == null ? AssetRegistry.empty() : newRegistry;
        generatedRuntime = GeneratedAssetRuntime.loadDefault(registry.projectRoot());
        CACHE.clear();
        attemptedDefaultLoad = true;
    }

    public static AssetRegistry registry() {
        ensureDefaultRegistryLoaded();
        return registry;
    }

    public static Optional<AssetMetadata> metadata(String assetId) {
        ensureDefaultRegistryLoaded();
        return registry.find(assetId);
    }

    public static GeneratedAssetRuntime generatedAssetRuntime() {
        ensureDefaultRegistryLoaded();
        return runtimeForCurrentRegistry();
    }

    public static Optional<Path> resolvedAssetPath(String assetId) {
        ensureDefaultRegistryLoaded();
        return registry.find(assetId).map(AssetManager::resolveMetadataPath);
    }

    public static Path resolveMetadataPath(AssetMetadata metadata) {
        Objects.requireNonNull(metadata, "metadata cannot be null");
        String ref = metadata.pathOrUri();
        if (ref.startsWith("classpath:") || looksLikeUri(ref)) {
            return registry.resolvePath(metadata);
        }
        return runtimeForCurrentRegistry().resolvePath(ref);
    }

    /**
     * Load an asset by exact semantic ID. Invalid IDs return the general missing
     * icon for compatibility; new hardened callers should prefer
     * {@link #getAsset(String, AssetType)} when they know the expected family.
     */
    public static ImageIcon getAsset(String assetId) {
        return getAsset(assetId, AssetType.UNKNOWN);
    }

    /**
     * Load an asset by exact semantic ID with a typed missing-art fallback.
     */
    public static ImageIcon getAsset(String assetId, AssetType expectedType) {
        ensureDefaultRegistryLoaded();
        AssetType fallbackType = expectedType == null ? AssetType.UNKNOWN : expectedType;
        if (assetId == null || assetId.isBlank()) {
            return missingAssetIcon(fallbackType);
        }
        String normalized = assetId.trim().toUpperCase(Locale.ROOT);
        ImageIcon icon = CACHE.computeIfAbsent(normalized, AssetManager::loadIconById);
        if (isMissingAssetIcon(icon) && fallbackType != AssetType.UNKNOWN) {
            return missingAssetIcon(fallbackType);
        }
        return icon;
    }

    public static ImageIcon missingAssetIcon() {
        return MISSING_ICON;
    }

    public static ImageIcon missingAssetIcon(AssetType type) {
        if (type == null) return MISSING_ICON;
        return TYPED_MISSING_ICONS.getOrDefault(type, MISSING_ICON);
    }

    public static BufferedImage missingAssetImage(AssetType type) {
        return imageIconToBufferedImage(missingAssetIcon(type));
    }

    public static boolean isMissingAssetIcon(ImageIcon icon) {
        return icon == MISSING_ICON || ALL_MISSING_ICONS.contains(icon);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    public static void reloadGeneratedAssetRuntime() {
        ensureDefaultRegistryLoaded();
        synchronized (AssetManager.class) {
            generatedRuntime = GeneratedAssetRuntime.loadDefault(registry.projectRoot());
            CACHE.clear();
        }
    }

    public static int cacheSize() {
        return CACHE.size();
    }

    private static ImageIcon loadIconById(String assetId) {
        Optional<AssetMetadata> found = registry.find(assetId);
        if (found.isEmpty()) {
            return MISSING_ICON;
        }
        AssetMetadata metadata = found.get();
        try {
            BufferedImage image = loadImage(metadata);
            return image == null ? missingAssetIcon(metadata.type()) : new ImageIcon(image);
        } catch (RuntimeException | IOException ex) {
            return missingAssetIcon(metadata.type());
        }
    }

    private static BufferedImage loadImage(AssetMetadata metadata) throws IOException {
        String ref = metadata.pathOrUri();
        if (ref.startsWith("classpath:")) {
            String resource = ref.substring("classpath:".length());
            if (!resource.startsWith("/")) {
                resource = "/" + resource;
            }
            try (InputStream in = AssetManager.class.getResourceAsStream(resource)) {
                return in == null ? null : ImageIO.read(in);
            }
        }
        if (looksLikeUri(ref)) {
            URI uri = URI.create(ref);
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                return ImageIO.read(Paths.get(uri).toFile());
            }
            try (InputStream in = uri.toURL().openStream()) {
                return ImageIO.read(in);
            }
        }
        Path path = resolveMetadataPath(metadata);
        if (!Files.exists(path)) {
            return null;
        }
        return ImageIO.read(path.toFile());
    }

    private static void ensureDefaultRegistryLoaded() {
        if (attemptedDefaultLoad || registry.size() > 0) {
            return;
        }
        synchronized (AssetManager.class) {
            if (attemptedDefaultLoad || registry.size() > 0) {
                return;
            }
            attemptedDefaultLoad = true;
            Path root = Paths.get(System.getProperty("mechanist.assetRoot", ".")).toAbsolutePath().normalize();
            try {
                registry = AssetRegistry.loadDefault(root);
            } catch (IOException ignored) {
                registry = AssetRegistry.empty();
            }
            generatedRuntime = GeneratedAssetRuntime.loadDefault(registry.projectRoot());
        }
    }

    private static GeneratedAssetRuntime runtimeForCurrentRegistry() {
        GeneratedAssetRuntime current = generatedRuntime;
        if (current != null) return current;
        synchronized (AssetManager.class) {
            if (generatedRuntime == null) {
                generatedRuntime = GeneratedAssetRuntime.loadDefault(registry.projectRoot());
            }
            return generatedRuntime;
        }
    }

    private static boolean looksLikeUri(String value) {
        int colon = value.indexOf(':');
        if (colon <= 1) {
            return false;
        }
        for (int i = 0; i < colon; i++) {
            char c = value.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '+' && c != '-' && c != '.') {
                return false;
            }
        }
        return true;
    }

    private static Map<AssetType, ImageIcon> createTypedMissingIcons() {
        EnumMap<AssetType, ImageIcon> out = new EnumMap<>(AssetType.class);
        for (AssetType type : AssetType.values()) {
            out.put(type, createMissingIcon(type));
        }
        out.put(AssetType.UNKNOWN, MISSING_ICON);
        return Map.copyOf(out);
    }

    private static Set<ImageIcon> createMissingIconSet() {
        HashSet<ImageIcon> out = new HashSet<>();
        out.add(MISSING_ICON);
        out.addAll(TYPED_MISSING_ICONS.values());
        return Set.copyOf(out);
    }

    private static ImageIcon createMissingIcon(AssetType type) {
        int size = 64;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        AssetType t = type == null ? AssetType.UNKNOWN : type;
        Color fill = fillFor(t);
        Color stroke = strokeFor(t);
        g.setColor(fill);
        g.fillRect(0, 0, size, size);
        g.setColor(stroke);
        g.setStroke(new BasicStroke(3f));
        g.drawRect(2, 2, size - 5, size - 5);
        g.drawLine(10, 10, size - 11, size - 11);
        g.drawLine(size - 11, 10, 10, size - 11);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 9));
        g.drawString(labelFor(t), 9, 35);
        g.dispose();
        return new ImageIcon(image);
    }

    private static Color fillFor(AssetType type) {
        return switch (type) {
            case WALL_TILE, FLOOR_TILE, ROAD_TILE, SIDEWALK_TILE, CORRIDOR_TILE -> new Color(28, 34, 42, 238);
            case OBJECT, FIXTURE -> new Color(34, 27, 18, 238);
            case MACHINE -> new Color(23, 37, 30, 238);
            case ITEM_ICON, WEAPON_ICON, ARMOR_ICON -> new Color(42, 25, 24, 238);
            case PORTRAIT -> new Color(28, 28, 48, 238);
            case CORPSE_DECAY -> new Color(45, 16, 18, 238);
            case UI_ICON, INTERNAL -> new Color(28, 28, 28, 238);
            case UNKNOWN -> new Color(48, 10, 54, 235);
        };
    }

    private static Color strokeFor(AssetType type) {
        return switch (type) {
            case WALL_TILE, FLOOR_TILE, ROAD_TILE, SIDEWALK_TILE, CORRIDOR_TILE -> new Color(120, 170, 225);
            case OBJECT, FIXTURE -> new Color(230, 185, 85);
            case MACHINE -> new Color(110, 225, 160);
            case ITEM_ICON, WEAPON_ICON, ARMOR_ICON -> new Color(235, 120, 90);
            case PORTRAIT -> new Color(175, 150, 245);
            case CORPSE_DECAY -> new Color(220, 85, 85);
            case UI_ICON, INTERNAL -> new Color(200, 200, 200);
            case UNKNOWN -> new Color(255, 0, 255);
        };
    }

    private static String labelFor(AssetType type) {
        return switch (type) {
            case WALL_TILE -> "MISSWAL";
            case FLOOR_TILE -> "MISSFLR";
            case ROAD_TILE -> "MISSROD";
            case SIDEWALK_TILE -> "MISSSWK";
            case CORRIDOR_TILE -> "MISSCOR";
            case OBJECT -> "MISSOBJ";
            case FIXTURE -> "MISSFIX";
            case MACHINE -> "MISSMAC";
            case ITEM_ICON -> "MISSITM";
            case WEAPON_ICON -> "MISSWPN";
            case ARMOR_ICON -> "MISSARM";
            case PORTRAIT -> "MISSPOR";
            case CORPSE_DECAY -> "MISSCORP";
            case UI_ICON -> "MISSUI";
            case INTERNAL -> "MISSINT";
            case UNKNOWN -> "MISS";
        };
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
}
