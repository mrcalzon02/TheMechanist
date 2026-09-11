package mechanist.launcher;

import java.nio.file.Path;

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

        Path root = Path.of("launcher-root");
        require(root.resolve("profile-packages/human-8x8/assets/Humans8x8_r01c01_32px.png").equals(
                LauncherProfileSelectionDialog.portraitAssetPath(root, "human8x8-00")),
                "portrait zero should map to the first row-major launcher asset");
        require(root.resolve("profile-packages/human-8x8/assets/Humans8x8_r01c08_32px.png").equals(
                LauncherProfileSelectionDialog.portraitAssetPath(root, "human8x8-07")),
                "portrait seven should map to row one column eight");
        require(root.resolve("profile-packages/human-8x8/assets/Humans8x8_r02c01_32px.png").equals(
                LauncherProfileSelectionDialog.portraitAssetPath(root, "human8x8-08")),
                "portrait eight should map to row two column one");
        require(root.resolve("profile-packages/human-8x8/assets/Humans8x8_r08c08_32px.png").equals(
                LauncherProfileSelectionDialog.portraitAssetPath(root, "human8x8-63")),
                "final portrait should map to row eight column eight");
        require(LauncherProfileSelectionDialog.portraitAssetPath(root, "enemy-17") == null,
                "non-human ids must not resolve into launcher human portrait assets");

        System.out.println("LauncherProfileSelectionDialogSmoke PASS"
                + " wrap=true"
                + " partition=true"
                + " presentation=true"
                + " previewAssetMapping=true");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private LauncherProfileSelectionDialogSmoke() {}
}
