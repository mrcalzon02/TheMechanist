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
            String beforeReach = vehicle.stockState;
            int scriptBeforeReach = game.carriedScript;
            int partsBeforeReach = count(game, "Machine part");
            VehicleMaintenanceAuthority.Result distantField =
                    VehicleMaintenanceAuthority.perform(game, vehicle,
                            VehicleMaintenanceAuthority.Mode.FIELD_PATCH, null);
            VehicleMaintenanceAuthority.Result remoteGarage =
                    VehicleMaintenanceAuthority.perform(game, vehicle,
                            VehicleMaintenanceAuthority.Mode.GARAGE_REPAIR, null);
            VehicleMaintenanceAuthority.Result phantomRefurbishment =
                    VehicleMaintenanceAuthority.perform(game, vehicle,
                            VehicleMaintenanceAuthority.Mode.FULL_REFURBISHMENT, null);
            require(!distantField.success() && distantField.message().contains("Move adjacent")
                            && !remoteGarage.success() && remoteGarage.message().contains("service garage")
                            && !phantomRefurbishment.success()
                            && beforeReach.equals(vehicle.stockState)
                            && scriptBeforeReach == game.carriedScript
                            && partsBeforeReach == count(game, "Machine part"),
                    "remote field work and phantom garage modes must refuse without resource or vehicle mutation");
            game.playerX = 4;
            game.playerY = 5;
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
            MapObjectState healthy = vehicle(game.world, 4, 4,
                    VehicleRuntimeAuthority.OwnerType.PLAYER, Faction.NONE, 73L);
            healthy.id = "000-HEALTHY-CAR";
            game.world.mapObjects.add(healthy);
            int healthyIntegrity = VehicleRuntimeAuthority.integrity(healthy);
            game.inventory.add("Machine part");
            String garageLine = VehicleEconomyFrontageAuthority.interaction(game, garage);
            requireContains(garageLine, "VEHICLE MAINTENANCE",
                    "live service-garage integration");
            require(component(vehicle, VehicleRuntimeAuthority.Component.LIGHTS) == 100
                            && VehicleRuntimeAuthority.integrity(healthy) == healthyIntegrity,
                    "garage should select a damaged staged vehicle, not a closer healthy one");
            game.world.mapObjects.remove(healthy);

            int priorX = vehicle.x, priorY = vehicle.y;
            vehicle.x = 18; vehicle.y = 12;
            String remoteStock = vehicle.stockState;
            int remoteScript = game.carriedScript;
            int remoteParts = count(game, "Machine part");
            VehicleMaintenanceAuthority.Result remote = VehicleMaintenanceAuthority.perform(
                    game, vehicle, VehicleMaintenanceAuthority.Mode.GARAGE_REPAIR, garage);
            require(!remote.success() && remote.message().contains("within 6 tiles")
                            && remoteStock.equals(vehicle.stockState)
                            && remoteScript == game.carriedScript
                            && remoteParts == count(game, "Machine part"),
                    "distant garage service must refuse without spending parts, script, or changing vehicle state");
            VehicleMaintenanceAuthority.Result remoteNearest =
                    VehicleMaintenanceAuthority.serviceNearestPlayerVehicle(game, garage);
            require(!remoteNearest.success()
                            && remoteStock.equals(vehicle.stockState)
                            && remoteScript == game.carriedScript,
                    "garage must not search the entire zone for a remote vehicle");
            vehicle.x = priorX; vehicle.y = priorY;

            // The game must provide an honest paid fuel source after local
            // travel started consuming persistent energy, without silently
            // charging for inspection or allowing remote/full refueling.
            VehicleFuelAuthority.ensureInitialized(game.world, vehicle);
            vehicle.stockState = MapObjectState.setStockFlag(
                    vehicle.stockState, "fuelOrPowerCurrent", "8");
            require(VehicleFuelAuthority.affordableGarageUnits(8, 0) == 0
                            && VehicleFuelAuthority.affordableGarageUnits(8, 3) == 3
                            && VehicleFuelAuthority.affordableGarageUnits(8, 99) == 8
                            && VehicleFuelAuthority.affordableGarageUnits(2, 99) == 2
                            && VehicleFuelAuthority.affordableGarageUnits(0, 99) == 0,
                    "garage refilling must handle zero, partial, capped and full batches");
            require(VehicleFuelAuthority.localRoadRangeTiles(
                            VehicleRuntimeAuthority.VehicleClass.CIVILIAN_CAR, 4) == 24
                            && VehicleFuelAuthority.routeEnergyCost(
                            VehicleRuntimeAuthority.VehicleClass.CIVILIAN_CAR, 24) == 4
                            && VehicleFuelAuthority.localRoadRangeTiles(
                            VehicleRuntimeAuthority.VehicleClass.TANK, 4) == 8,
                    "readable road range must invert the same class-specific travel cost");
            game.carriedScript = 8;
            String beforeRefill = vehicle.stockState;
            int beforeRefillTurn = game.turn;
            int beforeRefillXp = game.xp;
            int beforeRefillVend = garage.vendCount;
            require(RoadTransitFixtureInteractionAuthority.tryInteract(
                            game, garage.x, garage.y),
                    "live service garage must support a paid refill after repairs");
            VehicleFuelAuthority.Snapshot afterRefill = VehicleFuelAuthority.inspect(
                    game.world, vehicle);
            require(afterRefill.current() == 16 && game.carriedScript == 0
                            && game.turn == beforeRefillTurn + 1
                            && game.xp > beforeRefillXp
                            && garage.vendCount == beforeRefillVend + 1
                            && !vehicle.stockState.equals(beforeRefill),
                    "paid live refill must add eight energy units, spend eight script and one turn");
            requireContains(MapObjectState.stockValue(
                            vehicle.stockState, "fuelOrPowerHistory"),
                    "paid service garage refill", "refill provenance");

            String insufficientStock = vehicle.stockState;
            int insufficientTurn = game.turn;
            long insufficientWorldTurn = game.worldTurn;
            int insufficientXp = game.xp;
            int insufficientVend = garage.vendCount;
            int insufficientCooldown = garage.cooldownUntilTurn;
            VehicleEconomyFrontageAuthority.InteractionResult noMoney =
                    VehicleEconomyFrontageAuthority.interactionResult(game, garage);
            require(!noMoney.success() && !noMoney.changed()
                            && noMoney.message().contains("8 script")
                            && vehicle.stockState.equals(insufficientStock)
                            && game.turn == insufficientTurn
                            && game.worldTurn == insufficientWorldTurn
                            && game.xp == insufficientXp
                            && garage.vendCount == insufficientVend
                            && garage.cooldownUntilTurn == insufficientCooldown,
                    "insufficient script must not grant fuel, spend time, or mutate vendor state");

            game.carriedScript = 3;
            int partialTurn = game.turn;
            int partialXp = game.xp;
            int partialVend = garage.vendCount;
            require(RoadTransitFixtureInteractionAuthority.tryInteract(
                            game, garage.x, garage.y),
                    "three script must purchase a partial batch from the live garage");
            require(VehicleFuelAuthority.inspect(game.world, vehicle).current() == 19
                            && game.carriedScript == 0
                            && game.turn == partialTurn + 1
                            && game.xp > partialXp
                            && garage.vendCount == partialVend + 1,
                    "partial garage refill must add three units and charge exactly three script once");

            game.carriedScript = 5;
            require(RoadTransitFixtureInteractionAuthority.tryInteract(
                            game, garage.x, garage.y),
                    "remaining five unit top-up must be physically reachable");
            require(VehicleFuelAuthority.inspect(game.world, vehicle).current() == 24
                            && game.carriedScript == 0,
                    "final five script must finish filling the tank without exceeding capacity");
            String fullStock = vehicle.stockState;
            int fullTurn = game.turn;
            int fullXp = game.xp;
            int fullVend = garage.vendCount;
            require(RoadTransitFixtureInteractionAuthority.tryInteract(
                            game, garage.x, garage.y),
                    "full garage fuel status remains inspectable");
            require(vehicle.stockState.equals(fullStock)
                            && game.turn == fullTurn
                            && game.xp == fullXp
                            && garage.vendCount == fullVend,
                    "full tank inspection cannot trigger hidden costs or XP rewards");
            vehicle.stockState = MapObjectState.setStockFlag(
                    vehicle.stockState, "fuelOrPowerCurrent", "14");
            int savedX = vehicle.x, savedY = vehicle.y;
            vehicle.x = 18; vehicle.y = 12;
            String remoteFuelStock = vehicle.stockState;
            int remoteFuelScript = game.carriedScript;
            VehicleFuelAuthority.Result remoteFuel = VehicleFuelAuthority.refuelAtGarage(
                    game, garage, vehicle);
            require(!remoteFuel.success()
                            && remoteFuelStock.equals(vehicle.stockState)
                            && remoteFuelScript == game.carriedScript,
                    "remote refuel requests must leave ledger and currency unchanged");
            vehicle.x = savedX; vehicle.y = savedY;
            vehicle.stockState = MapObjectState.setStockFlag(
                    vehicle.stockState, "fuelOrPowerCurrent", "24");

            MapObjectState detached = vehicle(game.world, 9, 9,
                    VehicleRuntimeAuthority.OwnerType.PLAYER, Faction.NONE, 74L);
            VehicleRuntimeAuthority.applyDamage(detached,
                    VehicleRuntimeAuthority.Component.MOBILITY, 35,
                    game.turn, "detached vehicle regression");
            String detachedStock = detached.stockState;
            int detachedScript = game.carriedScript;
            int detachedParts = count(game, "Machine part");
            VehicleMaintenanceAuthority.Result stale = VehicleMaintenanceAuthority.perform(
                    game, detached, VehicleMaintenanceAuthority.Mode.GARAGE_REPAIR, garage);
            require(!stale.success() && stale.message().contains("actual vehicle")
                            && detachedStock.equals(detached.stockState)
                            && detachedScript == game.carriedScript
                            && detachedParts == count(game, "Machine part"),
                    "a detached but otherwise titled vehicle cannot be serviced through a stale reference");

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
            requireContains(inspection, "stand next to the vehicle",
                    "physical field repair guidance");
            requireContains(inspection, "service garage required",
                    "required refurbishment facility guidance");
            requireContains(VehicleFuelAuthority.inspectionLines(game.world, vehicle),
                    "Garage refill:", "fuel and refilling player guidance");
            requireContains(VehicleFuelAuthority.inspectionLines(game.world, vehicle),
                    "Estimated local road range:", "reserve-aware local-road range guidance");
            requireContains(VehicleInfopediaBridgeAuthority.liveDossier(game, vehicle),
                    "Estimated local road range:", "live vehicle dashboard range guidance");
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
