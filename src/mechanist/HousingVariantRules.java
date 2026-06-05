package mechanist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Faction/wealth housing variant contract.
 *
 * Room generation should not only pick a room footprint.  It should also pick the
 * objects, containers, personal storage fixtures, vehicle/pet support objects,
 * and faction dressing that make that room belong to a specific person/faction
 * at a specific wealth tier.
 */
final class HousingVariantRules {
    enum HousingVariantKind {
        BUNK_SPACE,
        PERSONAL_DORMITORY,
        SHARED_APARTMENT,
        PRIVATE_APARTMENT,
        LARGE_APARTMENT,
        ESTATE_SUITE,
        SECURED_FACTION_QUARTERS,
        NO_FIXED_HOUSING
    }

    enum HousingObjectCategory {
        BED,
        COT,
        DRESSER,
        CABINET,
        SINK,
        TOILET,
        SHOWER,
        TABLE,
        CHAIR,
        COOKING_SURFACE,
        FOOD_STORAGE,
        PERSONAL_LOCKER,
        SECURE_LOCKER,
        WEAPON_LOCKER,
        STRONGBOX,
        PET_BED,
        PET_BOWL,
        VEHICLE_KEYS_HOOK,
        VEHICLE_PARTS_SHELF,
        WORKBENCH,
        SHRINE,
        DATA_TERMINAL,
        DOCUMENT_CABINET,
        TOOL_RACK,
        TRASH_PILE,
        CONTRABAND_CACHE,
        ART_OBJECT,
        SERVANT_BELL,
        FACTION_BANNER,
        LAUNDRY,
        LIGHTING,
        HEATING_UNIT
    }

    enum ContainerKind {
        PERSONAL_LOCKER,
        PLASTIC_BIN,
        METAL_CABINET,
        DRESSER_DRAWERS,
        FOOTLOCKER,
        WALL_SAFE,
        STRONGBOX,
        WEAPON_RACK,
        TOOL_CHEST,
        DOCUMENT_ARCHIVE,
        FOOD_CRATE,
        PET_SUPPLY_BOX,
        VEHICLE_PARTS_BOX,
        CONTRABAND_STASH,
        NOBLE_TRUNK,
        FACTION_EVIDENCE_LOCKER,
        SCRAP_SACK,
        RATION_CABINET
    }

    enum FactionHousingStyle {
        NEUTRAL_HAB,
        HIVER_BLOCK,
        CIVIC_BUREAU,
        SECURITY_BARRACKS,
        FORGE_DORMITORY,
        MINISTRY_CELL,
        NEWSROOM_LODGING,
        NOBLE_ESTATE,
        GANG_HIDEOUT,
        SCAVENGER_DEN,
        MUTANT_NEST,
        CULT_CELL,
        ROGUE_MACHINE_CELL
    }

    static final class HousingVariant {
        final Faction faction;
        final FactionHousingStyle style;
        final HousingVariantKind kind;
        final PersonalWealthPropertyRules.WealthTier wealthTier;
        final PersonalWealthPropertyRules.HousingType sourceHousingType;
        final ArrayList<HousingObjectCategory> requiredObjects;
        final ArrayList<ContainerKind> containers;
        final ArrayList<String> factionDressing;
        final ArrayList<String> personalBelongingTags;
        final int minimumRoomWidth;
        final int minimumRoomHeight;
        final boolean needsPetSpace;
        final boolean needsVehicleAccess;
        final boolean needsPrivateStorage;
        final boolean secureByDefault;
        final String label;

        HousingVariant(Faction faction, FactionHousingStyle style, HousingVariantKind kind,
                       PersonalWealthPropertyRules.WealthTier wealthTier,
                       PersonalWealthPropertyRules.HousingType sourceHousingType,
                       List<HousingObjectCategory> requiredObjects,
                       List<ContainerKind> containers,
                       List<String> factionDressing,
                       List<String> personalBelongingTags,
                       int minimumRoomWidth, int minimumRoomHeight,
                       boolean needsPetSpace, boolean needsVehicleAccess, boolean needsPrivateStorage,
                       boolean secureByDefault, String label) {
            this.faction = faction == null ? Faction.NONE : faction;
            this.style = style == null ? FactionHousingStyle.NEUTRAL_HAB : style;
            this.kind = kind == null ? HousingVariantKind.BUNK_SPACE : kind;
            this.wealthTier = wealthTier == null ? PersonalWealthPropertyRules.WealthTier.WORKING : wealthTier;
            this.sourceHousingType = sourceHousingType == null ? PersonalWealthPropertyRules.HousingType.BUNK : sourceHousingType;
            this.requiredObjects = new ArrayList<>(requiredObjects == null ? List.of() : requiredObjects);
            this.containers = new ArrayList<>(containers == null ? List.of() : containers);
            this.factionDressing = new ArrayList<>(factionDressing == null ? List.of() : factionDressing);
            this.personalBelongingTags = new ArrayList<>(personalBelongingTags == null ? List.of() : personalBelongingTags);
            this.minimumRoomWidth = Math.max(2, minimumRoomWidth);
            this.minimumRoomHeight = Math.max(2, minimumRoomHeight);
            this.needsPetSpace = needsPetSpace;
            this.needsVehicleAccess = needsVehicleAccess;
            this.needsPrivateStorage = needsPrivateStorage;
            this.secureByDefault = secureByDefault;
            this.label = label == null ? this.kind.name().toLowerCase(Locale.ROOT) : label;
        }

        List<HousingObjectCategory> objects() { return Collections.unmodifiableList(requiredObjects); }
        List<ContainerKind> containerKinds() { return Collections.unmodifiableList(containers); }
        List<String> dressing() { return Collections.unmodifiableList(factionDressing); }
        List<String> belongings() { return Collections.unmodifiableList(personalBelongingTags); }
    }

    static final class HousingLoadoutPlan {
        final HousingVariant variant;
        final ArrayList<String> objectSpawnKeys;
        final ArrayList<String> containerSpawnKeys;
        final ArrayList<String> looseBelongingSpawnKeys;
        final ArrayList<String> placementNotes;

        HousingLoadoutPlan(HousingVariant variant, List<String> objectSpawnKeys, List<String> containerSpawnKeys,
                           List<String> looseBelongingSpawnKeys, List<String> placementNotes) {
            this.variant = variant;
            this.objectSpawnKeys = new ArrayList<>(objectSpawnKeys == null ? List.of() : objectSpawnKeys);
            this.containerSpawnKeys = new ArrayList<>(containerSpawnKeys == null ? List.of() : containerSpawnKeys);
            this.looseBelongingSpawnKeys = new ArrayList<>(looseBelongingSpawnKeys == null ? List.of() : looseBelongingSpawnKeys);
            this.placementNotes = new ArrayList<>(placementNotes == null ? List.of() : placementNotes);
        }

        List<String> objects() { return Collections.unmodifiableList(objectSpawnKeys); }
        List<String> containers() { return Collections.unmodifiableList(containerSpawnKeys); }
        List<String> looseBelongings() { return Collections.unmodifiableList(looseBelongingSpawnKeys); }
        List<String> notes() { return Collections.unmodifiableList(placementNotes); }
    }

    private HousingVariantRules() {}

    static HousingVariant variantFor(Faction faction, PersonalWealthPropertyRules.WealthProfile wealthProfile) {
        PersonalWealthPropertyRules.WealthProfile profile = wealthProfile == null
                ? PersonalWealthPropertyRules.profileForTier(PersonalWealthPropertyRules.WealthTier.WORKING, faction)
                : wealthProfile;
        FactionHousingStyle style = styleForFaction(faction);
        HousingVariantKind kind = kindFor(profile.housingType);
        ArrayList<HousingObjectCategory> objects = baseObjectsFor(kind);
        ArrayList<ContainerKind> containers = baseContainersFor(profile);
        ArrayList<String> dressing = factionDressing(style);
        ArrayList<String> belongings = new ArrayList<>();
        for (PersonalWealthPropertyRules.PersonalAsset asset : profile.assets()) belongings.add(asset.name().toLowerCase(Locale.ROOT));

        if (profile.mayOwnPet) {
            objects.add(HousingObjectCategory.PET_BED);
            objects.add(HousingObjectCategory.PET_BOWL);
            containers.add(ContainerKind.PET_SUPPLY_BOX);
        }
        if (profile.mayOwnPersonalVehicle) {
            objects.add(HousingObjectCategory.VEHICLE_KEYS_HOOK);
            objects.add(HousingObjectCategory.VEHICLE_PARTS_SHELF);
            containers.add(ContainerKind.VEHICLE_PARTS_BOX);
        }
        if (profile.assets().contains(PersonalWealthPropertyRules.PersonalAsset.WEAPON_LOCKER)) {
            objects.add(HousingObjectCategory.WEAPON_LOCKER);
            containers.add(ContainerKind.WEAPON_RACK);
        }
        if (profile.assets().contains(PersonalWealthPropertyRules.PersonalAsset.VALUABLES_CACHE)) {
            objects.add(HousingObjectCategory.STRONGBOX);
            containers.add(profile.tier.rank >= PersonalWealthPropertyRules.WealthTier.NOBLE.rank ? ContainerKind.NOBLE_TRUNK : ContainerKind.STRONGBOX);
        }
        applyFactionObjects(style, objects, containers);
        int[] size = minimumSizeFor(kind, profile.tier);
        return new HousingVariant(faction, style, kind, profile.tier, profile.housingType,
                objects, containers, dressing, belongings, size[0], size[1], profile.mayOwnPet,
                profile.mayOwnPersonalVehicle, profile.mayHavePrivateStorage,
                isSecuredStyle(style) || profile.tier.rank >= PersonalWealthPropertyRules.WealthTier.NOBLE.rank,
                labelFor(style, kind, profile.tier));
    }

    static HousingLoadoutPlan loadoutFor(Faction faction, PersonalWealthPropertyRules.WealthProfile profile, long seed) {
        HousingVariant variant = variantFor(faction, profile);
        PersonalWealthPropertyRules.BelongingsPlan belongingsPlan = PersonalWealthPropertyRules.belongingsFor(profile, seed);
        ArrayList<String> objectKeys = new ArrayList<>();
        ArrayList<String> containerKeys = new ArrayList<>();
        ArrayList<String> looseKeys = new ArrayList<>();
        ArrayList<String> notes = new ArrayList<>();
        for (HousingObjectCategory object : variant.objects()) objectKeys.add(spawnKey(variant.style, object));
        for (ContainerKind container : variant.containerKinds()) containerKeys.add(spawnKey(variant.style, container));
        looseKeys.addAll(belongingsPlan.belongings());
        looseKeys.addAll(variant.belongings());
        notes.addAll(belongingsPlan.roomRequirements());
        if (variant.needsVehicleAccess) notes.add("place near assigned parking, garage, vehicle lot, or road-access storage bay");
        if (variant.needsPetSpace) notes.add("reserve a clear pet-access tile near bed/apartment storage");
        if (variant.needsPrivateStorage) notes.add("place personal containers against wall, not blocking door/corridor pathing");
        if (variant.secureByDefault) notes.add("prefer lockable room boundary and faction-secured door if available");
        return new HousingLoadoutPlan(variant, objectKeys, containerKeys, looseKeys, notes);
    }

    static FactionHousingStyle styleForFaction(Faction faction) {
        if (faction == null) return FactionHousingStyle.NEUTRAL_HAB;
        switch (faction) {
            case HIVER, HIVER_BLOCK_AUREL, HIVER_BLOCK_MARROW, HIVER_BLOCK_SUMPLEDGER:
                return FactionHousingStyle.HIVER_BLOCK;
            case ADMINISTRATUM, CIVIC_LEDGER_OFFICE:
                return FactionHousingStyle.CIVIC_BUREAU;
            case ARBITES, CIVIC_WARDENS, IMPERIAL_GUARD, SORORITAS:
                return FactionHousingStyle.SECURITY_BARRACKS;
            case MECHANICUS, MECHANIST_COLLEGIA, MECHANICUS_CLOISTER_RED, MECHANICUS_CLOISTER_RUST, MECHANICUS_CLOISTER_VOID:
                return FactionHousingStyle.FORGE_DORMITORY;
            case MINISTORUM:
                return FactionHousingStyle.MINISTRY_CELL;
            case INN:
                return FactionHousingStyle.NEWSROOM_LODGING;
            case NOBLE, NOBLE_HOUSE_VARN, NOBLE_HOUSE_KASTOR, NOBLE_HOUSE_MORVAIN, NOBLE_HOUSE_CYRA, NOBLE_HOUSE_DRAKE, NOBLE_HOUSE_TOLL, NOBLE_HOUSE_OSSUARY:
                return FactionHousingStyle.NOBLE_ESTATE;
            case BANDIT, GANGER_IRON_RATS, GANGER_BLACK_SUMP, GANGER_CANDLE_JACKS, GANGER_RED_GRIN, GANGER_CHAIN_SAINTS, GANGER_ASH_MARKET, GANGER_WIRE_WOLVES, GANGER_DROWNED_9TH:
                return FactionHousingStyle.GANG_HIDEOUT;
            case SCAVENGER:
                return FactionHousingStyle.SCAVENGER_DEN;
            case MUTANT:
                return FactionHousingStyle.MUTANT_NEST;
            case CULTIST, HERETIC:
                return FactionHousingStyle.CULT_CELL;
            case ROGUE_MACHINE:
                return FactionHousingStyle.ROGUE_MACHINE_CELL;
            default:
                return FactionHousingStyle.NEUTRAL_HAB;
        }
    }

    static HousingVariantKind kindFor(PersonalWealthPropertyRules.HousingType type) {
        if (type == null) return HousingVariantKind.BUNK_SPACE;
        return switch (type) {
            case NONE -> HousingVariantKind.NO_FIXED_HOUSING;
            case BUNK -> HousingVariantKind.BUNK_SPACE;
            case PERSONAL_DORMITORY -> HousingVariantKind.PERSONAL_DORMITORY;
            case SHARED_APARTMENT -> HousingVariantKind.SHARED_APARTMENT;
            case PRIVATE_APARTMENT -> HousingVariantKind.PRIVATE_APARTMENT;
            case LARGE_APARTMENT -> HousingVariantKind.LARGE_APARTMENT;
            case ESTATE_SUITE -> HousingVariantKind.ESTATE_SUITE;
            case SECURED_FACTION_QUARTERS -> HousingVariantKind.SECURED_FACTION_QUARTERS;
        };
    }

    static ArrayList<HousingObjectCategory> baseObjectsFor(HousingVariantKind kind) {
        ArrayList<HousingObjectCategory> out = new ArrayList<>();
        switch (kind) {
            case NO_FIXED_HOUSING -> {
                out.add(HousingObjectCategory.TRASH_PILE);
                out.add(HousingObjectCategory.LIGHTING);
            }
            case BUNK_SPACE -> {
                out.add(HousingObjectCategory.COT);
                out.add(HousingObjectCategory.PERSONAL_LOCKER);
                out.add(HousingObjectCategory.LIGHTING);
            }
            case PERSONAL_DORMITORY -> {
                out.add(HousingObjectCategory.COT);
                out.add(HousingObjectCategory.SINK);
                out.add(HousingObjectCategory.DRESSER);
                out.add(HousingObjectCategory.CABINET);
                out.add(HousingObjectCategory.PERSONAL_LOCKER);
            }
            case SHARED_APARTMENT -> {
                out.add(HousingObjectCategory.BED);
                out.add(HousingObjectCategory.TABLE);
                out.add(HousingObjectCategory.CHAIR);
                out.add(HousingObjectCategory.FOOD_STORAGE);
                out.add(HousingObjectCategory.PERSONAL_LOCKER);
            }
            case PRIVATE_APARTMENT, LARGE_APARTMENT -> {
                out.add(HousingObjectCategory.BED);
                out.add(HousingObjectCategory.TOILET);
                out.add(HousingObjectCategory.SHOWER);
                out.add(HousingObjectCategory.TABLE);
                out.add(HousingObjectCategory.CHAIR);
                out.add(HousingObjectCategory.COOKING_SURFACE);
                out.add(HousingObjectCategory.FOOD_STORAGE);
                out.add(HousingObjectCategory.DRESSER);
                out.add(HousingObjectCategory.CABINET);
            }
            case ESTATE_SUITE -> {
                out.add(HousingObjectCategory.BED);
                out.add(HousingObjectCategory.TOILET);
                out.add(HousingObjectCategory.SHOWER);
                out.add(HousingObjectCategory.TABLE);
                out.add(HousingObjectCategory.CHAIR);
                out.add(HousingObjectCategory.COOKING_SURFACE);
                out.add(HousingObjectCategory.FOOD_STORAGE);
                out.add(HousingObjectCategory.ART_OBJECT);
                out.add(HousingObjectCategory.SERVANT_BELL);
                out.add(HousingObjectCategory.STRONGBOX);
            }
            case SECURED_FACTION_QUARTERS -> {
                out.add(HousingObjectCategory.COT);
                out.add(HousingObjectCategory.SECURE_LOCKER);
                out.add(HousingObjectCategory.DATA_TERMINAL);
                out.add(HousingObjectCategory.TOOL_RACK);
                out.add(HousingObjectCategory.FACTION_BANNER);
            }
        }
        return out;
    }

    static ArrayList<ContainerKind> baseContainersFor(PersonalWealthPropertyRules.WealthProfile profile) {
        PersonalWealthPropertyRules.WealthProfile safe = profile == null ? PersonalWealthPropertyRules.profileForTier(PersonalWealthPropertyRules.WealthTier.WORKING, Faction.NONE) : profile;
        ArrayList<ContainerKind> out = new ArrayList<>();
        switch (safe.housingType) {
            case NONE -> out.add(ContainerKind.SCRAP_SACK);
            case BUNK -> out.add(ContainerKind.FOOTLOCKER);
            case PERSONAL_DORMITORY -> {
                out.add(ContainerKind.PERSONAL_LOCKER);
                out.add(ContainerKind.DRESSER_DRAWERS);
                out.add(ContainerKind.METAL_CABINET);
            }
            case SHARED_APARTMENT -> {
                out.add(ContainerKind.PERSONAL_LOCKER);
                out.add(ContainerKind.RATION_CABINET);
                out.add(ContainerKind.PLASTIC_BIN);
            }
            case PRIVATE_APARTMENT, LARGE_APARTMENT -> {
                out.add(ContainerKind.DRESSER_DRAWERS);
                out.add(ContainerKind.METAL_CABINET);
                out.add(ContainerKind.RATION_CABINET);
                out.add(ContainerKind.FOOD_CRATE);
            }
            case ESTATE_SUITE -> {
                out.add(ContainerKind.NOBLE_TRUNK);
                out.add(ContainerKind.WALL_SAFE);
                out.add(ContainerKind.DRESSER_DRAWERS);
                out.add(ContainerKind.FOOD_CRATE);
            }
            case SECURED_FACTION_QUARTERS -> {
                out.add(ContainerKind.FACTION_EVIDENCE_LOCKER);
                out.add(ContainerKind.METAL_CABINET);
                out.add(ContainerKind.TOOL_CHEST);
            }
        }
        return out;
    }

    static void applyFactionObjects(FactionHousingStyle style, ArrayList<HousingObjectCategory> objects, ArrayList<ContainerKind> containers) {
        if (objects == null || containers == null) return;
        switch (style) {
            case CIVIC_BUREAU -> { objects.add(HousingObjectCategory.DOCUMENT_CABINET); objects.add(HousingObjectCategory.DATA_TERMINAL); containers.add(ContainerKind.DOCUMENT_ARCHIVE); }
            case SECURITY_BARRACKS -> { objects.add(HousingObjectCategory.WEAPON_LOCKER); containers.add(ContainerKind.WEAPON_RACK); containers.add(ContainerKind.FACTION_EVIDENCE_LOCKER); }
            case FORGE_DORMITORY -> { objects.add(HousingObjectCategory.WORKBENCH); objects.add(HousingObjectCategory.TOOL_RACK); containers.add(ContainerKind.TOOL_CHEST); }
            case MINISTRY_CELL -> { objects.add(HousingObjectCategory.SHRINE); containers.add(ContainerKind.RATION_CABINET); }
            case NEWSROOM_LODGING -> { objects.add(HousingObjectCategory.DATA_TERMINAL); objects.add(HousingObjectCategory.DOCUMENT_CABINET); containers.add(ContainerKind.DOCUMENT_ARCHIVE); }
            case NOBLE_ESTATE -> { objects.add(HousingObjectCategory.ART_OBJECT); objects.add(HousingObjectCategory.SERVANT_BELL); containers.add(ContainerKind.NOBLE_TRUNK); }
            case GANG_HIDEOUT -> { objects.add(HousingObjectCategory.CONTRABAND_CACHE); containers.add(ContainerKind.CONTRABAND_STASH); }
            case SCAVENGER_DEN, MUTANT_NEST -> { objects.add(HousingObjectCategory.TRASH_PILE); containers.add(ContainerKind.SCRAP_SACK); }
            case CULT_CELL -> { objects.add(HousingObjectCategory.SHRINE); objects.add(HousingObjectCategory.CONTRABAND_CACHE); containers.add(ContainerKind.CONTRABAND_STASH); }
            case ROGUE_MACHINE_CELL -> { objects.add(HousingObjectCategory.DATA_TERMINAL); objects.add(HousingObjectCategory.TOOL_RACK); containers.add(ContainerKind.TOOL_CHEST); }
            case HIVER_BLOCK, NEUTRAL_HAB -> { objects.add(HousingObjectCategory.LAUNDRY); objects.add(HousingObjectCategory.HEATING_UNIT); }
        }
    }

    static ArrayList<String> factionDressing(FactionHousingStyle style) {
        ArrayList<String> out = new ArrayList<>();
        switch (style) {
            case CIVIC_BUREAU -> Collections.addAll(out, "ledger labels", "permit folders", "queue tickets");
            case SECURITY_BARRACKS -> Collections.addAll(out, "inspection placards", "armory tags", "duty roster");
            case FORGE_DORMITORY -> Collections.addAll(out, "oil stains", "machine prayer tags", "tool calibration strips");
            case MINISTRY_CELL -> Collections.addAll(out, "wall icons", "candles", "ration charity slips");
            case NEWSROOM_LODGING -> Collections.addAll(out, "news clippings", "recording slates", "press bags");
            case NOBLE_ESTATE -> Collections.addAll(out, "family crest", "fine rugs", "servant call plates");
            case GANG_HIDEOUT -> Collections.addAll(out, "tags", "scratched warnings", "contraband wrappers");
            case SCAVENGER_DEN -> Collections.addAll(out, "salvage piles", "patched tarps", "sorted scrap bins");
            case MUTANT_NEST -> Collections.addAll(out, "rag screens", "broken furniture", "improvised heat source");
            case CULT_CELL -> Collections.addAll(out, "coded marks", "concealed shrine", "burned papers");
            case ROGUE_MACHINE_CELL -> Collections.addAll(out, "loose conduit", "diagnostic paper", "machine access marks");
            case HIVER_BLOCK -> Collections.addAll(out, "laundry lines", "ration crates", "cheap privacy curtains");
            default -> Collections.addAll(out, "worn floor mats", "generic hab labels", "maintenance stickers");
        }
        return out;
    }

    static int[] minimumSizeFor(HousingVariantKind kind, PersonalWealthPropertyRules.WealthTier tier) {
        int wealthBonus = tier == null ? 0 : Math.max(0, tier.rank - PersonalWealthPropertyRules.WealthTier.WORKING.rank);
        return switch (kind) {
            case NO_FIXED_HOUSING -> new int[]{3, 3};
            case BUNK_SPACE -> new int[]{3, 4};
            case PERSONAL_DORMITORY -> new int[]{4, 4};
            case SHARED_APARTMENT -> new int[]{8 + wealthBonus, 6 + wealthBonus};
            case PRIVATE_APARTMENT -> new int[]{10 + wealthBonus, 8 + wealthBonus};
            case LARGE_APARTMENT -> new int[]{12 + wealthBonus, 10 + wealthBonus};
            case ESTATE_SUITE -> new int[]{16 + wealthBonus * 2, 12 + wealthBonus * 2};
            case SECURED_FACTION_QUARTERS -> new int[]{8 + wealthBonus, 7 + wealthBonus};
        };
    }

    static boolean isSecuredStyle(FactionHousingStyle style) {
        return style == FactionHousingStyle.SECURITY_BARRACKS
                || style == FactionHousingStyle.FORGE_DORMITORY
                || style == FactionHousingStyle.NOBLE_ESTATE
                || style == FactionHousingStyle.GANG_HIDEOUT
                || style == FactionHousingStyle.CULT_CELL
                || style == FactionHousingStyle.ROGUE_MACHINE_CELL;
    }

    static String spawnKey(FactionHousingStyle style, Object object) {
        String styleKey = (style == null ? FactionHousingStyle.NEUTRAL_HAB : style).name().toLowerCase(Locale.ROOT);
        String objectKey = object == null ? "unknown" : object.toString().toLowerCase(Locale.ROOT);
        return "housing." + styleKey + "." + objectKey;
    }

    static String labelFor(FactionHousingStyle style, HousingVariantKind kind, PersonalWealthPropertyRules.WealthTier tier) {
        return (tier == null ? "Working" : tier.label) + " "
                + (style == null ? "Neutral" : style.name().replace('_', ' ').toLowerCase(Locale.ROOT)) + " "
                + (kind == null ? "housing" : kind.name().replace('_', ' ').toLowerCase(Locale.ROOT));
    }
}
