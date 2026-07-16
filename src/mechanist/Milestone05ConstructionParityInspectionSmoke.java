package mechanist;

import java.util.List;

/** Focused Phase 18 smoke for construction parity and editor/audit inspection. */
final class Milestone05ConstructionParityInspectionSmoke {
    public static void main(String[] args) {
        List<BuildRecipe> recipes = BuildRecipe.allBuildRecipes();
        List<ConstructionParityInspectionAuthority.RecipeInspection> inspections =
                ConstructionParityInspectionAuthority.inspectAll();
        List<String> audit = ConstructionParityInspectionAuthority.auditLines();

        require(!recipes.isEmpty(), "construction catalog should not be empty");
        require(inspections.size() == recipes.size(),
                "every construction recipe should have exactly one parity inspection");
        int valid = 0;
        int conditional = 0;
        int supportedFaction = 0;
        for (ConstructionParityInspectionAuthority.RecipeInspection inspection
                : inspections) {
            require(inspection != null, "parity inspection must not be null");
            require(inspection.valid(),
                    "parity inspection should expose complete data: " + inspection);
            valid++;
            if (inspection.playerCapability()
                    == ConstructionParityInspectionAuthority.Capability.CONDITIONAL
                    || inspection.factionCapability()
                    == ConstructionParityInspectionAuthority.Capability.CONDITIONAL) {
                conditional++;
            }
            if (inspection.factionCapability()
                    == ConstructionParityInspectionAuthority.Capability.SUPPORTED) {
                supportedFaction++;
            }
            require(!inspection.recipeName().isBlank(), "recipe name must be player-facing");
            require(!inspection.category().isBlank(),
                    inspection.recipeName() + " must have a construction category");
            require(inspection.blueprintMappingValid(),
                    inspection.recipeName() + " must map to a stable blueprint identifier");
            require(!inspection.blueprintName().isBlank(),
                    inspection.recipeName() + " must have a readable blueprint name");
            require(!inspection.issuingFaction().isBlank(),
                    inspection.recipeName() + " must name an issuing market or faction");
            require(!inspection.vendorCategory().isBlank(),
                    inspection.recipeName() + " must name a vendor category");
            require(!inspection.acquisitionPath().isBlank(),
                    inspection.recipeName() + " must name an acquisition path");
            require(!inspection.accessGate().isBlank(),
                    inspection.recipeName() + " must explain access and reputation");
            require(!inspection.legalClass().isBlank(),
                    inspection.recipeName() + " must expose legality");
            require(!inspection.materialSummary().isBlank(),
                    inspection.recipeName() + " must expose materials");
            require(!inspection.workforceSummary().isBlank(),
                    inspection.recipeName() + " must expose player/faction workforce rules");
            require(!inspection.exceptionClass().isBlank()
                            && !inspection.exceptionReason().isBlank(),
                    inspection.recipeName() + " must classify any parity exception");
            String row = inspection.editorRow();
            requireContains(row, "player=", inspection.recipeName() + " player capability row");
            requireContains(row, "factions=", inspection.recipeName() + " faction capability row");
            requireContains(row, "vendor=", inspection.recipeName() + " vendor row");
            requireContains(row, "legal=", inspection.recipeName() + " legal row");
            requireContains(row, "parity=", inspection.recipeName() + " parity row");
            require(!PlayerFacingText.containsLikelyLeak(row),
                    inspection.recipeName() + " editor row leaked implementation text: " + row);
        }

        require(valid == recipes.size(), "all parity inspections should be structurally valid");
        require(conditional > 0,
                "audit should preserve conditional parity rather than declaring everything symmetric");
        require(supportedFaction > 0,
                "at least the live physical faction construction slice should report supported faction capability");
        requireContains(audit, "recipes=" + recipes.size(), "parity recipe count");
        requireContains(audit, "invalidMappings=0", "stable mapping coverage");
        requireContains(audit, "contractRewardPaths=", "contract acquisition coverage");
        requireContains(audit, "salvageOrRecoveryPaths=", "salvage acquisition coverage");
        requireContains(audit, "Vendor category coverage:", "vendor category coverage");
        requireContains(audit, "Legal/access coverage:", "legal coverage");
        requireContains(audit, "contract rewards=ConstructionBlueprintContractRewardAuthority",
                "live contract reward owner");
        requireContains(audit, "seizure, salvage, and specialists=FactionStrategicAssetAuthority",
                "live strategic asset owner");
        requireContains(audit, "Infopedia dossiers=ConstructionBlueprintInfopediaBridgeAuthority",
                "live Infopedia owner");
        requireContains(audit, "never upgrades them to symmetric by assumption",
                "honest exception boundary");

        BuildRecipe sample = recipes.get(0);
        List<String> filtered =
                ConstructionParityInspectionAuthority.editorRows(sample.name);
        requireContains(filtered, sample.name, "filtered parity editor row");
        require(ConstructionParityInspectionAuthority.editorRows(
                        "no-such-construction-parity-entry").size() == 1,
                "empty parity filter should return one readable empty-state row");
        requireContains(ConstructionParityInspectionAuthority.editorRows(
                        "no-such-construction-parity-entry"),
                "No construction parity entries match",
                "parity editor empty state");

        System.out.println("Milestone 05 construction parity inspection smoke passed.");
    }

    private static void requireContains(List<String> lines, String expected,
                                        String label) {
        for (String line : lines) {
            if (line != null && line.contains(expected)) return;
        }
        throw new AssertionError("Expected " + label + " to contain '"
                + expected + "': " + lines);
    }

    private static void requireContains(String text, String expected,
                                        String label) {
        require(text != null && text.contains(expected),
                "Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone05ConstructionParityInspectionSmoke() { }
}
