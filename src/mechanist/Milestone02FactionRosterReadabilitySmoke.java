package mechanist;

import java.util.ArrayList;
import java.util.List;

final class Milestone02FactionRosterReadabilitySmoke {
    public static void main(String[] args) {
        ArrayList<RecruitWorker> recruits = new ArrayList<>();
        RecruitWorker first = new RecruitWorker("Hest Var", "Forge aide", Faction.MECHANIST_COLLEGIA, 4, 6);
        first.duty = "machine labor";
        recruits.add(first);
        recruits.add(new RecruitWorker("Pell", "Guard", Faction.CIVIC_WARDENS, 3, 2));

        BaseObject forge = new BaseObject("Micro-forge", 'Y', 2, 2, 0, 0);
        forge.assignedWorker = "Hest Var";
        List<String> lines = FactionRosterReadabilityAuthority.summary(recruits, List.of(forge), true, 4);
        requireContains(lines, "Available members: 2", "member count");
        requireContains(lines, "separate tracks", "command track separation");
        requireContains(lines, "player retains the founded-faction command role", "player command role");
        requireContains(lines, "assignment is supported through station management", "staffing route");
        requireContains(lines, "no personal item ledger", "equipment privacy boundary");
        requireContains(lines, "rank and current world location are not stored", "record boundary");
        requireContains(lines, "Hest Var", "member identity");
        requireContains(lines, "machine labor", "duty");
        requireContains(lines, "trained skill", "skill band");
        requireContains(lines, "assigned to recorded station", "assigned availability");
        requireContains(lines, "station Micro-forge", "recorded station");
        requireContains(lines, "reliable", "loyalty band");
        requireContains(lines, "fragile", "low loyalty band");
        requireContains(lines, "warning: fragile loyalty", "personnel warning");

        List<String> empty = FactionRosterReadabilityAuthority.summary(List.of(), 4);
        requireContains(empty, "No recruited faction members", "empty roster");
        for (String line : lines) rejectLeaks(line);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void rejectLeaks(String line) {
        if (PlayerFacingText.containsLikelyLeak(line)) throw new AssertionError("Roster summary leaked implementation text: " + line);
    }

    private Milestone02FactionRosterReadabilitySmoke() {}
}
