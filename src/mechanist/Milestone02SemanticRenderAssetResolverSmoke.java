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

        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.STREETLIGHT_FIXTURE, "INF-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.TRAFFIC_LIGHT_FIXTURE, "INF-0002");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.GENERATOR_MACHINE, "INF-0003");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.TRANSFORMER_MACHINE, "INF-0004");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.JUNCTION_BOX_FIXTURE, "INF-0005");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.VENTILATION_UNIT_FIXTURE, "INF-0006");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.WATER_PIPE_FIXTURE, "INF-0007");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.SEWER_PIPE_FIXTURE, "INF-0008");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.SECURITY_CAMERA_FIXTURE, "INF-0009");

        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.DOOR_CLOSED, "DOR-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.DOOR_OPEN, "DOR-0002");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.WORKSHOP_TABLE, "FUR-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.MEDICAL_TABLE, "FUR-0002");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.MARKET_COUNTER, "FUR-0003");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.TOOLBOX_CONTAINER, "CON-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.MEDICAL_CABINET_CONTAINER, "CON-0002");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.WEAPONS_LOCKER_CONTAINER, "CON-0003");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.CARGO_CONTAINER, "CON-0004");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.REFRIGERATED_STORAGE_CONTAINER, "CON-0005");

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

        assertStableVariant(registry, SemanticRenderAssetResolver.RenderIntent.GENERIC_FLOOR, 0L, "GEN-0001");
        assertStableVariant(registry, SemanticRenderAssetResolver.RenderIntent.GENERIC_FLOOR, 1L, "GEN-0003");
        assertStableVariant(registry, SemanticRenderAssetResolver.RenderIntent.GENERIC_FLOOR, 1L, "GEN-0003");

        reject(asset("NEG-0001", AssetType.ITEM_ICON, "System Inventory Light", "assets/ui/system_light.png", "system inventory icon light"), SemanticRenderAssetResolver.RenderIntent.STREETLIGHT_FIXTURE, "streetlight resolver accepted system inventory icon");
        reject(asset("NEG-0002", AssetType.FLOOR_TILE, "Sewer Floor", "assets/sewer/floor.png", "sewer floor"), SemanticRenderAssetResolver.RenderIntent.GENERIC_FLOOR, "generic floor resolver accepted sewer floor tile");
        reject(asset("NEG-0003", AssetType.FLOOR_TILE, "Generic Floor", "assets/generic/floor.png", "generic main floor"), SemanticRenderAssetResolver.RenderIntent.SEWER_FLOOR, "sewer floor resolver accepted generic floor tile");
        reject(asset("NEG-0004", AssetType.WALL_TILE, "Closed Doorish Wall", "assets/wall.png", "generic wall"), SemanticRenderAssetResolver.RenderIntent.DOOR_CLOSED, "closed-door resolver accepted generic wall");
        reject(asset("NEG-0005", AssetType.ITEM_ICON, "Relic", "assets/items/relic.png", "religious relic icon"), SemanticRenderAssetResolver.RenderIntent.WEAPON_ITEM_ICON, "weapon item resolver accepted religious object icon");
        reject(asset("NEG-0006", AssetType.FLOOR_TILE, "Sewer Market Floor", "assets/tiles/sewer/market.png", "sewer market floor"), SemanticRenderAssetResolver.RenderIntent.MARKET_FLOOR, "market floor resolver accepted sewer-themed market collision");
        reject(asset("NEG-0007", AssetType.UI_ICON, "Traffic Light Button", "assets/ui/traffic_light.png", "system control ui icon traffic light"), SemanticRenderAssetResolver.RenderIntent.TRAFFIC_LIGHT_FIXTURE, "traffic-light resolver accepted UI control icon");
        reject(asset("NEG-0008", AssetType.FIXTURE, "Sewer Water Pipe", "assets/infrastructure/sewer_water_pipe.png", "sewer waste sludge water pipe"), SemanticRenderAssetResolver.RenderIntent.WATER_PIPE_FIXTURE, "water-pipe resolver accepted sewer-contaminated pipe");
        reject(asset("NEG-0009", AssetType.ITEM_ICON, "Camera Icon", "assets/ui/camera.png", "system inventory item icon security camera"), SemanticRenderAssetResolver.RenderIntent.SECURITY_CAMERA_FIXTURE, "security-camera resolver accepted item icon");
        reject(asset("NEG-0010", AssetType.ITEM_ICON, "Medical Cabinet Icon", "assets/items/medical_cabinet.png", "medical cabinet medicine cabinet clinic cabinet item icon"), SemanticRenderAssetResolver.RenderIntent.MEDICAL_CABINET_CONTAINER, "container resolver accepted inventory item icon as world container art");
        reject(asset("NEG-0011", AssetType.ROAD_TILE, "Market Access Road", "assets/roads/market_access.png", "market commercial access road tile"), SemanticRenderAssetResolver.RenderIntent.MARKET_FLOOR, "market floor resolver accepted road art as interior floor art");
        reject(asset("NEG-0012", AssetType.SIDEWALK_TILE, "Habitation Sidewalk", "assets/sidewalks/habitation.png", "habitation residential sidewalk tile"), SemanticRenderAssetResolver.RenderIntent.HABITATION_FLOOR, "habitation floor resolver accepted sidewalk art as interior floor art");
        reject(asset("NEG-0013", AssetType.CORRIDOR_TILE, "Transit Corridor", "assets/corridors/transit.png", "transit station corridor rail tile"), SemanticRenderAssetResolver.RenderIntent.TRANSIT_FLOOR, "transit floor resolver accepted corridor art as floor art");
        reject(asset("NEG-0014", AssetType.ITEM_ICON, "Department Archive", "assets/items/department_archive.png", "department inventory archive icon"), SemanticRenderAssetResolver.RenderIntent.INDUSTRIAL_COMPONENT_ITEM_ICON, "industrial component resolver accepted department because it contains the letters part");
        reject(asset("NEG-0015", AssetType.ITEM_ICON, "Database Archive", "assets/items/database_archive.png", "database archive inventory icon"), SemanticRenderAssetResolver.RenderIntent.DATA_DEVICE_ITEM_ICON, "data-device resolver accepted database because it begins with the letters data");
        reject(asset("NEG-0016", AssetType.FLOOR_TILE, "Marketplace Floor", "assets/tiles/commercial/marketplace.png", "marketplace commercial floor tile"), SemanticRenderAssetResolver.RenderIntent.MARKET_FLOOR, "market floor resolver accepted marketplace as the standalone market semantic");

        AssetMetadata atlasTool = asset("CEL-0001", AssetType.WEAPON_ICON,
                "Entrenching Shovel", "assets/items/weapons_1_r03c05.png",
                "cell rule weapon tool weapon shovel spade maintenance tool");
        AssetMetadata ordinaryRifle = asset("CEL-0002", AssetType.WEAPON_ICON,
                "Autogun Rifle", "assets/items/weapons_2_r03c03.png",
                "cell rule ranged weapon firearm rifle carbine autogun");
        AssetMetadata knowledgeDevice = asset("CEL-0003", AssetType.UI_ICON,
                "Knowledge Device", "assets/items/knowledge_devices_r01c01.png",
                "knowledge skill devices device equipment item inventory");
        AssetMetadata systemButton = asset("CEL-0004", AssetType.UI_ICON,
                "System Button", "assets/system/button_r01c01.png",
                "system control interface control rondel button");
        AssetMetadata combatKnife = asset("CEL-0005", AssetType.WEAPON_ICON,
                "Combat Knife", "assets/items/weapons_knife_r01c01.png",
                "combat knife melee equipment");
        AssetMetadata inventoryKnife = asset("CEL-0006", AssetType.ITEM_ICON,
                "Kitchen Knife", "assets/items/kitchen_knife.png",
                "kitchen knife utensil inventory item");
        AssetMetadata flakVest = asset("CEL-0007", AssetType.ARMOR_ICON,
                "Flak Vest", "assets/items/armor_flak_vest_r01c01.png",
                "flak vest protective equipment");
        AssetMetadata inventoryVest = asset("CEL-0008", AssetType.ITEM_ICON,
                "Utility Vest", "assets/items/utility_vest.png",
                "utility vest inventory item");
        AssetMetadata fieldAntiseptic = asset("CEL-0009", AssetType.ITEM_ICON,
                "Field Antiseptic", "assets/items/field_antiseptic.png",
                "antiseptic treatment supply inventory item");
        AssetMetadata combatStimInjector = asset("CEL-0010", AssetType.ITEM_ICON,
                "Combat Stim Injector", "assets/items/combat_stim_injector.png",
                "combat stim injector inventory item");
        AssetMetadata workshopHammer = asset("CEL-0011", AssetType.ITEM_ICON,
                "Workshop Hammer", "assets/items/workshop_hammer.png",
                "hammer inventory item");
        AssetMetadata devotionalRosary = asset("CEL-0012", AssetType.ITEM_ICON,
                "Pilgrim Rosary", "assets/items/pilgrim_rosary.png",
                "rosary inventory item");
        AssetMetadata fieldDataSlate = asset("CEL-0013", AssetType.ITEM_ICON,
                "Field Data Slate", "assets/items/field_data_slate.png",
                "data slate memory core inventory item");
        AssetMetadata fieldCanteen = asset("CEL-0014", AssetType.ITEM_ICON,
                "Field Canteen", "assets/items/field_canteen.png",
                "canteen hydration vessel inventory item");
        AssetMetadata luxuryExportGood = asset("CEL-0015", AssetType.ITEM_ICON,
                "Noble Export Lot", "assets/items/noble_export_lot.png",
                "luxury good export good inventory item");
        AssetMetadata authoredLamppost = asset("CEL-0016", AssetType.FIXTURE,
                "Civic Lamppost", "assets/infrastructure/civic_lamppost.png",
                "lamppost fixture");
        AssetMetadata securityInterviewDesk = asset("CEL-0017", AssetType.OBJECT,
                "Security Interview Desk", "assets/furniture/security_interview_desk.png",
                "security interview desk furniture");

        require(SemanticRenderAssetResolver.canUse(atlasTool,
                SemanticRenderAssetResolver.RenderIntent.TOOL_ITEM_ICON),
                "tool resolver rejected a weapon-atlas cell explicitly described as a tool");
        reject(ordinaryRifle, SemanticRenderAssetResolver.RenderIntent.TOOL_ITEM_ICON,
                "tool resolver accepted an ordinary rifle without tool semantics");
        require(SemanticRenderAssetResolver.canUse(workshopHammer,
                SemanticRenderAssetResolver.RenderIntent.TOOL_ITEM_ICON),
                "tool resolver rejected a hammer classified as a tool by the intent authority");
        require(SemanticRenderAssetResolver.canUse(combatKnife,
                SemanticRenderAssetResolver.RenderIntent.WEAPON_ITEM_ICON),
                "weapon resolver rejected a correctly typed combat knife because metadata lacked a generic weapon synonym");
        reject(inventoryKnife, SemanticRenderAssetResolver.RenderIntent.WEAPON_ITEM_ICON,
                "weapon resolver accepted a generic item-icon knife without explicit weapon typing");
        require(SemanticRenderAssetResolver.canUse(flakVest,
                SemanticRenderAssetResolver.RenderIntent.ARMOR_ITEM_ICON),
                "armor resolver rejected a correctly typed flak vest because metadata lacked a generic armor synonym");
        reject(inventoryVest, SemanticRenderAssetResolver.RenderIntent.ARMOR_ITEM_ICON,
                "armor resolver accepted a generic item-icon vest without explicit armor typing");
        require(SemanticRenderAssetResolver.canUse(fieldAntiseptic,
                SemanticRenderAssetResolver.RenderIntent.MEDICAL_ITEM_ICON),
                "medical resolver rejected an antiseptic item classified as medical by the intent authority");
        require(SemanticRenderAssetResolver.canUse(combatStimInjector,
                SemanticRenderAssetResolver.RenderIntent.DRUG_ITEM_ICON),
                "drug resolver rejected a combat-stim injector classified as a drug by the intent authority");
        require(SemanticRenderAssetResolver.canUse(devotionalRosary,
                SemanticRenderAssetResolver.RenderIntent.RELIGIOUS_OBJECT_ITEM_ICON),
                "religious-object resolver rejected a rosary classified as religious by the intent authority");
        require(SemanticRenderAssetResolver.canUse(fieldDataSlate,
                SemanticRenderAssetResolver.RenderIntent.DATA_DEVICE_ITEM_ICON),
                "data-device resolver rejected a data slate classified as a data device by the intent authority");
        require(SemanticRenderAssetResolver.canUse(fieldCanteen,
                SemanticRenderAssetResolver.RenderIntent.FOOD_ITEM_ICON),
                "food resolver rejected a canteen classified as food/drink by the intent authority");
        require(SemanticRenderAssetResolver.canUse(luxuryExportGood,
                SemanticRenderAssetResolver.RenderIntent.TRADE_GOOD_ITEM_ICON),
                "trade-good resolver rejected a luxury/export good classified as trade goods by the intent authority");
        require(SemanticRenderAssetResolver.canUse(authoredLamppost,
                SemanticRenderAssetResolver.RenderIntent.STREETLIGHT_FIXTURE),
                "streetlight resolver rejected a lamppost classified as a streetlight fixture by the intent authority");
        require(SemanticRenderAssetResolver.canUse(securityInterviewDesk,
                SemanticRenderAssetResolver.RenderIntent.INTERROGATION_DESK),
                "interrogation-desk resolver rejected a security interview desk classified by the intent authority");
        require(SemanticRenderAssetResolver.canUse(
                        asset("CEL-0018", AssetType.OBJECT, "Hab Clothing Cabinet", "assets/containers/hab_clothing_cabinet.png", "clothing cabinet storage"),
                        SemanticRenderAssetResolver.RenderIntent.WARDROBE_CONTAINER),
                "wardrobe resolver rejected a clothing cabinet classified by the intent authority");
        require(SemanticRenderAssetResolver.canUse(knowledgeDevice,
                SemanticRenderAssetResolver.RenderIntent.DATA_DEVICE_ITEM_ICON),
                "data-device resolver rejected the UI-typed Knowledge_devices family");
        reject(systemButton, SemanticRenderAssetResolver.RenderIntent.DATA_DEVICE_ITEM_ICON,
                "data-device resolver accepted a generic system-control icon");

        System.out.println("Milestone02SemanticRenderAssetResolverSmoke PASS " + SemanticRenderAssetResolver.VERSION);
    }

    private static void assertFound(AssetRegistry registry, SemanticRenderAssetResolver.RenderIntent intent, String expectedId) {
        SemanticRenderAssetResolver.Resolution resolution = SemanticRenderAssetResolver.resolve(registry, intent);
        if (!resolution.found()) throw new AssertionError(intent + " did not resolve: " + resolution.reason);
        if (!expectedId.equals(resolution.asset.id())) throw new AssertionError(intent + " resolved " + resolution.asset.id() + " instead of " + expectedId);
    }
    private static void assertStableVariant(AssetRegistry registry, SemanticRenderAssetResolver.RenderIntent intent, long key, String expectedId) {
        SemanticRenderAssetResolver.Resolution resolution = SemanticRenderAssetResolver.resolve(registry, intent, key);
        if (!resolution.found()) throw new AssertionError(intent + " stable variant did not resolve: " + resolution.reason);
        if (!expectedId.equals(resolution.asset.id())) throw new AssertionError(intent + " stable variant key " + key + " resolved " + resolution.asset.id() + " instead of " + expectedId);
    }

    private static void reject(AssetMetadata asset, SemanticRenderAssetResolver.RenderIntent intent, String message) {
        if (SemanticRenderAssetResolver.canUse(asset, intent)) throw new AssertionError(message);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static AssetRegistry testRegistry() throws Exception {
        Map<String, AssetMetadata> entries = new LinkedHashMap<>();
        put(entries, asset("SEM-0001", AssetType.FLOOR_TILE, "Sewer Floor Wet Utility Tunnel", "assets/tiles/sewer/floor_wet.png", "sewer floor wet utility tunnel tile"));
        put(entries, asset("SEM-0002", AssetType.WALL_TILE, "Sewer Wall Brick Drain", "assets/tiles/sewer/wall_brick.png", "sewer wall brick drain utility tunnel tile"));
        put(entries, asset("GEN-0001", AssetType.FLOOR_TILE, "Generic Floor Plain", "assets/tiles/generic/floor_plain.png", "generic main floor tile"));
        put(entries, asset("GEN-0003", AssetType.FLOOR_TILE, "Generic Floor Ribbed", "assets/tiles/generic/floor_ribbed.png", "generic main floor tile"));
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

        put(entries, asset("INF-0001", AssetType.FIXTURE, "Streetlight Pole Lamp", "assets/infrastructure/streetlight_pole.png", "streetlight fixture street light pole lamp"));
        put(entries, asset("INF-0002", AssetType.FIXTURE, "Traffic Light Signal", "assets/infrastructure/traffic_light.png", "traffic light signal light crossing signal"));
        put(entries, asset("INF-0003", AssetType.MACHINE, "Power Generator", "assets/infrastructure/generator.png", "generator power generator genset machine"));
        put(entries, asset("INF-0004", AssetType.MACHINE, "Power Transformer", "assets/infrastructure/transformer.png", "transformer power transformer electrical transformer machine"));
        put(entries, asset("INF-0005", AssetType.FIXTURE, "Junction Box", "assets/infrastructure/junction_box.png", "junction box electrical box power box fixture"));
        put(entries, asset("INF-0006", AssetType.MACHINE, "Ventilation Unit", "assets/infrastructure/ventilation.png", "ventilation unit vent unit air handler exhaust fan"));
        put(entries, asset("INF-0007", AssetType.FIXTURE, "Fresh Water Pipe", "assets/infrastructure/water_pipe.png", "water pipe fresh water pipe water main"));
        put(entries, asset("INF-0008", AssetType.FIXTURE, "Sewer Drain Pipe", "assets/infrastructure/sewer_pipe.png", "sewer pipe waste pipe drain pipe sludge pipe"));
        put(entries, asset("INF-0009", AssetType.FIXTURE, "Security Camera", "assets/infrastructure/security_camera.png", "security camera surveillance camera cctv"));

        put(entries, asset("DOR-0001", AssetType.FIXTURE, "Closed Door Variant", "assets/doors/door_closed_a.png", "door closed shut semantic state variant"));
        put(entries, asset("DOR-0002", AssetType.FIXTURE, "Open Door Variant", "assets/doors/door_open_a.png", "door open opened semantic state variant"));
        put(entries, asset("FUR-0001", AssetType.FIXTURE, "Workshop Table", "assets/furniture/workshop_table.png", "workshop table workbench fabrication table"));
        put(entries, asset("FUR-0002", AssetType.FIXTURE, "Medical Table", "assets/furniture/medical_table.png", "medical table operating table surgery table"));
        put(entries, asset("FUR-0003", AssetType.FIXTURE, "Market Counter", "assets/furniture/market_counter.png", "market counter shop counter stall counter"));
        put(entries, asset("CON-0001", AssetType.OBJECT, "Toolbox Container", "assets/containers/toolbox.png", "toolbox tool box container"));
        put(entries, asset("CON-0002", AssetType.OBJECT, "Medical Cabinet", "assets/containers/medical_cabinet.png", "medical cabinet medicine cabinet clinic cabinet"));
        put(entries, asset("CON-0003", AssetType.OBJECT, "Weapons Locker", "assets/containers/weapons_locker.png", "weapons locker armory locker"));
        put(entries, asset("CON-0004", AssetType.OBJECT, "Cargo Container", "assets/containers/cargo_container.png", "cargo container crate shipping container"));
        put(entries, asset("CON-0005", AssetType.OBJECT, "Cold Storage Freezer", "assets/containers/cold_storage.png", "refrigerated storage cold storage freezer refrigerator chiller locker"));

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

    private static void put(Map<String, AssetMetadata> entries, AssetMetadata asset) {
        entries.put(asset.id(), asset);
    }

    private static AssetMetadata asset(String id, AssetType type, String name, String path, String description) {
        return new AssetMetadata(id, path, name, type, description);
    }

    private Milestone02SemanticRenderAssetResolverSmoke() {}
}