package mechanist;

import java.util.List;
import java.util.Set;

/** Smoke for the contract skill-proof audit surface. */
final class Milestone03ContractSkillProofAuditSmoke {
    public static void main(String[] args) {
        FactionContract lockbox = new FactionContract();
        lockbox.id = "L-SKILL-PROOF-AUDIT-91";
        lockbox.type = "LOCKBOX";
        lockbox.faction = Faction.NOBLE;
        lockbox.targetName = "Stock certificate bundle from a noble bank vault";
        lockbox.targetEntityId = "CONTRACT-BANK-L-SKILL-PROOF-AUDIT-91";
        lockbox.requiredTurnInItem = "Stock certificate bundle";
        lockbox.targetZoneKey = "1,1,2,2,6,false";
        lockbox.description = "Acquire Stock certificate bundle from a bank vault and return it to the private noble courier.";
        lockbox.payout = 700;
        lockbox.repReward = 3;
        lockbox.spawned = true;

        List<String> audit = ContractObjectiveReadabilityAuthority.auditLines(List.of(lockbox),
                Set.of("trade-batch-appraisal", "trade-guilder-certification"), Set.of("Contract Negotiation"), 2);
        requireContains(audit, "owner=ContractObjectiveReadabilityAuthority", "owner");
        requireContains(audit, "completionMutation=false", "completion boundary");
        requireContains(audit, "rewardMutation=false", "reward boundary");
        requireContains(audit, "rawIdsHidden=true", "raw-id boundary");
        requireContains(audit, "skillProof=Certified Market Appraisal:trained", "trained skill proof");
        requireContains(audit, "knowledgeProof=Contract Negotiation:known", "known knowledge proof");
        requireContains(audit, "boundary=readiness only", "readiness boundary");
        requireContains(audit, "Milestone03ContractSkillProofAuditSmoke", "guard");
        for (String line : audit) rejectLeaks(line);

        List<String> untrained = ContractObjectiveReadabilityAuthority.auditLines(List.of(lockbox),
                Set.of(), Set.of(), 2);
        requireContains(untrained, "skillProof=Certified Market Appraisal:not trained", "untrained skill proof");
        requireContains(untrained, "knowledgeProof=Contract Negotiation:not known", "unknown knowledge proof");
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void rejectLeaks(String line) {
        if (line.contains("L-SKILL-PROOF-AUDIT-91") || line.contains("CONTRACT-BANK")) {
            throw new AssertionError("Contract skill-proof audit leaked internal identifier: " + line);
        }
        if (PlayerFacingText.containsLikelyLeak(line)) {
            throw new AssertionError("Contract skill-proof audit leaked implementation text: " + line);
        }
    }

    private Milestone03ContractSkillProofAuditSmoke() { }
}
