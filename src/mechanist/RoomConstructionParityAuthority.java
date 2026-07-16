package mechanist;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/** Audits room-stamp acquisition and faction-use parity without changing construction rules. */
final class RoomConstructionParityAuthority {
    record RoomParityEntry(String roomName, String sourceZone, String owningFaction,
                           String playerAcquisitionStatus, String factionUseStatus,
                           String matchingBlueprint, String exceptionNote) { }

    private RoomConstructionParityAuthority() { }

    static RoomParityEntry liveEntry(RoomProfile profile, ZoneType zone) {
        if (profile == null) {
            return new RoomParityEntry("Unknown room", zone == null ? "Unknown Zone" : zone.label,
                    "None", "non-acquirable: no room profile is available",
                    "faction use unknown", "unmapped", "");
        }
        return entryFor(profile, zone);
    }

    static BuildRecipe liveMatchingRecipe(RoomProfile profile) {
        return profile == null ? null : recipeNamed(matchingBlueprint(profile));
    }

    static List<RoomParityEntry> roomEntries() {
        LinkedHashMap<String, RoomParityEntry> entries = new LinkedHashMap<>();
        for (ZoneType zone : ZoneType.values()) {
            ArrayList<RoomProfile> profiles = new ArrayList<>();
            RoomProfile.addInfrastructureRooms(profiles, zone);
            profiles.add(RoomProfile.centralPlaza(zone, isSewer(zone), new Random(zone.ordinal() + 71L)));
            profiles.add(RoomProfile.neutralContestRoom(zone, new Random(zone.ordinal() + 107L)));
            profiles.add(RoomProfile.closetStub(zone, new Random(zone.ordinal() + 211L)));
            if (zone == ZoneType.MECHANICUS_FORGE_CLOISTER || zone == ZoneType.MECHANICUS_RELIC_DUCT) {
                profiles.add(new RoomProfile("Atmospheric Condenser Cell", "Martian condenser room: damp coils, intake vanes, and emergency water recovery hardware", 44, Faction.MECHANIST_COLLEGIA, new String[]{"water bottle"}, new char[]{'Y'}));
                profiles.add(new RoomProfile("Emergency Assembler Niche", "Martian assembler room: tiny manipulators, feed chutes, and a machine that wants instructions", 48, Faction.MECHANIST_COLLEGIA, new String[]{"machine parts"}, new char[]{'J'}));
                profiles.add(new RoomProfile("Micro Laboratorium Closet", "Martian lab room: sample trays, sealed lenses, and ritualized diagnostic mess", 36, Faction.MECHANIST_COLLEGIA, new String[]{"sample vial"}, new char[]{'K'}));
            }
            for (RoomProfile profile : profiles) {
                if (profile == null || profile.name == null || profile.name.isBlank()) continue;
                RoomParityEntry entry = entryFor(profile, zone);
                entries.putIfAbsent(entry.roomName + "|" + entry.sourceZone, entry);
            }
        }
        return List.copyOf(entries.values());
    }

    static List<String> definitionAuditLines() {
        List<RoomParityEntry> rooms = roomEntries();
        List<BuildRecipe> blueprints = BuildRecipe.allBuildRecipes();
        int factionRooms = 0;
        int playerAcquirable = 0;
        int nonAcquirable = 0;
        int exceptionRooms = 0;
        int mappedBlueprints = 0;
        int factionUsableBlueprints = 0;
        for (RoomParityEntry entry : rooms) {
            if (!"None".equals(entry.owningFaction) && !"local independent".equals(entry.owningFaction)) factionRooms++;
            if (entry.playerAcquisitionStatus.contains("player path")) playerAcquirable++;
            if (entry.playerAcquisitionStatus.contains("non-acquirable")) nonAcquirable++;
            if (!entry.exceptionNote.isBlank()) exceptionRooms++;
            if (!entry.matchingBlueprint.equals("unmapped")) mappedBlueprints++;
        }
        for (BuildRecipe recipe : blueprints) {
            if (recipe != null && factionUseStatusFor(recipe).contains("faction usable")) factionUsableBlueprints++;
        }
        return List.of(
                "Room construction parity audit: owner=RoomConstructionParityAuthority, roomOwner=RoomProfile, blueprintOwner=BuildRecipe, acquisitionOwner=BlueprintAcquisitionPathAuthority, ordinaryUiRawIds=false.",
                "Room parity catalog audit: roomProfiles=" + rooms.size()
                        + ", factionRooms=" + factionRooms
                        + ", playerAcquirableRooms=" + playerAcquirable
                        + ", nonAcquirableRooms=" + nonAcquirable
                        + ", documentedExceptions=" + exceptionRooms
                        + ", mappedBlueprints=" + mappedBlueprints
                        + ", playerBlueprints=" + blueprints.size()
                        + ", factionUsableBlueprints=" + factionUsableBlueprints + ".",
                "Room parity acquisition audit: faction rooms expose an exact registered plan channel, an explicit unmapped gap with no invented channel, or a non-acquirable civic/transition/utility exception.",
                "Room parity faction-use audit: player blueprints are marked faction usable when they are public, faction-approved, facility-like, defensive, logistical, medical, laboratory, or market-facing.",
                "Room parity sample audit: " + sampleLine("Civic Wardens Precinct Lobby")
                        + " | " + sampleLine("Maintenance closet")
                        + " | " + sampleLine("Component Warehouse") + ".",
                "Room parity boundary: this audit does not place rooms, unlock blueprints, mutate ownership, spend reputation, create faction construction jobs, or apply heat and suspicion consequences.",
                "Guard: Milestone03RoomConstructionParityAuditSmoke checks faction-room player acquisition status, player-blueprint faction-use status, documented exceptions, sample mappings, future-owner boundaries, and raw-ID hiding."
        );
    }

    static String factionUseStatusFor(BuildRecipe recipe) {
        if (recipe == null) return "faction use unknown";
        String category = ConstructionCategoryAuthority.categoryFor(recipe);
        Faction faction = FactionInventoryStockAuthority.normalizeFaction(recipe.requiredFaction);
        String text = text(recipe.name, recipe.description, category);
        if (faction != Faction.NONE) return "faction usable through " + faction.label + " construction or sanctioned acquisition";
        if (containsAny(text, "storage", "warehouse", "barracks", "clinic", "medical", "laboratory", "bench", "forge",
                "shop", "counter", "logistics", "supply", "defense", "turret", "door", "wall", "barricade", "cot", "water")) {
            return "faction usable as public or facility blueprint";
        }
        return "faction usable after future faction construction owner confirms demand";
    }

    private static RoomParityEntry entryFor(RoomProfile profile, ZoneType zone) {
        Faction faction = FactionInventoryStockAuthority.normalizeFaction(profile.faction);
        String blueprint = matchingBlueprint(profile);
        String acquisition = acquisitionStatus(profile, faction, blueprint);
        String factionUse = faction == Faction.NONE
                ? "unclaimed or civic-neutral room; faction use depends on takeover or facility need"
                : "faction use declared for " + faction.label;
        String exception = exceptionNote(profile, zone, blueprint);
        return new RoomParityEntry(clean(profile.name), zone == null ? "Unknown Zone" : zone.label,
                faction == Faction.NONE ? "None" : faction.label, acquisition, factionUse, blueprint, exception);
    }

    private static String acquisitionStatus(RoomProfile profile, Faction faction, String blueprint) {
        String roomText = text(profile.name, profile.descriptor, profile.featureText);
        if (isNonAcquirableException(roomText)) {
            return "non-acquirable exception: civic transition, plaza, closet, or unsafe utility space";
        }
        if (!"unmapped".equals(blueprint)) {
            BuildRecipe recipe = recipeNamed(blueprint);
            BlueprintAcquisitionPathAuthority.AcquisitionPath path = BlueprintAcquisitionPathAuthority.pathFor(recipe);
            return (faction == Faction.NONE ? "player path" : "restricted player path") + " via " + path.representativeType()
                    + " as " + path.legalLabel();
        }
        if (faction != Faction.NONE) {
            return "restricted player path is unmapped; no exact plan or acquisition channel is registered";
        }
        return "player path is unmapped; no exact public construction plan is registered";
    }

    private static String exceptionNote(RoomProfile profile, ZoneType zone, String blueprint) {
        String roomText = text(profile.name, profile.descriptor, profile.featureText);
        if (isNonAcquirableException(roomText)) {
            return "documented exception: spatial connector, public transition, fallback closet, or unsafe utility space should not become a normal room blueprint.";
        }
        if ("unmapped".equals(blueprint)) {
            return "documented gap: future room-to-blueprint data owner should map this room or keep its exception explicit.";
        }
        return "";
    }

    private static String matchingBlueprint(RoomProfile profile) {
        if (profile != null && ExplicitRoomTypeRequirementAuthority.CRECHE_ID
                .equals(profile.declaredPurposeId)) {
            return "unmapped";
        }
        String text = text(profile.name, profile.descriptor, profile.featureText);
        Map<String, String> map = new LinkedHashMap<>();
        map.put("micro forge", "EMM Micro Forge");
        map.put("assembler", "EMM Micro Forge");
        map.put("forge", "EMM Micro Forge");
        map.put("atmospheric condenser", "EMM Atmospheric Condenser");
        map.put("condenser", "EMM Atmospheric Condenser");
        map.put("warehouse", "Storage Crate");
        map.put("storehouse", "Storage Crate");
        map.put("storage", "Storage Crate");
        map.put("dormitory", "Sleeping Cot");
        map.put("barracks", "Guard Barracks");
        map.put("clinic", "Backroom Medicae Stall");
        map.put("medicae", "Backroom Medicae Stall");
        map.put("kitchen", "Licensed Shop Counter");
        map.put("storefront", "Licensed Shop Counter");
        map.put("counter", "Licensed Shop Counter");
        map.put("market", "Licensed Shop Counter");
        map.put("workshop", "Scrap Workbench");
        map.put("maintenance", "Scrap Workbench");
        map.put("laboratory", "Crude chem bench");
        map.put("lab", "Crude chem bench");
        map.put("diagnostic", "Reagent preparation bench");
        map.put("security", "Security Sensor Mast");
        map.put("armory", "Security Sensor Mast");
        map.put("watch", "Watch Post");
        map.put("guard", "Watch Post");
        map.put("logistics", "Logistics Center");
        map.put("supply", "Supply Post");
        map.put("water", "Water Barrel");
        map.put("relay", "Shield Relay");
        map.put("shrine", "Base Decor Object");
        map.put("chapel", "Base Decor Object");
        map.put("laundry", "Base Decor Object");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (containsTerm(text, entry.getKey())
                    && recipeNamed(entry.getValue()) != null) return entry.getValue();
        }
        return "unmapped";
    }

    private static boolean containsTerm(String text, String term) {
        if (text == null || term == null || term.isBlank()) return false;
        int from = 0;
        while (from < text.length()) {
            int index = text.indexOf(term, from);
            if (index < 0) return false;
            int end = index + term.length();
            boolean leftBoundary = index == 0
                    || !Character.isLetterOrDigit(text.charAt(index - 1));
            boolean rightBoundary = end >= text.length()
                    || !Character.isLetterOrDigit(text.charAt(end));
            if (leftBoundary && rightBoundary) return true;
            from = index + 1;
        }
        return false;
    }

    private static String sampleLine(String roomName) {
        for (RoomParityEntry entry : roomEntries()) {
            if (entry.roomName.equalsIgnoreCase(roomName)) {
                return entry.roomName + " -> " + entry.playerAcquisitionStatus + " / " + entry.factionUseStatus;
            }
        }
        return roomName + " missing";
    }

    private static BuildRecipe recipeNamed(String name) {
        if (name == null || name.isBlank()) return null;
        for (BuildRecipe recipe : BuildRecipe.allBuildRecipes()) {
            if (recipe != null && recipe.name != null && recipe.name.equalsIgnoreCase(name)) return recipe;
        }
        return null;
    }

    private static boolean isNonAcquirableException(String text) {
        return containsAny(text, "plaza", "transition", "closet fallback", "dead-end utility closet",
                "blind crawlspace", "false maintenance niche", "collapsed side cupboard", "one-person storage stub",
                "sealed service cubby", "single-door stub", "unoccupied neutral room");
    }

    private static boolean isSewer(ZoneType zone) {
        return zone == ZoneType.SEWER_CONDUIT || zone == ZoneType.MUTANT_SEWER_CAMP || zone == ZoneType.CULTIST_SEWER_CAMP;
    }

    private static boolean containsAny(String text, String... needles) {
        if (text == null) return false;
        for (String needle : needles) if (needle != null && text.contains(needle)) return true;
        return false;
    }

    private static String text(String... values) {
        StringBuilder out = new StringBuilder();
        if (values != null) for (String value : values) if (value != null) out.append(value).append(' ');
        return out.toString().toLowerCase(Locale.ROOT);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? "Unknown room" : value.trim();
    }
}
