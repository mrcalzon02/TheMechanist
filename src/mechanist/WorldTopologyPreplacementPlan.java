package mechanist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Converts a ZoneTransitionPlan into concrete pre-placement reservations.
 *
 * This is the second incremental topology layer.  The actual generator can adopt
 * these reservations in order before faction/ordinary room expansion, without
 * needing to rediscover plaza, road, door, elevator, stair, manhole, or drain
 * placement independently.
 */
final class WorldTopologyPreplacementPlan {
    private WorldTopologyPreplacementPlan() {}

    enum ReservationKind {
        CENTRAL_PLAZA,
        CARDINAL_ROAD,
        EDGE_DOUBLE_DOOR,
        EDGE_TRANSITION_ROOM,
        ELEVATOR_TRANSITION_ROOM,
        STAIRWELL_TRANSITION_ROOM,
        MANHOLE_TRANSITION_ROOM,
        DRAIN_OUTFLOW_ROOM
    }

    record TileRect(int x, int y, int width, int height) {
        int right() { return x + width - 1; }
        int bottom() { return y + height - 1; }
        boolean contains(int px, int py) { return px >= x && py >= y && px <= right() && py <= bottom(); }
        TileRect clampInside(int size) {
            int nx = Math.max(0, Math.min(size - 1, x));
            int ny = Math.max(0, Math.min(size - 1, y));
            int nr = Math.max(nx, Math.min(size - 1, right()));
            int nb = Math.max(ny, Math.min(size - 1, bottom()));
            return new TileRect(nx, ny, Math.max(1, nr - nx + 1), Math.max(1, nb - ny + 1));
        }
    }

    record Reservation(
            ReservationKind kind,
            String id,
            int priority,
            TileRect bounds,
            WorldTopologyContract.TransitionRoomOwnership ownership,
            String note
    ) {}

    record Plan(
            WorldTopologyContract.ZoneTransitionPlan topology,
            List<Reservation> reservations
    ) {
        String auditSummary() {
            int roads = 0;
            int doors = 0;
            int rooms = 0;
            for (Reservation r : reservations) {
                if (r.kind == ReservationKind.CARDINAL_ROAD) roads++;
                if (r.kind == ReservationKind.EDGE_DOUBLE_DOOR) doors++;
                if (r.kind.name().endsWith("ROOM")) rooms++;
            }
            return topology.auditSummary() + " preplacements=" + reservations.size()
                    + " roads=" + roads
                    + " doorPairs=" + doors
                    + " rooms=" + rooms;
        }
    }

    static Plan fromTopology(WorldTopologyContract.ZoneTransitionPlan topology) {
        if (topology == null) throw new IllegalArgumentException("topology plan is required");
        ArrayList<Reservation> out = new ArrayList<>();
        int size = topology.sectorSize().tiles;
        WorldTopologyContract.TilePoint plaza = topology.centralPlaza();
        int plazaSize = Math.max(32, WorldTopologyContract.TRANSITION_ROOM_SIZE * 2);
        out.add(new Reservation(
                ReservationKind.CENTRAL_PLAZA,
                "central-plaza",
                0,
                centeredRect(plaza, plazaSize, plazaSize).clampInside(size),
                WorldTopologyContract.TransitionRoomOwnership.NEUTRAL,
                "Central plaza is the required road and transition-room reference point."
        ));

        for (WorldTopologyContract.CardinalExit direction : WorldTopologyContract.CardinalExit.values()) {
            WorldTopologyContract.EdgeTransitionAnchor edge = topology.exit(direction);
            if (edge == null) continue;
            out.add(new Reservation(
                    ReservationKind.CARDINAL_ROAD,
                    "road-" + direction.name().toLowerCase(),
                    10,
                    roadRect(plaza, edge.roadCenter(), direction, size),
                    WorldTopologyContract.TransitionRoomOwnership.NEUTRAL,
                    "Road must connect central plaza to the " + direction + " edge gate."
            ));
            out.add(new Reservation(
                    ReservationKind.EDGE_TRANSITION_ROOM,
                    "edge-room-" + direction.name().toLowerCase(),
                    20,
                    roomRect(edge.room()).clampInside(size),
                    edge.room().ownership(),
                    "Road infrastructure transition room before ordinary room/faction expansion."
            ));
            out.add(new Reservation(
                    ReservationKind.EDGE_DOUBLE_DOOR,
                    "edge-door-" + direction.name().toLowerCase(),
                    25,
                    doorRect(edge, size),
                    WorldTopologyContract.TransitionRoomOwnership.NEUTRAL,
                    "Double door centered on the road and paired with the opposite neighboring zone."
            ));
        }

        int ordinal = 0;
        for (WorldTopologyContract.VerticalTransitionAnchor vertical : topology.verticalTransitions()) {
            ReservationKind kind = verticalKind(vertical);
            out.add(new Reservation(
                    kind,
                    vertical.groupId() + ":" + ordinal++,
                    30,
                    roomRect(vertical.room()).clampInside(size),
                    vertical.room().ownership(),
                    verticalNote(vertical)
            ));
        }

        out.sort((a, b) -> Integer.compare(a.priority, b.priority));
        return new Plan(topology, Collections.unmodifiableList(out));
    }

    static ReservationKind verticalKind(WorldTopologyContract.VerticalTransitionAnchor vertical) {
        if (vertical == null) return ReservationKind.STAIRWELL_TRANSITION_ROOM;
        if (vertical.isElevator()) return ReservationKind.ELEVATOR_TRANSITION_ROOM;
        if (vertical.isStair()) return ReservationKind.STAIRWELL_TRANSITION_ROOM;
        if (vertical.kind() == WorldTopologyContract.VerticalTransitionKind.MANHOLE_DOWN_TO_SEWER) return ReservationKind.MANHOLE_TRANSITION_ROOM;
        if (vertical.kind() == WorldTopologyContract.VerticalTransitionKind.DRAIN_DOWN_FROM_SEWER) return ReservationKind.DRAIN_OUTFLOW_ROOM;
        return ReservationKind.STAIRWELL_TRANSITION_ROOM;
    }

    static String verticalNote(WorldTopologyContract.VerticalTransitionAnchor vertical) {
        if (vertical == null) return "Vertical transition reservation.";
        return switch (vertical.kind()) {
            case ELEVATOR_BOTTOM -> "Elevator bottom stop; must match middle/top stops in this three-floor range.";
            case ELEVATOR_MIDDLE -> "Elevator middle stop; must match bottom/top stops in this three-floor range.";
            case ELEVATOR_TOP -> "Elevator top stop; must match bottom/middle stops in this three-floor range.";
            case STAIR_BOTTOM_GOES_UP -> "Bottom stair accesses the floor above.";
            case STAIR_TOP_GOES_DOWN -> "Top stair accesses the floor below.";
            case MANHOLE_DOWN_TO_SEWER -> "Manhole accesses the sewer sub-floor below.";
            case DRAIN_DOWN_FROM_SEWER -> "Drain exits to the floor below in a predictable open/neutral tile.";
        };
    }

    static TileRect roomRect(WorldTopologyContract.TransitionRoomAnchor room) {
        return new TileRect(room.left(), room.top(), room.width(), room.height());
    }

    static TileRect centeredRect(WorldTopologyContract.TilePoint center, int width, int height) {
        return new TileRect(center.x() - width / 2, center.y() - height / 2, width, height);
    }

    static TileRect doorRect(WorldTopologyContract.EdgeTransitionAnchor edge, int size) {
        int minX = Math.min(edge.firstDoorTile().x(), edge.secondDoorTile().x());
        int minY = Math.min(edge.firstDoorTile().y(), edge.secondDoorTile().y());
        int maxX = Math.max(edge.firstDoorTile().x(), edge.secondDoorTile().x());
        int maxY = Math.max(edge.firstDoorTile().y(), edge.secondDoorTile().y());
        return new TileRect(minX, minY, maxX - minX + 1, maxY - minY + 1).clampInside(size);
    }

    static TileRect roadRect(WorldTopologyContract.TilePoint plaza, WorldTopologyContract.TilePoint edge, WorldTopologyContract.CardinalExit direction, int size) {
        int half = Math.max(1, WorldTopologyContract.ROAD_WIDTH_TILES / 2);
        return switch (direction) {
            case NORTH -> new TileRect(plaza.x() - half, 0, WorldTopologyContract.ROAD_WIDTH_TILES, plaza.y() + 1).clampInside(size);
            case SOUTH -> new TileRect(plaza.x() - half, plaza.y(), WorldTopologyContract.ROAD_WIDTH_TILES, size - plaza.y()).clampInside(size);
            case EAST -> new TileRect(plaza.x(), plaza.y() - half, size - plaza.x(), WorldTopologyContract.ROAD_WIDTH_TILES).clampInside(size);
            case WEST -> new TileRect(0, plaza.y() - half, plaza.x() + 1, WorldTopologyContract.ROAD_WIDTH_TILES).clampInside(size);
        };
    }

    static void audit(WorldTopologyContract.ZoneTransitionPlan topology, String source) {
        try {
            Plan plan = fromTopology(topology);
            DebugLog.audit("WORLD_TOPOLOGY_PREPLACEMENT", "source=" + safe(source) + " " + plan.auditSummary());
        } catch (Throwable t) {
            DebugLog.warn("WORLD_TOPOLOGY_PREPLACEMENT", "Could not build preplacement plan from " + safe(source) + ": " + t.getMessage());
        }
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unspecified" : value.replace('\n', ' ').trim();
    }
}
