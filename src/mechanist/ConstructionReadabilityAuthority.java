package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ConstructionReadabilityAuthority {
    private ConstructionReadabilityAuthority() {}

    static List<String> detailLines(GamePanel game, BuildRecipe recipe, int x, int y) {
        if (recipe == null) return List.of("Select a blueprint to begin placement.");
        String placement = game == null ? "live placement state is unavailable." : game.rawCanPlacePendingBuildAt(x, y);
        ArrayList<String> componentLines = new ArrayList<>();
        for (Map.Entry<String, Integer> component : recipe.componentCosts.entrySet()) {
            int have = game == null ? 0 : game.countProductionInput(component.getKey());
            componentLines.add(component.getKey() + " " + have + "/" + component.getValue());
        }
        return preview(recipe, game == null ? 0 : game.supplies, game == null ? 0 : game.machineParts,
                x, y, placement, componentLines);
    }

    static List<String> preview(BuildRecipe recipe, int supplies, int machineParts, int x, int y,
                                String placementResult, List<String> componentLines) {
        ArrayList<String> lines = new ArrayList<>();
        if (recipe == null) {
            lines.add("Select a blueprint to begin placement.");
            return lines;
        }
        boolean ready = placementResult != null && placementResult.equalsIgnoreCase("ok");
        boolean stagedStart = placementResult != null && placementResult.toLowerCase().startsWith("staged start:");
        lines.add(ready ? "Placement: READY at " + x + "," + y + "."
                : stagedStart ? "Placement: STAGED START at " + x + "," + y + " - " + safe(placementResult.substring("staged start:".length()).trim(), "partial materials available") + "."
                : "Placement: BLOCKED at " + x + "," + y + " - " + safe(placementResult, "unknown reason") + ".");
        lines.add("Blueprint: " + recipe.name + " / " + safe(recipe.qualityName, "Common") + ".");
        lines.add("Cost: supplies " + Math.max(0, supplies) + "/" + Math.max(0, recipe.supplyCost)
                + "; machine parts " + Math.max(0, machineParts) + "/" + Math.max(0, recipe.partCost) + ".");
        if (componentLines == null || componentLines.isEmpty()) lines.add("Components: no named components required.");
        else for (String component : componentLines) lines.add("Component: " + component + ".");
        lines.add("Requirements: " + (recipe.requiresWorkbench ? "Scrap Workbench; " : "no workbench; ")
                + "knowledge " + safe(recipe.requiredKnowledge, "none") + "; faction "
                + (recipe.requiredFaction == null || recipe.requiredFaction == Faction.NONE ? "none" : recipe.requiredFaction.label) + ".");
        lines.add("Access: owning the blueprint is separate from permission, reputation, license, permit, materials, workbench, knowledge, placement access, utilities, and labor.");
        lines.add("Placement consequence: consumes the listed materials into a staged construction site; labor completion finishes the permanent base object.");
        lines.add("Purpose: " + safe(recipe.description, "No description recorded."));
        return lines;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
