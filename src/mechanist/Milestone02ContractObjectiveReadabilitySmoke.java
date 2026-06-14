package mechanist;

import java.util.List;

final class Milestone02ContractObjectiveReadabilitySmoke {
    public static void main(String[] args) {
        FactionContract bounty = new FactionContract();
        bounty.id = "B-SECRET-991";
        bounty.type = "BOUNTY";
        bounty.faction = Faction.ARBITES;
        bounty.targetName = "Wanted fugitive Sorn Ash";
        bounty.targetEntityId = "CONTRACT-NPC-B-SECRET-991";
        bounty.requiredTurnInItem = "Ident chip B-SECRET-991";
        bounty.targetZoneKey = "1,1,3,2,4,false";
        bounty.payout = 90;
        bounty.repReward = 2;
        bounty.spawned = true;

        List<String> lines = ContractObjectiveReadabilityAuthority.summary(List.of(bounty),
                List.of("Ident chip B-SECRET-991"), List.of(), 3);
        requireContains(lines, "Active contracts: 1", "active count");
        requireContains(lines, "the target's ident chip", "public evidence name");
        requireContains(lines, "carried and ready for turn-in", "possession state");
        requireContains(lines, "target or objective confirmed", "route confidence");
        requireContains(lines, "90 script", "reward");
        for (String line : lines) {
            if (line.contains("B-SECRET-991") || line.contains("CONTRACT-NPC")) {
                throw new AssertionError("Contract summary leaked internal identifiers: " + line);
            }
            if (PlayerFacingText.containsLikelyLeak(line)) throw new AssertionError("Contract summary leaked implementation text: " + line);
        }
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.toLowerCase().contains(expected.toLowerCase())) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone02ContractObjectiveReadabilitySmoke() {}
}
