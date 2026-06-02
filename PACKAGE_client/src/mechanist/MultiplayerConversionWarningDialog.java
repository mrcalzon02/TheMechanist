package mechanist;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.atomic.AtomicBoolean;

/** Mandatory explicit warning before a single-player world is converted into a local multiplayer session. */
final class MultiplayerConversionWarningDialog {
    static final String WARNING_TEXT = "WARNING: Converting this single-player world to a local multiplayer session will permanently change the save file architecture. The system will separate player data from world data to support incoming clients, and switch to multiplayer respawn logic. To convert this world back to a strictly turn-based single-player environment, you must exit to the main menu and restart the session.";

    private MultiplayerConversionWarningDialog() { }

    static boolean confirm(Component parent) {
        if (GraphicsEnvironment.isHeadless()) {
            DebugLog.warn("MULTIPLAYER_CONVERSION_WARNING", "Headless environment cannot show Swing confirmation dialog; conversion is denied by default.");
            return false;
        }
        Object[] options = {"Confirm Conversion", "Cancel"};
        JTextArea text = new JTextArea(WARNING_TEXT, 7, 58);
        text.setWrapStyleWord(true);
        text.setLineWrap(true);
        text.setEditable(false);
        text.setFont(text.getFont().deriveFont(Font.BOLD, 14f));
        int result = JOptionPane.showOptionDialog(parent, new JScrollPane(text), "Permanent Multiplayer Conversion Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        return result == JOptionPane.YES_OPTION;
    }
}
