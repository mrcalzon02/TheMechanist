package mechanist;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;

/** Finite trade exchanged between an inhabited floor and its waste-processing sewer layer. */
final class VerticalTradeReserveRecord {
    String id = "vertical.unassigned";
    String itemName = "Fertilizer";
    Faction faction = Faction.NONE;
    String sourceLabel = "unrecorded sewer processor";
    String route = "unrecorded inter-floor route";
    int capacity = 4;
    int remaining = 4;
    int restockIntervalTurns = GamePanel.TURNS_PER_HOUR * GamePanel.HOURS_PER_DAY * 2;
    long nextRestockWorldTurn = restockIntervalTurns;

    String saveLine() {
        return enc(id) + "|" + enc(itemName) + "|" + (faction == null ? Faction.NONE.name() : faction.name())
                + "|" + enc(sourceLabel) + "|" + enc(route) + "|" + capacity + "|" + remaining + "|"
                + restockIntervalTurns + "|" + nextRestockWorldTurn;
    }

    static VerticalTradeReserveRecord parse(String line) {
        try {
            String[] a = line.split("\\|", 9);
            if (a.length < 9) return null;
            VerticalTradeReserveRecord r = new VerticalTradeReserveRecord();
            r.id = dec(a[0]);
            r.itemName = dec(a[1]);
            r.faction = Faction.valueOf(a[2]);
            r.sourceLabel = dec(a[3]);
            r.route = dec(a[4]);
            r.capacity = Math.max(1, Integer.parseInt(a[5]));
            r.remaining = Math.max(0, Math.min(r.capacity, Integer.parseInt(a[6])));
            r.restockIntervalTurns = Math.max(1, Integer.parseInt(a[7]));
            r.nextRestockWorldTurn = Math.max(0L, Long.parseLong(a[8]));
            return r;
        } catch (Exception ignored) {
            return null;
        }
    }

    String playerLine() {
        return itemName + " route: " + sourceLabel + "; " + remaining + "/" + capacity
                + " unit(s) available; " + (remaining > 0 ? "next processing batch" : "depleted until batch")
                + " at world turn " + nextRestockWorldTurn + ".";
    }

    private static String enc(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    private static String dec(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}

final class VerticalFloorTradeAuthority {
    record Profile(boolean sewerMarket, int importBuyPremiumPct, int playerSalePremiumPct, String contextLine) { }

    private static final String[] EXPORTS = {"Fertilizer", "Chemical reagent bottle"};

    private VerticalFloorTradeAuthority() { }

    static Profile apply(TraderSession trader, World world, Faction faction, long worldTurn, int localTurn) {
        if (trader == null || world == null) return new Profile(false, 0, 0, "No inter-floor trade route available.");
        Faction owner = faction == null ? Faction.NONE : faction;
        int added = 0;
        int available = 0;
        for (String item : EXPORTS) {
            VerticalTradeReserveRecord reserve = reserveFor(world, owner, item);
            if (reserve == null) {
                reserve = create(world, owner, item, worldTurn);
                world.verticalTradeReserves.add(reserve);
            }
            refresh(reserve, worldTurn);
            removeOffer(trader, item);
            if (reserve.remaining <= 0) continue;
            TradeOffer offer = new TradeOffer(item, categoryFor(item), Math.max(1, ItemCatalog.priceFor(item)),
                    world.sewerLayer
                            ? "waste-runoff output processed for trade with inhabited floors above."
                            : "sewer-processed growth and chemical feedstock lifted from the layer below.");
            offer.verticalTradeReserveId = reserve.id;
            offer.provenance = provenance(offer.name, reserve, world, owner, localTurn);
            trader.offers.add(offer);
            added++;
            available += reserve.remaining;
            appendSummary(trader, reserve.playerLine());
        }

        String layer = "Floor " + Math.max(1, world.floor);
        String context;
        Profile profile;
        if (world.sewerLayer) {
            context = "Sewer import demand: " + layer + " above supplies food, clean water, filters, and tools; "
                    + "players receive +20% sale value for those goods here, while scarce local shelf stock costs +12%.";
            profile = new Profile(true, 12, 20, context);
        } else {
            context = "Inter-floor supply: " + layer + " sewer below exports waste-runoff fertilizer and basic chemical reagents upward; "
                    + available + " routed unit(s) are currently available.";
            profile = new Profile(false, 0, 0, context);
        }
        trader.verticalFloorTrade = profile;
        appendSummary(trader, context);
        DebugLog.audit("VERTICAL_FLOOR_TRADE", "zone=" + world.zoneType.label + " floor=" + world.floor
                + " sewer=" + world.sewerLayer + " added=" + added + " available=" + available);
        return profile;
    }

    static int adjustBuyPrice(Profile profile, TradeOffer offer, int ordinaryPrice) {
        if (profile == null || !profile.sewerMarket() || !isUpstairsProvision(offer == null ? "" : offer.name,
                offer == null ? "" : offer.category)) return Math.max(1, ordinaryPrice);
        return adjusted(ordinaryPrice, profile.importBuyPremiumPct());
    }

    static int adjustSellPrice(Profile profile, String item, int ordinaryPrice) {
        if (profile == null || !profile.sewerMarket() || !isUpstairsProvision(item, categoryFor(item))) {
            return Math.max(1, ordinaryPrice);
        }
        return adjusted(ordinaryPrice, profile.playerSalePremiumPct());
    }

    static String purchaseBlock(World world, TradeOffer offer, long worldTurn) {
        if (offer == null || offer.verticalTradeReserveId == null || offer.verticalTradeReserveId.isBlank()) return "";
        VerticalTradeReserveRecord reserve = reserveById(world, offer.verticalTradeReserveId);
        if (reserve == null) return "its inter-floor supply ledger is unavailable";
        refresh(reserve, worldTurn);
        return reserve.remaining > 0 ? "" : reserve.sourceLabel + " is depleted until world turn " + reserve.nextRestockWorldTurn;
    }

    static boolean consume(World world, TradeOffer offer, long worldTurn) {
        if (offer == null || offer.verticalTradeReserveId == null || offer.verticalTradeReserveId.isBlank()) return true;
        VerticalTradeReserveRecord reserve = reserveById(world, offer.verticalTradeReserveId);
        if (reserve == null) return false;
        refresh(reserve, worldTurn);
        if (reserve.remaining <= 0) return false;
        reserve.remaining--;
        return true;
    }

    static void updateSessionAfterPurchase(TraderSession trader, World world, TradeOffer offer) {
        if (trader == null || offer == null || offer.verticalTradeReserveId == null
                || offer.verticalTradeReserveId.isBlank()) return;
        VerticalTradeReserveRecord reserve = reserveById(world, offer.verticalTradeReserveId);
        if (reserve == null) return;
        appendSummary(trader, reserve.playerLine());
        if (reserve.remaining <= 0) trader.offers.removeIf(o -> o != null && reserve.id.equals(o.verticalTradeReserveId));
    }

    static VerticalTradeReserveRecord reserveFor(World world, Faction faction, String item) {
        if (world == null || world.verticalTradeReserves == null) return null;
        Faction wanted = faction == null ? Faction.NONE : faction;
        for (VerticalTradeReserveRecord reserve : world.verticalTradeReserves) {
            if (reserve != null && reserve.faction == wanted && ItemQuality.namesMatch(reserve.itemName, item)) return reserve;
        }
        return null;
    }

    static VerticalTradeReserveRecord reserveById(World world, String id) {
        if (world == null || id == null || id.isBlank() || world.verticalTradeReserves == null) return null;
        for (VerticalTradeReserveRecord reserve : world.verticalTradeReserves) {
            if (reserve != null && id.equals(reserve.id)) return reserve;
        }
        return null;
    }

    private static VerticalTradeReserveRecord create(World world, Faction faction, String item, long worldTurn) {
        VerticalTradeReserveRecord reserve = new VerticalTradeReserveRecord();
        reserve.id = "vertical." + Math.abs(Objects.hash(world.seed, world.floor, world.sewerLayer, faction.name(), item));
        reserve.itemName = item;
        reserve.faction = faction;
        String floor = "Floor " + Math.max(1, world.floor);
        if (world.sewerLayer) {
            reserve.sourceLabel = floor + " sewer waste processors fed by runoff from the floor above";
            reserve.route = floor + " waste chutes -> sewer settling and reclamation -> sewer trader shelf";
            reserve.capacity = item.equals("Fertilizer") ? 10 : 7;
        } else {
            reserve.sourceLabel = floor + " sewer processors below";
            reserve.route = floor + " sewer reclamation -> freight lift -> " + floor + " trader shelf";
            reserve.capacity = item.equals("Fertilizer") ? 7 : 5;
        }
        reserve.remaining = reserve.capacity;
        reserve.restockIntervalTurns = days(world.sewerLayer ? 1 : 2);
        reserve.nextRestockWorldTurn = Math.max(0L, worldTurn) + reserve.restockIntervalTurns;
        return reserve;
    }

    private static ItemProvenanceRecord provenance(String item, VerticalTradeReserveRecord reserve, World world,
                                                    Faction faction, int localTurn) {
        ItemProvenanceRecord record = ItemProvenanceRecord.of(item, faction, reserve.sourceLabel, world, localTurn,
                "universal waste runoff, separated mineral nutrients, and basic chemical feedstock", reserve.route);
        record.productionSource = "vertical sewer reclamation trade";
        record.producingFacility = reserve.sourceLabel;
        record.chain = reserve.route;
        return record;
    }

    private static void refresh(VerticalTradeReserveRecord reserve, long worldTurn) {
        if (reserve == null || worldTurn < reserve.nextRestockWorldTurn) return;
        reserve.remaining = reserve.capacity;
        reserve.nextRestockWorldTurn = Math.max(0L, worldTurn) + reserve.restockIntervalTurns;
    }

    private static void removeOffer(TraderSession trader, String item) {
        trader.offers.removeIf(offer -> offer != null && ItemQuality.namesMatch(offer.name, item));
    }

    private static boolean isUpstairsProvision(String item, String category) {
        String low = (safe(item) + " " + safe(category)).toLowerCase(Locale.ROOT);
        return contains(low, "food", "ration", "meal", "water", "canteen", "filter", "tool", "supplies");
    }

    private static String categoryFor(String item) {
        ItemDef definition = ItemCatalog.get(item);
        return definition == null ? "inter-floor goods" : definition.category;
    }

    private static int adjusted(int price, int pct) {
        int base = Math.max(1, price);
        int adjusted = Math.max(1, (int) Math.round(base * (100.0 + pct) / 100.0));
        return pct > 0 ? Math.max(base + 1, adjusted) : adjusted;
    }

    private static int days(int days) {
        return Math.max(1, days) * GamePanel.TURNS_PER_HOUR * GamePanel.HOURS_PER_DAY;
    }

    private static void appendSummary(TraderSession trader, String line) {
        if (line == null || line.isBlank()) return;
        String current = trader.supplyChainSummary == null ? "" : trader.supplyChainSummary;
        if (current.contains(line)) return;
        trader.supplyChainSummary = current.isBlank() ? line : current + " " + line;
    }

    private static boolean contains(String text, String... needles) {
        for (String needle : needles) if (text.contains(needle)) return true;
        return false;
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
