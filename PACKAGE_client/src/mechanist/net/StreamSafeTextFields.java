package mechanist.net;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.Arrays;

public final class StreamSafeTextFields {
    private StreamSafeTextFields() {}

    public static JPanel addressField(String label, JTextField field, boolean streamSafeDefault) {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        JCheckBox streamSafe = new JCheckBox("Stream safe", streamSafeDefault);
        field.setToolTipText("Server address. Enable stream-safe mode before recording or streaming.");
        streamSafe.setToolTipText("Masks the visible address while preserving the actual text for connection use.");
        applyAddressMask(field, streamSafe.isSelected());
        streamSafe.addActionListener(e -> applyAddressMask(field, streamSafe.isSelected()));
        panel.add(new javax.swing.JLabel(label), BorderLayout.WEST);
        panel.add(field, BorderLayout.CENTER);
        panel.add(streamSafe, BorderLayout.EAST);
        return panel;
    }

    public static JPanel passwordField(String label, JPasswordField field) {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        JButton show = new JButton("Show");
        field.setEchoChar('\u2022');
        field.setToolTipText("Server password. Hidden by default; reveal only when you are not streaming or sharing your screen.");
        show.setToolTipText("Temporarily reveal or hide the password field.");
        show.addActionListener(e -> {
            boolean hidden = field.getEchoChar() != 0;
            field.setEchoChar(hidden ? (char) 0 : '\u2022');
            show.setText(hidden ? "Hide" : "Show");
        });
        panel.add(new javax.swing.JLabel(label), BorderLayout.WEST);
        panel.add(field, BorderLayout.CENTER);
        panel.add(show, BorderLayout.EAST);
        return panel;
    }

    public static String redactAddress(String value) {
        if (value == null || value.isBlank()) return "<empty>";
        String trimmed = value.trim();
        int colon = trimmed.lastIndexOf(':');
        String port = colon > 0 && colon < trimmed.length() - 1 ? trimmed.substring(colon) : "";
        return "***.***.***.***" + port;
    }

    public static String redactPassword(char[] password) {
        if (password == null || password.length == 0) return "<empty>";
        char[] chars = new char[Math.min(12, Math.max(4, password.length))];
        Arrays.fill(chars, '\u2022');
        return new String(chars);
    }

    private static void applyAddressMask(JTextField field, boolean enabled) {
        Object stored = field.getClientProperty("mechanist.realAddress");
        if (enabled) {
            if (stored == null) field.putClientProperty("mechanist.realAddress", field.getText());
            field.setText(redactAddress(String.valueOf(field.getClientProperty("mechanist.realAddress"))));
            field.setEditable(false);
        } else {
            if (stored != null) field.setText(String.valueOf(stored));
            field.setEditable(true);
        }
    }

    public static String realAddress(JTextField field) {
        Object stored = field.getClientProperty("mechanist.realAddress");
        return stored == null ? field.getText() : String.valueOf(stored);
    }
}
