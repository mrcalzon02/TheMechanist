package mechanist;

import java.util.ArrayList;
import java.util.List;

final class FactionRosterReadabilityAuthority {
    private FactionRosterReadabilityAuthority() {}

    static List<String> summary(List<RecruitWorker> recruits, int limit) {
        return summary(recruits, List.of(), false, limit);
    }

    static List<String> summary(GamePanel game, int limit) {
        return summary(game == null ? List.of() : game.factionRecruits,
                game == null ? List.of() : game.baseObjects, game != null && game.baseClaimed, limit);
    }

    static List<String> summary(List<RecruitWorker> recruits, List<BaseObject> stations, boolean playerFactionFounded, int limit) {
        ArrayList<String> lines = new ArrayList<>();
        int count = recruits == null ? 0 : recruits.size();
        lines.add("Available members: " + count + ".");
        lines.add("Command structure: player command membership and the NPC worker roster use separate tracks"
                + (playerFactionFounded ? "; the player retains the founded-faction command role." : "; no player faction is currently founded."));
        lines.add("Staffing authority: machine and defense station assignment is supported through station management; direct duty editing is not available here.");
        lines.add("Equipment privacy: recruits have no personal item ledger in this roster, so member inventory viewing, transfer, and equipment commands remain unavailable.");
        lines.add("Record limits: recruit rank and current world location are not stored in this roster yet.");
        if (count == 0) {
            lines.add("No recruited faction members are currently available.");
            return lines;
        }
        int shown = Math.max(1, limit);
        for (int i = 0; i < Math.min(count, shown); i++) {
            RecruitWorker worker = recruits.get(i);
            if (worker == null) continue;
            String station = assignedStation(worker, stations);
            lines.add(safe(worker.name, "Unnamed member") + " / " + safe(worker.role, "worker")
                    + " / " + (worker.faction == null ? Faction.NONE.label : worker.faction.label)
                    + " / assignment " + safe(worker.duty, "labor")
                    + " / " + skillBand(worker.skill)
                    + " / loyalty " + loyaltyBand(worker.loyalty)
                    + " / " + availabilityBand(worker.duty, station)
                    + " / station " + station
                    + " / " + warningBand(worker.loyalty) + ".");
        }
        if (count > shown) lines.add((count - shown) + " additional member(s) not shown in this compact view.");
        return lines;
    }

    static String loyaltyBand(int loyalty) {
        if (loyalty >= 7) return "steadfast";
        if (loyalty >= 5) return "reliable";
        if (loyalty >= 3) return "uncertain";
        return "fragile";
    }

    static String skillBand(int skill) {
        if (skill >= 6) return "expert skill";
        if (skill >= 4) return "trained skill";
        if (skill >= 2) return "working skill";
        return "novice skill";
    }

    static String availabilityBand(String duty) {
        return availabilityBand(duty, "none");
    }

    static String availabilityBand(String duty, String station) {
        if (station != null && !station.isBlank() && !"none".equalsIgnoreCase(station)) return "assigned to recorded station";
        String assigned = safe(duty, "labor").toLowerCase();
        if (assigned.equals("labor") || assigned.contains("reserve") || assigned.contains("unassigned")) return "available for station staffing";
        return "duty recorded; station assignment available through station management";
    }

    private static String assignedStation(RecruitWorker worker, List<BaseObject> stations) {
        if (worker == null || worker.name == null || stations == null) return "none";
        for (BaseObject station : stations) {
            if (station == null || station.assignedWorker == null) continue;
            if (worker.name.equalsIgnoreCase(station.assignedWorker.trim())) return safe(station.name, "recorded station");
        }
        return "none";
    }

    static String warningBand(int loyalty) {
        if (loyalty <= 2) return "warning: fragile loyalty";
        if (loyalty <= 4) return "watch loyalty under pressure";
        return "no immediate personnel warning";
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
