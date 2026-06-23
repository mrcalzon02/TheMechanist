package mechanist;

/** Smoke for machine-installed production doctrine and save-line compatibility. */
final class Milestone03MachineKnowledgeSourceSmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.unlockedKnowledges.clear();
        BaseObject machine = new BaseObject("Test Forge", 'f', 0, 0, 0, 0);
        game.baseObjects.add(machine);
        CraftingRecipe recipe = new CraftingRecipe("Fine Tool", "Tool", Faction.HIVER,
                "Fine Tools Patterns", 'f', 0, 0, 1, 0, 1, 0, 0, "Mechanics", "test");

        require(!ProductionKnowledgeSourceAuthority.evaluate(game, machine, recipe.requiredKnowledge).available(),
                "knowledge should be unavailable initially");
        require(ProductionKnowledgeSourceAuthority.install(game, machine, recipe.requiredKnowledge).contains("learn"),
                "teaching should require player knowledge");
        game.unlockedKnowledges.add(recipe.requiredKnowledge);
        require(ProductionKnowledgeSourceAuthority.install(game, machine, recipe.requiredKnowledge).startsWith("Installed"),
                "known doctrine should install");
        game.unlockedKnowledges.clear();
        ProductionKnowledgeSourceAuthority.KnowledgeSource source = ProductionKnowledgeSourceAuthority.evaluate(
                game, machine, recipe.requiredKnowledge);
        require(source.available() && source.machineSupplied(), "machine should supply installed doctrine");
        require(recipe.visibleTo(game), "machine doctrine should keep recipe visible");
        require(recipe.blockingProblemForMachine(game, machine) == null, "machine doctrine should satisfy execution gate");
        require(ProductionQualityTraceAuthority.evaluate(source.effectiveKnowledge(), recipe.requiredKnowledge,
                "Fine", -1).doctrineTier() == 4, "machine doctrine should contribute its quality tier");

        String[] saved = machine.saveLine().split("\\|", -1);
        require(saved.length >= 24, "base object save line should preserve append-only doctrine field");
        require(recipe.requiredKnowledge.equals(saved[23]), "machine doctrine should occupy append-only field");
        require(saved.length < 25 || saved[24].isBlank(), "newer repair-history field should not displace doctrine");
        if (game.timer != null) game.timer.stop();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone03MachineKnowledgeSourceSmoke() { }
}
