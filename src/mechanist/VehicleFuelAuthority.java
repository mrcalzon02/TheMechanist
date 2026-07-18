package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Persistent fuel or power ledger attached to the authoritative vehicle fixture.
 * Reservations never consume this ledger; only a committed operation may deduct
 * units. Refueling is permission-gated and atomic.
 */
final class VehicleFuelAuthority {
    record Snapshot(int current, int capacity, int reserved, int available,
                    String energyType, String state) { }

    record Result(boolean success, boolean changed, int before, int after,
                  int amount, String message) {
        static Result blocked(int current, String message) {
            return new Result(false, false, current, current, 0,
                    message == null || message.isBlank()
                            ? "Vehicle fuel or power action was blocked." : message);
        }
    }

    private VehicleFuelAuthority() { }

    static Snapshot inspect(World world, MapObjectState vehicle) {
        if (vehicle == null || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            return new Snapshot(0, 0, 0, 0, "unavailable", "missing");
        }
        ensureInitialized(world, vehicle);
        int current = intValue(value(vehicle, "fuelOrPowerCurrent"), 0);
        int capacity = intValue(value(vehicle, "fuelOrPowerCapacity"), 0);
        int reserved = Math.min(current, Math.max(0,
                intValue(value(vehicle, "strategicTransitFuelReserved"), 0)));
        return new Snapshot(current, capacity, reserved,
                Math.max(0, current - reserved),
                value(vehicle, "fuelOrPowerType"),
                current <= 0 ? "empty" : current < Math.max(1, capacity / 4)
                        ? "low" : current >= capacity ? "full" : "serviceable");
    }

    static void ensureInitialized(World world, MapObjectState vehicle) {
        if (vehicle == null || !VehicleRuntimeAuthority.isVehicle(vehicle)) return;
        VehicleRuntimeAuthority.ensureInitialized(world, vehicle);
        VehicleRuntimeAuthority.VehicleClass definition =
                VehicleRuntimeAuthority.vehicleClass(vehicle.type);
        int capacity = capacity(definition);
        if (value(vehicle, "fuelOrPowerCapacity").isBlank()) {
            set(vehicle, "fuelOrPowerCapacity", Integer.toString(capacity));
        }
        if (value(vehicle, "fuelOrPowerCurrent").isBlank()) {
            set(vehicle, "fuelOrPowerCurrent", Integer.toString(capacity));
        }
        if (value(vehicle, "fuelOrPowerType").isBlank()) {
            set(vehicle, "fuelOrPowerType",
                    definition == VehicleRuntimeAuthority.VehicleClass.UTILITY_BIKE
                            ? "compact power cell" : "vehicle fuel or power reserve");
        }
        if (value(vehicle, "fuelOrPowerHistory").isBlank()) {
            append(vehicle, "fuelOrPowerHistory",
                    "Ledger initialized at " + capacity + "/" + capacity);
        }
        clampLedger(vehicle);
    }

    static Result consumeCommitted(World world, MapObjectState vehicle,
                                   int amount, int turn, String reason) {
        if (vehicle == null || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            return Result.blocked(0, "No vehicle fuel or power ledger is available.");
        }
        ensureInitialized(world, vehicle);
        int required = Math.max(0, amount);
        Snapshot before = inspect(world, vehicle);
        if (required <= 0) {
            return new Result(true, false, before.current(), before.current(), 0,
                    "No fuel or power consumption was required.");
        }
        if (before.current() < required) {
            return Result.blocked(before.current(),
                    "The vehicle requires " + required + " fuel or power units but only "
                            + before.current() + " remain.");
        }
        int after = before.current() - required;
        set(vehicle, "fuelOrPowerCurrent", Integer.toString(after));
        set(vehicle, "strategicTransitFuelReserved", "0");
        append(vehicle, "fuelOrPowerHistory", "Consumed " + required
                + " unit(s) at turn " + Math.max(0, turn) + " / "
                + clean(reason, "committed vehicle operation")
                + " / remaining " + after + "/" + before.capacity());
        return new Result(true, true, before.current(), after, required,
                "VEHICLE ENERGY: consumed " + required + " unit(s); "
                        + after + "/" + before.capacity() + " remain.");
    }

    static Result refuel(GamePanel game, MapObjectState vehicle, int amount,
                         String source) {
        if (game == null || game.world == null || vehicle == null
                || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            return Result.blocked(0,
                    "Refueling requires a physical vehicle in the loaded zone.");
        }
        VehicleAccessAuthority.Decision access = VehicleAccessAuthority.evaluate(
                game, vehicle, VehicleAccessAuthority.Permission.REFUEL);
        if (!access.allowed()) {
            Snapshot snapshot = inspect(game.world, vehicle);
            return Result.blocked(snapshot.current(), access.summary());
        }
        ensureInitialized(game.world, vehicle);
        Snapshot before = inspect(game.world, vehicle);
        int requested = Math.max(0, amount);
        int accepted = Math.min(requested, before.capacity() - before.current());
        if (accepted <= 0) {
            return new Result(true, false, before.current(), before.current(), 0,
                    "The vehicle fuel or power ledger is already full.");
        }
        int after = before.current() + accepted;
        set(vehicle, "fuelOrPowerCurrent", Integer.toString(after));
        append(vehicle, "fuelOrPowerHistory", "Added " + accepted
                + " unit(s) at turn " + Math.max(0, game.turn) + " / "
                + clean(source, "authorized refueling") + " / now "
                + after + "/" + before.capacity());
        return new Result(true, true, before.current(), after, accepted,
                "VEHICLE ENERGY: added " + accepted + " unit(s); "
                        + after + "/" + before.capacity() + " available.");
    }

    static List<String> inspectionLines(World world, MapObjectState vehicle) {
        Snapshot snapshot = inspect(world, vehicle);
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Fuel or power: " + snapshot.current() + "/"
                + snapshot.capacity() + " " + snapshot.energyType()
                + " unit(s); state " + snapshot.state() + ".");
        if (snapshot.reserved() > 0) {
            lines.add("Strategic reservation: " + snapshot.reserved()
                    + " unit(s) reserved; " + snapshot.available()
                    + " remain available for other committed operations.");
        } else {
            lines.add("Strategic reservation: no fuel or power units are reserved.");
        }
        return List.copyOf(lines);
    }

    static int capacity(VehicleRuntimeAuthority.VehicleClass definition) {
        return switch (definition) {
            case UTILITY_BIKE -> 12;
            case CIVILIAN_CAR -> 24;
            case CARGO_TRUCK -> 32;
            case ARMORED_CAR -> 36;
            case TANK -> 48;
        };
    }

    private static void clampLedger(MapObjectState vehicle) {
        int capacity = Math.max(1, intValue(value(vehicle,
                "fuelOrPowerCapacity"), 1));
        int current = Math.max(0, Math.min(capacity,
                intValue(value(vehicle, "fuelOrPowerCurrent"), capacity)));
        int reserved = Math.max(0, Math.min(current,
                intValue(value(vehicle, "strategicTransitFuelReserved"), 0)));
        set(vehicle, "fuelOrPowerCapacity", Integer.toString(capacity));
        set(vehicle, "fuelOrPowerCurrent", Integer.toString(current));
        set(vehicle, "strategicTransitFuelReserved", Integer.toString(reserved));
    }

    private static int intValue(String value, int fallback) {
        try { return Integer.parseInt(value); }
        catch (Exception ignored) { return fallback; }
    }

    private static void append(MapObjectState vehicle, String key,
                               String entry) {
        String existing = value(vehicle, key);
        String next = existing.isBlank() ? clean(entry, "")
                : existing + "~" + clean(entry, "");
        set(vehicle, key, next);
    }

    private static String value(MapObjectState vehicle, String key) {
        return vehicle == null ? ""
                : MapObjectState.stockValue(vehicle.stockState, key);
    }

    private static void set(MapObjectState vehicle, String key, String value) {
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                key, clean(value, "").replace(';', ',').replace('|', '/'));
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? ""
                : value.trim().replaceAll("\\s+", " ");
        return cleaned.isBlank() ? (fallback == null ? "" : fallback)
                : cleaned;
    }
}
