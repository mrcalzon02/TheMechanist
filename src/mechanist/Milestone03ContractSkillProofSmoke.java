package mechanist;

import java.util.List;
import java.util.Set;

/** Smoke for contract objective summaries exposing skill and knowledge proof readiness. */
final class Milestone03ContractSkillProofSmoke {
    public static void main(String[] args) {
        FactionContract lockbox = new FactionContract();
        lockbox.id = "L-SKILL-PROOF-77";
        lockbox.type = "LOCKBOX";
        lockbox.faction = Faction.NOBLE;
        lockbox.targetName = "Stock certificate bundle from a noble bank vault";
        lockbox.targetEntityId = "CONTRACT-BANK-L-SKILL-PROOF-77";
        lockbox.requiredTurnInItem = "Stock certificate bundle";
        lockbox.targetZoneKey = "1,1,2,2,6,false";
        lockbox.description = "Acquire Stock certificate bundle from a bank vault and return it to the private noble courier.";
        lockbox.payout = 700;
        lockbox.repReward = 3;
        lockbox.spawned = true;

        List<String> untrained = ContractObjectiveReadabilityAuthority.summary(List.of(lockbox),
                List.of(), List.of("Stock certificate bundle"), 3, Set.of(), Set.of());
        requireContains(untrained, "Skill proof: Certified Market Appraisal / not trained", "untrained certified skill");
        requireContains(untrained, "Knowledge proof: Contract Negotiation / not known", "untrained contract knowledge");
        requireContains(untrained, "held in base storage; retrieve before turn-in", "stored proof");

        List<String> trained = ContractObjectiveReadabilityAuthority.summary(List.of(lockbox),
                List.of("Stock certificate bundle"), List.of(), 3,
                Set.of("trade-batch-appraisal", "trade-guilder-certification"),
                Set.of("Contract Negotiation"));
        requireContains(trained, "Skill proof: Certified Market Appraisal / trained", "trained certified skill");
        requireContains(trained, "Knowledge proof: Contract Negotiation / known", "known contract knowledge");
        requireContains(trained, "carried and ready for turn-in", "carried proof");
        requireContains(trained, "completion and reward rules remain owned by the contract turn-in flow",
                "contract proof boundary");
        for (String line : trained) rejectLeaks(line);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void rejectLeaks(String line) {
        if (line.contains("L-SKILL-PROOF-77") || line.contains("CONTRACT-BANK")) {
            throw new AssertionError("Contract proof leaked internal identifier: " + line);
        }
        if (PlayerFacingText.containsLikelyLeak(line)) {
            throw new AssertionError("Contract proof leaked implementation text: " + line);
        }
    }

    private Milestone03ContractSkillProofSmoke() { }
}
