package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Compact relationship and service context for ordinary conversation panels. */
final class ConversationReadabilityAuthority {
    static final String VERSION = "0.9.10kn";

    record ConversationReadout(String relationshipLine, String serviceLine, String consequenceLine, List<String> lines) { }

    private ConversationReadabilityAuthority() { }

    static ConversationReadout describe(NpcEntity npc, int standing, int hostileTurns) {
        if (npc == null) {
            return new ConversationReadout("Relationship unavailable.", "No services available.",
                    "No conversation target is selected.", List.of("No conversation target is selected."));
        }
        String faction = npc.faction == null || npc.faction == Faction.NONE ? "local independent" : npc.faction.label;
        String relationship = "Relationship: " + standingBand(standing) + " with " + faction + ".";
        String service = npc.isTrader()
                ? "Services: trade stock is available from this speaker."
                : npc.isFactionRepresentative()
                    ? "Services: faction contact and standing context are available; no quest offer is currently listed on this panel."
                    : "Services: conversation and inspection only.";
        String consequence = hostileTurns > 0 || "Hostile".equalsIgnoreCase(npc.state)
                ? "Warning: hostility is active; peaceful choices or services may be unavailable."
                : standing < -10
                    ? "Low standing may restrict trust, prices, access, or future offers."
                    : standing > 10
                        ? "Good standing may improve trust and future service access."
                        : "No immediate standing consequence is shown for this conversation.";
        ArrayList<String> lines = new ArrayList<>();
        lines.add(relationship);
        lines.add(service);
        lines.add(consequence);
        return new ConversationReadout(relationship, service, consequence, List.copyOf(lines));
    }

    static String standingBand(int standing) {
        if (standing <= -25) return "hated";
        if (standing <= -10) return "distrusted";
        if (standing < 10) return "neutral";
        if (standing < 25) return "trusted";
        return "honored";
    }

    static String auditSummary() {
        return "conversationReadabilityAuthority version=" + VERSION
                + " exposes=speakerRole+factionStanding+services+knownConsequences"
                + " questPlaceholderClaims=false";
    }
}
