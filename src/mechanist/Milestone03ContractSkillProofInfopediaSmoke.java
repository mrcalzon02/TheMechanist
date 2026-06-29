package mechanist;

import java.util.List;

/** Smoke for Infopedia coverage of contract skill and knowledge proof readiness. */
final class Milestone03ContractSkillProofInfopediaSmoke {
    public static void main(String[] args) {
        List<String> lines = SemanticAssetInfopediaAuthority.mechanicDetailLinesByKey("contract-evidence");
        requireContains(lines, "skill and knowledge proof readiness", "proof-readiness explanation");
        requireContains(lines, "Certified Market Appraisal", "certified appraisal reference");
        requireContains(lines, "Investigation Trace Reading", "trace-reading reference");
        requireContains(lines, "Contract Negotiation", "knowledge proof reference");
        requireContains(lines, "does not complete the contract", "completion boundary");
        for (String line : lines) rejectLeaks(line);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void rejectLeaks(String line) {
        if (PlayerFacingText.containsLikelyLeak(line)) {
            throw new AssertionError("Contract skill-proof Infopedia leaked implementation text: " + line);
        }
        String lower = line == null ? "" : line.toLowerCase();
        String[] processTerms = {"guard", "smoke", "authority", "audit", "future", "owner=", "raw-id", "raw id"};
        for (String term : processTerms) {
            if (lower.contains(term)) {
                throw new AssertionError("Contract skill-proof Infopedia included process language '" + term + "': " + line);
            }
        }
    }

    private Milestone03ContractSkillProofInfopediaSmoke() { }
}
