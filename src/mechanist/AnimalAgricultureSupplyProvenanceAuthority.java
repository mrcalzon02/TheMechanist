package mechanist;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;

/** Finite pet, animal, and agricultural stock tied to living actors and operated rooms. */
final class AnimalAgricultureSupplyReserveRecord {
    String id = "animal-agriculture.unassigned";
    String itemName = "Animal feed sack";
    Faction faction = Faction.NONE;
    String goodsClass = "managed animal supply";
    String sourceKind = "local animal room";
    String sourceLabel = "unassigned animal room";
    String sourceFacilityId = "";
    String animalId = "";
    String animalLabel = "no linked animal";
    String breederOrOwner = "unrecorded breeder or owner";
    String penOwner = "unrecorded pen owner";
    String handlerLabel = "no assigned handler";
    String feedSource = "unrecorded feed source";
    String waterSource = "unrecorded water station";
    String careSource = "unrecorded animal care";
    String eventPressure = "no active animal disease, feed shortage, or import restriction";
    String route = "animal room -> trader shelf";
    int capacity = 3;
    int remaining = 3;
    int restockIntervalTurns = GamePanel.TURNS_PER_HOUR * GamePanel.HOURS_PER_DAY * 4;
    long nextRestockWorldTurn = restockIntervalTurns;

    String saveLine() {
        return enc(id) + "|" + enc(itemName) + "|" + (faction == null ? Faction.NONE.name() : faction.name())
                + "|" + enc(goodsClass) + "|" + enc(sourceKind) + "|" + enc(sourceLabel) + "|"
                + enc(sourceFacilityId) + "|" + enc(animalId) + "|" + enc(animalLabel) + "|"
                + enc(breederOrOwner) + "|" + enc(penOwner) + "|" + enc(handlerLabel) + "|"
                + enc(feedSource) + "|" + enc(waterSource) + "|" + enc(careSource) + "|"
                + enc(eventPressure) + "|" + enc(route) + "|" + capacity + "|" + remaining + "|"
                + restockIntervalTurns + "|" + nextRestockWorldTurn;
    }

    static AnimalAgricultureSupplyReserveRecord parse(String line) {
        try {
            String[] a = line.split("\\|", 21);
            if (a.length < 21) return null;
            AnimalAgricultureSupplyReserveRecord r = new AnimalAgricultureSupplyReserveRecord();
            r.id = dec(a[0]); r.itemName = dec(a[1]); r.faction = Faction.valueOf(a[2]);
            r.goodsClass = dec(a[3]); r.sourceKind = dec(a[4]); r.sourceLabel = dec(a[5]);
            r.sourceFacilityId = dec(a[6]); r.animalId = dec(a[7]); r.animalLabel = dec(a[8]);
            r.breederOrOwner = dec(a[9]); r.penOwner = dec(a[10]); r.handlerLabel = dec(a[11]);
            r.feedSource = dec(a[12]); r.waterSource = dec(a[13]); r.careSource = dec(a[14]);
            r.eventPressure = dec(a[15]); r.route = dec(a[16]);
            r.capacity = Math.max(1, Integer.parseInt(a[17]));
            r.remaining = Math.max(0, Math.min(r.capacity, Integer.parseInt(a[18])));
            r.restockIntervalTurns = Math.max(1, Integer.parseInt(a[19]));
            r.nextRestockWorldTurn = Math.max(0L, Long.parseLong(a[20]));
            return r;
        } catch (Exception ignored) { return null; }
    }

    String playerLine() {
        return itemName + " supply: " + goodsClass + " from " + sourceLabel + "; animal " + animalLabel
                + "; breeder/owner " + breederOrOwner + "; pen owner " + penOwner + "; handler "
                + handlerLabel + "; feed " + feedSource + "; water " + waterSource + "; care " + careSource
                + "; " + remaining + "/" + capacity + " unit(s); " + eventPressure + "; "
                + (remaining > 0 ? "next refill" : "depleted until refill") + " at world turn "
                + nextRestockWorldTurn + ".";
    }

    private static String enc(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }
    private static String dec(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}

final class AnimalAgricultureSupplyProvenanceAuthority {
    private record Source(String kind, String label, String facilityId, String animalId, String animalLabel,
                          String breederOrOwner, String penOwner, String handler, String feed, String water,
                          String care, String goodsClass, String pressure, String route, int capacity,
                          int restockTurns) { }

    private AnimalAgricultureSupplyProvenanceAuthority() { }

    static void apply(TraderSession trader, World world, Faction faction, long worldTurn, int localTurn) {
        if (trader == null || world == null) return;
        Faction owner = safeFaction(faction);
        boolean farmAnimal = findAnimal(world, owner, "farm-animal") != null;
        boolean petAnimal = findAnimal(world, owner, "pet", "kennel-animal") != null;
        boolean agriculture = findRoom(world, owner, "farm", "garden", "hydro", "orchard", "fungus", "nursery", "algae") >= 0
                || hasProduction(world, owner, "farm", "garden", "hydro", "crop", "fungus", "animal", "agriculture");
        boolean cloning = findRoom(world, owner, "cloning", "gene lab", "genetic", "nursery") >= 0
                || hasProduction(world, owner, "cloning", "genetic", "gene stock");
        boolean rail = hasRailAccess(world);
        boolean importsClosed = importRestricted(world);

        if (agriculture) {
            ensureOffer(trader, "Seed culture tray", "finite seed stock from an operated agricultural source.");
            ensureOffer(trader, "Fungus starter mat", "finite fungal stock from an operated garden or farm.");
        }
        if (farmAnimal) {
            ensureOffer(trader, "Animal feed sack", "finite feed allocated to local living farm stock.");
            ensureOffer(trader, "Farm animal product crate", "finite output from local living farm stock.");
            ensureOffer(trader, "Veterinary care kit", "finite care stock for local animals and handlers.");
        }
        if (petAnimal) {
            ensureOffer(trader, "Pet care bundle", "finite supplies tied to a local pet or kennel animal.");
            ensureOffer(trader, "Veterinary care kit", "finite care stock for local animals and handlers.");
        }
        if (cloning || (rail && !importsClosed)) {
            ensureOffer(trader, "Cloning sample ampoule", "controlled living sample from a local lab or import route.");
        }
        if (!agriculture && rail && !importsClosed) {
            ensureOffer(trader, "Seed culture tray", "outside-sector agricultural seed shipment.");
        }
        if (rail && importsClosed) {
            appendSummary(trader, "Animal/agriculture imports restricted: rail seed, breeding, and cloning stock is closed; only local living stock and operated rooms may supply this market.");
        }

        ArrayList<TradeOffer> managed = new ArrayList<>();
        for (TradeOffer offer : trader.offers) if (isManagedGoods(offer)) managed.add(offer);
        int available = 0;
        int removed = 0;
        for (TradeOffer offer : managed) {
            AnimalAgricultureSupplyReserveRecord reserve = reserveFor(world, owner, offer.name);
            if (reserve == null) {
                Source source = resolveSource(world, owner, offer.name);
                if (source == null) {
                    trader.offers.remove(offer);
                    removed++;
                    continue;
                }
                reserve = create(world, owner, offer.name, source, worldTurn);
                world.animalAgricultureSupplyReserves.add(reserve);
            }
            refresh(reserve, worldTurn);
            if (reserve.remaining <= 0) {
                trader.offers.remove(offer);
                removed++;
                continue;
            }
            attach(offer, reserve, world, owner, localTurn);
            available += reserve.remaining;
            appendSummary(trader, reserve.playerLine());
        }
        if (!managed.isEmpty() || agriculture || farmAnimal || petAnimal || rail) {
            appendSummary(trader, "Animal/agriculture supply: " + managed.size() + " traced offer(s), "
                    + available + " reserve unit(s) available"
                    + (removed > 0 ? ", " + removed + " unavailable or depleted offer(s) withheld" : "") + ".");
        }
    }

    static String purchaseBlock(World world, TradeOffer offer, long worldTurn) {
        if (offer == null || offer.animalAgricultureReserveId == null || offer.animalAgricultureReserveId.isBlank()) return "";
        AnimalAgricultureSupplyReserveRecord reserve = reserveById(world, offer.animalAgricultureReserveId);
        if (reserve == null) return "its animal/agriculture supply ledger is unavailable";
        refresh(reserve, worldTurn);
        return reserve.remaining > 0 ? "" : reserve.sourceLabel + " is depleted until world turn " + reserve.nextRestockWorldTurn;
    }

    static boolean consume(World world, TradeOffer offer, long worldTurn) {
        if (offer == null || offer.animalAgricultureReserveId == null || offer.animalAgricultureReserveId.isBlank()) return true;
        AnimalAgricultureSupplyReserveRecord reserve = reserveById(world, offer.animalAgricultureReserveId);
        if (reserve == null) return false;
        refresh(reserve, worldTurn);
        if (reserve.remaining <= 0) return false;
        reserve.remaining--;
        return true;
    }

    static void updateSessionAfterPurchase(TraderSession trader, World world, TradeOffer offer) {
        if (trader == null || offer == null || offer.animalAgricultureReserveId == null
                || offer.animalAgricultureReserveId.isBlank()) return;
        AnimalAgricultureSupplyReserveRecord reserve = reserveById(world, offer.animalAgricultureReserveId);
        if (reserve == null) return;
        appendSummary(trader, reserve.playerLine());
        if (reserve.remaining <= 0) trader.offers.removeIf(o -> o != null && reserve.id.equals(o.animalAgricultureReserveId));
    }

    static AnimalAgricultureSupplyReserveRecord reserveFor(World world, Faction faction, String item) {
        if (world == null || world.animalAgricultureSupplyReserves == null) return null;
        Faction owner = safeFaction(faction);
        for (AnimalAgricultureSupplyReserveRecord reserve : world.animalAgricultureSupplyReserves) {
            if (reserve != null && reserve.faction == owner && ItemQuality.namesMatch(reserve.itemName, item)) return reserve;
        }
        return null;
    }

    static AnimalAgricultureSupplyReserveRecord reserveById(World world, String id) {
        if (world == null || id == null || id.isBlank() || world.animalAgricultureSupplyReserves == null) return null;
        for (AnimalAgricultureSupplyReserveRecord reserve : world.animalAgricultureSupplyReserves) {
            if (reserve != null && id.equals(reserve.id)) return reserve;
        }
        return null;
    }

    private static AnimalAgricultureSupplyReserveRecord create(World world, Faction faction, String item,
                                                                 Source source, long worldTurn) {
        AnimalAgricultureSupplyReserveRecord reserve = new AnimalAgricultureSupplyReserveRecord();
        reserve.id = "animal-agriculture." + Math.abs(Objects.hash(world.seed, faction.name(), item));
        reserve.itemName = item; reserve.faction = faction; reserve.goodsClass = source.goodsClass();
        reserve.sourceKind = source.kind(); reserve.sourceLabel = source.label();
        reserve.sourceFacilityId = source.facilityId(); reserve.animalId = source.animalId();
        reserve.animalLabel = source.animalLabel(); reserve.breederOrOwner = source.breederOrOwner();
        reserve.penOwner = source.penOwner(); reserve.handlerLabel = source.handler();
        reserve.feedSource = source.feed(); reserve.waterSource = source.water(); reserve.careSource = source.care();
        reserve.eventPressure = source.pressure(); reserve.route = source.route();
        reserve.capacity = source.capacity(); reserve.remaining = source.capacity();
        reserve.restockIntervalTurns = source.restockTurns();
        reserve.nextRestockWorldTurn = Math.max(0L, worldTurn) + source.restockTurns();
        return reserve;
    }

    private static Source resolveSource(World world, Faction faction, String item) {
        String low = ItemQuality.stripQuality(item).toLowerCase(Locale.ROOT);
        boolean animalProduct = low.contains("animal product");
        boolean petCare = low.contains("pet care");
        boolean veterinary = low.contains("veterinary");
        boolean feed = low.contains("animal feed");
        boolean seed = low.contains("seed culture");
        boolean fungus = low.contains("fungus starter");
        boolean cloning = low.contains("cloning sample");
        String history = safe(world.zoneConflictLossHistory).toLowerCase(Locale.ROOT);
        boolean disease = contains(history, "animal disease", "livestock disease", "animal outbreak", "herd sickness");
        boolean feedShortage = contains(history, "feed shortage", "fodder shortage", "animal feed shortage");
        boolean importsClosed = importRestricted(world);
        String pressure = pressureLine(disease, feedShortage, importsClosed);

        NpcEntity animal = animalProduct || feed || veterinary
                ? findAnimal(world, faction, "farm-animal", "kennel-animal")
                : (petCare ? findAnimal(world, faction, "pet", "kennel-animal") : null);
        if (animal != null) {
            int roomId = world.roomIdAt(animal.x, animal.y);
            String room = animalRoomLabel(world, animal, roomId);
            String handler = handlerLabel(world, faction);
            String feedSource = roomLabel(world, faction, "feed", "fodder", "farm", "food store", "hydro", "garden");
            String waterSource = roomLabel(world, faction, "water station", "water store", "cistern", "pump", "recycler", "wash");
            String careSource = roomLabel(world, faction, "veterinary", "animal care", "clinic", "kennel", "nursery");
            String owner = animal.provenance == null || safe(animal.provenance.populationPool).isBlank()
                    ? faction.label + " breeder ledger" : animal.provenance.populationPool;
            String penOwner = faction.label + " room owner";
            String goodsClass = animalProduct ? (disease ? "disease-screened animal product" : "local animal product")
                    : feed ? (feedShortage ? "rationed animal feed" : "allocated animal feed")
                    : petCare ? "owned pet supply" : "handler veterinary supply";
            int capacity = animalProduct ? 4 : (petCare ? 3 : 5);
            if (disease && (animalProduct || veterinary)) capacity = Math.min(capacity, 1);
            if (feedShortage && (animalProduct || feed)) capacity = Math.min(capacity, 1);
            String facilityId = animal.provenance == null ? "animal." + animal.id : safe(animal.provenance.originSiteId);
            String route = animal.name + " at " + room + " -> " + handler + " -> " + item + " shelf";
            return new Source("living animal and handler supply", room, facilityId, safe(animal.id), animal.name,
                    owner, penOwner, handler, feedSource, waterSource, careSource, goodsClass, pressure, route,
                    Math.max(1, capacity), days(animalProduct ? 3 : 5));
        }

        for (ZoneProductionOutputRecord output : ProductionFacilityOutputSimulationApi.parseProductionLedger(world.zoneProductionHistory)) {
            String text = (safe(output.facilityId) + " " + safe(output.facilityPurpose) + " "
                    + safe(output.outputFocus) + " " + safe(output.sampleItems)).toLowerCase(Locale.ROOT);
            if (!agricultureText(text) || !controllerCompatible(faction, output.controller)) continue;
            if (cloning && !contains(text, "clon", "gene", "genetic", "sample")) continue;
            String label = safe(output.facilityId).isBlank() ? output.facilityPurpose : output.facilityId;
            int capacity = Math.max(2, Math.min(12, Math.max(1, output.batches) * 2));
            if ((feedShortage && feed) || (disease && animalProduct)) capacity = 1;
            return new Source("faction agricultural production", label, safe(output.facilityId), "",
                    "production batch", faction.label + " production owner", faction.label + " facility owner",
                    handlerLabel(world, faction), roomLabel(world, faction, "feed", "fodder", "farm", "hydro"),
                    roomLabel(world, faction, "water", "cistern", "pump", "recycler"),
                    roomLabel(world, faction, "veterinary", "animal care", "nursery", "clinic"),
                    cloning ? "controlled cloning sample" : (fungus ? "cultivated fungal stock" : "local agricultural stock"),
                    pressure, label + " -> faction agricultural store -> " + item + " shelf", capacity, days(3));
        }

        int roomId = findRoomForItem(world, faction, seed, fungus, cloning, feed, petCare || veterinary);
        if (roomId >= 0) {
            RoomProfile room = world.roomProfiles.get(roomId);
            String goodsClass = cloning ? "controlled cloning sample" : fungus ? "cultivated fungal stock"
                    : seed ? "locally maintained seed stock" : feed ? "room-stored animal feed"
                    : petCare ? "room-stored pet supply" : "room-stored veterinary supply";
            int capacity = (disease || feedShortage) && (feed || veterinary) ? 1 : 4;
            return new Source("faction animal or agricultural room", room.name, "room." + roomId, "",
                    "room-managed stock", faction.label + " breeder or grower", faction.label + " room owner",
                    handlerLabel(world, faction), roomLabel(world, faction, "feed", "fodder", "farm", "food store", "hydro"),
                    roomLabel(world, faction, "water", "cistern", "pump", "recycler", "wash"),
                    roomLabel(world, faction, "veterinary", "animal care", "clinic", "kennel", "nursery"),
                    goodsClass, pressure, room.name + " -> faction stockroom -> " + item + " shelf",
                    Math.max(1, capacity), days(4));
        }

        if (!importsClosed && hasRailAccess(world) && (seed || fungus || cloning)) {
            return new Source("outside-sector agricultural import", "arcology agricultural freight train",
                    "rail.agriculture.intake", "", "imported living stock", "outside-sector breeder/exporter",
                    faction.label + " import custodian", handlerLabel(world, faction), "freight feed reserve",
                    "rail freight water station", "import inspection station",
                    cloning ? "controlled imported cloning sample" : "outside-sector agricultural stock", pressure,
                    "outside-sector grower -> rail intake -> import inspection -> " + item + " shelf",
                    cloning ? 2 : 5, days(cloning ? 12 : 8));
        }
        return null;
    }

    private static void attach(TradeOffer offer, AnimalAgricultureSupplyReserveRecord reserve, World world,
                               Faction faction, int localTurn) {
        offer.animalAgricultureReserveId = reserve.id;
        ItemProvenanceRecord provenance = ItemProvenanceRecord.of(offer.name, faction, reserve.sourceLabel, world,
                localTurn, reserve.goodsClass + "; finite reserve " + reserve.remaining + "/" + reserve.capacity,
                reserve.route);
        provenance.productionSource = reserve.sourceKind;
        provenance.producingFacility = reserve.sourceFacilityId;
        provenance.batchIssueTags = reserve.eventPressure;
        provenance.chain = reserve.route + "; animal=" + reserve.animalLabel + "; breeder/owner="
                + reserve.breederOrOwner + "; handler=" + reserve.handlerLabel + "; feed=" + reserve.feedSource
                + "; water=" + reserve.waterSource + "; care=" + reserve.careSource;
        offer.provenance = provenance;
        String note = " " + reserve.goodsClass + " from " + reserve.sourceLabel + "; animal "
                + reserve.animalLabel + "; handler " + reserve.handlerLabel + "; reserve "
                + reserve.remaining + "/" + reserve.capacity + "; " + reserve.eventPressure + ".";
        if (offer.description == null) offer.description = note.trim();
        else if (!offer.description.contains("reserve " + reserve.remaining + "/" + reserve.capacity)) offer.description += note;
    }

    private static int findRoomForItem(World world, Faction faction, boolean seed, boolean fungus, boolean cloning,
                                       boolean feed, boolean care) {
        if (cloning) return findRoom(world, faction, "cloning", "gene lab", "genetic", "nursery");
        if (fungus) return findRoom(world, faction, "fungus", "sump farm", "garden", "hydro", "agriculture");
        if (seed) return findRoom(world, faction, "farm", "garden", "hydro", "orchard", "nursery", "agriculture");
        if (feed) return findRoom(world, faction, "feed", "fodder", "farm", "food store", "hydro");
        if (care) return findRoom(world, faction, "veterinary", "animal care", "kennel", "pet", "nursery", "clinic");
        return -1;
    }

    private static NpcEntity findAnimal(World world, Faction faction, String... kinds) {
        if (world == null) return null;
        for (NpcEntity npc : world.npcs) {
            if (npc == null || npc.hp <= 0 || !npc.isAnimalActor() || !factionCompatible(faction, npc.faction)) continue;
            for (String kind : kinds) if (kind.equals(npc.creatureKind)) return npc;
        }
        return null;
    }

    private static String handlerLabel(World world, Faction faction) {
        if (world != null) for (NpcEntity npc : world.npcs) {
            if (npc == null || npc.hp <= 0 || npc.isAnimalActor() || !factionCompatible(faction, npc.faction)) continue;
            String text = (safe(npc.role) + " " + safe(npc.name)).toLowerCase(Locale.ROOT);
            if (contains(text, "animal handler", "handler", "breeder", "farmer", "gardener", "kennel", "veterinar")) {
                return npc.name + " / " + npc.role;
            }
        }
        return "no assigned handler";
    }

    private static int findRoom(World world, Faction faction, String... terms) {
        if (world == null) return -1;
        for (int i = 0; i < world.roomProfiles.size(); i++) {
            RoomProfile room = world.roomProfiles.get(i);
            Faction owner = i < world.roomFactions.size() ? world.roomFactions.get(i) : Faction.NONE;
            if (room == null || !factionCompatible(faction, owner)) continue;
            String text = (safe(room.name) + " " + safe(room.descriptor) + " " + safe(room.featureText)).toLowerCase(Locale.ROOT);
            if (contains(text, terms)) return i;
        }
        return -1;
    }

    private static String roomLabel(World world, Faction faction, String... terms) {
        int roomId = findRoom(world, faction, terms);
        return roomId < 0 ? "unrecorded local source" : world.roomProfiles.get(roomId).name;
    }

    private static String animalRoomLabel(World world, NpcEntity animal, int roomId) {
        if (roomId >= 0 && roomId < world.roomProfiles.size() && world.roomProfiles.get(roomId) != null) {
            return world.roomProfiles.get(roomId).name;
        }
        if (animal.provenance != null && !safe(animal.provenance.originRoom).isBlank()) return animal.provenance.originRoom;
        return animal.name + " holding room";
    }

    private static boolean hasProduction(World world, Faction faction, String... terms) {
        for (ZoneProductionOutputRecord output : ProductionFacilityOutputSimulationApi.parseProductionLedger(world.zoneProductionHistory)) {
            String text = (safe(output.facilityId) + " " + safe(output.facilityPurpose) + " "
                    + safe(output.outputFocus) + " " + safe(output.sampleItems)).toLowerCase(Locale.ROOT);
            if (contains(text, terms) && controllerCompatible(faction, output.controller)) return true;
        }
        return false;
    }

    private static boolean hasRailAccess(World world) {
        if (world.zoneType == ZoneType.NEUTRAL_RAIL_DEPOT || world.zoneType == ZoneType.TRAIN_SERVICE_YARD) return true;
        return findRoom(world, Faction.NONE, "rail intake", "freight platform", "train", "rail hub") >= 0;
    }

    private static boolean importRestricted(World world) {
        String text = safe(world == null ? "" : world.zoneConflictLossHistory).toLowerCase(Locale.ROOT);
        return contains(text, "import restriction", "import ban", "blockade", "interdiction", "route closure", "freight closure");
    }

    private static String pressureLine(boolean disease, boolean feedShortage, boolean importsClosed) {
        ArrayList<String> parts = new ArrayList<>();
        if (disease) parts.add("animal disease screening reduces output");
        if (feedShortage) parts.add("feed shortage rations output");
        if (importsClosed) parts.add("animal/agriculture imports restricted");
        return parts.isEmpty() ? "no active animal disease, feed shortage, or import restriction" : String.join("; ", parts);
    }

    private static boolean agricultureText(String text) {
        return contains(text, "farm", "garden", "hydro", "crop", "fungus", "animal", "agriculture", "seed", "clon", "gene");
    }

    private static boolean controllerCompatible(Faction faction, String controller) {
        String low = safe(controller).toLowerCase(Locale.ROOT).trim();
        if (low.isBlank() || contains(low, "unknown", "unrecorded", "none", "neutral")) return true;
        Faction f = safeFaction(faction);
        String compact = compactIdentity(low);
        if (compact.equals(compactIdentity(f.name())) || compact.equals(compactIdentity(f.label))) return true;
        return (f == Faction.MECHANICUS || f == Faction.MECHANIST_COLLEGIA || f.name().startsWith("MECHANICUS_"))
                && contains(low, "mechanicus", "mechanist");
    }

    private static boolean factionCompatible(Faction wanted, Faction owner) {
        if (owner == null || owner == Faction.NONE || wanted == Faction.NONE || owner == wanted) return true;
        String a = wanted.name().split("_")[0], b = owner.name().split("_")[0];
        return a.equals(b);
    }

    private static void ensureOffer(TraderSession trader, String item, String description) {
        ItemDef definition = ItemCatalog.get(item);
        if (definition == null) return;
        for (TradeOffer offer : trader.offers) if (offer != null && ItemQuality.namesMatch(offer.name, item)) return;
        trader.offers.add(new TradeOffer(item, definition.category, definition.basePrice, description));
    }

    private static boolean isManagedGoods(TradeOffer offer) {
        if (offer == null) return false;
        String name = ItemQuality.stripQuality(offer.name).toLowerCase(Locale.ROOT);
        return name.equals("animal feed sack") || name.equals("farm animal product crate")
                || name.equals("pet care bundle") || name.equals("veterinary care kit")
                || name.equals("cloning sample ampoule") || name.equals("seed culture tray")
                || name.equals("fungus starter mat");
    }

    private static void refresh(AnimalAgricultureSupplyReserveRecord reserve, long worldTurn) {
        if (reserve == null || worldTurn < reserve.nextRestockWorldTurn) return;
        reserve.remaining = reserve.capacity;
        reserve.nextRestockWorldTurn = Math.max(0L, worldTurn) + reserve.restockIntervalTurns;
    }

    private static void appendSummary(TraderSession trader, String line) {
        if (trader == null || line == null || line.isBlank()) return;
        if (trader.supplyChainSummary == null || trader.supplyChainSummary.isBlank()) trader.supplyChainSummary = line;
        else if (!trader.supplyChainSummary.contains(line)) trader.supplyChainSummary += " | " + line;
    }

    private static Faction safeFaction(Faction faction) { return faction == null ? Faction.NONE : faction; }
    private static int days(int days) { return GamePanel.TURNS_PER_HOUR * GamePanel.HOURS_PER_DAY * Math.max(1, days); }
    private static String safe(String value) { return value == null ? "" : value; }
    private static boolean contains(String text, String... terms) {
        String low = safe(text).toLowerCase(Locale.ROOT);
        for (String term : terms) if (term != null && !term.isBlank() && low.contains(term.toLowerCase(Locale.ROOT))) return true;
        return false;
    }
    private static String compactIdentity(String text) { return safe(text).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""); }
}
