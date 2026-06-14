package mechanist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic topology contract for the post-range world-generation model.
 *
 * This class deliberately does not mutate WorldAtlas/World generation yet.  It
 * is the first migration layer: all future road, door, room, sewer, stair, and
 * elevator placement code should consume this contract instead of deriving its
 * own local transition placement.
 */
final class WorldTopologyContract {
    static final int ROAD_WIDTH_TILES = 8;
    static final int DOUBLE_DOOR_WIDTH_TILES = 2;
    static final int TRANSITION_ROOM_SIZE = 18;

    private WorldTopologyContract() {}

    enum SectorSize {
        COMPACT_500(500, "Compact Sector"),
        STANDARD_600(600, "Standard Sector"),
        EXPANDED_700(700, "Expanded Sector"),
        LARGE_800(800, "Large Sector"),
        HUGE_900(900, "Huge Sector"),
        ULTRA_HUGE_1000(1000, "Ultra-Huge Sector");

        final int tiles;
        final String label;

        SectorSize(int tiles, String label) {
            this.tiles = tiles;
            this.label = label;
        }

        static SectorSize fromTiles(int tiles) {
            for (SectorSize size : values()) if (size.tiles == tiles) return size;
            throw new IllegalArgumentException("Unsupported fixed sector size " + tiles + "; allowed values are 500, 600, 700, 800, 900, 1000.");
        }

        String displayLabel() { return label + " (" + tiles + " x " + tiles + ")"; }
    }

    enum CardinalExit {
        NORTH,
        SOUTH,
        EAST,
        WEST;

        CardinalExit opposite() {
            return switch (this) {
                case NORTH -> SOUTH;
                case SOUTH -> NORTH;
                case EAST -> WEST;
                case WEST -> EAST;
            };
        }
    }

    enum VerticalTransitionKind {
        ELEVATOR_BOTTOM,
        ELEVATOR_MIDDLE,
        ELEVATOR_TOP,
        STAIR_BOTTOM_GOES_UP,
        STAIR_TOP_GOES_DOWN,
        MANHOLE_DOWN_TO_SEWER,
        DRAIN_DOWN_FROM_SEWER
    }

    enum TransitionRoomKind {
        ROAD_EDGE_GATE,
        ELEVATOR_ROOM,
        STAIRWELL_ROOM,
        MANHOLE_ROOM,
        DRAIN_OUTFLOW_ROOM
    }

    enum TransitionRoomOwnership {
        NEUTRAL,
        SECURED_FACTION
    }

    record TilePoint(int x, int y) {
        TilePoint clampInside(int size) {
            return new TilePoint(Math.max(0, Math.min(size - 1, x)), Math.max(0, Math.min(size - 1, y)));
        }
    }

    record TransitionRoomAnchor(
            TransitionRoomKind kind,
            TransitionRoomOwnership ownership,
            TilePoint center,
            int width,
            int height
    ) {
        int left() { return center.x - width / 2; }
        int top() { return center.y - height / 2; }
        int right() { return left() + width - 1; }
        int bottom() { return top() + height - 1; }
    }

    record EdgeTransitionAnchor(
            CardinalExit direction,
            int sectorSize,
            TilePoint roadCenter,
            TilePoint firstDoorTile,
            TilePoint secondDoorTile,
            TransitionRoomAnchor room
    ) {
        EdgeTransitionAnchor matchingNeighborEntrance() {
            int center = sectorSize / 2;
            CardinalExit opposite = direction.opposite();
            return edgeAnchor(opposite, SectorSize.fromTiles(sectorSize), center);
        }

        boolean isDoubleDoorCenteredInRoad() {
            if (direction == CardinalExit.NORTH || direction == CardinalExit.SOUTH) {
                return firstDoorTile.y == secondDoorTile.y
                        && Math.abs(firstDoorTile.x - secondDoorTile.x) == 1
                        && firstDoorTile.x + secondDoorTile.x == roadCenter.x * 2 - 1;
            }
            return firstDoorTile.x == secondDoorTile.x
                    && Math.abs(firstDoorTile.y - secondDoorTile.y) == 1
                    && firstDoorTile.y + secondDoorTile.y == roadCenter.y * 2 - 1;
        }
    }

    record VerticalTransitionAnchor(
            String groupId,
            VerticalTransitionKind kind,
            int floor,
            TilePoint position,
            TransitionRoomAnchor room
    ) {
        boolean isElevator() {
            return kind == VerticalTransitionKind.ELEVATOR_BOTTOM
                    || kind == VerticalTransitionKind.ELEVATOR_MIDDLE
                    || kind == VerticalTransitionKind.ELEVATOR_TOP;
        }

        boolean isStair() {
            return kind == VerticalTransitionKind.STAIR_BOTTOM_GOES_UP
                    || kind == VerticalTransitionKind.STAIR_TOP_GOES_DOWN;
        }
    }

    record ZoneTransitionPlan(
            long worldSeed,
            int zoneX,
            int zoneY,
            int floor,
            SectorSize sectorSize,
            TilePoint centralPlaza,
            Map<CardinalExit, EdgeTransitionAnchor> cardinalExits,
            List<VerticalTransitionAnchor> verticalTransitions
    ) {
        int mapWidth() { return sectorSize.tiles; }
        int mapHeight() { return sectorSize.tiles; }
        EdgeTransitionAnchor exit(CardinalExit direction) { return cardinalExits.get(direction); }
        List<TransitionRoomAnchor> requiredPreplacementRooms() {
            ArrayList<TransitionRoomAnchor> rooms = new ArrayList<>();
            for (EdgeTransitionAnchor edge : cardinalExits.values()) rooms.add(edge.room);
            for (VerticalTransitionAnchor vertical : verticalTransitions) rooms.add(vertical.room);
            return Collections.unmodifiableList(rooms);
        }

        String auditSummary() {
            return "seed=" + worldSeed
                    + " zone=" + zoneX + "," + zoneY
                    + " floor=" + floor
                    + " size=" + sectorSize.displayLabel()
                    + " exits=" + cardinalExits.keySet()
                    + " vertical=" + verticalTransitions.size();
        }
    }

    static ZoneTransitionPlan planFor(long worldSeed, int zoneX, int zoneY, int floor, SectorSize sectorSize) {
        SectorSize safeSize = sectorSize == null ? SectorSize.STANDARD_600 : sectorSize;
        int size = safeSize.tiles;
        int center = size / 2;
        TilePoint plaza = new TilePoint(center, center);
        EnumMap<CardinalExit, EdgeTransitionAnchor> exits = new EnumMap<>(CardinalExit.class);
        for (CardinalExit direction : CardinalExit.values()) exits.put(direction, edgeAnchor(direction, safeSize, center));
        List<VerticalTransitionAnchor> vertical = verticalAnchors(worldSeed, zoneX, zoneY, floor, safeSize, plaza);
        return new ZoneTransitionPlan(worldSeed, zoneX, zoneY, floor, safeSize, plaza, Collections.unmodifiableMap(exits), Collections.unmodifiableList(vertical));
    }

    static EdgeTransitionAnchor edgeAnchor(CardinalExit direction, SectorSize sectorSize, int center) {
        int size = sectorSize.tiles;
        TilePoint roadCenter;
        TilePoint first;
        TilePoint second;
        TransitionRoomAnchor room;
        int roomOffset = TRANSITION_ROOM_SIZE / 2 + 2;
        switch (direction) {
            case NORTH -> {
                roadCenter = new TilePoint(center, 0);
                first = new TilePoint(center - 1, 0);
                second = new TilePoint(center, 0);
                room = new TransitionRoomAnchor(TransitionRoomKind.ROAD_EDGE_GATE, TransitionRoomOwnership.NEUTRAL, new TilePoint(center, roomOffset), TRANSITION_ROOM_SIZE, TRANSITION_ROOM_SIZE);
            }
            case SOUTH -> {
                roadCenter = new TilePoint(center, size - 1);
                first = new TilePoint(center - 1, size - 1);
                second = new TilePoint(center, size - 1);
                room = new TransitionRoomAnchor(TransitionRoomKind.ROAD_EDGE_GATE, TransitionRoomOwnership.NEUTRAL, new TilePoint(center, size - 1 - roomOffset), TRANSITION_ROOM_SIZE, TRANSITION_ROOM_SIZE);
            }
            case EAST -> {
                roadCenter = new TilePoint(size - 1, center);
                first = new TilePoint(size - 1, center - 1);
                second = new TilePoint(size - 1, center);
                room = new TransitionRoomAnchor(TransitionRoomKind.ROAD_EDGE_GATE, TransitionRoomOwnership.NEUTRAL, new TilePoint(size - 1 - roomOffset, center), TRANSITION_ROOM_SIZE, TRANSITION_ROOM_SIZE);
            }
            case WEST -> {
                roadCenter = new TilePoint(0, center);
                first = new TilePoint(0, center - 1);
                second = new TilePoint(0, center);
                room = new TransitionRoomAnchor(TransitionRoomKind.ROAD_EDGE_GATE, TransitionRoomOwnership.NEUTRAL, new TilePoint(roomOffset, center), TRANSITION_ROOM_SIZE, TRANSITION_ROOM_SIZE);
            }
            default -> throw new IllegalStateException("Unhandled direction " + direction);
        }
        return new EdgeTransitionAnchor(direction, size, roadCenter, first, second, room);
    }

    static List<VerticalTransitionAnchor> verticalAnchors(long worldSeed, int zoneX, int zoneY, int floor, SectorSize sectorSize, TilePoint plaza) {
        int size = sectorSize.tiles;
        ArrayList<VerticalTransitionAnchor> anchors = new ArrayList<>();
        int rangeStart = floor - Math.floorMod(floor, 3);
        anchors.add(new VerticalTransitionAnchor(
                "elevator:" + worldSeed + ":" + zoneX + ":" + zoneY + ":" + rangeStart,
                elevatorKindForFloor(floor),
                floor,
                plazaOffset(plaza, 24, -18, size),
                new TransitionRoomAnchor(TransitionRoomKind.ELEVATOR_ROOM, TransitionRoomOwnership.SECURED_FACTION, plazaOffset(plaza, 24, -18, size), TRANSITION_ROOM_SIZE, TRANSITION_ROOM_SIZE)
        ));
        anchors.add(new VerticalTransitionAnchor(
                "stair-up:" + worldSeed + ":" + zoneX + ":" + zoneY + ":" + floor,
                VerticalTransitionKind.STAIR_BOTTOM_GOES_UP,
                floor,
                plazaOffset(plaza, -24, -18, size),
                new TransitionRoomAnchor(TransitionRoomKind.STAIRWELL_ROOM, TransitionRoomOwnership.NEUTRAL, plazaOffset(plaza, -24, -18, size), TRANSITION_ROOM_SIZE, TRANSITION_ROOM_SIZE)
        ));
        anchors.add(new VerticalTransitionAnchor(
                "stair-down:" + worldSeed + ":" + zoneX + ":" + zoneY + ":" + (floor - 1),
                VerticalTransitionKind.STAIR_TOP_GOES_DOWN,
                floor,
                plazaOffset(plaza, -24, 18, size),
                new TransitionRoomAnchor(TransitionRoomKind.STAIRWELL_ROOM, TransitionRoomOwnership.NEUTRAL, plazaOffset(plaza, -24, 18, size), TRANSITION_ROOM_SIZE, TRANSITION_ROOM_SIZE)
        ));
        anchors.add(new VerticalTransitionAnchor(
                "manhole:" + worldSeed + ":" + zoneX + ":" + zoneY + ":" + floor,
                VerticalTransitionKind.MANHOLE_DOWN_TO_SEWER,
                floor,
                plazaOffset(plaza, 0, 32, size),
                new TransitionRoomAnchor(TransitionRoomKind.MANHOLE_ROOM, TransitionRoomOwnership.NEUTRAL, plazaOffset(plaza, 0, 32, size), TRANSITION_ROOM_SIZE, TRANSITION_ROOM_SIZE)
        ));
        anchors.add(new VerticalTransitionAnchor(
                "drain:" + worldSeed + ":" + zoneX + ":" + zoneY + ":" + floor,
                VerticalTransitionKind.DRAIN_DOWN_FROM_SEWER,
                floor,
                plazaOffset(plaza, 32, 32, size),
                new TransitionRoomAnchor(TransitionRoomKind.DRAIN_OUTFLOW_ROOM, TransitionRoomOwnership.NEUTRAL, plazaOffset(plaza, 32, 32, size), TRANSITION_ROOM_SIZE, TRANSITION_ROOM_SIZE)
        ));
        return anchors;
    }

    static VerticalTransitionKind elevatorKindForFloor(int floor) {
        return switch (Math.floorMod(floor, 3)) {
            case 0 -> VerticalTransitionKind.ELEVATOR_BOTTOM;
            case 1 -> VerticalTransitionKind.ELEVATOR_MIDDLE;
            default -> VerticalTransitionKind.ELEVATOR_TOP;
        };
    }

    static TilePoint plazaOffset(TilePoint plaza, int dx, int dy, int size) {
        return new TilePoint(plaza.x + dx, plaza.y + dy).clampInside(size);
    }
}
