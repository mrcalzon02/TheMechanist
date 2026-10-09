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

        // Ordinary driving may spend only unreserved energy.
        MapObjectState reservedVehicle = vehicle(world);
        reservedVehicle.id = "VEHICLE-FUEL-RESERVATION-SMOKE";
        world.mapObjects.add(reservedVehicle);
        VehicleFuelAuthority.ensureInitialized(world, reservedVehicle);
        int full = VehicleFuelAuthority.inspect(world, reservedVehicle).current();
        reservedVehicle.stockState = MapObjectState.setStockFlag(
                reservedVehicle.stockState, "strategicTransitFuelReserved", "8");
        reservedVehicle.stockState = MapObjectState.setStockFlag(
                reservedVehicle.stockState, "strategicTransitState", "reserved");
        reservedVehicle.stockState = MapObjectState.setStockFlag(
                reservedVehicle.stockState, "strategicTransitReservationId", "FUEL-SMOKE-RES-1");
        String beforeOverdraw = reservedVehicle.stockState;
        VehicleFuelAuthority.Result overdraw = VehicleFuelAuthority.consumeCommitted(
                world, reservedVehicle, full - 7, 3_000, "local overdraw");
        require(!overdraw.success() && !overdraw.changed()
                        && beforeOverdraw.equals(reservedVehicle.stockState),
                "ordinary operation must not spend reserved strategic energy");

        VehicleFuelAuthority.Result local = VehicleFuelAuthority.consumeCommitted(
                world, reservedVehicle, 2, 3_001, "permitted local operation");
        VehicleFuelAuthority.Snapshot localFuel = VehicleFuelAuthority.inspect(
                world, reservedVehicle);
        require(local.success() && local.changed()
                        && localFuel.current() == full - 2
                        && localFuel.reserved() == 8
                        && localFuel.available() == full - 10,
                "local consumption must preserve strategic reservation");

        String beforePremature = reservedVehicle.stockState;
        VehicleFuelAuthority.Result premature =
                VehicleFuelAuthority.consumeStrategicReservation(
                        world, reservedVehicle, 8, 3_002, "premature transfer");
        require(!premature.success() && !premature.changed()
                        && beforePremature.equals(reservedVehicle.stockState),
                "reservation must not be consumed before committing");
        reservedVehicle.stockState = MapObjectState.setStockFlag(
                reservedVehicle.stockState, "strategicTransitState", "committing");
        String beforeMismatch = reservedVehicle.stockState;
        VehicleFuelAuthority.Result mismatch =
                VehicleFuelAuthority.consumeStrategicReservation(
                        world, reservedVehicle, 7, 3_003, "wrong amount");
        require(!mismatch.success() && !mismatch.changed()
                        && beforeMismatch.equals(reservedVehicle.stockState),
                "strategic commit must match the exact reserved amount");

        VehicleFuelAuthority.Result strategic =
                VehicleFuelAuthority.consumeStrategicReservation(
                        world, reservedVehicle, 8, 3_004, "committed transfer");
        VehicleFuelAuthority.Snapshot committedFuel =
                VehicleFuelAuthority.inspect(world, reservedVehicle);
        require(strategic.success() && strategic.changed()
                        && committedFuel.current() == full - 10
                        && committedFuel.reserved() == 0
                        && "FUEL-SMOKE-RES-1".equals(MapObjectState.stockValue(
                        reservedVehicle.stockState,
                        "strategicTransitFuelLastCommitId")),
                "strategic commit must spend reserved energy with a durable receipt");
        String afterCommit = reservedVehicle.stockState;
        VehicleFuelAuthority.Result duplicate =
                VehicleFuelAuthority.consumeStrategicReservation(
                        world, reservedVehicle, 8, 3_005, "duplicate transfer");
        require(!duplicate.success() && !duplicate.changed()
                        && afterCommit.equals(reservedVehicle.stockState),
                "duplicate commit must not mutate fuel ledger");

        // A corrupt replay must not reintroduce the reservation and debit it
        // again after the matching durable receipt has already been written.
        reservedVehicle.stockState = MapObjectState.setStockFlag(
                reservedVehicle.stockState, "strategicTransitFuelReserved", "8");
        String conflictingReceiptStock = reservedVehicle.stockState;
        VehicleFuelAuthority.Result conflictingReceipt =
                VehicleFuelAuthority.consumeStrategicReservation(
                        world, reservedVehicle, 8, 3_006, "receipt replay");
        require(!conflictingReceipt.success() && !conflictingReceipt.changed()
                        && conflictingReceiptStock.equals(reservedVehicle.stockState),
                "matching receipt must prevent re-debit even if reservation is restored");

        System.out.println("Milestone 06 vehicle fuel history and reservation smoke passed.");
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
