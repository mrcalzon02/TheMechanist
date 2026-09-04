package mechanist;

import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetType;

/**
 * Focused regression smoke for infrastructure semantic type boundaries.
 * Generic world objects may carry infrastructure words in imported metadata,
 * but they must not become machine or fixture art merely because the text matches.
 */
public final class SemanticInfrastructureTypeBoundarySmoke {
    public static void main(String[] args) {
        rejectObject("OBJ-G001", "Portable Generator", "portable power generator genset",
                SemanticRenderAssetResolver.RenderIntent.GENERATOR_MACHINE);
        rejectObject("OBJ-T001", "Power Transformer", "electrical power transformer",
                SemanticRenderAssetResolver.RenderIntent.TRANSFORMER_MACHINE);
        rejectObject("OBJ-L001", "Street Light Prop", "streetlight street lamp lamp post",
                SemanticRenderAssetResolver.RenderIntent.STREETLIGHT_FIXTURE);
        rejectObject("OBJ-J001", "Junction Box Prop", "junction box electrical box power box",
                SemanticRenderAssetResolver.RenderIntent.JUNCTION_BOX_FIXTURE);
        rejectObject("OBJ-W001", "Water Pipe Prop", "fresh water pipe water main",
                SemanticRenderAssetResolver.RenderIntent.WATER_PIPE_FIXTURE);
        rejectObject("OBJ-S001", "Sewer Pipe Prop", "sewer pipe waste pipe drain pipe",
                SemanticRenderAssetResolver.RenderIntent.SEWER_PIPE_FIXTURE);
        rejectObject("OBJ-C001", "Security Camera Prop", "security camera surveillance camera cctv",
                SemanticRenderAssetResolver.RenderIntent.SECURITY_CAMERA_FIXTURE);

        require(canUse(AssetType.MACHINE, "MAC-G001", "Power Generator", "power generator genset",
                        SemanticRenderAssetResolver.RenderIntent.GENERATOR_MACHINE),
                "generator resolver rejected canonical MACHINE art");
        require(canUse(AssetType.FIXTURE, "FIX-C001", "Security Camera", "security camera cctv",
                        SemanticRenderAssetResolver.RenderIntent.SECURITY_CAMERA_FIXTURE),
                "security-camera resolver rejected canonical FIXTURE art");

        System.out.println("SemanticInfrastructureTypeBoundarySmoke PASS " + SemanticRenderAssetResolver.VERSION);
    }

    private static void rejectObject(String id, String name, String semanticDescription,
                                     SemanticRenderAssetResolver.RenderIntent intent) {
        if (canUse(AssetType.OBJECT, id, name, semanticDescription, intent)) {
            throw new AssertionError(intent + " accepted generic OBJECT art " + id);
        }
    }

    private static boolean canUse(AssetType type, String id, String name, String semanticDescription,
                                  SemanticRenderAssetResolver.RenderIntent intent) {
        AssetMetadata asset = new AssetMetadata(
                id,
                "assets/smoke/" + id.toLowerCase() + ".png",
                name,
                type,
                semanticDescription);
        return SemanticRenderAssetResolver.canUse(asset, intent);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
