package mechanist;

import mechanist.assets.AssetManager;
import mechanist.assets.AssetType;

import java.nio.file.Path;

/** Focused policy verification for exact, fail-closed NPC portrait partitions. */
public final class PortraitSemanticPartitionResolverSmoke {
    public static void main(String[] args) {
        Path root = Path.of(".").toAbsolutePath().normalize();
        var registry = AssetManager.registry();
        if (registry.size() <= 0) {
            throw new AssertionError("active semantic asset registry was empty");
        }

        var ordinary = PortraitSemanticPartitionResolver.orderedPool(
                root, registry, "administratum", false, false);
        if (ordinary.isEmpty()) {
            throw new AssertionError("ordinary administratum semantic portrait pool was empty");
        }
        for (var record : ordinary) {
            if (!"administratum".equals(record.partitionKey())) {
                throw new AssertionError("ordinary pool crossed partition boundary: " + record.compactLine());
            }
            if (record.nonhumanOrRestricted() || record.nameLockedOnly()) {
                throw new AssertionError("ordinary pool leaked protected portrait: " + record.compactLine());
            }
            var metadata = registry.find(record.assetId()).orElseThrow(
                    () -> new AssertionError("ordinary pool asset absent from registry: " + record.assetId()));
            if (metadata.type() != AssetType.PORTRAIT) {
                throw new AssertionError("ordinary pool asset was not PORTRAIT typed: " + record.assetId());
            }
        }

        String identity = "persistent-npc:hiver-worker:unit-4421";
        String ordinaryA = PortraitSemanticPartitionResolver.assetId(
                root, registry, "administratum", identity, false, false).orElseThrow();
        String ordinaryB = PortraitSemanticPartitionResolver.assetId(
                root, registry, "administratum", identity, false, false).orElseThrow();
        if (!ordinaryA.equals(ordinaryB)) {
            throw new AssertionError("ordinary partition selection was not deterministic");
        }

        if (PortraitSemanticPartitionResolver.assetId(
                root, registry, "administratum", "   ", false, false).isPresent()) {
            throw new AssertionError("blank identity unexpectedly selected a portrait");
        }
        if (PortraitSemanticPartitionResolver.assetId(
                root, registry, "pet", identity, true, false).isPresent()) {
            throw new AssertionError("partial partition key unexpectedly matched pets");
        }

        if (!PortraitSemanticPartitionResolver.orderedPool(
                root, registry, "pets", false, false).isEmpty()) {
            throw new AssertionError("restricted pets pool was available without authorization");
        }
        var pets = PortraitSemanticPartitionResolver.orderedPool(
                root, registry, "pets", true, false);
        if (pets.isEmpty()) {
            throw new AssertionError("authorized pets semantic portrait pool was empty");
        }
        for (var record : pets) {
            if (!record.nonhumanOrRestricted() || record.nameLockedOnly()) {
                throw new AssertionError("pets authorization selected the wrong policy family: "
                        + record.compactLine());
            }
        }

        if (!PortraitSemanticPartitionResolver.orderedPool(
                root, registry, "name_locked_profile", false, false).isEmpty()) {
            throw new AssertionError("name-locked pool was available without authorization");
        }
        var nameLocked = PortraitSemanticPartitionResolver.orderedPool(
                root, registry, "name_locked_profile", false, true);
        if (nameLocked.isEmpty()) {
            throw new AssertionError("authorized name-locked semantic portrait pool was empty");
        }
        for (var record : nameLocked) {
            if (!record.nameLockedOnly()) {
                throw new AssertionError("name-locked authorization selected an ordinary portrait: "
                        + record.compactLine());
            }
        }

        System.out.println("PortraitSemanticPartitionResolverSmoke PASS"
                + " ordinary=" + ordinary.size()
                + " pets=" + pets.size()
                + " nameLocked=" + nameLocked.size()
                + " selected=" + ordinaryA
                + " resolver=" + PortraitSemanticPartitionResolver.VERSION);
    }

    private PortraitSemanticPartitionResolverSmoke() {}
}
