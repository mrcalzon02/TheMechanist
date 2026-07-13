package mechanist;

import java.util.List;
import java.util.Random;

/** Smoke for browsing and spending skill XP through the live Character panel. */
final class Milestone03SkillTreeCharacterPanelSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        try {
            game.active = Candidate.random(new Random(33017L));
            game.xp = 70;
            int knowledgeBefore = game.unlockedKnowledges.size();
            if (game.world == null) game.world = new World(33017L, 40, 40);
            game.openPanel(GamePanel.PanelMode.CHARACTER);
            game.characterTab = CharacterEquipmentAndMedicalAuthority.CharacterTab.SKILLS.ordinal();
            game.setSize(1280, 720);

            java.awt.image.BufferedImage canvas = new java.awt.image.BufferedImage(1280, 720,
                    java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D graphics = canvas.createGraphics();
            game.paintComponent(graphics);
            graphics.dispose();
            require(hasButtonContaining(game, "Skills")
                            && hasButtonContaining(game, "Fabrication and Repair")
                            && hasButton(game, "Unlock") && hasButton(game, "Skill Info"),
                    "rendered Character Skills tab should expose branches, unlock, and reference controls: "
                            + buttonLabels(game));

            List<SkillTreeProgressionAuthority.SkillBranch> branches = SkillTreePanelAuthority.branches();
            SkillTreeProgressionAuthority.SkillBranch fabrication = branches.get(0);
            game.selectSkillBranch(0);
            SkillTreeProgressionAuthority.SkillNode fieldRepair = fabrication.nodes().get(0);
            game.selectSkillNode(fieldRepair);
            String fieldDetail = String.join(" | ", SkillTreePanelAuthority.detailLines(game, fabrication, fieldRepair));
            requireContains(fieldDetail, "Status: Available", "available root-node preview");
            requireContains(fieldDetail, "Capability:", "capability readback");
            requireNotContains(fieldDetail, fieldRepair.id(), "raw skill node ID");

            game.unlockSelectedSkillNode();
            require(game.unlockedSkillNodes.contains(fieldRepair.id()) && game.xp == 45,
                    "Character-panel unlock should spend XP and persist the root node");
            require(game.unlockedKnowledges.size() == knowledgeBefore,
                    "Character-panel skill spending must not unlock knowledge");

            SkillTreeProgressionAuthority.SkillNode materialEye = fabrication.nodes().get(1);
            game.selectSkillNode(materialEye);
            requireContains(SkillTreePanelAuthority.previewMessage(game, materialEye), "Ready to unlock",
                    "dependent-node preview after prerequisite");
            requireContains(game.unlockSkillNode(materialEye.name()), "Unlocked skill node",
                    "shared Character-panel spending action");
            require(game.unlockedSkillNodes.contains(materialEye.id()) && game.xp == 5,
                    "second Character-panel unlock should deduct its XP cost");

            SkillTreeProgressionAuthority.SkillNode masterWorkshop = fabrication.nodes().get(2);
            game.selectSkillNode(masterWorkshop);
            String blocked = String.join(" | ", SkillTreePanelAuthority.detailLines(game, fabrication, masterWorkshop));
            requireContains(blocked, "Status: Locked", "advanced-node blocked state");
            requireContains(blocked, "facility forge fabrication stall", "world-access blocker");
            int xpBeforeBlocked = game.xp;
            game.unlockSelectedSkillNode();
            require(game.xp == xpBeforeBlocked && !game.unlockedSkillNodes.contains(masterWorkshop.id()),
                    "blocked Character-panel unlock must not spend XP or grant the node");

            SkillTreeProgressionAuthority.SkillNode tutoredRepair =
                    SkillTreeProgressionAuthority.nodeByIdOrName("fab-repair-forge-tutoring");
            require(tutoredRepair != null, "trainer-gated repair node should be defined");
            requireContains(SkillTreePanelAuthority.previewMessage(game, tutoredRepair), "trainer forge tutor",
                    "trainer requirement before meeting a specialist");
            NpcEntity trainer = new NpcEntity();
            trainer.name = "Artificer Senn";
            trainer.role = "Forge Tutor";
            trainer.faction = Faction.MECHANIST_COLLEGIA;
            trainer.x = 5;
            trainer.y = 5;
            game.activeInteractionNpc = trainer;
            game.panelMode = GamePanel.PanelMode.DIALOGUE;
            game.screen = GamePanel.Screen.PANEL;
            graphics = canvas.createGraphics();
            game.paintComponent(graphics);
            graphics.dispose();
            require(hasButton(game, "Train"),
                    "qualifying NPC conversation should expose a Train action: " + buttonLabels(game));

            game.xp = 45;
            game.trainWithActiveNpc();
            require(game.characterTab == CharacterEquipmentAndMedicalAuthority.CharacterTab.SKILLS.ordinal()
                            && game.activeSkillTrainers.contains("forge-tutor")
                            && tutoredRepair.id().equals(game.selectedSkillNodeId),
                    "Train should open the matching skill node with temporary trainer access");
            requireContains(SkillTreePanelAuthority.previewMessage(game, tutoredRepair), "Ready to unlock",
                    "trainer-gated node with specialist present");
            requireContains(game.unlockSkillNode(tutoredRepair.name()), "Unlocked skill node",
                    "trainer-gated Character-panel unlock");
            require(game.xp == 0 && game.unlockedSkillNodes.contains(tutoredRepair.id()),
                    "trainer-gated unlock should spend XP and grant the capability");

            BaseObject broken = new BaseObject("Training Repair Forge", 'f', 6, 5, 0, 0);
            broken.integrity = 0;
            MachineRepairAuthority.RepairPreview trainedRepair = MachineRepairAuthority.preview(game, broken, 1);
            require(trainedRepair.projectedIntegrity() == MachineRepairAuthority.SERVICEABLE_INTEGRITY,
                    "Forge-Tutored Repair should restore a broken machine to serviceable integrity");
            requireContains(trainedRepair.summary(), "Forge-Tutored Repair", "trained repair preview");
            game.closePanel();
            require(game.activeSkillTrainers.isEmpty(), "closing training should clear temporary trainer access");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static boolean hasButton(GamePanel game, String label) {
        for (ButtonBox button : game.buttons) if (button != null && label.equals(button.label)) return true;
        return false;
    }

    private static boolean hasButtonContaining(GamePanel game, String fragment) {
        for (ButtonBox button : game.buttons) {
            if (button != null && button.label != null && button.label.contains(fragment)) return true;
        }
        return false;
    }

    private static String buttonLabels(GamePanel game) {
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        for (ButtonBox button : game.buttons) if (button != null) labels.add(button.label);
        return labels.toString();
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

    private Milestone03SkillTreeCharacterPanelSmoke() { }
}
