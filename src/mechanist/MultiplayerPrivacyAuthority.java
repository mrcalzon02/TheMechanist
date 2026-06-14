package mechanist;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;

/** Prevents multiplayer endpoints from being exposed by default on stream-visible surfaces. */
final class MultiplayerPrivacyAuthority {
    private MultiplayerPrivacyAuthority() { }

    static String redactEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) return "<address hidden>";
        String text = endpoint.trim();
        int bracket = text.lastIndexOf("]:");
        if (text.startsWith("[") && bracket > 0) return "[IPv6 address hidden]" + text.substring(bracket + 1);
        int colon = text.lastIndexOf(':');
        String port = colon > 0 && colon < text.length() - 1 ? text.substring(colon) : "";
        return "address hidden" + port;
    }

    static String redactLabeledEndpoint(String label, String endpoint) {
        String safeLabel = label == null || label.isBlank() ? "Server" : label.trim();
        String host = endpoint == null ? "" : endpoint.replaceAll("^\\[|\\]?:[0-9]+$", "");
        if ((!host.isBlank() && safeLabel.contains(host)) || safeLabel.matches("(?i).*\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}.*")) {
            safeLabel = "Server";
        }
        return safeLabel + " / " + redactEndpoint(endpoint);
    }

    static void promptDirectAddress(GamePanel panel) {
        if (panel == null || GraphicsEnvironment.isHeadless()) return;
        JPasswordField field = new JPasswordField(panel.multiplayerMenu.directInput(), 30);
        field.setEchoChar('\u2022');
        JButton reveal = new JButton("Reveal");
        reveal.addActionListener(e -> {
            boolean hidden = field.getEchoChar() != 0;
            field.setEchoChar(hidden ? (char) 0 : '\u2022');
            reveal.setText(hidden ? "Hide" : "Reveal");
        });
        JPanel editor = new JPanel(new BorderLayout(8, 8));
        editor.add(new JLabel("Server address (hidden for stream safety)"), BorderLayout.NORTH);
        editor.add(field, BorderLayout.CENTER);
        editor.add(reveal, BorderLayout.EAST);
        int result = JOptionPane.showConfirmDialog(panel, editor, "Direct Multiplayer Address",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) panel.multiplayerMenu.setDirectInput(new String(field.getPassword()));
        java.util.Arrays.fill(field.getPassword(), '\0');
        panel.requestFocusInWindow();
        panel.repaint();
    }
}
