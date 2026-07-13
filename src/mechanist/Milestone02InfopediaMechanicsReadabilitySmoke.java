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
        requireContains(rows, "Population and Market Pressure", "population market mechanic row");
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
        checkDetail("production-forecast", "Forge-Tutored Repair");
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
        checkDetail("production-forecast", "event log summarizes final quality");
        checkDetail("production-forecast", "main quality limiter");
        checkDetail("production-forecast", "shared machine-operation history preserves that summary");
        checkDetail("production-forecast", "shared machine-operation history");
        checkDetail("production-forecast", "forecast and Craft action use that operated machine for compatibility, condition, quality, fatigue, wear, provenance, and completion history");
        checkDetail("production-forecast", "Selecting another recipe keeps that machine-bound workbench context");
        checkDetail("production-forecast", "Operate and Craft on a built base machine open the same machine-bound workbench");
        checkDetail("production-forecast", "recipe list shows only known recipes compatible with that machine");
        checkDetail("production-forecast", "operate another machine or learn a matching recipe");
        checkDetail("production-forecast", "Use the crafting panel Status action or production_status for a base-wide live queue summary");
        checkDetail("production-forecast", "workbench or machine interaction Status action scopes queue counts, live operations, latest completion, and readiness to the machine being operated");
        checkDetail("production-forecast", "Use crafting panel History or production_history for recent base-wide records");
        checkDetail("production-forecast", "Each completed run consumes concrete inputs");
        checkDetail("production-forecast", "Staff Jobs opens the operated machine's generated-production setup");
        checkDetail("production-forecast", "Category, readiness, and page controls keep compatible known jobs reachable");
        checkDetail("production-forecast", "Assign validates access, doctrine, machine quality, and apparatus");
        checkDetail("production-forecast", "Worker selects the next valid recruit");
        checkDetail("production-forecast", "bounded 0 to 20 run queue");
        checkDetail("production-forecast", "advances in the background as ordinary game turns pass");
        checkDetail("production-forecast", "including while the player leaves the workbench");
        checkDetail("production-forecast", "Changing jobs clears the prior queue and current-run progress");
        checkDetail("production-forecast", "Materials cycles shortage handling");
        checkDetail("production-forecast", "pausing and releasing that worker");
        checkDetail("production-forecast", "cancelling the remaining queue");
        checkDetail("production-forecast", "nearest claimed-room faction container with available capacity");
        checkDetail("production-forecast", "interactable floor pile near the machine");
        checkDetail("production-forecast", "No Room cycles destination failure handling");
        checkDetail("production-forecast", "dumping output into the nearby floor pile");
        checkDetail("production-forecast", "Queue policies, progress, and the last blocker persist with the machine");
        checkDetail("production-forecast", "Production opens the base-wide production board");
        checkDetail("production-forecast", "prioritizes blocked and running machines");
        checkDetail("production-forecast", "without reopening every station");
        checkDetail("production-forecast", "Workbench opens that machine's staffed-job setup");
        checkDetail("production-forecast", "another available recruit never silently substitutes");
        checkDetail("production-forecast", "A blocked run does not consume inputs, add output, wear the machine, decrement the queue, or write completion history");
        checkDetail("production-forecast", "manual or staffed");
        checkDetail("production-forecast", "lawful, restricted, black-market, contraband");
        checkDetail("production-forecast", "generated recipe source");
        checkDetail("production-forecast", "Batch issue tags");
        checkDetail("production-forecast", "Repair history");
        checkDetail("population-markets", "semi-random availability turn");
        checkDetail("population-markets", "Receive Reinforcements");
        checkDetail("population-markets", "Reinforcement trains are free but slow");
        checkDetail("population-markets", "Barracks reserve musters are fastest");
        checkDetail("population-markets", "Paid local recruitment");
        checkDetail("population-markets", "Change Source");
        checkDetail("population-markets", "charges only for personnel who actually arrive");
        checkDetail("population-markets", "staffed housing or faction-room capacity is full");
        checkDetail("population-markets", "at least 24 floor tiles");
        checkDetail("population-markets", "maximum +25 faction boost at ten operating crèches");
        checkDetail("population-markets", "One care provider supports up to twelve children");
        checkDetail("population-markets", "each dense child bed unit holds four");
        checkDetail("population-markets", "abstract abandoned-or-ward cohort");
        checkDetail("population-markets", "recorded pregnancy reaches its due world turn");
        checkDetail("population-markets", "extra growth-food, clean-water, and pediatric-care market pressure");
        checkDetail("population-markets", "After sixteen full world years");
        checkDetail("population-markets", "Muster Cohort");
        checkDetail("skill-progression", "spends XP on durable capabilities");
        checkDetail("skill-progression", "Knowledge Tree");
        checkDetail("skill-progression", "Character Skills tab lists readable branches and nodes");
        checkDetail("skill-progression", "same validated spending path used by skill commands");
        checkDetail("skill-progression", "Qualifying specialist conversations offer Train");
        checkDetail("skill-progression", "temporary in-person access");
        checkDetail("skill-progression", "skill unlock controls");
        checkDetail("construction-blueprints", "complete catalog");
        checkDetail("construction-blueprints", "staged construction site instead of a finished facility");
        checkDetail("construction-blueprints", "Work on a staged site");
        checkDetail("construction-blueprints", "Work requires the selected staged site to still exist and be adjacent");
        checkDetail("construction-blueprints", "spends a turn only when the site actually changes");
        checkDetail("construction-blueprints", "Dismantle removes an adjacent unfinished staged site");
        checkDetail("construction-blueprints", "spends one turn when a site is removed");
        checkDetail("construction-blueprints", "construction dismantle command prefers the least-complete site first");
        checkDetail("construction-blueprints", "show the adjacent work target, its next action, and adjacent dismantle target before either command is run");
        checkDetail("construction-blueprints", "no work target is in reach and point to the nearest staged site");
        checkDetail("construction-blueprints", "no dismantle target is in reach and point to the nearest staged site");
        checkDetail("construction-blueprints", "it asks the player to stand adjacent and points to the nearest staged site");
        checkDetail("construction-blueprints", "command help for construction status, construction progress, and construction dismantle");
        checkDetail("construction-blueprints", "construction progress and construction status commands show the same staged-site packet");
        checkDetail("construction-blueprints", "That packet reports active staged-site count");
        checkDetail("construction-blueprints", "When no staged construction sites are waiting, that packet says there is no next construction action, no work target, and no dismantle target");
        checkDetail("construction-blueprints", "nearly complete and labor-ready work before material-blocked sites");
        checkDetail("construction-blueprints", "material-ready count");
        checkDetail("construction-blueprints", "in-work-reach count");
        checkDetail("construction-blueprints", "currently available to stage");
        checkDetail("construction-blueprints", "within work reach");
        checkDetail("construction-blueprints", "same priority as construction progress");
        checkDetail("construction-blueprints", "Construction work help names its 1 to 20 turn range, progress-priority target choice");
        checkDetail("construction-blueprints", "Construction work spends the actual productive work turns it uses");
        checkDetail("expansion-heat", "attention bands");
        checkDetail("interaction-approach", "never moves");
        checkDetail("contract-evidence", "carried, stored at base, or missing");
        checkDetail("contract-evidence", "skill and knowledge proof readiness");
        checkDetail("contract-evidence", "matching faction representative");
        checkDetail("contract-evidence", "Take Work");
        checkDetail("contract-evidence", "recorded production origin");
        checkDetail("contract-evidence", "blocked hand-in changes nothing");
        checkDetail("population-markets", "allocate basic food and water");
        checkDetail("population-markets", "changes both purchase prices and player sale value");
        checkDetail("population-markets", "local population target");
        checkDetail("population-markets", "Population identity matters as well as size");
        checkDetail("population-markets", "unstaffed site pauses production and exports");
        checkDetail("population-markets", "depleted sites contribute no free replacement item");
        checkDetail("population-markets", "finite persisted reserve");
        checkDetail("population-markets", "rail shipments or a small emergency allotment");
        checkDetail("population-markets", "reopened vendor sessions cannot recreate depleted stock");
        checkDetail("population-markets", "Universal waste runoff feeds sewer fertilizer");
        checkDetail("population-markets", "sewer markets pay a premium");
        checkDetail("transfer-workflows", "one item at a time");
        checkDetail("faction-personnel", "separate tracks");
        checkDetail("faction-personnel", "Machine workbench Staff Jobs can cycle to the next valid recruit");

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
