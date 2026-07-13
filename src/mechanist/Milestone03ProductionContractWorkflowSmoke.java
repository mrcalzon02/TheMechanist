package mechanist;

import java.util.List;
import java.util.Properties;

/** Smoke for accepting and completing a provenance-backed faction production order. */
final class Milestone03ProductionContractWorkflowSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        try {
            if (game.world == null) game.world = new World(73119L, 40, 40);
            NpcEntity representative = new NpcEntity();
            representative.name = "Civic Works Clerk";
            representative.role = "Faction Representative";
            representative.faction = Faction.CIVIC_WARDENS;
            representative.x = 5;
            representative.y = 5;
            game.activeInteractionNpc = representative;
            game.panelMode = GamePanel.PanelMode.DIALOGUE;
            game.screen = GamePanel.Screen.PANEL;
            game.setSize(1280, 720);

            render(game);
            require(hasButton(game, "Take Work") && hasButton(game, "Turn In"),
                    "representative dialogue should render production work and hand-in controls: " + buttonLabels(game));
            requireContains(ProductionContractAuthority.representativeLine(game, representative),
                    "Production work available", "available work readback");

            int turnBeforeAcceptance = game.turn;
            runButton(game, "Take Work");
            require(game.turn == turnBeforeAcceptance + 1, "accepting production work should spend one turn");
            FactionContract contract = game.factionContracts.stream()
                    .filter(candidate -> candidate != null && candidate.requiresProductionProof())
                    .findFirst().orElseThrow(() -> new AssertionError("Take Work should create a production order"));
            require(contract.minimumQualityTier == 3 && "Machine part".equals(contract.requiredTurnInItem),
                    "baseline order should request a Serviceable machine part");
            require(contract.skillXpReward == 4, "baseline production order should reward four skill XP");

            int contractsBeforeDuplicate = game.factionContracts.size();
            int turnBeforeDuplicate = game.turn;
            game.acceptProductionContractWithActiveNpc();
            require(game.factionContracts.size() == contractsBeforeDuplicate && game.turn == turnBeforeDuplicate,
                    "duplicate faction work acceptance should not add an order or spend time");

            Properties acceptedSave = new Properties();
            Persistence.writeCore(game, acceptedSave);
            GamePanel restored = new GamePanel();
            if (restored.timer != null) restored.timer.stop();
            try {
                Persistence.readCore(restored, acceptedSave);
                FactionContract persisted = restored.factionContracts.stream()
                        .filter(candidate -> candidate != null && candidate.requiresProductionProof())
                        .findFirst().orElseThrow(() -> new AssertionError("production order should persist"));
                require(persisted.minimumQualityTier == contract.minimumQualityTier
                                && persisted.skillXpReward == contract.skillXpReward,
                        "production quality and skill XP terms should persist");
            } finally {
                restored.shutdownRuntime();
            }

            game.unlockedSkillNodes.add("fab-repair-material-eye");
            game.unlockedKnowledges.add("Scrap-Forging Doctrine");
            String common = addProduced(game, "Common", "passed inspection");
            ContractTurnInAuthority.TurnInResult lowQuality =
                    ContractTurnInAuthority.turnInFirst(game, representative);
            require(!lowQuality.success(), "Common production should not satisfy a Serviceable order");
            requireContains(lowQuality.message(), "Serviceable quality or better", "minimum-quality blocker");
            require(game.inventory.contains(common) && !contract.completed,
                    "low-quality rejection must preserve the item and order");

            String untraced = "Serviceable Improvised Machine part";
            game.inventory.add(untraced);
            ContractTurnInAuthority.TurnInResult untracedResult =
                    ContractTurnInAuthority.turnInFirst(game, representative);
            require(!untracedResult.success(), "untraced item should not satisfy a production order");
            requireContains(untracedResult.message(), "recorded produced item", "production-record blocker");
            require(game.inventory.contains(untraced), "untraced rejection must preserve the item");

            String defective = addProduced(game, "Fine", "defect flagged");
            ContractTurnInAuthority.TurnInResult defectiveResult =
                    ContractTurnInAuthority.turnInFirst(game, representative);
            require(!defectiveResult.success(), "defect-flagged batch should not satisfy a faction standard");
            requireContains(defectiveResult.message(), "passed inspection", "inspection blocker");
            require(game.inventory.contains(defective), "inspection rejection must preserve the item");

            String valid = addProduced(game, "Masterwork", "passed inspection");
            int scriptBefore = game.carriedScript;
            int standingBefore = game.factionStanding.getOrDefault(Faction.CIVIC_WARDENS, 0);
            int xpBefore = game.xp;
            int turnBeforeCompletion = game.turn;
            game.turnInContractWithActiveNpc();
            require(contract.completed, "valid production proof should complete the order");
            require(!game.inventory.contains(valid) && game.peekProvenanceForItem(valid) == null,
                    "turn-in should consume the qualifying unit and its provenance record");
            require(game.inventory.contains(common) && game.inventory.contains(untraced) && game.inventory.contains(defective),
                    "turn-in should leave rejected alternatives untouched");
            require(game.carriedScript == scriptBefore + contract.payout,
                    "production order should pay its promised script");
            require(game.factionStanding.getOrDefault(Faction.CIVIC_WARDENS, 0) == standingBefore + contract.repReward,
                    "production order should award faction standing");
            require(game.xp == xpBefore + contract.skillXpReward,
                    "production order should award listed skill XP");
            require(game.turn == turnBeforeCompletion + 1, "successful production hand-in should spend one turn");
            requireContains(lastEventContaining(game, "Contract completed"), "skill XP +4",
                    "production completion reward readback");
            requireNotContains(lastEventContaining(game, "Contract completed"), "P-",
                    "private production contract ID");

            List<String> summary = ContractObjectiveReadabilityAuthority.summary(List.of(contract),
                    game.inventory, game.baseStorage, 2, game.unlockedSkillNodes, game.unlockedKnowledges);
            requireContains(ContractObjectiveReadabilityAuthority.summary(List.of(activeCopy(contract)),
                            List.of(valid), List.of(), 2, game.unlockedSkillNodes, game.unlockedKnowledges).toString(),
                    "Production standard: Serviceable quality or better", "production objective standard");
            require(summary.stream().anyMatch(line -> line.contains("Active contracts: 0")),
                    "completed production order should leave no active objective");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static FactionContract activeCopy(FactionContract source) {
        FactionContract copy = FactionContract.parse(source.saveLine());
        if (copy == null) throw new AssertionError("production contract should parse from its save line");
        copy.completed = false;
        return copy;
    }

    private static String addProduced(GamePanel game, String quality, String defectState) {
        ProductionRecipe recipe = ProductionRecipe.create("Machine part", Faction.MECHANICUS, quality,
                "Scrap-Forging Doctrine", "Test Forge");
        BaseObject machine = new BaseObject("Contract Test Forge", 'f', 1, 1, 0, 0);
        machine.qualityName = quality;
        ProductionBatchAuthority.BatchDisposition batch = new ProductionBatchAuthority.BatchDisposition(
                "BATCH-CONTRACT-" + quality.toUpperCase(), 5, defectState.equals("passed inspection") ? 99 : 1,
                defectState, List.of());
        ItemProvenanceRecord provenance = ItemProvenanceRecord.produced(recipe, machine, game.world, game.turn,
                "Test Operator", null, null, null, batch);
        String item = recipe.outputItemName();
        game.inventory.add(item);
        game.rememberItemProvenance(item, provenance);
        return item;
    }

    private static void render(GamePanel game) {
        java.awt.image.BufferedImage canvas = new java.awt.image.BufferedImage(1280, 720,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = canvas.createGraphics();
        game.paintComponent(graphics);
        graphics.dispose();
    }

    private static boolean hasButton(GamePanel game, String label) {
        for (ButtonBox button : game.buttons) if (button != null && label.equals(button.label)) return true;
        return false;
    }

    private static void runButton(GamePanel game, String label) {
        for (ButtonBox button : game.buttons) {
            if (button != null && label.equals(button.label) && button.action != null) {
                button.action.run();
                return;
            }
        }
        throw new AssertionError("Button not found: " + label + " / " + buttonLabels(game));
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

    private Milestone03ProductionContractWorkflowSmoke() { }
}
