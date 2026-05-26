package mechanist;

/** Runs the narrow Gate 3 readability smoke checks from one entry point. */
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
        System.out.println("Gate 3 player-facing text smoke suite passed.");
    }

    private Gate3PlayerFacingTextSmokeSuite() { }
}
