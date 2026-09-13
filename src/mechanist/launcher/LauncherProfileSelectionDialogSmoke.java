package mechanist.launcher;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

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

        Dimension previewSize = LauncherProfileSelectionDialog.portraitPreviewSize();
        require(previewSize.width == 144 && previewSize.height == 144,
                "portrait preview must keep a stable square footprint even when art is unavailable");

        require("human8x8-17".equals(
                LauncherProfileSelectionDialog.initialPortraitId("human8x8-17")),
                "valid saved portrait should remain selected when the chooser opens");
        require("human8x8-00".equals(
                LauncherProfileSelectionDialog.initialPortraitId("enemy-17")),
                "unavailable saved portrait should recover to the first human portrait when the chooser opens");
        require("human8x8-00".equals(
                LauncherProfileSelectionDialog.initialPortraitId(null)),
                "missing saved portrait should recover to the first human portrait when the chooser opens");

        require("human8x8-63".equals(
                LauncherProfileSelectionDialog.stepPortraitId("human8x8-00", -1)),
                "previous portrait should wrap to the final human portrait");
        require("human8x8-00".equals(
                LauncherProfileSelectionDialog.stepPortraitId("human8x8-63", 1)),
                "next portrait should wrap to the first human portrait");
        require("human8x8-18".equals(
                LauncherProfileSelectionDialog.stepPortraitId("human8x8-17", 1)),
                "next portrait should advance deterministically");
        require("human8x8-00".equals(
                LauncherProfileSelectionDialog.stepPortraitId("enemy-17", 1)),
                "next from an unavailable portrait should recover at the first human portrait");
        require("human8x8-63".equals(
                LauncherProfileSelectionDialog.stepPortraitId("enemy-17", -1)),
                "previous from an unavailable portrait should recover at the final human portrait");
        require("Portrait 18 of 64".equals(
                LauncherProfileSelectionDialog.portraitPresentation("human8x8-17")),
                "presentation should use player-facing ordinal rather than raw semantic id");
        require("Portrait unavailable".equals(
                LauncherProfileSelectionDialog.portraitPresentation("enemy-17")),
                "non-human portrait ids must not cross the launcher profile partition");
        require("Selected portrait 18 of 64".equals(
                LauncherProfileSelectionDialog.portraitAccessibilityDescription("human8x8-17", true)),
                "accessible portrait state must identify the currently selected portrait");
        require("Selected portrait unavailable; image unavailable".equals(
                LauncherProfileSelectionDialog.portraitAccessibilityDescription("enemy-17", false)),
                "accessible portrait state must expose unavailable image association without leaking a raw portrait id");

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
        writeCompletePortraitPackage(packagedRoot);
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

        Path incompleteWorking = temp.resolve("incomplete");
        Path firstIncompleteAsset = LauncherProfileSelectionDialog.portraitAssetPath(
                incompleteWorking,
                LauncherFallbackProfileAuthority.humanPortraitId(0)
        );
        Files.createDirectories(firstIncompleteAsset.getParent());
        Files.writeString(firstIncompleteAsset, "smoke");
        require(packagedRoot.toAbsolutePath().normalize().equals(
                ThinLauncherMain.selectAppHome(incompleteWorking, clientJar)),
                "thin launcher must reject incomplete portrait roots and recover the complete packaged asset home");

        Path unreadableWorking = temp.resolve("unreadable");
        writeCompletePortraitPackage(unreadableWorking);
        Path unreadableAsset = LauncherProfileSelectionDialog.portraitAssetPath(
                unreadableWorking,
                LauncherFallbackProfileAuthority.humanPortraitId(17)
        );
        Files.writeString(unreadableAsset, "not a png image");
        require(packagedRoot.toAbsolutePath().normalize().equals(
                ThinLauncherMain.selectAppHome(unreadableWorking, clientJar)),
                "thin launcher must reject portrait roots whose expected files cannot be decoded as images");

        require(LauncherProfileSelectionDialog.portraitAvailable(packagedRoot, "human8x8-17"),
                "profile confirmation must accept a decodable selected human portrait");
        require(!LauncherProfileSelectionDialog.portraitAvailable(unreadableWorking, "human8x8-17"),
                "profile confirmation must reject a selected portrait whose image cannot be decoded");
        require(!LauncherProfileSelectionDialog.portraitAvailable(packagedRoot, "enemy-17"),
                "profile confirmation must reject portrait ids outside the human launcher partition");

        System.out.println("LauncherProfileSelectionDialogSmoke PASS"
                + " profileIdentityHidden=true"
                + " previewFootprint=true"
                + " initialSelectionRecovery=true"
                + " wrap=true"
                + " unavailableRecovery=true"
                + " partition=true"
                + " presentation=true"
                + " accessibilitySelection=true"
                + " previewAssetMapping=true"
                + " packagedAssetHome=true"
                + " completeAssetRoot=true"
                + " readableAssetRoot=true"
                + " confirmationAvailability=true");
    }

    private static void writeCompletePortraitPackage(Path root) throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        for (int ordinal = 0; ordinal < LauncherFallbackProfileAuthority.humanPortraitCount(); ordinal++) {
            Path asset = LauncherProfileSelectionDialog.portraitAssetPath(
                    root,
                    LauncherFallbackProfileAuthority.humanPortraitId(ordinal)
            );
            Files.createDirectories(asset.getParent());
            require(ImageIO.write(image, "png", asset.toFile()),
                    "smoke fixture could not encode launcher portrait PNG");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private LauncherProfileSelectionDialogSmoke() {}
}
