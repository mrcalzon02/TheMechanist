package mechanist;

final class MapLayerSurfaceAuthority {

    // Phase 3 conservative tile state scaffold.
    // These arrays are optional until generation populates them; null means fail closed.
    private boolean[][] phase3TraversableTiles;
    private boolean[][] phase3BlockedTiles;
    private boolean[][] phase3ReservedTraversalTiles;
    private boolean[][] phase3RoadTiles;
    private boolean[][] phase3CorridorTiles;

    private MapLayerSurfaceAuthority() {}

    static int floor(int layer) {
        int idx = Math.max(0, Math.min(19, layer));
        return idx / 2 + 1;
    }

    static boolean isSewer(int layer) {
        int idx = Math.max(0, Math.min(19, layer));
        return (idx % 2) == 0;
    }

    static int displayLayerIndex(int floor, boolean sewer) {
        int f = Math.max(1, Math.min(10, floor));
        return (f - 1) * 2 + (sewer ? 0 : 1);
    }

    static String label(int layer) {
        int floorNum = floor(layer);
        return isSewer(layer) ? ("Floor " + floorNum + "B sewer") : ("Floor " + floorNum);
    }

    static String iconForZoneType(ZoneType z) {
        if (z == ZoneType.NEUTRAL_CIVILIAN_FLOOR) return "C";
        if (z == ZoneType.GANGER_TURF) return "G";
        if (z == ZoneType.MUTANT_WARRENS) return "M";
        if (z == ZoneType.HAB_STACK) return "H";
        if (z == ZoneType.NEUTRAL_RAIL_DEPOT) return "T";
        if (z == ZoneType.ARBITES_PRECINCT_EDGE) return "A";
        if (z == ZoneType.SECTOR_GOVERNORS_MANSION) return "G";
        if (z == ZoneType.SEWER_CONDUIT) return "S";
        if (z == ZoneType.MUTANT_SEWER_CAMP) return "m";
        if (z == ZoneType.CULTIST_SEWER_CAMP) return "c";
        if (z == ZoneType.NOBLE_SERVICE_SPINE) return "N";
        if (z == ZoneType.MECHANICUS_FORGE_CLOISTER || z == ZoneType.MECHANICUS_RELIC_DUCT) return "F";
        if (z == ZoneType.IMPERIAL_GUARD_BILLET) return "I";
        if (z == ZoneType.ADMINISTRATUM_ARCHIVE) return "P";
        return "?";
    }

    /**
     * Phase 3 conservative tile traversal query.
     *
     * This is intentionally conservative until the full live tile grid authority is
     * wired. It provides a confirmed query surface for Phase3TileAccess and fails
     * closed by default.
     */
    public boolean isPhase3TraversableTile(int x, int y) {
        if (!isPhase3InBounds(x, y)) {
            return false;
        }
        return phase3TraversableTiles[x][y];
    }

    /**
     * Phase 3 conservative tile blockage query.
     *
     * This is intentionally conservative until live tile/object occupancy is wired.
     * Unknown tiles are treated as blocked.
     */
    public boolean isPhase3BlockedTile(int x, int y) {
        if (!isPhase3InBounds(x, y)) {
            return true;
        }
        return phase3BlockedTiles[x][y];
    }


    /**
     * Initializes Phase 3 tile state arrays.
     *
     * This does not mark anything traversable by default. Unknown remains blocked
     * until generation explicitly marks safe floor/receiver tiles.
     */
    public void initializePhase3TileState(int width, int height) {
        int safeWidth = Math.max(0, width);
        int safeHeight = Math.max(0, height);
        phase3TraversableTiles = new boolean[safeWidth][safeHeight];
        phase3BlockedTiles = new boolean[safeWidth][safeHeight];
        phase3ReservedTraversalTiles = new boolean[safeWidth][safeHeight];
        phase3RoadTiles = new boolean[safeWidth][safeHeight];
        phase3CorridorTiles = new boolean[safeWidth][safeHeight];

        for (int x = 0; x < safeWidth; x++) {
            for (int y = 0; y < safeHeight; y++) {
                phase3TraversableTiles[x][y] = false;
                phase3BlockedTiles[x][y] = true;
                phase3ReservedTraversalTiles[x][y] = false;
                phase3RoadTiles[x][y] = false;
                phase3CorridorTiles[x][y] = false;
            }
        }
    }

    public void setPhase3TileState(int x, int y, boolean traversable, boolean blocked) {
        if (!hasPhase3TileState() || !isPhase3InBounds(x, y)) {
            return;
        }

        phase3TraversableTiles[x][y] = traversable;
        phase3BlockedTiles[x][y] = blocked;
    }

    public boolean hasPhase3TileState() {
        return phase3TraversableTiles != null && phase3BlockedTiles != null && phase3ReservedTraversalTiles != null && phase3RoadTiles != null && phase3CorridorTiles != null;
    }

    public boolean isPhase3InBounds(int x, int y) {
        return hasPhase3TileState()
                && x >= 0
                && y >= 0
                && x < phase3TraversableTiles.length
                && phase3TraversableTiles.length > 0
                && y < phase3TraversableTiles[0].length;
    }


    public void markPhase3FloorTile(int x, int y) {
        setPhase3TileState(x, y, true, false);
    }

    public void markPhase3WallTile(int x, int y) {
        setPhase3TileState(x, y, false, true);
    }

    public void markPhase3BlockedTile(int x, int y) {
        setPhase3TileState(x, y, false, true);
    }

    public void markPhase3ReceiverTile(int x, int y) {
        setPhase3TileState(x, y, true, false);
    }

    public void markPhase3RectAsFloor(int x, int y, int width, int height) {
        if (!hasPhase3TileState()) return;
        for (int tx = x; tx < x + width; tx++) {
            for (int ty = y; ty < y + height; ty++) {
                markPhase3FloorTile(tx, ty);
            }
        }
    }

    public void markPhase3RectAsBlocked(int x, int y, int width, int height) {
        if (!hasPhase3TileState()) return;
        for (int tx = x; tx < x + width; tx++) {
            for (int ty = y; ty < y + height; ty++) {
                markPhase3BlockedTile(tx, ty);
            }
        }
    }


    /**
     * Ensures Phase 3 tile state exists without destructively resetting existing data.
     */
    public void ensurePhase3TileState(int width, int height) {
        if (!hasPhase3TileState()) {
            initializePhase3TileState(width, height);
        }
    }


    public void markPhase3ReservedTraversalTile(int x, int y) {
        if (!hasPhase3TileState() || !isPhase3InBounds(x, y)) {
            return;
        }

        phase3ReservedTraversalTiles[x][y] = true;
        phase3TraversableTiles[x][y] = true;
        phase3BlockedTiles[x][y] = false;
    }

    public boolean isPhase3ReservedTraversalTile(int x, int y) {
        return hasPhase3TileState()
                && isPhase3InBounds(x, y)
                && phase3ReservedTraversalTiles[x][y];
    }

    public boolean canPlacePhase3BlockingFixture(int x, int y) {
        return hasPhase3TileState()
                && isPhase3InBounds(x, y)
                && !phase3ReservedTraversalTiles[x][y]
                && phase3TraversableTiles[x][y]
                && !phase3BlockedTiles[x][y];
    }

    public boolean canPlacePhase3BlockingFixtureRect(int x, int y, int width, int height) {
        if (!hasPhase3TileState()) return false;
        for (int tx = x; tx < x + width; tx++) {
            for (int ty = y; ty < y + height; ty++) {
                if (!canPlacePhase3BlockingFixture(tx, ty)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void markPhase3FixtureRectAsBlocked(int x, int y, int width, int height) {
        if (!canPlacePhase3BlockingFixtureRect(x, y, width, height)) {
            return;
        }

        markPhase3RectAsBlocked(x, y, width, height);
    }


    public boolean validatePhase3ReservedTraversalRectOpen(int x, int y, int width, int height) {
        if (!hasPhase3TileState()) return false;
        for (int tx = x; tx < x + width; tx++) {
            for (int ty = y; ty < y + height; ty++) {
                if (!isPhase3InBounds(tx, ty)) {
                    return false;
                }
                if (isPhase3ReservedTraversalTile(tx, ty)
                        && (!isPhase3TraversableTile(tx, ty) || isPhase3BlockedTile(tx, ty))) {
                    return false;
                }
            }
        }
        return true;
    }


    public void markPhase3RoadTile(int x, int y) {
        if (!hasPhase3TileState() || !isPhase3InBounds(x, y)) {
            return;
        }

        phase3RoadTiles[x][y] = true;
        phase3CorridorTiles[x][y] = false;
        markPhase3FloorTile(x, y);
    }

    public void markPhase3CorridorTile(int x, int y) {
        if (!hasPhase3TileState() || !isPhase3InBounds(x, y)) {
            return;
        }

        phase3CorridorTiles[x][y] = true;
        phase3RoadTiles[x][y] = false;
        markPhase3FloorTile(x, y);
    }

    public boolean isPhase3RoadTile(int x, int y) {
        return hasPhase3TileState()
                && isPhase3InBounds(x, y)
                && phase3RoadTiles[x][y];
    }

    public boolean isPhase3CorridorTile(int x, int y) {
        return hasPhase3TileState()
                && isPhase3InBounds(x, y)
                && phase3CorridorTiles[x][y];
    }

    public boolean isPhase3RoadOrCorridorTile(int x, int y) {
        return isPhase3RoadTile(x, y) || isPhase3CorridorTile(x, y);
    }

    public void markPhase3RoadRect(int x, int y, int width, int height) {
        if (!hasPhase3TileState()) return;
        for (int tx = x; tx < x + width; tx++) {
            for (int ty = y; ty < y + height; ty++) {
                markPhase3RoadTile(tx, ty);
            }
        }
    }

    public void markPhase3CorridorRect(int x, int y, int width, int height) {
        if (!hasPhase3TileState()) return;
        for (int tx = x; tx < x + width; tx++) {
            for (int ty = y; ty < y + height; ty++) {
                markPhase3CorridorTile(tx, ty);
            }
        }
    }

}
