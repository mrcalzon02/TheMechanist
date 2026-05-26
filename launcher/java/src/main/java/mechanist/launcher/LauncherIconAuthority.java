package mechanist.launcher;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class LauncherIconAuthority {
    private static final String[] ICON_CANDIDATES = {
            "assets/app/icons/the-mechanist-256.png",
            "assets/app/icons/the-mechanist-128.png",
            "assets/app/icons/the-mechanist-64.png",
            "assets/app/icons/the-mechanist.png",
            "assets/icons/the-mechanist-256.png",
            "assets/icons/the-mechanist.png"
    };

    private LauncherIconAuthority() {}

    static List<Image> loadWindowIcons(LauncherConfig config) {
        ArrayList<Image> icons = new ArrayList<>();
        for (String rel : ICON_CANDIDATES) {
            addClasspathIcon(icons, "/" + rel);
            addIfPresent(icons, config.launcherDir.resolve(rel));
            addIfPresent(icons, config.repoDir.resolve(rel));
        }
        return icons;
    }

    static String status(LauncherConfig config) {
        for (String rel : ICON_CANDIDATES) {
            if (LauncherIconAuthority.class.getResource("/" + rel) != null) return "bundled launcher icon: /" + rel;
            Path launcher = config.launcherDir.resolve(rel);
            if (Files.isRegularFile(launcher)) return "launcher icon: " + launcher;
            Path repo = config.repoDir.resolve(rel);
            if (Files.isRegularFile(repo)) return "game payload icon: " + repo;
        }
        return "no PNG window icon found; default Java/window icon will be used";
    }

    private static void addIfPresent(List<Image> icons, Path path) {
        try {
            if (Files.isRegularFile(path)) {
                Image image = ImageIO.read(path.toFile());
                if (image != null) icons.add(image);
            }
        } catch (Exception ignored) {
            // Icon loading must not block launcher startup.
        }
    }

    private static void addClasspathIcon(List<Image> icons, String path) {
        try (InputStream in = LauncherIconAuthority.class.getResourceAsStream(path)) {
            if (in == null) return;
            Image image = ImageIO.read(in);
            if (image != null) icons.add(image);
        } catch (Exception ignored) {
            // Icon loading must not block launcher startup.
        }
    }
}
