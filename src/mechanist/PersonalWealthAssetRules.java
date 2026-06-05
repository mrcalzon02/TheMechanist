package mechanist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Personal wealth-to-assets contract for generated residents, faction members,
 * dormitories, apartments, vehicles, pets, carried gear, equipment, and personal
 * storage contents.
 */
final class PersonalWealthAssetRules {
    enum WealthTier {
        DESTITUTE(0, 9, "Destitute"),
        POOR(10, 24, "Poor"),
        WORKING(25, 44, "Working"),
        COMFORTABLE(45, 64, "Comfortable"),
        AFFLUENT(65, 84, "Affluent"),
        ELITE(85, 100, "Elite");

        final int min;
        final int max;
        final String label;

        WealthTier(int min, int max, String label) {
            this.min = min;
            this.max = max;
            this.label = label;
        }

        static WealthTier fromScore(int score) {
            int s = Math.max(0, Math.min(100, score));
            for (WealthTier tier : values()) if (s >= tier.min && s <= tier.max) return tier;
            return WORKING;
        }
    }

    enum HousingEntitlement {
        NONE,
        BUNK,
        PERSONAL_DORMITORY,
        SHARED_APARTMENT,
        PRIVATE_APARTMENT,
        LUXURY_APARTMENT
    }

    static final class PersonalAssetProfile {
        final String entityRole;
        final String factionId;
        final int wealthScore;
        final WealthTier wealthTier;
        final HousingEntitlement housing;
        final boolean mayOwnPet;
        final boolean mayOwnPersonalVehicle;
        final int carriedGearBudget;
        final int storedBelongingsBudget;
        final int personalStorageSlots;
        final ArrayList<String> assetTags;

        PersonalAssetProfile(String entityRole, String factionId, int wealthScore, HousingEntitlement housing,
                             boolean mayOwnPet, boolean mayOwnPersonalVehicle, int carriedGearBudget,
                             int storedBelongingsBudget, int personalStorageSlots, List<String> assetTags) {
            this.entityRole = clean(entityRole, "resident");
            this.factionId = clean(factionId, "neutral");
            this.wealthScore = Math.max(0, Math.min(100, wealthScore));
            this.wealthTier = WealthTier.fromScore(this.wealthScore);
            this.housing = housing == null ? HousingEntitlement.BUNK : housing;
            this.mayOwnPet = mayOwnPet;
            this.mayOwnPersonalVehicle = mayOwnPersonalVehicle;
            this.carriedGearBudget = Math.max(0, carriedGearBudget);
            this.storedBelongingsBudget = Math.max(0, storedBelongingsBudget);
            this.personalStorageSlots = Math.max(0, personalStorageSlots);
            this.assetTags = assetTags == null ? new ArrayList<>() : new ArrayList<>(assetTags);
        }

        List<String> assetTags() { return Collections.unmodifiableList(assetTags); }
    }

    private PersonalWealthAssetRules() {}

    static PersonalAssetProfile profileFor(String entityRole, String factionId, int baseWealthScore, Random rng) {
        Random safeRng = rng == null ? new Random(0L) : rng;
        int score = adjustedWealthScore(entityRole, factionId, baseWealthScore, safeRng);
        WealthTier tier = WealthTier.fromScore(score);
        HousingEntitlement housing = housingFor(tier, entityRole, factionId);
        boolean pet = tier.ordinal() >= WealthTier.WORKING.ordinal() && safeRng.nextInt(100) < petChance(tier, entityRole);
        boolean vehicle = tier.ordinal() >= WealthTier.COMFORTABLE.ordinal() && safeRng.nextInt(100) < vehicleChance(tier, entityRole, factionId);
        int carried = carriedGearBudget(tier, entityRole, factionId);
        int stored = storedBelongingsBudget(tier, housing);
        int slots = storageSlots(housing, tier);
        ArrayList<String> tags = new ArrayList<>();
        tags.add("wealth:" + tier.label.toLowerCase(Locale.ROOT));
        tags.add("housing:" + housing.name().toLowerCase(Locale.ROOT));
        if (pet) tags.add("pet_eligible");
        if (vehicle) tags.add("personal_vehicle_eligible");
        if (tier.ordinal() >= WealthTier.AFFLUENT.ordinal()) tags.add("valuable_personal_storage");
        if (isSecurityRole(entityRole, factionId)) tags.add("weapon_or_armor_priority");
        return new PersonalAssetProfile(entityRole, factionId, score, housing, pet, vehicle, carried, stored, slots, tags);
    }

    static int adjustedWealthScore(String entityRole, String factionId, int base, Random rng) {
        int score = Math.max(0, Math.min(100, base));
        String role = clean(entityRole, "").toLowerCase(Locale.ROOT);
        String faction = clean(factionId, "").toLowerCase(Locale.ROOT);
        if (role.contains("noble") || faction.contains("noble")) score += 35;
        if (role.contains("officer") || role.contains("manager") || role.contains("magistrate")) score += 18;
        if (role.contains("merchant") || role.contains("guild") || faction.contains("guild")) score += 16;
        if (role.contains("arbite") || role.contains("pdf") || role.contains("security")) score += 10;
        if (role.contains("worker") || role.contains("servant")) score -= 6;
        if (role.contains("scav") || role.contains("outcast") || faction.contains("outcast")) score -= 18;
        if (rng != null) score += rng.nextInt(17) - 8;
        return Math.max(0, Math.min(100, score));
    }

    static HousingEntitlement housingFor(WealthTier tier, String entityRole, String factionId) {
        String role = clean(entityRole, "").toLowerCase(Locale.ROOT);
        String faction = clean(factionId, "").toLowerCase(Locale.ROOT);
        if (tier == WealthTier.DESTITUTE) return HousingEntitlement.NONE;
        if (role.contains("servitor") || role.contains("prisoner")) return HousingEntitlement.BUNK;
        if (tier == WealthTier.POOR) return HousingEntitlement.BUNK;
        if (tier == WealthTier.WORKING) return role.contains("family") || faction.contains("residential") ? HousingEntitlement.SHARED_APARTMENT : HousingEntitlement.PERSONAL_DORMITORY;
        if (tier == WealthTier.COMFORTABLE) return HousingEntitlement.PRIVATE_APARTMENT;
        if (tier == WealthTier.AFFLUENT) return HousingEntitlement.PRIVATE_APARTMENT;
        return HousingEntitlement.LUXURY_APARTMENT;
    }

    static int vehicleChance(WealthTier tier, String entityRole, String factionId) {
        String role = clean(entityRole, "").toLowerCase(Locale.ROOT);
        String faction = clean(factionId, "").toLowerCase(Locale.ROOT);
        int chance = switch (tier) {
            case DESTITUTE, POOR, WORKING -> 0;
            case COMFORTABLE -> 18;
            case AFFLUENT -> 45;
            case ELITE -> 75;
        };
        if (role.contains("driver") || role.contains("merchant") || role.contains("officer")) chance += 15;
        if (faction.contains("noble") || faction.contains("guild")) chance += 12;
        if (faction.contains("outcast") || role.contains("servant")) chance -= 12;
        return Math.max(0, Math.min(95, chance));
    }

    static int petChance(WealthTier tier, String entityRole) {
        int chance = switch (tier) {
            case DESTITUTE -> 2;
            case POOR -> 5;
            case WORKING -> 10;
            case COMFORTABLE -> 18;
            case AFFLUENT -> 28;
            case ELITE -> 36;
        };
        String role = clean(entityRole, "").toLowerCase(Locale.ROOT);
        if (role.contains("servitor") || role.contains("prisoner")) chance = 0;
        return chance;
    }

    static int carriedGearBudget(WealthTier tier, String entityRole, String factionId) {
        int budget = switch (tier) {
            case DESTITUTE -> 1;
            case POOR -> 2;
            case WORKING -> 4;
            case COMFORTABLE -> 7;
            case AFFLUENT -> 10;
            case ELITE -> 14;
        };
        if (isSecurityRole(entityRole, factionId)) budget += 6;
        return budget;
    }

    static int storedBelongingsBudget(WealthTier tier, HousingEntitlement housing) {
        int base = switch (tier) {
            case DESTITUTE -> 0;
            case POOR -> 2;
            case WORKING -> 5;
            case COMFORTABLE -> 9;
            case AFFLUENT -> 14;
            case ELITE -> 22;
        };
        if (housing == HousingEntitlement.PRIVATE_APARTMENT) base += 4;
        if (housing == HousingEntitlement.LUXURY_APARTMENT) base += 10;
        return base;
    }

    static int storageSlots(HousingEntitlement housing, WealthTier tier) {
        int base = switch (housing) {
            case NONE -> 0;
            case BUNK -> 1;
            case PERSONAL_DORMITORY -> 2;
            case SHARED_APARTMENT -> 3;
            case PRIVATE_APARTMENT -> 5;
            case LUXURY_APARTMENT -> 8;
        };
        if (tier.ordinal() >= WealthTier.AFFLUENT.ordinal()) base += 2;
        return base;
    }

    static boolean shouldReserveResidentParking(PersonalAssetProfile profile) {
        return profile != null && profile.mayOwnPersonalVehicle;
    }

    static boolean isSecurityRole(String entityRole, String factionId) {
        String role = clean(entityRole, "").toLowerCase(Locale.ROOT);
        String faction = clean(factionId, "").toLowerCase(Locale.ROOT);
        return role.contains("guard") || role.contains("soldier") || role.contains("arbite") || role.contains("security") || role.contains("officer") || faction.contains("pdf") || faction.contains("arbite");
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().replace('\n', ' ');
    }
}
