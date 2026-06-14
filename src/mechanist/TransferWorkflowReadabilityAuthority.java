package mechanist;

import java.util.ArrayList;
import java.util.List;

final class TransferWorkflowReadabilityAuthority {
    private TransferWorkflowReadabilityAuthority() {}

    static List<String> describe(String source, String destination, String item, int quantity,
                                 String permission, boolean capacityAvailable, boolean protectedItem,
                                 boolean reversible, String blockedReason) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Transfer: " + safe(source, "source") + " -> " + safe(destination, "destination")
                + "; quantity " + Math.max(1, quantity) + "; " + safe(item, "no item selected") + ".");
        lines.add("Permission: " + safe(permission, "current interaction authority") + ".");
        lines.add(capacityAvailable ? "Capacity: destination can accept this transfer."
                : "Capacity: transfer blocked" + (blockedReason == null || blockedReason.isBlank() ? "." : " - " + blockedReason + "."));
        if (protectedItem) lines.add("Protection warning: mission, evidence, or intelligence item; verify the destination before confirming.");
        lines.add(reversible ? "Reversibility: the item can be moved back through the paired transfer action while access remains available."
                : "Reversibility: this transaction is not automatically reversible through the current panel.");
        lines.add("Confirmation: the action button applies one item immediately; Cancel or Back leaves inventory unchanged.");
        return lines;
    }

    static boolean protectedItem(String item) {
        String risk = ContainerReadabilityAuthority.riskLabel(item);
        return risk.startsWith("mission, evidence, or intelligence");
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
