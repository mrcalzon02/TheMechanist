package mechanist;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Structured tile grid wrapper for generated zones.
 *
 * This is the compatibility bridge between the current legacy char[][] map path
 * and the richer tile-state model required by topology, rooms, objects,
 * containers, lighting, vehicles, pets, reservations, and transitions.
 */
final class ZoneTileGrid {
    private final int width;
    private final int height;
    private final ZoneTileState[][] tiles;

    ZoneTileGrid(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.tiles = new ZoneTileState[this.width][this.height];
        for (int x = 0; x < this.width; x++) {
            for (int y = 0; y < this.height; y++) {
                this.tiles[x][y] = new ZoneTileState();
            }
        }
    }

    static ZoneTileGrid fromLegacyTiles(char[][] legacyTiles) {
        if (legacyTiles == null || legacyTiles.length == 0) return new ZoneTileGrid(1, 1);
        int w = legacyTiles.length;
        int h = 1;
        for (char[] column : legacyTiles) if (column != null) h = Math.max(h, column.length);
        ZoneTileGrid grid = new ZoneTileGrid(w, h);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                char glyph = legacyTiles[x] != null && y < legacyTiles[x].length ? legacyTiles[x][y] : ' ';
                grid.tiles[x][y] = ZoneTileState.fromLegacyGlyph(glyph);
            }
        }
        return grid;
    }

    int width() { return width; }
    int height() { return height; }

    boolean inBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    ZoneTileState tile(int x, int y) {
        if (!inBounds(x, y)) return null;
        return tiles[x][y];
    }

    ZoneTileState requireTile(int x, int y) {
        if (!inBounds(x, y)) throw new IllegalArgumentException("tile outside zone grid: " + x + "," + y + " size=" + width + "x" + height);
        return tiles[x][y];
    }

    void setBase(int x, int y, ZoneTileState.BaseTileType baseType, char legacyGlyph) {
        if (!inBounds(x, y)) return;
        tiles[x][y].setBaseType(baseType, legacyGlyph);
    }

    void markRoom(Rectangle bounds, String roomId, Faction faction) {
        forEach(bounds, state -> state.markRoom(roomId, faction));
    }

    void markCorridor(Rectangle bounds, String corridorId) {
        forEach(bounds, state -> state.markCorridor(corridorId));
    }

    void markRoad(Rectangle bounds, String roadNetworkId) {
        forEach(bounds, state -> state.markRoad(roadNetworkId));
    }

    void markCentralPlaza(Rectangle bounds, String plazaId) {
        forEach(bounds, state -> state.setSpaceType(ZoneTileState.SpaceType.CENTRAL_PLAZA).reserve(plazaId));
    }

    void markTransition(Rectangle bounds, String transitionId, boolean vertical) {
        forEach(bounds, state -> state.setSpaceType(vertical ? ZoneTileState.SpaceType.TRANSITION_ROOM : ZoneTileState.SpaceType.ROAD_NETWORK).markTransition(transitionId, vertical));
    }

    void reserve(Rectangle bounds, String label) {
        forEach(bounds, state -> state.reserve(label));
    }

    void addObject(int x, int y, String objectId, String typeKey, String label, boolean blocksMovement, boolean container) {
        if (!inBounds(x, y)) return;
        tiles[x][y].addObject(objectId, typeKey, label, blocksMovement, container);
    }

    void addLight(int x, int y, ZoneTileState.LightKind kind, int intensityPercent, String sourceId) {
        if (!inBounds(x, y)) return;
        tiles[x][y].addLight(kind, intensityPercent, sourceId);
    }

    void setOccupant(int x, int y, String entityId) {
        if (!inBounds(x, y)) return;
        tiles[x][y].setOccupantEntityId(entityId);
    }

    void setPet(int x, int y, String petId) {
        if (!inBounds(x, y)) return;
        tiles[x][y].setPetEntityId(petId);
    }

    void setVehicle(int x, int y, String vehicleId) {
        if (!inBounds(x, y)) return;
        tiles[x][y].setVehicleId(vehicleId);
    }

    boolean blocksRoomPlacement(Rectangle candidate) {
        if (candidate == null || candidate.width <= 0 || candidate.height <= 0) return true;
        Rectangle clipped = candidate.intersection(new Rectangle(0, 0, width, height));
        if (clipped.width != candidate.width || clipped.height != candidate.height) return true;
        for (int x = clipped.x; x < clipped.x + clipped.width; x++) {
            for (int y = clipped.y; y < clipped.y + clipped.height; y++) {
                if (tiles[x][y].blocksRoomPlacement()) return true;
            }
        }
        return false;
    }

    List<ZoneTileState> tilesIn(Rectangle bounds) {
        ArrayList<ZoneTileState> out = new ArrayList<>();
        Rectangle clipped = clip(bounds);
        if (clipped == null) return out;
        for (int x = clipped.x; x < clipped.x + clipped.width; x++) {
            for (int y = clipped.y; y < clipped.y + clipped.height; y++) out.add(tiles[x][y]);
        }
        return out;
    }

    char[][] toLegacyGlyphs() {
        char[][] out = new char[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                out[x][y] = tiles[x][y].legacyGlyph();
            }
        }
        return out;
    }

    ZonePlacementValidator toPlacementValidator() {
        ZonePlacementValidator validator = new ZonePlacementValidator(width, height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                ZoneTileState state = tiles[x][y];
                if (!state.blocksRoomPlacement()) continue;
                String label = state.reservationLabel();
                if (label == null || label.isBlank()) label = state.spaceType().name().toLowerCase(java.util.Locale.ROOT);
                validator.reservePoint(x, y, 0, ZonePlacementValidator.ReservationKind.CORRIDOR, label);
            }
        }
        return validator;
    }

    private Rectangle clip(Rectangle bounds) {
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0) return null;
        Rectangle clipped = bounds.intersection(new Rectangle(0, 0, width, height));
        if (clipped.width <= 0 || clipped.height <= 0) return null;
        return clipped;
    }

    private void forEach(Rectangle bounds, TileOperation operation) {
        Rectangle clipped = clip(bounds);
        if (clipped == null || operation == null) return;
        for (int x = clipped.x; x < clipped.x + clipped.width; x++) {
            for (int y = clipped.y; y < clipped.y + clipped.height; y++) operation.apply(tiles[x][y]);
        }
    }

    private interface TileOperation {
        void apply(ZoneTileState state);
    }
}
