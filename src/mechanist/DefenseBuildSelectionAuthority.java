package mechanist;

import java.util.*;

/**
 * Defense build selection authority.
 *
 * Bridges semantic defense profiles into the player-facing build menu and
 * Infopedia without making defensive combat autonomous. It performs data lookup,
 * recipe grouping, and readable status reporting only: no map scanning,
 * targeting, projectile logic, or per-frame work.
 */
final class DefenseBuildSelectionAuthority {
    static final String VERSION = "0.9.09b";

    static ArrayList<BuildRecipe> buildRecipes() {
        ArrayList<BuildRecipe> out = new ArrayList<>();
        out.add(BuildRecipe.sandbagLine());
        out.add(BuildRecipe.razorWireCoil());
        out.add(BuildRecipe.reinforcedWallPanel());
        out.add(BuildRecipe.arbitesReinforcedDoor());
        out.add(BuildRecipe.securitySensorMast());
        out.add(BuildRecipe.lightStubTurret());
        out.add(BuildRecipe.heavyStubTurret());
        out.add(BuildRecipe.arbitesSuppressionTurret());
        out.add(BuildRecipe.gildedSentryTurret());
        out.add(BuildRecipe.precinctDefensiveFixtureSet());
        return out;
    }

    static ArrayList<String> buildSurfaceLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Defense build surface " + VERSION + ": semantic defenses are build-selectable and inspectable; combat automation remains disabled.");
        for (BuildRecipe r : buildRecipes()) {
            DefenseSemanticIntegration.DefenseProfile p = DefenseSemanticIntegration.byRecipe(r.name);
            String activation = p == null ? "legacy" : p.activation.name();
            String kind = p == null ? "unknown" : p.kind.name();
            String blueprint = p == null ? "legacy" : p.constructionBlueprint;
            lines.add(r.name + " | kind=" + kind + " blueprint=" + blueprint + " faction=" + r.requiredFaction.label
                    + " activation=" + activation + " cost=" + r.supplyCost + " supplies/" + r.partCost + " parts"
                    + " components=" + r.componentCostSummary(4));
        }
        return lines;
    }

    static String advisoryFor(BuildRecipe r) {
        if (r == null) return "Defense build authority: no defense recipe selected.";
        DefenseSemanticIntegration.DefenseProfile p = DefenseSemanticIntegration.byRecipe(r.name);
        if (p == null) return "Defense build authority: " + r.name + " uses legacy build semantics.";
        return "Defense build authority: " + p.label + " uses " + p.constructionBlueprint
                + "; activation=" + p.activation + "; combat role=" + p.combatRole
                + "; current targeting=manual/disabled.";
    }

    static String auditSummary() {
        int turret = 0, passive = 0;
        for (BuildRecipe r : buildRecipes()) {
            DefenseSemanticIntegration.DefenseProfile p = DefenseSemanticIntegration.byRecipe(r.name);
            if (p != null && p.kind == DefenseSemanticIntegration.DefenseKind.TURRET) turret++;
            if (p != null && p.activation != DefenseSemanticIntegration.ActivationState.COMBAT_HELD) passive++;
        }
        return "defenseBuildSelection version=" + VERSION + " recipes=" + buildRecipes().size()
                + " turretProfiles=" + turret + " passiveOrInspectable=" + passive;
    }
}
