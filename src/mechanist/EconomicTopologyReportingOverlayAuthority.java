package mechanist;

import java.util.*;

/**
 * Cached/reporting-only active-zone topology overlay.
 *
 * This authority summarizes the cached local topology metadata for inspection,
 * Infopedia, and audit surfaces. It does not draw paths, reserve goods, move
 * items, assign workers, mutate districts, or propagate pressure.
 */
final class EconomicTopologyReportingOverlayAuthority {
    static final String VERSION = "0.9.10eq";
    private static final int MAX_NOTES = 10;

    static final class Surface {
        final ZoneType zone;
        final String purpose;
        final String ageBand;
        final EconomicTopologyFramework.PressureType dominantPressure;
        final EconomicTopologyFramework.CirculationClass primaryCirculation;
        final LinkedHashMap<String, Integer> roomRoleCounts;
        final LinkedHashMap<String, Integer> circulationCounts;
        final ArrayList<String> advisoryNotes;
        final String summary;

        Surface(ZoneType zone,
                String purpose,
                String ageBand,
                EconomicTopologyFramework.PressureType dominantPressure,
                EconomicTopologyFramework.CirculationClass primaryCirculation,
                LinkedHashMap<String, Integer> roomRoleCounts,
                LinkedHashMap<String, Integer> circulationCounts,
                ArrayList<String> advisoryNotes,
                String summary) {
            this.zone = zone;
            this.purpose = purpose;
            this.ageBand = ageBand;
            this.dominantPressure = dominantPressure;
            this.primaryCirculation = primaryCirculation;
            this.roomRoleCounts = roomRoleCounts == null ? new LinkedHashMap<>() : roomRoleCounts;
            this.circulationCounts = circulationCounts == null ? new LinkedHashMap<>() : circulationCounts;
            this.advisoryNotes = advisoryNotes == null ? new ArrayList<>() : advisoryNotes;
            this.summary = summary == null ? "Topology reporting overlay has no summary." : summary;
        }
    }

    static final class Result {
        final Surface surface;
        Result(Surface surface) { this.surface = surface; }
        String summary() {
            if (surface == null) return "topologyReportingOverlay version=" + VERSION + " status=no-world";
            String zoneLabel = surface.zone == null ? "unknown zone" : surface.zone.label;
            return "topologyReportingOverlay version=" + VERSION
                    + " zone=" + zoneLabel
                    + " roleBuckets=" + surface.roomRoleCounts.size()
                    + " circulationBuckets=" + surface.circulationCounts.size()
                    + " primaryCirculation=" + label(surface.primaryCirculation)
                    + " dominantPressure=" + label(surface.dominantPressure)
                    + " mode=reporting-only no-pathfinding no-reservation no-live-economy";
        }
    }

    static Result apply(World world) {
        if (world == null) return new Result(null);
        EconomicLocalTopologyMetadataSurfaceAuthority.Surface local = localSurface(world);
        if (local == null) return new Result(null);

        LinkedHashMap<String, Integer> roleCounts = roleCounts(local);
        LinkedHashMap<String, Integer> circulationCounts = circulationCounts(local);
        ArrayList<String> notes = advisoryNotes(local, roleCounts, circulationCounts);
        String summary = summaryLine(local, roleCounts, circulationCounts);
        Surface surface = new Surface(world.zoneType,
                local.profile == null || local.profile.purpose == null ? "unclassified purpose" : local.profile.purpose.label,
                local.profile == null || local.profile.ageBand == null ? "unknown infrastructure age" : local.profile.ageBand.label,
                local.dominantPressure,
                local.primaryCirculation,
                roleCounts,
                circulationCounts,
                notes,
                summary);
        world.economicTopologyReportingOverlaySurface = surface;
        world.economicTopologyReportingOverlaySummary = summary;
        world.economicTopologyReportingOverlayNotes.clear();
        world.economicTopologyReportingOverlayNotes.addAll(notes);
        return new Result(surface);
    }

    static ArrayList<String> infopediaLines() {
        ArrayList<String> out = new ArrayList<>();
        out.add("Economic Topology Reporting Overlay Authority " + VERSION);
        out.add("Purpose: turns the cached local topology surface into a compact active-zone report that map, room-intel, construction, and logistics preview surfaces can display consistently.");
        out.add("What it summarizes: room-role counts, dominant circulation classes, zone purpose, infrastructure age, pressure reading, advisory placement fit, and route-readiness implications.");
        out.add("Why it exists: the previous preview consumers could explain one cursor or one route; this surface explains the whole zone context those selected actions sit inside.");
        out.add("Boundary: reporting-only; no construction blocking, no route reservation, no inventory lock, no pathfinding, no item movement, no labor dispatch, no pressure propagation, and no district mutation.");
        return out;
    }

    static ArrayList<String> statusLines(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        out.add("Economic Topology Reporting Overlay Authority " + VERSION + " — active-zone advisory report surface.");
        if (g == null || g.world == null) {
            out.add("  No active world; no overlay report available.");
            return out;
        }
        Surface s = ensure(g.world);
        if (s == null) {
            out.add("  Local topology cache unavailable; overlay report cannot be built.");
            return out;
        }
        out.add("  " + s.summary);
        if (!s.roomRoleCounts.isEmpty()) out.add("  Room role counts: " + countsLine(s.roomRoleCounts, 6) + ".");
        if (!s.circulationCounts.isEmpty()) out.add("  Circulation counts: " + countsLine(s.circulationCounts, 6) + ".");
        out.add("  Selected construction reading: " + EconomicTopologyPreviewConsumerAuthority.constructionCompactLine(g));
        LogisticsRouteIntentAuthority.RouteIntentRecord route = g.logisticsRouteIntentHistory.peekFirst();
        if (route == null) out.add("  Selected route reading: no route intent displayed yet.");
        else {
            out.add("  Selected route reading: " + route.compact());
            ArrayList<String> warnings = EconomicTopologyPreviewConsumerAuthority.routeReadinessWarnings(g, route);
            if (warnings.isEmpty()) out.add("  Route topology warnings: none from the reporting surface.");
            else for (int i = 0; i < Math.min(3, warnings.size()); i++) out.add("  " + warnings.get(i));
        }
        for (int i = 0; i < Math.min(5, s.advisoryNotes.size()); i++) out.add("  " + s.advisoryNotes.get(i));
        out.add("  Boundary: this overlay reports the local industrial reading; it does not command actors or move stock.");
        return out;
    }

    static ArrayList<String> auditLines(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        out.add("Economic Topology Reporting Overlay audit");
        out.add(auditSummary());
        if (g == null || g.world == null) {
            out.add("No active world loaded for overlay inspection.");
        } else {
            Surface s = ensure(g.world);
            out.add(s == null ? "No overlay surface available." : s.summary);
            if (s != null) {
                out.add("Role buckets: " + countsLine(s.roomRoleCounts, 12) + ".");
                out.add("Circulation buckets: " + countsLine(s.circulationCounts, 12) + ".");
                if (!s.advisoryNotes.isEmpty()) {
                    out.add("Advisory overlay notes:");
                    out.addAll(s.advisoryNotes);
                }
            }
        }
        out.add("Rule check: overlay is cached/reporting-only and never becomes a hidden hauling, production, permission, or district-conversion engine.");
        return out;
    }

    static ArrayList<String> worldSummaryLines(World world) {
        ArrayList<String> out = new ArrayList<>();
        if (world == null) return out;
        Surface s = ensure(world);
        if (s == null) return out;
        out.add("Topology reporting overlay: " + s.summary);
        if (!s.roomRoleCounts.isEmpty()) out.add("Topology role spread: " + countsLine(s.roomRoleCounts, 5) + ".");
        if (!s.advisoryNotes.isEmpty()) out.add("Topology advisory: " + s.advisoryNotes.get(0));
        return out;
    }

    static String auditSummary() {
        return "economicTopologyReportingOverlay version=" + VERSION + " mode=cached-reporting-only consumes=localTopologyMetadata+previewConsumers no-blocking no-pathfinding no-reservation no-live-economy";
    }

    private static Surface ensure(World world) {
        if (world == null) return null;
        if (world.economicTopologyReportingOverlaySurface == null) apply(world);
        return world.economicTopologyReportingOverlaySurface;
    }

    private static EconomicLocalTopologyMetadataSurfaceAuthority.Surface localSurface(World world) {
        if (world == null) return null;
        if (world.localTopologyMetadataSurface == null) EconomicLocalTopologyMetadataSurfaceAuthority.apply(world);
        return world.localTopologyMetadataSurface;
    }

    private static LinkedHashMap<String, Integer> roleCounts(EconomicLocalTopologyMetadataSurfaceAuthority.Surface s) {
        HashMap<String, Integer> raw = new HashMap<>();
        if (s != null && s.roomRoleByIndex != null) {
            for (String role : s.roomRoleByIndex) {
                String bucket = roleBucket(role);
                raw.put(bucket, raw.getOrDefault(bucket, 0) + 1);
            }
        }
        return ordered(raw);
    }

    private static LinkedHashMap<String, Integer> circulationCounts(EconomicLocalTopologyMetadataSurfaceAuthority.Surface s) {
        HashMap<String, Integer> raw = new HashMap<>();
        if (s != null && s.circulationCounts != null) {
            for (EconomicTopologyFramework.CirculationClass c : EconomicTopologyFramework.CirculationClass.values()) {
                int v = s.circulationCounts.getOrDefault(c, 0);
                if (v > 0) raw.put(c.label, v);
            }
        }
        return ordered(raw);
    }

    private static LinkedHashMap<String, Integer> ordered(HashMap<String, Integer> raw) {
        ArrayList<Map.Entry<String, Integer>> entries = new ArrayList<>(raw.entrySet());
        entries.sort((a, b) -> {
            int c = Integer.compare(b.getValue(), a.getValue());
            return c != 0 ? c : a.getKey().compareTo(b.getKey());
        });
        LinkedHashMap<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : entries) out.put(e.getKey(), e.getValue());
        return out;
    }

    private static ArrayList<String> advisoryNotes(EconomicLocalTopologyMetadataSurfaceAuthority.Surface s,
                                                   LinkedHashMap<String, Integer> roles,
                                                   LinkedHashMap<String, Integer> circulation) {
        ArrayList<String> out = new ArrayList<>();
        int supply = roles.getOrDefault("supply/throughput", 0);
        int technical = roles.getOrDefault("technical support", 0);
        int labor = roles.getOrDefault("labor support", 0);
        int security = roles.getOrDefault("access control", 0);
        int ritual = roles.getOrDefault("ritual legitimacy", 0);
        int decay = roles.getOrDefault("decay/reclamation", 0);
        int exchange = roles.getOrDefault("exchange-facing", 0);
        boolean freight = isFreightLike(s == null ? null : s.primaryCirculation);

        if (supply > 0) out.add("Supply/throughput rooms present: route-intent previews have credible stock or transfer anchors to explain.");
        else out.add("No strong supply/throughput room bucket detected: route-intent previews should describe source anchors as improvised until clearer storage exists.");
        if (technical > 0) out.add("Technical support rooms present: construction-fit previews can distinguish machinery/service rooms from ordinary habitation.");
        if (labor > technical && labor > supply) out.add("Labor-support rooms dominate: this zone reads more like a workforce reservoir than a clean freight or production district.");
        if (security > 0 || (s != null && s.dominantPressure == EconomicTopologyFramework.PressureType.SECURITY)) out.add("Security/access pressure is visible: route and placement reports should preserve inspection and permission language.");
        if (ritual > 0 || (s != null && s.dominantPressure == EconomicTopologyFramework.PressureType.RELIGIOUS)) out.add("Ritual legitimacy is visible: construction/logistics fit should avoid treating sacred rooms as ordinary stock rooms.");
        if (decay > 0 || (s != null && s.dominantPressure == EconomicTopologyFramework.PressureType.DECAY)) out.add("Decay/reclamation pressure is visible: salvage, waste, and maintenance readings should stay distinct from formal freight.");
        if (exchange > 0 || (s != null && s.dominantPressure == EconomicTopologyFramework.PressureType.BLACK_MARKET)) out.add("Exchange or black-market pressure is visible: reporting can flag trade/contraband implications without live stock churn.");
        if (freight) out.add("Primary circulation is freight-like: later route displays have a coherent corridor vocabulary, but still no pathfinding or reservations here.");
        else out.add("Primary circulation is not freight-like: route displays should remain careful and selected/manual rather than implying an automatic hauling network.");

        while (out.size() > MAX_NOTES) out.remove(out.size() - 1);
        return out;
    }

    private static String summaryLine(EconomicLocalTopologyMetadataSurfaceAuthority.Surface s,
                                      LinkedHashMap<String, Integer> roles,
                                      LinkedHashMap<String, Integer> circulation) {
        if (s == null) return "No local topology surface available.";
        String zoneLabel = s.zone == null ? "unknown zone" : s.zone.label;
        String purpose = s.profile == null || s.profile.purpose == null ? "unclassified purpose" : s.profile.purpose.label;
        String age = s.profile == null || s.profile.ageBand == null ? "unknown infrastructure age" : s.profile.ageBand.label;
        String topRole = roles.isEmpty() ? "no room-role bucket" : roles.entrySet().iterator().next().getKey();
        String topCirculation = circulation.isEmpty() ? "no circulation bucket" : circulation.entrySet().iterator().next().getKey();
        return zoneLabel + " overlay: " + purpose + "; " + age + "; dominant pressure " + label(s.dominantPressure) + "; primary circulation " + label(s.primaryCirculation) + "; leading room role " + topRole + "; leading corridor reading " + topCirculation + ".";
    }

    private static String roleBucket(String role) {
        if (role == null || role.isBlank()) return "unclassified";
        String r = role.toLowerCase(Locale.ROOT);
        if (r.startsWith("central district nexus")) return "central nexus";
        if (r.startsWith("special-purpose")) return "special purpose";
        if (r.startsWith("supply/throughput")) return "supply/throughput";
        if (r.startsWith("technical support")) return "technical support";
        if (r.startsWith("labor-support")) return "labor support";
        if (r.startsWith("access-control")) return "access control";
        if (r.startsWith("ritual legitimacy")) return "ritual legitimacy";
        if (r.startsWith("decay/reclamation")) return "decay/reclamation";
        if (r.startsWith("exchange-facing")) return "exchange-facing";
        if (r.contains("background")) return "background pressure";
        return "other topology role";
    }

    private static boolean isFreightLike(EconomicTopologyFramework.CirculationClass c) {
        return c == EconomicTopologyFramework.CirculationClass.FREIGHT_ARTERY
                || c == EconomicTopologyFramework.CirculationClass.CARGO_CORRIDOR
                || c == EconomicTopologyFramework.CirculationClass.INDUSTRIAL_SERVICE_LOOP;
    }

    private static String countsLine(LinkedHashMap<String, Integer> counts, int limit) {
        if (counts == null || counts.isEmpty()) return "none";
        ArrayList<String> parts = new ArrayList<>();
        int shown = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (shown >= Math.max(1, limit)) break;
            parts.add(e.getKey() + " " + e.getValue());
            shown++;
        }
        int hidden = counts.size() - shown;
        if (hidden > 0) parts.add("+" + hidden + " more");
        return String.join(", ", parts);
    }

    private static String label(EconomicTopologyFramework.CirculationClass c) { return c == null ? "unknown circulation" : c.label; }
    private static String label(EconomicTopologyFramework.PressureType p) { return p == null ? "unknown pressure" : p.label; }

    private EconomicTopologyReportingOverlayAuthority() {}
}
