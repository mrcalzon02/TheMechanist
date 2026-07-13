package mechanist;

import java.util.ArrayList;

/** Smoke for base-wide staffed-production queue visibility and control. */
final class Milestone03ProductionControlBoardSmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.baseClaimed = true;
        game.claimedRoomId = 6;
        game.baseX = 5;
        game.baseY = 5;

        FactionRecipeVariant variant = firstRunnableVariant(game);
        require(variant != null, "expected a runnable generated job for production-board coverage");
        game.unlockedKnowledges.add(variant.requiredKnowledge);
        game.factionRecruits.add(new RecruitWorker("Vara Coil", "forge worker", Faction.HIVER, 4, 4));

        BaseObject active = new BaseObject("Board Forge", 'w', 5, 5, 0, 0);
        active.qualityName = "Archeotech";
        active.integrity = 5;
        active.assignedRecipe = ControlledProductionJobAuthority.assignmentKey(variant);
        active.productionQueueTarget = 2;
        active.productionQueueRemaining = 2;
        active.productionProgressTurns = 1;
        active.productionLastBlocker = "waiting for test materials";
        game.baseObjects.add(active);
        ManualStaffingAssignmentAuthority.assign(game, active, game.factionRecruits.get(0));

        BaseObject idle = new BaseObject("Board Lathe", 'w', 7, 5, 0, 0);
        idle.qualityName = "Common";
        game.baseObjects.add(idle);
        BaseObject unfinished = new BaseObject("Unfinished Board Mill", 'w', 9, 5, 0, 0);
        unfinished.underConstruction = true;
        game.baseObjects.add(unfinished);

        game.openCraftingPanel();
        game.openProductionControlBoard();
        require(game.productionBoardActive, "crafting should enter the base production board");
        if (game.world == null) game.world = new World(73031L, 40, 40);
        game.setSize(1280, 720);
        java.awt.image.BufferedImage canvas = new java.awt.image.BufferedImage(1280, 720,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = canvas.createGraphics();
        game.paintComponent(graphics);
        graphics.dispose();
        require(hasButton(game, "Worker >") && hasButton(game, "Queue +")
                        && hasButton(game, "Materials >") && hasButton(game, "Output >")
                        && hasButton(game, "No Room >") && hasButton(game, "Workbench"),
                "rendered production board should expose worker, queue, policy, and workbench controls: "
                        + buttonLabels(game));
        ArrayList<BaseObject> machines = ProductionControlBoardAuthority.machines(game);
        require(machines.size() == 2 && machines.get(0) == active,
                "board should hide unfinished sites and prioritize a blocked queued machine");
        require(game.selectedProductionBoardMachine() == active,
                "board should select the highest-attention production machine");

        String row = ProductionControlBoardAuthority.rowLabel(game, active);
        requireContains(row, "Board Forge - blocked - queue 2", "bounded machine row");
        String detail = String.join(" | ", ProductionControlBoardAuthority.detailLines(game, active));
        requireContains(detail, variant.outputName, "readable generated job name");
        requireContains(detail, "waiting for test materials", "last blocker readback");
        requireContains(detail, "materials Wait / keep worker", "material policy readback");
        requireNotContains(detail, "GENVAR::", "raw generated assignment key");

        active.productionLastBlocker = "";
        requireContains(game.adjustProductionBoardQueue(-1), "1/1", "board queue removal");
        requireContains(game.adjustProductionBoardQueue(1), "2/2", "board queue addition");
        requireContains(game.updateProductionBoardPolicy("materials"), "Pause / release worker",
                "board material policy control");
        requireContains(game.updateProductionBoardPolicy("output"), "Nearest Cache",
                "board output policy control");
        requireContains(game.updateProductionBoardPolicy("no-room"), "Pause / release worker",
                "board no-room policy control");
        require(active.productionLastBlocker.isBlank(), "policy changes should clear stale blocker readback");

        active.assignedWorker = "";
        requireContains(game.cycleProductionBoardWorker(), "Vara Coil assigned",
                "board worker recovery after release");
        require("Vara Coil".equals(active.assignedWorker),
                "board worker control should reassign a released recruit to the selected queue");

        int eventsBeforeStatus = game.eventLog.size();
        game.reportProductionBoardStatus();
        require(eventSliceContains(game, eventsBeforeStatus, "Production status:"),
                "board status should use selected-machine production readback");
        int eventsBeforeHistory = game.eventLog.size();
        game.reportProductionBoardHistory();
        require(eventSliceContains(game, eventsBeforeHistory, "Production history for Board Forge"),
                "board history should use selected-machine production readback");

        active.productionProgressTurns = 2;
        requireContains(game.clearProductionBoardQueue(), "background production is idle",
                "board queue clear");
        require(active.productionQueueRemaining == 0 && active.productionProgressTurns == 0,
                "board clear should cancel remaining runs and current progress");

        game.openSelectedProductionBoardWorkbench();
        require(game.panelMode == GamePanel.PanelMode.WORKBENCH && game.activeInteractionBaseObject == active
                        && game.workbenchStaffedJobsActive && !game.productionBoardActive,
                "board Workbench should open selected-machine staffed-job setup");
        if (game.timer != null) game.timer.stop();
    }

    private static boolean eventSliceContains(GamePanel game, int first, String expected) {
        for (int i = Math.max(0, first); i < game.eventLog.size(); i++) {
            String line = game.eventLog.get(i);
            if (line != null && line.contains(expected)) return true;
        }
        return false;
    }

    private static boolean hasButton(GamePanel game, String label) {
        for (ButtonBox button : game.buttons) {
            if (button != null && label.equals(button.label)) return true;
        }
        return false;
    }

    private static String buttonLabels(GamePanel game) {
        ArrayList<String> labels = new ArrayList<>();
        for (ButtonBox button : game.buttons) if (button != null) labels.add(button.label);
        return labels.toString();
    }

    private static FactionRecipeVariant firstRunnableVariant(GamePanel game) {
        BaseObject auditMachine = new BaseObject("Board Audit Workbench", 'w', 5, 5, 0, 0);
        auditMachine.qualityName = "Archeotech";
        auditMachine.assignedWorker = "Vara Coil";
        for (FactionRecipeVariant variant : FactionRecipeVariantApi.generatedFactionVariants()) {
            if (variant == null || variant.itemInputs.isEmpty() || !variant.equipmentRequirements.isEmpty()) continue;
            game.unlockedKnowledges.add(variant.requiredKnowledge);
            if (ControlledProductionJobAuthority.assignmentProblem(game, auditMachine, variant) == null
                    && ControlledProductionJobAuthority.machineAcceptsVariant(auditMachine, variant)) return variant;
        }
        return null;
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

    private Milestone03ProductionControlBoardSmoke() { }
}
