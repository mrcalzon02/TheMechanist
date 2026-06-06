package mechanist;

/** Runs the narrow Gate 3 readability checks from one entry point. */
final class Gate3PlayerFacingTextSmokeSuite {
    public static void main(String[] args) {
        FactionContractDisplaySmoke.main(args);
        PlayerFacingCopySanitizerSmoke.main(args);
        PlayerFacingUiTextSmoke.main(args);
        PlayerFacingDenialTextSmoke.main(args);
        PlayerFacingInspectionTextSmoke.main(args);
        PlayerFacingActionTextSmoke.main(args);
        PlayerFacingTextWrapSmoke.main(args);
        PlayerFacingPanelBodySmoke.main(args);
        PlayerFacingEventLogTextSmoke.main(args);
        PlayerFacingTooltipTextSmoke.main(args);
        PlayerFacingListRowTextSmoke.main(args);
        PlayerFacingMenuOptionTextSmoke.main(args);
        Gate3PresentationAuditSmoke.main(args);
        Milestone02LookExamineReadabilitySmoke.main(args);
        Milestone02MovementPlanningReadabilitySmoke.main(args);
        Milestone02ContextPromptReadabilitySmoke.main(args);
        Milestone02InfopediaMechanicsReadabilitySmoke.main(args);
        Milestone02MenuUniformityReadabilitySmoke.main(args);
        Milestone02InputRebindingAuditSmoke.main(args);
        Milestone02InputConflictRecoverySmoke.main(args);
        Milestone02CurrentBindingPromptSmoke.main(args);
        Milestone02InputProfilePersistenceSmoke.main(args);
        Milestone02ControllerTuningRuntimeSmoke.main(args);
        Milestone02ControllerTapHoldSmoke.main(args);
        Milestone02ControllerConnectionSmoke.main(args);
        Milestone02ControllerGlyphPromptSmoke.main(args);
        Milestone02ZoneTileSlotStateSmoke.main(args);
        Milestone02ZoneTileLayerMappingSmoke.main(args);
        Milestone02ZoneTilePushSqueezeMovementSmoke.main(args);
        System.out.println("Gate 3 player-facing text smoke suite passed.");
    }

    private Gate3PlayerFacingTextSmokeSuite() { }
}
