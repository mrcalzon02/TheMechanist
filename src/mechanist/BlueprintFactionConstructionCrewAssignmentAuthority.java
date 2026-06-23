package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only crew assignment contract for future faction construction jobs. */
final class BlueprintFactionConstructionCrewAssignmentAuthority {
    record CrewAssignment(String jobId, String blueprintName, String factionName, String crewProfile,
                          int requiredCrew, int assignedCrew, int requiredLaborTurns,
                          boolean crewReady, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionCrewAssignmentAuthority() { }

    static List<CrewAssignment> sampleAssignments() {
        ArrayList<CrewAssignment> rows = new ArrayList<>();
        rows.add(assignmentFor("job-storage-public", Faction.HIVER, BuildRecipe.storage(), 1));
        rows.add(assignmentFor("job-sensor-restricted", Faction.CIVIC_WARDENS, BuildRecipe.securitySensorMast(), 2));
        rows.add(assignmentFor("job-shop-public", Faction.NONE, BuildRecipe.shopCounter(), 0));
        return List.copyOf(rows);
    }

    static CrewAssignment assignmentFor(String jobId, Faction faction, BuildRecipe recipe, int assignedCrew) {
        String id = clean(jobId, "job-unassigned");
        Faction owner = faction == null ? Faction.NONE : faction;
        BuildRecipe safeRecipe = recipe == null ? BuildRecipe.storage() : recipe;
        int requiredCrew = requiredCrewFor(safeRecipe);
        int assigned = Math.max(0, assignedCrew);
        int labor = Math.max(1, safeRecipe.baseTurns);
        boolean ready = assigned >= requiredCrew;
        String profile = crewProfileFor(safeRecipe);
        String blockers = ready ? "none" : "needs " + Math.max(0, requiredCrew - assigned) + " more crew";
        String boundary = id + " crew assignment for " + safeRecipe.name
                + " by " + owner.label
                + " requires " + requiredCrew + " " + profile
                + " crew and " + labor + " labor turn(s)"
                + "; assignedPreview=" + assigned
                + "; blockers=" + blockers
                + "; audit only, no worker dispatch.";
        return new CrewAssignment(id, safeRecipe.name, owner.label, profile, requiredCrew, assigned, labor, ready, blockers, boundary);
    }

    static List<String> definitionAuditLines() {
        List<CrewAssignment> samples = sampleAssignments();
        int ready = 0;
        int blocked = 0;
        int specialist = 0;
        int security = 0;
        for (CrewAssignment assignment : samples) {
            if (assignment.crewReady()) ready++;
            else blocked++;
            if (assignment.crewProfile().contains("specialist")) specialist++;
            if (assignment.crewProfile().contains("security")) security++;
        }
        return List.of(
                "Blueprint faction construction crew assignment audit: owner=BlueprintFactionConstructionCrewAssignmentAuthority, jobOwner=BlueprintFactionConstructionJobDefinitionAuthority, materialOwner=BlueprintFactionConstructionMaterialReservationAuthority, stagedConstructionOwner=ProgressiveConstructionAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction crew requirement audit: crew requirements derive from BuildRecipe workbench, faction restriction, attention, construction category, and base labor turns before a future job can bind workers.",
                "Blueprint faction construction crew sample audit: sampleAssignments=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", specialistProfiles=" + specialist
                        + ", securityProfiles=" + security + ".",
                "Blueprint faction construction crew examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction crew rule: a future execution owner must bind named available workers, keep crew assignment separate from material reservation, and release crew on cancellation or completion.",
                "Blueprint faction construction crew boundary: this audit does not assign recruits, move NPCs, reserve workers, create schedules, mutate room ownership, remove materials, place objects, advance labor, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionCrewAssignmentAuditSmoke checks crew profiles, sample readiness, labor requirements, future execution boundaries, and raw-ID hiding."
        );
    }

    private static int requiredCrewFor(BuildRecipe recipe) {
        if (recipe == null) return 1;
        int crew = 1;
        if (recipe.requiresWorkbench) crew++;
        if (recipe.requiredFaction != null && recipe.requiredFaction != Faction.NONE) crew++;
        if (recipe.attention >= 4) crew++;
        if (recipe.baseTurns >= 12) crew++;
        return Math.max(1, Math.min(5, crew));
    }

    private static String crewProfileFor(BuildRecipe recipe) {
        if (recipe == null) return "general construction";
        String text = (recipe.name + " " + recipe.description + " " + ConstructionCategoryAuthority.categoryFor(recipe)).toLowerCase(java.util.Locale.ROOT);
        ArrayList<String> tags = new ArrayList<>();
        if (recipe.requiredFaction != null && recipe.requiredFaction != Faction.NONE) tags.add("faction-cleared");
        if (recipe.requiresWorkbench || recipe.reqMechanics >= 4 || recipe.reqIntellect >= 4) tags.add("specialist");
        if (containsAny(text, "turret", "security", "sensor", "defense", "guard", "barricade", "door")) tags.add("security");
        if (containsAny(text, "shop", "counter", "commerce", "clinic", "medical", "laboratory", "logistics")) tags.add("facility");
        if (tags.isEmpty()) tags.add("general construction");
        return String.join(" ", tags);
    }

    private static boolean containsAny(String text, String... needles) {
        if (text == null) return false;
        for (String needle : needles) if (needle != null && text.contains(needle)) return true;
        return false;
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
