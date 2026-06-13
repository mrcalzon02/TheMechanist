package mechanist;

import java.util.List;

final class Milestone02TradeReadabilitySmoke {
    public static void main(String[] args) {
        TraderSession trader = new TraderSession();
        trader.name = "Mara Venn";
        trader.archetype = "Quartermaster";

        TradeOffer ordinary = new TradeOffer("Common Water bottle", "water", 3, "sealed drinking water");
        List<String> affordable = TradeReadabilityAuthority.offerPreview(trader, ordinary, 10, 2, 20);
        requireContains(affordable, "Vendor: Mara Venn", "vendor identity");
        requireContains(affordable, "Affordable", "affordability preview");
        requireContains(affordable, "Quality: Common", "quality preview");
        requireContains(affordable, "ordinary market stock", "ordinary legality preview");
        requireContains(affordable, "enters carried inventory", "inventory consequence preview");

        TradeOffer illicit = new TradeOffer("Witchsalt", "chem/cult-warp", 28, "forbidden ritual chem");
        List<String> blocked = TradeReadabilityAuthority.offerPreview(trader, illicit, 2, 20, 20);
        requireContains(blocked, "Unavailable", "insufficient-funds preview");
        requireContains(blocked, "forbidden", "illicit legality preview");
        requireContains(blocked, "carrying load is full", "capacity denial preview");

        for (String line : affordable) rejectLeaks(line);
        for (String line : blocked) rejectLeaks(line);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void rejectLeaks(String line) {
        if (PlayerFacingText.containsLikelyLeak(line)) throw new AssertionError("Trade preview leaked implementation text: " + line);
    }

    private Milestone02TradeReadabilitySmoke() {}
}
