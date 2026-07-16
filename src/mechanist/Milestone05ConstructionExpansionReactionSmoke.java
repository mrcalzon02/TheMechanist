package mechanist;

import java.util.List;
import java.util.Properties;

/** Smoke for cumulative Phase 12.3 construction thresholds and durable faction reactions. */
final class Milestone05ConstructionExpansionReactionSmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        game.initFactionState();

        ConstructionExpansionReactionAuthority.Reaction low = apply(game, 2, 6);
        require(low.triggered() && "Local notice".equals(low.title()), "pressure 6 should trigger local notice");
        require(game.constructionExpansionReactions.contains("noticeable"), "noticeable threshold should be recorded");
        require(game.factionMarketPressure.getOrDefault(Faction.CIVIC_WARDENS, 0) == 1,
                "local notice should raise civic market pressure");
        require(game.factionMarketPressure.getOrDefault(Faction.BANDIT, 0) == 1,
                "local notice should raise gang market pressure");
        requireContains(ConstructionExpansionReactionAuthority.playerLine(low), "permit offers", "local response guidance");

        ConstructionExpansionReactionAuthority.Reaction duplicate = apply(game, 6, 8);
        require(!duplicate.triggered(), "noticeable response should not repeat below the next threshold");
        require(game.factionMarketPressure.getOrDefault(Faction.CIVIC_WARDENS, 0) == 1,
                "duplicate construction should not repeat civic pressure");

        ConstructionExpansionReactionAuthority.Reaction high = apply(game, 8, 12);
        require(high.triggered() && "Faction pressure".equals(high.title()), "pressure 12 should trigger faction pressure");
        require(game.factionMarketPressure.getOrDefault(Faction.CIVIC_WARDENS, 0) == 3,
                "high threshold should add two civic pressure");
        require(game.factionMarketPressure.getOrDefault(Faction.NOBLE, 0) == 1,
                "high threshold should bring noble attention");

        Properties saved = new Properties();
        Persistence.writeCore(game, saved);
        require(Persistence.decList(saved.getProperty("run.constructionExpansionReactions", ""))
                        .containsAll(List.of("noticeable", "high")),
                "saved construction reaction ledger should contain crossed thresholds");
        GamePanel restored = new GamePanel();
        restored.shutdownRuntime();
        restored.initFactionState();
        restored.constructionExpansionReactions.addAll(
                Persistence.decList(saved.getProperty("run.constructionExpansionReactions", "")));
        require(restored.constructionExpansionReactions.containsAll(List.of("noticeable", "high")),
                "construction reaction thresholds should survive save/load");
        requireContains(ConstructionExpansionReactionAuthority.statusLine(restored),
                "Faction pressure recorded at 12", "restored expansion response readback");
        ConstructionExpansionReactionAuthority.Reaction restoredDuplicate = apply(restored, 12, 14);
        require(!restoredDuplicate.triggered(), "loaded threshold state should continue suppressing duplicates");

        ConstructionExpansionReactionAuthority.Reaction critical = apply(restored, 14, 20);
        require(critical.triggered() && "Rival power".equals(critical.title()),
                "pressure 20 should trigger rival-power treatment");
        requireContains(ConstructionExpansionReactionAuthority.playerLine(critical), "sabotage, raids",
                "critical response consequences");
        requireContains(ExpansionHeatReadabilityAuthority.summary(restored),
                "Expansion response: Rival power recorded at 20", "global response readback");

        restored.shutdownRuntime();
        game.shutdownRuntime();
        System.out.println("Milestone 05 construction expansion reaction smoke passed.");
    }

    private static ConstructionExpansionReactionAuthority.Reaction apply(GamePanel game, int before, int after) {
        return ConstructionExpansionReactionAuthority.apply(game,
                new BlueprintExpansionHeatAuthority.AppliedAttention(before, after, before, after,
                        Math.max(0, after - before), Math.max(0, after - before), "smoke construction footprint"));
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone05ConstructionExpansionReactionSmoke() { }
}
