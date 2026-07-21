package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Focused smoke for bounded persistent vehicle fuel and power provenance. */
final class Milestone06VehicleFuelHistorySmoke {
    public static void main(String[] args) {
        World world = world();
        MapObjectState vehicle = vehicle(world);
        world.mapObjects.add(vehicle);
        VehicleFuelAuthority.ensureInitialized(world, vehicle);

        for (int i = 0; i < 20; i++) {
            VehicleFuelAuthority.Result consumed =
                    VehicleFuelAuthority.consumeCommitted(world, vehicle, 1,
                            2_000 + i * 2,
                            "history smoke consume " + i);
            require(consumed.success() && consumed.changed()
                            && consumed.amount() == 1,
                    "fuel history consume cycle " + i + " should succeed");

            VehicleFuelAuthority.Result refueled =
                    VehicleFuelAuthority.refuelForFaction(world, vehicle, 1,
                            2_001 + i * 2,
                            Faction.MECHANIST_COLLEGIA,
                            "history smoke refill " + i);
            require(refueled.success() && refueled.changed()
                            && refueled.amount() == 1,
                    "fuel history refill cycle " + i + " should succeed");
        }

        VehicleFuelAuthority.Snapshot snapshot =
                VehicleFuelAuthority.inspect(world, vehicle);
        List<String> history = history(vehicle);
        require(snapshot.current() == snapshot.capacity()
                        && snapshot.state().equals("full"),
                "repeated bounded-history cycles must preserve the final fuel ledger");
        require(history.size() == 12,
                "fuel history must retain exactly the newest twelve entries: "
                        + history.size());
        require(history.get(0).contains("history smoke consume 14"),
                "oldest retained fuel entry should be consume cycle 14: "
                        + history.get(0));
        require(history.get(history.size() - 1)
                        .contains("history smoke refill 19"),
                "newest retained fuel entry should be refill cycle 19: "
                        + history.get(history.size() - 1));
        require(!String.join("~", history).contains("history smoke consume 13")
                        && !String.join("~", history).contains("Ledger initialized"),
                "retired fuel history must not remain in the persistent stock record");

        System.out.println("Milestone 06 vehicle fuel history smoke passed.");
    }

    private static World world() {
        World world = new World(61016L, 12, 10);
        world.zoneType = ZoneType.MECHANICUS_FORGE_CLOISTER;
        world.mapObjects.clear();
        for (int x = 0; x < world.w; x++) {
            for (int y = 0; y < world.h; y++) world.tiles[x][y] = '.';
        }
        return world;
    }

    private static MapObjectState vehicle(World world) {
        MapObjectState vehicle = new MapObjectState();
        vehicle.id = "VEHICLE-FUEL-HISTORY-SMOKE";
        vehicle.type = AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK;
        vehicle.label = "Mechanist fuel-history cargo truck";
        vehicle.glyph = 'N';
        vehicle.x = 4;
        vehicle.y = 4;
        vehicle.stockState = "";
        VehicleRuntimeAuthority.initialize(world, vehicle,
                Faction.MECHANIST_COLLEGIA, "faction",
                "fuel-history-smoke", false,
                new java.util.Random(61016L));
        return vehicle;
    }

    private static List<String> history(MapObjectState vehicle) {
        String text = MapObjectState.stockValue(vehicle.stockState,
                "fuelOrPowerHistory");
        ArrayList<String> entries = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            for (String token : text.split("~")) {
                if (token != null && !token.isBlank()) entries.add(token);
            }
        }
        return List.copyOf(entries);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone06VehicleFuelHistorySmoke() { }
}
