package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Connects an inspected room's mapped construction plan to a physical live vendor. */
final class RoomBlueprintVendorGuidanceAuthority {
    static final String PLAN_SELLER_OBJECTIVE_LABEL = "Construction plan seller";

    record Guidance(BuildRecipe recipe, NpcEntity seller, List<String> lines) {
        boolean locatable() { return seller != null; }
    }

    record SellerRoute(boolean reachable, String summary) { }

    private RoomBlueprintVendorGuidanceAuthority() { }

    static Guidance forTile(GamePanel game, int x, int y) {
        if (game == null || game.world == null || !game.world.inBounds(x, y)) return empty();
        return forRoom(game, game.world.roomIdAt(x, y));
    }

    static Guidance forRoom(GamePanel game, int roomId) {
        if (game == null || game.world == null || roomId < 0 || roomId >= game.world.rooms.size()) return empty();
        RoomProfile profile = game.world.roomProfile(roomId);
        BuildRecipe recipe = RoomConstructionParityAuthority.liveMatchingRecipe(profile);
        if (recipe == null) return empty();

        ArrayList<String> lines = new ArrayList<>();
        if (!ConstructionBlueprintOwnershipAuthority.requiresLicensedBlueprint(recipe)) {
            lines.add("Plan access: " + recipe.name
                    + " is public and already available in Build; no licensed seller is required.");
            return new Guidance(recipe, null, List.copyOf(lines));
        }
        if (ConstructionBlueprintOwnershipAuthority.owns(game, recipe)) {
            lines.add("Plan access: " + recipe.name
                    + " is already recorded in your construction library; ordinary knowledge, material, workbench, and placement checks still apply.");
            return new Guidance(recipe, null, List.copyOf(lines));
        }

        NpcEntity seller = nearestSeller(game, recipe);
        Faction issuer = ConstructionBlueprintOwnershipAuthority.issuingFactionFor(recipe);
        String role = ConstructionBlueprintOwnershipAuthority.vendorRoleFor(recipe);
        if (seller == null) {
            lines.add("Plan seller unavailable here: no living " + issuer.label + " " + role
                    + " is staffed in this zone. Seek another controlled " + issuer.label + " market.");
            lines.add(ConstructionBlueprintOwnershipAuthority.playerAccessLine(game, recipe));
            return new Guidance(recipe, null, List.copyOf(lines));
        }

        int distance = Math.abs(seller.x - game.playerX) + Math.abs(seller.y - game.playerY);
        String direction = QuestObjectiveGuidanceAuthority.directionLabel(game.playerX, game.playerY, seller.x, seller.y);
        String where = distance == 0 ? "at your position" : distance + " tile" + (distance == 1 ? "" : "s") + " " + direction;
        lines.add("Plan seller: " + seller.name + " / " + role + " / " + where
                + ". Ask for " + ConstructionBlueprintOwnershipAuthority.blueprintItemName(recipe)
                + "; catalog price " + ConstructionBlueprintOwnershipAuthority.blueprintPrice(recipe)
                + " script before live market pricing.");
        lines.add("Use Locate Seller to mark the live counter on Map and check current route access.");
        lines.add(ConstructionBlueprintOwnershipAuthority.playerAccessLine(game, recipe));
        return new Guidance(recipe, seller, List.copyOf(lines));
    }

    static SellerRoute routeToSeller(GamePanel game, NpcEntity seller) {
        if (game == null || game.world == null || seller == null
                || !game.world.inBounds(game.playerX, game.playerY)
                || !game.world.inBounds(seller.x, seller.y)) {
            return new SellerRoute(false, "The seller's current route cannot be checked in this area.");
        }
        int maxSteps = (int) Math.max(1L,
                Math.min(Integer.MAX_VALUE, (long) game.world.w * (long) game.world.h));
        boolean[][] reachable = MovementPlanningAuthority.reachableTiles(game, maxSteps);
        int[][] adjacent = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        if (reachable != null) {
            for (int[] offset : adjacent) {
                int x = seller.x + offset[0];
                int y = seller.y + offset[1];
                if (game.world.inBounds(x, y) && reachable[x][y]) {
                    return new SellerRoute(true, "Walkable interaction route confirmed.");
                }
            }
        }
        return new SellerRoute(false,
                "Exact seller location known, but no currently walkable route reaches an adjacent interaction tile; open sealed access or find another route.");
    }

    private static NpcEntity nearestSeller(GamePanel game, BuildRecipe recipe) {
        NpcEntity best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (NpcEntity npc : game.world.npcs) {
            if (!ConstructionBlueprintOwnershipAuthority.isLiveVendorFor(npc, recipe)) continue;
            int distance = Math.abs(npc.x - game.playerX) + Math.abs(npc.y - game.playerY);
            if (distance < bestDistance || (distance == bestDistance && sellerKey(npc).compareTo(sellerKey(best)) < 0)) {
                best = npc;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static String sellerKey(NpcEntity npc) {
        if (npc == null) return "~";
        return (npc.name == null ? "" : npc.name) + "|" + npc.x + "|" + npc.y;
    }

    private static Guidance empty() { return new Guidance(null, null, List.of()); }
}
