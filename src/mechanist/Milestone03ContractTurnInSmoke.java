package mechanist;

import java.util.Properties;

/** Smoke for skill/knowledge-gated contract completion through faction representatives. */
final class Milestone03ContractTurnInSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        System.out.println("[TURNIN_PROBE] before GamePanel construction");
        System.out.flush();
        GamePanel game = new GamePanel();
        System.out.println("[TURNIN_PROBE] after GamePanel construction");
        System.out.flush();
        if (game.timer != null) game.timer.stop();
        try {
            if (game.world == null) game.world = new World(73117L, 40, 40);
            FactionContract uncarriedContract = new FactionContract();
            uncarriedContract.id = "F-EARLIER-PRIVATE";
            uncarriedContract.type = "FETCH";
            uncarriedContract.faction = Faction.ARBITES;
            uncarriedContract.requiredTurnInItem = "Missing evidence satchel";
            uncarriedContract.payout = 40;
            uncarriedContract.repReward = 1;
            uncarriedContract.spawned = true;
            game.factionContracts.add(uncarriedContract);

            FactionContract contract = new FactionContract();
            contract.id = "F-PRIVATE-73117";
            contract.type = "FETCH";
            contract.faction = Faction.ARBITES;
            contract.targetZoneKey = "1,1,2,3,4,false";
            contract.targetName = "sealed evidence parcel";
            contract.targetEntityId = "CONTRACT-OBJ-PRIVATE";
            contract.requiredTurnInItem = "Sealed object F-PRIVATE-73117";
            contract.description = "Retrieve sealed evidence for this contract and return it unopened.";
            contract.payout = 125;
            contract.repReward = 3;
            contract.spawned = true;
            game.factionContracts.add(contract);
            game.inventory.add(contract.requiredTurnInItem);

            NpcEntity representative = new NpcEntity();
            representative.name = "Warden Contract Clerk";
            representative.role = "Faction Representative";
            representative.faction = Faction.CIVIC_WARDENS;
            representative.x = 5;
            representative.y = 5;

            System.out.println("[TURNIN_PROBE] before proof-block checks");
            System.out.flush();
            int scriptBefore = game.carriedScript;
            ContractTurnInAuthority.TurnInResult skillBlocked =
                    ContractTurnInAuthority.turnInFirst(game, representative);
            require(!skillBlocked.success(), "missing skill proof should block turn-in");
            requireContains(skillBlocked.message(), "Trace Reading", "skill blocker");
            require(!contract.completed && game.inventory.contains(contract.requiredTurnInItem)
                            && game.carriedScript == scriptBefore,
                    "blocked skill proof must not consume proof or grant rewards");

            game.unlockedSkillNodes.add("investigation-trace-reading");
            ContractTurnInAuthority.TurnInResult knowledgeBlocked =
                    ContractTurnInAuthority.turnInFirst(game, representative);
            require(!knowledgeBlocked.success(), "missing knowledge proof should block turn-in");
            requireContains(knowledgeBlocked.message(), "Contract Negotiation", "knowledge blocker");
            require(!contract.completed && game.inventory.contains(contract.requiredTurnInItem),
                    "blocked knowledge proof must remain non-mutating");

            System.out.println("[TURNIN_PROBE] before dialogue rendering");
            System.out.flush();
            game.unlockedKnowledges.add("Contract Negotiation");
            game.activeInteractionNpc = representative;
            game.panelMode = GamePanel.PanelMode.DIALOGUE;
            game.screen = GamePanel.Screen.PANEL;
            game.setSize(1280, 720);
            java.awt.image.BufferedImage canvas = new java.awt.image.BufferedImage(1280, 720,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D graphics = canvas.createGraphics();
            System.out.println("[TURNIN_PROBE] before dialogue paintComponent");
            System.out.flush();
            game.paintComponent(graphics);
            graphics.dispose();
            require(hasButton(game, "Turn In"),
                    "matching representative dialogue should render Turn In: " + buttonLabels(game));
            requireContains(ContractTurnInAuthority.representativeLine(game, representative),
                    "Contract turn-in ready", "ready representative readback");

            System.out.println("[TURNIN_PROBE] before successful turn-in");
            System.out.flush();
            int turnBefore = game.turn;
            game.turnInContractWithActiveNpc();
            System.out.println("[TURNIN_PROBE] after successful turn-in");
            System.out.flush();
            require(contract.completed, "successful representative turn-in should complete the contract");
            require(!uncarriedContract.completed,
                    "an earlier uncarried contract must not mask or receive a later carried turn-in");
            require(!game.inventory.contains(contract.requiredTurnInItem),
                    "successful turn-in should consume exactly one carried proof item");
            require(game.carriedScript == scriptBefore + contract.payout,
                    "successful turn-in should pay promised script");
            require(game.factionStanding.getOrDefault(Faction.ARBITES, 0) == contract.repReward,
                    "successful turn-in should award contract-faction standing");
            require(game.turn == turnBefore + 1, "successful turn-in should spend one game turn");
            requireContains(lastEventContaining(game, "Contract completed"), "125 script",
                    "completion event reward");
            requireNotContains(lastEventContaining(game, "Contract completed"), "F-PRIVATE",
                    "private contract ID in completion event");

            System.out.println("[TURNIN_PROBE] before persistence write");
            System.out.flush();
            Properties saved = new Properties();
            Persistence.writeCore(game, saved);
            System.out.println("[TURNIN_PROBE] before bounded restore GamePanel construction");
            System.out.flush();
            GamePanel restored = new GamePanel();
            if (restored.timer != null) restored.timer.stop();
            try {
                // The normal save decoder must be exercised without requiring
                // this synthetic 40x40 contract fixture to regenerate a 600x600
                // production world just to validate its contract fields.
                Persistence.restoreCarriedInventory(restored, saved);
                Persistence.restoreCarriedScript(restored, saved);
                Persistence.restoreFactionContractRewardState(restored, saved);
                System.out.println("[TURNIN_PROBE] after bounded contract restore");
                System.out.flush();
                assertRestoredContractRewards(restored, game, contract, uncarriedContract);
                if (args != null && java.util.Arrays.asList(args)
                        .contains("--full-core-restore")) {
                    // This expensive end-to-end world regeneration is retained
                    // as a separate late Gate 3 diagnostic, not discarded.
                    System.out.println("[TURNIN_PROBE] before full readCore world restoration");
                    System.out.flush();
                    Persistence.readCore(restored, saved);
                    System.out.println("[TURNIN_PROBE] after full restore readCore");
                    System.out.flush();
                    assertRestoredContractRewards(restored, game, contract, uncarriedContract);
                }
            } finally {
                restored.shutdownRuntime();
            }
        } finally {
            game.shutdownRuntime();
            System.out.println("[TURNIN_PROBE] complete");
            System.out.flush();
        }
    }

    private static void assertRestoredContractRewards(GamePanel restored, GamePanel original,
                                                      FactionContract completed,
                                                      FactionContract incomplete) {
        require(restored.factionContracts.stream().anyMatch(candidate ->
                        candidate != null && candidate.id.equals(completed.id)
                                && candidate.completed),
                "the exact completed contract identity must survive save restoration");
        require(restored.factionContracts.stream().anyMatch(candidate ->
                        candidate != null && candidate.id.equals(incomplete.id)
                                && !candidate.completed),
                "an earlier uncarried contract must remain incomplete after reload");
        require(restored.carriedScript == original.carriedScript,
                "contract payout must persist with carried script");
        require(restored.factionStanding.getOrDefault(Faction.ARBITES, 0)
                        == completed.repReward,
                "contract standing reward must persist");
        require(!restored.inventory.contains(completed.requiredTurnInItem),
                "restored inventory must not refund the consumed contract proof");
    }

    private static boolean hasButton(GamePanel game, String label) {
        for (ButtonBox button : game.buttons) if (button != null && label.equals(button.label)) return true;
        return false;
    }

    private static String buttonLabels(GamePanel game) {
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        for (ButtonBox button : game.buttons) if (button != null) labels.add(button.label);
        return labels.toString();
    }

    private static String lastEventContaining(GamePanel game, String expected) {
        for (int i = game.eventLog.size() - 1; i >= 0; i--) {
            String line = game.eventLog.get(i);
            if (line != null && line.contains(expected)) return line;
        }
        return "";
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void requireNotContains(String text, String forbidden, String label) {
        if (text == null || !text.contains(forbidden)) return;
        throw new AssertionError("Expected " + label + " not to contain '" + forbidden + "': " + text);
    }

    private Milestone03ContractTurnInSmoke() { }
}
