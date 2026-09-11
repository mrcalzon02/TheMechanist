package mechanist.launcher;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
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
    static LauncherFallbackProfileAuthority.LauncherProfile choose(
            LauncherFallbackProfileAuthority.LauncherProfile profile
    ) throws IOException {
        if (profile == null || GraphicsEnvironment.isHeadless()) return profile;

        AtomicReference<String> selectedPortrait = new AtomicReference<>(profile.portraitId());

        JLabel profileLabel = new JLabel("Profile: " + profile.profileId(), SwingConstants.CENTER);
        JLabel portraitLabel = new JLabel("", SwingConstants.CENTER);
        portraitLabel.setFont(portraitLabel.getFont().deriveFont(Font.BOLD, 18f));
        portraitLabel.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));

        JButton previous = new JButton("< Previous portrait");
        JButton next = new JButton("Next portrait >");
        Runnable refresh = () -> portraitLabel.setText(portraitPresentation(selectedPortrait.get()));
        previous.addActionListener(event -> {
            selectedPortrait.set(stepPortraitId(selectedPortrait.get(), -1));
            refresh.run();
        });
        next.addActionListener(event -> {
            selectedPortrait.set(stepPortraitId(selectedPortrait.get(), 1));
            refresh.run();
        });
        refresh.run();

        JPanel navigation = new JPanel(new GridLayout(1, 2, 8, 0));
        navigation.add(previous);
        navigation.add(next);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(profileLabel, BorderLayout.NORTH);
        panel.add(portraitLabel, BorderLayout.CENTER);
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

    private LauncherProfileSelectionDialog() {}
}
