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

        var audit = PortraitSemanticAssetAuthority.audit(root, registry);
        if (!audit.passed()) {
            throw new AssertionError("portrait partition audit failed: " + audit.errors());
        }

        var playerPool = PortraitSemanticAssetAuthority.activePlayerPool(root, registry);
        var npcPool = PortraitSemanticAssetAuthority.activeNpcPool(root, registry);
        if (playerPool.isEmpty()) throw new AssertionError("active player portrait pool was empty");
        if (npcPool.isEmpty()) throw new AssertionError("active NPC portrait pool was empty");

        assertOrdinaryPool("player", playerPool);
        assertOrdinaryPool("NPC", npcPool);

        String playerA = PortraitSemanticAssetAuthority.runtimePlayerAssetId(
                root, registry, "administratum baseline human", 7).orElseThrow(
                () -> new AssertionError("player portrait selection returned empty"));
        String playerB = PortraitSemanticAssetAuthority.runtimePlayerAssetId(
                root, registry, "administratum baseline human", 7).orElseThrow();
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
                + " activePlayer=" + playerPool.size()
                + " activeNpc=" + npcPool.size()
                + " player=" + playerA
                + " npc=" + npcA
                + " authority=" + PortraitSemanticAssetAuthority.VERSION);
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
