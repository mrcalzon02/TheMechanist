package mechanist;

import java.util.Locale;
import java.util.Random;

/** Connects need-provider fixtures to actual player/NPC need restoration. */
final class NeedSupplyInteractionAuthority {
    private NeedSupplyInteractionAuthority() {}

    static boolean tryUse(GamePanel g, MapObjectState m) {
        if (g == null || m == null) return false;
        NeedProfile profile = profileFor(m);
        if (!profile.any()) return false;

        boolean consumable = profile.food || profile.water;
        if (consumable && m.cooldownUntilTurn > g.turn) {
            g.logEvent(shortLabel(m) + " has already been used recently. It needs time to restock.");
            g.advanceTurn("checks a depleted need fixture.");
            g.repaint();
            return true;
        }

        StringBuilder report = new StringBuilder(shortLabel(m)).append(": ");
        boolean changed = false;
        boolean wrote = false;

        if (profile.food && g.food < GamePanel.MAX_FOOD_WATER) {
            int before = g.food;
            g.food = Math.min(GamePanel.MAX_FOOD_WATER, g.food + 28);
            append(report, "food " + before + "->" + g.food);
            changed = true;
            wrote = true;
        }
        if (profile.water && g.water < GamePanel.MAX_FOOD_WATER) {
            int before = g.water;
            g.water = Math.min(GamePanel.MAX_FOOD_WATER, g.water + 28);
            append(report, "water " + before + "->" + g.water);
            changed = true;
            wrote = true;
        }
        if (profile.sleep && (g.sleepNeed > 0 || g.fatigue > 0)) {
            int sleepBefore = g.sleepNeed;
            int fatigueBefore = g.fatigue;
            g.sleepNeed = Math.max(0, g.sleepNeed - 10);
            g.fatigue = Math.max(0, g.fatigue - 2);
            append(report, "rest sleep " + sleepBefore + "->" + g.sleepNeed + ", fatigue " + fatigueBefore + "->" + g.fatigue);
            changed = true;
            wrote = true;
        }
        if (profile.news) {
            String bulletin = ImperialNewsNetworkApi.broadcastBulletin(g, "need-provider/" + safe(m.type), localRandom(g, m, 71));
            g.lastBroadcastReport = bulletin;
            g.lastInnNewsIssue = bulletin;
            append(report, "news received");
            g.logEvent("NEWS SOURCE: " + bulletin);
            g.gainXp("Investigation", 1, "used a news need-provider fixture");
            changed = true;
            wrote = true;
        }
        if (profile.entertainment && (g.fatigue > 0 || g.suspicion > 0)) {
            int fatigueBefore = g.fatigue;
            int suspicionBefore = g.suspicion;
            g.fatigue = Math.max(0, g.fatigue - 1);
            g.suspicion = Math.max(0, g.suspicion - 1);
            append(report, "decompression fatigue " + fatigueBefore + "->" + g.fatigue + ", suspicion " + suspicionBefore + "->" + g.suspicion);
            changed = true;
            wrote = true;
        }
        if (profile.safety && (g.suspicion > 0 || g.gangHeat > 0)) {
            int suspicionBefore = g.suspicion;
            int heatBefore = g.gangHeat;
            g.suspicion = Math.max(0, g.suspicion - 2);
            g.gangHeat = Math.max(0, g.gangHeat - 1);
            append(report, "safety suspicion " + suspicionBefore + "->" + g.suspicion + ", heat " + heatBefore + "->" + g.gangHeat);
            changed = true;
            wrote = true;
        }

        if (!wrote) {
            append(report, "available, but your matching needs are already stable");
        }
        g.logEvent(report.toString() + ".");
        if (changed) g.gainXp(profile.skill(), 1, "used " + profile.primaryNeed() + " need-provider fixture");
        m.vendCount++;
        m.cooldownUntilTurn = g.turn + FixtureInteractionRegistry.cooldownFor(m.type, consumable ? 22 : 10);
        g.sounds.playDistantCue(FixtureInteractionRegistry.soundFor(m.type, profile.sound()), 3, g.options);
        g.advanceTurn("uses " + shortLabel(m).toLowerCase(Locale.ROOT) + " for " + profile.primaryNeed() + ".");
        g.repaint();
        return true;
    }

    static boolean applyNpcNeedAtProvider(World world, NpcEntity npc, String need) {
        if (world == null || npc == null || need == null) return false;
        NeedProfile profile = profileNear(world, npc.x, npc.y, need);
        if (!profile.any()) return false;
        switch (need.trim().toLowerCase(Locale.ROOT)) {
            case "food" -> npc.hunger = Math.max(0, npc.hunger - 4);
            case "water" -> npc.thirst = Math.max(0, npc.thirst - 4);
            case "sleep" -> npc.sleepDebt = Math.max(0, npc.sleepDebt - 5);
            case "safety" -> npc.state = "Reassured";
            case "entertainment" -> npc.state = "Passing Time";
            case "news" -> npc.state = "Reading News";
            default -> { return false; }
        }
        if (npc.hunger < 10 && npc.thirst < 10 && npc.sleepDebt < 15) {
            npc.needTarget = null;
            npc.needTargetKind = "none";
            if (!"Reassured".equals(npc.state) && !"Passing Time".equals(npc.state) && !"Reading News".equals(npc.state)) npc.state = "Idle";
        }
        return true;
    }

    private static NeedProfile profileNear(World world, int x, int y, String need) {
        String n = need.trim().toLowerCase(Locale.ROOT);
        NeedProfile best = NeedProfile.NONE;
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) {
            if (Math.abs(dx) + Math.abs(dy) > 1) continue;
            int tx = x + dx, ty = y + dy;
            if (!world.inBounds(tx, ty)) continue;
            MapObjectState m = world.mapObjectAt(tx, ty);
            if (m != null) {
                NeedProfile p = profileFor(m);
                if (p.supplies(n)) return p;
                if (p.any()) best = p;
            }
            if (world.tileMatchesNeed(world.tiles[tx][ty], n)) return NeedProfile.forNeed(n);
        }
        return best.supplies(n) ? best : NeedProfile.NONE;
    }

    static NeedProfile profileFor(MapObjectState m) {
        if (m == null) return NeedProfile.NONE;
        String type = AssetIntegrationDisciplineAuthority.canonicalType(m.type);
        String text = ((type == null ? "" : type) + " " + safe(m.label) + " " + safe(m.stockState)).toLowerCase(Locale.ROOT);
        boolean food = contains(text, "food", "ration", "meal", "mess", "galley", "kitchen", "canteen", "nutrient", "algae", "hydroponic", "fungal", "refrigerator", "stove", "prep", "shop", "market", "vending");
        boolean water = contains(text, "water", "canteen", "bottle", "sink", "cistern", "barrel", "pump", "tap", "condenser", "filter", "hydroponic", "algae", "vending", "shop", "bar");
        boolean sleep = contains(text, "sleep", "cot", "bed", "bunk", "dorm", "hab", "rest", "bench");
        boolean safety = contains(text, "safety", "light", "security", "guard", "precinct", "alarm", "bank", "sanctuary", "governor", "service counter");
        boolean entertainment = contains(text, "entertainment", "bar", "booth", "stool", "table", "radio", "pict", "screen", "shrine", "market", "bottle shelf", "keg");
        boolean news = contains(text, "news", "newspaper", "broadcast", "radio", "pict", "info kiosk", "journal", "notice", "concord news");
        return new NeedProfile(food, water, sleep, safety, entertainment, news);
    }

    private static void append(StringBuilder b, String text) {
        if (b == null || text == null || text.isBlank()) return;
        int len = b.length();
        if (len > 0 && b.charAt(len - 1) != ' ') b.append("; ");
        b.append(text);
    }

    private static boolean contains(String text, String... needles) {
        if (text == null || needles == null) return false;
        for (String needle : needles) if (needle != null && !needle.isBlank() && text.contains(needle)) return true;
        return false;
    }

    private static Random localRandom(GamePanel g, MapObjectState m, int salt) {
        long seed = (g == null ? 0 : g.seed) ^ (g == null ? 0 : g.turn * 65537L)
                ^ java.util.Objects.hash(m == null ? "" : m.id, m == null ? 0 : m.x, m == null ? 0 : m.y, salt);
        return new Random(seed);
    }

    private static String shortLabel(MapObjectState m) {
        String s = m == null || m.label == null ? "need fixture" : m.label;
        int slash = s.indexOf('/');
        if (slash > 0) s = s.substring(0, slash).trim();
        return s.isBlank() ? "need fixture" : s;
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace(';', ',');
    }

    record NeedProfile(boolean food, boolean water, boolean sleep, boolean safety, boolean entertainment, boolean news) {
        static final NeedProfile NONE = new NeedProfile(false, false, false, false, false, false);

        static NeedProfile forNeed(String need) {
            return switch (need == null ? "" : need) {
                case "food" -> new NeedProfile(true, false, false, false, false, false);
                case "water" -> new NeedProfile(false, true, false, false, false, false);
                case "sleep" -> new NeedProfile(false, false, true, false, false, false);
                case "safety" -> new NeedProfile(false, false, false, true, false, false);
                case "entertainment" -> new NeedProfile(false, false, false, false, true, false);
                case "news" -> new NeedProfile(false, false, false, false, false, true);
                default -> NONE;
            };
        }

        boolean any() { return food || water || sleep || safety || entertainment || news; }

        boolean supplies(String need) {
            return switch (need == null ? "" : need) {
                case "food" -> food;
                case "water" -> water;
                case "sleep" -> sleep;
                case "safety" -> safety;
                case "entertainment" -> entertainment;
                case "news" -> news;
                default -> false;
            };
        }

        String primaryNeed() {
            if (food) return "food";
            if (water) return "water";
            if (sleep) return "rest";
            if (news) return "news";
            if (entertainment) return "entertainment";
            if (safety) return "safety";
            return "need";
        }

        String skill() {
            if (news) return "Investigation";
            if (entertainment) return "Social";
            if (safety) return "Survival";
            return "Survival";
        }

        String sound() {
            if (news) return "ambient_radio";
            if (sleep || water) return "ambient_pipe";
            if (entertainment) return "ambient_machine";
            return "ambient_chime";
        }
    }
}
