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

        checkDetail("look-examine", "Examine the selected visible target");
        checkDetail("movement-planning", "Movement target selected");
        checkDetail("context-prompts", "Generic:");
        checkDetail("body-condition", "immediate danger");
        checkDetail("medical-treatment", "explicit treatment command");
        checkDetail("inventory-equipment", "one selected unit at a time");
        checkDetail("production-forecast", "queued machine job");
        checkDetail("production-forecast", "quality-sensitive defect risk");
        checkDetail("production-forecast", "one machine part and one turn");
        checkDetail("production-forecast", "Novice, practiced, skilled, and expert operators");
        checkDetail("production-forecast", "matching carried units first");
        checkDetail("production-forecast", "Manual Craft remains player-operated");
        checkDetail("production-forecast", "Teach Machine");
        checkDetail("production-forecast", "another serviceable station");
        checkDetail("production-forecast", "one batch ID");
        checkDetail("production-forecast", "serviceable production stations");
        checkDetail("production-forecast", "40% ordinary-trader resale penalty");
        checkDetail("production-forecast", "equipped fabrication or repair tool");
        checkDetail("production-forecast", "Faction production mutation");
        checkDetail("production-forecast", "exhausted band");
        checkDetail("production-forecast", "producing room and claimed facility");
        checkDetail("production-forecast", "exact producing station");
        checkDetail("production-forecast", "immediate manual operator");
        checkDetail("production-forecast", "shared machine-operation history");
        checkDetail("production-forecast", "one queued run");
        checkDetail("production-forecast", "manual or staffed");
        checkDetail("production-forecast", "lawful, restricted, black-market, contraband");
        checkDetail("production-forecast", "generated recipe source");
        checkDetail("production-forecast", "Batch issue tags");
        checkDetail("production-forecast", "Repair history");
        checkDetail("skill-progression", "spends XP on durable capabilities");
        checkDetail("skill-progression", "Knowledge Tree");
        checkDetail("skill-progression", "skill unlock controls");
        checkDetail("construction-blueprints", "complete catalog");
        checkDetail("construction-blueprints", "staged construction site instead of a finished facility");
        checkDetail("construction-blueprints", "Work on a staged site");
        checkDetail("construction-blueprints", "Dismantle removes an unfinished staged site");
        checkDetail("construction-blueprints", "construction progress command reports active staged-site count");
        checkDetail("construction-blueprints", "nearly complete and labor-ready work before material-blocked sites");
        checkDetail("construction-blueprints", "material-ready count");
        checkDetail("construction-blueprints", "in-work-reach count");
        checkDetail("construction-blueprints", "currently available to stage");
        checkDetail("construction-blueprints", "within work reach");
        checkDetail("construction-blueprints", "same priority as construction progress");
        checkDetail("expansion-heat", "attention bands");
        checkDetail("interaction-approach", "never moves");
        checkDetail("contract-evidence", "carried, stored at base, or missing");
        checkDetail("contract-evidence", "skill and knowledge proof readiness");
        checkDetail("transfer-workflows", "one item at a time");
        checkDetail("faction-personnel", "separate tracks");

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
        for (String row : rows) {
            for (String line : SemanticAssetInfopediaAuthority.detailLines(null, row, null, "")) {
                rejectLeaks(line, "mechanic detail sweep");
                rejectInfopediaProcessLanguage(line, "mechanic detail sweep");
            }
        }
    }

    private static void checkDetail(String key, String expected) {
        List<String> lines = SemanticAssetInfopediaAuthority.mechanicDetailLinesByKey(key);
        requireContains(lines, expected, key + " detail expected text");
        for (String line : lines) {
            rejectLeaks(line, key + " detail");
            rejectInfopediaProcessLanguage(line, key + " detail");
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

    private static void rejectInfopediaProcessLanguage(String text, String label) {
        if (text == null) return;
        String l = text.toLowerCase();
        String[] forbidden = {
                "guard", "smoke", "milestone", "authority", "audit", "future",
                "owner=", "raw-id", "raw id", "migration note", "stage status"
        };
        for (String token : forbidden) {
            if (l.contains(token)) throw new AssertionError(label + " contains process language '" + token + "': " + text);
        }
    }

    private Milestone02InfopediaMechanicsReadabilitySmoke() { }
}
