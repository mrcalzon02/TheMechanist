package mechanist;

import java.util.*;

/**
 * 0.9.09e construction feedback / efficiency bridge.
 *
 * Converts raw construction validation results into stable player-facing text and
 * keeps a tiny last-placement cache so cursor movement and confirmation do not
 * repeat expensive access-path work for the exact same recipe/tile/turn state.
 */
final class ConstructionPlacementFeedbackAuthority {
    static final String VERSION = "0.9.09f";
    private static final int MAX_RECENT = 16;
    private static final ArrayDeque<String> recent = new ArrayDeque<>();
    private static String lastKey = "";
    private static String lastRaw = "OK";
    private static long requests = 0L;
    private static long hits = 0L;
    private static long misses = 0L;
    private static long blocked = 0L;

    private ConstructionPlacementFeedbackAuthority() {}

    static String cachedRawPlacement(GamePanel g, BuildRecipe recipe, int x, int y) {
        requests++;
        String key = keyFor(g, recipe, x, y);
        if (key.equals(lastKey)) {
            hits++;
            return lastRaw;
        }
        misses++;
        String raw = g == null ? "No active construction context." : g.rawCanPlacePendingBuildAtUncached(x, y);
        lastKey = key;
        lastRaw = raw;
        if (!"OK".equals(raw)) blocked++;
        remember("placement raw=" + raw + " key=" + key);
        return raw;
    }

    static void invalidate() {
        lastKey = "";
        lastRaw = "OK";
    }

    static String formatSummary(BuildRecipe recipe, int x, int y, String placement, String components, int turns, int failChance) {
        String name = recipe == null ? "No build" : clean(recipe.name, "Unnamed build");
        String status = "OK".equals(placement) ? "VALID" : "BLOCKED";
        return name + " at " + x + "," + y + " | " + status + ": " + shortReason(placement)
                + " | " + components + " | " + turns + " turns | " + failChance + "% failure risk";
    }

    static String universalWindowContext(String placement) {
        if ("OK".equals(placement)) return "Construction placement valid.";
        return "Construction placement blocked: " + shortReason(placement);
    }

    static String shortReason(String placement) {
        if (placement == null || placement.trim().isEmpty()) return "unknown";
        String s = placement.trim();
        int context = s.indexOf(" context=");
        if (context >= 0) s = s.substring(0, context);
        if (s.length() > 180) s = s.substring(0, 177) + "...";
        return s;
    }

    static ArrayList<String> recentLines() {
        return new ArrayList<>(recent);
    }

    static String auditSummary() {
        long total = Math.max(1L, requests);
        long hitPct = (hits * 100L) / total;
        return "constructionFeedback version=" + VERSION + " requests=" + requests + " cacheHits=" + hits
                + " hitPct=" + hitPct + "% misses=" + misses + " blocked=" + blocked + " cache=last-placement-only no-render-loop";
    }

    private static String keyFor(GamePanel g, BuildRecipe recipe, int x, int y) {
        int turn = g == null ? -1 : g.turn;
        int objects = g == null || g.baseObjects == null ? 0 : g.baseObjects.size();
        int room = g == null ? -1 : g.claimedRoomId;
        String recipeKey = recipe == null ? "none" : recipe.name + ":" + recipe.symbol;
        return turn + "|" + room + "|" + objects + "|" + x + "," + y + "|" + recipeKey;
    }

    private static void remember(String line) {
        if (line == null) return;
        recent.addLast(line);
        while (recent.size() > MAX_RECENT) recent.removeFirst();
    }

    private static String clean(String s, String fallback) {
        if (s == null) return fallback;
        String t = s.trim();
        return t.isEmpty() ? fallback : t;
    }
}
