package mechanist;

import java.util.List;

/** Smoke for audit-only faction construction cancellation and release contracts. */
final class Milestone03BlueprintFactionConstructionCancellationReleaseAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionCancellationReleaseAuthority.ReleasePlan> samples =
                BlueprintFactionConstructionCancellationReleaseAuthority.sampleReleasePlans();
        List<String> audit = BlueprintFactionConstructionCancellationReleaseAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample release plans");
        requireContains(audit, "owner=BlueprintFactionConstructionCancellationReleaseAuthority", "release owner");
        requireContains(audit, "jobOwner=BlueprintFactionConstructionJobDefinitionAuthority", "job owner");
        requireContains(audit, "siteOwner=BlueprintFactionConstructionSiteReadinessAuthority", "site owner");
        requireContains(audit, "crewOwner=BlueprintFactionConstructionCrewAssignmentAuthority", "crew owner");
        requireContains(audit, "materialOwner=BlueprintFactionConstructionMaterialReservationAuthority", "material owner");
        requireContains(audit, "budgetOwner=BlueprintFactionConstructionBudgetHeatAuthorizationAuthority", "budget owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "record a reason and release site, crew, materials, budget hold, and attention preview", "release inputs");
        requireContains(audit, "sampleReleases=3", "sample count");
        requireContains(audit, "ready=2", "ready count");
        requireContains(audit, "blocked=1", "blocked count");
        requireContains(audit, "cancelled=2", "cancelled count");
        requireContains(audit, "failed=1", "failed count");
        requireContains(audit, "job-storage-public CANCELLED release for Storage Crate", "storage release");
        requireContains(audit, "player cancelled before staging", "storage reason");
        requireContains(audit, "job-sensor-restricted FAILED release for Security Sensor Mast", "sensor release");
        requireContains(audit, "heat authorization expired", "sensor reason");
        requireContains(audit, "job-shop-public CANCELLED release for Licensed Shop Counter", "shop release");
        requireContains(audit, "missing cancellation reason", "shop blocker");
        requireContains(audit, "reverse order of staging", "unwind rule");
        requireContains(audit, "does not cancel live jobs, release live reservations", "non-mutation boundary");
        requireContains(audit, "refund budget, mutate heat, mutate suspicion", "attention boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionCancellationReleaseAuditSmoke", "guard reference");

        BlueprintFactionConstructionCancellationReleaseAuthority.ReleasePlan direct =
                BlueprintFactionConstructionCancellationReleaseAuthority.releaseFor("job-direct", Faction.MECHANICUS,
                        BuildRecipe.microForge(), "FAILED", "site validation failed before staging",
                        true, true, true, true, true);
        require(direct.releaseReady(), "direct release should be ready");
        requireContains(direct.boundaryLine(), "audit only, no reservation mutation", "direct boundary");

        for (BlueprintFactionConstructionCancellationReleaseAuthority.ReleasePlan sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.lifecycleState(), "lifecycle state");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
            require(sample.siteReleaseRequired(), "site release should be declared in every sample");
            require(sample.attentionReleaseRequired(), "attention release should be declared in every sample");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction release audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionCancellationReleaseAuditSmoke() { }
}
