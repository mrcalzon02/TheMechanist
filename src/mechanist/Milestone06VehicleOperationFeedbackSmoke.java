package mechanist;

import java.util.ArrayList;
import java.util.Random;

/** Focused smoke for vehicle pulse, headlights, audio profiles, and transient cleanup. */
final class Milestone06VehicleOperationFeedbackSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            game.playerX = 2;
            game.playerY = 5;
            game.turn = 810;
            game.worldTurn = 810L;
            VehicleOperationFeedbackAuthority.clearTransientFeedback();

            MapObjectState car = vehicle(game.world, 6, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    61L);
            game.world.mapObjects.add(car);

            VehicleOperationFeedbackAuthority.Feedback started =
                    VehicleOperationFeedbackAuthority.begin(game, car, 12,
                            6, 5, 18, 5, 1, 0, 1_000L, false);
            require("running".equals(started.state())
                            && started.headlights()
                            && started.headlightRange() >= 3
                            && !started.soundCue().isBlank(),
                    "vehicle start feedback should expose running, lighting, and sound state");
            require("running".equals(MapObjectState.stockValue(
                            car.stockState, "operationState"))
                            && "true".equals(MapObjectState.stockValue(
                            car.stockState, "headlightsActive")),
                    "vehicle start feedback should write truthful persistent operation flags");

            VehicleOperationFeedbackAuthority.VisualState active =
                    VehicleOperationFeedbackAuthority.visualState(game, car, 1_100L);
            require(active.visible() && active.activelyOperating()
                            && active.headlightsVisible()
                            && active.facingDx() == 1 && active.facingDy() == 0
                            && active.pulseAlpha() > 0
                            && active.pulseScale() > 0.85
                            && active.pulseScale() < 1.15,
                    "active vehicle should expose a bounded pulse and forward headlights: "
                            + active.summary());

            ArrayList<WorldLightEmitterAuthority.Emitter> gameplay =
                    WorldLightEmitterAuthority.gameplayEmitters(game, 1_100L);
            require(hasEmitter(gameplay, "vehicle-operation-pulse-", 6, 5),
                    "gameplay lighting should consume the vehicle operation pulse");
            require(hasForwardHeadlight(gameplay, 6, 5),
                    "gameplay lighting should consume forward vehicle headlight emitters");
            ArrayList<WorldLightEmitterAuthority.Emitter> viewport =
                    WorldLightEmitterAuthority.viewportEmitters(game,
                            0, 0, 15, 12, 1_100L);
            require(hasEmitter(viewport, "vehicle-operation-pulse-", 6, 5)
                            && hasForwardHeadlight(viewport, 6, 5),
                    "viewport bloom should use the same operation and headlight state");

            VehicleRuntimeAuthority.applyDamage(car,
                    VehicleRuntimeAuthority.Component.LIGHTS, 100,
                    game.turn, "feedback smoke shattered lights");
            VehicleOperationFeedbackAuthority.VisualState dark =
                    VehicleOperationFeedbackAuthority.visualState(game, car, 1_150L);
            require(dark.visible() && dark.degraded() && !dark.headlightsVisible()
                            && dark.headlightRange() == 0,
                    "destroyed lights should preserve degraded pulse feedback but remove the cone");
            ArrayList<WorldLightEmitterAuthority.Emitter> darkEmitters =
                    WorldLightEmitterAuthority.gameplayEmitters(game, 1_150L);
            require(hasEmitter(darkEmitters, "vehicle-operation-pulse-", 6, 5)
                            && !hasPrefix(darkEmitters, "vehicle-headlight-"),
                    "destroyed lights must remove headlight emitters without hiding operation state");

            VehicleRuntimeAuthority.applyDamage(car,
                    VehicleRuntimeAuthority.Component.POWERPLANT, 75,
                    game.turn, "feedback smoke damaged engine");
            require("ambient_sparks".equals(
                            VehicleOperationFeedbackAuthority.soundCue(car)),
                    "severely damaged powerplant should select the degraded sound cue");

            VehicleOperationFeedbackAuthority.Feedback stopped =
                    VehicleOperationFeedbackAuthority.complete(game, car, 12,
                            6, 5, 18, 5, 1, 0, 2_000L, false);
            require("parked".equals(stopped.state())
                            && "parked".equals(MapObjectState.stockValue(
                            car.stockState, "operationState"))
                            && "false".equals(MapObjectState.stockValue(
                            car.stockState, "headlightsActive"))
                            && "recently-parked".equals(MapObjectState.stockValue(
                            car.stockState, "operationFeedback")),
                    "completed operation should stop persistent running and headlight state");
            VehicleOperationFeedbackAuthority.VisualState arrival =
                    VehicleOperationFeedbackAuthority.visualState(game, car, 2_100L);
            require(arrival.visible() && arrival.recentlyParked()
                            && arrival.pulseAlpha() > 0,
                    "atomic transit should retain a short bounded arrival pulse");
            require(!VehicleOperationFeedbackAuthority.visualState(
                            game, car, 5_000L).visible()
                            && VehicleOperationFeedbackAuthority.activeSessionCount(
                            5_000L) == 0,
                    "transient feedback must expire and cannot become stale save/load state");

            MapObjectState bike = vehicle(game.world, 8, 8,
                    AssetIntegrationDisciplineAuthority.PARKED_UTILITY_BIKE,
                    62L);
            MapObjectState tank = vehicle(game.world, 10, 8,
                    AssetIntegrationDisciplineAuthority.PARKED_TANK,
                    63L);
            require(VehicleOperationFeedbackAuthority.headlightRange(tank)
                            > VehicleOperationFeedbackAuthority.headlightRange(bike),
                    "heavy vehicle headlights should project farther than bike lamps");
            require(!VehicleOperationFeedbackAuthority.soundCue(bike).equals(
                            VehicleOperationFeedbackAuthority.soundCue(tank)),
                    "light and heavy vehicle classes should not share one identical sound profile");

            for (String line : VehicleTransitAuthority.inspectionLines(game, car)) {
                require(!PlayerFacingText.containsLikelyLeak(line),
                        "vehicle operation inspection leaked implementation text: " + line);
            }
            System.out.println("Milestone 06 vehicle operation feedback smoke passed.");
        } finally {
            VehicleOperationFeedbackAuthority.clearTransientFeedback();
            game.shutdownRuntime();
        }
    }

    private static World world() {
        World world = new World(61011L, 24, 16);
        world.zoneType = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
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
                "VEHICLE-FEEDBACK-SMOKE");
        VehicleRuntimeAuthority.initialize(world, vehicle, Faction.NONE,
                "player-owned", "operation-feedback", false,
                new Random(seed));
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerType", VehicleRuntimeAuthority.OwnerType.PLAYER.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerName", "Player");
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownership", "player-owned");
        return vehicle;
    }

    private static boolean hasEmitter(
            ArrayList<WorldLightEmitterAuthority.Emitter> emitters,
            String prefix, int x, int y) {
        for (WorldLightEmitterAuthority.Emitter emitter : emitters) {
            if (emitter != null && emitter.id.startsWith(prefix)
                    && emitter.x == x && emitter.y == y) return true;
        }
        return false;
    }

    private static boolean hasForwardHeadlight(
            ArrayList<WorldLightEmitterAuthority.Emitter> emitters,
            int vehicleX, int vehicleY) {
        for (WorldLightEmitterAuthority.Emitter emitter : emitters) {
            if (emitter != null && emitter.id.startsWith("vehicle-headlight-")
                    && emitter.x > vehicleX && emitter.y == vehicleY) return true;
        }
        return false;
    }

    private static boolean hasPrefix(
            ArrayList<WorldLightEmitterAuthority.Emitter> emitters,
            String prefix) {
        for (WorldLightEmitterAuthority.Emitter emitter : emitters) {
            if (emitter != null && emitter.id.startsWith(prefix)) return true;
        }
        return false;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone06VehicleOperationFeedbackSmoke() { }
}
