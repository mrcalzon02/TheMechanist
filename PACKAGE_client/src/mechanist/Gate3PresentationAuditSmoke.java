package mechanist;

final class Gate3PresentationAuditSmoke {
    public static void main(String[] args) {
        String dirty = "targetZoneKey=3,3,3,3,false registryKey=debug.value path=/srv/mechanist/audit.txt uuid=123e4567-e89b-12d3-a456-426614174000 className=mechanist.runtime.DebugSurface";

        String[] outputs = {
                PlayerFacingCopySanitizer.forOrdinaryPlayer(dirty),
                PlayerFacingUiText.lookDetail("Audit", dirty),
                PlayerFacingDenialText.interaction(dirty),
                PlayerFacingInspectionText.fixture("Audit fixture", dirty),
                PlayerFacingActionText.interaction("Audit action", dirty),
                PlayerFacingPanelBody.formatWithTitle("Audit panel", dirty, 28),
                PlayerFacingEventLogText.event("Audit", dirty, 28),
                PlayerFacingTooltipText.tooltip("Audit tooltip", dirty, 28),
                PlayerFacingListRowText.row("Audit row", dirty, 28),
                PlayerFacingMenuOptionText.option("Audit option", dirty, false, 28)
        };

        for (String output : outputs) {
            assertNoLeak(output);
        }
    }

    private static void assertNoLeak(String output) {
        if (output.contains("targetZoneKey")
                || output.contains("3,3,3,3,false")
                || output.contains("registryKey")
                || output.contains("/srv/mechanist/audit.txt")
                || output.contains("123e4567")
                || output.contains("mechanist.runtime.DebugSurface")) {
            throw new AssertionError("Gate 3 presentation surface leaked implementation residue: " + output);
        }
    }
}
