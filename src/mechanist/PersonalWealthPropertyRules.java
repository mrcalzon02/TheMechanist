package mechanist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Personal wealth/property contract for residents, faction members, and named
 * entities.  Generation should use this before assigning pets, private vehicles,
 * personal rooms, apartments, storage lockers, and private belongings.
 */
final class PersonalWealthPropertyRules {
    enum WealthTier {
        DESTITUTE(0, "Destitute"),
        POOR(1, "Poor"),
        WORKING(2, "Working"),
        COMFORTABLE(3, "Comfortable"),
        AFFLUENT(4, "Affluent"),
        NOBLE(5, "Noble"),
        OLIGARCH(6, "Oligarchic");

        final int rank;
        final String label;
        WealthTier(int rank, String label) { this.rank = rank; this.label = label; }
    }

    enum HousingType {
        NONE,
        BUNK,
        PERSONAL_DORMITORY,
        SHARED_APARTMENT,
        PRIVATE_APARTMENT,
        LARGE_APARTMENT,
        ESTATE_SUITE,
        SECURED_FACTION_QUARTERS
    }

    enum PersonalAsset {
        PET,
        PERSONAL_VEHICLE,
        WORK_VEHICLE_ACCESS,
        PRIVATE_GEAR,
        ADVANCED_EQUIPMENT,
        WEAPON_LOCKER,
        PERSONAL_STORAGE_LOCKER,
        APARTMENT_STORAGE,
        VALUABLES_CACHE,
        FACTION_PROPERTY
    }

    static final class WealthProfile {
        final WealthTier tier;
        final HousingType housingType;
        final int creditValue;
        final boolean mayOwnPet;
        final boolean mayOwnPersonalVehicle;
        final boolean mayHavePrivateStorage;
        final boolean mayHaveApartment;
        final boolean mayHavePersonalDormitory;
        final ArrayList<PersonalAsset> assets;
        final String label;

        WealthProfile(WealthTier tier, HousingType housingType, int creditValue,
                      boolean mayOwnPet, boolean mayOwnPersonalVehicle, boolean mayHavePrivateStorage,
                      boolean mayHaveApartment, boolean mayHavePersonalDormitory,
                      List<PersonalAsset> assets, String label) {
            this.tier = tier == null ? WealthTier.WORKING : tier;
            this.housingType = housingType == null ? HousingType.BUNK : housingType;
            this.creditValue = Math.max(0, creditValue);
            this.mayOwnPet = mayOwnPet;
            this.mayOwnPersonalVehicle = mayOwnPersonalVehicle;
            this.mayHavePrivateStorage = mayHavePrivateStorage;
            this.mayHaveApartment = mayHaveApartment;
            this.mayHavePersonalDormitory = mayHavePersonalDormitory;
            this.assets = new ArrayList<>(assets == null ? List.of() : assets);
            this.label = label == null ? this.tier.label : label;
        }

        List<PersonalAsset> assets() { return Collections.unmodifiableList(assets); }
    }

    static final class BelongingsPlan {
        final WealthProfile profile;
        final ArrayList<String> belongings;
        final ArrayList<String> storageFixtures;
        final ArrayList<String> roomRequirements;

        BelongingsPlan(WealthProfile profile, List<String> belongings, List<String> storageFixtures, List<String> roomRequirements) {
            this.profile = profile;
            this.belongings = new ArrayList<>(belongings == null ? List.of() : belongings);
            this.storageFixtures = new ArrayList<>(storageFixtures == null ? List.of() : storageFixtures);
            this.roomRequirements = new ArrayList<>(roomRequirements == null ? List.of() : roomRequirements);
        }

        List<String> belongings() { return Collections.unmodifiableList(belongings); }
        List<String> storageFixtures() { return Collections.unmodifiableList(storageFixtures); }
        List<String> roomRequirements() { return Collections.unmodifiableList(roomRequirements); }
    }

    private PersonalWealthPropertyRules() {}

    static WealthProfile profileForFactionRole(Faction faction, String role, long seed) {
        WealthTier tier = baseTierForFaction(faction);
        String safeRole = role == null ? "" : role.toLowerCase(Locale.ROOT);
        if (safeRole.contains("noble") || safeRole.contains("lord") || safeRole.contains("director")) tier = max(tier, WealthTier.NOBLE);
        else if (safeRole.contains("captain") || safeRole.contains("magistrate") || safeRole.contains("guild") || safeRole.contains("overseer")) tier = max(tier, WealthTier.AFFLUENT);
        else if (safeRole.contains("officer") || safeRole.contains("foreman") || safeRole.contains("adept") || safeRole.contains("specialist")) tier = max(tier, WealthTier.COMFORTABLE);
        else if (safeRole.contains("scav") || safeRole.contains("mutant") || safeRole.contains("beggar")) tier = min(tier, WealthTier.POOR);
        Random rng = new Random(seed ^ (faction == null ? 0L : faction.ordinal() * 0x9E3779B97F4A7C15L) ^ safeRole.hashCode());
        if (rng.nextInt(100) < 8 && tier.rank < WealthTier.NOBLE.rank) tier = WealthTier.values()[Math.min(WealthTier.NOBLE.rank, tier.rank + 1)];
        if (rng.nextInt(100) < 5 && tier.rank > WealthTier.DESTITUTE.rank) tier = WealthTier.values()[Math.max(WealthTier.DESTITUTE.rank, tier.rank - 1)];
        return profileForTier(tier, faction);
    }

    static WealthTier baseTierForFaction(Faction faction) {
        if (faction == null) return WealthTier.WORKING;
        switch (faction) {
            case NOBLE, NOBLE_HOUSE_VARN, NOBLE_HOUSE_KASTOR, NOBLE_HOUSE_MORVAIN, NOBLE_HOUSE_CYRA, NOBLE_HOUSE_DRAKE, NOBLE_HOUSE_TOLL, NOBLE_HOUSE_OSSUARY:
                return WealthTier.NOBLE;
            case MECHANICUS, MECHANIST_COLLEGIA, MECHANICUS_CLOISTER_RED, MECHANICUS_CLOISTER_RUST, MECHANICUS_CLOISTER_VOID, ARBITES, CIVIC_WARDENS, IMPERIAL_GUARD:
                return WealthTier.COMFORTABLE;
            case ADMINISTRATUM, CIVIC_LEDGER_OFFICE, INN, MINISTORUM, SORORITAS:
                return WealthTier.WORKING;
            case HIVER, HIVER_BLOCK_AUREL, HIVER_BLOCK_MARROW, HIVER_BLOCK_SUMPLEDGER:
                return WealthTier.WORKING;
            case BANDIT, GANGER_IRON_RATS, GANGER_BLACK_SUMP, GANGER_CANDLE_JACKS, GANGER_RED_GRIN, GANGER_CHAIN_SAINTS, GANGER_ASH_MARKET, GANGER_WIRE_WOLVES, GANGER_DROWNED_9TH:
                return WealthTier.POOR;
            case SCAVENGER, MUTANT:
                return WealthTier.POOR;
            default:
                return WealthTier.WORKING;
        }
    }

    static WealthProfile profileForTier(WealthTier tier, Faction faction) {
        WealthTier safe = tier == null ? WealthTier.WORKING : tier;
        ArrayList<PersonalAsset> assets = new ArrayList<>();
        boolean factionProperty = faction != null && faction != Faction.NONE;
        switch (safe) {
            case DESTITUTE:
                assets.add(PersonalAsset.PERSONAL_STORAGE_LOCKER);
                return new WealthProfile(safe, HousingType.NONE, 5, false, false, false, false, false, assets, "destitute belongings");
            case POOR:
                assets.add(PersonalAsset.PERSONAL_STORAGE_LOCKER);
                if (factionProperty) assets.add(PersonalAsset.FACTION_PROPERTY);
                return new WealthProfile(safe, HousingType.BUNK, 25, false, false, true, false, false, assets, "poor personal kit");
            case WORKING:
                assets.add(PersonalAsset.PERSONAL_STORAGE_LOCKER);
                assets.add(PersonalAsset.PRIVATE_GEAR);
                if (factionProperty) assets.add(PersonalAsset.FACTION_PROPERTY);
                return new WealthProfile(safe, HousingType.PERSONAL_DORMITORY, 90, true, false, true, false, true, assets, "working personal property");
            case COMFORTABLE:
                assets.add(PersonalAsset.PERSONAL_STORAGE_LOCKER);
                assets.add(PersonalAsset.PRIVATE_GEAR);
                assets.add(PersonalAsset.WORK_VEHICLE_ACCESS);
                assets.add(PersonalAsset.APARTMENT_STORAGE);
                return new WealthProfile(safe, HousingType.SHARED_APARTMENT, 250, true, true, true, true, true, assets, "comfortable personal property");
            case AFFLUENT:
                assets.add(PersonalAsset.PERSONAL_VEHICLE);
                assets.add(PersonalAsset.PET);
                assets.add(PersonalAsset.PRIVATE_GEAR);
                assets.add(PersonalAsset.ADVANCED_EQUIPMENT);
                assets.add(PersonalAsset.APARTMENT_STORAGE);
                assets.add(PersonalAsset.VALUABLES_CACHE);
                return new WealthProfile(safe, HousingType.PRIVATE_APARTMENT, 700, true, true, true, true, true, assets, "affluent property");
            case NOBLE:
                assets.add(PersonalAsset.PERSONAL_VEHICLE);
                assets.add(PersonalAsset.PET);
                assets.add(PersonalAsset.PRIVATE_GEAR);
                assets.add(PersonalAsset.ADVANCED_EQUIPMENT);
                assets.add(PersonalAsset.WEAPON_LOCKER);
                assets.add(PersonalAsset.APARTMENT_STORAGE);
                assets.add(PersonalAsset.VALUABLES_CACHE);
                return new WealthProfile(safe, HousingType.ESTATE_SUITE, 2500, true, true, true, true, true, assets, "noble property");
            case OLIGARCH:
            default:
                assets.add(PersonalAsset.PERSONAL_VEHICLE);
                assets.add(PersonalAsset.PET);
                assets.add(PersonalAsset.PRIVATE_GEAR);
                assets.add(PersonalAsset.ADVANCED_EQUIPMENT);
                assets.add(PersonalAsset.WEAPON_LOCKER);
                assets.add(PersonalAsset.APARTMENT_STORAGE);
                assets.add(PersonalAsset.VALUABLES_CACHE);
                return new WealthProfile(safe, HousingType.ESTATE_SUITE, 8000, true, true, true, true, true, assets, "oligarchic property");
        }
    }

    static BelongingsPlan belongingsFor(WealthProfile profile, long seed) {
        WealthProfile safe = profile == null ? profileForTier(WealthTier.WORKING, Faction.NONE) : profile;
        ArrayList<String> belongings = new ArrayList<>();
        ArrayList<String> storage = new ArrayList<>();
        ArrayList<String> rooms = new ArrayList<>();
        belongings.add("personal clothes");
        belongings.add("daily ration tokens");
        if (safe.tier.rank >= WealthTier.WORKING.rank) belongings.add("work tools");
        if (safe.mayOwnPet) belongings.add("pet supplies");
        if (safe.mayOwnPersonalVehicle) belongings.add("vehicle keys and maintenance papers");
        if (safe.assets.contains(PersonalAsset.PRIVATE_GEAR)) belongings.add("private gear bundle");
        if (safe.assets.contains(PersonalAsset.ADVANCED_EQUIPMENT)) belongings.add("advanced equipment case");
        if (safe.assets.contains(PersonalAsset.WEAPON_LOCKER)) belongings.add("secured arms ledger");
        if (safe.assets.contains(PersonalAsset.VALUABLES_CACHE)) belongings.add("valuables cache");
        if (safe.mayHavePrivateStorage) storage.add("personal locker");
        if (safe.assets.contains(PersonalAsset.APARTMENT_STORAGE)) storage.add("apartment storage cabinet");
        if (safe.assets.contains(PersonalAsset.VALUABLES_CACHE)) storage.add("hidden strongbox");
        switch (safe.housingType) {
            case PERSONAL_DORMITORY -> rooms.add("2x4 personal dormitory room with cot, sink, dresser, cabinet");
            case SHARED_APARTMENT -> rooms.add("shared apartment room cluster with private storage");
            case PRIVATE_APARTMENT -> rooms.add("private apartment with bedroom, bathroom, living room, dining nook");
            case LARGE_APARTMENT -> rooms.add("large apartment with expanded storage and pet space");
            case ESTATE_SUITE -> rooms.add("estate suite with secured storage and private vehicle access");
            case SECURED_FACTION_QUARTERS -> rooms.add("secured faction quarters with equipment locker");
            case BUNK -> rooms.add("bunk space with assigned locker");
            default -> rooms.add("no assigned room; belongings carried or cached");
        }
        return new BelongingsPlan(safe, belongings, storage, rooms);
    }

    private static WealthTier max(WealthTier a, WealthTier b) { return a.rank >= b.rank ? a : b; }
    private static WealthTier min(WealthTier a, WealthTier b) { return a.rank <= b.rank ? a : b; }
}
