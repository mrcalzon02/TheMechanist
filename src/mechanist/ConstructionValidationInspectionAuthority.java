package mechanist;

import java.util.*;

/**
 * 0.9.09f construction validation inspection/debug bridge.
 *
 * Keeps placement diagnostics player-visible and developer-auditable without
 * running any background scans.  All data is read from the existing
 * placement-only feedback/access authorities and the currently selected build.
 */
final class ConstructionValidationInspectionAuthority {
    static final String VERSION = "0.9.10ep";

    private ConstructionValidationInspectionAuthority() {}

    static ArrayList<String> buildInspectionLines(GamePanel g) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Construction validation inspection " + VERSION);
        lines.add("Scope: explains the current build cursor and recent placement checks. No map-wide scan is performed.");
        if (g == null) {
            lines.add("No active game panel context.");
            return lines;
        }
        BuildRecipe r = g.pendingBuildRecipe;
        if (r == null) {
            lines.add("Selected build: none.");
            lines.add("Open BUILD, choose an object, then inspect validation again.");
        } else {
            String raw = g.rawCanPlacePendingBuildAt(g.buildX, g.buildY);
            String finalResult = g.constructionPlacementResult(r, g.buildX, g.buildY, raw);
            lines.add("Selected build: " + safe(r.name) + " [" + r.symbol + "] at " + g.buildX + "," + g.buildY + ".");
            lines.add("Blueprint family: " + g.constructionBlueprintFor(r) + ".");
            lines.add("Raw placement: " + ConstructionPlacementFeedbackAuthority.shortReason(raw));
            lines.add("Governed result: " + ConstructionPlacementFeedbackAuthority.shortReason(finalResult));
            lines.add("Blocks access class: " + (SelfEntombmentConstructionAuthority.placementBlocksAccess(r) ? "yes" : "no") + ".");
            lines.add("Component requirement: " + g.buildComponentRequirementProblem(r));
            lines.add("Build requirement: " + g.buildRequirementProblem(r));
        }
        lines.add(ConstructionPlacementFeedbackAuthority.auditSummary());
        lines.add(SelfEntombmentConstructionAuthority.auditSummary());
        lines.addAll(EconomicTopologyPreviewConsumerAuthority.constructionPreviewLines(g));
        lines.add("Recent placement checks:");
        ArrayList<String> recent = ConstructionPlacementFeedbackAuthority.recentLines();
        if (recent.isEmpty()) lines.add("- none recorded yet this session.");
        else {
            int start = Math.max(0, recent.size() - 8);
            for (int i = start; i < recent.size(); i++) lines.add("- " + recent.get(i));
        }
        lines.add("Efficiency rule: checks run on cursor/confirmation/inspection only; rendering and turn advancement do not run BFS path validation.");
        return lines;
    }

    static String compactLine(GamePanel g) {
        if (g == null || g.pendingBuildRecipe == null) return "Validation: no selected build. Select a construction object to inspect placement safety.";
        String raw = g.rawCanPlacePendingBuildAt(g.buildX, g.buildY);
        String label = "OK".equals(raw) ? "valid" : "blocked";
        return "Validation: " + label + " | " + ConstructionPlacementFeedbackAuthority.shortReason(raw) + " | " + EconomicTopologyPreviewConsumerAuthority.constructionCompactLine(g);
    }

    static String auditSummary() {
        return "constructionValidationInspection version=" + VERSION + " mode=on-demand display=build-panel+infopedia topologyPreview=" + EconomicTopologyPreviewConsumerAuthority.VERSION + " no-background-scan";
    }

    private static String safe(String s) {
        return s == null || s.trim().isEmpty() ? "Unnamed build" : s.trim();
    }
}
