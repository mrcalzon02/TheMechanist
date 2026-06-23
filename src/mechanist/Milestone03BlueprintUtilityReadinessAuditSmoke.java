package mechanist;

import java.util.EnumSet;
import java.util.List;

/** Smoke for Phase 18 construction utility readiness audit coverage. */
final class Milestone03BlueprintUtilityReadinessAuditSmoke {
    public static void main(String[] args) {
        List<String> audit = ConstructionGovernanceAuthority.utilityReadinessAuditLines();
        requireContains(audit, "owner=ConstructionGovernanceAuthority", "utility audit owner");
        requireContains(audit, "blueprintOwner=BuildRecipe", "blueprint owner");
        requireContains(audit, "utilityNetworkOwner=future utility network owner", "future utility owner");
        requireContains(audit, "readiness=metadata-only", "metadata-only readiness");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-ID boundary");
        requireContains(audit, "utilityBearingRooms=", "room utility coverage");
        requireContains(audit, "utilityBearingBlueprints=", "blueprint utility coverage");
        requireContains(audit, "noUtilityBlueprints=", "no-utility blueprint count");
        requireContains(audit, "tracked families include power, water, waste, ventilation, exhaust, light, data, road access, storage access, and staffing access", "tracked utility families");
        requireContains(audit, "missing machine utility sample=", "missing utility sample");
        requireContains(audit, "machine_placement blocked: missing utility STAFFING_ACCESS", "fail-closed missing staffing access");
        requireContains(audit, "machine_placement ok: metadata validation passed", "ready utility sample");
        requireContains(audit, "defense_turret blocked: passability check failed", "passability interaction");
        requireContains(audit, "required utility hooks fail closed when absent", "fail-closed rule");
        requireContains(audit, "not permission to mutate a live power, water, exhaust, data, road, storage, or staffing network", "preview-not-mutation boundary");
        requireContains(audit, "does not create utility grids, consume fuel or water, schedule workers, mutate room ownership", "mutation boundary");
        requireContains(audit, "Milestone03BlueprintUtilityReadinessAuditSmoke", "guard reference");

        ConstructionGovernanceAuthority authority = new ConstructionGovernanceAuthority();
        String missing = authority.validateBlueprintMetadata("machine_placement", ConstructionGovernanceAuthority.RoomRole.FORGE,
                EnumSet.of(ConstructionGovernanceAuthority.UtilityTag.POWER, ConstructionGovernanceAuthority.UtilityTag.STORAGE_ACCESS),
                true, "smoke missing staffing");
        requireContains(missing, "missing utility STAFFING_ACCESS", "direct fail-closed missing utility");

        String ready = authority.validateBlueprintMetadata("machine_placement", ConstructionGovernanceAuthority.RoomRole.FORGE,
                EnumSet.of(ConstructionGovernanceAuthority.UtilityTag.POWER, ConstructionGovernanceAuthority.UtilityTag.STORAGE_ACCESS,
                        ConstructionGovernanceAuthority.UtilityTag.STAFFING_ACCESS),
                true, "smoke ready staffing");
        requireContains(ready, "metadata validation passed", "direct ready utility validation");

        String wrongRoom = authority.validateBlueprintMetadata("defense_precinct_fixture", ConstructionGovernanceAuthority.RoomRole.HAB,
                EnumSet.of(ConstructionGovernanceAuthority.UtilityTag.LIGHT, ConstructionGovernanceAuthority.UtilityTag.DATA),
                true, "smoke wrong room");
        requireContains(wrongRoom, "expected room role SECURITY", "direct room-role validation");

        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint utility readiness audit leaked implementation text: " + line);
            }
        }
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03BlueprintUtilityReadinessAuditSmoke() { }
}
