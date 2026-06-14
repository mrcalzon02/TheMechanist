package mechanist;

import java.util.Set;

/** Smoke for deliberately equipped production-tool quality capping. */
final class Milestone03ProductionToolQualitySmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.equippedLeftHandItem = "LEFT EMPTY";
        game.equippedRightHandItem = "RIGHT EMPTY";
        require(!ProductionToolQualityAuthority.evaluate(game).active(),
                "integrated machine tooling should leave the optional hand-tool cap open");

        game.equippedLeftHandItem = "Junk Cracked wrench";
        ProductionToolQualityAuthority.ToolQuality junk = ProductionToolQualityAuthority.evaluate(game);
        require(junk.active() && junk.tier() == 0, "equipped Junk repair tool should activate a Junk cap");
        ProductionQualityTraceAuthority.QualityTrace trace = ProductionQualityTraceAuthority.evaluate(
                Set.of("Masterwork Tools Patterns"), "Masterwork Tools Patterns", "Masterwork", -1, 5, junk.tier());
        require("Junk".equals(trace.outputQuality()), "equipped Junk tool should cap otherwise Masterwork output");
        require(trace.limiterLabel().contains("equipped tool"), "quality trace should name the equipped tool limiter");

        game.equippedRightHandItem = "Fine Tool bundle";
        ProductionToolQualityAuthority.ToolQuality best = ProductionToolQualityAuthority.evaluate(game);
        require(best.tier() == 4 && "Fine Tool bundle".equals(best.itemName()),
                "the better of two equipped production tools should govern");
        game.equippedRightHandItem = "Fine Autopistol";
        require(ProductionToolQualityAuthority.evaluate(game).tier() == 0,
                "an unrelated weapon must not replace the equipped repair tool");
        if (game.timer != null) game.timer.stop();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone03ProductionToolQualitySmoke() { }
}
