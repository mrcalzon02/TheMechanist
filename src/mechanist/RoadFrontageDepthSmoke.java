package mechanist;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;

/** Verifies varied road frontage and an actual connected setback room stamp. */
public final class RoadFrontageDepthSmoke {
    private RoadFrontageDepthSmoke() { }

    public static void main(String[] args) {
        int shallow = 0, middle = 0, deep = 0;
        for (int salt = 0; salt < 120; salt++) {
            int[] setbacks = RoadFrontageDepthAuthority.candidateSetbacks(43210L, salt, 21, 60, 1, 0);
            require(Arrays.equals(setbacks,
                    RoadFrontageDepthAuthority.candidateSetbacks(43210L, salt, 21, 60, 1, 0)),
                    "same seed and anchor changed setback preference");
            require(setbacks[setbacks.length - 1] == 2 || setbacks[0] == 2,
                    "direct frontage fallback was lost");
            int preferred = setbacks[0];
            if (preferred == 2) shallow++;
            else if (preferred < 9) middle++;
            else deep++;
        }
        require(shallow > 12 && middle > 12 && deep > 12,
                "frontage generation does not distribute direct, mid-block, and deep placements");

        int deepSalt = -1;
        for (int salt = 0; salt < 200; salt++) {
            if (RoadFrontageDepthAuthority.candidateSetbacks(
                    43210L, salt, 21, 60, 1, 0)[0] >= 9) {
                deepSalt = salt;
                break;
            }
        }
        require(deepSalt >= 0, "no reproducible deep-block test salt found");

        World world = new World(43210L, 124, 124);
        for (int x = 0; x < world.w; x++)
            for (int y = 0; y < world.h; y++) world.tiles[x][y] = '#';
        world.zoneType = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        Rectangle plaza = new Rectangle(75, 75, 9, 9);
        world.carve(plaza);
        world.rooms.add(plaza);
        world.tiles[20][60] = RoadGridIntegrationAuthority.ROAD_LANE;
        world.tiles[21][60] = RoadGridIntegrationAuthority.SIDEWALK;
        // Only the eastward direction has adjacent wall mass; all other
        // candidates are non-buildable, so the test examines real deep placement.
        world.tiles[21][59] = RoadGridIntegrationAuthority.SIDEWALK;
        world.tiles[21][61] = RoadGridIntegrationAuthority.SIDEWALK;
        // The additional sidewalks must not become independent room anchors.
        world.tiles[20][59] = RoadGridIntegrationAuthority.ROAD_LANE;
        world.tiles[20][61] = RoadGridIntegrationAuthority.ROAD_LANE;
        StampedRoomSpec spec = new StampedRoomSpec(
                "ROOM", "Interior Hab Room", "Long interior access corridor",
                12, Faction.HIVER, new String[]{"cloth scraps"}, new char[]{'q'});
        int result = world.tryPlaceRoadFirstRoom(spec, deepSalt);
        require(result > 0 && world.rooms.size() == 2,
                "deep-block room placement did not create a room");
        Rectangle room = world.rooms.get(1);
        require(room.x - 21 >= 9, "room was forced back onto direct frontage instead of block interior");
        int doorX = room.x;
        int doorY = 60;
        require(world.isDoorSymbol(world.tiles[doorX][doorY]),
                "set-back room missing its road-facing doorway");
        for (int x = 22; x < doorX; x++) {
            require(world.roomIds[x][60] < 0 && world.isCorridorGlyph(world.tiles[x][60]),
                    "deep room corridor has an uncarved or room-occupied tile at x=" + x);
        }
        require(world.tiles[20][60] == RoadGridIntegrationAuthority.ROAD_LANE
                        && world.tiles[21][60] == RoadGridIntegrationAuthority.SIDEWALK,
                "deep room connector destroyed arterial/sidewalk tiles");
        System.out.println("RoadFrontageDepthSmoke PASS shallow=" + shallow
                + " middle=" + middle + " deep=" + deep
                + " actualRoomSetback=" + (room.x - 21)
                + " connected=true streetIntact=true");
    }

    private static void require(boolean value, String detail) {
        if (!value) throw new AssertionError(detail);
    }
}
