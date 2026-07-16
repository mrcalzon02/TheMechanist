package mechanist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Player vehicle maintenance over the authoritative component ledger.
 *
 * Resource checks complete before any component or inventory mutation. Tools are
 * required but retained; consumable parts and script are deducted only when the
 * selected repair can actually begin.
 */
final class VehicleMaintenanceAuthority {
    enum Mode {
        FIELD_PATCH("field patch", 20, 1, 0, true, 1),
        GARAGE_REPAIR("garage repair", 35, 1, 8, false, 1),
        COMPONENT_REPLACEMENT("component replacement", 100, 2, 12, true, 2),
        FULL_REFURBISHMENT("full refurbishment", 100, 4, 30, true, 4);

        final String label;
        final int repairAmount;
        final int machineParts;
        final int script;
        final boolean requiresToolBundle;
        final int laborTurns;

        Mode(String label, int repairAmount, int machineParts, int script,
             boolean requiresToolBundle, int laborTurns) {
            this.label = label;
            this.repairAmount = repairAmount;
            this.machineParts = machineParts;
            this.script = script;
            this.requiresToolBundle = requiresToolBundle;
            this.laborTurns = laborTurns;
        }
    }

    record Result(boolean success, boolean changed, Mode mode,
                  VehicleRuntimeAuthority.Component component,
                  int before, int after, int partsSpent, int scriptSpent,
                  String message, MapObjectState vehicle) {
        static Result blocked(Mode mode, String message, MapObjectState vehicle) {
            return new Result(false, false, mode, null, 0, 0, 0, 0,
                    message == null || message.isBlank()
                            ? "Vehicle maintenance is blocked." : message,
                    vehicle);
        }
    }

    private VehicleMaintenanceAuthority() { }

    static Result serviceNearestPlayerVehicle(GamePanel game,
                                              MapObjectState garage) {
        if (game == null || game.world == null) {
            return Result.blocked(Mode.GARAGE_REPAIR,
                    "SERVICE GARAGE: no vehicle-service world is loaded.", null);
        }
        MapObjectState vehicle = nearestPlayerVehicle(game, garage);
        if (vehicle == null) {
            return Result.blocked(Mode.GARAGE_REPAIR,
                    "SERVICE GARAGE: no player-owned vehicle is staged in this zone.", null);
        }
        VehicleRuntimeAuthority.ensureInitialized(game.world, vehicle);
        VehicleRuntimeAuthority.Component worst = worstComponent(vehicle);
        if (worst == null) {
            return new Result(true, false, Mode.GARAGE_REPAIR, null,
                    100, 100, 0, 0,
                    "SERVICE GARAGE: " + displayName(vehicle)
                            + " is already at full component integrity; no payment or parts were taken.",
                    vehicle);
        }
        int damagedCount = damagedComponentCount(vehicle);
        int worstValue = component(vehicle, worst);
        Mode recommended = worstValue <= 0
                ? Mode.COMPONENT_REPLACEMENT
                : damagedCount >= 4 && VehicleRuntimeAuthority.integrity(vehicle) < 65
                ? Mode.FULL_REFURBISHMENT
                : Mode.GARAGE_REPAIR;
        Result result = perform(game, vehicle, recommended, garage);
        if (!result.success() && recommended != Mode.GARAGE_REPAIR) {
            Result fallback = perform(game, vehicle, Mode.GARAGE_REPAIR, garage);
            if (fallback.success()) return fallback;
        }
        return result;
    }

    static Result perform(GamePanel game, MapObjectState vehicle,
                          Mode mode, MapObjectState serviceSource) {
        if (game == null || game.world == null || vehicle == null
                || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            return Result.blocked(mode, "No repairable vehicle is available.", vehicle);
        }
        Mode selected = mode == null ? Mode.GARAGE_REPAIR : mode;
        VehicleRuntimeAuthority.ensureInitialized(game.world, vehicle);
        if (!VehicleRuntimeAuthority.playerOwns(game, vehicle)) {
            return Result.blocked(selected,
                    "Vehicle maintenance requires player ownership or an authorized player salvage claim.",
                    vehicle);
        }
        String condition = value(vehicle, "condition");
        if ("salvaged".equals(condition)
                || "dismantled".equals(value(vehicle, "operationState"))) {
            return Result.blocked(selected,
                    "A stripped vehicle hulk cannot be restored through ordinary maintenance.",
                    vehicle);
        }
        VehicleRuntimeAuthority.Component worst = worstComponent(vehicle);
        if (worst == null) {
            return new Result(true, false, selected, null, 100, 100,
                    0, 0, displayName(vehicle)
                    + " is already at full component integrity; no resources were consumed.", vehicle);
        }

        int partCost = selected.machineParts;
        int scriptCost = selected.script;
        boolean usePartAlternative = selected == Mode.GARAGE_REPAIR
                && countInventory(game, "Machine part") >= 1;
        if (usePartAlternative) scriptCost = 0;
        if (selected.requiresToolBundle
                && countInventory(game, "Tool bundle") < 1) {
            return Result.blocked(selected,
                    selected.label + " requires a carried Tool bundle; the tool is retained after work.",
                    vehicle);
        }
        if (countInventory(game, "Machine part") < partCost) {
            if (selected == Mode.GARAGE_REPAIR && game.carriedScript >= selected.script) {
                partCost = 0;
                scriptCost = selected.script;
            } else {
                return Result.blocked(selected,
                        selected.label + " requires " + partCost
                                + " Machine part(s)"
                                + (scriptCost > 0 ? " and " + scriptCost + " script" : "")
                                + "; no resources were consumed.", vehicle);
            }
        }
        if (game.carriedScript < scriptCost) {
            return Result.blocked(selected,
                    selected.label + " requires " + scriptCost
                            + " script after parts are reserved; no resources were consumed.",
                    vehicle);
        }

        int before = selected == Mode.FULL_REFURBISHMENT
                ? VehicleRuntimeAuthority.integrity(vehicle) : component(vehicle, worst);
        for (int i = 0; i < partCost; i++) removeInventory(game, "Machine part");
        if (scriptCost > 0) game.spendImperialScript(scriptCost);

        int after;
        if (selected == Mode.FULL_REFURBISHMENT) {
            for (VehicleRuntimeAuthority.Component component
                    : VehicleRuntimeAuthority.Component.values()) {
                setComponent(vehicle, component, 100);
            }
            after = 100;
        } else if (selected == Mode.COMPONENT_REPLACEMENT) {
            setComponent(vehicle, worst, 100);
            after = 100;
        } else {
            after = Math.min(100, before + selected.repairAmount);
            setComponent(vehicle, worst, after);
        }
        VehicleRuntimeAuthority.ensureInitialized(game.world, vehicle);
        append(vehicle, "repairHistory", selected.label + " / "
                + (selected == Mode.FULL_REFURBISHMENT
                ? "all component groups " + before + "->100 average"
                : worst.label + " " + before + "->" + after)
                + " / turn " + Math.max(0, game.turn)
                + " / parts " + partCost + " / script " + scriptCost
                + " / labor " + selected.laborTurns + " turn(s)"
                + sourceLabel(serviceSource));
        append(vehicle, "vehicleHistory", "Maintenance completed: "
                + selected.label + " at turn " + Math.max(0, game.turn));
        set(vehicle, "lastMaintenanceMode", selected.name());
        set(vehicle, "lastMaintenanceLaborTurns",
                Integer.toString(selected.laborTurns));
        set(vehicle, "lastMaintenanceSource",
                serviceSource == null ? "field maintenance"
                        : clean(serviceSource.label, "vehicle service facility"));
        if (partCost > 0) game.rebuildItemContainersFromLegacyLists();

        String target = selected == Mode.FULL_REFURBISHMENT
                ? "all major component groups"
                : worst.label;
        return new Result(true, true, selected, worst, before, after,
                partCost, scriptCost,
                "VEHICLE MAINTENANCE: " + selected.label + " restored "
                        + displayName(vehicle) + " " + target + " from "
                        + before + "% to " + after + "%. Cost: "
                        + (partCost == 0 ? "no Machine parts" : partCost + " Machine part(s)")
                        + ", " + scriptCost + " script; Tool bundle "
                        + (selected.requiresToolBundle ? "required and retained" : "not required")
                        + "; labor scale " + selected.laborTurns + " turn(s). Condition is now "
                        + value(vehicle, "condition") + ".",
                vehicle);
    }

    static List<String> inspectionLines(GamePanel game, MapObjectState vehicle) {
        ArrayList<String> lines = new ArrayList<>();
        if (vehicle == null || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            return List.of("No vehicle maintenance record is available.");
        }
        VehicleRuntimeAuthority.ensureInitialized(game == null ? null : game.world, vehicle);
        VehicleRuntimeAuthority.Component worst = worstComponent(vehicle);
        lines.add("Maintenance condition: " + value(vehicle, "condition")
                + "; overall integrity " + VehicleRuntimeAuthority.integrity(vehicle) + "%.");
        lines.add(worst == null
                ? "All major component groups are at full integrity."
                : "Lowest component: " + worst.label + " at "
                + component(vehicle, worst) + "%.");
        lines.add("Field patch: Tool bundle plus 1 Machine part; restores 20% to the lowest component.");
        lines.add("Garage repair: 1 Machine part or 8 script; restores 35% to the lowest component.");
        lines.add("Component replacement: Tool bundle, 2 Machine parts, and 12 script; replaces the lowest component.");
        lines.add("Full refurbishment: Tool bundle, 4 Machine parts, and 30 script; restores all major component groups.");
        return List.copyOf(lines);
    }

    private static MapObjectState nearestPlayerVehicle(GamePanel game,
                                                       MapObjectState origin) {
        int ox = origin == null ? game.playerX : origin.x;
        int oy = origin == null ? game.playerY : origin.y;
        ArrayList<MapObjectState> vehicles = new ArrayList<>();
        for (MapObjectState object : game.world.mapObjects) {
            if (VehicleRuntimeAuthority.isVehicle(object)
                    && VehicleRuntimeAuthority.playerOwns(game, object)
                    && !"salvaged".equals(value(object, "condition"))) {
                vehicles.add(object);
            }
        }
        vehicles.sort(Comparator
                .comparingInt((MapObjectState object) ->
                        Math.abs(object.x - ox) + Math.abs(object.y - oy))
                .thenComparing(object -> clean(object.id, object.label)));
        return vehicles.isEmpty() ? null : vehicles.get(0);
    }

    private static VehicleRuntimeAuthority.Component worstComponent(
            MapObjectState vehicle) {
        VehicleRuntimeAuthority.Component worst = null;
        int lowest = 101;
        for (VehicleRuntimeAuthority.Component component
                : VehicleRuntimeAuthority.Component.values()) {
            int value = component(vehicle, component);
            if (value < lowest) {
                lowest = value;
                worst = component;
            }
        }
        return lowest >= 100 ? null : worst;
    }

    private static int damagedComponentCount(MapObjectState vehicle) {
        int count = 0;
        for (VehicleRuntimeAuthority.Component component
                : VehicleRuntimeAuthority.Component.values()) {
            if (component(vehicle, component) < 100) count++;
        }
        return count;
    }

    private static int component(MapObjectState vehicle,
                                 VehicleRuntimeAuthority.Component component) {
        try {
            return Math.max(0, Math.min(100, Integer.parseInt(value(vehicle,
                    componentKey(component)))));
        } catch (Exception ignored) {
            return 100;
        }
    }

    private static void setComponent(MapObjectState vehicle,
                                     VehicleRuntimeAuthority.Component component,
                                     int amount) {
        set(vehicle, componentKey(component),
                Integer.toString(Math.max(0, Math.min(100, amount))));
    }

    private static String componentKey(VehicleRuntimeAuthority.Component component) {
        return "vehicleComponent" + component.name().charAt(0)
                + component.name().substring(1).toLowerCase(Locale.ROOT);
    }

    private static int countInventory(GamePanel game, String item) {
        int count = 0;
        if (game == null || item == null) return count;
        for (String carried : game.inventory) {
            if (ItemQuality.namesMatch(carried, item)) count++;
        }
        return count;
    }

    private static boolean removeInventory(GamePanel game, String item) {
        if (game == null || item == null) return false;
        for (int i = 0; i < game.inventory.size(); i++) {
            if (ItemQuality.namesMatch(game.inventory.get(i), item)) {
                game.inventory.remove(i);
                return true;
            }
        }
        return false;
    }

    private static String sourceLabel(MapObjectState source) {
        return source == null ? " / field maintenance"
                : " / " + clean(source.label, "vehicle service facility");
    }

    private static String displayName(MapObjectState vehicle) {
        VehicleRuntimeAuthority.Snapshot snapshot =
                VehicleRuntimeAuthority.inspect(null, vehicle);
        if (snapshot == null) return "vehicle";
        return (clean(snapshot.manufacturer(), "") + " "
                + clean(snapshot.model(), snapshot.vehicleClass().label)).trim();
    }

    private static void append(MapObjectState vehicle, String key, String entry) {
        String existing = value(vehicle, key);
        ArrayList<String> entries = new ArrayList<>();
        if (existing != null && !existing.isBlank()) {
            for (String part : existing.split("~")) {
                String clean = clean(part, "");
                if (!clean.isBlank()) entries.add(clean);
            }
        }
        entries.add(clean(entry, "maintenance record"));
        while (entries.size() > 8) entries.remove(0);
        set(vehicle, key, String.join("~", entries));
    }

    private static String value(MapObjectState vehicle, String key) {
        return vehicle == null ? "" : MapObjectState.stockValue(vehicle.stockState, key);
    }

    private static void set(MapObjectState vehicle, String key, String value) {
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                key, clean(value, "").replace(';', ',').replace('|', '/'));
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return cleaned.isBlank() ? (fallback == null ? "" : fallback) : cleaned;
    }
}
