package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class InventoryReadabilityAuthority {
    private InventoryReadabilityAuthority() {}

    static List<String> detailLines(String item, boolean stored, boolean equippedLeft, boolean equippedRight,
                                    int carriedWeight, int carryCapacity, ItemProvenanceRecord provenance) {
        ArrayList<String> lines = new ArrayList<>();
        if (item == null || item.isBlank()) {
            lines.add("No item selected.");
            return lines;
        }
        ItemDef definition = ItemCatalog.get(item);
        int quality = ItemQuality.tierIndex(item);
        lines.add(item);
        lines.add("Quality: " + ItemQuality.NAMES[Math.max(0, Math.min(ItemQuality.NAMES.length - 1, quality))] + ".");
        lines.add("Category: " + (definition == null ? "uncataloged item" : definition.category) + ".");
        lines.add("Condition: no separate damage or durability record is attached to this stack.");
        lines.add("Status: " + legalityLabel(item, definition) + ".");
        if (equippedLeft || equippedRight) {
            lines.add("Equipped: " + (equippedLeft && equippedRight ? "both hands" : equippedLeft ? "left hand" : "right hand") + ".");
        } else {
            lines.add("Equipped: no.");
        }
        lines.add(stored ? "Transfer result: taking moves this item into carried inventory."
                : "Transfer result: storing moves this item into base storage.");
        lines.add("Carried load: " + Math.max(0, carriedWeight) + "/" + Math.max(0, carryCapacity) + ".");
        if (definition != null) lines.add("Use: " + definition.use + ".");
        lines.add(provenance == null ? "Provenance: no trace record is currently attached."
                : "Provenance: " + provenance.shortChain() + ".");
        return lines;
    }

    static String legalityLabel(String item, ItemDef definition) {
        String text = ((item == null ? "" : item) + " "
                + (definition == null ? "" : definition.category) + " "
                + (definition == null ? "" : definition.description)).toLowerCase(Locale.ROOT);
        if (containsAny(text, "cult", "heretic", "warp", "blasphem", "forbidden")) return "forbidden or corruption-linked";
        if (containsAny(text, "contraband", "illegal", "stolen", "restricted")) return "restricted or illicit";
        if (containsAny(text, "weapon", "ammo", "explosive", "security")) return "regulated equipment";
        return "no obvious restriction";
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) if (text.contains(needle)) return true;
        return false;
    }
}
