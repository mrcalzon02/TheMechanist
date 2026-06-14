package mechanist;

import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetRegistry;
import mechanist.assets.AssetType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Semantic bridge from renderer intent to indexed graphical asset metadata.
 *
 * This is the repair-path layer that sits between live renderers and the
 * existing semantic asset indexes. Callers ask for meaning such as sewer floor,
 * generic wall, streetlight fixture, closed door, or open door. This resolver
 * searches the semantic registry and refuses known-bad cross-theme fallbacks.
 */
final class SemanticRenderAssetResolver {
    static final String VERSION = "semantic-render-asset-resolver-0.1";

    enum RenderIntent {
        SEWER_FLOOR,
        SEWER_WALL,
        GENERIC_FLOOR,
        GENERIC_WALL,
        INDUSTRIAL_FLOOR,
        HABITATION_FLOOR,
        MARKET_FLOOR,
        STREETLIGHT_FIXTURE,
        DOOR_CLOSED,
        DOOR_OPEN
    }

    static final class Resolution {
        final RenderIntent intent;
        final AssetMetadata asset;
        final String reason;

        private Resolution(RenderIntent intent, AssetMetadata asset, String reason) {
            this.intent = intent;
            this.asset = asset;
            this.reason = reason == null ? "" : reason;
        }

        static Resolution found(RenderIntent intent, AssetMetadata asset, String reason) {
            return new Resolution(intent, asset, reason);
        }

        static Resolution missing(RenderIntent intent, String reason) {
            return new Resolution(intent, null, reason);
        }

        boolean found() {
            return asset != null;
        }

        String assetIdOrMissing() {
            return asset == null ? "<missing>" : asset.id();
        }
    }

    private SemanticRenderAssetResolver() {}

    static Resolution resolve(AssetRegistry registry, RenderIntent intent) {
        AssetRegistry safe = registry == null ? AssetRegistry.empty() : registry;
        List<AssetMetadata> candidates = candidatesFor(safe, intent);
        if (candidates.isEmpty()) {
            return Resolution.missing(intent, "No semantic asset matched " + intent + ". Renderer must use explicit missing-art fallback, not unrelated art.");
        }
        return Resolution.found(intent, candidates.get(0), "Matched indexed semantic asset for " + intent + ".");
    }

    static Optional<AssetMetadata> optional(AssetRegistry registry, RenderIntent intent) {
        Resolution resolution = resolve(registry, intent);
        return resolution.found() ? Optional.of(resolution.asset) : Optional.empty();
    }

    static boolean canUse(AssetMetadata asset, RenderIntent intent) {
        if (asset == null || intent == null) return false;
        String haystack = haystack(asset);
        return switch (intent) {
            case SEWER_FLOOR -> asset.type() == AssetType.FLOOR_TILE && contains(haystack, "sewer", "sump", "drain", "utility tunnel") && !contains(haystack, "generic", "main floor");
            case SEWER_WALL -> asset.type() == AssetType.WALL_TILE && contains(haystack, "sewer", "sump", "drain", "utility tunnel") && !contains(haystack, "generic", "main wall");
            case GENERIC_FLOOR -> asset.type() == AssetType.FLOOR_TILE && contains(haystack, "generic", "plain", "main floor", "default floor") && !contains(haystack, "sewer", "sump");
            case GENERIC_WALL -> asset.type() == AssetType.WALL_TILE && contains(haystack, "generic", "plain", "main wall", "default wall") && !contains(haystack, "sewer", "sump");
            case INDUSTRIAL_FLOOR -> asset.type() == AssetType.FLOOR_TILE && contains(haystack, "industrial", "factory", "machine shop", "workshop") && !contains(haystack, "sewer");
            case HABITATION_FLOOR -> asset.type() == AssetType.FLOOR_TILE && contains(haystack, "habitation", "hab", "apartment", "residential") && !contains(haystack, "sewer");
            case MARKET_FLOOR -> asset.type() == AssetType.FLOOR_TILE && contains(haystack, "market", "bazaar", "shop", "commercial") && !contains(haystack, "sewer");
            case STREETLIGHT_FIXTURE -> (asset.type() == AssetType.FIXTURE || asset.type() == AssetType.OBJECT) && contains(haystack, "streetlight", "street light", "lamp post", "street lamp") && !contains(haystack, "system inventory", "item icon", "ui icon");
            case DOOR_CLOSED -> (asset.type() == AssetType.FIXTURE || asset.type() == AssetType.WALL_TILE || asset.type() == AssetType.CORRIDOR_TILE) && contains(haystack, "door") && contains(haystack, "closed", "shut") && !contains(haystack, "open");
            case DOOR_OPEN -> (asset.type() == AssetType.FIXTURE || asset.type() == AssetType.FLOOR_TILE || asset.type() == AssetType.CORRIDOR_TILE) && contains(haystack, "door") && contains(haystack, "open", "opened") && !contains(haystack, "closed", "shut");
        };
    }

    static List<String> auditLines(AssetRegistry registry) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Semantic render asset resolver " + VERSION + ".");
        for (RenderIntent intent : RenderIntent.values()) {
            Resolution r = resolve(registry, intent);
            lines.add(intent + ": " + r.assetIdOrMissing() + " - " + r.reason);
        }
        return lines;
    }

    private static List<AssetMetadata> candidatesFor(AssetRegistry registry, RenderIntent intent) {
        return registry.all().stream()
                .filter(asset -> canUse(asset, intent))
                .sorted(Comparator.comparing((AssetMetadata asset) -> priority(asset, intent)).reversed()
                        .thenComparing(AssetMetadata::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(AssetMetadata::id))
                .toList();
    }

    private static int priority(AssetMetadata asset, RenderIntent intent) {
        String h = haystack(asset);
        int score = 0;
        if (contains(h, normalize(intent.name()))) score += 8;
        if (asset.name() != null && contains(asset.name(), normalize(intent.name()))) score += 5;
        if (asset.semanticDescription() != null && contains(asset.semanticDescription(), normalize(intent.name()))) score += 4;
        if (asset.pathOrUri() != null && contains(asset.pathOrUri(), normalize(intent.name()))) score += 2;
        if (intent == RenderIntent.STREETLIGHT_FIXTURE && contains(h, "streetlight")) score += 10;
        if ((intent == RenderIntent.DOOR_OPEN || intent == RenderIntent.DOOR_CLOSED) && contains(h, "variant")) score += 3;
        return score;
    }

    private static String haystack(AssetMetadata asset) {
        return normalize(asset.id() + " " + asset.name() + " " + asset.pathOrUri() + " " + asset.type().displayName() + " " + asset.semanticDescription());
    }

    private static boolean contains(String text, String... needles) {
        String h = normalize(text);
        for (String needle : needles) {
            String n = normalize(needle);
            if (!n.isBlank() && h.contains(n)) return true;
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('-', ' ').replace('_', ' ').trim();
    }
}
