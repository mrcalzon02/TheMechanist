package mechanist;

import java.awt.Rectangle;
import java.util.Random;

public final class RoomShellAndRoadNeighborSmoke {
    private RoomShellAndRoadNeighborSmoke() { }

    public static void main(String[] args) {
        verifyRoomWallHasNoSecondOrdinaryShell();
        verifyDoorDoesNotPromoteRoadShape();
        System.out.println("RoomShellAndRoadNeighborSmoke OK");
    }

    private static void verifyRoomWallHasNoSecondOrdinaryShell() {
        World world = new World(1234L, 18, 18);
        for (int x = 0; x < world.w; x++) for (int y = 0; y < world.h; y++) world.tiles[x][y] = '#';
        Rectangle room = new Rectangle(6, 6, 6, 6);
        world.carve(room);
        world.rooms.add(room);
        int changed = InterstitialInfrastructureApi.applyInterstitialMass(world, new Random(99L));
        require(changed > 0, "expected interstitial mass conversion");
        require(world.tiles[5][8] != '#', "non-room tile outside authoritative west room wall stayed a second ordinary wall");
        require(world.tiles[6][8] == '#', "authoritative room shell was changed");
    }

    private static void verifyDoorDoesNotPromoteRoadShape() {
        RoadInfrastructureTileRules.Neighborhood n = new RoadInfrastructureTileRules.Neighborhood(
                true, true, false, false, false, false, false, false, false, false, true, false);
        RoadInfrastructureTileRules.TileChoice choice = RoadInfrastructureTileRules.chooseRoadTile(n, 1);
        require(choice.row == RoadInfrastructureTileRules.RoadRow.NORTH_SOUTH, "door incorrectly counted as a third road connection");
        require(!RoadInfrastructureTileRules.doorsCountAsRoads(), "road authority still reports doors as road neighbors");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
