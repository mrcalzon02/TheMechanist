package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only room claim and placement-readiness contract for future faction construction jobs. */
final class BlueprintFactionConstructionSiteReadinessAuthority {
    record SiteReadiness(String jobId, String blueprintName, String factionName, String targetScope,
                         String validationProfile, boolean siteReady, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionSiteReadinessAuthority() { }

    static List<SiteReadiness> sampleReadiness() {
        ArrayList<SiteReadiness> rows = new ArrayList<>();
        rows.add(readinessFor("job-storage-public", Faction.HIVER, BuildRecipe.storage(),
                "claimed storage room", true, true, true, true, true));
        rows.add(readinessFor("job-sensor-restricted", Faction.CIVIC_WARDENS, BuildRecipe.securitySensorMast(),
                "security room", true, true, true, false, true));
        rows.add(readinessFor("job-shop-public", Faction.NONE, BuildRecipe.shopCounter(),
                "market frontage", false, true, true, true, true));
        return List.copyOf(rows);
    }

    static SiteReadiness readinessFor(String jobId, Faction faction, BuildRecipe recipe, String targetScope,
                                      boolean roomClaimReady, boolean placementReady, boolean accessReady,
                                      boolean exitPathReady, boolean utilityReady) {
        String id = clean(jobId, "job-unassigned");
        Faction owner = faction == null ? Faction.NONE : faction;
        BuildRecipe safeRecipe = recipe == null ? BuildRecipe.storage() : recipe;
        String scope = clean(targetScope, "unselected room");
        ArrayList<String> blockers = new ArrayList<>();
        if (!roomClaimReady) blockers.add("room claim not ready");
        if (!placementReady) blockers.add("placement validation blocked");
        if (!accessReady) blockers.add("access route not ready");
        if (!exitPathReady) blockers.add("exit path would be unsafe");
        if (!utilityReady) blockers.add("utility readiness blocked");
        boolean ready = blockers.isEmpty();
        String profile = validationProfileFor(safeRecipe);
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " site readiness for " + safeRecipe.name
                + " by " + owner.label
                + " at " + scope
                + " uses " + profile
                + "; blockers=" + blockerLine
                + "; audit only, no room or tile mutation.";
        return new SiteReadiness(id, safeRecipe.name, owner.label, scope, profile, ready, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<SiteReadiness> samples = sampleReadiness();
        int ready = 0;
        int blocked = 0;
        int exitGuards = 0;
        int utilityProfiles = 0;
        for (SiteReadiness sample : samples) {
            if (sample.siteReady()) ready++;
            else blocked++;
            if (sample.validationProfile().contains("no-self-entombment")) exitGuards++;
            if (sample.validationProfile().contains("utility")) utilityProfiles++;
        }
        return List.of(
                "Blueprint faction construction site readiness audit: owner=BlueprintFactionConstructionSiteReadinessAuthority, jobOwner=BlueprintFactionConstructionJobDefinitionAuthority, crewOwner=BlueprintFactionConstructionCrewAssignmentAuthority, materialOwner=BlueprintFactionConstructionMaterialReservationAuthority, stagedConstructionOwner=ProgressiveConstructionAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction site validation audit: future jobs must confirm room claim, placement validation, access route, no-self-entombment exit path, utility readiness, and final staged-construction handoff before mutating the world.",
                "Blueprint faction construction site sample audit: sampleSites=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", exitGuardProfiles=" + exitGuards
                        + ", utilityProfiles=" + utilityProfiles + ".",
                "Blueprint faction construction site examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction site rule: a future execution owner must reserve exactly one valid target site, re-check placement immediately before staging, and release the site on cancellation or failure.",
                "Blueprint faction construction site boundary: this audit does not claim rooms, reserve tiles, write ownership, bypass placement validation, bypass no-self-entombment checks, create staged sites, apply utilities, assign crew, remove materials, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionSiteReadinessAuditSmoke checks room claim readiness, placement and exit-path blockers, utility profiles, future execution boundaries, and raw-ID hiding."
        );
    }

    private static String validationProfileFor(BuildRecipe recipe) {
        if (recipe == null) return "placement+access+no-self-entombment";
        String category = ConstructionCategoryAuthority.categoryFor(recipe);
        ArrayList<String> tags = new ArrayList<>();
        tags.add("room-claim");
        tags.add("placement");
        tags.add("access-route");
        tags.add("no-self-entombment");
        String text = (recipe.name + " " + recipe.description + " " + category).toLowerCase(java.util.Locale.ROOT);
        if (recipe.requiresWorkbench || containsAny(text, "power", "water", "utility", "condenser", "turret", "sensor", "lab", "clinic")) {
            tags.add("utility");
        }
        if (recipe.attention > 0 || recipe.requiredFaction != null && recipe.requiredFaction != Faction.NONE) {
            tags.add("heat-preview");
        }
        return String.join("+", tags);
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
