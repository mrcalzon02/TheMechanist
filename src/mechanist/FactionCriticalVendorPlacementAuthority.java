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

    record FacilityActivation(boolean handled, boolean opened, boolean existing,
                              String blocker, String message, int roomId, int workers,
                              NpcEntity vendor, MapObjectState market) {
        static FacilityActivation notHandled(String message) {
            return new FacilityActivation(false, false, false, "not-handled", safe(message),
                    -1, 0, null, null);
        }

        static FacilityActivation blocked(String blocker, String message, int roomId, int workers) {
            return new FacilityActivation(true, false, false, safe(blocker), safe(message),
                    roomId, Math.max(0, workers), null, null);
        }

        String vendorId() { return vendor == null || vendor.id == null ? "" : vendor.id; }
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

    /**
     * Opens or attaches a physical vendor only after its exact faction facility
     * has completed and the facility room still has assigned same-family staff.
     * The existing room roster is conserved; the vendor actor represents one of
     * those assigned workers rather than increasing the population ledger.
     */
    static FacilityActivation activateCompletedFacility(GamePanel game, NpcFactionSite site,
                                                         BaseObject facility) {
        Category category = categoryForFacility(facility);
        if (category == null) {
            return FacilityActivation.notHandled("The completed faction asset has no vendor service category.");
        }
        if (game == null || game.world == null) {
            return FacilityActivation.blocked("no-world",
                    "Vendor opening requires the completed facility's loaded world.", -1, 0);
        }
        if (site == null) {
            return FacilityActivation.blocked("no-site",
                    "Vendor opening requires the faction site linked to the completed facility.", -1, 0);
        }
        if (facility == null || facility.underConstruction
                || !FactionPhysicalConstructionAuthority.isFactionManaged(facility)
                || !FactionPhysicalConstructionAuthority.belongsToSite(facility, site)) {
            return FacilityActivation.blocked("facility-not-complete",
                    "Vendor opening waits for a completed faction-managed facility with a valid site receipt.",
                    facility == null ? -1 : game.world.roomIdAt(facility.x, facility.y), 0);
        }
        if (!sameLocation(site, game.world)) {
            return FacilityActivation.blocked("site-not-local",
                    safe(site.name) + " is not in the loaded zone, so its vendor cannot open here.", -1, 0);
        }

        int roomId = game.world.roomIdAt(facility.x, facility.y);
        if (roomId < 0 || !FactionIdentityAuthority.sameFamily(
                game.world.roomFaction(roomId), site.faction)) {
            return FacilityActivation.blocked("facility-room-control",
                    "The completed facility is not inside a same-family controlled room.", roomId, 0);
        }
        int workers = assignedFacilityWorkers(game.world, roomId, site);
        if (workers <= 0) {
            return FacilityActivation.blocked("facility-unstaffed",
                    RoomOwnershipAuthority.roomName(game.world, roomId)
                            + " has no assigned same-family worker available to operate a vendor.",
                    roomId, 0);
        }

        NpcEntity existingVendor = facilityVendor(game.world, site, category, roomId);
        if (existingVendor != null) {
            MapObjectState market = ensureFacilityMarket(game.world, site, facility, category,
                    roomId, workers, existingVendor.x, existingVendor.y);
            String message = market == null
                    ? existingVendor.name + " remains the staffed " + category.role
                            + " for the completed facility; its existing position is occupied by another fixture."
                    : existingVendor.name + " now operates the completed facility's "
                            + category.role + " counter with " + workers + " assigned worker(s).";
            return new FacilityActivation(true, false, true, "", message,
                    roomId, workers, existingVendor, market);
        }

        Point point = firstLegalVendorPoint(game, roomId);
        if (point == null) {
            return FacilityActivation.blocked("no-vendor-position",
                    "The completed facility has staff, but no legal open counter position remains in "
                            + RoomOwnershipAuthority.roomName(game.world, roomId) + ".",
                    roomId, workers);
        }

        Random r = new Random(game.world.seed ^ java.util.Objects.hash(
                siteToken(site), facilityToken(facility), category.id, roomId));
        NpcEntity vendor = NpcEntity.create(site.faction, game.world.zoneType, point.x, point.y, r);
        vendor.role = category.role;
        vendor.state = "Trade";
        vendor.symbol = 'T';
        vendor.name = vendorName(site.faction, category, vendor.name);
        vendor.id = facilityVendorId(site, category);
        MapObjectState market = ensureFacilityMarket(game.world, site, facility, category,
                roomId, workers, point.x, point.y);
        if (market == null) {
            return FacilityActivation.blocked("market-fixture-blocked",
                    "The completed facility's counter position became occupied before opening.",
                    roomId, workers);
        }
        game.world.npcs.add(vendor);

        RoomProfile profile = roomId < game.world.roomProfiles.size()
                ? game.world.roomProfile(roomId) : null;
        if (profile != null) {
            profile.featureText = append(profile.featureText,
                    "Operational faction vendor: " + vendor.name + " staffs " + category.coverage
                            + " from the completed facility with " + workers + " assigned worker(s).");
        }
        return new FacilityActivation(true, true, false, "",
                vendor.name + " opened a staffed " + category.role + " counter in "
                        + RoomOwnershipAuthority.roomName(game.world, roomId)
                        + "; the room roster remains " + workers + " assigned worker(s).",
                roomId, workers, vendor, market);
    }

    /** Reconciles completed faction facilities after load or an older completion. */
    static int reconcileCompletedFacilities(GamePanel game) {
        if (game == null || game.world == null || game.baseObjects == null
                || game.npcFactionSites == null) return 0;
        int opened = 0;
        for (BaseObject facility : new ArrayList<>(game.baseObjects)) {
            if (facility == null || facility.underConstruction
                    || !FactionPhysicalConstructionAuthority.isFactionManaged(facility)) continue;
            for (NpcFactionSite site : game.npcFactionSites) {
                if (site == null || !FactionPhysicalConstructionAuthority.belongsToSite(facility, site)) continue;
                FacilityActivation activation = activateCompletedFacility(game, site, facility);
                if (activation.opened()) opened++;
                break;
            }
        }
        return opened;
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
        String line = isFacilityVendor(npc)
                ? "Vendor remit: " + category.coverage + "; opened from a completed staffed faction facility."
                : "Vendor remit: " + category.coverage + "; physical faction facility required.";
        trader.supplyChainSummary = append(trader.supplyChainSummary, line);
    }

    static Category categoryForRole(String role) {
        if (role == null) return null;
        for (Category category : Category.values()) if (role.equalsIgnoreCase(category.role)) return category;
        return null;
    }

    static boolean isFacilityVendor(NpcEntity npc) {
        return npc != null && npc.id != null && npc.id.startsWith("FACTION-FACILITY-VENDOR-");
    }

    static String siteToken(NpcFactionSite site) {
        if (site == null) return "SITE-UNLINKED";
        return "SITE-" + Integer.toUnsignedString(java.util.Objects.hash(
                safe(site.name), site.faction, site.sectorX, site.sectorY,
                site.zoneX, site.zoneY, site.floor), 36).toUpperCase(Locale.ROOT);
    }

    private static Category categoryForFacility(BaseObject facility) {
        if (facility == null || facility.underConstruction) return null;
        char symbol = facility.symbol == '?' && facility.finalSymbol != 0
                ? facility.finalSymbol : facility.symbol;
        return switch (symbol) {
            case 'f', 'w', 'l', 's' -> Category.INDUSTRIAL;
            case 'M' -> Category.MEDICAL;
            case 'u', 'e', 'B' -> Category.PROVISIONS;
            default -> null;
        };
    }

    private static int assignedFacilityWorkers(World world, int roomId, NpcFactionSite site) {
        if (world == null || site == null || roomId < 0) return 0;
        if (world.roomPopulationLedgers == null || world.roomPopulationLedgers.isEmpty()) {
            return Math.max(0, FactionSiteWorkforceAuthority.evaluate(site, world).effectiveWorkers());
        }
        long assigned = 0;
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (ledger == null || ledger.roomId != roomId
                    || !FactionIdentityAuthority.sameFamily(ledger.faction, site.faction)) continue;
            assigned += Math.max(0, ledger.assigned);
        }
        return (int)Math.min(Integer.MAX_VALUE, assigned);
    }

    private static NpcEntity facilityVendor(World world, NpcFactionSite site, Category category,
                                            int roomId) {
        if (world == null || site == null || category == null || world.npcs == null) return null;
        String stableId = facilityVendorId(site, category);
        for (NpcEntity npc : world.npcs) {
            if (npc == null) continue;
            if (stableId.equals(npc.id)) return npc;
            if (world.roomIdAt(npc.x, npc.y) == roomId
                    && FactionIdentityAuthority.sameFamily(npc.faction, site.faction)
                    && category == categoryForRole(npc.role)) return npc;
        }
        return null;
    }

    private static Point firstLegalVendorPoint(GamePanel game, int roomId) {
        if (game == null || game.world == null || roomId < 0) return null;
        java.awt.Rectangle room = game.world.roomRect(roomId);
        if (room == null) return null;
        int xStart = Math.max(0, room.x + 1);
        int xEnd = Math.min(game.world.w, room.x + room.width - 1);
        int yStart = Math.max(0, room.y + 1);
        int yEnd = Math.min(game.world.h, room.y + room.height - 1);
        for (int x = xStart; x < xEnd; x++) {
            for (int y = yStart; y < yEnd; y++) {
                if (game.world.roomIdAt(x, y) != roomId || !game.world.walkable(x, y)) continue;
                if (x == game.playerX && y == game.playerY) continue;
                if (game.world.npcAt(x, y) != null || game.world.mapObjectAt(x, y) != null) continue;
                if (game.baseObjectAt(x, y) != null
                        || game.world.isDoorAccessReservedForObject(x, y)) continue;
                return new Point(x, y);
            }
        }
        return null;
    }

    private static MapObjectState ensureFacilityMarket(World world, NpcFactionSite site,
                                                        BaseObject facility, Category category,
                                                        int roomId, int workers, int x, int y) {
        if (world == null || !world.inBounds(x, y)) return null;
        MapObjectState existing = world.mapObjectAt(x, y);
        if (existing != null) {
            if (!"faction-market".equals(existing.type)) return null;
            enrichFacilityMarket(existing, site, facility, workers);
            return existing;
        }
        RoomProfile profile = roomId >= 0 && roomId < world.roomProfiles.size()
                ? world.roomProfile(roomId) : null;
        char under = world.tiles[x][y];
        MapObjectState market = MapObjectState.factionMarket(x, y, world.zoneType, site.faction,
                category.id, category.coverage, roomId,
                profile == null ? "controlled room" : profile.name, under);
        enrichFacilityMarket(market, site, facility, workers);
        world.tiles[x][y] = market.glyph;
        world.mapObjects.add(market);
        return market;
    }

    private static void enrichFacilityMarket(MapObjectState market, NpcFactionSite site,
                                              BaseObject facility, int workers) {
        if (market == null) return;
        market.stockState = MapObjectState.setStockFlag(market.stockState,
                "facilityVendor", "true");
        market.stockState = MapObjectState.setStockFlag(market.stockState,
                "sourceSite", siteToken(site));
        market.stockState = MapObjectState.setStockFlag(market.stockState,
                "sourceFacility", facilityToken(facility));
        market.stockState = MapObjectState.setStockFlag(market.stockState,
                "assignedWorkers", Integer.toString(Math.max(0, workers)));
    }

    private static String facilityVendorId(NpcFactionSite site, Category category) {
        String categoryId = category == null ? "GENERAL" : category.id.toUpperCase(Locale.ROOT).replace('-', '_');
        return "FACTION-FACILITY-VENDOR-" + categoryId + "-" + siteToken(site);
    }

    private static String facilityToken(BaseObject facility) {
        if (facility == null) return "FACILITY-UNLINKED";
        return "FACILITY-" + Integer.toUnsignedString(java.util.Objects.hash(
                facility.x, facility.y, facility.symbol, safe(facility.name),
                safe(facility.constructionLinkedSiteName)), 36).toUpperCase(Locale.ROOT);
    }

    private static boolean sameLocation(NpcFactionSite site, World world) {
        return site != null && world != null
                && site.sectorX == world.sectorX && site.sectorY == world.sectorY
                && site.zoneX == world.zoneX && site.zoneY == world.zoneY
                && site.floor == world.floor;
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
