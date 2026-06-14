package mechanist;

import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetRegistry;
import mechanist.assets.AssetType;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Milestone02SemanticAssetMigrationCoverageSmoke {
    public static void main(String[] args) throws Exception {
        AssetRegistry registry = registry();
        SemanticAssetMigrationCoverageAuthority.Report report =
                SemanticAssetMigrationCoverageAuthority.audit(registry);

        if (report.total() != SemanticRenderAssetResolver.RenderIntent.values().length) {
            throw new AssertionError("coverage did not audit every render intent");
        }
        if (report.available() < 7) {
            throw new AssertionError("expected representative semantic coverage, got " + report.available());
        }
        if (report.availableByFamily().getOrDefault(SemanticAssetMigrationCoverageAuthority.Family.DOOR, 0) != 2) {
            throw new AssertionError("open and closed door coverage not separated");
        }
        if (report.availableByFamily().getOrDefault(SemanticAssetMigrationCoverageAuthority.Family.INFRASTRUCTURE, 0) != 1) {
            throw new AssertionError("streetlight infrastructure coverage missing");
        }
        if (report.availableByFamily().getOrDefault(SemanticAssetMigrationCoverageAuthority.Family.ITEM, 0) < 2) {
            throw new AssertionError("item semantic coverage missing");
        }
        if (SemanticAssetMigrationCoverageAuthority.familyOf(
                SemanticRenderAssetResolver.RenderIntent.MEDICAL_FLOOR)
                != SemanticAssetMigrationCoverageAuthority.Family.ROOM_TILE) {
            throw new AssertionError("medical floor was not classified as room tile");
        }
        if (SemanticAssetMigrationCoverageAuthority.familyOf(
                SemanticRenderAssetResolver.RenderIntent.SEWER_FLOOR)
                != SemanticAssetMigrationCoverageAuthority.Family.ZONE_TILE) {
            throw new AssertionError("sewer floor was not classified as zone tile");
        }

        System.out.println("Milestone02SemanticAssetMigrationCoverageSmoke PASS " + report.summary());
    }

    private static AssetRegistry registry() throws Exception {
        Map<String, AssetMetadata> entries = new LinkedHashMap<>();
        put(entries, new AssetMetadata("SEW-0001", "assets/tiles/sewer/floor.png", "Sewer floor", AssetType.FLOOR_TILE, "sewer sump drain floor"));
        put(entries, new AssetMetadata("GEN-0001", "assets/tiles/generic/floor.png", "Generic floor", AssetType.FLOOR_TILE, "generic plain main floor"));
        put(entries, new AssetMetadata("MED-0001", "assets/tiles/medical/floor.png", "Medical floor", AssetType.FLOOR_TILE, "medical clinic hospital surgery floor"));
        put(entries, new AssetMetadata("LGT-0001", "assets/infrastructure/streetlight.png", "Streetlight", AssetType.FIXTURE, "streetlight street lamp infrastructure fixture"));
        put(entries, new AssetMetadata("DOP-0001", "assets/doors/open.png", "Open door", AssetType.WALL_TILE, "bulkhead door open variant"));
        put(entries, new AssetMetadata("DCL-0001", "assets/doors/closed.png", "Closed door", AssetType.WALL_TILE, "bulkhead door closed shut variant"));
        put(entries, new AssetMetadata("ITM-0001", "assets/items/medical.png", "Medical kit", AssetType.ITEM_ICON, "medical medkit medicine"));
        put(entries, new AssetMetadata("WEA-0001", "assets/items/weapon.png", "Weapon", AssetType.WEAPON_ICON, "weapon firearm gun"));
        put(entries, new AssetMetadata("CON-0001", "assets/containers/cargo.png", "Cargo container", AssetType.OBJECT, "cargo container shipping crate"));

        Constructor<AssetRegistry> ctor = AssetRegistry.class.getDeclaredConstructor(
                java.nio.file.Path.class, java.nio.file.Path.class, Map.class);
        ctor.setAccessible(true);
        return ctor.newInstance(java.nio.file.Path.of("."), null, entries);
    }

    private static void put(Map<String, AssetMetadata> entries, AssetMetadata metadata) {
        entries.put(metadata.id(), metadata);
    }

    private Milestone02SemanticAssetMigrationCoverageSmoke() {}
}
