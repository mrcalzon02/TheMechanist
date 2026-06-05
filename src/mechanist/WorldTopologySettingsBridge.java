package mechanist;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Migration bridge between current world setup settings and the new fixed-square
 * topology contract.
 *
 * This intentionally uses defensive reflection so the topology contract can be
 * introduced before every old setup/generator class is renamed or reshaped.  The
 * next migration pass can replace these reflection hooks with direct fields once
 * the owning settings class is updated.
 */
final class WorldTopologySettingsBridge {
    static final String FIELD_SECTOR_SIZE_TILES = "sectorSizeTiles";
    static final String FIELD_SECTOR_SIZE_INDEX = "sectorSizeIndex";
    static final String FIELD_WORLD_SCALE_INDEX = "worldScaleIndex";
    static final String FIELD_WORLD_SCALE = "worldScale";

    private WorldTopologySettingsBridge() {}

    static WorldTopologyContract.SectorSize fixedSectorSize(Object setup) {
        Integer tiles = readInt(setup, FIELD_SECTOR_SIZE_TILES, "sectorSizeTiles", "mapSizeTiles", "zoneSizeTiles", "worldSizeTiles");
        if (tiles != null) return WorldTopologyContract.SectorSize.fromTiles(tiles);

        Integer index = readInt(setup, FIELD_SECTOR_SIZE_INDEX, "sectorSizeIndex", "mapSizeIndex", FIELD_WORLD_SCALE_INDEX, FIELD_WORLD_SCALE);
        if (index != null) return sizeByIndex(index);

        return WorldTopologyContract.SectorSize.STANDARD_600;
    }

    static String fixedSectorSizeLabel(Object setup) {
        return fixedSectorSize(setup).displayLabel();
    }

    static String fixedSectorSizeSummary(Object setup) {
        WorldTopologyContract.SectorSize size = fixedSectorSize(setup);
        return "Sector Size: " + size.displayLabel() + " fixed square";
    }

    static List<String> fixedSectorSizeDetailLines(Object setup) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(fixedSectorSizeSummary(setup));
        lines.add("Allowed sector sizes: 500, 600, 700, 800, 900, 1000 tiles square.");
        lines.add("Cardinal transitions are reserved at North, South, East, and West road-center gates.");
        lines.add("Vertical transition rooms are pre-placement anchors near the central plaza.");
        return lines;
    }

    static WorldTopologyContract.SectorSize cycleFixedSectorSize(Object setup, int delta) {
        WorldTopologyContract.SectorSize[] values = WorldTopologyContract.SectorSize.values();
        WorldTopologyContract.SectorSize current = fixedSectorSize(setup);
        int idx = 0;
        for (int i = 0; i < values.length; i++) if (values[i] == current) { idx = i; break; }
        int next = Math.floorMod(idx + delta, values.length);
        applyFixedSectorSize(setup, values[next]);
        return values[next];
    }

    static void applyFixedSectorSize(Object setup, WorldTopologyContract.SectorSize size) {
        if (setup == null || size == null) return;
        if (writeInt(setup, FIELD_SECTOR_SIZE_TILES, size.tiles)) return;
        if (writeInt(setup, FIELD_SECTOR_SIZE_INDEX, size.ordinal())) return;
        if (writeInt(setup, FIELD_WORLD_SCALE_INDEX, size.ordinal())) return;
        if (writeInt(setup, FIELD_WORLD_SCALE, size.ordinal())) return;
        DebugLog.warn("WORLD_TOPOLOGY_SETTINGS", "Could not write fixed sector size into setup class " + setup.getClass().getName() + "; using runtime bridge value only.");
    }

    static WorldTopologyContract.ZoneTransitionPlan planForSetup(long seed, Object setup, int zoneX, int zoneY, int floor) {
        return WorldTopologyContract.planFor(seed, zoneX, zoneY, floor, fixedSectorSize(setup));
    }

    static String auditSetup(long seed, Object setup, int zoneX, int zoneY, int floor) {
        WorldTopologyContract.ZoneTransitionPlan plan = planForSetup(seed, setup, zoneX, zoneY, floor);
        return WorldTopologyAudit.validate(plan);
    }

    private static WorldTopologyContract.SectorSize sizeByIndex(int index) {
        WorldTopologyContract.SectorSize[] values = WorldTopologyContract.SectorSize.values();
        return values[Math.floorMod(index, values.length)];
    }

    private static Integer readInt(Object target, String... names) {
        if (target == null || names == null) return null;
        for (String name : names) {
            Integer viaField = readIntField(target, name);
            if (viaField != null) return viaField;
            Integer viaMethod = readIntMethod(target, name);
            if (viaMethod != null) return viaMethod;
        }
        return null;
    }

    private static Integer readIntField(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(target);
            if (value instanceof Number n) return n.intValue();
            if (value instanceof CharSequence s) return Integer.parseInt(s.toString().trim());
        } catch (Throwable ignored) {}
        return null;
    }

    private static Integer readIntMethod(Object target, String name) {
        for (String methodName : List.of(name, "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1))) {
            try {
                Method method = target.getClass().getDeclaredMethod(methodName);
                method.setAccessible(true);
                Object value = method.invoke(target);
                if (value instanceof Number n) return n.intValue();
                if (value instanceof CharSequence s) return Integer.parseInt(s.toString().trim());
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static boolean writeInt(Object target, String name, int value) {
        if (target == null || name == null || name.isBlank()) return false;
        if (writeIntField(target, name, value)) return true;
        return writeIntMethod(target, name, value);
    }

    private static boolean writeIntField(Object target, String name, int value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            Class<?> type = field.getType();
            if (type == int.class || type == Integer.class) {
                field.set(target, value);
                return true;
            }
            if (type == long.class || type == Long.class) {
                field.set(target, (long)value);
                return true;
            }
            if (type == String.class) {
                field.set(target, Integer.toString(value));
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static boolean writeIntMethod(Object target, String name, int value) {
        String setter = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        for (Class<?> type : List.of(int.class, Integer.class, long.class, Long.class, String.class)) {
            try {
                Method method = target.getClass().getDeclaredMethod(setter, type);
                method.setAccessible(true);
                if (type == String.class) method.invoke(target, Integer.toString(value));
                else if (type == long.class || type == Long.class) method.invoke(target, (long)value);
                else method.invoke(target, value);
                return true;
            } catch (Throwable ignored) {}
        }
        return false;
    }
}
