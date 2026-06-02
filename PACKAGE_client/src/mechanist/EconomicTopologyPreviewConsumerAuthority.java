package mechanist;

import java.util.*;

/**
 * 0.9.10ep — Phase 3.5 preview-only consumers for cached topology.
 *
 * This authority is deliberately advisory.  It lets construction validation,
 * logistics route intent, and manual-haul readiness explain how the current
 * placement/route reads against the cached local economic topology surface.
 * It does not block placement, reserve routes, move goods, assign workers,
 * mutate districts, or run any background scan.
 */
final class EconomicTopologyPreviewConsumerAuthority {
    static final String VERSION = "0.9.10ep";
    private static final int MAX_LINES = 8;
    private static final int MAX_WARNINGS = 5;

    private EconomicTopologyPreviewConsumerAuthority() {}

    static ArrayList<String> constructionPreviewLines(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        out.add("Economic topology construction advisory " + VERSION + ": preview-only reading of the selected build cursor.");
        if (g == null || g.world == null) {
            out.add("No active world loaded; no topology advisory available.");
            return out;
        }
        EconomicLocalTopologyMetadataSurfaceAuthority.Surface s = surface(g.world);
        if (s == null) {
            out.add("Local topology cache unavailable; placement validation falls back to ordinary construction rules.");
            return out;
        }
        BuildRecipe r = g.pendingBuildRecipe;
        if (r == null) {
            out.add("No selected build; choose a construction object to compare it against the local room/corridor role.");
            out.add("Zone context: " + zoneLine(s));
            return out;
        }
        TopologyContext ctx = contextAt(g.world, g.buildX, g.buildY);
        String blueprint = safe(g.constructionBlueprintFor(r), "unclassified blueprint");
        String intent = constructionIntent(r, blueprint);
        out.add("Selected build intent: " + intent + " via " + safe(r.name, "Unnamed build") + " / " + blueprint + ".");
        out.add("Cursor topology: " + ctx.summary() + ".");
        out.add("Zone context: " + zoneLine(s));
        out.add(constructionFitLine(intent, ctx, s));
        out.add("Boundary: this advisory never overrides placement legality; it only explains economic/industrial fit before later systems consume topology.");
        return cap(out, MAX_LINES);
    }

    static String constructionCompactLine(GamePanel g) {
        if (g == null || g.world == null || g.pendingBuildRecipe == null) return "Topology advisory: select a build to compare against local room/corridor purpose.";
        EconomicLocalTopologyMetadataSurfaceAuthority.Surface s = surface(g.world);
        if (s == null) return "Topology advisory: no local topology cache.";
        TopologyContext ctx = contextAt(g.world, g.buildX, g.buildY);
        String intent = constructionIntent(g.pendingBuildRecipe, g.constructionBlueprintFor(g.pendingBuildRecipe));
        return "Topology advisory: " + intent + " at " + shortContext(ctx) + "; " + shortFit(intent, ctx) + ".";
    }

    static ArrayList<String> routeIntentLines(GamePanel g, int fromX, int fromY, int toX, int toY) {
        ArrayList<String> out = new ArrayList<>();
        if (g == null || g.world == null) return out;
        EconomicLocalTopologyMetadataSurfaceAuthority.Surface s = surface(g.world);
        if (s == null) return out;
        TopologyContext from = contextAt(g.world, fromX, fromY);
        TopologyContext to = contextAt(g.world, toX, toY);
        out.add("topology source=" + shortContext(from));
        out.add("topology destination=" + shortContext(to));
        out.add("topology zone=" + zoneLine(s));
        out.add("topology advisory=display-only local economic reading; no route reservation, hauling, item lock, or pathfinding.");
        return cap(out, MAX_LINES);
    }

    static ArrayList<String> routeReadinessWarnings(GamePanel g, LogisticsRouteIntentAuthority.RouteIntentRecord r) {
        ArrayList<String> out = new ArrayList<>();
        if (g == null || g.world == null || r == null) return out;
        EconomicLocalTopologyMetadataSurfaceAuthority.Surface s = surface(g.world);
        if (s == null) return out;
        TopologyContext from = contextAt(g.world, r.fromX, r.fromY);
        TopologyContext to = contextAt(g.world, r.toX, r.toY);
        if (!from.inBounds || !to.inBounds) return out;
        if (!isGoodLogisticsSource(from)) {
            out.add("WARN: topology source context is " + shortContext(from) + "; treat this as a convenience anchor, not a proven stock/throughput room.");
        }
        if (!isGoodLogisticsDestination(to)) {
            out.add("WARN: topology destination context is " + shortContext(to) + "; delivery may be service/ritual/security support rather than ordinary freight handling.");
        }
        if (!isFreightLike(s.primaryCirculation)) {
            out.add("WARN: primary circulation is " + s.primaryCirculation.label + "; future hauling should remain explicit/manual here until a true route authority exists.");
        }
        if (s.dominantPressure == EconomicTopologyFramework.PressureType.SECURITY || s.dominantPressure == EconomicTopologyFramework.PressureType.BLACK_MARKET) {
            out.add("WARN: dominant pressure is " + s.dominantPressure.label + "; route previews should expect permission, inspection, or contraband-risk hooks later.");
        }
        return cap(out, MAX_WARNINGS);
    }

    static ArrayList<String> statusLines(GamePanel g) {
        ArrayList<String> out = new ArrayList<>();
        out.add("Economic Topology Preview Consumer Authority " + VERSION + " — advisory bridge from cached local topology into construction/logistics previews.");
        if (g == null || g.world == null) {
            out.add("  No active world; no preview context available.");
            return out;
        }
        EconomicLocalTopologyMetadataSurfaceAuthority.Surface s = surface(g.world);
        if (s == null) out.add("  Local topology cache unavailable.");
        else out.add("  Active zone: " + zoneLine(s));
        out.add("  Construction cursor: " + constructionCompactLine(g));
        LogisticsRouteIntentAuthority.RouteIntentRecord route = g.logisticsRouteIntentHistory.peekFirst();
        if (route == null) out.add("  Route advisory: no route intent displayed yet.");
        else {
            out.add("  Route advisory source: " + shortContext(contextAt(g.world, route.fromX, route.fromY)) + ".");
            out.add("  Route advisory destination: " + shortContext(contextAt(g.world, route.toX, route.toY)) + ".");
        }
        out.add("  Boundary: preview-only; no locks, no movement, no actor jobs, no pathfinding, no district mutation.");
        return out;
    }

    static ArrayList<String> infopediaLines() {
        ArrayList<String> out = new ArrayList<>();
        out.add("Economic Topology Preview Consumer Authority " + VERSION);
        out.add("Purpose: lets existing preview-only systems consume the cached local topology surface in one disciplined way.");
        out.add("Construction use: explains whether the selected build cursor sits in a room/corridor context that matches the build's broad intent, without changing placement legality.");
        out.add("Logistics use: annotates route intent and manual-haul readiness with source, destination, pressure, and circulation meaning, without reserving paths or moving stock.");
        out.add("Why this matters: later construction validation, logistics contracts, route overlays, and production ledgers can share one advisory language instead of inventing conflicting local interpretations.");
        out.add("Boundary: advisory only; no live economy, no autonomous hauling, no route reservation, no production consumption, no labor assignment, no district conversion.");
        return out;
    }

    static String auditSummary() {
        return "economicTopologyPreviewConsumer version=" + VERSION + " consumers=construction+routeIntent+haulReadiness mode=advisory-only no-pathfinding no-reservation no-live-economy";
    }

    private static EconomicLocalTopologyMetadataSurfaceAuthority.Surface surface(World world) {
        if (world == null) return null;
        if (world.localTopologyMetadataSurface == null) EconomicLocalTopologyMetadataSurfaceAuthority.apply(world);
        return world.localTopologyMetadataSurface;
    }

    private static TopologyContext contextAt(World world, int x, int y) {
        if (world == null || !world.inBounds(x, y)) return TopologyContext.outOfBounds(x, y);
        EconomicLocalTopologyMetadataSurfaceAuthority.Surface s = surface(world);
        int roomId = world.roomIds[x][y];
        String role = (s == null || roomId < 0) ? null : s.roomRole(roomId);
        EconomicTopologyFramework.CirculationClass circulation = s == null ? null : s.circulationAt(x, y);
        if ((role == null || role.isBlank()) && roomId < 0 && s != null) {
            int[] d = {0, -1, 1, 0, 0, 1, -1, 0};
            for (int i = 0; i < d.length; i += 2) {
                int nx = x + d[i], ny = y + d[i + 1];
                if (world.inBounds(nx, ny) && world.roomIds[nx][ny] >= 0) {
                    int adj = world.roomIds[nx][ny];
                    String adjRole = s.roomRole(adj);
                    if (adjRole != null && !adjRole.isBlank()) return new TopologyContext(true, x, y, adj, adjRole, circulation, true);
                }
            }
        }
        return new TopologyContext(true, x, y, roomId, role, circulation, false);
    }

    private static final class TopologyContext {
        final boolean inBounds;
        final int x, y, roomId;
        final String roomRole;
        final EconomicTopologyFramework.CirculationClass circulation;
        final boolean adjacentRoom;

        TopologyContext(boolean inBounds, int x, int y, int roomId, String roomRole, EconomicTopologyFramework.CirculationClass circulation, boolean adjacentRoom) {
            this.inBounds = inBounds;
            this.x = x; this.y = y; this.roomId = roomId; this.roomRole = roomRole; this.circulation = circulation; this.adjacentRoom = adjacentRoom;
        }
        static TopologyContext outOfBounds(int x, int y) { return new TopologyContext(false, x, y, -1, null, null, false); }
        String summary() {
            if (!inBounds) return "out-of-bounds coordinate " + x + "," + y;
            if (roomRole != null && !roomRole.isBlank()) return (adjacentRoom ? "adjacent room " : "room ") + roomId + " role " + roomRole;
            if (circulation != null) return "corridor/openwork circulation " + circulation.label;
            return "local topology unclassified at " + x + "," + y;
        }
    }

    private static String constructionIntent(BuildRecipe r, String blueprint) {
        String t = ((r == null ? "" : safe(r.name, "") + " " + r.symbol) + " " + safe(blueprint, "")).toLowerCase(Locale.ROOT);
        if (containsAny(t, "storage", "crate", "chest", "locker", "barrel", "tank", "depot", "stock")) return "logistics/storage support";
        if (containsAny(t, "workbench", "forge", "assembler", "machine", "lab", "laboratory", "condenser", "boiler", "generator", "pump", "production")) return "technical/production support";
        if (containsAny(t, "wall", "door", "barricade", "alarm", "turret", "trap", "security", "node")) return "access/security control";
        if (containsAny(t, "cot", "bed", "bunk", "berth", "mess", "kitchen")) return "labor-support habitation";
        if (containsAny(t, "shrine", "altar", "chapel", "relic", "temple")) return "ritual legitimacy support";
        if (containsAny(t, "counter", "stall", "shop", "market", "trade")) return "exchange-facing support";
        return "general base construction";
    }

    private static String constructionFitLine(String intent, TopologyContext ctx, EconomicLocalTopologyMetadataSurfaceAuthority.Surface s) {
        if (ctx == null || !ctx.inBounds) return "Topology fit: blocked from evaluation because the cursor is outside the loaded zone.";
        if (intent == null) intent = "general base construction";
        String c = shortContext(ctx).toLowerCase(Locale.ROOT);
        boolean good = false;
        if (intent.contains("logistics") && containsAny(c, "supply", "throughput", "exchange", "freight", "cargo", "depot")) good = true;
        else if (intent.contains("technical") && containsAny(c, "technical", "industrial", "machine", "maintenance", "support")) good = true;
        else if (intent.contains("security") && containsAny(c, "access-control", "security", "barracks", "checkpoint")) good = true;
        else if (intent.contains("labor") && containsAny(c, "labor-support", "habitation", "dormitory", "barracks")) good = true;
        else if (intent.contains("ritual") && containsAny(c, "ritual", "legitimacy", "temple", "shrine")) good = true;
        else if (intent.contains("exchange") && containsAny(c, "exchange", "market", "trade")) good = true;
        else if (intent.contains("general")) good = true;
        if (good) return "Topology fit: advisory match; the selected build reads coherently against this local room/circulation role.";
        return "Topology fit: advisory mismatch; legal placement may still be allowed, but later economy/logistics systems should treat this as an awkward or improvised location.";
    }

    private static String shortFit(String intent, TopologyContext ctx) {
        String line = constructionFitLine(intent, ctx, null).toLowerCase(Locale.ROOT);
        return line.contains("match") && !line.contains("mismatch") ? "coherent local fit" : (line.contains("outside") ? "outside local zone" : "awkward local fit");
    }

    private static boolean isGoodLogisticsSource(TopologyContext ctx) {
        String c = shortContext(ctx).toLowerCase(Locale.ROOT);
        return containsAny(c, "supply", "throughput", "exchange", "technical", "freight", "cargo", "depot", "stock", "storage");
    }

    private static boolean isGoodLogisticsDestination(TopologyContext ctx) {
        String c = shortContext(ctx).toLowerCase(Locale.ROOT);
        return containsAny(c, "supply", "throughput", "technical", "exchange", "freight", "cargo", "industrial", "machine", "maintenance", "nexus");
    }

    private static boolean isFreightLike(EconomicTopologyFramework.CirculationClass c) {
        return c == EconomicTopologyFramework.CirculationClass.FREIGHT_ARTERY
                || c == EconomicTopologyFramework.CirculationClass.CARGO_CORRIDOR
                || c == EconomicTopologyFramework.CirculationClass.INDUSTRIAL_SERVICE_LOOP;
    }

    private static String zoneLine(EconomicLocalTopologyMetadataSurfaceAuthority.Surface s) {
        if (s == null) return "unknown zone topology";
        String purpose = s.profile == null || s.profile.purpose == null ? "unclassified purpose" : s.profile.purpose.label;
        String age = s.profile == null || s.profile.ageBand == null ? "unknown age" : s.profile.ageBand.label;
        return purpose + "; " + age + "; pressure " + s.dominantPressure.label + "; primary circulation " + s.primaryCirculation.label;
    }

    private static String shortContext(TopologyContext ctx) {
        if (ctx == null) return "unknown topology context";
        if (!ctx.inBounds) return "out-of-bounds " + ctx.x + "," + ctx.y;
        if (ctx.roomRole != null && !ctx.roomRole.isBlank()) return (ctx.adjacentRoom ? "adjacent room " : "room ") + ctx.roomId + " / " + ctx.roomRole;
        if (ctx.circulation != null) return "corridor / " + ctx.circulation.label;
        return "unclassified tile " + ctx.x + "," + ctx.y;
    }

    private static ArrayList<String> cap(ArrayList<String> in, int max) {
        if (in == null) return new ArrayList<>();
        if (in.size() <= max) return in;
        ArrayList<String> out = new ArrayList<>(in.subList(0, Math.max(0, max - 1)));
        out.add("... " + (in.size() - out.size()) + " topology advisory line(s) hidden by bounded display.");
        return out;
    }

    private static boolean containsAny(String haystack, String... needles) {
        if (haystack == null) return false;
        for (String n : needles) if (n != null && haystack.contains(n)) return true;
        return false;
    }

    private static String safe(String s, String fallback) { return s == null || s.trim().isEmpty() ? fallback : s.trim(); }
}
