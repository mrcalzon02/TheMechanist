package mechanist;

import mechanist.assets.AssetManager;
import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetType;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Stage 4 bridge between carried item/UI preview labels and the Semantic Asset Registry.
 *
 * This authority deliberately maps player-visible item names to 8-character semantic
 * asset IDs before legacy icon aliases are consulted. It is not yet the final item
 * definition schema; it is the migration bridge that prevents high-error previews
 * (scrap knives as bolters, water barrels as shelves, cots as unrelated art, etc.)
 * while later stages add durable assetId fields to every catalog/fixture/tile entry.
 */
final class ItemSemanticAssetAuthority {
    static final String VERSION = "item-semantic-asset-authority-0.9.10kf";
    static final String MISSING_RECOGNIZED_ITEM_ID = "MISSING-SEMANTIC-ITEM";
    private static final Map<String, String> EXACT = new LinkedHashMap<>();
    private static final Set<AssetType> ITEM_ASSET_TYPES = Set.of(
            AssetType.ITEM_ICON, AssetType.WEAPON_ICON, AssetType.ARMOR_ICON,
            AssetType.OBJECT, AssetType.FIXTURE, AssetType.MACHINE);

    static {
        // High-error exact names from the reconciliation crosswalk.
        map("water barrel", "OBJ-WB01");
        map("domestic water storage fixture", "OBJ-WB01");
        map("water dispenser", "OBJ-WD01");
        map("supply shelf", "OBJ-SH01");
        map("sleeping cot", "OBJ-CT01");
        map("cot", "OBJ-CT01");
        map("hab cot", "OBJ-CT01");
        map("hab cot fixture", "OBJ-CT01");
        map("worn bed", "OBJ-BD01");
        map("guard cot", "DOM-0102");
        map("military cot", "DOM-0102");
        map("bunk bed", "DOM-0105");

        map("scrap knife", "WEAP-K01");
        map("rusty knife", "WEAP-K01");
        map("tiny knife", "WEAP-K01");
        map("shiv", "WEAP-K01");
        map("combat knife", "WP1-0302");
        map("bolter", "WEAP-B01");
        map("bolt pistol", "WEAP-B01");
        map("heavy bolter", "WP3-0201");
        map("heavy flamer", "WP3-0202");
        map("multi-melta", "WP3-0203");
        map("heavy stubber", "WP3-0204");
        map("autocannon", "WP3-0205");
        map("shotgun", "WP3-0101");
        map("sniper rifle", "WP3-0102");
        map("marksman rifle", "WP3-0103");
        map("hunting rifle", "WP3-0104");
        map("stubcarbine", "WP3-0105");
        map("stub carbine", "WP3-0105");

        map("newspaper", "ITEM-N01");
        map("scavenger rags", "WP3-0302");
        map("arbites armor", "ARMR-A01");
        map("arbites patrol coat", "ARMR-A01");
        map("pdf armor", "WP3-0304");
        map("servant clothing", "WP3-0305");
    }

    private ItemSemanticAssetAuthority() {}

    static String semanticAssetIdForItemName(String rawName) {
        String name = normalizedItemName(rawName);
        if (name.isBlank()) return "ITEM-G01";

        String exact = EXACT.get(name);
        if (exact != null) return exact;

        // Specific high-error and weapon-family classifiers must run before broad buckets.
        if (containsAny(name, "heavy bolter")) return "WP3-0201";
        if (containsAny(name, "heavy flamer")) return "WP3-0202";
        if (containsAny(name, "multi melta", "multi-melta")) return "WP3-0203";
        if (containsAny(name, "heavy stubber")) return "WP3-0204";
        if (containsAny(name, "autocannon")) return "WP3-0205";
        if (containsAny(name, "sniper rifle")) return "WP3-0102";
        if (containsAny(name, "marksman rifle")) return "WP3-0103";
        if (containsAny(name, "hunting rifle")) return "WP3-0104";
        if (containsAny(name, "stubcarbine", "stub carbine")) return "WP3-0105";
        if (containsAny(name, "shotgun")) return "WP3-0101";

        if (containsAny(name, "knife", "shiv", "dagger", "scalpel")) return "WEAP-K01";
        if (containsAny(name, "bolter", "bolt pistol")) return "WEAP-B01";
        if (containsAny(name, "lasgun", "laspistol", "hellgun", "hot shot", "hot-shot", "lascannon")) return "WP2-0202";
        if (containsAny(name, "rifle", "carbine", "autogun", "stub gun")) return "WP3-0104";
        if (containsAny(name, "revolver", "pistol", "handgun")) return "WP2-0105";
        if (containsAny(name, "maul", "club", "baton", "hammer", "mace", "spanner", "wrench")) return "WP1-0202";
        if (containsAny(name, "sword", "blade", "chainblade", "chainsword")) return "WP1-0204";
        if (containsAny(name, "axe", "hatchet")) return "WP1-0205";
        if (containsAny(name, "spear", "polearm")) return "WP1-0402";

        if (containsAny(name, "scavenger rags", "scavenger wraps")) return "WP3-0302";
        if (containsAny(name, "arbites")) return "ARMR-A01";
        if (containsAny(name, "pdf armor", "guard armor")) return "WP3-0304";
        if (containsAny(name, "servant")) return "WP3-0305";
        if (containsAny(name, "armor", "armour", "flak", "carapace", "vest", "leathers", "helmet", "helm")) return "ARMR-A01";
        if (containsAny(name, "clothing", "coat", "robe", "uniform", "rags", "coverall", "workwear", "overalls", "leathers")) return "WP3-0302";

        if (containsAny(name, "newspaper")) return "ITEM-N01";
        if (containsAny(name, "paper", "pamphlet", "book", "ledger", "journal", "manual", "dossier", "map", "scroll", "slate", "permit", "signet")) return "ITEM-N01";
        if (containsAny(name, "water", "canteen", "bottle", "flask", "thermos")) return "OBJ-WB01";
        if (containsAny(name, "cot", "bed", "bunk", "berth")) return "OBJ-CT01";
        if (containsAny(name, "crate", "box", "pouch", "pack", "canister", "container", "bundle", "case", "shelf")) return "OBJ-SH01";
        if (containsAny(name, "condenser")) return "MACH-C01";
        if (containsAny(name, "assembler")) return "MACH-A01";
        if (containsAny(name, "boiler")) return "MACH-B01";
        if (containsAny(name, "forge", "smelter")) return "MACH-F01";

        // Carry the recognized-family state into legacy image callers. The unknown ID is
        // intentionally absent from the registry so AssetManager returns a typed missing icon.
        if (SemanticRenderIntentAuthority.itemIntent(rawName).isPresent()) {
            return MISSING_RECOGNIZED_ITEM_ID;
        }
        return "ITEM-G01";
    }

    static Optional<String> runtimeAssetIdForItemName(String rawName) {
        String hint = semanticAssetIdForItemName(rawName);
        String semanticName = normalizedItemName(rawName);

        // Preserve exact and structured authored identities whenever the active registry can satisfy them.
        if (!"ITEM-G01".equals(hint) && !MISSING_RECOGNIZED_ITEM_ID.equals(hint)) {
            Optional<String> authored = SemanticAssetHintResolver.resolve(hint, semanticName, ITEM_ASSET_TYPES);
            if (authored.isPresent()) return authored;
        }

        Optional<SemanticRenderAssetResolver.RenderIntent> intent =
                SemanticRenderIntentAuthority.itemIntent(rawName);
        if (intent.isPresent()) {
            return Optional.of(SemanticRenderIntentAuthority.resolve(AssetManager.registry(), intent.get())
                    .orElse(MISSING_RECOGNIZED_ITEM_ID));
        }

        return SemanticAssetHintResolver.resolve(hint, semanticName, ITEM_ASSET_TYPES);
    }

    static Optional<AssetMetadata> metadataForItemName(String rawName) {
        return runtimeAssetIdForItemName(rawName).flatMap(AssetManager::metadata);
    }

    static String semanticSummaryForItemName(String rawName) {
        String hint = semanticAssetIdForItemName(rawName);
        Optional<AssetMetadata> meta = metadataForItemName(rawName);
        if (meta.isEmpty()) return "Semantic asset hint: " + hint + " (no compatible active-registry asset).";
        AssetMetadata m = meta.get();
        return "Semantic asset: " + m.id() + " / " + m.type().displayName() + " / " + m.name()
                + " / authoredHint=" + hint + ".";
    }

    static String auditSummary() {
        return "authority=" + VERSION + " exactMappings=" + EXACT.size()
                + " authoredFirst=true strictFamilyFallback=true recognizedFamiliesFailClosed=true"
                + " typedMissingFallbackId=" + MISSING_RECOGNIZED_ITEM_ID + " activeRegistryValidated=true";
    }

    private static void map(String token, String assetId) {
        EXACT.put(normalizedItemName(token), assetId);
    }

    private static String normalizedItemName(String raw) {
        if (raw == null) return "";
        String s = ItemQuality.stripManufacturingIdentity(ItemQuality.stripQuality(raw)).toLowerCase(Locale.ROOT);
        s = s.replace('‑', '-').replace('–', '-').replace('—', '-');
        s = s.replaceAll("[^a-z0-9+./ -]+", " ").replaceAll("\\s+", " ").trim();
        return s;
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && text.contains(needle.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }
}
