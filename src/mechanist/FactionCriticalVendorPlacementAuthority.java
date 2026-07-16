package mechanist;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/** Places and stocks physical faction vendors according to controlled infrastructure. */
final class FactionCriticalVendorPlacementAuthority {
    enum Category {
        PROVISIONS("provisions", "Provision Trader", "food, basic supplies, and drinkable water"),
        ARMORY("armory", "Armory Trader", "weapons, ammunition, armor, and restricted security supply"),
        MEDICAL("medical", "Medical Trader", "medicine, dressings, and treatment supply"),
        INDUSTRIAL("industrial-blueprints", "Industrial Blueprint Trader", "tools, construction, room blueprints, machine blueprints, and garage supply"),
        ANIMAL("animal-care", "Animal Supply Trader", "animal feed, pet supplies, and veterinary care"),
        LUXURY("luxury", "Luxury Broker Trader", "luxury goods, private medicine access, and estate imports"),
        BLACK_MARKET("black-market", "Black-Market Trader", "contraband weapons, ammunition, narcotics, stolen tools, and restricted goods");

        final String id;
        final String role;
        final String coverage;
        Category(String id, String role, String coverage) { this.id = id; this.role = role; this.coverage = coverage; }
    }

    record Placement(Faction faction, Category category, int roomId, String facilityName, String vendorId) {}
    record Result(ArrayList<Placement> placements, int consideredFactions) {
        int vendorsPlaced() { return placements.size(); }
        String summary() { return "criticalVendors placed=" + vendorsPlaced() + " factions=" + consideredFactions + " categories=" + categorySummary(placements); }
    }

    private FactionCriticalVendorPlacementAuthority() {}

    static Result apply(World world, Random rng) {
        ArrayList<Placement> placements = new ArrayList<>();
        if (world == null || world.rooms == null || world.rooms.size() <= 1) return new Result(placements, 0);
        Random r = rng == null ? new Random(world.seed ^ 0x161A11L) : rng;
        LinkedHashMap<Faction, Integer> roomCounts = controlledRoomCounts(world);
        ArrayList<Map.Entry<Faction, Integer>> candidates = new ArrayList<>(roomCounts.entrySet());
        candidates.sort(Map.Entry.<Faction, Integer>comparingByValue(Comparator.reverseOrder()).thenComparing(e -> e.getKey().name()));
        int considered = 0;
        for (Map.Entry<Faction, Integer> entry : candidates) {
            if (considered >= 3) break;
            if (entry.getValue() < 2 && candidates.size() > 1) continue;
            Faction faction = entry.getKey();
            considered++;
            LinkedHashSet<Category> categories = categoriesFor(world, faction);
            LinkedHashSet<Integer> usedRooms = new LinkedHashSet<>();
            for (Category category : categories) {
                if (hasVendor(world, faction, category)) continue;
                int roomId = bestRoom(world, faction, category, usedRooms);
                if (roomId < 0) continue;
                Point point = world.randomObjectPointInRoom(world.rooms.get(roomId));
                if (point == null) continue;
                RoomProfile profile = world.roomProfiles.get(roomId);
                NpcEntity vendor = NpcEntity.create(faction, world.zoneType, point.x, point.y, r);
                vendor.role = category.role;
                vendor.state = "Trade";
                vendor.symbol = 'T';
                vendor.name = vendorName(faction, category, vendor.name);
                vendor.id = "FACTION-VENDOR-" + faction.name() + "-" + category.id.toUpperCase(Locale.ROOT).replace('-', '_')
                        + "-" + Math.abs(java.util.Objects.hash(world.seed, roomId, point.x, point.y));
                PersonnelPopulationApi.attachExistingNpcToRoomLedger(vendor, world, roomId, r);
                world.npcs.add(vendor);
                char under = world.tiles[point.x][point.y];
                MapObjectState market = MapObjectState.factionMarket(point.x, point.y, world.zoneType, faction,
                        category.id, category.coverage, roomId, profile == null ? "controlled room" : profile.name, under);
                world.tiles[point.x][point.y] = market.glyph;
                world.mapObjects.add(market);
                if (profile != null) profile.featureText = append(profile.featureText,
                        "Staffed faction market facility: " + category.coverage + "; vendor=" + vendor.name + ".");
                usedRooms.add(roomId);
                placements.add(new Placement(faction, category, roomId, profile == null ? "controlled room" : profile.name, vendor.id));
            }
        }
        return new Result(placements, considered);
    }

    static void applyVendorStock(TraderSession trader, NpcEntity npc) {
        if (trader == null || npc == null) return;
        Category category = categoryForRole(npc.role);
        if (category == null) return;
        Faction normalized = FactionInventoryStockAuthority.normalizeFaction(npc.faction);
        trader.marketFaction = npc.faction == null ? Faction.NONE : npc.faction;
        trader.marketCategory = category.id;
        switch (category) {
            case PROVISIONS -> add(trader, "Emergency rations", "Water bottle", "Construction supplies");
            case ARMORY -> {
                if (normalized == Faction.IMPERIAL_GUARD) add(trader, "Light Rifle", "Las charge pack", "Guard flak vest", "Field dressings");
                else if (normalized == Faction.CIVIC_WARDENS) add(trader, "Webber", "Civic Wardens suppression shells", "Civic Wardens riot visor", "Field dressings");
                else add(trader, "Pipe shotgun", "Shot shell handful", "Padded coat", "Bandage roll");
            }
            case MEDICAL -> add(trader, "Bandage roll", "Antiseptic vial", "Medkit", "Field dressings");
            case INDUSTRIAL -> add(trader, "Tool bundle", "Construction supplies", "Room blueprint folio", "Machine blueprint slate", "Vehicle service component crate");
            case ANIMAL -> add(trader, "Animal feed sack", "Pet care bundle", "Veterinary care kit");
            case LUXURY -> add(trader, "Noble preserved delicacy", "Noble fur-lined coat", "Noble signet wax kit", "Pearl Obscura");
            case BLACK_MARKET -> add(trader, "Pipe shotgun", "Shot shell handful", "Street Stimm", "Grin Powder",
                    "Night Milk", "Lockpicks", "Tripwire mine");
        }
        ConstructionBlueprintOwnershipAuthority.applyVendorStock(trader, normalized, category);
        String line = "Vendor remit: " + category.coverage + "; physical faction facility required.";
        trader.supplyChainSummary = append(trader.supplyChainSummary, line);
    }

    static Category categoryForRole(String role) {
        if (role == null) return null;
        for (Category category : Category.values()) if (role.equalsIgnoreCase(category.role)) return category;
        return null;
    }

    private static LinkedHashSet<Category> categoriesFor(World world, Faction faction) {
        LinkedHashSet<Category> out = new LinkedHashSet<>();
        out.add(Category.PROVISIONS);
        Faction normalized = FactionInventoryStockAuthority.normalizeFaction(faction);
        if (normalized == Faction.IMPERIAL_GUARD || normalized == Faction.CIVIC_WARDENS) {
            out.add(Category.ARMORY); out.add(Category.MEDICAL);
        } else if (normalized == Faction.MECHANIST_COLLEGIA) {
            out.add(Category.INDUSTRIAL); out.add(Category.MEDICAL);
        } else if (normalized == Faction.NOBLE) {
            out.add(Category.LUXURY); out.add(Category.MEDICAL);
        } else if (normalized == Faction.BANDIT || normalized == Faction.CULTIST || normalized == Faction.HERETIC) {
            out.add(Category.BLACK_MARKET);
        } else {
            out.add(Category.INDUSTRIAL);
            if (hasRoom(world, faction, "clinic", "medical", "medicae", "aid station", "care room")) out.add(Category.MEDICAL);
        }
        if (hasRoom(world, faction, FactionFacilityPlacementAuthority.AGRICULTURE_TAG, "animal", "pet", "kennel", "farm", "garden", "veterinary")) out.add(Category.ANIMAL);
        return out;
    }

    private static LinkedHashMap<Faction, Integer> controlledRoomCounts(World world) {
        LinkedHashMap<Faction, Integer> counts = new LinkedHashMap<>();
        for (int i = 1; i < world.roomFactions.size(); i++) {
            Faction faction = world.roomFactions.get(i);
            if (faction == null || faction == Faction.NONE) continue;
            RoomProfile profile = i < world.roomProfiles.size() ? world.roomProfiles.get(i) : null;
            if (world.isFactionRepBarProfile(profile)) continue;
            counts.put(faction, counts.getOrDefault(faction, 0) + 1);
        }
        return counts;
    }

    private static int bestRoom(World world, Faction faction, Category category, LinkedHashSet<Integer> usedRooms) {
        String[] keywords = switch (category) {
            case PROVISIONS -> new String[]{"cafeteria", "kitchen", "food store", "mess", "galley", "canteen", "storefront", "counter"};
            case ARMORY -> new String[]{"armory", "munition", "security", "quartermaster", "checkpoint", "warehouse"};
            case MEDICAL -> new String[]{"clinic", "medical", "medicae", "aid station", "care room"};
            case INDUSTRIAL -> new String[]{"workshop", "machinery", "warehouse", "component", "repair", "storefront", "counter", "garage"};
            case ANIMAL -> new String[]{"animal", "pet", "kennel", "farm", "garden", "veterinary", "feed"};
            case LUXURY -> new String[]{"salon", "luxury", "private storefront", "estate", "noble", "warehouse"};
            case BLACK_MARKET -> new String[]{"fence", "contraband", "gang", "chem", "whisper", "stolen", "storefront"};
        };
        int best = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 1; i < world.roomProfiles.size() && i < world.roomFactions.size(); i++) {
            if (world.roomFactions.get(i) != faction) continue;
            RoomProfile profile = world.roomProfiles.get(i);
            if (world.isFactionRepBarProfile(profile)) continue;
            String text = roomText(profile).toLowerCase(Locale.ROOT);
            int score = usedRooms.contains(i) ? -8 : 4;
            for (String keyword : keywords) if (text.contains(keyword)) score += 12;
            if (i < world.roomSpecials.size() && world.roomSpecials.get(i)) score -= 5;
            if (score > bestScore) { bestScore = score; best = i; }
        }
        return best;
    }

    private static boolean hasVendor(World world, Faction faction, Category category) {
        for (NpcEntity npc : world.npcs) if (npc != null && npc.faction == faction && category == categoryForRole(npc.role)) return true;
        return false;
    }

    private static boolean hasRoom(World world, Faction faction, String... terms) {
        for (int i = 1; i < world.roomProfiles.size() && i < world.roomFactions.size(); i++) {
            if (world.roomFactions.get(i) != faction) continue;
            String text = roomText(world.roomProfiles.get(i)).toLowerCase(Locale.ROOT);
            for (String term : terms) if (text.contains(term.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static void add(TraderSession trader, String... items) {
        for (String item : items) {
            ItemDef definition = ItemCatalog.get(item);
            if (definition == null) continue;
            boolean exists = false;
            for (TradeOffer offer : trader.offers) if (offer != null && ItemQuality.namesMatch(offer.name, item)) { exists = true; break; }
            if (!exists) trader.offers.add(new TradeOffer(item, definition.category, definition.basePrice,
                    "stocked by this physical faction vendor's category remit."));
        }
    }

    private static String vendorName(Faction faction, Category category, String generatedName) {
        String surname = generatedName == null || generatedName.isBlank() ? "Factor" : generatedName;
        int space = surname.lastIndexOf(' ');
        if (space >= 0 && space + 1 < surname.length()) surname = surname.substring(space + 1);
        return switch (category) {
            case PROVISIONS -> faction.label + " Provisioner " + surname;
            case ARMORY -> faction.label + " Quartermaster " + surname;
            case MEDICAL -> faction.label + " Dispensary Factor " + surname;
            case INDUSTRIAL -> faction.label + " Works Factor " + surname;
            case ANIMAL -> faction.label + " Animal-Care Vendor " + surname;
            case LUXURY -> faction.label + " Estate Broker " + surname;
            case BLACK_MARKET -> faction.label + " Back-Room Dealer " + surname;
        };
    }

    private static String categorySummary(ArrayList<Placement> placements) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (Placement placement : placements) ids.add(placement.category.id);
        return ids.isEmpty() ? "none" : String.join(",", ids);
    }

    private static String roomText(RoomProfile profile) { return profile == null ? "" : safe(profile.name) + " " + safe(profile.descriptor) + " " + safe(profile.featureText); }
    private static String append(String current, String line) { return current == null || current.isBlank() ? line : current.contains(line) ? current : current + " " + line; }
    private static String safe(String value) { return value == null ? "" : value; }
}
