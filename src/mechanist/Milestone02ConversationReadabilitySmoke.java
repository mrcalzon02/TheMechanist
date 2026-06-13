package mechanist;

/** Smoke for relationship, service, and consequence context in conversation. */
final class Milestone02ConversationReadabilitySmoke {
    public static void main(String[] args) {
        NpcEntity representative = new NpcEntity();
        representative.name = "Guild Factor";
        representative.role = "Faction Representative";
        representative.faction = Faction.HIVER;
        representative.state = "Contract Desk";
        ConversationReadabilityAuthority.ConversationReadout trusted =
                ConversationReadabilityAuthority.describe(representative, 14, 0);
        requireContains(trusted.relationshipLine(), "trusted", "trusted standing band");
        requireContains(trusted.serviceLine(), "no quest offer is currently listed", "truthful quest availability");
        requireContains(trusted.consequenceLine(), "improve trust", "positive consequence hint");

        representative.state = "Hostile";
        ConversationReadabilityAuthority.ConversationReadout hostile =
                ConversationReadabilityAuthority.describe(representative, -30, 5);
        requireContains(hostile.relationshipLine(), "hated", "hostile standing band");
        requireContains(hostile.consequenceLine(), "hostility is active", "hostility warning");
        requireContains(ConversationReadabilityAuthority.auditSummary(), "questPlaceholderClaims=false", "conversation boundary audit");
    }

    private static void requireContains(String text, String expected, String label) {
        if (text == null || !text.contains(expected)) throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private Milestone02ConversationReadabilitySmoke() { }
}
