package mechanist;

import java.awt.Rectangle;
import java.util.Random;

/** End-to-end smoke for faction facilities, staffing, vendors, and specialist stock. */
final class Milestone04FactionFacilityVendorGenerationSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        require(ItemCatalog.get("Noble preserved delicacy") != null
                        && ItemCatalog.priceFor("Noble preserved delicacy") == 28,
                "canonical item names beginning with Noble must not be parsed as quality prefixes");
        testIllicitFacilityAndMarket();
        testNobleVaultAndLuxuryBroker();
        testAgricultureAndAnimalMarket();
        testGuardCriticalVendors();
        testMechanistBlueprintVendor();
        System.out.println("Milestone 04 faction facility and vendor generation smoke passed.");
    }

    private static void testIllicitFacilityAndMarket() {
        World world = world(16101L, ZoneType.GANGER_TURF);
        addRoom(world, "Gang Work Machinery", "a guarded back-room workshop", Faction.BANDIT);
        addRoom(world, "Fence Storefront", "a stolen-goods counter and whisper market", Faction.BANDIT);
        addRoom(world, "Stolen Goods Warehouse", "a concealed gang storehouse", Faction.BANDIT);

        FactionFacilityPlacementAuthority.Result facilities = FactionFacilityPlacementAuthority.apply(world, new Random(1));
        require(facilities.illicitFacilities() == 1, "gang territory should promote one illicit chem facility");
        int labRoom = taggedRoom(world, FactionFacilityPlacementAuthority.ILLICIT_TAG);
        require(labRoom > 0 && world.roomProfiles.get(labRoom).name.contains("Illicit Chem Kitchen"),
                "illicit facility should be a visible promoted room");
        requireContains(world.zoneFacilityHistory, "illicit narcotics production and packaging", "illicit facility ledger");
        requireContains(world.zoneProductionHistory, "Street Stimm, Grin Powder, Night Milk", "illicit production ledger");
        placeFixtures(world);
        require(roomHasFixture(world, labRoom, "lab"), "illicit chem room should receive a chemical-lab fixture");

        FactionFacilityPlacementAuthority.staffPromotedFacilities(world, new Random(2));
        require(role(world, "Chem Cook") != null, "illicit chem room should receive a working chem cook");
        FactionCriticalVendorPlacementAuthority.Result vendors = FactionCriticalVendorPlacementAuthority.apply(world, new Random(3));
        require(vendors.vendorsPlaced() >= 2, "gang territory should receive provisions and black-market access");
        NpcEntity dealer = role(world, "Black-Market Trader");
        require(dealer != null && dealer.isTrader(), "black-market access should be a physical trader NPC");
        TraderSession session = TraderTradeActionAuthority.createSessionForNpc(dealer, world.zoneType, new Random(4));
        requireOffer(session, "Pipe shotgun");
        requireOffer(session, "Street Stimm");
        requireOffer(session, "Lockpicks");
        require(factionMarketCount(world) >= 2, "placed faction vendors should have physical market markers");

        NpcEntity restored = NpcEntity.parseLine(dealer.saveLine(), world);
        require(restored != null && "Black-Market Trader".equals(restored.role) && restored.isTrader(),
                "specialist trader role should survive NPC persistence");
        require(FactionCriticalVendorPlacementAuthority.apply(world, new Random(5)).vendorsPlaced() == 0,
                "reapplying vendor placement must not duplicate established categories");
        require(FactionFacilityPlacementAuthority.apply(world, new Random(6)).total() == 0,
                "reapplying facility promotion must not duplicate tagged facilities");
    }

    private static void testNobleVaultAndLuxuryBroker() {
        World world = world(16102L, ZoneType.SECTOR_GOVERNORS_MANSION);
        addRoom(world, "House Varn Product Warehouse", "a guarded estate warehouse", Faction.NOBLE_HOUSE_VARN);
        addRoom(world, "Private Storefront Salon", "an estate luxury receiving counter", Faction.NOBLE_HOUSE_VARN);
        addRoom(world, "House Medicae Suite", "a private physician room", Faction.NOBLE_HOUSE_VARN);
        addRoom(world, "Noble Kitchen", "a household provisioning kitchen", Faction.NOBLE_HOUSE_VARN);

        FactionFacilityPlacementAuthority.Result facilities = FactionFacilityPlacementAuthority.apply(world, new Random(7));
        require(facilities.nobleVaults() == 1, "noble territory should promote one protected luxury vault");
        int vaultRoom = taggedRoom(world, FactionFacilityPlacementAuthority.NOBLE_TAG);
        require(vaultRoom > 0 && world.roomProfiles.get(vaultRoom).name.contains("Luxury and Draught Vault"),
                "noble vault should be named and physically represented");
        placeFixtures(world);
        require(roomHasFixture(world, vaultRoom, "noble"), "noble vault should receive an estate-security fixture");

        TraderSession custodyCounter = new TraderSession();
        custodyCounter.name = "House Varn Custody Counter";
        custodyCounter.archetype = "luxury trader";
        custodyCounter.zoneLabel = world.zoneType.label;
        custodyCounter.offers.add(new TradeOffer("Black Sun Draught", "chem/rare-campaign", 85,
                "generic stock that protected custody must withhold."));
        NobleLuxuryProvenanceAuthority.apply(custodyCounter, world, Faction.NOBLE_HOUSE_VARN, 1L, 1);
        DraughtCustodyRecord custody = firstCustody(world);
        require(custody != null && custody.vaultLabel.contains("Luxury and Draught Vault") && !custody.releasedForSale,
                "promoted noble vault should become the exact protected draught custody site");
        require(custodyCounter.offers.stream().noneMatch(o -> "Black Sun Draught".equals(o.name)),
                "protected draught custody must not become ordinary broker stock");

        FactionFacilityPlacementAuthority.staffPromotedFacilities(world, new Random(8));
        require(role(world, "Vault Guard") != null, "protected vault should receive an estate guard");
        FactionCriticalVendorPlacementAuthority.apply(world, new Random(9));
        NpcEntity broker = role(world, "Luxury Broker Trader");
        require(broker != null, "noble estate should receive a luxury broker");
        TraderSession brokerStock = TraderTradeActionAuthority.createSessionForNpc(broker, world.zoneType, new Random(10));
        requireOffer(brokerStock, "Noble preserved delicacy");
        requireOffer(brokerStock, "Pearl Obscura");
        require(brokerStock.offers.stream().noneMatch(o -> "Black Sun Draught".equals(o.name)),
                "luxury broker remit must not bypass protected draught custody");
    }

    private static void testAgricultureAndAnimalMarket() {
        World world = world(16103L, ZoneType.HAB_STACK);
        addRoom(world, "Civic Product Warehouse", "a household goods storehouse", Faction.HIVER);
        addRoom(world, "Barter Storefront", "a cooperative neighborhood counter", Faction.HIVER);
        addRoom(world, "Block Clinic", "a crowded local medical room", Faction.HIVER);
        addRoom(world, "Communal Kitchen", "a food and water service room", Faction.HIVER);

        FactionFacilityPlacementAuthority.Result facilities = FactionFacilityPlacementAuthority.apply(world, new Random(11));
        require(facilities.agricultureFacilities() == 1, "hiver territory should promote one agriculture and animal-care facility");
        int farmRoom = taggedRoom(world, FactionFacilityPlacementAuthority.AGRICULTURE_TAG);
        require(farmRoom > 0 && world.roomProfiles.get(farmRoom).name.contains("Hydroponic Farm"),
                "agriculture support should be a visible controlled room");
        placeFixtures(world);
        require(roomHasFixture(world, farmRoom, "food-bio"), "agriculture room should receive a food or bio-production fixture");

        FactionFacilityPlacementAuthority.staffPromotedFacilities(world, new Random(12));
        require(role(world, "Animal Handler") != null, "agriculture room should receive an animal handler");
        FactionCriticalVendorPlacementAuthority.apply(world, new Random(13));
        NpcEntity animalVendor = role(world, "Animal Supply Trader");
        require(animalVendor != null, "supported agriculture should create a physical animal-supply vendor");
        TraderSession stock = TraderTradeActionAuthority.createSessionForNpc(animalVendor, world.zoneType, new Random(14));
        requireOffer(stock, "Animal feed sack");
        requireOffer(stock, "Pet care bundle");
        requireOffer(stock, "Veterinary care kit");
    }

    private static void testGuardCriticalVendors() {
        World world = world(16104L, ZoneType.IMPERIAL_GUARD_BILLET);
        addRoom(world, "Billet Mess", "a ration and water counter", Faction.IMPERIAL_GUARD);
        addRoom(world, "Regimental Armory", "a guarded munition warehouse", Faction.IMPERIAL_GUARD);
        addRoom(world, "Field Medicae", "a military treatment room", Faction.IMPERIAL_GUARD);
        addRoom(world, "Quartermaster Store", "a controlled issue room", Faction.IMPERIAL_GUARD);

        FactionCriticalVendorPlacementAuthority.Result result = FactionCriticalVendorPlacementAuthority.apply(world, new Random(15));
        require(result.vendorsPlaced() == 3, "Guard territory should receive provisions, armory, and medical vendors");
        NpcEntity quartermaster = role(world, "Armory Trader");
        require(quartermaster != null, "Guard armory should receive a quartermaster trader");
        TraderSession stock = TraderTradeActionAuthority.createSessionForNpc(quartermaster, world.zoneType, new Random(16));
        requireOffer(stock, "Light Rifle");
        requireOffer(stock, "Las charge pack");
        requireOffer(stock, "Guard flak vest");
    }

    private static void testMechanistBlueprintVendor() {
        World world = world(16105L, ZoneType.MECHANICUS_FORGE_CLOISTER);
        addRoom(world, "Machine Workshop", "an operated fabrication room", Faction.MECHANIST_COLLEGIA);
        addRoom(world, "Component Warehouse", "technical stock and repair parts", Faction.MECHANIST_COLLEGIA);
        addRoom(world, "Works Counter", "a licensed industrial storefront", Faction.MECHANIST_COLLEGIA);
        addRoom(world, "Forge Medicae", "a calibrated treatment alcove", Faction.MECHANIST_COLLEGIA);

        FactionCriticalVendorPlacementAuthority.apply(world, new Random(17));
        NpcEntity factor = role(world, "Industrial Blueprint Trader");
        require(factor != null, "Mechanist territory should receive an industrial blueprint trader");
        TraderSession stock = TraderTradeActionAuthority.createSessionForNpc(factor, world.zoneType, new Random(18));
        requireOffer(stock, "Tool bundle");
        requireOffer(stock, "Construction supplies");
        requireOffer(stock, "Room blueprint folio");
        requireOffer(stock, "Machine blueprint slate");
        requireOffer(stock, "Vehicle service component crate");
    }

    private static World world(long seed, ZoneType zone) {
        World world = new World(seed, 88, 72);
        world.zoneType = zone;
        for (int x = 0; x < world.w; x++) for (int y = 0; y < world.h; y++) world.tiles[x][y] = '#';
        addRoom(world, "Central Plaza", "neutral transit plaza", Faction.NONE);
        return world;
    }

    private static int addRoom(World world, String name, String description, Faction faction) {
        int index = world.rooms.size();
        int slot = Math.max(0, index - 1);
        Rectangle room = new Rectangle(4 + (slot % 4) * 19, 4 + (slot / 4) * 15, 14, 10);
        world.carve(room);
        world.rooms.add(room);
        world.roomProfiles.set(index, new RoomProfile(name, description, 60, faction,
                new String[]{"Trade chit"}, new char[]{'Q'}));
        world.roomFactions.set(index, faction);
        world.roomSpecials.set(index, Boolean.FALSE);
        for (int x = room.x + 1; x < room.x + room.width - 1; x++) {
            for (int y = room.y + 1; y < room.y + room.height - 1; y++) world.tiles[x][y] = '.';
        }
        return index;
    }

    private static void placeFixtures(World world) {
        RoomFixtureInteractionAuthority.apply(world, new Random(19));
        FactionFacilityPlacementAuthority.ensureFacilityFixtures(world, new Random(20));
    }

    private static int taggedRoom(World world, String tag) {
        for (int i = 1; i < world.roomProfiles.size(); i++) {
            RoomProfile profile = world.roomProfiles.get(i);
            String text = (profile.name == null ? "" : profile.name) + " "
                    + (profile.descriptor == null ? "" : profile.descriptor) + " "
                    + (profile.featureText == null ? "" : profile.featureText);
            if (text.contains(tag)) return i;
        }
        return -1;
    }

    private static boolean roomHasFixture(World world, int roomId, String family) {
        for (MapObjectState object : world.mapObjects) {
            if (object == null || !world.inBounds(object.x, object.y) || world.roomIds[object.x][object.y] != roomId) continue;
            if ("lab".equals(family) && LabChemicalFixtureAuthority.isFamilyType(object.type)) return true;
            if ("noble".equals(family) && NobleEstateSecurityFixtureAuthority.isFamilyType(object.type)) return true;
            if ("food-bio".equals(family) && FoodBioProductionFixtureAuthority.isFamilyType(object.type)) return true;
        }
        return false;
    }

    private static NpcEntity role(World world, String role) {
        for (NpcEntity npc : world.npcs) if (npc != null && role.equals(npc.role)) return npc;
        return null;
    }

    private static int factionMarketCount(World world) {
        int count = 0;
        for (MapObjectState object : world.mapObjects) if (object != null && "faction-market".equals(object.type)) count++;
        return count;
    }

    private static DraughtCustodyRecord firstCustody(World world) {
        for (DraughtCustodyRecord custody : world.draughtCustodyRecords) if (custody != null) return custody;
        return null;
    }

    private static void requireOffer(TraderSession session, String item) {
        require(session != null && session.offers.stream().anyMatch(o -> o != null && ItemQuality.namesMatch(o.name, item)),
                "specialist vendor missing required stock: " + item + " offers="
                        + (session == null ? "none" : session.offers.stream().map(o -> o == null ? "null" : o.name).toList()));
    }

    private static void requireContains(String actual, String expected, String label) {
        require(actual != null && actual.contains(expected), label + " missing '" + expected + "': " + actual);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone04FactionFacilityVendorGenerationSmoke() {}
}
