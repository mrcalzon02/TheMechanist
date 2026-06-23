package mechanist;

import java.util.List;
import java.util.Set;

/** Smoke for the Phase 18 shared quality-band definition audit surface. */
final class Milestone03QualityBandDefinitionAuditSmoke {
    public static void main(String[] args) {
        List<String> audit = QualityAuthorityApi.definitionAuditLines();
        requireContains(audit, "owner=QualityAuthorityApi", "quality authority owner");
        requireContains(audit, "itemQualityOwner=ItemQuality", "item quality owner");
        requireContains(audit, "profileCount=8", "profile count");
        requireContains(audit, "itemQualityOrder=Junk > Shoddy > Common > Serviceable > Fine > Masterwork > Noble > Archeotech", "quality order");
        requireContains(audit, "doctrineBands=Junk > Common > Serviceable > Fine > Masterwork > Noble > Archeotech", "doctrine bands");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-ID boundary");
        requireContains(audit, "Shoddy is a degradation quality and not a target doctrine school", "Shoddy doctrine boundary");
        requireContains(audit, "Common is the civic baseline", "common baseline");
        requireContains(audit, "value and charge multipliers through ItemQuality", "item quality multipliers");
        requireContains(audit, "usefulness, reliability, efficiency, defect pressure, comfort, prestige", "profile role fields");
        requireContains(audit, "minimum of known doctrine tier, recipe requirement tier, machine ceiling tier", "capping rule");
        requireContains(audit, "Quality tier audit: Junk tier=0 value x0.20", "junk profile");
        requireContains(audit, "Quality tier audit: Archeotech tier=7 value x7.00", "archeotech profile");
        requireContains(audit, "Milestone03QualityBandDefinitionAuditSmoke", "guard reference");

        require(ItemQuality.tierIndex("Fine Autopistol") == 4, "Fine item prefix should map to tier 4");
        require(ItemQuality.priced(100, "Archeotech Autopistol") == 700, "Archeotech item value should use item multiplier");
        require(QualityAuthorityApi.bestKnownTier(Set.of("Masterwork Tools Patterns")) == 5,
                "Masterwork doctrine should raise known tier");
        require(QualityAuthorityApi.requiredTierForRecipeKnowledge("Fine Ballistics Patterns") == 4,
                "Fine recipe knowledge should require Fine tier");
        require(QualityAuthorityApi.cappedTier(5, 4, 7, -1, -1, -1) == 4,
                "recipe tier should cap higher doctrine and machine quality");
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Quality band definition audit leaked implementation text: " + line);
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03QualityBandDefinitionAuditSmoke() { }
}
