package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
        return salePreview(item, price, provenance, Set.of());
    }

    static List<String> salePreview(String item, int price, ItemProvenanceRecord provenance, Set<String> unlockedSkillNodes) {
        ArrayList<String> lines = new ArrayList<>();
        if (item == null || item.isBlank()) {
            lines.add("Sale preview: no carried item selected.");
            return lines;
        }
        boolean protectedItem = TransferWorkflowReadabilityAuthority.protectedItem(item);
        ProductionDefectAppraisalAuthority.Appraisal appraisal = ProductionDefectAppraisalAuthority.appraise(price, provenance);
        lines.add("Sale preview: " + item + " for " + appraisal.adjustedPrice() + " script.");
        if (appraisal.defectFlagged()) lines.addAll(appraisal.lines());
        lines.addAll(streetwiseAppraisalLines(provenance, unlockedSkillNodes));
        lines.addAll(certifiedAppraisalLines(provenance, unlockedSkillNodes));
        lines.add(protectedItem
                ? "Sale blocked: mission, evidence, or intelligence items require a dedicated hand-in or explicit release flow."
                : "Sale available: confirming removes one item from carried inventory and is not automatically reversible.");
        return lines;
    }

    static boolean saleAllowed(String item) {
        return item != null && !item.isBlank() && !TransferWorkflowReadabilityAuthority.protectedItem(item);
    }

    static List<String> streetwiseAppraisalLines(ItemProvenanceRecord provenance, Set<String> unlockedSkillNodes) {
        ArrayList<String> lines = new ArrayList<>();
        boolean trained = SkillTreeProgressionAuthority.hasCapability(unlockedSkillNodes, "trade-streetwise-appraisal");
        boolean risky = hasStreetRisk(provenance);
        if (!trained) {
            if (risky) {
                lines.add("Street appraisal: recorded batch or legal provenance may narrow buyers; trained Streetwise Appraisal would call out the risk before sale.");
            }
            return lines;
        }
        lines.add("Streetwise Appraisal: trained street-market judgment is active for this sale preview.");
        if (provenance == null) {
            lines.add("Streetwise Appraisal: provenance is missing, so only ordinary price and item name can be judged.");
            return lines;
        }
        if ("defect flagged".equalsIgnoreCase(provenance.defectState)) {
            lines.add("Streetwise Appraisal: confirms the recorded defect is the ordinary resale limiter.");
        }
        if (hasRiskText(provenance.batchIssueTags)) {
            lines.add("Streetwise Appraisal: buyer-risk tags noticed - " + readableRiskText(provenance.batchIssueTags) + ".");
        }
        if (hasRiskText(provenance.productionLegalStatus)) {
            lines.add("Streetwise Appraisal: legal status may narrow safe buyers - " + readableRiskText(provenance.productionLegalStatus) + ".");
        }
        if (!hasStreetRisk(provenance)) {
            lines.add("Streetwise Appraisal: no recorded street-market risk beyond ordinary haggling.");
        }
        lines.add("Streetwise boundary: this preview improves risk detection; it does not override protected hand-ins, law enforcement, or defect resale math.");
        return lines;
    }

    static List<String> certifiedAppraisalLines(ItemProvenanceRecord provenance, Set<String> unlockedSkillNodes) {
        ArrayList<String> lines = new ArrayList<>();
        boolean trained = SkillTreeProgressionAuthority.hasCapability(unlockedSkillNodes, "trade-guilder-certification");
        boolean certificateVisible = hasCertificateText(provenance);
        if (!trained) {
            if (certificateVisible) {
                lines.add("Certified appraisal: recorded certificate or legal provenance exists; trained Certified Market Appraisal would separate formal proof from ordinary item naming.");
            }
            return lines;
        }
        lines.add("Certified Market Appraisal: trained certificate review is active for this sale preview.");
        if (provenance == null) {
            lines.add("Certified Market Appraisal: provenance is missing, so there is no certificate or legal chain to review.");
            return lines;
        }
        if (hasText(provenance.batchIssueTags, "faction-certified")) {
            lines.add("Certified Market Appraisal: faction-certified batch proof is recognized as formal trade evidence.");
        }
        if (!provenance.productionLegalStatus.isBlank()) {
            lines.add("Certified Market Appraisal: legal status record available - "
                    + readableRiskText(provenance.productionLegalStatus) + ".");
        }
        if (!certificateVisible) {
            lines.add("Certified Market Appraisal: no formal certificate or legal provenance is recorded.");
        }
        lines.add("Certified appraisal boundary: this preview explains formal proof; it does not bypass faction access, protected hand-ins, or buyer policy.");
        return lines;
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

    private static boolean hasStreetRisk(ItemProvenanceRecord provenance) {
        return provenance != null && ("defect flagged".equalsIgnoreCase(provenance.defectState)
                || hasRiskText(provenance.batchIssueTags) || hasRiskText(provenance.productionLegalStatus));
    }

    private static boolean hasCertificateText(ItemProvenanceRecord provenance) {
        return provenance != null && (hasText(provenance.batchIssueTags, "faction-certified")
                || !provenance.productionLegalStatus.isBlank());
    }

    private static boolean hasRiskText(String text) {
        String value = safe(text, "").toLowerCase(Locale.ROOT);
        return containsAny(value, "defect", "counterfeit", "contaminated", "unstable", "stolen", "restricted",
                "contraband", "black-market", "hostile", "profane", "illegal");
    }

    private static boolean hasText(String text, String needle) {
        return safe(text, "").toLowerCase(Locale.ROOT).contains(safe(needle, "").toLowerCase(Locale.ROOT));
    }

    private static String readableRiskText(String text) {
        String value = safe(text, "unrecorded risk");
        value = value.replace('_', ' ').replace('-', ' ').replace(';', ',');
        return value;
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) if (text.contains(needle)) return true;
        return false;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
