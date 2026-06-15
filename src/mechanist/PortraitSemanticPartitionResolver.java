package mechanist;

import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetRegistry;
import mechanist.assets.AssetType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Exact-partition semantic portrait selection for NPC and special identity families.
 *
 * <p>This resolver deliberately does not infer a family from partial text. Callers
 * must provide the canonical partition key and explicitly authorize restricted or
 * name-locked families. Missing or unauthorized partitions fail closed so an NPC
 * can continue through the established legacy portrait fallback without leaking
 * unrelated, animal, restricted, or celebrity-locked artwork.</p>
 */
final class PortraitSemanticPartitionResolver {
    static final String VERSION = "portrait-semantic-partition-resolver-0.2";

    private PortraitSemanticPartitionResolver() {}

    static Optional<String> assetId(
            Path projectRoot,
            AssetRegistry registry,
            String partitionKey,
            String stableIdentity,
            boolean allowRestricted,
            boolean allowNameLocked
    ) {
        List<PortraitSemanticAssetAuthority.PartitionRecord> pool = orderedPool(
                projectRoot,
                registry,
                partitionKey,
                allowRestricted,
                allowNameLocked);
        if (pool.isEmpty() || stableIdentity == null || stableIdentity.isBlank()) {
            return Optional.empty();
        }
        int index = Math.floorMod(stableIdentity.hashCode(), pool.size());
        return Optional.of(pool.get(index).assetId());
    }

    static List<PortraitSemanticAssetAuthority.PartitionRecord> orderedPool(
            Path projectRoot,
            AssetRegistry registry,
            String partitionKey,
            boolean allowRestricted,
            boolean allowNameLocked
    ) {
        String canonicalKey = canonicalPartitionKey(partitionKey);
        if (canonicalKey.isEmpty()) return List.of();

        AssetRegistry safeRegistry = registry == null ? AssetRegistry.empty() : registry;
        Path safeRoot = projectRoot == null ? Path.of(".") : projectRoot;

        try {
            return PortraitSemanticAssetAuthority.loadDefault(safeRoot).stream()
                    .filter(record -> canonicalKey.equals(canonicalPartitionKey(record.partitionKey())))
                    .filter(record -> allowRestricted || !record.nonhumanOrRestricted())
                    .filter(record -> allowNameLocked || !record.nameLockedOnly())
                    .filter(record -> validRegistryPortrait(safeRegistry, record.assetId()))
                    .sorted(Comparator
                            .comparing((PortraitSemanticAssetAuthority.PartitionRecord record) ->
                                    normalizedPath(record.registryPath()))
                            .thenComparing(PortraitSemanticAssetAuthority.PartitionRecord::assetId))
                    .toList();
        } catch (IOException | RuntimeException ex) {
            return List.of();
        }
    }

    static String canonicalPartitionKey(String value) {
        if (value == null) return "";
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private static boolean validRegistryPortrait(AssetRegistry registry, String assetId) {
        if (assetId == null || assetId.isBlank()) return false;
        Optional<AssetMetadata> metadata = registry.find(assetId);
        return metadata.isPresent() && metadata.get().type() == AssetType.PORTRAIT;
    }

    private static String normalizedPath(String value) {
        return value == null ? "" : value.replace('\\', '/').toLowerCase(Locale.ROOT);
    }
}
