package mechanist;

import java.util.ArrayList;
import java.util.List;

/**
 * Atomic consumer for a READY strategic vehicle reservation.
 *
 * This class does not load worlds. The authoritative transition owner supplies
 * both already-resolved World instances and a destination parking cell. The
 * transaction either moves the same physical vehicle fixture and consumes its
 * reserved energy, or restores the complete pre-commit state.
 */
final class VehicleStrategicTransitCommitAuthority {
    enum Status { COMMITTED, BLOCKED, ROLLED_BACK, RECOVERED }

    record TransferRequest(World sourceWorld, World destinationWorld,
                           int destinationX, int destinationY,
                           String transitionReason) { }

    record Result(Status status, boolean success, boolean changed,
                  int fuelConsumed, String reservationId,
                  String message, MapObjectState vehicle) {
        static Result blocked(MapObjectState vehicle, String message) {
            return new Result(Status.BLOCKED, false, false, 0, "",
                    message == null || message.isBlank()
                            ? "Strategic vehicle transfer was blocked." : message,
                    vehicle);
        }
    }

    private VehicleStrategicTransitCommitAuthority() { }

    static Result commit(GamePanel game, MapObjectState vehicle,
                         TransferRequest request) {
        if (game == null || vehicle == null
                || !VehicleRuntimeAuthority.isVehicle(vehicle)) {
            return Result.blocked(vehicle,
                    "Strategic transfer requires a physical vehicle and active game state.");
        }
        if (request == null || request.sourceWorld() == null
                || request.destinationWorld() == null) {
            return Result.blocked(vehicle,
                    "Strategic transfer requires resolved source and destination worlds.");
        }
        World source = request.sourceWorld();
        World destination = request.destinationWorld();
        if (source == destination) {
            return Result.blocked(vehicle,
                    "Strategic transfer requires distinct source and destination worlds.");
        }
        if (!source.mapObjects.contains(vehicle)) {
            return Result.blocked(vehicle,
                    "The source world does not contain the reserved vehicle fixture.");
        }
        if (destination.mapObjects.contains(vehicle)) {
            return Result.blocked(vehicle,
                    "The destination already contains this vehicle fixture.");
        }
        if (!"reserved".equals(value(vehicle, "strategicTransitState"))) {
            return Result.blocked(vehicle,
                    "The vehicle has no active strategic transit reservation.");
        }
        String reservationId = value(vehicle, "strategicTransitReservationId");
        String expectedDestination = value(vehicle, "strategicTransitDestination");
        String resolvedDestination = Integer.toString(destination.locationKey());
        if (!expectedDestination.isBlank()
                && !expectedDestination.equals(resolvedDestination)) {
            return Result.blocked(vehicle,
                    "The loaded destination " + resolvedDestination
                            + " does not match reserved destination "
                            + expectedDestination + ".");
        }
        if (!legalDestination(destination, vehicle,
                request.destinationX(), request.destinationY())) {
            return Result.blocked(vehicle,
                    "The destination cell is not clear legal vehicle parking.");
        }
        VehicleFuelAuthority.ensureInitialized(source, vehicle);
        int fuelRequired = intValue(value(vehicle,
                "strategicTransitFuelReserved"), 0);
        VehicleFuelAuthority.Snapshot fuel =
                VehicleFuelAuthority.inspect(source, vehicle);
        if (fuelRequired <= 0) {
            return Result.blocked(vehicle,
                    "The reservation does not contain a positive fuel or power commitment.");
        }
        if (fuel.current() < fuelRequired) {
            return Result.blocked(vehicle,
                    "The vehicle has only " + fuel.current()
                            + " fuel or power units for a " + fuelRequired
                            + " unit reservation.");
        }

        int sourceX = vehicle.x;
        int sourceY = vehicle.y;
        String stockBefore = vehicle.stockState;
        char glyphBefore = vehicle.glyph;
        String labelBefore = vehicle.label;
        boolean sourceContained = source.mapObjects.contains(vehicle);
        boolean destinationContained = destination.mapObjects.contains(vehicle);
        String sourceKey = Integer.toString(source.locationKey());
        String destinationKey = Integer.toString(destination.locationKey());
        try {
            set(vehicle, "strategicTransitState", "committing");
            set(vehicle, "strategicTransitCommitSource", sourceKey);
            set(vehicle, "strategicTransitCommitDestination", destinationKey);
            set(vehicle, "strategicTransitSourceX", Integer.toString(sourceX));
            set(vehicle, "strategicTransitSourceY", Integer.toString(sourceY));
            set(vehicle, "strategicTransitCommitX",
                    Integer.toString(request.destinationX()));
            set(vehicle, "strategicTransitCommitY",
                    Integer.toString(request.destinationY()));
            set(vehicle, "strategicTransitCommitTurn",
                    Long.toString(Math.max(0L, game.worldTurn)));

            if (!source.mapObjects.remove(vehicle)) {
                throw new IllegalStateException("source removal failed");
            }
            vehicle.x = request.destinationX();
            vehicle.y = request.destinationY();
            destination.mapObjects.add(vehicle);

            VehicleFuelAuthority.Result consumed =
                    VehicleFuelAuthority.consumeCommitted(destination, vehicle,
                            fuelRequired, game.turn,
                            clean(request.transitionReason(),
                                    "strategic vehicle transfer"));
            if (!consumed.success()) {
                throw new IllegalStateException(consumed.message());
            }
            set(vehicle, "strategicTransitState", "completed");
            set(vehicle, "strategicTransitCompletedTurn",
                    Long.toString(Math.max(0L, game.worldTurn)));
            set(vehicle, "strategicTransitLastOrigin", sourceKey);
            set(vehicle, "strategicTransitLastDestination", destinationKey);
            set(vehicle, "operationState", "parked");
            set(vehicle, "headlightsActive", "false");
            append(vehicle, "deploymentHistory", "Strategic transfer "
                    + reservationId + " committed " + sourceKey
                    + " -> " + destinationKey + " / fuel "
                    + fuelRequired + " / destination " + vehicle.x + ","
                    + vehicle.y);
            append(vehicle, "vehicleHistory", "Strategic transit completed at turn "
                    + Math.max(0, game.turn));
            return new Result(Status.COMMITTED, true, true, fuelRequired,
                    reservationId,
                    "STRATEGIC TRANSIT COMMITTED: " + displayName(vehicle)
                            + " moved from " + sourceKey + " to "
                            + destinationKey + " at " + vehicle.x
                            + "," + vehicle.y + "; " + fuelRequired
                            + " fuel or power unit(s) were consumed.", vehicle);
        } catch (RuntimeException failure) {
            destination.mapObjects.remove(vehicle);
            if (sourceContained && !source.mapObjects.contains(vehicle)) {
                source.mapObjects.add(vehicle);
            }
            if (destinationContained && !destination.mapObjects.contains(vehicle)) {
                destination.mapObjects.add(vehicle);
            }
            vehicle.x = sourceX;
            vehicle.y = sourceY;
            vehicle.stockState = stockBefore;
            vehicle.glyph = glyphBefore;
            vehicle.label = labelBefore;
            return new Result(Status.ROLLED_BACK, false, false, 0,
                    reservationId,
                    "STRATEGIC TRANSIT ROLLED BACK: "
                            + clean(failure.getMessage(),
                            "the transfer transaction failed")
                            + ". Source position, ownership, reservation, and fuel ledger were restored.",
                    vehicle);
        }
    }

    static Result recoverInterrupted(GamePanel game, MapObjectState vehicle,
                                     World source, World destination) {
        if (vehicle == null || !VehicleRuntimeAuthority.isVehicle(vehicle)
                || source == null || destination == null) {
            return Result.blocked(vehicle,
                    "Interrupted transit recovery requires the vehicle and both candidate worlds.");
        }
        String state = value(vehicle, "strategicTransitState");
        if (!"committing".equals(state)) {
            return Result.blocked(vehicle,
                    "The vehicle is not marked as an interrupted committing transfer.");
        }
        boolean inSource = source.mapObjects.contains(vehicle);
        boolean inDestination = destination.mapObjects.contains(vehicle);
        String reservationId = value(vehicle, "strategicTransitReservationId");
        if (inSource && inDestination) {
            destination.mapObjects.remove(vehicle);
            vehicle.x = intValue(value(vehicle, "strategicTransitSourceX"),
                    vehicle.x);
            vehicle.y = intValue(value(vehicle, "strategicTransitSourceY"),
                    vehicle.y);
            set(vehicle, "strategicTransitState", "reserved");
            append(vehicle, "deploymentHistory", "Interrupted transfer "
                    + reservationId + " recovered to source after duplicate placement");
            return new Result(Status.RECOVERED, true, true, 0,
                    reservationId,
                    "STRATEGIC TRANSIT RECOVERED: duplicate destination placement was removed and the source reservation restored.",
                    vehicle);
        }
        if (inSource) {
            vehicle.x = intValue(value(vehicle, "strategicTransitSourceX"),
                    vehicle.x);
            vehicle.y = intValue(value(vehicle, "strategicTransitSourceY"),
                    vehicle.y);
            set(vehicle, "strategicTransitState", "reserved");
            append(vehicle, "deploymentHistory", "Interrupted transfer "
                    + reservationId + " recovered at source before fuel commit");
            return new Result(Status.RECOVERED, true, true, 0,
                    reservationId,
                    "STRATEGIC TRANSIT RECOVERED: the vehicle remains at source with its reservation and fuel intact.",
                    vehicle);
        }
        if (inDestination) {
            int reserved = intValue(value(vehicle,
                    "strategicTransitFuelReserved"), 0);
            VehicleFuelAuthority.Result consumed =
                    VehicleFuelAuthority.consumeCommitted(destination, vehicle,
                            reserved, game == null ? 0 : game.turn,
                            "interrupted strategic transfer recovery");
            if (!consumed.success()) {
                destination.mapObjects.remove(vehicle);
                source.mapObjects.add(vehicle);
                vehicle.x = intValue(value(vehicle,
                        "strategicTransitSourceX"), vehicle.x);
                vehicle.y = intValue(value(vehicle,
                        "strategicTransitSourceY"), vehicle.y);
                set(vehicle, "strategicTransitState", "reserved");
                return new Result(Status.RECOVERED, true, true, 0,
                        reservationId,
                        "STRATEGIC TRANSIT RECOVERED: destination fuel commit failed, so the vehicle returned to source with its reservation intact.",
                        vehicle);
            }
            set(vehicle, "strategicTransitState", "completed");
            set(vehicle, "operationState", "parked");
            append(vehicle, "deploymentHistory", "Interrupted transfer "
                    + reservationId + " completed at destination during recovery");
            return new Result(Status.RECOVERED, true, true, reserved,
                    reservationId,
                    "STRATEGIC TRANSIT RECOVERED: destination placement was retained and reserved fuel was committed.",
                    vehicle);
        }
        return Result.blocked(vehicle,
                "The interrupted vehicle is present in neither candidate world; manual save recovery is required.");
    }

    static List<String> inspectionLines(World world, MapObjectState vehicle) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Strategic transfer state: "
                + clean(value(vehicle, "strategicTransitState"), "none") + ".");
        String origin = value(vehicle, "strategicTransitLastOrigin");
        String destination = value(vehicle, "strategicTransitLastDestination");
        if (!origin.isBlank() || !destination.isBlank()) {
            lines.add("Last strategic transfer: " + clean(origin, "unknown")
                    + " to " + clean(destination, "unknown") + ".");
        }
        lines.addAll(VehicleFuelAuthority.inspectionLines(world, vehicle));
        lines.add("Transfer rule: the world-transition owner must supply both resolved worlds and a clear legal destination cell; failed commits restore source placement and fuel.");
        return List.copyOf(lines);
    }

    private static boolean legalDestination(World world,
                                            MapObjectState vehicle,
                                            int x, int y) {
        if (!world.inBounds(x, y) || world.npcAt(x, y) != null) return false;
        MapObjectState occupied = world.mapObjectAt(x, y);
        if (occupied != null && occupied != vehicle) return false;
        char tile = world.tiles[x][y];
        if (tile == RoadGridIntegrationAuthority.PARKING_SPACE) return true;
        if (tile != RoadGridIntegrationAuthority.SIDEWALK) return false;
        return road(world, x + 1, y) || road(world, x - 1, y)
                || road(world, x, y + 1) || road(world, x, y - 1);
    }

    private static boolean road(World world, int x, int y) {
        if (!world.inBounds(x, y)) return false;
        char tile = world.tiles[x][y];
        return tile == RoadGridIntegrationAuthority.ROAD_LANE
                || tile == RoadGridIntegrationAuthority.PARKING_SPACE;
    }

    private static int intValue(String value, int fallback) {
        try { return Integer.parseInt(value); }
        catch (Exception ignored) { return fallback; }
    }

    private static String displayName(MapObjectState vehicle) {
        VehicleRuntimeAuthority.Snapshot snapshot =
                VehicleRuntimeAuthority.inspect(null, vehicle);
        if (snapshot == null) return "vehicle";
        return (clean(snapshot.manufacturer(), "") + " "
                + clean(snapshot.model(), snapshot.vehicleClass().label)).trim();
    }

    private static void append(MapObjectState vehicle, String key,
                               String entry) {
        String existing = value(vehicle, key);
        set(vehicle, key, existing.isBlank() ? clean(entry, "")
                : existing + "~" + clean(entry, ""));
    }

    private static String value(MapObjectState vehicle, String key) {
        return vehicle == null ? ""
                : MapObjectState.stockValue(vehicle.stockState, key);
    }

    private static void set(MapObjectState vehicle, String key, String value) {
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                key, clean(value, "").replace(';', ',').replace('|', '/'));
    }

    private static String clean(String value, String fallback) {
        String cleaned = value == null ? ""
                : value.trim().replaceAll("\\s+", " ");
        return cleaned.isBlank() ? (fallback == null ? "" : fallback)
                : cleaned;
    }
}