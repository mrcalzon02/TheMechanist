package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Semantic rendering doctrine for the current repair lane.
 *
 * This is deliberately small and dependency-light so it can be used by smoke
 * tests before the full renderer is migrated. The rule is simple: live
 * rendered world things must resolve by semantic purpose before they fall back
 * to a generic/system icon.
 */
final class RenderAssetBindingDoctrineAuthority {
    static final String VERSION = "render-asset-binding-doctrine-0.2";

    enum RenderThing {
        STREET_LIGHT,
        DOOR_CLOSED,
        DOOR_OPEN,
        VISIBLE_TILE,
        VIEWPORT_ZOOM,
        PAUSE_MAIN_MENU,
        PAUSE_INFOPEDIA
    }

    static final class BindingRule {
        final RenderThing thing;
        final String requiredSemanticType;
        final String requiredMeaning;
        final String forbiddenFallback;

        BindingRule(RenderThing thing, String requiredSemanticType, String requiredMeaning, String forbiddenFallback) {
            this.thing = thing;
            this.requiredSemanticType = requiredSemanticType;
            this.requiredMeaning = requiredMeaning;
            this.forbiddenFallback = forbiddenFallback;
        }

        boolean accepts(String semanticType, String meaning, String fallback) {
            String type = normalize(semanticType);
            String text = normalize(meaning);
            String fb = normalize(fallback);
            return type.contains(normalize(requiredSemanticType))
                    && text.contains(normalize(requiredMeaning))
                    && (forbiddenFallback == null || forbiddenFallback.isBlank() || !fb.contains(normalize(forbiddenFallback)));
        }

        String summary() {
            return thing + " requires " + requiredSemanticType + " meaning " + requiredMeaning
                    + (forbiddenFallback == null || forbiddenFallback.isBlank() ? "" : " and forbids " + forbiddenFallback);
        }
    }

    private RenderAssetBindingDoctrineAuthority() {}

    static List<BindingRule> rules() {
        ArrayList<BindingRule> rules = new ArrayList<>();
        rules.add(new BindingRule(RenderThing.STREET_LIGHT, "fixture", "streetlight", "system inventory"));
        rules.add(new BindingRule(RenderThing.STREET_LIGHT, "fixture", "street light", "item icon"));
        rules.add(new BindingRule(RenderThing.DOOR_CLOSED, "door tile", "closed", "generic wall"));
        rules.add(new BindingRule(RenderThing.DOOR_OPEN, "door tile", "open", "generic floor"));
        rules.add(new BindingRule(RenderThing.VISIBLE_TILE, "overlay", "fog illumination gradient", "blue debug box"));
        rules.add(new BindingRule(RenderThing.VIEWPORT_ZOOM, "viewport", "stable frame internal tile scale", "window resize"));
        rules.add(new BindingRule(RenderThing.PAUSE_MAIN_MENU, "menu command", "return root main menu", "infopedia"));
        rules.add(new BindingRule(RenderThing.PAUSE_INFOPEDIA, "menu command", "open infopedia", "main menu alias"));
        return rules;
    }

    static List<String> doctrineLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Semantic render binding doctrine " + VERSION + ".");
        lines.add("Streetlights resolve as infrastructure fixtures/streetlight art, never as system inventory icons.");
        lines.add("Closed doors resolve as closed-door tile variants; open doors resolve as open-door tile variants.");
        lines.add("Visibility is conveyed through fog and graduated illumination, not blue debug boxes around every visible tile.");
        lines.add("2D zoom changes tile scale inside the existing map viewport; it must not resize the owning game menu/frame.");
        lines.add("Pause Main Menu returns to the root main menu. Infopedia is a separate explicit command bar and pause-menu route.");
        return lines;
    }

    static boolean doctrinePasses() {
        return rules().size() >= 8
                && acceptsStreetLight("fixture infrastructure", "streetlight pole lamp", "semantic fallback")
                && !acceptsStreetLight("item icon", "system inventory light", "system inventory")
                && acceptsClosedDoor("door tile", "door closed variant", "semantic fallback")
                && acceptsOpenDoor("door tile", "door open variant", "semantic fallback")
                && acceptsPauseMainMenu("menu command", "return root main menu", "root")
                && !acceptsPauseMainMenu("menu command", "return root main menu", "infopedia");
    }

    static boolean acceptsStreetLight(String semanticType, String meaning, String fallback) {
        return rules().stream().filter(r -> r.thing == RenderThing.STREET_LIGHT).anyMatch(r -> r.accepts(semanticType, meaning, fallback));
    }

    static boolean acceptsClosedDoor(String semanticType, String meaning, String fallback) {
        return rules().stream().filter(r -> r.thing == RenderThing.DOOR_CLOSED).anyMatch(r -> r.accepts(semanticType, meaning, fallback));
    }

    static boolean acceptsOpenDoor(String semanticType, String meaning, String fallback) {
        return rules().stream().filter(r -> r.thing == RenderThing.DOOR_OPEN).anyMatch(r -> r.accepts(semanticType, meaning, fallback));
    }

    static boolean acceptsPauseMainMenu(String semanticType, String meaning, String fallback) {
        return rules().stream().filter(r -> r.thing == RenderThing.PAUSE_MAIN_MENU).anyMatch(r -> r.accepts(semanticType, meaning, fallback));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('-', ' ').replace('_', ' ').trim();
    }
}
