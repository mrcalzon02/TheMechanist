package mechanist;

import java.util.List;
import java.util.Random;

/** Focused smoke for large-target combat preflight and structural damage scale. */
final class Milestone06StructuralScaleCombatSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            game.playerX = 2;
            game.playerY = 5;
            game.turn = 1_520;
            game.worldTurn = 1_520L;
            game.inventory.clear();
            game.baseObjects.clear();
            game.loadedWeaponShots.clear();
            game.terrainIntegrity.clear();

            MapObjectState tank = vehicle(game.world, 3, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_TANK, 301L);
            MapObjectState car = vehicle(game.world, 5, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    302L);
            game.world.mapObjects.add(tank);
            game.world.mapObjects.add(car);

            equip(game, "Scrap knife", 0, 0);
            game.combatX = tank.x;
            game.combatY = tank.y;
            int tankBeforeKnife = component(tank,
                    VehicleRuntimeAuthority.Component.ARMOR);
            int knifeTurn = game.turn;
            StructuralScaleCombatAuthority.Result knife =
                    StructuralScaleCombatAuthority.confirm(game);
            require(knife.success() && !knife.changed()
                            && knife.damage() == 0
                            && component(tank,
                            VehicleRuntimeAuthority.Component.ARMOR)
                            == tankBeforeKnife
                            && game.turn == knifeTurn + 1,
                    "knife strike should spend an action but cause no tank damage");
            requireContains(knife.message(), "cannot meaningfully penetrate",
                    "knife-versus-tank feedback");

            equip(game, "Light Rifle", 10, 0);
            game.combatX = car.x;
            game.combatY = car.y;
            int carArmorBefore = component(car,
                    VehicleRuntimeAuthority.Component.ARMOR);
            int rifleAmmoBefore = game.loadedWeaponShots.get("Light Rifle");
            StructuralScaleCombatAuthority.Result rifle =
                    StructuralScaleCombatAuthority.confirm(game);
            require(rifle.success() && rifle.changed()
                            && rifle.damage() > 0 && rifle.damage() < 20
                            && component(car,
                            VehicleRuntimeAuthority.Component.ARMOR)
                            == carArmorBefore - rifle.damage()
                            && game.loadedWeaponShots.get("Light Rifle")
                            == rifleAmmoBefore - 1,
                    "rifle should inflict bounded damage on a light civilian vehicle");

            equip(game, "Arc Rifle", 20, 1);
            game.combatX = tank.x;
            game.combatY = tank.y;
            int tankArmorBefore = component(tank,
                    VehicleRuntimeAuthority.Component.ARMOR);
            StructuralScaleCombatAuthority.Result antiArmor =
                    StructuralScaleCombatAuthority.confirm(game);
            require(antiArmor.success() && antiArmor.changed()
                            && antiArmor.damage() >= 5
                            && antiArmor.damage() <= 20
                            && component(tank,
                            VehicleRuntimeAuthority.Component.ARMOR)
                            == tankArmorBefore - antiArmor.damage(),
                    "aimed arc-rifle fire should meaningfully but not instantly damage tank armor");

            game.world.tiles[8][5] = '#';
            equip(game, "Needle Pistol", 6, 0);
            game.combatX = 8;
            game.combatY = 5;
            int pistolTurn = game.turn;
            int pistolAmmo = game.loadedWeaponShots.get("Needle Pistol");
            StructuralScaleCombatAuthority.Result pistolWall =
                    StructuralScaleCombatAuthority.confirm(game);
            require(pistolWall.success() && !pistolWall.changed()
                            && pistolWall.damage() == 0
                            && game.world.tiles[8][5] == '#'
                            && game.turn == pistolTurn + 1
                            && game.loadedWeaponShots.get("Needle Pistol")
                            == pistolAmmo - 1,
                    "handgun fire should spend ammunition and time without weakening a block wall");

            equip(game, "Arc Rifle", 40, 1);
            game.combatX = 8;
            game.combatY = 5;
            StructuralScaleCombatAuthority.Result firstWallHit =
                    StructuralScaleCombatAuthority.confirm(game);
            require(firstWallHit.changed() && firstWallHit.damage() > 0
                            && game.world.tiles[8][5] == '#',
                    "anti-armor fire should damage but not one-shot a full block wall");
            int wallHits = 1;
            while (game.world.tiles[8][5] == '#' && wallHits < 24) {
                StructuralScaleCombatAuthority.confirm(game);
                wallHits++;
            }
            require(game.world.tiles[8][5] == '.'
                            && wallHits >= 8 && wallHits <= 20,
                    "block wall should require repeated structural hits before breaching; hits="
                            + wallHits);

            game.world.tiles[9][5] = 'L';
            game.combatX = 9;
            game.combatY = 5;
            int doorHits = 0;
            while (game.world.tiles[9][5] != '/' && doorHits < 12) {
                StructuralScaleCombatAuthority.confirm(game);
                doorHits++;
            }
            require(game.world.tiles[9][5] == '/'
                            && doorHits > 1 && doorHits <= 8,
                    "locked door should become a passable breached doorway after repeated anti-armor impacts");

            BaseObject press = new BaseObject("Industrial press", 'P',
                    10, 5, 0, 0);
            press.description = "Heavy industrial machine";
            press.integrity = 120;
            game.baseObjects.add(press);
            game.combatX = press.x;
            game.combatY = press.y;
            int machineBefore = press.integrity;
            StructuralScaleCombatAuthority.Result machineHit =
                    StructuralScaleCombatAuthority.confirm(game);
            require(machineHit.changed() && machineHit.damage() > 0
                            && press.integrity == machineBefore
                            - machineHit.damage(),
                    "constructed machine should lose its existing BaseObject integrity through the structural authority");

            MapObjectState generator = durableObject(11, 5,
                    "industrial-generator", "Auxiliary generator");
            game.world.mapObjects.add(generator);
            game.combatX = generator.x;
            game.combatY = generator.y;
            StructuralScaleCombatAuthority.Result generatorHit =
                    StructuralScaleCombatAuthority.confirm(game);
            int generatorIntegrity = parse(MapObjectState.stockValue(
                    generator.stockState, "structuralIntegrity"), -1);
            require(generatorHit.changed() && generatorIntegrity > 0
                            && generatorIntegrity < 180
                            && !MapObjectState.stockValue(generator.stockState,
                            "structuralDamageHistory").isBlank(),
                    "durable map fixture should persist structural integrity and bounded damage history");

            game.combatX = 12;
            game.combatY = 5;
            int emptyTurn = game.turn;
            int emptyAmmo = game.loadedWeaponShots.get("Arc Rifle");
            StructuralScaleCombatAuthority.Result empty =
                    StructuralScaleCombatAuthority.confirm(game);
            require(!empty.success() && !empty.changed()
                            && game.turn == emptyTurn
                            && game.loadedWeaponShots.get("Arc Rifle")
                            == emptyAmmo,
                    "empty target refusal must not spend ammunition, time, or target state");

            equip(game, "Scrap knife", 0, 0);
            game.world.tiles[14][5] = '#';
            game.combatX = 14;
            game.combatY = 5;
            int rangeTurn = game.turn;
            StructuralScaleCombatAuthority.Result outOfRange =
                    StructuralScaleCombatAuthority.confirm(game);
            require(!outOfRange.success() && game.turn == rangeTurn
                            && game.world.tiles[14][5] == '#',
                    "out-of-range melee refusal must not advance time or damage the wall");

            equip(game, "Light Rifle", 0, 0);
            game.combatX = car.x;
            game.combatY = car.y;
            int dryTurn = game.turn;
            int dryIntegrity = component(car,
                    VehicleRuntimeAuthority.Component.ARMOR);
            StructuralScaleCombatAuthority.Result dry =
                    StructuralScaleCombatAuthority.confirm(game);
            require(!dry.success() && game.turn == dryTurn
                            && component(car,
                            VehicleRuntimeAuthority.Component.ARMOR)
                            == dryIntegrity,
                    "out-of-ammunition refusal must not advance time or mutate vehicle integrity");

            equip(game, "Light Rifle", 6, 2);
            game.combatX = car.x;
            game.combatY = car.y;
            int burstBefore = game.loadedWeaponShots.get("Light Rifle");
            StructuralScaleCombatAuthority.Result burst =
                    StructuralScaleCombatAuthority.confirm(game);
            require(burst.success() && burst.ammunitionSpent() == 3
                            && game.loadedWeaponShots.get("Light Rifle")
                            == burstBefore - 3,
                    "burst structural attack should consume exactly three loaded shots");

            List<String> playerText = List.of(
                    knife.message(), rifle.message(), antiArmor.message(),
                    pistolWall.message(), firstWallHit.message(),
                    machineHit.message(), generatorHit.message(),
                    StructuralScaleCombatAuthority.preview(game).summary());
            for (String line : playerText) {
                require(!PlayerFacingText.containsLikelyLeak(line),
                        "structural combat text leaked implementation detail: "
                                + line);
            }

            System.out.println("Milestone 06 structural-scale combat smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static World world() {
        World world = new World(61039L, 26, 12);
        world.zoneType = ZoneType.MECHANICUS_FORGE_CLOISTER;
        world.sectorX = 5;
        world.sectorY = 1;
        world.zoneX = 2;
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
                                          String type, long seed) {
        RoadTransitFixtureAuthority.VehicleProfile profile =
                new RoadTransitFixtureAuthority.VehicleProfile(type,
                        RoadTransitFixtureAuthority.labelForType(type,
                                world.zoneType, new Random(seed)),
                        RoadTransitFixtureAuthority.stockForType(type,
                                world.tiles[x][y]));
        MapObjectState vehicle = RoadTransitFixtureAuthority.newVehicleFixture(
                world, x, y, profile, new Random(seed),
                "STRUCTURAL-COMBAT-SMOKE");
        VehicleRuntimeAuthority.initialize(world, vehicle, Faction.NOBLE,
                "faction", "structural target", false, new Random(seed));
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerType", VehicleRuntimeAuthority.OwnerType.FACTION.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerFaction", Faction.NOBLE.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerName", Faction.NOBLE.label);
        return vehicle;
    }

    private static MapObjectState durableObject(int x, int y,
                                                String type, String label) {
        MapObjectState object = new MapObjectState();
        object.id = "STRUCTURAL-OBJECT-" + x + "-" + y;
        object.type = type;
        object.label = label;
        object.glyph = 'M';
        object.x = x;
        object.y = y;
        object.stockState = "";
        return object;
    }

    private static void equip(GamePanel game, String weapon,
                              int loadedShots, int fireMode) {
        game.equippedLeftHandItem = "LEFT EMPTY";
        game.equippedRightHandItem = weapon;
        game.activeWeaponHandIndex = 1;
        game.selectedFireModeIndex = fireMode;
        if (loadedShots >= 0) game.loadedWeaponShots.put(weapon, loadedShots);
    }

    private static int component(MapObjectState vehicle,
                                 VehicleRuntimeAuthority.Component component) {
        String key = "vehicleComponent" + component.name().charAt(0)
                + component.name().substring(1).toLowerCase(java.util.Locale.ROOT);
        return parse(MapObjectState.stockValue(vehicle.stockState, key), -1);
    }

    private static int parse(String value, int fallback) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static void requireContains(String value, String expected,
                                        String label) {
        require(value != null && value.toLowerCase().contains(
                        expected.toLowerCase()),
                "Expected " + label + " to contain '" + expected
                        + "': " + value);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone06StructuralScaleCombatSmoke() { }
}
