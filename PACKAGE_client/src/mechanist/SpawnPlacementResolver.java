package mechanist;

import java.util.Objects;

/** Spiral proximity search for safe login placement when a saved tile is occupied. */
final class SpawnPlacementResolver {
    record SpawnResolution(double x, double y, boolean originalClear, int searchRadius, String reason) { }

    private final AuthoritativeWorldGrid grid;
    private final int maxRadius;

    SpawnPlacementResolver(AuthoritativeWorldGrid grid, int maxRadius) {
        this.grid = Objects.requireNonNull(grid, "grid");
        this.maxRadius = Math.max(1, maxRadius);
    }

    SpawnResolution resolve(double savedX, double savedY) {
        int originX = (int)Math.round(savedX);
        int originY = (int)Math.round(savedY);
        AuthoritativeWorldGrid.Tile origin = new AuthoritativeWorldGrid.Tile(originX, originY);
        if (grid.isTraversable(origin)) return new SpawnResolution(savedX, savedY, true, 0, "saved tile clear");
        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                AuthoritativeWorldGrid.Tile north = new AuthoritativeWorldGrid.Tile(originX + dx, originY - radius);
                if (grid.isTraversable(north)) return resolved(north, radius, "north edge spiral fallback");
                AuthoritativeWorldGrid.Tile south = new AuthoritativeWorldGrid.Tile(originX + dx, originY + radius);
                if (grid.isTraversable(south)) return resolved(south, radius, "south edge spiral fallback");
            }
            for (int dy = -radius + 1; dy <= radius - 1; dy++) {
                AuthoritativeWorldGrid.Tile west = new AuthoritativeWorldGrid.Tile(originX - radius, originY + dy);
                if (grid.isTraversable(west)) return resolved(west, radius, "west edge spiral fallback");
                AuthoritativeWorldGrid.Tile east = new AuthoritativeWorldGrid.Tile(originX + radius, originY + dy);
                if (grid.isTraversable(east)) return resolved(east, radius, "east edge spiral fallback");
            }
        }
        throw new IllegalStateException("No traversable login tile within radius " + maxRadius + " of " + originX + "," + originY);
    }

    private static SpawnResolution resolved(AuthoritativeWorldGrid.Tile tile, int radius, String reason) { return new SpawnResolution(tile.x(), tile.y(), false, radius, reason); }
}
