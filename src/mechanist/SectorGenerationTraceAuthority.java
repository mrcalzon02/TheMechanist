package mechanist;

import java.awt.*;
import java.util.*;

/**
 * One-shot world-generation trace recorder used by Sector Audit.
 *
 * It does not change authoritative world generation. It takes lightweight tile/room
 * snapshots while generation runs, then the audit surface can replay those snapshots
 * at one step per second so placement order and validation failures can be seen.
 */
final class SectorGenerationTraceAuthority {
    static final String VERSION = "0.9.10jq";
    static final int MAX_STEPS = 1000;

    static final class Step {
        final int index;
        final String phase;
        final String detail;
        final int x;
        final int y;
        final Rectangle ghostRect;
        final boolean rejected;
        final World world;

        Step(int index, String phase, String detail, int x, int y, boolean rejected, World world) {
            this(index, phase, detail, x, y, null, rejected, world);
        }

        Step(int index, String phase, String detail, int x, int y, Rectangle ghostRect, boolean rejected, World world) {
            this.index = index;
            this.phase = safe(phase, "GENERATION");
            this.detail = safe(detail, "Generation step captured.");
            this.x = x;
            this.y = y;
            this.ghostRect = ghostRect == null ? null : new Rectangle(ghostRect);
            this.rejected = rejected;
            this.world = world;
        }

        String line(int total) {
            String mark = rejected ? "REJECT" : "STEP";
            return mark + " " + (index + 1) + "/" + Math.max(1, total) + " | " + phase + " | " + detail;
        }
    }

    private static final class Recorder {
        final ArrayList<Step> steps = new ArrayList<>();
        int rejectedSeen = 0;
    }

    private static final ThreadLocal<Recorder> ACTIVE = new ThreadLocal<>();

    private SectorGenerationTraceAuthority() {}

    static void begin() { ACTIVE.set(new Recorder()); }

    static ArrayList<Step> end() {
        Recorder r = ACTIVE.get();
        ACTIVE.remove();
        if (r == null) return new ArrayList<>();
        return new ArrayList<>(r.steps);
    }

    static boolean active() { return ACTIVE.get() != null; }

    static void record(World w, String phase, String detail) {
        int x = w == null ? 0 : Math.max(0, w.w / 2);
        int y = w == null ? 0 : Math.max(0, w.h / 2);
        record(w, phase, detail, x, y, false);
    }

    static void record(World w, String phase, String detail, Rectangle focus, boolean rejected) {
        int x = focus == null ? (w == null ? 0 : w.w / 2) : focus.x + Math.max(0, focus.width / 2);
        int y = focus == null ? (w == null ? 0 : w.h / 2) : focus.y + Math.max(0, focus.height / 2);
        record(w, phase, detail, x, y, focus, rejected);
    }

    static void reject(World w, String phase, String detail, Rectangle focus) {
        Recorder r = ACTIVE.get();
        if (r == null) return;
        // Rejection storms are exactly what we need to see, but not at thousands of frames.
        // Capture early rejects and then every tenth reject thereafter.
        r.rejectedSeen++;
        if (r.rejectedSeen > 40 && (r.rejectedSeen % 10) != 0) return;
        record(w, phase, detail, focus, true);
    }

    static void record(World w, String phase, String detail, int x, int y, boolean rejected) {
        record(w, phase, detail, x, y, null, rejected);
    }

    static void record(World w, String phase, String detail, int x, int y, Rectangle ghostRect, boolean rejected) {
        Recorder r = ACTIVE.get();
        if (r == null || w == null) return;
        boolean priority = isPriorityPhase(phase);
        if (r.steps.size() >= MAX_STEPS) {
            if (!priority) return;
            int removeAt = -1;
            for (int i = r.steps.size() - 1; i >= 0; i--) {
                Step old = r.steps.get(i);
                if (old == null || old.rejected || !isPriorityPhase(old.phase)) { removeAt = i; break; }
            }
            if (removeAt < 0) removeAt = r.steps.size() - 1;
            r.steps.remove(removeAt);
        }
        World snap = snapshot(w);
        int sx = Math.max(0, Math.min(Math.max(0, snap.w - 1), x));
        int sy = Math.max(0, Math.min(Math.max(0, snap.h - 1), y));
        r.steps.add(new Step(r.steps.size(), phase, detail, sx, sy, ghostRect, rejected, snap));
    }

    static World snapshot(World src) {
        World dst = new World(src.seed, src.w, src.h);
        dst.zoneType = src.zoneType;
        dst.sectorX = src.sectorX; dst.sectorY = src.sectorY;
        dst.zoneX = src.zoneX; dst.zoneY = src.zoneY;
        dst.floor = src.floor;
        dst.sewerLayer = src.sewerLayer;
        dst.hiveName = src.hiveName;
        dst.sectorName = src.sectorName;
        dst.zoneName = src.zoneName;
        dst.zoneHistory = src.zoneHistory;
        dst.zoneEpochHistory = src.zoneEpochHistory;
        dst.zoneFacilityHistory = src.zoneFacilityHistory;
        dst.zoneProductionHistory = src.zoneProductionHistory;
        dst.zoneStockMovementHistory = src.zoneStockMovementHistory;
        dst.zoneConflictLossHistory = src.zoneConflictLossHistory;
        dst.zoneMaterializedItemHistory = src.zoneMaterializedItemHistory;
        dst.zoneLaborAssignmentHistory = src.zoneLaborAssignmentHistory;
        for (int x = 0; x < src.w; x++) {
            System.arraycopy(src.tiles[x], 0, dst.tiles[x], 0, src.h);
            System.arraycopy(src.roomIds[x], 0, dst.roomIds[x], 0, src.h);
        }
        dst.rooms.addAll(src.rooms);
        dst.roomProfiles.addAll(src.roomProfiles);
        dst.roomFactions.addAll(src.roomFactions);
        dst.roomSpecials.addAll(src.roomSpecials);
        dst.mapObjects.addAll(src.mapObjects);
        dst.lightSources.addAll(src.lightSources);
        dst.hazardWarnings.addAll(src.hazardWarnings);
        dst.trapRecords.addAll(src.trapRecords);
        dst.npcs.addAll(src.npcs);
        if (src.compiledTileDescriptors != null) {
            dst.compiledTileDescriptors = null; // force snapshot-local descriptor resolution.
        }
        return dst;
    }

    static boolean isPriorityPhase(String phase) {
        if (phase == null) return false;
        switch (phase) {
            case "RESET": case "PLAZA": case "ROOM": case "CORRIDOR": case "ROADS": case "FRONTAGE":
            case "ROOM-ASSETS": case "BOUNDARY": case "INTERWALL": case "POPULATION": case "TILE-COMPILE":
                return true;
            default:
                return false;
        }
    }

    static String safe(String s, String fallback) { return s == null || s.isBlank() ? fallback : s; }
}
