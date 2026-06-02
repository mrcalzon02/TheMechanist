package mechanist;

import java.util.List;

/**
 * Gate 3 facade for ordinary player-facing presentation text.
 *
 * <p>This is the preferred migration entrypoint for gameplay/UI callsites. It keeps callers from
 * depending directly on the internal helper layout while still routing visible text through the
 * shared sanitization, wrapping, denial, inspection, action, panel, event, tooltip, list-row, and
 * menu-option authorities.</p>
 */
final class PlayerFacingText {
    private PlayerFacingText() { }

    static String sanitize(String text) {
        return PlayerFacingCopySanitizer.forOrdinaryPlayer(text);
    }

    static boolean containsLikelyLeak(String text) {
        return PlayerFacingCopySanitizer.containsLikelyPlayerFacingLeak(text);
    }

    static List<String> wrap(String text, int maxWidth) {
        return PlayerFacingTextWrap.wrap(text, maxWidth);
    }

    static String panel(String body, int wrapWidth) {
        return PlayerFacingPanelBody.format(body, wrapWidth);
    }

    static String titledPanel(String title, String body, int wrapWidth) {
        return PlayerFacingPanelBody.formatWithTitle(title, body, wrapWidth);
    }

    static String tooltip(String title, String detail, int wrapWidth) {
        return PlayerFacingTooltipText.tooltip(title, detail, wrapWidth);
    }

    static String event(String category, String body, int wrapWidth) {
        return PlayerFacingEventLogText.event(category, body, wrapWidth);
    }

    static String row(String primary, String secondary, int wrapWidth) {
        return PlayerFacingListRowText.row(primary, secondary, wrapWidth);
    }

    static String option(String label, String detail, boolean enabled, int wrapWidth) {
        return PlayerFacingMenuOptionText.option(label, detail, enabled, wrapWidth);
    }

    static String denial(PlayerFacingDenialText.Context context, String reason) {
        return PlayerFacingDenialText.message(context, reason);
    }

    static String inspectionTile(String tileName, String detail) {
        return PlayerFacingInspectionText.tile(tileName, detail);
    }

    static String inspectionFixture(String fixtureName, String detail) {
        return PlayerFacingInspectionText.fixture(fixtureName, detail);
    }

    static String inspectionActor(String actorName, String detail) {
        return PlayerFacingInspectionText.actor(actorName, detail);
    }

    static String inspectionItem(String itemName, String detail) {
        return PlayerFacingInspectionText.item(itemName, detail);
    }

    static String actionInventory(String action, String detail) {
        return PlayerFacingActionText.inventory(action, detail);
    }

    static String actionTravel(String action, String detail) {
        return PlayerFacingActionText.travel(action, detail);
    }
}
