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

        int persistentPortraitIndex = 7;
        var orderedPlayerPool = PortraitSemanticIdentityResolver.orderedPlayerPool(root, registry);
        if (orderedPlayerPool.size() != playerPool.size()) {
            throw new AssertionError("identity resolver changed player-pool membership");
        }
        String expectedPlayerId = orderedPlayerPool.get(
                Math.floorMod(persistentPortraitIndex, orderedPlayerPool.size())).assetId();
        String playerA = PortraitSemanticIdentityResolver.playerAssetId(
                root, registry, persistentPortraitIndex).orElseThrow(
                () -> new AssertionError("player portrait selection returned empty"));
        String playerB = PortraitSemanticIdentityResolver.playerAssetId(
                root, registry, persistentPortraitIndex).orElseThrow();
        if (!playerA.equals(playerB)) {
            throw new AssertionError("player portrait selection was not deterministic");
        }
        if (!playerA.equals(expectedPlayerId)) {
            throw new AssertionError("semantic player mapping changed the established portrait index: expected "
                    + expectedPlayerId + " but got " + playerA);
        }

        String firstPlayerId = PortraitSemanticIdentityResolver.playerAssetId(
                root, registry, 0).orElseThrow();
        String secondPlayerId = PortraitSemanticIdentityResolver.playerAssetId(
                root, registry, 1).orElseThrow();
        if (!firstPlayerId.equals(orderedPlayerPool.get(0).assetId())) {
            throw new AssertionError("portrait index zero did not map to the first path-sorted portrait");
        }
        if (orderedPlayerPool.size() > 1
                && !secondPlayerId.equals(orderedPlayerPool.get(1).assetId())) {
            throw new AssertionError("portrait index one did not map to the second path-sorted portrait");
        }

        String wrappedPlayerId = PortraitSemanticIdentityResolver.playerAssetId(
                root, registry, orderedPlayerPool.size()).orElseThrow();
        if (!wrappedPlayerId.equals(firstPlayerId)) {
            throw new AssertionError("portrait index equal to pool size did not wrap to the first portrait");
        }

        String negativePlayerId = PortraitSemanticIdentityResolver.playerAssetId(
                root, registry, -1).orElseThrow();
        String expectedNegativeId = orderedPlayerPool.get(orderedPlayerPool.size() - 1).assetId();
        if (!negativePlayerId.equals(expectedNegativeId)) {
            throw new AssertionError("negative portrait index did not floor-mod to the final portrait");
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
                + " wrapped=" + wrappedPlayerId
                + " negative=" + negativePlayerId
                + " npc=" + npcA
                + " authority=" + PortraitSemanticAssetAuthority.VERSION
                + " identityResolver=" + PortraitSemanticIdentityResolver.VERSION);
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
