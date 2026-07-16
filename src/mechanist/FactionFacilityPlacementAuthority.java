package mechanist;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

/** Promotes controlled rooms into player-relevant faction economy facilities. */
final class FactionFacilityPlacementAuthority {
    static final String ILLICIT_TAG = "phase16-illicit-narcotics-facility";
    static final String NOBLE_TAG = "phase16-noble-draught-vault";
    static final String AGRICULTURE_TAG = "phase16-agriculture-animal-facility";

    record Result(int illicitFacilities, int nobleVaults, int agricultureFacilities) {
        int total() { return illicitFacilities + nobleVaults + agricultureFacilities; }
        String summary() {
            return "factionFacilities illicit=" + illicitFacilities + " nobleVaults=" + nobleVaults
                    + " agricultureAnimal=" + agricultureFacilities
                    + " rule=controlled rooms become visible economy facilities before room fixtures";
        }
    }

    private FactionFacilityPlacementAuthority() {}

    static Result apply(World world, Random rng) {
        if (world == null || world.rooms == null || world.rooms.size() <= 1) return new Result(0, 0, 0);
        Random r = rng == null ? new Random(world.seed ^ 0x161234L) : rng;
        int illicit = promoteIllicitFacility(world, r);
        int noble = promoteNobleVault(world, r);
        int agriculture = promoteAgricultureFacility(world, r);
        return new Result(illicit, noble, agriculture);
    }

    static int ensureFacilityFixtures(World world, Random rng) {
        if (world == null) return 0;
        Random r = rng == null ? new Random(world.seed ^ 0x1612F1L) : rng;
        int placed = 0;
        for (int roomId = 1; roomId < world.roomProfiles.size() && roomId < world.rooms.size(); roomId++) {
            RoomProfile profile = world.roomProfiles.get(roomId);
            if (!isPromoted(profile) || hasSemanticFixture(world, roomId)) continue;
            Rectangle room = world.rooms.get(roomId);
            Point point = RoomFixtureInteractionAuthority.fixturePoint(world, room, r);
            if (point == null) continue;
            String type = RoomFixtureInteractionAuthority.fixtureTypeFor(world, profile, roomId, r);
            if (type == null) continue;
            char under = world.tiles[point.x][point.y];
            MapObjectState fixture = RoomFixtureInteractionAuthority.roomFixture(point.x, point.y, type,
                    RoomFixtureInteractionAuthority.labelFor(type, profile, world.zoneType),
                    RoomFixtureInteractionAuthority.stockFor(type, profile, world, roomId, under),
                    RoomFixtureInteractionAuthority.glyphFor(type));
            world.tiles[point.x][point.y] = fixture.glyph;
            world.mapObjects.add(fixture);
            placed++;
        }
        return placed;
    }

    static int staffPromotedFacilities(World world, Random rng) {
        if (world == null) return 0;
        Random r = rng == null ? new Random(world.seed ^ 0x16125AL) : rng;
        int staffed = 0;
        for (int roomId = 1; roomId < world.roomProfiles.size() && roomId < world.rooms.size(); roomId++) {
            RoomProfile profile = world.roomProfiles.get(roomId);
            String tag = promotedTag(profile);
            if (tag.isEmpty()) continue;
            String role = ILLICIT_TAG.equals(tag) ? "Chem Cook"
                    : NOBLE_TAG.equals(tag) ? "Vault Guard" : "Animal Handler";
            if (hasRoleInRoom(world, roomId, role)) continue;
            Point point = world.randomOpenPointInRoom(world.rooms.get(roomId));
            if (point == null) continue;
            Faction faction = roomId < world.roomFactions.size() ? world.roomFactions.get(roomId) : Faction.NONE;
            NpcEntity npc = NpcEntity.create(faction, world.zoneType, point.x, point.y, r);
            npc.role = role;
            npc.state = NOBLE_TAG.equals(tag) ? "Guard" : ILLICIT_TAG.equals(tag) ? "Working Batch" : "Animal Care";
            npc.symbol = NOBLE_TAG.equals(tag) ? 'A' : ILLICIT_TAG.equals(tag) ? 'k' : 'h';
            PersonnelPopulationApi.attachExistingNpcToRoomLedger(npc, world, roomId, r);
            world.npcs.add(npc);
            staffed++;
        }
        return staffed;
    }

    private static int promoteIllicitFacility(World world, Random r) {
        Faction faction = firstFaction(world, FactionFacilityPlacementAuthority::isIllicit);
        if (faction == null) return 0;
        int roomId = findExistingTaggedRoom(world, faction, ILLICIT_TAG);
        if (roomId >= 0) return 0;
        roomId = bestRoom(world, faction, "chem", "drug", "laboratory", "machinery", "workshop", "kitchen", "warehouse");
        if (roomId < 0) return 0;
        RoomProfile old = world.roomProfiles.get(roomId);
        String former = old == null || old.name == null ? "controlled back room" : old.name;
        RoomProfile promoted = new RoomProfile(faction.label + " Illicit Chem Kitchen",
                "a concealed narcotics workshop promoted from " + former
                        + ": crude chemical benches, drying racks, dose packets, a contaminated waste trap, guarded storage, and a dealer-side distribution hatch",
                62, faction,
                new String[]{"Street Stimm", "Grin Powder", "Night Milk", "Chemical reagent rack"},
                new char[]{'L', 'b', 'q', 'N'});
        promoted.featureText = ILLICIT_TAG + "; hidden drug lab; back-room chemical workshop; guarded stash and distribution room; contamination risk; black-market production.";
        replaceProfile(world, roomId, promoted, true);
        String facilityId = facilityId(world, roomId, "illicit-chem");
        appendUnique(world, true, facilityId,
                "F16I: illicit narcotics production and packaging :: by " + faction.label
                        + " :: room=" + promoted.name
                        + " :: output=street stimulants, sedatives, narcotics, and black-market chemical stock"
                        + " :: people=chem cooks, brokers, lookouts, and smugglers :: concealed inside faction-controlled territory");
        appendUnique(world, false, facilityId,
                "P16I: facility=" + facilityId + " :: purpose=illicit narcotics chem kitchen :: controller=" + faction.label
                        + " :: focus=bulk illicit stimulants and sedatives for internal issue and black-market sale"
                        + " :: cadence=nightly concealed batch-cycle :: batches=3 :: retained=2"
                        + " :: samples=Street Stimm, Grin Powder, Night Milk, Grey Mercy :: generated local illicit production");
        return 1;
    }

    private static int promoteNobleVault(World world, Random r) {
        Faction faction = firstFaction(world, FactionFacilityPlacementAuthority::isNoble);
        if (faction == null) return 0;
        int roomId = findExistingTaggedRoom(world, faction, NOBLE_TAG);
        if (roomId >= 0) return 0;
        roomId = bestRoom(world, faction, "vault", "warehouse", "security", "storehouse", "trophy", "private", "salon");
        if (roomId < 0) return 0;
        RoomProfile old = world.roomProfiles.get(roomId);
        String former = old == null || old.name == null ? "guarded estate room" : old.name;
        RoomProfile promoted = new RoomProfile(faction.label + " Luxury and Draught Vault",
                "a locked house vault promoted from " + former
                        + ": sealed off-world substance cabinets, luxury crates, a private medicine locker, ciphered ownership records, and armed estate security",
                72, faction,
                new String[]{"Noble preserved delicacy", "Noble fur-lined coat", "Noble signet wax kit", "High Amasec"},
                new char[]{'X', 'Q', 'A', 'q'});
        promoted.featureText = NOBLE_TAG + "; locked noble vault; sealed off-world substance cabinet; guarded luxury store; draught custody remains withheld from ordinary sale.";
        replaceProfile(world, roomId, promoted, true);
        String facilityId = facilityId(world, roomId, "noble-vault");
        appendUnique(world, true, facilityId,
                "F16N: protected noble luxury and draught custody :: by " + faction.label
                        + " :: room=" + promoted.name
                        + " :: output=guarded luxury storage, private medicine, and sealed off-world draught custody"
                        + " :: people=estate factor, house physician, vault guards, and bonded servants :: access requires house authority");
        appendUnique(world, false, facilityId,
                "P16N: facility=" + facilityId + " :: purpose=protected estate luxury intake :: controller=" + faction.label
                        + " :: focus=imported luxury goods and private medical stores; draughts remain custody-only"
                        + " :: cadence=guarded receiving-cycle :: batches=2 :: retained=2"
                        + " :: samples=Noble preserved delicacy, Noble fur-lined coat, Noble signet wax kit, High Amasec"
                        + " :: ordinary output excludes protected draught items");
        return 1;
    }

    private static int promoteAgricultureFacility(World world, Random r) {
        if (!agricultureAppropriate(world.zoneType)) return 0;
        Faction faction = firstFaction(world, FactionFacilityPlacementAuthority::supportsAgriculture);
        if (faction == null) return 0;
        int roomId = findExistingTaggedRoom(world, faction, AGRICULTURE_TAG);
        if (roomId >= 0) return 0;
        roomId = bestRoom(world, faction, "farm", "garden", "hydroponic", "fungus", "food", "warehouse", "machinery", "daycare");
        if (roomId < 0) return 0;
        String style = lowerAgriculture(world.zoneType) ? "Sump Mushroom Farm and Animal-Care Room"
                : isNoble(faction) ? faction.label + " Bio-Garden and Kennel"
                : faction.label + " Cooperative Hydroponic Farm and Animal-Care Room";
        RoomProfile promoted = new RoomProfile(style,
                "an economy-supporting farm facility with grow beds, feed storage, a water station, cleaning drain, compact animal pen, handler bench, and veterinary supplies",
                58, faction,
                new String[]{"Animal feed sack", "Pet care bundle", "Veterinary care kit", "Fungus starter mat"},
                new char[]{'m', 'Y', 'b', 'u'});
        promoted.featureText = AGRICULTURE_TAG + "; farm, garden, hydroponics, mushroom beds, animal pen, kennel, feed storage, water station, cleaning station, and animal-care room.";
        replaceProfile(world, roomId, promoted, false);
        String facilityId = facilityId(world, roomId, "agriculture-animal");
        appendUnique(world, true, facilityId,
                "F16A: agriculture, animal care, and local market support :: by " + faction.label
                        + " :: room=" + promoted.name
                        + " :: output=food crops, fungus, animal feed, pet supplies, and veterinary stock"
                        + " :: people=farm workers, animal handlers, cleaners, and market vendors :: local water and feed capacity bound");
        appendUnique(world, false, facilityId,
                "P16A: facility=" + facilityId + " :: purpose=agricultural and animal-care production :: controller=" + faction.label
                        + " :: focus=local food, feed, pet care, and veterinary supply"
                        + " :: cadence=daily care-and-harvest-cycle :: batches=3 :: retained=2"
                        + " :: samples=Animal feed sack, Farm animal product crate, Pet care bundle, Veterinary care kit"
                        + " :: output depends on handlers, water, feed, and disease control");
        return 1;
    }

    private static int bestRoom(World world, Faction faction, String... keywords) {
        int best = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 1; i < world.roomProfiles.size() && i < world.roomFactions.size(); i++) {
            if (world.roomFactions.get(i) != faction) continue;
            RoomProfile profile = world.roomProfiles.get(i);
            if (isPromoted(profile) || world.isFactionRepBarProfile(profile)) continue;
            String text = roomText(profile).toLowerCase(Locale.ROOT);
            int score = 0;
            for (String keyword : keywords) if (text.contains(keyword)) score += 10;
            if (i < world.roomSpecials.size() && world.roomSpecials.get(i)) score -= 6;
            score -= i / 8;
            if (score > bestScore) { bestScore = score; best = i; }
        }
        return best;
    }

    private static Faction firstFaction(World world, java.util.function.Predicate<Faction> predicate) {
        ArrayList<Faction> factions = new ArrayList<>();
        for (Faction faction : world.roomFactions) {
            if (faction == null || faction == Faction.NONE || !predicate.test(faction) || factions.contains(faction)) continue;
            factions.add(faction);
        }
        Faction best = null;
        int bestRooms = 0;
        for (Faction faction : factions) {
            int rooms = 0;
            for (Faction owner : world.roomFactions) if (owner == faction) rooms++;
            if (rooms > bestRooms) { bestRooms = rooms; best = faction; }
        }
        return best;
    }

    private static void replaceProfile(World world, int roomId, RoomProfile profile, boolean special) {
        world.roomProfiles.set(roomId, profile);
        world.roomFactions.set(roomId, profile.faction);
        if (roomId < world.roomSpecials.size()) world.roomSpecials.set(roomId, special);
    }

    private static void appendUnique(World world, boolean facilityLedger, String key, String line) {
        String current = facilityLedger ? world.zoneFacilityHistory : world.zoneProductionHistory;
        if (current != null && current.contains(key)) return;
        String next = current == null || current.isBlank() ? line : current + ";;" + line;
        if (facilityLedger) world.zoneFacilityHistory = next; else world.zoneProductionHistory = next;
    }

    private static String facilityId(World world, int roomId, String family) {
        return "facility.phase16." + family + "." + Math.abs(java.util.Objects.hash(world.seed, roomId, family));
    }

    private static int findExistingTaggedRoom(World world, Faction faction, String tag) {
        for (int i = 1; i < world.roomProfiles.size() && i < world.roomFactions.size(); i++) {
            if (world.roomFactions.get(i) == faction && roomText(world.roomProfiles.get(i)).contains(tag)) return i;
        }
        return -1;
    }

    private static boolean hasSemanticFixture(World world, int roomId) {
        for (MapObjectState object : world.mapObjects) {
            if (object != null && world.inBounds(object.x, object.y) && world.roomIds[object.x][object.y] == roomId
                    && FixtureInteractionRegistry.definitionFor(object.type) != null) return true;
        }
        return false;
    }

    private static boolean hasRoleInRoom(World world, int roomId, String role) {
        for (NpcEntity npc : world.npcs) {
            if (npc != null && role.equals(npc.role) && world.inBounds(npc.x, npc.y) && world.roomIds[npc.x][npc.y] == roomId) return true;
        }
        return false;
    }

    private static String promotedTag(RoomProfile profile) {
        String text = roomText(profile);
        if (text.contains(ILLICIT_TAG)) return ILLICIT_TAG;
        if (text.contains(NOBLE_TAG)) return NOBLE_TAG;
        if (text.contains(AGRICULTURE_TAG)) return AGRICULTURE_TAG;
        return "";
    }

    static boolean isPromoted(RoomProfile profile) { return !promotedTag(profile).isEmpty(); }
    static boolean isIllicit(Faction faction) { Faction f = FactionInventoryStockAuthority.normalizeFaction(faction); return f == Faction.BANDIT || f == Faction.CULTIST || f == Faction.HERETIC; }
    static boolean isNoble(Faction faction) { return FactionInventoryStockAuthority.normalizeFaction(faction) == Faction.NOBLE; }
    static boolean supportsAgriculture(Faction faction) { Faction f = FactionInventoryStockAuthority.normalizeFaction(faction); return f == Faction.HIVER || f == Faction.SCAVENGER || f == Faction.NOBLE || f == Faction.MUTANT; }
    private static boolean agricultureAppropriate(ZoneType zone) { return zone == ZoneType.HAB_STACK || zone == ZoneType.NEUTRAL_CIVILIAN_FLOOR || zone == ZoneType.SUMP_MARKET || zone == ZoneType.NOBLE_SERVICE_SPINE || zone == ZoneType.SECTOR_GOVERNORS_MANSION || zone == ZoneType.MUTANT_WARRENS || zone == ZoneType.MUTANT_SEWER_CAMP || zone == ZoneType.SEWER_CONDUIT || zone == ZoneType.TRASH_WARREN; }
    private static boolean lowerAgriculture(ZoneType zone) { return zone == ZoneType.SUMP_MARKET || zone == ZoneType.MUTANT_WARRENS || zone == ZoneType.MUTANT_SEWER_CAMP || zone == ZoneType.SEWER_CONDUIT || zone == ZoneType.TRASH_WARREN; }
    private static String roomText(RoomProfile profile) { return profile == null ? "" : safe(profile.name) + " " + safe(profile.descriptor) + " " + safe(profile.featureText); }
    private static String safe(String value) { return value == null ? "" : value; }
}
