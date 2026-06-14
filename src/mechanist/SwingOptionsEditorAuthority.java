package mechanist;

import javax.swing.ButtonGroup;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.util.function.IntConsumer;

/** Owns standard Swing editors used by the custom-painted Options shell. */
final class SwingOptionsEditorAuthority {
    private SwingOptionsEditorAuthority() { }

    static void editInt(GamePanel panel, String title, int value, int minimum, int maximum, int step,
                        String suffix, IntConsumer apply) {
        int safeStep = Math.max(1, step);
        JSlider slider = new JSlider(minimum, maximum, Math.max(minimum, Math.min(maximum, value)));
        slider.setMajorTickSpacing(Math.max(safeStep, ((maximum - minimum) / 4 / safeStep) * safeStep));
        slider.setMinorTickSpacing(safeStep);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setSnapToTicks(true);
        JLabel valueLabel = new JLabel(value + suffix, JLabel.CENTER);
        slider.addChangeListener(e -> valueLabel.setText(slider.getValue() + suffix));
        JPanel body = new JPanel(new BorderLayout(8, 8));
        body.add(slider, BorderLayout.CENTER);
        body.add(valueLabel, BorderLayout.SOUTH);
        if (GraphicsEnvironment.isHeadless()) return;
        int result = JOptionPane.showConfirmDialog(panel, body, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) apply.accept(slider.getValue());
        panel.repaint();
        panel.requestFocusInWindow();
    }

    static void editBoolean(GamePanel panel, String title, boolean current, Runnable toggle) {
        JRadioButton enabled = new JRadioButton("On", current);
        JRadioButton disabled = new JRadioButton("Off", !current);
        ButtonGroup group = new ButtonGroup();
        group.add(enabled);
        group.add(disabled);
        JPanel body = new JPanel();
        body.add(enabled);
        body.add(disabled);
        if (GraphicsEnvironment.isHeadless()) return;
        int result = JOptionPane.showConfirmDialog(panel, body, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION && enabled.isSelected() != current) toggle.run();
        panel.requestFocusInWindow();
    }

    static void editColor(GamePanel panel) {
        int key = Math.max(0, Math.min(GameOptions.COLOR_KEYS.length - 1, panel.options.colorTarget));
        Color chosen = GraphicsEnvironment.isHeadless() ? null : JColorChooser.showDialog(panel,
                "Choose " + GameOptions.COLOR_KEYS[key], new Color(panel.options.colorValue(key)));
        if (chosen != null) {
            panel.options.colors[key] = chosen.getRGB() & 0xFFFFFF;
            panel.options.save();
            panel.logEvent(GameOptions.COLOR_KEYS[key] + " color set to #"
                    + String.format("%06X", panel.options.colors[key]) + ".");
            panel.repaint();
        }
        panel.requestFocusInWindow();
    }
}
