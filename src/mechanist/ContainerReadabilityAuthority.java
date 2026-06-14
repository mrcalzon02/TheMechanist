package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ContainerReadabilityAuthority {
    private ContainerReadabilityAuthority() {}

    static List<String> transferPreview(String containerLabel, int itemCount, ItemInstance selected,
                                        String carriedItem, int carriedWeight, int carryCapacity) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Storage: " + safe(containerLabel, "Container") + ".");
        lines.add("Access: available through the current interaction; no separate lock or ownership record is attached.");
        lines.add("Capacity: " + Math.max(0, itemCount) + " stored item(s); this container has no enforced capacity record.");
        lines.add("Carried load: " + Math.max(0, carriedWeight) + "/" + Math.max(0, carryCapacity) + ".");
        if (selected == null) {
            lines.add("Take preview: no container item selected.");
        } else {
            lines.add("Take preview: " + selected.displayName + (carriedWeight < carryCapacity
                    ? " moves into carried inventory."
                    : " is blocked because carrying load is full."));
            lines.add("Selected item status: " + riskLabel(selected.displayName) + ".");
            lines.add(selected.provenance == null ? "Selected item provenance: untraced."
                    : "Selected item provenance: " + selected.provenance.shortChain() + ".");
        }
        lines.add(carriedItem == null || carriedItem.isBlank()
                ? "Put preview: no carried item selected."
                : "Put preview: " + carriedItem + " moves into this storage. " + warningForDeposit(carriedItem));
        String workflowItem = selected != null ? selected.displayName : carriedItem;
        lines.addAll(TransferWorkflowReadabilityAuthority.describe(
                selected != null ? safe(containerLabel, "Container") : "carried inventory",
                selected != null ? "carried inventory" : safe(containerLabel, "Container"), workflowItem, 1,
                "current container interaction access", selected == null || carriedWeight < carryCapacity,
                TransferWorkflowReadabilityAuthority.protectedItem(workflowItem), true,
                selected != null && carriedWeight >= carryCapacity ? "carrying load is full" : null));
        return lines;
    }

    static String riskLabel(String item) {
        String text = item == null ? "" : item.toLowerCase(Locale.ROOT);
        if (containsAny(text, "quest", "journal", "lockbox", "evidence", "proof")) return "mission, evidence, or intelligence item; transfer with care";
        if (containsAny(text, "cult", "heretic", "warp", "witchsalt", "blasphem")) return "forbidden or corruption-linked item";
        if (containsAny(text, "contraband", "stolen", "illegal", "restricted")) return "restricted or illicit item";
        if (containsAny(text, "explosive", "mine", "grenade", "volatile", "fuel")) return "volatile or hazardous item";
        return "ordinary stored item";
    }

    private static String warningForDeposit(String item) {
        String risk = riskLabel(item);
        return "ordinary stored item".equals(risk) ? "No obvious storage warning." : "Warning: " + risk + ".";
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) if (text.contains(needle)) return true;
        return false;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
