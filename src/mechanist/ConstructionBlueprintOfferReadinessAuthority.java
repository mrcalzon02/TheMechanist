package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Shared player-facing and execution preflight for a live construction-blueprint offer. */
final class ConstructionBlueprintOfferReadinessAuthority {
    record Readiness(BuildRecipe recipe, int price, List<String> purchaseBlockers,
                     List<String> capabilityBlockers, String materialState, List<String> lines) {
        Readiness {
            purchaseBlockers = purchaseBlockers == null ? List.of() : List.copyOf(purchaseBlockers);
            capabilityBlockers = capabilityBlockers == null ? List.of() : List.copyOf(capabilityBlockers);
            materialState = materialState == null ? "" : materialState;
            lines = lines == null ? List.of() : List.copyOf(lines);
        }

        boolean applies() { return recipe != null; }
        boolean purchaseReady() { return applies() && purchaseBlockers.isEmpty(); }
        boolean postPurchaseCapabilityReady() { return applies() && capabilityBlockers.isEmpty(); }
        String purchaseBlock() {
            return !applies() || purchaseBlockers.isEmpty() ? "" : String.join("; ", purchaseBlockers);
        }
    }

    private ConstructionBlueprintOfferReadinessAuthority() { }

    static Readiness evaluate(GamePanel game, TraderSession trader, TradeOffer offer,
                              FactionMarketAccessAuthority.Decision access) {
        BuildRecipe recipe = offer == null
                ? null
                : ConstructionBlueprintOwnershipAuthority.recipeForId(offer.constructionBlueprintId);
        if (recipe == null) return notApplicable();

        int price = trader == null
                ? ConstructionBlueprintOwnershipAuthority.blueprintPrice(recipe)
                : trader.buyPrice(offer);
        ArrayList<String> purchaseBlockers = new ArrayList<>();
        if (access == null) {
            purchaseBlockers.add("live market access is unavailable");
        } else if (!access.allowed()) {
            addUnique(purchaseBlockers, access.purchaseBlock());
        }

        if (game == null) {
            purchaseBlockers.add("live player purchase state is unavailable");
        } else {
            addUnique(purchaseBlockers,
                    ConstructionBlueprintOwnershipAuthority.purchaseBlock(game, offer));
            if (game.inventoryWeight() + 1 > game.carryCapacity()) {
                purchaseBlockers.add("carrying load is full");
            }
            if (game.carriedScript < price) {
                purchaseBlockers.add("need " + price + " script, have " + Math.max(0, game.carriedScript));
            }
        }

        ArrayList<String> capabilityBlockers = new ArrayList<>();
        if (game == null) {
            capabilityBlockers.add("live construction capability is unavailable");
        } else {
            if (recipe.requiredKnowledge != null && !recipe.requiredKnowledge.isBlank()
                    && !game.hasKnowledge(recipe.requiredKnowledge)) {
                capabilityBlockers.add("missing knowledge: " + recipe.requiredKnowledge);
            }
            if (recipe.requiresWorkbench && game.firstBaseObject('w') == null) {
                capabilityBlockers.add("requires a Scrap Workbench");
            }
        }

        String materialState = materialState(game, recipe);
        ArrayList<String> lines = new ArrayList<>();
        BlueprintAcquisitionPathAuthority.AcquisitionPath path =
                BlueprintAcquisitionPathAuthority.pathFor(recipe);
        Faction issuer = ConstructionBlueprintOwnershipAuthority.issuingFactionFor(recipe);
        String role = ConstructionBlueprintOwnershipAuthority.vendorRoleFor(recipe);
        String category = ConstructionCategoryAuthority.categoryFor(recipe);
        String legal = path == null ? "unclassified construction access" : path.legalLabel();
        String marketClass = access == null ? "not evaluated" : access.legalClass();

        lines.add("Blueprint offer: " + ConstructionBlueprintOwnershipAuthority.blueprintItemName(recipe)
                + " unlocks " + recipe.name + " / " + safe(category, "construction")
                + " / " + safe(recipe.qualityName, "Common") + ".");
        lines.add("Offer source: " + issuer.label + " " + role + "; legal status " + legal
                + "; live market class " + marketClass + ".");
        lines.add("Blueprint price: " + price + " script at this live counter.");
        lines.add(access == null
                ? "Live access: BLOCKED - market access could not be evaluated."
                : "Live access: " + (access.allowed() ? "AVAILABLE" : "BLOCKED") + " - " + access.requirement() + ".");
        lines.add(purchaseBlockers.isEmpty()
                ? "Purchase: READY - access, ownership, carried script, and inventory capacity permit this transaction."
                : "Purchase: BLOCKED - " + String.join("; ", purchaseBlockers) + ".");
        lines.add("Availability: one run unlock; successful purchase retains this exact folio as physical proof, "
                + "records the plan in the construction library, and removes learned offers from vendor shelves.");
        lines.add("Quest prerequisite: none is attached to this stocked folio; live market access is the acquisition gate.");
        lines.add(capabilityBlockers.isEmpty()
                ? "Build after purchase: READY - current knowledge and workbench requirements are satisfied."
                : "Build after purchase: BLOCKED - " + String.join("; ", capabilityBlockers)
                    + "; these construction blockers do not block buying the folio.");
        lines.add(materialState);
        lines.add(ConstructionReadabilityAuthority.effortPreview(recipe));
        lines.add(ConstructionReadabilityAuthority.attentionPreview(recipe));
        lines.add("Site readiness: deferred until a Build target is selected; terrain, occupancy, placement access, "
                + "and self-entombment checks remain site-specific and do not block buying the folio.");
        return new Readiness(recipe, price, purchaseBlockers, capabilityBlockers, materialState, lines);
    }

    private static Readiness notApplicable() {
        return new Readiness(null, 0, List.of(), List.of(), "", List.of());
    }

    private static String materialState(GamePanel game, BuildRecipe recipe) {
        if (game == null || recipe == null) {
            return "Construction materials: live holdings are unavailable; material state does not block buying the folio.";
        }
        ArrayList<String> holdings = new ArrayList<>();
        int supplies = Math.max(0, game.supplies);
        int parts = Math.max(0, game.machineParts);
        holdings.add("supplies " + supplies + "/" + Math.max(0, recipe.supplyCost));
        holdings.add("machine parts " + parts + "/" + Math.max(0, recipe.partCost));

        boolean complete = supplies >= Math.max(0, recipe.supplyCost)
                && parts >= Math.max(0, recipe.partCost);
        int requiredUnits = Math.max(0, recipe.supplyCost) + Math.max(0, recipe.partCost);
        for (Map.Entry<String, Integer> component : recipe.componentCosts.entrySet()) {
            if (component.getKey() == null || component.getValue() == null || component.getValue() <= 0) continue;
            int required = component.getValue();
            int held = Math.max(0, game.countProductionInput(component.getKey()));
            holdings.add(component.getKey() + " " + held + "/" + required);
            requiredUnits += required;
            if (held < required) complete = false;
        }

        String status;
        if (requiredUnits <= 0) status = "NO MATERIALS REQUIRED";
        else if (complete) status = "FULLY READY";
        else if (ProgressiveConstructionAuthority.availableMaterialUnits(game, recipe) > 0) {
            status = "PARTIAL STAGE AVAILABLE";
        } else status = "NO MATERIALS AVAILABLE";
        return "Construction materials: " + status + " - " + String.join(", ", holdings)
                + "; material holdings do not block buying the folio.";
    }

    private static void addUnique(ArrayList<String> lines, String value) {
        if (value == null || value.isBlank() || lines.contains(value)) return;
        lines.add(value);
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
