package mechanist;

import java.util.Locale;

/** Adds compact resolution paths to ordinary access, blueprint, construction, interaction, and movement denials. */
final class ActionDenialGuidanceAuthority {
    static final String VERSION = "0.9.10km";

    enum DenialKind { ACCESS, BLUEPRINT, CONSTRUCTION, INTERACTION, MOVEMENT }

    private ActionDenialGuidanceAuthority() { }

    static String explain(DenialKind kind, String rawReason) {
        DenialKind safeKind = kind == null ? DenialKind.INTERACTION : kind;
        String reason = PlayerFacingCopySanitizer.forOrdinaryPlayer(domainReason(rawReason));
        String hint = resolutionHint(rawReason);
        String combined = reason + (hint.isBlank() ? "" : " " + hint);
        return switch (safeKind) {
            case ACCESS -> PlayerFacingDenialText.interaction(combined);
            case BLUEPRINT, CONSTRUCTION -> PlayerFacingDenialText.construction(combined);
            case INTERACTION -> PlayerFacingDenialText.interaction(combined);
            case MOVEMENT -> PlayerFacingDenialText.movement(combined);
        };
    }

    static String resolutionHint(String rawReason) {
        String reason = rawReason == null ? "" : rawReason.toLowerCase(Locale.ROOT);
        if (reason.contains("move off") || reason.contains("player")) return "Move away from the target tile and try again.";
        if (reason.contains("occupied") || reason.contains("already contains") || reason.contains("obstruct")) return "Clear or relocate the obstruction first.";
        if (reason.contains("not walkable") || reason.contains("wall") || reason.contains("rock")) return "Choose clear floor or remove the blocking structure first.";
        if (reason.contains("outside") || reason.contains("bounds")) return "Choose a target inside the current buildable area.";
        if (reason.contains("supplies") || reason.contains("machine parts") || reason.contains("components") || reason.contains("resource shortfall")) return "Gather the listed materials before retrying.";
        if (reason.contains("knowledge")) return "Learn the required knowledge before retrying.";
        if (reason.contains("workbench") || reason.contains("machine")) return "Build or approach the required workstation first.";
        if (reason.contains("permit") || reason.contains("license") || reason.contains("reputation") || reason.contains("restricted")) return "Obtain the required permission or standing first.";
        if (reason.contains("owned") || reason.contains("claimed") || reason.contains("access")) return "Gain access or choose an area you control.";
        if (reason.contains("locked")) return "Find the appropriate key, tool, or authorized access route.";
        if (reason.contains("hostile")) return "End the threat or create distance before trying again.";
        return "Review the target and requirements before retrying.";
    }

    static String auditSummary() {
        return "actionDenialGuidanceAuthority version=" + VERSION
                + " contexts=access+blueprint+construction+interaction+movement"
                + " output=sanitizedReason+resolutionPath";
    }

    private static String domainReason(String rawReason) {
        String reason = rawReason == null ? "" : rawReason.trim();
        int categoryEnd = reason.indexOf("]: ");
        if (categoryEnd >= 0) reason = reason.substring(categoryEnd + 3);
        int context = reason.indexOf(" context=");
        if (context >= 0) reason = reason.substring(0, context);
        return reason;
    }
}
