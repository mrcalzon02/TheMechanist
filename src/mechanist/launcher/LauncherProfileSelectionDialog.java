package mechanist.launcher;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Minimal launcher-owned profile chooser.
 *
 * Keeps launcher profile identity outside the client and presents the persisted
 * human portrait selection before launch context and join identity are written.
 */
public final class LauncherProfileSelectionDialog {
    private static final int PREVIEW_SIZE = 128;
    private static final int PREVIEW_FRAME_PADDING = 16;

    static LauncherFallbackProfileAuthority.LauncherProfile choose(
            Path appHome,
            LauncherFallbackProfileAuthority.LauncherProfile profile
    ) throws IOException {
        if (profile == null || GraphicsEnvironment.isHeadless()) return profile;

        int availablePortraits = availablePortraitCount(appHome);
        if (availablePortraits == 0) {
            JOptionPane.showMessageDialog(
                    null,
                    "No usable character portrait images were found in the launcher profile package. "
                            + "Repair or reinstall the game files, then try again.",
                    "The Mechanist - Profile",
                    JOptionPane.ERROR_MESSAGE
            );
            return null;
        }

        AtomicReference<String> selectedPortrait = new AtomicReference<>(
                initialAvailablePortraitId(appHome, profile.portraitId()));

        JLabel profileLabel = new JLabel(profilePresentation(profile), SwingConstants.CENTER);
        profileLabel.setBorder(BorderFactory.createTitledBorder("Profile"));
        JLabel portraitPreview = new JLabel("", SwingConstants.CENTER);
        portraitPreview.setBorder(BorderFactory.createEtchedBorder());
        portraitPreview.setPreferredSize(portraitPreviewSize());
        portraitPreview.setMinimumSize(portraitPreviewSize());
        portraitPreview.getAccessibleContext().setAccessibleName("Selected character portrait preview");
        JLabel portraitLabel = new JLabel("", SwingConstants.CENTER);
        portraitLabel.setFont(portraitLabel.getFont().deriveFont(Font.BOLD, 18f));
        portraitLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JButton previous = new JButton("< Previous portrait");
        JButton next = new JButton("Next portrait >");
        previous.setToolTipText("Show the previous character portrait");
        next.setToolTipText("Show the next character portrait");
        boolean navigationEnabled = portraitNavigationEnabled(availablePortraits);
        previous.setEnabled(navigationEnabled);
        next.setEnabled(navigationEnabled);
        Runnable refresh = () -> {
            String portraitId = selectedPortrait.get();
            int currentAvailablePortraits = availablePortraitCount(appHome);
            boolean currentNavigationEnabled = portraitNavigationEnabled(currentAvailablePortraits);
            previous.setEnabled(currentNavigationEnabled);
            next.setEnabled(currentNavigationEnabled);
            int availablePosition = availablePortraitPosition(appHome, portraitId);
            portraitLabel.setText(portraitPresentation(
                    portraitId,
                    currentAvailablePortraits,
                    availablePosition));
            ImageIcon icon = portraitIcon(appHome, portraitId);
            portraitPreview.setIcon(icon);
            portraitPreview.setText(icon == null ? "Portrait image unavailable" : "");
            portraitPreview.getAccessibleContext().setAccessibleDescription(
                    portraitAccessibilityDescription(
                            portraitId,
                            icon != null,
                            currentAvailablePortraits,
                            availablePosition));
        };
        previous.addActionListener(event -> {
            selectedPortrait.set(stepAvailablePortraitId(appHome, selectedPortrait.get(), -1));
            refresh.run();
        });
        next.addActionListener(event -> {
            selectedPortrait.set(stepAvailablePortraitId(appHome, selectedPortrait.get(), 1));
            refresh.run();
        });
        refresh.run();

        JPanel navigation = new JPanel(new GridLayout(1, 2, 8, 0));
        navigation.add(previous);
        navigation.add(next);

        JPanel portraitPanel = new JPanel(new BorderLayout(0, 4));
        portraitPanel.setBorder(BorderFactory.createTitledBorder("Character portrait"));
        portraitPanel.add(portraitPreview, BorderLayout.CENTER);
        portraitPanel.add(portraitLabel, BorderLayout.NORTH);
        portraitPanel.add(navigation, BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(profileLabel, BorderLayout.NORTH);
        panel.add(portraitPanel, BorderLayout.CENTER);

        while (true) {
            int result = JOptionPane.showConfirmDialog(
                    null,
                    panel,
                    "The Mechanist - Profile",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );
            if (result != JOptionPane.OK_OPTION) return null;

            String selected = selectedPortrait.get();
            if (!portraitAvailable(appHome, selected)) {
                String recovered = initialAvailablePortraitId(appHome, selected);
                if (!selected.equals(recovered) && portraitAvailable(appHome, recovered)) {
                    selectedPortrait.set(recovered);
                    refresh.run();
                    JOptionPane.showMessageDialog(
                            null,
                            "The selected portrait image became unavailable. "
                                    + "Selection moved to the next usable portrait.",
                            "The Mechanist - Profile",
                            JOptionPane.WARNING_MESSAGE
                    );
                } else {
                    refresh.run();
                    JOptionPane.showMessageDialog(
                            null,
                            "No usable character portrait images remain available. "
                                    + "Repair or reinstall the game files, then try again.",
                            "The Mechanist - Profile",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
                continue;
            }
            if (selected.equals(profile.portraitId())) return profile;
            return LauncherFallbackProfileAuthority.selectHumanPortrait(profile, selected);
        }
    }

    static String profilePresentation(LauncherFallbackProfileAuthority.LauncherProfile profile) {
        return profile == null ? "Profile unavailable" : "Local profile";
    }

    static Dimension portraitPreviewSize() {
        int side = PREVIEW_SIZE + PREVIEW_FRAME_PADDING;
        return new Dimension(side, side);
    }

    static boolean portraitNavigationEnabled(int availablePortraits) {
        return availablePortraits > 1;
    }

    static String initialPortraitId(String current) {
        return LauncherFallbackProfileAuthority.isValidHumanPortraitId(current)
                ? current
                : LauncherFallbackProfileAuthority.humanPortraitId(0);
    }

    static String initialAvailablePortraitId(Path appHome, String current) {
        String initial = initialPortraitId(current);
        if (portraitAvailable(appHome, initial)) return initial;
        if (LauncherFallbackProfileAuthority.isValidHumanPortraitId(current)) {
            return stepAvailablePortraitId(appHome, current, 1);
        }
        for (int ordinal = 0; ordinal < LauncherFallbackProfileAuthority.humanPortraitCount(); ordinal++) {
            String candidate = LauncherFallbackProfileAuthority.humanPortraitId(ordinal);
            if (portraitAvailable(appHome, candidate)) return candidate;
        }
        return initial;
    }

    static String stepPortraitId(String current, int delta) {
        int ordinal = LauncherFallbackProfileAuthority.humanPortraitOrdinal(current);
        if (ordinal < 0) {
            return LauncherFallbackProfileAuthority.humanPortraitId(
                    delta < 0 ? LauncherFallbackProfileAuthority.humanPortraitCount() - 1 : 0
            );
        }
        return LauncherFallbackProfileAuthority.humanPortraitId(ordinal + delta);
    }

    static String stepAvailablePortraitId(Path appHome, String current, int delta) {
        int direction = delta < 0 ? -1 : 1;
        int ordinal = LauncherFallbackProfileAuthority.humanPortraitOrdinal(current);
        int base = ordinal >= 0 ? ordinal : (direction < 0 ? 0 : -1);
        for (int step = 1; step <= LauncherFallbackProfileAuthority.humanPortraitCount(); step++) {
            String candidate = LauncherFallbackProfileAuthority.humanPortraitId(base + direction * step);
            if (portraitAvailable(appHome, candidate)) return candidate;
        }
        return initialPortraitId(current);
    }

    static int availablePortraitCount(Path appHome) {
        int count = 0;
        for (int ordinal = 0; ordinal < LauncherFallbackProfileAuthority.humanPortraitCount(); ordinal++) {
            if (portraitAvailable(appHome, LauncherFallbackProfileAuthority.humanPortraitId(ordinal))) {
                count++;
            }
        }
        return count;
    }

    static int availablePortraitPosition(Path appHome, String portraitId) {
        int selectedOrdinal = LauncherFallbackProfileAuthority.humanPortraitOrdinal(portraitId);
        if (selectedOrdinal < 0 || !portraitAvailable(appHome, portraitId)) return 0;
        int position = 0;
        for (int ordinal = 0; ordinal <= selectedOrdinal; ordinal++) {
            String candidate = LauncherFallbackProfileAuthority.humanPortraitId(ordinal);
            if (portraitAvailable(appHome, candidate)) position++;
        }
        return position;
    }

    static String portraitPresentation(String portraitId) {
        int ordinal = LauncherFallbackProfileAuthority.humanPortraitOrdinal(portraitId);
        if (ordinal < 0) return "Portrait unavailable";
        return "Portrait " + (ordinal + 1) + " of "
                + LauncherFallbackProfileAuthority.humanPortraitCount();
    }

    static String portraitPresentation(String portraitId, int availablePortraits) {
        return portraitPresentation(portraitId, availablePortraits, 0);
    }

    static String portraitPresentation(String portraitId, int availablePortraits, int availablePosition) {
        String base = portraitPresentation(portraitId);
        int total = LauncherFallbackProfileAuthority.humanPortraitCount();
        if (LauncherFallbackProfileAuthority.humanPortraitOrdinal(portraitId) < 0
                || availablePortraits >= total) return base;
        int usable = Math.max(0, Math.min(availablePortraits, total));
        if (availablePosition > 0 && availablePosition <= usable) {
            return base + " · installed choice " + availablePosition + " of " + usable;
        }
        return base + " · " + usable + " usable installed";
    }

    static String portraitAccessibilityDescription(String portraitId, boolean imageAvailable) {
        String presentation = portraitPresentation(portraitId);
        return imageAvailable
                ? "Selected " + presentation.toLowerCase(Locale.ROOT)
                : "Selected " + presentation.toLowerCase(Locale.ROOT) + "; image unavailable";
    }

    static String portraitAccessibilityDescription(String portraitId,
                                                   boolean imageAvailable,
                                                   int availablePortraits) {
        return portraitAccessibilityDescription(portraitId, imageAvailable, availablePortraits, 0);
    }

    static String portraitAccessibilityDescription(String portraitId,
                                                   boolean imageAvailable,
                                                   int availablePortraits,
                                                   int availablePosition) {
        String presentation = portraitPresentation(portraitId, availablePortraits, availablePosition);
        return imageAvailable
                ? "Selected " + presentation.toLowerCase(Locale.ROOT)
                : "Selected " + presentation.toLowerCase(Locale.ROOT) + "; image unavailable";
    }

    static Path portraitAssetPath(Path appHome, String portraitId) {
        if (appHome == null) return null;
        int ordinal = LauncherFallbackProfileAuthority.humanPortraitOrdinal(portraitId);
        if (ordinal < 0) return null;
        int row = ordinal / 8 + 1;
        int column = ordinal % 8 + 1;
        String filename = String.format(
                Locale.ROOT,
                "Humans8x8_r%02dc%02d_32px.png",
                row,
                column
        );
        return appHome.resolve("profile-packages")
                .resolve("human-8x8")
                .resolve("assets")
                .resolve(filename)
                .normalize();
    }

    static boolean portraitAvailable(Path appHome, String portraitId) {
        return portraitIcon(appHome, portraitId) != null;
    }

    static ImageIcon portraitIcon(Path appHome, String portraitId) {
        Path asset = portraitAssetPath(appHome, portraitId);
        if (asset == null || !Files.isRegularFile(asset)) return null;
        ImageIcon source = new ImageIcon(asset.toString());
        if (source.getIconWidth() <= 0 || source.getIconHeight() <= 0) return null;
        Image scaled = source.getImage().getScaledInstance(PREVIEW_SIZE, PREVIEW_SIZE, Image.SCALE_REPLICATE);
        return new ImageIcon(scaled);
    }

    private LauncherProfileSelectionDialog() {}
}
