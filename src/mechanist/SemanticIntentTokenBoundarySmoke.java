package mechanist;

/**
 * Focused regression smoke for semantic intent token boundaries.
 * Semantic keywords must classify complete normalized words or phrases rather
 * than accidental substrings embedded inside unrelated player-facing text.
 */
public final class SemanticIntentTokenBoundarySmoke {
    public static void main(String[] args) {
        requireItem("battle axe", SemanticRenderAssetResolver.RenderIntent.WEAPON_ITEM_ICON);
        requireItem("utility vest", SemanticRenderAssetResolver.RenderIntent.ARMOR_ITEM_ICON);
        requireObject("airlock door", SemanticRenderAssetResolver.RenderIntent.DOOR_CLOSED);

        requireItem("harvest ration", SemanticRenderAssetResolver.RenderIntent.FOOD_ITEM_ICON);
        requireNoItem("taxes ledger");
        requireObject("outdoor market counter", SemanticRenderAssetResolver.RenderIntent.MARKET_COUNTER);

        System.out.println("SemanticIntentTokenBoundarySmoke PASS " + SemanticRenderIntentAuthority.VERSION);
    }

    private static void requireItem(String text, SemanticRenderAssetResolver.RenderIntent expected) {
        SemanticRenderAssetResolver.RenderIntent actual = SemanticRenderIntentAuthority.itemIntent(text)
                .orElseThrow(() -> new AssertionError("missing item intent for: " + text));
        if (actual != expected) {
            throw new AssertionError("item intent for '" + text + "' was " + actual + ", expected " + expected);
        }
    }

    private static void requireNoItem(String text) {
        if (SemanticRenderIntentAuthority.itemIntent(text).isPresent()) {
            throw new AssertionError("unexpected item intent for: " + text);
        }
    }

    private static void requireObject(String text, SemanticRenderAssetResolver.RenderIntent expected) {
        SemanticRenderAssetResolver.RenderIntent actual = SemanticRenderIntentAuthority.objectIntent(text)
                .orElseThrow(() -> new AssertionError("missing object intent for: " + text));
        if (actual != expected) {
            throw new AssertionError("object intent for '" + text + "' was " + actual + ", expected " + expected);
        }
    }
}
