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

        trader.supplyChainSummary = "Limited shipment: 2 offers remain.";
        List<String> context = TradeReadabilityAuthority.marketContext(trader, "Civic Wardens", -8);
        requireContains(context, "Civic Wardens", "market faction");
        requireContains(context, "strained (-8)", "standing band");
        requireContains(context, "item buying and selling only", "service boundary");
        requireContains(context, "Limited shipment", "supply context");

        List<String> evidenceSale = TradeReadabilityAuthority.salePreview("Sealed contract evidence packet", 12);
        requireContains(evidenceSale, "Sale blocked", "protected sale denial");
        requireContains(evidenceSale, "dedicated hand-in", "protected sale resolution");
        if (TradeReadabilityAuthority.saleAllowed("Sealed contract evidence packet")) {
            throw new AssertionError("Protected evidence must not be sellable through ordinary trade.");
        }
        if (!TradeReadabilityAuthority.saleAllowed("Common Water bottle")) {
            throw new AssertionError("Ordinary goods should remain sellable.");
        }

        for (String proof : List.of("Ident chip B-73117", "Sealed object F-73118",
                "Fine Ident chip B-73117", "Common Sealed object F-73118")) {
            if (TradeReadabilityAuthority.saleAllowed(proof)) {
                throw new AssertionError("Generated contract proof must not be sellable: " + proof);
            }
        }
        if (!TradeReadabilityAuthority.saleAllowed("Ident chip reader")) {
            throw new AssertionError("Ordinary identification equipment must remain sellable.");
        }
        verifyContractProofSaleRefusal();

        for (String line : affordable) rejectLeaks(line);
        for (String line : blocked) rejectLeaks(line);
        for (String line : context) rejectLeaks(line);
        for (String line : evidenceSale) rejectLeaks(line);
    }

    private static void verifyContractProofSaleRefusal() {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        try {
            game.activeTraderSession = new TraderSession();
            game.inventory.clear();
            game.inventory.add("Ident chip B-73117");
            game.inventory.add("Sealed object F-73118");
            int money = game.carriedScript;
            int turn = game.turn;
            java.lang.reflect.Method sell = GamePanel.class.getDeclaredMethod("sellSelectedInventoryItemToTrader");
            sell.setAccessible(true);
            for (int index = 0; index < 2; index++) {
                game.selectedInventoryIndex = index;
                sell.invoke(game);
                if (game.inventory.size() != 2 || game.carriedScript != money || game.turn != turn) {
                    throw new AssertionError("Refused proof sale must preserve inventory, currency and turn.");
                }
            }
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not exercise the ordinary sale action.", exception);
        } finally {
            game.shutdownRuntime();
        }
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
