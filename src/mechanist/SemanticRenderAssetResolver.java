package mechanist;

import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetRegistry;
import mechanist.assets.AssetType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Semantic bridge from renderer intent to indexed graphical asset metadata.
 *
 * This is the repair-path layer that sits between live renderers and the
 * existing semantic asset indexes. Callers ask for meaning such as sewer floor,
 * generic wall, streetlight fixture, closed door, room furniture, zone theme,
 * container class, or item icon. This resolver searches the semantic registry
 * and refuses known-bad cross-theme fallbacks.
 */
final class SemanticRenderAssetResolver {
    static final String VERSION = "semantic-render-asset-resolver-1.19-ammunition-not-weapon";

    enum RenderIntent {
        SEWER_FLOOR, SEWER_WALL, GENERIC_FLOOR, GENERIC_WALL, INDUSTRIAL_FLOOR, INDUSTRIAL_WALL,
        HABITATION_FLOOR, HABITATION_WALL, MARKET_FLOOR, MARKET_WALL, MEDICAL_FLOOR, SECURITY_FLOOR,
        ADMINISTRATIVE_FLOOR, RELIGIOUS_FLOOR, TRANSIT_FLOOR, WAREHOUSE_FLOOR, NOBLE_FLOOR, SLUM_FLOOR,
        STREETLIGHT_FIXTURE, TRAFFIC_LIGHT_FIXTURE, GENERATOR_MACHINE, TRANSFORMER_MACHINE,
        JUNCTION_BOX_FIXTURE, VENTILATION_UNIT_FIXTURE, WATER_PIPE_FIXTURE, SEWER_PIPE_FIXTURE,
        SECURITY_CAMERA_FIXTURE, DOOR_CLOSED, DOOR_OPEN, WORKSHOP_TABLE, DINING_TABLE, MEDICAL_TABLE,
        SHRINE_ALTAR, MARKET_COUNTER, ADMINISTRATIVE_DESK, INTERROGATION_DESK, TOOLBOX_CONTAINER,
        MEDICAL_CABINET_CONTAINER, WEAPONS_LOCKER_CONTAINER, WARDROBE_CONTAINER, CARGO_CONTAINER,
        FILING_CABINET_CONTAINER, REFRIGERATED_STORAGE_CONTAINER, WEAPON_ITEM_ICON, ARMOR_ITEM_ICON,
        TOOL_ITEM_ICON, MEDICAL_ITEM_ICON, DRUG_ITEM_ICON, FOOD_ITEM_ICON, INDUSTRIAL_COMPONENT_ITEM_ICON,
        TRADE_GOOD_ITEM_ICON, RELIGIOUS_OBJECT_ITEM_ICON, DATA_DEVICE_ITEM_ICON
    }

    static final class Resolution {
        final RenderIntent intent;
        final AssetMetadata asset;
        final String reason;
        private Resolution(RenderIntent intent, AssetMetadata asset, String reason) {
            this.intent = intent; this.asset = asset; this.reason = reason == null ? "" : reason;
        }
        static Resolution found(RenderIntent intent, AssetMetadata asset, String reason) { return new Resolution(intent, asset, reason); }
        static Resolution missing(RenderIntent intent, String reason) { return new Resolution(intent, null, reason); }
        boolean found() { return asset != null; }
        String assetIdOrMissing() { return asset == null ? "<missing>" : asset.id(); }
    }

    private SemanticRenderAssetResolver() {}

    static Resolution resolve(AssetRegistry registry, RenderIntent intent) {
        AssetRegistry safe = registry == null ? AssetRegistry.empty() : registry;
        List<AssetMetadata> candidates = candidatesFor(safe, intent);
        if (candidates.isEmpty()) return Resolution.missing(intent, "No semantic asset matched " + intent + ". Renderer must use explicit missing-art fallback, not unrelated art.");
        return Resolution.found(intent, candidates.get(0), "Matched indexed semantic asset for " + intent + ".");
    }

    static Resolution resolve(AssetRegistry registry, RenderIntent intent, long stableVariantKey) {
        AssetRegistry safe = registry == null ? AssetRegistry.empty() : registry;
        List<AssetMetadata> candidates = candidatesFor(safe, intent);
        if (candidates.isEmpty()) return Resolution.missing(intent, "No semantic asset matched " + intent + ". Renderer must use explicit missing-art fallback, not unrelated art.");
        int topPriority = priority(candidates.get(0), intent);
        int topCount = 1;
        while (topCount < candidates.size() && priority(candidates.get(topCount), intent) == topPriority) topCount++;
        int variantIndex = Math.floorMod(Long.hashCode(stableVariantKey), topCount);
        return Resolution.found(intent, candidates.get(variantIndex), "Matched stable variant " + (variantIndex + 1) + "/" + topCount + " from top-priority semantic assets for " + intent + ".");
    }

    static Optional<AssetMetadata> optional(AssetRegistry registry, RenderIntent intent) { Resolution r = resolve(registry, intent); return r.found() ? Optional.of(r.asset) : Optional.empty(); }
    static Optional<AssetMetadata> optional(AssetRegistry registry, RenderIntent intent, long stableVariantKey) { Resolution r = resolve(registry, intent, stableVariantKey); return r.found() ? Optional.of(r.asset) : Optional.empty(); }

    static boolean canUse(AssetMetadata asset, RenderIntent intent) {
        if (asset == null || intent == null) return false;
        String h = haystack(asset);
        return switch (intent) {
            case SEWER_FLOOR -> isFloor(asset) && themed(h,"sewer","sump","drain","utility tunnel") && !generic(h);
            case SEWER_WALL -> isWall(asset) && themed(h,"sewer","sump","drain","utility tunnel") && !generic(h);
            case GENERIC_FLOOR -> isFloor(asset) && generic(h) && !specializedSurface(h);
            case GENERIC_WALL -> isWall(asset) && generic(h) && !specializedSurface(h);
            case INDUSTRIAL_FLOOR -> isFloor(asset) && themed(h,"industrial","factory","machine shop","workshop") && !themed(h,"sewer");
            case INDUSTRIAL_WALL -> isWall(asset) && themed(h,"industrial","factory","machine shop","workshop") && !themed(h,"sewer");
            case HABITATION_FLOOR -> isFloor(asset) && themed(h,"habitation","hab","apartment","residential") && !themed(h,"sewer");
            case HABITATION_WALL -> isWall(asset) && themed(h,"habitation","hab","apartment","residential") && !themed(h,"sewer");
            case MARKET_FLOOR -> isFloor(asset) && themed(h,"market","bazaar","commercial","retail") && !themed(h,"sewer");
            case MARKET_WALL -> isWall(asset) && themed(h,"market","bazaar","commercial","retail") && !themed(h,"sewer");
            case MEDICAL_FLOOR -> isFloor(asset) && themed(h,"medical","clinic","hospital","surgery") && !themed(h,"sewer");
            case SECURITY_FLOOR -> isFloor(asset) && themed(h,"security","checkpoint","prison","brig") && !themed(h,"sewer");
            case ADMINISTRATIVE_FLOOR -> isFloor(asset) && themed(h,"administrative","office","records","bureau") && !themed(h,"sewer");
            case RELIGIOUS_FLOOR -> isFloor(asset) && themed(h,"religious","shrine","chapel","altar") && !themed(h,"sewer");
            case TRANSIT_FLOOR -> isFloor(asset) && themed(h,"transit","station","platform","rail") && !themed(h,"sewer");
            case WAREHOUSE_FLOOR -> isFloor(asset) && themed(h,"warehouse","storage","cargo","loading") && !themed(h,"sewer");
            case NOBLE_FLOOR -> isFloor(asset) && themed(h,"noble","luxury","estate","manor") && !themed(h,"sewer");
            case SLUM_FLOOR -> isFloor(asset) && themed(h,"slum","shanty","tenement","scrap") && !themed(h,"sewer");
            case STREETLIGHT_FIXTURE -> fixtureType(asset) && themed(h,"streetlight","street light","lamp post","street lamp","lamppost") && notUiIcon(h);
            case TRAFFIC_LIGHT_FIXTURE -> fixtureType(asset) && themed(h,"traffic light","signal light","crossing signal") && notUiIcon(h);
            case GENERATOR_MACHINE -> machineType(asset) && themed(h,"generator","power generator","genset") && notUiIcon(h);
            case TRANSFORMER_MACHINE -> machineType(asset) && themed(h,"transformer","power transformer","electrical transformer") && notUiIcon(h);
            case JUNCTION_BOX_FIXTURE -> fixtureType(asset) && themed(h,"junction box","electrical box","power box") && notUiIcon(h);
            case VENTILATION_UNIT_FIXTURE -> equipmentType(asset) && themed(h,"ventilation unit","vent unit","air handler","exhaust fan") && notUiIcon(h);
            case WATER_PIPE_FIXTURE -> fixtureType(asset) && themed(h,"water pipe","fresh water pipe","water main") && !themed(h,"sewer","waste","sludge") && notUiIcon(h);
            case SEWER_PIPE_FIXTURE -> fixtureType(asset) && themed(h,"sewer pipe","waste pipe","drain pipe","sludge pipe") && notUiIcon(h);
            case SECURITY_CAMERA_FIXTURE -> fixtureType(asset) && themed(h,"security camera","surveillance camera","cctv") && notUiIcon(h);
            case DOOR_CLOSED -> doorType(asset) && themed(h,"door","hatch","bulkhead") && !themed(h,"open","opened","unsealed") && !generic(h);
            case DOOR_OPEN -> doorType(asset) && themed(h,"door","hatch","bulkhead") && themed(h,"open","opened","unsealed") && !themed(h,"closed","shut") && !generic(h);
            case WORKSHOP_TABLE -> furnitureType(asset) && themed(h,"workshop table","workbench","fabrication table");
            case DINING_TABLE -> furnitureType(asset) && themed(h,"dining table","mess table","kitchen table");
            case MEDICAL_TABLE -> furnitureType(asset) && themed(h,"medical table","operating table","surgery table");
            case SHRINE_ALTAR -> furnitureType(asset) && themed(h,"altar","shrine");
            case MARKET_COUNTER -> furnitureType(asset) && themed(h,"market counter","shop counter","stall counter","trader counter");
            case ADMINISTRATIVE_DESK -> furnitureType(asset) && themed(h,"administrative desk","office desk","records desk");
            case INTERROGATION_DESK -> furnitureType(asset) && themed(h,"interrogation desk","security interview desk");
            case TOOLBOX_CONTAINER -> containerType(asset) && themed(h,"toolbox","tool box");
            case MEDICAL_CABINET_CONTAINER -> containerType(asset) && themed(h,"medical cabinet","medicine cabinet","clinic cabinet");
            case WEAPONS_LOCKER_CONTAINER -> containerType(asset) && themed(h,"weapons locker","weapon locker","armory locker","armoury locker");
            case WARDROBE_CONTAINER -> containerType(asset) && themed(h,"wardrobe","clothes cabinet","clothing cabinet");
            case CARGO_CONTAINER -> containerType(asset) && themed(h,"cargo container","shipping container","cargo crate","freight crate");
            case FILING_CABINET_CONTAINER -> containerType(asset) && themed(h,"filing cabinet","records cabinet","file cabinet");
            case REFRIGERATED_STORAGE_CONTAINER -> containerType(asset) && themed(h,"refrigerated storage","cold storage","freezer","refrigerator","chiller locker");
            case WEAPON_ITEM_ICON -> weaponIcon(asset) && themed(h,"gun","blade","knife","knives","shiv","dagger","sword","axe","hatchet","spear","polearm","pistol","rifle","carbine","shotgun","bolter","flamer","melta","stubber","autocannon","lasgun","lascannon");
            case ARMOR_ITEM_ICON -> armorIcon(asset) && themed(h,"armor","armour","helmet","helm","vest","carapace","flak","clothing","coat","robe","uniform","rags","coverall","workwear","overalls");
            case TOOL_ITEM_ICON -> toolIcon(asset) && themed(h,"tool","wrench","spanner","hammer","shovel","spade","cutter","drill","saw","repair kit","maintenance kit","fabrication kit","maintenance tools");
            case MEDICAL_ITEM_ICON -> itemIcon(asset) && themed(h,"medkit","medical kit","bandage","suture","medicine","antiseptic","tourniquet","splint","first aid","trauma kit");
            case DRUG_ITEM_ICON -> itemIcon(asset) && themed(h,"drug","narcotic","stimulant","dose","injector","opiate","sedative","combat stim","painkiller");
            case FOOD_ITEM_ICON -> itemIcon(asset) && themed(h,"food","ration","meal","water","canteen","flask","drink","provisions","nutrient");
            case INDUSTRIAL_COMPONENT_ITEM_ICON -> itemIcon(asset) && themed(h,"component","machine part","industrial part","bearing","fastener","rivet","circuit","scrap plate","construction supplies","reagent");
            case TRADE_GOOD_ITEM_ICON -> tradeGoodIcon(asset) && themed(h,"trade good","commodity","barter","merchandise","wares","luxury good","cargo lot","export good","import good") && !themed(h,"junk","scrap heap","debris");
            case RELIGIOUS_OBJECT_ITEM_ICON -> itemIcon(asset) && themed(h,"relic","prayer","holy object","devotional","icon of faith","religious object","rosary","censer");
            case DATA_DEVICE_ITEM_ICON -> dataDeviceIcon(asset) && themed(h,"datapad","data pad","data device","data slate","dataslate","terminal","cogitator","chip","knowledge device","knowledge devices","skill device","memory core") && !themed(h,"system control","system button","interface control","rondel");
        };
    }

    static List<String> auditLines(AssetRegistry registry) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Semantic render asset resolver " + VERSION + ".");
        for (RenderIntent intent : RenderIntent.values()) { Resolution r = resolve(registry, intent); lines.add(intent + ": " + r.assetIdOrMissing() + " - " + r.reason); }
        return lines;
    }

    private static List<AssetMetadata> candidatesFor(AssetRegistry registry, RenderIntent intent) {
        return registry.all().stream().filter(asset -> canUse(asset, intent))
                .sorted(Comparator.comparing((AssetMetadata asset) -> priority(asset, intent)).reversed().thenComparing(AssetMetadata::name, String.CASE_INSENSITIVE_ORDER).thenComparing(AssetMetadata::id)).toList();
    }

    private static int priority(AssetMetadata asset, RenderIntent intent) {
        String h = haystack(asset); int score = 0;
        if (intent == RenderIntent.SEWER_FLOOR || intent == RenderIntent.SEWER_WALL) { if (contains(h,"sewer","sump")) score += 10; else if (contains(h,"drain","utility tunnel")) score += 4; }
        if (intent == RenderIntent.GENERIC_FLOOR && contains(h,"generic","plain","main floor","default")) score += 10;
        if (intent == RenderIntent.GENERIC_WALL && contains(h,"generic","plain","main wall","default")) score += 10;
        if (intent == RenderIntent.INDUSTRIAL_FLOOR || intent == RenderIntent.INDUSTRIAL_WALL) { if (contains(h,"industrial","factory")) score += 10; else if (contains(h,"machine shop","workshop")) score += 4; }
        if (intent == RenderIntent.HABITATION_FLOOR || intent == RenderIntent.HABITATION_WALL) { if (contains(h,"habitation","hab")) score += 10; else if (contains(h,"apartment","residential")) score += 4; }
        if (intent == RenderIntent.MARKET_FLOOR || intent == RenderIntent.MARKET_WALL) { if (contains(h,"market","bazaar")) score += 10; else if (contains(h,"commercial","retail")) score += 4; }
        if (intent == RenderIntent.MEDICAL_FLOOR) { if (contains(h,"medical","clinic","hospital")) score += 10; else if (contains(h,"surgery")) score += 4; }
        if (intent == RenderIntent.SECURITY_FLOOR) { if (contains(h,"security","checkpoint")) score += 10; else if (contains(h,"prison","brig")) score += 4; }
        if (intent == RenderIntent.ADMINISTRATIVE_FLOOR) { if (contains(h,"administrative","bureau")) score += 10; else if (contains(h,"office","records")) score += 4; }
        if (intent == RenderIntent.RELIGIOUS_FLOOR) { if (contains(h,"religious","shrine","chapel")) score += 10; else if (contains(h,"altar")) score += 4; }
        if (intent == RenderIntent.TRANSIT_FLOOR) { if (contains(h,"transit","platform","rail")) score += 10; else if (contains(h,"station")) score += 4; }
        if (intent == RenderIntent.WAREHOUSE_FLOOR) { if (contains(h,"warehouse","cargo","loading")) score += 10; else if (contains(h,"storage")) score += 4; }
        if (intent == RenderIntent.NOBLE_FLOOR) { if (contains(h,"noble","estate","manor")) score += 10; else if (contains(h,"luxury")) score += 4; }
        if (intent == RenderIntent.SLUM_FLOOR) { if (contains(h,"slum","shanty","tenement")) score += 10; else if (contains(h,"scrap")) score += 4; }
        if (intent == RenderIntent.STREETLIGHT_FIXTURE) { if (contains(h,"streetlight","street light")) score += 10; else if (contains(h,"lamp post","street lamp","lamppost")) score += 4; }
        if (intent == RenderIntent.TRAFFIC_LIGHT_FIXTURE) { if (contains(h,"traffic light")) score += 10; else if (contains(h,"signal light","crossing signal")) score += 4; }
        if (intent == RenderIntent.GENERATOR_MACHINE) { if (contains(h,"generator","power generator")) score += 10; else if (contains(h,"genset")) score += 4; }
        if (intent == RenderIntent.TRANSFORMER_MACHINE) { if (contains(h,"power transformer","electrical transformer")) score += 10; else if (contains(h,"transformer")) score += 4; }
        if (intent == RenderIntent.JUNCTION_BOX_FIXTURE) { if (contains(h,"junction box")) score += 10; else if (contains(h,"electrical box","power box")) score += 4; }
        if (intent == RenderIntent.VENTILATION_UNIT_FIXTURE) { if (contains(h,"ventilation unit","vent unit","air handler")) score += 10; else if (contains(h,"exhaust fan")) score += 4; }
        if (intent == RenderIntent.WATER_PIPE_FIXTURE) { if (contains(h,"water pipe","fresh water pipe")) score += 10; else if (contains(h,"water main")) score += 4; }
        if (intent == RenderIntent.SEWER_PIPE_FIXTURE) { if (contains(h,"sewer pipe")) score += 10; else if (contains(h,"waste pipe","drain pipe","sludge pipe")) score += 4; }
        if (intent == RenderIntent.SECURITY_CAMERA_FIXTURE) { if (contains(h,"security camera")) score += 10; else if (contains(h,"surveillance camera","cctv")) score += 4; }
        if (intent == RenderIntent.WORKSHOP_TABLE) { if (contains(h,"workshop table")) score += 10; else if (contains(h,"workbench","fabrication table")) score += 4; }
        if (intent == RenderIntent.DINING_TABLE) { if (contains(h,"dining table")) score += 10; else if (contains(h,"mess table","kitchen table")) score += 4; }
        if (intent == RenderIntent.MEDICAL_TABLE) { if (contains(h,"medical table")) score += 10; else if (contains(h,"operating table","surgery table")) score += 4; }
        if (intent == RenderIntent.SHRINE_ALTAR) { if (contains(h,"altar")) score += 10; else if (contains(h,"shrine")) score += 4; }
        if (intent == RenderIntent.MARKET_COUNTER) { if (contains(h,"market counter")) score += 10; else if (contains(h,"shop counter","stall counter","trader counter")) score += 4; }
        if (intent == RenderIntent.ADMINISTRATIVE_DESK) { if (contains(h,"administrative desk")) score += 10; else if (contains(h,"office desk","records desk")) score += 4; }
        if (intent == RenderIntent.INTERROGATION_DESK) { if (contains(h,"interrogation desk")) score += 10; else if (contains(h,"security interview desk")) score += 4; }
        if (intent == RenderIntent.TOOLBOX_CONTAINER && contains(h,"toolbox","tool box")) score += 10;
        if (intent == RenderIntent.MEDICAL_CABINET_CONTAINER) { if (contains(h,"medical cabinet")) score += 10; else if (contains(h,"medicine cabinet","clinic cabinet")) score += 4; }
        if (intent == RenderIntent.WEAPONS_LOCKER_CONTAINER) { if (contains(h,"weapons locker","weapon locker")) score += 10; else if (contains(h,"armory locker","armoury locker")) score += 4; }
        if (intent == RenderIntent.WARDROBE_CONTAINER) { if (contains(h,"wardrobe")) score += 10; else if (contains(h,"clothes cabinet","clothing cabinet")) score += 4; }
        if (intent == RenderIntent.CARGO_CONTAINER) { if (contains(h,"cargo container")) score += 10; else if (contains(h,"shipping container","cargo crate","freight crate")) score += 4; }
        if (intent == RenderIntent.FILING_CABINET_CONTAINER) { if (contains(h,"filing cabinet")) score += 10; else if (contains(h,"records cabinet","file cabinet")) score += 4; }
        if (intent == RenderIntent.REFRIGERATED_STORAGE_CONTAINER) { if (contains(h,"refrigerated storage")) score += 10; else if (contains(h,"cold storage","freezer","refrigerator","chiller locker")) score += 4; }
        if (intent == RenderIntent.WEAPON_ITEM_ICON) { if (contains(h,"knife","knives","shiv","dagger","sword","axe","hatchet","spear","polearm","pistol","rifle","carbine","shotgun","bolter","flamer","melta","stubber","autocannon","lasgun","lascannon")) score += 10; else if (contains(h,"gun","blade")) score += 4; }
        if (intent == RenderIntent.ARMOR_ITEM_ICON) { if (contains(h,"armor","armour","helmet","helm","vest","carapace","flak")) score += 10; else if (contains(h,"clothing","coat","robe","uniform","rags","coverall","workwear","overalls")) score += 4; }
        if (intent == RenderIntent.TOOL_ITEM_ICON) { if (contains(h,"wrench","spanner","hammer","shovel","spade","cutter","drill","saw","repair kit","maintenance kit","fabrication kit","maintenance tools")) score += 10; else if (contains(h,"tool")) score += 4; }
        if (intent == RenderIntent.MEDICAL_ITEM_ICON) { if (contains(h,"medkit","medical kit","bandage","suture","antiseptic","tourniquet","splint","first aid","trauma kit")) score += 10; else if (contains(h,"medicine")) score += 4; }
        if (intent == RenderIntent.DRUG_ITEM_ICON) { if (contains(h,"stimulant","injector","opiate","sedative","combat stim","painkiller")) score += 10; else if (contains(h,"drug","narcotic","dose")) score += 4; }
        if (intent == RenderIntent.FOOD_ITEM_ICON) { if (contains(h,"ration","meal","canteen","flask","drink")) score += 10; else if (contains(h,"food","water","provisions","nutrient")) score += 4; }
        if (intent == RenderIntent.INDUSTRIAL_COMPONENT_ITEM_ICON) { if (contains(h,"machine part","industrial part","bearing","fastener","rivet","circuit","scrap plate")) score += 10; else if (contains(h,"component","construction supplies","reagent")) score += 4; }
        if (intent == RenderIntent.TRADE_GOOD_ITEM_ICON) { if (contains(h,"trade good","luxury good","cargo lot","export good","import good")) score += 10; else if (contains(h,"commodity","barter","merchandise","wares")) score += 4; }
        if (intent == RenderIntent.RELIGIOUS_OBJECT_ITEM_ICON) { if (contains(h,"relic","holy object","devotional","icon of faith","religious object","rosary","censer")) score += 10; else if (contains(h,"prayer")) score += 4; }
        if (intent == RenderIntent.DATA_DEVICE_ITEM_ICON) { if (contains(h,"datapad","data pad","data slate","dataslate","cogitator","knowledge device","knowledge devices","skill device","memory core")) score += 10; else if (contains(h,"data device","terminal","chip")) score += 4; }
        if (intent == RenderIntent.DOOR_CLOSED && !contains(h,"open","opened","unsealed")) { if (contains(h,"door")) score += 10; else if (contains(h,"hatch","bulkhead")) score += 4; }
        if (intent == RenderIntent.DOOR_OPEN && contains(h,"open","opened","unsealed")) { if (contains(h,"door")) score += 10; else if (contains(h,"hatch","bulkhead")) score += 4; }
        return score;
    }

    private static boolean isFloor(AssetMetadata a) { return a.type() == AssetType.FLOOR_TILE; }
    private static boolean isWall(AssetMetadata a) { return a.type() == AssetType.WALL_TILE; }
    private static boolean furnitureType(AssetMetadata a) { return a.type() == AssetType.FIXTURE || a.type() == AssetType.OBJECT; }
    private static boolean fixtureType(AssetMetadata a) { return a.type() == AssetType.FIXTURE || a.type() == AssetType.OBJECT; }
    private static boolean machineType(AssetMetadata a) { return a.type() == AssetType.MACHINE || a.type() == AssetType.OBJECT; }
    private static boolean equipmentType(AssetMetadata a) { return a.type() == AssetType.FIXTURE || a.type() == AssetType.MACHINE || a.type() == AssetType.OBJECT; }
    private static boolean containerType(AssetMetadata a) { return a.type() == AssetType.OBJECT || a.type() == AssetType.FIXTURE; }
    private static boolean itemIcon(AssetMetadata a) { return a.type() == AssetType.ITEM_ICON; }
    private static boolean weaponIcon(AssetMetadata a) { return a.type() == AssetType.WEAPON_ICON || itemIcon(a); }
    private static boolean armorIcon(AssetMetadata a) { return a.type() == AssetType.ARMOR_ICON || itemIcon(a); }
    private static boolean toolIcon(AssetMetadata a) { return a.type() == AssetType.ITEM_ICON || a.type() == AssetType.WEAPON_ICON; }
    private static boolean tradeGoodIcon(AssetMetadata a) { return itemIcon(a); }
    private static boolean dataDeviceIcon(AssetMetadata a) { return itemIcon(a); }
    private static boolean doorType(AssetMetadata a) { return a.type() == AssetType.FIXTURE || a.type() == AssetType.WALL_TILE; }
    private static boolean generic(String t) { return themed(t,"generic","plain","main floor","main wall","default"); }
    private static boolean specializedSurface(String t) { return themed(t,"sewer","sump","drain","utility tunnel","industrial","factory","machine shop","workshop","habitation","hab","apartment","residential","market","bazaar","commercial","retail","medical","clinic","hospital","surgery","security","checkpoint","prison","brig","administrative","office","records","bureau","religious","shrine","chapel","altar","transit","station","platform","rail","warehouse","storage","cargo","loading","noble","luxury","estate","manor","slum","shanty","tenement","scrap"); }
    private static boolean themed(String text, String... needles) { return semanticContains(text, needles); }
    private static boolean notUiIcon(String text) { return !themed(text,"system inventory","item icon","ui icon","system control","interface control"); }
    private static String haystack(AssetMetadata a) { return normalize(a.id()+" "+a.name()+" "+a.pathOrUri()+" "+a.type().displayName()+" "+a.semanticDescription()); }
    private static boolean semanticContains(String text, String... needles) {
        String[] textTokens = semanticNormalize(text).split(" ");
        for (String needle : needles) {
            String normalized = semanticNormalize(needle); if (normalized.isBlank()) continue;
            String[] needleTokens = normalized.split(" ");
            for (int start=0; start+needleTokens.length<=textTokens.length; start++) {
                boolean matches=true;
                for (int offset=0; offset<needleTokens.length; offset++) {
                    String actual=textTokens[start+offset], expected=needleTokens[offset];
                    if (!actual.equals(expected) && !(offset==needleTokens.length-1 && regularPlural(actual,expected))) { matches=false; break; }
                }
                if (matches) return true;
            }
        }
        return false;
    }
    private static boolean regularPlural(String actual, String singular) { if (singular.length()<3) return false; return actual.equals(singular+"s") || actual.equals(singular+"es"); }
    private static String semanticNormalize(String value) { return normalize(value).replaceAll("[^a-z0-9]+"," ").replaceAll("\\s+"," ").trim(); }
    private static boolean contains(String text, String... needles) { return semanticContains(text, needles); }
    private static String normalize(String value) { return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('-',' ').replace('_',' ').trim(); }
}
