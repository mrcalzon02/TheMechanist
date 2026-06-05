package mechanist;

import java.util.ArrayList;

/** Smoke coverage for road atlas row rules, 4x4 crossings, sidewalks, and curb parking. */
public final class RoadTemplateRulesSmoke {
    private RoadTemplateRulesSmoke() { }

    public static void main(String[] args) {
        verifyIntersectionFootprint();
        verifyCurbParkingShoulder();
        System.out.println("RoadTemplateRulesSmoke OK");
    }

    private static void verifyIntersectionFootprint() {
        World world = new World(24680L, 48, 36);
        fill(world, '.');
        RoadGridIntegrationAuthority.Result result = new RoadGridIntegrationAuthority.Result();
        RoadGridIntegrationAuthority.carveHorizontalStreet(world, 18, 2, world.w - 3, result);
        RoadGridIntegrationAuthority.carveVerticalStreet(world, 24, 2, world.h - 3, result);
        ArrayList<Integer> verticalCenters = new ArrayList<>();
        ArrayList<Integer> horizontalCenters = new ArrayList<>();
        verticalCenters.add(24);
        horizontalCenters.add(18);
        RoadGridIntegrationAuthority.stampIntersectionFootprints(world, result, verticalCenters, horizontalCenters);
        TileDataCompilationAuthority.compile(world);

        int intersection = 0;
        for(int x=22; x<=25; x++) for(int y=16; y<=19; y++) {
            require(world.tiles[x][y] == RoadGridIntegrationAuthority.ROAD_LANE, "4x4 crossing cell was not a road lane at " + x + "," + y);
            CompiledTileDescriptor d = world.compiledTileDescriptors[x][y];
            require(d != null && "intersection".equals(d.shape), "4x4 crossing cell did not resolve to intersection at " + x + "," + y + " -> " + (d == null ? "null" : d.composedKey));
            require(d.primaryArtKey != null && d.primaryArtKey.startsWith("road_intersection_v"), "intersection art key was not row-4 road art: " + d.primaryArtKey);
            intersection++;
        }
        require(intersection == 16, "expected exactly sixteen stamped crossing cells");
    }

    private static void verifyCurbParkingShoulder() {
        World world = new World(13579L, 32, 18);
        fill(world, '.');
        RoadGridIntegrationAuthority.Result result = new RoadGridIntegrationAuthority.Result();
        RoadGridIntegrationAuthority.carveHorizontalStreet(world, 9, 2, world.w - 3, result);
        boolean carved = RoadGridIntegrationAuthority.carveHorizontalParkingRun(world, result, 5, 7, 6, 6, new ArrayList<Integer>());
        require(carved, "expected curb parking run to carve");
        TileDataCompilationAuthority.compile(world);

        for(int x=5; x<11; x++) {
            require(world.tiles[x][6] == RoadGridIntegrationAuthority.SIDEWALK, "outer sidewalk missing at x=" + x);
            require(world.tiles[x][7] == RoadGridIntegrationAuthority.PARKING_SPACE, "parking shoulder missing at x=" + x);
            require(world.tiles[x][8] == RoadGridIntegrationAuthority.ROAD_LANE, "first road lane missing at x=" + x);
            require(world.tiles[x][9] == RoadGridIntegrationAuthority.ROAD_LANE, "second road lane missing at x=" + x);
            require(world.tiles[x][10] == RoadGridIntegrationAuthority.SIDEWALK, "opposite sidewalk missing at x=" + x);
            CompiledTileDescriptor parking = world.compiledTileDescriptors[x][7];
            require(parking != null && "parking".equals(parking.shape), "parking cell did not compile as parking at x=" + x);
            require(parking.primaryArtKey != null && parking.primaryArtKey.startsWith("road_parking_v"), "parking art key was not road_parking: " + (parking == null ? "null" : parking.primaryArtKey));
        }
    }

    private static void fill(World world, char glyph) {
        for(int x=0; x<world.w; x++) for(int y=0; y<world.h; y++) world.tiles[x][y] = glyph;
    }

    private static void require(boolean condition, String message) {
        if(!condition) throw new IllegalStateException(message);
    }
}
