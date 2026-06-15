package mechanist;

import mechanist.assets.AssetManager;
import mechanist.assets.AssetType;

import java.nio.file.Path;

/** Focused verification for legacy-compatible NPC identity-to-partition mapping. */
public final class PortraitSemanticNpcIdentityResolverSmoke {
    public static void main(String[] args) {
        assertSelection(context("celebrity-alpha", "", "", "historian", "Alpha", "none", 0, false, false),
                "name_locked_profile", false, true);
        assertSelection(context("", "human", "", "household chef", "Mara", "hiver", 0, false, false),
                "servants_butlers_and_chefs", false, false);
        assertSelection(context("", "human", "", "hospital surgeon", "Iria", "sororitas", 0, false, false),
                "sisters_hospital", false, false);
        assertSelection(context("", "farm beast", "goat", "livestock", "Brindle", "none", 0, true, false),
                "farm_beasts", true, false);
        assertSelection(context("", "human", "", "student", "Pell", "none", 0, false, true),
                "schola_children", false, false);
        assertSelection(context("", "machine", "", "rogue automata", "Unit 7", "rogue_machine", 0, false, false),
                "rogue_automata_servitors", true, false);
        assertSelection(context("", "human", "", "worker", "A", "hiver", 0, false, false),
                "administratum", false, false);
        assertSelection(context("", "human", "", "worker", "B", "hiver", 7, false, false),
                "gangers", false, false);
        assertSelection(context("", "human", "", "worker", "C", "hiver", 14, false, false),
                "servants_butlers_and_chefs", false, false);

        Path root = Path.of(".").toAbsolutePath().normalize();
        var registry = AssetManager.registry();
        var ordinaryContext = context("", "human", "", "records clerk", "Clerk 4421",
                "administratum", 3, false, false);
        String selectedA = PortraitSemanticNpcIdentityResolver.selectionFor(ordinaryContext)
                .flatMap(selection -> PortraitSemanticPartitionResolver.assetId(
                        root,
                        registry,
                        selection.partitionKey(),
                        selection.stableIdentity(),
                        selection.allowRestricted(),
                        selection.allowNameLocked()))
                .orElseThrow(() -> new AssertionError("ordinary NPC semantic selection returned empty"));
        String selectedB = PortraitSemanticNpcIdentityResolver.selectionFor(ordinaryContext)
                .flatMap(selection -> PortraitSemanticPartitionResolver.assetId(
                        root,
                        registry,
                        selection.partitionKey(),
                        selection.stableIdentity(),
                        selection.allowRestricted(),
                        selection.allowNameLocked()))
                .orElseThrow();
        if (!selectedA.equals(selectedB)) {
            throw new AssertionError("NPC semantic identity selection was not deterministic");
        }
        var metadata = registry.find(selectedA).orElseThrow(
                () -> new AssertionError("selected NPC portrait absent from registry: " + selectedA));
        if (metadata.type() != AssetType.PORTRAIT) {
            throw new AssertionError("selected NPC semantic asset was not PORTRAIT typed: " + selectedA);
        }

        System.out.println("PortraitSemanticNpcIdentityResolverSmoke PASS"
                + " selected=" + selectedA
                + " identityResolver=" + PortraitSemanticNpcIdentityResolver.VERSION
                + " partitionResolver=" + PortraitSemanticPartitionResolver.VERSION);
    }

    private static PortraitSemanticNpcIdentityResolver.Context context(
            String locked,
            String creature,
            String animalProfile,
            String role,
            String name,
            String faction,
            int portraitIndex,
            boolean animal,
            boolean child
    ) {
        return new PortraitSemanticNpcIdentityResolver.Context(
                locked, creature, animalProfile, role, name, faction, portraitIndex, animal, child);
    }

    private static void assertSelection(
            PortraitSemanticNpcIdentityResolver.Context context,
            String expectedPartition,
            boolean expectedRestricted,
            boolean expectedNameLocked
    ) {
        var selection = PortraitSemanticNpcIdentityResolver.selectionFor(context).orElseThrow(
                () -> new AssertionError("selection was empty for " + expectedPartition));
        if (!expectedPartition.equals(selection.partitionKey())) {
            throw new AssertionError("expected partition " + expectedPartition
                    + " but got " + selection.partitionKey());
        }
        if (selection.allowRestricted() != expectedRestricted) {
            throw new AssertionError(expectedPartition + " restricted authorization mismatch");
        }
        if (selection.allowNameLocked() != expectedNameLocked) {
            throw new AssertionError(expectedPartition + " name-lock authorization mismatch");
        }
        if (selection.stableIdentity() == null || selection.stableIdentity().isBlank()) {
            throw new AssertionError(expectedPartition + " stable identity was blank");
        }
    }

    private PortraitSemanticNpcIdentityResolverSmoke() {}
}
