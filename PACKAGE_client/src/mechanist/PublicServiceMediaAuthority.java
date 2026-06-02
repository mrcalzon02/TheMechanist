package mechanist;

import java.util.*;

/**
 * Runtime authority for public-service, vending, and media fixtures.
 *
 * Keeps labels, stock text, cooldowns, and vending definitions in one compact
 * source-owned table so asset promotion does not scatter repeated
 * switch statements or frontage strings through UI and generation code.
 */
final class PublicServiceMediaAuthority {
    static final String VERSION = "0.9.10af";

    static final String[] FRONTAGE_TYPES = {
            AssetIntegrationDisciplineAuthority.PUBLIC_RATION_BENCH,
            AssetIntegrationDisciplineAuthority.PUBLIC_PICT_SCREEN,
            AssetIntegrationDisciplineAuthority.CHEAP_RADIO,
            AssetIntegrationDisciplineAuthority.PUBLIC_INFO_KIOSK,
            AssetIntegrationDisciplineAuthority.PUBLIC_SERVICE_COUNTER
    };

    static final class VendingDefinition {
        final char symbol;
        final String handle;
        final String name;
        final String stockText;
        final int cost;
        final String[] legalStock;
        final String artKey;
        VendingDefinition(char symbol, String handle, String name, String stockText, int cost, String artKey, String... legalStock) {
            this.symbol = symbol;
            this.handle = handle;
            this.name = name;
            this.stockText = stockText;
            this.cost = Math.max(1, cost);
            this.artKey = artKey;
            this.legalStock = legalStock == null || legalStock.length == 0 ? new String[]{"Vended scrap"} : legalStock;
        }
    }

    private static final LinkedHashMap<Character, VendingDefinition> VENDING = makeVendingDefinitions();

    private PublicServiceMediaAuthority() {}

    private static LinkedHashMap<Character, VendingDefinition> makeVendingDefinitions() {
        LinkedHashMap<Character, VendingDefinition> m = new LinkedHashMap<>();
        add(m, new VendingDefinition('1', "nutrient-vending-shrine", "nutrient vending shrine", "canned food, water bottle, emergency ration", 3, "vending_food", "Canned food", "Water bottle", "Emergency ration"));
        add(m, new VendingDefinition('2', "armor-issue-vending-cabinet", "armor issue vending cabinet", "padded coat, battered helmet, scrap plates", 12, "vending_armor", "Battered helmet", "Padded coat", "Scrap plate"));
        add(m, new VendingDefinition('3', "weapon-requisition-vending-cage", "weapons requisition vending cage", "knife, stub rounds, crude pistol", 18, "vending_weapons", "Knife", "Stub round packet", "Crude pistol"));
        add(m, new VendingDefinition('4', "construction-material-vending-locker", "construction-material vending locker", "scrap, machine parts, plasteel fasteners, rare data spike", 9, "vending_materials", "Machine part", "Construction supplies", "Plasteel fasteners", "Data spike"));
        add(m, new VendingDefinition('5', "survival-supplies-vending-stack", "survival-supplies vending stack", "canteen, field supplies, medicae gauze", 6, "vending_survival", "Emergency ration", "Field supplies", "Canteen", "Medicae gauze"));
        return m;
    }

    private static void add(LinkedHashMap<Character, VendingDefinition> m, VendingDefinition d) { m.put(d.symbol, d); }

    static boolean isFrontageFamilyType(String type) {
        String c = AssetIntegrationDisciplineAuthority.canonicalType(type);
        for (String t : FRONTAGE_TYPES) if (t.equals(c)) return true;
        return false;
    }

    static boolean isVendingSymbol(char ch) { return VENDING.containsKey(ch); }
    static VendingDefinition vendingDefinition(char ch) { return VENDING.get(ch); }
    static int legalVendingCooldownTurns() { return 12 * GamePanel.TURNS_PER_HOUR; }
    static int hackedVendingCooldownTurns() { return GamePanel.TURNS_PER_HOUR; }
    static int maxHackedCycles() { return 8; }
    static int hackDifficulty(int hackedCount) { return 12 + Math.max(0, hackedCount) * 2; }

    static String vendingName(char ch) {
        VendingDefinition d = vendingDefinition(ch);
        return d == null ? "vending machine" : d.name;
    }

    static String vendingStockText(char ch) {
        VendingDefinition d = vendingDefinition(ch);
        return d == null ? "miscellaneous stock" : d.stockText;
    }

    static int vendingCost(char ch) {
        VendingDefinition d = vendingDefinition(ch);
        return d == null ? 5 : d.cost;
    }

    static String vendingHandle(char ch) {
        VendingDefinition d = vendingDefinition(ch);
        return d == null ? "vending-machine" : d.handle;
    }

    static String chooseVendedItem(char ch, Random r) {
        VendingDefinition d = vendingDefinition(ch);
        String[] stock = d == null ? new String[]{"Vended scrap"} : d.legalStock;
        if (r == null) r = new Random(0);
        for (int i = 0; i < 12; i++) {
            String item = stock[r.nextInt(stock.length)];
            if (ItemCatalog.get(item) != null) return item;
        }
        return stock[0];
    }

    static String vendingPreview(char ch, int cooldownUntilTurn, int hackedCycles) {
        String stock = vendingStockText(ch);
        if (hackedCycles > 0) return vendingName(ch) + " hacked cycle " + hackedCycles + "/" + maxHackedCycles() + ". Stock: " + stock + ". Cooldown until turn " + cooldownUntilTurn + ".";
        return vendingName(ch) + ". Stock: " + stock + ". Legal cycle: once per 12 hours. Cooldown until turn " + cooldownUntilTurn + ".";
    }

    static String frontageLabel(String type, ZoneType z, Random r) {
        if (r == null) r = new Random(0);
        String zone = z == null ? "Unknown zone" : z.label;
        String c = AssetIntegrationDisciplineAuthority.canonicalType(type);
        if (AssetIntegrationDisciplineAuthority.PUBLIC_RATION_BENCH.equals(c)) return pick(r, "Public ration bench", "Queue-worn public bench", "Sanctioned rest bench") + " / " + zone;
        if (AssetIntegrationDisciplineAuthority.PUBLIC_PICT_SCREEN.equals(c)) return pick(r, "Public pict/news screen", "INN pict display", "Civic bulletin pict screen") + " / " + zone;
        if (AssetIntegrationDisciplineAuthority.CHEAP_RADIO.equals(c)) return pick(r, "Cheap public radio", "Wall-mounted vox radio", "Roadside broadcast receiver") + " / " + zone;
        if (AssetIntegrationDisciplineAuthority.PUBLIC_INFO_KIOSK.equals(c)) return pick(r, "Public information kiosk", "Route and permit kiosk", "Civic directory kiosk") + " / " + zone;
        if (AssetIntegrationDisciplineAuthority.PUBLIC_SERVICE_COUNTER.equals(c)) return pick(r, "Public service counter", "Queue-service counter", "Civic forms counter") + " / " + zone;
        return "Public-service fixture / " + zone;
    }

    static String frontageStock(String type, char underlying) {
        String c = AssetIntegrationDisciplineAuthority.canonicalType(type);
        String semantic = AssetIntegrationDisciplineAuthority.semanticKeyForType(c);
        String service;
        if (AssetIntegrationDisciplineAuthority.PUBLIC_RATION_BENCH.equals(c)) service = "rest-social-cue";
        else if (AssetIntegrationDisciplineAuthority.PUBLIC_PICT_SCREEN.equals(c)) service = "news-propaganda-broadcast";
        else if (AssetIntegrationDisciplineAuthority.CHEAP_RADIO.equals(c)) service = "radio-bulletin-ambient-sound";
        else if (AssetIntegrationDisciplineAuthority.PUBLIC_INFO_KIOSK.equals(c)) service = "map-rumor-directory";
        else if (AssetIntegrationDisciplineAuthority.PUBLIC_SERVICE_COUNTER.equals(c)) service = "licensing-ration-admin";
        else service = "public-service";
        return "road-frontage;public-service-media;semantic=" + safe(semantic) + ";service=" + service + ";under=" + (int)underlying;
    }

    static String benchRumor(GamePanel g, Random r) {
        if (r == null) r = new Random(0);
        String[] v = {
                "Patrons mutter about patrol routes, bad water, and which corridor lights have started flickering.",
                "Somebody carved a direction mark under the bench. It points toward trouble with admirable confidence.",
                "The bench is warm, which means either public life continues or something recently died here.",
                "A nearby passerby complains about service counters, ration lines, and the price of not starving."
        };
        return v[r.nextInt(v.length)];
    }

    static String kioskLine(GamePanel g, Random r) {
        if (r == null) r = new Random(0);
        String zone = g == null || g.world == null || g.world.zoneType == null ? "this zone" : g.world.zoneType.label;
        String[] v = {
                "routing directory confirms that sidewalks count as corridor access for emergency passages and room frontage.",
                "public notice lists nearest medicae frontage, faction bar, and transit access with exactly enough detail to be almost useful.",
                "civic board warns that roads are not safe, sidewalks are not private, and alleys are not legally acknowledged.",
                "zone directory: " + zone + ". Public fixtures identify nearby service anchors, transit points, and civic access surfaces."
        };
        return v[r.nextInt(v.length)];
    }

    static String serviceCounterInspectionLine(Random r) {
        if (r == null) r = new Random(0);
        return pick(r,
                "forms, shutters, tired clerks, and a sign insisting the right office is somewhere else.",
                "a queue-number punch, stamped pads, and civic machinery arranged to metabolize impatience.",
                "counter glass, receipt hooks, and a directory of offices that all claim jurisdiction over one another.");
    }

    static String serviceCounterIssuedItem() { return "Civic inspection chit"; }

    static String shortLabel(MapObjectState m) {
        String s = m == null || m.label == null ? "public-service fixture" : m.label;
        int slash = s.indexOf('/');
        if (slash > 0) s = s.substring(0, slash).trim();
        return s.isBlank() ? "public-service fixture" : s;
    }

    static String auditSummary() {
        return "publicServiceMedia version=" + VERSION + " frontageTypes=" + FRONTAGE_TYPES.length +
                " vendingDefinitions=" + VENDING.size() + " rule=centralized fixture/vending definitions; no runtime scans";
    }

    private static String pick(Random r, String... values) { return values[Math.floorMod(r.nextInt(), values.length)]; }
    static String safe(String s) { return s == null ? "" : s.replace(';', ','); }
}
