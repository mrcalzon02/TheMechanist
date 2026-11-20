package mechanist;

import java.awt.Point;
import java.util.List;
import java.util.Random;

/** Focused smoke for local road routing, legal parking, and manual-plan execution. */
final class Milestone06VehicleTransitSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            VehicleOperationFeedbackAuthority.clearTransientFeedback();
            game.world = world();
            game.playerX = 1;
            game.playerY = 1;
            game.turn = 700;
            game.worldTurn = 700L;
            MapObjectState vehicle = vehicle(game.world, 2, 4,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    VehicleRuntimeAuthority.OwnerType.PLAYER,
                    Faction.NONE, "player-owned", 31L);
            game.world.mapObjects.add(vehicle);
            VehicleFuelAuthority.Snapshot initialEnergy =
                    VehicleFuelAuthority.inspect(game.world, vehicle);
            require(initialEnergy.current() == 24
                            && VehicleFuelAuthority.routeEnergyCost(
                                    VehicleRuntimeAuthority.VehicleClass.CIVILIAN_CAR, 23) == 4
                            && VehicleFuelAuthority.routeEnergyCost(
                                    VehicleRuntimeAuthority.VehicleClass.CIVILIAN_CAR, 0) == 0
                            && VehicleFuelAuthority.routeEnergyCost(
                                    VehicleRuntimeAuthority.VehicleClass.UTILITY_BIKE, 23) == 3
                            && VehicleFuelAuthority.routeEnergyCost(
                                    VehicleRuntimeAuthority.VehicleClass.CARGO_TRUCK, 23) == 6
                            && VehicleFuelAuthority.routeEnergyCost(
                                    VehicleRuntimeAuthority.VehicleClass.ARMORED_CAR, 23) == 8
                            && VehicleFuelAuthority.routeEnergyCost(
                                    VehicleRuntimeAuthority.VehicleClass.TANK, 23) == 12,
                    "class-scaled shared local/strategic energy cost must remain deterministic");

            // Preview is a query, never a hidden state-changing registration.
            // Even a refused private title must preserve its exact persisted
            // condition and ownership flags.
            String originalStock = vehicle.stockState;
            vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                    "ownerType", VehicleRuntimeAuthority.OwnerType.PRIVATE.name());
            vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                    "condition", "unregistered-preview-sentinel");
            String refusedStock = vehicle.stockState;
            VehicleTransitAuthority.RoutePlan unauthorized =
                    VehicleTransitAuthority.plan(game, vehicle, 25, 4);
            require(!unauthorized.valid()
                            && "vehicle-access-denied".equals(unauthorized.blocker())
                            && refusedStock.equals(vehicle.stockState),
                    "unauthorized route preview must not rewrite ownership or condition");
            vehicle.stockState = originalStock;

            vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                    "condition", "preview-should-not-recompute");
            String previewStock = vehicle.stockState;
            VehicleTransitAuthority.RoutePlan safePreview =
                    VehicleTransitAuthority.plan(game, vehicle, 25, 4);
            require(safePreview.valid() && safePreview.steps() == 23
                            && previewStock.equals(vehicle.stockState),
                    "valid route preview must not rewrite cached condition or vehicle history");
            vehicle.stockState = originalStock;

            vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                    "vehicleSchema", "pre-schema");
            String legacyStock = vehicle.stockState;
            VehicleTransitAuthority.RoutePlan legacyPreview =
                    VehicleTransitAuthority.plan(game, vehicle, 25, 4);
            require(legacyPreview.valid() && legacyPreview.steps() == 23
                            && legacyStock.equals(vehicle.stockState),
                    "legacy title preview must preserve schema while matching registered ownership");
            vehicle.stockState = originalStock;

            vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                    "vehicleComponentPowerplant", "0");
            String disabledStock = vehicle.stockState;
            VehicleTransitAuthority.RoutePlan disabledRoutePreview =
                    VehicleTransitAuthority.plan(game, vehicle, 25, 4);
            require(!disabledRoutePreview.valid()
                            && "vehicle-not-operational".equals(disabledRoutePreview.blocker())
                            && disabledStock.equals(vehicle.stockState),
                    "disabled vehicle refusal must not recompute condition or mutate components");
            vehicle.stockState = originalStock;

            // The closest legal parking tile may be disconnected. Choose a
            // reachable one nearby rather than refusing an otherwise valid trip.
            game.world.tiles[18][6] = RoadGridIntegrationAuthority.PARKING_SPACE;
            game.world.tiles[18][4] = RoadGridIntegrationAuthority.PARKING_SPACE;
            VehicleTransitAuthority.RoutePlan alternate =
                    VehicleTransitAuthority.plan(game, vehicle, 18, 6);
            require(alternate.valid()
                            && alternate.resolvedParking().equals(new Point(18, 4))
                            && alternate.path().get(alternate.path().size() - 1)
                                    .equals(new Point(18, 4)),
                    "disconnected closest parking must not hide reachable alternative: "
                            + alternate.summary());
            requireContains(alternate.summary(), "near requested 18,6",
                    "alternate parking explanation");
            game.world.tiles[18][6] = '#';
            game.world.tiles[18][4] = RoadGridIntegrationAuthority.ROAD_LANE;

            // An already-parked route must not produce fake engine activity.
            String beforeNoOp = vehicle.stockState;
            int beforeNoOpTurn = game.turn;
            VehicleTransitAuthority.RoutePlan parked =
                    VehicleTransitAuthority.plan(game, vehicle, 2, 4);
            require(parked.valid() && parked.steps() == 0, "same-cell parking preview");
            VehicleTransitAuthority.TransitResult noOp =
                    VehicleTransitAuthority.execute(game, vehicle, parked);
            require(noOp.success() && !noOp.changed() && noOp.steps() == 0
                            && game.turn == beforeNoOpTurn
                            && vehicle.stockState.equals(beforeNoOp)
                            && !VehicleOperationFeedbackAuthority.visualState(game, vehicle,
                                    System.currentTimeMillis()).visible(),
                    "same-cell vehicle route must not start an engine, spend a turn, "
                            + "write history, or emit transient operation feedback");

            // Road movement may never cross the player's actual tile or
            // park on the player. Blocking a route must be side-effect free.
            String playerBlockStock = vehicle.stockState;
            int playerBlockTurn = game.turn;
            game.playerX = 13;
            game.playerY = 4;
            VehicleTransitAuthority.RoutePlan playerInLane =
                    VehicleTransitAuthority.plan(game, vehicle, 25, 4);
            require(!playerInLane.valid()
                            && "no-road-route".equals(playerInLane.blocker())
                            && playerInLane.summary().contains("player")
                            && game.turn == playerBlockTurn
                            && playerBlockStock.equals(vehicle.stockState),
                    "player standing on the only road must prevent a trip without cost");
            game.playerX = 25;
            game.playerY = 4;
            VehicleTransitAuthority.RoutePlan playerOnParking =
                    VehicleTransitAuthority.plan(game, vehicle, 25, 4);
            require(!playerOnParking.valid()
                            && "no-legal-parking".equals(playerOnParking.blocker())
                            && playerOnParking.summary().contains("occupied")
                            && playerBlockStock.equals(vehicle.stockState),
                    "player standing on the destination must prevent parking");
            game.playerX = vehicle.x;
            game.playerY = vehicle.y;
            VehicleTransitAuthority.RoutePlan sharedStart =
                    VehicleTransitAuthority.plan(game, vehicle, vehicle.x, vehicle.y);
            require(sharedStart.valid() && sharedStart.steps() == 0
                            && VehicleTransitAuthority.execute(game, vehicle, sharedStart).success()
                            && game.turn == playerBlockTurn
                            && playerBlockStock.equals(vehicle.stockState),
                    "shared vehicle/player origin must preserve no-cost zero-step inspection");
            game.playerX = 1;
            game.playerY = 1;

            VehicleTransitAuthority.RoutePlan plan =
                    VehicleTransitAuthority.plan(game, vehicle, 25, 4);
            require(plan.valid() && plan.steps() == 23
                            && plan.resolvedParking().equals(new Point(25, 4)),
                    "contiguous road route should reach the marked destination parking cell: "
                            + plan.summary());
            require(contiguous(plan.path()),
                    "vehicle route preview must be a contiguous one-tile path");
            requireContains(plan.summary(), "Fuel or power: 4 unit(s) required",
                    "local route energy preview");
            // UI consumers may retain a cursor Point or mutate a rendered
            // path Point. Neither can edit the geometry already approved for
            // this exact vehicle/world, nor can a caller append new steps.
            String beforePreviewGeometry = vehicle.stockState;
            List<Point> renderedPath = plan.path();
            Point renderedRequest = plan.requestedTarget();
            Point renderedParking = plan.resolvedParking();
            renderedRequest.translate(-20, 5);
            renderedParking.translate(20, -5);
            renderedPath.get(0).translate(20, 5);
            renderedPath.get(renderedPath.size() - 1).translate(-20, -5);
            boolean listIsFixed = false;
            try {
                renderedPath.add(new Point(3, 4));
            } catch (UnsupportedOperationException expected) {
                listIsFixed = true;
            }
            require(listIsFixed
                            && plan.requestedTarget().equals(new Point(25, 4))
                            && plan.resolvedParking().equals(new Point(25, 4))
                            && plan.path().get(0).equals(new Point(2, 4))
                            && plan.path().get(plan.path().size() - 1)
                                    .equals(new Point(25, 4))
                            && plan.steps() == 23
                            && beforePreviewGeometry.equals(vehicle.stockState),
                    "external route-preview Point/list changes must never alter the confirmed path");
            // A valid preview authorizes exactly this physical vehicle in
            // this loaded world, not another car sharing the same start tile.
            MapObjectState otherCar = vehicle(game.world, 2, 4,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    VehicleRuntimeAuthority.OwnerType.PLAYER,
                    Faction.NONE, "player-owned", 61015L);
            game.world.mapObjects.add(otherCar);
            String otherBefore = otherCar.stockState;
            String selectedBefore = vehicle.stockState;
            int beforeWrongFixtureTurn = game.turn;
            VehicleTransitAuthority.TransitResult wrongFixture =
                    VehicleTransitAuthority.execute(game, otherCar, plan);
            require(!wrongFixture.success()
                            && "route-state-changed".equals(wrongFixture.plan().blocker())
                            && otherCar.x == 2 && otherCar.y == 4
                            && otherBefore.equals(otherCar.stockState)
                            && selectedBefore.equals(vehicle.stockState)
                            && game.turn == beforeWrongFixtureTurn,
                    "a route preview for one vehicle cannot move a different co-located vehicle");
            game.world.mapObjects.remove(otherCar);

            // A loaded-world switch also invalidates the preview, even if
            // the same fixture appears on identical road geometry.
            World originalRouteWorld = game.world;
            World replacementRouteWorld = world();
            replacementRouteWorld.mapObjects.add(vehicle);
            game.world = replacementRouteWorld;
            VehicleTransitAuthority.TransitResult wrongWorld =
                    VehicleTransitAuthority.execute(game, vehicle, plan);
            game.world = originalRouteWorld;
            require(!wrongWorld.success()
                            && "route-state-changed".equals(wrongWorld.plan().blocker())
                            && vehicle.x == 2 && vehicle.y == 4
                            && selectedBefore.equals(vehicle.stockState)
                            && game.turn == beforeWrongFixtureTurn,
                    "a route preview cannot be replayed against a replacement world");

            // A player who moves into an already approved route must block
            // its final commit before energy, history or position changes.
            game.playerX = 13;
            game.playerY = 4;
            String beforeStalePlayer = vehicle.stockState;
            int beforeStaleTurn = game.turn;
            VehicleTransitAuthority.TransitResult stalePlayer =
                    VehicleTransitAuthority.execute(game, vehicle, plan);
            require(!stalePlayer.success()
                            && "no-road-route".equals(stalePlayer.plan().blocker())
                            && vehicle.x == 2 && vehicle.y == 4
                            && game.playerX == 13 && game.playerY == 4
                            && beforeStalePlayer.equals(vehicle.stockState)
                            && game.turn == beforeStaleTurn
                            && VehicleFuelAuthority.inspect(game.world, vehicle).current() == 24,
                    "a player entering the road after preview must block commit without spending fuel");
            game.playerX = 1;
            game.playerY = 1;
            int playerX = game.playerX;
            int playerY = game.playerY;
            VehicleTransitAuthority.TransitResult moved =
                    VehicleTransitAuthority.execute(game, vehicle, plan);
            require(moved.success() && moved.changed()
                            && moved.fromX() == 2 && moved.fromY() == 4
                            && moved.toX() == 25 && moved.toY() == 4
                            && vehicle.x == 25 && vehicle.y == 4,
                    "validated route should move the physical vehicle fixture to legal parking");
            require(game.playerX == playerX && game.playerY == playerY,
                    "vehicle transit must not silently teleport the player actor");
            require(VehicleFuelAuthority.inspect(game.world, vehicle).current() == 20,
                    "committed 23-tile car route must debit four energy units exactly once");
            requireContains(moved.message(), "consumed 4 unit(s)",
                    "fuel readback after local travel");
            require("parked".equals(MapObjectState.stockValue(
                            vehicle.stockState, "operationState"))
                            && "false".equals(MapObjectState.stockValue(
                            vehicle.stockState, "headlightsActive"))
                            && "recently-parked".equals(MapObjectState.stockValue(
                            vehicle.stockState, "operationFeedback")),
                    "completed route should stop persistent running state while retaining bounded arrival feedback");
            VehicleOperationFeedbackAuthority.VisualState arrival =
                    VehicleOperationFeedbackAuthority.visualState(
                            game, vehicle, System.currentTimeMillis());
            require(arrival.visible() && arrival.recentlyParked()
                            && arrival.pulseAlpha() > 0,
                    "completed atomic route should expose a short operation-arrival pulse");
            requireContains(moved.message(), "bounded and transient",
                    "vehicle transit feedback summary");
            requireContains(MapObjectState.stockValue(vehicle.stockState, "lastRoute"),
                    "2,4", "vehicle route origin history");
            requireContains(MapObjectState.stockValue(vehicle.stockState, "lastRoute"),
                    "25,4", "vehicle route destination history");
            requireContains(MapObjectState.stockValue(
                            vehicle.stockState, "deploymentHistory"),
                    "Local road route", "vehicle deployment history");

            // A clear sidewalk immediately beside the road is a legal resolved
            // parking target but never becomes an intermediate through-route.
            game.world.tiles[22][3] = RoadGridIntegrationAuthority.SIDEWALK;
            VehicleTransitAuthority.RoutePlan curb =
                    VehicleTransitAuthority.plan(game, vehicle, 22, 3);
            require(curb.valid() && curb.resolvedParking().equals(new Point(22, 3))
                            && curb.path().get(curb.path().size() - 1)
                            .equals(new Point(22, 3)),
                    "clear curb-adjacent sidewalk should resolve as final parking");
            int sidewalkCount = 0;
            for (int i = 0; i < curb.path().size(); i++) {
                Point point = curb.path().get(i);
                if (game.world.tiles[point.x][point.y]
                        == RoadGridIntegrationAuthority.SIDEWALK) sidewalkCount++;
            }
            require(sidewalkCount == 1,
                    "sidewalk may be the final parking cell but not a vehicle through-route");

            VehicleTransitAuthority.RoutePlan remoteOffRoad =
                    VehicleTransitAuthority.plan(game, vehicle, 27, 12);
            require(!remoteOffRoad.valid()
                            && "no-legal-parking".equals(remoteOffRoad.blocker()),
                    "remote off-road target should fail without a nearby legal parking cell");

            // Return to the west parking cell before obstacle tests.
            VehicleTransitAuthority.RoutePlan west =
                    VehicleTransitAuthority.plan(game, vehicle, 2, 4);
            require(west.valid(), "return route should be available");
            require(VehicleTransitAuthority.execute(game, vehicle, west).success(),
                    "return route should execute");
            require(VehicleFuelAuthority.inspect(game.world, vehicle).current() == 16,
                    "second committed route should debit its own four energy units");

            VehicleTransitAuthority.RoutePlan stale =
                    VehicleTransitAuthority.plan(game, vehicle, 25, 4);
            require(stale.valid(), "stale-route fixture should begin valid");
            MapObjectState obstruction = new MapObjectState();
            obstruction.id = "VEHICLE-ROUTE-OBSTRUCTION";
            obstruction.type = "road-work-obstruction";
            obstruction.label = "Road works barrier";
            obstruction.glyph = 'X';
            obstruction.x = 13;
            obstruction.y = 4;
            obstruction.stockState = "route-blocking=true";
            game.world.mapObjects.add(obstruction);
            VehicleTransitAuthority.TransitResult staleResult =
                    VehicleTransitAuthority.execute(game, vehicle, stale);
            require(!staleResult.success()
                            && vehicle.x == 2 && vehicle.y == 4
                            && ("route-state-changed".equals(staleResult.plan().blocker())
                            || "no-road-route".equals(staleResult.plan().blocker())),
                    "changed road occupancy must invalidate preview before movement");
            require(!VehicleTransitAuthority.plan(game, vehicle, 25, 4).valid(),
                    "single-lane route should remain blocked while the obstacle exists");
            game.world.mapObjects.remove(obstruction);

            // The real player interaction must respect vehicle cursor range
            // independently of an empty actor movement path. First test a
            // missing target and an illegal target: both stay read-only.
            game.playerX = 2;
            game.playerY = 3;
            game.options.soundEnabled = false;
            game.manualMovementPlanActive = true;
            game.lookCursorActive = false;
            game.lookX = 25;
            game.lookY = 4; // stale coordinates must not become a hidden command
            game.manualMovementPlanPath.clear();
            String beforeCursorless = vehicle.stockState;
            int beforeCursorTurn = game.turn;
            int beforeCursorXp = game.xp;
            int beforeCursorVend = vehicle.vendCount;
            require(RoadTransitFixtureInteractionAuthority.tryInteract(game, 2, 4),
                    "a planned vehicle action should be handled even without an actor path");
            require(game.turn == beforeCursorTurn && game.xp == beforeCursorXp
                            && vehicle.vendCount == beforeCursorVend
                            && game.manualMovementPlanActive
                            && vehicle.x == 2 && vehicle.y == 4
                            && beforeCursorless.equals(vehicle.stockState),
                    "missing vehicle destination must not use stale cursor, turn, XP, fuel or history");
            game.lookCursorActive = true;
            game.lookX = 27;
            game.lookY = 12;
            require(RoadTransitFixtureInteractionAuthority.tryInteract(game, 2, 4),
                    "off-road destination must return an actionable refusal");
            require(game.turn == beforeCursorTurn && game.xp == beforeCursorXp
                            && vehicle.vendCount == beforeCursorVend
                            && vehicle.x == 2 && vehicle.y == 4
                            && game.manualMovementPlanActive
                            && beforeCursorless.equals(vehicle.stockState),
                    "invalid cursor-only route must not spend time/fuel or discard the pending plan");
            game.lookX = 25;
            game.lookY = 4;
            int manualTurn = game.turn, manualXp = game.xp;
            long manualWorldTurn = game.worldTurn;
            int manualVend = vehicle.vendCount;
            require(RoadTransitFixtureInteractionAuthority.tryInteract(game, 2, 4),
                    "real vehicle interaction should execute a cursor-only route without a walking path");
            require(vehicle.x == 25 && vehicle.y == 4
                            && !game.manualMovementPlanActive
                            && !game.lookCursorActive
                            && game.turn == manualTurn + 1
                            && game.worldTurn == manualWorldTurn + 1
                            && game.xp > manualXp
                            && vehicle.vendCount == manualVend + 1,
                    "cursor-only live interaction must drive once, award one turn, and clear planning");
            require(VehicleFuelAuthority.inspect(game.world, vehicle).current() == 12,
                    "live cursor-only vehicle movement must consume precisely four energy units");

            // A valid prior preview may no longer fit after energy changes.
            // The refetched plan must fail closed before any position or
            // transient operation state is changed.
            VehicleTransitAuthority.RoutePlan fueledPreview =
                    VehicleTransitAuthority.plan(game, vehicle, 2, 4);
            require(fueledPreview.valid(), "fixture must begin with enough fuel");
            String stockBeforeEnergyFailure = MapObjectState.setStockFlag(
                    vehicle.stockState, "fuelOrPowerCurrent", "0");
            vehicle.stockState = stockBeforeEnergyFailure;
            String historyBeforeFailure = MapObjectState.stockValue(
                    vehicle.stockState, "deploymentHistory");
            int tripStartX = vehicle.x, tripStartY = vehicle.y;
            VehicleTransitAuthority.TransitResult noFuel =
                    VehicleTransitAuthority.execute(game, vehicle, fueledPreview);
            require(!noFuel.success()
                            && "insufficient-energy".equals(noFuel.plan().blocker())
                            && tripStartX == vehicle.x && tripStartY == vehicle.y
                            && stockBeforeEnergyFailure.equals(vehicle.stockState)
                            && historyBeforeFailure.equals(MapObjectState.stockValue(
                                    vehicle.stockState, "deploymentHistory")),
                    "empty-fuel refusal must not change position, history, or vehicle state");
            require(!VehicleTransitAuthority.plan(game, vehicle, 2, 4).valid(),
                    "fuel-aware preview must refuse a real zero-energy route");
            // Zero-distance inspection must still work without fuel.
            VehicleTransitAuthority.RoutePlan emptyParked =
                    VehicleTransitAuthority.plan(game, vehicle, 25, 4);
            require(emptyParked.valid() && emptyParked.steps() == 0
                            && VehicleTransitAuthority.execute(game, vehicle, emptyParked).success()
                            && stockBeforeEnergyFailure.equals(vehicle.stockState),
                    "zero-step parking must remain free even with empty reserves");
            vehicle.stockState = MapObjectState.setStockFlag(
                    vehicle.stockState, "fuelOrPowerCurrent", "12");
            vehicle.stockState = MapObjectState.setStockFlag(
                    vehicle.stockState, "strategicTransitFuelReserved", "10");
            String reservedState = vehicle.stockState;
            VehicleTransitAuthority.RoutePlan reserved =
                    VehicleTransitAuthority.plan(game, vehicle, 2, 4);
            require(!reserved.valid() && "insufficient-energy".equals(reserved.blocker())
                            && reserved.summary().contains("10 reserved")
                            && vehicle.stockState.equals(reservedState),
                    "strategic fuel reservations must prevent local travel without mutation");
            vehicle.stockState = MapObjectState.setStockFlag(
                    vehicle.stockState, "strategicTransitFuelReserved", "0");

            VehicleRuntimeAuthority.applyDamage(vehicle,
                    VehicleRuntimeAuthority.Component.MOBILITY, 100,
                    game.turn, "smoke disabled wheels");
            VehicleTransitAuthority.RoutePlan disabled =
                    VehicleTransitAuthority.plan(game, vehicle, 2, 4);
            require(!disabled.valid()
                            && ("vehicle-not-operational".equals(disabled.blocker())
                            || "vehicle-mobility-failure".equals(disabled.blocker())),
                    "disabled mobility assembly must block vehicle routing");

            MapObjectState factionTruck = vehicle(game.world, 2, 4,
                    AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK,
                    VehicleRuntimeAuthority.OwnerType.FACTION,
                    Faction.NOBLE, "faction", 32L);
            game.world.mapObjects.add(factionTruck);
            VehicleTransitAuthority.RoutePlan denied =
                    VehicleTransitAuthority.plan(game, factionTruck, 25, 4);
            require(!denied.valid()
                            && "vehicle-access-denied".equals(denied.blocker()),
                    "player route command must not operate faction-owned vehicles");

            List<String> inspection =
                    VehicleTransitAuthority.inspectionLines(game, vehicle);
            requireContains(inspection, "roads and marked parking",
                    "vehicle transit rule inspection");
            requireContains(inspection, "manual movement plan",
                    "vehicle transit control inspection");
            requireContains(inspection, "No walking path is required",
                    "vehicle route-range separation inspection");
            requireContains(inspection, "bounded pulse",
                    "vehicle operation feedback inspection");
            requireContains(inspection, "Travel energy:",
                    "local route fuel and reservation guidance");
            for (String line : inspection) {
                require(!PlayerFacingText.containsLikelyLeak(line),
                        "vehicle transit inspection leaked implementation text: " + line);
            }

            verifyBoundedRoadSearch(game);
            verifyFuelAwareParkingChoice(game);
            System.out.println("Milestone 06 vehicle transit smoke passed.");
        } finally {
            VehicleOperationFeedbackAuthority.clearTransientFeedback();
            game.shutdownRuntime();
        }
    }

    /** Distinguish an exactly allowed route, over-range detour and road break. */
    private static void verifyBoundedRoadSearch(GamePanel game) {
        World longWorld = new World(61010L, 132, 15);
        longWorld.zoneType = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        longWorld.mapObjects.clear();
        longWorld.npcs.clear();
        for (char[] column : longWorld.tiles) java.util.Arrays.fill(column, '#');
        longWorld.tiles[2][4] = RoadGridIntegrationAuthority.PARKING_SPACE;
        for (int x = 3; x < 125; x++)
            longWorld.tiles[x][4] = RoadGridIntegrationAuthority.ROAD_LANE;
        longWorld.tiles[50][4] = RoadGridIntegrationAuthority.PARKING_SPACE;
        longWorld.tiles[125][4] = RoadGridIntegrationAuthority.PARKING_SPACE;
        MapObjectState car = vehicle(longWorld, 2, 4,
                AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                VehicleRuntimeAuthority.OwnerType.PLAYER,
                Faction.NONE, "player-owned", 61010L);
        longWorld.mapObjects.add(car);
        game.world = longWorld;
        VehicleTransitAuthority.RoutePlan exactLimit =
                VehicleTransitAuthority.plan(game, car, 50, 4);
        require(exactLimit.valid() && exactLimit.steps()
                        == VehicleTransitAuthority.MAX_LOCAL_ROUTE_STEPS
                        && exactLimit.resolvedParking().equals(new Point(50, 4)),
                "an exactly 48-step road trip must remain valid");
        String beforeFar = car.stockState;
        VehicleTransitAuthority.RoutePlan far =
                VehicleTransitAuthority.plan(game, car, 125, 4);
        require(!far.valid() && "route-too-long".equals(far.blocker())
                        && far.summary().contains("at least")
                        && car.stockState.equals(beforeFar),
                "far parking must fail the distance bound without mutation");

        // Six Manhattan tiles separate these points, but the one-cell-wide
        // serpentine road needs more than 48 steps to reach the destination.
        World winding = new World(61011L, 30, 12);
        winding.zoneType = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        winding.mapObjects.clear();
        winding.npcs.clear();
        for (char[] column : winding.tiles) java.util.Arrays.fill(column, '#');
        for (int y : new int[]{2, 4, 6, 8})
            for (int x = 2; x <= 25; x++)
                winding.tiles[x][y] = RoadGridIntegrationAuthority.ROAD_LANE;
        winding.tiles[25][3] = RoadGridIntegrationAuthority.ROAD_LANE;
        winding.tiles[2][5] = RoadGridIntegrationAuthority.ROAD_LANE;
        winding.tiles[25][7] = RoadGridIntegrationAuthority.ROAD_LANE;
        winding.tiles[2][2] = RoadGridIntegrationAuthority.PARKING_SPACE;
        winding.tiles[2][8] = RoadGridIntegrationAuthority.PARKING_SPACE;
        MapObjectState snakeCar = vehicle(winding, 2, 2,
                AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                VehicleRuntimeAuthority.OwnerType.PLAYER,
                Faction.NONE, "player-owned", 61011L);
        winding.mapObjects.add(snakeCar);
        game.world = winding;
        String beforeWinding = snakeCar.stockState;
        VehicleFuelAuthority.Snapshot windingPreview =
                VehicleFuelAuthority.preview(snakeCar);
        require(windingPreview.capacity() > 0 && windingPreview.available() > 0
                        && snakeCar.stockState.equals(beforeWinding),
                "read-only energy preview must never initialize a route candidate");
        VehicleTransitAuthority.RoutePlan windingRoute =
                VehicleTransitAuthority.plan(game, snakeCar, 2, 8);
        require(!windingRoute.valid()
                        && "route-too-long".equals(windingRoute.blocker())
                        && windingRoute.summary().contains("48")
                        && snakeCar.stockState.equals(beforeWinding),
                "nearby destination requiring a long detour must remain outside local range");
        winding.tiles[25][3] = '#';
        VehicleTransitAuthority.RoutePlan severed =
                VehicleTransitAuthority.plan(game, snakeCar, 2, 8);
        require(!severed.valid() && "no-road-route".equals(severed.blocker()),
                "a short, disconnected road must report no-road-route");
    }

    /**
     * Requested parking takes a long detour, while a slightly farther bay can
     * be reached with the actual unreserved fuel. The route must choose the
     * affordable bay rather than reject a trip that is otherwise possible.
     */
    private static void verifyFuelAwareParkingChoice(GamePanel game) {
        World road = new World(61012L, 26, 13);
        road.zoneType = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        road.mapObjects.clear();
        road.npcs.clear();
        for (char[] column : road.tiles) java.util.Arrays.fill(column, '#');

        road.tiles[2][6] = RoadGridIntegrationAuthority.PARKING_SPACE;
        for (int x = 3; x <= 11; x++)
            road.tiles[x][6] = RoadGridIntegrationAuthority.ROAD_LANE;
        road.tiles[12][6] = RoadGridIntegrationAuthority.PARKING_SPACE;
        for (int y = 7; y <= 9; y++)
            road.tiles[2][y] = RoadGridIntegrationAuthority.ROAD_LANE;
        for (int x = 3; x <= 21; x++)
            road.tiles[x][9] = RoadGridIntegrationAuthority.ROAD_LANE;
        for (int y = 4; y <= 8; y++)
            road.tiles[21][y] = RoadGridIntegrationAuthority.ROAD_LANE;
        for (int x = 13; x <= 20; x++)
            road.tiles[x][4] = RoadGridIntegrationAuthority.ROAD_LANE;
        road.tiles[12][4] = RoadGridIntegrationAuthority.PARKING_SPACE;

        MapObjectState car = vehicle(road, 2, 6,
                AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                VehicleRuntimeAuthority.OwnerType.PLAYER,
                Faction.NONE, "player-owned", 61012L);
        road.mapObjects.add(car);
        game.world = road;
        game.playerX = 2;
        game.playerY = 5;
        game.options.soundEnabled = false;
        VehicleFuelAuthority.inspect(road, car);

        // With ample fuel, the exact requested bay is legal, but requires
        // the 36-step detour on the isolated road branch.
        VehicleTransitAuthority.RoutePlan fullFuel =
                VehicleTransitAuthority.plan(game, car, 12, 4);
        require(fullFuel.valid() && fullFuel.steps() == 36
                        && fullFuel.resolvedParking().equals(new Point(12, 4)),
                "unconstrained route should prefer the exact requested parking bay: "
                        + fullFuel.summary());

        // A 12-unit reserve leaves only one unit: even the ten-step
        // alternate bay is unaffordable. Refusal must not alter the ledger.
        car.stockState = MapObjectState.setStockFlag(
                car.stockState, "fuelOrPowerCurrent", "12");
        car.stockState = MapObjectState.setStockFlag(
                car.stockState, "strategicTransitFuelReserved", "11");
        String beforeRefusal = car.stockState;
        VehicleTransitAuthority.RoutePlan insufficient =
                VehicleTransitAuthority.plan(game, car, 12, 4);
        require(!insufficient.valid()
                        && "insufficient-energy".equals(insufficient.blocker())
                        && insufficient.summary().contains("11 reserved")
                        && beforeRefusal.equals(car.stockState),
                "one unreserved unit cannot reach either parking bay or mutate fuel");

        // Two unreserved units cover the nearby 10-step alternative, but
        // not the preferred 36-step bay. The same preview must execute
        // without consuming one of the 10 strategically reserved units.
        car.stockState = MapObjectState.setStockFlag(
                car.stockState, "strategicTransitFuelReserved", "10");
        String beforePreview = car.stockState;
        VehicleTransitAuthority.RoutePlan affordable =
                VehicleTransitAuthority.plan(game, car, 12, 4);
        require(affordable.valid() && affordable.steps() == 10
                        && affordable.resolvedParking().equals(new Point(12, 6))
                        && affordable.summary().contains("near requested 12,4")
                        && affordable.summary().contains("2 unit(s) required")
                        && affordable.summary().contains("10 reserved")
                        && beforePreview.equals(car.stockState),
                "fuel-aware preview must select reachable parking without mutation: "
                        + affordable.summary());
        int turnBefore = game.turn;
        VehicleTransitAuthority.TransitResult delivered =
                VehicleTransitAuthority.execute(game, car, affordable);
        VehicleFuelAuthority.Snapshot after = VehicleFuelAuthority.inspect(road, car);
        require(delivered.success() && delivered.changed()
                        && delivered.toX() == 12 && delivered.toY() == 6
                        && after.current() == 10 && after.reserved() == 10
                        && after.available() == 0
                        && game.turn == turnBefore,
                "committed alternate parking must debit only two unreserved units");
    }

    private static World world() {
        World world = new World(61009L, 30, 15);
        world.zoneType = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        world.sectorX = 1;
        world.sectorY = 1;
        world.zoneX = 2;
        world.zoneY = 2;
        world.floor = 4;
        world.mapObjects.clear();
        world.npcs.clear();
        for (int x = 0; x < world.w; x++) {
            for (int y = 0; y < world.h; y++) world.tiles[x][y] = '#';
        }
        world.tiles[2][4] = RoadGridIntegrationAuthority.PARKING_SPACE;
        for (int x = 3; x < 25; x++) {
            world.tiles[x][4] = RoadGridIntegrationAuthority.ROAD_LANE;
        }
        world.tiles[25][4] = RoadGridIntegrationAuthority.PARKING_SPACE;
        return world;
    }

    private static MapObjectState vehicle(World world, int x, int y, String type,
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
                world, x, y, profile, new Random(seed), "VEHICLE-TRANSIT-SMOKE");
        VehicleRuntimeAuthority.initialize(world, vehicle,
                faction == null ? Faction.NONE : faction,
                ownership, "local-transit", false, new Random(seed));
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerType", ownerType.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerFaction", (faction == null ? Faction.NONE : faction).name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownership", ownership);
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerName", ownerType == VehicleRuntimeAuthority.OwnerType.PLAYER
                        ? "Player" : faction == null ? "Unassigned" : faction.label);
        return vehicle;
    }

    private static boolean contiguous(List<Point> path) {
        if (path == null || path.isEmpty()) return false;
        for (int i = 1; i < path.size(); i++) {
            Point before = path.get(i - 1);
            Point after = path.get(i);
            if (Math.abs(before.x - after.x) + Math.abs(before.y - after.y) != 1) return false;
        }
        return true;
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

    private Milestone06VehicleTransitSmoke() { }
}
