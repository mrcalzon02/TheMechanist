package mechanist;

import java.util.List;

/** Smoke for the Phase 18 blueprint acquisition-path audit surface. */
final class Milestone03BlueprintAcquisitionPathAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintAcquisitionPathAuthority.AcquisitionPath> paths = BlueprintAcquisitionPathAuthority.paths();
        List<String> audit = BlueprintAcquisitionPathAuthority.definitionAuditLines();
        int recipes = BuildRecipe.allBuildRecipes().size();

        require(paths.size() == recipes, "every build recipe should have an acquisition path");
        requireContains(audit, "owner=BlueprintAcquisitionPathAuthority", "acquisition owner");
        requireContains(audit, "catalogOwner=BuildRecipe", "catalog owner");
        requireContains(audit, "categoryOwner=ConstructionCategoryAuthority", "category owner");
        requireContains(audit, "traderStockOwner=TraderTradeActionAuthority", "trader owner");
        requireContains(audit, "factionStockOwner=FactionInventoryStockAuthority", "stock owner");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-ID boundary");
        requireContains(audit, "blueprintPaths=" + recipes, "path count");
        requireContains(audit, "factionBoundPaths=", "faction-bound count");
        requireContains(audit, "permitOrLicensePaths=", "permit count");
        requireContains(audit, "public construction market", "public channel");
        requireContains(audit, "faction representative", "faction channel");
        requireContains(audit, "civic permit office", "permit channel");
        requireContains(audit, "Mechanist Collegia vendor", "mechanist channel");
        requireContains(audit, "Guard quartermaster", "guard channel");
        requireContains(audit, "Civic Wardens armory desk", "wardens channel");
        requireContains(audit, "theft or black-market broker", "illicit channel");
        requireContains(audit, "owning a blueprint is distinct from having permission", "ownership boundary");
        requireContains(audit, "does not add live vendor offers", "future vendor boundary");
        requireContains(audit, "heat mutation, suspicion mutation, or faction construction execution", "future consequence boundary");
        requireContains(audit, "Milestone03BlueprintAcquisitionPathAuditSmoke", "guard reference");

        BlueprintAcquisitionPathAuthority.AcquisitionPath shop = requirePath(paths, "Licensed Shop Counter");
        requireContains(shop.representativeType(), "civic permit office", "shop representative");
        requireContains(shop.legalLabel(), "civic permit-bound", "shop permit label");
        requireContains(shop.acquisitionPath(), "permit or license check", "shop acquisition gate");

        BlueprintAcquisitionPathAuthority.AcquisitionPath sensor = requirePath(paths, "Security Sensor Mast");
        requireContains(sensor.representativeType(), "Civic Wardens armory desk", "security representative");
        requireContains(sensor.legalLabel(), "restricted civic", "security restriction");
        requireContains(sensor.accessLabel(), "knowledge gate: Security Cogitator Rites", "security knowledge gate");

        BlueprintAcquisitionPathAuthority.AcquisitionPath forge = requirePath(paths, "EMM Micro Forge");
        requireContains(forge.representativeType(), "Mechanist Collegia vendor", "forge representative");
        requireContains(forge.legalLabel(), "forge license-bound", "forge license");
        requireContains(forge.acquisitionPath(), "earn standing", "forge standing path");

        for (BlueprintAcquisitionPathAuthority.AcquisitionPath path : paths) {
            requireNotBlank(path.blueprintName(), "blueprint name");
            requireNotBlank(path.constructionCategory(), "construction category");
            requireNotBlank(path.representativeType(), "representative");
            requireNotBlank(path.accessLabel(), "access label");
            requireNotBlank(path.acquisitionPath(), "acquisition path");
            requireNotBlank(path.legalLabel(), "legal label");
            requireNotBlank(path.explanation(), "explanation");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint acquisition audit leaked implementation text: " + line);
            }
        }
    }

    private static BlueprintAcquisitionPathAuthority.AcquisitionPath requirePath(
            List<BlueprintAcquisitionPathAuthority.AcquisitionPath> paths, String name) {
        for (BlueprintAcquisitionPathAuthority.AcquisitionPath path : paths) {
            if (path != null && path.blueprintName().equalsIgnoreCase(name)) return path;
        }
        throw new AssertionError("Missing acquisition path for " + name);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireNotBlank(String value, String label) {
        require(value != null && !value.isBlank(), "expected nonblank " + label);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03BlueprintAcquisitionPathAuditSmoke() { }
}
