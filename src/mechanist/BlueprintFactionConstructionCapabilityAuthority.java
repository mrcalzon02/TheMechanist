package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Audit-only faction construction capability layer for existing build recipes. */
final class BlueprintFactionConstructionCapabilityAuthority {
    record FactionCapability(String blueprintName, String constructionCategory, String factionUseStatus,
                             String permissionClass, boolean factionCandidate, List<String> blockers,
                             String capabilityLine) { }

    private BlueprintFactionConstructionCapabilityAuthority() { }

    static List<FactionCapability> catalogCapabilities() {
        ArrayList<FactionCapability> rows = new ArrayList<>();
        for (BuildRecipe recipe : BuildRecipe.allBuildRecipes()) {
            if (recipe == null) continue;
            BlueprintAcquisitionPathAuthority.AcquisitionPath path = BlueprintAcquisitionPathAuthority.pathFor(recipe);
            BlueprintPermissionReadinessAuthority.PermissionReadiness permission =
                    BlueprintPermissionReadinessAuthority.evaluate(path, true, true, true, true, true);
            rows.add(evaluate(recipe, permission, true, true, true, true));
        }
        return List.copyOf(rows);
    }

    static FactionCapability evaluate(BuildRecipe recipe,
                                      BlueprintPermissionReadinessAuthority.PermissionReadiness permission,
                                      boolean factionBudgetReady,
                                      boolean crewReady,
                                      boolean roomClaimReady,
                                      boolean materialsReady) {
        String name = recipe == null ? "Unknown Blueprint" : safe(recipe.name, "Unknown Blueprint");
        String category = recipe == null ? "Unknown" : safe(ConstructionCategoryAuthority.categoryFor(recipe), "Unknown");
        String factionUse = RoomConstructionParityAuthority.factionUseStatusFor(recipe);
        String permissionClass = permission == null ? "unknown" : safe(permission.permissionClass(), "unknown");
        boolean factionCandidate = isFactionCandidate(recipe, category, factionUse);
        ArrayList<String> blockers = new ArrayList<>();
        if (!factionCandidate) blockers.add("not marked faction construction capable");
        if (permission != null && !permission.permissionReady()) blockers.add("permission readiness blocked");
        if (!factionBudgetReady) blockers.add("faction budget not ready");
        if (!crewReady) blockers.add("construction crew not ready");
        if (!roomClaimReady) blockers.add("room claim not ready");
        if (!materialsReady) blockers.add("construction materials not ready");
        boolean ready = blockers.isEmpty();
        String line = name + " faction construction " + (ready ? "capable" : "blocked")
                + ": category=" + category
                + ", permission=" + permissionClass
                + ", factionUse=" + factionUse
                + ", blockers=" + (ready ? "none" : String.join("; ", blockers)) + ".";
        return new FactionCapability(name, category, factionUse, permissionClass, factionCandidate, List.copyOf(blockers), line);
    }

    static List<String> definitionAuditLines() {
        List<FactionCapability> rows = catalogCapabilities();
        int candidates = 0;
        int publicCandidates = 0;
        int restrictedCandidates = 0;
        int factionStandingCandidates = 0;
        int defensiveCandidates = 0;
        int facilityCandidates = 0;
        for (FactionCapability row : rows) {
            if (!row.factionCandidate()) continue;
            candidates++;
            if ("public-ready".equals(row.permissionClass())) publicCandidates++;
            if ("restricted-legal-access".equals(row.permissionClass())) restrictedCandidates++;
            if ("faction-standing".equals(row.permissionClass())) factionStandingCandidates++;
            String text = (row.blueprintName() + " " + row.constructionCategory()).toLowerCase(Locale.ROOT);
            if (containsAny(text, "defense", "turret", "sensor", "wall", "door", "barricade")) defensiveCandidates++;
            if (containsAny(text, "machine", "utility", "medical", "commerce", "laboratory", "logistics", "storage")) facilityCandidates++;
        }
        FactionCapability publicStorage = evaluate(BuildRecipe.storage(),
                readinessFor(BuildRecipe.storage(), true, true, true, true, true),
                true, true, true, true);
        FactionCapability blockedSensor = evaluate(BuildRecipe.securitySensorMast(),
                readinessFor(BuildRecipe.securitySensorMast(), true, true, true, true, false),
                true, true, true, true);
        FactionCapability blockedShop = evaluate(BuildRecipe.shopCounter(),
                readinessFor(BuildRecipe.shopCounter(), true, true, true, true, true),
                false, false, true, true);
        return List.of(
                "Blueprint faction construction capability audit: owner=BlueprintFactionConstructionCapabilityAuthority, blueprintOwner=BuildRecipe, parityOwner=RoomConstructionParityAuthority, permissionOwner=BlueprintPermissionReadinessAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction catalog audit: blueprintCapabilityRows=" + rows.size()
                        + ", factionCandidates=" + candidates
                        + ", publicCandidates=" + publicCandidates
                        + ", restrictedCandidates=" + restrictedCandidates
                        + ", factionStandingCandidates=" + factionStandingCandidates
                        + ", defensiveCandidates=" + defensiveCandidates
                        + ", facilityCandidates=" + facilityCandidates + ".",
                "Blueprint faction construction readiness audit: candidate readiness considers faction-use parity, permission readiness, faction budget, construction crew, room claim, and construction materials before any faction job could be scheduled.",
                "Blueprint faction construction sample audit: " + publicStorage.capabilityLine()
                        + " | " + blockedSensor.capabilityLine()
                        + " | " + blockedShop.capabilityLine(),
                "Blueprint faction construction rule: public or faction-approved blueprints may be capability candidates, but capability is only a planning signal until a later job owner schedules crew, claims a room, reserves materials, and commits construction.",
                "Blueprint faction construction boundary: this audit does not spawn faction construction jobs, mutate room ownership, reserve or consume materials, spend faction budget, grant permits, apply heat or suspicion, bypass placement validation, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionCapabilityAuditSmoke checks faction candidate coverage, readiness blockers, sample capability lines, future-owner boundaries, and raw-ID hiding."
        );
    }

    private static BlueprintPermissionReadinessAuthority.PermissionReadiness readinessFor(
            BuildRecipe recipe, boolean owned, boolean reputation, boolean license, boolean permit, boolean legalAccess) {
        return BlueprintPermissionReadinessAuthority.evaluate(BlueprintAcquisitionPathAuthority.pathFor(recipe),
                owned, reputation, license, permit, legalAccess);
    }

    private static boolean isFactionCandidate(BuildRecipe recipe, String category, String factionUse) {
        if (recipe == null) return false;
        Faction faction = FactionInventoryStockAuthority.normalizeFaction(recipe.requiredFaction);
        String text = text(recipe.name, recipe.description, category, factionUse);
        if (faction != Faction.NONE) return true;
        if (factionUse != null && factionUse.contains("faction usable")) return true;
        return containsAny(text, "defense", "turret", "sensor", "wall", "door", "barricade", "storage",
                "warehouse", "clinic", "medical", "laboratory", "bench", "forge", "shop", "counter",
                "logistics", "supply", "water", "utility");
    }

    private static boolean containsAny(String text, String... needles) {
        if (text == null) return false;
        for (String needle : needles) if (needle != null && text.contains(needle)) return true;
        return false;
    }

    private static String text(String... values) {
        StringBuilder out = new StringBuilder();
        if (values != null) for (String value : values) if (value != null) out.append(value).append(' ');
        return out.toString().toLowerCase(Locale.ROOT);
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
