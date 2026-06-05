package mechanist;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Compatibility attachment layer for structured zone grids.
 *
 * The current recovered codebase still passes around legacy World instances with
 * char[][] tiles.  This runtime lets new generation passes attach structured
 * ZoneTileGrid data to those world objects without requiring a risky edit to the
 * recovered World class itself.
 */
final class StructuredZoneGridRuntime {
    private static final Map<Object, ZoneTileGrid> GRIDS_BY_WORLD = new WeakHashMap<>();

    private StructuredZoneGridRuntime() {}

    static ZoneTileGrid gridForWorld(Object world) {
        if (world == null) return null;
        ZoneTileGrid existing = GRIDS_BY_WORLD.get(world);
        if (existing != null) return existing;
        ZoneTileGrid created = createGridFromWorld(world);
        if (created != null) {
            GRIDS_BY_WORLD.put(world, created);
            DebugLog.audit("STRUCTURED_ZONE_GRID", "attached structured grid " + created.width() + "x" + created.height() + " to " + world.getClass().getSimpleName());
        }
        return created;
    }

    static ZoneTileGrid rebuildGridForWorld(Object world) {
        if (world == null) return null;
        ZoneTileGrid created = createGridFromWorld(world);
        if (created != null) {
            GRIDS_BY_WORLD.put(world, created);
            DebugLog.audit("STRUCTURED_ZONE_GRID", "rebuilt structured grid " + created.width() + "x" + created.height() + " for " + world.getClass().getSimpleName());
        }
        return created;
    }

    static void attachGrid(Object world, ZoneTileGrid grid) {
        if (world == null || grid == null) return;
        GRIDS_BY_WORLD.put(world, grid);
        DebugLog.audit("STRUCTURED_ZONE_GRID", "explicitly attached structured grid " + grid.width() + "x" + grid.height() + " to " + world.getClass().getSimpleName());
    }

    static boolean syncLegacyTiles(Object world) {
        if (world == null) return false;
        ZoneTileGrid grid = GRIDS_BY_WORLD.get(world);
        if (grid == null) return false;
        Field tilesField = findField(world.getClass(), "tiles");
        if (tilesField == null) return false;
        try {
            tilesField.setAccessible(true);
            tilesField.set(world, grid.toLegacyGlyphs());
            DebugLog.audit("STRUCTURED_ZONE_GRID", "synced structured grid back to legacy tiles for " + world.getClass().getSimpleName());
            return true;
        } catch (Throwable t) {
            DebugLog.warn("STRUCTURED_ZONE_GRID", "Could not sync structured grid to legacy tiles: " + t.getMessage());
            return false;
        }
    }

    static ZoneTileGrid gridForPanel(GamePanel panel) {
        return panel == null ? null : gridForWorld(panel.world);
    }

    static ZonePlacementValidator placementValidatorForWorld(Object world) {
        ZoneTileGrid grid = gridForWorld(world);
        return grid == null ? null : grid.toPlacementValidator();
    }

    static boolean worldHasStructuredGrid(Object world) {
        return world != null && GRIDS_BY_WORLD.containsKey(world);
    }

    private static ZoneTileGrid createGridFromWorld(Object world) {
        char[][] legacyTiles = legacyTilesFromWorld(world);
        if (legacyTiles != null) return ZoneTileGrid.fromLegacyTiles(legacyTiles);
        int width = intField(world, "w", intField(world, "width", 1));
        int height = intField(world, "h", intField(world, "height", 1));
        return new ZoneTileGrid(Math.max(1, width), Math.max(1, height));
    }

    private static char[][] legacyTilesFromWorld(Object world) {
        Field field = findField(world.getClass(), "tiles");
        if (field == null) return null;
        try {
            field.setAccessible(true);
            Object value = field.get(world);
            return value instanceof char[][] tiles ? tiles : null;
        } catch (Throwable t) {
            DebugLog.warn("STRUCTURED_ZONE_GRID", "Could not read legacy tiles from world: " + t.getMessage());
            return null;
        }
    }

    private static int intField(Object target, String fieldName, int fallback) {
        if (target == null || fieldName == null) return fallback;
        Field field = findField(target.getClass(), fieldName);
        if (field == null) return fallback;
        try {
            field.setAccessible(true);
            Object value = field.get(target);
            return value instanceof Number number ? number.intValue() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static Field findField(Class<?> type, String name) {
        if (type == null || name == null || name.isBlank()) return null;
        Class<?> cursor = type;
        while (cursor != null) {
            try {
                return cursor.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        return null;
    }
}
