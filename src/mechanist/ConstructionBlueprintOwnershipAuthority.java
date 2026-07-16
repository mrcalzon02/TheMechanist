package mechanist;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;

/** Live ownership, vendor, and readability rules for faction construction blueprints. */
final class ConstructionBlueprintOwnershipAuthority {
    private static final String ID_PREFIX = "construction.";

    private ConstructionBlueprintOwnershipAuthority() {}

    static boolean requiresLicensedBlueprint(BuildRecipe recipe) {
        return recipe != null && recipe.requiredFaction != null && recipe.requiredFaction != Faction.NONE;
    }

    static String blueprintId(BuildRecipe recipe) {
        if (recipe == null || recipe.name == null || recipe.name.isBlank()) return "";
        return ID_PREFIX + ContainerIdentityApi.safeToken(recipe.name).replace('_', '.');
    }

    static String blueprintItemName(BuildRecipe recipe) {
        return recipe == null ? "Unknown construction blueprint" : recipe.name + " licensed blueprint";
    }

    static int blueprintPrice(BuildRecipe recipe) {
        if (recipe == null) return 20;
        return Math.max(20, 14 + recipe.supplyCost * 2 + recipe.partCost * 3
                + recipe.reqMechanics * 2 + recipe.reqIntellect * 2);
    }

    static boolean owns(GamePanel game, BuildRecipe recipe) {
        if (!requiresLicensedBlueprint(recipe)) return true;
        return game != null && game.unlockedConstructionBlueprints.contains(blueprintId(recipe));
    }

    static String requirementProblem(GamePanel game, BuildRecipe recipe) {
        if (owns(game, recipe)) return "ok";
        return "missing licensed blueprint: " + blueprintItemName(recipe) + "; buy it from a "
                + issuingFaction(recipe).label + " " + vendorLabel(recipe);
    }

    static String playerAccessLine(GamePanel game, BuildRecipe recipe) {
        if (!requiresLicensedBlueprint(recipe)) {
            return "Blueprint access: PUBLIC - no licensed faction folio is required.";
        }
        if (owns(game, recipe)) {
            return "Blueprint access: OWNED - " + blueprintItemName(recipe)
                    + " is recorded in this run's construction library.";
        }
        return "Blueprint access: LOCKED - buy " + blueprintItemName(recipe) + " from a "
                + issuingFaction(recipe).label + " " + vendorLabel(recipe)
                + "; " + marketAccessRequirement(recipe) + ".";
    }

    static void registerCatalogItems(LinkedHashMap<String, ItemDef> catalog) {
        if (catalog == null) return;
        for (BuildRecipe recipe : BuildRecipe.allBuildRecipes()) {
            if (!requiresLicensedBlueprint(recipe)) continue;
            ItemCatalog.add(catalog, blueprintItemName(recipe), "knowledge/blueprint/construction",
                    blueprintPrice(recipe), issuingFaction(recipe).label + " " + vendorLabel(recipe) + "s",
                    "a licensed construction folio for " + recipe.name + ", with serialized plans, material callouts, and faction approval marks",
                    "purchase records the named construction blueprint as a persistent run unlock; retain the folio as physical proof",
                    false);
        }
    }

    static void applyVendorStock(TraderSession trader, Faction vendorFaction,
                                 FactionCriticalVendorPlacementAuthority.Category category) {
        if (trader == null || category == null) return;
        Faction normalizedVendor = FactionInventoryStockAuthority.normalizeFaction(vendorFaction);
        for (BuildRecipe recipe : BuildRecipe.allBuildRecipes()) {
            if (!requiresLicensedBlueprint(recipe)
                    || issuingFaction(recipe) != normalizedVendor
                    || category != vendorCategory(recipe)) continue;
            String id = blueprintId(recipe);
            boolean exists = false;
            for (TradeOffer offer : trader.offers) {
                if (offer == null) continue;
                if (id.equals(offer.constructionBlueprintId)
                        || ItemQuality.namesMatch(offer.name, blueprintItemName(recipe))) {
                    exists = true;
                    break;
                }
            }
            if (exists) continue;
            ItemDef definition = ItemCatalog.get(blueprintItemName(recipe));
            TradeOffer offer = new TradeOffer(blueprintItemName(recipe),
                    definition == null ? "knowledge/blueprint/construction" : definition.category,
                    blueprintPrice(recipe),
                    "Licensed by " + issuingFaction(recipe).label + ". Purchase permanently unlocks "
                            + recipe.name + " construction for this run; ordinary knowledge, material, workbench, and placement requirements still apply.");
            offer.constructionBlueprintId = id;
            trader.offers.add(offer);
        }
    }

    static void removeOwnedOffers(TraderSession trader, Set<String> ownedBlueprints) {
        if (trader == null || ownedBlueprints == null || ownedBlueprints.isEmpty()) return;
        trader.offers.removeIf(offer -> offer != null && !offer.constructionBlueprintId.isBlank()
                && ownedBlueprints.contains(offer.constructionBlueprintId));
    }

    static String purchaseBlock(GamePanel game, TradeOffer offer) {
        if (offer == null || offer.constructionBlueprintId == null || offer.constructionBlueprintId.isBlank()) return "";
        return game != null && game.unlockedConstructionBlueprints.contains(offer.constructionBlueprintId)
                ? "this construction blueprint is already recorded in your library" : "";
    }

    static String grantPurchasedBlueprint(GamePanel game, TradeOffer offer) {
        if (game == null || offer == null || offer.constructionBlueprintId == null
                || offer.constructionBlueprintId.isBlank()) return "";
        BuildRecipe recipe = recipeForId(offer.constructionBlueprintId);
        if (recipe == null || !game.unlockedConstructionBlueprints.add(offer.constructionBlueprintId)) return "";
        return recipe.name;
    }

    static BuildRecipe recipeForId(String id) {
        if (id == null || id.isBlank()) return null;
        for (BuildRecipe recipe : BuildRecipe.allBuildRecipes()) if (id.equals(blueprintId(recipe))) return recipe;
        return null;
    }

    static ArrayList<BuildRecipe> licensedRecipesForFaction(Faction faction) {
        Faction normalized = FactionInventoryStockAuthority.normalizeFaction(faction);
        ArrayList<BuildRecipe> out = new ArrayList<>();
        for (BuildRecipe recipe : BuildRecipe.allBuildRecipes())
            if (requiresLicensedBlueprint(recipe) && issuingFaction(recipe) == normalized) out.add(recipe);
        return out;
    }

    static Faction issuingFactionFor(BuildRecipe recipe) { return issuingFaction(recipe); }
    static FactionCriticalVendorPlacementAuthority.Category vendorCategoryFor(BuildRecipe recipe) {
        return vendorCategory(recipe);
    }
    static String vendorRoleFor(BuildRecipe recipe) { return vendorCategory(recipe).role; }
    static boolean isLiveVendorFor(NpcEntity npc, BuildRecipe recipe) {
        return npc != null && npc.hp > 0 && recipe != null
                && vendorCategory(recipe) == FactionCriticalVendorPlacementAuthority.categoryForRole(npc.role)
                && FactionIdentityAuthority.sameFamily(npc.faction, issuingFaction(recipe));
    }

    private static Faction issuingFaction(BuildRecipe recipe) {
        return FactionInventoryStockAuthority.normalizeFaction(recipe == null ? Faction.NONE : recipe.requiredFaction);
    }

    private static FactionCriticalVendorPlacementAuthority.Category vendorCategory(BuildRecipe recipe) {
        Faction faction = issuingFaction(recipe);
        if (faction == Faction.IMPERIAL_GUARD || faction == Faction.CIVIC_WARDENS)
            return FactionCriticalVendorPlacementAuthority.Category.ARMORY;
        if (faction == Faction.NOBLE) return FactionCriticalVendorPlacementAuthority.Category.LUXURY;
        return FactionCriticalVendorPlacementAuthority.Category.INDUSTRIAL;
    }

    private static String vendorLabel(BuildRecipe recipe) {
        return vendorCategory(recipe).role.toLowerCase(Locale.ROOT);
    }

    private static String marketAccessRequirement(BuildRecipe recipe) {
        return switch (vendorCategory(recipe)) {
            case ARMORY -> "favorable standing 10, faction membership, or permit-based service access is required";
            case LUXURY -> "neutral standing 0, faction membership, patronage, invitation, or licensed commerce is required";
            default -> "favorable standing 5, faction membership, or permit-based service access is required";
        };
    }
}
