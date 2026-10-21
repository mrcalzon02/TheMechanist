package mechanist;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Canonical, tile-independent 3D building blueprints. All rectangles use local
 * footprint coordinates: the same room/shaft geometry is reused on every floor.
 * Road-block siting and actual tile stamping must validate the entire stack
 * before using one of these blueprints; creating a blueprint paints no tiles.
 */
final class VerticalBuildingBlueprintAuthority {
    static final int FIRST_FLOOR = 1;
    static final int LAST_FLOOR = 10;

    enum Kind { APARTMENT_STACK, APARTMENT_COURT, ATRIUM_HALL }
    enum Use {
        APARTMENT, SHARED_SERVICE, LOBBY, GALLERY_ROOM, ATRIUM_FLOOR,
        CORRIDOR, STAIRWELL, ELEVATOR, BALCONY, OPEN_TO_BELOW;
        boolean countedRoom() {
            return this == APARTMENT || this == SHARED_SERVICE || this == LOBBY
                    || this == GALLERY_ROOM || this == ATRIUM_FLOOR;
        }
        boolean hasWalkingSurface() { return this != OPEN_TO_BELOW; }
    }
    enum LinkKind { STAIRS, LIFT }

    record Rect(int x, int y, int width, int height) {
        Rect {
            if (x < 0 || y < 0 || width <= 0 || height <= 0)
                throw new IllegalArgumentException("invalid local building rectangle");
        }
        boolean contains(Rect inner) {
            return inner != null && x <= inner.x && y <= inner.y
                    && x + width >= inner.x + inner.width
                    && y + height >= inner.y + inner.height;
        }
        boolean intersects(Rect other) {
            return other != null && x < other.x + other.width
                    && other.x < x + width && y < other.y + other.height
                    && other.y < y + height;
        }
    }

    record Space(String id, int floor, Use use, Rect bounds) {
        Space {
            if (id == null || id.isBlank() || use == null || bounds == null)
                throw new IllegalArgumentException("building space requires identity, kind and bounds");
        }
    }

    record FloorPlate(int floor, List<Space> spaces) {
        FloorPlate {
            spaces = List.copyOf(spaces);
            for (Space s : spaces)
                if (s.floor() != floor) throw new IllegalArgumentException("space on wrong floor");
        }
        int roomCount() {
            int count = 0;
            for (Space space : spaces) if (space.use().countedRoom()) count++;
            return count;
        }
        Space space(Use use) {
            for (Space space : spaces) if (space.use() == use) return space;
            return null;
        }
    }

    record VerticalLink(String id, LinkKind kind, int lowerFloor, int upperFloor, Rect shaft) {
        VerticalLink {
            if (id == null || id.isBlank() || kind == null || shaft == null
                    || upperFloor != lowerFloor + 1)
                throw new IllegalArgumentException("vertical links connect exactly two adjacent floors");
        }
    }

    record Blueprint(
            String buildingId, Kind kind, int sectorX, int sectorY, int zoneX, int zoneY,
            int firstFloor, int lastFloor, Rect footprint,
            List<FloorPlate> plates, List<VerticalLink> connections
    ) {
        Blueprint {
            plates = List.copyOf(plates);
            connections = List.copyOf(connections);
            if (buildingId == null || buildingId.isBlank() || kind == null || footprint == null
                    || firstFloor < FIRST_FLOOR || lastFloor > LAST_FLOOR || firstFloor > lastFloor
                    || plates.size() != lastFloor - firstFloor + 1)
                throw new IllegalArgumentException("invalid building floor span");
            Set<String> spaceIds = new HashSet<>();
            for (int index = 0; index < plates.size(); index++) {
                FloorPlate plate = plates.get(index);
                if (plate.floor() != firstFloor + index || plate.spaces().isEmpty())
                    throw new IllegalArgumentException("missing or unordered building floor");
                for (Space space : plate.spaces()) {
                    if (!footprint.contains(space.bounds()) || !spaceIds.add(space.id()))
                        throw new IllegalArgumentException("out-of-footprint or duplicate space " + space.id());
                    for (Space other : plate.spaces()) {
                        if (space != other && space.bounds().intersects(other.bounds()))
                            throw new IllegalArgumentException("overlapping building spaces " + space.id());
                    }
                    if (space.use() == Use.OPEN_TO_BELOW) {
                        if (plate.floor() == firstFloor)
                            throw new IllegalArgumentException("open-to-below on bottom floor");
                        FloorPlate below = plates.get(index - 1);
                        boolean supportedVolume = false;
                        for (Space lower : below.spaces()) {
                            if (lower.bounds().equals(space.bounds())
                                    && (lower.use() == Use.ATRIUM_FLOOR
                                    || lower.use() == Use.OPEN_TO_BELOW)) supportedVolume = true;
                        }
                        if (!supportedVolume)
                            throw new IllegalArgumentException("vertical opening has no aligned lower volume");
                    }
                }
            }
            Set<String> linkIds = new HashSet<>();
            for (VerticalLink link : connections) {
                if (!linkIds.add(link.id()) || link.lowerFloor() < firstFloor
                        || link.upperFloor() > lastFloor || !footprint.contains(link.shaft()))
                    throw new IllegalArgumentException("invalid vertical connection");
                Use expected = link.kind() == LinkKind.STAIRS ? Use.STAIRWELL : Use.ELEVATOR;
                for (int floor = link.lowerFloor(); floor <= link.upperFloor(); floor++) {
                    boolean found = false;
                    for (Space space : plates.get(floor - firstFloor).spaces())
                        if (space.use() == expected && space.bounds().equals(link.shaft())) found = true;
                    if (!found)
                        throw new IllegalArgumentException("unaligned lift/stair landing at floor " + floor);
                }
            }
        }
        int floorCount() { return lastFloor - firstFloor + 1; }
        int roomCount() {
            int rooms = 0;
            for (FloorPlate plate : plates) rooms += plate.roomCount();
            return rooms;
        }
        FloorPlate plate(int floor) {
            return floor < firstFloor || floor > lastFloor ? null : plates.get(floor - firstFloor);
        }
        boolean containsFloor(int floor) { return floor >= firstFloor && floor <= lastFloor; }
        String summary() {
            return buildingId + " " + kind + " floors=" + firstFloor + "-" + lastFloor
                    + " footprint=" + footprint.width + "x" + footprint.height
                    + " rooms=" + roomCount() + " connections=" + connections.size();
        }
    }

    private VerticalBuildingBlueprintAuthority() { }

    /**
     * Stable across local floor seeds and regeneration order. Returns reusable,
     * UNPLACED blueprints; future placement must check all floor reservations.
     */
    static List<Blueprint> forZone(long atlasSeed, int sx, int sy, int zx, int zy) {
        return List.of(
                apartment(id(atlasSeed, sx, sy, zx, zy, "hab-west"),
                        Kind.APARTMENT_STACK, sx, sy, zx, zy, 2, 4),
                apartment(id(atlasSeed, sx, sy, zx, zy, "hab-east"),
                        Kind.APARTMENT_COURT, sx, sy, zx, zy, 2, 4),
                atrium(id(atlasSeed, sx, sy, zx, zy, "upper-atrium"),
                        sx, sy, zx, zy, 6, 8));
    }

    static String id(long seed, int sx, int sy, int zx, int zy, String slot) {
        if (slot == null || slot.isBlank())
            throw new IllegalArgumentException("stable building slot required");
        long h = seed;
        int[] coordinates = {sx, sy, zx, zy};
        for (int c : coordinates) h = mix(h ^ ((long)c * 0x9e3779b97f4a7c15L));
        for (int i = 0; i < slot.length(); i++) h = mix(h ^ slot.charAt(i));
        return "BLD-" + Long.toUnsignedString(h, 36) + "-" + slot;
    }
    private static long mix(long h) {
        h ^= h >>> 30; h *= 0xbf58476d1ce4e5b9L;
        h ^= h >>> 27; h *= 0x94d049bb133111ebL;
        return h ^ (h >>> 31);
    }
    private static void add(List<Space> spaces, String id, int floor, Use use, Rect rect) {
        spaces.add(new Space(id + "/F" + floor + "/" + use + "/" + spaces.size(), floor, use, rect));
    }

    static Blueprint apartment(String id, Kind kind, int sx, int sy, int zx, int zy,
                               int bottom, int top) {
        if (kind != Kind.APARTMENT_STACK && kind != Kind.APARTMENT_COURT)
            throw new IllegalArgumentException("apartment template needs residential type");
        Rect footprint = new Rect(0, 0, 32, 26);
        Rect stairs = new Rect(2, 10, 4, 5);
        Rect lift = new Rect(2, 3, 4, 5);
        ArrayList<FloorPlate> plates = new ArrayList<>();
        ArrayList<VerticalLink> links = new ArrayList<>();
        for (int floor = bottom; floor <= top; floor++) {
            ArrayList<Space> spaces = new ArrayList<>();
            add(spaces, id, floor, Use.STAIRWELL, stairs);
            add(spaces, id, floor, Use.ELEVATOR, lift);
            add(spaces, id, floor, Use.CORRIDOR, new Rect(7, 12, 23, 2));
            add(spaces, id, floor, floor == bottom ? Use.LOBBY : Use.APARTMENT,
                    new Rect(7, 2, 9, 9));
            add(spaces, id, floor, Use.APARTMENT, new Rect(18, 2, 12, 9));
            add(spaces, id, floor, Use.APARTMENT, new Rect(7, 16, 9, 8));
            add(spaces, id, floor, floor == top ? Use.SHARED_SERVICE : Use.APARTMENT,
                    new Rect(18, 16, 12, 8));
            plates.add(new FloorPlate(floor, spaces));
            if (floor > bottom) {
                links.add(new VerticalLink(id + "/stair/" + (floor-1), LinkKind.STAIRS,
                        floor - 1, floor, stairs));
                links.add(new VerticalLink(id + "/lift/" + (floor-1), LinkKind.LIFT,
                        floor - 1, floor, lift));
            }
        }
        return new Blueprint(id, kind, sx, sy, zx, zy, bottom, top, footprint, plates, links);
    }

    static Blueprint atrium(String id, int sx, int sy, int zx, int zy, int bottom, int top) {
        Rect footprint = new Rect(0, 0, 38, 32);
        Rect stairs = new Rect(2, 12, 4, 6);
        Rect lift = new Rect(2, 3, 4, 6);
        Rect centralVolume = new Rect(10, 8, 20, 15);
        ArrayList<FloorPlate> plates = new ArrayList<>();
        ArrayList<VerticalLink> links = new ArrayList<>();
        for (int floor = bottom; floor <= top; floor++) {
            ArrayList<Space> spaces = new ArrayList<>();
            add(spaces, id, floor, Use.STAIRWELL, stairs);
            add(spaces, id, floor, Use.ELEVATOR, lift);
            add(spaces, id, floor, floor == bottom ? Use.ATRIUM_FLOOR : Use.OPEN_TO_BELOW, centralVolume);
            if (floor > bottom) {
                add(spaces, id, floor, Use.BALCONY, new Rect(10, 6, 20, 2));
                add(spaces, id, floor, Use.BALCONY, new Rect(10, 23, 20, 2));
            }
            add(spaces, id, floor, Use.GALLERY_ROOM, new Rect(10, 2, 9, 4));
            add(spaces, id, floor, Use.GALLERY_ROOM, new Rect(21, 2, 9, 4));
            add(spaces, id, floor, Use.GALLERY_ROOM, new Rect(10, 27, 9, 4));
            add(spaces, id, floor, Use.GALLERY_ROOM, new Rect(21, 27, 9, 4));
            plates.add(new FloorPlate(floor, spaces));
            if (floor > bottom) {
                links.add(new VerticalLink(id + "/stair/" + (floor-1), LinkKind.STAIRS,
                        floor - 1, floor, stairs));
                links.add(new VerticalLink(id + "/lift/" + (floor-1), LinkKind.LIFT,
                        floor - 1, floor, lift));
            }
        }
        return new Blueprint(id, Kind.ATRIUM_HALL, sx, sy, zx, zy, bottom, top, footprint, plates, links);
    }
}
