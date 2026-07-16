package mechanist;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Finite, faction-specific weapon and ammunition stock with facility and event provenance. */
final class SecuritySupplyReserveRecord {
    String id = "security.unassigned";
    String itemName = "Stub cartridge box";
    Faction faction = Faction.NONE;
    String stockClass = "civilian ammunition";
    String legality = "locally regulated sale";
    String sourceKind = "faction store";
    String sourceLabel = "local security store";
    String sourceFacilityId = "";
    String route = "faction store -> trader security shelf";
    String eventRestriction = "no active route restriction";
    int capacity = 2;
    int remaining = 2;
    int restockIntervalTurns = GamePanel.TURNS_PER_HOUR * GamePanel.HOURS_PER_DAY * 4;
    long nextRestockWorldTurn = restockIntervalTurns;

    String saveLine() {
        return enc(id) + "|" + enc(itemName) + "|" + (faction == null ? Faction.NONE.name() : faction.name())
                + "|" + enc(stockClass) + "|" + enc(legality) + "|" + enc(sourceKind) + "|"
                + enc(sourceLabel) + "|" + enc(sourceFacilityId) + "|" + enc(route) + "|"
                + enc(eventRestriction) + "|" + capacity + "|" + remaining + "|"
                + restockIntervalTurns + "|" + nextRestockWorldTurn;
    }

    static SecuritySupplyReserveRecord parse(String line) {
        try {
            String[] a = line.split("\\|", 14);
            if (a.length < 14) return null;
            SecuritySupplyReserveRecord r = new SecuritySupplyReserveRecord();
            r.id = dec(a[0]);
            r.itemName = dec(a[1]);
            r.faction = Faction.valueOf(a[2]);
            r.stockClass = dec(a[3]);
            r.legality = dec(a[4]);
            r.sourceKind = dec(a[5]);
            r.sourceLabel = dec(a[6]);
            r.sourceFacilityId = dec(a[7]);
            r.route = dec(a[8]);
            r.eventRestriction = dec(a[9]);
            r.capacity = Math.max(1, Integer.parseInt(a[10]));
            r.remaining = Math.max(0, Math.min(r.capacity, Integer.parseInt(a[11])));
            r.restockIntervalTurns = Math.max(1, Integer.parseInt(a[12]));
            r.nextRestockWorldTurn = Math.max(0L, Long.parseLong(a[13]));
            return r;
        } catch (Exception ignored) {
            return null;
        }
    }

    String playerLine() {
        return itemName + " supply: " + stockClass + " from " + sourceLabel + "; " + legality + "; "
                + remaining + "/" + capacity + " unit(s); " + eventRestriction + "; "
                + (remaining > 0 ? "next source refill" : "depleted until refill")
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

final class SecuritySupplyProvenanceAuthority {
    record Profile(String doctrine, String representativeWeapon, String representativeAmmo,
                   String weaponClass, String ammoClass, String legality, boolean armedFaction) { }
    private record Source(String kind, String label, String facilityId, String route,
                          String stockClass, String restriction, int capacity, int restockTurns) { }

    private SecuritySupplyProvenanceAuthority() { }

    static Profile apply(TraderSession trader, World world, Faction faction, long worldTurn, int localTurn) {
        Profile profile = profileFor(faction);
        if (trader == null || world == null) return profile;
        int demand = securityDemand(trader);
        boolean infrastructure = hasSecuritySource(world, faction);
        boolean shipmentAccess = hasRailAccess(world);
        if (profile.armedFaction() || demand >= 4 || infrastructure || shipmentAccess) {
            ensureOffer(trader, profile.representativeAmmo(), "faction security ammunition selected by " + profile.doctrine() + ".");
        }
        if (profile.armedFaction() || demand >= 8 || infrastructure || shipmentAccess) {
            ensureOffer(trader, profile.representativeWeapon(), "faction security weapon selected by " + profile.doctrine() + ".");
        }

        ArrayList<TradeOffer> securityOffers = new ArrayList<>();
        for (TradeOffer offer : trader.offers) if (isSecurityItem(offer)) securityOffers.add(offer);
        int available = 0;
        int removed = 0;
        for (TradeOffer offer : securityOffers) {
            SecuritySupplyReserveRecord reserve = reserveFor(world, faction, offer.name);
            if (reserve == null) {
                reserve = create(world, faction, offer.name, profile, worldTurn);
                world.securitySupplyReserves.add(reserve);
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
        String context = "Security supply doctrine: " + profile.doctrine() + "; " + securityOffers.size()
                + " traced offer(s), " + available + " reserve unit(s) available"
                + (removed > 0 ? ", " + removed + " depleted offer(s) withheld" : "") + ".";
        trader.securitySupplyProfile = profile;
        appendSummary(trader, context);
        DebugLog.audit("SECURITY_SUPPLY_PROVENANCE", "faction=" + safeFaction(faction).label + " zone="
                + world.zoneType.label + " offers=" + securityOffers.size() + " available=" + available
                + " removed=" + removed + " doctrine=" + profile.doctrine());
        return profile;
    }

    static String purchaseBlock(World world, TradeOffer offer, long worldTurn) {
        if (offer == null || offer.securitySupplyReserveId == null || offer.securitySupplyReserveId.isBlank()) return "";
        SecuritySupplyReserveRecord reserve = reserveById(world, offer.securitySupplyReserveId);
        if (reserve == null) return "its security supply ledger is unavailable";
        refresh(reserve, worldTurn);
        return reserve.remaining > 0 ? "" : reserve.sourceLabel + " is depleted until world turn " + reserve.nextRestockWorldTurn;
    }

    static boolean consume(World world, TradeOffer offer, long worldTurn) {
        if (offer == null || offer.securitySupplyReserveId == null || offer.securitySupplyReserveId.isBlank()) return true;
        SecuritySupplyReserveRecord reserve = reserveById(world, offer.securitySupplyReserveId);
        if (reserve == null) return false;
        refresh(reserve, worldTurn);
        if (reserve.remaining <= 0) return false;
        reserve.remaining--;
        return true;
    }

    static void updateSessionAfterPurchase(TraderSession trader, World world, TradeOffer offer) {
        if (trader == null || offer == null || offer.securitySupplyReserveId == null
                || offer.securitySupplyReserveId.isBlank()) return;
        SecuritySupplyReserveRecord reserve = reserveById(world, offer.securitySupplyReserveId);
        if (reserve == null) return;
        appendSummary(trader, reserve.playerLine());
        if (reserve.remaining <= 0) trader.offers.removeIf(o -> o != null && reserve.id.equals(o.securitySupplyReserveId));
    }

    static SecuritySupplyReserveRecord reserveFor(World world, Faction faction, String item) {
        if (world == null || world.securitySupplyReserves == null) return null;
        Faction owner = safeFaction(faction);
        for (SecuritySupplyReserveRecord reserve : world.securitySupplyReserves) {
            if (reserve != null && reserve.faction == owner && ItemQuality.namesMatch(reserve.itemName, item)) return reserve;
        }
        return null;
    }

    static SecuritySupplyReserveRecord reserveById(World world, String id) {
        if (world == null || id == null || id.isBlank() || world.securitySupplyReserves == null) return null;
        for (SecuritySupplyReserveRecord reserve : world.securitySupplyReserves) {
            if (reserve != null && id.equals(reserve.id)) return reserve;
        }
        return null;
    }

    static Profile profileFor(Faction faction) {
        Faction f = safeFaction(faction);
        String name = f.name();
        if (f == Faction.IMPERIAL_GUARD || f == Faction.SORORITAS) {
            return new Profile("disciplined military issue", "Guard lascarbine", "Las charge pack",
                    "military weapon", "military ammunition", "restricted military issue", true);
        }
        if (f == Faction.ARBITES || f == Faction.CIVIC_WARDENS) {
            return new Profile("controlled civic security issue", "Shotgun", "Arbites suppression shells",
                    "security weapon", "restricted security ammunition", "controlled security issue", true);
        }
        if (f == Faction.MECHANICUS || f == Faction.MECHANIST_COLLEGIA || name.startsWith("MECHANICUS_")) {
            return new Profile("sealed Mechanist custody", "Arc Rifle", "Arc capacitor pack",
                    "restricted technical weapon", "restricted technical ammunition", "Mechanist custody only", true);
        }
        if (f == Faction.NOBLE || name.startsWith("NOBLE_")) {
            return new Profile("licensed household security", "Noble dueling pistol", "Dueling pistol cartridge box",
                    "noble private-security weapon", "luxury ammunition", "household license required", true);
        }
        if (f == Faction.BANDIT || name.startsWith("GANGER_")) {
            return new Profile("gang black-market arming", "Sawed-off stub shotgun", "Shot shell handful",
                    "black-market weapon", "black-market ammunition", "contraband", true);
        }
        if (f == Faction.CULTIST || f == Faction.HERETIC) {
            return new Profile("hidden-cell illicit arming", "Autopistol", "Autogun magazine",
                    "illicit cell weapon", "black-market ammunition", "contraband", true);
        }
        if (f == Faction.MUTANT) {
            return new Profile("sump improvised defense", "Pipe shotgun", "Shot shell handful",
                    "improvised weapon", "salvaged ammunition", "unlicensed local possession", true);
        }
        return new Profile("civilian defensive stock", "Stub pistol", "Stub cartridge box",
                "civilian weapon", "civilian ammunition", "locally regulated sale", false);
    }

    private static SecuritySupplyReserveRecord create(World world, Faction faction, String item, Profile profile,
                                                       long worldTurn) {
        Faction owner = safeFaction(faction);
        boolean weapon = isWeapon(item);
        Source source = resolveSource(world, owner, item, weapon, profile);
        SecuritySupplyReserveRecord reserve = new SecuritySupplyReserveRecord();
        reserve.id = "security." + Math.abs(Objects.hash(world.seed, owner.name(), item));
        reserve.itemName = item;
        reserve.faction = owner;
        reserve.stockClass = source.stockClass();
        reserve.legality = profile.legality();
        reserve.sourceKind = source.kind();
        reserve.sourceLabel = source.label();
        reserve.sourceFacilityId = source.facilityId();
        reserve.route = source.route();
        reserve.eventRestriction = source.restriction();
        reserve.capacity = source.capacity();
        reserve.remaining = source.capacity();
        reserve.restockIntervalTurns = source.restockTurns();
        reserve.nextRestockWorldTurn = Math.max(0L, worldTurn) + source.restockTurns();
        return reserve;
    }

    private static Source resolveSource(World world, Faction faction, String item, boolean weapon, Profile profile) {
        boolean blockaded = hasRestriction(world, "blockade", "interdiction", "route closure", "quarantine");
        for (ZoneConflictLossRecord event : HistoricalConflictLossApi.parseConflictLossLedger(world.zoneConflictLossHistory)) {
            String text = safe(event.eventType) + " " + safe(event.actor) + " " + safe(event.affectedStock)
                    + " " + safe(event.destination) + " " + safe(event.historyNote);
            String low = text.toLowerCase(Locale.ROOT);
            boolean relevant = securityText(low) || ItemQuality.namesMatch(event.affectedStock, item);
            if (!relevant) continue;
            if (contains(low, "seizure", "confiscation", "evidence")
                    && (faction == Faction.ARBITES || faction == Faction.CIVIC_WARDENS)) {
                return eventSource(event, item, weapon, "emergency confiscated stock", "emergency confiscated stock",
                        "controlled evidence release", 10);
            }
            if (contains(low, "theft", "black-market", "diversion", "smuggl")
                    && (faction == Faction.BANDIT || faction.name().startsWith("GANGER_")
                    || faction == Faction.CULTIST || faction == Faction.HERETIC)) {
                String kind = contains(low, "counterfeit", "defect") ? "counterfeit or defective stock" : "stolen black-market stock";
                return eventSource(event, item, weapon, kind, kind, "illicit event route", 20);
            }
            if (contains(low, "raid", "battle", "abandoned", "recovery", "loss")) {
                return eventSource(event, item, weapon, "battlefield recovery", "battlefield-recovered stock",
                        "irregular recovered supply", 14);
            }
        }

        for (ZoneProductionOutputRecord output : ProductionFacilityOutputSimulationApi.parseProductionLedger(world.zoneProductionHistory)) {
            String text = safe(output.facilityId) + " " + safe(output.facilityPurpose) + " "
                    + safe(output.outputFocus) + " " + safe(output.sampleItems);
            if (!securityText(text.toLowerCase(Locale.ROOT)) || !controllerCompatible(faction, output.controller)) continue;
            int capacity = weapon ? Math.max(1, Math.min(4, output.batches))
                    : Math.max(4, Math.min(16, Math.max(1, output.batches) * 3));
            String label = safe(output.facilityId).isBlank() ? output.facilityPurpose : output.facilityId;
            return new Source("faction arms production", label, safe(output.facilityId),
                    label + " -> faction armory -> trader security shelf",
                    profileClass(profile, weapon), "no active route restriction", capacity, days(2));
        }

        for (int i = 0; i < world.roomProfiles.size(); i++) {
            RoomProfile room = world.roomProfiles.get(i);
            Faction roomFaction = i < world.roomFactions.size() ? world.roomFactions.get(i) : Faction.NONE;
            if (room == null || !factionCompatible(faction, roomFaction)) continue;
            String text = (safe(room.name) + " " + safe(room.descriptor) + " " + safe(room.featureText)).toLowerCase(Locale.ROOT);
            if (!securityText(text)) continue;
            String kind = contains(text, "evidence", "contraband") ? "controlled evidence store" :
                    (contains(text, "workshop", "factory") ? "arms workshop" : "armory or munition store");
            String stockClass = profileClass(profile, weapon);
            if (kind.equals("controlled evidence store")) stockClass = "emergency confiscated stock";
            return new Source(kind, room.name, "room." + i, room.name + " -> issue counter -> trader security shelf",
                    stockClass, "no active route restriction", weapon ? 2 : 7, days(3));
        }

        if (!blockaded && hasRailAccess(world)) {
            return new Source("outside-sector arms shipment", "arcology arms freight train", "rail.arms.intake",
                    "outside-sector arms shipment -> rail intake -> faction store -> trader security shelf",
                    "outside-sector arms shipment", "route open", weapon ? 1 : 5, days(6));
        }
        String stockClass = blockaded ? "blockade-restricted stock" :
                (profile.armedFaction() ? "faction surplus" : profileClass(profile, weapon));
        String restriction = blockaded ? "outside shipments blocked; local reserve only" : "no active route restriction";
        return new Source("faction reserve store", faction.label + " reserve cage", "faction.security.reserve",
                faction.label + " reserve cage -> trader security shelf", stockClass, restriction,
                blockaded ? 1 : (weapon ? 1 : 3), days(blockaded ? 10 : 5));
    }

    private static Source eventSource(ZoneConflictLossRecord event, String item, boolean weapon, String kind,
                                      String stockClass, String restriction, int days) {
        int capacity = weapon ? 1 : 3;
        String label = safe(event.destination).isBlank() ? event.eventType : event.destination;
        return new Source(kind, label, safe(event.sourceFacilityId),
                safe(event.sourceFacilityId) + " -> " + event.eventType + " by " + event.actor + " -> "
                        + label + " -> trader security shelf", stockClass, restriction, capacity, days(days));
    }

    private static void attach(TradeOffer offer, SecuritySupplyReserveRecord reserve, World world,
                               Faction faction, int localTurn) {
        offer.securitySupplyReserveId = reserve.id;
        ItemProvenanceRecord provenance = ItemProvenanceRecord.of(offer.name, faction, reserve.sourceLabel, world,
                localTurn, reserve.stockClass + "; finite reserve " + reserve.remaining + "/" + reserve.capacity,
                reserve.route);
        provenance.productionLegalStatus = reserve.legality;
        provenance.productionSource = reserve.sourceKind;
        provenance.producingFacility = reserve.sourceFacilityId;
        if (reserve.stockClass.contains("counterfeit") || reserve.stockClass.contains("defective")) {
            provenance.defectState = "unverified counterfeit or defective security stock";
        }
        provenance.chain = reserve.route;
        offer.provenance = provenance;
        String note = " " + reserve.stockClass + " from " + reserve.sourceLabel + "; " + reserve.legality
                + "; reserve " + reserve.remaining + "/" + reserve.capacity + "; " + reserve.eventRestriction + ".";
        if (offer.description == null) offer.description = note.trim();
        else if (!offer.description.contains("reserve " + reserve.remaining + "/" + reserve.capacity)) offer.description += note;
    }

    private static void ensureOffer(TraderSession trader, String item, String description) {
        if (item == null || item.isBlank() || ItemCatalog.get(item) == null) return;
        for (TradeOffer offer : trader.offers) if (offer != null && ItemQuality.namesMatch(offer.name, item)) return;
        ItemDef definition = ItemCatalog.get(item);
        trader.offers.add(new TradeOffer(item, definition.category, Math.max(1, definition.basePrice), description));
    }

    private static void refresh(SecuritySupplyReserveRecord reserve, long worldTurn) {
        if (reserve == null || worldTurn < reserve.nextRestockWorldTurn) return;
        reserve.remaining = reserve.capacity;
        reserve.nextRestockWorldTurn = Math.max(0L, worldTurn) + reserve.restockIntervalTurns;
    }

    private static int securityDemand(TraderSession trader) {
        if (trader == null || trader.populationPressure == null) return 0;
        return trader.populationPressure.pressureFor("Stub cartridge box", "ammo").demandUnits();
    }

    private static boolean hasSecuritySource(World world, Faction faction) {
        if (world == null) return false;
        for (int i = 0; i < world.roomProfiles.size(); i++) {
            RoomProfile room = world.roomProfiles.get(i);
            Faction owner = i < world.roomFactions.size() ? world.roomFactions.get(i) : Faction.NONE;
            if (room != null && factionCompatible(safeFaction(faction), owner)
                    && securityText((safe(room.name) + " " + safe(room.descriptor)).toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static boolean isSecurityItem(TradeOffer offer) {
        if (offer == null) return false;
        ItemDef definition = ItemCatalog.get(offer.name);
        String category = definition == null ? safe(offer.category) : safe(definition.category);
        String low = category.toLowerCase(Locale.ROOT);
        return low.startsWith("weapon") || low.startsWith("ammo");
    }

    private static boolean isWeapon(String item) {
        ItemDef definition = ItemCatalog.get(item);
        return definition != null && safe(definition.category).toLowerCase(Locale.ROOT).startsWith("weapon");
    }

    private static String profileClass(Profile profile, boolean weapon) {
        return weapon ? profile.weaponClass() : profile.ammoClass();
    }

    private static boolean securityText(String text) {
        return contains(text, "weapon", "armory", "armoury", "ammo", "ammunition", "munition", "security",
                "military", "quartermaster", "gun", "firearm", "evidence", "contraband", "arsenal");
    }

    private static boolean hasRestriction(World world, String... terms) {
        String low = safe(world == null ? "" : world.zoneConflictLossHistory).toLowerCase(Locale.ROOT);
        return contains(low, terms);
    }

    private static boolean hasRailAccess(World world) {
        if (world.zoneType == ZoneType.NEUTRAL_RAIL_DEPOT || world.zoneType == ZoneType.TRAIN_SERVICE_YARD) return true;
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            String text = (safe(ledger.sourceKind) + " " + safe(ledger.sourceLabel) + " " + safe(ledger.roomName)).toLowerCase(Locale.ROOT);
            if (contains(text, "rail intake", "train", "rail hub")) return true;
        }
        return false;
    }

    private static boolean controllerCompatible(Faction faction, String controller) {
        String low = safe(controller).toLowerCase(Locale.ROOT).trim();
        if (low.isBlank() || contains(low, "unknown", "unrecorded", "none", "neutral")) return true;
        Faction f = safeFaction(faction);
        String name = f.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        String label = safe(f.label).toLowerCase(Locale.ROOT);
        String compact = compactIdentity(low);
        if (low.contains(name) || low.contains(label) || name.contains(low) || label.contains(low)
                || compact.equals(compactIdentity(f.name())) || compact.equals(compactIdentity(f.label))) return true;
        if ((f == Faction.MECHANICUS || f == Faction.MECHANIST_COLLEGIA || f.name().startsWith("MECHANICUS_"))
                && contains(low, "mechanicus", "mechanist")) return true;
        if ((f == Faction.ARBITES || f == Faction.CIVIC_WARDENS) && contains(low, "arbites", "civic wardens")) return true;
        return false;
    }

    private static boolean factionCompatible(Faction wanted, Faction owner) {
        if (owner == null || owner == Faction.NONE || wanted == Faction.NONE) return true;
        if (owner == wanted) return true;
        String a = wanted.name().split("_")[0];
        String b = owner.name().split("_")[0];
        if (a.equals(b)) return true;
        return ((wanted == Faction.ARBITES || wanted == Faction.CIVIC_WARDENS)
                && (owner == Faction.ARBITES || owner == Faction.CIVIC_WARDENS));
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
        if (text == null) return false;
        for (String needle : needles) if (needle != null && !needle.isBlank() && text.contains(needle)) return true;
        return false;
    }

    private static Faction safeFaction(Faction faction) { return faction == null ? Faction.NONE : faction; }
    private static String compactIdentity(String value) {
        return safe(value).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
    private static String safe(String value) { return value == null ? "" : value; }
}
