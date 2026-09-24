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

        requireItem("reserve rifles", SemanticRenderAssetResolver.RenderIntent.WEAPON_ITEM_ICON);
        requireItem("field repair kits", SemanticRenderAssetResolver.RenderIntent.TOOL_ITEM_ICON);
        requireItem("ceremonial knives", SemanticRenderAssetResolver.RenderIntent.WEAPON_ITEM_ICON);
        requireObject("sealed cargo doors", SemanticRenderAssetResolver.RenderIntent.DOOR_CLOSED);
        requireObject("backup generators", SemanticRenderAssetResolver.RenderIntent.GENERATOR_MACHINE);
        requireObject("market counters", SemanticRenderAssetResolver.RenderIntent.MARKET_COUNTER);
        requireNoItem("taxes ledgers");

        requireNoObject("bulkhead light fixture");
        requireNoObject("hatch light switch");
        requireNoObject("door motion sensor");
        requireObject("sealed bulkhead door", SemanticRenderAssetResolver.RenderIntent.DOOR_CLOSED);

        // Milestone 06 vehicle parts must remain component artwork even when a
        // compound also names a complete weapon, worn armor, or generic object.
        requireItem("heavy bolter weapon mount", SemanticRenderAssetResolver.RenderIntent.INDUSTRIAL_COMPONENT_ITEM_ICON);
        requireItem("replacement armor plate", SemanticRenderAssetResolver.RenderIntent.INDUSTRIAL_COMPONENT_ITEM_ICON);
        requireItem("civilian vehicle chassis", SemanticRenderAssetResolver.RenderIntent.INDUSTRIAL_COMPONENT_ITEM_ICON);
        requireItem("tracked suspension assembly", SemanticRenderAssetResolver.RenderIntent.INDUSTRIAL_COMPONENT_ITEM_ICON);
        requireItem("forward vehicle optics", SemanticRenderAssetResolver.RenderIntent.INDUSTRIAL_COMPONENT_ITEM_ICON);
        requireItem("crew hatch assembly", SemanticRenderAssetResolver.RenderIntent.INDUSTRIAL_COMPONENT_ITEM_ICON);

        // Ammunition remains associated with weapons but is not complete weapon art.
        requireNoItem("heavy bolter ammunition");
        requireNoItem("rifle magazine");

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

    private static void requireNoObject(String text) {
        if (SemanticRenderIntentAuthority.objectIntent(text).isPresent()) {
            throw new AssertionError("unexpected object intent for: " + text);
        }
    }
}