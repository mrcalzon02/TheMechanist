package mechanist;

final class Milestone02InfopediaHotLinkSmoke {
    public static void main(String[] args) {
        requireRow("body-condition", "MECHANIC - Body Condition [Health]");
        requireRow("contract-evidence", "MECHANIC - Contract Objectives and Evidence [Quests]");
        requireRow("expansion-heat", "MECHANIC - Expansion Heat [Economy]");
        if (InfopediaHotLinkAuthority.resolveMechanicRow("missing-mechanic").isPresent()) {
            throw new AssertionError("Unknown mechanic hot links must not resolve.");
        }
        if (InfopediaHotLinkAuthority.openMechanic(null, "body-condition", "smoke")) {
            throw new AssertionError("A mechanic hot link must fail safely without a panel.");
        }
    }

    private static void requireRow(String key, String expected) {
        String row = InfopediaHotLinkAuthority.resolveMechanicRow(key)
                .orElseThrow(() -> new AssertionError("Missing mechanic hot link: " + key));
        if (!expected.equals(row)) throw new AssertionError("Unexpected mechanic row for " + key + ": " + row);
        if (PlayerFacingText.containsLikelyLeak(row)) throw new AssertionError("Mechanic hot link leaked implementation text: " + row);
    }

    private Milestone02InfopediaHotLinkSmoke() { }
}
