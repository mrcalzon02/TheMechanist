package mechanist;

import java.util.List;
import java.util.Random;

/** Focused smoke for distinct vehicle operation, passenger, cargo, repair, fuel, deployment, and seizure rights. */
final class Milestone06VehicleAccessSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            game.inventory.clear();

            MapObjectState playerCar = vehicle(game.world, 3, 4,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    VehicleRuntimeAuthority.OwnerType.PLAYER,
                    Faction.NONE, "player-owned", 101L);
            game.world.mapObjects.add(playerCar);
            for (VehicleAccessAuthority.Permission permission
                    : VehicleAccessAuthority.Permission.values()) {
                VehicleAccessAuthority.Decision decision =
                        VehicleAccessAuthority.evaluate(game, playerCar, permission);
                require(decision.allowed(),
                        "player-owned civilian vehicle should allow " + permission
                                + ": " + decision.summary());
            }

            MapObjectState restricted = vehicle(game.world, 5, 4,
                    AssetIntegrationDisciplineAuthority.PARKED_TANK,
                    VehicleRuntimeAuthority.OwnerType.PLAYER,
                    Faction.NONE, "player-owned", 102L);
            game.world.mapObjects.add(restricted);
            VehicleAccessAuthority.Decision restrictedDeploy =
                    VehicleAccessAuthority.evaluate(game, restricted,
                            VehicleAccessAuthority.Permission.DEPLOYMENT);
            require(!restrictedDeploy.allowed(),
                    "restricted player vehicle should require title or deployment authority");
            game.inventory.add("Vehicle title: "
                    + MapObjectState.stockValue(restricted.stockState, "manufacturer")
                    + " " + MapObjectState.stockValue(restricted.stockState, "model"));
            require(VehicleAccessAuthority.evaluate(game, restricted,
                            VehicleAccessAuthority.Permission.DEPLOYMENT).allowed(),
                    "matching carried vehicle title should authorize restricted deployment");

            VehicleRuntimeAuthority.applyDamage(playerCar,
                    VehicleRuntimeAuthority.Component.MOBILITY, 100,
                    10, "access smoke disabled mobility");
            require(!VehicleAccessAuthority.evaluate(game, playerCar,
                            VehicleAccessAuthority.Permission.OPERATION).allowed(),
                    "owner operation should still be blocked by disabled critical components");
            require(VehicleAccessAuthority.evaluate(game, playerCar,
                            VehicleAccessAuthority.Permission.REPAIR).allowed(),
                    "disabled owner vehicle should remain repair-accessible");

            MapObjectState publicBus = vehicle(game.world, 7, 4,
                    AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK,
                    VehicleRuntimeAuthority.OwnerType.PUBLIC,
                    Faction.CIVIC_LEDGER_OFFICE, "public", 103L);
            game.world.mapObjects.add(publicBus);
            require(!VehicleAccessAuthority.evaluate(game, publicBus,
                            VehicleAccessAuthority.Permission.PASSENGER).allowed(),
                    "public passenger access should require a transit credential");
            game.inventory.add("Public transit pass");
            require(VehicleAccessAuthority.evaluate(game, publicBus,
                            VehicleAccessAuthority.Permission.PASSENGER).allowed(),
                    "public transit pass should authorize boarding only");
            require(!VehicleAccessAuthority.evaluate(game, publicBus,
                            VehicleAccessAuthority.Permission.OPERATION).allowed(),
                    "transit pass must not silently authorize driving");
            game.inventory.add("Vehicle operator permit");
            require(VehicleAccessAuthority.evaluate(game, publicBus,
                            VehicleAccessAuthority.Permission.OPERATION).allowed(),
                    "vehicle operator permit should authorize public-service driving");
            require(!VehicleAccessAuthority.evaluate(game, publicBus,
                            VehicleAccessAuthority.Permission.CARGO).allowed(),
                    "public driver permission must remain distinct from cargo authority");

            MapObjectState privateCar = vehicle(game.world, 9, 4,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    VehicleRuntimeAuthority.OwnerType.PRIVATE,
                    Faction.NONE, "private", 104L);
            game.world.mapObjects.add(privateCar);
            require(!VehicleAccessAuthority.evaluate(game, privateCar,
                            VehicleAccessAuthority.Permission.REPAIR).allowed(),
                    "private property should reject uncontracted repair work");
            game.inventory.add("Vehicle service authorization");
            require(VehicleAccessAuthority.evaluate(game, privateCar,
                            VehicleAccessAuthority.Permission.REPAIR).allowed(),
                    "mechanic authorization should permit repair without title transfer");
            require(!VehicleAccessAuthority.evaluate(game, privateCar,
                            VehicleAccessAuthority.Permission.OPERATION).allowed(),
                    "repair authorization must not grant operation rights");
            require(!VehicleAccessAuthority.evaluate(game, privateCar,
                            VehicleAccessAuthority.Permission.SEIZURE).allowed(),
                    "private property should reject seizure without a warrant");
            game.inventory.add("Vehicle seizure warrant");
            require(VehicleAccessAuthority.evaluate(game, privateCar,
                            VehicleAccessAuthority.Permission.SEIZURE).allowed(),
                    "formal seizure warrant should authorize custody transfer action");

            MapObjectState abandoned = vehicle(game.world, 11, 4,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    VehicleRuntimeAuthority.OwnerType.ABANDONED,
                    Faction.NONE, "abandoned", 105L);
            game.world.mapObjects.add(abandoned);
            require(VehicleAccessAuthority.evaluate(game, abandoned,
                            VehicleAccessAuthority.Permission.REPAIR).allowed(),
                    "abandoned property should allow stabilization before claim");
            require(VehicleAccessAuthority.evaluate(game, abandoned,
                            VehicleAccessAuthority.Permission.SEIZURE).allowed(),
                    "ordinary abandoned civilian vehicle should expose the lawful claim path");
            require(!VehicleAccessAuthority.evaluate(game, abandoned,
                            VehicleAccessAuthority.Permission.OPERATION).allowed(),
                    "repair or inspection must not grant operation before claim");

            MapObjectState salvage = vehicle(game.world, 13, 4,
                    AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK,
                    VehicleRuntimeAuthority.OwnerType.SALVAGE,
                    Faction.NONE, "salvage-hulk", 106L);
            salvage.stockState = MapObjectState.setStockFlag(salvage.stockState,
                    "condition", "wreck");
            salvage.stockState = MapObjectState.setStockFlag(salvage.stockState,
                    "operationState", "disabled");
            game.world.mapObjects.add(salvage);
            require(VehicleAccessAuthority.evaluate(game, salvage,
                            VehicleAccessAuthority.Permission.CARGO).allowed(),
                    "salvage custody should allow recoverable material access");
            require(!VehicleAccessAuthority.evaluate(game, salvage,
                            VehicleAccessAuthority.Permission.OPERATION).allowed(),
                    "salvage access must not make a hulk drivable");

            List<String> lines = VehicleAccessAuthority.inspectionLines(game, privateCar);
            require(lines.size() == VehicleAccessAuthority.Permission.values().length,
                    "access inspection should expose every independent permission");
            requireContains(lines, "operate or drive", "operation permission row");
            requireContains(lines, "ride as a passenger", "passenger permission row");
            requireContains(lines, "open or transfer cargo", "cargo permission row");
            requireContains(lines, "repair or replace components", "repair permission row");
            requireContains(lines, "refuel or recharge", "refuel permission row");
            requireContains(lines, "command or deploy", "deployment permission row");
            requireContains(lines, "seize or confiscate", "seizure permission row");
            for (String line : lines) {
                require(!PlayerFacingText.containsLikelyLeak(line),
                        "vehicle access inspection leaked implementation text: " + line);
            }

            System.out.println("Milestone 06 vehicle access smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
        Milestone06VehicleStrategicTransitSmoke.main(args);
        Milestone06VehicleManifestSmoke.main(args);
    }

    private static World world() {
        World world = new World(61023L, 20, 12);
        world.zoneType = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        world.mapObjects.clear();
        world.npcs.clear();
        for (int x = 0; x < world.w; x++) {
            for (int y = 0; y < world.h; y++) world.tiles[x][y] = '.';
        }
        return world;
    }

    private static MapObjectState vehicle(World world, int x, int y,
                                          String type,
                                          VehicleRuntimeAuthority.OwnerType ownerType,
                                          Faction faction, String ownership,
                                          long seed) {
        RoadTransitFixtureAuthority.VehicleProfile profile =
                new RoadTransitFixtureAuthority.VehicleProfile(type,
                        RoadTransitFixtureAuthority.labelForType(type,
                                world.zoneType, new Random(seed)),
                        RoadTransitFixtureAuthority.stockForType(type,
                                world.tiles[x][y]));
        MapObjectState vehicle = RoadTransitFixtureAuthority.newVehicleFixture(
                world, x, y, profile, new Random(seed),
                "VEHICLE-ACCESS-SMOKE");
        VehicleRuntimeAuthority.initialize(world, vehicle,
                faction == null ? Faction.NONE : faction,
                ownership, "access-smoke", false, new Random(seed));
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerType", ownerType.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerFaction", (faction == null ? Faction.NONE : faction).name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerName", ownerType == VehicleRuntimeAuthority.OwnerType.PLAYER
                        ? "Player" : faction == null || faction == Faction.NONE
                        ? ownerType.name().toLowerCase() : faction.label);
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownership", ownership);
        return vehicle;
    }

    private static void requireContains(List<String> lines, String expected,
                                        String label) {
        for (String line : lines) {
            if (line != null && line.contains(expected)) return;
        }
        throw new AssertionError("Expected " + label + " to contain '"
                + expected + "': " + lines);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone06VehicleAccessSmoke() { }
}
