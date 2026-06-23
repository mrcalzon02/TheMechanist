package mechanist;

import java.util.List;

/** Smoke for blueprint permission readiness forecast coverage. */
final class Milestone03BlueprintPermissionReadinessAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintPermissionReadinessAuthority.PermissionReadiness> rows =
                BlueprintPermissionReadinessAuthority.catalogReadiness();
        List<String> audit = BlueprintPermissionReadinessAuthority.definitionAuditLines();
        int recipes = BuildRecipe.allBuildRecipes().size();

        require(rows.size() == recipes, "every build recipe should have a permission readiness row");
        requireContains(audit, "owner=BlueprintPermissionReadinessAuthority", "permission owner");
        requireContains(audit, "acquisitionOwner=BlueprintAcquisitionPathAuthority", "acquisition owner");
        requireContains(audit, "catalogOwner=BuildRecipe", "catalog owner");
        requireContains(audit, "readiness=forecast-only", "forecast-only boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-ID boundary");
        requireContains(audit, "blueprintReadinessRows=" + recipes, "row count");
        requireContains(audit, "publicReady=", "public count");
        requireContains(audit, "permitOrLicense=", "permit count");
        requireContains(audit, "factionStanding=", "faction count");
        requireContains(audit, "restrictedLegalAccess=", "restricted count");
        requireContains(audit, "illicitOrStolenRisk=", "illicit count");
        requireContains(audit, "supported classes are public-ready, permit-or-license, faction-standing, restricted-legal-access, and illicit-or-stolen-risk", "permission classes");
        requireContains(audit, "Licensed Shop Counter permission blocked", "shop blocked sample");
        requireContains(audit, "blueprint not owned", "unowned blocker");
        requireContains(audit, "EMM Micro Forge permission blocked", "forge blocked sample");
        requireContains(audit, "reputation or faction standing not ready", "standing blocker");
        requireContains(audit, "license not ready", "license blocker");
        requireContains(audit, "Security Sensor Mast permission blocked", "sensor blocked sample");
        requireContains(audit, "legal access not ready", "legal access blocker");
        requireContains(audit, "owning a blueprint is necessary but not sufficient", "ownership separation");
        requireContains(audit, "materials, workbench, knowledge, placement access, utilities, and construction labor remain separately checked", "separate checks");
        requireContains(audit, "does not add live vendor offers, spend reputation, buy permits, grant licenses", "future-owner boundary");
        requireContains(audit, "bypass placement validation, or execute faction construction", "placement and faction boundary");
        requireContains(audit, "Milestone03BlueprintPermissionReadinessAuditSmoke", "guard reference");

        BlueprintPermissionReadinessAuthority.PermissionReadiness readyShop =
                BlueprintPermissionReadinessAuthority.evaluate(pathNamed("Licensed Shop Counter"), true, true, true, true, true);
        require(readyShop.permissionReady(), "owned shop with permit inputs should be permission ready");
        requireContains(readyShop.readinessLine(), "blockers=none", "ready blocker text");

        BlueprintPermissionReadinessAuthority.PermissionReadiness missingPermit =
                BlueprintPermissionReadinessAuthority.evaluate(pathNamed("Licensed Shop Counter"), true, true, true, false, true);
        require(!missingPermit.permissionReady(), "shop should block when permit is missing");
        requireContains(missingPermit.readinessLine(), "permit not ready", "permit blocker");

        BlueprintPermissionReadinessAuthority.PermissionReadiness missingStanding =
                BlueprintPermissionReadinessAuthority.evaluate(pathNamed("EMM Micro Forge"), true, false, true, true, true);
        require(!missingStanding.permissionReady(), "forge should block when standing is missing");
        requireContains(missingStanding.readinessLine(), "reputation or faction standing not ready", "standing blocker direct");

        BlueprintPermissionReadinessAuthority.PermissionReadiness restricted =
                BlueprintPermissionReadinessAuthority.evaluate(pathNamed("Security Sensor Mast"), true, true, true, true, false);
        require(!restricted.permissionReady(), "sensor should block when legal access is missing");
        requireContains(restricted.readinessLine(), "legal access not ready", "legal access direct");

        for (BlueprintPermissionReadinessAuthority.PermissionReadiness row : rows) {
            requireNotBlank(row.blueprintName(), "blueprint name");
            requireNotBlank(row.permissionClass(), "permission class");
            requireNotBlank(row.readinessLine(), "readiness line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint permission readiness audit leaked implementation text: " + line);
            }
        }
    }

    private static BlueprintAcquisitionPathAuthority.AcquisitionPath pathNamed(String name) {
        for (BlueprintAcquisitionPathAuthority.AcquisitionPath path : BlueprintAcquisitionPathAuthority.paths()) {
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

    private Milestone03BlueprintPermissionReadinessAuditSmoke() { }
}
