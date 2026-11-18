package mechanist;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

/** Earn through a real dialogue action, resume mid-band, buy doctrine and use its recipe. */
final class KnowledgeProgressionIntegrationSmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        GamePanel resumed = new GamePanel();
        game.timer.stop(); resumed.timer.stop();
        try {
            game.seed = 94117L;
            game.worldSetup.zoneSize = 0;
            game.worldSetup.npcDensity = 0;
            game.worldSetup.simulationAge = 0;
            game.world = new World(game.seed, 32, 32);
            for (char[] column : game.world.tiles) Arrays.fill(column, '.');
            game.active = Candidate.random(new Random(94117));
            game.unlockedKnowledges.add("Underhive Basics");
            NpcEntity citizen = NpcEntity.create(Faction.HIVER, ZoneType.NEUTRAL_CIVILIAN_FLOOR, 2, 1, new Random(1));
            citizen.role = "Citizen";
            game.activeInteractionNpc = citizen;
            game.screen = GamePanel.Screen.PANEL;
            game.panelMode = GamePanel.PanelMode.DIALOGUE;
            game.setSize(1280, 720);
            BufferedImage canvas = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
            var graphics = canvas.createGraphics();
            try { game.paintComponent(graphics); } finally { graphics.dispose(); }
            ButtonBox talk = game.buttons.stream().filter(b -> "Talk".equals(b.label)).findFirst().orElseThrow();
            for (int i = 0; i < 24; i++) talk.action.run();
            check(game.xp == 24 && game.knowledgeCredits == 0 && game.knowledgeXpProgress == 24, "ordinary actions accrue a partial band");
            Properties saved = new Properties();
            Persistence.writeCore(game, saved);
            Persistence.readCore(resumed, saved);
            check(resumed.knowledgeXpProgress == 24 && resumed.knowledgeCredits == 0, "partial earning progress survives real save/load");
            resumed.gainXp("Social", 1, "continued work");
            check(resumed.xp == 25 && resumed.knowledgeCredits == 1 && resumed.knowledgeXpProgress == 0, "crossing the saved band awards exactly one credit");
            KnowledgeTree stale = KnowledgeBranchDefinitions.createBranches(1, resumed.unlockedKnowledges).get("survival");
            check(stale.canUnlock("Rough Bedding"), "fixture contains a second affordable node");
            check(KnowledgeProgressionAuthority.unlock(resumed, "survival", "Underhive Research Methods").success(), "earned credit buys doctrine through the shared UI authority");
            check(!KnowledgeProgressionAuthority.unlock(resumed, stale.id(), "Rough Bedding").success(), "stale menu balance cannot buy a second node");
            check(!KnowledgeProgressionAuthority.unlock(resumed, "survival", "Underhive Research Methods").success(), "duplicate purchase refused");
            check(!KnowledgeProgressionAuthority.unlock(resumed, "unknown", "anything").success() && resumed.knowledgeCredits == 0, "invalid branch is non-mutating");

            resumed.unlockSkillNode("fab-repair-field-tech");
            check(resumed.xp == 0 && resumed.unlockedSkillNodes.contains("fab-repair-field-tech"), "earned XP remains independently spendable on skills");
            resumed.gainXp("Mechanics", 24, "more work");
            resumed.gainXp("Mechanics", -100, "invalid award");
            check(resumed.knowledgeCredits == 0 && resumed.knowledgeXpProgress == 24, "spending XP cannot replay an earlier band; negative awards do nothing");

            CraftingRecipe research = CraftingRecipe.byName("Micro Lab Research Packet");
            check(research.visibleTo(resumed), "earned doctrine exposes its actual recipe");
            BaseObject lab = new BaseObject("EMM Micro Lab", 'l', 3, 1, 0, 0);
            resumed.baseObjects.add(lab);
            resumed.addInventoryItem("Construction supplies", null);
            resumed.addInventoryItem("Machine part", null);
            resumed.selectedInventoryIndex = resumed.inventory.indexOf("Construction supplies");
            resumed.useSelectedInventoryItemBody();
            resumed.selectedInventoryIndex = resumed.inventory.indexOf("Machine part");
            resumed.useSelectedInventoryItemBody();
            // Fulfil the recipe's configured difficulty-adjusted quantities via the same Use action.
            while (resumed.supplies < research.effectiveSuppliesCost()) pack(resumed, "Construction supplies");
            while (resumed.machineParts < research.effectiveMachinePartsCost()) pack(resumed, "Machine part");
            int inventoryBefore = resumed.inventory.size();
            resumed.selectedCraftingRecipe = research;
            resumed.craftSelectedRecipe();
            check(resumed.inventory.size() == inventoryBefore + 1, "unlocked recipe executes and produces a registered item");
            check(resumed.knowledgeCredits == 1 && resumed.knowledgeXpProgress == 4, "research work's five XP cross the next earned band once");
            Properties finalSave = new Properties();
            Persistence.writeCore(resumed, finalSave);
            Persistence.readCore(game, finalSave);
            check(game.unlockedKnowledges.contains("Underhive Research Methods") && game.knowledgeCredits == 1 && game.knowledgeXpProgress == 4, "doctrine, balance and remainder resume together");
            finalSave.remove("run.knowledgeXpProgress");
            Persistence.readCore(game, finalSave);
            check(game.knowledgeCredits == 1 && game.knowledgeXpProgress == game.xp % 25, "legacy saves infer only a remainder without retroactive credits");
            game.xp = Integer.MAX_VALUE - 1;
            game.knowledgeCredits = Integer.MAX_VALUE;
            game.gainXp("Mechanics", Integer.MAX_VALUE, "large award");
            check(game.xp == Integer.MAX_VALUE && game.knowledgeCredits == Integer.MAX_VALUE, "large awards cannot wrap balances negative");
            System.out.println("KnowledgeProgressionIntegrationSmoke PASS ordinaryEarning=true midBandResume=true stalePurchaseRefused=true independentSkillSpending=true recipeExecution=true legacyMigration=true");
        } finally {
            game.shutdownRuntime(); resumed.shutdownRuntime();
        }
    }

    private static void pack(GamePanel game, String item) {
        game.addInventoryItem(item, null);
        game.selectedInventoryIndex = game.inventory.lastIndexOf(item);
        game.useSelectedInventoryItemBody();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
