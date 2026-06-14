package mechanist;

import java.util.ArrayList;
import java.util.List;

final class ContractObjectiveReadabilityAuthority {
    private ContractObjectiveReadabilityAuthority() {}

    static List<String> summary(List<FactionContract> contracts, List<String> carriedItems, List<String> storedItems, int limit) {
        ArrayList<String> lines = new ArrayList<>();
        int active = 0;
        if (contracts != null) for (FactionContract contract : contracts) if (contract != null && !contract.completed) active++;
        lines.add("Active contracts: " + active + ".");
        if (active == 0) {
            lines.add("No active faction contract objectives are recorded.");
            return lines;
        }

        int shown = 0;
        for (FactionContract contract : contracts) {
            if (contract == null || contract.completed || shown >= Math.max(1, limit)) continue;
            String item = contract.publicRequiredItem();
            boolean carried = containsNamed(carriedItems, contract.requiredTurnInItem);
            boolean stored = containsNamed(storedItems, contract.requiredTurnInItem);
            lines.add(contract.displayFactionName() + " / " + contract.displayType() + ": " + objectiveText(contract) + ".");
            lines.add("Route: " + contract.displayLocation() + "; "
                    + (contract.spawned ? "target or objective confirmed" : "contract route recorded; exact local target not confirmed") + ".");
            lines.add("Required proof or delivery: " + item + " / "
                    + (carried ? "carried and ready for turn-in" : stored ? "held in base storage; retrieve before turn-in" : "not currently held") + ".");
            lines.add("Reward: " + contract.payout + " script and faction standing +" + contract.repReward + ".");
            shown++;
        }
        if (active > shown) lines.add((active - shown) + " additional active contract(s) not shown in this compact view.");
        lines.add("Evidence boundary: hidden identities and exact local targets remain undisclosed until the contract route confirms them.");
        return lines;
    }

    private static String objectiveText(FactionContract contract) {
        if ("BOUNTY".equals(contract.type)) return "find " + safe(contract.targetName, "the marked target") + " and recover " + contract.publicRequiredItem();
        if ("LOCKBOX".equals(contract.type)) return "acquire " + contract.publicRequiredItem() + " from the named vault route";
        return "retrieve " + safe(contract.targetName, contract.publicRequiredItem()) + " without compromising it";
    }

    private static boolean containsNamed(List<String> items, String wanted) {
        if (items == null || wanted == null || wanted.isBlank()) return false;
        for (String item : items) if (ItemQuality.namesMatch(item, wanted)) return true;
        return false;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
