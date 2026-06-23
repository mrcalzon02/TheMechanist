package mechanist;

import java.util.List;

/** Smoke for audit-only faction construction crew assignment contracts. */
final class Milestone03BlueprintFactionConstructionCrewAssignmentAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionCrewAssignmentAuthority.CrewAssignment> samples =
                BlueprintFactionConstructionCrewAssignmentAuthority.sampleAssignments();
        List<String> audit = BlueprintFactionConstructionCrewAssignmentAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample crew assignments");
        requireContains(audit, "owner=BlueprintFactionConstructionCrewAssignmentAuthority", "crew owner");
        requireContains(audit, "jobOwner=BlueprintFactionConstructionJobDefinitionAuthority", "job owner");
        requireContains(audit, "materialOwner=BlueprintFactionConstructionMaterialReservationAuthority", "material owner");
        requireContains(audit, "stagedConstructionOwner=ProgressiveConstructionAuthority", "staged construction owner");
        requireContains(audit, "readiness=audit-only", "audit-only boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-ID boundary");
        requireContains(audit, "workbench, faction restriction, attention, construction category, and base labor turns", "requirement sources");
        requireContains(audit, "sampleAssignments=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "specialistProfiles=1", "specialist count");
        requireContains(audit, "securityProfiles=1", "security count");
        requireContains(audit, "job-storage-public crew assignment for Storage Crate", "storage sample");
        requireContains(audit, "requires 1 general construction crew and 3 labor turn", "storage labor");
        requireContains(audit, "job-sensor-restricted crew assignment for Security Sensor Mast", "sensor sample");
        requireContains(audit, "requires 3 faction-cleared specialist security crew and 10 labor turn", "sensor labor");
        requireContains(audit, "needs 1 more crew", "sensor blocker");
        requireContains(audit, "job-shop-public crew assignment for Licensed Shop Counter", "shop sample");
        requireContains(audit, "requires 1 facility crew and 7 labor turn", "shop labor");
        requireContains(audit, "needs 1 more crew", "shop blocker");
        requireContains(audit, "bind named available workers", "future binding rule");
        requireContains(audit, "separate from material reservation", "ledger separation rule");
        requireContains(audit, "does not assign recruits, move NPCs, reserve workers, create schedules", "non-dispatch boundary");
        requireContains(audit, "remove materials, place objects, advance labor, or complete construction", "effect boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionCrewAssignmentAuditSmoke", "guard reference");

        BlueprintFactionConstructionCrewAssignmentAuthority.CrewAssignment direct =
                BlueprintFactionConstructionCrewAssignmentAuthority.assignmentFor("job-direct", Faction.MECHANICUS,
                        BuildRecipe.microForge(), 3);
        require(direct.crewReady(), "direct micro forge crew should be ready");
        require(direct.requiredCrew() == 3, "micro forge should require three crew");
        require(direct.requiredLaborTurns() == 10, "micro forge should preserve base labor turns");
        requireContains(direct.crewProfile(), "faction-cleared", "direct faction crew");
        requireContains(direct.crewProfile(), "specialist", "direct specialist crew");
        requireContains(direct.boundaryLine(), "audit only, no worker dispatch", "direct boundary");

        for (BlueprintFactionConstructionCrewAssignmentAuthority.CrewAssignment sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.crewProfile(), "crew profile");
            require(sample.requiredCrew() > 0, "required crew should be positive");
            require(sample.requiredLaborTurns() > 0, "required labor should be positive");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction crew assignment audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionCrewAssignmentAuditSmoke() { }
}
