package mechanist;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Graphical asset-loading boundary for the selected launcher/profile portrait.
 * Semantic identity and physical-path validation remain owned by UserProfileAuthority;
 * UI painters consume only the decoded image result and never filesystem paths.
 */
final class LauncherPortraitAssetLoader {
    private static Path cachedPath;
    private static BufferedImage cachedImage;

    private LauncherPortraitAssetLoader() {}

    static synchronized BufferedImage selectedProfilePortrait() {
        Path portraitPath = UserProfileAuthority.launcherPortraitAssetPath();
        if (portraitPath == null) {
            cachedPath = null;
            cachedImage = null;
            return null;
        }
        if (portraitPath.equals(cachedPath)) return cachedImage;

        cachedPath = portraitPath;
        try {
            cachedImage = ImageIO.read(portraitPath.toFile());
        } catch (Exception ex) {
            cachedImage = null;
            DebugLog.warn("PROFILE_PORTRAIT", "Could not decode selected launcher portrait: " + ex.getMessage());
        }
        return cachedImage;
    }
}
