package mechanist;

import java.util.ArrayList;
import java.util.Locale;

/** Deterministic, bounded NPC event memory; no wall-clock time or shared RNG. */
final class NpcLifeMemoryAuthority {
    static final int MAX_EVENTS = 12;
    private static final long DUTY_WINDOW = (long) GamePanel.TURNS_PER_HOUR * GamePanel.HOURS_PER_DAY;
    private NpcLifeMemoryAuthority() { }

    static void remember(NpcEntity npc, long worldTurn, String category, String detail) {
        if (npc == null) return;
        String kind = safe(category, 24);
        if (kind.isBlank()) return;
        String faction = npc.faction == null ? Faction.NONE.name() : npc.faction.name();
        String text = safe(detail, 140);
        String event = Math.max(0L, worldTurn) + "\t" + kind + "\t" + faction + "\t" + text;
        if (!npc.lifeMemories.isEmpty() && event.equals(npc.lifeMemories.get(npc.lifeMemories.size() - 1))) return;
        npc.lifeMemories.add(event);
        while (npc.lifeMemories.size() > MAX_EVENTS) npc.lifeMemories.remove(0);
    }

    static void observeState(NpcEntity npc, long worldTurn, String previous) {
        if (npc == null || npc.hp <= 0 || npc.isAnimalActor()) return;
        String now = npc.state == null ? "" : npc.state;
        if (now.equalsIgnoreCase(previous == null ? "" : previous)) return;
        String kind;
        String lower = now.toLowerCase(Locale.ROOT);
        if (lower.contains("hostile") || lower.contains("seeking safety")) kind = "threat";
        else if (lower.contains("search needs") || lower.contains("seeking food")
                || lower.contains("seeking water")) kind = "deprivation";
        else if (lower.equals("guard") || lower.equals("patrol") || lower.equals("trade")
                || lower.equals("inspect")) kind = "duty";
        else return;
        // Do not allow routine guard/patrol oscillations to overwrite personal
        // memories; once per in-world day per category is enough.
        for (int i = npc.lifeMemories.size() - 1; i >= 0; i--) {
            String[] fields = npc.lifeMemories.get(i).split("\\t", 4);
            if (fields.length < 3) continue;
            if (!fields[1].equals(kind)) continue;
            try {
                if (worldTurn - Long.parseLong(fields[0]) < DUTY_WINDOW) return;
            } catch (NumberFormatException ignored) { }
            break;
        }
        remember(npc, worldTurn, kind, "State changed to " + now);
    }

    /**
     * A personally experienced, recent deprivation increases susceptibility to a
     * rival offer. Only memories recorded under the NPC's CURRENT allegiance
     * qualify: a new faction must not inherit blame for the old one's shortages.
     * The event list is bounded and persisted with the NPC, so the modifier is
     * stable across save/load and independent of shared random state.
     */
    static int recruitmentPressure(NpcEntity npc, long worldTurn) {
        if (npc == null || npc.faction == null || worldTurn < 0L) return 0;
        long window = 3L * GamePanel.TURNS_PER_HOUR * GamePanel.HOURS_PER_DAY;
        for (int i = npc.lifeMemories.size() - 1; i >= 0; i--) {
            String[] fields = npc.lifeMemories.get(i).split("\\t", 4);
            if (fields.length != 4 || !"deprivation".equals(fields[1])
                    || !npc.faction.name().equals(fields[2])) continue;
            try {
                long experienced = Long.parseLong(fields[0]);
                if (experienced <= worldTurn && worldTurn - experienced <= window) return 12;
            } catch (NumberFormatException ignored) { }
        }
        return 0;
    }

    /** Compact, saved-fact readback, never a fabricated emotion or intention. */
    static String latestSummary(NpcEntity npc) {
        if (npc == null || npc.lifeMemories.isEmpty()) return "";
        String[] fields = npc.lifeMemories.get(npc.lifeMemories.size() - 1).split("\\t", 4);
        if (fields.length != 4) return "";
        String detail = safe(fields[3], 100);
        return detail.isBlank() ? "" : detail + " (turn " + fields[0] + ")";
    }

    static String save(NpcEntity npc) {
        if (npc == null || npc.lifeMemories.isEmpty()) return "";
        return String.join("\n", npc.lifeMemories);
    }

    static void restore(NpcEntity npc, String persisted) {
        if (npc == null) return;
        npc.lifeMemories.clear();
        if (persisted == null || persisted.isBlank()) return;
        for (String line : persisted.split("\\n")) {
            String[] fields = line.split("\\t", 4);
            if (fields.length != 4) continue;
            try {
                long turn = Long.parseLong(fields[0]);
                if (turn < 0 || fields[1].isBlank() || fields[2].isBlank()) continue;
                String memory = turn + "\t" + safe(fields[1], 24) + "\t"
                        + safe(fields[2], 75) + "\t" + safe(fields[3], 140);
                npc.lifeMemories.add(memory);
                while (npc.lifeMemories.size() > MAX_EVENTS) npc.lifeMemories.remove(0);
            } catch (NumberFormatException ignored) { }
        }
    }

    private static String safe(String text, int limit) {
        if (text == null) return "";
        String normalized = text.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim();
        return normalized.substring(0, Math.min(limit, normalized.length()));
    }
}
