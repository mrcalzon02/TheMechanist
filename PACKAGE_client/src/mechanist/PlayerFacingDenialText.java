package mechanist;

import java.util.Locale;

/**
 * Shared Gate 3 composer for ordinary player-facing denial feedback.
 *
 * <p>Gameplay systems should keep authoritative rejection state in their own domain objects and use
 * this class only when preparing a readable message for ordinary UI panels. Raw diagnostics can stay
 * in audit logs; this class makes the player-facing sentence useful without exposing save keys,
 * registry handles, Java class names, paths, or debug-only wording.</p>
 */
final class PlayerFacingDenialText {
    private static final String NO_READABLE_DETAILS = "No readable details are available yet.";

    enum Context {
        LOOK("Inspection unavailable", "There is nothing useful to inspect here."),
        INTERACTION("Interaction unavailable", "That cannot be used right now."),
        MOVEMENT("Movement unavailable", "You cannot move there right now."),
        TRANSITION("Route unavailable", "The way ahead is blocked or unavailable."),
        INVENTORY("Inventory action unavailable", "That item action cannot be completed right now."),
        CONSTRUCTION("Construction unavailable", "That construction order cannot be completed here."),
        TRADE("Trade unavailable", "That trade cannot be completed right now."),
        COMBAT("Attack unavailable", "That attack cannot be completed right now."),
        SAVE_LOAD("Save/load unavailable", "That save or load action cannot be completed right now."),
        OPTION("Option unavailable", "That option cannot be changed right now.");

        final String title;
        final String fallback;

        Context(String title, String fallback) {
            this.title = title;
            this.fallback = fallback;
        }
    }

    private PlayerFacingDenialText() { }

    static String message(Context context, String reason) {
        Context resolved = context == null ? Context.INTERACTION : context;
        String cleanReason = cleanReason(reason);
        if (cleanReason.isBlank()) {
            return resolved.fallback;
        }
        return resolved.title + ": " + cleanReason;
    }

    static String interaction(String reason) { return message(Context.INTERACTION, reason); }

    static String movement(String reason) { return message(Context.MOVEMENT, reason); }

    static String transition(String reason) { return message(Context.TRANSITION, reason); }

    static String inventory(String reason) { return message(Context.INVENTORY, reason); }

    static String construction(String reason) { return message(Context.CONSTRUCTION, reason); }

    static String trade(String reason) { return message(Context.TRADE, reason); }

    static String combat(String reason) { return message(Context.COMBAT, reason); }

    private static String cleanReason(String reason) {
        String cleaned = PlayerFacingCopySanitizer.forOrdinaryPlayer(reason)
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim();

        if (isEmptyReason(cleaned)) return "";
        return cleaned.endsWith(".") ? cleaned : cleaned + ".";
    }

    private static boolean isEmptyReason(String text) {
        if (text == null || text.isBlank()) return true;
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return normalized.equals(NO_READABLE_DETAILS.toLowerCase(Locale.ROOT))
                || normalized.equals("internal record")
                || normalized.equals("diagnostic details")
                || normalized.equals("runtime service")
                || normalized.equals("catalog")
                || normalized.equals("the marked route")
                || normalized.equals("debug")
                || normalized.equals("error")
                || normalized.equals("exception")
                || normalized.equals("failed")
                || normalized.equals("blocked")
                || normalized.equals("missing")
                || normalized.equals("unavailable")
                || normalized.equals("denied");
    }
}
