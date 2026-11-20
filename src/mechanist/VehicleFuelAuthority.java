package mechanist;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent fuel or power ledger attached to the authoritative vehicle fixture.
 * Reservations never consume this ledger; only a committed operation may deduct
 * units. Refueling is permission-gated and atomic.
 */
final class VehicleFuelAuthority {
    static final int GARAGE_REFUEL_BATCH_UNITS = 8;
    static final int GARAGE_REFUEL_SCRIPT_PER_UNIT = 1;

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
        int reserved = Math.max(0, Math.min(capacity,
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
        return consumeEnergy(world, vehicle, amount, turn, reason, false);
    }

    /** Only an authoritative committing transfer may spend reserved energy. */
    static Result consumeStrategicReservation(World world, MapObjectState vehicle,
                                              int amount, int turn, String reason) {
        return consumeEnergy(world, vehicle, amount, turn, reason, true);
    }

    private static Result consumeEnergy(World world, MapObjectState vehicle,
                                        int amount, int turn, String reason,
                                        boolean strategicReservation) {
        if (vehicle == null || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            return Result.blocked(0, "No vehicle fuel or power ledger is available.");
        }
        ensureInitialized(world, vehicle);
        int required = Math.max(0, amount);
        Snapshot before = inspect(world, vehicle);
        if (required <= 0) {
            return strategicReservation
                    ? Result.blocked(before.current(),
                    "Strategic transit requires a positive reserved fuel or power amount.")
                    : new Result(true, false, before.current(), before.current(), 0,
                    "No fuel or power consumption was required.");
        }
        if (strategicReservation
                && (!"committing".equals(value(vehicle, "strategicTransitState"))
                || before.reserved() != required
                || value(vehicle, "strategicTransitReservationId").isBlank()
                || value(vehicle, "strategicTransitReservationId").equals(
                        value(vehicle, "strategicTransitFuelLastCommitId")))) {
            return Result.blocked(before.current(),
                    "Strategic transit may consume only its exact active reservation during commit.");
        }
        if (before.current() < required) {
            return Result.blocked(before.current(),
                    "The vehicle requires " + required + " fuel or power units but only "
                            + before.current() + " remain.");
        }
        if (!strategicReservation && before.available() < required) {
            return Result.blocked(before.current(),
                    "The vehicle has " + before.reserved()
                            + " fuel or power units reserved for strategic transit; only "
                            + before.available() + " remain for other operations.");
        }
        int after = before.current() - required;
        String updatedStock = MapObjectState.setStockFlag(vehicle.stockState,
                "fuelOrPowerCurrent", Integer.toString(after));
        if (strategicReservation) {
            updatedStock = MapObjectState.setStockFlag(updatedStock,
                    "strategicTransitFuelReserved", "0");
            updatedStock = MapObjectState.setStockFlag(updatedStock,
                    "strategicTransitFuelLastCommitId",
                    value(vehicle, "strategicTransitReservationId"));
        }
        // One stock-state assignment records debit, reservation release and receipt.
        vehicle.stockState = updatedStock;
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
        return addAuthorized(game.world, vehicle, amount, game.turn,
                clean(source, "authorized refueling"));
    }

    /**
     * Player-facing garage fueling transaction. One visit repairs or refuels,
     * never both. Payment is checked before the existing fuel ledger changes.
     * No portable instant-fuel generator is granted by this interface.
     */
    static Result refuelAtGarage(GamePanel game, MapObjectState garage,
                                 MapObjectState vehicle) {
        if (game == null || game.world == null
                || !VehicleMaintenanceAuthority.stagedForGarage(
                        game.world, garage, vehicle)
                || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            return Result.blocked(0,
                    "Refueling requires a live player vehicle staged within "
                            + VehicleMaintenanceAuthority.GARAGE_STAGING_RADIUS_TILES
                            + " tiles of a service garage; no payment was taken.");
        }
        if (!VehicleRuntimeAuthority.playerOwns(game, vehicle)) {
            return Result.blocked(0,
                    "Refueling requires title to the staged vehicle; no payment was taken.");
        }
        VehicleRuntimeAuthority.Snapshot runtime =
                VehicleRuntimeAuthority.inspect(game.world, vehicle);
        if (runtime == null || "salvaged".equals(runtime.condition())
                || "wreck".equals(runtime.condition())
                || "dismantled".equals(runtime.operationState())) {
            return Result.blocked(0,
                    "A stripped or wrecked vehicle cannot receive ordinary garage fuel.");
        }
        Snapshot before = inspect(game.world, vehicle);
        int units = Math.min(GARAGE_REFUEL_BATCH_UNITS,
                Math.max(0, before.capacity() - before.current()));
        if (units == 0) {
            return new Result(true, false, before.current(), before.current(), 0,
                    "SERVICE GARAGE: fuel or power already full at "
                            + before.current() + "/" + before.capacity()
                            + "; no script or time spent.");
        }
        int price = units * GARAGE_REFUEL_SCRIPT_PER_UNIT;
        if (game.carriedScript < price) {
            return Result.blocked(before.current(),
                    "SERVICE GARAGE: " + units + " fuel or power unit(s) cost "
                            + price + " script; only " + game.carriedScript
                            + " carried. No fuel, script, or time was spent.");
        }
        // No world changes occur before the transaction succeeds. Since the
        // requested units are bounded to the remaining capacity, the existing
        // authorized ledger update cannot refuse an otherwise valid refill.
        Result filled = addAuthorized(game.world, vehicle, units, game.turn,
                "paid service garage refill");
        if (!filled.success() || !filled.changed()) return filled;
        if (!game.spendImperialScript(price)) {
            throw new IllegalStateException(
                    "Validated garage fuel payment exceeded carried script.");
        }
        return new Result(true, true, filled.before(), filled.after(),
                filled.amount(), "SERVICE GARAGE: added " + filled.amount()
                + " fuel or power unit(s) for " + price + " script; "
                + filled.after() + "/" + before.capacity() + " now available.");
    }

    static Result refuelForFaction(World world, MapObjectState vehicle, int amount,
                                   int turn, Faction faction, String source) {
        if (world == null || vehicle == null
                || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            return Result.blocked(0,
                    "Faction refueling requires a physical vehicle in the loaded zone.");
        }
        ensureInitialized(world, vehicle);
        Snapshot before = inspect(world, vehicle);
        if (faction == null || faction == Faction.NONE
                || !VehicleRuntimeAuthority.factionOwns(vehicle, faction)) {
            return Result.blocked(before.current(),
                    "Faction refueling requires same-family motor-pool ownership.");
        }
        VehicleRuntimeAuthority.Snapshot runtime =
                VehicleRuntimeAuthority.inspect(world, vehicle);
        if (runtime == null || "salvaged".equals(runtime.condition())
                || "dismantled".equals(runtime.operationState())) {
            return Result.blocked(before.current(),
                    "A stripped vehicle hulk cannot receive fuel or power support.");
        }
        return addAuthorized(world, vehicle, amount, turn,
                faction.label + " / " + clean(source, "faction refueling"));
    }

    static List<String> inspectionLines(World world, MapObjectState vehicle) {
        Snapshot snapshot = inspect(world, vehicle);
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Fuel or power: " + snapshot.current() + "/"
                + snapshot.capacity() + " " + snapshot.energyType()
                + " unit(s); state " + snapshot.state() + ".");
        lines.add("Garage refill: park your vehicle within "
                + VehicleMaintenanceAuthority.GARAGE_STAGING_RADIUS_TILES
                + " tiles of a maintenance garage. After repairs, one visit adds up to "
                + GARAGE_REFUEL_BATCH_UNITS + " fuel or power units at "
                + GARAGE_REFUEL_SCRIPT_PER_UNIT + " script each.");
        if (snapshot.reserved() > 0) {
            lines.add("Strategic reservation: " + snapshot.reserved()
                    + " unit(s) reserved; " + snapshot.available()
                    + " remain available for other committed operations."
                    + (snapshot.current() < snapshot.reserved()
                    ? " The reservation is stale because current energy is below the committed amount."
                    : ""));
        } else {
            lines.add("Strategic reservation: no fuel or power units are reserved.");
        }
        return List.copyOf(lines);
    }

    /**
     * Shared terrain-independent travel energy scale for both local road paths
     * and strategic journeys. A zero-step local inspection costs nothing.
     * This function reads no mutable ledgers and never consumes fuel.
     */
    static int routeEnergyCost(VehicleRuntimeAuthority.VehicleClass definition,
                               int routeSteps) {
        if (routeSteps <= 0) return 0;
        VehicleRuntimeAuthority.VehicleClass vehicleClass = definition == null
                ? VehicleRuntimeAuthority.VehicleClass.CIVILIAN_CAR : definition;
        int tilesPerUnit = switch (vehicleClass) {
            case UTILITY_BIKE -> 8;
            case CIVILIAN_CAR -> 6;
            case CARGO_TRUCK -> 4;
            case ARMORED_CAR -> 3;
            case TANK -> 2;
        };
        return 1 + (routeSteps - 1) / tilesPerUnit;
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

    private static Result addAuthorized(World world, MapObjectState vehicle,
                                        int amount, int turn, String source) {
        ensureInitialized(world, vehicle);
        Snapshot before = inspect(world, vehicle);
        int requested = Math.max(0, amount);
        int accepted = Math.min(requested,
                before.capacity() - before.current());
        if (accepted <= 0) {
            return new Result(true, false, before.current(), before.current(), 0,
                    "The vehicle fuel or power ledger is already full.");
        }
        int after = before.current() + accepted;
        set(vehicle, "fuelOrPowerCurrent", Integer.toString(after));
        append(vehicle, "fuelOrPowerHistory", "Added " + accepted
                + " unit(s) at turn " + Math.max(0, turn) + " / "
                + clean(source, "authorized refueling") + " / now "
                + after + "/" + before.capacity());
        return new Result(true, true, before.current(), after, accepted,
                "VEHICLE ENERGY: added " + accepted + " unit(s); "
                        + after + "/" + before.capacity() + " available.");
    }

    private static void clampLedger(MapObjectState vehicle) {
        int capacity = Math.max(1, intValue(value(vehicle,
                "fuelOrPowerCapacity"), 1));
        int current = Math.max(0, Math.min(capacity,
                intValue(value(vehicle, "fuelOrPowerCurrent"), capacity)));
        int reserved = Math.max(0, Math.min(capacity,
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
        ArrayList<String> entries = new ArrayList<>();
        String existing = value(vehicle, key);
        if (!existing.isBlank()) {
            for (String token : existing.split("~")) {
                String cleaned = clean(token, "");
                if (!cleaned.isBlank()) entries.add(cleaned);
            }
        }
        String cleanedEntry = clean(entry, "");
        if (!cleanedEntry.isBlank()) entries.add(cleanedEntry);
        while (entries.size() > 12) entries.remove(0);
        set(vehicle, key, String.join("~", entries));
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
