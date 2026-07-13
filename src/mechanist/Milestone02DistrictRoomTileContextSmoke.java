package mechanist;

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
