package mechanist;

/** Smoke for room, corridor, and entity mapping onto semantic tile-state layers. */
final class Milestone02ZoneTileLayerMappingSmoke {
    public static void main(String[] args) {
        ZoneTileState roomTile = ZoneTileState.fromLegacyGlyph('.')
                .markRoom("room-a", Faction.CIVIC_WARDENS)
                .addObject("desk-1", "work_desk", "work desk", true, false)
                .addObject("crate-1", "supply_crate", "supply crate", false, true)
                .addLooseItem("coin-1", "coin", "loose coin")
                .addLight(ZoneTileState.LightKind.ROOM_LIGHT, 80, "lamp-a");
        ZoneTileLayerMappingAuditAuthority.MappingAudit roomAudit = ZoneTileLayerMappingAuditAuthority.auditRoomTile(roomTile);
        require(roomAudit.valid(), "room tile should map cleanly to semantic layers: " + roomAudit.findings());
        require(roomAudit.layerCounts().containsKey(ZoneTileLayerMappingAuditAuthority.TileLayer.SURFACE), "room audit should count surface layer");
        require(roomAudit.layerCounts().containsKey(ZoneTileLayerMappingAuditAuthority.TileLayer.SPACE), "room audit should count space layer");
        require(roomAudit.layerCounts().containsKey(ZoneTileLayerMappingAuditAuthority.TileLayer.OWNERSHIP), "room audit should count ownership layer");
        require(roomAudit.layerCounts().containsKey(ZoneTileLayerMappingAuditAuthority.TileLayer.STRUCTURE), "room audit should count structure layer");
        require(roomAudit.layerCounts().containsKey(ZoneTileLayerMappingAuditAuthority.TileLayer.CONTENT), "room audit should count content layer");
        require(roomAudit.layerCounts().containsKey(ZoneTileLayerMappingAuditAuthority.TileLayer.LIGHTING), "room audit should count lighting layer");

        ZoneTileState corridorTile = ZoneTileState.fromLegacyGlyph('.')
                .markCorridor("corridor-b")
                .reserve("patrol lane");
        ZoneTileLayerMappingAuditAuthority.MappingAudit corridorAudit = ZoneTileLayerMappingAuditAuthority.auditCorridorTile(corridorTile);
        require(corridorAudit.valid(), "corridor tile should map cleanly to semantic layers: " + corridorAudit.findings());
        require(corridorAudit.layerCounts().containsKey(ZoneTileLayerMappingAuditAuthority.TileLayer.STRUCTURE), "corridor audit should count structure layer");

        ZoneTileState entityTile = ZoneTileState.fromLegacyGlyph('.')
                .markRoom("room-c", Faction.BANDIT)
                .setOccupantEntityId("npc-77")
                .setPetEntityId("pet-11")
                .setVehicleId("cart-3");
        ZoneTileLayerMappingAuditAuthority.MappingAudit entityAudit = ZoneTileLayerMappingAuditAuthority.auditEntityTile(entityTile);
        require(entityAudit.valid(), "entity tile should map actor data cleanly: " + entityAudit.findings());
        require(entityAudit.layerCounts().containsKey(ZoneTileLayerMappingAuditAuthority.TileLayer.ACTOR), "entity audit should count actor layer");
        requireContains(ZoneTileLayerMappingAuditAuthority.playerFacingSummary(entityTile), "Layer mapping ready", "player-facing mapping summary");

        ZoneTileState brokenEntityTile = ZoneTileState.fromLegacyGlyph('.');
        ZoneTileLayerMappingAuditAuthority.MappingAudit brokenEntityAudit = ZoneTileLayerMappingAuditAuthority.auditEntityTile(brokenEntityTile);
        require(!brokenEntityAudit.valid(), "entity audit should fail without entity slot");
        requireContains(brokenEntityAudit.findings().toString(), "Missing occupant entity id", "broken entity audit finding");
        requireContains(ZoneTileLayerMappingAuditAuthority.auditSummary(), "legacyGlyph=bridgeOnly", "layer audit summary");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text == null || !text.contains(expected)) {
            throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
        }
    }

    private Milestone02ZoneTileLayerMappingSmoke() { }
}
