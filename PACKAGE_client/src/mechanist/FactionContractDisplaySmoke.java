package mechanist;

final class FactionContractDisplaySmoke {
    public static void main(String[] args) {
        FactionContract contract = new FactionContract();
        contract.id = "B-12345";
        contract.type = "BOUNTY";
        contract.faction = Faction.CIVIC_WARDENS;
        contract.targetZoneKey = "1,1,2,3,4,false";
        contract.targetName = "Wanted fugitive Kessel Grint";
        contract.requiredTurnInItem = "Ident chip B-12345";
        contract.description = "Kill Wanted fugitive Kessel Grint in the adjacent contract zone and return Ident chip B-12345.";
        contract.payout = 120;
        contract.repReward = 2;

        String shortLine = contract.shortLine();
        String longLine = contract.longLine();
        if (!shortLine.contains("bounty contract for Adeptus Civic Wardens pays 120 script")) {
            throw new AssertionError("Contract short line was not readable: " + shortLine);
        }
        if (shortLine.contains("B-12345") || longLine.contains("B-12345")) {
            throw new AssertionError("Contract display leaked internal ID: " + shortLine + " / " + longLine);
        }
        if (longLine.contains("1,1,2,3,4,false")) {
            throw new AssertionError("Contract display leaked raw target zone key: " + longLine);
        }
        if (!longLine.contains("return the target's ident chip")) {
            throw new AssertionError("Contract display did not hide the internal turn-in item ID: " + longLine);
        }
        if (!longLine.contains("surface route near sector 1,1 zone 2,3 floor 4")) {
            throw new AssertionError("Contract display did not present a readable location: " + longLine);
        }
    }
}
