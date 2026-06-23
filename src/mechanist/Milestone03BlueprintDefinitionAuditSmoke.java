package mechanist;

import java.util.List;

/** Smoke for the Phase 18 blueprint definition audit surface. */
final class Milestone03BlueprintDefinitionAuditSmoke {
    public static void main(String[] args) {
        List<String> audit = BlueprintConstructionAuthority.definitionAuditLines();
        requireContains(audit, "owner=BlueprintConstructionAuthority", "blueprint owner");
        requireContains(audit, "schema=room blueprint", "schema");
        requireContains(audit, "relativeCells=true", "relative cells");
        requireContains(audit, "anchors=true", "anchors");
        requireContains(audit, "objectMatrix=true", "object matrix");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-ID boundary");
        requireContains(audit, "blueprint quality is named on BuildRecipe construction previews", "quality boundary");
        requireContains(audit, "not an independent quality-upgrade system yet", "quality upgrade boundary");
        requireContains(audit, "supported cell kinds are floor, wall, door, machine, furniture", "cell kind coverage");
        requireContains(audit, "itemized components and labor turns", "recipe mapping");
        requireContains(audit, "buildable target tiles", "buildable preflight");
        requireContains(audit, "existing obstructions", "obstruction preflight");
        requireContains(audit, "unmined wall or rock", "rock preflight");
        requireContains(audit, "resource shortfalls", "resource preflight");
        requireContains(audit, "no-self-entombment exit warnings", "entombment warning preflight");
        requireContains(audit, "ghost placement is collisionless", "ghost boundary");
        requireContains(audit, "exit route must remain open", "exit route warning");
        requireContains(audit, "Hollow Box Test Room cells=20", "sample cell count");
        requireContains(audit, "anchors=1", "sample anchor count");
        requireContains(audit, "Construction supplies=20", "sample supplies cost");
        requireContains(audit, "Basic door kit=3", "sample door labor");
        requireContains(audit, "does not place objects, consume materials, mutate room ownership", "mutation boundary");
        requireContains(audit, "Milestone03BlueprintDefinitionAuditSmoke", "guard reference");
        requireContains(audit, "Milestone03BlueprintNoSelfEntombmentAuditSmoke", "entombment guard reference");
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint definition audit leaked implementation text: " + line);
            }
        }
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03BlueprintDefinitionAuditSmoke() { }
}
