package mechanist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Executes logistics planning for faction stock movement.
 *
 * The manager decides whether a requested movement can be handled by available
 * hand assets, needs porter/carry-post labor, or requires faction vehicles with
 * cargo capacity. It is intentionally separate from stock routing so logistics
 * capacity can fail independently from inventory existence.
 */
final class FactionLogisticsExecutionManager {
    static final int DEFAULT_HAND_CAPACITY = 4;
    static final int DEFAULT_PORTER_CAPACITY = 12;

    private FactionLogisticsExecutionManager() {}

    static LogisticsExecutionPlan plan(Faction faction, int fromLocationKey, int toLocationKey, String item, int units, LogisticsAssetPool pool) {
        Faction f = FactionInventoryStockAuthority.normalizeFaction(faction);
        int requested = Math.max(0, units);
        LogisticsAssetPool usePool = pool == null ? new LogisticsAssetPool(f) : pool;
        LogisticsExecutionPlan plan = new LogisticsExecutionPlan(f, fromLocationKey, toLocationKey, item, requested);
        if (requested <= 0) {
            plan.mode = LogisticsMode.NONE;
            plan.status = "No logistics requested.";
            return plan;
        }

        int handCapacity = Math.max(0, usePool.availableHandCarriers) * Math.max(1, usePool.handCarryCapacity);
        int porterCapacity = Math.max(0, usePool.availablePorters) * Math.max(1, usePool.porterCarryCapacity);
        int vehicleCapacity = usePool.vehicleCapacity();
        plan.handCapacity = handCapacity;
        plan.porterCapacity = porterCapacity;
        plan.vehicleCapacity = vehicleCapacity;

        if (handCapacity >= requested) {
            plan.mode = LogisticsMode.HAND_CARRY;
            plan.assignedHandCarriers = carriersNeeded(requested, usePool.handCarryCapacity);
            plan.status = "Hand-carry route assigned.";
        } else if (handCapacity + porterCapacity >= requested) {
            plan.mode = LogisticsMode.CARRY_POST;
            int remaining = Math.max(0, requested - handCapacity);
            plan.assignedHandCarriers = Math.max(0, usePool.availableHandCarriers);
            plan.assignedPorters = carriersNeeded(remaining, usePool.porterCarryCapacity);
            plan.status = "Carry-post / porter movement assigned after hand capacity shortfall.";
        } else if (vehicleCapacity + handCapacity + porterCapacity >= requested) {
            plan.mode = LogisticsMode.VEHICLE_CARGO;
            int remaining = Math.max(0, requested - handCapacity - porterCapacity);
            plan.assignedHandCarriers = Math.max(0, usePool.availableHandCarriers);
            plan.assignedPorters = Math.max(0, usePool.availablePorters);
            plan.assignedVehicles.addAll(usePool.vehiclesForCapacity(remaining));
            plan.status = "Vehicle cargo route assigned for remaining load.";
        } else {
            plan.mode = LogisticsMode.INSUFFICIENT_CAPACITY;
            plan.assignedHandCarriers = Math.max(0, usePool.availableHandCarriers);
            plan.assignedPorters = Math.max(0, usePool.availablePorters);
            plan.assignedVehicles.addAll(usePool.availableVehicles);
            plan.unmetUnits = Math.max(0, requested - handCapacity - porterCapacity - vehicleCapacity);
            plan.status = "Insufficient logistics capacity; demand remains queued.";
        }
        return plan;
    }

    static boolean executePlannedRoute(LogisticsExecutionPlan plan, ZoneFactionStockTracker zoneStock) {
        if (plan == null || zoneStock == null || plan.requestedUnits <= 0) return false;
        if (plan.mode == LogisticsMode.INSUFFICIENT_CAPACITY || plan.mode == LogisticsMode.NONE) return false;
        if (!zoneStock.consume(plan.fromLocationKey, plan.faction, plan.item, plan.requestedUnits)) return false;
        zoneStock.add(plan.toLocationKey, plan.faction, plan.item, plan.requestedUnits);
        DebugLog.audit("FACTION_LOGISTICS", plan.summary());
        return true;
    }

    private static int carriersNeeded(int units, int capacity) {
        if (units <= 0) return 0;
        int cap = Math.max(1, capacity);
        return (units + cap - 1) / cap;
    }
}

enum LogisticsMode {
    NONE,
    HAND_CARRY,
    CARRY_POST,
    VEHICLE_CARGO,
    INSUFFICIENT_CAPACITY
}

final class LogisticsExecutionPlan {
    final Faction faction;
    final int fromLocationKey;
    final int toLocationKey;
    final String item;
    final int requestedUnits;
    LogisticsMode mode = LogisticsMode.NONE;
    int handCapacity;
    int porterCapacity;
    int vehicleCapacity;
    int assignedHandCarriers;
    int assignedPorters;
    int unmetUnits;
    String status = "Unplanned.";
    final ArrayList<LogisticsVehicleAsset> assignedVehicles = new ArrayList<>();

    LogisticsExecutionPlan(Faction faction, int fromLocationKey, int toLocationKey, String item, int requestedUnits) {
        this.faction = faction;
        this.fromLocationKey = fromLocationKey;
        this.toLocationKey = toLocationKey;
        this.item = item == null || item.isBlank() ? "Unknown item" : item;
        this.requestedUnits = Math.max(0, requestedUnits);
    }

    String summary() {
        return "faction=" + faction.label + " item=" + item + " units=" + requestedUnits + " from=" + fromLocationKey + " to=" + toLocationKey + " mode=" + mode + " handCap=" + handCapacity + " porterCap=" + porterCapacity + " vehicleCap=" + vehicleCapacity + " handAssigned=" + assignedHandCarriers + " portersAssigned=" + assignedPorters + " vehiclesAssigned=" + assignedVehicles.size() + " unmet=" + unmetUnits + " status=" + status;
    }
}

final class LogisticsAssetPool {
    final Faction faction;
    int availableHandCarriers;
    int handCarryCapacity = FactionLogisticsExecutionManager.DEFAULT_HAND_CAPACITY;
    int availablePorters;
    int porterCarryCapacity = FactionLogisticsExecutionManager.DEFAULT_PORTER_CAPACITY;
    final ArrayList<LogisticsVehicleAsset> availableVehicles = new ArrayList<>();

    LogisticsAssetPool(Faction faction) {
        this.faction = FactionInventoryStockAuthority.normalizeFaction(faction);
    }

    static LogisticsAssetPool fromPopulationAndVehicles(Faction faction, int populationUnits, int porterPosts, List<LogisticsVehicleAsset> vehicles) {
        LogisticsAssetPool pool = new LogisticsAssetPool(faction);
        pool.availableHandCarriers = Math.max(0, populationUnits);
        pool.availablePorters = Math.max(0, porterPosts);
        if (vehicles != null) pool.availableVehicles.addAll(vehicles);
        return pool;
    }

    int vehicleCapacity() {
        int total = 0;
        for (LogisticsVehicleAsset vehicle : availableVehicles) if (vehicle != null && vehicle.available) total += Math.max(0, vehicle.cargoCapacity);
        return total;
    }

    ArrayList<LogisticsVehicleAsset> vehiclesForCapacity(int units) {
        ArrayList<LogisticsVehicleAsset> sorted = new ArrayList<>();
        for (LogisticsVehicleAsset vehicle : availableVehicles) if (vehicle != null && vehicle.available && vehicle.cargoCapacity > 0) sorted.add(vehicle);
        sorted.sort(Comparator.comparingInt((LogisticsVehicleAsset v) -> v.cargoCapacity).reversed());
        ArrayList<LogisticsVehicleAsset> out = new ArrayList<>();
        int remaining = Math.max(0, units);
        for (LogisticsVehicleAsset vehicle : sorted) {
            if (remaining <= 0) break;
            out.add(vehicle);
            remaining -= vehicle.cargoCapacity;
        }
        return out;
    }

    String summary() {
        return "faction=" + faction.label + " handCarriers=" + availableHandCarriers + " handCapacity=" + handCarryCapacity + " porters=" + availablePorters + " porterCapacity=" + porterCarryCapacity + " vehicles=" + availableVehicles.size() + " vehicleCapacity=" + vehicleCapacity();
    }
}

final class LogisticsVehicleAsset {
    final String id;
    final String type;
    final int cargoCapacity;
    boolean available = true;

    LogisticsVehicleAsset(String id, String type, int cargoCapacity) {
        this.id = id == null || id.isBlank() ? "vehicle" : id;
        this.type = type == null || type.isBlank() ? "cargo vehicle" : type;
        this.cargoCapacity = Math.max(0, cargoCapacity);
    }

    static LogisticsVehicleAsset of(String id, String type, int cargoCapacity) {
        return new LogisticsVehicleAsset(id, type, cargoCapacity);
    }

    static LogisticsVehicleAsset inferred(String vehicleName) {
        String name = vehicleName == null ? "vehicle" : vehicleName;
        String low = name.toLowerCase(Locale.ROOT);
        int capacity;
        if (low.contains("bike")) capacity = 8;
        else if (low.contains("car")) capacity = 18;
        else if (low.contains("van")) capacity = 36;
        else if (low.contains("truck")) capacity = 80;
        else if (low.contains("cargo")) capacity = 120;
        else if (low.contains("armored")) capacity = 48;
        else capacity = 24;
        return new LogisticsVehicleAsset(name, name, capacity);
    }
}
