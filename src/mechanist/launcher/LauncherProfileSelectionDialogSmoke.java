package mechanist.launcher;

import java.nio.file.Files;
import java.nio.file.Path;

public final class LauncherProfileSelectionDialogSmoke {
    public static void main(String[] args) throws Exception {
        LauncherFallbackProfileAuthority.LauncherProfile profile =
                new LauncherFallbackProfileAuthority.LauncherProfile(
                        "fallback-0123456789abcdef",
                        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                        LauncherFallbackProfileAuthority.HUMAN_8X8_PACKAGE,
                        "human8x8-17",
                        LauncherFallbackProfileAuthority.SPECIAL_PORTRAIT_PACKAGE,
                        LauncherFallbackProfileAuthority.SPECIAL_NAME_PACKAGE,
                        Path.of("profile.properties")
                );
        require("Local profile".equals(LauncherProfileSelectionDialog.profilePresentation(profile)),
                "profile chooser must not expose the machine-derived internal profile id");
        require(!LauncherProfileSelectionDialog.profilePresentation(profile).contains(profile.profileId()),
                "player-facing profile presentation must hide the raw fallback profile key");

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

        Path temp = Files.createTempDirectory("mechanist-launcher-profile-home-");
        Path packagedRoot = temp.resolve("installed");
        Path portraitAssets = packagedRoot.resolve("profile-packages/human-8x8/assets");
        Files.createDirectories(portraitAssets);
        Path clientDir = packagedRoot.resolve("packages/client");
        Files.createDirectories(clientDir);
        Path clientJar = clientDir.resolve("TheMechanist-client.jar");
        Files.writeString(clientJar, "smoke");
        Path unrelatedWorking = temp.resolve("elsewhere");
        Files.createDirectories(unrelatedWorking);

        require(packagedRoot.toAbsolutePath().normalize().equals(
                ThinLauncherMain.selectAppHome(packagedRoot, clientJar)),
                "packaged launcher working root should remain the asset home");
        require(packagedRoot.toAbsolutePath().normalize().equals(
                ThinLauncherMain.selectAppHome(unrelatedWorking, clientJar)),
                "thin launcher should climb from the client jar to the packaged portrait root");

        System.out.println("LauncherProfileSelectionDialogSmoke PASS"
                + " profileIdentityHidden=true"
                + " wrap=true"
                + " partition=true"
                + " presentation=true"
                + " previewAssetMapping=true"
                + " packagedAssetHome=true");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private LauncherProfileSelectionDialogSmoke() {}
}
