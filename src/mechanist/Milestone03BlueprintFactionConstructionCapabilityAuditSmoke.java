package mechanist;

import java.util.List;

/** Smoke for audit-only faction construction capability coverage. */
final class Milestone03BlueprintFactionConstructionCapabilityAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionCapabilityAuthority.FactionCapability> rows =
                BlueprintFactionConstructionCapabilityAuthority.catalogCapabilities();
        List<String> audit = BlueprintFactionConstructionCapabilityAuthority.definitionAuditLines();
        int recipes = BuildRecipe.allBuildRecipes().size();

        require(rows.size() == recipes, "every build recipe should have a faction construction capability row");
        requireContains(audit, "owner=BlueprintFactionConstructionCapabilityAuthority", "capability owner");
        requireContains(audit, "blueprintOwner=BuildRecipe", "blueprint owner");
        requireContains(audit, "parityOwner=RoomConstructionParityAuthority", "parity owner");
        requireContains(audit, "permissionOwner=BlueprintPermissionReadinessAuthority", "permission owner");
        requireContains(audit, "readiness=audit-only", "audit-only boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-ID boundary");
        requireContains(audit, "blueprintCapabilityRows=" + recipes, "row count");
        requireContains(audit, "factionCandidates=", "candidate count");
        requireContains(audit, "publicCandidates=", "public candidate count");
        requireContains(audit, "restrictedCandidates=", "restricted candidate count");
        requireContains(audit, "defensiveCandidates=", "defensive candidate count");
        requireContains(audit, "facilityCandidates=", "facility candidate count");
        requireContains(audit, "candidate readiness considers faction-use parity, permission readiness, faction budget, construction crew, room claim, and construction materials", "readiness dimensions");
        requireContains(audit, "Storage Crate faction construction capable", "storage capable sample");
        requireContains(audit, "Security Sensor Mast faction construction blocked", "sensor blocked sample");
        requireContains(audit, "permission readiness blocked", "permission blocker");
        requireContains(audit, "Licensed Shop Counter faction construction blocked", "shop blocked sample");
        requireContains(audit, "faction budget not ready", "budget blocker");
        requireContains(audit, "construction crew not ready", "crew blocker");
        requireContains(audit, "only a planning signal", "planning-signal boundary");
        requireContains(audit, "does not spawn faction construction jobs, mutate room ownership", "job and ownership boundary");
        requireContains(audit, "reserve or consume materials, spend faction budget, grant permits", "resource boundary");
        requireContains(audit, "bypass placement validation, or complete construction", "placement boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionCapabilityAuditSmoke", "guard reference");

        BlueprintFactionConstructionCapabilityAuthority.FactionCapability storage =
                requireCapability(rows, "Storage Crate");
        require(storage.factionCandidate(), "storage should be a faction construction candidate");
        requireContains(storage.capabilityLine(), "blockers=none", "storage ready line");

        BlueprintFactionConstructionCapabilityAuthority.FactionCapability sensor =
                BlueprintFactionConstructionCapabilityAuthority.evaluate(BuildRecipe.securitySensorMast(),
                        BlueprintPermissionReadinessAuthority.evaluate(
                                BlueprintAcquisitionPathAuthority.pathFor(BuildRecipe.securitySensorMast()),
                                true, true, true, true, false),
                        true, true, true, true);
        require(!sensor.blockers().isEmpty(), "sensor should have a legal-access blocker");
        requireContains(sensor.capabilityLine(), "permission readiness blocked", "sensor permission blocker");

        BlueprintFactionConstructionCapabilityAuthority.FactionCapability shop =
                BlueprintFactionConstructionCapabilityAuthority.evaluate(BuildRecipe.shopCounter(),
                        BlueprintPermissionReadinessAuthority.evaluate(BlueprintAcquisitionPathAuthority.pathFor(BuildRecipe.shopCounter()),
                                true, true, true, true, true),
                        false, false, true, true);
        requireContains(shop.capabilityLine(), "faction budget not ready", "shop budget blocker");
        requireContains(shop.capabilityLine(), "construction crew not ready", "shop crew blocker");

        for (BlueprintFactionConstructionCapabilityAuthority.FactionCapability row : rows) {
            requireNotBlank(row.blueprintName(), "blueprint name");
            requireNotBlank(row.constructionCategory(), "construction category");
            requireNotBlank(row.factionUseStatus(), "faction use status");
            requireNotBlank(row.permissionClass(), "permission class");
            requireNotBlank(row.capabilityLine(), "capability line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction capability audit leaked implementation text: " + line);
            }
        }
    }

    private static BlueprintFactionConstructionCapabilityAuthority.FactionCapability requireCapability(
            List<BlueprintFactionConstructionCapabilityAuthority.FactionCapability> rows, String name) {
        for (BlueprintFactionConstructionCapabilityAuthority.FactionCapability row : rows) {
            if (row != null && row.blueprintName().equalsIgnoreCase(name)) return row;
        }
        throw new AssertionError("Missing faction construction capability for " + name);
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

    private Milestone03BlueprintFactionConstructionCapabilityAuditSmoke() { }
}
