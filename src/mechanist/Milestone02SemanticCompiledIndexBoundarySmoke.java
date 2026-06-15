package mechanist;

import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetType;

/** Regression checks for asset families whose cell semantics differ from their atlas-level type. */
public final class Milestone02SemanticCompiledIndexBoundarySmoke {
    public static void main(String[] args) {
        AssetMetadata atlasTool = asset(
                "CEL-0001", AssetType.WEAPON_ICON,
                "Entrenching Shovel",
                "32px/ITEMS/WEAPONS-1_r03c05_32px.png",
                "cell rule weapon tool weapon shovel spade maintenance tool");
        AssetMetadata ordinaryRifle = asset(
                "CEL-0002", AssetType.WEAPON_ICON,
                "Autogun Rifle",
                "32px/ITEMS/WEAPONS-2_r03c03_32px.png",
                "cell rule ranged weapon firearm rifle carbine autogun");
        AssetMetadata knowledgeDevice = asset(
                "CEL-0003", AssetType.UI_ICON,
                "Knowledge Device",
                "32px/ITEMS/Knowledge_devices_r01c01_32px.png",
                "knowledge skill devices device equipment item inventory");
        AssetMetadata systemButton = asset(
                "CEL-0004", AssetType.UI_ICON,
                "System Button",
                "32px/SYSTEM/System_r01c01_32px.png",
                "system control interface control rondel button");
        AssetMetadata goodsObject = asset(
                "CEL-0005", AssetType.OBJECT,
                "Trade Goods",
                "32px/Objects/goods_r01c01_32px.png",
                "objects goods cargo item object prop set dressing");
        AssetMetadata junkObject = asset(
                "CEL-0006", AssetType.OBJECT,
                "Junk Heap",
                "32px/Objects/Junk_RGBA_r01c01_32px.png",
                "objects junk scrap debris object prop set dressing");

        requireAccepted(atlasTool, SemanticRenderAssetResolver.RenderIntent.TOOL_ITEM_ICON);
        requireRejected(ordinaryRifle, SemanticRenderAssetResolver.RenderIntent.TOOL_ITEM_ICON);
        requireAccepted(knowledgeDevice, SemanticRenderAssetResolver.RenderIntent.DATA_DEVICE_ITEM_ICON);
        requireRejected(systemButton, SemanticRenderAssetResolver.RenderIntent.DATA_DEVICE_ITEM_ICON);
        requireAccepted(goodsObject, SemanticRenderAssetResolver.RenderIntent.TRADE_GOOD_ITEM_ICON);
        requireRejected(junkObject, SemanticRenderAssetResolver.RenderIntent.TRADE_GOOD_ITEM_ICON);

        System.out.println("Milestone02SemanticCompiledIndexBoundarySmoke PASS "
                + SemanticRenderAssetResolver.VERSION);
    }

    private static void requireAccepted(
            AssetMetadata asset,
            SemanticRenderAssetResolver.RenderIntent intent
    ) {
        if (!SemanticRenderAssetResolver.canUse(asset, intent)) {
            throw new AssertionError(intent + " rejected " + asset.name());
        }
    }

    private static void requireRejected(
            AssetMetadata asset,
            SemanticRenderAssetResolver.RenderIntent intent
    ) {
        if (SemanticRenderAssetResolver.canUse(asset, intent)) {
            throw new AssertionError(intent + " accepted unrelated " + asset.name());
        }
    }

    private static AssetMetadata asset(
            String id,
            AssetType type,
            String name,
            String path,
            String description
    ) {
        return new AssetMetadata(id, path, name, type, description);
    }

    private Milestone02SemanticCompiledIndexBoundarySmoke() {}
}
