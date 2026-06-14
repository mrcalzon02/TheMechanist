package mechanist;

import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetRegistry;
import mechanist.assets.AssetType;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Milestone02SemanticRenderAssetResolverSmoke {
    public static void main(String[] args) throws Exception {
        AssetRegistry registry = testRegistry();
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.SEWER_FLOOR, "SEM-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.SEWER_WALL, "SEM-0002");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.GENERIC_FLOOR, "GEN-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.GENERIC_WALL, "GEN-0002");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.INDUSTRIAL_FLOOR, "IND-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.INDUSTRIAL_WALL, "IND-0002");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.HABITATION_FLOOR, "HAB-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.HABITATION_WALL, "HAB-0002");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.MARKET_FLOOR, "MRK-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.MARKET_WALL, "MRK-0002");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.MEDICAL_FLOOR, "MED-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.SECURITY_FLOOR, "SEC-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.ADMINISTRATIVE_FLOOR, "ADM-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.RELIGIOUS_FLOOR, "REL-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.TRANSIT_FLOOR, "TRN-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.WAREHOUSE_FLOOR, "WAR-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.NOBLE_FLOOR, "NOB-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.SLUM_FLOOR, "SLM-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.STREETLIGHT_FIXTURE, "FIX-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.DOOR_CLOSED, "DOR-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.DOOR_OPEN, "DOR-0002");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.WORKSHOP_TABLE, "FUR-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.MEDICAL_TABLE, "FUR-0002");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.MARKET_COUNTER, "FUR-0003");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.TOOLBOX_CONTAINER, "CON-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.MEDICAL_CABINET_CONTAINER, "CON-0002");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.WEAPONS_LOCKER_CONTAINER, "CON-0003");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.CARGO_CONTAINER, "CON-0004");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.WEAPON_ITEM_ICON, "WEA-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.ARMOR_ITEM_ICON, "ARM-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.TOOL_ITEM_ICON, "ITE-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.MEDICAL_ITEM_ICON, "ITE-0002");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.DRUG_ITEM_ICON, "ITE-0003");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.FOOD_ITEM_ICON, "ITE-0004");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.INDUSTRIAL_COMPONENT_ITEM_ICON, "ITE-0005");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.TRADE_GOOD_ITEM_ICON, "ITE-0006");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.RELIGIOUS_OBJECT_ITEM_ICON, "ITE-0007");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.DATA_DEVICE_ITEM_ICON, "ITE-0008");

        if (SemanticRenderAssetResolver.canUse(asset("NEG-0001", AssetType.ITEM_ICON, "System Inventory Light", "assets/ui/system_light.png", "system inventory icon light"), SemanticRenderAssetResolver.RenderIntent.STREETLIGHT_FIXTURE)) throw new AssertionError("streetlight resolver accepted system inventory icon");
        if (SemanticRenderAssetResolver.canUse(asset("NEG-0002", AssetType.FLOOR_TILE, "Sewer Floor", "assets/sewer/floor.png", "sewer floor"), SemanticRenderAssetResolver.RenderIntent.GENERIC_FLOOR)) throw new AssertionError("generic floor resolver accepted sewer floor tile");
        if (SemanticRenderAssetResolver.canUse(asset("NEG-0003", AssetType.FLOOR_TILE, "Generic Floor", "assets/generic/floor.png", "generic main floor"), SemanticRenderAssetResolver.RenderIntent.SEWER_FLOOR)) throw new AssertionError("sewer floor resolver accepted generic floor tile");
        if (SemanticRenderAssetResolver.canUse(asset("NEG-0004", AssetType.WALL_TILE, "Closed Doorish Wall", "assets/wall.png", "generic wall"), SemanticRenderAssetResolver.RenderIntent.DOOR_CLOSED)) throw new AssertionError("closed-door resolver accepted generic wall");
        if (SemanticRenderAssetResolver.canUse(asset("NEG-0005", AssetType.ITEM_ICON, "Relic", "assets/items/relic.png", "religious relic icon"), SemanticRenderAssetResolver.RenderIntent.WEAPON_ITEM_ICON)) throw new AssertionError("weapon item resolver accepted religious object icon");
        if (SemanticRenderAssetResolver.canUse(asset("NEG-0006", AssetType.FLOOR_TILE, "Sewer Market Floor", "assets/tiles/sewer/market.png", "sewer market floor"), SemanticRenderAssetResolver.RenderIntent.MARKET_FLOOR)) throw new AssertionError("market floor resolver accepted sewer-themed market collision");

        System.out.println("Milestone02SemanticRenderAssetResolverSmoke PASS " + SemanticRenderAssetResolver.VERSION);
    }

    private static void assertFound(AssetRegistry registry, SemanticRenderAssetResolver.RenderIntent intent, String expectedId) {
        SemanticRenderAssetResolver.Resolution resolution = SemanticRenderAssetResolver.resolve(registry, intent);
        if (!resolution.found()) throw new AssertionError(intent + " did not resolve: " + resolution.reason);
        if (!expectedId.equals(resolution.asset.id())) throw new AssertionError(intent + " resolved " + resolution.asset.id() + " instead of " + expectedId);
    }

    private static AssetRegistry testRegistry() throws Exception {
        Map<String, AssetMetadata> entries = new LinkedHashMap<>();
        put(entries, asset("SEM-0001", AssetType.FLOOR_TILE, "Sewer Floor Wet Utility Tunnel", "assets/tiles/sewer/floor_wet.png", "sewer floor wet utility tunnel tile"));
        put(entries, asset("SEM-0002", AssetType.WALL_TILE, "Sewer Wall Brick Drain", "assets/tiles/sewer/wall_brick.png", "sewer wall brick drain utility tunnel tile"));
        put(entries, asset("GEN-0001", AssetType.FLOOR_TILE, "Generic Floor Plain", "assets/tiles/generic/floor_plain.png", "generic main floor tile"));
        put(entries, asset("GEN-0002", AssetType.WALL_TILE, "Generic Wall Plain", "assets/tiles/generic/wall_plain.png", "generic main wall tile"));
        put(entries, asset("IND-0001", AssetType.FLOOR_TILE, "Industrial Machine Shop Floor", "assets/tiles/industrial/floor_machine_shop.png", "industrial factory machine shop floor tile"));
        put(entries, asset("IND-0002", AssetType.WALL_TILE, "Industrial Factory Wall", "assets/tiles/industrial/wall_factory.png", "industrial factory wall tile"));
        put(entries, asset("HAB-0001", AssetType.FLOOR_TILE, "Habitation Apartment Floor", "assets/tiles/habitation/floor_apartment.png", "habitation apartment residential floor tile"));
        put(entries, asset("HAB-0002", AssetType.WALL_TILE, "Habitation Apartment Wall", "assets/tiles/habitation/wall_apartment.png", "habitation apartment residential wall tile"));
        put(entries, asset("MRK-0001", AssetType.FLOOR_TILE, "Market Bazaar Floor", "assets/tiles/market/floor_bazaar.png", "market bazaar shop commercial floor tile"));
        put(entries, asset("MRK-0002", AssetType.WALL_TILE, "Market Shop Wall", "assets/tiles/market/wall_shop.png", "market shop commercial wall tile"));
        put(entries, asset("MED-0001", AssetType.FLOOR_TILE, "Medical Clinic Floor", "assets/tiles/medical/floor_clinic.png", "medical clinic hospital surgery floor tile"));
        put(entries, asset("SEC-0001", AssetType.FLOOR_TILE, "Security Checkpoint Floor", "assets/tiles/security/floor_checkpoint.png", "security checkpoint prison floor tile"));
        put(entries, asset("ADM-0001", AssetType.FLOOR_TILE, "Administrative Office Floor", "assets/tiles/admin/floor_office.png", "administrative office records bureau floor tile"));
        put(entries, asset("REL-0001", AssetType.FLOOR_TILE, "Religious Shrine Floor", "assets/tiles/religious/floor_shrine.png", "religious shrine chapel altar floor tile"));
        put(entries, asset("TRN-0001", AssetType.FLOOR_TILE, "Transit Station Platform Floor", "assets/tiles/transit/floor_platform.png", "transit station platform rail floor tile"));
        put(entries, asset("WAR-0001", AssetType.FLOOR_TILE, "Warehouse Loading Floor", "assets/tiles/warehouse/floor_loading.png", "warehouse storage cargo loading floor tile"));
        put(entries, asset("NOB-0001", AssetType.FLOOR_TILE, "Noble Estate Floor", "assets/tiles/noble/floor_estate.png", "noble luxury estate manor floor tile"));
        put(entries, asset("SLM-0001", AssetType.FLOOR_TILE, "Slum Scrap Floor", "assets/tiles/slum/floor_scrap.png", "slum shanty tenement scrap floor tile"));
        put(entries, asset("FIX-0001", AssetType.FIXTURE, "Streetlight Pole Lamp", "assets/fixtures/infrastructure/streetlight_pole.png", "streetlight fixture street light pole lamp"));
        put(entries, asset("DOR-0001", AssetType.FIXTURE, "Closed Door Variant", "assets/tiles/doors/door_closed_a.png", "door closed shut tile variant"));
        put(entries, asset("DOR-0002", AssetType.FIXTURE, "Open Door Variant", "assets/tiles/doors/door_open_a.png", "door open opened tile variant"));
        put(entries, asset("FUR-0001", AssetType.FIXTURE, "Workshop Table", "assets/fixtures/workshop/table.png", "workshop table workbench fabrication table"));
        put(entries, asset("FUR-0002", AssetType.FIXTURE, "Medical Table", "assets/fixtures/medical/table.png", "medical table operating table surgery table"));
        put(entries, asset("FUR-0003", AssetType.FIXTURE, "Market Counter", "assets/fixtures/market/counter.png", "market counter shop counter stall counter"));
        put(entries, asset("CON-0001", AssetType.OBJECT, "Toolbox Container", "assets/containers/toolbox.png", "toolbox tool box container"));
        put(entries, asset("CON-0002", AssetType.OBJECT, "Medical Cabinet", "assets/containers/medical_cabinet.png", "medical cabinet medicine cabinet clinic cabinet"));
        put(entries, asset("CON-0003", AssetType.OBJECT, "Weapons Locker", "assets/containers/weapons_locker.png", "weapons locker armory locker"));
        put(entries, asset("CON-0004", AssetType.OBJECT, "Cargo Container", "assets/containers/cargo_container.png", "cargo container crate shipping container"));
        put(entries, asset("WEA-0001", AssetType.WEAPON_ICON, "Weapon Icon", "assets/items/weapon.png", "weapon gun blade ammo icon"));
        put(entries, asset("ARM-0001", AssetType.ARMOR_ICON, "Armor Icon", "assets/items/armor.png", "armor helmet clothing icon"));
        put(entries, asset("ITE-0001", AssetType.ITEM_ICON, "Tool Icon", "assets/items/tool.png", "tool wrench repair fabrication icon"));
        put(entries, asset("ITE-0002", AssetType.ITEM_ICON, "Medical Item Icon", "assets/items/medical.png", "medical medkit bandage suture medicine icon"));
        put(entries, asset("ITE-0003", AssetType.ITEM_ICON, "Drug Item Icon", "assets/items/drug.png", "drug narcotic stimulant dose icon"));
        put(entries, asset("ITE-0004", AssetType.ITEM_ICON, "Food Item Icon", "assets/items/food.png", "food ration meal water icon"));
        put(entries, asset("ITE-0005", AssetType.ITEM_ICON, "Industrial Component Icon", "assets/items/component.png", "industrial component machine part icon"));
        put(entries, asset("ITE-0006", AssetType.ITEM_ICON, "Trade Good Icon", "assets/items/trade_good.png", "trade good goods commodity barter icon"));
        put(entries, asset("ITE-0007", AssetType.ITEM_ICON, "Religious Object Icon", "assets/items/relic.png", "religious relic icon prayer object"));
        put(entries, asset("ITE-0008", AssetType.ITEM_ICON, "Data Device Icon", "assets/items/data_device.png", "data device datapad terminal chip icon"));
        Constructor<AssetRegistry> ctor = AssetRegistry.class.getDeclaredConstructor(java.nio.file.Path.class, java.nio.file.Path.class, Map.class);
        ctor.setAccessible(true);
        return ctor.newInstance(java.nio.file.Path.of("."), null, entries);
    }

    private static void put(Map<String, AssetMetadata> entries, AssetMetadata asset) { entries.put(asset.id(), asset); }
    private static AssetMetadata asset(String id, AssetType type, String name, String path, String description) { return new AssetMetadata(id, path, name, type, description); }
    private Milestone02SemanticRenderAssetResolverSmoke() {}
}
