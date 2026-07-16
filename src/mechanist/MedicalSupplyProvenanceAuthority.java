package mechanist;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;

/** Finite medical and drug stock tied to clinics, labs, routes, legality, and risk. */
final class MedicalSupplyReserveRecord {
    String id = "medical.unassigned";
    String itemName = "Bandage roll";
    Faction faction = Faction.NONE;
    String stockClass = "legal clinic medicine";
    String legality = "ordinary medical sale";
    String riskClass = "ordinary treatment risk";
    String sourceKind = "clinic reserve";
    String sourceLabel = "local clinic cabinet";
    String sourceFacilityId = "";
    String route = "clinic cabinet -> trader medical shelf";
    String routeRestriction = "no active route restriction";
    int capacity = 3;
    int remaining = 3;
    int restockIntervalTurns = GamePanel.TURNS_PER_HOUR * GamePanel.HOURS_PER_DAY * 3;
    long nextRestockWorldTurn = restockIntervalTurns;

    String saveLine() {
        return enc(id) + "|" + enc(itemName) + "|" + (faction == null ? Faction.NONE.name() : faction.name())
                + "|" + enc(stockClass) + "|" + enc(legality) + "|" + enc(riskClass) + "|"
                + enc(sourceKind) + "|" + enc(sourceLabel) + "|" + enc(sourceFacilityId) + "|"
                + enc(route) + "|" + enc(routeRestriction) + "|" + capacity + "|" + remaining + "|"
                + restockIntervalTurns + "|" + nextRestockWorldTurn;
    }

    static MedicalSupplyReserveRecord parse(String line) {
        try {
            String[] a = line.split("\\|", 15);
            if (a.length < 15) return null;
            MedicalSupplyReserveRecord r = new MedicalSupplyReserveRecord();
            r.id = dec(a[0]); r.itemName = dec(a[1]); r.faction = Faction.valueOf(a[2]);
            r.stockClass = dec(a[3]); r.legality = dec(a[4]); r.riskClass = dec(a[5]);
            r.sourceKind = dec(a[6]); r.sourceLabel = dec(a[7]); r.sourceFacilityId = dec(a[8]);
            r.route = dec(a[9]); r.routeRestriction = dec(a[10]);
            r.capacity = Math.max(1, Integer.parseInt(a[11]));
            r.remaining = Math.max(0, Math.min(r.capacity, Integer.parseInt(a[12])));
            r.restockIntervalTurns = Math.max(1, Integer.parseInt(a[13]));
            r.nextRestockWorldTurn = Math.max(0L, Long.parseLong(a[14]));
            return r;
        } catch (Exception ignored) { return null; }
    }

    String playerLine() {
        return itemName + " supply: " + stockClass + " from " + sourceLabel + "; " + legality + "; risk "
                + riskClass + "; " + remaining + "/" + capacity + " unit(s); " + routeRestriction + "; "
                + (remaining > 0 ? "next refill" : "depleted until refill") + " at world turn "
                + nextRestockWorldTurn + ".";
    }

    private static String enc(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }
    private static String dec(String value) { return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8); }
}

final class MedicalSupplyProvenanceAuthority {
    record Profile(String policy, String treatmentItem, String drugItem, String legality, boolean specialtyMarket) { }
    private record Source(String kind, String label, String facilityId, String route, String stockClass,
                          String restriction, int capacity, int restockTurns) { }

    private MedicalSupplyProvenanceAuthority() { }

    static Profile apply(TraderSession trader, World world, Faction faction, long worldTurn, int localTurn) {
        Profile profile = profileFor(faction);
        if (trader == null || world == null) return profile;
        int demand = medicalDemand(trader);
        boolean infrastructure = hasMedicalSource(world, faction);
        boolean relief = hasReliefDemand(world);
        boolean shipment = hasRailAccess(world);
        if (profile.specialtyMarket() || infrastructure || relief || demand >= 3 || shipment) {
            ensureOffer(trader, profile.treatmentItem(), "treatment stock selected by " + profile.policy() + ".");
        }
        if (profile.specialtyMarket() || infrastructure || demand >= 8) {
            ensureOffer(trader, profile.drugItem(), "medicine or drug stock selected by " + profile.policy() + ".");
        }

        ArrayList<TradeOffer> medicalOffers = new ArrayList<>();
        for (TradeOffer offer : trader.offers) if (isMedicalOrDrug(offer)) medicalOffers.add(offer);
        int available = 0;
        int removed = 0;
        for (TradeOffer offer : medicalOffers) {
            MedicalSupplyReserveRecord reserve = reserveFor(world, faction, offer.name);
            if (reserve == null) {
                reserve = create(world, faction, offer.name, profile, worldTurn);
                world.medicalSupplyReserves.add(reserve);
            }
            refresh(reserve, worldTurn);
            if (reserve.remaining <= 0) {
                trader.offers.remove(offer);
                removed++;
                continue;
            }
            attach(offer, reserve, world, faction, localTurn);
            available += reserve.remaining;
            appendSummary(trader, reserve.playerLine());
        }
        trader.medicalSupplyProfile = profile;
        appendSummary(trader, "Medical supply policy: " + profile.policy() + "; " + medicalOffers.size()
                + " traced offer(s), " + available + " reserve unit(s) available"
                + (removed > 0 ? ", " + removed + " depleted offer(s) withheld" : "") + ".");
        return profile;
    }

    static String purchaseBlock(World world, TradeOffer offer, long worldTurn) {
        if (offer == null || offer.medicalSupplyReserveId == null || offer.medicalSupplyReserveId.isBlank()) return "";
        MedicalSupplyReserveRecord reserve = reserveById(world, offer.medicalSupplyReserveId);
        if (reserve == null) return "its medical supply ledger is unavailable";
        refresh(reserve, worldTurn);
        return reserve.remaining > 0 ? "" : reserve.sourceLabel + " is depleted until world turn " + reserve.nextRestockWorldTurn;
    }

    static boolean consume(World world, TradeOffer offer, long worldTurn) {
        if (offer == null || offer.medicalSupplyReserveId == null || offer.medicalSupplyReserveId.isBlank()) return true;
        MedicalSupplyReserveRecord reserve = reserveById(world, offer.medicalSupplyReserveId);
        if (reserve == null) return false;
        refresh(reserve, worldTurn);
        if (reserve.remaining <= 0) return false;
        reserve.remaining--;
        return true;
    }

    static void updateSessionAfterPurchase(TraderSession trader, World world, TradeOffer offer) {
        if (trader == null || offer == null || offer.medicalSupplyReserveId == null
                || offer.medicalSupplyReserveId.isBlank()) return;
        MedicalSupplyReserveRecord reserve = reserveById(world, offer.medicalSupplyReserveId);
        if (reserve == null) return;
        appendSummary(trader, reserve.playerLine());
        if (reserve.remaining <= 0) trader.offers.removeIf(o -> o != null && reserve.id.equals(o.medicalSupplyReserveId));
    }

    static MedicalSupplyReserveRecord reserveFor(World world, Faction faction, String item) {
        if (world == null || world.medicalSupplyReserves == null) return null;
        Faction owner = safeFaction(faction);
        for (MedicalSupplyReserveRecord reserve : world.medicalSupplyReserves) {
            if (reserve != null && reserve.faction == owner && ItemQuality.namesMatch(reserve.itemName, item)) return reserve;
        }
        return null;
    }

    static MedicalSupplyReserveRecord reserveById(World world, String id) {
        if (world == null || id == null || id.isBlank() || world.medicalSupplyReserves == null) return null;
        for (MedicalSupplyReserveRecord reserve : world.medicalSupplyReserves) if (reserve != null && id.equals(reserve.id)) return reserve;
        return null;
    }

    static Profile profileFor(Faction faction) {
        Faction f = safeFaction(faction);
        String name = f.name();
        if (f == Faction.IMPERIAL_GUARD || f == Faction.SORORITAS) return new Profile(
                "controlled field medicine", "Field dressings", "Medi-Stimm", "restricted service medicine", true);
        if (f == Faction.ARBITES || f == Faction.CIVIC_WARDENS) return new Profile(
                "controlled custody medicine", "Medkit", "White Mercy", "controlled medical issue", true);
        if (f == Faction.NOBLE || name.startsWith("NOBLE_")) return new Profile(
                "private physician supply", "Medkit", "White Mercy", "household physician authorization", true);
        if (f == Faction.BANDIT || name.startsWith("GANGER_") || f == Faction.CULTIST || f == Faction.HERETIC) return new Profile(
                "illicit clinic and performance-drug trade", "Bandage roll", "Stim vial", "black-market drug sale", true);
        if (f == Faction.MUTANT || f == Faction.SCAVENGER) return new Profile(
                "sump survival medicine", "Bandage roll", "Sumpkalm", "unlicensed local medicine", true);
        if (f == Faction.MECHANICUS || f == Faction.MECHANIST_COLLEGIA || name.startsWith("MECHANICUS_")) return new Profile(
                "sealed diagnostic medicine", "Antiseptic vial", "Medi-Stimm", "Mechanist custody only", true);
        return new Profile("ordinary clinic supply", "Bandage roll", "Antiseptic vial", "ordinary medical sale", false);
    }

    private static MedicalSupplyReserveRecord create(World world, Faction faction, String item, Profile profile,
                                                      long worldTurn) {
        Faction owner = safeFaction(faction);
        Source source = resolveSource(world, owner, item, profile);
        MedicalSupplyReserveRecord reserve = new MedicalSupplyReserveRecord();
        reserve.id = "medical." + Math.abs(Objects.hash(world.seed, owner.name(), item));
        reserve.itemName = item; reserve.faction = owner; reserve.stockClass = source.stockClass();
        reserve.legality = profile.legality(); reserve.riskClass = riskFor(item, source.stockClass());
        reserve.sourceKind = source.kind(); reserve.sourceLabel = source.label();
        reserve.sourceFacilityId = source.facilityId(); reserve.route = source.route();
        reserve.routeRestriction = source.restriction(); reserve.capacity = source.capacity();
        reserve.remaining = source.capacity(); reserve.restockIntervalTurns = source.restockTurns();
        reserve.nextRestockWorldTurn = Math.max(0L, worldTurn) + source.restockTurns();
        return reserve;
    }

    private static Source resolveSource(World world, Faction faction, String item, Profile profile) {
        boolean blocked = hasRestriction(world, "blockade", "interdiction", "route closure", "quarantine");
        for (ZoneConflictLossRecord event : HistoricalConflictLossApi.parseConflictLossLedger(world.zoneConflictLossHistory)) {
            String text = (safe(event.eventType) + " " + safe(event.actor) + " " + safe(event.affectedStock)
                    + " " + safe(event.destination) + " " + safe(event.historyNote)).toLowerCase(Locale.ROOT);
            if (!medicalText(text)) continue;
            String label = safe(event.destination).isBlank() ? event.eventType : event.destination;
            String route = safe(event.sourceFacilityId) + " -> " + event.eventType + " by " + event.actor
                    + " -> " + label + " -> trader medical shelf";
            if (contains(text, "relief", "rescue", "evacuation")) return new Source(
                    "disaster relief shipment", label, safe(event.sourceFacilityId), route,
                    "disaster relief medicine", "priority relief issue", 5, days(4));
            if (contains(text, "counterfeit", "contaminated", "tainted")) return new Source(
                    "counterfeit or contaminated batch", label, safe(event.sourceFacilityId), route,
                    contains(text, "counterfeit") ? "counterfeit medicine" : "contaminated medicine",
                    "unsafe batch warning", 2, days(20));
            if (contains(text, "theft", "black-market", "diversion", "smuggl")) return new Source(
                    "black-market pharmaceutical diversion", label, safe(event.sourceFacilityId), route,
                    "black-market pharmaceutical", "illicit event route", 3, days(12));
        }
        for (ZoneProductionOutputRecord output : ProductionFacilityOutputSimulationApi.parseProductionLedger(world.zoneProductionHistory)) {
            String text = safe(output.facilityId) + " " + safe(output.facilityPurpose) + " "
                    + safe(output.outputFocus) + " " + safe(output.sampleItems);
            if (!medicalText(text.toLowerCase(Locale.ROOT)) || !controllerCompatible(faction, output.controller)) continue;
            String label = safe(output.facilityId).isBlank() ? output.facilityPurpose : output.facilityId;
            return new Source("faction clinic or lab production", label, safe(output.facilityId),
                    label + " -> medical store -> trader medical shelf", classFor(item, profile),
                    "no active route restriction", Math.max(4, Math.min(14, output.batches * 3)), days(2));
        }
        for (int i = 0; i < world.roomProfiles.size(); i++) {
            RoomProfile room = world.roomProfiles.get(i);
            Faction owner = i < world.roomFactions.size() ? world.roomFactions.get(i) : Faction.NONE;
            if (room == null || !factionCompatible(faction, owner)) continue;
            String text = (safe(room.name) + " " + safe(room.descriptor) + " " + safe(room.featureText)).toLowerCase(Locale.ROOT);
            if (!medicalText(text)) continue;
            String kind = contains(text, "lab", "laboratory", "chem") ? "local laboratory" :
                    (contains(text, "noble", "house medicae", "physician") ? "private physician supply" : "local clinic");
            return new Source(kind, room.name, "room." + i, room.name + " -> medical cabinet -> trader medical shelf",
                    classFor(item, profile), "no active route restriction", 6, days(3));
        }
        if (hasReliefDemand(world)) return new Source("disaster relief shipment", "population relief intake",
                "relief.intake", "relief intake -> triage store -> trader medical shelf", "disaster relief medicine",
                "priority relief issue", 5, days(4));
        if (!blocked && hasRailAccess(world)) return new Source("outside-sector pharmaceutical shipment",
                "arcology medical freight train", "rail.medical.intake",
                "outside-sector pharmaceutical shipment -> rail intake -> trader medical shelf",
                "outside-sector medicine", "route open", 5, days(6));
        String stockClass = blocked ? "blockade-restricted medicine" : classFor(item, profile);
        return new Source("faction medical reserve", faction.label + " medical cabinet", "faction.medical.reserve",
                faction.label + " medical cabinet -> trader medical shelf", stockClass,
                blocked ? "outside shipments blocked; local reserve only" : "no active route restriction",
                blocked ? 1 : 3, days(blocked ? 10 : 5));
    }

    private static void attach(TradeOffer offer, MedicalSupplyReserveRecord reserve, World world,
                               Faction faction, int localTurn) {
        offer.medicalSupplyReserveId = reserve.id;
        ItemProvenanceRecord provenance = ItemProvenanceRecord.of(offer.name, faction, reserve.sourceLabel, world,
                localTurn, reserve.stockClass + "; finite reserve " + reserve.remaining + "/" + reserve.capacity,
                reserve.route);
        provenance.productionLegalStatus = reserve.legality;
        provenance.productionSource = reserve.sourceKind;
        provenance.producingFacility = reserve.sourceFacilityId;
        if (reserve.stockClass.contains("counterfeit") || reserve.stockClass.contains("contaminated")) {
            provenance.defectState = reserve.stockClass;
            provenance.batchIssueTags = reserve.stockClass;
        } else if (reserve.legality.contains("restricted") || reserve.legality.contains("black-market")) {
            provenance.batchIssueTags = "restricted";
        }
        provenance.chain = reserve.route;
        offer.provenance = provenance;
        String note = " " + reserve.stockClass + " from " + reserve.sourceLabel + "; " + reserve.legality
                + "; risk " + reserve.riskClass + "; reserve " + reserve.remaining + "/" + reserve.capacity
                + "; " + reserve.routeRestriction + ".";
        if (offer.description == null) offer.description = note.trim();
        else if (!offer.description.contains("reserve " + reserve.remaining + "/" + reserve.capacity)) offer.description += note;
    }

    private static void ensureOffer(TraderSession trader, String item, String description) {
        if (item == null || item.isBlank() || ItemCatalog.get(item) == null) return;
        for (TradeOffer offer : trader.offers) if (offer != null && ItemQuality.namesMatch(offer.name, item)) return;
        ItemDef definition = ItemCatalog.get(item);
        trader.offers.add(new TradeOffer(item, definition.category, definition.basePrice, description));
    }

    private static void refresh(MedicalSupplyReserveRecord reserve, long worldTurn) {
        if (reserve == null || worldTurn < reserve.nextRestockWorldTurn) return;
        reserve.remaining = reserve.capacity;
        reserve.nextRestockWorldTurn = Math.max(0L, worldTurn) + reserve.restockIntervalTurns;
    }

    private static int medicalDemand(TraderSession trader) {
        if (trader == null || trader.populationPressure == null) return 0;
        return trader.populationPressure.pressureFor("Bandage roll", "medical").demandUnits();
    }

    private static boolean isMedicalOrDrug(TradeOffer offer) {
        if (offer == null) return false;
        ItemDef definition = ItemCatalog.get(offer.name);
        String low = (definition == null ? safe(offer.category) : safe(definition.category)).toLowerCase(Locale.ROOT);
        if (low.startsWith("chem/noble-luxury") || low.startsWith("chem/rare-campaign")) return false;
        return low.startsWith("medical") || low.startsWith("stimulant") || low.startsWith("chem/");
    }

    private static String classFor(String item, Profile profile) {
        String low = safe(item).toLowerCase(Locale.ROOT);
        if (contains(low, "stim", "medi-stimm")) return profile.legality().contains("black-market")
                ? "black-market performance drug" : "controlled performance medicine";
        if (contains(low, "sumpkalm", "mercy")) return profile.legality().contains("black-market")
                ? "black-market sedative" : "controlled sedative medicine";
        if (profile.policy().contains("private physician")) return "noble physician medicine";
        return "legal clinic medicine";
    }

    private static String riskFor(String item, String stockClass) {
        String low = (safe(item) + " " + safe(stockClass)).toLowerCase(Locale.ROOT);
        if (contains(low, "counterfeit", "contaminated")) return "counterfeit or contamination risk";
        if (contains(low, "stim", "performance")) return "stimulant strain and sleep-debt risk";
        if (contains(low, "sumpkalm", "sedative", "black-market")) return "dependency and contamination risk";
        return "ordinary treatment risk";
    }

    private static boolean hasMedicalSource(World world, Faction faction) {
        for (int i = 0; i < world.roomProfiles.size(); i++) {
            RoomProfile room = world.roomProfiles.get(i);
            Faction owner = i < world.roomFactions.size() ? world.roomFactions.get(i) : Faction.NONE;
            if (room != null && factionCompatible(safeFaction(faction), owner)
                    && medicalText((safe(room.name) + " " + safe(room.descriptor)).toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static boolean hasReliefDemand(World world) {
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            String text = (safe(ledger.sourceKind) + " " + safe(ledger.sourceLabel) + " " + safe(ledger.roomName)).toLowerCase(Locale.ROOT);
            if (contains(text, "relief", "refugee", "displaced", "evacuation")) return true;
        }
        return false;
    }

    private static boolean hasRailAccess(World world) {
        if (world.zoneType == ZoneType.NEUTRAL_RAIL_DEPOT || world.zoneType == ZoneType.TRAIN_SERVICE_YARD) return true;
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            String text = (safe(ledger.sourceKind) + " " + safe(ledger.sourceLabel) + " " + safe(ledger.roomName)).toLowerCase(Locale.ROOT);
            if (contains(text, "rail intake", "train", "rail hub")) return true;
        }
        return false;
    }

    private static boolean hasRestriction(World world, String... terms) {
        return contains(safe(world.zoneConflictLossHistory).toLowerCase(Locale.ROOT), terms);
    }
    private static boolean medicalText(String text) {
        return contains(text, "medical", "medicae", "clinic", "medicine", "pharma", "drug", "narcotic",
                "stimulant", "sedative", "antiseptic", "bandage", "medkit", "laboratory", "chem lab", "relief");
    }

    private static boolean controllerCompatible(Faction faction, String controller) {
        String low = safe(controller).toLowerCase(Locale.ROOT).trim();
        if (low.isBlank() || contains(low, "unknown", "unrecorded", "none", "neutral")) return true;
        Faction f = safeFaction(faction);
        String compact = compactIdentity(low);
        if (compact.equals(compactIdentity(f.name())) || compact.equals(compactIdentity(f.label))) return true;
        if ((f == Faction.MECHANICUS || f == Faction.MECHANIST_COLLEGIA || f.name().startsWith("MECHANICUS_"))
                && contains(low, "mechanicus", "mechanist")) return true;
        return (f == Faction.ARBITES || f == Faction.CIVIC_WARDENS) && contains(low, "arbites", "civic wardens");
    }

    private static boolean factionCompatible(Faction wanted, Faction owner) {
        if (owner == null || owner == Faction.NONE || wanted == Faction.NONE || owner == wanted) return true;
        String a = wanted.name().split("_")[0], b = owner.name().split("_")[0];
        if (a.equals(b)) return true;
        return ((wanted == Faction.ARBITES || wanted == Faction.CIVIC_WARDENS)
                && (owner == Faction.ARBITES || owner == Faction.CIVIC_WARDENS));
    }

    private static int days(int days) { return Math.max(1, days) * GamePanel.TURNS_PER_HOUR * GamePanel.HOURS_PER_DAY; }
    private static void appendSummary(TraderSession trader, String line) {
        if (line == null || line.isBlank()) return;
        String current = trader.supplyChainSummary == null ? "" : trader.supplyChainSummary;
        if (!current.contains(line)) trader.supplyChainSummary = current.isBlank() ? line : current + " " + line;
    }
    private static boolean contains(String text, String... needles) {
        if (text == null) return false;
        for (String needle : needles) if (needle != null && !needle.isBlank() && text.contains(needle)) return true;
        return false;
    }
    private static String compactIdentity(String value) { return safe(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""); }
    private static Faction safeFaction(Faction faction) { return faction == null ? Faction.NONE : faction; }
    private static String safe(String value) { return value == null ? "" : value; }
}
