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

        World seventh = streetedWorld(893L, 7, original);
        require(MultiFloorBuildingPlacementAuthority.stampFloor(seventh) == 4,
                "upper atrium floor does not contain its surrounding rooms");
        Rectangle voidRect = seventh.buildingOpenVolumes.get(0);
        int vx = voidRect.x + voidRect.width / 2, vy = voidRect.y + voidRect.height / 2;
        require(seventh.tiles[vx][vy] == InterstitialInfrastructureApi.VOID_SPACE
                        && !seventh.walkable(vx, vy) && seventh.roomIds[vx][vy] == -1,
                "upper atrium opening was incorrectly made into a walkable room/floor");

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
