package mechanist;

import java.awt.Rectangle;
import java.util.List;
import java.util.Random;

/** Physical geometry and rejection tests for deterministically shared floor sites. */
public final class MultiFloorBuildingPlacementSmoke {
    private MultiFloorBuildingPlacementSmoke() { }

    public static void main(String[] args) {
        List<MultiFloorBuildingPlacementAuthority.Site> original =
                MultiFloorBuildingPlacementAuthority.planSites(731113L, 1, 1, 2, 2, 500, 500);
        require(original.equals(MultiFloorBuildingPlacementAuthority.planSites(
                        731113L, 1, 1, 2, 2, 500, 500)),
                "building sites changed after regenerating the same sector");
        require(original.size() == 3, "missing residential/atrium building sites");

        World second = streetedWorld(891L, 2, original);
        World fourth = streetedWorld(892L, 4, original);
        require(MultiFloorBuildingPlacementAuthority.stampFloor(second) == 8,
                "two separate apartment complexes were not physically built on floor 2");
        require(MultiFloorBuildingPlacementAuthority.stampFloor(fourth) == 8,
                "same apartment buildings did not persist through floor 4");
        require(second.buildingRoomIds.size() == 8
                        && second.buildingRoomIds.keySet().stream().allMatch(fourth.buildingRoomIds::containsKey) == false,
                "room IDs should retain distinct identities on separate floors");
        for (var site : original.subList(0, 2)) {
            Rectangle footprint = site.footprint();
            require(second.buildingSites.contains(site) && fourth.buildingSites.contains(site),
                    "physical floors no longer refer to the same building site");
            var stairs = site.translate(site.blueprint().plate(2).space(
                    VerticalBuildingBlueprintAuthority.Use.STAIRWELL).bounds());
            var lift = site.translate(site.blueprint().plate(2).space(
                    VerticalBuildingBlueprintAuthority.Use.ELEVATOR).bounds());
            require(second.isCorridorGlyph(second.tiles[stairs.x + 1][stairs.y + 1])
                            && fourth.isCorridorGlyph(fourth.tiles[stairs.x + 1][stairs.y + 1]),
                    "building stair landing misaligned between floors");
            require(second.tiles[lift.x + 1][lift.y + 1] == 'E'
                            && fourth.tiles[lift.x + 1][lift.y + 1] == 'E',
                    "building lift shaft missing or shifted on upper floor");
            require(MultiFloorBuildingPlacementAuthority.reserved(second, footprint),
                    "ordinary generation no longer treats the building footprint as reserved");
            Rectangle approach = MultiFloorBuildingPlacementAuthority.facadeAccessReservation(
                    site, second.w);
            require(MultiFloorBuildingPlacementAuthority.reserved(second, approach)
                            && !footprint.intersects(approach),
                    "ordinary rooms may block the building entrance-to-road corridor");
        }

        World sixth = streetedWorld(8921L, 6, original);
        require(MultiFloorBuildingPlacementAuthority.stampFloor(sixth) == 5,
                "ground atrium did not stamp its four galleries and hall");
        var groundAtrium = original.get(2);
        require(sixth.isDoorSymbol(sixth.tiles[groundAtrium.x() + 14][groundAtrium.y() + 5])
                        && sixth.isDoorSymbol(sixth.tiles[groundAtrium.x() + 14][groundAtrium.y() + 27]),
                "lower atrium galleries were generated without accessible doors");
        World seventh = streetedWorld(893L, 7, original);
        require(MultiFloorBuildingPlacementAuthority.stampFloor(seventh) == 4,
                "upper atrium floor does not contain its surrounding rooms");
        Rectangle voidRect = seventh.buildingOpenVolumes.get(0);
        int vx = voidRect.x + voidRect.width / 2, vy = voidRect.y + voidRect.height / 2;
        require(seventh.tiles[vx][vy] == InterstitialInfrastructureApi.VOID_SPACE
                        && !seventh.walkable(vx, vy) && seventh.roomIds[vx][vy] == -1,
                "upper atrium opening was incorrectly made into a walkable room/floor");
        ZoneTileGrid semantic = StructuredZoneGridRuntime.rebuildGridForWorld(seventh);
        ZoneTileState air = semantic.requireTile(vx, vy);
        require(air.spaceType() == ZoneTileState.SpaceType.OPEN_VOLUME
                        && air.baseType() == ZoneTileState.BaseTileType.VOID
                        && air.hasFlag(ZoneTileState.TileFlag.BLOCKS_MOVEMENT)
                        && air.blocksRoomPlacement()
                        && air.reservationLabel().contains(original.get(2).blueprint().buildingId()),
                "atrium open shaft lost its building identity or placement/collision semantics");
        require(semantic.toLegacyGlyphs()[vx][vy] == InterstitialInfrastructureApi.VOID_SPACE,
                "semantic conversion flattened an upper open shaft into walkable legacy floor");

        World rejected = streetedWorld(894L, 2, original);
        var eastApartment = original.get(0);
        Rectangle blocked = eastApartment.footprint();
        rejected.tiles[blocked.x + 12][blocked.y + 10] =
                RoadGridIntegrationAuthority.ROAD_LANE;
        int before = rejected.rooms.size();
        int built = MultiFloorBuildingPlacementAuthority.stampFloor(rejected);
        require(built == 4 && rejected.rooms.size() == before + 4,
                "blocked building must reject its whole floor plate, without partial rooms");
        require(rejected.tiles[blocked.x + 12][blocked.y + 10]
                        == RoadGridIntegrationAuthority.ROAD_LANE,
                "existing road was destroyed by a rejected multi-floor structure");
        World renumber = new World(271L, 44, 44);
        for (int x = 0; x < renumber.w; x++)
            for (int y = 0; y < renumber.h; y++) renumber.tiles[x][y] = '#';
        Rectangle plaza = new Rectangle(20, 20, 5, 5);
        Rectangle obsolete = new Rectangle(3, 3, 5, 5);
        Rectangle owned = new Rectangle(11, 11, 5, 5);
        for (Rectangle room : new Rectangle[]{plaza, obsolete, owned}) {
            renumber.carve(room);
            renumber.rooms.add(room);
        }
        String retained = "BLD-REINDEX/F2/APARTMENT/0";
        renumber.buildingRoomIds.put(retained, 2);
        renumber.buildingRoomIds.put("BLD-REINDEX/F2/OLD", 1);
        renumber.removeRoomAtIndex(1);
        require(renumber.buildingRoomIds.size() == 1
                        && renumber.buildingRoomIds.get(retained) == 1,
                "removing a preceding room left the building ledger at a stale room index");
        renumber.removeRoomAtIndex(1);
        require(renumber.buildingRoomIds.isEmpty(),
                "removing a building room left a phantom building-room reference");

        System.out.println("MultiFloorBuildingPlacementSmoke PASS "
                + "apartmentFloors=2,4 atriumFloor=7 roadCollisionRejected=true");
    }

    private static World streetedWorld(long sliceSeed, int floor,
                                       List<MultiFloorBuildingPlacementAuthority.Site> sites) {
        World w = new World(sliceSeed, 500, 500);
        w.floor = floor;
        w.zoneType = ZoneType.HAB_STACK;
        for (int x = 0; x < w.w; x++)
            for (int y = 0; y < w.h; y++) w.tiles[x][y] = '#';
        Rectangle plaza = w.centralPlazaRect();
        w.carve(plaza);
        w.rooms.add(plaza);
        RoadGridIntegrationAuthority.apply(w, new Random(sliceSeed));
        w.buildingSites = sites;
        return w;
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
