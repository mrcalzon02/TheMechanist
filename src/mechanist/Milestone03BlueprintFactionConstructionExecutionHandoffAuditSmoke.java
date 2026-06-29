package mechanist;

import java.util.List;
import java.util.Map;

/** Smoke for audit-only faction construction execution handoff contracts. */
final class Milestone03BlueprintFactionConstructionExecutionHandoffAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionExecutionHandoffAuthority.ExecutionHandoff> samples =
                BlueprintFactionConstructionExecutionHandoffAuthority.sampleHandoffs();
        List<String> audit = BlueprintFactionConstructionExecutionHandoffAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample handoffs");
        requireContains(audit, "owner=BlueprintFactionConstructionExecutionHandoffAuthority", "handoff owner");
        requireContains(audit, "jobOwner=BlueprintFactionConstructionJobDefinitionAuthority", "job owner");
        requireContains(audit, "materialOwner=BlueprintFactionConstructionMaterialReservationAuthority", "material owner");
        requireContains(audit, "crewOwner=BlueprintFactionConstructionCrewAssignmentAuthority", "crew owner");
        requireContains(audit, "siteOwner=BlueprintFactionConstructionSiteReadinessAuthority", "site owner");
        requireContains(audit, "budgetOwner=BlueprintFactionConstructionBudgetHeatAuthorizationAuthority", "budget owner");
        requireContains(audit, "releaseOwner=BlueprintFactionConstructionCancellationReleaseAuthority", "release owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "job authorization, material reservation readiness, crew assignment readiness", "readiness list");
        requireContains(audit, "sampleHandoffs=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "rollbackReady=2", "rollback count");
        requireContains(audit, "siteBlocked=2", "site blocked count");
        requireContains(audit, "job-storage-public execution handoff for Storage Crate", "storage handoff");
        requireContains(audit, "readiness=capability:true, materials:true, crew:true, site:true, budgetHeat:true, rollback:true", "ready gates");
        requireContains(audit, "job-sensor-restricted execution handoff for Security Sensor Mast", "sensor handoff");
        requireContains(audit, "site readiness blocked", "site blocker");
        requireContains(audit, "job-shop-public execution handoff for Licensed Shop Counter", "shop handoff");
        requireContains(audit, "rollback release plan blocked", "rollback blocker");
        requireContains(audit, "atomically re-check the handoff", "atomic rule");
        requireContains(audit, "does not create a live job queue", "non-mutation boundary");
        requireContains(audit, "mutate heat, mutate suspicion", "heat boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionExecutionHandoffAuditSmoke", "guard reference");

        BlueprintFactionConstructionExecutionHandoffAuthority.ExecutionHandoff direct =
                BlueprintFactionConstructionExecutionHandoffAuthority.handoffFor(
                        BlueprintFactionConstructionJobDefinitionAuthority.definitionFor("job-direct", Faction.HIVER,
                                BuildRecipe.storage(), "AUTHORIZED", "storage alcove"),
                        BlueprintFactionConstructionMaterialReservationAuthority.reservationFor("job-direct", Faction.HIVER,
                                BuildRecipe.storage(), Map.of("Construction supplies", 4, "Rivet set", 2)),
                        BlueprintFactionConstructionCrewAssignmentAuthority.assignmentFor("job-direct", Faction.HIVER,
                                BuildRecipe.storage(), 1),
                        BlueprintFactionConstructionSiteReadinessAuthority.readinessFor("job-direct", Faction.HIVER,
                                BuildRecipe.storage(), "storage alcove", true, true, true, true, true),
                        BlueprintFactionConstructionBudgetHeatAuthorizationAuthority.authorizationFor("job-direct", Faction.HIVER,
                                BuildRecipe.storage(), 100, 20, 20),
                        BlueprintFactionConstructionCancellationReleaseAuthority.releaseFor("job-direct", Faction.HIVER,
                                BuildRecipe.storage(), "FAILED", "rollback predeclared", true, true, true, true, true));
        require(direct.handoffReady(), "direct handoff should be ready");
        requireContains(direct.boundaryLine(), "audit only, no faction job execution", "direct boundary");

        for (BlueprintFactionConstructionExecutionHandoffAuthority.ExecutionHandoff sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.lifecycleState(), "lifecycle state");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction handoff audit leaked implementation text: " + line);
            }
        }
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

    private Milestone03BlueprintFactionConstructionExecutionHandoffAuditSmoke() { }
}
