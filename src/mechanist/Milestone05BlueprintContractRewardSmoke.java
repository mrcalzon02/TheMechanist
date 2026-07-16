package mechanist;

import java.util.ArrayList;
import java.util.Properties;

/** Focused Phase 17.3 smoke for contract-driven construction blueprint acquisition. */
final class Milestone05BlueprintContractRewardSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        BuildRecipe recipe = firstLicensedRecipe();
        require(recipe != null, "fixture requires at least one licensed construction recipe");

        verifyMode(recipe, ConstructionBlueprintContractRewardAuthority.Mode.GRANTED,
                "Complete this construction blueprint commission for " + recipe.name + ".");
        verifyMode(recipe, ConstructionBlueprintContractRewardAuthority.Mode.PERMIT,
                "Purchase and return the legal shopfront permit for the " + recipe.name + " blueprint.");
        verifyMode(recipe, ConstructionBlueprintContractRewardAuthority.Mode.STOLEN,
                "Steal the hidden lab blueprint for " + recipe.name + " from an illicit archive.");
        verifyMode(recipe, ConstructionBlueprintContractRewardAuthority.Mode.RECOVERED,
                "Recover the industrial plan for " + recipe.name + " from a ruined workshop.");
        verifyMode(recipe, ConstructionBlueprintContractRewardAuthority.Mode.COUNTERFEIT,
                "Deliver a counterfeit plan for the " + recipe.name + " construction blueprint.");
        verifyMode(recipe, ConstructionBlueprintContractRewardAuthority.Mode.REVEALED,
                "Reveal the blueprint lead and plan location for " + recipe.name + ".");

        verifyOrdinaryContractUnaffected(recipe);
        System.out.println("Milestone 05 blueprint contract reward smoke passed.");
    }

    private static void verifyMode(BuildRecipe recipe,
                                   ConstructionBlueprintContractRewardAuthority.Mode mode,
                                   String description) {
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            if (game.world == null) game.world = new World(17300L + mode.ordinal(), 40, 40);
            game.factionContracts.clear();
            game.inventory.clear();
            game.unlockedConstructionBlueprints.clear();
            game.unlockedKnowledges.removeIf(value -> value != null
                    && (value.startsWith("Blueprint lead:")
                    || value.startsWith("Construction blueprint provenance:")
                    || value.startsWith("Construction permit:")
                    || value.startsWith("Counterfeit construction risk:")
                    || value.startsWith("Stolen construction plan:")));
            game.constructionExpansionReactions.clear();
            game.suspicion = 0;

            Faction issuer = ConstructionBlueprintOwnershipAuthority.issuingFactionFor(recipe);
            FactionContract contract = contract("CONTRACT-BLUEPRINT-" + mode.name(),
                    issuer, description, "Blueprint proof folio " + mode.name());
            satisfyProofGates(game, contract);
            game.factionContracts.add(contract);
            game.inventory.add(contract.requiredTurnInItem);
            NpcEntity representative = representative(issuer);

            String preview = ContractTurnInAuthority.representativeLine(game, representative);
            requireContains(preview, "Blueprint reward:", mode + " representative preview");
            requireContains(preview, mode.label, mode + " representative mode");

            int scriptBefore = game.carriedScript;
            int standingBefore = game.factionStanding.getOrDefault(issuer, 0);
            ContractTurnInAuthority.TurnInResult result =
                    ContractTurnInAuthority.turnInFirst(game, representative);
            require(result.success() && contract.completed,
                    mode + " blueprint contract should complete: " + result.message());
            require(!game.inventory.contains(contract.requiredTurnInItem),
                    mode + " completion should consume exactly one proof folio");
            require(game.carriedScript == scriptBefore + contract.payout,
                    mode + " completion should preserve ordinary script payout");
            require(game.factionStanding.getOrDefault(issuer, 0)
                            == standingBefore + contract.repReward,
                    mode + " completion should preserve ordinary standing payout");
            requireContains(result.message(), recipe.name,
                    mode + " completion blueprint name");
            requireContains(result.message(), mode.label,
                    mode + " completion acquisition mode");

            String blueprintId = ConstructionBlueprintOwnershipAuthority.blueprintId(recipe);
            String provenance = "Construction blueprint provenance: "
                    + mode.name() + " / " + blueprintId;
            require(game.unlockedKnowledges.contains(provenance),
                    mode + " completion should persist acquisition provenance knowledge");
            if (mode == ConstructionBlueprintContractRewardAuthority.Mode.REVEALED) {
                require(!game.unlockedConstructionBlueprints.contains(blueprintId),
                        "revealed blueprint should record a lead without granting ownership");
                require(game.unlockedKnowledges.contains(
                                "Blueprint lead: " + blueprintId + " / " + recipe.name),
                        "revealed blueprint should persist its acquisition lead");
                require(game.suspicion == 0,
                        "revealed blueprint lead should not create theft or counterfeit suspicion");
            } else {
                require(game.unlockedConstructionBlueprints.contains(blueprintId),
                        mode + " contract should unlock the real construction blueprint ledger");
            }
            if (mode == ConstructionBlueprintContractRewardAuthority.Mode.COUNTERFEIT) {
                require(game.suspicion == 5,
                        "counterfeit blueprint should add five suspicion");
                require(game.unlockedKnowledges.stream().anyMatch(value -> value != null
                                && value.startsWith("Counterfeit construction risk: " + blueprintId)),
                        "counterfeit blueprint should persist safety and legality risk");
                require(game.constructionExpansionReactions.stream().anyMatch(value -> value != null
                                && value.startsWith("COUNTERFEIT|" + blueprintId + "|")),
                        "counterfeit blueprint should persist a construction reaction marker");
            } else if (mode == ConstructionBlueprintContractRewardAuthority.Mode.STOLEN) {
                require(game.suspicion == 2,
                        "stolen blueprint should add two suspicion");
                require(game.unlockedKnowledges.stream().anyMatch(value -> value != null
                                && value.startsWith("Stolen construction plan: " + blueprintId)),
                        "stolen blueprint should persist disputed ownership knowledge");
            } else {
                require(game.suspicion == 0,
                        mode + " blueprint should not add theft or counterfeit suspicion");
            }
            if (mode == ConstructionBlueprintContractRewardAuthority.Mode.PERMIT) {
                require(game.unlockedKnowledges.contains(
                                "Construction permit: " + blueprintId + " / " + recipe.name),
                        "permit blueprint should persist the legal construction permit");
            }

            assertPersistence(game, recipe, mode);
        } finally {
            game.shutdownRuntime();
        }
    }

    private static void verifyOrdinaryContractUnaffected(BuildRecipe recipe) {
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            if (game.world == null) game.world = new World(17399L, 40, 40);
            game.factionContracts.clear();
            game.inventory.clear();
            game.unlockedConstructionBlueprints.clear();
            Faction issuer = ConstructionBlueprintOwnershipAuthority.issuingFactionFor(recipe);
            FactionContract contract = contract("CONTRACT-ORDINARY-EVIDENCE", issuer,
                    "Return sealed evidence from a local investigation.",
                    "Sealed evidence parcel");
            satisfyProofGates(game, contract);
            game.factionContracts.add(contract);
            game.inventory.add(contract.requiredTurnInItem);
            ContractTurnInAuthority.TurnInResult result =
                    ContractTurnInAuthority.turnInFirst(game, representative(issuer));
            require(result.success(), "ordinary contract should still complete");
            require(game.unlockedConstructionBlueprints.isEmpty(),
                    "ordinary non-blueprint contract must not grant a random plan");
            require(!result.message().contains("Construction blueprint"),
                    "ordinary contract completion must not claim a blueprint reward");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static void assertPersistence(GamePanel game, BuildRecipe recipe,
                                          ConstructionBlueprintContractRewardAuthority.Mode mode) {
        Properties saved = new Properties();
        Persistence.writeCore(game, saved);
        GamePanel restored = new GamePanel();
        restored.shutdownRuntime();
        try {
            Persistence.readCore(restored, saved);
            String id = ConstructionBlueprintOwnershipAuthority.blueprintId(recipe);
            String provenance = "Construction blueprint provenance: "
                    + mode.name() + " / " + id;
            require(restored.unlockedKnowledges.contains(provenance),
                    mode + " acquisition provenance should survive save/load");
            if (mode == ConstructionBlueprintContractRewardAuthority.Mode.REVEALED) {
                require(!restored.unlockedConstructionBlueprints.contains(id)
                                && restored.unlockedKnowledges.contains(
                                "Blueprint lead: " + id + " / " + recipe.name),
                        "revealed blueprint lead should survive without becoming owned");
            } else {
                require(restored.unlockedConstructionBlueprints.contains(id),
                        mode + " owned blueprint should survive save/load");
            }
            require(restored.suspicion == game.suspicion,
                    mode + " suspicion consequence should survive save/load");
            if (mode == ConstructionBlueprintContractRewardAuthority.Mode.COUNTERFEIT) {
                require(new ArrayList<>(restored.constructionExpansionReactions)
                                .stream().anyMatch(value -> value != null
                                && value.startsWith("COUNTERFEIT|" + id + "|")),
                        "counterfeit construction reaction should survive save/load");
            }
        } finally {
            restored.shutdownRuntime();
        }
    }

    private static BuildRecipe firstLicensedRecipe() {
        for (BuildRecipe recipe : BuildRecipe.allBuildRecipes()) {
            if (ConstructionBlueprintOwnershipAuthority.requiresLicensedBlueprint(recipe)) {
                return recipe;
            }
        }
        return null;
    }

    private static FactionContract contract(String id, Faction faction,
                                             String description, String proofItem) {
        FactionContract contract = new FactionContract();
        contract.id = id;
        contract.type = "FETCH";
        contract.faction = faction;
        contract.targetName = description;
        contract.requiredTurnInItem = proofItem;
        contract.description = description;
        contract.payout = 90;
        contract.repReward = 2;
        contract.spawned = true;
        return contract;
    }

    private static NpcEntity representative(Faction faction) {
        NpcEntity representative = new NpcEntity();
        representative.id = "BLUEPRINT-CONTRACT-REP-" + faction.name();
        representative.name = faction.label + " Blueprint Contract Clerk";
        representative.role = "Faction Representative";
        representative.faction = faction;
        representative.hp = 10;
        return representative;
    }

    private static void satisfyProofGates(GamePanel game, FactionContract contract) {
        game.unlockedSkillNodes.addAll(
                ContractObjectiveReadabilityAuthority.requiredCapabilityKeys(contract));
        game.unlockedKnowledges.addAll(
                ContractObjectiveReadabilityAuthority.requiredKnowledgeNames(contract));
    }

    private static void requireContains(String text, String expected, String label) {
        require(text != null && text.contains(expected),
                "Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone05BlueprintContractRewardSmoke() { }
}
