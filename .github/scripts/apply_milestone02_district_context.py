from pathlib import Path
import re

COMPILER = Path("src/mechanist/TileDataCompilationAuthority.java")
SUITE = Path("src/mechanist/Gate3PlayerFacingTextSmokeSuite.java")
SMOKE = Path("src/mechanist/Milestone02DistrictRoomTileContextSmoke.java")

text = COMPILER.read_text(encoding="utf-8").replace("\r\n", "\n")

if "import mechanist.assets.AssetManager;" not in text:
    marker = "package mechanist;\n\nimport java.util.*;\n"
    replacement = "package mechanist;\n\nimport mechanist.assets.AssetManager;\n\nimport java.util.*;\n"
    if marker not in text:
        raise SystemExit("Tile compiler import marker missing")
    text = text.replace(marker, replacement, 1)

if 'static final String MISSING_RECOGNIZED_TILE_ID = "MISSING-SEMANTIC-TILE";' not in text:
    marker = 'static final String VERSION = "0.9.10jq";'
    replacement = (
        'static final String VERSION = "0.9.10jr";\n'
        '    static final String MISSING_RECOGNIZED_TILE_ID = "MISSING-SEMANTIC-TILE";'
    )
    if marker not in text:
        raise SystemExit("Tile compiler version marker missing")
    text = text.replace(marker, replacement, 1)

if '"habitation_room".equals(family)' not in text:
    marker = (
        '                else if ("noble_room".equals(family)) art = "floor_noble_room_v" + variant;\n'
        '                else art = "floor_bare_underhive_v" + variant;'
    )
    replacement = (
        '                else if ("noble_room".equals(family)) art = "floor_noble_room_v" + variant;\n'
        '                else if ("habitation_room".equals(family)) art = "floor_habitation_room_v" + variant;\n'
        '                else if ("market_room".equals(family)) art = "floor_market_room_v" + variant;\n'
        '                else if ("security_room".equals(family)) art = "floor_security_room_v" + variant;\n'
        '                else if ("industrial_room".equals(family)) art = "floor_industrial_room_v" + variant;\n'
        '                else if ("administrative_room".equals(family)) art = "floor_administrative_room_v" + variant;\n'
        '                else if ("transit_room".equals(family)) art = "floor_transit_room_v" + variant;\n'
        '                else if ("religious_room".equals(family)) art = "floor_religious_room_v" + variant;\n'
        '                else art = "floor_bare_underhive_v" + variant;'
    )
    if marker not in text:
        raise SystemExit("Floor art selection marker missing")
    text = text.replace(marker, replacement, 1)

if "static String floorFamilyForContext(" not in text:
    pattern = re.compile(
        r"    static String roomFloorFamily\(World w, int x, int y, char glyph\) \{.*?\n    \}\n\n"
        r"    static String corridorOrientation",
        re.DOTALL,
    )
    replacement = '''    static String roomFloorFamily(World w, int x, int y, char glyph) {
        if (w == null || !w.inBounds(x, y)) return "bare_underhive";
        int rid = w.roomIdAt(x, y);
        return floorFamilyForContext(w.zoneType, w.roomFaction(rid), sewerContext(w, x, y), w.floor);
    }

    static String floorFamilyForContext(ZoneType zone, Faction faction, boolean sewer, int floor) {
        if (sewer || zone == ZoneType.SEWER_CONDUIT || zone == ZoneType.MUTANT_SEWER_CAMP || zone == ZoneType.CULTIST_SEWER_CAMP) {
            return "sewer_room";
        }
        if (isNobleFaction(faction) || zone == ZoneType.SECTOR_GOVERNORS_MANSION
                || zone == ZoneType.NOBLE_SERVICE_SPINE || floor >= 7) {
            return "noble_room";
        }
        if (zone == ZoneType.GANGER_TURF || zone == ZoneType.TRASH_WARREN || zone == ZoneType.MUTANT_WARRENS
                || faction == Faction.BANDIT || faction == Faction.MUTANT
                || (faction != null && faction.name().startsWith("GANGER_"))) {
            return "gang_or_trash_rough";
        }
        if (zone == ZoneType.SUMP_MARKET) return "market_room";
        if (zone == ZoneType.ARBITES_PRECINCT_EDGE || zone == ZoneType.IMPERIAL_GUARD_BILLET) return "security_room";
        if (zone == ZoneType.MECHANICUS_RELIC_DUCT || zone == ZoneType.MECHANICUS_FORGE_CLOISTER) return "industrial_room";
        if (zone == ZoneType.ADMINISTRATUM_ARCHIVE || zone == ZoneType.IMPERIAL_NEWS_NETWORK) return "administrative_room";
        if (zone == ZoneType.TRAIN_SERVICE_YARD || zone == ZoneType.NEUTRAL_RAIL_DEPOT) return "transit_room";
        if (isSecurityFaction(faction)) return "security_room";
        if (isMechanistFaction(faction)) return "industrial_room";
        if (isAdministrativeFaction(faction)) return "administrative_room";
        if (isReligiousFaction(faction)) return "religious_room";
        if (zone == ZoneType.HAB_STACK || zone == ZoneType.NEUTRAL_CIVILIAN_FLOOR || isHiverFaction(faction)) {
            return "habitation_room";
        }
        return "bare_underhive";
    }

    static boolean isNobleFaction(Faction faction) {
        return faction == Faction.NOBLE || (faction != null && faction.name().startsWith("NOBLE_HOUSE_"));
    }

    static boolean isHiverFaction(Faction faction) {
        return faction == Faction.HIVER || (faction != null && faction.name().startsWith("HIVER_BLOCK_"));
    }

    static boolean isSecurityFaction(Faction faction) {
        return faction == Faction.ARBITES || faction == Faction.CIVIC_WARDENS || faction == Faction.IMPERIAL_GUARD;
    }

    static boolean isMechanistFaction(Faction faction) {
        return faction == Faction.MECHANICUS || faction == Faction.MECHANIST_COLLEGIA
                || (faction != null && faction.name().startsWith("MECHANICUS_CLOISTER_"));
    }

    static boolean isAdministrativeFaction(Faction faction) {
        return faction == Faction.ADMINISTRATUM || faction == Faction.CIVIC_LEDGER_OFFICE || faction == Faction.INN;
    }

    static boolean isReligiousFaction(Faction faction) {
        return faction == Faction.MINISTORUM || faction == Faction.SORORITAS;
    }

    static SemanticRenderAssetResolver.RenderIntent floorIntentForFamily(String family) {
        if (family == null) return null;
        return switch (family) {
            case "sewer_room" -> SemanticRenderAssetResolver.RenderIntent.SEWER_FLOOR;
            case "gang_or_trash_rough" -> SemanticRenderAssetResolver.RenderIntent.SLUM_FLOOR;
            case "noble_room" -> SemanticRenderAssetResolver.RenderIntent.NOBLE_FLOOR;
            case "habitation_room" -> SemanticRenderAssetResolver.RenderIntent.HABITATION_FLOOR;
            case "market_room" -> SemanticRenderAssetResolver.RenderIntent.MARKET_FLOOR;
            case "security_room" -> SemanticRenderAssetResolver.RenderIntent.SECURITY_FLOOR;
            case "industrial_room" -> SemanticRenderAssetResolver.RenderIntent.INDUSTRIAL_FLOOR;
            case "administrative_room" -> SemanticRenderAssetResolver.RenderIntent.ADMINISTRATIVE_FLOOR;
            case "transit_room" -> SemanticRenderAssetResolver.RenderIntent.TRANSIT_FLOOR;
            case "religious_room" -> SemanticRenderAssetResolver.RenderIntent.RELIGIOUS_FLOOR;
            case "bare_underhive", "alleyway_cracked" -> SemanticRenderAssetResolver.RenderIntent.GENERIC_FLOOR;
            default -> null;
        };
    }

    static String primaryAssetIdForDescriptor(String baseLayer, String family, String artKey) {
        String direct = TileSemanticAssetAuthority.assetIdOrMissing(artKey);
        if (direct != null || !"floor".equals(baseLayer)) return direct;
        SemanticRenderAssetResolver.RenderIntent intent = floorIntentForFamily(family);
        if (intent == null) return null;
        SemanticRenderAssetResolver.Resolution resolution =
                SemanticRenderAssetResolver.resolve(AssetManager.registry(), intent);
        return resolution.found() ? resolution.asset.id() : MISSING_RECOGNIZED_TILE_ID;
    }

    static String corridorOrientation'''
    text, count = pattern.subn(replacement, text, count=1)
    if count != 1:
        raise SystemExit("Room floor family method marker missing")

assignment = "        this.primaryAssetId = TileSemanticAssetAuthority.assetIdOrMissing(primaryArtKey);"
new_assignment = "        this.primaryAssetId = TileDataCompilationAuthority.primaryAssetIdForDescriptor(baseLayer, family, primaryArtKey);"
if new_assignment not in text:
    if assignment not in text:
        raise SystemExit("Compiled descriptor asset marker missing")
    text = text.replace(assignment, new_assignment, 1)

COMPILER.write_text(text, encoding="utf-8", newline="")

SMOKE.write_text('''package mechanist;

/** Smoke for district and room context entering live compiled floor descriptors. */
final class Milestone02DistrictRoomTileContextSmoke {
    public static void main(String[] args) {
        requireFamily(ZoneType.HAB_STACK, Faction.NONE, false, 0, "habitation_room");
        requireFamily(ZoneType.SUMP_MARKET, Faction.NONE, false, 0, "market_room");
        requireFamily(ZoneType.ARBITES_PRECINCT_EDGE, Faction.NONE, false, 0, "security_room");
        requireFamily(ZoneType.MECHANICUS_FORGE_CLOISTER, Faction.NONE, false, 0, "industrial_room");
        requireFamily(ZoneType.ADMINISTRATUM_ARCHIVE, Faction.NONE, false, 0, "administrative_room");
        requireFamily(ZoneType.NEUTRAL_RAIL_DEPOT, Faction.NONE, false, 0, "transit_room");
        requireFamily(ZoneType.NEUTRAL_CIVILIAN_FLOOR, Faction.MINISTORUM, false, 0, "religious_room");
        requireFamily(ZoneType.NEUTRAL_CIVILIAN_FLOOR, Faction.NOBLE_HOUSE_VARN, false, 0, "noble_room");
        requireFamily(ZoneType.SEWER_CONDUIT, Faction.NONE, true, 0, "sewer_room");
        requireFamily(ZoneType.TRASH_WARREN, Faction.NONE, false, 0, "gang_or_trash_rough");

        World habitation = world(ZoneType.HAB_STACK);
        CompiledTileDescriptor hab = TileDataCompilationAuthority.resolveFresh(habitation, 4, 4, '.');
        require("habitation_room".equals(hab.family), "hab floor did not compile with habitation family: " + hab.inspectLine());
        require(hab.primaryArtKey.startsWith("floor_habitation_room_v"), "hab floor art key was not context-specific: " + hab.primaryArtKey);
        require(hab.primaryAssetId != null, "hab floor did not publish an asset or typed missing-art identity");

        World marketWorld = world(ZoneType.SUMP_MARKET);
        CompiledTileDescriptor market = TileDataCompilationAuthority.resolveFresh(marketWorld, 4, 4, '.');
        require("market_room".equals(market.family), "market floor did not compile with market family: " + market.inspectLine());
        require(market.primaryArtKey.startsWith("floor_market_room_v"), "market floor art key was not context-specific: " + market.primaryArtKey);
        require(!market.primaryArtKey.equals(hab.primaryArtKey), "different districts compiled to the same floor art key");

        World sewerWorld = world(ZoneType.SEWER_CONDUIT);
        sewerWorld.sewerLayer = true;
        CompiledTileDescriptor sewer = TileDataCompilationAuthority.resolveFresh(sewerWorld, 4, 4, '.');
        require("sewer_room".equals(sewer.family), "sewer floor did not retain sewer family: " + sewer.inspectLine());
        require(!"bare_underhive".equals(sewer.family), "sewer context silently degraded to generic floor");

        require(TileDataCompilationAuthority.floorIntentForFamily("market_room")
                        == SemanticRenderAssetResolver.RenderIntent.MARKET_FLOOR,
                "market family did not map to market semantic intent");
        require(TileDataCompilationAuthority.floorIntentForFamily("security_room")
                        == SemanticRenderAssetResolver.RenderIntent.SECURITY_FLOOR,
                "security family did not map to security semantic intent");
        String marketAsset = TileDataCompilationAuthority.primaryAssetIdForDescriptor(
                "floor", "market_room", "deliberately_missing_market_alias");
        require(marketAsset != null,
                "recognized district floor did not produce an asset or typed missing-art identity");

        System.out.println("Milestone02DistrictRoomTileContextSmoke PASS " + TileDataCompilationAuthority.VERSION);
    }

    private static World world(ZoneType zone) {
        World world = AssetAuditDevRoomAuthority.build(0xD157C7L + zone.ordinal());
        world.zoneType = zone;
        world.floor = 0;
        world.sewerLayer = false;
        world.tiles[4][4] = '.';
        if (world.roomIds != null) world.roomIds[4][4] = -1;
        return world;
    }

    private static void requireFamily(ZoneType zone, Faction faction, boolean sewer, int floor, String expected) {
        String actual = TileDataCompilationAuthority.floorFamilyForContext(zone, faction, sewer, floor);
        require(expected.equals(actual), "Expected " + zone + "/" + faction + " to use " + expected + " but got " + actual);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone02DistrictRoomTileContextSmoke() { }
}
''', encoding="utf-8", newline="")

suite_text = SUITE.read_text(encoding="utf-8").replace("\r\n", "\n")
smoke_call = "        Milestone02DistrictRoomTileContextSmoke.main(args);\n"
if smoke_call not in suite_text:
    marker = "        Milestone02SemanticRuntimeIntentBridgeSmoke.main(args);\n"
    if marker not in suite_text:
        raise SystemExit("Gate 3 semantic smoke marker missing")
    suite_text = suite_text.replace(marker, marker + smoke_call, 1)
SUITE.write_text(suite_text, encoding="utf-8", newline="")

print("District context source patch applied.")
