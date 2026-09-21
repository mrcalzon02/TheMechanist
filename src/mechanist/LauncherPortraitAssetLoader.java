package mechanist;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Graphical asset-loading boundary for the selected launcher/profile portrait.
 * Semantic identity and physical-path validation remain owned by UserProfileAuthority;
 * UI painters consume only the decoded image result and never filesystem paths.
 */
final class LauncherPortraitAssetLoader {
    private static Path cachedPath;
    private static long cachedLastModifiedMillis = Long.MIN_VALUE;
    private static BufferedImage cachedImage;

    private LauncherPortraitAssetLoader() {}

    static synchronized BufferedImage selectedProfilePortrait() {
        Path portraitPath = UserProfileAuthority.launcherPortraitAssetPath();
        if (portraitPath == null) {
            clearCache();
            return null;
        }

        long lastModifiedMillis = lastModifiedMillis(portraitPath);
        if (portraitPath.equals(cachedPath) && lastModifiedMillis == cachedLastModifiedMillis) return cachedImage;

        cachedPath = portraitPath;
        cachedLastModifiedMillis = lastModifiedMillis;
        try {
            cachedImage = ImageIO.read(portraitPath.toFile());
        } catch (Exception ex) {
            cachedImage = null;
            DebugLog.warn("PROFILE_PORTRAIT", "Could not decode selected launcher portrait: " + ex.getMessage());
        }
        return cachedImage;
    }

    private static long lastModifiedMillis(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception ex) {
            return Long.MIN_VALUE;
        }
    }

    private static void clearCache() {
        cachedPath = null;
        cachedLastModifiedMillis = Long.MIN_VALUE;
        cachedImage = null;
    }
}
