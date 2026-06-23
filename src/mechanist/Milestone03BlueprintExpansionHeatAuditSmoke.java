package mechanist;

import java.util.List;

/** Smoke for Phase 18 blueprint heat and suspicion audit coverage. */
final class Milestone03BlueprintExpansionHeatAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintExpansionHeatAuthority.HeatProfile> profiles = BlueprintExpansionHeatAuthority.profiles();
        List<String> audit = BlueprintExpansionHeatAuthority.definitionAuditLines();
        int recipes = BuildRecipe.allBuildRecipes().size();

        require(profiles.size() == recipes, "every build recipe should have a heat profile");
        requireContains(audit, "owner=BlueprintExpansionHeatAuthority", "heat audit owner");
        requireContains(audit, "blueprintOwner=BuildRecipe", "blueprint owner");
        requireContains(audit, "acquisitionOwner=BlueprintAcquisitionPathAuthority", "acquisition owner");
        requireContains(audit, "heatReadabilityOwner=ExpansionHeatReadabilityAuthority", "readability owner");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-ID boundary");
        requireContains(audit, "blueprintProfiles=" + recipes, "profile count");
        requireContains(audit, "heatBearingProfiles=", "heat-bearing count");
        requireContains(audit, "restrictedOrPermitProfiles=", "restricted count");
        requireContains(audit, "commerceProfiles=", "commerce count");
        requireContains(audit, "defenseProfiles=", "defense count");
        requireContains(audit, "laboratoryOrClinicProfiles=", "laboratory count");
        requireContains(audit, "productionFootprintProfiles=", "production count");
        requireContains(audit, "visible commerce", "commerce driver");
        requireContains(audit, "armed defenses", "defense driver");
        requireContains(audit, "industrial footprint", "production driver");
        requireContains(audit, "laboratory or clinic footprint", "laboratory driver");
        requireContains(audit, "access or legality risk", "access risk driver");
        requireContains(audit, "quiet, low, noticeable, high, and critical", "readability bands");
        requireContains(audit, "does not mutate gang heat, suspicion", "future mutation boundary");
        requireContains(audit, "Milestone03BlueprintExpansionHeatAuditSmoke", "guard reference");

        BlueprintExpansionHeatAuthority.HeatProfile shop = requireProfile(profiles, "Licensed Shop Counter");
        requireContains(shop.driverSummary(), "visible commerce", "shop commerce driver");
        require(shop.heatImpact() >= 2, "shop should carry heat");
        require(shop.suspicionImpact() >= 1, "shop should carry suspicion");

        BlueprintExpansionHeatAuthority.HeatProfile sensor = requireProfile(profiles, "Security Sensor Mast");
        requireContains(sensor.driverSummary(), "armed defenses", "sensor defense driver");
        requireContains(sensor.driverSummary(), "access or legality risk", "sensor access risk");
        require(sensor.suspicionImpact() >= 4, "sensor should carry visible suspicion");

        BlueprintExpansionHeatAuthority.HeatProfile hood = requireProfile(profiles, "Fume hood");
        requireContains(hood.driverSummary(), "laboratory or clinic", "fume hood laboratory driver");
        require(hood.heatImpact() >= 2, "fume hood should carry heat");

        require("quiet".equals(ExpansionHeatReadabilityAuthority.pressureBand(0)), "quiet band");
        require("noticeable".equals(ExpansionHeatReadabilityAuthority.pressureBand(6)), "noticeable band");
        require("critical".equals(ExpansionHeatReadabilityAuthority.pressureBand(20)), "critical band");

        for (BlueprintExpansionHeatAuthority.HeatProfile profile : profiles) {
            requireNotBlank(profile.blueprintName(), "blueprint name");
            requireNotBlank(profile.category(), "category");
            requireNotBlank(profile.legalLabel(), "legal label");
            require(profile.heatImpact() >= 0, "heat impact should be non-negative");
            require(profile.suspicionImpact() >= 0, "suspicion impact should be non-negative");
            requireNotBlank(profile.driverSummary(), "driver summary");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint expansion heat audit leaked implementation text: " + line);
            }
        }
    }

    private static BlueprintExpansionHeatAuthority.HeatProfile requireProfile(
            List<BlueprintExpansionHeatAuthority.HeatProfile> profiles, String name) {
        for (BlueprintExpansionHeatAuthority.HeatProfile profile : profiles) {
            if (profile != null && profile.blueprintName().equalsIgnoreCase(name)) return profile;
        }
        throw new AssertionError("Missing blueprint heat profile for " + name);
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

    private Milestone03BlueprintExpansionHeatAuditSmoke() { }
}
