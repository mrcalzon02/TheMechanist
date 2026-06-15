package mechanist;

import mechanist.assets.AssetManager;
import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetType;

import java.nio.file.Path;
import java.util.List;

/** Focused verification for active-registry portrait partition selection. */
public final class PortraitSemanticRuntimeSmoke {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(".").toAbsolutePath().normalize();
        var registry = AssetManager.registry();
        if (registry.size() <= 0) {
            throw new AssertionError("active semantic asset registry was empty");
        }

        String source = PortraitSemanticAssetAuthority.partitionSource(root);
        if (!"active-registry synthesis".equals(source)) {
            throw new AssertionError("expected registry-synthesized portrait partitions, got " + source);
        }

        var records = PortraitSemanticAssetAuthority.loadDefault(root);
        if (records.isEmpty()) {
            throw new AssertionError("synthesized portrait partition records were empty");
        }

        var audit = PortraitSemanticAssetAuthority.audit(root, registry);
        if (!audit.passed()) {
            throw new AssertionError("portrait partition audit failed: " + audit.errors());
        }

        assertPartition(records, "administratum", false, false);
        assertPartition(records, "name_locked_profile", false, true);
        assertPartition(records, "rogue_automata_servitors", true, false);
        assertPartition(records, "pets", true, false);
        assertPartition(records, "flesh_cult", true, false);
        assertPartition(records, "undying_lords", true, false);

        var playerPool = PortraitSemanticAssetAuthority.activePlayerPool(root, registry);
        var npcPool = PortraitSemanticAssetAuthority.activeNpcPool(root, registry);
        if (playerPool.isEmpty()) throw new AssertionError("active player portrait pool was empty");
        if (npcPool.isEmpty()) throw new AssertionError("active NPC portrait pool was empty");

        assertOrdinaryPool("player", playerPool);
        assertOrdinaryPool("NPC", npcPool);
        for (var record : playerPool) {
            if (!record.partitionKey().equals("administratum")) {
                throw new AssertionError("non-administratum portrait leaked into player pool: "
                        + record.compactLine());
            }
            String path = record.registryPath().toLowerCase(java.util.Locale.ROOT);
            if (!path.contains("humans8x8")) {
                throw new AssertionError("player pool contains non-Humans8x8 asset: "
                        + record.compactLine());
            }
        }

        String playerA = PortraitSemanticAssetAuthority.runtimePlayerAssetId(
                root, registry, "Humans8x8", 7).orElseThrow(
                () -> new AssertionError("player portrait selection returned empty"));
        String playerB = PortraitSemanticAssetAuthority.runtimePlayerAssetId(
                root, registry, "Humans8x8", 7).orElseThrow();
        if (!playerA.equals(playerB)) {
            throw new AssertionError("player portrait selection was not deterministic");
        }

        String npcIdentity = "persistent-npc:hiver-worker:unit-4421";
        String npcA = PortraitSemanticAssetAuthority.runtimeNpcAssetId(
                root, registry, npcIdentity).orElseThrow(
                () -> new AssertionError("NPC portrait selection returned empty"));
        String npcB = PortraitSemanticAssetAuthority.runtimeNpcAssetId(
                root, registry, npcIdentity).orElseThrow();
        if (!npcA.equals(npcB)) {
            throw new AssertionError("NPC portrait selection was not deterministic");
        }

        if (PortraitSemanticAssetAuthority.runtimeNpcAssetId(root, registry, "   ").isPresent()) {
            throw new AssertionError("blank NPC identity unexpectedly selected a portrait");
        }

        for (String id : List.of(playerA, npcA)) {
            AssetMetadata metadata = registry.find(id).orElseThrow(
                    () -> new AssertionError("selected portrait ID was absent from registry: " + id));
            if (metadata.type() != AssetType.PORTRAIT) {
                throw new AssertionError("selected runtime portrait was not PORTRAIT typed: "
                        + id + " / " + metadata.type());
            }
        }

        System.out.println("PortraitSemanticRuntimeSmoke PASS "
                + audit.summaryLine()
                + " source=" + source
                + " synthesizedRecords=" + records.size()
                + " activePlayer=" + playerPool.size()
                + " activeNpc=" + npcPool.size()
                + " player=" + playerA
                + " npc=" + npcA
                + " authority=" + PortraitSemanticAssetAuthority.VERSION);
    }

    private static void assertPartition(
            List<PortraitSemanticAssetAuthority.PartitionRecord> records,
            String key,
            boolean restricted,
            boolean nameLocked
    ) {
        var matching = records.stream()
                .filter(record -> record.partitionKey().equals(key))
                .toList();
        if (matching.isEmpty()) {
            throw new AssertionError("missing synthesized portrait partition " + key);
        }
        for (var record : matching) {
            if (record.nonhumanOrRestricted() != restricted) {
                throw new AssertionError(key + " restricted mismatch: " + record.compactLine());
            }
            if (record.nameLockedOnly() != nameLocked) {
                throw new AssertionError(key + " name-lock mismatch: " + record.compactLine());
            }
        }
    }

    private static void assertOrdinaryPool(
            String label,
            List<PortraitSemanticAssetAuthority.PartitionRecord> records
    ) {
        for (var record : records) {
            if (record.nameLockedOnly()) {
                throw new AssertionError(label + " pool leaked name-locked portrait "
                        + record.assetId());
            }
            if (record.nonhumanOrRestricted()) {
                throw new AssertionError(label + " pool leaked restricted portrait "
                        + record.assetId());
            }
        }
    }

    private PortraitSemanticRuntimeSmoke() {}
}
