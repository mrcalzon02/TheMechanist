package mechanist;

import java.util.List;

/** Smoke for definition-only faction construction job contracts. */
final class Milestone03BlueprintFactionConstructionJobDefinitionAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionJobDefinitionAuthority.JobDefinition> samples =
                BlueprintFactionConstructionJobDefinitionAuthority.sampleDefinitions();
        List<String> audit = BlueprintFactionConstructionJobDefinitionAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample faction construction job definitions");
        requireContains(audit, "owner=BlueprintFactionConstructionJobDefinitionAuthority", "job definition owner");
        requireContains(audit, "capabilityOwner=BlueprintFactionConstructionCapabilityAuthority", "capability owner");
        requireContains(audit, "permissionOwner=BlueprintPermissionReadinessAuthority", "permission owner");
        requireContains(audit, "stagedConstructionOwner=ProgressiveConstructionAuthority", "staged construction owner");
        requireContains(audit, "readiness=definition-only", "definition-only boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-ID boundary");
        requireContains(audit, "REQUESTED, BLOCKED, AUTHORIZED, RESERVED, STAGED, IN_PROGRESS, COMPLETED, CANCELLED, and FAILED", "lifecycle states");
        requireContains(audit, "sampleJobs=3", "sample job count");
        requireContains(audit, "requested=1", "requested count");
        requireContains(audit, "authorized=1", "authorized count");
        requireContains(audit, "blocked=1", "blocked count");
        requireContains(audit, "jobId, faction, blueprint, target room or site, lifecycle state", "identity fields");
        requireContains(audit, "permission readiness, capability readiness, material ledger, crew assignment, budget note, heat projection, and placement validation note", "readiness fields");
        requireContains(audit, "job-storage-public AUTHORIZED Storage Crate", "authorized storage sample");
        requireContains(audit, "job-sensor-restricted BLOCKED Security Sensor Mast", "blocked sensor sample");
        requireContains(audit, "job-shop-public REQUESTED Licensed Shop Counter", "requested shop sample");
        requireContains(audit, "definition only, no job queue mutation", "sample non-mutation line");
        requireContains(audit, "future execution owner must reserve materials, bind crew, confirm room claim, re-run placement validation", "future execution rule");
        requireContains(audit, "does not create a live job queue, reserve or consume materials, assign workers, mutate room ownership", "mutation boundary");
        requireContains(audit, "apply heat or suspicion, place objects, advance labor, or complete construction", "effect boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionJobDefinitionAuditSmoke", "guard reference");

        BlueprintFactionConstructionJobDefinitionAuthority.JobDefinition direct =
                BlueprintFactionConstructionJobDefinitionAuthority.definitionFor("job-direct", Faction.MECHANIST_COLLEGIA,
                        BuildRecipe.microForge(), "RESERVED", "forge room");
        requireContains(direct.requiredFields(), "material ledger", "direct material ledger field");
        requireContains(direct.requiredFields(), "crew assignment", "direct crew field");
        requireContains(direct.readinessSource(), "permission=", "direct permission source");
        requireContains(direct.boundaryLine(), "definition only, no job queue mutation", "direct non-mutation");

        for (BlueprintFactionConstructionJobDefinitionAuthority.JobDefinition sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.lifecycleState(), "lifecycle state");
            requireNotBlank(sample.targetScope(), "target scope");
            requireNotBlank(sample.requiredFields(), "required fields");
            requireNotBlank(sample.readinessSource(), "readiness source");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction job definition audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionJobDefinitionAuditSmoke() { }
}
