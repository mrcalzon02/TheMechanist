package mechanist;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Audit-only material reservation contract for future faction construction jobs. */
final class BlueprintFactionConstructionMaterialReservationAuthority {
    record MaterialReservation(String jobId, String blueprintName, String factionName,
                               String requiredLedger, String reservedLedger, String missingLedger,
                               boolean reservationReady, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionMaterialReservationAuthority() { }

    static List<MaterialReservation> sampleReservations() {
        ArrayList<MaterialReservation> rows = new ArrayList<>();
        rows.add(reservationFor("job-storage-public", Faction.HIVER, BuildRecipe.storage(),
                availability(4, 2, Map.of("Rivet set", 2))));
        rows.add(reservationFor("job-sensor-restricted", Faction.CIVIC_WARDENS, BuildRecipe.securitySensorMast(),
                availability(5, 1, Map.of("Sensor lens", 1, "Wire bundle", 1))));
        rows.add(reservationFor("job-shop-public", Faction.NONE, BuildRecipe.shopCounter(),
                availability(1, 0, Map.of("Warehouse inventory tag bundle", 1))));
        return List.copyOf(rows);
    }

    static MaterialReservation reservationFor(String jobId, Faction faction, BuildRecipe recipe,
                                              Map<String, Integer> availableMaterials) {
        String id = clean(jobId, "job-unassigned");
        Faction owner = faction == null ? Faction.NONE : faction;
        BuildRecipe safeRecipe = recipe == null ? BuildRecipe.storage() : recipe;
        LinkedHashMap<String, Integer> required = requirementsFor(safeRecipe);
        LinkedHashMap<String, Integer> available = normalize(availableMaterials);
        LinkedHashMap<String, Integer> reserved = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> missing = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            int need = Math.max(0, entry.getValue());
            int have = Math.max(0, available.getOrDefault(entry.getKey(), 0));
            int take = Math.min(need, have);
            if (take > 0) reserved.put(entry.getKey(), take);
            if (take < need) missing.put(entry.getKey(), need - take);
        }
        boolean ready = missing.isEmpty();
        String blockers = ready ? "none" : "missing " + ledger(missing);
        String boundary = id + " material reservation for " + safeRecipe.name
                + " by " + owner.label
                + " requires " + ledger(required)
                + "; reservedPreview=" + ledger(reserved)
                + "; blockers=" + blockers
                + "; audit only, no inventory mutation.";
        return new MaterialReservation(id, safeRecipe.name, owner.label, ledger(required), ledger(reserved),
                ledger(missing), ready, blockers, boundary);
    }

    static List<String> definitionAuditLines() {
        List<MaterialReservation> samples = sampleReservations();
        int ready = 0;
        int blocked = 0;
        int namedComponents = 0;
        for (MaterialReservation sample : samples) {
            if (sample.reservationReady()) ready++;
            else blocked++;
            if (sample.requiredLedger().contains("Rivet set") || sample.requiredLedger().contains("Sensor lens")
                    || sample.requiredLedger().contains("Warehouse inventory tag bundle")) namedComponents++;
        }
        return List.of(
                "Blueprint faction construction material reservation audit: owner=BlueprintFactionConstructionMaterialReservationAuthority, jobOwner=BlueprintFactionConstructionJobDefinitionAuthority, recipeOwner=BuildRecipe, stagedConstructionOwner=ProgressiveConstructionAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction material requirement audit: requirements include Construction supplies, Machine parts, and named component costs from BuildRecipe before a future job can reserve or stage materials.",
                "Blueprint faction construction material sample audit: sampleReservations=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", namedComponentSamples=" + namedComponents + ".",
                "Blueprint faction construction material examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction material rule: a future execution owner must reserve materials once, keep the reservation ledger separate from staged-site inserted materials, and release or transfer the reservation before completion or cancellation.",
                "Blueprint faction construction material boundary: this audit does not remove supplies, remove machine parts, remove named components, write reservation rows, stage materials into a site, assign crew, mutate room ownership, place objects, advance labor, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionMaterialReservationAuditSmoke checks requirement ledgers, sample reservation readiness, missing-material blockers, future execution boundaries, and raw-ID hiding."
        );
    }

    private static LinkedHashMap<String, Integer> requirementsFor(BuildRecipe recipe) {
        LinkedHashMap<String, Integer> out = new LinkedHashMap<>();
        if (recipe == null) return out;
        add(out, "Construction supplies", recipe.supplyCost);
        add(out, "Machine parts", recipe.partCost);
        if (recipe.componentCosts != null) {
            for (Map.Entry<String, Integer> entry : recipe.componentCosts.entrySet()) {
                if (entry == null) continue;
                add(out, entry.getKey(), entry.getValue() == null ? 0 : entry.getValue());
            }
        }
        return out;
    }

    private static LinkedHashMap<String, Integer> availability(int supplies, int parts, Map<String, Integer> components) {
        LinkedHashMap<String, Integer> out = new LinkedHashMap<>();
        add(out, "Construction supplies", supplies);
        add(out, "Machine parts", parts);
        if (components != null) {
            for (Map.Entry<String, Integer> entry : components.entrySet()) {
                if (entry == null) continue;
                add(out, entry.getKey(), entry.getValue() == null ? 0 : entry.getValue());
            }
        }
        return out;
    }

    private static LinkedHashMap<String, Integer> normalize(Map<String, Integer> values) {
        LinkedHashMap<String, Integer> out = new LinkedHashMap<>();
        if (values == null) return out;
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            if (entry == null) continue;
            add(out, entry.getKey(), entry.getValue() == null ? 0 : entry.getValue());
        }
        return out;
    }

    private static void add(Map<String, Integer> out, String item, int count) {
        if (out == null || item == null || item.isBlank() || count <= 0) return;
        String key = item.trim();
        out.put(key, out.getOrDefault(key, 0) + count);
    }

    private static String ledger(Map<String, Integer> values) {
        if (values == null || values.isEmpty()) return "none";
        ArrayList<String> rows = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null || entry.getValue() <= 0) continue;
            rows.add(entry.getKey() + " x" + entry.getValue());
        }
        return rows.isEmpty() ? "none" : String.join(", ", rows);
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
