package mechanist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * Structured per-tile state for generated zones.
 *
 * Legacy maps may still export a char glyph for current render paths, but new
 * generation code should store semantic tile data here: base type, room/corridor
 * ownership, transition identity, faction ownership, lighting, objects,
 * containers, vehicles, pets, occupancy, and placement reservations.
 */
final class ZoneTileState {
    enum BaseTileType {
        VOID,
        FLOOR,
        WALL,
        ROAD,
        SIDEWALK,
        DOOR,
        WATER,
        SEWER,
        STAIRS,
        ELEVATOR,
        MANHOLE,
        DRAIN,
        UNKNOWN
    }

    enum SpaceType {
        UNASSIGNED,
        ROOM,
        CORRIDOR,
        ROAD_NETWORK,
        CENTRAL_PLAZA,
        TRANSITION_ROOM,
        PARKING_LOT,
        UTILITY_SPACE,
        SEWER_NETWORK,
        EXTERIOR_EDGE
    }

    enum TileFlag {
        BLOCKS_MOVEMENT,
        BLOCKS_ROOM_PLACEMENT,
        RESERVED,
        ROAD_INFRASTRUCTURE,
        DOOR_BUFFER,
        TRANSITION_ANCHOR,
        VERTICAL_TRANSITION,
        HAS_LIGHT,
        HAS_OBJECT,
        HAS_CONTAINER,
        HAS_ENTITY,
        HAS_PET,
        HAS_VEHICLE,
        FACTION_CONTROLLED,
        NEUTRAL,
        LEGACY_IMPORTED
    }

    enum LightKind {
        NONE,
        AMBIENT,
        STREET_LIGHT,
        ROOM_LIGHT,
        WARNING_LIGHT,
        EMERGENCY_LIGHT,
        MACHINE_GLOW
    }

    static final class PlacedObjectRef {
        final String objectId;
        final String typeKey;
        final String label;
        final boolean blocksMovement;
        final boolean container;

        PlacedObjectRef(String objectId, String typeKey, String label, boolean blocksMovement, boolean container) {
            this.objectId = safe(objectId);
            this.typeKey = safe(typeKey);
            this.label = safe(label);
            this.blocksMovement = blocksMovement;
            this.container = container;
        }
    }

    static final class LightState {
        final LightKind kind;
        final int intensityPercent;
        final String sourceId;

        LightState(LightKind kind, int intensityPercent, String sourceId) {
            this.kind = kind == null ? LightKind.NONE : kind;
            this.intensityPercent = Math.max(0, Math.min(100, intensityPercent));
            this.sourceId = safe(sourceId);
        }
    }

    private BaseTileType baseType;
    private SpaceType spaceType;
    private Faction ownerFaction;
    private char legacyGlyph;
    private String roomId;
    private String corridorId;
    private String roadNetworkId;
    private String transitionId;
    private String verticalTransitionId;
    private String occupantEntityId;
    private String petEntityId;
    private String vehicleId;
    private String reservationLabel;
    private final EnumSet<TileFlag> flags = EnumSet.noneOf(TileFlag.class);
    private final ArrayList<PlacedObjectRef> objects = new ArrayList<>();
    private final ArrayList<LightState> lights = new ArrayList<>();

    ZoneTileState() {
        this(BaseTileType.VOID, ' ');
    }

    ZoneTileState(BaseTileType baseType, char legacyGlyph) {
        this.baseType = baseType == null ? BaseTileType.UNKNOWN : baseType;
        this.spaceType = SpaceType.UNASSIGNED;
        this.ownerFaction = Faction.NONE;
        this.legacyGlyph = legacyGlyph;
    }

    static ZoneTileState fromLegacyGlyph(char glyph) {
        ZoneTileState state = new ZoneTileState(baseTypeForLegacyGlyph(glyph), glyph);
        state.flags.add(TileFlag.LEGACY_IMPORTED);
        if (state.baseType == BaseTileType.WALL || state.baseType == BaseTileType.VOID || state.baseType == BaseTileType.WATER) state.flags.add(TileFlag.BLOCKS_MOVEMENT);
        if (state.baseType == BaseTileType.ROAD || state.baseType == BaseTileType.SIDEWALK) {
            state.spaceType = SpaceType.ROAD_NETWORK;
            state.flags.add(TileFlag.ROAD_INFRASTRUCTURE);
            state.flags.add(TileFlag.BLOCKS_ROOM_PLACEMENT);
        }
        return state;
    }

    static BaseTileType baseTypeForLegacyGlyph(char glyph) {
        return switch (glyph) {
            case '#', '█', '▓', '▒' -> BaseTileType.WALL;
            case '.', ',', ':', ';', '=' -> BaseTileType.FLOOR;
            case '+', 'D', '/', '\\' -> BaseTileType.DOOR;
            case '~' -> BaseTileType.WATER;
            case 'R', 'r' -> BaseTileType.ROAD;
            case 'S' -> BaseTileType.SIDEWALK;
            case '^', 'v' -> BaseTileType.STAIRS;
            case 'E' -> BaseTileType.ELEVATOR;
            case 'M' -> BaseTileType.MANHOLE;
            case 'd' -> BaseTileType.DRAIN;
            case ' ' -> BaseTileType.VOID;
            default -> BaseTileType.UNKNOWN;
        };
    }

    BaseTileType baseType() { return baseType; }
    SpaceType spaceType() { return spaceType; }
    Faction ownerFaction() { return ownerFaction; }
    char legacyGlyph() { return legacyGlyph; }
    String roomId() { return roomId; }
    String corridorId() { return corridorId; }
    String roadNetworkId() { return roadNetworkId; }
    String transitionId() { return transitionId; }
    String verticalTransitionId() { return verticalTransitionId; }
    String occupantEntityId() { return occupantEntityId; }
    String petEntityId() { return petEntityId; }
    String vehicleId() { return vehicleId; }
    String reservationLabel() { return reservationLabel; }
    boolean hasFlag(TileFlag flag) { return flag != null && flags.contains(flag); }
    List<PlacedObjectRef> objects() { return Collections.unmodifiableList(objects); }
    List<LightState> lights() { return Collections.unmodifiableList(lights); }
    EnumSet<TileFlag> flags() { return flags.clone(); }

    ZoneTileState setBaseType(BaseTileType baseType, char legacyGlyph) {
        this.baseType = baseType == null ? BaseTileType.UNKNOWN : baseType;
        this.legacyGlyph = legacyGlyph;
        return this;
    }

    ZoneTileState setSpaceType(SpaceType spaceType) {
        this.spaceType = spaceType == null ? SpaceType.UNASSIGNED : spaceType;
        return this;
    }

    ZoneTileState setOwnerFaction(Faction ownerFaction) {
        this.ownerFaction = ownerFaction == null ? Faction.NONE : ownerFaction;
        if (this.ownerFaction == Faction.NONE) flags.add(TileFlag.NEUTRAL); else flags.add(TileFlag.FACTION_CONTROLLED);
        return this;
    }

    ZoneTileState markRoom(String roomId, Faction faction) {
        this.roomId = safe(roomId);
        this.spaceType = SpaceType.ROOM;
        setOwnerFaction(faction);
        return this;
    }

    ZoneTileState markCorridor(String corridorId) {
        this.corridorId = safe(corridorId);
        this.spaceType = SpaceType.CORRIDOR;
        return this;
    }

    ZoneTileState markRoad(String roadNetworkId) {
        this.roadNetworkId = safe(roadNetworkId);
        this.spaceType = SpaceType.ROAD_NETWORK;
        this.baseType = BaseTileType.ROAD;
        this.legacyGlyph = 'R';
        flags.add(TileFlag.ROAD_INFRASTRUCTURE);
        flags.add(TileFlag.BLOCKS_ROOM_PLACEMENT);
        return this;
    }

    ZoneTileState markTransition(String transitionId, boolean vertical) {
        this.transitionId = safe(transitionId);
        flags.add(TileFlag.TRANSITION_ANCHOR);
        flags.add(TileFlag.BLOCKS_ROOM_PLACEMENT);
        if (vertical) {
            this.verticalTransitionId = safe(transitionId);
            flags.add(TileFlag.VERTICAL_TRANSITION);
        }
        return this;
    }

    ZoneTileState reserve(String label) {
        this.reservationLabel = safe(label);
        flags.add(TileFlag.RESERVED);
        flags.add(TileFlag.BLOCKS_ROOM_PLACEMENT);
        return this;
    }

    ZoneTileState addFlag(TileFlag flag) {
        if (flag != null) flags.add(flag);
        return this;
    }

    ZoneTileState removeFlag(TileFlag flag) {
        if (flag != null) flags.remove(flag);
        return this;
    }

    ZoneTileState addObject(String objectId, String typeKey, String label, boolean blocksMovement, boolean container) {
        objects.add(new PlacedObjectRef(objectId, typeKey, label, blocksMovement, container));
        flags.add(TileFlag.HAS_OBJECT);
        if (container) flags.add(TileFlag.HAS_CONTAINER);
        if (blocksMovement) flags.add(TileFlag.BLOCKS_MOVEMENT);
        return this;
    }

    ZoneTileState addLight(LightKind kind, int intensityPercent, String sourceId) {
        lights.add(new LightState(kind, intensityPercent, sourceId));
        if (kind != null && kind != LightKind.NONE && intensityPercent > 0) flags.add(TileFlag.HAS_LIGHT);
        return this;
    }

    ZoneTileState setOccupantEntityId(String occupantEntityId) {
        this.occupantEntityId = safe(occupantEntityId);
        if (!this.occupantEntityId.isBlank()) flags.add(TileFlag.HAS_ENTITY); else flags.remove(TileFlag.HAS_ENTITY);
        return this;
    }

    ZoneTileState setPetEntityId(String petEntityId) {
        this.petEntityId = safe(petEntityId);
        if (!this.petEntityId.isBlank()) flags.add(TileFlag.HAS_PET); else flags.remove(TileFlag.HAS_PET);
        return this;
    }

    ZoneTileState setVehicleId(String vehicleId) {
        this.vehicleId = safe(vehicleId);
        if (!this.vehicleId.isBlank()) flags.add(TileFlag.HAS_VEHICLE); else flags.remove(TileFlag.HAS_VEHICLE);
        return this;
    }

    boolean blocksRoomPlacement() {
        return flags.contains(TileFlag.BLOCKS_ROOM_PLACEMENT)
                || flags.contains(TileFlag.RESERVED)
                || spaceType == SpaceType.ROAD_NETWORK
                || spaceType == SpaceType.CENTRAL_PLAZA
                || spaceType == SpaceType.TRANSITION_ROOM
                || spaceType == SpaceType.SEW ER_NETWORK;
    }

    private static String safe(String text) {
        return text == null ? "" : text.trim();
    }
}
