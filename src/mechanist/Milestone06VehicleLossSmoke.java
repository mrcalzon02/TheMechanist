package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Focused smoke for persistent vehicle loss, recovery, capture, hazards, and salvage. */
final class Milestone06VehicleLossSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            game.playerX = 2;
            game.playerY = 2;
            game.turn = 1_040;
            game.worldTurn = 1_040L;
            game.carriedScript = 80;
            game.inventory.clear();
            game.inventory.add("Tool bundle");
            game.inventory.add("Machine part");
            game.inventory.add("Machine part");

            MapObjectState disabled = vehicle(game.world, 5, 6,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    VehicleRuntimeAuthority.OwnerType.PLAYER,
                    Faction.NONE, "player-owned", 81L);
            game.world.mapObjects.add(disabled);
            VehicleOperationFeedbackAuthority.begin(game, disabled, 4,
                    5, 6, 9, 6, 1, 0, 1_000L, false);
            require(VehicleOperationFeedbackAuthority.activeSessionCount(1_050L) == 1,
                    "loss fixture should begin with one transient operation session");
            VehicleRuntimeAuthority.applyDamage(disabled,
                    VehicleRuntimeAuthority.Component.MOBILITY, 100,
                    game.turn, "loss smoke destroyed wheels");
            int mapObjectCount = game.world.mapObjects.size();
            PlayerState beforeLoss = PlayerState.capture(game);
            VehicleLossAuthority.Resolution disabledLoss =
                    VehicleLossAuthority.resolve(game, disabled,
                            VehicleLossAuthority.Cause.COMPONENT_FAILURE,
                            Faction.NONE, "mobility failure on a public road");
            require(disabledLoss.success() && disabledLoss.changed()
                            && disabledLoss.primaryOutcome()
                            == VehicleLossAuthority.Outcome.DISABLED_REPAIR_PROJECT
                            && disabledLoss.outcomes().contains(
                            VehicleLossAuthority.Outcome.BLOCKED_ROAD_OBSTACLE)
                            && VehicleLossAuthority.isRoadBlocker(disabled)
                            && "disabled".equals(MapObjectState.stockValue(
                            disabled.stockState, "operationState")),
                    "critical mobility loss should become a physical road-side repair project");
            require(game.world.mapObjects.size() == mapObjectCount
                            && game.world.mapObjects.contains(disabled),
                    "vehicle loss must preserve the physical fixture instead of deleting it");
            require(VehicleOperationFeedbackAuthority.activeSessionCount(1_050L) == 0,
                    "disabled vehicle loss should terminate transient operation feedback");
            beforeLoss.requireSame(game,
                    "loss-state resolution must not spend player resources");

            VehicleMaintenanceAuthority.Result recovery =
                    VehicleMaintenanceAuthority.perform(game, disabled,
                            VehicleMaintenanceAuthority.Mode.COMPONENT_REPLACEMENT,
                            null);
            require(recovery.success()
                            && component(disabled,
                            VehicleRuntimeAuthority.Component.MOBILITY) == 100
                            && "parked".equals(MapObjectState.stockValue(
                            disabled.stockState, "operationState"))
                            && !VehicleLossAuthority.isRoadBlocker(disabled)
                            && "recovered".equals(MapObjectState.stockValue(
                            disabled.stockState, "lossRecoveryState")),
                    "component replacement should recover the repair project and clear active loss obstruction");
            requireContains(MapObjectState.stockValue(
                            disabled.stockState, "lossHistory"),
                    "critical systems restored", "loss recovery history");
            requireContains(MapObjectState.stockValue(
                            disabled.stockState, "lossOutcomeTags"),
                    "DISABLED_REPAIR_PROJECT", "retained historical loss tags");

            MapObjectState burned = vehicle(game.world, 12, 6,
                    AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK,
                    VehicleRuntimeAuthority.OwnerType.PRIVATE,
                    Faction.NONE, "private", 82L);
            game.world.mapObjects.add(burned);
            PlayerState beforeFire = PlayerState.capture(game);
            VehicleLossAuthority.Resolution fire =
                    VehicleLossAuthority.resolve(game, burned,
                            VehicleLossAuthority.Cause.FIRE,
                            Faction.NONE, "cargo-bay fire");
            require(fire.success()
                            && fire.primaryOutcome()
                            == VehicleLossAuthority.Outcome.BURNED_OUT_WRECK
                            && fire.outcomes().contains(
                            VehicleLossAuthority.Outcome.BLOCKED_ROAD_OBSTACLE)
                            && fire.outcomes().contains(
                            VehicleLossAuthority.Outcome.FUEL_OR_POWER_LEAK)
                            && "wreck".equals(MapObjectState.stockValue(
                            burned.stockState, "condition"))
                            && component(burned,
                            VehicleRuntimeAuthority.Component.FRAME) == 0
                            && burned.glyph == 'x'
                            && VehicleLossAuthority.hasLeakHazard(burned),
                    "vehicle fire should produce a persistent burned road wreck and leak hazard");
            beforeFire.requireSame(game,
                    "fire loss resolution must not spend player resources");

            VehicleLossAuthority.Resolution hulk =
                    VehicleLossAuthority.resolve(game, burned,
                            VehicleLossAuthority.Cause.SALVAGE_CONVERSION,
                            Faction.NONE, "recovery crew declared a salvage hulk");
            require(hulk.success()
                            && hulk.primaryOutcome()
                            == VehicleLossAuthority.Outcome.SALVAGE_HULK
                            && "SALVAGE".equals(MapObjectState.stockValue(
                            burned.stockState, "ownerType"))
                            && "salvage-hulk".equals(MapObjectState.stockValue(
                            burned.stockState, "ownership"))
                            && "wreck".equals(MapObjectState.stockValue(
                            burned.stockState, "condition"))
                            && burned.glyph == 'o',
                    "burned wreck should convert into a persistent recoverable salvage hulk");
            int partsBeforeStrip = count(game, "Machine part");
            VehicleRuntimeAuthority.Result stripped =
                    VehicleRuntimeAuthority.salvageForPlayer(game, burned);
            require(stripped.success() && stripped.amount() > 0
                            && count(game, "Machine part")
                            == partsBeforeStrip + stripped.amount()
                            && "salvaged".equals(MapObjectState.stockValue(
                            burned.stockState, "condition"))
                            && "dismantled".equals(MapObjectState.stockValue(
                            burned.stockState, "operationState"))
                            && game.world.mapObjects.contains(burned),
                    "salvage hulk should feed the existing one-time salvage path and remain as a stripped fixture");

            MapObjectState abandoned = vehicle(game.world, 18, 6,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    VehicleRuntimeAuthority.OwnerType.PRIVATE,
                    Faction.NONE, "private", 83L);
            game.world.mapObjects.add(abandoned);
            VehicleLossAuthority.Resolution abandonment =
                    VehicleLossAuthority.resolve(game, abandoned,
                            VehicleLossAuthority.Cause.ABANDONMENT,
                            Faction.NONE, "registered owner departed the district");
            require(abandonment.success()
                            && abandonment.primaryOutcome()
                            == VehicleLossAuthority.Outcome.ABANDONED_RECOVERY_ASSET
                            && "ABANDONED".equals(MapObjectState.stockValue(
                            abandoned.stockState, "ownerType"))
                            && "abandoned".equals(MapObjectState.stockValue(
                            abandoned.stockState, "ownership")),
                    "intact abandonment should become an abandoned recovery asset, not false looting");
            VehicleRuntimeAuthority.Result claimed =
                    VehicleRuntimeAuthority.interact(game, abandoned);
            require(claimed.success() && claimed.changed()
                            && "PLAYER".equals(MapObjectState.stockValue(
                            abandoned.stockState, "ownerType"))
                            && "salvage-claim".equals(MapObjectState.stockValue(
                            abandoned.stockState, "ownership")),
                    "legal civilian abandonment should feed the existing player claim path");

            MapObjectState nobleTank = vehicle(game.world, 22, 6,
                    AssetIntegrationDisciplineAuthority.PARKED_TANK,
                    VehicleRuntimeAuthority.OwnerType.FACTION,
                    Faction.NOBLE, "faction", 84L);
            game.world.mapObjects.add(nobleTank);
            int noblePressure = game.factionMarketPressure.getOrDefault(
                    Faction.NOBLE, 0);
            PlayerState beforeCapture = PlayerState.capture(game);
            VehicleLossAuthority.Resolution captured =
                    VehicleLossAuthority.resolve(game, nobleTank,
                            VehicleLossAuthority.Cause.CAPTURE,
                            Faction.MECHANIST_COLLEGIA,
                            "motor-pool seizure after district defeat");
            require(captured.success()
                            && captured.primaryOutcome()
                            == VehicleLossAuthority.Outcome.CAPTURED_MOTOR_POOL_ASSET
                            && captured.outcomes().contains(
                            VehicleLossAuthority.Outcome.FACTION_TROPHY)
                            && captured.outcomes().contains(
                            VehicleLossAuthority.Outcome.STRATEGIC_ASSET_LOSS)
                            && "FACTION".equals(MapObjectState.stockValue(
                            nobleTank.stockState, "ownerType"))
                            && Faction.MECHANIST_COLLEGIA.name().equals(
                            MapObjectState.stockValue(nobleTank.stockState,
                                    "ownerFaction"))
                            && "captured-motor-pool".equals(
                            MapObjectState.stockValue(nobleTank.stockState,
                                    "vehicleRole"))
                            && game.factionMarketPressure.getOrDefault(
                            Faction.NOBLE, 0) > noblePressure,
                    "restricted rival tank capture should become a motor-pool asset, trophy, and strategic loss");
            beforeCapture.requireSame(game,
                    "background capture must not spend player resources");

            VehicleLossAuthority.Resolution quest =
                    VehicleLossAuthority.resolve(game, nobleTank,
                            VehicleLossAuthority.Cause.QUEST_MARK,
                            Faction.NONE, "recover the captured crawler records");
            require(quest.success()
                            && quest.primaryOutcome()
                            == VehicleLossAuthority.Outcome.QUEST_OBJECTIVE
                            && "true".equals(MapObjectState.stockValue(
                            nobleTank.stockState, "questObjective")),
                    "captured vehicle should be markable as a persistent quest objective");
            VehicleLossAuthority.Resolution strategic =
                    VehicleLossAuthority.resolve(game, nobleTank,
                            VehicleLossAuthority.Cause.STRATEGIC_DEFEAT,
                            Faction.NONE, "crawler written off after convoy loss");
            require(strategic.success()
                            && "true".equals(MapObjectState.stockValue(
                            nobleTank.stockState, "strategicAssetLost")),
                    "vehicle should retain a separate strategic-loss record");

            VehicleLossAuthority.Resolution looted =
                    VehicleLossAuthority.resolve(game, nobleTank,
                            VehicleLossAuthority.Cause.LOOTING,
                            Faction.NONE, "crew stores stripped during confusion");
            require(looted.success()
                            && looted.primaryOutcome()
                            == VehicleLossAuthority.Outcome.LOOTED_VEHICLE
                            && "true".equals(MapObjectState.stockValue(
                            nobleTank.stockState, "cargoLooted"))
                            && component(nobleTank,
                            VehicleRuntimeAuthority.Component.CARGO) == 35,
                    "vehicle looting should persist cargo loss without deleting the asset");
            String tankBeforeRepeat = nobleTank.stockState;
            VehicleLossAuthority.Resolution repeatLoot =
                    VehicleLossAuthority.resolve(game, nobleTank,
                            VehicleLossAuthority.Cause.LOOTING,
                            Faction.NONE, "duplicate loot attempt");
            require(!repeatLoot.success()
                            && tankBeforeRepeat.equals(nobleTank.stockState),
                    "vehicle looting must be idempotent after loose cargo is gone");

            MapObjectState playerCar = vehicle(game.world, 26, 6,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    VehicleRuntimeAuthority.OwnerType.PLAYER,
                    Faction.NONE, "player-owned", 85L);
            game.world.mapObjects.add(playerCar);
            String playerCarBefore = playerCar.stockState;
            VehicleLossAuthority.Resolution protectedPlayer =
                    VehicleLossAuthority.resolve(game, playerCar,
                            VehicleLossAuthority.Cause.CAPTURE,
                            Faction.NOBLE, "background confiscation attempt");
            require(!protectedPlayer.success()
                            && playerCarBefore.equals(playerCar.stockState),
                    "background loss handling must not confiscate player-owned vehicles");

            List<String> inspection =
                    VehicleLossAuthority.inspectionLines(game.world, nobleTank);
            requireContains(inspection, "quest objective",
                    "vehicle objective consequence");
            requireContains(inspection, "material loss",
                    "vehicle strategic consequence");
            requireContains(inspection, "Recovery paths",
                    "vehicle recovery guidance");
            for (String line : inspection) {
                require(!PlayerFacingText.containsLikelyLeak(line),
                        "vehicle loss inspection leaked implementation text: " + line);
            }

            System.out.println("Milestone 06 vehicle loss smoke passed.");
        } finally {
            VehicleOperationFeedbackAuthority.clearTransientFeedback();
            game.shutdownRuntime();
        }
    }

    private static World world() {
        World world = new World(61019L, 30, 16);
        world.zoneType = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        world.mapObjects.clear();
        world.npcs.clear();
        for (int x = 0; x < world.w; x++) {
            for (int y = 0; y < world.h; y++) world.tiles[x][y] = '.';
        }
        for (int x = 3; x < 29; x++) {
            world.tiles[x][6] = RoadGridIntegrationAuthority.ROAD_LANE;
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
                "VEHICLE-LOSS-SMOKE");
        VehicleRuntimeAuthority.initialize(world, vehicle,
                faction == null ? Faction.NONE : faction,
                ownership, "loss-smoke", false, new Random(seed));
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerType", ownerType.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerFaction", (faction == null ? Faction.NONE : faction).name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerName", ownerType == VehicleRuntimeAuthority.OwnerType.PLAYER
                        ? "Player" : faction == null || faction == Faction.NONE
                        ? "Registered private owner" : faction.label);
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

    private record PlayerState(int script, int supplies, int machineParts,
                               int turn, long worldTurn,
                               List<String> inventory) {
        static PlayerState capture(GamePanel game) {
            return new PlayerState(game.carriedScript, game.supplies,
                    game.machineParts, game.turn, game.worldTurn,
                    new ArrayList<>(game.inventory));
        }

        void requireSame(GamePanel game, String message) {
            require(script == game.carriedScript
                            && supplies == game.supplies
                            && machineParts == game.machineParts
                            && turn == game.turn
                            && worldTurn == game.worldTurn
                            && inventory.equals(game.inventory),
                    message);
        }
    }

    private Milestone06VehicleLossSmoke() { }
}
