package mechanist;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Connects persisted local population ledgers to essential stock and category prices. */
final class PopulationMarketPressureAuthority {
    record Pressure(String key, String label, int demandUnits, int visibleSupplyUnits,
                    int buyAdjustmentPct, int sellAdjustmentPct) {
        String band() {
            if (buyAdjustmentPct >= 24) return "severe shortage";
            if (buyAdjustmentPct >= 12) return "strained";
            if (buyAdjustmentPct >= 4) return "tight";
            if (buyAdjustmentPct <= -5) return "surplus";
            return "balanced";
        }
    }

    record Profile(int populationTarget, int assignedWorkers, int recordedDeaths,
                   int facilityLinkedLedgers, List<String> demandDrivers, List<Pressure> pressures) {
        Pressure pressureFor(String item, String category) {
            String key = categoryKey(item, category);
            for (Pressure pressure : pressures) if (pressure.key().equals(key)) return pressure;
            return new Pressure("general", "General goods", Math.max(1, populationTarget / 8),
                    12, 0, 0);
        }

        List<String> contextLines() {
            ArrayList<String> lines = new ArrayList<>();
            lines.add("Population demand: target " + populationTarget + " people; assigned workforce "
                    + assignedWorkers + "; recorded losses " + recordedDeaths + ".");
            lines.add("Market support: " + facilityLinkedLedgers
                    + " population ledger(s) are linked to named facilities.");
            if (!demandDrivers.isEmpty()) lines.add("Demand identity: " + String.join("; ", demandDrivers) + ".");
            Pressure highest = pressures.isEmpty() ? null : pressures.get(0);
            for (Pressure pressure : pressures) {
                if (highest == null || pressure.buyAdjustmentPct() > highest.buyAdjustmentPct()) highest = pressure;
            }
            if (highest != null) {
                lines.add("Strongest local pressure: " + highest.label() + " is " + highest.band()
                        + " (demand " + highest.demandUnits() + " / visible supply "
                        + highest.visibleSupplyUnits() + ").");
            }
            return lines;
        }

        String offerLine(TradeOffer offer) {
            if (offer == null) return "Population pressure: select an offer for category demand.";
            Pressure pressure = pressureFor(offer.name, offer.category);
            return "Population pressure: " + pressure.label() + " is " + pressure.band()
                    + "; demand " + pressure.demandUnits() + " / visible supply "
                    + pressure.visibleSupplyUnits() + "; buy " + signedPercent(pressure.buyAdjustmentPct())
                    + ", sale value " + signedPercent(pressure.sellAdjustmentPct()) + ".";
        }
    }

    private PopulationMarketPressureAuthority() { }

    static Profile apply(TraderSession trader, World world, Faction faction, int turn) {
        return apply(trader, world, faction, (long) turn, turn);
    }

    static Profile apply(TraderSession trader, World world, Faction faction, long worldTurn, int localTurn) {
        if (trader == null) return emptyProfile();
        PopulationTotals totals = totals(world, worldTurn);
        int population = totals.populationTarget();
        ArrayList<String> allocations = new ArrayList<>();
        if (population >= 8) {
            EssentialSupplyProvenanceAuthority.Allocation food = EssentialSupplyProvenanceAuthority.allocate(
                    trader, world, faction, worldTurn, localTurn, "food", "Emergency rations",
                    "allocated to this counter because local residents require a dependable food source.");
            EssentialSupplyProvenanceAuthority.Allocation water = EssentialSupplyProvenanceAuthority.allocate(
                    trader, world, faction, worldTurn, localTurn, "water", "Water bottle",
                    "allocated to this counter because local residents require a dependable water source.");
            if (food.newlyAdded()) allocations.add("Emergency rations");
            if (water.newlyAdded()) allocations.add("Water bottle");
        }
        if (population >= 20 || totals.recordedDeaths() > 0) {
            ensureOffer(trader, world, faction, localTurn, "Bandage roll", "medical",
                    "allocated to this counter because local population and casualty pressure support basic medicine.", allocations);
        }
        if (totals.productionBonus() >= 4) {
            ensureOffer(trader, world, faction, localTurn, "Tool bundle", "tool",
                    "allocated because local labor and industrial rosters support practical tool demand.", allocations);
        }
        if (totals.securityBonus() >= 4) {
            ensureOffer(trader, world, faction, localTurn, "Stub cartridge box", "ammo",
                    "allocated because local duty, custody, or gang rosters support ammunition demand.", allocations);
        }
        if (totals.luxuryBonus() >= 4) {
            ensureOffer(trader, world, faction, localTurn, "Noble preserved delicacy", "food/luxury",
                    "allocated because local noble households support prestige-food demand.", allocations);
        }

        ArrayList<Pressure> pressures = new ArrayList<>();
        pressures.add(pressure("food", "Food", population + totals.foodBonus(), supplyUnits(trader, "food")));
        pressures.add(pressure("water", "Water", population + totals.waterBonus(), supplyUnits(trader, "water")));
        pressures.add(pressure("medical", "Medicine",
                Math.max(2, population / 3 + totals.recordedDeaths() * 4 + totals.medicalBonus()),
                supplyUnits(trader, "medical")));
        pressures.add(pressure("production", "Tools and components",
                Math.max(2, totals.assignedWorkers() * 2 + totals.facilityLinkedLedgers() * 4
                        + totals.productionBonus()),
                supplyUnits(trader, "production")));
        int securityDemand = (securityFaction(faction) ? Math.max(3, population / 2) : Math.max(2, population / 8))
                + totals.securityBonus();
        pressures.add(pressure("security", "Weapons and ammunition", securityDemand,
                supplyUnits(trader, "security")));
        pressures.add(pressure("luxury", "Luxury goods", Math.max(1, totals.luxuryBonus()),
                supplyUnits(trader, "luxury")));

        Profile profile = new Profile(population, totals.assignedWorkers(), totals.recordedDeaths(),
                totals.facilityLinkedLedgers(), totals.demandDrivers(), List.copyOf(pressures));
        trader.populationPressure = profile;
        if (!allocations.isEmpty()) {
            String addition = "Population allocation added " + String.join(", ", allocations) + ".";
            trader.supplyChainSummary = trader.supplyChainSummary == null || trader.supplyChainSummary.isBlank()
                    ? addition : trader.supplyChainSummary + " " + addition;
        }
        return profile;
    }

    static int adjustBuyPrice(Profile profile, TradeOffer offer, int ordinaryPrice) {
        if (profile == null || offer == null) return Math.max(1, ordinaryPrice);
        int pct = profile.pressureFor(offer.name, offer.category).buyAdjustmentPct();
        return adjusted(ordinaryPrice, pct);
    }

    static int adjustSellPrice(Profile profile, String item, int ordinaryPrice) {
        if (profile == null || item == null || item.isBlank()) return Math.max(1, ordinaryPrice);
        int pct = profile.pressureFor(item, categoryForItem(item)).sellAdjustmentPct();
        return adjusted(ordinaryPrice, pct);
    }

    private static Pressure pressure(String key, String label, int demand, int supply) {
        int buyPct = Math.max(-8, Math.min(30, Math.floorDiv(demand - supply, 4)));
        int sellPct = Math.max(-4, Math.min(15, Math.floorDiv(buyPct, 2)));
        return new Pressure(key, label, Math.max(0, demand), Math.max(0, supply), buyPct, sellPct);
    }

    private static int adjusted(int price, int pct) {
        return Math.max(1, (int) Math.round(Math.max(1, price) * (100.0 + pct) / 100.0));
    }

    private static int supplyUnits(TraderSession trader, String key) {
        int matchingOffers = 0;
        for (TradeOffer offer : trader.offers) {
            if (offer != null && key.equals(categoryKey(offer.name, offer.category))) matchingOffers++;
        }
        int siteUnits = 0;
        if (trader.sourceSite != null && trader.sourceSite.outputs != null) {
            int matchingOutputs = 0;
            for (String output : trader.sourceSite.outputs) {
                if (key.equals(categoryKey(output, categoryForItem(output)))) matchingOutputs++;
            }
            if (matchingOutputs > 0) {
                siteUnits = Math.max(1, trader.sourceSite.stock / Math.max(1, trader.sourceSite.outputs.size()));
            }
        }
        return matchingOffers * 12 + siteUnits;
    }

    private static void ensureOffer(TraderSession trader, World world, Faction faction, int turn,
                                    String item, String category, String description,
                                    List<String> allocations) {
        for (TradeOffer offer : trader.offers) {
            if (offer != null && ItemQuality.namesMatch(offer.name, item)) return;
        }
        ItemProvenanceRecord provenance = ItemProvenanceRecord.of(item,
                faction == null ? Faction.NONE : faction, "local population allocation", world, turn,
                "resident demand and accessible vendor capacity", "allocated into essential local vendor stock");
        trader.offers.add(new TradeOffer(item, category, Math.max(1, ItemCatalog.priceFor(item)),
                description, provenance));
        allocations.add(item);
    }

    private static PopulationTotals totals(World world, long worldTurn) {
        if (world == null) return new PopulationTotals(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, List.of());
        int population = 0;
        int assigned = 0;
        int dead = 0;
        int facilities = 0;
        int foodBonus = 0;
        int waterBonus = 0;
        int medicalBonus = 0;
        int productionBonus = 0;
        int securityBonus = 0;
        int luxuryBonus = 0;
        LinkedHashSet<String> drivers = new LinkedHashSet<>();
        if (world.roomPopulationLedgers != null) {
            for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
                if (ledger == null) continue;
                int units = Math.max(0, ledger.capacity);
                population += units;
                assigned += Math.max(0, ledger.assigned);
                dead += Math.max(0, ledger.dead);
                if (ledger.facilityId != null && !ledger.facilityId.isBlank()) facilities++;
                String text = (safe(ledger.sourceKind) + " " + safe(ledger.sourceLabel) + " "
                        + safe(ledger.roomName) + " " + safe(ledger.facilityPurpose) + " "
                        + safe(ledger.facilityProductFocus) + " "
                        + (ledger.faction == null ? "" : ledger.faction.label)).toLowerCase(Locale.ROOT);
                if (contains(text, "barracks", "duty", "guard", "military", "custody", "prison", "holding")) {
                    securityBonus += Math.max(1, units / 2);
                    medicalBonus += Math.max(1, units / 4);
                    drivers.add("security and custody rosters raise ammunition and medical demand");
                }
                if (contains(text, "contract labor", "forge", "industrial", "factory", "worker", "rail")) {
                    productionBonus += Math.max(1, units / 2);
                    foodBonus += Math.max(1, units / 4);
                    drivers.add("industrial and transport labor raises tool and work-food demand");
                }
                if (contains(text, "household", "noble", "servant", "estate")) {
                    luxuryBonus += Math.max(1, units);
                    medicalBonus += Math.max(1, units / 4);
                    drivers.add("noble households raise luxury and private-care demand");
                }
                if (contains(text, "gang", "hidden congregation", "cult", "sump brood")) {
                    securityBonus += Math.max(1, units / 3);
                    drivers.add("gang and hidden populations raise weapon and illicit-market pressure");
                }
                if (contains(text, "displaced", "refugee", "relief", "evacuation", "rail intake", "pilgrim")) {
                    foodBonus += Math.max(1, units / 2);
                    waterBonus += Math.max(1, units / 2);
                    medicalBonus += Math.max(1, units / 3);
                    drivers.add("arrivals and displaced populations raise immediate food, water, and medical demand");
                }
                if (contains(text, "creche", "daycare", "local hab", "family")) {
                    foodBonus += Math.max(1, units / 4);
                    waterBonus += Math.max(1, units / 4);
                    drivers.add("resident households and creches raise everyday food and water demand");
                }
            }
        }
        int growingChildren = 0;
        if (world.crecheCohorts != null) {
            FactionCrecheAuthority.tick(world, worldTurn);
            for (CrecheCohortRecord cohort : world.crecheCohorts) {
                if (cohort != null && cohort.remaining > 0
                        && cohort.ageYears(worldTurn) < FactionCrecheAuthority.MATURITY_YEARS) {
                    growingChildren += cohort.remaining;
                }
            }
        }
        if (growingChildren > 0) {
            foodBonus += growingChildren * 2;
            waterBonus += growingChildren;
            medicalBonus += Math.max(1, growingChildren / 2);
            drivers.add("growing crèche cohorts add " + growingChildren
                    + " children of growth-food, clean-water, and pediatric-care demand");
        }
        if (population <= 0 && world.npcs != null) population = world.npcs.size();
        return new PopulationTotals(population, assigned, dead, facilities, foodBonus, waterBonus,
                medicalBonus, productionBonus, securityBonus, luxuryBonus, List.copyOf(drivers));
    }

    private static String categoryKey(String item, String category) {
        String text = (safe(item) + " " + safe(category) + " " + categoryForItem(item)).toLowerCase(Locale.ROOT);
        if (contains(text, "luxury", "prestige", "noble delicacy", "amasec", "gildwine")) return "luxury";
        if (contains(text, "food", "ration", "meal", "nutrient", "fungus")) return "food";
        if (contains(text, "water", "canteen", "purification")) return "water";
        if (contains(text, "medical", "bandage", "medkit", "antiseptic", "dressing", "medicine")) return "medical";
        if (contains(text, "tool", "component", "machine", "part", "supplies", "wire", "mechanical")) return "production";
        if (contains(text, "weapon", "ammo", "ammunition", "security", "gun", "pistol", "cartridge", "shell")) return "security";
        return "general";
    }

    private static String categoryForItem(String item) {
        ItemDef definition = ItemCatalog.get(item);
        return definition == null ? "" : safe(definition.category) + " " + safe(definition.use);
    }

    private static boolean securityFaction(Faction faction) {
        return faction == Faction.ARBITES || faction == Faction.CIVIC_WARDENS
                || faction == Faction.IMPERIAL_GUARD;
    }

    private static boolean contains(String text, String... needles) {
        for (String needle : needles) if (text.contains(needle)) return true;
        return false;
    }

    private static String signedPercent(int pct) { return (pct >= 0 ? "+" : "") + pct + "%"; }
    private static String safe(String value) { return value == null ? "" : value; }
    private static Profile emptyProfile() { return new Profile(0, 0, 0, 0, List.of(), List.of()); }

    private record PopulationTotals(int populationTarget, int assignedWorkers,
                                    int recordedDeaths, int facilityLinkedLedgers,
                                    int foodBonus, int waterBonus, int medicalBonus,
                                    int productionBonus, int securityBonus, int luxuryBonus,
                                    List<String> demandDrivers) { }
}
