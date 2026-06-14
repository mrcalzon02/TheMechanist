package mechanist;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Random;

/** Smoke coverage for faction vehicle and street-frontage vehicle commerce seeding. */
public final class VehicleEconomyFrontageSmoke {
    private VehicleEconomyFrontageSmoke() {}

    public static void main(String[] args) {
        require(VehicleEconomyFrontageAuthority.factionsForZone(ZoneType.GANGER_TURF).size() == 8, "ganger vehicle roster is incomplete");
        require(VehicleEconomyFrontageAuthority.factionsForZone(ZoneType.NOBLE_SERVICE_SPINE).size() == 7, "noble vehicle roster is incomplete");
        require(VehicleEconomyFrontageAuthority.factionsForZone(ZoneType.MECHANICUS_FORGE_CLOISTER).size() == 4, "Mechanist vehicle roster is incomplete");
        World world = new World(8675309L, 180, 100);
        world.zoneType = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        ArrayList<Point> candidates = new ArrayList<>();
        for (int x = 4; x < world.w - 4; x += 5) for (int y = 4; y < world.h - 4; y += 5) {
            world.tiles[x][y] = RoadGridIntegrationAuthority.SIDEWALK;
            candidates.add(new Point(x, y));
        }

        VehicleEconomyFrontageAuthority.Result result = VehicleEconomyFrontageAuthority.apply(world, candidates, new Random(42L));
        require(result.dealerships == 1, "dealership was not seeded");
        require(result.partsStores == 1, "parts store was not seeded");
        require(result.serviceGarages == 1, "service garage was not seeded");
        require(result.transitStops == 1, "public transit stop was not seeded");
        require(result.motorPools == VehicleEconomyFrontageAuthority.factionsForZone(world.zoneType).size(), "not every zone faction received a motor pool");
        require(result.personalVehicles == result.motorPools, "not every zone faction received personal transport");
        require(result.factionVehicles == result.motorPools, "not every zone faction received faction transport");
        require(result.publicVehicles == 1, "public transport vehicle was not seeded");

        int ownedVehicles = 0;
        for (MapObjectState m : world.mapObjects) {
            if (!RoadTransitFixtureAuthority.isVehicleType(m.type)) continue;
            require(!MapObjectState.stockValue(m.stockState, "ownerFaction").isBlank(), "vehicle missing ownerFaction: " + m.summary());
            require(!MapObjectState.stockValue(m.stockState, "ownership").isBlank(), "vehicle missing ownership: " + m.summary());
            require(!MapObjectState.stockValue(m.stockState, "vehicleRole").isBlank(), "vehicle missing vehicleRole: " + m.summary());
            ownedVehicles++;
        }
        require(ownedVehicles == result.personalVehicles + result.factionVehicles + result.publicVehicles, "vehicle result count disagrees with generated records");
        System.out.println("VehicleEconomyFrontageSmoke OK " + result.summary());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
