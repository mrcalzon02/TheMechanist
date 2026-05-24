package mechanist;

import java.awt.*;
import java.util.*;

/** Launcher/Mods tool surface for generating cursor-only full-bright worldgen audit slices. */
final class SectorAuditRuntimeAuthority {
    static final String VERSION = "sector-audit-0.9.10ji";

    static final String[] OVERLAY_LABELS = {"NONE", "FINDINGS", "ROOMS", "ROADS", "BOUNDARY", "DESCRIPTORS", "INTERACT", "CONTAINERS", "LIGHTS", "TRAPS", "ENTITIES", "TRANSITIONS"};

    static final class AuditFinding {
        final String severity;
        final String category;
        final int x;
        final int y;
        final String label;
        final String detail;
        AuditFinding(String severity, String category, int x, int y, String label, String detail) {
            this.severity = safe(severity, "INFO");
            this.category = safe(category, "GENERAL");
            this.x = x;
            this.y = y;
            this.label = safe(label, "Finding");
            this.detail = safe(detail, "No detail.");
        }
        String compactLine() { return severity + " " + category + " @ " + x + "," + y + " — " + label + ": " + detail; }
    }

    static final class AuditSnapshot {
        final ArrayList<AuditFinding> findings = new ArrayList<>();
        int roads;
        int sidewalks;
        int intersections;
        int suspiciousIntersections;
        int voids;
        int bulkheads;
        int lightsOnRoad;
        int mapObjectsOnRoad;
        int fallbackDescriptors;
        int unreachableRooms;
        boolean reachable;
        String summary = "Audit snapshot has not run.";

        AuditFinding selected(int index) {
            if (findings.isEmpty()) return null;
            return findings.get(Math.floorMod(index, findings.size()));
        }
    }

    private SectorAuditRuntimeAuthority() {}

    static boolean[][] fullBrightMask(World w) {
        if (w == null || w.w <= 0 || w.h <= 0) return new boolean[1][1];
        boolean[][] mask = new boolean[w.w][w.h];
        for (int x=0; x<w.w; x++) Arrays.fill(mask[x], true);
        return mask;
    }

    static AuditSnapshot analyze(World w, WorldSetupSettings settings, long previousSeed, long seed) {
        AuditSnapshot s = new AuditSnapshot();
        if (w == null) {
            s.summary = "authority=" + VERSION + " no world generated";
            return s;
        }
        try { s.reachable = w.allRoomsReachableStrict(); } catch (Throwable ignored) { s.reachable = false; }
        boolean[][] reachableMask = null;
        try { reachableMask = w.reachableFromStart(); } catch (Throwable ignored) {}

        for (int x=0; x<w.w; x++) {
            for (int y=0; y<w.h; y++) {
                char ch = w.tiles[x][y];
                if (ch == RoadGridIntegrationAuthority.ROAD_LANE) s.roads++;
                if (ch == RoadGridIntegrationAuthority.SIDEWALK) s.sidewalks++;
                if (ch == InterstitialInfrastructureApi.VOID_SPACE) s.voids++;
                CompiledTileDescriptor d = TileDataCompilationAuthority.resolve(w, x, y, ch);
                if (d != null) {
                    if ("exterior_maintenance_bulkhead".equals(d.family)) s.bulkheads++;
                    if ("fallback".equals(d.baseLayer)) {
                        s.fallbackDescriptors++;
                        if (s.findings.size() < 160) s.findings.add(new AuditFinding("WARN", "DESCRIPTOR", x, y, "Fallback tile descriptor", "Glyph '" + ch + "' compiled through the fallback path instead of a named terrain/fixture key."));
                    }
                    if (d.isRoad && "intersection".equals(d.shape)) {
                        s.intersections++;
                        int roadNeighbors = trueRoadNeighborCount(w, x, y);
                        if (roadNeighbors < 3) {
                            s.suspiciousIntersections++;
                            if (s.findings.size() < 160) s.findings.add(new AuditFinding("WARN", "ROAD", x, y, "Suspicious intersection", "Road descriptor says intersection with only " + roadNeighbors + " true road-lane neighbor(s). Sidewalks must not be counted as road continuity."));
                        }
                    }
                }
            }
        }

        if (w.lightSources != null) {
            for (ZoneLightSourceRecord l : w.lightSources) {
                if (l != null && w.inBounds(l.x, l.y) && w.tiles[l.x][l.y] == RoadGridIntegrationAuthority.ROAD_LANE) {
                    s.lightsOnRoad++;
                    s.findings.add(new AuditFinding("ERROR", "LIGHT", l.x, l.y, "Light fixture on road lane", "Zone lights may sit on sidewalks or legal floors, not true road carriageway tiles."));
                }
            }
        }
        if (w.mapObjects != null) {
            for (MapObjectState mo : w.mapObjects) {
                if (mo != null && w.inBounds(mo.x, mo.y) && w.tiles[mo.x][mo.y] == RoadGridIntegrationAuthority.ROAD_LANE) {
                    s.mapObjectsOnRoad++;
                    if (s.findings.size() < 200) s.findings.add(new AuditFinding("WARN", "OBJECT", mo.x, mo.y, "Object on road lane", (mo.label == null ? mo.type : mo.label) + " is sitting on a true road lane."));
                }
            }
        }

        if (reachableMask != null) {
            for (int i=0; i<w.rooms.size(); i++) {
                Rectangle rr = w.rooms.get(i);
                boolean ok = false;
                for (int x=rr.x; x<rr.x+rr.width && !ok; x++) {
                    for (int y=rr.y; y<rr.y+rr.height; y++) {
                        if (w.inBounds(x, y) && w.roomIds[x][y] == i && reachableMask[x][y]) { ok = true; break; }
                    }
                }
                if (!ok) {
                    s.unreachableRooms++;
                    int cx = Math.max(0, Math.min(w.w-1, rr.x + rr.width/2));
                    int cy = Math.max(0, Math.min(w.h-1, rr.y + rr.height/2));
                    Faction f = w.roomFaction(i);
                    s.findings.add(new AuditFinding("ERROR", "ROOM", cx, cy, "Topologically sealed room " + i, "Layout reachability treats doors and transitions as connectors. Room rect=" + rr.x + "," + rr.y + "," + rr.width + "x" + rr.height + " faction=" + (f == null ? "unknown" : f.label) + "."));
                }
            }
        }

        if (s.bulkheads <= 0) s.findings.add(new AuditFinding("ERROR", "BOUNDARY", Math.max(0, w.w/2), Math.max(0, w.h/2), "No exterior bulkheads detected", "The exterior maintenance corridor should be bounded by inner and outer maintenance bulkheads."));
        if (s.voids <= 0) s.findings.add(new AuditFinding("ERROR", "BOUNDARY", Math.max(0, w.w-2), Math.max(0, w.h/2), "No void detected", "The exterior envelope should end in void beyond the outer maintenance bulkhead."));

        String band = settings == null ? "unknown" : WorldGenerationApi.worldgenWeightBandLabel(settings.zoneSize);
        String density = settings == null ? "unknown" : WorldSetupSettings.ZONE_DENSITY[Math.max(0, Math.min(3, settings.zoneDensity))];
        s.summary = "authority=" + VERSION
            + " zone=" + w.zoneType.label
            + " layer=" + (w.sewerLayer ? "sewer" : "floor")
            + " size=" + w.w + "x" + w.h
            + " worldgenWeight=" + band
            + " density=" + density
            + " rooms=" + w.rooms.size()
            + " reachable=" + s.reachable
            + " sealedRooms=" + s.unreachableRooms
            + " findings=" + s.findings.size()
            + " npcs=" + (w.npcs == null ? 0 : w.npcs.size())
            + " objects=" + (w.mapObjects == null ? 0 : w.mapObjects.size())
            + " mapObjectsOnRoad=" + s.mapObjectsOnRoad
            + " lights=" + (w.lightSources == null ? 0 : w.lightSources.size())
            + " lightsOnRoad=" + s.lightsOnRoad
            + " roads=" + s.roads
            + " sidewalks=" + s.sidewalks
            + " intersections=" + s.intersections
            + " suspiciousIntersections=" + s.suspiciousIntersections
            + " exteriorBulkheads=" + s.bulkheads
            + " void=" + s.voids
            + " fallbackDescriptors=" + s.fallbackDescriptors
            + " seed=" + seed
            + " previousSeed=" + previousSeed;
        return s;
    }

    static String report(World w, WorldSetupSettings settings, long previousSeed, long seed) {
        return analyze(w, settings, previousSeed, seed).summary;
    }

    static java.util.List<String> compactPanelLines(AuditSnapshot s, int selectedFinding) {
        ArrayList<String> out = new ArrayList<>();
        if (s == null) {
            out.add("Audit snapshot has not run.");
            return out;
        }
        out.add("Findings " + s.findings.size() + " | rooms sealed " + s.unreachableRooms + " | lights-on-road " + s.lightsOnRoad + " | objects-on-road " + s.mapObjectsOnRoad + " | fallback descriptors " + s.fallbackDescriptors + ".");
        out.add("Roads " + s.roads + " | sidewalks " + s.sidewalks + " | intersections " + s.intersections + " | suspect intersections " + s.suspiciousIntersections + " | bulkheads " + s.bulkheads + " | void " + s.voids + ".");
        AuditFinding f = s.selected(selectedFinding);
        if (f == null) out.add("Selected finding: none. Current slice passes the checks this audit surface knows how to run.");
        else out.add("Selected finding " + (Math.floorMod(selectedFinding, Math.max(1, s.findings.size())) + 1) + "/" + s.findings.size() + ": " + f.compactLine());
        return out;
    }

    static int trueRoadNeighborCount(World w, int x, int y) {
        int n = 0;
        if (w != null && w.inBounds(x-1,y) && w.tiles[x-1][y] == RoadGridIntegrationAuthority.ROAD_LANE) n++;
        if (w != null && w.inBounds(x+1,y) && w.tiles[x+1][y] == RoadGridIntegrationAuthority.ROAD_LANE) n++;
        if (w != null && w.inBounds(x,y-1) && w.tiles[x][y-1] == RoadGridIntegrationAuthority.ROAD_LANE) n++;
        if (w != null && w.inBounds(x,y+1) && w.tiles[x][y+1] == RoadGridIntegrationAuthority.ROAD_LANE) n++;
        return n;
    }

    static String overlayLabel(int overlayMode) {
        return OVERLAY_LABELS[Math.floorMod(overlayMode, OVERLAY_LABELS.length)];
    }

    private static String safe(String s, String fallback) { return s == null || s.isBlank() ? fallback : s; }
}
