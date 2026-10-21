package mechanist;

import java.awt.Rectangle;
import java.util.List;
import java.util.Random;

/** Real stamped buildings, adjacent-floor links, denial and preservation. */
public final class MultiFloorBuildingTransitSmoke {
    private MultiFloorBuildingTransitSmoke() { }

    public static void main(String[] args) {
        var sites = MultiFloorBuildingPlacementAuthority.planSites(731113L, 1, 1, 2, 2, 500, 500);
        World level2 = level(2, 891L, sites);
        World level3 = level(3, 892L, sites);
        World level4 = level(4, 893L, sites);
        for (World w : new World[]{level2, level3, level4})
            require(MultiFloorBuildingPlacementAuthority.stampFloor(w) == 8,
                    "expected two physically stamped apartment buildings on floor " + w.floor);

        var site = sites.get(0);
        for (var use : new VerticalBuildingBlueprintAuthority.Use[]{
                VerticalBuildingBlueprintAuthority.Use.STAIRWELL,
                VerticalBuildingBlueprintAuthority.Use.ELEVATOR}) {
            Rectangle rect = site.translate(site.blueprint().plate(2).space(use).bounds());
            int x = rect.x + 1, y = rect.y + 1;
            var climb = MultiFloorBuildingTransitAuthority.resolve(level2, level3, x, y, 1);
            var descend = MultiFloorBuildingTransitAuthority.resolve(level3, level2, x, y, -1);
            var upper = MultiFloorBuildingTransitAuthority.resolve(level3, level4, x, y, 1);
            require(climb.allowed() && descend.allowed() && upper.allowed(),
                    "stairs or lift cannot travel between aligned, physically built adjacent floors");
            require(climb.to().buildingId().equals(climb.from().buildingId())
                            && climb.to().x() == x && climb.to().y() == y,
                    "destination changed building identity or shaft coordinates");
            require(!MultiFloorBuildingTransitAuthority.resolve(level2, level4, x, y, 1).allowed(),
                    "building travel skipped a floor");
            require(!MultiFloorBuildingTransitAuthority.resolve(level4, level3, x, y, 1).allowed(),
                    "building travel went against the selected direction");
            require(MultiFloorBuildingTransitAuthority.landingAt(level2, x + 1, y) == null,
                    "walking near the shaft must not permit a floor teleport");
            World blocked = level(3, 900L, sites);
            require(!MultiFloorBuildingTransitAuthority.resolve(level2, blocked, x, y, 1).allowed(),
                    "link to a building not physically built should be refused");
            MultiFloorBuildingPlacementAuthority.stampFloor(blocked);
            blocked.tiles[x][y] = '#';
            require(!MultiFloorBuildingTransitAuthority.resolve(level2, blocked, x, y, 1).allowed(),
                    "blocked destination landing must not be accepted");
        }

        World mismatched = level(3, 897L, sites);
        MultiFloorBuildingPlacementAuthority.stampFloor(mismatched);
        mismatched.zoneX = 3;
        var stairs = site.translate(site.blueprint().plate(2).space(
                VerticalBuildingBlueprintAuthority.Use.STAIRWELL).bounds());
        require(!MultiFloorBuildingTransitAuthority.resolve(level2, mismatched,
                        stairs.x + 1, stairs.y + 1, 1).allowed(),
                "travel crossed between different zones with the same local shaft coordinates");

        System.out.println("MultiFloorBuildingTransitSmoke PASS stairs=true lifts=true "
                + "adjacentOnly=true blockedTargetDenied=true sameBuilding=true");
    }

    private static World level(int floor, long seed, List<MultiFloorBuildingPlacementAuthority.Site> sites) {
        World w = new World(seed, 500, 500);
        w.sectorX = 1; w.sectorY = 1; w.zoneX = 2; w.zoneY = 2;
        w.floor = floor; w.zoneType = ZoneType.HAB_STACK;
        for (int x = 0; x < w.w; x++)
            for (int y = 0; y < w.h; y++) w.tiles[x][y] = '#';
        Rectangle plaza = w.centralPlazaRect();
        w.carve(plaza);
        w.rooms.add(plaza);
        RoadGridIntegrationAuthority.apply(w, new Random(seed));
        w.buildingSites = sites;
        return w;
    }
    private static void require(boolean valid, String reason) {
        if (!valid) throw new AssertionError(reason);
    }
}
