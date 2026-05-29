package mechanist;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

/**
 * Server/gameplay authority for death drops, corpse-search currency, and zone container bonus loot.
 *
 * This is deliberately outside rendering paths. It may allocate result records/lists because it runs on
 * entity death or container seeding, not inside the 30 FPS paint/update hot loop.
 */
final class LootDropSystemAuthority {
    static final double BASE_ITEM_DROP_CHANCE = 0.25d;
    static final double MUTANT_CULTIST_ITEM_DROP_CHANCE = 0.75d;
    static final double SECURE_DEATH_INJECTION_CHANCE = 0.02d;
    static final double SECURE_ZONE_CONTAINER_INJECTION_CHANCE = 0.02d;
    static final int MAX_CARRIED_ITEMS_PER_ENTITY = 12;
    static final int MAX_SCRIPT_DROP = 5000;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String[] EMPTY_ITEMS = new String[0];

    private static final String[] MANUFACTURING_TABLE = catalogByTerms(
            "machine", "component", "mechanical", "wire", "fastener", "barrel", "receiver", "trigger", "capacitor", "regulator", "coil", "housing", "motor", "blank", "pipe", "tool", "scrap");
    private static final String[] PDF_AND_GUARD_TABLE = catalogByTerms(
            "guard", "ration", "las", "rifle", "ammo", "charge", "armor", "fatigues", "weapon", "medkit", "field", "water", "permit");
    private static final String[] NOBLE_TABLE = catalogByTerms(
            "noble", "permit", "signet", "vault", "commerce", "fine", "medkit", "water", "ration", "coat", "ticket", "gilded", "optic", "precision");
    private static final String[] TRASH_AND_SCAVENGE_TABLE = catalogByTerms(
            "scrap", "junk", "detritus", "cloth", "wire", "bolt", "canteen", "patch", "rags", "dirty", "cracked", "improvised");
    private static final String[] CULT_MUTANT_TABLE = catalogByTerms(
            "cult", "ritual", "ash", "blade", "hide", "sump", "tainted", "fungus", "bone", "scrap", "profaned");
    private static final String[] CIVILIAN_TABLE = catalogByTerms(
            "food", "water", "civilian", "tool", "paperwork", "coat", "ration", "canteen", "lunch", "gloves", "blank form", "permit");
    private static final String[] FULL_REGISTRY = fullCatalog();

    private LootDropSystemAuthority() {}

    interface EntityInventoryModel {
        int carriedCount();
        LootCarriedItem carriedAt(int index);
        Faction faction();
        String role();
        String entityName();
        int equipmentTier();
    }

    enum LootCarryKind { ARMOR, EQUIPMENT, AMMO, HEALTH, CURRENCY, MISC }
    enum LootDisposition { CORPSE, GROUND }
    enum LootItemQuality {
        SCRAPPED("Junk", 0),
        NORMAL("Common", 2),
        SERVICEABLE("Serviceable", 3),
        FINE("Fine", 4),
        MASTERWORK("Masterwork", 5),
        NOBLE("Noble", 6),
        ARCHEOTECH("Archeotech", 7);
        final String itemPrefix;
        final int tier;
        LootItemQuality(String itemPrefix, int tier) { this.itemPrefix = itemPrefix; this.tier = tier; }
        String applyTo(String baseItem) {
            String base = ItemQuality.stripQuality(baseItem == null || baseItem.isBlank() ? "Unknown item" : baseItem.trim());
            if (this == NORMAL) return base;
            return itemPrefix + " " + base;
        }
    }

    record LootCarriedItem(String itemName, LootCarryKind kind, boolean active) {
        LootCarriedItem {
            itemName = itemName == null ? "" : itemName.trim();
            kind = kind == null ? LootCarryKind.MISC : kind;
        }
    }

    record LootDropEvent(String itemName, LootItemQuality quality, LootDisposition disposition, int tileX, int tileY, float x, float y, String reason) {
        LootDropEvent {
            itemName = itemName == null ? "Unknown item" : itemName.trim();
            quality = quality == null ? LootItemQuality.NORMAL : quality;
            disposition = disposition == null ? LootDisposition.CORPSE : disposition;
            reason = reason == null ? "drop" : reason;
        }
    }

    record LootResolution(List<LootDropEvent> drops,
                          int imperialScript,
                          double carriedItemDropChance,
                          boolean secureDeathInjectionTriggered,
                          String summary) {
        LootResolution {
            drops = drops == null ? List.of() : List.copyOf(drops);
            imperialScript = Math.max(0, Math.min(MAX_SCRIPT_DROP, imperialScript));
            carriedItemDropChance = clamp01(carriedItemDropChance);
            summary = summary == null ? "loot resolved" : summary;
        }
        List<String> corpseItemNames() {
            ArrayList<String> out = new ArrayList<>();
            for (LootDropEvent e : drops) if (e.disposition == LootDisposition.CORPSE) out.add(e.itemName);
            return out;
        }
        List<String> groundItemNames() {
            ArrayList<String> out = new ArrayList<>();
            for (LootDropEvent e : drops) if (e.disposition == LootDisposition.GROUND) out.add(e.itemName);
            return out;
        }
    }

    record ZoneContainerInjection(String itemName, LootItemQuality quality, ZoneType zoneType, double sectorZoneTier, String tableName) {
        ZoneContainerInjection {
            itemName = itemName == null ? "" : itemName.trim();
            quality = quality == null ? LootItemQuality.NORMAL : quality;
            zoneType = zoneType == null ? ZoneType.NEUTRAL_CIVILIAN_FLOOR : zoneType;
            sectorZoneTier = clamp01(sectorZoneTier);
            tableName = tableName == null ? "general" : tableName;
        }
        boolean present() { return !itemName.isBlank(); }
        String qualifiedItemName() { return quality.applyTo(itemName); }
    }

    static EntityInventoryModel npcInventory(NpcEntity npc) { return new NpcInventoryModel(npc); }

    static LootResolution resolveEntityDeathLoot(EntityInventoryModel entity, ZoneType zoneType, double sectorZoneTier, int tileX, int tileY, Random ordinaryRandom, String cause) {
        Random rng = ordinaryRandom == null ? new Random(0L) : ordinaryRandom;
        Faction faction = entity == null ? Faction.NONE : entity.faction();
        String role = entity == null ? "" : safe(entity.role());
        double tier = clamp01(sectorZoneTier);
        double chance = carriedItemDropChance(faction, role, tier);
        ArrayList<LootDropEvent> drops = new ArrayList<>(8);
        int count = entity == null ? 0 : Math.min(MAX_CARRIED_ITEMS_PER_ENTITY, Math.max(0, entity.carriedCount()));
        for (int i = 0; i < count; i++) {
            LootCarriedItem carried = entity.carriedAt(i);
            if (carried == null || carried.itemName.isBlank() || !carried.active) continue;
            if (rng.nextDouble() <= chance) {
                LootDisposition where = rng.nextBoolean() ? LootDisposition.GROUND : LootDisposition.CORPSE;
                LootItemQuality quality = baselineQualityForCarriedItem(carried, faction, tier);
                drops.add(new LootDropEvent(quality.applyTo(carried.itemName), quality, where, tileX, tileY, tileX + 0.5f, tileY + 0.5f, "carried " + carried.kind + " after " + safe(cause)));
            }
        }
        boolean injected = false;
        if (SECURE_RANDOM.nextDouble() < SECURE_DEATH_INJECTION_CHANCE) {
            String item = securePick(FULL_REGISTRY);
            LootItemQuality quality = secureQualityForTier(tier, highThreatFaction(faction));
            LootDisposition where = SECURE_RANDOM.nextBoolean() ? LootDisposition.GROUND : LootDisposition.CORPSE;
            drops.add(new LootDropEvent(quality.applyTo(item), quality, where, tileX, tileY, tileX + 0.5f, tileY + 0.5f, "secure 2 percent death injection"));
            injected = true;
        }
        int script = imperialScriptForCorpseSearch(faction, role, zoneType, tier, rng);
        String summary = "drops=" + drops.size() + " script=" + script + " chance=" + percentText(chance) + (injected ? " secure-injection" : "");
        return new LootResolution(drops, script, chance, injected, summary);
    }

    static ZoneContainerInjection maybeInjectZoneContainerBonus(ZoneType zoneType, double sectorZoneTier, String purposeText) {
        if (SECURE_RANDOM.nextDouble() >= SECURE_ZONE_CONTAINER_INJECTION_CHANCE) {
            return new ZoneContainerInjection("", LootItemQuality.NORMAL, zoneType, sectorZoneTier, "none");
        }
        ZoneType zt = zoneType == null ? ZoneType.NEUTRAL_CIVILIAN_FLOOR : zoneType;
        String tableName = tableNameForZone(zt, purposeText);
        String[] table = tableForName(tableName);
        String item = securePick(table.length == 0 ? FULL_REGISTRY : table);
        LootItemQuality quality = secureQualityForTier(sectorZoneTier, highThreatZone(zt));
        return new ZoneContainerInjection(item, quality, zt, sectorZoneTier, tableName);
    }

    static double sectorZoneTier(World world) {
        if (world == null) return 0.35d;
        double floor = (Math.max(1, Math.min(10, world.floor)) - 1) / 9.0d;
        double richness = world.zoneType == null ? 0.0d : Math.max(-3, Math.min(3, world.zoneType.richness)) / 6.0d + 0.5d;
        if (world.sewerLayer) floor *= 0.45d;
        return clamp01(floor * 0.70d + richness * 0.30d);
    }

    static double carriedItemDropChance(Faction faction, String role, double sectorZoneTier) {
        double tier = clamp01(sectorZoneTier);
        boolean cultMutant = faction == Faction.MUTANT || faction == Faction.CULTIST || faction == Faction.HERETIC;
        if (cultMutant) return MUTANT_CULTIST_ITEM_DROP_CHANCE;
        double base = BASE_ITEM_DROP_CHANCE;
        if (isNobleOrHouse(faction) || isServant(role)) base = 0.12d;
        else if (faction == Faction.SCAVENGER || faction == Faction.BANDIT || isGanger(faction)) base = 0.35d;
        else if (faction == Faction.ROGUE_MACHINE) base = 0.45d;
        double lowTierRawDropPressure = (1.0d - tier) * 0.22d;
        double highTierDiscipline = tier * (isNobleOrHouse(faction) ? 0.08d : 0.02d);
        return clamp(base + lowTierRawDropPressure - highTierDiscipline, 0.05d, 0.85d);
    }

    static int imperialScriptForCorpseSearch(Faction faction, String role, ZoneType zoneType, double sectorZoneTier, Random rng) {
        if (faction == Faction.MUTANT || faction == Faction.CULTIST || faction == Faction.HERETIC || faction == Faction.ROGUE_MACHINE) return 0;
        double tier = clamp01(sectorZoneTier);
        int base = switch (faction == null ? Faction.NONE : faction) {
            case NOBLE, NOBLE_HOUSE_VARN, NOBLE_HOUSE_KASTOR, NOBLE_HOUSE_MORVAIN, NOBLE_HOUSE_CYRA, NOBLE_HOUSE_DRAKE, NOBLE_HOUSE_TOLL, NOBLE_HOUSE_OSSUARY -> 65;
            case CIVIC WARDENS, IMPERIAL_GUARD, SORORITAS -> 22;
            case CIVIC LEDGER OFFICE, MINISTORUM, INN -> 18;
            case MECHANIST COLLEGIA, MECHANICUS_CLOISTER_RED, MECHANICUS_CLOISTER_RUST, MECHANICUS_CLOISTER_VOID -> 14;
            case HIVER, HIVER_BLOCK_AUREL, HIVER_BLOCK_MARROW, HIVER_BLOCK_SUMPLEDGER -> 5;
            case SCAVENGER, BANDIT, GANGER_IRON_RATS, GANGER_BLACK_SUMP, GANGER_CANDLE_JACKS, GANGER_RED_GRIN, GANGER_CHAIN_SAINTS, GANGER_ASH_MARKET, GANGER_WIRE_WOLVES, GANGER_DROWNED_9TH -> 3;
            default -> 2;
        };
        String lowRole = safe(role).toLowerCase(Locale.ROOT);
        double roleScale = lowRole.contains("servant") ? 1.35d : lowRole.contains("guard") && isNobleOrHouse(faction) ? 1.65d : lowRole.contains("trader") ? 1.45d : 1.0d;
        double zoneScale = 0.25d + tier * 1.95d;
        double wealthBias = zoneType == null ? 1.0d : 1.0d + Math.max(-3, Math.min(3, zoneType.richness)) * 0.12d;
        int randomAdd = rng == null ? 0 : rng.nextInt(Math.max(1, base + 1));
        return Math.max(0, Math.min(MAX_SCRIPT_DROP, (int)Math.round((base + randomAdd) * roleScale * zoneScale * wealthBias)));
    }

    static String tableNameForZone(ZoneType zt, String purposeText) {
        String hay = (safe(purposeText) + " " + (zt == null ? "" : zt.label) + " " + (zt == null ? "" : zt.descriptor)).toLowerCase(Locale.ROOT);
        if (has(hay, "manufact", "forge", "mechanist Collegia", "workshop", "machine", "component", "warehouse", "freight", "cargo")) return "manufacturing";
        if (has(hay, "guard", "pdf", "civic Wardens", "precinct", "billet", "barracks", "armory", "munition")) return "pdf_guard";
        if (has(hay, "noble", "governor", "spire", "mansion", "service spine", "vault")) return "noble";
        if (has(hay, "trash", "dump", "scav", "sump", "warren", "refuse")) return "trash_scavenge";
        if (has(hay, "cult", "mutant", "heretic", "sewer camp")) return "cult_mutant";
        return "civilian";
    }

    static String[] tableForName(String tableName) {
        return switch (safe(tableName)) {
            case "manufacturing" -> MANUFACTURING_TABLE;
            case "pdf_guard" -> PDF_AND_GUARD_TABLE;
            case "noble" -> NOBLE_TABLE;
            case "trash_scavenge" -> TRASH_AND_SCAVENGE_TABLE;
            case "cult_mutant" -> CULT_MUTANT_TABLE;
            case "civilian" -> CIVILIAN_TABLE;
            default -> FULL_REGISTRY;
        };
    }

    private static LootItemQuality baselineQualityForCarriedItem(LootCarriedItem carried, Faction faction, double tier) {
        if (carried == null) return LootItemQuality.NORMAL;
        if (carried.kind == LootCarryKind.AMMO || carried.kind == LootCarryKind.HEALTH) return tier < 0.20d ? LootItemQuality.SCRAPPED : LootItemQuality.NORMAL;
        if (isNobleOrHouse(faction) && tier > 0.65d) return LootItemQuality.SERVICEABLE;
        if (faction == Faction.MUTANT || faction == Faction.CULTIST || faction == Faction.HERETIC) return LootItemQuality.SCRAPPED;
        return tier < 0.15d ? LootItemQuality.SCRAPPED : LootItemQuality.NORMAL;
    }

    private static LootItemQuality secureQualityForTier(double sectorZoneTier, boolean highThreat) {
        double tier = clamp01(sectorZoneTier);
        int ceiling = 1;
        if (tier > 0.25d || highThreat) ceiling = 2;
        if (tier > 0.50d) ceiling = 3;
        if (tier > 0.72d || (highThreat && tier > 0.45d)) ceiling = 4;
        if (tier > 0.88d) ceiling = 5;
        int roll = SECURE_RANDOM.nextInt(Math.max(1, ceiling + 1));
        return switch (roll) {
            case 0 -> LootItemQuality.SCRAPPED;
            case 1 -> LootItemQuality.NORMAL;
            case 2 -> LootItemQuality.SERVICEABLE;
            case 3 -> LootItemQuality.FINE;
            case 4 -> LootItemQuality.MASTERWORK;
            default -> LootItemQuality.NOBLE;
        };
    }

    private static boolean highThreatFaction(Faction faction) {
        return faction == Faction.CIVIC WARDENS || faction == Faction.IMPERIAL_GUARD || faction == Faction.SORORITAS || faction == Faction.ROGUE_MACHINE || faction == Faction.MECHANIST COLLEGIA || isNobleOrHouse(faction);
    }

    private static boolean highThreatZone(ZoneType zt) {
        return zt == ZoneType.SECTOR_GOVERNORS_MANSION || zt == ZoneType.NOBLE_SERVICE_SPINE || zt == ZoneType.IMPERIAL_GUARD_BILLET || zt == ZoneType.ARBITES_PRECINCT_EDGE || zt == ZoneType.MECHANICUS_FORGE_CLOISTER || zt == ZoneType.MECHANICUS_RELIC_DUCT;
    }

    private static String securePick(String[] table) {
        String[] use = table == null || table.length == 0 ? FULL_REGISTRY : table;
        if (use.length == 0) return "Scrap bit";
        return use[SECURE_RANDOM.nextInt(use.length)];
    }

    private static String[] fullCatalog() {
        if (ItemCatalog.ITEMS == null || ItemCatalog.ITEMS.isEmpty()) return new String[]{"Scrap bit", "Emergency rations", "Water bottle"};
        String[] out = new String[ItemCatalog.ITEMS.size()];
        int i = 0;
        for (ItemDef d : ItemCatalog.ITEMS.values()) out[i++] = d.name;
        return out;
    }

    private static String[] catalogByTerms(String... terms) {
        if (ItemCatalog.ITEMS == null || ItemCatalog.ITEMS.isEmpty()) return EMPTY_ITEMS;
        ArrayList<String> out = new ArrayList<>();
        for (ItemDef d : ItemCatalog.ITEMS.values()) {
            String hay = (safe(d.name) + " " + safe(d.category) + " " + safe(d.source) + " " + safe(d.description) + " " + safe(d.use)).toLowerCase(Locale.ROOT);
            if (has(hay, terms)) out.add(d.name);
        }
        if (out.isEmpty()) return EMPTY_ITEMS;
        Collections.sort(out);
        return out.toArray(String[]::new);
    }

    private static boolean isNobleOrHouse(Faction faction) { return faction != null && (faction == Faction.NOBLE || faction.name().startsWith("NOBLE_")); }
    private static boolean isGanger(Faction faction) { return faction != null && faction.name().startsWith("GANGER_"); }
    private static boolean isServant(String role) { return role != null && role.toLowerCase(Locale.ROOT).contains("servant"); }

    private static boolean has(String hay, String... terms) {
        if (hay == null || terms == null) return false;
        for (String t : terms) if (t != null && !t.isBlank() && hay.contains(t.toLowerCase(Locale.ROOT))) return true;
        return false;
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static double clamp01(double v) { return clamp(v, 0.0d, 1.0d); }
    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, Double.isFinite(v) ? v : lo)); }
    private static String percentText(double v) { return Math.round(clamp01(v) * 100.0d) + "%"; }

    private static final class NpcInventoryModel implements EntityInventoryModel {
        private final NpcEntity npc;
        private final LootCarriedItem[] carried = new LootCarriedItem[MAX_CARRIED_ITEMS_PER_ENTITY];
        private int count;
        NpcInventoryModel(NpcEntity npc) {
            this.npc = npc;
            if (npc == null) return;
            add(npc.equippedArmor, LootCarryKind.ARMOR);
            add(npc.equippedMeleeWeapon, LootCarryKind.EQUIPMENT);
            add(npc.equippedRangedWeapon, LootCarryKind.EQUIPMENT);
            add(npc.equippedExplosive, LootCarryKind.EQUIPMENT);
            if (npc.loadedShots > 0 || npc.ammoReloadsRemaining > 0) add(ammoItemFor(npc.equippedRangedWeapon), LootCarryKind.AMMO);
            if (npc.role != null && npc.role.toLowerCase(Locale.ROOT).contains("medicae")) add("Bandage roll", LootCarryKind.HEALTH);
            if (npc.role != null && npc.role.toLowerCase(Locale.ROOT).contains("trader")) add("Trade chit", LootCarryKind.CURRENCY);
        }
        private void add(String item, LootCarryKind kind) {
            if (count >= carried.length || item == null || item.isBlank() || item.equalsIgnoreCase("none")) return;
            carried[count++] = new LootCarriedItem(item, kind, true);
        }
        @Override public int carriedCount() { return count; }
        @Override public LootCarriedItem carriedAt(int index) { return index < 0 || index >= count ? null : carried[index]; }
        @Override public Faction faction() { return npc == null || npc.faction == null ? Faction.NONE : npc.faction; }
        @Override public String role() { return npc == null ? "" : npc.role; }
        @Override public String entityName() { return npc == null ? "unknown" : npc.name; }
        @Override public int equipmentTier() { return npc == null ? 0 : npc.equipmentTier; }
    }

    private static String ammoItemFor(String weapon) {
        String low = safe(weapon).toLowerCase(Locale.ROOT);
        if (low.contains("las")) return "Las charge pack";
        if (low.contains("shotgun")) return "Shotgun shells";
        if (low.contains("bolt") || low.contains("mass-Reactive Carbine")) return "Bolt shells";
        if (low.contains("plasma")) return "Plasma fuel flask";
        if (low.contains("flamer")) return "Industrial Fuelgel canister";
        if (low.contains("melta")) return "Melta fuel canister";
        return "Stub rounds";
    }
}
