package mechanist;

import java.util.Properties;

/** Smoke for skill/knowledge-gated contract completion through faction representatives. */
final class Milestone03ContractTurnInSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
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

            game.unlockedKnowledges.add("Contract Negotiation");
            game.activeInteractionNpc = representative;
            game.panelMode = GamePanel.PanelMode.DIALOGUE;
            game.screen = GamePanel.Screen.PANEL;
            game.setSize(1280, 720);
            java.awt.image.BufferedImage canvas = new java.awt.image.BufferedImage(1280, 720,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D graphics = canvas.createGraphics();
            game.paintComponent(graphics);
            graphics.dispose();
            require(hasButton(game, "Turn In"),
                    "matching representative dialogue should render Turn In: " + buttonLabels(game));
            requireContains(ContractTurnInAuthority.representativeLine(game, representative),
                    "Contract turn-in ready", "ready representative readback");

            int turnBefore = game.turn;
            game.turnInContractWithActiveNpc();
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

            Properties saved = new Properties();
            Persistence.writeCore(game, saved);
            GamePanel restored = new GamePanel();
            if (restored.timer != null) restored.timer.stop();
            try {
                Persistence.readCore(restored, saved);
                require(restored.factionContracts.stream().anyMatch(candidate -> candidate != null && candidate.completed),
                        "completed contract state should persist");
                require(restored.carriedScript == game.carriedScript,
                        "contract payout should persist with carried script");
                require(restored.factionStanding.getOrDefault(Faction.ARBITES, 0) == contract.repReward,
                        "contract standing reward should persist");
            } finally {
                restored.shutdownRuntime();
            }
        } finally {
            game.shutdownRuntime();
        }
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
