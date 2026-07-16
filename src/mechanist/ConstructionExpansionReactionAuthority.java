package mechanist;

import java.util.List;

/** Applies one-time faction pressure when player construction crosses expansion thresholds. */
final class ConstructionExpansionReactionAuthority {
    record Reaction(String key, String title, int threshold, int pressureAdded, String summary) {
        boolean triggered() { return key != null && !key.isBlank(); }
    }

    private static final Reaction NONE = new Reaction("", "", 0, 0, "");
    private static final List<Reaction> REACTIONS = List.of(
            new Reaction("noticeable", "Local notice", 6, 1,
                    "Local brokers and neighborhood gangs have noticed the growing footprint; permit offers, service pitches, and protection demands may follow."),
            new Reaction("high", "Faction pressure", 12, 2,
                    "Civic assessors, gangs, and established factions now treat the holding as a serious local enterprise; surveillance, fees, and recruitment approaches are likely."),
            new Reaction("critical", "Rival power", 20, 3,
                    "Major factions now treat the holding as a rival power center; diplomacy, sabotage, raids, and scheme targeting are credible responses.")
    );

    private ConstructionExpansionReactionAuthority() { }

    static Reaction apply(GamePanel game, BlueprintExpansionHeatAuthority.AppliedAttention attention) {
        if (game == null || attention == null) return NONE;
        int pressure = Math.max(attention.heatAfter(), attention.suspicionAfter());
        Reaction selected = NONE;
        for (Reaction reaction : REACTIONS) {
            if (pressure >= reaction.threshold() && !game.constructionExpansionReactions.contains(reaction.key())) {
                selected = reaction;
            }
        }
        if (!selected.triggered()) return NONE;
        for (Reaction reaction : REACTIONS) {
            if (reaction.threshold() <= selected.threshold()) game.constructionExpansionReactions.add(reaction.key());
        }
        game.addFactionMarketPressure(Faction.CIVIC_WARDENS, selected.pressureAdded(),
                "player construction reached " + selected.title().toLowerCase());
        game.addFactionMarketPressure(Faction.BANDIT, selected.pressureAdded(),
                "player construction reached " + selected.title().toLowerCase());
        if (selected.threshold() >= 12) {
            game.addFactionMarketPressure(Faction.NOBLE, Math.max(1, selected.pressureAdded() - 1),
                    "player construction reached " + selected.title().toLowerCase());
        }
        return selected;
    }

    static String playerLine(Reaction reaction) {
        if (reaction == null || !reaction.triggered()) return "";
        return "Expansion response - " + reaction.title() + ": " + reaction.summary()
                + " Faction market pressure +" + reaction.pressureAdded() + ".";
    }

    static String statusLine(GamePanel game) {
        if (game == null || game.constructionExpansionReactions.isEmpty()) {
            return "Expansion response: no faction threshold response has been recorded.";
        }
        Reaction strongest = NONE;
        for (Reaction reaction : REACTIONS) {
            if (game.constructionExpansionReactions.contains(reaction.key())) strongest = reaction;
        }
        return "Expansion response: " + strongest.title() + " recorded at " + strongest.threshold()
                + " pressure; further construction can still reach stronger responses.";
    }
}
