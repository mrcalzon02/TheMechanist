package mechanist;

import java.awt.Rectangle;
import java.util.List;
import java.util.Random;

/** Places existing structural targets in the isolated arena; does not synthesize firing AI. */
final class DeveloperCombatFixtureAuthority {
    record Target(String id, String label, char symbol, VehicleRuntimeAuthority.VehicleClass vehicle) { }
    static final List<Target> TARGETS = List.of(
            new Target("suppression", "Arbites Suppression Turret", 'J', null),
            new Target("stub", "Heavy Stub Turret", 'U', null),
            new Target("sentry", "Noble Sentry Turret", 'Z', null),
            new Target("powered", "Powered Defense Turret", 'T', null),
            new Target("bike", "Utility bike", 'o', VehicleRuntimeAuthority.VehicleClass.UTILITY_BIKE),
            new Target("car", "Civilian car", 'o', VehicleRuntimeAuthority.VehicleClass.CIVILIAN_CAR),
            new Target("truck", "Cargo truck", 'o', VehicleRuntimeAuthority.VehicleClass.CARGO_TRUCK),
            new Target("armored-car", "Armored car", 'o', VehicleRuntimeAuthority.VehicleClass.ARMORED_CAR),
            new Target("tank", "Armored crawler", 'o', VehicleRuntimeAuthority.VehicleClass.TANK));

    static boolean valid(String selection) { return List.of("none", "turrets", "vehicles", "all").contains(selection); }

    static void scatter(GamePanel game) {
        clear(game);
        var layout = DeveloperFacilityAuthority.layout(game);
        Rectangle room = layout.rooms.get("combat");
        int placed = 0;
        for (Target target : TARGETS) {
            if (layout.fixtureSet.equals("none")
                    || layout.fixtureSet.equals("turrets") && target.vehicle() != null
                    || layout.fixtureSet.equals("vehicles") && target.vehicle() == null) continue;
            int x = room.x + 8 + (placed % 5) * 8, y = room.y + 26 + (placed / 5) * 6;
            placed++;
            if (game.world.tiles[x][y] != '.' || game.world.npcAt(x,y) != null
                    || game.world.mapObjectAt(x,y) != null || game.baseObjectAt(x,y) != null
                    || game.playerX == x && game.playerY == y) continue;
            create(game, target, x, y, layout.fixtureSeed);
            if (target.vehicle() == null) layout.combatFixtures.add(game.baseObjectAt(x,y));
            else layout.combatVehicles.add(game.world.mapObjectAt(x,y));
        }
        game.world.dirtyVisionRevision++;
        game.logEvent("Combat fixtures: " + layout.combatFixtures.size() + " passive turret mounts, "
                + layout.combatVehicles.size() + " vehicle targets; seed " + layout.fixtureSeed
                + ". Structural damage is active. Turret targeting and vehicle weapon fire are not implemented.");
    }

    static void create(GamePanel game, Target target, int x, int y, long seed) {
        if (target.vehicle() == null) {
            BaseObject object = new BaseObject(target.label(), target.symbol(), x, y, 0, 0);
            game.configureBaseObject(object);
            object.integrity = 100;
            game.baseObjects.add(object);
            game.world.tiles[x][y] = object.symbol;
            refresh(game, x, y);
        } else {
            Random random = new Random(seed ^ target.id().hashCode());
            String type = target.vehicle().type;
            var profile = new RoadTransitFixtureAuthority.VehicleProfile(type,
                    RoadTransitFixtureAuthority.labelForType(type, game.world.zoneType, random),
                    RoadTransitFixtureAuthority.stockForType(type, game.world.tiles[x][y]));
            MapObjectState object = RoadTransitFixtureAuthority.newVehicleFixture(game.world, x, y,
                    profile, random, "DEV-COMBAT-" + target.id());
            VehicleRuntimeAuthority.initialize(game.world, object, Faction.IMPERIAL_GUARD,
                    "faction", "combat test target", false, random);
            game.world.mapObjects.add(object);
        }
    }

    static void clear(GamePanel game) {
        var layout = DeveloperFacilityAuthority.layout(game);
        for (BaseObject object : layout.combatFixtures) {
            game.baseObjects.remove(object);
            if (game.world.tiles[object.x][object.y] == object.symbol) {
                game.world.tiles[object.x][object.y] = '.';
                refresh(game, object.x, object.y);
            }
        }
        game.world.mapObjects.removeAll(layout.combatVehicles);
        layout.combatFixtures.clear();
        layout.combatVehicles.clear();
    }

    private static void refresh(GamePanel game, int x, int y) {
        if (game.world.compiledTileDescriptors != null)
            game.world.compiledTileDescriptors[x][y] = TileDataCompilationAuthority.resolveFresh(
                    game.world, x, y, game.world.tiles[x][y]);
    }
    private DeveloperCombatFixtureAuthority() { }
}
