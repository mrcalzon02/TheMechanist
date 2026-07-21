package mechanist;

import java.util.Random;

/** Focused smoke for motor-pool repair, fuel, and ordinary maintenance priority. */
final class Milestone06VehicleContractPrioritySmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            game.turn = 1_560;
            game.worldTurn = 1_560L;
            NpcEntity representative = representative(
                    Faction.MECHANIST_COLLEGIA);

            verifyCriticalRepairPrecedesFuel(game, representative);
            verifyHealthyFuelPrecedesWornRepair(game, representative);
            verifyWornVehicleStillReceivesRepair(game, representative);

            System.out.println(
                    "Milestone 06 vehicle contract priority smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static void verifyCriticalRepairPrecedesFuel(
            GamePanel game, NpcEntity representative) {
        reset(game);
        MapObjectState critical = vehicle(game.world, 4, 4,
                "critical damaged reserve", 501L);
        MapObjectState healthyLow = vehicle(game.world, 7, 4,
                "healthy low-energy reserve", 502L);
        game.world.mapObjects.add(critical);
        game.world.mapObjects.add(healthyLow);
        VehicleRuntimeAuthority.applyDamage(critical,
                VehicleRuntimeAuthority.Component.POWERPLANT, 100,
                game.turn, "priority smoke destroyed powerplant");
        setFuel(critical, 0);
        setFuel(healthyLow, 0);

        VehicleRuntimeAuthority.Snapshot criticalSnapshot =
                VehicleRuntimeAuthority.inspect(game.world, critical);
        require("disabled".equals(criticalSnapshot.condition())
                        || "disabled".equals(
                        criticalSnapshot.operationState()),
                "critical fixture should enter a disabled state");
        String offer = FactionMarketContractAuthority.representativeLine(
                game, representative);
        require(offer.contains(
                        "critical damaged vehicle repair backlog")
                        && offer.contains(criticalSnapshot.model()),
                "critical damaged vehicle must be offered before a healthy empty vehicle: "
                        + offer);

        FactionMarketContractAuthority.WorkResult accepted =
                FactionMarketContractAuthority.accept(game, representative);
        require(accepted.success() && accepted.contract() != null
                        && accepted.contract().targetEntityId.startsWith(
                        "MARKET:VEHICLE_REPAIR:")
                        && accepted.contract().targetEntityId.endsWith(
                        critical.id),
                "critical repair contract must bind the exact disabled vehicle: "
                        + accepted.message());
    }

    private static void verifyHealthyFuelPrecedesWornRepair(
            GamePanel game, NpcEntity representative) {
        reset(game);
        MapObjectState healthyLow = vehicle(game.world, 4, 6,
                "healthy low-energy courier", 503L);
        MapObjectState worn = vehicle(game.world, 7, 6,
                "worn full-energy courier", 504L);
        game.world.mapObjects.add(healthyLow);
        game.world.mapObjects.add(worn);
        setFuel(healthyLow, 0);
        VehicleRuntimeAuthority.applyDamage(worn,
                VehicleRuntimeAuthority.Component.LIGHTS, 20,
                game.turn, "priority smoke ordinary wear");

        VehicleRuntimeAuthority.Snapshot wornSnapshot =
                VehicleRuntimeAuthority.inspect(game.world, worn);
        require("worn".equals(wornSnapshot.condition()),
                "ordinary repair fixture should remain worn rather than critical: "
                        + wornSnapshot.condition());
        String offer = FactionMarketContractAuthority.representativeLine(
                game, representative);
        VehicleRuntimeAuthority.Snapshot fuelSnapshot =
                VehicleRuntimeAuthority.inspect(game.world, healthyLow);
        require(offer.contains(
                        "empty or low vehicle fuel or power reserve")
                        && offer.contains(fuelSnapshot.model()),
                "healthy empty vehicle should receive fuel before merely worn repair: "
                        + offer);

        FactionMarketContractAuthority.WorkResult accepted =
                FactionMarketContractAuthority.accept(game, representative);
        require(accepted.success() && accepted.contract() != null
                        && accepted.contract().targetEntityId.startsWith(
                        "MARKET:VEHICLE_FUEL:")
                        && accepted.contract().targetEntityId.endsWith(
                        healthyLow.id),
                "fuel contract must bind the exact healthy empty vehicle: "
                        + accepted.message());
    }

    private static void verifyWornVehicleStillReceivesRepair(
            GamePanel game, NpcEntity representative) {
        reset(game);
        MapObjectState worn = vehicle(game.world, 5, 8,
                "worn maintenance backlog", 505L);
        game.world.mapObjects.add(worn);
        VehicleRuntimeAuthority.applyDamage(worn,
                VehicleRuntimeAuthority.Component.LIGHTS, 20,
                game.turn, "priority smoke ordinary repair fallback");

        VehicleRuntimeAuthority.Snapshot wornSnapshot =
                VehicleRuntimeAuthority.inspect(game.world, worn);
        String offer = FactionMarketContractAuthority.representativeLine(
                game, representative);
        require(offer.contains("damaged vehicle repair backlog")
                        && !offer.contains(
                        "critical damaged vehicle repair backlog")
                        && offer.contains(wornSnapshot.model()),
                "worn full-energy vehicle should remain eligible for ordinary repair: "
                        + offer);

        FactionMarketContractAuthority.WorkResult accepted =
                FactionMarketContractAuthority.accept(game, representative);
        require(accepted.success() && accepted.contract() != null
                        && accepted.contract().targetEntityId.startsWith(
                        "MARKET:VEHICLE_REPAIR:")
                        && accepted.contract().targetEntityId.endsWith(worn.id),
                "ordinary repair contract must bind the exact worn vehicle: "
                        + accepted.message());
    }

    private static void reset(GamePanel game) {
        game.factionContracts.clear();
        game.inventory.clear();
        game.factionMarketPressure.clear();
        game.world.mapObjects.clear();
        game.world.topDownWorldEvents.clear();
        game.world.shipmentRecords.clear();
        game.world.replacementQueue.clear();
        game.world.essentialSupplyReserves.clear();
        game.world.rawMaterialSupplyReserves.clear();
    }

    private static void setFuel(MapObjectState vehicle, int amount) {
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "fuelOrPowerCurrent", Integer.toString(Math.max(0, amount)));
    }

    private static NpcEntity representative(Faction faction) {
        NpcEntity representative = new NpcEntity();
        representative.id = "VEHICLE-PRIORITY-REP-" + faction.name();
        representative.name = faction.label + " Motor Pool Clerk";
        representative.role = "Faction Representative";
        representative.faction = faction;
        representative.hp = 10;
        return representative;
    }

    private static World world() {
        World world = new World(61040L, 16, 12);
        world.zoneType = ZoneType.MECHANICUS_FORGE_CLOISTER;
        world.sectorX = 5;
        world.sectorY = 1;
        world.zoneX = 3;
        world.zoneY = 1;
        world.floor = 0;
        world.mapObjects.clear();
        world.npcs.clear();
        for (int x = 0; x < world.w; x++) {
            for (int y = 0; y < world.h; y++) world.tiles[x][y] = '.';
        }
        return world;
    }

    private static MapObjectState vehicle(World world, int x, int y,
                                          String role, long seed) {
        String type = AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK;
        RoadTransitFixtureAuthority.VehicleProfile profile =
                new RoadTransitFixtureAuthority.VehicleProfile(type,
                        RoadTransitFixtureAuthority.labelForType(type,
                                world.zoneType, new Random(seed)),
                        RoadTransitFixtureAuthority.stockForType(type,
                                world.tiles[x][y]));
        MapObjectState vehicle = RoadTransitFixtureAuthority.newVehicleFixture(
                world, x, y, profile, new Random(seed),
                "VEHICLE-CONTRACT-PRIORITY-SMOKE");
        VehicleRuntimeAuthority.initialize(world, vehicle,
                Faction.MECHANIST_COLLEGIA, "faction", role,
                false, new Random(seed));
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerType", VehicleRuntimeAuthority.OwnerType.FACTION.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerFaction", Faction.MECHANIST_COLLEGIA.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerName", Faction.MECHANIST_COLLEGIA.label);
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownership", "faction");
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "vehicleRole", role);
        VehicleFuelAuthority.ensureInitialized(world, vehicle);
        return vehicle;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone06VehicleContractPrioritySmoke() { }
}
