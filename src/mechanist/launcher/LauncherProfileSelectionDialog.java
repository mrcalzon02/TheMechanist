package mechanist.launcher;

import java.awt.BorderLayout;
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

    static LauncherFallbackProfileAuthority.LauncherProfile choose(
            Path appHome,
            LauncherFallbackProfileAuthority.LauncherProfile profile
    ) throws IOException {
        if (profile == null || GraphicsEnvironment.isHeadless()) return profile;

        AtomicReference<String> selectedPortrait = new AtomicReference<>(profile.portraitId());

        JLabel profileLabel = new JLabel("Profile: " + profile.profileId(), SwingConstants.CENTER);
        JLabel portraitPreview = new JLabel("", SwingConstants.CENTER);
        portraitPreview.setBorder(BorderFactory.createEtchedBorder());
        JLabel portraitLabel = new JLabel("", SwingConstants.CENTER);
        portraitLabel.setFont(portraitLabel.getFont().deriveFont(Font.BOLD, 18f));
        portraitLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JButton previous = new JButton("< Previous portrait");
        JButton next = new JButton("Next portrait >");
        Runnable refresh = () -> {
            String portraitId = selectedPortrait.get();
            portraitLabel.setText(portraitPresentation(portraitId));
            ImageIcon icon = portraitIcon(appHome, portraitId);
            portraitPreview.setIcon(icon);
            portraitPreview.setText(icon == null ? "Portrait image unavailable" : "");
        };
        previous.addActionListener(event -> {
            selectedPortrait.set(stepPortraitId(selectedPortrait.get(), -1));
            refresh.run();
        });
        next.addActionListener(event -> {
            selectedPortrait.set(stepPortraitId(selectedPortrait.get(), 1));
            refresh.run();
        });
        refresh.run();

        JPanel portraitPanel = new JPanel(new BorderLayout(0, 4));
        portraitPanel.add(portraitPreview, BorderLayout.CENTER);
        portraitPanel.add(portraitLabel, BorderLayout.SOUTH);

        JPanel navigation = new JPanel(new GridLayout(1, 2, 8, 0));
        navigation.add(previous);
        navigation.add(next);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(profileLabel, BorderLayout.NORTH);
        panel.add(portraitPanel, BorderLayout.CENTER);
        panel.add(navigation, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(
                null,
                panel,
                "The Mechanist - Profile",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) return null;

        String selected = selectedPortrait.get();
        if (selected.equals(profile.portraitId())) return profile;
        return LauncherFallbackProfileAuthority.selectHumanPortrait(profile, selected);
    }

    static String stepPortraitId(String current, int delta) {
        int ordinal = LauncherFallbackProfileAuthority.humanPortraitOrdinal(current);
        if (ordinal < 0) ordinal = 0;
        return LauncherFallbackProfileAuthority.humanPortraitId(ordinal + delta);
    }

    static String portraitPresentation(String portraitId) {
        int ordinal = LauncherFallbackProfileAuthority.humanPortraitOrdinal(portraitId);
        if (ordinal < 0) return "Portrait unavailable";
        return "Portrait " + (ordinal + 1) + " of "
                + LauncherFallbackProfileAuthority.humanPortraitCount();
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
