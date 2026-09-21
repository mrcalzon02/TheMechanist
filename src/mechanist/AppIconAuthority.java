package mechanist;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import java.awt.Image;
import java.awt.Taskbar;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Owns desktop/window icon loading for the Java desktop client.
 *
 * The icon pack is kept as PNG assets so Linux desktop environments, the Swing
 * frame, and packaged jars can all use the same source images without pulling in
 * platform-specific icon dependencies.
 */
public final class AppIconAuthority {
    static final String VERSION = "app-icon-authority-0.9.10ht";

    private static final int[] ICON_SIZES = {16, 32, 48, 64, 128, 256, 512};
    private static final String FILE_PATTERN = "assets/app/icons/the-mechanist-%d.png";
    private static final String RESOURCE_PATTERN = "/assets/app/icons/the-mechanist-%d.png";

    private AppIconAuthority() {}

    public static void applyTo(JFrame frame) {
        if (frame == null) return;
        List<Image> icons = loadIcons();
        if (icons.isEmpty()) {
            DebugLog.warn("APP_ICON", "No application icon images were loaded; using host default window icon.");
            return;
        }
        frame.setIconImages(icons);
        applyTaskbarIcon(icons.get(icons.size() - 1));
        DebugLog.audit("APP_ICON", "Loaded " + icons.size() + " application icon sizes using " + VERSION + ".");
    }

    public static List<Image> loadIcons() {
        List<Image> icons = new ArrayList<>();
        for (int size : ICON_SIZES) {
            Image image = loadIcon(size);
            if (image != null) icons.add(image);
        }
        return Collections.unmodifiableList(icons);
    }

    public static String assetPathForSize(int size) {
        for (int allowed : ICON_SIZES) {
            if (allowed == size) return String.format(FILE_PATTERN, size);
        }
        return String.format(FILE_PATTERN, 512);
    }

    private static Image loadIcon(int size) {
        String filePath = String.format(FILE_PATTERN, size);
        File file = new File("packages/client", filePath);
        if (!file.isFile()) file = new File("client", filePath);
        if (!file.isFile()) file = new File(filePath);
        if (file.isFile()) {
            try {
                BufferedImage image = ImageIO.read(file);
                if (isExpectedIcon(image, size, filePath)) return image;
            } catch (IOException ioe) {
                DebugLog.warn("APP_ICON", "Could not read application icon file " + filePath + ": " + ioe.getMessage());
            }
        }

        String resourcePath = String.format(RESOURCE_PATTERN, size);
        try (InputStream in = AppIconAuthority.class.getResourceAsStream(resourcePath)) {
            if (in != null) {
                BufferedImage image = ImageIO.read(in);
                if (isExpectedIcon(image, size, resourcePath)) return image;
            }
        } catch (IOException ioe) {
            DebugLog.warn("APP_ICON", "Could not read bundled application icon resource " + resourcePath + ": " + ioe.getMessage());
        }
        return null;
    }

    private static boolean isExpectedIcon(BufferedImage image, int expectedSize, String source) {
        if (image == null) {
            DebugLog.warn("APP_ICON", "Application icon is not a decodable image: " + source);
            return false;
        }
        if (image.getWidth() != expectedSize || image.getHeight() != expectedSize) {
            DebugLog.warn("APP_ICON", "Application icon has unexpected dimensions "
                    + image.getWidth() + "x" + image.getHeight() + "; expected "
                    + expectedSize + "x" + expectedSize + ": " + source);
            return false;
        }
        return true;
    }

    private static void applyTaskbarIcon(Image image) {
        if (image == null) return;
        try {
            if (!Taskbar.isTaskbarSupported()) return;
            Taskbar taskbar = Taskbar.getTaskbar();
            if (!taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) return;
            taskbar.setIconImage(image);
        } catch (UnsupportedOperationException | SecurityException | IllegalStateException ex) {
            DebugLog.warn("APP_ICON", "Taskbar icon was not accepted by this desktop session: " + ex.getMessage());
        }
    }
}
