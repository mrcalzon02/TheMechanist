package mechanist.launcher;

public final class LauncherProfileSelectionDialogSmoke {
    public static void main(String[] args) {
        require("human8x8-63".equals(
                LauncherProfileSelectionDialog.stepPortraitId("human8x8-00", -1)),
                "previous portrait should wrap to the final human portrait");
        require("human8x8-00".equals(
                LauncherProfileSelectionDialog.stepPortraitId("human8x8-63", 1)),
                "next portrait should wrap to the first human portrait");
        require("human8x8-18".equals(
                LauncherProfileSelectionDialog.stepPortraitId("human8x8-17", 1)),
                "next portrait should advance deterministically");
        require("Portrait 18 of 64".equals(
                LauncherProfileSelectionDialog.portraitPresentation("human8x8-17")),
                "presentation should use player-facing ordinal rather than raw semantic id");
        require("Portrait unavailable".equals(
                LauncherProfileSelectionDialog.portraitPresentation("enemy-17")),
                "non-human portrait ids must not cross the launcher profile partition");

        System.out.println("LauncherProfileSelectionDialogSmoke PASS"
                + " wrap=true"
                + " partition=true"
                + " presentation=true");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private LauncherProfileSelectionDialogSmoke() {}
}
