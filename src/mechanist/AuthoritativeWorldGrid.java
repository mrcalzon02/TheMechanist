package mechanist;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/** Minimal synchronized authoritative tile occupancy grid for login placement. */
final class AuthoritativeWorldGrid {
    enum TileBlocker { NONE, SOLID_BLOCK, VEHICLE, ENVIRONMENTAL_HAZARD, ACTIVE_ENTITY }
    record Tile(int x, int y) { }

    private final int minX;
    private final int minY;
    private final int maxX;
    private final int maxY;
    private final Set<Tile> blocked = new HashSet<>();
    private final Set<Tile> hazards = new HashSet<>();
    private final ReentrantLock lock = new ReentrantLock();

    AuthoritativeWorldGrid(int minX, int minY, int maxX, int maxY) {
        if (maxX < minX || maxY < minY) throw new IllegalArgumentException("invalid grid bounds");
        this.minX = minX; this.minY = minY; this.maxX = maxX; this.maxY = maxY;
    }

    void setBlocked(Tile tile, boolean value) {
        Objects.requireNonNull(tile, "tile");
        lock.lock();
        try { if (value) blocked.add(tile); else blocked.remove(tile); }
        finally { lock.unlock(); }
    }

    void setHazard(Tile tile, boolean value) {
        Objects.requireNonNull(tile, "tile");
        lock.lock();
        try { if (value) hazards.add(tile); else hazards.remove(tile); }
        finally { lock.unlock(); }
    }

    boolean isTraversable(Tile tile) {
        lock.lock();
        try { return inBounds(tile) && !blocked.contains(tile) && !hazards.contains(tile); }
        finally { lock.unlock(); }
    }

    TileBlocker blockerAt(Tile tile) {
        lock.lock();
        try {
            if (!inBounds(tile)) return TileBlocker.SOLID_BLOCK;
            if (hazards.contains(tile)) return TileBlocker.ENVIRONMENTAL_HAZARD;
            if (blocked.contains(tile)) return TileBlocker.SOLID_BLOCK;
            return TileBlocker.NONE;
        } finally { lock.unlock(); }
    }

    private boolean inBounds(Tile t) { return t != null && t.x() >= minX && t.x() <= maxX && t.y() >= minY && t.y() <= maxY; }
}
