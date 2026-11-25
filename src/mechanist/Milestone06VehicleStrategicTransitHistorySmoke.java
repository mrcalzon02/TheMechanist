package mechanist;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

/** Focused smoke for bounded persistent strategic vehicle transit history. */
final class Milestone06VehicleStrategicTransitHistorySmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            game.worldTurn = 1_500L;
            game.inventory.clear();

            MapObjectState car = vehicle(game.world, 4, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    181L);
            game.world.mapObjects.add(car);

            int scriptBefore = game.carriedScript;
            int suppliesBefore = game.supplies;
            int partsBefore = game.machineParts;
            int startX = car.x;
            int startY = car.y;

            for (int cycle = 0; cycle < 19; cycle++) {
                game.worldTurn++;
                VehicleStrategicTransitAuthority.Request request =
                        request(cycle);
                VehicleStrategicTransitAuthority.Reservation reserved =
                        VehicleStrategicTransitAuthority.reserve(game, car,
                                request);
                require(reserved.success() && reserved.changed()
                                && reserved.readiness().status()
                                == VehicleStrategicTransitAuthority.Status.RESERVED,
                        "cycle " + cycle + " should create a strategic reservation");

                game.worldTurn++;
                VehicleStrategicTransitAuthority.Reservation cancelled =
                        VehicleStrategicTransitAuthority.cancel(game, car,
                                "cycle cancel " + cycle);
                require(cancelled.success() && cancelled.changed()
                                && "cancelled".equals(MapObjectState.stockValue(
                                car.stockState, "strategicTransitState"))
                                && "0".equals(MapObjectState.stockValue(
                                car.stockState, "strategicTransitFuelReserved")),
                        "cycle " + cycle + " should cancel without consuming fuel");
            }

            game.worldTurn++;
            VehicleStrategicTransitAuthority.Reservation finalReservation =
                    VehicleStrategicTransitAuthority.reserve(game, car,
                            request(19));
            require(finalReservation.success() && finalReservation.changed()
                            && "reserved".equals(MapObjectState.stockValue(
                            car.stockState, "strategicTransitState"))
                            && "sector-2/zone-19/floor-0".equals(
                            MapObjectState.stockValue(car.stockState,
                                    "strategicTransitDestination"))
                            && "3".equals(MapObjectState.stockValue(
                            car.stockState, "strategicTransitFuelReserved")),
                    "final cycle should leave one exact active reservation");

            List<String> history = tokens(MapObjectState.stockValue(
                    car.stockState, "deploymentHistory"));
            require(history.size() == 12,
                    "strategic transit history should retain exactly twelve entries: "
                            + history);
            require(history.get(0).contains("cycle cancel 13"),
                    "oldest retained strategic transit entry should be cycle 13 cancellation: "
                            + history.get(0));
            require(history.get(history.size() - 1).contains(
                            "sector-2/zone-19/floor-0"),
                    "newest strategic transit entry should be the final reservation: "
                            + history.get(history.size() - 1));
            require(history.stream().noneMatch(line -> line.contains(
                            "cycle cancel 12")
                            || line.contains("sector-2/zone-13/floor-0")),
                    "retired strategic transit history must not survive the retention boundary");

            require(game.carriedScript == scriptBefore
                            && game.supplies == suppliesBefore
                            && game.machineParts == partsBefore
                            && car.x == startX && car.y == startY
                            && game.world.mapObjects.contains(car),
                    "reservation history stress must not consume resources or move the physical vehicle");

            MapObjectState restored = copy(car);
            require("reserved".equals(MapObjectState.stockValue(
                            restored.stockState, "strategicTransitState"))
                            && "sector-2/zone-19/floor-0".equals(
                            MapObjectState.stockValue(restored.stockState,
                                    "strategicTransitDestination"))
                            && tokens(MapObjectState.stockValue(restored.stockState,
                            "deploymentHistory")).equals(history),
                    "bounded reservation state and history should reconstruct from persisted stock state");

            verifyDiskSaveReload(game, car, finalReservation.reservationId(), history);
            verifySavedEmptyAndReplacedFixtures();
            System.out.println("Milestone 06 strategic vehicle transit history and disk restore smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    /**
     * Exercises the production world-state encoder/decoder through an actual
     * Properties file, rather than shallow-copying a vehicle's stock string.
     * Physical slices stay intentionally tiny to avoid a 600x600 worldgen.
     */
    private static void verifyDiskSaveReload(GamePanel game,
                                             MapObjectState original,
                                             String expectedReservationId,
                                             List<String> priorHistory) {
        Path file = null;
        World initialWorld = game.world;
        int initialTurn = game.turn;
        long initialWorldTurn = game.worldTurn;
        int initialFuel = VehicleFuelAuthority.preview(original).current();
        int initialX = original.x;
        int initialY = original.y;
        try {
            file = Files.createTempFile("mechanist-strategic-save-reload-", ".properties");

            // One actual disk save/load must reconstruct a DIFFERENT physical
            // fixture with the same reservation, title, fuel and bounded history.
            World loaded = saveReloadWorld(game.world, file);
            require(loaded != game.world && loaded.mapObjects.size() == 1,
                    "disk restore must reconstruct one new vehicle world");
            MapObjectState booked = loaded.mapObjects.get(0);
            require(booked != original && booked.id.equals(original.id)
                            && booked.stockState.equals(original.stockState)
                            && booked.x == initialX && booked.y == initialY
                            && tokens(MapObjectState.stockValue(booked.stockState,
                                    "deploymentHistory")).equals(priorHistory)
                            && VehicleFuelAuthority.preview(booked).current() == initialFuel
                            && VehicleFuelAuthority.preview(booked).reserved() == 3
                            && expectedReservationId.equals(MapObjectState.stockValue(
                                    booked.stockState, "strategicTransitReservationId")),
                    "disk restore must preserve exact pending reservation, history and fuel");
            game.world = loaded;
            String reservedStock = booked.stockState;
            require(VehicleStrategicTransitAuthority.playerCancellationEligibility(
                            game, booked).allowed()
                            && reservedStock.equals(booked.stockState),
                    "restored title must remain cancellable without a read-side mutation");
            VehicleStrategicTransitAuthority.Reservation repeat =
                    VehicleStrategicTransitAuthority.reserve(game, booked, request(19));
            require(repeat.success() && !repeat.changed()
                            && repeat.reservationId().equals(expectedReservationId)
                            && reservedStock.equals(booked.stockState),
                    "reload must not cause a duplicate reservation or history entry");

            booked.stockState = MapObjectState.setStockFlag(
                    booked.stockState, "ownerPlayerId", "another-character-id");
            String unrelatedTitle = booked.stockState;
            require(!VehicleStrategicTransitAuthority.cancel(
                            game, booked, "wrong owner after reload").success()
                            && unrelatedTitle.equals(booked.stockState),
                    "reloaded reservation must reject an unrelated character title");
            booked.stockState = reservedStock;

            // Cancel after reload, save the result, and verify the hold cannot
            // come back from disk or consume fuel on a second cancellation.
            VehicleStrategicTransitAuthority.Reservation cancelled =
                    VehicleStrategicTransitAuthority.cancel(
                            game, booked, "owner cancelled after disk restore");
            require(cancelled.success() && cancelled.changed()
                            && VehicleFuelAuthority.preview(booked).current() == initialFuel
                            && VehicleFuelAuthority.preview(booked).reserved() == 0,
                    "restored cancellation must release the hold, not spend fuel");
            World cancelledWorld = saveReloadWorld(game.world, file);
            game.world = cancelledWorld;
            MapObjectState cancelledVehicle = cancelledWorld.mapObjects.get(0);
            String cancelledStock = cancelledVehicle.stockState;
            require("cancelled".equals(MapObjectState.stockValue(
                            cancelledStock, "strategicTransitState"))
                            && "0".equals(MapObjectState.stockValue(
                                    cancelledStock, "strategicTransitFuelReserved"))
                            && VehicleFuelAuthority.preview(cancelledVehicle).current() == initialFuel
                            && !VehicleStrategicTransitAuthority.cancel(
                                    game, cancelledVehicle,
                                    "duplicate cancellation after reload").success()
                            && cancelledStock.equals(cancelledVehicle.stockState),
                    "disk reload must not resurrect cancelled holds or duplicate cancellation");

            // Rebook through the ordinary route authority. Persist a damaged
            // legacy origin and a missing hold, reload, and prove the rightful
            // owner can recover it without moving or draining the car.
            game.worldTurn++;
            VehicleStrategicTransitAuthority.Reservation rebooked =
                    VehicleStrategicTransitAuthority.reserve(
                            game, cancelledVehicle, request(19));
            require(rebooked.success() && rebooked.changed(),
                    "restored cancelled booking must support a normal new reservation");
            cancelledVehicle.stockState = MapObjectState.setStockFlag(
                    cancelledVehicle.stockState, "strategicTransitOrigin",
                    "outdated-world-location");
            cancelledVehicle.stockState = MapObjectState.setStockFlag(
                    cancelledVehicle.stockState, "strategicTransitFuelReserved", "0");
            World staleWorld = saveReloadWorld(game.world, file);
            game.world = staleWorld;
            MapObjectState stale = staleWorld.mapObjects.get(0);
            String staleStock = stale.stockState;
            require(VehicleStrategicTransitAuthority.playerCancellationEligibility(
                            game, stale).allowed()
                            && staleStock.equals(stale.stockState)
                            && VehicleFuelAuthority.preview(stale).current() == initialFuel
                            && stale.x == initialX && stale.y == initialY,
                    "reloaded stale reservation must retain title, fuel and position");
            VehicleStrategicTransitAuthority.Reservation recovered =
                    VehicleStrategicTransitAuthority.cancel(
                            game, stale, "recover stale booking after disk reload");
            require(recovered.success() && recovered.changed()
                            && VehicleFuelAuthority.preview(stale).current() == initialFuel
                            && stale.x == initialX && stale.y == initialY,
                    "stale booking cancellation must not move the vehicle or debit fuel");

            World finalWorld = saveReloadWorld(game.world, file);
            MapObjectState finalVehicle = finalWorld.mapObjects.get(0);
            require("cancelled".equals(MapObjectState.stockValue(
                            finalVehicle.stockState, "strategicTransitState"))
                            && VehicleFuelAuthority.preview(finalVehicle).reserved() == 0
                            && VehicleFuelAuthority.preview(finalVehicle).current() == initialFuel
                            && finalVehicle.x == initialX && finalVehicle.y == initialY
                            && game.turn == initialTurn
                            && game.worldTurn == initialWorldTurn + 1,
                    "second restored cancellation must persist without ghost reservations or travel");
        } catch (IOException failure) {
            throw new AssertionError("strategic reservation disk round trip failed", failure);
        } finally {
            game.world = initialWorld;
            if (file != null) {
                try { Files.deleteIfExists(file); }
                catch (IOException failure) {
                    throw new AssertionError("temporary reservation save was not cleaned", failure);
                }
            }
        }
    }

    private static World saveReloadWorld(World source, Path file)
            throws IOException {
        Properties stored = new Properties();
        Persistence.writeWorldState(source, stored);
        try (OutputStream out = Files.newOutputStream(file)) {
            stored.store(out, "Milestone 06 strategic reservation continuity");
        }
        Properties restored = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            restored.load(in);
        }
        World result = world();
        Persistence.readWorldState(result, restored);
        return result;
    }

    /**
     * A generated slice may contain fixtures that were later cleared. The
     * production decoder must honor an explicitly empty saved list, restore
     * underlying road tiles, and only keep generated fixtures for legacy saves
     * that omitted the field entirely.
     */
    private static void verifySavedEmptyAndReplacedFixtures() {
        World cleared = world();
        Properties saved = new Properties();
        Persistence.writeWorldState(cleared, saved);
        require(saved.containsKey("world.mapObjects")
                        && saved.getProperty("world.mapObjects", "missing").isEmpty(),
                "the production world encoder must write an explicit empty fixture list");

        World emptyTarget = world();
        MapObjectState ghost = vehicle(emptyTarget, 7, 5,
                AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR, 191L);
        emptyTarget.mapObjects.add(ghost);
        emptyTarget.tiles[7][5] = ghost.glyph;
        Persistence.readWorldState(emptyTarget, saved);
        require(emptyTarget.mapObjects.isEmpty()
                        && emptyTarget.tiles[7][5] == '.',
                "loading a cleared saved zone must not respawn its generated car "
                        + "or retain a phantom vehicle tile");

        Properties legacy = new Properties();
        legacy.putAll(saved);
        legacy.remove("world.mapObjects");
        // This predates both fixture lists and exact terrain snapshots.
        legacy.remove(WorldTerrainSnapshotAuthority.KEY);
        World legacyTarget = world();
        MapObjectState legacyCar = vehicle(legacyTarget, 7, 5,
                AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR, 192L);
        legacyTarget.mapObjects.add(legacyCar);
        legacyTarget.tiles[7][5] = legacyCar.glyph;
        Persistence.readWorldState(legacyTarget, legacy);
        require(legacyTarget.mapObjects.size() == 1
                        && legacyTarget.mapObjects.get(0) == legacyCar
                        && legacyTarget.tiles[7][5] == legacyCar.glyph,
                "legacy saves without a fixture field must keep generated defaults");

        World oneVehicle = world();
        MapObjectState savedCar = vehicle(oneVehicle, 4, 5,
                AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR, 193L);
        oneVehicle.mapObjects.add(savedCar);
        oneVehicle.tiles[savedCar.x][savedCar.y] = savedCar.glyph;
        Properties replacement = new Properties();
        Persistence.writeWorldState(oneVehicle, replacement);
        World replacedTarget = world();
        MapObjectState previous = vehicle(replacedTarget, 7, 5,
                AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR, 194L);
        replacedTarget.mapObjects.add(previous);
        replacedTarget.tiles[7][5] = previous.glyph;
        Persistence.readWorldState(replacedTarget, replacement);
        require(replacedTarget.mapObjects.size() == 1
                        && savedCar.id.equals(replacedTarget.mapObjects.get(0).id)
                        && savedCar.stockState.equals(
                                replacedTarget.mapObjects.get(0).stockState)
                        && replacedTarget.tiles[4][5] == savedCar.glyph
                        && replacedTarget.tiles[7][5] == '.',
                "loading a saved car must replace generated fixtures and "
                        + "clear their stale glyphs");
    }

    private static VehicleStrategicTransitAuthority.Request request(int cycle) {
        return new VehicleStrategicTransitAuthority.Request(
                "sector-2/zone-" + cycle + "/floor-0",
                Set.of(VehicleStrategicTransitAuthority.Infrastructure.ROAD,
                        VehicleStrategicTransitAuthority.Infrastructure.CHECKPOINT,
                        VehicleStrategicTransitAuthority.Infrastructure.PARKING_LOT),
                18, 2, 3, true, 8, 3,
                true, true, false, Faction.NONE,
                "history stress route " + cycle);
    }

    private static List<String> tokens(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("~"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .toList();
    }

    private static World world() {
        World world = new World(61031L, 18, 12);
        world.zoneType = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        world.sectorX = 1;
        world.sectorY = 1;
        world.zoneX = 2;
        world.zoneY = 2;
        world.floor = 0;
        world.mapObjects.clear();
        world.npcs.clear();
        for (int x = 0; x < world.w; x++) {
            for (int y = 0; y < world.h; y++) world.tiles[x][y] = '.';
        }
        return world;
    }

    private static MapObjectState vehicle(World world, int x, int y,
                                          String type, long seed) {
        RoadTransitFixtureAuthority.VehicleProfile profile =
                new RoadTransitFixtureAuthority.VehicleProfile(type,
                        RoadTransitFixtureAuthority.labelForType(type,
                                world.zoneType, new Random(seed)),
                        RoadTransitFixtureAuthority.stockForType(type,
                                world.tiles[x][y]));
        MapObjectState vehicle = RoadTransitFixtureAuthority.newVehicleFixture(
                world, x, y, profile, new Random(seed),
                "VEHICLE-STRATEGIC-TRANSIT-HISTORY-SMOKE");
        VehicleRuntimeAuthority.initialize(world, vehicle, Faction.NONE,
                "player-owned", "strategic-transit-history-smoke", false,
                new Random(seed));
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerType", VehicleRuntimeAuthority.OwnerType.PLAYER.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerFaction", Faction.NONE.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerName", "Player");
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownership", "player-owned");
        return vehicle;
    }

    private static MapObjectState copy(MapObjectState source) {
        MapObjectState copy = new MapObjectState();
        copy.id = source.id;
        copy.type = source.type;
        copy.label = source.label;
        copy.glyph = source.glyph;
        copy.x = source.x;
        copy.y = source.y;
        copy.stockState = source.stockState;
        return copy;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone06VehicleStrategicTransitHistorySmoke() { }
}
