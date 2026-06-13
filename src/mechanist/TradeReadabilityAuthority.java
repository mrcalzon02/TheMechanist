package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class TradeReadabilityAuthority {
    private TradeReadabilityAuthority() {}

    static List<String> offerPreview(TraderSession trader, TradeOffer offer, int carriedScript, int carriedWeight, int carryCapacity) {
        ArrayList<String> lines = new ArrayList<>();
        if (trader == null) {
            lines.add("No trader session is available.");
            return lines;
        }
        lines.add("Vendor: " + safe(trader.name, "Store") + " / " + safe(trader.archetype, "trader") + ".");
        if (offer == null) {
            lines.add("Select an offer to preview price, quality, legality, and inventory result.");
            return lines;
        }

        int price = trader.buyPrice(offer);
        int remaining = carriedScript - price;
        lines.add("Price: " + price + " script / you carry " + Math.max(0, carriedScript) + ".");
        lines.add(remaining >= 0 ? "Affordable: purchase leaves " + remaining + " script."
                : "Unavailable: need " + Math.abs(remaining) + " more script.");
        int quality = ItemQuality.tierIndex(offer.name);
        lines.add("Quality: " + ItemQuality.NAMES[Math.max(0, Math.min(ItemQuality.NAMES.length - 1, quality))] + ".");
        lines.add("Status: " + legalityLabel(offer) + ".");
        lines.add(carriedWeight < carryCapacity
                ? "Inventory result: item enters carried inventory."
                : "Inventory result: carrying load is full; purchase is blocked.");
        if (offer.provenance != null) lines.add("Provenance: " + offer.provenance.shortChain() + ".");
        return lines;
    }

    static String legalityLabel(TradeOffer offer) {
        if (offer == null) return "unknown";
        String text = (safe(offer.name, "") + " " + safe(offer.category, "") + " " + safe(offer.description, "")).toLowerCase(Locale.ROOT);
        if (containsAny(text, "cult", "heretic", "warp", "blasphem", "forbidden")) return "forbidden or corruption-linked goods; possession may attract severe consequences";
        if (containsAny(text, "contraband", "illegal", "stolen", "interrogation", "restricted")) return "restricted or illicit goods; possession may create legal or faction risk";
        if (containsAny(text, "weapon", "ammo", "security", "explosive")) return "regulated equipment; access and local enforcement may matter";
        return "ordinary market stock with no obvious restriction";
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) if (text.contains(needle)) return true;
        return false;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
