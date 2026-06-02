package mechanist;

import java.util.*;

/**
 * Compact chronology/personnel authority for Phase 3.6 runtime separation work.
 * It keeps universal world time distinct from the player's survived-turn counter
 * and gives actors stable birth-turn based ages without running a live aging loop.
 */
class AgeAndWorldTimeAuthority {
    static final String VERSION = "0.9.10ff";
    static final int TURNS_PER_YEAR = 365 * 24 * 100;

    private AgeAndWorldTimeAuthority() {}

    static String auditSummary() {
        return "ageWorldTime version=" + VERSION + " turnsPerYear=" + TURNS_PER_YEAR + " model=birth-turn-derived worldTurnSeparateFromPlayerTurn=true rankMinorGate=16";
    }

    static long birthTurnForAge(int ageYears, Random r) {
        int age = Math.max(0, ageYears);
        int dayOffset = r == null ? 0 : Math.floorMod(r.nextInt(), TURNS_PER_YEAR);
        return -((long)age * (long)TURNS_PER_YEAR) - dayOffset;
    }

    static int ageAtWorldTurn(long birthWorldTurn, long worldTurn) {
        long elapsed = Math.max(0L, worldTurn - birthWorldTurn);
        return (int)Math.max(0L, Math.min(240L, elapsed / Math.max(1, TURNS_PER_YEAR)));
    }

    static String bandForAge(int age) {
        if (age < 13) return "child";
        if (age < 16) return "teen";
        if (age < 21) return "young adult";
        if (age < 60) return "adult";
        return "elder";
    }

    static boolean isMinorAge(int age) { return age >= 0 && age < 16; }
    static boolean isYoungAdultAge(int age) { return age >= 16 && age < 21; }

    static int playerStartingAge(Random r) {
        if (r == null) r = new Random();
        int roll = Math.floorMod(r.nextInt(), 100);
        if (roll < 18) return 18 + r.nextInt(5);
        if (roll < 78) return 23 + r.nextInt(18);
        return 41 + r.nextInt(18);
    }

    static void initializeCandidateAge(Candidate c, Random r) {
        if (c == null) return;
        int age = c.ageYears > 0 ? c.ageYears : playerStartingAge(r);
        c.ageYears = age;
        c.birthWorldTurn = c.birthWorldTurn == 0L ? birthTurnForAge(age, r) : c.birthWorldTurn;
        c.ageBand = bandForAge(c.ageYears);
    }

    static void synchronizeCandidate(Candidate c, long worldTurn) {
        if (c == null) return;
        if (c.birthWorldTurn == 0L) initializeCandidateAge(c, new Random(Objects.hash(c.name, c.job, c.portraitIndex)));
        c.ageYears = ageAtWorldTurn(c.birthWorldTurn, worldTurn);
        c.ageBand = bandForAge(c.ageYears);
    }

    static int startingNpcAge(NpcEntity n, Faction f, ZoneType z, Random r) {
        if (r == null) r = new Random();
        String text = ((n == null ? "" : (n.role + " " + n.name + " " + (n.provenance == null ? "" : n.provenance.populationPool))) + " " + (z == null ? "" : z.label)).toLowerCase(Locale.ROOT);
        if (text.contains("schola") || text.contains("teen") || text.contains("youth")) return 12 + r.nextInt(4);
        if (text.contains("child") || text.contains("creche") || text.contains("nursery") || text.contains("daycare")) return 5 + r.nextInt(8);
        if (f == Faction.IMPERIAL_GUARD || f == Faction.ARBITES || f == Faction.SORORITAS) return 18 + r.nextInt(30);
        if (f == Faction.ADMINISTRATUM || f == Faction.MINISTORUM || f == Faction.NOBLE || (f != null && f.name().startsWith("NOBLE"))) return 18 + r.nextInt(58);
        if (f == Faction.MECHANICUS || (f != null && f.name().startsWith("MECHANICUS"))) return 18 + r.nextInt(74);
        if (f == Faction.BANDIT || (f != null && f.name().startsWith("GANGER")) || f == Faction.SCAVENGER) return 14 + r.nextInt(36);
        return 16 + r.nextInt(48);
    }

    static void initializeNpcAge(NpcEntity n, ZoneType z, Random r) {
        if (n == null || n.isAnimalActor()) return;
        int age = n.ageYears > 0 ? n.ageYears : startingNpcAge(n, n.faction, z, r);
        n.ageYears = age;
        n.birthWorldTurn = n.birthWorldTurn == 0L ? birthTurnForAge(age, r) : n.birthWorldTurn;
        n.ageBand = bandForAge(n.ageYears);
    }

    static void synchronizeNpc(NpcEntity n, long worldTurn) {
        if (n == null || n.isAnimalActor()) return;
        if (n.birthWorldTurn == 0L) initializeNpcAge(n, null, new Random(Objects.hash(n.id, n.name, n.numericId)));
        n.ageYears = ageAtWorldTurn(n.birthWorldTurn, worldTurn);
        n.ageBand = bandForAge(n.ageYears);
        n.ensureRankIdentity(new Random(n.numericId == 0 ? Objects.hash(n.name, n.faction) : n.numericId));
    }

    static int synchronizeLoadedActors(GamePanel g, String reason) {
        if (g == null) return 0;
        int count = 0;
        synchronizeCandidate(g.active, g.worldTurn); if (g.active != null) count++;
        for (Candidate c : g.candidates) { synchronizeCandidate(c, g.worldTurn); count++; }
        if (g.world != null && g.world.npcs != null) {
            for (NpcEntity n : g.world.npcs) { synchronizeNpc(n, g.worldTurn); count++; }
        }
        return count;
    }

    static String worldTimeText(long worldTurn) {
        long turns = Math.max(0L, worldTurn);
        long hour = (turns / 100L) % 24L;
        long day = ((turns / 100L) / 24L) % 365L + 1L;
        long year = ((turns / 100L) / (24L * 365L)) + 1L;
        return "Y" + year + " D" + day + " H" + hour + " +" + (turns % 100L) + "/100t";
    }
}
