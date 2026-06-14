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
        lines.addAll(TransferWorkflowReadabilityAuthority.describe("vendor stock", "carried inventory", offer.name, 1,
                "vendor purchase access", remaining >= 0 && carriedWeight < carryCapacity,
                TransferWorkflowReadabilityAuthority.protectedItem(offer.name), false,
                remaining < 0 ? "insufficient script" : carriedWeight >= carryCapacity ? "carrying load is full" : null));
        if (offer.provenance != null) lines.add("Provenance: " + offer.provenance.shortChain() + ".");
        return lines;
    }

    static List<String> marketContext(TraderSession trader, String factionLabel, int standing) {
        ArrayList<String> lines = new ArrayList<>();
        if (trader == null) return List.of("Market context: no active vendor session.");
        lines.add("Market affiliation: " + safe(factionLabel, "local independent trade") + "; standing " + standingBand(standing) + ".");
        lines.add(trader.offers == null || trader.offers.isEmpty()
                ? "Stock unavailable: this vendor currently has no accessible goods."
                : "Accessible stock: " + trader.offers.size() + " offer(s); unavailable or restricted goods are not silently listed as purchasable.");
        lines.add(trader.supplyChainSummary == null || trader.supplyChainSummary.isBlank()
                ? "Supply context: no additional shipment or scarcity record is available."
                : "Supply context: " + trader.supplyChainSummary);
        lines.add("Service scope: this panel supports item buying and selling only; repairs, treatment, lodging, banking, training, and other services require their owning interaction surface.");
        return lines;
    }

    static List<String> salePreview(String item, int price) {
        return salePreview(item, price, null);
    }

    static List<String> salePreview(String item, int price, ItemProvenanceRecord provenance) {
        ArrayList<String> lines = new ArrayList<>();
        if (item == null || item.isBlank()) {
            lines.add("Sale preview: no carried item selected.");
            return lines;
        }
        boolean protectedItem = TransferWorkflowReadabilityAuthority.protectedItem(item);
        ProductionDefectAppraisalAuthority.Appraisal appraisal = ProductionDefectAppraisalAuthority.appraise(price, provenance);
        lines.add("Sale preview: " + item + " for " + appraisal.adjustedPrice() + " script.");
        if (appraisal.defectFlagged()) lines.addAll(appraisal.lines());
        lines.add(protectedItem
                ? "Sale blocked: mission, evidence, or intelligence items require a dedicated hand-in or explicit release flow."
                : "Sale available: confirming removes one item from carried inventory and is not automatically reversible.");
        return lines;
    }

    static boolean saleAllowed(String item) {
        return item != null && !item.isBlank() && !TransferWorkflowReadabilityAuthority.protectedItem(item);
    }

    private static String standingBand(int standing) {
        if (standing >= 25) return "trusted (" + standing + ")";
        if (standing >= 5) return "favorable (" + standing + ")";
        if (standing <= -25) return "hostile (" + standing + ")";
        if (standing <= -5) return "strained (" + standing + ")";
        return "neutral (" + standing + ")";
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
