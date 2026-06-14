package mechanist;

import java.util.List;

/** Smoke for Milestone 02 mechanic-reference entries in the Infopedia bridge. */
final class Milestone02InfopediaMechanicsReadabilitySmoke {
    public static void main(String[] args) {
        List<String> rows = SemanticAssetInfopediaAuthority.mechanicEntryRows("");
        requireContains(rows, "Look and Examine", "Look/Examine mechanic row");
        requireContains(rows, "Movement Planning", "movement mechanic row");
        requireContains(rows, "Context Prompts", "context prompt mechanic row");
        requireContains(rows, "Body Condition", "body condition mechanic row");
        requireContains(rows, "Medical Treatment Readiness", "medical mechanic row");
        requireContains(rows, "Inventory and Equipment", "inventory mechanic row");
        requireContains(rows, "Production Forecast", "production mechanic row");
        requireContains(rows, "Construction Blueprints", "construction mechanic row");
        requireContains(rows, "Expansion Heat", "expansion heat mechanic row");
        requireContains(rows, "Interaction Approach Planning", "approach mechanic row");
        requireContains(rows, "Contract Objectives and Evidence", "contract mechanic row");
        requireContains(rows, "Transfer Workflows", "transfer mechanic row");
        requireContains(rows, "Faction Personnel and Staffing", "faction personnel mechanic row");

        List<String> filtered = SemanticAssetInfopediaAuthority.mechanicEntryRows("ghost");
        requireContains(filtered, "Movement Planning", "movement filter row");

        checkDetail("look-examine", "Examine the selected visible target", "Milestone02LookExamineReadabilitySmoke");
        checkDetail("movement-planning", "Movement target selected", "Milestone02MovementPlanningReadabilitySmoke");
        checkDetail("context-prompts", "Generic:", "Milestone02ContextPromptReadabilitySmoke");
        checkDetail("body-condition", "immediate danger", "Milestone02BodyConditionReadabilitySmoke");
        checkDetail("medical-treatment", "explicit treatment command", "Milestone02MedicalTreatmentReadabilitySmoke");
        checkDetail("inventory-equipment", "one selected unit at a time", "Milestone02InventoryReadabilitySmoke");
        checkDetail("production-forecast", "queued machine job", "Milestone02ProductionReadabilitySmoke");
        checkDetail("production-forecast", "quality-sensitive defect risk", "Milestone03ProductionOutcomeForecastSmoke");
        checkDetail("production-forecast", "one machine part and one turn", "Milestone03MachineRepairWorkflowSmoke");
        checkDetail("production-forecast", "Manual operator skill", "Milestone03ProductionOperatorSkillSmoke");
        checkDetail("production-forecast", "matching carried units first", "Milestone03ProductionMaterialQualitySmoke");
        checkDetail("production-forecast", "manual Craft remains player-operated", "Milestone03ProductionWorkerQualitySmoke");
        checkDetail("production-forecast", "Teach Machine", "Milestone03MachineKnowledgeSourceSmoke");
        checkDetail("production-forecast", "one batch ID", "Milestone03ProductionBatchProvenanceSmoke");
        checkDetail("construction-blueprints", "complete catalog", "Milestone02ConstructionCategorySmoke");
        checkDetail("expansion-heat", "attention bands", "Milestone02ExpansionHeatReadabilitySmoke");
        checkDetail("interaction-approach", "never moves", "Milestone02InteractionApproachSmoke");
        checkDetail("contract-evidence", "carried, stored at base, or missing", "Milestone02ContractObjectiveReadabilitySmoke");
        checkDetail("transfer-workflows", "one item at a time", "Milestone02TransferWorkflowConsistencySmoke");
        checkDetail("faction-personnel", "separate tracks", "Milestone02FactionRosterReadabilitySmoke");

        List<String> healthFiltered = SemanticAssetInfopediaAuthority.mechanicEntryRows("bleeding");
        requireContains(healthFiltered, "Body Condition", "body condition filter row");
        requireContains(healthFiltered, "Medical Treatment Readiness", "medical filter row");

        List<String> detailFromRow = SemanticAssetInfopediaAuthority.detailLines(null, rows.get(0), null, "");
        requireContains(detailFromRow, "Reference:", "mechanic detail reference");
        requireContains(detailFromRow, "MECHANIC - Movement Planning", "mechanic related entry row");
        List<String> relatedRows = SemanticAssetInfopediaAuthority.relatedRowsForEntry(null, rows.get(0), null);
        requireContains(relatedRows, "Movement Planning", "structured related row");
        String firstRelated = SemanticAssetInfopediaAuthority.firstRelatedRowForEntry(null, rows.get(0), null)
                .orElseThrow(() -> new AssertionError("Expected first related mechanic row"));
        if (!firstRelated.startsWith("MECHANIC - ")) {
            throw new AssertionError("Related mechanic row should be navigable: " + firstRelated);
        }
        for (String line : detailFromRow) rejectLeaks(line, "mechanic detail from row");
    }

    private static void checkDetail(String key, String expected, String guardName) {
        List<String> lines = SemanticAssetInfopediaAuthority.mechanicDetailLinesByKey(key);
        requireContains(lines, expected, key + " detail expected text");
        requireContains(lines, guardName, key + " smoke guard name");
        for (String line : lines) {
            rejectLeaks(line, key + " detail");
            rejectContains(line, "targetZoneKey", key + " raw route key");
            rejectContains(line, "className", key + " raw class key");
        }
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) {
            if (line != null && line.contains(expected)) return;
        }
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void rejectContains(String text, String forbidden, String label) {
        if (text != null && text.contains(forbidden)) throw new AssertionError(label + ": " + text);
    }

    private static void rejectLeaks(String text, String label) {
        if (PlayerFacingText.containsLikelyLeak(text)) {
            throw new AssertionError("Player-facing leak in " + label + ": " + text);
        }
    }

    private Milestone02InfopediaMechanicsReadabilitySmoke() { }
}
