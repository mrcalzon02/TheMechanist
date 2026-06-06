package mechanist;

/** Smoke for structured zone tile slots beyond single-glyph floor encoding. */
final class Milestone02ZoneTileSlotStateSmoke {
    public static void main(String[] args) {
        ZoneTileState wall = ZoneTileState.fromLegacyGlyph('\u2588');
        require(wall.baseType() == ZoneTileState.BaseTileType.WALL, "unicode block wall glyph should import as wall");
        require(wall.hasFlag(ZoneTileState.TileFlag.BLOCKS_MOVEMENT), "wall should block movement");

        ZoneTileState tile = ZoneTileState.fromLegacyGlyph('.')
                .markRoom("hab-17", Faction.CIVIC_WARDENS)
                .addObject("locker-1", "storage_locker", "sealed locker", true, true)
                .addLooseItem("ration-1", "ration_pack", "ration pack")
                .setOccupantEntityId("worker-44")
                .setPetEntityId("cat-2")
                .setVehicleId("cart-9")
                .addLight(ZoneTileState.LightKind.ROOM_LIGHT, 70, "lamp-1")
                .reserve("delivery route");

        require(tile.baseType() == ZoneTileState.BaseTileType.FLOOR, "tile surface should remain floor");
        require(tile.legacyGlyph() == '.', "legacy glyph should remain an export bridge");
        require(tile.spaceType() == ZoneTileState.SpaceType.ROOM, "room marker should set room space");
        require(tile.ownerFaction() == Faction.CIVIC_WARDENS, "owner faction should be stored separately from glyph");
        require(tile.hasSlot(ZoneTileState.TileSlot.SURFACE), "surface slot should exist");
        require(tile.hasSlot(ZoneTileState.TileSlot.ROOM), "room slot should exist");
        require(tile.hasSlot(ZoneTileState.TileSlot.CONTAINER), "container slot should exist");
        require(tile.hasSlot(ZoneTileState.TileSlot.LOOSE_ITEM), "loose item slot should exist");
        require(tile.hasSlot(ZoneTileState.TileSlot.ENTITY), "entity slot should exist");
        require(tile.hasSlot(ZoneTileState.TileSlot.PET), "pet slot should exist");
        require(tile.hasSlot(ZoneTileState.TileSlot.VEHICLE), "vehicle slot should exist");
        require(tile.hasSlot(ZoneTileState.TileSlot.LIGHT), "light slot should exist");
        require(tile.hasSlot(ZoneTileState.TileSlot.RESERVATION), "reservation slot should exist");
        require(tile.hasFloorSpaceContent(), "floor tile should report layered floor-space content");
        require(tile.slotSnapshot().containsKey(ZoneTileState.TileSlot.CONTAINER), "snapshot should include container slot");
        requireContains(tile.playerFacingSlotSummary(), "container", "player-facing slot summary");
        requireContains(tile.playerFacingSlotSummary(), "loose item", "player-facing loose item summary");

        tile.setOccupantEntityId("");
        require(!tile.hasSlot(ZoneTileState.TileSlot.ENTITY), "clearing occupant should clear entity slot");
        require(!tile.hasFlag(ZoneTileState.TileFlag.HAS_ENTITY), "clearing occupant should clear entity flag");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text == null || !text.contains(expected)) {
            throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
        }
    }

    private Milestone02ZoneTileSlotStateSmoke() { }
}
