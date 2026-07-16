package mechanist;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;

/** Smoke for live faction blueprint vendors, purchases, build gating, and persistence. */
final class Milestone05ConstructionBlueprintOwnershipSmoke {
    public static void main(String[] args) throws Exception {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();

        BuildRecipe publicRecipe = recipe("Storage Crate");
        BuildRecipe guardRecipe = recipe("Sandbag Line");
        BuildRecipe arbitesRecipe = recipe("Razor Wire Coil");
        BuildRecipe nobleRecipe = recipe("Gilded Sentry Turret");
        BuildRecipe mechanistRecipe = recipe("EMM Micro Forge");

        require(ConstructionBlueprintOwnershipAuthority.owns(game, publicRecipe),
                "public starter construction should not require a licensed blueprint");
        requireContains(ConstructionBlueprintOwnershipAuthority.playerAccessLine(game, publicRecipe),
                "Blueprint access: PUBLIC", "public blueprint access line");
        require(!ConstructionBlueprintOwnershipAuthority.owns(game, guardRecipe),
                "restricted Guard construction should begin locked");
        requireContains(game.visibleBuildRecipeLabel(0, guardRecipe), "[LOCKED] Sandbag Line",
                "locked build-menu label");
        requireContains(game.buildRequirementProblem(guardRecipe), "missing licensed blueprint",
                "restricted construction requirement");
        requireContains(game.buildRequirementProblem(guardRecipe),
                FactionInventoryStockAuthority.normalizeFaction(guardRecipe.requiredFaction).label + " armory trader",
                "restricted construction acquisition source");
        requireLineContains(ConstructionReadabilityAuthority.detailLines(game, guardRecipe,
                        game.playerX, game.playerY),
                "Blueprint access: LOCKED", "locked construction detail");

        require(ItemCatalog.get(ConstructionBlueprintOwnershipAuthority.blueprintItemName(guardRecipe)) != null,
                "named Guard blueprint should be an inspectable catalog item");
        require(ItemCatalog.get(ConstructionBlueprintOwnershipAuthority.blueprintItemName(arbitesRecipe)) != null,
                "named Arbites blueprint should be an inspectable catalog item");
        require(ItemCatalog.get(ConstructionBlueprintOwnershipAuthority.blueprintItemName(nobleRecipe)) != null,
                "named Noble blueprint should be an inspectable catalog item");
        require(ItemCatalog.get(ConstructionBlueprintOwnershipAuthority.blueprintItemName(mechanistRecipe)) != null,
                "named Mechanist blueprint should be an inspectable catalog item");

        TraderSession guardArmory = vendor(Faction.IMPERIAL_GUARD,
                FactionCriticalVendorPlacementAuthority.Category.ARMORY);
        TradeOffer guardOffer = offerFor(guardArmory, guardRecipe);
        require(guardOffer != null, "Guard armory should stock its licensed construction blueprint");
        require(offerFor(guardArmory, arbitesRecipe) == null,
                "Guard armory should not stock Arbites construction blueprints");

        TraderSession arbitesArmory = vendor(Faction.CIVIC_WARDENS,
                FactionCriticalVendorPlacementAuthority.Category.ARMORY);
        require(offerFor(arbitesArmory, arbitesRecipe) != null,
                "Civic Wardens armory should stock normalized Arbites blueprints");
        TraderSession nobleBroker = vendor(Faction.NOBLE,
                FactionCriticalVendorPlacementAuthority.Category.LUXURY);
        require(offerFor(nobleBroker, nobleRecipe) != null,
                "Noble broker should stock Noble construction blueprints");
        TraderSession mechanistWorks = vendor(Faction.MECHANIST_COLLEGIA,
                FactionCriticalVendorPlacementAuthority.Category.INDUSTRIAL);
        require(offerFor(mechanistWorks, mechanistRecipe) != null,
                "Mechanist works factor should stock Mechanicus-family construction blueprints");

        game.activeTraderSession = guardArmory;
        game.selectedTradeOfferIndex = guardArmory.offers.indexOf(guardOffer);
        game.carriedScript = guardArmory.buyPrice(guardOffer) + 20;
        int scriptBeforeDeniedPurchase = game.carriedScript;
        Method buy = GamePanel.class.getDeclaredMethod("buySelectedTradeOffer");
        buy.setAccessible(true);
        buy.invoke(game);
        require(!ConstructionBlueprintOwnershipAuthority.owns(game, guardRecipe),
                "blocked market purchase must not grant a blueprint");
        require(game.carriedScript == scriptBeforeDeniedPurchase,
                "blocked market purchase must not spend script");

        game.factionStanding.put(Faction.IMPERIAL_GUARD, 10);
        int price = guardArmory.buyPrice(guardOffer);
        int scriptBeforePurchase = game.carriedScript;
        buy.invoke(game);
        require(ConstructionBlueprintOwnershipAuthority.owns(game, guardRecipe),
                "successful licensed purchase should grant the construction blueprint");
        require(game.carriedScript == scriptBeforePurchase - price,
                "successful blueprint purchase should spend the displayed price");
        require(game.inventory.contains(guardOffer.name),
                "successful blueprint purchase should retain the physical folio");
        require(!guardArmory.offers.contains(guardOffer),
                "a learned blueprint should leave the active vendor shelf");
        requireContains(game.visibleBuildRecipeLabel(0, guardRecipe), "Sandbag Line",
                "unlocked build-menu label");
        require(!game.visibleBuildRecipeLabel(0, guardRecipe).contains("[LOCKED]"),
                "unlocked recipe should lose its locked menu marker");
        require("ok".equals(game.buildRequirementProblem(guardRecipe)),
                "owned Sandbag Line blueprint should pass non-material build requirements");
        requireLineContains(ConstructionReadabilityAuthority.detailLines(game, guardRecipe,
                        game.playerX, game.playerY),
                "Blueprint access: OWNED", "owned construction detail");
        requireContains(ConstructionBlueprintOwnershipAuthority.purchaseBlock(game, guardOffer),
                "already recorded", "duplicate blueprint purchase block");

        Properties saved = new Properties();
        Persistence.writeCore(game, saved);
        require(Persistence.decList(saved.getProperty("run.constructionBlueprints", ""))
                        .contains(ConstructionBlueprintOwnershipAuthority.blueprintId(guardRecipe)),
                "saved construction blueprint ledger should contain the learned blueprint ID");
        GamePanel restored = new GamePanel();
        if (restored.timer != null) restored.timer.stop();
        Persistence.readCore(restored, saved);
        require(ConstructionBlueprintOwnershipAuthority.owns(restored, guardRecipe),
                "construction blueprint ownership should survive save/load");
        TraderSession restoredGuardArmory = vendor(Faction.IMPERIAL_GUARD,
                FactionCriticalVendorPlacementAuthority.Category.ARMORY);
        ConstructionBlueprintOwnershipAuthority.removeOwnedOffers(restoredGuardArmory,
                restored.unlockedConstructionBlueprints);
        require(offerFor(restoredGuardArmory, guardRecipe) == null,
                "known blueprints should be filtered from newly opened vendor sessions");

        if (restored.timer != null) restored.timer.stop();
        if (game.timer != null) game.timer.stop();
        System.out.println("Milestone 05 construction blueprint ownership smoke passed.");
    }

    private static TraderSession vendor(Faction faction,
                                        FactionCriticalVendorPlacementAuthority.Category category) {
        TraderSession trader = new TraderSession();
        trader.name = faction.label + " test vendor";
        trader.marketFaction = faction;
        trader.marketCategory = category.id;
        FactionCriticalVendorPlacementAuthority.applyVendorStock(trader, npc(faction, category));
        return trader;
    }

    private static NpcEntity npc(Faction faction,
                                 FactionCriticalVendorPlacementAuthority.Category category) {
        NpcEntity npc = new NpcEntity();
        npc.faction = faction;
        npc.role = category.role;
        npc.name = faction.label + " test factor";
        return npc;
    }

    private static TradeOffer offerFor(TraderSession trader, BuildRecipe recipe) {
        String id = ConstructionBlueprintOwnershipAuthority.blueprintId(recipe);
        for (TradeOffer offer : trader.offers)
            if (offer != null && id.equals(offer.constructionBlueprintId)) return offer;
        return null;
    }

    private static BuildRecipe recipe(String name) {
        for (BuildRecipe recipe : BuildRecipe.allBuildRecipes())
            if (recipe.name.equals(name)) return recipe;
        throw new AssertionError("Missing build recipe: " + name);
    }

    private static void requireLineContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone05ConstructionBlueprintOwnershipSmoke() {}
}
