package mechanist;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/** Smoke for Phase 17.1 construction-blueprint offer readiness and purchase parity. */
final class Milestone05ConstructionBlueprintOfferReadinessSmoke {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.inventory.clear();
            game.baseStorage.clear();
            game.baseObjects.clear();
            game.unlockedConstructionBlueprints.clear();
            game.supplies = 0;
            game.machineParts = 0;
            game.gangHeat = 0;
            game.suspicion = 0;

            Method buy = GamePanel.class.getDeclaredMethod("buySelectedTradeOffer");
            buy.setAccessible(true);

            BuildRecipe guardRecipe = recipe("Sandbag Line");
            TraderSession guardArmory = vendor(Faction.IMPERIAL_GUARD,
                    FactionCriticalVendorPlacementAuthority.Category.ARMORY);
            TradeOffer guardOffer = offerFor(guardArmory, guardRecipe);
            require(guardOffer != null, "Guard armory should stock the Sandbag Line folio");
            game.factionStanding.put(Faction.IMPERIAL_GUARD, 0);
            game.carriedScript = guardArmory.buyPrice(guardOffer) + 50;

            FactionMarketAccessAuthority.Decision guardAccess = access(game, guardArmory, guardOffer);
            ConstructionBlueprintOfferReadinessAuthority.Readiness denied =
                    ConstructionBlueprintOfferReadinessAuthority.evaluate(game, guardArmory, guardOffer, guardAccess);
            require(denied.applies(), "licensed Guard folio should use blueprint offer readiness");
            require(denied.price() == guardArmory.buyPrice(guardOffer),
                    "readiness must use canonical live trader pricing");
            require(!denied.purchaseReady(), "neutral Guard buyer should be purchase-blocked");
            requireContains(denied.purchaseBlock(), "standing 10", "Guard purchase blocker");
            requireContains(denied.lines().toString(), "Concord Guard Armory Trader", "exact issuer and role");
            requireContains(denied.lines().toString(), "military restricted", "exact legal status");
            requireContains(denied.lines().toString(), "Effort preview:", "exact effort readback");
            requireContains(denied.lines().toString(), "Attention preview:", "exact attention readback");

            game.factionStanding.put(Faction.IMPERIAL_GUARD, 10);
            game.suspicion = 70;
            ConstructionBlueprintOfferReadinessAuthority.Readiness inspectionHold =
                    ConstructionBlueprintOfferReadinessAuthority.evaluate(game, guardArmory, guardOffer,
                            access(game, guardArmory, guardOffer));
            require(!inspectionHold.purchaseReady(), "high suspicion should retain the armory inspection hold");
            requireContains(inspectionHold.purchaseBlock(), "suspicion 70", "armory suspicion blocker");
            game.factionStanding.put(Faction.IMPERIAL_GUARD, 0);
            game.suspicion = 0;

            game.activeTraderSession = guardArmory;
            game.selectedTradeOfferIndex = guardArmory.offers.indexOf(guardOffer);
            int deniedScript = game.carriedScript;
            int deniedTurn = game.turn;
            List<String> deniedInventory = List.copyOf(game.inventory);
            Set<String> deniedUnlocks = Set.copyOf(game.unlockedConstructionBlueprints);
            int deniedShelfSize = guardArmory.offers.size();
            buy.invoke(game);
            require(game.carriedScript == deniedScript, "denied purchase must not spend script");
            require(game.turn == deniedTurn, "denied purchase must not spend time");
            require(game.inventory.equals(deniedInventory), "denied purchase must not transfer a folio");
            require(game.unlockedConstructionBlueprints.equals(deniedUnlocks),
                    "denied purchase must not grant a construction unlock");
            require(guardArmory.offers.size() == deniedShelfSize && guardArmory.offers.contains(guardOffer),
                    "denied purchase must not mutate the vendor shelf");

            BuildRecipe forgeRecipe = recipe("EMM Micro Forge");
            TraderSession forgeVendor = vendor(Faction.MECHANIST_COLLEGIA,
                    FactionCriticalVendorPlacementAuthority.Category.INDUSTRIAL);
            TradeOffer forgeOffer = offerFor(forgeVendor, forgeRecipe);
            require(forgeOffer != null, "Mechanist works factor should stock the Micro Forge folio");
            game.factionStanding.put(Faction.MECHANIST_COLLEGIA, 5);
            game.unlockedKnowledges.remove(forgeRecipe.requiredKnowledge);
            game.baseObjects.clear();
            game.inventory.clear();
            game.baseStorage.clear();
            game.supplies = 0;
            game.machineParts = 0;
            int forgePrice = forgeVendor.buyPrice(forgeOffer);
            game.carriedScript = forgePrice + 20;

            FactionMarketAccessAuthority.Decision forgeAccess = access(game, forgeVendor, forgeOffer);
            require(forgeAccess.allowed(), "favorable Mechanist standing should authorize the live folio offer");
            ConstructionBlueprintOfferReadinessAuthority.Readiness ready =
                    ConstructionBlueprintOfferReadinessAuthority.evaluate(game, forgeVendor, forgeOffer, forgeAccess);
            require(ready.purchaseReady(),
                    "post-purchase construction blockers must not block buying an authorized folio");
            require(!ready.postPurchaseCapabilityReady(),
                    "missing doctrine and workbench should remain visible after purchase readiness");
            requireContains(ready.capabilityBlockers().toString(), "Scrap-Forging Doctrine",
                    "post-purchase knowledge blocker");
            requireContains(ready.capabilityBlockers().toString(), "Scrap Workbench",
                    "post-purchase workbench blocker");
            requireContains(ready.materialState(), "NO MATERIALS AVAILABLE", "material readiness state");
            requireContains(ready.lines().toString(), "Mechanist Collegia Industrial Blueprint Trader",
                    "Mechanist issuer and role");
            requireContains(ready.lines().toString(), "forge license-bound", "Mechanist legal status");
            requireContains(ready.lines().toString(), "faction-only licensed blueprint",
                    "live market access class");
            requireContains(ready.lines().toString(), "do not block buying the folio",
                    "purchase versus construction boundary");
            requireContains(ready.lines().toString(), "Blueprint price: " + forgePrice,
                    "live blueprint price readback");
            requireContains(ready.lines().toString(), "Purchase: READY", "purchase verdict");
            requireContains(ready.lines().toString(), "Build after purchase: BLOCKED", "build verdict");

            TradeOffer ordinary = new TradeOffer("Tool bundle", "tool", 7, "ordinary works-factor stock");
            ConstructionBlueprintOfferReadinessAuthority.Readiness ordinaryReadiness =
                    ConstructionBlueprintOfferReadinessAuthority.evaluate(game, forgeVendor, ordinary,
                            access(game, forgeVendor, ordinary));
            require(!ordinaryReadiness.applies(), "ordinary trade offers must not use blueprint readiness");
            require(ordinaryReadiness.lines().isEmpty() && ordinaryReadiness.purchaseBlock().isBlank(),
                    "ordinary trade offers should receive no blueprint-only readback or blocker");

            game.activeTraderSession = forgeVendor;
            game.selectedTradeOfferIndex = forgeVendor.offers.indexOf(forgeOffer);
            int scriptBeforePurchase = game.carriedScript;
            int turnBeforePurchase = game.turn;
            buy.invoke(game);
            require(game.carriedScript == scriptBeforePurchase - forgePrice,
                    "successful folio purchase must spend the displayed live price");
            require(game.turn == turnBeforePurchase + 1, "successful folio purchase must spend one turn");
            require(ConstructionBlueprintOwnershipAuthority.owns(game, forgeRecipe),
                    "successful purchase must grant the exact construction unlock");
            require(game.inventory.contains(ConstructionBlueprintOwnershipAuthority.blueprintItemName(forgeRecipe)),
                    "successful purchase must retain the exact physical folio");
            require(!forgeVendor.offers.contains(forgeOffer),
                    "successful purchase must remove the learned folio from the active shelf");
            requireContains(game.buildRequirementProblem(forgeRecipe), "Scrap-Forging Doctrine",
                    "post-purchase construction knowledge blocker must remain active");

            ConstructionBlueprintOfferReadinessAuthority.Readiness owned =
                    ConstructionBlueprintOfferReadinessAuthority.evaluate(game, forgeVendor, forgeOffer, forgeAccess);
            require(!owned.purchaseReady(), "owned folio must no longer be purchase-ready");
            requireContains(owned.purchaseBlock(), "already recorded", "owned folio blocker");

            System.out.println("Milestone 05 construction blueprint offer readiness smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static FactionMarketAccessAuthority.Decision access(GamePanel game, TraderSession trader,
                                                                  TradeOffer offer) {
        Faction vendor = trader == null || trader.marketFaction == null ? Faction.NONE : trader.marketFaction;
        return FactionMarketAccessAuthority.evaluate(trader, offer,
                new FactionMarketAccessAuthority.AccessContext(game.playerFaction(),
                        game.factionStanding.getOrDefault(vendor, 0), game.gangHeat, game.suspicion,
                        game.unlockedSkillNodes, game.unlockedKnowledges, game.inventory,
                        game.world, game.worldTurn));
    }

    private static TraderSession vendor(Faction faction,
                                        FactionCriticalVendorPlacementAuthority.Category category) {
        TraderSession trader = new TraderSession();
        trader.name = faction.label + " test vendor";
        trader.archetype = category.role;
        trader.zoneLabel = "Readiness smoke market";
        NpcEntity npc = new NpcEntity();
        npc.faction = faction;
        npc.role = category.role;
        npc.name = trader.name;
        FactionCriticalVendorPlacementAuthority.applyVendorStock(trader, npc);
        return trader;
    }

    private static TradeOffer offerFor(TraderSession trader, BuildRecipe recipe) {
        String id = ConstructionBlueprintOwnershipAuthority.blueprintId(recipe);
        for (TradeOffer offer : trader.offers) {
            if (offer != null && id.equals(offer.constructionBlueprintId)) return offer;
        }
        return null;
    }

    private static BuildRecipe recipe(String name) {
        for (BuildRecipe recipe : BuildRecipe.allBuildRecipes()) {
            if (recipe != null && name.equals(recipe.name)) return recipe;
        }
        throw new AssertionError("Missing build recipe: " + name);
    }

    private static void requireContains(String text, String expected, String label) {
        require(text != null && text.contains(expected), label + " missing '" + expected + "': " + text);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone05ConstructionBlueprintOfferReadinessSmoke() { }
}
