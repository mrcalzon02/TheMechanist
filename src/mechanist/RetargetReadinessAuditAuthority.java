package mechanist;

import java.util.*;

/**
 * Endpoint audit surface for confirming that the topology chain is complete and advisory-only.
 */
final class RetargetReadinessAuditAuthority {
    static final String VERSION = "0.9.10es";

    static final class Surface {
        final boolean ready;
        final int checks;
        final int blockers;
        final String headline;
        final ArrayList<String> checkLines;
        final ArrayList<String> blockerLines;
        final ArrayList<String> handoffLines;

        Surface(boolean ready, int checks, int blockers, String headline, ArrayList<String> checkLines, ArrayList<String> blockerLines, ArrayList<String> handoffLines) {
            this.ready = ready;
            this.checks = checks;
            this.blockers = blockers;
            this.headline = headline == null ? "Retarget readiness audit has no active reading." : headline;
            this.checkLines = checkLines == null ? new ArrayList<>() : checkLines;
            this.blockerLines = blockerLines == null ? new ArrayList<>() : blockerLines;
            this.handoffLines = handoffLines == null ? new ArrayList<>() : handoffLines;
        }
    }

    static final class Result {
        final Surface surface;
        Result(Surface surface) { this.surface = surface; }
        String summary() {
            if (surface == null) return "retargetReadiness version=" + VERSION + " status=no-world checks=0 blockers=1";
            return "retargetReadiness version=" + VERSION
                    + " status=" + (surface.ready ? "READY" : "BLOCKED")
                    + " checks=" + surface.checks
                    + " blockers=" + surface.blockers
                    + " mode=endpoint-audit no-live-economy no-route-reservation no-district-mutation";
        }
    }

    static Result apply(World world) {
        ArrayList<String> checks = new ArrayList<>();
        ArrayList<String> blockers = new ArrayList<>();
        ArrayList<String> handoff = new ArrayList<>();

        addCheck(checks, blockers, "Topology vocabulary authority present", true, EconomicTopologyFramework.auditSummary());
        addCheck(checks, blockers, "Generation-bias authority present", true, "authority=" + EconomicGenerationBiasAuthority.VERSION + " consumes shared topology vocabulary");
        addCheck(checks, blockers, "Preview consumer authority present", true, EconomicTopologyPreviewConsumerAuthority.auditSummary());
        addCheck(checks, blockers, "Reporting overlay authority present", true, EconomicTopologyReportingOverlayAuthority.auditSummary());
        addCheck(checks, blockers, "Map/intel bridge authority present", true, EconomicTopologyMapIntelBridgeAuthority.auditSummary());

        if (world == null) {
            addCheck(checks, blockers, "Active-world topology surfaces available", false, "No active world is loaded for endpoint verification.");
            handoff.add("Load or generate a zone to verify cached local topology, reporting, and map/intel surfaces together.");
        } else {
            EconomicLocalTopologyMetadataSurfaceAuthority.Surface local = localSurface(world);
            EconomicTopologyReportingOverlayAuthority.Surface report = reportSurface(world);
            EconomicTopologyMapIntelBridgeAuthority.Surface mapIntel = mapIntelSurface(world);

            boolean localReady = local != null && local.roomRoleByIndex != null && local.roomRoleByIndex.length >= Math.max(1, world.rooms.size());
            boolean reportReady = report != null && report.roomRoleCounts != null && !report.roomRoleCounts.isEmpty();
            boolean mapReady = mapIntel != null && mapIntel.legendLines != null && !mapIntel.legendLines.isEmpty();
            boolean generationReady = world.economicTopologyGenerationSummary != null && !world.economicTopologyGenerationSummary.toLowerCase(Locale.ROOT).contains("not been applied");

            addCheck(checks, blockers, "Generation bias applied to active zone", generationReady, valueOrFallback(world.economicTopologyGenerationSummary, "No generation-bias summary recorded."));
            addCheck(checks, blockers, "Cached local topology surface available", localReady, valueOrFallback(world.localTopologyMetadataSummary, "No local topology cache recorded."));
            addCheck(checks, blockers, "Reporting overlay surface available", reportReady, valueOrFallback(world.economicTopologyReportingOverlaySummary, "No topology reporting overlay recorded."));
            addCheck(checks, blockers, "Map/intel bridge surface available", mapReady, valueOrFallback(world.economicTopologyMapIntelBridgeSummary, "No topology map/intel bridge recorded."));

            String zoneLabel = world.zoneType == null ? "unknown zone" : world.zoneType.label;
            handoff.add("Active-zone handoff: " + zoneLabel + " has a cached topology chain from generation bias through map/intel display.");
            if (report != null) {
                handoff.add("Dominant district reading: purpose " + report.purpose + "; pressure " + label(report.dominantPressure) + "; circulation " + label(report.primaryCirculation) + ".");
                handoff.add("Room-role buckets: " + countsLine(report.roomRoleCounts, 5) + ".");
                handoff.add("Circulation buckets: " + countsLine(report.circulationCounts, 5) + ".");
            }
            if (mapIntel != null && mapIntel.cautionLines != null && !mapIntel.cautionLines.isEmpty()) {
                handoff.add("Current topology caution: " + mapIntel.cautionLines.get(0));
            }
        }

        boolean boundaryOk = EconomicTopologyPreviewConsumerAuthority.auditSummary().contains("no-live-economy")
                && EconomicTopologyReportingOverlayAuthority.auditSummary().contains("no-live-economy")
                && EconomicTopologyMapIntelBridgeAuthority.auditSummary().contains("no-live-economy");
        addCheck(checks, blockers, "Advisory/display boundary preserved", boundaryOk, "Phase 3.5 endpoint keeps topology explanatory: no pathfinding, no route reservation, no item movement, no labor assignment, no district mutation, no pressure propagation.");

        if (handoff.isEmpty()) handoff.add("Retarget handoff can be prepared after an active world verifies the cached topology surfaces.");
        handoff.add("Endpoint rule: the next retarget may alter core features only after this audit remains green through compile, jar rebuild, and package integrity checks.");
        handoff.add("Carry-forward boundary: topology may inform explanations and previews; it still must not become a hidden economy loop unless a later master-plan gate explicitly opens that behavior.");

        boolean ready = blockers.isEmpty();
        String headline = ready
                ? "Phase 3.5 endpoint ready: topology chain is cached, reportable, visible, and advisory-only."
                : "Phase 3.5 endpoint blocked: " + blockers.size() + " readiness issue(s) require correction before retarget.";
        Surface surface = new Surface(ready, checks.size(), blockers.size(), headline, checks, blockers, handoff);
        if (world != null) {
            world.retargetReadinessAuditSurface = surface;
            world.retargetReadinessAuditSummary = headline;
            world.retargetReadinessAuditNotes.clear();
            world.retargetReadinessAuditNotes.addAll(checks);
            world.retargetReadinessAuditNotes.addAll(handoff);
        }
        return new Result(surface);
    }

    static ArrayList<String> infopediaLines() {
        ArrayList<String> out = new ArrayList<>();
        out.add("Retarget Readiness Audit Authority " + VERSION);
        out.add("Purpose: closes the Phase 3.5 economic-topology chain as a stability checkpoint before the next core-feature retarget begins.");
        out.add("What it verifies: shared topology vocabulary, generation-bias consumption, cached local topology, preview consumers, reporting overlay, map/intel bridge visibility, and advisory-only boundaries.");
        out.add("Why it exists: the coming retarget can alter core features, so this endpoint records a clean handoff instead of letting topology scaffolding remain half-integrated.");
        out.add("Boundary: audit and handoff only; it does not block construction, reserve routes, pathfind, move goods, assign labor, mutate districts, or propagate pressure.");
        return out;
    }

    static ArrayList<String> statusLines(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        out.add("Retarget Readiness Audit " + VERSION + " — Phase 3.5 endpoint checkpoint.");
        if (g == null || g.world == null) {
            out.add("  No active world loaded; runtime endpoint checks cannot verify cached topology surfaces yet.");
            out.add("  Static chain: framework, generation bias, local metadata, preview consumers, reporting overlay, and map/intel bridge authorities are present.");
            return out;
        }
        Surface s = apply(g.world).surface;
        out.add("  " + s.headline);
        out.add("  Checks: " + s.checks + "   Blockers: " + s.blockers + ".");
        int maxBlockers = Math.min(3, s.blockerLines.size());
        for (int i = 0; i < maxBlockers; i++) out.add("  BLOCKER: " + s.blockerLines.get(i));
        int maxHandoff = Math.min(4, s.handoffLines.size());
        for (int i = 0; i < maxHandoff; i++) out.add("  " + s.handoffLines.get(i));
        return out;
    }

    static ArrayList<String> auditLines(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        out.add("Retarget Readiness Audit");
        out.add(auditSummary());
        if (g == null || g.world == null) {
            out.add("No active world loaded for runtime surface verification.");
            out.add("Static authorities remain available, but endpoint readiness cannot be marked green without a generated zone.");
            return out;
        }
        Surface s = apply(g.world).surface;
        out.add(s.headline);
        out.add("Checks: " + s.checks + "; blockers: " + s.blockers + ".");
        out.add("Check ledger:");
        out.addAll(s.checkLines);
        if (!s.blockerLines.isEmpty()) {
            out.add("Blocker ledger:");
            out.addAll(s.blockerLines);
        }
        out.add("Handoff ledger:");
        out.addAll(s.handoffLines);
        return out;
    }

    static String auditSummary() {
        return "retargetReadiness version=" + VERSION + " mode=stability-endpoint verifies=topologyFramework+generationBias+localMetadata+previewConsumers+reportingOverlay+mapIntelBridge no-live-economy no-route-reservation no-district-mutation";
    }

    private static void addCheck(ArrayList<String> checks, ArrayList<String> blockers, String name, boolean ok, String detail) {
        String line = (ok ? "OK: " : "BLOCKED: ") + name + " — " + valueOrFallback(detail, "No detail recorded.");
        checks.add(line);
        if (!ok) blockers.add(name + " — " + valueOrFallback(detail, "No detail recorded."));
    }

    private static EconomicLocalTopologyMetadataSurfaceAuthority.Surface localSurface(World world) {
        if (world == null) return null;
        if (world.localTopologyMetadataSurface == null) EconomicLocalTopologyMetadataSurfaceAuthority.apply(world);
        return world.localTopologyMetadataSurface;
    }

    private static EconomicTopologyReportingOverlayAuthority.Surface reportSurface(World world) {
        if (world == null) return null;
        if (world.economicTopologyReportingOverlaySurface == null) EconomicTopologyReportingOverlayAuthority.apply(world);
        return world.economicTopologyReportingOverlaySurface;
    }

    private static EconomicTopologyMapIntelBridgeAuthority.Surface mapIntelSurface(World world) {
        if (world == null) return null;
        if (world.economicTopologyMapIntelBridgeSurface == null) EconomicTopologyMapIntelBridgeAuthority.apply(world);
        return world.economicTopologyMapIntelBridgeSurface;
    }

    private static String label(EconomicTopologyFramework.PressureType t) {
        return t == null ? "unclassified pressure" : t.label;
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

    private static String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private RetargetReadinessAuditAuthority() {}
}
