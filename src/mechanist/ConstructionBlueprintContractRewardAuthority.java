package mechanist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

/**
 * Converts blueprint-themed faction contract completion into a persistent
 * construction acquisition result without introducing a parallel quest token.
 */
final class ConstructionBlueprintContractRewardAuthority {
    enum Mode {
        GRANTED("granted"),
        PERMIT("licensed by permit"),
        STOLEN("stolen"),
        RECOVERED("recovered"),
        COUNTERFEIT("counterfeit"),
        REVEALED("revealed");

        final String label;
        Mode(String label) { this.label = label; }
    }

    record RewardResult(boolean applies, boolean changed, Mode mode,
                        String blueprintId, String blueprintName,
                        String summary) {
        static RewardResult none() {
            return new RewardResult(false, false, Mode.GRANTED,
                    "", "", "");
        }
    }

    private ConstructionBlueprintContractRewardAuthority() { }

    static RewardResult apply(GamePanel game, FactionContract contract,
                              String deliveredItem) {
        if (game == null || contract == null) return RewardResult.none();
        String text = contractText(contract, deliveredItem);
        if (!blueprintThemed(text)) return RewardResult.none();

        BuildRecipe recipe = selectRecipe(contract, text);
        if (recipe == null) {
            return new RewardResult(true, false, modeFor(text), "", "",
                    "Blueprint reward unresolved: the contract referenced a plan, but no compatible construction definition exists.");
        }

        String id = ConstructionBlueprintOwnershipAuthority.blueprintId(recipe);
        String name = recipe.name;
        Mode mode = modeFor(text);
        String provenanceKnowledge = "Construction blueprint provenance: "
                + mode.name() + " / " + id;

        if (mode == Mode.REVEALED) {
            String lead = "Blueprint lead: " + id + " / " + name;
            boolean changed = game.unlockedKnowledges.add(lead);
            game.unlockedKnowledges.add(provenanceKnowledge);
            return new RewardResult(true, changed, mode, id, name,
                    changed
                            ? "Blueprint pathway revealed: " + name
                                    + " is now recorded as a known acquisition lead, but is not yet owned."
                            : "Blueprint pathway already known: " + name + ".");
        }

        boolean changed = game.unlockedConstructionBlueprints.add(id);
        game.unlockedKnowledges.add(provenanceKnowledge);
        if (mode == Mode.PERMIT) {
            game.unlockedKnowledges.add("Construction permit: " + id + " / " + name);
        } else if (mode == Mode.COUNTERFEIT && changed) {
            game.unlockedKnowledges.add("Counterfeit construction risk: " + id
                    + " / safety and legality unverified");
            game.constructionExpansionReactions.add("COUNTERFEIT|" + id
                    + "|contract=" + safe(contract.id));
            game.suspicion = Math.min(100, Math.max(0, game.suspicion) + 5);
        } else if (mode == Mode.STOLEN && changed) {
            game.unlockedKnowledges.add("Stolen construction plan: " + id
                    + " / issuing faction ownership disputed");
            game.suspicion = Math.min(100, Math.max(0, game.suspicion) + 2);
        }

        String result = changed ? "unlocked" : "was already owned";
        String risk = mode == Mode.COUNTERFEIT && changed
                ? " Suspicion increased by 5; safety and legality remain unverified."
                : mode == Mode.STOLEN && changed
                ? " Suspicion increased by 2; issuing-faction ownership remains disputed."
                : "";
        return new RewardResult(true, changed, mode, id, name,
                "Construction blueprint " + mode.label + ": " + name
                        + " " + result + " in the persistent construction library."
                        + risk);
    }

    static String preview(FactionContract contract) {
        if (contract == null) return "";
        String text = contractText(contract, "");
        if (!blueprintThemed(text)) return "";
        BuildRecipe recipe = selectRecipe(contract, text);
        Mode mode = modeFor(text);
        if (recipe == null) {
            return "Blueprint reward: " + mode.label
                    + " plan referenced, but no compatible construction definition is currently available.";
        }
        String action = mode == Mode.REVEALED
                ? "records an acquisition lead without granting ownership"
                : "adds the plan to the persistent construction library";
        return "Blueprint reward: " + mode.label + " " + recipe.name
                + "; completion " + action + ".";
    }

    private static BuildRecipe selectRecipe(FactionContract contract, String text) {
        ArrayList<BuildRecipe> candidates =
                ConstructionBlueprintOwnershipAuthority.licensedRecipesForFaction(
                        contract.faction == null ? Faction.NONE : contract.faction);
        if (candidates.isEmpty()) {
            for (BuildRecipe recipe : BuildRecipe.allBuildRecipes()) {
                if (ConstructionBlueprintOwnershipAuthority.requiresLicensedBlueprint(recipe)) {
                    candidates.add(recipe);
                }
            }
        }
        if (candidates.isEmpty()) return null;

        candidates.sort(Comparator
                .comparingInt((BuildRecipe recipe) -> -recipeScore(recipe, text))
                .thenComparing(ConstructionBlueprintOwnershipAuthority::blueprintId));
        int bestScore = recipeScore(candidates.get(0), text);
        if (bestScore > 0) return candidates.get(0);
        int index = Math.floorMod(Objects.hash(safe(contract.id),
                contract.faction, safe(contract.type), text), candidates.size());
        return candidates.get(index);
    }

    private static int recipeScore(BuildRecipe recipe, String text) {
        if (recipe == null) return Integer.MIN_VALUE;
        String low = safe(text).toLowerCase(Locale.ROOT);
        String id = ConstructionBlueprintOwnershipAuthority.blueprintId(recipe)
                .toLowerCase(Locale.ROOT);
        String name = safe(recipe.name).toLowerCase(Locale.ROOT);
        int score = 0;
        if (!id.isBlank() && low.contains(id)) score += 200;
        if (!name.isBlank() && low.contains(name)) score += 120;
        for (String token : name.split("[^a-z0-9]+")) {
            if (token.length() >= 4 && low.contains(token)) score += 12;
        }
        if (contains(low, "clinic", "medical", "medicae")
                && contains(name, "clinic", "medical", "medicae")) score += 60;
        if (contains(low, "armory", "defensive", "security", "barracks")
                && contains(name, "armory", "defensive", "security", "barracks")) score += 60;
        if (contains(low, "shopfront", "storefront", "trade counter", "market")
                && contains(name, "shop", "store", "trade", "market", "counter")) score += 60;
        if (contains(low, "lab", "workshop", "industrial", "factory", "forge")
                && contains(name, "lab", "workshop", "industrial", "factory", "forge")) score += 60;
        return score;
    }

    private static Mode modeFor(String text) {
        String low = safe(text).toLowerCase(Locale.ROOT);
        if (contains(low, "counterfeit", "forged blueprint", "false permit")) return Mode.COUNTERFEIT;
        if (contains(low, "steal", "stolen", "hidden lab", "illicit plan")) return Mode.STOLEN;
        if (contains(low, "recover", "recovered", "ruined workshop", "wrecked workshop", "salvaged plan")) return Mode.RECOVERED;
        if (contains(low, "reveal", "blueprint lead", "plan location", "intelligence lead")) return Mode.REVEALED;
        if (contains(low, "permit", "licensed shopfront", "civic license")) return Mode.PERMIT;
        return Mode.GRANTED;
    }

    private static boolean blueprintThemed(String text) {
        String low = safe(text).toLowerCase(Locale.ROOT);
        return contains(low, "blueprint", "construction plan", "room plan",
                "machine plan", "licensed folio", "construction folio",
                "shopfront permit", "building permit", "hidden lab plan",
                "industrial plan", "armory plan", "defensive-room plan",
                "clinic-room plan", "counterfeit plan");
    }

    private static String contractText(FactionContract contract,
                                       String deliveredItem) {
        if (contract == null) return safe(deliveredItem);
        return (safe(contract.type) + " " + safe(contract.targetName) + " "
                + safe(contract.requiredTurnInItem) + " "
                + safe(contract.description) + " " + safe(deliveredItem))
                .toLowerCase(Locale.ROOT);
    }

    private static boolean contains(String text, String... terms) {
        String low = safe(text).toLowerCase(Locale.ROOT);
        for (String term : terms) {
            if (term != null && !term.isBlank()
                    && low.contains(term.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '/');
    }
}
