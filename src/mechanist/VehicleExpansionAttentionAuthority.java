package mechanist;

import java.util.Locale;

/** Applies player heat and suspicion from vehicle-scale ownership and salvage. */
final class VehicleExpansionAttentionAuthority {
    record Attention(int heatAdded, int suspicionAdded, int ownedVehicles,
                     String band, String summary) { }

    private VehicleExpansionAttentionAuthority() { }

    static Attention applyAcquisition(GamePanel game, MapObjectState vehicle,
                                      String source) {
        if (game == null || vehicle == null || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            return new Attention(0, 0, 0, "none",
                    "No vehicle attention was generated.");
        }
        VehicleRuntimeAuthority.VehicleClass vehicleClass =
                VehicleRuntimeAuthority.vehicleClass(vehicle.type);
        int countBefore = Math.max(0, playerOwnedCount(game) - 1);
        int footprint = footprint(vehicleClass);
        int fleetPressure = countBefore >= 2 ? 2 + countBefore / 2 : 0;
        int heat = Math.max(0, footprint + fleetPressure);
        int suspicion = switch (vehicleClass) {
            case UTILITY_BIKE -> 0;
            case CIVILIAN_CAR -> countBefore >= 3 ? 1 : 0;
            case CARGO_TRUCK -> 1;
            case ARMORED_CAR -> 4;
            case TANK -> 10;
        };
        game.gangHeat = Math.min(100, Math.max(0, game.gangHeat) + heat);
        game.suspicion = Math.min(100, Math.max(0, game.suspicion) + suspicion);
        int owned = playerOwnedCount(game);
        String band = band(owned, vehicleClass);
        String summary = "Vehicle attention: +" + heat + " heat"
                + (suspicion > 0 ? ", +" + suspicion + " suspicion" : "")
                + "; player fleet " + owned + " / " + band
                + " after " + clean(source, "vehicle acquisition") + ".";
        return new Attention(heat, suspicion, owned, band, summary);
    }

    static Attention applySalvage(GamePanel game, MapObjectState vehicle, int yield) {
        if (game == null || vehicle == null) {
            return new Attention(0, 0, 0, "none",
                    "No vehicle salvage attention was generated.");
        }
        VehicleRuntimeAuthority.VehicleClass vehicleClass =
                VehicleRuntimeAuthority.vehicleClass(vehicle.type);
        int heat = switch (vehicleClass) {
            case UTILITY_BIKE, CIVILIAN_CAR -> 0;
            case CARGO_TRUCK -> 1;
            case ARMORED_CAR -> 2;
            case TANK -> 5;
        };
        int suspicion = (vehicleClass == VehicleRuntimeAuthority.VehicleClass.ARMORED_CAR
                || vehicleClass == VehicleRuntimeAuthority.VehicleClass.TANK) ? 2 : 0;
        game.gangHeat = Math.min(100, Math.max(0, game.gangHeat) + heat);
        game.suspicion = Math.min(100, Math.max(0, game.suspicion) + suspicion);
        int owned = playerOwnedCount(game);
        String band = band(owned, vehicleClass);
        String summary = "Salvage attention: " + yield + " recovered part(s), +"
                + heat + " heat"
                + (suspicion > 0 ? ", +" + suspicion + " suspicion" : "")
                + "; remaining player fleet " + owned + " / " + band + ".";
        return new Attention(heat, suspicion, owned, band, summary);
    }

    static int playerOwnedCount(GamePanel game) {
        if (game == null || game.world == null || game.world.mapObjects == null) return 0;
        int count = 0;
        for (MapObjectState object : game.world.mapObjects) {
            if (VehicleRuntimeAuthority.isVehicle(object)
                    && VehicleRuntimeAuthority.playerOwns(game, object)
                    && !"salvaged".equals(MapObjectState.stockValue(
                    object.stockState, "condition"))) count++;
        }
        return count;
    }

    static String fleetSummary(GamePanel game) {
        int bikes = 0;
        int cars = 0;
        int trucks = 0;
        int armored = 0;
        int tanks = 0;
        if (game != null && game.world != null && game.world.mapObjects != null) {
            for (MapObjectState object : game.world.mapObjects) {
                if (!VehicleRuntimeAuthority.isVehicle(object)
                        || !VehicleRuntimeAuthority.playerOwns(game, object)
                        || "salvaged".equals(MapObjectState.stockValue(
                        object.stockState, "condition"))) continue;
                switch (VehicleRuntimeAuthority.vehicleClass(object.type)) {
                    case UTILITY_BIKE -> bikes++;
                    case CIVILIAN_CAR -> cars++;
                    case CARGO_TRUCK -> trucks++;
                    case ARMORED_CAR -> armored++;
                    case TANK -> tanks++;
                }
            }
        }
        int total = bikes + cars + trucks + armored + tanks;
        return "Player vehicle footprint: total " + total
                + ", bikes " + bikes + ", cars " + cars + ", trucks " + trucks
                + ", armored cars " + armored + ", tanks " + tanks
                + "; attention band " + band(total,
                tanks > 0 ? VehicleRuntimeAuthority.VehicleClass.TANK
                        : armored > 0 ? VehicleRuntimeAuthority.VehicleClass.ARMORED_CAR
                        : trucks > 0 ? VehicleRuntimeAuthority.VehicleClass.CARGO_TRUCK
                        : cars > 0 ? VehicleRuntimeAuthority.VehicleClass.CIVILIAN_CAR
                        : VehicleRuntimeAuthority.VehicleClass.UTILITY_BIKE) + ".";
    }

    private static int footprint(VehicleRuntimeAuthority.VehicleClass vehicleClass) {
        return switch (vehicleClass) {
            case UTILITY_BIKE -> 1;
            case CIVILIAN_CAR -> 2;
            case CARGO_TRUCK -> 4;
            case ARMORED_CAR -> 8;
            case TANK -> 15;
        };
    }

    private static String band(int count,
                               VehicleRuntimeAuthority.VehicleClass strongest) {
        if (strongest == VehicleRuntimeAuthority.VehicleClass.TANK) {
            return "strategic military asset";
        }
        if (strongest == VehicleRuntimeAuthority.VehicleClass.ARMORED_CAR
                || count >= 6) return "faction-scale motor pool";
        if (strongest == VehicleRuntimeAuthority.VehicleClass.CARGO_TRUCK
                || count >= 3) return "visible commercial fleet";
        if (count >= 1) return "personal transport footprint";
        return "no active vehicle holdings";
    }

    private static String clean(String value, String fallback) {
        String text = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return text.isBlank() ? fallback : text.toLowerCase(Locale.ROOT);
    }
}
