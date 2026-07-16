package mechanist;

import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Focused smoke for atomic vehicle maintenance modes and garage integration. */
final class Milestone06VehicleMaintenanceSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            game.playerX = 2;
            game.playerY = 2;
            game.turn = 920;
            game.worldTurn = 920L;
            game.carriedScript = 100;
            game.inventory.clear();
            game.inventory.add("Tool bundle");
            game.inventory.add("Machine part");

            MapObjectState garage = garage(4, 5);
            MapObjectState vehicle = vehicle(game.world, 5, 5,
                    VehicleRuntimeAuthority.OwnerType.PLAYER,
                    Faction.NONE, 71L);
            game.world.mapObjects.add(garage);
            game.world.mapObjects.add(vehicle);

            VehicleRuntimeAuthority.applyDamage(vehicle,
                    VehicleRuntimeAuthority.Component.MOBILITY, 60,
                    game.turn, "maintenance smoke worn wheels");
            VehicleMaintenanceAuthority.Result field =
                    VehicleMaintenanceAuthority.perform(game, vehicle,
                            VehicleMaintenanceAuthority.Mode.FIELD_PATCH, null);
            require(field.success() && field.changed()
                            && field.mode() == VehicleMaintenanceAuthority.Mode.FIELD_PATCH
                            && field.component() == VehicleRuntimeAuthority.Component.MOBILITY
                            && field.before() == 40 && field.after() == 60
                            && field.partsSpent() == 1 && field.scriptSpent() == 0,
                    "field patch should consume one part and restore twenty percent");
            require(count(game, "Machine part") == 0
                            && count(game, "Tool bundle") == 1
                            && game.carriedScript == 100,
                    "field patch should retain tools and avoid script charges");

            VehicleRuntimeAuthority.applyDamage(vehicle,
                    VehicleRuntimeAuthority.Component.DRIVE, 50,
                    game.turn, "maintenance smoke worn drive");
            VehicleMaintenanceAuthority.Result garageRepair =
                    VehicleMaintenanceAuthority.perform(game, vehicle,
                            VehicleMaintenanceAuthority.Mode.GARAGE_REPAIR, garage);
            require(garageRepair.success()
                            && garageRepair.component() == VehicleRuntimeAuthority.Component.DRIVE
                            && garageRepair.before() == 50 && garageRepair.after() == 85
                            && garageRepair.partsSpent() == 0
                            && garageRepair.scriptSpent() == 8
                            && game.carriedScript == 92,
                    "garage repair should use the eight-script alternative when no part is carried");

            VehicleRuntimeAuthority.applyDamage(vehicle,
                    VehicleRuntimeAuthority.Component.POWERPLANT, 100,
                    game.turn, "maintenance smoke destroyed engine");
            game.inventory.add("Machine part");
            game.inventory.add("Machine part");
            VehicleMaintenanceAuthority.Result replacement =
                    VehicleMaintenanceAuthority.perform(game, vehicle,
                            VehicleMaintenanceAuthority.Mode.COMPONENT_REPLACEMENT,
                            garage);
            require(replacement.success()
                            && replacement.component() == VehicleRuntimeAuthority.Component.POWERPLANT
                            && replacement.before() == 0 && replacement.after() == 100
                            && replacement.partsSpent() == 2
                            && replacement.scriptSpent() == 12
                            && component(vehicle,
                            VehicleRuntimeAuthority.Component.POWERPLANT) == 100
                            && game.carriedScript == 80,
                    "component replacement should atomically restore a destroyed powerplant");
            require("parked".equals(MapObjectState.stockValue(
                            vehicle.stockState, "operationState")),
                    "replacing a destroyed critical component should return the vehicle to parked service");

            VehicleRuntimeAuthority.applyDamage(vehicle,
                    VehicleRuntimeAuthority.Component.FRAME, 50,
                    game.turn, "maintenance smoke frame wear");
            VehicleRuntimeAuthority.applyDamage(vehicle,
                    VehicleRuntimeAuthority.Component.ARMOR, 60,
                    game.turn, "maintenance smoke armor wear");
            VehicleRuntimeAuthority.applyDamage(vehicle,
                    VehicleRuntimeAuthority.Component.CARGO, 70,
                    game.turn, "maintenance smoke cargo wear");
            VehicleRuntimeAuthority.applyDamage(vehicle,
                    VehicleRuntimeAuthority.Component.LIGHTS, 80,
                    game.turn, "maintenance smoke light wear");
            for (int i = 0; i < 4; i++) game.inventory.add("Machine part");
            VehicleMaintenanceAuthority.Result refurbishment =
                    VehicleMaintenanceAuthority.perform(game, vehicle,
                            VehicleMaintenanceAuthority.Mode.FULL_REFURBISHMENT,
                            garage);
            require(refurbishment.success()
                            && refurbishment.before() < 100
                            && refurbishment.after() == 100
                            && refurbishment.partsSpent() == 4
                            && refurbishment.scriptSpent() == 30
                            && game.carriedScript == 50
                            && allComponents(vehicle, 100),
                    "full refurbishment should restore every major component group");
            require(count(game, "Tool bundle") == 1,
                    "full refurbishment should require but retain the Tool bundle");
            requireContains(MapObjectState.stockValue(
                            vehicle.stockState, "repairHistory"),
                    "full refurbishment", "vehicle repair provenance");
            require("FULL_REFURBISHMENT".equals(MapObjectState.stockValue(
                            vehicle.stockState, "lastMaintenanceMode"))
                            && "4".equals(MapObjectState.stockValue(
                            vehicle.stockState, "lastMaintenanceLaborTurns")),
                    "maintenance mode and labor scale should persist on the vehicle record");

            VehicleRuntimeAuthority.applyDamage(vehicle,
                    VehicleRuntimeAuthority.Component.MOBILITY, 100,
                    game.turn, "maintenance smoke second disabled state");
            int scriptBeforeBlocked = game.carriedScript;
            String stockBeforeBlocked = vehicle.stockState;
            VehicleMaintenanceAuthority.Result blocked =
                    VehicleMaintenanceAuthority.perform(game, vehicle,
                            VehicleMaintenanceAuthority.Mode.COMPONENT_REPLACEMENT,
                            garage);
            require(!blocked.success() && game.carriedScript == scriptBeforeBlocked
                            && stockBeforeBlocked.equals(vehicle.stockState),
                    "insufficient replacement resources must leave script and vehicle state untouched");

            for (int i = 0; i < 2; i++) game.inventory.add("Machine part");
            VehicleMaintenanceAuthority.Result restored =
                    VehicleMaintenanceAuthority.perform(game, vehicle,
                            VehicleMaintenanceAuthority.Mode.COMPONENT_REPLACEMENT,
                            garage);
            require(restored.success() && component(vehicle,
                            VehicleRuntimeAuthority.Component.MOBILITY) == 100,
                    "replacement resources should restore the disabled mobility assembly");

            VehicleRuntimeAuthority.applyDamage(vehicle,
                    VehicleRuntimeAuthority.Component.LIGHTS, 20,
                    game.turn, "maintenance smoke garage interaction");
            game.inventory.add("Machine part");
            String garageLine = VehicleEconomyFrontageAuthority.interaction(game, garage);
            requireContains(garageLine, "VEHICLE MAINTENANCE",
                    "live service-garage integration");
            require(component(vehicle, VehicleRuntimeAuthority.Component.LIGHTS) == 100,
                    "live service garage should repair the nearest player-owned vehicle");

            MapObjectState factionVehicle = vehicle(game.world, 7, 5,
                    VehicleRuntimeAuthority.OwnerType.FACTION,
                    Faction.NOBLE, 72L);
            game.world.mapObjects.add(factionVehicle);
            VehicleRuntimeAuthority.applyDamage(factionVehicle,
                    VehicleRuntimeAuthority.Component.FRAME, 20,
                    game.turn, "maintenance smoke foreign property");
            VehicleMaintenanceAuthority.Result denied =
                    VehicleMaintenanceAuthority.perform(game, factionVehicle,
                            VehicleMaintenanceAuthority.Mode.FIELD_PATCH, null);
            require(!denied.success(),
                    "player maintenance must not mutate faction-owned vehicles");

            List<String> inspection =
                    VehicleMaintenanceAuthority.inspectionLines(game, vehicle);
            requireContains(inspection, "Field patch", "field-patch guidance");
            requireContains(inspection, "Garage repair", "garage guidance");
            requireContains(inspection, "Component replacement",
                    "replacement guidance");
            requireContains(inspection, "Full refurbishment",
                    "refurbishment guidance");
            for (String line : inspection) {
                require(!PlayerFacingText.containsLikelyLeak(line),
                        "vehicle maintenance guidance leaked implementation text: " + line);
            }
            System.out.println("Milestone 06 vehicle maintenance smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
        Milestone06VehicleLossSmoke.main(args);
    }

    private static World world() {
        World world = new World(61015L, 20, 14);
        world.zoneType = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        world.mapObjects.clear();
        world.npcs.clear();
        for (int x = 0; x < world.w; x++) {
            for (int y = 0; y < world.h; y++) world.tiles[x][y] = '.';
        }
        return world;
    }

    private static MapObjectState garage(int x, int y) {
        MapObjectState garage = new MapObjectState();
        garage.id = "VEHICLE-MAINTENANCE-GARAGE";
        garage.type = AssetIntegrationDisciplineAuthority.VEHICLE_SERVICE_GARAGE_FRONTAGE;
        garage.label = "Civic vehicle maintenance garage";
        garage.glyph = 'T';
        garage.x = x;
        garage.y = y;
        garage.stockState = "service=vehicle-maintenance;partsAccepted=true";
        return garage;
    }

    private static MapObjectState vehicle(World world, int x, int y,
                                          VehicleRuntimeAuthority.OwnerType ownerType,
                                          Faction faction, long seed) {
        String type = AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR;
        RoadTransitFixtureAuthority.VehicleProfile profile =
                new RoadTransitFixtureAuthority.VehicleProfile(type,
                        RoadTransitFixtureAuthority.labelForType(type,
                                world.zoneType, new Random(seed)),
                        RoadTransitFixtureAuthority.stockForType(type,
                                world.tiles[x][y]));
        MapObjectState vehicle = RoadTransitFixtureAuthority.newVehicleFixture(
                world, x, y, profile, new Random(seed),
                "VEHICLE-MAINTENANCE-SMOKE");
        String ownership = ownerType == VehicleRuntimeAuthority.OwnerType.PLAYER
                ? "player-owned" : "faction";
        VehicleRuntimeAuthority.initialize(world, vehicle,
                faction == null ? Faction.NONE : faction,
                ownership, "maintenance-smoke", false, new Random(seed));
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerType", ownerType.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerFaction", (faction == null ? Faction.NONE : faction).name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerName", ownerType == VehicleRuntimeAuthority.OwnerType.PLAYER
                        ? "Player" : faction == null ? "Unassigned" : faction.label);
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownership", ownership);
        return vehicle;
    }

    private static int component(MapObjectState vehicle,
                                 VehicleRuntimeAuthority.Component component) {
        String key = "vehicleComponent" + component.name().charAt(0)
                + component.name().substring(1).toLowerCase(Locale.ROOT);
        try {
            return Integer.parseInt(MapObjectState.stockValue(
                    vehicle.stockState, key));
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static boolean allComponents(MapObjectState vehicle, int expected) {
        for (VehicleRuntimeAuthority.Component component
                : VehicleRuntimeAuthority.Component.values()) {
            if (component(vehicle, component) != expected) return false;
        }
        return true;
    }

    private static int count(GamePanel game, String item) {
        int count = 0;
        for (String carried : game.inventory) {
            if (ItemQuality.namesMatch(carried, item)) count++;
        }
        return count;
    }

    private static void requireContains(List<String> lines, String expected,
                                        String label) {
        for (String line : lines) {
            if (line != null && line.contains(expected)) return;
        }
        throw new AssertionError("Expected " + label + " to contain '"
                + expected + "': " + lines);
    }

    private static void requireContains(String text, String expected,
                                        String label) {
        require(text != null && text.contains(expected),
                "Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone06VehicleMaintenanceSmoke() { }
}
