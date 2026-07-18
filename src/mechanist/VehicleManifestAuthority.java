package mechanist;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Persistent driver, operational crew, passenger, and cargo custody manifests. */
final class VehicleManifestAuthority {
    record CargoEntry(String label, String owner, int units) { }

    record Snapshot(String driver, List<String> crew, List<String> passengers,
                    List<CargoEntry> cargo, int assignedCrew,
                    int occupiedSeats, int seatCapacity,
                    int cargoUnits, int cargoCapacity) { }

    record Result(boolean success, boolean changed, String action,
                  String message, Snapshot snapshot) {
        static Result blocked(String action, String message, Snapshot snapshot) {
            return new Result(false, false, clean(action, "manifest action"),
                    clean(message, "Vehicle manifest action was blocked."), snapshot);
        }
    }

    private VehicleManifestAuthority() { }

    static Snapshot inspect(World world, MapObjectState vehicle) {
        if (vehicle == null || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            return new Snapshot("", List.of(), List.of(), List.of(),
                    0, 0, 0, 0, 0);
        }
        VehicleRuntimeAuthority.ensureInitialized(world, vehicle);
        VehicleRuntimeAuthority.VehicleClass definition =
                VehicleRuntimeAuthority.vehicleClass(vehicle.type);
        String driver = value(vehicle, "vehicleDriver");
        List<String> crew = split(value(vehicle, "vehicleCrewManifest"));
        List<String> passengers = split(value(vehicle,
                "vehiclePassengerManifest"));
        List<CargoEntry> cargo = cargoEntries(value(vehicle,
                "vehicleCargoManifest"));
        int cargoUnits = 0;
        for (CargoEntry entry : cargo) cargoUnits += entry.units();
        int assignedCrew = (driver.isBlank() ? 0 : 1) + crew.size();
        int occupiedSeats = (driver.isBlank() ? 0 : 1) + passengers.size();
        return new Snapshot(driver, crew, passengers, cargo, assignedCrew,
                occupiedSeats, definition.seats, cargoUnits,
                definition.cargoCapacity);
    }

    static Result assignDriver(GamePanel game, MapObjectState vehicle,
                               String driverName) {
        Snapshot before = inspect(game == null ? null : game.world, vehicle);
        VehicleAccessAuthority.Decision access = VehicleAccessAuthority.evaluate(
                game, vehicle, VehicleAccessAuthority.Permission.OPERATION);
        if (!access.allowed()) {
            return Result.blocked("assign driver", access.summary(), before);
        }
        String name = clean(driverName, "");
        if (name.isBlank()) {
            return Result.blocked("assign driver",
                    "A readable driver identity is required.", before);
        }
        if (name.equalsIgnoreCase(before.driver())) {
            return new Result(true, false, "assign driver",
                    "The same driver is already assigned.", before);
        }
        if (before.driver().isBlank() && before.occupiedSeats() >= before.seatCapacity()) {
            return Result.blocked("assign driver",
                    "No driver seat is available.", before);
        }
        set(vehicle, "vehicleDriver", name);
        append(vehicle, "crewHistory", "Driver assigned: " + name
                + turn(game));
        return changed("assign driver", "VEHICLE MANIFEST: driver "
                + name + " assigned.", game, vehicle);
    }

    static Result addCrew(GamePanel game, MapObjectState vehicle,
                          String crewName) {
        Snapshot before = inspect(game == null ? null : game.world, vehicle);
        VehicleAccessAuthority.Decision access = VehicleAccessAuthority.evaluate(
                game, vehicle, VehicleAccessAuthority.Permission.DEPLOYMENT);
        if (!access.allowed()) {
            return Result.blocked("assign crew", access.summary(), before);
        }
        String name = clean(crewName, "");
        if (name.isBlank()) {
            return Result.blocked("assign crew",
                    "A readable crew identity is required.", before);
        }
        if (containsIgnoreCase(before.crew(), name)
                || name.equalsIgnoreCase(before.driver())) {
            return new Result(true, false, "assign crew",
                    "That person is already assigned to the vehicle.", before);
        }
        ArrayList<String> crew = new ArrayList<>(before.crew());
        crew.add(name);
        set(vehicle, "vehicleCrewManifest", String.join("~", crew));
        append(vehicle, "crewHistory", "Crew assigned: " + name
                + turn(game));
        return changed("assign crew", "VEHICLE MANIFEST: crew member "
                + name + " assigned.", game, vehicle);
    }

    static Result boardPassenger(GamePanel game, MapObjectState vehicle,
                                 String passengerName) {
        Snapshot before = inspect(game == null ? null : game.world, vehicle);
        VehicleAccessAuthority.Decision access = VehicleAccessAuthority.evaluate(
                game, vehicle, VehicleAccessAuthority.Permission.PASSENGER);
        if (!access.allowed()) {
            return Result.blocked("board passenger", access.summary(), before);
        }
        String name = clean(passengerName, "");
        if (name.isBlank()) {
            return Result.blocked("board passenger",
                    "A readable passenger identity is required.", before);
        }
        if (containsIgnoreCase(before.passengers(), name)) {
            return new Result(true, false, "board passenger",
                    "That passenger is already aboard.", before);
        }
        if (before.occupiedSeats() >= before.seatCapacity()) {
            return Result.blocked("board passenger",
                    "All passenger seats are occupied.", before);
        }
        ArrayList<String> passengers = new ArrayList<>(before.passengers());
        passengers.add(name);
        set(vehicle, "vehiclePassengerManifest", String.join("~", passengers));
        append(vehicle, "passengerHistory", "Passenger boarded: " + name
                + turn(game));
        return changed("board passenger", "VEHICLE MANIFEST: passenger "
                + name + " boarded.", game, vehicle);
    }

    static Result registerCargo(GamePanel game, MapObjectState vehicle,
                                String label, String owner, int units) {
        Snapshot before = inspect(game == null ? null : game.world, vehicle);
        VehicleAccessAuthority.Decision access = VehicleAccessAuthority.evaluate(
                game, vehicle, VehicleAccessAuthority.Permission.CARGO);
        if (!access.allowed()) {
            return Result.blocked("register cargo", access.summary(), before);
        }
        String cargoLabel = clean(label, "");
        String cargoOwner = clean(owner, "unrecorded owner");
        int amount = Math.max(0, units);
        if (cargoLabel.isBlank() || amount <= 0) {
            return Result.blocked("register cargo",
                    "Cargo requires a readable label and positive unit count.", before);
        }
        if (before.cargoUnits() + amount > before.cargoCapacity()) {
            return Result.blocked("register cargo",
                    "Cargo capacity " + before.cargoCapacity()
                            + " would be exceeded by " + amount + " unit(s).",
                    before);
        }
        ArrayList<CargoEntry> cargo = new ArrayList<>(before.cargo());
        cargo.add(new CargoEntry(cargoLabel, cargoOwner, amount));
        set(vehicle, "vehicleCargoManifest", cargoText(cargo));
        append(vehicle, "cargoHistory", "Cargo registered: " + cargoLabel
                + " / owner " + cargoOwner + " / units " + amount
                + turn(game));
        return changed("register cargo", "VEHICLE MANIFEST: registered "
                + amount + " unit(s) of " + cargoLabel + " for "
                + cargoOwner + ".", game, vehicle);
    }

    static Result clearManifest(GamePanel game, MapObjectState vehicle,
                                String reason) {
        Snapshot before = inspect(game == null ? null : game.world, vehicle);
        VehicleAccessAuthority.Decision access = VehicleAccessAuthority.evaluate(
                game, vehicle, VehicleAccessAuthority.Permission.DEPLOYMENT);
        if (!access.allowed()) {
            return Result.blocked("clear manifest", access.summary(), before);
        }
        boolean changed = !before.driver().isBlank() || !before.crew().isEmpty()
                || !before.passengers().isEmpty() || !before.cargo().isEmpty();
        set(vehicle, "vehicleDriver", "");
        set(vehicle, "vehicleCrewManifest", "");
        set(vehicle, "vehiclePassengerManifest", "");
        set(vehicle, "vehicleCargoManifest", "");
        append(vehicle, "crewHistory", "Manifest cleared / "
                + clean(reason, "vehicle stood down") + turn(game));
        return new Result(true, changed, "clear manifest",
                changed ? "VEHICLE MANIFEST: driver, crew, passenger, and cargo assignments were cleared."
                        : "The vehicle manifest was already empty.",
                inspect(game == null ? null : game.world, vehicle));
    }

    static VehicleStrategicTransitAuthority.Request strategicRequest(
            GamePanel game, MapObjectState vehicle, String destinationKey,
            Set<VehicleStrategicTransitAuthority.Infrastructure> infrastructure,
            int routeDistance, int destinationParkingCapacity,
            int fuelRequired, boolean gateOpen, boolean checkpointOpen,
            boolean securityClosure, Faction routeController,
            String routeReason) {
        Snapshot manifest = inspect(game == null ? null : game.world, vehicle);
        VehicleFuelAuthority.Snapshot fuel = VehicleFuelAuthority.inspect(
                game == null ? null : game.world, vehicle);
        return new VehicleStrategicTransitAuthority.Request(destinationKey,
                infrastructure == null ? Set.of() : infrastructure,
                routeDistance, destinationParkingCapacity,
                manifest.assignedCrew(), !manifest.driver().isBlank(),
                fuel.current(), fuelRequired, gateOpen, checkpointOpen,
                securityClosure, routeController, routeReason);
    }

    static List<String> inspectionLines(World world, MapObjectState vehicle) {
        Snapshot snapshot = inspect(world, vehicle);
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Driver: " + clean(snapshot.driver(), "unassigned")
                + "; operational crew " + snapshot.assignedCrew() + "/"
                + VehicleRuntimeAuthority.vehicleClass(vehicle.type).crewRequired + ".");
        lines.add("Passenger seating: " + snapshot.occupiedSeats() + "/"
                + snapshot.seatCapacity() + " occupied; passengers "
                + (snapshot.passengers().isEmpty() ? "none"
                : String.join(", ", snapshot.passengers())) + ".");
        lines.add("Cargo custody: " + snapshot.cargoUnits() + "/"
                + snapshot.cargoCapacity() + " unit(s) registered.");
        for (CargoEntry cargo : snapshot.cargo()) {
            lines.add("Cargo: " + cargo.label() + " / owner "
                    + cargo.owner() + " / " + cargo.units() + " unit(s).");
        }
        return List.copyOf(lines);
    }

    private static Result changed(String action, String message,
                                  GamePanel game, MapObjectState vehicle) {
        return new Result(true, true, action, message,
                inspect(game == null ? null : game.world, vehicle));
    }

    private static List<CargoEntry> cargoEntries(String text) {
        ArrayList<CargoEntry> entries = new ArrayList<>();
        for (String token : split(text)) {
            String[] fields = token.split("\\^", -1);
            if (fields.length < 3) continue;
            int units;
            try { units = Math.max(0, Integer.parseInt(fields[2])); }
            catch (Exception ignored) { continue; }
            if (units <= 0) continue;
            entries.add(new CargoEntry(clean(fields[0], "cargo"),
                    clean(fields[1], "unrecorded owner"), units));
        }
        return List.copyOf(entries);
    }

    private static String cargoText(List<CargoEntry> entries) {
        ArrayList<String> tokens = new ArrayList<>();
        for (CargoEntry entry : entries) {
            tokens.add(safeToken(entry.label()) + "^" + safeToken(entry.owner())
                    + "^" + Math.max(0, entry.units()));
        }
        return String.join("~", tokens);
    }

    private static String safeToken(String value) {
        return clean(value, "").replace('~', '-').replace('^', '-');
    }

    private static List<String> split(String text) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (text != null) for (String token : text.split("~")) {
            String cleaned = clean(token, "");
            if (!cleaned.isBlank()) values.add(cleaned);
        }
        return List.copyOf(values);
    }

    private static boolean containsIgnoreCase(List<String> values,
                                              String expected) {
        for (String value : values) {
            if (value.equalsIgnoreCase(expected)) return true;
        }
        return false;
    }

    private static String turn(GamePanel game) {
        return " / turn " + (game == null ? 0 : Math.max(0, game.turn));
    }

    private static void append(MapObjectState vehicle, String key,
                               String entry) {
        String existing = value(vehicle, key);
        set(vehicle, key, existing.isBlank() ? clean(entry, "")
                : existing + "~" + clean(entry, ""));
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
