package mechanist;

import java.util.List;
import java.util.Map;

/** Smoke for audit-only faction construction queue admission contracts. */
final class Milestone03BlueprintFactionConstructionQueueAdmissionAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionQueueAdmissionAuthority.QueueAdmission> samples =
                BlueprintFactionConstructionQueueAdmissionAuthority.sampleAdmissions();
        List<String> audit = BlueprintFactionConstructionQueueAdmissionAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample admissions");
        requireContains(audit, "owner=BlueprintFactionConstructionQueueAdmissionAuthority", "queue admission owner");
        requireContains(audit, "handoffOwner=BlueprintFactionConstructionExecutionHandoffAuthority", "handoff owner");
        requireContains(audit, "jobOwner=BlueprintFactionConstructionJobDefinitionAuthority", "job owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "reserve a job slot only after the execution handoff is ready", "admission rule");
        requireContains(audit, "sampleAdmissions=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "reserved=1", "reserved count");
        requireContains(audit, "slotBlocked=1", "slot blocked count");
        requireContains(audit, "job-storage-public queue admission for Storage Crate", "storage admission");
        requireContains(audit, "admitted=RESERVED", "reserved state");
        requireContains(audit, "job-sensor-restricted queue admission for Security Sensor Mast", "sensor admission");
        requireContains(audit, "execution handoff blocked", "handoff blocker");
        requireContains(audit, "job-shop-public queue admission for Licensed Shop Counter", "shop admission");
        requireContains(audit, "queue slot unavailable", "slot blocker");
        requireContains(audit, "keep queue admission separate from material removal", "separation rule");
        requireContains(audit, "does not create a live job queue", "non-mutation boundary");
        requireContains(audit, "mutate heat, mutate suspicion", "attention boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionQueueAdmissionAuditSmoke", "guard reference");

        BlueprintFactionConstructionExecutionHandoffAuthority.ExecutionHandoff readyHandoff =
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
        BlueprintFactionConstructionQueueAdmissionAuthority.QueueAdmission direct =
                BlueprintFactionConstructionQueueAdmissionAuthority.admissionFor(readyHandoff, 8, true);
        require(direct.admissionReady(), "direct admission should be ready");
        require("RESERVED".equals(direct.admittedState()), "direct admission should project RESERVED");
        requireContains(direct.boundaryLine(), "audit only, no live queue mutation", "direct boundary");

        for (BlueprintFactionConstructionQueueAdmissionAuthority.QueueAdmission sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.requestedState(), "requested state");
            requireNotBlank(sample.admittedState(), "admitted state");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction queue admission audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionQueueAdmissionAuditSmoke() { }
}
