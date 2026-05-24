package mechanist;

import java.awt.*;
import java.util.*;

final class EconomicGenerationBiasAuthority {
    static final String VERSION = "0.9.10en";

    static final class Result {
        final ZoneType zone;
        final EconomicTopologyFramework.ZonePurposeProfile profile;
        final EconomicTopologyFramework.PressureType dominantPressure;
        final EconomicTopologyFramework.CirculationClass primaryCirculation;
        final int roomsAnnotated;
        final int roomsReviewed;
        final int noteCount;

        Result(ZoneType zone,
               EconomicTopologyFramework.ZonePurposeProfile profile,
               EconomicTopologyFramework.PressureType dominantPressure,
               EconomicTopologyFramework.CirculationClass primaryCirculation,
               int roomsAnnotated,
               int roomsReviewed,
               int noteCount) {
            this.zone = zone;
            this.profile = profile;
            this.dominantPressure = dominantPressure;
            this.primaryCirculation = primaryCirculation;
            this.roomsAnnotated = roomsAnnotated;
            this.roomsReviewed = roomsReviewed;
            this.noteCount = noteCount;
        }

        String summary() {
            String zoneLabel = zone == null ? "unknown zone" : zone.label;
            String purpose = profile == null || profile.purpose == null ? "unclassified purpose" : profile.purpose.label;
            String pressure = dominantPressure == null ? "none" : dominantPressure.label;
            String circulation = primaryCirculation == null ? "none" : primaryCirculation.label;
            return "economicGenerationBias version=" + VERSION
                    + " zone=" + zoneLabel
                    + " purpose=" + purpose
                    + " primaryCirculation=" + circulation
                    + " dominantPressure=" + pressure
                    + " roomsAnnotated=" + roomsAnnotated + "/" + roomsReviewed
                    + " notes=" + noteCount
                    + " liveEconomy=false";
        }
    }

    static int weightRoomSelection(ZoneType zone, ArrayList<RoomProfile> roomPool) {
        if (roomPool == null || roomPool.isEmpty()) return 0;
        EconomicTopologyFramework.ZonePurposeProfile profile = EconomicTopologyFramework.profileFor(zone);
        ArrayList<RoomProfile> original = new ArrayList<>(roomPool);
        int added = 0;
        for (RoomProfile rp : original) {
            int bonus = Math.min(4, roomSelectionBonus(profile, rp));
            for (int i = 0; i < bonus; i++) {
                roomPool.add(copyRoomProfile(rp));
                added++;
            }
        }
        return added;
    }

    static Result apply(World world, Random rng) {
        if (world == null) return new Result(null, null, null, null, 0, 0, 0);
        EconomicTopologyFramework.ZonePurposeProfile profile = EconomicTopologyFramework.profileFor(world.zoneType);
        EconomicTopologyFramework.PressureType dominantPressure = dominantPressure(profile);
        EconomicTopologyFramework.CirculationClass primaryCirculation = primaryCirculation(profile);
        if (world.economicTopologyGenerationNotes == null) world.economicTopologyGenerationNotes = new ArrayList<>();
        world.economicTopologyGenerationNotes.clear();

        int reviewed = world.roomProfiles == null ? 0 : world.roomProfiles.size();
        int annotated = 0;
        for (int i = 0; i < reviewed; i++) {
            RoomProfile rp = world.roomProfiles.get(i);
            if (rp == null) continue;
            Faction owner = i < world.roomFactions.size() ? world.roomFactions.get(i) : Faction.NONE;
            String role = roomEconomicRole(profile, rp, i, owner);
            if (role == null || role.isBlank()) continue;
            String note = "Economic topology: " + role + " Circulation reading: " + primaryCirculation.label + "; dominant pressure: " + dominantPressure.label + ".";
            if (rp.featureText == null || rp.featureText.isBlank()) rp.featureText = note;
            else if (!rp.featureText.contains("Economic topology:")) rp.featureText = rp.featureText + " " + note;
            annotated++;
            if (world.economicTopologyGenerationNotes.size() < 10) {
                world.economicTopologyGenerationNotes.add("room " + i + " " + rp.name + ": " + role);
            }
        }
        world.economicTopologyGenerationSummary = summaryLine(world.zoneType, profile, dominantPressure, primaryCirculation, annotated, reviewed);
        return new Result(world.zoneType, profile, dominantPressure, primaryCirculation, annotated, reviewed, world.economicTopologyGenerationNotes.size());
    }

    static ArrayList<String> infopediaLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Economic Generation Bias Authority " + VERSION);
        lines.add("Purpose: lets zone generation read economic-topology profiles while remaining a local, non-simulating generation layer.");
        lines.add("Current behavior: room profile pools are weighted by zone purpose, pressure fields, and circulation class; generated room feature text receives topology notes after faction assignment.");
        lines.add("Boundary: no district conversion, no autonomous workers, no stock movement, no demand propagation, no pathfinding, and no full-map pressure scan.");
        lines.add("Consumer path: RoomProfile.forZone uses weightRoomSelection(); World.generate applies local room annotations after contestable room insertion.");
        lines.add("Next dependency: route/corridor-tagging can consume the same profile once a safe, cached corridor metadata surface exists.");
        return lines;
    }

    static ArrayList<String> auditLines(World world) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Economic Generation Bias audit");
        lines.add("Authority version: " + VERSION + ".");
        if (world == null) {
            lines.add("No active world loaded for selected-zone bias report.");
        } else {
            lines.add(world.economicTopologyGenerationSummary == null ? "No generation-bias summary recorded for the active world." : world.economicTopologyGenerationSummary);
            if (world.economicTopologyGenerationNotes != null && !world.economicTopologyGenerationNotes.isEmpty()) {
                lines.add("Sample room topology notes:");
                lines.addAll(world.economicTopologyGenerationNotes);
            }
        }
        lines.add("Rule check: this authority only weights generation selection and description metadata; it does not schedule, simulate, or mutate economic state after generation.");
        return lines;
    }

    static ArrayList<String> worldSummaryLines(World world) {
        ArrayList<String> lines = new ArrayList<>();
        if (world == null) return lines;
        if (world.economicTopologyGenerationSummary != null && !world.economicTopologyGenerationSummary.isBlank()) {
            lines.add("Economic generation bias: " + world.economicTopologyGenerationSummary);
        }
        if (world.economicTopologyGenerationNotes != null && !world.economicTopologyGenerationNotes.isEmpty()) {
            lines.add("Topology sample: " + world.economicTopologyGenerationNotes.get(0));
        }
        return lines;
    }

    static String corridorInspectionLine(World world, int x, int y) {
        if (world == null || !world.inBounds(x, y)) return null;
        char tile = world.tiles[x][y];
        if (world.roomIds[x][y] >= 0) return null;
        if (!(world.isCorridorGlyph(tile) || tile == '=' || tile == ':' || tile == '+' || tile == '/' || tile == 'D')) return null;
        EconomicTopologyFramework.ZonePurposeProfile profile = EconomicTopologyFramework.profileFor(world.zoneType);
        EconomicTopologyFramework.CirculationClass circulation = classifyCorridor(profile, tile);
        EconomicTopologyFramework.PressureType pressure = dominantPressure(profile);
        return "ECONOMIC CIRCULATION: " + circulation.label + " under " + pressure.label + " pressure; this is descriptive routing metadata, not a live hauling reservation.";
    }

    private static int roomSelectionBonus(EconomicTopologyFramework.ZonePurposeProfile profile, RoomProfile rp) {
        if (profile == null || rp == null) return 0;
        String text = roomText(rp);
        int score = 0;
        if (pressure(profile, EconomicTopologyFramework.PressureType.INDUSTRIAL) >= 7 && containsAny(text, "forge", "machine", "workshop", "assembler", "smelter", "relay", "boiler", "laboratorium", "maintenance")) score += 2;
        if (pressure(profile, EconomicTopologyFramework.PressureType.LOGISTICS) >= 7 && containsAny(text, "warehouse", "storehouse", "depot", "rail", "cargo", "ration", "armory", "freight", "storefront")) score += 2;
        if (pressure(profile, EconomicTopologyFramework.PressureType.LABOR) >= 7 && containsAny(text, "dormitory", "barracks", "kitchen", "laundry", "clinic", "servant", "hab", "mess")) score += 2;
        if (pressure(profile, EconomicTopologyFramework.PressureType.SECURITY) >= 7 && containsAny(text, "security", "watch", "armory", "checkpoint", "evidence", "holding", "barricade", "guard")) score += 2;
        if (pressure(profile, EconomicTopologyFramework.PressureType.RELIGIOUS) >= 7 && containsAny(text, "shrine", "chapel", "relic", "temple", "ritual", "pilgrim")) score += 2;
        if ((pressure(profile, EconomicTopologyFramework.PressureType.POLLUTION) >= 7 || pressure(profile, EconomicTopologyFramework.PressureType.DECAY) >= 7) && containsAny(text, "trash", "scrap", "sewer", "sanitation", "recycler", "sluice", "fungus", "maintenance")) score += 2;
        if (pressure(profile, EconomicTopologyFramework.PressureType.BLACK_MARKET) >= 6 && containsAny(text, "stash", "pawn", "debt", "contraband", "back room", "hidden", "gang", "whisper", "storefront")) score += 2;
        if (usesCirculation(profile, EconomicTopologyFramework.CirculationClass.FREIGHT_ARTERY) && containsAny(text, "warehouse", "rail", "cargo", "depot", "storehouse")) score++;
        if (usesCirculation(profile, EconomicTopologyFramework.CirculationClass.NOBLE_BOULEVARD) && containsAny(text, "servant", "salon", "luxury", "house", "audience", "laundry")) score++;
        if (usesCirculation(profile, EconomicTopologyFramework.CirculationClass.SEWER_TRUNK) && containsAny(text, "sewer", "sump", "drain", "sluice", "fungus")) score++;
        if (usesCirculation(profile, EconomicTopologyFramework.CirculationClass.BARRACKS_ACCESS_GRID) && containsAny(text, "barracks", "armory", "ration", "muster", "medical")) score++;
        return score;
    }

    private static String roomEconomicRole(EconomicTopologyFramework.ZonePurposeProfile profile, RoomProfile rp, int roomIndex, Faction owner) {
        if (profile == null || rp == null) return null;
        if (roomIndex == 0) return "central nexus for " + profile.purpose.label + "; it anchors how this district distributes movement and services.";
        String text = roomText(rp);
        if (containsAny(text, "warehouse", "storehouse", "depot", "cargo", "rail", "ration", "armory")) return "stock, throughput, or controlled supply room inside the " + profile.purpose.label + ".";
        if (containsAny(text, "forge", "machine", "workshop", "assembler", "smelter", "relay", "boiler", "laboratorium", "maintenance")) return "technical or production support room shaped by " + dominantPressure(profile).label + " pressure.";
        if (containsAny(text, "dormitory", "barracks", "hab", "servant", "kitchen", "laundry", "mess")) return "labor-support room; it explains where bodies sleep, eat, wait, and get turned back into shifts.";
        if (containsAny(text, "security", "watch", "holding", "checkpoint", "evidence", "guard", "barricade")) return "access-control room; it hardens circulation and expresses local authority.";
        if (containsAny(text, "shrine", "chapel", "temple", "relic", "ritual")) return "ritual-service room; it gives the district social legitimacy or forbidden gravity.";
        if (containsAny(text, "trash", "scrap", "sewer", "sluice", "recycler", "fungus")) return "decay/reclamation room; waste and salvage are part of this district's industrial ecology.";
        if (containsAny(text, "storefront", "counter", "pawn", "debt", "barter", "market", "trade")) return "exchange-facing room; goods, favors, permissions, and lies become local throughput.";
        if (owner != null && owner != Faction.NONE) return owner.label + " room interpreted through " + profile.purpose.label + ".";
        return "ordinary room carrying background pressure from the " + profile.purpose.label + ".";
    }

    private static String summaryLine(ZoneType zone,
                                      EconomicTopologyFramework.ZonePurposeProfile profile,
                                      EconomicTopologyFramework.PressureType pressure,
                                      EconomicTopologyFramework.CirculationClass circulation,
                                      int annotated,
                                      int reviewed) {
        String zoneLabel = zone == null ? "unknown zone" : zone.label;
        String purpose = profile == null ? "unclassified purpose" : profile.purpose.label;
        String age = profile == null ? "unknown infrastructure age" : profile.ageBand.label;
        return zoneLabel + " generated as " + purpose + "; " + age + "; primary circulation " + circulation.label + "; dominant pressure " + pressure.label + "; room topology notes " + annotated + "/" + reviewed + ".";
    }

    private static EconomicTopologyFramework.PressureType dominantPressure(EconomicTopologyFramework.ZonePurposeProfile profile) {
        EconomicTopologyFramework.PressureType best = EconomicTopologyFramework.PressureType.INDUSTRIAL;
        int bestValue = -1;
        if (profile == null || profile.pressure == null) return best;
        for (EconomicTopologyFramework.PressureType t : EconomicTopologyFramework.PressureType.values()) {
            int v = profile.pressure.get(t);
            if (v > bestValue) { bestValue = v; best = t; }
        }
        return best;
    }

    private static EconomicTopologyFramework.CirculationClass primaryCirculation(EconomicTopologyFramework.ZonePurposeProfile profile) {
        if (profile != null && profile.circulation != null && !profile.circulation.isEmpty()) return profile.circulation.get(0);
        return EconomicTopologyFramework.CirculationClass.PUBLIC_SERVICE_SPINE;
    }

    private static EconomicTopologyFramework.CirculationClass classifyCorridor(EconomicTopologyFramework.ZonePurposeProfile profile, char tile) {
        if (tile == '=' || tile == ':') {
            if (usesCirculation(profile, EconomicTopologyFramework.CirculationClass.FREIGHT_ARTERY)) return EconomicTopologyFramework.CirculationClass.FREIGHT_ARTERY;
            if (usesCirculation(profile, EconomicTopologyFramework.CirculationClass.NOBLE_BOULEVARD)) return EconomicTopologyFramework.CirculationClass.NOBLE_BOULEVARD;
            if (usesCirculation(profile, EconomicTopologyFramework.CirculationClass.BARRACKS_ACCESS_GRID)) return EconomicTopologyFramework.CirculationClass.BARRACKS_ACCESS_GRID;
        }
        if (tile == '+' || tile == '/' || tile == 'D') return primaryCirculation(profile);
        return primaryCirculation(profile);
    }

    private static int pressure(EconomicTopologyFramework.ZonePurposeProfile profile, EconomicTopologyFramework.PressureType type) {
        if (profile == null || profile.pressure == null || type == null) return 0;
        return profile.pressure.get(type);
    }

    private static boolean usesCirculation(EconomicTopologyFramework.ZonePurposeProfile profile, EconomicTopologyFramework.CirculationClass c) {
        return profile != null && profile.circulation != null && profile.circulation.contains(c);
    }

    private static boolean containsAny(String haystack, String... needles) {
        if (haystack == null) return false;
        for (String n : needles) if (n != null && haystack.contains(n)) return true;
        return false;
    }

    private static String roomText(RoomProfile rp) {
        if (rp == null) return "";
        return ((rp.name == null ? "" : rp.name) + " "
                + (rp.descriptor == null ? "" : rp.descriptor) + " "
                + (rp.featureText == null ? "" : rp.featureText)).toLowerCase(Locale.ROOT);
    }

    private static RoomProfile copyRoomProfile(RoomProfile rp) {
        if (rp == null) return RoomProfile.generic();
        String[] loot = rp.loot == null ? new String[]{"scrap bundle"} : Arrays.copyOf(rp.loot, rp.loot.length);
        char[] contents = rp.contents == null ? new char[]{'p'} : Arrays.copyOf(rp.contents, rp.contents.length);
        RoomProfile copy = new RoomProfile(rp.name, rp.descriptor, rp.scavengeChance, rp.faction, loot, contents);
        copy.featureText = rp.featureText;
        return copy;
    }

    private EconomicGenerationBiasAuthority() {}
}
