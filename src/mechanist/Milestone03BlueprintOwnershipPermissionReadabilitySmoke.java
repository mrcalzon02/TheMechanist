package mechanist;

import java.util.List;

/** Smoke for blueprint ownership versus build-permission readability. */
final class Milestone03BlueprintOwnershipPermissionReadabilitySmoke {
    public static void main(String[] args) {
        BuildRecipe publicRecipe = BuildRecipe.shopCounter();
        List<String> publicPreview = ConstructionReadabilityAuthority.preview(publicRecipe, 4, 1, 8, 9, "ok",
                List.of("Warehouse inventory tag bundle 1/1"));
        requireContains(publicPreview, "owning the blueprint is separate from permission", "ownership boundary");
        requireContains(publicPreview, "reputation, license, permit, materials, workbench, knowledge", "access factors");
        requireContains(publicPreview, "placement access, utilities, and labor", "placement and labor factors");

        BuildRecipe restrictedRecipe = BuildRecipe.securitySensorMast();
        List<String> restrictedPreview = ConstructionReadabilityAuthority.preview(restrictedRecipe, 1, 0, 3, 4,
                "need Security Cogitator Rites knowledge", List.of("Sensor lens 0/1"));
        requireContains(restrictedPreview, "faction Civic Wardens", "restricted faction label");
        requireContains(restrictedPreview, "owning the blueprint is separate from permission", "restricted ownership boundary");
        requireContains(restrictedPreview, "Placement: BLOCKED", "restricted block remains visible");

        List<String> acquisitionAudit = BlueprintAcquisitionPathAuthority.definitionAuditLines();
        requireContains(acquisitionAudit, "owning a blueprint is distinct from having permission", "acquisition ownership audit");
        requireContains(acquisitionAudit, "materials, workbench, knowledge, placement access, utilities, and construction labor", "acquisition access factors");

        for (String line : publicPreview) rejectLeaks(line);
        for (String line : restrictedPreview) rejectLeaks(line);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.toLowerCase().contains(expected.toLowerCase())) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void rejectLeaks(String line) {
        if (PlayerFacingText.containsLikelyLeak(line)) {
            throw new AssertionError("Blueprint ownership preview leaked implementation text: " + line);
        }
    }

    private Milestone03BlueprintOwnershipPermissionReadabilitySmoke() { }
}
