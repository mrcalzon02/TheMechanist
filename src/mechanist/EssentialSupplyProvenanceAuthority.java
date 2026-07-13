package mechanist;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Persisted, finite food and water stock allocated to population-serving traders. */
final class EssentialSupplyReserveRecord {
    String id = "essential.unassigned";
    String category = "food";
    String itemName = "Emergency rations";
    Faction faction = Faction.NONE;
    String sourceKind = "emergency fallback";
    String sourceLabel = "civic emergency allotment";
    String sourceFacilityId = "";
    String stockClass = "disaster relief stock";
    String route = "released from emergency reserve into essential local vendor stock";
    int capacity = 2;
    int remaining = 2;
    int restockIntervalTurns = GamePanel.TURNS_PER_HOUR * GamePanel.HOURS_PER_DAY * 7;
    long nextRestockWorldTurn = restockIntervalTurns;

    String saveLine() {
        return enc(id) + "|" + enc(category) + "|" + enc(itemName) + "|"
                + (faction == null ? Faction.NONE.name() : faction.name()) + "|"
                + enc(sourceKind) + "|" + enc(sourceLabel) + "|" + enc(sourceFacilityId) + "|"
                + enc(stockClass) + "|" + enc(route) + "|" + capacity + "|" + remaining + "|"
                + restockIntervalTurns + "|" + nextRestockWorldTurn;
    }

    static EssentialSupplyReserveRecord parse(String line) {
        try {
            String[] a = line.split("\\|", 13);
            if (a.length < 13) return null;
            EssentialSupplyReserveRecord r = new EssentialSupplyReserveRecord();
            r.id = dec(a[0]);
            r.category = dec(a[1]);
            r.itemName = dec(a[2]);
            r.faction = Faction.valueOf(a[3]);
            r.sourceKind = dec(a[4]);
            r.sourceLabel = dec(a[5]);
            r.sourceFacilityId = dec(a[6]);
            r.stockClass = dec(a[7]);
            r.route = dec(a[8]);
            r.capacity = Math.max(1, Integer.parseInt(a[9]));
            r.remaining = Math.max(0, Math.min(r.capacity, Integer.parseInt(a[10])));
            r.restockIntervalTurns = Math.max(1, Integer.parseInt(a[11]));
            r.nextRestockWorldTurn = Math.max(0L, Long.parseLong(a[12]));
            return r;
        } catch (Exception ignored) {
            return null;
        }
    }

    String playerLine() {
        return capitalize(category) + " supply: " + stockClass + " from " + sourceLabel
                + "; " + remaining + "/" + capacity + " unit(s) available; "
                + (remaining > 0 ? "next source refill" : "source depleted, refill")
                + " at world turn " + nextRestockWorldTurn + ".";
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) return "Essential";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String enc(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    private static String dec(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}

final class EssentialSupplyProvenanceAuthority {
    record Allocation(TradeOffer offer, EssentialSupplyReserveRecord reserve, boolean newlyAdded) { }
    private record Source(String kind, String label, String facilityId, String stockClass,
                          String route, int capacity, int restockTurns) { }

    private EssentialSupplyProvenanceAuthority() { }

    static Allocation allocate(TraderSession trader, World world, Faction faction, long worldTurn, int localTurn,
                               String category, String preferredItem, String description) {
        if (trader == null || world == null || !isEssentialCategory(category)) return new Allocation(null, null, false);
        EssentialSupplyReserveRecord reserve = reserveFor(world, faction, category);
        if (reserve == null) {
            reserve = createReserve(world, faction, category, preferredItem, worldTurn);
            world.essentialSupplyReserves.add(reserve);
        }
        refresh(reserve, worldTurn);
        if (reserve.remaining <= 0) {
            removeCategoryOffers(trader, category);
            appendSummary(trader, reserve.playerLine());
            return new Allocation(null, reserve, false);
        }

        TradeOffer preferred = null;
        ArrayList<TradeOffer> matches = new ArrayList<>();
        for (TradeOffer offer : trader.offers) {
            if (offer == null || !matchesCategory(offer.name, offer.category, category)) continue;
            matches.add(offer);
            if (ItemQuality.namesMatch(offer.name, preferredItem)) preferred = offer;
        }
        boolean added = false;
        if (preferred == null) {
            preferred = new TradeOffer(preferredItem, category, Math.max(1, ItemCatalog.priceFor(preferredItem)),
                    description);
            trader.offers.add(preferred);
            matches.add(preferred);
            added = true;
        }
        for (TradeOffer offer : matches) attach(offer, reserve, world, faction, localTurn);
        appendSummary(trader, reserve.playerLine());
        return new Allocation(preferred, reserve, added);
    }

    static boolean canFulfill(World world, TradeOffer offer, long worldTurn) {
        if (offer == null || offer.essentialSupplyReserveId == null || offer.essentialSupplyReserveId.isBlank()) return true;
        EssentialSupplyReserveRecord reserve = reserveById(world, offer.essentialSupplyReserveId);
        if (reserve == null) return false;
        refresh(reserve, worldTurn);
        return reserve.remaining > 0;
    }

    static String purchaseBlock(World world, TradeOffer offer, long worldTurn) {
        if (canFulfill(world, offer, worldTurn)) return "";
        EssentialSupplyReserveRecord reserve = offer == null ? null : reserveById(world, offer.essentialSupplyReserveId);
        if (reserve == null) return "its essential supply ledger is unavailable";
        return reserve.sourceLabel + " is depleted until world turn " + reserve.nextRestockWorldTurn;
    }

    static boolean consume(World world, TradeOffer offer, long worldTurn) {
        if (offer == null || offer.essentialSupplyReserveId == null || offer.essentialSupplyReserveId.isBlank()) return true;
        EssentialSupplyReserveRecord reserve = reserveById(world, offer.essentialSupplyReserveId);
        if (reserve == null) return false;
        refresh(reserve, worldTurn);
        if (reserve.remaining <= 0) return false;
        reserve.remaining--;
        return true;
    }

    static void updateSessionAfterPurchase(TraderSession trader, World world, TradeOffer offer) {
        if (trader == null || offer == null || offer.essentialSupplyReserveId == null
                || offer.essentialSupplyReserveId.isBlank()) return;
        EssentialSupplyReserveRecord reserve = reserveById(world, offer.essentialSupplyReserveId);
        if (reserve == null) return;
        appendSummary(trader, reserve.playerLine());
        if (reserve.remaining <= 0) removeReserveOffers(trader, reserve.id);
    }

    static EssentialSupplyReserveRecord reserveFor(World world, Faction faction, String category) {
        if (world == null || world.essentialSupplyReserves == null) return null;
        Faction wanted = faction == null ? Faction.NONE : faction;
        for (EssentialSupplyReserveRecord reserve : world.essentialSupplyReserves) {
            if (reserve != null && reserve.faction == wanted && category.equalsIgnoreCase(reserve.category)) return reserve;
        }
        return null;
    }

    static EssentialSupplyReserveRecord reserveById(World world, String id) {
        if (world == null || id == null || id.isBlank() || world.essentialSupplyReserves == null) return null;
        for (EssentialSupplyReserveRecord reserve : world.essentialSupplyReserves) {
            if (reserve != null && id.equals(reserve.id)) return reserve;
        }
        return null;
    }

    private static EssentialSupplyReserveRecord createReserve(World world, Faction faction, String category,
                                                               String item, long worldTurn) {
        Faction owner = faction == null ? Faction.NONE : faction;
        Source source = resolveSource(world, owner, category, item);
        EssentialSupplyReserveRecord reserve = new EssentialSupplyReserveRecord();
        reserve.id = "essential." + Math.abs(Objects.hash(world.seed, owner.name(), category));
        reserve.category = category;
        reserve.itemName = item;
        reserve.faction = owner;
        reserve.sourceKind = source.kind();
        reserve.sourceLabel = source.label();
        reserve.sourceFacilityId = source.facilityId();
        reserve.stockClass = source.stockClass();
        reserve.route = source.route();
        reserve.capacity = source.capacity();
        reserve.remaining = source.capacity();
        reserve.restockIntervalTurns = source.restockTurns();
        reserve.nextRestockWorldTurn = Math.max(0L, worldTurn) + source.restockTurns();
        return reserve;
    }

    private static Source resolveSource(World world, Faction faction, String category, String item) {
        for (ZoneProductionOutputRecord output : ProductionFacilityOutputSimulationApi.parseProductionLedger(world.zoneProductionHistory)) {
            String text = safe(output.facilityId) + " " + safe(output.facilityPurpose) + " "
                    + safe(output.outputFocus) + " " + safe(output.sampleItems);
            if (!sourceMatches(text, category)) continue;
            int capacity = Math.max(8, Math.min(24, Math.max(1, output.batches) * 4));
            String label = safe(output.facilityId).isBlank() ? safe(output.facilityPurpose) : output.facilityId;
            return new Source("faction production site", label, safe(output.facilityId),
                    stockClass(item, category, "production", faction),
                    label + " -> local faction store -> essential local vendor stock", capacity, days(1));
        }

        for (RoomPopulationLedger ledger : safeLedgers(world)) {
            if (ledger == null || !factionCompatible(faction, ledger.faction)) continue;
            String text = safe(ledger.roomName) + " " + safe(ledger.sourceKind) + " " + safe(ledger.sourceLabel)
                    + " " + safe(ledger.facilityPurpose) + " " + safe(ledger.facilityProductFocus);
            if (!sourceMatches(text, category)) continue;
            String label = !safe(ledger.facilityId).isBlank() ? ledger.facilityId : ledger.roomName;
            int capacity = Math.max(6, Math.min(18, 6 + Math.max(0, ledger.capacity) / 12));
            String kind = category.equals("water") && contains(text.toLowerCase(Locale.ROOT), "recycler", "purif", "treatment")
                    ? "local recycler or purifier" : "local store or provisioning room";
            return new Source(kind, label, safe(ledger.facilityId), stockClass(item, category, kind, faction),
                    label + " -> faction ration issue -> essential local vendor stock", capacity, days(2));
        }

        for (int i = 0; i < world.roomProfiles.size(); i++) {
            RoomProfile room = world.roomProfiles.get(i);
            Faction owner = i < world.roomFactions.size() ? world.roomFactions.get(i) : Faction.NONE;
            if (room == null || !factionCompatible(faction, owner)) continue;
            String text = safe(room.name) + " " + safe(room.descriptor) + " " + safe(room.featureText);
            if (!sourceMatches(text, category)) continue;
            String kind = category.equals("water") && contains(text.toLowerCase(Locale.ROOT), "recycler", "purif", "treatment")
                    ? "local recycler or purifier" : "local store or provisioning room";
            return new Source(kind, room.name, "room." + i, stockClass(item, category, kind, faction),
                    room.name + " -> local faction store -> essential local vendor stock", 8, days(2));
        }

        if (hasRailAccess(world)) {
            return new Source("outside-sector rail shipment", "arcology reinforcement train", "rail.intake",
                    "outside-sector shipment", "outside-sector train -> rail intake -> faction store -> essential local vendor stock",
                    10, days(4));
        }
        return new Source("emergency fallback", "civic emergency allotment", "emergency.reserve",
                "disaster relief stock", "released from emergency reserve into essential local vendor stock",
                2, days(7));
    }

    private static void attach(TradeOffer offer, EssentialSupplyReserveRecord reserve, World world,
                               Faction faction, int localTurn) {
        offer.essentialSupplyReserveId = reserve.id;
        ItemProvenanceRecord provenance = ItemProvenanceRecord.of(offer.name, faction, reserve.sourceLabel,
                world, localTurn, reserve.stockClass + "; finite reserve " + reserve.remaining + "/" + reserve.capacity,
                reserve.route);
        provenance.producingFacility = reserve.sourceFacilityId;
        provenance.productionSource = reserve.sourceKind;
        provenance.chain = reserve.sourceLabel + " -> " + reserve.route;
        offer.provenance = provenance;
        String note = " " + reserve.stockClass + " from " + reserve.sourceLabel + "; reserve "
                + reserve.remaining + "/" + reserve.capacity + ".";
        if (offer.description == null) offer.description = note.trim();
        else if (!offer.description.contains("reserve " + reserve.remaining + "/" + reserve.capacity)) offer.description += note;
    }

    private static void refresh(EssentialSupplyReserveRecord reserve, long worldTurn) {
        if (reserve == null || worldTurn < reserve.nextRestockWorldTurn) return;
        reserve.remaining = reserve.capacity;
        reserve.nextRestockWorldTurn = Math.max(0L, worldTurn) + reserve.restockIntervalTurns;
    }

    private static void removeCategoryOffers(TraderSession trader, String category) {
        trader.offers.removeIf(offer -> offer != null && matchesCategory(offer.name, offer.category, category));
    }

    private static void removeReserveOffers(TraderSession trader, String reserveId) {
        trader.offers.removeIf(offer -> offer != null && reserveId.equals(offer.essentialSupplyReserveId));
    }

    private static void appendSummary(TraderSession trader, String line) {
        if (line == null || line.isBlank()) return;
        String current = trader.supplyChainSummary == null ? "" : trader.supplyChainSummary;
        if (current.contains(line)) return;
        trader.supplyChainSummary = current.isBlank() ? line : current + " " + line;
    }

    private static List<RoomPopulationLedger> safeLedgers(World world) {
        return world.roomPopulationLedgers == null ? List.of() : world.roomPopulationLedgers;
    }

    private static boolean sourceMatches(String text, String category) {
        String low = safe(text).toLowerCase(Locale.ROOT);
        if ("food".equals(category)) return contains(low, "food", "ration", "meal", "nutrient", "fungus",
                "farm", "garden", "hydropon", "mushroom", "kitchen", "cafeteria", "mess", "pantry");
        return contains(low, "water", "recycler", "purif", "reservoir", "cistern", "pump", "treatment");
    }

    private static boolean matchesCategory(String item, String offerCategory, String wanted) {
        String low = (safe(item) + " " + safe(offerCategory)).toLowerCase(Locale.ROOT);
        if ("food".equals(wanted)) {
            if (contains(low, "luxury", "prestige", "noble delicacy", "amasec", "gildwine")) return false;
            return contains(low, "food", "ration", "meal", "nutrient", "fungus", "loaf");
        }
        return "water".equals(wanted) && contains(low, "water", "canteen", "purification");
    }

    private static boolean isEssentialCategory(String category) {
        return "food".equals(category) || "water".equals(category);
    }

    private static boolean hasRailAccess(World world) {
        if (world.zoneType == ZoneType.NEUTRAL_RAIL_DEPOT || world.zoneType == ZoneType.TRAIN_SERVICE_YARD) return true;
        for (RoomPopulationLedger ledger : safeLedgers(world)) {
            String text = safe(ledger.sourceKind) + " " + safe(ledger.sourceLabel) + " " + safe(ledger.roomName);
            if (contains(text.toLowerCase(Locale.ROOT), "rail intake", "train", "rail hub")) return true;
        }
        return false;
    }

    private static boolean factionCompatible(Faction wanted, Faction owner) {
        if (owner == null || owner == Faction.NONE || wanted == null || wanted == Faction.NONE) return true;
        if (owner == wanted) return true;
        String a = wanted.name().split("_")[0];
        String b = owner.name().split("_")[0];
        return a.equals(b);
    }

    private static String stockClass(String item, String category, String sourceKind, Faction faction) {
        String low = (safe(item) + " " + safe(sourceKind)).toLowerCase(Locale.ROOT);
        if (contains(low, "spoiled", "tainted", "contaminated", "dubious")) return "spoiled or contaminated " + category;
        if (contains(low, "luxury", "fine", "noble")) return "noble imported food";
        if (faction == Faction.BANDIT || (faction != null && faction.name().startsWith("GANGER"))) return "black-market " + category;
        if ("water".equals(category) && contains(low, "recycler", "purif")) return "locally recycled and purified water";
        if ("food".equals(category) && contains(low, "farm", "garden", "hydropon", "kitchen")) return "fresh local food";
        if ("food".equals(category) && contains(low, "ration", "preserved")) return "preserved faction ration stock";
        return "faction ration stock";
    }

    private static int days(int days) {
        return Math.max(1, days) * GamePanel.TURNS_PER_HOUR * GamePanel.HOURS_PER_DAY;
    }

    private static boolean contains(String text, String... needles) {
        for (String needle : needles) if (text.contains(needle)) return true;
        return false;
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
