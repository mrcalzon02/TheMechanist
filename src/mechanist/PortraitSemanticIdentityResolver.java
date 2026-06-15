package mechanist;

import mechanist.assets.AssetRegistry;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class PortraitSemanticIdentityResolver {
    static final String VERSION = "portrait-semantic-identity-0.1";

    private PortraitSemanticIdentityResolver() {}

    static Optional<String> playerAssetId(
            Path projectRoot,
            AssetRegistry registry,
            int portraitIndex
    ) {
        List<PortraitSemanticAssetAuthority.PartitionRecord> pool =
                orderedPlayerPool(projectRoot, registry);
        if (pool.isEmpty()) return Optional.empty();
        return Optional.of(pool.get(Math.floorMod(portraitIndex, pool.size())).assetId());
    }

    static List<PortraitSemanticAssetAuthority.PartitionRecord> orderedPlayerPool(
            Path projectRoot,
            AssetRegistry registry
    ) {
        return PortraitSemanticAssetAuthority.activePlayerPool(projectRoot, registry).stream()
                .sorted(Comparator
                        .comparing((PortraitSemanticAssetAuthority.PartitionRecord record) ->
                                normalizedPath(record.registryPath()))
                        .thenComparing(PortraitSemanticAssetAuthority.PartitionRecord::assetId))
                .toList();
    }

    private static String normalizedPath(String value) {
        return value == null ? "" : value.replace('\\', '/').toLowerCase(Locale.ROOT);
    }
}
