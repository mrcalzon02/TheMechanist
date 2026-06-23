package mechanist;

import java.util.List;

/** Smoke for audit-only faction construction site readiness contracts. */
final class Milestone03BlueprintFactionConstructionSiteReadinessAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionSiteReadinessAuthority.SiteReadiness> samples =
                BlueprintFactionConstructionSiteReadinessAuthority.sampleReadiness();
        List<String> audit = BlueprintFactionConstructionSiteReadinessAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample site readiness rows");
        requireContains(audit, "owner=BlueprintFactionConstructionSiteReadinessAuthority", "site owner");
        requireContains(audit, "jobOwner=BlueprintFactionConstructionJobDefinitionAuthority", "job owner");
        requireContains(audit, "crewOwner=BlueprintFactionConstructionCrewAssignmentAuthority", "crew owner");
        requireContains(audit, "materialOwner=BlueprintFactionConstructionMaterialReservationAuthority", "material owner");
        requireContains(audit, "stagedConstructionOwner=ProgressiveConstructionAuthority", "staged owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "room claim, placement validation, access route, no-self-entombment exit path, utility readiness", "validation list");
        requireContains(audit, "sampleSites=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "exitGuardProfiles=3", "exit guard profiles");
        requireContains(audit, "utilityProfiles=1", "utility profiles");
        requireContains(audit, "job-storage-public site readiness for Storage Crate", "storage sample");
        requireContains(audit, "blockers=none", "ready blocker");
        requireContains(audit, "job-sensor-restricted site readiness for Security Sensor Mast", "sensor sample");
        requireContains(audit, "exit path would be unsafe", "exit blocker");
        requireContains(audit, "job-shop-public site readiness for Licensed Shop Counter", "shop sample");
        requireContains(audit, "room claim not ready", "room blocker");
        requireContains(audit, "reserve exactly one valid target site", "site reservation rule");
        requireContains(audit, "re-check placement immediately before staging", "pre-stage recheck");
        requireContains(audit, "does not claim rooms, reserve tiles, write ownership", "non-ownership boundary");
        requireContains(audit, "bypass no-self-entombment checks, create staged sites", "non-placement boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionSiteReadinessAuditSmoke", "guard reference");

        BlueprintFactionConstructionSiteReadinessAuthority.SiteReadiness direct =
                BlueprintFactionConstructionSiteReadinessAuthority.readinessFor("job-direct", Faction.MECHANICUS,
                        BuildRecipe.microForge(), "forge room", true, true, true, true, true);
        require(direct.siteReady(), "direct micro forge site should be ready");
        requireContains(direct.validationProfile(), "utility", "direct utility profile");
        requireContains(direct.validationProfile(), "heat-preview", "direct heat preview");
        requireContains(direct.boundaryLine(), "audit only, no room or tile mutation", "direct boundary");

        for (BlueprintFactionConstructionSiteReadinessAuthority.SiteReadiness sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.targetScope(), "target scope");
            requireNotBlank(sample.validationProfile(), "validation profile");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction site readiness audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionSiteReadinessAuditSmoke() { }
}
