package mechanist;

import java.util.*;

/**
 * Display-only bridge from cached topology reports into map and room-intel text.
 *
 * The bridge consumes existing cached/reporting surfaces. It does not scan in a
 * render loop, recolor legality, pathfind, reserve routes, move stock, assign
 * labor, propagate pressure, or mutate districts.
 */
final class EconomicTopologyMapIntelBridgeAuthority {
    static final String VERSION = "0.9.10er";

    static final class Surface {
        final String headline;
        final ArrayList<String> legendLines;
        final ArrayList<String> roomIntelLines;
        final ArrayList<String> cautionLines;

        Surface(String headline, ArrayList<String> legendLines, ArrayList<String> roomIntelLines, ArrayList<String> cautionLines) {
            this.headline = headline == null ? "Topology map/intel bridge has no active reading." : headline;
            this.legendLines = legendLines == null ? new ArrayList<>() : legendLines;
            this.roomIntelLines = roomIntelLines == null ? new ArrayList<>() : roomIntelLines;
            this.cautionLines = cautionLines == null ? new ArrayList<>() : cautionLines;
        }
    }

    static final class Result {
        final Surface surface;
        Result(Surface surface) { this.surface = surface; }
        String summary() {
            if (surface == null) return "topologyMapIntelBridge version=" + VERSION + " status=no-world";
            return "topologyMapIntelBridge version=" + VERSION
                    + " legendLines=" + surface.legendLines.size()
                    + " roomIntelLines=" + surface.roomIntelLines.size()
                    + " cautions=" + surface.cautionLines.size()
                    + " mode=display-only no-legality-recolor no-pathfinding no-reservation no-live-economy";
        }
    }

    static Result apply(World world) {
        if (world == null) return new Result(null);
        EconomicTopologyReportingOverlayAuthority.Surface report = reportSurface(world);
        if (report == null) return new Result(null);
        EconomicLocalTopologyMetadataSurfaceAuthority.Surface local = localSurface(world);

        ArrayList<String> legend = new ArrayList<>();
        legend.add("Topology legend: " + report.purpose + "; infrastructure " + report.ageBand + "; pressure " + label(report.dominantPressure) + "; circulation " + label(report.primaryCirculation) + ".");
        legend.add("Room-role spread: " + countsLine(report.roomRoleCounts, 6) + ".");
        legend.add("Circulation spread: " + countsLine(report.circulationCounts, 6) + ".");
        legend.add("Display boundary: these readings describe district meaning; they do not alter placement legality, path routes, or inventory state.");

        ArrayList<String> roomIntel = new ArrayList<>();
        if (local != null && local.roomRoleByIndex != null) {
            for (int i = 0; i < local.roomRoleByIndex.length && roomIntel.size() < 16; i++) {
                String role = local.roomRoleByIndex[i];
                if (role == null || role.isBlank()) continue;
                roomIntel.add("room " + i + " legend tag: " + legendCode(role) + " " + role + ".");
            }
        }
        if (roomIntel.isEmpty()) roomIntel.add("No room legend tags are available yet for the current visibility state.");

        ArrayList<String> cautions = new ArrayList<>();
        if (report.advisoryNotes != null) {
            for (String note : report.advisoryNotes) {
                if (note == null || note.isBlank()) continue;
                cautions.add("topology caution: " + note);
                if (cautions.size() >= 4) break;
            }
        }
        if (cautions.isEmpty()) cautions.add("topology caution: no immediate map/intel caution beyond the district's ordinary pressure and circulation profile.");

        String zoneLabel = world.zoneType == null ? "unknown zone" : world.zoneType.label;
        String headline = zoneLabel + " map/intel topology: " + leadingBucket(report.roomRoleCounts) + " rooms around " + leadingBucket(report.circulationCounts) + " circulation; display-only.";
        Surface surface = new Surface(headline, legend, roomIntel, cautions);
        world.economicTopologyMapIntelBridgeSurface = surface;
        world.economicTopologyMapIntelBridgeSummary = headline;
        world.economicTopologyMapIntelBridgeNotes.clear();
        world.economicTopologyMapIntelBridgeNotes.addAll(legend);
        world.economicTopologyMapIntelBridgeNotes.addAll(cautions);
        return new Result(surface);
    }

    static ArrayList<String> infopediaLines() {
        ArrayList<String> out = new ArrayList<>();
        out.add("Economic Topology Map/Intel Bridge Authority " + VERSION);
        out.add("Purpose: exposes the existing active-zone topology report through map and room-intel text so the player can read the industrial ecology of the current district without requiring a new simulation loop.");
        out.add("What it displays: zone purpose, infrastructure age, dominant pressure, primary circulation, room-role bucket spread, corridor/circulation spread, and compact room legend tags.");
        out.add("How it fits the chain: topology framework defines meaning; generation bias uses it; local metadata caches it; preview consumers advise on selected actions; reporting overlay summarizes it; this bridge makes that report readable in map/intel surfaces.");
        out.add("Boundary: display-only; no legality recolor, no route pathfinding, no reservation, no inventory lock, no worker dispatch, no district mutation, and no pressure propagation.");
        return out;
    }

    static ArrayList<String> statusLines(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        out.add("Economic Topology Map/Intel Bridge " + VERSION + " — display-only topology legend surface.");
        if (g == null || g.world == null) {
            out.add("  No active world; no map/intel topology bridge available.");
            return out;
        }
        Surface s = ensure(g.world);
        if (s == null) {
            out.add("  Reporting overlay unavailable; map/intel bridge cannot summarize the current zone.");
            return out;
        }
        out.add("  " + s.headline);
        for (int i = 0; i < Math.min(4, s.legendLines.size()); i++) out.add("  " + s.legendLines.get(i));
        for (int i = 0; i < Math.min(3, s.cautionLines.size()); i++) out.add("  " + s.cautionLines.get(i));
        out.add("  Boundary: map/intel display only; no gameplay command side effects.");
        return out;
    }

    static ArrayList<String> mapPanelLines(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        if (g == null || g.world == null) return out;
        Surface s = ensure(g.world);
        if (s == null) return out;
        out.add("Topology map/intel bridge: " + s.headline);
        out.addAll(s.legendLines);
        int maxCautions = Math.min(3, s.cautionLines.size());
        for (int i = 0; i < maxCautions; i++) out.add(s.cautionLines.get(i));
        return out;
    }

    static ArrayList<String> roomIntelHeaderLines(World world) {
        ArrayList<String> out = new ArrayList<>();
        Surface s = ensure(world);
        if (s == null) return out;
        out.add("Topology map/intel bridge: " + s.headline);
        int max = Math.min(3, s.legendLines.size());
        for (int i = 0; i < max; i++) out.add(s.legendLines.get(i));
        return out;
    }

    static String roomIntelLine(World world, int roomId) {
        if (world == null || roomId < 0) return null;
        EconomicLocalTopologyMetadataSurfaceAuthority.Surface local = localSurface(world);
        if (local == null || local.roomRoleByIndex == null || roomId >= local.roomRoleByIndex.length) return null;
        String role = local.roomRoleByIndex[roomId];
        if (role == null || role.isBlank()) return null;
        return "MAP/INTEL TAG: " + legendCode(role) + " " + role + " — display legend only, not an assignment or production order.";
    }

    static ArrayList<String> auditLines(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        out.add("Economic Topology Map/Intel Bridge audit");
        out.add(auditSummary());
        if (g == null || g.world == null) {
            out.add("No active world loaded for map/intel bridge inspection.");
        } else {
            Surface s = ensure(g.world);
            out.add(s == null ? "No bridge surface available." : s.headline);
            if (s != null) {
                out.add("Legend lines: " + s.legendLines.size() + "; room-intel legend samples: " + s.roomIntelLines.size() + "; caution lines: " + s.cautionLines.size() + ".");
                out.addAll(s.legendLines);
            }
        }
        out.add("Rule check: bridge consumes cached reporting output only and never becomes a map renderer, legality override, pathfinder, reservation engine, hauling loop, or pressure simulator.");
        return out;
    }

    static String auditSummary() {
        return "economicTopologyMapIntelBridge version=" + VERSION + " mode=display-only consumes=topologyReportingOverlay+localTopologyMetadata no-legality-recolor no-render-loop-scan no-pathfinding no-reservation no-live-economy";
    }

    private static Surface ensure(World world) {
        if (world == null) return null;
        if (world.economicTopologyMapIntelBridgeSurface == null) apply(world);
        return world.economicTopologyMapIntelBridgeSurface;
    }

    private static EconomicTopologyReportingOverlayAuthority.Surface reportSurface(World world) {
        if (world == null) return null;
        if (world.economicTopologyReportingOverlaySurface == null) EconomicTopologyReportingOverlayAuthority.apply(world);
        return world.economicTopologyReportingOverlaySurface;
    }

    private static EconomicLocalTopologyMetadataSurfaceAuthority.Surface localSurface(World world) {
        if (world == null) return null;
        if (world.localTopologyMetadataSurface == null) EconomicLocalTopologyMetadataSurfaceAuthority.apply(world);
        return world.localTopologyMetadataSurface;
    }

    private static String label(EconomicTopologyFramework.PressureType t) {
        return t == null ? "unclassified" : t.label;
    }

    private static String label(EconomicTopologyFramework.CirculationClass c) {
        return c == null ? "unclassified circulation" : c.label;
    }

    private static String countsLine(LinkedHashMap<String, Integer> counts, int limit) {
        if (counts == null || counts.isEmpty()) return "none recorded";
        ArrayList<String> parts = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (i++ >= Math.max(1, limit)) break;
            parts.add(e.getKey() + " " + e.getValue());
        }
        return String.join(", ", parts);
    }

    private static String leadingBucket(LinkedHashMap<String, Integer> counts) {
        if (counts == null || counts.isEmpty()) return "unclassified";
        Map.Entry<String, Integer> best = null;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (best == null || e.getValue() > best.getValue()) best = e;
        }
        return best == null ? "unclassified" : best.getKey();
    }

    private static String legendCode(String role) {
        String s = role == null ? "" : role.toLowerCase(Locale.ROOT);
        if (s.contains("supply") || s.contains("throughput") || s.contains("stock")) return "[S]";
        if (s.contains("technical") || s.contains("machine") || s.contains("maintenance")) return "[T]";
        if (s.contains("access") || s.contains("security") || s.contains("control")) return "[C]";
        if (s.contains("labor") || s.contains("hab") || s.contains("billet")) return "[L]";
        if (s.contains("ritual") || s.contains("religious") || s.contains("legitimacy")) return "[R]";
        if (s.contains("decay") || s.contains("reclamation") || s.contains("salvage")) return "[D]";
        if (s.contains("exchange") || s.contains("market") || s.contains("trade")) return "[E]";
        return "[?]";
    }
}
