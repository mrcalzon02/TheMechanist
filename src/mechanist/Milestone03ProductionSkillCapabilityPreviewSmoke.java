package mechanist;

import java.util.List;

/** Smoke for exposing unlocked skill capability hooks in manual production preview text. */
final class Milestone03ProductionSkillCapabilityPreviewSmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.unlockedKnowledges.add("Scrap-Forging Doctrine");
        game.supplies = 4;
        game.machineParts = 4;
        game.unlockedSkillNodes.add("fab-repair-field-tech");
        game.unlockedSkillNodes.add("fab-repair-material-eye");
        game.unlockedSkillNodes.add("machine-safe-start");

        BaseObject forge = new BaseObject("Preview Forge", 'f', 0, 0, 0, 0);
        forge.qualityName = "Fine";
        forge.integrity = 5;
        game.baseObjects.add(forge);
        CraftingRecipe recipe = new CraftingRecipe("Preview Cutter", "Emergency Cutter", Faction.MECHANICUS,
                "Scrap-Forging Doctrine", 'f', 1, 1, 1, 1, 8, 3, 1, "Mechanics", "Preview hook test.");

        List<String> lines = ProductionReadabilityAuthority.detailLines(game, recipe);
        requireContains(lines, "Skill capability hooks: preview context only", "hook boundary");
        requireContains(lines, "Skill passive hook: fab-repair-material-eye | passive production-limiter-readability:+1",
                "passive limiter hook");
        requireContains(lines, "Skill active ability: machine-safe-start | active machine-readiness-preview",
                "machine readiness active hook");
        requireContains(lines, "Outcome estimate", "unchanged production preview remains present");
        for (String line : lines) rejectLeaks(line);
        if (game.timer != null) game.timer.stop();
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void rejectLeaks(String line) {
        if (PlayerFacingText.containsLikelyLeak(line)) {
            throw new AssertionError("Production skill capability preview leaked implementation text: " + line);
        }
    }

    private Milestone03ProductionSkillCapabilityPreviewSmoke() { }
}
