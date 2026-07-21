package mechanist;

import java.util.List;
import java.util.Random;

/** Focused smoke for bounded persistent motor-pool and deployment history. */
final class Milestone06VehicleMotorPoolHistorySmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            game.turn = 1_700;
            game.worldTurn = 1_700L;
            game.npcFactionSites.clear();

            NpcFactionSite site = NpcFactionSite.create(
                    "Bounded Motor Pool",
                    Faction.MECHANIST_COLLEGIA, "motor pool",
                    game.world.sectorX, game.world.sectorY,
                    game.world.zoneX, game.world.zoneY, game.world.floor,
                    "Machine part", "Tool bundle",
                    "Bounded Fleet Doctrine");
            site.stock = 60;
            site.workers = 5;
            site.baseLevel = 2;
            site.machineLevel = 3;
            game.npcFactionSites.add(site);

            MapObjectState truck = vehicle(game.world, 5, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK,
                    site.faction, 191L);
            game.world.mapObjects.add(truck);

            for (int cycle = 0; cycle < 19; cycle++) {
                game.turn++;
                game.worldTurn++;
                VehicleMotorPoolAuthority.Result assigned =
                        VehicleMotorPoolAuthority.assign(game, truck, site,
                                "reserve " + cycle,
                                "cycle assignment " + cycle);
                require(assigned.success() && assigned.changed()
                                && assigned.snapshot().assigned(),
                        "cycle " + cycle + " should create an active assignment");

                game.turn++;
                game.worldTurn++;
                VehicleMotorPoolAuthority.Result released =
                        VehicleMotorPoolAuthority.release(game, truck, site,
                                "cycle release " + cycle);
                require(released.success() && released.changed()
                                && !released.snapshot().assigned(),
                        "cycle " + cycle + " should release the assignment");
            }

            game.turn++;
            game.worldTurn++;
            VehicleMotorPoolAuthority.Result finalAssignment =
                    VehicleMotorPoolAuthority.assign(game, truck, site,
                            "reserve 19", "final active assignment");
            require(finalAssignment.success() && finalAssignment.changed()
                            && finalAssignment.snapshot().assigned()
                            && "reserve 19".equals(finalAssignment.snapshot().role()),
                    "final cycle should leave the vehicle actively assigned");

            VehicleMotorPoolAuthority.Snapshot snapshot =
                    VehicleMotorPoolAuthority.inspect(game, truck, site);
            List<String> history = snapshot.history();
            require(history.size() == 12,
                    "motor-pool history should retain exactly twelve entries: "
                            + history);
            require(history.get(0).contains("cycle release 13"),
                    "oldest retained motor-pool entry should be cycle release 13: "
                            + history.get(0));
            require(history.get(history.size() - 1).contains("final active assignment")
                            && history.get(history.size() - 1).contains("reserve 19"),
                    "newest motor-pool entry should describe the final assignment: "
                            + history.get(history.size() - 1));
            require(history.stream().noneMatch(line -> line.contains("cycle assignment 0")
                            || line.contains("cycle release 12")),
                    "retired motor-pool history must not survive the retention boundary");

            List<String> deployment = tokens(MapObjectState.stockValue(
                    truck.stockState, "deploymentHistory"));
            require(deployment.size() == 12,
                    "motor-pool-owned deployment history should retain twelve entries: "
                            + deployment);
            require(deployment.get(0).contains("cycle release 13")
                            && deployment.get(deployment.size() - 1).contains("reserve 19"),
                    "deployment history should retain the same newest motor-pool boundary");

            MapObjectState restored = copy(truck);
            VehicleMotorPoolAuthority.Snapshot restoredSnapshot =
                    VehicleMotorPoolAuthority.inspect(game, restored, site);
            require(restoredSnapshot.assigned()
                            && "reserve 19".equals(restoredSnapshot.role())
                            && restoredSnapshot.history().equals(history),
                    "bounded assignment and history should reconstruct from persisted stock state");

            System.out.println("Milestone 06 vehicle motor-pool history smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static List<String> tokens(String value) {
        if (value == null || value.isBlank()) return List.of();
        return java.util.Arrays.stream(value.split("~"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .toList();
    }

    private static World world() {
        World world = new World(61043L, 18, 12);
        world.zoneType = ZoneType.MECHANICUS_FORGE_CLOISTER;
        world.sectorX = 4;
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
                                          String type, Faction faction,
                                          long seed) {
        RoadTransitFixtureAuthority.VehicleProfile profile =
                new RoadTransitFixtureAuthority.VehicleProfile(type,
                        RoadTransitFixtureAuthority.labelForType(type,
                                world.zoneType, new Random(seed)),
                        RoadTransitFixtureAuthority.stockForType(type,
                                world.tiles[x][y]));
        MapObjectState vehicle = RoadTransitFixtureAuthority.newVehicleFixture(
                world, x, y, profile, new Random(seed),
                "VEHICLE-MOTOR-POOL-HISTORY-SMOKE");
        VehicleRuntimeAuthority.initialize(world, vehicle, faction,
                "faction", "motor-pool-history-smoke", false,
                new Random(seed));
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerType", VehicleRuntimeAuthority.OwnerType.FACTION.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerFaction", faction.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerName", faction.label);
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownership", "faction");
        VehicleFuelAuthority.ensureInitialized(world, vehicle);
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

    private Milestone06VehicleMotorPoolHistorySmoke() { }
}
