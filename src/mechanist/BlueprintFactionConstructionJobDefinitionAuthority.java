package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Definition-only contract for future faction construction jobs. */
final class BlueprintFactionConstructionJobDefinitionAuthority {
    record JobDefinition(String jobId, String factionName, String blueprintName, String lifecycleState,
                         String targetScope, String requiredFields, String readinessSource,
                         String boundaryLine) { }

    private BlueprintFactionConstructionJobDefinitionAuthority() { }

    static List<JobDefinition> sampleDefinitions() {
        ArrayList<JobDefinition> rows = new ArrayList<>();
        rows.add(definitionFor("job-storage-public", Faction.HIVER, BuildRecipe.storage(), "AUTHORIZED", "claimed room"));
        rows.add(definitionFor("job-sensor-restricted", Faction.CIVIC_WARDENS, BuildRecipe.securitySensorMast(), "BLOCKED", "security room"));
        rows.add(definitionFor("job-shop-public", Faction.NONE, BuildRecipe.shopCounter(), "REQUESTED", "market frontage"));
        return List.copyOf(rows);
    }

    static JobDefinition definitionFor(String jobId, Faction faction, BuildRecipe recipe, String lifecycleState, String targetScope) {
        String id = clean(jobId, "job-unassigned");
        Faction owner = faction == null ? Faction.NONE : faction;
        BuildRecipe safeRecipe = recipe == null ? BuildRecipe.storage() : recipe;
        String state = clean(lifecycleState, "REQUESTED").toUpperCase(java.util.Locale.ROOT);
        String scope = clean(targetScope, "unselected room");
        BlueprintPermissionReadinessAuthority.PermissionReadiness permission =
                BlueprintPermissionReadinessAuthority.evaluate(BlueprintAcquisitionPathAuthority.pathFor(safeRecipe),
                        true, true, true, true, true);
        BlueprintFactionConstructionCapabilityAuthority.FactionCapability capability =
                BlueprintFactionConstructionCapabilityAuthority.evaluate(safeRecipe, permission, true, true, true, true);
        String required = "jobId, faction, blueprint, target room or site, lifecycle state, permission readiness, capability readiness, material ledger, crew assignment, budget note, heat projection, and placement validation note";
        String source = "permission=" + permission.permissionClass()
                + ", capabilityCandidate=" + capability.factionCandidate()
                + ", blockers=" + (capability.blockers().isEmpty() ? "none" : String.join("; ", capability.blockers()));
        String boundary = id + " " + state + " " + safeRecipe.name
                + " for " + owner.label
                + " at " + scope
                + " uses " + source
                + "; definition only, no job queue mutation.";
        return new JobDefinition(id, owner.label, safeRecipe.name, state, scope, required, source, boundary);
    }

    static List<String> definitionAuditLines() {
        List<JobDefinition> samples = sampleDefinitions();
        int requested = 0;
        int authorized = 0;
        int blocked = 0;
        int requiredFields = 0;
        for (JobDefinition job : samples) {
            if ("REQUESTED".equals(job.lifecycleState())) requested++;
            if ("AUTHORIZED".equals(job.lifecycleState())) authorized++;
            if ("BLOCKED".equals(job.lifecycleState())) blocked++;
            if (job.requiredFields().contains("material ledger") && job.requiredFields().contains("placement validation note")) requiredFields++;
        }
        return List.of(
                "Blueprint faction construction job definition audit: owner=BlueprintFactionConstructionJobDefinitionAuthority, capabilityOwner=BlueprintFactionConstructionCapabilityAuthority, permissionOwner=BlueprintPermissionReadinessAuthority, stagedConstructionOwner=ProgressiveConstructionAuthority, readiness=definition-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction job lifecycle audit: supported states are REQUESTED, BLOCKED, AUTHORIZED, RESERVED, STAGED, IN_PROGRESS, COMPLETED, CANCELLED, and FAILED; sampleJobs=" + samples.size()
                        + ", requested=" + requested
                        + ", authorized=" + authorized
                        + ", blocked=" + blocked
                        + ", requiredFieldSamples=" + requiredFields + ".",
                "Blueprint faction construction job field audit: required fields include jobId, faction, blueprint, target room or site, lifecycle state, permission readiness, capability readiness, material ledger, crew assignment, budget note, heat projection, and placement validation note.",
                "Blueprint faction construction job sample audit: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction job rule: a future execution owner must reserve materials, bind crew, confirm room claim, re-run placement validation, and hand completion to staged construction before mutating the world.",
                "Blueprint faction construction job boundary: this audit does not create a live job queue, reserve or consume materials, assign workers, mutate room ownership, apply heat or suspicion, place objects, advance labor, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionJobDefinitionAuditSmoke checks job lifecycle states, required fields, sample definitions, future execution boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
