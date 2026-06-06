package mechanist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Structured per-tile state for generated zones.
 *
 * Legacy maps may still export a char glyph for current render paths, but new
 * generation code should store semantic tile data here: base type, room/corridor
 * ownership, transition identity, faction ownership, lighting, objects,
 * containers, vehicles, pets, occupancy, and placement reservations.
 */
final class ZoneTileState {
    enum BaseTileType { VOID, FLOOR, WALL, ROAD, SIDEWALK, DOOR, WATER, SEWER, STAIRS, ELEVATOR, MANHOLE, DRAIN, UNKNOWN }
    enum SpaceType { UNASSIGNED, ROOM, CORRIDOR, ROAD_NETWORK, CENTRAL_PLAZA, TRANSITION_ROOM, PARKING_LOT, UTILITY_SPACE, SEWER_NETWORK, EXTERIOR_EDGE }
    enum TileFlag { BLOCKS_MOVEMENT, BLOCKS_ROOM_PLACEMENT, RESERVED, ROAD_INFRASTRUCTURE, DOOR_BUFFER, TRANSITION_ANCHOR, VERTICAL_TRANSITION, HAS_LIGHT, HAS_OBJECT, HAS_CONTAINER, HAS_ITEM, HAS_ENTITY, HAS_PET, HAS_VEHICLE, FACTION_CONTROLLED, NEUTRAL, LEGACY_IMPORTED }
    enum LightKind { NONE, AMBIENT, STREET_LIGHT, ROOM_LIGHT, WARNING_LIGHT, EMERGENCY_LIGHT, MACHINE_GLOW }
    enum TileSlot { SURFACE, SPACE, OWNER, ROOM, CORRIDOR, ROAD_NETWORK, TRANSITION, VERTICAL_TRANSITION, RESERVATION, FIXTURE, CONTAINER, LOOSE_ITEM, ENTITY, PET, VEHICLE, LIGHT, OVERLAY }

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

    static final class TileSlotRef {
        final TileSlot slot;
        final String id;
        final String typeKey;
        final String label;
        final boolean blocksMovement;
        TileSlotRef(TileSlot slot, String id, String typeKey, String label, boolean blocksMovement) {
            this.slot = slot == null ? TileSlot.OVERLAY : slot;
            this.id = safe(id);
            this.typeKey = safe(typeKey);
            this.label = safe(label);
            this.blocksMovement = blocksMovement;
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
    private final EnumMap<TileSlot, ArrayList<TileSlotRef>> slots = new EnumMap<>(TileSlot.class);

    ZoneTileState() { this(BaseTileType.VOID, ' '); }
    ZoneTileState(BaseTileType baseType, char legacyGlyph) {
        this.baseType = baseType == null ? BaseTileType.UNKNOWN : baseType;
        this.spaceType = SpaceType.UNASSIGNED;
        this.ownerFaction = Faction.NONE;
        this.legacyGlyph = legacyGlyph;
        replaceSingleSlot(TileSlot.SURFACE, Character.toString(legacyGlyph), this.baseType.name(), this.baseType.name().toLowerCase().replace('_', ' '), false);
    }

    static ZoneTileState fromLegacyGlyph(char glyph) {
        ZoneTileState state = new ZoneTileState(baseTypeForLegacyGlyph(glyph), glyph);
        state.flags.add(TileFlag.LEGACY_IMPORTED);
        if (state.baseType == BaseTileType.WALL || state.baseType == BaseTileType.VOID || state.baseType == BaseTileType.WATER) state.flags.add(TileFlag.BLOCKS_MOVEMENT);
        if (state.baseType == BaseTileType.ROAD || state.baseType == BaseTileType.SIDEWALK) {
            state.spaceType = SpaceType.ROAD_NETWORK;
            state.flags.add(TileFlag.ROAD_INFRASTRUCTURE);
            state.flags.add(TileFlag.BLOCKS_ROOM_PLACEMENT);
            state.replaceSingleSlot(TileSlot.ROAD_NETWORK, Character.toString(glyph), state.baseType.name(), "legacy road surface", false);
        }
        return state;
    }

    static BaseTileType baseTypeForLegacyGlyph(char glyph) {
        return switch (glyph) {
            case '#', '\u2588', '\u2593', '\u2592' -> BaseTileType.WALL;
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
    boolean hasSlot(TileSlot slot) { return slot != null && slots.containsKey(slot) && !slots.get(slot).isEmpty(); }
    List<PlacedObjectRef> objects() { return Collections.unmodifiableList(objects); }
    List<LightState> lights() { return Collections.unmodifiableList(lights); }
    List<TileSlotRef> slots(TileSlot slot) { return Collections.unmodifiableList(slots.getOrDefault(slot, new ArrayList<>())); }
    EnumSet<TileFlag> flags() { return flags.clone(); }

    Map<TileSlot, List<TileSlotRef>> slotSnapshot() {
        EnumMap<TileSlot, List<TileSlotRef>> copy = new EnumMap<>(TileSlot.class);
        for (Map.Entry<TileSlot, ArrayList<TileSlotRef>> entry : slots.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    ZoneTileState setBaseType(BaseTileType baseType, char legacyGlyph) {
        this.baseType = baseType == null ? BaseTileType.UNKNOWN : baseType;
        this.legacyGlyph = legacyGlyph;
        replaceSingleSlot(TileSlot.SURFACE, Character.toString(legacyGlyph), this.baseType.name(), this.baseType.name().toLowerCase().replace('_', ' '), false);
        return this;
    }
    ZoneTileState setSpaceType(SpaceType spaceType) { this.spaceType = spaceType == null ? SpaceType.UNASSIGNED : spaceType; replaceSingleSlot(TileSlot.SPACE, this.spaceType.name(), "space", this.spaceType.name().toLowerCase().replace('_', ' '), false); return this; }
    ZoneTileState setOwnerFaction(Faction ownerFaction) { this.ownerFaction = ownerFaction == null ? Faction.NONE : ownerFaction; if (this.ownerFaction == Faction.NONE) flags.add(TileFlag.NEUTRAL); else flags.add(TileFlag.FACTION_CONTROLLED); replaceSingleSlot(TileSlot.OWNER, this.ownerFaction.name(), "faction", this.ownerFaction.name().toLowerCase().replace('_', ' '), false); return this; }
    ZoneTileState markRoom(String roomId, Faction faction) { this.roomId = safe(roomId); this.spaceType = SpaceType.ROOM; replaceSingleSlot(TileSlot.ROOM, this.roomId, "room", "room " + this.roomId, false); replaceSingleSlot(TileSlot.SPACE, SpaceType.ROOM.name(), "space", "room", false); setOwnerFaction(faction); return this; }
    ZoneTileState markCorridor(String corridorId) { this.corridorId = safe(corridorId); this.spaceType = SpaceType.CORRIDOR; replaceSingleSlot(TileSlot.CORRIDOR, this.corridorId, "corridor", "corridor " + this.corridorId, false); replaceSingleSlot(TileSlot.SPACE, SpaceType.CORRIDOR.name(), "space", "corridor", false); return this; }
    ZoneTileState markRoad(String roadNetworkId) { this.roadNetworkId = safe(roadNetworkId); this.spaceType = SpaceType.ROAD_NETWORK; this.baseType = BaseTileType.ROAD; this.legacyGlyph = 'R'; replaceSingleSlot(TileSlot.SURFACE, "R", BaseTileType.ROAD.name(), "road", false); replaceSingleSlot(TileSlot.ROAD_NETWORK, this.roadNetworkId, "road network", "road network " + this.roadNetworkId, false); flags.add(TileFlag.ROAD_INFRASTRUCTURE); flags.add(TileFlag.BLOCKS_ROOM_PLACEMENT); return this; }
    ZoneTileState markTransition(String transitionId, boolean vertical) { this.transitionId = safe(transitionId); replaceSingleSlot(TileSlot.TRANSITION, this.transitionId, "transition", "transition " + this.transitionId, false); flags.add(TileFlag.TRANSITION_ANCHOR); flags.add(TileFlag.BLOCKS_ROOM_PLACEMENT); if (vertical) { this.verticalTransitionId = safe(transitionId); replaceSingleSlot(TileSlot.VERTICAL_TRANSITION, this.verticalTransitionId, "vertical transition", "vertical transition " + this.verticalTransitionId, false); flags.add(TileFlag.VERTICAL_TRANSITION); } return this; }
    ZoneTileState reserve(String label) { this.reservationLabel = safe(label); replaceSingleSlot(TileSlot.RESERVATION, this.reservationLabel, "reservation", this.reservationLabel, false); flags.add(TileFlag.RESERVED); flags.add(TileFlag.BLOCKS_ROOM_PLACEMENT); return this; }
    ZoneTileState addFlag(TileFlag flag) { if (flag != null) flags.add(flag); return this; }
    ZoneTileState removeFlag(TileFlag flag) { if (flag != null) flags.remove(flag); return this; }
    ZoneTileState addObject(String objectId, String typeKey, String label, boolean blocksMovement, boolean container) { objects.add(new PlacedObjectRef(objectId, typeKey, label, blocksMovement, container)); addSlot(container ? TileSlot.CONTAINER : TileSlot.FIXTURE, objectId, typeKey, label, blocksMovement); flags.add(TileFlag.HAS_OBJECT); if (container) flags.add(TileFlag.HAS_CONTAINER); if (blocksMovement) flags.add(TileFlag.BLOCKS_MOVEMENT); return this; }
    ZoneTileState addLooseItem(String itemId, String typeKey, String label) { addSlot(TileSlot.LOOSE_ITEM, itemId, typeKey, label, false); flags.add(TileFlag.HAS_ITEM); return this; }
    ZoneTileState addLight(LightKind kind, int intensityPercent, String sourceId) { lights.add(new LightState(kind, intensityPercent, sourceId)); if (kind != null && kind != LightKind.NONE && intensityPercent > 0) { flags.add(TileFlag.HAS_LIGHT); addSlot(TileSlot.LIGHT, sourceId, kind.name(), kind.name().toLowerCase().replace('_', ' '), false); } return this; }
    ZoneTileState setOccupantEntityId(String occupantEntityId) { this.occupantEntityId = safe(occupantEntityId); if (!this.occupantEntityId.isBlank()) { flags.add(TileFlag.HAS_ENTITY); replaceSingleSlot(TileSlot.ENTITY, this.occupantEntityId, "entity", this.occupantEntityId, true); } else { flags.remove(TileFlag.HAS_ENTITY); clearSlot(TileSlot.ENTITY); } return this; }
    ZoneTileState setPetEntityId(String petEntityId) { this.petEntityId = safe(petEntityId); if (!this.petEntityId.isBlank()) { flags.add(TileFlag.HAS_PET); replaceSingleSlot(TileSlot.PET, this.petEntityId, "pet", this.petEntityId, true); } else { flags.remove(TileFlag.HAS_PET); clearSlot(TileSlot.PET); } return this; }
    ZoneTileState setVehicleId(String vehicleId) { this.vehicleId = safe(vehicleId); if (!this.vehicleId.isBlank()) { flags.add(TileFlag.HAS_VEHICLE); replaceSingleSlot(TileSlot.VEHICLE, this.vehicleId, "vehicle", this.vehicleId, true); } else { flags.remove(TileFlag.HAS_VEHICLE); clearSlot(TileSlot.VEHICLE); } return this; }

    boolean blocksRoomPlacement() {
        return flags.contains(TileFlag.BLOCKS_ROOM_PLACEMENT)
                || flags.contains(TileFlag.RESERVED)
                || spaceType == SpaceType.ROAD_NETWORK
                || spaceType == SpaceType.CENTRAL_PLAZA
                || spaceType == SpaceType.TRANSITION_ROOM
                || spaceType == SpaceType.SEWER_NETWORK;
    }

    boolean hasFloorSpaceContent() {
        return hasSlot(TileSlot.FIXTURE)
                || hasSlot(TileSlot.CONTAINER)
                || hasSlot(TileSlot.LOOSE_ITEM)
                || hasSlot(TileSlot.ENTITY)
                || hasSlot(TileSlot.PET)
                || hasSlot(TileSlot.VEHICLE)
                || hasSlot(TileSlot.RESERVATION)
                || hasSlot(TileSlot.LIGHT);
    }

    String playerFacingSlotSummary() {
        ArrayList<String> parts = new ArrayList<>();
        parts.add(baseType.name().toLowerCase().replace('_', ' '));
        for (TileSlot slot : TileSlot.values()) {
            if (slot == TileSlot.SURFACE || slot == TileSlot.SPACE || slot == TileSlot.OWNER) continue;
            List<TileSlotRef> refs = slots(slot);
            if (!refs.isEmpty()) parts.add(slot.name().toLowerCase().replace('_', ' ') + " x" + refs.size());
        }
        return PlayerFacingText.sanitize(String.join(", ", parts));
    }

    private ZoneTileState addSlot(TileSlot slot, String id, String typeKey, String label, boolean blocksMovement) {
        if (slot == null) return this;
        slots.computeIfAbsent(slot, ignored -> new ArrayList<>()).add(new TileSlotRef(slot, id, typeKey, label, blocksMovement));
        if (blocksMovement) flags.add(TileFlag.BLOCKS_MOVEMENT);
        return this;
    }

    private ZoneTileState replaceSingleSlot(TileSlot slot, String id, String typeKey, String label, boolean blocksMovement) {
        clearSlot(slot);
        return addSlot(slot, id, typeKey, label, blocksMovement);
    }

    private ZoneTileState clearSlot(TileSlot slot) {
        if (slot != null) slots.remove(slot);
        return this;
    }

    private static String safe(String text) { return text == null ? "" : text.trim(); }
}
